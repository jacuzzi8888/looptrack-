package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SessionRepository

class LoopTrackApp : Application() {
    lateinit var database: AppDatabase
        private set
        
    lateinit var sessionRepository: SessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "looptrack_db"
        )
        .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        .build()
        
        sessionRepository = SessionRepository(database.sessionDao())
    }
}
