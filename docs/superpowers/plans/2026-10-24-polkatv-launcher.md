# PolkaTV («Полка») Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build «Полка», an Android TV home-screen launcher for the Xiaomi MiTV-MOEU1 (Android 14) with a clock, a multi-city weather shelf, 4–6 user-chosen app tiles, system shortcut icons and a rotating photo background, and install it as the device's HOME app.

**Architecture:** Single Gradle module `app`, Kotlin + Jetpack Compose + Compose for TV. Five platform-facing units (`apps`, `weather`, `wallpaper`, `settings`, `system`) expose Kotlin APIs/Flows; Compose screens under `ui/` consume them through ViewModels. Persistence is DataStore Preferences (one JSON blob) plus a JSON file cache for weather. No DI framework: a hand-built `AppContainer` on the `Application` object.

**Tech Stack:** Gradle 8.9 (already cached on the Mac), AGP 8.7.3, Kotlin 2.0.21 (with Compose compiler plugin and kotlinx.serialization plugin), Compose BOM 2024.09.03, `androidx.tv:tv-material:1.0.0`, Navigation Compose 2.8.3, Lifecycle 2.8.7, DataStore Preferences 1.1.1, OkHttp 4.12.0, kotlinx-serialization-json 1.7.3, kotlinx-coroutines 1.9.0, Palette 1.0.0, JUnit 4.13.2, OkHttp MockWebServer 4.12.0. compileSdk 34, minSdk 30, targetSdk 34.

Spec: `docs/superpowers/specs/2026-10-24-polkatv-launcher-design.md`. Read it once before starting; every task below cites the spec sections it implements.

## Global Constraints

- Target device only: Xiaomi MiTV-MOEU1, Android 14 (API 34), armeabi-v7a, UI rendered at 1920×1080, density 320, locale ru-RU, D-pad only (no touch). ADB over network at `192.168.100.2:5555`.
- Package / applicationId: `ru.polkatv.launcher`. App label: «Полка» (ru), "Polka" (en).
- compileSdk 34, minSdk 30, targetSdk 34. Do not add libraries that require compileSdk 35 (e.g. `androidx.core:core` 1.15+). Pin `androidx.core:core-ktx:1.13.1`.
- Build toolchain: `./gradlew` wrapper pinned to Gradle 8.9; JDK is the JetBrains Runtime 21 at `/Users/konstantinsakhtaridis/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home`; SDK at `/Users/konstantinsakhtaridis/Library/Android/sdk` (platform android-34, build-tools 34.0.0 are installed; nothing else must be downloaded from the SDK).
- UI language: Russian strings in `values/strings.xml` (default), English in `values-en/strings.xml`. Never hard-code user-visible text in Kotlin.
- All user-visible dimensions in dp/sp tuned for 1920×1080 at density 320 (1 dp = 2 px). Safe-zone padding 48 dp horizontally, 40 dp vertically.
- No analytics, no crash reporting, no third-party services other than Open-Meteo.
- Wallpapers: 23 originals in `design/wallpapers-src/` (wp_01 … wp_24 minus wp_19, a byte-identical duplicate of wp_18) (4K). The APK ships 1920×1080 JPEG copies in `app/src/main/assets/wallpapers/wp_01.jpg … wp_24.jpg` except `wp_19.jpg`, produced by `tools/prepare_wallpapers.sh`. Never load the 4K originals at runtime.
- Both debug and release builds are signed with the same keystore `keystore/polkatv.jks` (git-ignored) so a release APK installs over a debug one without uninstalling.
- Commit after every task with the message style `feat: …`, `test: …`, `chore: …`; end every commit message with `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.
- Work on branch `design/polkatv-spec` (the user chose it); do not touch `main`.
- Device verification commands (used throughout): `adb connect 192.168.100.2:5555`, `adb -s 192.168.100.2:5555 install -r <apk>`, `adb -s 192.168.100.2:5555 shell am start -n ru.polkatv.launcher/.MainActivity`, `adb -s 192.168.100.2:5555 exec-out screencap -p > /tmp/polka.png`, `adb -s 192.168.100.2:5555 shell input keyevent <KEYCODE>` (DPAD_UP=19, DPAD_DOWN=20, DPAD_LEFT=21, DPAD_RIGHT=22, DPAD_CENTER=23, BACK=4, HOME=3, MENU=82). A long press is `input keyevent --longpress 23`.

---

## File Structure

```
PolkaTV/
├─ settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew, gradle/wrapper/*
├─ local.properties                      (git-ignored) sdk.dir
├─ keystore/polkatv.jks, keystore/keystore.properties   (git-ignored)
├─ tools/prepare_wallpapers.sh           4K WebP → 1080p JPEG assets (sips)
├─ tools/install_tv.sh                   build debug, install, set as HOME
├─ tools/restore_googletv.sh             set Google TV back as HOME
├─ design/wallpapers-src/*.webp          sources (already committed)
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ assets/wallpapers/wp_01.jpg … wp_24.jpg
      ├─ res/values/strings.xml, res/values-en/strings.xml, res/values/themes.xml
      ├─ res/drawable/banner.xml (320×180 leanback banner), res/mipmap-*/ic_launcher
      └─ java/ru/polkatv/launcher/
         ├─ PolkaApplication.kt         Application; owns AppContainer
         ├─ AppContainer.kt             wires repositories/controllers (manual DI)
         ├─ MainActivity.kt             HOME activity, NavHost
         ├─ settings/Models.kt          City, Prefs, DimLevel, ClockFormat, DEFAULT_TILES
         ├─ settings/TileOps.kt         pure tile list operations
         ├─ settings/SettingsRepository.kt  DataStore-backed Prefs flow
         ├─ apps/TvApp.kt               TvApp model
         ├─ apps/AppsRepository.kt      PackageManager queries, launch intents, package-change receiver
         ├─ apps/TileArtLoader.kt       banner / icon+palette gradient loading, LRU cache
         ├─ wallpaper/WallpaperSequence.kt   shuffled cycle of indices (pure)
         ├─ wallpaper/WallpaperLoader.kt     asset decoding to ImageBitmap
         ├─ wallpaper/WallpaperController.kt timer, current/next frames, trim
         ├─ weather/OpenMeteoDto.kt     @Serializable DTOs
         ├─ weather/OpenMeteoApi.kt     OkHttp calls (forecast, geocoding)
         ├─ weather/WeatherModels.kt    domain: Weather, CurrentWeather, HourForecast, DayForecast, WindDir
         ├─ weather/WmoCodes.kt         WMO code → WmoKind (description res + icon)
         ├─ weather/Units.kt            hPa→mmHg, degrees→WindDir
         ├─ weather/WeatherMapper.kt    DTO → domain
         ├─ weather/WeatherCache.kt     per-city JSON file cache
         ├─ weather/WeatherRepository.kt   fresh-or-cached policy, refresh-all
         ├─ system/SystemActions.kt     open settings / notifications / network / app info
         ├─ system/NetworkStatus.kt     NetState flow
         ├─ system/InputsController.kt  TvInputManager list + switch strategies
         └─ ui/
            ├─ theme/Theme.kt, theme/Dimens.kt
            ├─ PolkaApp.kt              NavHost + routes
            ├─ home/HomeViewModel.kt, home/HomeState.kt
            ├─ home/HomeScreen.kt       three tiers + focus wiring
            ├─ home/Header.kt           clock, date, system icons
            ├─ home/AppRow.kt, home/AppTile.kt, home/TileMenuDialog.kt
            ├─ home/WeatherShelf.kt     card, hourly strip, dots, city menu
            ├─ apps/AllAppsScreen.kt, apps/AppPickerDialog.kt
            ├─ weather/WeatherDetailScreen.kt, weather/AddCityDialog.kt, weather/WeatherIcons.kt
            ├─ inputs/InputsDialog.kt
            ├─ settings/SettingsScreen.kt (+ sections in the same file family)
            └─ common/PolkaDialog.kt, common/Formatters.kt
   └─ src/test/java/ru/polkatv/launcher/…   JUnit tests mirroring the packages above
   └─ src/test/resources/openmeteo/forecast_moscow.json, geocoding_moskva.json
```

---

### Task 1: Project skeleton that builds, installs and shows «Полка» on the TV

Implements spec §2, §5.1, §10 (build), part of §6.1 (LEANBACK entry). Deliverable: a signed debug APK that installs on the TV and shows a dark screen with the text «Полка».

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `local.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/drawable/banner.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/values/ic_launcher_background.xml`, `app/src/main/java/ru/polkatv/launcher/PolkaApplication.kt`, `app/src/main/java/ru/polkatv/launcher/AppContainer.kt`, `app/src/main/java/ru/polkatv/launcher/MainActivity.kt`, `app/src/main/java/ru/polkatv/launcher/ui/theme/Theme.kt`, `app/src/main/java/ru/polkatv/launcher/ui/theme/Dimens.kt`, `app/src/test/java/ru/polkatv/launcher/SmokeTest.kt`, `keystore/keystore.properties`, `keystore/polkatv.jks`, `tools/install_tv.sh`, `tools/restore_googletv.sh`
- Modify: `.gitignore` (already ignores `/keystore/`, `*.jks`, `/local.properties`)

**Interfaces:**
- Produces: `class PolkaApplication : Application { val container: AppContainer }`, `class AppContainer(context: Context)` (empty now; later tasks add fields), `object Dimens` (safe-zone paddings), `@Composable fun PolkaTheme(content: @Composable () -> Unit)`.

- [ ] **Step 1: Generate the Gradle wrapper from the cached Gradle 8.9**

```bash
cd /Users/konstantinsakhtaridis/Documents/GitHub/PolkaTV
GRADLE=/Users/konstantinsakhtaridis/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle
"$GRADLE" wrapper --gradle-version 8.9 --distribution-type bin --no-daemon
./gradlew --version
```
Expected: `Gradle 8.9`, `Launcher JVM: 21.0.11`. Files created: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 2: Write `local.properties` and `gradle.properties`**

`local.properties` (git-ignored):
```properties
sdk.dir=/Users/konstantinsakhtaridis/Library/Android/sdk
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.java.home=/Users/konstantinsakhtaridis/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 3: Write the version catalog `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
composeBom = "2024.09.03"
tvMaterial = "1.0.0"
activityCompose = "1.9.3"
navigation = "2.8.3"
lifecycle = "2.8.7"
datastore = "1.1.1"
coreKtx = "1.13.1"
palette = "1.0.0"
okhttp = "4.12.0"
serialization = "1.7.3"
coroutines = "1.9.0"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-palette = { module = "androidx.palette:palette-ktx", version.ref = "palette" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
tv-material = { module = "androidx.tv:tv-material", version.ref = "tvMaterial" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 4: Write `settings.gradle.kts` and root `build.gradle.kts`**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PolkaTV"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 5: Create the signing keystore (once, git-ignored)**

```bash
cd /Users/konstantinsakhtaridis/Documents/GitHub/PolkaTV && mkdir -p keystore
PW=$(openssl rand -base64 18)
keytool -genkeypair -v -keystore keystore/polkatv.jks -storetype PKCS12 -alias polkatv \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass "$PW" -keypass "$PW" \
  -dname "CN=PolkaTV, OU=Home, O=PolkaTV, L=Home, C=RU"
cat > keystore/keystore.properties <<EOF
storeFile=../keystore/polkatv.jks
storePassword=$PW
keyAlias=polkatv
keyPassword=$PW
EOF
git check-ignore keystore/polkatv.jks keystore/keystore.properties
```
Expected: both paths printed by `git check-ignore` (they are ignored). Never print `$PW` in chat.

- [ ] **Step 6: Write `app/build.gradle.kts`**

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "ru.polkatv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.polkatv.launcher"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        resourceConfigurations += listOf("ru", "en")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("polka") {
                storeFile = rootProject.file("keystore/" + File(keystoreProps["storeFile"] as String).name)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("polka") ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("polka") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.tv.material)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
```

Also create an empty `app/proguard-rules.pro` containing:
```
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class ru.polkatv.launcher.** { *** Companion; }
-keepclasseswithmembers class ru.polkatv.launcher.** { kotlinx.serialization.KSerializer serializer(...); }
```

- [ ] **Step 7: Write the manifest (spec §5.1)**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-feature android:name="android.software.leanback" android:required="true" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <application
        android:name=".PolkaApplication"
        android:allowBackup="false"
        android:banner="@drawable/banner"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="false"
        android:theme="@style/Theme.Polka">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:excludeFromRecents="true"
            android:stateNotNeeded="true"
            android:screenOrientation="landscape"
            android:configChanges="locale|layoutDirection|uiMode|screenSize|smallestScreenSize|screenLayout|density|fontScale"
            android:theme="@style/Theme.Polka">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Write resources**

`app/src/main/res/values/strings.xml` (Russian, default):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Полка</string>
    <string name="hello_placeholder">Полка</string>
</resources>
```
`app/src/main/res/values-en/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Polka</string>
    <string name="hello_placeholder">Polka</string>
</resources>
```
`app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Polka" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">#000000</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```
`app/src/main/res/drawable/banner.xml` (320×180 leanback banner):
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="135" android:startColor="#1D2A44" android:endColor="#060912" />
            <size android:width="320dp" android:height="180dp" />
        </shape>
    </item>
    <item android:gravity="center">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="6dp" />
            <size android:width="120dp" android:height="14dp" />
        </shape>
    </item>
</layer-list>
```
`app/src/main/res/values/ic_launcher_background.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#1D2A44</color>
</resources>
```
`app/src/main/res/drawable/ic_launcher_foreground.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M24,52 h60 a4,4 0 0 1 4,4 v4 a4,4 0 0 1 -4,4 h-60 a4,4 0 0 1 -4,-4 v-4 a4,4 0 0 1 4,-4z" />
</vector>
```
`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 9: Write the Application, container, theme and activity**

`app/src/main/java/ru/polkatv/launcher/PolkaApplication.kt`:
```kotlin
package ru.polkatv.launcher

import android.app.Application

class PolkaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```
`app/src/main/java/ru/polkatv/launcher/AppContainer.kt`:
```kotlin
package ru.polkatv.launcher

import android.content.Context

/** Hand-wired dependency container. Later tasks add repositories here. */
class AppContainer(val appContext: Context)
```
`app/src/main/java/ru/polkatv/launcher/ui/theme/Dimens.kt`:
```kotlin
package ru.polkatv.launcher.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    val safeH = 48.dp
    val safeV = 40.dp
    val tierGap = 28.dp
    val tileCorner = 16.dp
    val cardCorner = 24.dp
    val iconButton = 44.dp
    val clockSize = 64.sp
    val dateSize = 20.sp
    val tempSize = 72.sp
}
```
`app/src/main/java/ru/polkatv/launcher/ui/theme/Theme.kt`:
```kotlin
package ru.polkatv.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val GlassFill = Color(0x73141828)      // rgba(20,24,40,0.45)
val GlassBorder = Color(0x2EFFFFFF)    // white 18%
val TextPrimary = Color.White
val TextSecondary = Color(0xCCFFFFFF)  // white 80%
val TextTertiary = Color(0x99FFFFFF)   // white 60%

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PolkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color(0xFF111111),
            surface = Color(0xFF0B1020),
            onSurface = TextPrimary,
            background = Color(0xFF060912),
            onBackground = TextPrimary,
        ),
        content = content,
    )
}
```
`app/src/main/java/ru/polkatv/launcher/MainActivity.kt`:
```kotlin
package ru.polkatv.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import ru.polkatv.launcher.ui.theme.PolkaTheme

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PolkaTheme {
                Box(Modifier.fillMaxSize().background(Color(0xFF060912)), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.hello_placeholder), fontSize = 64.sp, color = Color.White)
                }
            }
        }
    }
}
```

- [ ] **Step 10: Write a smoke unit test and run the full build**

`app/src/test/java/ru/polkatv/launcher/SmokeTest.kt`:
```kotlin
package ru.polkatv.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test fun `test infrastructure runs`() { assertEquals(4, 2 + 2) }
}
```
Run:
```bash
cd /Users/konstantinsakhtaridis/Documents/GitHub/PolkaTV && ./gradlew --no-daemon testDebugUnitTest assembleDebug 2>&1 | tail -15
ls -la app/build/outputs/apk/debug/app-debug.apk
```
Expected: `BUILD SUCCESSFUL`, APK exists (~8–12 MB with icons-extended in debug). First run downloads Maven artifacts (several minutes). If the build fails with `requires compileSdk 35`, some dependency drifted: check `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -E "androidx.core:core"` and add `implementation("androidx.core:core:1.13.1")` plus a `resolutionStrategy.force` for it; do not raise compileSdk.

- [ ] **Step 11: Install on the TV, launch from the Leanback entry, screenshot**

```bash
TV=192.168.100.2:5555; adb connect $TV
adb -s $TV install -r app/build/outputs/apk/debug/app-debug.apk
adb -s $TV shell am start -n ru.polkatv.launcher/.MainActivity
sleep 3; adb -s $TV exec-out screencap -p > /tmp/polka_task1.png
adb -s $TV shell dumpsys activity activities | grep topResumedActivity
```
Expected: `Success` on install; topResumedActivity is `ru.polkatv.launcher/.MainActivity`; screenshot shows dark screen with white «Полка». Also confirm the HOME chooser now lists us: `adb -s $TV shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME` includes `ru.polkatv.launcher/.MainActivity`. Do NOT set it as home yet.

- [ ] **Step 12: Write the install/restore scripts**

`tools/install_tv.sh`:
```bash
#!/bin/bash
# Build debug APK, install on the Xiaomi TV and (with --set-home) make it the HOME app.
set -euo pipefail
cd "$(dirname "$0")/.."
TV="${TV:-192.168.100.2:5555}"
adb connect "$TV" >/dev/null
./gradlew --no-daemon -q assembleDebug
adb -s "$TV" install -r app/build/outputs/apk/debug/app-debug.apk
if [ "${1:-}" = "--set-home" ]; then
  adb -s "$TV" shell cmd package set-home-activity ru.polkatv.launcher/.MainActivity
fi
adb -s "$TV" shell am start -n ru.polkatv.launcher/.MainActivity >/dev/null
echo "Installed. HOME now: $(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
```
`tools/restore_googletv.sh`:
```bash
#!/bin/bash
# Make Google TV the HOME app again (Polka stays installed).
set -euo pipefail
TV="${TV:-192.168.100.2:5555}"
adb connect "$TV" >/dev/null
adb -s "$TV" shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
adb -s "$TV" shell input keyevent 3
echo "HOME now: $(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
```
`chmod +x tools/*.sh`.

- [ ] **Step 13: Commit**

```bash
git add -A && git status --short   # must NOT list keystore/ or local.properties
git commit -m "chore: Android TV project skeleton (Compose for TV, signed debug build)

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 2: Wallpaper assets and rotating photo background

Implements spec §4.5 and the asset pipeline from §5.2. Deliverable: the activity shows the 24 photos crossfading with the dim gradient; a unit-tested shuffle sequence.

**Files:**
- Create: `tools/prepare_wallpapers.sh`, `app/src/main/assets/wallpapers/wp_01.jpg … wp_24.jpg`, `app/src/main/java/ru/polkatv/launcher/wallpaper/WallpaperSequence.kt`, `app/src/main/java/ru/polkatv/launcher/wallpaper/WallpaperLoader.kt`, `app/src/main/java/ru/polkatv/launcher/wallpaper/WallpaperController.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/WallpaperBackground.kt`, `app/src/test/java/ru/polkatv/launcher/wallpaper/WallpaperSequenceTest.kt`
- Modify: `AppContainer.kt`, `MainActivity.kt`

**Interfaces:**
- Produces: `class WallpaperSequence(count: Int, random: Random) { fun next(): Int }`; `class WallpaperLoader(context) { val count: Int; suspend fun load(index: Int): ImageBitmap }`; `class WallpaperController(loader, intervalMinutes: StateFlow<Int>, scope) { val current: StateFlow<ImageBitmap?>; fun start(); fun stop(); fun trimMemory() }`; `enum class DimLevel` is defined in Task 3 — for this task `WallpaperBackground(bitmap: ImageBitmap?, dimStrength: Float)` takes a plain float (0.5 light, 1.0 medium).

- [ ] **Step 1: Write the asset preparation script and run it**

`tools/prepare_wallpapers.sh`:
```bash
#!/bin/bash
# Downscale design/wallpapers-src/*.webp (4K) to 1920x1080 JPEG assets for the APK.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=app/src/main/assets/wallpapers
mkdir -p "$OUT"
for src in design/wallpapers-src/wp_*.webp; do
  name=$(basename "${src%.webp}")
  sips -z 1080 1920 -s format jpeg -s formatOptions 85 "$src" --out "$OUT/$name.jpg" >/dev/null
