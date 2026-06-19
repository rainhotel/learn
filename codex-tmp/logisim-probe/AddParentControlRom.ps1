param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = 'Stop'
$encoding = New-Object System.Text.UTF8Encoding($true)
$text = [System.IO.File]::ReadAllText($Path, $encoding)

$comboToken = '<circuit name="◇硬布线控制器组合逻辑单元">'
$comboStart = $text.IndexOf($comboToken)
if ($comboStart -lt 0) {
    throw 'Cannot find combo circuit'
}
$comboEnd = $text.IndexOf('</circuit>', $comboStart)
if ($comboEnd -lt 0) {
    throw 'Cannot find combo circuit end'
}
$comboBody = $text.Substring($comboStart, $comboEnd - $comboStart)
$romMatch = [regex]::Match(
    $comboBody,
    '<comp lib="4" loc="\((330),(140)\)" name="ROM">(?:(?!</comp>).)*?</comp>',
    [System.Text.RegularExpressions.RegexOptions]::Singleline)
if (!$romMatch.Success) {
    throw 'Cannot find combo ROM'
}
$parentRom = $romMatch.Value.Replace('loc="(330,140)"', 'loc="(680,350)"')

$ctrlToken = '<circuit name="◇硬布线控制器">'
$start = $text.IndexOf($ctrlToken)
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

$body = [regex]::Replace(
    $body,
    '(<comp lib="0" loc="\((1240),(360)\)" name="Tunnel">(?:(?!</comp>).)*?<a name="label" val=")ControlBus(" />)',
    '${1}ControlBus_old${4}',
    [System.Text.RegularExpressions.RegexOptions]::Singleline)

if ($body -notlike '*loc="(680,350)" name="ROM"*') {
    $new = @(
        $parentRom,
        '<comp lib="0" loc="(540,350)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="13" /><a name="label" val="OCInput" /><a name="labelfont" val="Dialog plain 12" /></comp>',
        '<comp lib="0" loc="(570,390)" name="Constant"><a name="facing" val="east" /><a name="width" val="1" /><a name="value" val="0x0" /></comp>',
        '<wire from="(570,390)" to="(590,390)" />',
        '<comp lib="0" loc="(710,350)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="22" /><a name="label" val="ControlBus" /><a name="labelfont" val="Dialog plain 12" /></comp>',
        '<wire from="(680,350)" to="(710,350)" />'
    ) -join ''

    $insertBefore = '<comp loc="(1150,250)" name="◇硬布线控制器组合逻辑单元">'
    $index = $body.IndexOf($insertBefore)
    if ($index -lt 0) {
        throw 'Cannot find insertion point before combo instance'
    }
    $body = $body.Substring(0, $index) + $new + $body.Substring($index)
}

$text = $before + $body + $after
[System.IO.File]::WriteAllText($Path, $text, $encoding)
