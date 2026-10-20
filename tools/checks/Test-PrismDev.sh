#!/usr/bin/env bash
# Runs the same isolated no-JVM/no-network fixtures, not a second implementation.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/Test-PrismDev.ps1"
if command -v powershell.exe >/dev/null 2>&1; then
    if command -v cygpath >/dev/null 2>&1; then SCRIPT="$(cygpath -w "$SCRIPT")"; fi
    exec powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "$SCRIPT" "$@"
elif command -v pwsh >/dev/null 2>&1; then
    exec pwsh -NoLogo -NoProfile -NonInteractive -File "$SCRIPT" "$@"
else
    printf '%s\n' 'PowerShell 5.1+ or pwsh is required to run the isolated Windows fixtures.' >&2
    exit 127
fi
