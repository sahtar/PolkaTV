#!/bin/bash
# Откат изменений на Xiaomi TV (MiTV-MOEU1, сейчас 192.168.100.16 по LAN), сделанных 2026-10-24 и 2026-10-25.
# Запуск: bash xiaomi_tv_rollback.sh   (нужен adb и включённая отладка по сети на телевизоре)
set -u
TV="${TV:-192.168.100.16:5555}"
adb connect "$TV" >/dev/null 2>&1
D() { adb -s "$TV" shell "$@"; }

echo "== Включаю обратно отключённые системные пакеты =="
for p in com.miui.tv.analytics com.xiaomi.statistic com.xiaomi.mitv.tvmanager com.google.android.play.games; do
  printf "%-36s " "$p"; D pm enable --user 0 "$p" | tr -d '\r'
done

echo "== Включаю обратно пакеты, отключённые 2026-10-25 (PatchWall, Mi Connect, Google Assistant) =="
for p in com.mitv.tvhome.atv com.mitv.tvhome.michannel com.xiaomi.mi_connect_service com.google.android.katniss; do printf "%-36s " "$p"; D pm enable --user 0 "$p" | tr -d '\r'; done

echo "== Снимаю ограничение фоновой работы с приложений =="
for p in ru.mts.mtstv ru.kinopoisk.tv ru.ivi.client ru.start.androidmobile net.megogo.tv com.mobisystems.fileman ru.yandex.games com.vk.tv ru.rutube.app ru.more.play live.tvoe.app de.benzac.msx com.tcl.browser com.mxtech.videoplayer.ad com.nousguide.android.rbtv ru.rt.video.app.tv; do
  D cmd appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND allow
  D cmd appops set --user 0 "$p" RUN_IN_BACKGROUND allow
  echo "$p: фон разрешён"
done
# AppOps пишет файл с задержкой; без принудительной записи перезагрузка теряет изменения.
D cmd appops write-settings

echo "== Итог =="; D pm list packages -d | tr -d '\r'
