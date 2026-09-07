package com.example.feature.root

import androidx.lifecycle.ViewModel
import com.example.core.common.RootCheckerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class RootCheckState(
    val isChecking: Boolean = true,
    val rootIndicators: List<String> = emptyList()
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val rootCheckerHelper: RootCheckerHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootCheckState())
    val uiState: StateFlow<RootCheckState> = _uiState.asStateFlow()

    init {
        performCheck()
    }

    fun performCheck() {
        _uiState.value = RootCheckState(isChecking = true)
        val indicators = rootCheckerHelper.checkRootIndicators()
        _uiState.value = RootCheckState(isChecking = false, rootIndicators = indicators)
    }
}
