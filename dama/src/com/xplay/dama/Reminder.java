package com.xplay.dama;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Random;

/**
 * Lembrete local para voltar a jogar: agendado quando o app sai da tela,
 * cancelado quando ele volta. Não usa internet.
 */
public class Reminder extends BroadcastReceiver {
    private static final String CHANNEL = "lembretes";
    private static final long DAY = 24L * 60 * 60 * 1000;
    /** Intervalo de cada lembrete, contado a partir do anterior. Depois do último, para. */
    private static final long[] DELAYS = {DAY, 3 * DAY, 7 * DAY};

    private static final String[] MESSAGES = {
            "Que tal uma partida rápida de damas?",
            "O tabuleiro está montado. Bora jogar?",
            "Faz tempo que você não joga. Topa uma revanche contra o celular?",
            "Treine suas capturas múltiplas: uma partida leva poucos minutos.",
    };

    /** Cria o canal de notificações (no Android 13+ isso também pede a permissão). */
    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            // O build compila contra a API 23, então as classes do Android 8+ vêm por reflexão.
            Class<?> cls = Class.forName("android.app.NotificationChannel");
            Object channel = cls.getConstructor(String.class, CharSequence.class, int.class)
                    .newInstance(CHANNEL, "Lembretes para jogar", 3 /* IMPORTANCE_DEFAULT */);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationManager.class.getMethod("createNotificationChannel", cls).invoke(nm, channel);
        } catch (Exception ignored) {
        }
    }

    /** Chamado quando o jogador sai do app: recomeça a sequência de lembretes. */
    public static void scheduleFirst(Context ctx) {
        prefs(ctx).edit().putInt("reminder_step", 0).apply();
        schedule(ctx, System.currentTimeMillis() + DELAYS[0]);
    }

    /** Chamado quando o jogador abre o app. */
    public static void cancel(Context ctx) {
        alarms(ctx).cancel(alarmIntent(ctx));
        prefs(ctx).edit().remove("reminder_at").apply();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(1);
    }

    private static void schedule(Context ctx, long at) {
        prefs(ctx).edit().putLong("reminder_at", at).apply();
        alarms(ctx).set(AlarmManager.RTC, at, alarmIntent(ctx));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // alarmes somem quando o aparelho reinicia
            long at = prefs(ctx).getLong("reminder_at", 0);
            if (at > 0) schedule(ctx, Math.max(at, System.currentTimeMillis() + 60_000));
            return;
        }
        show(ctx);
        SharedPreferences p = prefs(ctx);
        int step = p.getInt("reminder_step", 0) + 1;
        if (step < DELAYS.length) {
            p.edit().putInt("reminder_step", step).apply();
            schedule(ctx, System.currentTimeMillis() + DELAYS[step]);
        } else {
            p.edit().remove("reminder_at").apply();
        }
    }

    @SuppressWarnings("deprecation")
    private static void show(Context ctx) {
        createChannel(ctx);
        Intent open = new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent tap = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(ctx)
                .setSmallIcon(R.drawable.ic_notify)
                .setColor(0xFFFFC107)
                .setContentTitle("Damas")
                .setContentText(MESSAGES[new Random().nextInt(MESSAGES.length)])
                .setContentIntent(tap)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                Notification.Builder.class.getMethod("setChannelId", String.class).invoke(b, CHANNEL);
            } catch (Exception ignored) {
            }
        }
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, b.build());
    }

    private static PendingIntent alarmIntent(Context ctx) {
        return PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, Reminder.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static AlarmManager alarms(Context ctx) {
        return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("dama", Context.MODE_PRIVATE);
    }
}
