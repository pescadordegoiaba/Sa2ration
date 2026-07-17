from __future__ import annotations

from dataclasses import asdict, dataclass, field
from enum import StrEnum
import math
from typing import Any


def _finite(value: object, fallback: float, minimum: float, maximum: float) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError, OverflowError):
        return fallback
    if not math.isfinite(number):
        return fallback
    return max(minimum, min(maximum, number))


class CapabilityState(StrEnum):
    SUPPORTED = "supported"
    UNSUPPORTED = "unsupported"
    UNAVAILABLE = "unavailable"
    EXPERIMENTAL = "experimental"


@dataclass(frozen=True)
class Capability:
    state: CapabilityState
    backend: str = ""
    reason: str = ""

    @property
    def available(self) -> bool:
        return self.state == CapabilityState.SUPPORTED


@dataclass(frozen=True)
class DisplayMode:
    id: str
    width: int
    height: int
    refresh_hz: float
    preferred: bool = False

    @property
    def label(self) -> str:
        suffix = " (preferido)" if self.preferred else ""
        return f"{self.width} × {self.height} @ {self.refresh_hz:.2f} Hz{suffix}"


@dataclass
class MonitorInfo:
    id: str
    connector: str
    name: str
    manufacturer: str = "Desconhecido"
    serial: str = ""
    panel_technology: str = "LCD (provável)"
    technology_confidence: int = 35
    current_mode_id: str = ""
    modes: list[DisplayMode] = field(default_factory=list)
    brightness: float | None = None
    enabled: bool = True
    primary: bool = False


@dataclass
class DisplayCapabilities:
    brightness: Capability
    temperature: Capability
    resolution: Capability
    refresh_rate: Capability
    physical_brightness: Capability
    physical_contrast: Capability
    gamma: Capability
    gpu_brightness: Capability
    gpu_contrast: Capability
    gpu_saturation: Capability


@dataclass
class DisplaySettings:
    monitor_id: str
    brightness: float = 100.0
    temperature: int = 6500
    mode_id: str = ""
    physical_brightness: int | None = None
    physical_contrast: int | None = None
    gpu_enabled: bool = False
    gpu_brightness: float = 1.0
    gpu_contrast: float = 1.0
    gpu_saturation: float = 1.0
    gpu_offset: float = 0.0

    def normalized(self) -> "DisplaySettings":
        return DisplaySettings(
            monitor_id=self.monitor_id,
            brightness=_finite(self.brightness, 100.0, 5.0, 100.0),
            temperature=round(_finite(self.temperature, 6500, 1000, 10000)),
            mode_id=self.mode_id,
            physical_brightness=(None if self.physical_brightness is None else round(_finite(self.physical_brightness, 50, 0, 100))),
            physical_contrast=(None if self.physical_contrast is None else round(_finite(self.physical_contrast, 50, 0, 100))),
            gpu_enabled=bool(self.gpu_enabled),
            gpu_brightness=_finite(self.gpu_brightness, 1.0, 0.0, 10.0),
            gpu_contrast=_finite(self.gpu_contrast, 1.0, 0.0, 10.0),
            gpu_saturation=_finite(self.gpu_saturation, 1.0, 0.0, 10.0),
            gpu_offset=_finite(self.gpu_offset, 0.0, -2.0, 2.0),
        )

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> "DisplaySettings":
        allowed = {key: value[key] for key in cls.__dataclass_fields__ if key in value}
        return cls(**allowed).normalized()


@dataclass(frozen=True)
class ApplyResult:
    success: bool
    messages: tuple[str, ...] = ()
    errors: tuple[str, ...] = ()
    dangerous: bool = False
