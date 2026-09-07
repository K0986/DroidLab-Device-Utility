package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.ui.theme.DroidLabTheme
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.device.DeviceScreen
import com.example.feature.cpu.CpuScreen
import com.example.feature.memory.MemoryScreen
import com.example.feature.storage.StorageScreen
import com.example.feature.battery.BatteryScreen
import com.example.feature.root.RootScreen
import com.example.feature.devtools.DevToolsScreen
import com.example.feature.system.SystemScreen
import com.example.feature.apps.AppsScreen
import com.example.feature.apps.AppDetailsScreen
import com.example.feature.sensors.SensorsScreen
import com.example.feature.display.DisplayScreen
import com.example.feature.network.NetworkScreen
import com.example.feature.adb.DeviceBridgeScreen
import com.example.feature.terminal.TerminalScreen
import com.example.feature.logcat.LogcatScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DroidLabTheme {
        val navController = rememberNavController()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("dashboard") {
              DashboardScreen(navController = navController)
            }
            composable("device") {
              DeviceScreen(navController = navController)
            }
            composable("cpu") {
              CpuScreen(navController = navController)
            }
            composable("ram") {
              MemoryScreen(navController = navController)
            }
            composable("storage") {
              StorageScreen(navController = navController)
            }
            composable("battery") {
              BatteryScreen(navController = navController)
            }
            composable("root") {
              RootScreen(navController = navController)
            }
            composable("devtools") {
              DevToolsScreen(navController = navController)
            }
            composable("system") {
              SystemScreen(navController = navController)
            }
            composable("apps") {
              AppsScreen(navController = navController)
            }
            composable(
              route = "app_details/{packageName}",
              arguments = listOf(androidx.navigation.navArgument("packageName") { type = androidx.navigation.NavType.StringType })
            ) {
              AppDetailsScreen(navController = navController)
            }
            composable("sensors") {
              SensorsScreen(navController = navController)
            }
            composable("display") {
              DisplayScreen(navController = navController)
            }
            composable("network") {
              NetworkScreen(navController = navController)
            }
            composable("bridge") {
              DeviceBridgeScreen(navController = navController)
            }
            composable("terminal") {
              TerminalScreen(navController = navController)
            }
            composable("logcat") {
              LogcatScreen(navController = navController)
            }
          }
        }
      }
    }
  }
}

