"""Trains the XGBoost direction classifier on the pooled multi-coin dataset.

Time-based split (never shuffled) with a purge gap sized to the label's 1-day
lookahead, per Phase 5 plan section 9. Reports precision/recall/F1/confusion
matrix -- not accuracy alone, per the plan's explicit instruction -- for two
naive baselines AND the trained model, so the model's actual added value (or
lack of it) is visible rather than assumed.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import classification_report, confusion_matrix
from xgboost import XGBClassifier

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from features.feature_engineering import FEATURE_COLUMNS  # noqa: E402
from training.build_dataset import build_pooled_dataset  # noqa: E402

THRESHOLD_PCT = 1.5  # validated against 1.0/2.0/2.5% alternatives -- see ai-service/README.md
TRAIN_FRACTION = 0.8
PURGE_DAYS_MS = 2 * 24 * 60 * 60 * 1000  # covers the label's 1-day lookahead + 1-day buffer
ARTIFACT_DIR = Path(__file__).resolve().parent.parent / "artifacts"
LABELS = ["DOWN", "FLAT", "UP"]  # fixed order for confusion matrices / reports


def time_based_split(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, int]:
    sorted_ts = np.sort(df["timestamp"].unique())
    cutoff_ts = int(sorted_ts[int(len(sorted_ts) * TRAIN_FRACTION)])

    train = df[(df["timestamp"] < cutoff_ts) & (df["timestamp"] + PURGE_DAYS_MS < cutoff_ts)]
    val = df[df["timestamp"] >= cutoff_ts]
    return train, val, cutoff_ts


def majority_class_baseline(train_labels: pd.Series, val_len: int) -> np.ndarray:
    majority = train_labels.value_counts().idxmax()
    return np.full(val_len, majority)


def persistence_baseline(val_df: pd.DataFrame) -> np.ndarray:
    """'Tomorrow repeats today's realized direction' -- derived from roc_1d (today's
    own 1-day return, already computed as a feature), re-bucketed with the same
    threshold used for labeling, not a peek at tomorrow's actual label."""
    roc = val_df["roc_1d"].to_numpy()
    preds = np.full(len(val_df), "FLAT", dtype=object)
    preds[roc > THRESHOLD_PCT] = "UP"
    preds[roc < -THRESHOLD_PCT] = "DOWN"
    return preds


def report(name: str, y_true: pd.Series, y_pred: np.ndarray) -> dict:
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


def main() -> None:
    pooled = build_pooled_dataset(THRESHOLD_PCT)
    train_df, val_df, cutoff_ts = time_based_split(pooled)
    print(f"Pooled dataset: {len(pooled)} rows, {pooled['coin_id'].nunique()} coins, threshold={THRESHOLD_PCT}%")
    print(f"Train: {len(train_df)} rows | Val: {len(val_df)} rows | cutoff_ts={cutoff_ts} "
          f"({pd.to_datetime(cutoff_ts, unit='ms').date()})")
    print(f"Purge gap: rows within {PURGE_DAYS_MS // 86_400_000} day(s) of the cutoff excluded from training")

    results = {}
    results["majority_class_baseline"] = report(
        "Baseline: majority class", val_df["label"], majority_class_baseline(train_df["label"], len(val_df)),
    )
    results["persistence_baseline"] = report(
        "Baseline: persistence (today's realized direction)", val_df["label"], persistence_baseline(val_df),
    )

    X_train = train_df[FEATURE_COLUMNS + ["coin_id"]].copy()
    X_val = val_df[FEATURE_COLUMNS + ["coin_id"]].copy()
    y_train = train_df["label"]
    y_val = val_df["label"]

    model = XGBClassifier(
        n_estimators=300,
        max_depth=4,
        learning_rate=0.05,
        subsample=0.8,
        colsample_bytree=0.8,
        objective="multi:softprob",
        num_class=3,
        enable_categorical=True,
        eval_metric="mlogloss",
        random_state=42,
    )
    label_to_idx = {label: i for i, label in enumerate(LABELS)}
    y_train_idx = y_train.map(label_to_idx)
    y_val_idx = y_val.map(label_to_idx)
    model.fit(X_train, y_train_idx)

    val_pred_idx = model.predict(X_val)
    idx_to_label = {i: label for label, i in label_to_idx.items()}
    val_pred = np.array([idx_to_label[i] for i in val_pred_idx])
    results["xgboost"] = report("XGBoost", y_val, val_pred)

    ARTIFACT_DIR.mkdir(exist_ok=True)
    joblib.dump({"model": model, "feature_columns": FEATURE_COLUMNS, "label_order": LABELS,
                 "coin_categories": list(X_train["coin_id"].cat.categories), "threshold_pct": THRESHOLD_PCT},
                ARTIFACT_DIR / "xgboost_model.joblib")
    (ARTIFACT_DIR / "training_report.json").write_text(json.dumps({
        "threshold_pct": THRESHOLD_PCT,
        "train_rows": len(train_df),
        "val_rows": len(val_df),
        "cutoff_ts": cutoff_ts,
        "results": results,
    }, indent=2))
    print(f"\nModel + training report saved to {ARTIFACT_DIR}")


if __name__ == "__main__":
    main()
