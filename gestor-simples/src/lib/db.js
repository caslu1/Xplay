// Acesso às entidades. Mesma interface para Supabase e para o modo demonstração.
// - Cache em memória com TTL de 5 minutos, invalidado a cada escrita na entidade.
// - list() pagina no servidor (fetchAllRecords) para não truncar listas grandes.
import { supabase, DEMO } from './supabase';

export const TABELAS = {
  Cliente: 'clientes',
  Plano: 'planos',
  Mensalidade: 'mensalidades',
  ConfiguracaoMensagem: 'configuracoes_mensagem',
  HistoricoMensagem: 'historico_mensagens',
  NotaAdesiva: 'notas_adesivas',
  Tarefa: 'tarefas',
  Meta: 'metas',
};

const TTL = 5 * 60 * 1000;
const PAGINA = 1000;
const cache = new Map(); // tabela -> { at, data }

export const DATA_EVENT = 'gs:data-changed';

function invalidar(tabela) {
  cache.delete(tabela);
  window.dispatchEvent(new CustomEvent(DATA_EVENT, { detail: { tabela } }));
}

export function limparCache() {
  cache.clear();
}

// ---------------------------------------------------------------- modo demo
const DEMO_USER = 'demo-user';
const lsKey = (t) => `gs_demo_${t}`;
function lerLocal(t) {
  try {
    return JSON.parse(localStorage.getItem(lsKey(t)) || '[]');
  } catch {
    return [];
  }
}
function gravarLocal(t, rows) {
  localStorage.setItem(lsKey(t), JSON.stringify(rows));
}
const uuid = () => (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2) + Date.now().toString(36));

// ---------------------------------------------------------------- helpers
function ordenar(rows, order) {
  if (!order) return rows;
  const desc = order.startsWith('-');
  const campo = desc ? order.slice(1) : order;
  return [...rows].sort((a, b) => {
    const x = a[campo] ?? '', y = b[campo] ?? '';
    if (x < y) return desc ? 1 : -1;
    if (x > y) return desc ? -1 : 1;
    return 0;
  });
}

function limpar(data) {
  const out = { ...data };
  for (const k of Object.keys(out)) if (out[k] === undefined) delete out[k];
  return out;
}

/** Busca todas as linhas, página por página (o PostgREST limita cada resposta). */
async function fetchAllRecords(tabela, order) {
  const desc = order?.startsWith('-');
  const campo = order ? order.replace(/^-/, '') : 'created_date';
  const todos = [];
  for (let from = 0; ; from += PAGINA) {
    const { data, error } = await supabase
      .from(tabela)
      .select('*')
      .order(campo, { ascending: !desc })
      .order('id')
      .range(from, from + PAGINA - 1);
    if (error) throw error;
    todos.push(...data);
    if (data.length < PAGINA) return todos;
  }
}

function makeEntity(tabela) {
  return {
    tabela,

    async list(order = '-created_date', { force = false } = {}) {
      const hit = cache.get(tabela);
      if (!force && hit && Date.now() - hit.at < TTL) return ordenar(hit.data, order);
      const data = DEMO ? lerLocal(tabela) : await fetchAllRecords(tabela, order);
      cache.set(tabela, { at: Date.now(), data });
      return ordenar(data, order);
    },

    async filter(criterios = {}, order) {
      const rows = await this.list(order);
      return rows.filter((r) => Object.entries(criterios).every(([k, v]) => r[k] === v));
    },

    async get(id) {
      return (await this.list()).find((r) => r.id === id) || null;
    },

    async create(data) {
      const [row] = await this.bulkCreate([data]);
      return row;
    },

    async bulkCreate(lista) {
      if (!lista.length) return [];
      let rows;
      if (DEMO) {
        const agora = new Date().toISOString();
        rows = lista.map((d) => ({ id: uuid(), created_date: agora, updated_date: agora, created_by: DEMO_USER, ...limpar(d) }));
        gravarLocal(tabela, [...lerLocal(tabela), ...rows]);
      } else {
        const out = [];
        for (let i = 0; i < lista.length; i += 500) {
          const { data, error } = await supabase.from(tabela).insert(lista.slice(i, i + 500).map(limpar)).select();
          if (error) throw error;
          out.push(...data);
        }
        rows = out;
      }
      invalidar(tabela);
      return rows;
    },

    async update(id, data) {
      let row;
      if (DEMO) {
        const all = lerLocal(tabela);
        const i = all.findIndex((r) => r.id === id);
        if (i < 0) throw new Error('Registro não encontrado');
        all[i] = row = { ...all[i], ...limpar(data), updated_date: new Date().toISOString() };
        gravarLocal(tabela, all);
      } else {
        const { data: d, error } = await supabase.from(tabela).update(limpar(data)).eq('id', id).select().single();
        if (error) throw error;
        row = d;
      }
      invalidar(tabela);
      return row;
    },

    async bulkUpdate(ids, data) {
      if (!ids.length) return;
      if (DEMO) {
        const set = new Set(ids);
        gravarLocal(tabela, lerLocal(tabela).map((r) => (set.has(r.id) ? { ...r, ...limpar(data), updated_date: new Date().toISOString() } : r)));
      } else {
        for (let i = 0; i < ids.length; i += 200) {
          const { error } = await supabase.from(tabela).update(limpar(data)).in('id', ids.slice(i, i + 200));
          if (error) throw error;
        }
      }
      invalidar(tabela);
    },

    /** Insere ou substitui pelo id (usado pelo "Desfazer" e pelo backup). */
    async upsert(lista) {
      if (!lista.length) return;
      if (DEMO) {
        const all = lerLocal(tabela);
        const pos = new Map(all.map((r, i) => [r.id, i]));
        for (const r of lista) pos.has(r.id) ? (all[pos.get(r.id)] = r) : all.push({ created_by: DEMO_USER, ...r });
        gravarLocal(tabela, all);
      } else {
        for (let i = 0; i < lista.length; i += 500) {
          const { error } = await supabase.from(tabela).upsert(lista.slice(i, i + 500).map(limpar));
          if (error) throw error;
        }
      }
      invalidar(tabela);
    },

    async delete(id) {
      return this.bulkDelete([id]);
    },

    async bulkDelete(ids) {
      if (!ids.length) return;
      if (DEMO) {
        const set = new Set(ids);
        gravarLocal(tabela, lerLocal(tabela).filter((r) => !set.has(r.id)));
      } else {
        for (let i = 0; i < ids.length; i += 200) {
          const { error } = await supabase.from(tabela).delete().in('id', ids.slice(i, i + 200));
          if (error) throw error;
        }
      }
      invalidar(tabela);
    },
  };
}

export const db = Object.fromEntries(Object.entries(TABELAS).map(([nome, t]) => [nome, makeEntity(t)]));