done
ls "$OUT" | wc -l; du -sh "$OUT"
```
Run `chmod +x tools/prepare_wallpapers.sh && tools/prepare_wallpapers.sh`.
Expected: `24` files, total roughly 7–10 MB. Verify one: `sips -g pixelWidth -g pixelHeight app/src/main/assets/wallpapers/wp_01.jpg` → 1920 × 1080.

- [ ] **Step 2: Write the failing test for `WallpaperSequence`**

`app/src/test/java/ru/polkatv/launcher/wallpaper/WallpaperSequenceTest.kt`:
```kotlin
package ru.polkatv.launcher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class WallpaperSequenceTest {
    @Test fun `first cycle is a permutation of all indices`() {
        val seq = WallpaperSequence(count = 24, random = Random(1))
        val first = List(24) { seq.next() }
        assertEquals((0 until 24).toSet(), first.toSet())
    }

    @Test fun `second cycle is again a permutation and does not repeat the last frame`() {
        val seq = WallpaperSequence(count = 24, random = Random(7))
        val first = List(24) { seq.next() }
        val second = List(24) { seq.next() }
        assertEquals((0 until 24).toSet(), second.toSet())
        assertNotEquals(first.last(), second.first())
    }

    @Test fun `single wallpaper always returns zero`() {
        val seq = WallpaperSequence(count = 1, random = Random(3))
        repeat(5) { assertEquals(0, seq.next()) }
    }

    @Test fun `no repeats across many cycles boundary check`() {
        val seq = WallpaperSequence(count = 5, random = Random(11))
        var prev = seq.next()
        repeat(200) { val n = seq.next(); assertNotEquals(prev, n); prev = n }
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.wallpaper.WallpaperSequenceTest"`
Expected: compilation FAILS with `Unresolved reference: WallpaperSequence`.

- [ ] **Step 4: Implement `WallpaperSequence`**

`app/src/main/java/ru/polkatv/launcher/wallpaper/WallpaperSequence.kt`:
```kotlin
package ru.polkatv.launcher.wallpaper

import kotlin.random.Random

/**
 * Yields indices 0 until count in a random order, then reshuffles for the next cycle.
 * Guarantees the first index of a new cycle differs from the last index of the previous one
 * (when count > 1).
 */
class WallpaperSequence(private val count: Int, private val random: Random = Random.Default) {
    private var order: List<Int> = emptyList()
    private var pos = 0
    private var last = -1

    fun next(): Int {
        require(count > 0) { "count must be positive" }
        if (count == 1) return 0
        if (pos >= order.size) {
            var shuffled = (0 until count).shuffled(random)
            if (shuffled.first() == last) shuffled = shuffled.drop(1) + shuffled.first()
            order = shuffled
            pos = 0
        }
        last = order[pos++]
        return last
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.wallpaper.WallpaperSequenceTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 6: Implement the loader and controller**

`app/src/main/java/ru/polkatv/launcher/wallpaper/WallpaperLoader.kt`:
```kotlin
package ru.polkatv.launcher.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperLoader(private val context: Context, private val targetWidth: Int = 1920, private val targetHeight: Int = 1080) {
    private val names: List<String> =
        (context.assets.list("wallpapers") ?: emptyArray()).filter { it.endsWith(".jpg") }.sorted()

    val count: Int get() = names.size

    suspend fun load(index: Int): ImageBitmap = withContext(Dispatchers.IO) {
        val name = names[index]
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth && bounds.outHeight / (sample * 2) >= targetHeight) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = context.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("cannot decode $name")
        bmp.asImageBitmap()
    }
}
```
`app/src/main/java/ru/polkatv/launcher/wallpaper/WallpaperController.kt`:
```kotlin
package ru.polkatv.launcher.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Keeps at most two decoded frames (current + preloaded next). intervalMinutes == 0 disables rotation.
 */
class WallpaperController(
    private val loader: WallpaperLoader,
    private val intervalMinutes: StateFlow<Int>,
    private val scope: CoroutineScope,
    random: Random = Random.Default,
) {
    private val sequence = WallpaperSequence(loader.count.coerceAtLeast(1), random)
    private val _current = MutableStateFlow<ImageBitmap?>(null)
    val current: StateFlow<ImageBitmap?> = _current.asStateFlow()

    private var next: ImageBitmap? = null
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true || loader.count == 0) return
        job = scope.launch {
            if (_current.value == null) _current.value = loader.load(sequence.next())
            while (isActive) {
                val minutes = intervalMinutes.value
                if (minutes <= 0) { delay(30_000); continue }
                if (next == null) next = runCatching { loader.load(sequence.next()) }.getOrNull()
                delay(minutes * 60_000L)
                val n = next ?: continue
                next = null
                _current.value = n
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    /** Called from Activity.onTrimMemory: drop the preloaded frame. */
    fun trimMemory() { next = null }
}
```
`app/src/main/java/ru/polkatv/launcher/ui/home/WallpaperBackground.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

/** dimStrength: 1.0 = medium (0.35/0.20/0.65 black), 0.5 = light. */
@Composable
fun WallpaperBackground(bitmap: ImageBitmap?, dimStrength: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xFF060912))) {
        Crossfade(targetState = bitmap, animationSpec = tween(1500), label = "wallpaper") { bmp ->
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f * dimStrength),
                    0.4f to Color.Black.copy(alpha = 0.20f * dimStrength),
                    1f to Color.Black.copy(alpha = 0.65f * dimStrength),
                )
            )
        )
    }
}
```

- [ ] **Step 7: Wire into the container and the activity (temporary 1-minute interval to see it work)**

`AppContainer.kt` becomes:
```kotlin
package ru.polkatv.launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import ru.polkatv.launcher.wallpaper.WallpaperController
import ru.polkatv.launcher.wallpaper.WallpaperLoader

