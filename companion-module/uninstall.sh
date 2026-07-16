#!/system/bin/sh
MODDIR=${0%/*}
if [ -x "$MODDIR/adapters/active/reset" ]; then
  "$MODDIR/adapters/active/reset" >/dev/null 2>&1
fi
rm -rf /data/adb/sa2ration/companion
