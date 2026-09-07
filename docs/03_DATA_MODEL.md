# 03 — Data Model

Use integer minor units for money. For VND, store whole VND as `Long`.

Do not use floating-point types for financial amounts.

## 1. RawNotificationEvent

Represents exactly what Android delivered after safe extraction.

```text
id: UUID/string PK
packageName: String
notificationKey: String?
notificationId: Int?
postedAtEpochMs: Long

title: String?
text: String?
bigText: String?
subText: String?
category: String?
channelId: String?

contentHash: String
captureStatus: RawEventStatus

createdAtEpochMs: Long
parserVersionSeen: String?
```

`contentHash` should use normalized safe fields so exact re-posts can be detected.

Possible `captureStatus`:

```text
CAPTURED
PARSED
IGNORED
UNPARSED
SENSITIVE_IGNORED
ERROR
```

## 2. ParsedObservation

Represents one parser's interpretation of one raw event.

```text
id: UUID/string PK
rawEventId: FK

sourceApp: SourceApp
amountVnd: Long?
directionHint: DirectionHint
eventAtEpochMs: Long

merchant: String?
service: String?
fundingSourceHint: String?
paymentChannelHint: String?
referenceHint: String?
descriptionNormalized: String?

parserName: String
parserVersion: Int
confidence: Double

createdAtEpochMs: Long
```

`DirectionHint`:

```text
DEBIT
CREDIT
TRANSFER
UNKNOWN
```

## 3. CanonicalTransaction

This is the financial record used by the UI and Google Sheets.

```text
id: UUID/string PK

occurredAtEpochMs: Long
amountVnd: Long
currency: String = "VND"

type: TransactionType
merchant: String?
service: String?
categoryId: String
subcategory: String?

fundingSource: String?
paymentChannel: String?

description: String?
note: String?

classificationConfidence: Double
matchConfidence: Double

userEdited: Boolean
userCategoryLocked: Boolean

syncState: SyncState
sheetRowIdOrNumber: String?

createdAtEpochMs: Long
updatedAtEpochMs: Long
```

`TransactionType`:

```text
EXPENSE
INCOME
INTERNAL_TRANSFER
REFUND
UNKNOWN
```

`SyncState`:

```text
NOT_SYNCED
QUEUED
SYNCING
SYNCED
AUTH_REQUIRED
FAILED
```

## 4. TransactionObservationLink

Many observations may describe one transaction.

```text
transactionId: FK
observationId: FK
linkRole: ObservationRole
matchScore: Double
```

Possible roles:

```text
FUNDING_SOURCE
PAYMENT_CHANNEL
MERCHANT
SERVICE_DETAIL
PRIMARY
SUPPORTING
```

This link table is essential for explaining why a canonical transaction was formed.

## 5. Category

```text
id: String PK
displayName: String
parentId: String?
isSystem: Boolean
sortOrder: Int
active: Boolean
```

Suggested IDs:

```text
food
transportation
shopping
bills
entertainment
education
health
housing
subscriptions
gifts
travel
cash
transfer
income
other
unclassified
```

## 6. CategorizationRule

```text
id: UUID/string PK
priority: Int
enabled: Boolean

field: RuleField
operator: RuleOperator
pattern: String

targetCategoryId: String
targetSubcategory: String?
targetService: String?

source: RuleSource
createdAtEpochMs: Long
updatedAtEpochMs: Long
```

`RuleField` examples:

```text
MERCHANT
SERVICE
DESCRIPTION
SOURCE_APP
PAYMENT_CHANNEL
```

`RuleOperator`:

```text
EQUALS
CONTAINS
STARTS_WITH
REGEX
```

`RuleSource`:

```text
SYSTEM
USER
```

User rules outrank system rules.

## 7. Account / payment source

Optional but recommended:

```text
id
displayName
kind
isOwnedByUser
active
```

Kinds:

```text
BANK_ACCOUNT
EWALLET
SAVINGS_POCKET
CASH
CARD
OTHER
```

Examples:

```text
Techcombank
Timo
MoMo
MoMo Túi Thần Tài
```

`isOwnedByUser = true` helps internal transfer detection.

## 8. SyncRecord

If row-level sync metadata needs to be separated:

```text
transactionId
remoteSpreadsheetId
remoteSheetName
remoteRowKey
lastSyncedUpdatedAtEpochMs
lastAttemptAtEpochMs
attemptCount
lastErrorCode
lastErrorMessageSanitized
```

## 9. Stable identity

Never use spreadsheet row number as the transaction identity because rows can move.

Always include the local canonical `transaction_id` as the first column in Google Sheets.

## 10. Deletion strategy

Prefer soft delete:

```text
isDeleted: Boolean
deletedAtEpochMs: Long?
```

Then sync deletion state rather than accidentally resurrecting a transaction after a retry.

For V1, a simpler option is to disallow permanent deletion and provide `Exclude from reports` until remote delete semantics are implemented safely.
