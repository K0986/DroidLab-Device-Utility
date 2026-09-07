package com.example.feature.apps

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.adb.AdbManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AppDetailsState(
    val packageName: String = "",
    val name: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val targetSdk: Int = 0,
    val minSdk: Int = 0,
    val sourceDir: String = "",
    val apkSize: Long = 0,
    val permissions: List<String> = emptyList(),
    val isSystemApp: Boolean = false,
    val isLoaded: Boolean = false,
    val adbActionOutput: String = ""
)

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailsState())
    val uiState: StateFlow<AppDetailsState> = _uiState.asStateFlow()

    init {
        val packageName = savedStateHandle.get<String>("packageName")
        if (packageName != null) {
            loadAppDetails(packageName)
        }
    }

    private fun loadAppDetails(packageName: String) {
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val applicationInfo = packageInfo.applicationInfo
            
            if (applicationInfo == null) {
                _uiState.value = _uiState.value.copy(isLoaded = true, name = "Application info not found")
                return
            }
            
            val file = File(applicationInfo.sourceDir)
            val apkSize = if (file.exists()) file.length() else 0L

            _uiState.value = AppDetailsState(
                packageName = packageName,
                name = packageManager.getApplicationLabel(applicationInfo).toString(),
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
                targetSdk = applicationInfo.targetSdkVersion,
                minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) applicationInfo.minSdkVersion else 0,
                sourceDir = applicationInfo.sourceDir,
                apkSize = apkSize,
                permissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
                isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                isLoaded = true
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoaded = true, name = "Error loading app")
        }
    }

    fun executeAdbAction(action: String) {
        val packageName = _uiState.value.packageName
        if (packageName.isBlank()) return
        
        viewModelScope.launch {
            val command = when (action) {
                "force-stop" -> "am force-stop $packageName"
                "clear-data" -> "pm clear $packageName"
                "uninstall" -> "pm uninstall $packageName"
                "disable" -> "pm disable-user --user 0 $packageName"
                else -> ""
            }
            if (command.isNotBlank()) {
                val result = adbManager.currentExecutor.executeCommand(command)
                _uiState.value = _uiState.value.copy(adbActionOutput = result.output + "\n" + result.errorOutput)
            }
        }
    }
}
