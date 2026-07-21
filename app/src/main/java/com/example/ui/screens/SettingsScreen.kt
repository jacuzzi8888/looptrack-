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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var units by remember { mutableStateOf("Metric") }
    var placement by remember { mutableStateOf("Pocket") }
    var battery by remember { mutableStateOf("Balanced") }
    var cuesEnabled by remember { mutableStateOf(true) }
    var gpsOptional by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LoopCard {
                    SectionTitle("Preferences")
                    ChoiceRow("Units", listOf("Metric", "Imperial"), units) { units = it }
                    Spacer(modifier = Modifier.height(12.dp))
                    ChoiceRow("Phone placement", listOf("Pocket", "Hand", "Armband"), placement) { placement = it }
                    Spacer(modifier = Modifier.height(12.dp))
                    ToggleRow("Audio cues", "Lap alerts and pace updates", cuesEnabled) { cuesEnabled = it }
                }
            }
            item {
                LoopCard {
                    SectionTitle("Tracking")
                    ChoiceRow("Battery profile", listOf("Accuracy", "Balanced", "Endurance"), battery) { battery = it }
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Step source", "Phone sensors", true)
                    Spacer(modifier = Modifier.height(12.dp))
                    ToggleRow("GPS optional", "Use GPS only for map-assisted sessions", gpsOptional) { gpsOptional = it }
                }
            }
            item {
                LoopCard {
                    SectionTitle("Privacy and data")
                    StatusRow("Privacy", "Local processing only", true)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Export data", "CSV / JSON / GPX", true)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Delete all data", "Manual confirmation required", false)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ChoiceRow(label: String, choices: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { choice ->
                FilterChip(selected = selected == choice, onClick = { onSelected(choice) }, label = { Text(choice) })
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, supporting: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
