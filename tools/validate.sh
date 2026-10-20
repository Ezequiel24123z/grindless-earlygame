#!/usr/bin/env bash
# One deterministic foundation entry point. Add --smokes for the complete isolated inventory.
set -eu
ROOT="$(cd "${1:-.}" && pwd)"
SMOKES=false
if [ "$#" -gt 2 ]; then echo "usage: validate.sh [root] [--smokes]" >&2; exit 2; fi
if [ "$#" -eq 2 ]; then
  if [ "$2" != "--smokes" ]; then echo "Unknown validation option: $2" >&2; exit 2; fi
  SMOKES=true
fi
cd "$ROOT"
RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-$$"
LOG_DIR="$ROOT/build/validation/$RUN_ID"
mkdir -p "$LOG_DIR"
LEDGER="$LOG_DIR/status.tsv"
printf 'check\tstarted_utc\tfinished_utc\texit_code\tlog\n' > "$LEDGER"
trap 'printf "ERROR\t%s\t\t1\tinterrupted\n" "$(date -u +%FT%TZ)" >> "$LEDGER"; exit 1' HUP INT TERM
{
  printf 'HEAD=%s\nWorking directory=repository root\n' "$(git rev-parse HEAD)"
  git status --porcelain
} > "$LOG_DIR/source.txt"

check() {
  local name="$1" started code
  shift
  started="$(date -u +%FT%TZ)"
  if "$@" > "$LOG_DIR/$name.log" 2>&1; then code=0; else code=$?; fi
  printf '%s\t%s\t%s\t%s\t%s.log\n' "$name" "$started" "$(date -u +%FT%TZ)" "$code" "$name" >> "$LEDGER"
  echo "$name exit=$code (build/validation/$RUN_ID/$name.log)"
  if [ "$code" -ne 0 ]; then tail -35 "$LOG_DIR/$name.log"; exit "$code"; fi
}

check environment "$ROOT/gradlew" --version
check build "$ROOT/gradlew" --no-daemon --console=plain :common:compileJava build
check behavior bash "$ROOT/tools/run-checks.sh" "$ROOT"
check documentation bash "$ROOT/tools/check-links.sh" "$ROOT"
if [ "$SMOKES" = true ]; then
  check smokes bash "$ROOT/tools/run-smokes.sh" "$ROOT"
else
  echo "Full smokes not requested; this is the fast gate, not integration acceptance."
fi
echo "Requested validation completed. Ledger: build/validation/$RUN_ID/status.tsv"
