# Приставка Xiaomi Mi Box 3 — чистка 2026-10-31

**Устройство:** MIBOX3 (кодовое имя once), Android TV 9 (API 28), Amlogic, armeabi-v7a, 1920×1080, 2 ГБ ОЗУ, Wi-Fi `192.168.100.39:5555`.
**Хранилище:** /data 5,2 ГБ, занято 85 % (приложения 3,2 ГБ, их данные 1,5 ГБ, кэш 0,46 ГБ). Крупнейшие: gms 288+141 МБ, ivi 118+208, Google TTS 88+270 (данные), Кинопоиск 134+66, more.tv 136+29, MX Player 175, Триколор 71+52, Netflix 134+24, Beach Buggy 145.

## Что было в фоне до чистки (свободно 980 МБ через 4 мин после загрузки)

- Телеметрия Xiaomi `com.miui.tv.analytics` — 197 МБ резидентно.
- Google Assistant `com.google.android.katniss` — два процесса, 313 МБ.
- PatchWall `com.mitv.tvhome.atv` — 104 МБ (домашний экран теперь «Полка», лаунчер Google TV отключён ещё при установке «Полки»).
- Mi Link `com.mitv.milinkservice` — 73 МБ (трансляция с телефонов Xiaomi).
- Кинотеатры и прочие «только для просмотра»: ivi 262 МБ, Кинопоиск 169+130 (AppMetrica), Google Play Music 131, Netflix 97, Amazon Prime Video 95, Kinopub 93, Megogo 89, Play Movies 112, Live Channels 81.

## Сделано (`mibox_cleanup.sh`, откат — `mibox_rollback.sh`)

- Отключены (`pm disable-user`, обратимо): `com.miui.tv.analytics`, `com.google.android.play.games`, `com.mitv.tvhome.atv`, `com.mitv.tvhome.michannel`, `com.google.android.katniss`, `com.mitv.milinkservice`. Те же решения владельца, что и для телевизора: кнопка микрофона/ассистента на пульте и трансляция с телефона Xiaomi не работают.
- Запрещён фон (`appops RUN_ANY_IN_BACKGROUND=ignore` + `cmd appops write-settings`, иначе перезагрузка теряет изменения) 21 приложению: ivi, Кинопоиск, Megogo, Kinopub, Netflix, Amazon Prime Video, Google Play Movies, Google Play Music, Live Channels, more.tv, Триколор, Premier, Мульт, Wink, MobiSystems File Manager, MX Player, Red Bull TV, TCL Browser, Яндекс Браузер, Beach Buggy Racing. Запуск с полки не меняется.
- На Android 9 `BOOT_COMPLETED` доходит и до ограниченных приложений: Netflix и Amazon Prime Video стартуют при загрузке своими приёмниками (Amazon сам завершается, Netflix остаётся резидентно ~105 МБ). Отключить один приёмник нельзя: на Android 9 оболочка adb не вправе менять компоненты чужих приложений (SecurityException). Убрать Netflix из фона можно только отключив приложение целиком (`pm disable-user --user 0 com.netflix.ninja`) — решение владельца, не выполнено.
- Не тронуты: gms, Play Store, mediashell (Chromecast), tvrecommendations (панель уведомлений), Google TTS, клавиатура, сервис пульта, обновления Xiaomi (`com.xiaomi.mitv.updateservice`) и прочие системные службы.

## Результат

- После перезагрузки: «Полка» на экране сама, процессов отключённых пакетов нет, ограничения на месте; свободно 1,19 ГБ через минуту после загрузки (до чистки — 0,98 ГБ через 4 минуты). Падений в журнале нет.
- Место на диске не трогалось: кэш приложений 0,46 ГБ можно сбросить (`pm trim-caches`), данные Google TTS (270 МБ) и неиспользуемые приложения — на усмотрение владельца.
