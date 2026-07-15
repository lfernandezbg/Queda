#!/bin/bash
set -euo pipefail
./gradlew :quality:architecture:testDebugUnitTest
