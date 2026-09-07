package com.example.feature.adb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.ContentResolver
import android.net.Uri

@HiltViewModel
class DeviceBridgeViewModel @Inject constructor(
    private val adbManager: AdbManager,
    private val usbHostManager: com.example.feature.usb.UsbHostManager,
    private val fastbootManager: com.example.feature.fastboot.FastbootManager
) : ViewModel() {

    val connectionState: StateFlow<AdbConnectionState> = adbManager.connectionState
    val usbDevices = usbHostManager.connectedDevices
    val usbPermissionState = usbHostManager.permissionState
    val fastbootState = fastbootManager.connectionState
    val fastbootDeviceInfo = fastbootManager.deviceInfo

    fun connectFastboot(device: android.hardware.usb.UsbDevice) {
        viewModelScope.launch {
            if (fastbootManager.connectDevice(device)) {
                fastbootManager.loadDeviceInfo()
            }
        }
    }

    fun disconnectFastboot() {
        fastbootManager.disconnect()
    }

    suspend fun executeFastbootCommand(command: String): String {
        return fastbootManager.executeRawCommand(command)
    }

    suspend fun flashFastbootImage(partition: String, uri: Uri, contentResolver: ContentResolver): String {
        return fastbootManager.flashImage(partition, uri, contentResolver)
    }

    fun refreshUsbDevices() {
        usbHostManager.refreshDevices()
    }

    fun requestUsbPermission(device: android.hardware.usb.UsbDevice) {
        usbHostManager.requestPermission(device)
    }

    fun getUsbProtocol(device: android.hardware.usb.UsbDevice): String {
        return usbHostManager.getDeviceProtocol(device)
    }

    fun connectLocal() {
        adbManager.connectLocalShell()
    }

    fun connectRoot() {
        adbManager.connectRootShell()
    }

    fun connectWireless(host: String, port: Int, pairingCode: String, pairingPort: Int) {
        adbManager.connectWireless(host, port, pairingCode, pairingPort)
    }

    fun disconnect() {
        adbManager.disconnect()
    }
}
