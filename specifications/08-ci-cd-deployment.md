# CI/CD and VPS Deployment

## Objective

Deploy tested, immutable application images to an isolated staging environment and promote the same artifacts to production only after the release is approved.

The deployment design must support:

- Automated validation of every pull request and branch update.
- Automatic staging deployment after `main` passes CI.
- Production deployment from a versioned release with explicit approval.
- Environment-specific secrets and configuration.
- Health-gated rollout and rollback.
- PostgreSQL backup and restore.
- Appropriate protection for candidate CV data.

## Target delivery flow

```text
Pull request
    |
    v
Backend and frontend CI
    |
    v
Merge to main
    |
    v
Build images once and publish immutable SHA tags to GHCR
    |
    v
Automatically deploy those images to staging
    |
    v
Run health checks and smoke tests
    |
    v
Create a semantic version tag and GitHub Release
    |
    v
Approve the protected production environment
    |
    v
Promote the staging-tested image digests to production
    |
    v
Run health checks or roll back to the previous release
```

The production workflow must not rebuild the source. It must deploy the exact image digests already validated in staging.

## Current repository assessment

### Existing CI

`.github/workflows/ci.yml` currently:

- Runs on pushes and pull requests.
- Executes Maven `clean verify` for the backend.
- Installs frontend dependencies and builds Angular.
- Builds backend and frontend Docker images locally.

Required improvements:

- Replace `npm install` with deterministic `npm ci`.
- Run Angular tests in CI rather than only building the application.
- Cache Maven and npm dependencies where it improves execution time without weakening reproducibility.
- Make CI a required status check on the protected `main` branch.
- Consider dependency, secret, and container-image scanning.

### Existing CD

`.github/workflows/cd.yml` currently:

- Runs for semantic-looking `v*.*.*` tags.
- Builds and pushes backend and frontend images to GHCR.
- Connects to an Azure VM over SSH.
- Mutates a production Compose file with `sed` and starts it.

Problems to correct:

- Image names still use the obsolete `finance-ai-*` identifiers.
- The workflow is tied to Azure-specific secret names and paths.
- Images are rebuilt for production instead of promoting staging-tested digests.
- Compose mutation with `sed` is brittle and makes rollback difficult.
- There is no staging deployment, health gate, smoke test, concurrency control, or automatic rollback.
- Production is not protected by a GitHub environment approval.

`.github/workflows/runner.yml` runs on a self-hosted production-like runner when `main` changes, but only builds Compose images. It neither starts nor validates the deployment. Running a GitHub Actions runner on an application VPS also increases attack surface. Remove or disable this workflow after the SSH-based deployment pipeline replaces it.

## Recommended GitHub workflows

### 1. Continuous integration

Triggers:

- Every pull request.
- Every push, including `main`.

Required jobs:

1. Backend verification with Java 21 and `mvn -B clean verify`.
2. Frontend verification with Node 20, `npm ci`, headless Angular tests, and `npm run build`.
3. Docker build verification for both images.
4. Optional security jobs for dependency review, secret scanning, SBOM generation, and image vulnerability scanning.

CI must not require an OpenAI API key. Automated tests should use mocks or test doubles for AI calls.

### 2. Build and publish images

Trigger:

- A successful CI run for `main`, or a reusable workflow explicitly called after CI.

Behavior:

1. Log in to `ghcr.io` with the workflow `GITHUB_TOKEN`.
2. Build backend and frontend images with Buildx.
3. Publish immutable tags based on the full Git commit SHA.
4. Capture the content digest of each image.
5. Optionally publish a mutable `staging` alias for operator convenience, but never use it as the deployment source of truth.
6. Add build provenance and an SBOM when the repository's GitHub plan and security process support them.

Recommended image names:

```text
ghcr.io/<owner>/<repository>/hr-ai-backend:<git-sha>
ghcr.io/<owner>/<repository>/hr-ai-frontend:<git-sha>
```

Deployments should ultimately reference digests:

```text
ghcr.io/<owner>/<repository>/hr-ai-backend@sha256:<digest>
```

### 3. Automatic staging deployment

Trigger:

- Successful publication of images for `main`.

GitHub environment:

- Name: `staging`.
- Restrict deployment to `main`.
- Store staging-only SSH credentials, host details, URLs, and application secrets.

Behavior:

1. Prevent concurrent staging deployments with a GitHub Actions concurrency group.
2. Copy or select a versioned deployment manifest without editing it in place.
3. Pull the immutable backend and frontend image digests.
4. Preserve the previously deployed digest for rollback.
5. Start the Compose project.
6. Wait for PostgreSQL and backend health checks.
7. Run smoke tests against the public staging URLs.
8. Mark the deployment successful only after every check passes.
9. Roll back to the previous digests if startup or smoke testing fails.

