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

APK_FILE=$(find "$APK_DIR" -name "*.apk" | head -n 1)
if [ -z "$APK_FILE" ]; then
    echo "No release APK found in $APK_DIR"
    exit 1
fi

echo "Analyzing APK: $APK_FILE"

APP_ID=$(apkanalyzer manifest application-id "$APK_FILE")
if [ "$APP_ID" != "com.luisete.queda" ]; then
    echo "Invalid Application ID: $APP_ID"
    exit 1
fi

if apkanalyzer manifest print "$APK_FILE" | grep -qE "E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e"; then
    echo "CRITICAL: E2E leakage found in APK manifest"
    exit 1
fi

if apkanalyzer dex packages "$APK_FILE" | grep -q "com\.luisete\.queda\.core\.testing"; then
    echo "CRITICAL: com.luisete.queda.core.testing found in APK classes"
    exit 1
fi

echo "APK content check PASS"
echo "PASS"
