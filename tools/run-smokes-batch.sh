#!/usr/bin/env bash
# Linux/macOS twin of run-smokes-batch.ps1: one shared-world Forge boot, not isolated evidence.
#
#     GRINDLESS_ACCEPT_EULA=true tools/run-smokes-batch.sh .
#     tools/run-smokes-batch.sh . --only first-iron,first-factory,first-washer
#
# The existing smoke-boot.sh owns startup, SMOKE-READY, assertion budgets, shutdown and cleanup.
# Per-scenario .timeout files belong to run-smokes.sh, not this legacy shared-world batch.
set -eo pipefail

fail() {
  printf '%s\n' "$*" >&2
  exit 1
}

trim_space() {
  TRIMMED="${1#"${1%%[![:space:]]*}"}"
  TRIMMED="${TRIMMED%"${TRIMMED##*[![:space:]]}"}"
}

ROOT="."
ROOT_SET="no"
ONLY=""
ONLY_SET="no"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --only)
      [ "$#" -ge 2 ] || fail 'Expected comma-separated smoke names after --only.'
      [ "$ONLY_SET" = "no" ] || fail 'Specify --only once.'
      case "$2" in --*) fail 'Expected comma-separated smoke names after --only.' ;; esac
      ONLY="$2"
      ONLY_SET="yes"
      shift 2
      ;;
    --help|-h)
      printf '%s\n' 'Usage: tools/run-smokes-batch.sh [project-root] [--only name,name]'
      exit 0
      ;;
    --)
      shift
      [ "$ROOT_SET" = "no" ] && [ "$#" -eq 1 ] ||
        fail 'Expected one project root after --.'
      ROOT="$1"
      ROOT_SET="yes"
      shift
      ;;
    -*) fail "Unknown batch option: $1" ;;
    *)
      [ "$ROOT_SET" = "no" ] || fail 'Specify only one project root.'
      ROOT="$1"
      ROOT_SET="yes"
      shift
      ;;
  esac
done

ROOT="$(CDPATH= cd -- "$ROOT" && pwd)" || exit 1
SMOKE_DIR="$ROOT/tools/smoke"
RUN_DIR="$ROOT/forge/run"
mkdir -p "$RUN_DIR"
[ -f "$SMOKE_DIR/scenarios.txt" ] && [ -r "$SMOKE_DIR/scenarios.txt" ] ||
  fail "Missing or unreadable smoke scenario list: $SMOKE_DIR/scenarios.txt"

ALL=()
first="yes"
while IFS= read -r line || [ -n "$line" ]; do
  if [ "$first" = "yes" ]; then
    line="${line#$'\xef\xbb\xbf'}"
    first="no"
  fi
  trim_space "$line"
  case "$TRIMMED" in ''|\#*) continue ;; esac
  ALL+=("${TRIMMED%%[[:space:]]*}")
done < "$SMOKE_DIR/scenarios.txt"

# Preserve the PS batch's case-insensitive filtering and manifest order. Empty normalized
# selections mean all; unknown names only fail when no known scenario remains.
REQUESTED=()
remaining="$ONLY"
while :; do
  trim_space "${remaining%%,*}"
  if [ -n "$TRIMMED" ]; then
    REQUESTED+=("$(printf '%s' "$TRIMMED" | tr '[:upper:]' '[:lower:]')")
  fi
  case "$remaining" in
    *,*) remaining="${remaining#*,}" ;;
    *) break ;;
  esac
done

SELECTED=()
for name in "${ALL[@]}"; do
  if [ "${#REQUESTED[@]}" -gt 0 ]; then
    normalized="$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')"
    found="no"
    for requested in "${REQUESTED[@]}"; do
      if [ "$normalized" = "$requested" ]; then found="yes"; break; fi
    done
    [ "$found" = "yes" ] || continue
  fi
  SELECTED+=("$name")
done
[ "${#SELECTED[@]}" -gt 0 ] || fail 'No named smoke scenarios were found.'

