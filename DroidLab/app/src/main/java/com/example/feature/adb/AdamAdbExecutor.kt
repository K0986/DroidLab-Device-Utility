package com.example.feature.adb

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import com.malinskiy.adam.request.misc.ConnectDeviceRequest
import com.malinskiy.adam.request.misc.DisconnectDeviceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdamAdbExecutor @Inject constructor() : CommandExecutor {
    private val client: AndroidDebugBridgeClient = AndroidDebugBridgeClientFactory().build()
    var targetSerial: String? = null

    override suspend fun executeCommand(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val serial = targetSerial ?: return@withContext AdbCommandResult(command, "", "No device targeted", -1)
        try {
            val result: ShellCommandResult = client.execute(ShellCommandRequest(command), serial)
            
            val outString = result.stdout?.let { String(it, Charsets.UTF_8).trim() } ?: ""
            val errString = result.stderr?.let { String(it, Charsets.UTF_8).trim() } ?: ""

            AdbCommandResult(
                command = command,
                output = outString,
                errorOutput = errString,
                exitCode = result.exitCode
            )
        } catch (e: Exception) {
            AdbCommandResult(
                command = command,
                output = "",
                errorOutput = e.message ?: "Adam execution failed",
                exitCode = -1
            )
        }
    }

    override fun executeStreamingCommand(command: String): Flow<String> = flow {
        val serial = targetSerial ?: run {
            emit("Error: No device targeted")
            return@flow
        }
        
        try {
            // For simplicity in Phase 3, we execute synchronously and emit.
            // Full v2 streaming requires passing a CoroutineScope to execute.
            val result: ShellCommandResult = client.execute(ShellCommandRequest(command), serial)
            val outString = result.stdout?.let { String(it, Charsets.UTF_8) } ?: ""
            if (outString.isNotEmpty()) emit(outString)
            val errString = result.stderr?.let { String(it, Charsets.UTF_8) } ?: ""
            if (errString.isNotEmpty()) emit(errString)
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelCurrentCommand() {
        // Not straightforward with basic execute
    }

    suspend fun connectWireless(host: String, port: Int): Boolean {
        return try {
            val response = client.execute(ConnectDeviceRequest(host, port))
            val connected = response.contains("connected", ignoreCase = true)
            if (connected) targetSerial = "$host:$port"
            connected
        } catch (e: Exception) {
            false
        }
    }

    suspend fun disconnectWireless() {
        val serial = targetSerial ?: return
        try {
            client.execute(DisconnectDeviceRequest(serial))
        } finally {
            targetSerial = null
        }
    }
}
