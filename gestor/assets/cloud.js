'use strict';
/* Login (Supabase Auth) and cloud storage of the whole database (table gestor_data, one row per account).
   The phone keeps a full local copy per account, so the app works offline and uploads when it can. */

const CFG_KEY = 'gs-server', SES_KEY = 'gs-session', SYNC_PREFIX = 'gs-sync:';
const DEVICE = localStorage.getItem('gs-device') || (() => { const d = Math.random().toString(36).slice(2) + Date.now().toString(36); localStorage.setItem('gs-device', d); return d; })();
const cloud = {
  cfg: null, session: null, timer: 0, busy: false, status: 'idle', error: '',
};
// Same Supabase project as the Xplay panel (table gestor_data, see supabase/gestor.sql).
const DEFAULT_SERVER = { url: 'https://xgubfucijtaazvuzcluw.supabase.co', key: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhndWJmdWNpanRhYXp2dXpjbHV3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI3MzcxMDIsImV4cCI6MjEwODMxMzEwMn0.Bjd6ZmbeGBc6_nxjVD8F0rtamnOKKHLQ-uMpCTjnCNI' };
function loadCfg() {
  localStorage.removeItem(CFG_KEY);   // a server typed in older builds is no longer used
  return (cloud.cfg = DEFAULT_SERVER);
}
function loadSession() { try { cloud.session = JSON.parse(localStorage.getItem(SES_KEY)); } catch (e) { cloud.session = null; } return cloud.session; }
function storeSession(s) { cloud.session = s; if (s) localStorage.setItem(SES_KEY, JSON.stringify(s)); else localStorage.removeItem(SES_KEY); }
const syncMeta = () => { try { return JSON.parse(localStorage.getItem(SYNC_PREFIX + cloud.session.user.id)) || {}; } catch (e) { return {}; } };
const setSyncMeta = m => localStorage.setItem(SYNC_PREFIX + cloud.session.user.id, JSON.stringify(Object.assign(syncMeta(), m)));

/* ---------- HTTP ---------- */
async function api(path, opts = {}) {
  const c = cloud.cfg, headers = Object.assign({ apikey: c.key, 'Content-Type': 'application/json' }, opts.headers || {});
  if (opts.auth) headers.Authorization = 'Bearer ' + cloud.session.access_token;
  let res;
  try { res = await fetch(c.url.replace(/\/+$/, '') + path, { method: opts.method || 'GET', headers, body: opts.body ? JSON.stringify(opts.body) : undefined }); }
  catch (e) { const err = new Error('Sem conexão com o servidor'); err.offline = true; throw err; }
  const text = await res.text();
  let json = null; try { json = text ? JSON.parse(text) : null; } catch (e) { }
  if (!res.ok) {
    const msg = (json && (json.msg || json.error_description || json.message || json.error)) || ('Erro ' + res.status);
    const err = new Error(authMsg(msg)); err.status = res.status; throw err;
  }
  return json;
}
function authMsg(m) {
  const t = String(m).toLowerCase();
  if (t.includes('invalid login')) return 'E-mail ou senha incorretos';
  if (t.includes('email not confirmed')) return 'Confirme seu e-mail antes de entrar (veja sua caixa de entrada)';
  if (t.includes('already registered') || t.includes('already been registered')) return 'Este e-mail já tem conta. Use "Entrar".';
  if (t.includes('password should be') || t.includes('at least 6')) return 'A senha precisa ter pelo menos 6 caracteres';
  if (t.includes('invalid email') || t.includes('unable to validate email')) return 'E-mail inválido';
  if (t.includes('signups not allowed') || t.includes('signup is disabled')) return 'Novas contas estão desativadas neste servidor';
  if (t.includes('rate limit')) return 'Muitas tentativas. Aguarde um pouco.';
  return m;
}
const withExpiry = s => Object.assign(s, { expires_at: s.expires_at || Math.floor(Date.now() / 1000) + (s.expires_in || 3600) });
async function ensureToken() {
  const s = cloud.session;
  if (s.expires_at - 60 > Date.now() / 1000) return;
  const r = await api('/auth/v1/token?grant_type=refresh_token', { method: 'POST', body: { refresh_token: s.refresh_token } });
  storeSession(withExpiry(r));
}
async function authed(path, opts) {
  await ensureToken();
  try { return await api(path, Object.assign({ auth: true }, opts)); }
  catch (e) {
    if (e.status === 401) { cloud.session.expires_at = 0; await ensureToken(); return api(path, Object.assign({ auth: true }, opts)); }
    throw e;
  }
}

/* ---------- Sync ---------- */
function setStatus(st, err) {
  cloud.status = st; cloud.error = err || '';
  const el = document.getElementById('syncDot');
  if (el) { el.className = 'sync-dot ' + st; el.title = { ok: 'Sincronizado', busy: 'Sincronizando…', off: 'Sem conexão — salvo no aparelho', err: 'Erro ao sincronizar', idle: '' }[st] || ''; }
  const p = document.getElementById('syncInfo'); if (p) p.innerHTML = syncText();
}
function syncText() {
  const m = cloud.session ? syncMeta() : {};
  const when = m.lastSync ? new Date(m.lastSync).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }) : 'nunca';
  if (cloud.status === 'busy') return 'Sincronizando…';
  if (cloud.status === 'off') return `Sem conexão — os dados estão salvos no aparelho e serão enviados quando a internet voltar. Última sincronização: ${when}`;
  if (cloud.status === 'err') return `Erro: ${esc(cloud.error)}. Última sincronização: ${when}`;
  return `Sincronizado · ${when}`;
}
async function remoteStamp() {
  const r = await authed('/rest/v1/gestor_data?select=updated_at');
  return r && r[0] ? r[0].updated_at : null;
}
async function pullData() {
  const r = await authed('/rest/v1/gestor_data?select=data,updated_at');
  return r && r[0] ? r[0] : null;
}
async function pushData() {
  const r = await authed('/rest/v1/gestor_data?on_conflict=user_id', {
    method: 'POST', headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
    // _sync marks which phone wrote this copy, so a write whose answer got lost is not taken for another phone's.
    body: { user_id: cloud.session.user.id, data: Object.assign({}, db, { _sync: { device: DEVICE, at: new Date().toISOString() } }) },
  });
  return r && r[0] ? r[0].updated_at : new Date().toISOString();
}
/* Called after every local save. */
window.onPersist = () => {
  if (!cloud.session) return;
  // rev counts local changes, so an edit made while an upload is running is not marked as sent.
  setSyncMeta({ dirty: true, rev: (syncMeta().rev || 0) + 1 });
  clearTimeout(cloud.timer); cloud.timer = setTimeout(() => sync(), 2500);
};
async function sync(opts = {}) {
  if (!cloud.session) return;
  if (cloud.busy) { cloud.again = true; return; }   // run once more when the current sync ends
  cloud.busy = true; setStatus('busy');
  try {
    const meta = syncMeta(), stamp = await remoteStamp();
    const remoteNewer = stamp && (!meta.lastSync || new Date(stamp) > new Date(meta.lastSync));
    if (remoteNewer && meta.dirty && !opts.force) {
      const row = await pullData();
      const verdict = row ? compareCopies(row.data) : 'local';
      if (verdict === 'same') { setSyncMeta({ lastSync: row.updated_at, dirty: false }); setStatus('ok'); return; }
      if (verdict === 'conflict') { cloud.busy = false; setStatus('ok'); return conflict(row); }
      opts = Object.assign({}, opts, { force: true });   // this phone's copy already contains the cloud's
    }
    if (remoteNewer && !opts.force) {
      const row = await pullData();
      if (row) { applyRemote(row.data); setSyncMeta({ lastSync: row.updated_at, dirty: false }); }
    } else if (meta.dirty || !stamp || opts.force) {
      flushLocal();
      const rev = syncMeta().rev || 0;
      const at = await pushData();
      setSyncMeta({ lastSync: at, dirty: (syncMeta().rev || 0) !== rev });
    }
    setStatus(syncMeta().dirty ? 'busy' : 'ok');
  } catch (e) {
    setStatus(e.offline ? 'off' : 'err', e.message);
    if (e.status === 401 || e.status === 400 && /refresh/i.test(e.message)) { toast('Sessão expirada. Entre de novo.'); logout(true); }
  } finally {
    cloud.busy = false;
    if (cloud.again || syncMeta().dirty && cloud.status === 'busy') { cloud.again = false; setTimeout(() => sync(), 300); }
  }
}
// Postgres jsonb reorders object keys, so compare with keys sorted.
function canon(v) {
  if (Array.isArray(v)) return '[' + v.map(canon).join(',') + ']';
  if (v && typeof v === 'object') return '{' + Object.keys(v).filter(k => v[k] !== undefined).sort().map(k => JSON.stringify(k) + ':' + canon(v[k])).join(',') + '}';
  return JSON.stringify(v);
}
/* Compares this phone with the cloud copy, item by item. */
async function cloudCheck() {
  flushLocal();
  toast('Conferindo a nuvem…');
  try {
    const row = await pullData(), r = row && row.data || {};
    const cnt = d => ({ c: (d.clients || []).length, p: (d.plans || []).length, g: (d.payments || []).length, h: (d.history || []).length });
    const a = cnt(db), b = cnt(r);
    const ids = new Set((r.clients || []).map(c => c.id)), missing = db.clients.filter(c => !ids.has(c.id)).length;
    const same = canon(r) === canon(db);
    const line = (t, x, y) => `<tr><td>${t}</td><td>${x}</td><td>${y}</td><td>${x === y ? '✅' : '⚠️'}</td></tr>`;
    const when = row ? new Date(row.updated_at).toLocaleString('pt-BR') : '—';
    overlay(`<div class="dialog confirm chk">${ic(same ? 'checkc' : 'alert', `style="color:${same ? '#23944f' : '#d97706'}"`)}
      <h3>${same ? 'Tudo salvo na nuvem' : 'Há diferenças'}</h3>
      <table><tr><th></th><th>Aparelho</th><th>Nuvem</th><th></th></tr>${line('Clientes', a.c, b.c)}${line('Planos', a.p, b.p)}${line('Pagamentos', a.g, b.g)}${line('Histórico', a.h, b.h)}</table>
      <p>${same ? `A cópia da nuvem é idêntica à deste aparelho.` : missing ? `${missing} cliente(s) deste aparelho ainda não estão na nuvem. Toque em Enviar agora.` : 'A nuvem tem uma versão diferente. Toque em Sincronizar.'}<br><small>Última gravação na nuvem: ${when}</small></p>
      <div class="two">${same ? '' : '<button class="cancel" id="chkSend">Enviar agora</button>'}<button class="ok" style="background:#4f46e5" data-act="close">OK</button></div></div>`, 'center');
    const b2 = document.getElementById('chkSend');
    if (b2) b2.onclick = () => { closeTop(); setSyncMeta({ dirty: true }); sync({ force: true }); };
  } catch (e) { toast('Não foi possível conferir: ' + e.message); }
}
window.cloudCheck = cloudCheck;
const stripSync = d => { if (!d) return d; const x = Object.assign({}, d); delete x._sync; return x; };
/* 'same': nothing to do; 'local': safe to upload this phone's copy; 'conflict': another phone changed things this one lacks. */
function compareCopies(remote) {
  if (!remote || !remote.clients) return 'local';
  if (canon(stripSync(remote)) === canon(stripSync(db))) return 'same';
  if (remote._sync && remote._sync.device === DEVICE) return 'local';   // last cloud write came from this phone
  const has = (mine, theirs) => { const ids = new Set((mine || []).map(x => x.id)); return (theirs || []).every(x => ids.has(x.id)); };
  // Everything in the cloud also exists here (only additions/edits on this phone): keep this phone.
  if (has(db.clients, remote.clients) && has(db.payments, remote.payments) && has(db.plans, remote.plans)) return 'local';
  return 'conflict';
}
function flushLocal() { if (window.flushSave) flushSave(); }
function applyRemote(data) {
  if (!data || !data.clients) return;
  db = stripSync(data); migrate(); invalidate();
  try { localStorage.setItem(KEY, JSON.stringify(db)); } catch (e) { }
  applyTheme(); render();
}
function conflict(row) {
  if (document.getElementById('cfDlg')) return;
  const r = row.data || {}, when = new Date(row.updated_at).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  const n = (d, k) => (d[k] || []).length;
  const line = (t, k) => `<tr><td>${t}</td><td>${n(db, k)}</td><td>${n(r, k)}</td></tr>`;
  const s = overlay(`<div class="dialog confirm chk" id="cfDlg">${ic('alert')}<h3>Dados diferentes</h3>
    <p>Esta conta foi alterada em outro aparelho (${when}) e este aparelho também tem alterações. Compare e escolha qual cópia manter:</p>
    <table><tr><th></th><th>Este aparelho</th><th>Nuvem</th></tr>${line('Clientes', 'clients')}${line('Planos', 'plans')}${line('Pagamentos', 'payments')}</table>
    <div class="two" style="flex-direction:column;gap:10px"><button class="ok" style="background:#4f46e5" id="useLocal">Manter a deste aparelho</button><button class="cancel" id="useCloud">Usar a da nuvem</button></div></div>`, 'center');
  s.querySelector('#useCloud').onclick = () => { closeTop(); applyRemote(row.data); setSyncMeta({ dirty: false, lastSync: row.updated_at }); setStatus('ok'); };
  s.querySelector('#useLocal').onclick = () => { closeTop(); sync({ force: true }); };
}
addEventListener('online', () => sync());
document.addEventListener('visibilitychange', () => { if (!document.hidden) sync(); });
setInterval(() => { if (!document.hidden) sync(); }, 120000);

