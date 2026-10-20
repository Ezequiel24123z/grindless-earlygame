#!/usr/bin/env bash
# Usage: tools/build-modpack.sh [project-root] [expanded|preparation|playable] [runtime-revision]
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
PROFILE="${2:-expanded}"
case "$PROFILE" in expanded|preparation|playable) ;; *) echo "Unknown pack profile: $PROFILE" >&2; exit 1 ;; esac
ARGS=(buildModpack "-PpackProfile=$PROFILE" --console=plain)
if [ -n "${3:-}" ]; then ARGS+=("-PpackRuntimeRevision=$3"); fi
(cd "$ROOT" && ./gradlew "${ARGS[@]}")
