package com.example.feature.adb

data class AdbDevice(
    val serial: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val connectionType: ConnectionType,
    val authorizationState: AuthorizationState
)

enum class ConnectionType(val label: String) {
    LOCAL("Local Shell"),
    ROOT("Root Shell"),
    WIRELESS("Wireless ADB"),
    USB("USB Debugging"),
    NETWORK("Network TCP")
}

enum class AuthorizationState(val label: String) {
    AUTHORIZED("Authorized"),
    UNAUTHORIZED("Unauthorized"),
    OFFLINE("Offline"),
    UNKNOWN("Unknown")
}
