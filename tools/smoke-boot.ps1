# Windows twin of tools/smoke-boot.sh: boots a headless Forge dedicated server with the mod
# loaded and fails if it does not come up.
#
#     powershell -ExecutionPolicy Bypass -File .\tools\smoke-boot.ps1 -Root .
#
# Why this exists as a separate script rather than running the .sh under Git Bash: that script
# feeds the server console through a FIFO, and a Cygwin FIFO is emulated by Cygwin. The server
# is a native Windows java.exe, so it cannot read one. The boot itself succeeds and then every
# scenario fails at the handshake, which looks like a mod bug and is not. This script uses a
# real Win32 pipe through System.Diagnostics.Process instead.
#
# It writes eula.txt only when you opt in, because that records acceptance of the Minecraft
# EULA on your behalf:
#
#     $env:GRINDLESS_ACCEPT_EULA='true'; powershell -File .\tools\smoke-boot.ps1 -Root .
#
# Environment (identical contract to smoke-boot.sh, so CI and a desk agree):
#     SMOKE_TIMEOUT      seconds to wait for the server to finish starting (default 900)
#     SMOKE_PORT         server port (default 25599)
#     SMOKE_DATAPACK     a datapack directory to enable in the new world (ADR-0061)
#     SMOKE_COMMANDS     a file of console commands to run once the server is up
#     SMOKE_EXPECT       extended regexes, one per line, that must each appear in the log
#     SMOKE_EXPECT_FILE  the same, read from a file
#     SMOKE_LOG          where to write the server log

param([Parameter(Mandatory = $true)][string]$Root)

$ErrorActionPreference = 'Stop'

$Root = (Resolve-Path $Root).Path
$runDir = Join-Path $Root 'forge\run'
$log = if ($env:SMOKE_LOG) { $env:SMOKE_LOG } else { Join-Path $runDir 'smoke-boot.log' }
$timeout = if ($env:SMOKE_TIMEOUT) { [int]$env:SMOKE_TIMEOUT } else { 900 }
$port = if ($env:SMOKE_PORT) { $env:SMOKE_PORT } else { '25599' }
$datapack = $env:SMOKE_DATAPACK
$commandsFile = $env:SMOKE_COMMANDS

$expect = $env:SMOKE_EXPECT
if ($env:SMOKE_EXPECT_FILE) {
    $expect = [System.IO.File]::ReadAllText($env:SMOKE_EXPECT_FILE)
}

if ($env:GRINDLESS_ACCEPT_EULA -ne 'true' -and $env:CI -ne 'true') {
    Write-Output 'Refusing to write eula.txt on your behalf.'
    Write-Output 'Re-run with $env:GRINDLESS_ACCEPT_EULA=''true'' to accept the Minecraft EULA (https://aka.ms/MinecraftEULA).'
    exit 2
}

