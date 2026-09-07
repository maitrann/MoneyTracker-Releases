# 05 — Transaction Matching, De-duplication & Categorization

## 1. Objective

Convert multiple observations of the same real-world financial event into exactly one canonical transaction.

This is the most important accounting rule in the project.

## 2. Matching philosophy

Never merge solely because amounts match.

Use a weighted score based on:

- exact amount
- close timestamp
- compatible direction
- source/payment chain compatibility
- merchant/service similarity
- reference hint
- known integration route
- already-linked observations

## 3. Default correlation window

Start with a configurable window around 5 minutes.

Different matching rules may use tighter windows.

Example:
- same amount + MoMo payment + Grab service detail within 3 minutes => strong candidate;
- same amount at two unrelated merchants within 5 minutes => do not merge.

## 4. Example payment chain

Observed:

```text
10:31 Techcombank  -120,000  "payment ... MoMo"
10:31 MoMo         -120,000  "payment ... Grab"
10:32 Grab          120,000  "GrabFood order"
```

Canonical:

```text
type: EXPENSE
amountVnd: 120000
fundingSource: Techcombank
paymentChannel: MoMo
merchant: Grab
service: GrabFood
category: Food
occurredAt: best transaction timestamp
```

Three observations are linked to the same transaction.

## 5. Partial chains

If only this exists:

```text
MoMo -120,000 Grab
```

create a transaction:

```text
merchant = Grab
service = null/unknown
category = Unclassified or existing Grab default
```

If a GrabFood observation arrives shortly afterward, enrich the existing transaction rather than insert a second transaction.

## 6. Pending reconciliation

A newly parsed observation may create a transaction in a short-lived `PENDING_RECONCILIATION` domain state before being considered stable for remote sync.

Recommended approach:

- Save immediately.
- Allow a short reconciliation delay before first sync, e.g. 1–3 minutes.
- If the user opens the app, display it immediately.
- Reconcile as additional observations arrive.
- Sync once stable or when the delay expires.

Do not delay local persistence.

## 7. Exact duplicates

Treat repeated notification updates separately from cross-app correlation.

Exact duplicate:
- same source package;
- same notification key/content fingerprint;
- equivalent financial content.

Cross-app duplicate:
- different notifications but same real transaction.

## 8. Internal transfers

A transfer should be `INTERNAL_TRANSFER` when both source and destination are known user-owned financial containers.

Examples:

```text
Techcombank -> MoMo
Timo -> MoMo
MoMo -> Túi Thần Tài
Túi Thần Tài -> MoMo
```

Important distinction:

```text
Techcombank -> MoMo top-up
then MoMo -> GrabFood
```

The top-up is an internal transfer.
The GrabFood transaction is an expense.

If a linked bank is charged directly for a MoMo/Grab purchase without increasing a persistent MoMo balance, observations may all represent one expense instead. Matcher rules should be based on actual observed semantics, not assumptions.

## 9. Match score suggestion

Example weighting only; tune with tests:

```text
+50 exact amount
+25 timestamp <= 60 sec
+15 timestamp <= 180 sec
+20 compatible known chain (bank -> MoMo -> Grab)
+20 matching reference hint
+15 compatible merchant/channel
-80 conflicting merchant/service
-100 opposite incompatible direction
```

Merge only above a conservative threshold.

If uncertain, keep transactions separate and let debug/review tooling expose candidates.

False merge is worse than an obvious duplicate because it corrupts accounting history.

## 10. Canonical field precedence

Recommended field resolution:

### Amount
Use highest-confidence financial debit/credit observation.

### Funding source
Prefer bank/card observation if it is clearly the source of a linked payment.

### Payment channel
Prefer explicit intermediary such as MoMo.

### Merchant
Prefer merchant-level observation.

### Service
Prefer the most specific service observation, often from merchant app.

### Category
Use user lock > user rule > service rule > merchant rule > fallback.

## 11. Categorization

Deterministic engine:

```text
if userCategoryLocked:
    preserve category
else:
    apply highest-priority enabled matching rule
```

Suggested system rules:

```text
service == GRAB_FOOD     -> Food
service == GRAB_BIKE     -> Transportation
service == GRAB_CAR      -> Transportation
service == GRAB_MART     -> Shopping/Groceries
service == GRAB_EXPRESS  -> Other/Delivery
```

Merchant-only `Grab` is not enough to choose Food vs Transportation.

## 12. User-created rule workflow

When user edits:

```text
CGV -> Entertainment
```

offer:

```text
Apply only this transaction
Create rule for future transactions
```

If rule:
- store as `USER`;
- assign a higher priority than system merchant rules;
- do not overwrite manually locked old transactions unless user requests reclassification.

## 13. Explainability

Transaction detail should be able to show:

```text
Why this transaction exists:
- Techcombank observation: funding source
- MoMo observation: payment channel + merchant
- Grab observation: service = GrabFood
- Rule: GrabFood -> Food
```

This is invaluable for debugging incorrect matching.
