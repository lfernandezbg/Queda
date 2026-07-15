#!/bin/bash
set -euo pipefail

DEVICE_SERIAL=""

while [[ "$#" -gt 0 ]]; do
    case "$1" in
        --device-serial)
            if [[ "$#" -lt 2 ]]; then
                echo "Missing value for --device-serial."
                exit 1
            fi

            DEVICE_SERIAL="$2"
            shift 2
            ;;

        *)
            echo "Unknown argument: $1"
            exit 1
            ;;
    esac
done

if ! command -v adb >/dev/null 2>&1; then
    echo "ADB not found."
    exit 1
fi

mapfile -t CONNECTED_SERIALS < <(
    adb devices |
    awk 'NR > 1 && $2 == "device" { print $1 }'
)

if [[ -n "$DEVICE_SERIAL" ]]; then
    FOUND=false

    for connected in "${CONNECTED_SERIALS[@]}"; do
        if [[ "$connected" == "$DEVICE_SERIAL" ]]; then
            FOUND=true
            break
        fi
    done

    if [[ "$FOUND" != true ]]; then
        echo "Device $DEVICE_SERIAL not found or not in device state."
        exit 1
    fi

    SERIAL="$DEVICE_SERIAL"
else
    if [[ "${#CONNECTED_SERIALS[@]}" -eq 0 ]]; then
        echo "No Android device is connected."
        exit 1
    fi

    if [[ "${#CONNECTED_SERIALS[@]}" -gt 1 ]]; then
        echo "Multiple Android devices found. Specify --device-serial."
        exit 1
    fi

    SERIAL="${CONNECTED_SERIALS[0]}"
fi

echo "--- Device Info ---"
echo "Serial: $SERIAL"

PROPERTIES=(
    "ro.product.manufacturer"
    "ro.product.model"
    "ro.build.version.release"
    "ro.build.version.sdk"
    "ro.build.version.security_patch"
    "ro.build.version.incremental"
)

for property in "${PROPERTIES[@]}"; do
    value="$(
        adb -s "$SERIAL" \
            shell getprop "$property" |
        tr -d '\r'
    )"

    echo "$property: $value"
done

adb -s "$SERIAL" shell wm size
adb -s "$SERIAL" shell wm density
