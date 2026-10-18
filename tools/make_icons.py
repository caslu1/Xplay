"""Generates the launcher icon and Android TV banner PNGs."""
import sys
from PIL import Image, ImageDraw, ImageFont

out = sys.argv[1]
BG, ACCENT = (30, 30, 46), (232, 70, 58)

def play_triangle(d, cx, cy, r):
    d.polygon([(cx - r * 0.6, cy - r), (cx - r * 0.6, cy + r), (cx + r, cy)], fill="white")

icon = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
d = ImageDraw.Draw(icon)
d.rounded_rectangle((8, 8, 184, 184), radius=40, fill=ACCENT)
play_triangle(d, 100, 96, 46)
icon.save(f"{out}/ic_launcher.png")

banner = Image.new("RGB", (320, 180), BG)
d = ImageDraw.Draw(banner)
d.rounded_rectangle((24, 50, 104, 130), radius=18, fill=ACCENT)
play_triangle(d, 68, 90, 24)
try:
    font = ImageFont.truetype("DejaVuSans-Bold.ttf", 56)
except OSError:
    font = ImageFont.load_default()
d.text((122, 58), "Xplay", fill="white", font=font)
banner.save(f"{out}/banner.png")
