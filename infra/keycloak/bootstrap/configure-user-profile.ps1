param(
    [Parameter(Mandatory = $true)]
    [string]$AdminPassword
)

$ErrorActionPreference = "Stop"

$container = "autovision-keycloak"
$realm = "autovision"
$attributeName = "autovision_user_ref_id"
$kcadmConfig = "/tmp/autovision-profile-$([guid]::NewGuid().ToString('N')).config"

function Invoke-NativeDocker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [string]$InputText,

        [Parameter(Mandatory = $true)]
        [string]$Purpose,

        [switch]$SuppressFailure
    )

    $stderrPath =
        Join-Path `
            $env:TEMP `
            "autovision-docker-$([guid]::NewGuid().ToString('N')).stderr"

    try {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"

        try {
            if ($PSBoundParameters.ContainsKey("InputText")) {
                $output =
                    $InputText |
                    & docker @Arguments 2> $stderrPath
            }
            else {
                $output =
                    & docker @Arguments 2> $stderrPath
            }

            $nativeExitCode = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }

        $stderr = ""
        if (Test-Path $stderrPath) {
            $stderr = Get-Content $stderrPath -Raw
        }

        if ($nativeExitCode -ne 0) {

            if ($SuppressFailure) {
                return $null
            }

            $sanitizedStderr =
                $stderr `
                    -replace [regex]::Escape($AdminPassword), "<REDACTED>" `
                    -replace '(?i)(bearer\s+)[^\s]+', '$1<REDACTED>' `
                    -replace '(?i)((password|token|secret)[\s:=]+)[^\s]+', '$1<REDACTED>'

            throw "$Purpose failed with exit code $nativeExitCode. stderr: $sanitizedStderr"
        }

        return $output
    }
    finally {
        Remove-Item `
            $stderrPath `
            -ErrorAction SilentlyContinue
    }
}

function Invoke-Keycloak {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [string]$InputText,

        [Parameter(Mandatory = $true)]
        [string]$Purpose
    )

    $dockerArguments = @(
        "exec",
        $container,
        "/opt/keycloak/bin/kcadm.sh"
    ) + $Arguments + @(
        "--config",
        $kcadmConfig
    )

    if ($PSBoundParameters.ContainsKey("InputText")) {
        return Invoke-NativeDocker `
            -Arguments $dockerArguments `
            -InputText $InputText `
            -Purpose $Purpose
    }

    return Invoke-NativeDocker `
        -Arguments $dockerArguments `
        -Purpose $Purpose
}

try {
    Write-Host "Authenticating Keycloak admin..."

    Invoke-Keycloak `
        -Arguments @(
            "config",
            "credentials",
            "--server",
            "http://localhost:8080",
            "--realm",
            "master",
            "--user",
            "admin",
            "--password",
            $AdminPassword
        ) `
        -Purpose "Keycloak admin authentication" |
        Out-Null

    $profileJson =
        Invoke-Keycloak `
            -Arguments @(
                "get",
                "users/profile",
                "-r",
                $realm
            ) `
            -Purpose "Keycloak User Profile read"

    $profile =
        ($profileJson | Out-String) |
        ConvertFrom-Json

    $existing =
        $profile.attributes |
        Where-Object {
            $_.name -eq $attributeName
        }

    if ($existing) {
        Write-Host "Managed attribute already exists: $attributeName"
    }
    else {
        $newAttribute = [PSCustomObject]@{
            name        = $attributeName
            displayName = "AutoVision User Reference ID"
            validations = [PSCustomObject]@{
                length = [PSCustomObject]@{
                    min = 36
                    max = 36
                }
            }
            permissions = [PSCustomObject]@{
                view = @("admin")
                edit = @("admin")
            }
            multivalued = $false
        }

        $profile.attributes += $newAttribute

        $updatedProfileJson =
            $profile |
            ConvertTo-Json -Depth 20

        $dockerArguments = @(
            "exec",
            "-i",
            $container,
            "/opt/keycloak/bin/kcadm.sh",
            "update",
            "users/profile",
            "-r",
            $realm,
            "-f",
            "-",
            "--config",
            $kcadmConfig
        )

        Invoke-NativeDocker `
            -Arguments $dockerArguments `
            -InputText $updatedProfileJson `
            -Purpose "Keycloak User Profile update" |
            Out-Null

        Write-Host "Created managed attribute: $attributeName"
    }

    Write-Host "KEYCLOAK USER PROFILE: PASS"
}
finally {
    Invoke-NativeDocker `
        -Arguments @(
            "exec",
            $container,
            "rm",
            "-f",
            $kcadmConfig
        ) `
        -Purpose "kcadm config cleanup" `
        -SuppressFailure |
        Out-Null
}