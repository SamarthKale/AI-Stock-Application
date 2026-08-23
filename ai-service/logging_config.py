"""Phase 6: structured (JSON) logging for the ai-service, stdlib-only (no new dependency, so the
already ARM64-verified requirements.txt pin set — see CLAUDE.md's Phase 6 plan step 6 — doesn't
need re-checking). Mirrors the backend's logstash-logback-encoder JSON shape closely enough that
both services' Docker container logs read the same way under `docker compose logs`.
"""
from __future__ import annotations

import json
import logging
import os
import sys
import time


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(record.created)),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "service": "ai-service",
        }
        for key in ("method", "path", "status", "durationMs", "coinId"):
            value = getattr(record, key, None)
            if value is not None:
                payload[key] = value
        if record.exc_info:
            payload["exc_info"] = self.formatException(record.exc_info)
        return json.dumps(payload)


def configure_logging() -> None:
    """Called once at import time by main.py. Plain human-readable text locally (LOG_FORMAT unset
    or 'text'), JSON when LOG_FORMAT=json — set that in docker-compose.yml/the OCI deployment,
    same env-var-driven convention already used throughout the backend's application.yml."""
    handler = logging.StreamHandler(sys.stdout)
    if os.environ.get("LOG_FORMAT", "text").lower() == "json":
        handler.setFormatter(JsonFormatter())
    else:
        handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)-8s %(name)s - %(message)s"))
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(logging.INFO)
