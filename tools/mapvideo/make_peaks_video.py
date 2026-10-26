#!/usr/bin/env python3
"""Vertical video: "Onde ficam as maiores montanhas do Brasil?" (50 s).

Satellite imagery (NASA Blue Marble) shaded with real elevation (AWS Terrain
Tiles), a camera that flies from the whole of Brazil down to the summits of
Pico da Neblina / Pico 31 de Março and over to Pico da Bandeira.

    python3 make_peaks_video.py                  # brasil_montanhas.mp4
    python3 make_peaks_video.py --preview 2 20 34 44 48
"""
import argparse
import math
import os
import subprocess
import sys
import tempfile
from collections import OrderedDict
from concurrent.futures import ThreadPoolExecutor

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from make_antipode_video import FPS, H, W, YELLOW, caption, draw_glow_lines, draw_pin, shadow_text
from make_map_video import (COUNTRIES_URL, FONT, FONT_LABEL, NE, TILE, TILE_URL, ease_out, fetch,
                            grade_array, load_json, outer_rings, phase, smooth)

STATES_URL = NE + "ne_50m_admin_1_states_provinces.geojson"
TERRAIN_URL = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"

# Summits located on the elevation model; heights are the official IBGE values.
NEBLINA = (-66.0041, 0.8076)
MARCO31 = (-66.0091, 0.7999)
BANDEIRA = (-41.7965, -20.4341)

CAPTIONS = [
    (0.3, 2.8, "Você sabe onde fica o *ponto mais alto* do Brasil?"),
    (2.9, 5.0, "Talvez você se *surpreenda* com a resposta!"),
    (5.1, 8.9, "Muita gente imagina que as maiores montanhas ficam no *Sul* ou no *Sudeste*"),
    (9.0, 13.0, "Mas a verdade é que elas estão no *extremo norte* do país!"),
    (13.1, 17.9, "É na *Serra do Imeri*, no estado do *Amazonas*, perto da fronteira com a Venezuela..."),
    (18.0, 23.0, "...que encontramos o *Pico da Neblina*, o ponto mais alto do Brasil"),
    (23.1, 26.4, "Ele tem cerca de *2.995 metros* de altitude!"),
    (26.5, 31.0, "Quase *3 km* acima do nível do mar, cercado pela imensidão da floresta amazônica"),
    (31.1, 35.3, "Ali perto fica o *Pico 31 de Março*, com aproximadamente *2.974 metros*"),
    (35.4, 39.0, "Considerado o *segundo* ponto mais alto do país"),
    (39.1, 42.6, "Já no Sudeste, na divisa entre *Minas Gerais* e *Espírito Santo*..."),
    (42.7, 46.0, "...fica o *Pico da Bandeira*, com cerca de *2.891 metros*"),
    (46.1, 50.0, "E aí, você sabia que o *topo do Brasil* fica no *Amazonas*?"),
]

# Camera keyframes: time, lon, lat, zoom (Web Mercator zoom level, fractional).
KEYS = [
    (0.0, -53.5, -16.0, 4.45),
    (5.0, -53.5, -15.0, 4.80),
    (13.0, -53.5, -15.0, 4.92),
    (17.0, -64.0, -3.0, 6.25),
    (19.5, -65.6, 0.2, 7.6),
    (23.0, -66.005, 0.805, 11.0),
    (30.5, -66.005, 0.805, 12.6),
    (33.5, -66.0066, 0.8038, 14.0),
    (39.0, -66.0066, 0.8038, 14.2),
    (41.0, -52.0, -9.0, 5.0),
    (44.0, -41.8, -20.43, 10.4),
    (46.0, -41.797, -20.434, 11.0),
    (48.0, -53.5, -15.5, 4.85),
    (50.0, -53.5, -15.5, 4.92),
]


def mxy(lon, lat):
    """Normalised Web Mercator (0..1)."""
    lat = np.clip(lat, -85, 85)
    x = (np.asarray(lon) + 180) / 360
    y = (1 - np.log(np.tan(np.radians(lat)) + 1 / np.cos(np.radians(lat))) / math.pi) / 2
    return x, y


