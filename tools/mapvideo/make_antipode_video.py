#!/usr/bin/env python3
"""Vertical "where is the other side of the world" video on real satellite imagery.

Close-up of a city -> zoom out to a 3D globe -> a tunnel straight through the
Earth -> the 20 000 km surface arc -> dive into the antipode on the map.

    python3 make_antipode_video.py
    python3 make_antipode_video.py --place "Lisboa" --lat 38.72 --lon -9.14 --ocean "Oceano Pacífico"

Captions come from --captions (JSON list of [start, end, text]; *word* is
highlighted) or the built-in São Paulo script. Reuses the data/tiles of
make_map_video.py.
"""
import argparse
import json
import math
import os
import subprocess

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

from make_map_video import (COUNTRIES_URL, FONT, FONT_LABEL, TILE, back_out, ease_out, grade_array,
                            load_json, merc, mosaic, outer_rings, phase, smooth)

W, H, FPS = 1080, 1920, 30
EARTH_KM = 6371.0
YELLOW = np.array([255, 212, 0], np.float32) / 255

CAPTIONS = [
    [0.2, 3.9, "Qual é o lugar mais *distante* de {place} no planeta?"],
    [4.1, 8.3, "Imagine atravessar a Terra em *linha reta* e sair do outro lado do mundo!"],
    [8.5, 12.4, "Esse lugar é chamado de *ANTÍPODA*: o ponto exatamente oposto no globo"],
    [12.6, 16.3, "Pela superfície, seriam cerca de *20 mil km* de viagem!"],
    [16.5, 21.0, "O antípoda de {place} fica no *{ocean}*, perto das Filipinas e do Japão"],
    [21.2, 25.0, "Se você cavasse um túnel, onde iria sair? *Comenta sua cidade!*"],
]


# ---------------------------------------------------------------- vectors

def vec(lat, lon):
    la, lo = np.radians(lat), np.radians(lon)
    return np.array([np.cos(la) * np.cos(lo), np.cos(la) * np.sin(lo), np.sin(la)])


def norm(v):
    return v / np.linalg.norm(v)


def view(fwd, up_hint=(0, 0, 1)):
    """Rows: screen right, screen up, towards the viewer (world coords)."""
    f = norm(np.asarray(fwd, float))
    u = norm(np.asarray(up_hint, float) - np.dot(up_hint, f) * f)
    r = np.cross(u, f)
    return np.array([r, u, f])


def view_side(a):
    """Camera looking at the point 90° from `a`, with `a` on the left limb
    and its antipode on the right limb, north up."""
    f = norm(np.cross(a, [0, 0, 1]))  # on the great circle 90° away, at the equator side
    if np.dot(f, vec(10, 20)) < 0:  # prefer the side with more land facing us
        f = -f
    r = -a
    u = np.cross(f, r)
    if u[2] < 0:  # keep north up; flip the camera to the other side
        f, u = -f, -u
        r = np.cross(u, f)
        if np.dot(r, a) > 0:
            r = -r
    return np.array([r, u, f])


def mat2quat(m):
    m = m.T  # columns are the camera axes
    t = np.trace(m)
    if t > 0:
        s = math.sqrt(t + 1) * 2
        return np.array([0.25 * s, (m[2, 1] - m[1, 2]) / s, (m[0, 2] - m[2, 0]) / s, (m[1, 0] - m[0, 1]) / s])
    i = int(np.argmax(np.diag(m)))
    j, k = (i + 1) % 3, (i + 2) % 3
    s = math.sqrt(1 + m[i, i] - m[j, j] - m[k, k]) * 2
    q = np.zeros(4)
    q[0] = (m[k, j] - m[j, k]) / s
    q[1 + i] = 0.25 * s
    q[1 + j] = (m[j, i] + m[i, j]) / s
    q[1 + k] = (m[k, i] + m[i, k]) / s
    return q


def quat2mat(q):
    w, x, y, z = q / np.linalg.norm(q)
    m = np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                  [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                  [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])
    return m.T