Minimum staging smoke tests:

- `GET /actuator/health` returns `200` and `{"status":"UP"}`.
- The frontend root returns `200`.
- Angular routes fall back correctly through Nginx.
- A safe read-only jobs endpoint returns a valid response.
- A safe read-only candidates endpoint returns a valid response.
- Database connectivity is confirmed indirectly through backend health and API checks.

Live OpenAI smoke tests should be separate, opt-in, rate-limited tests. They should not run on every deployment because of cost and provider variability.

### 4. Approved production promotion

Trigger:

- A published GitHub Release or semantic version tag such as `v1.2.0`.
- Optional manual `workflow_dispatch` for an explicitly selected, previously validated image SHA.

GitHub environment:

- Name: `production`.
- Restrict deployment to approved release tags.
- Require a reviewer when the repository visibility and GitHub plan support environment reviewers.
- Prevent self-review where operationally appropriate.
- Store production-only secrets; do not reuse staging credentials.

Release readiness should require:

- CI passed for the release commit.
- The exact image digests were successfully deployed and smoke-tested in staging.
- Product acceptance criteria for the release are met.
- Database migration and rollback compatibility have been reviewed.
- Backup freshness and restore procedures have been confirmed.
- Known security and privacy risks have been accepted or resolved.

Production behavior:

1. Resolve the staging-tested image digests associated with the release commit.
2. Take or verify a recent PostgreSQL backup before database-affecting changes.
3. Deploy the immutable digests with a production Compose project.
4. Run health checks and production-safe smoke tests.
5. Roll back application images if validation fails.
6. Record the deployed version, commit, digests, actor, and timestamp.

Database rollback is not the same as application rollback. Migrations must be backward compatible whenever possible, and destructive migrations require a separate reviewed recovery plan.

## VPS recommendation

### Recommended initial provider: OVHcloud

Use two separate OVHcloud VPS instances, one for staging and one for production. At the time this specification was updated, the advertised VPS-2 configuration included approximately:

- 4 vCores.
- 8 GB RAM.
- 75 GB NVMe storage.
- Automated one-day backup.
- Unlimited traffic and integrated DDoS protection.
- An entry price around EUR 7.21 excluding VAT per VPS per month.

Two instances would therefore start around EUR 14.42 excluding VAT per month before optional services. Pricing, tax, commitment, location, backup retention, and availability must be confirmed at checkout.

Official pricing: <https://www.ovhcloud.com/nl/vps/>

Why this is suitable:

- 8 GB RAM is a reasonable starting point for PostgreSQL, Spring Boot, Nginx, Docker, and lightweight monitoring.
- Separate machines prevent staging deployments and load from affecting production.
- European hosting is preferable for personal CV data, subject to the final legal and data-residency review.
- Included infrastructure backup and DDoS protection improve the starting operational baseline.

### Alternatives

**Hetzner Cloud** often offers strong European price/performance. Its prices changed substantially in June 2026, so the current instance specification and final price must be verified when ordering.

Official pricing notice: <https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/>

**Scaleway DEV1-L** was advertised with 4 vCPUs and 8 GB RAM at approximately EUR 31.27 per month before tax. Public IPv4 and some storage can be billed separately. It is a reasonable alternative when a France-based European cloud or Scaleway's wider managed-service ecosystem is preferred.

Official pricing: <https://www.scaleway.com/en/pricing/virtual-instances/>

### Single-VPS option

For a short-lived demonstration, staging and production can run on one VPS as isolated Compose projects, using separate networks, volumes, secrets, ports, and hostnames. This reduces cost but creates shared-resource and shared-failure risks.

Do not use the single-VPS model once real candidate data or meaningful production traffic is introduced.

## Target server architecture

Recommended directory layout on each VPS:

```text
/opt/nevgiu/
|-- compose.yml
|-- .env
|-- deploy.sh
|-- backups/
`-- data/
    `-- postgres/
```

Use Caddy or Traefik as the public reverse proxy and automatic TLS terminator.

Canonical HR AI hostnames:

```text
staging-hr.nevgiuai.com
staging-api.hr.nevgiuai.com
hr.nevgiuai.com
api.hr.nevgiuai.com
```

The reusable Nevgiu AI convention is:

