-- Separa as contas do painel Xplay das contas do Gestor Simples.
-- Sem isto, QUALQUER usuário logado (inclusive uma conta do Gestor) consegue ler e alterar
-- os clientes, recargas, aparelhos e configurações do Xplay, porque as regras do schema.sql
-- liberam essas tabelas para todo usuário autenticado.
--
-- Não precisa editar nada: rode ANTES de criar as contas do Gestor.
-- Todas as contas que já existem no projeto (as do painel admin) viram administradoras do painel.
-- Contas que já usam o Gestor (têm dados em gestor_data) ficam de fora.
-- Cole tudo em: SQL Editor → New query → Run.

create table if not exists panel_admins (
  user_id uuid primary key references auth.users(id) on delete cascade
);
alter table panel_admins enable row level security;

insert into panel_admins (user_id)
  select u.id from auth.users u
   where not exists (select 1 from gestor_data g where g.user_id = u.id)
  on conflict do nothing;

-- Roda com permissão do dono, para a regra conseguir consultar panel_admins.
create or replace function is_panel_admin() returns boolean
  language sql stable security definer set search_path = public as $$
  select exists (select 1 from panel_admins where user_id = auth.uid());
$$;

drop policy if exists admin_all on clients;
create policy admin_all on clients   for all to authenticated using (is_panel_admin()) with check (is_panel_admin());
drop policy if exists admin_all on recharges;
create policy admin_all on recharges for all to authenticated using (is_panel_admin()) with check (is_panel_admin());
drop policy if exists admin_all on devices;
create policy admin_all on devices   for all to authenticated using (is_panel_admin()) with check (is_panel_admin());
drop policy if exists admin_all on config;
create policy admin_all on config    for all to authenticated using (is_panel_admin()) with check (is_panel_admin());

-- Conferência: deve mostrar o e-mail do painel admin.
select u.email from panel_admins a join auth.users u on u.id = a.user_id;
