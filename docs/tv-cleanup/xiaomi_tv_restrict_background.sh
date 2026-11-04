#!/bin/bash
# Ограничение фоновой работы «только для просмотра» приложений на Xiaomi TV (MiTV-MOEU1).
# Запуск: bash xiaomi_tv_restrict_background.sh   (нужен adb и отладка по сети на телевизоре)
# Откат: xiaomi_tv_rollback.sh
set -u
TV="${TV:?Укажите адрес устройства: TV=<ip>:5555}"
adb connect "$TV" >/dev/null 2>&1
D() { adb -s "$TV" shell "$@"; }

PKGS="ru.mts.mtstv ru.kinopoisk.tv ru.ivi.client ru.start.androidmobile net.megogo.tv com.mobisystems.fileman ru.yandex.games com.vk.tv ru.rutube.app ru.more.play live.tvoe.app de.benzac.msx com.tcl.browser com.mxtech.videoplayer.ad com.nousguide.android.rbtv ru.rt.video.app.tv"

echo "== Запрещаю фоновую работу (RUN_ANY_IN_BACKGROUND=ignore) =="
for p in $PKGS; do
  D appops set --user 0 "$p" RUN_ANY_IN_BACKGROUND ignore
done
# AppOps пишет файл с задержкой; без принудительной записи перезагрузка теряет изменения.
D cmd appops write-settings
sleep 1

echo "== Проверка (перечитываю настройки с диска) =="
D cmd appops read-settings
for p in $PKGS; do
  printf "%-32s %s\n" "$p" "$(D appops get --user 0 "$p" RUN_ANY_IN_BACKGROUND | tr -d '\r' | head -1)"
done

echo "== Останавливаю фоновые процессы =="
for p in $PKGS; do D am force-stop "$p"; done
D cat /proc/meminfo | tr -d '\r' | grep MemAvailable
