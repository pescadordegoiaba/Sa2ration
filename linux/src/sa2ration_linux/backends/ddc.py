from __future__ import annotations

import re
import shutil
from dataclasses import dataclass

from sa2ration_linux.command import CommandRunner


@dataclass(frozen=True)
class DdcDisplay:
    number: int
    connector: str
    brightness: int | None
    contrast: int | None


class DdcUtilBackend:
    name = "DDC/CI (ddcutil)"

    def __init__(self, runner: CommandRunner | None = None) -> None:
        self.runner = runner or CommandRunner()
        self._cache: dict[str, DdcDisplay] | None = None

    def available(self) -> bool:
        return shutil.which("ddcutil") is not None

    @staticmethod
    def _value(text: str) -> int | None:
        match = re.search(r"(?:current value\s*=|C\s*=)\s*(\d+)", text, re.IGNORECASE)
        return int(match.group(1)) if match else None

    def _get(self, display: int, code: str) -> int | None:
        result = self.runner.run(("ddcutil", "--display", str(display), "getvcp", code, "--brief"), timeout=8)
        return self._value(result.stdout) if result.ok else None

    def detect(self, refresh: bool = False) -> dict[str, DdcDisplay]:
        if self._cache is not None and not refresh:
            return self._cache
        found: dict[str, DdcDisplay] = {}
        if not self.available():
            self._cache = found
            return found
        result = self.runner.run(("ddcutil", "detect", "--brief"), timeout=12)
        if not result.ok:
            self._cache = found
            return found
        blocks = re.split(r"(?=^Display \d+)", result.stdout, flags=re.MULTILINE)
        for block in blocks:
            number_match = re.search(r"^Display (\d+)", block, re.MULTILINE)
            connector_match = re.search(r"DRM connector:\s*(?:card\d+-)?([^\s]+)", block)
            if not number_match or not connector_match or "Invalid display" in block:
                continue
            number = int(number_match.group(1))
            connector = connector_match.group(1)
            found[connector] = DdcDisplay(number, connector, self._get(number, "10"), self._get(number, "12"))
        self._cache = found
        return found

    def apply(self, connector: str, brightness: int | None, contrast: int | None) -> tuple[bool, list[str]]:
        display = self.detect().get(connector)
        if display is None:
            return False, ["O monitor não confirmou suporte DDC/CI"]
        errors: list[str] = []
        for code, value, supported in (("10", brightness, display.brightness), ("12", contrast, display.contrast)):
            if value is None:
                continue
            if supported is None:
                errors.append(f"VCP {code} não suportado; nada foi gravado")
                continue
            result = self.runner.run(("ddcutil", "--display", str(display.number), "setvcp", code, str(max(0, min(100, value)))), timeout=10)
            if not result.ok:
                errors.append(result.stderr or result.stdout or f"Falha ao gravar VCP {code}")
        return not errors, errors
