#!/usr/bin/env bash
# Reuse the complete physical T0 contract; no JVM, codecs or second implementation.
set -eu
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "NOT_EXECUTED: PowerShell Core (pwsh) is required for the physical T0 audio contract." >&2
  exit 2
fi
exec pwsh -NoProfile -File "$HERE/verify-mixer-audio.ps1" "$@"
