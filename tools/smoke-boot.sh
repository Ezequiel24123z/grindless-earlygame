#!/usr/bin/env bash
# Boots the remapped distributable on an isolated production Forge server (ADR-0126).
# Requires boot, scenario assertions and a clean successful process exit.
#
#     tools/smoke-boot.sh [project-root]
#
# Why this exists: `./gradlew build` and the behaviour checks in tools/checks never instantiate
# Forge, so a mod that crashes in its constructor still builds and still passes every check. That
# is exactly what happened with ModCreativeTabs, which called get() on a registry object before
# the registry was populated. Only starting the game finds that class of bug.
#
# This starts the dedicated server (no window, no client). It writes eula.txt only when you
# opt in, because that records acceptance of the Minecraft EULA on your behalf:
#
#     GRINDLESS_ACCEPT_EULA=true tools/smoke-boot.sh      (CI=true also counts)
#
# Environment:
#     SMOKE_TIMEOUT   seconds to wait for the server to finish starting (default 900)
#     SMOKE_EXPECT_TIMEOUT  seconds to await scheduled scenario assertions (default 120)
#     SMOKE_PORT      server port (default 25599)
#     SMOKE_DATAPACK  a datapack directory to enable in the new world, to simulate other mods
#                     or to hold a smoke function (ADR-0061)
#     SMOKE_OPTIONAL_DATAPACK one additional directory, staged but explicitly disabled
#     SMOKE_COMMANDS  a file of console commands to run once the server is up, one per line.
#                     After Done, the script probes with `say SMOKE-READY` until the broadcast
#                     appears; Gradle can swallow the first FIFO writes. Each scenario line is
#                     then followed by a 1s pause so hoppers and belts can tick; a scenario that
#                     must not yield to block entities should be a datapack function.
#     SMOKE_EXPECT    extended regexes, one per line, that must each appear in the log
#     SMOKE_EXPECT_FILE  the same, read from a file
set -u

ROOT="$(cd "${1:-.}" && pwd)"
RUN_DIR="$ROOT/forge/run/smoke-server"
LOG="${SMOKE_LOG:-$ROOT/forge/run/smoke-boot.log}"
TIMEOUT="${SMOKE_TIMEOUT:-900}"
EXPECT_TIMEOUT="${SMOKE_EXPECT_TIMEOUT:-120}"
case "$TIMEOUT:$EXPECT_TIMEOUT" in
  *[!0-9:]*|:*|*:) echo "Smoke timeouts must be positive integers."; exit 1 ;;
esac
if [ "$TIMEOUT" -le 0 ] || [ "$EXPECT_TIMEOUT" -le 0 ]; then
  echo "Smoke timeouts must be positive."; exit 1
fi
PORT="${SMOKE_PORT:-25599}"
DATAPACK="${SMOKE_DATAPACK:-}"
OPTIONAL_DATAPACK="${SMOKE_OPTIONAL_DATAPACK:-}"
EXPECT="${SMOKE_EXPECT:-}"
COMMANDS="${SMOKE_COMMANDS:-}"
if [ -n "${SMOKE_EXPECT_FILE:-}" ]; then EXPECT="$(cat "$SMOKE_EXPECT_FILE")"; fi

resolve_smoke_datapack() {
  local path="$1" world="$2" source name links
  if [ ! -d "$path" ] || [ -L "$path" ]; then
    echo "Invalid smoke datapack '$path': expected a non-symlink directory." >&2; return 1
  fi
  source="$(cd "$path" && pwd -P)" || return 1
  name="$(basename "$source")"
  if [ ! -f "$source/pack.mcmeta" ] || [[ ! "$name" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
    echo "Invalid smoke datapack '$path': require pack.mcmeta and a portable pack name." >&2; return 1
  fi
  case "$source/" in "$world/"*)
    echo "Invalid smoke datapack '$path': overlaps the disposable smoke world." >&2; return 1;; esac
  case "$world/" in "$source/"*)
    echo "Invalid smoke datapack '$path': overlaps the disposable smoke world." >&2; return 1;; esac
  links="$(find "$source" -type l -print -quit)" || return 1
  if [ -n "$links" ]; then
    echo "Invalid smoke datapack '$path': symlinks are not supported." >&2; return 1
  fi
  printf '%s\n' "$source"
}

smoke_pack_properties() {
  local primary="$1" optional="$2"
  if [ -n "$primary" ]; then printf 'initial-enabled-packs=vanilla,file/%s\n' "$primary"; fi
  if [ -n "$optional" ]; then
    if [ -z "$primary" ]; then printf 'initial-enabled-packs=vanilla\n'; fi
    printf 'initial-disabled-packs=file/%s\n' "$optional"
  fi
}

