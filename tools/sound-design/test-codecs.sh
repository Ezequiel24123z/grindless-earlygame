#!/usr/bin/env bash
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "PowerShell Core (pwsh) is required for the offline real-codec tests." >&2
  exit 1
fi
exec pwsh -NoProfile -File "$HERE/test-codecs.ps1" "$@"
