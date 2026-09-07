package com.example.core.di

import com.example.feature.adb.AdbManager
import com.example.feature.adb.LocalShellExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdbModule {

    @Provides
    @Singleton
    fun provideLocalShellExecutor(): LocalShellExecutor {
        return LocalShellExecutor()
    }

    @Provides
    @Singleton
    fun provideAdbManager(
        localShellExecutor: LocalShellExecutor,
        rootExecutor: com.example.feature.root.LibsuRootExecutor,
        usbExecutor: com.example.feature.adb.AdbUsbExecutor,
        wirelessExecutor: com.example.feature.adb.AdamAdbExecutor
    ): AdbManager {
        return AdbManager(localShellExecutor, rootExecutor, usbExecutor, wirelessExecutor)
    }
}
