"""Fake Xtream Codes server with made-up channels, movies and series, used for screenshots.

Usage: python3 mock_server.py <port> <media_dir>
media_dir holds live.m3u8 (+ .ts segments) and movie.mp4 produced by ffmpeg.
"""
import base64
import colorsys
import io
import json
import os
import random
import sys
import time
import urllib.parse
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler

from PIL import Image, ImageDraw, ImageFont

PORT = int(sys.argv[1])
MEDIA = sys.argv[2]
USER, PASS = "demo", "demo"
random.seed(7)

LIVE = {
    "1": ("Abertos", ["Canal Um", "Rede Sol", "TV Horizonte", "Canal Brasil Novo", "Rede Aurora", "TV Litoral"]),
    "2": ("Esportes", ["Esporte Total", "Arena Sports", "Gol TV", "Fight Club", "Motor HD", "Tênis Mais"]),
    "3": ("Filmes e Séries", ["Cine Max", "Telecine Ação", "Série Mania", "Cine Clássico", "Studio 21"]),
    "4": ("Notícias", ["Notícias 24h", "Jornal Agora", "Economia TV"]),
    "5": ("Infantil", ["Kids Fun", "Desenhos HD", "Pequenos Gênios"]),
    "6": ("Documentários", ["Planeta Vivo", "História Viva", "Ciência Hoje"]),
}
MOVIE_CATS = {"10": "Ação", "11": "Aventura", "12": "Animação", "13": "Comédia", "14": "Drama", "15": "Terror"}
WORDS = ["Último", "Tiro", "Certo", "Noite", "Sombra", "Missão", "Fronteira", "Legado", "Código", "Horizonte",
         "Destino", "Tempestade", "Silêncio", "Resgate", "Império", "Segredo", "Caçada", "Origem"]
MOVIES = []
for cid in MOVIE_CATS:
    for i in range(10):
        MOVIES.append({
            "stream_id": len(MOVIES) + 1000,
            "name": "O " + " ".join(random.sample(WORDS, 2)),
            "category_id": cid,
            "rating": round(random.uniform(4.5, 8.9), 1),
            "added": int(time.time()) - random.randint(0, 9000000),
            "container_extension": "mp4",
        })
SERIES_CATS = {"20": "Drama", "21": "Suspense", "22": "Comédia"}
SERIES = []
for cid in SERIES_CATS:
    for i in range(7):
        SERIES.append({
            "series_id": len(SERIES) + 5000,
            "name": "A " + " ".join(random.sample(WORDS, 2)),
            "category_id": cid,
            "rating": round(random.uniform(5.5, 9.2), 1),
            "last_modified": int(time.time()) - random.randint(0, 9000000),
        })

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def font(size):
    try:
        return ImageFont.truetype(FONT, size)
    except OSError:
        return ImageFont.load_default()


def poster(text, seed):
    rnd = random.Random(seed)
    w, h = 300, 450
    hue = rnd.random()
    top = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.65, 0.55))
    bottom = tuple(int(c * 255) for c in colorsys.hsv_to_rgb((hue + 0.12) % 1, 0.8, 0.15))
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3)))
    for _ in range(6):
        x, y, r = rnd.randint(0, w), rnd.randint(0, h // 2), rnd.randint(30, 120)
        d.ellipse([x - r, y - r, x + r, y + r], outline=(255, 255, 255, 40), width=2)
    words = text.upper().split()
    y = h - 60 - 46 * len(words)
    for word in words:
        f = font(40 if len(word) < 9 else 30)
        tw = d.textlength(word, font=f)
        d.text(((w - tw) / 2, y), word, font=f, fill=(255, 255, 255))
        y += 46
    return img


def logo(text, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (200, 140), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    hue = rnd.random()
    color = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.15, 1.0))
    d.ellipse([60, 10, 140, 90], outline=color, width=8)
    initials = "".join(w[0] for w in text.split()[:2]).upper()
    f = font(30)
    tw = d.textlength(initials, font=f)
    d.text(((200 - tw) / 2, 32), initials, font=f, fill=color)
    f2 = font(18)
    label = text.upper()[:14]
    tw = d.textlength(label, font=f2)
    d.text(((200 - tw) / 2, 104), label, font=f2, fill=color)
    return img


def b64(s):
    return base64.b64encode(s.encode()).decode()


