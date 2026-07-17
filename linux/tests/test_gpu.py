import configparser
import math
import tempfile
import unittest
from pathlib import Path

from sa2ration_linux.backends.gpu import KWinGpuEffectBackend
from sa2ration_linux.command import CommandResult
from sa2ration_linux.models import DisplaySettings


class FakeRunner:
    def __init__(self):
        self.calls = []

    def run(self, args, timeout=8):
        args = tuple(args)
        self.calls.append(args)
        if any(part.endswith(".isEffectLoaded") for part in args):
            output = "false"
        elif any(part.endswith((".loadEffect", ".isEffectSupported")) for part in args):
            output = "true"
        elif args[-1:] == ("listOfEffects",):
            output = "blur\nsa2ration_gpu\n"
        elif "supportInformation" in args:
            output = "Compositing Type: OpenGL"
        else:
            output = ""
        return CommandResult(args, 0, output, "", False, 1)


class GpuBackendTest(unittest.TestCase):
    def test_writes_current_stable_and_temporary_deadline(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "gpu.ini"
            backend = KWinGpuEffectBackend(FakeRunner(), path)
            backend.available = lambda: True
            backend.installed = lambda: True
            current = DisplaySettings("HDMI-A-1", gpu_enabled=True, gpu_brightness=1.4, gpu_contrast=2.0, gpu_saturation=3.2)
            stable = DisplaySettings("HDMI-A-1", gpu_enabled=True, gpu_brightness=1.0, gpu_contrast=1.1, gpu_saturation=1.2)
            success, _ = backend.apply(current, stable, temporary_seconds=15)
            self.assertTrue(success)
            parser = configparser.ConfigParser()
            parser.optionxform = str
            parser.read(path)
            effect = parser["Effect"]
            self.assertEqual("1.400000", effect["Brightness"])
            self.assertEqual("1.100000", effect["StableContrast"])
            self.assertGreater(int(effect["TemporaryUntilMs"]), 0)

    def test_extreme_gpu_values_require_confirmation(self):
        self.assertFalse(KWinGpuEffectBackend.is_dangerous(DisplaySettings("DP-1", gpu_enabled=False, gpu_contrast=10)))
        self.assertFalse(KWinGpuEffectBackend.is_dangerous(DisplaySettings("DP-1", gpu_enabled=True, gpu_contrast=1.5)))
        self.assertTrue(KWinGpuEffectBackend.is_dangerous(DisplaySettings("DP-1", gpu_enabled=True, gpu_contrast=8)))
        self.assertTrue(KWinGpuEffectBackend.is_dangerous(DisplaySettings("DP-1", gpu_enabled=True, gpu_brightness=0)))

    def test_non_finite_values_become_neutral(self):
        normalized = DisplaySettings(
            "DP-1",
            brightness=math.nan,
            gpu_enabled=True,
            gpu_brightness=math.inf,
            gpu_contrast=math.nan,
            gpu_saturation=-math.inf,
        ).normalized()
        self.assertEqual(100, normalized.brightness)
        self.assertEqual(1, normalized.gpu_brightness)
        self.assertEqual(1, normalized.gpu_contrast)
        self.assertEqual(1, normalized.gpu_saturation)


if __name__ == "__main__":
    unittest.main()
