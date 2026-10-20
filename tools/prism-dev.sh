#!/usr/bin/env bash
# Thin twin: the deployment contract and implementation live in prism-dev.ps1.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/prism-dev.ps1"
if command -v powershell.exe >/dev/null 2>&1; then
    if command -v cygpath >/dev/null 2>&1; then SCRIPT="$(cygpath -w "$SCRIPT")"; fi
    exec powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "$SCRIPT" "$@"
elif command -v pwsh >/dev/null 2>&1; then
    exec pwsh -NoLogo -NoProfile -NonInteractive -File "$SCRIPT" "$@"
else
    printf '%s\n' 'PowerShell 5.1+ or pwsh is required; no alternate deployment implementation is used.' >&2
    exit 127
fi
