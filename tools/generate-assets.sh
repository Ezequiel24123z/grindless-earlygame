#!/usr/bin/env bash
# Linux/macOS port of tools/generate-assets.ps1 (same logic, no PowerShell).
# Usage: tools/generate-assets.sh [project-root]
set -euo pipefail

ROOT="${1:-.}"
OUT="$(mktemp -d /tmp/grindless-assetgen.XXXXXX)"
trap 'rm -rf "$OUT"' EXIT

MATERIAL_SRC="$ROOT/common/src/main/java/io/github/ezequiel24123z/grindless/material"
mapfile -t SOURCES < <(find "$ROOT/tools/assetgen" -name '*.java' | sort)

# SupplyCatalogue is shared with the mod so the list of materials is written once. It and
# MaterialForm have no Minecraft imports, which is what lets the generator compile them alone.
# -encoding UTF-8 keeps this identical to the PowerShell run on a machine whose default charset
# is not UTF-8; without it the em dash in the provenance text is written as mojibake.
javac -nowarn -encoding UTF-8 -d "$OUT" "${SOURCES[@]}" "$MATERIAL_SRC/SupplyCatalogue.java" "$MATERIAL_SRC/MaterialForm.java" "$ROOT/common/src/main/java/io/github/ezequiel24123z/grindless/registry/BlockCatalogue.java"
java -cp "$OUT" io.github.ezequiel24123z.grindless.assetgen.GenerateAssets "$(cd "$ROOT" && pwd)"
