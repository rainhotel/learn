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
    throw 'Cannot find combo circuit'
}
$end = $text.IndexOf('</circuit>', $start)
if ($end -lt 0) {
    throw 'Cannot find combo circuit end'
}
$end += '</circuit>'.Length

$before = $text.Substring(0, $start)
$body = $text.Substring($start, $end - $start)
$after = $text.Substring($end)

$ys = 30,40,50,60,70,80,90,100,110,120,130,140,150,160,170,180,190,200
foreach ($y in $ys) {
    $body = [regex]::Replace(
        $body,
        "<comp lib=`"0`" loc=`"\(250,$y\)`" name=`"Tunnel`">(?:(?!</comp>).)*?</comp>",
        '',
        [System.Text.RegularExpressions.RegexOptions]::Singleline)
    $body = $body.Replace("<wire from=`"(250,$y)`" to=`"(280,$y)`" />", '')
}

if ($body -like '*loc="(300,80)" name="Tunnel"*') {
    throw 'Right-side combo address tunnels already exist'
}

$address = @(
    @{ Y = 80; Label = 'ctl_safe_SLT' },
    @{ Y = 90; Label = 'ctl_safe_ADDI' },
    @{ Y = 100; Label = 'ctl_safe_LW' },
    @{ Y = 110; Label = 'ctl_safe_SW' },
    @{ Y = 120; Label = 'ctl_safe_BEQ' },
    @{ Y = 130; Label = 'ctl_raw_Mif' },
    @{ Y = 140; Label = 'ctl_raw_Mcal' },
    @{ Y = 150; Label = 'ctl_raw_Mex' },
    @{ Y = 160; Label = 'ctl_raw_T1' },
    @{ Y = 170; Label = 'ctl_raw_T2' },
    @{ Y = 180; Label = 'ctl_raw_T3' },
    @{ Y = 190; Label = 'ctl_raw_T4' },
    @{ Y = 200; Label = 'ctl_safe_EQUAL' }
)

$new = ''
foreach ($entry in $address) {
    $y = $entry.Y
    $label = $entry.Label
    $new += "<comp lib=`"0`" loc=`"(300,$y)`" name=`"Tunnel`"><a name=`"facing`" val=`"west`" /><a name=`"width`" val=`"1`" /><a name=`"label`" val=`"$label`" /><a name=`"labelfont`" val=`"Dialog plain 12`" /></comp><wire from=`"(280,$y)`" to=`"(300,$y)`" />"
}

$insertBefore = '<comp lib="0" loc="(450,40)" name="Tunnel">'
$index = $body.IndexOf($insertBefore)
if ($index -lt 0) {
    throw 'Cannot find insertion point before output tunnels'
}
$body = $body.Substring(0, $index) + $new + $body.Substring($index)

$text = $before + $body + $after
[System.IO.File]::WriteAllText($Path, $text, $encoding)
