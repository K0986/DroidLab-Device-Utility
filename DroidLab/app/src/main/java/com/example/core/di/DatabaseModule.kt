package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.core.data.AppDatabase
import com.example.core.data.BatteryHistoryDao
import com.example.core.data.CommandHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "droidlab_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideBatteryHistoryDao(database: AppDatabase): BatteryHistoryDao {
        return database.batteryHistoryDao()
    }

    @Provides
    fun provideCommandHistoryDao(database: AppDatabase): CommandHistoryDao {
        return database.commandHistoryDao()
    }
}
