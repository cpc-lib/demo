[CmdletBinding()]
param(
  [ValidateSet('pc','mobile','both')]
  [string]$App = 'both'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if ($App -eq 'both') {
  foreach ($target in @('mobile','pc')) {
    $arguments = @('-NoProfile','-ExecutionPolicy','Bypass','-NoExit','-File',
      ('"' + $PSCommandPath + '"'),'-App',$target)
    Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -ErrorAction Stop | Out-Null
    Write-Output ("Started independent " + $target + " terminal")
  }
  Write-Output 'PC: http://localhost:5174/ | Mobile: http://localhost:5173/'
  return
}

$directory = Join-Path $root $(if ($App -eq 'pc') {'frontend-pc'} else {'frontend-mobile'})
if (!(Test-Path -LiteralPath (Join-Path $directory 'package.json'))) {
  throw "Missing independent frontend project: $directory"
}
Push-Location $directory
try {
  if (!(Test-Path -LiteralPath 'node_modules/vite/package.json')) {
    npm ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
  }
  npm run dev
  if ($LASTEXITCODE -ne 0) { throw "Could not start $App frontend" }
} finally {
  Pop-Location
}
