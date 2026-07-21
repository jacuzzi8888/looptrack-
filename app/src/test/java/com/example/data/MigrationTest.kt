package com.example.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    @Test
    fun testMigration2To3() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
            .name(null) // In-memory database
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS sessions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            loopId INTEGER,
                            mode TEXT NOT NULL,
                            state TEXT NOT NULL,
                            startElapsedTime INTEGER NOT NULL,
                            endElapsedTime INTEGER,
                            pausedDuration INTEGER NOT NULL,
                            totalSteps INTEGER NOT NULL,
                            distanceMetres REAL NOT NULL,
                            confidence TEXT NOT NULL,
                            distanceSource TEXT NOT NULL
                        )
                    """.trimIndent())
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
            
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = openHelper.writableDatabase
        
        // Insert some data in V2 schema
        db.execSQL("INSERT INTO sessions (mode, state, startElapsedTime, pausedDuration, totalSteps, distanceMetres, confidence, distanceSource) VALUES ('WALK', 'ACTIVE', 0, 0, 100, 75.0, 'HIGH', 'CALIBRATED_LOOP')")
        
        // Run migration
        AppDatabase.MIGRATION_2_3.migrate(db)
        
        // Verify V3 schema by reading the new columns
        val cursor = db.query("SELECT startTimeMillis, endTimeMillis FROM sessions")
        assertTrue(cursor.moveToFirst())
        assertEquals(0L, cursor.getLong(0))
        assertEquals(0L, cursor.getLong(1))
        cursor.close()
        
        db.close()
    }
}
