"""FastAPI entrypoint. The trained XGBoost artifact is loaded once during
application startup, via the `lifespan` async context manager below (FastAPI's
current, non-deprecated startup/shutdown mechanism -- `@app.on_event("startup")`
is legacy as of FastAPI 0.93+, and this project's installed version, 0.115.6,
supports lifespan) -- not lazily on the first /predict request. models/
xgboost_model.py's load() populates that module's single `_artifact` cache;
predict() reuses the same cache on every subsequent call, so a request is
never what triggers a disk read. If the artifact is missing or corrupt,
load() raises and is deliberately left unhandled here, so FastAPI/uvicorn
fails to start rather than serving traffic with no model available (Phase 5
Definition of Done: "FastAPI loads its model artifact once at startup, not
per-request").
"""
from __future__ import annotations

import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request

from api.prediction_router import router as prediction_router
from logging_config import configure_logging
from models import xgboost_model

configure_logging()
logger = logging.getLogger("ai-service.startup")
access_logger = logging.getLogger("ai-service.access")


@asynccontextmanager
async def lifespan(app: FastAPI):
    xgboost_model.load()
    logger.info("XGBoost model loaded successfully at startup from %s", xgboost_model.ARTIFACT_PATH)
    yield


app = FastAPI(title="AI Crypto Predictor - Prediction Service", lifespan=lifespan)
app.include_router(prediction_router)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Phase 6: one structured log line per request, same fields (method/path/status/durationMs)
    as the backend's RequestLoggingFilter — kept as simple ASGI middleware rather than a FastAPI
    dependency so it wraps every route (including /health) with no per-router opt-in needed."""
    start = time.monotonic()
    response = await call_next(request)
    duration_ms = int((time.monotonic() - start) * 1000)
    access_logger.info(
        "http_request",
        extra={
            "method": request.method,
            "path": request.url.path,
            "status": response.status_code,
            "durationMs": duration_ms,
        },
    )
    return response


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "model_ready": xgboost_model.is_ready()}
