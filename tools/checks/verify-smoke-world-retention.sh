#!/usr/bin/env bash
# Exercises real retention helpers/EXIT status and rejected entrypoints without a JVM.
set -euo pipefail
ROOT="$(cd "${1:-"$(dirname "$0")/../.."}" && pwd)"
BOOT="$ROOT/tools/smoke-boot.sh"
COUNT=0
ORIGINAL_KEEP="${SMOKE_KEEP_WORLD-<unset>}"
assert() {
  if ! "$@"; then echo "Smoke retention contract failed: $*" >&2; exit 1; fi
  COUNT=$((COUNT + 1))
}
DEFINITIONS="$(sed -n '/^validate_smoke_keep_world()/,/^}/p; /^cleanup_smoke_world()/,/^}/p' "$BOOT")"
eval "$DEFINITIONS"
assert validate_smoke_keep_world 0
assert validate_smoke_keep_world 1
mkdir -p "$ROOT/build"
FIXTURE="$(mktemp -d "$ROOT/build/retention-tests.XXXXXX")"
trap 'rm -rf -- "$FIXTURE"' EXIT
world() {
  mkdir -p "$FIXTURE/$1/smoke-world"
  printf 'owned-test-world\n' > "$FIXTURE/$1/smoke-world/owned.dat"
  printf '%s\n' "$FIXTURE/$1/smoke-world"
}
printf 'preserve-sibling\n' > "$FIXTURE/sibling.txt"
KEEP_SELECTION="$(grep -F 'KEEP_WORLD="${SMOKE_KEEP_WORLD-0}"' "$BOOT")"$'\n'"$(grep -F 'validate_smoke_keep_world "$KEEP_WORLD" || exit 1' "$BOOT")"
WORLD="$(world absent)"
env -u SMOKE_KEEP_WORLD WORLD="$WORLD" "$BASH" --noprofile --norc -c "$DEFINITIONS"$'\n'"$KEEP_SELECTION"$'\n''cleanup_smoke_world "$WORLD"'
assert test ! -e "$WORLD"
assert test "$(cat "$FIXTURE/sibling.txt")" = preserve-sibling
for value in 0 1; do
  WORLD="$(world mode-$value)"
  KEEP_WORLD="$value"
  cleanup_smoke_world "$WORLD" > "$FIXTURE/mode-$value.log"
  if [ "$value" = 0 ]; then
    assert test ! -e "$WORLD"
  else
    assert test "$(cat "$WORLD/owned.dat")" = owned-test-world
    assert grep -Fq 'SMOKE WORLD RETAINED' "$FIXTURE/mode-$value.log"
    assert grep -Fq "$WORLD" "$FIXTURE/mode-$value.log"
    assert grep -Fq 'not PASS or clean shutdown' "$FIXTURE/mode-$value.log"
  fi
  assert test "$(cat "$FIXTURE/sibling.txt")" = preserve-sibling
done
KEEP_WORLD=1
cleanup_smoke_world "$FIXTURE/missing/smoke-world" > "$FIXTURE/missing.log"
assert test ! -e "$FIXTURE/missing/smoke-world"
assert grep -Fq 'SMOKE WORLD ABSENT' "$FIXTURE/missing.log"
for code in 0 23; do
  WORLD="$(world exit-$code)"
  if KEEP_WORLD=1 WORLD="$WORLD" "$BASH" --noprofile --norc -c "$DEFINITIONS"$'\n''trap '\''cleanup_smoke_world "$WORLD"'\'' EXIT; exit '"$code" > "$FIXTURE/exit-$code.log" 2>&1; then
    result=0
  else
    result=$?
  fi
  assert test "$result" -eq "$code"
  assert test "$(cat "$WORLD/owned.dat")" = owned-test-world
  assert grep -Fq 'SMOKE WORLD RETAINED' "$FIXTURE/exit-$code.log"
done
for value in '' 2 true 01 ' 1' '1 ' -1 yes; do
  if env CI=false GRINDLESS_ACCEPT_EULA=false SMOKE_KEEP_WORLD="$value" "$BASH" --noprofile --norc "$BOOT" "$FIXTURE" > "$FIXTURE/invalid.log" 2>&1; then
    echo 'Invalid retention value accepted' >&2; exit 1
  fi
  assert grep -Fq 'Invalid SMOKE_KEEP_WORLD' "$FIXTURE/invalid.log"
  assert test ! -e "$FIXTURE/forge"
done
if env CI=false GRINDLESS_ACCEPT_EULA=false SMOKE_KEEP_WORLD=1 SMOKE_OPTIONAL_DATAPACK="$FIXTURE/missing-pack" "$BASH" --noprofile --norc "$BOOT" "$FIXTURE" > "$FIXTURE/preflight.log" 2>&1; then
  echo 'Retention bypassed optional preflight' >&2; exit 1
fi
assert grep -Fq 'Invalid smoke datapack' "$FIXTURE/preflight.log"
assert test ! -e "$FIXTURE/forge"
assert test "${SMOKE_KEEP_WORLD-<unset>}" = "$ORIGINAL_KEEP"
assert grep -Fq 'rm -rf "$RUN_DIR/smoke-world"' "$BOOT"
assert grep -Fq 'cleanup_smoke_world "$RUN_DIR/smoke-world"' "$BOOT"
reset_line="$(grep -nF 'rm -rf "$RUN_DIR/smoke-world"' "$BOOT" | cut -d: -f1)"
final_line="$(grep -nF 'cleanup_smoke_world "$RUN_DIR/smoke-world"' "$BOOT" | cut -d: -f1)"
assert test "$reset_line" -lt "$final_line"
assert grep -Fq '"$JAVA" -Xmx2G ' "$BOOT"
KEEP_WORLD=0
cleanup_smoke_world "$FIXTURE/mode-1/smoke-world"
assert test ! -e "$FIXTURE/mode-1/smoke-world"
echo "ALL SMOKE WORLD RETENTION CHECKS PASSED: $COUNT (no JVM or Minecraft)"
