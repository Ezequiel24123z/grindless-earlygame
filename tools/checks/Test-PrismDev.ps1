#requires -Version 5.1
<#
Isolated Windows PowerShell fixtures. No JVM, SSH, network, real launcher instance,
Git write, Gradle cache, account or save is accessed. Only OS/Git/download/transport
boundaries are substituted in-process; the production CLI exposes no bypass flags.
#>
[CmdletBinding()]
param([string]$RepositoryRoot, [ValidateSet('All', 'QuestDefinitions')][string]$Suite = 'All')

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
if (-not $RepositoryRoot) { $RepositoryRoot = Join-Path $PSScriptRoot '../..' }
$RepositoryRoot = [IO.Path]::GetFullPath($RepositoryRoot)
$script:FixtureSourceRoot = $RepositoryRoot
. (Join-Path $RepositoryRoot 'tools/prism-dev.ps1')
. (Join-Path $RepositoryRoot 'tools/pc-workflow.ps1')
$script:Assertions = 0
$script:Cases = 0
$script:DownloadCalls = 0
$script:OriginalReplacement = ${function:Set-PrismReplacement}
$script:OriginalTransport = ${function:Invoke-PcWorkflowTransport}
$script:OriginalByteWriter = ${function:Write-PrismNewBytes}
$script:StagesInsideScan = 0
$script:TestTemp = Join-Path ([IO.Path]::GetTempPath()) ('grindless-prism-tests-' + [Guid]::NewGuid().ToString('N'))
$script:Junctions = @()
New-Item -ItemType Directory -Path $script:TestTemp | Out-Null
Add-Type -AssemblyName System.IO.Compression

function Assert-Test([bool]$Condition, [string]$Label) {
    if (-not $Condition) { throw "ASSERTION FAILED: $Label" }
    $script:Assertions++
}

function Assert-Refused([scriptblock]$Body, [string]$Message, [string]$Label) {
    $caught = $null
    try { $null = & $Body } catch { $caught = $_.Exception.Message }
    Assert-Test ($null -ne $caught) "$Label must refuse"
    Assert-Test ($caught -like ('*' + $Message + '*')) "$Label refusal must identify the cause (actual: $caught)"
    Assert-Test (-not $caught.Contains('TOKEN_DO_NOT_PRINT')) "$Label must not disclose Java arguments"
}

function Write-FixtureText([string]$Path, [string]$Text) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($Path)) | Out-Null
    [IO.File]::WriteAllText($Path, $Text, (New-Object Text.UTF8Encoding($false)))
}

function Write-FixtureJson([string]$Path, $Value) {
    Write-FixtureText $Path ((ConvertTo-Json $Value -Depth 40) + "`n")
}

function Get-FixtureSnapshot([string]$Path) {
    if (-not [IO.Directory]::Exists($Path)) { return '<absent>' }
    return (@(Get-ChildItem -LiteralPath $Path -Recurse -Force -File | Sort-Object FullName | ForEach-Object {
        $_.FullName.Substring($Path.Length) + ':' + $_.Length + ':' + $_.LastWriteTimeUtc.Ticks + ':' +
            (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    }) -join "`n")
}

function New-Fixture {
    $script:Cases++
    $caseDir = Join-Path $script:TestTemp ('case-' + $script:Cases)
    $repo = Join-Path $caseDir 'source'
    $instances = Join-Path $caseDir 'Prism/instances'
    $template = Join-Path $instances 'OldTemplate'
    foreach ($directory in @($repo, $instances, $template)) { [IO.Directory]::CreateDirectory($directory) | Out-Null }
    foreach ($relative in @('gradle.properties', 'pack/curseforge/manifest.json', 'pack/profiles.json',
            'pack/dependency-metadata.json', 'pack/public-overrides.json')) {
        Write-FixtureText (Join-Path $repo $relative) ([IO.File]::ReadAllText((Join-Path $script:FixtureSourceRoot $relative)))
    }
    foreach ($relative in @('pack/README.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md')) {
        Write-FixtureText (Join-Path $repo $relative) ('public fixture ' + $relative + "`n")
    }
    $build = "// fixture application inputs`n// Application build inputs stay above this boundary`n// pack inputs`n"
    Write-FixtureText (Join-Path $repo 'build.gradle') $build
    Write-FixtureText (Join-Path $repo 'gradlew.bat') '@exit /b 91'
    $metadata = Read-PrismJson (Join-Path $repo 'pack/dependency-metadata.json')
    $pinBytes = @{}
    foreach ($pin in $metadata.files) {
        if ($pin.projectID -notin $script:PrismDevProjects) { continue }
        $bytes = [Text.Encoding]::UTF8.GetBytes(('synthetic official pin fixture ' + $pin.projectID + "`n"))
        $pinBytes[$pin.fileName] = $bytes
        $destination = Join-Path $template ('.minecraft/mods/' + $pin.fileName)
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
        [IO.File]::WriteAllBytes($destination, $bytes)
        $pin.bytes = $bytes.Length
        $pin.sha1 = (Get-FileHash -LiteralPath $destination -Algorithm SHA1).Hash.ToLowerInvariant()
        if ($null -ne $pin.PSObject.Properties['sha256']) {
            $pin.sha256 = Get-PrismHash $destination
        }
    }
    Write-FixtureJson (Join-Path $repo 'pack/dependency-metadata.json') $metadata
    $properties = Get-PrismProperties $repo
    Write-FixtureJson (Join-Path $template 'mmc-pack.json') ([ordered]@{ formatVersion = 1; components = @(
        @{ uid = 'org.lwjgl3'; version = '3.3.1'; dependencyOnly = $true
            cachedName = 'LWJGL3'; cachedVersion = '3.3.1'; cachedVolatile = $true },
        @{ uid = 'net.minecraft'; version = $properties.minecraft_version; important = $true;
            cachedVersion = $properties.minecraft_version; cachedRequires = @(@{ uid = 'org.lwjgl3'; suggests = '3.3.1' }) },
        @{ uid = 'net.minecraftforge'; version = $properties.forge_version.Substring($properties.minecraft_version.Length + 1)
            cachedVersion = $properties.forge_version.Substring($properties.minecraft_version.Length + 1)
            cachedRequires = @(@{ uid = 'net.minecraft'; equals = $properties.minecraft_version }) }
    ) })
    foreach ($relative in @('.minecraft/saves/exploratory/level.dat', '.minecraft/config/private.cfg',
            '.minecraft/options.txt', 'accounts.json', 'instance.cfg', '.index/private-metadata')) {
        Write-FixtureText (Join-Path $template $relative) ('PRIVATE SYNTHETIC SENTINEL ' + $relative)
    }
    $javaHome = Join-Path $caseDir 'jdk 17'
    $java = Join-Path $javaHome 'bin/javaw.exe'
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($java)) | Out-Null
    # Synthetic PE headers, never executable code and never launched.
    $pe = New-Object byte[] 256
    $pe[0] = 0x4D; $pe[1] = 0x5A; $pe[0x3C] = 0x80
    $pe[0x80] = 0x50; $pe[0x81] = 0x45; $pe[0x84] = 0x64; $pe[0x85] = 0x86
    [IO.File]::WriteAllBytes($java, $pe)
    [IO.File]::WriteAllBytes((Join-Path $javaHome 'bin/java.exe'), $pe)
    Write-FixtureText (Join-Path $javaHome 'release') "JAVA_VERSION=`"17.0.1`"`nOS_ARCH=`"x86_64`"`n"
    $fixture = [pscustomobject]@{
        Home = $caseDir; Root = $repo; Instances = $instances; Template = $template
        Target = (Join-Path $instances 'Grindless-T0-Dev'); InstanceId = 'Grindless-T0-Dev'
        JavaHome = $javaHome; Java = $java; JavaHash = (Get-PrismHash $java)
        Source = ('1' * 40); Build = $build; Dirty = $false; RuntimeDrift = $false
        Processes = @(); InspectionFailure = $false; GitCalls = @(); PinBytes = $pinBytes
        Zip = (Join-Path $caseDir 'selected playable.zip'); AllowDownload = $false
        BadDownload = $false; TargetDuringDownload = $false
        BuildLaunches = 0; ResourceReads = 0; GradleArguments = @()
        Resources = [pscustomobject]@{ AvailablePhysicalMiB = 16384; AvailableCommitMiB = 32768
            WorkDisks = @([pscustomobject]@{ AvailableBytes = 64GB }) }
    }
    $script:CurrentFixture = $fixture
    New-FixtureZip $fixture ([Text.Encoding]::UTF8.GetBytes('synthetic remapped Grindless revision one'))
    return $fixture
}

function New-FixtureZip($Fixture, [byte[]]$Jar, [scriptblock]$Change = {}, [string]$Extra = '',
        [switch]$BadJar, [switch]$Symlink) {
    $contract = Get-PrismContract $Fixture.Root
    $jarName = "$($contract.Properties.archives_base_name)-$($contract.Properties.mod_version)-forge.jar"
    $provenance = [ordered]@{
        sourceRevision = $Fixture.Source; runtimeSourceRevision = $Fixture.Source
        profile = 'playable'; status = $script:PrismDevStatus; inputProfile = $contract.Profile
        pinAuthority = 'pack/curseforge/manifest.json'; minecraft = $contract.Properties.minecraft_version
        forge = $contract.Properties.forge_version; java = $contract.Properties.java_version
        grindlessJar = $jarName; grindlessSha256 = (Get-PrismBytesHash $Jar)
        dependencies = $contract.Dependencies
        dependencyMetadataEndpoint = $contract.Metadata.metadataEndpoint
        dependencyPublisherEndpoint = $contract.Metadata.publisherEndpoint; worldsIncluded = $false
        configuration = 'Mod defaults; no private launcher settings or gameplay overrides.'
        configurationPolicy = @{ base = 'upstream-defaults'; overriddenDestinations = @() }
        publicOverrides = @{ manifest = 'pack/public-overrides.json'; sourceRoot = 'pack/public-overrides'; files = @() }
    }
    $payload = [pscustomobject]@{ Manifest = $contract.Manifest; Provenance = $provenance }
    & $Change $payload
    $content = [ordered]@{
        'manifest.json' = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json $payload.Manifest -Depth 40))
        'provenance.json' = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json $payload.Provenance -Depth 40))
        'README.md' = [IO.File]::ReadAllBytes((Join-Path $Fixture.Root 'pack/README.md'))
        'LICENSE' = [IO.File]::ReadAllBytes((Join-Path $Fixture.Root 'LICENSE'))
        'THIRD_PARTY_NOTICES.md' = [IO.File]::ReadAllBytes((Join-Path $Fixture.Root 'THIRD_PARTY_NOTICES.md'))
        ('overrides/mods/' + $jarName) = $Jar
    }
    if ($BadJar) { $content['overrides/mods/' + $jarName] = [Text.Encoding]::UTF8.GetBytes('tampered runtime') }
    if ($Extra) { $content[$Extra] = [Text.Encoding]::UTF8.GetBytes('unexpected file') }
    if ([IO.File]::Exists($Fixture.Zip)) { [IO.File]::Delete($Fixture.Zip) }
    $stream = [IO.File]::Open($Fixture.Zip, [IO.FileMode]::CreateNew)
    $zip = New-Object IO.Compression.ZipArchive($stream, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entry in $content.GetEnumerator()) {
            $item = $zip.CreateEntry($entry.Key)
            if ($Symlink -and $entry.Key -ceq 'LICENSE') { $item.ExternalAttributes = -1577123840 }
            $out = $item.Open()
            try { $out.Write($entry.Value, 0, $entry.Value.Length) } finally { $out.Dispose() }
        }
    } finally { $zip.Dispose(); $stream.Dispose() }
}

function Invoke-PrismGit([string]$Root, [string[]]$Arguments) {
    $f = $script:CurrentFixture
    if ($Root -cne $f.Root) { throw 'TEST: Git escaped the isolated fixture.' }
    $f.GitCalls += ,$Arguments
    switch ($Arguments[0]) {
        'rev-parse' {
            if ($Arguments[1] -eq '--show-toplevel') { return $f.Root }
            return $f.Source
        }
        'status' { if ($f.Dirty) { return ' M dirty-fixture' }; return '' }
        'merge-base' { if ($f.RuntimeDrift) { throw 'Runtime ancestry verification failed.' }; return '' }
        'diff' { if ($f.RuntimeDrift) { throw 'Runtime application inputs differ.' }; return '' }
        'show' { return $f.Build }
        default { throw 'TEST: unexpected Git operation.' }
    }
}

