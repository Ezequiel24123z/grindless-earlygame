#!/usr/bin/env bash
# Source-only twin of verify-blueprint-fixture.ps1; no JVM, server or files written.
set -euo pipefail
ROOT="$(cd "${1:-"$(dirname "$0")/../.."}" && pwd)"
SMOKE="$ROOT/tools/smoke"
PACK="$SMOKE/first-blueprint-pack"

normalize_commands() {
  printf '%s\n' "$1" | sed -e 's/\r$//' -e 's/^[[:space:]]*//' \
    -e 's/[[:space:]]*$//' -e '/^#/d' -e '/^$/d'
}

# The existing exosuit oracle remains untouched. Only three items/markers are removed.
convert_exosuit_reference() {
  local text="$1" line item selector result=''
  while IFS= read -r line; do
    for item in flux_exosuit_chestplate network_tap exoskeleton_legs; do
      selector=' if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:'"$item"'",Count:1b}}]'
      line="${line//"$selector"/}"
    done
    case "$line" in
      *chest_spawn*|*tap_spawn*|*legs_spawn*|*flux_exosuit_chestplate*|*network_tap*|*exoskeleton_legs*|*EXO-CHEST-OK*|*TAP-OK*|*LEGS-MOD-OK*|*DIAG-SUMMON-CHEST-*|*DIAG-SUMMON-TAP-*|*DIAG-SUMMON-LEGS-*|*DIAG-ENTITY-CHEST-*|*DIAG-ENTITY-TAP-*|*DIAG-ENTITY-LEGS-*) continue;;
    esac
    line="${line//four once-summoned registered exosuit items/one once-summoned registered blueprint tool}"
    line="${line//grindless_exosuit/grindless_blueprint}"
    line="${line//exosuit_check/blueprint_check}"
    line="${line//helmet_spawn/tool_spawn}"
    line="${line//flux_exosuit_helmet/blueprint_tool}"
    line="${line//EXO-HELMET-OK/BLUEPRINT-OK}"
    line="${line//EXOSUIT/BLUEPRINT}"
    line="${line//HELMET/TOOL}"
    line="${line//first-exosuit-pack/first-blueprint-pack}"
    result+="$line"$'\n'
  done <<< "$text"
  printf '%s' "${result%$'\n'}"
}

FUNCTIONS=(begin spawn poll passed failed cleanup)
PATHS=(first-blueprint.commands first-blueprint.expect first-blueprint-pack/pack.mcmeta)
for name in "${FUNCTIONS[@]}"; do
  PATHS+=("first-blueprint-pack/data/grindless_blueprint/functions/$name.mcfunction")
done
ACTUAL=()
EXPECTED=()
# Freeze both sources once; the mutation cases never modify the files on disk.
for path in "${PATHS[@]}"; do
  reference="${path//first-blueprint/first-exosuit}"
  reference="${reference//grindless_blueprint/grindless_exosuit}"
  ACTUAL+=("$(normalize_commands "$(<"$SMOKE/$path")")")
  EXPECTED+=("$(convert_exosuit_reference "$(normalize_commands "$(<"$SMOKE/$reference")")")")
done
inventory=''
while IFS= read -r path; do
  inventory+="${path#"$PACK/"}"$'\n'
done < <(find "$PACK" -type f | LC_ALL=C sort)
ACTUAL+=("${inventory%$'\n'}")
EXPECTED+=("$(
  for name in "${FUNCTIONS[@]}"; do printf 'data/grindless_blueprint/functions/%s.mcfunction\n' "$name"; done
  printf 'pack.mcmeta\n'
)" )
EXPECTED[9]="$(printf '%s\n' "${EXPECTED[9]}" | LC_ALL=C sort)"
if [ -e "$SMOKE/first-blueprint.timeout" ]; then ACTUAL+=(present); else ACTUAL+=(absent); fi
EXPECTED+=(absent)

