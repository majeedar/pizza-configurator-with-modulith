#!/usr/bin/env bash
# Restores a backup produced by backup-postgres.sh — this is the "tested
# restore procedure" agent.md §27/§31 requires alongside the backup itself
# (a backup nobody has ever restored from is not a backup). Restores into
# the *running* postgres container, replacing its current data — meant for
# disaster recovery or a periodic restore drill against a throwaway
# database, not routine use.
#
# Usage: ./restore-postgres.sh ./backups/pizzaconfigurator-20260101T000000Z.sql.gz
set -euo pipefail

BACKUP_FILE="${1:?Usage: restore-postgres.sh <path-to-backup.sql.gz>}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.yaml}"
DB_NAME="${DB_NAME:-pizzaconfigurator}"
DB_USER="${DB_USER:-pizzaconfigurator}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "No such backup file: $BACKUP_FILE" >&2
  exit 1
fi

echo "### This will DROP and recreate '${DB_NAME}' on the '${COMPOSE_FILE}' postgres service."
read -r -p "Type the database name to confirm (${DB_NAME}): " CONFIRM
if [ "$CONFIRM" != "$DB_NAME" ]; then
  echo "Confirmation did not match — aborting." >&2
  exit 1
fi

echo "### Terminating other connections and recreating ${DB_NAME} ..."
docker compose -f "$COMPOSE_FILE" exec -T postgres psql -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 <<SQL
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS ${DB_NAME};
CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};
SQL

echo "### Restoring ${BACKUP_FILE} ..."
gunzip -c "$BACKUP_FILE" | docker compose -f "$COMPOSE_FILE" exec -T postgres \
  psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1

echo "### Restore complete. Restart the backend so Flyway re-checks schema state:"
echo "###   docker compose -f ${COMPOSE_FILE} restart backend"
