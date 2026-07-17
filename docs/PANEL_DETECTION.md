# Detecção de painel

`DisplayPanelDetector` combina fontes Java e root somente leitura: `DisplayManager`, `Display.Mode`, `getprop`, `dumpsys display`, `dumpsys SurfaceFlinger --display-id`, `/sys/class/drm`, `/sys/class/backlight`, gráficos e nomes de painel/device-tree.

`PanelClassifier` pondera tokens como `oled`, `amoled`, `poled`, `ltpo`, `samoled`, `lcd`, `ips`, `pls`, `tft`, `ltps`, `mini-led`, controladores conhecidos e presença de backlight. Evidências específicas vencem tokens genéricos. A marca do aparelho sozinha não decide a tecnologia.

O resultado contém tecnologia detectada e efetiva, confiança, fontes, evidências, nome, fabricante, backend provável, suporte confirmado/inferido e conflito manual. O override manual somente mostra OLED/LCD e perfis relacionados; capacidades de hardware continuam obrigatórias.
