package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.pm.PackageManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class StepSource {
    STEP_COUNTER,
    STEP_DETECTOR,
    UNAVAILABLE
}

data class StepReading(
    val totalSteps: Int,
    val source: StepSource
)

data class SensorCapabilities(
    val hasStepCounter: Boolean,
    val hasStepDetector: Boolean,
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasMagnetometer: Boolean,
    val hasRotationVector: Boolean,
    val hasGps: Boolean
)

class SensorGateway(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    
    fun getCapabilities(): SensorCapabilities {
        return SensorCapabilities(
            hasStepCounter = stepCounter != null,
            hasStepDetector = stepDetector != null,
            hasAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
            hasGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null,
            hasMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null,
            hasRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null,
            hasGps = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        )
    }

    fun getSteps(): Flow<StepReading> = callbackFlow {
        var detectorSteps = 0
        val selectedSensor = stepCounter ?: stepDetector
        val selectedSource = when {
            stepCounter != null -> StepSource.STEP_COUNTER
            stepDetector != null -> StepSource.STEP_DETECTOR
            else -> StepSource.UNAVAILABLE
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        event.values.firstOrNull()?.let { steps ->
                            trySend(StepReading(steps.toInt(), StepSource.STEP_COUNTER))
                        }
                    }
                    Sensor.TYPE_STEP_DETECTOR -> {
                        detectorSteps += 1
                        trySend(StepReading(detectorSteps, StepSource.STEP_DETECTOR))
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        if (selectedSensor != null) {
            sensorManager.registerListener(listener, selectedSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            trySend(StepReading(0, selectedSource))
        }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
