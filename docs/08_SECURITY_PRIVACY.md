# 08 — Security & Privacy Requirements

This app handles highly sensitive personal financial metadata.

Security rules are product requirements, not optional cleanup.

## 1. Forbidden data

Never request or store:

- bank username/password;
- MoMo/Timo/Techcombank password;
- PIN;
- Smart OTP;
- OTP;
- CVV;
- full card secret data;
- secret/private API credentials.

## 2. Forbidden implementation techniques

Do not:

- automate login to banking/payment apps;
- scrape banking UI with Accessibility Service;
- reverse-engineer private bank APIs;
- intercept TLS;
- bypass certificate pinning;
- inject code into other apps;
- store a service-account private key in the APK;
- upload raw notifications to an unrelated server.

## 3. Notification content

Process only allowlisted packages.

Sensitive/OTP-like notifications:
- reject;
- do not retain full body;
- do not sync.

## 4. Local storage

Use Room in app-private storage.

For V1:
- rely on Android app sandbox;
- do not expose database files through broad external storage.

Consider encrypted storage in a later hardening phase if threat model requires it.

## 5. Logs

Release builds must not log:

- raw notification text;
- account/reference numbers;
- transaction notes;
- Google access tokens;
- authorization codes.

Debug logging should use sanitized synthetic/test data where possible.

## 6. Google credentials

Use supported Google authorization flows.

Never ask the user to type a Google password into this app.

Never commit:
- OAuth secrets that should be confidential;
- tokens;
- keystore passwords;
- local configuration containing secrets.

Android OAuth client configuration identifiers that are designed to be public are different from secrets; still keep environment-specific configuration organized.

## 7. Least privilege

Request Google authorization only when user chooses Google Sheets sync.

Prefer the narrowest practical scope.

Do not request full Drive access just for convenience.

## 8. Network

Only call expected HTTPS Google API endpoints in V1.

No analytics SDK is required.

No ad SDK.

No crash-reporting SDK unless explicitly added later with a privacy review.

## 9. Backups

Decide explicitly whether Android Auto Backup should include the finance database.

For a privacy-first V1, document the chosen behavior and test restore implications rather than accepting defaults unknowingly.

## 10. Screenshots and recent-app preview

Consider protecting highly sensitive screens with Android secure-window behavior in a later hardening milestone.

Not required to block the first functional V1, but include it before calling the app privacy-hardened.

## 11. Data deletion

Provide a settings action to:
- clear raw notification history;
- optionally clear all local app data with strong confirmation.

Deleting local canonical data does not automatically delete Google Sheet rows unless a remote-delete feature is explicitly implemented.

## 12. Threat model

Protect against:

- accidental duplicate accounting;
- secrets leaking to logs;
- accidental broad Google permissions;
- unauthorized package collection;
- retry-induced duplicate Sheet rows;
- sensitive notification retention;
- data loss while offline.

V1 is not intended to defend against a fully compromised/rooted Android device.
