"""Builds the pooled multi-coin training dataset from the raw JSON fetched by
fetch_training_data.py: loads each coin's daily price/volume series, computes
features (including BTC-relative features), labels each row Up/Down/Flat based
on the next day's return, and pools every coin into one DataFrame with `coin_id`
kept as a categorical feature (Phase 5 plan decision: pooled dataset, not one
model per coin; coinId available as a feature).
"""
from __future__ import annotations

import json
from pathlib import Path

import pandas as pd

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from features.feature_engineering import build_feature_frame, align_btc_prices, FEATURE_COLUMNS, REQUIRED_WARMUP_DAYS  # noqa: E402

COINS = [
    "bitcoin", "ethereum", "solana", "binancecoin", "ripple", "cardano",
    "dogecoin", "polkadot", "chainlink", "litecoin", "avalanche-2", "tron",
]
DATA_DIR = Path(__file__).resolve().parent.parent / "data"


def load_coin_series(coin_id: str) -> pd.DataFrame:
    raw = json.loads((DATA_DIR / f"{coin_id}.json").read_text())
    prices = pd.DataFrame(raw["prices"], columns=["timestamp", "price"])
    volumes = pd.DataFrame(raw["total_volumes"], columns=["timestamp", "volume"])
    df = prices.merge(volumes, on="timestamp", how="inner").sort_values("timestamp").reset_index(drop=True)
    return df


def label_direction(df: pd.DataFrame, threshold_pct: float) -> pd.Series:
    """Label row t by the return from price[t] to price[t+1] (next day) -- the
    target the model predicts *at* time t. The last row has no next-day price and
    is dropped by the caller."""
    next_return_pct = (df["price"].shift(-1) - df["price"]) / df["price"] * 100.0
    labels = pd.Series("FLAT", index=df.index, dtype="object")
    labels[next_return_pct > threshold_pct] = "UP"
    labels[next_return_pct < -threshold_pct] = "DOWN"
    return labels


def build_pooled_dataset(threshold_pct: float) -> pd.DataFrame:
    btc_df = load_coin_series("bitcoin")

    frames = []
    for coin_id in COINS:
        df = load_coin_series(coin_id)
        btc_aligned = align_btc_prices(df["timestamp"], btc_df)
        featured = build_feature_frame(df, btc_prices=btc_aligned)
        featured["label"] = label_direction(df, threshold_pct)
        featured["coin_id"] = coin_id
        # Drop warmup rows (NaN features) and the last row (no next-day label).
        featured = featured.iloc[REQUIRED_WARMUP_DAYS:-1].copy()
        frames.append(featured)

    pooled = pd.concat(frames, ignore_index=True)
    pooled["coin_id"] = pooled["coin_id"].astype("category")
    return pooled


if __name__ == "__main__":
    THRESHOLD = 1.5
    pooled = build_pooled_dataset(THRESHOLD)
    print(f"Pooled dataset: {len(pooled)} rows across {pooled['coin_id'].nunique()} coins")
    print(f"Date-equivalent rows per coin:\n{pooled.groupby('coin_id', observed=True).size()}")
    print(f"\nLabel distribution at +/-{THRESHOLD}% threshold:")
    counts = pooled["label"].value_counts()
    pcts = pooled["label"].value_counts(normalize=True) * 100
    for label in ["UP", "DOWN", "FLAT"]:
        c = counts.get(label, 0)
        p = pcts.get(label, 0.0)
        print(f"  {label}: {c} ({p:.1f}%)")
    print(f"\nAny NaN in feature columns after warmup drop: {pooled[FEATURE_COLUMNS].isna().any().any()}")
