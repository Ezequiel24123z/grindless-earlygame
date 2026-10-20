param(
    [Parameter(Mandatory = $true)][string]$Python,
    [ValidateSet('comfy_asset', 'visual_presets', 't0_visuals')][string]$Tool = 'comfy_asset',
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$PipelineArgs
)

$ErrorActionPreference = 'Stop'
& $Python -B -c "import sys,runpy; sys.path.insert(0,sys.argv.pop(1)); runpy.run_module(sys.argv.pop(1),run_name='__main__')" $PSScriptRoot $Tool @PipelineArgs
exit $LASTEXITCODE
