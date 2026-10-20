#!/usr/bin/env bash
set -euo pipefail
here="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
java="${JAVA_HOME:+${JAVA_HOME}/bin/}java"
exec "$java" --add-modules jdk.httpserver "$here/DashboardTestServer.java" "$(dirname -- "$here")" "${1:-8877}"