def camera(T):
    for (t0, lo0, la0, z0), (t1, lo1, la1, z1) in zip(KEYS, KEYS[1:]):
        if T <= t1:
            break
    k = smooth(phase(T, t0, t1))
    z = z0 + (z1 - z0) * k
    # Move the centre in step with the visible width so zooms stay on target.
    if abs(z1 - z0) > 1e-3:
        w = (2 ** -z0 - 2 ** -z) / (2 ** -z0 - 2 ** -z1)
    else:
        w = k
    x0, y0 = mxy(lo0, la0)
    x1, y1 = mxy(lo1, la1)
    return x0 + (x1 - x0) * w, y0 + (y1 - y0) * w, z


class Tiles:
    def __init__(self, url, folder, maxz, kind):
        self.url, self.folder, self.maxz, self.kind = url, folder, maxz, kind
        self.mem = OrderedDict()

    def path(self, z, x, y):
        return os.path.join(self.folder, str(z), "%d-%d.%s" % (y, x, "jpg" if self.kind == "sat" else "png"))

    def url_for(self, z, x, y):
        if self.kind == "sat":
            return self.url.format(z=z, y=y, x=x)
        return self.url.format(z=z, x=x, y=y)

    def download(self, z, x, y):
        try:
            fetch(self.url_for(z, x, y), self.path(z, x, y))
        except Exception as e:
            print("tile %s %d/%d/%d: %s" % (self.kind, z, x, y, e), file=sys.stderr)

    def get(self, z, x, y):
        key = (z, x, y)
        if key in self.mem:
            self.mem.move_to_end(key)
            return self.mem[key]
        p = self.path(z, x, y)
        if not os.path.exists(p):
            self.download(z, x, y)
        try:
            im = Image.open(p).convert("RGB")
            a = np.asarray(im)
            if self.kind == "sat":
                a = grade_array(a)
            else:
                a = a.astype(np.float32)
                a = a[..., 0] * 256 + a[..., 1] + a[..., 2] / 256 - 32768
        except Exception:
            a = np.full((TILE, TILE, 3), (8, 18, 40), np.uint8) if self.kind == "sat" else np.zeros((TILE, TILE), np.float32)
        self.mem[key] = a
        if len(self.mem) > 900:
            self.mem.popitem(last=False)
        return a

    def level(self, zc):
        return max(1, min(self.maxz, int(round(zc))))

    def boxes(self, cx, cy, zc):
        z = self.level(zc)
        s = 2 ** (zc - z)
        n = TILE * 2 ** z
        px, py = cx * n, cy * n
        x0, y0, x1, y1 = px - W / 2 / s, py - H / 2 / s, px + W / 2 / s, py + H / 2 / s
        return z, s, (x0, y0, x1, y1)

    def needed(self, cx, cy, zc):
        z, s, (x0, y0, x1, y1) = self.boxes(cx, cy, zc)
        n = 2 ** z
        return {(z, tx % n, ty) for ty in range(max(0, int(y0 // TILE)), min(n, int(y1 // TILE) + 1))
                for tx in range(int(x0 // TILE), int(x1 // TILE) + 1)}

    def view(self, cx, cy, zc):
        """Screen-sized image (sat: float RGB 0..1, terrain: elevation in m)."""
        z, s, (x0, y0, x1, y1) = self.boxes(cx, cy, zc)
        n = 2 ** z
        tx0, ty0 = int(x0 // TILE), int(y0 // TILE)
        tx1, ty1 = int(x1 // TILE) + 1, int(y1 // TILE) + 1
        shape = ((ty1 - ty0) * TILE, (tx1 - tx0) * TILE)
        if self.kind == "sat":
            canvas = np.empty(shape + (3,), np.uint8)
            canvas[:] = (8, 18, 40)
        else:
            canvas = np.zeros(shape, np.float32)
        for ty in range(ty0, ty1):
            if not 0 <= ty < n:
                continue
            for tx in range(tx0, tx1):
                canvas[(ty - ty0) * TILE:(ty - ty0 + 1) * TILE, (tx - tx0) * TILE:(tx - tx0 + 1) * TILE] = self.get(z, tx % n, ty)
        box = (x0 - tx0 * TILE, y0 - ty0 * TILE, x1 - tx0 * TILE, y1 - ty0 * TILE)
        if self.kind == "sat":
            im = Image.fromarray(canvas).resize((W, H), Image.BILINEAR, box=box, reducing_gap=2.0)
            return np.asarray(im).astype(np.float32) / 255
        im = Image.fromarray(canvas, mode="F").resize((W, H), Image.BILINEAR, box=box)
        return np.asarray(im)


def shade(sat, elev, zc, lat):
    """Hillshade from the elevation, plus forest/rock colours when close in."""
    e = np.maximum(elev, 0)
    m_px = 40075016 * math.cos(math.radians(lat)) / (TILE * 2 ** zc)
    ex = float(np.clip((m_px / 8) ** 0.5, 1.6, 22))
    gy, gx = np.gradient(e, m_px)
    nx, ny = -gx * ex, gy * ex  # screen y points south
    L = np.array([-0.55, 0.55, 0.63])
    L = L / np.linalg.norm(L)
    d = (nx * L[0] + ny * L[1] + L[2]) / np.sqrt(nx * nx + ny * ny + 1)
    k = np.clip(d / L[2], 0, 1.8)[..., None]
    land = (elev > 1)[..., None]
    img = sat
    close = float(np.clip((zc - 6.5) / 2.5, 0, 1))
    if close > 0:
        # Blue Marble is ~500 m/px; close in, colour by altitude instead.
        ramp_e = np.array([0, 300, 1000, 1800, 2300, 2700, 3000], np.float32)
        ramp_c = np.array([[0.10, 0.22, 0.08], [0.11, 0.25, 0.09], [0.14, 0.28, 0.11], [0.20, 0.30, 0.13],
                           [0.30, 0.31, 0.20], [0.42, 0.40, 0.34], [0.55, 0.53, 0.50]], np.float32)
        col = np.stack([np.interp(e, ramp_e, ramp_c[:, i]) for i in range(3)], axis=-1)
        img = img * (1 - close) + col * close
        img = img * (1 + 0.35 * float(np.clip((zc - 6) / 2, 0, 1)) * (1 - close))  # Amazon imagery is very dark
    img = np.where(land, img * (0.25 + 0.85 * k), img)
    return np.clip(img, 0, 1)


class Scene:
    def __init__(self, cache):
        self.sat = Tiles(TILE_URL, os.path.join(cache, "bluemarble", "tiles"), 8, "sat")
        self.dem = Tiles(TERRAIN_URL, os.path.join(cache, "terrarium"), 13, "dem")
        countries = load_json(COUNTRIES_URL, cache)["features"]
        states = load_json(STATES_URL, cache)["features"]

        def rings(f):
            return [np.array(r) for r in outer_rings(f["geometry"])]
        by = {f["properties"]["ADM0_A3"]: f for f in countries}
        self.brazil = rings(by["BRA"])
        self.venezuela = rings(by["VEN"])
        st = {f["properties"]["name"]: f for f in states if f["properties"]["adm0_a3"] == "BRA"}
        self.st = {k: rings(v) for k, v in st.items()}
        self.st_label = {k: (v["properties"]["longitude"], v["properties"]["latitude"]) for k, v in st.items()}
        self.south = ["Rio Grande do Sul", "Santa Catarina", "Paraná", "São Paulo", "Rio de Janeiro",
                      "Minas Gerais", "Espírito Santo"]
        self.north = ["Amazonas", "Roraima"]

        self.f_cap = ImageFont.truetype(FONT, 64)
        self.f_title = ImageFont.truetype(FONT, 76)
        self.f_lab = ImageFont.truetype(FONT_LABEL, 40)
        self.f_state = ImageFont.truetype(FONT, 38)
        self.f_big = ImageFont.truetype(FONT, 150)
        self.f_small = ImageFont.truetype(FONT_LABEL, 32)

    def prefetch(self, times):
        need = set()
        for T in times:
            cx, cy, zc = camera(T)
            need |= {("sat",) + k for k in self.sat.needed(cx, cy, zc)}
            need |= {("dem",) + k for k in self.dem.needed(cx, cy, zc)}
        todo = [k for k in need if not os.path.exists((self.sat if k[0] == "sat" else self.dem).path(*k[1:]))]
        print("tiles: %d needed, %d to download" % (len(need), len(todo)))
        with ThreadPoolExecutor(24) as ex:
            list(ex.map(lambda k: (self.sat if k[0] == "sat" else self.dem).download(*k[1:]), todo))

    def frame(self, T):
        cx, cy, zc = camera(T)
        lat = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * cy))))
        img = shade(self.sat.view(cx, cy, zc), self.dem.view(cx, cy, zc), zc, lat)
        scale = TILE * 2 ** zc

        def P(lon, lat_):
            x, y = mxy(lon, lat_)
            return np.stack([(x - cx) * scale + W / 2, (y - cy) * scale + H / 2], axis=-1)

        def ring_px(rs, step=1):
            return [P(r[::step, 0], r[::step, 1]) for r in rs]

        def fill(img, rs, col, a):
            if a <= 0:
                return img
            m = Image.new("L", (W, H), 0)
            d = ImageDraw.Draw(m)
            for p in ring_px(rs):
                if len(p) > 2:
                    d.polygon([tuple(q) for q in p], fill=255)
            k = np.asarray(m).astype(np.float32)[..., None] / 255 * a
            return img * (1 - k) + np.array(col, np.float32) * k

        # Darken a little everywhere except during the summit close-ups.
        img = img * (1 - 0.18 * (1 - float(np.clip((zc - 6) / 1.5, 0, 1))))

        # --- Brazil outline: drawn in the hook, kept while the map is wide.
        wide = 1 - float(np.clip((zc - 6.8) / 0.8, 0, 1))
        bt = ease_out(phase(T, 0.4, 3.6))
        if wide > 0 and bt > 0:
            lines = []
            for p in ring_px(self.brazil):
                seg = np.hypot(*np.diff(p, axis=0).T)
                cum = np.concatenate([[0], np.cumsum(seg)])
                k = int(np.searchsorted(cum, cum[-1] * bt))
                if k > 1:
                    lines.append(p[:k])
            img = draw_glow_lines(img, lines, 4, YELLOW * wide)

        # --- South/Southeast guess (5-9 s), then the north (9-13 s).
        a_s = smooth(phase(T, 5.4, 6.2)) * (1 - smooth(phase(T, 8.6, 9.3)))
        if a_s > 0:
            for name in self.south:
                img = fill(img, self.st[name], (1, 1, 1), 0.35 * a_s)
            img = draw_glow_lines(img, [p for n in self.south for p in ring_px(self.st[n])], 2, np.array([1, 1, 1.0]) * a_s)
        a_n = smooth(phase(T, 9.2, 10.0)) * (1 - smooth(phase(T, 16.5, 17.5)))
        if a_n > 0:
            for name in self.north:
                img = fill(img, self.st[name], (1.0, 0.83, 0.0), 0.3 * a_n)
        a_am = smooth(phase(T, 13.2, 14.2)) * (1 - smooth(phase(T, 18.2, 19.2)))
        if a_am > 0:
            img = draw_glow_lines(img, ring_px(self.st["Amazonas"]), 4, YELLOW * a_am)
            img = draw_glow_lines(img, ring_px(self.venezuela), 3, np.array([1, 1, 1.0]) * 0.8 * a_am)

        # --- Southeast: MG / ES border (39-46 s).
        a_se = smooth(phase(T, 41.5, 42.5)) * (1 - smooth(phase(T, 46.0, 46.8)))
        if a_se > 0:
            img = draw_glow_lines(img, ring_px(self.st["Minas Gerais"]) + ring_px(self.st["Espírito Santo"]), 4,
                                  YELLOW * a_se)

        out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))

        def pt(ll):
            p = P(np.array([ll[0]]), np.array([ll[1]]))[0]
            return float(p[0]), float(p[1])

        # --- labels
        if a_s > 0:
            x, y = pt((-51.5, -27.5))
            shadow_text(out, (x, y), "SUL ?", self.f_state, alpha=a_s)
            x, y = pt((-45.0, -19.5))
            shadow_text(out, (x, y), "SUDESTE ?", self.f_state, alpha=a_s)
        if a_n > 0 and T < 13.5:
            x, y = pt((-64.0, -3.5))
            shadow_text(out, (x, y), "EXTREMO NORTE", self.f_state, fill=(255, 214, 0), alpha=a_n)
            x, y = pt(NEBLINA)
            ph = (T * 1.1) % 1
            d = ImageDraw.Draw(out)
            r = 8 + 40 * ph
            d.ellipse([x - r, y - r, x + r, y + r], outline=(255, int(214 + 41 * ph), int(255 * ph)), width=4)
        if a_am > 0:
            x, y = pt(self.st_label["Amazonas"])
            shadow_text(out, (x, y), "AMAZONAS", self.f_state, fill=(255, 214, 0), alpha=a_am)
            x, y = pt((-66.2, 6.5))
            shadow_text(out, (x, y), "VENEZUELA", self.f_state, alpha=a_am)
        a_imeri = smooth(phase(T, 15.0, 15.8)) * (1 - smooth(phase(T, 21.0, 22.0)))
        if a_imeri > 0:
            x, y = pt((-65.9, 0.55))
            shadow_text(out, (x + 10, y + 70), "Serra do Imeri", self.f_lab, fill=(220, 235, 255), alpha=a_imeri)

        # --- summit pins
        if 18.6 <= T < 39.6:
            fade = 1 - phase(T, 39.0, 39.6)
            x, y = pt(NEBLINA)
            label = "Pico da Neblina" if T < 31 else "Pico da Neblina • 2.995 m"
            draw_pin(out, x, y, phase(T, 18.6, 19.3) * fade, label, self.f_lab, T=T)
        if 31.3 <= T < 39.6:
            fade = 1 - phase(T, 39.0, 39.6)
            x, y = pt(MARCO31)
            draw_pin(out, x, y, phase(T, 31.3, 32.0) * fade, "Pico 31 de Março • 2.974 m", self.f_lab,
                     colour=(255, 150, 0), T=T, below=True)
        if 42.6 <= T < 46.8:
            fade = 1 - phase(T, 46.0, 46.8)
            x, y = pt(BANDEIRA)
            draw_pin(out, x, y, phase(T, 42.6, 43.3) * fade, "Pico da Bandeira • 2.891 m", self.f_lab,
                     colour=(40, 160, 255), T=T)
            for name, dx in (("MINAS GERAIS", -230), ("ESPÍRITO SANTO", 230)):
                shadow_text(out, (x + dx, y + 200), name, self.f_state, alpha=fade * phase(T, 42.0, 42.8))

        # --- altitude gauge (23-31 s)
        a_g = smooth(phase(T, 23.2, 23.8)) * (1 - smooth(phase(T, 30.6, 31.2)))
        if a_g > 0:
            self.gauge(out, ease_out(phase(T, 23.4, 26.0)) * 2995, a_g)

        # --- final ranking (46-50 s)
        if T >= 46.4:
            a = smooth(phase(T, 46.4, 47.2))
            for ll, c in ((NEBLINA, (230, 30, 30)), (BANDEIRA, (40, 160, 255))):
                x, y = pt(ll)
                draw_pin(out, x, y, a, None, self.f_lab, colour=c, T=T)
            self.ranking(out, a)

        if T < 5.2:
            a = ease_out(phase(T, 0.2, 1.0)) * (1 - phase(T, 4.6, 5.2))
            caption(out, "ONDE FICAM AS *MAIORES MONTANHAS* DO BRASIL?", self.f_title, a, H * 0.14)
        for s, e, text in CAPTIONS:
            if s <= T <= e:
                caption(out, text, self.f_cap, min(1, (T - s) / 0.25, (e - T) / 0.25), H * 0.8)
        return out

    def gauge(self, out, value, a):
        lay = Image.new("RGBA", out.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        x0, x1, top, bot = W - 150, W - 100, H * 0.24, H * 0.66
        al = int(255 * a)
        d.rounded_rectangle([x0 - 90, top - 40, x1 + 40, bot + 40], radius=24, fill=(0, 0, 0, int(110 * a)))
        d.rounded_rectangle([x0, top, x1, bot], radius=12, fill=(255, 255, 255, int(60 * a)))
        y = bot - (bot - top) * value / 3000
        d.rounded_rectangle([x0, y, x1, bot], radius=12, fill=(255, 214, 0, al))
        for m in (0, 1000, 2000, 3000):
            ty = bot - (bot - top) * m / 3000
            d.line([x0 - 14, ty, x0 - 2, ty], fill=(255, 255, 255, al), width=3)
            d.text((x0 - 20, ty), "%d" % m, font=self.f_small, fill=(255, 255, 255, al), anchor="rm")
        out.paste(lay, (0, 0), lay)
        txt = "%s m" % format(int(round(value)), ",").replace(",", ".")
        shadow_text(out, (W / 2 - 60, H * 0.13), txt, self.f_big, fill=(255, 214, 0), alpha=a)
        shadow_text(out, (W / 2 - 60, H * 0.13 + 110), "acima do nível do mar", self.f_lab, alpha=a)

    def ranking(self, out, a):
        rows = [("1", "Pico da Neblina (AM)", 2995, (230, 30, 30)),
                ("2", "Pico 31 de Março (AM)", 2974, (255, 150, 0)),
                ("3", "Pico da Bandeira (MG/ES)", 2891, (40, 160, 255))]
        lay = Image.new("RGBA", out.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        x0, x1, y0 = 60, W - 60, H * 0.07
        rh = 96
        al = int(255 * a)
        d.rounded_rectangle([x0, y0, x1, y0 + rh * 3 + 40], radius=28, fill=(0, 0, 0, int(150 * a)))
        for i, (n, name, h, c) in enumerate(rows):
            y = y0 + 20 + rh * i + rh / 2
            k = clamp((a - i * 0.15) / 0.55)
            d.ellipse([x0 + 30, y - 28, x0 + 86, y + 28], fill=c + (int(al * k),))
            d.text((x0 + 58, y), n, font=self.f_lab, fill=(255, 255, 255, int(al * k)), anchor="mm")
            d.text((x0 + 110, y - 14), name, font=self.f_small, fill=(255, 255, 255, int(al * k)), anchor="lm")
            bw = (x1 - 60 - (x0 + 110)) * (h - 2700) / 300 * ease_out(k)
            d.rounded_rectangle([x0 + 110, y + 10, x0 + 110 + max(bw, 1), y + 28], radius=8, fill=c + (int(al * k),))
            d.text((x1 - 30, y - 14), "%s m" % format(h, ",").replace(",", "."), font=self.f_lab,
                   fill=(255, 214, 0, int(al * k)), anchor="rm")
        out.paste(lay, (0, 0), lay)


def clamp(t):
    return max(0.0, min(1.0, t))


def render_range(scene, i0, i1, path):
    ff = subprocess.Popen([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "%dx%d" % (W, H),
        "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p", path],
        stdin=subprocess.PIPE)
    for i in range(i0, i1):
        ff.stdin.write(scene.frame(i / FPS).tobytes())
        if i % FPS == 0:
            print("frame %d" % i, flush=True)
    ff.stdin.close()
    ff.wait()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("-o", "--out", default="brasil_montanhas.mp4")
    ap.add_argument("--duration", type=float, default=50)
    ap.add_argument("--jobs", type=int, default=os.cpu_count() or 1)
    ap.add_argument("--preview", type=float, nargs="*", help="write PNGs at these times instead of the video")
    ap.add_argument("--cache", default=os.path.expanduser("~/.cache/mapvideo"))
    a = ap.parse_args()

    sc = Scene(a.cache)
    n = int(a.duration * FPS)
    if a.preview is not None:
        sc.prefetch(a.preview)
        for t in a.preview:
            p = "%s_%04.1f.png" % (os.path.splitext(a.out)[0], t)
            sc.frame(t).save(p)
            print(p)
        return
    sc.prefetch([i / FPS for i in range(0, n, 3)] + [(n - 1) / FPS])

    # Render in parallel chunks, then join them without re-encoding.
    tmp = tempfile.mkdtemp(prefix="peaks-")
    chunks = [(n * k // a.jobs, n * (k + 1) // a.jobs, os.path.join(tmp, "part%d.mp4" % k)) for k in range(a.jobs)]
    pids = []
    for c in chunks:
        pid = os.fork()
        if pid == 0:
            render_range(sc, *c)
            os._exit(0)
        pids.append(pid)
    for pid in pids:
        os.waitpid(pid, 0)
    lst = os.path.join(tmp, "list.txt")
    with open(lst, "w") as f:
        f.writelines("file '%s'\n" % c[2] for c in chunks)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy",
                    "-movflags", "+faststart", a.out], check=True)
    print(a.out)


if __name__ == "__main__":
    main()
