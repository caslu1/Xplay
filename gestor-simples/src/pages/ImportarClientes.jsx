import { useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileSpreadsheet, CheckCircle2, AlertCircle, Download, ClipboardPaste } from 'lucide-react';
import { useEntities } from '../lib/hooks';
import { db } from '../lib/db';
import { registrarAtividade } from '../lib/profile';
import { usePopup } from '../context/PopupContext';
import { addPeriodo, hojeISO, cn, formatarDataBR, formatarTelefone, baixarArquivo } from '../lib/utils';
import { Button, Card, Field, Select, Textarea, PageHeader, Toggle, Badge } from '../components/ui';

// Cabeçalhos aceitos para cada campo (sem acento, minúsculo).
const COLUNAS = {
  nome: ['nome', 'cliente', 'name', 'nome do cliente'],
  whatsapp: ['whatsapp', 'telefone', 'celular', 'fone', 'numero', 'phone', 'contato', 'zap'],
  plano: ['plano', 'plano nome', 'plan', 'codigo', 'codigo do plano', 'pacote'],
  data_vencimento: ['vencimento', 'data vencimento', 'data de vencimento', 'vence', 'validade', 'expira', 'data'],
  quantidade: ['quantidade', 'conexoes', 'telas', 'qtd'],
  desconto: ['desconto'],
  notas: ['notas', 'observacao', 'observacoes', 'obs', 'nota'],
  status: ['status', 'situacao'],
};
const norm = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[_\-.]/g, ' ').replace(/\s+/g, ' ').trim();

function lerData(v) {
  if (v == null || v === '') return null;
  if (typeof v === 'number') return new Date(Math.round((v - 25569) * 86400000)).toISOString().slice(0, 10); // serial do Excel
  const s = String(v).trim();
  let m = s.match(/^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2,4})/);
  if (m) {
    const y = m[3].length === 2 ? `20${m[3]}` : m[3];
    return `${y}-${m[2].padStart(2, '0')}-${m[1].padStart(2, '0')}`;
  }
  m = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return m ? `${m[1]}-${m[2]}-${m[3]}` : null;
}

