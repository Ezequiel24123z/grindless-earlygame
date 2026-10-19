[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'sound-design.ps1')
$script:FlowChecks = 0
$script:ProductionWriteJson = ${function:Write-SoundJson}
$script:ProductionAssertGrant = ${function:Assert-Grant}
$script:FlowInputs = Get-Inputs $ManifestPath $ContractPath
$script:FlowHead = Get-GitHead

function Check-Flow([bool]$Condition, [string]$Label) {
    if (-not $Condition) { throw ('TEST flow failed: ' + $Label) }
    $script:FlowChecks++
}
function Reject-Flow([scriptblock]$Action, [string]$Code, [string]$Label) {
    $message = $null
    try { $null = & $Action } catch { $message = $_.Exception.Message }
    Check-Flow ($null -ne $message -and $message.StartsWith($Code + ': ')) $Label
}

# Only the orchestration is under test; real grant/checkpoint guards have their own suite.
function Assert-Checkpoint([string]$Manifest, [string]$Contract) {
    Check-Flow ($Manifest -ceq $ManifestPath -and $Contract -ceq $ContractPath) 'flow keeps the real manifest/contract paths'
}
function Assert-Grant($Grant, $Inputs, [string]$Output, [string]$Head) {
    Assert-GrantShape $Grant
    Check-Flow ((Test-Bool $Grant.generation_approved $false) -and $null -eq $Grant.approved_by -and
        $Grant.authorization_reference -ceq 'TEST OFFLINE FLOW - NOT AUTHORIZATION') 'substitute accepts only an explicitly unauthorized TEST marker'
    Check-Flow ($Grant.output_directory -ceq $script:FlowOutput -and $Output -ceq $script:FlowOutput -and
        $Head -ceq $script:FlowHead -and $Grant.tooling_sha256 -ceq $Inputs.ToolingHash) 'TEST marker stays bound to this fixture/source'
}
function Send-ProviderAudio([string]$SecretPath, [string]$Payload) {
    $receipt = Read-SoundJson $script:FlowReceiptPath
    Assert-Receipt $receipt $script:FlowInputs $script:FlowOutput
    $attempt = $receipt.attempts[-1]
    $body = ConvertFrom-SoundJson $Payload
    $candidate = $script:FlowInputs.Manifest.calibration.candidates[$attempt.ordinal - 1]
    Check-Flow ($attempt.state -ceq 'attempting' -and $attempt.candidate_id -ceq $candidate.id) 'transport sees the exact durable reservation before delivery'
    Check-Flow ($body.text -ceq $candidate.prompt) 'reserved candidate matches the submitted payload'
    Check-Flow ($SecretPath -ceq $script:FlowSentinel -and (Get-Item -LiteralPath $SecretPath).Length -eq 0) 'transport receives only the empty TEST path, never reads a key'
    $script:SimulatedCalls++
    if ($attempt.ordinal -eq $script:FlowFaultOrdinal) {
        if ($script:FlowFault -ceq 'ambiguous') { Stop-Sound 'SD_DELIVERY_UNKNOWN' 'TEST simulated ambiguous delivery; no HTTP occurred.' }
        if ($script:FlowFault -ceq 'http429') {
            return [pscustomobject]@{ Status = 429; RequestId = 'TEST-rate-limit'; Bytes = $null }
        }
        if ($script:FlowFault -ceq 'raw_collision') {
            [IO.File]::WriteAllText((Join-Path $script:FlowOutput ($candidate.id + '.raw.mp3')), 'TEST preexisting raw; do not overwrite')
        }
    }
    return [pscustomobject]@{ Status = 200; RequestId = 'TEST-no-provider'
        Bytes = [Text.Encoding]::ASCII.GetBytes('TEST FAKE MP3 - NOT AUDIO - NEVER SHIP') }
}
function Invoke-Qc($Inputs, [string]$Output, $Tools) {
    $receipt = Read-SoundJson (Join-Path $Output 'receipt.json')
    Assert-Receipt $receipt $Inputs $Output
    Check-Flow (@($receipt.attempts).Count -eq 3 -and
        @($receipt.attempts | Where-Object { $_.state -cne 'response_saved' }).Count -eq 0) 'codec substitute is reached only after all TEST responses are saved'
    $script:SimulatedQcCalls++
}
function Write-SoundJson([string]$Path, $Value) {
    $fault = $false
    if ($script:StorageFaultArmed) {
        if ($Path -ceq $script:FlowReceiptPath -and @($Value.attempts).Count -eq $script:FlowFaultOrdinal) {
            $state = $Value.attempts[-1].state
            $fault = (($script:FlowFault -in @('reservation_before_write','reservation_after_write') -and $state -ceq 'attempting') -or
                ($script:FlowFault -ceq 'saved_receipt' -and $state -ceq 'response_saved'))
        } elseif ($script:FlowFault -ceq 'provenance' -and
            $Path -ceq (Join-Path $script:FlowOutput ($script:CandidateIds[$script:FlowFaultOrdinal - 1] + '.provenance.json'))) {
            $fault = $true
        }
    }
    if ($fault -and $script:FlowFault -cne 'reservation_after_write') {
        $script:StorageFaultArmed = $false
        Stop-Sound 'SD_TEST_STORAGE' 'TEST injected write failure; no real candidate directory is used.'
    }
    & $script:ProductionWriteJson $Path $Value
    if ($fault) {
        $script:StorageFaultArmed = $false
        Stop-Sound 'SD_TEST_STORAGE' 'TEST interruption after durable reservation; no transport was invoked.'
    }
}
function New-FlowCase([string]$Name, [string]$Fault = 'none', [int]$Ordinal = 1) {
    $script:FlowOutput = Join-Path $script:FlowRoot ('TEST-' + $Name)
    [void][IO.Directory]::CreateDirectory($script:FlowOutput)
    $script:FlowReceiptPath = Join-Path $script:FlowOutput 'receipt.json'
    $script:FlowGrantPath = Join-Path $script:FlowRoot ('TEST-' + $Name + '-NOT-AUTHORIZATION.json')
    $script:FlowFault = $Fault; $script:FlowFaultOrdinal = $Ordinal
    $script:StorageFaultArmed = $Fault -in @('reservation_before_write','reservation_after_write','provenance','saved_receipt')
    $script:SimulatedCalls = 0; $script:SimulatedQcCalls = 0
    $grant = Read-SoundJson (Join-Path $PSScriptRoot 'grant.example.json')
    $grant.id = 'test-flow-not-authorization'
    $grant.authorization_reference = 'TEST OFFLINE FLOW - NOT AUTHORIZATION'
    $grant.manifest_commit = $script:FlowHead; $grant.contract_commit = $script:FlowHead; $grant.tooling_commit = $script:FlowHead
    $grant.manifest_sha256 = $script:FlowInputs.ManifestHash; $grant.contract_sha256 = $script:FlowInputs.ContractHash
    $grant.tooling_sha256 = $script:FlowInputs.ToolingHash; $grant.output_directory = $script:FlowOutput
    Reject-Flow { & $script:ProductionAssertGrant $grant $script:FlowInputs $script:FlowOutput $script:FlowHead } 'SD_VALIDATION' 'TEST marker is rejected by the real authorization guard'
    & $script:ProductionWriteJson $script:FlowGrantPath $grant
}
function Invoke-TestFlow {
    Invoke-Generate $script:FlowInputs $ManifestPath $ContractPath $script:FlowGrantPath $script:FlowOutput $script:FlowSentinel $null
}

