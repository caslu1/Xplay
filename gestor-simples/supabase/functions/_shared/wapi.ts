import { limparNumero } from './regras.js';

/** Envia um texto pela W-API. Devolve { ok, numero, resposta?, erro? }. */
export async function enviarWapi(numero: string, mensagem: string) {
  const instance = Deno.env.get('WAPI_INSTANCE_ID');
  const token = Deno.env.get('WAPI_TOKEN');
  const base = (Deno.env.get('WAPI_BASE_URL') || 'https://waapi.app').replace(/\/$/, '');
  const limpo = limparNumero(numero);
  if (!instance || !token) return { ok: false, numero: limpo, erro: 'WAPI_INSTANCE_ID / WAPI_TOKEN não configurados' };
  if (limpo.length < 12) return { ok: false, numero: limpo, erro: 'Número de WhatsApp inválido' };
  try {
    const res = await fetch(`${base}/api/v1/instances/${instance}/client/action/send-message`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ chatId: `${limpo}@c.us`, message: mensagem }),
    });
    const resposta = await res.json().catch(() => ({}));
    // A API pode responder 200 com status "error" (ex.: instância desconectada).
    const erroApi = resposta?.status === 'error' || resposta?.data?.status === 'error';
    if (!res.ok || erroApi) {
      const erro = resposta?.message || resposta?.data?.message || resposta?.error || `HTTP ${res.status}`;
      return { ok: false, numero: limpo, resposta, erro: String(erro) };
    }
    return { ok: true, numero: limpo, resposta };
  } catch (e) {
    return { ok: false, numero: limpo, erro: String((e as Error)?.message || e) };
  }
}
