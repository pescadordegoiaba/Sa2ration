# Sa2ration Companion

O Companion é um módulo root compatível com o formato usado por Magisk, KernelSU/SukiSU/ReSukiSU e APatch. Ele contém serviço de boot, controlador `sa2rationctl`, diagnóstico e protocolo de adaptadores.

## Build e instalação

```shell
./gradlew packageCompanionModule
```

Instale `build/outputs/module/Sa2ration-Companion-1.0.0.zip` pelo gerenciador de root e reinicie. Em **Configurações → Backend**, use **Detectar novamente** para ver versão, adaptador e capacidades.

## Operações

```shell
su -c /data/adb/modules/sa2ration_companion/bin/sa2rationctl probe
su -c /data/adb/modules/sa2ration_companion/bin/sa2rationctl health
su -c /data/adb/modules/sa2ration_companion/bin/sa2rationctl reset
```

`apply-lut1d` e `apply-lut3d` validam presença e tamanho do arquivo e encaminham argumentos sem `eval`. O app serializa valores com `Locale.US`.

## Segurança

O módulo base retorna `unsupported` para gama/LUT. Um adaptador em `adapters/active` precisa declarar capacidades e fornecer executáveis próprios. Ele deve validar dispositivo/painel/revisão, salvar o valor anterior e implementar reset idempotente. Não há adaptador sysfs genérico.
