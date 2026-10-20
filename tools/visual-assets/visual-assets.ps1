param(
    [Parameter(Mandatory = $true)][string]$Python,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$PipelineArgs
)

$ErrorActionPreference = 'Stop'
& $Python -B (Join-Path $PSScriptRoot 'comfy_asset.py') @PipelineArgs
exit $LASTEXITCODE
