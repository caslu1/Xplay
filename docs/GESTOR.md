# Gestor Simples — como ligar o login e a nuvem

Tempo: uns 10 minutos, uma vez só. Tudo é feito no site do Supabase, no **mesmo projeto do painel Xplay**. Faça pelo computador, que é mais fácil copiar e colar.

Você vai precisar de:
- o login do Supabase (o mesmo de quando criou o painel Xplay);
- o **e-mail que você usa para entrar no painel admin do Xplay**;
- um e-mail e uma senha para a sua conta do Gestor. Pode ser o mesmo e-mail do painel ou outro.

---

## Passo 1 — Criar a tabela do Gestor

1. Entre em <https://supabase.com/dashboard> e abra o projeto do **Xplay**.
2. Abra direto: <https://supabase.com/dashboard/project/xgubfucijtaazvuzcluw/sql/new> (ou menu **SQL Editor** → **+ New query**).
4. Abra o arquivo [`supabase/gestor.sql`](../supabase/gestor.sql), copie **todo** o conteúdo e cole no editor.
5. Clique em **Run**, no canto inferior direito (ou aperte Ctrl+Enter).

✅ **Deu certo se** aparecer embaixo: `Success. No rows returned`.

---

## Passo 2 — Proteger os clientes do Xplay (não pule)

Hoje, qualquer pessoa logada no Supabase consegue ver os clientes do Xplay. Este passo deixa esses dados só para as contas que já existem (as do painel admin). **Faça antes do Passo 3.**

1. Abra <https://supabase.com/dashboard/project/xgubfucijtaazvuzcluw/sql/new>.
2. Copie todo o conteúdo de [`supabase/gestor-isolamento.sql`](../supabase/gestor-isolamento.sql) e cole. Não precisa mudar nada.
3. Clique em **Run**.

✅ **Deu certo se** aparecer embaixo uma tabela com o **e-mail do painel admin**. Depois abra o painel admin e confira se os clientes continuam aparecendo.

---

## Passo 3 — Criar a sua conta do Gestor

1. Abra <https://supabase.com/dashboard/project/xgubfucijtaazvuzcluw/auth/users> (ou menu **Authentication** → **Users**).
2. Clique em **Add user** → **Create new user**.
3. Preencha o **e-mail** e uma **senha** (mínimo 6 caracteres).
4. Deixe marcado **Auto Confirm User**.
5. Clique em **Create user**.

✅ **Deu certo se** o e-mail aparecer na lista de usuários.

> Repita este passo para cada pessoa que for usar o Gestor. Cada conta tem os próprios clientes e não enxerga os das outras.

---

## Passo 4 — Entrar no app

1. Instale o APK do Gestor Simples (por cima da versão anterior, sem desinstalar).
2. Abra o app. Vai aparecer a tela **Gestor Simples** com **Entrar** e **Criar conta**.
3. Em **Entrar**, digite o e-mail e a senha do passo 3 e toque em **Entrar**.

✅ **Deu certo se** o app abrir no Dashboard e aparecer um **ponto verde** no canto do logo, no topo.

- **Se o celular já tinha clientes:** eles vão para a sua conta automaticamente na primeira vez que você entra.
- **Num celular novo:** entre com a mesma conta e os clientes são baixados.

---

## No dia a dia

| O que você vê | O que significa |
|---|---|
| 🟢 ponto verde no logo | Tudo salvo na nuvem |
| 🟡 ponto amarelo piscando | Enviando as alterações |
| ⚪ ponto cinza | Sem internet. Está tudo salvo no celular e será enviado quando a internet voltar |
| 🔴 ponto vermelho | Erro ao sincronizar. Veja o motivo em Ferramentas → Perfil |

- Você não precisa salvar nada: cada alteração é enviada sozinha alguns segundos depois.
- Em **Ferramentas → Perfil → Conta e nuvem** ficam a sua conta, a última sincronização e os botões **Sincronizar** e **Sair**.
- Se usar a mesma conta em **dois celulares sem internet** e mexer nos dois, o app pergunta qual cópia manter. A cópia que você não escolher perde as alterações que só ela tinha.

---

## Problemas comuns

| Mensagem no app | Como resolver |
|---|---|
| **E-mail ou senha incorretos** | Confira os dados. Se esqueceu a senha, veja **Trocar a senha** abaixo. Não apague a conta: os clientes dela são apagados junto. |
| **Confirme seu e-mail antes de entrar** | A conta foi criada sem **Auto Confirm User**. Em Authentication → Users, clique nos **⋯** da conta → **Confirm email**. |
| **Novas contas estão desativadas neste servidor** | Normal: o botão "Criar conta" do app fica desligado por segurança. Crie as contas pelo Passo 3. |
| **Erro: relation "gestor_data" does not exist** | O Passo 1 não foi feito no projeto certo. Rode `gestor.sql` no projeto do Xplay. |
| **Sem conexão com o servidor** | O celular está sem internet. Pode continuar usando: os dados ficam no celular e são enviados depois. |
| O painel admin parou de mostrar clientes | O Passo 2 foi feito depois de entrar no app com o e-mail do painel. Me avise que eu mando o comando para liberar esse e-mail. |

---

## Trocar a senha de uma conta

O link de "Esqueci minha senha" do app ainda não tem página para abrir, então troque pelo Supabase:

1. **SQL Editor → + New query**, cole o texto abaixo trocando o e-mail e a nova senha:
   ```sql
   update auth.users
      set encrypted_password = crypt('NOVA-SENHA', gen_salt('bf'))
    where lower(email) = lower('voce@gmail.com');
   ```
2. Clique em **Run**. ✅ Deve aparecer `Success. 1 row affected`.
3. No app, entre com a senha nova.

---

## Opcional — deixar o "Criar conta" do app funcionando

Só faça isso **depois do Passo 2**. Sem ele, quem criar uma conta consegue ver os clientes do Xplay.

1. **Authentication → Sign In / Providers**.
2. Ative **Allow new users to sign up**.
3. Se **Confirm email** estiver ligado, a pessoa precisa clicar no link que chega no e-mail antes de entrar. Desligue essa opção se preferir que ela entre direto.

---

## Ver os dados no Supabase

O app guarda tudo de cada conta numa linha só da tabela `gestor_data` (coluna `data`). Para ver clientes, planos e pagamentos como tabelas:

1. Abra <https://supabase.com/dashboard/project/xgubfucijtaazvuzcluw/sql/new>, cole [`supabase/gestor-visoes.sql`](../supabase/gestor-visoes.sql) e clique em **Run**.
2. Em **Table Editor**, aparecem `gestor_clientes`, `gestor_planos` e `gestor_pagamentos`, uma linha por item, com a coluna `conta` (e-mail).

São só para consulta: alterações devem ser feitas pelo app.
