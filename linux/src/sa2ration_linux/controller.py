from __future__ import annotations

import json
import os
import tempfile
from pathlib import Path

from sa2ration_linux.backends.manager import BackendManager
from sa2ration_linux.models import ApplyResult, DisplaySettings, MonitorInfo
from sa2ration_linux.profiles import profile_by_name


class StateStore:
    SCHEMA = 1

    def __init__(self, path: Path | None = None) -> None:
        config_home = Path(os.environ.get("XDG_CONFIG_HOME", Path.home() / ".config"))
        self.path = path or config_home / "sa2ration-linux" / "state.json"

    def load(self) -> dict:
        try:
            value = json.loads(self.path.read_text(encoding="utf-8"))
            return value if value.get("schema") == self.SCHEMA else {}
        except (OSError, ValueError, TypeError):
            return {}

    def save(self, current: DisplaySettings, stable: DisplaySettings) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        payload = {"schema": self.SCHEMA, "current": current.to_dict(), "stable": stable.to_dict()}
        descriptor, temporary = tempfile.mkstemp(prefix="state-", suffix=".json", dir=self.path.parent)
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
                json.dump(payload, stream, ensure_ascii=False, indent=2)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, self.path)
        finally:
            try:
                os.unlink(temporary)
            except FileNotFoundError:
                pass


class DisplayController:
    def __init__(self, manager: BackendManager | None = None, store: StateStore | None = None) -> None:
        self.manager = manager or BackendManager()
        self.store = store or StateStore()
        self.monitors = self.manager.monitors()
        saved = self.store.load()
        self.current = self._load_settings(saved.get("current"))
        self.stable = self._load_settings(saved.get("stable"))

    def _load_settings(self, value: object) -> DisplaySettings | None:
        if not isinstance(value, dict):
            return None
        try:
            return DisplaySettings.from_dict(value)
        except (TypeError, ValueError):
            return None

    def settings_for(self, monitor: MonitorInfo) -> DisplaySettings:
        if self.current and self.current.monitor_id == monitor.id:
            return self.current
        return DisplaySettings(
            monitor_id=monitor.id,
            brightness=monitor.brightness if monitor.brightness is not None else 100,
            temperature=6500,
            mode_id=monitor.current_mode_id,
        )

    def apply(self, monitor: MonitorInfo, settings: DisplaySettings, confirm: bool = False) -> ApplyResult:
        normalized = settings.normalized()
        previous = self.settings_for(monitor)
        stable_for_recovery = self.stable if self.stable and self.stable.monitor_id == monitor.id else previous
        gpu_dangerous = self.manager.gpu.is_dangerous(normalized)
        result = self.manager.apply(
            monitor,
            normalized,
            stable_settings=stable_for_recovery,
            temporary_gpu=gpu_dangerous and not confirm,
        )
        if result.success:
            self.current = normalized
            if confirm or not result.dangerous:
                self.stable = normalized
            elif self.stable is None:
                self.stable = previous
            self._save()
        return result

    def confirm_current(self) -> tuple[bool, str]:
        if self.current is not None:
            if self.current.gpu_enabled:
                ok, message = self.manager.gpu.confirm(self.current)
                if not ok:
                    return False, message
            self.stable = self.current
            self._save()
        return True, "Configuração confirmada"

    def restore_stable(self, monitor: MonitorInfo) -> ApplyResult:
        if self.stable is None or self.stable.monitor_id != monitor.id:
            return self.neutral(monitor)
        return self.apply(monitor, self.stable, confirm=True)

    def neutral(self, monitor: MonitorInfo) -> ApplyResult:
        current = self.settings_for(monitor)
        settings = DisplaySettings(
            monitor_id=monitor.id,
            brightness=100,
            temperature=6500,
            mode_id=current.mode_id or monitor.current_mode_id,
        )
        reset_ok, reset_message = self.manager.reset_color(monitor.id)
        result = self.apply(monitor, settings, confirm=True)
        if not reset_ok:
            return ApplyResult(False, result.messages, result.errors + (reset_message,), result.dangerous)
        return result

    def apply_profile(self, monitor: MonitorInfo, profile_name: str) -> ApplyResult:
        profile = profile_by_name(profile_name)
        if profile is None:
            return ApplyResult(False, errors=(f"Perfil desconhecido: {profile_name}",))
        current = self.settings_for(monitor)
        settings = DisplaySettings(
            monitor_id=monitor.id,
            brightness=profile.brightness,
            temperature=profile.temperature,
            mode_id=current.mode_id or monitor.current_mode_id,
            gpu_enabled=current.gpu_enabled,
            gpu_brightness=current.gpu_brightness,
            gpu_contrast=current.gpu_contrast,
            gpu_saturation=current.gpu_saturation,
            gpu_offset=current.gpu_offset,
        )
        return self.apply(monitor, settings)

    def _save(self) -> None:
        if self.current is None:
            return
        self.store.save(self.current, self.stable or self.current)

    def diagnostic(self) -> dict:
        displays = []
        for monitor in self.monitors:
            capabilities = self.manager.capabilities(monitor)
            displays.append({
                "id": monitor.id,
                "connector": monitor.connector,
                "name": monitor.name,
                "manufacturer": monitor.manufacturer,
                "serial": monitor.serial,
                "technology": monitor.panel_technology,
                "technologyConfidence": monitor.technology_confidence,
                "currentMode": monitor.current_mode_id,
                "modes": [mode.label for mode in monitor.modes],
                "capabilities": {
                    key: {"state": value.state.value, "backend": value.backend, "reason": value.reason}
                    for key, value in capabilities.__dict__.items()
                },
            })
        return {
            "version": 2,
            "backend": self.manager.backend_name,
            "gpuEffect": {
                "installed": self.manager.gpu.installed(),
                "available": self.manager.gpu.available(),
                "reason": self.manager.gpu.status_reason(),
            },
            "displays": displays,
        }
