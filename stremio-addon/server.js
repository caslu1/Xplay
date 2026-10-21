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
 *
 * Também publica catálogos ("MegaEmbed Filmes" / "MegaEmbed Séries") e as
 * páginas de detalhes (meta) montadas com os dados do próprio painel, para
 * que o conteúdo apareça no Stremio sem depender de outro addon.
 */
const fs = require('fs');
const path = require('path');
const express = require('express');
const cors = require('cors');
const axios = require('axios');

const config = require('./config');

// Aceita só o domínio ou a URL inteira copiada do painel (…/player_api.php?username=…).
const cleanUrl = (url) =>
  String(url || '')
    .trim()
    .replace(/\/player_api\.php.*$/i, '')
    .replace(/\/+$/, '');

// Endereço configurado primeiro, depois os espelhos do painel como reserva.
const PANEL_URLS = [...new Set([config.BASE_URL, ...(config.MIRRORS || [])].map(cleanUrl).filter(Boolean))];
// Endereço que respondeu por último; usado também nas URLs de stream.
let BASE_URL = PANEL_URLS[0];
const { USERNAME, PASSWORD } = config;
const PORT = config.PORT || 7000;
const CACHE_TTL_MS = (config.CACHE_TTL_SECONDS || 7200) * 1000;
const CACHE_DIR = path.join(__dirname, 'cache');
const PAGE_SIZE = 100;

const http = axios.create({
  timeout: 60000,
  headers: { 'User-Agent': 'stremio-megaembed-addon/1.0' },
});

// ---------------------------------------------------------------------------
// Manifesto
// ---------------------------------------------------------------------------

/** Monta o manifesto com os gêneros (categorias do painel) atuais. */
async function buildManifest() {
  const genres = async (name) => {
    try {
      return (await getList(name)).map((c) => c.category_name).filter(Boolean);
    } catch {
      return [];
    }
  };
  const [movieGenres, seriesGenres] = await Promise.all([genres('vod_categories'), genres('series_categories')]);

  const catalog = (type, id, name, options) => ({
    type,
    id,
    name,
    extra: [
      { name: 'search', isRequired: false },
      { name: 'genre', isRequired: false, options },
      { name: 'skip', isRequired: false },
    ],
    extraSupported: ['search', 'genre', 'skip'],
  });

  return {
    id: 'org.megaembed.private',
    version: '1.1.0',
    name: 'MegaEmbed',
    description: 'Filmes e séries privados do painel MegaEmbed (Xtream).',
    resources: ['catalog', 'meta', 'stream'],
    types: ['movie', 'series'],
    idPrefixes: ['tmdb:'],
    catalogs: [
      catalog('movie', 'megaembed-movies', 'MegaEmbed Filmes', movieGenres),
      catalog('series', 'megaembed-series', 'MegaEmbed Séries', seriesGenres),
    ],
    behaviorHints: { configurable: false, configurationRequired: false },
  };
}

// ---------------------------------------------------------------------------
// Cache em disco (2 horas)
// ---------------------------------------------------------------------------

/** Listas que ficam em cache. A chave vira o nome do arquivo .json. */
const LISTS = {
  vod_streams: { action: 'get_vod_streams' },
  series: { action: 'get_series' },
  vod_categories: { action: 'get_vod_categories' },
  series_categories: { action: 'get_series_categories' },
};

// Listas e índices em memória (tmdb_id -> itens), refeitos quando o arquivo muda.
const memory = {};
// Requisições em andamento, para não bater no painel duas vezes ao mesmo tempo.
const inflight = new Map();
// Último erro de cada chave de cache, exibido em /status.
const lastErrors = {};

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

/**
 * Chamada ao player_api.php com as credenciais. Se o endereço atual não
 * responder (domínio errado, fora do ar), tenta os outros espelhos.
 */
async function playerApi(params) {
  const order = [BASE_URL, ...PANEL_URLS.filter((url) => url !== BASE_URL)];
  let lastError;
  for (const base of order) {
    try {
      const { data } = await http.get(`${base}/player_api.php`, {
        params: { username: USERNAME, password: PASSWORD, ...params },
      });
      if (base !== BASE_URL) console.log(`[painel] usando ${base}`);
      BASE_URL = base;
      return data;
    } catch (err) {
      lastError = err;
      // O painel respondeu com erro HTTP: outro espelho não vai resolver.
      if (err.response) throw err;
    }
  }
  throw lastError;
}

