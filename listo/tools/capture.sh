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
tap "Listas"; shot 02_todo 3
hold() {
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
    xy=$(python3 ../tools/screenshots/tap.py /tmp/ui.xml "$1") || { echo "not found: $1"; return 1; }
    adb shell input swipe $xy $xy 1100
}
hold "Apresentação"
adb exec-out screencap -p > "$OUT/03_mission_a.png"; echo "shot 03"
sleep 0.4; adb exec-out screencap -p > "$OUT/04_mission_b.png"; echo "shot 04"
shot 05_after 3
tap "Calendário"; shot 06_calendar_done 2
echo "=== crashes ==="
adb logcat -d -s AndroidRuntime:E | tail -40
for f in "$OUT"/*.png; do
    python3 -c "
import sys,io,base64
from PIL import Image
im=Image.open(sys.argv[1]).convert('RGB'); im.thumbnail((360,800))
b=io.BytesIO(); im.save(b,'JPEG',quality=60); print('IMG', sys.argv[1], base64.b64encode(b.getvalue()).decode())" "$f"
done
