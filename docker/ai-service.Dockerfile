# Phase 6 — python:3.13-slim is Docker Hub's official multi-arch image (linux/arm64 included);
# every runtime dependency pin in ai-service/requirements.txt was verified to have an aarch64
# wheel at its exact pinned version before this was written (CLAUDE.md's Phase 6 plan §7 step 6).
#
# Build context is the repo root (see docker-compose.yml). artifacts/ is committed directly to
# the repo (Phase 6 CI/CD fix — previously fetched from OCI Object Storage at build time; removed
# when OCI became blocked on payment verification, see CLAUDE.md's Phase 6 STATUS block) — this
# Dockerfile only ever COPYs the already-committed model, it never trains one. Never add a
# `RUN python training/train_*.py` step here.
FROM python:3.13-slim
WORKDIR /app

COPY ai-service/requirements.txt .
# requirements.txt platform-splits xgboost: plain `xgboost`'s x86_64 wheel bundles GPU/CUDA
# device code (and a marker-gated nvidia-nccl-cu12 dependency) that this CPU-only service never
# uses, inflating amd64 alone to ~635MB vs arm64's ~160MB. `xgboost-cpu` is the same project's
# official CPU-only x86_64 build (~5MB) — same `xgboost` import name, no GPU code, no nvidia-*
# dependency at all, so there's nothing left here to exclude/uninstall after install.
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
