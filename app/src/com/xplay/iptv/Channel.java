package com.xplay.iptv;

public final class Channel {
    public final String name;
    public final String url;
    public final String group;
    public final String logo;
    /** Xtream stream id, used for the quick EPG; null for M3U channels. */
    public final String streamId;
    public final int num;
    /** Channel id in the XMLTV guide (tvg-id / epg_channel_id), or null. */
    public final String epgId;

    public Channel(String name, String url, String group, String logo, String streamId, int num, String epgId) {
        this.name = name;
        this.url = url;
        this.group = group;
        this.logo = logo;
        this.streamId = streamId;
        this.num = num;
        this.epgId = epgId;
    }
}
