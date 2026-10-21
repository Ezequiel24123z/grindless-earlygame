#!/usr/bin/env bash
# One shared no-server implementation, including native-backend dispatch and mocked Sync.
# Both real runner twins are tested only against temporary fake launch boundaries.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/Test-SmokesBatch.ps1"
if command -v powershell.exe >/dev/null 2>&1; then
    if command -v cygpath >/dev/null 2>&1; then
        SCRIPT="$(cygpath -w "$SCRIPT")"
        export GRINDLESS_SMOKES_TEST_BASH="$(cygpath -w "$BASH")"
    else
        # Git's minimal bundled Bash may have neither bash.exe nor cygpath.
        SCRIPT="$(cd -- "$SCRIPT_DIR" && pwd -W)/Test-SmokesBatch.ps1"
        export GRINDLESS_SMOKES_TEST_BASH="$(cd -- "${BASH%/*}" && pwd -W)/${BASH##*/}"
    fi
    exec powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "$SCRIPT" "$@"
elif command -v pwsh >/dev/null 2>&1; then
    export GRINDLESS_SMOKES_TEST_BASH="$BASH"
    exec pwsh -NoLogo -NoProfile -NonInteractive -File "$SCRIPT" "$@"
else
    printf '%s\n' 'PowerShell 5.1+ or pwsh is required for these no-server tooling checks; supply -BashPath if needed.' >&2
    exit 127
fi
