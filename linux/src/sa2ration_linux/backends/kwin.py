from __future__ import annotations

import shutil

from sa2ration_linux.command import CommandRunner


class KWinNightLightBackend:
    name = "KWin Night Light"
    SERVICE = "org.kde.KWin.NightLight"
    PATH = "/org/kde/KWin/NightLight"
    INTERFACE = "org.kde.KWin.NightLight"

    def __init__(self, runner: CommandRunner | None = None) -> None:
        self.runner = runner or CommandRunner()

    def available(self) -> bool:
        if shutil.which("qdbus6") is None:
            return False
        result = self.runner.run(("qdbus6", self.SERVICE, self.PATH, "org.freedesktop.DBus.Properties.Get", self.INTERFACE, "available"))
        return result.ok and "true" in result.stdout.lower()

    def apply_temperature(self, kelvin: int) -> tuple[bool, str]:
        temperature = max(1000, min(6500, int(kelvin)))
        result = self.runner.run(("qdbus6", self.SERVICE, self.PATH, f"{self.INTERFACE}.preview", str(temperature)))
        return result.ok, result.stderr or result.stdout

    def reset(self) -> tuple[bool, str]:
        result = self.runner.run(("qdbus6", self.SERVICE, self.PATH, f"{self.INTERFACE}.stopPreview"))
        return result.ok, result.stderr or result.stdout
