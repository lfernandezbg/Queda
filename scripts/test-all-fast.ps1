$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

. "$PSScriptRoot\Invoke-CheckedCommand.ps1"

Write-Host "--- Phase 0: Test All Fast ---"

Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments "clean"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments "test"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":quality:architecture:testDebugUnitTest"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments "lint"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments "detekt"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments "ktlintCheck"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":app:assembleDebug"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":app:assembleRelease"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":app:assembleE2E"

Invoke-CheckedCommand -Executable "powershell" -Arguments "-ExecutionPolicy", "Bypass", "-File", "$PSScriptRoot\verify-release-isolation.ps1"

Write-Host "PASS"
