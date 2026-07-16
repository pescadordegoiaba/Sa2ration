# Sa2ration Advanced

Painel avançado de imagem e display para Android com root. Esta versão preserva a saturação, o contraste, o gerenciamento de cor e a restauração no boot do Sa2ration, mas substitui a lógica monolítica por um pipeline de matrizes 4×4, MVVM, persistência versionada, detecção de ambiente e recuperação automática.

> O backend atual realmente aplica matriz, saturação global e gerenciamento de cor pelas transações legadas `1015`, `1022` e `1023` do SurfaceFlinger. Elas são interfaces privadas e variam entre ROMs. Controles sem backend comprovado aparecem como indisponíveis, não testados, experimentais ou “requer módulo”; o aplicativo não tenta adivinhar comandos sysfs.

## O que funciona nesta versão

- saturação e contraste global e por canal, com slider prático até `10×` e entrada manual ampla;
- brilho digital global/RGB, nível de preto, nível de branco, ganho e offset RGB;
- temperatura de cor, tint verde–magenta, rotação de matiz e misturador 3×3;
- grayscale Rec. 601/709/2020/média, inversão global/por canal e filtros lineares;
- matriz personalizada 4×4 e exibição/cópia da matriz final composta;
- switches individuais: desligar um estágio o remove do pipeline e restaura seu efeito neutro;
- confirmação de mudanças perigosas por 15 segundos, último estado estável e rollback;
- detecção heurística de LCD/OLED/AMOLED e seleção manual apenas para organizar a interface;
- detecção funcional de root e classificação de Magisk, KernelSU, SukiSU Ultra, ReSukiSU e APatch;
- perfis padrão e personalizados em JSON versionado, com importação/exportação;
- tiles de liga/desliga, próximo perfil e reset de emergência;
- restauração segura no boot usando WorkManager;
- relatório diagnóstico TXT/JSON e tela de configurações com Root, Painel, Backend, Segurança, Persistência, Inicialização, Diagnóstico, Logs e Sobre;
- Material 3 edge-to-edge com insets de status bar, navbar, recorte e teclado;
- controles responsivos para tela compacta e fonte ampliada;
- padrões SMPTE, grayscale, clipping, gradiente, OLED e LCD;
- módulo complementar instalável para Magisk, KernelSU/SukiSU/ReSukiSU e APatch.

## Interface

As abas são: **Simples**, **Cor**, **RGB**, **Tela**, **OLED**, **LCD**, **Acessibilidade**, **Perfis**, **Automação**, **Compatibilidade** e **Avançado**. OLED/LCD são exibidas conforme detecção ou seleção manual. Cada página usa `NestedScrollView`; recursos avançados usam cards com descrição, switch, controles e reset. Desligar o switch oculta os modificadores e recompõe a matriz sem o estágio.

A aba Simples mantém somente liga/desliga geral, saturação, contraste, brilho digital, temperatura, perfil, Aplicar, Resetar, Antes/Depois e backend.

## Pipeline linear

A ordem é fixa e testada:

```text
identidade → temperatura → tint → ganho RGB → offset RGB
→ misturador → matiz → saturação RGB → contraste global/RGB
→ brilho digital → nível de preto → nível de branco
→ grayscale → inversão → filtro → matriz personalizada
```

A saturação global continua na transação nativa `1022`. As demais transformações lineares são multiplicadas corretamente em formato column-major para a transação `1015`. Com o master ou todos os efeitos desligados, o resultado é identidade e saturação `1.0`.

Gama real, curvas e LUT não são falsamente aproximadas por matriz. O Sa2ration Companion fornece protocolo e serviço root, mas só anuncia/aplica uma operação quando existe um adaptador validado para o hardware.

## Root e painel

Root só é considerado funcional quando `su -c id -u` conclui, sem timeout, e stdout é exatamente `0`. `su -v`, `su -V`, executáveis, propriedades, mounts, daemons e strings conhecidas formam evidências com confiança; a preferência manual nunca transforma um acesso não funcional em funcional.

O tipo do painel é inferido por `DisplayManager`, modos do display, propriedades, dumpsys e caminhos DRM/backlight/device-tree lidos via root. Nome de painel, existência de backlight e tokens específicos possuem pesos; a marca sozinha não classifica o painel. A seleção manual só muda as categorias visuais — comandos físicos ainda exigem capacidade confirmada.

