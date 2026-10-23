import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Users, DollarSign, AlertTriangle, CalendarClock, Star, CheckSquare, Plus, MessageCircle, RefreshCw, ArrowRight, Bell, StickyNote, TrendingUp,
} from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { useAuth } from '../context/AuthContext';
import { usePopup } from '../context/PopupContext';
import { db } from '../lib/db';
import { renovarCliente } from '../lib/acoes';
import { cn, formatarDataBR, formatarMoeda, hojeISO, situacaoVencimento, valorCliente, diffDias } from '../lib/utils';
import { Card, StatCard, Spinner, Button, Badge, IconButton } from '../components/ui';
import MensagemModal from '../components/MensagemModal';
import RenovarModal from '../components/RenovarModal';

const COR_NOTA = { amarelo: 'bg-amber-100', verde: 'bg-emerald-100', azul: 'bg-sky-100', rosa: 'bg-pink-100', roxo: 'bg-violet-100', laranja: 'bg-orange-100' };
const PRIORIDADE = { alta: 'bg-red-100 text-red-600', media: 'bg-amber-100 text-amber-700', baixa: 'bg-slate-100 text-slate-500' };

export default function Dashboard() {
  const { profile } = useAuth();
  const { toast } = usePopup();
  const { data, loading } = useEntities(['Cliente', 'Plano', 'Mensalidade', 'Tarefa', 'NotaAdesiva', 'HistoricoMensagem']);
  const [msg, setMsg] = useState(null);
  const [renovar, setRenovar] = useState(null);
  const hoje = hojeISO();
  const mes = hoje.slice(0, 7);
  const planoPor = useMemo(() => new Map(data.Plano.map((p) => [p.id, p])), [data.Plano]);

  const r = useMemo(() => {
    const ativos = data.Cliente.filter((c) => c.status === 'ativo');
    const comDias = ativos.map((c) => ({ c, d: diffDias(hoje, String(c.data_vencimento).slice(0, 10)) }));
    const pagosMes = data.Mensalidade.filter((m) => m.status === 'pago' && String(m.data_pagamento).slice(0, 7) === mes);
    const receitaMes = pagosMes.reduce((s, m) => s + Number(m.valor), 0);
    const previsto = ativos.filter((c) => String(c.data_vencimento).slice(0, 7) === mes).reduce((s, c) => s + valorCliente(c, planoPor.get(c.plano_id)), 0);
    return {
      ativos: ativos.length,
      receitaMes,
      lucroMes: receitaMes - pagosMes.reduce((s, m) => s + Number(m.custo || 0), 0),
      previsto,
      vencidos: comDias.filter((x) => x.d < 0).sort((a, b) => b.d - a.d),
      proximos: comDias.filter((x) => x.d >= 0 && x.d <= 7).sort((a, b) => a.d - b.d),
      essenciais: data.Cliente.filter((c) => c.essencial && c.status !== 'arquivado'),
      tarefas: data.Tarefa.filter((t) => t.status !== 'concluida').sort((a, b) => String(a.data_prazo || '9').localeCompare(String(b.data_prazo || '9'))),
      notas: data.NotaAdesiva.filter((n) => n.fixada),
      erros: data.HistoricoMensagem.filter((h) => h.status === 'erro' && diffDias(String(h.data_envio).slice(0, 10), hoje) <= 2),
      enviadasHoje: data.HistoricoMensagem.filter((h) => h.status === 'enviada' && String(h.data_envio).slice(0, 10) === hoje).length,
    };
  }, [data, hoje, mes, planoPor]);

  if (loading) return <Spinner />;
  const saudacao = new Date().getHours() < 12 ? 'Bom dia' : new Date().getHours() < 18 ? 'Boa tarde' : 'Boa noite';

  const LinhaCliente = ({ c, compacto }) => {
    const s = situacaoVencimento(c.data_vencimento);
    return (
      <div className="flex items-center gap-3 py-2.5">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold" style={{ background: planoPor.get(c.plano_id)?.cor || '#e2e8f0' }}>{c.nome[0]}</div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold">{c.nome}</p>
          <p className="truncate text-xs text-slate-400">{c.plano_nome} · {formatarDataBR(c.data_vencimento)}</p>
          <p className={cn('text-xs font-bold', !compacto && 'sm:hidden', s.cls)}>{s.label}</p>
        </div>
        {!compacto && <span className={cn('hidden text-xs font-bold sm:block', s.cls)}>{s.label}</span>}
        <IconButton icon={RefreshCw} title="Renovar" className="text-emerald-600 hover:bg-emerald-50" onClick={() => setRenovar(c)} />
        <IconButton icon={MessageCircle} title="WhatsApp" className="text-[#25D366] hover:bg-green-50" onClick={() => setMsg({ cliente: c, modelo: s.dias < 0 ? 'whatsapp_expirado_message' : 'whatsapp_message' })} />
      </div>
    );
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">{saudacao}, {profile?.full_name?.split(' ')[0]} 👋</h1>
          <p className="text-sm text-slate-500">{new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}</p>
        </div>
        <div className="flex gap-2">
          <Link to="/Clientes?novo=1"><Button icon={Plus}>Novo cliente</Button></Link>
          <Link to="/Tarefas"><Button variant="secondary" icon={CheckSquare}>Tarefa</Button></Link>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard label="Clientes ativos" value={r.ativos} icon={Users} tone="brand" />
        <StatCard label="Receita do mês" value={formatarMoeda(r.receitaMes)} icon={DollarSign} tone="green" hint={`Lucro ${formatarMoeda(r.lucroMes)}`} />
        <StatCard label="Vencendo (7 dias)" value={r.proximos.length} icon={CalendarClock} tone="amber" hint={`Previsto no mês ${formatarMoeda(r.previsto)}`} />
        <StatCard label="Vencidos" value={r.vencidos.length} icon={AlertTriangle} tone="red" />
      </div>

      {(r.erros.length > 0 || r.vencidos.length > 0) && (
        <Card className="flex flex-col gap-2 border-amber-200 bg-amber-50 p-4 sm:flex-row sm:items-center">
          <Bell className="h-5 w-5 shrink-0 text-amber-600" />
          <div className="flex-1 text-sm text-amber-900">
            {r.vencidos.length > 0 && <p><b>{r.vencidos.length}</b> cliente(s) com mensalidade vencida aguardando renovação.</p>}
            {r.erros.length > 0 && <p><b>{r.erros.length}</b> mensagem(ns) de WhatsApp com erro nos últimos 2 dias. <Link to="/MensagensAutomaticas?aba=historico" className="font-bold underline">Ver histórico</Link></p>}
          </div>
          {r.vencidos.length > 0 && <Link to="/Clientes?filtro=vencidos"><Button size="sm" variant="secondary">Ver vencidos</Button></Link>}
        </Card>
      )}

      <div className="grid gap-5 lg:grid-cols-3">
        <Card className="p-5 lg:col-span-2">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="flex items-center gap-2 font-bold"><CalendarClock className="h-5 w-5 text-brand-600" /> Próximos vencimentos</h2>
            <Link to="/Clientes?filtro=vencendo" className="flex items-center gap-1 text-sm font-semibold text-brand-600">Ver todos <ArrowRight className="h-4 w-4" /></Link>
          </div>
          {r.proximos.length === 0 && r.vencidos.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">Nenhum vencimento nos próximos 7 dias 🎉</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {[...r.vencidos.slice(0, 4), ...r.proximos.slice(0, 8)].map(({ c }) => <LinhaCliente key={c.id} c={c} />)}
            </div>
          )}
        </Card>

        <Card className="p-5">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="flex items-center gap-2 font-bold"><Star className="h-5 w-5 fill-amber-400 text-amber-500" /> Clientes essenciais</h2>
            <Link to="/Clientes?filtro=essenciais" className="text-sm font-semibold text-brand-600">Ver</Link>
          </div>
          {r.essenciais.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">Marque clientes importantes com ★ em Clientes.</p>
          ) : (
            <div className="divide-y divide-slate-100">{r.essenciais.slice(0, 6).map((c) => <LinhaCliente key={c.id} c={c} compacto />)}</div>
          )}
        </Card>

        <Card className="p-5 lg:col-span-2">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="flex items-center gap-2 font-bold"><CheckSquare className="h-5 w-5 text-brand-600" /> Tarefas pendentes</h2>
            <Link to="/Tarefas" className="text-sm font-semibold text-brand-600">Abrir tarefas</Link>
          </div>
          {r.tarefas.length === 0 ? <p className="py-6 text-center text-sm text-slate-400">Nenhuma tarefa pendente.</p> : (
            <ul className="space-y-2">
              {r.tarefas.slice(0, 6).map((t) => {
                const atrasada = t.data_prazo && t.data_prazo < hoje;
                return (
                  <li key={t.id} className="flex items-center gap-3 rounded-xl bg-slate-50 px-3 py-2.5">
                    <input type="checkbox" className="h-4 w-4 accent-brand-600" onChange={async () => { await db.Tarefa.update(t.id, { status: 'concluida' }); toast('Tarefa concluída'); }} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold">{t.titulo}</p>
                      {t.data_prazo && <p className={cn('text-xs', atrasada ? 'font-bold text-red-500' : 'text-slate-400')}>{atrasada ? 'Atrasada · ' : ''}{formatarDataBR(t.data_prazo)}</p>}
                    </div>
                    <Badge className={PRIORIDADE[t.prioridade]}>{t.prioridade === 'media' ? 'média' : t.prioridade}</Badge>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>

        <div className="space-y-5">
          <Card className="p-5">
            <h2 className="mb-3 flex items-center gap-2 font-bold"><TrendingUp className="h-5 w-5 text-brand-600" /> Hoje</h2>
            <div className="grid grid-cols-2 gap-2 text-center">
              <div className="rounded-xl bg-emerald-50 p-3"><p className="text-2xl font-black text-emerald-700">{r.enviadasHoje}</p><p className="text-xs text-emerald-600">msgs enviadas</p></div>
              <div className="rounded-xl bg-orange-50 p-3"><p className="text-2xl font-black text-orange-600">{r.proximos.filter((x) => x.d === 0).length}</p><p className="text-xs text-orange-600">vencem hoje</p></div>
            </div>
          </Card>
          {r.notas.length > 0 && (
            <Card className="p-5">
              <h2 className="mb-3 flex items-center gap-2 font-bold"><StickyNote className="h-5 w-5 text-brand-600" /> Notas fixadas</h2>
              <div className="space-y-2">
                {r.notas.slice(0, 3).map((n) => (
                  <Link to="/NotasAdesivas" key={n.id} className={cn('block rounded-xl p-3', COR_NOTA[n.cor])}>
                    {n.titulo && <p className="text-sm font-bold">{n.titulo}</p>}
                    <p className="line-clamp-2 whitespace-pre-line text-sm text-slate-700">{n.conteudo}</p>
                  </Link>
                ))}
              </div>
            </Card>
          )}
        </div>
      </div>

      <MensagemModal open={!!msg} cliente={msg?.cliente} plano={msg && planoPor.get(msg.cliente.plano_id)} modelo={msg?.modelo} sucesso={msg?.sucesso} tipo={msg?.tipo} onClose={() => setMsg(null)} />
      <RenovarModal open={!!renovar} cliente={renovar} plano={renovar && planoPor.get(renovar.plano_id)} onClose={() => setRenovar(null)}
        onConfirm={async (opts) => {
          const { cliente } = await renovarCliente(renovar, planoPor.get(renovar.plano_id), opts);
          setMsg({ cliente, modelo: 'whatsapp_message', sucesso: 'Renovação confirmada! ✅', tipo: 'renovacao' });
        }} />
    </div>
  );
}
