#!/usr/bin/env bash
# Install the minified release APK on the running emulator, open it, and fail unless it is still
# running 15 seconds later with nothing in the crash buffer.
#
# Usage: launch-release.sh <signed release APK>
set -euo pipefail

APK=$1
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
PKG=$("$BT/aapt2" dump badging "$APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
ACTIVITY=$("$BT/aapt2" dump badging "$APK" | sed -n "s/^launchable-activity: name='\([^']*\)'.*/\1/p" | head -1)
echo "Opening $PKG/$ACTIVITY"

adb install -r "$APK"
adb logcat -b all -c
# No -W: a process that dies in startup never reports "launch complete", and -W waits for it.
adb shell am start -n "$PKG/$ACTIVITY"
sleep 15

if adb logcat -d -b crash | grep -q 'FATAL EXCEPTION'; then
  adb logcat -d -b crash
  echo "::error::$PKG crashed after launch"
  exit 1
fi
if ! adb shell pidof "$PKG" > /dev/null; then
  adb logcat -d | tail -200
  echo "::error::$PKG is not running 15 s after launch"
  exit 1
fi
echo "$PKG is running 15 s after launch, with nothing in the crash buffer"
