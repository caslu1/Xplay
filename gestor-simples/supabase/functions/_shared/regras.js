// Regras de negócio compartilhadas entre o app (modo demonstração) e as Edge Functions.
// JavaScript puro, sem dependências: roda no navegador e no Deno.

export const FUSO = 'America/Sao_Paulo';

/** Data de hoje (AAAA-MM-DD) no fuso de Brasília. */
export function hojeISO(agora = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: FUSO, year: 'numeric', month: '2-digit', day: '2-digit' }).format(agora);
}

/** Hora atual (0–23) no fuso de Brasília. */
export function horaAtual(agora = new Date()) {
  return Number(new Intl.DateTimeFormat('en-GB', { timeZone: FUSO, hour: '2-digit', hour12: false }).format(agora)) % 24;
}

const ms = (iso) => Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10));

/** Dias de `de` até `ate` (positivo se `ate` é depois). */
export function diffDias(de, ate) {
  return Math.round((ms(ate) - ms(de)) / 86400000);
}

export function addDias(iso, dias) {
  return new Date(ms(iso) + dias * 86400000).toISOString().slice(0, 10);
}

export const MESES_PERIODO = { mensal: 1, trimestral: 3, semestral: 6, anual: 12 };

/** Soma o período do plano a uma data, mantendo o dia (31/01 + 1 mês = 28/02). */
export function addPeriodo(iso, periodo = 'mensal') {
  const meses = MESES_PERIODO[periodo] || 1;
  const y = +iso.slice(0, 4), m = +iso.slice(5, 7) - 1, d = +iso.slice(8, 10);
  const alvo = new Date(Date.UTC(y, m + meses, 1));
  const ultimo = new Date(Date.UTC(alvo.getUTCFullYear(), alvo.getUTCMonth() + 1, 0)).getUTCDate();
  alvo.setUTCDate(Math.min(d, ultimo));
  return alvo.toISOString().slice(0, 10);
}

/** Valor da mensalidade: preço × quantidade − desconto. */
export function valorCliente(cliente, plano) {
  const preco = Number(plano?.preco || 0);
  const qtd = Number(cliente?.quantidade || 1);
  return Math.max(0, preco * qtd - Number(cliente?.desconto || 0));
}

export function custoCliente(cliente, plano) {
  return Number(plano?.custo || 0) * Number(cliente?.quantidade || 1);
}

/** Só dígitos, com DDI 55 quando faltar. */
export function limparNumero(numero) {
  let n = String(numero || '').replace(/\D/g, '');
  if (n.startsWith('00')) n = n.slice(2);
  if (n.length === 10 || n.length === 11) n = '55' + n;
  return n;
}

export function formatarDataBR(iso) {
  if (!iso) return '';
  const [y, m, d] = String(iso).slice(0, 10).split('-');
  return `${d}/${m}/${y}`;
}

export function formatarMoeda(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

/**
 * Troca as variáveis do template. Aceita os dois estilos usados no app:
 * {nome} {valor} {data} {plano} {empresa} {link} e {{cliente}} {{plano}} {{vencimento}} {{valor}}.
 */
export function aplicarTemplate(template, { cliente, plano, empresa } = {}) {
  const vars = {
    nome: cliente?.nome || '',
    cliente: cliente?.nome || '',
    valor: formatarMoeda(valorCliente(cliente, plano)),
    data: formatarDataBR(cliente?.data_vencimento),
    vencimento: formatarDataBR(cliente?.data_vencimento),
    plano: plano?.nome || cliente?.plano_nome || '',
    link: plano?.link || '',
    quantidade: String(cliente?.quantidade || 1),
    empresa: empresa || '',
  };
  return String(template || '')
    .replace(/\{\{\s*(\w+)\s*\}\}/g, (m, k) => (k in vars ? vars[k] : m))
    .replace(/\{(\w+)\}/g, (m, k) => (k in vars ? vars[k] : m));
}

/**
 * Decide se a configuração `config` manda mensagem para `cliente` hoje.
 * vencimento_proximo: X dias antes · vencimento_hoje · atraso / plano_expirado: X dias depois ·
 * boas_vindas: cliente criado hoje.
 */
export function deveEnviar(config, cliente, hoje = hojeISO()) {
  if (!config?.ativo || !cliente || cliente.status !== 'ativo' || !cliente.data_vencimento) return false;
  const faltam = diffDias(hoje, String(cliente.data_vencimento).slice(0, 10));
  switch (config.tipo) {
    case 'vencimento_proximo':
      return faltam === Number(config.dias_antes || 0) && faltam > 0;
    case 'vencimento_hoje':
      return faltam === 0;
    case 'atraso':
    case 'plano_expirado':
      return -faltam === Number(config.dias_apos || 0) && faltam < 0;
    case 'boas_vindas':
      return hojeISO(new Date(cliente.created_date || 0)) === hoje;
    default:
      return false; // renovação é enviada no momento da renovação
  }
}

/** A hora do envio chegou? (horario_envio "HH:MM" → compara a hora cheia) */
export function horaDeEnviar(config, agora = new Date()) {
  const h = Number(String(config?.horario_envio || '09:00').split(':')[0]);
  return h === horaAtual(agora);
}

/** Já mandou esse tipo de mensagem para esse cliente hoje? */
export function jaEnviadoHoje(historico, cliente_id, tipo, hoje = hojeISO()) {
  return historico.some(
    (h) => h.cliente_id === cliente_id && h.tipo_mensagem === tipo && h.status === 'enviada' && hojeISO(new Date(h.data_envio)) === hoje
  );
}
