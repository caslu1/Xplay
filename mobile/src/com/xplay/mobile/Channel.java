package com.xplay.mobile;

/** A live TV channel. */
public final class Channel {
    public final String name;
    public final String url;
    public final String group;
    public final String logo;
    /** Xtream stream id (for the EPG); null for M3U channels. */
    public final String streamId;
    public final int num;

    public Channel(String name, String url, String group, String logo, String streamId, int num) {
        this.name = name;
        this.url = url;
        this.group = group;
        this.logo = logo;
        this.streamId = streamId;
        this.num = num;
    }

    public String favKey() {
        return "live:" + url;
    }
}
