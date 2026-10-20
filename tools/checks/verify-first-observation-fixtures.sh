#!/usr/bin/env bash
# Shared source-only implementation; never launches Minecraft or Java.
set -euo pipefail
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/verify-first-observation-fixtures.ps1"
if command -v powershell.exe >/dev/null 2>&1; then
    if command -v cygpath >/dev/null 2>&1; then
        SCRIPT="$(cygpath -w "$SCRIPT")"
    else
        SCRIPT="$(CDPATH= cd -- "$SCRIPT_DIR" && pwd -W)/verify-first-observation-fixtures.ps1"
    fi
    exec powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "$SCRIPT" "$@"
elif command -v pwsh >/dev/null 2>&1; then
    exec pwsh -NoLogo -NoProfile -NonInteractive -File "$SCRIPT" "$@"
else
    printf '%s\n' 'PowerShell 5.1+ or pwsh is required for the source-only fixture checks.' >&2
    exit 127
fi