def slerp(m0, m1, t):
    q0, q1 = mat2quat(m0), mat2quat(m1)
    d = np.dot(q0, q1)
    if d < 0:
        q1, d = -q1, -d
    if d > 0.9995:
        return quat2mat(q0 + (q1 - q0) * t)
    th = math.acos(d)
    return quat2mat((math.sin((1 - t) * th) * q0 + math.sin(t * th) * q1) / math.sin(th))


# ---------------------------------------------------------------- imagery

def equirect_texture(cache, z=5):
    path = os.path.join(cache, "equirect_z%d.jpg" % z)
    if os.path.exists(path):
        return np.asarray(Image.open(path).convert("RGB"))
    n = TILE * 2 ** z
    img, _, _ = mosaic(z, 0, 0, n, n, os.path.join(cache, "bluemarble"), grade=False)
    m = np.asarray(img)
    th = n // 2
    lat = 90 - (np.arange(th) + 0.5) * 180 / th
    _, y = merc(np.zeros(th), lat, z)
    y = np.clip(y, 0, n - 1.001)
    y0 = y.astype(int)
    f = (y - y0)[:, None, None]
    tex = np.empty((th, n, 3), np.uint8)
    for a in range(0, th, 256):  # chunked to keep memory low
        b = min(th, a + 256)
        rows = m[y0[a:b]] * (1 - f[a:b]) + m[y0[a:b] + 1] * f[a:b]
        tex[a:b] = grade_array(rows.astype(np.uint8))
    Image.fromarray(tex).save(path, quality=92)
    return tex


class FlatMap:
    """North-up Web Mercator view centred on a point; picks the tile zoom per frame."""

    def __init__(self, lat, lon, cache):
        self.lat, self.lon, self.cache = lat, lon, cache
        self.levels = {}

    def level(self, z):
        if z not in self.levels:
            cx, cy = merc(np.array(self.lon), np.array(self.lat), z)
            hw, hh = W / 2 / 0.68, H / 2 / 0.68
            n = TILE * 2 ** z
            base, ox, oy = mosaic(z, cx - hw, max(0, cy - hh), cx + hw, min(n, cy + hh),
                                  os.path.join(self.cache, "bluemarble"))
            self.levels[z] = (base, float(cx) - ox, float(cy) - oy, ox, oy)
        return self.levels[z]

    def scale(self, R, z):
        return R * 2 * math.pi * math.cos(math.radians(self.lat)) / (TILE * 2 ** z)

    def render(self, R):
        z = int(round(math.log2(R * 2 * math.pi * math.cos(math.radians(self.lat)) / TILE)))
        z = max(2, min(8, z))
        base, cx, cy, ox, oy = self.level(z)
        s = self.scale(R, z)
        x0, y0, x1, y1 = cx - W / 2 / s, cy - H / 2 / s, cx + W / 2 / s, cy + H / 2 / s
        ix0, iy0 = math.floor(x0), math.floor(y0)
        part = base.crop((ix0, iy0, math.ceil(x1) + 1, math.ceil(y1) + 1))
        img = part.resize((W, H), Image.BILINEAR, box=(x0 - ix0, y0 - iy0, x1 - ix0, y1 - iy0), reducing_gap=2.0)

        def project(lon, lat):
            x, y = merc(np.asarray(lon, float), np.asarray(lat, float), z)
            return (x - ox - cx) * s + W / 2, (y - oy - cy) * s + H / 2
        return np.asarray(img).astype(np.float32) / 255, project


