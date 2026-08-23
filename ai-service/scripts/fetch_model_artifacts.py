"""Phase 6: downloads a published model artifact version from OCI Object Storage into
ai-service/artifacts/ — the only way the Docker build (or CI) ever acquires a model. Never trains
anything itself; see publish_model_artifacts.py's docstring for why training is a separate,
deliberate, manually-triggered step.

Usage:
    python scripts/fetch_model_artifacts.py [--version YYYYMMDDHHMMSS]

With no --version, resolves the current LATEST pointer. Requires the OCI_OBJECT_STORAGE_* /
OCI_*_ACCESS_KEY env vars documented in _object_storage.py.

Called from deploy.yml (and, for a local Docker build, manually) *before* `docker build` runs —
deliberately outside the Dockerfile, so OCI Object Storage credentials are never present in the
image or its build cache (see this repo's docker/ai-service.Dockerfile comment for the matching
half of this contract).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from _object_storage import bucket_name, object_storage_client
from publish_model_artifacts import ARTIFACT_FILES, KEY_PREFIX, LATEST_POINTER_KEY

ARTIFACTS_DIR = Path(__file__).resolve().parent.parent / "artifacts"


def resolve_version(client, bucket: str) -> str:
    response = client.get_object(Bucket=bucket, Key=LATEST_POINTER_KEY)
    return response["Body"].read().decode("utf-8").strip()


def fetch(version: str | None) -> None:
    client = object_storage_client()
    bucket = bucket_name()

    if version is None:
        version = resolve_version(client, bucket)
        print(f"Resolved LATEST -> {version}")

    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    for filename in ARTIFACT_FILES:
        key = f"{KEY_PREFIX}/{version}/{filename}"
        dest = ARTIFACTS_DIR / filename
        print(f"Downloading s3://{bucket}/{key} -> {dest}")
        client.download_file(bucket, key, str(dest))

    (ARTIFACTS_DIR / "VERSION").write_text(version, encoding="utf-8")
    print(f"Fetched version {version} into {ARTIFACTS_DIR}.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default=None, help="Specific version to fetch (default: current LATEST).")
    args = parser.parse_args()
    try:
        fetch(args.version)
    except Exception as e:  # noqa: BLE001 — top-level CLI entrypoint, deliberately broad
        print(f"fetch_model_artifacts failed: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
