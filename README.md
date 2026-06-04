# Battery Monitor

A persistent precision battery monitoring application for modern Android devices, including optimized builds with OnePlus Nord / OxygenOS 14+ background execution support.

## Features
- Displays battery percentage with **2 decimal precision** (e.g. 73.42%) in an un-swipeable persistent notification.
- Beautiful updated card-based dark UI with neon green glowing highlights.
- Auto-starts on device boot.
- Specific overrides for modern Android Battery Optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), preventing OxygenOS, ColorOS, and MIUI from silently killing the service.
- Visual progress bar and charging/discharging states built directly into the notification.

## Installation
1. A finalized, optimized APK has been built and is located in the root of this repository: `BatteryMonitor.apk`.
2. Transfer it to your device.
3. Enable "Install from unknown sources" and tap the APK to install.
4. Open the app and grant the prompt to "Ignore Battery Optimizations".

## Building from source
This project can be built using Gradle 8.2 and Java 17.
```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```
