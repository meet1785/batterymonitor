# Battery Monitor APK - Build Instructions

## What this app does
- Shows battery % with **2 decimal precision** (e.g. 73.42%) in a persistent notification
- Notification stays always visible, can't be swiped away
- Updates every 30 seconds AND instantly on battery change
- Auto-starts when phone boots
- Progress bar in notification
- Shows charging state ⚡ / discharging 🔋

---

## How to build the APK (pick one method)

---

### METHOD 1: Android Studio (Recommended, Free)
1. Download Android Studio: https://developer.android.com/studio
2. Open Android Studio → "Open an existing project" → select this folder
3. Wait for Gradle sync (2-3 min, downloads dependencies)
4. Click **Build → Build Bundle(s)/APK(s) → Build APK(s)**
5. APK saved to: `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer APK to phone via USB/Google Drive/WhatsApp
7. On phone: Settings → Install unknown apps → Allow → tap APK

---

### METHOD 2: GitHub Codespaces (Free, no install)
1. Push this folder to a GitHub repo
2. Click the green "Code" button → "Codespaces" → "New codespace"
3. In the terminal:
   ```
   sudo apt-get install -y openjdk-17-jdk
   export ANDROID_HOME=/workspace/android-sdk
   ./gradlew assembleDebug
   ```
4. Download `app/build/outputs/apk/debug/app-debug.apk`

---

### METHOD 3: Replit (Free, browser only)
1. Go to replit.com → New Repl → import from zip
2. Run: `./gradlew assembleDebug`
3. Download the APK from the file explorer

---

## Install on phone
1. Enable "Install from unknown sources":
   - Settings → Apps → Special app access → Install unknown apps
   - Allow your file manager or browser
2. Tap the APK file → Install
3. Open "Battery Monitor" app
4. Tap "Start Persistent Notification"
5. Done! Notification appears in status bar and notification shade.

---

## Permissions needed
- `FOREGROUND_SERVICE` - to run notification service
- `RECEIVE_BOOT_COMPLETED` - to auto-start on boot  
- `POST_NOTIFICATIONS` - to show notification (Android 13+)

No internet, no location, no data collection.
