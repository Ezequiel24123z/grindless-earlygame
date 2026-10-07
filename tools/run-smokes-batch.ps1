# Fast, shared-world counterpart to run-smokes.ps1.
#
# It starts Forge once, runs the ordinary smoke command files in order and checks every scenario's
# expected markers. This is for the local development loop; run-smokes.ps1 remains the isolated
# integration check because it recreates the world for every scenario.
#
#     $env:GRINDLESS_ACCEPT_EULA='true'
#     powershell -ExecutionPolicy Bypass -File .\tools\run-smokes-batch.ps1 -Root .
#
# Choose a small route while working on it:
#
#     ... -Root . -Only first-iron,first-factory,first-washer

param(
    [Parameter(Mandatory = $true)][string]$Root,
    [string[]]$Only
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path $Root).Path
$smokeDir = Join-Path $Root 'tools\smoke'
$runDir = Join-Path $Root 'forge\run'
$utf8 = New-Object System.Text.UTF8Encoding($false)
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

$all = @()
foreach ($line in [System.IO.File]::ReadAllLines((Join-Path $smokeDir 'scenarios.txt'), $utf8)) {
    $trimmed = $line.Trim()
    if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
    $all += ($trimmed -split '\s+', 2)[0]
}

$onlyNames = @($Only | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne '' })
$selected = if ($onlyNames.Count -gt 0) { @($all | Where-Object { $onlyNames -contains $_ }) } else { $all }
if ($selected.Count -eq 0) { throw 'No named smoke scenarios were found.' }

# These scenarios need a pack enabled while a fresh world is created. Enabling both in one shared
# world would hide the provider and function-isolation guarantees they are meant to prove.
$isolated = @('foreign-providers', 'states')
$skipped = @($selected | Where-Object { $isolated -contains $_ })
$batch = @($selected | Where-Object { $isolated -notcontains $_ })
if ($batch.Count -eq 0) {
    throw 'The requested scenario(s) require a fresh datapack world; use tools\run-smokes.ps1.'
}

$commands = New-Object 'System.Collections.Generic.List[string]'
$expect = New-Object 'System.Collections.Generic.List[string]'
foreach ($name in $batch) {
    $commands.Add("say BATCH-BEGIN $name")
    $commandFile = Join-Path $smokeDir "$name.commands"
    if (Test-Path -LiteralPath $commandFile) {
        foreach ($command in [System.IO.File]::ReadAllLines($commandFile, $utf8)) {
            if ([string]::IsNullOrWhiteSpace($command) -or $command.TrimStart().StartsWith('#')) { continue }
            $commands.Add($command)
        }
    }
    $commands.Add("say BATCH-END $name")
    $expect.Add("BATCH-END $name")

    $expectFile = Join-Path $smokeDir "$name.expect"
    if (Test-Path -LiteralPath $expectFile) {
        foreach ($pattern in [System.IO.File]::ReadAllLines($expectFile, $utf8)) {
            if (-not [string]::IsNullOrWhiteSpace($pattern)) { $expect.Add($pattern) }
        }
    }
}

$commandPath = Join-Path $runDir 'smoke-batch.commands'
$expectPath = Join-Path $runDir 'smoke-batch.expect'
[System.IO.File]::WriteAllLines($commandPath, $commands, $utf8)
[System.IO.File]::WriteAllLines($expectPath, $expect, $utf8)

Write-Output "Running $($batch.Count) shared-world smoke scenario(s) in one Forge boot."
if ($skipped.Count -gt 0) {
    Write-Output "Skipped fresh-world datapack scenario(s): $($skipped -join ', ')."
}

$env:SMOKE_COMMANDS = $commandPath
$env:SMOKE_EXPECT_FILE = $expectPath
$env:SMOKE_LOG = Join-Path $runDir 'smoke-batch.log'
$env:SMOKE_DATAPACK = $null
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root 'tools\smoke-boot.ps1') -Root $Root
exit $LASTEXITCODE
