param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('topics', 'producer', 'consumer', 'groups', 'configs')]
    [string]$Tool,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
$env:DOCKER_CONFIG = Join-Path $labRoot '.docker'
$composeFile = Join-Path $labRoot 'compose.yaml'

$toolPaths = @{
    topics   = '/opt/kafka/bin/kafka-topics.sh'
    producer = '/opt/kafka/bin/kafka-console-producer.sh'
    consumer = '/opt/kafka/bin/kafka-console-consumer.sh'
    groups   = '/opt/kafka/bin/kafka-consumer-groups.sh'
    configs  = '/opt/kafka/bin/kafka-configs.sh'
}

$containerId = docker compose -f $composeFile ps -q kafka
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
    throw 'Kafka container is not running. Run scripts\start-lab.ps1 first.'
}

$toolPath = $toolPaths[$Tool]
& docker exec $containerId $toolPath @Arguments
exit $LASTEXITCODE
