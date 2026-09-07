package com.personal.moneytracker.di

import android.content.Context
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.dao.ParsedObservationDao
import com.personal.moneytracker.data.local.dao.RawEventDao
import com.personal.moneytracker.data.local.dao.TransactionDao
import com.personal.moneytracker.data.local.dao.SheetSyncDao
import com.personal.moneytracker.domain.matching.MatchScorer
import com.personal.moneytracker.domain.categorization.CategorizationEngine
import com.personal.moneytracker.data.repository.DefaultRawEventRepository
import com.personal.moneytracker.data.repository.RawEventRepository
import com.personal.moneytracker.domain.parser.NotificationParser
import com.personal.moneytracker.domain.parser.ParserRegistry
import com.personal.moneytracker.domain.parser.generic.GenericFinancialParser
import com.personal.moneytracker.domain.parser.grab.GrabNotificationParser
import com.personal.moneytracker.domain.parser.momo.MomoNotificationParser
import com.personal.moneytracker.domain.parser.techcombank.TechcombankNotificationParser
import com.personal.moneytracker.domain.parser.timo.TimoNotificationParser
import com.personal.moneytracker.sync.GoogleAuthorizationGateway
import com.personal.moneytracker.sync.GoogleIdentityAuthorizationGateway
import com.personal.moneytracker.sync.GoogleSheetsRemote
import com.personal.moneytracker.sync.GoogleSheetsRestRemote
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindRawEventRepository(implementation: DefaultRawEventRepository): RawEventRepository
    @Binds abstract fun bindGoogleAuthorizationGateway(implementation: GoogleIdentityAuthorizationGateway): GoogleAuthorizationGateway
    @Binds abstract fun bindGoogleSheetsRemote(implementation: GoogleSheetsRestRemote): GoogleSheetsRemote
}

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase = AppDatabase.create(context)
    @Provides fun rawEventDao(database: AppDatabase): RawEventDao = database.rawEventDao()
    @Provides fun observationDao(database: AppDatabase): ParsedObservationDao = database.parsedObservationDao()
    @Provides fun transactionDao(database: AppDatabase): TransactionDao = database.transactionDao()
    @Provides fun sheetSyncDao(database: AppDatabase): SheetSyncDao = database.sheetSyncDao()
    @Provides @Singleton fun matchScorer(): MatchScorer = MatchScorer()
    @Provides @Singleton fun categorizationEngine(): CategorizationEngine = CategorizationEngine()

    @Provides @Singleton
    fun parserRegistry(
        momo: MomoNotificationParser,
        timo: TimoNotificationParser,
        techcombank: TechcombankNotificationParser,
        grab: GrabNotificationParser,
        generic: GenericFinancialParser,
    ): ParserRegistry = ParserRegistry(
        sourceParsers = listOf<NotificationParser>(momo, timo, techcombank, grab),
        genericParser = generic,
    )
}
