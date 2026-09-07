package com.example.feature.fastboot

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import android.net.Uri
import android.content.ContentResolver
import java.io.InputStream
import kotlin.math.min

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class FastbootState {
    DISCONNECTED,
    CONNECTED
}

data class FastbootDeviceInfo(
    val product: String,
    val serial: String,
    val currentSlot: String,
    val unlocked: Boolean?,
    val isUserspace: Boolean
)

@Singleton
class FastbootManager @Inject constructor(
    private val usbManager: UsbManager
) {
    
    private val _connectionState = MutableStateFlow(FastbootState.DISCONNECTED)
    val connectionState: StateFlow<FastbootState> = _connectionState.asStateFlow()

    private val _deviceInfo = MutableStateFlow<FastbootDeviceInfo?>(null)
    val deviceInfo: StateFlow<FastbootDeviceInfo?> = _deviceInfo.asStateFlow()

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    
    private val TIMEOUT = 1000

    fun connectDevice(usbDevice: UsbDevice): Boolean {
        disconnect()
        
        var fastbootInterface: UsbInterface? = null
        for (i in 0 until usbDevice.interfaceCount) {
            val intf = usbDevice.getInterface(i)
            if (intf.interfaceClass == 255 && intf.interfaceSubclass == 66 && intf.interfaceProtocol == 3) {
                fastbootInterface = intf
                break
            }
        }
        
        if (fastbootInterface == null) return false

        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null

        for (i in 0 until fastbootInterface.endpointCount) {
            val ep = fastbootInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) {
                    epIn = ep
                } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    epOut = ep
                }
            }
        }

        if (epIn == null || epOut == null) return false

        val conn = usbManager.openDevice(usbDevice) ?: return false
        
        if (conn.claimInterface(fastbootInterface, true)) {
            connection = conn
            usbInterface = fastbootInterface
            endpointIn = epIn
            endpointOut = epOut
            _connectionState.value = FastbootState.CONNECTED
            return true
        } else {
            conn.close()
            return false
        }
    }

    suspend fun getVar(variable: String): String = withContext(Dispatchers.IO) {
        val result = executeRawCommand("getvar:$variable")
        if (result.startsWith("Error:")) return@withContext result
        return@withContext result
    }

    suspend fun flashImage(partition: String, uri: Uri, contentResolver: ContentResolver): String = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext "Error: Not connected"
        val epOut = endpointOut ?: return@withContext "Error: Not connected"
        val epIn = endpointIn ?: return@withContext "Error: Not connected"

        var inputStream: InputStream? = null
        try {
            val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext "Error: Could not open file"
            val fileSize = pfd.statSize
            if (fileSize <= 0) {
                pfd.close()
                return@withContext "Error: File size is 0 or unknown"
            }
            pfd.close()

            // 1. Send download command
            val downloadCmd = "download:${fileSize.toString(16).padStart(8, '0')}"
            var res = executeRawCommand(downloadCmd)
            if (!res.contains("DATA")) {
                return@withContext "Error: Device refused download. Response: $res"
            }

            // 2. Stream data
            inputStream = contentResolver.openInputStream(uri) ?: return@withContext "Error: Could not read stream"
            val buffer = ByteArray(1024 * 1024) // 1MB chunks
            var totalRead = 0L
            
            while (totalRead < fileSize) {
                val read = inputStream.read(buffer)
                if (read <= 0) break
                
                var chunkWritten = 0
                while (chunkWritten < read) {
                    val toWrite = min(read - chunkWritten, 16384) // USB max bulk size is typically smaller, let OS chunk it or we chunk it. Bulk transfer allows up to 16384 usually
                    val w = conn.bulkTransfer(epOut, buffer, chunkWritten, toWrite, TIMEOUT * 10)
                    if (w < 0) {
                        return@withContext "Error: Failed to write data chunk at $totalRead"
                    }
                    chunkWritten += w
                }
                totalRead += read
            }

            // 3. Read OKAY after download
            val respBuffer = ByteArray(64)
            val readAck = conn.bulkTransfer(epIn, respBuffer, respBuffer.size, TIMEOUT * 10)
            if (readAck > 0) {
                val response = String(respBuffer, 0, readAck, Charsets.UTF_8)
                if (!response.startsWith("OKAY")) {
                    return@withContext "Error: Flash failed after download: $response"
                }
            } else {
                return@withContext "Error: Timeout waiting for OKAY after download"
            }

            // 4. Send flash command
            val flashCmd = "flash:$partition"
            return@withContext executeRawCommand(flashCmd)

        } catch (e: Exception) {
            return@withContext "Error flashing: ${e.message}"
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }

    suspend fun executeRawCommand(command: String): String = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext "Error: Not connected"
        val epOut = endpointOut ?: return@withContext "Error: Not connected"
        val epIn = endpointIn ?: return@withContext "Error: Not connected"

        val cmd = command.toByteArray(Charsets.UTF_8)
        val written = conn.bulkTransfer(epOut, cmd, cmd.size, TIMEOUT)
        
        if (written < 0) {
            return@withContext "Error: Failed to write command"
        }

        var result = ""
        val buffer = ByteArray(512)
        
        while (true) {
            val read = conn.bulkTransfer(epIn, buffer, buffer.size, TIMEOUT)
            if (read > 0) {
                val response = String(buffer, 0, read, Charsets.UTF_8)
                if (response.startsWith("INFO")) {
                    result += response.substring(4) + "\n"
                } else if (response.startsWith("OKAY")) {
                    result += "OKAY: " + response.substring(4)
                    break
                } else if (response.startsWith("FAIL")) {
                    result += "FAIL: " + response.substring(4)
                    break
                } else {
                    result += response
                    break
                }
            } else {
                result += "\n(Timeout or end of stream)"
                break
            }
        }
        
        return@withContext result.trim()
    }

    suspend fun loadDeviceInfo() {
        if (_connectionState.value == FastbootState.CONNECTED) {
            val product = getVar("product")
            val serial = getVar("serialno")
            val currentSlot = getVar("current-slot")
            val unlockedStr = getVar("unlocked")
            val isUserspace = getVar("is-userspace") == "yes"
            
            val unlocked = when (unlockedStr.lowercase()) {
                "yes", "true", "1" -> true
                "no", "false", "0" -> false
                else -> null
            }
            
            _deviceInfo.value = FastbootDeviceInfo(
                product = product,
                serial = serial,
                currentSlot = currentSlot,
                unlocked = unlocked,
                isUserspace = isUserspace
            )
        }
    }
    
    fun disconnect() {
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) {
            Log.e("FastbootManager", "Error closing connection", e)
        } finally {
            connection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
            _connectionState.value = FastbootState.DISCONNECTED
            _deviceInfo.value = null
        }
    }
}
