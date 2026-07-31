# Open Decisions and Source Inconsistencies

Resolve these items before treating the functional design as implementation-ready.

## 1. Messaging channel

The source mentions WhatsApp in the product overview and one output example, but the detailed chat specification requires Telegram.

Decide:

- Telegram only
- WhatsApp only
- Both, delivered in a defined order

Recommended approach: implement a channel-neutral messaging adapter, stabilize web chat first, then add one external channel.

## 2. Composite evaluation formula

The source provides category weights but not individual metric weights. It also does not say whether AI confidence should affect candidate fit or remain a separate reliability indicator.

Decide:

- The exact weight of each metric.
- Whether weights are global defaults, per-job overrides, or both.
- Whether AI confidence changes the score or only triggers review.
- How missing metrics affect normalization.

Recommended approach: keep AI confidence outside candidate fit and use it to trigger human verification.

## 3. Employment gaps

The source says gaps longer than six months are “penalized.” This can create unfair or legally sensitive outcomes.

Decide:

- Whether the metric is scored at all.
- What counts as a gap.
- Which user-provided explanations are recognized.
- Whether it should only be a neutral review flag.

Recommended approach: flag potential gaps neutrally and exclude them from automatic ranking.

## 4. Candidate data governance

The source does not define:

- Candidate consent and lawful processing basis
- Retention and deletion periods
- Data residency
- Access roles and organization isolation
- Audit-log retention
- Candidate correction or appeal workflow

These decisions are release blockers because CVs contain personal data.

## 5. Authentication and authorization

The source assumes user identifiers but does not define authentication. Decide the identity provider, roles, tenant model, session behavior, and authorization rules for jobs, CVs, evaluations, chat, and dashboards.

## 6. “Real-time” dashboard behavior

Define an observable freshness target, such as “new evaluations appear within 10 seconds,” and select polling, server-sent events, or WebSockets accordingly.

## 7. Performance targets

Clarify:

- Whether the 30-second generation target applies to p95 or another percentile.
- Whether the 3-second dashboard target includes network time and first meaningful content.
- The dataset size and concurrent-user load used for testing.

## 8. Job-board export

The source marks export or direct posting as a future integration. Decide whether basic file export belongs in the first release and keep direct publishing out of scope until provider requirements are known.

## 9. AI quality measurement

Define:

- What “minimally edit” means for the 90% acceptance target.
- Ground-truth datasets for CV metric evaluation.
- Retrieval precision and answer-faithfulness thresholds.
- Owners and cadence for prompt/model regression review.
