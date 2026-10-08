#!/bin/sh
set -eu
APPDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "$APPDIR/runtime/bin/java" --enable-native-access=ALL-UNNAMED \
  -jar "$APPDIR/app/logisim-panels.jar" "$@"
