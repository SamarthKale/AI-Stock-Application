# AI Crypto Predictor — Prediction Service (Phase 5)

FastAPI service that predicts a coin's 24h price direction (Up/Down/Flat) with a
single confidence score, matching Android's existing `Prediction` model exactly
(see the Phase 5 plan — output shape was deliberately kept unchanged from what
`PredictionConfidenceBar` already renders, no Android UI change needed).

## Dataset

**Pooled, multi-coin** — one model trained across all 12 coins together, not one
model per coin (Phase 5 plan decision). `coin_id` is kept as a categorical
feature so the model can still learn coin-specific effects if any exist.

Coin list (mirrors `app/.../mock/MockCoins.kt` exactly, so the pipeline stays
consistent with what the app already shows): bitcoin, ethereum, solana,
binancecoin, ripple, cardano, dogecoin, polkadot, chainlink, litecoin,
avalanche-2, tron.

- **Source:** CoinGecko `/coins/{id}/market_chart?vs_currency=usd&days=365`, one call
  per coin (`training/fetch_training_data.py`) — 366 raw daily points per coin.
- **After feature warmup (40 days, see below) and dropping the unlabeled last row:**
  325 rows per coin × 12 coins = **3,900 pooled training rows**, spanning
  ~2025-08 to 2026-08 (the trailing 365 days as of when this was built).
- Raw data cached in `ai-service/data/*.json` (gitignored) — re-run
  `fetch_training_data.py` to refresh.

## Label definition

`label(t) = direction of (price[t+1] - price[t]) / price[t]`, i.e. the label at
row *t* is what actually happened by the next day — the target the model
predicts *at* time *t* using only data available up to *t*.

**Threshold: ±1.5%.** This was validated against the alternatives, not assumed
— actual pooled-dataset distribution at each candidate threshold:

| Threshold | UP | DOWN | FLAT |
|---|---|---|---|
| 1.0% | 28.7% | 36.0% | 35.3% |
| **1.5% (chosen)** | **22.4%** | **29.5%** | **48.1%** |
| 2.0% | 18.3% | 23.5% | 58.1% |
| 2.5% | 14.4% | 18.8% | 66.8% |

1.0% is close to daily noise for crypto (risks labeling routine volatility as a
directional signal); 2.0%+ lets FLAT dominate over half to two-thirds of the
data, which would let a naive "always predict FLAT" strategy post artificially
high accuracy (exactly the failure mode the majority-class baseline below
demonstrates). 1.5% keeps every class above 20% while still requiring a
meaningfully-sized move to count as directional.

One consistent, honestly-reported observation: **DOWN is more common than UP at
every threshold tested** (29.5% vs 22.4% at the chosen threshold). This reflects
the actual trailing-365-day window the training data was pulled in (a net
volatile/declining period for these coins, e.g. Bitcoin's price in this window
ran from an all-time-high down to roughly half that) — it is a property of
*when this was trained*, not a permanent asset-class bias. Retraining on a
different window will shift this.

## Feature engineering

Hand-rolled in `features/feature_engineering.py` (not `pandas-ta` — its PyPI
package metadata had an empty `license` field at the time of writing; only
~8 indicators were needed, so hand-rolling avoided a license-verification
step for negligible extra effort): SMA(7/14/30), EMA(12/26), RSI(14),
MACD(12,26,9), Bollinger Bands(20,2σ), 1d/7d rate-of-change, 7d/14d rolling
volatility, 14d volume trend, and three BTC-relative features (BTC's own 24h
return, and the coin's trailing 30d correlation/beta to BTC) — a crypto-specific
signal with no direct equities equivalent, since altcoin price action is
frequently BTC-driven.

**Warmup requirement: 40 days**, not simply the largest single window (30).
MACD's signal line is an EMA-of-an-EMA: the MACD line itself needs 26 rows
before its first valid value, and the 9-period signal EMA needs a further 9
valid MACD values on top of that (26 + 9 − 1 = 34) — verified empirically via
`build_dataset.py`'s NaN check, which is why the constant is 40 (34 plus a
safety margin), not the naively-assumed 30.

