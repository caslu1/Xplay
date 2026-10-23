import { useMemo, useRef, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { useAuth } from '../context/AuthContext';
import { cn, situacaoVencimento, formatarDataBR } from '../lib/utils';

/** Sino com os clientes a vencer (configurável em Configurações → Painel). */
export default function VencimentoNotifications({ dark }) {
  const { profile } = useAuth();
  const { data } = useEntities(['Cliente']);
  const [aberto, setAberto] = useState(false);
  const ref = useRef(null);
  const cfg = profile?.dashboard_settings || {};
  const dias = Number(cfg.dias_antecedencia ?? 3);

  const lista = useMemo(() => {
    if (cfg.notificar_vencimentos === false) return [];
    return data.Cliente
      .filter((c) => c.status === 'ativo')
      .map((c) => ({ c, s: situacaoVencimento(c.data_vencimento) }))
      .filter(({ s }) => s.dias !== null && s.dias <= dias && s.dias >= -(cfg.dias_atraso ?? 7))
      .sort((a, b) => a.s.dias - b.s.dias);
  }, [data.Cliente, dias, cfg.notificar_vencimentos, cfg.dias_atraso]);

  useEffect(() => {
    const fora = (e) => ref.current && !ref.current.contains(e.target) && setAberto(false);
    document.addEventListener('mousedown', fora);
    return () => document.removeEventListener('mousedown', fora);
  }, []);

  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setAberto(!aberto)} aria-label="Notificações"
        className={cn('relative rounded-xl p-2 transition', dark ? 'hover:bg-white/10' : 'text-slate-500 hover:bg-slate-100')}>
        <Bell className="h-5 w-5" />
        {lista.length > 0 && <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">{lista.length > 99 ? '99+' : lista.length}</span>}
      </button>
      {aberto && (
        <div className="animate-pop absolute right-0 top-12 z-50 w-80 max-w-[calc(100vw-24px)] overflow-hidden rounded-2xl border border-slate-200 bg-white text-slate-800 shadow-2xl">
          <div className="border-b border-slate-100 px-4 py-3">
            <p className="font-bold">Vencimentos</p>
            <p className="text-xs text-slate-500">Atrasados e vencendo nos próximos {dias} dias</p>
          </div>
          <div className="max-h-80 overflow-y-auto">
            {lista.length === 0 && <p className="px-4 py-8 text-center text-sm text-slate-400">Nenhum vencimento próximo 🎉</p>}
            {lista.map(({ c, s }) => (
              <Link key={c.id} to={`/Clientes?busca=${encodeURIComponent(c.nome)}`} onClick={() => setAberto(false)}
                className="flex items-center justify-between gap-2 border-b border-slate-50 px-4 py-2.5 hover:bg-slate-50">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{c.nome}</p>
                  <p className="text-xs text-slate-400">{c.plano_nome} · {formatarDataBR(c.data_vencimento)}</p>
                </div>
                <span className={cn('shrink-0 text-xs font-bold', s.cls)}>{s.label}</span>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
