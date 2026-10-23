import { createContext, useCallback, useContext, useRef, useState } from 'react';
import GlobalPopup from '../components/GlobalPopup';

const PopupContext = createContext(null);

/**
 * Popups globais: confirm(), alert(), prompt() que devolvem Promise, e toast().
 *   const { confirm, toast } = usePopup();
 *   if (await confirm({ title: 'Excluir?', danger: true })) ...
 */
export function PopupProvider({ children }) {
  const [popup, setPopup] = useState(null);
  const [toasts, setToasts] = useState([]);
  const resolver = useRef(null);

  const abrir = useCallback((cfg) => new Promise((resolve) => {
    resolver.current = resolve;
    setPopup(cfg);
  }), []);

  const fechar = (valor) => {
    setPopup(null);
    resolver.current?.(valor);
    resolver.current = null;
  };

  const toast = useCallback((mensagem, tipo = 'success', acao) => {
    const id = Math.random();
    setToasts((t) => [...t, { id, mensagem, tipo, acao }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), acao ? 6000 : 3200);
  }, []);

  const value = {
    confirm: (cfg) => abrir({ kind: 'confirm', ...(typeof cfg === 'string' ? { title: cfg } : cfg) }),
    alert: (cfg) => abrir({ kind: 'alert', ...(typeof cfg === 'string' ? { title: cfg } : cfg) }),
    prompt: (cfg) => abrir({ kind: 'prompt', ...(typeof cfg === 'string' ? { title: cfg } : cfg) }),
    toast,
  };

  return (
    <PopupContext.Provider value={value}>
      {children}
      <GlobalPopup popup={popup} onClose={fechar} toasts={toasts} dismissToast={(id) => setToasts((t) => t.filter((x) => x.id !== id))} />
    </PopupContext.Provider>
  );
}

export const usePopup = () => useContext(PopupContext);
