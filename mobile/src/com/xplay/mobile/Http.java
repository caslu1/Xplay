package com.xplay.mobile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/** Plain HTTP GET that follows redirects (also between http and https) and un-gzips. */
public final class Http {

    public static final String AGENT = "XplayMobile/1.0";

    private static final java.util.regex.Pattern URL = java.util.regex.Pattern.compile("(?i)https?://[^\\s\"'<>]+");

    private Http() {}

    /**
     * The address inside pasted text: people often copy a name or label with the link
     * ("nuvio http://host/get.php?…"). Without a scheme, the last word gets http://.
     */
    public static String extractUrl(String text) {
        String t = text == null ? "" : text.trim();
        java.util.regex.Matcher m = URL.matcher(t);
        if (m.find()) return m.group().replaceAll("[),.;]+$", "");
        String[] words = t.split("\\s+");
        String last = words[words.length - 1];
        return last.isEmpty() ? "" : "http://" + last;
    }

    public static InputStream open(String address, int readTimeout) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn = null;
        for (int redirects = 0; ; redirects++) {
            if (redirects > 5) throw new IOException("Redirecionamentos demais");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(readTimeout);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", AGENT);
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

    public static String get(String url) throws IOException {
        return new String(bytes(open(url, 60000)), "UTF-8");
    }

    public static byte[] bytes(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
