from __future__ import annotations

import json
import re
import shutil

from sa2ration_linux.command import CommandRunner
from sa2ration_linux.edid import infer_panel_technology, read_edid
from sa2ration_linux.models import ApplyResult, DisplayMode, DisplaySettings, MonitorInfo

_SAFE_TOKEN = re.compile(r"^[A-Za-z0-9_.:+-]+$")


class KScreenBackend:
    name = "KDE KScreen (Wayland)"

    def __init__(self, runner: CommandRunner | None = None) -> None:
        self.runner = runner or CommandRunner()

    def available(self) -> bool:
        return shutil.which("kscreen-doctor") is not None

    def detect(self) -> list[MonitorInfo]:
        if not self.available():
            return []
        result = self.runner.run(("kscreen-doctor", "--json"))
        if not result.ok:
            return []
        try:
            payload = json.loads(result.stdout)
        except (ValueError, TypeError):
            return []
        monitors: list[MonitorInfo] = []
        for output in payload.get("outputs", []):
            if not output.get("connected", False):
                continue
            connector = str(output.get("name", ""))
            if not connector or not _SAFE_TOKEN.fullmatch(connector):
                continue
            edid = read_edid(connector)
            display_name = edid.get("name") or str(output.get("model", "")) or connector
            technology, confidence = infer_panel_technology(display_name)
            preferred = {str(mode) for mode in output.get("preferredModes", [])}
            modes = []
            for mode in output.get("modes", []):
                size = mode.get("size", {})
                mode_id = str(mode.get("id", ""))
                if not mode_id or not _SAFE_TOKEN.fullmatch(mode_id):
                    continue
                modes.append(DisplayMode(
                    id=mode_id,
                    width=int(size.get("width", 0)),
                    height=int(size.get("height", 0)),
                    refresh_hz=float(mode.get("refreshRate", 0.0)),
                    preferred=mode_id in preferred,
                ))
            monitors.append(MonitorInfo(
                id=connector,
                connector=connector,
                name=display_name,
                manufacturer=edid.get("manufacturer", "Desconhecido"),
                serial=edid.get("serial", ""),
                panel_technology=technology,
                technology_confidence=confidence,
                current_mode_id=str(output.get("currentModeId", "")),
                modes=modes,
                brightness=float(output.get("brightness", 1.0)) * 100.0,
                enabled=bool(output.get("enabled", True)),
                primary=bool(output.get("priority", 0) == 1),
            ))
        return monitors

    def apply(self, monitor: MonitorInfo, settings: DisplaySettings) -> ApplyResult:
        if not _SAFE_TOKEN.fullmatch(monitor.connector):
            return ApplyResult(False, errors=("Conector inválido",))
        normalized = settings.normalized()
        actions = [f"output.{monitor.connector}.brightness.{normalized.brightness:.2f}"]
        if normalized.mode_id:
            known_modes = {mode.id for mode in monitor.modes}
            if normalized.mode_id not in known_modes or not _SAFE_TOKEN.fullmatch(normalized.mode_id):
                return ApplyResult(False, errors=("Modo de vídeo não reconhecido",))
            actions.append(f"output.{monitor.connector}.mode.{normalized.mode_id}")
        result = self.runner.run(("kscreen-doctor", *actions), timeout=12)
        if not result.ok:
            return ApplyResult(False, errors=(result.stderr or result.stdout or "KScreen falhou",))
        dangerous = normalized.brightness < 10 or (normalized.mode_id and normalized.mode_id != monitor.current_mode_id)
        return ApplyResult(True, messages=("Brilho e modo aplicados pelo KScreen",), dangerous=dangerous)
