package com.example.feature.root

import com.example.feature.adb.AdbCommandResult
import com.example.feature.adb.CommandExecutor
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibsuRootExecutor @Inject constructor() : CommandExecutor {

    private var activeJob: Shell.Job? = null

    // Ensure shell is initialized before use
    init {
        // You can configure libsu here if needed, e.g. Shell.enableVerboseLogging = BuildConfig.DEBUG
    }

    override suspend fun executeCommand(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd(command).exec()
            
            AdbCommandResult(
                command = command,
                output = result.out.joinToString("\n").trim(),
                errorOutput = result.err.joinToString("\n").trim(),
                exitCode = result.code
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

    override fun executeStreamingCommand(command: String): Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        try {
            val job = Shell.cmd(command)
            activeJob = job
            job.to(object : java.util.ArrayList<String>() {
                override fun add(element: String): Boolean {
                    trySend(element)
                    return super.add(element)
                }
            }, object : java.util.ArrayList<String>() {
                override fun add(element: String): Boolean {
                    trySend(element)
                    return super.add(element)
                }
            }).exec()
            close()
        } catch (e: Exception) {
            trySend("Error: ${e.message}")
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelCurrentCommand() {
        // Not perfectly supported by libsu without getting the raw Process
    }
}
