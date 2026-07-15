param (
    [string]$DeviceSerial
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. "$PSScriptRoot\Invoke-CheckedCommand.ps1"

Write-Host "--- Phase 0: Maestro Smoke ---"

if (Test-Path ".maestro/results") {
    Remove-Item -Path ".maestro/results" -Recurse -Force
}
New-Item -ItemType Directory -Path ".maestro/results" -Force

if (!(Get-Command maestro -ErrorAction SilentlyContinue)) {
    throw "Maestro CLI not found."
}

$maestroVersion = (& maestro --version | Out-String).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Unable to obtain Maestro version."
}

if ($maestroVersion -notmatch '(^|[^0-9])2\.6\.0([^0-9]|$)') {
    throw "Expected Maestro 2.6.0, found: $maestroVersion"
}

if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "ADB not found."
}

$adbDevices = @(adb devices | Select-String -Pattern "\tdevice$")
$connectedSerials = @($adbDevices | ForEach-Object { $_.ToString().Split("`t")[0] })

if ($DeviceSerial) {
    if ($connectedSerials -notcontains $DeviceSerial) {
        throw "Device $DeviceSerial not found or not in 'device' state."
    }
    $serial = $DeviceSerial
} else {
    if ($connectedSerials.Count -eq 0) {
        throw "No devices connected."
    }
    if ($connectedSerials.Count -gt 1) {
        throw "Multiple devices found. Specify -DeviceSerial."
    }
    $serial = $connectedSerials[0]
}

Write-Host "Using device: $serial"

Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":app:assembleE2E"

$apkPath = "app/build/outputs/apk/e2e"
$apks = @(Get-ChildItem -Path $apkPath -Filter "*.apk")
if ($apks.Count -ne 1) {
    throw "Expected exactly one APK in $apkPath, found $($apks.Count)"
}
$apkFile = $apks[0].FullName

Invoke-CheckedCommand -Executable "adb" -Arguments "-s", $serial, "install", "-r", $apkFile

$flows = @(
    ".maestro/flows/smoke/00_launch_app.yaml",
    ".maestro/flows/smoke/01_reset_and_launch.yaml",
    ".maestro/flows/smoke/02_seed_empty_and_launch.yaml"
)

foreach ($flow in $flows) {
    Invoke-CheckedCommand -Executable "maestro" -Arguments "--device=$serial", "test", $flow
}

Invoke-CheckedCommand -Executable "maestro" -Arguments "--device=$serial", "test", ".maestro/flows/smoke", "--format", "junit", "--output", ".maestro/results/report.xml"

if (!(Test-Path ".maestro/results/report.xml")) {
    throw "Maestro report.xml not found."
}

$reportContent = Get-Content ".maestro/results/report.xml" -Raw
if ([string]::IsNullOrWhiteSpace($reportContent)) {
    throw "Maestro report.xml is empty."
}

Write-Host "PASS"
