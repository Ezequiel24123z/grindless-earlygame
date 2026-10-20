#!/usr/bin/env bash
set -euo pipefail
here="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
root="$(CDPATH= cd -- "$here/../.." && pwd)"
output="docs/tiers/t0-economy.json"
action=write
offline=()
while [ "$#" -gt 0 ]; do
  case "$1" in
    --root) root="$(CDPATH= cd -- "$2" && pwd)"; shift 2 ;;
    --output) output="$2"; shift 2 ;;
    --check-only) action=check; shift ;;
    --offline) offline=(--offline); shift ;;
    *) echo "Unknown economy export argument." >&2; exit 1 ;;
  esac
done
java="${JAVA_HOME:+${JAVA_HOME}/bin/}java"
javac="${JAVA_HOME:+${JAVA_HOME}/bin/}javac"
version="$("$javac" -version 2>&1)"
case "$version" in "javac 17"|"javac 17."*) ;; *) echo "Economy export requires JDK 17." >&2; exit 1 ;; esac
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$root/.gradle/user-home}"
cd "$root"
"$root/gradlew" --no-daemon --quiet --console=plain --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx768m -XX:ActiveProcessorCount=2' "${offline[@]}" :common:writeCheckClasspath
classpath="$(<"$root/common/build/checks/runtime-classpath.txt")"
if [ -z "$classpath" ]; then echo "Gradle exported an empty classpath." >&2; exit 1; fi
mkdir -p "$root/build"
out="$root/build/economy-tools.${BASHPID:-$$}.$RANDOM"
(umask 077; mkdir -- "$out")
trap 'rm -rf -- "$out"' EXIT
sources=()
for name in VerifySustainability VerifySustainabilityAdapters VerifySustainabilityRegistry VerifySustainabilityT0 VerifyResourceGuidance VerifyEconomy; do
  sources+=("$root/tools/checks/$name.java")
done
sources+=("$root/tools/economy/EconomyExport.java")
"$javac" -J-Xmx256m -J-XX:ActiveProcessorCount=2 -nowarn -encoding UTF-8 --release 17 \
  -cp "$classpath" -d "$out" "${sources[@]}"
"$java" -Xmx256m -XX:ActiveProcessorCount=2 --add-modules jdk.compiler -cp "$out:$classpath" \
  io.github.ezequiel24123z.grindless.sustainability.EconomyExport "$root" "$output" "$action"
