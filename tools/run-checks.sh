#!/usr/bin/env bash
# Linux/macOS port of tools/run-checks.ps1 (same logic, no PowerShell).
# Usage: tools/run-checks.sh [project-root]   (run `./gradlew :common:build` first)
set -u

ROOT="${1:-.}"
CLASSES="$ROOT/common/build/classes/java/main"
CHECK_DIR="$ROOT/tools/checks"
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"

if [ ! -d "$CLASSES" ]; then
  echo "No compiled classes at $CLASSES - run ./gradlew :common:build first."; exit 1
fi
mapfile -t SOURCES < <(find "$CHECK_DIR" -maxdepth 1 -name '*.java' | sort)
if [ "${#SOURCES[@]}" -eq 0 ]; then echo "No checks found in $CHECK_DIR."; exit 1; fi

# The Mojang-named, un-patched Minecraft jar that :common compiles against. It is identified by
# containing a Mojmap class (Direction) AND by not being one of the Forge-remapped variants.
MC_JAR=""
while IFS= read -r jar; do
  case "$jar" in *forge-1.20.1*|*intermediary*|*srg*) continue ;; esac
  if unzip -l "$jar" 2>/dev/null | grep -q 'net/minecraft/core/Direction.class'; then MC_JAR="$jar"; break; fi
done < <(find "$GRADLE_HOME_DIR/caches/fabric-loom/minecraftMaven" -name '*.jar' -size +5M 2>/dev/null)
if [ -z "$MC_JAR" ]; then echo "Could not locate a mapped Minecraft jar - run a build first."; exit 1; fi

DEPS="$(find "$GRADLE_HOME_DIR/caches/modules-2/files-2.1" -name '*.jar' \
        ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | paste -sd: -)"

OUT="$(mktemp -d /tmp/grindless-checks.XXXXXX)"
trap 'rm -rf "$OUT"' EXIT

CP_COMPILE="$CLASSES:$MC_JAR:$DEPS"
CP_RUN="$OUT:$CLASSES:$MC_JAR:$DEPS"

javac -nowarn -encoding UTF-8 -cp "$CP_COMPILE" -d "$OUT" "${SOURCES[@]}" || { echo "CHECKS DID NOT COMPILE"; exit 1; }

FAILED=0
for src in "${SOURCES[@]}"; do
  name="$(basename "$src" .java)"
  pkg="$(sed -n 's/^package \([A-Za-z0-9_.]*\);.*/\1/p' "$src" | head -1)"
  echo "--- $name ---"
  if [ -n "$pkg" ]; then fq="$pkg.$name"; else fq="$name"; fi
  java -cp "$CP_RUN" "$fq" 2>/dev/null || FAILED=$((FAILED + 1))
done

if [ "$FAILED" -gt 0 ]; then echo "$FAILED CHECK SUITE(S) FAILED"; exit 1; fi
echo "ALL CHECK SUITES PASSED"
