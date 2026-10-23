import { useEffect, useRef, useState } from 'react';
import {
  Settings, User, Bell, MessageCircle, Lock, FileSpreadsheet, HelpCircle, Sparkles, Mic, MicOff, Send, Camera, LogOut, ChevronDown, Plug,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { usePopup } from '../context/PopupContext';
import { db } from '../lib/db';
import { invokeLLM } from '../lib/functions';
import { aplicarTemplate, addDias, hojeISO, baixarArquivo, cn, formatarDataBR, METODOS, STATUS_CLIENTE } from '../lib/utils';
import { Button, Card, Field, Input, Textarea, PageHeader, Toggle, Select, Tabs } from '../components/ui';

const EXEMPLO = { cliente: { nome: 'Maria Silva', data_vencimento: addDias(hojeISO(), 3), quantidade: 1, desconto: 0, plano_nome: 'Premium' }, plano: { nome: 'Premium', preco: 50 } };

const FAQ = [
  ['Como instalo o app no celular?', 'Abra o site no navegador do celular. No Android (Chrome), toque em ⋮ → “Instalar app” ou “Adicionar à tela inicial”. No iPhone (Safari), toque em Compartilhar → “Adicionar à Tela de Início”.'],
  ['Como funcionam as mensagens automáticas?', 'Em Mensagens automáticas você define o tipo (antes, no dia ou depois do vencimento, boas-vindas), o texto e o horário. A rotina roda de hora em hora e nunca envia o mesmo tipo para o mesmo cliente duas vezes no dia.'],
  ['Preciso de quê para enviar pelo WhatsApp?', 'De uma instância na W-API conectada ao seu WhatsApp. O ID da instância e o token ficam guardados como segredos no servidor (WAPI_INSTANCE_ID e WAPI_TOKEN). Sem isso, use “Abrir WhatsApp” ou “Copiar” na mensagem.'],
  ['Errei uma ação. Dá para voltar?', 'Sim. Toque no ícone de relógio (Histórico de atividades) no topo e use “Desfazer”. As ações ficam guardadas por 3 dias.'],
  ['Como o valor da mensalidade é calculado?', 'Preço do plano × número de conexões − desconto do cliente.'],
  ['O que é cliente essencial?', 'Uma marcação (★) para clientes importantes. Eles aparecem em destaque no Dashboard.'],
  ['Como importo meus clientes?', 'Em Importar clientes, envie uma planilha (.xlsx ou .csv) com colunas como nome, whatsapp, plano e vencimento. Você confere tudo antes de importar.'],
];

export default function Configuracoes() {
  const [aba, setAba] = useState('perfil');
  const { logout } = useAuth();
  return (
    <div>
      <PageHeader icon={Settings} title="Configurações" subtitle="Perfil, mensagens, segurança e suporte"
        actions={<Button variant="secondary" icon={LogOut} onClick={logout}>Sair</Button>} />
      <div className="mb-5"><Tabs value={aba} onChange={setAba} tabs={[
        { value: 'perfil', label: 'Perfil' }, { value: 'mensagens', label: 'Mensagens do WhatsApp' }, { value: 'painel', label: 'Notificações' },
        { value: 'seguranca', label: 'Segurança' }, { value: 'exportar', label: 'Relatórios' }, { value: 'suporte', label: 'Suporte' },
      ]} /></div>
      {aba === 'perfil' && <Perfil />}
      {aba === 'mensagens' && <Mensagens />}
      {aba === 'painel' && <Painel />}
      {aba === 'seguranca' && <Seguranca />}
      {aba === 'exportar' && <Exportar />}
      {aba === 'suporte' && <Suporte />}
    </div>
  );
}

function Secao({ icon: Icon, title, desc, children }) {
  return (
    <Card className="mb-5 p-5">
      <div className="mb-4 flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-700"><Icon className="h-5 w-5" /></div>
        <div><h2 className="font-bold text-slate-800">{title}</h2>{desc && <p className="text-sm text-slate-500">{desc}</p>}</div>
      </div>
      {children}
    </Card>
  );
}

function Perfil() {
  const { profile, updateProfile, demo } = useAuth();
  const { toast } = usePopup();
  const [f, setF] = useState({ full_name: profile.full_name || '', empresa: profile.empresa || '' });
  const arquivo = useRef(null);

  function foto(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    const img = new Image();
    img.onload = () => {
      const c = document.createElement('canvas');
      const s = 160;
      c.width = c.height = s;
      const r = Math.max(s / img.width, s / img.height);
      c.getContext('2d').drawImage(img, (s - img.width * r) / 2, (s - img.height * r) / 2, img.width * r, img.height * r);
      updateProfile({ profile_photo: c.toDataURL('image/jpeg', 0.85) }).then(() => toast('Foto atualizada'));
      URL.revokeObjectURL(img.src);
    };
    img.src = URL.createObjectURL(file);
  }

  return (
    <>
      <Secao icon={User} title="Seu perfil" desc="O nome da empresa entra nas mensagens pela variável {empresa}.">
        <div className="flex flex-col gap-5 sm:flex-row">
          <button onClick={() => arquivo.current.click()} className="group relative h-24 w-24 shrink-0 overflow-hidden rounded-full bg-brand-100">
            {profile.profile_photo ? <img src={profile.profile_photo} alt="" className="h-full w-full object-cover" /> : <span className="text-3xl font-black text-brand-700">{(profile.full_name || '?')[0]}</span>}
            <span className="absolute inset-0 flex items-center justify-center bg-black/40 text-white opacity-0 transition group-hover:opacity-100"><Camera className="h-6 w-6" /></span>
          </button>
          <input ref={arquivo} type="file" accept="image/*" className="hidden" onChange={foto} />
          <div className="grid flex-1 gap-4 sm:grid-cols-2">
            <Field label="Seu nome"><Input value={f.full_name} onChange={(e) => setF({ ...f, full_name: e.target.value })} /></Field>
            <Field label="Nome da empresa"><Input value={f.empresa} onChange={(e) => setF({ ...f, empresa: e.target.value })} /></Field>
            <Field label="E-mail"><Input value={profile.email || ''} disabled /></Field>
            <div className="flex items-end"><Button onClick={async () => { await updateProfile(f); toast('Perfil salvo'); }}>Salvar perfil</Button></div>
          </div>
        </div>
      </Secao>
      <Secao icon={Plug} title="Integração com WhatsApp (W-API)" desc="O envio automático usa a W-API pelo servidor.">
        <div className="space-y-2 text-sm text-slate-600">
          {demo ? (
            <p className="rounded-xl bg-amber-50 p-3 text-amber-800">Você está no modo demonstração. Para enviar de verdade, conecte o Supabase (veja o README do projeto) e configure os segredos abaixo.</p>
          ) : (
            <p className="rounded-xl bg-emerald-50 p-3 text-emerald-800">Conectado ao servidor. Os envios usam os segredos configurados nas Edge Functions.</p>
          )}
          <p>Segredos necessários no Supabase (Edge Functions → Secrets): <code className="rounded bg-slate-100 px-1">WAPI_INSTANCE_ID</code>, <code className="rounded bg-slate-100 px-1">WAPI_TOKEN</code> e, para a IA, <code className="rounded bg-slate-100 px-1">ANTHROPIC_API_KEY</code>.</p>
        </div>
      </Secao>
    </>
  );
}

const CAMPOS_MSG = [
  { k: 'whatsapp_message', t: 'Cobrança / lembrete', d: 'Usada no botão de WhatsApp e na confirmação de renovação.' },
  { k: 'whatsapp_cadastro_message', t: 'Boas-vindas (cadastro)', d: 'Mostrada para copiar logo após cadastrar um cliente.' },
  { k: 'whatsapp_expirado_message', t: 'Plano expirado', d: 'Sugerida para clientes com vencimento passado.' },
];

function Mensagens() {
  const { profile, updateProfile } = useAuth();
  const { toast } = usePopup();
  const [f, setF] = useState(() => Object.fromEntries(CAMPOS_MSG.map(({ k }) => [k, profile[k] || ''])));
  const [gerando, setGerando] = useState(null);

  async function gerar(k, t) {
    setGerando(k);
    try {
      const texto = await invokeLLM({
        tipo: { whatsapp_message: 'vencimento_proximo', whatsapp_cadastro_message: 'boas_vindas', whatsapp_expirado_message: 'plano_expirado' }[k],
        prompt: `Escreva uma mensagem curta de WhatsApp (máx. 350 caracteres), simpática e profissional, do tipo "${t}". Use as variáveis {{cliente}}, {{plano}}, {{vencimento}}, {{valor}} e {empresa} quando fizer sentido. Responda só com o texto.`,
      });
      setF((x) => ({ ...x, [k]: texto.trim() }));
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setGerando(null);
    }
  }

  return (
    <Secao icon={MessageCircle} title="Mensagens personalizadas do WhatsApp" desc="Variáveis: {{cliente}}, {{plano}}, {{vencimento}}, {{valor}}, {empresa}">
      <div className="space-y-6">
        {CAMPOS_MSG.map(({ k, t, d }) => (
          <div key={k} className="grid gap-3 lg:grid-cols-2">
            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <span className="label !mb-0">{t}</span>
                <Button size="sm" variant="ghost" icon={Sparkles} loading={gerando === k} onClick={() => gerar(k, t)}>Gerar com IA</Button>
              </div>
              <Textarea className="min-h-[130px]" value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} />
              <p className="mt-1 text-xs text-slate-400">{d}</p>
            </div>
            <div>
              <span className="label">Pré-visualização</span>
              <div className="rounded-2xl bg-[#efeae2] p-3">
                <p className="whitespace-pre-line rounded-2xl rounded-tr-sm bg-[#dcf8c6] p-3 text-sm text-slate-800 shadow-sm">
                  {aplicarTemplate(f[k], { ...EXEMPLO, empresa: profile.empresa })}
                </p>
              </div>
            </div>
          </div>
        ))}
        <Button onClick={async () => { await updateProfile(f); toast('Mensagens salvas'); }}>Salvar mensagens</Button>
      </div>
    </Secao>
  );
}

