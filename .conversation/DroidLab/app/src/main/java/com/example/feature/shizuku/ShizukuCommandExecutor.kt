package com.example.feature.shizuku

import android.content.pm.PackageManager
import com.example.feature.adb.AdbCommandResult
import com.example.feature.adb.CommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuCommandExecutor @Inject constructor() : CommandExecutor {
    
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun requestPermission() {
        if (isShizukuAvailable() && !hasPermission()) {
            Shizuku.requestPermission(0)
        }
    }

    override suspend fun executeCommand(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable() || !hasPermission()) {
            return@withContext AdbCommandResult(
                command = command,
                output = "",
                errorOutput = "Shizuku is not available or permission denied.",
                exitCode = -1
            )
        }
        
        try {
            // Shizuku.newProcess is private/deprecated in modern versions.
            // Executing shell commands directly requires setting up a UserService, 
            // which is beyond the current Phase 3 scope.
            AdbCommandResult(
                command = command,
                output = "",
                errorOutput = "Shizuku Shell (via UserService) is not yet fully implemented.",
                exitCode = -1
            )
        } catch (e: Exception) {
            AdbCommandResult(
                command = command,
                output = "",
                errorOutput = e.message ?: "Shizuku execution failed",
                exitCode = -1
            )
        }
    }

    override fun executeStreamingCommand(command: String): Flow<String> = flow {
        if (!isShizukuAvailable() || !hasPermission()) {
            emit("Error: Shizuku is not available or permission denied.")
            return@flow
        }
        
        try {
            emit("Error: Shizuku Shell (via UserService) is not yet fully implemented.")
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelCurrentCommand() {}
}
