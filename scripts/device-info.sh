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

echo "--- Device Info: $SERIAL ---"

PROPS=(
    "ro.product.manufacturer"
    "ro.product.model"
    "ro.build.version.release"
    "ro.build.version.sdk"
    "ro.build.version.security_patch"
    "ro.build.version.incremental"
)

for prop in "${PROPS[@]}"; do
    val=$(adb -s "$SERIAL" shell getprop "$prop")
    echo "$prop: $val"
done

adb -s "$SERIAL" shell wm size
adb -s "$SERIAL" shell wm density
