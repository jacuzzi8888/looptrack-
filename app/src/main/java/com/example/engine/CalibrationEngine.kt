package com.example.engine

import kotlin.math.abs

data class CalibrationLapInput(
    val index: Int,
    val steps: Int,
    val durationSeconds: Long
)

data class CalibrationResult(
    val acceptedLapCount: Int,
    val averageStepsPerLap: Float,
    val averageDurationSeconds: Float,
    val distanceConfidence: String,
    val warning: String?
)

object CalibrationEngine {
    const val REQUIRED_LAPS = 5

    fun evaluate(laps: List<CalibrationLapInput>): CalibrationResult {
        val accepted = laps
            .filter { it.steps >= 0 && it.durationSeconds >= 0 }
            .sortedBy { it.index }

        if (accepted.isEmpty()) {
            return CalibrationResult(
                acceptedLapCount = 0,
                averageStepsPerLap = 0f,
                averageDurationSeconds = 0f,
                distanceConfidence = "LOW",
                warning = "Mark five laps to calibrate this loop."
            )
        }

        val averageSteps = accepted.map { it.steps }.average().toFloat()
        val averageDuration = accepted.map { it.durationSeconds }.average().toFloat()
        val stepsVariation = relativeMaxDeviation(accepted.map { it.steps.toFloat() }, averageSteps)
        val durationVariation = relativeMaxDeviation(accepted.map { it.durationSeconds.toFloat() }, averageDuration)
        val maxVariation = maxOf(stepsVariation, durationVariation)

        val confidence = when {
            accepted.size < REQUIRED_LAPS -> "LOW"
            maxVariation <= 0.15f -> "HIGH"
            maxVariation <= 0.30f -> "MEDIUM"
            else -> "LOW"
        }

        val warning = when {
            accepted.size < REQUIRED_LAPS -> "Mark ${REQUIRED_LAPS - accepted.size} more laps to save this calibration."
            confidence == "LOW" -> "Lap steps or times varied a lot. Save only if you trust the measured loop distance."
            confidence == "MEDIUM" -> "Calibration is usable, but lap consistency was not tight."
            else -> null
        }

        return CalibrationResult(
            acceptedLapCount = accepted.size,
            averageStepsPerLap = averageSteps,
            averageDurationSeconds = averageDuration,
            distanceConfidence = confidence,
            warning = warning
        )
    }

    private fun relativeMaxDeviation(values: List<Float>, average: Float): Float {
        if (values.isEmpty() || average == 0f) return 0f
        return values.maxOf { abs(it - average) } / average
    }
}
