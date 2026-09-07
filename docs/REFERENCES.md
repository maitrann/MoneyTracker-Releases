# Official Technical References

These are reference starting points for the implementation. When coding, prefer current official documentation over copied blog snippets.

## Android

### NotificationListenerService
Android API for receiving callbacks when notifications are posted/removed.

https://developer.android.com/reference/android/service/notification/NotificationListenerService

### Android 15 notification privacy / OTP redaction
Android 15 restricts untrusted notification listeners from reading unredacted content when OTP is detected.

https://developer.android.com/about/versions/15/behavior-changes-all

### Room
Local persistence abstraction over SQLite.

https://developer.android.com/training/data-storage/room

### WorkManager
Recommended Android solution for persistent/deferrable background work such as resilient sync.

https://developer.android.com/develop/background-work/background-tasks/persistent

## Google Identity

### Authorize access to Google user data
Use the current Google Identity authorization flow for Google API access. Authentication and authorization are separate concerns.

https://developer.android.com/identity/authorization

### OAuth overview
https://developers.google.com/identity/protocols/oauth2

## Google Sheets API

### API overview
https://developers.google.com/workspace/sheets/api/guides/concepts

### Append values
https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets.values/append

### Update values
https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets.values/update

### Scopes
Google recommends using the narrowest practical scope. `drive.file` is the preferred per-file access scope when the file-selection/creation flow supports it.

https://developers.google.com/workspace/sheets/api/scopes

### Usage limits
https://developers.google.com/workspace/sheets/api/limits

## Codex

### AGENTS.md
Codex reads `AGENTS.md` project guidance before work.

https://developers.openai.com/codex/agent-configuration/agents-md

### Codex best practices
https://developers.openai.com/codex/learn/best-practices
