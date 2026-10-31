#!/bin/bash
# Откат чистки приставки Xiaomi Mi Box 3 (mibox_cleanup.sh). Запуск: bash mibox_rollback.sh
set -u
TV="${TV:-192.168.100.39:5555}"
adb connect "$TV" >/dev/null 2>&1
D() { adb -s "$TV" shell "$@"; }

echo "== Включаю обратно отключённые пакеты =="
for p in com.miui.tv.analytics com.google.android.play.games com.mitv.tvhome.atv com.mitv.tvhome.michannel com.google.android.katniss com.mitv.milinkservice; do
  printf "%-40s " "$p"; D pm enable --user 0 "$p" | tr -d '\r'
done

echo "== Снимаю ограничение фоновой работы =="
for p in ru.ivi.client ru.kinopoisk.tv net.megogo.tv com.kinopub com.netflix.ninja com.amazon.amazonvideo.livingroom com.google.android.videos com.google.android.music com.google.android.tv com.ctcmediagroup.videomore com.gsgroup.tricoloronline gpm.tnt_premier ru.imult.mult ru.more.play ru.rt.video.app.tv com.mobisystems.fileman com.mxtech.videoplayer.ad com.nousguide.android.rbtv com.tcl.browser com.yandex.browser.tv com.vectorunit.purple.googleplay; do
  D appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND allow; echo "$p: фон разрешён"
done
# AppOps пишет файл с задержкой; без принудительной записи перезагрузка теряет изменения.
D cmd appops write-settings
echo "Готово. Лаунчер Google TV возвращается отдельно: TV=$TV tools/restore_googletv.sh"
