import { useEffect, useState } from 'react';
import { Copy, Send, ExternalLink, CheckCircle2 } from 'lucide-react';
import { Modal, Button, Textarea, Select, Field } from './ui';
import { usePopup } from '../context/PopupContext';
import { useAuth } from '../context/AuthContext';
import { aplicarTemplate, copiar, linkWhatsApp, formatarTelefone } from '../lib/utils';
import { enviarMensagemWhatsApp } from '../lib/functions';

export const MODELOS = [
  { value: 'whatsapp_message', label: 'Cobrança / lembrete' },
  { value: 'whatsapp_cadastro_message', label: 'Boas-vindas (cadastro)' },
  { value: 'whatsapp_expirado_message', label: 'Plano expirado' },
];

/**
 * Mensagem de WhatsApp para um cliente: edita, copia, abre no WhatsApp ou envia pela W-API.
 * `sucesso` mostra o cabeçalho de "cadastro/renovação concluída".
 */
export default function MensagemModal({ open, onClose, cliente, plano, modelo = 'whatsapp_message', sucesso, tipo = 'manual' }) {
  const { profile } = useAuth();
  const { toast } = usePopup();
  const [qual, setQual] = useState(modelo);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => { if (open) setQual(modelo); }, [open, modelo]);
  useEffect(() => {
    if (open && cliente) setTexto(aplicarTemplate(profile?.[qual] || '', { cliente, plano, empresa: profile?.empresa }));
  }, [open, qual, cliente, plano, profile]);

  if (!cliente) return null;

  async function enviar() {
    setEnviando(true);
    try {
      const r = await enviarMensagemWhatsApp({ numero: cliente.whatsapp, mensagem: texto, cliente_id: cliente.id, cliente_nome: cliente.nome, tipo_mensagem: tipo });
      if (r.success) { toast('Mensagem enviada pelo WhatsApp'); onClose(); }
      else toast(r.error || 'Falha ao enviar', 'error');
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={sucesso ? sucesso : `Mensagem para ${cliente.nome}`}
      footer={<>
        <Button variant="secondary" icon={Copy} onClick={async () => { await copiar(texto); toast('Mensagem copiada'); }}>Copiar</Button>
        <a href={linkWhatsApp(cliente.whatsapp, texto)} target="_blank" rel="noreferrer"><Button variant="secondary" icon={ExternalLink}>Abrir WhatsApp</Button></a>
        <Button variant="whatsapp" icon={Send} loading={enviando} onClick={enviar}>Enviar pela API</Button>
      </>}>
      {sucesso && (
        <div className="mb-4 flex items-center gap-3 rounded-2xl bg-emerald-50 p-3 text-emerald-700">
          <CheckCircle2 className="h-6 w-6 shrink-0" />
          <p className="text-sm font-medium">Tudo certo! Copie a mensagem abaixo e envie ao cliente.</p>
        </div>
      )}
      <p className="mb-3 text-sm text-slate-500">Para: <b className="text-slate-700">{formatarTelefone(cliente.whatsapp)}</b></p>
      <Field label="Modelo" className="mb-3">
        <Select value={qual} onChange={(e) => setQual(e.target.value)}>{MODELOS.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}</Select>
      </Field>
      <Textarea className="min-h-[160px]" value={texto} onChange={(e) => setTexto(e.target.value)} />
      <p className="mt-2 text-xs text-slate-400">Edite os modelos em Configurações → Mensagens do WhatsApp.</p>
    </Modal>
  );
}
