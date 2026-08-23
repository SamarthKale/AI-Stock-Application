# Phase 6 — python:3.13-slim is Docker Hub's official multi-arch image (linux/arm64 included);
# every runtime dependency pin in ai-service/requirements.txt was verified to have an aarch64
# wheel at its exact pinned version before this was written (CLAUDE.md's Phase 6 plan §7 step 6).
#
# Build context is the repo root (see docker-compose.yml). artifacts/ is expected to already be
# populated (by scripts/fetch_model_artifacts.py in CI, or by local training for dev) *before*
# `docker build` runs — this Dockerfile only ever COPYs an already-published model, it never
# trains one. Never add a `RUN python training/train_*.py` step here.
FROM python:3.13-slim
WORKDIR /app

COPY ai-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ai-service/main.py ai-service/logging_config.py ./
COPY ai-service/api ./api
COPY ai-service/models ./models
COPY ai-service/features ./features
COPY ai-service/artifacts ./artifacts

RUN useradd --system --uid 10001 --create-home appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000
# No public route (see CLAUDE.md's Phase 6 architecture) — reachable only from the backend
# container over the Compose-internal network; nginx never proxies to this service directly.
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
