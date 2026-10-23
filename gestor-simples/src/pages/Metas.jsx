import { useMemo, useState } from 'react';
import { Target, Plus, Pencil, Trash2, Trophy, ChevronLeft, ChevronRight } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { db } from '../lib/db';
import { usePopup } from '../context/PopupContext';
import { cn, formatarMoeda, hojeISO, addMeses, nomeMesLongo } from '../lib/utils';
import { Button, Card, Modal, Field, Input, Select, PageHeader, Empty, Spinner, IconButton } from '../components/ui';

const TIPOS = {
  receita: { label: 'Receita', moeda: true },
  lucro: { label: 'Lucro', moeda: true },
  clientes_novos: { label: 'Clientes novos', moeda: false },
  renovacoes: { label: 'Renovações', moeda: false },
};

export default function Metas() {
  const { data, loading } = useEntities(['Meta', 'Mensalidade']);
  const { confirm, toast } = usePopup();
  const [mes, setMes] = useState(hojeISO().slice(0, 7));
  const [form, setForm] = useState(null);

  const realizado = useMemo(() => {
    const ms = data.Mensalidade.filter((m) => m.status === 'pago' && String(m.data_pagamento).slice(0, 7) === mes);
    const receita = ms.reduce((s, m) => s + Number(m.valor), 0);
    return {
      receita,
      lucro: receita - ms.reduce((s, m) => s + Number(m.custo || 0), 0),
      clientes_novos: ms.filter((m) => m.metodo_pagamento === 'cadastro').length,
      renovacoes: ms.filter((m) => m.metodo_pagamento !== 'cadastro').length,
    };
  }, [data.Mensalidade, mes]);

  const metas = data.Meta.filter((m) => m.mes === mes);
  const batidas = metas.filter((m) => realizado[m.tipo] >= Number(m.valor_alvo)).length;

  async function salvar() {
    const { id, created_date, updated_date, created_by, ...dados } = form;
    const payload = { ...dados, mes, valor_alvo: Number(form.valor_alvo) || 0 };
    if (id) await db.Meta.update(id, payload); else await db.Meta.create(payload);
    toast('Meta salva');
    setForm(null);
  }

  if (loading) return <Spinner />;
  return (
    <div>
      <PageHeader icon={Target} title="Metas" subtitle="Acompanhe seus objetivos do mês"
        actions={<Button icon={Plus} onClick={() => setForm({ tipo: 'receita', valor_alvo: '', descricao: '' })}>Nova meta</Button>} />
      <Card className="mb-5 flex items-center justify-between p-3">
        <IconButton icon={ChevronLeft} title="Mês anterior" onClick={() => setMes(addMeses(mes, -1))} />
        <div className="text-center">
          <p className="font-bold">{nomeMesLongo(mes)}</p>
          {metas.length > 0 && <p className="text-xs text-slate-500">{batidas} de {metas.length} metas batidas</p>}
        </div>
        <IconButton icon={ChevronRight} title="Próximo mês" onClick={() => setMes(addMeses(mes, 1))} />
      </Card>
      {metas.length === 0 ? (
        <Card><Empty icon={Target} title="Nenhuma meta para este mês" action={<Button icon={Plus} onClick={() => setForm({ tipo: 'receita', valor_alvo: '', descricao: '' })}>Criar meta</Button>}>Defina quanto quer faturar, lucrar, vender ou renovar.</Empty></Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {metas.map((m) => {
            const t = TIPOS[m.tipo];
            const atual = realizado[m.tipo];
            const pct = Number(m.valor_alvo) ? Math.min(100, Math.round((atual / Number(m.valor_alvo)) * 100)) : 0;
            const ok = atual >= Number(m.valor_alvo);
            const fmt = (v) => (t.moeda ? formatarMoeda(v) : v);
            return (
              <Card key={m.id} className={cn('p-5', ok && 'border-emerald-200 bg-emerald-50/40')}>
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-xs font-bold uppercase tracking-wide text-brand-600">{t.label}</p>
                    <p className="mt-0.5 font-bold text-slate-800">{m.descricao || `Meta de ${t.label.toLowerCase()}`}</p>
                  </div>
                  <div className="flex">
                    <IconButton icon={Pencil} title="Editar" onClick={() => setForm(m)} />
                    <IconButton icon={Trash2} title="Excluir" className="hover:!text-red-500" onClick={async () => {
                      if (await confirm({ title: 'Excluir meta?', danger: true })) { await db.Meta.delete(m.id); toast('Meta excluída'); }
                    }} />
                  </div>
                </div>
                <div className="mt-4 flex items-end justify-between">
                  <p className="text-2xl font-black text-slate-900">{fmt(atual)}</p>
                  <p className="text-sm text-slate-500">de {fmt(m.valor_alvo)}</p>
                </div>
                <div className="mt-2 h-3 overflow-hidden rounded-full bg-slate-100">
                  <div className={cn('h-full rounded-full transition-all', ok ? 'bg-emerald-500' : 'bg-gradient-to-r from-brand-500 to-fuchsia-500')} style={{ width: `${pct}%` }} />
                </div>
                <p className={cn('mt-2 flex items-center gap-1 text-sm font-semibold', ok ? 'text-emerald-600' : 'text-slate-500')}>
                  {ok ? <><Trophy className="h-4 w-4" /> Meta batida!</> : `${pct}% · faltam ${fmt(Math.max(0, Number(m.valor_alvo) - atual))}`}
                </p>
              </Card>
            );
          })}
        </div>
      )}
      <Modal open={!!form} onClose={() => setForm(null)} title={form?.id ? 'Editar meta' : `Nova meta — ${nomeMesLongo(mes)}`} size="sm"
        footer={<><Button variant="secondary" onClick={() => setForm(null)}>Cancelar</Button><Button onClick={salvar} disabled={!form?.valor_alvo}>Salvar</Button></>}>
        {form && (
          <div className="space-y-4">
            <Field label="Tipo"><Select value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>{Object.entries(TIPOS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}</Select></Field>
            <Field label={TIPOS[form.tipo].moeda ? 'Valor alvo (R$)' : 'Quantidade alvo'}><Input type="number" min="0" step="0.01" value={form.valor_alvo} onChange={(e) => setForm({ ...form, valor_alvo: e.target.value })} /></Field>
            <Field label="Descrição (opcional)"><Input value={form.descricao || ''} onChange={(e) => setForm({ ...form, descricao: e.target.value })} /></Field>
          </div>
        )}
      </Modal>
    </div>
  );
}
