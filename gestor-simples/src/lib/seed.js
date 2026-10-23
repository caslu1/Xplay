// Dados de exemplo para o modo demonstração.
import { db } from './db';
import { addDias, hojeISO } from './utils';

export async function popularDemo() {
  if (localStorage.getItem('gs_demo_seeded')) return;
  localStorage.setItem('gs_demo_seeded', '1');
  const hoje = hojeISO();
  const planos = await db.Plano.bulkCreate([
    { codigo: '1001', nome: 'Básico', preco: 30, custo: 10, periodo: 'mensal', cor: '#bfdbfe', status: 'ativo', descricao: '1 tela' },
    { codigo: '1002', nome: 'Premium', preco: 50, custo: 18, periodo: 'mensal', cor: '#ddd6fe', status: 'ativo', descricao: 'Telas extras + suporte' },
    { codigo: '2001', nome: 'Trimestral', preco: 80, custo: 30, periodo: 'trimestral', cor: '#bbf7d0', status: 'ativo' },
    { codigo: '3001', nome: 'Anual', preco: 280, custo: 100, periodo: 'anual', cor: '#fde68a', status: 'ativo' },
  ]);
  const nomes = ['Ana Souza', 'Bruno Lima', 'Carla Mendes', 'Diego Rocha', 'Eduarda Alves', 'Felipe Costa', 'Gabriela Nunes', 'Henrique Dias',
    'Isabela Martins', 'João Pereira', 'Karina Lopes', 'Lucas Ribeiro', 'Mariana Teixeira', 'Nicolas Freitas', 'Olívia Cardoso', 'Paulo Moreira',
    'Rafaela Barros', 'Samuel Pinto', 'Tatiane Gomes', 'Vinícius Araújo'];
  const ddds = ['11', '21', '31', '41', '51', '61', '71', '81', '85', '91', '62', '48'];
  const clientesIn = nomes.map((nome, i) => {
    const plano = planos[i % planos.length];
    return {
      nome,
      whatsapp: `${ddds[i % ddds.length]}9${String(80000000 + i * 137531).slice(0, 8)}`,
      plano_id: plano.id,
      plano_nome: plano.nome,
      data_vencimento: addDias(hoje, (i * 5) % 40 - 8),
      quantidade: (i % 3) + 1,
      desconto: i % 4 === 0 ? 5 : 0,
      status: i === 17 ? 'inativo' : i === 18 ? 'pendente' : 'ativo',
      essencial: i % 6 === 0,
      notas: i % 5 === 0 ? 'Prefere pagar por PIX' : '',
    };
  });
  const clientes = await db.Cliente.bulkCreate(clientesIn);

  // Histórico de pagamentos dos últimos 6 meses
  const mensalidades = [];
  clientes.forEach((c, i) => {
    const plano = planos.find((p) => p.id === c.plano_id);
    const valor = plano.preco * c.quantidade - c.desconto;
    for (let m = 0; m < 6; m++) {
      if ((i + m) % 7 === 0 && m > 0) continue;
      const dt = addDias(hoje, -30 * m - (i % 20));
      if (plano.periodo !== 'mensal' && m % (plano.periodo === 'anual' ? 12 : 3) !== 0) continue;
      mensalidades.push({
        cliente_id: c.id, cliente_nome: c.nome, plano_id: plano.id, plano_nome: plano.nome,
        valor, custo: plano.custo * c.quantidade, data_vencimento: dt, data_pagamento: dt, status: 'pago',
        metodo_pagamento: m === 5 || (i + m) % 9 === 0 ? 'cadastro' : ['pix', 'renovacao', 'dinheiro', 'renovacao'][m % 4],
        descricao: `${plano.nome} — ${c.nome}`,
      });
    }
  });
  await db.Mensalidade.bulkCreate(mensalidades);

  await db.ConfiguracaoMensagem.bulkCreate([
    { nome: 'Lembrete 3 dias antes', tipo: 'vencimento_proximo', dias_antes: 3, dias_apos: 0, horario_envio: '09:00', ativo: true,
      mensagem: 'Olá {nome}! 😊 Seu plano {plano} vence em {data}. Valor: {valor}. Qualquer dúvida estou por aqui!' },
    { nome: 'Vence hoje', tipo: 'vencimento_hoje', dias_antes: 0, dias_apos: 0, horario_envio: '10:00', ativo: true,
      mensagem: 'Oi {nome}! Seu plano {plano} vence hoje. Para renovar: {valor}. Me chama que eu te mando o PIX 💜' },
    { nome: 'Atraso 2 dias', tipo: 'atraso', dias_antes: 0, dias_apos: 2, horario_envio: '11:00', ativo: false,
      mensagem: 'Olá {nome}, o plano {plano} venceu em {data}. Vamos renovar? Valor: {valor}.' },
    { nome: 'Boas-vindas', tipo: 'boas_vindas', dias_antes: 0, dias_apos: 0, horario_envio: '09:00', ativo: true,
      mensagem: 'Seja bem-vindo(a), {nome}! 🎉 Seu plano {plano} está ativo até {data}.' },
  ]);

  await db.NotaAdesiva.bulkCreate([
    { titulo: 'Chave PIX', conteudo: 'pix@minhaempresa.com.br', cor: 'verde', fixada: true },
    { titulo: 'Lembrar', conteudo: 'Ligar para fornecedor na sexta', cor: 'amarelo', fixada: false },
    { titulo: 'Promo do mês', conteudo: 'Anual com 10% de desconto até o dia 30', cor: 'rosa', fixada: false },
  ]);
  await db.Tarefa.bulkCreate([
    { titulo: 'Cobrar clientes em atraso', descricao: 'Ver lista de vencidos no painel', data_prazo: hoje, prioridade: 'alta', status: 'pendente', categoria: 'cobranca' },
    { titulo: 'Responder suporte do Bruno', data_prazo: addDias(hoje, 1), prioridade: 'media', status: 'em_andamento', categoria: 'atendimento' },
    { titulo: 'Fechar caixa do mês', data_prazo: addDias(hoje, 5), prioridade: 'baixa', status: 'pendente', categoria: 'administrativo' },
  ]);
  const mes = hoje.slice(0, 7);
  await db.Meta.bulkCreate([
    { mes, tipo: 'receita', valor_alvo: 2500, descricao: 'Faturar R$ 2.500 no mês' },
    { mes, tipo: 'clientes_novos', valor_alvo: 8, descricao: 'Conquistar 8 clientes novos' },
    { mes, tipo: 'renovacoes', valor_alvo: 15, descricao: '15 renovações' },
  ]);
}
