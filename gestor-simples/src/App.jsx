import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import { Spinner } from './components/ui';
import LandingPage from './pages/LandingPage';
import Login from './pages/Login';

const Dashboard = lazy(() => import('./pages/Dashboard'));
const Clientes = lazy(() => import('./pages/Clientes'));
const Planos = lazy(() => import('./pages/Planos'));
const Faturamento = lazy(() => import('./pages/Faturamento'));
const Metas = lazy(() => import('./pages/Metas'));
const NotasAdesivas = lazy(() => import('./pages/NotasAdesivas'));
const MensagensAutomaticas = lazy(() => import('./pages/MensagensAutomaticas'));
const Tarefas = lazy(() => import('./pages/Tarefas'));
const Configuracoes = lazy(() => import('./pages/Configuracoes'));
const ImportarClientes = lazy(() => import('./pages/ImportarClientes'));
const BackupDados = lazy(() => import('./pages/BackupDados'));

function Protegido({ children }) {
  const { user, profile, loading } = useAuth();
  const loc = useLocation();
  if (loading || (user && !profile)) return <Spinner className="min-h-screen" />;
  if (!user) return <Navigate to="/Login" replace state={{ de: loc.pathname }} />;
  return children;
}

export default function App() {
  const { user, loading } = useAuth();
  return (
    <Suspense fallback={<Spinner className="min-h-screen" />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/Login" element={!loading && user ? <Navigate to="/Dashboard" replace /> : <Login />} />
        <Route element={<Protegido><Layout /></Protegido>}>
          <Route path="/Dashboard" element={<Dashboard />} />
          <Route path="/Clientes" element={<Clientes />} />
          <Route path="/Planos" element={<Planos />} />
          <Route path="/Faturamento" element={<Faturamento />} />
          <Route path="/Metas" element={<Metas />} />
          <Route path="/NotasAdesivas" element={<NotasAdesivas />} />
          <Route path="/MensagensAutomaticas" element={<MensagensAutomaticas />} />
          <Route path="/Tarefas" element={<Tarefas />} />
          <Route path="/Configuracoes" element={<Configuracoes />} />
          <Route path="/ImportarClientes" element={<ImportarClientes />} />
          <Route path="/BackupDados" element={<BackupDados />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
