#!/usr/bin/env bash
# Scheduled Postgres backup (agent.md §27/§31) — Hetzner has no managed
# Postgres, so this is the entire backup story: `pg_dump` inside the running
# `postgres` container, gzip, prune anything older than
# BACKUP_RETENTION_DAYS locally, and upload off-VM via rclone if
# RCLONE_REMOTE is set. Run via cron/systemd (see infrastructure/systemd/)
# on the Hetzner VM, or manually to test.
#
# Usage: BACKUP_DIR=/opt/pizza-configurator/backups ./backup-postgres.sh
# (all env vars below have sane defaults for local/manual testing against
# compose.yaml; on the VM, compose.prod.yaml's postgres service is the same
# service name so no changes are needed beyond pointing COMPOSE_FILE at it.)
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-compose.yaml}"
DB_NAME="${DB_NAME:-pizzaconfigurator}"
DB_USER="${DB_USER:-pizzaconfigurator}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILENAME="pizzaconfigurator-${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "### Dumping ${DB_NAME} from the running postgres container ..."
docker compose -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$DB_USER" --format=plain --no-owner --no-privileges "$DB_NAME" \
  | gzip > "${BACKUP_DIR}/${FILENAME}"

SIZE=$(du -h "${BACKUP_DIR}/${FILENAME}" | cut -f1)
echo "### Wrote ${BACKUP_DIR}/${FILENAME} (${SIZE})"

if [ -n "${RCLONE_REMOTE:-}" ]; then
  echo "### Uploading to ${RCLONE_REMOTE} ..."
  rclone copy "${BACKUP_DIR}/${FILENAME}" "${RCLONE_REMOTE}"
else
  echo "### RCLONE_REMOTE not set — skipping off-VM upload (local-only backup)."
  echo "### Set RCLONE_REMOTE (e.g. a Hetzner Storage Box configured as an rclone SFTP remote,"
  echo "### per https://docs.hetzner.com/storage/storage-box/backup-space-rclone/) for a real deployment."
fi

echo "### Pruning local backups older than ${BACKUP_RETENTION_DAYS} days ..."
find "$BACKUP_DIR" -name "pizzaconfigurator-*.sql.gz" -mtime "+${BACKUP_RETENTION_DAYS}" -print -delete

echo "### Done."
