param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = 'Stop'

$encoding = New-Object System.Text.UTF8Encoding($true)
$text = [System.IO.File]::ReadAllText($Path, $encoding)

$startToken = '<circuit name="◇硬布线控制器组合逻辑单元">'
$start = $text.IndexOf($startToken)
if ($start -lt 0) {
    throw "Cannot find combo circuit"
}
$end = $text.IndexOf('</circuit>', $start)
if ($end -lt 0) {
    throw "Cannot find combo circuit end"
}
$end += '</circuit>'.Length

$before = $text.Substring(0, $start)
$body = $text.Substring($start, $end - $start)
$after = $text.Substring($end)

function Set-TunnelLabel {
    param(
        [string]$Body,
        [int]$X,
        [int]$Y,
        [string]$Label
    )

    $pattern = "(<comp lib=`"0`" loc=`"\($X,$Y\)`" name=`"Tunnel`">(?:(?!</comp>).)*?<a name=`"label`" val=`")[^`"]+(`" />)"
    $replacement = "`${1}$Label`${2}"
    $newBody = [regex]::Replace($Body, $pattern, $replacement, [System.Text.RegularExpressions.RegexOptions]::Singleline)
    if ($newBody -eq $Body) {
        throw "Cannot set label for tunnel ($X,$Y)"
    }
    return $newBody
}

$labels = @(
    @{ X = 250; Y = 80; Label = 'ctl_safe_SLT' },
    @{ X = 250; Y = 90; Label = 'ctl_safe_ADDI' },
    @{ X = 250; Y = 100; Label = 'ctl_safe_LW' },
    @{ X = 250; Y = 110; Label = 'ctl_safe_SW' },
    @{ X = 250; Y = 120; Label = 'ctl_safe_BEQ' },
    @{ X = 250; Y = 130; Label = 'ctl_raw_Mif' },
    @{ X = 250; Y = 140; Label = 'ctl_raw_Mcal' },
    @{ X = 250; Y = 150; Label = 'ctl_raw_Mex' }
)

foreach ($entry in $labels) {
    $body = Set-TunnelLabel -Body $body -X $entry.X -Y $entry.Y -Label $entry.Label
}

if ($body -notlike '*loc="(250,160)" name="Tunnel"*') {
    $insertAfter = '<wire from="(250,150)" to="(280,150)" />'
    $insert = @(
        '<comp lib="0" loc="(250,160)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="ctl_raw_T1" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(250,160)" to="(280,160)" />',
        '<comp lib="0" loc="(250,170)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="ctl_raw_T2" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(250,170)" to="(280,170)" />',
        '<comp lib="0" loc="(250,180)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="ctl_raw_T3" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(250,180)" to="(280,180)" />',
        '<comp lib="0" loc="(250,190)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="ctl_raw_T4" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(250,190)" to="(280,190)" />',
        '<comp lib="0" loc="(250,200)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="ctl_safe_EQUAL" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(250,200)" to="(280,200)" />'
    ) -join ''

    $index = $body.IndexOf($insertAfter)
    if ($index -lt 0) {
        throw "Cannot find insertion point"
    }
    $index += $insertAfter.Length
    $body = $body.Substring(0, $index) + $insert + $body.Substring($index)
}

$text = $before + $body + $after
[System.IO.File]::WriteAllText($Path, $text, $encoding)
