package com.example.core.common

import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuInfoHelper @Inject constructor() {

    fun getNumberOfCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    fun getCpuArchitecture(): String {
        return System.getProperty("os.arch") ?: "Unknown"
    }

    fun getBogoMips(): String {
        return try {
            val cpuInfo = File("/proc/cpuinfo").readText()
            val match = Regex("BogoMIPS\\s*:\\s*(.+)").find(cpuInfo)
            match?.groupValues?.get(1) ?: "Unavailable"
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    fun getHardware(): String {
        return try {
            val cpuInfo = File("/proc/cpuinfo").readText()
            val match = Regex("Hardware\\s*:\\s*(.+)").find(cpuInfo)
            match?.groupValues?.get(1) ?: Build.HARDWARE
        } catch (e: Exception) {
            Build.HARDWARE
        }
    }
}
