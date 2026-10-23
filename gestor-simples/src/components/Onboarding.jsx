import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Package, MessageSquare, BarChart3, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Button } from './ui';
import { cn } from '../lib/utils';

const PASSOS = [
  { icon: Sparkles, titulo: 'Bem-vindo ao Gestor Simples!', texto: 'Em poucos passos você organiza seus clientes, mensalidades e cobranças. Vamos fazer um tour rápido?' },
  { icon: Package, titulo: '1. Cadastre seus planos', texto: 'Em Planos, crie cada plano com preço, custo e período (mensal, trimestral, semestral ou anual).', rota: '/Planos' },
  { icon: Users, titulo: '2. Adicione seus clientes', texto: 'Em Clientes, cadastre nome, WhatsApp, plano e vencimento. Você também pode importar uma planilha.', rota: '/Clientes' },
  { icon: MessageSquare, titulo: '3. Automatize as cobranças', texto: 'Em Mensagens automáticas, configure lembretes antes, no dia e depois do vencimento pelo WhatsApp.', rota: '/MensagensAutomaticas' },
  { icon: BarChart3, titulo: '4. Acompanhe o financeiro', texto: 'Receita, custo, lucro, gráficos e o comparador de planos entre meses — tudo em Financeiro.', rota: '/Faturamento' },
];

export default function Onboarding() {
  const { updateProfile } = useAuth();
  const [passo, setPasso] = useState(0);
  const nav = useNavigate();
  const p = PASSOS[passo];
  const fim = () => updateProfile({ onboarding_completed: true });

  return (
    <div className="animate-fade fixed inset-0 z-[60] flex items-end justify-center bg-slate-900/60 p-4 backdrop-blur-sm sm:items-center">
      <div className="animate-pop w-full max-w-md rounded-3xl bg-white p-6 text-center shadow-2xl">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-3xl bg-gradient-to-br from-brand-500 to-fuchsia-500 text-white shadow-lg">
          <p.icon className="h-8 w-8" />
        </div>
        <h2 className="text-lg font-extrabold text-slate-900">{p.titulo}</h2>
        <p className="mt-2 text-sm text-slate-500">{p.texto}</p>
        <div className="my-5 flex justify-center gap-1.5">
          {PASSOS.map((_, i) => <span key={i} className={cn('h-1.5 rounded-full transition-all', i === passo ? 'w-6 bg-brand-600' : 'w-1.5 bg-slate-200')} />)}
        </div>
        <div className="flex gap-2">
          <Button variant="ghost" className="flex-1" onClick={fim}>Pular</Button>
          <Button className="flex-1" onClick={() => {
            if (passo === PASSOS.length - 1) { fim(); nav('/Planos'); return; }
            setPasso(passo + 1);
            if (PASSOS[passo + 1].rota) nav(PASSOS[passo + 1].rota);
          }}>
            {passo === PASSOS.length - 1 ? 'Começar' : 'Próximo'}
          </Button>
        </div>
      </div>
    </div>
  );
}
