#!/usr/bin/env bash
set -euo pipefail

# --- Arguments ---
APK_FILE=""
SHARD_INDEX=0
SHARD_COUNT=1
OUTPUT_DIR="maestro-results"

while [[ $# -gt 0 ]]; do
  case $1 in
    --apk) APK_FILE="$2"; shift 2 ;;
    --shard-index) SHARD_INDEX="$2"; shift 2 ;;
    --shard-count) SHARD_COUNT="$2"; shift 2 ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

# --- Validation ---
if [[ ! -f "$APK_FILE" || ! -r "$APK_FILE" ]]; then
    echo "Error: APK file '$APK_FILE' not found or not readable."
    exit 1
fi

if ! [[ "$SHARD_COUNT" =~ ^[0-9]+$ ]] || [ "$SHARD_COUNT" -le 0 ]; then
    echo "Error: Shard count must be > 0."
    exit 1
fi

if ! [[ "$SHARD_INDEX" =~ ^[0-9]+$ ]] || [ "$SHARD_INDEX" -ge "$SHARD_COUNT" ]; then
    echo "Error: Shard index out of range."
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
if [[ ! -w "$OUTPUT_DIR" ]]; then
    echo "Error: Output directory '$OUTPUT_DIR' is not writable."
    exit 1
fi

echo "--- Detecting Android device ---"
mapfile -t SERIALS < <(
    adb devices |
        awk 'NR > 1 && $2 == "device" { print $1 }'
)

