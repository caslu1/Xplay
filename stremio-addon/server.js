#!/usr/bin/env node
// Xplay addon for Stremio: shows the live channels, movies and series of an
// Xtream Codes login. No dependencies, only Node 18+.
//
// The login travels inside the addon URL (base64url JSON), so one running
// server can serve any number of users:
//   http://host:7000/<config>/manifest.json

'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.PORT) || 7000;
const PAGE_SIZE = 100;
const CACHE_MS = 30 * 60 * 1000;
const VERSION = '1.0.0';

const cache = new Map();

function normalizeServer(dns) {
    let s = String(dns || '').trim();
    if (!s.includes('://')) s = 'http://' + s;
    return s.replace(/\/+$/, '');
}

function decodeConfig(str) {
    try {
        const c = JSON.parse(Buffer.from(str, 'base64url').toString('utf8'));
        if (!c.dns || !c.user || !c.pass) return null;
        return { server: normalizeServer(c.dns), user: String(c.user), pass: String(c.pass) };
    } catch (e) {
        return null;
    }
}

function enc(s) {
    return encodeURIComponent(s);
}

function api(cfg, action, extra) {
    let url = cfg.server + '/player_api.php?username=' + enc(cfg.user) + '&password=' + enc(cfg.pass);
    if (action) url += '&action=' + action;
    if (extra) url += extra;
    return url;
}

async function getJson(url) {
    const res = await fetch(url, {
        headers: { 'User-Agent': 'Xplay/2.0' },
        signal: AbortSignal.timeout(60000),
    });
    if (!res.ok) throw new Error('Servidor respondeu HTTP ' + res.status);
    return res.json();
}

// Category and stream lists are large and change rarely; keep them per login.
async function cached(cfg, action, extra) {
    const key = api(cfg, action, extra);
    const hit = cache.get(key);
    if (hit && Date.now() - hit.time < CACHE_MS) return hit.value;
    const value = getJson(key);
    cache.set(key, { time: Date.now(), value });
    try {
        return await value;
    } catch (e) {
        cache.delete(key);
        throw e;
    }
}

function list(v) {
    return Array.isArray(v) ? v : [];
}

function httpOrNull(s) {
    return typeof s === 'string' && s.startsWith('http') ? s : undefined;
}

function rating(s) {
    let r = parseFloat(s.rating);
    if (!(r > 0)) r = (parseFloat(s.rating_5based) || 0) * 2;
    return r > 0 ? Math.min(r, 10).toFixed(1) : undefined;
}

function isoDate(s) {
    const d = s ? new Date(s) : null;
    return d && !isNaN(d) ? d.toISOString() : undefined;
}

function b64(s) {
    if (!s) return '';
    try {
        return Buffer.from(s, 'base64').toString('utf8').trim();
    } catch (e) {
        return s;
    }
}

// Stremio types, Xtream actions and how to read each item.
const KINDS = {
    tv: {
        name: 'Xplay TV',
        categories: 'get_live_categories',
        streams: 'get_live_streams',
        id: s => s.stream_id,
        poster: s => httpOrNull(s.stream_icon),
        shape: 'square',
    },
    movie: {
        name: 'Xplay Filmes',
        categories: 'get_vod_categories',
        streams: 'get_vod_streams',
        id: s => s.stream_id,
        poster: s => httpOrNull(s.stream_icon),
        shape: 'poster',
    },
    series: {
        name: 'Xplay Séries',
        categories: 'get_series_categories',
        streams: 'get_series',
        id: s => s.series_id,
        poster: s => httpOrNull(s.cover),
        shape: 'poster',
    },
};

async function categoryMap(cfg, type) {
    const map = new Map();
    for (const c of list(await cached(cfg, KINDS[type].categories))) {
        map.set(String(c.category_id), String(c.category_name || '').trim());
    }
    return map;
}

