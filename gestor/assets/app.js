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
  wa: '<path fill="currentColor" stroke="none" d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413Z"/>',
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
  arrowL: '<line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>',
  chart: '<path d="M3 3v18h18"/><path d="M8 17v-5"/><path d="M13 17V7"/><path d="M18 17v-8"/>',
  camera: '<path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/>',
  save: '<path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/>',
  clock: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
  info: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>',
  drive: '<line x1="22" y1="12" x2="2" y2="12"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/><line x1="6" y1="16" x2="6.01" y2="16"/><line x1="10" y1="16" x2="10.01" y2="16"/>',
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  copy: '<rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>',
  link: '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>',
  trendDown: '<polyline points="22 17 13.5 8.5 8.5 13.5 2 7"/><polyline points="16 17 22 17 22 11"/>',
  clipboard: '<rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><path d="m9 14 2 2 4-4"/>',
  moon: '<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>',
  up: '<polyline points="18 15 12 9 6 15"/>',
  cloud: '<path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/>',
  eye: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z"/>',
  send: '<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
};
const ic = (n, extra) => {
  const fill = extra && extra.includes('class="fill"');
  return `<i data-i="${n}" data-p="1"${extra ? ' ' + extra : ''}><svg viewBox="0 0 24 24" fill="${fill ? 'currentColor' : 'none'}"><use href="#i-${n}"/></svg></i>`;
};
(function sprite() {
  const d = document.createElement('div');
  d.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" style="display:none">${Object.keys(ICONS).map(k => `<symbol id="i-${k}" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${ICONS[k]}</symbol>`).join('')}</svg>`;
  document.body.prepend(d.firstChild);
})();
// Icons written directly in index.html.
function paintIcons(root) {
  (root || document).querySelectorAll('i[data-i]:not([data-p])').forEach(el => { el.outerHTML = ic(el.dataset.i, el.className ? `class="${el.className}"` : ''); });
}

