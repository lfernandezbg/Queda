param(
    [string]$DeviceSerial
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-AdbCapture {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $result = & adb @Arguments 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "ADB failed: adb $($Arguments -join ' ')"
    }

    return ($result | Out-String).Trim()
}

if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "ADB not found."
}

$deviceOutput = Invoke-AdbCapture -Arguments @("devices")

$connectedSerials =
    @(
        $deviceOutput -split "`r?`n" |
        ForEach-Object {
            if ($_ -match "^([^\s]+)\s+device$") {
                $matches[1]
            }
        }
    )

if ($DeviceSerial) {
    if ($connectedSerials -notcontains $DeviceSerial) {
        throw "Device $DeviceSerial not found or not in device state."
    }

    $serial = $DeviceSerial
}
else {
    if ($connectedSerials.Count -eq 0) {
        throw "No Android device is connected."
    }

    if ($connectedSerials.Count -gt 1) {
        throw "Multiple Android devices found. Specify -DeviceSerial."
    }

    $serial = $connectedSerials[0]
}

Write-Host "--- Device Info ---"
Write-Host "Serial: $serial"

$properties = @(
    "ro.product.manufacturer",
    "ro.product.model",
    "ro.build.version.release",
    "ro.build.version.sdk",
    "ro.build.version.security_patch",
    "ro.build.version.incremental"
)

foreach ($property in $properties) {
    $value =
        Invoke-AdbCapture -Arguments @(
            "-s",
            $serial,
            "shell",
            "getprop",
            $property
        )

    Write-Host "${property}: $value"
}

$size =
    Invoke-AdbCapture -Arguments @(
        "-s",
        $serial,
        "shell",
        "wm",
        "size"
    )

$density =
    Invoke-AdbCapture -Arguments @(
        "-s",
        $serial,
        "shell",
        "wm",
        "density"
    )

Write-Host $size
Write-Host $density
