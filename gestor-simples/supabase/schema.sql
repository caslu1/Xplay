-- GESTOR SIMPLES — banco de dados (Supabase / Postgres)
-- Cole tudo no Supabase: SQL Editor → New query → Run.
-- Cada usuário vê e edita só os próprios registros (created_by = usuário logado);
-- quem tiver role = 'admin' em profiles tem acesso total.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- Perfil do usuário (extensão do User do Supabase Auth)
-- ---------------------------------------------------------------------------
create table if not exists profiles (
  id                         uuid primary key references auth.users(id) on delete cascade,
  email                      text,
  full_name                  text,
  empresa                    text,
  role                       text not null default 'user' check (role in ('admin', 'user')),
  onboarding_completed       boolean not null default false,
  profile_photo              text,
  dashboard_settings         jsonb not null default '{"notificar_vencimentos": true, "dias_antecedencia": 3}',
  whatsapp_message           text,
  whatsapp_cadastro_message  text,
  whatsapp_expirado_message  text,
  pin_lock_enabled           boolean not null default false,
  pin_lock_code              text,
  activity_logs              jsonb not null default '[]',
  created_at                 timestamptz not null default now(),
  updated_at                 timestamptz not null default now()
);

-- Cria o perfil automaticamente quando alguém se cadastra.
create or replace function handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into profiles (id, email, full_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)))
  on conflict (id) do nothing;
  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function handle_new_user();

create or replace function is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from profiles where id = auth.uid() and role = 'admin');
$$;

-- ---------------------------------------------------------------------------
-- Entidades
-- ---------------------------------------------------------------------------
create table if not exists planos (
  id          uuid primary key default gen_random_uuid(),
  codigo      char(4) check (codigo ~ '^[0-9]{4}$'),
  nome        text not null,
  preco       numeric(12,2) not null default 0,
  custo       numeric(12,2) not null default 0,
  periodo     text not null default 'mensal' check (periodo in ('mensal', 'trimestral', 'semestral', 'anual')),
  link        text,
  descricao   text,
  cor         text default '#ddd6fe',
  status      text not null default 'ativo' check (status in ('ativo', 'inativo')),
  created_by  uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date timestamptz not null default now(),
  updated_date timestamptz not null default now()
);

create table if not exists clientes (
  id              uuid primary key default gen_random_uuid(),
  nome            text not null,
  whatsapp        text not null,
  plano_id        uuid not null,
  plano_nome      text,
  data_vencimento date not null,
  quantidade      int not null default 1 check (quantidade between 1 and 10),
  desconto        numeric(12,2) not null default 0,
  notas           text,
  status          text not null default 'ativo' check (status in ('ativo', 'inativo', 'pendente', 'arquivado')),
  essencial       boolean not null default false,
  created_by      uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date    timestamptz not null default now(),
  updated_date    timestamptz not null default now()
);

create table if not exists mensalidades (
  id               uuid primary key default gen_random_uuid(),
  cliente_id       uuid,
  cliente_nome     text,
  plano_id         uuid,
  plano_nome       text,
  valor            numeric(12,2) not null default 0,
  custo            numeric(12,2) not null default 0,
  data_vencimento  date,
  data_pagamento   date,
  status           text not null default 'pendente' check (status in ('pendente', 'pago', 'vencido', 'cancelado')),
  descricao        text,
  metodo_pagamento text check (metodo_pagamento in ('dinheiro', 'pix', 'cartao', 'transferencia', 'renovacao', 'cadastro')),
  created_by       uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date     timestamptz not null default now(),
  updated_date     timestamptz not null default now()
);

create table if not exists configuracoes_mensagem (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null,
  tipo          text not null check (tipo in ('vencimento_proximo', 'vencimento_hoje', 'atraso', 'renovacao', 'boas_vindas', 'plano_expirado')),
  mensagem      text not null,
  dias_antes    int not null default 0,
  dias_apos     int not null default 0,
  horario_envio text not null default '09:00',
  ativo         boolean not null default true,
  created_by    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date  timestamptz not null default now(),
  updated_date  timestamptz not null default now()
);

create table if not exists historico_mensagens (
  id            uuid primary key default gen_random_uuid(),
  cliente_id    uuid,
  cliente_nome  text,
  whatsapp      text,
  tipo_mensagem text,
  mensagem      text,
  status        text not null default 'pendente' check (status in ('enviada', 'erro', 'pendente')),
  erro          text,
  data_envio    timestamptz not null default now(),
  created_by    uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date  timestamptz not null default now(),
  updated_date  timestamptz not null default now()
);

