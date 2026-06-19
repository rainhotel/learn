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

$ys = 80,90,100,110,120,130,140,150,160,170,180,190,200
foreach ($y in $ys) {
    $body = $body.Replace("<wire from=`"(280,$y)`" to=`"(300,$y)`" />", '')
}

if ($body -notlike '*loc="(280,80)" name="Buffer"*') {
    $buffers = ''
    foreach ($y in $ys) {
        $buffers += "<comp lib=`"1`" loc=`"(280,$y)`" name=`"Buffer`"><a name=`"facing`" val=`"west`" /><a name=`"width`" val=`"1`" /><a name=`"out`" val=`"01`" /><a name=`"label`" val=`"`" /><a name=`"labelfont`" val=`"Dialog plain 12`" /><a name=`"labelcolor`" val=`"#000000`" /></comp>"
    }

    $insertBefore = '<comp lib="0" loc="(450,40)" name="Tunnel">'
    $index = $body.IndexOf($insertBefore)
    if ($index -lt 0) {
        throw 'Cannot find insertion point before output tunnels'
    }
    $body = $body.Substring(0, $index) + $buffers + $body.Substring($index)
}

$text = $before + $body + $after
[System.IO.File]::WriteAllText($Path, $text, $encoding)
