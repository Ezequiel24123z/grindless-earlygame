#!/usr/bin/env bash
# Linux/macOS twin of tools/run-smokes.ps1. Runs the scenarios in tools/smoke/scenarios.txt.
#
#     GRINDLESS_ACCEPT_EULA=true tools/run-smokes.sh .
#     tools/run-smokes.sh . --only states,first-exosuit
#     tools/run-smokes.sh . --shard 1 --of 6
#
# CI uses --shard/--of to spread 39 server boots across parallel jobs; one sequential job no
# longer fits in the timeout (ADR-0105). Scenario conventions are documented in scenarios.txt.
set -u

ROOT="."
ONLY=""
SHARD=0
OF=0
LIST="no"

while [ $# -gt 0 ]; do
  case "$1" in
    --only) ONLY="$2"; shift 2 ;;
    --shard) SHARD="$2"; shift 2 ;;
    --of) OF="$2"; shift 2 ;;
    --list) LIST="yes"; shift ;;
    *) ROOT="$1"; shift ;;
  esac
done

ROOT="$(cd "$ROOT" && pwd)"
SMOKE_DIR="$ROOT/tools/smoke"
RUN_DIR="$ROOT/forge/run"
REPORT="$RUN_DIR/smoke-report.html"
mkdir -p "$RUN_DIR"

ALL=()
DESCRIPTIONS=()
while IFS= read -r line; do
  # Git hands this file back with CRLF on a Windows checkout. Without stripping the CR a blank
  # line is "\r", which does not match '' and becomes a scenario with an empty name.
  line="${line%$'\r'}"
  case "$line" in ''|\#*) continue ;; esac
  ALL+=("$(printf '%s' "$line" | awk '{print $1}')")
  DESCRIPTIONS+=("$(printf '%s' "$line" | sed -E 's/^[^[:space:]]+[[:space:]]+//')")
done < "$SMOKE_DIR/scenarios.txt"

SELECTED=()
index=0
for name in "${ALL[@]}"; do
  if [ -n "$ONLY" ]; then
    case ",$ONLY," in *",$name,"*) ;; *) index=$((index + 1)); continue ;; esac
  fi
  if [ "$OF" -gt 0 ]; then
    # Round-robin rather than contiguous blocks: the long scenarios are not evenly spread
    # through the list, and striping keeps the shards closer to the same wall-clock time.
    if [ $((index % OF)) -ne $((SHARD - 1)) ]; then index=$((index + 1)); continue; fi
  fi
  SELECTED+=("$name")
  index=$((index + 1))
done

if [ "${#SELECTED[@]}" -eq 0 ]; then
  echo "No scenarios selected."
  exit 1
fi

if [ "$LIST" = "yes" ]; then
  for name in "${SELECTED[@]}"; do echo "$name"; done
  exit 0
fi

# STATUS[i] tracks each selected scenario so the report can be rewritten after every boot.
# A long run is otherwise a silent terminal; CI keeps the page as an artifact beside the logs.
STATUS=()
SECS=()
for _ in "${SELECTED[@]}"; do STATUS+=("pending"); SECS+=("0"); done

write_report() {
  local done_count=0 passed=0 failed=0 active="no" rows="" idx=0 pct=0
  for s in "${STATUS[@]}"; do
    case "$s" in
      passed) done_count=$((done_count + 1)); passed=$((passed + 1)) ;;
      failed) done_count=$((done_count + 1)); failed=$((failed + 1)) ;;
      *) active="yes" ;;
    esac
  done
  [ "${#SELECTED[@]}" -gt 0 ] && pct=$((100 * done_count / ${#SELECTED[@]}))

  local bar="live" headline="running"
  if [ "$failed" -gt 0 ]; then bar="bad"; headline="$failed failed"
  elif [ "$active" = "no" ]; then bar="good"; headline="all passed"; fi

  local refresh=""
  [ "$active" = "yes" ] && refresh='<meta http-equiv="refresh" content="3">'

  for name in "${SELECTED[@]}"; do
    local st="${STATUS[$idx]}" mark="" link="" time=""
    case "$st" in
      passed) mark="PASS"; link="<a href=\"smoke-$name.log\">log</a>" ;;
      failed) mark="FAIL"; link="<a href=\"smoke-$name.log\">log</a>" ;;
      running) mark="..." ;;
    esac
    [ "${SECS[$idx]}" -gt 0 ] && time="${SECS[$idx]}s"
    idx=$((idx + 1))
    rows="$rows<tr class=\"$st\"><td class=\"n\">$idx</td><td class=\"s\">$mark</td><td class=\"name\">$name</td><td class=\"t\">$time</td><td class=\"l\">$link</td></tr>"
  done

  cat > "$REPORT" <<HTML
