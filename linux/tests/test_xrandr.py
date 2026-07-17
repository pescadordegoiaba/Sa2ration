import unittest

from sa2ration_linux.backends.xrandr import XRandRBackend, kelvin_to_gamma
from sa2ration_linux.command import CommandResult


XRANDR = """Screen 0: minimum 8 x 8, current 1920 x 1080
HDMI-1 connected primary 1920x1080+0+0
   1920x1080     60.00*+  59.94
   1280x720      60.00
"""


class FakeRunner:
    def run(self, args, timeout=8):
        return CommandResult(tuple(args), 0, XRANDR, "", False, 1)


class XRandRTest(unittest.TestCase):
    def test_neutral_gamma(self):
        red, green, blue = kelvin_to_gamma(6500)
        self.assertAlmostEqual(1.0, red, places=2)
        self.assertAlmostEqual(1.0, green, places=2)
        self.assertAlmostEqual(1.0, blue, places=2)

    def test_only_available_on_real_x11_session(self):
        self.assertFalse(XRandRBackend(FakeRunner(), {"XDG_SESSION_TYPE": "wayland"}).available())

    def test_parses_modes(self):
        backend = XRandRBackend(FakeRunner(), {"XDG_SESSION_TYPE": "x11"})
        backend.available = lambda: True
        monitor = backend.detect()[0]
        self.assertEqual("HDMI-1", monitor.connector)
        self.assertEqual(3, len(monitor.modes))
        self.assertEqual("1920x1080@60.00", monitor.current_mode_id)


if __name__ == "__main__":
    unittest.main()