function Get-PrismJavaProcesses {
    if ($script:CurrentFixture.InspectionFailure) { throw 'Cannot inspect Java process safety state; deployment is refused.' }
    return $script:CurrentFixture.Processes
}

function Save-PrismOfficialPin($Pin, [string]$Destination) {
    $script:DownloadCalls++
    $f = $script:CurrentFixture
    if (-not $f.AllowDownload) { throw 'Fixture official CDN failure (no network used).' }
    $bytes = $f.PinBytes[$Pin.fileName]
    if ($f.BadDownload) { $bytes = [Text.Encoding]::UTF8.GetBytes('corrupted CDN fixture') }
    [IO.File]::WriteAllBytes($Destination, $bytes)
    if ($f.TargetDuringDownload) {
        [IO.Directory]::CreateDirectory($f.Target) | Out-Null
        Write-FixtureText (Join-Path $f.Target 'preserve-me.txt') 'concurrent unowned target'
    }
}

function Write-PrismNewBytes([string]$Path, [byte[]]$Bytes) {
    if ([IO.Path]::GetFileName($Path) -ceq 'instance.cfg' -and
        (Test-PrismWithin $Path $script:CurrentFixture.Instances)) { $script:StagesInsideScan++ }
    & $script:OriginalByteWriter $Path $Bytes
}

function Get-PrismBuildResources([string]$Root, [string]$GradleHome) {
    if ($Root -cne $script:CurrentFixture.Root) { throw 'TEST: resource inspection escaped fixture.' }
    $script:CurrentFixture.ResourceReads++
    return $script:CurrentFixture.Resources
}

function Invoke-PrismGradle([string]$Wrapper, [string[]]$Arguments) {
    $f = $script:CurrentFixture
    if ($Wrapper -cne (Join-Path $f.Root 'gradlew.bat')) { throw 'TEST: build escaped fixture.' }
    $f.BuildLaunches++
    $f.GradleArguments = $Arguments
    $properties = Get-PrismProperties $f.Root
    $destination = Join-Path $f.Root ("dist/Grindless-T0-$($properties.pack_version)-playable-$($f.Source.Substring(0, 7)).zip")
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
    [IO.File]::Copy($f.Zip, $destination, $true)
}

function Invoke-Fixture($Fixture, [string]$Action = 'Deploy', [hashtable]$Changes = @{}) {
    $script:CurrentFixture = $Fixture
    $arguments = @{ Action = $Action; Root = $Fixture.Root; InstancesRoot = $Fixture.Instances
        InstanceId = $Fixture.InstanceId; TemplateInstance = $Fixture.Template
        JavaPath = $Fixture.Java; JavaSha256 = $Fixture.JavaHash; PackZip = $Fixture.Zip; ExpectedRevision = $Fixture.Source }
    foreach ($key in $Changes.Keys) { $arguments[$key] = $Changes[$key] }
    return Invoke-PrismDev @arguments
}

function New-FixtureQuestPlan($Fixture, [switch]$Managed, [switch]$TwoChanged) {
    # Synthetic chapter bytes exercise the production filesystem transaction, not native loading.
    $chapters = Join-Path $Fixture.Target 'minecraft/config/ftbquests/quests/chapters'
    $files = @()
    foreach ($tier in @(0, 1)) {
        $filename = "grindless_t$tier.snbt"
        $oldCount = @(32, 14)[$tier]
        $newCount = @(32, 18)[$tier]
        $versions = @()
        foreach ($count in @($oldCount, $newCount)) {
            $quests = @(0..($count - 1) | ForEach-Object {
                $id = '{0:X16}' -f (100 * ($tier + 1) + $_)
                $task = '{0:X16}' -f (1000 * ($tier + 1) + $_)
                '{id:"' + $id + '",tasks:[{id:"' + $task + '",type:"ftbquests:checkmark"}],rewards:[]}'
            })
            $versions += ,([Text.Encoding]::UTF8.GetBytes(('{id:"' + ('{0:X16}' -f ($tier + 1)) +
                '",quests:[' + ($quests -join ',') + "]}`n")))
        }
        if ($TwoChanged -and $tier -eq 0) {
            $versions[1] = [Text.Encoding]::UTF8.GetBytes(([Text.Encoding]::UTF8.GetString($versions[1]) + "`n"))
        }
        $path = Join-Path $chapters $filename
        [IO.Directory]::CreateDirectory($chapters) | Out-Null
        [IO.File]::WriteAllBytes($path, $versions[0])
        $files += [ordered]@{ fileName = $filename; beforeSha256 = (Get-PrismBytesHash $versions[0])
            beforeBytes = $versions[0].Length; afterSha256 = (Get-PrismBytesHash $versions[1])
            afterBytes = $versions[1].Length; contentsBase64 = [Convert]::ToBase64String($versions[1]) }
    }
    Write-FixtureText (Join-Path $chapters 'foreign.snbt') '{id:"0000000000007777",quests:[]}'
    foreach ($relative in @('minecraft/saves/reference/ftbquests/team.snbt',
            'minecraft/saves/reference/data/grindless_quests.dat', 'minecraft/config/ftbquests/quests/data.snbt',
            'minecraft/options.txt')) {
        Write-FixtureText (Join-Path $Fixture.Target $relative) ('UNCHANGED SYNTHETIC SENTINEL ' + $relative)
    }
    $state = Get-PrismOwnedState $Fixture.Target $Fixture.InstanceId
    $receipt = [ordered]@{ schemaVersion = 1; tool = 'grindless-ftb-definitions'
        instanceId = $Fixture.InstanceId; ownershipId = $state.Owner.ownershipId
        sourceRevision = $Fixture.Source; runtimeSourceRevision = $Fixture.Source
        grindlessSha256 = $state.Receipt.grindless.sha256; questCount = 50
        files = @($files | ForEach-Object {
            [ordered]@{ fileName = $_.fileName; bytes = $_.afterBytes; sha256 = $_.afterSha256 }
        }) }
    $previousReceipt = $null
    if ($Managed) {
        $oldReceipt = (ConvertTo-Json $receipt -Depth 30) | ConvertFrom-Json
        $oldReceipt.questCount = 46
        foreach ($file in $oldReceipt.files) {
            $oldFile = @($files | Where-Object { $_.fileName -ceq $file.fileName })[0]
            $file.bytes = $oldFile.beforeBytes; $file.sha256 = $oldFile.beforeSha256
        }
        $previousReceipt = Join-Path $Fixture.Target 'minecraft/config/ftbquests/.grindless-definitions.json'
        Write-FixtureJson $previousReceipt $oldReceipt
    }
    $plan = [ordered]@{ schemaVersion = 1; tool = 'grindless-ftb-definition-update'
        sourceRevision = $Fixture.Source; runtimeSourceRevision = $Fixture.Source
        grindlessSha256 = $state.Receipt.grindless.sha256
        instanceId = $Fixture.InstanceId; ownershipId = $state.Owner.ownershipId; target = $Fixture.Target
        ownerSha256 = (Get-PrismHash (Join-Path $Fixture.Target $script:PrismDevOwnerFile))
        deploymentSha256 = (Get-PrismHash (Join-Path $Fixture.Target $script:PrismDevReceiptFile))
        previousReceiptSha256 = $(if ($previousReceipt) { Get-PrismHash $previousReceipt } else { $null })
        previousDefinitionsSha256 = ('a' * 64)
        chapters = @(Get-ChildItem -LiteralPath $chapters -File | Sort-Object Name | ForEach-Object {
            [ordered]@{ fileName = $_.Name; bytes = $_.Length; sha256 = (Get-PrismHash $_.FullName) }
        })
        files = $files; receipt = $receipt }
    $path = Join-Path $Fixture.Home 'native-preflight-plan.json'
    Write-FixtureJson $path $plan
    return [pscustomobject]@{ Path = $path; Sha256 = (Get-PrismHash $path); Plan = $plan; Chapters = $chapters }
}

function Invoke-FixtureQuestUpdate($Fixture, $Plan, [string]$Action = 'UpdateQuestDefinitions') {
    return Invoke-Fixture $Fixture $Action @{ QuestUpdatePlan = $Plan.Path; QuestUpdateSha256 = $Plan.Sha256 }
}

function Save-FixtureQuestPlan($Plan) {
    Write-FixtureJson $Plan.Path $Plan.Plan
    $Plan.Sha256 = Get-PrismHash $Plan.Path
}

