package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.LoopTrackApp
import com.example.data.Lap
import com.example.data.LocationSample
import com.example.data.Session
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = (context.applicationContext as LoopTrackApp).sessionRepository
    var session by remember { mutableStateOf<Session?>(null) }
    var laps by remember { mutableStateOf<List<Lap>>(emptyList()) }
    var locations by remember { mutableStateOf<List<LocationSample>>(emptyList()) }

    LaunchedEffect(sessionId) {
        session = repository.getSessionById(sessionId)
        launch { repository.getLapsForSession(sessionId).collect { laps = it } }
        launch { repository.getLocationsForSession(sessionId).collect { locations = it } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    session?.let {
                        IconButton(onClick = { exportGpx(context, it, locations) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Export")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val current = session
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().loopTrackBackground().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LoopCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(current.mode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(current.distanceSource, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ConfidenceBadge(current.confidence)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SchematicLoop(progress = 1f, modifier = Modifier.fillMaxWidth().height(96.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricTile("Duration", loopTrackTime(current.endElapsedTime ?: 0L), modifier = Modifier.weight(1f))
                        MetricTile("Distance", String.format("%.2f km", current.distanceMetres / 1000f), modifier = Modifier.weight(1f))
                        MetricTile("Laps", laps.size.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val pace = if (current.distanceMetres > 0f) ((current.endElapsedTime ?: 0L) / (current.distanceMetres / 1000f)).toLong() else 0L
                        MetricTile("Steps", current.totalSteps.toString(), modifier = Modifier.weight(1f))
                        MetricTile("Avg Pace", if (pace > 0L) "${loopTrackTime(pace)}/km" else "--:--", modifier = Modifier.weight(1f))
                        MetricTile("Paused", loopTrackTime(current.pausedDuration), modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                LoopCard {
                    Text("Source and uncertainty", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusRow("Distance source", current.distanceSource, current.distanceSource == "CALIBRATED_LOOP")
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRow("Location samples", locations.size.toString(), locations.isNotEmpty())
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRow("Corrections", "Manual audit coming next", true)
                }
            }
            if (laps.isNotEmpty()) {
                item { Text("Lap splits", style = MaterialTheme.typography.titleLarge) }
                items(laps) { lap ->
                    LoopCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lap ${lap.index}", fontWeight = FontWeight.SemiBold)
                            Text(loopTrackTime(lap.duration), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${lap.steps} steps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { exportGpx(context, current, locations) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Export")
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

private fun exportGpx(context: Context, session: Session, locations: List<LocationSample>) {
    val gpxContent = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"LoopTrack\">\n")
        append("  <trk>\n")
        append("    <name>${session.mode} Session</name>\n")
        append("    <trkseg>\n")
        for (loc in locations) {
            append("      <trkpt lat=\"${loc.latitude}\" lon=\"${loc.longitude}\"></trkpt>\n")
        }
        append("    </trkseg>\n")
        append("  </trk>\n")
        append("</gpx>")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gpx+xml"
        putExtra(Intent.EXTRA_SUBJECT, "LoopTrack GPX")
        putExtra(Intent.EXTRA_TEXT, gpxContent)
    }
    context.startActivity(Intent.createChooser(intent, "Export GPX"))
}