| Environment | Frontend | Backend API |
| --- | --- | --- |
| Production | `<project>.nevgiuai.com` | `api.<project>.nevgiuai.com` |
| Staging | `staging-<project>.nevgiuai.com` | `staging-api.<project>.nevgiuai.com` |

For this application, `<project>` is `hr`. The apex `nevgiuai.com` remains available for the Nevgiu AI company and project portal.

Environment configuration must use:

| Environment | Angular API URL | `APP_CORS_ALLOWED_ORIGINS` |
| --- | --- | --- |
| Production | `https://api.hr.nevgiuai.com/api` | `https://hr.nevgiuai.com` |
| Staging | `https://staging-api.hr.nevgiuai.com/api` | `https://staging-hr.nevgiuai.com` |

The Angular `production` and `staging` build configurations embed their corresponding API URL. The backend reads allowed browser origins from `APP_CORS_ALLOWED_ORIGINS`; do not add API hostnames to CORS unless a browser application is actually served from those hosts.

Only the following ports should be publicly reachable:

- `22/tcp` for restricted SSH administration.
- `80/tcp` for HTTP-to-HTTPS redirection and ACME challenges.
- `443/tcp` for HTTPS.

PostgreSQL port `5432`, the current host mapping `5433`, and backend port `8080` must not be exposed publicly. The reverse proxy, frontend, backend, and database should communicate through private Docker networks.

Server hardening should include:

- SSH keys only; disable password and root login.
- A firewall allowing only required ports.
- Automated security updates or an agreed patch schedule.
- Fail2ban or equivalent SSH abuse protection where appropriate.
- A non-root deployment user with narrowly scoped permissions.
- Docker log rotation and disk-space alerts.
- Time synchronization.
- Monitoring of CPU, memory, disk, container health, certificate expiry, and backup success.

## Production Compose requirements

Create a production-specific Compose file instead of deploying the current development file unchanged.

Required changes:

- Use prebuilt GHCR image references supplied through environment variables.
- Remove application `build` directives.
- Remove the PostgreSQL host port.
- Do not expose backend port `8080` publicly.
- Replace hardcoded database credentials with high-entropy environment secrets.
- Use separate staging and production database credentials.
- Add explicit health checks and appropriate restart policies.
- Add bounded CPU and memory configuration where supported.
- Put services on separate public-proxy and private-data networks.
- Disable built-in CV import in production with `app.cv-ingestion.initial-import-enabled=false`.
- Disable SQL logging in production.
- Replace Hibernate `ddl-auto: update` with Flyway or Liquibase migrations before production data is introduced.
- Configure trusted frontend origins instead of unrestricted development CORS.
- Configure structured logs without CV text, prompts, API keys, or personal data.

## Secrets and GitHub environments

Use separate GitHub `staging` and `production` environments. Environment secrets become available only to jobs targeting that environment and, when protection rules are configured, only after approval.

Suggested environment secrets:

```text
DEPLOY_HOST
DEPLOY_PORT
DEPLOY_USER
DEPLOY_SSH_KEY
GHCR_PULL_USERNAME
GHCR_PULL_TOKEN
OPENAI_API_KEY
POSTGRES_PASSWORD
```

Suggested non-secret GitHub environment variables include:

```text
FRONTEND_URL
API_URL
APP_CORS_ALLOWED_ORIGINS
```

Prefer a dedicated read-only GHCR token on the VPS if private packages require authentication. Do not place secrets in Compose files, workflow logs, image layers, repository variables, or command-line output.

GitHub documentation:

- Environments and protection rules: <https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments>
- Publishing Docker images: <https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images>
- Artifact attestations: <https://docs.github.com/en/actions/how-tos/secure-your-work/use-artifact-attestations/use-artifact-attestations>

Required-reviewer availability depends on repository visibility and GitHub plan. Confirm those constraints before relying on GitHub approval as the only production gate.

## Database backup and recovery

Provider snapshots and VPS backups do not replace application-aware PostgreSQL backups.

Minimum policy:

- Run an encrypted daily `pg_dump` or equivalent PostgreSQL backup.
- Keep backups outside the application VPS and preferably in a separate account or object-storage service.
- Define retention tiers, for example seven daily, four weekly, and three monthly copies, subject to candidate-data retention policy.
- Record and alert on backup success or failure.
- Test restoration regularly on an isolated environment.
- Back up before risky schema changes and releases.
- Document recovery point and recovery time objectives.
- Ensure expired candidate data is removed from active data and backups according to the approved governance policy.

## Rollback strategy

Before every deployment, record the currently running backend and frontend image digests.

If startup or smoke tests fail:

