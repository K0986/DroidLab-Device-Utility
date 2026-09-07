package com.example.feature.adb

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.math.min

/**
 * A small, self-contained ADB host implementation for Android USB host mode.
 *
 * This intentionally speaks the ADB transport and sync protocols directly rather
 * than shelling out to a bundled adb binary. That keeps the app usable on phones
 * and tablets, where a desktop adb executable is not available.
 */
class AdbUsbSession(
    private val context: Context,
    private val usbManager: UsbManager
) {
    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var nextLocalId = 1
    private val lock = Any()
    private var connected = false

    var deviceInfo: AdbSessionDevice? = null
        private set

    suspend fun connect(device: UsbDevice): AdbSessionDevice = withContext(Dispatchers.IO) {
        synchronized(lock) {
            closeLocked()
            val adbInterface = device.findAdbInterface()
                ?: error("This USB device does not expose an ADB interface.")
            val endpoints = adbInterface.bulkEndpoints()
            val input = endpoints.first ?: error("ADB device has no bulk IN endpoint.")
            val output = endpoints.second ?: error("ADB device has no bulk OUT endpoint.")
            val usbConnection = usbManager.openDevice(device)
                ?: error("Android denied access to the USB device.")
            if (!usbConnection.claimInterface(adbInterface, true)) {
                usbConnection.close()
                error("Could not claim the device's ADB interface.")
            }

            connection = usbConnection
            usbInterface = adbInterface
            endpointIn = input
            endpointOut = output
            connected = true

            try {
                val info = handshake()
                deviceInfo = info
                info
            } catch (error: Throwable) {
                closeLocked()
                throw error
            }
        }
    }

    suspend fun shell(command: String): String = withContext(Dispatchers.IO) {
        synchronized(lock) {
            requireConnected()
            val stream = openStream("shell:$command")
            val output = StringBuilder()
            try {
                while (true) {
                    val packet = readPacket()
                    when (packet.command) {
                        WRTE -> {
                            output.append(String(packet.payload, StandardCharsets.UTF_8))
                            sendPacket(OKAY, stream.localId, stream.remoteId)
                        }
                        CLSE -> {
                            sendPacket(CLSE, stream.localId, stream.remoteId)
                            break
                        }
                        else -> error("Unexpected ADB shell packet: ${packet.commandName}")
                    }
                }
            } finally {
                closeStreamQuietly(stream)
            }
            output.toString()
        }
    }

    suspend fun push(localFile: File, remotePath: String, mode: Int = 0x1A4): Unit =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                requireConnected()
                require(localFile.isFile) { "The selected local file does not exist." }
                val stream = openStream("sync:")
                try {
                    sendSyncFrame(stream, "SEND", "$remotePath,$mode".toByteArray(StandardCharsets.UTF_8))
                    FileInputStream(localFile).use { input ->
                        val buffer = ByteArray(SYNC_CHUNK_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count <= 0) break
                            sendSyncFrame(stream, "DATA", buffer.copyOf(count))
                        }
                    }
                    val modified = (localFile.lastModified() / 1000L).coerceAtLeast(0L)
                    sendSyncFrame(stream, "DONE", intPayload(modified))
                    expectSyncResult(stream)
                } finally {
                    closeStreamQuietly(stream)
                }
            }
        }

    suspend fun pull(remotePath: String, destination: File): Unit =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                requireConnected()
                destination.parentFile?.mkdirs()
                val stream = openStream("sync:")
                try {
                    sendSyncFrame(stream, "RECV", remotePath.toByteArray(StandardCharsets.UTF_8))
                    FileOutputStream(destination).use { output ->
                        while (true) {
                            val frame = readSyncFrame(stream)
                            when (frame.id) {
                                "DATA" -> output.write(frame.payload)
                                "DONE" -> break
                                "FAIL" -> error(String(frame.payload, StandardCharsets.UTF_8))
                                else -> error("Unexpected ADB pull response: ${frame.id}")
                            }
                        }
                    }
                } finally {
                    closeStreamQuietly(stream)
                }
            }
        }

    suspend fun list(remotePath: String): List<AdbRemoteEntry> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            requireConnected()
            val stream = openStream("sync:")
            val entries = mutableListOf<AdbRemoteEntry>()
            try {
                sendSyncFrame(stream, "LIST", remotePath.toByteArray(StandardCharsets.UTF_8))
                while (true) {
                    val frame = readSyncFrame(stream)
                    when (frame.id) {
                        "DENT" -> {
                            require(frame.payload.size >= 16) { "Malformed directory entry." }
                            val buffer = ByteBuffer.wrap(frame.payload).order(ByteOrder.LITTLE_ENDIAN)
                            val mode = buffer.int
                            val size = buffer.int.toLong() and 0xffffffffL
                            val modified = buffer.int.toLong() and 0xffffffffL
                            val nameLength = buffer.int
                            require(nameLength >= 0 && nameLength <= buffer.remaining()) {
                                "Malformed directory entry name."
                            }
                            val nameBytes = ByteArray(nameLength)
                            buffer.get(nameBytes)
                            entries += AdbRemoteEntry(
                                name = String(nameBytes, StandardCharsets.UTF_8),
                                size = size,
                                modifiedSeconds = modified,
                                isDirectory = (mode and 0x4000) == 0x4000
                            )
                        }
                        "DONE" -> break
                        "FAIL" -> error(String(frame.payload, StandardCharsets.UTF_8))
                        else -> error("Unexpected ADB directory response: ${frame.id}")
                    }
                }
                entries.sortedWith(compareByDescending<AdbRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            } finally {
                closeStreamQuietly(stream)
            }
        }
    }

    fun close() {
        synchronized(lock) {
            closeLocked()
        }
    }

    private fun handshake(): AdbSessionDevice {
        sendPacket(CNXN, ADB_VERSION, MAX_DATA, "host::features=shell_v2,cmd,stat_v2,apex,abb_exec,fixed_push_mkdir,ls_v2\\u0000".toByteArray())
        val keyStore = AdbHostKeyStore(context)
        var signatureSent = false

        repeat(MAX_AUTH_ATTEMPTS) {
            when (val packet = readPacket()) {
                is AdbPacket -> when (packet.command) {
                    AUTH -> {
                        when (packet.arg0) {
                            AUTH_TOKEN -> {
                                if (!signatureSent) {
                                    sendPacket(AUTH, AUTH_SIGNATURE, 0, keyStore.sign(packet.payload))
                                    signatureSent = true
                                } else {
                                    sendPacket(AUTH, AUTH_RSAPUBLICKEY, 0, keyStore.adbPublicKey())
                                }
                            }
                            else -> error("ADB returned an unknown authentication request.")
                        }
                    }
                    CNXN -> {
                        val banner = String(packet.payload, StandardCharsets.UTF_8).trimEnd('\u0000')
                        if (!banner.startsWith("device")) {
                            error("ADB device is not online: $banner")
                        }
                        return parseDeviceBanner(banner)
                    }
                    CLSE -> error("ADB closed the connection during authorization.")
                    else -> error("Unexpected ADB handshake packet: ${packet.commandName}")
                }
            }
        }
        error("ADB authorization timed out. Unlock the connected phone and accept the USB debugging prompt.")
    }

    private fun parseDeviceBanner(banner: String): AdbSessionDevice {
        val properties = banner.substringAfter("::", "")
            .split(';')
            .mapNotNull { pair ->
                val index = pair.indexOf(':')
                if (index > 0) pair.substring(0, index) to pair.substring(index + 1) else null
            }
            .toMap()
        val model = properties["ro.product.model"] ?: properties["model"] ?: "Android device"
        val device = properties["ro.product.device"] ?: properties["device"] ?: "unknown"
        return AdbSessionDevice(
            serial = properties["ro.serialno"] ?: "usb",
            model = model.replace('_', ' '),
            manufacturer = properties["ro.product.manufacturer"] ?: "Android",
            codename = device,
            androidVersion = properties["ro.build.version.release"] ?: "Connected",
            apiLevel = properties["ro.build.version.sdk"]?.toIntOrNull() ?: 0
        )
    }

    private fun openStream(service: String): AdbStreamHandle {
        val localId = nextLocalId++
        sendPacket(OPEN, localId, 0, (service + "\u0000").toByteArray(StandardCharsets.UTF_8))
        while (true) {
            val packet = readPacket()
            when (packet.command) {
                OKAY -> return AdbStreamHandle(localId, packet.arg0)
                WRTE -> sendPacket(OKAY, localId, packet.arg0)
                CLSE -> error("ADB rejected service '$service'.")
                else -> error("Unexpected packet while opening '$service': ${packet.commandName}")
            }
        }
    }

    private fun sendSyncFrame(stream: AdbStreamHandle, id: String, payload: ByteArray) {
        require(id.length == 4)
        val frame = ByteBuffer.allocate(8 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
            .put(id.toByteArray(StandardCharsets.US_ASCII))
            .putInt(payload.size)
            .put(payload)
            .array()
        writeStream(stream, frame)
    }

    private fun expectSyncResult(stream: AdbStreamHandle) {
        val frame = readSyncFrame(stream)
        when (frame.id) {
            "OKAY" -> Unit
            "FAIL" -> error(String(frame.payload, StandardCharsets.UTF_8))
            else -> error("Unexpected ADB sync result: ${frame.id}")
        }
    }

    private fun readSyncFrame(stream: AdbStreamHandle): SyncFrame {
        val header = readStreamBytes(stream, 8)
        val id = String(header.copyOfRange(0, 4), StandardCharsets.US_ASCII)
        val length = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        require(length >= 0 && length <= MAX_SYNC_FRAME) { "ADB sent an invalid sync frame." }
        return SyncFrame(id, readStreamBytes(stream, length))
    }

    private fun writeStream(stream: AdbStreamHandle, payload: ByteArray) {
        sendPacket(WRTE, stream.localId, stream.remoteId, payload)
        while (true) {
            when (val response = readPacket()) {
                is AdbPacket -> when (response.command) {
                    OKAY -> return
                    WRTE -> sendPacket(OKAY, stream.localId, response.arg0)
                    CLSE -> error("ADB closed the active stream.")
                    else -> error("Unexpected ADB stream acknowledgement: ${response.commandName}")
                }
            }
        }
    }

    private fun readStreamBytes(stream: AdbStreamHandle, count: Int): ByteArray {
        while (stream.pending.size < count) {
            val packet = readPacket()
            when (packet.command) {
                WRTE -> {
                    sendPacket(OKAY, stream.localId, packet.arg0)
                    stream.pending.write(packet.payload)
                }
                CLSE -> error("ADB closed the active stream.")
                OKAY -> Unit
                else -> error("Unexpected ADB stream packet: ${packet.commandName}")
            }
        }
        return stream.pending.take(count)
    }

    private fun closeStreamQuietly(stream: AdbStreamHandle) {
        try {
            sendPacket(CLSE, stream.localId, stream.remoteId)
        } catch (_: Throwable) {
            // The physical close below is the source of truth if the device disappeared.
        }
    }

    private fun readPacket(): AdbPacket {
        val header = readFully(ADB_HEADER_SIZE)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val length = buffer.int
        val checksum = buffer.int
        val magic = buffer.int
        require(magic == command.inv()) { "ADB packet magic mismatch." }
        require(length in 0..MAX_PACKET_SIZE) { "ADB packet payload is too large." }
        val payload = readFully(length)
        if (payload.isNotEmpty()) {
            require(checksum(payload) == checksum) { "ADB packet checksum mismatch." }
        }
        return AdbPacket(command, arg0, arg1, payload)
    }

    private fun sendPacket(command: Int, arg0: Int, arg1: Int, payload: ByteArray = ByteArray(0)) {
        val header = ByteBuffer.allocate(ADB_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(command)
            .putInt(arg0)
            .putInt(arg1)
            .putInt(payload.size)
            .putInt(checksum(payload))
            .putInt(command.inv())
            .array()
        writeFully(header)
        if (payload.isNotEmpty()) writeFully(payload)
    }

    private fun readFully(length: Int): ByteArray {
        val result = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = connection?.bulkTransfer(endpointIn, result, offset, length - offset, IO_TIMEOUT_MS)
                ?: error("USB ADB connection was closed.")
            if (count <= 0) error("Timed out reading from the USB ADB device.")
            offset += count
        }
        return result
    }

    private fun writeFully(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val count = connection?.bulkTransfer(endpointOut, bytes, offset, min(bytes.size - offset, SYNC_CHUNK_SIZE), IO_TIMEOUT_MS)
                ?: error("USB ADB connection was closed.")
            if (count <= 0) error("Failed writing to the USB ADB device.")
            offset += count
        }
    }

    private fun requireConnected() {
        check(connected && connection != null && endpointIn != null && endpointOut != null) {
            "No USB ADB device is connected."
        }
    }

    private fun closeLocked() {
        connected = false
        deviceInfo = null
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
        } catch (_: Throwable) {
        }
        connection?.close()
        connection = null
        usbInterface = null
        endpointIn = null
        endpointOut = null
    }

    private data class AdbPacket(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val payload: ByteArray
    ) {
        val commandName: String
            get() = AdbUsbSession.commandName(command)
    }

    private data class AdbStreamHandle(
        val localId: Int,
        val remoteId: Int,
        val pending: ByteArrayOutput = ByteArrayOutput()
    )
    private data class SyncFrame(val id: String, val payload: ByteArray)

    private class ByteArrayOutput(initialCapacity: Int = 1) {
        private var value = ByteArray(initialCapacity.coerceAtLeast(1))
        var size = 0
            private set

        fun write(bytes: ByteArray) {
            if (size + bytes.size > value.size) {
                value = value.copyOf((size + bytes.size).coerceAtLeast(value.size * 2))
            }
            bytes.copyInto(value, size)
            size += bytes.size
        }

        fun take(count: Int): ByteArray {
            require(count in 0..size)
            val result = value.copyOfRange(0, count)
            val remaining = size - count
            if (remaining > 0) value.copyInto(value, 0, count, size)
            size = remaining
            return result
        }

        fun toByteArray(): ByteArray = value.copyOf(size)
    }

    companion object {
        private const val ADB_HEADER_SIZE = 24
        private const val ADB_VERSION = 0x01000000
        private const val MAX_DATA = 64 * 1024
        private const val MAX_PACKET_SIZE = 1024 * 1024
        private const val MAX_SYNC_FRAME = 1024 * 1024
        private const val SYNC_CHUNK_SIZE = 64 * 1024
        private const val IO_TIMEOUT_MS = 10_000
        private const val MAX_AUTH_ATTEMPTS = 5

        private const val AUTH_TOKEN = 1
        private const val AUTH_SIGNATURE = 2
        private const val AUTH_RSAPUBLICKEY = 3

        private val CNXN = command("CNXN")
        private val AUTH = command("AUTH")
        private val OPEN = command("OPEN")
        private val OKAY = command("OKAY")
        private val CLSE = command("CLSE")
        private val WRTE = command("WRTE")

        private fun command(value: String): Int =
            ByteBuffer.wrap(value.toByteArray(StandardCharsets.US_ASCII))
                .order(ByteOrder.LITTLE_ENDIAN)
                .int

        private fun commandName(command: Int): String {
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(command).array()
            return String(bytes, StandardCharsets.US_ASCII)
        }

        private fun checksum(payload: ByteArray): Int =
            payload.fold(0) { sum, byte -> sum + (byte.toInt() and 0xff) }

        private fun intPayload(value: Long): ByteArray =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array()

        private fun UsbDevice.findAdbInterface(): UsbInterface? =
            (0 until interfaceCount).map { getInterface(it) }.firstOrNull {
                it.interfaceClass == 255 &&
                    it.interfaceSubclass == 66 &&
                    it.interfaceProtocol == 1
            }

        private fun UsbInterface.bulkEndpoints(): Pair<UsbEndpoint?, UsbEndpoint?> {
            var input: UsbEndpoint? = null
            var output: UsbEndpoint? = null
            (0 until endpointCount).map { getEndpoint(it) }
                .filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
                .forEach {
                    if (it.direction == UsbConstants.USB_DIR_IN) input = it else output = it
                }
            return input to output
        }
    }
}

