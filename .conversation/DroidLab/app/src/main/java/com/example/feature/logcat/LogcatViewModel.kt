package com.example.feature.logcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.adb.AdbManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogcatState(
    val logs: List<String> = emptyList(),
    val isRunning: Boolean = false,
    val filter: String = ""
)

@HiltViewModel
class LogcatViewModel @Inject constructor(
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogcatState())
    val uiState: StateFlow<LogcatState> = _uiState.asStateFlow()
    
    private var logcatJob: Job? = null
    
    fun startLogcat() {
        if (_uiState.value.isRunning) return
        _uiState.value = _uiState.value.copy(isRunning = true)
        
        logcatJob = viewModelScope.launch {
            adbManager.currentExecutor.executeStreamingCommand("logcat -v time")
                .catch { e ->
                    appendLog("Error: ${e.message}")
                    _uiState.value = _uiState.value.copy(isRunning = false)
                }
                .collect { line ->
                    if (_uiState.value.filter.isBlank() || line.contains(_uiState.value.filter, ignoreCase = true)) {
                        appendLog(line)
                    }
                }
        }
    }
    
    fun stopLogcat() {
        viewModelScope.launch {
            adbManager.currentExecutor.cancelCurrentCommand()
            logcatJob?.cancel()
            _uiState.value = _uiState.value.copy(isRunning = false)
        }
    }
    
    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
        viewModelScope.launch {
            adbManager.currentExecutor.executeCommand("logcat -c")
        }
    }
    
    fun updateFilter(newFilter: String) {
        _uiState.value = _uiState.value.copy(filter = newFilter)
    }
    
    private fun appendLog(line: String) {
        val current = _uiState.value.logs
        // keep last 1000 lines
        val newLogs = if (current.size >= 1000) current.drop(1) + line else current + line
        _uiState.value = _uiState.value.copy(logs = newLogs)
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLogcat()
    }
}
