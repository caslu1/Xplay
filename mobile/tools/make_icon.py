"""Draws the launcher icon: a pink gradient ring open on the right, with a play triangle inside."""
import sys
from PIL import Image, ImageDraw

S = 4  # supersampling
W = 192 * S
out = sys.argv[1]

def gradient(size, a, b):
    g = Image.new("RGBA", (size, size))
    px = g.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size - 2)
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)
    return g

bg = Image.new("RGBA", (W, W), (0, 0, 0, 0))
d = ImageDraw.Draw(bg)
d.rounded_rectangle((0, 0, W - 1, W - 1), radius=44 * S, fill=(14, 12, 16, 255))

mask = Image.new("L", (W, W), 0)
m = ImageDraw.Draw(mask)
c, r, t = W // 2, 66 * S, 26 * S
m.ellipse((c - r, c - r, c + r, c + r), fill=255)
m.ellipse((c - r + t, c - r + t, c + r - t, c + r - t), fill=0)
m.polygon([(c, c), (W, c - 40 * S), (W, c + 40 * S)], fill=0)  # opening
tr = 24 * S
m.polygon([(c - tr * 0.55, c - tr), (c - tr * 0.55, c + tr), (c + tr, c)], fill=255)
bg.paste(gradient(W, (247, 172, 205), (232, 84, 146)), (0, 0), mask)
bg.resize((192, 192), Image.LANCZOS).save(f"{out}/ic_launcher.png")
