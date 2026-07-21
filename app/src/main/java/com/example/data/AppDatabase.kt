package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Session::class, Lap::class, LoopProfile::class, CalibrationLap::class, LocationSample::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN startTimeMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN endTimeMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN mode TEXT NOT NULL DEFAULT 'WALK'")
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN distanceConfidence TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN calibrationLapCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN averageStepsPerLap REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN averageDurationSeconds REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loop_profiles ADD COLUMN archivedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calibration_laps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `loopProfileId` INTEGER NOT NULL,
                        `index` INTEGER NOT NULL,
                        `mode` TEXT NOT NULL,
                        `steps` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `distanceMetres` REAL NOT NULL,
                        `accepted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
