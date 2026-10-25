'use strict';

/* ---------- Icons (stroke icons, 24x24) ---------- */
const ICONS = {
  home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"/>',
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  trending: '<polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/>',
  wrench: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>',
  bell: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',
  history: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/>',
  dollar: '<line x1="12" y1="2" x2="12" y2="22"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
  alert: '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
  calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
  calcheck: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="m9 16 2 2 4-4"/>',
  caldays: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01"/>',
  star: '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
  arrowR: '<line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>',
  search: '<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>',
  filter: '<polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>',
  down: '<polyline points="6 9 12 15 18 9"/>',
  left: '<polyline points="15 18 9 12 15 6"/>',
  right: '<polyline points="9 18 15 12 9 6"/>',
  edit: '<path d="M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.4 2.6a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/>',
  renew: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/>',
  trash: '<polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>',
  wa: '<path d="M3 21l1.65-4.8A9 9 0 1 1 7.8 19.4z"/><path d="M9.2 8.2c-.4 2.8 3.6 6.9 6.6 6.6l.9-1.5-2-1.1-.9.8c-1-.4-2.4-1.8-2.8-2.8l.8-.9-1.1-2z"/>',
  chat: '<path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/>',
  x: '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',
  plus: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
  check: '<polyline points="20 6 9 17 4 12"/>',
  checkc: '<circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>',
  download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
  upload: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>',
  wallet: '<path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/>',
  piggy: '<path d="M19 5c-1.5 0-2.8 1.4-3 2-3.5-1.5-11-.3-11 5 0 1.8 0 3 2 4.5V20h4v-2h3v2h4v-4c1-.5 1.7-1 2-2h2v-4h-2c0-1-.5-1.5-1-2V5z"/><path d="M2 9v1c0 1.1.9 2 2 2h1"/><path d="M16 11h.01"/>',
  box: '<path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="m3.3 7 8.7 5 8.7-5"/><path d="M12 22V12"/><path d="m7.5 4.27 9 5.15"/>',
  user: '<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  userplus: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/>',
  usercheck: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="16 11 18 13 22 9"/>',
  refresh: '<path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/>',
  pie: '<path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>',
  pin: '<path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>',
  archive: '<rect x="2" y="3" width="20" height="5" rx="1"/><path d="M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8"/><path d="M10 12h4"/>',
  phone: '<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z"/>',
  tag: '<path d="M12.6 2.6A2 2 0 0 0 11.2 2H4a2 2 0 0 0-2 2v7.2a2 2 0 0 0 .6 1.4l8.7 8.7a2.43 2.43 0 0 0 3.4 0l6.6-6.6a2.43 2.43 0 0 0 0-3.4z"/><circle cx="7.5" cy="7.5" r=".5"/>',
  percent: '<line x1="19" y1="5" x2="5" y2="19"/><circle cx="6.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="17.5" r="2.5"/>',
  file: '<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M16 13H8"/><path d="M16 17H8"/><path d="M10 9H8"/>',
  undo: '<path d="M9 14 4 9l5-5"/><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11"/>',
  layers: '<path d="m12 2 10 5-10 5L2 7Z"/><path d="m2 17 10 5 10-5"/><path d="m2 12 10 5 10-5"/>',
  db: '<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/><path d="M3 12c0 1.66 4 3 9 3s9-1.34 9-3"/>',
  msg: '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
  sparkle: '<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>',
  send: '<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
};
const ic = (n, extra) => `<i data-i="${n}"${extra ? ' ' + extra : ''}></i>`;
function paintIcons(root) {
  (root || document).querySelectorAll('i[data-i]:not([data-p])').forEach(el => {
    const n = el.dataset.i;
    const fill = n === 'star' && el.classList.contains('fill');
    el.innerHTML = `<svg viewBox="0 0 24 24" fill="${fill ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${ICONS[n] || ''}</svg>`;
    el.dataset.p = 1;
  });
}

/* ---------- Helpers ---------- */
const $ = s => document.querySelector(s);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
const pad = n => String(n).padStart(2, '0');
const iso = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const parse = s => { const [y, m, d] = s.slice(0, 10).split('-').map(Number); return new Date(y, m - 1, d); };
const today = () => iso(new Date());
const addDays = (s, n) => { const d = parse(s); d.setDate(d.getDate() + n); return iso(d); };
function addMonths(s, n) {
  const d = parse(s), day = d.getDate();
  d.setDate(1); d.setMonth(d.getMonth() + n);
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(day, last));
  return iso(d);
}
const diffDays = (a, b) => Math.round((parse(a) - parse(b)) / 864e5);
const br = s => s ? s.slice(0, 10).split('-').reverse().join('/') : '—';
const brShort = s => br(s).slice(0, 5);
const money = v => 'R$ ' + (Number(v) || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const MONTHS = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
const monKey = s => s.slice(0, 7);
const monLabel = (key, short) => {
  const [y, m] = key.split('-').map(Number);
  return short ? `${MONTHS[m - 1].slice(0, 3)}/${String(y).slice(2)}` : `${MONTHS[m - 1]} de ${y}`;
};
const cap = s => s.replace(/(^|\s)\S/g, c => c.toUpperCase());
const shiftMonth = (key, n) => addMonths(key + '-01', n).slice(0, 7);
const nowStamp = () => { const d = new Date(); return `${iso(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`; };
function fmtPhone(p) {
  const d = String(p || '').replace(/\D/g, '');
  if (d.length >= 12 && d.startsWith('55')) {
    const n = d.slice(4);
    return `+55 ${d.slice(2, 4)} ${n.slice(0, n.length - 4)}-${n.slice(-4)}`;
  }
  if (d.length === 10 || d.length === 11) return `+55 ${d.slice(0, 2)} ${d.slice(2, d.length - 4)}-${d.slice(-4)}`;
  return p || '';
}
function normPhone(p) {
  let d = String(p || '').replace(/\D/g, '');
  if (d.length === 10 || d.length === 11) d = '55' + d;
  return d;
}
function toast(msg) {
  const t = $('#toast'); t.textContent = msg; t.classList.add('on');
  clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove('on'), 2200);
}
function openUrl(url) {
  if (window.Android) Android.open(url); else window.open(url, '_blank');
}
const kFmt = v => Math.abs(v) >= 1000 ? (Math.round(v / 100) / 10).toLocaleString('pt-BR') + 'k' : String(Math.round(v));

/* ---------- Data ---------- */
const TYPES = { mensal: 1, trimestral: 3, semestral: 6, anual: 12 };
const TYPE_NAMES = { mensal: 'Mensal', trimestral: 'Trimestral', semestral: 'Semestral', anual: 'Anual' };
const KEY = 'xplay-gestor-v1';
let db;
function blank() {
  return {
    plans: [
      { id: 'p1', name: 'XPRIMETV MENSAL', type: 'mensal', price: 34.89, cost: 7.25 },
      { id: 'p2', name: 'XPRIMETV TRIMESTRAL', type: 'trimestral', price: 94.9, cost: 21.75 },
      { id: 'p3', name: 'XPRIMETV ANUAL', type: 'anual', price: 349.9, cost: 87 },
    ],
    clients: [], payments: [], history: [], snapshots: {},
    settings: { brand: 'xplay', template: 'Olá {nome}! Seu plano {plano} vence em {vencimento}. Valor: {valor}. Para renovar é só responder esta mensagem. 😉' },
  };
}
function load() {
  try { db = JSON.parse(localStorage.getItem(KEY)); } catch (e) { db = null; }
  if (!db || !db.clients) db = blank();
  db.snapshots = db.snapshots || {};
  db.settings = Object.assign(blank().settings, db.settings || {});
}
function save() {
  try { localStorage.setItem(KEY, JSON.stringify(db)); } catch (e) { toast('Não foi possível salvar: ' + e.message); }
}
const planOf = c => db.plans.find(p => p.id === c.planId);
const priceOf = c => { const p = planOf(c); return p ? Math.max(0, p.price * (c.qty || 1) - (Number(c.discount) || 0)) : 0; };
const costOf = c => { const p = planOf(c); return p ? p.cost * (c.qty || 1) : 0; };
const daysLeft = c => c.due ? diffDays(c.due, today()) : null;
const live = () => db.clients.filter(c => !c.archived);
const isActive = c => c.due && daysLeft(c) >= 0;

function snapshot() {
  db.snapshots[today()] = live().filter(isActive).length;
  const keys = Object.keys(db.snapshots).sort();
  while (keys.length > 120) delete db.snapshots[keys.shift()];
}

function log(title, desc, undo) {
  db.history.unshift({ id: uid(), title, desc, at: nowStamp(), undo: undo || null, undone: false });
  if (db.history.length > 300) db.history.length = 300;
}

function addPayment(c, kind, amount) {
  const p = planOf(c);
  const pay = { id: uid(), clientId: c.id, clientName: c.name, planName: p ? p.name : '', amount, cost: costOf(c), date: nowStamp(), kind };
  db.payments.push(pay);
  return pay;
}

/* ---------- Router & header ---------- */
const PAGES = {
  dashboard: ['Dashboard', 'Visão geral do seu negócio'],
  clientes: ['Clientes', 'Gerencie seus clientes'],
  financeiro: ['Financeiro', 'Relatórios financeiros'],
  ferramentas: ['Ferramentas', 'Planos, backup e ajustes'],
};
const state = {
  tab: 'dashboard',
  q: '', fStatus: 'todos', fType: 'todos', fPlan: 'todos', show: 20,
  period: 'hoje', from: '', to: '',
  lucroMonth: monKey(today()), lucroMode: 'mes',
  dayMonth: monKey(today()), renMonth: monKey(today()), newMonth: monKey(today()), avgMonth: monKey(today()),
  lateType: 'todos',
};
function go(tab, keepScroll) {
  state.tab = tab;
  document.querySelectorAll('.tabs button').forEach(b => b.classList.toggle('on', b.dataset.tab === tab));
  $('#pageTitle').textContent = PAGES[tab][0];
  $('#pageSub').textContent = PAGES[tab][1];
  $('#fab').hidden = !(tab === 'dashboard' || tab === 'clientes');
  render();
  if (!keepScroll) window.scrollTo(0, 0);
}
function render() {
  const brand = db.settings.brand || 'xplay';
  $('#avatar').textContent = brand;
  const due = live().filter(c => c.due && daysLeft(c) === 0).length;
  const dot = $('#bellDot'); dot.hidden = !due; dot.textContent = due > 99 ? '99+' : due;
  $('#page').innerHTML = VIEWS[state.tab]();
  paintIcons($('#page'));
  if (VIEWS[state.tab + 'After']) VIEWS[state.tab + 'After']();
}
const refresh = () => { save(); render(); };

