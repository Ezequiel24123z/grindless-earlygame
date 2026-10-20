[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'sound-design.ps1')
$script:PurposeChecks = 0
$script:SecretGateCalls = 0
$script:PurposeTransportCalls = 0
$purposeManifest = Join-Path $PSScriptRoot 't0-wet-work-purpose.json'
$purposeContract = Join-Path $PSScriptRoot 'purpose-contract.json'
$purposeExample = Join-Path $PSScriptRoot 'purpose-grant.example.json'
$script:PurposeInputs = Get-Inputs $purposeManifest $purposeContract
$script:PurposeHead = Get-GitHead

function Check-Purpose([bool]$Condition, [string]$Label) {
    if (-not $Condition) { throw ('TEST purpose failed: ' + $Label) }
    $script:PurposeChecks++
}
function Reject-Purpose([scriptblock]$Action, [string]$Code, [string]$Fragment) {
    $script:LastPurposeRejection = $null
    try { $null = & $Action } catch { $script:LastPurposeRejection = $_.Exception.Message }
    Check-Purpose ($null -ne $script:LastPurposeRejection -and
        $script:LastPurposeRejection.StartsWith($Code + ': ') -and
        $script:LastPurposeRejection.Contains($Fragment)) ('intended diagnostic: ' + $Fragment)
}
function Copy-Purpose($Value) { return (ConvertFrom-SoundJson ($Value | ConvertTo-Json -Depth 24)) }
function Get-FixtureHash($Value) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($Value | ConvertTo-Json -Depth 24 -Compress))))).Replace('-','').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
function Assert-LegacyImmutable([string]$Path, $Pin) {
    # Test-only whole-file pairs. Never normalize bytes or change authorization hashing.
    Require-Sound (@($Pin.Lf, $Pin.Crlf) -ccontains (Get-SoundHash $Path)) 'Frozen legacy input must match its exact whole-file LF or CRLF hash.'
}
function Read-LegacyGitBytes([string]$Blob) {
    # These frozen blobs are still referenced by HEAD, including in shallow CI.
    # BaseStream is essential: a PowerShell text pipeline would alter the proof.
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = (Get-Command git -CommandType Application | Select-Object -First 1).Source
    $start.Arguments = 'cat-file blob ' + $Blob; $start.WorkingDirectory = $script:SoundRoot
    $start.UseShellExecute = $false; $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $start
    $bytes = [IO.MemoryStream]::new()
    try {
        [void]$process.Start(); $stderr = $process.StandardError.ReadToEndAsync()
        $process.StandardOutput.BaseStream.CopyTo($bytes); $process.WaitForExit()
        Require-Sound ($process.ExitCode -eq 0) 'Cannot read the frozen public Git blob.'
        return ,($bytes.ToArray())
    } finally { $bytes.Dispose(); $process.Dispose() }
}
function Test-LegacyImmutableBytes($Pin) {
    $actual = Join-Path $script:SoundRoot $Pin.Path
    Assert-LegacyImmutable $actual $Pin
    Check-Purpose $true ('real frozen checkout matches an exact LF/CRLF pin: ' + $Pin.Path)
    $raw = Read-LegacyGitBytes $Pin.Blob
    $utf8 = [Text.UTF8Encoding]::new($false, $true)
    $text = $utf8.GetString($raw)
    $lf = Join-Path $script:PurposeRoot ('TEST-LF-' + [IO.Path]::GetFileName($Pin.Path))
    $crlf = Join-Path $script:PurposeRoot ('TEST-CRLF-' + [IO.Path]::GetFileName($Pin.Path))
    [IO.File]::WriteAllBytes($lf, $raw) # Actual raw Git bytes, not reserialized JSON.
    Assert-LegacyImmutable $lf $Pin
    Check-Purpose ($raw.Length -eq $Pin.Size -and ($text.Split([char]10).Length - 1) -eq $Pin.Lines -and
        $text.IndexOf([char]13) -eq -1 -and $text[0] -ne [char]0xfeff) 'canonical Git fixture has the measured size/LF count, no CR and no BOM'
    # Conversion is fixture construction only, from the verified CR-free Git blob.
    $windows = $utf8.GetBytes($text.Replace("`n", "`r`n"))
    [IO.File]::WriteAllBytes($crlf, $windows)
    Assert-LegacyImmutable $crlf $Pin
    Check-Purpose ((Get-SoundHash $lf) -ceq $Pin.Lf -and (Get-SoundHash $crlf) -ceq $Pin.Crlf -and
        $Pin.Lf -cne $Pin.Crlf -and $windows.Length -eq $Pin.Size + $Pin.Lines) 'raw LF reproduces the old CRLF-only mismatch; Get-SoundHash still distinguishes both exact byte forms'

    $firstLf = $text.IndexOf("`n")
    $mutations = @(
        @{ Name = 'budget'; Bytes = $utf8.GetBytes($text.Replace('"max_calls": 3', '"max_calls": 4')) },
        @{ Name = 'whitespace'; Bytes = $utf8.GetBytes($text + ' ') },
        @{ Name = 'mixed-lines'; Bytes = $utf8.GetBytes($text.Insert($firstLf, "`r")) },
        @{ Name = 'stray-CR'; Bytes = [byte[]](@(13) + $raw) },
        @{ Name = 'BOM'; Bytes = [byte[]](@(239,187,191) + $raw) }
    )
    if ($Pin.Path -ceq 'docs/assets/t0-sound-design.json') {
        $mutations += @{ Name = 'prompt'; Bytes = $utf8.GetBytes($text.Replace('Dry, close granular crunch', 'Dry, close granular scrape')) }
    }
    if ($Pin.Path -ceq 'tools/sound-design/request-contract.json') {
        $mutations += @{ Name = 'parameter'; Bytes = $utf8.GetBytes($text.Replace('"duration_seconds": 3.0', '"duration_seconds": 3.1')) }
    }
    $path = Join-Path $script:PurposeRoot ('TEST-mutation-' + [IO.Path]::GetFileName($Pin.Path))
    foreach ($mutation in $mutations) {
        [IO.File]::WriteAllBytes($path, $raw)
        Assert-LegacyImmutable $path $Pin
        Check-Purpose ((Get-SoundHash $path) -ceq $Pin.Lf -and
            [Convert]::ToBase64String($raw) -cne [Convert]::ToBase64String($mutation.Bytes)) ('valid raw LF baseline before ' + $mutation.Name)
        [IO.File]::WriteAllBytes($path, $mutation.Bytes)
        Reject-Purpose { Assert-LegacyImmutable $path $Pin } 'SD_VALIDATION' 'Frozen legacy input must match its exact whole-file LF or CRLF hash.'
    }
}
function New-FixtureInputs($Manifest) {
    return [pscustomobject]@{ Manifest = $Manifest; Contract = $script:PurposeInputs.Contract
        ManifestHash = (Get-FixtureHash $Manifest); ContractHash = $script:PurposeInputs.ContractHash
        ToolingHash = $script:PurposeInputs.ToolingHash }
}
function New-BindingFixture($Inputs, [string]$Output) {
    # Remains explicitly UNAPPROVED, with no Sol signature or approval timestamps.
    # Only the pure validator may accept its bindings; it is never generation authority.
    $g = Read-SoundJson $purposeExample
    $g.id = 'test-bindings-not-authorization'; $g.reviewed_commit = $script:PurposeHead
    $g.manifest_sha256 = $Inputs.ManifestHash; $g.contract_sha256 = $Inputs.ContractHash
    $g.tooling_sha256 = $Inputs.ToolingHash; $g.output_directory = $Output
    $g.purpose_id = $Inputs.Manifest.purpose_id; $g.run_id = $Inputs.Manifest.run_id
    $g.family_ids = @($Inputs.Manifest.families | ForEach-Object { $_.id })
    $g.candidate_ids = @(Get-Candidates $Inputs | ForEach-Object { $_.id }); $g.max_calls = Get-CallLimit $Inputs
    return $g
}
function New-TestLedger([string]$Name, $Inputs) {
    $output = Join-Path $script:PurposeRoot ('TEST-' + $Name)
    [void][IO.Directory]::CreateDirectory($output)
    $g = New-BindingFixture $Inputs $output
    Assert-PurposeBindings $g $Inputs $output $script:PurposeHead
    $receipt = New-Receipt $g $Inputs ('0' * 64) $output
    $path = Join-Path $output 'receipt.json'
    Write-SoundJson $path $receipt
    return [pscustomobject]@{ Output = $output; Path = $path; Grant = $g; Receipt = (Read-SoundJson $path) }
}
function Complete-TestLedger($Case, $Inputs) {
    foreach ($candidate in @(Get-Candidates $Inputs)) {
        $Case.Receipt = Reserve-Attempt $Case.Path $Case.Receipt $Inputs $Case.Output $candidate.id
        Check-Purpose ((Read-SoundJson $Case.Path).attempts[-1].state -ceq 'attempting') 'actual reservation is on disk before a simulated response'
        $attempt = $Case.Receipt.attempts[-1]; $attempt.raw_file = $candidate.id + '.raw.mp3'
        [IO.File]::WriteAllText((Join-Path $Case.Output $attempt.raw_file), 'TEST FAKE MP3 - NOT AUDIO - NEVER SHIP')
        $attempt.source_sha256 = Get-SoundHash (Join-Path $Case.Output $attempt.raw_file)
        $attempt.state = 'response_saved'; $attempt.http_status = 200
        $attempt.completed_utc = [DateTime]::UtcNow.ToString('o')
        Write-SoundJson $Case.Path $Case.Receipt
    }
    Assert-Receipt (Read-SoundJson $Case.Path) $Inputs $Case.Output
}
function Invoke-PurposeCli([string[]]$Arguments, [string]$Label) {
    # The current test host only; no executable search, codec or provider process.
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = (Get-Process -Id $PID).Path
    $start.UseShellExecute = $false; $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    $all = @('-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$script:SoundScript) + $Arguments
    if ($start.PSObject.Properties.Name -contains 'ArgumentList') {
        foreach ($arg in $all) { [void]$start.ArgumentList.Add($arg) }
    } else {
        $start.Arguments = (@($all | ForEach-Object { '"' + [regex]::Replace([regex]::Replace($_, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"' }) -join ' ')
    }
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $start
    try {
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync(); $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(30000)) { Stop-Process -Id $process.Id -Force; throw ('TEST CLI timed out: ' + $Label) }
        return [pscustomobject]@{ ExitCode = $process.ExitCode; Stdout = $stdout.GetAwaiter().GetResult(); Stderr = $stderr.GetAwaiter().GetResult() }
    } finally { $process.Dispose() }
}
function Invoke-CheckpointFixture([bool]$Tracked, [bool]$Clean) {
    # Exercise the real path/Git/checkpoint guard without a commit or an approved
    # grant. Substitute only Git observations, in this invocation's local scope.
    $script:CheckpointGitCalls = 0
    function git {
        $script:CheckpointGitCalls++
        if ($args -ccontains 'ls-files') {
            $global:LASTEXITCODE = 1; if ($Tracked) { $global:LASTEXITCODE = 0 }
            return
        }
        if ($args -ccontains 'status') {
            $global:LASTEXITCODE = 0
            if (-not $Clean) { ' M tools/sound-design/TEST-dirty-input.json' }
            return
        }
        throw 'TEST: unexpected Git operation in checkpoint fixture.'
    }
    Assert-Checkpoint $purposeManifest $purposeContract
}
# Tripwires for the real Invoke-Generate negative cases, not a substitute approval.
function Get-SecretPath { $script:SecretGateCalls++; throw 'TEST: secret-path inspection forbidden.' }
function Send-ProviderAudio { $script:PurposeTransportCalls++; throw 'TEST: transport forbidden.' }

$script:PurposeRoot = Join-Path ([IO.Path]::GetTempPath()) ('GrindlessPurpose-TEST-not-art-' + [Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($script:PurposeRoot)
try {
    $inputs = $script:PurposeInputs; $head = $script:PurposeHead
    Check-Purpose ($inputs.Contract.review_status -ceq 'reviewed' -and $inputs.Manifest.review_status -ceq 'reviewed') 'real contract and manifest retain their legitimate reviewed status'
    Check-Purpose ($inputs.Manifest.source_revision -ceq '269965b1b0b8c1becda74459a853f245105649ee') 'proposal retains exact runtime audit source'
    Check-Purpose ($inputs.Manifest.families.Count -eq 1 -and $inputs.Manifest.families[0].id -ceq 'wet_work' -and
        (Test-OrderedIds $inputs.Manifest.families[0].machines @('clay_mixer')) -and (Get-CallLimit $inputs) -eq 2) 'actual scope is Clay Mixer only, with two calls'
    # Exact LF blobs from 7dad69df7f5f7cba2eaf83fb1db1a50c0ad6338c,
    # byte-verified equal to HEAD's blobs. Keep the historical Windows CRLF pins.
    $legacyPins = @(
        [pscustomobject]@{ Path = 'docs/assets/t0-sound-design.json'; Blob = 'cfdc8c1a183b7eed587ddd0005d1f9cb3ddd5c18'
            Lf = '4a716239ac2fd5fcfb307e54a619f7c17875f20f25e5ea37fced689f651d7f60'
            Crlf = 'c39e76c88e4755d51b3d3ce76c4d819cdf985c6d5977fd614ce3115dc155732e'; Size = 14568; Lines = 179 },
        [pscustomobject]@{ Path = 'tools/sound-design/request-contract.json'; Blob = 'b175242ca04b540cbb7f0ee78882ae5d4a174528'
            Lf = 'ac65888bc2ed2065941fbc5b32997d853b6355f6dfe7de1aae0c0c1c2c4ac0d6'
            Crlf = '2a986693cb3a919a573da5de0739c0486dde3c2d462004a7d64eae3607829a43'; Size = 1312; Lines = 29 },
        [pscustomobject]@{ Path = 'tools/sound-design/grant.example.json'; Blob = 'fb1aee404031dfff2f30eeaa4b2d625f87f7aaf0'
            Lf = '384313eccc7886358612fc38c06d0ced59af9e94713eb2de0bf632c0cfb2fe0e'
            Crlf = 'f84bd409fa3c9d6a36b7abf8a091ffceb1197c7a564ce7c52363841d08be3f90'; Size = 658; Lines = 22 }
    )
    foreach ($pin in $legacyPins) { Test-LegacyImmutableBytes $pin }
    $output = Join-Path $script:PurposeRoot 'TEST-must-not-be-created'
    $g = New-BindingFixture $inputs $output
    Assert-PurposeBindings $g $inputs $output $head
    Check-Purpose (-not $g.generation_approved -and $null -eq $g.approved_by -and $null -eq $g.approved_utc -and $null -eq $g.expires_utc) 'positive binding fixture makes no authority claim'
    Reject-Purpose { Assert-Grant $g $inputs $output $head } 'SD_PURPOSE_UNAPPROVED' 'Unapproved generation markers:'
    $grantMarkers = @('grant.generation_approved','grant.approved_by')
    Check-Purpose ((Test-OrderedIds @(Get-PurposeApprovalFailures $g $inputs) $grantMarkers) -and
        $script:LastPurposeRejection.Contains($grantMarkers[0]) -and $script:LastPurposeRejection.Contains($grantMarkers[1]) -and
        -not $script:LastPurposeRejection.Contains('contract.review_status') -and
        -not $script:LastPurposeRejection.Contains('manifest.review_status')) 'reviewed real inputs fail only the false/null example grant markers'
    # Negative metadata is explicit in-memory TEST data, never a change to the
    # reviewed files or a forged Sol approval. Each clone keeps the grant false/null.
    foreach ($section in @('Contract','Manifest')) {
        $unapproved = Copy-Purpose $inputs; $unapproved.$section.review_status = 'unapproved'
        $unapproved.ManifestHash = Get-FixtureHash $unapproved.Manifest
        $unapproved.ContractHash = Get-FixtureHash $unapproved.Contract
        $unapprovedGrant = New-BindingFixture $unapproved $output
        Assert-PurposeBindings $unapprovedGrant $unapproved $output $head
        $marker = $section.ToLowerInvariant() + '.review_status'
        Reject-Purpose { Assert-Grant $unapprovedGrant $unapproved $output $head } 'SD_PURPOSE_UNAPPROVED' $marker
        Check-Purpose ((Test-OrderedIds @(Get-PurposeApprovalFailures $unapprovedGrant $unapproved) (@($marker) + $grantMarkers)) -and
            $script:LastPurposeRejection.Contains($grantMarkers[0]) -and $script:LastPurposeRejection.Contains($grantMarkers[1])) ('real guard rejects only the unapproved ' + $section + ' clone and example grant markers')
    }
    Reject-Purpose { Invoke-Generate $inputs $purposeManifest $purposeContract $purposeExample $output 'TEST-NOT-A-SECRET-PATH' $null } 'SD_PURPOSE_UNAPPROVED' 'Unapproved generation markers:'
    $legacyInputs = Get-Inputs $ManifestPath $ContractPath
    $legacyExample = Join-Path $PSScriptRoot 'grant.example.json'
    Reject-Purpose { Assert-Grant (Read-SoundJson $legacyExample) $inputs $output $head } 'SD_VALIDATION' 'legacy grants cannot authorize a new purpose'
    Reject-Purpose { Invoke-Generate $inputs $purposeManifest $purposeContract $legacyExample $output 'TEST-NOT-A-SECRET-PATH' $null } 'SD_VALIDATION' 'legacy grants cannot authorize a new purpose'
    Reject-Purpose { Invoke-Generate $legacyInputs $ManifestPath $ContractPath $legacyExample $output 'TEST-NOT-A-SECRET-PATH' $null } 'SD_VALIDATION' 'A NEW explicit Astra grant is required.'
    Check-Purpose ($script:SecretGateCalls -eq 0 -and $script:PurposeTransportCalls -eq 0 -and -not [IO.Directory]::Exists($output)) 'both real unapproved Generate paths stop before secret inspection, transport or output creation'

    $cliArgs = @('-ManifestPath',$purposeManifest,'-ContractPath',$purposeContract,'-GrantFile',$purposeExample,
        '-OutputDirectory',$output,'-FfmpegPath',(Join-Path $script:PurposeRoot 'missing/ffmpeg.exe'),'-FfprobePath',(Join-Path $script:PurposeRoot 'missing/ffprobe.exe'))
    $cli = Invoke-PurposeCli (@('-Mode','Plan') + $cliArgs) 'explicit v2 Plan'
    Check-Purpose ($cli.ExitCode -eq 0 -and $cli.Stderr.Length -eq 0) 'actual explicit-path Plan CLI exits zero'
    $plan = ConvertFrom-SoundJson $cli.Stdout
    Check-Purpose ($plan.purpose_id -ceq $inputs.Manifest.purpose_id -and $plan.run_id -ceq $inputs.Manifest.run_id -and
        $plan.max_posts -eq 2 -and $plan.wave_ceiling_not_granted.max_calls -eq 4 -and $plan.requests.Count -eq 2 -and
        ($plan.requests.id -join ',') -ceq 'wet-work-a,wet-work-b') 'Plan shows selected two, not unused four, with purpose and candidate identities'
    Check-Purpose (-not $plan.generation_authorized -and -not $plan.network -and -not $plan.secret_read -and
        -not [IO.Directory]::Exists($output) -and -not [IO.File]::Exists((Join-Path $output 'receipt.json'))) 'Plan is read-only and never authorization'
    foreach ($request in $plan.requests) {
        Assert-Keys $request.body @('text','model_id','duration_seconds','loop','prompt_influence')
        Check-Purpose ($request.endpoint -ceq $script:Endpoint -and $request.method -ceq 'POST' -and
            $request.body.model_id -ceq 'eleven_text_to_sound_v2' -and $request.body.duration_seconds -eq 3 -and
            $request.body.loop -is [bool] -and $request.body.loop -and $request.body.prompt_influence -eq 0.3 -and
            $request.body.text.Length -le 450) 'Plan emits only the exact reviewed payload'
    }
    $cli = Invoke-PurposeCli (@('-Mode','Generate','-SecretFile','TEST-NOT-A-SECRET-PATH') + $cliArgs) 'unapproved v2 Generate'
    Check-Purpose ($cli.ExitCode -eq 1 -and $cli.Stdout.Length -eq 0 -and $cli.Stderr.StartsWith('SD_PURPOSE_UNAPPROVED: ') -and
        $cli.Stderr.Contains($grantMarkers[0]) -and $cli.Stderr.Contains($grantMarkers[1]) -and
        -not $cli.Stderr.Contains('contract.review_status') -and -not $cli.Stderr.Contains('manifest.review_status') -and
        -not $cli.Stderr.Contains('TEST-NOT-A-SECRET-PATH') -and -not [IO.Directory]::Exists($output)) 'actual unapproved Generate CLI fails the real guard before missing tools or secret path, with safe diagnostics'
    # Default legacy manifest/contract, but no PATH/tool discovery in this focused
    # offline harness. Missing explicit paths cannot start or discover a codec.
    $legacyCli = Invoke-PurposeCli @('-Mode','Plan','-FfmpegPath',(Join-Path $script:PurposeRoot 'missing/ffmpeg.exe'),
        '-FfprobePath',(Join-Path $script:PurposeRoot 'missing/ffprobe.exe')) 'default legacy Plan with explicit absent codecs'
    Check-Purpose ($legacyCli.ExitCode -eq 0 -and $legacyCli.Stderr.Length -eq 0) 'default legacy Plan CLI still exits zero'
    $legacyPlan = ConvertFrom-SoundJson $legacyCli.Stdout
    Check-Purpose ($legacyPlan.max_posts -eq 3 -and $legacyPlan.requests.Count -eq 3 -and
        ($legacyPlan.requests.id -join ',') -ceq 'quern-a,quern-b,quern-c' -and -not $legacyPlan.generation_authorized) 'default legacy Plan keeps Quern/three'

    foreach ($change in @('endpoint','model','duration','loop','influence','seed','logging','text','retry','format','families','per_family','global','schema','unknown')) {
        $c = Copy-Purpose $inputs.Contract; $c.review_status = 'unapproved'; Assert-Contract $c
        $fragment = 'Unexpected'; $code = 'SD_VALIDATION'
        switch ($change) {
            'endpoint' { $c.endpoint += '&seed=1'; $fragment = 'exact official HTTPS POST' }
            'model' { $c.body_parameters.model_id = 'other'; $fragment = 'Unexpected provider parameters' }
            'duration' { $c.body_parameters.duration_seconds = '3.0'; $fragment = 'Unexpected provider parameters' }
            'loop' { $c.body_parameters.loop = 'true'; $fragment = 'Unexpected provider parameters' }
            'influence' { $c.body_parameters.prompt_influence = 0.4; $fragment = 'Unexpected provider parameters' }
            'seed' { $c.body_parameters | Add-Member NoteProperty seed 1 }
            'logging' { $c | Add-Member NoteProperty enable_logging $false }
            'text' { $c.text_max_characters = 451 }
            'retry' { $c.automatic_retries = 1 }
            'format' { $c.alternate_formats = $true }
            'families' { $c.wave_limits.max_families = 3; $fragment = 'ADR-0156 ceilings' }
            'per_family' { $c.wave_limits.max_candidates_per_family = 3; $fragment = 'ADR-0156 ceilings' }
            'global' { $c.wave_limits.max_calls = 5; $fragment = 'ADR-0156 ceilings' }
            'schema' { $c.schema = 'grindless.sound-purpose-contract/v3'; $fragment = 'Wrong contract schema.' }
            'unknown' { $c | Add-Member NoteProperty fallback_endpoint 'https://example.invalid' }
        }
        Reject-Purpose { Assert-Contract $c } $code $fragment
    }
    $two = Copy-Purpose $inputs.Manifest; $two.review_status = 'unapproved'
    $family = Copy-Purpose $two.families[0]; $family.id = 'test_family_two'; $family.machines = @('test_machine_two')
    $family.candidates[0].id = 'test-family-two-a'; $family.candidates[1].id = 'test-family-two-b'
    $two.families = @($two.families) + $family; $two.max_calls = 4
    $two.purpose_id = 'test-two-family-not-approved'; $two.run_id = 'test-two-family-finite-run'
    Assert-Manifest $two $inputs.Contract
    $twoInputs = New-FixtureInputs $two
    $twoGrant = New-BindingFixture $twoInputs $output
    Assert-PurposeBindings $twoGrant $twoInputs $output $head
    Check-Purpose ($twoGrant.max_calls -eq 4 -and $twoGrant.family_ids.Count -eq 2 -and -not $twoGrant.generation_approved) 'two-family/four fixture is finite and remains unapproved'
    foreach ($change in @('three_families','three_per_family','unused_four','duplicate_candidate','duplicate_family','duplicate_machine','long_prompt','unknown_candidate','unknown_manifest','schema')) {
        $m = Copy-Purpose $inputs.Manifest; $m.review_status = 'unapproved'; Assert-Manifest $m $inputs.Contract
        $fragment = 'Unexpected or missing object fields.'
        switch ($change) {
            'three_families' {
                $m = Copy-Purpose $two; $third = Copy-Purpose $family
                $third.id = 'test_family_three'; $third.machines = @('test_machine_three')
                $third.candidates[0].id = 'test-family-three-a'; $m.families += $third
                foreach ($f in $m.families) { $f.candidates = @($f.candidates[0]) }
                $m.max_calls = 3; $fragment = 'Select one or two families'
            }
            'three_per_family' { $third = Copy-Purpose $m.families[0].candidates[0]; $third.id = 'test-third-candidate'; $m.families[0].candidates += $third; $m.max_calls = 3; $fragment = 'Select one or two candidates per family' }
            'unused_four' { $m.max_calls = 4; $fragment = 'Effective max_calls must equal' }
            'duplicate_candidate' { $m = Copy-Purpose $two; $m.families[1].candidates[0].id = 'wet-work-a'; $fragment = 'Invalid or duplicate candidate ID.' }
            'duplicate_family' { $m = Copy-Purpose $two; $m.families[1].id = 'wet_work'; $fragment = 'Invalid or duplicate family ID.' }
            'duplicate_machine' { $m.families[0].machines = @('clay_mixer','clay_mixer'); $fragment = 'Invalid or duplicate machine ID.' }
            'long_prompt' { $m.families[0].candidates[0].prompt = 'x' * 451; $fragment = 'prompt 1..450 characters' }
            'unknown_candidate' { $m.families[0].candidates[0] | Add-Member NoteProperty seed 1 }
            'unknown_manifest' { $m | Add-Member NoteProperty per_family_output_directory 'TEST-forbidden' }
            'schema' { $m.schema = 'grindless.sound-purpose-manifest/v3'; $fragment = 'Wrong manifest schema.' }
        }
        Reject-Purpose { Assert-Manifest $m $inputs.Contract } 'SD_VALIDATION' $fragment
    }
    foreach ($change in @('purpose_id','run_id','families','candidates','duplicate','reordered','max_four','max_five','commit','manifest_hash','contract_hash','runner_hash','output','entitlement','distribution','unknown','schema')) {
        $bad = Copy-Purpose $g; Assert-PurposeBindings $bad $inputs $output $head
        $fragment = 'Selected family/candidate binding mismatch.'
        switch ($change) {
            'purpose_id' { $bad.purpose_id = 'test-other-purpose'; $fragment = 'Purpose/run binding mismatch.' }
            'run_id' { $bad.run_id = 'test-other-run'; $fragment = 'Purpose/run binding mismatch.' }
            'families' { $bad.family_ids = @('test_family_two') }
            'candidates' { $bad.candidate_ids = @('test-other-a','test-other-b') }
            'duplicate' { $bad.candidate_ids = @('wet-work-a','wet-work-a') }
            'reordered' { $bad.candidate_ids = @('wet-work-b','wet-work-a') }
            'max_four' { $bad.max_calls = 4; $fragment = 'exact effective selected max_calls' }
            'max_five' { $bad.max_calls = 5; $fragment = 'exact effective selected max_calls' }
            'commit' { $bad.reviewed_commit = 'f' * 40; $fragment = 'current reviewed Git checkpoint' }
            'manifest_hash' { $bad.manifest_sha256 = 'f' * 64; $fragment = 'input/tooling hash mismatch.' }
            'contract_hash' { $bad.contract_sha256 = 'f' * 64; $fragment = 'input/tooling hash mismatch.' }
            'runner_hash' { $bad.tooling_sha256 = 'f' * 64; $fragment = 'input/tooling hash mismatch.' }
            'output' { $bad.output_directory = Join-Path $script:PurposeRoot 'TEST-other-output'; $fragment = 'one exact absolute output directory' }
            'entitlement' { $bad.active_entitlement_confirmed = $false; $fragment = 'existing owner entitlement attestation' }
            'distribution' { $bad.public_distribution_approved = $true; $fragment = 'cannot authorize distribution' }
            'unknown' { $bad | Add-Member NoteProperty per_family_max_calls 2; $fragment = 'Unexpected or missing object fields.' }
            'schema' { $bad.schema = 'grindless.sound-purpose-grant/v3'; $fragment = 'Wrong grant schema.' }
        }
        Reject-Purpose { Assert-PurposeBindings $bad $inputs $output $head } 'SD_VALIDATION' $fragment
    }
    Reject-Purpose { Assert-PurposeBindings $g $twoInputs $output $head } 'SD_VALIDATION' 'Purpose/run binding mismatch.'
    Reject-Purpose { Assert-PurposeBindings $twoGrant $inputs $output $head } 'SD_VALIDATION' 'Purpose/run binding mismatch.'
    $changed = Copy-Purpose $inputs.Manifest; $changed.review_status = 'unapproved'; $changed.families[0].candidates[0].prompt += ' Changed.'
    $changedInputs = New-FixtureInputs $changed
    Reject-Purpose { Assert-PurposeBindings $g $changedInputs $output $head } 'SD_VALIDATION' 'input/tooling hash mismatch.'
    Reject-Purpose { Assert-PurposeBindings $g $inputs $output ('e' * 40) } 'SD_VALIDATION' 'current reviewed Git checkpoint'
    Invoke-CheckpointFixture $true $true
    Check-Purpose ($script:CheckpointGitCalls -eq 2) 'real checkpoint guard inspects tracked inputs and the entire owned subtree'
    Reject-Purpose { Invoke-CheckpointFixture $false $true } 'SD_VALIDATION' 'must all be tracked'
    Reject-Purpose { Invoke-CheckpointFixture $true $false } 'SD_VALIDATION' 'all dirty/untracked sound-design inputs'

    $case = New-TestLedger 'interrupted' $inputs
    $lock = Open-SoundLock $case.Output
    try { Reject-Purpose { Open-SoundLock $case.Output } 'SD_LOCKED' 'locked or not writable' } finally { $lock.Dispose() }
    $case.Receipt = Reserve-Attempt $case.Path $case.Receipt $inputs $case.Output 'wet-work-a'
    $disk = Read-SoundJson $case.Path
    Check-Purpose ($disk.schema -ceq 'grindless.sound-purpose-receipt/v2' -and $disk.max_calls -eq 2 -and
        $disk.attempts.Count -eq 1 -and $disk.attempts[0].state -ceq 'attempting' -and $disk.attempts[0].started_utc -is [string]) 'v2 reservation persists selected two and original UTC strings'
    Resolve-Interrupted $case.Path $disk
    $disk = Read-SoundJson $case.Path
    Reject-Purpose { Reserve-Attempt $case.Path $disk $inputs $case.Output 'wet-work-b' } 'SD_VALIDATION' 'Human review is required, never a retry.'
    Resolve-Interrupted $case.Path $disk
    Check-Purpose ((Read-SoundJson $case.Path).attempts[0].state -ceq 'needs_review' -and
        (Read-SoundJson $case.Path).attempts.Count -eq 1 -and $script:PurposeTransportCalls -eq 0) 'ambiguous reservation survives reentry without transport or clearing spend'
    $bad = Copy-Purpose $disk; $bad.attempts[0].state = 'http_error'; $bad.attempts[0].http_status = 429
    Assert-Receipt $bad $inputs $case.Output
    Reject-Purpose { Assert-CanAttempt $bad } 'SD_VALIDATION' 'Human review is required, never a retry.'
    $stale = Copy-Purpose $disk; $disk.attempts[0].failure_code = 'TEST changed on disk'
    Write-SoundJson $case.Path $disk
    Reject-Purpose { Reserve-Attempt $case.Path $stale $inputs $case.Output 'wet-work-b' } 'SD_VALIDATION' 'Receipt changed since validation'

    $done = New-TestLedger 'selected-two-completed' $inputs; Complete-TestLedger $done $inputs
    Assert-ReceiptGrant $done.Receipt $done.Grant ('0' * 64) $inputs
    Reject-Purpose { Assert-CanAttempt $done.Receipt } 'SD_VALIDATION' 'All selected purpose attempts are consumed.'
    Reject-Purpose { Reserve-Attempt $done.Path $done.Receipt $inputs $done.Output 'test-spare-candidate' } 'SD_VALIDATION' 'All selected purpose attempts are consumed.'
    $bad = Copy-Purpose $done.Receipt; $bad.max_calls = 4
    Reject-Purpose { Assert-Receipt $bad $inputs $done.Output } 'SD_VALIDATION' 'Receipt identity, path or budget mismatch.'
    $bad = Copy-Purpose $done.Receipt; $bad.attempts[1].candidate_id = 'wet-work-a'
    Reject-Purpose { Assert-Receipt $bad $inputs $done.Output } 'SD_VALIDATION' 'Duplicate candidate or invalid attempt ordering.'
    $bad = Copy-Purpose $done.Receipt; $bad | Add-Member NoteProperty per_family_max_calls 2
    Reject-Purpose { Assert-Receipt $bad $inputs $done.Output } 'SD_VALIDATION' 'Unexpected or missing object fields.'
    foreach ($field in @('purpose_id','run_id','purpose_array','grant_id_array','family_ids','candidate_ids','tooling_sha256','output_directory','schema')) {
        $bad = Copy-Purpose $done.Receipt; Assert-Receipt $bad $inputs $done.Output
        $fragment = 'Receipt purpose/run/selected candidate binding mismatch.'
        switch ($field) {
            'purpose_array' { $bad.purpose_id = @($bad.purpose_id,'test-other-purpose') }
            'grant_id_array' { $bad.grant_id = @($bad.grant_id,'test-other-grant'); $fragment = 'Receipt identity, path or budget mismatch.' }
            'family_ids' { $bad.$field = @('test_family_two') }
            'candidate_ids' { $bad.$field = @('wet-work-b','wet-work-a') }
            'tooling_sha256' { $bad.$field = 'f' * 64; $fragment = 'exact runner and an attempt array' }
            'output_directory' { $bad.$field = $case.Output; $fragment = 'Receipt identity, path or budget mismatch.' }
            'schema' { $bad.$field = 'grindless.sound-purpose-receipt/v3'; $fragment = 'Receipt identity, path or budget mismatch.' }
            default { $bad.$field = 'test-cross-purpose' }
        }
        Reject-Purpose { Assert-Receipt $bad $inputs $done.Output } 'SD_VALIDATION' $fragment
    }
    foreach ($change in @('id','hash','commit')) {
        $bad = Copy-Purpose $done.Grant
        Assert-ReceiptGrant $done.Receipt $bad ('0' * 64) $inputs
        $hash = '0' * 64
        switch ($change) { 'id' { $bad.id = 'test-replacement-grant' }; 'hash' { $hash = 'f' * 64 }; 'commit' { $bad.reviewed_commit = 'f' * 40 } }
        Reject-Purpose { Assert-ReceiptGrant $done.Receipt $bad $hash $inputs } 'SD_VALIDATION' 'Receipt/grant/checkpoint mismatch'
    }
    $four = New-TestLedger 'two-families-one-ledger' $twoInputs; Complete-TestLedger $four $twoInputs
    Check-Purpose ($four.Receipt.attempts.Count -eq 4 -and $four.Receipt.family_ids.Count -eq 2 -and
        $four.Receipt.output_directory -ceq $four.Output) 'two families share one four-attempt ledger/output, not separate budgets'
    Reject-Purpose { Assert-CanAttempt $four.Receipt } 'SD_VALIDATION' 'All selected purpose attempts are consumed.'
    $bad = Copy-Purpose $four.Receipt; $bad.attempts += $bad.attempts[0]
    Reject-Purpose { Assert-Receipt $bad $twoInputs $four.Output } 'SD_VALIDATION' 'Receipt budget is invalid.'
    Reject-Purpose { Assert-Receipt $done.Receipt $twoInputs $done.Output } 'SD_VALIDATION' 'Receipt identity, path or budget mismatch.'
    $cross = Copy-Purpose $inputs.Manifest; $cross.review_status = 'unapproved'; $cross.purpose_id = 'test-other-purpose'
    $crossInputs = New-FixtureInputs $cross
    Reject-Purpose { Assert-Receipt $done.Receipt $crossInputs $done.Output } 'SD_VALIDATION' 'Receipt purpose/run/selected candidate binding mismatch.'

    $candidate = @(Get-Candidates $inputs)[0]
    $provenance = New-SoundProvenance $inputs $done.Grant $done.Receipt.attempts[0] $candidate
    Check-Purpose ($provenance.purpose_id -ceq $inputs.Manifest.purpose_id -and $provenance.family_id -ceq 'wet_work' -and
        (Test-OrderedIds $provenance.machines @('clay_mixer')) -and $provenance.request.text -ceq $candidate.prompt -and
        $provenance.entitlement_basis -ceq 'existing_owner_attestation_no_account_or_billing_requests' -and
        $provenance.source_sha256 -ceq $done.Receipt.attempts[0].source_sha256 -and
        -not ($provenance | ConvertTo-Json -Depth 12).Contains('Quern') -and
        -not ($provenance | ConvertTo-Json -Depth 12).Contains('Astra')) 'new provenance binds purpose, prompt/settings, attestation, terms and source hash without legacy identity'
    Write-Audition $inputs $done.Receipt $done.Output
    $html = [IO.File]::ReadAllText((Join-Path $done.Output 'audition.html'))
    Check-Purpose ($html.Contains($inputs.Manifest.label) -and $html.Contains($inputs.Manifest.purpose_id) -and
        $html.Contains('eleven_text_to_sound_v2') -and -not $html.Contains('Quern') -and
        -not $html.Contains('Astra') -and -not $html.Contains('<script') -and -not $html.Contains('https://')) 'new local audition is purpose-labelled, with no remote resources or invented listening pass'
    Check-Purpose ($script:SecretGateCalls -eq 0 -and $script:PurposeTransportCalls -eq 0) 'all real-guard tests remain before secret access and transport'
    & (Join-Path $PSScriptRoot 'test-generation-flow.ps1') -Purpose
    Write-Output ('PASS: ' + $script:PurposeChecks + ' focused v2 purpose assertions plus the v2 flow suite; no approved Sol fixtures, credentials, HTTP, codecs or JVMs.')
} finally {
    if ([IO.Directory]::Exists($script:PurposeRoot)) { Remove-Item -LiteralPath $script:PurposeRoot -Recurse -Force }
}
