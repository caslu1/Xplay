import { useMemo, useState } from 'react';
import { CheckSquare, Plus, Pencil, Trash2, Calendar } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { db } from '../lib/db';
import { usePopup } from '../context/PopupContext';
import { cn, formatarDataBR, hojeISO } from '../lib/utils';
import { Button, Card, Badge, Modal, Field, Input, Select, Textarea, PageHeader, Empty, Spinner, Tabs, IconButton } from '../components/ui';

const PRIORIDADES = { alta: { l: 'Alta', c: 'bg-red-100 text-red-600' }, media: { l: 'Média', c: 'bg-amber-100 text-amber-700' }, baixa: { l: 'Baixa', c: 'bg-slate-100 text-slate-500' } };
const CATEGORIAS = { cobranca: 'Cobrança', atendimento: 'Atendimento', administrativo: 'Administrativo', outros: 'Outros' };
const STATUS = { pendente: 'Pendente', em_andamento: 'Em andamento', concluida: 'Concluída' };
const PESO = { alta: 0, media: 1, baixa: 2 };

export default function Tarefas() {
  const { data, loading } = useEntities(['Tarefa']);
  const { confirm, toast } = usePopup();
  const [aba, setAba] = useState('abertas');
  const [cat, setCat] = useState('');
  const [form, setForm] = useState(null);
  const hoje = hojeISO();

  const lista = useMemo(() => data.Tarefa
    .filter((t) => (aba === 'abertas' ? t.status !== 'concluida' : aba === 'todas' ? true : t.status === aba))
    .filter((t) => !cat || t.categoria === cat)
    .sort((a, b) => (a.status === 'concluida') - (b.status === 'concluida') || String(a.data_prazo || '9999').localeCompare(String(b.data_prazo || '9999')) || PESO[a.prioridade] - PESO[b.prioridade]),
  [data.Tarefa, aba, cat]);

  const novo = () => setForm({ titulo: '', descricao: '', data_prazo: hoje, prioridade: 'media', status: 'pendente', categoria: 'outros' });

  async function salvar() {
    const { id, created_date, updated_date, created_by, ...dados } = form;
    if (!dados.data_prazo) dados.data_prazo = null;
    if (id) await db.Tarefa.update(id, dados); else await db.Tarefa.create(dados);
    toast('Tarefa salva');
    setForm(null);
  }

  if (loading) return <Spinner />;
  const conta = (f) => data.Tarefa.filter(f).length;
  return (
    <div>
      <PageHeader icon={CheckSquare} title="Tarefas" subtitle="Organize cobranças, atendimentos e o administrativo" actions={<Button icon={Plus} onClick={novo}>Nova tarefa</Button>} />
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <Tabs value={aba} onChange={setAba} tabs={[
          { value: 'abertas', label: 'Abertas', count: conta((t) => t.status !== 'concluida') },
          { value: 'pendente', label: 'Pendentes', count: conta((t) => t.status === 'pendente') },
          { value: 'em_andamento', label: 'Em andamento', count: conta((t) => t.status === 'em_andamento') },
          { value: 'concluida', label: 'Concluídas', count: conta((t) => t.status === 'concluida') },
          { value: 'todas', label: 'Todas' },
        ]} />
        <Select className="sm:w-48" value={cat} onChange={(e) => setCat(e.target.value)}>
          <option value="">Todas as categorias</option>
          {Object.entries(CATEGORIAS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </Select>
      </div>
      {lista.length === 0 ? (
        <Card><Empty icon={CheckSquare} title="Nenhuma tarefa aqui" action={<Button icon={Plus} onClick={novo}>Criar tarefa</Button>} /></Card>
      ) : (
        <Card className="divide-y divide-slate-100">
          {lista.map((t) => {
            const feita = t.status === 'concluida';
            const atrasada = !feita && t.data_prazo && t.data_prazo < hoje;
            return (
              <div key={t.id} className="flex items-start gap-3 px-4 py-3">
                <input type="checkbox" className="mt-1 h-5 w-5 accent-brand-600" checked={feita}
                  onChange={() => db.Tarefa.update(t.id, { status: feita ? 'pendente' : 'concluida' })} />
                <div className="min-w-0 flex-1">
                  <p className={cn('font-semibold', feita && 'text-slate-400 line-through')}>{t.titulo}</p>
                  {t.descricao && <p className="text-sm text-slate-500">{t.descricao}</p>}
                  <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
                    <Badge className={PRIORIDADES[t.prioridade].c}>{PRIORIDADES[t.prioridade].l}</Badge>
                    <Badge className="bg-brand-50 text-brand-700">{CATEGORIAS[t.categoria]}</Badge>
                    {t.status === 'em_andamento' && <Badge className="bg-sky-100 text-sky-700">Em andamento</Badge>}
                    {t.data_prazo && <span className={cn('inline-flex items-center gap-1 text-xs', atrasada ? 'font-bold text-red-500' : 'text-slate-400')}><Calendar className="h-3.5 w-3.5" />{atrasada && 'Atrasada · '}{formatarDataBR(t.data_prazo)}</span>}
                  </div>
                </div>
                <IconButton icon={Pencil} title="Editar" onClick={() => setForm(t)} />
                <IconButton icon={Trash2} title="Excluir" className="hover:!text-red-500" onClick={async () => {
                  if (await confirm({ title: 'Excluir tarefa?', danger: true })) { await db.Tarefa.delete(t.id); toast('Tarefa excluída'); }
                }} />
              </div>
            );
          })}
        </Card>
      )}
      <Modal open={!!form} onClose={() => setForm(null)} title={form?.id ? 'Editar tarefa' : 'Nova tarefa'}
        footer={<><Button variant="secondary" onClick={() => setForm(null)}>Cancelar</Button><Button onClick={salvar} disabled={!form?.titulo?.trim()}>Salvar</Button></>}>
        {form && (
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Título *" className="sm:col-span-2"><Input autoFocus value={form.titulo} onChange={(e) => setForm({ ...form, titulo: e.target.value })} /></Field>
            <Field label="Descrição" className="sm:col-span-2"><Textarea value={form.descricao || ''} onChange={(e) => setForm({ ...form, descricao: e.target.value })} /></Field>
            <Field label="Prazo"><Input type="date" value={form.data_prazo || ''} onChange={(e) => setForm({ ...form, data_prazo: e.target.value })} /></Field>
            <Field label="Prioridade"><Select value={form.prioridade} onChange={(e) => setForm({ ...form, prioridade: e.target.value })}>{Object.entries(PRIORIDADES).map(([k, v]) => <option key={k} value={k}>{v.l}</option>)}</Select></Field>
            <Field label="Categoria"><Select value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}>{Object.entries(CATEGORIAS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</Select></Field>
            <Field label="Status"><Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>{Object.entries(STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</Select></Field>
          </div>
        )}
      </Modal>
    </div>
  );
}
