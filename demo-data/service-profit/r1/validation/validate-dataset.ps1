param(
    [string]$DatasetRoot = (
        Join-Path $PSScriptRoot ".."
    )
)

$ErrorActionPreference = "Stop"

Write-Host "`n=== AUTOVISION SERVICE PROFIT R1 DATASET VALIDATION ==="

$manifestPath = Join-Path $DatasetRoot "manifest.json"
$sourceRoot = Join-Path $DatasetRoot "source"

$requiredFiles = @(
    "organization.csv",
    "customers.csv",
    "vehicles.csv",
    "repair_orders.csv",
    "service_jobs.csv",
    "recommendations.csv",
    "advisor_notes.csv",
    "invoices.csv",
    "invoice_lines.csv",
    "costs.csv",
    "service_history.csv"
)

$pass = $true

if (-not (Test-Path $manifestPath)) {
    Write-Host "FAIL: manifest.json missing"
    return
}

$manifest =
    Get-Content $manifestPath -Raw |
    ConvertFrom-Json

Write-Host ("Dataset : {0}" -f $manifest.datasetId)
Write-Host ("Version : {0}" -f $manifest.version)
Write-Host ("Market  : {0}" -f $manifest.market)

foreach ($file in $requiredFiles) {

    $path = Join-Path $sourceRoot $file

    if (-not (Test-Path $path)) {
        Write-Host ("FAIL: missing {0}" -f $file)
        $pass = $false
    }
}

if (-not $pass) {
    Write-Host "`nDATASET VALIDATION: FAIL"
    return
}

$organization =
    Import-Csv (Join-Path $sourceRoot "organization.csv")

$customers =
    Import-Csv (Join-Path $sourceRoot "customers.csv")

$vehicles =
    Import-Csv (Join-Path $sourceRoot "vehicles.csv")

$repairOrders =
    Import-Csv (Join-Path $sourceRoot "repair_orders.csv")

$jobs =
    Import-Csv (Join-Path $sourceRoot "service_jobs.csv")

$recommendations =
    Import-Csv (Join-Path $sourceRoot "recommendations.csv")

$notes =
    Import-Csv (Join-Path $sourceRoot "advisor_notes.csv")

$invoices =
    Import-Csv (Join-Path $sourceRoot "invoices.csv")

$invoiceLines =
    Import-Csv (Join-Path $sourceRoot "invoice_lines.csv")

$costs =
    Import-Csv (Join-Path $sourceRoot "costs.csv")

$serviceHistory =
    Import-Csv (Join-Path $sourceRoot "service_history.csv")

Write-Host "`n--- RECORD COUNTS ---"

Write-Host ("Organization    : {0}" -f $organization.Count)
Write-Host ("Customers       : {0}" -f $customers.Count)
Write-Host ("Vehicles        : {0}" -f $vehicles.Count)
Write-Host ("Repair Orders   : {0}" -f $repairOrders.Count)
Write-Host ("Service Jobs    : {0}" -f $jobs.Count)
Write-Host ("Recommendations : {0}" -f $recommendations.Count)
Write-Host ("Notes           : {0}" -f $notes.Count)
Write-Host ("Invoices        : {0}" -f $invoices.Count)
Write-Host ("Invoice Lines   : {0}" -f $invoiceLines.Count)
Write-Host ("Costs           : {0}" -f $costs.Count)
Write-Host ("Service History : {0}" -f $serviceHistory.Count)

Write-Host "`n--- SCENARIO DEFINITIONS ---"

$scenarioIds = @(
    $manifest.scenarios |
        ForEach-Object { $_.id }
)

$duplicateScenarios =
    $scenarioIds |
    Group-Object |
    Where-Object { $_.Count -gt 1 }

if ($duplicateScenarios) {

    Write-Host "FAIL: duplicate manifest scenarios"

    $duplicateScenarios.Name |
        ForEach-Object {
            Write-Host "  $_"
        }

    $pass = $false
}
else {
    Write-Host "PASS: manifest scenario IDs unique"
}

$implementedScenarios = @(
    $repairOrders.scenario_id
    $jobs.scenario_id
    $recommendations.scenario_id
    $notes.scenario_id
    $invoices.scenario_id
    $invoiceLines.scenario_id
    $costs.scenario_id
    $serviceHistory.scenario_id
) |
    Where-Object { $_ } |
    Sort-Object -Unique

foreach ($scenario in $manifest.scenarios) {

    if ($implementedScenarios -contains $scenario.id) {

        Write-Host (
            "SOURCE_COMPLETE: {0} - {1}" -f
            $scenario.id,
            $scenario.name
        )
    }
    else {

        Write-Host (
            "FUTURE_SOURCE_PENDING: {0} - {1}" -f
            $scenario.id,
            $scenario.name
        )
    }
}

