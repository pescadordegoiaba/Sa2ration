# Arquitetura do Sa2ration Linux 2.0

## Escopo

O Sa2ration Linux 2.0 é uma implementação separada da edição Android. O
executável principal é escrito em C++20 e combina Qt 6 e Dear ImGui na mesma
janela. O processamento global de cor é executado por um plugin OpenGL do KWin.

Não existe root, módulo de kernel, DKMS, injeção em processos, alteração de
firmware ou escrita em `sysfs`. Todo o código roda com as permissões normais da
sessão gráfica do usuário.

## Componentes e limites de processo

| Componente | Processo | Responsabilidade |
|---|---|---|
| `sa2ration-linux` | aplicação do usuário | janela, controles, persistência, validação, D-Bus e recuperação |
| Qt Widgets | aplicação do usuário | janela, cabeçalho, status, diálogos, escala e integração com o desktop |
| Dear ImGui | aplicação do usuário | switch, sliders, inputs numéricos e botões do painel em tempo real |
| `sa2ration_gpu.so` | processo do KWin | redirecionamento das janelas e execução do fragment shader |
| KWin/RenderEngine | compositor | composição OpenGL e entrega da imagem para o pipeline de saída |

O frontend é um único ELF. Dear ImGui é compilado estaticamente dentro dele;
Qt permanece uma dependência dinâmica do sistema. O plugin precisa ser um
arquivo separado porque é carregado no espaço de processo do KWin e deve
implementar a ABI de efeitos do compositor.

## Fluxo de inicialização

```text
QApplication
  → MainWindow
  → GpuEffectController lê gpu.ini
  → QtDBus conecta em org.kde.KWin
  → isEffectLoaded(sa2ration_gpu)
      → false: loadEffect(sa2ration_gpu)
  → reconfigureEffect(sa2ration_gpu)
  → estado é apresentado ao ImGuiControlWidget
```

O usuário não precisa abrir as Configurações do Plasma. O carregamento ocorre
ao iniciar o programa. Se `Enabled=true` estiver persistido, o efeito começa a
processar a imagem assim que o KWin o reconfigura. Se `Enabled=false`, o plugin
fica carregado, mas não redireciona janelas nem altera pixels.

Uma sessão do KWin iniciada antes da primeira instalação pode ainda não ter
indexado o novo plugin. Nesse caso, sair e entrar novamente na sessão atualiza
o catálogo. Isso só é necessário após a instalação ou mudança de ABI, não a
cada execução.

## Composição Qt + Dear ImGui

`MainWindow` cria a estrutura Qt e hospeda um `ImGuiControlWidget`, derivado de
`QOpenGLWidget`. A cada frame:

1. o widget torna seu contexto OpenGL corrente;
2. atualiza tamanho, escala HiDPI e `DeltaTime` do ImGui;
3. inicia o backend OpenGL 3;
4. constrói os controles imediatos;
5. gera os draw lists;
6. renderiza no framebuffer do `QOpenGLWidget`;
7. o Qt compõe o widget com o restante da janela.

A ponte de plataforma traduz eventos Qt de mouse, roda, teclado, texto e foco
para a fila moderna de eventos do Dear ImGui. Não são criadas janelas GLFW ou
SDL auxiliares.

## Caminho de uma alteração

```text
slider/input ImGui
  → valuesEdited(GpuEffectValues)
  → debounce de 70 ms no MainWindow
  → normalização e classificação de risco
  → QSettings grava gpu.ini
  → reconfigureEffect via QtDBus
  → KWin lê KConfig
  → shader recebe novos uniforms
  → repaint global solicitado
```

O debounce evita uma chamada D-Bus para cada frame durante o arraste sem tirar
a sensação de ajuste imediato. Toda entrada é verificada com `std::isfinite` e
limitada antes de ser persistida.

## Persistência

O arquivo usado pelo frontend e pelo plugin é:

```text
~/.config/sa2ration-linux/gpu.ini
```

Schema atual:

```ini
[Effect]
Enabled=false
Brightness=1.0
Contrast=1.0
Saturation=1.0
Offset=0.0
StableEnabled=false
StableBrightness=1.0
StableContrast=1.0
StableSaturation=1.0
StableOffset=0.0
TemporaryUntilMs=0
```

`Enabled`, `Brightness`, `Contrast`, `Saturation` e `Offset` representam o
estado atual. As chaves `Stable*` guardam o último estado confirmado. Um valor
positivo em `TemporaryUntilMs` arma a recuperação dentro do próprio plugin.

## Classes principais

### `GpuEffectValues`

Modelo pequeno e copiável contendo o switch e os quatro parâmetros do shader.
Centraliza normalização e classificação de valores perigosos.

### `GpuEffectController`

Responsável por persistência, D-Bus, carregamento automático, reconfiguração,
confirmação, reversão e neutralização. Nenhuma chamada é feita por shell.

### `ImGuiControlWidget`

Mantém o contexto ImGui, adapta eventos Qt e desenha o painel. Sliders oferecem
faixas práticas; os inputs preservam a liberdade de valores até os limites
técnicos documentados.

### `MainWindow`

Conecta os dois toolkits, mantém o debounce e apresenta o diálogo Qt de 15
segundos. O diagnóstico mostra backend, carregamento e valores efetivos.

### `Sa2rationGpuEffect`

Implementa `KWin::OffscreenEffect`. Quando ativado, redireciona as janelas,
associa o shader, atualiza uniforms e solicita repaint. Quando desligado,
remove os redirecionamentos e deixa de participar da composição.

## ABI e empacotamento

Plugins binários do KWin incluem a versão da factory no IID. O artefato atual
é compilado para:

```text
org.kde.kwin.EffectPluginFactory6.7.2
```

Por isso o `PKGBUILD` depende de `kwin=6.7.2`. Depois de uma atualização do
KWin, o pacote deve ser recompilado. Forçar a instalação contra outra ABI pode
fazer o compositor recusar o plugin; não se deve contornar essa verificação.

## Código legado Python

`linux/src/` e `linux/tests/` preservam a implementação 1.x de KScreen,
Night Light, XRandR, DDC/CI e perfis como referência e cobertura de parsing.
Ela não é instalada pelo pacote 2.0. Esses backends serão portados
gradualmente para C++ sem voltar a introduzir um runtime Python no aplicativo
principal.