/* ---------- Helpers ---------- */
const $ = s => document.querySelector(s);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
const pad = n => String(n).padStart(2, '0');
const iso = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const parse = s => { const [y, m, d] = s.slice(0, 10).split('-').map(Number); return new Date(y, m - 1, d); };
let _today = '', _todayAt = 0;
const today = () => { const n = Date.now(); if (n - _todayAt > 20000) { _today = iso(new Date()); _todayAt = n; } return _today; };
const _dn = new Map();
const dayNum = s => { s = s.slice(0, 10); let v = _dn.get(s); if (v === undefined) { const [y, m, d] = s.split('-').map(Number); v = Math.round(Date.UTC(y, m - 1, d) / 864e5); _dn.set(s, v); } return v; };
const addDays = (s, n) => { const d = parse(s); d.setDate(d.getDate() + n); return iso(d); };
function addMonths(s, n) {
  const d = parse(s), day = d.getDate();
  d.setDate(1); d.setMonth(d.getMonth() + n);
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(day, last));
  return iso(d);
}
const diffDays = (a, b) => dayNum(a) - dayNum(b);
const br = s => s ? s.slice(0, 10).split('-').reverse().join('/') : '—';
const brShort = s => br(s).slice(0, 5);
const money = v => 'R$ ' + (Number(v) || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const MONTHS = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
const monKey = s => s.slice(0, 7);
const monLabel = (key, short) => {
  const [y, m] = key.split('-').map(Number);
  return short ? `${MONTHS[m - 1].slice(0, 3)}/${String(y).slice(2)}` : `${MONTHS[m - 1]} de ${y}`;
};
const capFirst = s => s.charAt(0).toUpperCase() + s.slice(1);
const cap = s => s.replace(/(^|\s)\S/g, c => c.toUpperCase());
const shiftMonth = (key, n) => addMonths(key + '-01', n).slice(0, 7);
const nowStamp = () => { const d = new Date(); return `${iso(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`; };
// Phones are stored as digits with country code (Brazilian numbers always start with 55).
function fmtPhone(p) {
  const d = String(p || '').replace(/\D/g, '');
  if (!d) return '';
  if (d.startsWith('55') && (d.length === 12 || d.length === 13)) {
    const n = d.slice(4);
    return `+55 ${d.slice(2, 4)} ${n.slice(0, n.length - 4)}-${n.slice(-4)}`;
  }
  return '+' + d;
}
function normPhone(p) {
  const raw = String(p || '').trim();
  let d = raw.replace(/\D/g, '');
  if (!raw.startsWith('+') && (d.length === 10 || d.length === 11)) d = '55' + d;
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
const LEGACY_KEY = 'xplay-gestor-v1';
let KEY = LEGACY_KEY;   // becomes per account after login (cloud.js)
let db;
function blank() {
  return {
    plans: [],
    clients: [], payments: [], history: [], snapshots: {},
    settings: {
      brand: 'GS', company: '', owner: '', phone: '', photo: '',
      msgWhats: 'Bom dia, tudo bem? 😊\n\n🕗 Estou passando para lembrar que seu plano {{plano}} vence em {{vencimento}}.\n\n💰 Valor: {{valor}}\n\nPara renovar é só responder esta mensagem. {empresa} agradece! 🙏',
      msgCadastro: '🛍️ Seu cadastro foi realizado com sucesso!\n🕒 Vencimento: {{vencimento}}\n📦 Plano: {{plano}}\n📌 Caso tenha alguma dúvida ou problema estamos à disposição.',
      msgRenovacao: '✅ Renovação confirmada!\n📦 Plano: {{plano}}\n🕒 Novo vencimento: {{vencimento}}\nObrigado pela preferência! 🙏',
    },
  };
}
function load() {
  try { db = JSON.parse(localStorage.getItem(KEY)); } catch (e) { db = null; }
  if (!db || !db.clients) db = blank();
  db.snapshots = db.snapshots || {};
  migrate();
}
// Example plans that older versions put in every new account; dropped while unused and unchanged.
const OLD_SAMPLE_PLANS = [['p1', 'XPRIMETV MENSAL', 34.89, 7.25], ['p2', 'XPRIMETV TRIMESTRAL', 94.9, 21.75], ['p3', 'XPRIMETV ANUAL', 349.9, 87]];
function migrate() {
  if (db.plans && db.clients) {
    const used = new Set(db.clients.map(c => c.planId));
    db.plans = db.plans.filter(p => !OLD_SAMPLE_PLANS.some(([id, n, pr, co]) => p.id === id && p.name === n && +p.price === pr && +p.cost === co && !used.has(p.id)));
  }
  db.snapshots = db.snapshots || {}; db.history = db.history || []; db.payments = db.payments || [];
  const old = db.settings || {};
  db.settings = Object.assign(blank().settings, old);
  if (old.template && !old.msgWhats) db.settings.msgWhats = old.template;
  if (db.settings.brand === 'xplay') db.settings.brand = 'GS';
  delete db.settings.template;
  const used = new Set();
  db.plans.forEach(p => {
    if (!p.code || used.has(String(p.code))) { let c; do { c = String(1000 + Math.floor(Math.random() * 9000)); } while (used.has(c)); p.code = c; }
    used.add(String(p.code)); p.color = p.color || '#E3F2FD';
  });
}
/* Lookup tables rebuilt lazily after any change (save() drops them). */
let IDX = null;
function idx() {
  if (IDX) return IDX;
  const plans = {}, paid = new Set(), byMonth = {}, use = {};
  db.plans.forEach(p => { plans[p.id] = p; });
  db.clients.forEach(c => { if (!c.archived && c.planId) use[c.planId] = (use[c.planId] || 0) + 1; });
  db.payments.forEach(p => { paid.add(p.clientId); const k = p.date.slice(0, 7); (byMonth[k] || (byMonth[k] = [])).push(p); });
  return (IDX = { plans, paid, byMonth, use });
}
const invalidate = () => { IDX = null; };
const plansByUse = () => { const u = idx().use; return db.plans.slice().sort((a, b) => (u[b.id] || 0) - (u[a.id] || 0) || a.name.localeCompare(b.name)); };
const paysIn = k => idx().byMonth[k] || [];
// Writing ~2 MB to localStorage takes a few ms, so it runs after the UI has updated.
let saveT = 0;
function save() { invalidate(); clearTimeout(saveT); saveT = setTimeout(persist, 350); }
function persist() {
  clearTimeout(saveT); saveT = 0;
  const str = JSON.stringify(db);
  if (str === localStorage.getItem(KEY)) return;   // nothing changed: no upload
  try { localStorage.setItem(KEY, str); } catch (e) { toast('Não foi possível salvar: ' + e.message); }
  if (window.onPersist) onPersist();
}
window.flushSave = () => { if (saveT) persist(); };
document.addEventListener('visibilitychange', () => { if (document.hidden) flushSave(); });
addEventListener('pagehide', () => flushSave());
const planOf = c => idx().plans[c.planId];
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
  ferramentas: ['Configurações', 'Configurações do sistema'],
};
const state = {
  tab: 'dashboard',
  q: '', fStatus: 'todos', fType: 'todos', fPlan: 'todos', show: 20,
  period: 'hoje', from: '', to: '',
  lucroMonth: monKey(today()), lucroMode: 'mes',
  dayMonth: monKey(today()), renMonth: monKey(today()), newMonth: monKey(today()), avgMonth: monKey(today()),
  lateType: 'todos', sub: null, planQ: '', fmtClients: 'pdf', fmtFin: 'pdf',
  finMonth: monKey(today()), anchor: monKey(today()), cmpA: monKey(today()), cmpB: shiftMonth(monKey(today()), -1), cmpDay: '0',
};
function go(tab, keepScroll) {
  if (state.sel) selEnd();
  state.tab = tab;
  if (tab === 'ferramentas' && state.sub) { document.querySelectorAll('.tabs button').forEach(b => b.classList.toggle('on', b.dataset.tab === tab)); return setSub(null); }
  document.querySelectorAll('.tabs button').forEach(b => b.classList.toggle('on', b.dataset.tab === tab));
  $('#pageTitle').textContent = PAGES[tab][0];
  $('#pageSub').textContent = PAGES[tab][1];
  $('#fab').hidden = !(tab === 'dashboard' || tab === 'clientes');
  $('#fab').dataset.act = 'new-client';
  render();
  const pg = $('#page'); pg.classList.remove('enter'); void pg.offsetWidth; pg.classList.add('enter');
  if (!keepScroll) window.scrollTo(0, 0);
}
function updateHeader() {
  const due = notifItems().filter(n => !n.read).length;
  const dot = $('#bellDot'); dot.hidden = !due; dot.textContent = due > 99 ? '99+' : due;
}
function render() {
  for (const k in CH) delete CH[k];
  PIE.sel = -1;
  const tip = $('#ctip'); if (tip) tip.hidden = true;
  updateHeader();
  const page = $('#page');
  page.innerHTML = VIEWS[state.tab]();
  if (VIEWS[state.tab + 'After']) VIEWS[state.tab + 'After']();
}
// Re-draws only what depends on data: the list on Clientes, the whole page elsewhere.
function rerender() {
  if (state.tab === 'clientes' && $('#clist')) { updateHeader(); renderList(true); } else render();
}
const refresh = () => { save(); rerender(); };

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
// "Lucas De Oliveira Santos" -> "Lucas O." (skips de/da/do/dos/das/e).
function shortName(n) {
  const w = String(n || '').trim().split(/\s+/).filter(Boolean);
  if (w.length < 2) return w[0] || '';
  const sur = w.slice(1).find(x => !/^(d[aeo]s?|e)$/i.test(x)) || w[1];
  const first = w[0].charAt(0).toUpperCase() + w[0].slice(1).toLowerCase();
  return `${first} ${sur.charAt(0).toUpperCase()}.`;
}
const initials = n => { const w = String(n || '?').trim().split(/\s+/).filter(x => !/^(d[aeo]s?|e)$/i.test(x)); return esc(((w[0] || '?')[0] + (w.length > 1 ? w[w.length - 1][0] : '')).toUpperCase()); };
const initial = n => esc((n || '?').trim().charAt(0).toUpperCase());
function waLink(c, text) {
  const d = String(c.phone || '').replace(/\D/g, '');
  return `https://wa.me/${d}${text ? '?text=' + encodeURIComponent(text) : ''}`;
}
const waMessage = c => fillMsg(db.settings.msgWhats, c);

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
      <div class="nm"><b>${esc(shortName(c.name))}</b><span>${date}</span></div>
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
      ${ess.slice(0, 3).map(c => row(c, br(c.due), ['#f6b73c', '#ef8a2c'])).join('') || '<div class="empty" style="padding:20px">Marque clientes com a ⭐ para vê-los aqui</div>'}
    </div>
    <div class="card list-card" style="background:linear-gradient(180deg,#f5f5ff,#fff 70px)">
      <div class="lc-head"><div class="ico" style="background:linear-gradient(135deg,#6366f1,#9333ea)">${ic('calendar')}</div>
        <div><b>Próx. Vencimentos</b><small>${upcoming.length} em 5 dias</small></div>
        <button class="see" style="color:#4f46e5" data-act="filter" data-f="p5h">Ver Todos ${ic('arrowR')}</button></div>
      ${upcoming.slice(0, 3).map(c => row(c, brShort(c.due), ['#6366f1', '#9333ea'])).join('') || '<div class="empty" style="padding:20px">Nenhum vencimento nos próximos 5 dias</div>'}
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
const planOpts = () => [['todos', 'Todos']].concat(plansByUse().map(p => [p.id, p.name]));

VIEWS.clientes = () => {
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
      <span class="chip">${ic('users')} <span id="cnum"></span></span>
      <button class="sel-btn" data-act="sel-start">${ic('checkc')} Selecionar</button>
      <button class="dd" data-act="dd" data-k="show"><span>${state.show || 'Todos'}</span>${ic('down')}</button>
    </div>
    <div id="clist"></div><div id="cmore"></div>`;
};
function clientCard(c) {
  const p = planOf(c), st = statusOf(c);
  const hasPays = idx().paid.has(c.id);
  return `<div class="card client st-${c.archived ? 'n' : st.cls}${state.sel && state.sel.has(c.id) ? ' picked' : ''}" data-act="detail" data-id="${c.id}"><i class="pick">${ic('check')}</i>
    <div class="head">
      <div class="ini">${initials(c.name)}</div>
      <div class="who"><b><span class="n">${esc(shortName(c.name))}</span>${hasPays ? `<button class="hist" data-act="pays" data-id="${c.id}" aria-label="Pagamentos">${ic('history')}</button>` : ''}</b>
        <span>${p ? esc(p.name) : 'Sem plano'}${c.qty > 1 ? ` · ${c.qty} telas` : ''}</span></div>
      <span class="badge">${c.archived ? '📦 Arquivado' : st.text}</span>
    </div>
    <div class="meta"><span>${ic('calendar')} ${br(c.due)}</span><b>${p ? money(priceOf(c)) : '—'}</b></div>
    <div class="actions">
      <button class="btn-wa" data-act="wa" data-id="${c.id}">${ic('wa')} WhatsApp</button>
      ${starBtn(c)}
      <button class="sq" data-act="edit" data-id="${c.id}" aria-label="Editar">${ic('edit')}</button>
      <button class="sq renew" data-act="renew" data-id="${c.id}" aria-label="Renovar">${ic('renew')}</button>
      <button class="sq del" data-act="del" data-id="${c.id}" aria-label="Excluir">${ic('trash')}</button>
    </div></div>`;
}
const starBtn = c => `<button class="sq star ${c.essential ? 'on' : ''}" data-act="star" data-id="${c.id}">${ic('star', c.essential ? 'class="fill"' : '')}</button>`;
/* Fills #clist: the first cards at once, the rest a frame at a time so taps stay responsive. */
function renderList(keep) {
  const box = $('#clist'); if (!box) return;
  const list = filteredClients(), tok = renderList.tok = (renderList.tok || 0) + 1;
  state.list = list;
  $('#cnum').textContent = `${list.length} ${list.length === 1 ? 'cliente' : 'clientes'}`;
  const want = state.show === 0 ? list.length : Math.min(state.show, list.length);
  const keepN = keep ? Math.min(+box.dataset.n || 0, list.length) : 0;
  box.innerHTML = ''; box.dataset.n = 0;
  if (!list.length) { box.innerHTML = `<div class="card empty">${ic('users')}<div>Nenhum cliente encontrado</div></div>`; $('#cmore').innerHTML = ''; return; }
  appendCards(Math.max(keepN, Math.min(want, 20)), tok, want);
}
function appendCards(upto, tok, want) {
  const box = $('#clist'), list = state.list;
  if (!box || tok !== renderList.tok) return;
  const from = +box.dataset.n || 0, to = Math.min(upto, list.length);
  if (to > from) { box.insertAdjacentHTML('beforeend', list.slice(from, to).map(clientCard).join('')); box.dataset.n = to; if (state.sel) selPaint(); }
  if (to < want) requestAnimationFrame(() => appendCards(to + 60, tok, want));
  else {
    const rest = list.length - to;
    $('#cmore').innerHTML = rest > 0 ? `<button class="card more" data-act="more">Mostrar mais (${rest} restantes)</button>` : '';
  }
}
VIEWS.clientesAfter = () => {
  const q = $('#q');
  q.addEventListener('input', () => { state.q = q.value; clearTimeout(q.t); q.t = setTimeout(() => renderList(), 140); });
  renderList();
};

/* ----- Finance ----- */
const PERIODS = [['hoje', 'Hoje'], ['ontem', 'Ontem'], ['semana', 'Semana'], ['mes', 'Mês'], ['tudo', 'Todo período'], ['custom', 'Selecionar Período']];
function periodRange() {
  const t = today();
  switch (state.period) {
    case 'hoje': return [t, t];
    case 'ontem': { const y = addDays(t, -1); return [y, y]; }
    case 'semana': return [addDays(t, -6), t];
    case 'mes': { const k = state.finMonth, end = iso(new Date(+k.slice(0, 4), +k.slice(5), 0)); return [k + '-01', end < t ? end : t]; }
    case 'tudo': return ['0000-01-01', '9999-12-31'];
    case 'custom': return [state.from || '0000-01-01', state.to || '9999-12-31'];
  }
}
const inRange = (p, r) => p.date.slice(0, 10) >= r[0] && p.date.slice(0, 10) <= r[1];
function periodShort() {
  if (state.period === 'mes') return cap(MONTHS[+state.finMonth.slice(5) - 1]) + (state.finMonth.slice(0, 4) !== today().slice(0, 4) ? ' ' + state.finMonth.slice(0, 4) : '');
  if (state.period !== 'custom') return optLabel(PERIODS, state.period);
  return state.from || state.to ? `${state.from ? brShort(state.from) : '…'}–${state.to ? brShort(state.to) : '…'}` : 'Período';
}
/* Every month-based card follows the period picked at the top of Financeiro. */
function syncMonths() {
  const r = periodRange(), cur = monKey(today());
  let m = state.period === 'tudo' ? cur : monKey(r[1] > today() ? today() : r[1]);
  if (m > cur) m = cur;
  Object.assign(state, { anchor: m, lucroMonth: m, dayMonth: m, renMonth: m, newMonth: m, avgMonth: m, cmpA: m, cmpB: shiftMonth(m, -1) });
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
      ${state.period === 'mes' ? `<div style="height:12px"></div><button class="dd full alt" data-act="dd" data-k="finMonth"><span>${monLabel(state.finMonth)}</span>${ic('down')}</button>` : ''}
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
    <div class="fp-bar"><b>Painéis</b><button data-act="fp-all" data-v="1">${ic('down')} Expandir tudo</button><button data-act="fp-all" data-v="0">${ic('up')} Minimizar tudo</button></div>
    <div id="fpanels">
    ${lucroCard()}
    ${evolucaoCard()}
    ${receitaPlanoCard()}
    ${historicoAtivosCard()}
    ${porDiaCard()}
    ${countCard('renMonth', 'refresh', '#e8f7f1', '#2f7d6a', 'Planos Renovados', 'planos renovados', p => p.kind === 'renovacao')}
    ${countCard('newMonth', 'userplus', '#ebf2ff', '#2f5fd0', 'Clientes Novos', 'clientes adicionados', p => p.kind === 'cadastro')}
    ${mediaCard()}
    ${naoRenovaramCard()}
    ${estadosCard()}
    ${comparadorCard()}
    </div>`;
};
VIEWS.financeiroAfter = () => {
  const f = $('#pFrom'), t = $('#pTo');
  if (f) f.onchange = () => { state.from = f.value; syncMonths(); render(); };
  if (t) t.onchange = () => { state.to = t.value; syncMonths(); render(); };
  setupPanels();
};
/* Every finance panel gets the same header with a collapse toggle; the choice is remembered. */
const panelKey = card => (card.querySelector('.card-head h3') || {}).textContent.trim().replace(/\s+/g, ' ');
function setupPanels() {
  const closed = db.settings.finClosed || (db.settings.finClosed = {});
  document.querySelectorAll('#fpanels > .card').forEach(card => {
    const head = card.querySelector('.card-head'); if (!head) return;
    const key = panelKey(card);
    const body = document.createElement('div'); body.className = 'pb';
    const inner = document.createElement('div'); inner.className = 'pbi';
    while (head.nextSibling) inner.appendChild(head.nextSibling);
    body.appendChild(inner); card.appendChild(body);
    card.classList.add('panel');
    if (!head.querySelector('.grow')) { const h3 = head.querySelector('h3'); const g = document.createElement('div'); g.className = 'grow'; h3.replaceWith(g); g.appendChild(h3); }
    const btn = document.createElement('button');
    btn.className = 'pt'; btn.setAttribute('aria-label', 'Expandir ou minimizar'); btn.innerHTML = ic('down');
    head.appendChild(btn);
    card.classList.toggle('closed', !!closed[key]);
    head.addEventListener('click', e => {
      if (e.target.closest('[data-act]')) return;
      const c = card.classList.toggle('closed');
      if (c) closed[key] = 1; else delete closed[key];
      save();
    });
  });
}

function monthOpts() {
  const keys = new Set([monKey(today())]);
  Object.keys(idx().byMonth).forEach(k => keys.add(k));
  return [...keys].sort().reverse().map(k => [k, `${MONTHS[+k.slice(5) - 1].slice(0, 3)} de ${k.slice(0, 4)}`]);
}

function lucroCard() {
  const mode = state.lucroMode, key = state.lucroMonth;
  let labels = [], vals = [];
  if (mode === 'mes') {
    const [y, m] = key.split('-').map(Number), n = new Date(y, m, 0).getDate();
    const byDay = Array(n).fill(0);
    paysIn(key).forEach(p => { byDay[+p.date.slice(8, 10) - 1] += p.amount - (p.cost || 0); });
    vals = byDay; labels = byDay.map((_, i) => i + 1);
  } else if (mode === 'sem') {
    const end = key === monKey(today()) ? today() : iso(new Date(+key.slice(0, 4), +key.slice(5), 0));
    for (let i = 6; i >= 0; i--) {
      const d = addDays(end, -i);
      labels.push(brShort(d));
      vals.push(sumBy(paysIn(d.slice(0, 7)).filter(p => p.date.slice(0, 10) === d)).prof);
    }
  } else {
    const y = key.slice(0, 4);
    for (let m = 1; m <= 12; m++) {
      const k = `${y}-${pad(m)}`;
      labels.push(MONTHS[m - 1].slice(0, 3));
      vals.push(sumBy(paysIn(k)).prof);
    }
  }
  let slots = labels.length, tips = labels.slice();
  if (mode === 'mes') {
    tips = labels.map(d => `${pad(d)}/${key.slice(5)}`);
    if (key === monKey(today())) vals = vals.slice(0, +today().slice(8));   // no flat line for days still ahead
  }
  if (mode === 'ano') tips = labels.map(l => `${cap(l)} ${key.slice(0, 4)}`);
  const xl = labels.map((l, i) => mode !== 'mes' || i === 0 || ((i + 1) % 5 === 0 && i < labels.length - 3) || i === labels.length - 1 ? l : '');
  const tot = vals.reduce((a, b) => a + b, 0);
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#f5f0ff;color:#7c3aed">${ic('dollar')}</div><h3>Análise de Lucro</h3></div>
    <div class="chart-ctl">
      <button class="dd" data-act="dd" data-k="lucroMonth"><span>${optLabel(monthOpts(), key)}</span>${ic('down')}</button>
      <div class="seg">${[['sem', 'Sem.'], ['mes', 'Mês'], ['ano', 'Ano']].map(([k, l]) => `<button class="${mode === k ? 'on' : ''}" data-act="set" data-k="lucroMode" data-v="${k}">${l}</button>`).join('')}</div>
    </div>
    <div class="chart-sum"><span>Lucro no ${mode === 'sem' ? 'período' : mode === 'mes' ? 'mês' : 'ano'}</span><b>${money(tot)}</b></div>
    ${lineChart([{ name: 'Lucro', vals, color: '#7c3aed' }], xl, { fmt: kFmt, vfmt: money, tips, slots, peak: true })}
  </div>`;
}

function evolucaoCard() {
  const keys = []; for (let i = 5; i >= 0; i--) keys.push(shiftMonth(state.anchor, -i));
  const rev = [], prof = [];
  keys.forEach(k => { const s = sumBy(paysIn(k)); rev.push(s.rev); prof.push(s.prof); });
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#eef4ff;color:#2f6ae0">${ic('trending')}</div><div class="grow"><h3>Evolução Financeira</h3><small>Últimos 6 meses</small></div></div>
    <div class="legend line"><span><i class="swc" style="background:#3b82f6"></i>Faturamento</span><span><i class="swc" style="background:#16a34a"></i>Lucro</span></div>
    ${lineChart([{ name: 'Faturamento', vals: rev, color: '#3b82f6' }, { name: 'Lucro', vals: prof, color: '#16a34a' }],
      keys.map(k => monLabel(k, true)), { fmt: v => kFmt(v), vfmt: money, tips: keys.map(k => capFirst(monLabel(k))), left: 36 })}
  </div>`;
}

const PIE_COLORS = ['#4f86e8', '#43a35a', '#ea7d32', '#9b5cf0', '#e0559a', '#4ab0cc', '#8b5cf6', '#e8a33a'];
function receitaPlanoCard() {
  const r = periodRange(), map = {}, all = new Set();
  db.payments.forEach(p => {
    if (!inRange(p, r)) return;
    const k = p.planName || 'Sem plano';
    const m = map[k] || (map[k] = { name: k, ids: new Set(), v: 0, l: 0 });
    m.ids.add(p.clientId); all.add(p.clientId); m.v += p.amount; m.l += p.amount - (p.cost || 0);
  });
  const order = {}; db.plans.forEach((p, i) => { order[p.name] = i; });
  const rows = Object.values(map).map(m => ({ name: m.name, n: m.ids.size, v: m.v, l: m.l }))
    .sort((a, b) => b.n - a.n || b.v - a.v);
  const total = rows.reduce((a, x) => a + x.n, 0);
  if (rows.length > 8) {
    const rest = rows.splice(7);
    rows.push({ name: `Outros (${rest.length} planos)`, n: rest.reduce((a, x) => a + x.n, 0), v: rest.reduce((a, x) => a + x.v, 0), l: rest.reduce((a, x) => a + x.l, 0), other: true });
  }
  rows.forEach((x, i) => { x.color = x.other ? '#b8bdc9' : PIE_COLORS[i % PIE_COLORS.length]; });
  PIE.rows = rows; PIE.total = total;
  if (!rows.length) return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#f5f0ff;color:#7c3aed">${ic('box')}</div><h3>Receita por Plano</h3></div>
    <div class="empty">${ic('box')}<div>Sem dados de faturamento</div></div></div>`;
  // Pie slices
  const C = 110, R = 100;
  let a0 = -Math.PI / 2, paths = '';
  rows.forEach((x, i) => {
    const ang = x.n / total * Math.PI * 2, a1 = a0 + ang;
    const p0 = [C + R * Math.cos(a0), C + R * Math.sin(a0)], p1 = [C + R * Math.cos(a1), C + R * Math.sin(a1)];
    const d = rows.length === 1 ? `M${C - R},${C}a${R},${R} 0 1,0 ${2 * R},0a${R},${R} 0 1,0 ${-2 * R},0Z`
      : `M${C},${C}L${p0[0].toFixed(2)},${p0[1].toFixed(2)}A${R},${R} 0 ${ang > Math.PI ? 1 : 0},1 ${p1[0].toFixed(2)},${p1[1].toFixed(2)}Z`;
    paths += `<path data-pi="${i}" d="${d}" fill="${x.color}" stroke="#fff" stroke-width="1.5"/>`;
    a0 = a1;
  });
  return `<div class="card pie-card">
    <div class="card-head"><div class="ico" style="background:#f5f0ff;color:#7c3aed">${ic('box')}</div><h3>Receita por Plano</h3></div>
    <p class="pie-tot">Total: <b>${total} clientes ativos</b></p>
    <div class="pie-wrap"><svg class="pie" viewBox="0 0 220 220">${paths}</svg><div class="pie-info" id="pieInfo" hidden></div></div>
    <div class="pie-leg">${rows.map((x, i) => `<button data-pi="${i}"><i style="background:${x.color}"></i><span>${esc(x.name)}</span><b>${x.n}</b></button>`).join('')}</div>
  </div>`;
}
const PIE = { rows: [], total: 0, sel: -1 };
function pieSelect(i) {
  const box = $('#pieInfo'); if (!box) return;
  PIE.sel = i;
  document.querySelectorAll('.pie path').forEach(p => p.classList.toggle('dim', i >= 0 && +p.dataset.pi !== i));
  document.querySelectorAll('.pie-leg button').forEach(b => b.classList.toggle('on', +b.dataset.pi === i));
  if (i < 0) { box.hidden = true; return; }
  const x = PIE.rows[i];
  box.hidden = false;
  box.innerHTML = `<b class="pn">${esc(x.name)}</b><b class="pc" style="color:${x.color}">${x.n} ${x.n === 1 ? 'cliente' : 'clientes'}</b>
    <span class="pp">${(x.n / PIE.total * 100).toFixed(1)}% do total</span><hr><span class="pr">Receita: ${money(x.v)}</span><span class="pl">Lucro: ${money(x.l)}</span>`;
}
document.addEventListener('click', e => {
  const t = e.target.closest('[data-pi]');
  if (t) { const i = +t.dataset.pi; pieSelect(PIE.sel === i ? -1 : i); }
  else if (PIE.sel >= 0 && !e.target.closest('.pie-info')) pieSelect(-1);
});

function historicoAtivosCard() {
  const cutoff = addDays(today(), -29);
  const keys = Object.keys(db.snapshots).filter(k => k >= cutoff).sort().slice(-10);
  let body;
  if (!keys.length) body = `<div class="empty">${ic('users')}<div>O histórico começa a ser registrado hoje</div></div>`;
  else {
    const vals = keys.map(k => db.snapshots[k]);
    const all = Object.keys(db.snapshots).sort(), j = all.indexOf(keys[0]);
    body = columnChart(vals, keys.map(brShort), { color: '#6d5dfc', soft: '#d9d5fd', tips: keys.map(k => br(k)), before: j > 0 ? db.snapshots[all[j - 1]] : null });
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
  paysIn(key).forEach(p => { const d = +p.date.slice(8, 10) - 1; if (d >= last) return; (p.kind === 'cadastro' ? cad : ren)[d]++; });
  const R = ren.reduce((a, b) => a + b, 0), C = cad.reduce((a, b) => a + b, 0);
  let peak = -1, pv = 0; ren.forEach((v, i) => { if (v + cad[i] > pv) { pv = v + cad[i]; peak = i; } });
  const mini = (icon, bg, col, v, l, hl) => `<div class="mini ${hl ? 'hl' : ''}"><div class="ic" style="background:${bg};color:${col}">${ic(icon)}</div><div><b>${v}</b><span>${l}</span></div></div>`;
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:#eef0ff;color:#4f46e5">${ic('caldays')}</div><div class="grow"><h3>Clientes por Dia</h3><small>${cap(monLabel(key))}</small></div>${monthNav('dayMonth')}</div>
    <div class="minis">
      ${mini('usercheck', '#eeeefe', '#5b5bd6', R, 'Renovados')}${mini('userplus', '#e8f7ec', '#3fae56', C, 'Cadastrados')}
      ${mini('users', '#fff4e0', '#e0a526', R + C, 'Total no mês')}${mini('calendar', '#fde9d9', '#e07a2c', peak >= 0 ? `${pad(peak + 1)}/${pad(m)}` : '—', 'Pico do mês', true)}
    </div>
    <div class="legend sq" style="margin:0 0 6px"><span><i class="swc" style="background:#6366f1"></i>Renovados</span><span><i class="swc" style="background:#16a34a"></i>Cadastrados</span></div>
    ${stackChart(ren, cad, ren.map((_, i) => i % 5 === 0 ? `${pad(i + 1)}/${pad(m)}` : ''), { colors: ['#6366f1', '#16a34a'], names: ['Renovados', 'Cadastrados'], tips: ren.map((_, i) => `${pad(i + 1)}/${pad(m)}/${y}`) })}
  </div>`;
}
function countCard(k, icon, bg, col, title, sub, pred) {
  const n = paysIn(state[k]).filter(pred).length;
  return `<div class="card">
    <div class="card-head"><div class="ico" style="background:${bg};color:${col}">${ic(icon)}</div><div class="grow"><h3>${title}</h3><small>${cap(monLabel(state[k]))}</small></div>${monthNav(k)}</div>
    <div class="big" style="color:${col}">${n}</div><p>${sub}</p></div>`;
}
function mediaCard() {
  const key = state.avgMonth, [y, m] = key.split('-').map(Number);
  const days = key === monKey(today()) ? +today().slice(8) : new Date(y, m, 0).getDate();
  const n = paysIn(key).length;
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
    <div class="types">${Object.keys(by).map(t => `<button class="type ${by[t] ? '' : 'zero'}" data-act="late-type" data-v="${t}"><div class="h"><i class="swc" style="background:${colors[t][0]}"></i>${TYPE_NAMES[t]}</div>
      <b style="${by[t] ? `color:${colors[t][1]}` : ''}">${by[t]}</b><small>${by[t] ? 'não renovaram' : 'em dia'}</small></button>`).join('')}</div>
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
  const d = String(c.phone || '').replace(/\D/g, '');
  return d.startsWith('55') && d.length >= 11 ? DDD[d.slice(2, 4)] || null : null;
}
const UF_NAMES = {}; BR_MAP.loc.forEach(l => { UF_NAMES[l.id] = l.name; });
// Teal -> green -> yellow -> orange -> red by share of the busiest state.
const MAP_STOPS = [[0, [63, 147, 168]], [.3, [92, 186, 110]], [.55, [227, 195, 65]], [.78, [236, 154, 60]], [1, [224, 82, 82]]];
function mapColor(t) {
  for (let i = 1; i < MAP_STOPS.length; i++) if (t <= MAP_STOPS[i][0]) {
    const [t0, c0] = MAP_STOPS[i - 1], [t1, c1] = MAP_STOPS[i], k = (t - t0) / (t1 - t0);
    return `rgb(${c0.map((v, j) => Math.round(v + (c1[j] - v) * k)).join(',')})`;
  }
  return 'rgb(224,82,82)';
}
function estadosCard() {
  const cnt = {}; let total = 0;
  db.clients.forEach(c => { const uf = ufOf(c); if (uf) { cnt[uf] = (cnt[uf] || 0) + 1; total++; } });
  const max = Math.max(1, ...Object.values(cnt));
  const col = uf => mapColor((cnt[uf] || 0) / max);
  const paths = BR_MAP.loc.map(l => `<path data-uf="${l.id}" d="${l.path}" fill="${col(l.id)}"/>`).join('');
  const top = Object.entries(cnt).sort((a, b) => b[1] - a[1]).slice(0, 5);
  const noDdd = db.clients.length - total;
  return `<div class="card dark">
    <div class="card-head"><div class="ico">${ic('pin')}</div><h3>Clientes por Estado</h3><span class="tot">Total: <b>${total}</b></span></div>
    <div class="br-map"><svg viewBox="${BR_MAP.viewBox}">${paths}</svg><div class="uf-tip" id="ufTip" hidden></div></div>
    <div class="map-scale"><span>0</span><i></i><span>${max}</span></div>
    <div class="top-uf"><h4>Top Estados</h4><div>${top.map(([uf, n]) => `<div><i style="background:${col(uf)}"></i><span>${UF_NAMES[uf]}</span><b>${n}</b></div>`).join('')}</div>
      ${noDdd ? `<p>${noDdd} cliente(s) sem DDD identificado</p>` : ''}</div>
  </div>`;
}
document.addEventListener('click', e => {
  const tip = $('#ufTip'); if (!tip) return;
  const p = e.target.closest('.br-map path');
  document.querySelectorAll('.br-map path.on').forEach(x => x.classList.remove('on'));
  if (!p) { tip.hidden = true; return; }
  const uf = p.dataset.uf; let n = 0;
  db.clients.forEach(c => { if (ufOf(c) === uf) n++; });
  p.classList.add('on'); p.parentNode.appendChild(p);
  tip.innerHTML = `<b>${UF_NAMES[uf]}</b> ${n} ${n === 1 ? 'cliente' : 'clientes'}`; tip.hidden = false;
});

function comparadorCard() {
  const A = state.cmpA, B = state.cmpB, day = +state.cmpDay;
  const tally = key => { const m = {}; let tot = 0; paysIn(key).forEach(p => {
    if (day && +p.date.slice(8, 10) > day) return;
    const k = p.planName || 'Sem plano'; m[k] = m[k] || { n: 0, v: 0 }; m[k].n++; m[k].v += p.amount; tot++; }); return [m, tot]; };
  const [ma, ta] = tally(A), [mb, tb] = tally(B);
  const names = [...new Set(Object.keys(ma).concat(Object.keys(mb)))].sort((x, y) => ((mb[y] || {}).n || 0) + ((ma[y] || {}).n || 0) - ((ma[x] || {}).n || 0) - ((mb[x] || {}).n || 0));
  const cols = ['#4f86e8', '#43a35a', '#ea7d32', '#9b51e0', '#e0457b', '#0ea5e9', '#d4a017', '#64748b'];
  const mon = k => MONTHS[+k.slice(5) - 1], diff = ta - tb;
  return `<div class="card cmp">
    <div class="card-head"><div class="ico" style="background:#eef4ff;color:#2f6ae0">${ic('box')}</div><h3>Planos Vendidos — Comparador</h3></div>
    <button class="dd full" data-act="dd" data-k="cmpA"><span>${monLabel(A)}</span>${ic('down')}</button>
    <div style="height:10px"></div>
    <button class="dd full alt" data-act="dd" data-k="cmpB"><span>${monLabel(B)}</span>${ic('down')}</button>
    <div class="cmp-day"><span>Comparar até o dia:</span><button class="dd" data-act="dd" data-k="cmpDay"><span>${day ? 'Até o dia ' + day : 'Mês inteiro'}</span>${ic('down')}</button></div>
    <div class="cmp-tot"><div><small><i class="swc" style="background:#3b82f6"></i>${capFirst(mon(A))}</small><b>${ta}</b></div><div><small><i class="swc" style="background:#e8833a"></i>${capFirst(mon(B))}</small><b>${tb}</b></div> <span class="delta ${diff >= 0 ? 'up' : 'down'}">${ic(diff >= 0 ? 'trending' : 'trendDown')} ${diff >= 0 ? '+' : ''}${diff}</span></div>
    ${names.map((n, i) => { const a = ma[n] || { n: 0, v: 0 }, b = mb[n] || { n: 0, v: 0 }, mx = Math.max(a.n, b.n, 1), d = a.n - b.n;
      return `<div class="cmp-row"><div class="cr-h"><i class="swc" style="background:${cols[i % cols.length]}"></i><b>${esc(n)}</b>
        <span class="delta ${d >= 0 ? 'up' : 'down'}">${ic(d >= 0 ? 'trending' : 'trendDown')} ${d >= 0 ? '+' : ''}${d}</span></div>
        <div class="cr-two"><div><div class="cr-l"><span>${cap(mon(A))}</span><b>${a.n}</b></div><div class="tr"><div style="width:${a.n / mx * 100}%;background:#3b82f6"></div></div><small>${money(a.v)}</small></div>
          <div><div class="cr-l"><span>${cap(mon(B))}</span><b>${b.n}</b></div><div class="tr"><div style="width:${b.n / mx * 100}%;background:#e8833a"></div></div><small>${money(b.v)}</small></div></div></div>`; }).join('') || '<div class="empty">Sem vendas nesses meses</div>'}
  </div>`;
}

/* ----- Charts ----- */
/* Shared look: hairline solid grid, 2px lines, ~10% area wash, bars ≤24px with 4px rounded tops,
   2px white gap between stacked parts. Tap/drag on any chart shows a crosshair + tooltip. */
const CH = {}; let chSeq = 0;
const GRID = '#eef0f4', AXIS = '#9aa0ae';
function niceMax(v) {
  if (v <= 0) return 4;
  const e = Math.pow(10, Math.floor(Math.log10(v))), f = v / e;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 4 ? 4 : f <= 5 ? 5 : f <= 8 ? 8 : 10) * e;
}
// Monotone cubic (no overshoot below zero or above the peak).
function monoPath(pts) {
  const n = pts.length;
  if (n < 2) return n ? `M${pts[0][0]},${pts[0][1]}` : '';
  const dx = [], m = [], t = [];
  for (let i = 0; i < n - 1; i++) { dx[i] = pts[i + 1][0] - pts[i][0]; m[i] = (pts[i + 1][1] - pts[i][1]) / dx[i]; }
  t[0] = m[0]; t[n - 1] = m[n - 2];
  for (let i = 1; i < n - 1; i++) t[i] = m[i - 1] * m[i] <= 0 ? 0 : 3 * (dx[i - 1] + dx[i]) / ((2 * dx[i] + dx[i - 1]) / m[i - 1] + (dx[i] + 2 * dx[i - 1]) / m[i]);
  let d = `M${pts[0][0].toFixed(1)},${pts[0][1].toFixed(1)}`;
  for (let i = 0; i < n - 1; i++) {
    const h = dx[i] / 3;
    d += ` C${(pts[i][0] + h).toFixed(1)},${(pts[i][1] + t[i] * h).toFixed(1)} ${(pts[i + 1][0] - h).toFixed(1)},${(pts[i + 1][1] - t[i + 1] * h).toFixed(1)} ${pts[i + 1][0].toFixed(1)},${pts[i + 1][1].toFixed(1)}`;
  }
  return d;
}
const topRound = (x, y, w, h, r) => {
  r = Math.min(r, w / 2, h);
  return `M${x},${y + h}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${y + h}Z`;
};
function yGrid(max, L, W, R, y, fmt) {
  let g = '';
  for (let i = 0; i <= 4; i++) { const v = max * i / 4, yy = y(v).toFixed(1); g += `<line x1="${L}" x2="${W - R}" y1="${yy}" y2="${yy}" stroke="${i ? GRID : '#dfe2e8'}"/><text x="${L - 8}" y="${+yy + 4}" text-anchor="end">${fmt(v)}</text>`; }
  return g;
}
/* series: [{ name, color, vals }]; labels: axis text per slot ('' = none); o: { tips, fmt (axis), vfmt (tooltip), left, slots, peak } */
function lineChart(series, labels, o) {
  const id = 'ch' + (++chSeq), W = 340, H = 210, L = o.left || 40, R = 14, T = 18, B = 28;
  const slots = o.slots || labels.length;
  const max = niceMax(Math.max(0, ...series.flatMap(s => s.vals)));
  const x = i => L + (slots === 1 ? (W - L - R) / 2 : i * (W - L - R) / (slots - 1)), y = v => T + (1 - v / max) * (H - T - B);
  let g = yGrid(max, L, W, R, y, o.fmt);
  labels.forEach((l, i) => { if (l !== '') g += `<text x="${x(i)}" y="${H - 6}" text-anchor="${slots > 8 && i === 0 ? 'start' : slots > 8 && i === slots - 1 ? 'end' : 'middle'}">${l}</text>`; });
  series.forEach(s => {
    const pts = s.vals.map((v, i) => [x(i), y(v)]);
    if (!pts.length) return;
    const path = monoPath(pts), gid = id + 'g' + s.color.slice(1);
    g += `<defs><linearGradient id="${gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${s.color}" stop-opacity=".16"/><stop offset="1" stop-color="${s.color}" stop-opacity=".01"/></linearGradient></defs>`;
    g += `<path d="${path} L${pts[pts.length - 1][0]},${y(0)} L${pts[0][0]},${y(0)}Z" fill="url(#${gid})"/>`;
    g += `<path d="${path}" fill="none" stroke="${s.color}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>`;
    const lp = pts[pts.length - 1];
    g += `<circle cx="${lp[0]}" cy="${lp[1]}" r="4.5" fill="${s.color}" stroke="#fff" stroke-width="2"/>`;
  });
  // Selective labels: the peak of the first series.
  if (o.peak && series[0].vals.length) {
    const v = series[0].vals, i = v.indexOf(Math.max(...v));
    if (v[i] > 0) {
      const px = Math.min(Math.max(x(i), L + 26), W - R - 26);
      g += `<g class="peak"><rect x="${px - 26}" y="${y(v[i]) - 25}" width="52" height="17" rx="8.5" fill="#1f2433"/><text x="${px}" y="${y(v[i]) - 13}" text-anchor="middle">${o.fmt(v[i])}</text></g>`;
    }
  }
  CH[id] = { kind: 'line', series, tips: o.tips || labels, vfmt: o.vfmt || o.fmt, x, y, slots, W, H, T, B, L, R };
  return `<svg class="chart" data-ch="${id}" viewBox="0 0 ${W} ${H}">${g}<g class="hover" visibility="hidden"><line class="xh" y1="${T - 6}" y2="${H - B}" stroke="#c5c9d3" stroke-width="1"/>${series.map(s => `<circle r="5" fill="${s.color}" stroke="#fff" stroke-width="2"/>`).join('')}</g></svg>`;
}
/* Stacked columns: a (bottom) + b (top). */
function stackChart(a, b, labels, o) {
  const id = 'ch' + (++chSeq), W = 340, H = 220, L = 30, R = 6, T = 22, B = 26;
  const tot = a.map((v, i) => v + b[i]), max = niceMax(Math.max(1, ...tot));
  const n = a.length, slot = (W - L - R) / Math.max(n, 1), bw = Math.min(24, Math.max(3, slot - 3)), y = v => T + (1 - v / max) * (H - T - B);
  let g = `<rect class="band" y="${T - 6}" height="${H - B - T + 6}" width="${slot}" rx="4" fill="#6366f1" fill-opacity=".07" visibility="hidden"/>`;
  g += yGrid(max, L, W, R, y, v => String(Math.round(v)));
  const peak = tot.indexOf(Math.max(...tot));
  a.forEach((v, i) => {
    const x0 = L + i * slot + (slot - bw) / 2, base = y(0);
    const hA = base - y(v), hB = y(v) - y(v + b[i]);
    if (v) g += b[i] ? `<rect x="${x0}" y="${y(v)}" width="${bw}" height="${hA}" fill="${o.colors[0]}"/>` : `<path d="${topRound(x0, y(v), bw, hA, 3)}" fill="${o.colors[0]}"/>`;
    if (b[i]) g += `<path d="${topRound(x0, y(v + b[i]), bw, Math.max(0, hB - (v ? 2 : 0)), 3)}" fill="${o.colors[1]}"/>`;
    if (labels[i]) g += `<text x="${x0 + bw / 2}" y="${H - 6}" text-anchor="middle">${labels[i]}</text>`;
    if (i === peak && tot[i]) g += `<text class="val" x="${x0 + bw / 2}" y="${y(tot[i]) - 6}" text-anchor="middle">${tot[i]}</text>`;
  });
  CH[id] = { kind: 'bars', slot, L, n, W, T, H, B, rows: i => [[o.names[0], o.colors[0], a[i]], [o.names[1], o.colors[1], b[i]], ['Total', '', tot[i]]], tips: o.tips, vfmt: v => String(v) };
  return `<svg class="chart" data-ch="${id}" viewBox="0 0 ${W} ${H}">${g}</svg>`;
}
/* Simple columns with every value on its cap (≤ 10 columns). */
function columnChart(vals, labels, o) {
  const id = 'ch' + (++chSeq), W = 340, H = 200, L = 6, R = 6, T = 34, B = 26;
  const n = vals.length, max = Math.max(...vals, 1), min = Math.min(...vals);
  const lo = Math.max(0, min - (max - min) * 1.2 - 1);   // zoom so changes are visible
  const slot = (W - L - R) / n, bw = Math.min(24, slot - 8), y = v => T + (1 - (v - lo) / (max - lo)) * (H - T - B);
  let g = `<line x1="${L}" x2="${W - R}" y1="${H - B}" y2="${H - B}" stroke="#dfe2e8"/>`;
  vals.forEach((v, i) => {
    const cx = L + i * slot + slot / 2, x0 = cx - bw / 2, yy = y(v), last = i === n - 1;
    g += `<path d="${topRound(x0, yy, bw, H - B - yy, 4)}" fill="${last ? o.color : o.soft}"/>`;
    g += `<text class="val" x="${cx}" y="${yy - 8}" text-anchor="middle"${n > 8 ? ' style="font-size:10px"' : ''}>${v}</text>`;
    const prev = i ? vals[i - 1] : o.before;
    if (prev != null && v !== prev) g += `<path d="${v > prev ? `M${cx - 3.5} ${yy - 21} l3.5 -5 l3.5 5z` : `M${cx - 3.5} ${yy - 26} l3.5 5 l3.5 -5z`}" fill="${v > prev ? '#16a34a' : '#dc2626'}"/>`;
    g += `<text x="${cx}" y="${H - 8}" text-anchor="middle" style="font-size:10px">${labels[i]}</text>`;
  });
  CH[id] = { kind: 'bars', slot, L, n, W, T, H, B, rows: i => [['Clientes ativos', o.color, vals[i]]], tips: o.tips, vfmt: v => String(v) };
  return `<svg class="chart" data-ch="${id}" viewBox="0 0 ${W} ${H}"><rect class="band" y="${T - 30}" height="${H - B - T + 30}" width="${slot}" rx="6" fill="#6366f1" fill-opacity=".07" visibility="hidden"/>${g}</svg>`;
}
/* Tooltip: one floating box for every chart. */
function chartHover(e) {
  const svg = e.target.closest && e.target.closest('svg[data-ch]');
  const tip = $('#ctip');
  if (!svg) { tip.hidden = true; document.querySelectorAll('svg[data-ch] .hover, svg[data-ch] .band').forEach(h => h.setAttribute('visibility', 'hidden')); return; }
  const c = CH[svg.dataset.ch]; if (!c) return;
  const r = svg.getBoundingClientRect(), px = (e.clientX - r.left) / r.width * c.W;
  let i, cx, rows;
  if (c.kind === 'line') {
    const span = (c.W - c.L - c.R) / Math.max(1, c.slots - 1);
    const len = Math.max(...c.series.map(s => s.vals.length));
    i = Math.max(0, Math.min(len - 1, Math.round((px - c.L) / span)));
    cx = c.x(i);
    const hov = svg.querySelector('.hover'); hov.setAttribute('visibility', 'visible');
    const xh = hov.querySelector('.xh'); xh.setAttribute('x1', cx); xh.setAttribute('x2', cx);
    hov.querySelectorAll('circle').forEach((dot, k) => { const v = c.series[k].vals[i]; dot.setAttribute('visibility', v == null ? 'hidden' : 'visible'); if (v != null) { dot.setAttribute('cx', cx); dot.setAttribute('cy', c.y(v)); } });
    rows = c.series.map(s => [s.name, s.color, s.vals[i]]).filter(x => x[2] != null);
  } else {
    i = Math.max(0, Math.min(c.n - 1, Math.floor((px - c.L) / c.slot)));
    cx = c.L + i * c.slot + c.slot / 2;
    const band = svg.querySelector('.band'); band.setAttribute('x', c.L + i * c.slot); band.setAttribute('visibility', 'visible');
    rows = c.rows(i);
  }
  tip.innerHTML = `<small>${esc(c.tips[i])}</small>${rows.map(([n, col, v]) => `<div><span>${col ? `<i style="background:${col}"></i>` : ''}${esc(n)}</span><b>${c.vfmt(v)}</b></div>`).join('')}`;
  tip.hidden = false;
  const sx = r.left + cx / c.W * r.width, tw = tip.offsetWidth, th = tip.offsetHeight;
  tip.style.left = Math.max(8, Math.min(innerWidth - tw - 8, sx - tw / 2)) + 'px';
  tip.style.top = Math.max(84, r.top + 4 - th) + 'px';
}
document.addEventListener('pointerdown', chartHover);
document.addEventListener('pointermove', e => { if (e.pointerType === 'mouse' || e.buttons) chartHover(e); });
addEventListener('scroll', () => { const t = $('#ctip'); if (t && !t.hidden) chartHover({ target: document.body }); }, { passive: true });

