# MegaEmbed — addon privado do Stremio

Mostra os filmes e séries do painel Xtream da MegaEmbed no Stremio (catálogos "MegaEmbed Filmes" e "MegaEmbed Séries", com busca e gêneros) e resolve os streams de filmes (`tmdb:ID`) e episódios (`tmdb:ID:T:E`). As respostas do painel ficam em `cache/*.json` por 2 horas. Só aparecem itens que têm `tmdb_id` no painel.

```sh
npm install
# edite config.js (ou use MEGAEMBED_BASE_URL, MEGAEMBED_USERNAME, MEGAEMBED_PASSWORD)
npm start
```

No Stremio: **Addons → campo de busca → cole** `http://SEU_IP:7000/manifest.json`.

Rotas:
- `GET /manifest.json`
- `GET /catalog/:type/:id/:extra?.json` — grade de filmes/séries (`search`, `genre`, `skip`)
- `GET /meta/:type/:id.json` — página de detalhes e lista de episódios
- `GET /stream/:type/:id.json`
- `GET /cache/status` — idade de cada arquivo de cache
- `POST /cache/refresh` — força a renovação das listas

Depois de atualizar o addon, desinstale e instale de novo no Stremio: ele guarda uma cópia do manifesto antigo.

Fora de `127.0.0.1` / rede local, o Stremio exige HTTPS no endereço do addon.

Itens do Cinemeta usam IDs IMDb (`tt...`) e não chegam a este addon; use os catálogos "MegaEmbed".
