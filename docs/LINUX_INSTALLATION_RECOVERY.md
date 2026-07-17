# Instalação, diagnóstico e recuperação no Linux

## Sistemas suportados nesta versão

- Arch Linux ou Manjaro `x86_64`;
- Qt 6;
- KDE Plasma com KWin 6.7.2;
- composição OpenGL ativa;
- sessão Wayland ou X11 gerenciada pelo KWin.

O frontend abre em outros desktops Qt, mas o processamento global aparece como
indisponível porque não existe um plugin equivalente para o compositor.

## Dependências

```bash
sudo pacman -S --needed \
  base-devel \
  cmake \
  extra-cmake-modules \
  python-build \
  qt6-base \
  kwin
```

`python-build` é usado apenas para formar o arquivo de source consumido pelo
`makepkg`. O pacote instalado não depende de Python ou PySide.

## Gerar o pacote

Na raiz do repositório:

```bash
cd linux
./build-arch-package.sh
```

O script:

1. remove metadados de build antigos;
2. cria o source tarball 2.0.0;
3. executa o `PKGBUILD`;
4. compila o frontend nativo;
5. compila o plugin contra a ABI instalada do KWin;
6. gera o pacote em `linux/packaging/`.

Instalação:

```bash
sudo pacman -U packaging/sa2ration-linux-2.0.0-1-x86_64.pkg.tar.zst
```

Arquivos instalados relevantes:

```text
/usr/bin/sa2ration-linux
/usr/lib/qt6/plugins/kwin/effects/plugins/sa2ration_gpu.so
/usr/share/applications/io.github.sa2ration.Linux.desktop
/usr/share/icons/hicolor/scalable/apps/io.github.sa2ration.Linux.svg
```

## Primeira execução

Abra pelo menu ou execute:

```bash
sa2ration-linux
```

O programa tenta carregar o efeito automaticamente. Se a sessão já estava
aberta antes da instalação e o KWin ainda não indexou o arquivo, saia e entre
novamente na sessão uma vez. Não habilite manualmente o efeito no KCM: a
aplicação controla seu ciclo de vida.

## Verificação

Confirmar que o plugin está instalado:

```bash
qtplugininfo6 /usr/lib/qt6/plugins/kwin/effects/plugins/sa2ration_gpu.so
```

O IID deve corresponder à versão instalada do KWin. Para o pacote atual:

```text
org.kde.kwin.EffectPluginFactory6.7.2
```

Consultar carregamento pelo D-Bus:

```bash
qdbus6 org.kde.KWin /Effects \
  org.kde.kwin.Effects.isEffectLoaded sa2ration_gpu
```

Consultar o arquivo persistido:

```bash
sed -n '1,120p' ~/.config/sa2ration-linux/gpu.ini
```

## Recuperação pela interface

- **Ativar/Desativar** remove imediatamente o processamento;
- **Última estável** volta à última configuração confirmada;
- **Restaurar neutro** desliga o efeito e restaura `1×/1×/1×/0`;
- valores extremos abrem confirmação de 15 segundos;
- fechar o diálogo ou deixar a contagem terminar reverte a alteração.

## Recuperação pelo terminal

Com a sessão gráfica funcional:

```bash
qdbus6 org.kde.KWin /Effects \
  org.kde.kwin.Effects.unloadEffect sa2ration_gpu
rm -f ~/.config/sa2ration-linux/gpu.ini
```

O primeiro comando remove o efeito da sessão. O segundo apaga o estado; a
próxima abertura começa neutra e desativada.

## Recuperação por TTY

Se a imagem estiver difícil de usar:

1. pressione `Ctrl+Alt+F3`;
2. faça login;
3. remova a configuração:

```bash
rm -f ~/.config/sa2ration-linux/gpu.ini
```

4. encerre a sessão gráfica ou reinicie:

```bash
systemctl reboot
```

O efeito tem `EnabledByDefault=false`. Sem o frontend para carregá-lo, uma nova
sessão começa neutra. O Sa2ration não possui serviço de sistema, initramfs,
hook de boot ou unidade systemd, portanto não consegue bloquear a inicialização
do Linux.

## Desinstalação

Primeiro desligue ou descarregue o efeito. Depois:

```bash
sudo pacman -Rns sa2ration-linux
rm -rf ~/.config/sa2ration-linux
```

Reinicie a sessão do Plasma para garantir que o compositor descarregue qualquer
referência antiga ao plugin removido.

## Atualização do KWin

Não ignore a dependência exata do pacote. Depois de atualizar o KWin:

```bash
cd linux
./build-arch-package.sh
sudo pacman -U packaging/sa2ration-linux-2.0.0-1-x86_64.pkg.tar.zst
```

Se o frontend abrir, mas o efeito não carregar, compare a versão mostrada pelo
`qtplugininfo6` com a versão do KWin. Um mismatch de ABI exige recompilação,
não alteração manual do metadata.

## Diagnóstico de problemas

### “KWin GPU indisponível”

- confirme que a sessão usa KWin;
- confirme que a composição OpenGL está ativa;
- verifique a presença do `.so`;
- reinicie a sessão após a primeira instalação;
- confira a ABI do plugin.

### A janela abre, mas os controles não mudam a tela

- confirme que o switch está ligado;
- consulte `isEffectLoaded` pelo D-Bus;
- verifique o status dentro da aplicação;
- restaure neutro e tente um valor moderado, como saturação `1,2×`;
- não use valores extremos como primeiro teste.

### Uso de GPU elevado

Desative o switch. Isso remove o redirecionamento offscreen das janelas sem
encerrar o programa. O custo adicional de pixels existe somente enquanto o
efeito está ativo.