$script:FlowRoot = Join-Path ([IO.Path]::GetTempPath()) ('GrindlessSoundFlow-TEST-not-art-' + [Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($script:FlowRoot)
$script:FlowSentinel = Join-Path $script:FlowRoot 'TEST-empty-path-NOT-A-KEY.txt'
[IO.File]::WriteAllText($script:FlowSentinel, '')
try {
    New-FlowCase 'success'
    Invoke-TestFlow
    $receipt = Read-SoundJson $script:FlowReceiptPath
    Check-Flow ($script:SimulatedCalls -eq 3 -and $script:SimulatedQcCalls -eq 1) 'three simulated responses reach the codec substitute once'
    Check-Flow (($receipt.attempts.candidate_id -join ',') -ceq ($script:CandidateIds -join ',') -and
        ($receipt.attempts.state -join ',') -ceq 'response_saved,response_saved,response_saved') 'actual orchestration saves ordered response states'
    foreach ($attempt in $receipt.attempts) {
        $provenance = Read-SoundJson (Join-Path $script:FlowOutput ($attempt.candidate_id + '.provenance.json'))
        Check-Flow ($provenance.source_sha256 -ceq $attempt.source_sha256 -and $provenance.source_commit -ceq $script:FlowHead -and
            $provenance.grant_id -ceq $receipt.grant_id -and -not $provenance.import_approved -and -not $provenance.publication_approved) 'flow binds saved TEST response to non-import provenance'
    }
    Invoke-TestFlow
    Check-Flow ($script:SimulatedCalls -eq 3 -and $script:SimulatedQcCalls -eq 2) 'reentry into a completed ledger never repeats transport'

    $failures = @(
        @{ Name = 'ambiguous-first'; Fault = 'ambiguous'; Ordinal = 1; Code = 'SD_DELIVERY_UNKNOWN'; Calls = 1; States = 'needs_review' },
        @{ Name = 'ambiguous-after-success'; Fault = 'ambiguous'; Ordinal = 2; Code = 'SD_DELIVERY_UNKNOWN'; Calls = 2; States = 'response_saved,needs_review' },
        @{ Name = 'http429'; Fault = 'http429'; Ordinal = 1; Code = 'SD_HTTP'; Calls = 1; States = 'http_error' },
        @{ Name = 'reservation-after-write'; Fault = 'reservation_after_write'; Ordinal = 1; Code = 'SD_TEST_STORAGE'; Calls = 0; States = 'attempting' },
        @{ Name = 'provenance'; Fault = 'provenance'; Ordinal = 1; Code = 'SD_TEST_STORAGE'; Calls = 1; States = 'needs_review' },
        @{ Name = 'saved-receipt'; Fault = 'saved_receipt'; Ordinal = 1; Code = 'SD_TEST_STORAGE'; Calls = 1; States = 'attempting' },
        @{ Name = 'raw-collision'; Fault = 'raw_collision'; Ordinal = 1; Code = 'SD_VALIDATION'; Calls = 1; States = 'needs_review' }
    )
    foreach ($case in $failures) {
        New-FlowCase $case.Name $case.Fault $case.Ordinal
        if ($case.Fault -ceq 'raw_collision') {
            # CreateNew throws a local I/O error, not an SD transport diagnostic.
            $failed = $false
            try { Invoke-TestFlow } catch { $failed = $_.Exception -is [Management.Automation.MethodInvocationException] }
            Check-Flow $failed 'raw collision remains an explicit local storage failure'
        } else {
            Reject-Flow { Invoke-TestFlow } $case.Code ('flow reports ' + $case.Name)
        }
        $receipt = Read-SoundJson $script:FlowReceiptPath
        Assert-Receipt $receipt $script:FlowInputs $script:FlowOutput
        Check-Flow (($receipt.attempts.state -join ',') -ceq $case.States -and
            $script:SimulatedCalls -eq $case.Calls -and $script:SimulatedQcCalls -eq 0) ($case.Name + ' retains charged/uncertain state without processing')
        $beforeCount = @($receipt.attempts).Count
        $script:FlowFault = 'none'; $script:StorageFaultArmed = $false
        Reject-Flow { Invoke-TestFlow } 'SD_VALIDATION' ($case.Name + ' restart refuses another request')
        $recovered = Read-SoundJson $script:FlowReceiptPath
        $expected = $case.States -replace 'attempting', 'needs_review'
        Check-Flow (($recovered.attempts.state -join ',') -ceq $expected -and
            @($recovered.attempts).Count -eq $beforeCount -and $script:SimulatedCalls -eq $case.Calls -and
            $script:SimulatedQcCalls -eq 0) ($case.Name + ' restart preserves budget and stops before transport')
        if ($case.Fault -in @('provenance','saved_receipt','raw_collision')) {
            Check-Flow ([IO.File]::Exists((Join-Path $script:FlowOutput 'quern-a.raw.mp3'))) ($case.Name + ' retains the response/collision artifact for review')
        }
        if ($case.Fault -ceq 'saved_receipt') {
            Check-Flow ([IO.File]::Exists((Join-Path $script:FlowOutput 'quern-a.provenance.json'))) 'receipt failure does not delete already written provenance'
        }
        if ($case.Fault -ceq 'http429') {
            Check-Flow ($recovered.attempts[0].http_status -eq 429 -and $recovered.attempts[0].failure_code -ceq 'http_status_429') 'HTTP failure keeps its exact status without response-body disclosure'
        }
        if ($case.Fault -ceq 'raw_collision') {
            Check-Flow ([IO.File]::ReadAllText((Join-Path $script:FlowOutput 'quern-a.raw.mp3')) -ceq 'TEST preexisting raw; do not overwrite') 'raw collision never overwrites the preexisting artifact'
        }
    }

    New-FlowCase 'reservation-before-write' 'reservation_before_write'
    Reject-Flow { Invoke-TestFlow } 'SD_TEST_STORAGE' 'failed reservation write stops before transport'
    $receipt = Read-SoundJson $script:FlowReceiptPath
    Check-Flow (@($receipt.attempts).Count -eq 0 -and $script:SimulatedCalls -eq 0) 'failed uncommitted reservation has no provider delivery'
    $script:FlowFault = 'none'
    Invoke-TestFlow
    Check-Flow ($script:SimulatedCalls -eq 3 -and $script:SimulatedQcCalls -eq 1) 'only a never-persisted never-sent reservation can continue'

    New-FlowCase 'missing-ledger'
    [IO.File]::WriteAllText((Join-Path $script:FlowOutput 'TEST-existing-artifact.txt'), 'TEST preserve this directory')
    Reject-Flow { Invoke-TestFlow } 'SD_VALIDATION' 'nonempty folder cannot start a replacement ledger'
    Check-Flow ($script:SimulatedCalls -eq 0 -and -not [IO.File]::Exists($script:FlowReceiptPath)) 'missing-ledger refusal spends nothing and does not reset evidence'
    Check-Flow ((Get-Item -LiteralPath $script:FlowSentinel).Length -eq 0) 'TEST sentinel remains empty; no credential was supplied'
    Write-Output ('PASS: ' + $script:FlowChecks + ' offline generation-flow assertions across 10 isolated TEST cases; zero provider requests, credential reads, codecs or JVMs.')
} finally {
    if ([IO.Directory]::Exists($script:FlowRoot)) { Remove-Item -LiteralPath $script:FlowRoot -Recurse -Force }
}