PACK_NAME=""
OPTIONAL_PACK_NAME=""
[ -n "$DATAPACK" ] && PACK_NAME="$(basename "$DATAPACK")"
if [ -n "$OPTIONAL_DATAPACK" ]; then
  OPTIONAL_DATAPACK="$(resolve_smoke_datapack "$OPTIONAL_DATAPACK" "$RUN_DIR/smoke-world")" || exit 1
  OPTIONAL_PACK_NAME="$(basename "$OPTIONAL_DATAPACK")"
  if [ -n "$DATAPACK" ]; then
    DATAPACK="$(resolve_smoke_datapack "$DATAPACK" "$RUN_DIR/smoke-world")" || exit 1
    PACK_NAME="$(basename "$DATAPACK")"
    if [ "$(printf '%s' "$PACK_NAME" | tr '[:upper:]' '[:lower:]')" = \
         "$(printf '%s' "$OPTIONAL_PACK_NAME" | tr '[:upper:]' '[:lower:]')" ]; then
      echo "Smoke datapack name collision: primary and optional names must differ, ignoring case." >&2
      exit 1
    fi
  fi
fi

if [ "${GRINDLESS_ACCEPT_EULA:-}" != "true" ] && [ "${CI:-}" != "true" ]; then
  echo "Refusing to write eula.txt on your behalf."
  echo "Re-run with GRINDLESS_ACCEPT_EULA=true to accept the Minecraft EULA (https://aka.ms/MinecraftEULA)."
  exit 2
fi

cd "$ROOT" || exit 1
./gradlew --no-daemon --console=plain :forge:prepareSmokeServer ||
  { echo "SMOKE BOOT FAILED: could not prepare the production Forge server."; exit 1; }
FORGE_VERSION="$(sed -n 's/^forge_version=//p' "$ROOT/gradle.properties" | tail -1 | tr -d '\r')"
case "$FORGE_VERSION" in
  ""|*[!0-9A-Za-z._+-]*) echo "Invalid or missing forge_version."; exit 1 ;;
esac
LAUNCH_ARGS="$RUN_DIR/libraries/net/minecraftforge/forge/$FORGE_VERSION/unix_args.txt"
if [ ! -f "$LAUNCH_ARGS" ]; then echo "Forge did not produce $LAUNCH_ARGS"; exit 1; fi
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

mkdir -p "$RUN_DIR"
printf 'eula=true\n' > "$RUN_DIR/eula.txt"
printf 'online-mode=false\nserver-ip=127.0.0.1\nserver-port=%s\nlevel-name=smoke-world\nview-distance=2\nsimulation-distance=2\n' \
  "$PORT" > "$RUN_DIR/server.properties"
rm -rf "$RUN_DIR/smoke-world"
if [ -n "$DATAPACK" ]; then
  mkdir -p "$RUN_DIR/smoke-world/datapacks"
  cp -r "$DATAPACK" "$RUN_DIR/smoke-world/datapacks/$PACK_NAME" || exit 1
fi
if [ -n "$OPTIONAL_DATAPACK" ]; then
  mkdir -p "$RUN_DIR/smoke-world/datapacks"
  cp -r "$OPTIONAL_DATAPACK" "$RUN_DIR/smoke-world/datapacks/$OPTIONAL_PACK_NAME" || exit 1
fi
smoke_pack_properties "$PACK_NAME" "$OPTIONAL_PACK_NAME" >> "$RUN_DIR/server.properties"
: > "$LOG"

# The server reads commands from stdin; a FIFO lets this script send `stop` once it is up.
FIFO="$(mktemp -u /tmp/grindless-smoke.XXXXXX)"
mkfifo "$FIFO"
exec 3<>"$FIFO"
CONSOLE_OPEN=1

cd "$RUN_DIR" || exit 1
# setsid puts the server in its own process group, so cleanup can signal
# the whole tree at once. Git Bash on Windows has no setsid, and refusing to run there would
# make this script CI-only -- which is the opposite of what it is for, since a developer who
# can boot locally finds a loading crash before pushing. Fall back to signalling the launcher.
if command -v setsid >/dev/null 2>&1; then
  setsid "$JAVA" -Xmx2G "@$LAUNCH_ARGS" nogui < "$FIFO" 3>&- > "$LOG" 2>&1 &
  SERVER_PID=$!
  KILL_TARGET="-$SERVER_PID"
else
  "$JAVA" -Xmx2G "@$LAUNCH_ARGS" nogui < "$FIFO" 3>&- > "$LOG" 2>&1 &
  SERVER_PID=$!
  KILL_TARGET="$SERVER_PID"
