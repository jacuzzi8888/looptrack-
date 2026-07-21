package com.example.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {

    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    fun getLapsForSession(sessionId: Int): Flow<List<Lap>> = sessionDao.getLapsForSession(sessionId)

    suspend fun getSessionById(id: Int): Session? = sessionDao.getSessionById(id)

    suspend fun saveSession(session: Session): Int {
        return sessionDao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    suspend fun saveLap(lap: Lap) {
        sessionDao.insertLap(lap)
    }
    
    // Locations
    suspend fun saveLocationSample(sample: LocationSample) {
        sessionDao.insertLocationSample(sample)
    }
    
    fun getLocationsForSession(sessionId: Int): Flow<List<LocationSample>> = sessionDao.getLocationsForSession(sessionId)
    
    // Loop Profiles
    fun getAllLoopProfiles(): Flow<List<LoopProfile>> = sessionDao.getAllLoopProfiles()
    
    suspend fun saveLoopProfile(profile: LoopProfile): Int {
        return sessionDao.insertLoopProfile(profile).toInt()
    }

    suspend fun saveLoopProfileWithCalibration(profile: LoopProfile, calibrationLaps: List<CalibrationLap>): Int {
        return sessionDao.insertLoopProfileWithCalibration(profile, calibrationLaps).toInt()
    }
    
    suspend fun getLoopProfileById(id: Int): LoopProfile? = sessionDao.getLoopProfileById(id)

    fun getCalibrationLaps(profileId: Int): Flow<List<CalibrationLap>> = sessionDao.getCalibrationLaps(profileId)
}
