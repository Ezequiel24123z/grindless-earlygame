# Development-only deterministic processing. Never generates, reads credentials or grants listening approval.
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$InputDirectory,
    [Parameter(Mandatory = $true)][string]$CandidateId,
    [Parameter(Mandatory = $true)][ValidatePattern('^t0_[a-z_]+$')][string]$EventName,
    [double]$TrimStart = 0,
    [double]$TrimEnd = 3,
    [double]$CrossfadeSeconds = 0,
    [string]$EncoderPath,
    [string]$ProbePath
)

. (Join-Path $PSScriptRoot 'sound-design.ps1')
try {
    $inputs = Get-Inputs (Join-Path $PSScriptRoot 't0-physical-purpose.json') (Join-Path $PSScriptRoot 't0-physical-contract.json')
    $inputRoot = Get-OutputPath $InputDirectory
    $receipt = Read-SoundJson (Join-Path $inputRoot 'receipt.json')
    Assert-Receipt $receipt $inputs $inputRoot
    $attempts = @($receipt.attempts | Where-Object { $_.candidate_id -ceq $CandidateId -and $_.state -ceq 'response_saved' })
    Require-Sound ($attempts.Count -eq 1) 'Exactly one saved authorized source is required; no provider replay.'
    $raw = Join-Path $inputRoot ($CandidateId + '.raw.mp3')
    $preview = Join-Path $inputRoot ($CandidateId + '.preview.wav')
    $original = Read-SoundJson (Join-Path $inputRoot ($CandidateId + '.qc.json'))
    Require-Sound ((Get-SoundHash $raw) -ceq $attempts[0].source_sha256 -and
        (Get-SoundHash $preview) -ceq $original.preview_sha256) 'Preserved source/preview hash mismatch.'
    Require-Sound ($TrimStart -ge 0 -and $TrimEnd -le 3 -and $TrimEnd - $TrimStart -ge 2 -and
        $CrossfadeSeconds -ge 0 -and $CrossfadeSeconds -le 0.1) 'Only a bounded two-to-three-second source window and at most 100ms crossfade are allowed.'
    Require-Sound ($CrossfadeSeconds -gt 0 -or ($TrimStart -eq 0 -and $TrimEnd -eq 3)) 'Trimming requires an explicit periodic crossfade; never silently pad an incomplete loop.'
    $output = Join-Path $inputRoot ('derived-' + $EventName)
    Require-Sound (-not [IO.Directory]::Exists($output)) 'Derived output already exists; preserve it rather than overwriting processing evidence.'
    $tools = Get-SoundTools $EncoderPath $ProbePath $true
    [void][IO.Directory]::CreateDirectory($output)
    $culture = [Globalization.CultureInfo]::InvariantCulture
    $adapted = $preview
    $filter = $null
    if ($CrossfadeSeconds -gt 0) {
        $start = $TrimStart.ToString('R', $culture); $end = $TrimEnd.ToString('R', $culture)
        $crossfade = $CrossfadeSeconds.ToString('R', $culture)
        $periodSamples = [int][Math]::Round(($TrimEnd - $TrimStart - $CrossfadeSeconds) * 44100)
        Require-Sound ($periodSamples % 3 -eq 0) 'Periodic resampling must map exactly to 132300 output frames.'
        $period = ($periodSamples / 44100.0).ToString('R', $culture)
        $twice = (2 * $periodSamples / 44100.0).ToString('R', $culture)
        $rate = [int]($periodSamples / 3)
        $filter = "[0:a]atrim=start=${start}:end=${end},asetpts=PTS-STARTPTS,asplit=3[a][b][c];[a][b]acrossfade=d=${crossfade}:c1=tri:c2=tri[ab];[ab][c]acrossfade=d=${crossfade}:c1=tri:c2=tri,atrim=start=${period}:end=${twice},asetpts=PTS-STARTPTS,asetrate=${rate},aresample=44100[out]"
        $adapted = Join-Path $output 'periodic.wav'
        $null = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-v','error','-protocol_whitelist','file',
            '-i',$preview,'-filter_complex',$filter,'-map','[out]','-map_metadata','-1',
            '-fflags','+bitexact','-flags:a','+bitexact','-c:a','pcm_s16le',$adapted)
    }
    $before = Measure-Wave $adapted
    Require-Sound ($before.rms_linear -gt 0 -and $before.peak_linear -gt 0) 'Cannot normalize silent audio.'
    $gain = [Math]::Min(0.03 / $before.rms_linear, 0.35 / $before.peak_linear)
    $gainText = $gain.ToString('R', $culture)
    $normalized = Join-Path $output 'normalized.wav'
    $null = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-v','error','-protocol_whitelist','file',
        '-i',$adapted,'-af',('volume=' + $gainText),'-map_metadata','-1','-ac','1','-ar','44100',
        '-fflags','+bitexact','-flags:a','+bitexact','-c:a','pcm_s16le',$normalized)
    $ogg = Join-Path $output ($EventName + '.ogg')
    $null = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-v','error','-protocol_whitelist','file',
        '-i',$normalized,'-map_metadata','-1','-ac','1','-ar','44100',
        '-fflags','+bitexact','-flags:a','+bitexact','-c:a','libvorbis','-q:a','4',$ogg)
    $decoded = Join-Path $output 'decoded.wav'
    $null = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-v','error','-protocol_whitelist','file',
        '-i',$ogg,'-map_metadata','-1','-c:a','pcm_f32le',$decoded)
    $metrics = Measure-Wave $decoded
    $report = [ordered]@{
        schema = 'grindless.physical-sound-processing/v1'; candidate_id = $CandidateId; event = $EventName
        source_sha256 = (Get-SoundHash $raw); source_preview_sha256 = (Get-SoundHash $preview)
        original_qc_sha256 = (Get-SoundHash (Join-Path $inputRoot ($CandidateId + '.qc.json')))
        generation_receipt_sha256 = (Get-SoundHash (Join-Path $inputRoot 'receipt.json'))
        processing_script_sha256 = (Get-SoundHash $PSCommandPath)
        trim_start = $TrimStart; trim_end = $TrimEnd; crossfade_seconds = $CrossfadeSeconds
        periodic_filter = $filter; pre_gain_metrics = $before; gain_linear = $gain
        gain_rule = 'min(0.03/RMS, 0.35/peak)'; normalized_sha256 = (Get-SoundHash $normalized)
        ogg_sha256 = (Get-SoundHash $ogg); ogg_bytes = (Get-Item -LiteralPath $ogg).Length
        probe = (Get-Probe $ogg $tools); decoded_metrics = $metrics
        ffmpeg_sha256 = (Get-SoundHash $tools.Ffmpeg); ffprobe_sha256 = (Get-SoundHash $tools.Ffprobe)
        bitexact = $true; listening_pass = $false; runtime_imported = $false
    }
    Write-SoundJson (Join-Path $output 'processing.json') $report
    Require-Sound ($metrics.flags.Count -eq 0 -and $metrics.peak_linear -le 0.4) 'Encoded sound fails technical QC; preserve the derivation and do not import.'
    $report | ConvertTo-Json -Depth 12
} catch {
    [Console]::Error.WriteLine($script:SafeFailure)
    exit 1
}
