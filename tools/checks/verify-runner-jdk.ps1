param([string]$Root = (Join-Path $PSScriptRoot '..\..'))

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
if (-not $env:JAVA_HOME) { throw 'Select the existing JDK 17 through JAVA_HOME.' }
$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
$tokens = $null
$errors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile(
    (Join-Path $Root 'tools\run-checks.ps1'), [ref]$tokens, [ref]$errors)
if ($errors.Count -ne 0) { throw 'The official runner does not parse.' }
$function = $ast.Find({
    param($node)
    $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
        $node.Name -eq 'Test-Javac17'
}, $false)
if (-not $function) { throw 'The official JDK probe is missing.' }
Invoke-Expression $function.Extent.Text

$out = Join-Path $Root ('build\runner-jdk-probe-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $out | Out-Null
$fixture = Join-Path $out 'javac.exe'
Add-Type -OutputAssembly $fixture -OutputType ConsoleApplication -TypeDefinition @'
using System;
public static class JavacFixture {
    public static int Main(string[] args) {
        if (args.Length != 1 || args[0] != "-version") return 91;
        string mode = Environment.GetEnvironmentVariable("GRINDLESS_JAVAC_PROBE_CASE");
        Console.WriteLine("fixture-stdout:" + mode);
        Console.Error.WriteLine("fixture-stderr:" + mode);
        switch (mode) {
            case "stdout": Console.WriteLine("javac 17.0.20.1"); return 0;
            case "stderr": Console.Error.WriteLine("javac 17.0.20.1"); return 0;
            case "banner":
                Console.Error.WriteLine("Picked up JAVA_TOOL_OPTIONS: -Xmx256m -XX:ActiveProcessorCount=2");
                Console.Error.WriteLine("javac 17.0.20.1"); return 0;
            case "wrong": Console.WriteLine("javac 21.0.1"); Console.Error.WriteLine("diagnostic mentions 17"); return 0;
            case "missing": Console.WriteLine("a compiler mentioning 17 is not a version"); return 0;
            case "nonzero": Console.WriteLine("javac 17.0.20.1"); return 7;
            case "ambiguous": Console.WriteLine("javac 17.0.20.1"); Console.Error.WriteLine("javac 21.0.1"); return 0;
            case "suffix": Console.WriteLine("javac 17.invalid"); return 0;
            case "flood":
                Console.WriteLine(new string('O', 131072));
                Console.Error.WriteLine(new string('E', 131072));
                Console.WriteLine("javac 17.0.20.1"); return 0;
            default: return 99;
        }
    }
}
'@

$assertions = 0
function Assert-Probe([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:assertions++
}
$previousOptions = $env:JAVA_TOOL_OPTIONS
$previousCase = $env:GRINDLESS_JAVAC_PROBE_CASE
try {
    $env:JAVA_TOOL_OPTIONS = '-Xmx256m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC'
    $oldRejected = $false
    try {
        $oldVersion = & $javac -version 2>&1
        $oldRejected = [bool]($LASTEXITCODE -ne 0 -or $oldVersion -notmatch '^javac 17(\.|$)')
    } catch {
        # WinPS hosts wrap this expected native error in different exception types.
        if ($_.FullyQualifiedErrorId -cne 'NativeCommandError') { throw }
        $oldRejected = $true
        $_ | Out-String | Set-Content -LiteralPath (Join-Path $out 'old-guard-error.txt') -Encoding UTF8
    }
    Assert-Probe $oldRejected 'The original guard must reproduce its banner rejection.'

    $script:accepted = $false
    $real = @(& { $script:accepted = Test-Javac17 $javac } 6>&1) | Out-String
    $real | Set-Content -LiteralPath (Join-Path $out 'real-jdk17.txt') -Encoding UTF8
    Assert-Probe $script:accepted 'The real JDK 17 must pass with JAVA_TOOL_OPTIONS.'
    Assert-Probe ($real -match 'Picked up JAVA_TOOL_OPTIONS:') 'The real launcher diagnostic was lost.'
    Assert-Probe ($real -match '(?m)^javac 17\.') 'The real version was lost.'

    $cases = [ordered]@{
        stdout = $true; stderr = $true; banner = $true; wrong = $false
        missing = $false; nonzero = $false; ambiguous = $false; suffix = $false; flood = $true
    }
    foreach ($case in $cases.GetEnumerator()) {
        $env:GRINDLESS_JAVAC_PROBE_CASE = $case.Key
        $script:accepted = $false
        $diagnostic = @(& { $script:accepted = Test-Javac17 $fixture } 6>&1) | Out-String
        $diagnostic | Set-Content -LiteralPath (Join-Path $out ($case.Key + '.txt')) -Encoding UTF8
        Assert-Probe ($script:accepted -eq $case.Value) ("Wrong guard result: " + $case.Key)
        Assert-Probe ($diagnostic.Contains('fixture-stdout:' + $case.Key)) ("stdout lost: " + $case.Key)
        Assert-Probe ($diagnostic.Contains('fixture-stderr:' + $case.Key)) ("stderr lost: " + $case.Key)
        if ($case.Key -eq 'nonzero') {
            Assert-Probe ($diagnostic.Contains('exited with code 7')) 'The nonzero exit diagnostic was lost.'
        }
    }
    Write-Output ("ALL RUNNER JDK PROBE CHECKS PASSED: " + $assertions)
    Write-Output ("Evidence: " + $out.Substring($Root.Length + 1))
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousOptions
    $env:GRINDLESS_JAVAC_PROBE_CASE = $previousCase
}
