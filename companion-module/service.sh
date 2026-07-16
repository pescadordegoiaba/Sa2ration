#!/system/bin/sh
MODDIR=${0%/*}
STATE_DIR=/data/adb/sa2ration/companion
mkdir -p "$STATE_DIR" "$MODDIR/adapters/active"
chmod 0700 "$STATE_DIR"
TMP="$STATE_DIR/capabilities.tmp"
OUT="$STATE_DIR/capabilities.properties"
if "$MODDIR/bin/sa2rationctl" probe >"$TMP" 2>"$STATE_DIR/last-error.log"; then
  mv -f "$TMP" "$OUT"
else
  rm -f "$TMP"
fi
