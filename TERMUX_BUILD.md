# YESMIN AI — Termux se source manage, cloud mein build, phone par install

## 1. Verified toolchain decision

The selected Android toolchain is AGP 8.9.2 + Gradle 8.11.1 + full JDK 17 + Android SDK 35/build-tools 35.0.0. Google's AGP 8.9 compatibility page lists these requirements:
https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes

The official standard Linux build-tools 35.0.0 `aapt2` downloaded for verification was identified by `file` as:

```
ELF 64-bit ... x86-64 ... interpreter /lib64/ld-linux-x86-64.so.2
```

Most Android phones run ARM64 + Android Bionic, not this x86-64 glibc host. Installing Java in Termux alone does NOT make that executable work. Community AAPT2 substitutions, patched SDKs, emulation/proot and unofficial repositories are device/version-specific. This project does not present them as a verified native-Termux build.

**Supported phone-only workflow:** Termux controls source, GitHub Actions compiles on supported x86-64 Linux with JDK 17, Termux downloads the resulting APK, Android installs it after your approval. A GitHub account, internet and available Actions quota are required. Check GitHub's current account limits; do not assume unlimited builds.

No Java or Android SDK installation is required on the phone for this workflow. A real ARM Termux device was not available in the build environment; see BUILD_VERIFICATION.md for precisely what was tested.

## 2. Install tools in a supported Termux installation

Use the official Termux project's maintained distribution and package repositories. Do not mix APKs/plugins signed by different distributors.

Run in Termux:

```bash
pkg update
pkg upgrade
pkg install git gh unzip
termux-setup-storage
```

Approve Android's storage prompt. The actual `gh` package exists in the Termux repository:
https://github.com/termux/termux-packages/blob/master/packages/gh/build.sh

If `pkg` cannot find it, fix your Termux repository/mirror with `termux-change-repo`, then `pkg update`; do not install a random script claiming to be an Android toolchain.

## 3. Extract source into Termux HOME

Download `YESMIN-AI-source.zip` from the supplied download link into your phone's Download folder using your browser. Avoid trying to `curl` the authenticated attachment link without your signed-in browser session.

```bash
mkdir -p "$HOME/projects"
cd "$HOME/projects"
unzip "$HOME/storage/downloads/YESMIN-AI-source.zip"
cd YESMIN-AI
chmod +x gradlew scripts/*.sh
```

If Android saved a different filename, adjust that filename only. Keep the working project under `$HOME/projects`; Android shared storage may disallow executable bits/symlinks required by build tools.

## 4. Configure Firebase (needed for real Gemini)

Follow FIREBASE_SETUP.md. Register `com.yesmin.ai`, select the Gemini Developer API backend in Firebase AI Logic, and download `google-services.json`. Move your own file to:

```bash
cp "$HOME/storage/downloads/google-services.json" app/google-services.json
```

It is ignored by git intentionally. The app builds without it for local-tool tests; it cannot invent a Firebase project or successfully call Gemini without yours.

## 5. Create your private GitHub repository

```bash
gh auth login --hostname github.com --git-protocol https --web
gh auth setup-git
git init -b main
git config user.name "Your Name"
git config user.email "YOUR_GITHUB_NOREPLY_EMAIL"
git add .
git status
```

Review `git status`: do NOT commit tokens, App Check debug secrets, signing keys or `app/google-services.json`. Replace the two identity strings above with your own values; your GitHub noreply email is visible in your GitHub email settings.

```bash
git commit -m "Initial YESMIN AI Android app"
gh repo create YESMIN-AI --private --source=. --remote=origin --push
```

If you already have a repository named YESMIN-AI, choose a new repository name or connect the existing one instead. Never overwrite an unrelated repository.

Store the Firebase client configuration as a repository Actions secret, using stdin rather than pasting its contents into a command:

```bash
base64 -w0 app/google-services.json | gh secret set FIREBASE_CONFIG_BASE64
```

This command uses the repo associated with the current directory. The Firebase client API key inside this file is not a Gemini secret; App Check and backend configuration are still mandatory. There should be no Gemini secret API key anywhere in this project.

The initial push may already start a config-free build. After setting the secret, explicitly trigger a new run using the next step.

## 6. Build and download from Termux

```bash
bash scripts/termux-cloud-build.sh
```

This dispatches `android.yml`, waits for a matching manual run, watches it, and downloads the debug artifact under:

```
apk-download/<RUN_ID>/app-debug.apk
```

The script prints the exact installation command. You can also do it manually:

```bash
gh workflow run android.yml --ref main
gh run list --workflow android.yml --event workflow_dispatch --limit 5
```

Read the displayed run ID for your latest requested build, then replace `RUN_ID` below with its numeric value (do not type literal angle brackets):

```bash
gh run watch RUN_ID --exit-status
mkdir -p apk-manual
gh run download RUN_ID --name YESMIN-AI-debug --dir apk-manual
termux-open apk-manual/app-debug.apk
```

If `apk-manual` already contains a previous file, use a fresh destination name to avoid ambiguity.

The workflow runs unit tests, Android lint, the debug APK build and release-variant compilation. Source build location on its runner:

