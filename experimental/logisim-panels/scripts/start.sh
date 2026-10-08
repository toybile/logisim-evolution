#!/bin/sh
set -eu
APPDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "$APPDIR/runtime/bin/java" --enable-native-access=ALL-UNNAMED \
  -cp "$APPDIR/app/interface.jar:$APPDIR/app/logisim-evolution-5.0.0-all.jar" \
  local.logisim.panels.Launcher "$@"
