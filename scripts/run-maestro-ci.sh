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