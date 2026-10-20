[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$FfmpegPath,
    [Parameter(Mandatory=$true)][string]$FfprobePath,
    [Parameter(Mandatory=$true)][string]$EvidenceDirectory
)
$ErrorActionPreference = 'Stop'
# Preserve these before dot-sourcing the offline suite/main parameters.
$codecFfmpeg = $FfmpegPath; $codecFfprobe = $FfprobePath; $codecEvidence = $EvidenceDirectory
$codecTestScript = $PSCommandPath; $offlineTestScript = Join-Path $PSScriptRoot 'test-sound-design.ps1'
# Runs the 67 budget/contract/PCM guards, defines TEST fixture writer and failing transport guard.
. $offlineTestScript
$unitChecks = $script:Checks
$root = Get-OutputPath $codecEvidence
Require-Sound ([IO.Directory]::Exists($root)) 'Create the explicitly chosen evidence directory first.'
$run = Join-Path $root ('TEST-codecs-' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
Require-Sound (-not (Test-Path -LiteralPath $run)) 'Do not overwrite previous codec evidence.'
[void][IO.Directory]::CreateDirectory($run)
$reportPath = Join-Path $run 'TEST-codec-results.json'; $stage = 'preflight'
$inputs = Get-Inputs $ManifestPath $ContractPath
$report = [ordered]@{ schema = 'grindless.sound-codec-tests/v1'; TEST_only_not_art = $true; status = 'running'
    source_commit = (Get-GitHead); runner_sha256 = $inputs.ToolingHash; manifest_sha256 = $inputs.ManifestHash
    contract_sha256 = $inputs.ContractHash; offline_test_sha256 = (Get-SoundHash $offlineTestScript)
    codec_test_sha256 = (Get-SoundHash $codecTestScript); offline_checks = $unitChecks; total_checks = $null
    provider_posts = 0; secret_reads = 0; fake_receipts_only = $true; paid_budget_unspent = $true
    listening_pass = $false; import_approved = $false; publication_approved = $false
    ffmpeg_sha256 = $null; ffprobe_sha256 = $null; fixture_results = @(); artifact_hashes = @() }
Write-SoundJson $reportPath $report
try {
    $tools = Get-SoundTools $codecFfmpeg $codecFfprobe $true
    $report.ffmpeg_sha256 = Get-SoundHash $tools.Ffmpeg; $report.ffprobe_sha256 = Get-SoundHash $tools.Ffprobe
    $report['ffmpeg_version'] = ((Invoke-AudioTool $tools.Ffmpeg @('-version')) -split "`n")[0].Trim()
    $report['ffprobe_version'] = ((Invoke-AudioTool $tools.Ffprobe @('-version')) -split "`n")[0].Trim()
    Check ($report.ffmpeg_version -match '^ffmpeg version ' -and $report.ffprobe_version -match '^ffprobe version ') 'actual executable/version argument identities'
    $encoders = Invoke-AudioTool $tools.Ffmpeg @('-hide_banner','-encoders')
    Check ($encoders -match '\blibvorbis\b' -and $encoders -match '\blibmp3lame\b') 'real Vorbis and TEST MP3 encoders'
    $stage = 'native_streams'
    $null = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-hide_banner','-f','lavfi','-i','anullsrc=r=44100:cl=mono','-t','0.05','-f','null','-')
    Check ($script:LastAudioProcess.exit_code -eq 0 -and $script:LastAudioProcess.stderr.Length -gt 0) 'informational stderr is not a process failure'
    Write-SoundJson (Join-Path $run 'TEST-informational-stderr.json') $script:LastAudioProcess
    $hashText = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-hide_banner','-loglevel','debug','-f','lavfi','-i','sine=frequency=330:duration=1:sample_rate=44100','-f','framehash','-')
    Check ($hashText.Length -gt 0 -and $script:LastAudioProcess.stderr.Length -gt 4096 -and $script:LastAudioProcess.exit_code -eq 0) 'concurrent stdout and stderr draining without JSON contamination/deadlock'
    Write-SoundJson (Join-Path $run 'TEST-concurrent-streams.json') $script:LastAudioProcess
    Reject { Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-hide_banner','-v','error','-i',(Join-Path $run 'TEST missing input.mp3'),'-f','null','-') } 'real nonzero codec exit remains a failure'
    Check ($script:LastAudioProcess.exit_code -ne 0 -and $script:LastAudioProcess.stderr.Length -gt 0) 'nonzero native exit and diagnostics are retained'
    Write-SoundJson (Join-Path $run 'TEST-expected-nonzero-exit.json') $script:LastAudioProcess

    $stage = 'synthetic_mp3_inputs'
    $caseDir = Join-Path $run 'TEST saved responses with spaces'
    [void][IO.Directory]::CreateDirectory($caseDir)
    [IO.File]::WriteAllText((Join-Path $caseDir 'TEST-NOT-ART.txt'), 'TEST synthetic signals, NOT ElevenLabs outputs. Fake receipt, zero provider requests. Never import, publish or ship.', [Text.UTF8Encoding]::new($false))
    $fakeGrant = [pscustomobject]@{ id = 'test-codec-no-provider-not-authorization'; manifest_commit = (Get-GitHead) }
    $receiptPath = Join-Path $caseDir 'receipt.json'
    $receipt = New-Receipt $fakeGrant $inputs ('0' * 64) $caseDir
    Write-SoundJson $receiptPath $receipt
    $kinds = @('sine','silence','seam')
    for ($i = 0; $i -lt 3; $i++) {
        $id = $script:CandidateIds[$i]; $channels = 1; $rate = 44100
        if ($i -eq 0) { $channels = 2; $rate = 48000 }
        $wave = Join-Path $caseDir ($id + '.TEST-synthetic-never-ship.wav')
        [GrindlessTestWave]::Write($wave, $channels, $rate, 3, $kinds[$i])
        $receipt = Reserve-Attempt $receiptPath $receipt $inputs $caseDir $id
        $raw = Join-Path $caseDir ($id + '.raw.mp3')
        $null = Invoke-AudioTool $tools.Ffmpeg @('-nostdin','-hide_banner','-v','error','-protocol_whitelist','file','-i',$wave,'-map_metadata','-1','-metadata','title=TEST synthetic fixture - no provider - never ship','-c:a','libmp3lame','-b:a','128k',$raw)
        $attempt = $receipt.attempts[-1]; $attempt.raw_file = $id + '.raw.mp3'
        $attempt.source_sha256 = Get-SoundHash $raw; $attempt.http_status = 200
        $attempt.completed_utc = [DateTime]::UtcNow.ToString('o'); $attempt.state = 'response_saved'
        Write-SoundJson (Join-Path $caseDir ($id + '.provenance.json')) ([ordered]@{
            TEST_only_not_art = $true; provider = 'NONE - synthetic codec fixture'; provider_posts = 0
            fixture_kind = $kinds[$i]; source_wave_sha256 = (Get-SoundHash $wave); source_mp3_sha256 = $attempt.source_sha256
            licence = 'TEST_ONLY_NEVER_SHIP'; import_approved = $false; publication_approved = $false })
        Write-SoundJson $receiptPath $receipt
    }
    Assert-Receipt $receipt $inputs $caseDir
    Reject { Assert-CanAttempt $receipt } 'fake three-entry ledger remains exhausted'
    $stage = 'real_plan_and_qc_entrypoints'
    $hostExe = Join-Path $PSHOME 'powershell.exe'
    if ($PSVersionTable.PSEdition -eq 'Core') { $hostExe = Join-Path $PSHOME 'pwsh'; if ($env:OS -eq 'Windows_NT') { $hostExe += '.exe' } }
    $common = @('-NoProfile','-ExecutionPolicy','Bypass','-File',$script:SoundScript)
    $toolArgs = @('-FfmpegPath',$tools.Ffmpeg,'-FfprobePath',$tools.Ffprobe)
    $plan = (Invoke-AudioTool $hostExe ($common + @('-Mode','Plan') + $toolArgs)) | ConvertFrom-Json
    Check (-not $plan.network -and -not $plan.secret_read -and -not $plan.generation_authorized -and $plan.requests.Count -eq 3) 'actual Plan CLI remains offline/unapproved with explicit portable paths'
    Write-SoundJson (Join-Path $run 'TEST-plan.json') $plan
    $qcResult = (Invoke-AudioTool $hostExe ($common + @('-Mode','QC','-OutputDirectory',$caseDir) + $toolArgs)) | ConvertFrom-Json
    Check ($qcResult.saved_sources -eq 3 -and -not $qcResult.listening_pass -and -not $qcResult.import_approved) 'actual offline QC CLI handles three fake saved responses'
    Write-SoundJson (Join-Path $run 'TEST-qc-cli.json') $qcResult
    foreach ($id in $script:CandidateIds) {
        $qc = Read-SoundJson (Join-Path $caseDir ($id + '.qc.json'))
        Check ($qc.source_probe.format -ceq 'mp3' -and $qc.source_metrics.encoding -ceq 'pcm_f32le') 'real MP3 decode to IEEE float analysis'
        Check ($qc.preview_metrics.encoding -ceq 'pcm_s16le' -and $qc.preview_metrics.channels -eq 1 -and $qc.preview_metrics.rate_hz -eq 44100 -and [Math]::Abs($qc.preview_metrics.duration_seconds - 3) -lt 0.12) 'real mono/44100 PCM16 preview and duration'
        Check ($qc.ogg_probe.format -ceq 'ogg' -and $qc.ogg_probe.codec -ceq 'vorbis' -and $qc.ogg_metrics.channels -eq 1 -and $qc.ogg_metrics.rate_hz -eq 44100 -and -not $qc.listening_pass) 'real Vorbis encoding and decoded seam screening, never listening approval'
        $report.fixture_results += [ordered]@{ candidate_id = $id; TEST_only_not_art = $true
            source_sha256 = $qc.source_sha256; preview_sha256 = $qc.preview_sha256; ogg_sha256 = $qc.ogg_sha256
            source_flags = $qc.source_metrics.flags; ogg_flags = $qc.ogg_metrics.flags }
    }
    $first = Read-SoundJson (Join-Path $caseDir 'quern-a.qc.json')
    Check ($first.source_metrics.flags -contains 'not_mono' -and $first.source_metrics.flags -contains 'not_44100_hz') 'source stereo/48000 faults are not hidden by conversion'
    Check ((Read-SoundJson (Join-Path $caseDir 'quern-b.qc.json')).source_metrics.flags -contains 'silence_risk') 'real encoded silence detected'
    Check ((Read-SoundJson (Join-Path $caseDir 'quern-c.qc.json')).source_metrics.flags -contains 'loop_boundary_risk') 'real encoded discontinuity detected'
    # Mark audition unmistakably TEST without changing any frozen manifest prompt/file.
    $testInputs = [pscustomobject]@{ Manifest = (Copy-Object $inputs.Manifest) }
    foreach ($candidate in $testInputs.Manifest.calibration.candidates) {
        $candidate.label = 'TEST synthetic codec fixture / NOT ART / ' + $candidate.id
        $candidate.difference = 'Not an ElevenLabs output and not the calibration prompt. Never ship.'
    }
    Write-Audition $testInputs $receipt $caseDir
    Check ([IO.File]::ReadAllText((Join-Path $caseDir 'audition.html')).Contains('TEST synthetic codec fixture')) 'audition evidence cannot be mistaken for art candidates'

    $stage = 'distinct_ambiguous_fake_receipt'
    $ambiguousDir = Join-Path $run 'TEST ambiguous no provider'
    [void][IO.Directory]::CreateDirectory($ambiguousDir)
    $fakeGrant.id = 'test-codec-ambiguous-not-authorization'
    $ambiguousPath = Join-Path $ambiguousDir 'receipt.json'
    $ambiguous = New-Receipt $fakeGrant $inputs ('1' * 64) $ambiguousDir
    Write-SoundJson $ambiguousPath $ambiguous
    $ambiguous = Reserve-Attempt $ambiguousPath $ambiguous $inputs $ambiguousDir 'quern-a'
    Reject { Invoke-AudioTool $hostExe ($common + @('-Mode','QC','-OutputDirectory',$ambiguousDir) + $toolArgs) } 'actual QC CLI fails closed on ambiguous/no saved source'
    Write-SoundJson (Join-Path $run 'TEST-ambiguous-qc-exit.json') $script:LastAudioProcess
    $ambiguous = Read-SoundJson $ambiguousPath
    Check ($ambiguous.attempts[0].state -ceq 'needs_review') 'real QC startup persists terminal ambiguous state without provider use'
    Reject { Assert-CanAttempt $ambiguous } 'ambiguous fake receipt cannot become a new request'
    Check ($script:NetworkCalls -eq 0) 'no Generate/transport invocation in codec suite'
    Check ((Get-SoundHash $ManifestPath) -ceq $inputs.ManifestHash -and (Get-SoundHash $ContractPath) -ceq $inputs.ContractHash) 'frozen prompts/contract unchanged'
    $report.status = 'passed'
} catch {
    $report.status = 'failed_preserved'; $report['failed_stage'] = $stage
    $message = $_.Exception.Message
    if ($message -notmatch '^(TEST failed:|SD_)') { $message = 'Unexpected local codec-test failure; inspect the preserved fixture/diagnostics.' }
    $report['failure'] = $message
    if ($null -ne $script:LastAudioProcess) { Write-SoundJson (Join-Path $run 'TEST-failure-native-diagnostics.json') $script:LastAudioProcess }
} finally {
    $report.total_checks = $script:Checks
    foreach ($file in @(Get-ChildItem -LiteralPath $run -File -Recurse | Where-Object { $_.FullName -cne $reportPath })) {
        $report.artifact_hashes += [ordered]@{ path = $file.FullName.Substring($run.Length + 1).Replace('\','/')
            bytes = $file.Length; sha256 = (Get-SoundHash $file.FullName) }
    }
    Write-SoundJson $reportPath $report
}
Write-Output ($report.status + ': ' + $report.total_checks + ' checks; TEST-only evidence retained: ' + $reportPath)
if ($report.status -cne 'passed') { exit 1 }
