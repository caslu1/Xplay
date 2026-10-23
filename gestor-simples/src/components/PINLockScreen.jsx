import { useState } from 'react';
import { Delete, Lock } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { cn } from '../lib/utils';

export default function PINLockScreen({ onUnlock }) {
  const { profile, logout } = useAuth();
  const [pin, setPin] = useState('');
  const [erro, setErro] = useState(false);
  const tamanho = String(profile?.pin_lock_code || '').length || 4;

  const digitar = (d) => {
    if (pin.length >= tamanho) return;
    const novo = pin + d;
    setErro(false);
    setPin(novo);
    if (novo.length === tamanho) {
      if (novo === String(profile.pin_lock_code)) onUnlock();
      else setTimeout(() => { setErro(true); setPin(''); }, 150);
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-brand-700 via-brand-600 to-fuchsia-600 p-6 text-white">
      <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-3xl bg-white/15"><Lock className="h-8 w-8" /></div>
      <h1 className="text-xl font-bold">App bloqueado</h1>
      <p className="mb-6 text-sm text-white/70">Digite seu PIN para continuar</p>
      <div className={cn('mb-8 flex gap-3', erro && 'animate-bounce')}>
        {Array.from({ length: tamanho }).map((_, i) => (
          <span key={i} className={cn('h-4 w-4 rounded-full border-2 border-white transition', i < pin.length && 'bg-white', erro && 'border-red-300')} />
        ))}
      </div>
      {erro && <p className="-mt-5 mb-4 text-sm font-semibold text-red-200">PIN incorreto</p>}
      <div className="grid grid-cols-3 gap-4">
        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((n) => (
          <button key={n} onClick={() => digitar(String(n))} className="h-16 w-16 rounded-full bg-white/15 text-2xl font-semibold transition active:scale-95 hover:bg-white/25">{n}</button>
        ))}
        <span />
        <button onClick={() => digitar('0')} className="h-16 w-16 rounded-full bg-white/15 text-2xl font-semibold transition active:scale-95 hover:bg-white/25">0</button>
        <button onClick={() => setPin(pin.slice(0, -1))} className="flex h-16 w-16 items-center justify-center rounded-full transition hover:bg-white/10" aria-label="Apagar"><Delete className="h-6 w-6" /></button>
      </div>
      <button onClick={logout} className="mt-10 text-sm text-white/70 underline">Esqueci o PIN — sair da conta</button>
    </div>
  );
}
