package com.example.feature.adb

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceBridgeScreen(
    navController: NavController,
    viewModel: DeviceBridgeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val devices by viewModel.usbDevices.collectAsStateWithLifecycle(emptyList())
    val permissions by viewModel.usbPermissionState.collectAsStateWithLifecycle(emptyMap())
    val remoteEntries by viewModel.remoteEntries.collectAsStateWithLifecycle()
    val fastbootState by viewModel.fastbootState.collectAsStateWithLifecycle()
    val fastbootInfo by viewModel.fastbootDeviceInfo.collectAsStateWithLifecycle()

    var remotePath by remember { mutableStateOf("/sdcard/") }
    var transferStatus by remember { mutableStateOf("") }
    var localPushFile by remember { mutableStateOf<File?>(null) }
    var selectedMethod by remember { mutableStateOf(ConnectionType.USB) }
    var fastbootResult by remember { mutableStateOf("") }
    var wirelessHost by remember { mutableStateOf("") }
    var wirelessPort by remember { mutableStateOf("5555") }

    val pushPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val temp = File.createTempFile("droidlab-push-", ".bin", context.cacheDir)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not read the selected file.")
                    localPushFile = temp
                    transferStatus = "Ready to push ${temp.name}"
                } catch (error: Throwable) {
                    temp.delete()
                    transferStatus = error.message ?: "Could not read the selected file."
                }
            }
        }
    }

    val pullPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.pullFileToUri(remotePath, uri, context.contentResolver) {
                transferStatus = it
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fastbootResult = "Flashing boot image…"
            scope.launch {
                fastbootResult = viewModel.flashFastbootImage("boot", uri, context.contentResolver)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Bridge", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshUsbDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh USB devices")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                ConnectionStatusCard(connectionState, fastbootState, fastbootInfo) {
                    viewModel.disconnect()
                    viewModel.disconnectFastboot()
                }
            }

            if (connectionState is AdbConnectionState.Connected) {
                item {
                    QuickActions(
                        onShell = { navController.navigate("terminal") },
                        onLogcat = { navController.navigate("logcat") },
                        onPackages = { navController.navigate("apps") }
                    )
                }
                item {
                    FileTransferCard(
                        remotePath = remotePath,
                        onRemotePathChange = { remotePath = it },
                        onBrowse = {
                            viewModel.listRemotePath(remotePath)
                            transferStatus = "Refreshing $remotePath"
                        },
                        onSelectPush = { pushPicker.launch("*/*") },
                        onPush = {
                            val file = localPushFile
                            if (file == null) {
                                transferStatus = "Choose a local file first."
                            } else {
                                viewModel.pushFile(file, remotePath) { transferStatus = it }
                            }
                        },
                        onPull = { pullPicker.launch("*/*") },
                        entries = remoteEntries,
                        transferStatus = transferStatus
                    )
                }
            } else {
                item {
                    ConnectionMethodCard(
                        selectedMethod = selectedMethod,
                        onMethodChange = { selectedMethod = it },
                        devices = devices,
                        permissions = permissions,
                        viewModel = viewModel,
                        wirelessHost = wirelessHost,
                        onWirelessHostChange = { wirelessHost = it },
                        wirelessPort = wirelessPort,
                        onWirelessPortChange = { wirelessPort = it }
                    )
                }
            }

            if (fastbootState == com.example.feature.fastboot.FastbootState.CONNECTED) {
                item {
                    FastbootPanel(
                        onCommand = { command ->
                            scope.launch { fastbootResult = viewModel.executeFastbootCommand(command) }
                        },
                        onFlash = { imagePicker.launch("*/*") },
                        resultText = fastbootResult
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    state: AdbConnectionState,
    fastbootState: com.example.feature.fastboot.FastbootState,
    fastbootInfo: com.example.feature.fastboot.FastbootDeviceInfo?,
    onDisconnect: () -> Unit
) {
    val connected = state is AdbConnectionState.Connected ||
        fastbootState == com.example.feature.fastboot.FastbootState.CONNECTED
    val accent = if (connected) Color(0xFF43E0B2) else MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (connected) Color(0xFF102E2B) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (connected) Icons.Default.CheckCircle else Icons.Default.Cable,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (connected) "Bridge online" else "No device connected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            fastbootInfo != null -> "${fastbootInfo.product} · Fastboot"
                            state is AdbConnectionState.Connected -> "${state.device.model} · USB ADB"
                            state is AdbConnectionState.Connecting -> "Opening USB transport…"
                            state is AdbConnectionState.Authorizing -> "Waiting for USB debugging approval…"
                            state is AdbConnectionState.Error -> state.message
                            else -> "Connect a phone over USB OTG to begin"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (connected) {
                    OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                }
            }
            if (state is AdbConnectionState.Connected) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "${state.device.manufacturer} ${state.device.model} · Android ${state.device.androidVersion} · API ${state.device.apiLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onShell: () -> Unit, onLogcat: () -> Unit, onPackages: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("COMMAND CENTER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Shell", Icons.Default.Terminal, onShell, Modifier.weight(1f))
            ActionButton("Logcat", Icons.Default.Terminal, onLogcat, Modifier.weight(1f))
            ActionButton("Apps", Icons.Default.Folder, onPackages, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FileTransferCard(
    remotePath: String,
    onRemotePathChange: (String) -> Unit,
    onBrowse: () -> Unit,
    onSelectPush: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    entries: List<AdbRemoteEntry>,
    transferStatus: String
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("FILE OPERATIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Move files without leaving the phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = remotePath,
                onValueChange = onRemotePathChange,
                label = { Text("Remote path") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Browse")
                }
                OutlinedButton(onClick = onSelectPush, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Choose file")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onPush, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Push")
                }
                Button(onClick = onPull, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Pull")
                }
            }
            if (transferStatus.isNotBlank()) {
                Text(transferStatus, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            entries.take(8).forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(entry.name, modifier = Modifier.weight(1f))
                    Text(if (entry.isDirectory) "DIR" else formatBytes(entry.size), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ConnectionMethodCard(
    selectedMethod: ConnectionType,
    onMethodChange: (ConnectionType) -> Unit,
    devices: List<android.hardware.usb.UsbDevice>,
    permissions: Map<String, Boolean>,
    viewModel: DeviceBridgeViewModel,
    wirelessHost: String,
    onWirelessHostChange: (String) -> Unit,
    wirelessPort: String,
    onWirelessPortChange: (String) -> Unit
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CONNECT A DEVICE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Choose a bridge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(ConnectionType.USB, ConnectionType.WIRELESS, ConnectionType.LOCAL, ConnectionType.ROOT).forEach { method ->
                    if (method == selectedMethod) {
                        Button(onClick = { onMethodChange(method) }, modifier = Modifier.weight(1f)) { Text(method.label) }
                    } else {
                        OutlinedButton(onClick = { onMethodChange(method) }, modifier = Modifier.weight(1f)) { Text(method.label) }
                    }
                }
            }
            when (selectedMethod) {
                ConnectionType.USB -> {
                    Text("USB host mode supports ADB and Fastboot interfaces. Unlock the phone and accept the debugging prompt.", style = MaterialTheme.typography.bodySmall)
                    if (devices.isEmpty()) {
                        Text("No compatible USB device detected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        devices.forEach { device ->
                            val protocol = viewModel.getUsbProtocol(device)
                            val authorized = permissions[device.deviceName] == true
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(device.productName ?: device.deviceName, fontWeight = FontWeight.SemiBold)
                                    Text("VID ${device.vendorId} · PID ${device.productId} · $protocol", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (authorized) {
                                        Button(
                                            onClick = {
                                                if (protocol == "FASTBOOT") viewModel.connectFastboot(device)
                                                else viewModel.connectUsb(device)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Connect $protocol") }
                                    } else {
                                        OutlinedButton(onClick = { viewModel.requestUsbPermission(device) }, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.Warning, contentDescription = null)
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Grant USB access")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ConnectionType.LOCAL -> Button(onClick = viewModel::connectLocal, modifier = Modifier.fillMaxWidth()) { Text("Start local shell") }
                ConnectionType.ROOT -> Button(onClick = viewModel::connectRoot, modifier = Modifier.fillMaxWidth()) { Text("Request root shell") }
                ConnectionType.WIRELESS -> {
                    Text("Legacy TCP ADB works when the phone exposes a reachable adb port. Android 11+ pairing-code flow is handled by the system's Wireless debugging settings.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = wirelessHost,
                        onValueChange = onWirelessHostChange,
                        label = { Text("Device IP address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wirelessPort,
                        onValueChange = onWirelessPortChange,
                        label = { Text("ADB port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            viewModel.connectWireless(
                                wirelessHost,
                                wirelessPort.toIntOrNull() ?: 5555,
                                "",
                                0
                            )
                        },
                        enabled = wirelessHost.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Connect over TCP ADB") }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun FastbootPanel(
    onCommand: (String) -> Unit,
    onFlash: () -> Unit,
    resultText: String
) {
    var command by remember { mutableStateOf("getvar all") }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    val isDestructive = { value: String ->
        val normalized = value.lowercase()
        normalized.contains("unlock") ||
            normalized.contains("lock") ||
            normalized.contains("erase") ||
            normalized.contains("flash")
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Confirm bootloader operation") },
            text = {
                Text(
                    "This can erase data, change the bootloader state, or make the device unbootable. Verify the connected device and command before continuing:\n\n$action"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        if (action == "Flash boot image") onFlash() else onCommand(action)
                    }
                ) { Text("Run") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
            }
        )
    }

    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("FASTBOOT CONSOLE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Bootloader operations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Unlocking, locking, erasing, and flashing can permanently affect the phone. Verify every command before running it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            OutlinedTextField(command, { command = it }, label = { Text("Fastboot command") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = { if (isDestructive(command)) pendingAction = command else onCommand(command) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Execute command") }
            OutlinedButton(onClick = { pendingAction = "Flash boot image" }, modifier = Modifier.fillMaxWidth()) {
                Text("Flash boot image")
            }
            if (resultText.isNotBlank()) Text(resultText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return "${bytes / (1024 * 1024)} MB"
}