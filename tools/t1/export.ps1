[CmdletBinding()]
param(
    [string]$Root,
    [string]$Output = 'docs\tiers\t1-production-graph.json',
    [switch]$CheckOnly,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Root)) { $Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent }
$Root = (Resolve-Path -LiteralPath $Root).Path
if (-not $env:JAVA_HOME) { throw 'Select JDK 17 with JAVA_HOME before exporting the T1 graph.' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
$version = & $javac -version 2>&1
if ($LASTEXITCODE -ne 0 -or $version -notmatch '^javac 17(\.|$)') { throw 'T1 export requires JDK 17.' }
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $Root '.gradle\user-home' }
$gradleArgs = @('--no-daemon', '--quiet', '--console=plain', '--max-workers=2',
    '-Dorg.gradle.jvmargs=-Xmx768m -XX:ActiveProcessorCount=2')
if ($Offline) { $gradleArgs += '--offline' }
$gradleArgs += ':common:writeCheckClasspath'
Push-Location $Root
try {
    & (Join-Path $Root 'gradlew.bat') @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw 'Gradle could not prepare the existing T1 check classpath.' }
    $classpath = [IO.File]::ReadAllText((Join-Path $Root 'common\build\checks\runtime-classpath.txt')).Trim()
    if ([string]::IsNullOrWhiteSpace($classpath)) { throw 'Gradle exported an empty classpath.' }
    $out = Join-Path $Root ('build\t1-tools-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $out | Out-Null
    try {
        $sources = @('VerifySustainability.java', 'VerifySustainabilityAdapters.java',
            'VerifySustainabilityRegistry.java', 'VerifySustainabilityT0.java',
            'VerifyResourceGuidance.java', 'VerifyEconomy.java', 'VerifyT1Progression.java') |
            ForEach-Object { Join-Path $Root ('tools\checks\' + $_) }
        $sources += Join-Path $Root 'tools\t1\T1Export.java'
        $utf8 = New-Object Text.UTF8Encoding($false)
        $compile = Join-Path $out 'compile.txt'
        $run = Join-Path $out 'run.txt'
        [IO.File]::WriteAllText($compile, ('-cp "' + $classpath.Replace('\', '/') + '"'), $utf8)
        [IO.File]::WriteAllText($run, ('-cp "' + ($out + ';' + $classpath).Replace('\', '/') + '"'), $utf8)
        & $javac -J-Xmx256m -J-XX:ActiveProcessorCount=2 -nowarn -encoding UTF-8 --release 17 "@$compile" -d $out @sources
        if ($LASTEXITCODE -ne 0) { throw 'T1 tools did not compile.' }
        $action = if ($CheckOnly) { 'check' } else { 'write' }
        & $java -Xmx256m -XX:ActiveProcessorCount=2 --add-modules jdk.compiler "@$run" `
            io.github.ezequiel24123z.grindless.sustainability.T1Export $Root $Output $action
        if ($LASTEXITCODE -ne 0) { throw 'T1 export/check failed; no acceptance claim is implied.' }
    } finally {
        Remove-Item -LiteralPath $out -Recurse -Force
    }
} finally {
    Pop-Location
}
