package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-2-4-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration2To4() = runTest {
        createVersion2Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        try {
            val session = database.sessionDao().getSessionById(1)
            val profile = database.sessionDao().getAllLoopProfiles().first().single()
            val calibrationLaps = database.sessionDao().getCalibrationLaps(profile.id).first()

            assertNotNull(session)
            assertEquals(1, session?.id)
            assertEquals("WALK", session?.mode)
            assertEquals("COMPLETED", session?.state)
            assertEquals(0L, session?.startTimeMillis)
            assertEquals(0L, session?.endTimeMillis)
            assertEquals("Home loop", profile.name)
            assertEquals("WALK", profile.mode)
            assertEquals("MEDIUM", profile.distanceConfidence)
            assertEquals(0, profile.calibrationLapCount)
            assertEquals(0f, profile.averageStepsPerLap, 0.001f)
            assertEquals(0f, profile.averageDurationSeconds, 0.001f)
            assertEquals(0, calibrationLaps.size)
        } finally {
            database.close()
        }
    }

    private fun createVersion2Database() {
        context.deleteDatabase(dbName)
        val databaseFile = context.getDatabasePath(dbName)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile.path, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `loopId` INTEGER,
                    `mode` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `startElapsedTime` INTEGER NOT NULL,
                    `endElapsedTime` INTEGER,
                    `pausedDuration` INTEGER NOT NULL,
                    `totalSteps` INTEGER NOT NULL,
                    `distanceMetres` REAL NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `distanceSource` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `laps` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `index` INTEGER NOT NULL,
                    `startElapsedTime` INTEGER NOT NULL,
                    `endElapsedTime` INTEGER NOT NULL,
                    `steps` INTEGER NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `distance` REAL NOT NULL,
                    `source` TEXT NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `corrected` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `loop_profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `distanceMetres` REAL NOT NULL,
                    `distanceSource` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `location_samples` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `elapsedTime` INTEGER NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `accuracy` REAL NOT NULL,
                    `speed` REAL NOT NULL,
                    `bearing` REAL NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `sessions` (
                    `id`,
                    `loopId`,
                    `mode`,
                    `state`,
                    `startElapsedTime`,
                    `endElapsedTime`,
                    `pausedDuration`,
                    `totalSteps`,
                    `distanceMetres`,
                    `confidence`,
                    `distanceSource`
                ) VALUES (1, NULL, 'WALK', 'COMPLETED', 0, 120, 10, 160, 120.0, 'HIGH', 'CALIBRATED_LOOP')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `loop_profiles` (
                    `id`,
                    `name`,
                    `distanceMetres`,
                    `distanceSource`,
                    `createdAt`
                ) VALUES (1, 'Home loop', 32.0, 'KNOWN', 1710000000000)
                """.trimIndent()
            )
            db.version = 2
        }
    }
}
