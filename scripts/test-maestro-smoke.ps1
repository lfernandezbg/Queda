param (
    [string]$DeviceSerial,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. "$PSScriptRoot\Invoke-CheckedCommand.ps1"

Write-Host "--- Maestro Smoke (Local) ---"

$resultsDir = ".maestro/results"
if (Test-Path $resultsDir) {
    Remove-Item -Path $resultsDir -Recurse -Force
}
New-Item -ItemType Directory -Path $resultsDir -Force

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

if (!$SkipBuild) {
    Write-Host "Building E2E APK..."
    Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":app:assembleE2E"
}

$apkPath = "app/build/outputs/apk/e2e"
$apks = @(Get-ChildItem -Path $apkPath -Filter "*.apk")
if ($apks.Count -ne 1) {
    throw "Expected exactly one APK in $apkPath, found $($apks.Count)"
}
$apkFile = $apks[0].FullName

Write-Host "Installing $apkFile (non-streaming)..."
Invoke-CheckedCommand -Executable "adb" -Arguments "-s", $serial, "install", "--no-streaming", "-r", $apkFile

$flows = @(
    Get-ChildItem -Path ".maestro/flows/smoke" -Filter "*.yaml" -File |
        Sort-Object Name |
        ForEach-Object { $_.FullName }
)

if ($flows.Count -eq 0) {
    throw "No Maestro flows found."
}

Write-Host "Running Maestro tests in .maestro/flows/smoke ..."
$maestroArguments = @(
    "--device=$serial",
    "test",
    "--config",
    ".maestro/config.yaml"
)
$maestroArguments += $flows
$maestroArguments += @(
    "--format",
    "junit",
    "--output",
    "$resultsDir/report.xml",
    "--test-output-dir",
    "$resultsDir/maestro-artifacts"
)

Invoke-CheckedCommand `
    -Executable "maestro" `
    -Arguments $maestroArguments

if (!(Test-Path "$resultsDir/report.xml")) {
    throw "Maestro report.xml not found."
}

$reportContent = Get-Content "$resultsDir/report.xml" -Raw
if ([string]::IsNullOrWhiteSpace($reportContent)) {
    throw "Maestro report.xml is empty."
}

Write-Host "PASS"
