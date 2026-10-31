[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^(?:\d{1,3}\.){3}\d{1,3}$')]
    [string]$LanIp
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root '.cert'
$cert = Join-Path $dir 'scan-login.pem'
$key = Join-Path $dir 'scan-login-key.pem'

$mkcert = Get-Command mkcert -ErrorAction SilentlyContinue
if (-not $mkcert) {
    throw 'mkcert was not found. Install mkcert and run "mkcert -install" manually on the PC, then retry. Your phone must also trust the local CA certificate.'
}

# Certificate generation does not silently install trust roots on this PC or any phone.
New-Item -ItemType Directory -Force -Path $dir | Out-Null
& $mkcert.Source -cert-file $cert -key-file $key localhost 127.0.0.1 $LanIp
if ($LASTEXITCODE -ne 0) { throw "mkcert failed with exit code $LASTEXITCODE" }

Write-Host "Certificate generated: $cert"
Write-Host "Private key: $key"
Write-Host "Next: make your phone trust the mkcert CA, then run:"
Write-Host "  .\scripts\start-camera-https.ps1"
Write-Host "Mobile URL: https://$($LanIp):5173/"
Write-Host "The backend SCAN_URL may use this direct Mobile URL, or the PC reverse-proxy /mobile URL when both apps run."