/* ----- Tools (Configurações) ----- */
const SUBS = {
  perfil: ['Configurações', 'Configurações do sistema', 'Perfil'],
  relatorios: ['Configurações', 'Configurações do sistema', 'Exportar Relatórios'],
  mensagens: ['Configurações', 'Configurações do sistema', 'Mensagem WhatsApp'],
  atividades: ['Configurações', 'Configurações do sistema', 'Histórico de Atividades'],
  importar: ['Importar', 'Gestão de mensalidades', 'Importar Clientes'],
  backup: ['Configurações', 'Configurações do sistema', 'Backup Completo'],
  planos: ['Planos', 'Configure seus planos'],
};
function setSub(sub) {
  state.sub = sub || null;
  const h = sub ? SUBS[sub] : PAGES.ferramentas;
  $('#pageTitle').textContent = h[0];
  $('#pageSub').textContent = h[1];
  $('#fab').hidden = sub !== 'planos';
  $('#fab').dataset.act = sub === 'planos' ? 'plan-new' : 'new-client';
  render();
  window.scrollTo(0, 0);
}
const subHead = (title, right) => `<div class="sub-head"><button data-act="sub" data-v="">${ic('arrowL')}</button><h2>${title}</h2>${right || ''}</div>`;

VIEWS.ferramentas = () => {
  if (state.sub) return SUBVIEWS[state.sub]();
  const tool = (sub, icon, bg, title, desc) => `<button class="card tool" data-act="sub" data-v="${sub}"><div class="ico" style="background:${bg}">${ic(icon)}</div><div><b>${title}</b><span>${desc}</span></div>${ic('right')}</button>`;
  const blue = 'linear-gradient(135deg,#4f86e8,#46a9d6)';
  return `
    <h2 class="sec">Configurações do Sistema</h2>
    ${tool('perfil', 'user', blue, 'Perfil', 'Gerencie suas informações pessoais e foto de perfil.')}
    ${tool('relatorios', 'chart', blue, 'Exportar Relatórios', 'Gere relatórios de clientes e financeiros em PDF ou CSV.')}
    ${tool('mensagens', 'chat', 'linear-gradient(135deg,#5ac35f,#4fb88a)', 'Mensagem WhatsApp', 'Personalize a mensagem enviada aos clientes.')}
    ${tool('atividades', 'history', 'linear-gradient(135deg,#f0a02c,#ec7a32)', 'Histórico de Atividades', 'Veja tudo que foi feito no sistema.')}
    <button class="card tool" data-act="dark"><div class="ico" style="background:linear-gradient(135deg,#334155,#0f172a)">${ic('moon')}</div><div><b>Modo escuro</b><span>Tema escuro em todo o app.</span></div><i class="sw ${db.settings.dark ? 'on' : ''}"></i></button>
    <h2 class="sec">Dados e Backups</h2>
    ${tool('importar', 'upload', 'linear-gradient(135deg,#5ac35f,#2f9c50)', 'Importar Clientes', 'Importe clientes em massa via planilha CSV ou Excel.')}
    ${tool('backup', 'db', 'linear-gradient(135deg,#6366f1,#9333ea)', 'Backup Completo', 'Faça backup ou restaure todos os dados do sistema.')}
    ${tool('planos', 'box', 'linear-gradient(135deg,#a855f7,#6366f1)', 'Planos', `${db.plans.length} planos cadastrados`)}
    <p class="hint" style="text-align:center">Gestor Simples — v3.11 · os dados ficam salvos neste aparelho</p>`;
};
VIEWS.ferramentasAfter = () => { if (state.sub && SUBVIEWS[state.sub + 'After']) SUBVIEWS[state.sub + 'After'](); };

