#!/usr/bin/env python3
"""Vertical (9:16) map animation on real satellite imagery.

The border of a country is traced over NASA Blue Marble tiles, the country
fills with colour, its flag fades in over the terrain and the capital pops up
with a label - the style of the "map animation" shorts.

    python3 make_map_video.py China
    python3 make_map_video.py Brazil --color "#009c3b" --border "#ffdf00"
    python3 make_map_video.py "United States of America" --title EUA

Needs: numpy, pillow, cairosvg, ffmpeg. Data is downloaded once into --cache.
"""
import argparse
import io
import json
import math
import os
import subprocess
import sys
import tarfile
import urllib.request
from concurrent.futures import ThreadPoolExecutor

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

NE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/"
COUNTRIES_URL = NE + "ne_50m_admin_0_countries.geojson"
PLACES_URL = NE + "ne_50m_populated_places.geojson"
TILE_URL = "https://s3.amazonaws.com/com.modestmaps.bluemarble/{z}-r{y}-c{x}.jpg"
FLAGS_URL = "https://registry.npmjs.org/flag-icons/-/flag-icons-7.6.0.tgz"
TILE = 256
MAX_ZOOM = 8
FONT = "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf"
FONT_LABEL = "/usr/share/fonts/opentype/inter/Inter-Bold.otf"


# ---------------------------------------------------------------- download

def fetch(url, path):
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with urllib.request.urlopen(url, timeout=60) as r:
            data = r.read()
        with open(path + ".part", "wb") as f:
            f.write(data)
        os.replace(path + ".part", path)
    return path


def load_json(url, cache):
    return json.load(open(fetch(url, os.path.join(cache, os.path.basename(url)))))


def flag_image(iso2, cache, width):
    tgz = fetch(FLAGS_URL, os.path.join(cache, "flag-icons.tgz"))
    with tarfile.open(tgz) as t:
        svg = t.extractfile("package/flags/4x3/%s.svg" % iso2.lower()).read()
    import cairosvg
    png = cairosvg.svg2png(bytestring=svg, output_width=width)
    return Image.open(io.BytesIO(png)).convert("RGB")


# ---------------------------------------------------------------- geometry

def merc(lon, lat, z):
    """Lon/lat (degrees) to Web Mercator pixels at zoom z."""
    n = TILE * 2 ** z
    lat = np.clip(lat, -85.0, 85.0)
    x = (lon + 180.0) / 360.0 * n
    y = (1 - np.log(np.tan(np.radians(lat)) + 1 / np.cos(np.radians(lat))) / math.pi) / 2 * n
    return x, y


def find_country(features, query):
    q = query.strip().lower()
    keys = ("NAME", "NAME_LONG", "ADMIN", "NAME_PT", "NAME_EN", "ADM0_A3", "ISO_A2", "ISO_A3", "SOVEREIGNT")
    for f in features:
        if any(str(f["properties"].get(k, "")).lower() == q for k in keys):
            return f
    for f in features:
        if q in str(f["properties"].get("NAME_LONG", "")).lower():
            return f
    sys.exit("Country not found: %s" % query)


def outer_rings(geom):
    polys = geom["coordinates"] if geom["type"] == "MultiPolygon" else [geom["coordinates"]]
    return [np.array(p[0], dtype=float) for p in polys]


def ring_area(r):
    x, y = r[:, 0], r[:, 1]
    return abs(np.dot(x, np.roll(y, 1)) - np.dot(y, np.roll(x, 1))) / 2


# ---------------------------------------------------------------- satellite

