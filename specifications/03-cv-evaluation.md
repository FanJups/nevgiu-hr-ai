# CV Ingestion and Evaluation

## Objective

Accept candidate CV documents, extract reliable text and metadata, create candidate records, and produce explainable job-specific evaluations that support rather than replace human review.

Ingestion and evaluation are separate operations:

```text
PDF or ZIP upload
       |
       v
Validate and extract CV content
       |
       v
Create candidate and document records
       |
       v
Explicitly evaluate selected candidate against selected job
```

Uploading a CV must not automatically call the AI evaluation service. This separation keeps ingestion fast, prevents accidental model cost, and allows one candidate to be evaluated against multiple jobs.

## Current implementation

### Candidate persistence

The backend has a `Candidate` JPA entity and repository. A candidate currently contains:

- ID
- Name
- Email
- Location
- Current title
- Years of experience
- Extracted CV text in the `cvText` field
- Creation timestamp

The existing candidate API exposes:

| Method | Endpoint | Current purpose |
| --- | --- | --- |
| `GET` | `/api/candidates` | List candidate entities |
| `POST` | `/api/candidates` | Create a candidate from JSON |

The JSON creation endpoint assumes that the caller already has extracted CV text. It is not a document-upload endpoint.

### Built-in CV dataset

The backend includes a built-in archive at:

```text
classpath:intial/CVs.zip
```

Repository location:

```text
backend/src/main/resources/intial/CVs.zip
```

The archive is packaged into the backend application and Docker image by Maven. It currently contains 35 PDF files under a `CVs/` directory, with approximately 4.8 MB of uncompressed content.

The archive is seed and test data for the initial ingestion pipeline. It includes both CV-like documents and templates, so import must support per-file warnings and must not assume that every PDF represents a valid candidate.

### Candidate evaluation

The evaluation API is implemented:

| Method | Endpoint | Current purpose |
| --- | --- | --- |
| `POST` | `/api/evaluations` | Evaluate an existing candidate against an existing job |

The request contains:

```json
{
  "candidateId": 1,
  "jobId": 2,
  "weights": null
}
```

The current service:

1. Loads the candidate and job from PostgreSQL.
2. Builds a job description from the job summary, responsibilities, and required qualifications.
3. Sends the job description and `candidate.cvText` to the configured OpenAI model through Spring AI.
4. Requests all eight defined evaluation metrics as JSON.
5. Parses the model response.
6. Applies supplied weights or default weights.
7. Normalizes 0-10 metrics to a 0-100 scale and calculates the composite score.
8. Persists the candidate evaluation and AI explanation.
9. Returns a typed evaluation response.

The prompt explicitly excludes demographic and diversity-based scoring.

### Persisted evaluation data

Each `CandidateEvaluation` stores:

- Candidate reference
- Job reference
- All eight metric values
- Overall fit score
- AI explanation
- Creation timestamp

### Existing evaluation tests

Unit tests currently cover:

- Parsing valid AI evaluation JSON.
- Rejecting malformed AI JSON as a typed upstream failure.
- Composite-score normalization and weighting.
- Rejecting invalid score ranges and invalid custom weight totals.
- PDF text extraction, PDF validation, safe archive traversal, and unsupported archive entries.
- Candidate/evaluation HTTP validation and typed ingestion errors through MockMvc.
- Candidate-document persistence, relationships, and unique SHA-256 constraints with an embedded database.
- Missing candidates and blank CV text without invoking the AI provider.

Test totals are intentionally omitted because they change as coverage grows. PostgreSQL/Testcontainers, live-provider, adversarial prompt-injection, and full multi-service end-to-end suites remain future work.

## Evaluation metrics

### Job fit

- **Skills match (0-100%)**: semantic and exact overlap between required skills and extracted CV skills. The source suggests above 70% as a positive signal.
- **Experience relevance (0-10)**: relevant roles, industries, duration, and recency.
- **Education fit (0-10)**: degree level, relevant field, and certifications relative to job requirements.

### CV quality

- **Achievement impact (0-10)**: relevant, measurable outcomes and evidence-backed accomplishments.
- **Keyword density (0-100%)**: appropriate use of job-related terms while detecting keyword stuffing.

### Risk and confidence

- **Employment gap score (0-10)**: currently scores unexplained gaps longer than six months. The production design should convert this into a neutral human-review flag rather than an automatic ranking penalty.
- **Readability and structure (0-10)**: clarity, organization, and appropriate length.
- **AI confidence (0-100%)**: reliability of extraction and scoring. The source suggests human verification below 80%.

