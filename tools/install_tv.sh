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
  adb -s "$TV" shell pm disable-user --user 0 com.google.android.tvlauncher
  # Overlay permission is never used for overlays: it exempts BootReceiver from the background-start block (see README).
  # pm grant persists across reboots (the appop alone is reset at boot); appops set applies it immediately.
  adb -s "$TV" shell pm grant ru.polkatv.launcher android.permission.SYSTEM_ALERT_WINDOW
  adb -s "$TV" shell appops set ru.polkatv.launcher SYSTEM_ALERT_WINDOW allow
  echo "Google TV launcher disabled; Home now opens Polka, also right after boot. Restore with tools/restore_googletv.sh"
fi
adb -s "$TV" shell am start -n ru.polkatv.launcher/.MainActivity >/dev/null
echo "Installed. HOME now: $(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
