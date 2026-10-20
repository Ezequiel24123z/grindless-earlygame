# Source contracts only: no JVM, Minecraft, network or world mutation.
param([string]$Root = (Join-Path $PSScriptRoot '..\..'))

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Root = (Resolve-Path -LiteralPath $Root).ProviderPath
$smoke = Join-Path $Root 'tools\smoke'
$script:Expected = [ordered]@{}
$script:Actual = [ordered]@{}

function Normalize-Commands([string]$Text) {
    return (@($Text -split '\r?\n' | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') }) -join "`n")
}

function Read-Source([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return '<missing>' }
    return Normalize-Commands ([IO.File]::ReadAllText($Path))
}

function Add-Contract([string]$Path, [string]$Expected) {
    $script:Expected[$Path] = Normalize-Commands $Expected
    $script:Actual[$Path] = Read-Source (Join-Path $smoke $Path)
}

function Convert-HarnessReference([string]$Text) {
    $cellDiagnostics = @()
    $lines = foreach ($sourceLine in ($Text -split "`n")) {
        $line = $sourceLine.Replace('four once-summoned registered exosuit items',
                'five once-summoned registered Voltaic Harness items').
            Replace('first-exosuit', 'first-harness').
            Replace('grindless_exosuit', 'grindless_harness').
            Replace('exosuit_check', 'harness_check').
            Replace('flux_exosuit_helmet', 'voltaic_helmet').
            Replace('flux_exosuit_chestplate', 'voltaic_chestplate').
            Replace('network_tap', 'voltaic_leggings').
            Replace('exoskeleton_legs', 'voltaic_boots').
            Replace('tap_spawn', '@LEGS@').Replace('legs_spawn', 'boots_spawn').Replace('@LEGS@', 'legs_spawn').
            Replace('TAP', '@THIRD@').Replace('LEGS', 'BOOTS').Replace('@THIRD@', 'LEGS').
            Replace('EXO-HELMET-OK', 'HELMET-OK').Replace('EXO-CHEST-OK', 'CHEST-OK').
            Replace('BOOTS-MOD-OK', 'BOOTS-OK').Replace('EXOSUIT', 'HARNESS')
        if ($line.Contains('run scoreboard players set observed harness_check 1')) {
            $selector = ' if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:flux_cell",Count:1b}}]'
            $line = $line.Replace(' run scoreboard players set observed', $selector + ' run scoreboard players set observed')
        }
        if ($line -eq 'scoreboard players set phase harness_check -1') { $cellDiagnostics }
        $line
        if ($line -eq 'scoreboard players set boots_spawn harness_check -1') {
            'scoreboard players set cell_spawn harness_check -1'
        }
        if ($line.Contains('run summon minecraft:item') -and $line.Contains('grindless:voltaic_boots')) {
            $line.Replace('boots_spawn', 'cell_spawn').Replace('voltaic_boots', 'flux_cell')
        }
        if ($line -eq 'say BOOTS-OK' -or $line -eq 'BOOTS-OK') { $line.Replace('BOOTS', 'CELL') }
        if ($line.Contains('DIAG-SUMMON-BOOTS-') -or $line.Contains('DIAG-ENTITY-BOOTS-')) {
            $cellDiagnostics += $line.Replace('boots_spawn', 'cell_spawn').
                Replace('voltaic_boots', 'flux_cell').Replace('BOOTS', 'CELL')
        }
    }
    return ($lines -join "`n")
}

$functions = @('begin', 'spawn', 'poll', 'passed', 'failed', 'cleanup')
$harnessPaths = @('first-harness.commands', 'first-harness.expect', 'first-harness-pack\pack.mcmeta') +
    @($functions | ForEach-Object { "first-harness-pack\data\grindless_harness\functions\$_.mcfunction" })
foreach ($path in $harnessPaths) {
    $reference = $path.Replace('first-harness', 'first-exosuit').Replace('grindless_harness', 'grindless_exosuit')
    Add-Contract $path (Convert-HarnessReference (Read-Source (Join-Path $smoke $reference)))
}

Add-Contract 'first-fluids.commands' 'execute in minecraft:overworld run function grindless_fluids:begin'
Add-Contract 'first-fluids.expect' @'
PUMP-OK
PIPE-OK
TANK-OK
WAIT-FLUID
WATER-IN
CO-BURN
'@
Add-Contract 'first-fluids-pack\pack.mcmeta' @'
{
  "pack": {
    "pack_format": 15,
    "description": "Bounded observation of the real pump/conduit/tank and CO fuel bindings"
  }
}
'@
$fluidFunctions = [ordered]@{
    begin = @'
scoreboard objectives add fluids_check dummy
scoreboard players set phase fluids_check 0
scoreboard players set tries fluids_check 0
scoreboard players set water_seen fluids_check 0
scoreboard players set co_seen fluids_check 0
forceload add 0 0
schedule function grindless_fluids:poll 1t replace
'@
    setup = @'
scoreboard players set phase fluids_check 1
fill 6 68 6 12 73 12 minecraft:stone
fill 7 70 7 11 72 11 minecraft:air
fill 7 69 7 9 70 9 minecraft:stone
setblock 8 71 8 grindless:hand_pump[facing=south,status=idle]
execute if block 8 71 8 grindless:hand_pump[facing=south] run say PUMP-OK
setblock 8 71 9 grindless:clay_conduit[facing=south,status=idle]
execute if block 8 71 9 grindless:clay_conduit[facing=south] run say PIPE-OK
setblock 8 71 10 grindless:basic_tank[facing=south,status=idle]
execute if block 8 71 10 grindless:basic_tank[facing=south] run say TANK-OK
setblock 8 70 8 minecraft:water[level=0]
setblock 10 70 8 grindless:basic_tank[facing=south,status=idle]{Fluid:{Id:"grindless:carbon_monoxide",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 10 70 9 grindless:thermal_generator[facing=south,status=idle]
say WAIT-FLUID
'@
    poll = @'
scoreboard players add tries fluids_check 1
execute if score phase fluids_check matches 0 if loaded 8 71 8 run function grindless_fluids:setup
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 0 store result score water_amount fluids_check run data get block 8 71 10 Fluid.Amount
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 0 if score water_amount fluids_check matches 1.. if data block 8 71 10 Fluid{Id:"minecraft:water"} run say WATER-IN
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 0 if score water_amount fluids_check matches 1.. if data block 8 71 10 Fluid{Id:"minecraft:water"} run scoreboard players set water_seen fluids_check 1
execute if score phase fluids_check matches 1 if score co_seen fluids_check matches 0 store result score burn fluids_check run data get block 10 70 9 Burn
execute if score phase fluids_check matches 1 if score co_seen fluids_check matches 0 if score burn fluids_check matches 1.. run say CO-BURN
execute if score phase fluids_check matches 1 if score co_seen fluids_check matches 0 if score burn fluids_check matches 1.. run scoreboard players set co_seen fluids_check 1
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 1 if score co_seen fluids_check matches 1 run function grindless_fluids:passed
execute if score phase fluids_check matches 0..1 if score tries fluids_check matches ..159 run schedule function grindless_fluids:poll 5t replace
execute if score phase fluids_check matches 0..1 if score tries fluids_check matches 160.. run function grindless_fluids:failed
'@
    passed = @'
scoreboard players set phase fluids_check 2
function grindless_fluids:cleanup
'@
    failed = @'
say FLUIDS-FAIL-CHUNK-OR-TRANSFER-TIMEOUT
say FLUIDS-DIAG-SOURCE-first-fluids-pack-DIM-minecraft:overworld-CHUNK-0,0
say FLUIDS-DIAG-BUDGET-160-POLLS-5t-INTERVAL-RUNNER-DEFAULT-120s
execute if score phase fluids_check matches 0 run say FLUIDS-DIAG-PHASE-CHUNK-WAIT
execute if score phase fluids_check matches 1 run say FLUIDS-DIAG-PHASE-TRANSFER-WAIT
execute if loaded 8 71 8 run say FLUIDS-DIAG-CHUNK-LOADED
execute unless loaded 8 71 8 run say FLUIDS-DIAG-CHUNK-NOT-LOADED
execute if block 8 70 8 minecraft:water[level=0] run say FLUIDS-DIAG-SOURCE-WATER
execute unless block 8 70 8 minecraft:water[level=0] run say FLUIDS-DIAG-SOURCE-NOT-WATER
execute if block 8 71 8 grindless:hand_pump[facing=south] run say FLUIDS-DIAG-PUMP-PLACED
execute unless block 8 71 8 grindless:hand_pump[facing=south] run say FLUIDS-DIAG-PUMP-NOT-PLACED
execute if data block 8 71 8 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PUMP-HAS-WATER
execute unless data block 8 71 8 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PUMP-NO-WATER
execute if data block 8 71 9 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PIPE-HAS-WATER
execute unless data block 8 71 9 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PIPE-NO-WATER
execute if data block 8 71 10 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-TANK-HAS-WATER
execute unless data block 8 71 10 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-TANK-NO-WATER
execute if data block 10 70 8 Fluid{Id:"grindless:carbon_monoxide"} run say FLUIDS-DIAG-CO-TANK-HAS-CO
execute unless data block 10 70 8 Fluid{Id:"grindless:carbon_monoxide"} run say FLUIDS-DIAG-CO-TANK-NO-CO
execute if data block 10 70 9 Burn run say FLUIDS-DIAG-GENERATOR-HAS-BURN-FIELD
execute unless data block 10 70 9 Burn run say FLUIDS-DIAG-GENERATOR-MISSING-BURN-FIELD
execute store result score failed_burn fluids_check run data get block 10 70 9 Burn
execute if score failed_burn fluids_check matches 1.. run say FLUIDS-DIAG-CURRENT-BURN-POSITIVE
execute if score failed_burn fluids_check matches 0 run say FLUIDS-DIAG-CURRENT-BURN-ZERO
execute if score water_seen fluids_check matches 0 run say FLUIDS-DIAG-WATER-NOT-OBSERVED
execute if score water_seen fluids_check matches 1 run say FLUIDS-DIAG-WATER-OBSERVED
execute if score co_seen fluids_check matches 0 run say FLUIDS-DIAG-BURN-NOT-OBSERVED
execute if score co_seen fluids_check matches 1 run say FLUIDS-DIAG-BURN-OBSERVED
scoreboard players set phase fluids_check -1
function grindless_fluids:cleanup
'@
    cleanup = 'forceload remove 0 0'
}
foreach ($entry in $fluidFunctions.GetEnumerator()) {
    Add-Contract "first-fluids-pack\data\grindless_fluids\functions\$($entry.Key).mcfunction" $entry.Value
}

foreach ($name in @('harness', 'fluids')) {
    $pack = Join-Path $smoke "first-$name-pack"
    $names = if ($name -eq 'harness') { $functions } else { @($fluidFunctions.Keys) }
    $inventory = @('pack.mcmeta') + @($names | ForEach-Object { "data\grindless_$name\functions\$_.mcfunction" })
    $script:Expected["$name inventory"] = (@($inventory | Sort-Object -CaseSensitive) -join "`n")
    $script:Actual["$name inventory"] = if (Test-Path -LiteralPath $pack -PathType Container) {
        (@(Get-ChildItem -LiteralPath $pack -Recurse -File |
            ForEach-Object { $_.FullName.Substring($pack.Length + 1).Replace('/', '\') } |
            Sort-Object -CaseSensitive) -join "`n")
    } else { '<missing>' }
    $script:Expected["$name timeout"] = 'absent'
    $script:Actual["$name timeout"] = if (Test-Path -LiteralPath (Join-Path $smoke "first-$name.timeout")) { 'present' } else { 'absent' }
}

function Test-Snapshot([System.Collections.IDictionary]$Source) {
    if ($Source.Count -ne $script:Expected.Count) { return $false }
    foreach ($key in $script:Expected.Keys) {
        if (-not $Source.Contains($key) -or $Source[$key] -cne $script:Expected[$key]) { return $false }
    }
    return $true
}

$failures = @()
foreach ($key in $script:Expected.Keys) {
    if ($script:Actual[$key] -cne $script:Expected[$key]) { $failures += $key; Write-Output "  FAIL $key" }
    else { Write-Output "  ok   $key" }
}
if ($failures.Count -gt 0) { throw ("Source contract failure(s): " + ($failures -join ', ')) }
$crlf = [ordered]@{}
foreach ($key in $script:Actual.Keys) { $crlf[$key] = Normalize-Commands ($script:Actual[$key].Replace("`n", "`r`n")) }
if (-not (Test-Snapshot $crlf)) { throw 'CRLF normalization changed the source contract.' }
Write-Output '  ok   CRLF input'

$hp = 'first-harness-pack\data\grindless_harness\functions\'
$fp = 'first-fluids-pack\data\grindless_fluids\functions\'
$mutations = @(
    @{ Name = 'legacy harness console'; Key = 'first-harness.commands'; Old = $script:Actual['first-harness.commands']; New = 'forceload add 0 0' }
    @{ Name = 'legacy fluids console'; Key = 'first-fluids.commands'; Old = $script:Actual['first-fluids.commands']; New = 'say WAIT-FLUID' }
    @{ Name = 'harness loaded barrier'; Key = $hp + 'poll.mcfunction'; Old = 'if loaded 8 100 8 '; New = '' }
    @{ Name = 'harness repeated summon'; Key = $hp + 'poll.mcfunction'; Old = 'scoreboard players add tries harness_check 1'; New = "scoreboard players add tries harness_check 1`nfunction grindless_harness:spawn" }
    @{ Name = 'harness wrong helmet'; Key = $hp + 'poll.mcfunction'; Old = 'grindless:voltaic_helmet'; New = 'minecraft:stick' }
    @{ Name = 'harness exact count'; Key = $hp + 'poll.mcfunction'; Old = ',Count:1b'; New = '' }
    @{ Name = 'harness fifth item required'; Key = $hp + 'poll.mcfunction'; Old = ' if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:flux_cell",Count:1b}}]'; New = '' }
    @{ Name = 'harness entity ownership'; Key = $hp + 'spawn.mcfunction'; Old = 'Tags:["grindless_harness_smoke"],'; New = '' }
    @{ Name = 'harness incidental hazards'; Key = $hp + 'spawn.mcfunction'; Old = 'NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,'; New = '' }
    @{ Name = 'harness command success is not observation'; Key = $hp + 'poll.mcfunction'; Old = 'if score observed harness_check matches 1'; New = 'if score helmet_spawn harness_check matches 1' }
    @{ Name = 'harness false failure marker'; Key = $hp + 'failed.mcfunction'; Old = 'say HARNESS-FAIL-CHUNK-OR-ENTITY-TIMEOUT'; New = "say HELMET-OK" }
    @{ Name = 'harness unbounded poll'; Key = $hp + 'poll.mcfunction'; Old = 'if score tries harness_check matches ..159 '; New = '' }
    @{ Name = 'harness global cleanup'; Key = $hp + 'cleanup.mcfunction'; Old = 'kill @e[type=minecraft:item,tag=grindless_harness_smoke]'; New = 'kill @e[type=item]' }
    @{ Name = 'fluids loaded barrier'; Key = $fp + 'poll.mcfunction'; Old = 'if loaded 8 71 8 '; New = '' }
    @{ Name = 'fluids source omitted'; Key = $fp + 'setup.mcfunction'; Old = 'setblock 8 70 8 minecraft:water[level=0]'; New = 'setblock 8 70 8 minecraft:air' }
    @{ Name = 'fluids exposed source'; Key = $fp + 'setup.mcfunction'; Old = 'fill 6 68 6 12 73 12 minecraft:stone'; New = 'fill 6 68 6 12 73 12 minecraft:air' }
    @{ Name = 'fluids prefilled destination'; Key = $fp + 'setup.mcfunction'; Old = 'setblock 8 71 10 grindless:basic_tank[facing=south,status=idle]'; New = 'setblock 8 71 10 grindless:basic_tank[facing=south,status=idle]{Fluid:{Id:"minecraft:water",Amount:1000}}' }
    @{ Name = 'fluids wrong identity'; Key = $fp + 'poll.mcfunction'; Old = 'Fluid{Id:"minecraft:water"}'; New = 'Fluid{Id:"grindless:carbon_monoxide"}' }
    @{ Name = 'fluids zero water'; Key = $fp + 'poll.mcfunction'; Old = 'if score water_amount fluids_check matches 1..'; New = 'if score water_amount fluids_check matches 0..' }
    @{ Name = 'fluids missing Burn is not a burn'; Key = $fp + 'poll.mcfunction'; Old = 'if score burn fluids_check matches 1..'; New = 'unless data block 10 70 9 {Burn:0}' }
    @{ Name = 'fluids both independent observations'; Key = $fp + 'poll.mcfunction'; Old = 'if score co_seen fluids_check matches 1 run function'; New = 'run function' }
    @{ Name = 'fluids observation not wall time'; Key = $fp + 'poll.mcfunction'; Old = 'if score water_seen fluids_check matches 1'; New = 'if score tries fluids_check matches 20..' }
    @{ Name = 'fluids poll bound'; Key = $fp + 'poll.mcfunction'; Old = 'if score tries fluids_check matches ..159 '; New = '' }
    @{ Name = 'fluids failure cannot emit success'; Key = $fp + 'failed.mcfunction'; Old = 'say FLUIDS-FAIL-CHUNK-OR-TRANSFER-TIMEOUT'; New = 'say WATER-IN' }
    @{ Name = 'harness expectations unchanged'; Key = 'first-harness.expect'; Old = 'HELMET-OK'; New = 'SMOKE-READY' }
    @{ Name = 'fluids expectations unchanged'; Key = 'first-fluids.expect'; Old = 'WATER-IN'; New = 'SMOKE-READY' }
    @{ Name = 'harness outer budget'; Key = 'harness timeout'; Old = 'absent'; New = 'present' }
    @{ Name = 'fluids outer budget'; Key = 'fluids timeout'; Old = 'absent'; New = 'present' }
)
foreach ($case in $mutations) {
    if (-not $script:Actual[$case.Key].Contains($case.Old)) { throw ('Stale mutation: ' + $case.Name) }
    $mutated = [ordered]@{}
    foreach ($key in $script:Actual.Keys) { $mutated[$key] = $script:Actual[$key] }
    $mutated[$case.Key] = $mutated[$case.Key].Replace($case.Old, $case.New)
    if (Test-Snapshot $mutated) { throw ('Accepted invalid source: ' + $case.Name) }
    Write-Output ('  reject ' + $case.Name)
}
Write-Output ("ALL FIRST OBSERVATION FIXTURE SOURCE CHECKS PASSED: {0} positive, {1} negative (NOT A MINECRAFT RUN)" -f
    ($script:Expected.Count + 1), $mutations.Count)
