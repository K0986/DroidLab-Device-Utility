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
    private val rootExecutor: com.example.feature.root.LibsuRootExecutor,
    private val usbExecutor: AdbUsbExecutor,
    private val wirelessExecutor: AdamAdbExecutor
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

    suspend fun connectUsb(device: android.hardware.usb.UsbDevice): Result<AdbDevice> {
        _connectionState.value = AdbConnectionState.Connecting
        return try {
            _connectionState.value = AdbConnectionState.Authorizing
            val connected = usbExecutor.connect(device)
            val adbDevice = AdbDevice(
                serial = connected.serial,
                model = connected.model,
                manufacturer = connected.manufacturer,
                androidVersion = connected.androidVersion,
                apiLevel = connected.apiLevel,
                connectionType = ConnectionType.USB,
                authorizationState = AuthorizationState.AUTHORIZED
            )
            currentExecutor = usbExecutor
            _connectionState.value = AdbConnectionState.Connected(adbDevice)
            Result.success(adbDevice)
        } catch (error: Throwable) {
            usbExecutor.disconnect()
            val message = error.message ?: "Unable to connect to the USB ADB device."
            _connectionState.value = AdbConnectionState.Error(message)
            Result.failure(error)
        }
    }

    fun disconnect() {
        usbExecutor.disconnect()
        _connectionState.value = AdbConnectionState.Disconnected
    }
    
    suspend fun connectWireless(host: String, port: Int, pairingCode: String, pairingPort: Int): Result<AdbDevice> {
        _connectionState.value = AdbConnectionState.Connecting
        return try {
            if (host.isBlank() || port !in 1..65535) {
                error("Enter a valid wireless ADB host and port.")
            }
            if (!wirelessExecutor.connectWireless(host.trim(), port)) {
                error("Could not connect to $host:$port. Confirm Wireless debugging is enabled and the device is reachable.")
            }
            val device = AdbDevice(
                serial = "$host:$port",
                model = "Wireless Android device",
                manufacturer = "Android",
                androidVersion = "ADB",
                apiLevel = 0,
                connectionType = ConnectionType.WIRELESS,
                authorizationState = AuthorizationState.AUTHORIZED
            )
            currentExecutor = wirelessExecutor
            _connectionState.value = AdbConnectionState.Connected(device)
            Result.success(device)
        } catch (error: Throwable) {
            _connectionState.value = AdbConnectionState.Error(error.message ?: "Wireless ADB connection failed.")
            Result.failure(error)
        }
    }
}
