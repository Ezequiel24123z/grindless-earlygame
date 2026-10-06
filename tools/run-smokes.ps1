# Runs the smoke scenarios listed in tools/smoke/scenarios.txt.
#
#     $env:GRINDLESS_ACCEPT_EULA='true'
#     powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root .
#
# Run one scenario, or a few:
#     ... -Root . -Only states,first-exosuit
#
# Run one slice of the list, which is how CI splits 39 server boots across parallel jobs:
#     ... -Root . -Shard 1 -Of 6
#
# Each scenario boots a full dedicated server, so the whole list takes tens of minutes. That is
# the point: `./gradlew build` and the behaviour checks never instantiate Forge, so only a boot
# finds a mod that crashes while loading (ADR-0049).
#
# While it runs it rewrites forge/run/smoke-report.html after every scenario, a self-contained
# page that refreshes itself. A forty-minute run is otherwise a silent terminal, and the desk
# is where branches are validated (ADR-0106). Open it with -Report, or just open the file.
#
# Scenario conventions are documented at the top of tools/smoke/scenarios.txt.

param(
    [Parameter(Mandatory = $true)][string]$Root,
    [string[]]$Only,
    [int]$Shard = 0,
    [int]$Of = 0,
    [switch]$Report,
    [switch]$List
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path $Root).Path
$smokeDir = Join-Path $Root 'tools\smoke'
$runDir = Join-Path $Root 'forge\run'
$utf8 = New-Object System.Text.UTF8Encoding($false)
New-Item -ItemType Directory -Force -Path $runDir | Out-Null
$reportPath = Join-Path $runDir 'smoke-report.html'

$all = @()
foreach ($line in [System.IO.File]::ReadAllLines((Join-Path $smokeDir 'scenarios.txt'), $utf8)) {
    $trimmed = $line.Trim()
    if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
    $parts = $trimmed -split '\s+', 2
    $all += [pscustomobject]@{
        Name    = $parts[0]
        Proves  = if ($parts.Count -gt 1) { $parts[1].Trim() } else { '' }
        Status  = 'pending'
        Seconds = 0
    }
}

$selected = $all
if ($Only) { $selected = @($all | Where-Object { $Only -contains $_.Name }) }

if ($Of -gt 0) {
    if ($Shard -lt 1 -or $Shard -gt $Of) { throw "Shard must be between 1 and $Of" }
    # Round-robin rather than contiguous blocks: the long scenarios are not evenly spread
    # through the list, and striping keeps the shards closer to the same wall-clock time.
    $names = @($selected | ForEach-Object { $_.Name })
    $selected = @($selected | Where-Object { $names.IndexOf($_.Name) % $Of -eq ($Shard - 1) })
}

if ($selected.Count -eq 0) {
    Write-Output 'No scenarios selected.'
    exit 1
}

if ($List) {
    $selected | ForEach-Object { Write-Output $_.Name }
    exit 0
}

$startedAt = Get-Date

function Format-Span([double]$seconds) {
    $span = [TimeSpan]::FromSeconds([math]::Round($seconds))
    if ($span.TotalHours -ge 1) { return '{0}h {1:00}m' -f [int]$span.TotalHours, $span.Minutes }
    if ($span.TotalMinutes -ge 1) { return '{0}m {1:00}s' -f [int]$span.TotalMinutes, $span.Seconds }
    return '{0}s' -f $span.Seconds
}