### Current composite weights

The implemented default weights are:

| Metric | Weight |
| --- | ---: |
| Skills match | 25% |
| Experience relevance | 15% |
| Education fit | 15% |
| Achievement impact | 15% |
| Keyword density | 10% |
| Employment gap | 10% |
| Readability | 5% |
| AI confidence | 5% |

The weights total 100%. Custom weights must be finite, non-negative, and total exactly 100% within a small floating-point tolerance. The source groups metrics as 40% skills/experience, 30% education/achievements, and 30% quality/risk; the implemented individual weights are one interpretation of that incomplete formula.

AI confidence should ultimately be separated from candidate quality and used to trigger human verification instead of improving or reducing candidate fit.

Custom weights are supported by the backend API. The current Angular workflow sends the default weights and does not expose weight editing to recruiters.

## Implemented ingestion backend

The backend now provides one shared ingestion pipeline for user uploads and the built-in dataset:

- `POST /api/candidates/import` accepts one PDF as `multipart/form-data` and returns `201 Created`.
- `POST /api/candidates/import/archive` accepts a ZIP and returns a result for every file.
- `POST /api/candidates/import/initial` loads `classpath:intial/CVs.zip` through the same ZIP pipeline.
- Apache PDFBox extracts text behind the `CvTextExtractor` abstraction.
- Extension, declared content type, PDF signature, request size, and per-file size are validated.
- ZIPs are streamed in memory and protected by path, entry-count, expanded-size, per-entry-size, and compression-ratio checks.
- SHA-256 hashes and a database uniqueness constraint make repeat imports idempotent.
- `CvDocument` records retain source, status, filename, content type, size, hash, extracted text, error, candidate link, and import time.
- Results distinguish `IMPORTED`, `DUPLICATE`, `NEEDS_REVIEW`, `SKIPPED`, and `FAILED`.
- Low-text and probable image-only PDFs are stored as `NEEDS_REVIEW` without creating a misleading candidate.
- Candidate names are conservatively derived from filenames and the first valid email is extracted from CV text.
- Typed API errors cover invalid uploads, missing records, validation failures, oversized multipart requests, and evaluation-provider failures.

Ingestion does not invoke AI evaluation. A recruiter must explicitly select a candidate and job before calling the evaluation endpoint.

### Remaining backend work

- Store original files behind a storage abstraction and define retention/deletion rules.
- Add OCR and reprocessing for scanned PDFs.
- Add richer candidate/document detail and ingestion-history APIs.
- Detect templates and non-CV PDFs more accurately.
- Make large imports asynchronous and expose job progress.
- Require authentication, restrict the built-in import to administrators, and scan uploads for malware.

## Implemented ingestion API

### Upload one CV

```http
POST /api/candidates/import
Content-Type: multipart/form-data
```

Form field:

```text
file=<candidate.pdf>
```

Successful response using `201 Created`:

```json
{
  "candidateId": 42,
  "documentId": 87,
  "originalFilename": "candidate.pdf",
  "status": "IMPORTED",
  "contentType": "application/pdf",
  "textLength": 6842,
  "warnings": []
}
```

### Upload a ZIP archive

```http
POST /api/candidates/import/archive
Content-Type: multipart/form-data
```

Form field:

```text
file=<CVs.zip>
```

Archives at the configured limit are processed synchronously. Larger production archives should later create an asynchronous import job and return `202 Accepted`.

Example result:

```json
{
  "totalFiles": 35,
  "imported": 31,
  "duplicates": 1,
  "skipped": 2,
  "failed": 1,
  "results": [
    {
      "originalFilename": "CVs/amanda-akins-cv.pdf",
      "documentId": 87,
      "candidateId": 42,
      "status": "IMPORTED",
      "warnings": []
    },
    {
      "originalFilename": "CVs/cv-template.pdf",
      "documentId": 88,
      "candidateId": null,
      "status": "NEEDS_REVIEW",
      "warnings": ["Little or no text was extracted; OCR or manual review may be required"]
    }
  ]
}
```

### Load built-in CV data

The explicit endpoint opens `classpath:intial/CVs.zip` and passes it through the same archive-ingestion service used by user uploads.

```http
POST /api/candidates/import/initial
```

Current behavior:

