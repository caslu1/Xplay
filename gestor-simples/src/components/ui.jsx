import { useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { cn } from '../lib/utils';

const VARIANTES = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-sm shadow-brand-600/20',
  secondary: 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-50',
  ghost: 'text-slate-600 hover:bg-slate-100',
  danger: 'bg-red-50 text-red-600 hover:bg-red-100',
  success: 'bg-emerald-600 text-white hover:bg-emerald-700',
  whatsapp: 'bg-[#25D366] text-white hover:bg-[#1ebe5b]',
};

export function Button({ variant = 'primary', size = 'md', className, loading, children, icon: Icon, ...props }) {
  const tam = size === 'sm' ? 'px-3 py-1.5 text-xs' : size === 'lg' ? 'px-5 py-3 text-base' : 'px-4 py-2.5 text-sm';
  return (
    <button
      className={cn('inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition disabled:cursor-not-allowed disabled:opacity-50', VARIANTES[variant], tam, className)}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : Icon ? <Icon className={size === 'sm' ? 'h-3.5 w-3.5' : 'h-4 w-4'} /> : null}
      {children}
    </button>
  );
}

export function IconButton({ icon: Icon, className, title, ...props }) {
  return (
    <button title={title} aria-label={title} className={cn('inline-flex h-9 w-9 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800', className)} {...props}>
      <Icon className="h-4 w-4" />
    </button>
  );
}

export function Field({ label, children, hint, className }) {
  return (
    <label className={cn('block', className)}>
      {label && <span className="label">{label}</span>}
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-400">{hint}</span>}
    </label>
  );
}

export const Input = ({ className, ...p }) => <input className={cn('input', className)} {...p} />;
export const Textarea = ({ className, ...p }) => <textarea className={cn('input min-h-[90px] resize-y', className)} {...p} />;
export const Select = ({ className, children, ...p }) => (
  <select className={cn('input appearance-none bg-[url("data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2712%27 height=%2712%27 viewBox=%270 0 24 24%27 fill=%27none%27 stroke=%27%2394a3b8%27 stroke-width=%272.5%27%3E%3Cpath d=%27m6 9 6 6 6-6%27/%3E%3C/svg%3E")] bg-[length:12px] bg-[right_12px_center] bg-no-repeat pr-8', className)} {...p}>
    {children}
  </select>
);

export function Toggle({ checked, onChange, label }) {
  return (
    <button type="button" onClick={() => onChange(!checked)} className="inline-flex items-center gap-2" role="switch" aria-checked={checked}>
      <span className={cn('relative h-6 w-11 rounded-full transition', checked ? 'bg-brand-600' : 'bg-slate-300')}>
        <span className={cn('absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition', checked ? 'left-[22px]' : 'left-0.5')} />
      </span>
      {label && <span className="text-sm text-slate-700">{label}</span>}
    </button>
  );
}

export function Card({ className, children, ...p }) {
  return <div className={cn('card', className)} {...p}>{children}</div>;
}

export function Badge({ className, children }) {
  return <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', className)}>{children}</span>;
}

export function Modal({ open, onClose, title, children, footer, size = 'md' }) {
  useEffect(() => {
    if (!open) return;
    const esc = (e) => e.key === 'Escape' && onClose?.();
    document.addEventListener('keydown', esc);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', esc);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);
  if (!open) return null;
  const largura = { sm: 'sm:max-w-sm', md: 'sm:max-w-lg', lg: 'sm:max-w-2xl', xl: 'sm:max-w-4xl' }[size];
  return (
    <div className="animate-fade fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 backdrop-blur-sm sm:items-center sm:p-4" onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
      <div className={cn('animate-pop flex max-h-[92vh] w-full flex-col rounded-t-3xl bg-white shadow-2xl sm:rounded-3xl', largura)}>
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h3 className="text-base font-bold text-slate-900">{title}</h3>
          <IconButton icon={X} title="Fechar" onClick={onClose} />
        </div>
        <div className="flex-1 overflow-y-auto px-5 py-4">{children}</div>
        {footer && <div className="safe-bottom flex flex-wrap justify-end gap-2 border-t border-slate-100 px-5 py-3">{footer}</div>}
      </div>
    </div>
  );
}

export function PageHeader({ title, subtitle, actions, icon: Icon }) {
  return (
    <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-3">
        {Icon && (
          <div className="hidden h-11 w-11 items-center justify-center rounded-2xl bg-brand-100 text-brand-700 sm:flex">
            <Icon className="h-5 w-5" />
          </div>
        )}
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 sm:text-2xl">{title}</h1>
          {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function StatCard({ label, value, icon: Icon, tone = 'brand', hint }) {
  const tons = {
    brand: 'bg-brand-100 text-brand-700', green: 'bg-emerald-100 text-emerald-700', red: 'bg-red-100 text-red-600',
    amber: 'bg-amber-100 text-amber-700', blue: 'bg-sky-100 text-sky-700', slate: 'bg-slate-100 text-slate-600',
  };
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
          <p className="mt-1 truncate text-xl font-extrabold text-slate-900 sm:text-2xl">{value}</p>
          {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
        </div>
        {Icon && <div className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-xl', tons[tone])}><Icon className="h-5 w-5" /></div>}
      </div>
    </Card>
  );
}

export function Empty({ icon: Icon, title, children, action }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center">
      {Icon && <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-500"><Icon className="h-7 w-7" /></div>}
      <p className="font-bold text-slate-800">{title}</p>
      {children && <p className="mt-1 max-w-sm text-sm text-slate-500">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Spinner({ className }) {
  return (
    <div className={cn('flex items-center justify-center py-16', className)}>
      <Loader2 className="h-7 w-7 animate-spin text-brand-500" />
    </div>
  );
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="scrollbar-none flex gap-1 overflow-x-auto rounded-xl bg-slate-100 p-1">
      {tabs.map((t) => (
        <button key={t.value} onClick={() => onChange(t.value)}
          className={cn('whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-semibold transition', value === t.value ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700')}>
          {t.label}{t.count != null && <span className="ml-1.5 text-xs opacity-70">{t.count}</span>}
        </button>
      ))}
    </div>
  );
}
