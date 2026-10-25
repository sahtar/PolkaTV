# Xiaomi TV (MiTV-MOEU1) — ревизия и очистка, 2026-10-24

## Устройство
- Модель: Xiaomi MiTV-MOEU1, Android TV 14 (API 34), сборка V816.0.25.12.1, патч безопасности 2025-11
- Железо: MediaTek MT5896, 4 ядра, 2.4 ГБ RAM, 25 ГБ /data (занято 7 ГБ)
- Сеть: 192.168.100.2, ADB по сети включён (порт 5555)
- Главный экран по умолчанию: Google TV (com.google.android.tvlauncher). PatchWall установлен, но не является домашним экраном.
- Антенна/кабель не настроены: 0 каналов в базе Live TV.

## Что стояло
- Всего пакетов: 181 (161 системных, 20 пользовательских).
- Пользовательские: Иви, Кинопоиск, KION (МТС ТВ), Okko, Wink, Rutube, VK Видео, Megogo, Start, TVOE, Red Bull TV,
  Яндекс Игры, MX Player, TCL Browser, File Manager (MobiSystems), MSX, Google TV (videos),
- Уже были отключены производителем: com.iwedia.tvinput, тюнеры ATSC/ISDB.

## Что крутилось в фоне сразу после загрузки (до очистки)
47 app-процессов. Самые тяжёлые и лишние:
- com.mitv.tvhome.atv (PatchWall) — 176 МБ, не домашний экран, но стартует при загрузке
- ru.mts.mtstv (KION) — 213 МБ + отдельный процесс AppMetrica 100 МБ
- com.xiaomi.mitv.tvmanager:centralservice (TV Manager, «очистка памяти») — 197 МБ
- ru.kinopoisk.tv — 136 МБ + :passport 130 МБ + :AppMetrica 127 МБ
- ru.ivi.client — 105 МБ, net.megogo.tv — 88 МБ, ru.start.androidmobile — 81 МБ
- com.miui.tv.analytics (телеметрия Xiaomi) — 83 МБ, com.xiaomi.statistic (статистика Xiaomi) — 64 МБ
- com.xiaomi.mi_connect_service (Mi Connect, 10 сервисов) — 84 МБ

## Что сделано
### Отключено (pm disable-user), обратимо
| Пакет | Что это |
|---|---|
| com.miui.tv.analytics | Телеметрия Xiaomi (MiTVBiAnalytics) |
| com.xiaomi.statistic | Сбор статистики Xiaomi (MiTVStat) |
| com.xiaomi.mitv.tvmanager | TV Manager — «ускоритель/очистка памяти», сам висел на 120–200 МБ |
| com.google.android.play.games | Google Play Игры |

### Запрещена фоновая работа (appops RUN_ANY_IN_BACKGROUND / RUN_IN_BACKGROUND = ignore)
Приложения работают как раньше при запуске вручную, но больше не стартуют при включении ТВ и не держат процессы в памяти:
ru.mts.mtstv (KION), ru.kinopoisk.tv, ru.ivi.client, ru.start.androidmobile, net.megogo.tv, com.mobisystems.fileman.

### Проверка после перезагрузки
Ни один из перечисленных пакетов не поднялся при загрузке. Главный экран Google TV работает.

## Что НЕ трогал (решать вам)
- PatchWall (com.mitv.tvhome.atv + michannel) — 176–338 МБ при загрузке; прошивка Xiaomi стартует его как заставку. Отключать можно, но кнопка PatchWall на пульте перестанет работать.
- Mi TV+ / Wink (com.mitv.tvhome.mitvplus) — бесплатные каналы Xiaomi.
- Mi Connect (com.xiaomi.mi_connect_service) — связь с телефонами Xiaomi (трансляция/Mi Share).
- Служба обновлений Xiaomi (com.xiaomi.mitv.updateservice) — отключение остановит OTA-обновления.
- Live TV и DVB-тюнер — каналов нет, но нужны, если подключите антенну/кабель.
- YouTube Music, Google TV (videos), Google Assistant (katniss) — пользовательские/голосовой поиск.
- В Chromecast включена отправка статистики и крэш-репортов Google — меняется только в настройках ТВ.

## Откат
`bash xiaomi_tv_rollback.sh` — включает пакеты обратно и снимает ограничения фона.
Вручную: `adb shell pm enable --user 0 <пакет>` и `adb shell cmd appops set --user 0 <пакет> RUN_ANY_IN_BACKGROUND allow`, затем обязательно `adb shell cmd appops write-settings` (см. запись от 2026-10-25 22:16). Повторное применение ограничений: `xiaomi_tv_restrict_background.sh`.

## Повторная проверка 2026-10-25 (вечер, аптайм 11 ч)

