# Gestor Simples

Gestão de clientes e mensalidades (planos recorrentes) com cobrança pelo WhatsApp.
**Site e app de celular no mesmo código:** é um PWA, então no celular basta abrir o site e
tocar em “Instalar app” / “Adicionar à tela inicial”. Ele abre em tela cheia, com ícone próprio, e funciona offline.

- Front: React 18 + Vite + Tailwind + Recharts
- Backend: Supabase (Postgres com RLS, Auth, Edge Functions e pg_cron)
- WhatsApp: W-API · IA: Claude (Anthropic)

## Testar agora (modo demonstração)

```sh
cd gestor-simples
npm install
npm run dev        # http://localhost:5173
```

Sem o arquivo `.env`, o app abre em **modo demonstração**: entra sem senha, já vem com dados de exemplo
e guarda tudo no navegador. Os envios de WhatsApp só ficam registrados no histórico, e a IA usa textos prontos.

## Colocar no ar com dados reais

### 1. Supabase
1. Crie um projeto em [supabase.com](https://supabase.com).
2. **SQL Editor → New query**: cole `supabase/schema.sql` e rode. Isso cria as tabelas, o RLS (cada usuário só vê os próprios dados; `role = 'admin'` vê tudo) e o perfil automático no cadastro.
3. Publique as funções (precisa do [Supabase CLI](https://supabase.com/docs/guides/cli)):
   ```sh
   supabase login
   supabase link --project-ref SEU_PROJETO
   supabase functions deploy enviarMensagemWhatsApp
   supabase functions deploy verificarEnvioMensagens
   supabase functions deploy invokeLLM
   ```
4. Configure os segredos:
   ```sh
   supabase secrets set WAPI_INSTANCE_ID=... WAPI_TOKEN=... ANTHROPIC_API_KEY=... CRON_SECRET=uma-senha-longa
   # opcional, se a sua W-API usar outro endereço:
   supabase secrets set WAPI_BASE_URL=https://waapi.app
   ```
5. Mensagens automáticas de hora em hora: no fim do `schema.sql` há um bloco comentado com `cron.schedule`.
   Troque `<PROJETO>` e `<CRON_SECRET>`, descomente e rode no SQL Editor.

### 2. Front-end
```sh
cp .env.example .env     # preencha VITE_SUPABASE_URL e VITE_SUPABASE_ANON_KEY
npm run build            # gera dist/
```
Publique a pasta `dist/` em qualquer hospedagem estática (Vercel, Netlify, Cloudflare Pages). Os arquivos
`vercel.json` e `public/_redirects` já mandam todas as rotas para o `index.html`.
Para usar `gestor-simples.base44.app` ou outro domínio, aponte o domínio para essa hospedagem.

## Estrutura

| Caminho | O que é |
|---|---|
| `src/pages/` | As telas: LandingPage, Dashboard, Clientes, Planos, Faturamento, Metas, NotasAdesivas, MensagensAutomaticas, Tarefas, Configuracoes, ImportarClientes, BackupDados |
| `src/components/` | Layout (sidebar no desktop, header roxo + barra inferior no celular), PIN, onboarding, notificações, histórico com desfazer, modais |
| `src/lib/db.js` | Entidades com cache de 5 min, invalidação por evento e paginação no servidor (não trunca listas grandes) |
| `src/lib/acoes.js` | Cadastro, edição, renovação, exclusão e arquivamento, com registro financeiro e “Desfazer” |
| `src/lib/profile.js` | Perfil do usuário e histórico de atividades (até 100 entradas, limpeza a cada 3 dias) |
| `supabase/schema.sql` | Banco de dados + RLS |
| `supabase/functions/` | `enviarMensagemWhatsApp`, `verificarEnvioMensagens`, `invokeLLM` e as regras compartilhadas (`_shared/regras.js`, usadas também pelo app) |

## Regras principais

- Valor da mensalidade = preço do plano × conexões − desconto.
- Renovar cria uma Mensalidade paga (método escolhido) e empurra o vencimento um período do plano (mensal, trimestral, semestral ou anual).
- Mensagens automáticas: `vencimento_proximo` (X dias antes), `vencimento_hoje`, `atraso` / `plano_expirado` (X dias depois), `boas_vindas` (cliente criado hoje).
  Rodam no horário configurado (fuso de Brasília) e não repetem o mesmo tipo para o mesmo cliente no mesmo dia.
- Variáveis nos textos: `{nome}` `{plano}` `{valor}` `{data}` `{link}` `{empresa}` e também `{{cliente}}` `{{plano}}` `{{vencimento}}` `{{valor}}`.
