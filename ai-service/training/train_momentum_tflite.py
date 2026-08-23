"""Trains and exports the Phase 5b on-device "price-momentum" TFLite model.

This is a deliberately small, separate model from Phase 5's server-side XGBoost
predictor -- different purpose (an instant, fully-offline local signal shown
alongside, not instead of, the server prediction), different feature set (only
3 plain % returns, not the ~20-indicator set feature_engineering.py computes),
and a different runtime (TFLite Interpreter on-device, not a FastAPI service).

The 3-feature design is intentional: 1-day/3-day/7-day % return are each a
single subtraction+division, simple enough that OnDeviceMomentumClassifier.kt
can recompute the exact same formulas on-device from already-cached price
history without that Kotlin code rising to "feature engineering" (Tech
Stack's rule: Java/Kotlin never contains model logic -- training and
conversion happen here, in Python; Android only runs inference on numbers it
computed with plain arithmetic).

Reuses ai-service/data/*.json (the same raw CoinGecko history train_xgboost.py
already fetched via fetch_training_data.py -- no new network calls), the same
pooled multi-coin approach, the same +/-1.5% labeling threshold Phase 5 already
validated (not re-litigated here), and the same time-based-split-with-purge-gap
leakage-prevention discipline as train_xgboost.py.

Per explicit instruction: this script reports precision/recall/F1 and a
majority-class baseline comparison honestly -- a low-signal result must be
reported as such, not glossed over because the pipeline "runs".
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.metrics import classification_report, confusion_matrix

COINS = [
    "bitcoin", "ethereum", "solana", "binancecoin", "ripple", "cardano",
    "dogecoin", "polkadot", "chainlink", "litecoin", "avalanche-2", "tron",
]
DATA_DIR = Path(__file__).resolve().parent.parent / "data"
ARTIFACT_DIR = Path(__file__).resolve().parent.parent / "artifacts"

THRESHOLD_PCT = 1.5  # same threshold Phase 5 validated against the dataset -- see ai-service/README.md
TRAIN_FRACTION = 0.8
PURGE_DAYS_MS = 2 * 24 * 60 * 60 * 1000  # same purge gap as train_xgboost.py -- 1-day label lookahead + buffer
WARMUP_DAYS = 7  # longest single lookback needed below (the 7-day return) -- much shorter than
                  # Phase 5's 40-day warmup since there is no MACD/EMA compounding here
LABELS = ["DOWN", "FLAT", "UP"]

FEATURE_COLUMNS = ["ret_1d", "ret_3d", "ret_7d"]


def load_coin_series(coin_id: str) -> pd.DataFrame:
    raw = json.loads((DATA_DIR / f"{coin_id}.json").read_text())
    df = pd.DataFrame(raw["prices"], columns=["timestamp", "price"]).sort_values("timestamp").reset_index(drop=True)
    return df


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    """The exact 3 formulas OnDeviceMomentumClassifier.kt must replicate on-device:
    ret_Nd(t) = (price[t] - price[t-N]) / price[t-N] * 100."""
    out = df.copy()
    out["ret_1d"] = out["price"].pct_change(periods=1, fill_method=None) * 100.0
    out["ret_3d"] = out["price"].pct_change(periods=3, fill_method=None) * 100.0
    out["ret_7d"] = out["price"].pct_change(periods=7, fill_method=None) * 100.0
    return out


def label_direction(df: pd.DataFrame, threshold_pct: float) -> pd.Series:
    next_return_pct = (df["price"].shift(-1) - df["price"]) / df["price"] * 100.0
    labels = pd.Series("FLAT", index=df.index, dtype="object")
    labels[next_return_pct > threshold_pct] = "UP"
    labels[next_return_pct < -threshold_pct] = "DOWN"
    return labels


def build_pooled_dataset() -> pd.DataFrame:
    frames = []
    for coin_id in COINS:
        df = load_coin_series(coin_id)
        featured = build_features(df)
        featured["label"] = label_direction(df, THRESHOLD_PCT)
        featured["coin_id"] = coin_id
        # Drop warmup rows (NaN ret_7d) and the last row (no next-day label) -- same
        # convention as build_dataset.py's iloc[WARMUP:-1] slice.
        featured = featured.iloc[WARMUP_DAYS:-1].copy()
        frames.append(featured)
    return pd.concat(frames, ignore_index=True)


def time_based_split(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, int]:
    sorted_ts = np.sort(df["timestamp"].unique())
    cutoff_ts = int(sorted_ts[int(len(sorted_ts) * TRAIN_FRACTION)])
    train = df[(df["timestamp"] < cutoff_ts) & (df["timestamp"] + PURGE_DAYS_MS < cutoff_ts)]
    val = df[df["timestamp"] >= cutoff_ts]
    return train, val, cutoff_ts


def majority_class_baseline(train_labels: pd.Series, val_len: int) -> np.ndarray:
    majority = train_labels.value_counts().idxmax()
    return np.full(val_len, majority)


def report(name: str, y_true: pd.Series, y_pred) -> dict:
    print(f"\n=== {name} ===")
    print(classification_report(y_true, y_pred, labels=LABELS, zero_division=0))
    cm = confusion_matrix(y_true, y_pred, labels=LABELS)
    print(f"Confusion matrix (rows=true, cols=predicted), labels={LABELS}:")
    print(cm)
    return {
        "report": classification_report(y_true, y_pred, labels=LABELS, zero_division=0, output_dict=True),
        "confusion_matrix": cm.tolist(),
        "labels_order": LABELS,
    }


def build_model() -> tf.keras.Model:
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(len(FEATURE_COLUMNS),)),
        tf.keras.layers.Dense(8, activation="relu"),
        tf.keras.layers.Dense(len(LABELS), activation="softmax"),
    ])
    model.compile(optimizer="adam", loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    return model


def main() -> None:
    pooled = build_pooled_dataset()
    train_df, val_df, cutoff_ts = time_based_split(pooled)
    print(f"Pooled momentum dataset: {len(pooled)} rows, {pooled['coin_id'].nunique()} coins, "
          f"threshold={THRESHOLD_PCT}%, features={FEATURE_COLUMNS}")
    print(f"Train: {len(train_df)} rows | Val: {len(val_df)} rows | cutoff_ts={cutoff_ts} "
          f"({pd.to_datetime(cutoff_ts, unit='ms').date()})")

    label_to_idx = {label: i for i, label in enumerate(LABELS)}
    idx_to_label = {i: label for label, i in label_to_idx.items()}

    X_train = train_df[FEATURE_COLUMNS].to_numpy(dtype="float32")
    X_val = val_df[FEATURE_COLUMNS].to_numpy(dtype="float32")
    y_train = train_df["label"]
    y_val = val_df["label"]
    y_train_idx = y_train.map(label_to_idx).to_numpy()

    results = {}
    results["majority_class_baseline"] = report(
        "Baseline: majority class", y_val, majority_class_baseline(y_train, len(y_val)),
    )

    model = build_model()
    model.fit(X_train, y_train_idx, epochs=30, batch_size=32, verbose=0)

    val_pred_idx = np.argmax(model.predict(X_val, verbose=0), axis=1)
    val_pred = np.array([idx_to_label[i] for i in val_pred_idx])
    results["momentum_model"] = report("On-device momentum model (Keras, pre-TFLite)", y_val, val_pred)

    baseline_macro_f1 = results["majority_class_baseline"]["report"]["macro avg"]["f1-score"]
    model_macro_f1 = results["momentum_model"]["report"]["macro avg"]["f1-score"]
    beats_baseline = model_macro_f1 > baseline_macro_f1
    print(f"\nMajority-class baseline macro F1: {baseline_macro_f1:.4f}")
    print(f"Momentum model macro F1:          {model_macro_f1:.4f}")
    print(f"Model beats majority-class baseline on macro F1: {beats_baseline}")
    if not beats_baseline:
        print("HONEST FINDING: the on-device momentum model does NOT outperform the naive "
              "majority-class baseline on this validation split -- it should be reported as "
              "not providing useful signal beyond the baseline, not as a working feature that "
              "merely 'runs'.")

    # Convert to TFLite (dynamic-range quantization -- small footprint, no representative
    # dataset needed since the model has no conv layers to benefit from full-int8 calibration).
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    ARTIFACT_DIR.mkdir(exist_ok=True)
    tflite_path = ARTIFACT_DIR / "momentum_model.tflite"
    tflite_path.write_bytes(tflite_model)

    report_path = ARTIFACT_DIR / "momentum_training_report.json"
    report_path.write_text(json.dumps({
        "threshold_pct": THRESHOLD_PCT,
        "feature_columns": FEATURE_COLUMNS,
        "label_order": LABELS,
        "train_rows": len(train_df),
        "val_rows": len(val_df),
        "cutoff_ts": cutoff_ts,
        "baseline_macro_f1": baseline_macro_f1,
        "model_macro_f1": model_macro_f1,
        "model_beats_baseline": bool(beats_baseline),
        "results": results,
    }, indent=2))

    print(f"\nTFLite model written to {tflite_path} ({tflite_path.stat().st_size} bytes)")
    print(f"Training report written to {report_path}")
    print("\nNEXT STEP (manual, documented -- not automated): copy the .tflite file to "
          "app/src/main/assets/momentum_model.tflite for Android to bundle.")


if __name__ == "__main__":
    main()
