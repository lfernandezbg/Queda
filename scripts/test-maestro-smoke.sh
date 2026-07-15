#!/bin/bash
set -euo pipefail

DEVICE_SERIAL=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --device-serial)
      DEVICE_SERIAL="$2"
      shift
      shift
      ;;
    *)
      shift
      ;;
  esac
done

echo "--- Phase 0: Maestro Smoke ---"

rm -rf .maestro/results
mkdir -p .maestro/results

if ! command -v maestro &> /dev/null; then
    echo "Maestro CLI not found."
    exit 1
fi

if ! command -v adb &> /dev/null; then
    echo "ADB not found."
    exit 1
fi

CONNECTED_DEVICES=$(adb devices | grep -E "\tdevice$" | cut -f1)

if [ -n "$DEVICE_SERIAL" ]; then
    if ! echo "$CONNECTED_DEVICES" | grep -v '^$' | grep -q "^$DEVICE_SERIAL$"; then
        echo "Device $DEVICE_SERIAL not found or not in 'device' state."
        exit 1
    fi
    SERIAL=$DEVICE_SERIAL
else
    DEVICE_COUNT=$(echo "$CONNECTED_DEVICES" | grep -v '^$' | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "No devices connected."
        exit 1
    fi
    if [ "$DEVICE_COUNT" -gt 1 ]; then
        echo "Multiple devices found. Specify --device-serial."
        exit 1
    fi
    SERIAL=$(echo "$CONNECTED_DEVICES" | xargs)
fi

echo "Using device: $SERIAL"

./gradlew :app:assembleE2E

APK_PATH="app/build/outputs/apk/e2e"
APK_FILE=$(find "$APK_PATH" -name "*.apk")
APK_COUNT=$(echo "$APK_FILE" | grep -v '^$' | wc -l)

if [ "$APK_COUNT" -ne 1 ]; then
    echo "Expected exactly one APK in $APK_PATH, found $APK_COUNT"
    exit 1
fi

adb -s "$SERIAL" install -r "$APK_FILE"

FLOWS=(
    ".maestro/flows/smoke/00_launch_app.yaml"
    ".maestro/flows/smoke/01_reset_and_launch.yaml"
    ".maestro/flows/smoke/02_seed_empty_and_launch.yaml"
)

for flow in "${FLOWS[@]}"; do
    maestro --device "$SERIAL" test "$flow"
done

maestro --device "$SERIAL" test .maestro/flows/smoke

echo "PASS"
