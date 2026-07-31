# Implementation Roadmap

## Phase 0 — Resolve foundations

- [ ] Decide Telegram, WhatsApp, or both.
- [ ] Finalize the composite scoring formula and individual weights.
- [ ] Define authentication, roles, organization boundaries, and audit requirements.
- [ ] Define candidate consent, retention, deletion, and data-residency policies.
- [ ] Define the supported CV formats and maximum upload size.
- [ ] Establish API error format, observability, and environment configuration standards.
- [ ] Replace development credentials and confirm secrets are never committed.

**Exit condition:** Product, security, and scoring decisions are documented and testable.

## Phase 1 — Stabilize job-offer generation

- [ ] Review existing backend job generation and frontend job-offer flows against the feature specification.
- [ ] Version request, response, prompt, and persistence schemas.
- [ ] Complete edit, regeneration, approval, and listing behavior.
- [ ] Add missing-information and inclusive-language checks.
- [ ] Add unit, integration, and end-to-end coverage.
- [ ] Measure latency and draft acceptance/edit rates.

**Exit condition:** A recruiter can generate, review, edit, approve, save, and reopen a job offer reliably.

## Phase 2 — Complete CV ingestion and evaluation

- [ ] Implement secure CV upload and storage.
- [ ] Parse and normalize CV content.
- [ ] Complete all eight metrics with evidence and confidence.
- [ ] Implement validated per-job weight configuration.
- [ ] Persist versioned evaluations and audit history.
- [ ] Build candidate list and evaluation-detail UI.
- [ ] Validate scoring consistency, bias controls, and adversarial inputs.

**Exit condition:** Uploaded candidates receive explainable, reproducible, human-reviewable evaluations.

## Phase 3 — Add semantic indexing and web chat

- [ ] Enable pgvector integration currently present but disabled in backend dependencies.
- [ ] Create embeddings and index CV chunks with authorization metadata.
- [ ] Implement retrieval, filtering, candidate detail, and comparison tools.
- [ ] Persist scoped conversation memory.
- [ ] Build the Angular chat interface and structured result cards.
- [ ] Add citations, clarification, injection defenses, and evaluation tests.

**Exit condition:** Authorized users can reliably find and compare candidates through the web chat with evidence-backed answers.

## Phase 4 — Build the dashboard

- [ ] Define metric semantics and freshness targets.
- [ ] Implement aggregate APIs and database indexes.
- [ ] Build filterable dashboard widgets and drill-down navigation.
- [ ] Add near-real-time updates and resilient UI states.
- [ ] Add evidence-backed AI insights.
- [ ] Validate accessibility, usability, and the 3-second target.

**Exit condition:** Recruiters can monitor roles and candidate evaluations and reach source records from every aggregate view.

## Phase 5 — Add external messaging

- [ ] Implement the selected provider behind a channel-neutral messaging interface.
- [ ] Link messaging identities to authenticated application users securely.
- [ ] Reuse the same authorization, retrieval, and conversation services as web chat.
- [ ] Minimize sensitive candidate data in messages and links.
- [ ] Add provider signature validation, rate limiting, retry, and audit logging.

**Exit condition:** External chat has security and answer quality equivalent to the web channel.

## Phase 6 — Release readiness

- [ ] Run full unit, integration, end-to-end, performance, and security test suites.
- [ ] Verify backup, restore, retention, deletion, and incident procedures.
- [ ] Add model, token, latency, error, retrieval-quality, and cost monitoring.
- [ ] Conduct recruiter acceptance testing with representative workflows.
- [ ] Document deployment, rollback, support, and model-change procedures.

**Exit condition:** Operational, security, AI-quality, and product owners approve release.

## Definition of done for every feature

- [ ] Requirements and acceptance criteria are agreed.
- [ ] API and data contracts are documented.
- [ ] Authorization and privacy behavior are tested.
- [ ] AI output is explainable and low-confidence behavior is explicit.
- [ ] Unit and integration tests pass.
- [ ] Relevant end-to-end flow passes.
- [ ] Error, empty, timeout, and retry states are handled.
- [ ] Logs and metrics support diagnosis without exposing candidate data.
- [ ] User documentation is updated.
