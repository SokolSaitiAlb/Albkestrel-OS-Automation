# Albkestrel OS Automation

**Albkestrel OS Automation** is an offline-first Android automation engine inspired by Samsung Modes & Routines, specifically built and tuned for the **Google Pixel 8a (codename: Akita)** running standard Android (14/15) or **LineageOS for Akita**.

---

## 1. Product Vision & Architecture

Albkestrel OS Automation provides seamless, on-device automation routines without telemetry, cloud dependencies, or privileged system modification. This build operates strictly as a **standalone user APK for testing**, utilizing Android public SDK APIs and user-approved Special App Access (Notification Policy, Exact Alarms, Battery Optimization exemptions).

### Architecture Overview

- **Presentation Layer**: 100% Jetpack Compose with Material Design 3 (Material You dynamic theming on Android 12+), Single Activity architecture, and AndroidX Navigation Compose.
- **MVVM & Flow**: Unidirectional data flow using `ViewModel`, Kotlin Coroutines, and `StateFlow` collected via `collectAsStateWithLifecycle()`. Scoped to `NavBackStackEntry` to prevent stale backstack artifacts.
- **Domain & Rule Engine**: Clean, testable, decoupled domain logic (`RuleEngine`, `RoutineValidator`, `ExecutionStatusResolver`) evaluating conditions (Schedule, Battery, Wi-Fi, Manual) independently of Compose or Android frameworks.
- **Persistence**:
  - **Room Database**: Local SQLite storage for routines and audit execution logs.
  - **DataStore Preferences**: Local key-value store for user theme selection (System, Light, Dark) and automation toggles.
- **Background Execution**:
  - **WorkManager**: Deferrable periodic routine evaluation (battery drops, state transitions).
  - **AlarmManager**: Exact alarms (`SCHEDULE_EXACT_ALARM`) for punctual daily scheduled routines using deterministic `PendingIntent` identities.
  - **BroadcastReceivers**: Restores enabled routine alarms and periodic workers upon device restart (`BOOT_COMPLETED`), with `goAsync()` lifecycle management.

---

## 2. Public SDK vs. Privileged ROM Capabilities

This distinction is mandatory and strictly respected in Albkestrel OS:

| Feature / Action | Standalone APK (Current Implementation) | Future Privileged ROM / LineageOS Integration |
|---|---|---|
| **Do Not Disturb Mode** | Directly controlled via `NotificationManager.setInterruptionFilter()` after user grants **Notification Policy Access** in Special App Access. | Can be controlled directly as a system service without user prompts. |
| **Local Alerts & Notifications** | Managed via standard Android Notification Channels (`POST_NOTIFICATIONS`). | Direct Heads-Up Notification or SystemUI status bar ticker. |
| **App Launching** | Launches installed activities through `PackageManager.getLaunchIntentForPackage()`. Returns `UNSUPPORTED` if package is missing. | Background task switching or window pinning. |
| **System Settings** | Direct deep-linking to system settings (Display, Sound, Network, Battery Saver). | Direct modification of system settings (`WRITE_SECURE_SETTINGS`). |
| **Battery Saver** | **User-Guided Activation**: Guides user to `Settings.ACTION_BATTERY_SAVER_SETTINGS` toggle. Public SDK prohibits non-system apps from invoking `setPowerSaveModeEnabled()`. | Direct programmatic toggle via internal `IPowerManager.setPowerSaveModeEnabled()`. |
| **Airplane Mode / Cellular / GPS** | Deep-links to Network & Internet settings. No root or hidden reflection hacks used. | Platform signature access to toggle radios directly. |

---

## 3. Supported Triggers & Actions

### Triggers
1. **Manual Activation**: Run on demand from Home or Routines screen.
2. **Daily Schedule**: Specified hour & minute with customizable days of the week (Mon–Sun).
3. **Battery Threshold**: Triggers when battery level drops to/below target percentage (e.g. $\le 20\%$) or when charging status changes.
4. **Wi-Fi Connection**: Triggers on Wi-Fi connect or disconnect. Strict network SSID matching prevents unknown, redacted, or null SSIDs from triggering false matches.

### Actions
1. **Show Notification**: Custom notification title and body.
2. **Launch Application**: Launches any user-selected installed app from launcher intents.
3. **Do Not Disturb**: Sets Priority Only, Total Silence, Alarms Only, or Normal mode.
4. **Battery Saver Guide**: Honest, guided shortcut to battery optimization.
5. **Open System Settings**: Direct navigation to Display, Sound, Wi-Fi, or DND settings.

---

## 4. Google Pixel 8a (Akita) Compatibility Notes

- Tested against Pixel 8a hardware profile (Tensor G3, 1080x2400 display, Android 14/15).
- Respects Android Doze mode: Prompts user to exempt app from battery restrictions in Settings screen.
- Exact alarm permissions are checked before scheduling; gracefully falls back to inexact/WorkManager execution if restricted, catching `SecurityException`.
- Credential-Encrypted Storage: Direct-Boot `LOCKED_BOOT_COMPLETED` is omitted to avoid accessing encrypted Room database before the user enters their PIN/pattern.
- Offline privacy guarantee: **Zero Internet permissions (`android.permission.INTERNET` is omitted from manifest)**. No telemetry, ads, or analytics.

---

## 5. Building & Verifying on CachyOS Linux