function Test-PrismQuestDefinitionTransactions {
    $previousFixture = $script:CurrentFixture
    try {
        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f -Managed
        $world = Get-FixtureSnapshot (Join-Path $f.Target 'minecraft/saves')
        $data = Get-PrismHash (Join-Path $f.Target 'minecraft/config/ftbquests/quests/data.snbt')
        $options = Get-PrismHash (Join-Path $f.Target 'minecraft/options.txt')
        $owner = Get-PrismHash (Join-Path $f.Target $script:PrismDevOwnerFile)
        $deployment = Get-PrismHash (Join-Path $f.Target $script:PrismDevReceiptFile)
        $oldReceipt = $plan.Plan.previousReceiptSha256
        $result = Invoke-FixtureQuestUpdate $f $plan
        Assert-Test ($result.Outcome -ceq 'DefinitionsUpdated') 'Managed46-to50 transaction succeeds'
        Assert-Test ((Get-PrismHash (Join-Path $f.Target ($result.Backup + '/plan.json'))) -ceq $plan.Sha256) 'Original verified plan survives outside disposable build output'
        Assert-Test ((Get-PrismHash (Join-Path $f.Target ($result.Backup + '/previous-receipt.json'))) -ceq $oldReceipt) 'Original definition ownership backup is byte-verified'
        Assert-Test ((Read-PrismJson (Join-Path $f.Target $script:PrismQuestReceiptFile)).questCount -eq 50) 'Only completed publication receives the50 ownership receipt'
        foreach ($file in $plan.Plan.files) {
            Assert-Test ((Get-PrismHash (Join-Path $plan.Chapters $file.fileName)) -ceq $file.afterSha256) "Complete managed after bytes: $($file.fileName)"
            Assert-Test ((Get-PrismHash (Join-Path $f.Target ($result.Backup + '/before-' + $file.fileName))) -ceq $file.beforeSha256) "Verified original chapter backup: $($file.fileName)"
        }
        Assert-Test ((Get-FixtureSnapshot (Join-Path $f.Target 'minecraft/saves')) -ceq $world) 'All native/team/legacy save bytes and timestamps are preserved'
        Assert-Test ((Get-PrismHash (Join-Path $f.Target 'minecraft/config/ftbquests/quests/data.snbt')) -ceq $data) 'Global native quest settings are never rewritten'
        Assert-Test ((Get-PrismHash (Join-Path $f.Target 'minecraft/options.txt')) -ceq $options) 'Player options are never rewritten'
        Assert-Test ((Get-PrismHash (Join-Path $f.Target $script:PrismDevOwnerFile)) -ceq $owner) 'Instance ownership marker is never rewritten'
        Assert-Test ((Get-PrismHash (Join-Path $f.Target $script:PrismDevReceiptFile)) -ceq $deployment) 'Runtime deployment receipt is never rewritten'
        $stable = Get-FixtureSnapshot $f.Target
        Assert-Test ((Invoke-FixtureQuestUpdate $f $plan).Outcome -ceq 'Unchanged') 'Managed repeated update is an explicit no-op'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Managed no-op changes no file, backup, receipt or timestamp'
        Assert-Refused {
            Invoke-PrismDev -Action UpdateQuestDefinitions -Root $f.Root -InstancesRoot $f.Instances `
                -ExpectedRevision $f.Source -PackZip $f.Zip -QuestUpdatePlan $plan.Path -QuestUpdateSha256 $plan.Sha256
        } 'explicitly selected InstanceId' 'No implicit/default player target may receive definition updates'
        Assert-Refused {
            Invoke-Fixture $f 'Inspect' @{ QuestUpdatePlan = $plan.Path; QuestUpdateSha256 = $plan.Sha256 }
        } 'explicit definition update/recovery action' 'Inspect cannot silently ignore a selected update plan'
        Assert-Refused {
            Invoke-Fixture $f 'Build' @{ QuestUpdatePlan = $plan.Path; QuestUpdateSha256 = $plan.Sha256 }
        } 'explicit definition update/recovery action' 'A definition plan cannot accidentally dispatch a build'
        Assert-Refused {
            Invoke-Fixture $f 'UpdateQuestDefinitions' @{ QuestUpdatePlan = $plan.Path; QuestUpdateSha256 = $plan.Sha256
                RuntimeRevision = $f.Source }
        } 'RuntimeRevision is only a Build/BuildAndDeploy input' 'Definition actions never ignore a conflicting runtime selector'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'CLI ambiguity refusals preserve every target byte/timestamp'

        foreach ($name in @('grindless_t0.snbt', 'grindless_t1.snbt', 'foreign.snbt')) {
            $f = New-Fixture
            $null = Invoke-Fixture $f
            $plan = New-FixtureQuestPlan $f -Managed -TwoChanged
            Write-FixtureText (Join-Path $plan.Chapters $name) '{ user_edits: "preserve even malformed/unknown bytes"'
            $stable = Get-FixtureSnapshot $f.Target
            Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'drifted or were edited' "Changed chapter $name"
            Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Whole two-file preflight refuses before any mutation'
            Assert-Test (-not [IO.Directory]::Exists((Join-Path $f.Target '.grindless-dev-backups'))) 'Refused preflight creates no backups'
            Assert-Test (-not [IO.File]::Exists((Join-Path $f.Instances ('.' + $f.InstanceId + '.grindless-deploy.lock')))) 'Refused preflight creates no deployment lock'
        }
        foreach ($mutation in @('version', 'string-version', 'fractional-size', 'payload', 'collision', 'ownership', 'snapshot', 'hash')) {
            $f = New-Fixture
            $null = Invoke-Fixture $f
            $plan = New-FixtureQuestPlan $f -Managed
            $message = ''
            switch ($mutation) {
                'version' { $plan.Plan.schemaVersion = 2; $message = 'Unknown native definition plan version' }
                'string-version' { $plan.Plan.schemaVersion = '1'; $message = 'Unknown native definition plan version' }
                'fractional-size' { $plan.Plan.files[0].afterBytes = 10.5; $message = 'hash or size' }
                'payload' { $plan.Plan.files[1].contentsBase64 = [Convert]::ToBase64String([byte[]]@(0, 1)); $message = 'payload hash mismatch' }
                'collision' { $plan.Plan.files[1].fileName = $plan.Plan.files[0].fileName; $message = 'filenames differs' }
                'ownership' { $plan.Plan.ownershipId = 'f' * 32; $message = 'target ownership' }
                'snapshot' {
                    @($plan.Plan.chapters | Where-Object { $_.fileName -ceq 'grindless_t0.snbt' })[0].sha256 = 'b' * 64
                    $message = 'preflight snapshot'
                }
                'hash' { $message = 'plan SHA256' }
            }
            if ($mutation -ne 'hash') { Save-FixtureQuestPlan $plan } else { $plan.Sha256 = 'b' * 64 }
            $stable = Get-FixtureSnapshot $f.Target
            Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } $message "Malformed managed plan $mutation"
            Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Bad plan preserves every target byte/timestamp'
        }
        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f
        $f.Source = '2' * 40
        New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic50 runtime must be installed before chapter update'))
        $package = Read-PrismPackage $f.Root $f.Zip (Get-PrismContract $f.Root) $f.Source
        $plan.Plan.sourceRevision = $f.Source
        $plan.Plan.runtimeSourceRevision = $f.Source
        $plan.Plan.grindlessSha256 = $package.JarSha256
        $plan.Plan.receipt.sourceRevision = $f.Source
        $plan.Plan.receipt.runtimeSourceRevision = $f.Source
        $plan.Plan.receipt.grindlessSha256 = $package.JarSha256
        Save-FixtureQuestPlan $plan
        $stable = Get-FixtureSnapshot $f.Target
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'Deploy the verified current package while stopped' 'Never publish50 chapters under an old46 runtime'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Runtime sequencing refusal does not touch definitions or deploy anything'

        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f -Managed
        Write-FixtureText (Join-Path $plan.Chapters 'new-foreign.snbt') '{id:"0000000000008888",quests:[]}'
        $stable = Get-FixtureSnapshot $f.Target
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'inventory drifted' 'Chapter added after native preflight'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Additional native chapter is preserved'
        $f.Processes = @([pscustomobject]@{ Name = 'javaw.exe'; ExecutablePath = $f.Java
            CommandLine = ('javaw --gameDir "' + $f.Target + '\minecraft" --accessToken TOKEN_DO_NOT_PRINT') })
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'is running' 'Running target refuses definition mutation'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Stopped-target refusal preserves all bytes'

        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f -Managed -TwoChanged
        function Write-PrismNewBytes([string]$Path, [byte[]]$Bytes) {
            & $script:OriginalByteWriter $Path $Bytes
            if ([IO.Path]::GetFileName($Path) -ceq 'before-grindless_t1.snbt') {
                [IO.File]::AppendAllText($Path, 'INJECTED BACKUP CORRUPTION')
            }
        }
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'backup/staging verification failed' 'Corrupted backup refuses before replacement'
        Set-Item Function:\Write-PrismNewBytes $script:OriginalByteWriter
        foreach ($file in $plan.Plan.files) {
            Assert-Test ((Get-PrismHash (Join-Path $plan.Chapters $file.fileName)) -ceq $file.beforeSha256) 'Bad backup never touches installed chapters'
        }
        Assert-Test (-not [IO.File]::Exists((Join-Path $f.Target $script:PrismQuestPendingFile))) 'No transaction journal is published for unverified backups'

        foreach ($afterReplacement in @($false, $true)) {
            $f = New-Fixture
            $null = Invoke-Fixture $f
            $plan = New-FixtureQuestPlan $f -Managed -TwoChanged
            $script:QuestFaulted = $false
            $script:QuestThrowAfter = $afterReplacement
            function Set-PrismReplacement([string]$Source, [string]$Destination) {
                $name = [IO.Path]::GetFileName($Destination)
                if (-not $script:QuestFaulted -and
                    (($script:QuestThrowAfter -and $name -ceq 'grindless_t0.snbt') -or
                     (-not $script:QuestThrowAfter -and $name -ceq 'grindless_t1.snbt'))) {
                    $script:QuestFaulted = $true
                    if ($script:QuestThrowAfter) { & $script:OriginalReplacement $Source $Destination }
                    throw 'Injected partial chapter replacement failure.'
                }
                & $script:OriginalReplacement $Source $Destination
            }
            Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'original chapters and ownership receipt were restored' "Actual partial failure after-replacement=$afterReplacement"
            Set-Item Function:\Set-PrismReplacement $script:OriginalReplacement
            foreach ($file in $plan.Plan.files) {
                Assert-Test ((Get-PrismHash (Join-Path $plan.Chapters $file.fileName)) -ceq $file.beforeSha256) 'Verified rollback restores every original chapter byte'
            }
            Assert-Test ((Get-PrismHash (Join-Path $f.Target $script:PrismQuestReceiptFile)) -ceq $plan.Plan.previousReceiptSha256) 'Rollback cannot publish a success-shaped ownership receipt'
            Assert-Test (-not [IO.File]::Exists((Join-Path $f.Target $script:PrismQuestPendingFile))) 'Only verified recovery clears its journal'
            Assert-Test ((Invoke-FixtureQuestUpdate $f $plan).Outcome -ceq 'DefinitionsUpdated') 'Explicit retry after rollback succeeds'
        }

        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f -Managed -TwoChanged
        $script:QuestFaulted = $false
        function Set-PrismReplacement([string]$Source, [string]$Destination) {
            & $script:OriginalReplacement $Source $Destination
            if (-not $script:QuestFaulted -and [IO.Path]::GetFileName($Destination) -ceq '.grindless-definitions.json') {
                $script:QuestFaulted = $true
                throw 'Injected failure after ownership receipt replacement.'
            }
        }
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'original chapters and ownership receipt were restored' 'Failure after receipt commit is not a false success'
        Set-Item Function:\Set-PrismReplacement $script:OriginalReplacement
        foreach ($file in $plan.Plan.files) {
            Assert-Test ((Get-PrismHash (Join-Path $plan.Chapters $file.fileName)) -ceq $file.beforeSha256) 'Post-receipt rollback restores original definition bytes'
        }
        Assert-Test ((Get-PrismHash (Join-Path $f.Target $script:PrismQuestReceiptFile)) -ceq $plan.Plan.previousReceiptSha256) 'Post-receipt rollback restores original ownership bytes'

        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f -Managed -TwoChanged
        $script:QuestReplaceCalls = 0
        function Set-PrismReplacement([string]$Source, [string]$Destination) {
            $script:QuestReplaceCalls++
            if ($script:QuestReplaceCalls -gt 1) { throw 'Injected update AND recovery failure.' }
            & $script:OriginalReplacement $Source $Destination
        }
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'recovery is incomplete' 'Failed rollback remains visible'
        Set-Item Function:\Set-PrismReplacement $script:OriginalReplacement
        $journal = Join-Path $f.Target $script:PrismQuestPendingFile
        Assert-Test ([IO.File]::Exists($journal)) 'Interrupted/failed recovery retains its durable pending journal'
        Assert-Refused { Invoke-Fixture $f 'Inspect' } 'incomplete native definition update' 'Inspect cannot hide a partial definition state'
        Assert-Refused { Invoke-Fixture $f } 'incomplete native definition update' 'Runtime deployment cannot bypass a partial definition state'
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'incomplete native definition update' 'Repeated apply cannot relabel interrupted work as success'
        $t0 = Join-Path $plan.Chapters 'grindless_t0.snbt'
        $originalPartial = [IO.File]::ReadAllBytes($t0)
        Write-FixtureText $t0 'FOREIGN EDIT DURING INTERRUPTION'
        $stable = Get-FixtureSnapshot $f.Target
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan 'RecoverQuestDefinitions' } 'drifted or were edited' 'Recovery refuses foreign edits before any mutation'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Foreign edit and all recovery evidence remain intact'
        [IO.File]::WriteAllBytes($t0, $originalPartial)
        $interrupted = Read-PrismJson $journal
        $backupT1 = Join-Path $f.Target ($interrupted.backup + '/before-grindless_t1.snbt')
        $originalBackup = [IO.File]::ReadAllBytes($backupT1)
        [IO.File]::AppendAllText($backupT1, 'CORRUPTED BACKUP')
        $stable = Get-FixtureSnapshot $f.Target
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan 'RecoverQuestDefinitions' } 'backup verification failed' 'Recovery verifies every backup before touching any chapter'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'Bad recovery backup cannot partially restore the first chapter'
        [IO.File]::WriteAllBytes($backupT1, $originalBackup)
        $backupPlan = Join-Path $f.Target ($interrupted.backup + '/plan.json')
        $originalPlan = [IO.File]::ReadAllBytes($backupPlan)
        [IO.File]::AppendAllText($backupPlan, 'CORRUPTED PLAN')
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan 'RecoverQuestDefinitions' } 'plan backup verification failed' 'Recovery refuses changed original plan bytes'
        [IO.File]::WriteAllBytes($backupPlan, $originalPlan)
        $lock = Join-Path $f.Instances ('.' + $f.InstanceId + '.grindless-deploy.lock')
        Write-FixtureText $lock 'FOREIGN LOCK'
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan 'RecoverQuestDefinitions' } 'Foreign deployment lock' 'Recovery never adopts a foreign lock'
        Assert-Test ([IO.File]::ReadAllText($lock) -ceq 'FOREIGN LOCK') 'Foreign lock remains byte-identical after refusal'
        [IO.File]::Delete($lock)
        Write-FixtureText $lock ('grindless-ftb-definition-update:' + $plan.Sha256)
        $active = [IO.File]::Open($lock, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None)
        try {
            Assert-Refused { Invoke-FixtureQuestUpdate $f $plan 'RecoverQuestDefinitions' } 'active or uninspectable' 'Even a matching plan lock cannot bypass an active writer'
            Assert-Test ([IO.File]::Exists($journal) -and [IO.File]::Exists($lock)) 'Active recovery lock and pending journal remain present'
        } finally { $active.Dispose() }
        $result = Invoke-FixtureQuestUpdate $f $plan 'RecoverQuestDefinitions'
        Assert-Test ($result.Outcome -ceq 'DefinitionsRestored') 'Explicit recovery admits only its matching stale lock and verified backups'
        foreach ($file in $plan.Plan.files) {
            Assert-Test ((Get-PrismHash (Join-Path $plan.Chapters $file.fileName)) -ceq $file.beforeSha256) 'Recovery restores interrupted chapter bytes'
        }
        Assert-Test (-not [IO.File]::Exists($journal) -and -not [IO.File]::Exists($lock)) 'Completed recovery removes only its own journal/lock'

        $f = New-Fixture
        $null = Invoke-Fixture $f
        $plan = New-FixtureQuestPlan $f
        $outside = Join-Path $f.Home 'outside-definitions'
        Write-FixtureText (Join-Path $outside 'sentinel.txt') 'NO LINK FOLLOWING'
        $link = Join-Path $plan.Chapters 'unsafe.snbt'
        New-Item -ItemType Junction -Path $link -Target $outside | Out-Null
        $script:Junctions += $link
        Assert-Refused { Invoke-FixtureQuestUpdate $f $plan } 'Reparse' 'Real chapter junction refuses managed update'
        Assert-Test ([IO.File]::ReadAllText((Join-Path $outside 'sentinel.txt')) -ceq 'NO LINK FOLLOWING') 'Linked destination is preserved'
        [IO.Directory]::Delete($link)
        $script:Junctions = @($script:Junctions | Where-Object { $_ -cne $link })
        Assert-Test ((Invoke-FixtureQuestUpdate $f $plan).Outcome -ceq 'DefinitionsUpdated') 'Explicit verified no-receipt reference plan can establish minimal ownership'
        $stable = Get-FixtureSnapshot $f.Target
        Assert-Test ((Invoke-FixtureQuestUpdate $f $plan).Outcome -ceq 'Unchanged') 'No-receipt transition is idempotent after its first managed receipt'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $stable) 'No-receipt repeat does not create additional backups'
        Write-Output 'PASS: native definition filesystem46-to50, two-file preflight, verified backups, rollback/recovery, drift/ownership/link refusals and no-op.'
        Write-Output 'These synthetic plan/byte fixtures do not execute the Java SNBT publisher, native loader, player import or saved-world migration.'
    } finally {
        Set-Item Function:\Set-PrismReplacement $script:OriginalReplacement
        Set-Item Function:\Write-PrismNewBytes $script:OriginalByteWriter
        $script:CurrentFixture = $previousFixture
    }
}

function Invoke-PcWorkflowTransport([string]$Target, $Packet) {
    $script:CapturedSshTarget = $Target
    $script:CapturedPacket = $Packet
    return $script:TransportExit
}

function New-PcFixtureConfig($Fixture) {
    return [ordered]@{
        schema = 'grindless.pc-workflow'; schema_version = 1; sshTarget = 'fixture-user@fixture-pc'
        clonePath = (Join-Path $Fixture.Home 'personal clone')
        worktreePath = (Join-Path $Fixture.Home ("execution & (literal)' " + [char]0xE9))
        gradleUserHome = (Join-Path $Fixture.Home 'gradle-local')
        prismRoot = (Split-Path -Parent $Fixture.Instances); templateInstanceId = 'OldTemplate'
        instanceId = $Fixture.InstanceId; javaHome = $Fixture.JavaHome; sourceBranch = 'fixture-source'
        receivedSourceRevision = ('1' * 40); personalGitSshAlias = 'fixture-personal'; javawSha256 = $Fixture.JavaHash
    }
}

# This substitutes the git executable only inside the fixed remote script fixture.
# It never calls real Git, SSH or any native launcher/build entrypoint.
function git {
    $all = @($args)
    if ($all.Count -lt 4 -or $all[0] -cne '--no-pager' -or $all[1] -cne '-C') { throw 'TEST: unexpected Git shape.' }
    $at = [string]$all[2]
    $command = @($all[3..($all.Count - 1)])
    $r = $script:RemoteFixture
    $r.Calls += ,([pscustomobject]@{ At = $at; Arguments = $command })
    $global:LASTEXITCODE = 0
    switch ($command[0]) {
        'rev-parse' {
            switch ($command[1]) {
                '--show-toplevel' { return $at }
                '--git-common-dir' { return (Join-Path $r.Config.clonePath '.git') }
                '--verify' {
                    if ($command[2] -ceq 'HEAD^{commit}') {
                        if ($at -ieq $r.Config.clonePath) { return $r.CloneHead }
                        return $r.Head
                    }
                    return $command[2].Substring(0, 40)
                }
            }
        }
        'symbolic-ref' {
            if ($at -ieq $r.Config.clonePath) { return 'refs/heads/unchanged-clone-branch' }
            if ($r.Attached) { return 'refs/heads/preserved-worktree-branch' }
            $global:LASTEXITCODE = 1
            return
        }
        'remote' { return $r.Origin }
        'status' { if ($r.Dirty) { return '?? unknown-fixture' }; return }
        'fetch' { if ($r.FetchFailure) { $global:LASTEXITCODE = 41 }; return }
        'merge-base' { if ($r.Unrelated) { $global:LASTEXITCODE = 1 }; return }
        'worktree' {
            if ($command[1] -cne 'add' -or $command[2] -cne '--detach') { throw 'TEST: unsafe worktree operation.' }
            [IO.Directory]::CreateDirectory($r.Config.worktreePath) | Out-Null
            $r.Head = $command[4]
            return
        }
        'checkout' {
            if ($at -ieq $r.Config.clonePath -or $command[1] -cne '--detach') { throw 'TEST: unsafe checkout.' }
            $r.Head = $command[2]
            return
        }
    }
    throw 'TEST: unexpected Git command.'
}

function New-RemoteFixture($Fixture) {
    $path = Join-Path $Fixture.Home 'private-pc.json'
    Write-FixtureJson $path (New-PcFixtureConfig $Fixture)
    $config = Read-PcWorkflowConfig $path
    [IO.Directory]::CreateDirectory($config.clonePath) | Out-Null
    [IO.Directory]::CreateDirectory((Join-Path $config.clonePath '.git')) | Out-Null
    $remote = [pscustomobject]@{ Config = $config; Calls = @(); Head = ('1' * 40); CloneHead = ('a' * 40)
        Attached = $false; Dirty = $false; Unrelated = $false; FetchFailure = $false
        Processes = @(); InspectionFailure = $false
        Origin = 'git@fixture-personal:fixture/repository.git' }
    $script:RemoteFixture = $remote
    return $remote
}

function Invoke-RemoteFixture($Remote, [string]$Action, [string]$Revision = ('1' * 40), [string]$Pack = '') {
    $request = [pscustomobject]@{ protocol = 'grindless.pc-workflow/1'; action = $Action
        revision = $Revision; config = $Remote.Config; packZip = $Pack }
    return & $script:PcWorkflowRemote $request
}

function Get-CimInstance {
    [CmdletBinding()]
    param([string]$ClassName, [string]$Filter)
    if ($ClassName -cne 'Win32_Process') { throw 'TEST: unexpected real resource query.' }
    if ($script:RemoteFixture.InspectionFailure) { throw 'Fixture CIM query failure.' }
    return $script:RemoteFixture.Processes
}

try {
    foreach ($relative in @('tools/prism-dev.ps1', 'tools/pc-workflow.ps1', 'tools/checks/Test-PrismDev.ps1')) {
        $tokens = $null; $errors = $null
        $null = [Management.Automation.Language.Parser]::ParseFile((Join-Path $RepositoryRoot $relative), [ref]$tokens, [ref]$errors)
        Assert-Test ($errors.Count -eq 0) "$relative parses in Windows PowerShell 5.1"
    }
    $nativeAst = [Management.Automation.Language.Parser]::ParseFile((Join-Path $RepositoryRoot 'tools/prism-dev.ps1'),
        [ref]$tokens, [ref]$errors)
    $allFunctions = @($nativeAst.FindAll({ param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] }, $true))
    $topFunctions = @($nativeAst.EndBlock.Statements | Where-Object { $_ -is [Management.Automation.Language.FunctionDefinitionAst] })
    Assert-Test ($allFunctions.Count -eq $topFunctions.Count) 'All native helpers are top-level, never nested in package verification'
    Assert-Test (@($nativeAst.EndBlock.Statements | Where-Object { $_ -is [Management.Automation.Language.IfStatementAst] }).Count -eq 1) 'Sole native entrypoint is top-level'

    if ($Suite -eq 'QuestDefinitions') {
        $script:CurrentFixture = $null
        Test-PrismQuestDefinitionTransactions
        Write-Output ("FTB DEFINITION FILESYSTEM CHECKS PASSED: " + $script:Assertions + " assertions; " + $script:Cases + " isolated fixtures.")
        return
    }
    $f = New-Fixture
    $contract = Get-PrismContract $f.Root
    Assert-Test ($contract.Dependencies.Count -eq 9 -and $contract.Manifest.files.Count -eq 9) `
        'Playable has nine authoritative dependency pins'
    Assert-Test ((@($contract.Dependencies.projectID | Sort-Object) -join ',') -ceq
        (@(419699, 238222, 289412, 404465, 404468, 889915, 790626, 429235, 908741 | Sort-Object) -join ',')) 'Exact native FTB playable allowlist'
    $nativePins = @{
        289412 = @('ftb-quests', 'ftbquests')
        404465 = @('ftb-library', 'ftblibrary')
        404468 = @('ftb-teams', 'ftbteams')
        889915 = @('ftb-xmod-compat', 'ftbxmodcompat')
    }
    foreach ($id in $nativePins.Keys) {
        $pin = @($contract.Dependencies | Where-Object { $_.projectID -eq $id })[0]
        $coordinate = $nativePins[$id]
        Assert-Test ($pin.fileName -ceq ($coordinate[0] + '-forge-' +
            $contract.Properties[$coordinate[1] + '_version'] + '.jar')) "Native FTB $id exact versioned filename"
        Assert-Test ($pin.fileID.ToString() -ceq $contract.Properties[$coordinate[1] + '_curse_file']) `
            "Native FTB $id exact file ID"
        foreach ($required in $pin.requiredProjectIDs) {
            Assert-Test ($required -in $contract.Dependencies.projectID) "Native FTB $id dependency closure"
        }
    }
    $templateBefore = Get-FixtureSnapshot $f.Template
    $before = Get-FixtureSnapshot $f.Instances
    $result = Invoke-Fixture $f 'Prepare'
    Assert-Test ($result.Outcome -ceq 'PreflightVerified') 'Prepare validates without installing'
    Assert-Test ((Get-FixtureSnapshot $f.Instances) -ceq $before) 'Prepare is byte/timestamp read-only'
    Assert-Test ($script:DownloadCalls -eq 0) 'Prepare does not download'
    $result = Invoke-Fixture $f
    Assert-Test ($result.Outcome -ceq 'Deployed') 'Create deployment succeeds'
    Assert-Test ($result.MissingLocalDependencies.Count -eq 0 -and $result.DownloadedDependencies.Count -eq 0) 'Cached create reports neither missing nor downloaded dependencies'
    Assert-Test ($script:StagesInsideScan -eq 0) 'No instance.cfg is ever staged inside the live Prism instances scan'
    Assert-Test ([IO.File]::Exists((Join-Path $f.Target 'minecraft/mods/grindless-0.1.0-forge.jar'))) 'New instance uses supported minecraft root'
    Assert-Test ((Get-ChildItem -LiteralPath (Join-Path $f.Target 'minecraft/mods') -Force).Count -eq 10) 'Exactly nine pins plus Grindless'
    Assert-Test (-not [IO.Directory]::Exists((Join-Path $f.Target '.minecraft'))) 'No legacy game-root duplicate'
    Assert-Test (-not [IO.Directory]::Exists((Join-Path $f.Target '.index'))) 'No template index copied'
    Assert-Test (-not [IO.File]::Exists((Join-Path $f.Target 'accounts.json'))) 'No account data copied'
    Assert-Test (-not [IO.Directory]::Exists((Join-Path $f.Target 'minecraft/saves'))) 'No exploratory save copied'
    Assert-Test ((Get-FixtureSnapshot $f.Template) -ceq $templateBefore) 'Old template and real-save stand-in unchanged'
    $templateMetadata = Read-PrismJson (Join-Path $f.Template 'mmc-pack.json')
    $sanitizedMetadata = Read-PrismJson (Join-Path $f.Target 'mmc-pack.json')
    Assert-Test ($templateMetadata.components[0].cachedVolatile -eq $true) 'Fixture matches actual working Prism 11.0.2 cachedVolatile shape'
    Assert-Test ($null -eq $sanitizedMetadata.components[0].PSObject.Properties['cachedVolatile']) 'Standard volatile cache is accepted but never copied'
    Assert-Test ($sanitizedMetadata.components[0].version -ceq $templateMetadata.components[0].version) 'LWJGL version comes only from working component metadata'
    $cfg = Read-PrismCfg (Join-Path $f.Target 'instance.cfg')
    Assert-Test ($cfg.JavaPath -ceq $f.Java.Replace('\', '/') -and $cfg.OverrideJavaLocation -ceq 'true') 'Explicit per-instance Java'
    Assert-Test ($cfg.ManagedPack -ceq 'false' -and $cfg.MaxMemAlloc -ceq '4096') 'Unmanaged conservative engineering settings'
    Write-FixtureText (Join-Path $f.Target 'minecraft/mods/.index/grindless.pw.toml') 'synthetic launcher sidecar'
    $indexBefore = Get-FixtureSnapshot (Join-Path $f.Target 'minecraft/mods/.index')
    $before = Get-FixtureSnapshot $f.Target
    $result = Invoke-Fixture $f
    Assert-Test ($result.Outcome -ceq 'Unchanged') 'Idempotent same package is a no-op'
    Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $before) 'Idempotence preserves every byte and timestamp'
    $result = Invoke-Fixture $f 'Inspect'
    Assert-Test ($result.Outcome -ceq 'VerifiedStopped') 'Inspect verifies stopped owned target'
    foreach ($relative in @('minecraft/saves/engineering/level.dat', 'minecraft/config/player.cfg', 'minecraft/options.txt')) {
        Write-FixtureText (Join-Path $f.Target $relative) ('retained synthetic player data ' + $relative)
    }
    $retained = @{}
    foreach ($name in @('saves', 'config')) { $retained[$name] = Get-FixtureSnapshot (Join-Path $f.Target ('minecraft/' + $name)) }
    $optionsHash = Get-PrismHash (Join-Path $f.Target 'minecraft/options.txt')
    $oldReceipt = Read-PrismJson (Join-Path $f.Target $script:PrismDevReceiptFile)
    $oldJarHash = $oldReceipt.grindless.sha256
    $f.Source = '2' * 40
    New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic remapped Grindless revision two'))
    $result = Invoke-Fixture $f
    Assert-Test ($result.Outcome -ceq 'Deployed' -and $result.SourceRevision -ceq $f.Source) 'Update records exact new source'
    $newReceipt = Read-PrismJson (Join-Path $f.Target $script:PrismDevReceiptFile)
    $backup = Join-Path $f.Target ('.grindless-dev-backups/' + $newReceipt.operationId)
    Assert-Test ((Get-PrismHash (Join-Path $backup $oldReceipt.grindless.fileName)) -ceq $oldJarHash) 'Old owned Grindless backup retained outside loadable mods'
    foreach ($name in @('saves', 'config')) {
        Assert-Test ((Get-FixtureSnapshot (Join-Path $f.Target ('minecraft/' + $name))) -ceq $retained[$name]) "Update preserves $name"
    }
    Assert-Test ((Get-PrismHash (Join-Path $f.Target 'minecraft/options.txt')) -ceq $optionsHash) 'Update preserves options'
    Assert-Test ((Get-FixtureSnapshot (Join-Path $f.Target 'minecraft/mods/.index')) -ceq $indexBefore) 'Update preserves non-loadable Prism index sidecars byte-for-byte'
    Assert-Test ((Get-FixtureSnapshot $f.Template) -ceq $templateBefore) 'Updates never mutate old instance'
    Assert-Test ($newReceipt.packStatus -ceq 'restricted-playable-profile-not-rc') 'Deployment is not RC acceptance'

    $questPlan = New-FixtureQuestPlan $f
    $savedWorld = Get-FixtureSnapshot (Join-Path $f.Target 'minecraft/saves')
    $foreignHash = Get-PrismHash (Join-Path $questPlan.Chapters 'foreign.snbt')
    $beforeDefinitionReceipt = Get-PrismHash (Join-Path $f.Target $script:PrismDevReceiptFile)
    $result = Invoke-FixtureQuestUpdate $f $questPlan
    Assert-Test ($result.Outcome -ceq 'DefinitionsUpdated') 'Explicit native46-to50 chapter update succeeds'
    foreach ($file in $questPlan.Plan.files) {
        Assert-Test ((Get-PrismHash (Join-Path $questPlan.Chapters $file.fileName)) -ceq $file.afterSha256) `
            "Managed update publishes complete bytes for $($file.fileName)"
        Assert-Test ((Get-PrismHash (Join-Path $f.Target ($result.Backup + '/before-' + $file.fileName))) -ceq $file.beforeSha256) `
            "Managed update retains verified original bytes for $($file.fileName)"
    }
    Assert-Test ((Get-FixtureSnapshot (Join-Path $f.Target 'minecraft/saves')) -ceq $savedWorld) 'Native team data and legacy paid-history bytes are untouched'
    Assert-Test ((Get-PrismHash (Join-Path $questPlan.Chapters 'foreign.snbt')) -ceq $foreignHash) 'Foreign chapter bytes are untouched'
    Assert-Test ((Get-PrismHash (Join-Path $f.Target $script:PrismDevReceiptFile)) -ceq $beforeDefinitionReceipt) 'Definition-only update never changes runtime deployment receipt'
    $afterQuestUpdate = Get-FixtureSnapshot $f.Target
    Assert-Test ((Invoke-FixtureQuestUpdate $f $questPlan).Outcome -ceq 'Unchanged') 'Repeated managed definition update is a no-op'
    Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $afterQuestUpdate) 'Repeated definition update preserves every byte and timestamp'

    # Failure between jar replacement and receipt commit rolls back the old runtime.
    $stable = Get-PrismHash (Join-Path $f.Target $script:PrismDevReceiptFile)
    $stableJar = $newReceipt.grindless.sha256
    $f.Source = '3' * 40
    New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('third synthetic runtime'))
    function Set-PrismReplacement([string]$Source, [string]$Destination) {
        if ([IO.Path]::GetFileName($Destination) -ceq $script:PrismDevReceiptFile) { throw 'Injected receipt commit failure.' }
        & $script:OriginalReplacement $Source $Destination
    }
    Assert-Refused { Invoke-Fixture $f } 'previous managed runtime and receipt were restored' 'Partial update rollback'
    Set-Item Function:\Set-PrismReplacement $script:OriginalReplacement
    Assert-Test ((Get-PrismHash (Join-Path $f.Target $script:PrismDevReceiptFile)) -ceq $stable) 'Failed update cannot publish a new success receipt'
    Assert-Test ((Get-PrismHash (Join-Path $f.Target ('minecraft/mods/' + $newReceipt.grindless.fileName))) -ceq $stableJar) 'Failed update restores previous runtime'
    Assert-Test (-not [IO.File]::Exists((Join-Path $f.Target $script:PrismDevPendingFile))) 'Verified rollback clears only its own journal'
    $null = Invoke-Fixture $f 'Inspect'

    foreach ($declared in @($true, $false)) {
        $f = New-Fixture
        $path = Join-Path $f.Root 'pack/dependency-metadata.json'
        $metadata = Read-PrismJson $path
        foreach ($id in $nativePins.Keys) {
            $pin = @($metadata.files | Where-Object { $_.projectID -eq $id })[0]
            if ($declared) {
                # Synthetic authority describes the fixture bytes, never real upstream binaries.
                $pin | Add-Member -NotePropertyName sha256 -NotePropertyValue `
                    (Get-PrismHash (Join-Path $f.Template ('.minecraft/mods/' + $pin.fileName))) -Force
            } else { $pin.PSObject.Properties.Remove('sha256') }
        }
        Write-FixtureJson $path $metadata
        $authorityBefore = Get-PrismHash $path
        New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic digest-aware runtime'))
        Assert-Test ((Invoke-Fixture $f).Outcome -ceq 'Deployed') "Declared SHA256=$declared creates an owned instance"
        $receipt = Read-PrismJson (Join-Path $f.Target $script:PrismDevReceiptFile)
        foreach ($id in $nativePins.Keys) {
            $pin = @($receipt.dependencies | Where-Object { $_.projectID -eq $id })[0]
            Assert-Test ($pin.sha256 -ceq (Get-PrismHash (Join-Path $f.Target ('minecraft/mods/' + $pin.fileName)))) `
                "Native FTB $id receipt retains the measured and, when declared, published SHA256"
        }
        Assert-Test ((Invoke-Fixture $f).Outcome -ceq 'Unchanged') "Declared SHA256=$declared remains idempotent"
        $f.Source = '5' * 40
        New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic digest-aware runtime update'))
        Assert-Test ((Invoke-Fixture $f).Outcome -ceq 'Deployed') "Declared SHA256=$declared permits a same-pin Grindless update"
        Assert-Test ((Get-PrismHash $path) -ceq $authorityBefore) 'Receipt creation never mutates published metadata'
        if ($declared) {
            $pin = @($metadata.files | Where-Object { $_.projectID -eq 889915 })[0]
            $pin.sha256 = '0' * 64
            Write-FixtureJson $path $metadata
            New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic coherent metadata digest mismatch'))
            $before = Get-FixtureSnapshot $f.Target
            $downloadsBefore = $script:DownloadCalls
            foreach ($action in @('Prepare', 'Deploy', 'BuildAndDeploy')) {
                Assert-Refused { Invoke-Fixture $f $action @{ PackZip = '' } } 'pin migrations require a new instance' `
                    "Changed published SHA256 refuses $action without ignoring the declared digest"
            }
            Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $before) 'Published digest mismatch preserves the entire installed instance'
            Assert-Test ($script:DownloadCalls -eq $downloadsBefore -and $f.BuildLaunches -eq 0) `
                'Published digest mismatch never downloads or builds'
        }
    }

    $f = New-Fixture
    Write-FixtureText (Join-Path $f.Target 'minecraft/saves/leave/level.dat') 'unowned player save'
    $before = Get-FixtureSnapshot $f.Target
    Assert-Refused { Invoke-Fixture $f } 'unowned' 'Unowned existing target'
    Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $before) 'Unowned target unchanged'
    $f = New-Fixture
    $f.Processes = @([pscustomobject]@{ Name = 'javaw.exe'; ExecutablePath = $f.Java
        CommandLine = ('javaw --gameDir "' + $f.Target + '\minecraft" --accessToken TOKEN_DO_NOT_PRINT') })
    Assert-Refused { Invoke-Fixture $f } 'running' 'Running target'
    Assert-Test (-not [IO.Directory]::Exists($f.Target)) 'Running target never installed'
    $f.Processes = @([pscustomobject]@{ Name = 'javaw.exe'; ExecutablePath = $null; CommandLine = $null })
    Assert-Refused { Invoke-Fixture $f } 'uninspectable' 'Uninspectable Java process'
    $f.Processes = @()
    $f.InspectionFailure = $true
    Assert-Refused { Invoke-Fixture $f } 'Cannot inspect' 'Failed process query'
    $f.InspectionFailure = $false
    $f.Processes = @(
        [pscustomobject]@{ Name = 'prismlauncher.exe'; ExecutablePath = 'fixture'; CommandLine = $null },
        [pscustomobject]@{ Name = 'java.exe'; ExecutablePath = $f.Java
            CommandLine = ('java --gameDir "' + $f.Template + '\.minecraft" --accessToken TOKEN_DO_NOT_PRINT') }
    )
    Assert-Test ((Invoke-Fixture $f).Outcome -ceq 'Deployed') 'Prism itself and attributable other instance do not lock target'
    $unknown = Join-Path $f.Target 'minecraft/mods/unknown.jar'
    Write-FixtureText $unknown 'unknown mod must remain'
    $before = Get-FixtureSnapshot $f.Target
    Assert-Refused { Invoke-Fixture $f } 'Unknown' 'Unknown mod refusal'
    Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $before) 'Unknown mod is preserved, not deleted'
    [IO.File]::Delete($unknown)
    $receipt = Read-PrismJson (Join-Path $f.Target $script:PrismDevReceiptFile)
    Write-FixtureText (Join-Path $f.Target ('minecraft/mods/' + $receipt.dependencies[0].fileName)) 'tampered'
    Assert-Refused { Invoke-Fixture $f } 'tampered' 'Tampered previously managed mod'

    $f = New-Fixture
    Assert-Refused { Invoke-Fixture $f 'Deploy' @{ InstanceId = '../OldTemplate' } } 'Unsafe' 'Instance traversal'
    Assert-Refused { Invoke-Fixture $f 'Deploy' @{ InstancesRoot = ($f.Instances + '\..\instances') } } 'traversal' 'Root traversal'
    Assert-Refused { Get-PrismPath 'C:relative' 'Directory' -AllowMissing } 'absolute local path' 'Drive-relative Windows target'
    Assert-Refused { Get-PrismPath '\root-relative' 'Directory' -AllowMissing } 'absolute local path' 'Root-relative Windows target'
    Assert-Refused { Invoke-Fixture $f 'Deploy' @{ TemplateInstance = $f.Target } } 'missing' 'Missing distinct template'
    Assert-Refused { Invoke-Fixture $f 'Deploy' @{ JavaSha256 = ('0' * 64) } } 'Java executable SHA256' 'Wrong Java hash'
    Write-FixtureText (Join-Path $f.JavaHome 'release') "JAVA_VERSION=`"21.0.1`"`nOS_ARCH=`"x86_64`"`n"
    Assert-Refused { Invoke-Fixture $f } 'Java 17' 'Wrong Java major'
    $f = New-Fixture
    $f.Dirty = $true
    Assert-Refused { Invoke-Fixture $f } 'clean and committed' 'Dirty source'
    $f.Dirty = $false
    Assert-Refused { Invoke-Fixture $f 'Deploy' @{ ExpectedRevision = ('9' * 40) } } 'ExpectedRevision' 'Wrong exact source'
    Assert-Refused { Invoke-Fixture $f 'BuildAndDeploy' @{ ExpectedRevision = '1234567'; PackZip = '' } } 'full' 'Build requires full SHA before any JVM'
    $f.RuntimeDrift = $true
    Assert-Refused { Invoke-Fixture $f } 'Runtime' 'Unauthorized runtime ancestry/inputs'
    $f.RuntimeDrift = $false
    $mmc = Read-PrismJson (Join-Path $f.Template 'mmc-pack.json')
    $mmc.components[2].version = '0.0.0'
    Write-FixtureJson (Join-Path $f.Template 'mmc-pack.json') $mmc
    Assert-Refused { Invoke-Fixture $f } 'version mismatch' 'Template platform mismatch'
    $f = New-Fixture
    $mmc = Read-PrismJson (Join-Path $f.Template 'mmc-pack.json')
    $mmc.components[0].cachedVolatile = 'true'
    Write-FixtureJson (Join-Path $f.Template 'mmc-pack.json') $mmc
    Assert-Refused { Invoke-Fixture $f } 'boolean' 'Invalid cachedVolatile type'
    $f = New-Fixture
    Write-FixtureText (Join-Path $f.Template 'patches/custom.json') '{}'
    Assert-Refused { Invoke-Fixture $f } 'patches' 'Custom template patch'
    $f = New-Fixture
    $mmc = Read-PrismJson (Join-Path $f.Template 'mmc-pack.json')
    $mmc.components += [pscustomobject]@{ uid = 'custom.loader'; version = '1' }
    Write-FixtureJson (Join-Path $f.Template 'mmc-pack.json') $mmc
    Assert-Refused { Invoke-Fixture $f } 'exactly' 'Unknown enabled template component'

    $f = New-Fixture
    $manifest = Read-PrismJson (Join-Path $f.Root 'pack/curseforge/manifest.json')
    $manifest.files = @($manifest.files | Where-Object { $_.projectID -ne 908741 })
    Write-FixtureJson (Join-Path $f.Root 'pack/curseforge/manifest.json') $manifest
    Assert-Refused { Invoke-Fixture $f } 'Expanded dependency selection' 'Missing authoritative pin'
    $f = New-Fixture
    $path = Join-Path $f.Root 'gradle.properties'
    Write-FixtureText $path ([regex]::Replace([IO.File]::ReadAllText($path), '(?m)^jei_version=.*$', 'jei_version=invalid'))
    Assert-Refused { Invoke-Fixture $f } 'version mismatch' 'Gradle/dependency version mismatch'

    foreach ($change in @('invalid', 'mismatch')) {
        $f = New-Fixture
        $path = Join-Path $f.Root 'pack/dependency-metadata.json'
        $metadata = Read-PrismJson $path
        $pin = @($metadata.files | Where-Object { $_.projectID -eq 889915 })[0]
        $digest = 'invalid'
        $message = 'Invalid dependency SHA256 metadata'
        if ($change -ceq 'mismatch') { $digest = '0' * 64; $message = 'dependency SHA256 mismatch' }
        $pin | Add-Member -NotePropertyName sha256 -NotePropertyValue $digest -Force
        Write-FixtureJson $path $metadata
        if ($change -ceq 'mismatch') {
            New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic package with wrong declared dependency digest'))
        }
        $before = Get-FixtureSnapshot $f.Template
        Assert-Refused { Invoke-Fixture $f } $message "Published dependency SHA256 $change refuses installation"
        Assert-Test (-not [IO.Directory]::Exists($f.Target)) 'Bad published SHA256 creates no target'
        Assert-Test ((Get-FixtureSnapshot $f.Template) -ceq $before) 'Bad published SHA256 never mutates the template'
    }

    foreach ($profileName in @('preparation', 'playable')) {
        foreach ($id in @(289412, 404465, 404468, 889915, 324717, 845831, 570073)) {
            $f = New-Fixture
            $path = Join-Path $f.Root 'pack/profiles.json'
            $profiles = Read-PrismJson $path
            if ($id -in @(289412, 404465, 404468, 889915)) {
                $profiles.$profileName.projectIDs = @($profiles.$profileName.projectIDs | Where-Object { $_ -ne $id })
            } else { $profiles.$profileName.projectIDs += $id }
            Write-FixtureJson $path $profiles
            Assert-Refused { Invoke-Fixture $f } ("$profileName selection") `
                "Exact $profileName selection rejects missing native/foreign project $id"
            Assert-Test (-not [IO.Directory]::Exists($f.Target)) 'Invalid selection never creates a target'
        }
    }
    foreach ($id in @(1, 845831, 570073)) {
        $f = New-Fixture
        $manifestPath = Join-Path $f.Root 'pack/curseforge/manifest.json'
        $metadataPath = Join-Path $f.Root 'pack/dependency-metadata.json'
        $manifest = Read-PrismJson $manifestPath
        $metadata = Read-PrismJson $metadataPath
        $pin = (ConvertTo-Json $manifest.files[0] -Depth 20) | ConvertFrom-Json
        $record = (ConvertTo-Json $metadata.files[0] -Depth 20) | ConvertFrom-Json
        $pin.projectID = $id; $record.projectID = $id
        $manifest.files += $pin; $metadata.files += $record
        Write-FixtureJson $manifestPath $manifest
        Write-FixtureJson $metadataPath $metadata
        Assert-Refused { Invoke-Fixture $f } 'Expanded dependency selection' `
            "Foreign/legacy project $id is not authorized by matching metadata"
    }
    foreach ($change in @('missing', 'duplicate', 'foreign', 'file-pin')) {
        $f = New-Fixture
        $path = Join-Path $f.Root 'pack/dependency-metadata.json'
        $metadata = Read-PrismJson $path
        switch ($change) {
            'missing' { $metadata.files = @($metadata.files | Where-Object { $_.projectID -ne 324717 }) }
            'duplicate' { $metadata.files += $metadata.files[0] }
            'foreign' {
                $record = (ConvertTo-Json $metadata.files[0] -Depth 20) | ConvertFrom-Json
                $record.projectID = 1
                $metadata.files += $record
            }
            'file-pin' { @($metadata.files | Where-Object { $_.projectID -eq 324717 })[0].fileID++ }
        }
        Write-FixtureJson $path $metadata
        Assert-Refused { Invoke-Fixture $f } 'metadata pin parity' `
            "Complete manifest/metadata parity rejects $change unselected metadata"
    }
    foreach ($id in $nativePins.Keys) {
        foreach ($change in @('filename', 'paired-pin', 'closure', 'missing-version', 'missing-file')) {
            $f = New-Fixture
            $manifestPath = Join-Path $f.Root 'pack/curseforge/manifest.json'
            $metadataPath = Join-Path $f.Root 'pack/dependency-metadata.json'
            $manifest = Read-PrismJson $manifestPath
            $metadata = Read-PrismJson $metadataPath
            $pin = @($manifest.files | Where-Object { $_.projectID -eq $id })[0]
            $record = @($metadata.files | Where-Object { $_.projectID -eq $id })[0]
            $message = 'version mismatch'
            switch ($change) {
                'filename' { $record.fileName = 'wrong-version.jar' }
                'paired-pin' { $pin.fileID++; $record.fileID = $pin.fileID; $message = 'FTB quest file pin mismatch' }
                'closure' { $record.requiredProjectIDs = @(1); $message = 'Missing required dependency project' }
                { $_ -in @('missing-version', 'missing-file') } {
                    $suffix = '_version'
                    if ($change -ceq 'missing-file') { $suffix = '_curse_file' }
                    $key = $nativePins[$id][1] + $suffix
                    $path = Join-Path $f.Root 'gradle.properties'
                    Write-FixtureText $path ([regex]::Replace([IO.File]::ReadAllText($path),
                        ('(?m)^' + $key + '=.*$'), ''))
                    $message = 'Missing required Gradle version property'
                }
            }
            Write-FixtureJson $manifestPath $manifest
            Write-FixtureJson $metadataPath $metadata
            Assert-Refused { Invoke-Fixture $f } $message "Native FTB $id refuses $change"
            Assert-Test (-not [IO.Directory]::Exists($f.Target)) "Native FTB $id invalid contract installs nothing"
        }
    }
    $f = New-Fixture
    $path = Join-Path $f.Root 'pack/dependency-metadata.json'
    $metadata = Read-PrismJson $path
    @($metadata.files | Where-Object { $_.projectID -eq 289412 })[0].requiredProjectIDs += 324717
    Write-FixtureJson $path $metadata
    Assert-Refused { Invoke-Fixture $f } 'Missing required dependency project' 'Playable dependency closure cannot borrow an expanded-only project'

    # Coherent new source pins still cannot migrate any dependency in an existing instance.
    foreach ($id in $nativePins.Keys) {
        $f = New-Fixture
        $null = Invoke-Fixture $f
        $before = Get-FixtureSnapshot $f.Target
        $downloadsBefore = $script:DownloadCalls
        $manifestPath = Join-Path $f.Root 'pack/curseforge/manifest.json'
        $metadataPath = Join-Path $f.Root 'pack/dependency-metadata.json'
        $manifest = Read-PrismJson $manifestPath
        $metadata = Read-PrismJson $metadataPath
        $pin = @($manifest.files | Where-Object { $_.projectID -eq $id })[0]
        $pin.fileID++
        @($metadata.files | Where-Object { $_.projectID -eq $id })[0].fileID = $pin.fileID
        Write-FixtureJson $manifestPath $manifest
        Write-FixtureJson $metadataPath $metadata
        $key = $nativePins[$id][1] + '_curse_file'
        $path = Join-Path $f.Root 'gradle.properties'
        Write-FixtureText $path ([regex]::Replace([IO.File]::ReadAllText($path),
            ('(?m)^' + $key + '=.*$'), ($key + '=' + $pin.fileID)))
        $f.Source = '4' * 40
        New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('synthetic runtime with changed native pin'))
        foreach ($action in @('Prepare', 'Deploy', 'BuildAndDeploy')) {
            Assert-Refused { Invoke-Fixture $f $action @{ PackZip = '' } } 'pin migrations require a new instance' `
                "Native FTB $id $action cannot update installed pins"
        }
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $before) "Native FTB $id migration refusal preserves the entire old instance"
        Assert-Test ($script:DownloadCalls -eq $downloadsBefore -and $f.BuildLaunches -eq 0) `
            'Pin migration refusal precedes all downloads and builds'
    }
    foreach ($legacySelection in @('heracles-seven', 'ftb-eight')) {
        $f = New-Fixture
        $null = Invoke-Fixture $f
        $path = Join-Path $f.Target $script:PrismDevReceiptFile
        $receipt = Read-PrismJson $path
        # Synthesize only the old selection; no legacy binaries are adopted or required.
        if ($legacySelection -ceq 'heracles-seven') {
            $legacyQuest = @($receipt.dependencies | Where-Object { $_.projectID -eq 289412 })[0]
            $legacyLibrary = @($receipt.dependencies | Where-Object { $_.projectID -eq 404465 })[0]
            $legacyQuest = (ConvertTo-Json $legacyQuest -Depth 20) | ConvertFrom-Json
            $legacyLibrary = (ConvertTo-Json $legacyLibrary -Depth 20) | ConvertFrom-Json
            $legacyQuest.projectID = 845831; $legacyLibrary.projectID = 570073
            $receipt.dependencies = @($receipt.dependencies | Where-Object {
                $_.projectID -notin @(289412, 404465, 404468, 889915)
            }) + @($legacyQuest, $legacyLibrary)
        } else {
            $receipt.dependencies = @($receipt.dependencies | Where-Object { $_.projectID -ne 889915 })
        }
        Write-FixtureJson $path $receipt
        $before = Get-FixtureSnapshot $f.Target
        foreach ($action in @('Inspect', 'Prepare', 'Deploy', 'BuildAndDeploy')) {
            Assert-Refused { Invoke-Fixture $f $action @{ PackZip = '' } } 'Installed nine-project selection' `
                "Legacy $legacySelection instance refuses $action"
        }
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $before) "Old $legacySelection selection is never migrated in place"
    }

    $f = New-Fixture
    Write-FixtureJson (Join-Path $f.Root 'pack/public-overrides.json') @{
        schemaVersion = 1; files = @(@{ source = 'public.cfg'; destination = 'config/public.cfg' })
    }
    Assert-Refused { Invoke-Fixture $f } 'Nonempty public overrides' 'Future selected overrides never silently dropped'
    Assert-Test (-not [IO.Directory]::Exists($f.Target)) 'Unsupported public overrides fail before installation'

    foreach ($change in @('source', 'status', 'worlds', 'dependencies', 'manifest', 'override')) {
        $f = New-Fixture
        $script:ProvenanceCase = $change
        New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('fixture runtime')) {
            param($data)
            switch ($script:ProvenanceCase) {
                'source' { $data.Provenance.sourceRevision = 'f' * 40 }
                'status' { $data.Provenance.status = 'release-candidate' }
                'worlds' { $data.Provenance.worldsIncluded = $true }
                'dependencies' { $data.Provenance.dependencies[0].fileID++ }
                'manifest' { $data.Manifest.version = 'wrong-version' }
                'override' { $data.Provenance.publicOverrides.files = @(@{ destination = 'config/hidden.cfg' }) }
            }
        }
        $message = 'provenance'
        if ($change -ceq 'manifest') { $message = 'manifest' }
        Assert-Refused { Invoke-Fixture $f } $message "Wrong archived $change provenance/manifest"
        Assert-Test (-not [IO.Directory]::Exists($f.Target)) "Wrong $change creates no target"
    }
    $f = New-Fixture
    New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('fixture runtime')) -BadJar
    Assert-Refused { Invoke-Fixture $f } 'SHA256 mismatch' 'Tampered archived remapped jar'
    foreach ($entry in @('../escape.jar', 'overrides/config/private.cfg', 'overrides/mods/extra.jar', 'saves/world/level.dat')) {
        $f = New-Fixture
        New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('fixture runtime')) -Extra $entry
        Assert-Refused { Invoke-Fixture $f } 'whitelist' "Unselected archive entry $entry"
        Assert-Test (-not [IO.Directory]::Exists($f.Target)) 'Archive refusal precedes installation'
    }
    $f = New-Fixture
    New-FixtureZip $f ([Text.Encoding]::UTF8.GetBytes('fixture runtime')) -Symlink
    Assert-Refused { Invoke-Fixture $f } 'symlink' 'Archive symlink'
    $f = New-Fixture
    $first = (Get-PrismContract $f.Root).Dependencies[0]
    Write-FixtureText (Join-Path $f.Template ('.minecraft/mods/' + $first.fileName)) 'corrupted cached candidate'
    Assert-Refused { Invoke-Fixture $f } 'published SHA1' 'Bad local pinned file'
    $f = New-Fixture
    $first = (Get-PrismContract $f.Root).Dependencies[0]
    $url = Get-PrismOfficialUrl $first
    Assert-Test ($url -ceq ("https://edge.forgecdn.net/files/" + [Math]::Floor($first.fileID / 1000) +
        '/' + ($first.fileID % 1000) + '/' + [Uri]::EscapeDataString($first.fileName))) 'Only pinned original official CDN URL is derived'
    [IO.File]::Delete((Join-Path $f.Template ('.minecraft/mods/' + $first.fileName)))
    $downloadsBefore = $script:DownloadCalls
    $result = Invoke-Fixture $f 'Prepare'
    Assert-Test ($result.MissingLocalDependencies.Count -eq 1 -and $script:DownloadCalls -eq $downloadsBefore) 'Prepare reports missing pin without network'
    Assert-Refused { Invoke-Fixture $f } 'CDN failure' 'Download failure before install'
    Assert-Test (-not [IO.Directory]::Exists($f.Target)) 'Failed download creates no target or success receipt'
    Assert-Test (@(Get-ChildItem -LiteralPath $f.Instances -Force).Count -eq 1) 'Failed stage/lock cleaned without changing template'
    $f.AllowDownload = $true; $f.BadDownload = $true
    Assert-Refused { Invoke-Fixture $f } 'published SHA1' 'Downloaded wrong bytes'
    $f.BadDownload = $false
    $result = Invoke-Fixture $f
    Assert-Test ($result.Outcome -ceq 'Deployed') 'Exact official-download fixture is independently size/hash checked'
    Assert-Test ($result.MissingLocalDependencies.Count -eq 0) 'Successful download leaves no reported missing dependency'
    Assert-Test ($result.DownloadedDependencies.Count -eq 1 -and
        $result.DownloadedDependencies[0] -ceq "$($first.projectID)/$($first.fileID)") 'Successful download reports the exact fetched pin separately'
    $f = New-Fixture
    $first = (Get-PrismContract $f.Root).Dependencies[0]
    [IO.File]::Delete((Join-Path $f.Template ('.minecraft/mods/' + $first.fileName)))
    $f.AllowDownload = $true; $f.TargetDuringDownload = $true
    Assert-Refused { Invoke-Fixture $f } 'appeared during staging' 'Concurrent unowned target'
    Assert-Test ([IO.File]::ReadAllText((Join-Path $f.Target 'preserve-me.txt')) -ceq 'concurrent unowned target') 'Concurrent target preserved'
    $f = New-Fixture
    $staleLock = Join-Path $f.Instances ('.' + $f.InstanceId + '.grindless-deploy.lock')
    Write-FixtureText $staleLock 'unknown stale lock'
    Assert-Refused { Invoke-Fixture $f } 'stale deployment lock' 'Uninspectable deployment lock'
    Assert-Test ([IO.File]::ReadAllText($staleLock) -ceq 'unknown stale lock') 'Unknown lock is not deleted'

    # Directory junctions exercise real Windows reparse handling without symlink privilege.
    $f = New-Fixture
    $outside = Join-Path $f.Home 'outside'
    Write-FixtureText (Join-Path $outside 'preserve.txt') 'outside marker'
    New-Item -ItemType Junction -Path $f.Target -Target $outside | Out-Null
    $script:Junctions += $f.Target
    Assert-Refused { Invoke-Fixture $f } 'Reparse' 'Reparse target'
    Assert-Test ([IO.File]::ReadAllText((Join-Path $outside 'preserve.txt')) -ceq 'outside marker') 'Reparse destination unchanged'
    [IO.Directory]::Delete($f.Target)
    $script:Junctions = @($script:Junctions | Where-Object { $_ -cne $f.Target })
    $null = Invoke-Fixture $f
    $nested = Join-Path $f.Target 'minecraft/config'
    New-Item -ItemType Junction -Path $nested -Target $outside | Out-Null
    $script:Junctions += $nested
    Assert-Refused { Invoke-Fixture $f } 'Reparse' 'Nested reparse in retained config'
    [IO.Directory]::Delete($nested)
    $script:Junctions = @($script:Junctions | Where-Object { $_ -cne $nested })
    Write-Output 'PASS: deployment create/idempotent/update, preservation, rollback and safety refusals.'

    # Build-only is deliberately independent of instance ownership/running state.
    $f = New-Fixture
    Write-FixtureText (Join-Path $f.Target 'minecraft/saves/playing/level.dat') 'player is using the previous deployed jar'
    $f.Processes = @([pscustomobject]@{ Name = 'javaw.exe'; ExecutablePath = $f.Java
        CommandLine = ('javaw --gameDir "' + $f.Target + '\minecraft" --accessToken TOKEN_DO_NOT_PRINT') })
    $playerBefore = Get-FixtureSnapshot $f.Target
    $templateBefore = Get-FixtureSnapshot $f.Template
    $previousJava = $env:JAVA_HOME; $previousGradle = $env:GRADLE_USER_HOME
    try {
        $env:JAVA_HOME = $f.JavaHome
        $env:GRADLE_USER_HOME = Join-Path $f.Home 'isolated-gradle-build'
        $result = Invoke-Fixture $f 'Build' @{ PackZip = ''; InstancesRoot = 'C:never-inspect-this-target' }
        Assert-Test ($result.Outcome -ceq 'BuiltVerified' -and $f.BuildLaunches -eq 1) 'Build permits attributable player game with measured headroom'
        Assert-Test ($f.ResourceReads -eq 1) 'Build takes a fresh resource snapshot immediately before Gradle'
        Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $playerBefore) 'Build never reads/adopts/mutates the unowned running target'
        Assert-Test ((Get-FixtureSnapshot $f.Template) -ceq $templateBefore) 'Build does not access or change the template'
        foreach ($argument in @('verifyModpack', 'buildModpack', '-PpackProfile=playable', '--no-daemon', '--no-parallel', '--max-workers=1')) {
            Assert-Test ($argument -cin $f.GradleArguments) "Build reuses bounded existing infrastructure $argument"
        }
        foreach ($failure in @('physical', 'commit', 'disk', 'unknown')) {
            $f.Resources = [pscustomobject]@{ AvailablePhysicalMiB = 16384; AvailableCommitMiB = 32768
                WorkDisks = @([pscustomobject]@{ AvailableBytes = 64GB }) }
            switch ($failure) {
                'physical' { $f.Resources.AvailablePhysicalMiB = 6143 }
                'commit' { $f.Resources.AvailableCommitMiB = 8191 }
                'disk' { $f.Resources.WorkDisks[0].AvailableBytes = 8GB - 1 }
                'unknown' { $f.Resources = $null }
            }
            $message = 'Insufficient build headroom'
            if ($failure -ceq 'unknown') { $message = 'Unknown build resource metrics' }
            Assert-Refused { Invoke-Fixture $f 'Build' @{ PackZip = '' } } $message "Build $failure resources"
            Assert-Test ($f.BuildLaunches -eq 1) "No Gradle launch on $failure resource failure"
            Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $playerBefore) "Resource failure $failure leaves game untouched"
        }
        $f.Resources = [pscustomobject]@{ AvailablePhysicalMiB = 16384; AvailableCommitMiB = 32768
            WorkDisks = @([pscustomobject]@{ AvailableBytes = 64GB }) }
        $f.Processes = @([pscustomobject]@{ Name = 'java.exe'; ExecutablePath = $f.Java
            CommandLine = 'java org.gradle.launcher.daemon.bootstrap.GradleDaemon TOKEN_DO_NOT_PRINT' })
        Assert-Refused { Invoke-Fixture $f 'Build' @{ PackZip = '' } } 'Competing agent/Gradle' 'Competing agent Gradle JVM'
        Assert-Test ($f.BuildLaunches -eq 1) 'No duplicate agent build launch'
        $f.Processes = @()
        $lockPath = Join-Path $f.Root 'build/.grindless-prism-build.lock'
        Write-FixtureText $lockPath 'preserve active or stale agent build lock'
        Assert-Refused { Invoke-Fixture $f 'Build' @{ PackZip = '' } } 'build lock exists' 'Concurrent worktree build lock'
        Assert-Test ([IO.File]::ReadAllText($lockPath) -ceq 'preserve active or stale agent build lock') 'Unknown build lock is not deleted'
        [IO.File]::Delete($lockPath)
        Assert-Test (-not [IO.File]::Exists((Join-Path $env:GRADLE_USER_HOME '.grindless-prism-build.lock'))) 'All acquired cache locks released on failure'
    } finally { $env:JAVA_HOME = $previousJava; $env:GRADLE_USER_HOME = $previousGradle }
    Write-Output 'PASS: Build-only while playing, actual-heap headroom and competing-build refusals; no JVM launched.'

    # Transport/config tests never start SSH. Test hostile quoting as JSON DATA.
    $f = New-Fixture
    $private = Join-Path $f.Home 'private-pc.json'
    $privateData = New-PcFixtureConfig $f
    Write-FixtureJson $private $privateData
    $script:TransportExit = 23
    $code = Invoke-PcWorkflow -ConfigFile $private -Action Sync -Revision ('2' * 40)
    Assert-Test ($code -eq 23) 'SSH nonzero exit code propagated unchanged'
    Assert-Test ($script:CapturedSshTarget -ceq 'fixture-user@fixture-pc') 'Existing configured SSH identity used'
    Assert-Test ($script:CapturedPacket.Command.Length -lt 7000) 'Fixed encoded command fits Windows SSH cap'
    Assert-Test ($script:CapturedPacket.Command.Contains(' -OutputFormat Text ')) 'Remote PowerShell emits text rather than CLIXML'
    Assert-Test ($script:CapturedPacket.Json.IndexOfAny([char[]]"`r`n") -eq -1) 'Stdin packet is exactly one compact JSON line'
    Assert-Test (-not $script:CapturedPacket.Command.Contains($privateData.worktreePath)) 'Private paths absent from remote command line'
    $packet = $script:CapturedPacket.Json | ConvertFrom-Json
    Assert-Test ($packet.request.config.worktreePath -ceq $privateData.worktreePath) 'Spaces, quotes, metacharacters and Unicode survive JSON roundtrip'
    Assert-Test ($packet.request.revision -ceq ('2' * 40)) 'Exact requested revision sent as data'
    $code = Invoke-PcWorkflow -ConfigFile $private -Action Build -Revision ('2' * 40)
    Assert-Test (($script:CapturedPacket.Json | ConvertFrom-Json).request.action -ceq 'Build') 'Outer workflow exposes Build-only action'
    $bootstrap = [Text.Encoding]::Unicode.GetString([Convert]::FromBase64String(($script:CapturedPacket.Command -split ' ')[-1]))
    Assert-Test ($bootstrap.Contains("`$ProgressPreference='SilentlyContinue'")) 'Fixed bootstrap suppresses module-autoload progress'
    Assert-Test ($bootstrap.Contains('[Console]::In.ReadLine()') -and
        -not $bootstrap.Contains('[Console]::In.ReadToEnd()')) 'Bootstrap consumes one packet line without waiting for SSH EOF'
    Assert-Test ($bootstrap.Contains('UTF8.GetByteCount($line) -gt 262144')) 'Bootstrap enforces the 256 KiB UTF-8 packet limit before JSON parsing'
    $tokens = $null; $errors = $null
    $null = [Management.Automation.Language.Parser]::ParseInput($bootstrap, [ref]$tokens, [ref]$errors)
    Assert-Test ($errors.Count -eq 0) 'Fixed bootstrap parses in WinPS 5.1'
    $decoded = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($packet.code))
    Assert-Test ($decoded -ceq $script:PcWorkflowRemote.ToString()) 'Transport uses only the fixed reviewed workflow'
    $null = [Management.Automation.Language.Parser]::ParseInput($decoded, [ref]$tokens, [ref]$errors)
    Assert-Test ($errors.Count -eq 0) 'Fixed remote workflow parses in WinPS 5.1'
    $oversizedConfig = (ConvertTo-Json $privateData -Depth 10) | ConvertFrom-Json
    $oversizedConfig.worktreePath = 'C:\' + (([string][char]0xE9) * 131072)
    Assert-Refused { New-PcWorkflowPacket $oversizedConfig 'Sync' ('1' * 40) '' } '256 KiB' 'Oversized UTF-8 stdin packet'
    Assert-Refused { Invoke-PcWorkflow -ConfigFile $private -Action Sync -Revision 'main' } 'full commit SHA' 'No branch name in Revision'
    Assert-Refused { Invoke-PcWorkflow -ConfigFile $private -Action Deploy -Revision ('1' * 40) } 'absolute' 'Deploy requires explicit remote PackZip'
    Assert-Refused { Invoke-PcWorkflow -ConfigFile $private -Action Sync -Revision ('1' * 40) -PackZip 'D:\pack.zip' } 'only accepted' 'No ignored PackZip'
    foreach ($bad in @('sshTarget', 'schema', 'worktreePath', 'instanceId', 'receivedSourceRevision', 'personalGitSshAlias')) {
        $broken = (ConvertTo-Json $privateData -Depth 10) | ConvertFrom-Json
        switch ($bad) {
            'sshTarget' { $broken.sshTarget = '-oProxyCommand=anything' }
            'schema' { $broken.schema = 'wrong' }
            'worktreePath' { $broken.worktreePath = $broken.clonePath + '\nested' }
            'instanceId' { $broken.instanceId = $broken.templateInstanceId }
            'receivedSourceRevision' { $broken.receivedSourceRevision = 'main' }
            'personalGitSshAlias' { $broken.personalGitSshAlias = 'fixture; command' }
        }
        Write-FixtureJson $private $broken
        Assert-Refused { Read-PcWorkflowConfig $private } '' "Invalid private config $bad"
    }
    Write-FixtureJson $private $privateData
    function ssh {
        $script:SshArgv = @($args)
        $script:SshStdin = $input -join "`n"
        $global:LASTEXITCODE = 37
        Write-Output 'synthetic SSH stdout'
    }
    $encodingBefore = $OutputEncoding
    $code = & $script:OriginalTransport 'fixture-pc' $script:CapturedPacket
    Assert-Test ($code -eq 37) 'Concrete transport propagates SSH executable exit'
    foreach ($option in @('-T', 'BatchMode=yes', 'StrictHostKeyChecking=yes', 'ConnectTimeout=15', 'ConnectionAttempts=1',
            'ServerAliveInterval=15', 'ServerAliveCountMax=3')) {
        Assert-Test ($option -cin $script:SshArgv) "SSH transport requires $option"
    }
    Assert-Test ($script:SshStdin.Trim() -ceq $script:CapturedPacket.Json) 'JSON travels on stdin, not a long remote argument'
    Assert-Test ($OutputEncoding -eq $encodingBefore) 'Transport restores process pipe encoding'
    Remove-Item Function:\ssh

    $f = New-Fixture
    $remote = New-RemoteFixture $f
    $result = Invoke-RemoteFixture $remote 'Sync'
    Assert-Test ($result.Outcome -ceq 'VerifiedDetached') 'Sync materializes exact detached fixture worktree'
    $fetch = @($remote.Calls | Where-Object { $_.Arguments[0] -ceq 'fetch' })
    Assert-Test ($fetch.Count -eq 1 -and $fetch[0].At -ceq $remote.Config.clonePath) 'Fetch only through original clone'
    Assert-Test (($fetch[0].Arguments -join '|') -ceq ('fetch|--no-tags|--no-recurse-submodules|origin|' + ('1' * 40))) 'Fetch exact SHA, no branch/refspec drift'
    $result = Invoke-RemoteFixture $remote 'Sync' ('2' * 40)
    Assert-Test ($remote.Head -ceq ('2' * 40)) 'Sync advances only clean detached worktree'
    Assert-Test ($remote.CloneHead -ceq ('a' * 40)) 'Clone HEAD preserved'
    Write-FixtureText (Join-Path $f.Target 'minecraft/saves/playing/level.dat') 'active player fixture'
    $playerBefore = Get-FixtureSnapshot $f.Target
    $remote.Processes = @([pscustomobject]@{ Name = 'javaw.exe'; ExecutablePath = $f.Java
        CommandLine = ('javaw --gameDir "' + $f.Target + '\minecraft" --accessToken TOKEN_DO_NOT_PRINT') })
    $null = Invoke-RemoteFixture $remote 'Sync' ('2' * 40)
    Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $playerBefore) 'Sync does not stop/reconfigure the player game'
    $syncLockPath = Join-Path $remote.Config.worktreePath 'build/.grindless-prism-build.lock'
    Write-FixtureText $syncLockPath 'active native build'
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'build lock exists' 'Sync during native build'
    Assert-Test ([IO.File]::ReadAllText($syncLockPath) -ceq 'active native build') 'Sync preserves another build lock'
    [IO.File]::Delete($syncLockPath)
    $remote.Processes = @([pscustomobject]@{ Name = 'java.exe'; ExecutablePath = $f.Java
        CommandLine = ('java org.gradle.wrapper.GradleWrapperMain -p "' + $remote.Config.worktreePath + '" TOKEN_DO_NOT_PRINT') })
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'active/shared execution-worktree build' 'Sync during external worktree build'
    $remote.Processes = @()
    Assert-Test (@($remote.Calls | Where-Object {
        $_.Arguments[0] -cin @('reset', 'clean', 'config', 'branch') -or '--force' -cin $_.Arguments
    }).Count -eq 0) 'No reset/clean/config/branch/force commands'
    $fetchCount = @($remote.Calls | Where-Object { $_.Arguments[0] -ceq 'fetch' }).Count
    $remote.Dirty = $true
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'dirty' 'Sync dirty worktree'
    Assert-Test (@($remote.Calls | Where-Object { $_.Arguments[0] -ceq 'fetch' }).Count -eq $fetchCount) 'Dirty worktree rejected even before fetch'
    $remote.Dirty = $false; $remote.Attached = $true
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'detached' 'Sync attached worktree'
    $remote.Attached = $false; $remote.Unrelated = $true
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'verification/synchronization failed' 'Sync non-ancestral source'
    Assert-Test ($remote.Head -ceq ('2' * 40)) 'Rejected ancestry leaves worktree at prior revision'
    $remote.Unrelated = $false; $remote.FetchFailure = $true
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'verification/synchronization failed' 'Failed fetch'
    Assert-Test ($remote.Head -ceq ('2' * 40)) 'Failed fetch cannot produce success state'
    $remote.FetchFailure = $false; $remote.Origin = 'git@wrong-identity:fixture/repo.git'
    Assert-Refused { Invoke-RemoteFixture $remote 'Sync' ('3' * 40) } 'existing personal Git SSH alias' 'Origin identity mismatch is preserved'
    $remote.Origin = 'git@fixture-personal:fixture/repository.git'
    Assert-Refused { Invoke-RemoteFixture $remote 'BuildAndDeploy' ('3' * 40) } 'Run Sync first' 'No implicit sync/build on wrong SHA'
    # A harmless native fixture captures dispatch/environment; it is not the real tool.
    $nativeFixture = @'
