-- Gestor Simples — visões para enxergar os dados do app como tabelas no Supabase.
-- O app guarda tudo de cada conta em gestor_data.data (um JSON). Estas visões só leem
-- esse JSON e mostram uma linha por cliente, plano e pagamento. Não mudam nada no app.
-- Rode depois de gestor.sql: SQL Editor → New query → Run.
-- security_invoker: quem consulta pelo app (anon/usuário) continua vendo só a própria conta.

create or replace view gestor_clientes with (security_invoker = on) as
select g.user_id,
       u.email                                   as conta,
       c->>'id'                                  as id,
       c->>'name'                                as nome,
       c->>'phone'                               as whatsapp,
       p->>'name'                                as plano,
       nullif(c->>'due', '')::date               as vencimento,
       (c->>'qty')::int                          as quantidade,
       (c->>'discount')::numeric                 as desconto,
       coalesce((c->>'essential')::boolean, false) as essencial,
       coalesce((c->>'archived')::boolean, false)  as arquivado,
       c->>'notes'                               as notas,
       nullif(c->>'createdAt', '')::date         as cadastrado_em
  from gestor_data g
  join auth.users u on u.id = g.user_id
  cross join lateral jsonb_array_elements(g.data->'clients') c
  left join lateral (select x from jsonb_array_elements(g.data->'plans') x where x->>'id' = c->>'planId' limit 1) pl(p) on true;

create or replace view gestor_planos with (security_invoker = on) as
select g.user_id,
       u.email                  as conta,
       p->>'id'                 as id,
       p->>'code'               as codigo,
       p->>'name'               as nome,
       p->>'type'               as periodo,
       (p->>'price')::numeric   as preco,
       (p->>'cost')::numeric    as custo
  from gestor_data g
  join auth.users u on u.id = g.user_id
  cross join lateral jsonb_array_elements(g.data->'plans') p;

create or replace view gestor_pagamentos with (security_invoker = on) as
select g.user_id,
       u.email                                as conta,
       x->>'id'                               as id,
       x->>'clientName'                       as cliente,
       x->>'planName'                         as plano,
       (x->>'amount')::numeric                as valor,
       (x->>'cost')::numeric                  as custo,
       (x->>'amount')::numeric - coalesce((x->>'cost')::numeric, 0) as lucro,
       x->>'kind'                             as tipo,
       (left(x->>'date', 10))::date           as data
  from gestor_data g
  join auth.users u on u.id = g.user_id
  cross join lateral jsonb_array_elements(g.data->'payments') x;

-- As visões leem auth.users (e-mail); só o painel do Supabase (admin) precisa delas.
revoke all on gestor_clientes, gestor_planos, gestor_pagamentos from anon, authenticated;