def mosaic(z, x0, y0, x1, y1, cache, grade=True):
    """Stitch Blue Marble tiles covering world pixels [x0,x1) x [y0,y1)."""
    n = 2 ** z
    tx0, ty0 = int(x0 // TILE), int(y0 // TILE)
    tx1, ty1 = int(math.ceil(x1 / TILE)), int(math.ceil(y1 / TILE))
    img = Image.new("RGB", ((tx1 - tx0) * TILE, (ty1 - ty0) * TILE), (8, 18, 40))

    def get(tx, ty):
        if not 0 <= ty < n:
            return None
        x = tx % n
        path = os.path.join(cache, "tiles", str(z), "%d-%d.jpg" % (ty, x))
        try:
            return tx, ty, Image.open(fetch(TILE_URL.format(z=z, y=ty, x=x), path)).convert("RGB")
        except Exception as e:  # missing tile: keep the ocean colour
            print("tile %d/%d/%d: %s" % (z, x, ty, e), file=sys.stderr)
            return None

    jobs = [(tx, ty) for ty in range(ty0, ty1) for tx in range(tx0, tx1)]
    print("satellite: %d tiles at zoom %d" % (len(jobs), z))
    with ThreadPoolExecutor(16) as ex:
        for r in ex.map(lambda j: get(*j), jobs):
            if r:
                tx, ty, t = r
                img.paste(t, ((tx - tx0) * TILE, (ty - ty0) * TILE))
    if grade:
        img = Image.fromarray(grade_array(np.asarray(img)))
    return img, tx0 * TILE, ty0 * TILE


def grade_array(rgb):
    """Blue Marble is bright and flat; push it towards the darker,
    contrasty look of satellite basemaps. uint8 in, uint8 out."""
    a = rgb.astype(np.float32) / 255
    a = np.clip((a - 0.04) * 1.18, 0, 1) ** 1.25
    lum = a.mean(axis=2, keepdims=True)
    a = np.clip(lum + (a - lum) * 1.15, 0, 1) * 0.92
    return (a * 255).astype(np.uint8)


# ---------------------------------------------------------------- easing

def clamp01(t):
    return max(0.0, min(1.0, t))


def phase(t, a, b):
    return clamp01((t - a) / (b - a))


def smooth(t):
    return t * t * (3 - 2 * t)


def ease_out(t):
    return 1 - (1 - t) ** 3


def back_out(t):
    c = 1.9
    return 1 + (c + 1) * (t - 1) ** 3 + c * (t - 1) ** 2


def hexcolor(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


# ---------------------------------------------------------------- rendering

class Scene:
    def __init__(self, a):
        self.a = a
        self.W, self.H = a.width, a.height
        cache = a.cache

        countries = load_json(COUNTRIES_URL, cache)["features"]
        feat = find_country(countries, a.country)
        p = feat["properties"]
        self.iso2 = p["ISO_A2"] if p["ISO_A2"] != "-99" else p["ISO_A2_EH"]
        rings = outer_rings(feat["geometry"])

        # Countries across the antimeridian (Russia, Fiji...): keep them in one piece.
        lons = np.concatenate([r[:, 0] for r in rings])
        if lons.max() - lons.min() > 180:
            for r in rings:
                r[r[:, 0] < 0, 0] += 360

        self.title = a.title or (p.get("NAME_PT") if a.lang == "pt" else p["NAME"]) or p["NAME"]
        self.title = self.title.upper()

        cap = None
        if not a.no_capital:
            for f in load_json(PLACES_URL, cache)["features"]:
                q = f["properties"]
                if q.get("ADM0_A3") == p["ADM0_A3"] and q.get("ADM0CAP") == 1:
                    cap = q
                    break
        self.capital = None
        if cap:
            lon = cap["LONGITUDE"] + (360 if lons.max() - lons.min() > 180 and cap["LONGITUDE"] < 0 else 0)
            name = a.capital or (cap.get("NAME_PT") if a.lang == "pt" else None) or cap["NAME"]
            self.capital = (lon, cap["LATITUDE"], name)

        # Frame on the main landmass (plus anything close to it), not on far
        # away islands or territories.
        main = max(rings, key=ring_area)
        mx0, my0 = main.min(axis=0)
        mx1, my1 = main.max(axis=0)
        sx, sy = (mx1 - mx0) * 0.25, (my1 - my0) * 0.25
        if a.frame_all:
            near = rings
        else:
            near = [r for r in rings if r[:, 0].min() > mx0 - sx and r[:, 0].max() < mx1 + sx
                    and r[:, 1].min() > my0 - sy and r[:, 1].max() < my1 + sy]

        # Pick the zoom level from the size the country has on screen at the end.
        def bbox(z, rs):
            xs, ys = zip(*(merc(r[:, 0], r[:, 1], z) for r in rs))
            xs, ys = np.concatenate(xs), np.concatenate(ys)
            return xs.min(), ys.min(), xs.max(), ys.max()

        fit_w, fit_h = self.W * 0.84, self.H * 0.56
        z = 1
        while z < MAX_ZOOM:
            bx0, by0, bx1, by1 = bbox(z, near)
            if min(fit_w / (bx1 - bx0), fit_h / (by1 - by0)) * a.zoom_end <= 1.15:
                break
            z += 1
        self.z = z
        bx0, by0, bx1, by1 = bbox(z, near)
        self.scale_end = min(fit_w / (bx1 - bx0), fit_h / (by1 - by0)) * a.zoom_end
        self.scale_start = self.scale_end / a.zoom_start
        self.center = ((bx0 + bx1) / 2, (by0 + by1) / 2 + (by1 - by0) * 0.04)

        # Satellite mosaic covering the widest camera.
        hw, hh = self.W / 2 / self.scale_start * 1.02, self.H / 2 / self.scale_start * 1.02
        cx, cy = self.center
        world = TILE * 2 ** z
        wy0, wy1 = max(0, cy - hh), min(world, cy + hh)
        self.base, self.ox, self.oy = mosaic(z, cx - hw, wy0, cx + hw, wy1, os.path.join(cache, "bluemarble"))
        self.center = (cx - self.ox, cy - self.oy)  # everything below is in mosaic pixels

        # Rings in mosaic pixels, plus cumulative lengths for the line drawing.
        self.rings = []
        for r in rings:
            x, y = merc(r[:, 0], r[:, 1], z)
            pts = np.stack([x - self.ox, y - self.oy], axis=1)
            seg = np.hypot(*np.diff(pts, axis=0).T)
            self.rings.append((pts, np.concatenate([[0], np.cumsum(seg)])))
        self.longest = max(cum[-1] for _, cum in self.rings)
        nx = np.concatenate([merc(r[:, 0], r[:, 1], z)[0] for r in near]) - self.ox
        ny = np.concatenate([merc(r[:, 0], r[:, 1], z)[1] for r in near]) - self.oy
        self.main_box = (nx.min(), ny.min(), nx.max(), ny.max())
        if self.capital:
            x, y = merc(np.array(self.capital[0]), np.array(self.capital[1]), z)
            self.cap_px = (float(x) - self.ox, float(y) - self.oy)

        self.fill = hexcolor(a.color)
        self.border = hexcolor(a.border)
        self.flag = None
        if not a.no_flag and self.iso2 and self.iso2 != "-99":
            try:
                self.flag = flag_image(self.iso2, cache, 2000)
            except KeyError:
                print("no flag for %s" % self.iso2, file=sys.stderr)

        yy, xx = np.mgrid[0:self.H, 0:self.W]
        d = np.hypot((xx - self.W / 2) / (self.W * 0.75), (yy - self.H / 2) / (self.H * 0.7))
        self.vignette = (1 - 0.55 * np.clip(d - 0.35, 0, 1) ** 1.5)[..., None].astype(np.float32)

        self.font_title = ImageFont.truetype(FONT, int(self.W * 0.12))
        self.font_label = ImageFont.truetype(FONT_LABEL, int(self.W * 0.042))

    # camera: screen = (base - center) * s + screen_center
    def camera(self, t):
        k = smooth(t)
        s = self.scale_start * (self.scale_end / self.scale_start) ** k
        return s, self.center

    def to_screen(self, pts, s, c, ss=1):
        return (pts - np.array(c) + [self.ox * 0, 0]) * s * ss + np.array([self.W, self.H]) * ss / 2

    def render(self, i, n):
        a = self.a
        T = i / a.fps
        t = i / (n - 1)
        s, c = self.camera(t)
        W, H = self.W, self.H

        # Satellite crop for this camera.
        x0, y0 = c[0] - W / 2 / s, c[1] - H / 2 / s
        x1, y1 = c[0] + W / 2 / s, c[1] + H / 2 / s
        # crop() pads outside the mosaic (poles), resize() then takes the sub-pixel box
        ix0, iy0 = math.floor(x0), math.floor(y0)
        part = self.base.crop((ix0, iy0, math.ceil(x1) + 1, math.ceil(y1) + 1))
        sat = part.resize((W, H), Image.BILINEAR, box=(x0 - ix0, y0 - iy0, x1 - ix0, y1 - iy0), reducing_gap=2.0)
        img = np.asarray(sat).astype(np.float32) / 255

        # Timeline (seconds).
        draw_t = ease_out(phase(T, 0.8, 3.6))
        fill_t = smooth(phase(T, 3.4, 4.4))
        flag_t = smooth(phase(T, 4.6, 5.8)) if self.flag else 0
        cap_t = phase(T, 6.0, 6.7)
        title_t = ease_out(phase(T, 0.3, 1.3))

        SS = 2
        poly_screen = [self.to_screen(p, s, c, SS) for p, _ in self.rings]

        if fill_t > 0:
            m = Image.new("L", (W * SS, H * SS), 0)
            d = ImageDraw.Draw(m)
            for p in poly_screen:
                d.polygon([tuple(q) for q in p], fill=255)
            mask = np.asarray(m.resize((W, H), Image.BOX)).astype(np.float32)[..., None] / 255

            lum = img.mean(axis=2, keepdims=True)
            shade = 0.45 + 0.9 * lum  # keep the relief visible through the colour
            outside = 1 - 0.38 * fill_t * (1 - mask)
            img = img * outside

            colour = np.array(self.fill, np.float32) / 255 * shade
            if flag_t > 0:
                bx0, by0, bx1, by1 = self.main_box
                fx0, fy0 = (np.array([bx0, by0]) - c) * s + [W / 2, H / 2]
                fx1, fy1 = (np.array([bx1, by1]) - c) * s + [W / 2, H / 2]
                fw, fh = int(fx1 - fx0) + 2, int(fy1 - fy0) + 2
                flag = self.flag.resize((max(fw, 1), max(fh, 1)), Image.BILINEAR)
                layer = Image.new("RGB", (W, H), self.fill)
                layer.paste(flag, (int(fx0), int(fy0)))
                fl = np.asarray(layer).astype(np.float32) / 255 * shade
                colour = colour * (1 - flag_t) + fl * flag_t
            alpha = mask * fill_t * 0.82
            img = img * (1 - alpha) + np.clip(colour, 0, 1) * alpha

        img = img * self.vignette

        # Border: glow + core line drawn up to draw_t of each ring's length.
        if draw_t > 0:
            core = Image.new("L", (W * SS, H * SS), 0)
            dc = ImageDraw.Draw(core)
            heads = []
            for (pts, cum), p in zip(self.rings, poly_screen):
                L = cum[-1] * draw_t
                k = int(np.searchsorted(cum, L))
                if k == 0:
                    continue
                if k < len(cum):
                    f = (L - cum[k - 1]) / max(cum[k] - cum[k - 1], 1e-9)
                    end = p[k - 1] + (p[k] - p[k - 1]) * f
                    line = np.vstack([p[:k], end])
                    if cum[-1] > 0.2 * self.longest:  # no pen tip on small islands
                        heads.append(end / SS)
                else:
                    line = p
                if len(line) > 1:
                    dc.line([tuple(q) for q in line], fill=255, width=int(a.line * SS), joint="curve")
            core = core.resize((W, H), Image.BOX)
            pulse = 0.85 + 0.15 * math.sin(T * 4) if draw_t >= 1 else 1
            glow = core.filter(ImageFilter.GaussianBlur(a.line * 2.6))
            g = np.asarray(glow).astype(np.float32)[..., None] / 255
            k = np.asarray(core).astype(np.float32)[..., None] / 255
            col = np.array(self.border, np.float32) / 255
            img = img + col * np.clip(g * 2.2 * pulse, 0, 1) * 0.9
            img = img * (1 - k) + (col * 0.35 + 0.65 * np.array([1, 1, 0.9])) * k * 0 + col * k
            img = np.clip(img, 0, 1)
        else:
            heads = []

        out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        d = ImageDraw.Draw(out)

        # Bright pen tip while the border is being traced.
        if 0 < draw_t < 1:
            for h in heads:
                r = a.line * 1.6
                d.ellipse([h[0] - r, h[1] - r, h[0] + r, h[1] + r], fill=(255, 255, 240))

        if self.capital and cap_t > 0:
            self.draw_capital(out, d, s, c, cap_t, T)

        if title_t > 0:
            self.draw_title(out, title_t)
        return out

    def draw_capital(self, out, d, s, c, t, T):
        x, y = (np.array(self.cap_px) - c) * s + [self.W / 2, self.H / 2]
        pop = back_out(t)
        # pulsing ring
        ph = (T * 0.9) % 1
        rr = 10 + 40 * ph
        ring = Image.new("RGBA", out.size, (0, 0, 0, 0))
        ImageDraw.Draw(ring).ellipse([x - rr, y - rr, x + rr, y + rr], outline=(255, 255, 255, int(220 * (1 - ph) * t)), width=4)
        out.paste(ring, (0, 0), ring)
        r = 11 * pop
        d.ellipse([x - r - 4, y - r - 4, x + r + 4, y + r + 4], fill=(255, 255, 255))
        d.ellipse([x - r, y - r, x + r, y + r], fill=(200, 20, 20))

        name = self.capital[2]
        f = self.font_label
        tw = d.textlength(name, font=f)
        pad = 18
        bh = f.size + pad * 1.4
        bw = tw + pad * 2
        bx, by = x - bw / 2, y - 28 - bh - 20 * (1 - ease_out(t))
        lab = Image.new("RGBA", out.size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(lab)
        al = int(255 * clamp01(t * 1.5))
        ld.rounded_rectangle([bx, by, bx + bw, by + bh], radius=12, fill=(255, 255, 255, al))
        ld.polygon([(x - 12, by + bh - 1), (x + 12, by + bh - 1), (x, by + bh + 14)], fill=(255, 255, 255, al))
        ld.text((x, by + bh / 2), name, font=f, fill=(20, 20, 20, al), anchor="mm")
        out.paste(lab, (0, 0), lab)

    def draw_title(self, out, t):
        f = self.font_title
        lay = Image.new("RGBA", out.size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(lay)
        y = self.H * 0.13 + 40 * (1 - t)
        al = int(255 * t)
        ld.text((self.W / 2, y), self.title, font=f, fill=(0, 0, 0, int(al * 0.8)), anchor="mm")
        sh = lay.filter(ImageFilter.GaussianBlur(14))
        out.paste(sh, (0, 0), sh)
        top = Image.new("RGBA", out.size, (0, 0, 0, 0))
        td = ImageDraw.Draw(top)
        td.text((self.W / 2, y), self.title, font=f, fill=(255, 255, 255, al), anchor="mm",
                stroke_width=3, stroke_fill=(0, 0, 0, int(al * 0.6)))
        out.paste(top, (0, 0), top)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("country", help="name (English or Portuguese) or ISO code, e.g. China, Brasil, US")
    ap.add_argument("-o", "--out", help="output .mp4 (default: <country>.mp4)")
    ap.add_argument("--title", help="text at the top (default: country name)")
    ap.add_argument("--capital", help="capital label (default: from Natural Earth)")
    ap.add_argument("--lang", default="pt", choices=["pt", "en"], help="language of default names")
    ap.add_argument("--color", default="#d81e05", help="fill colour")
    ap.add_argument("--border", default="#ffd400", help="border colour")
    ap.add_argument("--line", type=float, default=5, help="border width in pixels")
    ap.add_argument("--duration", type=float, default=10)
    ap.add_argument("--fps", type=int, default=30)
    ap.add_argument("--width", type=int, default=1080)
    ap.add_argument("--height", type=int, default=1920)
    ap.add_argument("--zoom-start", type=float, default=1.6, help="how far out the camera starts")
    ap.add_argument("--zoom-end", type=float, default=1.0, help="extra zoom at the end")
    ap.add_argument("--frame-all", action="store_true", help="frame every island/territory, not just the mainland")
    ap.add_argument("--no-flag", action="store_true")
    ap.add_argument("--no-capital", action="store_true")
    ap.add_argument("--preview", type=float, help="write a single PNG at this time (seconds) instead of the video")
    ap.add_argument("--cache", default=os.path.expanduser("~/.cache/mapvideo"))
    a = ap.parse_args()

    sc = Scene(a)
    n = int(a.duration * a.fps)
    out = a.out or "%s.mp4" % a.country.lower().replace(" ", "_")
    if a.preview is not None:
        png = os.path.splitext(out)[0] + ".png"
        sc.render(min(int(a.preview * a.fps), n - 1), n).save(png)
        print(png)
        return

    ff = subprocess.Popen([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24",
        "-s", "%dx%d" % (a.width, a.height), "-r", str(a.fps), "-i", "-",
        "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
        "-movflags", "+faststart", out], stdin=subprocess.PIPE)
    for i in range(n):
        ff.stdin.write(sc.render(i, n).tobytes())
        if i % a.fps == 0:
            print("\rframe %d/%d" % (i, n), end="", flush=True)
    ff.stdin.close()
    ff.wait()
    print("\n" + out)


if __name__ == "__main__":
    main()
