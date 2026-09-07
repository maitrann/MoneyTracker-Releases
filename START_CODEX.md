# START_CODEX.md — Continue from Phase 2

## Current project state

Assume:

```text
Phase 0 — Bootstrap: COMPLETE
Phase 1 — Notification ingestion: COMPLETE
Phase 2 — Parser framework: NEXT
```

Do not redo Phase 0 or Phase 1 unless repository inspection finds a regression or a missing prerequisite that blocks the current phase.

Before every phase:
1. read `AGENTS.md`;
2. read `PROJECT_STATUS.md`;
3. read the relevant section of `PLANS.md`;
4. inspect the repository instead of assuming the docs perfectly match the code;
5. run the existing relevant tests before changing behavior.

After every phase:
1. run relevant tests/build;
2. audit for privacy/security regressions;
3. update `PROJECT_STATUS.md`;
4. report `PASS`, `PARTIAL`, or `FAIL`;
5. do not begin the next phase automatically.

---

# Prompt — Start Phase 2

Paste this into Codex:

```text
Project Phase 0 and Phase 1 are complete. Apply the one-time architecture lock in `PLANS.md`, then begin Phase 2 only.

Before changing code, read:
- AGENTS.md
- PROJECT_STATUS.md
- README.md
- PLANS.md
- docs/02_ARCHITECTURE.md
- docs/03_DATA_MODEL.md
- docs/04_NOTIFICATION_PIPELINE.md
- docs/08_SECURITY_PRIVACY.md
- docs/09_TEST_PLAN.md
- docs/10_ACCEPTANCE_CRITERIA.md

Then inspect the current repository and verify the actual Phase 0/1 implementation.

Do not redo completed Phase 0/1 behavior.

Before Phase 2 parser work, apply the architecture lock from `PLANS.md`:
- add/configure Hilt if needed;
- replace manual dependency construction with Hilt where appropriate;
- move screen packages from `ui/` to `feature/`;
- keep navigation under `app/navigation/`;
- add/use `NotificationProcessor` for local coroutine-based parsing after raw-event persistence;
- reserve WorkManager for durable Google Sheets sync/recovery, not one job per notification;
- preserve all working Phase 0/1 behavior.

Run build/tests after this structural alignment.

Then implement Phase 2 — Parser framework from PLANS.md.

Required scope:
1. NotificationParser contract.
2. ParseResult states.
3. ParserRegistry.
4. Common notification text normalization.
5. VND amount parser using Long.
6. Direction inference: DEBIT, CREDIT, TRANSFER, UNKNOWN.
7. Conservative generic financial parser.
8. Conservative parser implementations/skeletons for:
   - MoMo
   - Timo
   - Techcombank
   - Grab
9. Persist ParsedObservation in Room and link it to RawNotificationEvent.
10. Connect:
    RawNotificationEvent
      -> NotificationProcessor
      -> ParserRegistry
      -> ParseResult
      -> ParsedObservation / Ignored / Unparsed
11. Extend debug UI to show parser result and extracted fields.
12. Add fixture-driven unit tests.

Critical constraints:
- Do not guess exact MoMo/Timo/Techcombank/Grab notification formats without sanitized real fixtures.
- Unknown/ambiguous data must become Unparsed/Unknown rather than invented values.
- Do not implement Phase 3 matching/canonical transaction features.
- Do not enqueue one WorkManager request per notification for normal parsing.
- Hilt is the required DI mechanism for the final V1 architecture.
- No Accessibility Service.
- No bank/payment login automation.
- No private API reverse engineering.
- No OTP/passcode capture.
- No credential capture.
- Never log full sensitive financial notification content in release builds.

VND parser tests must cover at least:
- 125.000đ
- 125,000 VND
- 125 000 ₫
- -125.000
- +5.000.000đ
- missing amount
- multiple numeric candidates
- multiline content

Before editing, report briefly:
- what Phase 0 currently contains;
- what Phase 1 currently contains;
- what Phase 2 pieces are missing;
- whether Hilt / `ui` -> `feature` / WorkManager-role alignment is already complete;
- which files/packages you expect to create/change/move;
- your implementation plan.

Then implement Phase 2 end-to-end.

After implementation:
- run relevant unit tests;
- run the appropriate project build/check;
- check Room schema/migration impact;
- verify no secrets/private samples are committed;
- verify no Phase 3 behavior was introduced;
- update PROJECT_STATUS.md.

End with exactly this structure:

Phase 2 status: PASS / PARTIAL / FAIL

Implemented:
- ...

Tests:
- ...

Build:
- ...

Known limitations:
- ...

Real notification samples needed next:
- MoMo: ...
- Timo: ...
- Techcombank: ...
- Grab: ...

Ready for Phase 3:
YES / NO

Only report Ready for Phase 3: YES if all Phase 2 completion conditions in PLANS.md are satisfied.
```

