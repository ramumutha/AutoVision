param(
    [string]$Username = $env:AUTOVISION_LOCAL_DEV_USERNAME,
    [string]$UserRefId = $env:AUTOVISION_LOCAL_DEV_USER_REF_ID,
    [string]$TenantId = $env:AUTOVISION_LOCAL_DEV_TENANT_ID,
    [string]$Email = $env:AUTOVISION_LOCAL_DEV_EMAIL
)

$ErrorActionPreference = "Stop"
$AdminPassword = $env:KEYCLOAK_ADMIN_PASSWORD
$UserPassword = $env:AUTOVISION_LOCAL_DEV_PASSWORD

function Assert-LocalValue([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required. Set it in ignored .env or pass it to the script."
    }
}

Assert-LocalValue "KEYCLOAK_ADMIN_PASSWORD" $AdminPassword
Assert-LocalValue "AUTOVISION_LOCAL_DEV_PASSWORD" $UserPassword
Assert-LocalValue "AUTOVISION_LOCAL_DEV_USERNAME" $Username
Assert-LocalValue "AUTOVISION_LOCAL_DEV_USER_REF_ID" $UserRefId
Assert-LocalValue "AUTOVISION_LOCAL_DEV_TENANT_ID" $TenantId

$parsedUserRefId = [guid]::Empty
$parsedTenantId = [guid]::Empty
if (-not [guid]::TryParse($UserRefId, [ref]$parsedUserRefId)) {
    throw "AUTOVISION_LOCAL_DEV_USER_REF_ID must be a UUID."
}
if (-not [guid]::TryParse($TenantId, [ref]$parsedTenantId)) {
    throw "AUTOVISION_LOCAL_DEV_TENANT_ID must be a UUID."
}

$container = "autovision-keycloak"
$databaseContainer = "autovision-postgres"
$realm = "autovision"
$displayName = "AutoVision Local Development User"
$kcadmConfig = "/tmp/autovision-kcadm-$([guid]::NewGuid().ToString('N')).config"
if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = "$Username@local.autovision"
}

