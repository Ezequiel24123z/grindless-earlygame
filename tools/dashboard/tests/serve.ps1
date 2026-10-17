[CmdletBinding()]
param([ValidateRange(1024, 65535)][int]$Port = 8877)

$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
if ($env:JAVA_HOME -and -not (Test-Path -LiteralPath $java)) {
    throw 'JAVA_HOME does not contain bin\java.exe. Select JDK 17 before starting the test server.'
}
$root = Split-Path $PSScriptRoot -Parent
& $java --add-modules jdk.httpserver (Join-Path $PSScriptRoot 'DashboardTestServer.java') $root $Port
exit $LASTEXITCODE