function Painel() {
  const { profile, updateProfile } = useAuth();
  const { toast } = usePopup();
  const [s, setS] = useState({ notificar_vencimentos: true, dias_antecedencia: 3, dias_atraso: 7, ...profile.dashboard_settings });
  return (
    <Secao icon={Bell} title="Notificações de vencimento" desc="Controla o sino no topo da tela.">
      <div className="space-y-4">
        <Toggle checked={s.notificar_vencimentos} onChange={(v) => setS({ ...s, notificar_vencimentos: v })} label="Mostrar clientes a vencer no sino" />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Avisar com quantos dias de antecedência">
            <Select value={s.dias_antecedencia} onChange={(e) => setS({ ...s, dias_antecedencia: Number(e.target.value) })}>{[0, 1, 2, 3, 5, 7, 10, 15].map((n) => <option key={n} value={n}>{n === 0 ? 'Só no dia' : `${n} dia(s)`}</option>)}</Select>
          </Field>
          <Field label="Mostrar atrasados de até">
            <Select value={s.dias_atraso} onChange={(e) => setS({ ...s, dias_atraso: Number(e.target.value) })}>{[0, 3, 7, 15, 30, 90].map((n) => <option key={n} value={n}>{n === 0 ? 'Não mostrar' : `${n} dia(s)`}</option>)}</Select>
          </Field>
        </div>
        <Button onClick={async () => { await updateProfile({ dashboard_settings: s }); toast('Preferências salvas'); }}>Salvar</Button>
      </div>
    </Secao>
  );
}

