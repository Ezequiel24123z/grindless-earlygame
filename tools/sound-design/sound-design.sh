#!/usr/bin/env bash
# Shared implementation, like tools/check-links.sh. No Python/Node dependency.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "PowerShell Core (pwsh) is required. No provider request was made." >&2
  exit 1
fi
exec pwsh -NoProfile -File "$HERE/sound-design.ps1" "$@"