create table if not exists notas_adesivas (
  id           uuid primary key default gen_random_uuid(),
  titulo       text,
  conteudo     text,
  cor          text not null default 'amarelo' check (cor in ('amarelo', 'verde', 'azul', 'rosa', 'roxo', 'laranja')),
  fixada       boolean not null default false,
  created_by   uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date timestamptz not null default now(),
  updated_date timestamptz not null default now()
);

create table if not exists tarefas (
  id           uuid primary key default gen_random_uuid(),
  titulo       text not null,
  descricao    text,
  data_prazo   date,
  prioridade   text not null default 'media' check (prioridade in ('baixa', 'media', 'alta')),
  status       text not null default 'pendente' check (status in ('pendente', 'em_andamento', 'concluida')),
  categoria    text not null default 'outros' check (categoria in ('cobranca', 'atendimento', 'administrativo', 'outros')),
  created_by   uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date timestamptz not null default now(),
  updated_date timestamptz not null default now()
);

create table if not exists metas (
  id           uuid primary key default gen_random_uuid(),
  mes          char(7) not null,               -- AAAA-MM
  tipo         text not null check (tipo in ('receita', 'lucro', 'clientes_novos', 'renovacoes')),
  valor_alvo   numeric(12,2) not null default 0,
  descricao    text,
  created_by   uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_date timestamptz not null default now(),
  updated_date timestamptz not null default now()
);

create index if not exists clientes_owner_idx     on clientes (created_by, data_vencimento);
create index if not exists mensalidades_owner_idx on mensalidades (created_by, data_pagamento);
create index if not exists historico_owner_idx    on historico_mensagens (created_by, data_envio);

-- updated_date automático
create or replace function touch_updated() returns trigger language plpgsql as $$
begin new.updated_date = now(); return new; end $$;

do $$
declare t text;
begin
  foreach t in array array['planos','clientes','mensalidades','configuracoes_mensagem','historico_mensagens','notas_adesivas','tarefas','metas'] loop
    execute format('drop trigger if exists touch_%1$s on %1$s', t);
    execute format('create trigger touch_%1$s before update on %1$s for each row execute function touch_updated()', t);
  end loop;
end $$;

-- ---------------------------------------------------------------------------
-- RLS: cada um só enxerga o que criou; admin vê tudo
-- ---------------------------------------------------------------------------
alter table profiles enable row level security;
drop policy if exists profiles_self on profiles;
create policy profiles_self on profiles for all
  using (id = auth.uid() or is_admin())
  with check (id = auth.uid() or is_admin());

-- Ninguém promove a si mesmo a admin pelo app.
create or replace function protect_role() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.role is distinct from old.role and not is_admin() then
    new.role = old.role;
  end if;
  new.updated_at = now();
  return new;
end $$;
drop trigger if exists protect_role on profiles;
create trigger protect_role before update on profiles for each row execute function protect_role();

do $$
declare t text;
begin
  foreach t in array array['planos','clientes','mensalidades','configuracoes_mensagem','historico_mensagens','notas_adesivas','tarefas','metas'] loop
    execute format('alter table %I enable row level security', t);
    execute format('drop policy if exists owner_all on %I', t);
    execute format($p$create policy owner_all on %I for all
                     using (created_by = auth.uid() or is_admin())
                     with check (created_by = auth.uid() or is_admin())$p$, t);
  end loop;
end $$;

-- ---------------------------------------------------------------------------
-- Mensagens automáticas: roda a função verificarEnvioMensagens de hora em hora.
-- Antes, troque <PROJETO> pelo ref do projeto e <CRON_SECRET> pelo mesmo valor
-- configurado no secret CRON_SECRET das Edge Functions. Depois rode este bloco.
-- ---------------------------------------------------------------------------
-- create extension if not exists pg_cron;
-- create extension if not exists pg_net;
-- select cron.schedule('gestor-mensagens-automaticas', '0 * * * *', $$
--   select net.http_post(
--     url     := 'https://<PROJETO>.supabase.co/functions/v1/verificarEnvioMensagens',
--     headers := '{"Content-Type": "application/json", "x-cron-secret": "<CRON_SECRET>"}'::jsonb,
--     body    := '{}'::jsonb
--   );
-- $$);
