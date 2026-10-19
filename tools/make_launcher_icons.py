"""Generates the Xplay Launcher icon and Android TV banner PNGs."""
import sys
from PIL import Image, ImageDraw, ImageFont

out = sys.argv[1]
BG, ACCENT = (8, 14, 30), (59, 130, 246)


def grid(d, x, y, size):
    cell, gap = size * 0.42, size * 0.16
    for i in range(2):
        for j in range(2):
            x0, y0 = x + i * (cell + gap), y + j * (cell + gap)
            d.rounded_rectangle((x0, y0, x0 + cell, y0 + cell), radius=cell * 0.28, fill="white")


icon = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
d = ImageDraw.Draw(icon)
d.rounded_rectangle((8, 8, 184, 184), radius=40, fill=ACCENT)
grid(d, 50, 50, 92)
icon.save(f"{out}/ic_launcher.png")

banner = Image.new("RGB", (320, 180), BG)
d = ImageDraw.Draw(banner)
d.rounded_rectangle((20, 50, 100, 130), radius=18, fill=ACCENT)
grid(d, 38, 68, 44)
try:
    bold = ImageFont.truetype("DejaVuSans-Bold.ttf", 44)
    small = ImageFont.truetype("DejaVuSans.ttf", 20)
except OSError:
    bold = small = ImageFont.load_default()
d.text((116, 56), "Xplay", fill="white", font=bold)
d.text((118, 106), "launcher", fill=ACCENT, font=small)
banner.save(f"{out}/banner.png")
