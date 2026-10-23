import { useCallback, useEffect, useRef, useState } from 'react';
import { db, DATA_EVENT } from './db';

/**
 * Carrega várias entidades e recarrega quando alguma delas muda.
 *   const { data, loading, reload } = useEntities(['Cliente', 'Plano']);
 *   data.Cliente, data.Plano
 */
export function useEntities(nomes) {
  const chave = nomes.join(',');
  const [data, setData] = useState(() => Object.fromEntries(nomes.map((n) => [n, []])));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const vivo = useRef(true);

  const reload = useCallback(async (force = false) => {
    try {
      const lista = chave.split(',');
      const res = await Promise.all(lista.map((n) => db[n].list('-created_date', { force })));
      if (vivo.current) {
        setData(Object.fromEntries(lista.map((n, i) => [n, res[i]])));
        setError(null);
      }
    } catch (e) {
      if (vivo.current) setError(e);
    } finally {
      if (vivo.current) setLoading(false);
    }
  }, [chave]);

  useEffect(() => {
    vivo.current = true;
    reload();
    const tabelas = new Set(chave.split(',').map((n) => db[n].tabela));
    const onChange = (e) => tabelas.has(e.detail?.tabela) && reload();
    window.addEventListener(DATA_EVENT, onChange);
    return () => {
      vivo.current = false;
      window.removeEventListener(DATA_EVENT, onChange);
    };
  }, [chave, reload]);

  return { data, loading, error, reload };
}

export function useLocalState(chave, inicial) {
  const [v, setV] = useState(() => {
    try {
      const s = localStorage.getItem(chave);
      return s == null ? inicial : JSON.parse(s);
    } catch {
      return inicial;
    }
  });
  useEffect(() => {
    try {
      localStorage.setItem(chave, JSON.stringify(v));
    } catch { /* sem storage */ }
  }, [chave, v]);
  return [v, setV];
}
