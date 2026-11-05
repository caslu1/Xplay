#!/usr/bin/env bash
# Runs on an emulator (see .github/workflows/listo.yml): installs the APK, takes screenshots,
# and prints them as small base64 JPEGs in the log so they can be checked without artifacts.
set -u
cd "$(dirname "$0")/.."
PKG=com.listo.app
OUT=shots
mkdir -p "$OUT"
shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
tap() {
    for _ in 1 2 3; do
        if adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 \
            && adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 \
            && xy=$(python3 ../tools/screenshots/tap.py /tmp/ui.xml "$1"); then
            adb shell input tap $xy; return 0
        fi
        sleep 2
    done
    echo "not found: $1"; return 1
}

adb install -r build/Listo.apk
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity
shot 01_calendar 5
adb shell input tap 980 2000; shot 02_new_task 3
adb shell input text "Estudar"; shot 03_typed 2
tap "re:(?i)^salvar$"; shot 04_saved 3
tap "Listas"; shot 05_lists 3
tap "re:Tomate"; shot 06_checked 2
adb shell input swipe 540 1800 540 600; shot 07_scrolled 2
adb shell input tap 980 2000; shot 08_new_list 3
adb shell input keyevent 4; adb shell input keyevent 4; sleep 1
tap "Lista de estudos"; shot 08b_list_menu 2
adb shell input keyevent 4; adb shell input keyevent 4
tap "Calendário"; adb shell input swipe 900 900 150 900; shot 09_next_month 2

echo "=== crashes ==="
adb logcat -d -s AndroidRuntime:E | tail -40
for f in "$OUT"/*.png; do
    python3 -c "
import sys,io,base64
from PIL import Image
im=Image.open(sys.argv[1]).convert('RGB'); im.thumbnail((360,800))
b=io.BytesIO(); im.save(b,'JPEG',quality=60); print('IMG', sys.argv[1], base64.b64encode(b.getvalue()).decode())" "$f"
done