/* ---------- Status helpers ---------- */
function statusOf(c) {
  const d = daysLeft(c);
  if (d === null) return { cls: 'n', text: 'Sem vencimento', color: '#9aa0ae' };
  if (d < 0) return { cls: 'r', text: `${-d} ${-d === 1 ? 'dia vencido' : 'dias vencidos'}`, color: '#e05252', short: `Vencido há ${-d}d` };
  if (d === 0) return { cls: 'o', text: 'Vence hoje', color: '#ef7d3b', short: 'Vence hoje' };
  if (d <= 5) return { cls: 'y', text: `${d} ${d === 1 ? 'dia' : 'dias'}`, color: '#e0a526', short: `Vence em ${d}d` };
  return { cls: 'g', text: `${d} dias`, color: '#3fae74', short: `Em dia · ${d}d` };
}
function pillOf(c) {
  const d = daysLeft(c);
  if (d === null) return '<span class="pill" style="background:#9aa0ae">—</span>';
  if (d < 0) return `<span class="pill r">🔴 ${-d}d</span>`;
  if (d === 0) return '<span class="pill r">🔴 Hoje</span>';
  if (d <= 5) return `<span class="pill y">🟡 ${d} ${d === 1 ? 'dia' : 'dias'}</span>`;
  return `<span class="pill g">✅ ${d} dias</span>`;
}
function matchStatus(c, f) {
  const d = daysLeft(c);
  switch (f) {
    case 'todos': return !c.archived;
    case 'arquivados': return c.archived;
  }
  if (c.archived) return false;
  switch (f) {
    case 'ativos': return d !== null && d >= 0;
    case 'vencidos': return d !== null && d < 0;
    case 'hoje': return d === 0;
    case 'a3': return d !== null && d >= 1 && d <= 3;
    case 'p5': return d !== null && d >= 1 && d <= 5;
    case 'emdia': return d !== null && d > 5;
    case 'semvenc': return d === null;
    case 'semplano': return !planOf(c);
    case 'essenciais': return c.essential;
  }
  return true;
}
const STATUS_OPTS = [['todos', 'Todos'], ['ativos', 'Ativos'], ['vencidos', 'Vencidos'], ['hoje', 'Hoje'], ['a3', 'A Vencer (3d)'], ['p5', 'Próximos 5d'],
  ['emdia', 'Em Dia'], ['semvenc', 'Sem Venc.'], ['semplano', 'Sem Plano'], ['essenciais', '⭐ Essenciais'], ['arquivados', '📦 Arquivados']];
const TYPE_OPTS = [['todos', 'Todos'], ['mensal', 'Mensal'], ['trimestral', 'Trimestral'], ['semestral', 'Semestral'], ['anual', 'Anual']];
const gradFor = name => {
  const g = [['#6366f1', '#9333ea'], ['#f59e0b', '#ea7c2c'], ['#ec4899', '#a855f7'], ['#10b981', '#0ea5e9'], ['#3b82f6', '#6366f1']];
  let h = 0; for (const ch of name || '') h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return g[h % g.length];
};
const initial = n => esc((n || '?').trim().charAt(0).toUpperCase());
function waLink(c, text) {
  const d = normPhone(c.phone);
  return `https://wa.me/${d}${text ? '?text=' + encodeURIComponent(text) : ''}`;
}
function waMessage(c) {
  const p = planOf(c);
  return db.settings.template
    .replace(/{nome}/g, c.name).replace(/{plano}/g, p ? p.name : '')
    .replace(/{vencimento}/g, br(c.due)).replace(/{valor}/g, money(priceOf(c)));
}

/* ---------- Views ---------- */
const VIEWS = {};

VIEWS.dashboard = () => {
  const L = live();
  const active = L.filter(isActive).length;
  const late = L.filter(c => c.due && daysLeft(c) < 0).length;
  const todayN = L.filter(c => c.due && daysLeft(c) === 0).length;
  const next5 = L.filter(c => { const d = daysLeft(c); return d !== null && d >= 1 && d <= 5; }).length;
  const stat = (f, color, icon, title, n) => `
    <button class="card stat" data-act="filter" data-f="${f}" style="width:100%;text-align:left">
      <div class="st"><div class="ic" style="background:${color}">${ic(icon)}</div>
        <div class="num"><b>${n}</b><span>clientes</span></div></div>
      <b>${title}</b><span>Toque para ver detalhes</span>
    </button>`;
  const ess = L.filter(c => c.essential).sort((a, b) => (a.due || '9') < (b.due || '9') ? -1 : 1);
  const upcoming = L.filter(c => { const d = daysLeft(c); return d !== null && d >= 0 && d <= 5; })
    .sort((a, b) => a.due < b.due ? -1 : a.due > b.due ? 1 : a.name.localeCompare(b.name));
  const row = (c, date, grad) => {
    const d = daysLeft(c);
    return `<div class="row ${d !== null && d <= 0 ? 'late' : d !== null && d <= 5 ? 'warn' : ''}" data-act="detail" data-id="${c.id}">
      <div class="ini" style="background:linear-gradient(135deg,${grad[0]},${grad[1]})">${initial(c.name)}</div>
      <div class="nm"><b>${esc(c.name)}</b><span>${date}</span></div>
      ${pillOf(c)}
      <button class="wa" data-act="wa" data-id="${c.id}">${ic('wa')}</button></div>`;
  };
  return `
    ${stat('ativos', 'linear-gradient(135deg,#3b82f6,#4f46e5)', 'users', 'Clientes Ativos', active)}
    ${stat('vencidos', 'linear-gradient(135deg,#e0457b,#c0303a)', 'alert', 'Vencidos', late)}
    ${stat('hoje', 'linear-gradient(135deg,#ea7c2c,#b8742a)', 'calcheck', 'Vencem Hoje', todayN)}
    ${stat('p5', 'linear-gradient(135deg,#a855f7,#7c3aed)', 'dollar', 'Próximos 5 Dias', next5)}
    <div class="card list-card ess">
      <div class="lc-head"><div class="ico" style="background:linear-gradient(135deg,#f6b73c,#ef8a2c)">${ic('star', 'class="fill"')}</div>
        <div><b>Essenciais</b><small>${ess.length} ${ess.length === 1 ? 'cliente' : 'clientes'}</small></div>
        <button class="see" style="color:#c96f19" data-act="filter" data-f="essenciais">Ver Todos ${ic('arrowR')}</button></div>
      ${ess.slice(0, 10).map(c => row(c, br(c.due), ['#f6b73c', '#ef8a2c'])).join('') || '<div class="empty" style="padding:20px">Marque clientes com a ⭐ para vê-los aqui</div>'}
    </div>
    <div class="card list-card" style="background:linear-gradient(180deg,#f5f5ff,#fff 70px)">
      <div class="lc-head"><div class="ico" style="background:linear-gradient(135deg,#6366f1,#9333ea)">${ic('calendar')}</div>
        <div><b>Próx. Vencimentos</b><small>${upcoming.length} em 5 dias</small></div>
        <button class="see" style="color:#4f46e5" data-act="filter" data-f="p5h">Ver Todos ${ic('arrowR')}</button></div>
      ${upcoming.slice(0, 15).map(c => row(c, brShort(c.due), ['#6366f1', '#9333ea'])).join('') || '<div class="empty" style="padding:20px">Nenhum vencimento nos próximos 5 dias</div>'}
    </div>`;
};

function filteredClients() {
  const q = state.q.trim().toLowerCase(), qd = q.replace(/\D/g, '');
  return db.clients.filter(c => {
    if (state.fStatus === 'p5h') { const d = daysLeft(c); if (c.archived || d === null || d < 0 || d > 5) return false; }
    else if (!matchStatus(c, state.fStatus)) return false;
    const p = planOf(c);
    if (state.fType !== 'todos' && (!p || p.type !== state.fType)) return false;
    if (state.fPlan !== 'todos' && c.planId !== state.fPlan) return false;
    if (q && !(c.name.toLowerCase().includes(q) || (qd && String(c.phone).includes(qd)) || (c.notes || '').toLowerCase().includes(q))) return false;
    return true;
  }).sort((a, b) => {
    if (!a.due) return 1; if (!b.due) return -1;
    return a.due < b.due ? -1 : a.due > b.due ? 1 : a.name.localeCompare(b.name);
  });
}
const optLabel = (opts, v) => (opts.find(o => o[0] === v) || opts[0])[1];
const planOpts = () => [['todos', 'Todos']].concat(db.plans.map(p => [p.id, p.name]));

