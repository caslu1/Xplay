#!/usr/bin/env bash
# Drives Xplay Mobile on a running emulator against tools/screenshots/mock_server.py (port 8000 on the
# host): logs in through the form, creates a profile, browses and plays. Screenshots go to shots/.
set -u
cd "$(dirname "$0")/../.."
OUT=shots
PKG=com.xplay.mobile
mkdir -p "$OUT"

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
key() { adb shell input keyevent "$1"; sleep 1; }
tap() {
    for _ in 1 2 3 4 5; do
        if adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 \
            && adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 \
            && xy=$(python3 mobile/tools/tap.py /tmp/ui.xml "$1" "${2:-1}"); then
            adb shell input tap $xy
            return 0
        fi
        sleep 2
    done
    echo "not found: $1"
    cp /tmp/ui.xml "$OUT/notfound_$(date +%s).xml" 2>/dev/null
    return 1
}
type_in() { tap "$1" && sleep 1 && adb shell input text "$2" && sleep 1; }
swipe_up() { adb shell input swipe 540 1800 540 700 500; sleep 2; }

adb install -r mobile/build/XplayMobile.apk
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb logcat -c

adb shell am start -W -n $PKG/.LoginActivity
shot 01_login 4
type_in "Servidor (ex.: http://servidor.com:8080)" "http://10.0.2.2:8000"
type_in "Usuário" "demo"
type_in "Senha" "demo"
key KEYCODE_BACK
tap "Entrar"
shot 02_new_profile 10
adb shell input text "LUCAS"
key KEYCODE_BACK
tap "Salvar"
shot 03_profiles 3
tap "LUCAS"
shot 04_home 10
swipe_up
shot 05_home_rows 4
swipe_up
shot 06_home_rows2 4

# Movie: details, play ~22s so it lands in "Continuar assistindo"
adb shell input swipe 540 700 540 2000 300; adb shell input swipe 540 700 540 2000 300; sleep 2
tap "re:^O [A-ZÀ-Ú]" 2
shot 07_movie_detail 6
tap "Assistir"
shot 08_player 8
adb shell input tap 1200 300
shot 09_player_controls 2
sleep 14
key KEYCODE_BACK
shot 10_movie_detail_continue 3
key KEYCODE_BACK
shot 11_home_continue 4

# Series: details, episodes, play
tap "Séries"
shot 12_series_tab 8
tap "re:^A [A-ZÀ-Ú]" 1
shot 13_series_detail 6
swipe_up
shot 14_series_episodes 3
tap "re:^1\\. "
shot 15_episode_player 8
adb shell input tap 1200 300
shot 16_episode_controls 2
sleep 12
key KEYCODE_BACK
shot 17_series_continue 3
key KEYCODE_BACK

tap "Ao vivo"
shot 18_live 6
tap "Rede Sol"
shot 19_live_player 8
adb shell input tap 1200 300
shot 20_live_controls 2
key KEYCODE_BACK
sleep 2
tap "Favoritos"
shot 21_favorites 3
tap "Ajustes"
shot 22_settings 3
tap "Início"
sleep 2
tap "Buscar"
sleep 2
adb shell input text "ra"
shot 23_search 5
key KEYCODE_BACK
key KEYCODE_BACK

# Landscape home
adb shell settings put system user_rotation 1
shot 24_home_landscape 5
adb shell settings put system user_rotation 0

adb logcat -d -b crash > "$OUT/crash.txt" 2>&1
adb logcat -d AndroidRuntime:E '*:S' > "$OUT/errors.txt" 2>&1
adb logcat -d -s MediaPlayer:* NuPlayer:* > "$OUT/media.txt" 2>&1
ls -la "$OUT"
