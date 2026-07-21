package com.example

import com.example.data.Session
import com.example.data.Lap
import com.example.data.LoopProfile
import com.example.engine.CalibrationEngine
import com.example.engine.CalibrationLapInput
import org.junit.Assert.*
import org.junit.Test

class LoopTrackDataUnitTest {

    @Test
    fun testSessionEntityProperties() {
        val session = Session(
            id = 1,
            loopId = 2,
            mode = "RUN",
            state = "COMPLETED",
            startElapsedTime = 0L,
            endElapsedTime = 3600L,
            pausedDuration = 120L,
            totalSteps = 8000,
            distanceMetres = 6400f,
            confidence = "HIGH",
            distanceSource = "CALIBRATED_LOOP",
            startTimeMillis = 1710000000000L,
            endTimeMillis = 1710003600000L
        )

        assertEquals(1, session.id)
        assertEquals(2, session.loopId)
        assertEquals("RUN", session.mode)
        assertEquals("COMPLETED", session.state)
        assertEquals(0L, session.startElapsedTime)
        assertEquals(3600L, session.endElapsedTime)
        assertEquals(120L, session.pausedDuration)
        assertEquals(8000, session.totalSteps)
        assertEquals(6400f, session.distanceMetres, 0.001f)
        assertEquals("HIGH", session.confidence)
        assertEquals("CALIBRATED_LOOP", session.distanceSource)
        assertEquals(1710000000000L, session.startTimeMillis)
        assertEquals(1710003600000L, session.endTimeMillis)
    }

    @Test
    fun testLapEntityProperties() {
        val lap = Lap(
            id = 5,
            sessionId = 1,
            index = 1,
            startElapsedTime = 0L,
            endElapsedTime = 300L,
            steps = 400,
            duration = 300L,
            distance = 300f,
            source = "MANUAL",
            confidence = "HIGH",
            corrected = false
        )

        assertEquals(5, lap.id)
        assertEquals(1, lap.sessionId)
        assertEquals(1, lap.index)
        assertEquals(0L, lap.startElapsedTime)
        assertEquals(300L, lap.endElapsedTime)
        assertEquals(400, lap.steps)
        assertEquals(300L, lap.duration)
        assertEquals(300f, lap.distance, 0.001f)
        assertEquals("MANUAL", lap.source)
        assertEquals("HIGH", lap.confidence)
        assertFalse(lap.corrected)
    }

    @Test
    fun testLoopProfileCalibrationProperties() {
        val profile = LoopProfile(
            id = 3,
            name = "Home loop",
            distanceMetres = 28.5f,
            distanceSource = "KNOWN",
            mode = "WALK",
            distanceConfidence = "HIGH",
            calibrationLapCount = 5,
            averageStepsPerLap = 38.4f,
            averageDurationSeconds = 21.2f,
            createdAt = 1710000000000L,
            updatedAt = 1710000100000L
        )

        assertEquals(3, profile.id)
        assertEquals("Home loop", profile.name)
        assertEquals(28.5f, profile.distanceMetres, 0.001f)
        assertEquals("KNOWN", profile.distanceSource)
        assertEquals("WALK", profile.mode)
        assertEquals("HIGH", profile.distanceConfidence)
        assertEquals(5, profile.calibrationLapCount)
        assertEquals(38.4f, profile.averageStepsPerLap, 0.001f)
        assertEquals(21.2f, profile.averageDurationSeconds, 0.001f)
        assertEquals(1710000100000L, profile.updatedAt)
    }

    @Test
    fun testCalibrationRequiresFiveLaps() {
        val result = CalibrationEngine.evaluate(
            listOf(
                CalibrationLapInput(index = 1, steps = 40, durationSeconds = 22),
                CalibrationLapInput(index = 2, steps = 41, durationSeconds = 23)
            )
        )

        assertEquals(2, result.acceptedLapCount)
        assertEquals("LOW", result.distanceConfidence)
        assertNotNull(result.warning)
    }

    @Test
    fun testConsistentCalibrationIsHighConfidence() {
        val result = CalibrationEngine.evaluate(
            listOf(
                CalibrationLapInput(index = 1, steps = 40, durationSeconds = 22),
                CalibrationLapInput(index = 2, steps = 41, durationSeconds = 22),
                CalibrationLapInput(index = 3, steps = 39, durationSeconds = 23),
                CalibrationLapInput(index = 4, steps = 40, durationSeconds = 22),
                CalibrationLapInput(index = 5, steps = 41, durationSeconds = 23)
            )
        )

        assertEquals(5, result.acceptedLapCount)
        assertEquals("HIGH", result.distanceConfidence)
        assertEquals(40.2f, result.averageStepsPerLap, 0.001f)
        assertNull(result.warning)
    }
}
