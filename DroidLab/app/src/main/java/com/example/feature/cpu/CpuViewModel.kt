package com.example.feature.cpu

import androidx.lifecycle.ViewModel
import com.example.core.common.CpuInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CpuState(
    val cores: Int = 0,
    val architecture: String = "",
    val bogoMips: String = "",
    val hardware: String = ""
)

@HiltViewModel
class CpuViewModel @Inject constructor(
    private val cpuInfoHelper: CpuInfoHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(CpuState())
    val uiState: StateFlow<CpuState> = _uiState.asStateFlow()

    init {
        loadCpuInfo()
    }

    private fun loadCpuInfo() {
        _uiState.value = CpuState(
            cores = cpuInfoHelper.getNumberOfCores(),
            architecture = cpuInfoHelper.getCpuArchitecture(),
            bogoMips = cpuInfoHelper.getBogoMips(),
            hardware = cpuInfoHelper.getHardware()
        )
    }
}
