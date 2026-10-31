#!/bin/bash
# Build the release APK, install it on a Xiaomi TV or box and (with --set-home) make Polka the HOME app.
# Usage: TV=192.168.100.39:5555 tools/install_tv.sh [--set-home]
set -euo pipefail
cd "$(dirname "$0")/.."
TV="${TV:-192.168.100.16:5555}"
adb connect "$TV" >/dev/null
S() { adb -s "$TV" shell "$@"; }
./gradlew --no-daemon -q assembleRelease
adb -s "$TV" install -r app/build/outputs/apk/release/app-release.apk
if [ "${1:-}" = "--set-home" ]; then
  sdk=$(S getprop ro.build.version.sdk | tr -d '\r')
  S cmd package set-home-activity ru.polkatv.launcher/.MainActivity
  home=$(S cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -1)
  if [ "$home" != "ru.polkatv.launcher/.MainActivity" ]; then
    # Xiaomi TV firmware gives Google TV a higher HOME priority than any third-party app: the only way is to disable it (reversible).
    S pm disable-user --user 0 com.google.android.tvlauncher
    echo "Google TV launcher disabled (its HOME priority beats ours). Restore with tools/restore_googletv.sh"
  else
    echo "Google TV launcher left enabled: the preferred HOME activity is honoured on this device."
  fi
  if [ "$sdk" -ge 29 ]; then
    # Android 10+ blocks activity starts from the background; the overlay permission exempts BootReceiver/WakeReceiver.
    # It is never used for overlays (see README). pm grant persists across reboots; appops set applies it immediately.
    S pm grant ru.polkatv.launcher android.permission.SYSTEM_ALERT_WINDOW
    S appops set ru.polkatv.launcher SYSTEM_ALERT_WINDOW allow
  fi
fi
S am start -n ru.polkatv.launcher/.MainActivity >/dev/null
echo "Installed. HOME now: $(S cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -1)"
