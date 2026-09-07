package com.example.feature.root

enum class RootStatus {
    UNAVAILABLE,
    AVAILABLE_BUT_DENIED,
    GRANTED
}

interface RootCommandExecutor {
    suspend fun executeAsRoot(command: String): RootCommandResult
}

data class RootCommandResult(
    val command: String,
    val output: String,
    val errorOutput: String,
    val exitCode: Int
)
