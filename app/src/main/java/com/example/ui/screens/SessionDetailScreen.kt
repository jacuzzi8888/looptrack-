package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.LoopTrackApp
import com.example.data.Lap
import com.example.data.Session
import com.example.data.LocationSample
import kotlinx.coroutines.flow.firstOrNull
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
                    if (session != null) {
                        IconButton(onClick = { exportGpx(context, session!!, locations) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Export GPX")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        
        val s = session!!
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val title = s.mode + if (s.loopId != null) " (Calibrated)" else " (Free Track)"
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItemSmall("DURATION", formatTime(s.endElapsedTime ?: 0L))
                            MetricItemSmall("DISTANCE", String.format("%.2f km", s.distanceMetres / 1000f))
                            MetricItemSmall("STEPS", s.totalSteps.toString())
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val pace = if (s.distanceMetres > 0) ((s.endElapsedTime ?: 0L) / (s.distanceMetres / 1000f)).toLong() else 0L
                            MetricItemSmall("AVG PACE", formatTime(pace) + "/km")
                            MetricItemSmall("LAPS", laps.size.toString())
                            MetricItemSmall("SOURCE", s.distanceSource)
                        }
                    }
                }
            }
            
            if (laps.isNotEmpty()) {
                item {
                    Text("Laps", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                }
                
                items(laps) { lap ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lap ${lap.index}", style = MaterialTheme.typography.titleMedium)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatTime(lap.duration), style = MaterialTheme.typography.bodyLarge)
                                Text("${lap.steps} steps", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItemSmall(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun exportGpx(context: Context, session: Session, locations: List<LocationSample>) {
    val gpxContent = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"LoopTrack\">\n")
        append("  <trk>\n")
        append("    <name>${session.mode} Session</name>\n")
        append("    <trkseg>\n")
        for (loc in locations) {
            append("      <trkpt lat=\"${loc.latitude}\" lon=\"${loc.longitude}\">\n")
            append("      </trkpt>\n")
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
