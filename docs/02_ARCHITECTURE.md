# 02 — Technical Architecture

## 1. Architectural principle

Local-first, event-driven, deterministic V1. Room is the source of truth. Google Sheets is a synchronization/backup/report destination.

```text
Android notifications
        |
        v
NotificationListenerService
        |
        v
RawNotificationEvent (Room)
        |
        v
NotificationProcessor
        |
        v
Parser Registry
        |
        v
ParsedObservation (Room)
        |
        v
Transaction Matcher / Reconciler
        |
        v
CanonicalTransaction (Room)
        |
        +---------------------+
        |                     |
        v                     v
Feature UI                Sync Queue
                              |
                              v
                          WorkManager
                              |
                              v
                       Google Sheets API
```

## 2. Final V1 stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM / unidirectional data flow
- Coroutines + Flow
- Room
- Hilt
- WorkManager for persistent sync/recovery work
- NotificationListenerService
- Google Identity authorization
- Google Sheets API

Keep a single Gradle `:app` module for V1.

## 3. Package organization

```text
com.personal.moneytracker/
  app/
    MoneyTrackerApp.kt
    navigation/

  data/
    local/
      database/
      dao/
      entity/
      mapper/
    remote/
      sheets/
        GoogleAuthorizationManager.kt
        SheetsApiClient.kt
        SheetsRowMapper.kt
    repository/

  domain/
    model/
    parser/
      NotificationParser.kt
      ParserRegistry.kt
      common/
      momo/
      timo/
      techcombank/
      grab/
    matching/
    categorization/
    usecase/

  notification/
    FinanceNotificationListenerService.kt
    NotificationExtractor.kt
    SensitiveContentFilter.kt
    NotificationProcessor.kt

  sync/
    SheetSyncWorker.kt
    SyncCoordinator.kt
    SyncScheduler.kt

  feature/
    onboarding/
    dashboard/
    transactions/
    transactiondetail/
    rules/
    debug/
    settings/

  di/
    DatabaseModule.kt
    RepositoryModule.kt
    SheetsModule.kt
```

## 4. Layer responsibilities

### App
Own application initialization and top-level navigation. No business logic.

### Feature
Each feature keeps its Compose screen, ViewModel, UI state, and feature-specific components together.

```text
feature/dashboard/
  DashboardScreen.kt
  DashboardViewModel.kt
  DashboardUiState.kt
  components/
```

Feature UI must not access DAOs or Google API clients directly.

### Data
Own Room database/DAO/entity/mapper code, Google Sheets client code, and repository implementations.

### Domain
Own pure reusable business rules: parsing contracts, matching, duplicate prevention, internal-transfer detection, categorization, and use cases. Keep Android-framework dependencies out where practical.

### Notification
Own Android notification integration. The listener should verify the allowlist, extract safe fields, filter likely sensitive content, persist raw data, trigger local processing, and return quickly.

### Sync
Own durable external synchronization. WorkManager belongs here.

## 5. Normal notification path

```text
NotificationListenerService
        |
        v
persist RawNotificationEvent in Room
        |
        v
NotificationProcessor
        |
        v
coroutine/domain processing
        |
        v
ParserRegistry
        |
        v
ParsedObservation in Room
```

Do not create one WorkRequest per notification in the normal path.

If the process dies after raw persistence but before parsing, a recovery path may scan unfinished persisted events later.

## 6. WorkManager policy

Use WorkManager for:
- Google Sheets synchronization;
- network retry/backoff;
- durable work that should survive process death;
- optional recovery of unfinished persisted local work.

Do not use WorkManager as the primary parser dispatcher for every notification.

## 7. Hilt dependency injection

Use Hilt consistently.

```kotlin
@HiltAndroidApp
class MoneyTrackerApp : Application()
```

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel()
```

Prefer constructor injection. Use modules for Room providers, interface bindings, Google API client construction, and other dependencies that cannot be constructor-injected.

Do not introduce a service locator.

## 8. Dependency direction

```text
feature -> ViewModel -> repository/use case
notification -> NotificationProcessor -> repository/domain
sync -> repository/remote
data -> implements persistence/remote boundaries
domain -> pure models/rules
```

## 9. Threading

Notification callbacks remain short. Local processing uses coroutines with appropriate dispatchers. Network sync and durable deferred work use WorkManager. Never call Google APIs directly from `NotificationListenerService`.

## 10. Error handling

Never discard a notification merely because parsing fails.

```text
CAPTURED
PARSED
IGNORED
UNPARSED
SENSITIVE_IGNORED
ERROR
```

## 11. Configuration

Use DataStore/preferences for small settings and Room for transaction/event history.

## 12. No backend in V1

Never embed banking credentials, service-account private keys, or confidential server secrets in the APK.
