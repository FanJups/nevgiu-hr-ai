# OVHcloud VPS Provisioning Runbook

## Purpose

Provide a repeatable, security-conscious procedure for preparing the HR AI staging and production VPSs before GitHub Actions performs automated deployments.

This runbook records the staging procedure validated in August 2026. Use the same sequence for production, changing only environment-specific hosts, keys, credentials, and policy choices.

## Infrastructure inventory

Public IP addresses and DNS records are not credentials. Private SSH keys, passwords, API keys, recovery codes, and real environment files must never be added to this document or committed to Git.

| Environment | OVHcloud VPS | Operating system | Public IP | Status |
| --- | --- | --- | --- | --- |
| Staging | VPS-2 | Ubuntu Server 26.04 LTS | `141.94.33.197` | Provisioned and manually deployed |
| Production | VPS-2 | Ubuntu Server 26.04 LTS | `141.94.94.198` | Provisioned; configuration pending |

VPS-2 was selected as the initial size because its 4 vCores, 8 GB RAM, and NVMe storage provide a reasonable baseline for PostgreSQL, Spring Boot, Nginx, Caddy, Docker, and lightweight monitoring.

## DNS inventory

The `nevgiuai.com` domain is registered and authoritatively served through Squarespace DNS. It is not managed by OVHcloud DNS.

Authoritative nameservers at the time of setup:

```text
nsa1.squarespacedns.com
nsa2.squarespacedns.com
nsa3.squarespacedns.com
nsa4.squarespacedns.com
```

| Environment | Type | Squarespace host | Fully qualified name | Target | Status |
| --- | --- | --- | --- | --- | --- |
| Staging frontend | A | `staging-hr` | `staging-hr.nevgiuai.com` | `141.94.33.197` | Configured |
| Staging API | A | `staging-api.hr` | `staging-api.hr.nevgiuai.com` | `141.94.33.197` | Configured |
| Production frontend | A | `hr` | `hr.nevgiuai.com` | `141.94.94.198` | Pending |
| Production API | A | `api.hr` | `api.hr.nevgiuai.com` | `141.94.94.198` | Pending |

Use TTL `300` during initial setup and troubleshooting. Increase it to `3600` after the environment is stable if faster DNS changes are no longer required.

Do not change existing Google Workspace MX or TXT records when adding application hosts.

## Account model

Each VPS uses two Linux accounts:

| User | Purpose |
| --- | --- |
| `ubuntu` | Human administration, package updates, firewall, SSH policy, and recovery |
| `deploy` | Application deployment through Docker and GitHub Actions |

Direct root SSH login and password-based SSH login are disabled after key access is verified.

The `deploy` account is a member of the `docker` group. Docker group membership is effectively root-equivalent, so its SSH key must be treated as a privileged deployment credential. Staging and production require different key pairs.

## Secret-handling rules

- Store real runtime credentials only in `/opt/nevgiu/deploy/.env` on the corresponding VPS.
- Keep `deploy/.env.example` safe to commit by leaving secret values empty.
- Never copy real values into `.env.example`, Markdown, issue comments, workflow YAML, screenshots, or terminal transcripts.
- Never send private SSH keys, OpenAI API keys, PostgreSQL passwords, or recovery credentials through chat.
- Keep staging and production credentials separate.
- Store only deployment transport credentials in GitHub environments. The deployment workflow does not need the application OpenAI or PostgreSQL secrets.
- Revoke and rotate a credential immediately if it appears in a tracked file or unexpected output, even when GitHub Push Protection blocks the push.

## Backup selection

OVHcloud Standard automatic backup is included and retains one daily VPS backup for 24 hours.

Recommended policy:

- Staging: Standard backup is sufficient during initial development.
- Production: upgrade to Premium before storing real candidate data to retain seven rolling daily restore points.
- Both environments: add encrypted, application-aware PostgreSQL backups stored outside the VPS and preferably outside the same data centre/account.
- Before risky releases: take a manual snapshot after confirming the snapshot policy and cost.