VIEWS.clientes = () => {
  const list = filteredClients();
  const shown = state.show === 0 ? list : list.slice(0, state.show);
  const statusOpts = STATUS_OPTS.concat(state.fStatus === 'p5h' ? [['p5h', 'Próx. 5d']] : []);
  return `
    <div class="card">
      <label class="search">${ic('search')}<input id="q" type="search" placeholder="Buscar cliente..." value="${esc(state.q)}" autocomplete="off"></label>
      <div class="filters">
        <button class="dd" data-act="dd" data-k="fStatus">${ic('filter', 'style="color:#3b82f6"')}<span>${optLabel(statusOpts, state.fStatus)}</span>${ic('down')}</button>
        <button class="dd" data-act="dd" data-k="fType">${ic('filter', 'style="color:#a855f7"')}<span>${optLabel(TYPE_OPTS, state.fType)}</span>${ic('down')}</button>
        <button class="dd" data-act="dd" data-k="fPlan">${ic('filter', 'style="color:#22a35a"')}<span>${esc(optLabel(planOpts(), state.fPlan))}</span>${ic('down')}</button>
      </div>
    </div>
    <div class="count-row">
      <span class="chip">${ic('users')} ${list.length} ${list.length === 1 ? 'cliente' : 'clientes'}</span>
      <label>Mostrar:</label>
      <button class="dd" data-act="dd" data-k="show"><span>${state.show || 'Todos'}</span>${ic('down')}</button>
    </div>
    <div id="clist">${shown.map(clientCard).join('') || `<div class="card empty">${ic('users')}<div>Nenhum cliente encontrado</div></div>`}</div>
    ${shown.length < list.length ? `<button class="card more" data-act="more">Mostrar mais (${list.length - shown.length} restantes)</button>` : ''}`;
};
function clientCard(c) {
  const p = planOf(c), st = statusOf(c);
  const hasPays = db.payments.some(x => x.clientId === c.id);
  return `<div class="card client" data-act="detail" data-id="${c.id}">
    <div class="head"><div class="ini">${initial(c.name)}</div>
      <div class="who"><b>${hasPays ? `<button data-act="pays" data-id="${c.id}">${ic('history')}</button>` : ''}<span class="n">${esc(c.name)}</span></b>
        <span>${p ? esc(p.name) : 'Sem plano'}${c.qty > 1 ? ` · ${c.qty} telas` : ''}</span></div>
      <span class="badge ${st.cls}">${c.archived ? '📦 ' : ''}${st.text}</span></div>
    <div class="due">${ic('calendar')} Vencimento <b>${br(c.due)}</b></div>
    <div class="actions">
      <button class="btn-wa" data-act="wa" data-id="${c.id}">${ic('wa')} WhatsApp</button>
      <button class="sq star ${c.essential ? 'on' : ''}" data-act="star" data-id="${c.id}">${ic('star', c.essential ? 'class="fill"' : '')}</button>
      <button class="sq" data-act="edit" data-id="${c.id}">${ic('edit')}</button>
      <button class="sq renew" data-act="renew" data-id="${c.id}">${ic('renew')}</button>
      <button class="sq del" data-act="del" data-id="${c.id}">${ic('trash')}</button>
    </div></div>`;
}
VIEWS.clientesAfter = () => {
  const q = $('#q');
  q.addEventListener('input', () => {
    state.q = q.value;
    clearTimeout(q.t);
    q.t = setTimeout(() => {
      const pos = q.selectionStart; render();
      const n = $('#q'); n.focus(); try { n.setSelectionRange(pos, pos); } catch (e) { }
    }, 220);
  });
};

/* ----- Finance ----- */
const PERIODS = [['hoje', 'Hoje'], ['ontem', 'Ontem'], ['semana', 'Semana'], ['mes', 'Mês'], ['tudo', 'Todo período'], ['custom', 'Selecionar Período']];
function periodRange() {
  const t = today();
  switch (state.period) {
    case 'hoje': return [t, t];
    case 'ontem': { const y = addDays(t, -1); return [y, y]; }
    case 'semana': return [addDays(t, -6), t];
    case 'mes': return [t.slice(0, 8) + '01', t];
    case 'tudo': return ['0000-01-01', '9999-12-31'];
    case 'custom': return [state.from || '0000-01-01', state.to || '9999-12-31'];
  }
}
const inRange = (p, r) => p.date.slice(0, 10) >= r[0] && p.date.slice(0, 10) <= r[1];
function periodShort() {
  if (state.period !== 'custom') return optLabel(PERIODS, state.period);
  return state.from || state.to ? `${state.from ? brShort(state.from) : '…'}–${state.to ? brShort(state.to) : '…'}` : 'Período';
}
function sumBy(pays) {
  let rev = 0, prof = 0; for (const p of pays) { rev += p.amount; prof += p.amount - (p.cost || 0); }
  return { rev, prof };
}

VIEWS.financeiro = () => {
  const r = periodRange();
  const pays = db.payments.filter(p => inRange(p, r));
  const { rev, prof } = sumBy(pays);
  const clients = new Set(pays.map(p => p.clientId)).size;
  const label = periodShort();
  const kpi = (cls, icon, l, v) => `<div class="kpi ${cls}"><div class="t"><div class="ic">${ic(icon)}</div><small>${esc(label)}</small></div><div class="l">${l}</div><div class="v">${v}</div></div>`;
  return `
    <div class="card">
      <button class="dd full" data-act="dd" data-k="period"><span>${optLabel(PERIODS, state.period)}</span>${ic('down')}</button>
      ${state.period === 'custom' ? `<div class="date-range"><input type="date" id="pFrom" value="${state.from}"><input type="date" id="pTo" value="${state.to}"></div>` : ''}
      <div style="height:14px"></div>
      <button class="btn" data-act="pdf">${ic('download')} PDF</button>
    </div>
    <div class="kpis">
      ${kpi('k1', 'dollar', 'Receita total', money(rev))}
      ${kpi('k2', 'trending', 'Lucro líquido', money(prof))}
      ${kpi('k3', 'wallet', 'Clientes ativos', clients)}
      ${kpi('k4', 'piggy', 'Ticket médio', money(pays.length ? rev / pays.length : 0))}
    </div>
    ${lucroCard()}
    ${evolucaoCard()}
    ${receitaPlanoCard()}
    ${historicoAtivosCard()}
    ${porDiaCard()}
    ${countCard('renMonth', 'refresh', '#e8f7f1', '#2f7d6a', 'Planos Renovados', 'planos renovados', p => p.kind === 'renovacao')}
    ${countCard('newMonth', 'userplus', '#ebf2ff', '#2f5fd0', 'Clientes Novos', 'clientes adicionados', p => p.kind === 'cadastro')}
    ${mediaCard()}
    ${naoRenovaramCard()}
    ${estadosCard()}`;
};
VIEWS.financeiroAfter = () => {
  const f = $('#pFrom'), t = $('#pTo');
  if (f) f.onchange = () => { state.from = f.value; render(); };
  if (t) t.onchange = () => { state.to = t.value; render(); };
};

function monthOpts() {
  const keys = new Set([monKey(today())]);
  db.payments.forEach(p => keys.add(monKey(p.date)));
  return [...keys].sort().reverse().map(k => [k, `${MONTHS[+k.slice(5) - 1].slice(0, 3)} de ${k.slice(0, 4)}`]);
}

function lucroCard() {
  const mode = state.lucroMode, key = state.lucroMonth;
  let labels = [], vals = [];
  if (mode === 'mes') {
    const [y, m] = key.split('-').map(Number), n = new Date(y, m, 0).getDate();
    const byDay = Array(n).fill(0);
    db.payments.forEach(p => { if (monKey(p.date) === key) byDay[+p.date.slice(8, 10) - 1] += p.amount - (p.cost || 0); });
    vals = byDay; labels = byDay.map((_, i) => i + 1);
  } else if (mode === 'sem') {
    const end = key === monKey(today()) ? today() : iso(new Date(+key.slice(0, 4), +key.slice(5), 0));
    for (let i = 6; i >= 0; i--) {
      const d = addDays(end, -i);
      labels.push(brShort(d));
      vals.push(sumBy(db.payments.filter(p => p.date.slice(0, 10) === d)).prof);
    }
  } else {
    const y = key.slice(0, 4);
    for (let m = 1; m <= 12; m++) {
      const k = `${y}-${pad(m)}`;
      labels.push(MONTHS[m - 1].slice(0, 3));
      vals.push(sumBy(db.payments.filter(p => monKey(p.date) === k)).prof);
    }
  }
  const step = mode === 'mes' ? 5 : 1;
  const xl = labels.map((l, i) => (mode !== 'mes' || i % step === 0 || i === labels.length - 1) ? l : '');
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#f5f0ff;color:#7c3aed">${ic('dollar')}</div><h3>Análise de Lucro</h3></div>
    <div class="chart-ctl">
      <button class="dd" data-act="dd" data-k="lucroMonth"><span>${optLabel(monthOpts(), key)}</span>${ic('down')}</button>
      <div class="seg">${[['sem', 'Sem.'], ['mes', 'Mês'], ['ano', 'Ano']].map(([k, l]) => `<button class="${mode === k ? 'on' : ''}" data-act="set" data-k="lucroMode" data-v="${k}">${l}</button>`).join('')}</div>
    </div>
    ${lineChart([{ vals, color: '#7c3aed', fill: 'rgba(124,58,237,.10)' }], xl, { dots: true, fmt: kFmt })}
  </div>`;
}

function evolucaoCard() {
  const keys = []; for (let i = 5; i >= 0; i--) keys.push(shiftMonth(monKey(today()), -i));
  const rev = [], prof = [];
  keys.forEach(k => { const s = sumBy(db.payments.filter(p => monKey(p.date) === k)); rev.push(s.rev); prof.push(s.prof); });
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#eef4ff;color:#2f6ae0">${ic('trending')}</div><h3>Evolução Financeira</h3></div>
    ${lineChart([{ vals: rev, color: '#4f86e8', fill: 'rgba(79,134,232,.16)' }, { vals: prof, color: '#43a35a', fill: 'rgba(67,163,90,.16)' }],
      keys.map(k => monLabel(k, true)), { fmt: v => 'R$' + kFmt(v), left: 54 })}
    <div class="legend"><span><i class="swc" style="background:#4f86e8"></i>Faturamento</span><span><i class="swc" style="background:#43a35a"></i>Lucro</span></div>
  </div>`;
}

