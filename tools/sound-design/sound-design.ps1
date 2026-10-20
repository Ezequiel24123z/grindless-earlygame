[CmdletBinding()]
param(
    [ValidateSet('Plan', 'Generate', 'QC')][string]$Mode = 'Plan',
    [string]$ManifestPath,
    [string]$ContractPath,
    [string]$GrantFile,
    [string]$OutputDirectory,
    [string]$SecretFile,
    [string]$FfmpegPath,
    [string]$FfprobePath
)

# Development only. Dot-sourcing defines helpers for the offline tests, not an invocation.
Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$script:SoundHome = $PSScriptRoot
$script:SoundScript = $PSCommandPath
$script:SoundRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (-not $ManifestPath) { $ManifestPath = Join-Path $script:SoundRoot 'docs/assets/t0-sound-design.json' }
if (-not $ContractPath) { $ContractPath = Join-Path $script:SoundHome 'request-contract.json' }
$script:SafeFailure = 'SD_LOCAL_FAILURE: Local operation failed; no raw diagnostic was recorded.'
$script:Endpoint = 'https://api.elevenlabs.io/v1/sound-generation?output_format=mp3_44100_128'
$script:CandidateIds = @('quern-a', 'quern-b', 'quern-c')

function Stop-Sound([string]$Code, [string]$Message) {
    $script:SafeFailure = $Code + ': ' + $Message
    throw [InvalidOperationException]::new($script:SafeFailure)
}
function Require-Sound([bool]$Condition, [string]$Message) {
    if (-not $Condition) { Stop-Sound 'SD_VALIDATION' $Message }
}
function ConvertFrom-SoundJson([string]$Json) {
    # Core 7.5+ otherwise coerces ISO strings to DateTime; WinPS5 keeps strings.
    $options = @{}
    if ((Get-Command ConvertFrom-Json -CommandType Cmdlet).Parameters.ContainsKey('DateKind')) {
        $options['DateKind'] = 'String'
    }
    return (ConvertFrom-Json -InputObject $Json @options)
}
function Read-SoundJson([string]$Path) {
    try {
        $Path = Get-LocalPath $Path
        $item = Get-Item -LiteralPath $Path -Force
        Require-Sound (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0 -and $item.Length -le 262144) 'JSON must be a small regular local file, not a redirected path.'
        return (ConvertFrom-SoundJson ([IO.File]::ReadAllText($Path)))
    } catch { Stop-Sound 'SD_JSON' 'Cannot read the required JSON object; check its path, size and syntax.' }
}
function Get-SoundHash([string]$Path) {
    $Path = Get-LocalPath $Path
    Require-Sound (((Get-Item -LiteralPath $Path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'Do not hash redirected/symlink files.'
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}
function Assert-Keys($Value, [string[]]$Keys) {
    Require-Sound ($null -ne $Value) 'Missing object.'
    $actual = @($Value.PSObject.Properties.Name)
    Require-Sound ($actual.Count -eq $Keys.Count -and @($actual | Where-Object { $Keys -cnotcontains $_ }).Count -eq 0) 'Unexpected or missing object fields.'
}
function Test-Number($Value, [double]$Expected) {
    return (($Value -is [int] -or $Value -is [long] -or $Value -is [double] -or $Value -is [decimal]) -and [double]$Value -eq $Expected)
}
function Test-Bool($Value, [bool]$Expected) { return ($Value -is [bool] -and $Value -eq $Expected) }
function Test-UtcTimestamp($Value) {
    if ($Value -isnot [string] -or $Value -cnotmatch '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,7})?Z$') { return $false }
    $parsed = [DateTimeOffset]::MinValue
    return [DateTimeOffset]::TryParse($Value, [Globalization.CultureInfo]::InvariantCulture,
        [Globalization.DateTimeStyles]::None, [ref]$parsed)
}
function Get-LocalPath([string]$Path) {
    $full = [IO.Path]::GetFullPath($Path)
    Require-Sound (-not $full.StartsWith('\\') -and -not $full.StartsWith('//')) 'Network/UNC filesystem paths are not supported.'
    if ($env:OS -eq 'Windows_NT') {
        Require-Sound ([IO.DriveInfo]::new([IO.Path]::GetPathRoot($full)).DriveType -ne [IO.DriveType]::Network) 'Use a local volume, not a mapped network drive.'
    }
    return $full
}

function Test-Purpose($Inputs) { return ($Inputs.Manifest.schema -ceq 'grindless.sound-purpose-manifest/v2') }
function Test-SoundText($Value, [int]$Maximum) {
    return ($Value -is [string] -and $Value.Length -gt 0 -and $Value.Length -le $Maximum -and $Value -notmatch '[\x00-\x1f\x7f]')
}
function Test-OrderedIds($Actual, [object[]]$Expected) {
    if ($Actual -isnot [array] -or $Actual.Count -ne $Expected.Count) { return $false }
    for ($i = 0; $i -lt $Expected.Count; $i++) {
        if ($Actual[$i] -isnot [string] -or $Actual[$i] -cne $Expected[$i]) { return $false }
    }
    return $true
}
function Get-Candidates($Inputs) {
    if (Test-Purpose $Inputs) {
        foreach ($family in $Inputs.Manifest.families) { foreach ($candidate in $family.candidates) { $candidate } }
    } else { $Inputs.Manifest.calibration.candidates }
}
function Get-CallLimit($Inputs) {
    if (Test-Purpose $Inputs) { return [int]$Inputs.Manifest.max_calls }
    return 3
}
function Get-GrantCommit($Grant) {
    if ($Grant.schema -ceq 'grindless.sound-purpose-grant/v2') { return $Grant.reviewed_commit }
    return $Grant.manifest_commit
}
function Assert-ProviderContract($Contract) {
    Require-Sound ($Contract.method -is [string] -and $Contract.endpoint -is [string] -and $Contract.method -ceq 'POST' -and $Contract.endpoint -ceq $script:Endpoint) 'Only the exact official HTTPS POST endpoint is allowed.'
    Require-Sound ($Contract.reference -is [string] -and $Contract.reference -ceq 'https://elevenlabs.io/docs/api-reference/text-to-sound-effects/convert') 'Wrong contract reference.'
    Require-Sound ($Contract.authentication_header -is [string] -and $Contract.content_type -is [string] -and $Contract.response -is [string] -and $Contract.authentication_header -ceq 'xi-api-key' -and $Contract.content_type -ceq 'application/json' -and $Contract.response -ceq 'binary_audio_not_JSON') 'Wrong authentication or response contract.'
    Assert-Keys $Contract.body_parameters @('model_id','duration_seconds','loop','prompt_influence')
    Require-Sound ($Contract.body_parameters.model_id -is [string] -and $Contract.body_parameters.model_id -ceq 'eleven_text_to_sound_v2' -and (Test-Number $Contract.body_parameters.duration_seconds 3) -and (Test-Bool $Contract.body_parameters.loop $true) -and (Test-Number $Contract.body_parameters.prompt_influence 0.3)) 'Unexpected provider parameters.'
    Require-Sound ((Test-Number $Contract.text_max_characters 450) -and (Test-Number $Contract.automatic_retries 0) -and (Test-Bool $Contract.alternate_formats $false) -and (Test-Bool $Contract.publication_approved $false)) 'Unexpected text limit, retry, format fallback or publication permission.'
}
function Assert-Contract($Contract) {
    Require-Sound ($null -ne $Contract -and $Contract.PSObject.Properties.Name -ccontains 'schema') 'Missing contract schema.'
    Require-Sound (@('grindless.sound-calibration-contract/v1','grindless.sound-purpose-contract/v2') -ccontains $Contract.schema) 'Wrong contract schema.'
    if ($Contract.schema -ceq 'grindless.sound-purpose-contract/v2') {
        Assert-Keys $Contract @('schema','id','review_status','authorization_authority','reference','method','endpoint','authentication_header','content_type','text_max_characters','body_parameters','response','wave_limits','automatic_retries','alternate_formats','licence_scope','publication_approved','notes')
        $physicalWave = $Contract.id -ceq 't0-physical-20261020-elevenlabs-purpose-3s-mp3'
        Require-Sound ($Contract.id -is [string] -and $Contract.authorization_authority -is [string] -and ($physicalWave -or $Contract.id -ceq 'adr0156-elevenlabs-internal-purpose-3s-mp3') -and $Contract.authorization_authority -ceq 'Sol' -and @('unapproved','reviewed') -ccontains $Contract.review_status) 'Wrong purpose contract identity, authority or review status.'
        Assert-ProviderContract $Contract
        Assert-Keys $Contract.wave_limits @('max_families','max_candidates_per_family','max_calls')
        $families = 2; $calls = 4
        if ($physicalWave) { $families = 4; $calls = 8 }
        Require-Sound ((Test-Number $Contract.wave_limits.max_families $families) -and (Test-Number $Contract.wave_limits.max_candidates_per_family 2) -and (Test-Number $Contract.wave_limits.max_calls $calls)) 'ADR-0156 ceilings remain fixed to the selected contract identity: legacy 2/2/4, new physical wave 4/2/8.'
        Require-Sound ($Contract.licence_scope -ceq 'internal_audition_only') 'Purpose output is internal audition only.'
        return
    }
    Assert-Keys $Contract @('schema','id','review','reference','method','endpoint','authentication_header','content_type','text_max_characters','body_parameters','response','max_calls','automatic_retries','alternate_formats','licence_scope','publication_approved','notes')
    Require-Sound ($Contract.schema -ceq 'grindless.sound-calibration-contract/v1' -and $Contract.id -ceq 'elevenlabs-sfx-v2-internal-3s-mp3') 'Wrong contract identity.'
    Assert-ProviderContract $Contract
    Require-Sound ((Test-Number $Contract.text_max_characters 450) -and (Test-Number $Contract.max_calls 3) -and (Test-Number $Contract.automatic_retries 0)) 'Wrong text or spending limit.'
    Require-Sound ((Test-Bool $Contract.alternate_formats $false) -and (Test-Bool $Contract.publication_approved $false) -and $Contract.licence_scope -ceq 'internal_calibration_only') 'Unexpected format fallback or publication permission.'
}
function Assert-PurposeManifest($Manifest, $Contract) {
    Assert-Keys $Manifest @('schema','source_revision','policy','purpose_id','run_id','label','review_status','contract_id','max_calls','automatic_rerolls','licence_scope','runtime_import_approved','publication_approved','technical_qc_is_artistic_approval','mechanism_evidence','cost_note','families','next_gate')
    Require-Sound ($Contract.schema -ceq 'grindless.sound-purpose-contract/v2' -and $Manifest.contract_id -ceq $Contract.id -and $Manifest.policy -ceq 'ADR-0156') 'Purpose requires its v2 contract and ADR-0156 policy.'
    Require-Sound ($Manifest.source_revision -is [string] -and $Manifest.source_revision -cmatch '^[0-9a-f]{40}$' -and $Manifest.purpose_id -is [string] -and $Manifest.purpose_id -cmatch '^[a-z0-9][a-z0-9-]{2,63}$' -and $Manifest.run_id -is [string] -and $Manifest.run_id -cmatch '^[a-z0-9][a-z0-9-]{2,63}$') 'Invalid purpose, run or source identity.'
    Require-Sound ((Test-SoundText $Manifest.label 120) -and (Test-SoundText $Manifest.mechanism_evidence 1600) -and (Test-SoundText $Manifest.cost_note 1600) -and (Test-SoundText $Manifest.next_gate 1600) -and @('unapproved','reviewed') -ccontains $Manifest.review_status) 'Missing bounded purpose evidence or review status.'
    Require-Sound ($Manifest.licence_scope -ceq 'internal_audition_only' -and (Test-Bool $Manifest.automatic_rerolls $false) -and (Test-Bool $Manifest.runtime_import_approved $false) -and (Test-Bool $Manifest.publication_approved $false) -and (Test-Bool $Manifest.technical_qc_is_artistic_approval $false)) 'Purpose cannot authorize rerolls, import, publication or a listening pass.'
    Require-Sound ($Manifest.families -is [array] -and $Manifest.families.Count -ge 1 -and $Manifest.families.Count -le $Contract.wave_limits.max_families) 'Select one or two families under the legacy contract, or at most four under the new physical-wave contract.'
    $familyIds = @(); $candidateIds = @()
    foreach ($family in $Manifest.families) {
        Assert-Keys $family @('id','machines','candidates')
        Require-Sound ($family.id -is [string] -and $family.id -cmatch '^[a-z][a-z0-9_]{1,63}$' -and $familyIds -cnotcontains $family.id) 'Invalid or duplicate family ID.'
        $familyIds += $family.id
        Require-Sound ($family.machines -is [array] -and $family.machines.Count -gt 0) 'A selected family must name its machines.'
        $machines = @()
        foreach ($machine in $family.machines) {
            Require-Sound ($machine -is [string] -and $machine -cmatch '^[a-z][a-z0-9_]{1,63}$' -and $machines -cnotcontains $machine) 'Invalid or duplicate machine ID.'
            $machines += $machine
        }
        Require-Sound ($family.candidates -is [array] -and $family.candidates.Count -ge 1 -and $family.candidates.Count -le 2) 'Select one or two candidates per family, never more.'
        foreach ($candidate in $family.candidates) {
            Assert-Keys $candidate @('id','label','difference','prompt','artistic_approval')
            Require-Sound ($candidate.id -is [string] -and $candidate.id -cmatch '^[a-z0-9][a-z0-9-]{2,63}$' -and $candidateIds -cnotcontains $candidate.id) 'Invalid or duplicate candidate ID.'
            $candidateIds += $candidate.id
            Require-Sound ((Test-SoundText $candidate.label 120) -and (Test-SoundText $candidate.difference 450) -and (Test-SoundText $candidate.prompt 450)) 'Candidate label, difference and concrete prompt must be bounded text (prompt 1..450 characters).'
            Require-Sound ($candidate.artistic_approval -ceq 'unapproved') 'Technical tooling cannot claim artistic approval.'
        }
    }
    Require-Sound ((Test-Number $Manifest.max_calls $candidateIds.Count) -and $candidateIds.Count -le $Contract.wave_limits.max_calls) 'Effective max_calls must equal the finite selected candidate count, never the unused wave ceiling.'
}
function Assert-Manifest($Manifest, $Contract) {
    Require-Sound ($null -ne $Manifest -and $Manifest.PSObject.Properties.Name -ccontains 'schema') 'Missing manifest schema.'
    Require-Sound (@('grindless.t0-sound-design/v1','grindless.sound-purpose-manifest/v2') -ccontains $Manifest.schema) 'Wrong manifest schema.'
    if ($Manifest.schema -ceq 'grindless.sound-purpose-manifest/v2') { Assert-PurposeManifest $Manifest $Contract; return }
    Require-Sound ($Contract.schema -ceq 'grindless.sound-calibration-contract/v1') 'Legacy manifest requires its v1 contract.'
    Require-Sound ($Manifest.schema -ceq 'grindless.t0-sound-design/v1' -and $Manifest.policy -ceq 'ADR-0145') 'Wrong manifest or policy.'
    Require-Sound ((Test-Bool $Manifest.publication_approved $false) -and (Test-Bool $Manifest.licence.internal_calibration_only $true) -and (Test-Bool $Manifest.licence.asset_import_approved $false)) 'Internal calibration cannot authorize import or publication.'
    $c = $Manifest.calibration
    Require-Sound ($c.machine -ceq 'quern' -and $c.sound_id -ceq 'grindless:t0_grinding' -and $c.contract_id -ceq $Contract.id) 'Only the approved Quern role is in scope.'
    Require-Sound ((Test-Number $c.max_calls 3) -and (Test-Number $c.listening_segment_seconds 3) -and (Test-Bool $c.automatic_rerolls $false) -and (Test-Bool $c.runtime_import_approved $false) -and (Test-Bool $c.technical_qc_is_artistic_approval $false)) 'Wrong calibration boundaries.'
    Require-Sound (@($c.candidates).Count -eq 3) 'Exactly three contrastive prompts are required.'
    for ($i = 0; $i -lt 3; $i++) {
        $candidate = $c.candidates[$i]
        Require-Sound ($candidate.id -ceq $script:CandidateIds[$i]) 'Candidate identities/order changed.'
        Require-Sound ($candidate.prompt -is [string] -and $candidate.prompt.Length -gt 0 -and $candidate.prompt.Length -le 450 -and $candidate.prompt -notmatch '[\x00-\x1f\x7f]') 'Prompt must contain 1..450 characters without control characters.'
    }
}
function Get-Payload($Candidate, $Contract) {
    return ([ordered]@{ text = $Candidate.prompt; model_id = $Contract.body_parameters.model_id
        duration_seconds = 3.0; loop = $true; prompt_influence = 0.3 } | ConvertTo-Json -Compress)
}
function Get-Inputs([string]$Manifest, [string]$Contract) {
    $m = Read-SoundJson $Manifest; $c = Read-SoundJson $Contract
    Assert-Contract $c; Assert-Manifest $m $c
    return [pscustomobject]@{ Manifest = $m; Contract = $c; ManifestHash = (Get-SoundHash $Manifest)
        ContractHash = (Get-SoundHash $Contract); ToolingHash = (Get-SoundHash $script:SoundScript) }
}
function Get-GitHead {
    $result = @(& git -C $script:SoundRoot rev-parse HEAD 2>$null)
    Require-Sound ($LASTEXITCODE -eq 0 -and $result.Count -eq 1 -and $result[0] -cmatch '^[0-9a-f]{40}$') 'Cannot establish the source checkpoint.'
    return [string]$result[0]
}
function Test-Within([string]$Path, [string]$Parent) {
    $comparison = [StringComparison]::Ordinal
    if ($env:OS -eq 'Windows_NT') { $comparison = [StringComparison]::OrdinalIgnoreCase }
    $p = $Parent.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    return ($Path.Equals($p, $comparison) -or $Path.StartsWith($p + [IO.Path]::DirectorySeparatorChar, $comparison))
}
function Get-OutputPath([string]$Path) {
    Require-Sound (-not [string]::IsNullOrWhiteSpace($Path) -and [IO.Path]::IsPathRooted($Path)) 'Specify an absolute candidate directory; no default drive is assumed.'
    $full = (Get-LocalPath $Path).TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    Require-Sound ($full.Length -gt [IO.Path]::GetPathRoot($full).Length) 'A filesystem root is not a candidate directory.'
    $ancestor = $full
    while ($ancestor) {
        if (Test-Path -LiteralPath $ancestor) {
            Require-Sound (((Get-Item -LiteralPath $ancestor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'Symlink/junction output paths are not supported.'
        }
        $ancestor = [IO.Path]::GetDirectoryName($ancestor)
    }
    if (Test-Within $full $script:SoundRoot) {
        & git -C $script:SoundRoot check-ignore --quiet -- $full
        Require-Sound ($LASTEXITCODE -eq 0) 'Candidate output inside the repository must be ignored.'
    }
    return $full
}
function Assert-GrantShape($Grant) {
    Require-Sound ($null -ne $Grant -and $Grant.PSObject.Properties.Name -ccontains 'schema') 'Missing grant schema.'
    Require-Sound (@('grindless.sound-calibration-grant/v1','grindless.sound-purpose-grant/v2') -ccontains $Grant.schema) 'Wrong grant schema.'
    if ($Grant.schema -ceq 'grindless.sound-purpose-grant/v2') {
        Assert-Keys $Grant @('schema','id','generation_approved','approved_by','authorization_reference','approved_utc','expires_utc','reviewed_commit','manifest_sha256','contract_sha256','tooling_sha256','purpose_id','run_id','family_ids','candidate_ids','max_calls','output_directory','active_entitlement_confirmed','entitlement_at_generation','entitlement_basis','licence_scope','public_distribution_approved')
        return
    }
    Require-Sound ($Grant.schema -ceq 'grindless.sound-calibration-grant/v1') 'Wrong grant schema.'
    Assert-Keys $Grant @('schema','id','generation_approved','approved_by','authorization_reference','approved_utc','expires_utc','manifest_commit','contract_commit','tooling_commit','manifest_sha256','contract_sha256','tooling_sha256','output_directory','active_entitlement_confirmed','entitlement_at_generation','licence_scope','public_distribution_approved','max_calls','candidate_ids')
}
function Assert-PurposeBindings($Grant, $Inputs, [string]$Output, [string]$Head) {
    # Pure binding checks, also called by the real authorization guard. No approval,
    # filesystem, Git, secret, codec or transport operation is performed here.
    Assert-GrantShape $Grant
    Require-Sound ($Grant.schema -ceq 'grindless.sound-purpose-grant/v2') 'A purpose requires a v2 grant; legacy grants cannot authorize a new purpose.'
    Assert-Contract $Inputs.Contract; Assert-Manifest $Inputs.Manifest $Inputs.Contract
    Require-Sound ((Test-Purpose $Inputs) -and $Grant.purpose_id -is [string] -and $Grant.run_id -is [string] -and $Grant.purpose_id -ceq $Inputs.Manifest.purpose_id -and $Grant.run_id -ceq $Inputs.Manifest.run_id) 'Purpose/run binding mismatch.'
    Require-Sound ($Head -cmatch '^[0-9a-f]{40}$' -and $Grant.reviewed_commit -is [string] -and $Grant.reviewed_commit -ceq $Head) 'Purpose grant must bind the current reviewed Git checkpoint.'
    foreach ($field in @('ManifestHash','ContractHash','ToolingHash')) {
        Require-Sound ($Inputs.$field -is [string] -and $Inputs.$field -cmatch '^[0-9a-f]{64}$') 'Invalid current input hash.'
    }
    Require-Sound ($Grant.manifest_sha256 -is [string] -and $Grant.contract_sha256 -is [string] -and $Grant.tooling_sha256 -is [string] -and $Grant.manifest_sha256 -ceq $Inputs.ManifestHash -and $Grant.contract_sha256 -ceq $Inputs.ContractHash -and $Grant.tooling_sha256 -ceq $Inputs.ToolingHash) 'Reviewed purpose input/tooling hash mismatch.'
    Require-Sound ((Test-OrderedIds $Grant.family_ids @($Inputs.Manifest.families | ForEach-Object { $_.id })) -and (Test-OrderedIds $Grant.candidate_ids @(Get-Candidates $Inputs | ForEach-Object { $_.id }))) 'Selected family/candidate binding mismatch.'
    Require-Sound ((Test-Number $Grant.max_calls (Get-CallLimit $Inputs))) 'Purpose grant must bind the exact effective selected max_calls.'
    Require-Sound ($Grant.output_directory -is [string] -and [IO.Path]::IsPathRooted($Output) -and $Grant.output_directory -ceq $Output -and [IO.Path]::GetFullPath($Output) -ceq $Output) 'Purpose grant binds one exact absolute output directory; no relocation or per-family reset.'
    Require-Sound ((Test-Bool $Grant.active_entitlement_confirmed $true) -and $Grant.entitlement_at_generation -ceq 'owner_confirmed_paid_subscription' -and $Grant.entitlement_basis -ceq 'existing_owner_attestation_no_account_or_billing_requests') 'Use the existing owner entitlement attestation; never query account/billing data.'
    Require-Sound ($Grant.licence_scope -ceq 'internal_audition_only' -and (Test-Bool $Grant.public_distribution_approved $false)) 'Purpose grant cannot authorize distribution.'
}
function Get-PurposeApprovalFailures($Grant, $Inputs) {
    if ($Inputs.Contract.review_status -cne 'reviewed') { 'contract.review_status' }
    if ($Inputs.Manifest.review_status -cne 'reviewed') { 'manifest.review_status' }
    if (-not (Test-Bool $Grant.generation_approved $true)) { 'grant.generation_approved' }
    if ($Grant.approved_by -isnot [string] -or $Grant.approved_by -cne 'Sol') { 'grant.approved_by' }
}
function Assert-PurposeGrant($Grant, $Inputs, [string]$Output, [string]$Head) {
    Assert-GrantShape $Grant
    Require-Sound ($Grant.schema -ceq 'grindless.sound-purpose-grant/v2') 'A purpose requires a v2 grant; legacy grants cannot authorize a new purpose.'
    $pending = @(Get-PurposeApprovalFailures $Grant $Inputs)
    if ($pending.Count -gt 0) {
        Stop-Sound 'SD_PURPOSE_UNAPPROVED' ('Unapproved generation markers: ' + ($pending -join ', ') + '. Explicit Sol review and a NEW private grant are required.')
    }
    Assert-PurposeBindings $Grant $Inputs $Output $Head
    Require-Sound ($Grant.id -is [string] -and $Grant.id -cmatch '^[a-z0-9][a-z0-9-]{2,63}$' -and $Grant.id -cne 'unapproved-purpose-example' -and (Test-SoundText $Grant.authorization_reference 160)) 'Missing NEW bounded Sol authorization reference/identity.'
    try {
        Require-Sound ((Test-UtcTimestamp $Grant.approved_utc) -and (Test-UtcTimestamp $Grant.expires_utc)) 'Grant timestamps must be original UTC strings.'
        $approved = [DateTimeOffset]::Parse($Grant.approved_utc, [Globalization.CultureInfo]::InvariantCulture)
        $expires = [DateTimeOffset]::Parse($Grant.expires_utc, [Globalization.CultureInfo]::InvariantCulture)
        Require-Sound ($approved -le [DateTimeOffset]::UtcNow -and $expires -gt [DateTimeOffset]::UtcNow -and $expires -gt $approved) 'Grant is not currently valid.'
    } catch { Stop-Sound 'SD_GRANT_TIME' 'Grant timestamps are invalid, expired or not yet valid.' }
}
function Assert-Grant($Grant, $Inputs, [string]$Output, [string]$Head) {
    if (Test-Purpose $Inputs) { Assert-PurposeGrant $Grant $Inputs $Output $Head; return }
    Assert-GrantShape $Grant
    Require-Sound ($Grant.schema -ceq 'grindless.sound-calibration-grant/v1') 'Legacy calibration requires its v1 grant.'
    Require-Sound ((Test-Bool $Grant.generation_approved $true) -and $Grant.approved_by -ceq 'Astra' -and $Grant.id -cmatch '^[a-z0-9][a-z0-9-]{2,63}$' -and $Grant.id -cne 'unapproved-example') 'A NEW explicit Astra grant is required.'
    Require-Sound ($Grant.authorization_reference -is [string] -and $Grant.authorization_reference.Length -gt 0 -and $Grant.authorization_reference.Length -le 160 -and $Grant.authorization_reference -notmatch '[\x00-\x1f\x7f]') 'Missing bounded authorization reference.'
    foreach ($field in @('manifest_commit','contract_commit','tooling_commit')) {
        Require-Sound ($Grant.$field -ceq $Head -and $Head -cmatch '^[0-9a-f]{40}$') 'Grant must bind the current reviewed Git checkpoint.'
    }
    Require-Sound ($Grant.manifest_sha256 -ceq $Inputs.ManifestHash -and $Grant.contract_sha256 -ceq $Inputs.ContractHash -and $Grant.tooling_sha256 -ceq $Inputs.ToolingHash) 'Reviewed input/tooling hash mismatch.'
    Require-Sound ($Grant.output_directory -is [string] -and (Get-OutputPath $Grant.output_directory) -ceq $Output) 'Grant binds one exact output directory; do not reset the budget in another folder.'
    Require-Sound ((Test-Bool $Grant.active_entitlement_confirmed $true) -and $Grant.entitlement_at_generation -ceq 'owner_confirmed_paid_subscription') 'Record owner-confirmed paid entitlement at generation; never fetch billing/account data.'
    Require-Sound ($Grant.licence_scope -ceq 'internal_calibration_only' -and (Test-Bool $Grant.public_distribution_approved $false) -and (Test-Number $Grant.max_calls 3) -and $Grant.candidate_ids -is [array] -and $Grant.candidate_ids.Count -eq 3 -and ($Grant.candidate_ids -join ',') -ceq ($script:CandidateIds -join ',')) 'Grant cannot expand calibration or distribution.'
    try {
        Require-Sound ((Test-UtcTimestamp $Grant.approved_utc) -and (Test-UtcTimestamp $Grant.expires_utc)) 'Grant timestamps must be original UTC strings.'
        $approved = [DateTimeOffset]::Parse($Grant.approved_utc, [Globalization.CultureInfo]::InvariantCulture)
        $expires = [DateTimeOffset]::Parse($Grant.expires_utc, [Globalization.CultureInfo]::InvariantCulture)
        Require-Sound ($approved -le [DateTimeOffset]::UtcNow -and $expires -gt [DateTimeOffset]::UtcNow -and $expires -gt $approved) 'Grant is not currently valid.'
    } catch { Stop-Sound 'SD_GRANT_TIME' 'Grant timestamps are invalid, expired or not yet valid.' }
}
function Assert-Checkpoint([string]$Manifest, [string]$Contract) {
    $m = Read-SoundJson $Manifest
    if ($m.schema -ceq 'grindless.sound-purpose-manifest/v2') {
        $manifestFull = Get-LocalPath $Manifest; $contractFull = Get-LocalPath $Contract
        $c = Read-SoundJson $Contract; Assert-Contract $c
        $contractName = 'purpose-contract.json'
        if ($c.id -ceq 't0-physical-20261020-elevenlabs-purpose-3s-mp3') { $contractName = 't0-physical-contract.json' }
        Require-Sound ((Test-Within $manifestFull $script:SoundHome) -and [IO.Path]::GetExtension($manifestFull) -ceq '.json' -and $contractFull -ceq [IO.Path]::GetFullPath((Join-Path $script:SoundHome $contractName))) 'Purpose generation inputs must be inside the reviewed sound-design subtree with its exact v2 contract.'
        $relative = $manifestFull.Substring($script:SoundRoot.Length + 1).Replace('\','/')
        $contractRelative = $contractFull.Substring($script:SoundRoot.Length + 1).Replace('\','/')
        & git -C $script:SoundRoot ls-files --error-unmatch -- $relative $contractRelative tools/sound-design/sound-design.ps1 1>$null 2>$null
        $tracked = ($LASTEXITCODE -eq 0)
        $status = @(& git -C $script:SoundRoot status --porcelain --untracked-files=all -- tools/sound-design 2>$null)
        Assert-PurposeCheckpointState $tracked ($LASTEXITCODE -eq 0 -and $status.Count -eq 0)
        return
    }
    Require-Sound ([IO.Path]::GetFullPath($Manifest) -ceq [IO.Path]::GetFullPath((Join-Path $script:SoundRoot 'docs/assets/t0-sound-design.json')) -and [IO.Path]::GetFullPath($Contract) -ceq [IO.Path]::GetFullPath((Join-Path $script:SoundHome 'request-contract.json'))) 'Generation uses only the repository manifest and contract.'
    $status = @(& git -C $script:SoundRoot status --porcelain --untracked-files=all -- docs/assets/t0-sound-design.json tools/sound-design 2>$null)
    Require-Sound ($LASTEXITCODE -eq 0 -and $status.Count -eq 0) 'Astra must review and checkpoint all owned files before generation.'
    & git -C $script:SoundRoot ls-files --error-unmatch -- docs/assets/t0-sound-design.json tools/sound-design/request-contract.json tools/sound-design/sound-design.ps1 1>$null 2>$null
    Require-Sound ($LASTEXITCODE -eq 0) 'Reviewed generation inputs must be tracked.'
}
function Assert-PurposeCheckpointState([bool]$Tracked, [bool]$Clean) {
    Require-Sound $Tracked 'Purpose manifest, contract and runner must all be tracked at the reviewed checkpoint.'
    Require-Sound $Clean 'Sol must review and checkpoint all dirty/untracked sound-design inputs before generation.'
}

function Write-SoundJson([string]$Path, $Value) {
    # Same-volume replacement + flush: a process crash never exposes a half-written receipt.
    if (Test-Path -LiteralPath $Path) {
        Require-Sound (((Get-Item -LiteralPath $Path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'Do not overwrite redirected/symlink files.'
    }
    $temp = $Path + '.' + [Guid]::NewGuid().ToString('N') + '.pending'
    try {
        $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($Value | ConvertTo-Json -Depth 24) + "`n")
        $stream = [IO.FileStream]::new($temp, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None, 4096, [IO.FileOptions]::WriteThrough)
        try { $stream.Write($bytes, 0, $bytes.Length); $stream.Flush($true) } finally { $stream.Dispose() }
        if ([IO.File]::Exists($Path)) { [IO.File]::Replace($temp, $Path, [System.Management.Automation.Language.NullString]::Value) } else { [IO.File]::Move($temp, $Path) }
    } finally { if ([IO.File]::Exists($temp)) { [IO.File]::Delete($temp) } }
}
function Open-SoundLock([string]$Output) {
    try { return [IO.FileStream]::new((Join-Path $Output 'calibration.lock'), [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None) }
    catch { Stop-Sound 'SD_LOCKED' 'Candidate folder is locked or not writable; do not use another folder to evade it.' }
}
function New-Receipt($Grant, $Inputs, [string]$GrantHash, [string]$Output) {
    $receipt = [ordered]@{ schema = 'grindless.sound-calibration-receipt/v1'; grant_id = $Grant.id
        grant_sha256 = $GrantHash; source_commit = (Get-GrantCommit $Grant); manifest_sha256 = $Inputs.ManifestHash
        contract_sha256 = $Inputs.ContractHash; tooling_sha256 = $Inputs.ToolingHash; output_directory = $Output
        max_calls = (Get-CallLimit $Inputs); attempts = @() }
    if (Test-Purpose $Inputs) {
        $receipt.schema = 'grindless.sound-purpose-receipt/v2'
        $receipt['purpose_id'] = $Inputs.Manifest.purpose_id; $receipt['run_id'] = $Inputs.Manifest.run_id
        $receipt['family_ids'] = @($Inputs.Manifest.families | ForEach-Object { $_.id })
        $receipt['candidate_ids'] = @(Get-Candidates $Inputs | ForEach-Object { $_.id })
    }
    return [pscustomobject]$receipt
}
function Assert-Receipt($Receipt, $Inputs, [string]$Output) {
    $keys = @('schema','grant_id','grant_sha256','source_commit','manifest_sha256','contract_sha256','tooling_sha256','output_directory','max_calls','attempts')
    $schema = 'grindless.sound-calibration-receipt/v1'; $limit = Get-CallLimit $Inputs
    $ids = @(Get-Candidates $Inputs | ForEach-Object { $_.id })
    if (Test-Purpose $Inputs) {
        $keys += @('purpose_id','run_id','family_ids','candidate_ids'); $schema = 'grindless.sound-purpose-receipt/v2'
    }
    Assert-Keys $Receipt $keys
    Require-Sound ($Receipt.schema -is [string] -and $Receipt.schema -ceq $schema -and $Receipt.grant_id -is [string] -and (Test-Number $Receipt.max_calls $limit) -and $Receipt.output_directory -is [string] -and $Receipt.output_directory -ceq $Output) 'Receipt identity, path or budget mismatch.'
    if (Test-Purpose $Inputs) {
        Require-Sound ($Receipt.purpose_id -is [string] -and $Receipt.run_id -is [string] -and $Receipt.purpose_id -ceq $Inputs.Manifest.purpose_id -and $Receipt.run_id -ceq $Inputs.Manifest.run_id -and (Test-OrderedIds $Receipt.family_ids @($Inputs.Manifest.families | ForEach-Object { $_.id })) -and (Test-OrderedIds $Receipt.candidate_ids $ids)) 'Receipt purpose/run/selected candidate binding mismatch.'
        Require-Sound ($Receipt.tooling_sha256 -ceq $Inputs.ToolingHash -and $Receipt.attempts -is [array]) 'Purpose receipt requires the exact runner and an attempt array.'
    }
    foreach ($field in @('grant_sha256','manifest_sha256','contract_sha256','tooling_sha256')) {
        Require-Sound ($Receipt.$field -is [string] -and $Receipt.$field -cmatch '^[0-9a-f]{64}$') 'Invalid receipt hash.'
    }
    Require-Sound ($Receipt.source_commit -is [string] -and $Receipt.source_commit -cmatch '^[0-9a-f]{40}$' -and $Receipt.manifest_sha256 -ceq $Inputs.ManifestHash -and $Receipt.contract_sha256 -ceq $Inputs.ContractHash) 'Receipt is bound to different source inputs.'
    Require-Sound ($null -ne $Receipt.attempts -and @($Receipt.attempts).Count -le $limit) 'Receipt budget is invalid.'
    $seen = @(); $ordinal = 0
    foreach ($attempt in @($Receipt.attempts)) {
        $ordinal++
        Assert-Keys $attempt @('ordinal','candidate_id','started_utc','state','completed_utc','http_status','request_id','raw_file','source_sha256','failure_code')
        Require-Sound ((Test-Number $attempt.ordinal $ordinal) -and $ids -ccontains $attempt.candidate_id -and $seen -cnotcontains $attempt.candidate_id) 'Duplicate candidate or invalid attempt ordering.'
        $seen += $attempt.candidate_id
        Require-Sound (@('attempting','response_saved','needs_review','http_error') -ccontains $attempt.state) 'Unknown receipt state; review rather than retry.'
        Require-Sound (Test-UtcTimestamp $attempt.started_utc) 'Missing or invalid UTC attempt timestamp.'
        Require-Sound ($null -eq $attempt.completed_utc -or (Test-UtcTimestamp $attempt.completed_utc)) 'Invalid UTC completion timestamp.'
        Require-Sound ($null -eq $attempt.request_id -or ($attempt.request_id -is [string] -and $attempt.request_id -cmatch '^[A-Za-z0-9._-]{1,128}$')) 'Unsafe request identifier.'
        if ($attempt.state -ceq 'response_saved') {
            Require-Sound ($attempt.raw_file -ceq ($attempt.candidate_id + '.raw.mp3') -and $attempt.source_sha256 -cmatch '^[0-9a-f]{64}$' -and (Test-Number $attempt.http_status 200)) 'Invalid saved response.'
            $raw = Join-Path $Output $attempt.raw_file
            Require-Sound ([IO.File]::Exists($raw) -and (Get-SoundHash $raw) -ceq $attempt.source_sha256) 'Saved source is missing or altered; no resend is allowed.'
        }
    }
}
function Assert-ReceiptGrant($Receipt, $Grant, [string]$GrantHash, $Inputs) {
    Require-Sound ($Receipt.grant_id -ceq $Grant.id -and $Receipt.grant_sha256 -ceq $GrantHash -and $Receipt.source_commit -ceq (Get-GrantCommit $Grant) -and $Receipt.tooling_sha256 -ceq $Inputs.ToolingHash) 'Receipt/grant/checkpoint mismatch; no reset or replacement grant is accepted.'
}
function Assert-CanAttempt($Receipt) {
    Require-Sound (@($Receipt.attempts | Where-Object { $_.state -cne 'response_saved' }).Count -eq 0) 'A prior attempt is unresolved/failed. Human review is required, never a retry.'
    if ($Receipt.schema -ceq 'grindless.sound-purpose-receipt/v2') {
        Require-Sound (@($Receipt.attempts).Count -lt $Receipt.max_calls) 'All selected purpose attempts are consumed.'
        return
    }
    Require-Sound (@($Receipt.attempts).Count -lt 3) 'All three attempts are consumed.'
}
function Reserve-Attempt([string]$ReceiptPath, $Receipt, $Inputs, [string]$Output, [string]$CandidateId) {
    $disk = Read-SoundJson $ReceiptPath
    Assert-Receipt $disk $Inputs $Output
    Require-Sound (($disk | ConvertTo-Json -Depth 24 -Compress) -ceq ($Receipt | ConvertTo-Json -Depth 24 -Compress)) 'Receipt changed since validation; stop rather than overwrite spend evidence.'
    Assert-Receipt $Receipt $Inputs $Output; Assert-CanAttempt $Receipt
    Require-Sound (@(Get-Candidates $Inputs | ForEach-Object { $_.id }) -ccontains $CandidateId -and @($Receipt.attempts | Where-Object { $_.candidate_id -ceq $CandidateId }).Count -eq 0) 'Candidate was already attempted or is not approved.'
    $Receipt.attempts = @($Receipt.attempts) + [pscustomobject][ordered]@{
        ordinal = @($Receipt.attempts).Count + 1; candidate_id = $CandidateId
        started_utc = [DateTime]::UtcNow.ToString('o'); state = 'attempting'; completed_utc = $null
        http_status = $null; request_id = $null; raw_file = $null; source_sha256 = $null; failure_code = $null }
    Write-SoundJson $ReceiptPath $Receipt
    $persisted = Read-SoundJson $ReceiptPath
    Assert-Receipt $persisted $Inputs $Output
    Require-Sound (($persisted | ConvertTo-Json -Depth 24 -Compress) -ceq ($Receipt | ConvertTo-Json -Depth 24 -Compress) -and $persisted.attempts[-1].state -ceq 'attempting' -and $persisted.attempts[-1].candidate_id -ceq $CandidateId) 'Reservation was not durably verified; no HTTP is allowed.'
    return $persisted
}
function Resolve-Interrupted([string]$ReceiptPath, $Receipt) {
    $interrupted = @($Receipt.attempts | Where-Object { $_.state -ceq 'attempting' })
    if ($interrupted.Count -gt 0) {
        foreach ($attempt in $interrupted) { $attempt.state = 'needs_review'; $attempt.failure_code = 'interrupted_delivery_unknown' }
        Write-SoundJson $ReceiptPath $Receipt
    }
}

function Find-SoundTool([string]$Name, [string]$Explicit) {
    $path = $Explicit
    if (-not $path) {
        $command = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { $path = $command.Source }
    }
    if (-not $path) { return $null }
    $path = Get-LocalPath $path
    if (-not [IO.File]::Exists($path)) { return $null }
    Require-Sound (((Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'Use an actual local tool executable, not a redirected path.'
    Require-Sound ([IO.Path]::GetFileNameWithoutExtension($path) -ceq $Name) 'Tool path must name the requested executable.'
    return (Get-LocalPath $path)
}
function Get-SoundTools([string]$Ffmpeg, [string]$Ffprobe, [bool]$Required = $false) {
    $tools = [pscustomobject]@{ Ffmpeg = (Find-SoundTool 'ffmpeg' $Ffmpeg); Ffprobe = (Find-SoundTool 'ffprobe' $Ffprobe) }
    if ($Required -and (-not $tools.Ffmpeg -or -not $tools.Ffprobe)) { Stop-Sound 'SD_TOOLS_MISSING' 'FFmpeg and ffprobe are required for MP3 QC before spending credits. Supply verified paths; nothing was installed.' }
    if ($Required) {
        Require-Sound ((Invoke-AudioTool $tools.Ffmpeg @('-version')) -match '^ffmpeg version ' -and (Invoke-AudioTool $tools.Ffprobe @('-version')) -match '^ffprobe version ') 'Executable identities are not FFmpeg/ffprobe.'
        Require-Sound ((Invoke-AudioTool $tools.Ffmpeg @('-hide_banner','-encoders')) -match '\blibvorbis\b') 'FFmpeg requires the existing libvorbis encoder; install nothing automatically.'
    }
    return $tools
}
function Invoke-AudioTool([string]$Tool, [string[]]$Arguments) {
    # WinPS5 promotes native stderr through 2>&1 under ErrorActionPreference=Stop.
    # Drain both real streams concurrently; only the actual process exit is success.
    $script:LastAudioProcess = $null
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $Tool; $start.UseShellExecute = $false; $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    if ($start.PSObject.Properties.Name -contains 'ArgumentList') {
        foreach ($argument in $Arguments) { [void]$start.ArgumentList.Add($argument) }
    } else {
        $quoted = foreach ($argument in $Arguments) {
            # Windows CommandLineToArgvW quoting, including embedded quotes/trailing slashes.
            $escaped = [regex]::Replace($argument, '(\\*)"', '$1$1\"')
            $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
            '"' + $escaped + '"'
        }
        $start.Arguments = $quoted -join ' '
    }
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $start
    $timedOut = $false
    try {
        [void]$process.Start()
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(120000)) {
            $timedOut = $true
            Stop-Process -Id $process.Id -Force
            $process.WaitForExit()
        }
        $script:LastAudioProcess = [pscustomobject]@{ exit_code = $process.ExitCode
            stdout = $stdoutTask.GetAwaiter().GetResult(); stderr = $stderrTask.GetAwaiter().GetResult()
            timed_out = $timedOut }
    } catch { Stop-Sound 'SD_CODEC' 'Cannot start or collect the audio process; no raw exception was reported.' }
    finally { $process.Dispose() }
    if ($timedOut) { Stop-Sound 'SD_CODEC_TIMEOUT' 'Audio process exceeded the bounded local timeout; its specific PID was stopped.' }
    if ($script:LastAudioProcess.exit_code -ne 0) {
        Stop-Sound 'SD_CODEC' ('Audio tool exited with code ' + $script:LastAudioProcess.exit_code + '. Diagnostics retained in process, never echoed automatically.')
    }
    # JSON metadata must not be contaminated by informational stderr.
    return $script:LastAudioProcess.stdout
}
function Get-Probe([string]$Path, $Tools) {
    $json = Invoke-AudioTool $Tools.Ffprobe @('-v','error','-protocol_whitelist','file','-show_entries','format=format_name,duration:stream=codec_type,codec_name,channels,sample_rate','-of','json',$Path)
    try { $probe = $json | ConvertFrom-Json } catch { Stop-Sound 'SD_PROBE' 'ffprobe did not return valid metadata.' }
    Require-Sound (@($probe.streams).Count -eq 1 -and $probe.streams[0].codec_type -ceq 'audio') 'Exactly one audio stream is required.'
    return [pscustomobject]@{ format = $probe.format.format_name; codec = $probe.streams[0].codec_name
        channels = [int]$probe.streams[0].channels; rate_hz = [int]$probe.streams[0].sample_rate
        duration_seconds = [double]::Parse($probe.format.duration, [Globalization.CultureInfo]::InvariantCulture) }
}

function Measure-Wave([string]$Path) {
    if (-not ('GrindlessSoundQc' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.IO;
public static class GrindlessSoundQc {
    // PCM16 and IEEE float32 RIFF only; MP3/Vorbis are decoded by FFmpeg, never guessed.
    public static double[] Read(string path) {
        using (var r = new BinaryReader(File.OpenRead(path))) {
            if (new string(r.ReadChars(4)) != "RIFF") throw new InvalidDataException();
            r.ReadUInt32();
            if (new string(r.ReadChars(4)) != "WAVE") throw new InvalidDataException();
            int format = 0, channels = 0, rate = 0, bits = 0, align = 0;
            long start = -1, size = 0;
            while (r.BaseStream.Position + 8 <= r.BaseStream.Length) {
                string tag = new string(r.ReadChars(4)); uint length = r.ReadUInt32();
                long body = r.BaseStream.Position, next = body + length + (length % 2);
                if (body + length > r.BaseStream.Length) throw new InvalidDataException();
                if (tag == "fmt " && length >= 16) {
                    format = r.ReadUInt16(); channels = r.ReadUInt16(); rate = r.ReadInt32();
                    r.ReadUInt32(); align = r.ReadUInt16(); bits = r.ReadUInt16();
                    if (format == 65534 && length >= 40) {
                        r.BaseStream.Position = body + 24; format = r.ReadUInt16();
                    }
                }
                if (tag == "data") { start = body; size = length; }
                r.BaseStream.Position = next;
            }
            if (start < 0 || size == 0 || channels < 1 || channels > 8 || rate < 8000 || rate > 192000 ||
                !((format == 1 && bits == 16) || (format == 3 && bits == 32)) ||
                align != channels * (bits / 8) || size % align != 0 || size > 16777216) throw new InvalidDataException();
            int frames = (int)(size / align), window = Math.Min(frames, Math.Max(1, rate / 100));
            double peak = 0, sum = 0, clipped = 0, silent = 0, leading = 0, trailing = 0, longest = 0;
            double seam = 0, slope = 0, headEnergy = 0, tailEnergy = 0;
            var first = new double[channels]; var second = new double[channels];
            var last = new double[channels]; var previous = new double[channels];
            r.BaseStream.Position = start;
            for (int f = 0; f < frames; f++) {
                bool quiet = true;
                for (int c = 0; c < channels; c++) {
                    double value = format == 1 ? r.ReadInt16() / 32768.0 : r.ReadSingle();
                    if (Double.IsNaN(value) || Double.IsInfinity(value)) throw new InvalidDataException();
                    double abs = Math.Abs(value); peak = Math.Max(peak, abs); sum += value * value;
                    if (abs >= 0.999) clipped++;
                    if (abs >= 0.001) quiet = false;
                    if (f < window) headEnergy += value * value;
                    if (f >= frames - window) tailEnergy += value * value;
                    if (f == 0) first[c] = value;
                    if (f == 1) second[c] = value;
                    previous[c] = last[c]; last[c] = value;
                }
                if (quiet) { silent++; trailing++; longest = Math.Max(longest, trailing); if (leading == f) leading++; }
                else trailing = 0;
            }
            for (int c = 0; c < channels; c++) {
                seam = Math.Max(seam, Math.Abs(first[c] - last[c]));
                slope = Math.Max(slope, Math.Abs((second[c] - first[c]) - (last[c] - previous[c])));
            }
            double rms = Math.Sqrt(sum / (frames * (double)channels));
            double energyDb = 10 * Math.Log10((headEnergy + 1e-15) / (tailEnergy + 1e-15));
            return new double[] { channels, rate, frames / (double)rate, peak, rms, clipped,
                silent / frames, longest / rate, leading / rate, trailing / rate, seam, slope, energyDb, format, bits };
        }
    }
}
'@
    }
    try { $v = [GrindlessSoundQc]::Read($Path) } catch { Stop-Sound 'SD_WAVE' 'Invalid, unsupported or oversized PCM/float WAV; no listening pass is implied.' }
    $flags = @()
    if ($v[0] -ne 1) { $flags += 'not_mono' }
    if ($v[1] -ne 44100) { $flags += 'not_44100_hz' }
    if ([Math]::Abs($v[2] - 3) -gt 0.12) { $flags += 'duration_outside_3s_tolerance' }
    if ($v[5] -gt 0) { $flags += 'clipping_or_near_full_scale' }
    if ($v[4] -lt 0.0031623 -or $v[6] -gt 0.95 -or $v[7] -gt 0.25) { $flags += 'silence_risk' }
    $boundary = ($v[10] -gt 0.02 -or $v[11] -gt 0.04 -or [Math]::Abs($v[12]) -gt 6 -or $v[8] -gt 0.05 -or $v[9] -gt 0.05)
    if ($boundary) { $flags += 'loop_boundary_risk' }
    $encoding = 'pcm_s16le'; if ($v[13] -eq 3) { $encoding = 'pcm_f32le' }
    return [pscustomobject][ordered]@{ format = 'wav'; encoding = $encoding; bits_per_sample = [int]$v[14]
        channels = [int]$v[0]; rate_hz = [int]$v[1]; duration_seconds = $v[2]
        peak_linear = $v[3]; rms_linear = $v[4]; clipped_samples = [long]$v[5]
        silent_frame_fraction = $v[6]; longest_silence_seconds = $v[7]
        leading_silence_seconds = $v[8]; trailing_silence_seconds = $v[9]
        loop_boundary = @{ sample_jump = $v[10]; slope_jump = $v[11]; window_energy_delta_db = $v[12]
            risk_detected = $boundary; listening_required = $true; listening_pass = $false }
        flags = @($flags); listening_pass = $false }
}

function Invoke-FileQc([string]$Raw, [string]$Id, [string]$Output, $Tools, $Inputs = $null) {
    Require-Sound ((Get-Item -LiteralPath $Raw).Length -le 2097152) 'Source audio exceeds the calibration size bound.'
    $probe = Get-Probe $Raw $Tools
    Require-Sound ($probe.format -ceq 'mp3' -and $probe.codec -ceq 'mp3' -and $probe.duration_seconds -gt 0 -and $probe.duration_seconds -le 6) 'Source is not bounded MP3 audio.'
    $decoded = Join-Path $Output ($Id + '.decode.tmp.wav')
    $preview = Join-Path $Output ($Id + '.preview.wav')
    $ogg = Join-Path $Output ($Id + '.candidate.ogg')
    $decodeArgs = @('-nostdin','-hide_banner','-v','error','-y','-protocol_whitelist','file','-i',$Raw,'-t','6','-map','0:a:0','-vn','-map_metadata','-1','-c:a','pcm_f32le',$decoded)
    $previewArgs = @('-nostdin','-hide_banner','-v','error','-y','-protocol_whitelist','file','-i',$Raw,'-map','0:a:0','-vn','-map_metadata','-1','-ac','1','-ar','44100','-af','volume=0.7079457844','-c:a','pcm_s16le',$preview)
    $oggArgs = @('-nostdin','-hide_banner','-v','error','-y','-protocol_whitelist','file','-i',$preview,'-map_metadata','-1','-ac','1','-ar','44100','-c:a','libvorbis','-q:a','4',$ogg)
    try {
        $null = Invoke-AudioTool $Tools.Ffmpeg $decodeArgs
        $sourceMetrics = Measure-Wave $decoded
        $null = Invoke-AudioTool $Tools.Ffmpeg $previewArgs
        $previewMetrics = Measure-Wave $preview
        $null = Invoke-AudioTool $Tools.Ffmpeg $oggArgs
        $oggProbe = Get-Probe $ogg $Tools
        $oggDecode = @('-nostdin','-hide_banner','-v','error','-y','-protocol_whitelist','file','-i',$ogg,'-t','6','-map_metadata','-1','-c:a','pcm_f32le',$decoded)
        $null = Invoke-AudioTool $Tools.Ffmpeg $oggDecode
        $oggMetrics = Measure-Wave $decoded
        $versionOutput = Invoke-AudioTool $Tools.Ffmpeg @('-version')
        Require-Sound ($versionOutput -match '^ffmpeg version ([A-Za-z0-9._+-]+)') 'Cannot identify FFmpeg version.'
        $version = $Matches[1]
        $report = [ordered]@{ schema = 'grindless.sound-calibration-qc/v1'; candidate_id = $Id
            licence = 'INTERNAL_CALIBRATION_ONLY'; import_approved = $false; publication_approved = $false
            listening_pass = $false; source_sha256 = (Get-SoundHash $Raw); source_probe = $probe; source_metrics = $sourceMetrics
            preview_sha256 = (Get-SoundHash $preview); preview_metrics = $previewMetrics
            ogg_sha256 = (Get-SoundHash $ogg); ogg_probe = $oggProbe; ogg_metrics = $oggMetrics
            transforms = @{ ffmpeg_version = $version; ffmpeg_sha256 = (Get-SoundHash $Tools.Ffmpeg)
                source_decode_arguments = $decodeArgs; preview_arguments = $previewArgs; ogg_arguments = $oggArgs
                note = 'Fixed -3 dB gain, mono 44.1 kHz, PCM16 preview and Vorbis q4; no seam repair, trimming or artistic approval.' } }
        if ($null -ne $Inputs -and (Test-Purpose $Inputs)) {
            $report.schema = 'grindless.sound-purpose-qc/v2'; $report.licence = 'INTERNAL_AUDITION_ONLY'
            $report['purpose_id'] = $Inputs.Manifest.purpose_id; $report['run_id'] = $Inputs.Manifest.run_id
            $candidate = @(Get-Candidates $Inputs | Where-Object { $_.id -ceq $Id })[0]
            $report['request'] = ConvertFrom-SoundJson (Get-Payload $candidate $Inputs.Contract)
            $report['provenance_sha256'] = Get-SoundHash (Join-Path $Output ($Id + '.provenance.json'))
        }
        Write-SoundJson (Join-Path $Output ($Id + '.qc.json')) $report
    } finally { if ([IO.File]::Exists($decoded)) { [IO.File]::Delete($decoded) } }
}
function Write-Audition($Inputs, $Receipt, [string]$Output) {
    $title = 'Quern internal calibration'; $heading = 'Quern: internal calibration only'
    if (Test-Purpose $Inputs) { $title = $Inputs.Manifest.label; $heading = $title + ': internal audition only' }
    $html = '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>' + [Net.WebUtility]::HtmlEncode($title) + '</title><body><h1>' + [Net.WebUtility]::HtmlEncode($heading) + '</h1><p>Not release-approved. Technical metrics are not a listening pass. Choose a direction before expansion. File controls only; no server, scripts or remote resources.</p>'
    if (Test-Purpose $Inputs) {
        $html += '<p>' + [Net.WebUtility]::HtmlEncode(('Purpose: ' + $Inputs.Manifest.purpose_id + '; run: ' + $Inputs.Manifest.run_id + '; model: ' + $Inputs.Contract.body_parameters.model_id)) + '</p>'
    }
    foreach ($candidate in @(Get-Candidates $Inputs)) {
        $html += '<section><h2>' + [Net.WebUtility]::HtmlEncode($candidate.label) + '</h2><p>' + [Net.WebUtility]::HtmlEncode($candidate.difference) + '</p>'
        $attempt = @($Receipt.attempts | Where-Object { $_.candidate_id -ceq $candidate.id -and $_.state -ceq 'response_saved' })
        if ($attempt.Count -eq 1 -and [IO.File]::Exists((Join-Path $Output ($candidate.id + '.qc.json')))) {
            $qc = Read-SoundJson (Join-Path $Output ($candidate.id + '.qc.json'))
            $flags = @(@($qc.source_metrics.flags) + @($qc.preview_metrics.flags) + @($qc.ogg_metrics.flags) | Sort-Object -Unique)
            $screening = 'No threshold flags; listening is still required.'
            if ($flags.Count -gt 0) { $screening = 'Screening flags (not a listening verdict): ' + ($flags -join ', ') }
            $html += '<p>' + [Net.WebUtility]::HtmlEncode($screening) + '</p>'
            $html += '<audio controls loop preload="none" src="' + $candidate.id + '.preview.wav"></audio><p><a href="' + $candidate.id + '.qc.json">Technical QC and transforms</a> | <a href="' + $candidate.id + '.raw.mp3">Unmodified source</a></p>'
        } else { $html += '<p>No technically processed source available; no listening pass claimed.</p>' }
        $html += '</section>'
    }
    [IO.File]::WriteAllText((Join-Path $Output 'audition.html'), $html + '</body></html>', [Text.UTF8Encoding]::new($false))
}
function New-SoundProvenance($Inputs, $Grant, $Attempt, $Candidate) {
    $reviewer = 'Astra'; $use = 'Quern internal audition; runtime playback not integrated'
    $licence = 'INTERNAL_CALIBRATION_ONLY'
    if (Test-Purpose $Inputs) {
        $reviewer = 'Sol'; $use = $Inputs.Manifest.label + '; internal audition only; runtime import not authorized'
        $licence = 'INTERNAL_AUDITION_ONLY'
    }
    $record = [ordered]@{
        provider = 'ElevenLabs'; generated_utc = $Attempt.completed_utc; request_id = $Attempt.request_id
        model = $Inputs.Contract.body_parameters.model_id; request = (ConvertFrom-SoundJson (Get-Payload $Candidate $Inputs.Contract))
        source_sha256 = $Attempt.source_sha256; source_commit = (Get-GrantCommit $Grant)
        manifest_sha256 = $Inputs.ManifestHash; contract_sha256 = $Inputs.ContractHash; tooling_sha256 = $Inputs.ToolingHash
        grant_id = $Grant.id; entitlement = $Grant.entitlement_at_generation; licence = $licence
        creative_terms = 'https://elevenlabs.io/creative-terms'
        rights_review = ('Game incorporation is plan-dependent; standalone audio distribution is not permitted. ' + $reviewer + ' reviews actual release use.')
        intended_use = $use; import_approved = $false; publication_approved = $false }
    if (Test-Purpose $Inputs) {
        $record['schema'] = 'grindless.sound-purpose-provenance/v2'
        $record['purpose_id'] = $Inputs.Manifest.purpose_id; $record['run_id'] = $Inputs.Manifest.run_id
        $record['entitlement_basis'] = $Grant.entitlement_basis
        $record['candidate_id'] = $Candidate.id; $record['label'] = $Candidate.label; $record['difference'] = $Candidate.difference
        foreach ($family in $Inputs.Manifest.families) {
            if (@($family.candidates | ForEach-Object { $_.id }) -ccontains $Candidate.id) {
                $record['family_id'] = $family.id; $record['machines'] = @($family.machines)
            }
        }
    }
    return [pscustomobject]$record
}
function Get-SecretPath([string]$Path, [string]$Output) {
    Require-Sound ([IO.Path]::IsPathRooted($Path)) 'SecretFile must be an absolute local path, never a key argument.'
    $Path = Get-LocalPath $Path
    Require-Sound ([IO.File]::Exists($Path) -and -not (Test-Within $Path $script:SoundRoot) -and -not (Test-Within $Path $Output)) 'SecretFile must be an existing local file outside the repository and candidate folder.'
    Require-Sound (((Get-Item -LiteralPath $Path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'SecretFile must be a regular local file, not a redirected path.'
    return $Path
}

function Send-ProviderAudio([string]$SecretPath, [string]$Payload) {
    # The only secret read and only HTTP call in this tool. Never return/echo headers, key or bodies on error.
    Add-Type -AssemblyName System.Net.Http
    $handler = $null; $client = $null; $request = $null; $response = $null; $cts = $null; $key = $null
    $previousTls = [Net.ServicePointManager]::SecurityProtocol
    try {
        Require-Sound ($null -eq [Net.ServicePointManager]::ServerCertificateValidationCallback) 'A global certificate override is active; refuse provider access.'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $key = [IO.File]::ReadAllText($SecretPath).Trim()
        Require-Sound ($key -cmatch '^[A-Za-z0-9_-]{16,256}$') 'Secret file must contain only one API key; its contents are never reported.'
        $handler = [Net.Http.HttpClientHandler]::new()
        $handler.AllowAutoRedirect = $false; $handler.UseCookies = $false
        $handler.UseProxy = $false
        $handler.UseDefaultCredentials = $false; $handler.Credentials = $null
        $handler.CheckCertificateRevocationList = $true
        $client = [Net.Http.HttpClient]::new($handler)
        $cts = [Threading.CancellationTokenSource]::new(); $cts.CancelAfter(60000)
        $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Post, $script:Endpoint)
        $request.Headers.ConnectionClose = $true; $request.Headers.ExpectContinue = $false
        [void]$request.Headers.TryAddWithoutValidation('xi-api-key', $key)
        $request.Content = [Net.Http.StringContent]::new($Payload, [Text.Encoding]::UTF8, 'application/json')
        $response = $client.SendAsync($request, [Net.Http.HttpCompletionOption]::ResponseHeadersRead, $cts.Token).GetAwaiter().GetResult()
        $status = [int]$response.StatusCode
        $requestId = $null
        foreach ($name in @('request-id','x-request-id')) {
            if ($response.Headers.Contains($name)) {
                $value = @($response.Headers.GetValues($name))[0]
                if ($value -cmatch '^[A-Za-z0-9._-]{1,128}$') { $requestId = $value }
            }
        }
        if ($status -ne 200) { return [pscustomobject]@{ Status = $status; RequestId = $requestId; Bytes = $null } }
        $type = $response.Content.Headers.ContentType.MediaType
        Require-Sound (@('audio/mpeg','audio/mp3','application/octet-stream') -ccontains $type) 'Unexpected successful response type; delivery requires review.'
        $memory = [IO.MemoryStream]::new(); $audioStream = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
        try {
            $buffer = New-Object byte[] 8192
            while (($read = $audioStream.ReadAsync($buffer, 0, $buffer.Length, $cts.Token).GetAwaiter().GetResult()) -gt 0) {
                Require-Sound ($memory.Length + $read -le 2097152) 'Response exceeded the audio size bound; delivery requires review.'
                $memory.Write($buffer, 0, $read)
            }
            $bytes = $memory.ToArray()
            Require-Sound ($bytes.Length -gt 3 -and (($bytes[0] -eq 73 -and $bytes[1] -eq 68 -and $bytes[2] -eq 51) -or ($bytes[0] -eq 255 -and ($bytes[1] -band 224) -eq 224))) 'Successful response is not recognizable MP3; delivery requires review.'
            return [pscustomobject]@{ Status = $status; RequestId = $requestId; Bytes = $bytes }
        } finally { $audioStream.Dispose(); $memory.Dispose() }
    } catch { Stop-Sound 'SD_DELIVERY_UNKNOWN' 'Provider delivery is ambiguous or locally rejected. Review the receipt; do not retry. No raw exception/body/header was recorded.' }
    finally {
        $key = $null
        foreach ($item in @($response,$request,$client,$handler,$cts)) { if ($null -ne $item) { $item.Dispose() } }
        [Net.ServicePointManager]::SecurityProtocol = $previousTls
    }
}
function Invoke-Generate($Inputs, [string]$Manifest, [string]$Contract, [string]$GrantPath, [string]$Output, [string]$SecretPath, $Tools) {
    $grant = Read-SoundJson $GrantPath; $grantHash = Get-SoundHash $GrantPath
    Assert-Grant $grant $Inputs $Output (Get-GitHead)
    Assert-Checkpoint $Manifest $Contract
    Require-Sound ((Get-OutputPath $Output) -ceq $Output) 'Use the exact canonical granted output path.'
    $SecretPath = Get-SecretPath $SecretPath $Output
    [void][IO.Directory]::CreateDirectory($Output)
    $lock = Open-SoundLock $Output; $receiptPath = Join-Path $Output 'receipt.json'
    try {
        if (-not [IO.File]::Exists($receiptPath)) {
            Require-Sound (@(Get-ChildItem -LiteralPath $Output -Force | Where-Object { $_.Name -cne 'calibration.lock' }).Count -eq 0) 'Missing receipt in a nonempty folder: stop for review, never reset the budget.'
            Write-SoundJson $receiptPath (New-Receipt $grant $Inputs $grantHash $Output)
        }
        $receipt = Read-SoundJson $receiptPath; Assert-Receipt $receipt $Inputs $Output
        Assert-ReceiptGrant $receipt $grant $grantHash $Inputs
        Resolve-Interrupted $receiptPath $receipt
        foreach ($candidate in @(Get-Candidates $Inputs)) {
            if (@($receipt.attempts | Where-Object { $_.candidate_id -ceq $candidate.id -and $_.state -ceq 'response_saved' }).Count -eq 1) { continue }
            Assert-CanAttempt $receipt
            Assert-Checkpoint $Manifest $Contract
            $current = Get-Inputs $Manifest $Contract
            Require-Sound ((Get-SoundHash $GrantPath) -ceq $grantHash) 'Grant changed during execution.'
            Assert-Grant $grant $current $Output (Get-GitHead)
            $receipt = Reserve-Attempt $receiptPath $receipt $current $Output $candidate.id
            $attempt = $receipt.attempts[-1]
            try {
                $result = Send-ProviderAudio $SecretPath (Get-Payload $candidate $Inputs.Contract)
                $attempt.http_status = $result.Status; $attempt.request_id = $result.RequestId
                $attempt.completed_utc = [DateTime]::UtcNow.ToString('o')
                if ($result.Status -ne 200) {
                    $attempt.state = 'http_error'; $attempt.failure_code = 'http_status_' + $result.Status
                    Write-SoundJson $receiptPath $receipt
                    Stop-Sound 'SD_HTTP' ('Provider returned HTTP ' + $result.Status + '. Attempt consumed; review before any further action, never retry automatically.')
                }
                $rawPath = Join-Path $Output ($candidate.id + '.raw.mp3')
                $raw = [IO.FileStream]::new($rawPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
                try { $raw.Write($result.Bytes, 0, $result.Bytes.Length); $raw.Flush($true) } finally { $raw.Dispose() }
                $attempt.raw_file = $candidate.id + '.raw.mp3'; $attempt.source_sha256 = Get-SoundHash $rawPath
                Write-SoundJson (Join-Path $Output ($candidate.id + '.provenance.json')) (New-SoundProvenance $Inputs $grant $attempt $candidate)
                $attempt.state = 'response_saved'; Write-SoundJson $receiptPath $receipt
            } catch {
                if ($attempt.state -ceq 'attempting') {
                    $attempt.state = 'needs_review'; $attempt.failure_code = 'delivery_or_storage_unknown'
                    Write-SoundJson $receiptPath $receipt
                }
                throw
            }
        }
    } finally { $lock.Dispose() }
    Invoke-Qc $Inputs $Output $Tools
}
function Invoke-Qc($Inputs, [string]$Output, $Tools) {
    Require-Sound ([IO.Directory]::Exists($Output)) 'Candidate folder does not exist.'
    $lock = Open-SoundLock $Output
    try {
        $path = Join-Path $Output 'receipt.json'; $receipt = Read-SoundJson $path
        Assert-Receipt $receipt $Inputs $Output
        Resolve-Interrupted $path $receipt
        $saved = @($receipt.attempts | Where-Object { $_.state -ceq 'response_saved' })
        Require-Sound ($saved.Count -gt 0) 'No saved source is available for QC. No generation or listening pass is implied.'
        foreach ($attempt in $saved) { Invoke-FileQc (Join-Path $Output $attempt.raw_file) $attempt.candidate_id $Output $Tools $Inputs }
        Write-Audition $Inputs $receipt $Output
        return [pscustomobject]@{ mode = 'QC'; saved_sources = $saved.Count; audition = (Join-Path $Output 'audition.html')
            listening_pass = $false; import_approved = $false; publication_approved = $false }
    } finally { $lock.Dispose() }
}
function Get-SoundPlan($Inputs, $Tools) {
    $plan = [ordered]@{ mode = 'Plan'; network = $false; secret_read = $false; generation_authorized = $false
        manifest_sha256 = $Inputs.ManifestHash; contract_sha256 = $Inputs.ContractHash; tooling_sha256 = $Inputs.ToolingHash
        source_commit = (Get-GitHead); max_posts = (Get-CallLimit $Inputs); retries = 0; tools = $Tools
        grant_gate = 'NEW Astra grant after reviewed checkpoint; paid entitlement is owner-confirmed, not account-queried.'
        requests = @(Get-Candidates $Inputs | ForEach-Object {
            [ordered]@{ id = $_.id; label = $_.label; text_characters = $_.prompt.Length
                method = 'POST'; endpoint = $script:Endpoint; body = (ConvertFrom-SoundJson (Get-Payload $_ $Inputs.Contract)) }
        }) }
    if (Test-Purpose $Inputs) {
        $plan['schema'] = 'grindless.sound-purpose-plan/v2'
        $plan['purpose_id'] = $Inputs.Manifest.purpose_id; $plan['run_id'] = $Inputs.Manifest.run_id
        $plan['source_revision'] = $Inputs.Manifest.source_revision
        $plan['manifest_review_status'] = $Inputs.Manifest.review_status; $plan['contract_review_status'] = $Inputs.Contract.review_status
        $plan['families'] = @($Inputs.Manifest.families | ForEach-Object {
            [ordered]@{ id = $_.id; machines = @($_.machines); candidate_ids = @($_.candidates | ForEach-Object { $_.id }) }
        })
        $plan['wave_ceiling_not_granted'] = $Inputs.Contract.wave_limits
        $plan.grant_gate = 'UNAPPROVED until Sol reviews/checkpoints contract, manifest and runner and issues a NEW private purpose-bound grant. Existing entitlement is an owner attestation; no account query.'
    }
    return [pscustomobject]$plan
}

if ($MyInvocation.InvocationName -ne '.') {
    try {
        Require-Sound ($Mode -eq 'Generate' -or -not $SecretFile) 'SecretFile is only accepted in explicit Generate mode.'
        $inputs = Get-Inputs $ManifestPath $ContractPath
        switch ($Mode) {
            'Plan' {
                if ($GrantFile) {
                    $grant = Read-SoundJson $GrantFile; Assert-GrantShape $grant
                    Require-Sound ((Test-Purpose $inputs) -eq ($grant.schema -ceq 'grindless.sound-purpose-grant/v2')) 'Plan grant and manifest schema versions must agree.'
                }
                Get-SoundPlan $inputs (Get-SoundTools $FfmpegPath $FfprobePath) | ConvertTo-Json -Depth 12
            }
            'Generate' {
                $output = Get-OutputPath $OutputDirectory
                # An unapproved purpose fails its real guard before tool preflight,
                # secret-path inspection, output creation or any possible HTTP.
                Assert-Grant (Read-SoundJson $GrantFile) $inputs $output (Get-GitHead)
                Assert-Checkpoint $ManifestPath $ContractPath
                $tools = Get-SoundTools $FfmpegPath $FfprobePath $true
                Invoke-Generate $inputs $ManifestPath $ContractPath $GrantFile $output $SecretFile $tools | ConvertTo-Json -Depth 8
            }
            'QC' {
                $tools = Get-SoundTools $FfmpegPath $FfprobePath $true
                Invoke-Qc $inputs (Get-OutputPath $OutputDirectory) $tools | ConvertTo-Json -Depth 8
            }
        }
    } catch {
        [Console]::Error.WriteLine($script:SafeFailure)
        exit 1
    }
}
