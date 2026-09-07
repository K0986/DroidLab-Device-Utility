package com.example.core.capability

sealed class CapabilityState {
    object Available : CapabilityState()
    data class Unavailable(val reason: String, val requiredPrivilege: PrivilegeLevel) : CapabilityState()
}
