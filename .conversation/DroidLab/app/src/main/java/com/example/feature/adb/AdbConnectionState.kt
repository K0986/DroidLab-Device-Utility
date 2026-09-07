package com.example.feature.adb

sealed class AdbConnectionState {
    object Disconnected : AdbConnectionState()
    object Connecting : AdbConnectionState()
    object Authorizing : AdbConnectionState()
    data class Connected(val device: AdbDevice) : AdbConnectionState()
    data class Error(val message: String) : AdbConnectionState()
    object Unsupported : AdbConnectionState()
}
