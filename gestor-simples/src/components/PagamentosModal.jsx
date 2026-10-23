import { useMemo } from 'react';
import { Receipt, Trash2 } from 'lucide-react';
import { Modal, Empty, Badge, IconButton } from './ui';
import { useEntities } from '../lib/hooks';
import { db } from '../lib/db';
import { usePopup } from '../context/PopupContext';
import { formatarDataBR, formatarMoeda, METODOS } from '../lib/utils';

const STATUS = { pago: 'bg-emerald-100 text-emerald-700', pendente: 'bg-amber-100 text-amber-700', vencido: 'bg-red-100 text-red-600', cancelado: 'bg-slate-100 text-slate-500' };

export default function PagamentosModal({ open, onClose, cliente }) {
  const { data } = useEntities(['Mensalidade', 'HistoricoMensagem']);
  const { confirm, toast } = usePopup();
  const lista = useMemo(() => data.Mensalidade.filter((m) => m.cliente_id === cliente?.id)
    .sort((a, b) => String(b.data_pagamento || b.created_date).localeCompare(String(a.data_pagamento || a.created_date))), [data.Mensalidade, cliente]);
  const msgs = useMemo(() => data.HistoricoMensagem.filter((m) => m.cliente_id === cliente?.id).slice(0, 10), [data.HistoricoMensagem, cliente]);
  if (!cliente) return null;
  const total = lista.filter((m) => m.status === 'pago').reduce((s, m) => s + Number(m.valor), 0);

  return (
    <Modal open={open} onClose={onClose} title={`Histórico — ${cliente.nome}`} size="lg">
      <div className="mb-4 grid grid-cols-2 gap-3">
        <div className="rounded-2xl bg-brand-50 p-3"><p className="text-xs text-brand-600">Total pago</p><p className="text-lg font-extrabold text-brand-800">{formatarMoeda(total)}</p></div>
        <div className="rounded-2xl bg-slate-50 p-3"><p className="text-xs text-slate-500">Pagamentos</p><p className="text-lg font-extrabold">{lista.length}</p></div>
      </div>
      {lista.length === 0 ? <Empty icon={Receipt} title="Nenhum pagamento registrado" /> : (
        <div className="divide-y divide-slate-100 rounded-2xl border border-slate-100">
          {lista.map((m) => (
            <div key={m.id} className="flex items-center gap-3 px-4 py-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold">{m.descricao || m.plano_nome}</p>
                <p className="text-xs text-slate-400">Pago em {formatarDataBR(m.data_pagamento)} · {METODOS[m.metodo_pagamento] || m.metodo_pagamento}</p>
              </div>
              <Badge className={STATUS[m.status]}>{m.status}</Badge>
              <p className="w-24 text-right text-sm font-bold">{formatarMoeda(m.valor)}</p>
              <IconButton icon={Trash2} title="Excluir pagamento" className="hover:!text-red-500" onClick={async () => {
                if (await confirm({ title: 'Excluir este pagamento?', message: 'Ele deixa de contar no financeiro.', danger: true })) {
                  await db.Mensalidade.delete(m.id);
                  toast('Pagamento excluído');
                }
              }} />
            </div>
          ))}
        </div>
      )}
      {msgs.length > 0 && (
        <>
          <p className="mb-2 mt-5 text-xs font-bold uppercase tracking-wide text-slate-500">Mensagens recentes</p>
          <ul className="space-y-1.5 text-sm">
            {msgs.map((h) => (
              <li key={h.id} className="flex items-center justify-between gap-2 rounded-xl bg-slate-50 px-3 py-2">
                <span className="truncate">{h.tipo_mensagem} · {new Date(h.data_envio).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</span>
                <Badge className={h.status === 'enviada' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-600'}>{h.status}</Badge>
              </li>
            ))}
          </ul>
        </>
      )}
    </Modal>
  );
}
