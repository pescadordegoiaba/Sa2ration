# Pipeline GPU do Sa2ration Linux

## Onde o processamento acontece

O efeito `sa2ration_gpu` é executado pelo KWin durante a composição OpenGL. Ele
processa a textura de cada janela redirecionada antes da saída final do
compositor. Isso permite controlar a imagem global sem alterar aplicativos ou
depender de configurações individuais do monitor.

O efeito é digital. Ele não muda:

- potência ou intensidade física do backlight;
- controles DDC/CI do monitor;
- tensão, PWM ou firmware do painel;
- LUT interna do monitor;
- perfil ICC salvo no hardware.

## Ordem da transformação

Para cada pixel RGB de entrada, o shader usa esta ordem:

```text
entrada
→ offset
→ ganho de brilho
→ contraste em torno de 0,5
→ cálculo de luminância Rec. 709
→ interpolação de saturação
→ saída do efeito
→ gerenciamento de cor do KWin
```

Forma conceitual:

```text
c0 = entrada.rgb + offset
c1 = c0 × brilho
c2 = (c1 - 0,5) × contraste + 0,5
Y  = dot(c2, [0,2126, 0,7152, 0,0722])
c3 = mix([Y, Y, Y], c2, saturação)
saída.rgb = c3
```

O shader não faz clamp antecipado. O compositor e o formato do framebuffer
determinam o tratamento final de valores fora do gamut, permitindo valores
extremos sem apresentá-los como livres de clipping.

## Controles

| Controle | Neutro | Slider prático | Input aceito | Implementação |
|---|---:|---:|---:|---|
| Ativar/Desativar | desligado | switch | — | adiciona/remove o efeito do pipeline |
| Brilho digital | `1,0×` | `0–3×` | `0–10×` | ganho RGB global |
| Contraste | `1,0×` | `0–5×` | `0–10×` | escala em torno de `0,5` |
| Saturação | `1,0×` | `0–5×` | `0–10×` | interpolação pela luminância Rec. 709 |
| Offset | `0,0` | `-1–+1` | `-2–+2` | soma linear antes do ganho |

O input numérico existe ao lado de cada slider. Assim, o slider permanece
preciso em uso normal sem impedir experimentos com valores maiores.

## Semântica do switch

O switch **Ativar/Desativar** não abre outro painel nem depende do KCM de
efeitos do Plasma.

Quando ligado:

- `Enabled=true` é persistido;
- o plugin é carregado se necessário;
- o KWin relê os uniforms;
- janelas existentes e novas são redirecionadas;
- o display é repintado.

Quando desligado:

- `Enabled=false` é persistido;
- todas as janelas são removidas do redirecionamento;
- o shader deixa de alterar a imagem;
- o plugin permanece carregado e pronto para uma próxima ativação.

Manter o plugin carregado evita latência e elimina a necessidade de entrar nas
configurações. Desativado não significa que os valores antigos sejam apagados;
significa que eles não participam do pipeline.

## Recuperação de valores extremos

Uma configuração é considerada perigosa se estiver ativa e atender a pelo
menos uma condição:

- brilho abaixo de `0,2×` ou acima de `2×`;
- contraste abaixo de `0,15×` ou acima de `2,5×`;
- saturação acima de `3×`;
- offset absoluto acima de `0,4`.

Nesses casos:

1. o estado anterior permanece em `Stable*`;
2. o novo estado recebe prazo de 15 segundos;
3. o frontend mostra um diálogo Qt com **Manter** e **Reverter**;
4. confirmar promove o estado atual para estável;
5. rejeitar restaura imediatamente `Stable*`;
6. se a aplicação travar, fechar ou perder D-Bus, o timer interno do plugin
   executa a mesma restauração.

O timer dentro do KWin é importante: depender somente do frontend deixaria a
recuperação vulnerável a um crash da interface.

## Custo e comportamento do compositor

O plugin usa `OffscreenEffect`, portanto há um passe adicional para as janelas
afetadas. Enquanto ativo ele pode:

- aumentar uso de GPU e memória de renderização;
- impedir direct scanout;
- elevar consumo em notebooks;
- interagir com outros efeitos que também redirecionam janelas;
- produzir clipping visível em valores altos.

Quando desativado, os redirecionamentos são removidos. O custo permanente fica
restrito ao objeto do plugin carregado e não ao processamento de pixels.

## O que não é simulado

O pipeline atual não anuncia suporte para operações que exigem transformação
não linear ou acesso ao hardware:

- gama real por canal;
- LUT 1D ou 3D;
- curvas RGB;
- contraste local;
- black equalizer real;
- tone mapping HDR;
- nitidez, debanding e redução de ruído;
- brilho físico, CABC, local dimming ou overdrive.

Esses recursos só devem ser adicionados quando existir uma API verificável no
compositor, DRM/KMS ou monitor. Não serão aproximados silenciosamente com o
shader linear atual.
