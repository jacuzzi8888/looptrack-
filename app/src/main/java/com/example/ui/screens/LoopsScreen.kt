package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.LoopTrackApp
import com.example.data.LoopProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopsScreen(onBack: () -> Unit, onStartCalibration: () -> Unit) {
    val context = LocalContext.current
    val repository = (context.applicationContext as LoopTrackApp).sessionRepository
    val profiles by repository.getAllLoopProfiles().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loops") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartCalibration) {
                Icon(Icons.Filled.Add, contentDescription = "Calibrate loop")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Calibrated profiles", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("Each loop keeps its mode, confidence, and five-lap calibration stats.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (profiles.isEmpty()) {
                item {
                    LoopCard {
                        Text("No calibrated loops yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to mark five laps and save your first measured loop profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(profiles) { profile ->
                LoopProfileItem(profile)
            }
        }
    }
}

@Composable
fun LoopProfileItem(profile: LoopProfile) {
    LoopCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(profile.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Text("${profile.mode} / ${profile.distanceSource}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ConfidenceBadge(profile.distanceConfidence)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SchematicLoop(progress = 0.82f, modifier = Modifier.fillMaxWidth().height(84.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricTile("Distance", "${profile.distanceMetres} m", modifier = Modifier.weight(1f))
            MetricTile("Cal laps", profile.calibrationLapCount.toString(), modifier = Modifier.weight(1f))
            MetricTile("Avg lap", "${profile.averageDurationSeconds.toInt()}s", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Avg ${profile.averageStepsPerLap.toInt()} steps per lap", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}