/** Mensagem de erro legível, com o trecho da resposta do painel (sem a senha). */
function describeError(err) {
  if (!err.response) return err.message;
  const body = typeof err.response.data === 'string' ? err.response.data : JSON.stringify(err.response.data || '');
  const snippet = body.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 200);
  return `HTTP ${err.response.status}${snippet ? `: ${snippet}` : ''}`.split(PASSWORD).join('***');
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
      delete lastErrors[name];
      console.log(`[cache] ${name} atualizado`);
      return data;
    } catch (err) {
      lastErrors[name] = describeError(err);
      if (age !== null) {
        console.warn(`[cache] falha ao atualizar ${name} (${describeError(err)}); usando cópia antiga`);
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

/** ID do TMDB de um item; alguns painéis usam 'tmdb' em vez de 'tmdb_id'. */
function tmdbOf(item) {
  if (!item) return '';
  const raw = item.tmdb_id != null && item.tmdb_id !== '' ? item.tmdb_id : item.tmdb;
  const tmdb = raw != null ? String(raw).trim() : '';
  return /^\d+$/.test(tmdb) && tmdb !== '0' ? tmdb : '';
}

/** Converte a lista do painel num Map tmdb_id -> [itens]. */
function indexByTmdb(list) {
  const map = new Map();
  for (const item of Array.isArray(list) ? list : []) {
    const tmdb = tmdbOf(item);
    if (!tmdb) continue;
    if (!map.has(tmdb)) map.set(tmdb, []);
    map.get(tmdb).push(item);
  }
  return map;
}

// Categorias usadas para buscar uma lista aos pedaços quando o painel recusa a lista inteira.
const BY_CATEGORY = { vod_streams: 'vod_categories', series: 'series_categories' };

/** Busca uma lista no painel; se a lista inteira for recusada, junta categoria por categoria. */
async function fetchList(name) {
  const { action } = LISTS[name];
  try {
    const list = await playerApi(LISTS[name]);
    // Login inválido ou erro do painel vem como objeto: não pode ir para o cache.
    if (!Array.isArray(list)) throw new Error(`resposta inesperada do painel em ${action}`);
    return list;
  } catch (err) {
    if (!BY_CATEGORY[name]) throw err;
    console.warn(`[painel] ${action} falhou (${describeError(err)}); buscando por categoria`);

    const categories = await getList(BY_CATEGORY[name]);
    const merged = [];
    const seen = new Set();
    let answered = 0;
    for (const category of categories) {
      try {
        const part = await playerApi({ action, category_id: category.category_id });
        if (!Array.isArray(part)) continue;
        answered++;
        for (const item of part) {
          const key = item.stream_id != null ? item.stream_id : item.series_id;
          if (seen.has(key)) continue;
          seen.add(key);
          merged.push(item);
        }
      } catch (partErr) {
        console.warn(`[painel] ${action} categoria ${category.category_id} falhou: ${describeError(partErr)}`);
      }
    }
    if (!answered) throw err;
    console.log(`[painel] ${action}: ${merged.length} itens de ${answered}/${categories.length} categorias`);
    return merged;
  }
}

/** Lista + índice tmdb_id, reconstruídos só quando o arquivo de cache muda. */
async function loadList(name, opts) {
  const data = await cached(name, () => fetchList(name), opts);
  const mtime = fs.statSync(cacheFile(name)).mtimeMs;
  if (!memory[name] || memory[name].mtime !== mtime) {
    memory[name] = { mtime, list: data, index: indexByTmdb(data) };
  }
  return memory[name];
}

async function getList(name, opts) {
  return (await loadList(name, opts)).list;
}

async function getIndex(name, opts) {
  return (await loadList(name, opts)).index;
}

/** Detalhes de uma série (episódios), também em cache de 2h por série. */
function getSeriesInfo(seriesId, opts) {
  return cached(
    `series_info_${seriesId}`,
    async () => {
      const data = await playerApi({ action: 'get_series_info', series_id: seriesId });
      if (!data || typeof data !== 'object' || Array.isArray(data)) {
        throw new Error(`resposta inesperada do painel em get_series_info ${seriesId}`);
      }
      return data;
    },
    opts,
  );
}

/**
 * Episódios de um get_series_info como { season, episode, ep }.
 * 'episodes' vem como { "1": [ ... ], "2": [ ... ] } (às vezes como array);
 * a temporada sai do próprio episódio ou, na falta dela, da chave.
 */
function episodesOf(data) {
  const episodes = (data && data.episodes) || {};
  const groups = Array.isArray(episodes) ? episodes.map((group) => [null, group]) : Object.entries(episodes);
  const out = [];
  for (const [key, group] of groups) {
    for (const ep of Array.isArray(group) ? group : [group]) {
      if (!ep || typeof ep !== 'object') continue;
      const season = Number(ep.season != null && ep.season !== '' ? ep.season : key);
      const episode = Number(ep.episode_num);
      if (Number.isInteger(season) && Number.isInteger(episode)) out.push({ season, episode, ep });
    }
  }
  return out;
}

/** Detalhes de um filme (sinopse, elenco, capa), em cache de 2h por filme. */
function getVodInfo(streamId, opts) {
  return cached(`vod_info_${streamId}`, () => playerApi({ action: 'get_vod_info', vod_id: streamId }), opts);
}

// ---------------------------------------------------------------------------
// Catálogos e metadados
// ---------------------------------------------------------------------------

const CATALOGS = {
  'megaembed-movies': { type: 'movie', list: 'vod_streams', categories: 'vod_categories', sortKey: 'added' },
  'megaembed-series': { type: 'series', list: 'series', categories: 'series_categories', sortKey: 'last_modified' },
};

function firstImage(value) {
  const img = Array.isArray(value) ? value[0] : value;
  return typeof img === 'string' && /^https?:\/\//.test(img) ? img : undefined;
}

function splitList(value) {
  if (Array.isArray(value)) return value.filter(Boolean);
  return value ? String(value).split(/\s*[,/]\s*/).filter(Boolean) : [];
}

function yearOf(value) {
  const m = String(value || '').match(/\d{4}/);
  return m ? m[0] : undefined;
}

function rating(value) {
  const n = Number(value);
  return n > 0 ? n.toFixed(1) : undefined;
}

/** Converte datas do painel (texto ou timestamp unix) em ISO, se possível. */
function isoDate(value) {
  if (!value) return undefined;
  const d = /^\d+$/.test(String(value)) ? new Date(Number(value) * 1000) : new Date(value);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

/** Item resumido para a grade do catálogo. */
function toPreview(type, item) {
  return {
    id: `tmdb:${tmdbOf(item)}`,
    type,
    name: item.name,
    poster: firstImage(type === 'movie' ? item.stream_icon : item.cover),
    posterShape: 'poster',
    background: firstImage(item.backdrop_path),
    description: item.plot || undefined,
    genres: splitList(item.genre),
    releaseInfo: yearOf(item.releaseDate || item.year),
    imdbRating: rating(item.rating),
  };
}

async function catalogMetas(catalogId, extra) {
  const cat = CATALOGS[catalogId];
  const { list, index } = await loadList(cat.list);
  let items = [...index.values()].map((group) => group[0]);

  if (extra.genre) {
    const categories = await getList(cat.categories).catch(() => []);
    const wanted = categories.find((c) => c.category_name === extra.genre);
    items = wanted ? items.filter((i) => String(i.category_id) === String(wanted.category_id)) : [];
  }

  if (extra.search) {
    const q = extra.search.toLowerCase();
    items = items.filter((i) => String(i.name || '').toLowerCase().includes(q));
  } else {
    // Mais recentes primeiro (mantém a ordem do painel quando não há data).
    const order = new Map(list.map((item, pos) => [item, pos]));
    items.sort((a, b) => Number(b[cat.sortKey] || 0) - Number(a[cat.sortKey] || 0) || order.get(a) - order.get(b));
  }

  const skip = Math.max(0, Number(extra.skip) || 0);
  return items.slice(skip, skip + PAGE_SIZE).map((item) => toPreview(cat.type, item));
}

async function movieMeta(tmdbId) {
  const [movie] = (await getIndex('vod_streams')).get(tmdbId) || [];
  if (!movie) return null;

  const meta = toPreview('movie', movie);
  try {
    const info = (await getVodInfo(movie.stream_id)).info || {};
    Object.assign(meta, {
      poster: firstImage(info.cover_big) || firstImage(info.movie_image) || meta.poster,
      background: firstImage(info.backdrop_path) || meta.background,
      description: info.plot || info.description || meta.description,
      genres: splitList(info.genre).length ? splitList(info.genre) : meta.genres,
      director: splitList(info.director),
      cast: splitList(info.cast || info.actors),
      releaseInfo: yearOf(info.releasedate || info.release_date) || meta.releaseInfo,
      runtime: info.duration || undefined,
      imdbRating: rating(info.rating) || meta.imdbRating,
      trailers: info.youtube_trailer ? [{ source: info.youtube_trailer, type: 'Trailer' }] : undefined,
    });
  } catch (err) {
    console.warn(`[meta] get_vod_info ${movie.stream_id} falhou: ${err.message}`);
  }
  return meta;
}

async function seriesMeta(tmdbId) {
  const [show] = (await getIndex('series')).get(tmdbId) || [];
  if (!show) return null;

  const meta = toPreview('series', show);
  let data = {};
  try {
    data = await getSeriesInfo(show.series_id);
  } catch (err) {
    // Sem os episódios a página ainda abre com capa e sinopse da lista.
    console.warn(`[meta] get_series_info ${show.series_id} falhou: ${describeError(err)}`);
  }
  const info = (data && data.info) || {};

  Object.assign(meta, {
    poster: firstImage(info.cover) || meta.poster,
    background: firstImage(info.backdrop_path) || meta.background,
    description: info.plot || meta.description,
    genres: splitList(info.genre).length ? splitList(info.genre) : meta.genres,
    director: splitList(info.director),
    cast: splitList(info.cast),
    releaseInfo: yearOf(info.releaseDate) || meta.releaseInfo,
    imdbRating: rating(info.rating) || meta.imdbRating,
    trailers: info.youtube_trailer ? [{ source: info.youtube_trailer, type: 'Trailer' }] : undefined,
    videos: episodesOf(data).map(({ season, episode, ep }) => {
      const epInfo = ep.info || {};
      return {
        id: `tmdb:${tmdbId}:${season}:${episode}`,
        title: ep.title || `Episódio ${episode}`,
        season,
        episode,
        released: isoDate(epInfo.releasedate || epInfo.air_date || ep.added) || new Date(0).toISOString(),
        thumbnail: firstImage(epInfo.movie_image),
        overview: epInfo.plot || undefined,
      };
    }),
  });
  return meta;
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

    for (const found of episodesOf(info)) {
      if (found.season !== season || found.episode !== episode) continue;
      const { ep } = found;
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

app.get('/manifest.json', async (req, res) => {
  res.json(await buildManifest());
});

// Catálogo: /catalog/movie/megaembed-movies.json ou .../search=x&skip=100.json
app.get('/catalog/:type/:id/:extra?.json', async (req, res) => {
  const cat = CATALOGS[req.params.id];
  if (!cat || cat.type !== req.params.type) return res.json({ metas: [] });

  const extra = Object.fromEntries(new URLSearchParams(req.params.extra || ''));
  try {
    const metas = await catalogMetas(req.params.id, extra);
    // Lista vazia não fica guardada no Stremio por 2h, para aparecer assim que o painel responder.
    res.json({ metas, cacheMaxAge: metas.length ? CACHE_TTL_MS / 1000 : 60 });
  } catch (err) {
    console.error(`[catalog] ${req.params.id}: ${err.message}`);
    res.json({ metas: [] });
  }
});

app.get('/meta/:type/:id.json', async (req, res) => {
  const { type } = req.params;
  const parsed = parseId(req.params.id);
  if (!parsed || (type !== 'movie' && type !== 'series')) return res.status(404).json({ meta: null });

  try {
    const meta = type === 'movie' ? await movieMeta(parsed.tmdbId) : await seriesMeta(parsed.tmdbId);
    if (!meta) return res.status(404).json({ meta: null });
    // Série sem episódios (painel falhou) não fica guardada por 2h no Stremio.
    const incomplete = type === 'series' && !(meta.videos && meta.videos.length);
    res.json({ meta, cacheMaxAge: incomplete ? 60 : CACHE_TTL_MS / 1000 });
  } catch (err) {
    console.error(`[meta] ${type} ${req.params.id}: ${err.message}`);
    res.status(404).json({ meta: null });
  }
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

// Diagnóstico: o painel respondeu? Quantos itens têm ID do TMDB? (sem expor a senha)
app.get('/status', async (req, res) => {
  const report = async (name) => {
    try {
      const { list, index } = await loadList(name);
      const sample = list[0] || {};
      return {
        total: list.length,
        withTmdb: [...index.values()].reduce((n, group) => n + group.length, 0),
        sampleName: sample.name,
        sampleTmdb: sample.tmdb_id !== undefined ? sample.tmdb_id : sample.tmdb,
        sampleFields: Object.keys(sample),
        error: lastErrors[name],
      };
    } catch (err) {
      return { error: describeError(err) };
    }
  };
  const countOf = async (name) => {
    try {
      return (await getList(name)).length;
    } catch (err) {
      return describeError(err);
    }
  };

  const [movies, series, movieCategories, seriesCategories] = await Promise.all([
    report('vod_streams'),
    report('series'),
    countOf('vod_categories'),
    countOf('series_categories'),
  ]);

  // Testa os episódios da primeira série com ID do TMDB.
  let seriesInfoTest;
  try {
    const [first] = [...(await getIndex('series')).values()];
    if (first) {
      const show = first[0];
      const eps = episodesOf(await getSeriesInfo(show.series_id));
      seriesInfoTest = { id: `tmdb:${tmdbOf(show)}`, name: show.name, episodes: eps.length };
    }
  } catch (err) {
    seriesInfoTest = { error: describeError(err) };
  }

  res.json({
    panel: BASE_URL,
    tried: PANEL_URLS,
    username: USERNAME,
    movieCategories,
    seriesCategories,
    movies,
    series,
    seriesInfoTest,
  });
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
      getList('vod_categories', { force: true }).catch(() => []),
      getList('series_categories', { force: true }).catch(() => []),
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
