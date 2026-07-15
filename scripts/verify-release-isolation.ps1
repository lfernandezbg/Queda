$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. "$PSScriptRoot\Invoke-CheckedCommand.ps1"

Write-Host "--- Phase 0: Verifying Release Isolation ---"

# 1. Manifest Check
$manifestDir = "app/build/intermediates/merged_manifests/release/"
if (!(Test-Path $manifestDir)) {
    throw "Merged manifest directory not found: $manifestDir. Run assembleRelease first."
}

$manifests = @(Get-ChildItem -Path $manifestDir -Filter "AndroidManifest.xml" -Recurse)
if ($manifests.Count -eq 0) {
    throw "No AndroidManifest.xml found in $manifestDir"
}

foreach ($m in $manifests) {
    $content = Get-Content $m.FullName -Raw
    if ($content -match "E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e") {
        throw "CRITICAL: E2E leakage found in $($m.FullName)"
    }
}

Write-Host "Manifest check PASS"

# 2. APK Check
$apkDir = "app/build/outputs/apk/release/"
if (!(Test-Path $apkDir)) {
    throw "Release APK directory not found: $apkDir"
}

$apks = @(Get-ChildItem -Path $apkDir -Filter "*.apk" -Recurse)
if ($apks.Count -eq 0) {
    throw "No release APK found in $apkDir"
}
if ($apks.Count -gt 1) {
    throw "Multiple APKs found in $apkDir. Expected exactly one."
}
$apkFile = $apks[0].FullName

Write-Host "Analyzing APK: $apkFile"

$apkanalyzer = ""
if (Get-Command apkanalyzer -ErrorAction SilentlyContinue) {
    $apkanalyzer = "apkanalyzer"
} elseif ($env:ANDROID_SDK_ROOT -and (Test-Path "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\apkanalyzer.bat")) {
    $apkanalyzer = "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\apkanalyzer.bat"
} elseif ($env:ANDROID_HOME -and (Test-Path "$env:ANDROID_HOME\cmdline-tools\latest\bin\apkanalyzer.bat")) {
    $apkanalyzer = "$env:ANDROID_HOME\cmdline-tools\latest\bin\apkanalyzer.bat"
} else {
    throw "apkanalyzer not found. Please set ANDROID_SDK_ROOT or add to PATH."
}

$appId = & $apkanalyzer manifest application-id $apkFile
if ($LASTEXITCODE -ne 0) { throw "apkanalyzer failed" }
if ($appId -ne "com.luisete.queda") {
    throw "Invalid Application ID: $appId"
}

$apkManifest = & $apkanalyzer manifest print $apkFile
if ($LASTEXITCODE -ne 0) { throw "apkanalyzer failed" }
if ($apkManifest -match "E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e") {
    throw "CRITICAL: E2E leakage found in APK manifest"
}

$dexPackages = & $apkanalyzer dex packages $apkFile
if ($LASTEXITCODE -ne 0) { throw "apkanalyzer failed" }
if ($dexPackages -match "com\.luisete\.queda\.core\.testing|E2ETestControlActivity|queda-e2e|com\.luisete\.queda\.e2e") {
    throw "CRITICAL: E2E leakage found in APK classes/DEX"
}

Write-Host "APK content check PASS"
Write-Host "PASS"
