import { createContext, useContext, useEffect, useState } from 'react';
import { supabase, DEMO } from '../lib/supabase';
import { carregarPerfil, atualizarPerfil, limparAtividadesAntigas, PROFILE_EVENT } from '../lib/profile';
import { limparCache } from '../lib/db';
import { popularDemo } from '../lib/seed';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  async function entrarCom(u) {
    if (!u) {
      setUser(null);
      setProfile(null);
      return;
    }
    setUser(u);
    const p = await carregarPerfil(u);
    setProfile(p);
    limparAtividadesAntigas();
  }

  useEffect(() => {
    const onProfile = (e) => setProfile({ ...e.detail });
    window.addEventListener(PROFILE_EVENT, onProfile);

    if (DEMO) {
      if (localStorage.getItem('gs_demo_logged')) {
        popularDemo().then(() => entrarCom({ id: 'demo-user', email: 'demo@gestorsimples.app' })).finally(() => setLoading(false));
      } else setLoading(false);
      return () => window.removeEventListener(PROFILE_EVENT, onProfile);
    }

    supabase.auth.getSession().then(({ data }) => entrarCom(data.session?.user).finally(() => setLoading(false)));
    const { data: sub } = supabase.auth.onAuthStateChange((evento, session) => {
      if (evento === 'SIGNED_IN' || evento === 'SIGNED_OUT') {
        limparCache();
        entrarCom(session?.user);
      }
    });
    return () => {
      sub.subscription.unsubscribe();
      window.removeEventListener(PROFILE_EVENT, onProfile);
    };
  }, []);

  const value = {
    user,
    profile,
    loading,
    demo: DEMO,
    isAdmin: profile?.role === 'admin',
    async login(email, senha) {
      if (DEMO) {
        localStorage.setItem('gs_demo_logged', '1');
        await popularDemo();
        return entrarCom({ id: 'demo-user', email: 'demo@gestorsimples.app' });
      }
      const { error } = await supabase.auth.signInWithPassword({ email, password: senha });
      if (error) throw new Error(traduzir(error.message));
    },
    async cadastrar(email, senha, nome) {
      const { data, error } = await supabase.auth.signUp({ email, password: senha, options: { data: { full_name: nome } } });
      if (error) throw new Error(traduzir(error.message));
      return { confirmar: !data.session };
    },
    async recuperarSenha(email) {
      const { error } = await supabase.auth.resetPasswordForEmail(email, { redirectTo: `${location.origin}/Configuracoes` });
      if (error) throw new Error(traduzir(error.message));
    },
    async logout() {
      if (DEMO) {
        localStorage.removeItem('gs_demo_logged');
        setUser(null);
        setProfile(null);
        return;
      }
      await supabase.auth.signOut();
    },
    updateProfile: atualizarPerfil,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

function traduzir(msg) {
  const m = String(msg);
  if (/Invalid login/i.test(m)) return 'E-mail ou senha incorretos.';
  if (/already registered/i.test(m)) return 'Este e-mail já está cadastrado.';
  if (/Password should be/i.test(m)) return 'A senha precisa ter pelo menos 6 caracteres.';
  if (/Email not confirmed/i.test(m)) return 'Confirme seu e-mail antes de entrar (veja sua caixa de entrada).';
  return m;
}