## Train/validation split and leakage prevention

- **Time-based, never shuffled.** A single global cutoff timestamp is computed
  at the 80th percentile of the pooled dataset's date range — because all 12
  coins share the same real-world calendar, this is a single date cutoff
  applied consistently across every coin, not a per-coin percentage split
  (which would let coins' individual date ranges drift out of alignment).
- **Purge gap: 2 days** at the boundary — sized to the label's exact 1-day
  lookahead (next-day return) plus a 1-day buffer, not the full 40-day feature
  warmup: a training row's *features* only ever look backward from its own
  timestamp (never leak forward), but a training row's *label* looks 1 day
  ahead — so any training row whose next-day price falls on or after the
  cutoff is excluded, keeping the validation period completely uncontaminated.
- Actual run: 3,096 train rows / 780 validation rows, cutoff at 2026-06-16.
- **Not yet implemented, documented as future work:** walk-forward validation
  (multiple rolling splits moving forward in time) would give a more robust
  performance estimate than this single split, especially given crypto's
  regime shifts (bull/bear cycles) — worth adding before trusting this model
  for anything beyond an illustrative Phase 5 integration.

## Results — baselines vs. XGBoost (honest reporting, not accuracy alone)

Two naive baselines were run alongside XGBoost specifically to check whether
the model adds real value, per the Phase 5 plan's explicit instruction not to
claim quality from accuracy alone:

| Model | Accuracy | Macro F1 | DOWN P/R | FLAT P/R | UP P/R |
|---|---|---|---|---|---|
| Majority-class (always "FLAT") | 0.60 | **0.25** | 0.00 / 0.00 | 0.60 / 1.00 | 0.00 / 0.00 |
| Persistence ("tomorrow repeats today") | 0.48 | 0.37 | 0.28 / 0.28 | 0.62 / 0.63 | 0.22 / 0.20 |
| **XGBoost** | 0.56 | **0.39** | 0.35 / 0.21 | 0.66 / 0.82 | 0.21 / 0.14 |

Full classification reports and confusion matrices: `artifacts/training_report.json`.

**Honest interpretation:** XGBoost has the best macro-F1 of the three (0.39 vs
0.37 persistence vs 0.25 majority-class) — it is not simply memorizing the
majority class, and it modestly outperforms both naive baselines. However, its
UP/DOWN recall is weak (0.14–0.21) — most of its edge over the majority-class
baseline comes from correctly calling FLAT more often (0.82 recall) while still
catching *some* real UP/DOWN moves the majority-class baseline catches none of
(0.00/0.00). **This should be described as modest, not strong, predictive
signal.** It is a legitimate first-pass result for a technical-indicators-only
model on ~3,900 pooled samples with no sentiment/news/on-chain data — not
evidence of a production-grade trading signal. The "Predictions are for
informational purposes only" disclaimer (already planned for Phase 6) is
doing real work here, not boilerplate.

## Architecture

```
Android (already has cached history via CoinRepository)
  --POST {coinId, history}--> Spring Boot POST /api/predictions/{coinId}
    --checks Postgres prediction_cache first--
    --cache miss/stale: forwards--> FastAPI POST /predict
    <--PredictionResponseDto--
  <--PredictionResponseDto-- Android (PredictionRepository, writes CachedPredictionDao)
```

Model loaded once at FastAPI startup (`main.py`), never per-request. LSTM/GRU
(`models/lstm_model.py`) is a stub/interface only in Phase 5, per the plan —
XGBoost is the only trained model this phase.

## Running locally

```
python -m pip install -r requirements.txt
COINGECKO_API_KEY=... python training/fetch_training_data.py   # one-time/periodic, not part of the running service
python training/train_xgboost.py                                # writes artifacts/xgboost_model.joblib
uvicorn main:app --host 0.0.0.0 --port 8000
```
