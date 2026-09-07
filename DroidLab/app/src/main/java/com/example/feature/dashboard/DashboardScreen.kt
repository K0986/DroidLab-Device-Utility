package com.example.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DroidLab", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "DEVICE INTELLIGENCE / CONTROL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Your pocket lab for Android.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Inspect. Diagnose. Bridge. Operate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            
            item { SectionTitle("DEVICE") }
            item {
                DashboardCard(
                    title = "Device Information",
                    subtitle = "Model, OS, Hardware",
                    icon = Icons.Default.PhoneAndroid,
                    onClick = { navController.navigate("device") }
                )
            }

            item { SectionTitle("PERFORMANCE") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardSmallCard(
                        title = "CPU",
                        icon = Icons.Default.Memory,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("cpu") }
                    )
                    DashboardSmallCard(
                        title = "RAM",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("ram") }
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardSmallCard(
                        title = "Storage",
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("storage") }
                    )
                    DashboardSmallCard(
                        title = "Battery",
                        icon = Icons.Default.BatteryFull,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("battery") }
                    )
                }
            }

            item { SectionTitle("DIAGNOSTICS") }
            item {
                DashboardCard("Sensors", "Test hardware sensors", Icons.Default.Explore, { navController.navigate("sensors") })
            }
            item {
                DashboardCard("Display", "Screen and touch tests", Icons.Default.TouchApp, { navController.navigate("display") })
            }
            item {
                DashboardCard("Network", "Wi-Fi and connectivity", Icons.Default.Wifi, { navController.navigate("network") })
            }

            item { SectionTitle("POWER & DEVELOPER") }
            item {
                DashboardCard("Device Bridge", "ADB / Shell / Device management", Icons.Default.Terminal, { navController.navigate("bridge") })
            }
            item {
                DashboardCard("App Manager", "Applications and packages", Icons.Default.Apps, { navController.navigate("apps") })
            }
            item {
                DashboardCard("Terminal", "Developer command-line tools", Icons.Default.Code, { navController.navigate("terminal") })
            }
            
            item { SectionTitle("TOOLS") }
            item {
                DashboardCard("Root Checker", "Check system integrity", Icons.Default.Security, { navController.navigate("root") })
            }
            item {
                DashboardCard("Dev Tools", "Utilities for developers", Icons.Default.Build, { navController.navigate("devtools") })
            }
            item {
                DashboardCard("System", "Detailed API information", Icons.Default.Info, { navController.navigate("system") })
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun DashboardSmallCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
