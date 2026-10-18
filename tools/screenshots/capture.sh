#!/usr/bin/env bash
# Drives the app on a running emulator against mock_server.py (port 8000 on the host)
# and saves screenshots to shots/.
set -u
cd "$(dirname "$0")/../.."
OUT=shots
PKG=com.xplay.iptv
SERVER=http://10.0.2.2:8000
mkdir -p "$OUT"

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
key() { adb shell input keyevent "$1"; sleep 1; }
tap() {
    for _ in 1 2 3 4; do
        if adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 \
            && adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 \
            && xy=$(python3 tools/screenshots/tap.py /tmp/ui.xml "$1"); then
            adb shell input tap $xy
            return 0
        fi
        sleep 2
    done
    echo "not found: $1"
    return 1
}

adb install -r build/Xplay.apk
adb root
sleep 4
adb shell settings put system user_rotation 0

adb shell am start -W -n $PKG/.LoginActivity
shot 01_login 4
adb shell am force-stop $PKG

cat > /tmp/xplay.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="mode">xtream</string>
    <string name="last_mode">xtream</string>
    <string name="xtream_dns">$SERVER</string>
    <string name="xtream_user">demo</string>
    <string name="xtream_pass">demo</string>
    <string name="last_channel">$SERVER/live/demo/demo/1.m3u8</string>
    <set name="favorites">
        <string>$SERVER/live/demo/demo/2.m3u8</string>
        <string>movie:1001</string>
    </set>
</map>
XML
adb push /tmp/xplay.xml /data/local/tmp/xplay.xml
adb shell "mkdir -p /data/data/$PKG/shared_prefs \
    && cp /data/local/tmp/xplay.xml /data/data/$PKG/shared_prefs/xplay.xml \
    && chown -R \$(stat -c %u:%g /data/data/$PKG) /data/data/$PKG/shared_prefs \
    && chmod 660 /data/data/$PKG/shared_prefs/xplay.xml \
    && restorecon -R /data/data/$PKG/shared_prefs"

adb shell am start -W -n $PKG/.LoginActivity
shot 02_live_groups 12
tap "ABERTOS" && shot 03_live_channels 6
tap "Rede Sol" && shot 04_live_preview 8
tap "Menu" && shot 05_menu 2
key KEYCODE_BACK
tap "Rede Sol" && shot 06_player 4
key KEYCODE_BACK
key KEYCODE_BACK
sleep 2

tap "Menu" && sleep 1 && tap "FILMES" && shot 07_movies 12
adb shell input swipe 500 1600 500 700 400
shot 08_movies_scrolled 3
tap "re:^O [A-ZÀ-Ú]" && shot 09_movie_detail 6
key KEYCODE_BACK
adb shell input swipe 500 700 500 1600 300
sleep 1
tap "Séries" && shot 10_series 10
tap "re:^A [A-ZÀ-Ú]" && shot 11_series_detail 6
adb shell input swipe 500 1600 500 600 400
shot 12_series_episodes 3
key KEYCODE_BACK
tap "Perfil" && shot 13_profile 3
tap "Buscar" && sleep 1 && adb shell input text "ra" && key KEYCODE_ESCAPE && shot 14_search 5
adb logcat -d -b crash > "$OUT/crash.txt" 2>&1
adb logcat -d AndroidRuntime:E '*:S' > "$OUT/errors.txt" 2>&1
ls -la "$OUT"
