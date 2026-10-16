#!/usr/bin/env bash
# Linux/macOS twin of tools/run-checks.ps1. Gradle supplies the compiled classes and classpath.
# Usage: tools/run-checks.sh [project-root]
set -u

ROOT="$(cd "${1:-.}" && pwd)" || exit 1
CHECK_DIR="$ROOT/tools/checks"
cd "$ROOT" || exit 1

SOURCES=()
while IFS= read -r source; do SOURCES+=("$source"); done < <(find "$CHECK_DIR" -maxdepth 1 -name '*.java' | sort)
if [ "${#SOURCES[@]}" -eq 0 ]; then echo "No checks found in $CHECK_DIR."; exit 1; fi

"$ROOT/gradlew" --no-daemon --quiet --console=plain :common:writeCheckClasspath ||
  { echo "COULD NOT PREPARE CHECK CLASSPATH"; exit 1; }
CLASSPATH="$(<"$ROOT/common/build/checks/runtime-classpath.txt")"
if [ -z "$CLASSPATH" ]; then echo "Gradle exported an empty check classpath."; exit 1; fi
mkdir -p "$ROOT/build" || exit 1
OUT="$(mktemp -d "$ROOT/build/checks.XXXXXX")" || exit 1
trap 'rm -rf -- "$OUT"' EXIT

CP_COMPILE="$CLASSPATH"
CP_RUN="$OUT:$CLASSPATH"

javac -nowarn -encoding UTF-8 -cp "$CP_COMPILE" -d "$OUT" "${SOURCES[@]}" || { echo "CHECKS DID NOT COMPILE"; exit 1; }

FAILED=0
for src in "${SOURCES[@]}"; do
  name="$(basename "$src" .java)"
  pkg="$(sed -n 's/^package \([A-Za-z0-9_.]*\);.*/\1/p' "$src" | head -1)"
  echo "--- $name ---"
  if [ -n "$pkg" ]; then fq="$pkg.$name"; else fq="$name"; fi
  java -cp "$CP_RUN" "$fq" || FAILED=$((FAILED + 1))
done

if [ "$FAILED" -gt 0 ]; then echo "$FAILED CHECK SUITE(S) FAILED"; exit 1; fi
echo "ALL CHECK SUITES PASSED"