function Seguranca() {
  const { profile, updateProfile } = useAuth();
  const { toast } = usePopup();
  const [pin, setPin] = useState('');
  const [conf, setConf] = useState('');
  const ativo = profile.pin_lock_enabled;

  async function ativar() {
    if (!/^\d{4,6}$/.test(pin)) return toast('O PIN precisa ter de 4 a 6 números', 'error');
    if (pin !== conf) return toast('Os PINs não conferem', 'error');
    await updateProfile({ pin_lock_enabled: true, pin_lock_code: pin });
    sessionStorage.setItem('gs_pin_ok', '1');
    setPin(''); setConf('');
    toast('Bloqueio por PIN ativado');
  }

  return (
    <Secao icon={Lock} title="Bloqueio por PIN" desc="Pede um PIN sempre que o app for aberto neste aparelho.">
      {ativo ? (
        <div className="flex flex-wrap items-center gap-3">
          <p className="flex-1 text-sm text-emerald-700">✓ O bloqueio está ativo.</p>
          <Button variant="danger" onClick={async () => { await updateProfile({ pin_lock_enabled: false, pin_lock_code: null }); toast('Bloqueio desativado'); }}>Desativar PIN</Button>
        </div>
      ) : (
        <div className="grid max-w-md gap-4 sm:grid-cols-2">
          <Field label="Novo PIN"><Input type="password" inputMode="numeric" maxLength={6} value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} /></Field>
          <Field label="Confirme o PIN"><Input type="password" inputMode="numeric" maxLength={6} value={conf} onChange={(e) => setConf(e.target.value.replace(/\D/g, ''))} /></Field>
          <div className="sm:col-span-2"><Button onClick={ativar}>Ativar bloqueio</Button></div>
        </div>
      )}
    </Secao>
  );
}

