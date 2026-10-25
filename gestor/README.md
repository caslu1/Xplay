# Gestor Simples

App Android para revendedores gerenciarem clientes, vencimentos e faturamento.

- **Dashboard**: clientes ativos, vencidos, vencem hoje, próximos 5 dias, essenciais e próximos vencimentos.
- **Clientes**: busca, filtros (status, período do plano, plano), WhatsApp, essencial, editar, renovar, excluir, histórico de pagamentos.
- **Financeiro**: receita, lucro, ticket médio por período, relatório em PDF, gráficos de lucro, evolução, receita por plano, clientes ativos, clientes por dia, renovações, novos clientes, inadimplentes e clientes por estado (pelo DDD).
- **Financeiro segue o período escolhido**: com "Mês" aparece o seletor do mês, e cartões, gráficos e contadores mensais acompanham a seleção. Inclui o comparador de planos vendidos entre dois meses.
- **Configurações**: perfil (foto, empresa), exportar relatórios (PDF/CSV), mensagens do WhatsApp (botão, cadastro, renovação), histórico de atividades.
- **Dados e Backups**: importar clientes por CSV/Excel (.xlsx) com código do plano, backup completo (salva o arquivo onde você escolher) e restauração que adiciona dados — aceita também o backup do Gestor Simples (`gestor_simples_backup_*.json`).
- **Planos**: busca, código de 4 dígitos, cor, link, descrição, lucro e margem, lista de clientes do plano.
- Histórico de ações com **Desfazer**.

Login por e-mail e senha, com os dados de cada conta salvos no Supabase e uma cópia no aparelho para funcionar sem internet. Configuração: [docs/GESTOR.md](../docs/GESTOR.md).

## Build

```sh
./build.sh   # gera build/XplayGestor.apk, assinado com ../tools/xplay.keystore
```

A interface fica em `assets/` (HTML/CSS/JS) e pode ser testada abrindo `assets/index.html` no navegador.
