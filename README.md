# HR AI Recruitment Manager

An AI-assisted recruitment application for generating job offers, evaluating candidates, and managing recruitment workflows.

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
git clone https://github.com/githubId/nevgiu-hr-ai.git
cd nevgiu-hr-ai
```

### 2. Configure the OpenAI API key

Create a `.env` file in the repository root:

```env
OPENAI_API_KEY=your-openai-api-key
```

The key is passed to the backend container and is required for AI job-offer generation. Do not commit the `.env` file or expose the key in logs or screenshots.

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
- Backend health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- PostgreSQL: `localhost:5433`

The PostgreSQL container listens on port `5432` inside the Docker network and is exposed as `5433` on the host to avoid conflicts with another local PostgreSQL instance.

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
