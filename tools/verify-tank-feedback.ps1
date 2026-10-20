# The historical TANK-only checkpoint proof remains in Git; current checks share the complete palette contract.
param(
    [string]$Root = (Join-Path $PSScriptRoot '..'),
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'verify-mixer-audio.ps1') -Root $Root -SelfTest:$SelfTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
