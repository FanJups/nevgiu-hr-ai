#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <backend-image> <frontend-image>" >&2
  exit 2
fi

backend_image=$1
frontend_image=$2
deploy_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$deploy_dir"

if [ ! -r .env ]; then
  echo "Missing readable $deploy_dir/.env" >&2
  exit 1
fi

compose() {
  docker compose --env-file .env --env-file .images.env "$@"
}

service_state() {
  container_id=$(compose ps -q "$1")
  if [ -z "$container_id" ]; then
    echo "missing"
    return
  fi

  docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id"
}

wait_for_stack() {
  attempts=36
  while [ "$attempts" -gt 0 ]; do
    db_state=$(service_state db)
    backend_state=$(service_state backend)
    frontend_state=$(service_state frontend)
    caddy_state=$(service_state caddy)

    if [ "$db_state" = "healthy" ] \
      && [ "$backend_state" = "healthy" ] \
      && [ "$frontend_state" = "healthy" ] \
      && [ "$caddy_state" = "running" ]; then
      return 0
    fi

    attempts=$((attempts - 1))
    sleep 5
  done

  return 1
}

previous_backend=$(docker inspect --format '{{.Config.Image}}' nevgiu-hr-ai-backend-1 2>/dev/null || true)
previous_frontend=$(docker inspect --format '{{.Config.Image}}' nevgiu-hr-ai-frontend-1 2>/dev/null || true)

new_images=.images.env.new
printf 'BACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\n' "$backend_image" "$frontend_image" > "$new_images"
chmod 600 "$new_images"
mv "$new_images" .images.env

compose config --quiet
compose pull
compose up -d --remove-orphans

if wait_for_stack; then
  compose ps
  exit 0
fi

echo "Deployment failed health validation; attempting application rollback" >&2
compose logs --tail 100 >&2 || true

if [ -n "$previous_backend" ] && [ -n "$previous_frontend" ]; then
  printf 'BACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\n' "$previous_backend" "$previous_frontend" > .images.env
  chmod 600 .images.env
  compose up -d --remove-orphans

  if wait_for_stack; then
    echo "Rollback completed" >&2
  else
    echo "Rollback also failed; manual intervention is required" >&2
  fi
else
  echo "No previous application images were available for rollback" >&2
fi

exit 1
