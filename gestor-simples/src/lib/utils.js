export * from '../../supabase/functions/_shared/regras.js';
import { hojeISO, diffDias } from '../../supabase/functions/_shared/regras.js';

export const cn = (...c) => c.filter(Boolean).join(' ');

export const STATUS_CLIENTE = {
  ativo: { label: 'Ativo', cls: 'bg-emerald-100 text-emerald-700' },
  inativo: { label: 'Inativo', cls: 'bg-slate-100 text-slate-600' },
  pendente: { label: 'Pendente', cls: 'bg-amber-100 text-amber-700' },
  arquivado: { label: 'Arquivado', cls: 'bg-violet-100 text-violet-700' },
};

export const PERIODOS = { mensal: 'Mensal', trimestral: 'Trimestral', semestral: 'Semestral', anual: 'Anual' };

export const METODOS = {
  pix: 'PIX', dinheiro: 'Dinheiro', cartao: 'Cartão', transferencia: 'Transferência', renovacao: 'Renovação', cadastro: 'Cadastro',
};

export const CORES_PASTEL = ['#ddd6fe', '#bfdbfe', '#bbf7d0', '#fde68a', '#fecaca', '#fbcfe8', '#a5f3fc', '#fed7aa', '#e9d5ff', '#d9f99d'];

export function formatarTelefone(n) {
  const d = String(n || '').replace(/\D/g, '').replace(/^55(?=\d{10,11}$)/, '');
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return n || '';
}

/** Situação do vencimento em relação a hoje. */
export function situacaoVencimento(iso, hoje = hojeISO()) {
  if (!iso) return { dias: null, label: '—', cls: 'text-slate-400' };
  const dias = diffDias(hoje, String(iso).slice(0, 10));
  if (dias < 0) return { dias, label: `Venceu há ${-dias} dia${dias === -1 ? '' : 's'}`, cls: 'text-red-600' };
  if (dias === 0) return { dias, label: 'Vence hoje', cls: 'text-orange-600' };
  if (dias <= 3) return { dias, label: `Vence em ${dias} dia${dias === 1 ? '' : 's'}`, cls: 'text-amber-600' };
  return { dias, label: `Vence em ${dias} dias`, cls: 'text-emerald-600' };
}

export function mesISO(iso) {
  return String(iso || '').slice(0, 7);
}

export function nomeMes(ym) {
  const [y, m] = ym.split('-').map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' }).replace('.', '');
}

export function nomeMesLongo(ym) {
  const [y, m] = ym.split('-').map(Number);
  const s = new Date(y, m - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  return s.charAt(0).toUpperCase() + s.slice(1);
}

/** Lista de meses AAAA-MM entre dois meses (inclusive). */
export function mesesEntre(de, ate) {
  const out = [];
  let [y, m] = de.split('-').map(Number);
  const [y2, m2] = ate.split('-').map(Number);
  while (y < y2 || (y === y2 && m <= m2)) {
    out.push(`${y}-${String(m).padStart(2, '0')}`);
    if (++m > 12) { m = 1; y++; }
  }
  return out;
}

export function addMeses(ym, n) {
  let [y, m] = ym.split('-').map(Number);
  m += n;
  while (m > 12) { m -= 12; y++; }
  while (m < 1) { m += 12; y--; }
  return `${y}-${String(m).padStart(2, '0')}`;
}

export function copiar(texto) {
  if (navigator.clipboard?.writeText) return navigator.clipboard.writeText(texto);
  const ta = document.createElement('textarea');
  ta.value = texto;
  document.body.appendChild(ta);
  ta.select();
  document.execCommand('copy');
  ta.remove();
  return Promise.resolve();
}

export function baixarArquivo(nome, conteudo, tipo = 'application/json') {
  const blob = conteudo instanceof Blob ? conteudo : new Blob([conteudo], { type: tipo });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = nome;
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 1000);
}

export function linkWhatsApp(numero, mensagem) {
  const n = String(numero || '').replace(/\D/g, '');
  const comDDI = n.length === 10 || n.length === 11 ? '55' + n : n;
  return `https://wa.me/${comDDI}?text=${encodeURIComponent(mensagem || '')}`;
}

// DDD → UF, para o gráfico de clientes por estado.
const DDD_UF = {
  AC: [68], AL: [82], AP: [96], AM: [92, 97], BA: [71, 73, 74, 75, 77], CE: [85, 88], DF: [61], ES: [27, 28],
  GO: [62, 64], MA: [98, 99], MT: [65, 66], MS: [67], MG: [31, 32, 33, 34, 35, 37, 38], PA: [91, 93, 94], PB: [83],
  PR: [41, 42, 43, 44, 45, 46], PE: [81, 87], PI: [86, 89], RJ: [21, 22, 24], RN: [84], RS: [51, 53, 54, 55],
  RO: [69], RR: [95], SC: [47, 48, 49], SP: [11, 12, 13, 14, 15, 16, 17, 18, 19], SE: [79], TO: [63],
};
const UF_POR_DDD = Object.fromEntries(Object.entries(DDD_UF).flatMap(([uf, ddds]) => ddds.map((d) => [d, uf])));

export function estadoPorWhatsApp(numero) {
  let d = String(numero || '').replace(/\D/g, '');
  if (d.startsWith('55') && d.length >= 12) d = d.slice(2);
  return UF_POR_DDD[Number(d.slice(0, 2))] || 'Outros';
}