export default function ImportarClientes() {
  const { data } = useEntities(['Plano', 'Cliente']);
  const { toast } = usePopup();
  const nav = useNavigate();
  const input = useRef(null);
  const [linhas, setLinhas] = useState([]);
  const [texto, setTexto] = useState('');
  const [planoPadrao, setPlanoPadrao] = useState('');
  const [criarPlanos, setCriarPlanos] = useState(true);
  const [pularDuplicados, setPularDuplicados] = useState(true);
  const [importando, setImportando] = useState(false);
  const [arquivo, setArquivo] = useState('');

  async function lerArquivo(file) {
    const XLSX = await import('xlsx');
    const wb = XLSX.read(await file.arrayBuffer(), { type: 'array' });
    setArquivo(file.name);
    setLinhas(XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { defval: '' }));
  }

  async function lerTexto() {
    const XLSX = await import('xlsx');
    const sep = texto.includes('\t') ? '\t' : texto.split('\n')[0].includes(';') ? ';' : ',';
    const wb = XLSX.read(texto, { type: 'string', FS: sep });
    setArquivo('texto colado');
    setLinhas(XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { defval: '', raw: false }));
  }

  const mapa = useMemo(() => {
    const cab = Object.keys(linhas[0] || {});
    return Object.fromEntries(Object.entries(COLUNAS).map(([campo, nomes]) => [campo, cab.find((c) => nomes.includes(norm(c))) || null]));
  }, [linhas]);

  const preview = useMemo(() => {
    const existentes = new Set(data.Cliente.map((c) => c.whatsapp.replace(/\D/g, '').slice(-9)));
    const vistos = new Set();
    return linhas.map((l) => {
      const v = (campo) => (mapa[campo] ? l[mapa[campo]] : '');
      const nome = String(v('nome')).trim();
      const whatsapp = String(v('whatsapp')).replace(/\D/g, '');
      const planoTxt = String(v('plano')).trim();
      const plano = data.Plano.find((p) => norm(p.nome) === norm(planoTxt) || p.codigo === planoTxt) || (!planoTxt && data.Plano.find((p) => p.id === planoPadrao));
      const venc = lerData(v('data_vencimento')) || addPeriodo(hojeISO(), plano?.periodo);
      const st = norm(v('status'));
      const erros = [];
      if (!nome) erros.push('sem nome');
      if (whatsapp.length < 10) erros.push('WhatsApp inválido');
      if (!plano && !(planoTxt && criarPlanos) && !planoPadrao) erros.push('plano não encontrado');
      const chave = whatsapp.slice(-9);
      const duplicado = existentes.has(chave) || vistos.has(chave);
      vistos.add(chave);
      return {
        nome, whatsapp, planoTxt, plano: plano || (planoPadrao && !planoTxt ? data.Plano.find((p) => p.id === planoPadrao) : null),
        data_vencimento: venc,
        quantidade: Math.min(10, Math.max(1, parseInt(v('quantidade'), 10) || 1)),
        desconto: Number(String(v('desconto')).replace(',', '.')) || 0,
        notas: String(v('notas') || ''),
        status: ['ativo', 'inativo', 'pendente', 'arquivado'].includes(st) ? st : 'ativo',
        erros, duplicado,
      };
    });
  }, [linhas, mapa, data.Plano, data.Cliente, planoPadrao, criarPlanos]);

  const validos = preview.filter((p) => !p.erros.length && !(pularDuplicados && p.duplicado));

  async function importar() {
    setImportando(true);
    try {
      const planosCriados = [];
      const porNome = new Map(data.Plano.map((p) => [norm(p.nome), p]));
      const resolver = async (p) => {
        if (p.plano) return p.plano;
        if (!porNome.has(norm(p.planoTxt))) {
          const usados = new Set([...data.Plano, ...planosCriados].map((x) => x.codigo));
          let cod = 1001;
          while (usados.has(String(cod))) cod++;
          const novo = await db.Plano.create({ nome: p.planoTxt, codigo: String(cod), preco: 0, custo: 0, periodo: 'mensal', status: 'ativo', cor: '#e9d5ff' });
          planosCriados.push(novo);
          porNome.set(norm(p.planoTxt), novo);
        }
        return porNome.get(norm(p.planoTxt));
      };
      const registros = [];
      for (const p of validos) {
        const plano = await resolver(p);
        registros.push({ nome: p.nome, whatsapp: p.whatsapp, plano_id: plano.id, plano_nome: plano.nome, data_vencimento: p.data_vencimento, quantidade: p.quantidade, desconto: p.desconto, notas: p.notas, status: p.status, essencial: false });
      }
      const criados = await db.Cliente.bulkCreate(registros);
      await registrarAtividade({
        acao: 'importar', entidade: 'Cliente', descricao: `${criados.length} clientes importados${planosCriados.length ? ` e ${planosCriados.length} plano(s) criado(s)` : ''}`,
        desfazer: [{ op: 'delete', entidade: 'Cliente', ids: criados.map((c) => c.id) }, ...(planosCriados.length ? [{ op: 'delete', entidade: 'Plano', ids: planosCriados.map((p) => p.id) }] : [])],
      });
      toast(`${criados.length} clientes importados`);
      nav('/Clientes');
    } catch (e) {
      toast('Erro ao importar: ' + e.message, 'error');
    } finally {
      setImportando(false);
    }
  }

  function modelo() {
    const csv = 'nome;whatsapp;plano;vencimento;quantidade;desconto;notas\nAna Souza;11999998888;Premium;10/11/2026;1;0;Cliente desde 2024\n';
    baixarArquivo('modelo-clientes.csv', '﻿' + csv, 'text/csv;charset=utf-8');
  }

  return (
    <div>
      <PageHeader icon={Upload} title="Importar clientes" subtitle="Traga sua planilha (.xlsx, .xls ou .csv) ou cole a lista"
        actions={<Button variant="secondary" icon={Download} onClick={modelo}>Baixar modelo</Button>} />

      {!linhas.length ? (
        <div className="grid gap-5 lg:grid-cols-2">
          <Card onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); const f = e.dataTransfer.files[0]; if (f) lerArquivo(f); }}
            className="flex cursor-pointer flex-col items-center justify-center border-2 border-dashed border-brand-200 bg-brand-50/40 p-10 text-center hover:bg-brand-50"
            onClick={() => input.current.click()}>
            <FileSpreadsheet className="mb-3 h-12 w-12 text-brand-500" />
            <p className="font-bold">Clique ou arraste sua planilha aqui</p>
            <p className="mt-1 text-sm text-slate-500">Colunas reconhecidas: nome, whatsapp, plano, vencimento, quantidade, desconto, notas, status</p>
            <input ref={input} type="file" accept=".xlsx,.xls,.csv,.ods" className="hidden" onChange={(e) => e.target.files[0] && lerArquivo(e.target.files[0])} />
          </Card>
          <Card className="p-5">
            <p className="mb-2 flex items-center gap-2 font-bold"><ClipboardPaste className="h-5 w-5 text-brand-600" /> Ou cole a lista</p>
            <p className="mb-3 text-sm text-slate-500">A primeira linha deve ter os nomes das colunas. Copiar direto do Excel/Google Planilhas funciona.</p>
            <Textarea className="min-h-[160px] font-mono text-xs" value={texto} onChange={(e) => setTexto(e.target.value)} placeholder={'nome;whatsapp;plano;vencimento\nAna;11999998888;Premium;10/11/2026'} />
            <Button className="mt-3" onClick={lerTexto} disabled={!texto.trim()}>Ler lista</Button>
          </Card>
        </div>
      ) : (
        <>
          <Card className="mb-4 p-5">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end">
              <div className="flex-1">
                <p className="font-bold">{arquivo}: {linhas.length} linhas</p>
                <p className="text-sm text-slate-500">
                  Colunas: {Object.entries(mapa).map(([k, v]) => <span key={k} className={cn('mr-2', v ? 'text-emerald-600' : 'text-slate-400')}>{k}{v ? ` ← “${v}”` : ' (não encontrada)'}</span>)}
                </p>
              </div>
              <Field label="Plano para linhas sem plano" className="lg:w-60">
                <Select value={planoPadrao} onChange={(e) => setPlanoPadrao(e.target.value)}>
                  <option value="">Nenhum</option>
                  {data.Plano.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
                </Select>
              </Field>
            </div>
            <div className="mt-4 flex flex-wrap gap-5">
              <Toggle checked={criarPlanos} onChange={setCriarPlanos} label="Criar planos que não existem (preço R$ 0, ajuste depois)" />
              <Toggle checked={pularDuplicados} onChange={setPularDuplicados} label="Pular WhatsApp já cadastrado" />
            </div>
          </Card>
          <Card className="mb-4 overflow-x-auto">
            <table className="w-full min-w-[720px] text-sm">
              <thead><tr className="border-b border-slate-100 text-left text-xs uppercase tracking-wide text-slate-400"><th className="px-4 py-2.5" /><th>Nome</th><th>WhatsApp</th><th>Plano</th><th>Vencimento</th><th>Qtd</th><th className="pr-4">Situação</th></tr></thead>
              <tbody>
                {preview.slice(0, 300).map((p, i) => {
                  const pulado = pularDuplicados && p.duplicado;
                  return (
                    <tr key={i} className={cn('border-b border-slate-50', (p.erros.length || pulado) && 'bg-red-50/40')}>
                      <td className="px-4 py-2">{p.erros.length || pulado ? <AlertCircle className="h-4 w-4 text-red-500" /> : <CheckCircle2 className="h-4 w-4 text-emerald-500" />}</td>
                      <td className="font-medium">{p.nome || '—'}</td>
                      <td>{formatarTelefone(p.whatsapp) || '—'}</td>
                      <td>{p.plano?.nome || (p.planoTxt ? <span className="text-amber-600">{p.planoTxt} {criarPlanos && '(novo)'}</span> : '—')}</td>
                      <td>{formatarDataBR(p.data_vencimento)}</td>
                      <td>{p.quantidade}</td>
                      <td className="pr-4">{p.erros.length ? <span className="text-xs text-red-600">{p.erros.join(', ')}</span> : pulado ? <Badge className="bg-amber-100 text-amber-700">duplicado</Badge> : <Badge className="bg-emerald-100 text-emerald-700">ok</Badge>}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {preview.length > 300 && <p className="p-3 text-center text-xs text-slate-400">Mostrando 300 de {preview.length} linhas</p>}
          </Card>
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="secondary" onClick={() => { setLinhas([]); setTexto(''); }}>Trocar arquivo</Button>
            <Button icon={Upload} loading={importando} disabled={!validos.length} onClick={importar}>Importar {validos.length} cliente(s)</Button>
          </div>
        </>
      )}
    </div>
  );
}
