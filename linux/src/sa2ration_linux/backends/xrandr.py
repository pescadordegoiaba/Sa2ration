from __future__ import annotations

import math
import os
import re
import shutil

from sa2ration_linux.command import CommandRunner
from sa2ration_linux.edid import infer_panel_technology, read_edid
from sa2ration_linux.models import ApplyResult, DisplayMode, DisplaySettings, MonitorInfo

_OUTPUT = re.compile(r"^(?P<name>[A-Za-z0-9_.:+-]+) connected(?: primary)?(?: (?P<w>\d+)x(?P<h>\d+)\+\d+\+\d+)?")
_MODE = re.compile(r"^\s+(?P<w>\d+)x(?P<h>\d+)\s+(?P<rates>.+)$")


def kelvin_to_gamma(kelvin: int) -> tuple[float, float, float]:
    """Approximate white-point multipliers, normalized for xrandr gamma."""
    if int(kelvin) == 6500:
        return 1.0, 1.0, 1.0
    value = max(1000, min(10000, kelvin)) / 100.0
    red = 255.0 if value <= 66 else 329.698727446 * math.pow(value - 60, -0.1332047592)
    green = 99.4708025861 * math.log(value) - 161.1195681661 if value <= 66 else 288.1221695283 * math.pow(value - 60, -0.0755148492)
    blue = 255.0 if value >= 66 else (0.0 if value <= 19 else 138.5177312231 * math.log(value - 10) - 305.044792731)
    values = [max(0.05, min(255.0, channel)) / 255.0 for channel in (red, green, blue)]
    maximum = max(values)
    return tuple(channel / maximum for channel in values)  # type: ignore[return-value]


class XRandRBackend:
    name = "XRandR (X11)"

    def __init__(self, runner: CommandRunner | None = None, environment: dict[str, str] | None = None) -> None:
        self.runner = runner or CommandRunner()
        self.environment = environment or os.environ

    def available(self) -> bool:
        return self.environment.get("XDG_SESSION_TYPE", "").lower() == "x11" and shutil.which("xrandr") is not None

    def detect(self) -> list[MonitorInfo]:
        if not self.available():
            return []
        result = self.runner.run(("xrandr", "--query"))
        if not result.ok:
            return []
        monitors: list[MonitorInfo] = []
        current: MonitorInfo | None = None
        for line in result.stdout.splitlines():
            output_match = _OUTPUT.match(line)
            if output_match:
                connector = output_match.group("name")
                edid = read_edid(connector)
                display_name = edid.get("name") or connector
                technology, confidence = infer_panel_technology(display_name)
                current = MonitorInfo(
                    id=connector,
                    connector=connector,
                    name=display_name,
                    manufacturer=edid.get("manufacturer", "Desconhecido"),
                    serial=edid.get("serial", ""),
                    panel_technology=technology,
                    technology_confidence=confidence,
                    primary=" primary " in line,
                )
                monitors.append(current)
                continue
            mode_match = _MODE.match(line)
            if current is None or mode_match is None:
                continue
            width, height = int(mode_match.group("w")), int(mode_match.group("h"))
            for raw_rate in mode_match.group("rates").split():
                clean_rate = raw_rate.rstrip("*+")
                try:
                    refresh = float(clean_rate)
                except ValueError:
                    continue
                mode_id = f"{width}x{height}@{refresh:.2f}"
                preferred = "+" in raw_rate
                current.modes.append(DisplayMode(mode_id, width, height, refresh, preferred))
                if "*" in raw_rate:
                    current.current_mode_id = mode_id
        return monitors

    def apply(self, monitor: MonitorInfo, settings: DisplaySettings) -> ApplyResult:
        normalized = settings.normalized()
        gamma = kelvin_to_gamma(normalized.temperature)
        args = [
            "xrandr", "--output", monitor.connector,
            "--brightness", f"{normalized.brightness / 100.0:.3f}",
            "--gamma", ":".join(f"{channel:.3f}" for channel in gamma),
        ]
        if normalized.mode_id:
            mode = next((item for item in monitor.modes if item.id == normalized.mode_id), None)
            if mode is None:
                return ApplyResult(False, errors=("Modo XRandR desconhecido",))
            args.extend(("--mode", f"{mode.width}x{mode.height}", "--rate", f"{mode.refresh_hz:.2f}"))
        result = self.runner.run(tuple(args), timeout=12)
        if not result.ok:
            return ApplyResult(False, errors=(result.stderr or result.stdout or "XRandR falhou",))
        dangerous = normalized.brightness < 10 or normalized.temperature < 1800 or normalized.mode_id != monitor.current_mode_id
        return ApplyResult(True, messages=("Cor e modo aplicados pelo XRandR",), dangerous=dangerous)
