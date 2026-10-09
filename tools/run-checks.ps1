# Compiles and runs the behaviour checks in tools/checks/ against the compiled common classes.
#
#     powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .
#
# Run `gradlew :common:build` first; this checks the classes Gradle produced rather than
# recompiling them, so what is verified is what would ship.
#
# Why this is not a JUnit suite: the project has no test infrastructure yet, and adding one is
# step 26's job. These checks exist because the alternative was trusting that arithmetic and
# defensive branches were right without ever running them, and they have already caught two real
# bugs — an EnumMap constructor that throws on an empty source map, and a band efficiency
# expectation that was simply wrong.
#
# Expect noise on stderr. The classpath is the whole Gradle module cache, which contains several
# slf4j versions, so anything touching a class with a logger prints an "Unexpected problem occured
# during version sanity check" stack trace. slf4j catches it and carries on; the suites still run
# and still report correctly. The process exit code is the authoritative result, not the output.
#
# Two Windows details are load-bearing:
#   1. The classpath needs the whole Gradle module cache. `Direction.<clinit>` drags in most of
#      Minecraft's bootstrap - DataFixerUpper, brigadier, joml - so a minimal classpath fails at
#      runtime with a cascade of NoClassDefFoundError that says nothing useful.
#   2. That classpath is far past the Windows command-line limit, so it goes in an argfile. Inside
#      an argfile a backslash is an escape character, so the paths are rewritten with forward
#      slashes; without that, javac reports every package as missing.

param([Parameter(Mandatory = $true)][string]$Root)

$ErrorActionPreference = 'Stop'

# The project targets Java 17, but a Windows PATH commonly still resolves to an old Java 8
# launcher.  Java 8 treats the argfile below as a class name, making every check look as though
# it failed. Prefer an explicitly configured JDK 17; otherwise discover Temurin's standard JDK
# 17 installation.
$javaHome = $env:JAVA_HOME
if ($javaHome -and (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
    $configuredVersion = & (Join-Path $javaHome 'bin\javac.exe') -version 2>&1
    if ($configuredVersion -notmatch '^javac 17(\.|$)') { $javaHome = $null }
} else {
    $javaHome = $null
}

if (-not $javaHome) {
    $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path -LiteralPath $adoptium) {
        $candidate = Get-ChildItem -LiteralPath $adoptium -Directory -Filter 'jdk-17*' |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
            Select-Object -First 1
        if ($candidate) { $javaHome = $candidate.FullName }
    }
}

if (-not $javaHome -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\javac.exe'))) {
    Write-Output 'A JDK 17 is required for checks. Set JAVA_HOME to its installation directory.'
    exit 1
}

$java = Join-Path $javaHome 'bin\java.exe'
$javac = Join-Path $javaHome 'bin\javac.exe'

$classes = Join-Path $Root 'common\build\classes\java\main'
if (-not (Test-Path -LiteralPath $classes)) {
    Write-Output "No compiled classes at $classes - run gradlew :common:build first."
    exit 1
}

$checkDir = Join-Path $Root 'tools\checks'
$sources = @(Get-ChildItem -Path $checkDir -Filter '*.java' -File | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    Write-Output "No checks found in $checkDir."
    exit 1
}

# The mapped Minecraft jar Loom produced, identified by actually containing a Mojang-named class
# rather than by guessing at the filename, which encodes a mappings hash.
$mcJar = $null
$loom = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\minecraftMaven'
if (Test-Path -LiteralPath $loom) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($candidate in Get-ChildItem -Path $loom -Filter '*.jar' -Recurse -ErrorAction SilentlyContinue) {
        if ($candidate.Length -lt 5MB) { continue }
        try {
            $zip = [System.IO.Compression.ZipFile]::OpenRead($candidate.FullName)
            $hit = $zip.Entries | Where-Object { $_.FullName -eq 'net/minecraft/core/Direction.class' } | Select-Object -First 1
            $zip.Dispose()
            if ($hit) { $mcJar = $candidate.FullName; break }
        } catch { }
    }
}
if (-not $mcJar) {
    Write-Output 'Could not locate a mapped Minecraft jar - run a build first.'
    exit 1
}

$deps = @(Get-ChildItem -Path (Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1') `
        -Filter '*.jar' -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } |
    ForEach-Object { $_.FullName })

$out = Join-Path ([System.IO.Path]::GetTempPath()) ("grindless-checks-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $out | Out-Null

try {
    $compileCp = ((@($classes, $mcJar) + $deps) -join ';').Replace('\', '/')
    $runCp = ((@($out, $classes, $mcJar) + $deps) -join ';').Replace('\', '/')
    $compileArgs = Join-Path $out 'compile.txt'
    $runArgs = Join-Path $out 'run.txt'
    Set-Content -Path $compileArgs -Value "-cp `"$compileCp`"" -Encoding ASCII
    Set-Content -Path $runArgs -Value "-cp `"$runCp`"" -Encoding ASCII

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
        & $java "@$runArgs" "$package.$name"
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