function receitaPlanoCard() {
  const map = {};
  live().filter(isActive).forEach(c => { const p = planOf(c); if (!p) return; map[p.id] = map[p.id] || { name: p.name, n: 0, v: 0 }; map[p.id].n++; map[p.id].v += priceOf(c); });
  const rows = Object.values(map).sort((a, b) => b.v - a.v);
  const max = rows.length ? rows[0].v : 1;
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#f5f0ff;color:#7c3aed">${ic('box')}</div><div class="grow"><h3>Receita por Plano</h3><small>Clientes ativos por plano</small></div></div>
    ${rows.length ? `<div class="bars-h">${rows.map(r => `<div class="bh"><div><span>${esc(r.name)} <small style="color:#9aa0ae">· ${r.n}</small></span><b>${money(r.v)}</b></div>
      <div class="track"><div class="fill" style="width:${Math.max(3, r.v / max * 100)}%"></div></div></div>`).join('')}</div>`
      : `<div class="empty">${ic('box')}<div>Sem dados de faturamento</div></div>`}
  </div>`;
}

function historicoAtivosCard() {
  const cutoff = addDays(today(), -29);
  const keys = Object.keys(db.snapshots).filter(k => k >= cutoff).sort().slice(-10);
  let body;
  if (!keys.length) body = `<div class="empty">${ic('users')}<div>O histórico começa a ser registrado hoje</div></div>`;
  else {
    const vals = keys.map(k => db.snapshots[k]);
    const max = Math.max(...vals, 1), W = 340, H = 230, n = keys.length, bw = Math.min(30, (W - 10) / n - 6), gap = (W - n * bw) / (n + 1);
    let s = '';
    keys.forEach((k, i) => {
      const v = vals[i], h = Math.max(4, v / max * 140), x = gap + i * (bw + gap), y = 180 - h;
      const prev = i ? vals[i - 1] : (() => { const all = Object.keys(db.snapshots).sort(); const j = all.indexOf(k); return j > 0 ? db.snapshots[all[j - 1]] : v; })();
      s += `<rect x="${x}" y="${y}" width="${bw}" height="${h}" rx="5" fill="#ebe8fd" stroke="#a49be8" stroke-width=".8"/>`;
      s += `<text x="${x + bw / 2}" y="${y - 22}" text-anchor="middle" style="fill:#3b3f4c;font-weight:700;font-size:${n > 8 ? 10.5 : 12}px">${v}</text>`;
      if (v !== prev) s += `<path d="${v > prev ? `M${x + bw / 2 - 4} ${y - 8} l4 -6 l4 6z` : `M${x + bw / 2 - 4} ${y - 14} l4 6 l4 -6z`}" fill="${v > prev ? '#4cb782' : '#e05252'}"/>`;
      s += `<text x="${x + bw / 2}" y="200" text-anchor="end" transform="rotate(-35 ${x + bw / 2} 200)" style="font-size:10.5px">${brShort(k)}</text>`;
    });
    body = `<svg class="chart" viewBox="0 0 ${W} ${H}">${s}</svg>`;
  }
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#f5f0ff;color:#7c3aed">${ic('users')}</div><div class="grow"><h3>Histórico de Clientes Ativos</h3><small>Últimos 30 dias</small></div></div>
    ${body}</div>`;
}

function monthNav(k) {
  const cur = monKey(today());
  return `<div class="arrows"><button data-act="mon" data-k="${k}" data-v="-1">${ic('left')}</button><button data-act="mon" data-k="${k}" data-v="1" ${state[k] >= cur ? 'disabled' : ''}>${ic('right')}</button></div>`;
}
function porDiaCard() {
  const key = state.dayMonth, [y, m] = key.split('-').map(Number);
  const last = key === monKey(today()) ? +today().slice(8) : new Date(y, m, 0).getDate();
  const ren = Array(last).fill(0), cad = Array(last).fill(0);
  db.payments.forEach(p => { if (monKey(p.date) !== key) return; const d = +p.date.slice(8, 10) - 1; if (d >= last) return; (p.kind === 'cadastro' ? cad : ren)[d]++; });
  const R = ren.reduce((a, b) => a + b, 0), C = cad.reduce((a, b) => a + b, 0);
  let peak = -1, pv = 0; ren.forEach((v, i) => { if (v + cad[i] > pv) { pv = v + cad[i]; peak = i; } });
  const mini = (icon, bg, col, v, l, hl) => `<div class="mini ${hl ? 'hl' : ''}"><div class="ic" style="background:${bg};color:${col}">${ic(icon)}</div><div><b>${v}</b><span>${l}</span></div></div>`;
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#eef0ff;color:#4f46e5">${ic('caldays')}</div><div class="grow"><h3>Clientes por Dia</h3><small>${cap(monLabel(key))}</small></div>${monthNav('dayMonth')}</div>
    <div class="minis">
      ${mini('usercheck', '#eeeefe', '#5b5bd6', R, 'Renovados')}${mini('userplus', '#e8f7ec', '#3fae56', C, 'Cadastrados')}
      ${mini('users', '#fff4e0', '#e0a526', R + C, 'Total no mês')}${mini('calendar', '#fde9d9', '#e07a2c', peak >= 0 ? `${pad(peak + 1)}/${pad(m)}` : '—', 'Pico do mês', true)}
    </div>
    <div class="legend sq" style="margin:0 0 6px"><span><i class="swc" style="background:#6366f1"></i>Renovados</span><span><i class="swc" style="background:#5cc46e"></i>Cadastrados</span></div>
    ${stackChart(ren, cad, ren.map((_, i) => i % 5 === 0 ? `${pad(i + 1)}/${pad(m)}` : ''))}
  </div>`;
}
function countCard(k, icon, bg, col, title, sub, pred) {
  const n = db.payments.filter(p => monKey(p.date) === state[k] && pred(p)).length;
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:${bg};color:${col}">${ic(icon)}</div><div class="grow"><h3 style="display:flex;gap:8px;align-items:center">${title} ${ic('pie', `style="color:${col};width:20px;height:20px"`)}</h3><small>${cap(monLabel(state[k]))}</small></div>${monthNav(k)}</div>
    <div class="big" style="color:${col}">${n}</div><p>${sub}</p></div>`;
}
function mediaCard() {
  const key = state.avgMonth, [y, m] = key.split('-').map(Number);
  const days = key === monKey(today()) ? +today().slice(8) : new Date(y, m, 0).getDate();
  const n = db.payments.filter(p => monKey(p.date) === key).length;
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#fff6e0;color:#c2861d">${ic('users')}</div><div class="grow"><h3>Média de Clientes no Mês</h3><small>${cap(monLabel(key))}</small></div>${monthNav('avgMonth')}</div>
    <div class="big" style="color:#a85d1c">${Math.round(n / days)}</div><p style="display:flex;justify-content:space-between">média diária de clientes <small style="color:#9aa0ae">${days} dias</small></p></div>`;
}
function naoRenovaramCard() {
  const late = live().filter(c => c.due && daysLeft(c) < 0 && (state.lateType === 'todos' || c.planId === state.lateType));
  const by = { mensal: 0, trimestral: 0, semestral: 0, anual: 0 };
  late.forEach(c => { const p = planOf(c); if (p) by[p.type]++; });
  const colors = { mensal: ['#4f86e8', '#2f5fd0'], trimestral: ['#e9a23b', '#a85d1c'], semestral: ['#a855f7', '#7c3aed'], anual: ['#4cb782', '#2f8a4c'] };
  const opts = [['todos', 'Todos os planos']].concat(db.plans.map(p => [p.id, p.name]));
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#fdecec;color:#d24444">${ic('refresh')}</div><div class="grow"><h3>Clientes que Não Renovaram</h3><small>${late.length} clientes em atraso</small></div></div>
    <div style="display:flex;gap:10px;align-items:center;margin-bottom:14px">${ic('filter', 'style="color:#9aa0ae"')}<button class="dd" style="flex:1" data-act="dd" data-k="lateType"><span>${esc(optLabel(opts, state.lateType))}</span>${ic('down')}</button></div>
    <div class="types">${Object.keys(by).map(t => `<div class="type ${by[t] ? '' : 'zero'}"><div class="h"><i class="swc" style="background:${colors[t][0]}"></i>${TYPE_NAMES[t]}</div>
      <b style="${by[t] ? `color:${colors[t][1]}` : ''}">${by[t]}</b><small>${by[t] ? 'não renovaram' : 'em dia'}</small></div>`).join('')}</div>
  </div>`;
}

const DDD = {};
[[['11', '12', '13', '14', '15', '16', '17', '18', '19'], 'SP'], [['21', '22', '24'], 'RJ'], [['27', '28'], 'ES'], [['31', '32', '33', '34', '35', '37', '38'], 'MG'],
 [['41', '42', '43', '44', '45', '46'], 'PR'], [['47', '48', '49'], 'SC'], [['51', '53', '54', '55'], 'RS'], [['61'], 'DF'], [['62', '64'], 'GO'], [['63'], 'TO'],
 [['65', '66'], 'MT'], [['67'], 'MS'], [['68'], 'AC'], [['69'], 'RO'], [['71', '73', '74', '75', '77'], 'BA'], [['79'], 'SE'], [['81', '87'], 'PE'], [['82'], 'AL'],
 [['83'], 'PB'], [['84'], 'RN'], [['85', '88'], 'CE'], [['86', '89'], 'PI'], [['91', '93', '94'], 'PA'], [['92', '97'], 'AM'], [['95'], 'RR'], [['96'], 'AP'], [['98', '99'], 'MA']]
  .forEach(([ks, uf]) => ks.forEach(k => DDD[k] = uf));
const UF_GRID = { RR: [1, 0], AP: [3, 0], AM: [1, 1], PA: [2, 1], MA: [3, 1], CE: [4, 1], RN: [5, 1], AC: [0, 2], RO: [1, 2], TO: [2, 2], PI: [3, 2], PE: [4, 2], PB: [5, 2],
  MT: [1, 3], GO: [2, 3], BA: [3, 3], SE: [4, 3], AL: [5, 3], MS: [1, 4], DF: [2, 4], MG: [3, 4], ES: [4, 4], SP: [2, 5], RJ: [3, 5], PR: [2, 6], SC: [2, 7], RS: [2, 8] };
function ufOf(c) {
  const d = normPhone(c.phone);
  return d.startsWith('55') && d.length >= 12 ? DDD[d.slice(2, 4)] : null;
}
function estadosCard() {
  const cnt = {}; let total = 0;
  db.clients.forEach(c => { const uf = ufOf(c); if (uf) { cnt[uf] = (cnt[uf] || 0) + 1; total++; } });
  const max = Math.max(1, ...Object.values(cnt));
  const color = n => {
    if (!n) return ['#2c3a4f', '#7b8aa0'];
    const t = n / max;
    if (t > .66) return ['#e05252', '#fff'];
    if (t > .2) return ['#5cba6e', '#fff'];
    if (t > .05) return ['#3fae9a', '#fff'];
    return ['#3f93a8', '#fff'];
  };
  let tiles = '';
  for (let r = 0; r <= 8; r++) for (let col = 0; col < 7; col++) {
    const uf = Object.keys(UF_GRID).find(k => UF_GRID[k][0] === col && UF_GRID[k][1] === r);
    if (!uf) { tiles += '<div></div>'; continue; }
    const [bg, fg] = color(cnt[uf]);
    tiles += `<div class="tile" style="background:${bg};color:${fg}">${uf}<small>${cnt[uf] || 0}</small></div>`;
  }
  const top = Object.entries(cnt).sort((a, b) => b[1] - a[1]).slice(0, 5);
  return `<div class="card dark">
    <div class="card-head"><div class="ico">${ic('pin')}</div><h3>Clientes por Estado</h3><span class="tot">Total: <b>${total}</b></span></div>
    <div class="tilemap">${tiles}</div>
    <div class="uf-list">${top.map(([uf, n]) => `<div><span>${uf}</span><b>${n} · ${Math.round(n / total * 100)}%</b></div>`).join('') || '<div><span style="color:#9aa0ae">Estado identificado pelo DDD do WhatsApp</span></div>'}</div>
  </div>`;
}

