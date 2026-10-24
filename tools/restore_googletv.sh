#!/bin/bash
# Make Google TV the HOME app again (Polka stays installed).
set -euo pipefail
TV="${TV:-192.168.100.2:5555}"
adb connect "$TV" >/dev/null
adb -s "$TV" shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
adb -s "$TV" shell input keyevent 3
echo "HOME now: $(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
