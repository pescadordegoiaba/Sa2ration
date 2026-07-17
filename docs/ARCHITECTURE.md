# Arquitetura

## Camadas

- `ui`: Activity, fragments por categoria, ViewModel e widgets reutilizáveis.
- `domain`: `DisplayConfiguration`, `Matrix4`, estágios e pipeline.
- `backend`: contrato de display, capacidades e SurfaceFlinger.
- `shell` e `root`: comandos estruturados, sessão serializada, detecção e backends de root.
- `persistence` e `profile`: DataStore, migração legada e perfis JSON atômicos.
- `panel` e `manufacturer`: classificação por evidências e adaptadores conservadores.
- `recovery` e `tiles`: estado estável, rollback, boot e entradas de emergência.
- `lut` e `automation`: modelos independentes de backend.
- `companion-module`: módulo root empacotável e protocolo de adaptadores não lineares.

`MainActivity` somente hospeda toolbar, tabs e diálogo de confirmação. `DisplayViewModel` coordena estado e I/O fora da thread principal. `RootShellExecutor` mantém execução serializada, captura stdout/stderr/exit code/timeout/duração e fecha processos corretamente.

## Composição

`ColorTransformStage` declara ativação, linearidade, requisito de backend, neutralidade, validação e matriz. `ColorTransformPipeline` multiplica estágios habilitados na ordem documentada no README. `Matrix4` usa column-major, valida finitude e preserva a última linha afim exigida pelo SurfaceFlinger.

O switch desligado preserva o valor editado em `DisplayConfiguration`, mas o estágio retorna neutro ao não participar da composição. O master desligado força identidade e saturação nativa `1.0`.

## Persistência

`StoredApplicationState` possui estado atual, estável, seleção manual, contador de falhas e automações. `ConfigurationRepository` migra `info.properties` uma vez e grava JSON via AndroidX DataStore. Perfis usam `AtomicFile`, schema e formato próprios. Backups são validados e saneados antes de substituir o estado.

## Capacidade antes de comando

`BackendCapabilities` diferencia suporte real, preparado sem backend, não testado, indisponível, falha, Companion ausente e adaptador ausente. Adaptadores OEM nunca habilitam escrita somente por fabricante/modelo. Uma futura operação física deverá comprovar caminho, formato, intervalo, valor anterior e método de restauração.

## Interface adaptativa

Activities desenham edge-to-edge e aplicam `systemBars`, `displayCutout` e `ime` por `WindowInsetsCompat`. O conteúdo é centralizado até 840dp; controles numéricos usam sempre slider e input verticais em largura total para evitar colapso de medição em ROMs diferentes. Botões de ação também empilham em largura compacta.

As páginas principais mostram apenas editores funcionais. Modelos sem executor real — automação contínua, controles físicos OLED/LCD e escrita de resolução/refresh — são consolidados no painel de compatibilidade.
