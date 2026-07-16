param(
    [switch]$RemoveData
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
$env:DOCKER_CONFIG = Join-Path $labRoot '.docker'
$composeFile = Join-Path $labRoot 'compose.yaml'

if ($RemoveData) {
    docker compose -f $composeFile down --volumes
} else {
    docker compose -f $composeFile down
}

if ($LASTEXITCODE -ne 0) {
    throw 'Failed to stop the Kafka lab.'
}

Write-Host "KAFKA_LAB_STOPPED RemoveData=$RemoveData"
