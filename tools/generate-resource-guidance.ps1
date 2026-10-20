[CmdletBinding()]
param(
    [string]$Root,
    [switch]$CheckOnly,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Root)) { $Root = Split-Path $PSScriptRoot -Parent }
$Root = (Resolve-Path -LiteralPath $Root).Path
$action = if ($CheckOnly) { 'check' } else { 'write' }
# Use the existing canonical game-edge binding and Gradle-resolved compiler/classpath wrapper.
& (Join-Path $Root 'tools\dashboard\export.ps1') -Root $Root -ResourceGuidance $action -Offline:$Offline
if ($LASTEXITCODE -ne 0) { throw 'Canonical resource guidance generation/check failed.' }
