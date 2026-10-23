import { useMemo, useState } from 'react';
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, PieChart, Pie, Cell, ReferenceLine,
} from 'recharts';
import { BarChart3, DollarSign, TrendingUp, Wallet, Receipt, FileDown, ArrowUpRight, ArrowDownRight, Minus, Users, RefreshCw } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { useAuth } from '../context/AuthContext';
import {
  cn, formatarMoeda, hojeISO, mesesEntre, addMeses, nomeMes, nomeMesLongo, estadoPorWhatsApp, METODOS,
} from '../lib/utils';
import { Card, StatCard, Spinner, Select, Button, Field, Input, PageHeader } from '../components/ui';

// Paleta categórica validada (ordem fixa) e cor única da marca para séries simples.
const CATEGORICAS = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#6250d6', '#e34948'];
const MARCA = '#7c3aed';
const GRID = '#e2e8f0';
const EIXO = { fontSize: 11, fill: '#64748b' };

const PERIODOS = [
  { v: 'mes', l: 'Este mês' }, { v: '3', l: 'Últimos 3 meses' }, { v: '6', l: 'Últimos 6 meses' },
  { v: '12', l: 'Últimos 12 meses' }, { v: 'ano', l: 'Este ano' }, { v: 'custom', l: 'Personalizado' },
];

const moedaCurta = (v) => (Math.abs(v) >= 1000 ? `R$${(v / 1000).toFixed(1).replace('.', ',')}k` : `R$${Math.round(v)}`);

function Dica({ active, payload, label, formato = formatarMoeda }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs shadow-lg">
      <p className="mb-1 font-bold text-slate-700">{label}</p>
      {payload.map((p) => (
        <p key={p.dataKey} className="flex items-center gap-2 text-slate-600">
          <span className="h-2 w-2 rounded-full" style={{ background: p.color || p.payload?.fill }} />
          {p.name}: <b className="text-slate-800">{formato(p.value)}</b>
        </p>
      ))}
    </div>
  );
}

function ChartCard({ title, subtitle, children, className }) {
  return (
    <Card className={cn('p-5', className)}>
      <h3 className="font-bold text-slate-800">{title}</h3>
      {subtitle && <p className="mb-3 text-xs text-slate-400">{subtitle}</p>}
      {children}
    </Card>
  );
}

