#!/bin/bash
set -euo pipefail

DEVICE_SERIAL=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --device-serial)
      if [ "$#" -lt 2 ]; then
          echo "Missing value for --device-serial"
          exit 1
      fi
      DEVICE_SERIAL="$2"
      shift
      shift
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

echo "--- Iteration 1: Maestro Smoke ---"

rm -rf .maestro/results
mkdir -p .maestro/results

if ! command -v maestro &> /dev/null; then
    echo "Maestro CLI not found."
    exit 1
fi

MAESTRO_OUTPUT="$(maestro --version)"
echo "$MAESTRO_OUTPUT"

if ! printf '%s' "$MAESTRO_OUTPUT" | grep -Eq '(^|[^0-9])2\.6\.0([^0-9]|$)'; then
    echo "Expected Maestro 2.6.0, found: $MAESTRO_OUTPUT"
    exit 1
fi

if ! command -v adb &> /dev/null; then
    echo "ADB not found."
    exit 1
fi

CONNECTED_DEVICES="$(
    adb devices |
    awk 'NR > 1 && $2 == "device" { print $1 }'
)"

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
APK_FILES=$(find "$APK_PATH" -name "*.apk")
APK_COUNT=$(echo "$APK_FILES" | grep -v '^$' | wc -l)

if [ "$APK_COUNT" -ne 1 ]; then
    echo "Expected exactly one APK in $APK_PATH, found $APK_COUNT"
    exit 1
fi
APK_FILE=$(echo "$APK_FILES" | xargs)

adb -s "$SERIAL" install -r "$APK_FILE"

FLOWS=(
    ".maestro/flows/smoke/00_launch_app.yaml"
    ".maestro/flows/smoke/01_reset_and_launch.yaml"
    ".maestro/flows/smoke/02_seed_empty_and_launch.yaml"
    ".maestro/flows/smoke/03_inventory_empty_state.yaml"
    ".maestro/flows/smoke/04_add_exact_unit_item.yaml"
    ".maestro/flows/smoke/05_add_exact_mass_item.yaml"
    ".maestro/flows/smoke/06_add_exact_volume_comma_item.yaml"
    ".maestro/flows/smoke/07_add_exact_item_validation.yaml"
    ".maestro/flows/smoke/08_duplicate_normalized_name.yaml"
    ".maestro/flows/smoke/09_cancel_add_item.yaml"
    ".maestro/flows/smoke/10_multiple_items_visible.yaml"
    ".maestro/flows/smoke/11_item_persists_after_relaunch.yaml"
)

for flow in "${FLOWS[@]}"; do
    maestro --device="$SERIAL" test "$flow"
done

maestro --device="$SERIAL" test .maestro/flows/smoke --format junit --output .maestro/results/report.xml

if [ ! -s ".maestro/results/report.xml" ]; then
    echo "Maestro report.xml not found or empty."
    exit 1
fi

echo "PASS"
