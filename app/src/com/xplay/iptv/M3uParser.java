package com.xplay.iptv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Downloads and parses M3U / M3U8 playlists (#EXTM3U with #EXTINF entries). */
public final class M3uParser {

    private static final Pattern ATTR = Pattern.compile("([a-zA-Z0-9_-]+)=\"([^\"]*)\"");

    private M3uParser() {}

    public static List<Channel> download(String playlistUrl) throws IOException {
        URL url = new URL(playlistUrl);
        HttpURLConnection conn = null;
        for (int redirects = 0; redirects < 5; redirects++) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Xplay/1.0");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) throw new IOException("Redirecionamento inválido");
                // Follow redirects manually so http <-> https hops work too.
                url = new URL(url, location);
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new IOException("Servidor respondeu HTTP " + code);
            }
            break;
        }
        InputStream in = conn.getInputStream();
        try {
            return parse(in);
        } finally {
            in.close();
            conn.disconnect();
        }
    }

    public static List<Channel> parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        List<Channel> channels = new ArrayList<Channel>();
        String name = null, group = null, logo = null;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF")) {
                group = null;
                logo = null;
                String tvgName = null;
                int comma = firstCommaOutsideQuotes(line);
                String attrs = comma >= 0 ? line.substring(0, comma) : line;
                name = comma >= 0 ? line.substring(comma + 1).trim() : "";
                Matcher m = ATTR.matcher(attrs);
                while (m.find()) {
                    String key = m.group(1).toLowerCase();
                    if (key.equals("group-title")) group = m.group(2);
                    else if (key.equals("tvg-logo")) logo = m.group(2);
                    else if (key.equals("tvg-name")) tvgName = m.group(2);
                }
                if (name.isEmpty() && tvgName != null) name = tvgName;
            } else if (line.startsWith("#EXTGRP:")) {
                group = line.substring(8).trim();
            } else if (!line.startsWith("#")) {
                String n = (name == null || name.isEmpty()) ? line : name;
                String g = (group == null || group.isEmpty()) ? "Sem grupo" : group;
                channels.add(new Channel(n, line, g, logo));
                name = null;
                group = null;
                logo = null;
            }
        }
        return channels;
    }

    private static int firstCommaOutsideQuotes(String s) {
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) {
                return i;
            }
        }
        return -1;
    }
}