Provider backups do not replace tested PostgreSQL restore procedures.

## Staging procedure

### 1. Update Ubuntu

Connect with the OVH-provided administrative user:

```bash
ssh ubuntu@141.94.33.197
```

Update and reboot:

```bash
sudo apt update
sudo apt upgrade -y
sudo reboot
```

Wait approximately one minute and reconnect.

### 2. Create the deployment user

```bash
sudo adduser --disabled-password --gecos "" deploy
sudo mkdir -p /home/deploy/.ssh
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
```

Generate a dedicated staging key on the administrator workstation. On Windows PowerShell:

```powershell
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\nevgiu_hr_staging"
```

The CI/CD key must not reuse a personal SSH key. A passphrase-free key is required for unattended GitHub Actions unless a separate supported key-agent mechanism is introduced.

Copy only the public key:

```powershell
scp "$env:USERPROFILE\.ssh\nevgiu_hr_staging.pub" ubuntu@141.94.33.197:/tmp/deploy-key.pub
```

Install it from the administrative session:

```bash
sudo cp /tmp/deploy-key.pub /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
sudo rm /tmp/deploy-key.pub
```

Test from a second terminal before closing the administrative session:

```powershell
ssh -i "$env:USERPROFILE\.ssh\nevgiu_hr_staging" deploy@141.94.33.197
```

### 3. Install Docker

Run as `ubuntu`:

```bash
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker deploy
```

Disconnect and reconnect as `deploy` so the new group membership applies, then verify without `sudo`:

```bash
docker version
docker compose version
```

### 4. Enable administrative key authentication

Before disabling password authentication, install and verify an SSH key for `ubuntu`. The staging setup reused the staging key's public half for initial administration:

```bash
sudo mkdir -p /home/ubuntu/.ssh
sudo cp /home/deploy/.ssh/authorized_keys /home/ubuntu/.ssh/authorized_keys
sudo chown -R ubuntu:ubuntu /home/ubuntu/.ssh
sudo chmod 700 /home/ubuntu/.ssh
sudo chmod 600 /home/ubuntu/.ssh/authorized_keys
```

Verify key-based administrative access from a new terminal:

```powershell
ssh -i "$env:USERPROFILE\.ssh\nevgiu_hr_staging" ubuntu@141.94.33.197
```

For a mature production environment, consider separate human-administration and deployment keys instead of reusing one public key across both accounts.

### 5. Configure the firewall

Run as `ubuntu`:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable
sudo ufw status
```

`OpenSSH` represents TCP port 22. Keep an existing SSH session open and verify a new connection succeeds before continuing.

Only ports 22, 80, and 443 are public. PostgreSQL, backend port 8080, and frontend port 80 inside Docker are not published directly by the deployment Compose file.

### 6. Disable password and root SSH login

Run as `ubuntu` only after both `ubuntu` and `deploy` key access have been tested:

```bash
printf '%s\n' \
  'PasswordAuthentication no' \
  'KbdInteractiveAuthentication no' \
  'PermitRootLogin no' \
  | sudo tee /etc/ssh/sshd_config.d/99-nevgiu-hardening.conf

