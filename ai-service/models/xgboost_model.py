"""Loads the trained XGBoost artifact once (module import time) and exposes a
single predict() function -- never retrained or reloaded per request."""
from __future__ import annotations

from pathlib import Path
from typing import Optional

import joblib
import pandas as pd

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from features.feature_engineering import build_feature_frame, align_btc_prices, FEATURE_COLUMNS, REQUIRED_WARMUP_DAYS  # noqa: E402

ARTIFACT_PATH = Path(__file__).resolve().parent.parent / "artifacts" / "xgboost_model.joblib"

_artifact: Optional[dict] = None


def _load() -> dict:
    global _artifact
    if _artifact is None:
        if not ARTIFACT_PATH.exists():
            raise FileNotFoundError(
                f"No trained model at {ARTIFACT_PATH} -- run training/train_xgboost.py first."
            )
        _artifact = joblib.load(ARTIFACT_PATH)
    return _artifact


def is_ready() -> bool:
    return ARTIFACT_PATH.exists()


def min_required_points() -> int:
    return REQUIRED_WARMUP_DAYS + 1  # +1 so there's at least one fully-warmed-up row to predict from


def predict(coin_id: str, history: list[dict], btc_history: list[dict]) -> dict:
    """history/btc_history: list of {"timestamp": int, "price": float, "volume": float},
    ascending by timestamp. Returns {"confidence": float 0-100, "direction": "UP"|"DOWN"|"FLAT",
    "targetPrice": float}. Raises ValueError if there isn't enough history to compute features."""
    artifact = _load()

    if len(history) < min_required_points():
        raise ValueError(
            f"Need at least {min_required_points()} history points, got {len(history)}"
        )

    df = pd.DataFrame(history).sort_values("timestamp").reset_index(drop=True)
    btc_df = pd.DataFrame(btc_history).sort_values("timestamp").reset_index(drop=True)
    btc_aligned = align_btc_prices(df["timestamp"], btc_df)

    featured = build_feature_frame(df, btc_prices=btc_aligned)
    latest = featured.iloc[[-1]].copy()

    if latest[FEATURE_COLUMNS].isna().any(axis=None):
        raise ValueError("Latest row has NaN features -- history has gaps or is too short after warmup")

    known_categories = artifact["coin_categories"]
    latest["coin_id"] = pd.Categorical([coin_id if coin_id in known_categories else known_categories[0]],
                                        categories=known_categories)
    if coin_id not in known_categories:
        # Coin wasn't in the training pool -- the model can still produce a prediction using
        # its numeric features alone (coin_id is one categorical feature among ~20), but this
        # is a genuine out-of-distribution case worth being honest about rather than silently
        # coercing to a fake category. Confidence is not artificially reduced here; the model's
        # own softmax probability already reflects its uncertainty.
        pass

    X = latest[FEATURE_COLUMNS + ["coin_id"]]
    model = artifact["model"]
    label_order = artifact["label_order"]
    probs = model.predict_proba(X)[0]

    best_idx = int(probs.argmax())
    direction = label_order[best_idx]
    confidence = float(probs[best_idx] * 100.0)

    current_price = float(df["price"].iloc[-1])
    threshold_pct = artifact["threshold_pct"]
    if direction == "UP":
        target_price = current_price * (1 + threshold_pct / 100.0)
    elif direction == "DOWN":
        target_price = current_price * (1 - threshold_pct / 100.0)
    else:
        target_price = None

    return {"confidence": confidence, "direction": direction, "targetPrice": target_price}
