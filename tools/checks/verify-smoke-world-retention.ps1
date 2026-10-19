# Exercises the real retention/cleanup helpers and rejected entrypoints without a JVM.
param([string]$Root = (Join-Path $PSScriptRoot '..\..'))
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Root = (Resolve-Path -LiteralPath $Root).ProviderPath
$count = 0
function Assert([bool]$condition, [string]$label) {
    if (-not $condition) { throw "Smoke retention contract failed: $label" }
    $script:count++
}
$boot = Join-Path $Root 'tools\smoke-boot.ps1'
$tokens = $null; $errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile($boot, [ref]$tokens, [ref]$errors)
Assert ($errors.Count -eq 0) 'production boot parses'
$definitions = ''
foreach ($name in @('Get-SmokeKeepWorld', 'Remove-Tree', 'Complete-SmokeWorld')) {
    $definition = @($ast.FindAll({
        param($node)
        $node -is [Management.Automation.Language.FunctionDefinitionAst]
    }, $true) | Where-Object { $_.Name -eq $name })
    Assert ($definition.Count -eq 1) "one actual $name helper"
    Invoke-Expression $definition[0].Extent.Text
    $definitions += $definition[0].Extent.Text + "`n"
}
Assert (-not (Get-SmokeKeepWorld $null)) 'absent flag means cleanup'
Assert (-not (Get-SmokeKeepWorld '0')) 'explicit zero means cleanup'
Assert (Get-SmokeKeepWorld '1') 'one means retain'
foreach ($value in @('', '2', 'true', '01', ' 1', '1 ', '-1', 'yes')) {
    $rejected = $false
    try { $null = Get-SmokeKeepWorld $value } catch { $rejected = $_.Exception.Message.Contains('Invalid SMOKE_KEEP_WORLD') }
    Assert $rejected 'reject every unsupported flag value'
}
$fixture = Join-Path $Root ('build\retain-' + [guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($fixture)
$utf8 = New-Object Text.UTF8Encoding($false)
function World([string]$name) {
    $path = Join-Path $fixture ($name + '\smoke-world')
    [void][IO.Directory]::CreateDirectory($path)
    [IO.File]::WriteAllText((Join-Path $path 'owned.dat'), 'owned-test-world', $utf8)
    return $path
}
try {
    $sibling = Join-Path $fixture 'not-the-world.txt'
    [IO.File]::WriteAllText($sibling, 'preserve-sibling', $utf8)
    foreach ($value in @($null, '0')) {
        $world = World ('default-' + [guid]::NewGuid().ToString('N').Substring(0, 6))
        $null = Complete-SmokeWorld $world (Get-SmokeKeepWorld $value)
        Assert (-not [IO.Directory]::Exists($world)) 'default/zero deletes only the named world'
        Assert ([IO.File]::ReadAllText($sibling) -ceq 'preserve-sibling') 'cleanup does not touch a sibling'
    }
    $world = World 'retained'
    $output = @(Complete-SmokeWorld $world (Get-SmokeKeepWorld '1')) -join "`n"
    Assert ([IO.File]::ReadAllText((Join-Path $world 'owned.dat')) -ceq 'owned-test-world') 'opt-in preserves exact world bytes'
    Assert ($output.Contains('SMOKE WORLD RETAINED') -and $output.Contains($world)) 'retention announces exact path/state'
    Assert ($output.Contains('not PASS or clean shutdown') -and -not $output.Contains('SMOKE BOOT PASSED')) 'retention cannot announce a smoke pass'
    $missing = Join-Path $fixture 'missing\smoke-world'
    $output = @(Complete-SmokeWorld $missing $true) -join "`n"
    Assert ($output.Contains('SMOKE WORLD ABSENT') -and $output.Contains($missing)) 'missing world is explicitly absent'
    Assert (-not [IO.Directory]::Exists($missing)) 'retention never creates a world'
    foreach ($code in @(0, 23)) {
        $exitWorld = World ('exit-' + $code)
        $script = $definitions + "`n" + "try { exit $code } finally { Complete-SmokeWorld '" +
            $exitWorld.Replace("'", "''") + "' (Get-SmokeKeepWorld '1') }"
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
        $log = Join-Path $fixture ('exit-' + $code + '.log')
        & powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded *> $log
        Assert ($LASTEXITCODE -eq $code) 'finally retention preserves the original process exit status'
        Assert ([IO.File]::ReadAllText($log).Contains('SMOKE WORLD RETAINED') -and
            [IO.File]::Exists((Join-Path $exitWorld 'owned.dat'))) 'both successful and failed exits retain diagnostic data'
    }
    $source = [IO.File]::ReadAllText($boot)
    Assert ($source.IndexOf('$keepWorld = Get-SmokeKeepWorld') -lt $source.IndexOf('$Root = Get-SmokeFileSystemPath')) 'flag validation precedes root/preparation/mutation'
    Assert ($source.Contains('Remove-Tree $world') -and
        $source.LastIndexOf('Complete-SmokeWorld $world $keepWorld') -gt $source.LastIndexOf('} finally {')) 'fresh-world reset stays unconditional; only final cleanup is optional'
    Assert ($source.Contains('$psi.Arguments = "-Xmx2G ') -and
        $source.Contains('if (-not $proc.HasExited) { Stop-Tree $proc.Id }')) 'retention does not bypass heap or process cleanup'

    $entryLog = Join-Path $fixture 'invalid-entry.log'
    $invalidScript = @'
$ErrorActionPreference='Stop'
$old=$env:SMOKE_KEEP_WORLD
$env:CI='false'; $env:GRINDLESS_ACCEPT_EULA='false'
try {
    foreach($value in @('2','true','01',' 1','1 ','-1','yes')) {
        $env:SMOKE_KEEP_WORLD=$value
        try {
            & '__BOOT__' -Root '__ROOT__'
            throw 'Unexpected accepted retention value'
        } catch {
            if(-not $_.Exception.Message.Contains('Invalid SMOKE_KEEP_WORLD')) { throw }
        }
    }
    Write-Output 'INVALID-ENTRYPOINTS-REJECTED'
} finally { $env:SMOKE_KEEP_WORLD=$old }
Write-Output ('ENV-RESTORED=' + $env:SMOKE_KEEP_WORLD)
'@
    $invalidScript = $invalidScript.Replace('__BOOT__', $boot.Replace("'", "''")).Replace('__ROOT__', $fixture.Replace("'", "''"))
    $originalKeep = $env:SMOKE_KEEP_WORLD
    try {
        $env:SMOKE_KEEP_WORLD = 'caller-sentinel'
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($invalidScript))
        & powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded *> $entryLog
        Assert ($LASTEXITCODE -eq 0 -and [IO.File]::ReadAllText($entryLog).Contains('INVALID-ENTRYPOINTS-REJECTED')) 'actual invalid entrypoints fail before preparation'
        Assert ([IO.File]::ReadAllText($entryLog).Contains('ENV-RESTORED=caller-sentinel')) 'entrypoint caller environment restored'
    } finally { $env:SMOKE_KEEP_WORLD = $originalKeep }
    Assert ([IO.File]::ReadAllText($sibling) -ceq 'preserve-sibling' -and
        -not [IO.Directory]::Exists((Join-Path $fixture 'forge'))) 'invalid entrypoints make no world/runtime mutation'
    $alias = Join-Path $fixture 'root-alias'
    $null = New-Item -ItemType Junction -Path $alias -Target (Split-Path $world -Parent)
    try {
        $guardScript = '$ErrorActionPreference=''Stop''; $env:SMOKE_KEEP_WORLD=''1''; $env:SMOKE_OPTIONAL_DATAPACK=''missing''; ' +
            '$env:CI=''false''; $env:GRINDLESS_ACCEPT_EULA=''false''; try { & ''' +
            $boot.Replace("'", "''") + "' -Root '" + $alias.Replace("'", "''") +
            "'; exit " + '$LASTEXITCODE } catch { Write-Output $_.Exception.Message; exit 91 }'
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($guardScript))
        $guardLog = Join-Path $fixture 'retention-preflight.log'
        & powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded *> $guardLog
        Assert ($LASTEXITCODE -eq 91 -and [IO.File]::ReadAllText($guardLog).Contains('reparse points')) 'retention still enforces the existing path preflight'
        Assert ([IO.File]::ReadAllText((Join-Path $world 'owned.dat')) -ceq 'owned-test-world') 'rejected retained-world alias does not mutate its destination'
    } finally { [IO.Directory]::Delete($alias) }
    $null = Complete-SmokeWorld $world $false
    Assert (-not [IO.Directory]::Exists($world)) 'explicit post-inspection cleanup removes the retained world'
    Write-Output ("ALL SMOKE WORLD RETENTION CHECKS PASSED: {0} (no JVM or Minecraft)" -f $count)
} finally {
    [IO.Directory]::Delete($fixture, $true)
}
