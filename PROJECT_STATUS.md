# PROJECT_STATUS.md

This file is the durable handoff state for Codex between phases.

Codex must update it after completing or auditing a phase.

## Current status

| Phase | Name | Status |
|---|---|---|
| 0 | Bootstrap | COMPLETE |
| 1 | Notification ingestion | COMPLETE |
| 2 | Parser framework | COMPLETE |
| 3 | Canonical transaction creation | COMPLETE |
| 4 | Cross-app enrichment | COMPLETE |
| 5 | Categorization rules | COMPLETE |
| 6 | Google Sheets connection | COMPLETE |
| 7 | Dashboard | COMPLETE |
| 8 | Real notification fixture collection | NOT STARTED |
| 9 | Hardening & V1 audit | NOT STARTED |

## Current next action

Phase 7 Dashboard is complete (45 tests PASS). Sẵn sàng bắt đầu Phase 8 (thu thập notification thật từ thiết bị Android) khi có điện thoại thật.

## Completed foundations

### Phase 0
Status: COMPLETE

Treat the existing implementation as the baseline.
Do not rebuild Phase 0 unless repository inspection identifies a concrete regression or missing prerequisite.

### Phase 1
Status: COMPLETE

Treat the existing notification-ingestion implementation as the baseline.
Do not rebuild Phase 1 unless repository inspection identifies a concrete regression or missing prerequisite.

## Phase 2 handoff

Status: COMPLETE

Expected output:
- parser contracts;
- parser registry;
- text normalization;
- VND amount parsing;
- direction inference;
- conservative generic parser;
- source parser skeletons;
- ParsedObservation persistence;
- debug parser-result UI;
- fixture-driven parser tests.

## Real notification fixture coverage

Update this table only from sanitized samples actually captured from the user's device.

| Source | Sample type | Fixture exists | Parser verified |
|---|---|---:|---:|
| MoMo | Payment | No | No |
| MoMo | Transfer/top-up | No | No |
| MoMo | Túi Thần Tài | No | No |
| Timo | Debit | No | No |
| Timo | Credit | No | No |
| Techcombank | Debit | No | No |
| Techcombank | Credit | No | No |
| Grab | GrabFood | No | No |
| Grab | GrabBike | No | No |
| Grab | GrabCar | No | No |

## Build/test checkpoint

Codex should overwrite this section after every phase.

```text
Last verified phase: Phase 7
Build command: .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
Build result: PASS
Test command: .\gradlew.bat :app:testDebugUnitTest --no-daemon
Test result: PASS (45 tests)
Known failing tests: None
Known limitations: Source-specific MoMo/Timo/Techcombank/Grab notification formats remain unverified because no sanitized device fixtures have been supplied. Google Sheets OAuth client/Sheets API/test sheet not yet set up for real-device testing.
```

## Decisions that must remain stable

- Android only for V1.
- Kotlin + Jetpack Compose.
- Room is source of truth.
- NotificationListenerService is the ingestion mechanism.
- Google Sheets is a sync/backup/report destination.
- No backend for V1.
- No Accessibility Service.
- No bank/payment credential storage.
- No login automation.
- No OTP/passcode capture.
- No private API reverse engineering.
- No AI/LLM classifier in V1.
- Internal transfers do not count as income/expense.
- Multiple observations of one real payment must not become duplicate expenses.
- Source-specific notification formats are fixture-driven, not guessed.

## Phase completion log

Append one entry after each completed/audited phase.

Template:

```text
### Phase N — YYYY-MM-DD

Status: PASS / PARTIAL / FAIL

Implemented:
- ...

Tests/build:
- ...

Known limitations:
- ...

Next phase ready:
YES / NO
```

### Phase 2 — 2026-08-13

Status: PASS