Detalhes: [arquitetura](docs/ARCHITECTURE.md), [módulo complementar](docs/COMPANION_MODULE.md), [detecção de root](docs/ROOT_DETECTION.md), [detecção de painel](docs/PANEL_DETECTION.md), [compatibilidade](docs/COMPATIBILITY.md) e [recuperação](docs/RECOVERY.md).

## Perfis e automação

Os 16 presets são Neutro, Natural, sRGB, Vívido, AMOLED, LCD, Filme, Fotografia, Leitura, Noturno, Jogos, FPS, Baixa luminosidade, Escala de cinza, Economia OLED e Externo. O documento armazena configuração, painel, backend, modo de cor, brilho/taxa opcionais, automações, criação e versão.

O motor de prioridades para aplicação, horário, dia, luz, bateria, carregamento, economia, temperatura, orientação, display externo, desktop, jogo, vídeo, HDR e brilho está implementado e testado. A observação contínua dos eventos ainda é parcial: não se afirma automação ativa quando a ROM não fornece sinal confiável. Um perfil por aplicativo sempre altera a transformação global enquanto aquele app está em primeiro plano; não é isolamento por layer.

## Compatibilidade resumida

| Recurso | Estado atual |
| --- | --- |
| Matriz 4×4 / saturação / gerenciamento de cor | Backend real legado; validar no aparelho |
| Controles lineares e filtros | Implementados no pipeline |
| Root genérico/Magisk/KSU/forks/APatch | Execução genérica real + classificação heurística |
| Painel LCD/OLED/AMOLED | Detecção heurística + override visual |
| Perfis / backup / tiles / boot | Implementados |
| Gama, LUT 1D/3D e curvas | Companion implementado; requer adaptador vendor validado |
| Brilho físico, HBM, DC dimming, PWM, CABC, MEMC | Somente capacidade/estado indisponível |
| Refresh rate, resolução e DPI | Arquitetura preparada; backend de escrita não habilitado |
| Samsung/Qualcomm/MediaTek/Exynos/OEMs | Adaptadores de detecção; nenhuma escrita desconhecida |

## Build

Requisitos: JDK 17, Android SDK Platform 35 e acesso às dependências Gradle na primeira sincronização. O projeto usa Gradle Wrapper 8.9, AGP 8.7.3, `minSdk 24`, `compileSdk 35` e `targetSdk 35`.

```shell
git clone https://github.com/pescadordegoiaba/Sa2ration.git
cd Sa2ration
./gradlew clean
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
./gradlew packageCompanionModule
```

No Windows use `gradlew.bat`. O APK é gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
build/outputs/module/Sa2ration-Companion-1.0.0.zip
```

Instalação:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abra o aplicativo, autorize root e confira **Configurações → Diagnóstico** antes de aplicar valores extremos.

## Recuperação rápida

- use o tile **Reset Sa2ration**;
- na tela de configurações, toque em **Restaurar display neutro**;
- aguarde 15 segundos sem confirmar para rollback automático;
- via ADB shell/root:

```shell
adb shell am broadcast -a com.xda.sa2ration.action.ADB_RESET_DISPLAY -n com.xda.sa2ration/.recovery.AdbResetReceiver
```

- para impedir restauração no próximo boot:

```shell
adb shell su -c 'mkdir -p /data/adb/sa2ration && touch /data/adb/sa2ration/disable'
```

ou:

```shell
adb shell su -c 'setprop persist.sa2ration.safe_mode 1'
```

Consulte [RECOVERY.md](docs/RECOVERY.md) para reset direto e reativação.

## Limitações importantes

- o resultado visual e os códigos privados do SurfaceFlinger precisam de teste no hardware/ROM alvo;
- o preview da Activity pode diferir do resultado global do compositor;
- valores extremos podem causar clipping, tela branca/escura ou dominante forte;
- a seleção manual de painel/root não ignora verificações funcionais;
- não há escrita ativa em sysfs vendor, ciclos de compensação OLED ou controladores do painel;
- gama/LUT real requer adaptador compatível dentro do Companion; o módulo base não inventa comandos;
- automação contínua permanece parcial; os padrões essenciais de calibração estão implementados.

## Desenvolvimento

Antes de publicar:

```shell
./gradlew clean
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
git diff --check
```

O APK antigo em `app/release` é um binário histórico e não é alterado por este trabalho.
