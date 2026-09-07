package com.example.core.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryData(
    val percentage: Int,
    val isCharging: Boolean,
    val plugType: String,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val technology: String,
    val health: String,
    val capacityMah: Double
)

@Singleton
class BatteryInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun observeBatteryStream(): Flow<BatteryData> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    trySend(extractBatteryData(it, context))
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(receiver, filter)
        
        initialIntent?.let {
            trySend(extractBatteryData(it, context))
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun extractBatteryData(intent: Intent, context: Context?): BatteryData {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level != -1 && scale != -1) (level * 100 / scale) else 0
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not Plugged"
        }
        
        val tempTenthsCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temperature = if (tempTenthsCelsius != -1) tempTenthsCelsius / 10f else 0f
        
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        
        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        
        // Try to get capacity
        var capacityMah = 0.0
        if (context != null) {
            val powerProfileClass = "com.android.internal.os.PowerProfile"
            try {
                val mPowerProfile = Class.forName(powerProfileClass).getConstructor(Context::class.java).newInstance(context)
                capacityMah = Class.forName(powerProfileClass)
                    .getMethod("getBatteryCapacity")
                    .invoke(mPowerProfile) as Double
            } catch (e: Exception) {
                // Ignore if unavailable
            }
        }
        
        return BatteryData(
            percentage = percentage,
            isCharging = isCharging,
            plugType = plugType,
            temperatureCelsius = temperature,
            voltageMv = voltage,
            technology = technology,
            health = health,
            capacityMah = capacityMah
        )
    }
}
