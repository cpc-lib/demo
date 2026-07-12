$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
Write-Warning "This removes the MySQL and PowerJob data volumes."
docker compose down -v --remove-orphans
docker compose up -d --force-recreate
docker compose ps
