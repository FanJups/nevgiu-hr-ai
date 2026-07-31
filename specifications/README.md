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

## Recommended delivery order

1. Confirm the unresolved product and scoring decisions.
2. Stabilize the existing job-offer workflow.
3. Complete CV ingestion, evaluation, and persistence.
4. Add vector indexing and conversational candidate search.
5. Build dashboard APIs and UI from persisted recruitment data.
6. Add the selected messaging integration after the web chat is stable.
7. Validate security, privacy, AI quality, performance, and usability before release.

## Current repository snapshot

The repository already contains:

- An Angular 19 frontend with job-offer generation, preview, approval, and listing components.
- A Spring Boot 3 / Java 21 backend with job generation, candidate, and CV evaluation domains.
- PostgreSQL with the pgvector image in Docker Compose.
- Spring AI and OpenAI model integration.

The repository does not yet visibly contain complete implementations for:

- CV upload and document parsing.
- Enabled pgvector/Spring AI vector-store integration.
- Conversational CV search and conversation memory.
- Telegram or WhatsApp integration.
- The interactive recruitment dashboard.
- Authentication, authorization, and audit logging.

> This snapshot is based on the source tree at the time these Markdown files were created. Keep it updated as features are delivered.