/* ---------- Screens ---------- */
function authShell(inner) {
  let el = document.getElementById('auth');
  if (!el) { el = document.createElement('div'); el.id = 'auth'; document.body.appendChild(el); }
  document.body.classList.add('locked');
  el.innerHTML = `<div class="au-card"><img src="logo.png" alt=""><h1>Gestor Simples</h1><p class="au-sub">Gestão de Mensalidades</p>${inner}</div>`;
  paintIcons(el);
  return el;
}
function showLogin(mode) {
  mode = mode || 'login';
  const signup = mode === 'signup';
  const el = authShell(`<div class="au-tabs"><button data-m="login" class="${signup ? '' : 'on'}">Entrar</button><button data-m="signup" class="${signup ? 'on' : ''}">Criar conta</button></div>
    <form id="auForm" novalidate>
      ${signup ? `<label>Nome<input id="auName" autocomplete="name" placeholder="Seu nome"></label>` : ''}
      <label>E-mail<input id="auEmail" type="email" inputmode="email" autocomplete="email" autocapitalize="off" placeholder="voce@email.com" value="${esc(localStorage.getItem('gs-last-email') || '')}"></label>
      <label>Senha<span class="pw"><input id="auPass" type="password" autocomplete="${signup ? 'new-password' : 'current-password'}" placeholder="${signup ? 'Mínimo 6 caracteres' : 'Sua senha'}"><button type="button" id="auEye">${ic('eye')}</button></span></label>
      ${signup ? `<label>Confirmar senha<input id="auPass2" type="password" autocomplete="new-password"></label>` : ''}
      <p class="au-err" id="auErr" hidden></p>
      <button class="btn" type="submit" id="auGo">${signup ? 'Criar conta' : 'Entrar'}</button>
    </form>
    ${signup ? '' : '<button class="au-link" id="auForgot">Esqueci minha senha</button>'}`);
  el.querySelectorAll('.au-tabs button').forEach(b => b.onclick = () => showLogin(b.dataset.m));
  el.querySelector('#auEye').onclick = () => { const i = el.querySelector('#auPass'); i.type = i.type === 'password' ? 'text' : 'password'; };
  const err = m => { const p = el.querySelector('#auErr'); p.textContent = m; p.hidden = !m; };
  const f = el.querySelector('#auForgot');
  if (f) f.onclick = async () => {
    const email = el.querySelector('#auEmail').value.trim();
    if (!email) return err('Digite seu e-mail para receber o link de nova senha');
    try { await api('/auth/v1/recover', { method: 'POST', body: { email } }); err(''); toast('Enviamos um link para ' + email); }
    catch (e) { err(e.message); }
  };
  el.querySelector('#auForm').onsubmit = async e => {
    e.preventDefault();
    const email = el.querySelector('#auEmail').value.trim().toLowerCase(), pass = el.querySelector('#auPass').value;
    if (!/^\S+@\S+\.\S+$/.test(email)) return err('E-mail inválido');
    if (pass.length < 6) return err('A senha precisa ter pelo menos 6 caracteres');
    if (signup && pass !== el.querySelector('#auPass2').value) return err('As senhas não conferem');
    const btn = el.querySelector('#auGo'); btn.disabled = true; btn.textContent = 'Aguarde…'; err('');
    try {
      let s;
      if (signup) {
        const name = el.querySelector('#auName').value.trim();
        s = await api('/auth/v1/signup', { method: 'POST', body: { email, password: pass, data: { name } } });
        if (!s || !s.access_token) { btn.disabled = false; btn.textContent = 'Criar conta'; showLogin('login'); toast('Conta criada! Confirme pelo link enviado ao seu e-mail e depois entre.'); return; }
      } else {
        s = await api('/auth/v1/token?grant_type=password', { method: 'POST', body: { email, password: pass } });
      }
      localStorage.setItem('gs-last-email', email);
      storeSession(withExpiry(s));
      await enterApp(signup ? el.querySelector('#auName').value.trim() : '');
    } catch (ex) { err(ex.message); btn.disabled = false; btn.textContent = signup ? 'Criar conta' : 'Entrar'; }
  };
}
function hideAuth() { const el = document.getElementById('auth'); if (el) el.remove(); document.body.classList.remove('locked'); }

