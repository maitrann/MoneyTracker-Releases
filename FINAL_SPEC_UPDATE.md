# FINAL_SPEC_UPDATE.md

## Final architecture normalization before Phase 2

Completed Phase 0 and Phase 1 behavior remains the baseline. This update only normalizes the architecture before deeper implementation.

### Locked decisions

1. **Hilt** is the DI framework.
2. Screen-oriented packages use **`feature/`** instead of generic `ui/`.
3. Top-level navigation lives in **`app/navigation/`**.
4. Normal notification processing is:

```text
NotificationListenerService
  -> persist RawNotificationEvent
  -> NotificationProcessor
  -> coroutine/domain parsing
  -> Room
```

5. **WorkManager** is reserved for durable sync/recovery, especially Google Sheets synchronization. It is not the normal one-job-per-notification parser dispatcher.
6. V1 stays in a single Gradle **`:app`** module.

### Files synchronized in this final pack

- `AGENTS.md`
- `README.md`
- `PLANS.md`
- `START_CODEX.md`
- `PROJECT_STATUS.md`
- `docs/02_ARCHITECTURE.md`
- `docs/07_UI_UX.md`
- `docs/09_TEST_PLAN.md`
- `docs/10_ACCEPTANCE_CRITERIA.md`

Other product/data/security/sync specs are retained.
