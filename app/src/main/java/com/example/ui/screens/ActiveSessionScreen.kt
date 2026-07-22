package com.example.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.LoopTrackApp
import com.example.data.LoopProfile
import com.example.engine.TrackingEngine
import com.example.service.TrackingForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    mode: String,
    loopId: Int?,
    onEndSession: () -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as LoopTrackApp).sessionRepository
    val state by TrackingEngine.state.collectAsState()
    val laps by TrackingEngine.lapRecordsFlow.collectAsState()
    var profile by remember { mutableStateOf<LoopProfile?>(null) }
    var showCorrection by remember { mutableStateOf(false) }
    var correctedLaps by remember { mutableIntStateOf(0) }

    LaunchedEffect(loopId) {
        profile = loopId?.let { repository.getLoopProfileById(it) }
    }

    val loopName = profile?.name ?: "Free Track"
    val loopDistance = profile?.distanceMetres ?: 0f
    val progress = (state.elapsedSeconds % 60) / 60f
    val totalDistance = if (loopId != null && loopDistance > 0f) {
        (state.laps * loopDistance) + (progress * loopDistance)
    } else {
        state.distanceMetres
    }
    val confidence = profile?.distanceConfidence ?: if (loopId == null) "LOW" else "MEDIUM"
    val source = if (loopId == null) "Estimated / phone sensors" else "${profile?.distanceSource ?: "CALIBRATED"} loop"

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .loopTrackBackground()
                .padding(padding)
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(loopName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${state.mode.ifBlank { mode }} / $source", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.Lock, contentDescription = "Foreground tracking", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LoopCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    MetricTile("Elapsed Time", loopTrackTime(state.elapsedSeconds), modifier = Modifier.weight(1f))
                    MetricTile("Distance", String.format("%.2f km", totalDistance / 1000f), modifier = Modifier.weight(1f), supporting = "km")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricTile("Laps", state.laps.toString(), modifier = Modifier.weight(1f))
                    MetricTile("Steps", state.steps.toString(), modifier = Modifier.weight(1f), supporting = state.stepSource.replace('_', ' '))
                    val pace = if (totalDistance > 0f) (state.elapsedSeconds / (totalDistance / 1000f)).toLong() else 0L
                    MetricTile("Pace", if (pace > 0) "${loopTrackTime(pace)}/km" else "--:--", modifier = Modifier.weight(1f))
                }
            }

            LoopCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Confidence", style = MaterialTheme.typography.titleMedium)
                    ConfidenceBadge(confidence)
                }
                Spacer(modifier = Modifier.height(10.dp))
                SchematicLoop(progress = progress, modifier = Modifier.fillMaxWidth().height(150.dp))
                Text("Tap laps to correct the count. Distance uses confirmed laps for calibrated loops.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }

            LoopCard(modifier = Modifier.clickable {
                correctedLaps = state.laps
                showCorrection = true
            }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Lap correction", style = MaterialTheme.typography.titleMedium)
                        Text("Recorded laps: ${state.laps}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Adjust", color = MaterialTheme.colorScheme.secondary)
                }
            }

            if (laps.isNotEmpty()) {
                LoopCard {
                    Text("Recent laps", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    laps.takeLast(4).reversed().forEach { lap ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lap ${lap.index}")
                            Text(loopTrackTime(lap.duration), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${lap.steps} steps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (!state.isActive && state.elapsedSeconds == 0L) {
                Button(
                    onClick = {
                        TrackingEngine.startTracking(mode, loopId)
                        startTrackingService(context)
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Start Session", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledIconButton(
                        onClick = { sendTrackingAction(context, TrackingForegroundService.ACTION_PAUSE) },
                        modifier = Modifier.weight(1f).height(68.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (state.isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Pause")
                            Text(if (state.isActive) "Pause" else "Resume")
                        }
                    }
                    Button(
                        onClick = { sendTrackingAction(context, TrackingForegroundService.ACTION_LAP) },
                        enabled = state.isActive,
                        modifier = Modifier.weight(1f).height(68.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Flag, contentDescription = null)
                            Text("Lap")
                        }
                    }
                    Button(
                        onClick = {
                            sendTrackingAction(context, TrackingForegroundService.ACTION_STOP)
                            onEndSession()
                        },
                        modifier = Modifier.weight(1f).height(68.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Text("End")
                        }
                    }
                }
            }
        }
    }

    if (showCorrection) {
        ModalBottomSheet(onDismissRequest = { showCorrection = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Lap Correction", style = MaterialTheme.typography.titleLarge)
                Text("Adjust the confirmed lap count. This creates a visible correction in the session flow.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { correctedLaps = (correctedLaps - 1).coerceAtLeast(0) }, modifier = Modifier.size(58.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Remove lap")
                    }
                    Text(correctedLaps.toString(), style = MaterialTheme.typography.displayMedium)
                    OutlinedButton(onClick = { correctedLaps += 1 }, modifier = Modifier.size(58.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Add lap")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showCorrection = false }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        TrackingEngine.correctLapCount(correctedLaps)
                        showCorrection = false
                    }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun startTrackingService(context: android.content.Context) {
    val intent = Intent(context, TrackingForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}

private fun sendTrackingAction(context: android.content.Context, action: String) {
    val intent = Intent(context, TrackingForegroundService::class.java).apply {
        this.action = action
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}
