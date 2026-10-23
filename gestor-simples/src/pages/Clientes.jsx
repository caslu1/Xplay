import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Users, Plus, Search, LayoutGrid, List, RefreshCw, MessageCircle, Pencil, Trash2, Archive, Star, Receipt, Upload,
  ClipboardPaste, MoreVertical, ChevronLeft, ChevronRight, X, ArchiveRestore,
} from 'lucide-react';
import { useEntities, useLocalState } from '../lib/hooks';
import { usePopup } from '../context/PopupContext';
import { criarCliente, editarCliente, renovarCliente, excluirClientes, mudarStatusClientes } from '../lib/acoes';
import { db } from '../lib/db';
import {
  cn, formatarDataBR, formatarMoeda, formatarTelefone, situacaoVencimento, valorCliente, STATUS_CLIENTE, hojeISO, diffDias,
} from '../lib/utils';
import { Button, Card, Badge, Input, Select, PageHeader, Empty, Spinner, Tabs, IconButton } from '../components/ui';
import ClienteForm from '../components/ClienteForm';
import MensagemModal from '../components/MensagemModal';
import RenovarModal from '../components/RenovarModal';
import PagamentosModal from '../components/PagamentosModal';

const POR_PAGINA = 24;

/** Tenta reconhecer um cliente em um texto copiado (ex.: "Nome: Ana\nWhatsApp: 11 99999-9999"). */
export function lerClienteDoTexto(texto) {
  const t = String(texto || '').trim();
  if (!t || t.length > 500) return null;
  const tel = t.match(/(?:\+?55\s?)?\(?\d{2}\)?\s?9?\d{4}[-\s]?\d{4}/);
  if (!tel) return null;
  const nomeRotulo = t.match(/(?:nome|cliente)\s*[:\-]\s*(.+)/i);
  let nome = nomeRotulo?.[1]?.trim();
  if (!nome) {
    nome = t.split(/\n/).map((l) => l.trim()).find((l) => l && !/\d{4}/.test(l) && l.length <= 60);
  }
  if (!nome) return null;
  const venc = t.match(/(\d{2})\/(\d{2})\/(\d{4})/);
  return {
    nome: nome.replace(/[*_]/g, '').slice(0, 80),
    whatsapp: tel[0].replace(/\D/g, ''),
    ...(venc ? { data_vencimento: `${venc[3]}-${venc[2]}-${venc[1]}` } : {}),
  };
}

