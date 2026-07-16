param(
    [switch]$StaticOnly
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
$env:DOCKER_CONFIG = Join-Path $labRoot '.docker'
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string]$Message)
    $failures.Add($Message)
}

function Assert-FileExists {
    param([string]$RelativePath)
    $path = Join-Path $labRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        Add-Failure "missing file: $RelativePath"
    }
}

function Assert-Contains {
    param(
        [string]$RelativePath,
        [string]$Pattern,
        [string]$Description
    )

    $path = Join-Path $labRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        return
    }

    $content = Get-Content -Raw -LiteralPath $path
    if ($content -notmatch $Pattern) {
        Add-Failure "$RelativePath does not declare $Description"
    }
}

function Invoke-DockerProbe {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & docker version --format '{{.Server.Version}}' 2>&1
        return [PSCustomObject]@{
            ExitCode = $LASTEXITCODE
            Output   = @($output)
        }
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

$requiredFiles = @(
    '.docker\config.json',
    'compose.yaml',
    'config\producer.properties',
    'config\consumer.properties',
    'scripts\start-lab.ps1',
    'scripts\stop-lab.ps1',
    'scripts\kafka-command.ps1',
    'experiments\01-partition-order.ps1',
    'experiments\02-offset-and-lag.ps1',
    'evidence\README.md'
)

foreach ($file in $requiredFiles) {
    Assert-FileExists $file
}

Assert-Contains 'compose.yaml' 'apache/kafka:4\.3\.1' 'the pinned Kafka 4.3.1 image'
Assert-Contains 'compose.yaml' '9092:9092' 'the host Kafka listener'
Assert-Contains 'config\producer.properties' 'acks=all' 'acks=all'
Assert-Contains 'config\producer.properties' 'enable\.idempotence=true' 'producer idempotence'
Assert-Contains 'config\consumer.properties' 'enable\.auto\.commit=false' 'manual offset management'
Assert-Contains 'scripts\verify-lab.ps1' 'DOCKER_CONFIG' 'an isolated Docker CLI config'
Assert-Contains 'scripts\verify-lab.ps1' 'function\s+Invoke-DockerProbe' 'controlled native Docker error handling'
Assert-Contains 'scripts\start-lab.ps1' 'DOCKER_CONFIG' 'an isolated Docker CLI config'
Assert-Contains 'scripts\stop-lab.ps1' 'DOCKER_CONFIG' 'an isolated Docker CLI config'
Assert-Contains 'scripts\kafka-command.ps1' 'DOCKER_CONFIG' 'an isolated Docker CLI config'
Assert-Contains 'experiments\01-partition-order.ps1' 'DOCKER_CONFIG' 'an isolated Docker CLI config'
Assert-Contains 'experiments\02-offset-and-lag.ps1' 'DOCKER_CONFIG' 'an isolated Docker CLI config'

if ($failures.Count -gt 0) {
    Write-Host 'STATIC_CHECKS_FAILED'
    $failures | ForEach-Object { Write-Host "- $_" }
    exit 1
}

Write-Host 'STATIC_CHECKS_PASSED'

if ($StaticOnly) {
    exit 0
}

$dockerProbe = Invoke-DockerProbe
if ($dockerProbe.ExitCode -ne 0) {
    Write-Host 'RUNTIME_CHECKS_FAILED'
    Write-Host '- Docker Engine is not reachable.'
    Write-Host ($dockerProbe.Output -join [Environment]::NewLine)
    exit 2
}

$serverVersion = $dockerProbe.Output | Select-Object -Last 1

docker compose -f (Join-Path $labRoot 'compose.yaml') config --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host 'RUNTIME_CHECKS_FAILED'
    Write-Host '- docker compose config validation failed.'
    exit 3
}

$containerId = docker compose -f (Join-Path $labRoot 'compose.yaml') ps -q kafka
if ([string]::IsNullOrWhiteSpace($containerId)) {
    Write-Host 'RUNTIME_CHECKS_FAILED'
    Write-Host '- Kafka container is not running.'
    exit 4
}

docker exec $containerId /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'RUNTIME_CHECKS_FAILED'
    Write-Host '- Kafka CLI cannot reach the broker.'
    exit 5
}

Write-Host "DOCKER_SERVER_VERSION=$serverVersion"
Write-Host 'RUNTIME_CHECKS_PASSED'
