# Phase 6 — local Docker Compose

Brings up the same 5-service topology (nginx, backend, ai-service, postgres, redis) that runs on
the OCI VM in production — only `.env` values and the nginx cert source differ. See
`CLAUDE.md`'s Phase 6 plan for the full architecture.

## First-time setup

1. `cp .env.example .env` and fill in real values (`POSTGRES_PASSWORD`, `GEMINI_API_KEY`,
   `COINGECKO_API_KEY`). Never commit `.env`.
2. Place a real Firebase service-account JSON at the repo root as `firebase-service-account.json`
   (never committed — gitignored). Without this, the backend can't verify Firebase ID tokens and
   will fail to start.
3. `bash docker/generate-local-certs.sh` — generates a self-signed TLS cert for local nginx.
   Your browser/emulator will show a certificate-untrusted warning; that's expected for a
   self-signed dev cert (the real OCI VM gets a real Let's Encrypt cert instead — see
   `docker/nginx/nginx.conf.template`'s comment and CLAUDE.md's Phase 6 plan §2b/§5).
4. Populate `ai-service/artifacts/` — either run `ai-service/training/train_xgboost.py` and
   `ai-service/training/train_momentum_tflite.py` locally, or run
   `ai-service/scripts/fetch_model_artifacts.py` against a previously published version (needs
   OCI Object Storage credentials — see that script's docstring). The ai-service Docker build
   only ever copies whatever is already in that directory; it never trains anything itself.

## Bring the stack up

```
docker compose up -d --build
```

Postgres/Redis have healthchecks; backend waits on both plus ai-service; nginx waits on backend.
Flyway migrations run automatically on backend startup (`spring.flyway.enabled: true`) — no
separate manual migration step.

Verify:
- `curl -k https://localhost/actuator/health` (via nginx, `-k` because of the self-signed cert)
- `docker compose logs -f backend` / `ai-service` — both emit structured JSON log lines
  (`LOG_FORMAT=json` / `SPRING_PROFILES_ACTIVE=docker`, see each service's logging config).

## Building for the OCI VM specifically

The OCI Ampere A1 VM is ARM64 — build (or have CI build, see `.github/workflows/deploy.yml`)
with:

```
docker buildx build --platform linux/arm64 -f docker/backend.Dockerfile -t backend:arm64 .
docker buildx build --platform linux/arm64 -f docker/ai-service.Dockerfile -t ai-service:arm64 .
```

Every base image and every `ai-service/requirements.txt` pin was verified to have an aarch64
build/wheel before this Dockerfile set was written (CLAUDE.md's Phase 6 plan §7 step 6) — this
`docker buildx build --platform linux/arm64` command is the actual acceptance check that
verification promised; it has not been run in the environment this was authored in (no Docker
daemon available there) and should be run for real before first deploying to the VM.
