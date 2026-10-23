import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { MessageSquare, Plus, Pencil, Trash2, Sparkles, Send, Play, Clock, Search, RotateCw, FlaskConical, Info } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { db } from '../lib/db';
import { useAuth } from '../context/AuthContext';
import { usePopup } from '../context/PopupContext';
import { enviarMensagemWhatsApp, verificarEnvioMensagens, invokeLLM } from '../lib/functions';
import { aplicarTemplate, cn, deveEnviar, hojeISO, formatarTelefone } from '../lib/utils';
import { Button, Card, Badge, Modal, Field, Input, Select, Textarea, PageHeader, Empty, Spinner, Tabs, Toggle, IconButton } from '../components/ui';

export const TIPOS_MSG = {
  vencimento_proximo: { l: 'Vencimento próximo', c: 'bg-amber-100 text-amber-700', quando: (c) => `${c.dias_antes} dia(s) antes do vencimento` },
  vencimento_hoje: { l: 'Vence hoje', c: 'bg-orange-100 text-orange-700', quando: () => 'No dia do vencimento' },
  atraso: { l: 'Atraso', c: 'bg-red-100 text-red-600', quando: (c) => `${c.dias_apos} dia(s) após o vencimento` },
  plano_expirado: { l: 'Plano expirado', c: 'bg-rose-100 text-rose-700', quando: (c) => `${c.dias_apos} dia(s) após expirar` },
  boas_vindas: { l: 'Boas-vindas', c: 'bg-emerald-100 text-emerald-700', quando: () => 'No dia do cadastro' },
  renovacao: { l: 'Renovação', c: 'bg-brand-100 text-brand-700', quando: () => 'Ao renovar (enviada pela tela de renovação)' },
};

const VARIAVEIS = ['{nome}', '{plano}', '{valor}', '{data}', '{link}', '{empresa}'];

