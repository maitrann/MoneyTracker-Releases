# 06 — Google Sheets Synchronization

## 1. Principle

Room is the source of truth.

Google Sheets is:
- backup;
- external reporting;
- manual inspection;
- later integration point for Excel/Power BI/Python.

A failed Google sync must never delete or block local transaction capture.

## 2. Preferred remote format

Use a native Google Spreadsheet, not an `.xlsx` file stored in Drive.

Reason: Sheets API supports direct row append/update semantics.

## 3. Authentication / authorization

Use Google Identity authorization APIs for access to Google user data.

V1 simplest path:

- user taps `Connect Google Sheets`;
- request only the scope required for the selected implementation;
- store no Google password;
- keep tokens out of logs;
- if authorization can no longer be silently obtained, mark sync `AUTH_REQUIRED` and ask the user to reconnect from the UI.

Scope options:

### Option A — simpler V1
Use the Sheets scope when the user manually provides a spreadsheet URL/ID.

This grants broader Google Sheets access and is a sensitive scope.

### Option B — preferred narrower-access evolution
Use per-file access (`drive.file`) with a proper file creation/selection flow so the app gets access only to the chosen file.

Do not use the full Drive scope unless there is a concrete need.

Do not put a service-account private key inside the APK.

## 4. User configuration

Settings:

```text
Google authorization: Connected / Not connected
Spreadsheet URL or ID
Target sheet/tab: Transactions
Last sync time
Pending rows
Sync now
```

Parse a normal Google Sheets URL to extract `spreadsheetId` when possible.

Validate access before saving configuration.

## 5. Transactions sheet schema

Recommended columns:

| Column | Field |
|---|---|
| A | transaction_id |
| B | occurred_at_iso |
| C | type |
| D | amount_vnd |
| E | currency |
| F | merchant |
| G | service |
| H | category |
| I | subcategory |
| J | funding_source |
| K | payment_channel |
| L | description |
| M | note |
| N | match_confidence |
| O | classification_confidence |
| P | source_apps |
| Q | created_at_iso |
| R | updated_at_iso |
| S | deleted_or_excluded |

The first row is a header.

`transaction_id` is mandatory.

## 6. Append behavior

For a brand-new stable canonical transaction:

1. mark `QUEUED`;
2. WorkManager finds pending rows;
3. authorize/access token if available without UI;
4. call Sheets values append;
5. on success, persist remote row metadata if available;
6. mark `SYNCED`.

Use `USER_ENTERED` or `RAW` consistently. Prefer `RAW` if all formatting/formulas are managed by the sheet itself and exact values are desired.

## 7. Update behavior

A user may edit a transaction after it was synced.

The app needs a deterministic way to update the same remote transaction.

Do not trust row number forever.

Safe strategies:

### V1
Before update, search/read the transaction ID column to find the current row, then update that row.

### Later optimization
Maintain a local `transaction_id -> row` cache and validate it before destructive updates.

Never append a second row simply because a previous transaction was edited.

## 8. Idempotency

Every sync operation must be safe to retry.

Before appending a transaction that may have succeeded during a network timeout, verify whether `transaction_id` already exists remotely.

Potential sequence:

```text
request sent
server writes row
network response lost
worker retries
```

Without idempotency, this creates duplicate rows.

## 9. WorkManager

Use WorkManager for persistent sync.

Constraints:

```text
NetworkType.CONNECTED
```

Use retry with backoff for:
- transient network failure;
- HTTP 429;
- transient 5xx.

Do not retry indefinitely at tight intervals.

Do not retry authorization-resolution errors as network errors.

## 10. Batch sync

If several transactions are pending, batch operations where practical to reduce API requests.

Keep batch size conservative.

## 11. Conflict policy

Local app wins for V1.

Google Sheets is not treated as a two-way editable database in V1.

If the user manually edits the Sheet, the app does not automatically import those changes.

This avoids a large conflict-resolution problem.

A future version can add two-way sync explicitly.

## 12. Sheet tabs

Required V1:

```text
Transactions
```

Optional later:

```text
Categories
Rules
Accounts
Monthly_Summary
```

Keep app configuration/rules local first. Do not block V1 on synchronizing all tabs.

## 13. Monthly summary

Prefer formulas/pivot tables in the Google Sheet rather than syncing precomputed totals.

Key accounting rule:

```text
Total Expense = SUM(EXPENSE)
Total Income  = SUM(INCOME)
Internal transfers excluded
```

## 14. Failure UX

Examples:

```text
Synced
12 pending
Needs Google authorization
Spreadsheet not found / permission denied
Sheet tab missing
Rate limited; will retry
```

Never silently drop failed rows.
