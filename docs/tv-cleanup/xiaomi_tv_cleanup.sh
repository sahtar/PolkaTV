#!/bin/bash
# Чистка телевизора Xiaomi MiTV-MOEU1 (Android TV 14) по решениям владельца от 2026-10-24 и 2026-10-25:
# телеметрия, TV Manager, Play Games, PatchWall, Mi Connect и Google Assistant отключаются,
# «только для просмотра» приложениям запрещается фон (xiaomi_tv_restrict_background.sh).
# Запуск: TV=<ip>:5555 bash xiaomi_tv_cleanup.sh      Откат: TV=<ip>:5555 bash xiaomi_tv_rollback.sh
# Без PatchWall, Mi Connect и ассистента (остаются кнопка PatchWall, Mi Share и голосовой поиск с пульта):
#   OPTIONAL=0 TV=<ip>:5555 bash xiaomi_tv_cleanup.sh
# Список приложений для запрета фона — в xiaomi_tv_restrict_background.sh, подправьте его под свой телевизор.
set -u
TV="${TV:?Укажите адрес устройства: TV=<ip>:5555}"
OPTIONAL="${OPTIONAL:-1}"
adb connect "$TV" >/dev/null 2>&1
adb -s "$TV" get-state >/dev/null 2>&1 || { echo "Устройство $TV недоступно: включите телевизор и отладку по сети" >&2; exit 1; }
D() { adb -s "$TV" shell "$@"; }

# Телеметрия Xiaomi, статистика Xiaomi, TV Manager («очистка памяти»), Google Play Игры.
DISABLE="com.miui.tv.analytics com.xiaomi.statistic com.xiaomi.mitv.tvmanager com.google.android.play.games"
# PatchWall и его сервис (кнопка PatchWall на пульте перестанет работать), Mi Connect (Mi Share, связка
# с телефоном Xiaomi), Google Assistant (кнопка микрофона на пульте).
if [ "$OPTIONAL" != "0" ]; then
  DISABLE="$DISABLE com.mitv.tvhome.atv com.mitv.tvhome.michannel com.xiaomi.mi_connect_service com.google.android.katniss"
fi

echo "== Отключаю пакеты =="
for p in $DISABLE; do printf "%-40s " "$p"; D pm disable-user --user 0 "$p" | tr -d '\r'; done

echo "== Запрещаю фоновую работу приложений «только для просмотра» =="
TV="$TV" bash "$(dirname "$0")/xiaomi_tv_restrict_background.sh"

echo "== Итог: отключённые пакеты =="
D pm list packages -d | tr -d '\r'
