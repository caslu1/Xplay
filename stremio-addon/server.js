/**
 * Addon privado do Stremio para o painel MegaEmbed (Xtream Codes).
 *
 * O Stremio pede streams por ID do TMDB:
 *   filmes -> tmdb:{TMDB_ID}
 *   séries -> tmdb:{TMDB_ID}:{TEMPORADA}:{EPISODIO}
 *
 * O addon procura esse tmdb_id nas listas do painel (get_vod_streams /
 * get_series), que ficam em cache no disco por 2 horas, e devolve a URL
 * direta do arquivo no painel.
 */
const fs = require('fs');
const path = require('path');
const express = require('express');
const cors = require('cors');
const axios = require('axios');

const config = require('./config');

const BASE_URL = String(config.BASE_URL).replace(/\/+$/, '');
const { USERNAME, PASSWORD } = config;
const PORT = config.PORT || 7000;
const CACHE_TTL_MS = (config.CACHE_TTL_SECONDS || 7200) * 1000;
const CACHE_DIR = path.join(__dirname, 'cache');

const http = axios.create({
  baseURL: BASE_URL,
  timeout: 60000,
  headers: { 'User-Agent': 'stremio-megaembed-addon/1.0' },
});

// ---------------------------------------------------------------------------
// Manifesto
// ---------------------------------------------------------------------------

const manifest = {
  id: 'org.megaembed.private',
  version: '1.0.0',
  name: 'MegaEmbed',
  description: 'Streams privados de filmes e séries do painel MegaEmbed (Xtream).',
  resources: ['stream'],
  types: ['movie', 'series'],
  idPrefixes: ['tmdb:'],
  catalogs: [],
  behaviorHints: { configurable: false, configurationRequired: false },
};

// ---------------------------------------------------------------------------
// Cache em disco (2 horas)
// ---------------------------------------------------------------------------

/** Listas que ficam em cache. A chave vira o nome do arquivo .json. */
const LISTS = {
  vod_streams: { action: 'get_vod_streams' },
  series: { action: 'get_series' },
};

// Índices em memória (tmdb_id -> itens), refeitos quando o arquivo muda.
const memory = {};
// Requisições em andamento, para não bater no painel duas vezes ao mesmo tempo.
const inflight = new Map();

fs.mkdirSync(CACHE_DIR, { recursive: true });

function cacheFile(name) {
  return path.join(CACHE_DIR, `${name}.json`);
}

/** Idade do arquivo de cache em ms, ou null se ele não existir. */
function cacheAge(name) {
  try {
    return Date.now() - fs.statSync(cacheFile(name)).mtimeMs;
  } catch {
    return null;
  }
}

function readCache(name) {
  return JSON.parse(fs.readFileSync(cacheFile(name), 'utf8'));
}

