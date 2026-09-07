package com.example.feature.display

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalConfiguration
import com.example.feature.device.InfoCard
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen(navController: NavController) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    
    var isTestingTouch by remember { mutableStateOf(false) }

    if (isTestingTouch) {
        TouchTestScreen(onClose = { isTestingTouch = false })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Display & Touch") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoCard("Resolution (dp)", "${screenWidth}x${screenHeight}")
                InfoCard("Density DPI", configuration.densityDpi.toString())
                InfoCard("Font Scale", configuration.fontScale.toString())
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { isTestingTouch = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Touch & Draw Test")
                }
                
                Text(
                    text = "Professional display calibration requires specialized hardware.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TouchTestScreen(onClose: () -> Unit) {
    var lines by remember { mutableStateOf(listOf<Line>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        lines = lines + Line(start = offset, end = offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (lines.isNotEmpty()) {
                            val lastLine = lines.last()
                            val newLine = lastLine.copy(end = lastLine.end + dragAmount)
                            lines = lines.dropLast(1) + newLine + Line(start = newLine.end, end = newLine.end)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            lines.forEach { line ->
                drawLine(
                    color = Color.Green,
                    start = line.start,
                    end = line.end,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }
        
        Button(
            onClick = onClose,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Text("Close Test")
        }
    }
}

data class Line(
    val start: Offset,
    val end: Offset
)
