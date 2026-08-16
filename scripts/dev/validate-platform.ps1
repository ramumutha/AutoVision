$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path "$PSScriptRoot\..\.."
$Platform = Join-Path $RepoRoot "platform"

Write-Host "`n=== PLATFORM VALIDATION ==="
Push-Location $Platform

try {
    & .\mvnw.cmd -q test
    if ($LASTEXITCODE -ne 0) {
        throw "Platform tests failed with exit code $LASTEXITCODE"
    }

    $reports = Get-ChildItem "target\surefire-reports\*.txt" -ErrorAction SilentlyContinue
    $summary = $reports | Select-String -Pattern "Tests run:"

    Write-Host "`n=== TEST RESULTS ==="
    $summary

    Pop-Location

    Push-Location $RepoRoot

    Write-Host "`n=== DIFF CHECK ==="
    git diff --check
    if ($LASTEXITCODE -ne 0) {
        throw "git diff --check failed"
    }

    Write-Host "`nVALIDATION PASS"
}
finally {
    Pop-Location -ErrorAction SilentlyContinue
}
