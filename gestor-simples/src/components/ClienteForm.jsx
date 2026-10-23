import { useEffect, useState } from 'react';
import { Star } from 'lucide-react';
import { Modal, Button, Field, Input, Select, Textarea, Toggle } from './ui';
import { addPeriodo, hojeISO, formatarMoeda, valorCliente, cn } from '../lib/utils';

const VAZIO = { nome: '', whatsapp: '', plano_id: '', data_vencimento: '', quantidade: 1, desconto: 0, notas: '', status: 'ativo', essencial: false };

export default function ClienteForm({ open, onClose, cliente, planos, onSave, inicial }) {
  const [f, setF] = useState(VAZIO);
  const [registrar, setRegistrar] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const ativos = planos.filter((p) => p.status === 'ativo' || p.id === cliente?.plano_id);

  useEffect(() => {
    if (!open) return;
    setErro('');
    if (cliente) setF({ ...VAZIO, ...cliente, data_vencimento: String(cliente.data_vencimento).slice(0, 10) });
    else {
      const plano = ativos[0];
      setF({ ...VAZIO, plano_id: plano?.id || '', data_vencimento: addPeriodo(hojeISO(), plano?.periodo), ...inicial });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, cliente]);

  const set = (k, v) => setF((x) => ({ ...x, [k]: v }));
  const plano = planos.find((p) => p.id === f.plano_id);

  async function salvar(e) {
    e?.preventDefault();
    if (!f.nome.trim() || !f.whatsapp.trim() || !f.plano_id || !f.data_vencimento) {
      setErro('Preencha nome, WhatsApp, plano e vencimento.');
      return;
    }
    const digitos = f.whatsapp.replace(/\D/g, '');
    if (digitos.length < 10) {
      setErro('WhatsApp inválido: use DDD + número.');
      return;
    }
    setSalvando(true);
    try {
      const { id, created_date, updated_date, created_by, ...dados } = f;
      await onSave({ ...dados, nome: f.nome.trim(), whatsapp: digitos, quantidade: Number(f.quantidade) || 1, desconto: Number(f.desconto) || 0 }, plano, { registrarPagamento: registrar });
      onClose();
    } catch (e2) {
      setErro(e2.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={cliente ? 'Editar cliente' : 'Novo cliente'}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={salvar} loading={salvando}>{cliente ? 'Salvar alterações' : 'Cadastrar cliente'}</Button>
      </>}>
      <form onSubmit={salvar} className="grid gap-4 sm:grid-cols-2">
        <Field label="Nome *" className="sm:col-span-2"><Input autoFocus value={f.nome} onChange={(e) => set('nome', e.target.value)} placeholder="Nome do cliente" /></Field>
        <Field label="WhatsApp *" hint="DDD + número. O 55 é adicionado sozinho."><Input inputMode="tel" value={f.whatsapp} onChange={(e) => set('whatsapp', e.target.value)} placeholder="(11) 99999-9999" /></Field>
        <Field label="Plano *">
          <Select value={f.plano_id} onChange={(e) => {
            const p = planos.find((x) => x.id === e.target.value);
            setF((x) => ({ ...x, plano_id: e.target.value, ...(!cliente && p ? { data_vencimento: addPeriodo(hojeISO(), p.periodo) } : {}) }));
          }}>
            {ativos.length === 0 && <option value="">Cadastre um plano primeiro</option>}
            {ativos.map((p) => <option key={p.id} value={p.id}>{p.nome} — {formatarMoeda(p.preco)}</option>)}
          </Select>
        </Field>
        <Field label="Vencimento *"><Input type="date" value={f.data_vencimento} onChange={(e) => set('data_vencimento', e.target.value)} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Conexões">
            <Select value={f.quantidade} onChange={(e) => set('quantidade', Number(e.target.value))}>
              {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => <option key={n} value={n}>{n}</option>)}
            </Select>
          </Field>
          <Field label="Desconto (R$)"><Input type="number" min="0" step="0.01" value={f.desconto} onChange={(e) => set('desconto', e.target.value)} /></Field>
        </div>
        {cliente && (
          <Field label="Status">
            <Select value={f.status} onChange={(e) => set('status', e.target.value)}>
              <option value="ativo">Ativo</option><option value="pendente">Pendente</option><option value="inativo">Inativo</option><option value="arquivado">Arquivado</option>
            </Select>
          </Field>
        )}
        <Field label="Notas" className="sm:col-span-2"><Textarea value={f.notas || ''} onChange={(e) => set('notas', e.target.value)} placeholder="Observações (opcional)" /></Field>
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-brand-50 p-3 sm:col-span-2">
          <button type="button" onClick={() => set('essencial', !f.essencial)} className={cn('inline-flex items-center gap-2 text-sm font-semibold', f.essencial ? 'text-amber-600' : 'text-slate-500')}>
            <Star className={cn('h-4 w-4', f.essencial && 'fill-amber-400')} /> Cliente essencial
          </button>
          <p className="text-sm text-slate-600">Valor: <b className="text-brand-700">{formatarMoeda(valorCliente({ quantidade: f.quantidade, desconto: f.desconto }, plano))}</b></p>
        </div>
        {!cliente && <div className="sm:col-span-2"><Toggle checked={registrar} onChange={setRegistrar} label="Registrar o primeiro pagamento no financeiro" /></div>}
        {erro && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-600 sm:col-span-2">{erro}</p>}
        <button type="submit" className="hidden" />
      </form>
    </Modal>
  );
}
