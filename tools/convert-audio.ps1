# Converts only the five synthetic legacy WAVs. Never rebuild the approved Quern Ogg (ADR-0148).
# Usage: powershell -ExecutionPolicy Bypass -File .\tools\convert-audio.ps1 -Root .
param([string]$Root = '.')

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$source = Join-Path $Root 'tools\audio'
$destination = Join-Path $Root 'common\src\main\resources\assets\grindless\sounds'
New-Item -ItemType Directory -Force -Path $destination | Out-Null

$names = @('machine_hum', 'machine_hum_heavy', 'pylon_link', 'relay_click', 'brownout_alarm')
foreach ($name in $names) {
    $wav = Join-Path $source ($name + '.wav')
    if (-not (Test-Path -LiteralPath $wav -PathType Leaf)) {
        throw "Missing generated WAV: $wav"
    }
    $ogg = Join-Path $destination ($name + '.ogg')
    & ffmpeg -y -v error -i $wav -ac 1 -ar 44100 -map_metadata -1 `
        -fflags +bitexact -flags:a +bitexact -c:a libvorbis -q:a 4 $ogg
    if ($LASTEXITCODE -ne 0) {
        throw "Could not convert $name; ffmpeg exited $LASTEXITCODE."
    }
    Write-Output "converted $name"
}
