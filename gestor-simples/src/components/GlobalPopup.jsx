import { useEffect, useState } from 'react';
import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react';
import { Button, Input } from './ui';
import { cn } from '../lib/utils';

export default function GlobalPopup({ popup, onClose, toasts, dismissToast }) {
  const [texto, setTexto] = useState('');
  useEffect(() => setTexto(popup?.defaultValue || ''), [popup]);

  return (
    <>
      {popup && (
        <div className="animate-fade fixed inset-0 z-[70] flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
          <div className="animate-pop w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <div className={cn('mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl', popup.danger ? 'bg-red-100 text-red-600' : 'bg-brand-100 text-brand-600')}>
              {popup.danger ? <AlertTriangle className="h-6 w-6" /> : <Info className="h-6 w-6" />}
            </div>
            <h3 className="text-center text-lg font-bold text-slate-900">{popup.title}</h3>
            {popup.message && <p className="mt-1 whitespace-pre-line text-center text-sm text-slate-500">{popup.message}</p>}
            {popup.kind === 'prompt' && (
              <Input autoFocus className="mt-4" value={texto} placeholder={popup.placeholder} type={popup.inputType || 'text'}
                onChange={(e) => setTexto(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && onClose(texto)} />
            )}
            <div className="mt-5 flex gap-2">
              {popup.kind !== 'alert' && (
                <Button variant="secondary" className="flex-1" onClick={() => onClose(popup.kind === 'prompt' ? null : false)}>
                  {popup.cancelText || 'Cancelar'}
                </Button>
              )}
              <Button variant={popup.danger ? 'danger' : 'primary'} className={cn('flex-1', popup.danger && '!bg-red-600 !text-white hover:!bg-red-700')}
                onClick={() => onClose(popup.kind === 'prompt' ? texto : true)}>
                {popup.confirmText || (popup.kind === 'alert' ? 'Entendi' : 'Confirmar')}
              </Button>
            </div>
          </div>
        </div>
      )}
      <div className="pointer-events-none fixed inset-x-0 bottom-20 z-[80] flex flex-col items-center gap-2 px-4 lg:bottom-6">
        {toasts.map((t) => (
          <div key={t.id} className="animate-pop pointer-events-auto flex max-w-md items-center gap-3 rounded-2xl bg-slate-900 px-4 py-3 text-sm text-white shadow-xl">
            {t.tipo === 'error' ? <XCircle className="h-5 w-5 shrink-0 text-red-400" /> : <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-400" />}
            <span>{t.mensagem}</span>
            {t.acao && (
              <button className="ml-1 font-bold text-brand-300 hover:text-brand-200" onClick={() => { t.acao.onClick(); dismissToast(t.id); }}>
                {t.acao.label}
              </button>
            )}
          </div>
        ))}
      </div>
    </>
  );
}
