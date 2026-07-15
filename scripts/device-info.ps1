$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
. "$PSScriptRoot\Invoke-CheckedCommand.ps1"
Invoke-CheckedCommand -Executable "adb" -Arguments "devices"
Invoke-CheckedCommand -Executable "adb" -Arguments "shell", "getprop", "ro.build.version.release"
Invoke-CheckedCommand -Executable "adb" -Arguments "shell", "getprop", "ro.product.model"
