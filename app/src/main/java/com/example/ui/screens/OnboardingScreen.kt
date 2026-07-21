package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sensors.SensorGateway
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val capabilities = remember { SensorGateway(context).getCapabilities() }
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LoopTrack", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Repeated-loop walking and running, built around calibrated laps instead of noisy indoor GPS.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                LoopCard {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Let's check your device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    StatusRow("Step counter", if (capabilities.hasStepCounter) "Ready" else "Missing", capabilities.hasStepCounter)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Step detector", if (capabilities.hasStepDetector) "Available" else "Fallback off", capabilities.hasStepDetector)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Motion sensors", if (capabilities.hasAccelerometer || capabilities.hasGyroscope) "Ready" else "Limited", capabilities.hasAccelerometer || capabilities.hasGyroscope)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("GPS", if (capabilities.hasGps) "Optional" else "Unavailable", capabilities.hasGps)
                }
            }
            item {
                LoopCard {
                    Text("Permissions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Activity recognition powers step tracking. Location remains optional and is only needed for map-assisted sessions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (permissions.isNotEmpty() && !permissionState.allPermissionsGranted) {
                                permissionState.launchMultiplePermissionRequest()
                            } else {
                                onContinue()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (permissions.isNotEmpty() && !permissionState.allPermissionsGranted) "Grant Permissions" else "Continue")
                    }
                }
            }
            item {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
