from __future__ import annotations

from pathlib import Path


def _descriptor(edid: bytes, tag: int) -> str:
    for offset in range(54, min(len(edid), 126), 18):
        block = edid[offset : offset + 18]
        if len(block) == 18 and block[:3] == b"\x00\x00\x00" and block[3] == tag:
            return block[5:18].decode("ascii", errors="ignore").replace("\x00", "").strip()
    return ""


def manufacturer_code(edid: bytes) -> str:
    if len(edid) < 10:
        return ""
    value = int.from_bytes(edid[8:10], "big")
    return "".join(chr(((value >> shift) & 0x1F) + 64) for shift in (10, 5, 0))


def read_edid(connector: str, root: Path = Path("/sys/class/drm")) -> dict[str, str]:
    candidates = sorted(root.glob(f"card*-{connector}/edid"))
    if not candidates:
        return {}
    try:
        raw = candidates[0].read_bytes()
    except OSError:
        return {}
    if len(raw) < 128 or raw[:8] != b"\x00\xff\xff\xff\xff\xff\xff\x00":
        return {}
    return {
        "manufacturer": manufacturer_code(raw),
        "name": _descriptor(raw, 0xFC),
        "serial": _descriptor(raw, 0xFF),
    }


def infer_panel_technology(name: str) -> tuple[str, int]:
    lowered = name.lower()
    if any(token in lowered for token in ("oled", "amoled", "poled")):
        return "OLED", 85
    if "mini-led" in lowered or "miniled" in lowered:
        return "Mini-LED LCD", 80
    if any(token in lowered for token in ("ips", "pls", "tft", "lcd")):
        return "LCD", 75
    return "LCD (provável)", 35
