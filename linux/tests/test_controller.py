import tempfile
import unittest
from pathlib import Path

from sa2ration_linux.controller import StateStore
from sa2ration_linux.models import DisplaySettings


class StateStoreTest(unittest.TestCase):
    def test_round_trip_current_and_stable_state(self):
        with tempfile.TemporaryDirectory() as directory:
            store = StateStore(Path(directory) / "state.json")
            current = DisplaySettings("HDMI-A-1", 45, 3200, "2")
            stable = DisplaySettings("HDMI-A-1", 80, 5000, "1")
            store.save(current, stable)
            loaded = store.load()
            self.assertEqual(45, loaded["current"]["brightness"])
            self.assertEqual(5000, loaded["stable"]["temperature"])

    def test_extreme_values_are_clamped(self):
        settings = DisplaySettings("DP-1", -5, 50000, physical_contrast=900).normalized()
        self.assertEqual(5, settings.brightness)
        self.assertEqual(10000, settings.temperature)
        self.assertEqual(100, settings.physical_contrast)


if __name__ == "__main__":
    unittest.main()
