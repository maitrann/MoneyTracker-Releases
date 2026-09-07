package com.personal.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.moneytracker.data.local.dao.RawEventDao
import com.personal.moneytracker.data.local.dao.ParsedObservationDao
import com.personal.moneytracker.data.local.dao.TransactionDao
import com.personal.moneytracker.data.local.dao.CategorizationDao
import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.TransactionObservationLink
import com.personal.moneytracker.data.local.entity.Category
import com.personal.moneytracker.data.local.entity.CategorizationRule
import com.personal.moneytracker.data.local.entity.ParsedObservation
import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import com.personal.moneytracker.data.local.entity.SheetSyncRecord
import com.personal.moneytracker.data.local.dao.SheetSyncDao
import com.personal.moneytracker.domain.parser.DirectionHint
import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.model.ObservationRole
import com.personal.moneytracker.domain.model.SyncState
import com.personal.moneytracker.domain.model.TransactionType
import com.personal.moneytracker.domain.model.RuleField
import com.personal.moneytracker.domain.model.RuleOperator
import com.personal.moneytracker.domain.model.RuleSource

class RoomConverters {
    @TypeConverter fun fromStatus(value: RawEventStatus): String = value.name
    @TypeConverter fun toStatus(value: String): RawEventStatus = RawEventStatus.valueOf(value)
    @TypeConverter fun fromDirection(value: DirectionHint): String = value.name
    @TypeConverter fun toDirection(value: String): DirectionHint = DirectionHint.valueOf(value)
    @TypeConverter fun fromSource(value: SourceApp): String = value.name
    @TypeConverter fun toSource(value: String): SourceApp = SourceApp.valueOf(value)
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun fromSyncState(value: SyncState): String = value.name
    @TypeConverter fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
    @TypeConverter fun fromRole(value: ObservationRole): String = value.name
    @TypeConverter fun toRole(value: String): ObservationRole = ObservationRole.valueOf(value)
    @TypeConverter fun fromRuleField(value: RuleField): String = value.name
    @TypeConverter fun toRuleField(value: String): RuleField = RuleField.valueOf(value)
    @TypeConverter fun fromRuleOperator(value: RuleOperator): String = value.name
    @TypeConverter fun toRuleOperator(value: String): RuleOperator = RuleOperator.valueOf(value)
    @TypeConverter fun fromRuleSource(value: RuleSource): String = value.name
    @TypeConverter fun toRuleSource(value: String): RuleSource = RuleSource.valueOf(value)
}

@Database(entities = [RawNotificationEvent::class, ParsedObservation::class, CanonicalTransaction::class, TransactionObservationLink::class, Category::class, CategorizationRule::class, SheetSyncRecord::class], version = 6, exportSchema = true)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawEventDao(): RawEventDao
    abstract fun parsedObservationDao(): ParsedObservationDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categorizationDao(): CategorizationDao
    abstract fun sheetSyncDao(): SheetSyncDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "money-tracker.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE raw_notification_events ADD COLUMN parserName TEXT")
                db.execSQL("ALTER TABLE raw_notification_events ADD COLUMN processingReason TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS parsed_observations (
                        id TEXT NOT NULL PRIMARY KEY,
                        rawEventId TEXT NOT NULL,
                        sourceApp TEXT NOT NULL,
                        amountVnd INTEGER NOT NULL,
                        directionHint TEXT NOT NULL,
                        eventAtEpochMs INTEGER NOT NULL,
                        merchant TEXT,
                        service TEXT,
                        fundingSourceHint TEXT,
                        paymentChannelHint TEXT,
                        referenceHint TEXT,
                        descriptionNormalized TEXT,
                        parserName TEXT NOT NULL,
                        parserVersion INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(rawEventId) REFERENCES raw_notification_events(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_parsed_observations_rawEventId ON parsed_observations(rawEventId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS canonical_transactions (
                        id TEXT NOT NULL PRIMARY KEY, occurredAtEpochMs INTEGER NOT NULL, amountVnd INTEGER NOT NULL,
                        currency TEXT NOT NULL, type TEXT NOT NULL, merchant TEXT, service TEXT, categoryId TEXT NOT NULL,
                        subcategory TEXT, fundingSource TEXT, paymentChannel TEXT, description TEXT, note TEXT,
                        classificationConfidence REAL NOT NULL, matchConfidence REAL NOT NULL, userEdited INTEGER NOT NULL,
                        userCategoryLocked INTEGER NOT NULL, syncState TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_observation_links (
                        transactionId TEXT NOT NULL, observationId TEXT NOT NULL, linkRole TEXT NOT NULL,
                        matchScore REAL NOT NULL, explanation TEXT NOT NULL, PRIMARY KEY(transactionId, observationId),
                        FOREIGN KEY(transactionId) REFERENCES canonical_transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(observationId) REFERENCES parsed_observations(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transaction_observation_links_observationId ON transaction_observation_links(observationId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE canonical_transactions ADD COLUMN reconciliationReadyAtEpochMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT NOT NULL PRIMARY KEY, displayName TEXT NOT NULL, parentId TEXT, isSystem INTEGER NOT NULL, sortOrder INTEGER NOT NULL, active INTEGER NOT NULL)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categorization_rules (
                        id TEXT NOT NULL PRIMARY KEY, priority INTEGER NOT NULL, enabled INTEGER NOT NULL,
                        field TEXT NOT NULL, operator TEXT NOT NULL, pattern TEXT NOT NULL, targetCategoryId TEXT NOT NULL,
                        targetSubcategory TEXT, targetService TEXT, source TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sheet_sync_records (
                        transactionId TEXT NOT NULL PRIMARY KEY,
                        spreadsheetId TEXT NOT NULL,
                        sheetName TEXT NOT NULL,
                        remoteRow INTEGER,
                        lastSyncedUpdatedAtEpochMs INTEGER NOT NULL,
                        lastAttemptAtEpochMs INTEGER,
                        lastErrorCode TEXT,
                        FOREIGN KEY(transactionId) REFERENCES canonical_transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }
    }
}
