# MegaEmbed — addon privado do Stremio

Resolve streams de filmes (`tmdb:ID`) e episódios (`tmdb:ID:T:E`) a partir do painel Xtream da MegaEmbed. As listas `get_vod_streams`, `get_series` e cada `get_series_info` ficam em `cache/*.json` por 2 horas.

```sh
npm install
# edite config.js (ou use MEGAEMBED_BASE_URL, MEGAEMBED_USERNAME, MEGAEMBED_PASSWORD)
npm start
```

No Stremio: **Addons → campo de busca → cole** `http://SEU_IP:7000/manifest.json`.

Rotas:
- `GET /manifest.json`
- `GET /stream/:type/:id.json`
- `GET /cache/status` — idade de cada arquivo de cache
- `POST /cache/refresh` — força a renovação das listas

O Stremio só pede streams `tmdb:` quando o item vem de um addon de catálogo que usa IDs do TMDB (ex.: "TMDB Addon"). Itens do Cinemeta usam IDs IMDb (`tt...`) e não chegam a este addon.