/** Grava via arquivo temporário para nunca deixar um .json pela metade. */
function writeCache(name, data) {
  const file = cacheFile(name);
  const tmp = `${file}.${process.pid}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(data));
  fs.renameSync(tmp, file);
}

/** Chamada ao player_api.php com as credenciais. */
async function playerApi(params) {
  const { data } = await http.get('/player_api.php', {
    params: { username: USERNAME, password: PASSWORD, ...params },
  });
  return data;
}

/**
 * Devolve os dados de uma chave de cache.
 * - Arquivo com menos de 2h: lê do disco.
 * - Expirado ou inexistente: busca no painel e regrava.
 * - Se o painel falhar e houver um arquivo antigo, usa o antigo.
 */
async function cached(name, fetcher, { force = false } = {}) {
  const age = cacheAge(name);
  if (!force && age !== null && age < CACHE_TTL_MS) {
    return readCache(name);
  }

  if (inflight.has(name)) return inflight.get(name);

  const job = (async () => {
    try {
      const data = await fetcher();
      writeCache(name, data);
      console.log(`[cache] ${name} atualizado`);
      return data;
    } catch (err) {
      if (age !== null) {
        console.warn(`[cache] falha ao atualizar ${name} (${err.message}); usando cópia antiga`);
        return readCache(name);
      }
      throw err;
    } finally {
      inflight.delete(name);
    }
  })();

  inflight.set(name, job);
  return job;
}

/** Converte a lista do painel num Map tmdb_id -> [itens]. */
function indexByTmdb(list) {
  const map = new Map();
  for (const item of Array.isArray(list) ? list : []) {
    const tmdb = item && item.tmdb_id != null ? String(item.tmdb_id).trim() : '';
    if (!tmdb || tmdb === '0') continue;
    if (!map.has(tmdb)) map.set(tmdb, []);
    map.get(tmdb).push(item);
  }
  return map;
}

/** Índice tmdb_id de uma lista, reconstruído só quando o arquivo de cache muda. */
async function getIndex(name, opts) {
  const data = await cached(
    name,
    async () => {
      const list = await playerApi(LISTS[name]);
      // Login inválido ou erro do painel vem como objeto: não pode ir para o cache.
      if (!Array.isArray(list)) throw new Error(`resposta inesperada do painel em ${LISTS[name].action}`);
      return list;
    },
    opts,
  );
  const mtime = fs.statSync(cacheFile(name)).mtimeMs;
  if (!memory[name] || memory[name].mtime !== mtime) {
    memory[name] = { mtime, index: indexByTmdb(data) };
  }
  return memory[name].index;
}

/** Detalhes de uma série (episódios), também em cache de 2h por série. */
function getSeriesInfo(seriesId, opts) {
  return cached(
    `series_info_${seriesId}`,
    () => playerApi({ action: 'get_series_info', series_id: seriesId }),
    opts,
  );
}

// ---------------------------------------------------------------------------
// Resolução de streams
// ---------------------------------------------------------------------------

function streamUrl(kind, id, ext) {
  const user = encodeURIComponent(USERNAME);
  const pass = encodeURIComponent(PASSWORD);
  return `${BASE_URL}/${kind}/${user}/${pass}/${id}.${ext || 'mp4'}`;
}

/** "tmdb:123" ou "tmdb:123:1:2" -> { tmdbId, season, episode } */
function parseId(id) {
  const [prefix, tmdbId, season, episode] = String(id).split(':');
  if (prefix !== 'tmdb' || !/^\d+$/.test(tmdbId || '')) return null;
  return {
    tmdbId,
    season: season != null ? Number(season) : null,
    episode: episode != null ? Number(episode) : null,
  };
}

async function movieStreams(tmdbId) {
  const index = await getIndex('vod_streams');
  return (index.get(tmdbId) || []).map((movie) => ({
    name: 'MegaEmbed',
    title: `${movie.name}\n${String(movie.container_extension || 'mp4').toUpperCase()}`,
    url: streamUrl('movie', movie.stream_id, movie.container_extension),
    behaviorHints: { notWebReady: true },
  }));
}

async function seriesStreams(tmdbId, season, episode) {
  if (!Number.isInteger(season) || !Number.isInteger(episode)) return [];

  const index = await getIndex('series');
  const matches = index.get(tmdbId) || [];
  const streams = [];

  for (const show of matches) {
    let info;
    try {
      info = await getSeriesInfo(show.series_id);
    } catch (err) {
      console.warn(`[series] get_series_info ${show.series_id} falhou: ${err.message}`);
      continue;
    }

    // 'episodes' vem como { "1": [ ... ], "2": [ ... ] } (às vezes como array).
    const episodes = (info && info.episodes) || {};
    const seasonList = Array.isArray(episodes)
      ? episodes.flat().filter((ep) => Number(ep.season) === season)
      : episodes[String(season)] || [];

    for (const ep of seasonList) {
      if (Number(ep.episode_num) !== episode) continue;
      const label = ep.title || `${show.name} S${season}E${episode}`;
      streams.push({
        name: 'MegaEmbed',
        title: `${label}\n${String(ep.container_extension || 'mp4').toUpperCase()}`,
        url: streamUrl('series', ep.id, ep.container_extension),
        behaviorHints: { notWebReady: true, bingeGroup: `megaembed-${show.series_id}` },
      });
    }
  }

  return streams;
}

// ---------------------------------------------------------------------------
// Servidor HTTP
// ---------------------------------------------------------------------------

const app = express();
app.use(cors());

app.get('/', (req, res) => res.redirect('/manifest.json'));

app.get('/manifest.json', (req, res) => {
  res.json(manifest);
});

app.get('/stream/:type/:id.json', async (req, res) => {
  const { type } = req.params;
  const parsed = parseId(req.params.id);

  if (!parsed || (type !== 'movie' && type !== 'series')) {
    return res.json({ streams: [] });
  }

  try {
    const streams =
      type === 'movie'
        ? await movieStreams(parsed.tmdbId)
        : await seriesStreams(parsed.tmdbId, parsed.season, parsed.episode);

    // cacheMaxAge segue o TTL do cache local; o Stremio não repergunta antes disso.
    res.json({ streams, cacheMaxAge: streams.length ? CACHE_TTL_MS / 1000 : 300 });
  } catch (err) {
    console.error(`[stream] ${type} ${req.params.id}: ${err.message}`);
    res.json({ streams: [] });
  }
});

// Estado do cache: idade de cada arquivo e quanto falta para expirar.
app.get('/cache/status', (req, res) => {
  const files = fs.readdirSync(CACHE_DIR).filter((f) => f.endsWith('.json'));
  const status = files.map((file) => {
    const name = file.replace(/\.json$/, '');
    const ageSeconds = Math.round(cacheAge(name) / 1000);
    return {
      name,
      ageSeconds,
      expiresInSeconds: Math.max(0, CACHE_TTL_MS / 1000 - ageSeconds),
      fresh: ageSeconds * 1000 < CACHE_TTL_MS,
    };
  });
  res.json({ ttlSeconds: CACHE_TTL_MS / 1000, entries: status });
});

// Força a renovação das listas de filmes e séries (ignora as 2h).
app.post('/cache/refresh', async (req, res) => {
  try {
    const [vod, series] = await Promise.all([
      getIndex('vod_streams', { force: true }),
      getIndex('series', { force: true }),
    ]);
    res.json({ ok: true, movies: vod.size, series: series.size });
  } catch (err) {
    res.status(502).json({ ok: false, error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`MegaEmbed addon rodando em http://127.0.0.1:${PORT}/manifest.json`);

  // Aquece o cache ao iniciar para a primeira busca já sair rápida.
  Promise.all([getIndex('vod_streams'), getIndex('series')])
    .then(([vod, series]) => console.log(`[cache] ${vod.size} filmes e ${series.size} séries com tmdb_id`))
    .catch((err) => console.warn(`[cache] não foi possível carregar as listas: ${err.message}`));
});
