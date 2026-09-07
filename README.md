# Personal Money Tracker — Android

A private, local-first Android application for automatically recording personal cash flow from transaction notifications and synchronizing normalized transactions to Google Sheets.

## Primary use case

The user pays through several financial/payment apps:

- MoMo
- Timo
- Techcombank
- Grab
- potentially other apps later

The same real-world payment may generate multiple notifications. The app must recognize that these notifications describe the same transaction instead of counting each notification as a separate expense.

Example:

```text
Timo -> MoMo -> Grab -> GrabFood
```

Desired result:

```text
Expense:       125,000 VND
Merchant:      Grab
Service:       GrabFood
Category:      Food
Funding source:Timo
Payment via:   MoMo
```

Only one expense is recorded.

## V1 goals

- Capture allowlisted Android notifications.
- Store raw notification events locally.
- Parse amount, direction, merchant/source clues, description, and timestamp.
- Match related events.
- Detect internal transfers.
- Categorize transactions with deterministic rules.
- Let the user correct category/service/note.
- Learn reusable user-created rules.
- Store canonical transactions in Room.
- Sync canonical transaction rows to a configured Google Sheet.
- Work offline and sync later.
- Provide a basic dashboard and transaction history.
- Provide a debug/sample-capture screen for improving source parsers.

## Explicitly out of scope for V1

- Publishing to Google Play.
- iOS support.
- Multi-user accounts.
- Bank credential storage.
- Bank login automation.
- Private/reverse-engineered banking APIs.
- Accessibility scraping.
- AI/LLM categorization.
- A backend server.
- Investment portfolio management.
- Budget recommendations.
- Automatic tax/accounting advice.

## Recommended reading order for Codex

1. `AGENTS.md`
2. `docs/01_PRODUCT_REQUIREMENTS.md`
3. `docs/02_ARCHITECTURE.md`
4. `docs/03_DATA_MODEL.md`
5. `docs/04_NOTIFICATION_PIPELINE.md`
6. `docs/05_TRANSACTION_MATCHING.md`
7. `docs/06_GOOGLE_SHEETS_SYNC.md`
8. `docs/07_UI_UX.md`
9. `docs/08_SECURITY_PRIVACY.md`
10. `docs/09_TEST_PLAN.md`
11. `docs/10_ACCEPTANCE_CRITERIA.md`
12. `PLANS.md`

## First development milestone

The first useful vertical slice is:

```text
Notification
    -> capture
    -> raw event saved in Room
    -> NotificationProcessor / coroutine processing
    -> parse amount/source
    -> canonical transaction created
    -> transaction visible in UI
    -> queued sync
    -> row appended to Google Sheets
```

Do this for generic/mock notifications first. Add real MoMo/Timo/Techcombank/Grab parsing only after actual sample notification payloads are available.


## Final V1 package organization

Keep a single Gradle `:app` module for V1 and organize packages as:

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
    repository/
  domain/
    model/
    parser/
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

Final architecture decisions: Hilt for DI; feature-oriented UI packages; WorkManager for durable sync/recovery rather than one job per notification.
