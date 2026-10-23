// POST { prompt, system? } → { text }
// Gerador de mensagens por IA e assistente de suporte. Usa o secret ANTHROPIC_API_KEY.
import Anthropic from 'npm:@anthropic-ai/sdk';
import { createClient } from 'npm:@supabase/supabase-js@2';
import { corsHeaders, json } from '../_shared/cors.ts';

const SYSTEM_PADRAO =
  'Você é o assistente do GESTOR SIMPLES, um app de gestão de clientes e mensalidades com cobrança pelo WhatsApp. ' +
  'Responda sempre em português do Brasil, de forma curta, clara e simpática.';

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
  const supabase = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_ANON_KEY')!, {
    global: { headers: { Authorization: req.headers.get('Authorization') || '' } },
  });
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return json({ error: 'Não autenticado' }, 401);
  if (!Deno.env.get('ANTHROPIC_API_KEY')) return json({ error: 'ANTHROPIC_API_KEY não configurada' }, 500);

  const { prompt, system } = await req.json().catch(() => ({}));
  if (!prompt) return json({ error: 'Informe o prompt' }, 400);

  try {
    const client = new Anthropic();
    const response = await client.beta.messages.create({
      model: 'claude-opus-5-5',
      max_tokens: 16000,
      output_config: { effort: 'low' },
      betas: ['server-side-fallback-2026-07-01'],
      fallbacks: 'default',
      system: system || SYSTEM_PADRAO,
      messages: [{ role: 'user', content: String(prompt).slice(0, 8000) }],
    } as any);
    if (response.stop_reason === 'refusal') return json({ error: 'A IA não pôde responder a esse pedido.' }, 422);
    const text = response.content.filter((b: any) => b.type === 'text').map((b: any) => b.text).join('\n').trim();
    return json({ text });
  } catch (e) {
    if (e instanceof Anthropic.RateLimitError) return json({ error: 'Muitas solicitações, tente de novo em instantes.' }, 429);
    if (e instanceof Anthropic.APIError) return json({ error: `Erro da IA (${e.status})` }, 502);
    return json({ error: String((e as Error)?.message || e) }, 500);
  }
});
