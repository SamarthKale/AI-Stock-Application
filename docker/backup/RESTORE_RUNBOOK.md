# Phase 6 — restore runbook

Recovery procedure if the OCI VM itself is lost (no managed-DB point-in-time recovery exists in
this architecture — see CLAUDE.md's Phase 6 plan §6). This is a manual procedure by design; the
acceptance bar is that it's been **actually rehearsed**, not just written down.

## What's been verified so far (this environment, no live OCI infra available)

The core Postgres dump/restore mechanics were rehearsed for real against a local Postgres
instance: `pg_dump | gzip` of a fully Flyway-migrated database, restored into a fresh scratch
database, and every table (`users`, `watchlist`, `portfolio_holdings`, `prediction_cache`,
`alert_cooldowns`, `flyway_schema_history`) came back identically. That confirms the dump/restore
commands `pg_backup.sh` and the steps below use are correct.

**Not yet rehearsed** (needs a real OCI VM + Object Storage, neither available in the environment
this runbook was written in): the Object Storage upload/download leg, and a full end-to-end drill
on an actual second VM. Do that once for real before relying on this in production — spin up a
second temporary Always Free... no, a *second concurrent* A1 instance isn't possible under the
post-2026 Always Free budget (see the plan's Context section), so rehearse this by temporarily
repurposing a non-production window on the one VM, or a short-lived Pay-As-You-Go instance you
delete immediately after (outside the ₹0 Always Free guarantee for that brief window — confirm
you're comfortable with that before doing it that way).

## Recovery steps

1. **Re-provision the VM** — `cd infra && terraform apply` (or redo the manual checklist in
   CLAUDE.md's Phase 6 plan §2c) against the same or a fresh Always Free Ampere A1 instance.
2. **Restore secrets** — copy `.env` and `firebase-service-account.json` back onto the new VM from
   wherever you keep them outside this repo (a password manager / secrets vault — never only on
   the VM itself, or losing the VM loses the secrets too).
3. **Bring up the stack minus data**:
   ```
   docker compose up -d postgres redis
   ```
4. **Download the latest backup**:
   ```
   AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... \
     aws s3 cp s3://<backups-bucket>/postgres/<timestamp>.sql.gz ./restore.sql.gz \
     --endpoint-url "$OCI_OBJECT_STORAGE_ENDPOINT"
   ```
5. **Restore it**:
   ```
   gunzip -c restore.sql.gz | docker compose exec -T postgres psql -U "$POSTGRES_USER" "$POSTGRES_DB"
   ```
6. **Bring up the rest of the stack**:
   ```
   docker compose up -d
   ```
   Flyway runs its own startup check against the restored schema (`ddl-auto: validate`) — a
   successful backend startup is itself a correctness signal for the restore.
7. **Re-issue the TLS cert** (a fresh VM has no certs yet) — run certbot against the (same,
   because the Reserved IP survives VM loss only if you reserved it separately and re-attach it;
   otherwise it's a new IP and a new sslip.io hostname) hostname, per CLAUDE.md's Phase 6 plan §2b.
8. **Point DNS/Android at the (possibly new) hostname** if the IP changed, and smoke-test the
   full flow: login, watchlist, a prediction request, the chatbot.
