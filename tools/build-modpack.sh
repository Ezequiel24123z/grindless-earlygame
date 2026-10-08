#!/usr/bin/env bash
# Builds the reproducible client-pack archive from the tracked CurseForge manifest and local jar.
# Usage: tools/build-modpack.sh [project-root]
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
PROPERTIES="$ROOT/gradle.properties"
MANIFEST="$ROOT/pack/curseforge/manifest.json"

read_property() {
  local name="$1"
  local value
  value="$(sed -n "s/^${name}=//p" "$PROPERTIES" | head -n 1)"
  [ -n "$value" ] || { echo "Missing $name in gradle.properties." >&2; exit 1; }
  printf '%s' "$value"
}

MOD_VERSION="$(read_property mod_version)"
PACK_VERSION="$(read_property pack_version)"
ARCHIVE_NAME="$(read_property archives_base_name)"
MANIFEST_VERSION="$(sed -n 's/^[[:space:]]*"version": "\([^"]*\)",/\1/p' "$MANIFEST" | head -n 1)"
[ "$MANIFEST_VERSION" = "$PACK_VERSION" ] || {
  echo "Pack manifest version '$MANIFEST_VERSION' differs from gradle.properties '$PACK_VERSION'." >&2
  exit 1
}

(cd "$ROOT" && ./gradlew :forge:remapJar)
JAR="$ROOT/forge/build/libs/$ARCHIVE_NAME-$MOD_VERSION-forge.jar"
[ -f "$JAR" ] || { echo "Expected remapped jar was not produced: $JAR" >&2; exit 1; }
command -v zip >/dev/null || { echo "zip is required to assemble the pack." >&2; exit 1; }

STAGE="$(mktemp -d "${TMPDIR:-/tmp}/grindless-pack.XXXXXX")"
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$STAGE/overrides/mods" "$ROOT/dist"
cp "$MANIFEST" "$STAGE/manifest.json"
cp "$JAR" "$STAGE/overrides/mods/"
OUTPUT="$ROOT/dist/Grindless-T0-$PACK_VERSION.zip"
rm -f "$OUTPUT"
(cd "$STAGE" && zip -qr "$OUTPUT" manifest.json overrides)
echo "Built $OUTPUT"
