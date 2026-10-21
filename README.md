## Pocket Ledger — Money Tree

Offline Android app with an interactive money tree, allowance tracking, savings, spending, editable activities, monthly totals, notes and backups.

**The ready-made APK is available in this GitHub repository.** Download it to your phone and open it to install. To build it yourself, follow the Linux instructions below.

**Version 2.0 clears the previous ledger on its first launch.**

## Build the APK on Linux

These commands target **Linux Mint / Ubuntu, 64-bit Intel or AMD**.

### 1. Check installed tools

```bash
java -version
gradle --version
"$HOME/tools/gradle-8.9/bin/gradle" --version
ls -d "$HOME/Android/Sdk" /usr/lib/android-sdk 2>/dev/null
```

The project uses **Java 17 and Gradle 8.9**. Skip installations you already have. [developer.android.com](https://developer.android.com/build/releases/agp-8-7-0-release-notes?hl=en\&utm_source=chatgpt.com)

### 2. Install missing requirements

Java 17 builds the app; Java 21 runs the current Android SDK manager.

```bash
sudo apt update
sudo apt install openjdk-17-jdk openjdk-21-jdk curl unzip

mkdir -p "$HOME/tools"
curl -fL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o /tmp/gradle.zip
unzip -qo /tmp/gradle.zip -d "$HOME/tools"
```

### 3. Install Android command-line tools if missing

```bash
mkdir -p "$HOME/Android/Sdk/cmdline-tools"
curl -fL https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip -o /tmp/android-tools.zip
unzip -qo /tmp/android-tools.zip -d "$HOME/Android/Sdk/cmdline-tools"
```

The download comes from Google’s Android tools distribution. [Android Developers](https://developer.android.com/studio/index.html/?utm_source=chatgpt.com)

### 4. Install SDK components

For an existing SDK, substitute its path below.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
"$ANDROID_HOME/cmdline-tools/cmdline-tools/bin/sdkmanager" \
--sdk_root="$ANDROID_HOME" "platforms;android-35" "build-tools;34.0.0"
```

Read and accept the licence prompts.

### 5. Build from the downloaded source folder

Open a terminal in the folder containing `settings.gradle`, then run:

```bash
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
"$HOME/tools/gradle-8.9/bin/gradle" assembleDebug
```

### 6. Install

After **BUILD SUCCESSFUL**, copy this file to your phone and open it:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For updates, use the same signing key as the installed app.