function Exportar() {
  const [carregando, setCarregando] = useState(null);

  async function exportar(tipo, formato) {
    setCarregando(tipo + formato);
    try {
      const XLSX = await import('xlsx');
      let linhas;
      if (tipo === 'clientes') {
        const [clientes, planos] = await Promise.all([db.Cliente.list('nome'), db.Plano.list()]);
        const pp = new Map(planos.map((p) => [p.id, p]));
        linhas = clientes.map((c) => ({
          Nome: c.nome, WhatsApp: c.whatsapp, Plano: c.plano_nome, Vencimento: formatarDataBR(c.data_vencimento), Conexões: c.quantidade,
          Desconto: Number(c.desconto), Valor: Number(pp.get(c.plano_id)?.preco || 0) * c.quantidade - Number(c.desconto),
          Status: STATUS_CLIENTE[c.status]?.label, Essencial: c.essencial ? 'Sim' : 'Não', Notas: c.notas || '',
        }));
      } else if (tipo === 'mensalidades') {
        linhas = (await db.Mensalidade.list('-data_pagamento')).map((m) => ({
          Cliente: m.cliente_nome, Plano: m.plano_nome, Valor: Number(m.valor), Custo: Number(m.custo || 0), Vencimento: formatarDataBR(m.data_vencimento),
          Pagamento: formatarDataBR(m.data_pagamento), Status: m.status, Método: METODOS[m.metodo_pagamento] || m.metodo_pagamento, Descrição: m.descricao || '',
        }));
      } else {
        linhas = (await db.HistoricoMensagem.list('-data_envio')).map((h) => ({
          Data: new Date(h.data_envio).toLocaleString('pt-BR'), Cliente: h.cliente_nome, WhatsApp: h.whatsapp, Tipo: h.tipo_mensagem, Status: h.status, Erro: h.erro || '', Mensagem: h.mensagem,
        }));
      }
      const ws = XLSX.utils.json_to_sheet(linhas);
      if (formato === 'csv') baixarArquivo(`${tipo}-${hojeISO()}.csv`, '﻿' + XLSX.utils.sheet_to_csv(ws, { FS: ';' }), 'text/csv;charset=utf-8');
      else {
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, tipo);
        XLSX.writeFile(wb, `${tipo}-${hojeISO()}.xlsx`);
      }
    } finally {
      setCarregando(null);
    }
  }

  return (
    <Secao icon={FileSpreadsheet} title="Exportar relatórios" desc="Baixe seus dados em Excel ou CSV. O relatório financeiro em PDF fica em Financeiro.">
      <div className="grid gap-3 sm:grid-cols-3">
        {[['clientes', 'Clientes'], ['mensalidades', 'Pagamentos'], ['historico', 'Mensagens enviadas']].map(([k, l]) => (
          <div key={k} className="rounded-2xl border border-slate-100 p-4">
            <p className="mb-3 font-bold">{l}</p>
            <div className="flex gap-2">
              <Button size="sm" loading={carregando === k + 'xlsx'} onClick={() => exportar(k, 'xlsx')}>Excel</Button>
              <Button size="sm" variant="secondary" loading={carregando === k + 'csv'} onClick={() => exportar(k, 'csv')}>CSV</Button>
            </div>
          </div>
        ))}
      </div>
    </Secao>
  );
}

