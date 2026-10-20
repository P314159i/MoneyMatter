# Pocket Ledger — Android source project
## Disclaimer: This app is coded entirely by ChatGPT 6.1 Sol

Offline Android expense, income and savings tracker. This archive contains source code, not a compiled APK. Minimum Android 8.0 (API 26).

## Features
- Income grouped by named sources.
- Opening and current spending/savings balances.
- Expenses paid from spending or directly from savings.
- Transfers between accounts never counted as income or expenses.
- Monthly summaries and transaction deletion.
- Private on-device storage with Android document-picker backup export/import.
- No internet permission, bank connection, analytics, or hosted website.

The application uses an Android Java Activity and bundled HTML/JavaScript UI in a WebView. Financial data persists in the application's SharedPreferences. Uninstalling deletes local data. Export backups first. Currency changes format existing amounts without converting them.

## Build an installable APK on a computer

Install JDK 17, Gradle 8.9, and Android SDK platform 35/build tools 35.0.0. Set ANDROID_HOME to your Android SDK directory (or set sdk.dir in a local.properties file).

From this project's directory:

    gradle assembleDebug

The signed debug installer is created at:

    app/build/outputs/apk/debug/app-debug.apk

Copy that APK to your Android phone, open it, and allow installation from your chosen Files/browser application when Android asks. A debug APK is intended for personal testing. For distribution, create your own release signing key and signed release build in Android Studio. Keep the signing key to update the app without uninstalling.

For Android Studio: install/select Gradle 8.9 locally, or first run `gradle wrapper --gradle-version 8.9` using installed Gradle. Then open this project, sync, and build the debug APK.

## Alternative: GitHub Actions

Upload this project to a repository you control. In Actions, run **Build Android APK**. Download the **Pocket-Ledger-APK** artifact and unzip it to obtain app-debug.apk. This workflow is supplied but has not been run here. It uses third-party GitHub Actions and downloads build tools/dependencies during compilation. The app itself works offline.

## Verification status

JavaScript syntax and finance calculations were checked. XML and packaged file checks were completed. Android compilation and device/emulator testing were not performed: the creation environment lacks a working JDK/Android SDK/Gradle installation. A successful APK build is not claimed.

## Accounting rules

Income adds to spending. Spending expenses reduce spending. Transfers to savings reduce spending and increase savings. Savings withdrawals increase spending and reduce savings without counting as income. Direct savings expenses reduce savings and count as expenses. Monthly income minus expenses includes both expense types and excludes transfers. Account balances include every recorded transaction. Future-dated actual transactions are rejected.

## Build reference

Android Gradle plugin 8.7 requires Gradle 8.9 and JDK 17:
https://developer.android.com/build/releases/agp-8-7-0-release-notes
