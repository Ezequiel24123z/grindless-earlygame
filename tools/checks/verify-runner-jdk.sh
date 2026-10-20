#!/usr/bin/env bash
# Run on a Bash host; this is not Windows execution evidence.
set -eu
ROOT="$(cd "${1:-"$(dirname "$0")/../.."}" && pwd)"
cd "$ROOT"
eval "$(sed -n '/^is_javac17() {$/,/^}$/p' tools/run-checks.sh)"
declare -F is_javac17 >/dev/null
mkdir -p "$ROOT/build"
OUT="$(mktemp -d "$ROOT/build/runner-jdk-probe.XXXXXX")"
trap 'status=$?; if [ "$status" -eq 0 ]; then rm -rf -- "$OUT"; else printf "Evidence: %s\n" "$OUT" >&2; fi' EXIT
cat > "$OUT/javac" <<'FIXTURE'
#!/usr/bin/env bash
printf 'fixture-stdout:%s\n' "$GRINDLESS_JAVAC_PROBE_CASE"
printf 'fixture-stderr:%s\n' "$GRINDLESS_JAVAC_PROBE_CASE" >&2
case "$GRINDLESS_JAVAC_PROBE_CASE" in
  stdout) printf 'javac 17.0.20.1\n';;
  stderr) printf 'javac 17.0.20.1\n' >&2;;
  banner) printf 'Picked up JAVA_TOOL_OPTIONS: -Xmx256m -XX:ActiveProcessorCount=2\njavac 17.0.20.1\n' >&2;;
  wrong) printf 'javac 21.0.1\ndiagnostic mentions 17\n';;
  missing) printf 'a compiler mentioning 17 is not a version\n';;
  nonzero) printf 'javac 17.0.20.1\n'; exit 7;;
  ambiguous) printf 'javac 17.0.20.1\njavac 21.0.1\n';;
  suffix) printf 'javac 17.invalid\n';;
  flood) printf '%0131072d\n' 0; printf '%0131072d\n' 0 >&2; printf 'javac 17.0.20.1\n';;
  *) exit 99;;
esac
FIXTURE
chmod +x "$OUT/javac"
export JAVA_TOOL_OPTIONS='-Xmx256m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC'
is_javac17 javac 2>"$OUT/real-jdk17.txt"
grep -q 'Picked up JAVA_TOOL_OPTIONS:' "$OUT/real-jdk17.txt"
grep -q '^javac 17\.' "$OUT/real-jdk17.txt"
ASSERTIONS=3
for CASE in stdout stderr banner wrong missing nonzero ambiguous suffix flood; do
  export GRINDLESS_JAVAC_PROBE_CASE="$CASE"
  EXPECTED=1
  case "$CASE" in stdout|stderr|banner|flood) EXPECTED=0;; esac
  if is_javac17 "$OUT/javac" 2>"$OUT/$CASE.txt"; then ACTUAL=0; else ACTUAL=$?; fi
  [ "$ACTUAL" -eq "$EXPECTED" ]
  grep -q "fixture-stdout:$CASE" "$OUT/$CASE.txt"
  grep -q "fixture-stderr:$CASE" "$OUT/$CASE.txt"
  ASSERTIONS=$((ASSERTIONS + 3))
  if [ "$CASE" = nonzero ]; then
    grep -q 'exited with code 7' "$OUT/$CASE.txt"
    ASSERTIONS=$((ASSERTIONS + 1))
  fi
done
printf 'ALL RUNNER JDK PROBE CHECKS PASSED: %s\n' "$ASSERTIONS"
