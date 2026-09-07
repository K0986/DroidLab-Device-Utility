package com.example.core.common

import android.os.Environment
import android.os.StatFs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageInfoHelper @Inject constructor() {

    data class StorageData(
        val totalBytes: Long,
        val freeBytes: Long,
        val usedBytes: Long,
        val usedPercentage: Float
    )

    fun getInternalStorageInfo(): StorageData {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes
        
        val percentage = if (totalBytes > 0) {
            usedBytes.toFloat() / totalBytes.toFloat()
        } else 0f

        return StorageData(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            usedPercentage = percentage
        )
    }
}
