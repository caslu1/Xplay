# Painel Xplay (Supabase)

O painel controla quem entra no app:

- **Cliente**: você cadastra o nome e o link M3U da pessoa. O painel gera um **código de 9 dígitos**, e é com ele que o cliente entra no app.
- **Recarga**: cada recarga soma dias à validade do cliente (30, 60, 90 dias…). Quando a validade vence, o app bloqueia o login e mostra "Sua recarga venceu em DD/MM/AAAA".
- **Usuário do aparelho**: cada app instalado cria um usuário de 3 letras e 3 números (ex.: `KTR482`). Ele aparece na tela de login e em Configurações → Conta. No painel, você vincula esse usuário ao cliente (o vínculo também é feito sozinho quando o cliente entra com o código).
- **Chave do TMDB**: você coloca uma vez no painel e todos os apps passam a usar. Não existe mais essa opção dentro do app.

## 1. Criar o projeto (uma vez)

1. Crie uma conta grátis em <https://supabase.com> e clique em **New project**.
2. Abra **SQL Editor → New query**, cole todo o conteúdo de [`supabase/schema.sql`](../supabase/schema.sql) e clique em **Run**.
3. Abra **Authentication → Users → Add user → Create new user**. Informe o seu e-mail e uma senha. Esse é o login do painel.
4. Em **Authentication → Sign In / Providers**, desative **Allow new users to sign up**. Assim ninguém mais consegue criar uma conta de admin.
5. Abra **Project Settings → API** e copie a **Project URL** e a chave **anon public**.

## 2. Ligar o app ao painel

Envie a **Project URL** e a chave **anon public** para gerar a versão do app ligada ao seu painel. Elas ficam em `app/src/com/xplay/iptv/Backend.java` (`BASE` e `ANON_KEY`).

Enquanto o app não estiver ligado, a tela de login continua com o formulário antigo (Xtream ou M3U).

A chave anon é pública: o app só consegue chamar as funções de login. As tabelas dos clientes ficam fechadas para quem não entrou no painel.

## 3. Abrir o painel

O painel é o arquivo [`admin/index.html`](../admin/index.html). Escolha um jeito de abrir:

- **No computador**: baixe o arquivo e abra no Chrome.
- **Online, de qualquer lugar**: entre em <https://app.netlify.com/drop> e arraste a pasta `admin`. A Netlify devolve um link para o painel.

Na primeira vez, o painel pede a Project URL e a chave anon. Depois disso, entre com o e-mail e a senha do passo 1.3.

## 4. Dia a dia

1. **Clientes → Novo cliente**: informe o nome, o link M3U e os dias da primeira recarga. O painel mostra o código de 9 dígitos para você enviar ao cliente.
2. O cliente abre o app, digita o código e entra.
3. **Recarga**: clique em *Recarga* na linha do cliente e escolha os dias.
4. **Bloquear**: corta o acesso na hora, mesmo que a validade não tenha vencido. O app confere o código sempre que abre.
5. **Editar → Gerar novo código**: troca o código, se o antigo tiver vazado.
6. **Aparelhos**: lista todos os apps que já abriram, com o modelo e o último acesso. Ali você escolhe a qual cliente cada usuário pertence.
7. **Configurações**: cole a chave do TMDB (grátis em themoviedb.org → Configurações → API).
