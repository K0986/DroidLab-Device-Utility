package com.example.feature.adb

import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbManager @Inject constructor(
    private val localShellExecutor: LocalShellExecutor,
    private val rootExecutor: com.example.feature.root.LibsuRootExecutor
) {
    private val _connectionState = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val connectionState: StateFlow<AdbConnectionState> = _connectionState.asStateFlow()

    // Default to local executor
    var currentExecutor: CommandExecutor = localShellExecutor
        private set

    fun connectLocalShell() {
        val device = AdbDevice(
            serial = "localhost",
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            connectionType = ConnectionType.LOCAL,
            authorizationState = AuthorizationState.AUTHORIZED
        )
        currentExecutor = localShellExecutor
        _connectionState.value = AdbConnectionState.Connected(device)
    }

    fun connectRootShell() {
        val device = AdbDevice(
            serial = "localhost-root",
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            connectionType = ConnectionType.LOCAL,
            authorizationState = AuthorizationState.AUTHORIZED
        )
        currentExecutor = rootExecutor
        _connectionState.value = AdbConnectionState.Connected(device)
    }

    fun disconnect() {
        _connectionState.value = AdbConnectionState.Disconnected
    }
    
    // Stubs for Wireless/USB
    fun connectWireless(host: String, port: Int, pairingCode: String, pairingPort: Int) {
        _connectionState.value = AdbConnectionState.Error("Real Wireless ADB client not yet fully implemented. Try Local Shell instead.")
    }
}
