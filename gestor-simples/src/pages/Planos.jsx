import { useMemo, useState, useEffect } from 'react';
import { Package, Plus, Pencil, Trash2, Link2, Users, Copy } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { usePopup } from '../context/PopupContext';
import { salvarPlano, excluirPlano } from '../lib/acoes';
import { cn, formatarMoeda, PERIODOS, CORES_PASTEL, copiar } from '../lib/utils';
import { Button, Card, Badge, Modal, Field, Input, Select, Textarea, PageHeader, Empty, Spinner, IconButton, Toggle } from '../components/ui';

const VAZIO = { codigo: '', nome: '', preco: '', custo: '', periodo: 'mensal', link: '', descricao: '', cor: CORES_PASTEL[0], status: 'ativo' };

export default function Planos() {
  const { data, loading } = useEntities(['Plano', 'Cliente']);
  const { confirm, toast } = usePopup();
  const [form, setForm] = useState(null); // null | { plano? }

  const uso = useMemo(() => {
    const m = new Map();
    for (const c of data.Cliente) if (c.status === 'ativo') m.set(c.plano_id, (m.get(c.plano_id) || 0) + 1);
    return m;
  }, [data.Cliente]);

  async function excluir(p) {
    const n = data.Cliente.filter((c) => c.plano_id === p.id).length;
    const ok = await confirm({
      title: `Excluir o plano ${p.nome}?`, danger: true, confirmText: 'Excluir',
      message: n ? `${n} cliente(s) usam este plano. Eles continuam cadastrados, mas sem plano válido. Prefira inativar.` : 'Você pode desfazer pelo histórico.',
    });
    if (!ok) return;
    await excluirPlano(p);
    toast('Plano excluído');
  }

  if (loading) return <Spinner />;
  const planos = [...data.Plano].sort((a, b) => (a.status === b.status ? String(a.codigo || '').localeCompare(String(b.codigo || '')) : a.status === 'ativo' ? -1 : 1));

  return (
    <div>
      <PageHeader icon={Package} title="Planos" subtitle="Defina preços, custos e períodos dos seus planos"
        actions={<Button icon={Plus} onClick={() => setForm({})}>Novo plano</Button>} />
      {planos.length === 0 ? (
        <Card><Empty icon={Package} title="Nenhum plano cadastrado" action={<Button icon={Plus} onClick={() => setForm({})}>Criar plano</Button>}>Crie seu primeiro plano para começar a cadastrar clientes.</Empty></Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {planos.map((p) => {
            const lucro = Number(p.preco) - Number(p.custo);
            const margem = Number(p.preco) ? Math.round((lucro / Number(p.preco)) * 100) : 0;
            return (
              <Card key={p.id} className={cn('overflow-hidden', p.status === 'inativo' && 'opacity-60')}>
                <div className="h-2" style={{ background: p.cor }} />
                <div className="p-5">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-xs font-bold tracking-widest text-slate-400">#{p.codigo || '----'}</p>
                      <h3 className="text-lg font-extrabold text-slate-900">{p.nome}</h3>
                      <div className="mt-1 flex gap-1.5">
                        <Badge className="bg-slate-100 text-slate-600">{PERIODOS[p.periodo]}</Badge>
                        <Badge className={p.status === 'ativo' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}>{p.status}</Badge>
                      </div>
                    </div>
                    <div className="flex">
                      <IconButton icon={Pencil} title="Editar" onClick={() => setForm({ plano: p })} />
                      <IconButton icon={Trash2} title="Excluir" className="hover:!text-red-500" onClick={() => excluir(p)} />
                    </div>
                  </div>
                  <p className="mt-4 text-3xl font-black text-slate-900">{formatarMoeda(p.preco)}</p>
                  {p.descricao && <p className="mt-1 text-sm text-slate-500">{p.descricao}</p>}
                  <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs">
                    <div className="rounded-xl bg-slate-50 p-2"><p className="text-slate-400">Custo</p><p className="font-bold text-slate-700">{formatarMoeda(p.custo)}</p></div>
                    <div className="rounded-xl bg-emerald-50 p-2"><p className="text-emerald-600">Lucro</p><p className="font-bold text-emerald-700">{formatarMoeda(lucro)} <span className="font-medium">({margem}%)</span></p></div>
                    <div className="rounded-xl bg-brand-50 p-2"><p className="text-brand-600">Clientes</p><p className="flex items-center justify-center gap-1 font-bold text-brand-700"><Users className="h-3 w-3" />{uso.get(p.id) || 0}</p></div>
                  </div>
                  {p.link && (
                    <button onClick={async () => { await copiar(p.link); toast('Link copiado'); }} className="mt-3 flex w-full items-center gap-2 truncate rounded-xl border border-dashed border-slate-200 px-3 py-2 text-xs text-slate-500 hover:border-brand-300 hover:text-brand-600">
                      <Link2 className="h-3.5 w-3.5 shrink-0" /><span className="truncate">{p.link}</span><Copy className="ml-auto h-3.5 w-3.5 shrink-0" />
                    </button>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}
      <PlanoForm aberto={!!form} plano={form?.plano} planos={data.Plano} onClose={() => setForm(null)}
        onSave={async (dados) => { await salvarPlano(form.plano, dados); toast(form.plano ? 'Plano atualizado' : 'Plano criado'); setForm(null); }} />
    </div>
  );
}

function PlanoForm({ aberto, plano, planos, onClose, onSave }) {
  const [f, setF] = useState(VAZIO);
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  useEffect(() => {
    if (!aberto) return;
    setErro('');
    if (plano) setF({ ...VAZIO, ...plano });
    else {
      const usados = new Set(planos.map((p) => p.codigo));
      let cod = 1001;
      while (usados.has(String(cod))) cod++;
      setF({ ...VAZIO, codigo: String(cod), cor: CORES_PASTEL[planos.length % CORES_PASTEL.length] });
    }
  }, [aberto, plano, planos]);
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }));

  async function salvar() {
    if (!f.nome.trim()) return setErro('Informe o nome do plano.');
    if (!/^\d{4}$/.test(String(f.codigo))) return setErro('O código precisa ter 4 dígitos numéricos.');
    if (planos.some((p) => p.codigo === String(f.codigo) && p.id !== plano?.id)) return setErro('Já existe um plano com este código.');
    if (!(Number(f.preco) >= 0)) return setErro('Preço inválido.');
    setSalvando(true);
    try {
      const { id, created_date, updated_date, created_by, ...dados } = f;
      await onSave({ ...dados, codigo: String(f.codigo), nome: f.nome.trim(), preco: Number(f.preco) || 0, custo: Number(f.custo) || 0 });
    } catch (e) {
      setErro(e.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal open={aberto} onClose={onClose} title={plano ? 'Editar plano' : 'Novo plano'}
      footer={<><Button variant="secondary" onClick={onClose}>Cancelar</Button><Button loading={salvando} onClick={salvar}>Salvar</Button></>}>
      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Código *"><Input inputMode="numeric" maxLength={4} value={f.codigo} onChange={(e) => set('codigo', e.target.value.replace(/\D/g, '').slice(0, 4))} /></Field>
        <Field label="Nome *" className="sm:col-span-2"><Input autoFocus value={f.nome} onChange={(e) => set('nome', e.target.value)} placeholder="Ex.: Premium" /></Field>
        <Field label="Preço (R$) *"><Input type="number" step="0.01" min="0" value={f.preco} onChange={(e) => set('preco', e.target.value)} /></Field>
        <Field label="Custo (R$)"><Input type="number" step="0.01" min="0" value={f.custo} onChange={(e) => set('custo', e.target.value)} /></Field>
        <Field label="Período">
          <Select value={f.periodo} onChange={(e) => set('periodo', e.target.value)}>
            {Object.entries(PERIODOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </Select>
        </Field>
        <Field label="Link (opcional)" className="sm:col-span-3"><Input value={f.link || ''} onChange={(e) => set('link', e.target.value)} placeholder="https://… (pagamento, acesso, etc.)" /></Field>
        <Field label="Descrição" className="sm:col-span-3"><Textarea className="min-h-[70px]" value={f.descricao || ''} onChange={(e) => set('descricao', e.target.value)} /></Field>
        <div className="sm:col-span-3">
          <span className="label">Cor</span>
          <div className="flex flex-wrap gap-2">
            {CORES_PASTEL.map((c) => (
              <button key={c} type="button" onClick={() => set('cor', c)} className={cn('h-8 w-8 rounded-full border-2 transition', f.cor === c ? 'scale-110 border-slate-700' : 'border-white shadow')} style={{ background: c }} aria-label={c} />
            ))}
            <input type="color" value={f.cor} onChange={(e) => set('cor', e.target.value)} className="h-8 w-8 cursor-pointer rounded-full border-0 bg-transparent" title="Outra cor" />
          </div>
        </div>
        <div className="sm:col-span-3"><Toggle checked={f.status === 'ativo'} onChange={(v) => set('status', v ? 'ativo' : 'inativo')} label="Plano ativo (aparece no cadastro de clientes)" /></div>
        {erro && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-600 sm:col-span-3">{erro}</p>}
      </div>
    </Modal>
  );
}
