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
- Graceful conversion of malformed JSON into zero scores and a parsing explanation.
- Composite-score normalization and weighting.

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

The weights total 100%, but custom weights are not validated. The source groups metrics as 40% skills/experience, 30% education/achievements, and 30% quality/risk; the implemented individual weights are one interpretation of that incomplete formula.

AI confidence should ultimately be separated from candidate quality and used to trigger human verification instead of improving or reducing candidate fit.

## Missing ingestion capabilities

The backend does not currently provide:

- A `multipart/form-data` CV upload endpoint.
- PDF text extraction through PDFBox, Tika, or another parser.
- Safe ZIP enumeration and ingestion.
- Automatic loading of the built-in archive.
- File extension, MIME type, signature, or size validation.
- ZIP path-traversal, decompression-bomb, entry-count, or expansion-size protection.
- Original-document storage or document lifecycle metadata.
- SHA-256 duplicate detection.
- Per-file ingestion status, warnings, and error reporting.
- OCR or a clear result for image-only PDFs.
- Candidate metadata extraction from CV content.
- An upload or ingestion-results interface in Angular.

## Planned ingestion API

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

Initial archives of the built-in dataset size can be processed synchronously. Larger production archives should create an asynchronous import job and return `202 Accepted`.

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
      "filename": "CVs/amanda-akins-cv.pdf",
      "candidateId": 42,
      "status": "IMPORTED",
      "warnings": []
    },
    {
      "filename": "CVs/cv-template.pdf",
      "candidateId": null,
      "status": "SKIPPED",
      "warnings": ["Document appears to be a CV template"]
    }
  ]
}
```

### Load built-in CV data

Provide an explicit administrative or startup operation that opens `classpath:intial/CVs.zip` and passes it through the same archive-ingestion service used by user uploads.

Recommended initial endpoint:

```http
POST /api/candidates/import/initial
```

Requirements:

- Do not duplicate candidates when the operation is called more than once.
- Restrict the endpoint to development/administrative use.
- Return the same bulk-import result contract as an uploaded ZIP.
- Do not embed separate parsing logic for built-in data.
- Do not automatically evaluate imported candidates.

## Planned data model

Candidate identity and uploaded-document lifecycle should be separate concerns. Introduce a `CvDocument` entity related to `Candidate` with:

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
- Extraction confidence or warning state
- Import timestamp

The original file should be stored through a storage abstraction rather than in the candidate database row. Local filesystem storage can be used for development; object storage is preferable for production.

## Planned ingestion pipeline

1. Accept an individual PDF, uploaded ZIP, or built-in classpath archive.
2. Enforce request-size and archive-size limits before processing.
3. Validate filename, extension, declared MIME type, and file signature.
4. For ZIPs, stream entries without extracting them to arbitrary filesystem paths.
5. Reject absolute paths and `..` path traversal.
6. Enforce entry-count, per-entry-size, total-expanded-size, and compression-ratio limits.
7. Calculate a SHA-256 hash and detect previously imported content.
8. Extract PDF text with a dedicated `CvTextExtractor` abstraction.
9. Flag empty or image-only documents for OCR/manual review rather than creating misleading content.
10. Extract conservative candidate metadata; unknown fields remain null.
11. Persist the document record, candidate, extracted text, status, and warnings.
12. Continue an archive import when an individual entry fails.
13. Return a complete per-file outcome without exposing internal stack traces.

## Delivery plan

### Phase 1 - PDF ingestion foundation

- [ ] Add Apache PDFBox for PDF text extraction.
- [ ] Configure multipart request and file-size limits.
- [ ] Add ingestion response DTOs and typed error responses.
- [ ] Introduce `CvDocument`, source, status, and repository types.
- [ ] Create `CvTextExtractor` and a PDF implementation.
- [ ] Implement SHA-256 duplicate detection.
- [ ] Implement `POST /api/candidates/import`.
- [ ] Add unit and integration tests using representative PDFs.

**Exit condition:** a valid text-based PDF creates one candidate and document record with extracted text, and a repeated upload is reported as a duplicate.

### Phase 2 - ZIP and built-in archive ingestion

- [ ] Implement a streaming, guarded ZIP ingestion service.
- [ ] Add `POST /api/candidates/import/archive`.
- [ ] Add `POST /api/candidates/import/initial` using `classpath:intial/CVs.zip`.
- [ ] Return per-file imported, duplicate, skipped, and failed outcomes.
- [ ] Test path traversal, decompression limits, corrupt entries, unsupported entries, and partial failures.
- [ ] Import the 35 built-in PDFs and review warnings and failures.

**Exit condition:** the built-in archive and an equivalent uploaded archive use the same code path, repeated imports are idempotent, and one bad entry does not fail the entire batch.

### Phase 3 - Metadata quality and user workflow

- [ ] Extract candidate email and other high-confidence metadata deterministically.
- [ ] Add AI-assisted metadata extraction only for fields that cannot be reliably parsed, with confidence and provenance.
- [ ] Detect probable templates and non-CV documents for manual review.
- [ ] Add candidate detail and ingestion-status APIs.
- [ ] Build Angular PDF/ZIP upload and bulk-result views.
- [ ] Add correction and reprocessing workflows.
- [ ] Define secure original-document storage, retention, and deletion.

**Exit condition:** recruiters can upload, review, correct, and manage CV ingestion outcomes without direct database access.

### Phase 4 - Evaluation hardening

- [ ] Validate candidate and job IDs with typed not-found responses.
- [ ] Validate every metric range and require custom weights to be non-negative and total 100%.
- [ ] Use schema-constrained AI output and return a typed failure instead of persisting zero-score parse failures.
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

## Known evaluation limitations

1. Custom weights can be null, negative, or fail to total 100% without rejection.
2. Metric values are not range-validated before persistence.
3. A malformed model response becomes a persisted zero-score evaluation rather than a technical failure.
4. The AI explanation is not broken into metric-level evidence or linked to source text.
5. Model, prompt, weights, and source-document versions are not persisted for reproducibility.
6. Candidate/job not-found errors are generic exceptions without a consistent API problem format.
7. Employment gaps and AI confidence currently affect the overall candidate score.
8. CV content is inserted into the model prompt without explicit prompt-injection defenses.
9. Only service-level parsing and composite-score tests exist; controller, persistence, provider-failure, and end-to-end coverage are missing.
10. Candidate endpoints return persistence entities directly and currently permit unrestricted development CORS.

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

- [ ] Users can upload a supported PDF through `multipart/form-data`.
- [ ] Users can upload a ZIP and receive an outcome for every supported entry.
- [ ] Administrators can idempotently import `classpath:intial/CVs.zip` through the shared ZIP pipeline.
- [ ] Unsupported, corrupt, unsafe, oversized, and image-only files receive clear outcomes.
- [ ] Duplicate documents are detected by content hash and do not create duplicate candidates.
- [ ] Partial archive failures do not roll back successful independent entries.
- [ ] Original-document metadata, source, status, warnings, and extracted text are traceable.
- [ ] Upload and ingestion-result workflows are available in the frontend.

### Required for evaluation production readiness

- [ ] Every score is range-validated and includes traceable job/CV evidence.
- [ ] Custom weights are validated and total 100%.
- [ ] Low-confidence extraction and evaluation trigger human review and are never presented as certainty.
- [ ] Employment gaps are neutral review flags rather than automatic rejection or ranking penalties.
- [ ] Technical/model failures do not create valid-looking zero-score evaluations.
- [ ] Re-evaluation preserves prior results and all inputs needed for reproducibility.
- [ ] Authentication, authorization, privacy controls, and audit history are enforced.
- [ ] Bias, consistency, adversarial, integration, and end-to-end tests pass.
