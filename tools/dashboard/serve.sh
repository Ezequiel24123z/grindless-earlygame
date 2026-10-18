#!/usr/bin/env bash
set -euo pipefail
here="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
java="${JAVA_HOME:+${JAVA_HOME}/bin/}java"
exec "$java" -Xmx128m -XX:ActiveProcessorCount=2 --add-modules jdk.httpserver \
  "$here/DashboardServer.java" "${1:-$here/../../build/dashboard/site}" "${2:-8878}"
