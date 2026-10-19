# Executes production pack-planning helpers and real runner dispatch with fake boots; no JVM.
param([string]$Root = (Join-Path $PSScriptRoot '..\..'))

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Root = (Resolve-Path -LiteralPath $Root).Path
$count = 0
function Assert([bool]$condition, [string]$label) {
    if (-not $condition) { throw "Optional datapack contract failed: $label" }
    $script:count++
}
function Reject([scriptblock]$action, [string]$label, [string]$diagnostic = '') {
    $rejected = $false
    try { $null = & $action } catch { $rejected = $_.Exception.Message.Contains($diagnostic) }
    Assert $rejected $label
}

$reloadFunctions = Join-Path $Root 'tools\smoke\recipe-cache-reload\data\grindless_cache_reload\functions'
$budgetSource = @{}
foreach ($name in @('begin', 'observe', 'after_reload', 'failed')) {
    $budgetSource[$name] = [IO.File]::ReadAllText((Join-Path $reloadFunctions ($name + '.mcfunction'))).Replace("`r`n", "`n")
}
$publishIncrement = 'execute if score phase cache_reload matches 2 run scoreboard players add publish_ticks cache_reload 1'
$wakeIncrement = 'execute if score phase cache_reload matches 3 if score reload_seen cache_reload matches 1 run scoreboard players add run_ticks cache_reload 1'
$confirmedReset = 'execute if score phase cache_reload matches 3 if score reload_seen cache_reload matches 1 run scoreboard players set run_ticks cache_reload 0'
function Test-ReloadBudgetContract([hashtable]$source) {
    $writes = @($source.observe -split "`n" | Where-Object { $_ -match 'scoreboard players add (publish_ticks|run_ticks) ' })
    if (($writes -join "`n") -cne ($publishIncrement + "`n" + $wakeIncrement)) { return $false }
    foreach ($counter in @('publish_ticks', 'run_ticks')) {
        if (-not $source.begin.Contains("scoreboard players set $counter cache_reload 0`n")) { return $false }
    }
    $confirmation = "execute if score tag_ok cache_reload matches 1 run scoreboard players set reload_seen cache_reload 1`n" +
        "execute if score tag_ok cache_reload matches 1 run scoreboard players set phase cache_reload 3`n" + $confirmedReset + "`n"
    if (-not $source.after_reload.Contains($confirmation)) { return $false }
    if (@($source.after_reload -split "`n" | Where-Object { $_ -match 'scoreboard players set run_ticks ' }).Count -ne 1) { return $false }
    foreach ($entry in @(@('2', '', 'publish_ticks', '8'), @('3', 'if score reload_seen cache_reload matches 1 ', 'run_ticks', '6'))) {
        $gate = "execute if score phase cache_reload matches $($entry[0]) $($entry[1])if score $($entry[2]) cache_reload matches 400.. run "
        if (-not $source.observe.Contains($gate + "scoreboard players set error cache_reload $($entry[3])`n") -or
                -not $source.observe.Contains($gate + "function grindless_cache_reload:failed`n")) { return $false }
    }
    return $source.failed.Contains('execute if score error cache_reload matches 8 run say CACHE-RELOAD-FAIL-PUBLICATION-TIMEOUT') -and
        $source.after_reload -notmatch 'scoreboard players (?:set|operation) (?:prev_paid|prev_time|paid) '
}
Assert (Test-ReloadBudgetContract $budgetSource) 'publication wait cannot consume or masquerade as confirmed wake budget'
$budgetMutations = @(
    @('observe', $wakeIncrement, 'scoreboard players add run_ticks cache_reload 1'),
    @('observe', $wakeIncrement, $wakeIncrement.Replace('matches 3 ', 'matches 2 ')),
    @('observe', $wakeIncrement, $wakeIncrement.Replace('if score reload_seen cache_reload matches 1 ', '')),
    @('after_reload', $confirmedReset, ''),
    @('observe', 'scoreboard players set error cache_reload 8', 'scoreboard players set error cache_reload 6'),
    @('observe', 'if score publish_ticks cache_reload matches 400.. ', ''),
    @('observe', 'if score run_ticks cache_reload matches 400.. ', 'if score run_ticks cache_reload matches 800.. ')
)
foreach ($mutation in $budgetMutations) {
    Assert ($budgetSource[$mutation[0]].Contains($mutation[1])) 'budget mutation has an actual source target'
    $changed = $budgetSource.Clone()
    $changed[$mutation[0]] = $changed[$mutation[0]].Replace($mutation[1], $mutation[2])
    Assert (-not (Test-ReloadBudgetContract $changed)) 'reject conflated, unconfirmed, absent-reset or relaxed budget'
}

