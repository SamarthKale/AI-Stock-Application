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
from contextlib import asynccontextmanager

from fastapi import FastAPI

from api.prediction_router import router as prediction_router
from models import xgboost_model

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai-service.startup")


@asynccontextmanager
async def lifespan(app: FastAPI):
    xgboost_model.load()
    logger.info("XGBoost model loaded successfully at startup from %s", xgboost_model.ARTIFACT_PATH)
    yield


app = FastAPI(title="AI Crypto Predictor - Prediction Service", lifespan=lifespan)
app.include_router(prediction_router)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "model_ready": xgboost_model.is_ready()}