Implemented:
- Applied the architecture lock: Hilt DI, feature-oriented screens, app navigation package, and coroutine-based `NotificationProcessor`.
- Added parser contracts, result states, registry, text normalization, VND amount parsing, direction inference, conservative generic parsing, and source skeletons.
- Added retry-safe `ParsedObservation` Room persistence with migration 1 to 2 and debug parser-result UI.
- Preserved Phase 0/1 capture, allowlist, sensitive filtering, fingerprint deduplication, and local-first behavior.

Tests/build:
- Parser, registry, privacy, deduplication, persistence, and Room migration tests pass.
- Clean debug build and Android lint pass.

Known limitations:
- No source-specific format is claimed verified until sanitized real device fixtures are supplied.
- Device notification capture still requires validation on a real Android phone.

Next phase ready:
YES

### Phase 3 — 2026-08-20

Status: PASS

Implemented:
- Added canonical transaction and observation-link Room tables with a tested 2 to 3 migration.
- Added retry-safe reconciliation: each parsed observation creates one canonical transaction unless a conservative match links it.
- Added explainable link roles, score, and reasons. Amount/time alone never merge observations; a matching non-empty reference is required at this phase.
- Added transaction list/detail UI showing linked observations and their explainability.
- Added `INTERNAL_TRANSFER` type support when a parsed observation explicitly identifies an internal transfer.

Tests/build:
- 25 unit tests pass, including one-observation creation, retry/repost protection, same-amount separation, ambiguous non-merge, explicit internal transfer, and explainable reference-based linking.
- Clean debug build and Android lint pass.

Known limitations:
- User edit/category lock UI is not implemented yet; no existing user-edit architecture existed to preserve.
- Phase 4 bank-to-wallet-to-merchant chain compatibility is intentionally not implemented.
- Source-specific formats remain fixture-driven and unverified without sanitized real device samples.

Next phase ready:
YES

### Phase 4 — 2026-08-20

Status: PASS

Implemented:
- Added a five-minute conservative correlation window and known source-route compatibility for bank to MoMo and MoMo to Grab.
- Added service enrichment, funding-source/payment-channel precedence, match-confidence updates, and explainable link reasons.
- Added a two-minute reconciliation stabilization timestamp for a future sync worker; no remote sync behavior was added.
- Added service-evidenced provisional categories for GrabFood and GrabBike/GrabCar only.

Tests/build:
- 27 unit tests pass, including Techcombank to MoMo to GrabFood, MoMo to GrabBike, same-amount separation, and ambiguous non-merge.
- Clean debug build and Android lint pass; Room schema v4 is exported.

Known limitations:
- Chain behavior is validated with structured synthetic fixtures; real source notification wording still needs sanitized device fixtures.
- No Google Sheets worker exists yet, so stabilization is persisted as forward-compatible local state only.

Next phase ready:
YES

### Phase 5 — 2026-08-20

Status: PASS

Implemented:
- Added persisted categories and categorization rules with Room migration 4 to 5.
- Added deterministic categorization engine with lock, user-rule, system-service, system-merchant, and fallback precedence.
- Seeded system service rules for GrabFood, GrabBike, GrabCar, GrabMart, and GrabExpress.
- Added manual category correction, optional category lock, create-rule-from-correction, and Rules UI with enable/disable controls.
- Removed the Phase 4 hard-coded categorization helper in favor of persisted system rules.

Tests/build:
- 33 unit tests pass, including precedence, generic Grab fallback, manual lock preservation, and user rule creation for future transactions.
- Clean debug build and Android lint pass; Room schema v5 is exported.

Known limitations:
- Category editing currently accepts a category ID; a richer category picker can be refined later.
- No Google Sheets connection exists yet.

Next phase ready:
YES

### Phase 6 — 2026-08-20

Status: PASS

Implemented:
- Đã thêm Google Identity authorization theo AuthorizationClient, chỉ kích hoạt khi người dùng bấm kết nối.
- Có màn hình cấu hình Spreadsheet URL/ID và tab đích.
- Room vẫn là nguồn dữ liệu chính; thêm Room schema v6 + migration 5→6 cho sync records.
- WorkManager dùng unique immediate/periodic work với ràng buộc mạng.
- Đồng bộ luôn dò transaction_id ở cột A trước khi append/update, nhằm tránh dòng trùng khi timeout.
- Lỗi auth chuyển sang AUTH_REQUIRED; lỗi mạng tạm thời retry; giao dịch đã sync khi chỉnh category được queue để update dòng cũ.
- Đã thêm fake-remote tests cho append/update, retry sau timeout, và auth-required.

