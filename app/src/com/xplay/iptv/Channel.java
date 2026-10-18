package com.xplay.iptv;

public final class Channel {
    public final String name;
    public final String url;
    public final String group;
    public final String logo;

    public Channel(String name, String url, String group, String logo) {
        this.name = name;
        this.url = url;
        this.group = group;
        this.logo = logo;
    }
}
