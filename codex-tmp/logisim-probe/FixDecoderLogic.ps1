param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$repo = (Resolve-Path -LiteralPath 'D:\moniC\project\learn').Path
$resolved = (Resolve-Path -LiteralPath $Path).Path
if (-not $resolved.StartsWith($repo, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to edit outside workspace: $resolved"
}

$text = [IO.File]::ReadAllText($resolved, [Text.Encoding]::UTF8)
if ($text.Contains('loc="(160,510)" name="Comparator"')) {
    Write-Host "Decoder patch already present: $resolved"
    return
}

function OpCompare {
    param(
        [string]$Label,
        [string]$Value,
        [int]$Y
    )
    $y2 = $Y + 20
    $yc = $Y + 10
    return @"
<comp lib="0" loc="(80,$Y)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="6" /><a name="label" val="OP" /><a name="labelfont" val="Dialog plain 12" /></comp><comp lib="0" loc="(80,$y2)" name="Constant"><a name="facing" val="east" /><a name="width" val="6" /><a name="value" val="$Value" /></comp><comp lib="3" loc="(160,$yc)" name="Comparator"><a name="width" val="6" /><a name="mode" val="twosComplement" /></comp><comp lib="0" loc="(210,$yc)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="$Label" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(80,$Y)" to="(120,$Y)" /><wire from="(80,$y2)" to="(120,$y2)" /><wire from="(160,$yc)" to="(210,$yc)" />
"@
}

$patch = @"
<comp lib="6" loc="(226,486)" name="Text"><a name="text" val="Codex补全译码逻辑" /><a name="font" val="SansSerif plain 12" /><a name="color" val="#000000" /><a name="halign" val="center" /><a name="valign" val="base" /></comp>
$(OpCompare 'LW' '0x23' 500)
$(OpCompare 'SW' '0x2b' 550)
$(OpCompare 'BEQ' '0x4' 600)
$(OpCompare 'ADDI' '0x8' 650)
<comp lib="0" loc="(80,700)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="6" /><a name="label" val="OP" /><a name="labelfont" val="Dialog plain 12" /></comp><comp lib="0" loc="(80,720)" name="Constant"><a name="facing" val="east" /><a name="width" val="6" /><a name="value" val="0x0" /></comp><comp lib="3" loc="(160,710)" name="Comparator"><a name="width" val="6" /><a name="mode" val="twosComplement" /></comp><wire from="(80,700)" to="(120,700)" /><wire from="(80,720)" to="(120,720)" /><comp lib="0" loc="(80,750)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="6" /><a name="label" val="FUNCT" /><a name="labelfont" val="Dialog plain 12" /></comp><comp lib="0" loc="(80,770)" name="Constant"><a name="facing" val="east" /><a name="width" val="6" /><a name="value" val="0x2a" /></comp><comp lib="3" loc="(160,760)" name="Comparator"><a name="width" val="6" /><a name="mode" val="twosComplement" /></comp><wire from="(80,750)" to="(120,750)" /><wire from="(80,770)" to="(120,770)" /><comp lib="1" loc="(260,735)" name="AND Gate"><a name="facing" val="east" /><a name="width" val="1" /><a name="size" val="30" /><a name="inputs" val="2" /><a name="out" val="01" /><a name="label" val="" /><a name="labelfont" val="Dialog plain 12" /><a name="labelcolor" val="#000000" /><a name="negate0" val="false" /><a name="negate1" val="false" /></comp><comp lib="0" loc="(310,735)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="SLT" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(160,710)" to="(230,710)" /><wire from="(230,710)" to="(230,725)" /><wire from="(160,760)" to="(230,760)" /><wire from="(230,745)" to="(230,760)" /><wire from="(260,735)" to="(310,735)" />
"@

$marker = '</circuit><circuit name="◇时序发生器状态机(定长指令周期)">'
if (-not $text.Contains($marker)) {
    throw "Could not find decoder circuit boundary"
}
$text = $text.Replace($marker, "$patch$marker")
[IO.File]::WriteAllText($resolved, $text, [Text.Encoding]::UTF8)
Write-Host "Updated decoder: $resolved"