export default function Clientes() {
  const { data, loading } = useEntities(['Cliente', 'Plano']);
  const { confirm, toast } = usePopup();
  const [params, setParams] = useSearchParams();
  const [busca, setBusca] = useState(params.get('busca') || '');
  const [aba, setAba] = useState(params.get('filtro') || 'ativos');
  const [planoF, setPlanoF] = useState('');
  const [periodo, setPeriodo] = useState('');
  const [ordem, setOrdem] = useLocalState('gs_clientes_ordem', 'vencimento');
  const [visao, setVisao] = useLocalState('gs_clientes_visao', 'lista');
  const [pagina, setPagina] = useState(1);
  const [sel, setSel] = useState(new Set());
  const [form, setForm] = useState({ open: false, cliente: null, inicial: null });
  const [msg, setMsg] = useState({ open: false });
  const [renovar, setRenovar] = useState(null);
  const [historico, setHistorico] = useState(null);
  const [copiado, setCopiado] = useState(null);

  const planoPor = useMemo(() => new Map(data.Plano.map((p) => [p.id, p])), [data.Plano]);
  const hoje = hojeISO();

  useEffect(() => {
    const b = params.get('busca');
    if (b != null) setBusca(b);
    if (params.get('novo')) setForm({ open: true, cliente: null });
  }, [params]);

  // Detecta cliente copiado na área de transferência quando a janela volta ao foco.
  useEffect(() => {
    let ultimo = '';
    async function checar() {
      try {
        const perm = await navigator.permissions?.query({ name: 'clipboard-read' });
        if (perm && perm.state !== 'granted') return;
        const texto = await navigator.clipboard.readText();
        if (!texto || texto === ultimo) return;
        ultimo = texto;
        const c = lerClienteDoTexto(texto);
        if (c && !data.Cliente.some((x) => x.whatsapp.replace(/\D/g, '').endsWith(c.whatsapp.slice(-9)))) setCopiado(c);
      } catch { /* sem permissão */ }
    }
    window.addEventListener('focus', checar);
    return () => window.removeEventListener('focus', checar);
  }, [data.Cliente]);

  const contagem = useMemo(() => {
    const c = { ativos: 0, vencendo: 0, vencidos: 0, pendente: 0, inativo: 0, arquivado: 0, essenciais: 0, todos: data.Cliente.length };
    for (const x of data.Cliente) {
      const d = diffDias(hoje, String(x.data_vencimento).slice(0, 10));
      if (x.status === 'ativo') {
        c.ativos++;
        if (d >= 0 && d <= 7) c.vencendo++;
        if (d < 0) c.vencidos++;
      }
      if (x.status in c) c[x.status]++;
      if (x.essencial && x.status !== 'arquivado') c.essenciais++;
    }
    return c;
  }, [data.Cliente, hoje]);

  const filtrados = useMemo(() => {
    const q = busca.trim().toLowerCase();
    const qDig = q.replace(/\D/g, '');
    let l = data.Cliente.filter((c) => {
      const d = diffDias(hoje, String(c.data_vencimento).slice(0, 10));
      if (aba === 'ativos' && c.status !== 'ativo') return false;
      if (aba === 'vencendo' && !(c.status === 'ativo' && d >= 0 && d <= 7)) return false;
      if (aba === 'vencidos' && !(c.status === 'ativo' && d < 0)) return false;
      if (['pendente', 'inativo', 'arquivado'].includes(aba) && c.status !== aba) return false;
      if (aba === 'essenciais' && !(c.essencial && c.status !== 'arquivado')) return false;
      if (aba === 'todos' && c.status === 'arquivado') return false;
      if (planoF && c.plano_id !== planoF) return false;
      if (periodo === 'hoje' && d !== 0) return false;
      if (periodo === '7' && !(d >= 0 && d <= 7)) return false;
      if (periodo === '30' && !(d >= 0 && d <= 30)) return false;
      if (periodo === 'mes' && String(c.data_vencimento).slice(0, 7) !== hoje.slice(0, 7)) return false;
      if (periodo === 'prox_mes') {
        const [y, m] = hoje.split('-').map(Number);
        const prox = m === 12 ? `${y + 1}-01` : `${y}-${String(m + 1).padStart(2, '0')}`;
        if (String(c.data_vencimento).slice(0, 7) !== prox) return false;
      }
      if (q && !(c.nome.toLowerCase().includes(q) || (qDig.length >= 3 && c.whatsapp.includes(qDig)) || (c.notas || '').toLowerCase().includes(q) || (c.plano_nome || '').toLowerCase().includes(q))) return false;
      return true;
    });
    const cmp = {
      vencimento: (a, b) => String(a.data_vencimento).localeCompare(String(b.data_vencimento)),
      nome: (a, b) => a.nome.localeCompare(b.nome, 'pt-BR'),
      recentes: (a, b) => String(b.created_date).localeCompare(String(a.created_date)),
      valor: (a, b) => valorCliente(b, planoPor.get(b.plano_id)) - valorCliente(a, planoPor.get(a.plano_id)),
    }[ordem];
    l = [...l].sort(cmp);
    return l;
  }, [data.Cliente, busca, aba, planoF, periodo, ordem, hoje, planoPor]);

  useEffect(() => { setPagina(1); setSel(new Set()); }, [busca, aba, planoF, periodo]);

  const paginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA));
  const visiveis = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
  const selecionados = data.Cliente.filter((c) => sel.has(c.id));

  const alternar = (id) => setSel((s) => { const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n; });
  const todosVisiveisSel = visiveis.length > 0 && visiveis.every((c) => sel.has(c.id));

  async function salvar(dados, plano, opts) {
    if (form.cliente) {
      await editarCliente(form.cliente, dados, plano);
      toast('Cliente atualizado');
    } else {
      const novo = await criarCliente(dados, plano, opts);
      setMsg({ open: true, cliente: novo, modelo: 'whatsapp_cadastro_message', sucesso: 'Cliente cadastrado! 🎉', tipo: 'boas_vindas' });
    }
  }

  async function confirmarRenovacao(opts) {
    const c = renovar;
    const plano = planoPor.get(c.plano_id);
    const { cliente } = await renovarCliente(c, plano, opts);
    setMsg({ open: true, cliente, modelo: 'whatsapp_message', sucesso: 'Renovação confirmada! ✅', tipo: 'renovacao' });
  }

  async function excluir(lista) {
    const ok = await confirm({
      title: lista.length === 1 ? `Excluir ${lista[0].nome}?` : `Excluir ${lista.length} clientes?`,
      message: 'Você pode desfazer pelo histórico de atividades.', danger: true, confirmText: 'Excluir',
    });
    if (!ok) return;
    await excluirClientes(lista);
    setSel(new Set());
    toast(lista.length === 1 ? 'Cliente excluído' : `${lista.length} clientes excluídos`);
  }

  async function status(lista, s) {
    await mudarStatusClientes(lista, s);
    setSel(new Set());
    toast(s === 'arquivado' ? `${lista.length} arquivado(s)` : `${lista.length} atualizado(s)`);
  }

  async function colar() {
    try {
      const texto = await navigator.clipboard.readText();
      const c = lerClienteDoTexto(texto);
      if (c) setForm({ open: true, cliente: null, inicial: c });
      else toast('Não encontrei nome e WhatsApp no texto copiado', 'error');
    } catch {
      toast('Permita o acesso à área de transferência para colar', 'error');
    }
  }

  if (loading) return <Spinner />;

  const abas = [
    { value: 'ativos', label: 'Ativos', count: contagem.ativos },
    { value: 'vencendo', label: 'Vencendo (7d)', count: contagem.vencendo },
    { value: 'vencidos', label: 'Vencidos', count: contagem.vencidos },
    { value: 'essenciais', label: '★ Essenciais', count: contagem.essenciais },
    { value: 'pendente', label: 'Pendentes', count: contagem.pendente },
    { value: 'inativo', label: 'Inativos', count: contagem.inativo },
    { value: 'arquivado', label: 'Arquivados', count: contagem.arquivado },
    { value: 'todos', label: 'Todos' },
  ];

  const acoesCliente = (c) => {
    const plano = planoPor.get(c.plano_id);
    return [
      { label: 'Renovar', icon: RefreshCw, onClick: () => setRenovar(c) },
      { label: 'WhatsApp', icon: MessageCircle, onClick: () => setMsg({ open: true, cliente: c, plano, modelo: situacaoVencimento(c.data_vencimento).dias < 0 ? 'whatsapp_expirado_message' : 'whatsapp_message' }) },
      { label: 'Histórico de pagamentos', icon: Receipt, onClick: () => setHistorico(c) },
      { label: 'Editar', icon: Pencil, onClick: () => setForm({ open: true, cliente: c }) },
      { label: c.essencial ? 'Remover dos essenciais' : 'Marcar como essencial', icon: Star, onClick: async () => { await db.Cliente.update(c.id, { essencial: !c.essencial }); } },
      c.status === 'arquivado'
        ? { label: 'Desarquivar', icon: ArchiveRestore, onClick: () => status([c], 'ativo') }
        : { label: 'Arquivar', icon: Archive, onClick: () => status([c], 'arquivado') },
      { label: 'Excluir', icon: Trash2, danger: true, onClick: () => excluir([c]) },
    ];
  };

  return (
    <div>
      <PageHeader icon={Users} title="Clientes" subtitle={`${contagem.ativos} ativos · ${contagem.todos} no total`}
        actions={<>
          <Button variant="secondary" icon={ClipboardPaste} onClick={colar} className="hidden sm:inline-flex">Colar cliente</Button>
          <Link to="/ImportarClientes"><Button variant="secondary" icon={Upload}>Importar</Button></Link>
          <Button icon={Plus} onClick={() => setForm({ open: true, cliente: null })}>Novo cliente</Button>
        </>} />

      {copiado && (
        <Card className="mb-4 flex flex-wrap items-center gap-3 border-brand-200 bg-brand-50 p-3">
          <ClipboardPaste className="h-5 w-5 text-brand-600" />
          <p className="flex-1 text-sm">Encontramos um cliente copiado: <b>{copiado.nome}</b> · {formatarTelefone(copiado.whatsapp)}</p>
          <Button size="sm" onClick={() => { setForm({ open: true, cliente: null, inicial: copiado }); setCopiado(null); }}>Cadastrar</Button>
          <IconButton icon={X} title="Dispensar" onClick={() => setCopiado(null)} />
        </Card>
      )}

      <div className="mb-3"><Tabs tabs={abas} value={aba} onChange={(v) => { setAba(v); setParams({}, { replace: true }); }} /></div>

      <Card className="mb-4 p-3">
        <div className="flex flex-col gap-2 md:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
            <Input className="pl-9" placeholder="Buscar por nome, WhatsApp, plano ou nota…" value={busca} onChange={(e) => setBusca(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-2 md:flex">
            <Select className="md:w-40" value={planoF} onChange={(e) => setPlanoF(e.target.value)}>
              <option value="">Todos os planos</option>
              {data.Plano.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
            </Select>
            <Select className="md:w-44" value={periodo} onChange={(e) => setPeriodo(e.target.value)}>
              <option value="">Qualquer vencimento</option>
              <option value="hoje">Vence hoje</option>
              <option value="7">Próximos 7 dias</option>
              <option value="30">Próximos 30 dias</option>
              <option value="mes">Este mês</option>
              <option value="prox_mes">Próximo mês</option>
            </Select>
            <Select className="col-span-2 md:w-40" value={ordem} onChange={(e) => setOrdem(e.target.value)}>
              <option value="vencimento">Por vencimento</option>
              <option value="nome">Por nome</option>
              <option value="recentes">Mais recentes</option>
              <option value="valor">Maior valor</option>
            </Select>
          </div>
          <div className="hidden gap-1 rounded-xl bg-slate-100 p-1 md:flex">
            <IconButton icon={List} title="Lista" className={cn('h-8 w-8', visao === 'lista' && 'bg-white text-brand-600 shadow-sm')} onClick={() => setVisao('lista')} />
            <IconButton icon={LayoutGrid} title="Grade" className={cn('h-8 w-8', visao === 'grade' && 'bg-white text-brand-600 shadow-sm')} onClick={() => setVisao('grade')} />
          </div>
        </div>
      </Card>

      {sel.size > 0 && (
        <div className="animate-pop sticky top-16 z-20 mb-3 flex flex-wrap items-center gap-2 rounded-2xl bg-slate-900 px-4 py-2.5 text-white shadow-xl lg:top-16">
          <span className="mr-auto text-sm font-semibold">{sel.size} selecionado(s)</span>
          <Button size="sm" variant="ghost" className="!text-white hover:!bg-white/10" icon={Archive} onClick={() => status(selecionados, 'arquivado')}>Arquivar</Button>
          <Button size="sm" variant="ghost" className="!text-white hover:!bg-white/10" onClick={() => status(selecionados, 'ativo')}>Ativar</Button>
          <Button size="sm" variant="ghost" className="!text-white hover:!bg-white/10" onClick={() => status(selecionados, 'inativo')}>Inativar</Button>
          <Button size="sm" variant="danger" icon={Trash2} onClick={() => excluir(selecionados)}>Excluir</Button>
          <IconButton icon={X} title="Limpar seleção" className="text-white hover:bg-white/10 hover:text-white" onClick={() => setSel(new Set())} />
        </div>
      )}

      {filtrados.length === 0 ? (
        <Card>
          <Empty icon={Users} title={data.Cliente.length ? 'Nenhum cliente encontrado' : 'Nenhum cliente ainda'}
            action={!data.Cliente.length && <Button icon={Plus} onClick={() => setForm({ open: true, cliente: null })}>Cadastrar o primeiro</Button>}>
            {data.Cliente.length ? 'Tente mudar a busca ou os filtros.' : data.Plano.length ? 'Cadastre seu primeiro cliente ou importe uma planilha.' : 'Comece cadastrando um plano em Planos.'}
          </Empty>
        </Card>
      ) : visao === 'grade' ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {visiveis.map((c) => <ClienteCard key={c.id} c={c} plano={planoPor.get(c.plano_id)} sel={sel.has(c.id)} onSel={() => alternar(c.id)} acoes={acoesCliente(c)} />)}
        </div>
      ) : (
        <Card className="overflow-hidden">
          <div className="hidden items-center gap-3 border-b border-slate-100 bg-slate-50/60 px-4 py-2.5 text-xs font-bold uppercase tracking-wide text-slate-400 md:flex">
            <input type="checkbox" className="h-4 w-4 accent-brand-600" checked={todosVisiveisSel}
              onChange={() => setSel((s) => { const n = new Set(s); visiveis.forEach((c) => (todosVisiveisSel ? n.delete(c.id) : n.add(c.id))); return n; })} />
            <span className="flex-1">Cliente</span><span className="w-32">Plano</span><span className="w-40">Vencimento</span><span className="w-24 text-right">Valor</span><span className="w-[140px]" />
          </div>
          <div className="divide-y divide-slate-100">
            {visiveis.map((c) => <ClienteLinha key={c.id} c={c} plano={planoPor.get(c.plano_id)} sel={sel.has(c.id)} onSel={() => alternar(c.id)} acoes={acoesCliente(c)} />)}
          </div>
        </Card>
      )}

      {paginas > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <IconButton icon={ChevronLeft} title="Anterior" disabled={pagina === 1} onClick={() => setPagina(pagina - 1)} />
          <span className="text-sm text-slate-500">Página <b>{pagina}</b> de {paginas} · {filtrados.length} clientes</span>
          <IconButton icon={ChevronRight} title="Próxima" disabled={pagina === paginas} onClick={() => setPagina(pagina + 1)} />
        </div>
      )}

      <ClienteForm open={form.open} cliente={form.cliente} inicial={form.inicial} planos={data.Plano} onClose={() => setForm({ open: false })} onSave={salvar} />
      <MensagemModal open={msg.open} cliente={msg.cliente} plano={msg.cliente && (msg.plano || planoPor.get(msg.cliente.plano_id))}
        modelo={msg.modelo} sucesso={msg.sucesso} tipo={msg.tipo} onClose={() => setMsg({ open: false })} />
      <RenovarModal open={!!renovar} cliente={renovar} plano={renovar && planoPor.get(renovar.plano_id)} onClose={() => setRenovar(null)} onConfirm={confirmarRenovacao} />
      <PagamentosModal open={!!historico} cliente={historico} onClose={() => setHistorico(null)} />
    </div>
  );
}

