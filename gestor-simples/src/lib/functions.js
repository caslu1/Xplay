// Funções de backend (Edge Functions do Supabase). No modo demonstração,
// os envios são apenas registrados no histórico e a IA usa modelos prontos.
import { supabase, DEMO } from './supabase';
import { db } from './db';
import { aplicarTemplate, deveEnviar, hojeISO, jaEnviadoHoje, limparNumero } from './utils';

async function invocar(nome, body) {
  const { data, error } = await supabase.functions.invoke(nome, { body });
  if (error) {
    let msg = error.message;
    try {
      const j = await error.context?.json();
      if (j?.error) msg = j.error;
    } catch { /* corpo não é JSON */ }
    throw new Error(msg);
  }
  return data;
}

/** Envia um WhatsApp pela W-API e grava no histórico. */
export async function enviarMensagemWhatsApp({ numero, mensagem, cliente_id, cliente_nome, tipo_mensagem = 'manual' }) {
  if (!DEMO) return invocar('enviarMensagemWhatsApp', { numero, mensagem, cliente_id, cliente_nome, tipo_mensagem });
  const erro = 'Modo demonstração: conecte o Supabase e a W-API para enviar de verdade.';
  await db.HistoricoMensagem.create({
    cliente_id, cliente_nome, whatsapp: limparNumero(numero), tipo_mensagem, mensagem, status: 'erro', erro, data_envio: new Date().toISOString(),
  });
  return { success: false, error: erro, demo: true };
}

/** Roda a rotina das mensagens automáticas agora (ignorando o horário configurado). */
export async function verificarEnvioMensagens({ forcar = true, empresa } = {}) {
  if (!DEMO) return invocar('verificarEnvioMensagens', { forcar });
  const hoje = hojeISO();
  const [configs, clientes, planos, historico] = await Promise.all([
    db.ConfiguracaoMensagem.list(), db.Cliente.list(), db.Plano.list(), db.HistoricoMensagem.list(),
  ]);
  const planoPor = new Map(planos.map((p) => [p.id, p]));
  const resumo = { enviadas: 0, erros: 0, ignoradas: 0, detalhes: [] };
  for (const cfg of configs.filter((c) => c.ativo)) {
    for (const cliente of clientes) {
      if (!deveEnviar(cfg, cliente, hoje)) continue;
      if (jaEnviadoHoje(historico, cliente.id, cfg.tipo, hoje)) { resumo.ignoradas++; continue; }
      const mensagem = aplicarTemplate(cfg.mensagem, { cliente, plano: planoPor.get(cliente.plano_id), empresa });
      const r = await enviarMensagemWhatsApp({ numero: cliente.whatsapp, mensagem, cliente_id: cliente.id, cliente_nome: cliente.nome, tipo_mensagem: cfg.tipo });
      r.success ? resumo.enviadas++ : resumo.erros++;
      resumo.detalhes.push({ cliente: cliente.nome, tipo: cfg.tipo, ok: r.success, erro: r.error });
    }
  }
  return resumo;
}

const MODELOS_DEMO = {
  vencimento_proximo: 'Olá {nome}! 😊 Passando para lembrar que seu plano {plano} vence em {data}. O valor é {valor}. Qualquer dúvida, estou à disposição!',
  vencimento_hoje: 'Oi {nome}! Seu plano {plano} vence hoje ({data}). Para renovar, o valor é {valor}. Me chama aqui que eu te mando o PIX! 💜',
  atraso: 'Olá {nome}, tudo bem? Notamos que o plano {plano} venceu em {data}. Para continuar aproveitando, é só renovar ({valor}). Posso ajudar?',
  plano_expirado: 'Oi {nome}! Seu plano {plano} expirou em {data}. Sentimos sua falta! Renove por {valor} e volte a aproveitar. 🙌',
  renovacao: 'Obrigado, {nome}! 🎉 Sua renovação do plano {plano} foi confirmada. Próximo vencimento: {data}.',
  boas_vindas: 'Seja bem-vindo(a), {nome}! 🎉 Seu plano {plano} já está ativo. Vencimento: {data}. Conte com a gente!',
};

/** IA (Claude) para gerar mensagens e responder dúvidas. */
export async function invokeLLM({ prompt, system, tipo }) {
  if (!DEMO) return (await invocar('invokeLLM', { prompt, system })).text;
  await new Promise((r) => setTimeout(r, 600));
  if (tipo && MODELOS_DEMO[tipo]) return MODELOS_DEMO[tipo];
  return 'No modo demonstração a IA não está conectada. Configure o Supabase e o secret ANTHROPIC_API_KEY para respostas reais. Dica: veja o FAQ logo abaixo — a maioria das dúvidas já está respondida lá.';
}
