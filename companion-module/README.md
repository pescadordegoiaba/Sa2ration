# Sa2ration Companion

Módulo passivo instalável em Magisk, KernelSU/SukiSU/ReSukiSU e APatch. Fornece diagnóstico e protocolo seguro sob demanda para adaptadores não lineares/vendor. Não executa serviço no boot.

```shell
./gradlew packageCompanionModule
```

Saída: `build/outputs/module/Sa2ration-Companion-1.1.0.zip`. O módulo base não anuncia gama ou LUT como suportada sem adaptador validado e explicitamente ativado para o dispositivo.
