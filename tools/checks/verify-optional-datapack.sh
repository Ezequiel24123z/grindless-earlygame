#!/usr/bin/env bash
# Executes production pack helpers and real runner dispatch with fake boots; no JVM.
set -euo pipefail
ROOT="$(cd "${1:-"$(dirname "$0")/../.."}" && pwd)"
BOOT="$ROOT/tools/smoke-boot.sh"
COUNT=0
assert() {
  if ! "$@"; then echo "Optional datapack contract failed: $*" >&2; exit 1; fi
  COUNT=$((COUNT + 1))
}
reject() {
  if "$@" > /dev/null 2>&1; then echo "Accepted invalid datapack: $*" >&2; exit 1; fi
  COUNT=$((COUNT + 1))
}
reject_message() {
  local expected="$1"; shift
  if "$@" > "$FIXTURE/rejected.log" 2>&1; then echo "Accepted invalid datapack: $*" >&2; exit 1; fi
  if ! grep -Fq "$expected" "$FIXTURE/rejected.log"; then cat "$FIXTURE/rejected.log" >&2; exit 1; fi
  COUNT=$((COUNT + 1))
}
# Load only the real pure helper definitions, never the boot/Gradle entrypoint.
eval "$(sed -n '/^resolve_smoke_datapack()/,/^}/p; /^smoke_pack_properties()/,/^}/p' "$BOOT")"
mkdir -p "$ROOT/build"
FIXTURE="$(mktemp -d "$ROOT/build/optional-pack-tests.XXXXXX")"
trap 'rm -rf -- "$FIXTURE"' EXIT
pack() {
  mkdir -p "$FIXTURE/$1"
  printf '{"pack":{"pack_format":15,"description":"test"}}\n' > "$FIXTURE/$1/pack.mcmeta"
  printf '%s\n' "$FIXTURE/$1"
}
PRIMARY="$(pack primary)"
OPTIONAL="$(pack optional)"
WORLD="$FIXTURE/forge/run/smoke-server/smoke-world"
assert test "$(smoke_pack_properties '' '')" = ''
assert test "$(smoke_pack_properties primary '')" = 'initial-enabled-packs=vanilla,file/primary'
assert test "$(smoke_pack_properties primary optional)" = $'initial-enabled-packs=vanilla,file/primary\ninitial-disabled-packs=file/optional'
assert test "$(smoke_pack_properties '' optional)" = $'initial-enabled-packs=vanilla\ninitial-disabled-packs=file/optional'
assert test "$(resolve_smoke_datapack "$PRIMARY" "$WORLD")" = "$PRIMARY"
assert test "$(resolve_smoke_datapack "$OPTIONAL" "$WORLD")" = "$OPTIONAL"
reject resolve_smoke_datapack "$FIXTURE/missing" "$WORLD"
reject resolve_smoke_datapack "$PRIMARY/pack.mcmeta" "$WORLD"
mkdir -p "$FIXTURE/no-metadata"
reject resolve_smoke_datapack "$FIXTURE/no-metadata" "$WORLD"
reject resolve_smoke_datapack ' ' "$WORLD"
reject resolve_smoke_datapack "$(pack 'bad,name')" "$WORLD"
reject resolve_smoke_datapack "$(pack 'bad primary')" "$WORLD"
reject resolve_smoke_datapack "$(pack forge/run/smoke-server/smoke-world/pack)" "$WORLD"
reject resolve_smoke_datapack "$(pack forge)" "$WORLD"
ln -s "$OPTIONAL" "$FIXTURE/alias"
reject resolve_smoke_datapack "$FIXTURE/alias" "$WORLD"
ln -s "$OPTIONAL/pack.mcmeta" "$PRIMARY/linked"
reject resolve_smoke_datapack "$PRIMARY" "$WORLD"
rm "$PRIMARY/linked"

# Invalid optional inputs must fail in the actual boot before EULA/Gradle/Java.
reject_message 'name collision' env SMOKE_DATAPACK="$PRIMARY" SMOKE_OPTIONAL_DATAPACK="$PRIMARY" bash "$BOOT" "$FIXTURE"
ALIAS="$(pack elsewhere/PRIMARY)"
reject_message 'name collision' env SMOKE_DATAPACK="$PRIMARY" SMOKE_OPTIONAL_DATAPACK="$ALIAS" bash "$BOOT" "$FIXTURE"
reject_message 'Invalid smoke datapack' env SMOKE_DATAPACK="$PRIMARY" SMOKE_OPTIONAL_DATAPACK="$FIXTURE/missing" bash "$BOOT" "$FIXTURE"
assert test ! -e "$FIXTURE/forge/run/smoke-server/eula.txt"
assert grep -Fq 'cp -r "$OPTIONAL_DATAPACK"' "$BOOT"
assert grep -Fq 'smoke_pack_properties "$PACK_NAME" "$OPTIONAL_PACK_NAME"' "$BOOT"
assert grep -Fq '"$JAVA" -Xmx2G ' "$BOOT"
assert grep -Fq '\[Server\] SMOKE-READY' "$BOOT"

mkdir -p "$FIXTURE/tools/smoke"
cp "$ROOT/tools/run-smokes.sh" "$FIXTURE/tools/run-smokes.sh"
printf 'alpha first\nbeta fails\ngamma last\n' > "$FIXTURE/tools/smoke/scenarios.txt"
for name in alpha beta gamma; do printf 'say %s\n' "$name" > "$FIXTURE/tools/smoke/$name.commands"; done
mkdir -p "$FIXTURE/tools/smoke/alpha-optional"
cat > "$FIXTURE/tools/smoke-boot.sh" <<'BOOT'
#!/usr/bin/env bash
name="$(basename "$SMOKE_COMMANDS" .commands)"
optional="${SMOKE_OPTIONAL_DATAPACK-}"
if [ -n "$optional" ]; then optional="$(basename "$optional")"; else optional='<unset>'; fi
printf '%s|%s\n' "$name" "$optional" >> "$1/calls.txt"
[ "$name" != beta ]
BOOT
chmod +x "$FIXTURE/tools/smoke-boot.sh"
(
  export SMOKE_OPTIONAL_DATAPACK=caller-sentinel
  trap 'printf "RESTORED=%s\n" "$SMOKE_OPTIONAL_DATAPACK"' EXIT
  source "$FIXTURE/tools/run-smokes.sh" "$FIXTURE" --only alpha,gamma
) > "$FIXTURE/dispatch.log" 2>&1
assert test "$(cat "$FIXTURE/calls.txt")" = $'alpha|alpha-optional\ngamma|<unset>'
assert grep -Fq RESTORED=caller-sentinel "$FIXTURE/dispatch.log"
rm "$FIXTURE/calls.txt"
reject env SMOKE_OPTIONAL_DATAPACK=caller-sentinel bash "$FIXTURE/tools/run-smokes.sh" "$FIXTURE" --only alpha,beta
assert test "$(cat "$FIXTURE/calls.txt")" = $'alpha|alpha-optional\nbeta|<unset>'
echo "ALL OPTIONAL DATAPACK CHECKS PASSED: $COUNT (no JVM or Minecraft)"
