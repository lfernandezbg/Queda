$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
. "$PSScriptRoot\Invoke-CheckedCommand.ps1"
Invoke-CheckedCommand -Executable ".\gradlew.bat" -Arguments ":app:assembleDebug"