function Menu({ acoes }) {
  const [aberto, setAberto] = useState(false);
  return (
    <div className="relative">
      <IconButton icon={MoreVertical} title="Mais ações" onClick={() => setAberto(!aberto)} />
      {aberto && (
        <>
          <div className="fixed inset-0 z-30" onClick={() => setAberto(false)} />
          <div className="animate-pop absolute right-0 top-10 z-40 w-56 overflow-hidden rounded-2xl border border-slate-200 bg-white py-1 shadow-2xl">
            {acoes.map((a) => (
              <button key={a.label} onClick={() => { setAberto(false); a.onClick(); }}
                className={cn('flex w-full items-center gap-2.5 px-3.5 py-2 text-left text-sm hover:bg-slate-50', a.danger ? 'text-red-600' : 'text-slate-700')}>
                <a.icon className="h-4 w-4" /> {a.label}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function Avatar({ c, plano }) {
  return (
    <div className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-full font-bold text-slate-700" style={{ background: plano?.cor || '#e2e8f0' }}>
      {c.nome[0]?.toUpperCase()}
      {c.essencial && <Star className="absolute -right-1 -top-1 h-4 w-4 fill-amber-400 text-amber-500" />}
    </div>
  );
}

function ClienteLinha({ c, plano, sel, onSel, acoes }) {
  const s = situacaoVencimento(c.data_vencimento);
  const st = STATUS_CLIENTE[c.status];
  return (
    <div className={cn('flex items-center gap-3 px-4 py-3 transition hover:bg-slate-50/70', sel && 'bg-brand-50/60')}>
      <input type="checkbox" className="h-4 w-4 shrink-0 accent-brand-600" checked={sel} onChange={onSel} />
      <Avatar c={c} plano={plano} />
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-2 truncate font-semibold text-slate-800">
          {c.nome}
          {c.status !== 'ativo' && <Badge className={st.cls}>{st.label}</Badge>}
        </p>
        <p className="truncate text-xs text-slate-400">
          {formatarTelefone(c.whatsapp)}{c.quantidade > 1 && ` · ${c.quantidade} conexões`}
          <span className="md:hidden"> · {c.plano_nome}</span>
        </p>
        <p className={cn('text-xs font-semibold md:hidden', s.cls)}>{formatarDataBR(c.data_vencimento)} · {s.label}</p>
      </div>
      <div className="hidden w-32 md:block"><Badge className="text-slate-700" ><span className="h-2 w-2 rounded-full" style={{ background: plano?.cor }} />{c.plano_nome}</Badge></div>
      <div className="hidden w-40 md:block">
        <p className="text-sm font-medium">{formatarDataBR(c.data_vencimento)}</p>
        <p className={cn('text-xs font-semibold', s.cls)}>{s.label}</p>
      </div>
      <p className="hidden w-24 text-right text-sm font-bold text-slate-700 md:block">{formatarMoeda(valorCliente(c, plano))}</p>
      <div className="flex items-center gap-0.5">
        <IconButton icon={RefreshCw} title="Renovar" className="hidden text-emerald-600 hover:bg-emerald-50 sm:inline-flex" onClick={acoes[0].onClick} />
        <IconButton icon={MessageCircle} title="WhatsApp" className="text-[#25D366] hover:bg-green-50" onClick={acoes[1].onClick} />
        <Menu acoes={acoes} />
      </div>
    </div>
  );
}

function ClienteCard({ c, plano, sel, onSel, acoes }) {
  const s = situacaoVencimento(c.data_vencimento);
  const st = STATUS_CLIENTE[c.status];
  return (
    <Card className={cn('p-4 transition hover:shadow-md', sel && 'ring-2 ring-brand-400')}>
      <div className="flex items-start gap-3">
        <Avatar c={c} plano={plano} />
        <div className="min-w-0 flex-1">
          <p className="truncate font-bold">{c.nome}</p>
          <p className="text-xs text-slate-400">{formatarTelefone(c.whatsapp)}</p>
        </div>
        <input type="checkbox" className="mt-1 h-4 w-4 accent-brand-600" checked={sel} onChange={onSel} />
        <Menu acoes={acoes} />
      </div>
      <div className="mt-3 grid grid-cols-2 gap-2 text-sm">
        <div className="rounded-xl bg-slate-50 p-2"><p className="text-[11px] text-slate-400">Plano</p><p className="truncate font-semibold">{c.plano_nome}{c.quantidade > 1 && ` ×${c.quantidade}`}</p></div>
        <div className="rounded-xl bg-slate-50 p-2"><p className="text-[11px] text-slate-400">Valor</p><p className="font-semibold">{formatarMoeda(valorCliente(c, plano))}</p></div>
      </div>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400">Vence {formatarDataBR(c.data_vencimento)}</p>
          <p className={cn('text-xs font-bold', s.cls)}>{s.label}</p>
        </div>
        {c.status !== 'ativo' && <Badge className={st.cls}>{st.label}</Badge>}
      </div>
      <div className="mt-3 grid grid-cols-2 gap-2">
        <Button size="sm" variant="secondary" icon={RefreshCw} onClick={acoes[0].onClick}>Renovar</Button>
        <Button size="sm" variant="whatsapp" icon={MessageCircle} onClick={acoes[1].onClick}>WhatsApp</Button>
      </div>
    </Card>
  );
}
