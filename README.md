<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" height="100" alt="VitalTrack Logo"/>

# VitalTrack – Health Monitor

**Your all-in-one personal health companion for Android**

[![Android](https://img.shields.io/badge/Platform-Android-3ddc84?logo=android&logoColor=white)](https://android.com)
[![API](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-brightgreen)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-brightgreen)](https://developer.android.com/about/versions)
[![Java](https://img.shields.io/badge/Language-Java-orange?logo=openjdk&logoColor=white)](https://www.java.com)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Developer](https://img.shields.io/badge/Developer-Xero%20Trust-1a6b45)](https://github.com/xerotrust)

<br/>

> Track steps · Log vitals · Never miss a medicine · Stay hydrated

<br/>

</div>

---

## 📱 Overview

VitalTrack is a fully offline, no-account-required Android health tracking app. It brings together step counting, medicine reminders, water intake tracking, blood pressure and blood sugar logging, BMI calculation, calorie tracking, and more — all in one clean, bilingual (English / বাংলা) interface.

All data is stored **locally on-device** using SQLite and SharedPreferences. No server. No account. No data collection.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🏃 **Step Counter** | Walk & run tracking with foreground service, weekly chart, distance & calories |
| 💊 **Medicine Reminder** | Multi-medicine support, custom schedules, sound + vibration alerts |
| 💧 **Water Intake & Alarm** | Daily goal tracking with repeating hydration reminders |
| ❤️ **Blood Pressure Log** | Systolic / diastolic / pulse history with full record list |
| 🩸 **Blood Sugar Tracker** | Glucose log with normal range indicator |
| ⚖️ **BMI Calculator** | cm or feet/inches input, instant BMI with category result |
| 🔥 **Calorie Calculator** | Daily calorie needs based on TDEE (age, weight, height, activity level) |
| 🏋️ **Calories Burned** | Track calories burned per exercise session |
| 🎯 **Ideal Weight** | Devine Formula-based ideal weight range for your height & gender |
| 💡 **Daily Health Tips** | 500+ curated tips across hydration, sleep, nutrition, mental health & more |
| 👤 **Personal Profile** | Name, age, height, weight, blood group — powers all calculations |
| 🌐 **Bilingual UI** | Full English and বাংলা (Bangla) support, switchable at runtime |

---

## 🏗️ Architecture

```
com.xerotrust.vitaltrack
│
├── activities/          # Feature screens (BMI, BP, Blood Sugar, Water, Calories…)
├── fragments/           # Main nav fragments (Home, StepCounter, MedicineReminder, Profile)
├── services/            # Foreground services (StepCounter, MedicineAlarm, WaterAlarm)
├── receivers/           # BroadcastReceivers (AlarmReceiver, WaterAlarmReceiver, BootReceiver)
├── adapters/            # RecyclerView adapters
├── models/              # Data models (BloodPressureRecord, MedicinePlan, WaterAlarm…)
├── dialogs/             # DialogFragments
├── ui/                  # Wizard UI (MedicinePlanWizard)
├── views/               # Custom views (WeeklyStepsChartView)
└── utils/               # Helpers (DatabaseHelper, AppPrefs, LanguageHelper, MedicineScheduleUtils…)
```

**Data flow:**
- Health records → `DatabaseHelper` (SQLite)
- User profile & preferences → `AppPrefs` (SharedPreferences)
- Alarms → `AlarmManager` (exact alarms) → `AlarmReceiver` / `WaterAlarmReceiver`
- Step counting → `StepCounterService` (foreground, `TYPE_STEP_COUNTER` sensor)
- Boot restore → `BootReceiver` reschedules all active alarms on device restart

---

## 🛠️ Tech Stack

| Layer | Library / API |
|---|---|
| Language | Java 11 |
| UI | Material Design 3, ConstraintLayout |
| Charts | [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) v3.1.0 |
| HTTP Client | Retrofit 3.0.0 + Gson 2.14.0 |
| Database | SQLite via `SQLiteOpenHelper` |
| Build | Gradle 9.2.1 with Version Catalog (`libs.versions.toml`) |
| Min SDK | 24 (Android 7.0 Nougat) |
| Target SDK | 36 |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK with API 36 installed

### Clone & Build

```bash
git clone https://github.com/xerotrust/vitaltrack.git
cd vitaltrack
```

Open in Android Studio → `File → Open` → select the `vitaltrack` folder.

Let Gradle sync complete, then run on a device or emulator (API 24+).

### Build from CLI

```bash
./gradlew assembleDebug
```

Release APK:

```bash
./gradlew assembleRelease
```

---

## 📋 Permissions

| Permission | Why it's needed |
|---|---|
| `ACTIVITY_RECOGNITION` | Read step counter sensor for walk/run tracking |
| `POST_NOTIFICATIONS` | Send medicine and water reminder notifications |
| `SCHEDULE_EXACT_ALARM` | Fire reminders at the exact time the user sets |
| `FOREGROUND_SERVICE_HEALTH` | Keep step tracking active while screen is off |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Play alarm sound for medicine/water reminders |
| `RECEIVE_BOOT_COMPLETED` | Restore all alarms after device reboot |
| `VIBRATE` | Vibrate on reminder notifications |
| `WAKE_LOCK` | Ensure on-time alarm delivery |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Reserved for future use — no data is sent currently |

---

## 📂 Project Structure

```
vitaltrack/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/xerotrust/vitaltrack/
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── drawable/
│   │       │   ├── values/          # English strings
│   │       │   └── values-bn/       # Bangla strings
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/
├── build.gradle
└── settings.gradle
```

---

## 🌐 Localization

The app ships with full string translations for:

- 🇬🇧 **English** (`res/values/strings.xml`)
- 🇧🇩 **বাংলা / Bangla** (`res/values-bn/strings.xml`)

Language can be switched at runtime via `LanguageHelper.applyLanguage()` without restarting the app.

To add a new language, create `res/values-{locale}/strings.xml` and translate all keys from `values/strings.xml`.

---

## 🔒 Privacy

VitalTrack is **100% offline**. No data ever leaves your device.

- No analytics SDK
- No crash reporting SDK  
- No advertising SDK
- No account or login required
- All health data stored in local SQLite database

Full privacy policy: [Privacy Policy](https://xerotrust.github.io/vitaltrack/privacy-policy)

---

## 📄 License

```
MIT License

Copyright (c) 2025 Xero Trust

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add some feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

<div align="center">

Made with ❤️ by **[Md Shihab Mia](https://github.com/devshihab945/)**

`com.xerotrust.vitaltrack`

</div>
