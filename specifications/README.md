# HR AI Recruitment Manager Specifications

This directory translates `FD - Hackathon KLX 2025.docx` into implementation-oriented Markdown.

## Document map

1. [Project context and scope](./01-project-context.md)
2. [AI-generated job offers](./02-job-offer-generation.md)
3. [CV evaluation](./03-cv-evaluation.md)
4. [CV database chat](./04-cv-database-chat.md)
5. [Interactive dashboard](./05-interactive-dashboard.md)
6. [Implementation roadmap](./06-implementation-roadmap.md)
7. [Open decisions](./07-open-decisions.md)
8. [CI/CD and VPS deployment](./08-ci-cd-deployment.md)
9. [OVHcloud VPS provisioning runbook](./09-vps-provisioning-runbook.md)

## Recommended delivery order

1. Confirm the unresolved product and scoring decisions.
2. Harden the implemented job-offer workflow and complete its production controls.
3. Extend the implemented CV ingestion and evaluation workflow with review, governance, and reproducibility features.
4. Add vector indexing and conversational candidate search.
5. Build dashboard APIs and UI from persisted recruitment data.
6. Add the selected messaging integration after the web chat is stable.
7. Validate security, privacy, AI quality, performance, and usability before release.

## Current repository snapshot

| Capability | Status | Notes |
| --- | --- | --- |
| Job-offer generation | Implemented, needs hardening | Generate, edit, approve, persist, and list are available. Direct approval, schema-constrained output, lifecycle management, and production security remain. |
| CV ingestion | Implemented, needs hardening | PDF, ZIP, and built-in imports share a guarded pipeline with extraction, duplicate detection, per-file results, and Angular UI. OCR, original-file storage, correction, and governance remain. |
| Candidate evaluation | Implemented, needs hardening | Explicit job-specific AI evaluation, eight validated metrics, weighting, persistence, and result UI are available. Evidence, versioning, retrieval/history, and bias testing remain. |
| CV database chat | Planned | pgvector is available in the database image, but vector indexing, retrieval, and conversation memory are not implemented. |
| Interactive dashboard | Planned | Aggregate APIs and dashboard UI are not implemented. |
| External messaging | Planned | Telegram/WhatsApp choice and integration remain open. |
| Production security | Planned | Authentication, authorization, tenant isolation, restricted CORS, audit logging, malware scanning, and retention controls remain. |
| CI/CD and VPS deployment | Implemented, environment setup pending | CI, immutable GHCR publication, automatic staging deployment, staging validation, and release-gated production promotion are implemented. GitHub environments and production VPS setup remain. |
| VPS provisioning | Staging complete, production pending | OVHcloud staging is hardened and serving HTTPS through Squarespace DNS. The validated procedure is documented for production reuse. |

The application uses an Angular 19 frontend, a Spring Boot 3 / Java 21 backend, PostgreSQL with the pgvector image, and Spring AI with OpenAI.

> This snapshot is based on the source tree at the time these Markdown files were created. Keep it updated as features are delivered.