const SUBVIEWS = {};

/* Perfil */
SUBVIEWS.perfil = () => {
  const st = db.settings;
  return `${subHead('Perfil')}
    <div class="card">
      <div class="profile-pic">${st.photo ? `<img src="${st.photo}" alt="">` : `<span>${esc((st.brand || 'x').slice(0, 2).toUpperCase())}</span>`}</div>
      <div style="display:flex;gap:10px;justify-content:center;margin:12px 0 18px">
        <button class="btn light" style="width:auto;padding:0 18px;height:42px" data-act="photo">${ic('camera')} Alterar foto</button>
        ${st.photo ? `<button class="btn light" style="width:auto;padding:0 18px;height:42px;color:#d24444" data-act="photo-del">${ic('trash')}</button>` : ''}
      </div>
      <input type="file" id="photoIn" accept="image/*" hidden>
      <div class="f"><label>${ic('user', 'style="color:#a855f7"')} Seu nome</label><input id="prName" value="${esc(st.owner || '')}" placeholder="Seu nome"></div>
      <div class="f"><label>${ic('box', 'style="color:#4f46e5"')} Empresa</label><input id="prCompany" value="${esc(st.company || '')}" placeholder="Nome da revenda (usado em {empresa})"></div>
      <div class="f"><label>${ic('phone', 'style="color:#3fae56"')} WhatsApp</label><input id="prPhone" type="tel" value="${esc(st.phone || '')}"></div>
      <button class="btn" data-act="save-profile">${ic('save')} Salvar</button>
    </div>
    <div class="card acct">
      <div class="ac-h">${ic('cloud')}<div><b>Conta e nuvem</b><span>${esc((window.cloudInfo && cloudInfo().email) || '')}</span></div></div>
      <p id="syncInfo">${window.cloudInfo ? cloudInfo().text : ''}</p>
      <button class="btn blue" data-act="cloud-check" style="margin-bottom:10px">${ic('checkc')} Conferir dados na nuvem</button>
      <div class="two"><button class="btn light" data-act="sync-now">${ic('refresh')} Sincronizar</button><button class="btn red" data-act="logout">${ic('logout')} Sair</button></div>
    </div>`;
};
SUBVIEWS.perfilAfter = () => {
  $('#photoIn').onchange = e => {
    const f = e.target.files[0]; if (!f) return;
    const img = new Image();
    img.onload = () => {
      const S = 160, cv = document.createElement('canvas'); cv.width = cv.height = S;
      const k = Math.max(S / img.width, S / img.height), w = img.width * k, h = img.height * k;
      cv.getContext('2d').drawImage(img, (S - w) / 2, (S - h) / 2, w, h);
      db.settings.photo = cv.toDataURL('image/jpeg', .85); URL.revokeObjectURL(img.src); refresh();
    };
    img.onerror = () => toast('Imagem inválida');
    img.src = URL.createObjectURL(f);
  };
};

/* Relatórios */
const FORMATS = [['pdf', 'PDF'], ['csv', 'CSV']];
SUBVIEWS.relatorios = () => {
  const box = (k, cls, icon, title, desc, color) => `<div class="rep ${cls}">
      <div class="rh"><div class="ico" style="background:${color}">${ic(icon)}</div><div><b>${title}</b><span>${desc}</span></div></div>
      <div class="rr"><button class="dd" data-act="dd" data-k="${k}">${ic('file', 'style="color:#5b6275"')}<span>${optLabel(FORMATS, state[k])}</span>${ic('down')}</button>
        <button class="gen" style="background:${color}" data-act="report" data-v="${k}">Gerar</button></div></div>`;
  return `${subHead('Exportar Relatórios')}
    <div class="card">
      ${box('fmtClients', 'blue', 'users', 'Relatório de Clientes', 'Lista completa com todos os dados', '#3b6fe0')}
      ${box('fmtFin', 'green', 'trending', 'Relatório Financeiro', `Faturamento e mensalidades · ${esc(periodShort())}`, '#43a35a')}
      <p class="hint">O período do relatório financeiro é o mesmo escolhido na aba Financeiro.</p>
    </div>`;
};

/* Mensagens */
const MSG_FIELDS = [
  ['msgWhats', 'Mensagem botão do WhatsApp', 'Use <code>{empresa}</code> para sua empresa, <code>{{cliente}}</code> para o nome do cliente, <code>{{plano}}</code> para o nome do plano, <code>{{vencimento}}</code> para a data de vencimento e <code>{{valor}}</code> para o valor.'],
  ['msgCadastro', 'Mensagem de Cadastro', 'Enviada após cadastrar um novo cliente. Use <code>{{vencimento}}</code> para a data e <code>{{plano}}</code> para o nome do plano.'],
  ['msgRenovacao', 'Mensagem de Renovação', 'Enviada após renovar um plano. Use <code>{{vencimento}}</code> para a nova data e <code>{{plano}}</code> para o nome do plano.'],
];
SUBVIEWS.mensagens = () => `${subHead('Mensagem WhatsApp', `<button class="save-btn" data-act="save-msgs">${ic('save')} Salvar</button>`)}
  ${MSG_FIELDS.map(([k, t, h]) => `<div class="card msg-card"><b>${t}</b><p>${h}</p><textarea id="${k}" rows="6">${esc(db.settings[k] || '')}</textarea></div>`).join('')}`;

/* Atividades */
function actIcon(h) {
  const t = h.title.toLowerCase();
  if (t.includes('cadastr')) return ['👤', 'linear-gradient(135deg,#5ac35f,#3e9156)'];
  if (t.includes('renov')) return ['🔄', 'linear-gradient(135deg,#a855f7,#db5b97)'];
  if (t.includes('exclu') || t.includes('limp')) return ['🗑️', 'linear-gradient(135deg,#ef4444,#b91c1c)'];
  if (t.includes('edit')) return ['✏️', 'linear-gradient(135deg,#4f86e8,#4f46e5)'];
  return ['📦', 'linear-gradient(135deg,#f0a02c,#ec7a32)'];
}
const fmtStamp = s => `${br(s)}, ${s.slice(11, 16)}`;
SUBVIEWS.atividades = () => `${subHead('Histórico de Atividades')}
  <div class="card">
    <div class="act-top"><div><h3>Registro de Atividades</h3><small>${db.history.length} atividades registradas</small></div>
      <button class="clear" data-act="clear-hist">${ic('trash')} Limpar Histórico</button></div>
    ${db.history.slice(0, 200).map(h => { const [e, bg] = actIcon(h); return `<div class="act">
      <div class="ai" style="background:${bg}">${e}</div>
      <div class="ab"><b>${esc(h.title)}</b><p>${esc(h.desc)}</p><small>${ic('clock')} ${fmtStamp(h.at)}</small>
        ${h.undone ? `<span class="done">${ic('checkc')} Ação desfeita</span>` : h.undo ? `<button class="undo" data-act="undo" data-v="${h.id}">${ic('undo')} Desfazer</button>` : ''}</div></div>`; }).join('') || '<div class="empty">Nenhuma atividade registrada</div>'}
  </div>`;

/* Importar clientes */
SUBVIEWS.importar = () => `${subHead('Importar Clientes')}
  <div class="card steps">
    <h3>${ic('info')} Como Importar - Passo a Passo</h3>
    <div class="step"><span class="n" style="background:#3b6fe0">1</span><div><b>Baixe o Modelo</b><p>Clique no botão abaixo para baixar a planilha modelo.</p>
      <button class="out-btn" data-act="model-csv">${ic('download')} Baixar Modelo CSV</button></div></div>
    <div class="step"><span class="n" style="background:#5046d8">2</span><div><b>Copie o Código do Plano</b><p>Vá até a página <b>Planos</b> e copie o código de 4 dígitos do plano.</p>
      <div class="warn-note">${ic('alert')} Use o CÓDIGO (ex: 1234), não o nome do plano</div></div></div>
    <div class="step"><span class="n" style="background:#9333ea">3</span><div><b>Preencha a Planilha</b>
      <p class="ck">${ic('check')} Cole o código de 4 dígitos na coluna plano_id</p><p class="ck">${ic('check')} Data no formato: DD/MM/AAAA (ex: 25/12/2025)</p></div></div>
    <div class="step"><span class="n" style="background:#43a35a">4</span><div><b>Faça o Upload</b><p>Salve o arquivo e faça o upload abaixo.</p></div></div>
  </div>
  <div class="card up-card green">
    <div class="up-h">${ic('upload')} Fazer Upload do Arquivo</div>
    <div class="drop">${ic('upload', 'class="big-ic"')}<b>Selecione o arquivo</b><span>Suporta arquivos CSV e Excel (.csv, .xlsx)</span>
      <button class="pick" data-act="pick" data-v="csvIn">${ic('file')} Selecionar Arquivo</button></div>
    <input type="file" id="csvIn" accept=".csv,.xlsx,.xls,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel" hidden>
    <div class="note g">${ic('check')}<div><b>Importação Inteligente</b><span>Aceita CÓDIGO do plano, ID completo ou o nome.</span></div></div>
    <div class="note b">${ic('info')}<div><b>Mensalidade Automática</b><span>A primeira mensalidade será criada como PAGA.</span></div></div>
  </div>`;
SUBVIEWS.importarAfter = () => { $('#csvIn').onchange = e => { importSheet(e.target.files[0]); e.target.value = ''; }; };

