# Auditoria funcional

## Funciona pelo aplicativo, sem Companion

| Área | Estado | Observação |
| --- | --- | --- |
| Saturação global | Implementada | Transação legada SurfaceFlinger 1022; validar na ROM |
| Matriz e controles lineares | Implementados | Transação legada 1015; inclui contraste, RGB, brilho digital, níveis, temperatura, tint, matiz, grayscale, inversão, filtros e matriz personalizada |
| Gerenciamento de cor | Implementado | Transação legada 1023; pode variar em ROM OEM |
| Perfis e persistência | Implementados | DataStore + JSON/AtomicFile |
| Aplicação temporária e rollback | Implementados | Janela de 15 segundos para configurações perigosas |
| Boot seguro e reset | Implementados | WorkManager aplica somente estado estável; tile/receiver restauram neutro |
| Detecção de root/painel | Implementada por heurística | Não transforma inferência em suporte de hardware |

## Exige Companion e adaptador compatível

Gama real, gama RGB, curvas, LUT 1D e LUT 3D precisam de um backend não linear específico. Instalar somente o Companion não habilita esses controles: o adaptador precisa ser compatível com o aparelho, declarar a capacidade e possuir o marcador `adapters/active/enabled`.

## Preparado, mas não funcional nesta versão

| Área | O que falta |
| --- | --- |
| Automação contínua | Observadores de foreground, horário, sensores e eventos; o motor de prioridade isolado está testado |
| Refresh rate, resolução e DPI | Executor real, validação por dispositivo e rollback |
| HBM, DC dimming, PWM, CABC e MEMC | Backend oficial/vendor confirmado |
| Recursos físicos OLED/LCD | Interface segura do fabricante e restauração do valor anterior |
| Backends Samsung/Qualcomm/MediaTek/Exynos/OEM | Implementação de escrita validada; atualmente são modelos/detecção |
| UI completa de LUT/curvas | Importador/editor conectado ao adaptador; os modelos e protocolo existem |

Essas áreas não aparecem como controles acionáveis. O painel Compatibilidade informa que estão preparadas ou indisponíveis.

## Problemas corrigidos na revisão

- abas duplicadas ou compostas somente por placeholders foram consolidadas;
- seleção de backend OEM sem efeito foi removida da interface;
- automações deixaram de ser marcadas incorretamente como dependentes do Companion;
- temperatura RGB manual e luminância personalizada deixaram de ser oferecidas sem editor completo;
- gerenciamento de cor voltou a ter switch visível;
- aplicação de perfil agora força a UI a refletir o perfil ativo;
- criação de perfil passa a persistir o novo identificador ativo;
- matriz colada rejeita NaN, infinito e última linha incompatível;
- rollback ignora valores extremos preservados em estágios desligados;
- comandos SurfaceFlinger tentam reset neutro se uma etapa falhar;
- classificação de painel usa a evidência mais forte, sem sobreposição por token genérico;
- regras booleanas inválidas não casam silenciosamente e horários podem cruzar meia-noite.

## Segurança do Companion 1.1.0

O módulo base é passivo: não contém `service.sh`, `post-fs-data.sh`, `system.prop`, `sepolicy.rule` ou overlay `system/`. Ele inclui `skip_mount`, não executa adaptadores no boot/desinstalação, exige ativação explícita e aplica timeout às operações sob demanda. O build executa `verifyCompanionModuleSafety` e falha se esses invariantes forem quebrados.

Isso elimina hooks de boot do módulo base, mas não permite prometer risco zero para um adaptador externo ou falha do próprio gerenciador root. Todo adaptador continua sendo código privilegiado e deve ser auditado separadamente.
