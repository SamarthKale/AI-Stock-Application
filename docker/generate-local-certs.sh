#!/usr/bin/env bash
# Phase 6 — local dev only. Generates a self-signed TLS cert so nginx can terminate HTTPS the
# same way locally (docker-compose.yml) as it will on the real OCI VM (Let's Encrypt via
# certbot, run separately against the VM's real sslip.io hostname — see CLAUDE.md's Phase 6
# plan §2b/§5). The browser/emulator will show a certificate-untrusted warning locally; that is
# expected and correct for a self-signed dev cert, not a bug.
set -euo pipefail

CERT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/nginx/certs"
mkdir -p "$CERT_DIR"

if [[ -f "$CERT_DIR/fullchain.pem" && -f "$CERT_DIR/privkey.pem" ]]; then
    echo "Certs already exist at $CERT_DIR — remove them first to regenerate."
    exit 0
fi

# The doubled leading slash in -subj avoids Git-for-Windows' MSYS2 bash rewriting "/CN=..." into
# a filesystem path (the standard MSYS workaround) — a no-op on real Linux/macOS shells, where
# "//CN=localhost" and "/CN=localhost" are equivalent.
openssl req -x509 -nodes -newkey rsa:2048 \
    -keyout "$CERT_DIR/privkey.pem" \
    -out "$CERT_DIR/fullchain.pem" \
    -days 365 \
    -subj "//CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

echo "Generated self-signed dev cert at $CERT_DIR"
