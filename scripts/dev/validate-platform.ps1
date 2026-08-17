$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$platform = Join-Path $repoRoot "platform"
$logFile = Join-Path $env:TEMP "autovision-platform-test.log"

Push-Location $platform

try {
    cmd /c ".\mvnw.cmd -q test > `"$logFile`" 2>&1"
    $mavenExit = $LASTEXITCODE

    if ($mavenExit -ne 0) {
        Write-Host "PLATFORM VALIDATION: FAIL"
        Get-Content $logFile |
            Select-String -Pattern "\[ERROR\]|FAILURE|Failures:|Errors:|Exception|Caused by" |
            Select-Object -Last 30
        exit 1
    }

    $reports = Get-ChildItem ".\target\surefire-reports\*.txt" -ErrorAction SilentlyContinue

    $tests = 0
    $failures = 0
    $errors = 0
    $skipped = 0

    foreach ($report in $reports) {
        $line = Select-String -Path $report.FullName `
            -Pattern "Tests run:\s*(\d+), Failures:\s*(\d+), Errors:\s*(\d+), Skipped:\s*(\d+)" |
            Select-Object -First 1

        if ($line) {
            $tests += [int]$line.Matches[0].Groups[1].Value
            $failures += [int]$line.Matches[0].Groups[2].Value
            $errors += [int]$line.Matches[0].Groups[3].Value
            $skipped += [int]$line.Matches[0].Groups[4].Value
        }
    }

    Pop-Location
    Push-Location $repoRoot

    cmd /c "git diff --check >nul 2>&1"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "PLATFORM VALIDATION: FAIL"
        Write-Host "Diff     : FAIL"
        exit 1
    }

    Write-Host "PLATFORM VALIDATION: PASS"
    Write-Host "Tests    : $tests"
    Write-Host "Failures : $failures"
    Write-Host "Errors   : $errors"
    Write-Host "Skipped  : $skipped"
    Write-Host "Diff     : PASS"
}
finally {
    if ((Get-Location).Path -ne $repoRoot.Path) {
        Pop-Location
    }

    Remove-Item $logFile -ErrorAction SilentlyContinue
}
