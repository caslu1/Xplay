"""Gera o ícone e o banner de Android TV do jogo de damas."""
import sys
from PIL import Image, ImageDraw, ImageFont

out = sys.argv[1]

def board(d, x, y, size):
    cell = size / 4
    for r in range(4):
        for c in range(4):
            col = (122, 75, 42) if (r + c) % 2 else (240, 217, 181)
            d.rectangle((x + c * cell, y + r * cell, x + (c + 1) * cell, y + (r + 1) * cell), fill=col)

def piece(d, cx, cy, r, white):
    d.ellipse((cx - r + 3, cy - r + 5, cx + r + 3, cy + r + 5), fill=(0, 0, 0, 110))
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(250, 245, 230) if white else (30, 30, 30),
              outline=(180, 165, 135) if white else (0, 0, 0), width=3)
    d.ellipse((cx - r * .7, cy - r * .7, cx + r * .7, cy + r * .7), outline=(180, 165, 135) if white else (80, 80, 80), width=3)

icon = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
mask = Image.new("L", (192, 192), 0)
ImageDraw.Draw(mask).rounded_rectangle((8, 8, 184, 184), radius=36, fill=255)
layer = Image.new("RGBA", (192, 192), (93, 58, 26, 255))
d = ImageDraw.Draw(layer)
board(d, 20, 20, 152)
piece(d, 20 + 38 * 1.5, 20 + 38 * 2.5, 30, True)
piece(d, 20 + 38 * 2.5, 20 + 38 * 1.5, 30, False)
icon.paste(layer, (0, 0), mask)
icon.save(f"{out}/ic_launcher.png")

banner = Image.new("RGB", (320, 180), (27, 27, 27))
d = ImageDraw.Draw(banner, "RGBA")
board(d, 20, 30, 120)
piece(d, 20 + 30 * 1.5, 30 + 30 * 2.5, 24, True)
piece(d, 20 + 30 * 2.5, 30 + 30 * 1.5, 24, False)
try:
    font = ImageFont.truetype("DejaVuSans-Bold.ttf", 46)
except OSError:
    font = ImageFont.load_default()
d.text((158, 62), "Damas", fill=(255, 193, 7), font=font)
banner.save(f"{out}/banner.png")
