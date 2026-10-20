#!/usr/bin/env bash
set -euo pipefail
here="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
tool=comfy_asset
if [[ "${1:-}" == "--tool" ]]; then
    tool="${2:-}"
    shift 2
fi
case "$tool" in
    comfy_asset|visual_presets|t0_visuals) ;;
    *) printf '%s\n' "Unknown visual tool: $tool" >&2; exit 2 ;;
esac
exec "${PYTHON:-python3}" -B -c \
    "import sys,runpy; sys.path.insert(0,sys.argv.pop(1)); runpy.run_module(sys.argv.pop(1),run_name='__main__')" \
    "$here" "$tool" "$@"
