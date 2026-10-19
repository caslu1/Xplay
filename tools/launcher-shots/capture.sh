#!/usr/bin/env bash
# Runs inside the emulator job: installs the launcher and screenshots its screens into shots/.
set -x
mkdir -p shots
adb wait-for-device
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb install -r -g launcher/build/XplayLauncher.apk
shot() { sleep "${2:-3}"; adb exec-out screencap -p > "shots/$1.png"; }
key() { adb shell input keyevent "$@"; sleep 1; }

adb shell am start -W -n com.xplay.launcher/.HomeActivity
shot 01-home 12
key KEYCODE_DPAD_RIGHT
shot 02-home-focus 1
key KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN
shot 03-home-apps 1

adb shell am start -W -n com.xplay.launcher/.AppsActivity --ez pick true
shot 04-pick-app 4
key KEYCODE_BACK

adb shell am start -W -n com.xplay.launcher/.StoreActivity
shot 05-store 6
key KEYCODE_BACK

adb shell am start -W -n com.xplay.launcher/.SettingsActivity
shot 06-settings 3
key KEYCODE_BACK

adb logcat -d -s AndroidRuntime:E > shots/crash.log || true
ls -la shots
