package com.personal.moneytracker.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-test.db"

    @Before fun setUp() { context.deleteDatabase(databaseName) }
    @After fun tearDown() { context.deleteDatabase(databaseName) }

    @Test fun migrationOneToTwoPreservesRawEventsAndCreatesObservationTable() = runTest {
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("""
                CREATE TABLE raw_notification_events (
                    id TEXT NOT NULL PRIMARY KEY, packageName TEXT NOT NULL, notificationKey TEXT,
                    notificationId INTEGER, postedAtEpochMs INTEGER NOT NULL, title TEXT, text TEXT,
                    bigText TEXT, subText TEXT, category TEXT, channelId TEXT, contentHash TEXT NOT NULL,
                    captureStatus TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX index_raw_notification_events_contentHash ON raw_notification_events(contentHash)")
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, '515abfba42613438f6976d2dcf5f73c5')")
            db.execSQL("INSERT INTO raw_notification_events VALUES ('raw-1','fixture.app','key',1,1000,'title','text',NULL,NULL,NULL,NULL,'hash','CAPTURED',1000)")
            db.version = 1
        }

        withContext(Dispatchers.IO) {
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build()
            try {
                    assertEquals("raw-1", migrated.rawEventDao().findById("raw-1")?.id)
                    assertEquals(0, migrated.parsedObservationDao().count())
                    assertEquals(6, migrated.openHelper.readableDatabase.version)
            } finally {
                migrated.close()
            }
        }
    }
}