/* ---------- Session lifecycle ---------- */
async function enterApp(name) {
  const uid = cloud.session.user.id;
  KEY = 'gs-data:' + uid;
  const hadLocal = !!localStorage.getItem(KEY);
  // First login on this phone: bring the data that was here before accounts existed.
  if (!hadLocal && localStorage.getItem(LEGACY_KEY) && !localStorage.getItem('gs-legacy-owner')) {
    localStorage.setItem(KEY, localStorage.getItem(LEGACY_KEY)); localStorage.setItem('gs-legacy-owner', uid);
    localStorage.removeItem(LEGACY_KEY);
  }
  startApp();
  if (name && !db.settings.owner) { db.settings.owner = name; save(); }
  hideAuth();
  if (window.Android && Android.checkClip) setTimeout(() => Android.checkClip(), 300);
  // Bring the cloud copy (another phone) or upload this one.
  try {
    setStatus('busy');
    const row = await pullData(), m = syncMeta();
    const localHas = db.clients.length > 0, remoteHas = row && row.data && row.data.clients && row.data.clients.length > 0;
    if (!row) setSyncMeta({ dirty: true });
    else if (m.lastSync) { /* known device: regular sync below */ }
    else if (localHas && remoteHas) {
      const v = compareCopies(row.data);
      if (v === 'conflict') { setStatus('ok'); return conflict(row); }
      setSyncMeta({ lastSync: row.updated_at, dirty: v !== 'same' });
    }
    else if (remoteHas || !localHas) { applyRemote(row.data); setSyncMeta({ lastSync: row.updated_at, dirty: false }); }
    else setSyncMeta({ dirty: true, lastSync: row.updated_at });
    setStatus('ok');
  } catch (e) { setStatus(e.offline ? 'off' : 'err', e.message); }
  snapshot(); save();
  sync();
}
async function logout(silent) {
  const go2 = async () => {
    flushLocal();
    try { if (cloud.session) await api('/auth/v1/logout', { method: 'POST', auth: true }); } catch (e) { }
    storeSession(null); layer().innerHTML = ''; closeMenus();
    showLogin('login');
  };
  if (silent === true) return go2();
  const pending = cloud.session && syncMeta().dirty;
  confirmDialog('Sair da conta', pending ? 'Há alterações ainda não enviadas para a nuvem. Se sair agora sem internet, elas ficam só neste aparelho. Sair mesmo assim?' : 'Seus dados continuam salvos na nuvem. Para voltar, entre com seu e-mail e senha.', go2);
}
window.cloudLogout = logout;
window.cloudSyncNow = () => { setSyncMeta({}); sync(); };
window.cloudInfo = () => ({ email: cloud.session && cloud.session.user.email, text: syncText(), status: cloud.status });

(function boot() {
  loadCfg();
  if (!loadSession()) return showLogin('login');
  enterApp('');
})();
