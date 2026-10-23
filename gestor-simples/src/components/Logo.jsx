export default function Logo({ light }) {
  return (
    <div className="flex items-center gap-2.5">
      <img src="/icon.svg" alt="" className="h-9 w-9" />
      <div className="leading-none">
        <p className={light ? 'text-base font-black tracking-tight text-white' : 'text-base font-black tracking-tight text-slate-900'}>GESTOR</p>
        <p className={light ? 'text-[11px] font-bold tracking-[0.25em] text-brand-200' : 'text-[11px] font-bold tracking-[0.25em] text-brand-600'}>SIMPLES</p>
      </div>
    </div>
  );
}