sudo sshd -t
sudo systemctl reload ssh
```

Keep the current session open and test a new key-based connection. If `sshd -t` fails, do not reload SSH; correct the reported configuration error first.

### 7. Prepare the application directory

Run as `ubuntu`:

```bash
sudo hostnamectl set-hostname nevgiu-hr-staging
sudo mkdir -p /opt/nevgiu/backups /opt/nevgiu/data/postgres
sudo chown -R deploy:deploy /opt/nevgiu
sudo chmod 755 /opt/nevgiu
```

Verify:

```bash
hostname
ls -ld /opt/nevgiu
```

### 8. Configure Squarespace DNS

In Squarespace:

1. Open the Squarespace account's domain management.
2. Select `nevgiuai.com`.
3. Open **DNS** or **DNS Settings**.
4. Under custom records, create the two staging A records listed in the DNS inventory.
5. Use TTL `300` during setup.
6. Do not modify Google Workspace email records.

Verify from Windows PowerShell:

```powershell
Resolve-DnsName staging-hr.nevgiuai.com
Resolve-DnsName staging-api.hr.nevgiuai.com
```

Both must resolve to `141.94.33.197` before Caddy requests certificates.

### 9. Copy the deployment bundle

From the repository workstation:

```powershell
scp -i "$env:USERPROFILE\.ssh\nevgiu_hr_staging" -r "C:\GitHub\nevgiu-hr-ai\deploy" deploy@141.94.33.197:/opt/nevgiu/
```

Verify on the VPS:

```bash
ls -la /opt/nevgiu/deploy
```

Expected tracked files include:

```text
.env.example
Caddyfile
compose.yml
deploy.sh
README.md
```

If `deploy` receives permission errors:

```bash
sudo chown -R deploy:deploy /opt/nevgiu
sudo chmod 755 /opt/nevgiu /opt/nevgiu/deploy
```

Run those repair commands as `ubuntu`, then return to `deploy` for application operations.

### 10. Create the private environment file

Run as `deploy`:

```bash
cd /opt/nevgiu/deploy
cp .env.example .env
openssl rand -hex 32
nano .env
chmod 600 .env
```

Set real staging values only in `.env`:

```env
ACME_EMAIL=<operations email>
POSTGRES_PASSWORD=<generated staging password>
OPENAI_API_KEY=<staging OpenAI key>
```

Confirm these environment-specific values remain:

```env
FRONTEND_HOST=staging-hr.nevgiuai.com
API_HOST=staging-api.hr.nevgiuai.com
FRONTEND_URL=https://staging-hr.nevgiuai.com
INITIAL_IMPORT_ENABLED=true
```

Do not display or transmit the completed file.

### 11. Perform the first manual image build

This step bootstraps staging before GHCR automation is enabled. Clone the repository as `deploy`:

```bash
cd /opt/nevgiu
git clone https://github.com/FanJups/nevgiu-hr-ai.git source
```

Build local images:

```bash
docker build -t hr-ai-backend:staging-manual /opt/nevgiu/source/backend

docker build \
  --build-arg CONFIGURATION=production \
  -t hr-ai-frontend:staging-manual \
  /opt/nevgiu/source/frontend
