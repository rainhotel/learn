param(
    [Parameter(Mandatory = $true)]
    [string]$TopicName,

    [string]$Slug
)

function Convert-ToSlug {
    param([string]$Value)

    $slug = $Value.ToLowerInvariant()
    $slug = $slug -replace "[^a-z0-9]+", "-"
    $slug = $slug.Trim("-")

    if ([string]::IsNullOrWhiteSpace($slug)) {
        throw "Could not generate a valid slug from TopicName."
    }

    return $slug
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$templateDir = Join-Path $repoRoot "01-topics\\_template"
$topicsDir = Join-Path $repoRoot "01-topics"

if (-not $Slug) {
    $Slug = Convert-ToSlug -Value $TopicName
}

$targetDir = Join-Path $topicsDir $Slug

if (Test-Path $targetDir) {
    throw "Topic directory already exists: $targetDir"
}

New-Item -ItemType Directory -Path $targetDir | Out-Null

$today = Get-Date -Format "yyyy-MM-dd"

Get-ChildItem -Path $templateDir -File | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content.Replace("{{TOPIC_NAME}}", $TopicName)
    $content = $content.Replace("{{DATE}}", $today)

    $targetFile = Join-Path $targetDir $_.Name
    Set-Content -Path $targetFile -Value $content -Encoding UTF8
}

Write-Output "Created topic scaffold at: $targetDir"

