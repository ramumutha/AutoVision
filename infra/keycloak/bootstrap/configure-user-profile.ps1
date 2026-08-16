param(
    [Parameter(Mandatory = $true)]
    [string]$AdminPassword
)

$ErrorActionPreference = "Stop"

$container = "autovision-keycloak"
$realm = "autovision"
$attributeName = "autovision_user_ref_id"

Write-Host "Authenticating Keycloak admin..."

docker exec $container `
    /opt/keycloak/bin/kcadm.sh `
    config credentials `
    --server http://localhost:8080 `
    --realm master `
    --user admin `
    --password "$AdminPassword"

if ($LASTEXITCODE -ne 0) {
    throw "Keycloak admin authentication failed."
}

$profileJson = docker exec $container `
    /opt/keycloak/bin/kcadm.sh `
    get users/profile `
    -r $realm

if ($LASTEXITCODE -ne 0) {
    throw "Unable to read Keycloak User Profile."
}

$profile = $profileJson | ConvertFrom-Json

$existing = $profile.attributes |
    Where-Object { $_.name -eq $attributeName }

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

    $updatedProfileJson = $profile |
        ConvertTo-Json -Depth 20

    $updatedProfileJson |
        docker exec -i $container `
            /opt/keycloak/bin/kcadm.sh `
            update users/profile `
            -r $realm `
            -f -

    if ($LASTEXITCODE -ne 0) {
        throw "Unable to update Keycloak User Profile."
    }

    Write-Host "Created managed attribute: $attributeName"
}

Write-Host "KEYCLOAK USER PROFILE: PASS"
