# Sa2ration Companion

O Companion é um módulo root passivo compatível com o formato usado por Magisk, KernelSU/SukiSU/ReSukiSU e APatch. Ele contém o controlador sob demanda `sa2rationctl`, diagnóstico e protocolo de adaptadores. Não contém `service.sh` nem executa comandos durante o boot.

## Build e instalação

```shell
./gradlew packageCompanionModule
```

Instale `build/outputs/module/Sa2ration-Companion-1.1.0.zip` pelo gerenciador de root e reinicie. A reinicialização serve apenas para o gerenciador registrar o módulo; o Companion não executa script de inicialização. Em **Configurações → Backend**, use **Detectar novamente** para ver versão, adaptador e capacidades.

## Operações

```shell
su -c /data/adb/modules/sa2ration_companion/bin/sa2rationctl probe
su -c /data/adb/modules/sa2ration_companion/bin/sa2rationctl health
su -c /data/adb/modules/sa2ration_companion/bin/sa2rationctl reset
```

`apply-lut1d` e `apply-lut3d` validam presença e tamanho do arquivo e encaminham argumentos sem `eval`. O app serializa valores com `Locale.US`. Operações de adaptador possuem timeout; nenhuma é chamada no boot.

## Segurança

O módulo base retorna `unsupported` para gama/LUT. Um adaptador em `adapters/active` precisa declarar capacidades, fornecer executáveis próprios e possuir o arquivo `adapters/active/enabled`, criado conscientemente pelo usuário. Ele deve validar dispositivo/painel/revisão, salvar o valor anterior e implementar reset idempotente. Não há adaptador sysfs genérico.

O arquivo `/data/adb/sa2ration/companion-disable` bloqueia todos os adaptadores. O ZIP contém `skip_mount`, não instala arquivos em `/system`, não fornece política SELinux, não altera propriedades e não inicia daemon. O instalador remove hooks residuais da versão 1.0.0, e a desinstalação nunca chama adaptadores porque pode ser processada durante o boot. Essas medidas reduzem o risco do módulo base a um pacote inerte; um adaptador externo continua sendo código privilegiado e deve ser auditado separadamente.
