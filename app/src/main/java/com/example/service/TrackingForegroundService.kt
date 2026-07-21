package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.TrackingCheckpointStore
import com.example.engine.TrackingEngine
import com.example.sensors.LocationGateway
import com.example.sensors.SensorGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TrackingForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorGateway: SensorGateway
    private lateinit var locationGateway: LocationGateway
    private lateinit var checkpointStore: TrackingCheckpointStore

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorGateway = SensorGateway(this)
        locationGateway = LocationGateway(this)
        checkpointStore = TrackingCheckpointStore(this)

        val checkpoint = checkpointStore.load()
        if (checkpoint != null && TrackingEngine.state.value.startTimeMillis == 0L) {
            TrackingEngine.restoreFromCheckpoint(checkpoint.state, checkpoint.laps)
        }
        
        try {
            sensorGateway.getSteps()
                .onEach { reading ->
                    TrackingEngine.updateStepsFromSensor(reading.totalSteps, reading.source.name)
                    saveCheckpoint()
                }
                .launchIn(serviceScope)
        } catch (e: Exception) {
            e.printStackTrace()
        }
            
        // Collect location updates if permission is granted
        try {
            locationGateway.getLocationUpdates()
                .onEach { location ->
                    TrackingEngine.updateLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        accuracy = location.accuracy,
                        speed = location.speed,
                        bearing = location.bearing
                    )
                }
                .launchIn(serviceScope)
        } catch (e: Exception) {
            e.printStackTrace()
        }
            
        serviceScope.launch {
            while (true) {
                delay(1000)
                if (TrackingEngine.state.value.startTimeMillis != 0L) {
                    try {
                        saveCheckpoint()
                        updateNotification()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_PAUSE) {
            TrackingEngine.togglePause()
            saveCheckpoint()
            updateNotification()
            return START_STICKY
        }

        if (action == ACTION_LAP) {
            TrackingEngine.markLap()
            saveCheckpoint()
            updateNotification()
            return START_STICKY
        }

        if (action == ACTION_STOP) {
            val (finalState, lapsAndLocations) = TrackingEngine.stopTracking()
            val laps = lapsAndLocations.first
            val locations = lapsAndLocations.second
            saveCheckpoint()
            
            // Save to database
            val repository = (application as com.example.LoopTrackApp).sessionRepository
            serviceScope.launch {
                val sessionId = repository.saveSession(
                    com.example.data.Session(
                        loopId = finalState.loopId,
                        mode = finalState.mode,
                        state = "COMPLETED",
                        startElapsedTime = 0L,
                        endElapsedTime = finalState.elapsedSeconds,
                        pausedDuration = finalState.pausedSeconds,
                        totalSteps = finalState.steps,
                        distanceMetres = finalState.distanceMetres,
                        confidence = "HIGH",
                        distanceSource = if (finalState.loopId != null) "CALIBRATED_LOOP" else "FREE_TRACK",
                        startTimeMillis = finalState.startTimeMillis,
                        endTimeMillis = finalState.endTimeMillis
                    )
                )
                
                laps.forEach { lap ->
                    repository.saveLap(
                        com.example.data.Lap(
                            sessionId = sessionId,
                            index = lap.index,
                            startElapsedTime = lap.startElapsedTime,
                            endElapsedTime = lap.endElapsedTime,
                            steps = lap.steps,
                            duration = lap.duration,
                            distance = lap.distance,
                            source = "MANUAL",
                            confidence = "HIGH"
                        )
                    )
                }
                
                locations.forEach { location ->
                    repository.saveLocationSample(
                        location.copy(sessionId = sessionId)
                    )
                }
                
                checkpointStore.clear()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_CANCEL) {
            TrackingEngine.stopTracking()
            checkpointStore.clear()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(1, createNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for missing permissions
            startForeground(1, createNotification())
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_channel",
                "Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, createNotification())
    }

    private fun createNotification(): Notification {
        val state = TrackingEngine.state.value
        val text = "Time: ${formatTime(state.elapsedSeconds)} | Laps: ${state.laps} | Steps: ${state.steps}"
        
        return NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("LoopTrack Active - ${state.mode}")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(R.mipmap.ic_launcher, if (state.isActive) "Pause" else "Resume", actionIntent(ACTION_PAUSE, 10))
            .addAction(R.mipmap.ic_launcher, "Lap", actionIntent(ACTION_LAP, 11))
            .addAction(R.mipmap.ic_launcher, "Stop", actionIntent(ACTION_STOP, 12))
            .build()
    }

    private fun saveCheckpoint() {
        val (state, laps) = TrackingEngine.checkpoint()
        if (state.startTimeMillis != 0L) {
            checkpointStore.save(state, laps)
        }
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TrackingForegroundService::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(this, requestCode, intent, flags)
    }
    
    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    companion object {
        const val ACTION_PAUSE = "com.example.service.action.PAUSE"
        const val ACTION_LAP = "com.example.service.action.LAP"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_CANCEL = "com.example.service.action.CANCEL"
    }
}
