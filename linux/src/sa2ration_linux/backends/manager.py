from __future__ import annotations

import os

from sa2ration_linux.backends.ddc import DdcUtilBackend
from sa2ration_linux.backends.gpu import KWinGpuEffectBackend
from sa2ration_linux.backends.kscreen import KScreenBackend
from sa2ration_linux.backends.kwin import KWinNightLightBackend
from sa2ration_linux.backends.xrandr import XRandRBackend
from sa2ration_linux.command import CommandRunner
from sa2ration_linux.models import Capability, CapabilityState, DisplayCapabilities, DisplaySettings, MonitorInfo, ApplyResult


class BackendManager:
    def __init__(self, runner: CommandRunner | None = None, environment: dict[str, str] | None = None) -> None:
        self.runner = runner or CommandRunner()
        self.environment = environment or os.environ
        self.kscreen = KScreenBackend(self.runner)
        self.xrandr = XRandRBackend(self.runner, self.environment)
        self.kwin = KWinNightLightBackend(self.runner)
        self.ddc = DdcUtilBackend(self.runner)
        self.gpu = KWinGpuEffectBackend(self.runner)
        session = self.environment.get("XDG_SESSION_TYPE", "").lower()
        desktop = self.environment.get("XDG_CURRENT_DESKTOP", "").lower()
        self.display_backend = self.kscreen if session == "wayland" and "kde" in desktop and self.kscreen.available() else self.xrandr

    @property
    def backend_name(self) -> str:
        return self.display_backend.name if self.display_backend.available() else "Nenhum backend de sessão"

    def monitors(self) -> list[MonitorInfo]:
        return self.display_backend.detect() if self.display_backend.available() else []

    def capabilities(self, monitor: MonitorInfo) -> DisplayCapabilities:
        display_ok = self.display_backend.available()
        is_x11 = self.display_backend is self.xrandr
        ddc = self.ddc.detect().get(monitor.connector)
        supported = lambda backend, reason="": Capability(CapabilityState.SUPPORTED, backend, reason)
        unavailable = lambda reason: Capability(CapabilityState.UNAVAILABLE, "", reason)
        gpu_capability = supported(self.gpu.name, self.gpu.status_reason()) if self.gpu.available() else unavailable(self.gpu.status_reason())
        return DisplayCapabilities(
            brightness=supported(self.backend_name, "Brilho digital/sessão") if display_ok else unavailable("Nenhum backend de brilho da sessão"),
            temperature=supported("XRandR") if is_x11 else (supported(self.kwin.name) if self.kwin.available() else unavailable("KWin Night Light não disponível")),
            resolution=supported(self.backend_name) if display_ok and bool(monitor.modes) else unavailable("Modos não informados"),
            refresh_rate=supported(self.backend_name) if display_ok and bool(monitor.modes) else unavailable("Taxas não informadas"),
            physical_brightness=supported(self.ddc.name) if ddc and ddc.brightness is not None else unavailable("DDC/CI VCP 0x10 não confirmado"),
            physical_contrast=supported(self.ddc.name) if ddc and ddc.contrast is not None else unavailable("DDC/CI VCP 0x12 não confirmado"),
            gamma=supported("XRandR") if is_x11 else unavailable("KDE Wayland não expõe gama RGB genérica com segurança"),
            gpu_brightness=gpu_capability,
            gpu_contrast=gpu_capability,
            gpu_saturation=gpu_capability,
        )

    def apply(
        self,
        monitor: MonitorInfo,
        settings: DisplaySettings,
        stable_settings: DisplaySettings | None = None,
        temporary_gpu: bool = False,
    ) -> ApplyResult:
        base = self.display_backend.apply(monitor, settings)
        messages, errors = list(base.messages), list(base.errors)
        if base.success and self.display_backend is self.kscreen:
            if self.kwin.available():
                ok, detail = self.kwin.apply_temperature(settings.temperature)
                if ok:
                    messages.append("Temperatura aplicada pelo KWin Night Light")
                else:
                    errors.append(detail or "Falha ao aplicar temperatura")
            else:
                messages.append("Temperatura ignorada: KWin Night Light indisponível")
        if settings.physical_brightness is not None or settings.physical_contrast is not None:
            ok, ddc_errors = self.ddc.apply(monitor.connector, settings.physical_brightness, settings.physical_contrast)
            if ok:
                messages.append("Controles físicos aplicados via DDC/CI")
            else:
                errors.extend(ddc_errors)
        if settings.gpu_enabled or self.gpu.installed():
            ok, detail = self.gpu.apply(
                settings,
                stable_settings or settings,
                15 if temporary_gpu else 0,
            )
            if ok:
                messages.append(detail)
            else:
                errors.append(detail)
        dangerous = base.dangerous or settings.temperature < 1800 or self.gpu.is_dangerous(settings)
        return ApplyResult(base.success and not errors, tuple(messages), tuple(errors), dangerous)

    def reset_color(self, monitor_id: str = "") -> tuple[bool, str]:
        gpu_ok, gpu_message = self.gpu.reset(monitor_id)
        if self.display_backend is self.kscreen:
            kwin_ok, kwin_message = self.kwin.reset() if self.kwin.available() else (True, "KWin Night Light indisponível; nada para resetar")
            return gpu_ok and kwin_ok, f"{kwin_message}; {gpu_message}".strip("; ")
        return gpu_ok, f"A neutralização é aplicada junto ao XRandR; {gpu_message}"
