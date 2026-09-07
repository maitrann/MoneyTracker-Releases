# 09 — Test Plan

## 1. Test strategy

The highest-risk areas are:

1. amount parsing;
2. transaction direction;
3. duplicate detection;
4. cross-app matching;
5. internal transfer classification;
6. category precedence;
7. Google Sheets idempotency;
8. offline retry;
9. privacy filtering.

Favor many small deterministic unit tests.

## 2. Parser unit tests

For each source parser create sanitized fixtures.

Test:

```text
VND with dot separator
VND with comma separator
VND with spaces
plus amount
minus amount
debit wording
credit wording
missing amount
multiple numeric values
reference number next to amount
multiline notification
notification update/repost
unrecognized wording
```

Expected parser behavior must be explicit.

## 3. Sensitive-content tests

Inputs containing:

```text
OTP
mã OTP
verification code
Smart OTP
one-time password
```

should be rejected from raw full-text persistence according to the sensitive-content policy.

Also test that normal amount-only financial notifications are not falsely discarded.

## 4. Matcher tests

### Case A — bank + MoMo + GrabFood

```text
Techcombank -120k
MoMo -120k Grab
Grab 120k GrabFood
```

Expected: one `EXPENSE`, Food.

### Case B — MoMo + GrabBike

Expected: one `EXPENSE`, Transportation.

### Case C — two separate same-amount purchases

```text
Coffee shop -50k
Convenience store -50k
```

Expected: two transactions.

### Case D — internal top-up

```text
Techcombank -> MoMo 1,000k
```

Expected: `INTERNAL_TRANSFER`.

### Case E — top-up then purchase

```text
Techcombank -> MoMo 1,000k
MoMo -> GrabFood 120k later
```

Expected:
- one transfer 1,000k;
- one expense 120k.

### Case F — notification re-post

Expected: no duplicate transaction.

### Case G — later service enrichment

```text
MoMo -125k Grab
then GrabFood detail arrives
```

Expected:
- same transaction ID;
- service becomes GrabFood;
- category becomes Food;
- no second row locally.

## 5. Categorization tests

Priority order:

```text
user locked category
> user rule
> service system rule
> merchant system rule
> fallback
```

Test that a manual correction stays intact when reconciliation adds new observations.

## 6. Room tests

Test:

- insert raw event;
- unique/fingerprint constraint behavior;
- transaction + observation links;
- migration tests after first shipped schema;
- pending sync query;
- user edit update timestamps.

## 7. WorkManager/sync tests

Test:

- no network -> queued;
- network restored -> sync;
- 429 -> retry/backoff;
- 5xx -> retry;
- 401/authorization issue -> `AUTH_REQUIRED`, not tight retry;
- timeout after server append -> retry does not duplicate row;
- edited synced transaction -> update same transaction remotely.

Use a fake Sheets API layer for most automated tests.

## 8. Google integration test

A manual/dev integration test can use a dedicated test spreadsheet.

Verify:

- header present;
- row appended;
- amount is numeric;
- transaction ID preserved;
- edit updates intended row;
- retry does not duplicate.

Never run integration tests against the user's real finance spreadsheet by default.

## 9. UI tests

Minimum:

- onboarding permission state;
- transaction list renders;
- edit category/note persists;
- internal transfer excluded from dashboard totals;
- pending sync state displayed;
- Google auth-required state displayed.

## 10. Device tests

Use a real Android phone for final notification tests.

Test scenarios:

- screen on/off;
- app foreground/background;
- source app foreground/background;
- device reboot;
- internet disconnected/reconnected;
- battery optimization conditions;
- notification access toggled off/on.

## 11. Regression fixtures

Every time a real notification format breaks parsing:

1. sanitize the sample;
2. add it as a failing fixture;
3. fix parser;
4. keep fixture permanently.

This is the main maintenance strategy for bank/payment app format changes.


## 12. Architecture regression checks

Verify:
- ViewModels do not construct repositories manually.
- Compose screens do not access Room DAOs directly.
- Hilt graph resolves for application, ViewModels, notification dependencies, repositories, and sync dependencies.
- Normal notification processing does not enqueue one WorkManager request per notification.
- WorkManager is reserved for durable sync/recovery.
- Parser/matcher/categorization logic remains unit-testable independently of Compose UI.
