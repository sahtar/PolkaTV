# PolkaTV («Полка»)

Домашний экран для Xiaomi MiTV-MOEU1 (Android TV 14) в стиле Apple TV: часы, погода для нескольких городов (OpenWeatherMap; поиск городов через геокодинг Open-Meteo), 4–6 плиток приложений, служебные иконки, фото из заставки Xiaomi фоном.

Спецификация: `docs/superpowers/specs/2026-10-24-polkatv-launcher-design.md`. План: `docs/superpowers/plans/2026-10-24-polkatv-launcher.md`.

## Сборка и установка
- Требования: JDK 21, Android SDK (platform 34, build-tools 34.0.0), `keystore/keystore.properties` + `keystore/polkatv.jks` (не в git), `local.properties` с `sdk.dir`.
- Прогноз погоды берётся из OpenWeatherMap (Open-Meteo-хост прогноза недоступен из сети ТВ). Ключ: зарегистрируйтесь на openweathermap.org, создайте API-ключ (раздел «API keys»), добавьте строку `owm.apiKey=<ключ>` в `local.properties` (файл в `.gitignore`, в git не попадает) и пересоберите приложение. Без ключа на полке погоды будет надпись «Не задан ключ OpenWeatherMap».
- Лимиты бесплатного тарифа OpenWeatherMap: 60 запросов в минуту; лаунчер делает 2 запроса на город раз в 30 минут. Поиск городов по-прежнему использует геокодинг Open-Meteo.
- `tools/prepare_wallpapers.sh` — пересобрать обои из `design/wallpapers-src/`.
- `tools/install_tv.sh` — собрать debug и установить; `tools/install_tv.sh --set-home` — ещё и сделать домашним экраном (см. «Домашний экран»).
- `./gradlew assembleRelease` — релизная сборка (подписана тем же ключом, поэтому ставится поверх debug: `adb install -r app/build/outputs/apk/release/app-release.apk`).

## Домашний экран
На этом телевизоре лаунчер Google TV (`com.google.android.tvlauncher`) отключён, и кнопка Home открывает «Полку». На прошивке Xiaomi у HOME-фильтра Google TV приоритет 2, у «Полки» — 0 (сторонние приложения поднять приоритет не могут), поэтому одного `set-home-activity` недостаточно. Применённые команды:

```
adb shell cmd package set-home-activity ru.polkatv.launcher/.MainActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

После перезагрузки прошивка запускает свой быстрый запуск PatchWall (`com.mitv.tvhome.atv`), и он остаётся поверх всех окон. `BootReceiver` (по `BOOT_COMPLETED`) пытается вывести «Полку» вперёд, но Android 14 блокирует такой запуск из фона (политика background activity launch), поэтому после загрузки нужно один раз нажать Home.

`tools/install_tv.sh --set-home` применяет эту настройку, `tools/restore_googletv.sh` возвращает Google TV одной командой (вручную: `adb shell pm enable --user 0 com.google.android.tvlauncher`).

Необязательно, на усмотрение владельца: снять ограничение после загрузки можно, выдав приложению разрешение на показ поверх других окон с компьютера: `adb shell appops set ru.polkatv.launcher SYSTEM_ALERT_WINDOW allow` (вернуть: `adb shell appops set ru.polkatv.launcher SYSTEM_ALERT_WINDOW default`). Для этого в манифесте должно быть объявлено `android.permission.SYSTEM_ALERT_WINDOW`; сейчас оно не объявлено, и ни скрипты, ни сборка эту команду не выполняют.

## Откат
`tools/restore_googletv.sh` возвращает Google TV как домашний экран. «Полка» остаётся установленной; удалить: `adb shell pm uninstall ru.polkatv.launcher`.

## Управление
- Плитки: OK — запуск, долгое нажатие — заменить/сдвинуть. Последняя плитка — все приложения.
- Погода: ←/→ — города, OK — подробный прогноз, долгое нажатие — добавить/удалить город.
- Иконки справа сверху: входы, уведомления, сеть, настройки ТВ, настройки Полки.