---

# Prompt — Add real notification samples during/after Phase 2

Use only after sanitized samples have been captured:

```text
I will provide sanitized notification samples from the app's debug screen.

For every sample:
1. identify its source;
2. add it as a regression fixture;
3. define the expected parser result before changing parser logic;
4. implement or adjust the smallest source-specific rule;
5. preserve generic fallback behavior;
6. never infer fields that are not evidenced by the sample;
7. run all affected parser tests.

Do not commit:
- real account numbers;
- phone numbers;
- personal names;
- OTP/passcodes;
- private transaction references;
- private notes.

If a sample is insufficient to reliably identify amount, direction, merchant, or service, keep that field Unknown/Unparsed and explain why.
```

---

# Prompt — Start Phase 3

Use only when `PROJECT_STATUS.md` says Phase 2 is PASS:

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, docs/03_DATA_MODEL.md, docs/05_TRANSACTION_MATCHING.md, docs/08_SECURITY_PRIVACY.md, docs/09_TEST_PLAN.md, and docs/10_ACCEPTANCE_CRITERIA.md.

Verify Phase 2 tests/build are still passing.

Implement Phase 3 — Canonical transaction creation only.

Requirements:
- one ParsedObservation can create one canonical transaction;
- multiple observations can eventually link to one transaction;
- exact notification reposts must not create duplicate transactions;
- do not merge solely because amounts match;
- create observation-to-transaction links;
- create conservative match-scoring abstractions;
- support INTERNAL_TRANSFER as a distinct type;
- add transaction list/detail UI;
- preserve explainability;
- preserve user edits if the existing architecture already supports them.

Required tests:
- one observation -> one transaction;
- repost -> no duplicate;
- two unrelated same-amount purchases -> two transactions;
- ambiguous observations are not force-merged.

Do not implement Phase 4 cross-app chain heuristics beyond the abstractions needed by Phase 3.

Before editing, inspect the repository and give a concise plan.
After implementation, run tests/build, update PROJECT_STATUS.md, and report PASS/PARTIAL/FAIL plus Ready for Phase 4: YES/NO.
```

---

# Prompt — Start Phase 4

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, and docs/05_TRANSACTION_MATCHING.md.

Verify Phase 3 is PASS.

Implement Phase 4 — Cross-app enrichment only.

Main invariant:
one real-world financial event = one canonical transaction.

Required supported fixture behavior:
- Techcombank -> MoMo -> GrabFood = one Food expense;
- MoMo -> GrabBike = one Transportation expense;
- two unrelated same-amount purchases remain separate.

Implement:
- known-chain compatibility;
- conservative time-window correlation;
- service enrichment;
- match confidence;
- reconciliation/stabilization before first remote sync;
- explainable linked observations.

Never merge on amount alone.

Run matcher regression tests, update PROJECT_STATUS.md, and report PASS/PARTIAL/FAIL plus Ready for Phase 5: YES/NO.
```

---

# Prompt — Start Phase 5

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, docs/03_DATA_MODEL.md, and docs/05_TRANSACTION_MATCHING.md.

Verify Phase 4 is PASS.

Implement Phase 5 — deterministic categorization rules.

Required precedence:
user locked category
> user rule
> system service rule
> system merchant rule
> fallback