/* Backup */
SUBVIEWS.backup = () => `${subHead('Backup Completo')}
  <div class="card bk">
    <div class="bk-h"><div class="ico">${ic('db')}</div><div><b>Backup Completo do Sistema</b><span>Clientes, planos, mensalidades, histórico, mensagens e configurações</span></div></div>
    <div class="bk-b">
      <div class="bk-t"><div class="ico" style="background:#e3ecfd;color:#3b6fe0">${ic('drive')}</div><b>Fazer Backup</b></div>
      <p>Baixe todos os dados do sistema em um único arquivo JSON.</p>
      <button class="btn blue" data-act="backup-now">${ic('download')} Fazer Backup Agora</button>
      <div class="tip">💡 <b>Dica:</b> Faça backups regulares para não perder dados.</div>
      <div class="bk-t"><div class="ico" style="background:#f1e8ff;color:#9333ea">${ic('shield')}</div><b>Restaurar Backup</b></div>
      <p>Importe um arquivo de backup JSON para restaurar dados. Aceita backups deste app e do Gestor Simples.</p>
      <div class="drop purple">${ic('upload', 'class="big-ic"')}<b>Selecione um arquivo de backup (.json ou .zip)</b>
        <button class="pick" data-act="pick" data-v="bkIn">${ic('upload')} Selecionar Arquivo</button></div>
      <input type="file" id="bkIn" accept=".json,.zip,application/json,application/zip,text/*" hidden>
      <div class="att">⚠️ <b>Atenção:</b> A importação adiciona dados, não substitui.</div>
    </div>
  </div>`;
SUBVIEWS.backupAfter = () => { $('#bkIn').onchange = e => { importBackup(e.target.files[0]); e.target.value = ''; }; };

/* Planos */
SUBVIEWS.planos = () => {
  const q = (state.planQ || '').trim().toLowerCase();
  const list = db.plans.filter(p => !q || p.name.toLowerCase().includes(q) || String(p.code).includes(q)).sort((a, b) => a.name.localeCompare(b.name));
  const counts = {}; db.clients.forEach(c => { if (!c.archived) counts[c.planId] = (counts[c.planId] || 0) + 1; });
  return `<div class="plans-top">
      <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px"><button class="back-mini" data-act="sub" data-v="">${ic('arrowL')}</button>
        <label class="search" style="flex:1">${ic('search')}<input id="planQ" type="search" placeholder="Buscar planos ou códigos..." value="${esc(state.planQ || '')}"></label></div>
      <div class="plan-count">${ic('box')} <b>${list.length}</b> planos</div></div>
    ${list.map(p => {
      const lucro = p.price - p.cost, marg = p.price > 0 ? Math.round(lucro / p.price * 100) : 0;
      return `<div class="card plan-card" style="--pc:${p.color || '#E3F2FD'}">
        <div class="pc-h"><div class="ico">${ic('box')}</div><div><b>${esc(p.name)}</b>
          <div class="pc-tags"><span>${TYPE_NAMES[p.type]}</span><button class="code" data-act="copy" data-v="${esc(p.code)}">#${esc(p.code)} ${ic('copy')}</button></div></div></div>
        <div class="pc-price"><small>Preço</small><b>${money(p.price)}</b>${p.desc ? `<p>${esc(p.desc)}</p>` : ''}</div>
        <div class="pc-two">
          <div class="pc-l"><small>${ic('trending')} Lucro</small><b>${money(lucro)}</b><span>${marg}% margem</span></div>
          <button class="pc-c" data-act="plan-list" data-id="${p.id}"><small>${ic('users')} Clientes</small><b>${counts[p.id] || 0}</b><span>ver lista →</span></button>
        </div>
        ${p.link ? `<button class="pc-link" data-act="open-link" data-v="${esc(p.link)}">${ic('link')} ${esc(p.link)}</button>` : ''}
        <div class="pc-act"><button class="e" data-act="plan-edit" data-id="${p.id}">${ic('edit')} Editar</button><button class="d" data-act="plan-del" data-id="${p.id}">${ic('trash')} Excluir</button></div>
      </div>`; }).join('') || '<div class="card empty">Nenhum plano encontrado</div>'}`;
};
SUBVIEWS.planosAfter = () => {
  const q = $('#planQ');
  q.oninput = () => { state.planQ = q.value; clearTimeout(q.t); q.t = setTimeout(() => { const pos = q.selectionStart; render(); const n = $('#planQ'); n.focus(); try { n.setSelectionRange(pos, pos); } catch (e) { } }, 200); };
};

const PLAN_COLORS = [['#E3F2FD', 'Azul'], ['#E8F5E9', 'Verde'], ['#FCE4EC', 'Rosa'], ['#F3E5F5', 'Roxo'], ['#FFF3E0', 'Laranja'], ['#FFFDE7', 'Amarelo'],
  ['#E0F7FA', 'Ciano'], ['#F9FBE7', 'Lima'], ['#FFE0B2', 'Pêssego'], ['#EDE7F6', 'Lavanda'], ['#E0F2F1', 'Turquesa'], ['#FFEBEE', 'Vermelho']];
function newPlanCode() {
  const used = new Set(db.plans.map(p => String(p.code)));
  for (let i = 0; i < 9000; i++) { const c = String(1000 + Math.floor(Math.random() * 9000)); if (!used.has(c)) return c; }
  return String(Date.now()).slice(-6);
}
function planForm(p) {
  const isNew = !p; p = p || { name: '', type: 'mensal', price: '', cost: '', color: PLAN_COLORS[0][0], link: '', desc: '' };
  let color = p.color || PLAN_COLORS[0][0];
  const s = overlay(`<div class="sheet"><form class="form plain">
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <h2 class="center-t">${isNew ? 'Criar Novo Plano' : 'Editar Plano'}</h2>
    <div class="f"><label>Nome do Plano <em>*</em></label><input id="pName" value="${esc(p.name)}" placeholder="Ex: Plano Premium" required></div>
    <div class="f2"><div class="f"><label>Preço <em>*</em></label><input id="pPrice" type="number" step="0.01" min="0" inputmode="decimal" placeholder="0.00" value="${p.price}"></div>
      <div class="f"><label>Custo <em>*</em></label><input id="pCost" type="number" step="0.01" min="0" inputmode="decimal" placeholder="0.00" value="${p.cost}"></div></div>
    <div class="f"><label>Período <em>*</em></label><select id="pType">${Object.keys(TYPES).map(t => `<option value="${t}" ${t === p.type ? 'selected' : ''}>${TYPE_NAMES[t]}</option>`).join('')}</select></div>
    <div class="f"><label>Cor do Plano</label><div class="swatches">${PLAN_COLORS.map(([c]) => `<button type="button" data-c="${c}" style="background:${c}" class="${c.toUpperCase() === color.toUpperCase() ? 'on' : ''}"></button>`).join('')}</div>
      <div class="sel-c"><i style="background:${color}"></i><span>Cor selecionada: ${(PLAN_COLORS.find(x => x[0].toUpperCase() === color.toUpperCase()) || [0, 'Personalizada'])[1]}</span></div></div>
    <div class="f"><label>Link (Opcional)</label><input id="pLink" type="url" placeholder="https://..." value="${esc(p.link || '')}"></div>
    <div class="f"><label>Descrição</label><textarea id="pDesc" placeholder="Descreva os benefícios do plano...">${esc(p.desc || '')}</textarea></div>
    <button class="btn" type="submit">${isNew ? 'Criar Plano' : 'Salvar Alterações'}</button></form></div>`);
  s.querySelector('.swatches').onclick = e => {
    const b = e.target.closest('[data-c]'); if (!b) return;
    color = b.dataset.c;
    s.querySelectorAll('.swatches button').forEach(x => x.classList.toggle('on', x === b));
    s.querySelector('.sel-c').innerHTML = `<i style="background:${color}"></i><span>Cor selecionada: ${PLAN_COLORS.find(x => x[0] === color)[1]}</span>`;
  };
  s.querySelector('form').onsubmit = e => {
    e.preventDefault();
    const data = { name: $('#pName').value.trim().toUpperCase(), type: $('#pType').value, price: parseFloat(String($('#pPrice').value).replace(',', '.')) || 0,
      cost: parseFloat(String($('#pCost').value).replace(',', '.')) || 0, color, link: $('#pLink').value.trim(), desc: $('#pDesc').value.trim() };
    if (!data.name) return toast('Informe o nome');
    if (isNew) db.plans.push(Object.assign({ id: uid(), code: newPlanCode() }, data)); else Object.assign(p, data);
    closeTop(); refresh(); toast(isNew ? 'Plano criado' : 'Plano atualizado');
  };
}

/* Messages */
function fillMsg(tpl, c) {
  const p = planOf(c), st = db.settings;
  return String(tpl || '')
    .replace(/{{\s*cliente\s*}}|{nome}/g, c.name).replace(/{{\s*plano\s*}}|{plano}/g, p ? p.name : '')
    .replace(/{{\s*vencimento\s*}}|{vencimento}/g, br(c.due)).replace(/{{\s*valor\s*}}|{valor}/g, money(priceOf(c)))
    .replace(/{{\s*empresa\s*}}|{empresa}/g, st.company || st.brand || '');
}

/* Confirmation after a signup or renewal, with the matching WhatsApp text ready to copy. */
function successDialog(c, kind) {
  const isNew = kind === 'cadastro';
  const s = overlay(`<div class="dialog okd">
    <div class="okd-h"><button class="x" data-act="close">${ic('x')}</button><div class="c">${ic('checkc')}</div>
      <h3>${isNew ? 'Cliente Cadastrado!' : 'Renovação Efetuada!'}</h3><p>Tudo certo por aqui!</p></div>
    <div class="okd-b">
      <div class="okd-c"><b>${esc(c.name)}</b><span>${esc(fmtPhone(c.phone))}</span>
        <div class="okd-d">Vencimento: <b>${br(c.due)}</b></div></div>
      <button class="btn blue" id="okCopy">${ic('copy')} Copiar Mensagem</button>
      <button class="btn light" data-act="close">Fechar</button>
    </div></div>`, 'center');
  s.querySelector('#okCopy').onclick = () => {
    const text = fillMsg(db.settings[isNew ? 'msgCadastro' : 'msgRenovacao'], c);
    if (window.Android && Android.copy) Android.copy(text);
    else if (navigator.clipboard) navigator.clipboard.writeText(text).catch(() => {});
    toast('Mensagem copiada');
  };
}

