$ErrorActionPreference = "Stop"

$labRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceRoot = Join-Path $labRoot "src"
$buildRoot = Join-Path $labRoot "build"

New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
$sources = Get-ChildItem -Path $sourceRoot -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

& javac.exe -encoding UTF-8 -d $buildRoot $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

Write-Output "Compiled V0 classes to $buildRoot"

