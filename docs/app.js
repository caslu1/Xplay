/*
 * Painel do Xplay Launcher.
 * Lê e grava config/config.json no repositório do GitHub (API de Contents). Imagens e APKs enviados
 * vão para config/media/ e o launcher os baixa pelo raw.githubusercontent.com.
 */
'use strict';

const DEFAULTS = { repo: 'caslu1/Xplay', branch: 'ccr-c1251b75-o5q4e9' };
const CONFIG_PATH = 'config/config.json';
const MEDIA_DIR = 'config/media';

const ICONS = {
  dashboard: 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z',
  branding: 'M12 3a9 9 0 0 0 0 18c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm5 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z',
  ads: 'M18 11v2h4v-2h-4zm-2 6.61c.96.71 2.21 1.65 3.2 2.39.4-.53.8-1.07 1.2-1.6-.99-.74-2.24-1.68-3.2-2.4-.4.54-.8 1.08-1.2 1.61zM20.4 5.6c-.4-.53-.8-1.07-1.2-1.6-.99.74-2.24 1.68-3.2 2.4.4.53.8 1.07 1.2 1.6.96-.72 2.21-1.65 3.2-2.4zM4 9c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h1v4h2v-4h1l5 3V6L8 9H4zm11.5 3c0-1.33-.58-2.53-1.5-3.35v6.69c.92-.81 1.5-2.01 1.5-3.34z',
  buttons: 'M12 2 3 7v10l9 5 9-5V7l-9-5zm0 2.3L18.7 8 12 11.7 5.3 8 12 4.3zM5 9.7l6 3.3v6.6l-6-3.3V9.7zm8 9.9V13l6-3.3v6.6l-6 3.3z',
  grid: 'M4 4h4v4H4V4zm6 0h4v4h-4V4zm6 0h4v4h-4V4zM4 10h4v4H4v-4zm6 0h4v4h-4v-4zm6 0h4v4h-4v-4zM4 16h4v4H4v-4zm6 0h4v4h-4v-4zm6 0h4v4h-4v-4z',
  store: 'M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z',
  devices: 'M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 6a2 2 0 0 1 1 3.73V15h-2v-4.27A2 2 0 0 1 12 7z',
  plans: 'm21.41 11.58-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z',
  settings: 'M19.14 12.94a7.07 7.07 0 0 0 0-1.88l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84a.48.48 0 0 0-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.49.49 0 0 0-.59.22L2.74 8.87a.48.48 0 0 0 .12.61l2.03 1.58a7.4 7.4 0 0 0 0 1.88l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.49.49 0 0 0-.12-.61l-2.01-1.58zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z',
  themes: 'M12 22C6.49 22 2 17.51 2 12S6.49 2 12 2s10 4.04 10 9c0 3.31-2.69 6-6 6h-1.77c-.28 0-.5.22-.5.5 0 .12.05.23.13.33.41.47.64 1.06.64 1.67A2.5 2.5 0 0 1 12 22zm0-18c-4.41 0-8 3.59-8 8s3.59 8 8 8c.28 0 .5-.22.5-.5a.54.54 0 0 0-.14-.35c-.41-.46-.63-1.05-.63-1.65a2.5 2.5 0 0 1 2.5-2.5H16c2.21 0 4-1.79 4-4 0-3.86-3.59-7-8-7zM6.5 13a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm5 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z',
};

const PAGES = [
  { id: 'dashboard', group: 'main', title: 'Dashboard', render: pageDashboard },
  { id: 'branding', group: 'launcher', title: 'Branding (Fundo/Logo)', render: pageBranding },
  { id: 'ads', group: 'launcher', title: 'Propaganda', render: pageAds },
  { id: 'buttons', group: 'launcher', title: 'Botões Principais', render: pageButtons },
  { id: 'grid', group: 'launcher', title: 'Grade de Apps', render: pageGrid },
  { id: 'store', group: 'launcher', title: 'Loja de Apps (Store)', render: pageStore },
  { id: 'devices', group: 'launcher', title: 'Gestão de Dispositivos', render: pageDevices },
  { id: 'plans', group: 'launcher', title: 'Planos e Preços', render: pagePlans },
  { id: 'settings', group: 'launcher', title: 'Configurações', render: pageSettings },
  { id: 'themes', group: 'launcher', title: 'Gerenciar Temas', render: pageThemes },
];

const ACTION_TYPES = [
  ['none', 'Nada'], ['app', 'Abrir app (pacote)'], ['url', 'Abrir link / URL'], ['store', 'Abrir a loja'],
  ['allapps', 'Todos os apps'], ['settings', 'Configurações do Android'], ['wifi', 'Wi‑Fi'],
  ['launcher_settings', 'Configurações do launcher'],
];
const BUTTON_ICONS = [['', 'Ícone do app (automático)'], ['tv', 'TV'], ['store', 'Loja'], ['apps', 'Apps'],
  ['settings', 'Engrenagem'], ['android', 'Android'], ['wifi', 'Wi‑Fi'], ['refresh', 'Atualizar']];

