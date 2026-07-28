param(
    [int]$Port = 8080,
    [int]$Stock = 100000,
    [int]$ServiceTimeMs = 5,
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"
$labRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $labRoot "build.ps1")
$quietValue = $Quiet.IsPresent.ToString().ToLowerInvariant()

& java.exe -cp (Join-Path $labRoot "build") dev.learn.systemdesign.v0.InventoryServer `
    "--port=$Port" `
    "--stock=$Stock" `
    "--service-time-ms=$ServiceTimeMs" `
    "--quiet=$quietValue"