param($Action,$Root,$InstancesRoot,$InstanceId,$ExpectedRevision,$TemplateInstance,$JavaPath,$JavaSha256,$PackZip)
[pscustomobject]@{ Action=$Action; Revision=$ExpectedRevision; Pack=$PackZip; Root=$Root
 JavaHome=$env:JAVA_HOME; GradleHome=$env:GRADLE_USER_HOME; JavaPath=$JavaPath; JavaHash=$JavaSha256 }
'@
    Write-FixtureText (Join-Path $remote.Config.worktreePath 'tools/prism-dev.ps1') $nativeFixture
    $packPath = Join-Path $f.Home "literal & (safe)' pack.zip"
    Write-FixtureText $packPath 'dispatch-only fixture'
    $oldJava = $env:JAVA_HOME; $oldGradle = $env:GRADLE_USER_HOME
    $result = Invoke-RemoteFixture $remote 'Deploy' ('2' * 40) $packPath
    Assert-Test ($result.Pack -ceq $packPath -and $result.Revision -ceq ('2' * 40)) 'Native Deploy receives exact literal remote archive and revision'
    Assert-Test ($result.JavaHome -ceq $f.JavaHome -and $result.GradleHome -ceq $remote.Config.gradleUserHome) 'Only child-process environment selects JDK/cache'
    Assert-Test ($env:JAVA_HOME -ceq $oldJava -and $env:GRADLE_USER_HOME -ceq $oldGradle) 'Remote dispatcher restores environment'
    Assert-Test ($result.JavaHash -ceq $f.JavaHash) 'Dispatcher supplies explicit verified Java executable hash without launching it'
    $result = Invoke-RemoteFixture $remote 'BuildAndDeploy' ('2' * 40)
    Assert-Test ($result.Action -ceq 'BuildAndDeploy') 'BuildAndDeploy is dispatched only after exact detached source check'
    $remote.Config.prismRoot = Join-Path $f.Home 'deliberately-missing-prism'
    $result = Invoke-RemoteFixture $remote 'Build' ('2' * 40)
    Assert-Test ($result.Action -ceq 'Build') 'Remote Build never inspects Prism/template/target existence'
    Assert-Test ((Get-FixtureSnapshot $f.Target) -ceq $playerBefore) 'Remote Build keeps the playing instance unchanged'
    Write-Output 'PASS: private config, fixed stdin SSH transport, exact detached Sync and literal native dispatch.'
    Test-PrismQuestDefinitionTransactions
    Write-Output ("ALL PRISM/PC WORKFLOW CHECKS PASSED: " + $script:Assertions + " assertions; " + $script:Cases + " isolated fixtures.")
    Write-Output 'No JVM, network, SSH, real Git writes, launcher, cache or player instance was used.'
} finally {
    Set-Item Function:\Set-PrismReplacement $script:OriginalReplacement
    foreach ($junction in $script:Junctions) {
        if ([IO.Directory]::Exists($junction)) { [IO.Directory]::Delete($junction) }
    }
    if ([IO.Directory]::Exists($script:TestTemp)) { Remove-Item -LiteralPath $script:TestTemp -Recurse -Force }
}
