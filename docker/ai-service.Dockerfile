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
# xgboost's PyPI wheel metadata declares `nvidia-nccl-cu12; platform_system == 'Linux' and
# platform_machine != 'aarch64'` — i.e. it's pulled in ONLY on x86_64 Linux, never on arm64
# (confirmed via the wheel's own METADATA, not guessed), which is exactly why this image was
# ~635MB on amd64 vs ~160MB on arm64 before this fix: a single package, ~450MB unpacked, that
# exists purely for multi-GPU NCCL collective-ops support xgboost doesn't use for CPU-only
# inference (this service never touches a GPU — see the Tech Stack table's on-device/AI-service
# rows). Deliberately NOT using `pip install --no-deps xgboost` here: that would make xgboost's
# real runtime deps (numpy, scipy) look like they're satisfied only by coincidence via the
# scikit-learn pin below, so a future change to that pin could silently break xgboost's import
# with no visible connection back to this line. Instead: install everything normally (full
# resolution, so numpy/scipy are genuinely resolved as xgboost's own declared deps, same as
# scikit-learn's), then explicitly uninstall the unwanted nvidia-* packages in this same layer
# (so the removed files never end up committed to an image layer in the first place) — the
# exclusion's cause and effect both stay readable in this file instead of being implicit.
RUN pip install --no-cache-dir -r requirements.txt && \
    pip uninstall -y $(pip list --format=freeze | grep -i '^nvidia-' | cut -d= -f1) 2>/dev/null || true

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
