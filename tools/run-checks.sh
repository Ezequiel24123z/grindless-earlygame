#!/usr/bin/env bash
# Linux/macOS twin of tools/run-checks.ps1. Gradle supplies the compiled classes and classpath.
# Usage: tools/run-checks.sh [project-root]
set -u

ROOT="$(cd "${1:-.}" && pwd)" || exit 1
CHECK_DIR="$ROOT/tools/checks"
cd "$ROOT" || exit 1

is_javac17() {
  local output status line version='' count=0
  if output="$("$1" -version 2>&1)"; then status=0; else status=$?; fi
  printf '%s\n' "$output" >&2
  if [ "$status" -ne 0 ]; then printf 'javac -version exited with code %s.\n' "$status" >&2; fi
  while IFS= read -r line; do
    line="${line%$'\r'}"
    if [[ "$line" == javac\ * ]]; then count=$((count + 1)); version="$line"; fi
  done <<< "$output"
  [[ "$status" -eq 0 && "$count" -eq 1 &&
    "$version" =~ ^javac\ 17(\.[0-9]+)*([-+][0-9A-Za-z][0-9A-Za-z.+-]*)?$ ]]
}

if ! is_javac17 javac; then
  echo "A JDK 17 is required for checks. Select javac 17 on PATH."
  exit 1
fi

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