/* ----- Charts ----- */
function smoothPath(pts) {
  if (pts.length < 2) return pts.length ? `M${pts[0][0]},${pts[0][1]}` : '';
  let d = `M${pts[0][0]},${pts[0][1]}`;
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] || pts[i], p1 = pts[i], p2 = pts[i + 1], p3 = pts[i + 2] || p2;
    const c1 = [p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6];
    const c2 = [p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6];
    d += ` C${c1[0].toFixed(1)},${c1[1].toFixed(1)} ${c2[0].toFixed(1)},${c2[1].toFixed(1)} ${p2[0].toFixed(1)},${p2[1].toFixed(1)}`;
  }
  return d;
}
function niceMax(v) {
  if (v <= 0) return 4;
  const e = Math.pow(10, Math.floor(Math.log10(v))), f = v / e;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 4 ? 4 : f <= 5 ? 5 : f <= 8 ? 8 : 10) * e;
}
function lineChart(series, labels, o) {
  const W = 340, H = 220, L = o.left || 40, R = 8, T = 12, B = 30;
  const max = niceMax(Math.max(0, ...series.flatMap(s => s.vals)));
  const n = labels.length, x = i => L + (n === 1 ? 0 : i * (W - L - R) / (n - 1)), y = v => T + (1 - v / max) * (H - T - B);
  let g = '';
  for (let i = 0; i <= 4; i++) {
    const v = max * i / 4, yy = y(v);
    g += `<line x1="${L}" x2="${W - R}" y1="${yy}" y2="${yy}" stroke="#e8eaf0" stroke-dasharray="4 4"/><text x="${L - 6}" y="${yy + 4}" text-anchor="end">${o.fmt(v)}</text>`;
  }
  labels.forEach((l, i) => { if (l !== '') g += `<text x="${x(i)}" y="${H - 8}" text-anchor="middle">${l}</text>`; });
  series.forEach((s, si) => {
    const pts = s.vals.map((v, i) => [x(i), y(v)]);
    const path = smoothPath(pts);
    const gid = 'g' + Math.random().toString(36).slice(2, 7);
    g += `<defs><linearGradient id="${gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${s.fill}"/><stop offset="1" stop-color="${s.fill}" stop-opacity="0"/></linearGradient></defs>`;
    g += `<path d="${path} L${x(n - 1)},${y(0)} L${x(0)},${y(0)}Z" fill="url(#${gid})"/>`;
    g += `<path d="${path}" fill="none" stroke="${s.color}" stroke-width="${o.dots ? 2.6 : 3}" stroke-linecap="round"/>`;
    if (o.dots) pts.forEach(p => g += `<circle cx="${p[0]}" cy="${p[1]}" r="3.2" fill="${s.color}" fill-opacity=".75"/>`);
  });
  return `<svg class="chart" viewBox="0 0 ${W} ${H}">${g}</svg>`;
}
function stackChart(a, b, labels) {
  const W = 340, H = 240, L = 30, R = 4, T = 10, B = 26;
  const max = niceMax(Math.max(1, ...a.map((v, i) => v + b[i])));
  const n = a.length, slot = (W - L - R) / Math.max(n, 1), bw = Math.max(3, slot * .72), y = v => T + (1 - v / max) * (H - T - B);
  let g = '';
  for (let i = 0; i <= 4; i++) { const v = max * i / 4; g += `<line x1="${L}" x2="${W - R}" y1="${y(v)}" y2="${y(v)}" stroke="#e8eaf0" stroke-dasharray="4 4"/><text x="${L - 6}" y="${y(v) + 4}" text-anchor="end">${Math.round(v)}</text>`; }
  a.forEach((v, i) => {
    const x0 = L + i * slot + (slot - bw) / 2;
    if (v) g += `<rect x="${x0}" y="${y(v)}" width="${bw}" height="${y(0) - y(v)}" fill="#6366f1"/>`;
    if (b[i]) g += `<rect x="${x0}" y="${y(v + b[i])}" width="${bw}" height="${y(v) - y(v + b[i])}" fill="#6fd17f" rx="1.5"/>`;
    if (labels[i]) g += `<text x="${x0 + bw / 2}" y="${H - 6}" text-anchor="middle">${labels[i]}</text>`;
  });
  g += `<line x1="${L}" x2="${W - R}" y1="${y(0)}" y2="${y(0)}" stroke="#cfd3dc"/>`;
  return `<svg class="chart" viewBox="0 0 ${W} ${H}">${g}</svg>`;
}

/* ----- Tools ----- */
VIEWS.ferramentas = () => {
  const tool = (act, icon, bg, title, sub) => `<button class="card tool" data-act="${act}"><div class="ico" style="background:${bg}">${ic(icon)}</div><div><b>${title}</b><span>${sub}</span></div>${ic('right')}</button>`;
  return `
    ${tool('plans', 'layers', 'linear-gradient(135deg,#a855f7,#6366f1)', 'Planos', `${db.plans.length} planos cadastrados`)}
    ${tool('template', 'msg', 'linear-gradient(135deg,#5ac35f,#2f9c50)', 'Mensagem de cobrança', 'Texto enviado pelo WhatsApp')}
    ${tool('bulk', 'send', 'linear-gradient(135deg,#ef8a2c,#e0457b)', 'Cobrar vencidos hoje', 'Abre o WhatsApp de cada cliente')}
    ${tool('export', 'download', 'linear-gradient(135deg,#3b82f6,#4f46e5)', 'Exportar backup', 'Salva todos os dados em JSON')}
    ${tool('import', 'upload', 'linear-gradient(135deg,#0ea5e9,#10b981)', 'Importar backup / CSV', 'Restaura JSON ou importa planilha CSV')}
    ${tool('profile', 'user', 'linear-gradient(135deg,#64748b,#334155)', 'Perfil', 'Nome exibido no topo')}
    ${tool('demo', 'sparkle', 'linear-gradient(135deg,#f6b73c,#ef8a2c)', 'Dados de exemplo', 'Preenche o app para testar')}
    ${tool('wipe', 'trash', 'linear-gradient(135deg,#ef4444,#b91c1c)', 'Apagar tudo', 'Remove clientes, pagamentos e histórico')}
    <input type="file" id="fileIn" accept=".json,.csv,text/*,application/json" hidden>
    <p class="hint" style="text-align:center">Os dados ficam salvos neste aparelho. Faça backup com frequência.</p>`;
};
VIEWS.ferramentasAfter = () => { $('#fileIn').onchange = e => importFile(e.target.files[0]); };

/* ---------- Overlays ---------- */
const layer = () => $('#layer');
function overlay(html, cls) {
  closeMenus();
  const s = document.createElement('div');
  s.className = 'scrim' + (cls ? ' ' + cls : '');
  s.innerHTML = html;
  s.addEventListener('click', e => { if (e.target === s) closeTop(); });
  layer().appendChild(s);
  paintIcons(s);
  return s;
}
function closeTop() { const l = layer().lastElementChild; if (l) l.remove(); }
function closeMenus() { document.querySelectorAll('.menu,.pop,.pop-scrim').forEach(m => m.remove()); }
window.onBack = () => {
  if (document.querySelector('.menu,.pop')) { closeMenus(); return true; }
  if (layer().children.length) { closeTop(); return true; }
  if (state.tab !== 'dashboard') { go('dashboard'); return true; }
  return false;
};

function dropdown(btn, opts, value, onPick) {
  closeMenus();
  const r = btn.getBoundingClientRect();
  const m = document.createElement('div');
  m.className = 'menu';
  m.innerHTML = opts.map(([v, l]) => `<button data-v="${esc(v)}" class="${String(v) === String(value) ? 'on' : ''}"><span>${esc(l)}</span>${String(v) === String(value) ? ic('check') : ''}</button>`).join('');
  const back = document.createElement('div');
  back.className = 'pop-scrim';
  back.style.cssText = 'position:fixed;inset:0;z-index:59';
  back.onclick = closeMenus;
  document.body.append(back, m);
  paintIcons(m);
  const w = Math.max(r.width, opts.reduce((a, o) => Math.max(a, String(o[1]).length), 0) > 14 ? 260 : 160);
  m.style.minWidth = w + 'px';
  let left = Math.min(r.left, innerWidth - w - 8);
  m.style.left = Math.max(8, left) + 'px';
  const below = innerHeight - r.bottom - 80;
  if (below > 220 || below > r.top - 90) { m.style.top = (r.bottom + 6) + 'px'; m.style.maxHeight = Math.max(160, below) + 'px'; }
  else { m.style.bottom = (innerHeight - r.top + 6) + 'px'; m.style.maxHeight = Math.max(160, r.top - 90) + 'px'; }
  m.addEventListener('click', e => { const b = e.target.closest('button'); if (b) { closeMenus(); onPick(b.dataset.v); } });
  const on = m.querySelector('.on'); if (on) on.scrollIntoView({ block: 'nearest' });
}

