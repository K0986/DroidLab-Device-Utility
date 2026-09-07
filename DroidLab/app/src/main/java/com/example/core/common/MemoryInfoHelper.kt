package com.example.core.common

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class MemoryData(
        val totalRamBytes: Long,
        val availableRamBytes: Long,
        val isLowMemory: Boolean
    )

    fun getMemoryInfo(): MemoryData {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryData(
            totalRamBytes = memoryInfo.totalMem,
            availableRamBytes = memoryInfo.availMem,
            isLowMemory = memoryInfo.lowMemory
        )
    }
}