/* Files */
function saveFile(name, mime, text) {
  if (window.Android && Android.saveFile) { Android.saveFile(name, mime, text); return; }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([text], { type: mime })); a.download = name; a.click();
}
const csvCell = v => { const t = String(v == null ? '' : v); return /[";\n]/.test(t) ? `"${t.replace(/"/g, '""')}"` : t; };
const toCsv = rows => '﻿' + rows.map(r => r.map(csvCell).join(';')).join('\r\n');
function parseCsv(text) {
  text = text.replace(/^﻿/, '');
  const first = text.split(/\r?\n/)[0] || '';
  const sep = (first.match(/;/g) || []).length >= (first.match(/,/g) || []).length ? ';' : ',';
  const rows = []; let row = [], cell = '', q = false;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (q) { if (ch === '"') { if (text[i + 1] === '"') { cell += '"'; i++; } else q = false; } else cell += ch; }
    else if (ch === '"') q = true;
    else if (ch === sep) { row.push(cell); cell = ''; }
    else if (ch === '\n' || ch === '\r') { if (ch === '\r' && text[i + 1] === '\n') i++; row.push(cell); rows.push(row); row = []; cell = ''; }
    else cell += ch;
  }
  if (cell !== '' || row.length) { row.push(cell); rows.push(row); }
  return rows.filter(r => r.some(x => String(x).trim()));
}
/* Minimal .xlsx reader: first worksheet, shared/inline strings, numbers. */
/* Minimal ZIP reader: lists entries and inflates them (browser DecompressionStream, or the Android app). */
function openZip(buf) {
  const dv = new DataView(buf), u8 = new Uint8Array(buf);
  let eocd = -1;
  for (let i = u8.length - 22; i >= Math.max(0, u8.length - 66000); i--) if (dv.getUint32(i, true) === 0x06054b50) { eocd = i; break; }
  if (eocd < 0) throw new Error('arquivo compactado inválido');
  const n = dv.getUint16(eocd + 10, true); let off = dv.getUint32(eocd + 16, true);
  const files = {};
  for (let k = 0; k < n; k++) {
    const method = dv.getUint16(off + 10, true), csize = dv.getUint32(off + 20, true), nl = dv.getUint16(off + 28, true), el = dv.getUint16(off + 30, true), cl = dv.getUint16(off + 32, true), lo = dv.getUint32(off + 42, true);
    const name = new TextDecoder().decode(u8.subarray(off + 46, off + 46 + nl));
    files[name] = { method, csize, lo };
    off += 46 + nl + el + cl;
  }
  const read = async name => {
    const f = files[name]; if (!f) return null;
    const start = f.lo + 30 + dv.getUint16(f.lo + 26, true) + dv.getUint16(f.lo + 28, true);
    const data = u8.subarray(start, start + f.csize);
    if (f.method === 0) return new TextDecoder().decode(data);
    if (typeof DecompressionStream !== 'undefined') {
      try { return await new Response(new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'))).text(); } catch (e) { }
    }
    if (window.Android && Android.inflate) {
      let bin = ''; for (let i = 0; i < data.length; i += 0x8000) bin += String.fromCharCode.apply(null, data.subarray(i, i + 0x8000));
      const out = Android.inflate(btoa(bin));
      if (out != null) return out;
    }
    throw new Error('não foi possível descompactar o arquivo neste aparelho');
  };
  return { names: Object.keys(files), read };
}
const isZip = buf => { const u = new Uint8Array(buf, 0, Math.min(4, buf.byteLength)); return u[0] === 0x50 && u[1] === 0x4b; };
async function readXlsx(buf) {
  const z = openZip(buf), files = {}; z.names.forEach(n => { files[n] = 1; }); const read = z.read;
  const xml = t => new DOMParser().parseFromString(t, 'application/xml');
  const sst = [];
  const ss = await read('xl/sharedStrings.xml');
  if (ss) xml(ss).querySelectorAll('si').forEach(si => sst.push([...si.getElementsByTagName('t')].map(t => t.textContent).join('')));
  const sheetName = Object.keys(files).filter(f => /^xl\/worksheets\/sheet\d+\.xml$/.test(f)).sort()[0];
  if (!sheetName) throw new Error('planilha vazia');
  const doc = xml(await read(sheetName));
  const colIdx = ref => { let n = 0; for (const ch of ref.replace(/\d+/g, '')) n = n * 26 + ch.charCodeAt(0) - 64; return n - 1; };
  const rows = [];
  doc.querySelectorAll('sheetData row').forEach(r => {
    const out = [];
    r.querySelectorAll('c').forEach(c => {
      const t = c.getAttribute('t'), v = c.getElementsByTagName('v')[0];
      let val = '';
      if (t === 's') val = sst[+(v ? v.textContent : 0)] || '';
      else if (t === 'inlineStr') val = [...c.getElementsByTagName('t')].map(x => x.textContent).join('');
      else val = v ? v.textContent : '';
      out[colIdx(c.getAttribute('r') || 'A')] = val;
    });
    rows.push(Array.from(out, x => x == null ? '' : x));
  });
  return rows.filter(r => r.some(x => String(x).trim()));
}
function parseDateBR(v) {
  v = String(v || '').trim();
  let m = v.match(/^(\d{1,2})[\/\-.](\d{1,2})[\/\-.](\d{2,4})$/);
  if (m) return `${m[3].length === 2 ? '20' + m[3] : m[3]}-${pad(m[2])}-${pad(m[1])}`;
  m = v.match(/^(\d{4})-(\d{2})-(\d{2})/); if (m) return `${m[1]}-${m[2]}-${m[3]}`;
  if (/^\d{5}(\.\d+)?$/.test(v)) return iso(new Date(Math.round((parseFloat(v) - 25569) * 864e5) + new Date().getTimezoneOffset() * 6e4));
  return '';
}
function findPlan(v) {
  v = String(v || '').trim(); if (!v) return null;
  const up = v.toUpperCase();
  return db.plans.find(p => String(p.code) === v) || db.plans.find(p => p.id === v) || db.plans.find(p => p.name.toUpperCase() === up) || null;
}
async function importSheet(f) {
  if (!f) return;
  try {
    let rows;
    if (/\.xls$/i.test(f.name)) throw new Error('formato .xls antigo; salve como .xlsx ou .csv');
    if (/\.xlsx$/i.test(f.name)) rows = await readXlsx(await f.arrayBuffer());
    else rows = parseCsv(await f.text());
    if (rows.length < 2) throw new Error('a planilha não tem linhas de clientes');
    const head = rows[0].map(h => String(h).trim().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, ''));
    const col = names => head.findIndex(h => names.some(n => h === n || h.includes(n)));
    const iN = col(['nome', 'name', 'cliente']), iW = col(['whats', 'telefone', 'phone', 'celular']), iP = col(['plano_id', 'plano', 'codigo']), iD = col(['vencimento', 'data_vencimento', 'venc', 'data']),
      iQ = col(['quantidade', 'qtd', 'telas']), iDs = col(['desconto']), iNo = col(['notas', 'obs', 'observ']);
    if (iN < 0) throw new Error('a planilha precisa da coluna "nome"');
    const errors = []; let ok = 0;
    rows.slice(1).forEach((r, k) => {
      const line = k + 2, name = String(r[iN] || '').trim();
      if (!name) return;
      const plan = iP >= 0 ? findPlan(r[iP]) : null;
      if (iP >= 0 && String(r[iP] || '').trim() && !plan) { errors.push(`Linha ${line} (${name}): plano "${r[iP]}" não encontrado`); return; }
      const due = iD >= 0 ? parseDateBR(r[iD]) : '';
      const c = { id: uid(), name, phone: normPhone(iW >= 0 ? r[iW] : ''), planId: plan ? plan.id : '', due: due || (plan ? addMonths(today(), TYPES[plan.type]) : ''),
        qty: Math.max(1, parseInt(iQ >= 0 ? r[iQ] : 1, 10) || 1), discount: Math.max(0, parseFloat(String(iDs >= 0 ? r[iDs] : 0).replace(',', '.')) || 0),
        notes: iNo >= 0 ? String(r[iNo] || '').trim() : '', essential: false, archived: false, createdAt: today() };
      db.clients.push(c);
      if (plan) addPayment(c, 'cadastro', priceOf(c));
      ok++;
    });
    log('Clientes Importados', `${ok} clientes importados de ${f.name}${errors.length ? ` (${errors.length} com erro)` : ''}`);
    snapshot(); refresh();
    overlay(`<div class="dialog confirm">${ic('checkc', 'style="color:#3fae56"')}<h3>Importação concluída</h3><p>${ok} clientes importados.${errors.length ? `<br><small style="color:#d24444">${errors.slice(0, 8).map(esc).join('<br>')}${errors.length > 8 ? `<br>… e mais ${errors.length - 8}` : ''}</small>` : ''}</p>
      <div class="two"><button class="ok" style="background:#4f46e5" data-act="close">OK</button></div></div>`, 'center');
  } catch (e) { toast('Erro: ' + e.message); }
}
function modelCsv() {
  const p = db.plans[0];
  saveFile('modelo-importacao-clientes.csv', 'text/csv', toCsv([['nome', 'whatsapp', 'plano_id', 'data_vencimento', 'quantidade', 'desconto', 'notas'],
    ['Maria Silva', '+55 11 99999-9999', p ? p.code : '1234', '25/12/2026', '1', '0', 'Exemplo']]));
}
/* Parses a backup. Files saved by versions before 3.2 over a bigger file have the old file's tail
   after the JSON; when that tail is a Gestor Simples export, it is rebuilt and used. */
function parseBackup(txt) {
  try { return JSON.parse(txt); } catch (err) {
    const end = jsonEnd(txt);
    if (end < 0) throw err;
    const tail = txt.slice(end), m = /\n\s*\},\s*\n\s*\{/.exec(tail);
    if (m && /"(nome|plano_id|data_vencimento)"\s*:/.test(tail)) {
      try { const r = JSON.parse('{"version":"2.0","app":"Gestor Simples","data":{"clientes":[' + tail.slice(m.index + m[0].length - 1)); toast('Backup danificado recuperado'); return r; } catch (e) { }
    }
    const first = JSON.parse(txt.slice(0, end));
    if (first.clients && first.clients.length) return first;
    throw new Error('o arquivo está danificado. Use o arquivo "gestor_simples_backup_recuperado.json"');
  }
}
// Index just after the first complete top-level JSON object.
function jsonEnd(t) {
  let depth = 0, str = false;
  for (let i = 0; i < t.length; i++) {
    const c = t[i];
    if (str) { if (c === '\\') i++; else if (c === '"') str = false; continue; }
    if (c === '"') str = true;
    else if (c === '{' || c === '[') depth++;
    else if ((c === '}' || c === ']') && --depth === 0) return i + 1;
  }
  return -1;
}
async function backupText(f) {
  const buf = await f.arrayBuffer();
  if (!isZip(buf)) return new TextDecoder().decode(buf);
  // Backup that came compressed (.zip from WhatsApp, Drive, e-mail…): use the .json inside.
  const z = openZip(buf), name = z.names.find(n => /\.json$/i.test(n) && !/__MACOSX/.test(n)) || z.names.find(n => !n.endsWith('/'));
  if (!name) throw new Error('o arquivo .zip não tem um backup .json');
  return z.read(name);
}
function importBackup(f) {
  if (!f) return;
  toast('Lendo backup…');
  backupText(f).then(txt => {
    txt = String(txt).replace(/^\uFEFF/, '').trim();
    if (!txt.startsWith('{')) throw new Error('o arquivo escolhido não é um backup .json');
    let d = parseBackup(txt), label = 'backup';
    if (d.data && Array.isArray(d.data.clientes)) { d = fromGestorSimples(d); label = 'backup do Gestor Simples'; }
    if (!d.clients || !d.plans) throw new Error('arquivo não é um backup reconhecido');
    const known = new Set(db.clients.map(c => c.id));
    const novos = d.clients.filter(c => !known.has(c.id)).length;
    confirmDialog('Restaurar backup', `Adicionar dados do ${label}: ${novos} clientes novos (de ${d.clients.length}), ${d.plans.length} planos e ${(d.payments || []).length} pagamentos? Itens que já existem são mantidos.`, () => {
      const r = mergeData(d); snapshot(); refresh(); toast(`${r.clients} clientes e ${r.payments} pagamentos adicionados`);
    });
  }).catch(e => toast('Erro: ' + e.message));
}
function mergeData(d) {
  const planMap = {};
  d.plans.forEach(p => {
    const ex = db.plans.find(x => x.id === p.id) || db.plans.find(x => x.name.toUpperCase() === String(p.name).toUpperCase());
    if (ex) {
      // Same plan: take the backup's price, cost, period and details; keep the code unless it is free to reuse.
      ['name', 'type', 'price', 'cost', 'color', 'link', 'desc'].forEach(k => { if (p[k] !== undefined && p[k] !== '') ex[k] = p[k]; });
      if (p.code && !db.plans.some(x => x !== ex && String(x.code) === String(p.code))) ex.code = String(p.code);
      planMap[p.id] = ex.id;
    }
    else { const np = Object.assign({ color: PLAN_COLORS[0][0], link: '', desc: '' }, p); if (!np.code || db.plans.some(x => String(x.code) === String(np.code))) np.code = newPlanCode(); db.plans.push(np); planMap[p.id] = np.id; }
  });
  const cids = new Set(db.clients.map(c => c.id)), pids = new Set(db.payments.map(p => p.id));
  let nc = 0, np = 0;
  d.clients.forEach(c => { if (cids.has(c.id)) return; db.clients.push(Object.assign({}, c, { planId: planMap[c.planId] || c.planId || '' })); nc++; });
  (d.payments || []).forEach(p => { if (pids.has(p.id)) return; db.payments.push(p); np++; });
  if (d.snapshots) Object.keys(d.snapshots).forEach(k => { if (!(k in db.snapshots)) db.snapshots[k] = d.snapshots[k]; });
  if (d.settings) ['msgWhats', 'msgCadastro', 'msgRenovacao', 'company', 'owner', 'photo'].forEach(k => { if (!db.settings[k] && d.settings[k]) db.settings[k] = d.settings[k]; });
  log('Backup Importado', `${nc} clientes, ${np} pagamentos adicionados`);
  return { clients: nc, payments: np };
}
function backupNow() {
  saveFile(`backup-gestor-app_${today()}_${pad(new Date().getHours())}${pad(new Date().getMinutes())}.json`, 'application/json', JSON.stringify(Object.assign({ app: 'Gestor Simples (app)', version: 2, exported_at: new Date().toISOString() }, db)));
}

/* Reports */
function clientsReport(fmt) {
  const rows = db.clients.slice().sort((a, b) => a.name.localeCompare(b.name));
  const stTxt = c => c.archived ? 'Arquivado' : statusOf(c).text;
  if (fmt === 'csv') {
    saveFile(`clientes_${today()}.csv`, 'text/csv', toCsv([['nome', 'whatsapp', 'plano', 'plano_codigo', 'vencimento', 'status', 'quantidade', 'desconto', 'valor', 'essencial', 'notas', 'cadastro']]
      .concat(rows.map(c => { const p = planOf(c); return [c.name, fmtPhone(c.phone), p ? p.name : '', p ? p.code : '', br(c.due), stTxt(c), c.qty, c.discount, priceOf(c).toFixed(2).replace('.', ','), c.essential ? 'sim' : 'não', c.notes, br(c.createdAt)]; }))));
    return;
  }
  const L = rows.filter(c => !c.archived);
  const kp = (l, v, c) => `<div class="kp"><small>${l}</small><b style="color:${c}">${v}</b></div>`;
  $('#print').innerHTML = reportShell('Relatório de Clientes', `${br(today())}`, `
    <div class="rp-k">${kp('TOTAL', rows.length, '#7c3aed')}${kp('ATIVOS', L.filter(isActive).length, '#3e9a4f')}${kp('VENCIDOS', L.filter(c => c.due && daysLeft(c) < 0).length, '#d23c3c')}${kp('VENCEM HOJE', L.filter(c => daysLeft(c) === 0).length, '#e0702c')}${kp('ARQUIVADOS', rows.length - L.length, '#5b6275')}</div>
    <div class="rp-t"><b>Clientes</b><span>${rows.length} registros</span></div>
    <table><thead><tr><th>#</th><th>CLIENTE</th><th>WHATSAPP</th><th>PLANO</th><th>VENCIMENTO</th><th class="r">VALOR</th><th>STATUS</th></tr></thead><tbody>
    ${rows.map((c, i) => `<tr><td class="m">${i + 1}</td><td>${esc(c.name)}</td><td>${esc(fmtPhone(c.phone))}</td><td>${esc(planOf(c) ? planOf(c).name : '—')}</td><td>${br(c.due)}</td><td class="r b">${money(priceOf(c))}</td><td>${stTxt(c)}</td></tr>`).join('')}</tbody></table>`);
  if (window.Android) Android.print('Relatorio-Clientes'); else window.print();
}
function finReport(fmt) {
  if (fmt !== 'csv') return pdf();
  const r = periodRange();
  const pays = db.payments.filter(p => inRange(p, r)).sort((a, b) => a.date < b.date ? -1 : 1);
  saveFile(`financeiro_${today()}.csv`, 'text/csv', toCsv([['data', 'cliente', 'plano', 'tipo', 'valor', 'custo', 'lucro']]
    .concat(pays.map(p => [`${br(p.date)} ${p.date.slice(11, 16)}`, p.clientName, p.planName, p.kind === 'cadastro' ? 'Cadastro' : 'Renovação',
      p.amount.toFixed(2).replace('.', ','), (p.cost || 0).toFixed(2).replace('.', ','), (p.amount - (p.cost || 0)).toFixed(2).replace('.', ',')]))));
}

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
const openLayers = () => [...layer().children].filter(x => !x.classList.contains('out'));
function closeTop() {
  const l = openLayers().pop(); if (!l) return;
  l.classList.add('out');
  setTimeout(() => { l.remove(); if (state.dirty && !openLayers().length) { state.dirty = false; rerender(); } }, 170);
}
function closeMenus() { document.querySelectorAll('.menu,.pop,.pop-scrim').forEach(m => m.remove()); }
window.onBack = () => {
  if (state.sel) { selEnd(); return true; }
  if (document.querySelector('.menu,.pop')) { closeMenus(); return true; }
  if (openLayers().length) { closeTop(); return true; }
  if (state.tab === 'ferramentas' && state.sub) { setSub(null); return true; }
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
  const s = overlay(`<div class="sheet det">
    <div class="det-hero"><div class="ini">${initials(c.name)}</div>
      <div class="dh-t"><b>${esc(c.name)}</b><span class="status" style="--c:${st.color}">${st.short || st.text}</span></div>
      <button class="x" data-act="close">${ic('x')}</button></div>
    <div class="det-body">
      <button class="wa-row" data-act="wa" data-id="${c.id}">${ic('wa')}<span><b>WhatsApp</b>${esc(fmtPhone(c.phone))}</span>${ic('right')}</button>
      <div class="det-plan">
        <div class="dp-h">${ic('box')}<b>${p ? esc(p.name) : 'Sem plano'}</b>${p ? `<span class="tag">${p.type}${c.qty > 1 ? ` · ${c.qty} telas` : ''}</span>` : ''}</div>
        <div class="dp-v">
          <div><small>Valor</small><b>${money(price)}</b>${c.discount > 0 ? `<em>${money(base)} −${money(c.discount).replace('R$ ', '')}</em>` : ''}</div>
          <div><small>Custo</small><b>${money(cost)}</b></div>
          <div><small>Lucro</small><b class="g">${money(price - cost)}</b></div>
        </div>
        <div class="dp-due">${ic('calendar')}<span>Vencimento</span><b>${br(c.due)}</b></div>
      </div>
      ${c.notes ? `<div class="notes">${esc(c.notes)}</div>` : ''}
      <div class="det-tg">
        <button class="tg" data-act="t-ess" data-id="${c.id}">${ic('star')}<span>Essencial</span><i class="sw ${c.essential ? 'on' : ''}"></i></button>
        <button class="tg" data-act="t-arc" data-id="${c.id}">${ic('archive')}<span>Arquivar</span><i class="sw ${c.archived ? 'on' : ''}"></i></button>
      </div>
      <div class="det-act">
        <button class="b-light" data-act="pays" data-id="${c.id}">${ic('history')} Pagamentos</button>
        <button class="b-light" data-act="edit" data-id="${c.id}">${ic('edit')} Editar</button>
        <button class="b-blue" data-act="renew" data-id="${c.id}">${ic('renew')} Renovar</button>
      </div>
      <div class="reg">${ic('userplus')} Cadastrado em ${br(c.createdAt)}</div>
    </div></div>`);
  s.dataset.detail = c.id;
}
function reopenDetail(id) {
  const open = layer().querySelector('[data-detail]:not(.out)');
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

/* Plan field: button + inline list with search, like the original app. */
function planPicker(sel) {
  const p = db.plans.find(x => x.id === sel);
  return `<button type="button" class="pp-btn" id="ppBtn"><span class="${p ? '' : 'ph'}">${p ? esc(p.name) : 'Selecione um plano'}</span>${ic('down')}</button>`;
}
/* Full-width plan list, most used plans first; search only when the user taps it (no keyboard over the list). */
function planSheet(current, onPick) {
  const use = idx().use;
  const s = overlay(`<div class="sheet psheet">
    <div class="ps-h"><b>Selecionar Plano</b><button class="x" data-act="close">${ic('x')}</button></div>
    <label class="search ps-s">${ic('search')}<input id="psQ" type="search" placeholder="Pesquisar plano ou código..." autocomplete="off"></label>
    <div class="ps-list" id="psList"></div></div>`);
  const list = s.querySelector('#psList'), q = s.querySelector('#psQ');
  const fill = () => {
    const t = q.value.trim().toUpperCase();
    const items = plansByUse().filter(p => !t || p.name.toUpperCase().includes(t) || String(p.code).includes(t));
    const top = t ? 0 : Math.min(5, items.filter(p => use[p.id]).length);
    list.innerHTML = items.map((p, i) => `${i === 0 && top ? '<div class="ps-sec">Mais usados</div>' : ''}${i === top && top ? '<div class="ps-sec">Todos os planos</div>' : ''}
      <button type="button" data-id="${p.id}" class="${p.id === current ? 'on' : ''}"><i class="dotc" style="background:${p.color || '#E3F2FD'}"></i>
        <span><b>${esc(p.name)}</b><small>${TYPE_NAMES[p.type]} · #${esc(p.code)} · ${use[p.id] || 0} clientes</small></span><em>${money(p.price)}</em>${p.id === current ? ic('check') : ''}</button>`).join('')
      || '<div class="pp-none">Nenhum plano encontrado</div>';
    paintIcons(list);
  };
  q.oninput = fill; fill();
  list.onclick = e => { const b = e.target.closest('[data-id]'); if (!b) return; closeTop(); onPick(db.plans.find(x => x.id === b.dataset.id)); };
}
const dot2 = v => 'R$ ' + (Number(v) || 0).toFixed(2);
function clientForm(c, prefill) {
  const isNew = !c;
  c = c || Object.assign({ name: '', phone: '', planId: '', due: '', qty: 1, discount: 0, notes: '' }, prefill);
  let planId = planOf(c) ? c.planId : '';
  const s = overlay(`<div class="sheet"><form class="form" id="cform" autocomplete="off" novalidate>
    <button type="button" class="x" data-act="close">${ic('x')}</button>
    <div class="fh"><div class="ico">${ic('user')}</div><h2>${isNew ? 'Novo Cliente' : 'Editar Cliente'}</h2></div>
    <p class="sub">${isNew ? 'Preencha os dados do novo cliente' : 'Atualize as informações do cliente'}</p>
    <div class="f"><label>${ic('user', 'style="color:#a855f7"')} Nome <em>*</em></label><input id="fName" placeholder="Nome completo do cliente" value="${esc(c.name)}"></div>
    <div class="f"><label>${ic('phone', 'style="color:#3fae56"')} WhatsApp <em>*</em></label><input id="fPhone" type="tel" inputmode="tel" placeholder="(11) 99999-9999" value="${esc(fmtPhone(c.phone))}"></div>
    <div class="f2">
      <div class="f"><label>${ic('box', 'style="color:#4f46e5"')} Plano <em>*</em></label>${planPicker(planId)}</div>
      <div class="f"><label>${ic('calendar', 'style="color:#e05252"')} Vencimento <em>*</em></label><input id="fDue" type="date" value="${c.due || ''}"></div>
    </div>
    <div class="f2">
      <div class="f"><label>${ic('tag', 'style="color:#3b82f6"')} Quantidade</label><input id="fQty" type="number" min="1" inputmode="numeric" value="${c.qty || 1}"></div>
      <div class="f"><label>${ic('percent', 'style="color:#ef8a2c"')} Desconto (R$)</label><div class="pre"><span>R$</span><input id="fDisc" type="number" min="0" step="0.01" inputmode="decimal" value="${Number(c.discount) || 0}"></div></div>
    </div>
    <div class="price-sum" id="priceSum" hidden></div>
    <button type="button" class="fold" id="foldNotes">${ic('file')} ${c.notes ? 'Notas' : 'Adicionar Notas (Opcional)'} ${ic('down')}</button>
    <div class="f" id="notesBox" ${c.notes ? '' : 'hidden'}><textarea id="fNotes" placeholder="Usuário, senha, aparelho...">${esc(c.notes || '')}</textarea></div>
    <button class="btn" type="submit">${isNew ? '+ Adicionar Cliente' : '✓ Atualizar Cliente'}</button>
  </form></div>`);
  const sum = () => {
    const p = db.plans.find(x => x.id === planId), box = $('#priceSum');
    if (!p) { box.hidden = true; return; }
    const qty = Math.max(1, parseInt($('#fQty').value, 10) || 1), disc = Math.max(0, parseFloat(String($('#fDisc').value).replace(',', '.')) || 0);
    box.hidden = false;
    box.innerHTML = `<span>${dot2(p.price)}${qty > 1 ? ` × ${qty}` : ''}${disc ? ` − ${dot2(disc)}` : ''}</span><b>${dot2(Math.max(0, p.price * qty - disc))}</b>`;
  };
  $('#ppBtn').onclick = () => planSheet(planId, p => {
    planId = p.id;
    $('#ppBtn span').textContent = p.name; $('#ppBtn span').className = '';
    if (isNew || !$('#fDue').value) $('#fDue').value = addMonths(today(), TYPES[p.type] || 1);
    sum();
  });
  $('#fQty').oninput = sum; $('#fDisc').oninput = sum; sum();
  $('#foldNotes').onclick = () => { $('#notesBox').hidden = !$('#notesBox').hidden; if (!$('#notesBox').hidden) $('#fNotes').focus(); };
  s.querySelector('form').onsubmit = e => {
    e.preventDefault();
    const name = $('#fName').value.trim(), phone = normPhone($('#fPhone').value);
    if (!name) return toast('Informe o nome');
    if (phone.length < 10) return toast('WhatsApp inválido');
    if (!planId) return toast('Selecione um plano');
    if (!$('#fDue').value) return toast('Informe o vencimento');
    const data = { name, phone, planId, due: $('#fDue').value, qty: Math.max(1, parseInt($('#fQty').value, 10) || 1),
      discount: Math.max(0, parseFloat(String($('#fDisc').value).replace(',', '.')) || 0), notes: $('#fNotes').value.trim() };
    if (isNew) {
      const nc = Object.assign({ id: uid(), essential: false, archived: false, createdAt: today() }, data);
      db.clients.push(nc);
      const p = planOf(nc), pay = addPayment(nc, 'cadastro', priceOf(nc));
      log('Novo Cliente Cadastrado', `${nc.name} - Plano: ${p.name} | Valor: R$ ${priceOf(nc).toFixed(2)}${nc.discount ? ` (Desconto: R$ ${nc.discount.toFixed(2)})` : ''}`,
        { type: 'create', clientId: nc.id, paymentId: pay.id });
      setTimeout(() => successDialog(nc, 'cadastro'), 200);
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
  const s = overlay(`<div class="dialog ren">
    <div class="ren-h"><div class="c">${ic('renew')}</div><h3>Renovar Plano</h3><button class="x" data-act="close">${ic('x')}</button></div>
    <div class="ren-b">
      <div class="ren-who"><div><b>${esc(c.name)}</b><span>${esc(p.name)}${c.qty > 1 ? ` · ${c.qty} telas` : ''}</span></div>
        <div class="p"><b>${money(priceOf(c))}</b>${c.discount ? `<small>−${money(c.discount)}</small>` : ''}</div></div>
      <div class="ren-dates"><div><small>Vencimento atual</small><b>${br(c.due)}</b></div>${ic('arrowR')}<div class="new"><small>Novo vencimento</small><b>${br(next)}</b></div></div>
      <p class="ren-note">⚠️ Renovação será registrada como paga</p>${c.archived ? '<p class="ren-note" style="color:#1f6b3a">📦 O cliente sairá dos Arquivados e voltará para a lista</p>' : ''}
      <div class="two"><button class="btn light" data-act="close">Cancelar</button><button class="btn blue" id="doRenew">Confirmar</button></div>
    </div></div>`, 'center');
  s.querySelector('#doRenew').onclick = () => {
    const old = c.due, wasArchived = !!c.archived;
    c.due = next;
    c.archived = false;   // a renewed client is active again
    const pay = addPayment(c, 'renovacao', priceOf(c));
    log('Plano Renovado', `${c.name} - ${p.name} | Valor: R$ ${pay.amount.toFixed(2)} | Novo vencimento: ${br(next)}`, { type: 'renew', clientId: c.id, oldDue: old, paymentId: pay.id, wasArchived });
    snapshot(); closeTop(); refresh(); reopenDetail(c.id);
    setTimeout(() => successDialog(c, 'renovacao'), 200);
  };
}

function confirmDialog(title, text, onOk) {
  const s = overlay(`<div class="dialog confirm">${ic('alert')}<h3>${title}</h3><p>${text}</p>
    <div class="two"><button class="cancel" data-act="close">Cancelar</button><button class="ok" id="okBtn">Confirmar</button></div></div>`, 'center');
  s.querySelector('#okBtn').onclick = () => { closeTop(); onOk(); };
}

function historyPop() {
  closeMenus();
  const dayLbl = d => d === today() ? 'Hoje' : d === addDays(today(), -1) ? 'Ontem' : `${parse(d).getDate()} de ${MONTHS[parse(d).getMonth()]}`;
  let last = '', html = '';
  db.history.slice(0, 150).forEach(h => {
    const d = h.at.slice(0, 10), [e, bg] = actIcon(h);
    if (d !== last) { html += `<div class="ns-sec">${dayLbl(d)}</div>`; last = d; }
    html += `<div class="hs-it${h.undone ? ' undone' : ''}"><div class="ai" style="background:${bg}">${e}</div>
      <div class="hb"><div class="ht"><b>${esc(h.title)}</b><time>${h.at.slice(11, 16)}</time></div><p>${esc(h.desc)}</p>
      ${h.undone ? `<span class="done">${ic('checkc')} Ação desfeita</span>` : h.undo ? `<button class="undo" data-undo="${h.id}">${ic('undo')} Desfazer</button>` : ''}</div></div>`;
  });
  const s = overlay(`<div class="sheet nsheet">
    <div class="ns-h"><div class="ico" style="background:#eef0ff;color:#4f46e5">${ic('history')}</div><div><b>Histórico</b><small>${db.history.length} atividades</small></div><button class="x" data-act="close">${ic('x')}</button></div>
    <div class="ns-b">${html || `<div class="empty">${ic('history')}<div>Nenhuma ação ainda</div></div>`}</div></div>`);
  s.querySelector('.ns-b').addEventListener('click', e => { const b = e.target.closest('[data-undo]'); if (b) { e.stopPropagation(); confirmUndo(b.dataset.undo, () => { closeTop(); setTimeout(historyPop, 200); }); } });
}
/* Notifications: due today, tomorrow and yesterday; each can be marked as read (kept for the day). */
function readSet() {
  const r = db.settings.notifRead;
  if (!r || r.day !== today()) db.settings.notifRead = { day: today(), ids: [] };
  return db.settings.notifRead;
}
function notifItems() {
  const read = new Set(readSet().ids), out = [];
  const add = (kind, d) => live().forEach(c => { if (daysLeft(c) === d) { const key = kind + ':' + c.id; out.push({ key, kind, c, read: read.has(key) }); } });
  add('hoje', 0); add('amanha', 1); add('ontem', -1);
  return out;
}
const NOTIF_GROUPS = [['hoje', 'Vencem hoje', '#ea580c'], ['amanha', 'Vencem amanhã', '#ca8a04'], ['ontem', 'Venceram ontem', '#dc2626']];
function notificationsPop() {
  closeMenus();
  const items = notifItems(), unread = items.filter(n => !n.read).length;
  const body = NOTIF_GROUPS.map(([k, t, col]) => {
    const g = items.filter(n => n.kind === k); if (!g.length) return '';
    return `<div class="ns-sec"><i style="background:${col}"></i>${t}<span>${g.length}</span></div>` + g.map(n => { const c = n.c, p = planOf(c);
      return `<div class="ns-it${n.read ? ' read' : ''}" data-key="${n.key}">
        <div class="ini">${initials(c.name)}</div>
        <div class="nb" data-act="detail" data-id="${c.id}"><b>${esc(shortName(c.name))}</b><span>${p ? esc(p.name) : 'Sem plano'} · ${money(priceOf(c))}</span></div>
        <button class="wa" data-act="wa" data-id="${c.id}" aria-label="WhatsApp">${ic('wa')}</button>
        <button class="rd" data-read="${n.key}" aria-label="Marcar como lido">${ic(n.read ? 'checkc' : 'check')}</button></div>`; }).join('');
  }).join('');
  const s = overlay(`<div class="sheet nsheet">
    <div class="ns-h"><div class="ico" style="background:#fff1e6;color:#ea580c">${ic('bell')}</div><div><b>Avisos</b><small>${unread ? `${unread} não lido${unread > 1 ? 's' : ''}` : 'Tudo lido'}</small></div><button class="x" data-act="close">${ic('x')}</button></div>
    ${items.length ? `<button class="ns-all" id="readAll" ${unread ? '' : 'disabled'}>${ic('checkc')} Marcar todas como lidas</button>` : ''}
    <div class="ns-b">${body || `<div class="empty">${ic('bell')}<div>Nenhum aviso por enquanto</div></div>`}</div></div>`);
  const mark = keys => { const r = readSet(); keys.forEach(k => { if (!r.ids.includes(k)) r.ids.push(k); }); save(); updateHeader(); };
  s.querySelector('.ns-b').addEventListener('click', e => {
    const b = e.target.closest('[data-read]'); if (!b) return;
    e.stopPropagation();
    const r = readSet(), k = b.dataset.read, it = b.closest('.ns-it');
    if (r.ids.includes(k)) { r.ids = r.ids.filter(x => x !== k); save(); updateHeader(); it.classList.remove('read'); b.innerHTML = ic('check'); }
    else { mark([k]); it.classList.add('read'); b.innerHTML = ic('checkc'); }
    const left = notifItems().filter(n => !n.read).length;
    s.querySelector('.ns-h small').textContent = left ? `${left} não lido${left > 1 ? 's' : ''}` : 'Tudo lido';
    const all = s.querySelector('#readAll'); if (all) all.disabled = !left;
  });
  const all = s.querySelector('#readAll');
  if (all) all.onclick = () => { mark(items.map(n => n.key)); closeTop(); setTimeout(notificationsPop, 200); toast('Avisos marcados como lidos'); };
}
function confirmUndo(id, after) {
  const h = db.history.find(x => x.id === id); if (!h) return;
  closeMenus();
  confirmDialog('Desfazer ação?', `Tem certeza que deseja desfazer <b>${esc(h.title)}</b>?<br><small>${esc(h.desc)}</small>`, () => { undo(id); if (after) after(); });
}
function undo(id) {
  const h = db.history.find(x => x.id === id); if (!h || !h.undo || h.undone) return;
  const u = h.undo;
  const dropPay = pid => { db.payments = db.payments.filter(p => p.id !== pid); };
  if (u.type === 'create') { db.clients = db.clients.filter(c => c.id !== u.clientId); if (u.paymentId) dropPay(u.paymentId); }
  else if (u.type === 'renew') { const c = db.clients.find(x => x.id === u.clientId); if (c) { c.due = u.oldDue; if (u.wasArchived) c.archived = true; } dropPay(u.paymentId); }
  else if (u.type === 'delete') { db.clients.push(u.client); }
  else if (u.type === 'bulk-archive') { const m = new Map(u.before); db.clients.forEach(c => { if (m.has(c.id)) c.archived = m.get(c.id); }); }
  else if (u.type === 'edit') { const i = db.clients.findIndex(x => x.id === u.before.id); if (i >= 0) db.clients[i] = u.before; }
  h.undone = true;
  snapshot(); refresh(); toast('Ação desfeita');
}

/* ---------- Backup / import ---------- */
/* Converts a "Gestor Simples" export (version 2.x: data.clientes / planos / mensalidades). */
function fromGestorSimples(src) {
  const D = src.data, out = blank();
  const localDate = s => { if (!s) return ''; const t = new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(s) ? s : s + 'Z'); return isNaN(t) ? s.slice(0, 10) : iso(t); };
  const localTime = s => { if (!s) return '12:00'; const t = new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(s) ? s : s + 'Z'); return isNaN(t) ? '12:00' : `${pad(t.getHours())}:${pad(t.getMinutes())}`; };
  const num = v => { const n = parseFloat(String(v == null ? '' : v).replace(',', '.')); return isNaN(n) ? 0 : n; };
  out.plans = (D.planos || []).map(p => ({ id: String(p.id), name: String(p.nome || 'PLANO').trim(), type: TYPES[String(p.periodo).toLowerCase()] ? String(p.periodo).toLowerCase() : 'mensal',
    price: num(p.preco), cost: num(p.custo), code: String(p.codigo || ''), color: p.cor || '#E3F2FD', link: p.link || '', desc: p.descricao || '' }));
  const planById = {}, planByName = {};
  out.plans.forEach(p => { planById[p.id] = p; planByName[p.name.toUpperCase()] = p; });
  out.clients = (D.clientes || []).map(c => {
    let plan = planById[String(c.plano_id)] || planByName[String(c.plano_nome || '').trim().toUpperCase()];
    return { id: String(c.id), name: String(c.nome || '').trim() || 'Sem nome', phone: normPhone(c.whatsapp), planId: plan ? plan.id : '',
      due: /^\d{4}-\d{2}-\d{2}/.test(c.data_vencimento || '') ? c.data_vencimento.slice(0, 10) : '',
      qty: Math.max(1, parseInt(c.quantidade, 10) || 1), discount: Math.max(0, num(c.desconto)), notes: c.notas || '',
      essential: !!c.essencial, archived: String(c.status).toLowerCase() === 'arquivado', createdAt: localDate(c.created_date) || today() };
  });
  const clientById = {}; out.clients.forEach(c => { clientById[c.id] = c; });
  out.payments = (D.mensalidades || []).filter(m => !m.status || String(m.status).toLowerCase() === 'pago').map(m => {
    const c = clientById[String(m.cliente_id)];
    const desc = String(m.descricao || '');
    const nm = (desc.match(/plano(?: para)? (.+?)\s*(?:\(|$)/i) || [])[1];
    const plan = (nm && planByName[nm.trim().toUpperCase()]) || (c && planById[c.planId]);
    const qty = c ? c.qty : 1;
    const day = /^\d{4}-\d{2}-\d{2}/.test(m.data_pagamento || '') ? m.data_pagamento.slice(0, 10) : localDate(m.created_date) || today();
    return { id: String(m.id), clientId: String(m.cliente_id), clientName: m.cliente_nome || (c ? c.name : ''), planName: plan ? plan.name : (nm || ''),
      amount: num(m.valor), cost: plan ? plan.cost * qty : 0, date: `${day}T${localTime(m.created_date)}`,
      kind: String(m.metodo_pagamento).toLowerCase() === 'cadastro' || /^pagamento inicial/i.test(desc) ? 'cadastro' : 'renovacao' };
  });
  out.history = [{ id: uid(), title: 'Backup Importado', desc: `${src.app || 'Gestor Simples'} - ${out.clients.length} clientes, ${out.plans.length} planos, ${out.payments.length} pagamentos`, at: nowStamp(), undo: null, undone: false }];
  const notes = (D.notas_adesivas || []).map(n => `${n.titulo || ''}: ${n.conteudo || ''}`.trim()).filter(Boolean);
  if (notes.length) out.settings.stickyNotes = notes;
  return out;
}
/* ---------- PDF report ---------- */
function periodTitle() {
  const r = periodRange();
  if (state.period === 'tudo') return 'Todo o período';
  if (state.period === 'mes') return monLabel(state.finMonth);
  const long = d => { const x = parse(d); return `${x.getDate()} de ${MONTHS[x.getMonth()]} de ${x.getFullYear()}`; };
  if (r[0] === r[1]) return long(r[0]);
  return `${br(r[0])} a ${br(r[1])}`;
}
function reportShell(title, period, body) {
  const d = new Date(), stamp = `${br(today())} às ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return `<div class="rp">
    <div class="rp-h"><img src="logo.png" alt=""><div><h1>${title}</h1><p>Gestor Simples — Gestão de Mensalidades</p></div>
      <div class="rp-per"><small>PERÍODO</small><b>${esc(period)}</b></div></div>
    ${body}
    <div class="rp-f"><span>Relatório gerado em ${stamp}</span><span>Gestor Simples © ${d.getFullYear()}</span></div></div>`;
}
function pdf() {
  const r = periodRange();
  const pays = db.payments.filter(p => inRange(p, r)).sort((a, b) => a.date < b.date ? -1 : 1);
  const { rev, prof } = sumBy(pays), cost = rev - prof, n = new Set(pays.map(p => p.clientId)).size;
  const kp = (l, v, c) => `<div class="kp"><small>${l}</small><b style="color:${c}">${v}</b></div>`;
  $('#print').innerHTML = reportShell('Relatório Financeiro', periodTitle(), `
    <div class="rp-k">${kp('RECEITA TOTAL', money(rev), '#3e9a4f')}${kp('CUSTO TOTAL', money(cost), '#d23c3c')}${kp('LUCRO LÍQUIDO', money(prof), '#3b6fe0')}${kp('CLIENTES ATIVOS', n, '#7c3aed')}${kp('TICKET MÉDIO', money(pays.length ? rev / pays.length : 0), '#e0702c')}</div>
    <div class="rp-t"><b>Transações do Período</b><span>${pays.length} ${pays.length === 1 ? 'registro' : 'registros'}</span></div>
    <table><thead><tr><th>#</th><th>CLIENTE</th><th>PLANO</th><th class="r">VALOR</th><th class="r">CUSTO</th><th class="r">LUCRO</th><th>DATA</th><th>MÉTODO</th><th>STATUS</th></tr></thead><tbody>
    ${pays.map((p, i) => `<tr><td class="m">${i + 1}</td><td>${esc(p.clientName)}</td><td>${esc(p.planName)}</td><td class="r b">${money(p.amount)}</td><td class="r b">${money(p.cost || 0)}</td><td class="r b">${money(p.amount - (p.cost || 0))}</td><td>${br(p.date)}</td><td>${p.kind === 'cadastro' ? 'Cadastro' : 'Renovação'}</td><td><span class="pg">Pago</span></td></tr>`).join('')}
    </tbody></table>`);
  const name = `Relatorio-Financeiro-${r[0] === '0000-01-01' ? 'completo' : r[0]}`;
  if (window.Android) Android.print(name); else window.print();
}

/* ---------- Bulk selection (Clientes) ---------- */
function selStart(firstId) {
  state.sel = new Set(firstId ? [firstId] : []);
  document.body.classList.add('selecting');
  let bar = document.getElementById('selBar');
  if (!bar) { bar = document.createElement('div'); bar.id = 'selBar'; document.body.appendChild(bar); }
  selPaint();
}
function selEnd() {
  state.sel = null; document.body.classList.remove('selecting');
  const bar = document.getElementById('selBar'); if (bar) bar.remove();
  document.querySelectorAll('#clist .client.picked').forEach(c => c.classList.remove('picked'));
}
function selPaint() {
  const bar = document.getElementById('selBar'); if (!bar || !state.sel) return;
  const n = state.sel.size, list = state.list || [];
  const picked = list.filter(c => state.sel.has(c.id)), allArch = picked.length && picked.every(c => c.archived);
  const all = list.length && list.every(c => state.sel.has(c.id));
  bar.innerHTML = `<div class="sb-top"><button data-act="sel-end" aria-label="Cancelar">${ic('x')}</button><b>${n} selecionado${n === 1 ? '' : 's'}</b>
      <button class="sb-all" data-act="sel-all">${all ? 'Limpar' : `Todos (${list.length})`}</button></div>
    <button class="btn ${allArch ? 'blue' : ''}" data-act="sel-archive" ${n ? '' : 'disabled'}>${ic('archive')} ${allArch ? 'Desarquivar' : 'Arquivar'} ${n || ''}</button>`;
  document.querySelectorAll('#clist .client').forEach(c => c.classList.toggle('picked', state.sel.has(c.dataset.id)));
}
function selArchive() {
  const ids = [...state.sel], picked = db.clients.filter(c => state.sel.has(c.id));
  const archive = !picked.every(c => c.archived), n = picked.length;
  confirmDialog(archive ? 'Arquivar clientes' : 'Desarquivar clientes',
    `${archive ? 'Arquivar' : 'Desarquivar'} <b>${n}</b> cliente${n === 1 ? '' : 's'}? ${archive ? 'Eles saem da lista e dos números do Dashboard, e podem ser vistos no filtro 📦 Arquivados.' : 'Eles voltam para a lista normal.'}`, () => {
      const before = picked.map(c => [c.id, !!c.archived]);
      picked.forEach(c => { c.archived = archive; });
      log(archive ? 'Clientes Arquivados' : 'Clientes Desarquivados', `${n} cliente${n === 1 ? '' : 's'}: ${picked.slice(0, 5).map(c => c.name).join(', ')}${n > 5 ? '…' : ''}`, { type: 'bulk-archive', before });
      selEnd(); snapshot(); refresh();
      toast(`${n} cliente${n === 1 ? '' : 's'} ${archive ? 'arquivado' : 'desarquivado'}${n === 1 ? '' : 's'}`);
    });
}
// Long press on a client card starts selecting.
(() => {
  let t = 0, x = 0, y = 0;
  document.addEventListener('pointerdown', e => {
    const card = e.target.closest('#clist .client'); if (!card || state.sel) return;
    x = e.clientX; y = e.clientY;
    t = setTimeout(() => { t = 0; selStart(card.dataset.id); selPaint.long = Date.now(); if (navigator.vibrate) navigator.vibrate(20); }, 500);
  });
  const cancel = e => { if (t && (e.type !== 'pointermove' || Math.abs(e.clientX - x) + Math.abs(e.clientY - y) > 10)) { clearTimeout(t); t = 0; } };
  ['pointerup', 'pointercancel', 'pointermove'].forEach(k => document.addEventListener(k, cancel));
})();
// While selecting, a tap on a card (anywhere on it) toggles it.
document.addEventListener('click', e => {
  if (!state.sel) return;
  const card = e.target.closest('#clist .client');
  if (!card) return;
  e.stopPropagation(); e.preventDefault();
  if (selPaint.long && Date.now() - selPaint.long < 600) return;   // the click that ends the long press
  const id = card.dataset.id;
  if (state.sel.has(id)) state.sel.delete(id); else state.sel.add(id);
  selPaint();
}, true);

/* ---------- Events ---------- */
document.addEventListener('click', e => {
  const tab = e.target.closest('[data-tab]');
  if (tab) { closeMenus(); layer().innerHTML = ''; state.dirty = false; return go(tab.dataset.tab); }
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
    case 'wa': if (c) openUrl(waLink(c, waMessage(c))); break;
    case 'wa-msg': if (c) openUrl(waLink(c, waMessage(c))); break;
    case 'pays': if (c) paysSheet(c); break;
    case 'edit': if (c) clientForm(c); break;
    case 'new-client': clientForm(null); break;
    case 'renew': if (c) renewDialog(c); break;
    case 'star': if (c) {
      c.essential = !c.essential; save();
      if (state.tab === 'clientes') document.querySelectorAll(`.star[data-id="${c.id}"]`).forEach(b => { b.outerHTML = starBtn(c); }); else render();
      toast(c.essential ? 'Marcado como essencial' : 'Removido dos essenciais');
    } break;
    case 't-ess': if (c) { c.essential = !c.essential; el.querySelector('.sw').classList.toggle('on', c.essential); save(); state.dirty = true; } break;
    case 't-arc': if (c) {
      c.archived = !c.archived; el.querySelector('.sw').classList.toggle('on', c.archived); snapshot(); save(); state.dirty = true;
      toast(c.archived ? 'Cliente arquivado' : 'Cliente restaurado');
    } break;
    case 'del': if (c) confirmDialog('Confirmar Exclusão', 'Tem certeza que deseja apagar este cliente? Esta ação não pode ser desfeita.', () => {
      db.clients = db.clients.filter(x => x.id !== c.id);
      log('Cliente Excluído', `${c.name} - ${planOf(c) ? planOf(c).name : 'Sem plano'}`, { type: 'delete', client: c });
      snapshot(); refresh(); toast('Cliente apagado');
    }); break;
    case 'more': { const n = +$('#clist').dataset.n; state.show = n + 50; $('#cmore').innerHTML = ''; appendCards(n + 50, renderList.tok, n + 50); break; }
    case 'dd': {
      const k = el.dataset.k;
      const opts = { fStatus: STATUS_OPTS, fType: TYPE_OPTS, fPlan: planOpts(), show: [['20', '20'], ['50', '50'], ['100', '100'], ['0', 'Todos']], period: PERIODS,
        lucroMonth: monthOpts(), finMonth: monthOpts().map(([k]) => [k, monLabel(k)]), cmpA: monthOpts().map(([k]) => [k, monLabel(k)]), cmpB: monthOpts().map(([k]) => [k, monLabel(k)]),
        cmpDay: [['0', 'Mês inteiro']].concat(Array.from({ length: 31 }, (_, i) => [String(i + 1), `Até o dia ${i + 1}`])),
        fmtClients: FORMATS, fmtFin: FORMATS, lateType: [['todos', 'Todos os planos']].concat(db.plans.map(p => [p.id, p.name])) }[k];
      dropdown(el, opts, state[k], v => {
        state[k] = k === 'show' ? +v : v;
        if (k === 'period' || k === 'finMonth') syncMonths();
        render();
      });
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
    case 'profile': state.tab !== 'ferramentas' && go('ferramentas'); setSub('perfil'); break;
    case 'sub': setSub(el.dataset.v); break;
    case 'plan-new': planForm(null); break;
    case 'plan-edit': planForm(db.plans.find(p => p.id === id)); break;
    case 'plan-del': {
      const p = db.plans.find(x => x.id === id), n = db.clients.filter(x => x.planId === id).length;
      confirmDialog('Excluir plano', n ? `${n} clientes usam "${esc(p.name)}" e ficarão sem plano. Continuar?` : `Excluir "${esc(p.name)}"?`, () => {
        db.plans = db.plans.filter(x => x.id !== id); db.clients.forEach(x => { if (x.planId === id) x.planId = ''; });
        log('Plano Excluído', p.name); refresh();
      });
      break;
    }
    case 'plan-list': Object.assign(state, { fStatus: 'todos', fType: 'todos', fPlan: id, q: '' }); go('clientes'); break;
    case 'copy': if (window.Android && Android.copy) Android.copy(el.dataset.v); else if (navigator.clipboard) navigator.clipboard.writeText(el.dataset.v); toast('Código ' + el.dataset.v + ' copiado'); break;
    case 'open-link': openUrl(el.dataset.v); break;
    case 'pick': $('#' + el.dataset.v).click(); break;
    case 'photo': $('#photoIn').click(); break;
    case 'photo-del': db.settings.photo = ''; refresh(); break;
    case 'save-profile':
      Object.assign(db.settings, { owner: $('#prName').value.trim(), company: $('#prCompany').value.trim(), phone: $('#prPhone').value.trim() });
      refresh(); toast('Perfil salvo'); break;
    case 'save-msgs': MSG_FIELDS.forEach(([k]) => { db.settings[k] = $('#' + k).value; }); save(); toast('Mensagens salvas'); break;
    case 'clear-hist': confirmDialog('Limpar Histórico', 'Apagar todas as atividades registradas? Os clientes e pagamentos não são afetados.', () => { db.history = []; refresh(); toast('Histórico limpo'); }); break;
    case 'undo': confirmUndo(el.dataset.v); break;
    case 'sel-start': selStart(); break;
    case 'sel-end': selEnd(); break;
    case 'sel-all': { const list = state.list || [], all = list.every(c => state.sel.has(c.id)); state.sel = new Set(all ? [] : list.map(c => c.id)); if (!all) { const box = $('#clist'); appendCards(list.length, renderList.tok, list.length); } selPaint(); break; }
    case 'sel-archive': selArchive(); break;
    case 'fp-all': {
      const open = el.dataset.v === '1', closed = db.settings.finClosed || (db.settings.finClosed = {});
      document.querySelectorAll('#fpanels > .card.panel').forEach(c => { c.classList.toggle('closed', !open); const k = panelKey(c); if (open) delete closed[k]; else closed[k] = 1; });
      save(); break;
    }
    case 'cloud-check': if (window.cloudCheck) cloudCheck(); break;
    case 'sync-now': if (window.cloudSyncNow) cloudSyncNow(); break;
    case 'logout': if (window.cloudLogout) cloudLogout(); break;
    case 'dark': db.settings.dark = !db.settings.dark; applyTheme(); refresh(); break;
    case 'late-type':
      Object.assign(state, { fStatus: 'vencidos', fType: el.dataset.v, fPlan: state.lateType === 'todos' ? 'todos' : state.lateType, q: '' });
      go('clientes'); break;
    case 'report': (el.dataset.v === 'fmtClients' ? clientsReport : finReport)(state[el.dataset.v]); break;
    case 'model-csv': modelCsv(); break;
    case 'backup-now': backupNow(); break;
  }
});

/* ---------- Copied phone number ---------- */
// Called by the Android app when it regains focus with text on the clipboard.
// Returns true once the copied text was looked at (shown or ignored), false while the app isn't ready.
window.onClipboard = text => {
  if (!db || document.body.classList.contains('locked')) return false;
  text = String(text || '').trim();
  if (!text || text.length > 40) return true;
  const digits = text.replace(/\D/g, '');
  if (digits.length < 10 || digits.length > 13 || /[a-zA-Z]{3,}/.test(text)) return true;
  const phone = normPhone(text), tail = phone.slice(-8);
  const found = db.clients.filter(c => String(c.phone).slice(-8) === tail);
  const c = found.find(x => x.phone === phone) || found[0];
  showClipCard(c, phone);
  return true;
};
function showClipCard(c, phone) {
  document.querySelectorAll('.clip').forEach(x => x.remove());
  const el = document.createElement('div');
  el.className = 'clip';
  el.innerHTML = `<div class="clip-h">${ic('clipboard')}<span>Número copiado detectado</span><button data-x>${ic('x')}</button></div>
    <div class="clip-b"><div class="ini">${c ? initials(c.name) : ic('phone')}</div><div><b>${c ? esc(c.name) : 'Número novo'}</b><span class="ph">${ic('phone')} ${esc(fmtPhone(phone))}</span></div></div>
    <button class="btn" data-go>${ic(c ? 'user' : 'userplus')} ${c ? 'Ver Cliente' : 'Cadastrar Cliente'}</button>`;
  document.body.appendChild(el);
  const close = () => { el.classList.add('out'); setTimeout(() => el.remove(), 180); };
  el.querySelector('[data-x]').onclick = close;
  el.querySelector('[data-go]').onclick = () => {
    close();
    if (!c) return clientForm(null, { phone });
    // Open the client list already searched by this client's number.
    layer().innerHTML = ''; closeMenus();
    Object.assign(state, { q: fmtPhone(c.phone), fStatus: c.archived ? 'arquivados' : 'todos', fType: 'todos', fPlan: 'todos' });
    go('clientes');
  };
  setTimeout(() => { if (el.isConnected) close(); }, 15000);
}

function applyTheme() {
  document.body.classList.toggle('theme-dark', !!db.settings.dark);
  if (window.Android && Android.setTheme) Android.setTheme(!!db.settings.dark);
}

/* ---------- Start (called by cloud.js after login) ---------- */
function startApp() {
  load();
  applyTheme();
  paintIcons();
  state.sub = null;
  go('dashboard');
}
// Day rollover while the app stays open.
setInterval(() => { if (!db) return; const t = today(); if (t !== go.day) { go.day = t; snapshot(); save(); render(); } }, 60000);
go.day = today();
