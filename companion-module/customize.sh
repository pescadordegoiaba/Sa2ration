#!/system/bin/sh
SKIPMOUNT=true
PROPFILE=false
POSTFSDATA=false
LATESTARTSERVICE=false
ui_print "- Instalando Sa2ration Companion"
ui_print "- Módulo passivo: nenhum script será executado no boot"
ui_print "- Nenhum nó sysfs será gravado pelo módulo base"
ui_print "- Adaptadores exigem ativação manual explícita"
# Remove hooks that could remain after an in-place update from Companion 1.0.0.
rm -f "$MODPATH/service.sh" "$MODPATH/post-fs-data.sh" "$MODPATH/system.prop" "$MODPATH/sepolicy.rule"
rm -rf "$MODPATH/system"
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/bin/sa2rationctl" 0 0 0755
