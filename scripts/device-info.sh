#!/bin/bash
set -euo pipefail
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.product.model