class AppContainer(val appContext: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val wallpaperInterval = MutableStateFlow(1)   // minutes; Task 3 replaces this with the Prefs flow
    val wallpapers = WallpaperController(WallpaperLoader(appContext), wallpaperInterval, appScope)
}
```
`MainActivity.kt` `setContent` body becomes:
```kotlin
val container = (application as PolkaApplication).container
setContent {
    PolkaTheme {
        val bmp by container.wallpapers.current.collectAsStateWithLifecycle()
        Box(Modifier.fillMaxSize()) {
            WallpaperBackground(bitmap = bmp, dimStrength = 1f)
            Text(text = stringResource(R.string.hello_placeholder), fontSize = 64.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}
```
plus lifecycle hooks in the activity class:
```kotlin
override fun onStart() { super.onStart(); (application as PolkaApplication).container.wallpapers.start() }
override fun onStop() { (application as PolkaApplication).container.wallpapers.stop(); super.onStop() }
override fun onTrimMemory(level: Int) { super.onTrimMemory(level); (application as PolkaApplication).container.wallpapers.trimMemory() }
```
Imports needed: `androidx.lifecycle.compose.collectAsStateWithLifecycle`, `androidx.compose.runtime.getValue`, `ru.polkatv.launcher.ui.home.WallpaperBackground`.

- [ ] **Step 8: Build, install, verify rotation and memory**

```bash
tools/install_tv.sh
sleep 5; adb -s 192.168.100.2:5555 exec-out screencap -p > /tmp/polka_wp1.png
sleep 70; adb -s 192.168.100.2:5555 exec-out screencap -p > /tmp/polka_wp2.png
adb -s 192.168.100.2:5555 shell dumpsys meminfo ru.polkatv.launcher | grep -E "TOTAL PSS|Graphics"
```
Expected: two screenshots show different photos with a darkened gradient and the white «Полка»; TOTAL PSS under ~120 MB. Open both PNGs with the Read tool to confirm visually.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: rotating photo background from 24 bundled wallpapers

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 3: Settings model, tile operations and DataStore repository

Implements spec §4.8 data model, §5 "Хранение". Deliverable: unit-tested `Prefs` codec and `TileOps`; `SettingsRepository` wired into the container, wallpaper interval now read from prefs.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/settings/Models.kt`, `app/src/main/java/ru/polkatv/launcher/settings/TileOps.kt`, `app/src/main/java/ru/polkatv/launcher/settings/PrefsCodec.kt`, `app/src/main/java/ru/polkatv/launcher/settings/SettingsRepository.kt`, `app/src/test/java/ru/polkatv/launcher/settings/TileOpsTest.kt`, `app/src/test/java/ru/polkatv/launcher/settings/PrefsCodecTest.kt`
- Modify: `AppContainer.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class City(val name: String, val region: String? = null, val country: String? = null, val latitude: Double, val longitude: Double, val timezone: String? = null) { val key: String; val displayName: String }`
  - `@Serializable enum class DimLevel { LIGHT, MEDIUM; val strength: Float }` (0.5f / 1.0f)
  - `@Serializable enum class ClockFormat { SYSTEM, H24, H12 }`
  - `@Serializable data class Prefs(val tiles: List<String?>, val cities: List<City>, val selectedCity: Int, val wallpaperIntervalMinutes: Int, val dimLevel: DimLevel, val showDate: Boolean, val clockFormat: ClockFormat)` with defaults.
  - `val DEFAULT_TILES: List<String?>`
  - `object TileOps { fun replace(tiles, index, pkg: String?): List<String?>; fun move(tiles, index, direction: Int): List<String?>; fun resize(tiles, newSize: Int): List<String?>; fun addToFirstEmpty(tiles, pkg): List<String?>? }`
  - `object PrefsCodec { fun encode(p: Prefs): String; fun decode(s: String?): Prefs }`
  - `class SettingsRepository(dataStore: DataStore<Preferences>) { val prefs: Flow<Prefs>; suspend fun update(transform: (Prefs) -> Prefs) }`
  - `val Context.polkaDataStore: DataStore<Preferences>`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/ru/polkatv/launcher/settings/TileOpsTest.kt`:
```kotlin
package ru.polkatv.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileOpsTest {
    private val t = listOf<String?>("a", "b", null, "d", "e")

    @Test fun `replace sets package at index`() {
        assertEquals(listOf("a", "x", null, "d", "e"), TileOps.replace(t, 1, "x"))
        assertEquals(listOf("a", "b", null, "d", null), TileOps.replace(t, 4, null))
    }

    @Test fun `replace ignores out of range index`() {
        assertEquals(t, TileOps.replace(t, 9, "x"))
        assertEquals(t, TileOps.replace(t, -1, "x"))
    }

    @Test fun `move swaps with neighbour`() {
        assertEquals(listOf("b", "a", null, "d", "e"), TileOps.move(t, 1, -1))
        assertEquals(listOf("a", null, "b", "d", "e"), TileOps.move(t, 1, +1))
    }

    @Test fun `move at edges is a no-op`() {
        assertEquals(t, TileOps.move(t, 0, -1))
        assertEquals(t, TileOps.move(t, 4, +1))
    }

    @Test fun `resize pads with null or truncates, clamped to 4..6`() {
        assertEquals(listOf("a", "b", null, "d", "e", null), TileOps.resize(t, 6))
        assertEquals(listOf("a", "b", null, "d"), TileOps.resize(t, 4))
        assertEquals(4, TileOps.resize(t, 1).size)
        assertEquals(6, TileOps.resize(t, 10).size)
    }

    @Test fun `addToFirstEmpty fills the first null and returns null when full`() {
        assertEquals(listOf("a", "b", "x", "d", "e"), TileOps.addToFirstEmpty(t, "x"))
        assertNull(TileOps.addToFirstEmpty(listOf("a", "b", "c", "d"), "x"))
    }

    @Test fun `addToFirstEmpty does not duplicate an existing package`() {
        assertEquals(t, TileOps.addToFirstEmpty(t, "a"))
    }
}
```
`app/src/test/java/ru/polkatv/launcher/settings/PrefsCodecTest.kt`:
```kotlin
package ru.polkatv.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsCodecTest {
    @Test fun `round trip keeps every field`() {
        val p = Prefs(
            tiles = listOf("a", null, "c", "d"),
            cities = listOf(City("Москва", "Москва", "Россия", 55.7558, 37.6173, "Europe/Moscow")),
            selectedCity = 0,
            wallpaperIntervalMinutes = 15,
            dimLevel = DimLevel.LIGHT,
            showDate = false,
            clockFormat = ClockFormat.H12,
        )
        assertEquals(p, PrefsCodec.decode(PrefsCodec.encode(p)))
    }

    @Test fun `null or garbage yields defaults`() {
        assertEquals(Prefs(), PrefsCodec.decode(null))
        assertEquals(Prefs(), PrefsCodec.decode("{not json"))
    }

    @Test fun `unknown keys are ignored and missing keys take defaults`() {
        val decoded = PrefsCodec.decode("""{"tiles":["x","y","z","w"],"futureField":1}""")
        assertEquals(listOf("x", "y", "z", "w"), decoded.tiles)
        assertEquals(5, decoded.wallpaperIntervalMinutes)
    }

    @Test fun `city key is stable and display name adds region when different`() {
        val c = City("Ростов-на-Дону", "Ростовская область", "Россия", 47.2357, 39.7015)
        assertEquals("47.2357_39.7015", c.key)
        assertEquals("Ростов-на-Дону, Ростовская область", c.displayName)
        assertEquals("Москва", City("Москва", "Москва", "Россия", 55.7558, 37.6173).displayName)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.settings.*"`
Expected: compilation FAILS (unresolved `TileOps`, `Prefs`, `PrefsCodec`, `City`).

- [ ] **Step 3: Implement models, TileOps and codec**

`app/src/main/java/ru/polkatv/launcher/settings/Models.kt`:
```kotlin
package ru.polkatv.launcher.settings

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class City(
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
) {
    val key: String get() = String.format(Locale.US, "%.4f_%.4f", latitude, longitude)
    val displayName: String get() = if (!region.isNullOrBlank() && region != name) "$name, $region" else name
}

@Serializable
enum class DimLevel(val strength: Float) { LIGHT(0.5f), MEDIUM(1.0f) }

@Serializable
enum class ClockFormat { SYSTEM, H24, H12 }

val DEFAULT_TILES: List<String?> = listOf(
    "com.google.android.youtube.tv",
    "ru.kinopoisk.tv",
    "ru.ivi.client",
    "ru.mts.mtstv",
    "ru.more.play",
)

@Serializable
data class Prefs(
    val tiles: List<String?> = DEFAULT_TILES,
    val cities: List<City> = emptyList(),
    val selectedCity: Int = 0,
    val wallpaperIntervalMinutes: Int = 5,
    val dimLevel: DimLevel = DimLevel.MEDIUM,
    val showDate: Boolean = true,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
)
```
`app/src/main/java/ru/polkatv/launcher/settings/TileOps.kt`:
```kotlin
package ru.polkatv.launcher.settings

object TileOps {
    const val MIN_TILES = 4
    const val MAX_TILES = 6

    fun replace(tiles: List<String?>, index: Int, pkg: String?): List<String?> =
        if (index !in tiles.indices) tiles else tiles.toMutableList().also { it[index] = pkg }

    /** direction: -1 moves left, +1 moves right; no-op at the edges. */
    fun move(tiles: List<String?>, index: Int, direction: Int): List<String?> {
        val target = index + direction
        if (index !in tiles.indices || target !in tiles.indices) return tiles
        return tiles.toMutableList().also { it[index] = tiles[target]; it[target] = tiles[index] }
    }

    fun resize(tiles: List<String?>, newSize: Int): List<String?> {
        val size = newSize.coerceIn(MIN_TILES, MAX_TILES)
        return if (size <= tiles.size) tiles.take(size) else tiles + List(size - tiles.size) { null }
    }

    /** Returns the new list, or null if there is no empty slot. Existing package → unchanged list. */
    fun addToFirstEmpty(tiles: List<String?>, pkg: String): List<String?>? {
        if (pkg in tiles) return tiles
        val i = tiles.indexOf(null)
        return if (i < 0) null else replace(tiles, i, pkg)
    }
}
```
`app/src/main/java/ru/polkatv/launcher/settings/PrefsCodec.kt`:
```kotlin
package ru.polkatv.launcher.settings

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PrefsCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(p: Prefs): String = json.encodeToString(p)
    fun decode(s: String?): Prefs = s?.let { runCatching { json.decodeFromString<Prefs>(it) }.getOrNull() } ?: Prefs()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.settings.*"`
Expected: 11 tests pass.

- [ ] **Step 5: Implement the repository and wire the container**

`app/src/main/java/ru/polkatv/launcher/settings/SettingsRepository.kt`:
```kotlin
package ru.polkatv.launcher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.polkaDataStore: DataStore<Preferences> by preferencesDataStore(name = "polka_prefs")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("prefs_json")

    val prefs: Flow<Prefs> = dataStore.data
        .map { PrefsCodec.decode(it[key]) }
        .catch { emit(Prefs()) }

    suspend fun update(transform: (Prefs) -> Prefs) {
        dataStore.edit { it[key] = PrefsCodec.encode(transform(PrefsCodec.decode(it[key]))) }
    }
}
```
`AppContainer.kt` replace the `wallpaperInterval` line with:
```kotlin
val settings = SettingsRepository(appContext.polkaDataStore)
val prefs: StateFlow<Prefs> = settings.prefs.stateIn(appScope, SharingStarted.Eagerly, Prefs())
val wallpaperInterval: StateFlow<Int> = prefs.map { it.wallpaperIntervalMinutes }.stateIn(appScope, SharingStarted.Eagerly, 5)
```
(imports: `kotlinx.coroutines.flow.SharingStarted`, `kotlinx.coroutines.flow.StateFlow`, `kotlinx.coroutines.flow.map`, `kotlinx.coroutines.flow.stateIn`, `ru.polkatv.launcher.settings.*`). Keep `wallpapers` using `wallpaperInterval`.

- [ ] **Step 6: Build and smoke-run on the TV**

```bash
./gradlew --no-daemon testDebugUnitTest && tools/install_tv.sh
adb -s 192.168.100.2:5555 shell run-as ru.polkatv.launcher ls files/datastore 2>/dev/null || echo "(datastore file appears after first write — fine)"
```
Expected: build green, app still shows wallpapers (interval now 5 min from defaults).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: settings model, tile operations and DataStore repository

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 4: Installed apps repository and tile art

Implements spec §4.3. Deliverable: a repository listing Leanback apps with launch intents and live package updates; banner/icon+gradient art loader; unit-tested tile resolution.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/apps/TvApp.kt`, `app/src/main/java/ru/polkatv/launcher/apps/AppsRepository.kt`, `app/src/main/java/ru/polkatv/launcher/apps/TileArtLoader.kt`, `app/src/main/java/ru/polkatv/launcher/apps/TileResolver.kt`, `app/src/test/java/ru/polkatv/launcher/apps/TileResolverTest.kt`
- Modify: `AppContainer.kt`

**Interfaces:**
- Produces:
  - `data class TvApp(val packageName: String, val label: String, val component: ComponentName, val hasBanner: Boolean)`
  - `class AppsRepository(context, scope) { val apps: StateFlow<List<TvApp>>; fun refresh(); fun launchIntent(packageName: String): Intent?; fun start(); fun stop() }`
  - `sealed interface TileArt { data class Banner(val image: ImageBitmap) : TileArt; data class Icon(val image: ImageBitmap, val start: Color, val end: Color) : TileArt }`
  - `class TileArtLoader(context) { suspend fun load(app: TvApp): TileArt; fun trim() }`
  - `sealed interface TileSlot { val index: Int; data class App(override val index: Int, val app: TvApp) : TileSlot; data class Empty(override val index: Int) : TileSlot }`
  - `object TileResolver { fun resolve(tiles: List<String?>, apps: List<TvApp>): List<TileSlot> }`

- [ ] **Step 1: Write the failing test**

`app/src/test/java/ru/polkatv/launcher/apps/TileResolverTest.kt`:
```kotlin
package ru.polkatv.launcher.apps

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Test

class TileResolverTest {
    private fun app(pkg: String) = TvApp(pkg, pkg.uppercase(), ComponentName(pkg, "$pkg.Main"), hasBanner = false)
    private val installed = listOf(app("a"), app("c"))

    @Test fun `installed packages resolve to App slots, others to Empty, order preserved`() {
        val slots = TileResolver.resolve(listOf("a", "b", null, "c"), installed)
        assertEquals(
            listOf(
                TileSlot.App(0, app("a")),
                TileSlot.Empty(1),
                TileSlot.Empty(2),
                TileSlot.App(3, app("c")),
            ),
            slots,
        )
    }

    @Test fun `empty tiles list resolves to empty slots list`() {
        assertEquals(emptyList<TileSlot>(), TileResolver.resolve(emptyList(), installed))
    }
}
```
Note: `ComponentName` is an Android class; `testOptions.unitTests.isReturnDefaultValues = true` (set in Task 1) makes its constructor usable in JVM tests. `equals` on the stub returns default `false`, so compare slots by fields instead if the assert fails: change the first test to compare `slots.map { it.index to (it as? TileSlot.App)?.app?.packageName }` against `listOf(0 to "a", 1 to null, 2 to null, 3 to "c")`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.apps.TileResolverTest"`
Expected: compilation FAILS (unresolved `TvApp`, `TileResolver`, `TileSlot`).

- [ ] **Step 3: Implement model, resolver, repository and art loader**

`app/src/main/java/ru/polkatv/launcher/apps/TvApp.kt`:
```kotlin
package ru.polkatv.launcher.apps

import android.content.ComponentName

data class TvApp(
    val packageName: String,
    val label: String,
    val component: ComponentName,
    val hasBanner: Boolean,
)
```
`app/src/main/java/ru/polkatv/launcher/apps/TileResolver.kt`:
```kotlin
package ru.polkatv.launcher.apps

sealed interface TileSlot {
    val index: Int
    data class App(override val index: Int, val app: TvApp) : TileSlot
    data class Empty(override val index: Int) : TileSlot
}

object TileResolver {
    fun resolve(tiles: List<String?>, apps: List<TvApp>): List<TileSlot> {
        val byPkg = apps.associateBy { it.packageName }
        return tiles.mapIndexed { i, pkg ->
            val app = pkg?.let { byPkg[it] }
            if (app != null) TileSlot.App(i, app) else TileSlot.Empty(i)
        }
    }
}
```
`app/src/main/java/ru/polkatv/launcher/apps/AppsRepository.kt`:
```kotlin
package ru.polkatv.launcher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class AppsRepository(private val context: Context, private val scope: CoroutineScope) {
    private val pm: PackageManager = context.packageManager
    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { refresh() }
    }

    fun start() {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, f)
        refresh()
    }

    fun stop() { runCatching { context.unregisterReceiver(receiver) } }

    fun refresh() { scope.launch { _apps.value = query() } }

    private suspend fun query(): List<TvApp> = withContext(Dispatchers.IO) {
        val self = context.packageName
        val leanback = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val launcher = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val seen = HashSet<String>()
        val result = ArrayList<TvApp>()
        fun add(ri: ResolveInfo) {
            val pkg = ri.activityInfo.packageName
            if (pkg == self || !seen.add(pkg)) return
            val hasBanner = ri.activityInfo.banner != 0 || ri.activityInfo.applicationInfo.banner != 0
            result += TvApp(pkg, ri.loadLabel(pm).toString(), ri.activityInfo.let { android.content.ComponentName(it.packageName, it.name) }, hasBanner)
        }
        leanback.forEach(::add)
        launcher.forEach(::add)
        val collator = Collator.getInstance(Locale("ru"))
        result.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launchIntent(packageName: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName)
}
```
`app/src/main/java/ru/polkatv/launcher/apps/TileArtLoader.kt`:
```kotlin
package ru.polkatv.launcher.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface TileArt {
    data class Banner(val image: ImageBitmap) : TileArt
    data class Icon(val image: ImageBitmap, val start: Color, val end: Color) : TileArt
}

class TileArtLoader(private val context: Context) {
    private val pm = context.packageManager
    private val cache = LruCache<String, TileArt>(32)

    suspend fun load(app: TvApp): TileArt {
        cache.get(app.packageName)?.let { return it }
        val art = withContext(Dispatchers.IO) { build(app) }
        cache.put(app.packageName, art)
        return art
    }

    fun trim() { cache.evictAll() }

    private fun build(app: TvApp): TileArt {
        val banner: Drawable? = runCatching { pm.getActivityInfo(app.component, 0).loadBanner(pm) }.getOrNull()
            ?: runCatching { pm.getApplicationBanner(app.packageName) }.getOrNull()
        if (banner != null && banner.intrinsicWidth > 0) {
            return TileArt.Banner(banner.toBitmap(640, 360, Bitmap.Config.ARGB_8888).asImageBitmap())
        }
        val icon = runCatching { pm.getActivityIcon(app.component) }.getOrElse { pm.getApplicationIcon(app.packageName) }
        val bmp = icon.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val palette = Palette.from(bmp).clearFilters().generate()
        val start = palette.vibrantSwatch ?: palette.dominantSwatch
        val end = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.mutedSwatch
        val c1 = start?.let { Color(it.rgb) } ?: Color(0xFF3A4A6B)
        val c2 = end?.let { Color(it.rgb) } ?: Color(c1.red * 0.45f, c1.green * 0.45f, c1.blue * 0.45f)
        return TileArt.Icon(bmp.asImageBitmap(), c1, c2)
    }
}
```
`AppContainer.kt` add:
```kotlin
val apps = AppsRepository(appContext, appScope)
val tileArt = TileArtLoader(appContext)
```
and in `MainActivity.onStart/onStop` call `container.apps.start()` / `container.apps.stop()`; in `onTrimMemory` also `container.tileArt.trim()`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.apps.TileResolverTest"`
Expected: 2 tests pass. If `ComponentName.equals` stub breaks equality, apply the field-wise comparison described in Step 1.

- [ ] **Step 5: Device check of the app list (temporary log)**

Temporarily add to `MainActivity.onStart`, after `start()`: `lifecycleScope.launch { container.apps.apps.collect { android.util.Log.i("Polka", "apps=" + it.size + " " + it.take(8).map { a -> a.label }) } }` (import `androidx.lifecycle.lifecycleScope`, `kotlinx.coroutines.launch`). Then:
```bash
tools/install_tv.sh; sleep 3; adb -s 192.168.100.2:5555 logcat -d -s Polka | tail -3
```
Expected: `apps=~30` with labels like `Google Play Игры` absent (disabled) and `YouTube`, `Кинопоиск`, `Иви` present. Remove the temporary log before committing.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: installed apps repository, tile art loader and tile resolver

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 5: Home screen — header with clock, app tiles row, tile menu, app picker

Implements spec §4.1, §4.2, §4.3 (UI), header layout from §4.6 (icons present, actions wired in Task 9), «Все приложения» tile (screen itself in Task 6). Deliverable: a navigable home screen on the TV with real app tiles that launch apps; long-press menu to replace/move tiles; unit-tested formatters.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/ui/common/Formatters.kt`, `app/src/main/java/ru/polkatv/launcher/ui/common/PolkaDialog.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/HomeState.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/HomeViewModel.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/HomeScreen.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/Header.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/AppRow.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/AppTile.kt`, `app/src/main/java/ru/polkatv/launcher/ui/home/TileMenuDialog.kt`, `app/src/main/java/ru/polkatv/launcher/ui/apps/AppPickerDialog.kt`, `app/src/main/java/ru/polkatv/launcher/ui/PolkaApp.kt`, `app/src/test/java/ru/polkatv/launcher/ui/common/FormattersTest.kt`
- Modify: `MainActivity.kt`, `res/values/strings.xml`, `res/values-en/strings.xml`

**Interfaces:**
- Consumes: `AppContainer.{prefs, settings, apps, tileArt, wallpapers}`, `TileResolver`, `TileOps`, `TileArtLoader.load`.
- Produces:
  - `object Formatters { fun clock(t: LocalTime, use24h: Boolean): String; fun date(d: LocalDate, locale: Locale): String }`
  - `@Composable fun PolkaDialog(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit)`
  - `data class HomeState(val prefs: Prefs, val tiles: List<TileSlot>, val apps: List<TvApp>)`
  - `class HomeViewModel(container: AppContainer) : ViewModel() { val state: StateFlow<HomeState>; fun launchApp(app: TvApp); fun replaceTile(index: Int, pkg: String?); fun moveTile(index: Int, direction: Int); fun addToFirstEmpty(pkg: String) }`
  - `@Composable fun HomeScreen(vm: HomeViewModel, container: AppContainer, onOpenAllApps: () -> Unit, onOpenSettings: () -> Unit, onOpenWeather: (Int) -> Unit)` (weather callback used from Task 8; header callbacks for system actions arrive in Task 9 via `HeaderActions`)
  - `data class HeaderActions(val onInputs: () -> Unit, val onNotifications: () -> Unit, val onNetwork: () -> Unit, val onTvSettings: () -> Unit, val onPolkaSettings: () -> Unit)`
  - `@Composable fun AppPickerDialog(apps: List<TvApp>, onPick: (TvApp) -> Unit, onDismiss: () -> Unit)`
  - `@Composable fun PolkaApp(container: AppContainer)` — NavHost with route `"home"` (more routes in later tasks).

- [ ] **Step 1: Write the failing formatter test**

`app/src/test/java/ru/polkatv/launcher/ui/common/FormattersTest.kt`:
```kotlin
package ru.polkatv.launcher.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class FormattersTest {
    @Test fun `clock 24h and 12h`() {
        assertEquals("21:47", Formatters.clock(LocalTime.of(21, 47), use24h = true))
        assertEquals("09:05", Formatters.clock(LocalTime.of(9, 5), use24h = true))
        assertEquals("9:47", Formatters.clock(LocalTime.of(21, 47), use24h = false))
        assertEquals("12:00", Formatters.clock(LocalTime.of(0, 0), use24h = false))
    }

    @Test fun `russian date is weekday comma day month in genitive`() {
        assertEquals("суббота, 24 октября", Formatters.date(LocalDate.of(2026, 10, 24), Locale("ru")))
        assertEquals("понедельник, 2 марта", Formatters.date(LocalDate.of(2026, 3, 2), Locale("ru")))
    }

    @Test fun `english date`() {
        assertEquals("Saturday, 24 October", Formatters.date(LocalDate.of(2026, 10, 24), Locale.ENGLISH))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.ui.common.FormattersTest"`
Expected: compilation FAILS (unresolved `Formatters`).

- [ ] **Step 3: Implement formatters**

`app/src/main/java/ru/polkatv/launcher/ui/common/Formatters.kt`:
```kotlin
package ru.polkatv.launcher.ui.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val f24 = DateTimeFormatter.ofPattern("HH:mm")
    private val f12 = DateTimeFormatter.ofPattern("h:mm", Locale.US)

    fun clock(t: LocalTime, use24h: Boolean): String = if (use24h) f24.format(t) else f12.format(t)

    fun date(d: LocalDate, locale: Locale): String = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(d)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.ui.common.FormattersTest"`
Expected: 3 tests pass. (If the Russian weekday comes out capitalised on the JVM, lower-case the first character in `date()` — the device CLDR data produces lowercase.)

- [ ] **Step 5: Add strings**

Append to `res/values/strings.xml`:
```xml
    <string name="tile_all_apps">Все приложения</string>
    <string name="tile_empty">Выбрать приложение</string>
    <string name="tile_menu_title">Плитка</string>
    <string name="tile_menu_replace">Заменить приложение</string>
    <string name="tile_menu_move_left">Сдвинуть влево</string>
    <string name="tile_menu_move_right">Сдвинуть вправо</string>
    <string name="picker_title">Выберите приложение</string>
    <string name="header_inputs">Входы</string>
    <string name="header_notifications">Уведомления</string>
    <string name="header_network">Сеть</string>
    <string name="header_tv_settings">Настройки ТВ</string>
    <string name="header_polka_settings">Настройки Полки</string>
    <string name="action_cancel">Отмена</string>
```
Append to `res/values-en/strings.xml`:
```xml
    <string name="tile_all_apps">All apps</string>
    <string name="tile_empty">Choose an app</string>
    <string name="tile_menu_title">Tile</string>
    <string name="tile_menu_replace">Replace app</string>
    <string name="tile_menu_move_left">Move left</string>
    <string name="tile_menu_move_right">Move right</string>
    <string name="picker_title">Choose an app</string>
    <string name="header_inputs">Inputs</string>
    <string name="header_notifications">Notifications</string>
    <string name="header_network">Network</string>
    <string name="header_tv_settings">TV settings</string>
    <string name="header_polka_settings">Polka settings</string>
    <string name="action_cancel">Cancel</string>
```
Remove `hello_placeholder` from both files.

- [ ] **Step 6: Implement the dialog shell and the ViewModel**

`app/src/main/java/ru/polkatv/launcher/ui/common/PolkaDialog.kt`:
```kotlin
package ru.polkatv.launcher.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import ru.polkatv.launcher.ui.theme.GlassBorder
import ru.polkatv.launcher.ui.theme.TextPrimary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PolkaDialog(onDismiss: () -> Unit, title: String, width: Int = 560, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = androidx.compose.ui.graphics.Color(0xF0141828)),
            modifier = Modifier.width(width.dp).border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        ) {
            Column(Modifier.padding(28.dp)) {
                Text(title, fontSize = 26.sp, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}
```
`app/src/main/java/ru/polkatv/launcher/ui/home/HomeState.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.Prefs

data class HomeState(
    val prefs: Prefs = Prefs(),
    val tiles: List<TileSlot> = emptyList(),
    val apps: List<TvApp> = emptyList(),
)
```
`app/src/main/java/ru/polkatv/launcher/ui/home/HomeViewModel.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileResolver
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.TileOps

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<HomeState> = combine(container.prefs, container.apps.apps) { prefs, apps ->
        HomeState(prefs = prefs, tiles = TileResolver.resolve(prefs.tiles, apps), apps = apps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun launchApp(app: TvApp) {
        val intent = container.apps.launchIntent(app.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { container.appContext.startActivity(intent) }
    }

    fun replaceTile(index: Int, pkg: String?) = viewModelScope.launch {
        container.settings.update { it.copy(tiles = TileOps.replace(it.tiles, index, pkg)) }
    }

    fun moveTile(index: Int, direction: Int) = viewModelScope.launch {
        container.settings.update { it.copy(tiles = TileOps.move(it.tiles, index, direction)) }
    }

    fun addToFirstEmpty(pkg: String) = viewModelScope.launch {
        container.settings.update { p -> TileOps.addToFirstEmpty(p.tiles, pkg)?.let { p.copy(tiles = it) } ?: p }
    }
}
```

- [ ] **Step 7: Implement tiles, row, menu and picker**

`app/src/main/java/ru/polkatv/launcher/ui/home/AppTile.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileArt
import ru.polkatv.launcher.apps.TileArtLoader
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.ui.theme.Dimens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TileCard(modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Dimens.tileCorner)
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.aspectRatio(16f / 9f),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(containerColor = Color(0x12FFFFFF)),
        scale = CardDefaults.scale(focusedScale = 1.10f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape)),
        glow = CardDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.6f), 24.dp)),
    ) { Box(Modifier.fillMaxSize()) { content() } }
}

@Composable
fun AppTile(slot: TileSlot.App, artLoader: TileArtLoader, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    var art by remember(slot.app.packageName) { mutableStateOf<TileArt?>(null) }
    LaunchedEffect(slot.app.packageName) { art = runCatching { artLoader.load(slot.app) }.getOrNull() }
    TileCard(modifier, onClick, onLongClick) {
        when (val a = art) {
            is TileArt.Banner -> Image(a.image, contentDescription = slot.app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is TileArt.Icon -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(a.start, a.end)))) {
                Image(a.image, contentDescription = null, modifier = Modifier.size(64.dp).align(Alignment.Center))
                Text(slot.app.label, fontSize = 16.sp, color = Color.White, maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
            }
            null -> Box(Modifier.fillMaxSize().background(Color(0x22FFFFFF)))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyTile(modifier: Modifier, onClick: () -> Unit) {
    TileCard(modifier, onClick) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(36.dp))
            Text(stringResource(R.string.tile_empty), fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AllAppsTile(modifier: Modifier, onClick: () -> Unit) {
    TileCard(modifier, onClick) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            Text(stringResource(R.string.tile_all_apps), fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}
```
`app/src/main/java/ru/polkatv/launcher/ui/home/AppRow.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import ru.polkatv.launcher.apps.TileArtLoader
import ru.polkatv.launcher.apps.TileSlot

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppRow(
    tiles: List<TileSlot>,
    artLoader: TileArtLoader,
    firstTileFocus: FocusRequester,
    onOpen: (TileSlot.App) -> Unit,
    onPickFor: (Int) -> Unit,
    onMenu: (TileSlot.App) -> Unit,
    onAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().focusGroup().focusRestorer { firstTileFocus },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tiles.forEachIndexed { i, slot ->
            val m = Modifier.weight(1f).let { if (i == 0) it.focusRequester(firstTileFocus) else it }
            when (slot) {
                is TileSlot.App -> AppTile(slot, artLoader, m, onClick = { onOpen(slot) }, onLongClick = { onMenu(slot) })
                is TileSlot.Empty -> EmptyTile(m, onClick = { onPickFor(slot.index) })
            }
        }
        AllAppsTile(Modifier.weight(1f), onClick = onAllApps)
    }
}
```
`app/src/main/java/ru/polkatv/launcher/ui/home/TileMenuDialog.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.ui.common.PolkaDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TileMenuDialog(slot: TileSlot.App, tileCount: Int, onReplace: () -> Unit, onMove: (Int) -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = slot.app.label) {
        ListItem(selected = false, onClick = onReplace, headlineContent = { Text(stringResource(R.string.tile_menu_replace)) })
        if (slot.index > 0) ListItem(selected = false, onClick = { onMove(-1) }, headlineContent = { Text(stringResource(R.string.tile_menu_move_left)) })
        if (slot.index < tileCount - 1) ListItem(selected = false, onClick = { onMove(+1) }, headlineContent = { Text(stringResource(R.string.tile_menu_move_right)) })
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
```
`app/src/main/java/ru/polkatv/launcher/ui/apps/AppPickerDialog.kt`:
```kotlin
package ru.polkatv.launcher.ui.apps

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.ui.common.PolkaDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppPickerDialog(apps: List<TvApp>, onPick: (TvApp) -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.picker_title)) {
        LazyColumn(Modifier.height(620.dp)) {
            items(apps, key = { it.packageName }) { app ->
                ListItem(selected = false, onClick = { onPick(app) }, headlineContent = { Text(app.label) })
            }
        }
    }
}
```

- [ ] **Step 8: Implement the header (icons present, actions passed in)**

`app/src/main/java/ru/polkatv/launcher/ui/home/Header.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import ru.polkatv.launcher.R
import ru.polkatv.launcher.settings.ClockFormat
import ru.polkatv.launcher.system.NetState
import ru.polkatv.launcher.ui.common.Formatters
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import java.time.LocalDateTime

data class HeaderActions(
    val onInputs: () -> Unit,
    val onNotifications: () -> Unit,
    val onNetwork: () -> Unit,
    val onTvSettings: () -> Unit,
    val onPolkaSettings: () -> Unit,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Header(clockFormat: ClockFormat, showDate: Boolean, net: NetState, actions: HeaderActions, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }
    val use24h = when (clockFormat) {
        ClockFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column {
            Text(Formatters.clock(now.toLocalTime(), use24h), fontSize = Dimens.clockSize, color = TextPrimary, lineHeight = Dimens.clockSize)
            if (showDate) Text(Formatters.date(now.toLocalDate(), locale), fontSize = Dimens.dateSize, color = TextSecondary)
        }
        Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderIcon(Icons.Default.Input, stringResource(R.string.header_inputs), actions.onInputs)
            HeaderIcon(Icons.Default.Notifications, stringResource(R.string.header_notifications), actions.onNotifications)
            val netIcon: ImageVector = when (net) {
                NetState.NONE -> Icons.Default.WifiOff
                NetState.WIFI -> Icons.Default.Wifi
                NetState.ETHERNET -> Icons.Default.SettingsEthernet
            }
            HeaderIcon(netIcon, stringResource(R.string.header_network), actions.onNetwork)
            HeaderIcon(Icons.Default.Settings, stringResource(R.string.header_tv_settings), actions.onTvSettings)
            HeaderIcon(Icons.Default.Tune, stringResource(R.string.header_polka_settings), actions.onPolkaSettings)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(Dimens.iconButton)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
    }
}
```
`NetState` is produced by Task 9; for this task create the enum now in `app/src/main/java/ru/polkatv/launcher/system/NetworkStatus.kt` with only:
```kotlin
package ru.polkatv.launcher.system

enum class NetState { NONE, WIFI, ETHERNET }
```
(Task 9 adds the class that produces it.)

- [ ] **Step 9: Implement HomeScreen and PolkaApp, replace the activity content**

`app/src/main/java/ru/polkatv/launcher/ui/home/HomeScreen.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.system.NetState
import ru.polkatv.launcher.ui.apps.AppPickerDialog
import ru.polkatv.launcher.ui.theme.Dimens

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    container: AppContainer,
    headerActions: HeaderActions,
    onOpenAllApps: () -> Unit,
    weatherShelf: @Composable (Modifier) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val wallpaper by container.wallpapers.current.collectAsStateWithLifecycle()
    val firstTileFocus = remember { FocusRequester() }
    var menuFor by remember { mutableStateOf<TileSlot.App?>(null) }
    var pickFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.tiles.isNotEmpty()) {
        if (state.tiles.isNotEmpty()) runCatching { firstTileFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(bitmap = wallpaper, dimStrength = state.prefs.dimLevel.strength)
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Header(
                clockFormat = state.prefs.clockFormat,
                showDate = state.prefs.showDate,
                net = NetState.WIFI,                       // Task 9 replaces with the live flow
                actions = headerActions,
            )
            Spacer(Modifier.height(Dimens.tierGap))
            weatherShelf(Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(Dimens.tierGap))
            AppRow(
                tiles = state.tiles,
                artLoader = container.tileArt,
                firstTileFocus = firstTileFocus,
                onOpen = vm::launchApp,
                onPickFor = { pickFor = it },
                onMenu = { menuFor = it },
                onAllApps = onOpenAllApps,
            )
        }
    }

    menuFor?.let { slot ->
        TileMenuDialog(
            slot = slot, tileCount = state.tiles.size,
            onReplace = { menuFor = null; pickFor = slot.index },
            onMove = { dir -> vm.moveTile(slot.index, dir); menuFor = null },
            onDismiss = { menuFor = null },
        )
    }
    pickFor?.let { index ->
        AppPickerDialog(apps = state.apps, onPick = { vm.replaceTile(index, it.packageName); pickFor = null }, onDismiss = { pickFor = null })
    }
}
```
`app/src/main/java/ru/polkatv/launcher/ui/PolkaApp.kt`:
```kotlin
package ru.polkatv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.ui.home.HeaderActions
import ru.polkatv.launcher.ui.home.HomeScreen
import ru.polkatv.launcher.ui.home.HomeViewModel
import ru.polkatv.launcher.ui.theme.GlassFill

object Routes {
    const val HOME = "home"
    const val ALL_APPS = "allapps"
    const val WEATHER = "weather/{index}"
    const val SETTINGS = "settings"
    fun weather(index: Int) = "weather/$index"
}

@Composable
fun PolkaApp(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
            HomeScreen(
                vm = vm,
                container = container,
                headerActions = HeaderActions({}, {}, {}, {}, {}),   // wired in Task 9/10
                onOpenAllApps = { nav.navigate(Routes.ALL_APPS) },  // screen arrives in Task 6
                weatherShelf = { m -> Box(m.background(GlassFill, RoundedCornerShape(24.dp))) },   // Task 8 replaces
            )
        }
    }
}
```
`MainActivity.kt` `setContent { PolkaTheme { PolkaApp(container) } }`; keep the lifecycle hooks (wallpapers/apps start/stop, trim). Remove the old placeholder `Text`. Add `override fun onBackPressed()`? No: Compose NavHost handles Back on nested routes; on `home` the system ignores Back for a HOME activity (spec §4.2). Verify in Step 10.

- [ ] **Step 10: Build, install, exercise with the D-pad, screenshot**

```bash
./gradlew --no-daemon testDebugUnitTest && tools/install_tv.sh
TV=192.168.100.2:5555
sleep 4; adb -s $TV exec-out screencap -p > /tmp/polka_home1.png
adb -s $TV shell input keyevent 22; adb -s $TV shell input keyevent 22   # right x2
adb -s $TV exec-out screencap -p > /tmp/polka_home2.png
adb -s $TV shell input keyevent --longpress 23; sleep 1; adb -s $TV exec-out screencap -p > /tmp/polka_menu.png
adb -s $TV shell input keyevent 4                                     # back closes menu
adb -s $TV shell input keyevent 19; adb -s $TV shell input keyevent 19   # up to header
adb -s $TV exec-out screencap -p > /tmp/polka_header.png
adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 23   # down, down, OK launches app
sleep 4; adb -s $TV shell dumpsys activity activities | grep topResumedActivity
adb -s $TV shell input keyevent 3   # HOME → back to Google TV for now (Polka is not HOME yet)
```
Expected: screenshot 1 shows clock top-left, five icons top-right, a glass placeholder shelf, six tiles with the first (YouTube) enlarged with white border; screenshot 2 shows the third tile focused; menu screenshot shows the dialog with «Заменить приложение / Сдвинуть…»; header screenshot shows an icon focused (white). The OK press launches the third app (`topResumedActivity` is that app's activity). Open PNGs with the Read tool. Fix focus or layout issues before committing.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: home screen with clock header, app tiles, tile menu and app picker

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 6: «Все приложения» screen

Implements spec §4.7 and the app-info/system action used from it. Deliverable: a 6-column grid of all apps reachable from the last tile; long-press adds an app to the home row or opens its system info.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/ui/apps/AllAppsScreen.kt`, `app/src/main/java/ru/polkatv/launcher/ui/apps/AllAppsViewModel.kt`, `app/src/main/java/ru/polkatv/launcher/system/SystemActions.kt`
- Modify: `apps/AppsRepository.kt` (add `launch`), `ui/home/HomeViewModel.kt` (delegate launching), `ui/PolkaApp.kt` (route), `AppContainer.kt`, `res/values/strings.xml`, `res/values-en/strings.xml`

**Interfaces:**
- Consumes: `AppsRepository.apps`, `TileArtLoader`, `TileOps`, `TileResolver`, `TileCard` (Task 5), `AppPickerDialog` pattern, `PolkaDialog`.
- Produces:
  - `fun AppsRepository.launch(packageName: String): Boolean` (starts the leanback/launcher intent with `FLAG_ACTIVITY_NEW_TASK`, false if none)
  - `class SystemActions(context) { fun launch(intent: Intent): Boolean; fun openAppInfo(packageName: String): Boolean }` (Task 9 adds more members)
  - `class AllAppsViewModel(container) : ViewModel() { val apps: StateFlow<List<TvApp>>; val tiles: StateFlow<List<TileSlot>>; fun launch(app: TvApp); fun addToFirstEmpty(pkg: String): Boolean; fun replaceTile(index: Int, pkg: String) }`
  - `@Composable fun AllAppsScreen(vm: AllAppsViewModel, container: AppContainer, onBack: () -> Unit)`

- [ ] **Step 1: Add strings**

`values/strings.xml`:
```xml
    <string name="all_apps_title">Все приложения</string>
    <string name="app_menu_add_home">Добавить на главный экран</string>
    <string name="app_menu_info">Информация о приложении</string>
    <string name="replace_which_title">Все плитки заняты. Какую заменить?</string>
    <string name="tile_n">Плитка %1$d: %2$s</string>
    <string name="tile_n_empty">Плитка %1$d: пусто</string>
    <string name="unavailable_on_tv">Недоступно на этом телевизоре</string>
```
`values-en/strings.xml`:
```xml
    <string name="all_apps_title">All apps</string>
    <string name="app_menu_add_home">Add to home screen</string>
    <string name="app_menu_info">App info</string>
    <string name="replace_which_title">All tiles are taken. Replace which one?</string>
    <string name="tile_n">Tile %1$d: %2$s</string>
    <string name="tile_n_empty">Tile %1$d: empty</string>
    <string name="unavailable_on_tv">Not available on this TV</string>
```

- [ ] **Step 2: Move launching into the repository and create SystemActions**

Add to `AppsRepository`:
```kotlin
    fun launch(packageName: String): Boolean {
        val intent = launchIntent(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
```
In `HomeViewModel`, replace the body of `launchApp` with `container.apps.launch(app.packageName)` and remove the now-unused `Intent` import.

`app/src/main/java/ru/polkatv/launcher/system/SystemActions.kt`:
```kotlin
package ru.polkatv.launcher.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class SystemActions(private val context: Context) {
    /** Starts the activity if something handles the intent. Returns false otherwise (caller shows a message). */
    fun launch(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openAppInfo(packageName: String): Boolean =
        launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
}
```
`AppContainer.kt` add `val system = SystemActions(appContext)`.

- [ ] **Step 3: ViewModel and screen**

`app/src/main/java/ru/polkatv/launcher/ui/apps/AllAppsViewModel.kt`:
```kotlin
package ru.polkatv.launcher.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileResolver
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.TileOps

class AllAppsViewModel(private val container: AppContainer) : ViewModel() {
    val apps: StateFlow<List<TvApp>> = container.apps.apps
    val tiles: StateFlow<List<TileSlot>> = combine(container.prefs, container.apps.apps) { p, a -> TileResolver.resolve(p.tiles, a) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun launch(app: TvApp) { container.apps.launch(app.packageName) }

    /** Returns false when every tile is taken (caller asks which one to replace). */
    fun addToFirstEmpty(pkg: String): Boolean {
        val current = container.prefs.value.tiles
        val next = TileOps.addToFirstEmpty(current, pkg) ?: return false
        viewModelScope.launch { container.settings.update { it.copy(tiles = next) } }
        return true
    }

    fun replaceTile(index: Int, pkg: String) = viewModelScope.launch {
        container.settings.update { it.copy(tiles = TileOps.replace(it.tiles, index, pkg)) }
    }

    fun openAppInfo(pkg: String): Boolean = container.system.openAppInfo(pkg)
}
```
`app/src/main/java/ru/polkatv/launcher/ui/apps/AllAppsScreen.kt`:
```kotlin
package ru.polkatv.launcher.ui.apps

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileArt
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.home.TileCard
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AllAppsScreen(vm: AllAppsViewModel, container: AppContainer, onBack: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<TvApp?>(null) }
    var replaceFor by remember { mutableStateOf<TvApp?>(null) }

    Box(Modifier.fillMaxSize().background(Color(0xFF060912))) {
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Text(stringResource(R.string.all_apps_title), fontSize = 36.sp, color = TextPrimary)
            Spacer(Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GridTile(app, container, onClick = { vm.launch(app) }, onLongClick = { menuFor = app })
                        Spacer(Modifier.height(8.dp))
                        Text(app.label, fontSize = 15.sp, color = TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }

    menuFor?.let { app ->
        PolkaDialog(onDismiss = { menuFor = null }, title = app.label) {
            ListItem(selected = false, onClick = {
                menuFor = null
                if (!vm.addToFirstEmpty(app.packageName)) replaceFor = app
            }, headlineContent = { Text(stringResource(R.string.app_menu_add_home)) })
            ListItem(selected = false, onClick = {
                menuFor = null
                if (!vm.openAppInfo(app.packageName)) Toast.makeText(context, R.string.unavailable_on_tv, Toast.LENGTH_SHORT).show()
            }, headlineContent = { Text(stringResource(R.string.app_menu_info)) })
            ListItem(selected = false, onClick = { menuFor = null }, headlineContent = { Text(stringResource(R.string.action_cancel)) })
        }
    }
    replaceFor?.let { app ->
        PolkaDialog(onDismiss = { replaceFor = null }, title = stringResource(R.string.replace_which_title)) {
            tiles.forEach { slot ->
                val label = when (slot) {
                    is TileSlot.App -> stringResource(R.string.tile_n, slot.index + 1, slot.app.label)
                    is TileSlot.Empty -> stringResource(R.string.tile_n_empty, slot.index + 1)
                }
                ListItem(selected = false, onClick = { vm.replaceTile(slot.index, app.packageName); replaceFor = null }, headlineContent = { Text(label) })
            }
        }
    }
}

@Composable
private fun GridTile(app: TvApp, container: AppContainer, onClick: () -> Unit, onLongClick: () -> Unit) {
    var art by remember(app.packageName) { mutableStateOf<TileArt?>(null) }
    LaunchedEffect(app.packageName) { art = runCatching { container.tileArt.load(app) }.getOrNull() }
    TileCard(Modifier.fillMaxWidth(), onClick, onLongClick) {
        when (val a = art) {
            is TileArt.Banner -> Image(a.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is TileArt.Icon -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(a.start, a.end)))) {
                Image(a.image, contentDescription = null, modifier = Modifier.size(56.dp).align(Alignment.Center))
            }
            null -> Box(Modifier.fillMaxSize().background(Color(0x22FFFFFF)))
        }
    }
}
```
In `PolkaApp.kt` add the route:
```kotlin
        composable(Routes.ALL_APPS) {
            val vm: AllAppsViewModel = viewModel(factory = viewModelFactory { initializer { AllAppsViewModel(container) } })
            AllAppsScreen(vm = vm, container = container, onBack = { nav.popBackStack() })
        }
```

- [ ] **Step 4: Build, install, verify**

```bash
./gradlew --no-daemon testDebugUnitTest && tools/install_tv.sh
TV=192.168.100.2:5555; sleep 4
for i in 1 2 3 4 5; do adb -s $TV shell input keyevent 22; done   # move to the «Все приложения» tile (6th)
adb -s $TV shell input keyevent 23; sleep 2; adb -s $TV exec-out screencap -p > /tmp/polka_allapps.png
adb -s $TV shell input keyevent --longpress 23; sleep 1; adb -s $TV exec-out screencap -p > /tmp/polka_allapps_menu.png
adb -s $TV shell input keyevent 4; adb -s $TV shell input keyevent 4   # close menu, back to home
adb -s $TV shell dumpsys activity activities | grep topResumedActivity
```
Expected: grid of ~30 apps with labels, focused tile enlarged; the long-press menu shows «Добавить на главный экран / Информация о приложении»; Back returns to the home screen (still Polka).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: all-apps grid with add-to-home and app info actions

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 7: Weather data layer (Open-Meteo, mapping, cache, repository)

Implements spec §7 and the data half of §4.4, §8 (network errors). Deliverable: fully unit-tested weather package with no UI yet.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/weather/OpenMeteoDto.kt`, `weather/WeatherModels.kt`, `weather/WmoCodes.kt`, `weather/Units.kt`, `weather/WeatherMapper.kt`, `weather/WeatherApi.kt`, `weather/OpenMeteoApi.kt`, `weather/WeatherCache.kt`, `weather/WeatherRepository.kt`; tests `app/src/test/java/ru/polkatv/launcher/weather/{WmoCodesTest,UnitsTest,WeatherMapperTest,OpenMeteoApiTest,WeatherCacheTest,WeatherRepositoryTest}.kt`; fixtures `app/src/test/resources/openmeteo/forecast_sample.json`, `app/src/test/resources/openmeteo/geocoding_sample.json`
- Modify: `AppContainer.kt`

**Interfaces:**
- Produces:
  - DTOs: `ForecastResponse(utcOffsetSeconds, timezone, current: CurrentDto, hourly: HourlyDto, daily: DailyDto)`, `GeoResultDto(name, latitude, longitude, country?, admin1?, timezone?)`, `CachedForecast(fetchedAtEpochMs: Long, response: ForecastResponse)`
  - Domain: `enum class WindDir { N, NE, E, SE, S, SW, W, NW }`, `data class CurrentWeather(tempC: Double, feelsLikeC: Double, humidity: Int, pressureMmHg: Int, windMs: Double, windDir: WindDir, precipProbability: Int?, code: Int, isDay: Boolean)`, `data class HourForecast(time: LocalDateTime, tempC: Double, code: Int, isDay: Boolean, precipProbability: Int?)`, `data class DayForecast(date: LocalDate, tMinC: Double, tMaxC: Double, code: Int, precipProbabilityMax: Int?, sunrise: LocalTime, sunset: LocalTime)`, `data class Weather(current, hourly: List<HourForecast>, daily: List<DayForecast>, fetchedAtEpochMs: Long, zoneId: String)`
  - `enum class WmoKind`, `enum class WeatherIcon { SUN, MOON, PARTLY_SUN, PARTLY_MOON, CLOUD, FOG, DRIZZLE, RAIN, SNOW, THUNDER }`, `object WmoCodes { fun kind(code: Int): WmoKind; fun icon(code: Int, isDay: Boolean): WeatherIcon }`
  - `object Units { fun hpaToMmHg(hpa: Double): Int; fun windDir(degrees: Double): WindDir }`
  - `object WeatherMapper { fun map(resp: ForecastResponse, fetchedAtEpochMs: Long, now: LocalDateTime): Weather }`
  - `interface WeatherApi { suspend fun forecast(lat: Double, lon: Double): ForecastResponse; suspend fun search(query: String): List<GeoResultDto> }`, `class OpenMeteoApi(client: OkHttpClient, forecastBase: String = "https://api.open-meteo.com", geoBase: String = "https://geocoding-api.open-meteo.com") : WeatherApi`
  - `class WeatherCache(dir: File) { fun read(key: String): CachedForecast?; fun write(key: String, value: CachedForecast); fun delete(key: String) }`
  - `data class CityWeather(city: City, weather: Weather?, stale: Boolean, error: Boolean, loading: Boolean)`
  - `class WeatherRepository(api: WeatherApi, cache: WeatherCache, clock: () -> Long = System::currentTimeMillis) { val states: StateFlow<Map<String, CityWeather>>; suspend fun refresh(city: City, force: Boolean = false); suspend fun refreshAll(cities: List<City>, force: Boolean = false); fun remove(city: City); suspend fun search(query: String): List<City>; companion { FRESH_MS = 30 min, STALE_MS = 60 min } }`

- [ ] **Step 1: Write fixtures**

`app/src/test/resources/openmeteo/forecast_sample.json` (hourly covers 2026-10-24T18:00 … 2026-10-25T05:00):
```json
{
  "latitude": 55.75, "longitude": 37.625, "utc_offset_seconds": 10800, "timezone": "Europe/Moscow",
  "current": {
    "time": "2026-10-24T21:30", "temperature_2m": 7.3, "relative_humidity_2m": 81, "apparent_temperature": 4.9,
    "is_day": 0, "precipitation": 0.2, "weather_code": 61, "pressure_msl": 1003.4, "wind_speed_10m": 6.1, "wind_direction_10m": 250.0
  },
  "hourly": {
    "time": ["2026-10-24T18:00","2026-10-24T19:00","2026-10-24T20:00","2026-10-24T21:00","2026-10-24T22:00","2026-10-24T23:00",
             "2026-10-25T00:00","2026-10-25T01:00","2026-10-25T02:00","2026-10-25T03:00","2026-10-25T04:00","2026-10-25T05:00"],
    "temperature_2m": [9.1, 8.6, 8.0, 7.4, 6.8, 6.1, 5.5, 5.0, 4.6, 4.2, 4.0, 3.7],
    "weather_code": [3, 3, 61, 61, 61, 3, 3, 2, 2, 1, 1, 0],
    "precipitation_probability": [20, 35, 60, 65, 55, 30, 20, 10, 5, 0, null, 0],
    "is_day": [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
  },
  "daily": {
    "time": ["2026-10-24","2026-10-25","2026-10-26","2026-10-27","2026-10-28","2026-10-29","2026-10-30"],
    "weather_code": [61, 3, 2, 71, 0, 45, 95],
    "temperature_2m_max": [9.5, 8.0, 7.2, 2.1, 4.0, 3.3, 6.0],
    "temperature_2m_min": [3.7, 2.0, 1.1, -2.5, -1.0, 0.2, 1.5],
    "sunrise": ["2026-10-24T07:21","2026-10-25T07:23","2026-10-26T07:25","2026-10-27T07:27","2026-10-28T07:29","2026-10-29T07:31","2026-10-30T07:33"],
    "sunset":  ["2026-10-24T17:02","2026-10-25T17:00","2026-10-26T16:58","2026-10-27T16:56","2026-10-28T16:54","2026-10-29T16:52","2026-10-30T16:50"],
    "precipitation_probability_max": [65, 30, 10, 80, 0, null, 90]
  }
}
```
`app/src/test/resources/openmeteo/geocoding_sample.json`:
```json
{"results":[
  {"id":524901,"name":"Москва","latitude":55.75222,"longitude":37.61556,"country":"Россия","admin1":"Москва","timezone":"Europe/Moscow"},
  {"id":1,"name":"Московский","latitude":55.59911,"longitude":37.35495,"country":"Россия","admin1":"Москва","timezone":"Europe/Moscow"}
]}
```

- [ ] **Step 2: Write the failing tests**

`WmoCodesTest.kt`:
```kotlin
package ru.polkatv.launcher.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WmoCodesTest {
    @Test fun `codes map to kinds`() {
        assertEquals(WmoKind.CLEAR, WmoCodes.kind(0))
        assertEquals(WmoKind.MAINLY_CLEAR, WmoCodes.kind(1))
        assertEquals(WmoKind.PARTLY_CLOUDY, WmoCodes.kind(2))
        assertEquals(WmoKind.OVERCAST, WmoCodes.kind(3))
        assertEquals(WmoKind.FOG, WmoCodes.kind(45)); assertEquals(WmoKind.FOG, WmoCodes.kind(48))
        assertEquals(WmoKind.DRIZZLE_LIGHT, WmoCodes.kind(51)); assertEquals(WmoKind.DRIZZLE, WmoCodes.kind(53)); assertEquals(WmoKind.DRIZZLE_DENSE, WmoCodes.kind(55))
        assertEquals(WmoKind.FREEZING_DRIZZLE, WmoCodes.kind(56)); assertEquals(WmoKind.FREEZING_DRIZZLE, WmoCodes.kind(57))
        assertEquals(WmoKind.RAIN_LIGHT, WmoCodes.kind(61)); assertEquals(WmoKind.RAIN, WmoCodes.kind(63)); assertEquals(WmoKind.RAIN_HEAVY, WmoCodes.kind(65))
        assertEquals(WmoKind.FREEZING_RAIN, WmoCodes.kind(66)); assertEquals(WmoKind.FREEZING_RAIN, WmoCodes.kind(67))
        assertEquals(WmoKind.SNOW_LIGHT, WmoCodes.kind(71)); assertEquals(WmoKind.SNOW, WmoCodes.kind(73)); assertEquals(WmoKind.SNOW_HEAVY, WmoCodes.kind(75))
        assertEquals(WmoKind.SNOW_GRAINS, WmoCodes.kind(77))
        assertEquals(WmoKind.SHOWERS_LIGHT, WmoCodes.kind(80)); assertEquals(WmoKind.SHOWERS, WmoCodes.kind(81)); assertEquals(WmoKind.SHOWERS_VIOLENT, WmoCodes.kind(82))
        assertEquals(WmoKind.SNOW_SHOWERS_LIGHT, WmoCodes.kind(85)); assertEquals(WmoKind.SNOW_SHOWERS_HEAVY, WmoCodes.kind(86))
        assertEquals(WmoKind.THUNDERSTORM, WmoCodes.kind(95))
        assertEquals(WmoKind.THUNDERSTORM_HAIL, WmoCodes.kind(96)); assertEquals(WmoKind.THUNDERSTORM_HAIL, WmoCodes.kind(99))
        assertEquals(WmoKind.UNKNOWN, WmoCodes.kind(42))
    }

    @Test fun `icons respect day and night for clear and partly cloudy`() {
        assertEquals(WeatherIcon.SUN, WmoCodes.icon(0, isDay = true))
        assertEquals(WeatherIcon.MOON, WmoCodes.icon(0, isDay = false))
        assertEquals(WeatherIcon.PARTLY_SUN, WmoCodes.icon(2, isDay = true))
        assertEquals(WeatherIcon.PARTLY_MOON, WmoCodes.icon(1, isDay = false))
        assertEquals(WeatherIcon.CLOUD, WmoCodes.icon(3, isDay = true))
        assertEquals(WeatherIcon.FOG, WmoCodes.icon(45, isDay = false))
        assertEquals(WeatherIcon.DRIZZLE, WmoCodes.icon(53, isDay = true))
        assertEquals(WeatherIcon.RAIN, WmoCodes.icon(65, isDay = true)); assertEquals(WeatherIcon.RAIN, WmoCodes.icon(81, isDay = true))
        assertEquals(WeatherIcon.SNOW, WmoCodes.icon(73, isDay = true)); assertEquals(WeatherIcon.SNOW, WmoCodes.icon(86, isDay = true))
        assertEquals(WeatherIcon.THUNDER, WmoCodes.icon(99, isDay = true))
        assertEquals(WeatherIcon.CLOUD, WmoCodes.icon(42, isDay = true))
    }
}
```
`UnitsTest.kt`:
```kotlin
package ru.polkatv.launcher.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test fun `hPa to mmHg rounds to integer`() {
        assertEquals(753, Units.hpaToMmHg(1003.4))
        assertEquals(760, Units.hpaToMmHg(1013.25))
    }

    @Test fun `wind degrees to eight sectors`() {
        assertEquals(WindDir.N, Units.windDir(0.0)); assertEquals(WindDir.N, Units.windDir(359.0)); assertEquals(WindDir.N, Units.windDir(22.4))
        assertEquals(WindDir.NE, Units.windDir(22.5)); assertEquals(WindDir.NE, Units.windDir(45.0))
        assertEquals(WindDir.E, Units.windDir(90.0)); assertEquals(WindDir.SE, Units.windDir(135.0))
        assertEquals(WindDir.S, Units.windDir(180.0)); assertEquals(WindDir.SW, Units.windDir(225.0))
        assertEquals(WindDir.W, Units.windDir(250.0)); assertEquals(WindDir.NW, Units.windDir(315.0))
    }
}
```
`WeatherMapperTest.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WeatherMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val resp = json.decodeFromString<ForecastResponse>(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText())
    private val now = LocalDateTime.of(2026, 10, 24, 21, 30)
    private val w = WeatherMapper.map(resp, fetchedAtEpochMs = 1_000L, now = now)

    @Test fun `current fields are converted`() {
        assertEquals(7.3, w.current.tempC, 0.001)
        assertEquals(4.9, w.current.feelsLikeC, 0.001)
        assertEquals(81, w.current.humidity)
        assertEquals(753, w.current.pressureMmHg)
        assertEquals(6.1, w.current.windMs, 0.001)
        assertEquals(WindDir.W, w.current.windDir)
        assertEquals(61, w.current.code)
        assertEquals(false, w.current.isDay)
        assertEquals(65, w.current.precipProbability)   // probability of the current hour (21:00)
        assertEquals("Europe/Moscow", w.zoneId)
        assertEquals(1_000L, w.fetchedAtEpochMs)
    }

    @Test fun `hourly starts at the current hour and keeps at most 24 entries`() {
        assertEquals(LocalDateTime.of(2026, 10, 24, 21, 0), w.hourly.first().time)
        assertEquals(9, w.hourly.size)
        assertEquals(7.4, w.hourly.first().tempC, 0.001)
        assertNull(w.hourly[7].precipProbability)        // 04:00 has null probability
    }

    @Test fun `daily has seven days with sunrise and sunset`() {
        assertEquals(7, w.daily.size)
        val d0 = w.daily.first()
        assertEquals(LocalDate.of(2026, 10, 24), d0.date)
        assertEquals(9.5, d0.tMaxC, 0.001); assertEquals(3.7, d0.tMinC, 0.001)
        assertEquals(61, d0.code); assertEquals(65, d0.precipProbabilityMax)
        assertEquals(LocalTime.of(7, 21), d0.sunrise); assertEquals(LocalTime.of(17, 2), d0.sunset)
        assertNull(w.daily[5].precipProbabilityMax)
    }

    @Test fun `when now is past all hourly data the hourly list is empty and precip is null`() {
        val late = WeatherMapper.map(resp, 0L, LocalDateTime.of(2026, 10, 26, 12, 0))
        assertEquals(0, late.hourly.size)
        assertNull(late.current.precipProbability)
    }
}
```
`OpenMeteoApiTest.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OpenMeteoApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OpenMeteoApi

    @Before fun setUp() {
        server = MockWebServer(); server.start()
        val base = server.url("/").toString().trimEnd('/')
        api = OpenMeteoApi(OkHttpClient(), forecastBase = base, geoBase = base)
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun `forecast requests the documented parameters and parses`() = runTest {
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText()))
        val r = api.forecast(55.75, 37.62)
        val req = server.takeRequest()
        assertEquals("/v1/forecast", req.requestUrl!!.encodedPath)
        val q = req.requestUrl!!
        assertEquals("55.75", q.queryParameter("latitude")); assertEquals("37.62", q.queryParameter("longitude"))
        assertEquals("ms", q.queryParameter("wind_speed_unit")); assertEquals("auto", q.queryParameter("timezone")); assertEquals("7", q.queryParameter("forecast_days"))
        assertTrue(q.queryParameter("current")!!.contains("pressure_msl"))
        assertTrue(q.queryParameter("hourly")!!.contains("precipitation_probability"))
        assertTrue(q.queryParameter("daily")!!.contains("sunrise"))
        assertEquals(61, r.current.code); assertEquals(12, r.hourly.time.size)
    }

    @Test fun `search uses russian language and parses results`() = runTest {
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/geocoding_sample.json")!!.readText()))
        val results = api.search("Moskva")
        val q = server.takeRequest().requestUrl!!
        assertEquals("/v1/search", q.encodedPath); assertEquals("Moskva", q.queryParameter("name")); assertEquals("ru", q.queryParameter("language")); assertEquals("8", q.queryParameter("count"))
        assertEquals(2, results.size); assertEquals("Москва", results[0].name); assertEquals("Москва", results[0].admin1)
    }

    @Test fun `empty results object parses to empty list`() = runTest {
        server.enqueue(MockResponse().setBody("{\"generationtime_ms\":0.1}"))
        assertEquals(0, api.search("zzzz").size)
    }

    @Test(expected = IOException::class)
    fun `http error throws IOException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        api.forecast(1.0, 2.0)
    }
}
```
`WeatherCacheTest.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WeatherCacheTest {
    @get:Rule val tmp = TemporaryFolder()
    private val resp = Json { ignoreUnknownKeys = true }.decodeFromString<ForecastResponse>(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText())

    @Test fun `write then read round trips`() {
        val cache = WeatherCache(tmp.root)
        cache.write("55.7500_37.6250", CachedForecast(123L, resp))
        val back = cache.read("55.7500_37.6250")!!
        assertEquals(123L, back.fetchedAtEpochMs); assertEquals(61, back.response.current.code)
    }

    @Test fun `missing key reads null`() { assertNull(WeatherCache(tmp.root).read("nope")) }

    @Test fun `corrupted file is deleted and reads null`() {
        val cache = WeatherCache(tmp.root)
        File(tmp.root, "weather_bad.json").writeText("{oops")
        assertNull(cache.read("bad")); assertEquals(false, File(tmp.root, "weather_bad.json").exists())
    }

    @Test fun `delete removes the file`() {
        val cache = WeatherCache(tmp.root)
        cache.write("k", CachedForecast(1L, resp)); cache.delete("k"); assertNull(cache.read("k"))
    }
}
```
`WeatherRepositoryTest.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.polkatv.launcher.settings.City
import java.io.IOException

class WeatherRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val resp = Json { ignoreUnknownKeys = true }.decodeFromString<ForecastResponse>(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText())
    private val city = City("Москва", "Москва", "Россия", 55.75, 37.625, "Europe/Moscow")

    private class FakeApi(var fail: Boolean = false, val resp: ForecastResponse) : WeatherApi {
        var calls = 0
        override suspend fun forecast(lat: Double, lon: Double): ForecastResponse { calls++; if (fail) throw IOException("offline"); return resp }
        override suspend fun search(query: String): List<GeoResultDto> = listOf(GeoResultDto("Москва", 55.75, 37.62, "Россия", "Москва", "Europe/Moscow"))
    }

    @Test fun `first refresh fetches, second within 30 minutes uses cache`() = runTest {
        var now = 1_000_000L
        val api = FakeApi(resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { now })
        repo.refresh(city); assertEquals(1, api.calls)
        now += 10 * 60_000L
        repo.refresh(city); assertEquals(1, api.calls)
        now += 25 * 60_000L
        repo.refresh(city); assertEquals(2, api.calls)
        val s = repo.states.value[city.key]!!
        assertNotNull(s.weather); assertFalse(s.stale); assertFalse(s.error)
    }

    @Test fun `failure keeps cached weather and marks stale and error`() = runTest {
        var now = 1_000_000L
        val api = FakeApi(resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { now })
        repo.refresh(city)
        now += 61 * 60_000L; api.fail = true
        repo.refresh(city)
        val s = repo.states.value[city.key]!!
        assertNotNull(s.weather); assertTrue(s.stale); assertTrue(s.error)
    }

    @Test fun `failure without cache yields error and null weather`() = runTest {
        val repo = WeatherRepository(FakeApi(fail = true, resp = resp), WeatherCache(tmp.root), clock = { 5L })
        repo.refresh(city)
        val s = repo.states.value[city.key]!!
        assertNull(s.weather); assertTrue(s.error)
    }

    @Test fun `force refresh ignores freshness`() = runTest {
        val api = FakeApi(resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { 1L })
        repo.refresh(city); repo.refresh(city, force = true); assertEquals(2, api.calls)
    }

    @Test fun `remove drops state and cache`() = runTest {
        val repo = WeatherRepository(FakeApi(resp = resp), WeatherCache(tmp.root), clock = { 1L })
        repo.refresh(city); repo.remove(city)
        assertNull(repo.states.value[city.key]); assertNull(WeatherCache(tmp.root).read(city.key))
    }

    @Test fun `search maps geo results to cities`() = runTest {
        val repo = WeatherRepository(FakeApi(resp = resp), WeatherCache(tmp.root))
        val cities = repo.search("Moskva")
        assertEquals(City("Москва", "Москва", "Россия", 55.75, 37.62, "Europe/Moscow"), cities.single())
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.weather.*"`
Expected: compilation FAILS (unresolved weather types).

- [ ] **Step 4: Implement DTOs, domain, codes, units, mapper**

`weather/OpenMeteoDto.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val timezone: String = "UTC",
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto,
)

@Serializable
data class CurrentDto(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("apparent_temperature") val apparent: Double,
    @SerialName("is_day") val isDay: Int,
    val precipitation: Double = 0.0,
    @SerialName("weather_code") val code: Int,
    @SerialName("pressure_msl") val pressureMsl: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Double,
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("weather_code") val code: List<Int>,
    @SerialName("precipitation_probability") val precipProbability: List<Int?>,
    @SerialName("is_day") val isDay: List<Int>,
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code") val code: List<Int>,
    @SerialName("temperature_2m_max") val tMax: List<Double>,
    @SerialName("temperature_2m_min") val tMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerialName("precipitation_probability_max") val precipProbabilityMax: List<Int?>,
)

@Serializable
data class GeoResponse(val results: List<GeoResultDto> = emptyList())

@Serializable
data class GeoResultDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
    val timezone: String? = null,
)

@Serializable
data class CachedForecast(val fetchedAtEpochMs: Long, val response: ForecastResponse)
```
`weather/WeatherModels.kt`:
```kotlin
package ru.polkatv.launcher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class WindDir { N, NE, E, SE, S, SW, W, NW }

data class CurrentWeather(
    val tempC: Double, val feelsLikeC: Double, val humidity: Int, val pressureMmHg: Int,
    val windMs: Double, val windDir: WindDir, val precipProbability: Int?, val code: Int, val isDay: Boolean,
)

data class HourForecast(val time: LocalDateTime, val tempC: Double, val code: Int, val isDay: Boolean, val precipProbability: Int?)

data class DayForecast(
    val date: LocalDate, val tMinC: Double, val tMaxC: Double, val code: Int,
    val precipProbabilityMax: Int?, val sunrise: LocalTime, val sunset: LocalTime,
)

data class Weather(
    val current: CurrentWeather,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>,
    val fetchedAtEpochMs: Long,
    val zoneId: String,
)
```
`weather/WmoCodes.kt`:
```kotlin
package ru.polkatv.launcher.weather

enum class WmoKind {
    CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG,
    DRIZZLE_LIGHT, DRIZZLE, DRIZZLE_DENSE, FREEZING_DRIZZLE,
    RAIN_LIGHT, RAIN, RAIN_HEAVY, FREEZING_RAIN,
    SNOW_LIGHT, SNOW, SNOW_HEAVY, SNOW_GRAINS,
    SHOWERS_LIGHT, SHOWERS, SHOWERS_VIOLENT, SNOW_SHOWERS_LIGHT, SNOW_SHOWERS_HEAVY,
    THUNDERSTORM, THUNDERSTORM_HAIL, UNKNOWN,
}

enum class WeatherIcon { SUN, MOON, PARTLY_SUN, PARTLY_MOON, CLOUD, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

object WmoCodes {
    fun kind(code: Int): WmoKind = when (code) {
        0 -> WmoKind.CLEAR
        1 -> WmoKind.MAINLY_CLEAR
        2 -> WmoKind.PARTLY_CLOUDY
        3 -> WmoKind.OVERCAST
        45, 48 -> WmoKind.FOG
        51 -> WmoKind.DRIZZLE_LIGHT
        53 -> WmoKind.DRIZZLE
        55 -> WmoKind.DRIZZLE_DENSE
        56, 57 -> WmoKind.FREEZING_DRIZZLE
        61 -> WmoKind.RAIN_LIGHT
        63 -> WmoKind.RAIN
        65 -> WmoKind.RAIN_HEAVY
        66, 67 -> WmoKind.FREEZING_RAIN
        71 -> WmoKind.SNOW_LIGHT
        73 -> WmoKind.SNOW
        75 -> WmoKind.SNOW_HEAVY
        77 -> WmoKind.SNOW_GRAINS
        80 -> WmoKind.SHOWERS_LIGHT
        81 -> WmoKind.SHOWERS
        82 -> WmoKind.SHOWERS_VIOLENT
        85 -> WmoKind.SNOW_SHOWERS_LIGHT
        86 -> WmoKind.SNOW_SHOWERS_HEAVY
        95 -> WmoKind.THUNDERSTORM
        96, 99 -> WmoKind.THUNDERSTORM_HAIL
        else -> WmoKind.UNKNOWN
    }

    fun icon(code: Int, isDay: Boolean): WeatherIcon = when (kind(code)) {
        WmoKind.CLEAR -> if (isDay) WeatherIcon.SUN else WeatherIcon.MOON
        WmoKind.MAINLY_CLEAR, WmoKind.PARTLY_CLOUDY -> if (isDay) WeatherIcon.PARTLY_SUN else WeatherIcon.PARTLY_MOON
        WmoKind.OVERCAST, WmoKind.UNKNOWN -> WeatherIcon.CLOUD
        WmoKind.FOG -> WeatherIcon.FOG
        WmoKind.DRIZZLE_LIGHT, WmoKind.DRIZZLE, WmoKind.DRIZZLE_DENSE, WmoKind.FREEZING_DRIZZLE -> WeatherIcon.DRIZZLE
        WmoKind.RAIN_LIGHT, WmoKind.RAIN, WmoKind.RAIN_HEAVY, WmoKind.FREEZING_RAIN,
        WmoKind.SHOWERS_LIGHT, WmoKind.SHOWERS, WmoKind.SHOWERS_VIOLENT -> WeatherIcon.RAIN
        WmoKind.SNOW_LIGHT, WmoKind.SNOW, WmoKind.SNOW_HEAVY, WmoKind.SNOW_GRAINS,
        WmoKind.SNOW_SHOWERS_LIGHT, WmoKind.SNOW_SHOWERS_HEAVY -> WeatherIcon.SNOW
        WmoKind.THUNDERSTORM, WmoKind.THUNDERSTORM_HAIL -> WeatherIcon.THUNDER
    }
}
```
`weather/Units.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlin.math.roundToInt

object Units {
    fun hpaToMmHg(hpa: Double): Int = (hpa * 0.750062).roundToInt()

    fun windDir(degrees: Double): WindDir {
        val d = ((degrees % 360) + 360) % 360
        val sector = ((d + 22.5) / 45.0).toInt() % 8
        return WindDir.values()[sector]
    }
}
```
`weather/WeatherMapper.kt`:
```kotlin
package ru.polkatv.launcher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object WeatherMapper {
    fun map(resp: ForecastResponse, fetchedAtEpochMs: Long, now: LocalDateTime): Weather {
        val startHour = now.truncatedTo(ChronoUnit.HOURS)
        val hourly = resp.hourly.time.indices.asSequence()
            .map { i ->
                HourForecast(
                    time = LocalDateTime.parse(resp.hourly.time[i]),
                    tempC = resp.hourly.temperature[i],
                    code = resp.hourly.code[i],
                    isDay = resp.hourly.isDay[i] == 1,
                    precipProbability = resp.hourly.precipProbability.getOrNull(i),
                )
            }
            .filter { !it.time.isBefore(startHour) }
            .take(24)
            .toList()
        val daily = resp.daily.time.indices.map { i ->
            DayForecast(
                date = LocalDate.parse(resp.daily.time[i]),
                tMinC = resp.daily.tMin[i],
                tMaxC = resp.daily.tMax[i],
                code = resp.daily.code[i],
                precipProbabilityMax = resp.daily.precipProbabilityMax.getOrNull(i),
                sunrise = LocalDateTime.parse(resp.daily.sunrise[i]).toLocalTime(),
                sunset = LocalDateTime.parse(resp.daily.sunset[i]).toLocalTime(),
            )
        }.take(7)
        val c = resp.current
        val current = CurrentWeather(
            tempC = c.temperature, feelsLikeC = c.apparent, humidity = c.humidity,
            pressureMmHg = Units.hpaToMmHg(c.pressureMsl), windMs = c.windSpeed, windDir = Units.windDir(c.windDirection),
            precipProbability = hourly.firstOrNull()?.precipProbability, code = c.code, isDay = c.isDay == 1,
        )
        return Weather(current, hourly, daily, fetchedAtEpochMs, resp.timezone)
    }
}
```

- [ ] **Step 5: Implement API, cache, repository**

`weather/WeatherApi.kt`:
```kotlin
package ru.polkatv.launcher.weather

interface WeatherApi {
    suspend fun forecast(lat: Double, lon: Double): ForecastResponse
    suspend fun search(query: String): List<GeoResultDto>
}
```
`weather/OpenMeteoApi.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

class OpenMeteoApi(
    private val client: OkHttpClient,
    private val forecastBase: String = "https://api.open-meteo.com",
    private val geoBase: String = "https://geocoding-api.open-meteo.com",
) : WeatherApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun forecast(lat: Double, lon: Double): ForecastResponse {
        val url = "$forecastBase/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", String.format(Locale.US, "%.4f", lat).trimZeros())
            .addQueryParameter("longitude", String.format(Locale.US, "%.4f", lon).trimZeros())
            .addQueryParameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m")
            .addQueryParameter("hourly", "temperature_2m,weather_code,precipitation_probability,is_day")
            .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max")
            .addQueryParameter("wind_speed_unit", "ms")
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "7")
            .build()
        return json.decodeFromString(get(url.toString()))
    }

    override suspend fun search(query: String): List<GeoResultDto> {
        val url = "$geoBase/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "8")
            .addQueryParameter("language", "ru")
            .addQueryParameter("format", "json")
            .build()
        return json.decodeFromString<GeoResponse>(get(url.toString())).results
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).header("User-Agent", "PolkaTV/0.1").build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    private fun String.trimZeros(): String = if (contains('.')) trimEnd('0').trimEnd('.') else this
}
```
`weather/WeatherCache.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WeatherCache(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(key: String) = File(dir, "weather_$key.json")

    fun read(key: String): CachedForecast? {
        val f = file(key)
        if (!f.exists()) return null
        return runCatching { json.decodeFromString<CachedForecast>(f.readText()) }
            .onFailure { f.delete() }
            .getOrNull()
    }

    fun write(key: String, value: CachedForecast) {
        dir.mkdirs()
        val tmp = File(dir, "weather_$key.tmp")
        tmp.writeText(json.encodeToString(value))
        tmp.renameTo(file(key))
    }

    fun delete(key: String) { file(key).delete() }
}
```
`weather/WeatherRepository.kt`:
```kotlin
package ru.polkatv.launcher.weather

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.polkatv.launcher.settings.City
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class CityWeather(
    val city: City,
    val weather: Weather? = null,
    val stale: Boolean = false,
    val error: Boolean = false,
    val loading: Boolean = false,
)

class WeatherRepository(
    private val api: WeatherApi,
    private val cache: WeatherCache,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _states = MutableStateFlow<Map<String, CityWeather>>(emptyMap())
    val states: StateFlow<Map<String, CityWeather>> = _states.asStateFlow()
    private val mutex = Mutex()

    suspend fun refresh(city: City, force: Boolean = false) = mutex.withLock {
        val now = clock()
        val cached = cache.read(city.key)
        if (!force && cached != null && now - cached.fetchedAtEpochMs < FRESH_MS) {
            publish(city, cached, now, error = false); return@withLock
        }
        _states.update { it + (city.key to (it[city.key] ?: CityWeather(city)).copy(city = city, loading = true)) }
        val fresh = runCatching { api.forecast(city.latitude, city.longitude) }
        fresh.onSuccess { resp ->
            val entry = CachedForecast(now, resp)
            cache.write(city.key, entry)
            publish(city, entry, now, error = false)
        }.onFailure {
            publish(city, cached, now, error = true)
        }
    }

    suspend fun refreshAll(cities: List<City>, force: Boolean = false) {
        cities.forEach { refresh(it, force) }
        val keep = cities.map { it.key }.toSet()
        _states.update { m -> m.filterKeys { it in keep } }
    }

    fun remove(city: City) {
        cache.delete(city.key)
        _states.update { it - city.key }
    }

    suspend fun search(query: String): List<City> =
        api.search(query).map { City(it.name, it.admin1, it.country, it.latitude, it.longitude, it.timezone) }

    private fun publish(city: City, entry: CachedForecast?, now: Long, error: Boolean) {
        val weather = entry?.let { e ->
            val zone = runCatching { ZoneId.of(e.response.timezone) }.getOrDefault(ZoneId.systemDefault())
            WeatherMapper.map(e.response, e.fetchedAtEpochMs, LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone))
        }
        val stale = error || (entry != null && now - entry.fetchedAtEpochMs > STALE_MS)
        _states.update { it + (city.key to CityWeather(city, weather, stale, error, loading = false)) }
    }

    companion object {
        const val FRESH_MS = 30 * 60_000L
        const val STALE_MS = 60 * 60_000L
    }
}
```
`AppContainer.kt` add:
```kotlin
val httpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
val weather = WeatherRepository(OpenMeteoApi(httpClient), WeatherCache(File(appContext.filesDir, "weather")))
```
(imports `okhttp3.OkHttpClient`, `java.io.File`, `java.util.concurrent.TimeUnit`, `ru.polkatv.launcher.weather.*`).

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.weather.*"`
Expected: all weather tests pass (2 + 2 + 4 + 4 + 4 + 6 = 22). If `WeatherMapperTest` fails on `precipProbability` for 04:00, check `getOrNull` vs. the fixture's `null` at index 10.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: Open-Meteo weather client, WMO mapping, cache and repository with tests

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 8: Weather shelf, add-city dialog, city menu and detail screen

Implements spec §4.4 (UI), §4.1 shelf, §7 refresh cadence, §8 offline display. Deliverable: the shelf shows live weather for the selected city, left/right switches cities, OK opens the detail screen, long-press opens the city menu; adding a city works through search.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/ui/weather/WeatherStrings.kt`, `ui/weather/WeatherIcons.kt`, `ui/home/WeatherShelf.kt`, `ui/weather/AddCityDialog.kt`, `ui/weather/WeatherDetailScreen.kt`, `app/src/test/java/ru/polkatv/launcher/ui/weather/AgoFormatterTest.kt`
- Modify: `ui/home/HomeViewModel.kt`, `ui/home/HomeState.kt`, `ui/home/HomeScreen.kt`, `ui/PolkaApp.kt`, `res/values/strings.xml`, `res/values-en/strings.xml`

**Interfaces:**
- Consumes: `WeatherRepository`, `CityWeather`, `Weather*` models, `WmoCodes`, `City`, `Prefs.cities/selectedCity`, `PolkaDialog`, `GlassFill/GlassBorder`.
- Produces:
  - `object WeatherStrings { @StringRes fun description(kind: WmoKind): Int; @StringRes fun windDir(dir: WindDir): Int; fun ago(ageMs: Long): Pair<Int, Int> }` where `ago` returns (string resource id, quantity) for «%d мин» / «%d ч» / «%d дн»
  - `fun WeatherIcon.vector(): ImageVector`
  - `HomeState` gains `val cities: List<CityWeather>, val selectedCity: Int`
  - `HomeViewModel` gains `fun selectCity(index: Int)`, `fun addCity(city: City)`, `fun removeCity(index: Int)`, `fun makeCityFirst(index: Int)`, `fun refreshWeather(force: Boolean = false)`, `suspend fun searchCities(query: String): List<City>`
  - `@Composable fun WeatherShelf(cities: List<CityWeather>, selected: Int, onSelect: (Int) -> Unit, onOpenDetail: () -> Unit, onLongPress: () -> Unit, onAddCity: () -> Unit, modifier: Modifier)`
  - `@Composable fun AddCityDialog(search: suspend (String) -> List<City>, onPick: (City) -> Unit, onDismiss: () -> Unit)`
  - `@Composable fun WeatherDetailScreen(cityWeather: CityWeather?, onBack: () -> Unit)`

- [ ] **Step 1: Strings**

`values/strings.xml` add:
```xml
    <!-- weather descriptions -->
    <string name="wx_clear">Ясно</string>
    <string name="wx_mainly_clear">Преимущественно ясно</string>
    <string name="wx_partly_cloudy">Переменная облачность</string>
    <string name="wx_overcast">Облачно</string>
    <string name="wx_fog">Туман</string>
    <string name="wx_drizzle_light">Слабая морось</string>
    <string name="wx_drizzle">Морось</string>
    <string name="wx_drizzle_dense">Сильная морось</string>
    <string name="wx_freezing_drizzle">Ледяная морось</string>
    <string name="wx_rain_light">Небольшой дождь</string>
    <string name="wx_rain">Дождь</string>
    <string name="wx_rain_heavy">Сильный дождь</string>
    <string name="wx_freezing_rain">Ледяной дождь</string>
    <string name="wx_snow_light">Небольшой снег</string>
    <string name="wx_snow">Снег</string>
    <string name="wx_snow_heavy">Сильный снег</string>
    <string name="wx_snow_grains">Снежные зёрна</string>
    <string name="wx_showers_light">Небольшой ливень</string>
    <string name="wx_showers">Ливень</string>
    <string name="wx_showers_violent">Сильный ливень</string>
    <string name="wx_snow_showers_light">Небольшой снегопад</string>
    <string name="wx_snow_showers_heavy">Сильный снегопад</string>
    <string name="wx_thunderstorm">Гроза</string>
    <string name="wx_thunderstorm_hail">Гроза с градом</string>
    <string name="wx_unknown">Нет данных</string>
    <!-- wind -->
    <string name="wind_n">С</string><string name="wind_ne">СВ</string><string name="wind_e">В</string><string name="wind_se">ЮВ</string>
    <string name="wind_s">Ю</string><string name="wind_sw">ЮЗ</string><string name="wind_w">З</string><string name="wind_nw">СЗ</string>
    <!-- shelf -->
    <string name="wx_feels">Ощущается</string>
    <string name="wx_wind">Ветер</string>
    <string name="wx_humidity">Влажность</string>
    <string name="wx_pressure">Давление</string>
    <string name="wx_precip">Осадки</string>
    <string name="unit_ms">%1$s м/с</string>
    <string name="unit_mmhg">%1$d мм</string>
    <string name="unit_percent">%1$d%%</string>
    <string name="unit_temp">%1$s°</string>
    <string name="wx_updated_ago">обновлено %1$s назад</string>
    <string name="ago_minutes">%1$d мин</string>
    <string name="ago_hours">%1$d ч</string>
    <string name="ago_days">%1$d дн</string>
    <string name="wx_no_data">Нет данных, проверьте сеть</string>
    <string name="wx_add_city_prompt">Добавьте город, чтобы видеть погоду</string>
    <string name="city_menu_add">Добавить город</string>
    <string name="city_menu_remove">Удалить этот город</string>
    <string name="city_menu_first">Сделать первым</string>
    <string name="add_city_title">Добавить город</string>
    <string name="add_city_hint">Название города</string>
    <string name="add_city_no_network">Нет связи</string>
    <string name="add_city_nothing">Ничего не найдено</string>
    <string name="wx_detail_hourly">Почасовой прогноз</string>
    <string name="wx_detail_daily">На 7 дней</string>
    <string name="wx_sunrise">Восход %1$s</string>
    <string name="wx_sunset">Закат %1$s</string>
    <string name="wx_today">Сегодня</string>
```
`values-en/strings.xml` add the English equivalents (Clear, Mainly clear, Partly cloudy, Overcast, Fog, Light drizzle, Drizzle, Dense drizzle, Freezing drizzle, Light rain, Rain, Heavy rain, Freezing rain, Light snow, Snow, Heavy snow, Snow grains, Light showers, Showers, Violent showers, Light snow showers, Heavy snow showers, Thunderstorm, Thunderstorm with hail, No data; N NE E SE S SW W NW; Feels like, Wind, Humidity, Pressure, Precipitation; `%1$s m/s`, `%1$d mmHg`, `%1$d%%`, `%1$s°`, `updated %1$s ago`, `%1$d min`, `%1$d h`, `%1$d d`, `No data, check the network`, `Add a city to see the weather`, `Add city`, `Remove this city`, `Make first`, `Add city`, `City name`, `No connection`, `Nothing found`, `Hourly forecast`, `7-day forecast`, `Sunrise %1$s`, `Sunset %1$s`, `Today`) with the same keys.

- [ ] **Step 2: Failing test for the "ago" formatter**

`app/src/test/java/ru/polkatv/launcher/ui/weather/AgoFormatterTest.kt`:
```kotlin
package ru.polkatv.launcher.ui.weather

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.polkatv.launcher.R

class AgoFormatterTest {
    @Test fun `minutes under an hour`() { assertEquals(R.string.ago_minutes to 5, WeatherStrings.ago(5 * 60_000L)) }
    @Test fun `zero rounds up to one minute`() { assertEquals(R.string.ago_minutes to 1, WeatherStrings.ago(10_000L)) }
    @Test fun `hours under a day`() { assertEquals(R.string.ago_hours to 2, WeatherStrings.ago(2 * 3_600_000L + 5)) }
    @Test fun `days`() { assertEquals(R.string.ago_days to 3, WeatherStrings.ago(3 * 86_400_000L)) }
}
```
Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.ui.weather.AgoFormatterTest"` → expected compilation FAILS (unresolved `WeatherStrings`).

- [ ] **Step 3: Implement strings/icons helpers**

`ui/weather/WeatherStrings.kt`:
```kotlin
package ru.polkatv.launcher.ui.weather

import androidx.annotation.StringRes
import ru.polkatv.launcher.R
import ru.polkatv.launcher.weather.WindDir
import ru.polkatv.launcher.weather.WmoKind

object WeatherStrings {
    @StringRes fun description(kind: WmoKind): Int = when (kind) {
        WmoKind.CLEAR -> R.string.wx_clear
        WmoKind.MAINLY_CLEAR -> R.string.wx_mainly_clear
        WmoKind.PARTLY_CLOUDY -> R.string.wx_partly_cloudy
        WmoKind.OVERCAST -> R.string.wx_overcast
        WmoKind.FOG -> R.string.wx_fog
        WmoKind.DRIZZLE_LIGHT -> R.string.wx_drizzle_light
        WmoKind.DRIZZLE -> R.string.wx_drizzle
        WmoKind.DRIZZLE_DENSE -> R.string.wx_drizzle_dense
        WmoKind.FREEZING_DRIZZLE -> R.string.wx_freezing_drizzle
        WmoKind.RAIN_LIGHT -> R.string.wx_rain_light
        WmoKind.RAIN -> R.string.wx_rain
        WmoKind.RAIN_HEAVY -> R.string.wx_rain_heavy
        WmoKind.FREEZING_RAIN -> R.string.wx_freezing_rain
        WmoKind.SNOW_LIGHT -> R.string.wx_snow_light
        WmoKind.SNOW -> R.string.wx_snow
        WmoKind.SNOW_HEAVY -> R.string.wx_snow_heavy
        WmoKind.SNOW_GRAINS -> R.string.wx_snow_grains
        WmoKind.SHOWERS_LIGHT -> R.string.wx_showers_light
        WmoKind.SHOWERS -> R.string.wx_showers
        WmoKind.SHOWERS_VIOLENT -> R.string.wx_showers_violent
        WmoKind.SNOW_SHOWERS_LIGHT -> R.string.wx_snow_showers_light
        WmoKind.SNOW_SHOWERS_HEAVY -> R.string.wx_snow_showers_heavy
        WmoKind.THUNDERSTORM -> R.string.wx_thunderstorm
        WmoKind.THUNDERSTORM_HAIL -> R.string.wx_thunderstorm_hail
        WmoKind.UNKNOWN -> R.string.wx_unknown
    }

    @StringRes fun windDir(dir: WindDir): Int = when (dir) {
        WindDir.N -> R.string.wind_n; WindDir.NE -> R.string.wind_ne; WindDir.E -> R.string.wind_e; WindDir.SE -> R.string.wind_se
        WindDir.S -> R.string.wind_s; WindDir.SW -> R.string.wind_sw; WindDir.W -> R.string.wind_w; WindDir.NW -> R.string.wind_nw
    }

    /** (resource, quantity) for the "updated … ago" label. */
    fun ago(ageMs: Long): Pair<Int, Int> {
        val minutes = (ageMs / 60_000L).toInt().coerceAtLeast(1)
        return when {
            minutes < 60 -> R.string.ago_minutes to minutes
            minutes < 24 * 60 -> R.string.ago_hours to minutes / 60
            else -> R.string.ago_days to minutes / (24 * 60)
        }
    }

    fun temp(t: Double): String { val r = Math.round(t).toInt(); return if (r > 0) "+$r" else "$r" }
}
```
`ui/weather/WeatherIcons.kt`:
```kotlin
package ru.polkatv.launcher.ui.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import ru.polkatv.launcher.weather.WeatherIcon

fun WeatherIcon.vector(): ImageVector = when (this) {
    WeatherIcon.SUN -> Icons.Filled.WbSunny
    WeatherIcon.MOON -> Icons.Filled.NightsStay
    WeatherIcon.PARTLY_SUN -> Icons.Filled.WbCloudy
    WeatherIcon.PARTLY_MOON -> Icons.Filled.NightsStay
    WeatherIcon.CLOUD -> Icons.Filled.Cloud
    WeatherIcon.FOG -> Icons.Filled.BlurOn
    WeatherIcon.DRIZZLE -> Icons.Filled.Grain
    WeatherIcon.RAIN -> Icons.Filled.WaterDrop
    WeatherIcon.SNOW -> Icons.Filled.AcUnit
    WeatherIcon.THUNDER -> Icons.Filled.FlashOn
}
```
Run the AgoFormatter test → expected 4 pass. (`R.string` constants resolve in JVM tests because the test classpath includes the generated `R` class.)

- [ ] **Step 4: Extend HomeState and HomeViewModel**

`HomeState.kt` add fields `val cities: List<CityWeather> = emptyList(), val selectedCity: Int = 0`.

`HomeViewModel.kt` replace `state` and add members:
```kotlin
    val state: StateFlow<HomeState> = combine(container.prefs, container.apps.apps, container.weather.states) { prefs, apps, wx ->
        val cities = prefs.cities.map { c -> wx[c.key] ?: CityWeather(c, loading = true) }
        HomeState(
            prefs = prefs,
            tiles = TileResolver.resolve(prefs.tiles, apps),
            apps = apps,
            cities = cities,
            selectedCity = prefs.selectedCity.coerceIn(0, (cities.size - 1).coerceAtLeast(0)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private var refreshJob: Job? = null

    /** Called when the home screen becomes visible: refresh now (cache decides), then every 30 minutes. */
    fun startWeatherLoop() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            while (isActive) {
                container.weather.refreshAll(container.prefs.value.cities)
                delay(WeatherRepository.FRESH_MS)
            }
        }
    }
    fun stopWeatherLoop() { refreshJob?.cancel(); refreshJob = null }

    fun refreshWeather(force: Boolean = false) = viewModelScope.launch { container.weather.refreshAll(container.prefs.value.cities, force) }

    fun selectCity(index: Int) = viewModelScope.launch {
        container.settings.update { it.copy(selectedCity = index.coerceIn(0, (it.cities.size - 1).coerceAtLeast(0))) }
        container.prefs.value.cities.getOrNull(index)?.let { container.weather.refresh(it) }
    }

    fun addCity(city: City) = viewModelScope.launch {
        container.settings.update { p -> if (p.cities.size >= 6 || p.cities.any { it.key == city.key }) p else p.copy(cities = p.cities + city, selectedCity = p.cities.size) }
        container.weather.refresh(city)
    }

    fun removeCity(index: Int) = viewModelScope.launch {
        val city = container.prefs.value.cities.getOrNull(index) ?: return@launch
        container.settings.update { p -> p.copy(cities = p.cities.filterIndexed { i, _ -> i != index }, selectedCity = 0) }
        container.weather.remove(city)
    }

    fun makeCityFirst(index: Int) = viewModelScope.launch {
        container.settings.update { p ->
            val c = p.cities.getOrNull(index) ?: return@update p
            p.copy(cities = listOf(c) + p.cities.filterIndexed { i, _ -> i != index }, selectedCity = 0)
        }
    }

    suspend fun searchCities(query: String): List<City> = container.weather.search(query)
```
Imports: `kotlinx.coroutines.Job`, `kotlinx.coroutines.delay`, `kotlinx.coroutines.isActive`, `ru.polkatv.launcher.settings.City`, `ru.polkatv.launcher.weather.CityWeather`, `ru.polkatv.launcher.weather.WeatherRepository`.

- [ ] **Step 5: Implement the shelf**

`ui/home/WeatherShelf.kt`:
```kotlin
package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.GlassBorder
import ru.polkatv.launcher.ui.theme.GlassFill
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary
import ru.polkatv.launcher.ui.weather.WeatherStrings
import ru.polkatv.launcher.ui.weather.vector
import ru.polkatv.launcher.weather.CityWeather
import ru.polkatv.launcher.weather.WmoCodes
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherShelf(
    cities: List<CityWeather>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onLongPress: () -> Unit,
    onAddCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.cardCorner)
    val current = cities.getOrNull(selected)
    Card(
        onClick = { if (current == null) onAddCity() else onOpenDetail() },
        onLongClick = onLongPress,
        modifier = modifier.onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown || cities.size < 2) return@onPreviewKeyEvent false
            when (e.key) {
                Key.DirectionLeft -> { onSelect((selected - 1 + cities.size) % cities.size); true }
                Key.DirectionRight -> { onSelect((selected + 1) % cities.size); true }
                else -> false
            }
        },
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = GlassFill, focusedContainerColor = GlassFill.copy(alpha = 0.6f)),
        scale = CardDefaults.scale(focusedScale = 1.01f),
        border = CardDefaults.border(border = Border(BorderStroke(1.dp, GlassBorder), shape = shape), focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp)) {
            when {
                current == null -> Text(stringResource(R.string.wx_add_city_prompt), fontSize = 26.sp, color = TextSecondary, modifier = Modifier.align(Alignment.Center))
                current.weather == null -> Column(Modifier.align(Alignment.CenterStart)) {
                    Text(current.city.displayName, fontSize = 30.sp, color = TextPrimary)
                    Text(stringResource(if (current.error) R.string.wx_no_data else R.string.wx_unknown), fontSize = 20.sp, color = TextSecondary)
                }
                else -> WeatherContent(current)
            }
            if (cities.size > 1) Dots(cities.size, selected, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WeatherContent(cw: CityWeather) {
    val w = cw.weather!!
    val c = w.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(cw.city.displayName, fontSize = 30.sp, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(WmoCodes.icon(c.code, c.isDay).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(WeatherStrings.description(WmoCodes.kind(c.code))), fontSize = 20.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Detail(stringResource(R.string.wx_feels), stringResource(R.string.unit_temp, WeatherStrings.temp(c.feelsLikeC)))
                    Detail(stringResource(R.string.wx_wind), stringResource(R.string.unit_ms, String.format("%.0f", c.windMs)) + " " + stringResource(WeatherStrings.windDir(c.windDir)))
                    Detail(stringResource(R.string.wx_humidity), stringResource(R.string.unit_percent, c.humidity))
                    Detail(stringResource(R.string.wx_pressure), stringResource(R.string.unit_mmhg, c.pressureMmHg))
                    c.precipProbability?.let { Detail(stringResource(R.string.wx_precip), stringResource(R.string.unit_percent, it)) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.unit_temp, WeatherStrings.temp(c.tempC)), fontSize = Dimens.tempSize, color = TextPrimary, lineHeight = Dimens.tempSize)
                if (cw.stale) {
                    val (res, qty) = WeatherStrings.ago(System.currentTimeMillis() - w.fetchedAtEpochMs)
                    Text(stringResource(R.string.wx_updated_ago, stringResource(res, qty)), fontSize = 16.sp, color = TextTertiary)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            w.hourly.take(8).forEach { h ->
                Column(
                    Modifier.weight(1f).background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(fmt.format(h.time), fontSize = 15.sp, color = TextTertiary)
                    Icon(WmoCodes.icon(h.code, h.isDay).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp).padding(vertical = 2.dp))
                    Text(stringResource(R.string.unit_temp, WeatherStrings.temp(h.tempC)), fontSize = 18.sp, color = TextPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Detail(label: String, value: String) {
    Column { Text(label, fontSize = 15.sp, color = TextTertiary); Text(value, fontSize = 20.sp, color = TextPrimary) }
}

@Composable
private fun Dots(count: Int, selected: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i -> Box(Modifier.size(8.dp).background(if (i == selected) Color.White else Color(0x4DFFFFFF), CircleShape)) }
    }
}
```

- [ ] **Step 6: Add-city dialog and city menu**

`ui/weather/AddCityDialog.kt`:
```kotlin
package ru.polkatv.launcher.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import ru.polkatv.launcher.R
import ru.polkatv.launcher.settings.City
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary

private sealed interface SearchState { object Idle : SearchState; object Loading : SearchState; object Error : SearchState; data class Results(val cities: List<City>) : SearchState }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddCityDialog(search: suspend (String) -> List<City>, onPick: (City) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(query) {
        if (query.trim().length < 2) { state = SearchState.Idle; return@LaunchedEffect }
        delay(400)
        state = SearchState.Loading
        state = runCatching { SearchState.Results(search(query.trim())) }.getOrElse { SearchState.Error }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.add_city_title), width = 640) {
        Box(Modifier.fillMaxWidth().background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (query.isEmpty()) Text(stringResource(R.string.add_city_hint), fontSize = 22.sp, color = TextTertiary)
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            SearchState.Idle -> Unit
            SearchState.Loading -> Text("…", fontSize = 20.sp, color = TextSecondary)
            SearchState.Error -> Text(stringResource(R.string.add_city_no_network), fontSize = 20.sp, color = TextSecondary)
            is SearchState.Results -> if (s.cities.isEmpty()) Text(stringResource(R.string.add_city_nothing), fontSize = 20.sp, color = TextSecondary)
            else LazyColumn(Modifier.height(420.dp)) {
                items(s.cities, key = { it.key }) { c ->
                    ListItem(selected = false, onClick = { onPick(c) },
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(listOfNotNull(c.region, c.country).joinToString(", ")) })
                }
            }
        }
    }
}
```
City menu (add to `ui/home/WeatherShelf.kt`):
```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CityMenuDialog(title: String, hasCity: Boolean, canAdd: Boolean, onAdd: () -> Unit, onRemove: () -> Unit, onMakeFirst: () -> Unit, onDismiss: () -> Unit) {
    ru.polkatv.launcher.ui.common.PolkaDialog(onDismiss = onDismiss, title = title) {
        if (canAdd) androidx.tv.material3.ListItem(selected = false, onClick = onAdd, headlineContent = { Text(stringResource(R.string.city_menu_add)) })
        if (hasCity) androidx.tv.material3.ListItem(selected = false, onClick = onRemove, headlineContent = { Text(stringResource(R.string.city_menu_remove)) })
        if (hasCity) androidx.tv.material3.ListItem(selected = false, onClick = onMakeFirst, headlineContent = { Text(stringResource(R.string.city_menu_first)) })
        androidx.tv.material3.ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
```
The caller passes the title: the selected city's `displayName`, or «Добавить город» (`R.string.add_city_title`) when there is no city yet.

- [ ] **Step 7: Detail screen**

`ui/weather/WeatherDetailScreen.kt`:
```kotlin
package ru.polkatv.launcher.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary
import ru.polkatv.launcher.weather.CityWeather
import ru.polkatv.launcher.weather.WmoCodes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherDetailScreen(cityWeather: CityWeather?, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
        val w = cityWeather?.weather
        if (cityWeather == null || w == null) {
            Text(stringResource(R.string.wx_no_data), fontSize = 26.sp, color = TextSecondary, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(cityWeather.city.displayName, fontSize = 36.sp, color = TextPrimary)
                    Text(stringResource(WeatherStrings.description(WmoCodes.kind(w.current.code))), fontSize = 22.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    val d0 = w.daily.firstOrNull()
                    if (d0 != null) {
                        val tf = DateTimeFormatter.ofPattern("HH:mm")
                        Text(stringResource(R.string.wx_sunrise, tf.format(d0.sunrise)) + "   " + stringResource(R.string.wx_sunset, tf.format(d0.sunset)), fontSize = 18.sp, color = TextTertiary)
                    }
                }
                Text(stringResource(R.string.unit_temp, WeatherStrings.temp(w.current.tempC)), fontSize = 88.sp, color = TextPrimary, lineHeight = 88.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.wx_detail_hourly), fontSize = 20.sp, color = TextTertiary)
            Spacer(Modifier.height(10.dp))
            val hf = DateTimeFormatter.ofPattern("HH:mm")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(w.hourly, key = { it.time.toString() }) { h ->
                    Column(Modifier.width(92.dp).background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(hf.format(h.time), fontSize = 15.sp, color = TextTertiary)
                        Icon(WmoCodes.icon(h.code, h.isDay).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp).padding(vertical = 3.dp))
                        Text(stringResource(R.string.unit_temp, WeatherStrings.temp(h.tempC)), fontSize = 18.sp, color = TextPrimary)
                        h.precipProbability?.let { if (it > 0) Text(stringResource(R.string.unit_percent, it), fontSize = 13.sp, color = TextTertiary) }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.wx_detail_daily), fontSize = 20.sp, color = TextTertiary)
            Spacer(Modifier.height(10.dp))
            val df = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
            w.daily.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (d.date == LocalDate.now()) stringResource(R.string.wx_today) else df.format(d.date), fontSize = 20.sp, color = TextPrimary, modifier = Modifier.width(360.dp))
                    Icon(WmoCodes.icon(d.code, true).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(WeatherStrings.description(WmoCodes.kind(d.code))), fontSize = 18.sp, color = TextSecondary, modifier = Modifier.width(360.dp))
                    Text(d.precipProbabilityMax?.let { stringResource(R.string.unit_percent, it) } ?: "", fontSize = 18.sp, color = TextTertiary, modifier = Modifier.width(90.dp))
                    Text(stringResource(R.string.unit_temp, WeatherStrings.temp(d.tMinC)) + " … " + stringResource(R.string.unit_temp, WeatherStrings.temp(d.tMaxC)), fontSize = 20.sp, color = TextPrimary)
                }
            }
        }
    }
}
```

- [ ] **Step 8: Wire into HomeScreen and PolkaApp**

In `HomeScreen`: replace the `weatherShelf: @Composable (Modifier) -> Unit` parameter with `onOpenWeather: (Int) -> Unit`; add state `var cityMenu by remember { mutableStateOf(false) }`, `var addCity by remember { mutableStateOf(false) }`; add `LaunchedEffect(Unit) { vm.startWeatherLoop() }` and `DisposableEffect(Unit) { onDispose { vm.stopWeatherLoop() } }`; in the Column, replace the placeholder call with:
```kotlin
            WeatherShelf(
                cities = state.cities, selected = state.selectedCity,
                onSelect = vm::selectCity,
                onOpenDetail = { onOpenWeather(state.selectedCity) },
                onLongPress = { cityMenu = true },
                onAddCity = { addCity = true },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
```
and after the dialogs:
```kotlin
    if (cityMenu) {
        val current = state.cities.getOrNull(state.selectedCity)
        CityMenuDialog(
            title = current?.city?.displayName ?: stringResource(R.string.add_city_title),
            hasCity = current != null, canAdd = state.cities.size < 6,
            onAdd = { cityMenu = false; addCity = true },
            onRemove = { vm.removeCity(state.selectedCity); cityMenu = false },
            onMakeFirst = { vm.makeCityFirst(state.selectedCity); cityMenu = false },
            onDismiss = { cityMenu = false },
        )
    }
    if (addCity) AddCityDialog(search = { vm.searchCities(it) }, onPick = { vm.addCity(it); addCity = false }, onDismiss = { addCity = false })
```
In `PolkaApp`: pass `onOpenWeather = { nav.navigate(Routes.weather(it)) }` and add the route:
```kotlin
        composable(Routes.WEATHER, arguments = listOf(navArgument("index") { type = NavType.IntType })) { entry ->
            val index = entry.arguments?.getInt("index") ?: 0
            val prefs by container.prefs.collectAsStateWithLifecycle()
            val states by container.weather.states.collectAsStateWithLifecycle()
            val city = prefs.cities.getOrNull(index)
            WeatherDetailScreen(cityWeather = city?.let { states[it.key] ?: CityWeather(it) }, onBack = { nav.popBackStack() })
        }
```
(imports `androidx.navigation.NavType`, `androidx.navigation.navArgument`, `androidx.lifecycle.compose.collectAsStateWithLifecycle`, `ru.polkatv.launcher.weather.CityWeather`, `ru.polkatv.launcher.ui.weather.WeatherDetailScreen`.)

- [ ] **Step 9: Build, install, verify on the TV**

```bash
./gradlew --no-daemon testDebugUnitTest && tools/install_tv.sh
TV=192.168.100.2:5555; sleep 4
adb -s $TV shell input keyevent 19                      # up → shelf (shows «Добавьте город…»)
adb -s $TV shell input keyevent 23; sleep 1             # OK → add-city dialog
adb -s $TV shell input text "Moskva"; sleep 3; adb -s $TV exec-out screencap -p > /tmp/polka_addcity.png
adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 23; sleep 4   # pick first result
adb -s $TV exec-out screencap -p > /tmp/polka_weather1.png
adb -s $TV shell input keyevent --longpress 23; sleep 1; adb -s $TV shell input keyevent 23; sleep 1   # menu → add city
adb -s $TV shell input text "Sochi"; sleep 3; adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 23; sleep 4
adb -s $TV shell input keyevent 21; sleep 1; adb -s $TV exec-out screencap -p > /tmp/polka_weather2.png   # left → back to Москва
adb -s $TV shell input keyevent 23; sleep 2; adb -s $TV exec-out screencap -p > /tmp/polka_detail.png     # OK → detail
adb -s $TV shell input keyevent 4
```
Expected: suggestions list shows «Москва, Россия»; shelf shows Москва with temperature, details, 8 hourly chips and two dots after Sochi is added; detail screen shows 24 hours and 7 days with sunrise/sunset. Then test offline: `adb -s $TV shell svc wifi disable; sleep 5; adb -s $TV shell svc wifi enable` — if ADB drops (Wi-Fi only), skip and rely on the unit tests for the stale path, noting that in the report.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: weather shelf with multi-city switching, add-city search and detail screen

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 9: System actions, network status and the Inputs dialog

Implements spec §4.6, §6.2, §6.3 and the system-action rows of §8. Deliverable: every header icon does its job; Inputs dialog lists HDMI/antenna with state and switches input.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/system/InputsController.kt`, `app/src/main/java/ru/polkatv/launcher/ui/inputs/InputsDialog.kt`, `app/src/test/java/ru/polkatv/launcher/system/InputStateMapperTest.kt`
- Modify: `system/SystemActions.kt`, `system/NetworkStatus.kt`, `AppContainer.kt`, `MainActivity.kt`, `ui/home/HomeScreen.kt`, `ui/PolkaApp.kt`, `res/values/strings.xml`, `res/values-en/strings.xml`

**Interfaces:**
- Produces:
  - `SystemActions` gains `fun openTvSettings(): Boolean`, `fun openNotifications(): Boolean`, `fun openNetworkSettings(): Boolean`, `fun openLiveTv(): Boolean`
  - `class NetworkStatus(context) { val state: StateFlow<NetState>; fun start(); fun stop() }`
  - `enum class InputState { CONNECTED, NO_SIGNAL, DISCONNECTED, UNKNOWN }`, `data class TvInput(val id: String, val label: String, val state: InputState)`, `object InputStateMapper { fun map(tvInputManagerState: Int): InputState }`, `class InputsController(context) { fun list(): List<TvInput>; fun switchTo(input: TvInput): Boolean }`
  - `@Composable fun InputsDialog(inputs: List<TvInput>, onPick: (TvInput) -> Unit, onDismiss: () -> Unit)`
  - `HomeScreen` signature becomes `HomeScreen(vm, container, onOpenAllApps: () -> Unit, onOpenWeather: (Int) -> Unit, onOpenSettings: () -> Unit)` and builds `HeaderActions` internally.

- [ ] **Step 1: Strings**

`values/strings.xml`:
```xml
    <string name="inputs_title">Входы</string>
    <string name="input_state_connected">Подключено</string>
    <string name="input_state_no_signal">Нет сигнала</string>
    <string name="input_state_disconnected">Не подключено</string>
    <string name="input_state_unknown">—</string>
    <string name="inputs_none">Входы не найдены</string>
```
`values-en/strings.xml`: Inputs / Connected / No signal / Not connected / — / No inputs found.

- [ ] **Step 2: Failing test for the state mapper**

`app/src/test/java/ru/polkatv/launcher/system/InputStateMapperTest.kt`:
```kotlin
package ru.polkatv.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Test

class InputStateMapperTest {
    // TvInputManager constants: INPUT_STATE_CONNECTED=0, INPUT_STATE_CONNECTED_STANDBY=1, INPUT_STATE_DISCONNECTED=2
    @Test fun `maps framework states`() {
        assertEquals(InputState.CONNECTED, InputStateMapper.map(0))
        assertEquals(InputState.NO_SIGNAL, InputStateMapper.map(1))
        assertEquals(InputState.DISCONNECTED, InputStateMapper.map(2))
        assertEquals(InputState.UNKNOWN, InputStateMapper.map(99))
    }
}
```
Run: `./gradlew --no-daemon testDebugUnitTest --tests "ru.polkatv.launcher.system.InputStateMapperTest"` → compilation FAILS.

- [ ] **Step 3: Implement system package**

Extend `system/SystemActions.kt`:
```kotlin
    fun openTvSettings(): Boolean = launch(Intent(Settings.ACTION_SETTINGS))

    fun openNotifications(): Boolean = launch(Intent("com.android.tv.NOTIFICATIONS_PANEL"))

    fun openNetworkSettings(): Boolean =
        launch(Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.connectivity.NetworkActivity"))
            || launch(Intent(Settings.ACTION_WIFI_SETTINGS))

    fun openLiveTv(): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage("com.mitv.livetv") ?: pm.getLaunchIntentForPackage("com.mitv.livetv") ?: return false
        return launch(intent)
    }
```
`system/NetworkStatus.kt` (replace the file, keeping the enum):
```kotlin
package ru.polkatv.launcher.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetState { NONE, WIFI, ETHERNET }

class NetworkStatus(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _state = MutableStateFlow(current())
    val state: StateFlow<NetState> = _state.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _state.value = current() }
        override fun onLost(network: Network) { _state.value = current() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { _state.value = toState(caps) }
    }

    fun start() { runCatching { cm.registerDefaultNetworkCallback(callback) }; _state.value = current() }
    fun stop() { runCatching { cm.unregisterNetworkCallback(callback) } }

    private fun current(): NetState = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }?.let(::toState) ?: NetState.NONE

    private fun toState(caps: NetworkCapabilities): NetState = when {
        !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetState.NONE
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetState.ETHERNET
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetState.WIFI
        else -> NetState.WIFI
    }
}
```
`system/InputsController.kt`:
```kotlin
package ru.polkatv.launcher.system

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.util.Log

enum class InputState { CONNECTED, NO_SIGNAL, DISCONNECTED, UNKNOWN }

data class TvInput(val id: String, val label: String, val state: InputState)

object InputStateMapper {
    fun map(tvInputManagerState: Int): InputState = when (tvInputManagerState) {
        TvInputManager.INPUT_STATE_CONNECTED -> InputState.CONNECTED            // 0
        TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> InputState.NO_SIGNAL    // 1
        TvInputManager.INPUT_STATE_DISCONNECTED -> InputState.DISCONNECTED      // 2
        else -> InputState.UNKNOWN
    }
}

class InputsController(private val context: Context, private val actions: SystemActions) {
    private val tim = context.getSystemService(TvInputManager::class.java)

    fun list(): List<TvInput> {
        val infos: List<TvInputInfo> = runCatching { tim.tvInputList }.getOrDefault(emptyList())
        return infos.filter { it.isPassthroughInput }
            .map { info ->
                val label = info.loadLabel(context)?.toString()?.takeIf { it.isNotBlank() } ?: info.id.substringAfterLast('/')
                TvInput(info.id, label, InputStateMapper.map(runCatching { tim.getInputState(info.id) }.getOrDefault(-1)))
            }
            .sortedBy { it.label }
    }

    /**
     * Strategy order (spec §6.2): 1) passthrough URI to Live TV, 2) passthrough URI to any handler, 3) plain Live TV launch.
     * Once the working strategy is confirmed on the device, keep the others as fallbacks.
     */
    fun switchTo(input: TvInput): Boolean {
        val uri = TvContract.buildChannelUriForPassthroughInput(input.id)
        val toLiveTv = Intent(Intent.ACTION_VIEW, uri).setPackage("com.mitv.livetv")
        if (actions.launch(toLiveTv)) { Log.i("Polka", "input switch via Live TV passthrough"); return true }
        val any = Intent(Intent.ACTION_VIEW, uri)
        if (actions.launch(any)) { Log.i("Polka", "input switch via generic passthrough"); return true }
        Log.i("Polka", "input switch fallback: launching Live TV")
        return actions.openLiveTv()
    }
}
```
Note: with `isReturnDefaultValues = true` the `TvInputManager.INPUT_STATE_*` constants are compile-time ints (0/1/2) and the mapper test passes on the JVM.

`AppContainer.kt` add `val network = NetworkStatus(appContext)` and `val inputs = InputsController(appContext, system)`; `MainActivity.onStart/onStop` call `container.network.start()/stop()`.

- [ ] **Step 4: Inputs dialog and header wiring**

`ui/inputs/InputsDialog.kt`:
```kotlin
package ru.polkatv.launcher.ui.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.system.InputState
import ru.polkatv.launcher.system.TvInput
import ru.polkatv.launcher.ui.common.PolkaDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InputsDialog(inputs: List<TvInput>, onPick: (TvInput) -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.inputs_title)) {
        if (inputs.isEmpty()) Text(stringResource(R.string.inputs_none))
        inputs.forEach { input ->
            val stateRes = when (input.state) {
                InputState.CONNECTED -> R.string.input_state_connected
                InputState.NO_SIGNAL -> R.string.input_state_no_signal
                InputState.DISCONNECTED -> R.string.input_state_disconnected
                InputState.UNKNOWN -> R.string.input_state_unknown
            }
            ListItem(selected = false, onClick = { onPick(input) }, headlineContent = { Text(input.label) }, supportingContent = { Text(stringResource(stateRes)) })
        }
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
```
In `HomeScreen`: change the signature to `HomeScreen(vm, container, onOpenAllApps, onOpenWeather, onOpenSettings)`; add
```kotlin
    val context = LocalContext.current
    val net by container.network.state.collectAsStateWithLifecycle()
    var inputs by remember { mutableStateOf<List<TvInput>?>(null) }
    fun orToast(ok: Boolean) { if (!ok) Toast.makeText(context, R.string.unavailable_on_tv, Toast.LENGTH_SHORT).show() }
    val headerActions = HeaderActions(
        onInputs = { inputs = container.inputs.list() },
        onNotifications = { orToast(container.system.openNotifications()) },
        onNetwork = { orToast(container.system.openNetworkSettings()) },
        onTvSettings = { orToast(container.system.openTvSettings()) },
        onPolkaSettings = onOpenSettings,
    )
```
pass `net = net` and `actions = headerActions` to `Header`, and render:
```kotlin
    inputs?.let { list -> InputsDialog(inputs = list, onPick = { inputs = null; orToast(container.inputs.switchTo(it)) }, onDismiss = { inputs = null }) }
```
In `PolkaApp` update the call: `HomeScreen(vm, container, onOpenAllApps = { nav.navigate(Routes.ALL_APPS) }, onOpenWeather = { nav.navigate(Routes.weather(it)) }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })` (the settings route is added in Task 10; until then navigating to it throws — add a temporary `composable(Routes.SETTINGS) { Box(Modifier.fillMaxSize().background(Color.Black)) }` placeholder now).

- [ ] **Step 5: Run tests, install, verify each icon and the input switch**

```bash
./gradlew --no-daemon testDebugUnitTest && tools/install_tv.sh
TV=192.168.100.2:5555; sleep 4
adb -s $TV shell input keyevent 19; adb -s $TV shell input keyevent 19     # to header icons (first = Inputs)
adb -s $TV shell input keyevent 23; sleep 1; adb -s $TV exec-out screencap -p > /tmp/polka_inputs.png
adb -s $TV shell input keyevent 23; sleep 4                                # pick first input (HDMI 1)
adb -s $TV shell dumpsys activity activities | grep topResumedActivity; adb -s $TV logcat -d -s Polka | tail -3
adb -s $TV shell am start -n ru.polkatv.launcher/.MainActivity; sleep 2
adb -s $TV shell input keyevent 19; adb -s $TV shell input keyevent 19; adb -s $TV shell input keyevent 22; adb -s $TV shell input keyevent 23; sleep 2
adb -s $TV shell dumpsys activity activities | grep topResumedActivity      # expect tvrecommendations NotificationsSidePanelActivity
adb -s $TV shell input keyevent 4; adb -s $TV shell input keyevent 22; adb -s $TV shell input keyevent 23; sleep 2
adb -s $TV shell dumpsys activity activities | grep topResumedActivity      # expect com.android.tv.settings NetworkActivity
adb -s $TV shell input keyevent 4; adb -s $TV shell input keyevent 22; adb -s $TV shell input keyevent 23; sleep 2
adb -s $TV shell dumpsys activity activities | grep topResumedActivity      # expect com.android.tv.settings MainSettings
adb -s $TV shell input keyevent 4
```
Expected: inputs dialog lists HDMI 1, HDMI 2, HDMI 3, ATV with states (HDMI 2/3 «Подключено» per the earlier `dumpsys tv_input`, state=1 means standby → «Нет сигнала»; adjust expectations to what is physically connected). After picking HDMI 1 the TV switches to that input (top activity is Live TV `com.mitv.livetv`) — record in `InputsController` KDoc which strategy logged success. Each other icon opens the expected system screen.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: system shortcuts, live network status and HDMI inputs dialog

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 10: «Настройки Полки» screen

Implements spec §4.8. Deliverable: two-pane settings screen that edits every preference through `SettingsRepository`.

**Files:**
- Create: `app/src/main/java/ru/polkatv/launcher/ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`
- Modify: `ui/PolkaApp.kt` (replace the placeholder route), `ui/apps/AppPickerDialog.kt` (add `extraAction`), `app/build.gradle.kts` (`buildConfig = true`), `res/values/strings.xml`, `res/values-en/strings.xml`

**Interfaces:**
- Consumes: `SettingsRepository.update`, `Prefs`, `TileOps.resize/replace`, `AppsRepository.apps`, `TileResolver`, `AppPickerDialog`, `AddCityDialog`, `WeatherRepository.search/remove`.
- Produces: `class SettingsViewModel(container) { val prefs: StateFlow<Prefs>; val tiles: StateFlow<List<TileSlot>>; val apps: StateFlow<List<TvApp>>; fun setTileCount(n: Int); fun setTile(index: Int, pkg: String?); fun addCity(city: City); fun removeCity(index: Int); fun moveCity(index: Int, direction: Int); fun setWallpaperInterval(min: Int); fun setDim(level: DimLevel); fun setClockFormat(f: ClockFormat); fun setShowDate(b: Boolean); suspend fun searchCities(q: String): List<City> }`, `@Composable fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit)`.

- [ ] **Step 1: Strings**

`values/strings.xml`:
```xml
    <string name="settings_title">Настройки Полки</string>
    <string name="settings_tiles">Плитки</string>
    <string name="settings_cities">Города</string>
    <string name="settings_background">Фон</string>
    <string name="settings_clock">Часы и дата</string>
    <string name="settings_about">О приложении</string>
    <string name="settings_tile_count">Количество плиток: %1$d</string>
    <string name="settings_tile_clear">Очистить плитку</string>
    <string name="settings_city_add">Добавить город</string>
    <string name="settings_city_up">Выше</string>
    <string name="settings_city_down">Ниже</string>
    <string name="settings_city_remove">Удалить</string>
    <string name="settings_interval">Смена фона</string>
    <string name="settings_interval_off">Выключена</string>
    <string name="settings_interval_min">Каждые %1$d мин</string>
    <string name="settings_dim">Затемнение</string>
    <string name="settings_dim_light">Лёгкое</string>
    <string name="settings_dim_medium">Среднее</string>
    <string name="settings_clock_format">Формат времени</string>
    <string name="settings_clock_system">Как в системе</string>
    <string name="settings_clock_24">24 часа</string>
    <string name="settings_clock_12">12 часов</string>
    <string name="settings_show_date">Показывать дату</string>
    <string name="settings_version">Версия %1$s</string>
    <string name="settings_set_home_hint">Сделать домашним экраном: adb shell cmd package set-home-activity ru.polkatv.launcher/.MainActivity</string>
    <string name="settings_restore_hint">Вернуть Google TV: adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity</string>
    <string name="on">Вкл</string>
    <string name="off">Выкл</string>
```
`values-en/strings.xml`: the English equivalents with the same keys (Polka settings, Tiles, Cities, Background, Clock and date, About, `Tile count: %1$d`, Clear tile, Add city, Up, Down, Remove, Background change, Off, `Every %1$d min`, Dimming, Light, Medium, Time format, As system, 24-hour, 12-hour, Show date, `Version %1$s`, the two adb hints, On, Off).

- [ ] **Step 2: ViewModel**

`ui/settings/SettingsViewModel.kt`:
```kotlin
package ru.polkatv.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileResolver
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.City
import ru.polkatv.launcher.settings.ClockFormat
import ru.polkatv.launcher.settings.DimLevel
import ru.polkatv.launcher.settings.Prefs
import ru.polkatv.launcher.settings.TileOps

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val prefs: StateFlow<Prefs> = container.prefs
    val apps: StateFlow<List<TvApp>> = container.apps.apps
    val tiles: StateFlow<List<TileSlot>> = combine(container.prefs, container.apps.apps) { p, a -> TileResolver.resolve(p.tiles, a) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun edit(f: (Prefs) -> Prefs) = viewModelScope.launch { container.settings.update(f) }

    fun setTileCount(n: Int) = edit { it.copy(tiles = TileOps.resize(it.tiles, n)) }
    fun setTile(index: Int, pkg: String?) = edit { it.copy(tiles = TileOps.replace(it.tiles, index, pkg)) }

    fun addCity(city: City) {
        edit { p -> if (p.cities.size >= 6 || p.cities.any { it.key == city.key }) p else p.copy(cities = p.cities + city) }
        viewModelScope.launch { container.weather.refresh(city) }
    }
    fun removeCity(index: Int) {
        val c = container.prefs.value.cities.getOrNull(index) ?: return
        edit { p -> p.copy(cities = p.cities.filterIndexed { i, _ -> i != index }, selectedCity = 0) }
        container.weather.remove(c)
    }
    fun moveCity(index: Int, direction: Int) = edit { p ->
        val t = index + direction
        if (index !in p.cities.indices || t !in p.cities.indices) p
        else p.copy(cities = p.cities.toMutableList().also { l -> l[index] = p.cities[t]; l[t] = p.cities[index] }, selectedCity = 0)
    }

    fun setWallpaperInterval(min: Int) = edit { it.copy(wallpaperIntervalMinutes = min) }
    fun setDim(level: DimLevel) = edit { it.copy(dimLevel = level) }
    fun setClockFormat(f: ClockFormat) = edit { it.copy(clockFormat = f) }
    fun setShowDate(b: Boolean) = edit { it.copy(showDate = b) }

    suspend fun searchCities(q: String): List<City> = container.weather.search(q)
}
```

- [ ] **Step 3: Screen**

`ui/settings/SettingsScreen.kt`:
```kotlin
package ru.polkatv.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import ru.polkatv.launcher.BuildConfig
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.settings.ClockFormat
import ru.polkatv.launcher.settings.DimLevel
import ru.polkatv.launcher.ui.apps.AppPickerDialog
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextTertiary
import ru.polkatv.launcher.ui.weather.AddCityDialog

private enum class Section(val titleRes: Int) {
    TILES(R.string.settings_tiles), CITIES(R.string.settings_cities), BACKGROUND(R.string.settings_background),
    CLOCK(R.string.settings_clock), ABOUT(R.string.settings_about),
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(Section.TILES) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    var addCity by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf<Int?>(null) }

    Row(Modifier.fillMaxSize().background(Color(0xFF0B1020)).padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
        Column(Modifier.width(360.dp).fillMaxHeight()) {
            Text(stringResource(R.string.settings_title), fontSize = 32.sp, color = TextPrimary)
            Spacer(Modifier.height(20.dp))
            Section.values().forEach { s ->
                ListItem(selected = section == s, onClick = { section = s }, headlineContent = { Text(stringResource(s.titleRes)) })
            }
        }
        Spacer(Modifier.width(48.dp))
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (section) {
                Section.TILES -> {
                    Text(stringResource(R.string.settings_tile_count, prefs.tiles.size), fontSize = 18.sp, color = TextTertiary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5, 6).forEach { n ->
                            ListItem(selected = prefs.tiles.size == n, onClick = { vm.setTileCount(n) }, headlineContent = { Text("$n") },
                                leadingContent = { RadioButton(selected = prefs.tiles.size == n, onClick = null) }, modifier = Modifier.width(120.dp))
                        }
                    }
                    tiles.forEach { slot ->
                        val label = when (slot) {
                            is TileSlot.App -> stringResource(R.string.tile_n, slot.index + 1, slot.app.label)
                            is TileSlot.Empty -> stringResource(R.string.tile_n_empty, slot.index + 1)
                        }
                        ListItem(selected = false, onClick = { pickFor = slot.index }, headlineContent = { Text(label) })
                    }
                }
                Section.CITIES -> {
                    prefs.cities.forEachIndexed { i, c ->
                        ListItem(selected = false, onClick = { cityMenu = i }, headlineContent = { Text(c.displayName) }, supportingContent = { Text(c.country ?: "") })
                    }
                    if (prefs.cities.size < 6) ListItem(selected = false, onClick = { addCity = true }, headlineContent = { Text(stringResource(R.string.settings_city_add)) })
                }
                Section.BACKGROUND -> {
                    Text(stringResource(R.string.settings_interval), fontSize = 18.sp, color = TextTertiary)
                    listOf(0, 1, 5, 15, 30).forEach { m ->
                        val label = if (m == 0) stringResource(R.string.settings_interval_off) else stringResource(R.string.settings_interval_min, m)
                        ListItem(selected = prefs.wallpaperIntervalMinutes == m, onClick = { vm.setWallpaperInterval(m) }, headlineContent = { Text(label) },
                            leadingContent = { RadioButton(selected = prefs.wallpaperIntervalMinutes == m, onClick = null) })
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.settings_dim), fontSize = 18.sp, color = TextTertiary)
                    listOf(DimLevel.LIGHT to R.string.settings_dim_light, DimLevel.MEDIUM to R.string.settings_dim_medium).forEach { (lvl, res) ->
                        ListItem(selected = prefs.dimLevel == lvl, onClick = { vm.setDim(lvl) }, headlineContent = { Text(stringResource(res)) },
                            leadingContent = { RadioButton(selected = prefs.dimLevel == lvl, onClick = null) })
                    }
                }
                Section.CLOCK -> {
                    Text(stringResource(R.string.settings_clock_format), fontSize = 18.sp, color = TextTertiary)
                    listOf(ClockFormat.SYSTEM to R.string.settings_clock_system, ClockFormat.H24 to R.string.settings_clock_24, ClockFormat.H12 to R.string.settings_clock_12).forEach { (f, res) ->
                        ListItem(selected = prefs.clockFormat == f, onClick = { vm.setClockFormat(f) }, headlineContent = { Text(stringResource(res)) },
                            leadingContent = { RadioButton(selected = prefs.clockFormat == f, onClick = null) })
                    }
                    Spacer(Modifier.height(16.dp))
                    ListItem(selected = false, onClick = { vm.setShowDate(!prefs.showDate) }, headlineContent = { Text(stringResource(R.string.settings_show_date)) },
                        trailingContent = { Switch(checked = prefs.showDate, onCheckedChange = null) })
                }
                Section.ABOUT -> {
                    Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), fontSize = 20.sp, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.settings_set_home_hint), fontSize = 16.sp, color = TextTertiary)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_restore_hint), fontSize = 16.sp, color = TextTertiary)
                }
            }
        }
    }

    pickFor?.let { index ->
        AppPickerDialog(
            apps = apps,
            onPick = { vm.setTile(index, it.packageName); pickFor = null },
            onDismiss = { pickFor = null },
            extraAction = stringResource(R.string.settings_tile_clear) to { vm.setTile(index, null); pickFor = null },
        )
    }
    if (addCity) AddCityDialog(search = { vm.searchCities(it) }, onPick = { vm.addCity(it); addCity = false }, onDismiss = { addCity = false })
    cityMenu?.let { i ->
        PolkaDialog(onDismiss = { cityMenu = null }, title = prefs.cities.getOrNull(i)?.displayName ?: "") {
            if (i > 0) ListItem(selected = false, onClick = { vm.moveCity(i, -1); cityMenu = null }, headlineContent = { Text(stringResource(R.string.settings_city_up)) })
            if (i < prefs.cities.size - 1) ListItem(selected = false, onClick = { vm.moveCity(i, +1); cityMenu = null }, headlineContent = { Text(stringResource(R.string.settings_city_down)) })
            ListItem(selected = false, onClick = { vm.removeCity(i); cityMenu = null }, headlineContent = { Text(stringResource(R.string.settings_city_remove)) })
            ListItem(selected = false, onClick = { cityMenu = null }, headlineContent = { Text(stringResource(R.string.action_cancel)) })
        }
    }
}
```
`AppPickerDialog` (file `ui/apps/AppPickerDialog.kt` from Task 5) gets a new optional parameter `extraAction: Pair<String, () -> Unit>? = null`; when non-null it is rendered as the first `ListItem` above the app list (`headlineContent = { Text(extraAction.first) }`, `onClick = extraAction.second`). Existing call sites compile unchanged thanks to the default. `BuildConfig` requires `buildFeatures { buildConfig = true }` in `app/build.gradle.kts` — add it next to `compose = true`.

In `PolkaApp`, replace the placeholder settings route with:
```kotlin
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container) } })
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
```

- [ ] **Step 4: Build, install, verify**

```bash
./gradlew --no-daemon testDebugUnitTest && tools/install_tv.sh
TV=192.168.100.2:5555; sleep 4
adb -s $TV shell input keyevent 19; adb -s $TV shell input keyevent 19; for i in 1 2 3 4; do adb -s $TV shell input keyevent 22; done; adb -s $TV shell input keyevent 23; sleep 2
adb -s $TV exec-out screencap -p > /tmp/polka_settings.png
adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 23; sleep 1   # Фон section
adb -s $TV shell input keyevent 22; adb -s $TV shell input keyevent 20; adb -s $TV shell input keyevent 23; sleep 1    # choose "Каждые 1 мин"
adb -s $TV exec-out screencap -p > /tmp/polka_settings_bg.png
adb -s $TV shell input keyevent 4; sleep 70; adb -s $TV exec-out screencap -p > /tmp/polka_home_after.png
```
Expected: settings screen with the left section list and right pane; selecting «Каждые 1 мин» persists (radio moves); after Back and 70 s the home wallpaper has changed. Also check `Плитки`: set count to 6 → home shows 6 tiles + «Все приложения». Restore interval to 5 min afterwards via the same path.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: Polka settings screen (tiles, cities, background, clock, about)

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
```

