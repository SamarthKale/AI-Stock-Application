"""Phase 6: publishes the locally trained model artifacts to OCI Object Storage under a new
version prefix, then advances the `LATEST` pointer object to that version.

Deliberately a separate, manually-triggered step (run locally after `training/train_xgboost.py` /
`training/train_momentum_tflite.py`, or from a manually-triggered GitHub Actions workflow) — never
invoked from deploy.yml or any Dockerfile. This is what CLAUDE.md's Phase 6 plan and the project's
own hard requirement mean by "never retrain during Docker builds": training happens here, once,
deliberately; the Docker build only ever *fetches* an already-published, pinned version (see
fetch_model_artifacts.py).

Usage:
    python scripts/publish_model_artifacts.py [--version YYYYMMDDHHMMSS]

Requires the OCI_OBJECT_STORAGE_* / OCI_*_ACCESS_KEY env vars documented in _object_storage.py.
"""
from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

from _object_storage import bucket_name, object_storage_client

ARTIFACTS_DIR = Path(__file__).resolve().parent.parent / "artifacts"
KEY_PREFIX = "model-artifacts"
LATEST_POINTER_KEY = f"{KEY_PREFIX}/LATEST"

# Every file that makes up "the model" — both the Phase 5 server-side XGBoost artifact and the
# Phase 5b on-device TFLite artifact travel together under the same version, since Android's
# CoinRepository/OnDeviceMomentumClassifier and the backend's PredictionClient are both meant to
# agree on "what version of the model is currently live" as a single unit, not two independently
# versioned things.
ARTIFACT_FILES = [
    "xgboost_model.joblib",
    "training_report.json",
    "momentum_model.tflite",
    "momentum_training_report.json",
]


def publish(version: str) -> None:
    missing = [f for f in ARTIFACT_FILES if not (ARTIFACTS_DIR / f).exists()]
    if missing:
        print(f"Missing artifact file(s), aborting: {missing}", file=sys.stderr)
        print("Run training/train_xgboost.py and training/train_momentum_tflite.py first.", file=sys.stderr)
        sys.exit(1)

    client = object_storage_client()
    bucket = bucket_name()

    for filename in ARTIFACT_FILES:
        key = f"{KEY_PREFIX}/{version}/{filename}"
        print(f"Uploading {filename} -> s3://{bucket}/{key}")
        client.upload_file(str(ARTIFACTS_DIR / filename), bucket, key)

    print(f"Advancing LATEST pointer -> {version}")
    client.put_object(Bucket=bucket, Key=LATEST_POINTER_KEY, Body=version.encode("utf-8"))
    print(f"Published version {version}.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--version",
        default=time.strftime("%Y%m%d%H%M%S", time.gmtime()),
        help="Version prefix to publish under (default: current UTC timestamp).",
    )
    args = parser.parse_args()
    publish(args.version)


if __name__ == "__main__":
    main()
