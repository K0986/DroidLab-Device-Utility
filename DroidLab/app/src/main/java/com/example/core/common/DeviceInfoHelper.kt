package com.example.core.common

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoHelper @Inject constructor() {

    fun getManufacturer(): String = Build.MANUFACTURER
    fun getBrand(): String = Build.BRAND
    fun getModel(): String = Build.MODEL
    fun getDevice(): String = Build.DEVICE
    fun getProduct(): String = Build.PRODUCT
    fun getBoard(): String = Build.BOARD
    fun getHardware(): String = Build.HARDWARE

    fun getAndroidVersion(): String = Build.VERSION.RELEASE
    fun getApiLevel(): Int = Build.VERSION.SDK_INT
    fun getSecurityPatch(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Build.VERSION.SECURITY_PATCH
    } else {
        "Unavailable"
    }
    
    fun getSupportedAbis(): List<String> = Build.SUPPORTED_ABIS.toList()

    fun getKernelVersion(): String = System.getProperty("os.version") ?: "Unavailable"
}
