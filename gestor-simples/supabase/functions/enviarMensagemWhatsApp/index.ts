// POST { numero, mensagem, cliente_id?, cliente_nome?, tipo_mensagem? }
// Envia pela W-API e grava o resultado em historico_mensagens (como o usuário logado).
import { createClient } from 'npm:@supabase/supabase-js@2';
import { corsHeaders, json } from '../_shared/cors.ts';
import { enviarWapi } from '../_shared/wapi.ts';

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  const auth = req.headers.get('Authorization') || '';
  const supabase = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_ANON_KEY')!, {
    global: { headers: { Authorization: auth } },
  });
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return json({ error: 'Não autenticado' }, 401);

  const { numero, mensagem, cliente_id, cliente_nome, tipo_mensagem = 'manual' } = await req.json().catch(() => ({}));
  if (!numero || !mensagem) return json({ error: 'Informe numero e mensagem' }, 400);

  const r = await enviarWapi(numero, mensagem);
  await supabase.from('historico_mensagens').insert({
    cliente_id: cliente_id || null,
    cliente_nome: cliente_nome || null,
    whatsapp: r.numero,
    tipo_mensagem,
    mensagem,
    status: r.ok ? 'enviada' : 'erro',
    erro: r.ok ? null : r.erro,
  });
  return json({ success: r.ok, numero: r.numero, error: r.erro, resposta: r.resposta });
});