fi

cleanup() {
  # The happy path has already sent `stop` and waited; this is the abort path.
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    if [ "$CONSOLE_OPEN" -eq 1 ]; then echo "stop" >&3 2>/dev/null; fi
    for _ in $(seq 1 15); do
      kill -0 "$SERVER_PID" 2>/dev/null || break
      sleep 1
    done
  fi
  exec 3>&-
  rm -f "$FIFO"
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    kill -TERM -- "$KILL_TARGET" 2>/dev/null
    sleep 2
    kill -KILL -- "$KILL_TARGET" 2>/dev/null
  fi
  rm -rf "$RUN_DIR/smoke-world"
}
trap cleanup EXIT

FAIL_PATTERN='Mod Loading has failed|Failed to create mod instance|Exception in server tick loop|Encountered an unexpected exception'
deadline=$((SECONDS + TIMEOUT))
while [ "$SECONDS" -lt "$deadline" ]; do
  if grep -Eq "$FAIL_PATTERN" "$LOG"; then
    echo "SMOKE BOOT FAILED: the server hit a fatal error while loading."
    grep -E -m3 -A8 "$FAIL_PATTERN" "$LOG"
    exit 1
  fi
  if grep -q 'Done (.*)! For help' "$LOG"; then
    grep -m1 'Done (.*)! For help' "$LOG"
    # Done is not "stdin is live". GitHub runs 37166092661 (ATLAS-OK) and 37167232051
    # (PICKAXE-OK hand_crank_dynamo) lost the first console lines; the same commits
    # passed on the other event. Probe until the server broadcasts SMOKE-READY.
    ready_deadline=$((SECONDS + 60))
    while [ "$SECONDS" -lt "$ready_deadline" ]; do
      if grep -E -q '\[Server\] SMOKE-READY' "$LOG"; then
        break
      fi
      echo "say SMOKE-READY" >&3
      sleep 1
    done
    if ! grep -E -q '\[Server\] SMOKE-READY' "$LOG"; then
      echo "SMOKE BOOT FAILED: the server started but never echoed SMOKE-READY"
      echo "stop" >&3
      exec 3>&-
      CONSOLE_OPEN=0
      exit 1
    fi
    if [ -n "$COMMANDS" ]; then
      while IFS= read -r command; do
        [ -z "$command" ] && continue
        case "$command" in \#*) continue ;; esac
        echo "$command" >&3
        sleep 1
      done < "$COMMANDS"
      sleep 2
    fi
    if [ -n "$EXPECT" ]; then
      expect_deadline=$((SECONDS + EXPECT_TIMEOUT))
      while [ "$SECONDS" -lt "$expect_deadline" ]; do
        missing=0
        while IFS= read -r pattern; do
          [ -z "$pattern" ] && continue
          grep -Eq "$pattern" "$LOG" || missing=1
        done <<< "$EXPECT"
        [ "$missing" -eq 0 ] && break
        kill -0 "$SERVER_PID" 2>/dev/null || break
        grep -Eq "$FAIL_PATTERN" "$LOG" && break
        sleep 1
      done
      while IFS= read -r pattern; do
        [ -z "$pattern" ] && continue
        if grep -Eq "$pattern" "$LOG"; then
          grep -E -m1 "$pattern" "$LOG"
        else
          echo "SMOKE BOOT FAILED: the server started but its log never matched: $pattern"
          echo "stop" >&3
          exit 1
        fi
      done <<< "$EXPECT"
    fi
    if grep -Eq "$FAIL_PATTERN" "$LOG"; then
      echo "SMOKE BOOT FAILED: the server hit a fatal error during the scenario."
      exit 1
    fi
    echo "stop" >&3
    for _ in $(seq 1 60); do
      kill -0 "$SERVER_PID" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "SMOKE BOOT FAILED: the server did not stop within 60 seconds."
      exit 1
    fi
    if ! wait "$SERVER_PID"; then
      echo "SMOKE BOOT FAILED: the server launcher exited unsuccessfully."
      exit 1
    fi
    if grep -Eq "$FAIL_PATTERN" "$LOG"; then
      echo "SMOKE BOOT FAILED: the server hit a fatal error before shutdown completed."
      exit 1
    fi
    echo "SMOKE BOOT PASSED"
    exit 0
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "SMOKE BOOT FAILED: the server process exited before finishing startup."
    tail -40 "$LOG"
    exit 1
  fi
  sleep 2
done

echo "SMOKE BOOT FAILED: no 'Done' after ${TIMEOUT}s."
tail -40 "$LOG"
exit 1
