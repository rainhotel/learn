param(
    [Parameter(Mandatory = $true)]
    [string]$DirectionName,

    [string]$Slug
)

function Convert-ToSlug {
    param([string]$Value)

    $slug = $Value.ToLowerInvariant()
    $slug = $slug -replace "[^a-z0-9]+", "-"
    $slug = $slug.Trim("-")

    if ([string]::IsNullOrWhiteSpace($slug)) {
        throw "Could not generate a valid slug from DirectionName."
    }

    return $slug
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$templateDir = Join-Path $repoRoot "06-research\_template"
$researchDir = Join-Path $repoRoot "06-research"

if (-not $Slug) {
    $Slug = Convert-ToSlug -Value $DirectionName
}

$targetDir = Join-Path $researchDir $Slug

if (Test-Path $targetDir) {
    throw "Research directory already exists: $targetDir"
}

New-Item -ItemType Directory -Path $targetDir | Out-Null

$today = Get-Date -Format "yyyy-MM-dd"

Get-ChildItem -Path $templateDir -File | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content.Replace("{{DIRECTION_NAME}}", $DirectionName)
    $content = $content.Replace("{{DATE}}", $today)

    $targetFile = Join-Path $targetDir $_.Name
    Set-Content -Path $targetFile -Value $content -Encoding UTF8
}

Write-Output "Created research scaffold at: $targetDir"
