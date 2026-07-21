package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.LoopTrackApp
import com.example.data.LoopProfile
import com.example.sensors.SensorGateway
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartSession: (String, Int?) -> Unit, onNavigateToHistory: () -> Unit, onNavigateToLoops: () -> Unit) {
    val permissions = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    val permissionState = rememberMultiplePermissionsState(permissions)
    
    val context = LocalContext.current
    val repository = (context.applicationContext as LoopTrackApp).sessionRepository
    val capabilities = remember { SensorGateway(context).getCapabilities() }
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val profiles by repository.getAllLoopProfiles().collectAsState(initial = emptyList())
    var selectedProfile by remember { mutableStateOf<LoopProfile?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onNavigateToLoops) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Loops", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Filled.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "LOOPTRACK", 
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (permissionState.allPermissionsGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onStartSession("WALK", selectedProfile?.id) },
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Walk")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Walk")
                    }
                }
                
                Button(
                    onClick = { onStartSession("RUN", selectedProfile?.id) },
                    modifier = Modifier.weight(1f).height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Run")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Run")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Loop Selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedProfile?.name ?: "Free Track",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Active Loop") }
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Free Track") },
                        onClick = { 
                            selectedProfile = null
                            expanded = false
                        }
                    )
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.name) },
                            onClick = { 
                                selectedProfile = profile
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sensor Readiness Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorIndicator(icon = Icons.AutoMirrored.Filled.DirectionsWalk, label = "Steps", ready = capabilities.hasStepCounter || capabilities.hasStepDetector)
                SensorIndicator(icon = Icons.Filled.Sensors, label = "Motion", ready = capabilities.hasAccelerometer || capabilities.hasGyroscope)
                SensorIndicator(icon = Icons.Filled.GpsFixed, label = "GPS", ready = capabilities.hasGps && hasLocationPermission)
                SensorIndicator(icon = Icons.Filled.BatteryFull, label = "Battery", ready = true)
            }
            
        } else {
            Text(
                text = "We need permissions to track your workout.",
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun SensorIndicator(icon: ImageVector, label: String, ready: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall,
            color = if (ready) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