export default function MensagensAutomaticas() {
  const { profile } = useAuth();
  const { confirm, toast, alert } = usePopup();
  const [params] = useSearchParams();
  const { data, loading } = useEntities(['ConfiguracaoMensagem', 'Cliente', 'Plano', 'HistoricoMensagem']);
  const [aba, setAba] = useState(params.get('aba') || 'config');
  const [form, setForm] = useState(null);
  const [teste, setTeste] = useState(null);
  const [rodando, setRodando] = useState(false);
  const [busca, setBusca] = useState('');
  const [statusF, setStatusF] = useState('');
  const planoPor = useMemo(() => new Map(data.Plano.map((p) => [p.id, p])), [data.Plano]);
  const hoje = hojeISO();

  const filaHoje = useMemo(() => {
    const out = [];
    for (const cfg of data.ConfiguracaoMensagem.filter((c) => c.ativo)) {
      for (const c of data.Cliente) if (deveEnviar(cfg, c, hoje)) out.push({ cfg, c });
    }
    return out;
  }, [data.ConfiguracaoMensagem, data.Cliente, hoje]);

  const historico = useMemo(() => {
    const q = busca.toLowerCase();
    return [...data.HistoricoMensagem]
      .filter((h) => !statusF || h.status === statusF)
      .filter((h) => !q || (h.cliente_nome || '').toLowerCase().includes(q) || (h.whatsapp || '').includes(q) || (h.mensagem || '').toLowerCase().includes(q))
      .sort((a, b) => String(b.data_envio).localeCompare(String(a.data_envio)));
  }, [data.HistoricoMensagem, busca, statusF]);

  async function executar() {
    if (!(await confirm({ title: 'Enviar agora?', message: `${filaHoje.length} mensagem(ns) programada(s) para hoje serão enviadas (as já enviadas hoje são ignoradas).` }))) return;
    setRodando(true);
    try {
      const r = await verificarEnvioMensagens({ forcar: true, empresa: profile?.empresa });
      await alert({ title: 'Rotina concluída', message: `Enviadas: ${r.enviadas}\nCom erro: ${r.erros}\nJá enviadas hoje (ignoradas): ${r.ignoradas}` });
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setRodando(false);
    }
  }

  async function reenviar(h) {
    try {
      const r = await enviarMensagemWhatsApp({ numero: h.whatsapp, mensagem: h.mensagem, cliente_id: h.cliente_id, cliente_nome: h.cliente_nome, tipo_mensagem: h.tipo_mensagem });
      toast(r.success ? 'Reenviada' : r.error, r.success ? 'success' : 'error');
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  if (loading) return <Spinner />;
  return (
    <div>
      <PageHeader icon={MessageSquare} title="Mensagens automáticas" subtitle="Lembretes e cobranças pelo WhatsApp, no horário que você escolher"
        actions={<>
          <Button variant="secondary" icon={Play} loading={rodando} onClick={executar}>Enviar fila de hoje ({filaHoje.length})</Button>
          <Button icon={Plus} onClick={() => setForm({ nome: '', tipo: 'vencimento_proximo', mensagem: '', dias_antes: 3, dias_apos: 1, horario_envio: '09:00', ativo: true })}>Nova mensagem</Button>
        </>} />

      <div className="mb-4"><Tabs value={aba} onChange={setAba} tabs={[
        { value: 'config', label: 'Configurações', count: data.ConfiguracaoMensagem.length },
        { value: 'fila', label: 'Fila de hoje', count: filaHoje.length },
        { value: 'historico', label: 'Histórico', count: data.HistoricoMensagem.length },
      ]} /></div>

      {aba === 'config' && (
        <>
          <Card className="mb-4 flex items-start gap-3 border-sky-100 bg-sky-50 p-4 text-sm text-sky-900">
            <Info className="mt-0.5 h-5 w-5 shrink-0 text-sky-600" />
            <p>A rotina roda de hora em hora e envia cada mensagem ativa no horário configurado, sem repetir para o mesmo cliente no mesmo dia. Variáveis: {VARIAVEIS.map((v) => <code key={v} className="mx-0.5 rounded bg-white px-1">{v}</code>)}</p>
          </Card>
          {data.ConfiguracaoMensagem.length === 0 ? (
            <Card><Empty icon={MessageSquare} title="Nenhuma mensagem configurada">Crie lembretes de vencimento, cobranças de atraso e boas-vindas.</Empty></Card>
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {data.ConfiguracaoMensagem.map((cfg) => {
                const t = TIPOS_MSG[cfg.tipo];
                return (
                  <Card key={cfg.id} className={cn('p-5', !cfg.ativo && 'opacity-60')}>
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <Badge className={t.c}>{t.l}</Badge>
                        <p className="mt-1.5 font-bold text-slate-800">{cfg.nome}</p>
                        <p className="flex items-center gap-1 text-xs text-slate-500"><Clock className="h-3.5 w-3.5" />{t.quando(cfg)} · às {cfg.horario_envio}</p>
                      </div>
                      <Toggle checked={cfg.ativo} onChange={(v) => db.ConfiguracaoMensagem.update(cfg.id, { ativo: v })} />
                    </div>
                    <p className="mt-3 line-clamp-4 whitespace-pre-line rounded-xl bg-[#dcf8c6]/60 p-3 text-sm text-slate-700">{cfg.mensagem}</p>
                    <div className="mt-3 flex justify-end gap-1">
                      <Button size="sm" variant="ghost" icon={FlaskConical} onClick={() => setTeste({ cfg, clienteId: data.Cliente[0]?.id || '', numero: '' })}>Testar</Button>
                      <IconButton icon={Pencil} title="Editar" onClick={() => setForm(cfg)} />
                      <IconButton icon={Trash2} title="Excluir" className="hover:!text-red-500" onClick={async () => {
                        if (await confirm({ title: `Excluir “${cfg.nome}”?`, danger: true })) { await db.ConfiguracaoMensagem.delete(cfg.id); toast('Mensagem excluída'); }
                      }} />
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </>
      )}

      {aba === 'fila' && (
        <Card className="divide-y divide-slate-100">
          {filaHoje.length === 0 && <Empty icon={Send} title="Nada programado para hoje" />}
          {filaHoje.map(({ cfg, c }) => {
            const enviada = data.HistoricoMensagem.some((h) => h.cliente_id === c.id && h.tipo_mensagem === cfg.tipo && h.status === 'enviada' && String(h.data_envio).slice(0, 10) === hoje);
            return (
              <div key={cfg.id + c.id} className="flex items-center gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">{c.nome} <span className="text-xs font-normal text-slate-400">{formatarTelefone(c.whatsapp)}</span></p>
                  <p className="truncate text-xs text-slate-500">{aplicarTemplate(cfg.mensagem, { cliente: c, plano: planoPor.get(c.plano_id), empresa: profile?.empresa })}</p>
                </div>
                <Badge className={TIPOS_MSG[cfg.tipo].c}>{TIPOS_MSG[cfg.tipo].l}</Badge>
                <Badge className={enviada ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}>{enviada ? 'enviada' : `às ${cfg.horario_envio}`}</Badge>
              </div>
            );
          })}
        </Card>
      )}

      {aba === 'historico' && (
        <>
          <Card className="mb-3 flex flex-col gap-2 p-3 sm:flex-row">
            <div className="relative flex-1"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><Input className="pl-9" placeholder="Buscar cliente, número ou texto…" value={busca} onChange={(e) => setBusca(e.target.value)} /></div>
            <Select className="sm:w-44" value={statusF} onChange={(e) => setStatusF(e.target.value)}><option value="">Todos os status</option><option value="enviada">Enviadas</option><option value="erro">Com erro</option><option value="pendente">Pendentes</option></Select>
          </Card>
          <Card className="divide-y divide-slate-100">
            {historico.length === 0 && <Empty icon={MessageSquare} title="Nenhuma mensagem no histórico" />}
            {historico.slice(0, 200).map((h) => (
              <div key={h.id} className="flex items-start gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold">{h.cliente_nome || formatarTelefone(h.whatsapp)} <span className="text-xs font-normal text-slate-400">· {new Date(h.data_envio).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })} · {TIPOS_MSG[h.tipo_mensagem]?.l || h.tipo_mensagem}</span></p>
                  <p className="line-clamp-2 text-sm text-slate-600">{h.mensagem}</p>
                  {h.erro && <p className="mt-0.5 text-xs text-red-500">{h.erro}</p>}
                </div>
                <Badge className={h.status === 'enviada' ? 'bg-emerald-100 text-emerald-700' : h.status === 'erro' ? 'bg-red-100 text-red-600' : 'bg-amber-100 text-amber-700'}>{h.status}</Badge>
                {h.status !== 'enviada' && <IconButton icon={RotateCw} title="Reenviar" onClick={() => reenviar(h)} />}
              </div>
            ))}
          </Card>
        </>
      )}

      <ConfigForm form={form} setForm={setForm} onSaved={() => toast('Mensagem salva')} />
      <TesteModal teste={teste} setTeste={setTeste} clientes={data.Cliente} planoPor={planoPor} empresa={profile?.empresa} />
    </div>
  );
}

function ConfigForm({ form, setForm, onSaved }) {
  const { toast } = usePopup();
  const [gerando, setGerando] = useState(false);
  const [tom, setTom] = useState('amigável');
  if (!form) return null;
  const set = (k, v) => setForm({ ...form, [k]: v });

  async function gerar() {
    setGerando(true);
    try {
      const texto = await invokeLLM({
        tipo: form.tipo,
        prompt: `Escreva UMA mensagem curta de WhatsApp (máx. 350 caracteres), tom ${tom}, para o tipo "${TIPOS_MSG[form.tipo].l}" (${TIPOS_MSG[form.tipo].quando(form)}). ` +
          `Use exatamente estas variáveis entre chaves quando fizer sentido: {nome}, {plano}, {valor}, {data}. Pode usar 1 ou 2 emojis. Responda só com o texto da mensagem, sem aspas.`,
      });
      set('mensagem', texto.trim());
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setGerando(false);
    }
  }

  async function salvar() {
    const { id, created_date, updated_date, created_by, ...dados } = form;
    const payload = { ...dados, nome: dados.nome || TIPOS_MSG[dados.tipo].l, dias_antes: Number(dados.dias_antes) || 0, dias_apos: Number(dados.dias_apos) || 0 };
    if (id) await db.ConfiguracaoMensagem.update(id, payload); else await db.ConfiguracaoMensagem.create(payload);
    onSaved();
    setForm(null);
  }

  return (
    <Modal open onClose={() => setForm(null)} title={form.id ? 'Editar mensagem automática' : 'Nova mensagem automática'} size="lg"
      footer={<><Button variant="secondary" onClick={() => setForm(null)}>Cancelar</Button><Button onClick={salvar} disabled={!form.mensagem.trim()}>Salvar</Button></>}>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Tipo"><Select value={form.tipo} onChange={(e) => set('tipo', e.target.value)}>{Object.entries(TIPOS_MSG).map(([k, v]) => <option key={k} value={k}>{v.l}</option>)}</Select></Field>
        <Field label="Nome"><Input value={form.nome} onChange={(e) => set('nome', e.target.value)} placeholder={TIPOS_MSG[form.tipo].l} /></Field>
        {form.tipo === 'vencimento_proximo' && <Field label="Dias antes do vencimento"><Input type="number" min="1" max="60" value={form.dias_antes} onChange={(e) => set('dias_antes', e.target.value)} /></Field>}
        {['atraso', 'plano_expirado'].includes(form.tipo) && <Field label="Dias após o vencimento"><Input type="number" min="1" max="90" value={form.dias_apos} onChange={(e) => set('dias_apos', e.target.value)} /></Field>}
        <Field label="Horário de envio"><Input type="time" step="3600" value={form.horario_envio} onChange={(e) => set('horario_envio', e.target.value)} /></Field>
        <div className="sm:col-span-2">
          <div className="mb-1.5 flex flex-wrap items-center justify-between gap-2">
            <span className="label !mb-0">Mensagem</span>
            <div className="flex items-center gap-2">
              <Select className="!w-32 !py-1.5 text-xs" value={tom} onChange={(e) => setTom(e.target.value)}>
                {['amigável', 'formal', 'divertido', 'direto', 'urgente'].map((t) => <option key={t}>{t}</option>)}
              </Select>
              <Button size="sm" variant="secondary" icon={Sparkles} loading={gerando} onClick={gerar}>Gerar com IA</Button>
            </div>
          </div>
          <Textarea className="min-h-[140px]" value={form.mensagem} onChange={(e) => set('mensagem', e.target.value)} placeholder="Olá {nome}! Seu plano {plano} vence em {data}…" />
          <div className="mt-2 flex flex-wrap gap-1">
            {VARIAVEIS.map((v) => <button key={v} type="button" onClick={() => set('mensagem', form.mensagem + v)} className="rounded-lg bg-brand-50 px-2 py-1 font-mono text-xs text-brand-700 hover:bg-brand-100">{v}</button>)}
          </div>
        </div>
        <div className="sm:col-span-2"><Toggle checked={form.ativo} onChange={(v) => set('ativo', v)} label="Ativa" /></div>
      </div>
    </Modal>
  );
}

function TesteModal({ teste, setTeste, clientes, planoPor, empresa }) {
  const { toast } = usePopup();
  const [enviando, setEnviando] = useState(false);
  if (!teste) return null;
  const cliente = clientes.find((c) => c.id === teste.clienteId) || { nome: 'Cliente Teste', whatsapp: '', data_vencimento: hojeISO(), quantidade: 1 };
  const texto = aplicarTemplate(teste.cfg.mensagem, { cliente, plano: planoPor.get(cliente.plano_id), empresa });

  async function enviar() {
    setEnviando(true);
    try {
      const r = await enviarMensagemWhatsApp({ numero: teste.numero || cliente.whatsapp, mensagem: texto, cliente_id: teste.numero ? null : cliente.id, cliente_nome: teste.numero ? 'Teste' : cliente.nome, tipo_mensagem: 'teste' });
      toast(r.success ? 'Mensagem de teste enviada' : r.error, r.success ? 'success' : 'error');
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal open onClose={() => setTeste(null)} title={`Testar “${teste.cfg.nome}”`}
      footer={<><Button variant="secondary" onClick={() => setTeste(null)}>Fechar</Button><Button variant="whatsapp" icon={Send} loading={enviando} onClick={enviar} disabled={!teste.numero && !cliente.whatsapp}>Enviar teste</Button></>}>
      <div className="space-y-4">
        <Field label="Usar dados do cliente">
          <Select value={teste.clienteId} onChange={(e) => setTeste({ ...teste, clienteId: e.target.value })}>
            {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </Select>
        </Field>
        <Field label="Enviar para outro número (opcional)" hint="Deixe vazio para enviar ao cliente escolhido. Use o seu número para testar."><Input inputMode="tel" value={teste.numero} onChange={(e) => setTeste({ ...teste, numero: e.target.value })} placeholder="(11) 99999-9999" /></Field>
        <div>
          <span className="label">Pré-visualização</span>
          <p className="whitespace-pre-line rounded-2xl rounded-tl-sm bg-[#dcf8c6] p-3 text-sm text-slate-800 shadow-sm">{texto}</p>
        </div>
      </div>
    </Modal>
  );
}
