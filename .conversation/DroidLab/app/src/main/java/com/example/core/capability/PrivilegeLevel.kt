package com.example.core.capability

enum class PrivilegeLevel {
    NORMAL,    // Standard Android application permissions
    ADB,       // Shell/ADB privilege (uid 2000)
    SHIZUKU,   // Shizuku (adb/root proxy)
    ROOT,      // Full root access (uid 0)
    FASTBOOT   // Bootloader level
}
