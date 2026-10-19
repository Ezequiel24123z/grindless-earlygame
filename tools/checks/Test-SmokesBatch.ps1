#requires -Version 5.1
<#
Bounded, no-server contracts for the native Bash shared-world batch runner.
Usage: powershell -NoProfile -File tools/checks/Test-SmokesBatch.ps1 [-BashPath <installed Bash>]
       pwsh -NoProfile -File tools/checks/Test-SmokesBatch.ps1

Only the batch entrypoints are copied, byte-for-byte, into temporary fixtures.
Every possible fixture launch target is a safe fake; the real smoke-boot, JVM,
Gradle, SSH, network, saves and private PC configuration are never invoked.
The current PowerShell reference is copied from the selected RepositoryRoot.
Its probes cover ordinary selection/aggregation/exit and A01 fresh-world isolation.
Handshake, process ownership and cleanup inside smoke-boot remain its contract,
not server behavior proved by these fake launch-boundary tests.
#>
[CmdletBinding()]
param(
    [string]$RepositoryRoot,
    [string]$BashPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
if (-not $RepositoryRoot) { $RepositoryRoot = Join-Path $PSScriptRoot '../..' }
$RepositoryRoot = [IO.Path]::GetFullPath($RepositoryRoot)
$script:WindowsRuntime = [IO.Path]::DirectorySeparatorChar -eq '\'
$script:Utf8 = New-Object Text.UTF8Encoding($false, $true)
$script:Assertions = 0
$script:Cases = 0
$script:NativeRuns = 0
$script:ReferenceRuns = 0
$script:Failures = New-Object 'Collections.Generic.List[string]'
$script:Clock = [Diagnostics.Stopwatch]::StartNew()
$script:FixtureId = [Guid]::NewGuid().ToString('N')
$script:TestTemp = Join-Path ([IO.Path]::GetTempPath()) ('grindless-smokes-batch-' + $script:FixtureId)
$script:EnvironmentNames = @(
    'SMOKE_COMMANDS', 'SMOKE_EXPECT_FILE', 'SMOKE_LOG', 'SMOKE_DATAPACK',
    'SMOKE_OPTIONAL_DATAPACK', 'SMOKE_TIMEOUT', 'SMOKE_EXPECT_TIMEOUT',
    'SMOKE_PORT', 'GRINDLESS_ACCEPT_EULA', 'CI', 'JAVA_HOME', 'SMOKE_EXPECT', 'CDPATH'
)
$script:CallerEnvironment = @{}
foreach ($name in $script:EnvironmentNames) {
    $script:CallerEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Assert-Test([bool]$Condition, [string]$Label) {
    $script:Assertions++
    if (-not $Condition) { throw "ASSERTION FAILED: $Label" }
}

function Write-FixtureText([string]$Path, [string]$Text) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($Path)) | Out-Null
    [IO.File]::WriteAllText($Path, $Text, $script:Utf8)
}

function Read-FixtureText([string]$Path) {
    return [IO.File]::ReadAllText($Path, $script:Utf8)
}

function Format-SafeEvidence([string]$Text) {
    $safe = $Text.Replace($script:TestTemp, '<fixture>').Replace($RepositoryRoot, '<repository>')
    if (Get-Variable NativeTemp -Scope Script -ErrorAction SilentlyContinue) {
        $safe = $safe.Replace($script:NativeTemp, '<fixture>')
    }
    return $safe
}

function Quote-ProcessArgument([string]$Value) {
    # ProcessStartInfo.ArgumentList is not available in Windows PowerShell 5.1.
    $escaped = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}

function Invoke-Child([string]$Executable, [string[]]$Arguments,
        [string]$WorkingDirectory, [hashtable]$Environment = @{}) {
    $remaining = 120000 - [int]$script:Clock.ElapsedMilliseconds
    if ($remaining -le 0) { throw 'The bounded tooling suite exceeded its 120-second deadline.' }
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Executable
    $info.Arguments = (@($Arguments | ForEach-Object { Quote-ProcessArgument $_ }) -join ' ')
    $info.WorkingDirectory = $WorkingDirectory
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = $script:Utf8
    $info.StandardErrorEncoding = $script:Utf8
    # Remove ambient shell hooks/search paths; explicit fixture overrides below still apply.
    foreach ($key in @($info.EnvironmentVariables.Keys)) {
        if ($key -in @('BASH_ENV', 'ENV', 'CDPATH', 'SHELLOPTS', 'BASHOPTS', 'GLOBIGNORE') -or
                $key -like 'BASH_FUNC_*') {
            $info.EnvironmentVariables.Remove($key)
        }
    }
    $binaryDirectory = [IO.Path]::GetDirectoryName($script:ResolvedBash)
    $info.EnvironmentVariables['PATH'] = $binaryDirectory + [IO.Path]::PathSeparator +
        $info.EnvironmentVariables['PATH']
    foreach ($key in $Environment.Keys) {
        if ($null -eq $Environment[$key]) { $info.EnvironmentVariables.Remove($key) }
        else { $info.EnvironmentVariables[$key] = [string]$Environment[$key] }
    }
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $info
    try {
        if (-not $process.Start()) { throw 'Could not start the isolated tooling process.' }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit([Math]::Min(15000, $remaining))) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            throw 'An isolated tooling process exceeded its 15-second limit.'
        }
        $process.WaitForExit()
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Out = $stdout.Result
            Err = $stderr.Result
        }
    } finally { $process.Dispose() }
}

