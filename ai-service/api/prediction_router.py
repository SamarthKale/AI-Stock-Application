from __future__ import annotations

import time

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from models import xgboost_model

router = APIRouter()

HORIZON = "24h"  # Phase 5 plan section 1 -- single horizon in v1


class PricePoint(BaseModel):
    timestamp: int
    price: float
    volume: float = 0.0


class PredictionRequest(BaseModel):
    coinId: str
    # Target coin's own history, ascending by timestamp -- Android already has this
    # cached via CoinRepository (Phase 5 plan section 8: Android sends its cached
    # history, no backend CoinGecko client).
    history: list[PricePoint]
    # Bitcoin's price history over the same period, required for the BTC-relative
    # features (section 4) -- when coinId == "bitcoin", send the same series as
    # `history` for both fields.
    btcHistory: list[PricePoint]


class PredictionResponse(BaseModel):
    coinId: str
    confidence: float = Field(ge=0, le=100)
    direction: str
    targetPrice: float | None
    horizon: str
    generatedAt: int


@router.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest) -> PredictionResponse:
    if not xgboost_model.is_ready():
        raise HTTPException(status_code=503, detail="Model not trained yet -- run training/train_xgboost.py")

    history = [p.model_dump() for p in request.history]
    btc_history = [p.model_dump() for p in request.btcHistory]

    if len(history) < xgboost_model.min_required_points():
        raise HTTPException(
            status_code=422,
            detail=f"Need at least {xgboost_model.min_required_points()} history points, got {len(history)}",
        )

    try:
        result = xgboost_model.predict(request.coinId, history, btc_history)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

    return PredictionResponse(
        coinId=request.coinId,
        confidence=result["confidence"],
        direction=result["direction"],
        targetPrice=result["targetPrice"],
        horizon=HORIZON,
        generatedAt=int(time.time() * 1000),
    )
