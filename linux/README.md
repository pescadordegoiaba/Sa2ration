# Sa2ration Linux

Aplicativo nativo para **Arch Linux e Manjaro**, focado em monitores LCD e no
KDE Plasma. A versão 2.0 é um único executável ELF C++ com **Qt 6 + Dear ImGui**
na mesma janela e um efeito OpenGL nativo do KWin.

A edição Linux é independente do aplicativo Android. Ela não usa root, Magisk,
KernelSU, módulo de kernel, DKMS, firmware nem escrita em `sysfs`.

## Funcionamento atual

Ao abrir `sa2ration-linux`, o programa:

1. lê `~/.config/sa2ration-linux/gpu.ini`;
2. conecta diretamente ao KWin pelo QtDBus;
3. carrega o plugin `sa2ration_gpu` automaticamente;
4. reaplica o estado salvo;
5. deixa o pipeline pronto para o switch **Ativar/Desativar**.

Não é necessário abrir as Configurações do Plasma ou ativar o efeito à mão. Se
o estado persistido estiver ativo, o processamento começa ao abrir o programa.
Se estiver desativado, o plugin fica carregado, mas neutro e sem redirecionar
janelas.

Controles funcionais no shader:

- brilho digital global de `0×` a `10×`;
- contraste global de `0×` a `10×` ao redor do ponto médio;
- saturação global de `0×` a `10×` com luminância Rec. 709;
- offset de luminância de `-2` a `+2`;
- switch **Ativar/Desativar** com aplicação imediata;
- sliders para ajustes normais e inputs numéricos para valores extremos;
- restauração neutra e retorno à última configuração estável;
- confirmação com rollback automático em 15 segundos para valores extremos.

O processamento ocorre no compositor, usando a GPU, antes da imagem chegar ao
monitor. Ele não altera o backlight físico, o menu interno do monitor ou o
perfil ICC gravado no hardware.

## Interface mesclada

Qt 6 implementa:

- janela, escala HiDPI e integração com o tema do desktop;
- cabeçalho, status bar, diagnóstico e diálogos de recuperação;
- comunicação D-Bus com o KWin;
- persistência do estado.

Dear ImGui é renderizado por OpenGL dentro de um `QOpenGLWidget` e implementa:

- switch principal;
- sliders;
- campos numéricos editáveis;
- botões de reset e recuperação;
- painel de status do backend.

Assim, a interface não é uma janela ImGui separada nem uma simulação visual:
os dois toolkits participam do mesmo binário e da mesma janela.

## Instalação no Arch/Manjaro

Dependências de build:

```bash
sudo pacman -S --needed base-devel cmake extra-cmake-modules python-build qt6-base kwin
```

Gerar o pacote local:

```bash
cd linux
./build-arch-package.sh
sudo pacman -U packaging/sa2ration-linux-2.0.0-1-x86_64.pkg.tar.zst
```

Depois, abra **Sa2ration Linux** pelo menu do Plasma ou execute:

```bash
sa2ration-linux
```

Na primeira instalação, uma sessão do KWin que já estava aberta pode ainda não
ter indexado o novo plugin binário. Se o diagnóstico disser que o efeito não
foi encontrado, encerre e entre novamente na sessão uma vez. Isso não exige
abrir nem alterar as Configurações do Plasma.

O pacote instala, em conjunto:

- `/usr/bin/sa2ration-linux` — frontend nativo Qt + ImGui;
- o plugin binário `sa2ration_gpu.so` na pasta de efeitos do KWin;
- atalho do menu e ícone.

Efeitos binários do KWin usam a ABI exata do compositor. O pacote gerado nesta
máquina exige **KWin 6.7.2**. Após atualizar o KWin, recompile e reinstale o
pacote antes de usar o efeito.

## Executar direto da source

```bash
cd linux
cmake -S native-app -B build-native -DCMAKE_BUILD_TYPE=Release
cmake --build build-native --parallel
./build-native/sa2ration-linux
```