class Handler(SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def send_json(self, obj):
        body = json.dumps(obj).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def send_png(self, img):
        buf = io.BytesIO()
        img.save(buf, "PNG")
        body = buf.getvalue()
        self.send_response(200)
        self.send_header("Content-Type", "image/png")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(url.query).items()}
        base = f"http://{self.headers.get('Host')}"
        path = url.path

        if path.startswith("/img/poster/"):
            key = urllib.parse.unquote(path.rsplit("/", 1)[1][:-4])
            name = next((m["name"] for m in MOVIES + SERIES
                         if str(m.get("stream_id", m.get("series_id"))) == key), key)
            return self.send_png(poster(name, key))
        if path.startswith("/img/logo/"):
            key = path.rsplit("/", 1)[1][:-4]
            name = next((n for _, (_, names) in LIVE.items() for n in names if str(abs(hash(n)) % 99991) == key), "TV")
            return self.send_png(logo(name, key))
        if path.endswith(".m3u8") and path.startswith("/live/"):
            return self.serve_file(os.path.join(MEDIA, "live.m3u8"), "application/vnd.apple.mpegurl")
        if path.endswith(".ts"):
            return self.serve_file(os.path.join(MEDIA, os.path.basename(path)), "video/mp2t")
        if path.startswith("/movie/") or path.startswith("/series/"):
            return self.serve_file(os.path.join(MEDIA, "movie.mp4"), "video/mp4")
        if path != "/player_api.php":
            self.send_error(404)
            return
        if q.get("username") != USER or q.get("password") != PASS:
            return self.send_json({"user_info": {"auth": 0}})

        action = q.get("action")
        now = int(time.time())
        if action is None:
            return self.send_json({"user_info": {
                "auth": 1, "status": "Active", "username": USER, "exp_date": str(now + 86400 * 120),
                "created_at": str(now - 86400 * 200), "max_connections": "2", "active_cons": "1"}})
        if action == "get_live_categories":
            return self.send_json([{"category_id": k, "category_name": v[0]} for k, v in LIVE.items()])
        if action == "get_live_streams":
            out, num = [], 1
            for cid, (_, names) in LIVE.items():
                for n in names:
                    out.append({"num": num, "name": n, "stream_id": num, "category_id": cid,
                                "stream_icon": f"{base}/img/logo/{abs(hash(n)) % 99991}.png"})
                    num += 1
            return self.send_json(out)
        if action == "get_short_epg":
            start = now - now % 3600 - 1800
            shows = ["Jornal da Manhã", "Filme: O Último Resgate", "Esporte em Foco", "Novela das Seis"]
            sid = int(q.get("stream_id", "1"))
            listings = []
            for i in range(3):
                title = shows[(sid + i) % len(shows)]
                listings.append({"title": b64(title),
                                 "description": b64("Uma história emocionante sobre coragem, amizade e "
                                                    "superação, apresentada ao vivo para todo o Brasil."),
                                 "start_timestamp": start + i * 5400, "stop_timestamp": start + (i + 1) * 5400})
            return self.send_json({"epg_listings": listings})
        if action == "get_vod_categories":
            return self.send_json([{"category_id": k, "category_name": v} for k, v in MOVIE_CATS.items()])
        if action == "get_vod_streams":
            return self.send_json([dict(m, stream_icon=f"{base}/img/poster/{m['stream_id']}.png") for m in MOVIES])
        if action == "get_vod_info":
            m = next(x for x in MOVIES if str(x["stream_id"]) == q.get("vod_id"))
            return self.send_json({"info": {
                "o_name": m["name"], "plot": "Um ex-agente precisa impedir uma equipe de mercenários, liderada "
                "por um antigo parceiro, de tomar o controle de uma rede de defesa. Para isso, ele terá que "
                "enfrentar o próprio passado e decidir em quem pode confiar.",
                "genre": MOVIE_CATS[m["category_id"]] + ", Suspense, Drama", "duration_secs": 5880,
                "releasedate": "2025-09-22", "country": "Reino Unido", "rating": m["rating"],
                "cast": "Ana Souza, Bruno Lima, Carla Mendes", "director": "Diego Alves"},
                "movie_data": {"stream_id": m["stream_id"], "container_extension": "mp4"}})
        if action == "get_series_categories":
            return self.send_json([{"category_id": k, "category_name": v} for k, v in SERIES_CATS.items()])
        if action == "get_series":
            return self.send_json([dict(s, cover=f"{base}/img/poster/{s['series_id']}.png") for s in SERIES])
        if action == "get_series_info":
            s = next(x for x in SERIES if str(x["series_id"]) == q.get("series_id"))
            eps = {}
            for season in (1, 2):
                eps[str(season)] = [{
                    "id": f"{s['series_id']}{season}{e}", "episode_num": e, "title": f"Capítulo {e}",
                    "container_extension": "mp4",
                    "info": {"duration": "00:45:00", "plot": "Segredos vêm à tona e tudo muda para a família.",
                             "movie_image": f"{base}/img/poster/{s['series_id']}.png"}} for e in range(1, 7)]
            return self.send_json({"info": {"name": s["name"], "plot": "Uma família guarda um segredo que pode "
                                            "mudar a cidade inteira.", "genre": SERIES_CATS[s["category_id"]],
                                            "releaseDate": "2024-03-01", "cast": "Elenco Fictício",
                                            "rating": s["rating"]}, "episodes": eps})
        self.send_json([])

    def serve_file(self, file, ctype):
        if not os.path.exists(file):
            self.send_error(404)
            return
        size = os.path.getsize(file)
        start, end = 0, size - 1
        rng = self.headers.get("Range")
        if rng and rng.startswith("bytes="):
            a, _, b = rng[6:].partition("-")
            start = int(a) if a else 0
            end = int(b) if b else size - 1
            self.send_response(206)
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        else:
            self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(end - start + 1))
        self.end_headers()
        with open(file, "rb") as f:
            f.seek(start)
            remaining = end - start + 1
            while remaining > 0:
                chunk = f.read(min(65536, remaining))
                if not chunk:
                    break
                try:
                    self.wfile.write(chunk)
                except (BrokenPipeError, ConnectionResetError):
                    return
                remaining -= len(chunk)


ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
