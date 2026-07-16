param(
    [string]$CourseRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$OutputRoot = (Join-Path (Split-Path -Parent $PSScriptRoot) 'dist'),
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$courseRootResolved = (Resolve-Path -LiteralPath $CourseRoot).Path
$outputRootFull = [IO.Path]::GetFullPath($OutputRoot)

if ($outputRootFull -eq $courseRootResolved -or $outputRootFull.StartsWith($courseRootResolved + [IO.Path]::DirectorySeparatorChar) -and
    -not $outputRootFull.StartsWith((Join-Path $courseRootResolved 'dist'))) {
    throw 'OutputRoot must not overwrite the course source tree.'
}

$studentRoot = Join-Path $outputRootFull 'student'
$teacherRoot = Join-Path $outputRootFull 'teacher'

if ($Clean -and (Test-Path -LiteralPath $outputRootFull)) {
    $resolvedOutput = (Resolve-Path -LiteralPath $outputRootFull).Path
    if ($resolvedOutput -eq $courseRootResolved -or -not $resolvedOutput.StartsWith([IO.Path]::GetPathRoot($resolvedOutput))) {
        throw "Refusing to clean unsafe output path: $resolvedOutput"
    }
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $studentRoot, $teacherRoot | Out-Null

function Get-RelativePath([string]$Base, [string]$Path) {
    return [IO.Path]::GetRelativePath($Base, $Path).Replace('\', '/')
}

function Test-CommonExcluded([string]$RelativePath) {
    return $RelativePath -match '^(dist|release-evidence)/' -or
        $RelativePath -match '^tools/' -or
        $RelativePath -match '(^|/)lab/evidence/' -or
        $RelativePath -match '(^|/)\.[^/]+($|/)' -or
        $RelativePath -match '(^|/)(target|build|tmp|node_modules)/'
}

function Test-StudentExcluded([string]$RelativePath) {
    if (Test-CommonExcluded $RelativePath) { return $true }
    if ($RelativePath -match '(^|/)answers\.md$') { return $true }
    if ($RelativePath -notmatch '/') {
        return $RelativePath -notin @(
            'dependency-map.md',
            'glossary.md',
            'learning-tracks.md',
            'student-workbook.md',
            'job-readiness-pack.md'
        )
    }
    return $RelativePath -notmatch '^\d{2}-'
}

function Test-TeacherExcluded([string]$RelativePath) {
    if (Test-CommonExcluded $RelativePath) { return $true }
    if ($RelativePath -notmatch '/') {
        return $RelativePath -notin @(
            'dependency-map.md',
            'glossary.md',
            'learning-tracks.md',
            'student-workbook.md',
            'job-readiness-pack.md',
            'instructor-editorial-guide.md'
        )
    }
    return $RelativePath -notmatch '^\d{2}-'
}

function Copy-PackageFiles(
    [string]$Destination,
    [scriptblock]$ExcludePredicate
) {
    $copied = [System.Collections.Generic.List[string]]::new()
    Get-ChildItem -LiteralPath $courseRootResolved -Recurse -File | ForEach-Object {
        $relative = Get-RelativePath $courseRootResolved $_.FullName
        if (& $ExcludePredicate $relative) { return }

        $target = Join-Path $Destination $relative
        $targetDirectory = Split-Path -Parent $target
        New-Item -ItemType Directory -Force -Path $targetDirectory | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $target -Force
        $copied.Add($relative)
    }
    return $copied
}

$studentFiles = Copy-PackageFiles $studentRoot { param($relative) Test-StudentExcluded $relative }
$teacherFiles = Copy-PackageFiles $teacherRoot { param($relative) Test-TeacherExcluded $relative }

$studentPackageNote = @'
# 学习者包说明

本包包含课程讲义、项目应用、实验说明、练习、面试和 Teach-back。

为保证独立作答，所有 `answers.md` 和 `lab/evidence/` 参考证据已从本包移除。完成作业并提交证据后，由教师按阶段解锁答案或参考运行结果。

治理和发布状态文件保留在学习者包中，用于明确课程边界、Pending 实验和诚实表达规则。
'@
$teacherPackageNote = @'
# 教师包说明

本包包含完整课程源内容、参考答案和已经归档的实验参考证据。教师应按学习流程解锁答案，不应把教师包直接分发给学习者。

`release-evidence/` 和 `tools/` 不进入教学包，由课程维护者单独管理。
'@
Set-Content -LiteralPath (Join-Path $studentRoot 'README.md') -Value $studentPackageNote -Encoding utf8
Set-Content -LiteralPath (Join-Path $teacherRoot 'README.md') -Value $teacherPackageNote -Encoding utf8
$studentFiles = @($studentFiles) + 'README.md'
$teacherFiles = @($teacherFiles) + 'README.md'

Get-ChildItem -LiteralPath $studentRoot -Recurse -File -Filter *.md | ForEach-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw
    $content = $content -replace '\[([^\]]+)\]\(answers\.md(?:#[^)]*)?\)', '$1（提交后由教师解锁）'
    $content = $content -replace '`answers\.md`', '参考答案（提交后由教师解锁）'
    $content = $content -replace '(?<![\w`])answers\.md(?![\w`])', '参考答案（提交后由教师解锁）'
    $content = $content -replace '`lab/evidence/[^`]*`', '教师参考证据（提交后解锁）'
    Set-Content -LiteralPath $_.FullName -Value $content -Encoding utf8 -NoNewline
}

foreach ($relative in $studentFiles | Where-Object { $_ -ne 'README.md' }) {
    $source = Join-Path $studentRoot $relative
    $target = Join-Path $teacherRoot $relative
    $targetDirectory = Split-Path -Parent $target
    New-Item -ItemType Directory -Force -Path $targetDirectory | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
}

function Write-Manifest([string]$Root, [string[]]$Files) {
    $rows = foreach ($relative in $Files | Sort-Object) {
        $file = Join-Path $Root $relative
        $hash = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $relative"
    }
    Set-Content -LiteralPath (Join-Path $Root 'MANIFEST.sha256') -Value $rows -Encoding utf8
}

Write-Manifest $studentRoot $studentFiles
Write-Manifest $teacherRoot $teacherFiles

$studentAnswers = Get-ChildItem -LiteralPath $studentRoot -Recurse -File -Filter answers.md
$studentEvidence = Get-ChildItem -LiteralPath $studentRoot -Recurse -File |
    Where-Object { (Get-RelativePath $studentRoot $_.FullName) -match '(^|/)lab/evidence/' }
$teacherAnswers = Get-ChildItem -LiteralPath $teacherRoot -Recurse -File -Filter answers.md
$chapterCount = (Get-ChildItem -LiteralPath $courseRootResolved -Directory |
    Where-Object { $_.Name -match '^\d{2}-' }).Count
$studentAnswerReferences = Get-ChildItem -LiteralPath $studentRoot -Recurse -File -Filter *.md |
    Select-String -Pattern 'answers\.md'
$studentChapterFiles = Get-ChildItem -LiteralPath $studentRoot -Directory |
    Where-Object { $_.Name -match '^\d{2}-' } |
    ForEach-Object {
        $chapter = $_
        $contentCount = (Get-ChildItem -LiteralPath $chapter.FullName -File -Filter *.md |
            Where-Object { $_.Name -in @('README.md', 'lesson.md', 'project-application.md', 'exercises.md', 'interview.md', 'teach-back.md', 'sources.md') }).Count
        [pscustomobject]@{
            Chapter = $chapter.Name
            ContentCount = $contentCount
            HasLabReadme = Test-Path -LiteralPath (Join-Path $chapter.FullName 'lab/README.md')
        }
    }
$invalidStudentChapters = $studentChapterFiles |
    Where-Object { $_.ContentCount -ne 7 -or -not $_.HasLabReadme }

function Find-BrokenLocalLinks([string]$Root) {
    $broken = [System.Collections.Generic.List[string]]::new()
    $pattern = [regex]'\[[^\]]+\]\((?!https?://|mailto:|#)(?<target>[^)]+)\)'
    Get-ChildItem -LiteralPath $Root -Recurse -File -Filter *.md | ForEach-Object {
        $file = $_
        $content = Get-Content -LiteralPath $file.FullName -Raw
        foreach ($match in $pattern.Matches($content)) {
            $target = $match.Groups['target'].Value.Trim().Split('#')[0].Trim('<', '>')
            if ([string]::IsNullOrWhiteSpace($target)) { continue }
            $resolved = [IO.Path]::GetFullPath((Join-Path $file.DirectoryName ([Uri]::UnescapeDataString($target))))
            if (-not $resolved.StartsWith([IO.Path]::GetFullPath($Root)) -or -not (Test-Path -LiteralPath $resolved)) {
                $broken.Add("$($file.FullName) -> $target")
            }
        }
    }
    return $broken
}

$studentBrokenLinks = @(Find-BrokenLocalLinks $studentRoot)
$teacherBrokenLinks = @(Find-BrokenLocalLinks $teacherRoot)
$sensitivePattern = '[A-Za-z]:\\Users\\|D:\\moniC\\|BEGIN (RSA |OPENSSH |EC )?PRIVATE KEY'
$studentSensitive = Get-ChildItem -LiteralPath $studentRoot -Recurse -File |
    Select-String -Pattern $sensitivePattern
$teacherSensitive = Get-ChildItem -LiteralPath $teacherRoot -Recurse -File |
    Select-String -Pattern $sensitivePattern
$teacherMismatch = [System.Collections.Generic.List[string]]::new()
foreach ($relative in $studentFiles | Where-Object { $_ -ne 'README.md' }) {
    $studentFile = Join-Path $studentRoot $relative
    $teacherFile = Join-Path $teacherRoot $relative
    if (-not (Test-Path -LiteralPath $teacherFile)) {
        $teacherMismatch.Add("MISSING: $relative")
        continue
    }
    $studentHash = (Get-FileHash -LiteralPath $studentFile -Algorithm SHA256).Hash
    $teacherHash = (Get-FileHash -LiteralPath $teacherFile -Algorithm SHA256).Hash
    if ($studentHash -ne $teacherHash) {
        $teacherMismatch.Add("HASH: $relative")
    }
}

$summary = [ordered]@{
    chapters = $chapterCount
    student_files = $studentFiles.Count
    teacher_files = $teacherFiles.Count
    student_answers = $studentAnswers.Count
    student_lab_evidence_files = $studentEvidence.Count
    teacher_answers = $teacherAnswers.Count
    student_answer_references = $studentAnswerReferences.Count
    invalid_student_chapters = $invalidStudentChapters.Count
    student_broken_links = $studentBrokenLinks.Count
    teacher_broken_links = $teacherBrokenLinks.Count
    teacher_student_mismatches = $teacherMismatch.Count
    student_sensitive_matches = $studentSensitive.Count
    teacher_sensitive_matches = $teacherSensitive.Count
}
$summary.GetEnumerator() | ForEach-Object { '{0}={1}' -f $_.Key, $_.Value }

if ($studentAnswers.Count -ne 0) { throw 'Student package contains answers.md.' }
if ($studentEvidence.Count -ne 0) { throw 'Student package contains locked lab evidence.' }
if ($teacherAnswers.Count -ne $chapterCount) {
    throw "Teacher package answer count $($teacherAnswers.Count) does not match chapter count $chapterCount."
}
if ($studentAnswerReferences.Count -ne 0) { throw 'Student package still references locked answers or evidence.' }
if ($invalidStudentChapters.Count -ne 0) { throw 'Student package chapter allowlist validation failed.' }
if ($studentBrokenLinks.Count -ne 0) { throw "Student package contains $($studentBrokenLinks.Count) broken local links." }
if ($teacherBrokenLinks.Count -ne 0) { throw "Teacher package contains $($teacherBrokenLinks.Count) broken local links." }
if ($teacherMismatch.Count -ne 0) { throw 'Teacher package is not a byte-identical superset of student content.' }
if ($studentSensitive.Count -ne 0) { throw 'Student package contains personal absolute paths or private-key material.' }
if ($teacherSensitive.Count -ne 0) { throw 'Teacher package contains personal absolute paths or private-key material.' }

'COURSE_PACKAGES_BUILT'