1. Restore the previous image digests.
2. Run `docker compose pull` and start the previous application version.
3. Repeat health and smoke tests.
4. Mark the deployment failed and retain diagnostic logs without personal data.

Do not automatically restore a database backup for ordinary application failure. Database restoration is destructive and requires an explicit incident decision based on the migration and data-loss impact.

## Data protection and production blockers

CVs contain personal data. Before real candidate information is deployed, resolve:

- Authentication and role-based authorization.
- Candidate lawful processing basis, notice/consent, retention, deletion, correction, and audit policies.
- Organization or tenant isolation.
- Restricted CORS.
- Malware scanning for uploads.
- Whether and where original CV files will be retained.
- OpenAI data-processing terms and disclosure that CV text is sent to the provider only during explicit evaluation.
- Prompt-injection defenses for untrusted CV content.
- Log and monitoring redaction.
- Administrator-only access to built-in dataset import, or disable it in production.

The deployment pipeline can be built before these items are complete, but the environment must not be described as production-ready for real candidate data until the blockers are addressed.

## Delivery plan

### Phase 1 - Production-ready container configuration

- [x] Add a provider-neutral deployment Compose file.
- [x] Add a Caddy reverse proxy with automatic TLS.
- [x] Externalize database and application secrets through the server environment file.
- [x] Remove public database and backend ports from the deployment topology.
- [x] Add environment-driven production Spring settings.
- [x] Make built-in CV import environment-controlled and document that production disables it.
- [ ] Add deployment and rollback scripts.
- [ ] Add database backup and restore scripts and test restoration.

**Exit condition:** the application can be deployed manually to a clean VPS over HTTPS without development credentials or public data services.

### Phase 2 - CI and image publication

- [ ] Run backend and frontend tests and builds deterministically.
- [ ] Make CI required for `main`.
- [ ] Rename container packages from `finance-ai-*` to `hr-ai-*`.
- [ ] Build images once and publish SHA tags and digests to GHCR.
- [ ] Add image metadata and optional SBOM/provenance.
- [ ] Remove or disable the self-hosted runner workflow.

**Exit condition:** every accepted `main` commit has traceable, immutable, tested container images.

### Phase 3 - Automatic staging

- [ ] Configure the GitHub `staging` environment and secrets.
- [ ] Deploy every successful `main` image automatically.
- [ ] Add concurrency protection, health waiting, and smoke tests.
- [ ] Add automatic application rollback.
- [ ] Publish deployment URL, commit, and image digests in the workflow summary.

**Exit condition:** a successful merge reaches staging automatically and is accepted only when application checks pass.

### Phase 4 - Approved production promotion

- [ ] Configure the protected `production` environment and separate secrets.
- [ ] Trigger promotion from a semantic version GitHub Release.
- [ ] Verify that image digests passed staging.
- [ ] Require production approval where supported.
- [ ] Check backup readiness before deployment.
- [ ] Add production health checks, rollback, and deployment records.

**Exit condition:** an approved release promotes the exact staging-tested artifacts and can safely return to the previous application version.

### Phase 5 - Operational hardening

- [ ] Add uptime, resource, certificate, disk, container, and backup monitoring.
- [ ] Add dependency and image vulnerability scanning.
- [ ] Add alert routing and an incident runbook.
- [ ] Test recovery from server loss and database corruption.
- [ ] Define release, rollback, patching, and secret-rotation ownership.
- [ ] Review capacity after observing real staging workloads.

**Exit condition:** deployment, backup, recovery, monitoring, and security processes have named owners and have been exercised successfully.

## Acceptance criteria

- [ ] Pull requests cannot merge unless required backend and frontend checks pass.
- [ ] A passing `main` commit publishes immutable images and deploys automatically to staging.
- [ ] Staging deployment fails and rolls back when health or smoke tests fail.
- [ ] Production deployment uses the same image digests validated in staging.
- [ ] Production requires a versioned release and an explicit approval gate where supported.
- [ ] Staging and production use separate hosts or an explicitly accepted temporary isolation model.
- [ ] Environment secrets are isolated and never written to logs or the repository.
- [ ] PostgreSQL and backend application ports are not publicly exposed.
- [ ] HTTPS is enforced for all public traffic.
- [ ] Backups are encrypted, stored off-server, monitored, retained according to policy, and restore-tested.
- [ ] Application rollback is automated and database recovery is documented separately.
- [ ] The deployed commit, release, actor, timestamp, and image digests are traceable.
- [ ] Real candidate data is prohibited until the documented privacy and security blockers are resolved.
