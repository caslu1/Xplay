// Rotina das mensagens automáticas.
// - Chamada pelo pg_cron (header x-cron-secret): processa todos os usuários e respeita horario_envio.
// - Chamada pelo app (usuário logado, body { forcar: true }): processa só o usuário e ignora o horário.
import { createClient } from 'npm:@supabase/supabase-js@2';
import { corsHeaders, json } from '../_shared/cors.ts';
import { enviarWapi } from '../_shared/wapi.ts';
import { aplicarTemplate, deveEnviar, hojeISO, horaDeEnviar, jaEnviadoHoje, addDias } from '../_shared/regras.js';

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  const url = Deno.env.get('SUPABASE_URL')!;
  const admin = createClient(url, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
  const body = await req.json().catch(() => ({}));

  let donos: string[] | null = null; // null = todos
  let respeitarHorario = true;
  const cron = Deno.env.get('CRON_SECRET');
  if (!(cron && req.headers.get('x-cron-secret') === cron)) {
    const userClient = createClient(url, Deno.env.get('SUPABASE_ANON_KEY')!, {
      global: { headers: { Authorization: req.headers.get('Authorization') || '' } },
    });
    const { data: { user } } = await userClient.auth.getUser();
    if (!user) return json({ error: 'Não autenticado' }, 401);
    donos = [user.id];
    respeitarHorario = !body?.forcar;
  }

  const hoje = hojeISO();
  let q = admin.from('configuracoes_mensagem').select('*').eq('ativo', true);
  if (donos) q = q.in('created_by', donos);
  const { data: configs, error } = await q;
  if (error) return json({ error: error.message }, 500);

  const porDono = new Map<string, any[]>();
  for (const c of configs || []) {
    if (respeitarHorario && !horaDeEnviar(c)) continue;
    porDono.set(c.created_by, [...(porDono.get(c.created_by) || []), c]);
  }

  const resumo = { enviadas: 0, erros: 0, ignoradas: 0, detalhes: [] as any[] };
  for (const [dono, cfgs] of porDono) {
    const [{ data: clientes }, { data: planos }, { data: historico }, { data: perfil }] = await Promise.all([
      fetchAll(admin.from('clientes').select('*').eq('created_by', dono).eq('status', 'ativo')),
      admin.from('planos').select('*').eq('created_by', dono),
      admin.from('historico_mensagens').select('cliente_id,tipo_mensagem,status,data_envio').eq('created_by', dono)
        .gte('data_envio', addDias(hoje, -1)),
      admin.from('profiles').select('empresa').eq('id', dono).maybeSingle(),
    ]);
    const planoPor = new Map((planos || []).map((p: any) => [p.id, p]));
    for (const cfg of cfgs) {
      for (const cliente of clientes || []) {
        if (!deveEnviar(cfg, cliente, hoje)) continue;
        if (jaEnviadoHoje(historico || [], cliente.id, cfg.tipo, hoje)) { resumo.ignoradas++; continue; }
        const mensagem = aplicarTemplate(cfg.mensagem, { cliente, plano: planoPor.get(cliente.plano_id), empresa: perfil?.empresa });
        const r = await enviarWapi(cliente.whatsapp, mensagem);
        await admin.from('historico_mensagens').insert({
          cliente_id: cliente.id, cliente_nome: cliente.nome, whatsapp: r.numero, tipo_mensagem: cfg.tipo,
          mensagem, status: r.ok ? 'enviada' : 'erro', erro: r.ok ? null : r.erro, created_by: dono,
        });
        (historico || []).push({ cliente_id: cliente.id, tipo_mensagem: cfg.tipo, status: r.ok ? 'enviada' : 'erro', data_envio: new Date().toISOString() });
        r.ok ? resumo.enviadas++ : resumo.erros++;
        resumo.detalhes.push({ cliente: cliente.nome, tipo: cfg.tipo, ok: r.ok, erro: r.erro });
      }
    }
  }
  return json(resumo);
});

// Paginação para não truncar em 1.000 linhas.
async function fetchAll(query: any) {
  const out: any[] = [];
  for (let from = 0; ; from += 1000) {
    const { data, error } = await query.range(from, from + 999);
    if (error) return { data: out, error };
    out.push(...(data || []));
    if (!data || data.length < 1000) return { data: out };
  }
}
