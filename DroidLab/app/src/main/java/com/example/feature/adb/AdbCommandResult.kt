package com.example.feature.adb

data class AdbCommandResult(
    val command: String,
    val output: String,
    val errorOutput: String,
    val exitCode: Int,
    val isSuccess: Boolean = exitCode == 0
)
