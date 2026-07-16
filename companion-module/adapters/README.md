# Contrato de adaptadores

O módulo não grava em hardware sozinho. Um adaptador validado deve ser instalado em `adapters/active/` e fornecer `capabilities.properties`, executáveis opcionais `apply-lut1d`, `apply-lut3d`, `health` e `reset`, validação do dispositivo e restauração idempotente.

```properties
adapter.id=vendor-device-revision
gamma=supported
rgbGamma=supported
lut1d=supported
lut3d=unsupported
curves=supported
```

Um adaptador não deve ser distribuído como genérico se depender de nó sysfs ou serviço específico.
