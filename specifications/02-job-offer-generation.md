# AI-Generated Job Offers

## Objective

Generate a complete, editable job-offer draft from a short description while maintaining a consistent organizational tone and inclusive language.

## Inputs

Required:

- Brief free-text role description.

Optional:

- Department
- Location
- Employment type
- Salary range
- Tone: formal, friendly, inclusive, or another approved option

## Expected output

A structured job offer containing:

- Inferred title and seniority level
- Role summary
- Key responsibilities
- Required qualifications
- Preferred qualifications
- Soft skills and cultural expectations
- Benefits and perks, when supplied or applicable
- Missing or ambiguous information requiring review

The user must be able to edit, regenerate, approve, and save the draft. Export and direct job-board publication are future extensions.

## Current implementation

The main generation workflow is implemented across the Angular frontend and Spring Boot backend.

### Frontend

- The default route opens the job-offer generator at `/jobs/job-offer`.
- A reactive form collects the required brief description and optional department, location, employment type, salary range, and tone.
- The brief description is required and must contain at least 10 characters.
- Optional fields are grouped under an Advanced Options section.
- The UI sends typed requests through `JobService` and displays loading, empty, and generation-error states.
- A generated offer is rendered as a structured preview with title, level, location, salary, responsibilities, qualifications, soft skills, benefits, summary, and missing-information warnings.
- Users can regenerate an offer from the original request without saving the current draft.
- Users can enter edit mode and change every generated job-offer field before approval.
- Multi-value fields are edited one item per line and converted back to arrays before approval.
- Successful approval clears the working draft, and users can navigate to the approved-offer listing.

### Backend

