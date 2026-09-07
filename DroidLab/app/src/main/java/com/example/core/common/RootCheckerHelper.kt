package com.example.core.common

import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootCheckerHelper @Inject constructor() {

    fun checkRootIndicators(): List<String> {
        val indicators = mutableListOf<String>()

        if (checkTestKeys()) {
            indicators.add("Test-keys detected in build properties")
        }

        val suPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in suPaths) {
            if (File(path).exists()) {
                indicators.add("SU binary found at: $path")
            }
        }

        // Check for common root management apps (heuristic)
        val packagePaths = arrayOf(
            "/data/app/eu.chainfire.supersu",
            "/data/app/com.topjohnwu.magisk"
        )
        
        for (path in packagePaths) {
            val file = File(path)
            if (file.exists() || file.parentFile?.listFiles()?.any { it.name.startsWith(File(path).name) } == true) {
                 indicators.add("Root management package detected: $path")
            }
        }

        return indicators
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
}
