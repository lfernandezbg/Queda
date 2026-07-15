param(
    [string]$DeviceSerial
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

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

Write-Host "--- Device Info: $serial ---"

$props = @(
    "ro.product.manufacturer",
    "ro.product.model",
    "ro.build.version.release",
    "ro.build.version.sdk",
    "ro.build.version.security_patch",
    "ro.build.version.incremental"
)

foreach ($prop in $props) {
    $val = (adb -s $serial shell getprop $prop).Trim()
    Write-Host "${prop}: $val"
}

$size = (adb -s $serial shell wm size).Trim()
Write-Host "$size"

$density = (adb -s $serial shell wm density).Trim()
Write-Host "$density"
