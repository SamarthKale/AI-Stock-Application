"""Verifies the Phase 5 startup-loading fix (re-audit defect): the trained
XGBoost artifact must be loaded once during FastAPI startup -- not lazily on
the first /predict request -- must stay cached across requests, and startup
must fail clearly if the artifact is missing/corrupt.

Requires a trained artifact at artifacts/xgboost_model.joblib (run
training/train_xgboost.py first) for the happy-path tests, same precondition
the pre-existing FastApiPredictionClientLiveVerification.java test documents
on the backend side. The missing-artifact test is self-contained (monkeypatches
ARTIFACT_PATH to a path that does not exist) and needs no trained model.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from models import xgboost_model

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
ARTIFACT_AVAILABLE = xgboost_model.ARTIFACT_PATH.exists()
BITCOIN_DATA_AVAILABLE = (DATA_DIR / "bitcoin.json").exists()

requires_trained_artifact = pytest.mark.skipif(
    not ARTIFACT_AVAILABLE,
    reason="No trained artifact at artifacts/xgboost_model.joblib -- run training/train_xgboost.py first",
)
requires_bitcoin_data = pytest.mark.skipif(
    not BITCOIN_DATA_AVAILABLE,
    reason="No raw data at ai-service/data/bitcoin.json -- run training/fetch_training_data.py first",
)


def _load_history(coin_id: str) -> list[dict]:
    """Real, previously-fetched daily history (same file training reads) -- gives
    a realistic 366-point series so feature engineering has enough warmup,
    rather than a hand-typed few points that would trip the 41-point minimum."""
    raw = json.loads((DATA_DIR / f"{coin_id}.json").read_text())
    prices = raw["prices"]
    volumes = raw["total_volumes"]
    return [
        {"timestamp": int(p[0]), "price": p[1], "volume": volumes[i][1] if i < len(volumes) else 0.0}
        for i, p in enumerate(prices)
    ]


@pytest.fixture(autouse=True)
def reset_model_cache():
    """xgboost_model._artifact is a module-level global -- reset it around every
    test so one test's loaded state can't leak into the next test's assertions
    about "was it loaded yet"."""
    xgboost_model._artifact = None
    yield
    xgboost_model._artifact = None


def test_model_not_ready_before_any_load():
    assert xgboost_model.is_ready() is False


@requires_trained_artifact
def test_load_is_idempotent_and_reads_disk_exactly_once(monkeypatch):
    calls = {"n": 0}
    real_joblib_load = xgboost_model.joblib.load

    def counting_load(path):
        calls["n"] += 1
        return real_joblib_load(path)

    monkeypatch.setattr(xgboost_model.joblib, "load", counting_load)

    first = xgboost_model.load()
    second = xgboost_model.load()

    assert calls["n"] == 1, "load() must not re-read the artifact file on a second call"
    assert first is second, "load() must return the same cached object, not a fresh copy"
    assert xgboost_model.is_ready() is True


@requires_trained_artifact
def test_lifespan_startup_loads_model_before_first_request(monkeypatch):
    """The core regression test for this fix: entering the FastAPI app's lifespan
    (what happens at real process startup) must populate the model cache before
    any request is served -- not on the first /predict call."""
    calls = {"n": 0}
    real_joblib_load = xgboost_model.joblib.load

    def counting_load(path):
        calls["n"] += 1
        return real_joblib_load(path)

    monkeypatch.setattr(xgboost_model.joblib, "load", counting_load)

    from main import app

    assert xgboost_model.is_ready() is False, "precondition: nothing loaded yet"

    with TestClient(app) as client:
        # Model must already be loaded here -- before any request was made.
        assert calls["n"] == 1
        assert xgboost_model.is_ready() is True

        health = client.get("/health")
        assert health.status_code == 200
        assert health.json() == {"status": "ok", "model_ready": True}

        # A disk load must not have happened again just by checking /health.
        assert calls["n"] == 1


@requires_trained_artifact
@requires_bitcoin_data
def test_predict_request_does_not_trigger_another_model_load(monkeypatch):
    calls = {"n": 0}
    real_joblib_load = xgboost_model.joblib.load

    def counting_load(path):
        calls["n"] += 1
        return real_joblib_load(path)

    monkeypatch.setattr(xgboost_model.joblib, "load", counting_load)

    from main import app

    history = _load_history("bitcoin")
    payload = {"coinId": "bitcoin", "history": history, "btcHistory": history}

    with TestClient(app) as client:
        assert calls["n"] == 1  # loaded once at startup

        first = client.post("/predict", json=payload)
        second = client.post("/predict", json=payload)

        assert first.status_code == 200
        assert second.status_code == 200
        body = first.json()
        assert body["coinId"] == "bitcoin"
        assert body["direction"] in ("UP", "DOWN", "FLAT")
        assert 0.0 <= body["confidence"] <= 100.0
        assert body["horizon"] == "24h"

        # Two /predict calls after startup must still be exactly one disk load.
        assert calls["n"] == 1


def test_startup_fails_clearly_when_artifact_missing(monkeypatch, tmp_path):
    """Startup must fail loudly (not silently start in a broken prediction
    state) when the artifact file isn't there -- covers the "fail clearly"
    requirement without needing a corrupt real artifact fixture."""
    monkeypatch.setattr(xgboost_model, "ARTIFACT_PATH", tmp_path / "does_not_exist.joblib")

    from main import app

    with pytest.raises(FileNotFoundError):
        with TestClient(app):
            pass

    assert xgboost_model.is_ready() is False


@requires_trained_artifact
def test_predict_still_works_via_direct_call_without_fastapi(monkeypatch):
    """predict() reuses load()'s single cache even when called directly (not
    through the FastAPI app) -- confirms there is only one loading/caching
    mechanism, not a second FastAPI-only path (Step 2 requirement)."""
    assert xgboost_model.is_ready() is False
    xgboost_model.load()
    assert xgboost_model.is_ready() is True

    if not BITCOIN_DATA_AVAILABLE:
        pytest.skip("No raw data at ai-service/data/bitcoin.json")

    history = _load_history("bitcoin")
    result = xgboost_model.predict("bitcoin", history, history)
    assert result["direction"] in ("UP", "DOWN", "FLAT")
