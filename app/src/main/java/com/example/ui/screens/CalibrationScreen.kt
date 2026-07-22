package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.LoopTrackApp
import com.example.data.CalibrationLap
import com.example.data.LoopProfile
import com.example.engine.CalibrationEngine
import com.example.engine.CalibrationLapInput
import com.example.engine.TrackingEngine
import com.example.service.TrackingForegroundService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = (context.applicationContext as LoopTrackApp).sessionRepository
    val scope = rememberCoroutineScope()
    val trackingState by TrackingEngine.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("WALK") }
    var started by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var lastLapElapsed by remember { mutableLongStateOf(0L) }
    var lastLapSteps by remember { mutableStateOf(0) }
    val calibrationLaps = remember { mutableStateListOf<CalibrationLapInput>() }
    val result = CalibrationEngine.evaluate(calibrationLaps)
    val knownDistance = distanceText.toFloatOrNull() ?: 0f
    val canStart = !started && name.isNotBlank() && knownDistance > 0f
    val canSave = started && calibrationLaps.size >= CalibrationEngine.REQUIRED_LAPS && !saving

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibrate Loop") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().loopTrackBackground().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LoopCard {
                    Text("Known-distance calibration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Walk or run the loop steadily and tap Mark Lap each time you pass your start point.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, enabled = !started, label = { Text("Loop name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(value = distanceText, onValueChange = { distanceText = it }, enabled = !started, label = { Text("Loop distance in meters") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "WALK", onClick = { mode = "WALK" }, label = { Text("Walk") }, enabled = !started)
                        FilterChip(selected = mode == "RUN", onClick = { mode = "RUN" }, label = { Text("Run") }, enabled = !started)
                    }
                }
            }
            item {
                LoopCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${calibrationLaps.size} / ${CalibrationEngine.REQUIRED_LAPS}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                            Text("laps completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ConfidenceBadge(result.distanceConfidence)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchematicLoop(progress = calibrationLaps.size / CalibrationEngine.REQUIRED_LAPS.toFloat(), modifier = Modifier.fillMaxWidth().height(128.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricTile("Elapsed", loopTrackTime(trackingState.elapsedSeconds), modifier = Modifier.weight(1f))
                        MetricTile("Steps", trackingState.steps.toString(), modifier = Modifier.weight(1f))
                        MetricTile("Avg lap", "${result.averageDurationSeconds.toInt()}s", modifier = Modifier.weight(1f))
                    }
                    result.warning?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!started) {
                        Button(onClick = {
                            TrackingEngine.startTracking(mode, null)
                            startTrackingService(context)
                            started = true
                            lastLapElapsed = 0L
                            lastLapSteps = 0
                            calibrationLaps.clear()
                        }, enabled = canStart, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                            Text("Start Calibration")
                        }
                    } else {
                        Button(
                            onClick = {
                                val lapIndex = calibrationLaps.size + 1
                                val lapDuration = (trackingState.elapsedSeconds - lastLapElapsed).coerceAtLeast(0L)
                                val lapSteps = (trackingState.steps - lastLapSteps).coerceAtLeast(0)
                                calibrationLaps.add(CalibrationLapInput(lapIndex, lapSteps, lapDuration))
                                lastLapElapsed = trackingState.elapsedSeconds
                                lastLapSteps = trackingState.steps
                                TrackingEngine.markLap()
                            },
                            enabled = calibrationLaps.size < CalibrationEngine.REQUIRED_LAPS,
                            modifier = Modifier.fillMaxWidth().height(58.dp)
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Mark Lap")
                        }
                    }
                    TextButton(onClick = {
                        stopTrackingService(context)
                        onBack()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel Calibration")
                    }
                }
            }
            if (calibrationLaps.isNotEmpty()) {
                items(calibrationLaps) { lap ->
                    LoopCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lap ${lap.index}", style = MaterialTheme.typography.titleMedium)
                            Text("${lap.steps} steps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(loopTrackTime(lap.durationSeconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        saving = true
                        val savedAt = System.currentTimeMillis()
                        val profile = LoopProfile(
                            name = name.trim(),
                            distanceMetres = knownDistance,
                            distanceSource = "KNOWN",
                            mode = mode,
                            distanceConfidence = result.distanceConfidence,
                            calibrationLapCount = result.acceptedLapCount,
                            averageStepsPerLap = result.averageStepsPerLap,
                            averageDurationSeconds = result.averageDurationSeconds,
                            createdAt = savedAt,
                            updatedAt = savedAt
                        )
                        val lapRows = calibrationLaps.map { lap ->
                            CalibrationLap(0, 0, lap.index, mode, lap.steps, lap.durationSeconds, knownDistance, true, savedAt)
                        }
                        scope.launch {
                            repository.saveLoopProfileWithCalibration(profile, lapRows)
                            TrackingEngine.stopTracking()
                            stopTrackingService(context)
                            saving = false
                            onBack()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Loop Profile")
                }
            }
        }
    }
}

private fun startTrackingService(context: Context) {
    val intent = Intent(context, TrackingForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}

private fun stopTrackingService(context: Context) {
    val intent = Intent(context, TrackingForegroundService::class.java).apply {
        action = TrackingForegroundService.ACTION_CANCEL
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}
