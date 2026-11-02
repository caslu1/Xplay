package com.ilha.notch;

import java.util.ArrayList;
import java.util.List;

/** Connects the notification listener with the overlay; both run in the same process. */
final class Hub {
    private Hub() {}

    static IslandService island;
    static NotifService notif;
    static List<Item> live = new ArrayList<>();

    static void publishLive(List<Item> items) {
        live = items;
        if (island != null) island.onLive(items);
    }

    static void notify(Item item) {
        if (island != null) island.onNotification(item);
    }

    static void removed(String key) {
        if (island != null) island.onRemoved(key);
    }

    static void requestResync() {
        if (notif != null) notif.resync();
    }
}
