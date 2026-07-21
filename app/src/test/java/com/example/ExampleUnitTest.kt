package com.example

import com.example.data.Session
import com.example.data.Lap
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
}