def render_globe(tex, M, R, cx, cy, bg):
    """Orthographic globe with camera rows M = (right, up, fwd), drawn over bg."""
    th, tw = tex.shape[:2]
    pad = R * 1.12
    x0, x1 = int(max(0, cx - pad)), int(min(W, cx + pad + 1))
    y0, y1 = int(max(0, cy - pad)), int(min(H, cy + pad + 1))
    if x0 >= x1 or y0 >= y1:
        return bg
    yy, xx = np.mgrid[y0:y1, x0:x1].astype(np.float32)
    u, v = (xx - cx) / R, -(yy - cy) / R
    d = np.sqrt(u * u + v * v)
    inside = d < 1
    zc = np.sqrt(np.clip(1 - d * d, 0, 1))
    p = u[..., None] * M[0] + v[..., None] * M[1] + zc[..., None] * M[2]
    lat = np.degrees(np.arcsin(np.clip(p[..., 2], -1, 1)))
    lon = np.degrees(np.arctan2(p[..., 1], p[..., 0]))
    fx = ((lon + 180) / 360 * tw - 0.5) % tw
    fy = np.clip((90 - lat) / 180 * th - 0.5, 0, th - 1.001)
    ix, iy = fx.astype(int), fy.astype(int)
    ax, ay = (fx - ix)[..., None], (fy - iy)[..., None]
    ix1 = (ix + 1) % tw
    c = (tex[iy, ix] * (1 - ax) * (1 - ay) + tex[iy, ix1] * ax * (1 - ay)
         + tex[iy + 1, ix] * (1 - ax) * ay + tex[iy + 1, ix1] * ax * ay) / 255

    # light from the upper left + blue haze towards the limb
    light = np.clip(0.35 + 0.75 * (-0.35 * u + 0.35 * v + 0.87 * zc), 0.25, 1.1)[..., None]
    haze = ((1 - zc) ** 2.5)[..., None]
    c = c * light * (1 - 0.55 * haze) + np.array([0.3, 0.55, 1.0]) * 0.55 * haze
    edge = np.clip((1 - d) * R, 0, 1)[..., None]

    out = bg.copy()
    region = out[y0:y1, x0:x1]
    # atmosphere outside the disc
    glow = np.clip(1 - (d - 1) / 0.07, 0, 1) ** 2 * (d >= 1)
    region += np.array([0.25, 0.5, 1.0]) * 0.55 * glow[..., None]
    region[:] = np.where(inside[..., None], region * (1 - edge) + c * edge, region)
    return np.clip(out, 0, 1)


def star_field(seed=7):
    rng = np.random.default_rng(seed)
    img = np.zeros((H, W, 3), np.float32)
    img[:] = np.array([3, 5, 14], np.float32) / 255
    n = 900
    xs, ys = rng.integers(0, W, n), rng.integers(0, H, n)
    b = rng.random(n) ** 3
    img[ys, xs] = np.clip(img[ys, xs] + b[:, None] * 0.9 + 0.05, 0, 1)
    return np.asarray(Image.fromarray((img * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.6))).astype(np.float32) / 255


# ---------------------------------------------------------------- overlays

def draw_glow_lines(img, lines, width, col, dash=None, offset=0):
    """Polylines with a soft glow; lines = list of Nx2 screen arrays."""
    lay = Image.new("L", (W * 2, H * 2), 0)
    d = ImageDraw.Draw(lay)
    for pts in lines:
        if len(pts) < 2:
            continue
        if dash:
            seg = np.hypot(*np.diff(pts, axis=0).T)
            cum = np.concatenate([[0], np.cumsum(seg)])
            for s0 in np.arange(-offset % (dash * 2) - dash * 2, cum[-1], dash * 2):
                a, b = max(s0, 0), min(s0 + dash, cum[-1])
                if b <= a:
                    continue
                q = np.array([np.interp(np.linspace(a, b, 6), cum, pts[:, k]) for k in (0, 1)]).T
                d.line([tuple(p * 2) for p in q], fill=255, width=int(width * 2))
        else:
            d.line([tuple(p * 2) for p in pts], fill=255, width=int(width * 2), joint="curve")
    core = lay.resize((W, H), Image.BOX)
    g = np.asarray(core.filter(ImageFilter.GaussianBlur(width * 2.4))).astype(np.float32)[..., None] / 255
    k = np.asarray(core).astype(np.float32)[..., None] / 255
    img = img + col * np.clip(g * 2.0, 0, 1) * 0.85
    return np.clip(img * (1 - k) + col * k, 0, 1)