Write-Host "`n--- REFERENTIAL CHECKS ---"

$customerIds = @($customers.customer_id)
$vehicleIds = @($vehicles.vehicle_id)
$repairOrderIds = @($repairOrders.repair_order_id)
$jobIds = @($jobs.service_job_id)
$invoiceIds = @($invoices.invoice_id)
$invoiceLineIds = @($invoiceLines.invoice_line_id)

foreach ($vehicle in $vehicles) {

    if (
        $vehicle.customer_id -and
        $vehicle.customer_id -notin $customerIds
    ) {
        Write-Host (
            "FAIL: vehicle customer missing {0}" -f
            $vehicle.vehicle_id
        )

        $pass = $false
    }
}

foreach ($ro in $repairOrders) {

    if (
        $ro.customer_id -and
        $ro.customer_id -notin $customerIds
    ) {
        Write-Host (
            "FAIL: RO customer missing {0}" -f
            $ro.repair_order_id
        )

        $pass = $false
    }

    if ($ro.vehicle_id -notin $vehicleIds) {

        Write-Host (
            "FAIL: RO vehicle missing {0}" -f
            $ro.repair_order_id
        )

        $pass = $false
    }
}

foreach ($job in $jobs) {

    if ($job.repair_order_id -notin $repairOrderIds) {

        Write-Host (
            "FAIL: job RO missing {0}" -f
            $job.service_job_id
        )

        $pass = $false
    }
}

foreach ($recommendation in $recommendations) {

    if ($recommendation.service_job_id -notin $jobIds) {

        Write-Host (
            "FAIL: recommendation job missing {0}" -f
            $recommendation.recommendation_id
        )

        $pass = $false
    }
}

foreach ($note in $notes) {

    if (
        $note.repair_order_id -and
        $note.repair_order_id -notin $repairOrderIds
    ) {
        Write-Host (
            "FAIL: note RO missing {0}" -f
            $note.note_id
        )

        $pass = $false
    }

    if (
        $note.service_job_id -and
        $note.service_job_id -notin $jobIds
    ) {
        Write-Host (
            "FAIL: note job missing {0}" -f
            $note.note_id
        )

        $pass = $false
    }
}

foreach ($invoice in $invoices) {

    if (
        $invoice.customer_id -and
        $invoice.customer_id -notin $customerIds
    ) {
        Write-Host (
            "FAIL: invoice customer missing {0}" -f
            $invoice.invoice_id
        )

        $pass = $false
    }

    if (
        $invoice.vehicle_id -and
        $invoice.vehicle_id -notin $vehicleIds
    ) {
        Write-Host (
            "FAIL: invoice vehicle missing {0}" -f
            $invoice.invoice_id
        )

        $pass = $false
    }
}

foreach ($line in $invoiceLines) {

    if ($line.invoice_id -notin $invoiceIds) {

        Write-Host (
            "FAIL: invoice line parent missing {0}" -f
            $line.invoice_line_id
        )

        $pass = $false
    }
}

foreach ($cost in $costs) {

    if ($cost.invoice_line_id -notin $invoiceLineIds) {

        Write-Host (
            "FAIL: cost invoice line missing {0}" -f
            $cost.cost_id
        )

        $pass = $false
    }
}

foreach ($history in $serviceHistory) {

    if (
        $history.customer_id -and
        $history.customer_id -notin $customerIds
    ) {
        Write-Host (
            "FAIL: service history customer missing {0}" -f
            $history.service_history_id
        )

        $pass = $false
    }

    if (
        $history.vehicle_id -and
        $history.vehicle_id -notin $vehicleIds
    ) {
        Write-Host (
            "FAIL: service history vehicle missing {0}" -f
            $history.service_history_id
        )

        $pass = $false
    }
}

Write-Host "`n--- SCENARIO COVERAGE ---"

$pendingScenarios = @(
    $manifest.scenarios |
        Where-Object {
            $_.id -notin $implementedScenarios
        }
)

if ($pendingScenarios.Count -eq 0) {
    Write-Host "PASS: all manifest scenarios have source evidence"
}
else {
    Write-Host (
        "REVIEW: {0} scenario(s) still lack source evidence" -f
        $pendingScenarios.Count
    )

    foreach ($scenario in $pendingScenarios) {
        Write-Host (
            "  {0} - {1}" -f
            $scenario.id,
            $scenario.name
        )
    }

    $pass = $false
}

if ($pass) {

    Write-Host "`nPASS: referential checks"
    Write-Host "PASS: scenario source completeness"
    Write-Host "`nR1.3.2 DATASET CONTRACT: PASS"
}
else {

    Write-Host "`nR1.3.2 DATASET CONTRACT: FAIL"
}
