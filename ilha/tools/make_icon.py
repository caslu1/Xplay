"""Draws the launcher icon: a black island with a lens and level bars on a mint→violet tile."""
import os
from PIL import Image, ImageDraw

SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144}
S = 1024

def icon():
    grad = Image.new("RGBA", (S, S))
    px = grad.load()
    a, b = (124, 242, 196), (150, 120, 255)
    for y in range(S):
        for x in range(S):
            t = (x + y) / (2 * S)
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle((40, 40, S - 40, S - 40), radius=230, fill=255)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    # island
    d.rounded_rectangle((170, 400, 854, 624), radius=112, fill=(10, 10, 14, 255))
    # lens
    d.ellipse((452, 452, 572, 572), fill=(38, 38, 48, 255))
    d.ellipse((484, 484, 540, 540), fill=(70, 80, 120, 255))
    d.ellipse((495, 492, 515, 512), fill=(170, 190, 255, 255))
    # art dot
    d.ellipse((222, 448, 350, 576), fill=(124, 242, 196, 255))
    # bars
    for i, hgt in enumerate((60, 110, 80, 130)):
        x = 668 + i * 34
        d.rounded_rectangle((x, 512 - hgt // 2, x + 18, 512 + hgt // 2), radius=9, fill=(150, 120, 255, 255))
    return img

base = icon()
root = os.path.join(os.path.dirname(__file__), "..", "app", "res")
for k, s in SIZES.items():
    out = os.path.join(root, "drawable-" + k)
    os.makedirs(out, exist_ok=True)
    base.resize((s, s), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"))
print("ok")