async function manifest(cfg, base) {
    const catalogs = [];
    for (const type of Object.keys(KINDS)) {
        let genres = [];
        if (cfg) {
            try {
                genres = [...new Set((await categoryMap(cfg, type)).values())].filter(Boolean);
            } catch (e) {
                // Keep the manifest usable even if the server is slow right now.
            }
        }
        catalogs.push({
            type,
            id: 'xplay-' + type,
            name: KINDS[type].name,
            extra: [
                { name: 'genre', options: genres },
                { name: 'search' },
                { name: 'skip' },
            ],
        });
    }
    return {
        id: 'com.xplay.iptv',
        version: VERSION,
        name: 'Xplay',
        description: 'Canais ao vivo, filmes e séries da sua lista Xtream (DNS, usuário e senha).',
        logo: base + '/logo.png',
        types: ['tv', 'movie', 'series'],
        idPrefixes: ['xplay:'],
        resources: ['catalog', 'meta', 'stream'],
        catalogs,
        behaviorHints: { configurable: true, configurationRequired: !cfg },
    };
}

function preview(type, s) {
    const kind = KINDS[type];
    return {
        id: 'xplay:' + type + ':' + kind.id(s),
        type,
        name: String(s.name || '').trim(),
        poster: kind.poster(s),
        posterShape: kind.shape,
        imdbRating: rating(s),
    };
}

async function catalog(cfg, type, extra) {
    const kind = KINDS[type];
    if (!kind) return { metas: [] };
    let items = list(await cached(cfg, kind.streams));
    if (extra.genre) {
        const cats = await categoryMap(cfg, type);
        items = items.filter(s => cats.get(String(s.category_id)) === extra.genre);
    }
    if (extra.search) {
        const q = extra.search.toLowerCase();
        items = items.filter(s => String(s.name || '').toLowerCase().includes(q));
    }
    const skip = parseInt(extra.skip, 10) || 0;
    return { metas: items.slice(skip, skip + PAGE_SIZE).map(s => preview(type, s)) };
}

async function findItem(cfg, type, id) {
    return list(await cached(cfg, KINDS[type].streams)).find(s => String(KINDS[type].id(s)) === id);
}

async function meta(cfg, type, id) {
    const [, , itemId] = id.split(':');
    if (type === 'tv') {
        const s = await findItem(cfg, 'tv', itemId);
        if (!s) return { meta: null };
        const m = preview('tv', s);
        try {
            const epg = await getJson(api(cfg, 'get_short_epg', '&limit=2&stream_id=' + enc(itemId)));
            const now = list(epg.epg_listings).map(p => b64(p.title)).filter(Boolean);
            if (now.length) m.description = 'Agora: ' + now.join('  •  Depois: ');
        } catch (e) {
            // Many servers have no EPG; the channel still plays.
        }
        m.logo = m.poster;
        return { meta: m };
    }
    if (type === 'movie') {
        const data = await getJson(api(cfg, 'get_vod_info', '&vod_id=' + enc(itemId)));
        const info = data.info || {};
        const s = (await findItem(cfg, 'movie', itemId)) || data.movie_data || {};
        return {
            meta: {
                ...preview('movie', { ...s, stream_id: itemId, name: s.name || info.name }),
                poster: httpOrNull(info.movie_image) || httpOrNull(s.stream_icon),
                background: httpOrNull(list(info.backdrop_path)[0]),
                description: info.plot || info.description,
                releaseInfo: String(info.releasedate || '').slice(0, 4) || undefined,
                runtime: info.duration,
                genres: info.genre ? String(info.genre).split(/\s*[,/]\s*/) : undefined,
                director: info.director ? [info.director] : undefined,
                cast: info.cast ? String(info.cast).split(/\s*,\s*/) : undefined,
            },
        };
    }
    if (type === 'series') {
        const data = await getJson(api(cfg, 'get_series_info', '&series_id=' + enc(itemId)));
        const info = data.info || {};
        const videos = [];
        for (const [season, eps] of Object.entries(data.episodes || {})) {
            for (const ep of list(eps)) {
                const e = ep.info || {};
                videos.push({
                    id: 'xplay:episode:' + ep.id + ':' + (ep.container_extension || 'mp4'),
                    title: ep.title || 'Episódio ' + ep.episode_num,
                    season: Number(ep.season || season) || 1,
                    episode: Number(ep.episode_num) || 1,
                    thumbnail: httpOrNull(e.movie_image),
                    overview: e.plot,
                    released: isoDate(e.releasedate),
                });
            }
        }
        return {
            meta: {
                id,
                type,
                name: info.name || 'Série',
                poster: httpOrNull(info.cover),
                background: httpOrNull(list(info.backdrop_path)[0]),
                description: info.plot,
                releaseInfo: String(info.releaseDate || info.releasedate || '').slice(0, 4) || undefined,
                genres: info.genre ? String(info.genre).split(/\s*[,/]\s*/) : undefined,
                cast: info.cast ? String(info.cast).split(/\s*,\s*/) : undefined,
                imdbRating: rating(info),
                videos,
            },
        };
    }
    return { meta: null };
}

