package com.example.core.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SensorInfo(
    val name: String,
    val vendor: String,
    val version: Int,
    val type: Int,
    val maxRange: Float,
    val resolution: Float,
    val power: Float
)

@Singleton
class SensorInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAvailableSensors(): List<SensorInfo> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                vendor = sensor.vendor,
                version = sensor.version,
                type = sensor.type,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution,
                power = sensor.power
            )
        }.sortedBy { it.name }
    }
}
