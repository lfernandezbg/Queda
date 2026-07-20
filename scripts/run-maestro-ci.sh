#!/usr/bin/env bash
set -euo pipefail

echo "--- Detecting Android device ---"

SERIALS="$(
    adb devices |
    awk 'NR > 1 && $2 == "device" { print $1 }'
)"

DEVICE_COUNT="$(
    printf '%s\n' "$SERIALS" |
    sed '/^$/d' |
    wc -l |
    tr -d ' '
)"

if [ "$DEVICE_COUNT" -ne 1 ]; then
    echo "Expected exactly one connected Android device, found $DEVICE_COUNT"
    adb devices -l
    exit 1
fi

SERIAL="$(
    printf '%s\n' "$SERIALS" |
    sed '/^$/d'
)"

echo "Using Android device: $SERIAL"

echo "--- Stabilizing emulator ---"
adb -s "$SERIAL" wait-for-device

MAX_RETRIES=30
RETRY_COUNT=0
while [ "$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
    if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
        echo "Timeout waiting for sys.boot_completed"
        exit 1
    fi
    echo "Waiting for sys.boot_completed... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 5
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

# Wait for essential services
adb -s "$SERIAL" shell service check activity
adb -s "$SERIAL" shell service check window
adb -s "$SERIAL" shell service check package

# Disable animations
adb -s "$SERIAL" shell settings put global window_animation_scale 0
adb -s "$SERIAL" shell settings put global transition_animation_scale 0
adb -s "$SERIAL" shell settings put global animator_duration_scale 0

# Unlock and wake
adb -s "$SERIAL" shell input keyevent 224
adb -s "$SERIAL" shell wm dismiss-keyguard

# Ensure System UI / Launcher is responsive
RETRY_COUNT=0
while ! adb -s "$SERIAL" shell uiautomator dump /sdcard/dump.xml > /dev/null 2>&1; do
    if [ "$RETRY_COUNT" -ge 10 ]; then
        echo "System UI not responding to uiautomator dump"
        break
    fi
    echo "Waiting for System UI responsiveness... ($RETRY_COUNT/10)"
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

mkdir -p .maestro/results

echo "--- Installing Maestro 2.6.0 ---"

export MAESTRO_VERSION=2.6.0
curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH:$HOME/.maestro/bin"

MAESTRO_OUTPUT="$(
    maestro --version 2>&1
)"

echo "$MAESTRO_OUTPUT"

if ! printf '%s' "$MAESTRO_OUTPUT" |
    grep -Eq '(^|[^0-9])2\.6\.0([^0-9]|$)'; then
    echo "Expected Maestro 2.6.0, found: $MAESTRO_OUTPUT"
    exit 1
fi

echo "--- Running Maestro smoke tests ---"

bash ./scripts/test-maestro-smoke.sh \
    --device-serial "$SERIAL"

if [ ! -s ".maestro/results/report.xml" ]; then
    echo "Maestro report was not generated or is empty."
    exit 1
fi

echo "Maestro smoke tests: PASS"
echo "Report: .maestro/results/report.xml"