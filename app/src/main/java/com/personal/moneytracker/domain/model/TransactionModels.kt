package com.personal.moneytracker.domain.model

enum class TransactionType { EXPENSE, INCOME, INTERNAL_TRANSFER, REFUND, UNKNOWN }
enum class SyncState { NOT_SYNCED, QUEUED, SYNCING, SYNCED, AUTH_REQUIRED, FAILED }
enum class ObservationRole { PRIMARY, SUPPORTING }
