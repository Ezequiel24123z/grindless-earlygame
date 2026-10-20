# Compiles and runs the behaviour checks in tools/checks/ against the compiled common classes.
#
#     powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .
#
# Gradle compiles common and exports its resolved runtime classpath before these checks run.
# The checks therefore use the classes and dependencies that would ship, not a cache scan.
#
# Why this is not a JUnit suite: the project has no test infrastructure yet, and adding one is
# step 26's job. These checks exist because the alternative was trusting that arithmetic and
# defensive branches were right without ever running them, and they have already caught two real
# bugs — an EnumMap constructor that throws on an empty source map, and a band efficiency
# expectation that was simply wrong.
#
# The classpath goes in an argfile to avoid the Windows command-line limit. Inside
#      an argfile a backslash is an escape character, so the paths are rewritten with forward
#      slashes; without that, javac reports every package as missing.

param([Parameter(Mandatory = $true)][string]$Root)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path $Root).Path

function Test-Javac17([string]$Javac) {
    $probe = New-Object System.Diagnostics.Process
    $probe.StartInfo.FileName = $Javac
    $probe.StartInfo.Arguments = '-version'
    $probe.StartInfo.UseShellExecute = $false
    $probe.StartInfo.CreateNoWindow = $true
    $probe.StartInfo.RedirectStandardOutput = $true
    $probe.StartInfo.RedirectStandardError = $true
    try {
        if (-not $probe.Start()) { throw "Could not start the JDK version probe: $Javac" }
        # Drain both pipes before waiting; either stream can contain launcher diagnostics.
        $stdout = $probe.StandardOutput.ReadToEndAsync()
        $stderr = $probe.StandardError.ReadToEndAsync()
        $probe.WaitForExit()
        $output = $stdout.GetAwaiter().GetResult()
        $diagnostic = $stderr.GetAwaiter().GetResult()
        if ($output) { Write-Host -NoNewline $output }
        if ($diagnostic) { Write-Host -NoNewline $diagnostic }
        if ($probe.ExitCode -ne 0) { Write-Host "javac -version exited with code $($probe.ExitCode)." }
        $versions = @(($output + "`n" + $diagnostic) -split '\r?\n' |
            Where-Object { $_ -cmatch '^javac ' })
        return ($probe.ExitCode -eq 0 -and $versions.Count -eq 1 -and
            $versions[0] -cmatch '^javac 17(\.[0-9]+)*([-+][0-9A-Za-z][0-9A-Za-z.+-]*)?$')
    } finally {
        $probe.Dispose()
    }
}

# The project targets Java 17, but a Windows PATH commonly still resolves to an old Java 8
# launcher.  Java 8 treats the argfile below as a class name, making every check look as though
# it failed. Prefer an explicitly configured JDK 17; otherwise discover Temurin's standard JDK
# 17 installation.
$javaHome = $env:JAVA_HOME
if ($javaHome -and (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
    if (-not (Test-Javac17 (Join-Path $javaHome 'bin\javac.exe'))) { $javaHome = $null }
} else {
    $javaHome = $null
}

if (-not $javaHome) {
    $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path -LiteralPath $adoptium) {
        $candidate = Get-ChildItem -LiteralPath $adoptium -Directory -Filter 'jdk-17*' |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
            Select-Object -First 1
        if ($candidate -and (Test-Javac17 (Join-Path $candidate.FullName 'bin\javac.exe'))) {
            $javaHome = $candidate.FullName
        }
    }
}

if (-not $javaHome -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
    Write-Output 'A JDK 17 is required for checks. Set JAVA_HOME to its installation directory.'
    exit 1
}

$java = Join-Path $javaHome 'bin\java.exe'
$javac = Join-Path $javaHome 'bin\javac.exe'

$checkDir = Join-Path $Root 'tools\checks'
$sources = @(Get-ChildItem -Path $checkDir -Filter '*.java' -File | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    Write-Output "No checks found in $checkDir."
    exit 1
}

$env:JAVA_HOME = $javaHome
Push-Location $Root
try {
    & (Join-Path $Root 'gradlew.bat') --no-daemon --quiet --console=plain :common:writeCheckClasspath
    if ($LASTEXITCODE -ne 0) {
        Write-Output 'COULD NOT PREPARE CHECK CLASSPATH'
        exit 1
    }
} finally {
    Pop-Location
}
$classpathFile = Join-Path $Root 'common\build\checks\runtime-classpath.txt'
$classpath = [System.IO.File]::ReadAllText($classpathFile, [System.Text.Encoding]::UTF8)
if ([string]::IsNullOrWhiteSpace($classpath)) { throw 'Gradle exported an empty check classpath.' }

$out = Join-Path $Root ("build\checks-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $out | Out-Null

try {
    $compileCp = $classpath.Replace('\', '/')
    $runCp = ("$out;$classpath").Replace('\', '/')
    $compileArgs = Join-Path $out 'compile.txt'
    $runArgs = Join-Path $out 'run.txt'
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($compileArgs, "-cp `"$compileCp`"", $utf8)
    [System.IO.File]::WriteAllText($runArgs, "-cp `"$runCp`"", $utf8)

    & $javac -nowarn -encoding UTF-8 "@$compileArgs" -d $out $sources
    if ($LASTEXITCODE -ne 0) {
        Write-Output 'CHECKS DID NOT COMPILE'
        exit 1
    }

    $failed = 0
    foreach ($source in $sources) {
        $name = [System.IO.Path]::GetFileNameWithoutExtension($source)
        $package = (Select-String -Path $source -Pattern '^package\s+([\w.]+);' | Select-Object -First 1).Matches[0].Groups[1].Value
        Write-Output "--- $name ---"
        Push-Location $Root
        try {
            & $java "@$runArgs" "$package.$name"
        } finally {
            Pop-Location
        }
        if ($LASTEXITCODE -ne 0) { $failed++ }
    }

    if ($failed -gt 0) {
        Write-Output "$failed CHECK SUITE(S) FAILED"
        exit 1
    }
    Write-Output 'ALL CHECK SUITES PASSED'
    # Explicit, because without it the script exits with whatever $LASTEXITCODE the last native
    # command happened to leave behind. A check runner that reports failure on success is worse
    # than no check runner: it trains you to ignore it.
    exit 0
} finally {
    Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
}
