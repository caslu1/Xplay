# Gestor Simples — login e dados na nuvem

O Gestor usa o mesmo projeto Supabase do painel Xplay. Cada conta tem os próprios dados (clientes, planos, pagamentos, histórico e configurações) guardados na tabela `gestor_data`, e o celular mantém uma cópia para funcionar sem internet.

## 1. Preparar o Supabase (uma vez)

1. Abra o projeto do Xplay no Supabase → **SQL Editor → New query**, cole [`supabase/gestor.sql`](../supabase/gestor.sql) e clique em **Run**.
2. **Importante:** abra outra query, cole [`supabase/gestor-isolamento.sql`](../supabase/gestor-isolamento.sql), troque `SEU-EMAIL-DO-PAINEL` pelo e-mail do painel admin e clique em **Run**. O resultado deve mostrar o seu e-mail.
   Sem esse passo, uma conta do Gestor conseguiria ver e alterar os clientes do Xplay.
3. Crie a conta do Gestor:
   - **Jeito recomendado (cadastro fechado):** Authentication → Users → **Add user → Create new user**, com e-mail e senha, marcando **Auto Confirm User**. Deixe "Allow new users to sign up" desativado. No app, use **Entrar**.
   - **Se quiser que o botão "Criar conta" do app funcione:** em Authentication → Sign In / Providers, ative **Allow new users to sign up**. Faça isso só depois do passo 2. Se "Confirm email" estiver ligado, a pessoa precisa clicar no link do e-mail antes de entrar.

## 2. Usar no app

- Ao abrir, o app pede **e-mail e senha**. O login fica salvo; só pede de novo se você tocar em **Sair** (Ferramentas → Perfil).
- Na primeira vez que entrar num celular que já tinha dados, eles são enviados para a sua conta.
- Num celular novo, ao entrar, os dados da conta são baixados.
- Toda alteração é salva no celular na hora e enviada para a nuvem alguns segundos depois. O ponto no logo do topo mostra o estado: **verde** sincronizado, **amarelo** enviando, **cinza** sem internet (envia quando voltar), **vermelho** erro.
- Se dois celulares mexerem na mesma conta sem internet, o app pergunta qual cópia manter.
- Ferramentas → Perfil → **Conta e nuvem**: e-mail da conta, última sincronização, **Sincronizar** e **Sair**.
