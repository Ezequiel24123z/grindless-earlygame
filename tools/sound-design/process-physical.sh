#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec pwsh -NoLogo -NoProfile -NonInteractive -File "$SCRIPT_DIR/process-physical.ps1" "$@"
