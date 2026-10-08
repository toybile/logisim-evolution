#!/bin/sh
set -eu
APPDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "$APPDIR/start.sh" "$APPDIR/examples/Exemplo.circ"
