#!/usr/bin/env bash
# Reuse the offline contract; no JVM, codecs or second validation implementation.
set -eu
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "NOT_RUN: PowerShell Core (pwsh) is required for the Mixer audio source/resource contract." >&2
  exit 2
fi
exec pwsh -NoProfile -File "$HERE/verify-mixer-audio.ps1" "$@"