- Repeated calls report documents as duplicates and do not create duplicate candidates.
- The endpoint returns the same bulk result as an uploaded ZIP.
- Parsing logic is shared with uploaded archives.
- Imported candidates are not automatically evaluated.
- The endpoint can be disabled with `app.cv-ingestion.initial-import-enabled`; role-based administrative access remains to be implemented.

## Evaluation API example

```http
POST /api/evaluations
Content-Type: application/json
```

Use `weights: null` for the documented default weights:

```json
{
  "candidateId": 42,
  "jobId": 7,
  "weights": null
}
```

The response contains an `evaluation` object with its persisted ID, candidate and job IDs, all eight metrics, the overall fit score, explanation, and creation time. It also exposes the explanation at the response's top level for the current frontend contract.

Each successful request creates a new evaluation record. Re-evaluation does not overwrite an earlier row, but there is currently no evaluation history or retrieval endpoint and the prompt/model/source-document versions needed for complete reproducibility are not stored.

## Implemented data model

Candidate identity and uploaded-document lifecycle are separate concerns. `CvDocument` is related to an optional `Candidate` and contains:

- ID
- Candidate reference
- Original filename
- Content type
- File size
- SHA-256 hash with a uniqueness constraint
- Source: `INITIAL_DATA` or `USER_UPLOAD`
- Ingestion status
- Ingestion error
- Extracted text
- Import timestamp

The extracted text is currently retained, but original binary files are not. Production should introduce a storage abstraction; local filesystem storage is sufficient for development and object storage is preferable for deployment.

Candidate names are conservatively derived from filenames and the first valid email address is extracted from CV text. This metadata is best-effort and must be reviewed; it is not identity verification.

CV text is personal data and is sent to the configured OpenAI service only when a recruiter explicitly requests evaluation. A production deployment must establish a lawful processing basis, candidate notice or consent where applicable, provider data-processing terms, access controls, retention/deletion rules, and a correction workflow before real candidate data is used.

## Implemented ingestion pipeline

1. Accept an individual PDF, uploaded ZIP, or built-in classpath archive.
2. Enforce request-size and archive-size limits before processing.
3. Validate filename, extension, declared MIME type, and file signature.
4. For ZIPs, stream entries without extracting them to arbitrary filesystem paths.
5. Reject absolute paths and `..` path traversal.
6. Enforce entry-count, per-entry-size, total-expanded-size, and compression-ratio limits.
7. Calculate a SHA-256 hash and detect previously imported content.
8. Extract PDF text with a dedicated `CvTextExtractor` abstraction.
9. Flag empty or image-only documents for OCR/manual review rather than creating misleading content.
10. Extract conservative candidate name and email metadata; unknown fields remain null.
11. Persist the document record, candidate, extracted text, status, and warnings.
12. Continue an archive import when an individual entry fails.
13. Return a complete per-file outcome without exposing internal stack traces.

## Delivery plan

### Phase 1 - PDF ingestion foundation

- [x] Add Apache PDFBox for PDF text extraction.
- [x] Configure multipart request and file-size limits.
- [x] Add ingestion response DTOs and typed error responses.
- [x] Introduce `CvDocument`, source, status, and repository types.
- [x] Create `CvTextExtractor` and a PDF implementation.
- [x] Implement SHA-256 duplicate detection.
- [x] Implement `POST /api/candidates/import`.
- [x] Add unit tests using representative generated PDFs and verify the API in Docker.

**Exit condition:** a valid text-based PDF creates one candidate and document record with extracted text, and a repeated upload is reported as a duplicate.

### Phase 2 - ZIP and built-in archive ingestion

- [x] Implement a streaming, guarded ZIP ingestion service.
- [x] Add `POST /api/candidates/import/archive`.
- [x] Add `POST /api/candidates/import/initial` using `classpath:intial/CVs.zip`.
- [x] Return per-file imported, duplicate, needs-review, skipped, and failed outcomes.
- [x] Test traversal rejection, unsupported entries, PDF validation, and archive continuation.
- [x] Import the 35 built-in PDFs through the running Docker backend and verify repeat-import idempotency.

**Exit condition:** the built-in archive and an equivalent uploaded archive use the same code path, repeated imports are idempotent, and one bad entry does not fail the entire batch.

### Phase 3 - Metadata quality and user workflow

- [x] Extract candidate name and email deterministically where possible.
- [ ] Add AI-assisted metadata extraction only for fields that cannot be reliably parsed, with confidence and provenance.
- [ ] Detect probable templates and non-CV documents for manual review.
- [ ] Add candidate detail and ingestion-status APIs.
- [x] Build Angular PDF/ZIP upload, bulk-result, and explicit evaluation views.
- [ ] Add correction and reprocessing workflows.
- [ ] Define secure original-document storage, retention, and deletion.