Tests/build:
- Tất cả 36 unit tests (bao gồm 33 tests cũ + các test fake-remote mới của Phase 6) đều PASS (`.\gradlew.bat :app:testDebugUnitTest`).
- Build APK debug thành công (`.\gradlew.bat :app:assembleDebug`).
- Room schema v6 đã được export đầy đủ.

Known limitations:
- Cần tạo Android OAuth client, bật Sheets API và thử với một Google Sheet test riêng khi test trên thiết bị thật; không có test sheet thật hay credential nào được commit.

Next phase ready:
YES

### Phase 7 — 2026-09-08

Status: PASS

Implemented:
- Added `domain/dashboard/DashboardCalculator.kt`: pure-Kotlin aggregation (no Android/Room dependency) that computes income, expense, net cash flow, per-category expense totals, recent transactions, pending-sync count, and an in-month search filter from a plain `List<CanonicalTransaction>`.
- Accounting rule enforced in code: `INTERNAL_TRANSFER` transactions are excluded from income, expense, net cash flow, and category totals; they still appear in the recent-transactions list so the user can see the money movement without it being double-counted as spend/earn.
- Added `feature/dashboard/DashboardViewModel.kt` (Hilt) combining `TransactionRepository.observeAll()` + `CategorizationRepository.observeCategories()` with selected-month and search-query state into a single `DashboardSummary` StateFlow.
- Added `feature/dashboard/DashboardScreen.kt` (Compose/Material3): month selector (prev/next), income/expense/net cards, pending-sync banner, search field, category breakdown list, and a recent-transactions list showing sync state for non-synced rows.
- Wired `"dashboard"` route into `MoneyTrackerNavigation.kt` and added a Dashboard entry card to `OnboardingScreen.kt`.
- No Room schema/migration change was needed; all required fields (`amountVnd`, `type`, `categoryId`, `occurredAtEpochMs`, `syncState`) already existed from Phases 3–6.

Tests/build:
- Added `DashboardCalculatorTest.kt` (9 new fixture-driven JUnit tests, no Robolectric needed): income/expense totals per month, INTERNAL_TRANSFER excluded from income/expense/net, INTERNAL_TRANSFER excluded from category totals, category grouping/sort order, month-boundary filtering, pending-sync count is global (not month-scoped), search filter across merchant/category/note, category display-name mapping, recent-list sort+limit.
- Full suite: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL, 45 tests PASS (36 previous + 9 new), 0 failures.
- Clean debug APK assembly succeeded.

Known limitations:
- Dashboard reads the full transaction list into memory and aggregates client-side (no SQL-level aggregation query); acceptable for a single-user personal dataset per V1 scope, would need revisiting if transaction volume becomes very large.
- Search is a simple case-insensitive substring match across merchant/service/category/subcategory/description/note/fundingSource/paymentChannel; no advanced filter UI (date range picker, category multi-select, amount range) beyond the month selector and free-text search, per PLANS.md "useful filters/search" (not specified as more granular).
- Category names in the dashboard come from `CategorizationRepository.observeCategories()`; if a transaction's `categoryId` has no matching row (e.g. deleted/renamed category), the raw id string is shown as a fallback display name.

Next phase ready:
YES


## Architecture baseline for Phase 2+

- Hilt is the dependency-injection framework.
- Screen-oriented packages live under `feature/`, not generic `ui/`.
- Top-level navigation lives under `app/navigation/`.
- `NotificationProcessor` performs normal coroutine-based processing after raw persistence.
- WorkManager is for durable Google Sheets sync/recovery, not one job per notification.
- V1 remains one Gradle `:app` module.