data class AdbSessionDevice(
    val serial: String,
    val model: String,
    val manufacturer: String,
    val codename: String,
    val androidVersion: String,
    val apiLevel: Int
)

data class AdbRemoteEntry(
    val name: String,
    val size: Long,
    val modifiedSeconds: Long,
    val isDirectory: Boolean
)

private class AdbHostKeyStore(context: Context) {
    private val directory = File(context.filesDir, "adb-host").apply { mkdirs() }
    private val privateKeyFile = File(directory, "adbkey.pk8")
    private val publicKeyFile = File(directory, "adbkey.x509")

    private val keyPair: KeyPair by lazy {
        if (privateKeyFile.isFile && publicKeyFile.isFile) {
            try {
                val factory = KeyFactory.getInstance("RSA")
                KeyPair(
                    factory.generatePublic(X509EncodedKeySpec(Base64.decode(publicKeyFile.readText(), Base64.DEFAULT))),
                    factory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKeyFile.readText(), Base64.DEFAULT)))
                )
            } catch (_: Throwable) {
                generateAndPersist()
            }
        } else {
            generateAndPersist()
        }
    }

    fun sign(token: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(keyPair.private)
        signature.update(token)
        return signature.sign()
    }

    fun adbPublicKey(): ByteArray {
        val publicKey = keyPair.public as java.security.interfaces.RSAPublicKey
        val modulus = publicKey.modulus
        val radix = BigInteger.ONE.shiftLeft(32)
        val n0 = modulus.and(radix.subtract(BigInteger.ONE))
        val n0Inverse = radix.subtract(n0.modInverse(radix)).and(radix.subtract(BigInteger.ONE))
        val rr = BigInteger.ONE.shiftLeft(2048).pow(2).mod(modulus)

        val key = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(64)
            .putInt(n0Inverse.toInt())
            .put(littleEndian(modulus, 256))
            .put(littleEndian(rr, 256))
            .putInt(publicKey.publicExponent.toInt())
            .array()
        return Base64.encode(key, Base64.NO_WRAP) + " DroidLab\u0000".toByteArray(StandardCharsets.US_ASCII)
    }

    private fun generateAndPersist(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val generated = generator.generateKeyPair()
        privateKeyFile.writeText(Base64.encodeToString(generated.private.encoded, Base64.NO_WRAP))
        publicKeyFile.writeText(Base64.encodeToString(generated.public.encoded, Base64.NO_WRAP))
        return generated
    }

    private fun littleEndian(value: BigInteger, size: Int): ByteArray {
        val source = value.toByteArray().let {
            if (it.size > size) it.copyOfRange(it.size - size, it.size) else it
        }
        val result = ByteArray(size)
        for (index in source.indices) {
            result[index] = source[source.size - 1 - index]
        }
        return result
    }
}