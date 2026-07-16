#!/system/bin/sh
MODDIR=${0%/*}
"$MODDIR/bin/sa2rationctl" probe
echo
"$MODDIR/bin/sa2rationctl" health
