#!/bin/bash
# Build debug APK, install on the Xiaomi TV and (with --set-home) make it the HOME app.
set -euo pipefail
cd "$(dirname "$0")/.."
TV="${TV:-192.168.100.2:5555}"
adb connect "$TV" >/dev/null
./gradlew --no-daemon -q assembleDebug
adb -s "$TV" install -r app/build/outputs/apk/debug/app-debug.apk
if [ "${1:-}" = "--set-home" ]; then
  adb -s "$TV" shell cmd package set-home-activity ru.polkatv.launcher/.MainActivity
fi
adb -s "$TV" shell am start -n ru.polkatv.launcher/.MainActivity >/dev/null
echo "Installed. HOME now: $(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
