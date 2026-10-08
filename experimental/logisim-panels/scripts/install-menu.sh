#!/bin/sh
set -eu
APPDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEST=${XDG_DATA_HOME:-"$HOME/.local/share"}/applications
mkdir -p "$DEST"
# Desktop Exec has its own escaping rules, distinct from shell quoting.
ESCAPED=$(printf '%s' "$APPDIR/start.sh" | sed 's/[\\`"$]/\\&/g;s/%/%%/g')
ICON=$(printf '%s' "$APPDIR/icon.png" | sed 's/\\/\\\\/g')
cat > "$DEST/logisim-panels.desktop" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Logisim Panels
Comment=Digital logic circuits and simulation
Exec="$ESCAPED" %F
Icon=$ICON
Terminal=false
Categories=Education;Electronics;
EOF
chmod 644 "$DEST/logisim-panels.desktop"
printf '%s\n' 'Logisim Panels is now available in the application menu.'
