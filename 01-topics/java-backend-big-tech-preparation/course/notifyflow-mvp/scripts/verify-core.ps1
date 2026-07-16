param(
    [string]$BuildDirectory
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($BuildDirectory)) {
    $workspaceRoot = (Resolve-Path (Join-Path $projectRoot '..\..\..\..')).Path
    $BuildDirectory = Join-Path $workspaceRoot 'tmp\notifyflow-core-contract-check'
}
$buildFull = [IO.Path]::GetFullPath($BuildDirectory)
New-Item -ItemType Directory -Force -Path $buildFull | Out-Null

$sources = @(
    Get-ChildItem (Join-Path $projectRoot 'notifyflow-domain\src\main\java') -Recurse -File -Filter *.java
    Get-ChildItem (Join-Path $projectRoot 'notifyflow-application\src\main\java') -Recurse -File -Filter *.java
    Get-Item (Join-Path $PSScriptRoot 'CoreContractCheck.java')
) | ForEach-Object { $_.FullName }

& javac --release 21 -d $buildFull $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& java -cp $buildFull CoreContractCheck
exit $LASTEXITCODE