let session = null;   // {repo, branch, token}
let cfg = null;       // the config being edited
let sha = null;       // blob sha of config.json, needed to overwrite it
let dirty = false;
let current = 'dashboard';

const $ = (s, el = document) => el.querySelector(s);

// ---------------------------------------------------------------------------------------------
// GitHub API
// ---------------------------------------------------------------------------------------------

async function gh(path, opts = {}) {
  const res = await fetch('https://api.github.com/repos/' + session.repo + path, {
    ...opts,
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: 'Bearer ' + session.token,
      'X-GitHub-Api-Version': '2022-11-28',
      ...(opts.body ? { 'Content-Type': 'application/json' } : {}),
    },
  });
  if (!res.ok) {
    let msg = res.status + ' ' + res.statusText;
    try { msg = (await res.json()).message || msg; } catch (e) { /* keep status */ }
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  return res.status === 204 ? null : res.json();
}

function utf8ToB64(text) {
  const bytes = new TextEncoder().encode(text);
  let bin = '';
  for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  return btoa(bin);
}

function b64ToUtf8(b64) {
  const bin = atob(b64.replace(/\n/g, ''));
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new TextDecoder().decode(bytes);
}

function rawUrl(path) {
  return 'https://raw.githubusercontent.com/' + session.repo + '/' + session.branch + '/' + path;
}

async function loadConfig() {
  const file = await gh('/contents/' + CONFIG_PATH + '?ref=' + encodeURIComponent(session.branch));
  sha = file.sha;
  cfg = normalize(JSON.parse(b64ToUtf8(file.content)));
}

async function saveConfig(message) {
  const body = {
    message: message || 'Painel: atualiza configuração do launcher',
    content: utf8ToB64(JSON.stringify(cfg, null, 2) + '\n'),
    branch: session.branch,
    sha,
  };
  try {
    const res = await gh('/contents/' + CONFIG_PATH, { method: 'PUT', body: JSON.stringify(body) });
    sha = res.content.sha;
  } catch (e) {
    if (e.status !== 409 && e.status !== 422) throw e;
    // Someone else saved meanwhile: take their sha and overwrite with what is on screen.
    const file = await gh('/contents/' + CONFIG_PATH + '?ref=' + encodeURIComponent(session.branch));
    body.sha = file.sha;
    const res = await gh('/contents/' + CONFIG_PATH, { method: 'PUT', body: JSON.stringify(body) });
    sha = res.content.sha;
  }
}

/** Uploads a file to config/media/ and returns its raw URL. */
async function uploadFile(file) {
  if (file.size > 90 * 1024 * 1024) throw new Error('Arquivo maior que 90 MB. Use um link externo.');
  const data = await new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(String(r.result).split(',')[1]);
    r.onerror = () => reject(r.error);
    r.readAsDataURL(file);
  });
  const safe = file.name.toLowerCase().replace(/[^a-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'arquivo';
  const path = MEDIA_DIR + '/' + Date.now() + '-' + safe;
  await gh('/contents/' + path, {
    method: 'PUT',
    body: JSON.stringify({ message: 'Painel: envia ' + safe, content: data, branch: session.branch }),
  });
  return rawUrl(path);
}

/** Fills in sections an older or hand-edited config may be missing. */
function normalize(c) {
  c = c || {};
  c.branding = Object.assign({ name: 'Xplay Launcher', logo: '', background: '', backgroundColor: '#05080F', overlay: 45 }, c.branding);
  c.theme = Object.assign({ active: '', themes: [] }, c.theme);
  c.ads = Object.assign({ enabled: true, interval: 8, items: [] }, c.ads);
  c.buttons = c.buttons || [];
  c.grid = Object.assign({ title: 'Meus apps', allowUserApps: true, apps: [] }, c.grid);
  c.store = Object.assign({ title: 'LOJA DE APPS', subtitle: '', apps: [] }, c.store);
  c.devices = Object.assign({ mode: 'open', message: '', allowed: [], blocked: [] }, c.devices);
  c.plans = c.plans || [];
  c.settings = Object.assign({ support: '', refreshMinutes: 30, showClock: true, showWifi: true, settingsPin: '', launcherUpdateUrl: '' }, c.settings);
  return c;
}

// ---------------------------------------------------------------------------------------------
// Small DOM helpers
// ---------------------------------------------------------------------------------------------

function h(tag, attrs, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === undefined || v === null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k === 'value') el.value = v;
    else if (k === 'checked') el.checked = v;
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const kid of kids.flat()) {
    if (kid === null || kid === undefined || kid === false) continue;
    el.append(kid instanceof Node ? kid : document.createTextNode(String(kid)));
  }
  return el;
}

