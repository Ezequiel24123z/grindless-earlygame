#!/usr/bin/env bash
set -euo pipefail
here="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
root="$(CDPATH= cd -- "$here/.." && pwd)"
action=write
offline=()
while [ "$#" -gt 0 ]; do
  case "$1" in
    --root) root="$(CDPATH= cd -- "$2" && pwd)"; shift 2 ;;
    --check-only) action=check; shift ;;
    --offline) offline=(--offline); shift ;;
    *) echo "Unknown resource guidance argument." >&2; exit 1 ;;
  esac
done
# No second compiler bootstrap or build dependency: reuse the canonical export seam.
bash "$root/tools/dashboard/export.sh" --root "$root" --resource-guidance "$action" "${offline[@]}"