- Load average ~22 на 4 ядрах — артефакт ядра MediaTek (потоки в состоянии D/RT: pqu_module, pre_signal_task), процессор при этом на 85–90% простаивал. Реальной нагрузки нет.
- Память была под давлением: свободно 641 МБ, подкачка 525 из 614 МБ занята, у tvrecommendations десятки тысяч major faults.
- **Ограничения фона, поставленные 2026-10-24, прошивка сбросила** (все шесть пакетов снова RUN_ANY_IN_BACKGROUND=allow), и кинотеатры снова стартовали при загрузке. Ограничения восстановлены и расширены на все пользовательские приложения «только для просмотра»: ru.yandex.games (его процесс AppMetrica висел в фоне на 39 МБ), com.vk.tv, ru.rutube.app, ru.more.play, live.tvoe.app, de.benzac.msx, com.tcl.browser, com.mxtech.videoplayer.ad, com.nousguide.android.rbtv, ru.rt.video.app.tv. После остановки фоновых процессов: свободно 944 МБ, подкачка 136 МБ свободно. Если ограничения снова сбросятся, причину надо искать в прошивке (возможно, при обновлении приложений через Play).
- Домашний экран теперь «Полка» (Google TV launcher отключён), поэтому PatchWall (com.mitv.tvhome.atv) стал лишним: прошивка держит его резидентно (105–150 МБ, 28 мин CPU за 11 ч). Отключение — решение владельца (кнопка PatchWall на пульте перестанет работать).
- Кандидаты на отключение по решению владельца: Mi Connect (com.xiaomi.mi_connect_service, 10 сервисов, ~25 МБ) — нужен только для связки с телефонами Xiaomi; Google Assistant (com.google.android.katniss, два процесса ~58 МБ) — нужен для голосового поиска с пульта.
- Оставлены намеренно: gms, Play Store, tvrecommendations (панель уведомлений), mediashell (Chromecast), TTS (подгружается ассистентом).

- 2026-10-25 21:50: по решению владельца отключены PatchWall (com.mitv.tvhome.atv) и com.mitv.tvhome.michannel. Проверка перезагрузкой: «Полка» на экране сразу после загрузки, свободной памяти при старте 929 МБ (было ~810), прошивка стартует штатно; падение com.mediatek.tv.agent (SQLite, cus_color_temp_switch) наблюдалось и до изменений. Откат — в xiaomi_tv_rollback.sh.
- 2026-10-25 22:01: по решению владельца отключены Mi Connect (com.xiaomi.mi_connect_service) и Google Assistant (com.google.android.katniss). Система сняла с katniss роль ассистента (ожидаемые строки RoleControllerServiceImpl в логе), других ошибок, связанных с этими пакетами, нет. Проверка перезагрузкой: «Полка» запущена самой системой как HOME сразу при загрузке (теперь, когда PatchWall отключён, BootReceiver уже не нужен), процессов отключённых пакетов нет, свободной памяти при старте 988 МБ. Последствия: кнопка микрофона/ассистента на пульте и Mi Share/связка с телефоном Xiaomi не работают; сервис распознавания речи Google TTS (voice_recognition_service) оставлен включённым, голосовой ввод внутри приложений не проверялся. Откат — в xiaomi_tv_rollback.sh.
- 2026-10-25 22:03, **важно**: после перезагрузки ограничения RUN_ANY_IN_BACKGROUND у всех 16 приложений снова стоят в allow (ставились 2026-10-24 и повторно 2026-10-25 вечером). Кинопоиск (3 процесса) и ivi снова поднялись при загрузке, вместе ~495 МБ RSS. Вывод: прошивка сбрасывает режимы appops при каждой загрузке, `appops set` для этой задачи не годится — нужен другой механизм (отдельная задача).
- 2026-10-25 22:16, **причина сбросов найдена, вывод от 22:03 неверен**: прошивка appops не трогает. AppOps сохраняет режимы в /data/system/appops.xml с задержкой, а перезагрузка через `adb reboot` (и, по-видимому, уход в режим ожидания) обходит штатное выключение Android, при котором файл сбрасывается на диск, — отложенная запись терялась, и после загрузки режимы возвращались к allow. Решение: после `appops set` выполнять `cmd appops write-settings` (немедленная запись); проверка без перезагрузки — `cmd appops read-settings` перечитывает файл с диска. Оформлено скриптом `xiaomi_tv_restrict_background.sh` (16 пакетов, запись, проверка, force-stop); в откат добавлена та же запись. Проверка перезагрузкой: все 16 режимов остались ignore сразу после загрузки и через минуту; ни одно из этих приложений не стартовало при загрузке (ранее ivi поднимался своим BootReceiver, Кинопоиск — RecommendationsUpdateReceiver и затем JobScheduler); на экране «Полка».
