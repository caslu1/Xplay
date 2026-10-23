import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, ArrowLeft } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Button, Input, Field } from '../components/ui';
import Logo from '../components/Logo';

export default function Login() {
  const { login, cadastrar, recuperarSenha, demo } = useAuth();
  const [modo, setModo] = useState('entrar');
  const [form, setForm] = useState({ nome: '', email: '', senha: '' });
  const [erro, setErro] = useState('');
  const [info, setInfo] = useState('');
  const [carregando, setCarregando] = useState(false);
  const nav = useNavigate();
  const loc = useLocation();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function enviar(e) {
    e.preventDefault();
    setErro('');
    setInfo('');
    setCarregando(true);
    try {
      if (modo === 'entrar') {
        await login(form.email, form.senha);
        nav(loc.state?.de || '/Dashboard', { replace: true });
      } else if (modo === 'cadastrar') {
        const r = await cadastrar(form.email, form.senha, form.nome);
        if (r.confirmar) {
          setInfo('Conta criada! Enviamos um link de confirmação para o seu e-mail.');
          setModo('entrar');
        } else nav('/Dashboard', { replace: true });
      } else {
        await recuperarSenha(form.email);
        setInfo('Se o e-mail existir, você receberá um link para redefinir a senha.');
        setModo('entrar');
      }
    } catch (e2) {
      setErro(e2.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="flex min-h-screen">
      <div className="relative hidden flex-1 flex-col justify-between overflow-hidden bg-gradient-to-br from-brand-700 via-brand-600 to-fuchsia-600 p-12 text-white lg:flex">
        <Logo light />
        <div>
          <h2 className="text-4xl font-black leading-tight">Seus clientes e mensalidades,<br />no piloto automático.</h2>
          <p className="mt-4 max-w-md text-brand-100">Cadastre, renove, cobre pelo WhatsApp e acompanhe o lucro — tudo em um só lugar, no celular ou no computador.</p>
        </div>
        <p className="text-sm text-brand-200">© {new Date().getFullYear()} Gestor Simples</p>
        <div className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-white/10" />
        <div className="absolute -bottom-24 right-20 h-80 w-80 rounded-full bg-fuchsia-400/20" />
      </div>
      <div className="flex flex-1 items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <Link to="/" className="mb-8 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-brand-600"><ArrowLeft className="h-4 w-4" /> Voltar</Link>
          <div className="mb-6 lg:hidden"><Logo /></div>
          <h1 className="text-2xl font-extrabold text-slate-900">
            {modo === 'entrar' ? 'Entrar na sua conta' : modo === 'cadastrar' ? 'Criar conta grátis' : 'Recuperar senha'}
          </h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">
            {demo ? 'Modo demonstração: entre sem senha e explore com dados de exemplo.' : 'Bem-vindo de volta! Use seu e-mail e senha.'}
          </p>
          {demo ? (
            <Button size="lg" className="w-full" loading={carregando} onClick={enviar}>Entrar na demonstração</Button>
          ) : (
            <form onSubmit={enviar} className="space-y-4">
              {modo === 'cadastrar' && (
                <Field label="Nome">
                  <div className="relative"><User className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><Input className="pl-9" required value={form.nome} onChange={set('nome')} placeholder="Seu nome" /></div>
                </Field>
              )}
              <Field label="E-mail">
                <div className="relative"><Mail className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><Input className="pl-9" type="email" required value={form.email} onChange={set('email')} placeholder="voce@email.com" autoComplete="email" /></div>
              </Field>
              {modo !== 'recuperar' && (
                <Field label="Senha">
                  <div className="relative"><Lock className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><Input className="pl-9" type="password" required minLength={6} value={form.senha} onChange={set('senha')} placeholder="••••••" autoComplete={modo === 'entrar' ? 'current-password' : 'new-password'} /></div>
                </Field>
              )}
              {erro && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-600">{erro}</p>}
              {info && <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}
              <Button size="lg" type="submit" className="w-full" loading={carregando}>
                {modo === 'entrar' ? 'Entrar' : modo === 'cadastrar' ? 'Criar conta' : 'Enviar link'}
              </Button>
              <div className="flex justify-between text-sm">
                {modo === 'entrar' ? (
                  <>
                    <button type="button" className="font-semibold text-brand-600" onClick={() => setModo('cadastrar')}>Criar conta</button>
                    <button type="button" className="text-slate-500" onClick={() => setModo('recuperar')}>Esqueci a senha</button>
                  </>
                ) : (
                  <button type="button" className="font-semibold text-brand-600" onClick={() => setModo('entrar')}>Já tenho conta</button>
                )}
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
