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
    val laps: Int = 0,
    val validationLapsRequired: Int = 3,
    val steps: Int = 0,
    val distanceMetres: Float = 0f,
    val initialStepCount: Int = -1,
    val lastLapElapsedSeconds: Long = 0,
    val lastLapSteps: Int = 0,
    val currentLocation: LocationPoint? = null
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

    fun startTracking(mode: String, loopId: Int? = null) {
        lapRecords.clear()
        _lapRecordsFlow.value = emptyList()
        locationSamples.clear()
        _state.update { 
            TrackingState(
                isActive = true, 
                phase = if (loopId != null) "VALIDATION" else "ACTIVE",
                mode = mode, 
                loopId = loopId
            ) 
        }
        startTimer()
    }

    fun togglePause() {
        val wasActive = _state.value.isActive
        _state.update { it.copy(isActive = !wasActive) }
        if (!wasActive) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    fun stopTracking(): Pair<TrackingState, Pair<List<LapRecord>, List<com.example.data.LocationSample>>> {
        stopTimer()
        _state.update { it.copy(isActive = false) }
        return Pair(state.value, Pair(lapRecords.toList(), locationSamples.toList()))
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

    fun updateStepsFromSensor(sensorSteps: Int) {
        _state.update { 
            val newSteps = if (it.initialStepCount == -1) 0 else sensorSteps - it.initialStepCount
            val stride = if (it.mode == "WALK") 0.75f else 1.0f
            it.copy(
                steps = newSteps,
                distanceMetres = newSteps * stride,
                initialStepCount = if (it.initialStepCount == -1) sensorSteps else it.initialStepCount
            )
        }
    }
    
    fun updateLocation(lat: Double, lng: Double, accuracy: Float, speed: Float, bearing: Float) {
        val currentState = state.value
        if (!currentState.isActive) return
        
        locationSamples.add(
            com.example.data.LocationSample(
                sessionId = 0, // Will be set before saving
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
        _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
    }
}
