package com.example.feature.storage

import androidx.lifecycle.ViewModel
import com.example.core.common.StorageInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class StorageState(
    val totalBytes: Long = 0,
    val freeBytes: Long = 0,
    val usedBytes: Long = 0,
    val usedPercentage: Float = 0f
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageInfoHelper: StorageInfoHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageState())
    val uiState: StateFlow<StorageState> = _uiState.asStateFlow()

    init {
        loadStorageInfo()
    }

    fun loadStorageInfo() {
        val info = storageInfoHelper.getInternalStorageInfo()
        _uiState.value = StorageState(
            totalBytes = info.totalBytes,
            freeBytes = info.freeBytes,
            usedBytes = info.usedBytes,
            usedPercentage = info.usedPercentage
        )
    }
}
