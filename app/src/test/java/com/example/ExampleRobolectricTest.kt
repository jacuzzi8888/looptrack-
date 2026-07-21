package com.example

import android.os.SystemClock
import com.example.engine.TrackingEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrackingEngineTest {

    @Test
    fun testStartTracking() {
        TrackingEngine.startTracking("WALK", null)
        val state = TrackingEngine.state.value
        assertTrue(state.isActive)
        assertEquals("WALK", state.mode)
        assertEquals(0L, state.elapsedSeconds)
        assertEquals(0, state.steps)
        assertEquals(0f, state.distanceMetres)
        assertTrue(state.startTimeMillis > 0L)
    }

    @Test
    fun testPauseAndResume() {
        TrackingEngine.startTracking("WALK", null)
        assertTrue(TrackingEngine.state.value.isActive)

        // Advance elapsedRealtime in Robolectric
        SystemClock.sleep(2000)
        TrackingEngine.tick()
        assertEquals(2L, TrackingEngine.state.value.elapsedSeconds)

        // Toggle Pause
        TrackingEngine.togglePause()
        assertFalse(TrackingEngine.state.value.isActive)

        // Sleep 3 seconds while paused
        SystemClock.sleep(3000)
        TrackingEngine.tick()
        // Elapsed active time should still be 2 seconds
        assertEquals(2L, TrackingEngine.state.value.elapsedSeconds)
        // Paused duration should be 3 seconds
        assertEquals(3L, TrackingEngine.state.value.pausedSeconds)

        // Toggle Resume
        TrackingEngine.togglePause()
        assertTrue(TrackingEngine.state.value.isActive)

        // Advance 1 second more
        SystemClock.sleep(1000)
        TrackingEngine.tick()
        assertEquals(3L, TrackingEngine.state.value.elapsedSeconds)
        assertEquals(3L, TrackingEngine.state.value.pausedSeconds)
    }

    @Test
    fun testStopWhilePaused() {
        TrackingEngine.startTracking("WALK", null)
        TrackingEngine.updateStepsFromSensor(1000)

        SystemClock.sleep(2000)
        TrackingEngine.tick()
        TrackingEngine.updateStepsFromSensor(1020)
        assertEquals(2L, TrackingEngine.state.value.elapsedSeconds)
        assertEquals(20, TrackingEngine.state.value.steps)

        TrackingEngine.togglePause()
        SystemClock.sleep(3000)

        val (finalState, _) = TrackingEngine.stopTracking()

        assertFalse(finalState.isActive)
        assertEquals(2L, finalState.elapsedSeconds)
        assertEquals(3L, finalState.pausedSeconds)
        assertEquals(20, finalState.steps)
        assertEquals(0, finalState.laps)
        assertTrue(finalState.endTimeMillis >= finalState.startTimeMillis)
    }

    @Test
    fun testRestoreFromCheckpoint() {
        val restoredState = com.example.engine.TrackingState(
            isActive = true,
            phase = "ACTIVE",
            mode = "WALK",
            elapsedSeconds = 12L,
            pausedSeconds = 3L,
            laps = 1,
            steps = 42,
            distanceMetres = 31.5f,
            lastLapElapsedSeconds = 10L,
            lastLapSteps = 40,
            startTimeMillis = System.currentTimeMillis() - 15000L,
            stepSource = "STEP_COUNTER"
        )
        val restoredLaps = listOf(
            com.example.engine.LapRecord(
                index = 1,
                startElapsedTime = 0L,
                endElapsedTime = 10L,
                steps = 40,
                duration = 10L,
                distance = 30f
            )
        )

        TrackingEngine.restoreFromCheckpoint(restoredState, restoredLaps)

        val state = TrackingEngine.state.value
        assertTrue(state.isActive)
        assertEquals(12L, state.elapsedSeconds)
        assertEquals(3L, state.pausedSeconds)
        assertEquals(1, state.laps)
        assertEquals(42, state.steps)
        assertEquals("STEP_COUNTER", state.stepSource)
        assertEquals(1, TrackingEngine.lapRecordsFlow.value.size)
    }

    @Test
    fun testSensorUpdatesDuringPause() {
        TrackingEngine.startTracking("WALK", null)
        
        // Initial sensor update
        TrackingEngine.updateStepsFromSensor(1000)
        assertEquals(0, TrackingEngine.state.value.steps)

        // Active steps
        TrackingEngine.updateStepsFromSensor(1015)
        assertEquals(15, TrackingEngine.state.value.steps)
        assertEquals(15 * 0.75f, TrackingEngine.state.value.distanceMetres, 0.001f)

        // Pause
        TrackingEngine.togglePause()

        // Sensor update while paused should be ignored/not advance steps
        TrackingEngine.updateStepsFromSensor(1030)
        assertEquals(15, TrackingEngine.state.value.steps)

        // Resume
        TrackingEngine.togglePause()

        // Next active sensor update
        TrackingEngine.updateStepsFromSensor(1040)
        assertEquals(25, TrackingEngine.state.value.steps)
        assertEquals(25 * 0.75f, TrackingEngine.state.value.distanceMetres, 0.001f)
    }

    @Test
    fun testLapCreation() {
        TrackingEngine.startTracking("WALK", null)
        
        // Advance steps and time
        TrackingEngine.updateStepsFromSensor(1000)
        SystemClock.sleep(5000)
        TrackingEngine.tick()
        TrackingEngine.updateStepsFromSensor(1050) // 50 steps
        
        // Mark Lap 1
        TrackingEngine.markLap()
        assertEquals(1, TrackingEngine.state.value.laps)
        val laps = TrackingEngine.lapRecordsFlow.value
        assertEquals(1, laps.size)
        assertEquals(1, laps[0].index)
        assertEquals(50, laps[0].steps)
        assertEquals(5L, laps[0].duration)

        // Next lap segment
        SystemClock.sleep(3000)
        TrackingEngine.tick()
        TrackingEngine.updateStepsFromSensor(1080) // 30 steps in this lap
        
        // Mark Lap 2
        TrackingEngine.markLap()
        assertEquals(2, TrackingEngine.state.value.laps)
        val lapsUpdated = TrackingEngine.lapRecordsFlow.value
        assertEquals(2, lapsUpdated.size)
        assertEquals(2, lapsUpdated[1].index)
        assertEquals(30, lapsUpdated[1].steps)
        assertEquals(3L, lapsUpdated[1].duration)
    }

    @Test
    fun testManualLapCorrection() {
        TrackingEngine.startTracking("WALK", null)
        TrackingEngine.correctLapCount(4)
        assertEquals(4, TrackingEngine.state.value.laps)

        TrackingEngine.correctLapCount(-2)
        assertEquals(0, TrackingEngine.state.value.laps)
    }

    @Test
    fun testStopAndReset() {
        TrackingEngine.startTracking("RUN", null)
        TrackingEngine.updateStepsFromSensor(5000)
        TrackingEngine.updateStepsFromSensor(5100)
        
        SystemClock.sleep(4000)
        TrackingEngine.tick()
        
        val (finalState, lapsAndLocations) = TrackingEngine.stopTracking()
        assertFalse(finalState.isActive)
        assertEquals(100, finalState.steps)
        assertEquals(100 * 1.0f, finalState.distanceMetres, 0.001f)
        assertTrue(finalState.endTimeMillis >= finalState.startTimeMillis)
        
        // Check that starting another resets everything
        TrackingEngine.startTracking("WALK", null)
        assertEquals(0, TrackingEngine.state.value.steps)
        assertEquals(0L, TrackingEngine.state.value.elapsedSeconds)
    }
}
