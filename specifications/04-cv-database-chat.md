# CV Database Chat and Prompting

## Objective

Allow authenticated HR users to retrieve, summarize, filter, and compare indexed candidates using natural-language queries.

## Supported interactions

- Find candidates by skills, experience, location, education, title, employer, or evaluation metric.
- Apply multiple filters and refine them over follow-up turns.
- Ask questions about a specific candidate.
- Compare selected candidates against explicit criteria.
- Return concise summaries with references to candidate profiles.
- Ask for clarification when the request is ambiguous.
- Explain when no candidate matches or when an internal operation fails.

## Inputs and outputs

Inputs:

- Natural-language query
- Authenticated user and organization identifier
- Conversation/session identifier
- Authorized conversation history
- CV vector index
- Structured candidate metadata and source CV text

Outputs:

- Natural-language answer
- Structured candidate results for UI cards or tables
- Candidate identifiers and links
- Evidence references supporting the answer
- Updated conversation state
- Clarification, empty-result, or error response when appropriate

## Implementation steps

1. Enable and configure the Spring AI pgvector integration.
2. Design the chunking strategy and metadata schema for candidate, organization, CV, and job identifiers.
3. Index normalized CV content and evaluation summaries after ingestion.
4. Implement organization- and role-based retrieval filters before semantic search.
5. Build a retrieval service that returns relevant chunks plus structured candidate data.
6. Build agent tools for candidate search, candidate detail, filtering, and comparison.
7. Require responses to cite candidate records or source excerpts and prohibit unsupported claims.
8. Persist conversation state with clear retention and deletion rules.
9. Implement the web chat UI with text responses, result cards, loading states, and follow-up prompts.
10. Add ambiguity detection, clarification, no-result, timeout, and partial-failure behavior.
11. Protect against prompt injection in CV content and user queries.
12. Add authorization tests proving users cannot retrieve another organization’s candidates.
13. Evaluate retrieval precision, answer faithfulness, latency, and token cost against a fixed test set.
14. Add the selected external messaging channel only after web-chat acceptance criteria pass.

## Acceptance criteria

- Follow-up questions retain the correct conversation context.
- Search results respect organization and user permissions.
- Each factual candidate claim links to supporting source data.
- Candidate comparison uses the requested criteria consistently.
- Ambiguous questions trigger clarification instead of guessed answers.
- No-result and service-failure states are explicit and actionable.
- CV prompt injection cannot override system policy or expose unrelated records.
