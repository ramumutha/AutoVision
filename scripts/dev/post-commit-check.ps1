$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path "$PSScriptRoot\..\.."
Push-Location $RepoRoot

try {
    Write-Host "`n=== POST-COMMIT STATUS ==="
    git status

    Write-Host "`n=== HEAD COMMIT ==="
    git show --stat --oneline HEAD

    Write-Host "`n=== RECENT HISTORY ==="
    git log -10 --oneline --decorate
}
finally {
    Pop-Location
}
