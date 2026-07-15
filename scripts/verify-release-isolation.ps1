$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $projectRoot

try {
    Write-Host "--- Phase 0: Verifying Release Isolation ---"

    $forbiddenManifestPatterns = @(
        "E2ETestControlActivity",
        "queda-e2e",
        "com\.luisete\.queda\.e2e"
    )

    $manifestRoot = "app/build/intermediates/merged_manifests/release"

    if (!(Test-Path $manifestRoot)) {
        throw "Merged release manifest directory not found: $manifestRoot"
    }

    $manifests = @(
        Get-ChildItem `
            -Path $manifestRoot `
            -Filter "AndroidManifest.xml" `
            -File `
            -Recurse
    )

    if ($manifests.Count -eq 0) {
        throw "No merged release AndroidManifest.xml was found."
    }

    foreach ($manifest in $manifests) {
        $content = Get-Content $manifest.FullName -Raw

        foreach ($pattern in $forbiddenManifestPatterns) {
            if ($content -match $pattern) {
                throw "E2E leakage found in merged manifest $($manifest.FullName): $pattern"
            }
        }
    }

    Write-Host "Merged manifest isolation: PASS"

    $metadataFiles = @(
        Get-ChildItem `
            -Path "app/build/outputs/apk/release" `
            -Filter "output-metadata.json" `
            -File `
            -Recurse
    )

    if ($metadataFiles.Count -ne 1) {
        throw "Expected exactly one release output-metadata.json, found $($metadataFiles.Count)."
    }

    $metadata =
        Get-Content $metadataFiles[0].FullName -Raw |
        ConvertFrom-Json

    if ($metadata.applicationId -ne "com.luisete.queda") {
        throw "Invalid release applicationId: $($metadata.applicationId)"
    }

    Write-Host "Release applicationId: PASS"

    $releaseApks = @(
        Get-ChildItem `
            -Path "app/build/outputs/apk/release" `
            -Filter "*.apk" `
            -File `
            -Recurse
    )

    if ($releaseApks.Count -ne 1) {
        throw "Expected exactly one release APK, found $($releaseApks.Count)."
    }

    $releaseApk = $releaseApks[0]

    Write-Host "Analyzing APK: $($releaseApk.FullName)"

    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $archive =
        [System.IO.Compression.ZipFile]::OpenRead(
            $releaseApk.FullName
        )

    try {
        $dexEntries =
            @(
                $archive.Entries |
                Where-Object {
                    $_.Name -match "^classes([0-9]*)\.dex$"
                }
            )

        if ($dexEntries.Count -eq 0) {
            throw "No classes*.dex files found in release APK."
        }

        $forbiddenDexTokens = @(
            "com/luisete/queda/core/testing",
            "com.luisete.queda.core.testing",
            "E2ETestControlActivity",
            "com/luisete/queda/e2e",
            "com.luisete.queda.e2e",
            "queda-e2e"
        )

        foreach ($entry in $dexEntries) {
            $entryStream = $entry.Open()
            $memoryStream = New-Object System.IO.MemoryStream

            try {
                $entryStream.CopyTo($memoryStream)
                $bytes = $memoryStream.ToArray()
                $ascii =
                    [System.Text.Encoding]::ASCII.GetString(
                        $bytes
                    )

                foreach ($token in $forbiddenDexTokens) {
                    if ($ascii.Contains($token)) {
                        throw "E2E leakage found in $($entry.Name): $token"
                    }
                }
            }
            finally {
                $memoryStream.Dispose()
                $entryStream.Dispose()
            }
        }
    }
    finally {
        $archive.Dispose()
    }

    Write-Host "Release DEX isolation: PASS"
    Write-Host "Release isolation: PASS"
}
finally {
    Pop-Location
}
