package com.example.feature.adb

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.feature.dashboard.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceBridgeScreen(
    navController: NavController,
    viewModel: DeviceBridgeViewModel = hiltViewModel()
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val devices by viewModel.usbDevices.collectAsStateWithLifecycle(emptyList())
    val permissions by viewModel.usbPermissionState.collectAsStateWithLifecycle(emptyMap())
    val fastbootState by viewModel.fastbootState.collectAsStateWithLifecycle()
    val fastbootDeviceInfo by viewModel.fastbootDeviceInfo.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf(ConnectionType.LOCAL) }
    
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var pairingPort by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var fastbootResult by remember { mutableStateOf("") }

    val bootImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fastbootResult = "Flashing boot image...\nPlease wait."
            scope.launch {
                fastbootResult = viewModel.flashFastbootImage("boot", uri, context.contentResolver)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Bridge") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state is AdbConnectionState.Connected || fastbootState == com.example.feature.fastboot.FastbootState.CONNECTED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "STATUS", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (fastbootState == com.example.feature.fastboot.FastbootState.CONNECTED) {
                            Text("FASTBOOT DEVICE CONNECTED", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            fastbootDeviceInfo?.let { info ->
                                Text("Product: ${info.product}")
                                Text("Serial: ${info.serial}")
                                Text("Slot: ${info.currentSlot}")
                                Text("Unlocked: ${info.unlocked?.toString() ?: "Unknown"}")
                                Text("Userspace (fastbootd): ${info.isUserspace}")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.disconnectFastboot() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect Fastboot")
                            }
                        } else {
                            when (val s = state) {
                                is AdbConnectionState.Connected -> {
                                    Text("DEVICE CONNECTED", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Model: ${s.device.model}")
                                    Text("Manufacturer: ${s.device.manufacturer}")
                                    Text("Android Version: ${s.device.androidVersion} (API ${s.device.apiLevel})")
                                    Text("Connection: ${s.device.connectionType.label}")
                                    Text("Authorization: ${s.device.authorizationState.label}")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.disconnect() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Disconnect")
                                    }
                                }
                                is AdbConnectionState.Connecting -> Text("Connecting...", style = MaterialTheme.typography.titleMedium)
                                is AdbConnectionState.Authorizing -> Text("Authorizing...", style = MaterialTheme.typography.titleMedium)
                                is AdbConnectionState.Disconnected -> Text("Not Connected", style = MaterialTheme.typography.titleMedium)
                                is AdbConnectionState.Error -> {
                                    Text("Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                    Text(s.message, color = MaterialTheme.colorScheme.error)
                                }
                                is AdbConnectionState.Unsupported -> Text("Unsupported", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            if (state is AdbConnectionState.Connected || fastbootState == com.example.feature.fastboot.FastbootState.CONNECTED) {
                item { SectionTitle("POWER TOOLS") }
                if (state is AdbConnectionState.Connected) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { navController.navigate("terminal") }, modifier = Modifier.weight(1f)) {
                                Text("Shell")
                            }
                            Button(onClick = { navController.navigate("logcat") }, modifier = Modifier.weight(1f)) {
                                Text("Logcat")
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { navController.navigate("apps") }, modifier = Modifier.weight(1f)) {
                                Text("Packages")
                            }
                            Button(onClick = { navController.navigate("device") }, modifier = Modifier.weight(1f)) {
                                Text("Device Info")
                            }
                        }
                    }
                }
                
                if (fastbootState == com.example.feature.fastboot.FastbootState.CONNECTED) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Fastboot Commands", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                fastbootResult = viewModel.executeFastbootCommand("flashing unlock")
                                                // Fallback for older devices if needed: "oem unlock"
                                            }
                                        }, 
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Unlock Bootloader")
                                    }
                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                fastbootResult = viewModel.executeFastbootCommand("flashing lock")
                                            }
                                        }, 
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Lock Bootloader")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { bootImagePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Flash Boot Image (Root)")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Custom Command", style = MaterialTheme.typography.labelMedium)
                                var fastbootCmd by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = fastbootCmd,
                                    onValueChange = { fastbootCmd = it },
                                    label = { Text("Command (e.g. getvar all)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            fastbootResult = viewModel.executeFastbootCommand(fastbootCmd)
                                        }
                                    }, 
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Execute")
                                }
                                if (fastbootResult.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Result:", style = MaterialTheme.typography.labelSmall)
                                    Text(fastbootResult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            } else {
                item { SectionTitle("CONNECTION METHOD") }
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedMethod == ConnectionType.LOCAL,
                            onClick = { selectedMethod = ConnectionType.LOCAL },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                        ) { Text("Local") }
                        SegmentedButton(
                            selected = selectedMethod == ConnectionType.ROOT,
                            onClick = { selectedMethod = ConnectionType.ROOT },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                        ) { Text("Root") }
                        SegmentedButton(
                            selected = selectedMethod == ConnectionType.WIRELESS,
                            onClick = { selectedMethod = ConnectionType.WIRELESS },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                        ) { Text("Wireless") }
                        SegmentedButton(
                            selected = selectedMethod == ConnectionType.USB,
                            onClick = { selectedMethod = ConnectionType.USB },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                        ) { Text("USB") }
                    }
                }

                when (selectedMethod) {
                    ConnectionType.LOCAL -> {
                        item {
                            Text(
                                "Local Shell uses the application's standard privileges (uid). It cannot perform root or true ADB actions, but is useful for standard shell commands.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.connectLocal() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Start Local Shell")
                            }
                        }
                    }
                    ConnectionType.ROOT -> {
                        item {
                            Text(
                                "Root Shell uses libsu to request elevated privileges (su). This requires a rooted device (Magisk/KernelSU).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.connectRoot() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Start Root Shell")
                            }
                        }
                    }
                    ConnectionType.WIRELESS -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Wireless Debugging Setup", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("1. Open Developer Options.", style = MaterialTheme.typography.bodyMedium)
                                    Text("2. Enable Wireless Debugging.", style = MaterialTheme.typography.bodyMedium)
                                    Text("3. Choose 'Pair device with pairing code'.", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    OutlinedTextField(
                                        value = host, onValueChange = { host = it },
                                        label = { Text("IP Address / Host") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = pairingPort, onValueChange = { pairingPort = it },
                                            label = { Text("Pairing Port") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = pairingCode, onValueChange = { pairingCode = it },
                                            label = { Text("Pairing Code") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = port, onValueChange = { port = it },
                                        label = { Text("Connection Port") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.connectWireless(host, port.toIntOrNull() ?: 0, pairingCode, pairingPort.toIntOrNull() ?: 0) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = host.isNotBlank() && port.isNotBlank()
                                    ) {
                                        Text("Pair & Connect")
                                    }
                                }
                            }
                        }
                    }
                    ConnectionType.USB -> {
                        item {
                            Text(
                                "USB connection to external Android devices requires USB OTG and device authorization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.refreshUsbDevices() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Refresh USB Devices")
                            }
                        }

                        if (devices.isEmpty()) {
                            item {
                                Text("No USB devices detected.", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            items(devices) { device ->
                                val hasPermission = permissions[device.deviceName] == true
                                val protocol = viewModel.getUsbProtocol(device)
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("USB DEVICE", style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${device.manufacturerName ?: "Unknown"} ${device.productName ?: ""}", style = MaterialTheme.typography.titleMedium)
                                        Text("VID: ${device.vendorId} | PID: ${device.productId}", style = MaterialTheme.typography.bodySmall)
                                        Text("Protocol: $protocol", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (hasPermission) {
                                            Text("✓ Authorized", color = Color.Green, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    if (protocol == "FASTBOOT") {
                                                        viewModel.connectFastboot(device)
                                                    } else {
                                                        // ADB connection via USB
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Connect $protocol")
                                            }
                                        } else {
                                            Text("⚠ Permission required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { viewModel.requestUsbPermission(device) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Grant USB Access")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
