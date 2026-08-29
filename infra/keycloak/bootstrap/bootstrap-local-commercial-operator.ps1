param(
    [string]$Username = $env:AUTOVISION_LOCAL_OPERATOR_USERNAME,
    [string]$UserRefId = $env:AUTOVISION_LOCAL_OPERATOR_USER_REF_ID,
    [string]$TenantId = $env:AUTOVISION_LOCAL_OPERATOR_TENANT_ID,
    [string]$Email = $env:AUTOVISION_LOCAL_OPERATOR_EMAIL
)

$ErrorActionPreference = "Stop"
$AdminPassword = $env:KEYCLOAK_ADMIN_PASSWORD
$UserPassword = $env:AUTOVISION_LOCAL_OPERATOR_PASSWORD

function Assert-LocalValue([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "$Name is required. Set it in ignored .env or pass it to the script."
    }
}

Assert-LocalValue "KEYCLOAK_ADMIN_PASSWORD" $AdminPassword
Assert-LocalValue "AUTOVISION_LOCAL_OPERATOR_PASSWORD" $UserPassword
Assert-LocalValue "AUTOVISION_LOCAL_OPERATOR_USERNAME" $Username
Assert-LocalValue "AUTOVISION_LOCAL_OPERATOR_USER_REF_ID" $UserRefId
Assert-LocalValue "AUTOVISION_LOCAL_OPERATOR_TENANT_ID" $TenantId

& "$PSScriptRoot\configure-user-profile.ps1" -AdminPassword $AdminPassword | Out-Null

$parsedUserRefId = [guid]::Empty
$parsedTenantId = [guid]::Empty
if (-not [guid]::TryParse($UserRefId, [ref]$parsedUserRefId)) {
    throw "AUTOVISION_LOCAL_OPERATOR_USER_REF_ID must be a UUID."
}
if (-not [guid]::TryParse($TenantId, [ref]$parsedTenantId)) {
    throw "AUTOVISION_LOCAL_OPERATOR_TENANT_ID must be a UUID."
}

if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = "$Username@local.autovision"
}

$container = "autovision-keycloak"
$databaseContainer = "autovision-postgres"
$realm = "autovision"
$issuer = "http://localhost:8081/realms/autovision"
$operatorRoleId = "4a2f94ad-56b6-4f97-92cc-0fcf4a4f2c02"
$kcadmConfig = "/tmp/autovision-kcadm-$([guid]::NewGuid().ToString('N')).config"

