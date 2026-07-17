from __future__ import annotations

import signal
import time

from sa2ration_linux.controller import DisplayController


def run_daemon() -> int:
    """Restores stable state and keeps KWin's safe preview temperature alive."""
    controller = DisplayController()
    if not controller.monitors:
        # A login without the expected compositor must not create a restart loop.
        return 0
    monitor = next((item for item in controller.monitors if controller.stable and item.id == controller.stable.monitor_id), controller.monitors[0])
    settings = controller.stable or controller.settings_for(monitor)
    result = controller.apply(monitor, settings, confirm=True)
    if not result.success:
        return 0
    running = True

    def stop(*_: object) -> None:
        nonlocal running
        running = False

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    while running:
        time.sleep(10)
        if controller.manager.display_backend is controller.manager.kscreen and settings.temperature < 6500:
            controller.manager.kwin.apply_temperature(settings.temperature)
    return 0
