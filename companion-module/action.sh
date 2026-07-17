#!/system/bin/sh
MODDIR=${0%/*}
echo "Sa2ration Companion (modo passivo; nada é executado no boot)"
"$MODDIR/bin/sa2rationctl" probe
echo
"$MODDIR/bin/sa2rationctl" health