function svg(name) {
  const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  s.setAttribute('viewBox', '0 0 24 24');
  const p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  p.setAttribute('d', ICONS[name] || '');
  s.append(p);
  return s;
}

function toast(text, isError) {
  const t = h('div', { class: 'toast' + (isError ? ' err' : '') }, text);
  document.body.append(t);
  setTimeout(() => t.remove(), isError ? 6000 : 2600);
}

function touch() {
  dirty = true;
  $('#dirty').classList.remove('hidden');
}

function card(title, hint, ...kids) {
  return h('div', { class: 'card' }, h('h3', {}, title), hint ? h('p', { class: 'hint' }, hint) : null, ...kids);
}

/** Text-like field bound to obj[key]. */
function field(obj, key, label, opts = {}) {
  const input = h(opts.textarea ? 'textarea' : 'input', {
    type: opts.textarea ? undefined : (opts.type || 'text'),
    value: obj[key] ?? '',
    placeholder: opts.placeholder,
    min: opts.min, max: opts.max,
    oninput: (e) => {
      const v = e.target.value;
      obj[key] = opts.type === 'number' ? (v === '' ? 0 : Number(v)) : v;
      touch();
      if (opts.onchange) opts.onchange(obj[key]);
    },
  });
  return h('label', { class: 'f' }, h('span', {}, label), input);
}

function checkbox(obj, key, label, onchange) {
  return h('label', { class: 'check' },
    h('input', { type: 'checkbox', checked: !!obj[key], onchange: (e) => { obj[key] = e.target.checked; touch(); if (onchange) onchange(); } }),
    label);
}

function select(obj, key, label, options, onchange) {
  const sel = h('select', { onchange: (e) => { obj[key] = e.target.value; touch(); if (onchange) onchange(e.target.value); } },
    options.map(([v, t]) => h('option', { value: v, selected: String(obj[key] ?? '') === v }, t)));
  return label ? h('label', { class: 'f' }, h('span', {}, label), sel) : sel;
}

