"""One-time/periodic batch pull of historical daily price+volume data for the pooled
training dataset. NOT part of the running FastAPI service and never touches a live
request path (Phase 5 plan section 12) -- run manually or on a schedule outside the app.

Coin list mirrors app/src/main/java/.../mock/MockCoins.kt exactly (Phase 5 plan
decision: pooled multi-coin dataset, BTC plus a representative set of major assets,
same 12 coins the rest of the app already knows about) so the pipeline stays
consistent with what Android actually shows.

Usage:
    COINGECKO_API_KEY=... python training/fetch_training_data.py
"""
from __future__ import annotations

import json
import os
import sys
import time
from pathlib import Path

import requests

COINS = [
    "bitcoin", "ethereum", "solana", "binancecoin", "ripple", "cardano",
    "dogecoin", "polkadot", "chainlink", "litecoin", "avalanche-2", "tron",
]

VS_CURRENCY = "usd"
DAYS = 365
BASE_URL = "https://api.coingecko.com/api/v3"
DATA_DIR = Path(__file__).resolve().parent.parent / "data"


def fetch_market_chart(coin_id: str, api_key: str) -> dict:
    url = f"{BASE_URL}/coins/{coin_id}/market_chart"
    params = {"vs_currency": VS_CURRENCY, "days": DAYS}
    headers = {"x-cg-demo-api-key": api_key} if api_key else {}
    response = requests.get(url, params=params, headers=headers, timeout=30)
    response.raise_for_status()
    return response.json()


def main() -> None:
    api_key = os.environ.get("COINGECKO_API_KEY", "")
    if not api_key:
        print("WARNING: COINGECKO_API_KEY not set -- falling back to the anonymous rate-limit pool.", file=sys.stderr)

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    for i, coin_id in enumerate(COINS):
        out_path = DATA_DIR / f"{coin_id}.json"
        print(f"[{i + 1}/{len(COINS)}] fetching {coin_id}...")
        data = fetch_market_chart(coin_id, api_key)
        prices = data.get("prices", [])
        volumes = data.get("total_volumes", [])
        print(f"  -> {len(prices)} price points, {len(volumes)} volume points")
        out_path.write_text(json.dumps({"prices": prices, "total_volumes": volumes}))
        if i < len(COINS) - 1:
            time.sleep(2)  # stay well under the Demo tier's ~30 calls/min budget

    print(f"Done. Raw data written to {DATA_DIR}")


if __name__ == "__main__":
    main()