BATCH=()
SKIPPED=()
NATIVE_SKIPPED="no"
for name in "${SELECTED[@]}"; do
  normalized="$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')"
  isolated="no"
  # A01's post-stop VeinData oracle requires exactly two worked chunks/total two in a fresh world.
  # workshop-ports needs an empty probe/drop region, not earlier scheduled fixture effects.
  # native-quests additionally needs the isolated FTB dependency composition.
  case "$normalized" in
    foreign-providers|states|recipe-cache-reload|extractor-backpressure|first-fluids|first-harness|workshop-ports|native-quests) isolated="yes" ;;
  esac
  # Even an invalid optional-pack path must never be treated as an ordinary shared-world case.
  if [ "$isolated" = "yes" ] || [ -e "$SMOKE_DIR/$name-optional" ] ||
      [ -L "$SMOKE_DIR/$name-optional" ]; then
    if [ "$normalized" = native-quests ]; then NATIVE_SKIPPED="yes"
    else SKIPPED+=("$name"); fi
  else
    BATCH+=("$name")
  fi
done
if [ "${#BATCH[@]}" -eq 0 ]; then
  [ "$NATIVE_SKIPPED" = "no" ] ||
    fail 'native-quests requires the isolated FTB backend; use tools/run-smokes.sh <project-root> --only native-quests.'
  fail 'The requested scenario(s) require a fresh datapack world; use tools/run-smokes.sh.'
fi

append_sidecar() {
  local path="$1" kind="$2" line first="yes"
  if [ ! -e "$path" ] && [ ! -L "$path" ]; then return 0; fi
  [ -f "$path" ] && [ -r "$path" ] || fail "Unreadable smoke $kind file: $path"
  while IFS= read -r line || [ -n "$line" ]; do
    if [ "$first" = "yes" ]; then
      line="${line#$'\xef\xbb\xbf'}"
      first="no"
    fi
    line="${line%$'\r'}"
    trim_space "$line"
    [ -n "$TRIMMED" ] || continue
    if [ "$kind" = "commands" ]; then
      case "$TRIMMED" in \#*) continue ;; esac
    fi
    printf '%s\n' "$line"
  done < "$path"
}

COMMAND_PATH="$RUN_DIR/smoke-batch.commands"
EXPECT_PATH="$RUN_DIR/smoke-batch.expect"
: > "$COMMAND_PATH"
: > "$EXPECT_PATH"
for name in "${BATCH[@]}"; do
  {
    printf 'say BATCH-BEGIN %s\n' "$name"
    append_sidecar "$SMOKE_DIR/$name.commands" commands
    printf 'say BATCH-END %s\n' "$name"
  } >> "$COMMAND_PATH"
  {
    printf 'BATCH-END %s\n' "$name"
    append_sidecar "$SMOKE_DIR/$name.expect" expectations
  } >> "$EXPECT_PATH"
done

printf 'Running %s shared-world smoke scenario(s) in one Forge boot.\n' "${#BATCH[@]}"
if [ "${#SKIPPED[@]}" -gt 0 ]; then
  printf 'Skipped fresh-world datapack scenario(s): '
  separator=""
  for name in "${SKIPPED[@]}"; do
    printf '%s%s' "$separator" "$name"
    separator=", "
  done
  printf '.\n'
fi
if [ "$NATIVE_SKIPPED" = "yes" ]; then
  printf '%s\n' 'Skipped native-quests: requires the isolated FTB backend (tools/run-smokes).'
fi

export SMOKE_COMMANDS="$COMMAND_PATH"
export SMOKE_EXPECT_FILE="$EXPECT_PATH"
export SMOKE_LOG="$RUN_DIR/smoke-batch.log"
unset SMOKE_DATAPACK SMOKE_OPTIONAL_DATAPACK SMOKE_QUEST_BACKEND
# Replace this runner so the native harness retains process ownership and its exact exit status.
exec "$ROOT/tools/smoke-boot.sh" "$ROOT"