O plugin precisa estar instalado para que a aplicação global funcione. O
frontend abre sem ele e mostra `KWin GPU · indisponível`, sem fingir suporte.

Para compilar o plugin separadamente:

```bash
cmake -S gpu-effect -B build-gpu -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr
cmake --build build-gpu --parallel
```

## Segurança e recuperação

O Sa2ration não participa do boot do kernel ou do sistema. O plugin tem
`EnabledByDefault=false`, só é carregado pela aplicação dentro da sessão do
usuário e nunca executa comandos privilegiados. Portanto, uma falha não cria
bootloop; no pior caso, a sessão gráfica pode precisar ser reiniciada.

Valores extremos são escritos como temporários. O próprio plugin mantém um
timer dentro do KWin e restaura a última configuração estável depois de 15
segundos se a confirmação não chegar, mesmo se a interface travar ou fechar.

Recuperação por TTY (`Ctrl+Alt+F3`):

```bash
qdbus6 org.kde.KWin /Effects org.kde.kwin.Effects.unloadEffect sa2ration_gpu
rm -f ~/.config/sa2ration-linux/gpu.ini
```

Se o D-Bus da sessão não estiver acessível no TTY, encerre a sessão gráfica ou
reinicie o computador. Como o efeito não é habilitado por padrão no KWin, ele
não volta sozinho no próximo login; abrir o Sa2ration é o que o carrega.

## Arquitetura

Documentação detalhada:

- [arquitetura e ciclo de vida](../docs/LINUX_ARCHITECTURE.md);
- [pipeline GPU, fórmulas e segurança](../docs/LINUX_GPU_PIPELINE.md);
- [instalação, diagnóstico e recuperação](../docs/LINUX_INSTALLATION_RECOVERY.md).

- `native-app/`: executável C++ Qt 6 + Dear ImGui;
- `native-app/gpueffectcontroller.*`: estado, validação, D-Bus, persistência e rollback;
- `native-app/imguiwidget.*`: ponte de eventos Qt, frame OpenGL e controles ImGui;
- `native-app/mainwindow.*`: composição visual Qt e diálogos;
- `gpu-effect/`: plugin KWin `OffscreenEffect` e shader GLSL;
- `third_party/imgui/`: Dear ImGui v1.92.7 e backend OpenGL 3 sob licença MIT;
- `packaging/PKGBUILD`: pacote Arch/Manjaro que instala o binário e o plugin juntos;
- `src/` e `tests/`: implementação Python 1.x mantida temporariamente como referência e testes de backends antigos; ela não é instalada pelo pacote 2.0.

Pipeline do fragment shader:

```text
textura da janela
→ offset de brilho
→ ganho de brilho
→ contraste ao redor de 0,5
→ luminância Rec. 709
→ saturação
→ saída gerenciada pelo KWin
```

## Validação de desenvolvimento

```bash
cmake -S native-app -B build-native -DCMAKE_BUILD_TYPE=Debug
cmake --build build-native --parallel
cmake -S gpu-effect -B build-gpu -DCMAKE_BUILD_TYPE=Release
cmake --build build-gpu --parallel
PYTHONPATH=src python -m unittest discover -s tests -v
git diff --check
```

## Limitações

- o backend GPU global atual exige KDE Plasma/KWin com composição OpenGL;
- GNOME e compositores wlroots ainda não possuem plugin equivalente;
- o plugin precisa ser recompilado quando a ABI do KWin mudar;
- o passe offscreen pode elevar o uso da GPU e impedir direct scanout enquanto ativo;
- brilho físico DDC/CI, temperatura, resolução e refresh rate ainda pertencem ao frontend Python legado e serão migrados para C++ em uma fase posterior;
- gama real, LUT, curvas, HDR e contraste local não são simulados por matriz linear;
- o pacote 2.0 é `x86_64`; outras arquiteturas precisam ser compiladas localmente.
