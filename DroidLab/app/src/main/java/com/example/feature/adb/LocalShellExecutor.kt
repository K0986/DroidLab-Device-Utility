package com.example.feature.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class LocalShellExecutor @Inject constructor() : CommandExecutor {
    
    private var currentProcess: Process? = null

    override suspend fun executeCommand(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()
            
            currentProcess = process

            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val errorOutput = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()

            currentProcess = null

            AdbCommandResult(
                command = command,
                output = output.trim(),
                errorOutput = errorOutput.trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            AdbCommandResult(
                command = command,
                output = "",
                errorOutput = e.message ?: "Unknown error",
                exitCode = -1
            )
        }
    }

    override fun executeStreamingCommand(command: String): Flow<String> = flow {
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            
            currentProcess = process
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                emit(line)
                line = reader.readLine()
            }
            
            process.waitFor()
            currentProcess = null
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelCurrentCommand() {
        withContext(Dispatchers.IO) {
            currentProcess?.destroy()
            currentProcess = null
        }
    }
}