function detailSheet(c) {
  const p = planOf(c), st = statusOf(c), price = priceOf(c), base = p ? p.price * (c.qty || 1) : 0, cost = costOf(c);
  const s = overlay(`<div class="sheet">
    <div class="sheet-hero"><button class="x" data-act="close">${ic('x')}</button>
      <div class="det-top"><div class="ini">${initial(c.name)}</div><div><b>${esc(c.name)}</b><br><span class="status" style="--c:${st.color}">${st.short || st.text}</span></div></div></div>
    <div class="sheet-body">
      <div class="toggles">
        <button class="tg" data-act="t-ess" data-id="${c.id}">${ic('star')}<span>Essencial</span><i class="sw ${c.essential ? 'on' : ''}"></i></button>
        <button class="tg" data-act="t-arc" data-id="${c.id}">${ic('archive')}<span>Arquivar</span><i class="sw ${c.archived ? 'on' : ''}"></i></button>
      </div>
      <button class="wa-big" data-act="wa" data-id="${c.id}"><div class="ic">${ic('wa')}</div><div><b>Abrir WhatsApp</b><span>${esc(fmtPhone(c.phone))}</span></div>${ic('chat')}</button>
      <div class="plan-box">
        <div class="ph"><div class="ico">${ic('box')}</div><b>${p ? esc(p.name) : 'Sem plano'}</b>${p ? `<span class="tag">${p.type}</span>` : ''}</div>
        <div class="vals">
          <div class="val"><small style="color:#9aa0ae">${ic('dollar', 'style="color:#3fae56"')} VALOR</small>${c.discount > 0 ? `<s>${money(base)}</s>` : ''}<b>${money(price)}</b>${c.discount > 0 ? ` <em>${ic('tag', 'style="width:13px;height:13px"')}-${money(c.discount)}</em>` : ''}${c.qty > 1 ? `<small>${c.qty} telas</small>` : ''}</div>
          <div class="val"><small>${ic('trending', 'style="color:#3b6fe0"')} CUSTO</small><b>${money(cost)}</b><span class="lucro">Lucro ${money(price - cost)}</span></div>
        </div>
        <div class="due-box"><div class="ico">${ic('calendar')}</div><div><b>${br(c.due)}</b><span>Vencimento</span></div></div>
      </div>
      ${c.notes ? `<div class="notes">${esc(c.notes)}</div>` : ''}
      <div class="two" style="margin-top:16px">
        <button class="btn light" data-act="pays" data-id="${c.id}">${ic('history')} Pagamentos</button>
        <button class="btn blue" data-act="renew" data-id="${c.id}">${ic('renew')} Renovar</button>
      </div>
      <div class="reg">${ic('userplus')} Cadastrado em ${br(c.createdAt)}</div>
    </div></div>`);
  s.dataset.detail = c.id;
}
function reopenDetail(id) {
  const open = layer().querySelector('[data-detail]');
  if (open) { open.remove(); const c = db.clients.find(x => x.id === id); if (c) detailSheet(c); }
}

function paysSheet(c) {
  const pays = db.payments.filter(p => p.clientId === c.id).sort((a, b) => a.date < b.date ? 1 : -1);
  const total = pays.reduce((a, p) => a + p.amount, 0);
  const fmtDate = d => { const x = parse(d); return `${x.getDate()} de ${MONTHS[x.getMonth()].slice(0, 3)}, ${x.getFullYear()}`; };
  overlay(`<div class="sheet">
    <div class="pay-hero"><button class="x" data-act="close">${ic('x')}</button>
      <div class="top2"><div class="ic">${ic('history')}</div><div><h3>Histórico de Pagamentos</h3><span>${esc(c.name)}</span></div></div>
      <div class="tots"><div><small>Total de Pagamentos</small><b>${pays.length}</b></div><div><small>Valor Total Pago</small><b>${money(total)}</b></div></div></div>
    <div class="sheet-body">${pays.map(p => `<div class="pay"><div class="l">
      <div class="chips"><span class="ok">${ic('checkc')} Pago</span>${p.kind === 'cadastro' ? `<span class="k">${ic('userplus')} Cadastro</span>` : `<span class="k ren">${ic('refresh')} Renovação</span>`}</div>
      <div class="d">${p.kind === 'cadastro' ? 'Pagamento inicial' : 'Renovação'} - Plano ${esc(p.planName)}</div>
      <div class="dt">${ic('calendar')} ${fmtDate(p.date)}</div></div><div class="amt">${money(p.amount)}</div></div>`).join('') || '<div class="empty">Nenhum pagamento registrado</div>'}</div>
  </div>`);
}

function clientForm(c) {
  const isNew = !c;
  c = c || { name: '', phone: '', planId: db.plans[0] ? db.plans[0].id : '', due: '', qty: 1, discount: 0, notes: '' };
  const defDue = () => { const p = db.plans.find(x => x.id === $('#fPlan').value); return addMonths(today(), p ? TYPES[p.type] : 1); };
  const s = overlay(`<div class="sheet"><form class="form" id="cform" autocomplete="off">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic(isNew ? 'userplus' : 'user')}</div><h2>${isNew ? 'Novo Cliente' : 'Editar Cliente'}</h2></div>
    <p class="sub">${isNew ? 'Preencha os dados do novo cliente' : 'Atualize as informações do cliente'}</p>
    <div class="f"><label>${ic('user', 'style="color:#a855f7"')} Nome <em>*</em></label><input id="fName" required value="${esc(c.name)}"></div>
    <div class="f"><label>${ic('phone', 'style="color:#3fae56"')} WhatsApp <em>*</em></label><input id="fPhone" type="tel" inputmode="tel" required placeholder="+55 11 99999-9999" value="${esc(fmtPhone(c.phone))}"></div>
    <div class="f2">
      <div class="f"><label>${ic('box', 'style="color:#4f46e5"')} Plano <em>*</em></label><select id="fPlan">${db.plans.map(p => `<option value="${p.id}" ${p.id === c.planId ? 'selected' : ''}>${esc(p.name)}</option>`).join('')}<option value="" ${!planOf(c) ? 'selected' : ''}>Sem plano</option></select></div>
      <div class="f"><label>${ic('calendar', 'style="color:#e05252"')} Vencimento <em>*</em></label><input id="fDue" type="date" value="${c.due || ''}"></div>
    </div>
    <div class="f2">
      <div class="f"><label>${ic('tag', 'style="color:#3b82f6"')} Quantidade</label><input id="fQty" type="number" min="1" inputmode="numeric" value="${c.qty || 1}"></div>
      <div class="f"><label>${ic('percent', 'style="color:#ef8a2c"')} Desconto (R$)</label><div class="pre"><span>R$</span><input id="fDisc" type="number" min="0" step="0.01" inputmode="decimal" value="${c.discount || ''}"></div></div>
    </div>
    <button type="button" class="fold" id="foldNotes">${ic('file')} ${c.notes ? 'Notas' : 'Adicionar Notas (Opcional)'} ${ic('down')}</button>
    <div class="f" id="notesBox" ${c.notes ? '' : 'hidden'}><textarea id="fNotes" placeholder="Usuário, senha, aparelho...">${esc(c.notes || '')}</textarea></div>
    ${isNew ? '<p class="hint" style="margin:-4px 0 14px">O cadastro é registrado como pagamento no Financeiro.</p>' : ''}
    <button class="btn" type="submit">✓ ${isNew ? 'Cadastrar Cliente' : 'Atualizar Cliente'}</button>
  </form></div>`);
  if (isNew) { $('#fDue').value = defDue(); $('#fPlan').onchange = () => { $('#fDue').value = defDue(); }; }
  $('#foldNotes').onclick = () => { $('#notesBox').hidden = !$('#notesBox').hidden; if (!$('#notesBox').hidden) $('#fNotes').focus(); };
  s.querySelector('form').onsubmit = e => {
    e.preventDefault();
    const name = $('#fName').value.trim(), phone = normPhone($('#fPhone').value);
    if (!name) return toast('Informe o nome');
    if (phone.length < 10) return toast('WhatsApp inválido');
    if (!$('#fDue').value) return toast('Informe o vencimento');
    const data = { name, phone, planId: $('#fPlan').value, due: $('#fDue').value, qty: Math.max(1, parseInt($('#fQty').value, 10) || 1),
      discount: Math.max(0, parseFloat(String($('#fDisc').value).replace(',', '.')) || 0), notes: $('#fNotes').value.trim() };
    if (isNew) {
      const nc = Object.assign({ id: uid(), essential: false, archived: false, createdAt: today() }, data);
      db.clients.push(nc);
      const p = planOf(nc), pay = p ? addPayment(nc, 'cadastro', priceOf(nc)) : null;
      log('Novo Cliente Cadastrado', `${nc.name} - Plano: ${p ? p.name : '—'} | Valor: R$ ${priceOf(nc).toFixed(2)}${nc.discount ? ` (Desconto: R$ ${nc.discount.toFixed(2)})` : ''}`,
        { type: 'create', clientId: nc.id, paymentId: pay && pay.id });
      toast('Cliente cadastrado');
    } else {
      const before = JSON.parse(JSON.stringify(c));
      Object.assign(c, data);
      log('Cliente Editado', `${c.name}`, { type: 'edit', before });
      toast('Cliente atualizado');
    }
    snapshot(); closeTop(); refresh();
    if (!isNew) reopenDetail(c.id);
  };
}

function renewDialog(c) {
  const p = planOf(c);
  if (!p) { toast('Escolha um plano para este cliente antes de renovar'); return clientForm(c); }
  const base = c.due && c.due >= today() ? c.due : today();
  const next = addMonths(base, TYPES[p.type] || 1);
  const s = overlay(`<div class="dialog">
    <div class="dlg-hero"><button class="x" data-act="close">${ic('x')}</button><div class="c">${ic('renew', 'style="width:30px;height:30px"')}</div><h3>Renovar Plano</h3></div>
    <div class="dlg-body">
      <div class="ren-c"><div><b>${ic('user')} ${esc(c.name)}</b><div style="margin-top:12px;color:#5b6275">${esc(p.name)}${c.qty > 1 ? ` · ${c.qty} telas` : ''}</div></div>
        <div class="p"><b>${money(priceOf(c))}</b>${c.discount ? `<small>-R$${Number(c.discount).toFixed(2)}</small>` : ''}</div></div>
      <div class="ren-new"><small>${ic('checkc')} Novo Vencimento</small><b>${br(next)}</b></div>
      <div class="ren-warn">⚠️ Renovação será registrada como paga</div>
      <div class="two"><button class="btn light" data-act="close">Cancelar</button><button class="btn blue" id="doRenew">Confirmar</button></div>
    </div></div>`, 'center');
  s.querySelector('#doRenew').onclick = () => {
    const old = c.due;
    c.due = next;
    const pay = addPayment(c, 'renovacao', priceOf(c));
    log('Plano Renovado', `${c.name} - ${p.name} | Valor: R$ ${pay.amount.toFixed(2)} | Novo vencimento: ${br(next)}`, { type: 'renew', clientId: c.id, oldDue: old, paymentId: pay.id });
    snapshot(); closeTop(); refresh(); reopenDetail(c.id);
    toast('Renovado até ' + br(next));
  };
}

