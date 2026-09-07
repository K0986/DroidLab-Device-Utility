package com.example.feature.adb

import android.content.Context
import android.hardware.usb.UsbDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbUsbExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: android.hardware.usb.UsbManager
) : CommandExecutor {
    private var session: AdbUsbSession? = null
    var connectedDevice: AdbSessionDevice? = null
        private set

    suspend fun connect(device: UsbDevice): AdbSessionDevice = withContext(Dispatchers.IO) {
        disconnect()
        val newSession = AdbUsbSession(context, usbManager)
        val info = newSession.connect(device)
        session = newSession
        connectedDevice = info
        info
    }

    fun disconnect() {
        session?.close()
        session = null
        connectedDevice = null
    }

    override suspend fun executeCommand(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        try {
            val output = session?.shell(command)
                ?: return@withContext AdbCommandResult(command, "", "No USB ADB device connected.", -1)
            AdbCommandResult(command, output.trimEnd(), "", 0)
        } catch (error: Throwable) {
            AdbCommandResult(command, "", error.message ?: "USB ADB command failed.", -1)
        }
    }

    override fun executeStreamingCommand(command: String): Flow<String> = flow {
        val result = executeCommand(command)
        if (result.output.isNotBlank()) emit(result.output)
        if (result.errorOutput.isNotBlank()) emit("Error: ${result.errorOutput}")
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelCurrentCommand() {
        // USB bulk transfers are bounded by IO_TIMEOUT_MS. Closing the session
        // safely interrupts an in-flight command and releases the device.
        disconnect()
    }

    suspend fun push(localFile: File, remotePath: String) {
        session?.push(localFile, remotePath) ?: error("No USB ADB device connected.")
    }

    suspend fun pull(remotePath: String, destination: File) {
        session?.pull(remotePath, destination) ?: error("No USB ADB device connected.")
    }

    suspend fun list(remotePath: String): List<AdbRemoteEntry> {
        return session?.list(remotePath) ?: error("No USB ADB device connected.")
    }
}