#!/usr/bin/env bash
set -euo pipefail
here="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
root="$(CDPATH= cd -- "$here/../../.." && pwd)"
compile=false
while [ "$#" -gt 0 ]; do
  case "$1" in
    --root) root="$(CDPATH= cd -- "$2" && pwd)"; shift 2 ;;
    --compile) compile=true; shift ;;
    *) echo "Unknown source-root test argument." >&2; exit 1 ;;
  esac
done
relative_sources=(
  tools/checks/VerifySustainability.java
  tools/checks/VerifySustainabilityAdapters.java
  tools/checks/VerifySustainabilityRegistry.java
  tools/checks/VerifySustainabilityT0.java
  tools/dashboard/DashboardExport.java
  tools/dashboard/tests/VerifyDashboardProjection.java
)
assertions=0
assert_root() {
  local message="$1"
  shift
  assertions=$((assertions + 1))
  if ! "$@"; then echo "$message" >&2; exit 1; fi
}
fixture_git() {
  local directory="$1"
  shift
  git --no-pager -c core.autocrlf=false -C "$directory" "$@"
}
mkdir -p "$root/build"
fixture="$(mktemp -d "$root/build/dr-XXXXXXXX")"
trap 'rm -rf -- "$fixture"' EXIT
caller="$fixture/caller A"
selected_root="$fixture/selected B"
for directory in "$caller" "$selected_root"; do
  for relative in "${relative_sources[@]}" tools/dashboard/export.sh; do
    mkdir -p "$(dirname -- "$directory/$relative")"
    cp -- "$root/$relative" "$directory/$relative"
  done
  printf 'build/\n' > "$directory/.gitignore"
  body="$(awk '
    /^sources=\(\)$/ { selected=1 }
    selected && /^"\$javac"/ { exit }
    selected { print }
  ' "$directory/tools/dashboard/export.sh")"
  assert_root "The actual source-selection boundary changed; update this focused test." test -n "$body"
  {
    printf '#!/usr/bin/env bash\nset -euo pipefail\nroot="$1"\n'
    printf 'here="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"\n'
    printf '%s\n' "$body"
    printf 'printf "%%s\\n" "${sources[@]}"\n'
  } > "$directory/tools/dashboard/source-selection-fixture.sh"
done
printf 'Caller A poison: this source must never be compiled for B.\n' > "$caller/tools/dashboard/DashboardExport.java"
printf 'Caller A poison: this test must never be compiled for B.\n' > "$caller/tools/dashboard/tests/VerifyDashboardProjection.java"
for directory in "$caller" "$selected_root"; do
  fixture_git "$directory" init --quiet
  fixture_git "$directory" add --all
  fixture_git "$directory" -c user.name='Copilot App' \
    -c user.email='223556219+Copilot@users.noreply.github.com' \
    commit --quiet -m 'Record a source-root selection fixture'
done
for state in divergent_clean_caller divergent_dirty_caller; do
  if [ "$state" = divergent_dirty_caller ]; then
    printf 'Dirty caller A must remain excluded.\n' >> "$caller/tools/dashboard/DashboardExport.java"
    printf 'Dirty caller A must remain excluded.\n' >> "$caller/tools/dashboard/tests/VerifyDashboardProjection.java"
    assert_root "Caller A must be genuinely dirty." test -n "$(fixture_git "$caller" status --porcelain --untracked-files=normal)"
  else
    assert_root "Caller A must begin genuinely clean." test -z "$(fixture_git "$caller" status --porcelain --untracked-files=normal)"
  fi
  assert_root "Selected B must remain genuinely clean; do not suppress its Git guard." \
    test -z "$(fixture_git "$selected_root" status --porcelain --untracked-files=normal)"
  selected="$(bash "$caller/tools/dashboard/source-selection-fixture.sh" "$selected_root")"
  sources=()
  while IFS= read -r source; do sources+=("$source"); done <<< "$selected"
  assert_root "The compiler source inventory changed." test "${#sources[@]}" -eq "${#relative_sources[@]}"
  for index in "${!relative_sources[@]}"; do
    expected="$selected_root/${relative_sources[$index]}"
    assert_root "Compiler source did not come from selected B: ${relative_sources[$index]}" test "${sources[$index]}" = "$expected"
    assert_root "Selected source bytes differ from B." test "$(git hash-object -- "${sources[$index]}")" = "$(git hash-object -- "$expected")"
  done
  for relative in tools/dashboard/DashboardExport.java tools/dashboard/tests/VerifyDashboardProjection.java; do
    assert_root "The cross-root fixture must actually contain divergent tools." \
      test "$(git hash-object -- "$caller/$relative")" != "$(git hash-object -- "$selected_root/$relative")"
  done
done
if "$compile"; then
  javac="${JAVA_HOME:+${JAVA_HOME}/bin/}javac"
  classpath="$(<"$root/common/build/checks/runtime-classpath.txt")"
  if [ -z "$classpath" ]; then echo "Use an already Gradle-resolved classpath; this test never runs Gradle." >&2; exit 1; fi
  classes="$selected_root/build/classes"
  mkdir -p "$classes"
  "$javac" -J-Xmx256m -J-XX:ActiveProcessorCount=2 -nowarn -encoding UTF-8 --release 17 \
    -cp "$classpath" -d "$classes" "${sources[@]}"
  assert_root "Selected-B exporter class is missing." test -f \
    "$classes/io/github/ezequiel24123z/grindless/sustainability/DashboardExport.class"
fi
assert_root "Compiling selected B must not dirty its tracked source." \
  test -z "$(fixture_git "$selected_root" status --porcelain --untracked-files=normal)"
printf 'DASHBOARD ROOT SELECTION: %s passed; Bash only; compile=%s; no production artifact, JVM audit or acceptance claim.\n' "$assertions" "$compile"