def draw_pin(out, x, y, t, label, font, colour=(230, 30, 30), T=0.0, below=False):
    if t <= 0:
        return
    pop = back_out(min(t, 1))
    lay = Image.new("RGBA", out.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    ph = (T * 0.8) % 1
    rr = 10 + 46 * ph
    d.ellipse([x - rr, y - rr, x + rr, y + rr], outline=(255, 255, 255, int(220 * (1 - ph))), width=4)
    # teardrop pin standing on the point
    h = 62 * pop
    r = 20 * pop
    cy = y - h + r
    d.polygon([(x - r * 0.82, cy + r * 0.55), (x + r * 0.82, cy + r * 0.55), (x, y)], fill=colour + (255,))
    d.ellipse([x - r, cy - r, x + r, cy + r], fill=colour + (255,), outline=(255, 255, 255, 255), width=4)
    d.ellipse([x - r * 0.38, cy - r * 0.38, x + r * 0.38, cy + r * 0.38], fill=(255, 255, 255, 255))
    if label:
        al = int(255 * min(1, t * 1.5))
        tw = d.textlength(label, font=font)
        bw, bh = tw + 36, font.size + 26
        by = (y + 26) if below else (cy - r - 22 - bh)
        d.rounded_rectangle([x - bw / 2, by, x + bw / 2, by + bh], radius=14, fill=(255, 255, 255, al))
        d.text((x, by + bh / 2), label, font=font, fill=(20, 20, 20, al), anchor="mm")
    out.paste(lay, (0, 0), lay)


def shadow_text(out, xy, text, font, fill=(255, 255, 255), alpha=1.0, anchor="mm", italic_col=None):
    lay = Image.new("RGBA", out.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    d.text(xy, text, font=font, fill=(0, 0, 0, int(200 * alpha)), anchor=anchor)
    sh = lay.filter(ImageFilter.GaussianBlur(6))
    out.paste(sh, (0, 0), sh)
    top = Image.new("RGBA", out.size, (0, 0, 0, 0))
    ImageDraw.Draw(top).text(xy, text, font=font, fill=fill + (int(255 * alpha),), anchor=anchor,
                             stroke_width=2, stroke_fill=(0, 0, 0, int(160 * alpha)))
    out.paste(top, (0, 0), top)


def caption(out, text, font, alpha, y_center):
    """Word-wrapped caption, *words* in yellow, white with black outline."""
    words = []
    for w in text.split():
        words.append(w)
    d = ImageDraw.Draw(out)
    space = d.textlength(" ", font=font)
    maxw = W * 0.86
    lines, cur, cw = [], [], 0
    for w in words:
        ww = d.textlength(w.replace("*", ""), font=font)
        if cur and cw + space + ww > maxw:
            lines.append((cur, cw))
            cur, cw = [], 0
        cur.append((w, ww))
        cw += ww + (space if len(cur) > 1 else 0)
    if cur:
        lines.append((cur, cw))
    lh = font.size * 1.22
    y = y_center - lh * (len(lines) - 1) / 2
    lay = Image.new("RGBA", out.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    hl = False
    for ws, lw in lines:
        x = W / 2 - lw / 2
        for w, ww in ws:
            start = w.startswith("*")
            if start:
                hl = True
            col = (255, 214, 0) if hl else (255, 255, 255)
            ld.text((x, y), w.replace("*", ""), font=font, fill=col + (int(255 * alpha),), anchor="lm",
                    stroke_width=7, stroke_fill=(0, 0, 0, int(255 * alpha)))
            if w.rstrip(".,!?:").endswith("*"):
                hl = False
            x += ww + space
        y += lh
    out.paste(lay, (0, 0), lay)


# ---------------------------------------------------------------- video

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--place", default="São Paulo")
    ap.add_argument("--lat", type=float, default=-23.55)
    ap.add_argument("--lon", type=float, default=-46.63)
    ap.add_argument("--ocean", default="Oceano Pacífico", help="label at the antipode")
    ap.add_argument("--captions", help="JSON file: [[start, end, text], ...]")
    ap.add_argument("--duration", type=float, default=25)
    ap.add_argument("-o", "--out", default="antipoda.mp4")
    ap.add_argument("--preview", type=float, nargs="*", help="write PNGs at these times instead of the video")
    ap.add_argument("--cache", default=os.path.expanduser("~/.cache/mapvideo"))
    a = ap.parse_args()

    lat, lon = a.lat, a.lon
    alat, alon = -lat, lon + 180 if lon <= 0 else lon - 180
    P, Q = vec(lat, lon), vec(alat, alon)
    caps = json.load(open(a.captions)) if a.captions else CAPTIONS
    caps = [(s, e, t.format(place=a.place, ocean=a.ocean)) for s, e, t in caps]

    tex = equirect_texture(a.cache)
    home, far = FlatMap(lat, lon, a.cache), FlatMap(alat, alon, a.cache)
    stars = star_field()
    countries = load_json(COUNTRIES_URL, a.cache)["features"]

    # Outline of the home country (for the globe) and the countries around the antipode.
    def containing(la, lo):
        best = None
        for f in countries:
            for r in outer_rings(f["geometry"]):
                x, y = r[:, 0], r[:, 1]
                inside = ((y > la) != (np.roll(y, 1) > la)) & (lo < (np.roll(x, 1) - x) * (la - y) / (np.roll(y, 1) - y + 1e-12) + x)
                if inside.sum() % 2:
                    best = f
        return best
    home_country = containing(lat, lon)
    home_rings = [np.array(r) for r in outer_rings(home_country["geometry"])] if home_country else []
    near = []
    for f in countries:
        p = f["properties"]
        lx, ly = p.get("LABEL_X"), p.get("LABEL_Y")
        if lx is None:
            continue
        big = p.get("POP_EST", 0) > 3e6 or p.get("LABELRANK", 9) <= 3
        if big and abs(ly - alat) < 22 and abs(((lx - alon + 180) % 360) - 180) < 20:
            near.append(f)

    V_home = view(P)
    V_side = view_side(P)
    V_far = view(Q)

    f_cap = ImageFont.truetype(FONT, 66)
    f_lab = ImageFont.truetype(FONT_LABEL, 42)
    f_country = ImageFont.truetype(FONT, 40)
    f_km = ImageFont.truetype(FONT, 96)
    f_small = ImageFont.truetype(FONT_LABEL, 34)

    R_close, R_cross, R_globe = 9000.0, 1500.0, 440.0
    GX, GY = W / 2, H * 0.44

    def frame(T):
        tile_alpha, globe = 0.0, None
        # --- camera
        if T < 5.2:  # home: zoom out on the map, then cross-fade to the globe
            k = smooth(phase(T, 0.6, 5.2))
            R = R_close * (R_cross / R_close) ** k
            M, cx, cy = V_home, W / 2, H / 2
            tile_alpha = 1 - smooth(phase(T, 4.4, 5.2))
            flat = home
        elif T < 15.5:  # globe: shrink, turn sideways, tunnel and arc
            k = smooth(phase(T, 5.2, 8.0))
            R = R_cross * (R_globe / R_cross) ** k
            M = slerp(V_home, V_side, smooth(phase(T, 5.4, 8.2)))
            cx, cy = W / 2 + (GX - W / 2) * k, H / 2 + (GY - H / 2) * k
            flat = None
        else:  # turn to the antipode and dive in
            k = smooth(phase(T, 15.5, 18.6))
            R = R_globe * (1900 / R_globe) ** smooth(phase(T, 16.2, 21.5))
            M = slerp(V_side, V_far, k)
            cx, cy = GX + (W / 2 - GX) * k, GY + (H / 2 - GY) * k
            tile_alpha = smooth(phase(T, 17.6, 18.6))
            flat = far

        img = stars
        if tile_alpha < 1:
            img = render_globe(tex, M, R, cx, cy, stars)
        project = None
        if flat is not None and tile_alpha > 0:
            fimg, project = flat.render(R)
            img = img * (1 - tile_alpha) + fimg * tile_alpha

        def gproj(v):
            s = M @ v
            return cx + s[0] * R, cy - s[1] * R, s[2]

        # --- globe overlays
        if tile_alpha < 1:
            if home_rings:
                lines = []
                for r in home_rings:
                    pts = np.array([gproj(vec(la, lo)) for lo, la in r[::2]])
                    vis = pts[:, 2] > 0
                    if vis.mean() > 0.5:
                        lines.append(pts[vis, :2])
                vis_a = 1 - tile_alpha
                if lines and T > 3.8:
                    img = draw_glow_lines(img, lines, 3, YELLOW * min(1, (T - 3.8) / 0.8) * vis_a)
            # tunnel through the Earth
            tt = ease_out(phase(T, 8.6, 10.6)) * (1 - smooth(phase(T, 15.6, 16.4)))
            if tt > 0:
                p0, p1 = np.array(gproj(P)[:2]), np.array(gproj(Q)[:2])
                end = p0 + (p1 - p0) * tt
                line = np.array([p0 + (end - p0) * s for s in np.linspace(0, 1, 40)])
                img = draw_glow_lines(img, [line], 5, np.array([1.0, 0.35, 0.2]), dash=22, offset=T * 60)
            # surface arc along the limb (the great circle between the two points)
            at = ease_out(phase(T, 12.2, 15.0)) * (1 - smooth(phase(T, 15.6, 16.4)))
            if at > 0:
                ang = np.linspace(math.pi, math.pi - math.pi * at, 120)
                arc = np.stack([cx + np.cos(ang) * R * 1.035, cy - np.sin(ang) * R * 1.035], axis=1)
                img = draw_glow_lines(img, [arc], 6, YELLOW)

        out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        d = ImageDraw.Draw(out)

        # --- markers
        if T < 5.2:
            draw_pin(out, W / 2, H / 2, phase(T, 0.5, 1.2) * (1 - phase(T, 4.6, 5.2)), a.place, f_lab, T=T)
        elif T < 16.4:
            fade = 1 - phase(T, 15.6, 16.4)
            x, y, z = gproj(P)
            if z > -0.05:
                draw_pin(out, x, y, min(1, phase(T, 5.2, 5.8)) * fade, a.place if T > 8.0 else None, f_small, T=T)
            x, y, z = gproj(Q)
            if z > -0.05 and T > 10.2:
                draw_pin(out, x, y, phase(T, 10.2, 10.8) * fade, "Antípoda", f_small, colour=(255, 170, 0), T=T)
            if T > 12.2:
                at = ease_out(phase(T, 12.2, 15.0))
                km = int(round(math.pi * EARTH_KM * at / 100) * 100)
                al = min(1, (T - 12.2) / 0.4) * fade
                shadow_text(out, (cx, cy - R - 120), "%s km" % format(km, ",").replace(",", "."), f_km,
                            fill=(255, 214, 0), alpha=al)
        if T >= 17.6 and project is not None:
            al = tile_alpha
            for f in near:
                p = f["properties"]
                x, y = project(p["LABEL_X"], p["LABEL_Y"])
                if 100 < x < W - 100 and 120 < y < H * 0.7:
                    shadow_text(out, (float(x), float(y)), (p.get("NAME_PT") or p["NAME"]).upper(), f_country, alpha=al)
            x, y = project(alon, alat)
            draw_pin(out, float(x), float(y), phase(T, 18.4, 19.1), "Antípoda de %s" % a.place, f_lab,
                     colour=(255, 170, 0), T=T)
            shadow_text(out, (float(x), float(y) + 70), "%.1f°%s  %.1f°%s" % (abs(alat), "N" if alat >= 0 else "S",
                        abs(alon), "E" if alon >= 0 else "W"), f_small, alpha=phase(T, 18.8, 19.4))
            shadow_text(out, (float(x), float(y) + 125), a.ocean.upper(), f_country, fill=(170, 210, 255),
                        alpha=phase(T, 19.0, 19.6))

        # --- captions
        for s, e, text in caps:
            if s <= T <= e:
                al = min(1, (T - s) / 0.25, (e - T) / 0.25)
                caption(out, text, f_cap, al, H * 0.8)
        return out

    n = int(a.duration * FPS)
    if a.preview is not None:
        for t in a.preview:
            p = "%s_%05.1f.png" % (os.path.splitext(a.out)[0], t)
            frame(t).save(p)
            print(p)
        return
    ff = subprocess.Popen([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "%dx%d" % (W, H),
        "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
        "-movflags", "+faststart", a.out], stdin=subprocess.PIPE)
    for i in range(n):
        ff.stdin.write(frame(i / FPS).tobytes())
        if i % FPS == 0:
            print("\rframe %d/%d" % (i, n), end="", flush=True)
    ff.stdin.close()
    ff.wait()
    print("\n" + a.out)


if __name__ == "__main__":
    main()
