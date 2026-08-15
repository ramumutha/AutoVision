$ErrorActionPreference = "Stop"

Write-Host "===== AUTOVISION PLATFORM DB BOUNDARY ====="

Write-Host "`n[1/5] PostgreSQL container"
docker compose ps postgres
if ($LASTEXITCODE -ne 0) {
    throw "PostgreSQL container check failed."
}

Write-Host "`n[2/5] Required schemas"
docker exec autovision-postgres `
    psql -U autovision -d autovision `
    -v ON_ERROR_STOP=1 `
    -c "SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name IN ('public','platform')
        ORDER BY schema_name;"
if ($LASTEXITCODE -ne 0) {
    throw "Schema verification failed."
}

Write-Host "`n[3/5] Flyway ownership"
docker exec autovision-postgres `
    psql -U autovision -d autovision `
    -v ON_ERROR_STOP=1 `
    -c "SELECT installed_rank, version, description, success
        FROM platform.flyway_schema_history
        ORDER BY installed_rank;"
if ($LASTEXITCODE -ne 0) {
    throw "Flyway history verification failed."
}

Write-Host "`n[4/5] Alembic ownership"
docker exec autovision-postgres `
    psql -U autovision -d autovision `
    -v ON_ERROR_STOP=1 `
    -c "SELECT version_num FROM public.alembic_version;"
if ($LASTEXITCODE -ne 0) {
    throw "Alembic verification failed."
}

Write-Host "`n[5/5] Canonical tenant table"
docker exec autovision-postgres `
    psql -U autovision -d autovision `
    -v ON_ERROR_STOP=1 `
    -c "SELECT COUNT(*) AS tenant_count FROM public.tenants;"
if ($LASTEXITCODE -ne 0) {
    throw "Canonical tenant verification failed."
}

Write-Host "`nPLATFORM DB BOUNDARY: PASS"
