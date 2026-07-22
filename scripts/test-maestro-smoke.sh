#!/bin/bash
set -euo pipefail

# --- Default values ---
DEVICE_SERIAL=""
SKIP_BUILD="false"
SKIP_INSTALL="false"
SHARD_INDEX=0
SHARD_COUNT=1
APK_FILE=""
OUTPUT_DIR=".maestro/results"

# --- Argument parsing ---
while [[ $# -gt 0 ]]; do
  case $1 in
    --device-serial) DEVICE_SERIAL="$2"; shift 2 ;;
    --skip-build) SKIP_BUILD="true"; shift ;;
    --skip-install) SKIP_INSTALL="true"; shift ;;
    --shard-index) SHARD_INDEX="$2"; shift 2 ;;
    --shard-count) SHARD_COUNT="$2"; shift 2 ;;
    --apk) APK_FILE="$2"; shift 2 ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

# --- Validation ---
if ! [[ "$SHARD_COUNT" =~ ^[0-9]+$ ]] || [ "$SHARD_COUNT" -le 0 ]; then
    echo "Error: Shard count must be a positive integer."
    exit 1
fi

if ! [[ "$SHARD_INDEX" =~ ^[0-9]+$ ]] || [ "$SHARD_INDEX" -ge "$SHARD_COUNT" ]; then
    echo "Error: Shard index '$SHARD_INDEX' out of range (count: $SHARD_COUNT)."
    exit 1
fi

echo "--- Maestro Smoke: Shard $SHARD_INDEX/$SHARD_COUNT ---"

mkdir -p "$OUTPUT_DIR"

if ! command -v maestro &> /dev/null; then
    echo "Error: Maestro CLI not found."
    exit 1
fi

MAESTRO_VERSION_OUT=$(maestro --version)
if ! echo "$MAESTRO_VERSION_OUT" | grep -Eq "(^|[^0-9])2\.6\.0([^0-9]|$)"; then
    echo "Error: Expected Maestro 2.6.0, found: $MAESTRO_VERSION_OUT"
    exit 1
fi

if ! command -v adb &> /dev/null; then
    echo "Error: ADB not found."
    exit 1
fi

# --- Device detection ---
mapfile -t CONNECTED_DEVICES < <(
    adb devices |
        awk 'NR > 1 && $2 == "device" { print $1 }'
)

if [ -n "$DEVICE_SERIAL" ]; then
    FOUND=false
    for d in "${CONNECTED_DEVICES[@]}"; do
        if [[ "$d" == "$DEVICE_SERIAL" ]]; then FOUND=true; break; fi
    done
    if [ "$FOUND" = false ]; then
        echo "Error: Device $DEVICE_SERIAL not found or not in 'device' state."
        exit 1
    fi
    SERIAL=$DEVICE_SERIAL
else
    if [ "${#CONNECTED_DEVICES[@]}" -eq 0 ]; then echo "Error: No devices connected."; exit 1; fi
    if [ "${#CONNECTED_DEVICES[@]}" -gt 1 ]; then echo "Error: Multiple devices found. Use --device-serial."; exit 1; fi
    SERIAL="${CONNECTED_DEVICES[0]}"
fi
echo "Using device: $SERIAL"

# --- Build (optional) ---
if [ "$SKIP_BUILD" != "true" ]; then
    echo "Building E2E APK..."
    ./gradlew :app:assembleE2E
fi

# --- APK Selection ---
if [ -z "$APK_FILE" ]; then
    APK_PATH="app/build/outputs/apk/e2e"
    mapfile -t APK_FILES < <(
        find "$APK_PATH" -maxdepth 1 -type f -name "*.apk" -print |
            sort
    )
    if [ "${#APK_FILES[@]}" -ne 1 ]; then
        echo "Error: Expected exactly one APK in $APK_PATH, found ${#APK_FILES[@]}"
        exit 1
    fi
    APK_FILE="${APK_FILES[0]}"
fi

# --- Install (optional) ---
if [ "$SKIP_INSTALL" != "true" ]; then
    echo "Installing $APK_FILE (non-streaming)..."
    adb -s "$SERIAL" install --no-streaming -r "$APK_FILE"
fi

# --- Sharding ---
FLOWS_DIR=".maestro/flows/smoke"
mapfile -t ALL_FLOWS < <(
    find "$FLOWS_DIR" -maxdepth 1 -type f -name "*.yaml" -print |
        sort
)
TOTAL_FLOWS_COUNT="${#ALL_FLOWS[@]}"

if [ "$TOTAL_FLOWS_COUNT" -eq 0 ]; then
    echo "Error: No flows found in $FLOWS_DIR"
    exit 1
fi

# Round-robin distribution
SHARD_FLOWS=()
for (( i=0; i<$TOTAL_FLOWS_COUNT; i++ )); do
    if [ $((i % SHARD_COUNT)) -eq "$SHARD_INDEX" ]; then
        SHARD_FLOWS+=("${ALL_FLOWS[i]}")
    fi
done

if [ "${#SHARD_FLOWS[@]}" -eq 0 ]; then
    echo "Error: No flows assigned to shard $SHARD_INDEX."
    exit 1
fi

echo "Flows assigned to this shard:"
for f in "${SHARD_FLOWS[@]}"; do echo "  - $f"; done

# --- Maestro Execution ---
echo "Running Maestro with config: .maestro/config.yaml"

EXIT_CODE=0
maestro \
    --device="$SERIAL" \
    test \
    --config .maestro/config.yaml \
    "${SHARD_FLOWS[@]}" \
    --format junit \
    --output "$OUTPUT_DIR/report.xml" \
    --test-output-dir "$OUTPUT_DIR/maestro-artifacts" || EXIT_CODE=$?

if [ ! -s "$OUTPUT_DIR/report.xml" ]; then
    echo "Error: Maestro report.xml not found or empty."
    exit 1
fi

if [ $EXIT_CODE -ne 0 ]; then
    echo "Maestro failed with exit code $EXIT_CODE"
    exit $EXIT_CODE
fi

echo "PASS"
