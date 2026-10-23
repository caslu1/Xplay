import { useState } from 'react';
import { History, Undo2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { usePopup } from '../context/PopupContext';
import { desfazerAtividade } from '../lib/profile';
import { Modal, Button, Empty } from './ui';
import { cn } from '../lib/utils';

const CORES = {
  criar: 'bg-emerald-100 text-emerald-700', editar: 'bg-sky-100 text-sky-700', renovar: 'bg-brand-100 text-brand-700',
  excluir: 'bg-red-100 text-red-600', arquivar: 'bg-amber-100 text-amber-700', importar: 'bg-teal-100 text-teal-700', restaurar: 'bg-slate-100 text-slate-700',
};

export default function ActivityHistory({ dark }) {
  const { profile } = useAuth();
  const { confirm, toast } = usePopup();
  const [aberto, setAberto] = useState(false);
  const [desfazendo, setDesfazendo] = useState(null);
  const logs = profile?.activity_logs || [];

  async function desfazer(log) {
    if (!(await confirm({ title: 'Desfazer esta ação?', message: log.descricao }))) return;
    setDesfazendo(log.id);
    try {
      await desfazerAtividade(log.id);
      toast('Ação desfeita');
    } catch (e) {
      toast('Não foi possível desfazer: ' + e.message, 'error');
    } finally {
      setDesfazendo(null);
    }
  }

  return (
    <>
      <button onClick={() => setAberto(true)} aria-label="Histórico de atividades" title="Histórico de atividades"
        className={cn('rounded-xl p-2 transition', dark ? 'hover:bg-white/10' : 'text-slate-500 hover:bg-slate-100')}>
        <History className="h-5 w-5" />
      </button>
      <Modal open={aberto} onClose={() => setAberto(false)} title="Histórico de atividades" size="md">
        <p className="mb-3 text-xs text-slate-400">Últimas {logs.length} ações (guardadas por 3 dias, até 100). Use “Desfazer” para voltar atrás.</p>
        {logs.length === 0 && <Empty icon={History} title="Nada por aqui ainda">As ações em clientes e planos aparecem aqui.</Empty>}
        <ul className="space-y-2">
          {logs.map((l) => (
            <li key={l.id} className={cn('flex items-center gap-3 rounded-2xl border border-slate-100 p-3', l.desfeita && 'opacity-50')}>
              <span className={cn('rounded-lg px-2 py-1 text-[11px] font-bold uppercase', CORES[l.acao] || 'bg-slate-100')}>{l.acao}</span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-slate-800">{l.descricao}</p>
                <p className="text-xs text-slate-400">{new Date(l.data).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}{l.desfeita && ' · desfeita'}</p>
              </div>
              {!l.desfeita && l.desfazer?.length > 0 && (
                <Button size="sm" variant="secondary" icon={Undo2} loading={desfazendo === l.id} onClick={() => desfazer(l)}>Desfazer</Button>
              )}
            </li>
          ))}
        </ul>
      </Modal>
    </>
  );
}
