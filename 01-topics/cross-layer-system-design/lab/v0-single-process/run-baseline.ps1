param(
    [int]$Port = 18080,
    [int]$Requests = 1000,
    [int[]]$Concurrency = @(1, 8, 32),
    [int]$ServiceTimeMs = 5
)

$ErrorActionPreference = "Stop"
$labRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildRoot = Join-Path $labRoot "build"
$resultsRoot = Join-Path $labRoot "results"
$serverOut = Join-Path $resultsRoot "server-out.log"
$serverErr = Join-Path $resultsRoot "server-error.log"

New-Item -ItemType Directory -Path $resultsRoot -Force | Out-Null
& (Join-Path $labRoot "build.ps1")

$server = Start-Process -FilePath "java.exe" `
    -ArgumentList @(
        "-cp", $buildRoot,
        "dev.learn.systemdesign.v0.InventoryServer",
        "--port=$Port",
        "--stock=1000000",
        "--service-time-ms=$ServiceTimeMs",
        "--quiet=true"
    ) `
    -RedirectStandardOutput $serverOut `
    -RedirectStandardError $serverErr `
    -WindowStyle Hidden `
    -PassThru

try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$Port/health" -TimeoutSec 1
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
            Start-Sleep -Milliseconds 200
        }
    }
    if (-not $ready) {
        throw "V0 server did not become ready. Inspect $serverErr"
    }

    foreach ($level in $Concurrency) {
        $output = Join-Path $resultsRoot "baseline-c$level.json"
        & java.exe -cp $buildRoot dev.learn.systemdesign.v0.LoadGenerator `
            "--base-url=http://127.0.0.1:$Port" `
            "--requests=$Requests" `
            "--concurrency=$level" `
            "--reset-stock=1000000" `
            "--output=$output"
        if ($LASTEXITCODE -ne 0) {
            throw "load generator failed at concurrency $level"
        }
    }
} finally {
    if ($server -and -not $server.HasExited) {
        Stop-Process -Id $server.Id
        $server.WaitForExit()
    }
}

Write-Output "Baseline results written to $resultsRoot"