**Exit condition:** recruiters can upload, review, correct, and manage CV ingestion outcomes without direct database access.

### Phase 4 - Evaluation hardening

- [x] Validate candidate and job IDs with typed not-found responses.
- [x] Validate every metric range and require custom weights to be non-negative and total 100%.
- [x] Return a typed failure instead of persisting zero-score parse failures.
- [ ] Use provider-level schema-constrained AI output.
- [ ] Persist metric-level evidence, weights, prompt version, model, source document version, and evaluation duration.
- [ ] Separate AI confidence from candidate-fit scoring.
- [ ] Change employment gaps to neutral review flags unless an approved policy requires otherwise.
- [ ] Add candidate ranking, evaluation retrieval, and controlled re-evaluation APIs.
- [ ] Add bias, consistency, prompt-injection, adversarial-CV, and regression datasets.

**Exit condition:** evaluations are validated, explainable, reproducible, versioned, and never convert technical failure into a candidate score.

### Phase 5 - Security and operations

- [ ] Require authentication and role-based authorization.
- [ ] Restrict CORS and administrative initial-data loading.
- [ ] Add malware scanning before parsing user uploads.
- [ ] Apply candidate consent, retention, deletion, and audit policies.
- [ ] Prevent CV content from overriding evaluation system instructions.
- [ ] Add structured ingestion and evaluation metrics without logging CV content.
- [ ] Monitor parsing failures, duplicates, OCR needs, latency, model usage, cost, and evaluation drift.

**Exit condition:** the workflow meets agreed security, privacy, operational, and data-governance requirements.

## Implemented frontend workflow

The Angular frontend now provides a dedicated `/candidates/import` workspace and primary navigation linking job generation, approved jobs, and CV evaluation.

Implemented behavior:

1. A typed `CvIngestionService` sends PDF/ZIP `FormData`, loads built-in data, retrieves candidates/jobs, and requests evaluation.
2. Separate PDF and ZIP file pickers enforce extensions and mirror the backend's 20 MB/100 MB limits before submission.
3. A clearly labelled development/administrative action loads the packaged CV archive.
4. Active requests disable conflicting actions and show operation-specific loading labels.
5. Import totals and every per-file status, candidate ID, extracted-text length, and warning are displayed.
6. Backend `ApiError.message` values are surfaced without exposing stack traces.
7. Candidate and approved-job selectors refresh after ingestion and evaluation only starts when the user presses **Evaluate candidate**.
8. The result view shows overall fit, all eight metrics, and the AI explanation.
9. Angular tests cover all ingestion service endpoints, multipart request construction, structured errors, routing/navigation, file validation, loading states, result rendering, selection gating, explicit evaluation, metric rendering, and provider failures.

Remaining frontend improvements:

- Add drag-and-drop and upload byte-progress/cancellation.
- Add filtering/pagination for large archive result sets.
- Link result rows to a dedicated candidate/document detail view with bounded extracted-text preview.
- Hide the built-in-data action based on authenticated administrator permissions.
- Add correction/reprocessing screens and full accessibility/end-to-end automation.

### Frontend acceptance criteria

- [x] A recruiter can upload one PDF and see its final status and candidate ID.
- [x] A recruiter can upload one ZIP and inspect every entry outcome without losing partial successes.
- [x] The built-in archive can be loaded and duplicate results are visible on repeat runs.
- [x] `NEEDS_REVIEW`, `SKIPPED`, and `FAILED` states have distinct labels and explanations.
- [x] Uploading never triggers evaluation; evaluation requires an explicit candidate/job action.
- [x] Backend errors and size restrictions are represented consistently.
- [ ] Administrative authorization, candidate detail links, upload progress, and full end-to-end browser tests are complete.

## How to test the complete workflow

### Prerequisites

1. Set `OPENAI_API_KEY` in the environment used by Docker Compose. Both job generation and CV evaluation call the configured model; ingestion itself does not require the key.
2. From the repository root, run `docker compose up --build`.
3. Wait until PostgreSQL, backend, and frontend are ready, then open `http://localhost:4200`.

### Job offer generation and approval