function Invoke-NativeDocker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [string]$InputText,
        [string]$Purpose = "Native Docker command",
        [switch]$SuppressFailure
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $stderrPath = [System.IO.Path]::GetTempFileName()
    try {
        $ErrorActionPreference = "Continue"
        if ($null -eq $InputText) {
            $output = & docker @Arguments 2> $stderrPath
        }
        else {
            $output = $InputText | & docker @Arguments 2> $stderrPath
        }
        $nativeExitCode = $LASTEXITCODE

        if ($nativeExitCode -ne 0 -and -not $SuppressFailure) {
            $stderr = Get-Content -LiteralPath $stderrPath -Raw -ErrorAction SilentlyContinue
            $sanitizedStderr = if ([string]::IsNullOrWhiteSpace($stderr)) {
                "<no stderr>"
            }
            else {
                $stderr `
                    -replace [regex]::Escape($AdminPassword), "<REDACTED>" `
                    -replace [regex]::Escape($UserPassword), "<REDACTED>" `
                    -replace '(?i)(bearer\s+)[^\s]+', '$1<REDACTED>' `
                    -replace '(?i)((password|token|secret)[\s:=]+)[^\s]+', '$1<REDACTED>'
            }
            throw "$Purpose failed with exit code $nativeExitCode. stderr: $sanitizedStderr"
        }
        return $output
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        Remove-Item -LiteralPath $stderrPath -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Keycloak([string[]]$Arguments) {
    $keycloakArguments = @(
        "exec", $container, "/opt/keycloak/bin/kcadm.sh"
    ) + $Arguments + @("--config", $kcadmConfig)
    Invoke-NativeDocker -Arguments $keycloakArguments -Purpose "Keycloak admin command"
}

function ConvertFrom-KeycloakUserJson([string]$Json) {
    if ([string]::IsNullOrWhiteSpace($Json)) {
        return [object[]]@()
    }

    $parsed = $Json | ConvertFrom-Json
    $users = New-Object System.Collections.ArrayList
    if ($parsed -is [System.Array]) {
        foreach ($user in $parsed) {
            [void]$users.Add($user)
        }
    }
    elseif ($null -ne $parsed) {
        [void]$users.Add($parsed)
    }

    return [object[]]$users.ToArray()
}

try {
    Write-Host "Authenticating Keycloak admin..."
    Invoke-Keycloak @(
        "config", "credentials",
        "--server", "http://localhost:8080",
        "--realm", "master",
        "--user", "admin",
        "--password", $AdminPassword
    )

$tenantExists = Invoke-NativeDocker -Arguments @(
    "exec", $databaseContainer, "psql", "-U", "autovision", "-d", "autovision",
    "-tAc", "SELECT 1 FROM public.tenants WHERE id = '$TenantId'"
) -Purpose "Tenant existence lookup"
if ([string]::IsNullOrWhiteSpace(($tenantExists -join "").Trim()) -or ($tenantExists -join "").Trim() -ne "1") {
    throw "AUTOVISION_LOCAL_DEV_TENANT_ID does not identify an existing tenant."
}

$escapedUsername = $Username.Replace("'", "''")
$escapedDisplayName = $displayName.Replace("'", "''")
$escapedEmail = $Email.Replace("'", "''")
# Use a dedicated development UserRefId; an existing UUID is updated by design.
$upsertSql = @"
INSERT INTO public.user_refs (id, tenant_id, external_user_id, display_name, email, is_active)
VALUES ('$UserRefId', '$TenantId', '$escapedUsername', '$escapedDisplayName', '$escapedEmail', TRUE)
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    external_user_id = EXCLUDED.external_user_id,
    display_name = EXCLUDED.display_name,
    email = EXCLUDED.email,
    is_active = TRUE;
"@
Invoke-NativeDocker -Arguments @(
    "exec", "-i", $databaseContainer, "psql", "-U", "autovision", "-d", "autovision",
    "-v", "ON_ERROR_STOP=1"
) -InputText $upsertSql -Purpose "Platform user-reference upsert" | Out-Null

$userJson = [ordered]@{
    username = $Username
    enabled = $true
    email = $Email
    emailVerified = $true
    firstName = "AutoVision"
    lastName = "Local User"
    attributes = @{
        autovision_user_ref_id = @($UserRefId)
    }
    credentials = @(
        [ordered]@{
            type = "password"
            value = $UserPassword
            temporary = $false
        }
    )
} | ConvertTo-Json -Depth 10

$existingJson = Invoke-NativeDocker -Arguments @(
    "exec", $container, "/opt/keycloak/bin/kcadm.sh", "get", "users",
    "--target-realm", $realm, "--query", "username=$Username", "--config", $kcadmConfig
) -Purpose "Keycloak user lookup"
$existingUsers = @(ConvertFrom-KeycloakUserJson (($existingJson -join "`n")))

if ($existingUsers.Count -eq 0) {
    Invoke-NativeDocker -Arguments @(
        "exec", "-i", $container, "/opt/keycloak/bin/kcadm.sh", "create", "users",
        "--target-realm", $realm, "--file", "-", "--config", $kcadmConfig
    ) -InputText $userJson -Purpose "Keycloak user create" | Out-Null
    Write-Host "Created local Keycloak user: $Username"
}
else {
    if ($existingUsers.Count -ne 1) {
        throw "More than one Keycloak user matched AUTOVISION_LOCAL_DEV_USERNAME."
    }
    $userId = [string]$existingUsers[0].id
    if ([string]::IsNullOrWhiteSpace($userId)) {
        throw "Existing Keycloak user has no usable ID; refusing to update."
    }
    $userResource = "users/$userId"
    Invoke-NativeDocker -Arguments @(
        "exec", "-i", $container, "/opt/keycloak/bin/kcadm.sh", "update", $userResource,
        "--target-realm", $realm, "--file", "-", "--config", $kcadmConfig
    ) -InputText $userJson -Purpose "Keycloak user update" | Out-Null
    Write-Host "Updated local Keycloak user: $Username"
}

Write-Host "LOCAL KEYCLOAK USER: PASS"
Write-Host "Platform user reference and Keycloak claim mapping are ready."
}
finally {
    Invoke-NativeDocker -Arguments @(
        "exec", $container, "rm", "-f", $kcadmConfig
    ) -Purpose "kcadm config cleanup" -SuppressFailure | Out-Null
}