The backend exposes:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/jobs/generate` | Generate a draft without saving it |
| `POST` | `/api/jobs/approve` | Persist the final, potentially edited offer |
| `GET` | `/api/jobs` | List approved jobs |
| `GET` | `/api/jobs/{id}` | Retrieve an approved job |

- Spring AI's `ChatClient` calls the configured OpenAI model.
- The system prompt is separated from recruiter input.
- The prompt requires clear, professional, bias-aware language and explicitly discourages age-related and gendered expressions.
- Tone is adapted to the selected formal, friendly, or inclusive value.
- The model is instructed not to invent salaries or benefits and to mark missing values instead.
- The response contract is structured into `jobOffer`, `missingInfo`, and `suggestions`.
- A malformed model response is converted into a fallback result containing `parseError` rather than causing an unhandled parsing exception.
- Generation and approval are deliberately separate: an AI draft is not persisted until a user approves it.
- Approved list values are stored as newline-separated text in PostgreSQL through Spring Data JPA.
- Approved jobs receive a creation timestamp.

### Existing test coverage

Backend tests cover:

- Prompt construction and handling of absent optional values.
- Parsing a valid structured model response.
- Graceful handling of invalid model JSON.
- Mapping and saving an approved job.

Frontend tests cover portions of:

- Form validation, submission, reset behavior, and Advanced Options toggling.
- Generation HTTP requests and error propagation.
- Generation success and regeneration behavior.
- Preview, loading, empty, and missing-information states.
- Approved-job listing rendering.

Test counts are intentionally not recorded here because they change as coverage grows. The backend suites are under `backend/src/test`, and the Angular `*.spec.ts` files live beside the frontend code they exercise.

## API examples

### Generate an unsaved draft

```http
POST /api/jobs/generate
Content-Type: application/json
```

```json
{
  "briefDescription": "Senior Java engineer building Spring Boot APIs",
  "department": "Engineering",
  "location": "Lisbon or remote",
  "employmentType": "Full-time",
  "salaryRange": null,
  "tone": "inclusive"
}
```

The response contains `jobOffer`, `missingInfo`, and `suggestions`. Generation does not write a job to the database.

### Approve and persist a reviewed draft

```http
POST /api/jobs/approve
Content-Type: application/json
```

The approval request contains the final generated or edited offer together with its original generation request. The current frontend exposes approval after the recruiter enters edit mode. A successful approval creates the job ID required by candidate evaluation.

OpenAI configuration is supplied through `OPENAI_API_KEY`. Generation errors or malformed model output must be treated as technical failures for human review; generated content should never be assumed correct solely because it is structurally valid.

## Known limitations

1. **Approved-list contract is transitional:** the backend `Job` response contains `title`; the Angular listing currently supports both `title` and `inferredTitle`. A dedicated response DTO should make the contract canonical.
2. **Approval is hidden until editing:** the Approve button is only rendered in edit mode. A correct unchanged draft cannot be approved directly.
3. **Suggestions are not displayed:** the API returns `suggestions`, but the preview only renders `missingInfo`.
4. **Backend input validation is missing:** API records do not use Bean Validation, so clients can bypass the Angular form rules.
5. **Structured model output is prompt-dependent:** extra prose or Markdown fences can cause JSON parsing to fail.
6. **Parse failures return HTTP success:** an `Unknown Title` fallback can be mistaken for a valid draft instead of being represented as a typed API error.
7. **Approval errors are console-only:** the UI does not show an actionable approval error or retry state.
8. **No timeout or retry policy:** slow or transient model failures are not handled explicitly.
9. **No generation audit trail:** the approved record does not retain the original request, generated draft, prompt version, model identifier, approval timestamp, or editor.
10. **No authentication or authorization:** job endpoints are public, and development CORS permits every origin.
11. **No update or delete API:** an approved job can be listed or retrieved but not maintained.
12. **Limited automated coverage:** there are no controller integration tests, stubbed-model API tests, or complete browser-level tests for generate, edit, approve, and reopen.

## Improvement plan

### Priority 1 — Correctness and user experience

1. Introduce an approved-job response DTO and align `title`/`inferredTitle` consistently across backend and frontend.
2. Show an Approve button in preview mode so users can accept an unchanged draft; retain Save and Approve in edit mode.
3. Display both missing information and AI suggestions with clear visual distinction.
4. Preserve the input form after generation or provide an explicit Clear action instead of resetting it immediately.
5. Add visible approval loading, success, failure, and retry states; replace browser alerts with application notifications.
6. Add a job-detail view so an approved offer can be reopened from the listing.

### Priority 2 — API and AI resilience

1. Add Bean Validation to generation and approval requests, including allowed employment types and tones.
2. Use Spring AI structured-output/schema support instead of relying only on prompt-enforced JSON.
3. Validate all returned fields, lists, score-free text lengths, and allowed enum values before sending a draft to the frontend.
4. Return a typed non-2xx problem response when generation or parsing fails; never expose internal exception messages as user suggestions.
5. Add model call timeouts, bounded retries with backoff, and request correlation identifiers.
6. Make prompt templates versioned and test them against a fixed regression dataset.
7. Add a deterministic inclusivity and clarity validation step after generation rather than relying only on model instructions.

### Priority 3 — Persistence and lifecycle

1. Persist the original request, generated draft, approved version, prompt version, model, generation duration, approval time, and user identity.
2. Represent job status explicitly, for example `DRAFT`, `APPROVED`, `PUBLISHED`, and `ARCHIVED`.
3. Store structured collections in normalized tables or JSON rather than newline-separated strings if they need to be queried or edited independently.
4. Add update, archive, and version-history operations for approved jobs.
5. Introduce database migrations instead of relying on `ddl-auto: update` for production.

### Priority 4 — Security and observability

1. Require authentication and role-based authorization for generation, approval, listing, and maintenance.
2. Restrict CORS to configured frontend origins.
3. Add rate limiting and usage controls to protect the model endpoint and budget.
4. Record structured metrics for latency, failures, parsing errors, token usage, cost, regeneration rate, and approval/edit rate without logging sensitive prompt content.
5. Ensure secrets remain environment-provided and are never returned through APIs or logs.

### Priority 5 — Testing and product measurement

1. Add controller integration tests covering validation, error mapping, persistence, and retrieval.
2. Add stubbed-model tests for valid output, malformed output, missing fields, timeouts, and provider errors.
3. Add end-to-end tests for generate, regenerate, direct approval, edit-and-approve, listing, and reopen flows.
4. Add accessibility tests for form controls, loading feedback, warnings, keyboard operation, and notifications.
5. Define “minimal edit” and measure the source target that 90% of users accept or minimally edit generated drafts.
6. Measure the 30-second target at an agreed percentile and concurrency level.

## Acceptance criteria

### Currently satisfied or substantially implemented

- [x] A valid request can produce a structured job-offer draft.
- [x] Generation does not automatically persist an unapproved AI draft.
- [x] Missing or ambiguous fields can be returned and displayed.
- [x] A user can regenerate a draft from the original request.
- [x] A user can edit and approve a generated result.
- [x] Approved content is persisted and can be listed or retrieved through the API.
- [x] The generation prompt includes inclusive-language guidance.

### Required before production readiness

- [ ] A valid short description produces a structured draft within 30 seconds at the agreed percentile and load.
- [ ] A user can approve an unchanged draft directly.
- [ ] Approved jobs display correctly and can be reopened in the UI.
- [ ] Missing information and improvement suggestions are both visible.
- [ ] Invalid requests and invalid model output cannot be approved or partially persisted.
- [ ] Provider, timeout, parsing, and approval failures return actionable user-facing errors.
- [ ] Regeneration and later edits cannot overwrite an approved version without an explicit versioned operation.
- [ ] Generated language passes an agreed, testable inclusivity and clarity check.
- [ ] Authentication, authorization, CORS restrictions, and audit history are enforced.
- [ ] Integration and end-to-end tests cover the complete workflow.
- [ ] At least 90% of users accept the generated draft with no or minimal edits, using an agreed measurement definition.
