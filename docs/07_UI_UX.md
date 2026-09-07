# 07 — UI/UX Specification

## Feature package convention

Screen-oriented code lives under `feature/<name>/`, not a generic `ui/` package. Keep the Screen, ViewModel, UiState, and feature-specific components together. Top-level navigation belongs under `app/navigation/`.


V1 should prioritize correctness and inspectability over visual complexity.

Use Material 3 and Jetpack Compose.

## 1. Onboarding

### Step 1 — Welcome

Explain:

```text
This app reads transaction notifications you choose,
stores them locally, and can sync normalized records
to your Google Sheet.
```

### Step 2 — Notification Access

Show status and button:

```text
Notification Access: Not enabled
[ Open Android settings ]
```

After returning, re-check permission.

### Step 3 — Select sources

List notification-producing apps discovered on device or allow manual package entry in debug mode.

Initial labels:

```text
MoMo
Timo
Techcombank
Grab
```

Only enabled sources are processed.

### Step 4 — Google Sheets

```text
[ Connect Google ]
Spreadsheet URL / ID
Sheet name: Transactions
[ Test connection ]
```

Google sync may be skipped and configured later.

## 2. Dashboard

Month selector.

Cards:

```text
Income
Expense
Net cash flow
Pending sync
```

Category breakdown.

Recent transactions.

Internal transfers are excluded from income/expense totals by default.

## 3. Transaction list

Each item:

```text
GrabFood                       -125,000 ₫
Food • MoMo • Techcombank
13 Aug 2026, 11:35            Synced
```

Filter/search controls.

## 4. Transaction detail

Editable fields:

```text
Amount
Date/time
Type
Merchant
Service
Category
Subcategory
Funding source
Payment channel
Note
```

Read-only diagnostic section:

```text
Match confidence
Classification confidence
Linked observations
Sync status
Transaction ID
```

Actions:

```text
Save
Create rule from this correction
Retry sync
Exclude from reports
```

## 5. Quick note/edit notification

Optional after core V1 works.

The app can post its own notification:

```text
Recorded 125,000 ₫ — GrabFood
Food

[ Add note ] [ Edit ]
```

Do not require this feature for the first end-to-end milestone.

## 6. Rules screen

List:

```text
IF service = GrabFood
THEN category = Food

IF merchant contains CGV
THEN category = Entertainment
```

Capabilities:
- enable/disable;
- edit;
- delete user rule;
- reorder user-rule priority if needed.

System rules should be visibly marked.

## 7. Settings

Sections:

### Notification sources
- access status
- tracked apps

### Google Sheets
- authorization status
- spreadsheet ID/name
- target tab
- last sync
- pending count
- sync now

### Matching
- default correlation window
- advanced debug controls

### Privacy
- clear local debug/raw data
- export sanitized debug samples
- app lock can be considered later

## 8. Debug screen

Developer/debug build only by default.

Show raw captured events with filters:

```text
All
Parsed
Unparsed
Ignored
Sensitive ignored
```

Selecting an event shows parser output.

Provide a copy/export action that sanitizes:
- account numbers;
- phone numbers;
- long references;
- names where possible.

Never expose sensitive-ignored original content.

## 9. Empty/error states

Examples:

```text
No transactions yet.
Make a supported transaction after enabling notification access.
```

```text
12 transactions are safely stored locally and waiting for internet.
```

```text
Google authorization needs attention.
Local tracking is still active.
```

The UI must reinforce that sync failure does not mean data loss.