function Suporte() {
  const [aberta, setAberta] = useState(null);
  const [pergunta, setPergunta] = useState('');
  const [conversa, setConversa] = useState([]);
  const [pensando, setPensando] = useState(false);
  const [ouvindo, setOuvindo] = useState(false);
  const rec = useRef(null);
  const SR = typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition);

  useEffect(() => () => rec.current?.abort(), []);

  async function perguntar(texto = pergunta, porVoz = false) {
    const q = texto.trim();
    if (!q) return;
    setPergunta('');
    setConversa((c) => [...c, { de: 'eu', texto: q }]);
    setPensando(true);
    try {
      const faq = FAQ.map(([p, r]) => `P: ${p}\nR: ${r}`).join('\n');
      const resposta = await invokeLLM({ prompt: `Base de conhecimento do app:\n${faq}\n\nPergunta do usuário: ${q}` });
      setConversa((c) => [...c, { de: 'ia', texto: resposta }]);
      if (porVoz && 'speechSynthesis' in window) {
        const u = new SpeechSynthesisUtterance(resposta);
        u.lang = 'pt-BR';
        speechSynthesis.speak(u);
      }
    } catch (e) {
      setConversa((c) => [...c, { de: 'ia', texto: 'Erro: ' + e.message }]);
    } finally {
      setPensando(false);
    }
  }

  function voz() {
    if (!SR) return;
    if (ouvindo) { rec.current?.stop(); return; }
    const r = new SR();
    r.lang = 'pt-BR';
    r.interimResults = false;
    r.onresult = (e) => perguntar(e.results[0][0].transcript, true);
    r.onend = () => setOuvindo(false);
    r.onerror = () => setOuvindo(false);
    rec.current = r;
    setOuvindo(true);
    r.start();
  }

  return (
    <>
      <Secao icon={Sparkles} title="Assistente" desc="Pergunte por texto ou por voz como usar o Gestor Simples.">
        <div className="mb-3 max-h-80 space-y-2 overflow-y-auto">
          {conversa.length === 0 && <p className="text-sm text-slate-400">Ex.: “Como cobro quem está atrasado?”</p>}
          {conversa.map((m, i) => (
            <div key={i} className={cn('max-w-[85%] whitespace-pre-line rounded-2xl px-3 py-2 text-sm', m.de === 'eu' ? 'ml-auto bg-brand-600 text-white' : 'bg-slate-100 text-slate-800')}>{m.texto}</div>
          ))}
          {pensando && <div className="w-16 rounded-2xl bg-slate-100 px-3 py-2 text-sm text-slate-400">…</div>}
        </div>
        <div className="flex gap-2">
          <Input value={pergunta} onChange={(e) => setPergunta(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && perguntar()} placeholder="Digite sua dúvida…" />
          {SR && <Button variant={ouvindo ? 'danger' : 'secondary'} icon={ouvindo ? MicOff : Mic} onClick={voz} title="Falar" />}
          <Button icon={Send} onClick={() => perguntar()} loading={pensando} />
        </div>
      </Secao>
      <Secao icon={HelpCircle} title="Perguntas frequentes">
        <div className="divide-y divide-slate-100">
          {FAQ.map(([p, r], i) => (
            <div key={p}>
              <button onClick={() => setAberta(aberta === i ? null : i)} className="flex w-full items-center justify-between py-3 text-left text-sm font-semibold text-slate-800">
                {p}<ChevronDown className={cn('h-4 w-4 shrink-0 transition', aberta === i && 'rotate-180')} />
              </button>
              {aberta === i && <p className="pb-3 text-sm text-slate-600">{r}</p>}
            </div>
          ))}
        </div>
      </Secao>
    </>
  );
}
