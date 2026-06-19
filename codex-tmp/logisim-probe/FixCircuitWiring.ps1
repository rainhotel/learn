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
$before = $text

function Replace-Once {
    param(
        [string]$Old,
        [string]$New
    )
    if ($script:text.Contains($Old)) {
        $script:text = $script:text.Replace($Old, $New)
        return $true
    }
    return $false
}

function Remove-Once {
    param([string]$Needle)
    [void](Replace-Once $Needle '')
}

# The original T3 wire runs vertically through the T4 splitter tap at (550,440),
# shorting T3 and T4 exactly when T3=1 and T4=0.
[void](Replace-Once `
    '<comp lib="0" loc="(550,470)" name="Tunnel"><a name="facing" val="north" /><a name="width" val="1" /><a name="label" val="T3" /><a name="labelfont" val="Dialog plain 12" /></comp>' `
    '<comp lib="0" loc="(540,470)" name="Tunnel"><a name="facing" val="north" /><a name="width" val="1" /><a name="label" val="T3" /><a name="labelfont" val="Dialog plain 12" /></comp>')
[void](Replace-Once `
    '<wire from="(550,430)" to="(550,470)" />' `
    '<wire from="(540,430)" to="(540,470)" /><wire from="(540,430)" to="(550,430)" />')
Remove-Once '<wire from="(550,470)" to="(550,480)" />'

# Debug tunnels must observe register outputs, not short controlled buffer outputs
# or the AR splitter/RAM address branch back into the 32-bit register value.
[void](Replace-Once `
    '<comp lib="0" loc="(550,140)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="DBG_PC" /><a name="labelfont" val="Dialog plain 12" /></comp>' `
    '<comp lib="0" loc="(530,140)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="DBG_PC" /><a name="labelfont" val="Dialog plain 12" /></comp>')
Remove-Once '<wire from="(530,140)" to="(550,140)" />'

[void](Replace-Once `
    '<comp lib="0" loc="(550,550)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="DBG_Z" /><a name="labelfont" val="Dialog plain 12" /></comp>' `
    '<comp lib="0" loc="(530,550)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="DBG_Z" /><a name="labelfont" val="Dialog plain 12" /></comp>')
Remove-Once '<wire from="(530,550)" to="(550,550)" />'

[void](Replace-Once `
    '<comp lib="0" loc="(190,280)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="DBG_AR" /><a name="labelfont" val="Dialog plain 12" /></comp>' `
    '<comp lib="0" loc="(170,280)" name="Tunnel"><a name="facing" val="east" /><a name="width" val="32" /><a name="label" val="DBG_AR" /><a name="labelfont" val="Dialog plain 12" /></comp>')
Remove-Once '<wire from="(170,280)" to="(190,280)" />'

if ($text -eq $before) {
    Write-Host "No changes needed: $resolved"
} else {
    [IO.File]::WriteAllText($resolved, $text, [Text.Encoding]::UTF8)
    Write-Host "Updated: $resolved"
}
