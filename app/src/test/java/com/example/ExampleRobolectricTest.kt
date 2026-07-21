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
    fun testStopWhilePaused() {
        TrackingEngine.startTracking("WALK", null)
        
        SystemClock.sleep(2000)
        TrackingEngine.updateStepsFromSensor(100) // Initial
        TrackingEngine.updateStepsFromSensor(150) // 50 steps
        TrackingEngine.tick()
        
        // Pause tracking
        TrackingEngine.togglePause()
        
        // Wait 3 seconds while paused
        SystemClock.sleep(3000)
        TrackingEngine.tick()
        
        // Ensure state is correct before stop
        val stateBeforeStop = TrackingEngine.state.value
        assertEquals(2L, stateBeforeStop.elapsedSeconds)
        assertEquals(3L, stateBeforeStop.pausedSeconds)
        assertEquals(50, stateBeforeStop.steps)
        
        // Stop tracking while paused
        val (finalState, _) = TrackingEngine.stopTracking()
        
        assertFalse(finalState.isActive)
        assertEquals(2L, finalState.elapsedSeconds) // Should remain 2 seconds
        assertEquals(3L, finalState.pausedSeconds) // Should include final paused interval
        assertEquals(50, finalState.steps) // Steps should remain unchanged
        assertEquals(0, finalState.laps) // Laps remain unchanged
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
