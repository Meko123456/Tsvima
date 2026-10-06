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

# Only this app's crashes count, its own ":name" processes included. The emulator image's own apps
# sometimes crash by themselves while it settles (Gmail did, under PrepParrot's check on 6 October
# 2026), and failing on those blames the build for something it had nothing to do with.
CRASHES=$(adb logcat -d -b crash)
PKG_RE=${PKG//./\\.}
if echo "$CRASHES" | grep -A1 'FATAL EXCEPTION' | grep -qE "Process: $PKG_RE(:[^,]*)?,"; then
  echo "$CRASHES"
  echo "::error::$PKG crashed after launch"
  exit 1
fi
if echo "$CRASHES" | grep -q 'FATAL EXCEPTION'; then
  echo "Crashes in other processes during the check, not counted against $PKG:"
  echo "$CRASHES" | grep -A1 'FATAL EXCEPTION' | grep 'Process: '
fi
if ! adb shell pidof "$PKG" > /dev/null; then
  adb logcat -d | tail -200
  echo "::error::$PKG is not running 15 s after launch"
  exit 1
fi
echo "$PKG is running 15 s after launch, with no crash of its own"
