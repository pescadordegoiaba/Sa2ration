import unittest

from sa2ration_linux.edid import infer_panel_technology, manufacturer_code


class EdidTest(unittest.TestCase):
    def test_decodes_manufacturer(self):
        # DEL = 00010 00101 01100
        self.assertEqual("DEL", manufacturer_code(b"\0" * 8 + bytes((0x10, 0xAC))))

    def test_does_not_invent_specific_lcd_technology(self):
        technology, confidence = infer_panel_technology("DELL E178FP")
        self.assertEqual("LCD (provável)", technology)
        self.assertLess(confidence, 50)

    def test_detects_explicit_oled_name(self):
        self.assertEqual(("OLED", 85), infer_panel_technology("Vendor AMOLED Panel"))


if __name__ == "__main__":
    unittest.main()
