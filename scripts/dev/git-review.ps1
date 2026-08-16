$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path "$PSScriptRoot\..\.."
Push-Location $RepoRoot

try {
    Write-Host "`n=== BRANCH ==="
    git branch --show-current

    Write-Host "`n=== STATUS ==="
    git status --short

    Write-Host "`n=== DIFF CHECK ==="
    git diff --check

    Write-Host "`n=== DIFF STAT ==="
    git diff --stat

    Write-Host "`n=== CHANGED FILES ==="
    git diff --name-only

    Write-Host "`n=== UNTRACKED FILES ==="
    git ls-files --others --exclude-standard
}
finally {
    Pop-Location
}