---

### Task 11: Make it the HOME app, reboot check, memory soak, release build, README

Implements spec §6.1, §9 (device checks 7–9), §10, §12 risk 2. Deliverable: «Полка» is the TV's home screen and survives a reboot; a signed release APK is installed; README documents install/rollback.

**Files:**
- Create: `README.md`, optionally `app/src/main/java/ru/polkatv/launcher/BootReceiver.kt` (only if the reboot check fails)
- Modify: `AndroidManifest.xml` (only with BootReceiver), `app/build.gradle.kts` (versionCode/versionName bump to 1.0.0 / 2)

- [ ] **Step 1: Set Polka as HOME and verify the Home button**

```bash
tools/install_tv.sh --set-home
TV=192.168.100.2:5555
adb -s $TV shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
adb -s $TV shell am start -n com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity; sleep 4
adb -s $TV shell input keyevent 3; sleep 2
adb -s $TV shell dumpsys activity activities | grep topResumedActivity
```
Expected: resolve shows `ru.polkatv.launcher/.MainActivity`; after HOME the top activity is `ru.polkatv.launcher/.MainActivity`.

- [ ] **Step 2: Reboot and verify HOME persists**

```bash
adb -s $TV reboot; sleep 90
for i in $(seq 1 20); do adb connect $TV >/dev/null 2>&1; [ "$(adb -s $TV get-state 2>/dev/null)" = "device" ] && break; sleep 5; done
sleep 60
adb -s $TV shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
adb -s $TV shell dumpsys activity activities | grep topResumedActivity
adb -s $TV exec-out screencap -p > /tmp/polka_after_reboot.png
```
Expected: HOME still resolves to Polka and the screen shows Polka (Xiaomi's PatchWall boot banner may show first; wait for it to hand over). If HOME was reset to Google TV, implement the fallback:

`BootReceiver.kt`:
```kotlin
package ru.polkatv.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { context.startActivity(i) }
    }
}
```
Manifest additions: `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />` and inside `<application>`:
```xml
        <receiver android:name=".BootReceiver" android:exported="true">
            <intent-filter><action android:name="android.intent.action.BOOT_COMPLETED" /></intent-filter>
        </receiver>
```
Also re-run `cmd package set-home-activity` and repeat the reboot check; record the outcome in README.

- [ ] **Step 3: Memory soak (30 minutes, 1-minute wallpaper interval)**

Set the interval to 1 minute in Настройки → Фон (or temporarily via the UI as in Task 10), then:
```bash
for i in $(seq 1 7); do adb -s $TV shell dumpsys meminfo ru.polkatv.launcher | grep -E "TOTAL PSS" ; sleep 300; done
```
Expected: TOTAL PSS stays within a 20 MB band (spec §9 item 7). If it climbs steadily, check that `WallpaperController` nulls `next` after swapping and that `TileArtLoader` cache is bounded (32). Restore the interval to 5 minutes.

- [ ] **Step 4: Release build and install**

In `app/build.gradle.kts` set `versionCode = 2`, `versionName = "1.0.0"`, then:
```bash
./gradlew --no-daemon testDebugUnitTest assembleRelease
ls -la app/build/outputs/apk/release/app-release.apk
adb -s $TV install -r app/build/outputs/apk/release/app-release.apk
adb -s $TV shell input keyevent 3; sleep 3; adb -s $TV exec-out screencap -p > /tmp/polka_release.png
adb -s $TV shell dumpsys package ru.polkatv.launcher | grep -E "versionName|flags="
```
Expected: install succeeds over the debug build (same key), home screen still shows the configured tiles and cities (DataStore preserved), `versionName=1.0.0`. If R8 strips serializers, the app crashes on start — check `adb logcat -d -s AndroidRuntime` and extend `proguard-rules.pro` with `-keep class ru.polkatv.launcher.**$$serializer { *; }`.

- [ ] **Step 5: README**

`README.md`:
```markdown
# PolkaTV («Полка»)

Домашний экран для Xiaomi MiTV-MOEU1 (Android TV 14) в стиле Apple TV: часы, погода для нескольких городов (Open-Meteo), 4–6 плиток приложений, служебные иконки, фото из заставки Xiaomi фоном.

Спецификация: `docs/superpowers/specs/2026-10-24-polkatv-launcher-design.md`. План: `docs/superpowers/plans/2026-10-24-polkatv-launcher.md`.

## Сборка и установка
- Требования: JDK 21, Android SDK (platform 34, build-tools 34.0.0), `keystore/keystore.properties` + `keystore/polkatv.jks` (не в git), `local.properties` с `sdk.dir`.
- `tools/prepare_wallpapers.sh` — пересобрать обои из `design/wallpapers-src/`.
- `tools/install_tv.sh` — собрать debug и установить; `tools/install_tv.sh --set-home` — ещё и сделать домашним экраном.
- `./gradlew assembleRelease` — релизная сборка (подписана тем же ключом).

## Откат
`tools/restore_googletv.sh` возвращает Google TV как домашний экран. «Полка» остаётся установленной; удалить: `adb shell pm uninstall ru.polkatv.launcher`.

## Управление
- Плитки: OK — запуск, долгое нажатие — заменить/сдвинуть. Последняя плитка — все приложения.
- Погода: ←/→ — города, OK — подробный прогноз, долгое нажатие — добавить/удалить город.
- Иконки справа сверху: входы, уведомления, сеть, настройки ТВ, настройки Полки.
```

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "chore: release 1.0.0, README, home-screen installation verified

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>"
git log --oneline | head -15
```
Then tell the user the branch `design/polkatv-spec` holds everything and ask whether to merge it into `main` (do not merge without their word).

---

## Spec coverage check (self-review)

| Spec section | Task |
|---|---|
| §2 device/constraints, §5.1 manifest, §10 build | 1 |
| §4.5 background, §5.2 wallpapers | 2 |
| §4.8 data model, §5 storage | 3 |
| §4.3 tiles source/art | 4 |
| §4.1 layout, §4.2 navigation, tile menu, header skeleton | 5 |
| §4.7 all apps, app info | 6 |
| §7 weather data, §8 network errors | 7 |
| §4.4 weather UI, refresh cadence, add city, detail screen | 8 |
| §4.6 icons actions, §6.2 inputs, §6.3 system screens, §8 unavailable screen | 9 |
| §4.8 settings screen | 10 |
| §6.1 HOME + reboot (+fallback receiver), §9 checks 7–9, §10 release, README | 11 |

Type consistency notes checked while writing: `TileSlot`/`TileResolver` (Task 4) are used unchanged in Tasks 5, 6, 10; `HeaderActions` (Task 5) built inside `HomeScreen` from Task 9 on; `CityWeather`/`WeatherRepository.FRESH_MS` (Task 7) used in Task 8; `AppPickerDialog` gains `extraAction` in Task 10 — if executing Task 10 before adjusting, add the parameter with a default so Task 5/6 call sites keep compiling; `NetState` enum lives in `system/NetworkStatus.kt` from Task 5 and the producing class is added in Task 9.
