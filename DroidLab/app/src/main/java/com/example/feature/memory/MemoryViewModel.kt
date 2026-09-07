package com.example.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.MemoryInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryState(
    val totalRamBytes: Long = 0,
    val availableRamBytes: Long = 0,
    val usedRamBytes: Long = 0,
    val usedPercentage: Float = 0f,
    val isLowMemory: Boolean = false
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryInfoHelper: MemoryInfoHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryState())
    val uiState: StateFlow<MemoryState> = _uiState.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                val memInfo = memoryInfoHelper.getMemoryInfo()
                val used = memInfo.totalRamBytes - memInfo.availableRamBytes
                val percentage = if (memInfo.totalRamBytes > 0) {
                    used.toFloat() / memInfo.totalRamBytes.toFloat()
                } else 0f

                _uiState.value = MemoryState(
                    totalRamBytes = memInfo.totalRamBytes,
                    availableRamBytes = memInfo.availableRamBytes,
                    usedRamBytes = used,
                    usedPercentage = percentage,
                    isLowMemory = memInfo.isLowMemory
                )
                
                // Update every 2 seconds
                delay(2000)
            }
        }
    }
}
