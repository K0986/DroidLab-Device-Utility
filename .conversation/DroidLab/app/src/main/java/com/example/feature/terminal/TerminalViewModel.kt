package com.example.feature.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.CommandHistoryDao
import com.example.core.data.CommandHistoryEntity
import com.example.feature.adb.AdbManager
import com.example.feature.adb.AdbCommandResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

data class TerminalState(
    val output: String = "",
    val isRunning: Boolean = false,
    val history: List<CommandHistoryEntity> = emptyList()
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val adbManager: AdbManager,
    private val commandHistoryDao: CommandHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalState())
    val uiState: StateFlow<TerminalState> = _uiState.asStateFlow()
    
    val adbConnectionState: StateFlow<com.example.feature.adb.AdbConnectionState> = adbManager.connectionState

    private var currentStreamJob: Job? = null

    init {
        viewModelScope.launch {
            commandHistoryDao.getHistory().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }

    fun executeCommand(command: String, streaming: Boolean = false) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            commandHistoryDao.insert(CommandHistoryEntity(command = command))
        }

        _uiState.value = _uiState.value.copy(isRunning = true, output = _uiState.value.output + "\n$ " + command + "\n")
        
        if (streaming) {
            currentStreamJob?.cancel()
            currentStreamJob = viewModelScope.launch {
                adbManager.currentExecutor.executeStreamingCommand(command)
                    .catch { e ->
                        appendOutput("Error: ${e.message}\n")
                        _uiState.value = _uiState.value.copy(isRunning = false)
                    }
                    .collect { line ->
                        appendOutput(line + "\n")
                    }
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        } else {
            viewModelScope.launch {
                val result = adbManager.currentExecutor.executeCommand(command)
                val newOutput = buildString {
                    if (result.output.isNotBlank()) appendLine(result.output)
                    if (result.errorOutput.isNotBlank()) appendLine("stderr: ${result.errorOutput}")
                    appendLine("[Exit: ${result.exitCode}]")
                }
                appendOutput(newOutput)
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        }
    }
    
    fun cancelCommand() {
        viewModelScope.launch {
            adbManager.currentExecutor.cancelCurrentCommand()
            currentStreamJob?.cancel()
            appendOutput("\n[Process cancelled]\n")
            _uiState.value = _uiState.value.copy(isRunning = false)
        }
    }

    fun clearOutput() {
        _uiState.value = _uiState.value.copy(output = "")
    }

    private fun appendOutput(text: String) {
        _uiState.value = _uiState.value.copy(output = _uiState.value.output + text)
    }
}
