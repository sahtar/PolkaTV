# PolkaTV («Полка»)

Домашний экран в духе Apple TV для Android TV: часы, погода для нескольких городов (OpenWeatherMap; поиск городов через геокодинг Open-Meteo), 4–6 плиток приложений по вашему выбору, служебные иконки (входы, уведомления, сеть, настройки) и фото-фон (12 видов Мальдив). Проверено на Xiaomi MiTV-MOEU1 (Android TV 14) и приставке Xiaomi Mi Box 3 (Android TV 9); минимальная версия — Android 9.

![Полка на Mi Box 3](docs/screenshots/home.jpg)

**English in brief.** PolkaTV is an Apple TV-style home screen (launcher) for Android TV 9+: a big clock, weather for several cities (OpenWeatherMap), 4–6 app tiles you pick yourself, the system shortcuts (inputs, notifications, network, settings) and a rotating photo background (12 Maldives photos from Pexels). Kotlin + Jetpack Compose for TV, no ads, no analytics, no accounts. Verified on a Xiaomi MiTV (Android 14) and a Mi Box 3 (Android 9). Grab the APK from [Releases](https://github.com/sahtar/PolkaTV/releases), `adb install -r` it, enter your free OpenWeatherMap key in Settings → Cities (scan the QR code with your phone and paste the key on the page it opens; or type it, or `adb shell am start -n ru.polkatv.launcher/.MainActivity --es owm_api_key KEY`) and make it the home app (`cmd package set-home-activity ru.polkatv.launcher/.MainActivity`; on Xiaomi firmware the Google TV launcher must be disabled, see below). Or build from source with `TV=<ip>:5555 tools/install_tv.sh --set-home`; Xiaomi firmware quirks (HOME priority, boot/standby handling) are documented in this README. The whole project was built in a conversation with Claude Code; [docs/HOWTO-with-claude.md](docs/HOWTO-with-claude.md) explains how to repeat it on your TV (in Russian).

Проект целиком сделан в диалоге с Claude Code — от поиска телевизора в сети до установки и проверки на устройстве. **Как повторить это на своём телевизоре: [docs/HOWTO-with-claude.md](docs/HOWTO-with-claude.md).** Спецификация: `docs/superpowers/specs/2026-10-24-polkatv-launcher-design.md`, план: `docs/superpowers/plans/2026-10-24-polkatv-launcher.md`. Лицензия — MIT.

## Быстрый старт: с нуля до «Полки» на экране

Нужны телевизор или приставка на Android TV 9+, компьютер (Mac, Windows или Linux) в той же домашней сети и 15 минут. Ничего собирать не придётся.

**Где что делается.** На телевизоре пультом выполняется только шаг 1 и подтверждение на шаге 3. Всё остальное — на компьютере: файл APK скачивается на компьютер, а команды, начинающиеся с `adb`, набираются в терминале компьютера (macOS и Linux: программа «Терминал»; Windows: «Командная строка» или PowerShell). Телевизор получает их по домашней сети, на нём ничего вводить не нужно.

### 1. Включите режим разработчика на телевизоре

1. Настройки → Система → Об устройстве (на старых прошивках: Настройки → Настройки устройства → Об устройстве).
2. Найдите пункт «Сборка» (Build) и нажмите на него семь раз подряд, пока не появится надпись «Вы стали разработчиком».
3. Вернитесь на шаг назад: появился раздел «Для разработчиков» (Developer options). Включите в нём «Отладка по USB». Если есть отдельный пункт «Отладка по сети» (Network debugging), включите и его — на Xiaomi он есть.
4. Там же, в Настройки → Сеть и интернет, посмотрите IP-адрес телевизора (вида 192.168.x.x). Он понадобится ниже.

### 2. Поставьте adb на компьютер

Это консольная утилита Android, через которую компьютер управляет телевизором по сети. Устанавливается один раз.

- macOS: `brew install android-platform-tools` (нужен [Homebrew](https://brew.sh)).
- Windows: скачайте [Platform Tools](https://developer.android.com/tools/releases/platform-tools), распакуйте архив, например в `C:\platform-tools`, и открывайте командную строку в этой папке (в Проводнике: правый клик по папке → «Открыть в терминале»); тогда команда `adb` будет находиться.
- Linux (Debian/Ubuntu): `sudo apt install adb`.

### 3. Подключитесь к телевизору

В терминале компьютера (адрес — из шага 1):

```bash
adb connect 192.168.x.x:5555
```

В первый раз на экране телевизора появится вопрос «Разрешить отладку по USB с этого компьютера?» — отметьте «Всегда разрешать» и нажмите ОК. Проверка: команда `adb devices` на компьютере показывает адрес телевизора со словом `device`. Если там `unauthorized`, подтверждение на экране ещё не нажато; если «No route to host», проверьте, что телевизор не ушёл в режим ожидания (в нём сеть выключается).

### 4. Установите «Полку»

На компьютере скачайте `PolkaTV-<версия>.apk` со страницы [Releases](https://github.com/sahtar/PolkaTV/releases) (обычно он попадает в папку «Загрузки»). Откройте терминал в папке со скачанным файлом (macOS: правый клик по папке в Finder → «Новый терминал в папке», или `cd ~/Downloads`; Windows: правый клик → «Открыть в терминале») и выполните, подставив имя файла:

```bash
adb install -r PolkaTV-<версия>.apk
```

Команда сама передаст файл на телевизор и установит его; через несколько секунд терминал напишет `Success`, и «Полка» появится в списке приложений телевизора. Если раньше ставили собственную сборку, сначала удалите её: `adb shell pm uninstall ru.polkatv.launcher` (подписи разные).

### 5. Задайте ключ погоды

1. Зарегистрируйтесь на [openweathermap.org](https://openweathermap.org) и возьмите ключ в разделе My API keys (бесплатный тариф; новый ключ начинает работать в течение часа-двух).
2. Откройте «Полку» → Настройки (иконка с ползунками справа вверху) → Города → Ключ OpenWeatherMap. На экране появится QR-код: отсканируйте его телефоном, вставьте ключ на открывшейся странице и нажмите «Отправить». Телевизор сохранит ключ сам; страница работает только в домашней сети и только пока открыт диалог.
3. Запасные пути: «Ввести ключ вручную» экранной клавиатурой телевизора или командой в терминале компьютера:
   ```bash
   adb shell am start -n ru.polkatv.launcher/.MainActivity --es owm_api_key ВАШ_КЛЮЧ
   ```
4. Там же, в разделе «Города», добавьте свои города; в разделе «Плитки» выберите 4–6 приложений для полки.

### 6. Сделайте «Полку» домашним экраном

В терминале компьютера:

```bash
adb shell cmd package set-home-activity ru.polkatv.launcher/.MainActivity
```

Нажмите Home на пульте. Если открылась «Полка» — готово. Если по-прежнему открывается прежний экран (так ведут себя прошивки Xiaomi: у их лаунчера приоритет выше), отключите лаунчер Google TV, это обратимо:

```bash
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

На Android 10 и новее, чтобы «Полка» открывалась сразу после включения телевизора, а не после нажатия Home, выдайте ей разрешение на запуск из фона (само наложение нигде не используется, подробности в разделе «Домашний экран»):

```bash
adb shell pm grant ru.polkatv.launcher android.permission.SYSTEM_ALERT_WINDOW
```

### Откат

Тоже в терминале компьютера:

```bash
adb shell pm enable --user 0 com.google.android.tvlauncher
adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
```

Удалить «Полку» целиком: `adb shell pm uninstall ru.polkatv.launcher`.

## Сборка и установка
- Требования: JDK 17+ (проверено на 21), Android SDK (platform 34, build-tools 34.0.0), `local.properties` с `sdk.dir`, свой ключ подписи в `keystore/` (не в git, см. ниже).
- Ключ подписи (один раз; debug и release подписываются им же, поэтому сборки взаимозаменяемы при установке поверх):
  ```bash
  mkdir -p keystore && keytool -genkeypair -v -keystore keystore/polkatv.jks -alias polka -keyalg RSA -keysize 2048 -validity 10000
  ```
  и файл `keystore/keystore.properties`:
  ```
  storeFile=polkatv.jks
  storePassword=<пароль хранилища>
  keyAlias=polka
  keyPassword=<пароль ключа>
  ```
- Прогноз погоды берётся из OpenWeatherMap (Open-Meteo-хост прогноза недоступен из сети ТВ). Ключ задаётся на устройстве (Настройки → Города → Ключ OpenWeatherMap: QR-код для телефона, ручной ввод или adb-команда из раздела выше) и хранится в настройках «Полки». Для собственной сборки ключ можно и вшить: строка `owm.apiKey=<ключ>` в `local.properties` (файл в `.gitignore`); ключ из настроек имеет приоритет. Публичный релиз собирается без вшитого ключа: `./gradlew -PowmApiKey= assembleRelease`. Без ключа на полке погоды будет надпись «Не задан ключ OpenWeatherMap».
- Лимиты бесплатного тарифа OpenWeatherMap: 60 запросов в минуту; лаунчер делает 2 запроса на город раз в 30 минут. Поиск городов по-прежнему использует геокодинг Open-Meteo.
- `tools/prepare_wallpapers.sh` — пересобрать обои из `design/wallpapers-src/`.
- Адрес устройства (IP в локальной сети и порт 5555) передаётся переменной `TV`, без неё скрипты не запускаются: `TV=<ip>:5555 tools/install_tv.sh`.
- `tools/install_tv.sh` — собрать release и установить; `tools/install_tv.sh --set-home` — ещё и сделать домашним экраном (см. «Домашний экран»). Скрипт сам решает по устройству, отключать ли лаунчер Google TV (только если предпочтительный HOME игнорируется) и выдавать ли разрешение на наложение (только на Android 10+).
- Минимальная версия Android — 9 (API 28), проверено на телевизоре MiTV-MOEU1 (Android 14) и приставке Mi Box 3 (Android 9).
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

Откат одной командой: `tools/restore_googletv.sh` — включает Google TV обратно, снимает разрешение (`pm revoke` + `appops set … default`) и возвращает ему Home. PatchWall (`com.mitv.tvhome.atv` и его сервис `com.mitv.tvhome.michannel`) отключён 2026-10-25 по решению владельца: прошивка больше не поднимает его при загрузке и пробуждении, «Полка» выходит сразу; кнопка PatchWall на пульте не работает. Вернуть: `adb shell pm enable --user 0 com.mitv.tvhome.atv && adb shell pm enable --user 0 com.mitv.tvhome.michannel`. Фоновые фото: с 1.1.1 — 12 видов Мальдив с Pexels (лицензия Pexels, ссылки в `design/wallpapers-src/SOURCES.txt`); до этого были кадры заставки Xiaomi.
## Откат для собственной сборки
`tools/restore_googletv.sh` возвращает Google TV как домашний экран. «Полка» остаётся установленной; удалить: `adb shell pm uninstall ru.polkatv.launcher`.

## Управление
- Плитки: OK — запуск, долгое нажатие — заменить/сдвинуть. Последняя плитка — все приложения.
- Погода: ←/→ — города, OK — подробный прогноз, долгое нажатие — добавить/удалить город.
- Иконки справа сверху: входы, уведомления, сеть, настройки ТВ, настройки Полки.

## Приставка Mi Box 3

С 1.1.0 «Полка» стоит и на приставке Xiaomi Mi Box 3 (MIBOX3, Android TV 9, armeabi-v7a, тот же экран 1920×1080 при плотности 320, 2 ГБ памяти; подключение по Wi-Fi). Отличия от телевизора:

- Прошивка приставки тоже даёт HOME-фильтру лаунчера Google приоритет 2, поэтому лаунчер Google TV отключён так же, как на телевизоре (`pm disable-user --user 0 com.google.android.tvlauncher`); откат — `TV=<ip>:5555 tools/restore_googletv.sh`. Панель уведомлений на Android TV 9 живёт в `com.google.android.tvrecommendations` и от лаунчера не зависит.
- На Android 9 нет запрета запуска активности из фона, поэтому разрешение `SYSTEM_ALERT_WINDOW` не выдаётся и не нужно: `BootReceiver` и `WakeReceiver` работают как есть (проверено перезагрузкой и сном/пробуждением; в спящем режиме приставка остаётся в сети).
- HDMI-входов у приставки нет: при пустом списке входов значок входов в шапке скрыт.
- PatchWall (`com.mitv.tvhome.atv`) на приставке остаётся включённым: прошивка держит его в фоне, но экран он не занимает.
- Настройки «Полки» (плитки, города погоды) хранятся на каждом устройстве отдельно; город для погоды на приставке добавляется в её настройках.
- Отладка по сети включается на приставке в настройках разработчика; при первом подключении приставка просит подтвердить отладку с этого компьютера.
- Чистка приставки от лишних фоновых процессов (телеметрия, PatchWall, Assistant, Mi Link, фон кинотеатров) описана в `docs/tv-cleanup/mibox_report.md`; применить/откатить — `docs/tv-cleanup/mibox_cleanup.sh` и `mibox_rollback.sh`.
