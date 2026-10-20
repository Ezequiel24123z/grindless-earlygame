#!/usr/bin/env bash
# Reuse the offline source contract; no JVM or second validation implementation.
set -eu
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "NOT_RUN: PowerShell Core (pwsh) is required for the passive TANK source contract." >&2
  exit 2
fi
exec pwsh -NoProfile -File "$HERE/verify-tank-feedback.ps1" "$@"
