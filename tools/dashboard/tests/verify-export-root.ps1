[CmdletBinding()]
param(
    [string]$Root,
    [switch]$Compile
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) -Parent
}
$Root = (Resolve-Path -LiteralPath $Root).Path
$relativeSources = @(
    'tools\checks\VerifySustainability.java',
    'tools\checks\VerifySustainabilityAdapters.java',
    'tools\checks\VerifySustainabilityRegistry.java',
    'tools\checks\VerifySustainabilityT0.java',
    'tools\dashboard\DashboardExport.java',
    'tools\dashboard\tests\VerifyDashboardProjection.java'
)
$script:assertions = 0

function Assert-RootSelection([bool]$Condition, [string]$Message) {
    $script:assertions++
    if (-not $Condition) { throw $Message }
}

function Invoke-FixtureGit([string]$Directory, [string[]]$Arguments) {
    $output = & git --no-pager -c core.autocrlf=false -C $Directory @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'Source-root fixture Git operation failed.' }
    return ($output -join "`n").Trim()
}

function Get-ActualSourceSelectionBody([string]$Script) {
    $tokens = $null
    $errors = $null
    $ast = [Management.Automation.Language.Parser]::ParseFile($Script, [ref]$tokens, [ref]$errors)
    if ($errors.Count -ne 0) { throw 'The actual export script did not parse.' }
    $assignments = @($ast.FindAll({
        param($node)
        $node -is [Management.Automation.Language.AssignmentStatementAst] -and $node.Left.Extent.Text -eq '$sources'
    }, $true))
    Assert-RootSelection ($assignments.Count -eq 3) 'The actual source-selection boundary changed; update this focused test.'
    return "param([string]`$Root)`n" + (($assignments | ForEach-Object { $_.Extent.Text }) -join "`n") + "`n`$sources"
}

$build = Join-Path $Root 'build'
$fixture = Join-Path $build ('dr-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
$caller = Join-Path $fixture 'caller A'
$selectedRoot = Join-Path $fixture 'selected B'
New-Item -ItemType Directory -Path $caller, $selectedRoot -Force | Out-Null
try {
    foreach ($directory in @($caller, $selectedRoot)) {
        foreach ($relative in ($relativeSources + 'tools\dashboard\export.ps1')) {
            $destination = Join-Path $directory $relative
            New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
            Copy-Item -LiteralPath (Join-Path $Root $relative) -Destination $destination
        }
        [IO.File]::WriteAllText((Join-Path $directory '.gitignore'), "build/`n")
        $body = Get-ActualSourceSelectionBody (Join-Path $directory 'tools\dashboard\export.ps1')
        [IO.File]::WriteAllText((Join-Path $directory 'tools\dashboard\source-selection-fixture.ps1'), $body,
            (New-Object Text.UTF8Encoding($false)))
    }
    $callerExport = Join-Path $caller 'tools\dashboard\DashboardExport.java'
    $callerTest = Join-Path $caller 'tools\dashboard\tests\VerifyDashboardProjection.java'
    [IO.File]::WriteAllText($callerExport, 'Caller A poison: this source must never be compiled for B.')
    [IO.File]::WriteAllText($callerTest, 'Caller A poison: this test must never be compiled for B.')
    foreach ($directory in @($caller, $selectedRoot)) {
        Invoke-FixtureGit $directory @('init', '--quiet') | Out-Null
        Invoke-FixtureGit $directory @('add', '--all') | Out-Null
        Invoke-FixtureGit $directory @('-c', 'user.name=Copilot App',
            '-c', 'user.email=223556219+Copilot@users.noreply.github.com', 'commit', '--quiet',
            '-m', 'Record a source-root selection fixture') | Out-Null
    }
    $callerSelection = Join-Path $caller 'tools\dashboard\source-selection-fixture.ps1'
    foreach ($state in @('divergent_clean_caller', 'divergent_dirty_caller')) {
        if ($state -eq 'divergent_dirty_caller') {
            [IO.File]::AppendAllText($callerExport, "`nDirty caller A must remain excluded.")
            [IO.File]::AppendAllText($callerTest, "`nDirty caller A must remain excluded.")
        }
        $callerStatus = Invoke-FixtureGit $caller @('status', '--porcelain', '--untracked-files=normal')
        Assert-RootSelection (($callerStatus.Length -gt 0) -eq ($state -eq 'divergent_dirty_caller')) `
            'Caller A did not have the requested real Git state.'
        Assert-RootSelection ((Invoke-FixtureGit $selectedRoot @('status', '--porcelain', '--untracked-files=normal')) -eq '') `
            'Selected B must remain genuinely clean; do not suppress its Git guard.'
        $sources = @(& $callerSelection -Root $selectedRoot)
        Assert-RootSelection ($sources.Count -eq $relativeSources.Count) 'The compiler source inventory changed.'
        for ($index = 0; $index -lt $relativeSources.Count; $index++) {
            $expected = Join-Path $selectedRoot $relativeSources[$index]
            Assert-RootSelection ($sources[$index] -eq $expected) `
                ('Compiler source did not come from selected B: ' + $relativeSources[$index])
            Assert-RootSelection ((Get-FileHash -Algorithm SHA256 -LiteralPath $sources[$index]).Hash -eq
                (Get-FileHash -Algorithm SHA256 -LiteralPath $expected).Hash) 'Selected source bytes differ from B.'
        }
        foreach ($relative in @('tools\dashboard\DashboardExport.java', 'tools\dashboard\tests\VerifyDashboardProjection.java')) {
            Assert-RootSelection ((Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $caller $relative)).Hash -ne
                (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $selectedRoot $relative)).Hash) `
                'The cross-root fixture must actually contain divergent tools.'
        }
    }
    if ($Compile) {
        if (-not $env:JAVA_HOME) { throw 'Select the existing JDK 17 with JAVA_HOME for -Compile.' }
        $classpath = [IO.File]::ReadAllText((Join-Path $Root 'common\build\checks\runtime-classpath.txt')).Trim()
        if ([string]::IsNullOrWhiteSpace($classpath)) { throw 'Use an already Gradle-resolved classpath; this test never runs Gradle.' }
        $classes = Join-Path $selectedRoot 'build\classes'
        New-Item -ItemType Directory -Path $classes | Out-Null
        $arguments = Join-Path $classes 'classpath.txt'
        [IO.File]::WriteAllText($arguments, ('-cp "' + $classpath.Replace('\', '/') + '"'),
            (New-Object Text.UTF8Encoding($false)))
        & (Join-Path $env:JAVA_HOME 'bin\javac.exe') -J-Xmx256m -J-XX:ActiveProcessorCount=2 `
            -nowarn -encoding UTF-8 --release 17 "@$arguments" -d $classes @sources
        if ($LASTEXITCODE -ne 0) { throw 'Actual selected-B sources did not compile.' }
        Assert-RootSelection (Test-Path -LiteralPath (Join-Path $classes `
            'io\github\ezequiel24123z\grindless\sustainability\DashboardExport.class')) 'Selected-B exporter class is missing.'
    }
    Assert-RootSelection ((Invoke-FixtureGit $selectedRoot @('status', '--porcelain', '--untracked-files=normal')) -eq '') `
        'Compiling selected B must not dirty its tracked source.'
} finally {
    Remove-Item -LiteralPath ('\\?\' + $fixture) -Recurse -Force
}
Write-Output ("DASHBOARD ROOT SELECTION: {0} passed; PowerShell only; compile={1}; no production artifact, JVM audit or acceptance claim." `
    -f $script:assertions, [bool]$Compile)