test_snapshot() {
  local source=("$@") i
  [ "${#source[@]}" -eq "${#EXPECTED[@]}" ] || return 1
  for ((i = 0; i < ${#EXPECTED[@]}; i++)); do
    [ "${source[$i]}" = "${EXPECTED[$i]}" ] || return 1
  done
}

LABELS=('console entry' 'original marker' 'pack metadata' begin 'one-shot spawn'
  'loaded bounded poll' 'observed success' 'console diagnostics' 'scoped cleanup' 'pack inventory' 'no timeout override')
for ((i = 0; i < ${#EXPECTED[@]}; i++)); do
  if [ "${ACTUAL[$i]}" != "${EXPECTED[$i]}" ]; then printf 'Source contract failed: %s\n' "${LABELS[$i]}" >&2; exit 1; fi
  printf '  ok   %s\n' "${LABELS[$i]}"
done
CRLF=()
for text in "${ACTUAL[@]}"; do CRLF+=("$(normalize_commands "${text//$'\n'/$'\r\n'}")"); done
test_snapshot "${CRLF[@]}" || { printf 'CRLF normalization changed the source contract.\n' >&2; exit 1; }
printf '  ok   CRLF input\n'

NEGATIVES=0
reject_mutation() {
  local name="$1" index="$2" old="$3" replacement="$4" mutated=("${ACTUAL[@]}")
  if [[ "${mutated[$index]}" != *"$old"* ]]; then printf 'Stale mutation: %s\n' "$name" >&2; exit 1; fi
  mutated[$index]="${mutated[$index]//"$old"/"$replacement"}"
  if test_snapshot "${mutated[@]}"; then printf 'Accepted invalid source: %s\n' "$name" >&2; exit 1; fi
  printf '  reject %s\n' "$name"
  NEGATIVES=$((NEGATIVES + 1))
}
OLD_CONSOLE='forceload add 0 0
say WAIT-CHUNK
summon minecraft:item 8 100 8 {Item:{id:"grindless:blueprint_tool",Count:1b}}
execute if entity @e[type=item,nbt={Item:{id:"grindless:blueprint_tool"}}] run say BLUEPRINT-OK
kill @e[type=item]'
reject_mutation 'original one-shot console' 0 "${ACTUAL[0]}" "$OLD_CONSOLE"
reject_mutation 'no loaded barrier' 5 'if loaded 8 100 8 ' ''
reject_mutation 'unverified count' 5 ',Count:1b' ''
reject_mutation 'wrong item ID' 5 'grindless:blueprint_tool' 'minecraft:stick'
reject_mutation 'wrong entity type' 5 'type=minecraft:item' 'type=minecraft:armor_stand'
reject_mutation 'repeated summon' 5 'scoreboard players add tries blueprint_check 1' $'scoreboard players add tries blueprint_check 1\nfunction grindless_blueprint:spawn'
reject_mutation 'unowned summon' 4 'Tags:["grindless_blueprint_smoke"],' ''
reject_mutation 'incidental hazards' 4 'NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,' ''
reject_mutation 'summon success is not observation' 5 'if score observed blueprint_check matches 1' 'if score tool_spawn blueprint_check matches 1'
reject_mutation 'false marker on failure' 7 'say BLUEPRINT-FAIL-CHUNK-OR-ENTITY-TIMEOUT' $'say BLUEPRINT-FAIL-CHUNK-OR-ENTITY-TIMEOUT\nsay BLUEPRINT-OK'
reject_mutation 'unbounded polling' 5 'if score tries blueprint_check matches ..159 ' ''
reject_mutation 'longer polling interval' 5 ':poll 5t' ':poll 20t'
reject_mutation 'global item cleanup' 8 'kill @e[type=minecraft:item,tag=grindless_blueprint_smoke]' 'kill @e[type=item]'
reject_mutation 'global forceload cleanup' 8 'forceload remove 0 0' 'forceload remove all'
reject_mutation 'silent failure feedback' 7 'execute if loaded 8 100 8 run say BLUEPRINT-DIAG-CHUNK-LOADED' 'data get entity @e[tag=grindless_blueprint_smoke,limit=1] Item'
reject_mutation 'outer timeout override' 10 absent present
reject_mutation 'changed expectation' 1 BLUEPRINT-OK SMOKE-READY
reject_mutation 'extra pack function' 9 pack.mcmeta $'pack.mcmeta\ndata/grindless_blueprint/functions/bypass.mcfunction'
printf 'ALL BLUEPRINT FIXTURE SOURCE CHECKS PASSED: 12 positive, %s negative (NOT A MINECRAFT RUN)\n' "$NEGATIVES"
