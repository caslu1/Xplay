-- Gestor Simples — armazenamento na nuvem (Supabase), no mesmo projeto do painel Xplay.
-- Cole tudo em: SQL Editor → New query → Run. Pode rodar de novo sem problema.
-- Depois rode também supabase/gestor-isolamento.sql (veja docs/GESTOR.md).

-- Uma linha por conta, com todos os dados do app (clientes, planos, pagamentos, histórico, configurações).
create table if not exists gestor_data (
  user_id    uuid primary key references auth.users(id) on delete cascade default auth.uid(),
  data       jsonb not null,
  updated_at timestamptz not null default now()
);

alter table gestor_data enable row level security;

-- Cada usuário só lê e grava a própria linha.
drop policy if exists own_row_select on gestor_data;
create policy own_row_select on gestor_data for select to authenticated using (user_id = auth.uid());
drop policy if exists own_row_insert on gestor_data;
create policy own_row_insert on gestor_data for insert to authenticated with check (user_id = auth.uid());
drop policy if exists own_row_update on gestor_data;
create policy own_row_update on gestor_data for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists own_row_delete on gestor_data;
create policy own_row_delete on gestor_data for delete to authenticated using (user_id = auth.uid());

-- A data de atualização é sempre a do servidor (usada para saber qual cópia é mais nova).
create or replace function gestor_touch() returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;
drop trigger if exists gestor_touch on gestor_data;
create trigger gestor_touch before insert or update on gestor_data for each row execute function gestor_touch();
