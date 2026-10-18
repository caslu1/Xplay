package com.xplay.iptv;

import java.io.BufferedInputStream;
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
import java.util.zip.GZIPInputStream;

/** Downloads and parses M3U / M3U8 playlists (#EXTM3U with #EXTINF entries). */
public final class M3uParser {

    /** Entries of a playlist plus the XMLTV guide address from its header, if any. */
    public static final class Playlist {
        public final List<Channel> entries = new ArrayList<Channel>();
        public String tvgUrl;
    }

    private static final Pattern ATTR = Pattern.compile("([a-zA-Z0-9_-]+)=\"([^\"]*)\"");

    private M3uParser() {}

    public static Playlist download(String playlistUrl) throws IOException {
        InputStream in = open(playlistUrl, 30000);
        try {
            return parse(in);
        } finally {
            in.close();
        }
    }

    /** Opens a URL following redirects (also between http and https), un-gzipping when needed. */
    public static InputStream open(String address, int readTimeout) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn = null;
        for (int redirects = 0; redirects < 5; redirects++) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(readTimeout);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Xplay/2.0");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) throw new IOException("Redirecionamento inválido");
                url = new URL(url, location);
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new IOException("Servidor respondeu HTTP " + code);
            }
            break;
        }
        BufferedInputStream in = new BufferedInputStream(conn.getInputStream(), 65536);
        in.mark(2);
        int b1 = in.read(), b2 = in.read();
        in.reset();
        return b1 == 0x1f && b2 == 0x8b ? new GZIPInputStream(in, 65536) : in;
    }

    public static Playlist parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), 65536);
        Playlist out = new Playlist();
        String name = null, group = null, logo = null, tvgId = null;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTM3U")) {
                Matcher m = ATTR.matcher(line);
                while (m.find()) {
                    String key = m.group(1).toLowerCase();
                    if ((key.equals("url-tvg") || key.equals("x-tvg-url")) && !m.group(2).isEmpty()) {
                        // Several guides may be listed; the first one is enough.
                        out.tvgUrl = m.group(2).split(",")[0].trim();
                    }
                }
            } else if (line.startsWith("#EXTINF")) {
                group = null;
                logo = null;
                tvgId = null;
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
                    else if (key.equals("tvg-id")) tvgId = m.group(2);
                }
                if (name.isEmpty() && tvgName != null) name = tvgName;
            } else if (line.startsWith("#EXTGRP:")) {
                group = line.substring(8).trim();
            } else if (!line.startsWith("#")) {
                String n = (name == null || name.isEmpty()) ? line : name;
                String g = (group == null || group.isEmpty()) ? "Sem grupo" : group;
                out.entries.add(new Channel(n, line, g, logo == null || logo.isEmpty() ? null : logo, null,
                        out.entries.size() + 1, tvgId == null || tvgId.isEmpty() ? null : tvgId));
                name = null;
                group = null;
                logo = null;
                tvgId = null;
            }
        }
        return out;
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