function Write-Report([string]$title) {
    $done = @($selected | Where-Object { $_.Status -in @('passed', 'failed') })
    $passed = @($selected | Where-Object { $_.Status -eq 'passed' }).Count
    $failed = @($selected | Where-Object { $_.Status -eq 'failed' }).Count
    $running = @($selected | Where-Object { $_.Status -eq 'running' })
    $elapsed = ((Get-Date) - $startedAt).TotalSeconds
    $percent = if ($selected.Count -gt 0) { [math]::Round(100.0 * $done.Count / $selected.Count) } else { 0 }

    $eta = ''
    if ($done.Count -gt 0 -and $done.Count -lt $selected.Count) {
        $average = ($done | Measure-Object -Property Seconds -Average).Average
        $eta = 'about ' + (Format-Span ($average * ($selected.Count - $done.Count))) + ' left'
    }

    $active = $running.Count -gt 0 -or $done.Count -lt $selected.Count
    $refresh = if ($active) { '<meta http-equiv="refresh" content="3">' } else { '' }

    $barClass = if ($failed -gt 0) { 'bad' } elseif ($active) { 'live' } else { 'good' }
    $headline = if ($failed -gt 0) { "$failed failed" } elseif ($active) { 'running' } else { 'all passed' }

    $rows = ''
    $index = 0
    foreach ($item in $selected) {
        $index++
        $logName = "smoke-$($item.Name).log"
        $time = if ($item.Seconds -gt 0) { Format-Span $item.Seconds } else { '' }
        $mark = switch ($item.Status) {
            'passed' { 'PASS' }
            'failed' { 'FAIL' }
            'running' { '...' }
            default { '' }
        }
        $link = if ($item.Status -in @('passed', 'failed')) { "<a href=`"$logName`">log</a>" } else { '' }
        $proves = [System.Net.WebUtility]::HtmlEncode($item.Proves)
        $rows += "<tr class=`"$($item.Status)`"><td class=`"n`">$index</td><td class=`"s`">$mark</td>" +
                 "<td class=`"name`">$($item.Name)</td><td class=`"p`">$proves</td>" +
                 "<td class=`"t`">$time</td><td class=`"l`">$link</td></tr>`n"
    }

    $nowText = (Get-Date).ToString('HH:mm:ss')
    $html = @"
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
$refresh
<title>$title</title>
<style>
 :root { color-scheme: dark light; }
 body { font: 14px/1.5 ui-monospace, "Cascadia Mono", Consolas, monospace;
        margin: 0; padding: 24px; background: #11141a; color: #d7dde8; }
 h1 { font-size: 18px; margin: 0 0 4px; letter-spacing: .3px; }
 .sub { color: #8b94a7; font-size: 12px; margin-bottom: 18px; }
 .bar { height: 10px; border-radius: 5px; background: #222733; overflow: hidden; margin-bottom: 8px; }
 .bar > div { height: 100%; width: ${percent}%; transition: width .4s ease; }
 .bar.live > div { background: #4b9fd5; }
 .bar.good > div { background: #3fb950; }
 .bar.bad  > div { background: #e5534b; }
 .stats { display: flex; gap: 18px; font-size: 12px; color: #8b94a7; margin-bottom: 20px; flex-wrap: wrap; }
 .stats b { color: #d7dde8; font-weight: 600; }
 table { border-collapse: collapse; width: 100%; }
 td { padding: 5px 10px; border-bottom: 1px solid #1c212b; vertical-align: top; }
 .n { color: #5c6578; text-align: right; width: 34px; }
 .s { width: 46px; font-weight: 700; }
 .name { width: 190px; }
 .p { color: #8b94a7; font-size: 12px; }
 .t { width: 70px; color: #8b94a7; text-align: right; font-size: 12px; }
 .l { width: 40px; font-size: 12px; }
 a { color: #4b9fd5; }
 tr.passed .s { color: #3fb950; }
 tr.failed .s { color: #e5534b; }
 tr.failed .name { color: #e5534b; font-weight: 700; }
 tr.running { background: #172030; }
 tr.running .s { color: #4b9fd5; }
 tr.pending .name, tr.pending .p { color: #4d5566; }
</style>
</head>
<body>
<h1>Grindless smoke scenarios &mdash; $headline</h1>
<div class="sub">Each row boots a headless Forge dedicated server (ADR-0049). Updated $nowText.</div>
<div class="bar $barClass"><div></div></div>
<div class="stats">
  <span><b>$($done.Count)</b> / $($selected.Count) done</span>
  <span><b>$passed</b> passed</span>
  <span><b>$failed</b> failed</span>
  <span>elapsed <b>$(Format-Span $elapsed)</b></span>
  <span>$eta</span>
</div>
<table>
$rows</table>
</body>
</html>
"@
    [System.IO.File]::WriteAllText($reportPath, $html, $utf8)
}

$title = if ($Of -gt 0) { "Grindless smokes shard $Shard/$Of" } else { 'Grindless smokes' }
Write-Report $title
Write-Output "Running $($selected.Count) smoke scenario(s)."
Write-Output "Live report: $reportPath"
if ($Report) { Start-Process $reportPath }

$failedNames = New-Object 'System.Collections.Generic.List[string]'
$index = 0
foreach ($item in $selected) {
    $index++
    Write-Output ''
    Write-Output "=== [$index/$($selected.Count)] $($item.Name) ==="
    $item.Status = 'running'
    Write-Report $title
    $scenarioStart = Get-Date

    $commands = Join-Path $smokeDir "$($item.Name).commands"
    $expectFile = Join-Path $smokeDir "$($item.Name).expect"
    $packNamed = Join-Path $smokeDir "$($item.Name)-pack"
    $packBare = Join-Path $smokeDir $item.Name

    $env:SMOKE_COMMANDS = if (Test-Path -LiteralPath $commands) { $commands } else { $null }
    $env:SMOKE_EXPECT_FILE = if (Test-Path -LiteralPath $expectFile) { $expectFile } else { $null }
    $env:SMOKE_EXPECT = $null
    if (Test-Path -LiteralPath $packNamed -PathType Container) {
        $env:SMOKE_DATAPACK = $packNamed
    } elseif (Test-Path -LiteralPath $packBare -PathType Container) {
        $env:SMOKE_DATAPACK = $packBare
    } else {
        $env:SMOKE_DATAPACK = $null
    }
    $env:SMOKE_LOG = Join-Path $runDir "smoke-$($item.Name).log"

    & powershell -ExecutionPolicy Bypass -File (Join-Path $Root 'tools\smoke-boot.ps1') -Root $Root
    $item.Seconds = ((Get-Date) - $scenarioStart).TotalSeconds

    if ($LASTEXITCODE -eq 0) {
        $item.Status = 'passed'
    } else {
        $item.Status = 'failed'
        $failedNames.Add($item.Name)
        Write-Output "SCENARIO FAILED: $($item.Name) (log: $($env:SMOKE_LOG))"
    }
    Write-Report $title
}

Write-Report $title
Write-Output ''
if ($failedNames.Count -eq 0) {
    Write-Output "ALL $($selected.Count) SMOKE SCENARIOS PASSED"
    exit 0
}
Write-Output "$($failedNames.Count) SMOKE SCENARIO(S) FAILED"
$failedNames | ForEach-Object { Write-Output "  $_" }
exit 1
