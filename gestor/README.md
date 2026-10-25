# Xplay Gestor

App Android para revendedores gerenciarem clientes, vencimentos e faturamento.

- **Dashboard**: clientes ativos, vencidos, vencem hoje, próximos 5 dias, essenciais e próximos vencimentos.
- **Clientes**: busca, filtros (status, período do plano, plano), WhatsApp, essencial, editar, renovar, excluir, histórico de pagamentos.
- **Financeiro**: receita, lucro, ticket médio por período, relatório em PDF, gráficos de lucro, evolução, receita por plano, clientes ativos, clientes por dia, renovações, novos clientes, inadimplentes e clientes por estado (pelo DDD).
- **Ferramentas**: planos (valor e custo), mensagem de cobrança, cobrança do dia, backup/restauração em JSON, importação de CSV, perfil e dados de exemplo.
- Histórico de ações com **Desfazer**.

Os dados ficam salvos no aparelho (sem servidor). Faça backup em Ferramentas.

## Build

```sh
./build.sh   # gera build/XplayGestor.apk, assinado com ../tools/xplay.keystore
```

A interface fica em `assets/` (HTML/CSS/JS) e pode ser testada abrindo `assets/index.html` no navegador.
