package com.xplay.mobile;

import java.io.IOException;

/** Signs in with the saved login and loads live channels, movies and series. */
public final class Catalog {

    public interface Progress {
        void step(String what);
    }

    public interface Listener {
        /** error is null when everything loaded. */
        void loaded(Exception error);
    }

    private static boolean loading;
    private static Exception lastError;
    private static String status = "";
    private static final java.util.List<Listener> LISTENERS = new java.util.ArrayList<Listener>();
    private static Progress statusListener;

    private Catalog() {}

    /** Starts loading the saved login in the background unless it is loaded or loading. Main thread. */
    public static void start() {
        if (loading || Repo.catalogsLoaded()) return;
        loading = true;
        lastError = null;
        new Thread(new Runnable() {
            public void run() {
                Exception err = null;
                try {
                    loadAll(new Progress() {
                        public void step(final String what) {
                            Task.MAIN.post(new Runnable() {
                                public void run() {
                                    status = what;
                                    if (statusListener != null) statusListener.step(what);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    err = e;
                }
                final Exception error = err;
                Task.MAIN.post(new Runnable() {
                    public void run() {
                        loading = false;
                        lastError = error;
                        if (error != null) Repo.clearCatalogs();
                        java.util.List<Listener> l = new java.util.ArrayList<Listener>(LISTENERS);
                        LISTENERS.clear();
                        for (Listener x : l) x.loaded(error);
                    }
                });
            }
        }, "catalog").start();
    }

    /** Calls back once loading ends (right away when already loaded or failed). Main thread. */
    public static void whenLoaded(Listener l) {
        if (Repo.catalogsLoaded()) {
            l.loaded(null);
        } else if (loading) {
            LISTENERS.add(l);
        } else if (lastError != null) {
            l.loaded(lastError);
        } else {
            LISTENERS.add(l);
            start();
        }
    }

    public static void forget(Listener l) {
        LISTENERS.remove(l);
    }

    public static boolean loading() {
        return loading;
    }

    public static String status() {
        return status;
    }

    public static void onStatus(Progress p) {
        statusListener = p;
    }

    /** Drops the catalogs and loads them again. */
    public static void reload() {
        Repo.clearCatalogs();
        lastError = null;
        start();
    }

    /** Worker thread. */
    public static void loadAll(Progress p) throws IOException {
        if (!Repo.isXtream()) {
            // M3U links from Xtream servers carry the login; the API gives covers, plots and seasons.
            String m3u = Repo.m3uUrl;
            String[] x = M3u.xtreamCredentials(m3u);
            if (x != null) {
                Repo.saveXtream(x[0], x[1], x[2]);
                try {
                    loadXtream(p);
                    return;
                } catch (IOException e) {
                    Repo.saveM3u(m3u);
                }
            }
            p.step("Baixando a lista…");
            M3u.load(Repo.m3uUrl);
            return;
        }
        loadXtream(p);
    }

    private static void loadXtream(Progress p) throws IOException {
        p.step("Conectando ao servidor…");
        Xtream.login();
        p.step("Carregando filmes e séries…");
        final IOException[] errors = new IOException[3];
        // The three lists load side by side: big catalogs take a while on slow servers.
        Thread series = new Thread(new Runnable() {
            public void run() {
                try {
                    Xtream.loadSeries();
                } catch (IOException e) {
                    errors[1] = e;
                }
            }
        });
        Thread live = new Thread(new Runnable() {
            public void run() {
                try {
                    Xtream.loadLive();
                } catch (IOException e) {
                    errors[2] = e;
                }
            }
        });
        series.start();
        live.start();
        try {
            Xtream.loadMovies();
        } catch (IOException e) {
            errors[0] = e;
        }
        try {
            series.join();
            live.join();
        } catch (InterruptedException e) {
            throw new IOException("Interrompido");
        }
        // A server without movies, series or channels still opens with what it has.
        if (Repo.movies == null) Repo.movies = new java.util.ArrayList<VodItem>();
        if (Repo.movieCategories == null) Repo.movieCategories = new java.util.LinkedHashMap<String, String>();
        if (Repo.series == null) Repo.series = new java.util.ArrayList<VodItem>();
        if (Repo.seriesCategories == null) Repo.seriesCategories = new java.util.LinkedHashMap<String, String>();
        if (Repo.channels == null) Repo.channels = new java.util.ArrayList<Channel>();
        if (Repo.liveGroups == null) Repo.liveGroups = new java.util.ArrayList<String>();
        if (Repo.movies.isEmpty() && Repo.series.isEmpty() && Repo.channels.isEmpty()) {
            for (IOException e : errors) if (e != null) throw e;
            throw new IOException("O servidor não tem canais, filmes nem séries");
        }
    }
}
