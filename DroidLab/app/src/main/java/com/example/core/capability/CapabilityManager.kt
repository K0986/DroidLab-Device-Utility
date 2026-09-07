package com.example.core.capability

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityManager @Inject constructor() {
    
    private val _currentPrivilege = MutableStateFlow(PrivilegeLevel.NORMAL)
    val currentPrivilege: StateFlow<PrivilegeLevel> = _currentPrivilege.asStateFlow()

    fun updatePrivilege(level: PrivilegeLevel) {
        _currentPrivilege.value = level
    }

    fun hasPrivilege(required: PrivilegeLevel): CapabilityState {
        val current = _currentPrivilege.value
        return if (current.ordinal >= required.ordinal) { // simplistic comparison, in reality Shizuku vs Root vs ADB is more complex
            CapabilityState.Available
        } else {
            CapabilityState.Unavailable("Requires $required access. Current is $current.", required)
        }
    }
}
