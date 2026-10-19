[CmdletBinding()]
param([switch]$JsonOnly)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'sound-design.ps1')
$script:Checks = 0
$script:NetworkCalls = 0
# Any accidental transport call fails without reading a secret or reaching a provider.
function Send-ProviderAudio { $script:NetworkCalls++; throw 'TEST: network is forbidden.' }
function Check([bool]$Condition, [string]$Label) {
    if (-not $Condition) { throw ('TEST failed: ' + $Label) }
    $script:Checks++
}
function Reject([scriptblock]$Action, [string]$Label, [string]$ExpectedCode) {
    $rejected = $false
    try { $null = & $Action } catch {
        $rejected = $_.Exception.Message -match '^SD_'
        if ($ExpectedCode) { $rejected = $_.Exception.Message.StartsWith($ExpectedCode + ': ') }
    }
    Check $rejected $Label
}
function Copy-Object($Value) { return (ConvertFrom-SoundJson ($Value | ConvertTo-Json -Depth 24)) }

$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('GrindlessSound-TEST-not-art-' + [Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($testRoot)
try {
    $stamp = '2026-01-02T03:04:05.1234567Z'
    $timeFixture = [pscustomobject]@{
        started_utc = $stamp; completed_utc = '2026-01-02T03:04:06Z'
        nested = @($stamp, '2026-01-02T03:04:05+00:00'); count = 3; enabled = $true; absent = $null
    }
    $timePath = Join-Path $testRoot 'TEST-UTC-roundtrip.json'
    Write-SoundJson $timePath $timeFixture
    $timeRead = Read-SoundJson $timePath
    Check ($timeRead.started_utc -is [string] -and $timeRead.started_utc -ceq $stamp -and $timeRead.completed_utc -is [string] -and $timeRead.completed_utc -ceq $timeFixture.completed_utc) 'write/read preserves original UTC string type, precision and Z suffix'
    Check ($timeRead.nested[0] -is [string] -and $timeRead.nested[0] -ceq $stamp -and $timeRead.nested[1] -is [string] -and $timeRead.nested[1] -ceq $timeFixture.nested[1]) 'nested JSON strings are not converted or timezone-normalized'
    $timeClone = Copy-Object $timeFixture
    Check ($timeClone.started_utc -is [string] -and $timeClone.started_utc -ceq $stamp -and $timeClone.completed_utc -is [string] -and $timeClone.completed_utc -ceq $timeFixture.completed_utc) 'test clone shares the string-preserving parser'
    Check ((Test-Number $timeRead.count 3) -and (Test-Bool $timeRead.enabled $true) -and $null -eq $timeRead.absent) 'JSON numbers booleans and null retain their types'
    $inputs = Get-Inputs $ManifestPath $ContractPath
    Assert-Contract $inputs.Contract; Assert-Manifest $inputs.Manifest $inputs.Contract
    Check ($inputs.Manifest.calibration.candidates.Count -eq 3) 'preserved three-prompt manifest'
    foreach ($candidate in $inputs.Manifest.calibration.candidates) {
        $payload = Get-Payload $candidate $inputs.Contract | ConvertFrom-Json
        Assert-Keys $payload @('text','model_id','duration_seconds','loop','prompt_influence')
        Check ($payload.text -ceq $candidate.prompt -and $payload.text.Length -le 450 -and $payload.loop -is [bool] -and $payload.loop) 'exact five-field payload without authentication/seed/batch/logging'
    }
    foreach ($change in @('endpoint','method','duration','loop','influence','model','seed','logging','budget','retry','format','publication','string_duration')) {
        $c = Copy-Object $inputs.Contract
        switch ($change) {
            'endpoint' { $c.endpoint = 'https://example.invalid/redirect' }
            'method' { $c.method = 'GET' }
            'duration' { $c.body_parameters.duration_seconds = 4 }
            'loop' { $c.body_parameters.loop = 'true' }
            'influence' { $c.body_parameters.prompt_influence = 0.4 }
            'model' { $c.body_parameters.model_id = 'another-model' }
            'seed' { $c.body_parameters | Add-Member NoteProperty seed 1 }
            'logging' { $c | Add-Member NoteProperty enable_logging $false }
            'budget' { $c.max_calls = 4 }
            'retry' { $c.automatic_retries = 1 }
            'format' { $c.alternate_formats = $true }
            'publication' { $c.publication_approved = $true }
            'string_duration' { $c.body_parameters.duration_seconds = '3.0' }
        }
        Reject { Assert-Contract $c } ('contract rejects ' + $change)
    }
    $m = Copy-Object $inputs.Manifest; $m.calibration.candidates[0].prompt = 'x' * 451
    Reject { Assert-Manifest $m $inputs.Contract } 'overlong prompt'
    $m = Copy-Object $inputs.Manifest; $m.calibration.candidates[1].id = 'quern-a'
    Reject { Assert-Manifest $m $inputs.Contract } 'duplicate prompt identity'
    $m = Copy-Object $inputs.Manifest; $m.calibration.runtime_import_approved = $true
    Reject { Assert-Manifest $m $inputs.Contract } 'runtime import flag'
    Reject { Get-OutputPath 'relative-folder' } 'explicit absolute output required'
    Reject { Get-OutputPath '\\example.invalid\share\TEST' } 'UNC rejected without access'
    Reject { Get-OutputPath (Join-Path $script:SoundRoot 'docs/TEST-candidates') } 'unignored repository output'
    Check ((Get-OutputPath (Join-Path $script:SoundHome 'local/TEST-candidates')).Length -gt 0) 'existing local ignore is sufficient'
    $output = Get-OutputPath $testRoot
    $grant = Read-SoundJson (Join-Path $PSScriptRoot 'grant.example.json')
    Assert-GrantShape $grant
    $head = Get-GitHead
    Reject { Assert-Grant $grant $inputs $output $head } 'example is not authorization'
    # In-memory shape fixture only. Never write this as an actual grant or invoke Generate.
    $grant.id = 'test-offline-not-authorization'; $grant.generation_approved = $true
    $grant.approved_by = 'Astra'; $grant.authorization_reference = 'TEST schema validation only; no authority'
    $grant.approved_utc = [DateTime]::UtcNow.AddMinutes(-1).ToString('o')
    $grant.expires_utc = [DateTime]::UtcNow.AddMinutes(10).ToString('o')
    $grant.manifest_commit = $head; $grant.contract_commit = $head; $grant.tooling_commit = $head
    $grant.manifest_sha256 = $inputs.ManifestHash; $grant.contract_sha256 = $inputs.ContractHash
    $grant.tooling_sha256 = $inputs.ToolingHash; $grant.output_directory = $output
    $grant.active_entitlement_confirmed = $true; $grant.entitlement_at_generation = 'owner_confirmed_paid_subscription'
    Assert-Grant $grant $inputs $output $head
    Check $true 'grant schema with three commit/hash bindings'
    $grantPath = Join-Path $testRoot 'TEST-grant-roundtrip-not-authorization.json'
    Write-SoundJson $grantPath $grant
    $grantRead = Read-SoundJson $grantPath
    Assert-Grant $grantRead $inputs $output $head
    Check ($grantRead.approved_utc -is [string] -and $grantRead.approved_utc -ceq $grant.approved_utc -and $grantRead.expires_utc -is [string] -and $grantRead.expires_utc -ceq $grant.expires_utc) 'valid grant survives file roundtrip with exact UTC strings'
    foreach ($field in @('manifest_sha256','contract_sha256','tooling_sha256','manifest_commit','contract_commit','tooling_commit','output_directory','expires_utc','active_entitlement_confirmed','public_distribution_approved','candidate_ids')) {
        $g = Copy-Object $grant
        # Prove the clone is valid BEFORE mutation, so date coercion cannot mask a negative.
        Assert-Grant $g $inputs $output $head
        Check ($g.approved_utc -is [string] -and $g.approved_utc -ceq $grant.approved_utc -and $g.expires_utc -is [string] -and $g.expires_utc -ceq $grant.expires_utc) ('valid cloned grant before negative ' + $field)
        switch ($field) {
            'expires_utc' { $g.$field = [DateTime]::UtcNow.AddMinutes(-2).ToString('o') }
            'active_entitlement_confirmed' { $g.$field = $false }
            'public_distribution_approved' { $g.$field = $true }
            'candidate_ids' { $g.$field = @('quern-a','quern-b','quern-a') }
            default { $g.$field = 'TEST-mismatch' }
        }
        $code = 'SD_VALIDATION'; if ($field -eq 'expires_utc') { $code = 'SD_GRANT_TIME' }
        Reject { Assert-Grant $g $inputs $output $head } ('grant rejects ' + $field) $code
    }
    $invalidTimes = @($null, 42, $true, [DateTime]::UtcNow, [DateTimeOffset]::UtcNow,
        'not-a-timestamp', 'not-a-timestampZ', '2026-02-30T00:00:00Z',
        '2026-01-02T03:04:05+00:00', '2026-01-02T03:04:05z', '2026-01-02T03:04:05Zjunk')
    foreach ($field in @('approved_utc','expires_utc')) {
        foreach ($invalid in $invalidTimes) {
            $g = Copy-Object $grant
            Assert-Grant $g $inputs $output $head
            $g.$field = $invalid
            Reject { Assert-Grant $g $inputs $output $head } ('grant rejects non-string/invalid UTC ' + $field) 'SD_GRANT_TIME'
        }
    }
    Reject { Assert-Checkpoint (Join-Path $testRoot 'TEST-manifest.json') $ContractPath } 'generation cannot select alternate inputs'
    Reject { Get-SoundTools (Join-Path $testRoot 'missing/ffmpeg.exe') (Join-Path $testRoot 'missing/ffprobe.exe') $true } 'missing codec preflight fails without installation'

    $receiptPath = Join-Path $output 'TEST-receipt.json'
    $receipt = New-Receipt $grant $inputs ('0' * 64) $output
    Write-SoundJson $receiptPath $receipt
    $receipt = Read-SoundJson $receiptPath; Assert-Receipt $receipt $inputs $output
    $lock = Open-SoundLock $output
    try { Reject { Open-SoundLock $output } 'exclusive budget lock' } finally { $lock.Dispose() }
    $receipt = Reserve-Attempt $receiptPath $receipt $inputs $output 'quern-a'
    Check ((Read-SoundJson $receiptPath).attempts[0].state -ceq 'attempting') 'reservation is durable before any transport'
    $receiptRead = Read-SoundJson $receiptPath
    Assert-Receipt $receiptRead $inputs $output
    $receiptClone = Copy-Object $receiptRead
    Assert-Receipt $receiptClone $inputs $output
    Check ($receiptRead.attempts[0].started_utc -is [string] -and $receiptRead.attempts[0].started_utc -ceq $receipt.attempts[0].started_utc -and $receiptClone.attempts[0].started_utc -is [string] -and $receiptClone.attempts[0].started_utc -ceq $receipt.attempts[0].started_utc) 'reserved receipt file/clone roundtrip preserves UTC string before budget validation'
    foreach ($field in @('started_utc','completed_utc')) {
        foreach ($invalid in $invalidTimes) {
            if ($field -eq 'completed_utc' -and $null -eq $invalid) { continue }
            $bad = Copy-Object $receiptRead
            Assert-Receipt $bad $inputs $output
            $bad.attempts[0].$field = $invalid
            Reject { Assert-Receipt $bad $inputs $output } ('receipt rejects non-string/invalid UTC ' + $field) 'SD_VALIDATION'
        }
    }
    $completed = Copy-Object $receiptRead
    $completed.attempts[0].completed_utc = $completed.attempts[0].started_utc
    Write-SoundJson (Join-Path $testRoot 'TEST-completed-roundtrip.json') $completed
    $completedRead = Read-SoundJson (Join-Path $testRoot 'TEST-completed-roundtrip.json')
    Assert-Receipt $completedRead $inputs $output
    Check ($completedRead.attempts[0].completed_utc -is [string] -and $completedRead.attempts[0].completed_utc -ceq $completed.attempts[0].completed_utc) 'non-null completion remains an exact UTC string after write/read'
    Reject { Reserve-Attempt $receiptPath $receipt $inputs $output 'quern-a' } 'in-flight/duplicate cannot resend'
    Resolve-Interrupted $receiptPath $receipt
    $receipt = Read-SoundJson $receiptPath
    Check ($receipt.attempts[0].state -ceq 'needs_review' -and $receipt.attempts[0].failure_code -ceq 'interrupted_delivery_unknown') 'crash converts to terminal review state'
    Reject { Assert-CanAttempt $receipt } 'ambiguous result blocks even a different candidate'
    Resolve-Interrupted $receiptPath $receipt
    Check ((Read-SoundJson $receiptPath).attempts[0].state -ceq 'needs_review') 'review state is not cleared by restart'
    $bad = Copy-Object $receipt; $bad.contract_sha256 = 'f' * 64
    Reject { Assert-Receipt $bad $inputs $output } 'altered receipt binding'
    $bad = Copy-Object $receipt; $bad.attempts[0].state = 'retryable'
    Reject { Assert-Receipt $bad $inputs $output } 'invented retry state'
    $http = Copy-Object $receipt; $http.attempts[0].state = 'http_error'; $http.attempts[0].http_status = 429
    Assert-Receipt $http $inputs $output
    Reject { Assert-CanAttempt $http } 'HTTP/rate-limit errors cannot auto-retry or continue'
    $stale = Copy-Object $receipt
    $receipt.attempts[0].failure_code = 'TEST changed on disk'
    Write-SoundJson $receiptPath $receipt
    Reject { Reserve-Attempt $receiptPath $stale $inputs $output 'quern-b' } 'reread refuses to overwrite changed spend evidence'

    # TEST fake MP3s are unmistakable text, not decodable art candidates. Deleted in finally.
    # A separate scenario directory, never a reset of the interrupted receipt above.
    $output = Join-Path $testRoot 'TEST-three-successes-not-art'
    [void][IO.Directory]::CreateDirectory($output)
    $receiptPath = Join-Path $output 'TEST-receipt.json'
    $receipt = New-Receipt $grant $inputs ('0' * 64) $output
    Write-SoundJson $receiptPath $receipt
    foreach ($id in $script:CandidateIds) {
        $receipt = Reserve-Attempt $receiptPath $receipt $inputs $output $id
        $a = $receipt.attempts[-1]; $a.raw_file = $id + '.raw.mp3'
        [IO.File]::WriteAllBytes((Join-Path $output $a.raw_file), [Text.Encoding]::ASCII.GetBytes('TEST FAKE MP3 - NOT AUDIO - NEVER SHIP'))
        $a.source_sha256 = Get-SoundHash (Join-Path $output $a.raw_file)
        $a.http_status = 200; $a.completed_utc = [DateTime]::UtcNow.ToString('o'); $a.state = 'response_saved'
        Write-SoundJson $receiptPath $receipt
    }
    Assert-Receipt $receipt $inputs $output
    Reject { Assert-CanAttempt $receipt } 'three successful attempts exhaust durable budget'
    Reject { Reserve-Attempt $receiptPath $receipt $inputs $output 'quern-a' } 'no fourth/duplicate attempt'
    $bad = Copy-Object $receipt; $bad.attempts = @($bad.attempts) + $bad.attempts[0]
    Reject { Assert-Receipt $bad $inputs $output } 'four-entry receipt rejected'
    [IO.File]::WriteAllBytes((Join-Path $output 'quern-a.raw.mp3'), [byte[]]@(0))
    Reject { Assert-Receipt $receipt $inputs $output } 'missing/altered source cannot cause resend'
    Reject { Measure-Wave (Join-Path $output 'quern-b.raw.mp3') } 'fake MP3 is not mistaken for WAV'
    [IO.File]::WriteAllText($receiptPath, '{ TEST corrupt receipt')
    Reject { Read-SoundJson $receiptPath } 'corrupt receipt fails closed'

    if ($JsonOnly) {
        Check ($script:NetworkCalls -eq 0) 'focused JSON/receipt guards never invoke transport'
        Write-Output ('PASS: ' + $script:Checks + ' focused JSON/contract/receipt guards; no audio created, provider or secret access. Default CI still runs the complete suite.')
        return
    }
    Add-Type -TypeDefinition @'
using System;
using System.IO;
public static class GrindlessTestWave {
    public static void Write(string path, int channels, int rate, double seconds, string kind) {
        int frames = (int)(rate * seconds), size = frames * channels * 2;
        using (var w = new BinaryWriter(File.Create(path))) {
            w.Write(System.Text.Encoding.ASCII.GetBytes("RIFF")); w.Write(36 + size);
            w.Write(System.Text.Encoding.ASCII.GetBytes("WAVEfmt ")); w.Write(16);
            w.Write((short)1); w.Write((short)channels); w.Write(rate);
            w.Write(rate * channels * 2); w.Write((short)(channels * 2)); w.Write((short)16);
            w.Write(System.Text.Encoding.ASCII.GetBytes("data")); w.Write(size);
            for (int i = 0; i < frames; i++) {
                double value = 0.2 * Math.Sin(2 * Math.PI * 220 * i / rate);
                if (kind == "silence") value = 0;
                if (kind == "clip") value = 1;
                if (kind == "seam" && i > frames - rate / 100) value = 0.7;
                if (kind == "gap" && i > rate && i < rate * 1.5) value = 0;
                for (int c = 0; c < channels; c++) w.Write((short)(value * 32767));
            }
        }
    }
}
'@
    $wave = Join-Path $output 'TEST-synthetic-fixture-never-ship.wav'
    [GrindlessTestWave]::Write($wave, 1, 44100, 3, 'sine')
    $qc = Measure-Wave $wave
    Check ($qc.format -ceq 'wav' -and $qc.encoding -ceq 'pcm_s16le' -and $qc.bits_per_sample -eq 16 -and $qc.flags.Count -eq 0 -and -not $qc.listening_pass -and -not $qc.loop_boundary.listening_pass) 'technical clean PCM is never a listening pass'
    [GrindlessTestWave]::Write($wave, 2, 32000, 1, 'sine')
    $qc = Measure-Wave $wave
    Check ($qc.flags -contains 'not_mono' -and $qc.flags -contains 'not_44100_hz' -and $qc.flags -contains 'duration_outside_3s_tolerance') 'channel/rate/duration detected before transformation'
    foreach ($pair in @(@('clip','clipping_or_near_full_scale'),@('silence','silence_risk'),@('seam','loop_boundary_risk'),@('gap','silence_risk'))) {
        [GrindlessTestWave]::Write($wave, 1, 44100, 3, $pair[0])
        Check ((Measure-Wave $wave).flags -contains $pair[1]) ('PCM detects ' + $pair[0])
    }
    [IO.File]::WriteAllBytes($wave, [byte[]]@(82,73,70,70,0,0))
    Reject { Measure-Wave $wave } 'truncated WAV rejected'
    $receipt = New-Receipt $grant $inputs ('0' * 64) $output
    $m = Copy-Object $inputs.Manifest; $m.calibration.candidates[0].label = '<TEST & label>'
    Write-Audition ([pscustomobject]@{ Manifest = $m }) $receipt $output
    $html = [IO.File]::ReadAllText((Join-Path $output 'audition.html'))
    Check ($html.Contains('&lt;TEST &amp; label&gt;') -and -not $html.Contains('<script') -and -not $html.Contains('https://') -and -not $html.Contains('<audio')) 'offline escaped audition does not invent missing candidates'
    $receipt.attempts = @([pscustomobject]@{ candidate_id = 'quern-a'; state = 'response_saved' })
    Write-SoundJson (Join-Path $output 'quern-a.qc.json') ([ordered]@{
        TEST_fixture_not_art = $true; source_metrics = @{ flags = @('loop_boundary_risk') }
        preview_metrics = @{ flags = @() }; ogg_metrics = @{ flags = @() } })
    Write-Audition ([pscustomobject]@{ Manifest = $m }) $receipt $output
    $html = [IO.File]::ReadAllText((Join-Path $output 'audition.html'))
    Check ($html.Contains('<audio controls loop') -and $html.Contains('loop_boundary_risk') -and $html.Contains('not a listening verdict') -and -not $html.Contains('<script')) 'local audio controls expose seam screening without a listening pass'
    Add-Type -AssemblyName System.Net.Http
    $handler = [Net.Http.HttpClientHandler]::new()
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Post, $script:Endpoint)
    try {
        # Construct local .NET objects only: no client, key, SendAsync, TLS handshake or HTTP.
        $handler.AllowAutoRedirect = $false; $handler.CheckCertificateRevocationList = $true
        $handler.UseDefaultCredentials = $false; $handler.UseProxy = $false; $handler.UseCookies = $false
        $request.Headers.ConnectionClose = $true; $request.Headers.ExpectContinue = $false
        Check (-not $handler.AllowAutoRedirect -and $handler.CheckCertificateRevocationList -and -not $handler.UseProxy -and $request.Headers.ConnectionClose) 'native HTTP safety primitives exist on WinPS5/.NET'
    } finally { $request.Dispose(); $handler.Dispose() }
    $source = [IO.File]::ReadAllText($script:SoundScript)
    Check ([regex]::Matches($source, "'-protocol_whitelist','file'").Count -eq 5) 'every media probe/decode/transform restricts FFmpeg to local file protocol'
    Check ($script:NetworkCalls -eq 0) 'zero transport calls and no secret invocation'
    & (Join-Path $PSScriptRoot 'test-generation-flow.ps1')
    Write-Output ('PASS: ' + $script:Checks + ' offline checks. TEST fixtures removed; no provider, secret, JVM or package install used.')
} finally {
    # Delete only this invocation's unique TEST directory, never candidate receipts.
    if ([IO.Directory]::Exists($testRoot)) { Remove-Item -LiteralPath $testRoot -Recurse -Force }
}
