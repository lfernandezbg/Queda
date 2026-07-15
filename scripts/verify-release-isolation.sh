#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(
    cd "$(dirname "${BASH_SOURCE[0]}")/.." &&
    pwd
)"

cd "$PROJECT_ROOT"

echo "--- Phase 0: Verifying Release Isolation ---"

MANIFEST_ROOT="app/build/intermediates/merged_manifests/release"

if [[ ! -d "$MANIFEST_ROOT" ]]; then
    echo "Merged release manifest directory not found: $MANIFEST_ROOT"
    exit 1
fi

mapfile -t MANIFESTS < <(
    find "$MANIFEST_ROOT" \
        -type f \
        -name "AndroidManifest.xml"
)

if [[ "${#MANIFESTS[@]}" -eq 0 ]]; then
    echo "No merged release AndroidManifest.xml was found."
    exit 1
fi

for manifest in "${MANIFESTS[@]}"; do
    if grep -Eq \
        'E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e' \
        "$manifest"; then
        echo "E2E leakage found in merged manifest: $manifest"
        exit 1
    fi
done

echo "Merged manifest isolation: PASS"

mapfile -t METADATA_FILES < <(
    find "app/build/outputs/apk/release" \
        -type f \
        -name "output-metadata.json"
)

if [[ "${#METADATA_FILES[@]}" -ne 1 ]]; then
    echo "Expected exactly one output-metadata.json, found ${#METADATA_FILES[@]}."
    exit 1
fi

mapfile -t RELEASE_APKS < <(
    find "app/build/outputs/apk/release" \
        -type f \
        -name "*.apk"
)

if [[ "${#RELEASE_APKS[@]}" -ne 1 ]]; then
    echo "Expected exactly one release APK, found ${#RELEASE_APKS[@]}."
    exit 1
fi

python3 - \
    "${METADATA_FILES[0]}" \
    "${RELEASE_APKS[0]}" <<'PYTHON'
import json
import re
import sys
import zipfile
from pathlib import Path

metadata_path = Path(sys.argv[1])
apk_path = Path(sys.argv[2])

metadata = json.loads(
    metadata_path.read_text(encoding="utf-8")
)

application_id = metadata.get("applicationId")

if application_id != "com.luisete.queda":
    raise SystemExit(
        f"Invalid release applicationId: {application_id}"
    )

forbidden_tokens = (
    b"com/luisete/queda/core/testing",
    b"com.luisete.queda.core.testing",
    b"E2ETestControlActivity",
    b"com/luisete/queda/e2e",
    b"com.luisete.queda.e2e",
    b"queda-e2e",
)

with zipfile.ZipFile(apk_path) as archive:
    dex_names = [
        name
        for name in archive.namelist()
        if re.fullmatch(r"classes[0-9]*\.dex", Path(name).name)
    ]

    if not dex_names:
        raise SystemExit(
            "No classes*.dex files found in release APK."
        )

    for dex_name in dex_names:
        content = archive.read(dex_name)

        for token in forbidden_tokens:
            if token in content:
                raise SystemExit(
                    f"E2E leakage found in {dex_name}: "
                    f"{token.decode('ascii')}"
                )

print("Release applicationId: PASS")
print("Release DEX isolation: PASS")
print("Release isolation: PASS")
PYTHON
