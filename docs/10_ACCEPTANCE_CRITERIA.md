# 10 — V1 Acceptance Criteria

V1 can be considered usable when all MUST items below are satisfied.

## A. Project/build

- [ ] Android project builds from a clean checkout.
- [ ] Kotlin + Compose app launches on a real Android phone.
- [ ] Hilt application setup and dependency graph build successfully.
- [ ] No banking credentials or secrets are committed.
- [ ] Core parser/matcher logic has automated tests.

## B. Notification ingestion

- [ ] User can open Notification Access settings from onboarding.
- [ ] App can detect whether notification listener access is granted.
- [ ] Only selected/allowlisted packages are processed.
- [ ] Raw allowed notification events are saved locally.
- [ ] Normal notification parsing does not require one WorkManager job per notification.
- [ ] Likely OTP/passcode notifications are not stored as normal raw finance events.
- [ ] Re-posted identical notifications do not create repeated finance observations.

## C. Parsing

- [ ] Parser registry exists.
- [ ] Generic parser exists.
- [ ] Source parser interfaces exist for MoMo/Timo/Techcombank/Grab.
- [ ] Amount parsing supports common VND separators.
- [ ] Unknown format is represented as unparsed, not guessed.
- [ ] Debug/sample screen exposes sanitized captured fields.

## D. Canonical transactions

- [ ] Parsed observation can create a canonical transaction.
- [ ] Multiple observations can link to one transaction.
- [ ] Transaction detail can show linked observations.
- [ ] Internal transfer is a distinct transaction type.
- [ ] Internal transfers are excluded from expense/income dashboard totals.
- [ ] User can edit category and note.
- [ ] User can lock/correct category without reconciliation overwriting it.

## E. Matching

- [ ] Same-event exact duplicates are prevented.
- [ ] Cross-app matching uses more than amount alone.
- [ ] `bank -> MoMo -> GrabFood` can result in one transaction when supported fixtures match.
- [ ] Two unrelated same-amount purchases remain separate.
- [ ] Later service detail enriches an existing transaction instead of creating another expense.

## F. Categorization

- [ ] GrabFood -> Food rule supported.
- [ ] GrabBike/GrabCar -> Transportation rule supported.
- [ ] User rules override system rules.
- [ ] Uncertain merchant-only Grab transaction is not automatically forced into Food/Transportation without service evidence or user rule.

## G. Local-first behavior

- [ ] Transaction remains available when internet is off.
- [ ] Unsynced transaction is visibly marked.
- [ ] App restart does not lose transactions.
- [ ] Sync failure never deletes local transactions.

## H. Google Sheets

- [ ] User can authorize Google access from an explicit action.
- [ ] User can configure spreadsheet ID/URL and target tab.
- [ ] Connection can be tested.
- [ ] Canonical transaction appends to `Transactions`.
- [ ] First remote column is stable `transaction_id`.
- [ ] Edited transaction updates the intended remote record.
- [ ] Retry after ambiguous network failure is idempotent.
- [ ] Offline transactions sync after network returns.
- [ ] Authorization problems result in `AUTH_REQUIRED`.
- [ ] No Google password/token appears in logs.

## I. Usability

- [ ] Screen-oriented code is organized under `feature/` packages, with top-level navigation under `app/navigation/`.
- [ ] Dashboard shows income, expense, net cash flow.
- [ ] Transaction list exists.
- [ ] Transaction detail/edit exists.
- [ ] Settings show notification and sync status.
- [ ] Debug screen helps capture real source formats.

## J. Privacy review

- [ ] No Accessibility Service.
- [ ] No bank login automation.
- [ ] No private bank API reverse engineering.
- [ ] No service-account secret embedded in APK.
- [ ] Release logging is sanitized.
