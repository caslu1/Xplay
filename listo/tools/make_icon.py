# Draws the launcher icon: a rounded blue square with a white check mark.
import sys
from PIL import Image, ImageDraw
S = 4 * 144
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
top, bot = (138, 170, 240), (95, 134, 230)
for y in range(S):
    t = y / S
    c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,)
    d.line([(0, y), (S, y)], fill=c)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).rounded_rectangle([16, 16, S - 16, S - 16], radius=130, fill=255)
img.putalpha(mask)
d = ImageDraw.Draw(img)
w = 46
d.line([(150, 300), (250, 400), (430, 200)], fill="white", width=w, joint="curve")
for p in [(150, 300), (430, 200)]:
    d.ellipse([p[0] - w / 2, p[1] - w / 2, p[0] + w / 2, p[1] + w / 2], fill="white")
img.resize((144, 144), Image.LANCZOS).save(sys.argv[1])
