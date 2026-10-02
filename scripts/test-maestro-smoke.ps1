param (
    [string]$DeviceSerial,
    [switch]$SkipBuild,
    [int]$ShardIndex = 0,
    [int]$ShardCount = 1
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. "$PSScriptRoot\Invoke-CheckedCommand.ps1"

Write-Host "--- Maestro Smoke (Local) ---"

$resultsDir = ".maestro/results"
if ($ShardIndex -eq 0 -and (Test-Path $resultsDir)) {
    Remove-Item -Path $resultsDir -Recurse -Force
}
if (!(Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force
}

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

$allFlows = @(
    Get-ChildItem -Path ".maestro/flows/smoke" -Filter "*.yaml" -File |
        Sort-Object Name |
        ForEach-Object { $_.FullName }
)

if ($allFlows.Count -eq 0) {
    throw "No Maestro flows found."
}

$flows = @()
for ($i = 0; $i -lt $allFlows.Count; $i++) {
    if (($i % $ShardCount) -eq $ShardIndex) {
        $flows += $allFlows[$i]
    }
}

Write-Host "Running Maestro tests in .maestro/flows/smoke ..."
$maestroArguments = @(
    "--device=$serial",
    "test",
    "--config",
    ".maestro/config.yaml"
)
$maestroArguments += $flows
$reportFileName = if ($ShardCount -gt 1) { "report_shard${ShardIndex}.xml" } else { "report.xml" }

$maestroArguments += @(
    "--format",
    "junit",
    "--output",
    "$resultsDir/$reportFileName",
    "--test-output-dir",
    "$resultsDir/maestro-artifacts"
)

Invoke-CheckedCommand `
    -Executable "maestro" `
    -Arguments $maestroArguments

if (!(Test-Path "$resultsDir/$reportFileName")) {
    throw "Maestro $reportFileName not found."
}

# Merge all shard reports if multiple shards have run
$shardReports = @(Get-ChildItem -Path $resultsDir -Filter "report_shard*.xml")
if ($shardReports.Count -gt 0) {
    $allTestCases = @()
    $totalTests = 0
    $totalFailures = 0
    $totalErrors = 0

    foreach ($sReport in $shardReports) {
        [xml]$xml = Get-Content $sReport.FullName
        $suite = $xml.testsuites.testsuite
        if ($suite) {
            $t = $suite.GetAttribute("tests")
            if ($t) { $totalTests += [int]$t }
            $f = $suite.GetAttribute("failures")
            if ($f) { $totalFailures += [int]$f }
            $e = $suite.GetAttribute("errors")
            if ($e) { $totalErrors += [int]$e }
            foreach ($tc in $suite.testcase) {
                $allTestCases += $tc.OuterXml
            }
        }
    }

    $mergedXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="Smoke Test Suite" tests="$totalTests" failures="$totalFailures" errors="$totalErrors">
$($allTestCases -join "`n")
  </testsuite>
</testsuites>
"@
    Set-Content -Path "$resultsDir/report.xml" -Value $mergedXml -Encoding UTF8
}

if (!(Test-Path "$resultsDir/report.xml")) {
    throw "Maestro report.xml not found."
}

$reportContent = Get-Content "$resultsDir/report.xml" -Raw
if ([string]::IsNullOrWhiteSpace($reportContent)) {
    throw "Maestro report.xml is empty."
}

Write-Host "PASS"
