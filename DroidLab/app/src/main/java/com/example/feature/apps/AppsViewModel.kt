package com.example.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.AppInfo
import com.example.core.common.AppInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsState(
    val isLoading: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val showSystemApps: Boolean = false
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appInfoHelper: AppInfoHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsState())
    val uiState: StateFlow<AppsState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appInfoHelper.getInstalledApps()
            val filtered = filterApps(apps, _uiState.value.searchQuery, _uiState.value.showSystemApps)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                allApps = apps,
                filteredApps = filtered
            )
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value
        val filtered = filterApps(current.allApps, query, current.showSystemApps)
        _uiState.value = current.copy(searchQuery = query, filteredApps = filtered)
    }

    fun toggleSystemApps(show: Boolean) {
        val current = _uiState.value
        val filtered = filterApps(current.allApps, current.searchQuery, show)
        _uiState.value = current.copy(showSystemApps = show, filteredApps = filtered)
    }

    private fun filterApps(apps: List<AppInfo>, query: String, showSystem: Boolean): List<AppInfo> {
        return apps.filter {
            (showSystem || !it.isSystemApp) && 
            (query.isEmpty() || it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true))
        }
    }
}