function Invoke-NativeDocker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [string]$InputText,
        [string]$Purpose = "Native Docker command"
    )

    $stderrPath = [System.IO.Path]::GetTempFileName()
    try {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        if ($null -eq $InputText) {
            $output = & docker @Arguments 2> $stderrPath
        }
        else {
            $output = $InputText | & docker @Arguments 2> $stderrPath
        }
        $nativeExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference
        if ($nativeExitCode -ne 0) {
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
        Remove-Item -LiteralPath $stderrPath -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Keycloak {
    param(
        [string[]]$Arguments,
        [string]$InputText
    )

    $keycloakArguments = @(
        "exec", $container, "/opt/keycloak/bin/kcadm.sh"
    ) + $Arguments + @("--config", $kcadmConfig)
    Invoke-NativeDocker -Arguments $keycloakArguments -InputText $InputText -Purpose "Keycloak admin command"
}

function Invoke-KeycloakWithInput {
    param(
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$InputText,
        [string]$Purpose = "Keycloak admin command"
    )

    $keycloakArguments = @(
        "exec", "-i", $container, "/opt/keycloak/bin/kcadm.sh"
    ) + $Arguments + @("--config", $kcadmConfig)
    Invoke-NativeDocker -Arguments $keycloakArguments -InputText $InputText -Purpose $Purpose
}

function ConvertFrom-KeycloakUserJson([string]$Json) {
    if ([string]::IsNullOrWhiteSpace($Json)) {
        return [object[]]@()
    }

    $parsed = $Json | ConvertFrom-Json
    if ($parsed -is [System.Array]) {
        return [object[]]$parsed
    }
    return [object[]]@($parsed)
}

try {
    Invoke-Keycloak @(
        "config", "credentials", "--server", "http://localhost:8080",
        "--realm", "master", "--user", "admin", "--password", $AdminPassword
    ) | Out-Null

    $escapedUsername = $Username.Replace("'", "''")
    $escapedDisplayName = "VERSPEN Local Commercial Operator".Replace("'", "''")
    $escapedEmail = $Email.Replace("'", "''")
    $upsertUserRefSql = @"
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
    ) -InputText $upsertUserRefSql -Purpose "Operator user-reference upsert" | Out-Null

    $userJson = [ordered]@{
        username = $Username
        enabled = $true
        email = $Email
        emailVerified = $true
        firstName = "VERSPEN"
        lastName = "Commercial Operator"
        attributes = @{ autovision_user_ref_id = @($UserRefId) }
    } | ConvertTo-Json -Depth 10

    $existingJson = Invoke-Keycloak @(
        "get", "users", "--target-realm", $realm, "--query", "username=$Username"
    )
    $existingUsers = @(ConvertFrom-KeycloakUserJson ($existingJson -join "`n"))
    if ($existingUsers.Count -eq 0) {
        Invoke-KeycloakWithInput @(
            "create", "users", "--target-realm", $realm, "--file", "-"
        ) -InputText $userJson -Purpose "Keycloak operator create" | Out-Null
        $existingJson = Invoke-Keycloak @(
            "get", "users", "--target-realm", $realm, "--query", "username=$Username"
        )
        $existingUsers = @(ConvertFrom-KeycloakUserJson ($existingJson -join "`n"))
    }
    if ($existingUsers.Count -ne 1 -or [string]::IsNullOrWhiteSpace([string]$existingUsers[0].id)) {
        throw "Exactly one Keycloak operator user must exist after bootstrap."
    }

    $userId = [string]$existingUsers[0].id
    Invoke-KeycloakWithInput @(
        "update", "users/$userId", "--target-realm", $realm, "--file", "-"
    ) -InputText $userJson -Purpose "Keycloak operator update" | Out-Null
    Invoke-Keycloak @(
        "set-password", "--target-realm", $realm, "--userid", $userId,
        "--new-password", $UserPassword
    ) -Purpose "Keycloak operator password synchronization" | Out-Null

    $escapedSubject = $userId.Replace("'", "''")
    $escapedIssuer = $issuer.Replace("'", "''")
    $principalSql = @"
WITH principal AS (
    INSERT INTO platform.authorization_principals
        (id, principal_type, user_ref_id, tenant_id, issuer, subject, display_name, status)
    VALUES
        (gen_random_uuid(), 'HUMAN', '$UserRefId', '$TenantId', '$escapedIssuer', '$escapedSubject', '$escapedDisplayName', 'ACTIVE')
    ON CONFLICT (issuer, subject) DO UPDATE SET
        user_ref_id = EXCLUDED.user_ref_id,
        tenant_id = EXCLUDED.tenant_id,
        display_name = EXCLUDED.display_name,
        status = 'ACTIVE'
    RETURNING id
)
INSERT INTO platform.scoped_role_assignments
    (id, principal_id, role_id, scope_type, is_active)
SELECT gen_random_uuid(), id, '$operatorRoleId', 'SYSTEM', TRUE
FROM principal
ON CONFLICT (principal_id, role_id) WHERE scope_type = 'SYSTEM'
DO UPDATE SET is_active = TRUE, valid_from = NULL, valid_until = NULL;
"@
    Invoke-NativeDocker -Arguments @(
        "exec", "-i", $databaseContainer, "psql", "-U", "autovision", "-d", "autovision",
        "-v", "ON_ERROR_STOP=1"
    ) -InputText $principalSql -Purpose "Operator authorization assignment" | Out-Null

    Write-Host "LOCAL VERSPEN COMMERCIAL OPERATOR: PASS"
}
finally {
    Invoke-NativeDocker -Arguments @(
        "exec", $container, "rm", "-f", $kcadmConfig
    ) -Purpose "kcadm config cleanup" | Out-Null
}