DEVICE_COUNT=${#SERIALS[@]}

if [ "$DEVICE_COUNT" -ne 1 ]; then
    echo "Expected exactly one connected Android device, found $DEVICE_COUNT"
    adb devices -l
    exit 1
fi

SERIAL="${SERIALS[0]}"
echo "Using Android device: $SERIAL"

# --- Setup Cleanup ---
LOGCAT_PID=""
cleanup() {
    echo "--- Finalizing Shard $SHARD_INDEX ---"
    if [[ -n "$LOGCAT_PID" ]]; then
        kill "$LOGCAT_PID" 2>/dev/null || true
    fi

    echo "Capturing final diagnostics..."
    timeout 15s adb -s "$SERIAL" shell dumpsys activity > "$OUTPUT_DIR/dumpsys_activity.txt" 2>&1 || true
    timeout 15s adb -s "$SERIAL" shell dumpsys window > "$OUTPUT_DIR/dumpsys_window.txt" 2>&1 || true
    timeout 15s adb -s "$SERIAL" shell dumpsys package com.luisete.queda.e2e > "$OUTPUT_DIR/dumpsys_package.txt" 2>&1 || true
    timeout 15s adb -s "$SERIAL" shell uiautomator dump "/sdcard/final_dump.xml" > /dev/null 2>&1 || true
    timeout 15s adb -s "$SERIAL" pull "/sdcard/final_dump.xml" "$OUTPUT_DIR/final_ui_dump.xml" 2>/dev/null || true
}
trap cleanup EXIT

# --- Start Logcat ---
adb -s "$SERIAL" logcat -v time > "$OUTPUT_DIR/logcat.txt" 2>&1 &
LOGCAT_PID=$!

# --- Health Check Function ---
check_health() {
    local phase=$1
    echo "--- Health Check: $phase ---"

    local MAX_RETRIES=15
    local ATTEMPT=1

    while [ $ATTEMPT -le $MAX_RETRIES ]; do
        local HEALTHY=true

        if [[ $(timeout 5s adb -s "$SERIAL" get-state 2>&1) != "device" ]]; then
            echo "ADB state not 'device'."
            HEALTHY=false
        fi

        if [[ $(timeout 5s adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r') != "1" ]]; then
            echo "sys.boot_completed != 1"
            HEALTHY=false
        fi

        for service in activity window package; do
            if ! timeout 5s adb -s "$SERIAL" shell service check "$service" | grep -q "found"; then
                echo "Service $service not found."
                HEALTHY=false
            fi
        done

        if ! timeout 10s adb -s "$SERIAL" shell cmd package list packages > /dev/null; then
            echo "Package Manager not responding."
            HEALTHY=false
        fi

        if $HEALTHY; then
            local DUMP_FILE="/sdcard/health_$phase.xml"
            if timeout 20s adb -s "$SERIAL" shell uiautomator dump "$DUMP_FILE" > /dev/null 2>&1; then
                local LOCAL_DUMP="$OUTPUT_DIR/health_$phase.xml"
                adb -s "$SERIAL" pull "$DUMP_FILE" "$LOCAL_DUMP" > /dev/null 2>&1

                if [[ -s "$LOCAL_DUMP" ]]; then
                    # Check for ANR/Crash
                    if grep -Ei "isn't responding|isn&apos;t responding|has stopped|keeps stopping" "$LOCAL_DUMP"; then
                        echo "Error: System dialog detected (ANR/Crash). Aborting."
                        exit 1
                    fi
                else
                    echo "UI dump empty."
                    HEALTHY=false
                fi
            else
                echo "uiautomator dump failed."
                HEALTHY=false
            fi
        fi

        if $HEALTHY; then
            echo "Device is healthy."
            return 0
        fi

        echo "Attempt $ATTEMPT/$MAX_RETRIES: Device unhealthy. Sleeping..."
        sleep 5
        ATTEMPT=$((ATTEMPT + 1))
    done

    echo "Error: Device health check failed after $MAX_RETRIES attempts."
    exit 1
}

check_health "initial"

# --- Installation ---
echo "--- Installing APK ---"
INSTALL_ATTEMPT=1
while [ $INSTALL_ATTEMPT -le 3 ]; do
    echo "Attempt $INSTALL_ATTEMPT to install $APK_FILE"

    set +e
    INSTALL_OUT=$(adb -s "$SERIAL" install --no-streaming -r "$APK_FILE" 2>&1)
    INSTALL_CODE=$?
    set -e

    echo "$INSTALL_OUT"

    if [ $INSTALL_CODE -eq 0 ] && echo "$INSTALL_OUT" | grep -q "Success"; then
        echo "Installation successful."
        break
    fi

    # Only retry on infrastructure errors
    if echo "$INSTALL_OUT" | grep -Ei "Broken pipe|device offline|closed|cannot connect|Failure calling service package|service package .* not found"; then
        echo "Infrastructure error detected. Resetting ADB..."
        adb kill-server
        adb start-server
        timeout 30s adb -s "$SERIAL" wait-for-device
        check_health "retry_$INSTALL_ATTEMPT"
    else
        echo "Deterministic installation error or timeout. Aborting."
        exit 1
    fi

    if [ $INSTALL_ATTEMPT -eq 3 ]; then
        echo "Failed to install APK after 3 attempts."
        exit 1
    fi
    INSTALL_ATTEMPT=$((INSTALL_ATTEMPT + 1))
done

# Verify with pm path
PM_PATH=$(timeout 10s adb -s "$SERIAL" shell pm path com.luisete.queda.e2e | tr -d '\r')
if [[ "$PM_PATH" != package:* ]]; then
    echo "Error: Package com.luisete.queda.e2e not verified. pm path: '$PM_PATH'"
    exit 1
fi

check_health "before_maestro"

# --- Maestro Execution ---
EXIT_CODE=0
bash ./scripts/test-maestro-smoke.sh \
    --device-serial "$SERIAL" \
    --skip-build \
    --skip-install \
    --shard-index "$SHARD_INDEX" \
    --shard-count "$SHARD_COUNT" \
    --apk "$APK_FILE" \
    --output-dir "$OUTPUT_DIR" || EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo "Maestro shard $SHARD_INDEX: PASS"
else
    echo "Maestro shard $SHARD_INDEX: FAIL (Exit code: $EXIT_CODE)"
fi

exit $EXIT_CODE
