#!/bin/bash
# Make Google TV the HOME app again (Polka stays installed). Usage: TV=<ip:port> tools/restore_googletv.sh
set -euo pipefail
TV="${TV:-192.168.100.16:5555}"
adb connect "$TV" >/dev/null
S() { adb -s "$TV" shell "$@"; }
S pm enable --user 0 com.google.android.tvlauncher
S pm revoke ru.polkatv.launcher android.permission.SYSTEM_ALERT_WINDOW || true
S appops set ru.polkatv.launcher SYSTEM_ALERT_WINDOW default || true
S cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
S input keyevent 3
echo "HOME now: $(S cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -1)"
