package com.example.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationPoint(val lat: Double, val lng: Double)

data class TrackingState(
    val isActive: Boolean = false,
    val phase: String = "ACTIVE", // "VALIDATION" or "ACTIVE"
    val mode: String = "WALK",
    val loopId: Int? = null,
    val elapsedSeconds: Long = 0,
    val pausedSeconds: Long = 0,
    val laps: Int = 0,
    val validationLapsRequired: Int = 3,
    val steps: Int = 0,
    val distanceMetres: Float = 0f,
    val initialStepCount: Int = -1,
    val lastLapElapsedSeconds: Long = 0,
    val lastLapSteps: Int = 0,
    val currentLocation: LocationPoint? = null,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val stepSource: String = "UNAVAILABLE"
)

data class LapRecord(
    val index: Int,
    val startElapsedTime: Long,
    val endElapsedTime: Long,
    val steps: Int,
    val duration: Long,
    val distance: Float = 0f
)

object TrackingEngine {
    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(TrackingState())
    val state = _state.asStateFlow()
    
    private val _lapRecordsFlow = MutableStateFlow<List<LapRecord>>(emptyList())
    val lapRecordsFlow = _lapRecordsFlow.asStateFlow()
    
    private val lapRecords = mutableListOf<LapRecord>()
    private val locationSamples = mutableListOf<com.example.data.LocationSample>()

    private var startTimeRealtime: Long = 0L
    private var accumulatedPausedTime: Long = 0L
    private var pauseStartedTimeRealtime: Long = 0L
    private var sessionStartWallClock: Long = 0L
    private var sessionEndWallClock: Long = 0L

    private var activeStepsBeforeCurrentSegment: Int = 0
    private var segmentStartSensorSteps: Int = -1
    private var lastReceivedSensorSteps: Int = -1

    fun startTracking(mode: String, loopId: Int? = null) {
        lapRecords.clear()
        _lapRecordsFlow.value = emptyList()
        locationSamples.clear()

        startTimeRealtime = android.os.SystemClock.elapsedRealtime()
        accumulatedPausedTime = 0L
        pauseStartedTimeRealtime = 0L
        sessionStartWallClock = System.currentTimeMillis()
        sessionEndWallClock = 0L

        activeStepsBeforeCurrentSegment = 0
        segmentStartSensorSteps = -1
        lastReceivedSensorSteps = -1

        _state.value = TrackingState(
            isActive = true,
            phase = if (loopId != null) "VALIDATION" else "ACTIVE",
            mode = mode,
            loopId = loopId,
            startTimeMillis = sessionStartWallClock
        )

        startTimer()
    }

    fun restoreFromCheckpoint(restoredState: TrackingState, restoredLapRecords: List<LapRecord>) {
        stopTimer()

        val now = android.os.SystemClock.elapsedRealtime()
        val restoredPausedMs = restoredState.pausedSeconds * 1000L
        startTimeRealtime = now - (restoredState.elapsedSeconds * 1000L) - restoredPausedMs
        accumulatedPausedTime = restoredPausedMs
        pauseStartedTimeRealtime = if (restoredState.isActive) 0L else now
        sessionStartWallClock = restoredState.startTimeMillis
        sessionEndWallClock = restoredState.endTimeMillis

        activeStepsBeforeCurrentSegment = restoredState.steps
        segmentStartSensorSteps = -1
        lastReceivedSensorSteps = -1

        lapRecords.clear()
        lapRecords.addAll(restoredLapRecords)
        _lapRecordsFlow.value = lapRecords.toList()
        locationSamples.clear()
        _state.value = restoredState

        if (restoredState.isActive) {
            startTimer()
        }
    }

    fun checkpoint(): Pair<TrackingState, List<LapRecord>> {
        return Pair(_state.value, lapRecords.toList())
    }

    fun togglePause() {
        val wasActive = _state.value.isActive
        val now = android.os.SystemClock.elapsedRealtime()

        if (wasActive) {
            // Transition ACTIVE -> PAUSED
            pauseStartedTimeRealtime = now
            if (lastReceivedSensorSteps != -1 && segmentStartSensorSteps != -1) {
                activeStepsBeforeCurrentSegment += (lastReceivedSensorSteps - segmentStartSensorSteps)
            }
            segmentStartSensorSteps = -1

            _state.update { it.copy(isActive = false) }
            stopTimer()
        } else {
            // Transition PAUSED -> ACTIVE
            val pausedDuration = now - pauseStartedTimeRealtime
            accumulatedPausedTime += pausedDuration
            pauseStartedTimeRealtime = 0L

            segmentStartSensorSteps = lastReceivedSensorSteps

            _state.update { it.copy(isActive = true) }
            startTimer()
        }
        tick()
    }

