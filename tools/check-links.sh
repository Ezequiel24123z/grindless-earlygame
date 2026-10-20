#!/usr/bin/env bash
# Run the same documentation validator on Linux/macOS; requires PowerShell Core.
set -eu
ROOT="$(cd "${1:-.}" && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "PowerShell Core (pwsh) is required for the shared documentation validator." >&2
  exit 1
fi
exec pwsh -NoProfile -File "$ROOT/tools/check-links.ps1" -Root "$ROOT"
