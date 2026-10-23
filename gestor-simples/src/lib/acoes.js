// Ações de clientes e planos que geram registro financeiro e entrada no histórico (com desfazer).
import { db } from './db';
import { registrarAtividade } from './profile';
import { addPeriodo, hojeISO, valorCliente, custoCliente } from './utils';

function mensalidadeDe(cliente, plano, metodo, extra = {}) {
  return {
    cliente_id: cliente.id,
    cliente_nome: cliente.nome,
    plano_id: plano?.id || cliente.plano_id,
    plano_nome: plano?.nome || cliente.plano_nome,
    valor: valorCliente(cliente, plano),
    custo: custoCliente(cliente, plano),
    data_vencimento: cliente.data_vencimento,
    data_pagamento: hojeISO(),
    status: 'pago',
    metodo_pagamento: metodo,
    descricao: `${metodo === 'cadastro' ? 'Cadastro' : 'Renovação'} — ${plano?.nome || cliente.plano_nome}`,
    ...extra,
  };
}

export async function criarCliente(dados, plano, { registrarPagamento = true } = {}) {
  const cliente = await db.Cliente.create({ ...dados, plano_nome: plano?.nome });
  const desfazer = [{ op: 'delete', entidade: 'Cliente', ids: [cliente.id] }];
  if (registrarPagamento) {
    const m = await db.Mensalidade.create(mensalidadeDe(cliente, plano, 'cadastro'));
    desfazer.push({ op: 'delete', entidade: 'Mensalidade', ids: [m.id] });
  }
  await registrarAtividade({ acao: 'criar', entidade: 'Cliente', descricao: `Cliente “${cliente.nome}” cadastrado`, desfazer });
  return cliente;
}

export async function editarCliente(anterior, dados, plano) {
  const cliente = await db.Cliente.update(anterior.id, { ...dados, plano_nome: plano?.nome || dados.plano_nome });
  await registrarAtividade({
    acao: 'editar', entidade: 'Cliente', descricao: `Cliente “${cliente.nome}” editado`,
    desfazer: [{ op: 'restore', entidade: 'Cliente', registros: [anterior] }],
  });
  return cliente;
}

/**
 * Renova: registra a mensalidade paga e empurra o vencimento um período.
 * Se o cliente estava vencido há muito tempo, `aPartirDeHoje` conta o novo período a partir de hoje.
 */
export async function renovarCliente(anterior, plano, { metodo = 'renovacao', meses = 1, aPartirDeHoje = false, valor } = {}) {
  const base = aPartirDeHoje ? hojeISO() : String(anterior.data_vencimento).slice(0, 10);
  let novaData = base;
  for (let i = 0; i < meses; i++) novaData = addPeriodo(novaData, plano?.periodo || 'mensal');
  const cliente = await db.Cliente.update(anterior.id, { data_vencimento: novaData, status: 'ativo' });
  const m = await db.Mensalidade.create(mensalidadeDe(anterior, plano, metodo === 'renovacao' ? 'renovacao' : metodo, {
    data_vencimento: anterior.data_vencimento,
    valor: valor ?? valorCliente(anterior, plano) * meses,
    custo: custoCliente(anterior, plano) * meses,
    descricao: `Renovação — ${plano?.nome || anterior.plano_nome}${meses > 1 ? ` (${meses}x)` : ''}`,
  }));
  await registrarAtividade({
    acao: 'renovar', entidade: 'Cliente', descricao: `Renovação de “${anterior.nome}” até ${novaData.split('-').reverse().join('/')}`,
    desfazer: [
      { op: 'restore', entidade: 'Cliente', registros: [anterior] },
      { op: 'delete', entidade: 'Mensalidade', ids: [m.id] },
    ],
  });
  return { cliente, mensalidade: m };
}

export async function excluirClientes(clientes) {
  await db.Cliente.bulkDelete(clientes.map((c) => c.id));
  await registrarAtividade({
    acao: 'excluir', entidade: 'Cliente',
    descricao: clientes.length === 1 ? `Cliente “${clientes[0].nome}” excluído` : `${clientes.length} clientes excluídos`,
    desfazer: [{ op: 'restore', entidade: 'Cliente', registros: clientes }],
  });
}

export async function mudarStatusClientes(clientes, status) {
  await db.Cliente.bulkUpdate(clientes.map((c) => c.id), { status });
  const verbo = status === 'arquivado' ? 'arquivado' : `marcado como ${status}`;
  await registrarAtividade({
    acao: status === 'arquivado' ? 'arquivar' : 'editar', entidade: 'Cliente',
    descricao: clientes.length === 1 ? `Cliente “${clientes[0].nome}” ${verbo}` : `${clientes.length} clientes: ${verbo}`,
    desfazer: [{ op: 'restore', entidade: 'Cliente', registros: clientes }],
  });
}

export async function salvarPlano(anterior, dados) {
  if (anterior) {
    const p = await db.Plano.update(anterior.id, dados);
    // Mantém o nome do plano atualizado nos clientes
    if (anterior.nome !== dados.nome) {
      const ids = (await db.Cliente.list()).filter((c) => c.plano_id === anterior.id).map((c) => c.id);
      await db.Cliente.bulkUpdate(ids, { plano_nome: dados.nome });
    }
    await registrarAtividade({ acao: 'editar', entidade: 'Plano', descricao: `Plano “${p.nome}” editado`, desfazer: [{ op: 'restore', entidade: 'Plano', registros: [anterior] }] });
    return p;
  }
  const p = await db.Plano.create(dados);
  await registrarAtividade({ acao: 'criar', entidade: 'Plano', descricao: `Plano “${p.nome}” criado`, desfazer: [{ op: 'delete', entidade: 'Plano', ids: [p.id] }] });
  return p;
}

export async function excluirPlano(plano) {
  await db.Plano.delete(plano.id);
  await registrarAtividade({ acao: 'excluir', entidade: 'Plano', descricao: `Plano “${plano.nome}” excluído`, desfazer: [{ op: 'restore', entidade: 'Plano', registros: [plano] }] });
}
