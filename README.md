# HR AI Recruitment Manager

An AI-assisted recruitment application for generating job offers, evaluating candidates, and managing recruitment workflows.

## Repository Transfer

We started working on this project for the 1st Edition of [KLx, Crédit Agricole Group](https://www.klx.pt/) https://github.com/hack6nov2025/nevgiu-hr-ai then move to this NevGiU AI Org

## Technology stack

- Angular frontend served by Nginx
- Spring Boot backend running on Java 21
- PostgreSQL 16 with pgvector
- Spring AI with OpenAI
- Docker Compose for local development

## Run locally with Docker

### Prerequisites

Install and start:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Docker Compose, included with current Docker Desktop versions

Verify that Docker is ready:

```bash
docker info
docker compose version
```

### 1. Clone and enter the repository

```bash
git clone <repository-url>
cd <repository-directory>
```

Use the HTTPS or SSH URL shown by the **Code** button on the current GitHub repository. This keeps the instructions valid if the repository is transferred or renamed.

### 2. Configure the OpenAI API key

Create a `.env` file in the repository root:

```env
OPENAI_API_KEY=your-openai-api-key
```

The key is passed to the backend container and is required for AI job-offer generation. Do not commit the `.env` file or expose the key in logs or screenshots.

CV ingestion and text extraction do not call OpenAI. An API key is required only when generating a job offer or explicitly evaluating a candidate against an approved job.

### 3. Build and start the application

From the repository root, run:

```bash
docker compose up -d --build
```

The first build can take several minutes because Docker downloads the Node, Maven, Java, Nginx, and PostgreSQL dependencies.

### 4. Check service status

```bash
docker compose ps
```

The `db`, `backend`, and `frontend` services should be running. The database should report `healthy`, and the backend may briefly report `health: starting` while Spring Boot initializes.

Check backend health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

### 5. Open the application

- Frontend: [http://localhost:4200](http://localhost:4200)
- Jobs API: [http://localhost:8080/api/jobs](http://localhost:8080/api/jobs)
- Candidates API: [http://localhost:8080/api/candidates](http://localhost:8080/api/candidates)
- Backend health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- PostgreSQL: `localhost:5433`

The PostgreSQL container listens on port `5432` inside the Docker network and is exposed as `5433` on the host to avoid conflicts with another local PostgreSQL instance.

## Deployment domains

HR AI is hosted under the `hr` project namespace of `nevgiuai.com`:

| Environment | Frontend | Backend API |
| --- | --- | --- |
| Production | `https://hr.nevgiuai.com` | `https://api.hr.nevgiuai.com` |
| Staging | `https://staging-hr.nevgiuai.com` | `https://staging-api.hr.nevgiuai.com` |

Local development continues to use `http://localhost:4200` and `http://localhost:8080`. Deployment DNS records, TLS routing, GitHub environment URLs, frontend builds, backend CORS origins, and smoke tests must use the environment-specific domains above.

## Complete recruitment workflow

1. Open **Generate job**, enter a role description, and generate a job-offer draft.
2. Review the draft. Select **Edit**, make any required changes, and approve it. Only approved jobs are persisted and available for candidate evaluation.
3. Open **CVs & Evaluation**.
4. Import one PDF, a ZIP archive, or the packaged built-in CV dataset.
5. Select an imported candidate and an approved job, then select **Evaluate candidate**.
6. Review the overall fit score, eight individual metrics, and AI explanation. Scores are decision-support information and do not replace human review.

Uploading a CV never starts an evaluation automatically. This makes ingestion reusable and prevents unexpected model calls: the same candidate can be evaluated against different approved jobs.

### CV import options and outcomes

The frontend supports:

- A single text-based PDF, limited to 20 MB.
- A ZIP archive containing PDFs, limited to a 100 MB request.
- The built-in archive packaged at `backend/src/main/resources/intial/CVs.zip` for development and demonstration.

Each document receives one of these outcomes:

| Status | Meaning |
| --- | --- |
| `IMPORTED` | Text was extracted and a candidate was created. |
| `DUPLICATE` | The same file content was imported previously; no duplicate candidate was created. |
| `NEEDS_REVIEW` | Too little usable text was extracted, commonly because OCR is required. |
| `SKIPPED` | The archive entry is unsupported or is not a PDF. |
| `FAILED` | Validation, extraction, or persistence failed for that entry. |

Candidate names are conservatively inferred from filenames and email addresses from extracted text. Review inferred metadata before relying on it. The application currently retains extracted text and document metadata, but not the original PDF binary.

## Run automated tests

Run backend tests:

```bash
cd backend
mvn test
```

Maven 3 and Java 21 must be available on the local `PATH`.

Run frontend tests:

```bash
cd frontend
npm ci
npm test -- --watch=false
```

The automated suites use mocked AI dependencies; an OpenAI API key is not required for these tests.

## Useful Docker commands

Follow application logs:

```bash
docker compose logs -f backend frontend
```

Rebuild after source or dependency changes:

```bash
docker compose up -d --build
```

Restart the services without rebuilding:

```bash
docker compose restart
```

Stop and remove the application containers:

```bash
docker compose down
```

Stop the application and remove its database volume:

```bash
docker compose down -v
```

> `docker compose down -v` permanently deletes the local PostgreSQL data managed by this Compose project.

This includes approved jobs, candidates, CV document metadata and extracted text, and persisted evaluations.

## Troubleshooting

### A port is already allocated

Check which containers are using host ports:

```bash
docker ps
```

The default application ports are:

- `4200` for the frontend
- `8080` for the backend
- `5433` for PostgreSQL host access

Stop the conflicting application or update the corresponding host-side port in `docker-compose.yml`.

### The backend does not start

Inspect its logs:

```bash
docker compose logs --tail 200 backend
```

Confirm that:

- The `db` service is healthy.
- `OPENAI_API_KEY` exists in the root `.env` file.
- Port `8080` is available.

### The frontend image fails with an esbuild platform mismatch

Ensure `frontend/.dockerignore` exists and excludes `node_modules`, `dist`, and `.angular`. Host-generated dependencies must not be copied into the Linux image.
