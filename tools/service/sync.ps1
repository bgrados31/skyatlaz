<#
.SYNOPSIS
Keeps the copy of the contract of the skyatlaz-service in sync with the service that is running.

.DESCRIPTION
The service publishes its own contract at /openapi.json. This script downloads it and writes it to
docs/api/skyatlaz-service.json, which is the copy the mod reads (and the one the test
ServiceContractTest compares against, so a change in the service that the mod does not know about fails
the build instead of failing in the game).

.EXAMPLE
.\tools\service\sync.ps1
.EXAMPLE
.\tools\service\sync.ps1 -Url http://127.0.0.1:9000
#>
param(
    [string]$Url = "http://127.0.0.1:8080",
    [string]$Out = "docs/api/skyatlaz-service.json"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$target = Join-Path $root $Out

try {
    $health = Invoke-RestMethod -Uri "$Url/health" -TimeoutSec 5
} catch {
    Write-Host "The service is not answering at $Url" -ForegroundColor Red
    Write-Host "Start it with:"
    Write-Host '  $env:HYPIXEL_API_KEY = "your-key"'
    Write-Host '  & "$env:USERPROFILE\.cargo\bin\cargo.exe" run --release --manifest-path ..\skyatlaz-service\Cargo.toml'
    exit 1
}

Write-Host "Service $($health.service) $($health.version) at $Url"

$contract = Invoke-RestMethod -Uri "$Url/openapi.json" -TimeoutSec 10
$json = $contract | ConvertTo-Json -Depth 20
[System.IO.File]::WriteAllText($target, $json, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Contract written to $Out"

foreach ($path in ($contract.paths.PSObject.Properties | Sort-Object Name)) {
    $methods = ($path.Value.PSObject.Properties.Name -join ", ")
    Write-Host ("  {0,-24} {1}" -f $path.Name, $methods)
}

$profile = $contract.components.schemas.SlimProfile.properties.PSObject.Properties.Name
Write-Host "Profile fields: $($profile -join ', ')"
$mining = $contract.components.schemas.MiningProfile.properties.PSObject.Properties.Name
Write-Host "Mining fields:  $($mining -join ', ')"
