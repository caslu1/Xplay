import { useEffect, useState } from 'react';
import { Modal, Button, Field, Select, Input, Toggle } from './ui';
import { addPeriodo, formatarDataBR, formatarMoeda, hojeISO, valorCliente, METODOS, diffDias } from '../lib/utils';

export default function RenovarModal({ open, onClose, cliente, plano, onConfirm }) {
  const [metodo, setMetodo] = useState('pix');
  const [meses, setMeses] = useState(1);
  const [deHoje, setDeHoje] = useState(false);
  const [valor, setValor] = useState(0);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!open || !cliente) return;
    setMetodo('pix');
    setMeses(1);
    setDeHoje(diffDias(String(cliente.data_vencimento).slice(0, 10), hojeISO()) > 30);
  }, [open, cliente]);
  useEffect(() => { if (cliente) setValor(valorCliente(cliente, plano) * meses); }, [cliente, plano, meses]);

  if (!cliente) return null;
  let nova = deHoje ? hojeISO() : String(cliente.data_vencimento).slice(0, 10);
  for (let i = 0; i < meses; i++) nova = addPeriodo(nova, plano?.periodo);

  async function confirmar() {
    setSalvando(true);
    try {
      await onConfirm({ metodo, meses, aPartirDeHoje: deHoje, valor: Number(valor) });
      onClose();
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={`Renovar ${cliente.nome}`} size="sm"
      footer={<><Button variant="secondary" onClick={onClose}>Cancelar</Button><Button variant="success" loading={salvando} onClick={confirmar}>Confirmar renovação</Button></>}>
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3 rounded-2xl bg-slate-50 p-3 text-sm">
          <div><p className="text-xs text-slate-400">Vencimento atual</p><p className="font-bold">{formatarDataBR(cliente.data_vencimento)}</p></div>
          <div><p className="text-xs text-slate-400">Novo vencimento</p><p className="font-bold text-emerald-600">{formatarDataBR(nova)}</p></div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Períodos">
            <Select value={meses} onChange={(e) => setMeses(Number(e.target.value))}>
              {[1, 2, 3, 6, 12].map((n) => <option key={n} value={n}>{n}× {plano?.periodo || 'mensal'}</option>)}
            </Select>
          </Field>
          <Field label="Forma de pagamento">
            <Select value={metodo} onChange={(e) => setMetodo(e.target.value)}>
              {['pix', 'dinheiro', 'cartao', 'transferencia', 'renovacao'].map((m) => <option key={m} value={m}>{METODOS[m]}</option>)}
            </Select>
          </Field>
        </div>
        <Field label="Valor recebido (R$)" hint={`Sugerido: ${formatarMoeda(valorCliente(cliente, plano) * meses)}`}>
          <Input type="number" step="0.01" min="0" value={valor} onChange={(e) => setValor(e.target.value)} />
        </Field>
        <Toggle checked={deHoje} onChange={setDeHoje} label="Contar o novo período a partir de hoje" />
      </div>
    </Modal>
  );
}
