package com.ilha.notch;

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;

/** Something the island shows: a live activity (music, timer, progress, call) or a notification. */
final class Item {
    static final int MEDIA = 1, PROGRESS = 2, TIMER = 3, CALL = 4, NOTIF = 5, INCOMING = 6, TEST = 7, AUDIO = 8;

    int type;
    String key;
    String pkg;
    String app;
    String title;
    String text;
    Drawable icon;      // small app icon
    Bitmap art;         // album art / large icon
    Drawable large;     // large icon when it is not a bitmap
    int color = 0xFFB8F2D9;
    int progress, max;
    boolean indeterminate;
    long chronoBase;    // wall-clock base for timers
    boolean countDown;
    PendingIntent contentIntent;
    Notification.Action[] actions;
    MediaController controller;
    boolean playing;
    long postedAt;

    boolean isLive() { return type == MEDIA || type == AUDIO || type == PROGRESS || type == TIMER || type == CALL; }
}