$boot = Join-Path $Root 'tools\smoke-boot.ps1'
$tokens = $null; $errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile($boot, [ref]$tokens, [ref]$errors)
Assert ($errors.Count -eq 0) 'production boot parses'
foreach ($name in @('Assert-SmokePathAncestors', 'Resolve-SmokeDatapack', 'Get-SmokePackPlan', 'Copy-Tree')) {
    $definition = @($ast.FindAll({
        param($node)
        $node -is [Management.Automation.Language.FunctionDefinitionAst]
    }, $true) | Where-Object { $_.Name -eq $name })
    Assert ($definition.Count -eq 1) "one production $name helper"
    Invoke-Expression $definition[0].Extent.Text
}

$fixture = Join-Path $Root ('build\op-' + [guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($fixture)
$utf8 = New-Object Text.UTF8Encoding($false)
function Pack([string]$relative) {
    $path = Join-Path $fixture $relative
    [void][IO.Directory]::CreateDirectory($path)
    [IO.File]::WriteAllText((Join-Path $path 'pack.mcmeta'), '{"pack":{"pack_format":15,"description":"test"}}', $utf8)
    return $path
}
try {
    $primary = Pack 'primary'
    $optional = Pack 'optional'
    $world = Join-Path $fixture 'forge\run\smoke-server\smoke-world'
    $none = Get-SmokePackPlan '' '' $world
    Assert ($null -eq $none.Primary -and $null -eq $none.Optional -and $none.Properties -eq '') 'plain boot unchanged'
    $legacy = Get-SmokePackPlan $primary '' $world
    Assert ($legacy.Primary.Path -eq $primary -and $null -eq $legacy.Optional) 'legacy primary unchanged'
    Assert ($legacy.Properties -ceq "initial-enabled-packs=vanilla,file/primary`n") 'legacy properties unchanged'
    $plan = Get-SmokePackPlan $primary $optional $world
    Assert ($plan.Properties -ceq "initial-enabled-packs=vanilla,file/primary`ninitial-disabled-packs=file/optional`n") 'optional explicitly disabled, not merely omitted'
    Assert ($plan.Optional.Path -eq $optional -and $plan.Optional.Name -eq 'optional') 'optional staging identity'
    Copy-Tree $plan.Primary.Path (Join-Path $world 'datapacks\primary')
    Copy-Tree $plan.Optional.Path (Join-Path $world 'datapacks\optional')
    Assert ([IO.File]::Exists("\\?\$world\datapacks\primary\pack.mcmeta") -and
        [IO.File]::Exists("\\?\$world\datapacks\optional\pack.mcmeta")) 'both actual trees copied to distinct pack roots'
    $alone = Get-SmokePackPlan '' $optional $world
    Assert ($alone.Properties -ceq "initial-enabled-packs=vanilla`ninitial-disabled-packs=file/optional`n") 'optional-only boot preserves vanilla'
    Reject { Get-SmokePackPlan $primary $primary $world } 'same-name collision' 'name collision'
    $caseAlias = Pack 'elsewhere\PRIMARY'
    Reject { Get-SmokePackPlan $primary $caseAlias $world } 'case-folded collision' 'name collision'
    Reject { Get-SmokePackPlan $primary (Join-Path $fixture 'missing') $world } 'missing optional path'
    Reject { Get-SmokePackPlan $primary (Join-Path $primary 'pack.mcmeta') $world } 'file instead of directory'
    $noMeta = Join-Path $fixture 'no-metadata'; [void][IO.Directory]::CreateDirectory($noMeta)
    Reject { Get-SmokePackPlan $primary $noMeta $world } 'missing metadata' 'directory with pack.mcmeta'
    Reject { Get-SmokePackPlan $primary ' ' $world } 'whitespace path' 'empty path'
    $badName = Pack 'bad,name'
    Reject { Get-SmokePackPlan $primary $badName $world } 'properties delimiter in basename' 'datapack name'
    $badPrimary = Pack 'bad primary'
    Reject { Get-SmokePackPlan $badPrimary $optional $world } 'unsafe primary name in optional mode' 'datapack name'
    $inside = Pack 'forge\run\smoke-server\smoke-world\pack'
    Reject { Get-SmokePackPlan $primary $inside $world } 'source inside disposable world' 'overlaps'
    $ancestor = Pack 'forge'
    Reject { Get-SmokePackPlan $primary $ancestor $world } 'source contains disposable world' 'overlaps'
    $alias = Join-Path $fixture 'alias'
    $null = New-Item -ItemType Junction -Path $alias -Target $optional
    try { Reject { Get-SmokePackPlan $primary $alias $world } 'root reparse point' 'reparse points' } finally { [IO.Directory]::Delete($alias) }
    $alias = Join-Path $primary 'linked'
    $null = New-Item -ItemType Junction -Path $alias -Target $optional
    try { Reject { Get-SmokePackPlan $primary $optional $world } 'nested reparse point' 'reparse points' } finally { [IO.Directory]::Delete($alias) }
    $insideOptional = Pack 'forge\run\smoke-server\smoke-world\optional'
    $insidePrimary = Pack 'forge\run\smoke-server\smoke-world\primary'
    $sentinel = Join-Path $insideOptional 'retained.txt'
    [IO.File]::WriteAllText($sentinel, 'owned-fixture-source-must-survive', $utf8)
    $alias = Join-Path $fixture 'ancestor-alias'
    $null = New-Item -ItemType Junction -Path $alias -Target $world
    try {
        Reject { Get-SmokePackPlan $primary (Join-Path $alias 'optional') $world } 'optional source through a junction ancestor' 'reparse points'
        Reject { Get-SmokePackPlan (Join-Path $alias 'primary') $optional $world } 'primary source through a junction ancestor' 'reparse points'
        Assert ([IO.File]::ReadAllText($sentinel) -ceq 'owned-fixture-source-must-survive') 'rejected ancestor aliases preserve the actual source'
    } finally { [IO.Directory]::Delete($alias) }
    $physicalWorld = Pack 'real-root\forge\run\smoke-server\smoke-world'
    $alias = Join-Path $fixture 'root-alias'
    $null = New-Item -ItemType Junction -Path $alias -Target (Join-Path $fixture 'real-root')
    try {
        $logicalWorld = Join-Path $alias 'forge\run\smoke-server\smoke-world'
        Reject { Get-SmokePackPlan $primary $optional $logicalWorld } 'ROOT junction with an existing world' 'reparse points'
        Reject { Get-SmokePackPlan $primary $optional ($logicalWorld + '-next') } 'ROOT junction with a not-yet-created world' 'reparse points'
        Assert ([IO.File]::Exists((Join-Path $physicalWorld 'pack.mcmeta'))) 'rejected ROOT aliases preserve the actual world'
    } finally { [IO.Directory]::Delete($alias) }

    $bootSource = [IO.File]::ReadAllText($boot)
    Assert ($bootSource.IndexOf('$packPlan = Get-SmokePackPlan') -lt $bootSource.IndexOf("') --no-daemon")) 'preflight precedes Gradle and world replacement'
    Assert ($bootSource.Contains('Copy-Tree $packPlan.Optional.Path') -and
        $bootSource.Contains('$properties += $packPlan.Properties')) 'production stages and writes the tested plan'
    Assert ($bootSource.Contains('$psi.Arguments = "-Xmx2G ') -and
        $bootSource.Contains("'\[Server\] SMOKE-READY'")) 'native heap and handshake retained'

    $tools = Join-Path $fixture 'tools'; $scenarios = Join-Path $tools 'smoke'
    [void][IO.Directory]::CreateDirectory($scenarios)
    foreach ($script in @('run-smokes.ps1', 'run-smokes-batch.ps1')) {
        [IO.File]::Copy((Join-Path $Root ('tools\' + $script)), (Join-Path $tools $script))
    }
    [IO.File]::WriteAllText((Join-Path $scenarios 'scenarios.txt'), "alpha first`nbeta fails`ngamma last`nrecipe-cache-reload isolated`n", $utf8)
    foreach ($name in @('alpha', 'beta', 'gamma', 'recipe-cache-reload')) {
        [IO.File]::WriteAllText((Join-Path $scenarios ($name + '.commands')), "say $name`n", $utf8)
    }
    [void][IO.Directory]::CreateDirectory((Join-Path $scenarios 'alpha-optional'))
    [void][IO.Directory]::CreateDirectory((Join-Path $scenarios 'recipe-cache-reload-optional'))
    [IO.File]::WriteAllText((Join-Path $tools 'smoke-boot.ps1'), @'
param([string]$Root)
$name = [IO.Path]::GetFileNameWithoutExtension($env:SMOKE_COMMANDS)
$optional = if ($env:SMOKE_OPTIONAL_DATAPACK) { Split-Path $env:SMOKE_OPTIONAL_DATAPACK -Leaf } else { '<unset>' }
Add-Content -LiteralPath (Join-Path $Root 'calls.txt') -Encoding ASCII -Value "$name|$optional"
if ($name -eq 'beta') { exit 1 }
exit 0
'@, $utf8)
    $oldOptional = $env:SMOKE_OPTIONAL_DATAPACK
    try {
        $env:SMOKE_OPTIONAL_DATAPACK = 'caller-sentinel'
        $log = Join-Path $fixture 'dispatch.log'
        $caller = "& '" + (Join-Path $tools 'run-smokes.ps1').Replace("'", "''") +
            "' -Root '" + $fixture.Replace("'", "''") + "' -Only 'alpha,gamma'; " +
            '$code=$LASTEXITCODE; Write-Output ("RESTORED=" + $env:SMOKE_OPTIONAL_DATAPACK); exit $code'
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($caller))
        & powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded *> $log
        Assert ($LASTEXITCODE -eq 0) 'real normal runner succeeds with fake boots'
        $calls = [IO.File]::ReadAllLines((Join-Path $fixture 'calls.txt'))
        Assert (($calls -join ',') -ceq 'alpha|alpha-optional,gamma|<unset>') 'per-scenario discovery and no cross-scenario/caller leakage'
        Assert ([IO.File]::ReadAllText($log).Contains('RESTORED=caller-sentinel')) 'same-process caller restored after successful boots'
        Remove-Item -LiteralPath (Join-Path $fixture 'calls.txt')
        $caller = $caller.Replace("-Only 'alpha,gamma'", "-Only 'alpha,beta'")
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($caller))
        & powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded *> $log
        Assert ($LASTEXITCODE -eq 1) 'failed fake boot propagates'
        Assert ([IO.File]::ReadAllText($log).Contains('RESTORED=caller-sentinel')) 'same-process caller restored after failed boot'
        $calls = [IO.File]::ReadAllLines((Join-Path $fixture 'calls.txt'))
        Assert (($calls -join ',') -ceq 'alpha|alpha-optional,beta|<unset>') 'failed following scenario has no optional pack'
        Remove-Item -LiteralPath (Join-Path $fixture 'calls.txt')
        $caller = "& '" + (Join-Path $tools 'run-smokes-batch.ps1').Replace("'", "''") +
            "' -Root '" + $fixture.Replace("'", "''") + "' -Only 'alpha,gamma,recipe-cache-reload'; " +
            '$code=$LASTEXITCODE; Write-Output ("RESTORED=" + $env:SMOKE_OPTIONAL_DATAPACK); exit $code'
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($caller))
        & powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded *> $log
        Assert ($LASTEXITCODE -eq 0) 'mixed batch runs only compatible scenarios'
        Assert ([IO.File]::ReadAllText($log).Contains('RESTORED=caller-sentinel')) 'batch restores same-process caller'
        Assert ([IO.File]::ReadAllText((Join-Path $fixture 'calls.txt')).Trim() -ceq 'smoke-batch|<unset>') 'batch clears inherited optional pack'
        $batchCommands = [IO.File]::ReadAllText((Join-Path $fixture 'forge\run\smoke-batch.commands'))
        Assert ($batchCommands.Contains('BATCH-BEGIN gamma') -and -not $batchCommands.Contains('BATCH-BEGIN alpha') -and
            -not $batchCommands.Contains('BATCH-BEGIN recipe-cache-reload')) 'optional and named reload scenarios excluded from shared world'
        Remove-Item -LiteralPath (Join-Path $fixture 'calls.txt')
        try {
            $ErrorActionPreference = 'Continue'
            & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $tools 'run-smokes-batch.ps1') -Root $fixture -Only 'recipe-cache-reload' *> $log
            $rejectedCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = 'Stop' }
        Assert ($rejectedCode -ne 0 -and -not (Test-Path -LiteralPath (Join-Path $fixture 'calls.txt'))) 'isolated-only batch rejected before boot'
    } finally {
        $env:SMOKE_OPTIONAL_DATAPACK = $oldOptional
    }
    Write-Output ("ALL OPTIONAL DATAPACK CHECKS PASSED: {0} (no JVM or Minecraft)" -f $count)
} finally {
    [IO.Directory]::Delete($fixture, $true)
}
