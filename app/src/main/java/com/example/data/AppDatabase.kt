package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Session::class, Lap::class, LoopProfile::class, LocationSample::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
