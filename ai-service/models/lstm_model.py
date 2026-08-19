"""Stub/interface only -- Phase 5 plan section 6: prove the XGBoost path works
end-to-end before building a second model type. Not trained, not wired into
the API. Matches xgboost_model.py's function signature so a future
implementation is a drop-in swap for prediction_router.py, not a rewrite.
"""
from __future__ import annotations


def is_ready() -> bool:
    return False


def predict(coin_id: str, history: list[dict], btc_history: list[dict]) -> dict:
    raise NotImplementedError(
        "LSTM/GRU is a Phase 5 stub only, per the Phase 5 plan (section 6) -- "
        "not trained or wired into the API this phase."
    )
