$ErrorActionPreference = "Stop"

$repoRoot = "D:\Projects\AutoVision"

if (-not (Test-Path (Join-Path $repoRoot ".git"))) {
    throw "AutoVision Git repository not found at $repoRoot"
}

Set-Location $repoRoot

Write-Host ""
Write-Host "=== STAGED REVIEW ==="

$branch = (git branch --show-current).Trim()

$stagedFiles = @(
    git diff --cached --name-only
)

$unstagedFiles = @(
    cmd.exe /c "git diff --name-only 2>nul"
)

$untrackedFiles = @(
    git ls-files --others --exclude-standard
)

$diffCheckOutput = @(
    cmd.exe /c "git diff --cached --check 2>nul"
)

$diffExit = $LASTEXITCODE

Write-Host "Branch    : $branch"
Write-Host "Staged    : $($stagedFiles.Count)"
Write-Host "Unstaged  : $($unstagedFiles.Count)"
Write-Host "Untracked : $($untrackedFiles.Count)"
Write-Host ("Diff      : " + $(if ($diffExit -eq 0) { "PASS" } else { "FAIL" }))

if ($stagedFiles.Count -gt 0) {

    Write-Host ""
    Write-Host "Staged files:"

    foreach ($file in $stagedFiles) {
        if ($file) {
            Write-Host "  $file"
        }
    }

    $stat = @(
        cmd.exe /c "git diff --cached --shortstat 2>nul"
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

if ($unstagedFiles.Count -gt 0) {

    Write-Host ""
    Write-Host "WARNING - Unstaged files:"

    foreach ($file in $unstagedFiles) {
        if ($file) {
            Write-Host "  $file"
        }
    }
}

if ($untrackedFiles.Count -gt 0) {

    Write-Host ""
    Write-Host "WARNING - Untracked files:"

    foreach ($file in $untrackedFiles) {
        if ($file) {
            Write-Host "  $file"
        }
    }
}

if ($diffExit -ne 0) {

    Write-Host ""
    Write-Host "STAGED DIFF CHECK FAILED:"

    foreach ($line in $diffCheckOutput) {
        Write-Host "  $line"
    }

    exit 1
}

if (
    $stagedFiles.Count -eq 0 -and
    $unstagedFiles.Count -eq 0 -and
    $untrackedFiles.Count -eq 0
) {
    Write-Host ""
    Write-Host "NOTHING TO REVIEW"
}