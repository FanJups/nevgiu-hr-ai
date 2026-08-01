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
- [ ] Build Angular PDF/ZIP upload and bulk-result views.
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

## Frontend implementation plan

The frontend is intentionally not part of the current backend change. It should be implemented in this order:

1. Add a typed Angular ingestion service that sends `FormData` to the PDF and ZIP endpoints and calls the built-in import endpoint without a file.
2. Add a `/candidates/import` route with separate PDF and ZIP drop zones, file pickers, accepted-extension hints, and client-side size checks that mirror backend limits.
3. Show upload progress, disable repeated submission while a request is active, and allow the user to cancel or retry a user upload where practical.
4. Present archive totals as summary cards and every file outcome in a filterable results table. Show candidate links for imported/duplicate rows and warnings for review, skipped, or failed rows.
5. Put “Load built-in CVs” in a clearly administrative section. Hide or disable it when the backend feature is disabled, and enforce role visibility once authentication exists.
6. Map the backend `ApiError` contract to actionable messages for unsupported files, size limits, unsafe archives, missing records, and evaluation failures. Never expose raw stack traces.
7. Extend candidate list/detail screens with ingestion status, source, original filename, import date, warnings, and a bounded extracted-text preview.
8. Add the explicit evaluation workflow: choose an imported candidate and job, submit `/api/evaluations`, then display the composite score and metric breakdown. Do not evaluate automatically after upload.
9. Add component/service tests for file selection, `FormData`, progress, all result states, duplicate imports, accessibility, and error recovery; add an end-to-end PDF/ZIP ingestion path.

### Frontend acceptance criteria

- [ ] A recruiter can upload one PDF and see its final status and candidate link.
- [ ] A recruiter can upload one ZIP and inspect every entry outcome without losing partial successes.
- [ ] An authorized administrator can load the built-in archive and clearly see duplicate results on repeat runs.
- [ ] `NEEDS_REVIEW`, `SKIPPED`, and `FAILED` states have distinct accessible labels and useful explanations.
- [ ] Uploading never triggers evaluation; evaluation requires an explicit candidate/job action.
- [ ] Backend errors and size restrictions are represented consistently and are covered by tests.

## Known evaluation limitations

1. The AI explanation is not broken into metric-level evidence or linked to source text.
2. Model, prompt, weights, and source-document versions are not persisted for reproducibility.
3. Employment gaps and AI confidence currently affect the overall candidate score.
4. CV content is inserted into the model prompt without explicit prompt-injection defenses.
5. Provider-level structured-output constraints are not yet enabled.
6. Controller, persistence, provider-failure, adversarial, and end-to-end evaluation coverage remains incomplete.
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
- [ ] Upload and ingestion-result workflows are available in the frontend.

### Required for evaluation production readiness

- [ ] Every score is range-validated and includes traceable job/CV evidence.
- [x] Custom weights are validated, non-negative, and total 100%.
- [ ] Low-confidence extraction and evaluation trigger human review and are never presented as certainty.
- [ ] Employment gaps are neutral review flags rather than automatic rejection or ranking penalties.
- [x] Malformed model output does not create a valid-looking zero-score evaluation.
- [ ] Re-evaluation preserves prior results and all inputs needed for reproducibility.
- [ ] Authentication, authorization, privacy controls, and audit history are enforced.
- [ ] Bias, consistency, adversarial, integration, and end-to-end tests pass.
