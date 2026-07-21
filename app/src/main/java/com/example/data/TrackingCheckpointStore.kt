package com.example.data

import android.content.Context
import com.example.engine.LapRecord
import com.example.engine.TrackingState
import org.json.JSONArray
import org.json.JSONObject

data class TrackingCheckpoint(
    val state: TrackingState,
    val laps: List<LapRecord>
)

class TrackingCheckpointStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(state: TrackingState, laps: List<LapRecord>) {
        prefs.edit()
            .putString(KEY_STATE, state.toJson().toString())
            .putString(KEY_LAPS, laps.toJson().toString())
            .apply()
    }

    fun load(): TrackingCheckpoint? {
        val stateJson = prefs.getString(KEY_STATE, null) ?: return null
        val lapsJson = prefs.getString(KEY_LAPS, "[]") ?: "[]"

        return TrackingCheckpoint(
            state = JSONObject(stateJson).toTrackingState(),
            laps = JSONArray(lapsJson).toLapRecords()
        )
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_STATE)
            .remove(KEY_LAPS)
            .apply()
    }

    private fun TrackingState.toJson(): JSONObject {
        return JSONObject()
            .put("isActive", isActive)
            .put("phase", phase)
            .put("mode", mode)
            .put("loopId", loopId)
            .put("elapsedSeconds", elapsedSeconds)
            .put("pausedSeconds", pausedSeconds)
            .put("laps", laps)
            .put("validationLapsRequired", validationLapsRequired)
            .put("steps", steps)
            .put("distanceMetres", distanceMetres)
            .put("initialStepCount", initialStepCount)
            .put("lastLapElapsedSeconds", lastLapElapsedSeconds)
            .put("lastLapSteps", lastLapSteps)
            .put("startTimeMillis", startTimeMillis)
            .put("endTimeMillis", endTimeMillis)
            .put("stepSource", stepSource)
    }

    private fun JSONObject.toTrackingState(): TrackingState {
        return TrackingState(
            isActive = getBoolean("isActive"),
            phase = getString("phase"),
            mode = getString("mode"),
            loopId = if (isNull("loopId")) null else getInt("loopId"),
            elapsedSeconds = getLong("elapsedSeconds"),
            pausedSeconds = getLong("pausedSeconds"),
            laps = getInt("laps"),
            validationLapsRequired = getInt("validationLapsRequired"),
            steps = getInt("steps"),
            distanceMetres = getDouble("distanceMetres").toFloat(),
            initialStepCount = getInt("initialStepCount"),
            lastLapElapsedSeconds = getLong("lastLapElapsedSeconds"),
            lastLapSteps = getInt("lastLapSteps"),
            startTimeMillis = getLong("startTimeMillis"),
            endTimeMillis = getLong("endTimeMillis"),
            stepSource = optString("stepSource", "UNAVAILABLE")
        )
    }

    private fun List<LapRecord>.toJson(): JSONArray {
        val array = JSONArray()
        forEach { lap ->
            array.put(
                JSONObject()
                    .put("index", lap.index)
                    .put("startElapsedTime", lap.startElapsedTime)
                    .put("endElapsedTime", lap.endElapsedTime)
                    .put("steps", lap.steps)
                    .put("duration", lap.duration)
                    .put("distance", lap.distance)
            )
        }
        return array
    }

    private fun JSONArray.toLapRecords(): List<LapRecord> {
        return buildList {
            for (index in 0 until length()) {
                val lap = getJSONObject(index)
                add(
                    LapRecord(
                        index = lap.getInt("index"),
                        startElapsedTime = lap.getLong("startElapsedTime"),
                        endElapsedTime = lap.getLong("endElapsedTime"),
                        steps = lap.getInt("steps"),
                        duration = lap.getLong("duration"),
                        distance = lap.getDouble("distance").toFloat()
                    )
                )
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "tracking_checkpoint"
        const val KEY_STATE = "state"
        const val KEY_LAPS = "laps"
    }
}
