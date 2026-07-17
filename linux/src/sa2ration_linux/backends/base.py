from __future__ import annotations

from abc import ABC, abstractmethod

from sa2ration_linux.models import ApplyResult, DisplaySettings, MonitorInfo


class DisplayBackend(ABC):
    name = "Indisponível"

    @abstractmethod
    def available(self) -> bool:
        raise NotImplementedError

    @abstractmethod
    def detect(self) -> list[MonitorInfo]:
        raise NotImplementedError

    @abstractmethod
    def apply(self, monitor: MonitorInfo, settings: DisplaySettings) -> ApplyResult:
        raise NotImplementedError
