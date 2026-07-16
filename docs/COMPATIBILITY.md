# Compatibilidade e capacidades

| Área | Estado | Observação |
| --- | --- | --- |
| Android 15 / SDK 35 | Compila | minSdk 24, JDK 17 |
| SurfaceFlinger 1015/1022/1023 | Não testado por aparelho | Backend real legado preservado |
| Matriz linear | Implementada | Pipeline e 16 valores testados |
| Magisk/KSU/Suki/ReSuki/APatch | Heurística + su real | versão/strings variam |
| LCD/OLED/AMOLED | Heurística | seleção manual apenas visual |
| Perfis | Suportado | JSON versionado/AtomicFile |
| Boot | Suportado | WorkManager e estado estável |
| Quick Settings | Suportado | master, próximo perfil, reset |
| Gama/LUT/curvas | Requer módulo | modelos/parser prontos, sem aplicação falsa |
| HDR/HBM/DC/PWM/CABC/MEMC | Desconhecido | nenhuma escrita sem backend confirmado |
| Resolução/DPI/refresh | Preparado | operações de escrita desativadas |
| Samsung/Qualcomm/MediaTek/Exynos/OEM | Detecção | adaptadores conservadores |

Tecnologias LCD, TFT, IPS, PLS, LTPS, LTPO LCD, OLED, POLED, AMOLED, Super/Dynamic AMOLED, LTPO OLED e Mini-LED podem ser classificadas ou escolhidas. “Detectado” não significa que um controle físico está disponível.

## Módulo complementar necessário

Gama por canal real, LUT 1D/3D, curvas, vibrance, black equalizer não linear, soft clipping, compressão de gamut, nitidez, debanding, dithering, contraste local, redução de ruído e tone mapping precisam de RenderEngine/HWC/LUT vendor ou serviço nativo. O contrato `NonLinearDisplayBackend` e os modelos já separam essa futura implementação do pipeline linear.
