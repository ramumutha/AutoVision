$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path "$PSScriptRoot\..\.."
Push-Location $RepoRoot

try {
    Write-Host "`n=== STAGED STATUS ==="
    git status

    Write-Host "`n=== STAGED DIFF CHECK ==="
    git diff --cached --check

    Write-Host "`n=== STAGED STAT ==="
    git diff --cached --stat

    Write-Host "`n=== STAGED FILES ==="
    git diff --cached --name-only

    Write-Host "`n=== UNSTAGED FILES ==="
    git diff --name-only

    Write-Host "`n=== UNTRACKED FILES ==="
    git ls-files --others --exclude-standard
}
finally {
    Pop-Location
}
