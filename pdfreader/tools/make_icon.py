"""Draws app/res/drawable-xxxhdpi/ic_launcher.png (192 px): a red page with text lines."""
import sys
from PIL import Image, ImageDraw

S = 4  # supersample
img = Image.new("RGBA", (192 * S, 192 * S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
def r(v): return [x * S for x in v]
d.rounded_rectangle(r([36, 12, 156, 180]), radius=10 * S, fill="#E53935")
d.polygon(r([118, 10, 158, 10, 158, 50]), fill=(0, 0, 0, 0))
d.polygon(r([120, 12, 156, 48, 120, 48]), fill="#FF8A80")
for y, w in ((84, 84), (108, 84), (132, 54)):
    d.rounded_rectangle(r([54, y, 54 + w, y + 12]), radius=6 * S, fill="white")
img.resize((192, 192), Image.LANCZOS).save(sys.argv[1] if len(sys.argv) > 1 else
    "app/res/drawable-xxxhdpi/ic_launcher.png")
