package com.example.feature.device

import androidx.lifecycle.ViewModel
import com.example.core.common.DeviceInfoHelper
import com.example.feature.adb.AdbManager
import com.example.feature.adb.AdbConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DeviceInfoState(
    val manufacturer: String = "",
    val brand: String = "",
    val model: String = "",
    val device: String = "",
    val product: String = "",
    val board: String = "",
    val hardware: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val securityPatch: String = "",
    val supportedAbis: List<String> = emptyList(),
    val kernelVersion: String = ""
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceInfoHelper: DeviceInfoHelper,
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceInfoState())
    val uiState: StateFlow<DeviceInfoState> = _uiState.asStateFlow()
    
    val adbConnectionState: StateFlow<AdbConnectionState> = adbManager.connectionState

    init {
        loadDeviceInfo()
    }

    private fun loadDeviceInfo() {
        _uiState.value = DeviceInfoState(
            manufacturer = deviceInfoHelper.getManufacturer(),
            brand = deviceInfoHelper.getBrand(),
            model = deviceInfoHelper.getModel(),
            device = deviceInfoHelper.getDevice(),
            product = deviceInfoHelper.getProduct(),
            board = deviceInfoHelper.getBoard(),
            hardware = deviceInfoHelper.getHardware(),
            androidVersion = deviceInfoHelper.getAndroidVersion(),
            apiLevel = deviceInfoHelper.getApiLevel(),
            securityPatch = deviceInfoHelper.getSecurityPatch(),
            supportedAbis = deviceInfoHelper.getSupportedAbis(),
            kernelVersion = deviceInfoHelper.getKernelVersion()
        )
    }
}
