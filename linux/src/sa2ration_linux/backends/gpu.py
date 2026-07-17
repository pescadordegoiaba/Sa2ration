from __future__ import annotations

import configparser
import os
import shutil
import tempfile
import time
from pathlib import Path

from sa2ration_linux.command import CommandRunner
from sa2ration_linux.models import DisplaySettings


class KWinGpuEffectBackend:
    """Controls the optional native KWin shader without injecting into apps."""

    name = "KWin GPU Color Pipeline"
    plugin_id = "sa2ration_gpu"
    service = "org.kde.KWin"
    path = "/Effects"
    interface = "org.kde.kwin.Effects"

    def __init__(self, runner: CommandRunner | None = None, config_path: Path | None = None) -> None:
        self.runner = runner or CommandRunner()
        config_home = Path(os.environ.get("XDG_CONFIG_HOME", Path.home() / ".config"))
        self.config_path = config_path or config_home / "sa2ration-linux" / "gpu.ini"

    def _call(self, method: str, *arguments: str):
        return self.runner.run(("qdbus6", self.service, self.path, f"{self.interface}.{method}", *arguments))

    def _property(self, name: str):
        return self.runner.run((
            "qdbus6", self.service, self.path,
            "org.freedesktop.DBus.Properties.Get", self.interface, name,
        ))

    def compositing_supported(self) -> bool:
        if shutil.which("qdbus6") is None:
            return False
        info = self.runner.run(("qdbus6", self.service, "/KWin", "org.kde.KWin.supportInformation"))
        return info.ok and "Compositing Type: OpenGL" in info.stdout

    def installed(self) -> bool:
        result = self._property("listOfEffects")
        return result.ok and self.plugin_id in result.stdout.splitlines()

    def available(self) -> bool:
        if not self.compositing_supported() or not self.installed():
            return False
        result = self._call("isEffectSupported", self.plugin_id)
        return result.ok and result.stdout.strip().lower() == "true"

    def status_reason(self) -> str:
        if not self.compositing_supported():
            return "KWin com composição OpenGL não detectado"
        if not self.installed():
            return "Efeito GPU não instalado ou a sessão do Plasma ainda não foi reiniciada"
        if not self.available():
            return "O KWin recusou o efeito GPU neste backend gráfico"
        return "Shader global por janela disponível"

    @staticmethod
    def is_dangerous(settings: DisplaySettings) -> bool:
        if not settings.gpu_enabled:
            return False
        return (
            settings.gpu_brightness < 0.2
            or settings.gpu_brightness > 2.0
            or settings.gpu_contrast < 0.15
            or settings.gpu_contrast > 2.5
            or settings.gpu_saturation > 3.0
            or abs(settings.gpu_offset) > 0.4
        )

    @staticmethod
    def _entries(settings: DisplaySettings, prefix: str = "") -> dict[str, str]:
        value = settings.normalized()
        return {
            f"{prefix}Enabled": "true" if value.gpu_enabled else "false",
            f"{prefix}Brightness": f"{value.gpu_brightness:.6f}",
            f"{prefix}Contrast": f"{value.gpu_contrast:.6f}",
            f"{prefix}Saturation": f"{value.gpu_saturation:.6f}",
            f"{prefix}Offset": f"{value.gpu_offset:.6f}",
        }

    def _write(self, current: DisplaySettings, stable: DisplaySettings, temporary_seconds: int = 0) -> None:
        self.config_path.parent.mkdir(parents=True, exist_ok=True)
        parser = configparser.ConfigParser()
        parser.optionxform = str
        values = self._entries(current)
        values.update(self._entries(stable, "Stable"))
        values["TemporaryUntilMs"] = str(int((time.time() + temporary_seconds) * 1000)) if temporary_seconds else "0"
        parser["Effect"] = values
        descriptor, temporary = tempfile.mkstemp(prefix="gpu-", suffix=".ini", dir=self.config_path.parent)
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
                parser.write(stream, space_around_delimiters=False)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, self.config_path)
        finally:
            try:
                os.unlink(temporary)
            except FileNotFoundError:
                pass

    def apply(self, current: DisplaySettings, stable: DisplaySettings, temporary_seconds: int = 0) -> tuple[bool, str]:
        if current.gpu_enabled and not self.available():
            return False, self.status_reason()
        try:
            self._write(current, stable, temporary_seconds)
        except OSError as error:
            return False, str(error)
        if not current.gpu_enabled:
            if self.installed():
                self._call("reconfigureEffect", self.plugin_id)
                self._call("unloadEffect", self.plugin_id)
            return True, "Processamento GPU desligado"
        loaded = self._call("isEffectLoaded", self.plugin_id)
        if not loaded.ok or loaded.stdout.strip().lower() != "true":
            loaded = self._call("loadEffect", self.plugin_id)
            if not loaded.ok or loaded.stdout.strip().lower() != "true":
                return False, loaded.stderr or loaded.stdout or "KWin não carregou o efeito GPU"
        result = self._call("reconfigureEffect", self.plugin_id)
        if not result.ok:
            return False, result.stderr or result.stdout or "KWin não reconfigurou o efeito GPU"
        return True, "Brilho, contraste e saturação aplicados pela GPU"

    def confirm(self, settings: DisplaySettings) -> tuple[bool, str]:
        return self.apply(settings, settings, 0)

    def reset(self, monitor_id: str = "") -> tuple[bool, str]:
        neutral = DisplaySettings(monitor_id=monitor_id)
        return self.apply(neutral, neutral, 0)
