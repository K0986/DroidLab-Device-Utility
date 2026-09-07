package com.example.feature.adb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

import android.content.ContentResolver
import android.net.Uri

@HiltViewModel
class DeviceBridgeViewModel @Inject constructor(
    private val adbManager: AdbManager,
    private val usbHostManager: com.example.feature.usb.UsbHostManager,
    private val fastbootManager: com.example.feature.fastboot.FastbootManager
) : ViewModel() {
    private val _remoteEntries = MutableStateFlow<List<AdbRemoteEntry>>(emptyList())
    val remoteEntries: StateFlow<List<AdbRemoteEntry>> = _remoteEntries.asStateFlow()

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

    fun connectUsb(device: android.hardware.usb.UsbDevice) {
        viewModelScope.launch {
            adbManager.connectUsb(device)
        }
    }

    fun pushFile(localFile: java.io.File, remotePath: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val executor = adbManager.currentExecutor as? AdbUsbExecutor
                    ?: error("Connect to a USB ADB device first.")
                executor.push(localFile, remotePath)
                onComplete("Pushed ${localFile.name} to $remotePath")
            } catch (error: Throwable) {
                onComplete("Push failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun pullFile(remotePath: String, localFile: java.io.File, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val executor = adbManager.currentExecutor as? AdbUsbExecutor
                    ?: error("Connect to a USB ADB device first.")
                executor.pull(remotePath, localFile)
                onComplete("Pulled $remotePath to ${localFile.name}")
            } catch (error: Throwable) {
                onComplete("Pull failed: ${error.message ?: "unknown error"}")
            }
        }
    }

    fun pullFileToUri(
        remotePath: String,
        uri: Uri,
        contentResolver: ContentResolver,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            val tempFile = File.createTempFile("droidlab-pull-", ".bin")
            try {
                val executor = adbManager.currentExecutor as? AdbUsbExecutor
                    ?: error("Connect to a USB ADB device first.")
                executor.pull(remotePath, tempFile)
                contentResolver.openOutputStream(uri)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("The selected destination could not be opened.")
                onComplete("Pulled $remotePath successfully")
            } catch (error: Throwable) {
                onComplete("Pull failed: ${error.message ?: "unknown error"}")
            } finally {
                tempFile.delete()
            }
        }
    }

    fun listRemotePath(path: String) {
        viewModelScope.launch {
            try {
                val executor = adbManager.currentExecutor as? AdbUsbExecutor
                    ?: error("Connect to a USB ADB device first.")
                _remoteEntries.value = executor.list(path)
            } catch (_: Throwable) {
                _remoteEntries.value = emptyList()
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
        viewModelScope.launch {
            adbManager.connectWireless(host, port, pairingCode, pairingPort)
        }
    }

    fun disconnect() {
        adbManager.disconnect()
    }
}
