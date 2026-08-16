$ErrorActionPreference = "Stop"

$repoRoot = "D:\Projects\AutoVision"

if (-not (Test-Path (Join-Path $repoRoot ".git"))) {
    throw "AutoVision Git repository not found at $repoRoot"
}

Set-Location $repoRoot

Write-Host ""
Write-Host "=== GIT REVIEW ==="

$branch = (git branch --show-current).Trim()

$statusLines = @(
    git status --short
)

$modified = @(
    $statusLines |
    Where-Object { $_ -notmatch '^\?\?' }
)

$untracked = @(
    $statusLines |
    Where-Object { $_ -match '^\?\?' }
)

# Suppress harmless LF/CRLF warnings while retaining the
# actual git diff --check result.
$diffCheckOutput = @(
    cmd.exe /c "git diff --check 2>nul"
)

$diffExit = $LASTEXITCODE

Write-Host "Branch    : $branch"
Write-Host "Modified  : $($modified.Count)"
Write-Host "Untracked : $($untracked.Count)"
Write-Host ("Diff      : " + $(if ($diffExit -eq 0) { "PASS" } else { "FAIL" }))

if ($modified.Count -gt 0) {

    Write-Host ""
    Write-Host "Changed files:"

    $changedFiles = @(
        cmd.exe /c "git diff --name-only 2>nul"
    )

    foreach ($file in $changedFiles) {
        if ($file) {
            Write-Host "  $file"
        }
    }

    $stat = @(
        cmd.exe /c "git diff --shortstat 2>nul"
    )

    if ($stat.Count -gt 0) {
        Write-Host ""
        Write-Host "Change summary:"

        foreach ($line in $stat) {
            if ($line) {
                Write-Host "  $line"
            }
        }
    }
}

if ($untracked.Count -gt 0) {

    Write-Host ""
    Write-Host "Untracked files:"

    foreach ($line in $untracked) {
        Write-Host "  $($line.Substring(3))"
    }
}

if ($diffExit -ne 0) {

    Write-Host ""
    Write-Host "Diff-check errors:"

    foreach ($line in $diffCheckOutput) {
        Write-Host "  $line"
    }
}

if (
    $modified.Count -eq 0 -and
    $untracked.Count -eq 0 -and
    $diffExit -eq 0
) {
    Write-Host ""
    Write-Host "WORKING TREE CLEAN"
}