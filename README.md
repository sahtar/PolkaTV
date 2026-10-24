# PolkaTV («Полка»)

Домашний экран для Xiaomi MiTV-MOEU1 (Android TV 14) в стиле Apple TV: часы, погода для нескольких городов (Open-Meteo), 4–6 плиток приложений, служебные иконки, фото из заставки Xiaomi фоном.

Спецификация: `docs/superpowers/specs/2026-10-24-polkatv-launcher-design.md`. План: `docs/superpowers/plans/2026-10-24-polkatv-launcher.md`.

## Сборка и установка
- Требования: JDK 21, Android SDK (platform 34, build-tools 34.0.0), `keystore/keystore.properties` + `keystore/polkatv.jks` (не в git), `local.properties` с `sdk.dir`.
- Источник прогноза погоды сейчас переключается на OpenWeatherMap; для сборки потребуется ключ: строка `owm.apiKey=…` в `local.properties`.
- `tools/prepare_wallpapers.sh` — пересобрать обои из `design/wallpapers-src/`.
- `tools/install_tv.sh` — собрать debug и установить; `tools/install_tv.sh --set-home` — ещё и сделать домашним экраном (см. «Домашний экран»).
- `./gradlew assembleRelease` — релизная сборка (подписана тем же ключом, поэтому ставится поверх debug: `adb install -r app/build/outputs/apk/release/app-release.apk`).

## Домашний экран
На прошивке этого Xiaomi у HOME-фильтра лаунчера Google TV (`com.google.android.tvlauncher`) приоритет 2, у «Полки» — 0 (сторонние приложения поднять приоритет не могут). Поэтому одного `cmd package set-home-activity ru.polkatv.launcher/.MainActivity` недостаточно: роль HOME и предпочтительная активность записываются, но система всё равно открывает Google TV по кнопке Home и `resolve-activity` показывает его же. Кнопка Home откроет «Полку» только если отключить лаунчер Google TV:

```
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Вернуть: `adb shell pm enable com.google.android.tvlauncher`.

Этот шаг ожидает решения владельца и пока не выполнен: сейчас домашний экран — Google TV, «Полка» запускается из его списка приложений. Спецификация (п. 6.1) исходно требовала не отключать Google TV; решение об отступлении от неё за владельцем.

## Откат
`tools/restore_googletv.sh` возвращает Google TV как домашний экран. «Полка» остаётся установленной; удалить: `adb shell pm uninstall ru.polkatv.launcher`.

## Управление
- Плитки: OK — запуск, долгое нажатие — заменить/сдвинуть. Последняя плитка — все приложения.
- Погода: ←/→ — города, OK — подробный прогноз, долгое нажатие — добавить/удалить город.
- Иконки справа сверху: входы, уведомления, сеть, настройки ТВ, настройки Полки.
