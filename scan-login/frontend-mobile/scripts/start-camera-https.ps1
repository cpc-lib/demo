$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$env:SCAN_HTTPS_CERT = Join-Path $root '.cert\scan-login.pem'
$env:SCAN_HTTPS_KEY = Join-Path $root '.cert\scan-login-key.pem'
if (!(Test-Path -LiteralPath $env:SCAN_HTTPS_CERT) -or !(Test-Path -LiteralPath $env:SCAN_HTTPS_KEY)) {
    throw 'HTTPS certificate missing. First run .\scripts\setup-camera-https.ps1 -LanIp YOUR_LAN_IP'
}
Push-Location $root
try {
    npm run dev
    if ($LASTEXITCODE -ne 0) { throw "npm run dev failed ($LASTEXITCODE)" }
} finally {
    Pop-Location
}
