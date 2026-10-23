import { Link } from 'react-router-dom';
import { Users, RefreshCw, MessageCircle, BarChart3, Target, Smartphone, ShieldCheck, Zap, Check } from 'lucide-react';
import Logo from '../components/Logo';
import { useAuth } from '../context/AuthContext';

const RECURSOS = [
  { icon: Users, t: 'Clientes organizados', d: 'Cadastro completo com plano, vencimento, conexões e desconto. Busca, filtros, arquivamento e ações em lote.' },
  { icon: RefreshCw, t: 'Renovação em 1 toque', d: 'Renove a mensalidade, registre o pagamento e já copie a mensagem de confirmação.' },
  { icon: MessageCircle, t: 'Cobrança no WhatsApp', d: 'Lembretes automáticos antes, no dia e após o vencimento, com mensagens geradas por IA.' },
  { icon: BarChart3, t: 'Financeiro claro', d: 'Receita, custo, lucro, ticket médio, renovação e comparador de planos entre meses. Exporte em PDF.' },
  { icon: Target, t: 'Metas, tarefas e notas', d: 'Defina objetivos do mês, organize tarefas e deixe recados fixados à mão.' },
  { icon: ShieldCheck, t: 'Seguro e com desfazer', d: 'Bloqueio por PIN, backup dos dados e histórico de ações com botão “Desfazer”.' },
];

export default function LandingPage() {
  const { user } = useAuth();
  const cta = user ? '/Dashboard' : '/Login';
  return (
    <div className="min-h-screen bg-white">
      <header className="sticky top-0 z-20 border-b border-slate-100 bg-white/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3">
          <Logo />
          <Link to={cta} className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-bold text-white shadow-md shadow-brand-600/25 hover:bg-brand-700">
            {user ? 'Abrir o app' : 'Entrar'}
          </Link>
        </div>
      </header>

      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-b from-brand-50 to-white" />
        <div className="absolute -right-32 -top-32 -z-10 h-96 w-96 rounded-full bg-fuchsia-200/40 blur-3xl" />
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-5 py-16 lg:grid-cols-2 lg:py-24">
          <div>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-100 px-3 py-1 text-xs font-bold text-brand-700"><Zap className="h-3.5 w-3.5" /> Gestão de mensalidades sem complicação</span>
            <h1 className="mt-4 text-4xl font-black leading-[1.1] tracking-tight text-slate-900 sm:text-5xl">
              Pare de perder dinheiro com <span className="bg-gradient-to-r from-brand-600 to-fuchsia-600 bg-clip-text text-transparent">mensalidades esquecidas</span>.
            </h1>
            <p className="mt-5 max-w-lg text-lg text-slate-600">O Gestor Simples controla seus clientes, avisa quem vai vencer e cobra pelo WhatsApp sozinho. No celular e no computador.</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={cta} className="rounded-2xl bg-brand-600 px-6 py-3.5 font-bold text-white shadow-lg shadow-brand-600/30 hover:bg-brand-700">Começar agora — é grátis</Link>
              <a href="#recursos" className="rounded-2xl border border-slate-200 px-6 py-3.5 font-bold text-slate-700 hover:bg-slate-50">Ver recursos</a>
            </div>
            <ul className="mt-6 flex flex-wrap gap-x-5 gap-y-2 text-sm text-slate-500">
              {['Sem cartão de crédito', 'Instale no celular', 'Dados só seus'].map((x) => <li key={x} className="flex items-center gap-1.5"><Check className="h-4 w-4 text-emerald-500" />{x}</li>)}
            </ul>
          </div>
          <div className="relative mx-auto w-full max-w-md">
            <div className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-2xl shadow-brand-900/10">
              <div className="rounded-2xl bg-gradient-to-r from-brand-700 to-brand-600 p-4 text-white">
                <p className="text-xs text-brand-200">Receita do mês</p>
                <p className="text-3xl font-black">R$ 4.820,00</p>
                <p className="text-xs text-emerald-300">▲ 12% vs mês anterior</p>
              </div>
              <div className="mt-3 space-y-2">
                {[['Ana Souza', 'Vence hoje', 'text-orange-600'], ['Bruno Lima', 'Vence em 2 dias', 'text-amber-600'], ['Carla Mendes', 'Renovado ✓', 'text-emerald-600']].map(([n, s, c]) => (
                  <div key={n} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2.5">
                    <div className="flex items-center gap-2"><div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-700">{n[0]}</div><span className="text-sm font-semibold">{n}</span></div>
                    <span className={`text-xs font-bold ${c}`}>{s}</span>
                  </div>
                ))}
              </div>
              <div className="mt-3 rounded-xl bg-[#dcf8c6] p-3 text-sm text-slate-700">Olá Ana! 😊 Seu plano Premium vence hoje. Valor: R$ 50,00. Me chama que eu te mando o PIX!</div>
            </div>
          </div>
        </div>
      </section>

      <section id="recursos" className="mx-auto max-w-6xl px-5 py-16">
        <h2 className="text-center text-3xl font-black text-slate-900">Tudo o que você precisa, nada que atrapalhe</h2>
        <p className="mx-auto mt-2 max-w-xl text-center text-slate-500">Feito para quem vende planos recorrentes e quer tempo livre.</p>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {RECURSOS.map(({ icon: Icon, t, d }) => (
            <div key={t} className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg">
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-100 text-brand-700"><Icon className="h-6 w-6" /></div>
              <h3 className="font-bold text-slate-900">{t}</h3>
              <p className="mt-1 text-sm text-slate-500">{d}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-5 pb-20">
        <div className="flex flex-col items-center gap-6 rounded-[2rem] bg-gradient-to-br from-brand-700 to-fuchsia-600 px-8 py-12 text-center text-white md:flex-row md:text-left">
          <Smartphone className="h-14 w-14 shrink-0" />
          <div className="flex-1">
            <h3 className="text-2xl font-black">Leve no bolso</h3>
            <p className="mt-1 text-brand-100">Abra no celular e toque em “Adicionar à tela inicial” (ou “Instalar app”). Funciona como um aplicativo, até offline.</p>
          </div>
          <Link to={cta} className="rounded-2xl bg-white px-6 py-3.5 font-bold text-brand-700 hover:bg-brand-50">Abrir o Gestor</Link>
        </div>
      </section>

      <footer className="border-t border-slate-100 py-8 text-center text-sm text-slate-400">© {new Date().getFullYear()} Gestor Simples · gestão de clientes e mensalidades</footer>
    </div>
  );
}
