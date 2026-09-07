# PLANS.md — Implementation Plan

Follow this plan in order unless an existing repository state makes a step obsolete.

Do not attempt all phases in one giant change. Keep each phase buildable.

## Phase 0 — Bootstrap

### Goal
Create a minimal Android project foundation.

### Tasks
- Kotlin + Compose + Material 3.
- Navigation shell.
- Room dependency and empty DB.
- WorkManager dependency.
- Basic repository interfaces.
- Basic unit-test setup.
- Add debug/release build configuration.

### Done when
- app builds;
- app launches;
- tests run.

---

## Phase 1 — Notification ingestion

### Goal
Capture allowlisted notifications safely.

### Tasks
- Implement `NotificationListenerService`.
- Add manifest declaration and required binding permission.
- Onboarding button to open Notification Access settings.
- Detect current listener access status.
- Add tracked-package allowlist.
- Extract title/text/bigText/subText/package/time.
- Add sensitive-content filter.
- Persist `RawNotificationEvent`.
- Add exact notification fingerprint.
- Add debug event list.

### Done when
A synthetic/test notification from an allowed package can be captured and viewed locally without network access.

---

## Architecture lock before Phase 2

This is a one-time structural alignment, not a new product phase. Preserve all completed Phase 0/1 behavior.

### Required alignment
- Add/configure Hilt if not already present.
- Annotate the application entry point with `@HiltAndroidApp`.
- Prefer Hilt constructor injection; add modules only where needed.
- Move screen-oriented packages from `ui/` to `feature/`.
- Keep top-level navigation under `app/navigation/`.
- Add/use `notification/NotificationProcessor` for coroutine-based processing after raw-event persistence.
- Put WorkManager-based durable Google Sheets work under `sync/`.
- Do not create one WorkRequest per notification for normal parsing.
- Keep a single Gradle `:app` module for V1.

### Validation before Phase 2 logic
- clean build passes;
- existing Phase 0/1 tests pass;
- notification access/capture still works;
- Hilt graph resolves;
- package moves do not remove working behavior.

## Phase 2 — Parser framework

### Status
CURRENT PHASE

### Goal
Turn captured `RawNotificationEvent` records into deterministic, testable `ParsedObservation` records without guessing unsupported source-specific formats.

### Tasks

#### Parser contracts
- Define `NotificationParser`.
- Define `ParseResult`:
  - `Parsed`
  - `Ignored`
  - `Unparsed`
  - `SensitiveIgnored` if consistent with the existing Phase 1 pipeline.
- Define `ParserRegistry`.
- Keep parser/domain logic independent from Compose UI.

#### Common normalization
- Normalize whitespace and multiline text.
- Normalize Unicode/currency symbols where safe.
- Preserve the original raw event separately.
- Do not destroy information needed for later source-specific parsing.

#### VND amount parsing
Support, with tests:

```text
125.000đ
125,000 VND
125 000 ₫
-125.000
+5.000.000đ
```

Requirements:
- store money as `Long`;
- never use `Float`/`Double` for transaction amounts;
- do not blindly choose the first number when several numeric candidates exist;
- expose ambiguity when the amount cannot be selected reliably.

#### Direction inference
Support:

```text
DEBIT
CREDIT
TRANSFER
UNKNOWN
```

Use source-specific semantics when available.
Generic keyword inference must remain conservative.

#### Generic parser
- Implement a low-confidence generic financial parser.
- It may parse obvious financial structure.
- It must return `Unparsed` rather than invent unsupported fields.

#### Source parser structure
Create conservative source-specific parser implementations/skeletons for:

```text
MoMo
Timo
Techcombank
Grab
```

Important:
Do not claim exact real notification wording unless supported by sanitized fixtures captured from the user's device.

#### Persistence
- Persist `ParsedObservation` in Room.
- Link it to its source `RawNotificationEvent`.
- Store parser name/version and confidence.
- Update raw-event processing status appropriately.

#### Pipeline
Implement:

```text
RawNotificationEvent
        ↓
NotificationProcessor
        ↓
ParserRegistry
        ↓
ParseResult
        ↓
ParsedObservation / Ignored / Unparsed
```

Processing should remain retry-safe.

#### Debug UI
Extend the existing debug screen so the user can inspect:

```text
source package
raw captured fields
parser selected
parse result
amount
direction
merchant
service
confidence
reason for Ignored/Unparsed
```

Do not expose sensitive-ignored original content.

#### Fixture-driven tests
Add sanitized/synthetic fixtures and tests for at least:

- VND dot separator;
- VND comma separator;
- VND space separator;
- plus amount;
- minus amount;
- debit;
- credit;
- transfer;
- unknown direction;
- missing amount;
- multiple numeric values;
- multiline notification;
- unrecognized notification;
- generic parser;
- parser-registry routing;
- source-parser fallback;
- sensitive notification handling where applicable.

Do not commit real:
- account numbers;
- phone numbers;
- names;
- OTP/passcodes;
- transaction references;
- private financial notes.

### Important
Source-specific parsers remain conservative until sanitized real samples are collected.

Unknown or ambiguous input must be represented explicitly rather than guessed.

### Done when
- parser contracts exist;
- VND amount parsing is tested;
- direction inference is tested;
- parser registry routes correctly;
- generic parser works conservatively;
- source parser skeletons exist;
- parsed observations persist in Room;
- debug UI exposes parser results;
- relevant build/tests pass;
- unknown input is safely marked `UNPARSED`;
- no Phase 3 matching/canonical-transaction feature is accidentally implemented.

---

## Phase 3 — Canonical transaction creation

### Goal
Create stable canonical transactions from parsed observations while preventing obvious duplicates.

