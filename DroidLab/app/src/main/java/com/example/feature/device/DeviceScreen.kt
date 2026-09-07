package com.example.feature.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.feature.dashboard.SectionTitle
import com.example.feature.adb.AdbConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    navController: NavController,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val adbState by viewModel.adbConnectionState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Information") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("MANUFACTURING") }
            item { InfoCard("Manufacturer", state.manufacturer) }
            item { InfoCard("Brand", state.brand) }
            item { InfoCard("Model", state.model) }
            
            item { SectionTitle("HARDWARE") }
            item { InfoCard("Device Codename", state.device) }
            item { InfoCard("Product", state.product) }
            item { InfoCard("Board", state.board) }
            item { InfoCard("Hardware", state.hardware) }
            item { InfoCard("Supported ABIs", state.supportedAbis.joinToString(", ")) }

            item { SectionTitle("SOFTWARE") }
            item { InfoCard("Android Version", state.androidVersion) }
            item { InfoCard("API Level", state.apiLevel.toString()) }
            item { InfoCard("Security Patch", state.securityPatch) }
            item { InfoCard("Kernel Version", state.kernelVersion) }
            
            if (adbState is AdbConnectionState.Connected) {
                item { SectionTitle("ADVANCED (ADB)") }
                item {
                    Button(onClick = { navController.navigate("terminal") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Inspect via ADB Shell (getprop)")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun InfoCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.ifEmpty { "Unavailable" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
