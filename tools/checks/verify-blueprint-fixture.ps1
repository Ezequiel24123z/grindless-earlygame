# Source-only twin of verify-blueprint-fixture.sh; no JVM, server or files written.
param([string]$Root = (Join-Path $PSScriptRoot '..\..'))

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Root = (Resolve-Path -LiteralPath $Root).Path
$smoke = Join-Path $Root 'tools\smoke'
$pack = Join-Path $smoke 'first-blueprint-pack'

function Normalize-Commands([string]$Text) {
    return (@($Text -split '\r?\n' | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') }) -join "`n")
}

# The existing exosuit oracle remains untouched. Compare its exact command structure,
# removing only the other three items/markers and renaming the single retained item.
function Convert-ExosuitReference([string]$Text) {
    $lines = foreach ($line in ($Text -split "`n")) {
        foreach ($item in @('flux_exosuit_chestplate', 'network_tap', 'exoskeleton_legs')) {
            $selector = ' if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:' +
                $item + '",Count:1b}}]'
            $line = $line.Replace($selector, '')
        }
        if ($line -match 'chest_spawn|tap_spawn|legs_spawn|flux_exosuit_chestplate|network_tap|exoskeleton_legs|EXO-CHEST-OK|TAP-OK|LEGS-MOD-OK|DIAG-(SUMMON|ENTITY)-(CHEST|TAP|LEGS)-') {
            continue
        }
        $line.Replace('four once-summoned registered exosuit items', 'one once-summoned registered blueprint tool').
            Replace('grindless_exosuit', 'grindless_blueprint').
            Replace('exosuit_check', 'blueprint_check').
            Replace('helmet_spawn', 'tool_spawn').
            Replace('flux_exosuit_helmet', 'blueprint_tool').
            Replace('EXO-HELMET-OK', 'BLUEPRINT-OK').
            Replace('EXOSUIT', 'BLUEPRINT').
            Replace('HELMET', 'TOOL').
            Replace('first-exosuit-pack', 'first-blueprint-pack')
    }
    return ($lines -join "`n")
}

$functions = @('begin', 'spawn', 'poll', 'passed', 'failed', 'cleanup')
$paths = @('first-blueprint.commands', 'first-blueprint.expect', 'first-blueprint-pack/pack.mcmeta') +
    @($functions | ForEach-Object { "first-blueprint-pack/data/grindless_blueprint/functions/$_.mcfunction" })
$actual = @()
$expected = @()
# Freeze both sources once; all negative cases below mutate only in-memory copies.
foreach ($path in $paths) {
    $reference = $path.Replace('first-blueprint', 'first-exosuit').Replace('grindless_blueprint', 'grindless_exosuit')
    $actual += Normalize-Commands ([IO.File]::ReadAllText((Join-Path $smoke $path)))
    $expected += Convert-ExosuitReference (Normalize-Commands ([IO.File]::ReadAllText((Join-Path $smoke $reference))))
}
$actual += (@(Get-ChildItem -LiteralPath $pack -Recurse -File | ForEach-Object {
    $_.FullName.Substring($pack.Length + 1).Replace('\', '/')
} | Sort-Object -CaseSensitive) -join "`n")
$inventory = @('pack.mcmeta') + @($functions | ForEach-Object { "data/grindless_blueprint/functions/$_.mcfunction" })
$expected += (@($inventory | Sort-Object -CaseSensitive) -join "`n")
$actual += $(if (Test-Path -LiteralPath (Join-Path $smoke 'first-blueprint.timeout')) { 'present' } else { 'absent' })
$expected += 'absent'

function Test-Snapshot([string[]]$Source) {
    if ($Source.Count -ne $expected.Count) { return $false }
    for ($i = 0; $i -lt $expected.Count; $i++) {
        if ($Source[$i] -cne $expected[$i]) { return $false }
    }
    return $true
}

$labels = @('console entry', 'original marker', 'pack metadata', 'begin', 'one-shot spawn',
    'loaded bounded poll', 'observed success', 'console diagnostics', 'scoped cleanup', 'pack inventory', 'no timeout override')
for ($i = 0; $i -lt $expected.Count; $i++) {
    if ($actual[$i] -cne $expected[$i]) { throw ('Source contract failed: ' + $labels[$i]) }
    Write-Output ('  ok   ' + $labels[$i])
}
$crlf = @($actual | ForEach-Object { Normalize-Commands ($_.Replace("`n", "`r`n")) })
if (-not (Test-Snapshot $crlf)) { throw 'CRLF normalization changed the source contract.' }
Write-Output '  ok   CRLF input'

$oldConsole = @('forceload add 0 0', 'say WAIT-CHUNK',
    'summon minecraft:item 8 100 8 {Item:{id:"grindless:blueprint_tool",Count:1b}}',
    'execute if entity @e[type=item,nbt={Item:{id:"grindless:blueprint_tool"}}] run say BLUEPRINT-OK',
    'kill @e[type=item]') -join "`n"
$cases = @(
    @{ Name = 'original one-shot console'; Index = 0; Old = $actual[0]; New = $oldConsole }
    @{ Name = 'no loaded barrier'; Index = 5; Old = 'if loaded 8 100 8 '; New = '' }
    @{ Name = 'unverified count'; Index = 5; Old = ',Count:1b'; New = '' }
    @{ Name = 'wrong item ID'; Index = 5; Old = 'grindless:blueprint_tool'; New = 'minecraft:stick' }
    @{ Name = 'wrong entity type'; Index = 5; Old = 'type=minecraft:item'; New = 'type=minecraft:armor_stand' }
    @{ Name = 'repeated summon'; Index = 5; Old = 'scoreboard players add tries blueprint_check 1'; New = "scoreboard players add tries blueprint_check 1`nfunction grindless_blueprint:spawn" }
    @{ Name = 'unowned summon'; Index = 4; Old = 'Tags:["grindless_blueprint_smoke"],'; New = '' }
    @{ Name = 'incidental hazards'; Index = 4; Old = 'NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,'; New = '' }
    @{ Name = 'summon success is not observation'; Index = 5; Old = 'if score observed blueprint_check matches 1'; New = 'if score tool_spawn blueprint_check matches 1' }
    @{ Name = 'false marker on failure'; Index = 7; Old = 'say BLUEPRINT-FAIL-CHUNK-OR-ENTITY-TIMEOUT'; New = "say BLUEPRINT-FAIL-CHUNK-OR-ENTITY-TIMEOUT`nsay BLUEPRINT-OK" }
    @{ Name = 'unbounded polling'; Index = 5; Old = 'if score tries blueprint_check matches ..159 '; New = '' }
    @{ Name = 'longer polling interval'; Index = 5; Old = ':poll 5t'; New = ':poll 20t' }
    @{ Name = 'global item cleanup'; Index = 8; Old = 'kill @e[type=minecraft:item,tag=grindless_blueprint_smoke]'; New = 'kill @e[type=item]' }
    @{ Name = 'global forceload cleanup'; Index = 8; Old = 'forceload remove 0 0'; New = 'forceload remove all' }
    @{ Name = 'silent failure feedback'; Index = 7; Old = 'execute if loaded 8 100 8 run say BLUEPRINT-DIAG-CHUNK-LOADED'; New = 'data get entity @e[tag=grindless_blueprint_smoke,limit=1] Item' }
    @{ Name = 'outer timeout override'; Index = 10; Old = 'absent'; New = 'present' }
    @{ Name = 'changed expectation'; Index = 1; Old = 'BLUEPRINT-OK'; New = 'SMOKE-READY' }
    @{ Name = 'extra pack function'; Index = 9; Old = 'pack.mcmeta'; New = "pack.mcmeta`ndata/grindless_blueprint/functions/bypass.mcfunction" }
)
foreach ($case in $cases) {
    if (-not $actual[$case.Index].Contains($case.Old)) { throw ('Stale mutation: ' + $case.Name) }
    $mutated = $actual.Clone()
    $mutated[$case.Index] = $mutated[$case.Index].Replace($case.Old, $case.New)
    if (Test-Snapshot $mutated) { throw ('Accepted invalid source: ' + $case.Name) }
    Write-Output ('  reject ' + $case.Name)
}
Write-Output ("ALL BLUEPRINT FIXTURE SOURCE CHECKS PASSED: 12 positive, {0} negative (NOT A MINECRAFT RUN)" -f $cases.Count)
