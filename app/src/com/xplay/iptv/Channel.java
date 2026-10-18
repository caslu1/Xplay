package com.xplay.iptv;

public final class Channel {
    public final String name;
    public final String url;
    public final String group;
    public final String logo;
    /** Xtream stream id, used for EPG; null for M3U channels. */
    public final String streamId;
    public final int num;

    public Channel(String name, String url, String group, String logo) {
        this(name, url, group, logo, null, 0);
    }

    public Channel(String name, String url, String group, String logo, String streamId, int num) {
        this.name = name;
        this.url = url;
        this.group = group;
        this.logo = logo;
        this.streamId = streamId;
        this.num = num;
    }
}
