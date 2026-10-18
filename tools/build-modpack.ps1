# Usage: powershell -ExecutionPolicy Bypass -File .\tools\build-modpack.ps1 [-Root .] [-Profile preparation]
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [ValidateSet('expanded', 'preparation')][string]$Profile = 'expanded',
    [string]$RuntimeRevision
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$arguments = @('buildModpack', "-PpackProfile=$Profile", '--console=plain')
if ($RuntimeRevision) { $arguments += "-PpackRuntimeRevision=$RuntimeRevision" }
Push-Location $Root
try {
    & .\gradlew.bat @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Client-pack build failed.' }
} finally {
    Pop-Location
}