# A datapack path inside forge/run/smoke-world/datapacks/<pack>/data/... runs past MAX_PATH on
# a checkout that is already deep, and Copy-Item then fails with a DirectoryNotFoundException
# naming the leaf file, which looks nothing like a path-length problem (ADR-0024). Prefix the
# extended-length marker and copy through .NET instead.
function Copy-Tree([string]$source, [string]$destination) {
    $source = [System.IO.Path]::GetFullPath($source).TrimEnd('\')
    $destination = [System.IO.Path]::GetFullPath($destination).TrimEnd('\')
    [void][System.IO.Directory]::CreateDirectory("\\?\$destination")
    foreach ($dir in [System.IO.Directory]::GetDirectories($source, '*', [System.IO.SearchOption]::AllDirectories)) {
        [void][System.IO.Directory]::CreateDirectory("\\?\" + $dir.Replace($source, $destination))
    }
    foreach ($file in [System.IO.Directory]::GetFiles($source, '*', [System.IO.SearchOption]::AllDirectories)) {
        [System.IO.File]::Copy("\\?\$file", "\\?\" + $file.Replace($source, $destination), $true)
    }
}

function Remove-Tree([string]$path) {
    $full = [System.IO.Path]::GetFullPath($path).TrimEnd('\')
    if ([System.IO.Directory]::Exists("\\?\$full")) {
        [System.IO.Directory]::Delete("\\?\$full", $true)
    }
}

New-Item -ItemType Directory -Force -Path $runDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Join-Path $runDir 'eula.txt'), "eula=true`n", $utf8)

$properties = "online-mode=false`nserver-port=$port`nlevel-name=smoke-world`nview-distance=2`nsimulation-distance=2`n"
$world = Join-Path $runDir 'smoke-world'
Remove-Tree $world
if ($datapack) {
    $packName = Split-Path $datapack -Leaf
    Copy-Tree $datapack (Join-Path $world "datapacks\$packName")
    # A new world only enables the packs named here; the others are found but left off.
    $properties += "initial-enabled-packs=vanilla,file/$packName`n"
}
[System.IO.File]::WriteAllText((Join-Path $runDir 'server.properties'), $properties, $utf8)
[System.IO.File]::WriteAllText($log, '', $utf8)

# Gradle's output arrives on background threads, so collect it in a concurrent queue and drain
# it from the polling loop. Reading a file that another thread is still writing is the
# alternative, and on Windows that means fighting the share mode for no benefit.
$queue = New-Object 'System.Collections.Concurrent.ConcurrentQueue[string]'

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = Join-Path $Root 'gradlew.bat'
$psi.Arguments = '--no-daemon :forge:runServer'
$psi.WorkingDirectory = $Root
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
$proc.EnableRaisingEvents = $true

$onData = {
    if ($null -ne $EventArgs.Data) { $Event.MessageData.Enqueue($EventArgs.Data) }
}
$stdoutEvent = Register-ObjectEvent -InputObject $proc -EventName OutputDataReceived -Action $onData -MessageData $queue
$stderrEvent = Register-ObjectEvent -InputObject $proc -EventName ErrorDataReceived -Action $onData -MessageData $queue

$script:text = ''
# Appending the whole buffer each time would duplicate it, so track what has been flushed.
$script:flushed = 0

function Pump {
    $line = $null
    while ($queue.TryDequeue([ref]$line)) { $script:text += $line + "`n" }
    if ($script:text.Length -gt $script:flushed) {
        [System.IO.File]::AppendAllText($log, $script:text.Substring($script:flushed), $utf8)
        $script:flushed = $script:text.Length
    }
}

# Stop-Process does not take the tree, and a cancelled run leaves Gradle's forked server behind
# holding the port. Walk the parent links and stop each descendant by id.
function Stop-Tree([int]$processId) {
    $all = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Select-Object ProcessId, ParentProcessId
    $targets = New-Object 'System.Collections.Generic.List[int]'
    $frontier = New-Object 'System.Collections.Generic.List[int]'
    $frontier.Add($processId)
    while ($frontier.Count -gt 0) {
        $current = $frontier[0]; $frontier.RemoveAt(0)
        $targets.Add($current)
        foreach ($p in $all) {
            if ($p.ParentProcessId -eq $current -and -not $targets.Contains([int]$p.ProcessId)) {
                $frontier.Add([int]$p.ProcessId)
            }
        }
    }
    # Children first, so a parent cannot respawn one while the tree is coming down.
    $targets.Reverse()
    foreach ($id in $targets) { Stop-Process -Id $id -Force -ErrorAction SilentlyContinue }
}

function Send-Console([string]$line) {
    try { $proc.StandardInput.WriteLine($line); $proc.StandardInput.Flush(); $true } catch { $false }
}

$exitCode = 1
try {
    [void]$proc.Start()
    $proc.BeginOutputReadLine()
    $proc.BeginErrorReadLine()

    $failPattern = 'Mod Loading has failed|Failed to create mod instance|Exception in server tick loop|Encountered an unexpected exception'
    $deadline = (Get-Date).AddSeconds($timeout)
    $done = $false

    while ((Get-Date) -lt $deadline) {
        Pump
        if ($script:text -match $failPattern) {
            Write-Output 'SMOKE BOOT FAILED: the server hit a fatal error while loading.'
            ($script:text -split "`n" | Select-String -Pattern $failPattern -Context 0, 8 | Select-Object -First 3) | Out-String | Write-Output
            $exitCode = 1
            break
        }
        if ($script:text -match 'Done \(.*\)! For help') {
            Write-Output ($script:text -split "`n" | Where-Object { $_ -match 'Done \(.*\)! For help' } | Select-Object -First 1)
            $done = $true
            break
        }
        if ($proc.HasExited) {
            Pump
            Write-Output 'SMOKE BOOT FAILED: the server process exited before finishing startup.'
            Write-Output (($script:text -split "`n") | Select-Object -Last 40 | Out-String)
            $exitCode = 1
            break
        }
        Start-Sleep -Milliseconds 500
    }

    if (-not $done -and $exitCode -eq 1 -and (Get-Date) -ge $deadline) {
        Write-Output "SMOKE BOOT FAILED: no 'Done' after ${timeout}s."
        Write-Output (($script:text -split "`n") | Select-Object -Last 40 | Out-String)
    }

    if ($done) {
        # Done is not "stdin is live": the first console writes can be swallowed. Probe until the
        # server broadcasts SMOKE-READY, exactly as the shell script does.
        $readyDeadline = (Get-Date).AddSeconds(60)
        while ((Get-Date) -lt $readyDeadline) {
            Pump
            if ($script:text -match '\[Server\] SMOKE-READY') { break }
            [void](Send-Console 'say SMOKE-READY')
            Start-Sleep -Seconds 1
        }
        Pump

        if ($script:text -notmatch '\[Server\] SMOKE-READY') {
            Write-Output 'SMOKE BOOT FAILED: the server started but never echoed SMOKE-READY'
            [void](Send-Console 'stop')
            $exitCode = 1
        } else {
            $failed = $false
            if ($commandsFile) {
                foreach ($command in [System.IO.File]::ReadAllLines($commandsFile)) {
                    if ([string]::IsNullOrWhiteSpace($command)) { continue }
                    if ($command.TrimStart().StartsWith('#')) { continue }
                    [void](Send-Console $command)
                    # Hoppers and belts need ticks between lines; a scenario that must not yield
                    # to a block entity belongs in a datapack function (ADR-0061).
                    Start-Sleep -Seconds 1
                    Pump
                }
                Start-Sleep -Seconds 2
                Pump
            }

            if ($expect) {
                foreach ($pattern in ($expect -split "`r?`n")) {
                    if ([string]::IsNullOrWhiteSpace($pattern)) { continue }
                    $hit = ($script:text -split "`n") | Where-Object { $_ -match $pattern } | Select-Object -First 1
                    if ($hit) {
                        Write-Output $hit
                    } else {
                        Write-Output "SMOKE BOOT FAILED: the server started but its log never matched: $pattern"
                        $failed = $true
                        break
                    }
                }
            }

            [void](Send-Console 'stop')
            for ($i = 0; $i -lt 60; $i++) {
                if ($proc.HasExited) { break }
                Pump
                Start-Sleep -Seconds 1
            }
            Pump

            if ($failed) {
                $exitCode = 1
            } else {
                Write-Output 'SMOKE BOOT PASSED'
                $exitCode = 0
            }
        }
    }
} finally {
    Pump
    if (-not $proc.HasExited) { Stop-Tree $proc.Id }
    Unregister-Event -SourceIdentifier $stdoutEvent.Name -ErrorAction SilentlyContinue
    Unregister-Event -SourceIdentifier $stderrEvent.Name -ErrorAction SilentlyContinue
    try { Remove-Tree $world } catch { }
}

exit $exitCode