### Entry condition
Phase 2 must be complete and parser fixtures/tests must pass.

### Tasks
- Create canonical transaction entity/domain model.
- Create observation-to-transaction link table.
- Implement exact duplicate detection.
- Create match-scoring abstraction.
- Implement a conservative matcher.
- Add `INTERNAL_TRANSFER` transaction type.
- Reconcile new observations with existing candidate transactions.
- Add transaction list/detail UI.
- Preserve user edits.
- Keep matching explainable.

### Required tests
- one observation -> one transaction;
- notification repost -> no duplicate;
- two unrelated same-amount purchases -> two transactions;
- ambiguous observations are not force-merged.

### Done when
Canonical transaction creation is stable, explainable, tested, and does not merge merely because amounts match.

---

## Phase 4 — Cross-app enrichment

### Goal
Recognize multiple app notifications as observations of one real-world payment.

### Entry condition
Phase 3 canonical transaction flow passes tests.

### Tasks
- Add known payment-chain compatibility concepts:
  - bank -> MoMo;
  - MoMo -> Grab.
- Add configurable correlation time window.
- Add service enrichment from merchant/service apps.
- Add confidence scoring for cross-app matching.
- Add short reconciliation/stabilization delay before first remote sync.
- Add linked-observation explainability in transaction detail.

### Required cases
```text
Techcombank -> MoMo -> GrabFood
=> one Food expense
```

```text
MoMo -> GrabBike
=> one Transportation expense
```

```text
two unrelated transactions with same amount
=> remain separate
```

### Done when
Supported fixture chains form one canonical transaction with correct funding source, payment channel, merchant, service, and no double-counting.

---

## Phase 5 — Categorization rules

### Goal
Deterministic categorization with explicit user control.

### Tasks
- Seed category table.
- Create rule table.
- Implement categorization engine.
- Add system service rules.
- Add manual user correction.
- Add `Create rule from correction`.
- Add user category lock.
- Add Rules UI.
- Preserve precedence:

```text
user locked category
> user rule
> system service rule
> system merchant rule
> fallback
```

### Initial service rules
```text
GrabFood -> Food
GrabBike -> Transportation
GrabCar -> Transportation
GrabMart -> Shopping/Groceries
GrabExpress -> Other/Delivery
```

Do not classify generic `Grab` as Food or Transportation without service evidence or an explicit user rule.

### Done when
Specific Grab services can be categorized differently and manual user corrections always win.

---

## Phase 6 — Google Sheets connection

### Goal
Synchronize canonical transactions to a user-configured Google Sheet without making Google Sheets the source of truth.

### Entry condition
Canonical transactions and user editing are stable.

### Tasks
- Implement explicit Google authorization flow.
- Add spreadsheet URL/ID setting.
- Add target sheet/tab setting.
- Validate/test access.
- Add Sheets API repository/client.
- Add canonical row mapper.
- Put stable `transaction_id` in column A.
- Add WorkManager sync worker under `sync/` and integrate it with the Hilt dependency graph using the supported Hilt/WorkManager pattern for the project toolchain.
- Add:
  - `NOT_SYNCED`
  - `QUEUED`
  - `SYNCING`
  - `SYNCED`
  - `AUTH_REQUIRED`
  - `FAILED`
- Implement idempotent append.
- Implement update of an already-synced edited transaction.
- Handle offline retry/backoff.
- Handle authorization recovery separately from transient network retry.

### Non-negotiable
- Room remains source of truth.
- No service-account private key in APK.
- No Google password collection.
- A retry after an ambiguous timeout must not create duplicate rows.
- Local notification capture continues even if Google sync is broken.

### Done when
A transaction created offline later appears exactly once in a test Google Sheet and later edits update the same remote transaction.

---

## Phase 7 — Dashboard

### Goal
Provide useful day-to-day financial visibility.

### Tasks
- month selector;
- income total;
- expense total;
- net cash flow;
- category totals;
- recent transactions;
- filters/search;
- pending sync indicator.

### Accounting rules
Exclude `INTERNAL_TRANSFER` from income and expense totals.

### Done when
Dashboard totals match a manually verified fixture dataset.

---

## Phase 8 — Real notification fixture collection

### Goal
Adapt conservative parser skeletons to the actual notification formats on the user's Android phone.

### Process
For each source:

1. enable tracking;
2. receive representative notifications;
3. inspect through debug screen;
4. sanitize the sample;
5. add fixture;
6. define expected parser output;
7. implement/adjust parser;
8. run regression tests.

### Target samples

MoMo:
- payment;
- transfer/top-up;
- linked-bank payment if present;
- Túi Thần Tài movement.

Timo:
- debit;
- credit;
- transfer/payment variants encountered.

Techcombank:
- debit;
- credit;
- transfer/card/payment variants encountered.

Grab:
- GrabFood;
- GrabBike;
- GrabCar;
- other services if used.

### Privacy
Never commit personally identifying raw samples.

### Done when
The user's commonly encountered notification formats have sanitized regression fixtures and pass parser tests.

---

## Phase 9 — Hardening & V1 audit

### Goal
Make the app reliable enough for daily personal use.

### Tasks
- reboot/device lifecycle tests;
- foreground/background tests;
- internet loss/recovery;
- battery/background behavior checks;
- Room migration tests;
- sync retry/backoff;
- HTTP 429/5xx handling;
- Google authorization recovery;
- release logging audit;
- privacy review;
- backup-policy review;
- local data clear/export tooling;
- acceptance-criteria audit.

### Done when
`docs/10_ACCEPTANCE_CRITERIA.md` is fully reviewed and every V1 MUST item is PASS or has an explicitly accepted limitation.
