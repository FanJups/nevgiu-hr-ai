# Project Context and Scope

## Product goal

Build an AI-assisted recruitment platform for recruiters and hiring managers. The product should reduce repetitive work, improve consistency, and support evidence-based hiring decisions without removing human oversight.

## Primary users

- Recruiters
- Hiring managers

## Core capabilities

The functional design defines four capabilities:

1. Generate a structured job offer from a short role description.
2. Extract and evaluate candidate CVs against a specific job.
3. Search, filter, summarize, and compare candidates using natural-language chat.
4. Monitor recruitment activity and candidate metrics through an interactive dashboard.

## Guiding principles

- Keep hiring decisions human-controlled; AI output is advisory.
- Explain scores and recommendations with evidence from the job and CV.
- Avoid protected attributes and bias-prone signals in automated scoring.
- Record model version, prompt version, weights, and source data for reproducibility.
- Protect candidate data with authentication, authorization, retention rules, and audit logs.
- Make ambiguous or low-confidence AI output visible to users.

## High-level workflow

1. A recruiter describes an open position.
2. AI generates a job-offer draft.
3. The recruiter edits and approves the offer.
4. Candidate CVs are uploaded and parsed.
5. Each candidate is evaluated against the approved job criteria.
6. CV content and metadata are indexed for semantic retrieval.
7. Recruiters query and compare candidates through chat or the dashboard.
8. Humans review evidence and make the hiring decision.

## Non-functional targets from the source

- Generate a job offer in under 30 seconds.
- Load the dashboard in under 3 seconds.
- Make key dashboard metrics accessible without training to at least 95% of users.
- Reflect newly saved data in the dashboard in real time or near real time.

## Out of scope until explicitly designed

- Fully automated rejection or hiring decisions.
- Diversity scoring based on protected personal characteristics.
- Direct job-board publishing, which the source identifies as a future integration.
- Production messaging integration before the web chat and security model are stable.
