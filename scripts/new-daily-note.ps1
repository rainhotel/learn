param(
    [datetime]$Date = (Get-Date)
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $repoRoot "02-journal\\_templates\\daily-note.md"

$year = Get-Date $Date -Format "yyyy"
$month = Get-Date $Date -Format "MM"
$day = Get-Date $Date -Format "yyyy-MM-dd"

$targetDir = Join-Path $repoRoot "02-journal\\$year\\$month"
$targetFile = Join-Path $targetDir "$day.md"

if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
}

if (-not (Test-Path $targetFile)) {
    $content = Get-Content $templatePath -Raw
    $content = $content.Replace("{{DATE}}", $day)
    Set-Content -Path $targetFile -Value $content -Encoding UTF8
    Write-Output "Created daily note: $targetFile"
} else {
    Write-Output "Daily note already exists: $targetFile"
}

