import json
import unittest

from sa2ration_linux.backends.kscreen import KScreenBackend
from sa2ration_linux.command import CommandResult
from sa2ration_linux.models import DisplaySettings


class FakeRunner:
    def __init__(self, payload):
        self.payload = payload
        self.calls = []

    def run(self, args, timeout=8):
        self.calls.append(tuple(args))
        output = json.dumps(self.payload) if tuple(args) == ("kscreen-doctor", "--json") else "done"
        return CommandResult(tuple(args), 0, output, "", False, 1)


PAYLOAD = {"outputs": [{
    "id": 1, "name": "HDMI-A-2", "connected": True, "enabled": True,
    "brightness": 0.8, "currentModeId": "2", "preferredModes": ["1"],
    "modes": [
        {"id": "1", "size": {"width": 1280, "height": 1024}, "refreshRate": 60.02},
        {"id": "2", "size": {"width": 1280, "height": 1024}, "refreshRate": 75.03},
    ],
}]}


class KScreenTest(unittest.TestCase):
    def test_parses_connected_monitor_and_modes(self):
        backend = KScreenBackend(FakeRunner(PAYLOAD))
        backend.available = lambda: True
        monitor = backend.detect()[0]
        self.assertEqual("HDMI-A-2", monitor.connector)
        self.assertEqual(80, monitor.brightness)
        self.assertEqual(2, len(monitor.modes))
        self.assertTrue(monitor.modes[0].preferred)

    def test_builds_argument_list_without_shell(self):
        runner = FakeRunner(PAYLOAD)
        backend = KScreenBackend(runner)
        backend.available = lambda: True
        monitor = backend.detect()[0]
        result = backend.apply(monitor, DisplaySettings(monitor.id, 70, 6500, "1"))
        self.assertTrue(result.success)
        self.assertEqual(("kscreen-doctor", "output.HDMI-A-2.brightness.70.00", "output.HDMI-A-2.mode.1"), runner.calls[-1])
        self.assertTrue(result.dangerous)

    def test_rejects_unknown_mode(self):
        backend = KScreenBackend(FakeRunner(PAYLOAD))
        backend.available = lambda: True
        monitor = backend.detect()[0]
        self.assertFalse(backend.apply(monitor, DisplaySettings(monitor.id, 70, 6500, "evil;id")).success)


if __name__ == "__main__":
    unittest.main()
