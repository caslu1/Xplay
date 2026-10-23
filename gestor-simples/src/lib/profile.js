// Perfil do usuário (tabela profiles) e histórico de atividades com "Desfazer".
import { supabase, DEMO } from './supabase';
import { db } from './db';

export const PROFILE_EVENT = 'gs:profile-changed';
export const ACTIVITY_EVENT = 'gs:activity-undone';

const DEMO_KEY = 'gs_demo_profile';
const PADRAO = {
  id: 'demo-user',
  email: 'demo@gestorsimples.app',
  full_name: 'Usuário Demo',
  empresa: 'Minha Empresa',
  role: 'user',
  onboarding_completed: false,
  profile_photo: null,
  dashboard_settings: { notificar_vencimentos: true, dias_antecedencia: 3 },
  whatsapp_message: 'Olá {{cliente}}! Seu plano {{plano}} vence em {{vencimento}}. Valor: {{valor}}. — {empresa}',
  whatsapp_cadastro_message: 'Olá {{cliente}}, seja bem-vindo(a) à {empresa}! 🎉\nPlano: {{plano}}\nVencimento: {{vencimento}}\nValor: {{valor}}',
  whatsapp_expirado_message: 'Oi {{cliente}}, seu plano {{plano}} expirou em {{vencimento}}. Quer renovar? Valor: {{valor}}.',
  pin_lock_enabled: false,
  pin_lock_code: null,
  activity_logs: [],
};

let atual = null;

export async function carregarPerfil(user) {
  if (DEMO) {
    try {
      atual = { ...PADRAO, ...JSON.parse(localStorage.getItem(DEMO_KEY) || '{}') };
    } catch {
      atual = { ...PADRAO };
    }
    return atual;
  }
  const { data, error } = await supabase.from('profiles').select('*').eq('id', user.id).maybeSingle();
  if (error) throw error;
  if (!data) {
    const novo = { id: user.id, email: user.email, full_name: user.user_metadata?.full_name || user.email.split('@')[0] };
    const { data: criado, error: e2 } = await supabase.from('profiles').upsert(novo).select().single();
    if (e2) throw e2;
    atual = { ...PADRAO, ...criado };
  } else {
    atual = { ...PADRAO, ...Object.fromEntries(Object.entries(data).filter(([, v]) => v !== null)) };
  }
  return atual;
}

export function perfilAtual() {
  return atual;
}

export async function atualizarPerfil(campos) {
  atual = { ...atual, ...campos };
  if (DEMO) {
    localStorage.setItem(DEMO_KEY, JSON.stringify(atual));
  } else {
    const { error } = await supabase.from('profiles').update(campos).eq('id', atual.id);
    if (error) throw error;
  }
  window.dispatchEvent(new CustomEvent(PROFILE_EVENT, { detail: atual }));
  return atual;
}

// ---------------------------------------------------------------- atividades
const MAX_LOGS = 100;
const TRES_DIAS = 3 * 24 * 60 * 60 * 1000;

function podar(logs) {
  const limite = Date.now() - TRES_DIAS;
  return logs.filter((l) => new Date(l.data).getTime() >= limite).slice(0, MAX_LOGS);
}

/**
 * Registra uma ação. `desfazer` é a lista de operações que revertem a ação:
 *   { op: 'delete', entidade: 'Cliente', ids: [...] }
 *   { op: 'restore', entidade: 'Cliente', registros: [snapshot, ...] }
 */
export async function registrarAtividade({ acao, entidade, descricao, desfazer = [] }) {
  if (!atual) return;
  const entrada = {
    id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    data: new Date().toISOString(),
    acao,
    entidade,
    descricao,
    desfazer,
    desfeita: false,
  };
  await atualizarPerfil({ activity_logs: podar([entrada, ...(atual.activity_logs || [])]) }).catch(() => {});
}

export async function desfazerAtividade(id) {
  const log = (atual.activity_logs || []).find((l) => l.id === id);
  if (!log || log.desfeita) return;
  for (const passo of log.desfazer || []) {
    const ent = db[passo.entidade];
    if (!ent) continue;
    if (passo.op === 'delete') await ent.bulkDelete(passo.ids || []);
    if (passo.op === 'restore') await ent.upsert(passo.registros || []);
  }
  await atualizarPerfil({ activity_logs: atual.activity_logs.map((l) => (l.id === id ? { ...l, desfeita: true } : l)) });
  window.dispatchEvent(new CustomEvent(ACTIVITY_EVENT, { detail: log }));
}

/** Limpeza automática (chamada ao abrir o app). */
export async function limparAtividadesAntigas() {
  if (!atual) return;
  const podado = podar(atual.activity_logs || []);
  if (podado.length !== (atual.activity_logs || []).length) await atualizarPerfil({ activity_logs: podado }).catch(() => {});
}
