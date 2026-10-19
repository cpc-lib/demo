$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
if (!(Test-Path $envFile)) { throw "Missing .env. See README.md" }
Get-Content $envFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') {
    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
  }
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET_BASE64)) { throw "JWT_SECRET_BASE64 is empty" }
Push-Location (Join-Path $root "backend")
try {
  mvn spring-boot:run
  if ($LASTEXITCODE -ne 0) { throw "Backend failed ($LASTEXITCODE)" }
} finally { Pop-Location }
