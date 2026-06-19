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
if ($text.Contains('label" val="RF_EQ_A"')) {
    Write-Host "Register equal patch already present: $resolved"
    return
}

$text = $text.Replace(
    '<comp lib="0" loc="(520,450)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="equal" /><a name="labelfont" val="SansSerif plain 10" /></comp>',
    '<comp lib="0" loc="(520,450)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="psw_equal" /><a name="labelfont" val="SansSerif plain 10" /></comp>')

$text = $text.Replace(
    '<wire from="(820,460)" to="(870,460)" />',
    '<wire from="(820,460)" to="(850,460)" /><wire from="(850,460)" to="(870,460)" />')

$patch = @'
<comp lib="0" loc="(850,460)" name="Tunnel"><a name="facing" val="north" /><a name="width" val="32" /><a name="label" val="RF_EQ_A" /><a name="labelfont" val="Dialog plain 12" /></comp><comp lib="0" loc="(850,560)" name="Tunnel"><a name="facing" val="north" /><a name="width" val="32" /><a name="label" val="RF_EQ_B" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(820,560)" to="(850,560)" /><comp lib="6" loc="(1035,710)" name="Text"><a name="text" val="Codex BEQ equal" /><a name="font" val="SansSerif plain 12" /><a name="color" val="#000000" /><a name="halign" val="center" /><a name="valign" val="base" /></comp><comp lib="0" loc="(960,730)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="RF_EQ_A" /><a name="labelfont" val="Dialog plain 12" /></comp><comp lib="0" loc="(960,750)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="RF_EQ_B" /><a name="labelfont" val="Dialog plain 12" /></comp><comp lib="3" loc="(1040,740)" name="Comparator"><a name="width" val="32" /><a name="mode" val="twosComplement" /></comp><comp lib="0" loc="(1080,740)" name="Tunnel"><a name="facing" val="west" /><a name="width" val="1" /><a name="label" val="equal" /><a name="labelfont" val="Dialog plain 12" /></comp><wire from="(960,730)" to="(1000,730)" /><wire from="(960,750)" to="(1000,750)" /><wire from="(1040,740)" to="(1080,740)" />
'@

$marker = '</circuit><circuit name="◇硬布线控制器">'
if (-not $text.Contains($marker)) {
    throw "Could not find main circuit boundary"
}
$text = $text.Replace($marker, "$patch$marker")
[IO.File]::WriteAllText($resolved, $text, [Text.Encoding]::UTF8)
Write-Host "Updated register equal: $resolved"
