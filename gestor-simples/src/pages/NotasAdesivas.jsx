import { useState } from 'react';
import { StickyNote, Plus, Pin, PinOff, Trash2, Pencil } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { db } from '../lib/db';
import { usePopup } from '../context/PopupContext';
import { cn } from '../lib/utils';
import { Button, Modal, Field, Input, Textarea, PageHeader, Empty, Spinner, Card, Toggle } from '../components/ui';

export const CORES_NOTA = {
  amarelo: 'bg-amber-100 border-amber-200', verde: 'bg-emerald-100 border-emerald-200', azul: 'bg-sky-100 border-sky-200',
  rosa: 'bg-pink-100 border-pink-200', roxo: 'bg-violet-100 border-violet-200', laranja: 'bg-orange-100 border-orange-200',
};

export default function NotasAdesivas() {
  const { data, loading } = useEntities(['NotaAdesiva']);
  const { confirm, toast } = usePopup();
  const [form, setForm] = useState(null);

  const notas = [...data.NotaAdesiva].sort((a, b) => (a.fixada === b.fixada ? String(b.updated_date).localeCompare(String(a.updated_date)) : a.fixada ? -1 : 1));

  async function salvar() {
    const { id, created_date, updated_date, created_by, ...dados } = form;
    if (id) await db.NotaAdesiva.update(id, dados); else await db.NotaAdesiva.create(dados);
    setForm(null);
  }

  if (loading) return <Spinner />;
  return (
    <div>
      <PageHeader icon={StickyNote} title="Notas adesivas" subtitle="Lembretes rápidos — fixe os mais importantes no topo"
        actions={<Button icon={Plus} onClick={() => setForm({ titulo: '', conteudo: '', cor: 'amarelo', fixada: false })}>Nova nota</Button>} />
      {notas.length === 0 ? (
        <Card><Empty icon={StickyNote} title="Nenhuma nota" action={<Button icon={Plus} onClick={() => setForm({ titulo: '', conteudo: '', cor: 'amarelo', fixada: false })}>Criar nota</Button>}>Anote chaves PIX, recados e ideias.</Empty></Card>
      ) : (
        <div className="columns-1 gap-4 sm:columns-2 lg:columns-3 [&>*]:mb-4">
          {notas.map((n, i) => (
            <div key={n.id} className={cn('group relative break-inside-avoid rounded-2xl border p-4 shadow-sm transition hover:shadow-md', CORES_NOTA[n.cor], i % 2 ? 'rotate-[0.4deg]' : '-rotate-[0.4deg]')}>
              {n.fixada && <Pin className="absolute -top-2 right-4 h-5 w-5 fill-red-400 text-red-500" />}
              {n.titulo && <p className="mb-1 pr-6 font-bold text-slate-800">{n.titulo}</p>}
              <p className="whitespace-pre-line text-sm text-slate-700">{n.conteudo}</p>
              <div className="mt-3 flex justify-end gap-1 opacity-70 transition group-hover:opacity-100">
                <button className="rounded-lg p-1.5 hover:bg-white/60" title={n.fixada ? 'Desafixar' : 'Fixar no topo'} onClick={() => db.NotaAdesiva.update(n.id, { fixada: !n.fixada })}>
                  {n.fixada ? <PinOff className="h-4 w-4" /> : <Pin className="h-4 w-4" />}
                </button>
                <button className="rounded-lg p-1.5 hover:bg-white/60" title="Editar" onClick={() => setForm(n)}><Pencil className="h-4 w-4" /></button>
                <button className="rounded-lg p-1.5 hover:bg-white/60 hover:text-red-600" title="Excluir" onClick={async () => {
                  if (await confirm({ title: 'Excluir nota?', danger: true })) { await db.NotaAdesiva.delete(n.id); toast('Nota excluída'); }
                }}><Trash2 className="h-4 w-4" /></button>
              </div>
            </div>
          ))}
        </div>
      )}
      <Modal open={!!form} onClose={() => setForm(null)} title={form?.id ? 'Editar nota' : 'Nova nota'} size="sm"
        footer={<><Button variant="secondary" onClick={() => setForm(null)}>Cancelar</Button><Button onClick={salvar} disabled={!form?.conteudo && !form?.titulo}>Salvar</Button></>}>
        {form && (
          <div className="space-y-4">
            <Field label="Título"><Input value={form.titulo || ''} onChange={(e) => setForm({ ...form, titulo: e.target.value })} /></Field>
            <Field label="Conteúdo"><Textarea autoFocus className="min-h-[120px]" value={form.conteudo || ''} onChange={(e) => setForm({ ...form, conteudo: e.target.value })} /></Field>
            <div>
              <span className="label">Cor</span>
              <div className="flex gap-2">
                {Object.keys(CORES_NOTA).map((c) => (
                  <button key={c} title={c} onClick={() => setForm({ ...form, cor: c })} className={cn('h-8 w-8 rounded-full border-2', CORES_NOTA[c], form.cor === c ? 'scale-110 !border-slate-700' : '')} />
                ))}
              </div>
            </div>
            <Toggle checked={!!form.fixada} onChange={(v) => setForm({ ...form, fixada: v })} label="Fixar no topo" />
          </div>
        )}
      </Modal>
    </div>
  );
}