    fun stopTracking(): Pair<TrackingState, Pair<List<LapRecord>, List<com.example.data.LocationSample>>> {
        stopTimer()
        val now = android.os.SystemClock.elapsedRealtime()

        if (_state.value.isActive) {
            pauseStartedTimeRealtime = now
            if (lastReceivedSensorSteps != -1 && segmentStartSensorSteps != -1) {
                activeStepsBeforeCurrentSegment += (lastReceivedSensorSteps - segmentStartSensorSteps)
            }
        } else if (pauseStartedTimeRealtime != 0L) {
            val pausedDuration = now - pauseStartedTimeRealtime
            accumulatedPausedTime += pausedDuration
            pauseStartedTimeRealtime = now
        }

        segmentStartSensorSteps = -1
        sessionEndWallClock = System.currentTimeMillis()

        _state.update { it.copy(isActive = false) }
        tick()

        _state.update {
            it.copy(
                endTimeMillis = sessionEndWallClock
            )
        }

        val finalState = _state.value
        return Pair(finalState, Pair(lapRecords.toList(), locationSamples.toList()))
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = engineScope.launch {
            while (true) {
                delay(1000)
                if (_state.value.isActive) {
                    tick()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun markLap() {
        val currentState = state.value
        if (!currentState.isActive) return
        
        val lapDuration = currentState.elapsedSeconds - currentState.lastLapElapsedSeconds
        val lapSteps = currentState.steps - currentState.lastLapSteps
        
        lapRecords.add(
            LapRecord(
                index = currentState.laps + 1,
                startElapsedTime = currentState.lastLapElapsedSeconds,
                endElapsedTime = currentState.elapsedSeconds,
                steps = lapSteps,
                duration = lapDuration,
                distance = if (currentState.mode == "WALK") lapSteps * 0.75f else lapSteps * 1.0f
            )
        )
        _lapRecordsFlow.value = lapRecords.toList()
        
        _state.update { 
            val newLaps = it.laps + 1
            val newPhase = if (it.phase == "VALIDATION" && newLaps >= it.validationLapsRequired) "ACTIVE" else it.phase
            it.copy(
                laps = newLaps,
                lastLapElapsedSeconds = it.elapsedSeconds,
                lastLapSteps = it.steps,
                phase = newPhase
            ) 
        }
    }

    fun correctLapCount(newLapCount: Int) {
        val correctedCount = newLapCount.coerceAtLeast(0)
        _state.update {
            it.copy(
                laps = correctedCount,
                lastLapElapsedSeconds = if (correctedCount == 0) 0L else it.lastLapElapsedSeconds,
                lastLapSteps = if (correctedCount == 0) 0 else it.lastLapSteps
            )
        }
    }

    fun updateStepsFromSensor(sensorSteps: Int, source: String = "STEP_COUNTER") {
        lastReceivedSensorSteps = sensorSteps
        val currentState = _state.value
        if (!currentState.isActive) {
            _state.update { it.copy(stepSource = source) }
            return
        }

        if (segmentStartSensorSteps == -1) {
            segmentStartSensorSteps = if (source == "STEP_DETECTOR") 0 else sensorSteps
        }

        val activeStepsInSegment = (sensorSteps - segmentStartSensorSteps).coerceAtLeast(0)
        val totalSteps = activeStepsBeforeCurrentSegment + activeStepsInSegment
        val stride = if (currentState.mode == "WALK") 0.75f else 1.0f
        val distance = totalSteps * stride

        _state.update {
            it.copy(
                steps = totalSteps,
                distanceMetres = distance,
                initialStepCount = if (it.initialStepCount == -1) sensorSteps else it.initialStepCount,
                stepSource = source
            )
        }
    }
    
    fun updateLocation(lat: Double, lng: Double, accuracy: Float, speed: Float, bearing: Float) {
        val currentState = state.value
        if (!currentState.isActive) return
        
        locationSamples.add(
            com.example.data.LocationSample(
                sessionId = 0,
                elapsedTime = currentState.elapsedSeconds,
                latitude = lat,
                longitude = lng,
                accuracy = accuracy,
                speed = speed,
                bearing = bearing
            )
        )
        
        _state.update {
            it.copy(currentLocation = LocationPoint(lat, lng))
        }
    }

    fun tick() {
        val now = android.os.SystemClock.elapsedRealtime()
        val currentState = _state.value
        val elapsedMs = if (currentState.isActive) {
            now - startTimeRealtime - accumulatedPausedTime
        } else {
            when {
                startTimeRealtime == 0L -> 0L
                pauseStartedTimeRealtime != 0L -> pauseStartedTimeRealtime - startTimeRealtime - accumulatedPausedTime
                else -> currentState.elapsedSeconds * 1000L
            }
        }
        val elapsedSec = (elapsedMs / 1000).coerceAtLeast(0L)
        
        val pausedMs = if (currentState.isActive) {
            accumulatedPausedTime
        } else {
            if (pauseStartedTimeRealtime == 0L) accumulatedPausedTime else accumulatedPausedTime + (now - pauseStartedTimeRealtime)
        }
        val pausedSec = (pausedMs / 1000).coerceAtLeast(0L)

        _state.update {
            it.copy(
                elapsedSeconds = elapsedSec,
                pausedSeconds = pausedSec
            )
        }
    }
}
