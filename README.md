# Sa2ration Enhanced — controles de cor 0–10×

Fork do [Sa2ration Enhanced de sansgood](https://github.com/sansgood/Sa2ration),
derivado do projeto original de
[zacharee](https://github.com/zacharee/Sa2ration). Esta versão amplia o aplicativo
com saturação e contraste globais e ajustes independentes para os canais vermelho,
verde e azul.

> **Requer root.** O aplicativo usa interfaces privadas do SurfaceFlinger. Elas
> existem no Android Open Source Project, mas fabricantes e ROMs customizadas podem
> alterar, bloquear ou remover essas transações.

## O que foi adicionado neste fork

- Saturação global configurável de `0.00×` a `10.00×`.
- Contraste global configurável de `0.00×` a `10.00×`.
- Saturação independente para vermelho, verde e azul, cada uma de `0.00×` a
  `10.00×`.
- Contraste independente para vermelho, verde e azul, cada um de `0.00×` a
  `10.00×`.
- Slider com resolução de `0.01` e campo numérico para todos os oito ajustes.
- Sincronização bidirecional: mover um slider atualiza o input; digitar no input
  atualiza o slider.
- Validação dos inputs e limitação automática ao intervalo de `0–10×`.
- Interface rolável, necessária para acomodar todos os controles em telas menores.
- Matriz de transformação de cor 4×4 para contraste e controles por canal.
- Salvamento de todos os parâmetros no armazenamento privado do aplicativo.
- Restauração dos parâmetros depois da reinicialização do Android.
- Reset completo de todos os multiplicadores para `1.00×`.
- Testes unitários para a geração e serialização da matriz de cor.
- Toolchain atualizado para Gradle 8.9, Android Gradle Plugin 8.7.3, AndroidX e
  `compileSdk`/`targetSdk` 35.
- Gradle Wrapper completo (`gradlew`, `gradlew.bat` e `gradle-wrapper.jar`) para
  builds reproduzíveis sem uma instalação global do Gradle.

## Controles disponíveis

| Grupo | Controle | Intervalo | Padrão |
| --- | --- | ---: | ---: |
| Global | Saturação | 0.00–10.00× | 1.00× |
| Global | Contraste | 0.00–10.00× | 1.00× |
| Saturação por cor | Vermelho | 0.00–10.00× | 1.00× |
| Saturação por cor | Verde | 0.00–10.00× | 1.00× |
| Saturação por cor | Azul | 0.00–10.00× | 1.00× |
| Contraste por cor | Vermelho | 0.00–10.00× | 1.00× |
| Contraste por cor | Verde | 0.00–10.00× | 1.00× |
| Contraste por cor | Azul | 0.00–10.00× | 1.00× |

`1.00×` é neutro. Em saturação, `0.00×` converte o componente correspondente para
luminância; em contraste, `0.00×` produz um valor uniforme no ponto médio. Valores
acima de `1.00×` intensificam o efeito. Os controles globais e por canal são
combinados; portanto, utilizar valores altos nos dois níveis produz um efeito
acumulado muito forte e pode causar clipping de cores.

O switch **Enable Color Management** mantém o comportamento do projeto anterior e
alterna o modo de cor do SurfaceFlinger.

## Como funciona

### 1. Inicialização e interface

`MainActivity.java` carrega os valores salvos, converte cada multiplicador para a
escala interna do slider (`0–1000`) e conecta cada slider ao seu respectivo campo
numérico. O progresso `100` corresponde a `1.00×`; cada unidade corresponde a
`0.01×`.

Ao soltar um slider ou confirmar/sair de um campo numérico, o aplicativo monta e
aplica a configuração completa. Isso evita executar um comando root para cada
pequeno evento de digitação.

### 2. Saturação global

A saturação global continua usando o caminho nativo empregado pelo projeto
original:

```shell
setprop persist.sys.sf.color_saturation VALOR
service call SurfaceFlinger 1022 f VALOR
```

A propriedade persistente ajuda o SurfaceFlinger a recuperar o valor, enquanto a
transação `1022` aplica a mudança imediatamente.

### 3. Contraste e ajustes por canal

`ColorMatrixController.java` cria uma matriz 4×4 em formato **column-major**, como
esperado pela transação `1015` do SurfaceFlinger. A matriz usa os coeficientes de
luminância Rec. 709/sRGB:

```text
Y = 0.2126R + 0.7152G + 0.0722B
```

Para cada canal de saída, a saturação mistura o canal original com a luminância. O
contraste é aplicado em torno do ponto médio `0.5`, usando a forma:

```text
saída = contraste × entrada + 0.5 × (1 - contraste)
```

O contraste global multiplica o contraste específico de cada canal. Depois de
compor todos os parâmetros, a matriz é serializada e enviada como 16 floats:

```shell
service call SurfaceFlinger 1015 i32 1 f M0 f M1 ... f M15
```

Essa abordagem permite tratar vermelho, verde e azul separadamente sem modificar
o framework ou a ROM do aparelho.

### 4. Root e execução dos comandos

`CommandController.java` abre um processo `su`, envia os comandos ao shell root e
aguarda sua conclusão. Na abertura do aplicativo, um teste de `su` é executado. Se
o root não estiver disponível ou não for autorizado, o aplicativo mostra um aviso
e encerra.

### 5. Salvamento e restauração

`PersistenceController.java` grava os valores em `info.properties`, dentro do
armazenamento privado do aplicativo. Os valores são salvos ao pausar a Activity e
também quando o botão **Save** é pressionado.

`NewBootReceiver.java` recebe `BOOT_COMPLETED`, restaura os oito multiplicadores,
reconstrói a matriz e reaplica a saturação global, a matriz por canal e o modo de
cor. A permissão `RECEIVE_BOOT_COMPLETED` está declarada no manifest.

### 6. Reset

O botão **Reset** retorna saturação e contraste, globais e por canal, para `1.00×`,
reaplica a matriz identidade e salva os valores padrão.

## Estrutura principal

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/xda/sa2ration/
│   ├── MainActivity.java             # interface, inputs e fluxo principal
│   ├── ColorMatrixController.java    # cálculo e serialização da matriz 4×4
│   ├── CommandController.java        # execução de comandos com su
│   ├── PersistenceController.java    # armazenamento privado dos parâmetros
│   └── NewBootReceiver.java          # reaplicação depois do boot
└── res/layout/content_main.xml       # interface rolável e seus oito controles

app/src/test/java/com/xda/sa2ration/
└── ColorMatrixControllerTest.java    # testes unitários da transformação
```

## Requisitos para compilar

- Linux, macOS ou Windows.
- JDK 17 ou mais recente compatível com Gradle 8.9.
- Android SDK Platform 35.
- Android SDK Build Tools instaladas.
- Acesso à internet no primeiro build para baixar dependências, caso elas ainda
  não estejam no cache do Gradle.

Defina `ANDROID_HOME` ou `ANDROID_SDK_ROOT` para o diretório do Android SDK. O
Android Studio também pode abrir o diretório raiz e sincronizar o projeto.

## Como compilar

Clone este fork:

```shell
git clone https://github.com/pescadordegoiaba/Sa2ration.git
cd Sa2ration
```

No Linux ou macOS:

```shell
chmod +x gradlew
./gradlew test lintDebug assembleDebug
```

No Windows:

```powershell
.\gradlew.bat test lintDebug assembleDebug
```

O APK de debug será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para compilar apenas o APK:

```shell
./gradlew assembleDebug
```

Para limpar todos os arquivos gerados:

```shell
./gradlew clean
```

## Como instalar

Ative a depuração USB, conecte o aparelho e execute:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abra o aplicativo e conceda acesso root quando o gerenciador de root solicitar.
Magisk, KernelSU, APatch ou outra solução equivalente deve fornecer um comando
`su` funcional.

O arquivo `app/release/app-release.apk` existente no histórico veio do projeto
anterior e **não representa as alterações deste fork**. Compile o código atual com
o Gradle Wrapper para obter a versão atualizada.

## Testes e validação

O código atual foi validado com:

```shell
./gradlew test assembleDebug lintDebug
```

Os testes cobrem:

- matriz identidade com todos os valores em `1.00×`;
- conversão para luminância com saturação zero;
- contraste global até `10.00×` em torno do ponto médio;
- independência do contraste por canal;
- limitação de valores inválidos ou fora do intervalo;
- serialização dos 16 floats para a transação do SurfaceFlinger.

O APK de debug também foi verificado com `apksigner` e possui assinatura Android
Debug válida pelo esquema APK Signature Scheme v2.

## Compatibilidade e limitações

- É obrigatório possuir root e autorizar o aplicativo no gerenciador de root.
- As transações `1015`, `1022` e `1023` são interfaces privadas; a compatibilidade
  depende da versão do Android, do fabricante e da ROM.
- Valores muito altos podem causar clipping, perda de detalhes e cores extremas.
- A matriz `1015` é compartilhada com recursos de cor do sistema. Night Light,
  correção de cores, acessibilidade ou outro aplicativo podem substituir a matriz;
  nesse caso, pressione **Save** ou altere um controle para reaplicá-la.
- O efeito real precisa ser validado no aparelho-alvo. O build e os cálculos são
  testados no host, mas não existe um teste automatizado que confirme o resultado
  visual de cada compositor/OEM.
- O aplicativo transforma a saída completa do SurfaceFlinger; não há seleção por
  aplicativo ou por display.

## Desenvolvimento

Antes de enviar mudanças, execute:

```shell
./gradlew test lintDebug assembleDebug
git diff --check
```

Arquivos gerados em `app/build/`, `.gradle/`, configurações locais do Android SDK e
arquivos específicos do Android Studio não devem ser versionados.
