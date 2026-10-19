package com.xplay.launcher;

import android.content.Context;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Identifies this box with a short code (shown on screen and typed into the panel) and decides,
 * from the panel's device list, whether the launcher may be used.
 */
public final class Device {

    /** Result of checking this device against the panel's "Gestão de Dispositivos" list. */
    public static final class Access {
        public final boolean allowed;
        public final String reason;
        public final JSONObject entry;

        Access(boolean allowed, String reason, JSONObject entry) {
            this.allowed = allowed;
            this.reason = reason;
            this.entry = entry;
        }
    }

    private Device() {}

    /** 12 hex characters as XXXX-XXXX-XXXX, stable across reinstalls on the same box. */
    public static String id(Context c) {
        String raw = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (raw == null || raw.length() < 8 || "9774d56d682e549c".equals(raw)) {
            // Some cheap boxes report no or a shared ANDROID_ID; fall back to one we create.
            raw = Config.prefs(c).getString("device_uuid", "");
            if (raw.isEmpty()) {
                raw = UUID.randomUUID().toString().replace("-", "");
                Config.prefs(c).edit().putString("device_uuid", raw).apply();
            }
        }
        String hex = (raw.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "") + "000000000000").substring(0, 12);
        return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12);
    }

    static String normalize(String id) {
        return id == null ? "" : id.toUpperCase(Locale.US).replaceAll("[^0-9A-Z]", "");
    }

    public static Access check(Context c, Config cfg) {
        String me = normalize(id(c));
        JSONObject devices = cfg.devices();
        JSONObject blocked = find(devices.optJSONArray("blocked"), me);
        if (blocked != null) {
            String note = blocked.optString("note", "");
            return new Access(false, "Este aparelho foi bloqueado." + (note.isEmpty() ? "" : "\n" + note), blocked);
        }
        JSONObject entry = find(devices.optJSONArray("allowed"), me);
        if (entry != null) {
            if (entry.optBoolean("disabled", false)) return new Access(false, "Este aparelho está desativado.", entry);
            String expires = entry.optString("expires", "");
            if (!expires.isEmpty() && today().compareTo(expires) > 0) {
                return new Access(false, "O plano deste aparelho venceu em " + br(expires) + ".", entry);
            }
            return new Access(true, "", entry);
        }
        if ("whitelist".equals(devices.optString("mode", "open"))) {
            String msg = devices.optString("message", "");
            return new Access(false, msg.isEmpty() ? "Este aparelho ainda não foi ativado." : msg, null);
        }
        return new Access(true, "", null);
    }

    private static JSONObject find(JSONArray list, String me) {
        if (list == null) return null;
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.optJSONObject(i);
            if (o != null && normalize(o.optString("id")).equals(me)) return o;
        }
        return null;
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** 2026-12-31 -> 31/12/2026 */
    static String br(String iso) {
        String[] p = iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : iso;
    }
}
