# 04 — Notification Ingestion & Parsing

## 1. Android entry point

Use `NotificationListenerService`.

The service observes posted notifications only after the user grants Notification Access in Android system settings.

## 2. Allowlist first

Before extracting financial content:

```text
if packageName not in userEnabledPackages:
    ignore
```

Do not build a broad surveillance collector.

## 3. Fields to inspect

From the notification/status-bar object, collect only fields useful for transaction detection.

Typical useful text fields include:

- title
- text
- big text
- sub text

Also capture:
- package
- notification key/id
- post time
- notification category/channel when helpful

Actual availability varies by app.

## 4. Sensitive-content filter

Before persistence, reject notifications that look like OTP/passcode/authentication messages.

Examples of indicators:

```text
OTP
one-time password
verification code
mã xác thực
mã OTP
Smart OTP
do not share
không chia sẻ
```

A numeric pattern alone is not enough because transaction amounts also contain digits.

The sensitive filter should combine keywords + context.

Status:

```text
SENSITIVE_IGNORED
```

Do not store the full sensitive text.

## 5. Exact duplicate filter

Notifications may be updated/re-posted.

Create a normalized event fingerprint from fields such as:

```text
packageName
notification key/id where stable
normalized title
normalized text
rounded post-time bucket if needed
```

Do not discard an event solely because amount/time match. Different real purchases can have the same amount.

## 6. Parser registry

Use package/source-specific parsers before generic heuristics.

```text
ParserRegistry
  1. source-specific parser
  2. generic financial parser
  3. Unparsed
```

Never treat generic heuristics as high confidence.

## 7. Amount normalization

Support common VND formats:

```text
125.000đ
125,000 VND
125 000 ₫
-125.000
+5.000.000đ
```

Normalize to:

```text
Long amountVnd = 125000
```

Parsing rules must be unit tested.

Do not interpret decimal punctuation as fractional VND.

## 8. Direction inference

Look for source-specific debit/credit semantics.

Examples of generic low-confidence indicators:

Debit:
```text
-
trừ
thanh toán
chi
debit
paid
```

Credit:
```text
+
cộng
nhận
credit
received
```

Do not rely only on generic Vietnamese words when source-specific format is known.

## 9. Source parsers

### MoMo

Potential observations:
- wallet debit/credit;
- payment merchant;
- transfer;
- linked funding source;
- Túi Thần Tài movement.

Do not assume exact wording until fixtures from the user's phone are captured.

### Timo

Potential observations:
- debit/credit;
- transfer;
- merchant/card payment.

### Techcombank

Potential observations:
- debit/credit;
- transfer;
- card/merchant payment.

### Grab

Potential observations:
- service type;
- order/ride amount;
- order/ride status.

Grab can enrich a payment with service context:

```text
GrabFood -> Food
GrabBike -> Transportation
GrabCar -> Transportation
GrabMart -> Groceries/Shopping
GrabExpress -> Delivery/Other
```

## 10. Debug sample collector

Add a debug screen with:

```text
Timestamp
Package
Title
Text
BigText
SubText
Parser result
```

Functions:

- copy a sanitized sample;
- mark expected amount;
- mark expected direction;
- mark expected service;
- mark whether it should be ignored.

This will turn real samples into parser test fixtures.

## 11. Parser fixture format

Use JSON resources in tests, for example:

```json
{
  "source": "momo",
  "packageName": "<verified-package>",
  "postedAtEpochMs": 0,
  "title": "<sanitized real sample>",
  "text": "<sanitized real sample>",
  "expected": {
    "amountVnd": 125000,
    "directionHint": "DEBIT",
    "merchant": "Grab"
  }
}
```

Never commit real account numbers, names, phone numbers, reference numbers, or private notes into test fixtures.

## 12. Parser versioning

Each parser exposes a version integer.

If parser logic changes later, raw events remain available for optional re-processing.
