#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="8.9"
GRADLE_HOME_BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/vpnsopr-bootstrap"
GRADLE_HOME_LOCAL="$GRADLE_HOME_BASE/gradle-$GRADLE_VERSION"

if command -v gradle >/dev/null 2>&1; then
  GRADLE_BIN="$(command -v gradle)"
elif [ -x "$GRADLE_HOME_LOCAL/bin/gradle" ]; then
  GRADLE_BIN="$GRADLE_HOME_LOCAL/bin/gradle"
else
  mkdir -p "$GRADLE_HOME_BASE"
  ZIP="$GRADLE_HOME_BASE/gradle-$GRADLE_VERSION-bin.zip"
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  unzip -q "$ZIP" -d "$GRADLE_HOME_BASE"
  GRADLE_BIN="$GRADLE_HOME_LOCAL/bin/gradle"
fi

if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  echo "Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK folder first." >&2
  exit 2
fi

"$GRADLE_BIN" --no-daemon :app:assembleDebug
printf '\nAPK: app/build/outputs/apk/debug/app-debug.apk\n'
