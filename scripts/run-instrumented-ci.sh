#!/usr/bin/env bash
set -euo pipefail

readonly MAX_RETRIES=60
readonly RETRY_INTERVAL_SECONDS=5
readonly DIAGNOSTICS_DIR="build/ci-diagnostics"

mkdir -p "$DIAGNOSTICS_DIR"

collect_diagnostics() {
    echo "--- Collecting Android diagnostics ---"

    adb devices -l \
        > "$DIAGNOSTICS_DIR/adb-devices.txt" 2>&1 || true

    if [ -n "${SERIAL:-}" ]; then
        adb -s "$SERIAL" shell getprop \
            > "$DIAGNOSTICS_DIR/getprop.txt" 2>&1 || true

        adb -s "$SERIAL" shell service list \
            > "$DIAGNOSTICS_DIR/service-list.txt" 2>&1 || true

        adb -s "$SERIAL" shell dumpsys activity \
            > "$DIAGNOSTICS_DIR/dumpsys-activity.txt" 2>&1 || true

        adb -s "$SERIAL" shell dumpsys window \
            > "$DIAGNOSTICS_DIR/dumpsys-window.txt" 2>&1 || true

        adb -s "$SERIAL" shell dumpsys package \
            > "$DIAGNOSTICS_DIR/dumpsys-package.txt" 2>&1 || true

        adb -s "$SERIAL" logcat -d \
            > "$DIAGNOSTICS_DIR/logcat.txt" 2>&1 || true
    fi
}

fail_with_diagnostics() {
    echo "$1"
    collect_diagnostics
    exit 1
}

detect_device() {
    local serials
    local device_count

    serials="$(
        adb devices |
        awk 'NR > 1 && $2 == "device" { print $1 }'
    )"

    device_count="$(
        printf '%s\n' "$serials" |
        sed '/^$/d' |
        wc -l |
        tr -d ' '
    )"

    if [ "$device_count" -ne 1 ]; then
        echo "Expected exactly one connected Android device, found $device_count"
        adb devices -l
        exit 1
    fi

    printf '%s\n' "$serials" |
        sed '/^$/d'
}

wait_for_boot_completion() {
    local attempt=1
    local boot_completed

    while [ "$attempt" -le "$MAX_RETRIES" ]; do
        boot_completed="$(
            adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null |
            tr -d '\r'
        )"

        if [ "$boot_completed" = "1" ]; then
            echo "Android boot completed."
            return 0
        fi

        echo "Waiting for Android boot: attempt $attempt/$MAX_RETRIES"
        sleep "$RETRY_INTERVAL_SECONDS"
        attempt=$((attempt + 1))
    done

    fail_with_diagnostics \
        "Android did not complete boot within the allowed time."
}

service_is_available() {
    local service_name="$1"

    adb -s "$SERIAL" shell service check "$service_name" 2>/dev/null |
        tr -d '\r' |
        grep -q "found"
}

package_manager_is_available() {
    service_is_available package &&
        adb -s "$SERIAL" shell cmd package list packages \
            > /dev/null 2>&1
}

wait_for_android_services() {
    local attempt=1

    while [ "$attempt" -le "$MAX_RETRIES" ]; do
        if service_is_available activity &&
            service_is_available window &&
            package_manager_is_available; then
            echo "Required Android services are available."
            return 0
        fi

        echo "Waiting for Android services: attempt $attempt/$MAX_RETRIES"

        adb -s "$SERIAL" shell service check activity || true
        adb -s "$SERIAL" shell service check window || true
        adb -s "$SERIAL" shell service check package || true

        sleep "$RETRY_INTERVAL_SECONDS"
        attempt=$((attempt + 1))
    done

    fail_with_diagnostics \
        "Required Android services did not become available."
}

stabilize_device() {
    echo "--- Stabilizing Android emulator ---"

    adb -s "$SERIAL" wait-for-device

    wait_for_boot_completion
    wait_for_android_services

    adb -s "$SERIAL" shell settings put global window_animation_scale 0
    adb -s "$SERIAL" shell settings put global transition_animation_scale 0
    adb -s "$SERIAL" shell settings put global animator_duration_scale 0

    adb -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP || true
    adb -s "$SERIAL" shell wm dismiss-keyguard || true

    # Verify package manager once more immediately before Gradle installation.
    if ! package_manager_is_available; then
        fail_with_diagnostics \
            "Android package manager became unavailable before test execution."
    fi
}

run_instrumented_tests() {
    echo "--- Running Android instrumented tests ---"

    ./gradlew \
        --no-daemon \
        --stacktrace \
        :app:connectedDebugAndroidTest
}

trap collect_diagnostics ERR

echo "--- Detecting Android device ---"

SERIAL="$(detect_device)"
readonly SERIAL

echo "Using Android device: $SERIAL"

stabilize_device
run_instrumented_tests

echo "Android instrumented tests: PASS"
