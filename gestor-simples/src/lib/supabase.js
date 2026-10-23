import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_ANON_KEY;

/** Sem Supabase configurado o app roda em "modo demonstração" (dados no navegador). */
export const DEMO = !url || !key;
export const supabase = DEMO ? null : createClient(url, key, { auth: { persistSession: true, autoRefreshToken: true } });