The official Gradle Wrapper is included in the repository.

```bash
# 1. Check Gradle Wrapper version (Gradle 9.3.1, Java 11/17 target)
./gradlew --version

# 2. Clean build cache
./gradlew clean

# 3. Run full automated unit test suite (RuleEngine, Wi-Fi matching, StatusResolver, AlarmIdentity)
./gradlew :app:testDebugUnitTest

# 4. Assemble standalone debug APK
./gradlew assembleDebug
```

### Verified Build Artifact
- **APK Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Package Name**: `com.albkestrel.osautomation`
- **APK Size**: `19,262,910 bytes` (~18.4 MB)
- **SHA-256**: `10e14a434b3601073eb6f10a2d48396e1c817fe4ee35d0d3da75f86fe2015209`

---

## 6. Installation & Physical-Device Testing (Pixel 8a / Akita)

### 6.1 Installation via ADB on CachyOS

Connect your Pixel 8a to the CachyOS development machine with USB debugging enabled:

```bash
# Verify device connection
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Verify package installation
adb shell pm list packages | grep albkestrel

# Stream live automation logs
adb logcat -v time -s "AlbkestrelApp" "AutomationScheduler" "RoutineAlarmReceiver" "BootReceiver" "ActionExecutor" "AutomationWorker"
```

---

### 6.2 Physical-Device Test Checklist

| Category | Test Case | Execution Steps | Expected Result | Result |
|---|---|---|---|---|
| **Persistence** | Create Routine | Fill name, schedule condition, notification action, tap Save. | Routine appears in Routines list and database. | [ ] |
| **Persistence** | Edit Routine | Open routine, modify name and time, tap Save. | Updated parameters persist without creating duplicate entries. | [ ] |
| **Persistence** | Enable / Disable | Toggle routine switch in Routines or Home screen. | Switch state persists; alarms scheduled when enabled, cancelled when disabled. | [ ] |
| **Persistence** | Delete Routine | Tap Delete in overflow menu, confirm dialog. | Routine is removed from database and UI; all scheduled alarms cancelled. | [ ] |
| **Persistence** | Process Restart | Force-close app via Recents, reopen app. | All routines, active counts, and execution logs reload cleanly. | [ ] |
| **Alarms** | Punctual Alarm | Schedule routine for current time + 2 minutes. | Executes punctually; notification displays; log recorded in history. | [ ] |
| **Alarms** | Time Modification | Change schedule time from 10:00 to 11:00. | Alarm at 10:00 does not trigger; alarm at 11:00 triggers punctually. | [ ] |
| **Alarms** | Duplicate Check | Leave routine active across multiple cycles. | Cooldown and single-alarm slot prevent duplicate firings. | [ ] |
| **Alarms** | Exact Alarm Denial | Revoke "Alarms & Reminders" special access in Settings. | App does not crash; falls back to inexact alarm with warning logged. | [ ] |
| **Reboot** | Boot Recovery | Reboot Pixel 8a with enabled schedule routine. | `BootReceiver` runs after unlock; enabled routines restored; logs verify count. | [ ] |
| **Outcomes** | Full Success | Run routine with Notification action. | Notification posts; history records `SUCCESS`. | [ ] |
| **Outcomes** | Permission Missing | Revoke Notification Policy (DND) access, run DND routine. | DND action blocked; history records `PERMISSION_REQUIRED` (not success). | [ ] |
| **Outcomes** | Unsupported App | Configure app launch with non-existent package. | Action reports `UNSUPPORTED`; history records `UNSUPPORTED`. | [ ] |
| **Outcomes** | Mixed Outcomes | Routine with 1 Notification and 1 ungranted DND action. | Overall status is `PERMISSION_REQUIRED` (never reported as full success). | [ ] |
| **Wi-Fi** | Matching SSID | Connect to target Wi-Fi SSID (e.g., "Home_Wi-Fi"). | Wi-Fi condition evaluates to true; routine executes. | [ ] |
| **Wi-Fi** | Different SSID | Connect to different Wi-Fi network. | Condition evaluates to false; routine does not fire. | [ ] |
| **Wi-Fi** | Redacted / Unknown | Disconnect Wi-Fi or turn off location (SSID `<unknown ssid>`). | Guard prevents false trigger; does not match specific network. | [ ] |
| **Wi-Fi** | Disconnected Trigger | Turn off Wi-Fi on device. | Disconnected trigger evaluates to true; routine executes. | [ ] |
| **DND** | Without Access | Tap DND action without granting Special App Access. | Returns `PERMISSION_REQUIRED`; user guided to system settings. | [ ] |
| **DND** | With Access | Grant Notification Policy Access, trigger routine. | DND mode transitions to Priority / Alarms Only / Total Silence. | [ ] |
| **Battery** | Low Battery Trigger | Set routine for battery $\le 25\%$; unplug or set battery via ADB. | Fires when threshold reached; guides user to Battery Saver settings. | [ ] |
| **Battery** | Battery Saver Notice | Trigger Battery Saver Guide action. | Opens system battery saver screen; explains public SDK restriction honestly. | [ ] |
| **Doze** | Standby Evaluation | Place Pixel 8a in Doze mode via `adb shell dumpsys deviceidle force-idle`. | WorkManager evaluates on maintenance window; exact alarms wake device. | [ ] |
