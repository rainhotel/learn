param(
    [string]$CourseRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$required = @(
    'README.md',
    'lesson.md',
    'project-application.md',
    'exercises.md',
    'answers.md',
    'interview.md',
    'teach-back.md',
    'sources.md'
)

$chapters = Get-ChildItem -LiteralPath $CourseRoot -Directory |
    Where-Object { $_.Name -match '^\d{2}-' } |
    Sort-Object Name

$missing = [System.Collections.Generic.List[string]]::new()
foreach ($chapter in $chapters) {
    foreach ($file in $required) {
        if (-not (Test-Path -LiteralPath (Join-Path $chapter.FullName $file))) {
            $missing.Add("$($chapter.Name)/$file")
        }
    }

    if (-not (Test-Path -LiteralPath (Join-Path $chapter.FullName 'lab/README.md'))) {
        $missing.Add("$($chapter.Name)/lab/README.md")
    }
}

$textFiles = Get-ChildItem -LiteralPath $CourseRoot -Recurse -File |
    Where-Object { $_.Extension -in '.md', '.java', '.js', '.yml', '.yaml', '.ps1' }
$markdownFiles = $textFiles | Where-Object { $_.Extension -eq '.md' }

$placeholderMatches = $markdownFiles |
    Select-String -Pattern 'TBD|TODO|PLACEHOLDER|待填写|待定'
$trailingWhitespace = $textFiles |
    Select-String -Pattern '[ \t]+$'

$brokenLinks = [System.Collections.Generic.List[string]]::new()
$linkPattern = [regex]'\[[^\]]+\]\((?!https?://|mailto:|#)(?<target>[^)]+)\)'
foreach ($file in $markdownFiles) {
    $content = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($match in $linkPattern.Matches($content)) {
        $target = $match.Groups['target'].Value.Trim()
        $target = $target.Split('#')[0]
        $target = $target.Trim('<', '>')
        if ([string]::IsNullOrWhiteSpace($target)) {
            continue
        }

        $decodedTarget = [Uri]::UnescapeDataString($target)
        $resolved = Join-Path $file.DirectoryName $decodedTarget
        if (-not (Test-Path -LiteralPath $resolved)) {
            $brokenLinks.Add("$($file.FullName) -> $target")
        }
    }
}

$result = [ordered]@{
    chapters = $chapters.Count
    markdown = $markdownFiles.Count
    java = ($textFiles | Where-Object { $_.Extension -eq '.java' }).Count
    javascript = ($textFiles | Where-Object { $_.Extension -eq '.js' }).Count
    yaml = ($textFiles | Where-Object { $_.Extension -in '.yml', '.yaml' }).Count
    powershell = ($textFiles | Where-Object { $_.Extension -eq '.ps1' }).Count
    missing_required_files = $missing.Count
    placeholder_matches = $placeholderMatches.Count
    trailing_whitespace_matches = $trailingWhitespace.Count
    broken_local_links = $brokenLinks.Count
}

$result.GetEnumerator() | ForEach-Object { '{0}={1}' -f $_.Key, $_.Value }

if ($missing.Count -gt 0) {
    $missing | ForEach-Object { "MISSING: $_" }
}
if ($placeholderMatches.Count -gt 0) {
    $placeholderMatches | ForEach-Object { "PLACEHOLDER: $($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
}
if ($trailingWhitespace.Count -gt 0) {
    $trailingWhitespace | ForEach-Object { "TRAILING: $($_.Path):$($_.LineNumber)" }
}
if ($brokenLinks.Count -gt 0) {
    $brokenLinks | ForEach-Object { "BROKEN_LINK: $_" }
}

if ($missing.Count -gt 0 -or
    $placeholderMatches.Count -gt 0 -or
    $trailingWhitespace.Count -gt 0 -or
    $brokenLinks.Count -gt 0) {
    exit 1
}

'COURSE_AUDIT_PASSED'
