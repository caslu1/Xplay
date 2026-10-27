package com.xplay.mobile;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runs work off the main thread and delivers the result to a still-alive activity. */
public final class Task {

    public interface Done<T> {
        void ok(T value);
        void fail(Exception e);
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    public static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Task() {}

    public static <T> void run(final Activity activity, final Callable<T> work, final Done<T> done) {
        POOL.execute(new Runnable() {
            public void run() {
                T result = null;
                Exception error = null;
                try {
                    result = work.call();
                } catch (Exception e) {
                    error = e;
                }
                final T r = result;
                final Exception err = error;
                MAIN.post(new Runnable() {
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (err != null) done.fail(err);
                        else done.ok(r);
                    }
                });
            }
        });
    }

    public static String message(Exception e) {
        String m = e.getMessage();
        if (e instanceof java.net.UnknownHostException) return "Servidor não encontrado (confira o DNS)";
        if (e instanceof java.net.SocketTimeoutException) return "O servidor demorou para responder";
        if (e instanceof java.net.ConnectException) return "Não foi possível conectar ao servidor";
        if (e instanceof javax.net.ssl.SSLException) return "Falha na conexão segura (tente http://)";
        return m == null ? e.getClass().getSimpleName() : m;
    }
}