function confirmDialog(title, text, onOk) {
  const s = overlay(`<div class="dialog confirm">${ic('alert')}<h3>${title}</h3><p>${text}</p>
    <div class="two"><button class="cancel" data-act="close">Cancelar</button><button class="ok" id="okBtn">Confirmar</button></div></div>`, 'center');
  s.querySelector('#okBtn').onclick = () => { closeTop(); onOk(); };
}

function historyPop() {
  closeMenus();
  const back = document.createElement('div'); back.className = 'pop-scrim'; back.style.cssText = 'position:fixed;inset:0;z-index:59'; back.onclick = closeMenus;
  const p = document.createElement('div'); p.className = 'pop';
  const when = s => { const d = parse(s); return `${d.getDate()} de ${MONTHS[d.getMonth()].slice(0, 3)} às ${s.slice(11, 16)}`; };
  p.innerHTML = `<div class="ph">${ic('history')} Histórico <span class="n">${db.history.length}</span></div><div class="pb">${db.history.map(h => `<div class="hi"><span class="ck">✓</span><div>
    <b>${esc(h.title)}</b><p>${esc(h.desc)}</p><small>${when(h.at)}</small>
    ${h.undone ? `<span class="done">${ic('checkc')} Ação desfeita</span>` : h.undo ? `<button class="undo" data-undo="${h.id}">${ic('undo')} Desfazer</button>` : ''}</div></div>`).join('') || '<div class="empty">Nenhuma ação ainda</div>'}</div>`;
  document.body.append(back, p); paintIcons(p);
  p.addEventListener('click', e => { const b = e.target.closest('[data-undo]'); if (b) { undo(b.dataset.undo); closeMenus(); historyPop(); } });
}
function undo(id) {
  const h = db.history.find(x => x.id === id); if (!h || !h.undo || h.undone) return;
  const u = h.undo;
  const dropPay = pid => { db.payments = db.payments.filter(p => p.id !== pid); };
  if (u.type === 'create') { db.clients = db.clients.filter(c => c.id !== u.clientId); if (u.paymentId) dropPay(u.paymentId); }
  else if (u.type === 'renew') { const c = db.clients.find(x => x.id === u.clientId); if (c) c.due = u.oldDue; dropPay(u.paymentId); }
  else if (u.type === 'delete') { db.clients.push(u.client); }
  else if (u.type === 'edit') { const i = db.clients.findIndex(x => x.id === u.before.id); if (i >= 0) db.clients[i] = u.before; }
  h.undone = true;
  snapshot(); refresh(); toast('Ação desfeita');
}

function notificationsPop() {
  closeMenus();
  const L = live();
  const groups = [
    ['Vencem hoje', L.filter(c => daysLeft(c) === 0)],
    ['Vencem amanhã', L.filter(c => daysLeft(c) === 1)],
    ['Venceram ontem', L.filter(c => daysLeft(c) === -1)],
  ];
  const back = document.createElement('div'); back.className = 'pop-scrim'; back.style.cssText = 'position:fixed;inset:0;z-index:59'; back.onclick = closeMenus;
  const p = document.createElement('div'); p.className = 'pop';
  const total = groups.reduce((a, g) => a + g[1].length, 0);
  p.innerHTML = `<div class="ph">${ic('bell')} Avisos <span class="n">${total}</span></div><div class="pb">${groups.filter(g => g[1].length).map(([t, list]) =>
    `<div style="padding:12px 18px 4px;font-weight:700;color:#5b6275">${t} · ${list.length}</div>` + list.map(c => `<div class="row" data-act="detail" data-id="${c.id}" style="border-color:#eceef4">
      <div class="ini" style="background:linear-gradient(135deg,#6366f1,#9333ea)">${initial(c.name)}</div><div class="nm"><b>${esc(c.name)}</b><span>${planOf(c) ? esc(planOf(c).name) : ''}</span></div>
      <button class="wa" data-act="wa" data-id="${c.id}">${ic('wa')}</button></div>`).join('')).join('') || '<div class="empty">Nenhum aviso por enquanto</div>'}</div>`;
  document.body.append(back, p); paintIcons(p);
}

function plansSheet() {
  const s = overlay(`<div class="sheet"><div class="form">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic('layers')}</div><h2>Planos</h2></div>
    <p class="sub">Valor cobrado do cliente e seu custo por tela</p>
    <div>${db.plans.map(p => `<div class="plan-item"><div><b>${esc(p.name)}</b><br><small>${TYPE_NAMES[p.type]} · ${money(p.price)} · custo ${money(p.cost)} · ${db.clients.filter(c => c.planId === p.id).length} clientes</small></div>
      <button class="sq" data-act="plan-edit" data-id="${p.id}">${ic('edit')}</button><button class="sq" style="color:#d24444" data-act="plan-del" data-id="${p.id}">${ic('trash')}</button></div>`).join('') || '<div class="empty">Nenhum plano</div>'}</div>
    <div style="height:16px"></div><button class="btn" data-act="plan-edit">${ic('plus')} Novo plano</button>
  </div></div>`);
  s.dataset.plans = 1;
}
function planForm(p) {
  const isNew = !p; p = p || { name: '', type: 'mensal', price: '', cost: '' };
  const s = overlay(`<div class="sheet"><form class="form">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic('box')}</div><h2>${isNew ? 'Novo Plano' : 'Editar Plano'}</h2></div><p class="sub"></p>
    <div class="f"><label>Nome <em>*</em></label><input id="pName" value="${esc(p.name)}" required></div>
    <div class="f"><label>Período</label><select id="pType">${Object.keys(TYPES).map(t => `<option value="${t}" ${t === p.type ? 'selected' : ''}>${TYPE_NAMES[t]}</option>`).join('')}</select></div>
    <div class="f2"><div class="f"><label>Valor (R$)</label><input id="pPrice" type="number" step="0.01" inputmode="decimal" value="${p.price}"></div>
      <div class="f"><label>Custo (R$)</label><input id="pCost" type="number" step="0.01" inputmode="decimal" value="${p.cost}"></div></div>
    <button class="btn" type="submit">Salvar</button></form></div>`);
  s.querySelector('form').onsubmit = e => {
    e.preventDefault();
    const data = { name: $('#pName').value.trim().toUpperCase(), type: $('#pType').value, price: parseFloat($('#pPrice').value) || 0, cost: parseFloat($('#pCost').value) || 0 };
    if (!data.name) return toast('Informe o nome');
    if (isNew) db.plans.push(Object.assign({ id: uid() }, data)); else Object.assign(p, data);
    save(); closeTop(); closeTop(); plansSheet(); render();
  };
}

function templateSheet() {
  const s = overlay(`<div class="sheet"><form class="form">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic('msg')}</div><h2>Mensagem</h2></div>
    <p class="sub">Use {nome}, {plano}, {vencimento} e {valor}.</p>
    <div class="f"><textarea id="tpl" style="height:160px">${esc(db.settings.template)}</textarea></div>
    <button class="btn" type="submit">Salvar</button></form></div>`);
  s.querySelector('form').onsubmit = e => { e.preventDefault(); db.settings.template = $('#tpl').value; save(); closeTop(); toast('Mensagem salva'); };
}
function profileSheet() {
  const s = overlay(`<div class="sheet"><form class="form">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic('user')}</div><h2>Perfil</h2></div><p class="sub">Nome da sua revenda, mostrado no círculo do topo.</p>
    <div class="f"><input id="brand" maxlength="12" value="${esc(db.settings.brand)}"></div>
    <button class="btn" type="submit">Salvar</button></form></div>`);
  s.querySelector('form').onsubmit = e => { e.preventDefault(); db.settings.brand = $('#brand').value.trim(); closeTop(); refresh(); };
}
function bulkSheet() {
  const list = live().filter(c => daysLeft(c) === 0);
  overlay(`<div class="sheet"><div class="form">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic('send')}</div><h2>Cobrar hoje</h2></div><p class="sub">Toque em cada cliente para abrir o WhatsApp com a mensagem pronta.</p>
    ${list.map(c => `<div class="plan-item"><div><b>${esc(c.name)}</b><br><small>${esc(fmtPhone(c.phone))} · ${money(priceOf(c))}</small></div>
      <button class="wa" data-act="wa-msg" data-id="${c.id}">${ic('wa')}</button></div>`).join('') || '<div class="empty">Nenhum cliente vence hoje</div>'}
  </div></div>`);
}

/* ---------- Backup / import ---------- */
function exportData() {
  const json = JSON.stringify(db);
  const name = `gestor-backup-${today()}.json`;
  if (window.Android) { Android.share(name, json); return; }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([json], { type: 'application/json' })); a.download = name; a.click();
}
function importFile(f) {
  if (!f) return;
  const r = new FileReader();
  r.onload = () => {
    const txt = String(r.result).replace(/^﻿/, '');
    try {
      if (txt.trim().startsWith('{')) {
        const d = JSON.parse(txt);
        if (!d.clients || !d.plans) throw new Error('arquivo não é um backup do gestor');
        confirmDialog('Restaurar backup', `Substituir os dados atuais por ${d.clients.length} clientes do backup?`, () => { db = d; load2(); refresh(); toast('Backup restaurado'); });
      } else importCsv(txt);
    } catch (e) { toast('Erro: ' + e.message); }
  };
  r.readAsText(f);
}
function load2() { db.snapshots = db.snapshots || {}; db.history = db.history || []; db.payments = db.payments || []; db.settings = Object.assign(blank().settings, db.settings || {}); save(); }
function importCsv(txt) {
  const lines = txt.split(/\r?\n/).filter(l => l.trim());
  const sep = (lines[0].match(/;/g) || []).length >= (lines[0].match(/,/g) || []).length ? ';' : ',';
  const head = lines[0].toLowerCase().split(sep).map(s => s.trim());
  const col = names => head.findIndex(h => names.some(n => h.includes(n)));
  const iN = col(['nome', 'name']), iP = col(['whats', 'telefone', 'phone', 'celular']), iPl = col(['plano', 'plan']), iD = col(['venc', 'due', 'data']);
  if (iN < 0) return toast('CSV precisa da coluna "nome"');
  let n = 0;
  lines.slice(1).forEach(l => {
    const v = l.split(sep).map(s => s.trim().replace(/^"|"$/g, ''));
    if (!v[iN]) return;
    let due = iD >= 0 ? v[iD] : '';
    const m = due.match(/^(\d{1,2})\/(\d{1,2})\/(\d{2,4})$/);
    if (m) due = `${m[3].length === 2 ? '20' + m[3] : m[3]}-${pad(m[2])}-${pad(m[1])}`;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(due)) due = '';
    let plan = iPl >= 0 && v[iPl] ? db.plans.find(p => p.name.toLowerCase() === v[iPl].toLowerCase()) : null;
    if (!plan && iPl >= 0 && v[iPl]) {
      const nm = v[iPl].toUpperCase();
      const type = /ANU/.test(nm) ? 'anual' : /SEMES/.test(nm) ? 'semestral' : /TRIM/.test(nm) ? 'trimestral' : 'mensal';
      plan = { id: uid(), name: nm, type, price: 0, cost: 0 }; db.plans.push(plan);
    }
    db.clients.push({ id: uid(), name: v[iN], phone: normPhone(iP >= 0 ? v[iP] : ''), planId: plan ? plan.id : '', due, qty: 1, discount: 0, notes: '', essential: false, archived: false, createdAt: today() });
    n++;
  });
  snapshot(); refresh(); toast(`${n} clientes importados`);
}

