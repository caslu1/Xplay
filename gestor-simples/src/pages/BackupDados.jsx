import { useRef, useState } from 'react';
import { DatabaseBackup, Download, Upload, ShieldCheck, AlertTriangle } from 'lucide-react';
import { db, TABELAS } from '../lib/db';
import { useAuth } from '../context/AuthContext';
import { usePopup } from '../context/PopupContext';
import { baixarArquivo, hojeISO } from '../lib/utils';
import { Button, Card, PageHeader } from '../components/ui';
import { useLocalState } from '../lib/hooks';

const NOMES = { Cliente: 'Clientes', Plano: 'Planos', Mensalidade: 'Pagamentos', ConfiguracaoMensagem: 'Mensagens automáticas', HistoricoMensagem: 'Histórico de mensagens', NotaAdesiva: 'Notas', Tarefa: 'Tarefas', Meta: 'Metas' };
// Ordem de restauração: planos antes dos clientes, clientes antes dos pagamentos.
const ORDEM = ['Plano', 'Cliente', 'Mensalidade', 'ConfiguracaoMensagem', 'HistoricoMensagem', 'NotaAdesiva', 'Tarefa', 'Meta'];

export default function BackupDados() {
  const { profile, updateProfile } = useAuth();
  const { confirm, toast } = usePopup();
  const [ultimo, setUltimo] = useLocalState('gs_ultimo_backup', null);
  const [carregando, setCarregando] = useState(false);
  const [contagem, setContagem] = useState(null);
  const input = useRef(null);

  async function gerar() {
    setCarregando(true);
    try {
      const dados = {};
      for (const nome of ORDEM) dados[nome] = await db[nome].list('created_date', { force: true });
      const { activity_logs, pin_lock_code, ...perfil } = profile;
      const backup = { app: 'gestor-simples', versao: 1, gerado_em: new Date().toISOString(), perfil, dados };
      baixarArquivo(`backup-gestor-simples-${hojeISO()}.json`, JSON.stringify(backup, null, 2));
      setUltimo(new Date().toISOString());
      setContagem(Object.fromEntries(ORDEM.map((n) => [n, dados[n].length])));
      toast('Backup gerado');
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setCarregando(false);
    }
  }

  async function restaurar(file) {
    let backup;
    try {
      backup = JSON.parse(await file.text());
      if (backup.app !== 'gestor-simples' || !backup.dados) throw new Error();
    } catch {
      toast('Arquivo de backup inválido', 'error');
      return;
    }
    const total = Object.values(backup.dados).reduce((s, l) => s + (l?.length || 0), 0);
    const ok = await confirm({
      title: 'Restaurar backup?',
      message: `${total} registros de ${new Date(backup.gerado_em).toLocaleString('pt-BR')}.\nRegistros com o mesmo ID serão substituídos; os demais dados atuais são mantidos.`,
      confirmText: 'Restaurar',
    });
    if (!ok) return;
    setCarregando(true);
    try {
      for (const nome of ORDEM) {
        const linhas = (backup.dados[nome] || []).map(({ created_by, ...r }) => r);
        if (linhas.length) await db[nome].upsert(linhas);
      }
      if (backup.perfil) {
        const { id, email, role, ...resto } = backup.perfil;
        await updateProfile(resto);
      }
      toast('Backup restaurado');
    } catch (e) {
      toast('Erro ao restaurar: ' + e.message, 'error');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div>
      <PageHeader icon={DatabaseBackup} title="Backup dos dados" subtitle="Guarde uma cópia de tudo e restaure quando precisar" />
      <div className="grid gap-5 lg:grid-cols-2">
        <Card className="p-6">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-100 text-brand-700"><Download className="h-6 w-6" /></div>
          <h2 className="text-lg font-bold">Gerar backup</h2>
          <p className="mb-4 mt-1 text-sm text-slate-500">Baixa um arquivo .json com {Object.keys(TABELAS).length} tipos de dados: clientes, planos, pagamentos, mensagens, notas, tarefas, metas e suas configurações.</p>
          <Button icon={Download} loading={carregando} onClick={gerar}>Baixar backup agora</Button>
          {ultimo && <p className="mt-3 flex items-center gap-1.5 text-xs text-emerald-600"><ShieldCheck className="h-4 w-4" /> Último backup neste aparelho: {new Date(ultimo).toLocaleString('pt-BR')}</p>}
          {contagem && (
            <ul className="mt-4 grid grid-cols-2 gap-2 text-sm">
              {ORDEM.map((n) => <li key={n} className="flex justify-between rounded-xl bg-slate-50 px-3 py-2"><span className="text-slate-500">{NOMES[n]}</span><b>{contagem[n]}</b></li>)}
            </ul>
          )}
        </Card>
        <Card className="p-6">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700"><Upload className="h-6 w-6" /></div>
          <h2 className="text-lg font-bold">Restaurar backup</h2>
          <p className="mb-4 mt-1 text-sm text-slate-500">Envie um arquivo gerado aqui. Útil para trocar de aparelho, de conta ou voltar dados apagados.</p>
          <Button variant="secondary" icon={Upload} loading={carregando} onClick={() => input.current.click()}>Escolher arquivo</Button>
          <input ref={input} type="file" accept=".json,application/json" className="hidden" onChange={(e) => { if (e.target.files[0]) restaurar(e.target.files[0]); e.target.value = ''; }} />
          <p className="mt-4 flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-xs text-amber-800"><AlertTriangle className="h-4 w-4 shrink-0" /> Faça backups com frequência e guarde o arquivo em local seguro (ele contém os dados dos seus clientes).</p>
        </Card>
      </div>
    </div>
  );
}