export default function Faturamento() {
  const { profile } = useAuth();
  const { data, loading } = useEntities(['Mensalidade', 'Cliente', 'Plano']);
  const hoje = hojeISO();
  const mesAtual = hoje.slice(0, 7);
  const [periodo, setPeriodo] = useState('6');
  const [de, setDe] = useState(addMeses(mesAtual, -5));
  const [ate, setAte] = useState(mesAtual);
  const [mesA, setMesA] = useState(addMeses(mesAtual, -1));
  const [mesB, setMesB] = useState(mesAtual);

  const [ini, fim] = useMemo(() => {
    if (periodo === 'mes') return [mesAtual, mesAtual];
    if (periodo === 'ano') return [`${mesAtual.slice(0, 4)}-01`, mesAtual];
    if (periodo === 'custom') return de <= ate ? [de, ate] : [ate, de];
    return [addMeses(mesAtual, -(Number(periodo) - 1)), mesAtual];
  }, [periodo, de, ate, mesAtual]);

  const planoPor = useMemo(() => new Map(data.Plano.map((p) => [p.id, p])), [data.Plano]);
  const corPlano = useMemo(() => {
    const ordem = [...data.Plano].sort((a, b) => String(a.codigo || a.nome).localeCompare(String(b.codigo || b.nome)));
    return new Map(ordem.map((p, i) => [p.id, i < CATEGORICAS.length ? CATEGORICAS[i] : '#94a3b8']));
  }, [data.Plano]);

  const r = useMemo(() => {
    const meses = mesesEntre(ini, fim);
    const pagos = data.Mensalidade.filter((m) => m.status === 'pago' && m.data_pagamento);
    const noPeriodo = pagos.filter((m) => { const k = String(m.data_pagamento).slice(0, 7); return k >= ini && k <= fim; });
    const custo = (m) => Number(m.custo ?? 0) || 0;

    const porMes = meses.map((k) => {
      const ms = noPeriodo.filter((m) => String(m.data_pagamento).slice(0, 7) === k);
      const receita = ms.reduce((s, m) => s + Number(m.valor), 0);
      const c = ms.reduce((s, m) => s + custo(m), 0);
      // Novos: pagamentos de cadastro do mês (ou clientes criados no mês, se não registrou pagamento).
      const novos = Math.max(ms.filter((m) => m.metodo_pagamento === 'cadastro').length,
        data.Cliente.filter((x) => String(x.created_date).slice(0, 7) === k).length);
      const renov = ms.filter((m) => m.metodo_pagamento !== 'cadastro').length;
      // vencimentos do mês que ficaram sem renovar (cliente ainda com vencimento nesse mês, já passado)
      const naoRenov = data.Cliente.filter((x) => x.status === 'ativo' && String(x.data_vencimento).slice(0, 7) === k && x.data_vencimento < hoje).length;
      return {
        mes: nomeMes(k), chave: k, receita, custo: c, lucro: receita - c, novos, renovacoes: renov,
        taxa: renov + naoRenov ? Math.round((renov / (renov + naoRenov)) * 100) : null,
      };
    });

    const receita = noPeriodo.reduce((s, m) => s + Number(m.valor), 0);
    const custoTotal = noPeriodo.reduce((s, m) => s + custo(m), 0);

    const porPlanoMap = new Map();
    for (const m of noPeriodo) {
      const id = m.plano_id || data.Cliente.find((c) => c.id === m.cliente_id)?.plano_id || 'sem';
      const nome = planoPor.get(id)?.nome || m.plano_nome || 'Sem plano';
      const atual = porPlanoMap.get(id) || { id, nome, valor: 0, qtd: 0 };
      atual.valor += Number(m.valor);
      atual.qtd++;
      porPlanoMap.set(id, atual);
    }
    const porPlano = [...porPlanoMap.values()].sort((a, b) => b.valor - a.valor);

    const estados = new Map();
    for (const c of data.Cliente) {
      if (c.status !== 'ativo') continue;
      const uf = estadoPorWhatsApp(c.whatsapp);
      estados.set(uf, (estados.get(uf) || 0) + 1);
    }
    const porEstado = [...estados.entries()].map(([uf, n]) => ({ uf, n })).sort((a, b) => b.n - a.n).slice(0, 10);

    const metodos = new Map();
    for (const m of noPeriodo) metodos.set(m.metodo_pagamento, (metodos.get(m.metodo_pagamento) || 0) + Number(m.valor));

    const taxas = porMes.filter((m) => m.taxa != null);
    return {
      porMes, porPlano, porEstado, receita, custo: custoTotal, lucro: receita - custoTotal, qtd: noPeriodo.length,
      ticket: noPeriodo.length ? receita / noPeriodo.length : 0,
      mediaNovos: porMes.length ? porMes.reduce((s, m) => s + m.novos, 0) / porMes.length : 0,
      novos: porMes.reduce((s, m) => s + m.novos, 0),
      taxaMedia: taxas.length ? Math.round(taxas.reduce((s, m) => s + m.taxa, 0) / taxas.length) : null,
      metodos: [...metodos.entries()].sort((a, b) => b[1] - a[1]),
      pagos,
    };
  }, [data, ini, fim, planoPor, hoje]);

  const comparador = useMemo(() => {
    const doMes = (k) => r.pagos.filter((m) => String(m.data_pagamento).slice(0, 7) === k);
    const a = doMes(mesA), b = doMes(mesB);
    const ids = new Set([...a, ...b].map((m) => m.plano_id || 'sem'));
    const linhas = [...ids].map((id) => {
      const fa = a.filter((m) => (m.plano_id || 'sem') === id), fb = b.filter((m) => (m.plano_id || 'sem') === id);
      const nome = planoPor.get(id)?.nome || fa[0]?.plano_nome || fb[0]?.plano_nome || 'Sem plano';
      return {
        id, nome, vendasA: fa.length, vendasB: fb.length,
        receitaA: fa.reduce((s, m) => s + Number(m.valor), 0), receitaB: fb.reduce((s, m) => s + Number(m.valor), 0),
      };
    }).sort((x, y) => y.vendasB + y.vendasA - (x.vendasB + x.vendasA));
    const tot = linhas.reduce((t, l) => ({ vendasA: t.vendasA + l.vendasA, vendasB: t.vendasB + l.vendasB, receitaA: t.receitaA + l.receitaA, receitaB: t.receitaB + l.receitaB }), { vendasA: 0, vendasB: 0, receitaA: 0, receitaB: 0 });
    return { linhas, tot };
  }, [r.pagos, mesA, mesB, planoPor]);

  const opcoesMes = useMemo(() => {
    const primeiro = r.pagos.reduce((min, m) => (String(m.data_pagamento).slice(0, 7) < min ? String(m.data_pagamento).slice(0, 7) : min), addMeses(mesAtual, -11));
    return mesesEntre(primeiro, mesAtual).reverse();
  }, [r.pagos, mesAtual]);

  async function exportarPDF() {
    const { jsPDF } = await import('jspdf');
    const { default: autoTable } = await import('jspdf-autotable');
    const doc = new jsPDF();
    const roxo = [124, 58, 237];
    doc.setFillColor(...roxo);
    doc.rect(0, 0, 210, 28, 'F');
    doc.setTextColor(255);
    doc.setFontSize(16);
    doc.text('Relatório financeiro — Gestor Simples', 14, 13);
    doc.setFontSize(10);
    doc.text(`${profile?.empresa || ''}  ·  ${nomeMesLongo(ini)} a ${nomeMesLongo(fim)}  ·  gerado em ${new Date().toLocaleDateString('pt-BR')}`, 14, 21);
    doc.setTextColor(30);
    autoTable(doc, {
      startY: 36, theme: 'grid', headStyles: { fillColor: roxo },
      head: [['Receita', 'Custo', 'Lucro', 'Ticket médio', 'Pagamentos', 'Clientes novos']],
      body: [[formatarMoeda(r.receita), formatarMoeda(r.custo), formatarMoeda(r.lucro), formatarMoeda(r.ticket), r.qtd, r.novos]],
    });
    autoTable(doc, {
      startY: doc.lastAutoTable.finalY + 8, headStyles: { fillColor: roxo },
      head: [['Mês', 'Receita', 'Custo', 'Lucro', 'Novos', 'Renovações', 'Taxa renov.']],
      body: r.porMes.map((m) => [nomeMesLongo(m.chave), formatarMoeda(m.receita), formatarMoeda(m.custo), formatarMoeda(m.lucro), m.novos, m.renovacoes, m.taxa == null ? '—' : `${m.taxa}%`]),
    });
    autoTable(doc, {
      startY: doc.lastAutoTable.finalY + 8, headStyles: { fillColor: roxo },
      head: [['Plano', 'Pagamentos', 'Receita', '% da receita']],
      body: r.porPlano.map((p) => [p.nome, p.qtd, formatarMoeda(p.valor), r.receita ? `${Math.round((p.valor / r.receita) * 100)}%` : '0%']),
    });
    autoTable(doc, {
      startY: doc.lastAutoTable.finalY + 8, headStyles: { fillColor: roxo },
      head: [['Plano', `Vendas ${nomeMes(mesA)}`, `Vendas ${nomeMes(mesB)}`, 'Variação', `Receita ${nomeMes(mesA)}`, `Receita ${nomeMes(mesB)}`]],
      body: comparador.linhas.map((l) => [l.nome, l.vendasA, l.vendasB, `${l.vendasB - l.vendasA >= 0 ? '+' : ''}${l.vendasB - l.vendasA}`, formatarMoeda(l.receitaA), formatarMoeda(l.receitaB)]),
    });
    doc.save(`financeiro-${ini}_${fim}.pdf`);
  }

  if (loading) return <Spinner />;
  const temDados = r.qtd > 0;

  return (
    <div className="space-y-5">
      <PageHeader icon={BarChart3} title="Financeiro" subtitle={`${nomeMesLongo(ini)}${ini !== fim ? ` a ${nomeMesLongo(fim)}` : ''}`}
        actions={<Button variant="secondary" icon={FileDown} onClick={exportarPDF}>Exportar PDF</Button>} />

      <Card className="flex flex-wrap items-end gap-3 p-3">
        <Field label="Período" className="w-full sm:w-52">
          <Select value={periodo} onChange={(e) => setPeriodo(e.target.value)}>{PERIODOS.map((p) => <option key={p.v} value={p.v}>{p.l}</option>)}</Select>
        </Field>
        {periodo === 'custom' && (
          <>
            <Field label="De"><Input type="month" value={de} onChange={(e) => setDe(e.target.value)} /></Field>
            <Field label="Até"><Input type="month" value={ate} onChange={(e) => setAte(e.target.value)} /></Field>
          </>
        )}
      </Card>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard label="Receita" value={formatarMoeda(r.receita)} icon={DollarSign} tone="brand" hint={`${r.qtd} pagamentos`} />
        <StatCard label="Custo" value={formatarMoeda(r.custo)} icon={Wallet} tone="slate" />
        <StatCard label="Lucro" value={formatarMoeda(r.lucro)} icon={TrendingUp} tone="green" hint={r.receita ? `Margem ${Math.round((r.lucro / r.receita) * 100)}%` : undefined} />
        <StatCard label="Ticket médio" value={formatarMoeda(r.ticket)} icon={Receipt} tone="blue" />
      </div>

      {!temDados && <Card className="p-6 text-center text-sm text-slate-500">Nenhum pagamento no período. Cadastre ou renove clientes para ver os gráficos.</Card>}

      <div className="grid gap-5 lg:grid-cols-3">
        <ChartCard title="Receita por mês" subtitle="Soma das mensalidades pagas" className="lg:col-span-2">
          <div className="h-64">
            <ResponsiveContainer>
              <LineChart data={r.porMes} margin={{ top: 10, right: 12, left: 0, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="mes" tick={EIXO} axisLine={false} tickLine={false} />
                <YAxis tick={EIXO} axisLine={false} tickLine={false} tickFormatter={moedaCurta} width={56} />
                <Tooltip content={<Dica />} cursor={{ stroke: '#cbd5e1' }} />
                <Line type="monotone" dataKey="receita" name="Receita" stroke={MARCA} strokeWidth={2} dot={{ r: 4, fill: MARCA, stroke: '#fff', strokeWidth: 2 }} activeDot={{ r: 6 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard title="Receita por plano" subtitle="Participação no período">
          <div className="relative h-44">
            <ResponsiveContainer>
              <PieChart>
                <Pie data={r.porPlano} dataKey="valor" nameKey="nome" innerRadius="62%" outerRadius="95%" paddingAngle={2} stroke="#fff" strokeWidth={2}>
                  {r.porPlano.map((p) => <Cell key={p.id} fill={corPlano.get(p.id) || '#94a3b8'} />)}
                </Pie>
                <Tooltip formatter={(v) => formatarMoeda(v)} />
              </PieChart>
            </ResponsiveContainer>
            <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
              <p className="text-[11px] text-slate-400">Total</p>
              <p className="text-sm font-extrabold text-slate-800">{moedaCurta(r.receita)}</p>
            </div>
          </div>
          <ul className="mt-3 space-y-1.5">
            {r.porPlano.map((p) => (
              <li key={p.id} className="flex items-center gap-2 text-sm">
                <span className="h-2.5 w-2.5 rounded-full" style={{ background: corPlano.get(p.id) || '#94a3b8' }} />
                <span className="flex-1 truncate text-slate-600">{p.nome}</span>
                <span className="font-semibold text-slate-800">{formatarMoeda(p.valor)}</span>
                <span className="w-10 text-right text-xs text-slate-400">{r.receita ? Math.round((p.valor / r.receita) * 100) : 0}%</span>
              </li>
            ))}
          </ul>
        </ChartCard>

        <ChartCard title="Lucro por mês" subtitle="Receita − custo dos planos">
          <div className="h-56">
            <ResponsiveContainer>
              <BarChart data={r.porMes} margin={{ top: 10, right: 8, left: 0, bottom: 0 }} barCategoryGap="28%">
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="mes" tick={EIXO} axisLine={false} tickLine={false} />
                <YAxis tick={EIXO} axisLine={false} tickLine={false} tickFormatter={moedaCurta} width={56} />
                <Tooltip content={<Dica />} cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="lucro" name="Lucro" fill="#1baf7a" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard title="Clientes novos" subtitle={`Média de ${r.mediaNovos.toFixed(1).replace('.', ',')} por mês (linha tracejada)`}>
          <div className="h-56">
            <ResponsiveContainer>
              <BarChart data={r.porMes} margin={{ top: 10, right: 8, left: -16, bottom: 0 }} barCategoryGap="28%">
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="mes" tick={EIXO} axisLine={false} tickLine={false} />
                <YAxis tick={EIXO} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip content={<Dica formato={(v) => v} />} cursor={{ fill: '#f1f5f9' }} />
                <ReferenceLine y={r.mediaNovos} stroke="#64748b" strokeDasharray="4 4" />
                <Bar dataKey="novos" name="Novos" fill={MARCA} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard title="Taxa de renovação" subtitle={r.taxaMedia == null ? 'Sem vencimentos no período' : `Média de ${r.taxaMedia}% no período`}>
          <div className="h-56">
            <ResponsiveContainer>
              <LineChart data={r.porMes} margin={{ top: 10, right: 12, left: -16, bottom: 0 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="mes" tick={EIXO} axisLine={false} tickLine={false} />
                <YAxis tick={EIXO} axisLine={false} tickLine={false} domain={[0, 100]} tickFormatter={(v) => `${v}%`} />
                <Tooltip content={<Dica formato={(v) => `${v}%`} />} cursor={{ stroke: '#cbd5e1' }} />
                <Line type="monotone" dataKey="taxa" name="Renovação" stroke="#2a78d6" strokeWidth={2} connectNulls dot={{ r: 4, fill: '#2a78d6', stroke: '#fff', strokeWidth: 2 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard title="Clientes por estado" subtitle="Pelo DDD do WhatsApp (clientes ativos)" className="lg:col-span-2">
          <div className="h-64">
            <ResponsiveContainer>
              <BarChart data={r.porEstado} layout="vertical" margin={{ top: 0, right: 24, left: 0, bottom: 0 }} barCategoryGap="22%">
                <CartesianGrid stroke={GRID} horizontal={false} />
                <XAxis type="number" tick={EIXO} axisLine={false} tickLine={false} allowDecimals={false} />
                <YAxis type="category" dataKey="uf" tick={{ ...EIXO, fontWeight: 600 }} axisLine={false} tickLine={false} width={48} />
                <Tooltip content={<Dica formato={(v) => `${v} cliente${v === 1 ? '' : 's'}`} />} cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="n" name="Clientes" fill={MARCA} radius={[0, 4, 4, 0]} label={{ position: 'right', fontSize: 11, fill: '#475569' }} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        <ChartCard title="Formas de pagamento" subtitle="Receita por método">
          <ul className="space-y-3">
            {r.metodos.length === 0 && <li className="text-sm text-slate-400">Sem pagamentos.</li>}
            {r.metodos.map(([m, v]) => (
              <li key={m}>
                <div className="mb-1 flex justify-between text-sm"><span className="text-slate-600">{METODOS[m] || m || '—'}</span><b>{formatarMoeda(v)}</b></div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-brand-500" style={{ width: `${r.receita ? (v / r.receita) * 100 : 0}%` }} /></div>
              </li>
            ))}
          </ul>
        </ChartCard>
      </div>

      {/* Comparador de planos vendidos */}
      <Card className="p-5">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h3 className="flex items-center gap-2 font-bold text-slate-800"><RefreshCw className="h-5 w-5 text-brand-600" /> Planos vendidos — Comparador</h3>
            <p className="text-xs text-slate-400">Compare as vendas de cada plano entre dois meses</p>
          </div>
          <div className="flex items-end gap-2">
            <Field label="Mês A"><Select value={mesA} onChange={(e) => setMesA(e.target.value)}>{opcoesMes.map((m) => <option key={m} value={m}>{nomeMesLongo(m)}</option>)}</Select></Field>
            <span className="pb-3 text-sm font-bold text-slate-400">vs</span>
            <Field label="Mês B"><Select value={mesB} onChange={(e) => setMesB(e.target.value)}>{opcoesMes.map((m) => <option key={m} value={m}>{nomeMesLongo(m)}</option>)}</Select></Field>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs uppercase tracking-wide text-slate-400">
                <th className="py-2 pr-2">Plano</th>
                <th className="px-2 text-right">Vendas {nomeMes(mesA)}</th>
                <th className="px-2 text-right">Vendas {nomeMes(mesB)}</th>
                <th className="px-2 text-right">Variação</th>
                <th className="px-2 text-right">Receita {nomeMes(mesA)}</th>
                <th className="pl-2 text-right">Receita {nomeMes(mesB)}</th>
              </tr>
            </thead>
            <tbody>
              {comparador.linhas.length === 0 && <tr><td colSpan={6} className="py-6 text-center text-slate-400">Nenhuma venda nesses meses.</td></tr>}
              {[...comparador.linhas, { id: 'total', nome: 'Total', ...comparador.tot }].map((l) => {
                const dif = l.vendasB - l.vendasA;
                const pct = l.vendasA ? Math.round((dif / l.vendasA) * 100) : null;
                const total = l.id === 'total';
                if (total && comparador.linhas.length === 0) return null;
                return (
                  <tr key={l.id} className={cn('border-b border-slate-50', total && 'bg-slate-50 font-bold')}>
                    <td className="py-2.5 pr-2">
                      <span className="flex items-center gap-2">{!total && <span className="h-2.5 w-2.5 rounded-full" style={{ background: corPlano.get(l.id) || '#94a3b8' }} />}{l.nome}</span>
                    </td>
                    <td className="px-2 text-right">{l.vendasA}</td>
                    <td className="px-2 text-right">{l.vendasB}</td>
                    <td className="px-2 text-right">
                      <span className={cn('inline-flex items-center gap-0.5 rounded-lg px-2 py-0.5 text-xs font-bold',
                        dif > 0 ? 'bg-emerald-100 text-emerald-700' : dif < 0 ? 'bg-red-100 text-red-600' : 'bg-slate-100 text-slate-500')}>
                        {dif > 0 ? <ArrowUpRight className="h-3.5 w-3.5" /> : dif < 0 ? <ArrowDownRight className="h-3.5 w-3.5" /> : <Minus className="h-3.5 w-3.5" />}
                        {dif > 0 ? '+' : ''}{dif}{pct != null && ` (${pct > 0 ? '+' : ''}${pct}%)`}
                      </span>
                    </td>
                    <td className="px-2 text-right">{formatarMoeda(l.receitaA)}</td>
                    <td className="pl-2 text-right">{formatarMoeda(l.receitaB)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      <Card className="p-5">
        <h3 className="mb-3 flex items-center gap-2 font-bold text-slate-800"><Users className="h-5 w-5 text-brand-600" /> Resumo mensal</h3>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-sm">
            <thead><tr className="border-b border-slate-100 text-left text-xs uppercase tracking-wide text-slate-400"><th className="py-2">Mês</th><th className="text-right">Receita</th><th className="text-right">Custo</th><th className="text-right">Lucro</th><th className="text-right">Novos</th><th className="text-right">Renovações</th><th className="text-right">Taxa</th></tr></thead>
            <tbody>
              {r.porMes.map((m) => (
                <tr key={m.chave} className="border-b border-slate-50">
                  <td className="py-2">{nomeMesLongo(m.chave)}</td>
                  <td className="text-right">{formatarMoeda(m.receita)}</td>
                  <td className="text-right text-slate-500">{formatarMoeda(m.custo)}</td>
                  <td className="text-right font-semibold">{formatarMoeda(m.lucro)}</td>
                  <td className="text-right">{m.novos}</td>
                  <td className="text-right">{m.renovacoes}</td>
                  <td className="text-right">{m.taxa == null ? '—' : `${m.taxa}%`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
