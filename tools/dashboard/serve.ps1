[CmdletBinding()]
param(
    [string]$Artifact,
    [ValidateRange(1024, 65535)][int]$Port = 8878
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Artifact)) {
    $Artifact = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'build\dashboard\site'
}
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$Artifact = (Resolve-Path -LiteralPath $Artifact).Path
& $java -Xmx128m -XX:ActiveProcessorCount=2 --add-modules jdk.httpserver `
    (Join-Path $PSScriptRoot 'DashboardServer.java') $Artifact $Port
exit $LASTEXITCODE
