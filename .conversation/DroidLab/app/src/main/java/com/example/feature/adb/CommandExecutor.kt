package com.example.feature.adb

import kotlinx.coroutines.flow.Flow

interface CommandExecutor {
    suspend fun executeCommand(command: String): AdbCommandResult
    fun executeStreamingCommand(command: String): Flow<String>
    suspend fun cancelCurrentCommand()
}
