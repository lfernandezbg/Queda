#!/bin/bash
set -euo pipefail

echo "--- Phase 0: Test All Fast ---"

./gradlew clean
./gradlew test
./gradlew :quality:architecture:testDebugUnitTest
./gradlew lint
./gradlew detekt
./gradlew ktlintCheck
./gradlew jacocoTestReport
./gradlew jacocoTestCoverageVerification
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:assembleE2E

chmod +x scripts/verify-release-isolation.sh
./scripts/verify-release-isolation.sh

echo "PASS"
