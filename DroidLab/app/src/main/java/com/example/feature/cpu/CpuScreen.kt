package com.example.feature.cpu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.feature.dashboard.SectionTitle
import com.example.feature.device.InfoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuScreen(
    navController: NavController,
    viewModel: CpuViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CPU Monitor") },
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
            item { SectionTitle("PROCESSOR") }
            item { InfoCard("Architecture", state.architecture) }
            item { InfoCard("Hardware", state.hardware) }
            item { InfoCard("Cores", state.cores.toString()) }
            item { InfoCard("BogoMIPS", state.bogoMips) }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            item {
                Text(
                    text = "Note: On modern Android versions, detailed per-core frequency scaling is restricted to system applications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
