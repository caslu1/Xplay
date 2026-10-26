# mapvideo

Vertical (1080x1920) map animations on real satellite imagery (NASA Blue
Marble), for Shorts/Reels/TikTok. Silent: add the music in the editor.

Requirements: Python 3 with `numpy`, `pillow`, `cairosvg`, plus `ffmpeg`.
Imagery, borders (Natural Earth) and flags (flag-icons) are downloaded on the
first run into `~/.cache/mapvideo`.

## Country: border, colour, flag, capital

```
python3 make_map_video.py China
python3 make_map_video.py Brasil --color "#009c3b" --border "#ffdf00"
python3 make_map_video.py "United States of America" --title EUA
python3 make_map_video.py Japão --preview 7      # one PNG instead of the video
```

## Antipode: "the other side of the world"

```
python3 make_antipode_video.py                    # São Paulo
python3 make_antipode_video.py --place Lisboa --lat 38.72 --lon -9.14
python3 make_antipode_video.py --captions legendas.json
```

`legendas.json` is a list of `[start_s, end_s, "text"]`; `*words*` show up in
yellow, `{place}` and `{ocean}` are replaced.

## Brazil's highest peaks

```
python3 make_peaks_video.py                       # brasil_montanhas.mp4, 50 s
python3 make_peaks_video.py --preview 20 34 44    # PNGs at those seconds
```

Blue Marble for the wide shots, Sentinel-2 at 10 m (ESA WorldCover composites,
read with `rasterio`) for the close-ups, shaded with real elevation (AWS
Terrain Tiles); renders on all CPU
cores (`--jobs`).
