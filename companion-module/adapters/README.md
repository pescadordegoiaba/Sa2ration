# Contrato de adaptadores

O módulo não grava em hardware sozinho e não executa serviço no boot. Um adaptador validado deve ser instalado em `adapters/active/`, fornecer `capabilities.properties`, executáveis opcionais `apply-lut1d`, `apply-lut3d`, `health` e `reset`, validação do dispositivo e restauração idempotente.

O adaptador permanece desativado até o usuário criar explicitamente `adapters/active/enabled`. Para desativar todos os adaptadores sem remover o módulo, crie `/data/adb/sa2ration/companion-disable`.

```properties
adapter.id=vendor-device-revision
gamma=supported
rgbGamma=supported
lut1d=supported
lut3d=unsupported
curves=supported
```

Um adaptador não deve ser distribuído como genérico se depender de nó sysfs ou serviço específico. Ele não pode instalar scripts de boot, iniciar daemon, bloquear esperando serviço vendor nem modificar partições. Todas as operações recebem timeout do controlador.
