package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.LoopTrackApp
import com.example.data.LoopProfile
import com.example.engine.TrackingEngine
import com.example.sensors.SensorGateway
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartSession: (String, Int?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLoops: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)
    val repository = (context.applicationContext as LoopTrackApp).sessionRepository
    val profiles by repository.getAllLoopProfiles().collectAsState(initial = emptyList())
    val trackingState by TrackingEngine.state.collectAsState()
    val capabilities = remember { SensorGateway(context).getCapabilities() }
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    var selectedProfile by remember { mutableStateOf<LoopProfile?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .loopTrackBackground()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    BrandLockup(subtitle = "Calibrated loop tracking")
                    Row {
                        IconButton(onClick = onNavigateToLoops) {
                            Icon(Icons.Filled.Route, contentDescription = "Loops", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Filled.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (trackingState.startTimeMillis != 0L && trackingState.endTimeMillis == 0L) {
                item {
                    LoopCard(modifier = Modifier.clickable { onStartSession(trackingState.mode, trackingState.loopId) }) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Resume Session", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("${trackingState.mode} in progress", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(loopTrackTime(trackingState.elapsedSeconds), style = MaterialTheme.typography.headlineSmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SchematicLoop(progress = (trackingState.elapsedSeconds % 60) / 60f, modifier = Modifier.fillMaxWidth().height(90.dp))
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumActionTile(
                        label = "Start Walk",
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        onClick = { onStartSession("WALK", selectedProfile?.id) },
                        modifier = Modifier.weight(1f).height(104.dp),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    PremiumActionTile(
                        label = "Start Run",
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        onClick = { onStartSession("RUN", selectedProfile?.id) },
                        modifier = Modifier.weight(1f).height(104.dp),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                LoopCard {
                    Text("Active loop", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Loop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedProfile?.name ?: "Free Track", style = MaterialTheme.typography.titleMedium)
                            }
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Choose loop", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Free Track") }, onClick = {
                                selectedProfile = null
                                expanded = false
                            })
                            profiles.forEach { profile ->
                                DropdownMenuItem(text = { Text("${profile.name} (${profile.mode})") }, onClick = {
                                    selectedProfile = profile
                                    expanded = false
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedProfile == null, onClick = { selectedProfile = null }, label = { Text("Free Track") })
                        selectedProfile?.let { ConfidenceBadge(it.distanceConfidence) }
                    }
                }
            }

            item {
                LoopCard {
                    Text("Sensor readiness", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Steps", if (capabilities.hasStepCounter) "Counter" else if (capabilities.hasStepDetector) "Detector" else "Missing", capabilities.hasStepCounter || capabilities.hasStepDetector)
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRow("Motion", if (capabilities.hasAccelerometer || capabilities.hasGyroscope) "Ready" else "Limited", capabilities.hasAccelerometer || capabilities.hasGyroscope)
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRow("GPS", if (hasLocationPermission) "Enabled" else "Optional", capabilities.hasGps)
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRow("Battery", "Balanced", true)
                }
            }

            if (!permissionState.allPermissionsGranted && permissions.isNotEmpty()) {
                item {
                    LoopCard {
                        Text("Permissions needed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Grant activity and notification permissions before long tracking sessions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { permissionState.launchMultiplePermissionRequest() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }
        }
    }
}