1. Open **Generate job**.
2. Enter a concrete brief such as “Senior Java engineer building Spring Boot APIs with PostgreSQL and Docker” and complete the required fields.
3. Press **Generate Job Offer** and verify that the preview contains a title, summary, responsibilities, and qualifications.
4. Optionally edit the generated content, then press **Approve Job Offer**.
5. Open **Approved jobs** and verify that the approved job appears. Approval is essential because evaluation requires a persisted job ID; a generated preview alone cannot be evaluated.

### CV ingestion

1. Open **CVs & Evaluation**.
2. For a single-file test, choose a text-based PDF and press **Upload PDF**. Verify `IMPORTED`, a candidate ID, and a non-zero text length.
3. Upload the same PDF again and verify `DUPLICATE` with the existing candidate ID.
4. For bulk testing, choose a ZIP containing PDFs and optional unsupported files. Verify that each entry receives its own result and that unsupported files are `SKIPPED` without losing successful imports.
5. Alternatively, press **Load built-in CVs**. The first run should import the packaged documents; later runs should report duplicates. Image-only or very low-text PDFs should be `NEEDS_REVIEW`.

### Candidate evaluation against the job

1. In Step 2 of **CVs & Evaluation**, select an imported candidate and the approved job created above.
2. Press **Evaluate candidate**. This is the only action that calls the evaluation model; uploading a CV must not trigger it.
3. Verify that the response displays an overall score from 0–100, all eight bounded metrics, and an explanation referring to the selected job and CV.
4. Repeat with a different candidate against the same job to confirm job-specific comparison, or the same candidate against another approved job to confirm that ingestion and evaluation are separate reusable stages.

### Expected failure checks

- Upload a renamed non-PDF file and verify a clear unsupported-file error.
- Upload a ZIP containing `../` traversal and verify rejection.
- Attempt evaluation without a candidate or job and verify that the button remains disabled.
- Stop or misconfigure the AI provider and verify that generation/evaluation show a technical error rather than a valid-looking zero score.
- Confirm that an ingestion request still works while the AI provider is unavailable.

## Known evaluation limitations

1. The AI explanation is not broken into metric-level evidence or linked to source text.
2. Model, prompt, weights, and source-document versions are not persisted for reproducibility.
3. Employment gaps and AI confidence currently affect the overall candidate score.
4. CV content is inserted into the model prompt without explicit prompt-injection defenses.
5. Provider-level structured-output constraints are not yet enabled.
6. Provider-level, PostgreSQL/Testcontainers, adversarial, and full end-to-end evaluation coverage remains incomplete.
7. Candidate endpoints return persistence entities directly and currently permit unrestricted development CORS.

## Acceptance criteria

### Currently satisfied or substantially implemented

- [x] Candidate records can store extracted CV text.
- [x] Candidates can be created from JSON and listed.
- [x] An existing candidate can be evaluated against an existing job.
- [x] All eight source metrics and a composite score are represented and persisted.
- [x] Default metric weights are implemented.
- [x] Protected demographic characteristics are explicitly excluded by the evaluation prompt.
- [x] A built-in 35-PDF archive is packaged as a backend classpath resource.

### Required for ingestion completion

- [x] The backend accepts a supported PDF through `multipart/form-data`.
- [x] The backend accepts a ZIP and returns an outcome for every file entry.
- [x] The backend idempotently imports `classpath:intial/CVs.zip` through the shared ZIP pipeline.
- [x] Unsupported, unsafe, oversized, and image-only files receive explicit outcomes.
- [x] Duplicate documents are detected by content hash and do not create duplicate candidates.
- [x] Per-entry parsing failures do not prevent later archive entries from being processed.
- [x] Document metadata, source, status, extracted text, and result warnings are traceable.
- [ ] Original binary documents are stored and governed by retention/deletion policy.
- [ ] OCR and reprocessing are available for scanned PDFs.
- [x] Upload, ingestion-result, candidate/job selection, and evaluation workflows are available in the frontend.

### Required for evaluation production readiness

- [ ] Every score is range-validated and includes traceable job/CV evidence.
- [x] Custom weights are validated, non-negative, and total 100%.
- [ ] Low-confidence extraction and evaluation trigger human review and are never presented as certainty.
- [ ] Employment gaps are neutral review flags rather than automatic rejection or ranking penalties.
- [x] Malformed model output does not create a valid-looking zero-score evaluation.
- [ ] Re-evaluation preserves prior results and all inputs needed for reproducibility.
- [ ] Authentication, authorization, privacy controls, and audit history are enforced.
- [ ] Bias, consistency, adversarial, integration, and end-to-end tests pass.
