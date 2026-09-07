# 01 — Product Requirements

## 1. Product objective

Create a private Android money-tracking app that records the user's real cash flow automatically from financial/payment notifications while avoiding duplicate accounting across payment layers.

The app should answer questions such as:

- How much did I spend this month?
- How much did I spend on food?
- How much did I spend on transportation?
- How much money ultimately came from Techcombank, Timo, or MoMo?
- How much did I spend through GrabFood vs GrabBike?
- Which transfers are merely movement between my own accounts?
- What was a particular payment for?
- Has every local transaction been synchronized to my Google Sheet?

## 2. User

V1 has exactly one local user.

No app account is required.

## 3. Tracked sources

Initial target sources:

- MoMo
- Timo
- Techcombank
- Grab

The architecture must support adding more source adapters later.

Do not assume package names until verified on the user's phone. Store allowlisted package names in configuration.

## 4. Functional requirements

### FR-01 — Notification permission onboarding

The app explains why Notification Access is needed and provides a button to open the Android system screen where the user enables access.

The app must clearly show:
- permission granted / not granted;
- number of selected source apps;
- last successfully captured event.

### FR-02 — Source allowlist

Only process notification packages explicitly enabled by the user.

Unknown packages are ignored by default.

### FR-03 — Raw event capture

For each allowed notification, capture the useful fields exposed by Android, including when available:

- package name
- notification key/id
- post time
- title
- text
- big text
- sub text
- category
- channel id
- selected safe metadata

Store raw events in app-private Room storage.

### FR-04 — Parser

Convert a raw event to zero or one parsed financial observation.

A parsed observation can contain:

- amount
- currency
- direction hint
- source account/app
- merchant
- service
- payment channel
- transaction/reference hint
- description
- event time
- confidence
- parser name/version

If a parser is uncertain, do not invent data.

### FR-05 — Canonical transaction

Related observations should resolve to one canonical transaction.

Transaction types:

- `EXPENSE`
- `INCOME`
- `INTERNAL_TRANSFER`
- `REFUND`
- `UNKNOWN`

### FR-06 — Duplicate prevention

The same notification re-posted or the same financial event observed by several apps must not create duplicate expense totals.

### FR-07 — Internal transfer handling

Moving money between the user's own accounts/wallets is not spending and is not income.

Examples:

- Techcombank -> MoMo balance
- Timo -> MoMo
- MoMo balance -> Túi Thần Tài
- Túi Thần Tài -> MoMo balance

These may be recorded for traceability as `INTERNAL_TRANSFER`, but dashboards must exclude them from total income/expense unless the user explicitly requests transfer analytics.

### FR-08 — Categorization

V1 uses deterministic rules.

Initial categories:

- Food
- Transportation
- Shopping
- Bills
- Entertainment
- Education
- Health
- Housing
- Subscriptions
- Gifts
- Travel
- Cash
- Transfer
- Income
- Other
- Unclassified

Example service rules:

- `GrabFood` -> Food
- `GrabBike` -> Transportation
- `GrabCar` -> Transportation
- `GrabMart` -> Shopping/Groceries
- `GrabExpress` -> Delivery/Other

The exact rule set must be editable.

### FR-09 — User correction

A user can edit:

- transaction type
- merchant
- service
- category
- subcategory
- funding source
- payment channel
- note
- date/time
- amount, only with a warning because it affects matching

### FR-10 — Learn from correction

The user may create a reusable rule from a correction.

Example:

```text
merchant contains "CGV"
=> category = Entertainment
```

Do not silently create rules without user confirmation in V1.

### FR-11 — Notes

Every transaction can have a free-text personal note.

### FR-12 — Local-first persistence

Every captured event and canonical transaction is saved locally before cloud sync is attempted.

### FR-13 — Google Sheets sync

The user configures a Google Spreadsheet and a target sheet/tab.

The app synchronizes canonical transactions.

Offline transactions remain queued and retry later.

### FR-14 — Sync state

Each transaction has a visible sync state:

- Not synced
- Syncing
- Synced
- Needs auth
- Failed

### FR-15 — Dashboard

At minimum show for a selected month:

- total income
- total expense
- net cash flow
- spending by category
- recent transactions

Exclude `INTERNAL_TRANSFER` from income/expense totals.

### FR-16 — Search/filter

Filter transactions by:

- date range
- type
- category
- merchant/service
- funding source
- payment channel
- sync state

### FR-17 — Debug/sample mode

Provide a developer/debug screen that lets the user inspect recently captured notification fields for tracked apps.

This is essential because real notification wording may differ by:
- app version;
- Android version;
- language;
- transaction type.

Debug data stays local unless the user explicitly exports it.

## 5. Non-functional requirements

### Reliability

A temporary lack of internet must not lose data.

### Privacy

Financial notification data stays local except the canonical fields intentionally synchronized to Google Sheets.

### Performance

Notification ingestion should return quickly. Heavy matching or sync work should be offloaded from the notification callback.

### Extensibility

Each source parser should be isolated behind an interface.

### Maintainability

Rules and parser tests should use fixtures so notification wording changes are easy to adapt.

## 6. Success metric for V1

For a manually reviewed sample of at least 50 real transaction events:

- no lost locally captured notifications from allowlisted apps under normal phone use;
- no known double-counted linked payment chains;
- at least 90% of supported-format notifications get a correct amount and direction;
- category can always be corrected manually;
- synced rows can be traced back to a stable transaction ID.
