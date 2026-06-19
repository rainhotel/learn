param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = 'Stop'
$encoding = New-Object System.Text.UTF8Encoding($true)
$text = [System.IO.File]::ReadAllText($Path, $encoding)

$ctrlToken = '<circuit name="◇硬布线控制器">'
$start = $text.IndexOf($ctrlToken)
if ($start -lt 0) { throw 'Cannot find controller circuit' }
$end = $text.IndexOf('</circuit>', $start)
if ($end -lt 0) { throw 'Cannot find controller circuit end' }

$before = $text.Substring(0, $start)
$body = $text.Substring($start, $end - $start)
$after = $text.Substring($end)

$romPattern = '<comp lib="4" loc="\((1500),(850)\)" name="ROM">(?:(?!</comp>).)*?</comp>'
$romMatch = [regex]::Match($body, $romPattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
if (!$romMatch.Success) { throw 'Cannot find parent control ROM at (1500,850)' }

$contentPattern = '<a name="contents">(?<contents>.*?)</a>'
$contentMatch = [regex]::Match($romMatch.Value, $contentPattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
if (!$contentMatch.Success) { throw 'Cannot find parent control ROM contents' }

$hexWords = [regex]::Matches($contentMatch.Groups['contents'].Value, '\b[0-9a-fA-F]{6}\b')
if ($hexWords.Count -le 0x90) { throw 'Parent control ROM contents are shorter than expected' }

$probeWord = $hexWords[0x90].Value.ToLowerInvariant()
if ($probeWord -eq '202400') {
    Write-Output 'Parent control ROM already has swapped bit-pair encoding.'
    return
}
if ($probeWord -ne '101800') {
    throw "Unexpected control word at address 0x90: $probeWord"
}

function Swap-AdjacentBits([int]$value) {
    $result = 0
    for ($i = 0; $i -lt 22; $i++) {
        if (($value -band (1 -shl $i)) -ne 0) {
            $result = $result -bor (1 -shl ($i -bxor 1))
        }
    }
    return $result
}

$newContents = [regex]::Replace(
    $contentMatch.Groups['contents'].Value,
    '\b[0-9a-fA-F]{6}\b',
    {
        param($m)
        $value = [Convert]::ToInt32($m.Value, 16)
        (Swap-AdjacentBits $value).ToString('x6')
    })

$contentIndex = $contentMatch.Groups['contents'].Index
$newRom = $romMatch.Value.Substring(0, $contentIndex) +
    $newContents +
    $romMatch.Value.Substring($contentIndex + $contentMatch.Groups['contents'].Length)

$body = $body.Substring(0, $romMatch.Index) + $newRom + $body.Substring($romMatch.Index + $romMatch.Length)
$text = $before + $body + $after
[System.IO.File]::WriteAllText($Path, $text, $encoding)
Write-Output 'Swapped adjacent bit pairs in parent control ROM contents.'
