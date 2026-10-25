# PolkaTV («Полка»)

Домашний экран для Xiaomi MiTV-MOEU1 (Android TV 14) в стиле Apple TV: часы, погода для нескольких городов (OpenWeatherMap; поиск городов через геокодинг Open-Meteo), 4–6 плиток приложений, служебные иконки, фото из заставки Xiaomi фоном.

Спецификация: `docs/superpowers/specs/2026-10-24-polkatv-launcher-design.md`. План: `docs/superpowers/plans/2026-10-24-polkatv-launcher.md`.

## Сборка и установка
- Требования: JDK 21, Android SDK (platform 34, build-tools 34.0.0), `keystore/keystore.properties` + `keystore/polkatv.jks` (не в git), `local.properties` с `sdk.dir`.
- Прогноз погоды берётся из OpenWeatherMap (Open-Meteo-хост прогноза недоступен из сети ТВ). Ключ: зарегистрируйтесь на openweathermap.org, создайте API-ключ (раздел «API keys»), добавьте строку `owm.apiKey=<ключ>` в `local.properties` (файл в `.gitignore`, в git не попадает) и пересоберите приложение. Без ключа на полке погоды будет надпись «Не задан ключ OpenWeatherMap».
- Лимиты бесплатного тарифа OpenWeatherMap: 60 запросов в минуту; лаунчер делает 2 запроса на город раз в 30 минут. Поиск городов по-прежнему использует геокодинг Open-Meteo.
- `tools/prepare_wallpapers.sh` — пересобрать обои из `design/wallpapers-src/`.
- Адрес телевизора задаётся переменной `TV` (по умолчанию `192.168.100.2:5555`, по кабелю телевизор получил `192.168.100.16`): `TV=192.168.100.16:5555 tools/install_tv.sh`.
- `tools/install_tv.sh` — собрать debug и установить; `tools/install_tv.sh --set-home` — ещё и сделать домашним экраном (см. «Домашний экран»).
- `./gradlew assembleRelease` — релизная сборка (подписана тем же ключом, поэтому ставится поверх debug: `adb install -r app/build/outputs/apk/release/app-release.apk`).

## Домашний экран
На этом телевизоре лаунчер Google TV (`com.google.android.tvlauncher`) отключён, и кнопка Home открывает «Полку». На прошивке Xiaomi у HOME-фильтра Google TV приоритет 2, у «Полки» — 0 (сторонние приложения поднять приоритет не могут), поэтому одного `set-home-activity` недостаточно.

После перезагрузки прошивка показывает PatchWall («быстрая загрузка»), и «Полка» выводит себя вперёд приёмником `BootReceiver`. Android 14 запрещает запуск активности из фона, поэтому приложению выдано разрешение на наложение поверх окон (`SYSTEM_ALERT_WINDOW`): само наложение нигде не используется, разрешение лишь снимает этот запрет.

Применённые команды (их выполняет `tools/install_tv.sh --set-home`):

```
adb shell cmd package set-home-activity ru.polkatv.launcher/.MainActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm grant ru.polkatv.launcher android.permission.SYSTEM_ALERT_WINDOW
adb shell appops set ru.polkatv.launcher SYSTEM_ALERT_WINDOW allow
```

Важно: одного `appops set` недостаточно — прошивка сбрасывает его при загрузке; именно `pm grant` делает разрешение постоянным (проверено перезагрузкой: в логе `BAL_ALLOW_SAW_PERMISSION`, «Полка» на экране через ~10 с после загрузки без нажатий).

При пробуждении из режима ожидания прошивка может поднять наверх задачу PatchWall; `WakeReceiver` возвращает «Полку» вперёд, если в момент выключения экрана она была на экране, а после включения её закрыли (приложение, которое смотрели до сна, не трогается).

Откат одной командой: `tools/restore_googletv.sh` — включает Google TV обратно, снимает разрешение (`pm revoke` + `appops set … default`) и возвращает ему Home. Кнопка PatchWall на пульте не затрагивается. Фоновые фото: 12 кадров из заставки Xiaomi (дубликаты, кадры с водяными знаками и «розовые замки» убраны по решению владельца).
## Откат
`tools/restore_googletv.sh` возвращает Google TV как домашний экран. «Полка» остаётся установленной; удалить: `adb shell pm uninstall ru.polkatv.launcher`.

## Управление
- Плитки: OK — запуск, долгое нажатие — заменить/сдвинуть. Последняя плитка — все приложения.
- Погода: ←/→ — города, OK — подробный прогноз, долгое нажатие — добавить/удалить город.
- Иконки справа сверху: входы, уведомления, сеть, настройки ТВ, настройки Полки.