```

The frontend image uses runtime `API_URL` injection, so the same production-built image format is valid for staging and production.

### 12. Start the stack

Run as `deploy`:

```bash
cd /opt/nevgiu/deploy
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d
```

Wait approximately one minute:

```bash
docker compose --env-file .env ps
```

Expected services:

- `db`: healthy.
- `backend`: healthy.
- `frontend`: healthy.
- `caddy`: running.

### 13. Verify HTTPS

```bash
curl --fail https://staging-api.hr.nevgiuai.com/actuator/health
curl --fail --head https://staging-hr.nevgiuai.com/
```

Expected backend response:

```json
{"status":"UP"}
```

Caddy obtains and renews certificates automatically after DNS resolves and ports 80/443 are reachable.

### 14. Run the functional smoke test

In the staging frontend:

1. Generate a job offer.
2. Enter edit mode and approve it.
3. Confirm it appears in approved jobs.
4. Import a PDF, ZIP, or the staging built-in CV dataset.
5. Select an imported candidate and approved job.
6. Explicitly evaluate the candidate.
7. Confirm the overall score, all eight metrics, and explanation appear.
8. Restart the Compose services and confirm persisted jobs, candidates, documents, and evaluations remain.

## Troubleshooting learned during staging

### Frontend reported unhealthy while Nginx was running

Symptom:

```text
wget: can't connect to remote host: Connection refused
```

Cause: `localhost` resolved to IPv6 while Nginx listened on IPv4.

Resolution: the tracked Compose health check uses:

```text
http://127.0.0.1/
```

Inspect health and logs with:

```bash
docker inspect nevgiu-hr-ai-frontend-1 --format '{{range .State.Health.Log}}{{.Output}}{{end}}'
docker logs nevgiu-hr-ai-frontend-1 --tail 50
```

### Caddy did not start

Caddy depends on healthy frontend and backend containers. Correct unhealthy services first, then rerun:

```bash
docker compose --env-file .env up -d
```

### `.env` permission denied

Run as `ubuntu`:

```bash
sudo chown deploy:deploy /opt/nevgiu/deploy/.env
sudo chmod 600 /opt/nevgiu/deploy/.env
```

Verify as `deploy` without printing its contents:

```bash
test -r /opt/nevgiu/deploy/.env && echo "Environment file is readable"
```

### `scp` requested the deploy password

Run PowerShell-style `scp` commands from the local Windows PowerShell prompt, not from inside an SSH session. Include the correct private-key path with `-i`.

## Credential rotation

### Rotate the OpenAI API key

1. Revoke the old key at the provider.
2. Create a new environment-specific key.
3. Edit `/opt/nevgiu/deploy/.env` as `deploy` and replace only `OPENAI_API_KEY`.
4. Recreate the backend:

```bash
cd /opt/nevgiu/deploy
docker compose --env-file .env up -d --force-recreate backend
```

5. Verify backend health and an AI operation.

### Rotate the PostgreSQL password

Generate a new password without sharing it:

```bash
openssl rand -hex 32
```

Open PostgreSQL as `deploy`:

```bash
docker exec -it nevgiu-hr-ai-db-1 psql -U hr_user -d hr_ai
```

At the PostgreSQL prompt:

```text
\password hr_user
\q
```

Update `POSTGRES_PASSWORD` in `/opt/nevgiu/deploy/.env` with the same value, then recreate the affected containers:

```bash
cd /opt/nevgiu/deploy
docker compose --env-file .env up -d --force-recreate db backend
docker compose --env-file .env ps
```

Confirm both services are healthy.

## Production checklist

Repeat the staging procedure with these changes:

- [ ] Use public IP `141.94.94.198`.
- [ ] Set hostname `nevgiu-hr-production`.
- [ ] Generate a separate production deployment SSH key.
- [ ] Do not copy staging private keys or credentials.
- [ ] Create Squarespace A records `hr` and `api.hr` targeting the production IP.
- [ ] Verify `hr.nevgiuai.com` and `api.hr.nevgiuai.com` resolve correctly.
- [ ] Use production-only OpenAI and PostgreSQL credentials.
- [ ] Set `FRONTEND_HOST=hr.nevgiuai.com`.
- [ ] Set `API_HOST=api.hr.nevgiuai.com`.
- [ ] Set `FRONTEND_URL=https://hr.nevgiuai.com`.
- [ ] Set `INITIAL_IMPORT_ENABLED=false`.
- [ ] Upgrade the OVHcloud production backup to Premium before real candidate data.
- [ ] Configure encrypted off-server PostgreSQL backups and test restoration.
- [ ] Configure the GitHub `production` environment with separate variables and secrets.
- [ ] Require production deployment approval where supported.
- [ ] Do not perform the first production deployment until a release commit has a successful staging-validation image alias.
- [ ] Run production-safe HTTPS and read-only smoke tests after deployment.

## Completion criteria

A VPS is ready for GitHub Actions only when:

- Key-only SSH works for `deploy`.
- Administrative recovery access works for `ubuntu`.
- Password and root SSH login are disabled.
- UFW exposes only SSH, HTTP, and HTTPS.
- Docker and Compose work for `deploy` without `sudo`.
- `/opt/nevgiu/deploy/.env` exists, belongs to `deploy`, has mode 600, and contains only that environment's credentials.
- PostgreSQL and backend ports are not public.
- Both DNS names resolve to the correct VPS.
- Caddy serves valid HTTPS.
- Database, backend, and frontend are healthy; Caddy is running.
- Functional smoke tests pass and persistence survives a container restart.
- GitHub environment host, SSH, known-host, and GHCR pull credentials are configured separately for that environment.
