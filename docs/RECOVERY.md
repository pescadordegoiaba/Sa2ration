# Recuperação

O aplicativo mantém configuração em edição e última configuração confirmada. Mudanças perigosas entram por 15 segundos; **Manter** confirma e **Reverter**, timeout ou saída restaura o último estado estável.

Reset atômico envia saturação `1.0`, matriz identidade, gerenciamento de cor neutro e persiste todos os estágios desligados/neutros. Valores físicos só serão restaurados no futuro quando o backend tiver salvo o valor anterior.

## Métodos

1. Tile **Reset Sa2ration**.
2. **Configurações → Restaurar display neutro**.
3. ADB shell/root protegido pela permissão de plataforma `DUMP`:

```shell
adb shell am broadcast -a com.xda.sa2ration.action.ADB_RESET_DISPLAY -n com.xda.sa2ration/.recovery.AdbResetReceiver
```

4. Bloquear a restauração no boot:

```shell
adb shell su -c 'mkdir -p /data/adb/sa2ration && touch /data/adb/sa2ration/disable'
```

ou:

```shell
adb shell su -c 'setprop persist.sa2ration.safe_mode 1'
```

Remova o arquivo ou defina a propriedade como `0` depois de recuperar a tela. O worker restaura apenas o estado estável e deixa de restaurar após três falhas consecutivas.

Como último recurso, reset direto do backend legado:

```shell
adb shell su -c 'setprop persist.sys.sf.color_saturation 1.0; service call SurfaceFlinger 1022 f 1.0; service call SurfaceFlinger 1015 i32 0'
```

Os códigos privados podem variar; prefira o tile ou receiver do aplicativo.
