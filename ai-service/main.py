"""FastAPI entrypoint. Loading the trained model happens lazily on first request
inside models/xgboost_model.py (module-level cache, loaded once) rather than
here, but importing api.prediction_router at startup is what makes the model
file's existence checkable via /health without waiting for the first /predict
call.
"""
from __future__ import annotations

from fastapi import FastAPI

from api.prediction_router import router as prediction_router
from models import xgboost_model

app = FastAPI(title="AI Crypto Predictor - Prediction Service")
app.include_router(prediction_router)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "model_ready": xgboost_model.is_ready()}
