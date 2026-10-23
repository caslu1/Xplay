import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Users, Package, BarChart3, Target, StickyNote, MessageSquare, CheckSquare, Settings, Upload,
  DatabaseBackup, LogOut, Menu, X, MoreHorizontal,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { cn } from '../lib/utils';
import VencimentoNotifications from './VencimentoNotifications';
import ActivityHistory from './ActivityHistory';
import PINLockScreen from './PINLockScreen';
import Onboarding from './Onboarding';
import Logo from './Logo';

export const NAV = [
  { to: '/Dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/Clientes', label: 'Clientes', icon: Users },
  { to: '/Planos', label: 'Planos', icon: Package },
  { to: '/Faturamento', label: 'Financeiro', icon: BarChart3 },
  { to: '/Metas', label: 'Metas', icon: Target },
  { to: '/Tarefas', label: 'Tarefas', icon: CheckSquare },
  { to: '/NotasAdesivas', label: 'Notas', icon: StickyNote },
  { to: '/MensagensAutomaticas', label: 'Mensagens automáticas', icon: MessageSquare },
  { to: '/ImportarClientes', label: 'Importar clientes', icon: Upload },
  { to: '/BackupDados', label: 'Backup', icon: DatabaseBackup },
  { to: '/Configuracoes', label: 'Configurações', icon: Settings },
];

const BOTTOM = NAV.slice(0, 4);

export default function Layout() {
  const { profile, logout, demo } = useAuth();
  const [drawer, setDrawer] = useState(false);
  const [bloqueado, setBloqueado] = useState(() => !!profile?.pin_lock_enabled && !sessionStorage.getItem('gs_pin_ok'));
  const loc = useLocation();

  useEffect(() => setDrawer(false), [loc.pathname]);
  useEffect(() => {
    if (profile?.pin_lock_enabled && !sessionStorage.getItem('gs_pin_ok')) setBloqueado(true);
  }, [profile?.pin_lock_enabled]);

  if (bloqueado && profile?.pin_lock_enabled) {
    return <PINLockScreen onUnlock={() => { sessionStorage.setItem('gs_pin_ok', '1'); setBloqueado(false); }} />;
  }

  const atual = NAV.find((n) => loc.pathname.startsWith(n.to));

  const links = (
    <nav className="flex flex-col gap-0.5">
      {NAV.map(({ to, label, icon: Icon }) => (
        <NavLink key={to} to={to} data-tour={to.slice(1)}
          className={({ isActive }) => cn('flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition',
            isActive ? 'bg-brand-600 text-white shadow-md shadow-brand-600/25' : 'text-slate-600 hover:bg-brand-50 hover:text-brand-700')}>
          <Icon className="h-[18px] w-[18px]" /> {label}
        </NavLink>
      ))}
    </nav>
  );

  const usuario = (
    <div className="flex items-center gap-3 rounded-2xl bg-slate-50 p-3">
      {profile?.profile_photo
        ? <img src={profile.profile_photo} alt="" className="h-9 w-9 rounded-full object-cover" />
        : <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-200 font-bold text-brand-800">{(profile?.full_name || '?')[0].toUpperCase()}</div>}
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-bold text-slate-800">{profile?.full_name}</p>
        <p className="truncate text-xs text-slate-500">{profile?.empresa || profile?.email}</p>
      </div>
      <button onClick={logout} title="Sair" className="rounded-lg p-1.5 text-slate-400 hover:bg-white hover:text-red-500"><LogOut className="h-4 w-4" /></button>
    </div>
  );

  return (
    <div className="min-h-screen lg:pl-64">
      {/* Sidebar desktop */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-slate-200 bg-white px-4 py-5 lg:flex">
        <div className="mb-6 px-2"><Logo /></div>
        <div className="flex-1 overflow-y-auto">{links}</div>
        {usuario}
      </aside>

      {/* Header mobile roxo */}
      <header className="safe-top sticky top-0 z-30 bg-gradient-to-r from-brand-700 to-brand-600 text-white shadow-lg lg:hidden">
        <div className="flex h-14 items-center gap-2 px-3">
          <button onClick={() => setDrawer(true)} className="rounded-xl p-2 hover:bg-white/10" aria-label="Menu"><Menu className="h-5 w-5" /></button>
          <p className="flex-1 truncate text-base font-bold">{atual?.label || 'Gestor Simples'}</p>
          <ActivityHistory dark />
          <VencimentoNotifications dark />
        </div>
      </header>

      {/* Barra superior desktop */}
      <div className="sticky top-0 z-20 hidden h-14 items-center justify-end gap-1 border-b border-slate-200/70 bg-slate-50/80 px-6 backdrop-blur lg:flex">
        {demo && <span className="mr-auto rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700">Modo demonstração — dados salvos só neste navegador</span>}
        <ActivityHistory />
        <VencimentoNotifications />
      </div>

      {demo && <div className="bg-amber-100 px-4 py-1.5 text-center text-xs font-semibold text-amber-800 lg:hidden">Modo demonstração — dados salvos só neste aparelho</div>}

      <main className="mx-auto max-w-7xl px-4 pb-28 pt-5 sm:px-6 lg:pb-10">
        <Outlet />
      </main>

      {/* Drawer mobile */}
      {drawer && (
        <div className="animate-fade fixed inset-0 z-50 bg-slate-900/50 lg:hidden" onClick={() => setDrawer(false)}>
          <div className="animate-pop safe-top flex h-full w-72 flex-col bg-white px-4 py-4" onClick={(e) => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between px-2">
              <Logo />
              <button onClick={() => setDrawer(false)} className="rounded-lg p-1.5 hover:bg-slate-100"><X className="h-5 w-5" /></button>
            </div>
            <div className="flex-1 overflow-y-auto">{links}</div>
            {usuario}
          </div>
        </div>
      )}

      {/* Barra de navegação mobile */}
      <nav className="safe-bottom fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 backdrop-blur lg:hidden">
        <div className="grid grid-cols-5">
          {BOTTOM.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => cn('flex flex-col items-center gap-0.5 py-2 text-[11px] font-semibold', isActive ? 'text-brand-600' : 'text-slate-400')}>
              {({ isActive }) => (
                <>
                  <span className={cn('rounded-full px-4 py-1 transition', isActive && 'bg-brand-100')}><Icon className="h-5 w-5" /></span>
                  {label === 'Dashboard' ? 'Início' : label}
                </>
              )}
            </NavLink>
          ))}
          <button onClick={() => setDrawer(true)} className="flex flex-col items-center gap-0.5 py-2 text-[11px] font-semibold text-slate-400">
            <span className="rounded-full px-4 py-1"><MoreHorizontal className="h-5 w-5" /></span>Mais
          </button>
        </div>
      </nav>

      {profile && !profile.onboarding_completed && <Onboarding />}
    </div>
  );
}
