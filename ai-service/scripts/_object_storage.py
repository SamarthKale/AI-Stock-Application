"""Shared OCI Object Storage client construction for publish_model_artifacts.py. (Its former
sibling, fetch_model_artifacts.py, was removed in the Phase 6 CI/CD fix — model artifacts are now
committed directly to the repo instead of fetched at build time; see CLAUDE.md's Phase 6 STATUS
block. This module is kept, currently unused by the build/deploy path, as the publish half of a
possible future OCI Object Storage path.) OCI Object Storage exposes an S3-compatible API (see
CLAUDE.md's Phase 6
plan §2c: "Customer Secret Key... scoped for S3-compatible Object Storage access"), so plain
boto3 works against it with no OCI-specific SDK dependency — one less thing to keep ARM64-verified
in the production image, since this module is never imported there (see requirements-ops.txt).

Required environment variables (never hardcoded, never committed — see CLAUDE.md's secrets rule):
    OCI_OBJECT_STORAGE_ENDPOINT  e.g. https://<namespace>.compat.objectstorage.<region>.oraclecloud.com
    OCI_OBJECT_STORAGE_BUCKET    bucket name (model artifacts and backups may share one bucket
                                  under different key prefixes, or use two buckets — see §2c)
    OCI_ACCESS_KEY_ID            OCI Customer Secret Key access key
    OCI_SECRET_ACCESS_KEY        OCI Customer Secret Key secret key
"""
from __future__ import annotations

import os


class MissingConfigError(RuntimeError):
    pass


def _require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise MissingConfigError(f"{name} is not set — see this module's docstring for the required env vars")
    return value


def object_storage_client():
    import boto3  # local import: only ever needed by the ops scripts, not the runtime service

    endpoint = _require_env("OCI_OBJECT_STORAGE_ENDPOINT")
    access_key = _require_env("OCI_ACCESS_KEY_ID")
    secret_key = _require_env("OCI_SECRET_ACCESS_KEY")
    return boto3.client(
        "s3",
        endpoint_url=endpoint,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        # OCI's S3-compatible endpoint is region-agnostic path-wise; boto3 still requires some
        # region string be set even though OCI ignores it.
        region_name="us-ashburn-1",
    )


def bucket_name() -> str:
    return _require_env("OCI_OBJECT_STORAGE_BUCKET")