function demoData() {
  const names = ['Julio', 'Edo', 'Maria rosa', 'Maria', 'Paulo', 'Débora', 'Wedson', 'Denio', 'Arsênio', 'Valeria', 'Andrr', 'Carlos', 'Fernanda', 'Rafael', 'Juliana', 'Bruno',
    'Patrícia', 'Lucas', 'Camila', 'Rodrigo', 'Aline', 'Marcos', 'Tatiane', 'Felipe', 'Renata', 'Gustavo', 'Simone', 'Diego', 'Larissa', 'Thiago', 'Vanessa', 'André', 'Priscila', 'Leandro'];
  const ddds = ['11', '11', '11', '12', '13', '19', '21', '31', '41', '47', '51', '61', '62', '71', '81', '85', '91', '92', '27', '35'];
  const plans = [['XPRIMETV MENSAL', 'mensal', 34.89, 7.25], ['XPRIMETV TRIMESTRAL', 'trimestral', 94.9, 21.75], ['PIXEL PLAYER MENSAL', 'mensal', 30, 6], ['FUNPLAY ANUAL', 'anual', 299.9, 70]];
  plans.forEach(([name, type, price, cost]) => { if (!db.plans.some(p => p.name === name)) db.plans.push({ id: uid(), name, type, price, cost }); });
  const pick = a => a[Math.floor(Math.random() * a.length)];
  const t = today();
  for (let i = 0; i < 160; i++) {
    const p = Math.random() < .75 ? db.plans.find(x => x.name === 'XPRIMETV MENSAL') : pick(db.plans);
    const months = TYPES[p.type];
    const created = addDays(t, -Math.floor(Math.random() * 200));
    const c = { id: uid(), name: pick(names) + (Math.random() < .3 ? ' ' + pick(['Silva', 'Souza', 'Lima', 'Costa']) : ''), phone: '55' + pick(ddds) + '9' + String(Math.floor(1e7 + Math.random() * 9e7)),
      planId: p.id, due: '', qty: Math.random() < .1 ? 2 : 1, discount: Math.random() < .2 ? 7 : 0, notes: '', essential: Math.random() < .04, archived: false, createdAt: created };
    db.clients.push(c);
    let due = created, first = true;
    while (due <= t) {
      const at = `${due}T${pad(8 + Math.floor(Math.random() * 13))}:${pad(Math.floor(Math.random() * 60))}`;
      if (!first && Math.random() < .12) break;
      db.payments.push({ id: uid(), clientId: c.id, clientName: c.name, planName: p.name, amount: priceOf(c), cost: costOf(c), date: at, kind: first ? 'cadastro' : 'renovacao' });
      first = false;
      due = addMonths(due, months);
    }
    c.due = due;
  }
  const keys = []; for (let i = 9; i >= 1; i--) keys.push(addDays(t, -i));
  let base = live().filter(isActive).length - 40;
  keys.forEach(k => { base += Math.round(Math.random() * 12 - 3); db.snapshots[k] = base; });
  snapshot(); refresh(); toast('Dados de exemplo adicionados');
}

/* ---------- PDF report ---------- */
function pdf() {
  const r = periodRange();
  const pays = db.payments.filter(p => inRange(p, r)).sort((a, b) => a.date < b.date ? -1 : 1);
  const { rev, prof } = sumBy(pays);
  const label = state.period === 'tudo' ? 'Todo período' : `${br(r[0])} a ${br(r[1])}`;
  $('#print').innerHTML = `<h1>Relatório Financeiro</h1><div>${esc(db.settings.brand)} · ${label} · gerado em ${br(today())}</div>
    <div class="kp"><div>Receita<b>${money(rev)}</b></div><div>Lucro<b>${money(prof)}</b></div><div>Pagamentos<b>${pays.length}</b></div><div>Ticket médio<b>${money(pays.length ? rev / pays.length : 0)}</b></div></div>
    <table><thead><tr><th>Data</th><th>Cliente</th><th>Plano</th><th>Tipo</th><th class="r">Valor</th><th class="r">Lucro</th></tr></thead><tbody>
    ${pays.map(p => `<tr><td>${br(p.date)} ${p.date.slice(11, 16)}</td><td>${esc(p.clientName)}</td><td>${esc(p.planName)}</td><td>${p.kind === 'cadastro' ? 'Cadastro' : 'Renovação'}</td><td class="r">${money(p.amount)}</td><td class="r">${money(p.amount - (p.cost || 0))}</td></tr>`).join('')}
    </tbody></table>`;
  const name = `Relatorio-${r[0] === '0000-01-01' ? 'completo' : r[0]}`;
  if (window.Android) Android.print(name); else window.print();
}

/* ---------- Events ---------- */
document.addEventListener('click', e => {
  const tab = e.target.closest('[data-tab]');
  if (tab) { closeMenus(); while (layer().children.length) closeTop(); return go(tab.dataset.tab); }
  const el = e.target.closest('[data-act]');
  if (!el) return;
  const act = el.dataset.act, id = el.dataset.id, c = id && db.clients.find(x => x.id === id);
  e.stopPropagation();
  switch (act) {
    case 'close': closeTop(); break;
    case 'filter':
      Object.assign(state, { fStatus: el.dataset.f, fType: 'todos', fPlan: 'todos', q: '' });
      closeMenus(); go('clientes'); break;
    case 'detail': closeMenus(); if (c) detailSheet(c); break;
    case 'wa': if (c) openUrl(waLink(c)); break;
    case 'wa-msg': if (c) openUrl(waLink(c, waMessage(c))); break;
    case 'pays': if (c) paysSheet(c); break;
    case 'edit': if (c) clientForm(c); break;
    case 'new-client': clientForm(null); break;
    case 'renew': if (c) renewDialog(c); break;
    case 'star': if (c) { c.essential = !c.essential; refresh(); toast(c.essential ? 'Marcado como essencial' : 'Removido dos essenciais'); } break;
    case 't-ess': if (c) { c.essential = !c.essential; el.querySelector('.sw').classList.toggle('on', c.essential); refresh(); } break;
    case 't-arc': if (c) {
      c.archived = !c.archived; el.querySelector('.sw').classList.toggle('on', c.archived); snapshot(); refresh();
      toast(c.archived ? 'Cliente arquivado' : 'Cliente restaurado');
    } break;
    case 'del': if (c) confirmDialog('Confirmar Exclusão', 'Tem certeza que deseja apagar este cliente? Esta ação não pode ser desfeita.', () => {
      db.clients = db.clients.filter(x => x.id !== c.id);
      log('Cliente Excluído', `${c.name} - ${planOf(c) ? planOf(c).name : 'Sem plano'}`, { type: 'delete', client: c });
      snapshot(); refresh(); toast('Cliente apagado');
    }); break;
    case 'more': state.show = state.show ? state.show + 50 : 0; render(); break;
    case 'dd': {
      const k = el.dataset.k;
      const opts = { fStatus: STATUS_OPTS, fType: TYPE_OPTS, fPlan: planOpts(), show: [['20', '20'], ['50', '50'], ['100', '100'], ['0', 'Todos']], period: PERIODS,
        lucroMonth: monthOpts(), lateType: [['todos', 'Todos os planos']].concat(db.plans.map(p => [p.id, p.name])) }[k];
      dropdown(el, opts, state[k], v => { state[k] = k === 'show' ? +v : v; render(); });
      break;
    }
    case 'set': state[el.dataset.k] = el.dataset.v; render(); break;
    case 'mon': {
      const k = el.dataset.k, next = shiftMonth(state[k], +el.dataset.v);
      if (next <= monKey(today())) { state[k] = next; render(); }
      break;
    }
    case 'pdf': pdf(); break;
    case 'history': historyPop(); break;
    case 'notifications': notificationsPop(); break;
    case 'profile': profileSheet(); break;
    case 'plans': plansSheet(); break;
    case 'plan-edit': planForm(db.plans.find(p => p.id === id)); break;
    case 'plan-del': {
      const p = db.plans.find(x => x.id === id), n = db.clients.filter(x => x.planId === id).length;
      confirmDialog('Excluir plano', n ? `${n} clientes usam "${esc(p.name)}" e ficarão sem plano. Continuar?` : `Excluir "${esc(p.name)}"?`, () => {
        db.plans = db.plans.filter(x => x.id !== id); db.clients.forEach(x => { if (x.planId === id) x.planId = ''; });
        save(); closeTop(); plansSheet(); render();
      });
      break;
    }
    case 'template': templateSheet(); break;
    case 'bulk': bulkSheet(); break;
    case 'export': exportData(); break;
    case 'import': $('#fileIn').click(); break;
    case 'demo': demoData(); break;
    case 'wipe': confirmDialog('Apagar tudo', 'Todos os clientes, pagamentos e histórico serão removidos deste aparelho.', () => {
      const keep = { plans: db.plans, settings: db.settings }; db = Object.assign(blank(), keep); refresh(); toast('Dados apagados');
    }); break;
  }
});

/* ---------- Start ---------- */
load();
snapshot(); save();
paintIcons();
go('dashboard');
// Day rollover while the app stays open.
setInterval(() => { const t = today(); if (t !== go.day) { go.day = t; snapshot(); save(); render(); } }, 60000);
go.day = today();
