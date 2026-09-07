package com.example.feature.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.BatteryData
import com.example.core.common.BatteryInfoHelper
import com.example.core.data.BatteryHistoryDao
import com.example.core.data.BatteryHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val batteryInfoHelper: BatteryInfoHelper,
    private val batteryHistoryDao: BatteryHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatteryData?>(null)
    val uiState: StateFlow<BatteryData?> = _uiState.asStateFlow()

    val historyState: StateFlow<List<BatteryHistoryEntity>> = batteryHistoryDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        var lastLoggedTime = 0L

        batteryInfoHelper.observeBatteryStream()
            .onEach { data ->
                _uiState.value = data
                val now = System.currentTimeMillis()
                // Log at most once per minute
                if (now - lastLoggedTime > 60000) {
                    lastLoggedTime = now
                    viewModelScope.launch {
                        batteryHistoryDao.insert(
                            BatteryHistoryEntity(
                                percentage = data.percentage,
                                temperatureCelsius = data.temperatureCelsius,
                                isCharging = data.isCharging
                            )
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
