package com.personal.moneytracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RawEventDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    @Test fun duplicateFingerprintIsIgnored() = runTest {
        val event = RawNotificationEvent("1", "allowed.app", "key", 1, 1, "Title", "Text", null, null, null, null, "same-hash", RawEventStatus.CAPTURED, 1)
        db.rawEventDao().insert(event)
        db.rawEventDao().insert(event.copy(id = "2"))
        assertEquals(1, db.rawEventDao().count())
    }
}