/** Android colors may carry alpha (#AARRGGBB); the picker edits the RGB part and keeps the alpha. */
function colorField(obj, key, label) {
  const text = h('input', { type: 'text', value: obj[key] || '' });
  const rgb = () => {
    const v = (obj[key] || '').trim();
    if (/^#[0-9a-f]{8}$/i.test(v)) return '#' + v.slice(3);
    if (/^#[0-9a-f]{6}$/i.test(v)) return v;
    return '#000000';
  };
  const pick = h('input', { type: 'color', value: rgb() });
  text.addEventListener('input', () => { obj[key] = text.value.trim(); pick.value = rgb(); touch(); });
  pick.addEventListener('input', () => {
    const v = (obj[key] || '').trim();
    const alpha = /^#[0-9a-f]{8}$/i.test(v) ? v.slice(1, 3) : '';
    obj[key] = ('#' + alpha + pick.value.slice(1)).toUpperCase();
    text.value = obj[key];
    touch();
  });
  return h('label', { class: 'f' }, h('span', {}, label), h('div', { class: 'inline' }, pick, text));
}

/** URL field with upload button and image preview. */
function mediaField(obj, key, label, opts = {}) {
  const input = h('input', { type: 'url', value: obj[key] || '', placeholder: 'https://… ou envie um arquivo' });
  const prev = opts.image === false ? null : h('img', { class: 'preview', alt: '' });
  const refresh = () => {
    if (!prev) return;
    prev.src = obj[key] || '';
    prev.classList.toggle('hidden', !obj[key]);
  };
  input.addEventListener('input', () => { obj[key] = input.value.trim(); touch(); refresh(); });
  const fileInput = h('input', { type: 'file', class: 'hidden', accept: opts.accept || 'image/*' });
  const btn = h('button', { class: 'btn', type: 'button', onclick: () => fileInput.click() }, 'Enviar');
  fileInput.addEventListener('change', async () => {
    const f = fileInput.files[0];
    if (!f) return;
    btn.disabled = true;
    btn.textContent = 'Enviando…';
    try {
      obj[key] = await uploadFile(f);
      input.value = obj[key];
      touch();
      refresh();
      if (opts.onupload) opts.onupload(f);
      toast('Arquivo enviado. Clique em Publicar para aplicar.');
    } catch (e) {
      toast('Falha no envio: ' + e.message, true);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Enviar';
      fileInput.value = '';
    }
  });
  refresh();
  return h('label', { class: 'f' }, h('span', {}, label), h('div', { class: 'inline' }, h('div', { class: 'grow' }, input), btn, fileInput), prev);
}

function actionField(obj, key, label) {
  obj[key] = obj[key] || { type: 'none', value: '' };
  const a = obj[key];
  const value = h('input', { type: 'text', value: a.value || '', oninput: (e) => { a.value = e.target.value.trim(); touch(); } });
  const sync = () => {
    const needs = a.type === 'app' || a.type === 'url';
    value.classList.toggle('hidden', !needs);
    value.placeholder = a.type === 'app' ? 'com.exemplo.app (vários: separe por vírgula)' : 'https://…';
  };
  const sel = select(a, 'type', null, ACTION_TYPES, sync);
  sel.style.flex = '0 0 46%';
  sync();
  return h('label', { class: 'f' }, h('span', {}, label), h('div', { class: 'inline' }, sel, h('div', { class: 'grow' }, value)));
}

/** Editable list: each entry rendered by renderItem(entry), with move up/down and remove. */
function listEditor(arr, opts) {
  const box = h('div');
  const draw = () => {
    box.innerHTML = '';
    if (!arr.length) box.append(h('div', { class: 'empty' }, opts.empty || 'Nada cadastrado ainda.'));
    arr.forEach((item, i) => {
      const head = h('div', { class: 'item-head' },
        h('b', {}, opts.title(item, i)),
        h('button', { class: 'btn small', type: 'button', disabled: i === 0, onclick: () => { [arr[i - 1], arr[i]] = [arr[i], arr[i - 1]]; touch(); draw(); } }, '↑'),
        h('button', { class: 'btn small', type: 'button', disabled: i === arr.length - 1, onclick: () => { [arr[i + 1], arr[i]] = [arr[i], arr[i + 1]]; touch(); draw(); } }, '↓'),
        h('button', { class: 'btn small danger', type: 'button', onclick: () => { if (confirm('Remover este item?')) { arr.splice(i, 1); touch(); draw(); } } }, 'Remover'));
      box.append(h('div', { class: 'item' }, head, opts.render(item, i, draw)));
    });
    box.append(h('button', { class: 'btn', type: 'button', onclick: () => { arr.push(opts.create()); touch(); draw(); } }, '+ ' + (opts.addLabel || 'Adicionar')));
  };
  draw();
  return box;
}

// ---------------------------------------------------------------------------------------------
// Pages
// ---------------------------------------------------------------------------------------------

function deviceStatus(d) {
  if (d.disabled) return ['Desativado', 'bad'];
  if (d.expires) {
    const today = new Date().toISOString().slice(0, 10);
    if (d.expires < today) return ['Vencido', 'bad'];
    const days = Math.round((new Date(d.expires) - new Date(today)) / 86400000);
    if (days <= 5) return ['Vence em ' + days + 'd', 'warn'];
  }
  return ['Ativo', 'ok'];
}

function pageDashboard(root) {
  const devs = cfg.devices.allowed;
  const active = devs.filter((d) => deviceStatus(d)[1] !== 'bad').length;
  const stat = (n, t) => h('div', { class: 'stat' }, h('b', {}, n), h('span', {}, t));
  root.append(h('div', { class: 'stats' },
    stat(cfg.buttons.length, 'Botões principais'),
    stat(cfg.grid.apps.length, 'Apps na grade'),
    stat(cfg.store.apps.length, 'Apps na loja'),
    stat(cfg.ads.items.length, 'Propagandas'),
    stat(active + ' / ' + devs.length, 'Dispositivos ativos'),
    stat(cfg.devices.blocked.length, 'Bloqueados')));

  const apkBox = h('p', { class: 'hint' }, 'Carregando…');
  root.append(card('APK do launcher', 'Instale este APK na TV Box, Fire TV ou Android TV (Downloader, pendrive ou navegador).', apkBox));
  fetch(rawUrl('releases/launcher/latest.json') + '?t=' + Date.now()).then((r) => r.ok ? r.json() : Promise.reject(r.status))
    .then((j) => {
      apkBox.innerHTML = '';
      apkBox.append('Versão ', h('b', {}, j.versionName), ' — ', h('a', { href: j.apk, target: '_blank', rel: 'noopener' }, 'baixar APK'),
        h('br'), h('span', { class: 'mono', style: 'font-size:12px;word-break:break-all' }, j.apk));
    })
    .catch(() => { apkBox.textContent = 'Nenhuma versão publicada em releases/launcher/latest.json.'; });

  const mode = cfg.devices.mode === 'whitelist' ? 'Somente aparelhos cadastrados' : 'Aberto (todos, menos bloqueados)';
  root.append(card('Resumo', null,
    h('p', {}, 'Nome: ', h('b', {}, cfg.branding.name)),
    h('p', {}, 'Acesso: ', h('b', {}, mode)),
    h('p', {}, 'Repositório: ', h('span', { class: 'mono' }, session.repo + ' @ ' + session.branch)),
    h('p', { class: 'hint' }, 'Depois de publicar, o launcher aplica as mudanças ao abrir, no botão ⟳ ou a cada ' + cfg.settings.refreshMinutes + ' minutos.')));

  const hist = h('div', {}, h('p', { class: 'hint' }, 'Carregando…'));
  root.append(card('Últimas publicações', null, hist));
  gh('/commits?path=' + CONFIG_PATH + '&sha=' + encodeURIComponent(session.branch) + '&per_page=6').then((list) => {
    hist.innerHTML = '';
    if (!list.length) hist.append(h('p', { class: 'hint' }, 'Nenhuma ainda.'));
    list.forEach((c) => hist.append(h('p', { style: 'margin:6px 0' },
      h('span', { class: 'mono', style: 'color:var(--muted)' }, new Date(c.commit.author.date).toLocaleString('pt-BR')), '  ', c.commit.message.split('\n')[0])));
  }).catch((e) => { hist.innerHTML = ''; hist.append(h('p', { class: 'hint' }, 'Não foi possível carregar: ' + e.message)); });
}

function pageBranding(root) {
  const b = cfg.branding;
  const overlayLabel = h('span', {}, 'Escurecer o fundo: ' + b.overlay + '%');
  root.append(card('Identidade', 'Nome e logo exibidos no topo do launcher.',
    field(b, 'name', 'Nome do launcher', { onchange: (v) => { $('#side-name').textContent = v || 'Xplay'; } }),
    mediaField(b, 'logo', 'Logo (PNG transparente, ~520×120)')));
  root.append(card('Fundo', 'Imagem de fundo da tela inicial (1920×1080 recomendado).',
    mediaField(b, 'background', 'Imagem de fundo'),
    colorField(b, 'backgroundColor', 'Cor de fundo (sem imagem ou enquanto carrega)'),
    h('label', { class: 'f' }, overlayLabel,
      h('input', { type: 'range', min: 0, max: 90, value: b.overlay, oninput: (e) => { b.overlay = Number(e.target.value); overlayLabel.textContent = 'Escurecer o fundo: ' + b.overlay + '%'; touch(); } }))));
}

function pageAds(root) {
  const a = cfg.ads;
  root.append(card('Banner rotativo', 'Aparece em destaque à esquerda dos botões principais.',
    checkbox(a, 'enabled', 'Mostrar propagandas'),
    field(a, 'interval', 'Trocar a cada (segundos)', { type: 'number', min: 3 })));
  root.append(card('Propagandas', 'Imagens 16:9 (1280×720). A ação roda quando o usuário clica no banner.',
    listEditor(a.items, {
      title: (it, i) => it.title || 'Propaganda ' + (i + 1),
      create: () => ({ image: '', title: '', action: { type: 'none', value: '' } }),
      addLabel: 'Nova propaganda',
      render: (it) => h('div', {}, mediaField(it, 'image', 'Imagem'), field(it, 'title', 'Texto sobre a imagem (opcional)'), actionField(it, 'action', 'Ao clicar')),
    })));
}

function pageButtons(root) {
  root.append(card('Botões principais', 'Blocos grandes da tela inicial. Até 4 ficam em 2 colunas; mais que isso, em 3.',
    listEditor(cfg.buttons, {
      title: (b, i) => b.label || 'Botão ' + (i + 1),
      create: () => ({ label: 'NOVO BOTÃO', icon: '', image: '', color: '#2563EB', action: { type: 'app', value: '' } }),
      addLabel: 'Novo botão',
      render: (b) => h('div', {},
        h('div', { class: 'grid2' }, field(b, 'label', 'Texto'), colorField(b, 'color', 'Cor')),
        h('div', { class: 'grid2' }, select(b, 'icon', 'Ícone', BUTTON_ICONS.some(([v]) => v === b.icon) ? BUTTON_ICONS : BUTTON_ICONS.concat([[b.icon, 'Imagem: ' + b.icon]])),
          actionField(b, 'action', 'Ao clicar')),
        mediaField(b, 'image', 'Imagem de capa (opcional, cobre o botão inteiro)')),
    })));
}

function pageGrid(root) {
  const g = cfg.grid;
  root.append(card('Fileira de apps', 'Apps mostrados na parte de baixo da tela inicial.',
    field(g, 'title', 'Título da fileira'),
    checkbox(g, 'allowUserApps', 'Permitir que o usuário adicione apps com o botão +')));
  root.append(card('Apps', 'Use o nome do pacote Android. Para TV e celular, liste os dois separados por vírgula (ex.: com.google.android.youtube.tv,com.google.android.youtube).',
    listEditor(g.apps, {
      title: (a, i) => a.label || a.package || 'App ' + (i + 1),
      create: () => ({ label: '', package: '', icon: '' }),
      addLabel: 'Novo app',
      render: (a) => h('div', {},
        h('div', { class: 'grid2' }, field(a, 'label', 'Nome'), field(a, 'package', 'Pacote(s)', { placeholder: 'com.exemplo.app' })),
        mediaField(a, 'icon', 'Ícone personalizado (opcional — vazio usa o ícone do app)'),
        checkbox(a, 'hideIfMissing', 'Esconder quando o app não estiver instalado')),
    })));
}

function pageStore(root) {
  const s = cfg.store;
  root.append(card('Loja', 'Título exibido no topo da loja do launcher.',
    h('div', { class: 'grid2' }, field(s, 'title', 'Título'), field(s, 'subtitle', 'Subtítulo'))));
  root.append(card('Apps da loja', 'Envie o APK (até 90 MB) ou cole um link direto. Aumente o "versionCode" para o launcher oferecer a atualização.',
    listEditor(s.apps, {
      title: (a, i) => (a.name || 'App ' + (i + 1)) + (a.versionName ? ' v' + a.versionName : ''),
      create: () => ({ name: '', package: '', versionCode: 1, versionName: '1.0', icon: '', apk: '', description: '' }),
      addLabel: 'Novo app na loja',
      render: (a) => h('div', {},
        h('div', { class: 'grid2' }, field(a, 'name', 'Nome'), field(a, 'package', 'Pacote', { placeholder: 'com.exemplo.app' })),
        h('div', { class: 'grid2' }, field(a, 'versionCode', 'versionCode', { type: 'number', min: 1 }), field(a, 'versionName', 'Versão (texto)')),
        mediaField(a, 'icon', 'Ícone'),
        mediaField(a, 'apk', 'Arquivo APK', { image: false, accept: '.apk,application/vnd.android.package-archive' }),
        field(a, 'description', 'Descrição', { textarea: true })),
    })));
}

function pageDevices(root) {
  const d = cfg.devices;
  root.append(card('Modo de acesso', 'O código aparece no launcher em Configurações e na tela de bloqueio.',
    select(d, 'mode', 'Quem pode usar o launcher', [['open', 'Aberto: todos, menos os bloqueados'], ['whitelist', 'Somente aparelhos cadastrados e dentro da validade']]),
    field(d, 'message', 'Mensagem na tela de bloqueio', { textarea: true })));

  const planOpts = [['', '—']].concat(cfg.plans.map((p) => [p.name, p.name]));
  const filter = h('input', { type: 'text', placeholder: 'Buscar por código, nome ou observação…' });
  const tbody = h('tbody');
  const addDays = (dev, days) => {
    const today = new Date().toISOString().slice(0, 10);
    const base = dev.expires && dev.expires > today ? new Date(dev.expires) : new Date(today);
    base.setDate(base.getDate() + days);
    dev.expires = base.toISOString().slice(0, 10);
  };
  const draw = () => {
    tbody.innerHTML = '';
    const q = filter.value.trim().toLowerCase();
    d.allowed.forEach((dev, i) => {
      if (q && ![dev.id, dev.name, dev.note].join(' ').toLowerCase().includes(q)) return;
      const [label, cls] = deviceStatus(dev);
      const bind = (key, attrs = {}) => h('input', { type: attrs.type || 'text', value: dev[key] || '', class: attrs.class, placeholder: attrs.placeholder,
        oninput: (e) => { dev[key] = e.target.value.trim(); touch(); }, onchange: attrs.redraw ? draw : undefined });
      const planSel = select(dev, 'plan', null, planOpts, (name) => {
        const plan = cfg.plans.find((p) => p.name === name);
        if (plan && plan.days) { addDays(dev, Number(plan.days)); draw(); }
      });
      tbody.append(h('tr', {},
        h('td', {}, bind('id', { class: 'mono', placeholder: 'XXXX-XXXX-XXXX' })),
        h('td', {}, bind('name', { placeholder: 'Cliente' })),
        h('td', {}, planSel),
        h('td', {}, bind('expires', { type: 'date', redraw: true })),
        h('td', {}, h('span', { class: 'tag ' + cls }, label)),
        h('td', {}, bind('note', { placeholder: 'Observação' })),
        h('td', { style: 'white-space:nowrap' },
          h('button', { class: 'btn small', type: 'button', title: 'Mais 30 dias', onclick: () => { addDays(dev, 30); touch(); draw(); } }, '+30d'), ' ',
          h('button', { class: 'btn small', type: 'button', onclick: () => { dev.disabled = !dev.disabled; touch(); draw(); } }, dev.disabled ? 'Ativar' : 'Desativar'), ' ',
          h('button', { class: 'btn small danger', type: 'button', onclick: () => { if (confirm('Remover ' + (dev.id || 'aparelho') + '?')) { d.allowed.splice(i, 1); touch(); draw(); } } }, '✕'))));
    });
    if (!tbody.children.length) tbody.append(h('tr', {}, h('td', { colspan: 7, class: 'empty' }, 'Nenhum aparelho.')));
  };
  filter.addEventListener('input', draw);
  draw();
  root.append(card('Aparelhos cadastrados', 'Escolher um plano soma os dias dele à validade. Deixe a validade vazia para não vencer.',
    h('div', { class: 'inline', style: 'margin-bottom:12px' }, h('div', { class: 'grow' }, filter),
      h('button', { class: 'btn primary', type: 'button', onclick: () => { d.allowed.unshift({ id: '', name: '', plan: '', expires: '', note: '' }); filter.value = ''; touch(); draw(); tbody.querySelector('input').focus(); } }, '+ Aparelho')),
    h('div', { class: 'table-wrap' }, h('table', {},
      h('thead', {}, h('tr', {}, ['Código', 'Nome', 'Plano', 'Validade', 'Situação', 'Obs.', ''].map((t) => h('th', {}, t)))), tbody))));

  root.append(card('Bloqueados', 'Estes aparelhos nunca abrem o launcher, em qualquer modo.',
    listEditor(d.blocked, {
      title: (b, i) => b.id || 'Aparelho ' + (i + 1),
      create: () => ({ id: '', note: '' }),
      addLabel: 'Bloquear aparelho',
      render: (b) => h('div', { class: 'grid2' }, field(b, 'id', 'Código', { placeholder: 'XXXX-XXXX-XXXX' }), field(b, 'note', 'Motivo (aparece na tela)')),
    })));
}

function pagePlans(root) {
  root.append(card('Planos', 'Mostrados na tela de bloqueio do launcher e usados para calcular a validade dos aparelhos.',
    listEditor(cfg.plans, {
      title: (p, i) => (p.name || 'Plano ' + (i + 1)) + (p.price ? ' — ' + p.price : ''),
      create: () => ({ name: '', price: 'R$ ', days: 30, features: '' }),
      addLabel: 'Novo plano',
      render: (p) => h('div', {},
        h('div', { class: 'grid3' }, field(p, 'name', 'Nome'), field(p, 'price', 'Preço'), field(p, 'days', 'Dias', { type: 'number', min: 1 })),
        field(p, 'features', 'Descrição curta')),
    })));
}

function pageSettings(root) {
  const s = cfg.settings;
  root.append(card('Launcher', null,
    field(s, 'support', 'Contato de suporte (aparece no bloqueio e em Configurações)'),
    h('div', { class: 'grid2' },
      field(s, 'refreshMinutes', 'Sincronizar com o painel a cada (minutos)', { type: 'number', min: 1 }),
      field(s, 'settingsPin', 'PIN das configurações do launcher (vazio = sem PIN)')),
    checkbox(s, 'showClock', 'Mostrar relógio'),
    checkbox(s, 'showWifi', 'Mostrar ícone de rede'),
    field(s, 'launcherUpdateUrl', 'Endereço do latest.json para atualizar o launcher')));

  const ta = h('textarea', { class: 'code', spellcheck: 'false' });
  ta.value = JSON.stringify(cfg, null, 2);
  root.append(card('Configuração avançada (JSON)', 'Edite tudo de uma vez, ou copie para backup.',
    ta,
    h('div', { class: 'inline', style: 'margin-top:12px' },
      h('button', { class: 'btn', type: 'button', onclick: () => {
        try { cfg = normalize(JSON.parse(ta.value)); touch(); toast('JSON aplicado. Clique em Publicar.'); } catch (e) { toast('JSON inválido: ' + e.message, true); }
      } }, 'Aplicar JSON'),
      h('button', { class: 'btn', type: 'button', onclick: () => {
        const a = h('a', { href: URL.createObjectURL(new Blob([JSON.stringify(cfg, null, 2)], { type: 'application/json' })), download: 'config.json' });
        a.click();
      } }, 'Baixar backup'))));
}

function pageThemes(root) {
  const t = cfg.theme;
  const box = h('div');
  const draw = () => {
    box.innerHTML = '';
    box.append(listEditor(t.themes, {
      title: (th) => (th.name || th.id || 'Tema') + (t.active === th.id ? '  (ativo)' : ''),
      create: () => ({ id: 'tema' + Date.now().toString(36), name: 'Novo tema', accent: '#3B82F6', card: '#CC111827', text: '#FFFFFF', muted: '#9CA3AF', focus: '#FFFFFF' }),
      addLabel: 'Novo tema',
      render: (th, i, redraw) => {
        const prev = h('div', { class: 'theme-prev' });
        const paint = () => {
          prev.innerHTML = '';
          prev.style.background = cfg.branding.backgroundColor || '#05080F';
          const col = (v) => { v = (v || '').trim(); return /^#[0-9a-f]{8}$/i.test(v) ? '#' + v.slice(3) + v.slice(1, 3) : v; };
          prev.append(
            h('div', { class: 'tile', style: 'background:' + col(th.accent) + ';outline:3px solid ' + col(th.focus) }),
            h('div', { class: 'tile', style: 'background:' + col(th.card) }),
            h('div', {}, h('b', { style: 'color:' + col(th.text) }, 'Texto'), h('br'), h('span', { style: 'color:' + col(th.muted) }, 'secundário')));
        };
        paint();
        const wrap = h('div', { oninput: paint },
          h('div', { class: 'grid2' }, field(th, 'name', 'Nome'), field(th, 'id', 'Identificador')),
          h('div', { class: 'grid3' }, colorField(th, 'accent', 'Destaque'), colorField(th, 'card', 'Cartões (#AARRGGBB)'), colorField(th, 'focus', 'Borda de foco')),
          h('div', { class: 'grid2' }, colorField(th, 'text', 'Texto'), colorField(th, 'muted', 'Texto secundário')),
          prev,
          h('div', { style: 'margin-top:12px' },
            t.active === th.id ? h('span', { class: 'tag ok' }, 'Tema ativo')
              : h('button', { class: 'btn primary small', type: 'button', onclick: () => { t.active = th.id; touch(); draw(); } }, 'Usar este tema')));
        return wrap;
      },
    }));
  };
  draw();
  root.append(card('Temas', 'Cores do launcher na TV. Escolha um tema ativo e publique.', box));
}

// ---------------------------------------------------------------------------------------------
// Shell
// ---------------------------------------------------------------------------------------------

function navButton(p) {
  return h('button', { class: 'nav' + (p.id === current ? ' on' : ''), 'data-id': p.id, onclick: () => go(p.id) }, svg(p.id), p.title);
}

function go(id) {
  current = id;
  try { localStorage.setItem('xl.page', id); } catch (e) { /* ignore */ }
  const page = PAGES.find((p) => p.id === id) || PAGES[0];
  document.querySelectorAll('.nav[data-id]').forEach((b) => b.classList.toggle('on', b.dataset.id === page.id));
  $('#page-title').textContent = page.title;
  const root = $('#content');
  root.innerHTML = '';
  page.render(root);
  $('#side').classList.remove('open');
  $('#scrim').classList.remove('open');
  window.scrollTo(0, 0);
}

async function start() {
  $('#login').classList.add('hidden');
  $('#app').classList.remove('hidden');
  $('#content').innerHTML = '<p class="hint">Carregando configuração…</p>';
  await loadConfig();
  $('#side-name').textContent = cfg.branding.name || 'Xplay';
  $('#nav-main').replaceChildren(...PAGES.filter((p) => p.group === 'main').map(navButton));
  $('#nav-launcher').replaceChildren(...PAGES.filter((p) => p.group === 'launcher').map(navButton));
  let page = 'dashboard';
  try { page = localStorage.getItem('xl.page') || page; } catch (e) { /* ignore */ }
  go(page);
}

function showLogin(err) {
  $('#app').classList.add('hidden');
  $('#login').classList.remove('hidden');
  $('#l-repo').value = (session && session.repo) || DEFAULTS.repo;
  $('#l-branch').value = (session && session.branch) || DEFAULTS.branch;
  $('#login-err').textContent = err || '';
}

$('#login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  session = { repo: $('#l-repo').value.trim(), branch: $('#l-branch').value.trim(), token: $('#l-token').value.trim() };
  try {
    await start();
    try { localStorage.setItem('xl.session', JSON.stringify(session)); } catch (err) { /* stays for this tab */ }
  } catch (err) {
    showLogin(err.status === 404 ? 'Não encontrei ' + CONFIG_PATH + ' nesse repositório/branch (ou o token não tem acesso).' : 'Erro: ' + err.message);
  }
});

$('#save').addEventListener('click', async () => {
  const btn = $('#save');
  btn.disabled = true;
  btn.textContent = 'Publicando…';
  try {
    await saveConfig('Painel: ' + ($('#page-title').textContent || 'atualização'));
    dirty = false;
    $('#dirty').classList.add('hidden');
    toast('Publicado! As TVs atualizam ao abrir o launcher ou no botão ⟳.');
  } catch (e) {
    toast('Falha ao publicar: ' + e.message, true);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Publicar';
  }
});

$('#logout').addEventListener('click', () => {
  if (dirty && !confirm('Há alterações não publicadas. Sair mesmo assim?')) return;
  try { localStorage.removeItem('xl.session'); } catch (e) { /* ignore */ }
  session = { repo: session.repo, branch: session.branch, token: '' };
  dirty = false;
  $('#l-token').value = '';
  showLogin();
});

$('#menu-btn').addEventListener('click', () => { $('#side').classList.add('open'); $('#scrim').classList.add('open'); });
$('#scrim').addEventListener('click', () => { $('#side').classList.remove('open'); $('#scrim').classList.remove('open'); });
window.addEventListener('beforeunload', (e) => { if (dirty) { e.preventDefault(); e.returnValue = ''; } });

(async function boot() {
  try { session = JSON.parse(localStorage.getItem('xl.session') || 'null'); } catch (e) { session = null; }
  if (!session || !session.token) return showLogin();
  try {
    await start();
  } catch (e) {
    showLogin(e.status === 401 ? 'Token inválido ou expirado. Entre de novo.' : 'Erro: ' + e.message);
  }
})();
