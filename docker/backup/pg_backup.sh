#!/usr/bin/env bash
# Phase 6 — daily Postgres backup, run on the OCI VM host via cron (see crontab.example in this
# directory), not inside a container (so it survives `docker compose down`/image updates).
# Retention (§6 of CLAUDE.md's Phase 6 plan) is enforced by an Object Storage lifecycle policy
# (infra/main.tf's oci_objectstorage_object_lifecycle_policy), not by this script — a dump left
# behind by a broken run still gets cleaned up on schedule either way.
#
# Requires: `docker` (to reach the postgres container), `aws` CLI (works against OCI's
# S3-compatible Object Storage endpoint via --endpoint-url — no separate OCI CLI needed for this
# one operation), and the env vars below set in the same .env this VM's docker-compose.yml reads.
set -euo pipefail

: "${POSTGRES_USER:?POSTGRES_USER must be set}"
: "${POSTGRES_DB:?POSTGRES_DB must be set}"
: "${OCI_OBJECT_STORAGE_ENDPOINT:?OCI_OBJECT_STORAGE_ENDPOINT must be set}"
: "${OCI_OBJECT_STORAGE_BACKUPS_BUCKET:?OCI_OBJECT_STORAGE_BACKUPS_BUCKET must be set}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DUMP_FILE="/tmp/stockpredictor_${TIMESTAMP}.sql.gz"

echo "[pg_backup] Dumping ${POSTGRES_DB} -> ${DUMP_FILE}"
docker compose -f /opt/ai-crypto-predictor/docker-compose.yml exec -T postgres \
    pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "${DUMP_FILE}"

echo "[pg_backup] Uploading to Object Storage"
AWS_ACCESS_KEY_ID="${OCI_ACCESS_KEY_ID}" AWS_SECRET_ACCESS_KEY="${OCI_SECRET_ACCESS_KEY}" \
    aws s3 cp "${DUMP_FILE}" \
    "s3://${OCI_OBJECT_STORAGE_BACKUPS_BUCKET}/postgres/${TIMESTAMP}.sql.gz" \
    --endpoint-url "${OCI_OBJECT_STORAGE_ENDPOINT}"

rm -f "${DUMP_FILE}"
echo "[pg_backup] Done: ${TIMESTAMP}.sql.gz"