Initial rules:
- GrabFood -> Food
- GrabBike -> Transportation
- GrabCar -> Transportation
- GrabMart -> Shopping/Groceries
- GrabExpress -> Other/Delivery

Do not force generic merchant "Grab" into Food or Transportation without service evidence or an explicit user rule.

Add:
- category/rule persistence;
- categorization engine;
- manual correction;
- user category lock;
- create-rule-from-correction flow;
- Rules UI;
- tests for rule precedence.

Run tests/build, update PROJECT_STATUS.md, and report PASS/PARTIAL/FAIL plus Ready for Phase 6: YES/NO.
```

---

# Prompt — Start Phase 6: Google Sheets

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, docs/06_GOOGLE_SHEETS_SYNC.md, docs/08_SECURITY_PRIVACY.md, and docs/09_TEST_PLAN.md.

Verify Phase 5 is PASS.

Implement Phase 6 — Google Sheets synchronization.

Requirements:
- Room stays the source of truth.
- Google authorization is explicitly user initiated.
- Do not collect Google password.
- Do not embed a service-account private key.
- User configures spreadsheet URL/ID and target tab.
- transaction_id is stable and stored in column A.
- WorkManager performs persistent network sync.
- Appends/retries are idempotent.
- Edited synced transactions update the existing remote transaction.
- Offline transactions stay local and sync later.
- Authorization failure becomes AUTH_REQUIRED, not a tight retry loop.
- A timeout after a successful remote write must not create a duplicate row on retry.

Use current official Google Identity/Google Sheets API patterns compatible with the current project toolchain. Do not copy deprecated sign-in examples.

Use a test spreadsheet for integration tests, never the user's real finance sheet by default.

Run tests/build, update PROJECT_STATUS.md, and report PASS/PARTIAL/FAIL plus Ready for Phase 7: YES/NO.
```

---

# Prompt — Start Phase 7

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, docs/07_UI_UX.md, and docs/10_ACCEPTANCE_CRITERIA.md.

Verify Phase 6 is PASS.

Implement Phase 7 — Dashboard.

Add:
- month selector;
- income;
- expense;
- net cash flow;
- category totals;
- recent transactions;
- useful filters/search;
- pending-sync indicator.

Accounting rule:
INTERNAL_TRANSFER must not affect income or expense totals.

Use a deterministic fixture dataset and add tests proving dashboard totals.

Run tests/build, update PROJECT_STATUS.md, and report PASS/PARTIAL/FAIL plus Ready for Phase 8: YES/NO.
```

---

# Prompt — Phase 8 real-device parser adaptation

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, and docs/04_NOTIFICATION_PIPELINE.md.

This phase adapts parser logic to sanitized real notification formats captured from the user's Android phone.

For every source/sample:
1. sanitize it;
2. add a regression fixture;
3. state expected output;
4. implement the smallest justified parser change;
5. run all parser/matcher regressions.

Target sources:
- MoMo
- Timo
- Techcombank
- Grab

Never commit private account/phone/name/OTP/reference/note data.

Do not weaken parser safety just to make one sample pass.

Update PROJECT_STATUS.md with which real source formats are covered and which remain unsupported.
```

---

# Prompt — Phase 9 / V1 final audit

```text
Read AGENTS.md, PROJECT_STATUS.md, PLANS.md, docs/08_SECURITY_PRIVACY.md, docs/09_TEST_PLAN.md, and docs/10_ACCEPTANCE_CRITERIA.md.

Do not add unrelated new features.

Perform Phase 9 hardening and a complete V1 audit:
- lifecycle/reboot/background behavior;
- offline/reconnect behavior;
- Room migrations;
- sync retry/backoff;
- Google authorization recovery;
- release log/privacy audit;
- backup policy review;
- local data clear/export behavior;
- acceptance criteria.

For every item in docs/10_ACCEPTANCE_CRITERIA.md, report PASS/FAIL/PARTIAL and cite the implementation/test evidence by file or test name.

Fix correctness/privacy issues before cosmetic issues.

Run the full available test/build suite.

Update PROJECT_STATUS.md with the final V1 state.
```
