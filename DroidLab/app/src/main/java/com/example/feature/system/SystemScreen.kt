package com.example.feature.system

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.feature.dashboard.SectionTitle
import com.example.feature.device.InfoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Information") },
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
            item { SectionTitle("BUILD PROPERTIES") }
            item { InfoCard("FINGERPRINT", Build.FINGERPRINT) }
            item { InfoCard("BOOTLOADER", Build.BOOTLOADER) }
            item { InfoCard("HOST", Build.HOST) }
            item { InfoCard("ID", Build.ID) }
            item { InfoCard("USER", Build.USER) }
            item { InfoCard("TIME", Build.TIME.toString()) }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
