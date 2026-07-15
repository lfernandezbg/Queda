function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Executable,

        [Parameter()]
        [string[]]$Arguments = @()
    )

    & $Executable @Arguments

    if ($LASTEXITCODE -ne 0) {
        $msg = "Command failed with exit code $LASTEXITCODE. Command: $Executable " + ($Arguments -join ' ')
        throw $msg
    }
}
