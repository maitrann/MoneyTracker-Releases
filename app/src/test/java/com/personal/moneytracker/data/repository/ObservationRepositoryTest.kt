package com.personal.moneytracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.domain.parser.DirectionHint
import com.personal.moneytracker.domain.parser.ObservationDraft
import com.personal.moneytracker.domain.parser.ParseResult
import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.parser.rawEvent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ObservationRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ObservationRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ObservationRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun parsedObservationIsLinkedAndRetrySafe() = runTest {
        val raw = rawEvent()
        database.rawEventDao().insert(raw)
        val result = ParseResult.Parsed(
            ObservationDraft(
                sourceApp = SourceApp.GENERIC, amountVnd = 125000,
                directionHint = DirectionHint.DEBIT, eventAtEpochMs = raw.postedAtEpochMs,
                parserName = "fixture", parserVersion = 1, confidence = 0.5,
            ),
        )

        repository.record(raw.id, result, now = 2000)
        repository.record(raw.id, result, now = 3000)

        assertEquals(1, database.parsedObservationDao().count())
        assertEquals(raw.id, database.parsedObservationDao().findByRawEventId(raw.id)?.rawEventId)
        assertEquals(RawEventStatus.PARSED, database.rawEventDao().findById(raw.id)?.captureStatus)
    }

    @Test fun unparsedReasonIsPersistedWithoutObservation() = runTest {
        val raw = rawEvent()
        database.rawEventDao().insert(raw)
        repository.record(raw.id, ParseResult.Unparsed("No amount", "generic"))

        val updated = database.rawEventDao().findById(raw.id)!!
        assertEquals(RawEventStatus.UNPARSED, updated.captureStatus)
        assertEquals("No amount", updated.processingReason)
        assertEquals(0, database.parsedObservationDao().count())
    }
}
