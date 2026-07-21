package com.example.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loopId: Int? = null,
    val mode: String, // "WALK" or "RUN"
    val state: String, // "ACTIVE", "PAUSED", "COMPLETED"
    val startElapsedTime: Long,
    val endElapsedTime: Long? = null,
    val pausedDuration: Long = 0L,
    val totalSteps: Int = 0,
    val distanceMetres: Float = 0f,
    val confidence: String = "HIGH", // HIGH, MEDIUM, LOW
    val distanceSource: String = "CALIBRATED_LOOP",
    @ColumnInfo(defaultValue = "0") val startTimeMillis: Long = 0L,
    @ColumnInfo(defaultValue = "0") val endTimeMillis: Long = 0L
)

@Entity(tableName = "laps")
data class Lap(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val index: Int,
    val startElapsedTime: Long,
    val endElapsedTime: Long,
    val steps: Int,
    val duration: Long,
    val distance: Float,
    val source: String, // "AUTO", "MANUAL"
    val confidence: String, // "HIGH", "MEDIUM", "LOW"
    val corrected: Boolean = false
)

@Entity(tableName = "loop_profiles")
data class LoopProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val distanceMetres: Float,
    val distanceSource: String, // "GPS", "KNOWN", "ESTIMATED"
    @ColumnInfo(defaultValue = "'WALK'") val mode: String = "WALK",
    @ColumnInfo(defaultValue = "'MEDIUM'") val distanceConfidence: String = "MEDIUM",
    @ColumnInfo(defaultValue = "0") val calibrationLapCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val averageStepsPerLap: Float = 0f,
    @ColumnInfo(defaultValue = "0") val averageDurationSeconds: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val archivedAt: Long = 0L
)

@Entity(tableName = "calibration_laps")
data class CalibrationLap(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loopProfileId: Int,
    val index: Int,
    val mode: String,
    val steps: Int,
    val durationSeconds: Long,
    val distanceMetres: Float,
    val accepted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "location_samples")
data class LocationSample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val elapsedTime: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float
)
