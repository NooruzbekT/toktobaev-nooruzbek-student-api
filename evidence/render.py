#!/usr/bin/env python3
"""Render evidence/*.txt terminal captures into terminal-style PNG screenshots.

Author: Nooruzbek Toktobaev
Usage:  python3 evidence/render.py [part-folder ...]   (default: all parts)
Each <name>.txt becomes <name>.png, or <name>-1.png, <name>-2.png ... for long output.
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

EVIDENCE_DIR = Path(__file__).resolve().parent
FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    "/Library/Fonts/Menlo.ttc",
    "/System/Library/Fonts/Menlo.ttc",
    "C:/Windows/Fonts/consola.ttf",
]
FONT_SIZE = 15
COLS = 130                # wrap long lines at this width
MAX_LINES = 70            # split into several images after this many lines
PAD = 16
TITLE_H = 34

BG = (30, 30, 30)
TITLE_BG = (50, 50, 52)
FG = (212, 212, 212)
USER_HOST = (78, 201, 78)
PATH_COLOR = (97, 175, 239)
CMD_COLOR = (255, 255, 255)
DIM = (150, 150, 150)

ANSI_RE = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]|\x1b\][^\x07]*\x07")
PROMPT_RE = re.compile(r"^(nooruzbek@[^:]+)(:)([^$]*)(\$ )(.*)$")


def load_font():
    for candidate in FONT_CANDIDATES:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, FONT_SIZE)
    return ImageFont.load_default()


def clean(text):
    text = ANSI_RE.sub("", text).replace("\r\n", "\n")
    lines = []
    for raw in text.split("\n"):
        raw = raw.split("\r")[-1].expandtabs(8)  # keep only the final state of progress bars
        if not raw:
            lines.append("")
        while raw:
            lines.append(raw[:COLS])
            raw = raw[COLS:]
    while lines and not lines[-1].strip():
        lines.pop()
    return lines


def draw_page(lines, title, font, out_path):
    char_w = font.getbbox("M")[2]
    line_h = FONT_SIZE + 5
    width = PAD * 2 + char_w * max(80, min(COLS, max((len(l) for l in lines), default=0)))
    height = TITLE_H + PAD * 2 + line_h * max(1, len(lines))
    img = Image.new("RGB", (width, height), BG)
    d = ImageDraw.Draw(img)

    # Window title bar with traffic-light buttons
    d.rectangle([0, 0, width, TITLE_H], fill=TITLE_BG)
    for i, color in enumerate([(255, 95, 86), (255, 189, 46), (39, 201, 63)]):
        x = 14 + i * 22
        d.ellipse([x, 11, x + 12, 23], fill=color)
    d.text((90, 9), title, font=font, fill=DIM)

    y = TITLE_H + PAD
    for line in lines:
        x = PAD
        m = PROMPT_RE.match(line)
        if m:
            for part, color in zip(m.groups(), [USER_HOST, FG, PATH_COLOR, FG, CMD_COLOR]):
                d.text((x, y), part, font=font, fill=color)
                x += char_w * len(part)
        else:
            d.text((x, y), line, font=font, fill=FG)
        y += line_h
    img.save(out_path)


def render_file(txt, font):
    lines = clean(txt.read_text(encoding="utf-8", errors="replace"))
    for old in txt.parent.glob(txt.stem + "*.png"):
        if old.stem == txt.stem or re.fullmatch(re.escape(txt.stem) + r"-\d+", old.stem):
            old.unlink()
    pages = [lines[i:i + MAX_LINES] for i in range(0, len(lines), MAX_LINES)] or [[]]
    outputs = []
    for n, page in enumerate(pages, 1):
        suffix = "" if len(pages) == 1 else f"-{n}"
        title = f"{txt.parent.name}/{txt.stem}" + (f"  ({n}/{len(pages)})" if len(pages) > 1 else "")
        out = txt.with_name(f"{txt.stem}{suffix}.png")
        draw_page(page, title, font, out)
        outputs.append(out)
    return outputs


def main():
    font = load_font()
    parts = sys.argv[1:] or sorted(p.name for p in EVIDENCE_DIR.iterdir() if p.is_dir())
    for part in parts:
        for txt in sorted((EVIDENCE_DIR / part).glob("*.txt")):
            for out in render_file(txt, font):
                print(f"rendered {out.relative_to(EVIDENCE_DIR)}")


if __name__ == "__main__":
    main()
