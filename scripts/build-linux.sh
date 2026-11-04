#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_HOME
if ! command -v javac >/dev/null; then echo 'A full JDK 17 is required (not only a Java runtime).' >&2; exit 1; fi
if [ ! -f "$ANDROID_HOME/platforms/android-35/android.jar" ]; then echo 'Install Android SDK platform 35 and build-tools 35.0.0; see TERMUX_BUILD.md.' >&2; exit 1; fi
chmod +x gradlew
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
printf '\nAPK: app/build/outputs/apk/debug/app-debug.apk\n'
