package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startElapsedTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): Session?

    // Laps
    @Query("SELECT * FROM laps WHERE sessionId = :sessionId ORDER BY `index` ASC")
    fun getLapsForSession(sessionId: Int): Flow<List<Lap>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: Lap)
    
    // Locations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationSample(sample: LocationSample)
    
    @Query("SELECT * FROM location_samples WHERE sessionId = :sessionId ORDER BY elapsedTime ASC")
    fun getLocationsForSession(sessionId: Int): Flow<List<LocationSample>>
    
    // Loop Profiles
    @Query("SELECT * FROM loop_profiles")
    fun getAllLoopProfiles(): Flow<List<LoopProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoopProfile(profile: LoopProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibrationLap(lap: CalibrationLap)

    @Transaction
    suspend fun insertLoopProfileWithCalibration(profile: LoopProfile, calibrationLaps: List<CalibrationLap>): Long {
        val profileId = insertLoopProfile(profile)
        calibrationLaps.forEach { lap ->
            insertCalibrationLap(lap.copy(loopProfileId = profileId.toInt()))
        }
        return profileId
    }
    
    @Query("SELECT * FROM loop_profiles WHERE id = :id")
    suspend fun getLoopProfileById(id: Int): LoopProfile?

    @Query("SELECT * FROM calibration_laps WHERE loopProfileId = :profileId ORDER BY `index` ASC")
    fun getCalibrationLaps(profileId: Int): Flow<List<CalibrationLap>>
}
