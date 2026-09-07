package com.example.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BatteryHistoryEntity::class, CommandHistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun commandHistoryDao(): CommandHistoryDao
}
