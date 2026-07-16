param(
    [int]$EventsPerKey = 10
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
$env:DOCKER_CONFIG = Join-Path $labRoot '.docker'
$composeFile = Join-Path $labRoot 'compose.yaml'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$topic = "notifyflow-order-$timestamp"
$runDir = Join-Path $labRoot "evidence\runs\$timestamp-order"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

$containerId = docker compose -f $composeFile ps -q kafka
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
    throw 'Kafka container is not running. Run scripts\start-lab.ps1 first.'
}

docker exec $containerId /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:9092 `
    --create `
    --topic $topic `
    --partitions 3 `
    --replication-factor 1
if ($LASTEXITCODE -ne 0) {
    throw 'Topic creation failed.'
}

$events = New-Object System.Collections.Generic.List[string]
for ($sequence = 1; $sequence -le $EventsPerKey; $sequence++) {
    $events.Add("task-A:$sequence")
    $events.Add("task-B:$sequence")
    $events.Add("task-C:$sequence")
}

$events | Set-Content -LiteralPath (Join-Path $runDir 'produced-events.txt') -Encoding UTF8
$events | docker exec -i $containerId /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:9092 `
    --topic $topic `
    --producer.config /course-config/producer.properties `
    --property parse.key=true `
    --property key.separator=:
if ($LASTEXITCODE -ne 0) {
    throw 'Producing keyed events failed.'
}

$expectedCount = $events.Count
$output = docker exec $containerId /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:9092 `
    --topic $topic `
    --from-beginning `
    --max-messages $expectedCount `
    --timeout-ms 15000 `
    --property print.key=true `
    --property key.separator='|' `
    --property print.partition=true `
    --property print.offset=true
if ($LASTEXITCODE -ne 0) {
    throw 'Consuming keyed events failed.'
}

$output | Set-Content -LiteralPath (Join-Path $runDir 'consumed-events.txt') -Encoding UTF8

foreach ($key in @('task-A', 'task-B', 'task-C')) {
    $actual = New-Object System.Collections.Generic.List[int]
    foreach ($line in $output) {
        if ($line -match ([regex]::Escape($key) + '\|(\d+)')) {
            $actual.Add([int]$Matches[1])
        }
    }

    $expected = 1..$EventsPerKey
    if (($actual -join ',') -ne ($expected -join ',')) {
        throw "Order assertion failed for $key. Actual=$($actual -join ',')"
    }
}

$description = docker exec $containerId /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:9092 `
    --describe `
    --topic $topic
$description | Set-Content -LiteralPath (Join-Path $runDir 'topic-description.txt') -Encoding UTF8

Write-Host "TOPIC=$topic"
Write-Host "EVENTS=$expectedCount"
Write-Host "EVIDENCE=$runDir"
Write-Host 'PARTITION_ORDER_EXPERIMENT_PASSED'
