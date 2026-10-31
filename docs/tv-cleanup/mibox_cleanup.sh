#!/bin/bash
# Чистка приставки Xiaomi Mi Box 3 (MIBOX3, Android TV 9) по тем же решениям владельца, что и для телевизора:
# телеметрия, Play Games, PatchWall, Google Assistant, Mi Link отключаются; «только для просмотра» приложениям запрещается фон.
# Запуск: bash mibox_cleanup.sh      Откат: bash mibox_rollback.sh
set -u
TV="${TV:-192.168.100.39:5555}"
adb connect "$TV" >/dev/null 2>&1
D() { adb -s "$TV" shell "$@"; }

DISABLE="com.miui.tv.analytics com.google.android.play.games com.mitv.tvhome.atv com.mitv.tvhome.michannel com.google.android.katniss com.mitv.milinkservice"
RESTRICT="ru.ivi.client ru.kinopoisk.tv net.megogo.tv com.kinopub com.netflix.ninja com.amazon.amazonvideo.livingroom com.google.android.videos com.google.android.music com.google.android.tv com.ctcmediagroup.videomore com.gsgroup.tricoloronline gpm.tnt_premier ru.imult.mult ru.more.play ru.rt.video.app.tv com.mobisystems.fileman com.mxtech.videoplayer.ad com.nousguide.android.rbtv com.tcl.browser com.yandex.browser.tv com.vectorunit.purple.googleplay"

echo "== Отключаю пакеты =="
for p in $DISABLE; do printf "%-40s " "$p"; D pm disable-user --user 0 "$p" | tr -d '\r'; done

echo "== Запрещаю фоновую работу (RUN_ANY_IN_BACKGROUND=ignore) =="
for p in $RESTRICT; do D appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND ignore; done
# AppOps пишет файл с задержкой; без принудительной записи перезагрузка теряет изменения.
D cmd appops write-settings
sleep 1
echo "== Проверка (перечитываю настройки с диска) =="
D cmd appops read-settings
for p in $RESTRICT; do printf "%-40s %s\n" "$p" "$(D appops get --user 0 "$p" RUN_ANY_IN_BACKGROUND | tr -d '\r' | head -1)"; done

echo "== Останавливаю фоновые процессы =="
for p in $RESTRICT; do D am force-stop "$p"; done
D cat /proc/meminfo | tr -d '\r' | grep MemAvailable
