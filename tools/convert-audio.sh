#!/usr/bin/env bash
# Converts the generated WAVs in tools/audio to mono Ogg Vorbis under the mod's sounds directory.
# Usage: tools/convert-audio.sh [project-root]
#
# Minecraft plays only Ogg Vorbis, and only a MONO file is positioned in 3D: a stereo file is
# played flat, with no direction and no distance falloff (ADR-0048). So every file is forced to
# one channel here, and a check (VerifyAssets) rejects anything else. Needs ffmpeg with libvorbis,
# which is why this is a separate step from tools/generate-assets.sh, which needs only a JDK.
set -euo pipefail

ROOT="${1:-.}"
SRC="$ROOT/tools/audio"
DST="$ROOT/common/src/main/resources/assets/grindless/sounds"
mkdir -p "$DST"

for wav in "$SRC"/*.wav; do
  name="$(basename "$wav" .wav)"
  # bitexact keeps the container free of random serial numbers, so converting twice gives the
  # same bytes and a reconversion never shows up as a spurious diff.
  ffmpeg -y -v error -i "$wav" -ac 1 -ar 44100 -map_metadata -1 \
    -fflags +bitexact -flags:a +bitexact -c:a libvorbis -q:a 4 "$DST/$name.ogg"
  echo "converted $name"
done
