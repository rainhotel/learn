param(
    [switch]$SkipPull
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
$env:DOCKER_CONFIG = Join-Path $labRoot '.docker'
$composeFile = Join-Path $labRoot 'compose.yaml'

docker version --format '{{.Server.Version}}' | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Engine is not reachable. Start Docker Desktop and retry.'
}

if (-not $SkipPull) {
    docker compose -f $composeFile pull kafka
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to pull apache/kafka:4.3.1.'
    }
}

docker compose -f $composeFile up -d kafka
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to start the Kafka container.'
}

$ready = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    $containerId = docker compose -f $composeFile ps -q kafka
    if (-not [string]::IsNullOrWhiteSpace($containerId)) {
        docker exec $containerId /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
    }

    Start-Sleep -Seconds 2
}

if (-not $ready) {
    docker compose -f $composeFile logs --tail 100 kafka
    throw 'Kafka did not become ready within 60 seconds.'
}

Write-Host 'KAFKA_LAB_STARTED'
Write-Host 'BOOTSTRAP_SERVER=localhost:9092'
