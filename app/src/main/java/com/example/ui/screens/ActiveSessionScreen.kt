package com.example.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.LoopTrackApp
import com.example.engine.TrackingEngine
import com.example.service.TrackingForegroundService

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
    
    var loopName by remember { mutableStateOf("Free Track") }
    var loopDistance by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        if (loopId != null) {
            val profile = repository.getLoopProfileById(loopId)
            if (profile != null) {
                loopName = profile.name
                loopDistance = profile.distanceMetres
            }
        }
        
        // Removed auto-start here. User must click START.
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(state.mode, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(loopName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            val phaseColor = if (state.phase == "VALIDATION") Color(0xFFFFB300) else Color(0xFF00E676)
            Text(state.phase, style = MaterialTheme.typography.labelMedium, color = phaseColor)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Primary Metric
        Text(
            text = formatTime(state.elapsedSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text("ELAPSED TIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val progress = (state.elapsedSeconds % 60) / 60f 
        
        // Schematic Loop Visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val progressColor = MaterialTheme.colorScheme.primary
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val rectSize = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
                val cornerRadius = rectSize.height / 2
                
                // Track Background
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(strokeWidth, strokeWidth),
                    size = rectSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = strokeWidth)
                )
                
                // Track Progress
                drawArc(
                    color = progressColor,
                    startAngle = 90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth, strokeWidth),
                    size = Size(cornerRadius * 2, cornerRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            if (state.phase == "VALIDATION") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.laps} / ${state.validationLapsRequired} LAPS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("VALIDATION PHASE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("${state.laps} LAPS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricItem("STEPS", state.steps.toString())
            
            val totalDist = if (loopId != null && loopDistance > 0) {
                // Calibrated distance calculation
                (state.laps * loopDistance) + (progress * loopDistance)
            } else {
                state.distanceMetres
            }
            
            MetricItem("DISTANCE", String.format("%.2f km", totalDist / 1000f))
            
            val pace = if (totalDist > 0) (state.elapsedSeconds / (totalDist / 1000f)).toLong() else 0L
            MetricItem("PACE", if (pace > 0) "${formatTime(pace)}/km" else "--:--")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (laps.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                Text("LAPS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                laps.takeLast(5).reversed().forEach { lap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lap ${lap.index}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTime(lap.duration), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${lap.steps} steps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Controls
        if (!state.isActive && state.elapsedSeconds == 0L) {
            Button(
                onClick = {
                    TrackingEngine.startTracking(mode, loopId)
                    try {
                        val intent = Intent(context, TrackingForegroundService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            val intent = Intent(context, TrackingForegroundService::class.java)
                            context.startService(intent)
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("START SESSION", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { TrackingEngine.togglePause() },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Icon(if (state.isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Toggle", modifier = Modifier.size(32.dp))
                }
                
                Button(
                    onClick = { TrackingEngine.markLap() },
                    modifier = Modifier.size(100.dp, 64.dp),
                    shape = RoundedCornerShape(32.dp),
                    enabled = state.isActive
                ) {
                    Text("LAP")
                }
                
                FilledIconButton(
                    onClick = {
                        try {
                            val intent = Intent(context, TrackingForegroundService::class.java)
                            intent.action = "STOP"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            try {
                                val intent = Intent(context, TrackingForegroundService::class.java)
                                intent.action = "STOP"
                                context.startService(intent)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                        onEndSession()
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
