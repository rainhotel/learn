param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = 'Stop'
$encoding = New-Object System.Text.UTF8Encoding($true)
$text = [System.IO.File]::ReadAllText($Path, $encoding)

$startToken = '<circuit name="◇硬布线控制器">'
$start = $text.IndexOf($startToken)
if ($start -lt 0) {
    throw 'Cannot find controller circuit'
}
$end = $text.IndexOf('</circuit>', $start)
if ($end -lt 0) {
    throw 'Cannot find controller circuit end'
}
$end += '</circuit>'.Length

$before = $text.Substring(0, $start)
$body = $text.Substring($start, $end - $start)
$after = $text.Substring($end)

if ($body -notlike '*loc="(1090,290)" name="Pull Resistor"*') {
    $pulls = @(
        '<comp lib="0" loc="(1090,290)" name="Pull Resistor"><a name="facing" val="west" /><a name="pull" val="0" /></comp>',
        '<comp lib="0" loc="(1060,300)" name="Pull Resistor"><a name="facing" val="west" /><a name="pull" val="0" /></comp>',
        '<comp lib="0" loc="(1030,310)" name="Pull Resistor"><a name="facing" val="west" /><a name="pull" val="0" /></comp>',
        '<comp lib="0" loc="(1000,320)" name="Pull Resistor"><a name="facing" val="west" /><a name="pull" val="0" /></comp>',
        '<comp lib="0" loc="(970,330)" name="Pull Resistor"><a name="facing" val="west" /><a name="pull" val="0" /></comp>'
    ) -join ''
    $insertBefore = '<comp loc="(1150,250)" name="◇硬布线控制器组合逻辑单元">'
    $index = $body.IndexOf($insertBefore)
    if ($index -lt 0) {
        throw 'Cannot find combo instance insertion point'
    }
    $body = $body.Substring(0, $index) + $pulls + $body.Substring($index)
}

$text = $before + $body + $after
[System.IO.File]::WriteAllText($Path, $text, $encoding)
