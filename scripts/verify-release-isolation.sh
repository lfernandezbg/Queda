#!/bin/bash
set -euo pipefail

echo "--- Phase 0: Verifying Release Isolation ---"

MANIFEST_DIR="app/build/intermediates/merged_manifests/release/"
if [ ! -d "$MANIFEST_DIR" ]; then
    echo "Merged manifest directory not found: $MANIFEST_DIR. Run assembleRelease first."
    exit 1
fi

FOUND_MANIFESTS=$(find "$MANIFEST_DIR" -name "AndroidManifest.xml")
if [ -z "$FOUND_MANIFESTS" ]; then
    echo "No AndroidManifest.xml found in $MANIFEST_DIR"
    exit 1
fi

for m in $FOUND_MANIFESTS; do
    if grep -qE "E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e" "$m"; then
        echo "CRITICAL: E2E leakage found in $m"
        exit 1
    fi
done

echo "Manifest check PASS"

APK_DIR="app/build/outputs/apk/release/"
if [ ! -d "$APK_DIR" ]; then
    echo "Release APK directory not found: $APK_DIR"
    exit 1
fi

APK_FILES=$(find "$APK_DIR" -name "*.apk")
APK_COUNT=$(echo "$APK_FILES" | grep -v '^$' | wc -l)

if [ "$APK_COUNT" -eq 0 ]; then
    echo "No release APK found in $APK_DIR"
    exit 1
fi
if [ "$APK_COUNT" -gt 1 ]; then
    echo "Multiple APKs found in $APK_DIR. Expected exactly one."
    exit 1
fi
APK_FILE=$(echo "$APK_FILES" | xargs)

echo "Analyzing APK: $APK_FILE"

APK_ANALYZER=""
if command -v apkanalyzer &> /dev/null; then
    APK_ANALYZER="apkanalyzer"
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -f "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/apkanalyzer" ]; then
    APK_ANALYZER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/apkanalyzer"
elif [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer" ]; then
    APK_ANALYZER="$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer"
else
    echo "apkanalyzer not found."
    exit 1
fi

APP_ID=$($APK_ANALYZER manifest application-id "$APK_FILE")
if [ "$APP_ID" != "com.luisete.queda" ]; then
    echo "Invalid Application ID: $APP_ID"
    exit 1
fi

if $APK_ANALYZER manifest print "$APK_FILE" | grep -qE "E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e"; then
    echo "CRITICAL: E2E leakage found in APK manifest"
    exit 1
fi

if $APK_ANALYZER dex packages "$APK_FILE" | grep -qE "com\.luisete\.queda\.core\.testing|E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e"; then
    echo "CRITICAL: E2E leakage found in APK classes/DEX"
    exit 1
fi

echo "APK content check PASS"
echo "PASS"