async function stream(cfg, type, id) {
    const parts = id.split(':');
    const login = enc(cfg.user) + '/' + enc(cfg.pass) + '/';
    if (parts[1] === 'tv') {
        return {
            streams: [
                { name: 'Xplay', title: 'HLS', url: cfg.server + '/live/' + login + parts[2] + '.m3u8' },
                { name: 'Xplay', title: 'MPEG-TS', url: cfg.server + '/live/' + login + parts[2] + '.ts' },
            ],
        };
    }
    if (parts[1] === 'movie') {
        const s = await findItem(cfg, 'movie', parts[2]);
        const ext = (s && s.container_extension) || 'mp4';
        return { streams: [{ name: 'Xplay', title: ext.toUpperCase(), url: cfg.server + '/movie/' + login + parts[2] + '.' + ext }] };
    }
    if (parts[1] === 'episode') {
        const ext = parts[3] || 'mp4';
        return { streams: [{ name: 'Xplay', title: ext.toUpperCase(), url: cfg.server + '/series/' + login + parts[2] + '.' + ext }] };
    }
    return { streams: [] };
}

// "genre=Esportes&skip=100" -> { genre: 'Esportes', skip: '100' }
function parseExtra(s) {
    const out = {};
    if (!s) return out;
    for (const [k, v] of new URLSearchParams(s)) out[k] = v;
    return out;
}

function send(res, code, body, type) {
    res.writeHead(code, {
        'Content-Type': type || 'application/json; charset=utf-8',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': '*',
        'Cache-Control': code === 200 && !type ? 'max-age=600' : 'no-cache',
    });
    res.end(type ? body : JSON.stringify(body));
}

const CONFIGURE_PAGE = fs.readFileSync(path.join(__dirname, 'configure.html'), 'utf8');
const LOGO = fs.readFileSync(path.join(__dirname, '..', 'app', 'res', 'drawable', 'ic_launcher.png'));

// Public address of this server, for absolute URLs (respects a reverse proxy).
function baseUrl(req) {
    const proto = String(req.headers['x-forwarded-proto'] || 'http').split(',')[0];
    return proto + '://' + req.headers.host;
}

async function route(req, res) {
    if (req.method === 'OPTIONS') return send(res, 204, '', 'text/plain');
    const parts = new URL(req.url, 'http://x').pathname.split('/').filter(Boolean).map(decodeURIComponent);

    if (parts.length === 0 || parts[parts.length - 1] === 'configure') {
        return send(res, 200, CONFIGURE_PAGE, 'text/html; charset=utf-8');
    }
    if (parts.length === 1 && parts[0] === 'logo.png') return send(res, 200, LOGO, 'image/png');
    if (parts.length === 1 && parts[0] === 'manifest.json') return send(res, 200, await manifest(null, baseUrl(req)));

    const cfg = decodeConfig(parts[0]);
    if (!cfg) return send(res, 400, { err: 'Configuração inválida. Abra /configure.' });
    const [, resource, type, rawId, rawExtra] = parts;
    if (resource === 'manifest.json') return send(res, 200, await manifest(cfg, baseUrl(req)));
    if (!rawId) return send(res, 404, { err: 'Não encontrado' });

    const id = rawId.replace(/\.json$/, '');
    const extra = parseExtra(rawExtra && rawExtra.replace(/\.json$/, ''));
    if (resource === 'catalog') return send(res, 200, await catalog(cfg, type, extra));
    if (resource === 'meta') return send(res, 200, await meta(cfg, type, id));
    if (resource === 'stream') return send(res, 200, await stream(cfg, type, id));
    return send(res, 404, { err: 'Não encontrado' });
}

if (require.main === module) {
    http.createServer((req, res) => {
        route(req, res).catch(e => {
            console.error(req.url.replace(/^\/[^/]+/, '/<config>'), e.message);
            send(res, 502, { err: e.message });
        });
    }).listen(PORT, () => console.log('Xplay addon em http://127.0.0.1:' + PORT + '/configure'));
}

module.exports = { route };
