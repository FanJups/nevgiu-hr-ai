# VPS deployment bundle

This directory contains the provider-neutral Docker Compose deployment used by staging and production.

For initial OVHcloud provisioning, SSH hardening, Squarespace DNS, credential rotation, troubleshooting, and the production checklist, see `specifications/09-vps-provisioning-runbook.md`.

## Domain mapping

| Environment | Frontend | Backend API |
| --- | --- | --- |
| Staging | `staging-hr.nevgiuai.com` | `staging-api.hr.nevgiuai.com` |
| Production | `hr.nevgiuai.com` | `api.hr.nevgiuai.com` |

## Security properties

- Only Caddy publishes host ports.
- PostgreSQL is reachable only on the internal `data` network.
- The backend is reachable externally only through Caddy.
- Caddy obtains and renews HTTPS certificates automatically.
- Database, Caddy certificate, and Caddy configuration data use named volumes.
- Frontend and backend deployments use environment-provided image references.
- The frontend API URL is injected when its container starts, allowing the same image digest to run in staging and production.
- Backend CORS trusts only the configured frontend URL.
- The built-in CV import can be disabled per environment and must be disabled in production.

## Environment file

Copy `.env.example` to `.env` on the target server and replace every placeholder. Never commit the real `.env` file.

Production must use:

```env
FRONTEND_HOST=hr.nevgiuai.com
API_HOST=api.hr.nevgiuai.com
FRONTEND_URL=https://hr.nevgiuai.com
INITIAL_IMPORT_ENABLED=false
```

Use immutable GHCR image digests once image publication is implemented:

```env
BACKEND_IMAGE=ghcr.io/<owner>/<repository>/hr-ai-backend@sha256:<digest>
FRONTEND_IMAGE=ghcr.io/<owner>/<repository>/hr-ai-frontend@sha256:<digest>
```

`API_URL` is generated from `API_HOST` when the frontend container starts. The frontend image is therefore identical in staging and production.

## GitHub environment configuration

Create separate `staging` and `production` environments in the repository settings.

Each environment requires these variables:

```text
DEPLOY_HOST
DEPLOY_USER
```

Each environment requires these secrets:

```text
DEPLOY_SSH_KEY
DEPLOY_KNOWN_HOSTS
GHCR_USERNAME
GHCR_TOKEN
```

- `DEPLOY_SSH_KEY` is the private key for that environment's dedicated `deploy` account.
- `DEPLOY_KNOWN_HOSTS` contains the trusted SSH host-key line for that VPS. Do not disable host-key checking.
- `GHCR_USERNAME` is the GitHub account permitted to pull the repository's packages.
- `GHCR_TOKEN` is a dedicated token with the minimum package-read permission required by the VPS.

Protect the `production` environment with required reviewers and release-tag restrictions where the repository visibility and GitHub plan support them.

The real application secrets (`OPENAI_API_KEY` and `POSTGRES_PASSWORD`) remain only in `/opt/nevgiu/deploy/.env` on each VPS. They are not GitHub Actions secrets because the deployment workflow does not transmit or rewrite them.

## Validate and start

From the deployment directory:

```bash
docker compose --env-file .env config --quiet
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

For the first manual staging deployment, build the local tags defined in `.env` before starting Compose.

Automated deployments write immutable image references to `.images.env` and invoke `deploy.sh`. The script waits for database, backend, and frontend health, requires Caddy to be running, and restores the previously running application images if validation fails. It does not roll back database contents.

## Verify

```bash
curl --fail https://staging-api.hr.nevgiuai.com/actuator/health
curl --fail --head https://staging-hr.nevgiuai.com/
```

## Stop

```bash
docker compose --env-file .env down
```

Do not add `--volumes` unless permanent database and certificate deletion is explicitly intended.
