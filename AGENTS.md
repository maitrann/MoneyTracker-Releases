# AGENTS.md — Personal Money Tracker

## Mission

Build a private Android app for one user that captures financial transaction notifications from apps such as MoMo, Timo, Techcombank, and Grab; normalizes and de-duplicates them; categorizes spending; stores the canonical data locally; and synchronizes canonical transactions to a user-selected Google Sheet.

Read the relevant documents in `docs/` before implementing or changing a subsystem.

## Non-negotiable rules

1. Use Kotlin and Jetpack Compose.
2. Use a local-first architecture. Room is the source of truth. Google Sheets is a synchronization/backup/report destination, not the primary database.
3. Use `NotificationListenerService` for notification ingestion.
4. Do NOT use Accessibility Service to scrape other apps.
5. Do NOT request, capture, store, log, or transmit bank passwords, PINs, Smart OTP, OTP, card CVV, or authentication secrets.
6. Ignore notifications that appear to contain OTP/passcodes.
7. Do NOT automate login into MoMo, Timo, Techcombank, Grab, or any bank.
8. Do NOT reverse-engineer private banking APIs.
9. Do NOT count transfers between the user's own accounts/wallets as expenses or income.
10. A payment observed by multiple apps must become ONE canonical transaction.
11. Preserve raw notification events separately from canonical transactions so parser/matcher behavior can be improved later.
12. Never log full financial notification contents to Logcat in release builds.
13. Do not hard-code assumptions about exact notification wording until sample notifications are available.
14. Source-specific parsers must fail safely and return `Unparsed` rather than inventing fields.
15. Every important parser, matcher, categorizer, and sync behavior needs tests.
16. Prefer the latest stable Android/Jetpack/Google libraries compatible with the project toolchain. Do not blindly pin versions copied from old examples.
17. Do not add AI/LLM classification in V1. Start with deterministic rules and user correction.
18. No backend server is required for V1.

## Suggested stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM / unidirectional data flow
- Coroutines + Flow
- Room
- Hilt for dependency injection
- WorkManager for persistent sync/recovery work only
- NotificationListenerService
- Google Identity authorization for Google user data
- Google Sheets API
- Repository layer around Room and remote sync
- JUnit + AndroidX test libraries

## Repository structure

Prefer:

```text
app/
  src/main/java/.../
    data/
      local/
      remote/
      repository/
    domain/
      model/
      parser/
      matching/
      categorization/
      sync/
    notification/
    sync/
    feature/
      onboarding/
      dashboard/
      transactions/
      transactiondetail/
      rules/
      debug/
      settings/
    di/
docs/
AGENTS.md
PLANS.md
README.md
```

Keep parsing/matching/categorization logic free of Android UI dependencies so it can be unit tested.

## Core invariant

One real-world financial event = one canonical transaction.

Example:

```text
Techcombank: -120,000 payment to MoMo
MoMo:       -120,000 payment to Grab
Grab:        120,000 GrabFood order

=> ONE transaction:
amount = 120000
type = EXPENSE
fundingSource = TECHCOMBANK
paymentChannel = MOMO
merchant = GRAB
service = GRAB_FOOD
category = FOOD
```


## Dependency injection

Use Hilt as the project dependency-injection framework. Prefer constructor injection for repositories, processors, parser registries, matchers, categorization engines, use cases, and ViewModels. Use Hilt modules only where constructor injection is impossible or an interface/provider binding is required.

Do not use a global service locator or manually construct large dependency graphs inside Activities, Composables, Services, or ViewModels.

## Background work policy

Do not enqueue one WorkManager job per notification as the normal parsing path.

Normal ingestion:

```text
NotificationListenerService
  -> persist RawNotificationEvent
  -> NotificationProcessor / coroutine processing
  -> ParserRegistry
  -> Room
```

Use WorkManager for work that must survive process/app restarts or depends on deferred constraints, especially:
- Google Sheets synchronization;
- retry/backoff after network loss;
- optional recovery of persisted unfinished work.

Persisted raw events are the recovery source if the process dies after capture.

## Build discipline

For every implementation task:

1. Inspect existing code and relevant docs.
2. State assumptions in code comments only when they matter.
3. Implement the smallest complete slice.
4. Add/update tests.
5. Run formatting/build/tests available in the environment.
6. Review the diff for privacy leaks, duplicate counting, destructive migrations, and sync duplication.
7. Update docs if behavior or schema changed.

## Definition of done

A change is not done until:
- it compiles;
- relevant tests pass;
- no secret is committed;
- no raw financial content is unnecessarily logged;
- offline behavior remains safe;
- sync is idempotent;
- canonical transaction totals are not double-counted.
