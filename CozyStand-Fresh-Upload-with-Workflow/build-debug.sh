#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1 && [[ ! -x ./gradlew ]]; then
  echo "Gradle is not installed. Install Gradle 8.11.1 or open this project in Android Studio first." >&2
  exit 1
fi
if [[ ! -x ./gradlew ]]; then
  gradle wrapper --gradle-version 8.11.1
fi
./gradlew assembleDebug
printf '\nAPK: %s\n' "$PWD/app/build/outputs/apk/debug/app-debug.apk"
