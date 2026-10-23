-- Xplay — banco de dados do painel (Supabase).
-- Cole tudo no Supabase: SQL Editor → New query → Run.
-- O app só usa as funções app_* (com a chave "anon"); as tabelas ficam fechadas para ele.
-- O painel admin entra com e-mail e senha (Supabase Auth) e tem acesso total.

create extension if not exists pgcrypto;

-- Clientes: cada um tem um M3U e um código de acesso de 9 dígitos.
create table if not exists clients (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  code        char(9) not null unique,
  m3u         text not null,
  device_user text,
  expires_at  timestamptz not null default now(),
  active      boolean not null default true,
  notes       text,
  created_at  timestamptz not null default now()
);

-- Recargas: cada uma soma dias à validade do cliente.
create table if not exists recharges (
  id         uuid primary key default gen_random_uuid(),
  client_id  uuid not null references clients(id) on delete cascade,
  days       int not null check (days > 0),
  note       text,
  created_at timestamptz not null default now()
);

-- Aparelhos: cada app instalado tem um usuário de 3 letras + 3 números (ex.: ABC123).
create table if not exists devices (
  user_code  char(6) primary key,
  client_id  uuid references clients(id) on delete set null,
  model      text,
  created_at timestamptz not null default now(),
  last_seen  timestamptz not null default now()
);

-- Configurações gerais (ex.: tmdb_key, a chave do TMDB usada por todos os apps).
create table if not exists config (
  key   text primary key,
  value text
);

alter table clients   enable row level security;
alter table recharges enable row level security;
alter table devices   enable row level security;
alter table config    enable row level security;

-- Só o admin (usuário logado no painel) mexe nas tabelas.
drop policy if exists admin_all on clients;
create policy admin_all on clients   for all to authenticated using (true) with check (true);
drop policy if exists admin_all on recharges;
create policy admin_all on recharges for all to authenticated using (true) with check (true);
drop policy if exists admin_all on devices;
create policy admin_all on devices   for all to authenticated using (true) with check (true);
drop policy if exists admin_all on config;
create policy admin_all on config    for all to authenticated using (true) with check (true);

-- Gera um código de 9 dígitos que ainda não existe.
create or replace function new_client_code() returns char(9)
language plpgsql as $$
declare c char(9);
begin
  loop
    c := lpad((floor(random() * 1000000000))::bigint::text, 9, '0');
    exit when c not like '0%' and not exists (select 1 from clients where code = c);
  end loop;
  return c;
end $$;

alter table clients alter column code set default new_client_code();

-- Uma recarga soma dias a partir de hoje ou da validade atual (o que for maior).
create or replace function apply_recharge() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  update clients
     set expires_at = greatest(expires_at, now()) + make_interval(days => new.days)
   where id = new.client_id;
  return new;
end $$;

drop trigger if exists recharge_applied on recharges;
create trigger recharge_applied after insert on recharges
  for each row execute function apply_recharge();

-- O app registra o usuário do aparelho na primeira vez que abre.
create or replace function app_register_device(p_user text, p_model text default null)
returns json language plpgsql security definer set search_path = public as $$
begin
  if p_user !~ '^[A-Z]{3}[0-9]{3}$' then
    return json_build_object('ok', false, 'error', 'Usuário inválido');
  end if;
  insert into devices(user_code, model) values (p_user, p_model)
  on conflict (user_code) do update set last_seen = now(), model = coalesce(excluded.model, devices.model);
  return json_build_object('ok', true);
end $$;

-- Login do cliente com o código de 9 dígitos.
create or replace function app_login(p_code text, p_user text)
returns json language plpgsql security definer set search_path = public as $$
declare c clients;
begin
  select * into c from clients where code = p_code;
  if not found then
    return json_build_object('ok', false, 'error', 'Código não encontrado');
  end if;
  if not c.active then
    return json_build_object('ok', false, 'error', 'Acesso bloqueado. Fale com o seu revendedor.');
  end if;
  if c.expires_at < now() then
    return json_build_object('ok', false, 'error', 'Sua recarga venceu em '
      || to_char(c.expires_at at time zone 'America/Sao_Paulo', 'DD/MM/YYYY') || '. Faça uma nova recarga.');
  end if;
  if p_user ~ '^[A-Z]{3}[0-9]{3}$' then
    update clients set device_user = p_user where id = c.id;
    insert into devices(user_code, client_id) values (p_user, c.id)
    on conflict (user_code) do update set client_id = c.id, last_seen = now();
  end if;
  return json_build_object(
    'ok', true,
    'name', c.name,
    'm3u', c.m3u,
    'expires_at', c.expires_at,
    'tmdb_key', (select value from config where key = 'tmdb_key'));
end $$;

grant execute on function app_register_device(text, text) to anon;
grant execute on function app_login(text, text) to anon;
revoke all on clients, recharges, devices, config from anon;
