#!/bin/bash
# Откат изменений на Xiaomi TV (MiTV-MOEU1, 192.168.100.2), сделанных 2026-10-24.
# Запуск: bash xiaomi_tv_rollback.sh   (нужен adb и включённая отладка по сети на телевизоре)
set -u
TV="192.168.100.2:5555"
adb connect "$TV" >/dev/null 2>&1
D() { adb -s "$TV" shell "$@"; }

echo "== Включаю обратно отключённые системные пакеты =="
for p in com.miui.tv.analytics com.xiaomi.statistic com.xiaomi.mitv.tvmanager com.google.android.play.games; do
  printf "%-36s " "$p"; D pm enable --user 0 "$p" | tr -d '\r'
done

echo "== Снимаю ограничение фоновой работы с приложений =="
for p in ru.mts.mtstv ru.kinopoisk.tv ru.ivi.client ru.start.androidmobile net.megogo.tv com.mobisystems.fileman; do
  D cmd appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND allow
  D cmd appops set --user 0 "$p" RUN_IN_BACKGROUND allow
  echo "$p: фон разрешён"
done

echo "== Итог =="; D pm list packages -d | tr -d '\r'
