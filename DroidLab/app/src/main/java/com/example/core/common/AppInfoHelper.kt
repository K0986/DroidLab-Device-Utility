package com.example.core.common

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val isSystemApp: Boolean
)

@Singleton
class AppInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(0)
        
        return packages.mapNotNull { packageInfo ->
            try {
                val appInfo = packageInfo.applicationInfo
                val isSystem = (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) != 0
                val name = appInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName
                
                AppInfo(
                    name = name,
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName ?: "Unknown",
                    isSystemApp = isSystem
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name.lowercase() }
    }
}