<!doctype html><html lang="en"><head><meta charset="utf-8">$refresh
<title>Grindless smokes</title><style>
 :root { color-scheme: dark light; }
 body { font: 14px/1.5 ui-monospace, Consolas, monospace; margin:0; padding:24px; background:#11141a; color:#d7dde8; }
 h1 { font-size:18px; margin:0 0 4px; }
 .sub { color:#8b94a7; font-size:12px; margin-bottom:18px; }
 .bar { height:10px; border-radius:5px; background:#222733; overflow:hidden; margin-bottom:8px; }
 .bar > div { height:100%; width:${pct}%; }
 .bar.live > div { background:#4b9fd5; } .bar.good > div { background:#3fb950; } .bar.bad > div { background:#e5534b; }
 .stats { display:flex; gap:18px; font-size:12px; color:#8b94a7; margin-bottom:20px; }
 .stats b { color:#d7dde8; }
 table { border-collapse:collapse; width:100%; }
 td { padding:5px 10px; border-bottom:1px solid #1c212b; }
 .n { color:#5c6578; text-align:right; width:34px; } .s { width:46px; font-weight:700; }
 .name { width:190px; } .t { width:70px; color:#8b94a7; text-align:right; font-size:12px; } .l { font-size:12px; }
 a { color:#4b9fd5; }
 tr.passed .s { color:#3fb950; } tr.failed .s { color:#e5534b; }
 tr.failed .name { color:#e5534b; font-weight:700; }
 tr.running { background:#172030; } tr.running .s { color:#4b9fd5; }
 tr.pending .name { color:#4d5566; }
</style></head><body>
<h1>Grindless smoke scenarios &mdash; $headline</h1>
<div class="sub">Each row boots a headless Forge dedicated server (ADR-0049).</div>
<div class="bar $bar"><div></div></div>
<div class="stats"><span><b>$done_count</b> / ${#SELECTED[@]} done</span><span><b>$passed</b> passed</span><span><b>$failed</b> failed</span></div>
<table>$rows</table></body></html>
HTML
}

echo "Running ${#SELECTED[@]} smoke scenario(s)."
echo "Live report: $REPORT"
write_report

FAILED=()
i=0
for name in "${SELECTED[@]}"; do
  echo
  echo "=== [$((i + 1))/${#SELECTED[@]}] $name ==="
  STATUS[$i]="running"
  write_report
  started=$SECONDS

  unset SMOKE_COMMANDS SMOKE_EXPECT_FILE SMOKE_EXPECT SMOKE_DATAPACK
  [ -f "$SMOKE_DIR/$name.commands" ] && export SMOKE_COMMANDS="$SMOKE_DIR/$name.commands"
  [ -f "$SMOKE_DIR/$name.expect" ] && export SMOKE_EXPECT_FILE="$SMOKE_DIR/$name.expect"
  if [ -d "$SMOKE_DIR/$name-pack" ]; then
    export SMOKE_DATAPACK="$SMOKE_DIR/$name-pack"
  elif [ -d "$SMOKE_DIR/$name" ]; then
    export SMOKE_DATAPACK="$SMOKE_DIR/$name"
  fi
  export SMOKE_LOG="$ROOT/forge/run/smoke-$name.log"

  if "$ROOT/tools/smoke-boot.sh" "$ROOT"; then
    STATUS[$i]="passed"
  else
    STATUS[$i]="failed"
    FAILED+=("$name")
    echo "SCENARIO FAILED: $name (log: $SMOKE_LOG)"
  fi
  SECS[$i]=$((SECONDS - started))
  write_report
  i=$((i + 1))
done

write_report
echo
if [ "${#FAILED[@]}" -eq 0 ]; then
  echo "ALL ${#SELECTED[@]} SMOKE SCENARIOS PASSED"
  exit 0
fi
echo "${#FAILED[@]} SMOKE SCENARIO(S) FAILED"
for name in "${FAILED[@]}"; do echo "  $name"; done
exit 1
