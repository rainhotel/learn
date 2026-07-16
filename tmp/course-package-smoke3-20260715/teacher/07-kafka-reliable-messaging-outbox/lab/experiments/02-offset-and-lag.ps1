param(
    [int]$MessageCount = 20,
    [int]$CommittedOffset = 5
)

$ErrorActionPreference = 'Stop'
if ($CommittedOffset -lt 0 -or $CommittedOffset -gt $MessageCount) {
    throw 'CommittedOffset must be between 0 and MessageCount.'
}

$labRoot = Split-Path -Parent $PSScriptRoot
$env:DOCKER_CONFIG = Join-Path $labRoot '.docker'
$composeFile = Join-Path $labRoot 'compose.yaml'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$topic = "notifyflow-lag-$timestamp"
$group = "notifyflow-lag-group-$timestamp"
$runDir = Join-Path $labRoot "evidence\runs\$timestamp-lag"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

$containerId = docker compose -f $composeFile ps -q kafka
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
    throw 'Kafka container is not running. Run scripts\start-lab.ps1 first.'
}

docker exec $containerId /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:9092 `
    --create `
    --topic $topic `
    --partitions 1 `
    --replication-factor 1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Topic creation failed.'
}

$messages = 1..$MessageCount | ForEach-Object { "event-$_" }
$messages | docker exec -i $containerId /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:9092 `
    --topic $topic `
    --producer.config /course-config/producer.properties
if ($LASTEXITCODE -ne 0) {
    throw 'Producing lag experiment events failed.'
}

docker exec $containerId /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:9092 `
    --topic $topic `
    --group $group `
    --from-beginning `
    --max-messages 1 `
    --timeout-ms 10000 `
    --consumer-property enable.auto.commit=true `
    --consumer-property auto.commit.interval.ms=100 | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to initialize the consumer group.'
}

docker exec $containerId /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:9092 `
    --group $group `
    --topic "$topic:0" `
    --reset-offsets `
    --to-offset $CommittedOffset `
    --execute | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to set the committed offset.'
}

$lagView = docker exec $containerId /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:9092 `
    --group $group `
    --describe
$lagView | Set-Content -LiteralPath (Join-Path $runDir 'lag-before-recovery.txt') -Encoding UTF8

$expectedLag = $MessageCount - $CommittedOffset
$dataLine = $lagView | Where-Object { $_ -match [regex]::Escape($topic) } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($dataLine) -or $dataLine -notmatch "\s$expectedLag\s") {
    throw "Expected lag $expectedLag was not found. Output=$($lagView -join ' | ')"
}

docker exec $containerId /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:9092 `
    --group $group `
    --topic "$topic:0" `
    --reset-offsets `
    --to-latest `
    --execute | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to advance the group to the latest offset.'
}

$recoveredView = docker exec $containerId /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:9092 `
    --group $group `
    --describe
$recoveredView | Set-Content -LiteralPath (Join-Path $runDir 'lag-after-recovery.txt') -Encoding UTF8

$recoveredLine = $recoveredView | Where-Object { $_ -match [regex]::Escape($topic) } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($recoveredLine) -or $recoveredLine -notmatch '\s0\s') {
    throw "Expected lag 0 was not found. Output=$($recoveredView -join ' | ')"
}

Write-Host "TOPIC=$topic"
Write-Host "GROUP=$group"
Write-Host "MESSAGE_COUNT=$MessageCount"
Write-Host "LAG_BEFORE=$expectedLag"
Write-Host 'LAG_AFTER=0'
Write-Host "EVIDENCE=$runDir"
Write-Host 'OFFSET_LAG_EXPERIMENT_PASSED'