```
app/build/outputs/apk/debug/app-debug.apk
```

## 7. Install with Android's package installer

Use the `termux-open ...apk` command printed by the script, or copy the APK to Download and tap it in your file manager. Android may require you to allow installation from Termux/your file manager. Read and approve the installer prompt yourself. No root, `pm install` privilege bypass or silent installation is required or implemented.

For the downloaded automated APK:

```bash
# Replace RUN_ID with the actual directory printed by the build script.
termux-open apk-download/RUN_ID/app-debug.apk
```

Updates must be signed with the same key as the installed app. Each fresh CI runner can generate a different debug key, so CI debug APKs may not update in place across runs. If Android reports incompatible signature, export any information you need, uninstall the old test app and install the new one (uninstall deletes its local data). For stable personal updates/production, configure a private persistent signing key; never publish it in source. The generated release APK is unsigned until you configure signing.

## 8. Register the App Check debug token

The debug provider uses a device-installation debug secret. It must be registered in your Firebase console. The SDK emits it to local Android logs. Treat it as a secret; do not share it in public issues, commits, screenshots or chat.

The simplest route is Android Studio Logcat on a trusted development computer. For a compatible Android 11+ phone, Termux can use Android's explicit Wireless debugging pairing instead:

```bash
pkg install android-tools
```

Enable Developer options and Wireless debugging, manually pair with the address/port shown by Android (pairing and connection ports are often different):

```bash
adb pair PAIRING_IP:PAIRING_PORT
adb connect DEBUGGING_IP:DEBUGGING_PORT
adb logcat -s DebugAppCheckProvider
```

Do not type those placeholders literally. Enter only your own device's addresses and the one-time pairing code displayed by Android. Open YESMIN and use the Gemini connection test while observing logs. Register the token privately under Firebase App Check → your Android app → Manage debug tokens. Stop logcat with Ctrl+C. Revoke the token when you stop testing. Wireless debugging requires compatible Android/OEM/Wi-Fi support; do not claim it works on every phone. Disable it after use. Never request root to bypass log access.

## 9. Subsequent code changes

```bash
git add .
git commit -m "Describe your changes"
git push
bash scripts/termux-cloud-build.sh
```

The push also triggers a build; manual dispatch is useful when you want the script to watch/download exactly a workflow_dispatch run. You can skip the extra dispatch and manually download the push run if desired.

## 10. Alternative: supported desktop/Linux build

Install a FULL JDK 17, official Android command-line tools and SDK packages on an appropriate supported host. The commands below run on Linux x86-64, NOT stock ARM Termux. After placing the extracted Android cmdline-tools under `$HOME/Android/Sdk/cmdline-tools/latest`:

```bash
export JAVA_HOME=/path/to/your/full/jdk-17
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
javac -version
sdkmanager --licenses
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
bash scripts/build-linux.sh
```

`/path/to/your/full/jdk-17` must be your actual JDK installation path. The project's Gradle wrapper downloads Gradle 8.11.1. Android Studio can also import the root project. Build commands:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:assembleRelease
```

## Troubleshooting

| Symptom | Check / fix |
|---|---|
| `Exec format error` / AAPT2 won't start in Termux | You are trying to run standard x86 Linux tools on ARM Android. Use the included cloud build, not a fabricated SDK package. |
| `JAVA_COMPILER` missing | Install full JDK 17, not just JRE; check `javac -version` and JAVA_HOME. |
| SDK location not found | On Linux set ANDROID_HOME or `local.properties` with `sdk.dir=/actual/sdk/path`; do not commit that file. |
| GitHub workflow not found | Push `.github/workflows/android.yml` to the repository default branch; enable Actions if your repo blocks it. |
| Actions unavailable/quota exhausted | Check account limits/permissions; use the documented Linux/Android Studio build instead. |
| No artifact | Read `gh run view RUN_ID --log-failed`; no APK is uploaded if tests/build fail. |
| Out of memory | Use one worker and `-Dorg.gradle.jvmargs=-Xmx1024m`; this does not fix architecture incompatibility. |
| Google Services "No matching client" | JSON must contain package `com.yesmin.ai`; download the correct Android-app Firebase config. |
| Gemini config missing | Add your JSON to app/ or the Actions secret and rebuild; restart alone cannot embed it. |
| App Check 401/403 | Register your current debug token and verify enforcement/project config. Production uses Play Integrity; arbitrary sideloads may not attest. |
| Model/quota error | Test another currently supported model ID; check Firebase AI Logic backend, region, API/quota/billing. |
| Mic not working | Grant microphone, enable a compatible recognition service and use selected language; text input remains available. |
| TTS language/voice missing | Install the language in device TTS settings; gender/quality depends on engine. |
| Can't enable service | Read app disclosure, Android restricted-settings warning and enable it yourself only if you trust it. Do not bypass protections. |
| Reminder delayed/missing | Check notification/channel permission, force-stop state, battery restrictions. Inexact alarms are not guaranteed punctual. |
| App unavailable | It must have a visible launcher or a supported intent handler; choose another app in App Launcher. |