function Resolve-InstalledBash {
    $candidates = New-Object 'Collections.Generic.List[string]'
    if ($BashPath) { $candidates.Add($BashPath) }
    elseif ($env:GRINDLESS_SMOKES_TEST_BASH) { $candidates.Add($env:GRINDLESS_SMOKES_TEST_BASH) }
    else {
        foreach ($command in @(Get-Command bash,bash.exe -CommandType Application -All -ErrorAction SilentlyContinue)) {
            $candidates.Add($command.Source)
        }
        if ($script:WindowsRuntime) {
            foreach ($command in @(Get-Command git.exe -CommandType Application -All -ErrorAction SilentlyContinue)) {
                $directory = [IO.Path]::GetDirectoryName($command.Source)
                for ($level = 0; $level -lt 4 -and $directory; $level++) {
                    foreach ($relative in @('bin/bash.exe', 'usr/bin/bash.exe', 'usr/bin/sh.exe')) {
                        $candidates.Add((Join-Path $directory $relative))
                    }
                    $directory = [IO.Path]::GetDirectoryName($directory)
                }
            }
            foreach ($base in @($env:ProgramFiles, ${env:ProgramFiles(x86)},
                    (Join-Path $env:LOCALAPPDATA 'Programs'))) {
                if ($base) { $candidates.Add((Join-Path $base 'Git/bin/bash.exe')) }
            }
        }
    }
    foreach ($candidate in $candidates) {
        $full = [IO.Path]::GetFullPath($candidate)
        # The Windows bash.exe shim starts WSL, not an installed native Bash.
        if ($script:WindowsRuntime -and $env:WINDIR -and
                $full.StartsWith(($env:WINDIR.TrimEnd('\') + '\'), [StringComparison]::OrdinalIgnoreCase)) {
            continue
        }
        if ([IO.File]::Exists($full)) { return $full }
        # MSYS $BASH (including cygpath output) can omit the real .exe suffix.
        if ($script:WindowsRuntime -and [IO.File]::Exists(($full + '.exe'))) {
            return $full + '.exe'
        }
    }
    throw 'An installed native Bash is required. Supply -BashPath; do not select the Windows WSL stub.'
}

$script:EnvironmentRecorder = @'
record_env() {
    printf '%s\000%s\000%s\000' "$1" "$2" "$3"
}
record_environment() {
    record_env SMOKE_COMMANDS "${SMOKE_COMMANDS+x}" "${SMOKE_COMMANDS-}"
    record_env SMOKE_EXPECT_FILE "${SMOKE_EXPECT_FILE+x}" "${SMOKE_EXPECT_FILE-}"
    record_env SMOKE_LOG "${SMOKE_LOG+x}" "${SMOKE_LOG-}"
    record_env SMOKE_DATAPACK "${SMOKE_DATAPACK+x}" "${SMOKE_DATAPACK-}"
    record_env SMOKE_OPTIONAL_DATAPACK "${SMOKE_OPTIONAL_DATAPACK+x}" "${SMOKE_OPTIONAL_DATAPACK-}"
    record_env SMOKE_TIMEOUT "${SMOKE_TIMEOUT+x}" "${SMOKE_TIMEOUT-}"
    record_env SMOKE_EXPECT_TIMEOUT "${SMOKE_EXPECT_TIMEOUT+x}" "${SMOKE_EXPECT_TIMEOUT-}"
    record_env SMOKE_PORT "${SMOKE_PORT+x}" "${SMOKE_PORT-}"
    record_env GRINDLESS_ACCEPT_EULA "${GRINDLESS_ACCEPT_EULA+x}" "${GRINDLESS_ACCEPT_EULA-}"
    record_env CI "${CI+x}" "${CI-}"
    record_env JAVA_HOME "${JAVA_HOME+x}" "${JAVA_HOME-}"
    record_env SMOKE_EXPECT "${SMOKE_EXPECT+x}" "${SMOKE_EXPECT-}"
    record_env CDPATH "${CDPATH+x}" "${CDPATH-}"
}
'@
$script:FakeBoot = "#!/bin/sh`nset -eu`n" + $script:EnvironmentRecorder + "`n" + @'
capture=$GRINDLESS_SMOKES_TEST_CAPTURE
printf 'call\n' >> "$capture/boot.calls"
printf '%s\000' "$@" > "$capture/boot.argv"
[ "$#" -eq 1 ] || exit 90
[ -f "$1/.batch-fixture-root" ] || exit 91
origin=$(cat "$1/.batch-fixture-root")
printf '%s' "$origin" > "$capture/boot.origin"
[ "$origin" = "$GRINDLESS_SMOKES_TEST_TOKEN" ] || exit 92
record_environment > "$capture/boot.environment"
[ -f "$SMOKE_COMMANDS" ] && [ -f "$SMOKE_EXPECT_FILE" ] || exit 93
cat "$SMOKE_COMMANDS" > "$capture/at-launch.commands"
cat "$SMOKE_EXPECT_FILE" > "$capture/at-launch.expect"
printf 'FAKE BOOT ONLY: exit %s\n' "$GRINDLESS_SMOKES_TEST_EXIT" > "$SMOKE_LOG"
printf 'FAKE-ROOT-SENTINEL: selected temporary root\n'
exit "$GRINDLESS_SMOKES_TEST_EXIT"
'@
$script:Driver = "#!/bin/sh`nset -u`n" + $script:EnvironmentRecorder + "`n" + @'
capture=$GRINDLESS_SMOKES_TEST_CAPTURE
entry=$1
shift
record_environment > "$capture/caller.before"
# A normal child CLI launch, never sourcing the executable batch runner.
"$BASH" --noprofile --norc "$entry" "$@"
status=$?
record_environment > "$capture/caller.after"
exit "$status"
'@

function New-Fixture([string]$Manifest = "alpha`nbeta`ngamma`n",
        [string]$RootName = 'selected root [literal] & spaces') {
    $relativeHome = 'case ' + $script:Cases
    $caseHome = Join-Path $script:TestTemp $relativeHome
    $root = Join-Path $caseHome $RootName
    $entryRoot = Join-Path $caseHome 'entrypoint copy'
    $cwd = Join-Path $caseHome 'unselected caller root'
    $capture = Join-Path $caseHome 'capture'
    [IO.Directory]::CreateDirectory($capture) | Out-Null
    foreach ($decoy in @($entryRoot, $cwd)) {
        Write-FixtureText (Join-Path $decoy 'tools/smoke-boot.sh') $script:FakeBoot
        Write-FixtureText (Join-Path $decoy 'tools/smoke-boot.ps1') "throw 'Only the safe intercepted launch is allowed.'"
        Write-FixtureText (Join-Path $decoy 'tools/smoke/scenarios.txt') "decoy`n"
        Write-FixtureText (Join-Path $decoy '.batch-fixture-root') 'WRONG-ROOT-DECOY'
    }
    $entry = Join-Path $entryRoot 'tools/run-smokes-batch.sh'
    [IO.File]::WriteAllBytes($entry, $script:BatchBytes)
    [IO.File]::WriteAllBytes((Join-Path $entryRoot 'tools/run-smokes-batch.ps1'), $script:ReferenceBytes)
    Write-FixtureText (Join-Path $root 'tools/smoke-boot.sh') $script:FakeBoot
    Write-FixtureText (Join-Path $root 'tools/smoke-boot.ps1') "throw 'Only the safe intercepted launch is allowed.'"
    $token = 'selected-fixture-' + $script:Cases
    Write-FixtureText (Join-Path $root '.batch-fixture-root') $token
    Write-FixtureText (Join-Path $root 'tools/smoke/scenarios.txt') $Manifest
    foreach ($name in @('alpha', 'beta', 'gamma')) {
        Write-FixtureText (Join-Path $root "tools/smoke/$name.commands") "say $name`n"
        Write-FixtureText (Join-Path $root "tools/smoke/$name.expect") "PASS $name`n"
    }
    if (-not $script:WindowsRuntime) {
        $permissions = Invoke-Child $script:ResolvedBash @('--noprofile', '--norc',
            $script:ExecutableHelperPath, (Join-Path $root 'tools/smoke-boot.sh'),
            (Join-Path $entryRoot 'tools/smoke-boot.sh'), (Join-Path $cwd 'tools/smoke-boot.sh')) $caseHome
        if ($permissions.ExitCode -ne 0) { throw "Could not make the POSIX fixture fakes executable: $($permissions.Err)" }
    }
    $nativeHome = $script:NativeTemp + '/' + $relativeHome
    $nativeRoot = $nativeHome + '/' + $RootName
    $environment = @{
        GRINDLESS_SMOKES_TEST_CAPTURE = ($nativeHome + '/capture')
        GRINDLESS_SMOKES_TEST_TOKEN = $token
        GRINDLESS_SMOKES_TEST_EXIT = '0'
        SMOKE_COMMANDS = ($nativeHome + '/unused inherited.commands')
        SMOKE_EXPECT_FILE = ($nativeHome + '/unused inherited.expect')
        SMOKE_LOG = ($nativeHome + '/unused inherited.log')
        SMOKE_DATAPACK = 'synthetic inherited datapack'
        SMOKE_OPTIONAL_DATAPACK = 'synthetic inherited optional datapack'
        SMOKE_TIMEOUT = '073'
        SMOKE_EXPECT_TIMEOUT = '019'
        SMOKE_PORT = '25421'
        GRINDLESS_ACCEPT_EULA = 'false'
        CI = 'synthetic-ci-value'
        JAVA_HOME = ($nativeHome + '/synthetic java home')
        SMOKE_EXPECT = 'inherited regex, not a batch budget'
    }
    return [pscustomobject]@{
        Home = $caseHome; Root = $root; EntryRoot = $entryRoot; Cwd = $cwd
        Capture = $capture; NativeRoot = $nativeRoot; NativeHome = $nativeHome
        Entry = ($nativeHome + '/entrypoint copy/tools/run-smokes-batch.sh')
        Run = (Join-Path $root 'forge/run'); Token = $token; Environment = $environment
    }
}

function Invoke-Batch($Fixture, [string[]]$Arguments, [hashtable]$Environment = @{},
        [string]$WorkingDirectory) {
    if (-not $PSBoundParameters.ContainsKey('Arguments')) { $Arguments = @($Fixture.NativeRoot) }
    if (-not $WorkingDirectory) { $WorkingDirectory = $Fixture.Cwd }
    $childEnvironment = @{}
    foreach ($key in $Fixture.Environment.Keys) { $childEnvironment[$key] = $Fixture.Environment[$key] }
    foreach ($key in $Environment.Keys) { $childEnvironment[$key] = $Environment[$key] }
    $script:NativeRuns++
    return Invoke-Child $script:ResolvedBash (@('--noprofile', '--norc',
        $script:DriverPath, $Fixture.Entry) + $Arguments) $WorkingDirectory $childEnvironment
}

function Read-CapturedEnvironment($Fixture, [string]$CaptureFile = 'boot.environment') {
    $parts = (Read-FixtureText (Join-Path $Fixture.Capture $CaptureFile)).Split([char]0)
    $result = @{}
    for ($i = 0; $i -lt $parts.Length - 1; $i += 3) {
        $result[$parts[$i]] = [pscustomobject]@{ Present = ($parts[$i + 1] -ceq 'x'); Value = $parts[$i + 2] }
    }
    return $result
}

function Assert-CallerPreserved($Fixture) {
    $before = Read-FixtureText (Join-Path $Fixture.Capture 'caller.before')
    $after = Read-FixtureText (Join-Path $Fixture.Capture 'caller.after')
    Assert-Test ($before -ceq $after) 'ordinary child launch leaves the Bash caller environment unchanged'
    foreach ($name in $script:EnvironmentNames) {
        Assert-Test ([Environment]::GetEnvironmentVariable($name, 'Process') -ceq
            $script:CallerEnvironment[$name]) "PowerShell caller $name is unchanged (values redacted)"
    }
}

function Assert-Dispatch($Fixture, $Result, [int]$ExitCode = 0) {
    Assert-Test ($Result.ExitCode -eq $ExitCode) "exact fake boot exit $ExitCode (actual $($Result.ExitCode)): $($Result.Err)"
    Assert-Test ((Read-FixtureText (Join-Path $Fixture.Capture 'boot.calls')) -ceq "call`n") 'exactly one boot dispatch'
    Assert-Test ((Read-FixtureText (Join-Path $Fixture.Capture 'boot.argv')) -ceq
        ($Fixture.NativeRoot + [char]0)) 'one literal canonical root argument, including spaces and metacharacters'
    Assert-Test ((Read-FixtureText (Join-Path $Fixture.Capture 'boot.origin')) -ceq $Fixture.Token) 'selected fake root, not a decoy or repository harness'
    Assert-Test ($Result.Out.Contains('FAKE-ROOT-SENTINEL')) 'positive selected-root sentinel was executed'
    $environment = Read-CapturedEnvironment $Fixture
    foreach ($pair in @(@('SMOKE_COMMANDS', 'commands'), @('SMOKE_EXPECT_FILE', 'expect'), @('SMOKE_LOG', 'log'))) {
        Assert-Test ($environment[$pair[0]].Value -ceq
            ($Fixture.NativeRoot + '/forge/run/smoke-batch.' + $pair[1])) "fixed selected-root $($pair[0]) path"
        Assert-Test ([IO.File]::Exists((Join-Path $Fixture.Run ('smoke-batch.' + $pair[1])))) "retained .$($pair[1]) artifact"
    }
    foreach ($name in @('SMOKE_DATAPACK', 'SMOKE_OPTIONAL_DATAPACK')) {
        Assert-Test (-not $environment[$name].Present) "$name is unset, not merely empty, at boot"
    }
    Assert-CallerPreserved $Fixture
}

function Assert-Aggregate($Fixture, [string]$Commands, [string]$Expect) {
    foreach ($pair in @(@('commands', $Commands), @('expect', $Expect))) {
        $path = Join-Path $Fixture.Run ('smoke-batch.' + $pair[0])
        $bytes = [IO.File]::ReadAllBytes($path)
        Assert-Test ([Convert]::ToBase64String($bytes) -ceq
            [Convert]::ToBase64String($script:Utf8.GetBytes($pair[1]))) "exact LF, no-BOM UTF8 .$($pair[0]) bytes and final newline"
        Assert-Test ((Read-FixtureText (Join-Path $Fixture.Capture ('at-launch.' + $pair[0]))) -ceq
            $pair[1]) ".$($pair[0]) was complete before launch"
    }
}

function Assert-Refused($Fixture, $Result, [string]$Message = '') {
    Assert-Test ($Result.ExitCode -ne 0) 'invalid request exits nonzero'
    Assert-Test (-not [IO.File]::Exists((Join-Path $Fixture.Capture 'boot.calls'))) 'invalid request never launches boot'
    Assert-Test ((($Result.Out + $Result.Err).Trim()).Length -gt 0) 'failure includes a diagnostic'
    if ($Message) { Assert-Test (($Result.Out + $Result.Err).Contains($Message)) "failure identifies $Message" }
    Assert-CallerPreserved $Fixture
}

function Invoke-PowerShellReference($Fixture, [string]$Only) {
    $environment = @{}
    foreach ($key in $Fixture.Environment.Keys) { $environment[$key] = $Fixture.Environment[$key] }
    $environment['GRINDLESS_SMOKES_REFERENCE_SCRIPT'] = Join-Path $Fixture.EntryRoot 'tools/run-smokes-batch.ps1'
    $environment['GRINDLESS_SMOKES_REFERENCE_ROOT'] = $Fixture.Root
    $environment['GRINDLESS_SMOKES_REFERENCE_CAPTURE'] = $Fixture.Capture
    $environment['GRINDLESS_SMOKES_REFERENCE_ONLY'] = $Only
    $result = Invoke-Child $script:PowerShellExecutable @('-NoLogo', '-NoProfile',
        '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $script:ReferenceDriverPath) $Fixture.Cwd $environment
    $script:ReferenceRuns++
    return $result
}

function Assert-ReferenceRefused($Fixture, [string]$Only) {
    $result = Invoke-PowerShellReference $Fixture $Only
    Assert-Test ($result.ExitCode -ne 0) 'current PS twin refuses an isolated selection'
    Assert-Test (-not [IO.File]::Exists((Join-Path $Fixture.Capture 'reference.calls'))) 'isolated PS selection never reaches even the fake boot'
    $diagnostic = Format-SafeEvidence ($result.Out + $result.Err)
    Assert-Test ($diagnostic.Contains('fresh datapack world')) ("PS refusal identifies the fresh-world requirement; actual: " + $diagnostic)
}

function Invoke-OrdinaryReference($Fixture, [string]$Only, $NativeResult) {
    $commands = Read-FixtureText (Join-Path $Fixture.Run 'smoke-batch.commands')
    $expect = Read-FixtureText (Join-Path $Fixture.Run 'smoke-batch.expect')
    $result = Invoke-PowerShellReference $Fixture $Only
    Assert-Test ($result.ExitCode -eq $NativeResult.ExitCode) ("ordinary PS reference and Bash propagate the same fake exit; PS=" +
        $result.ExitCode + '; Bash=' + $NativeResult.ExitCode + '; PS stderr=' + (Format-SafeEvidence $result.Err))
    Assert-Test ((Read-FixtureText (Join-Path $Fixture.Capture 'reference.calls')) -ceq 'one safe intercepted launch') 'ordinary PS launch was intercepted exactly once'
    Assert-Test ((Read-FixtureText (Join-Path $Fixture.Run 'smoke-batch.commands')).Replace("`r`n", "`n") -ceq
        $commands) 'ordinary PS and Bash command aggregation agree (platform newlines normalized)'
    Assert-Test ((Read-FixtureText (Join-Path $Fixture.Run 'smoke-batch.expect')).Replace("`r`n", "`n") -ceq
        $expect) 'ordinary PS and Bash expectation aggregation agree (platform newlines normalized)'
    Assert-Test ($result.Out.Replace("`r`n", "`n").Trim() -ceq
        $NativeResult.Out.Replace("`r`n", "`n").Replace("FAKE-ROOT-SENTINEL: selected temporary root`n", '').Trim()) 'ordinary PS and Bash batch announcement agrees'
}

function Invoke-Case([string]$Label, [scriptblock]$Body) {
    $script:Cases++
    try {
        & $Body
        Write-Output "PASS: $Label"
    } catch {
        $message = Format-SafeEvidence $_.Exception.Message
        $script:Failures.Add(($Label + ': ' + $message))
        Write-Output ("FAIL: $Label -- " + $message)
    }
}

[IO.Directory]::CreateDirectory($script:TestTemp) | Out-Null
try {
    Write-FixtureText (Join-Path $script:TestTemp '.owned-fixture') $script:FixtureId
    $script:ResolvedBash = Resolve-InstalledBash
    $script:PowerShellExecutable = (Get-Process -Id $PID).Path
    $script:BatchBytes = [IO.File]::ReadAllBytes((Join-Path $RepositoryRoot 'tools/run-smokes-batch.sh'))
    $script:ReferenceBytes = [IO.File]::ReadAllBytes((Join-Path $RepositoryRoot 'tools/run-smokes-batch.ps1'))
    $probePath = Join-Path $script:TestTemp 'runtime-probe.sh'
    Write-FixtureText $probePath @'
set -eu
[ -n "${BASH_VERSION-}" ] || { printf '%s\n' 'The selected executable is not Bash.' >&2; exit 127; }
for tool in mkdir tr cat dirname; do
    command -v "$tool" >/dev/null || { printf 'Missing installed Bash utility: %s\n' "$tool" >&2; exit 127; }
done
printf '%s\n' "$BASH_VERSION" "$OSTYPE" "$(pwd -P)"
'@
    $probe = Invoke-Child $script:ResolvedBash @('--noprofile', '--norc',
        $probePath.Replace('\', '/')) $script:TestTemp
    if ($probe.ExitCode -ne 0) { throw "Native Bash prerequisites failed: $($probe.Err)" }
    $runtime = $probe.Out.Replace("`r`n", "`n").TrimEnd("`n").Split("`n")
    $script:NativeTemp = $runtime[2]
    Write-Output ("Runtime: PowerShell " + $PSVersionTable.PSVersion + ' ' +
        $PSVersionTable.PSEdition + '; Bash ' + $runtime[0] + '; OSTYPE=' + $runtime[1] +
        '; executable=' + [IO.Path]::GetFileName($script:ResolvedBash))
    Write-Output 'The Bash subject is a byte-exact temporary copy; all launch targets are fixture fakes.'
    $script:DriverPath = $script:NativeTemp + '/child-driver.sh'
    Write-FixtureText (Join-Path $script:TestTemp 'child-driver.sh') $script:Driver
    $script:ReferenceDriverPath = Join-Path $script:TestTemp 'reference-driver.ps1'
    Write-FixtureText $script:ReferenceDriverPath @'
$ErrorActionPreference = 'Stop'
function global:powershell {
    if ($args.Count -ne 7 -or $args[0] -cne '-NoProfile' -or
            $args[1] -cne '-ExecutionPolicy' -or $args[2] -cne 'Bypass' -or
            $args[3] -cne '-File' -or $args[5] -cne '-Root' -or
            $args[4] -cne (Join-Path $env:GRINDLESS_SMOKES_REFERENCE_ROOT 'tools/smoke-boot.ps1') -or
            $args[6] -cne $env:GRINDLESS_SMOKES_REFERENCE_ROOT) {
        throw 'Unexpected PowerShell launch boundary; real boot is forbidden.'
    }
    $capture = Join-Path $env:GRINDLESS_SMOKES_REFERENCE_CAPTURE 'reference.calls'
    if ([IO.File]::Exists($capture)) { throw 'More than one reference boot launch.' }
    [IO.File]::WriteAllText($capture, 'one safe intercepted launch')
    $global:LASTEXITCODE = [int]$env:GRINDLESS_SMOKES_TEST_EXIT
}
try {
    & $env:GRINDLESS_SMOKES_REFERENCE_SCRIPT -Root $env:GRINDLESS_SMOKES_REFERENCE_ROOT -Only $env:GRINDLESS_SMOKES_REFERENCE_ONLY
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
exit $LASTEXITCODE
'@
    # On POSIX a newly written shell fake needs its executable bit. Git/MSYS
    # honors the shebang on its Windows filesystem without requiring chmod.
    $makeExecutablePath = Join-Path $script:TestTemp 'make-executable.sh'
    $script:ExecutableHelperPath = $script:NativeTemp + '/make-executable.sh'
    Write-FixtureText $makeExecutablePath @'
set -eu
for path in "$@"; do
    if [ ! -x "$path" ]; then chmod u+x "$path"; fi
done
'@

    Invoke-Case 'manifest order, case/comma/duplicate/unknown filtering, literal argv, overwrite, one dispatch and ordinary PS parity' {
        # The legacy PS reference uses wildcard-aware Resolve-Path. Keep its
        # ordinary parity root free of [] while native-only cases retain [].
        $f = New-Fixture " # manifest comment`n  `nalpha ignored metadata`nbeta`ngamma`tignored`n" 'selected parity root & spaces'
        foreach ($extension in @('commands', 'expect', 'log')) {
            Write-FixtureText (Join-Path $f.Run "smoke-batch.$extension") "STALE MUST BE REPLACED`n"
        }
        $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', ' GAMMA,unknown, ALPHA,alpha, , ')
        Assert-Dispatch $f $result
        Assert-Aggregate $f "say BATCH-BEGIN alpha`nsay alpha`nsay BATCH-END alpha`nsay BATCH-BEGIN gamma`nsay gamma`nsay BATCH-END gamma`n" "BATCH-END alpha`nPASS alpha`nBATCH-END gamma`nPASS gamma`n"
        Assert-Test ($result.Out.Contains('Running 2 shared-world smoke scenario(s) in one Forge boot.')) 'announcement reports selected ordinary count'
        Assert-Test (-not $result.Out.Contains('Skipped')) 'ordinary selection has no skip announcement'
        Assert-Test ((Read-FixtureText (Join-Path $f.Run 'smoke-batch.log')) -ceq "FAKE BOOT ONLY: exit 0`n") 'fake log replaces stale artifact and remains available'
        Assert-Test ([Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $f.EntryRoot 'tools/run-smokes-batch.sh'))) -ceq
            [Convert]::ToBase64String($script:BatchBytes)) 'executed subject bytes equal the real batch entrypoint'
        Invoke-OrdinaryReference $f ' GAMMA,unknown, ALPHA,alpha, , ' $result
    }
    Invoke-Case 'default all; timeout sidecars ignored and harness override values inherited unchanged' {
        $f = New-Fixture
        foreach ($name in @('alpha', 'beta', 'gamma')) {
            Write-FixtureText (Join-Path $f.Root "tools/smoke/$name.timeout") "not-a-timeout`n"
        }
        $result = Invoke-Batch $f
        Assert-Dispatch $f $result
        Assert-Test ($result.Out.Contains('Running 3 shared-world')) 'omitted --only selects all ordinary manifest entries'
        $environment = Read-CapturedEnvironment $f
        foreach ($name in @('SMOKE_TIMEOUT', 'SMOKE_EXPECT_TIMEOUT', 'SMOKE_PORT',
                'GRINDLESS_ACCEPT_EULA', 'CI', 'JAVA_HOME', 'SMOKE_EXPECT')) {
            Assert-Test ($environment[$name].Present -and $environment[$name].Value -ceq
                $f.Environment[$name]) "$name forwarded byte-for-byte, not recomputed from .timeout"
        }
    }
    Invoke-Case 'empty normalized --only retains legacy default-all behavior' {
        $f = New-Fixture
        $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', " , `t,  ")
        Assert-Dispatch $f $result
        Assert-Test ($result.Out.Contains('Running 3 shared-world')) 'empty token list means all, not no scenarios'
    }
    Invoke-Case 'literal empty --only argument is preserved across the native process boundary' {
        $f = New-Fixture
        $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', '')
        Assert-Dispatch $f $result
        Assert-Test ($result.Out.Contains('Running 3 shared-world')) 'an actual empty argv token is default-all, not a missing value'
    }
    Invoke-Case 'missing and empty sidecars are allowed; final manifest line needs no newline' {
        $f = New-Fixture "bare`nalpha`nbeta"
        foreach ($extension in @('commands', 'expect')) {
            [IO.File]::Delete((Join-Path $f.Root "tools/smoke/alpha.$extension"))
            Write-FixtureText (Join-Path $f.Root "tools/smoke/beta.$extension") ''
        }
        $result = Invoke-Batch $f
        Assert-Dispatch $f $result
        Assert-Aggregate $f "say BATCH-BEGIN bare`nsay BATCH-END bare`nsay BATCH-BEGIN alpha`nsay BATCH-END alpha`nsay BATCH-BEGIN beta`nsay BATCH-END beta`n" "BATCH-END bare`nBATCH-END alpha`nBATCH-END beta`n"
    }
    Invoke-Case 'BOM/CRLF/UTF8 inputs, command comments, literal regex hash/whitespace and unterminated sidecars; PS parity' {
        $bom = [string][char]0xfeff
        $word = 'caf' + [char]0xe9 + '-' + [char]0x03a9
        $f = New-Fixture ($bom + " # comment`r`n `t`r`n alpha metadata`r`nbeta") ('selected root caf' + [char]0xe9 + ' with spaces')
        Write-FixtureText (Join-Path $f.Root 'tools/smoke/alpha.commands') ($bom + "  # omit`r`n`r`n `t`r`n  say $word  `r`n`tsay tabs`t`r`nsay tail")
        Write-FixtureText (Join-Path $f.Root 'tools/smoke/beta.commands') "say beta # inline hash stays`n"
        Write-FixtureText (Join-Path $f.Root 'tools/smoke/alpha.expect') ($bom + "`r`n  ^$word$  `r`n#literal regex`r`n`t^tabs`t$`r`nfinal regex")
        [IO.File]::Delete((Join-Path $f.Root 'tools/smoke/beta.expect'))
        $result = Invoke-Batch $f
        Assert-Dispatch $f $result
        Assert-Aggregate $f "say BATCH-BEGIN alpha`n  say $word  `n`tsay tabs`t`nsay tail`nsay BATCH-END alpha`nsay BATCH-BEGIN beta`nsay beta # inline hash stays`nsay BATCH-END beta`n" "BATCH-END alpha`n  ^$word$  `n#literal regex`n`t^tabs`t$`nfinal regex`nBATCH-END beta`n"
        Invoke-OrdinaryReference $f '' $result
    }
    foreach ($isolatedName in @('foreign-providers', 'states', 'recipe-cache-reload')) {
        Invoke-Case "fixed isolated-only refusal: $isolatedName (no optional path required)" {
            $f = New-Fixture ("alpha`n" + $isolatedName + "`n")
            $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', $isolatedName.ToUpperInvariant())
            Assert-Refused $f $result 'fresh datapack world'
        }
    }
    Invoke-Case 'A01-only selection without an optional sibling is refused by both current twins' {
        $f = New-Fixture "alpha`nextractor-backpressure`nbeta`n" 'selected A01 only root & spaces'
        Assert-Test (-not (Test-Path -LiteralPath (Join-Path $f.Root 'tools/smoke/extractor-backpressure-optional'))) 'A01 isolation does not depend on an optional-pack sibling'
        $only = ' EXTRACTOR-BACKPRESSURE,extractor-backpressure '
        $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', $only)
        Assert-Refused $f $result 'fresh datapack world'
        Assert-ReferenceRefused $f $only
    }
    foreach ($selection in @('named', 'default-all')) {
        Invoke-Case "A01 $selection selection skips only the fresh-world oracle in both current twins" {
            $f = New-Fixture "alpha`nextractor-backpressure`nbeta`n" 'selected A01 mixed root & spaces'
            Write-FixtureText (Join-Path $f.Root 'tools/smoke/extractor-backpressure.commands') "say MUST-NOT-BATCH A01`n"
            Write-FixtureText (Join-Path $f.Root 'tools/smoke/extractor-backpressure.expect') "MUST-NOT-EXPECT A01`n"
            $only = ''
            if ($selection -eq 'named') {
                $only = 'BETA,EXTRACTOR-BACKPRESSURE,ALPHA,alpha,unknown'
                $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', $only)
            } else {
                $result = Invoke-Batch $f
            }
            Assert-Dispatch $f $result
            Assert-Test ($result.Out.Contains('Skipped fresh-world datapack scenario(s): extractor-backpressure.')) 'A01 is explicitly announced as skipped'
            Assert-Test ($result.Out.Contains('Running 2 shared-world')) 'only ordinary selected scenarios contribute to the boot count'
            Assert-Aggregate $f "say BATCH-BEGIN alpha`nsay alpha`nsay BATCH-END alpha`nsay BATCH-BEGIN beta`nsay beta`nsay BATCH-END beta`n" "BATCH-END alpha`nPASS alpha`nBATCH-END beta`nPASS beta`n"
            Invoke-OrdinaryReference $f $only $result
        }
    }
    Invoke-Case 'default-all with only fixed isolated scenarios including A01 refuses both current twins' {
        $f = New-Fixture "extractor-backpressure`nstates`nrecipe-cache-reload`nforeign-providers`n" 'selected A01 isolated root & spaces'
        $result = Invoke-Batch $f
        Assert-Refused $f $result 'fresh datapack world'
        Assert-ReferenceRefused $f ''
    }
    foreach ($optionalKind in @('directory', 'file')) {
        Invoke-Case "dynamic optional $optionalKind requires isolation before any boot" {
            $f = New-Fixture "alpha`naddon`n"
            $optionalPath = Join-Path $f.Root 'tools/smoke/addon-optional'
            if ($optionalKind -eq 'directory') { [IO.Directory]::CreateDirectory($optionalPath) | Out-Null }
            else { Write-FixtureText $optionalPath 'invalid pack path is still isolated' }
            $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', 'addon')
            Assert-Refused $f $result 'fresh datapack world'
        }
    }
    Invoke-Case 'mixed fixed/dynamic isolation announces all skips and retains ordinary manifest order' {
        $f = New-Fixture "foreign-providers`nalpha`naddon-dir`nstates`nbeta`nrecipe-cache-reload`naddon-file`ngamma`n"
        [IO.Directory]::CreateDirectory((Join-Path $f.Root 'tools/smoke/addon-dir-optional')) | Out-Null
        Write-FixtureText (Join-Path $f.Root 'tools/smoke/addon-file-optional') 'not a directory'
        foreach ($name in @('foreign-providers', 'states', 'recipe-cache-reload', 'addon-dir', 'addon-file')) {
            Write-FixtureText (Join-Path $f.Root "tools/smoke/$name.commands") "say MUST-NOT-BATCH $name`n"
            Write-FixtureText (Join-Path $f.Root "tools/smoke/$name.expect") "MUST-NOT-EXPECT $name`n"
        }
        $result = Invoke-Batch $f
        Assert-Dispatch $f $result
        Assert-Test ($result.Out.Contains('Skipped fresh-world datapack scenario(s): foreign-providers, addon-dir, states, recipe-cache-reload, addon-file.')) 'skip list follows manifest order and names every isolated selection'
        Assert-Test ($result.Out.Contains('Running 3 shared-world')) 'only ordinary scenarios contribute to batch count'
        Assert-Aggregate $f "say BATCH-BEGIN alpha`nsay alpha`nsay BATCH-END alpha`nsay BATCH-BEGIN beta`nsay beta`nsay BATCH-END beta`nsay BATCH-BEGIN gamma`nsay gamma`nsay BATCH-END gamma`n" "BATCH-END alpha`nPASS alpha`nBATCH-END beta`nPASS beta`nBATCH-END gamma`nPASS gamma`n"
    }
    foreach ($bootExit in @(2, 47)) {
        Invoke-Case "exact boot exit $bootExit; artifacts retained and inherited optional environment restored" {
            $f = New-Fixture -RootName 'selected failure root & spaces'
            $f.Environment.GRINDLESS_SMOKES_TEST_EXIT = [string]$bootExit
            $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', 'alpha')
            Assert-Dispatch $f $result $bootExit
            Assert-Test ((Read-FixtureText (Join-Path $f.Run 'smoke-batch.log')) -ceq
                "FAKE BOOT ONLY: exit $bootExit`n") 'failure log is retained, not cleaned up'
            Assert-Aggregate $f "say BATCH-BEGIN alpha`nsay alpha`nsay BATCH-END alpha`n" "BATCH-END alpha`nPASS alpha`n"
            if ($bootExit -eq 47) { Invoke-OrdinaryReference $f 'alpha' $result }
        }
    }
    Invoke-Case 'unset startup/expect/port/consent/java overrides remain unset; no batch budget defaults' {
        $f = New-Fixture
        $unset = @{}
        foreach ($name in @('SMOKE_DATAPACK', 'SMOKE_OPTIONAL_DATAPACK', 'SMOKE_TIMEOUT',
                'SMOKE_EXPECT_TIMEOUT', 'SMOKE_PORT', 'GRINDLESS_ACCEPT_EULA', 'CI', 'JAVA_HOME', 'SMOKE_EXPECT')) {
            $unset[$name] = $null
        }
        $result = Invoke-Batch $f -Environment $unset
        Assert-Dispatch $f $result
        $environment = Read-CapturedEnvironment $f
        foreach ($name in $unset.Keys) { Assert-Test (-not $environment[$name].Present) "unset $name not synthesized by batch" }
    }
    Invoke-Case 'omitted root resolves only the temporary working directory' {
        $f = New-Fixture
        $result = Invoke-Batch $f -Arguments @() -WorkingDirectory $f.Root
        Assert-Dispatch $f $result
    }
    Invoke-Case 'relative metacharacter root with effective CDPATH resolves once and preserves caller search path' {
        $relativeRoot = 'relative root [literal] & spaces'
        $f = New-Fixture -RootName $relativeRoot
        try {
            $result = Invoke-Batch $f -Arguments @($relativeRoot, '--only', 'alpha') `
                -WorkingDirectory $f.Home -Environment @{ CDPATH = '.' }
            $before = Read-CapturedEnvironment $f 'caller.before'
            Assert-Test ($before['CDPATH'].Present -and $before['CDPATH'].Value -ceq '.') `
                'CDPATH is effective before the actual batch CLI, not removed by fixture sanitization'
            Assert-Dispatch $f $result
            $environment = Read-CapturedEnvironment $f
            Assert-Test ($environment['CDPATH'].Present -and $environment['CDPATH'].Value -ceq '.') `
                'root resolution does not change the inherited CDPATH at the fake boot'
            Assert-Aggregate $f "say BATCH-BEGIN alpha`nsay alpha`nsay BATCH-END alpha`n" "BATCH-END alpha`nPASS alpha`n"
        } finally {
            # The red path can create native newline/long-path components that WinPS cannot remove.
            $cleanup = Invoke-Child $script:ResolvedBash @('--noprofile', '--norc', '-c',
                'set -eu; [ "${1%/*}" = "$2" ]; [ "$(cat "$1/.batch-fixture-root")" = "$3" ]; rm -rf -- "$2"',
                'owned-cdpath-case-cleanup', $f.NativeRoot, $f.NativeHome, $f.Token) $script:TestTemp
            Assert-Test ($cleanup.ExitCode -eq 0) 'native cleanup removes only the token-verified CDPATH case'
        }
    }
    Invoke-Case '-- accepts one literal root starting with a dash' {
        $f = New-Fixture -RootName '-literal root with spaces'
        $result = Invoke-Batch $f -Arguments @('--', '-literal root with spaces') -WorkingDirectory $f.Home
        Assert-Dispatch $f $result
    }
    Invoke-Case 'help is successful without launching any harness' {
        $f = New-Fixture
        $result = Invoke-Batch $f -Arguments @('--help')
        Assert-Test ($result.ExitCode -eq 0 -and $result.Out.Contains('Usage:')) 'help reports usage successfully'
        Assert-Test (-not [IO.File]::Exists((Join-Path $f.Capture 'boot.calls'))) 'help never boots'
        Assert-CallerPreserved $f
    }
    Invoke-Case 'unknown-only normalized selection fails without boot' {
        $f = New-Fixture
        $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', 'unknown, OTHER ')
        Assert-Refused $f $result 'No named smoke scenarios'
    }
    $badCli = @(
        @{ Label = 'unknown option'; Tail = @('--bogus') },
        @{ Label = 'unsupported --shard'; Tail = @('--shard', '1') },
        @{ Label = 'unsupported --of'; Tail = @('--of', '2') },
        @{ Label = 'unsupported --list'; Tail = @('--list') },
        @{ Label = 'missing --only value'; Tail = @('--only') },
        @{ Label = 'option instead of --only value'; Tail = @('--only', '--help') },
        @{ Label = 'duplicate --only'; Tail = @('--only', 'alpha', '--only', 'beta') },
        @{ Label = 'extra project root'; Tail = @('another root') },
        @{ Label = 'bare literal-root delimiter'; Tail = @('--') }
    )
    foreach ($invalid in $badCli) {
        Invoke-Case ("CLI rejects " + $invalid.Label) {
            $f = New-Fixture
            $result = Invoke-Batch $f -Arguments (@($f.NativeRoot) + $invalid.Tail)
            Assert-Refused $f $result
        }
    }
    Invoke-Case 'missing selected root is rejected before launch' {
        $f = New-Fixture
        $result = Invoke-Batch $f -Arguments @($f.NativeHome + '/does not exist')
        Assert-Refused $f $result
    }
    foreach ($manifestKind in @('missing', 'directory', 'comments-only')) {
        Invoke-Case "invalid manifest: $manifestKind" {
            $f = New-Fixture
            $path = Join-Path $f.Root 'tools/smoke/scenarios.txt'
            [IO.File]::Delete($path)
            if ($manifestKind -eq 'directory') { [IO.Directory]::CreateDirectory($path) | Out-Null }
            if ($manifestKind -eq 'comments-only') { Write-FixtureText $path "  # no scenarios`r`n `t" }
            $result = Invoke-Batch $f
            Assert-Refused $f $result
        }
    }
    foreach ($extension in @('commands', 'expect')) {
        Invoke-Case "existing .$extension sidecar directory is an error, not silently missing" {
            $f = New-Fixture
            $path = Join-Path $f.Root "tools/smoke/alpha.$extension"
            [IO.File]::Delete($path)
            [IO.Directory]::CreateDirectory($path) | Out-Null
            $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', 'alpha')
            Assert-Refused $f $result 'Unreadable smoke'
        }
    }
    foreach ($blockedOutput in @('run-file', 'commands-directory', 'expect-directory')) {
        Invoke-Case "output failure before boot: $blockedOutput" {
            $f = New-Fixture
            if ($blockedOutput -eq 'run-file') { Write-FixtureText $f.Run 'not a directory' }
            else {
                $extension = $blockedOutput.Split('-')[0]
                [IO.Directory]::CreateDirectory((Join-Path $f.Run "smoke-batch.$extension")) | Out-Null
            }
            $result = Invoke-Batch $f -Arguments @($f.NativeRoot, '--only', 'alpha')
            Assert-Refused $f $result
        }
    }
    if ($script:Failures.Count -gt 0) {
        throw ("FAILED: " + $script:Failures.Count + '/' + $script:Cases + ' cases; ' +
            $script:Assertions + " assertions attempted.`n" + ($script:Failures -join "`n"))
    }
} finally {
    $marker = Join-Path $script:TestTemp '.owned-fixture'
    if ([IO.Directory]::Exists($script:TestTemp)) {
        if (-not [IO.File]::Exists($marker) -or (Read-FixtureText $marker) -cne $script:FixtureId) {
            throw 'Refusing cleanup of an unrecognized fixture directory.'
        }
        Remove-Item -LiteralPath $script:TestTemp -Recurse -Force
        if ([IO.Directory]::Exists($script:TestTemp)) { throw 'Temporary fixture cleanup failed.' }
        Write-Output 'Cleanup: removed only the owned temporary fixture subtree.'
    }
}
Write-Output ("ALL SMOKES BATCH CONTRACTS PASSED: " + $script:Cases + ' cases; ' +
    $script:Assertions + ' assertions; ' + $script:NativeRuns + ' native Bash CLI runs; ' +
    $script:ReferenceRuns + ' PowerShell reference CLI comparisons (safe intercepted launch boundary).')
Write-Output 'No real smoke harness, JVM, Gradle, SSH, network, save or private PC configuration was used.'
Write-Output 'Boundary: fake launch tests do not prove SMOKE-READY/handshake, server assertions or real harness process cleanup.'
