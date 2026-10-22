#requires -Version 5.1
<#
.SYNOPSIS
Build a playable package, or preflight/deploy/inspect one owned Prism engineering instance.
.DESCRIPTION
Prepare is read-only (including no downloads). Deploy requires an explicit playable
PackZip and clean committed source matching its provenance. Build does not inspect
Prism and can run while the player uses the previous deployment, subject to measured
headroom and build locks. Build/BuildAndDeploy require ExpectedRevision, JAVA_HOME and
GRADLE_USER_HOME and use existing verifyModpack/buildModpack tasks with one worker.
No action launches Java for a probe,
Prism or Minecraft. JavaSha256 must identify the operator-selected Java 17 executable.
Only component uid/version metadata and exact hash-matched dependency jars are read
from TemplateInstance. Public overrides must currently be explicitly empty.
UpdateQuestDefinitions applies only an explicitly hash-selected native publisher plan
to the stopped owned target. RecoverQuestDefinitions restores its verified definition
backups, never world/team data. Neither action builds, downloads or launches Java.
Deploy the qualified current package while stopped, then produce a fresh native plan
and apply it before starting the game. Old incomplete chapters keep startup fail-closed.
Recovery restores definitions only, not the runtime: retry the update or explicitly
restore a compatible verified package before starting.
#>
[CmdletBinding()]
param(
    [ValidateSet('Prepare', 'Deploy', 'Inspect', 'Build', 'BuildAndDeploy', 'UpdateQuestDefinitions', 'RecoverQuestDefinitions')][string]$Action = 'Inspect',
    [string]$Root,
    [string]$InstancesRoot,
    [string]$InstanceId = 'Grindless-T0-Dev',
    [string]$TemplateInstance,
    [string]$JavaPath,
    [string]$JavaSha256,
    [string]$PackZip,
    [string]$ExpectedRevision,
    [string]$RuntimeRevision,
    [string]$QuestUpdatePlan,
    [string]$QuestUpdateSha256,
    [string[]]$DependencyDirectory = @()
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
$script:PrismDevTool = 'grindless-prism-dev'
$script:PrismDevStatus = 'restricted-playable-profile-not-rc'
$script:PrismDevPreparationProjects = @(419699, 238222, 289412, 404465, 404468, 889915)
$script:PrismDevProjects = $script:PrismDevPreparationProjects + @(790626, 429235, 908741)
$script:PrismDevExpandedProjects = $script:PrismDevProjects + @(324717, 422301, 618298, 410811, 314906)
$script:PrismDevQuestCoordinates = @{
    289412 = @('ftb-quests', 'ftbquests')
    404465 = @('ftb-library', 'ftblibrary')
    404468 = @('ftb-teams', 'ftbteams')
    889915 = @('ftb-xmod-compat', 'ftbxmodcompat')
}
$script:PrismDevOwnerFile = '.grindless-dev-owner.json'
$script:PrismDevReceiptFile = '.grindless-dev-deployment.json'
$script:PrismDevPendingFile = '.grindless-dev-pending.json'
$script:PrismQuestReceiptFile = 'minecraft\config\ftbquests\.grindless-definitions.json'
$script:PrismQuestPendingFile = 'minecraft\config\ftbquests\.grindless-definition-update.json'
$script:PrismQuestFilenames = @('grindless_t0.snbt', 'grindless_t1.snbt')
$script:PrismDevRepositoryRoot = Split-Path -Parent $PSScriptRoot

function Get-PrismCanonicalJson($Value) {
    if ($null -eq $Value) { return 'null' }
    if ($Value -is [System.Collections.IDictionary]) {
        $ordered = [ordered]@{}
        foreach ($key in @($Value.Keys | Sort-Object -CaseSensitive)) {
            $ordered[$key] = Get-PrismCanonicalJson $Value[$key]
        }
        return '{' + (($ordered.GetEnumerator() | ForEach-Object {
            (ConvertTo-Json ([string]$_.Key) -Compress) + ':' + $_.Value
        }) -join ',') + '}'
    }
    if ($Value -is [System.Management.Automation.PSCustomObject]) {
        $map = @{}
        foreach ($property in $Value.PSObject.Properties) { $map[$property.Name] = $property.Value }
        return Get-PrismCanonicalJson $map
    }
    if ($Value -is [System.Collections.IList]) {
        return '[' + ((@($Value | ForEach-Object { Get-PrismCanonicalJson $_ })) -join ',') + ']'
    }
    return ConvertTo-Json $Value -Compress -Depth 40
}

function Assert-PrismEqual($Actual, $Expected, [string]$Label) {
    if ((Get-PrismCanonicalJson $Actual) -cne (Get-PrismCanonicalJson $Expected)) {
        throw "$Label differs from the committed pack contract."
    }
}

function Assert-PrismKeys($Value, [string[]]$Keys, [string]$Label) {
    if ($null -eq $Value -or $Value -isnot [System.Management.Automation.PSCustomObject]) {
        throw "Invalid $Label object."
    }
    Assert-PrismEqual @($Value.PSObject.Properties.Name | Sort-Object -CaseSensitive) `
        @($Keys | Sort-Object -CaseSensitive) "$Label fields"
}

function Assert-PrismRevision([string]$Revision) {
    if ($Revision -cnotmatch '^[0-9a-f]{40}$') { throw 'A full lowercase 40-character commit SHA is required.' }
}

function Assert-PrismLeaf([string]$Name) {
    if ($Name -cnotmatch '^[A-Za-z0-9][A-Za-z0-9._+-]*$' -or $Name.EndsWith('.') -or
        ($Name.Split('.')[0] -match '^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$')) {
        throw 'Unsafe file or instance name.'
    }
}

function Get-PrismPath([string]$Path, [string]$Kind = 'Any', [switch]$AllowMissing) {
    if ([string]::IsNullOrWhiteSpace($Path) -or $Path -cnotmatch '^[A-Za-z]:[\\/]' -or
        $Path -match '[\x00-\x1f*?]' -or $Path -match '(^|[\\/])\.{1,2}([\\/]|$)' -or
        $Path.StartsWith('\\') -or $Path -match '^[\\/]{2}') {
        throw 'Use an absolute local path without traversal, devices or wildcards.'
    }
    $full = [IO.Path]::GetFullPath($Path).TrimEnd([char[]]'\/')
    $rootPath = [IO.Path]::GetPathRoot($full)
    if ($full.Length -le $rootPath.Length) { throw 'A volume root is not an allowed target.' }
    $rest = $full.Substring($rootPath.Length)
    $current = $rootPath
    foreach ($segment in $rest.Split([char[]]'\/', [StringSplitOptions]::RemoveEmptyEntries)) {
        if ($segment.Contains(':') -or $segment.EndsWith('.') -or $segment.EndsWith(' ')) {
            throw 'Unsafe path segment.'
        }
        $current = Join-Path $current $segment
        $item = $null
        try { $item = Get-Item -LiteralPath $current -Force -ErrorAction Stop }
        catch [Management.Automation.ItemNotFoundException] { }
        if ($null -ne $item -and ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw 'Reparse points and symlinks are not allowed.'
        }
    }
    if (-not (Test-Path -LiteralPath $full)) {
        if (-not $AllowMissing) { throw 'Required local path is missing.' }
    } elseif ($Kind -eq 'File' -and -not [IO.File]::Exists($full)) {
        throw 'A regular file was required.'
    } elseif ($Kind -eq 'Directory' -and -not [IO.Directory]::Exists($full)) {
        throw 'A directory was required.'
    }
    return $full
}

function Test-PrismWithin([string]$Path, [string]$Parent) {
    return $Path.Equals($Parent, [StringComparison]::OrdinalIgnoreCase) -or
        $Path.StartsWith($Parent.TrimEnd([char[]]'\/') + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase)
}

function Assert-PrismTree([string]$Path) {
    $null = Get-PrismPath $Path 'Directory'
    $pending = New-Object 'System.Collections.Generic.Queue[string]'
    $pending.Enqueue($Path)
    $seen = 0
    while ($pending.Count -gt 0) {
        foreach ($item in @(Get-ChildItem -LiteralPath $pending.Dequeue() -Force -ErrorAction Stop)) {
            $seen++
            if ($seen -gt 100000) { throw 'Target safety inspection exceeded its bounded file limit.' }
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw 'Reparse points and symlinks are not allowed in the target.'
            }
            if ($item.PSIsContainer) { $pending.Enqueue($item.FullName) }
        }
    }
}

function Get-PrismHash([string]$Path, [string]$Algorithm = 'SHA256') {
    $null = Get-PrismPath $Path 'File'
    return (Get-FileHash -LiteralPath $Path -Algorithm $Algorithm -ErrorAction Stop).Hash.ToLowerInvariant()
}

function Get-PrismBytesHash([byte[]]$Bytes) {
    $hash = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($hash.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $hash.Dispose() }
}

function Read-PrismJson([string]$Path) {
    $null = Get-PrismPath $Path 'File'
    if ((Get-Item -LiteralPath $Path).Length -gt 2MB) { throw 'JSON metadata exceeds its size limit.' }
    return [IO.File]::ReadAllText($Path, [Text.Encoding]::UTF8) | ConvertFrom-Json -ErrorAction Stop
}

function Write-PrismNewBytes([string]$Path, [byte[]]$Bytes) {
    $null = Get-PrismPath $Path 'File' -AllowMissing
    $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    try { $stream.Write($Bytes, 0, $Bytes.Length); $stream.Flush($true) } finally { $stream.Dispose() }
}

function Write-PrismNewJson([string]$Path, $Value) {
    $text = (ConvertTo-Json $Value -Depth 40) + "`n"
    Write-PrismNewBytes $Path ([Text.Encoding]::UTF8.GetBytes($text))
}

# These narrow OS/Git/transport functions are replaced by in-process fixture functions
# in Test-PrismDev.ps1. There are intentionally no production bypass parameters.
function Invoke-PrismGit([string]$Root, [string[]]$Arguments) {
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& git --no-pager -C $Root @Arguments 2>&1)
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    if ($code -ne 0) { throw 'Source Git verification failed; no Git state was changed.' }
    return ($output -join "`n").Trim()
}

function Assert-PrismSource([string]$Root, [string]$Expected) {
    $top = Invoke-PrismGit $Root @('rev-parse', '--show-toplevel')
    if (-not (Get-PrismPath $top 'Directory').Equals($Root, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Root must be the execution repository root.'
    }
    $revision = Invoke-PrismGit $Root @('rev-parse', '--verify', 'HEAD^{commit}')
    Assert-PrismRevision $revision
    if ($Expected) {
        Assert-PrismRevision $Expected
        if ($Expected -cne $revision) { throw 'Execution source is not ExpectedRevision.' }
    }
    if (Invoke-PrismGit $Root @('status', '--porcelain', '--untracked-files=normal')) {
        throw 'Source must be clean and committed.'
    }
    return $revision
}

function Assert-PrismRuntime([string]$Root, [string]$Source, [string]$Runtime) {
    Assert-PrismRevision $Runtime
    $null = Invoke-PrismGit $Root @('merge-base', '--is-ancestor', $Runtime, $Source)
    $null = Invoke-PrismGit $Root @('diff', '--exit-code', $Runtime, $Source, '--',
        'common', 'forge', 'settings.gradle', 'gradle.properties', 'gradle', 'gradlew', 'gradlew.bat')
    $boundary = '// Application build inputs stay above this boundary'
    $old = (Invoke-PrismGit $Root @('show', ($Runtime + ':build.gradle'))).Replace("`r`n", "`n")
    $new = [IO.File]::ReadAllText((Join-Path $Root 'build.gradle')).Replace("`r`n", "`n")
    if (-not $old.Contains($boundary) -or -not $new.Contains($boundary) -or
        $old.Split(@($boundary), 2, [StringSplitOptions]::None)[0].Trim() -cne
        $new.Split(@($boundary), 2, [StringSplitOptions]::None)[0].Trim()) {
        throw 'Runtime application build inputs differ from package source.'
    }
}

function Get-PrismProperties([string]$Root) {
    $path = Get-PrismPath (Join-Path $Root 'gradle.properties') 'File'
    $values = @{}
    foreach ($line in [IO.File]::ReadAllLines($path)) {
        if ($line -match '^\s*(#|$)') { continue }
        if ($line -notmatch '^([A-Za-z0-9_.]+)=(.*)$' -or $values.ContainsKey($matches[1])) {
            throw 'Invalid or duplicate Gradle property.'
        }
        $values[$matches[1]] = $matches[2].Trim()
    }
    foreach ($key in @('minecraft_version', 'forge_version', 'java_version', 'pack_version',
            'archives_base_name', 'mod_version', 'architectury_api_version', 'jei_version')) {
        if (-not $values.ContainsKey($key) -or -not $values[$key]) { throw 'Missing required Gradle version property.' }
    }
    foreach ($id in $script:PrismDevQuestCoordinates.Keys) {
        foreach ($suffix in @('_version', '_curse_file')) {
            $key = $script:PrismDevQuestCoordinates[$id][1] + $suffix
            if (-not $values.ContainsKey($key) -or -not $values[$key]) { throw 'Missing required Gradle version property.' }
        }
    }
    if ($values.java_version -cne '17') { throw 'This engineering tool requires the Java 17 contract.' }
    return $values
}

function Assert-PrismPin($Pin) {
    if ($null -eq $Pin -or $Pin.projectID -isnot [int] -or $Pin.projectID -le 0 -or
        $Pin.fileID -isnot [int] -or $Pin.fileID -le 0 -or
        $Pin.sha1 -cnotmatch '^[0-9a-f]{40}$' -or
        $Pin.bytes -le 0 -or $Pin.bytes -gt 64MB -or
        $Pin.requiredProjectIDs -isnot [System.Collections.IList]) { throw 'Invalid dependency verification metadata.' }
    Assert-PrismLeaf $Pin.fileName
    if (-not $Pin.fileName.EndsWith('.jar', [StringComparison]::Ordinal)) { throw 'A pinned dependency must be a jar.' }
    $declaredSha256 = $Pin.PSObject.Properties['sha256']
    if ($null -ne $declaredSha256 -and ($declaredSha256.Value -isnot [string] -or
        $declaredSha256.Value -cnotmatch '^[0-9a-f]{64}$')) { throw 'Invalid dependency SHA256 metadata.' }
    foreach ($required in @($Pin.requiredProjectIDs)) {
        if ($required -isnot [int] -or $required -le 0) { throw 'Invalid required dependency project.' }
    }
    if (@($Pin.requiredProjectIDs | Sort-Object -Unique).Count -ne @($Pin.requiredProjectIDs).Count) {
        throw 'Duplicate required dependency project.'
    }
}

function Get-PrismContract([string]$Root) {
    $props = Get-PrismProperties $Root
    $manifest = Read-PrismJson (Join-Path $Root 'pack/curseforge/manifest.json')
    $profiles = Read-PrismJson (Join-Path $Root 'pack/profiles.json')
    $metadata = Read-PrismJson (Join-Path $Root 'pack/dependency-metadata.json')
    $overrides = Read-PrismJson (Join-Path $Root 'pack/public-overrides.json')
    Assert-PrismKeys $overrides @('schemaVersion', 'files') 'public override manifest'
    if ($overrides.schemaVersion -ne 1 -or $overrides.files -isnot [System.Collections.IList]) {
        throw 'Invalid public override manifest.'
    }
    if (@($overrides.files).Count -ne 0) {
        throw 'Nonempty public overrides are unsupported by this bounded deployment tool; nothing was installed.'
    }
    Assert-PrismKeys $profiles @('expanded', 'preparation', 'playable') 'profile catalogue'
    Assert-PrismKeys $profiles.expanded @('status') 'expanded profile'
    if ($profiles.expanded.status -isnot [string] -or [string]::IsNullOrWhiteSpace($profiles.expanded.status)) {
        throw 'Invalid expanded profile data.'
    }
    foreach ($profileName in @('preparation', 'playable')) {
        $profile = $profiles.$profileName
        Assert-PrismKeys $profile @('name', 'status', 'projectIDs') "$profileName profile"
        if ($profile.name -isnot [string] -or [string]::IsNullOrWhiteSpace($profile.name) -or
            $profile.status -isnot [string] -or [string]::IsNullOrWhiteSpace($profile.status) -or
            $profile.projectIDs -isnot [System.Collections.IList]) {
            throw "Invalid $profileName profile data."
        }
    }
    if ($profiles.playable.status -cne $script:PrismDevStatus -or -not $profiles.playable.name) {
        throw 'The selected profile must retain its restricted, not-RC status.'
    }
    Assert-PrismEqual @($profiles.preparation.projectIDs | Sort-Object) `
        @($script:PrismDevPreparationProjects | Sort-Object) 'Six-project preparation selection'
    Assert-PrismEqual @($profiles.playable.projectIDs | Sort-Object) `
        @($script:PrismDevProjects | Sort-Object) 'Nine-project playable selection'
    if ($manifest.manifestType -cne 'minecraftModpack' -or $manifest.manifestVersion -ne 1 -or
        $manifest.overrides -cne 'overrides' -or $manifest.version -cne $props.pack_version -or
        $manifest.minecraft.version -cne $props.minecraft_version -or
        @($manifest.minecraft.modLoaders).Count -ne 1 -or
        $manifest.minecraft.modLoaders[0].primary -ne $true -or
        $manifest.minecraft.modLoaders[0].id -isnot [string] -or
        -not $manifest.minecraft.modLoaders[0].id.StartsWith('forge-') -or
        ($props.minecraft_version + '-' + $manifest.minecraft.modLoaders[0].id.Substring(6)) -cne $props.forge_version -or
        $metadata.minecraft -cne $props.minecraft_version -or $metadata.loader -cne 'Forge' -or
        $manifest.files -isnot [System.Collections.IList] -or $metadata.files -isnot [System.Collections.IList]) {
        throw 'Manifest/platform version mismatch against gradle.properties.'
    }
    $ids = @{}
    foreach ($pin in @($manifest.files)) {
        if ($pin.projectID -isnot [int] -or $pin.projectID -le 0 -or $pin.fileID -isnot [int] -or
            $pin.fileID -le 0 -or $pin.required -ne $true -or $ids.ContainsKey($pin.projectID)) {
            throw 'Invalid or duplicate authoritative manifest pin.'
        }
        $ids[$pin.projectID] = $true
    }
    Assert-PrismEqual @($manifest.files.projectID | Sort-Object) `
        @($script:PrismDevExpandedProjects | Sort-Object) 'Expanded dependency selection'
    if (@($metadata.files).Count -ne @($manifest.files).Count) {
        throw 'Manifest/dependency metadata pin parity mismatch.'
    }
    $expandedDependencies = @()
    foreach ($pin in @($manifest.files)) {
        $records = @($metadata.files | Where-Object { $_.projectID -eq $pin.projectID -and $_.fileID -eq $pin.fileID })
        if ($records.Count -ne 1) { throw 'Manifest/dependency metadata pin parity mismatch.' }
        Assert-PrismPin $records[0]
        foreach ($required in @($records[0].requiredProjectIDs)) {
            if (-not $ids.ContainsKey($required)) { throw 'Missing required dependency project.' }
        }
        $expandedDependencies += $records[0]
    }
    $names = @{}
    foreach ($dep in $expandedDependencies) {
        if ($names.ContainsKey($dep.fileName)) { throw 'Conflicting dependency jar names.' }
        $names[$dep.fileName] = $true
    }
    $selected = @($manifest.files | Where-Object { $_.projectID -in $profiles.playable.projectIDs })
    if ($selected.Count -ne 9) { throw 'Missing playable dependency pin.' }
    $dependencies = @($expandedDependencies | Where-Object { $_.projectID -in $profiles.playable.projectIDs })
    foreach ($dep in $dependencies) {
        foreach ($required in @($dep.requiredProjectIDs)) {
            if ($required -notin $profiles.playable.projectIDs) { throw 'Missing required dependency project.' }
        }
    }
    $expectedNames = @{
        419699 = "architectury-$($props.architectury_api_version)-forge.jar"
        238222 = "jei-$($props.minecraft_version)-forge-$($props.jei_version).jar"
    }
    $questFileProperties = @{}
    foreach ($id in $script:PrismDevQuestCoordinates.Keys) {
        $coordinate = $script:PrismDevQuestCoordinates[$id]
        $expectedNames[$id] = $coordinate[0] + '-forge-' + $props[$coordinate[1] + '_version'] + '.jar'
        $questFileProperties[$id] = $coordinate[1] + '_curse_file'
    }
    foreach ($id in $expectedNames.Keys) {
        if (@($dependencies | Where-Object { $_.projectID -eq $id })[0].fileName -cne $expectedNames[$id]) {
            throw 'Dependency version mismatch against gradle.properties.'
        }
    }
    foreach ($id in $questFileProperties.Keys) {
        if (@($dependencies | Where-Object { $_.projectID -eq $id })[0].fileID.ToString() -cne $props[$questFileProperties[$id]]) {
            throw 'Native FTB quest file pin mismatch against gradle.properties.'
        }
    }
    $manifest.name = $profiles.playable.name
    $manifest.version += '-playable'
    $manifest.files = $selected
    return [pscustomobject]@{ Properties = $props; Manifest = $manifest; Profile = $profiles.playable
        Metadata = $metadata; Dependencies = $dependencies }
}

function Read-PrismZipBytes($Entry, [long]$Limit) {
    if ($Entry.Length -lt 0 -or $Entry.Length -gt $Limit) { throw 'Archive entry exceeds its bounded size limit.' }
    $input = $Entry.Open()
    $output = New-Object IO.MemoryStream
    try {
        $buffer = New-Object byte[] 81920
        while (($count = $input.Read($buffer, 0, $buffer.Length)) -gt 0) {
            if ($output.Length + $count -gt $Limit) { throw 'Archive entry exceeds its bounded size limit.' }
            $output.Write($buffer, 0, $count)
        }
        if ($output.Length -ne $Entry.Length) { throw 'Archive entry length mismatch.' }
        return ,$output.ToArray()
    } finally { $input.Dispose(); $output.Dispose() }
}

function Read-PrismPackage([string]$Root, [string]$Path, $Contract, [string]$Source) {
    $Path = Get-PrismPath $Path 'File'
    Add-Type -AssemblyName System.IO.Compression
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None)
    $zip = $null
    try {
        if ($stream.Length -gt 96MB) { throw 'Package exceeds the bounded playable ZIP size limit.' }
        $hash = [Security.Cryptography.SHA256]::Create()
        try { $zipHash = ([BitConverter]::ToString($hash.ComputeHash($stream))).Replace('-', '').ToLowerInvariant() }
        finally { $hash.Dispose() }
        $stream.Position = 0
        $zip = New-Object IO.Compression.ZipArchive($stream, [IO.Compression.ZipArchiveMode]::Read, $true)
        $entries = @{}
        foreach ($entry in $zip.Entries) {
            if ($entries.ContainsKey($entry.FullName) -or
                (($entry.ExternalAttributes -shr 16) -band 0xF000) -eq 0xA000 -or
                ($entry.ExternalAttributes -band 0x400) -ne 0) { throw 'Duplicate or symlink archive entry.' }
            $entries[$entry.FullName] = $entry
        }
        if (-not $entries.ContainsKey('provenance.json')) { throw 'Missing package provenance.' }
        $provenance = [Text.Encoding]::UTF8.GetString((Read-PrismZipBytes $entries['provenance.json'] 1MB)) |
            ConvertFrom-Json -ErrorAction Stop
        Assert-PrismRevision $provenance.sourceRevision
        if ($provenance.sourceRevision -cne $Source) { throw 'Package provenance source does not match clean execution source.' }
        Assert-PrismRuntime $Root $Source $provenance.runtimeSourceRevision
        $jarName = "$($Contract.Properties.archives_base_name)-$($Contract.Properties.mod_version)-forge.jar"
        Assert-PrismLeaf $jarName
        if ($provenance.grindlessSha256 -cnotmatch '^[0-9a-f]{64}$') { throw 'Invalid Grindless SHA256 provenance.' }
        $expectedProvenance = [ordered]@{
            sourceRevision = $Source; runtimeSourceRevision = $provenance.runtimeSourceRevision
            profile = 'playable'; status = $script:PrismDevStatus; inputProfile = $Contract.Profile
            pinAuthority = 'pack/curseforge/manifest.json'; minecraft = $Contract.Properties.minecraft_version
            forge = $Contract.Properties.forge_version; java = $Contract.Properties.java_version
            grindlessJar = $jarName; grindlessSha256 = $provenance.grindlessSha256
            dependencies = @($Contract.Dependencies)
            dependencyMetadataEndpoint = $Contract.Metadata.metadataEndpoint
            dependencyPublisherEndpoint = $Contract.Metadata.publisherEndpoint
            worldsIncluded = $false
            configuration = 'Mod defaults; no private launcher settings or gameplay overrides.'
            configurationPolicy = @{ base = 'upstream-defaults'; overriddenDestinations = @() }
            publicOverrides = @{ manifest = 'pack/public-overrides.json'; sourceRoot = 'pack/public-overrides'; files = @() }
        }
        Assert-PrismEqual $provenance $expectedProvenance 'Package provenance'
        $jarEntry = 'overrides/mods/' + $jarName
        $files = @('manifest.json', 'provenance.json', 'README.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md', $jarEntry)
        $allowed = $files + @('overrides/', 'overrides/mods/')
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -cnotin $allowed) { throw 'Archive violates the explicit playable content whitelist.' }
        }
        foreach ($name in $files) {
            if (@($zip.Entries | Where-Object { $_.FullName -ceq $name }).Count -ne 1) {
                throw 'Missing or case-mismatched whitelisted archive file.'
            }
        }
        $manifest = [Text.Encoding]::UTF8.GetString((Read-PrismZipBytes $entries['manifest.json'] 1MB)) |
            ConvertFrom-Json -ErrorAction Stop
        Assert-PrismEqual $manifest $Contract.Manifest 'Selected playable manifest'
        foreach ($pair in @(@('README.md', 'pack/README.md'), @('LICENSE', 'LICENSE'),
                @('THIRD_PARTY_NOTICES.md', 'THIRD_PARTY_NOTICES.md'))) {
            $bytes = Read-PrismZipBytes $entries[$pair[0]] 8MB
            if ((Get-PrismBytesHash $bytes) -cne (Get-PrismHash (Join-Path $Root $pair[1]))) {
                throw 'Package public documentation hash mismatch.'
            }
        }
        $jar = Read-PrismZipBytes $entries[$jarEntry] 64MB
        if ((Get-PrismBytesHash $jar) -cne $provenance.grindlessSha256) { throw 'Remapped Grindless SHA256 mismatch.' }
        return [pscustomobject]@{ Path = $Path; Sha256 = $zipHash; Provenance = $provenance
            JarName = $jarName; JarBytes = $jar; JarSha256 = $provenance.grindlessSha256 }
    } finally {
        if ($null -ne $zip) { $zip.Dispose() }
        $stream.Dispose()
    }
}

        function Get-PrismJava([string]$Path, [string]$ExpectedHash) {
            $Path = Get-PrismPath $Path 'File'
            if ($ExpectedHash -cnotmatch '^[0-9a-f]{64}$' -or (Get-PrismHash $Path) -cne $ExpectedHash) {
                throw 'Java executable SHA256 does not match the supplied verified hash.'
            }
            $file = Get-Item -LiteralPath $Path
            if ($file.Name -cnotin @('java.exe', 'javaw.exe') -or $file.Directory.Name -ine 'bin') {
                throw 'Supply the Java 17 bin/java.exe or bin/javaw.exe, not a shim.'
            }
            $release = Get-PrismPath (Join-Path $file.Directory.Parent.FullName 'release') 'File'
            if ((Get-Item -LiteralPath $release).Length -gt 64KB) { throw 'Invalid Java release metadata.' }
            $text = [IO.File]::ReadAllText($release)
            if ($text -cnotmatch '(?m)^JAVA_VERSION="17(?:[.+_-][^"\r\n]+)?"\r?$' -or
                $text -cnotmatch '(?m)^OS_ARCH="(x86_64|amd64|aarch64)"\r?$') {
                throw 'Selected Java must have matching 64-bit Java 17 release metadata.'
            }
            # Inspect PE metadata only. Never start a JVM to check its version.
            $stream = [IO.File]::OpenRead($Path)
            $reader = New-Object IO.BinaryReader($stream)
            try {
                if ($stream.Length -lt 128 -or $reader.ReadUInt16() -ne 0x5A4D) { throw 'Java is not a Windows executable.' }
                $stream.Position = 0x3C
                $offset = $reader.ReadInt32()
                if ($offset -lt 64 -or $offset + 6 -gt $stream.Length) { throw 'Invalid Java executable header.' }
                $stream.Position = $offset
                if ($reader.ReadUInt32() -ne 0x00004550 -or $reader.ReadUInt16() -notin @(0x8664, 0xAA64)) {
                    throw 'Selected Java must be a 64-bit Windows executable.'
                }
            } finally { $reader.Dispose(); $stream.Dispose() }
            $version = [Diagnostics.FileVersionInfo]::GetVersionInfo($Path)
            if ($version.FileMajorPart -ne 0 -and $version.FileMajorPart -ne 17) {
                throw 'Java executable version disagrees with Java 17 release metadata.'
            }
            return [pscustomobject]@{ Path = $Path; Sha256 = $ExpectedHash; Major = 17 }
        }

        function Assert-PrismNoPatches([string]$Instance) {
            $patches = Join-Path $Instance 'patches'
            if (Test-Path -LiteralPath $patches) {
                $null = Get-PrismPath $patches 'Directory'
                if (@(Get-ChildItem -LiteralPath $patches -Force -ErrorAction Stop).Count -ne 0) {
                    throw 'Customized launcher patches are unsupported; the template/target is preserved.'
                }
            }
        }

        function Get-PrismComponents($Data, [string]$Minecraft, [string]$Forge) {
            Assert-PrismKeys $Data @('formatVersion', 'components') 'Prism pack metadata'
            if ($Data.formatVersion -ne 1 -or $Data.components -isnot [System.Collections.IList] -or
                @($Data.components).Count -ne 3) { throw 'Template must contain exactly Minecraft, Forge and LWJGL components.' }
            $versions = @{}
            foreach ($component in $Data.components) {
                $fields = @('uid', 'version', 'dependencyOnly', 'important', 'disabled', 'cachedVersion',
                    'cachedName', 'cachedRequires', 'cachedConflicts', 'volatile', 'cachedVolatile')
                foreach ($property in $component.PSObject.Properties) {
                    if ($property.Name -cnotin $fields) { throw 'Customized template component metadata is unsupported.' }
                }
                foreach ($flag in @('dependencyOnly', 'important', 'disabled', 'volatile', 'cachedVolatile')) {
                    if ($null -ne $component.PSObject.Properties[$flag] -and $component.$flag -isnot [bool]) {
                        throw 'Invalid boolean component cache/selection flag.'
                    }
                }
                if ($component.uid -cnotin @('net.minecraft', 'net.minecraftforge', 'org.lwjgl3') -or
                    $versions.ContainsKey($component.uid) -or
                    $component.version -cnotmatch '^[0-9][A-Za-z0-9._+-]*$') { throw 'Invalid or unsupported template component.' }
                if (($null -ne $component.PSObject.Properties['disabled'] -and $component.disabled -ne $false) -or
                    ($null -ne $component.PSObject.Properties['cachedConflicts'] -and @($component.cachedConflicts).Count -gt 0)) {
                    throw 'Disabled or conflicting template components are unsupported.'
                }
                $versions[$component.uid] = $component.version
            }
            if ($versions['net.minecraft'] -cne $Minecraft -or $versions['net.minecraftforge'] -cne $Forge) {
                throw 'Template component version mismatch against the selected pack.'
            }
            foreach ($component in $Data.components) {
                if ($null -ne $component.PSObject.Properties['cachedRequires']) {
                    foreach ($requirement in @($component.cachedRequires)) {
                        if ($requirement.uid -cnotin @('net.minecraft', 'net.minecraftforge', 'org.lwjgl3') -or
                            ($null -ne $requirement.PSObject.Properties['equals'] -and
                                $requirement.equals -cne $versions[$requirement.uid])) {
                            throw 'Customized or incompatible cached component requirements are unsupported.'
                        }
                    }
                }
            }
            # Ignore launcher caches, names and private config, but never hide custom patches.
            # LWJGL's version is taken ONLY from the working template, never from this tool.
            return [pscustomobject]@{ formatVersion = 1; components = @(
                [ordered]@{ uid = 'org.lwjgl3'; version = $versions['org.lwjgl3']; dependencyOnly = $true },
                [ordered]@{ uid = 'net.minecraft'; version = $versions['net.minecraft']; important = $true },
                [ordered]@{ uid = 'net.minecraftforge'; version = $versions['net.minecraftforge'] }
            ) }
        }

        function Get-PrismComponentIdentity($Data) {
            return @($Data.components | ForEach-Object { [ordered]@{ uid = $_.uid; version = $_.version } } |
                Sort-Object { $_.uid })
        }

        function Get-PrismJavaProcesses {
            try {
                return @(Get-CimInstance -ClassName Win32_Process -Filter `
                    "Name = 'java.exe' OR Name = 'javaw.exe' OR Name = 'java' OR Name = 'javaw'" -ErrorAction Stop)
            } catch { throw 'Cannot inspect Java process safety state; deployment is refused.' }
        }

        function Assert-PrismStopped([string]$Target) {
            $needle = $Target.Replace('\', '/').ToLowerInvariant()
            $id = [IO.Path]::GetFileName($Target).ToLowerInvariant()
            foreach ($process in @(Get-PrismJavaProcesses)) {
                if ($process.Name -notin @('java.exe', 'javaw.exe', 'java', 'javaw')) { continue }
                if ([string]::IsNullOrWhiteSpace($process.CommandLine) -or
                    [string]::IsNullOrWhiteSpace($process.ExecutablePath)) {
                    throw 'A Java process has an uninspectable safety state; deployment is refused.'
                }
                # Command lines can contain access tokens. Keep them internal, including errors.
                $command = $process.CommandLine.Replace('\', '/').ToLowerInvariant()
                if ($command.Contains($needle) -or $command.Contains($id)) {
                    throw 'The target engineering instance is running; stop it manually before deployment.'
                }
                $gameDir = [Regex]::Match($command, '--gamedir(?:=|\s+)(?:"([^"]+)"|(\S+))')
                if ($gameDir.Success) {
                    $path = $gameDir.Groups[1].Value
                    if (-not $path) { $path = $gameDir.Groups[2].Value }
                    if ($path -notmatch '^[a-z]:/' -or $path -match '~[0-9]') {
                        throw 'A game Java process cannot be safely attributed to an absolute instance path.'
                    }
                    $path = [IO.Path]::GetFullPath($path)
                    if (Test-PrismWithin $path $Target) { throw 'The target engineering instance is running; stop it manually.' }
                } elseif ($command -match '(org\.prismlauncher|org\.multimc|net\.minecraft)') {
                    throw 'A game Java process has no attributable game directory; deployment is refused.'
                }
            }
        }

        function Read-PrismCfg([string]$Path) {
            $null = Get-PrismPath $Path 'File'
            if ((Get-Item -LiteralPath $Path).Length -gt 256KB) { throw 'Instance configuration is unexpectedly large.' }
            $result = @{}
            $section = ''
            foreach ($line in [IO.File]::ReadAllLines($Path)) {
                if ($line -match '^\s*([#;]|$)') { continue }
                if ($line -match '^\[([A-Za-z0-9_.-]+)\]$') { $section = $matches[1]; continue }
                if ($section -ne 'General') { continue }
                if ($line -notmatch '^([A-Za-z0-9_.-]+)=(.*)$' -or $result.ContainsKey($matches[1])) {
                    throw 'Uninspectable or duplicate instance configuration.'
                }
                $result[$matches[1]] = $matches[2]
            }
            return $result
        }

        function Get-PrismCfgText([string]$InstanceId, [string]$JavaPath) {
            return (@(
                '[General]', 'InstanceType=OneSix', "name=Grindless T0 Engineering Dev [$InstanceId]",
                'iconKey=default', 'ManagedPack=false', 'OverrideJavaLocation=true', ('JavaPath=' + $JavaPath.Replace('\', '/')),
                'OverrideMemory=true', 'MinMemAlloc=512', 'MaxMemAlloc=4096'
            ) -join "`n") + "`n"
        }

        function Assert-PrismUnlocked([string]$Path) {
            try {
                $handle = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None)
                $handle.Dispose()
            } catch { throw 'A managed target file is locked or uninspectable; deployment is refused.' }
        }

function Assert-PrismModIndex([string]$Mods) {
    $index = Join-Path $Mods '.index'
    if (Test-Path -LiteralPath $index) {
        $null = Get-PrismPath $index 'Directory'
        $sidecars = @(Get-ChildItem -LiteralPath $index -Force -ErrorAction Stop)
        if ($sidecars.Count -gt 1000) { throw 'Prism mod index exceeds the bounded safety inspection limit.' }
        foreach ($sidecar in $sidecars) {
            if ($sidecar.PSIsContainer -or $sidecar.Extension -ine '.toml' -or
                ($sidecar.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw 'Uninspectable/non-sidecar content in Prism mods/.index; it is preserved.'
            }
        }
    }
}

        function Get-PrismOwnedState([string]$Target, [string]$InstanceId, [switch]$AllowQuestRecovery) {
            $null = Get-PrismPath $Target 'Directory'
            Assert-PrismStopped $Target
            Assert-PrismTree $Target
            Assert-PrismNoPatches $Target
            if (Test-Path -LiteralPath (Join-Path $Target '.minecraft')) {
                throw 'Ambiguous legacy game root in the new owned engineering instance.'
            }
            $ownerPath = Join-Path $Target $script:PrismDevOwnerFile
            $receiptPath = Join-Path $Target $script:PrismDevReceiptFile
            if (-not [IO.File]::Exists($ownerPath) -or -not [IO.File]::Exists($receiptPath)) {
                throw 'Existing target is unowned; it will not be adopted or overwritten.'
            }
            if (Test-Path -LiteralPath (Join-Path $Target $script:PrismDevPendingFile)) {
                throw 'An incomplete deployment journal exists; inspect its preserved backups manually.'
            }
            if (-not $AllowQuestRecovery -and
                (Test-Path -LiteralPath (Join-Path $Target $script:PrismQuestPendingFile))) {
                throw 'An incomplete native definition update exists; use RecoverQuestDefinitions with its original verified plan.'
            }
            $owner = Read-PrismJson $ownerPath
            Assert-PrismKeys $owner @('schemaVersion', 'tool', 'purpose', 'instanceId', 'ownershipId') 'ownership marker'
            if ($owner.schemaVersion -ne 1 -or $owner.tool -cne $script:PrismDevTool -or
                $owner.purpose -cne 'engineering-development' -or $owner.instanceId -cne $InstanceId -or
                $owner.ownershipId -cnotmatch '^[0-9a-f]{32}$') { throw 'Existing target has an invalid ownership marker.' }
            $receipt = Read-PrismJson $receiptPath
            Assert-PrismKeys $receipt @('schemaVersion', 'tool', 'state', 'instanceId', 'ownershipId', 'operationId',
                'deployedUtc', 'sourceRevision', 'runtimeSourceRevision', 'packSha256', 'packStatus', 'minecraft',
                'forge', 'java', 'javaExecutable', 'javaSha256', 'maxMemoryMiB', 'componentMetadata', 'grindless',
                'dependencies') 'deployment receipt'
            if ($receipt.schemaVersion -ne 1 -or $receipt.tool -cne $script:PrismDevTool -or
                $receipt.state -cne 'deployed' -or $receipt.instanceId -cne $InstanceId -or
                $receipt.ownershipId -cne $owner.ownershipId -or $receipt.packStatus -cne $script:PrismDevStatus -or
                $receipt.packSha256 -cnotmatch '^[0-9a-f]{64}$' -or $receipt.maxMemoryMiB -ne 4096 -or $receipt.java -ne '17') {
                throw 'Existing deployment receipt is invalid.'
            }
            Assert-PrismRevision $receipt.sourceRevision
            Assert-PrismRevision $receipt.runtimeSourceRevision
            $java = Get-PrismJava $receipt.javaExecutable $receipt.javaSha256
            $forge = $receipt.forge.Substring($receipt.minecraft.Length + 1)
            $components = Get-PrismComponents (Read-PrismJson (Join-Path $Target 'mmc-pack.json')) $receipt.minecraft $forge
            Assert-PrismEqual (Get-PrismComponentIdentity $components) `
                (Get-PrismComponentIdentity $receipt.componentMetadata) 'Installed component metadata'
            $cfg = Read-PrismCfg (Join-Path $Target 'instance.cfg')
            foreach ($key in @('InstanceType', 'name', 'OverrideJavaLocation', 'JavaPath', 'OverrideMemory', 'MinMemAlloc', 'MaxMemAlloc')) {
                if (-not $cfg.ContainsKey($key)) { throw 'A managed per-instance setting is missing.' }
            }
            if ($cfg.InstanceType -cne 'OneSix' -or $cfg.name -notlike '*Engineering Dev*' -or
                $cfg.OverrideJavaLocation -ine 'true' -or $cfg.OverrideMemory -ine 'true' -or
                $cfg.MinMemAlloc -ne '512' -or $cfg.MaxMemAlloc -ne '4096' -or
                $cfg.JavaPath.Replace('\', '/') -ine $java.Path.Replace('\', '/') -or
                ($cfg.ContainsKey('ManagedPack') -and $cfg.ManagedPack -ine 'false')) {
                throw 'Owned instance Java/memory/type or unmanaged engineering settings were changed.'
            }
            Assert-PrismEqual @($receipt.dependencies.projectID | Sort-Object) `
                @($script:PrismDevProjects | Sort-Object) 'Installed nine-project selection'
            $mods = Join-Path $Target 'minecraft/mods'
            $null = Get-PrismPath $mods 'Directory'
            Assert-PrismModIndex $mods
            $records = @($receipt.dependencies) + @($receipt.grindless)
            $actual = @(Get-ChildItem -LiteralPath $mods -Force | Where-Object { $_.Name -ine '.index' })
            if ($records.Count -ne 10 -or $actual.Count -ne 10) { throw 'Unknown, missing or tampered managed mods.' }
            $names = @{}
            foreach ($record in $records) {
                Assert-PrismLeaf $record.fileName
                if ($record.sha256 -cnotmatch '^[0-9a-f]{64}$' -or $record.bytes -le 0 -or
                    $names.ContainsKey($record.fileName)) { throw 'Invalid managed mod receipt.' }
                $names[$record.fileName] = $true
                $path = Get-PrismPath (Join-Path $mods $record.fileName) 'File'
                if ((Get-Item -LiteralPath $path).Length -ne $record.bytes -or (Get-PrismHash $path) -cne $record.sha256) {
                    throw 'Unknown, missing or tampered managed mods.'
                }
                Assert-PrismUnlocked $path
            }
            foreach ($item in $actual) {
                if ($item.PSIsContainer -or -not $names.ContainsKey($item.Name)) { throw 'Unknown or tampered mod file.' }
            }
            foreach ($dep in $receipt.dependencies) {
                Assert-PrismPin $dep
                if ((Get-PrismHash (Join-Path $mods $dep.fileName) 'SHA1') -cne $dep.sha1) {
                    throw 'Installed dependency published SHA1 mismatch.'
                }
            }
            foreach ($name in @($script:PrismDevOwnerFile, $script:PrismDevReceiptFile, 'mmc-pack.json', 'instance.cfg')) {
                Assert-PrismUnlocked (Join-Path $Target $name)
            }
            return [pscustomobject]@{ Owner = $owner; Receipt = $receipt; Components = $components; Java = $java }
        }

        function Assert-PrismDependency([string]$Path, $Pin) {
            $null = Get-PrismPath $Path 'File'
            if ((Get-Item -LiteralPath $Path).Length -ne $Pin.bytes -or (Get-PrismHash $Path 'SHA1') -cne $Pin.sha1) {
                throw 'Local/downloaded pinned dependency size or published SHA1 mismatch.'
            }
            if ($null -ne $Pin.PSObject.Properties['sha256'] -and (Get-PrismHash $Path) -cne $Pin.sha256) {
                throw 'Local/downloaded pinned dependency SHA256 mismatch.'
            }
        }

        function Get-PrismOfficialUrl($Pin) {
            Assert-PrismPin $Pin
            $group = [long][Math]::Floor($Pin.fileID / 1000)
            $file = $Pin.fileID % 1000
            return "https://edge.forgecdn.net/files/$group/$file/$([Uri]::EscapeDataString($Pin.fileName))"
        }

        function Save-PrismOfficialPin($Pin, [string]$Destination) {
            $url = [Uri](Get-PrismOfficialUrl $Pin)
            $response = $null
            $input = $null
            $output = $null
            try {
                for ($redirect = 0; $redirect -le 5; $redirect++) {
                    if ($url.Scheme -cne 'https' -or $url.Host -cnotin
                        @('edge.forgecdn.net', 'mediafilez.forgecdn.net', 'mediafiles.forgecdn.net') -or
                        -not $url.IsDefaultPort -or $url.UserInfo) { throw 'Non-official dependency redirect refused.' }
                    $request = [Net.HttpWebRequest]::Create($url)
                    $request.AllowAutoRedirect = $false
                    $request.UseDefaultCredentials = $false
                    $request.Timeout = 30000
                    $request.ReadWriteTimeout = 30000
                    $response = $request.GetResponse()
                    $status = [int]$response.StatusCode
                    if ($status -in @(301, 302, 303, 307, 308)) {
                        if ($redirect -eq 5 -or -not $response.Headers['Location']) { throw 'Too many official CDN redirects.' }
                        $url = New-Object Uri($url, $response.Headers['Location'])
                        $response.Dispose(); $response = $null
                        continue
                    }
                    if ($status -ne 200 -or ($response.ContentLength -ge 0 -and $response.ContentLength -ne $Pin.bytes)) {
                        throw 'Official CDN response size/status mismatch.'
                    }
                    $input = $response.GetResponseStream()
                    $null = Get-PrismPath $Destination 'File' -AllowMissing
                    $output = [IO.File]::Open($Destination, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
                    $buffer = New-Object byte[] 81920
                    $total = 0L
                    while (($count = $input.Read($buffer, 0, $buffer.Length)) -gt 0) {
                        $total += $count
                        if ($total -gt $Pin.bytes) { throw 'Official CDN response exceeds pinned size.' }
                        $output.Write($buffer, 0, $count)
                    }
                    $output.Flush($true); $output.Dispose(); $output = $null
                    if ($total -ne $Pin.bytes) { throw 'Official CDN download was incomplete.' }
                    Assert-PrismDependency $Destination $Pin
                    return
                }
                throw 'Official CDN download did not complete.'
            } catch {
                # Do not print proxy responses, credentials, signed redirect URLs or process data.
                throw "Official ForgeCDN download/verification failed for pin $($Pin.projectID)/$($Pin.fileID)."
            } finally {
                if ($null -ne $output) { $output.Dispose() }
                if ($null -ne $input) { $input.Dispose() }
                if ($null -ne $response) { $response.Dispose() }
            }
        }

        function Get-PrismDependencySources($Contract, [string]$Template, [string[]]$Directories, [string]$Target) {
            $roots = @()
            if ($Target) { $roots += (Join-Path $Target 'minecraft/mods') }
            foreach ($directory in $Directories) { $roots += (Get-PrismPath $directory 'Directory') }
            if ($Template) {
                # Probe ONLY known dependency filenames in the two launcher game-dir layouts.
                # Never enumerate or copy the template instance or its private game contents.
                foreach ($relative in @('.minecraft/mods', 'minecraft/mods')) {
                    $directory = Join-Path $Template $relative
                    if (Test-Path -LiteralPath $directory) { $roots += (Get-PrismPath $directory 'Directory') }
                }
            }
            $sources = @{}
            foreach ($pin in $Contract.Dependencies) {
                foreach ($directory in $roots) {
                    $candidate = Join-Path $directory $pin.fileName
                    if (-not (Test-Path -LiteralPath $candidate)) { continue }
                    Assert-PrismDependency $candidate $pin
                    $sources[$pin.fileName] = Get-PrismPath $candidate 'File'
                    break
                }
            }
            return $sources
        }

        function Set-PrismReplacement([string]$Source, [string]$Destination) {
            $null = Get-PrismPath $Source 'File'
            $null = Get-PrismPath $Destination 'File'
            # WinPS 5.1 binds a null string argument as "", which File.Replace rejects.
            # Its transient backup lives alongside the stage source, never in installed
            # mods. The durable, verified previous jar is already in target backups.
            $temporaryBackup = Join-Path ([IO.Path]::GetDirectoryName($Source)) ('.replaced-' + [Guid]::NewGuid().ToString('N'))
            $null = Get-PrismPath $temporaryBackup 'File' -AllowMissing
            [IO.File]::Replace($Source, $Destination, $temporaryBackup)
        }

function Read-PrismQuestPlan([string]$Path, [string]$ExpectedHash, [string]$Target, $State) {
    if ($ExpectedHash -cnotmatch '^[0-9a-f]{64}$' -or (Get-PrismHash $Path) -cne $ExpectedHash) {
        throw 'Native definition plan SHA256 does not match the explicitly selected publisher output.'
    }
    $plan = Read-PrismJson $Path
    Assert-PrismKeys $plan @('schemaVersion', 'tool', 'sourceRevision', 'runtimeSourceRevision',
        'grindlessSha256', 'instanceId', 'ownershipId', 'target', 'ownerSha256', 'deploymentSha256',
        'previousReceiptSha256', 'previousDefinitionsSha256', 'chapters', 'files', 'receipt') 'native definition plan'
    if (-not (Test-PrismQuestInteger $plan.schemaVersion 1 1) -or $plan.tool -cne 'grindless-ftb-definition-update' -or
        $plan.instanceId -cne $State.Owner.instanceId -or $plan.ownershipId -cne $State.Owner.ownershipId -or
        -not (Get-PrismPath $plan.target 'Directory').Equals($Target, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Unknown native definition plan version or target ownership.'
    }
    Assert-PrismRevision $plan.sourceRevision
    Assert-PrismRevision $plan.runtimeSourceRevision
    foreach ($hash in @($plan.grindlessSha256, $plan.ownerSha256, $plan.deploymentSha256,
            $plan.previousDefinitionsSha256)) {
        if ($hash -isnot [string] -or $hash -cnotmatch '^[0-9a-f]{64}$') { throw 'Invalid native definition plan hash.' }
    }
    if ($null -ne $plan.previousReceiptSha256 -and
        ($plan.previousReceiptSha256 -isnot [string] -or $plan.previousReceiptSha256 -cnotmatch '^[0-9a-f]{64}$')) {
        throw 'Invalid previous native definition ownership hash.'
    }
    if ($plan.files -isnot [System.Collections.IList] -or $plan.chapters -isnot [System.Collections.IList] -or
        $plan.chapters.Count -lt 2 -or $plan.chapters.Count -gt 1000) { throw 'Invalid native chapter snapshot.' }
    Assert-PrismEqual @($plan.files.fileName | Sort-Object -CaseSensitive) $script:PrismQuestFilenames 'Native definition filenames'
    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    foreach ($file in $plan.chapters) {
        Assert-PrismKeys $file @('fileName', 'bytes', 'sha256') 'native chapter snapshot entry'
        Assert-PrismLeaf $file.fileName
        if (-not $file.fileName.EndsWith('.snbt', [StringComparison]::OrdinalIgnoreCase) -or
            -not $seen.Add($file.fileName) -or -not (Test-PrismQuestInteger $file.bytes 1 1MB) -or
            $file.sha256 -cnotmatch '^[0-9a-f]{64}$') { throw 'Invalid or colliding native chapter snapshot entry.' }
    }
    foreach ($file in $plan.files) {
        Assert-PrismKeys $file @('fileName', 'beforeSha256', 'beforeBytes', 'afterSha256', 'afterBytes',
            'contentsBase64') 'native definition replacement'
        if ($file.beforeSha256 -cnotmatch '^[0-9a-f]{64}$' -or $file.afterSha256 -cnotmatch '^[0-9a-f]{64}$' -or
            -not (Test-PrismQuestInteger $file.beforeBytes 1 1MB) -or
            -not (Test-PrismQuestInteger $file.afterBytes 1 1MB) -or $file.contentsBase64 -isnot [string]) {
            throw 'Invalid native definition replacement hash or size.'
        }
        $bytes = [Convert]::FromBase64String($file.contentsBase64)
        if ($bytes.Length -ne $file.afterBytes -or (Get-PrismBytesHash $bytes) -cne $file.afterSha256) {
            throw 'Native definition replacement payload hash mismatch.'
        }
        $entry = @($plan.chapters | Where-Object { $_.fileName -ceq $file.fileName })
        if ($entry.Count -ne 1 -or $entry[0].sha256 -cne $file.beforeSha256 -or $entry[0].bytes -ne $file.beforeBytes) {
            throw 'Native definition replacement does not match its preflight snapshot.'
        }
    }
    $receipt = $plan.receipt
    Assert-PrismKeys $receipt @('schemaVersion', 'tool', 'instanceId', 'ownershipId', 'sourceRevision',
        'runtimeSourceRevision', 'grindlessSha256', 'questCount', 'files') 'native definition ownership receipt'
    if (-not (Test-PrismQuestInteger $receipt.schemaVersion 1 1) -or $receipt.tool -cne 'grindless-ftb-definitions' -or
        $receipt.instanceId -cne $plan.instanceId -or $receipt.ownershipId -cne $plan.ownershipId -or
        $receipt.sourceRevision -cne $plan.sourceRevision -or $receipt.runtimeSourceRevision -cne $plan.runtimeSourceRevision -or
        $receipt.grindlessSha256 -cne $plan.grindlessSha256 -or -not (Test-PrismQuestInteger $receipt.questCount 50 50) -or
        $receipt.files -isnot [System.Collections.IList]) { throw 'Invalid native definition ownership receipt.' }
    $expectedFiles = @($plan.files | ForEach-Object {
        [ordered]@{ fileName = $_.fileName; bytes = $_.afterBytes; sha256 = $_.afterSha256 }
    })
    Assert-PrismEqual $receipt.files $expectedFiles 'Native definition ownership payload'
    return $plan
}

function Test-PrismQuestInteger($Value, [long]$Minimum, [long]$Maximum) {
    return ($Value -is [int] -or $Value -is [long]) -and $Value -ge $Minimum -and $Value -le $Maximum
}

function Get-PrismQuestReceiptBytes($Plan) {
    return ,([Text.Encoding]::UTF8.GetBytes((ConvertTo-Json $Plan.receipt -Depth 40) + "`n"))
}

function Get-PrismQuestSnapshot([string]$Target) {
    $directory = Get-PrismPath (Join-Path $Target 'minecraft\config\ftbquests\quests\chapters') 'Directory'
    $result = @()
    foreach ($file in @(Get-ChildItem -LiteralPath $directory -Force -ErrorAction Stop)) {
        if (-not $file.Name.EndsWith('.snbt', [StringComparison]::OrdinalIgnoreCase)) { continue }
        Assert-PrismLeaf $file.Name
        $path = Get-PrismPath $file.FullName 'File'
        if ($file.Length -le 0 -or $file.Length -gt 1MB) { throw 'Native chapter exceeds its size limit.' }
        Assert-PrismUnlocked $path
        $result += [pscustomobject]@{ fileName = $file.Name; bytes = $file.Length; sha256 = (Get-PrismHash $path) }
    }
    return $result
}

function Assert-PrismQuestBinding($Plan) {
    Assert-PrismStopped $Plan.target
    if ((Get-PrismHash (Join-Path $Plan.target $script:PrismDevOwnerFile)) -cne $Plan.ownerSha256 -or
        (Get-PrismHash (Join-Path $Plan.target $script:PrismDevReceiptFile)) -cne $Plan.deploymentSha256) {
        throw 'Native definition target ownership/deployment drifted since preflight.'
    }
}

function Assert-PrismQuestSnapshot($Plan, [ValidateSet('Before', 'After', 'Recover')][string]$Phase) {
    Assert-PrismQuestBinding $Plan
    $actual = @(Get-PrismQuestSnapshot $Plan.target)
    if ($actual.Count -ne $Plan.chapters.Count) { throw 'Native chapter inventory drifted since preflight.' }
    foreach ($file in $actual) {
        $expected = @($Plan.chapters | Where-Object { $_.fileName -ceq $file.fileName })
        if ($expected.Count -ne 1) { throw 'Unknown native chapter appeared since preflight.' }
        $owned = @($Plan.files | Where-Object { $_.fileName -ceq $file.fileName })
        $valid = $file.sha256 -ceq $expected[0].sha256 -and $file.bytes -eq $expected[0].bytes
        if ($owned.Count -eq 1 -and $Phase -ne 'Before') {
            $after = $file.sha256 -ceq $owned[0].afterSha256 -and $file.bytes -eq $owned[0].afterBytes
            if ($Phase -eq 'After') { $valid = $after } else { $valid = $valid -or $after }
        }
        if (-not $valid) { throw 'Native chapter bytes drifted or were edited; existing files are preserved.' }
    }
}

function Assert-PrismQuestPreviousReceipt($Plan) {
    $path = Get-PrismPath (Join-Path $Plan.target $script:PrismQuestReceiptFile) 'File' -AllowMissing
    if ($null -eq $Plan.previousReceiptSha256) {
        if (Test-Path -LiteralPath $path) { throw 'Native definition ownership appeared since preflight.' }
    } elseif (-not [IO.File]::Exists($path) -or (Get-PrismHash $path) -cne $Plan.previousReceiptSha256) {
        throw 'Native definition ownership drifted since preflight.'
    }
}

function Read-PrismQuestJournal($Plan, [string]$PlanHash) {
    $journal = Read-PrismJson (Join-Path $Plan.target $script:PrismQuestPendingFile)
    Assert-PrismKeys $journal @('schemaVersion', 'tool', 'state', 'operationId', 'planSha256', 'backup') 'native definition journal'
    if (-not (Test-PrismQuestInteger $journal.schemaVersion 1 1) -or $journal.tool -cne 'grindless-ftb-definition-update' -or
        $journal.state -cne 'in-progress' -or $journal.operationId -cnotmatch '^[0-9a-f]{32}$' -or
        $journal.planSha256 -cne $PlanHash -or
        $journal.backup -cne ('.grindless-dev-backups/ftb-' + $journal.operationId)) {
        throw 'Unknown native definition journal; manual inspection is required.'
    }
    return $journal
}

function Restore-PrismQuestDefinitions($Plan, [string]$PlanHash) {
    $journal = Read-PrismQuestJournal $Plan $PlanHash
    $pending = Join-Path $Plan.target $script:PrismQuestPendingFile
    $journalHash = Get-PrismHash $pending
    $backup = Get-PrismPath (Join-Path $Plan.target $journal.backup) 'Directory'
    $receiptPath = Get-PrismPath (Join-Path $Plan.target $script:PrismQuestReceiptFile) 'File' -AllowMissing
    $afterReceiptHash = Get-PrismBytesHash (Get-PrismQuestReceiptBytes $Plan)
    # All current files AND all backup bytes must be admissible before any recovery write.
    Assert-PrismQuestSnapshot $Plan 'Recover'
    if ((Get-PrismHash (Join-Path $backup 'plan.json')) -cne $PlanHash) {
        throw 'Native definition recovery plan backup verification failed.'
    }
    foreach ($file in $Plan.files) {
        $path = Get-PrismPath (Join-Path $backup ('before-' + $file.fileName)) 'File'
        if ((Get-Item -LiteralPath $path).Length -ne $file.beforeBytes -or (Get-PrismHash $path) -cne $file.beforeSha256) {
            throw 'Native definition backup verification failed; recovery is refused.'
        }
    }
    if ($null -ne $Plan.previousReceiptSha256 -and
        (Get-PrismHash (Join-Path $backup 'previous-receipt.json')) -cne $Plan.previousReceiptSha256) {
        throw 'Native definition ownership backup verification failed.'
    }
    $receiptHash = $null
    if ([IO.File]::Exists($receiptPath)) { $receiptHash = Get-PrismHash $receiptPath }
    if ($receiptHash -cne $Plan.previousReceiptSha256 -and $receiptHash -cne $afterReceiptHash) {
        throw 'Native definition ownership was edited; recovery preserves it.'
    }
    foreach ($file in $Plan.files) {
        $path = Join-Path $Plan.target ('minecraft\config\ftbquests\quests\chapters\' + $file.fileName)
        if ((Get-PrismHash $path) -ceq $file.beforeSha256) { continue }
        Assert-PrismQuestSnapshot $Plan 'Recover'
        if ((Get-PrismHash $pending) -cne $journalHash) { throw 'Native definition journal changed during recovery.' }
        $restore = Join-Path $backup ('restore-' + [Guid]::NewGuid().ToString('N') + '.tmp')
        Write-PrismNewBytes $restore ([IO.File]::ReadAllBytes((Join-Path $backup ('before-' + $file.fileName))))
        if ((Get-PrismHash $restore) -cne $file.beforeSha256) { throw 'Native definition restore staging verification failed.' }
        if ((Get-PrismHash $path) -cne $file.afterSha256) { throw 'Native definition changed during recovery.' }
        Set-PrismReplacement $restore $path
    }
    Assert-PrismQuestSnapshot $Plan 'Before'
    if ($receiptHash -cne $Plan.previousReceiptSha256) {
        if ((Get-PrismHash $receiptPath) -cne $afterReceiptHash) { throw 'Native definition ownership changed during recovery.' }
        if ($null -eq $Plan.previousReceiptSha256) {
            [IO.File]::Delete($receiptPath)
        } else {
            $restore = Join-Path $backup ('restore-receipt-' + [Guid]::NewGuid().ToString('N') + '.tmp')
            Write-PrismNewBytes $restore ([IO.File]::ReadAllBytes((Join-Path $backup 'previous-receipt.json')))
            if ((Get-PrismHash $restore) -cne $Plan.previousReceiptSha256) { throw 'Native definition receipt restore verification failed.' }
            Set-PrismReplacement $restore $receiptPath
        }
    }
    Assert-PrismQuestPreviousReceipt $Plan
    if ((Get-PrismHash $pending) -cne $journalHash) { throw 'Native definition journal changed during recovery.' }
    [IO.File]::Delete($pending)
    return $journal.backup
}

function Invoke-PrismQuestDefinitions($Plan, [string]$PlanHash, [string]$InstancesRoot, [string]$PlanPath, [switch]$Recover) {
    $pending = Join-Path $Plan.target $script:PrismQuestPendingFile
    $receiptPath = Join-Path $Plan.target $script:PrismQuestReceiptFile
    $receiptBytes = Get-PrismQuestReceiptBytes $Plan
    if (-not $Recover) {
        if (Test-Path -LiteralPath $pending) { throw 'An incomplete native definition update requires explicit recovery.' }
        Assert-PrismQuestBinding $Plan
        if ([IO.File]::Exists($receiptPath) -and (Get-PrismHash $receiptPath) -ceq (Get-PrismBytesHash $receiptBytes)) {
            Assert-PrismQuestSnapshot $Plan 'After'
            return [pscustomobject]@{ Outcome = 'Unchanged'; Backup = $null }
        }
        Assert-PrismQuestSnapshot $Plan 'Before'
        Assert-PrismQuestPreviousReceipt $Plan
    } else {
        $null = Read-PrismQuestJournal $Plan $PlanHash
        Assert-PrismQuestSnapshot $Plan 'Recover'
    }
    $lockPath = Get-PrismPath (Join-Path $InstancesRoot ('.' + $Plan.instanceId + '.grindless-deploy.lock')) 'File' -AllowMissing
    $lock = $null
    $ownsLock = $false
    try {
        # A crashed attempt may leave our own lock. Recovery must acquire it exclusively,
        # with the exact plan identity; a running or foreign deployment is never bypassed.
        if ($Recover -and [IO.File]::Exists($lockPath)) {
            try { $lock = [IO.File]::Open($lockPath, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None) }
            catch { throw 'Native definition recovery lock is active or uninspectable; it is preserved.' }
            $reader = New-Object IO.StreamReader($lock, [Text.Encoding]::UTF8, $true, 1024, $true)
            try { $text = $reader.ReadToEnd() } finally { $reader.Dispose() }
            if ($text -cne ('grindless-ftb-definition-update:' + $PlanHash)) {
                throw 'Foreign deployment lock is preserved; native definition recovery is refused.'
            }
            $ownsLock = $true
        } else {
            try { $lock = [IO.File]::Open($lockPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None) }
            catch { throw 'Another deployment or a stale deployment lock exists; inspect it manually.' }
            $ownsLock = $true
            $bytes = [Text.Encoding]::UTF8.GetBytes('grindless-ftb-definition-update:' + $PlanHash)
            $lock.Write($bytes, 0, $bytes.Length); $lock.Flush($true)
        }
        if ($Recover) {
            $backup = Restore-PrismQuestDefinitions $Plan $PlanHash
            return [pscustomobject]@{ Outcome = 'DefinitionsRestored'; Backup = $backup }
        }
        Assert-PrismQuestSnapshot $Plan 'Before'
        Assert-PrismQuestPreviousReceipt $Plan
        $operation = [Guid]::NewGuid().ToString('N')
        $relativeBackup = '.grindless-dev-backups/ftb-' + $operation
        $backup = Get-PrismPath (Join-Path $Plan.target $relativeBackup) 'Directory' -AllowMissing
        if (Test-Path -LiteralPath $backup) { throw 'Native definition backup target already exists.' }
        [IO.Directory]::CreateDirectory($backup) | Out-Null
        $null = Get-PrismPath $PlanPath 'File'
        Write-PrismNewBytes (Join-Path $backup 'plan.json') ([IO.File]::ReadAllBytes($PlanPath))
        if ((Get-PrismHash (Join-Path $backup 'plan.json')) -cne $PlanHash) {
            throw 'Native definition plan changed during backup; no installed file was replaced.'
        }
        foreach ($file in $Plan.files) {
            $path = Join-Path $Plan.target ('minecraft\config\ftbquests\quests\chapters\' + $file.fileName)
            Write-PrismNewBytes (Join-Path $backup ('before-' + $file.fileName)) ([IO.File]::ReadAllBytes($path))
            Write-PrismNewBytes (Join-Path $backup ('after-' + $file.fileName)) ([Convert]::FromBase64String($file.contentsBase64))
            if ((Get-PrismHash (Join-Path $backup ('before-' + $file.fileName))) -cne $file.beforeSha256 -or
                (Get-PrismHash (Join-Path $backup ('after-' + $file.fileName))) -cne $file.afterSha256) {
                throw 'Native definition backup/staging verification failed; no installed file was replaced.'
            }
        }
        if ($null -ne $Plan.previousReceiptSha256) {
            Write-PrismNewBytes (Join-Path $backup 'previous-receipt.json') ([IO.File]::ReadAllBytes($receiptPath))
            if ((Get-PrismHash (Join-Path $backup 'previous-receipt.json')) -cne $Plan.previousReceiptSha256) {
                throw 'Native definition ownership backup verification failed.'
            }
        }
        Write-PrismNewBytes (Join-Path $backup 'new-receipt.json') $receiptBytes
        if ((Get-PrismHash (Join-Path $backup 'new-receipt.json')) -cne (Get-PrismBytesHash $receiptBytes)) {
            throw 'Native definition ownership staging verification failed.'
        }
        Assert-PrismQuestSnapshot $Plan 'Before'
        Assert-PrismQuestPreviousReceipt $Plan
        $journal = [ordered]@{ schemaVersion = 1; tool = 'grindless-ftb-definition-update'; state = 'in-progress'
            operationId = $operation; planSha256 = $PlanHash; backup = $relativeBackup }
        Write-PrismNewJson $pending $journal
        $journalHash = Get-PrismHash $pending
        try {
            foreach ($file in $Plan.files) {
                Assert-PrismQuestSnapshot $Plan 'Recover'
                Assert-PrismQuestPreviousReceipt $Plan
                if ((Get-PrismHash $pending) -cne $journalHash) { throw 'Native definition journal changed during update.' }
                $path = Join-Path $Plan.target ('minecraft\config\ftbquests\quests\chapters\' + $file.fileName)
                if ((Get-PrismHash $path) -cne $file.beforeSha256) { throw 'Native definition changed immediately before replacement.' }
                if ($file.beforeSha256 -cne $file.afterSha256) {
                    Set-PrismReplacement (Join-Path $backup ('after-' + $file.fileName)) $path
                }
            }
            Assert-PrismQuestSnapshot $Plan 'After'
            Assert-PrismQuestPreviousReceipt $Plan
            if ($null -eq $Plan.previousReceiptSha256) {
                [IO.File]::Move((Join-Path $backup 'new-receipt.json'), $receiptPath)
            } else { Set-PrismReplacement (Join-Path $backup 'new-receipt.json') $receiptPath }
            if ((Get-PrismHash $receiptPath) -cne (Get-PrismBytesHash $receiptBytes)) {
                throw 'Installed native definition ownership verification failed.'
            }
            Assert-PrismQuestSnapshot $Plan 'After'
            if ((Get-PrismHash $pending) -cne $journalHash) { throw 'Native definition journal changed during update.' }
            [IO.File]::Delete($pending)
        } catch {
            $failure = $_.Exception.Message
            try { $null = Restore-PrismQuestDefinitions $Plan $PlanHash }
            catch {
                $recoveryFailure = $_.Exception.Message
                throw "Native definition update failed and recovery is incomplete. Its journal and verified backups are preserved; no success is accepted. Use RecoverQuestDefinitions with the original plan. Update: $failure Recovery: $recoveryFailure"
            }
            throw "Native definition update failed; original chapters and ownership receipt were restored. $failure"
        }
        return [pscustomobject]@{ Outcome = 'DefinitionsUpdated'; Backup = $relativeBackup }
    } finally {
        if ($null -ne $lock) {
            $lock.Dispose()
            if ($ownsLock) { [IO.File]::Delete($lockPath) }
        }
    }
}

        function Invoke-PrismUpdate($Plan, [string]$Stage, $NewReceipt) {
            $target = $Plan.Target
            $old = $Plan.State.Receipt
            $operation = $NewReceipt.operationId
            $backup = Join-Path $target ('.grindless-dev-backups/' + $operation)
            $pending = Join-Path $target $script:PrismDevPendingFile
            $receiptPath = Join-Path $target $script:PrismDevReceiptFile
            $oldJar = Join-Path $target ('minecraft/mods/' + $old.grindless.fileName)
            $newJar = Join-Path $target ('minecraft/mods/' + $NewReceipt.grindless.fileName)
            $stageJar = Join-Path $Stage ('minecraft/mods/' + $NewReceipt.grindless.fileName)
            $backupJar = Join-Path $backup $old.grindless.fileName
            $backupReceipt = Join-Path $backup 'previous-receipt.json'
            $null = Get-PrismPath $backup 'Directory' -AllowMissing
            [IO.Directory]::CreateDirectory($backup) | Out-Null
            [IO.File]::Copy($oldJar, $backupJar, $false)
            [IO.File]::Copy($receiptPath, $backupReceipt, $false)
            if ((Get-PrismHash $backupJar) -cne $old.grindless.sha256) { throw 'Previous Grindless backup verification failed.' }
            $oldReceiptHash = Get-PrismHash $receiptPath
            if ((Get-PrismHash $backupReceipt) -cne $oldReceiptHash) { throw 'Previous receipt backup verification failed.' }
            Write-PrismNewJson $pending ([ordered]@{ schemaVersion = 1; state = 'in-progress'; operationId = $operation
                previousPackSha256 = $old.packSha256; requestedPackSha256 = $NewReceipt.packSha256
                backup = ('.grindless-dev-backups/' + $operation) })
            $jarTouched = $false
            $receiptTouched = $false
            try {
                # Backups are complete, outside mods, BEFORE replacing the owned runtime.
                if ($old.grindless.sha256 -cne $NewReceipt.grindless.sha256 -or $oldJar -ine $newJar) {
                    if ($oldJar -ieq $newJar) {
                        Set-PrismReplacement $stageJar $newJar
                        $jarTouched = $true
                    } else {
                        [IO.File]::Move($oldJar, (Join-Path $backup 'previous-original.jar'))
                        $jarTouched = $true
                        [IO.File]::Move($stageJar, $newJar)
                    }
                }
                if ((Get-PrismHash $newJar) -cne $NewReceipt.grindless.sha256) { throw 'Installed Grindless verification failed.' }
                Set-PrismReplacement (Join-Path $Stage $script:PrismDevReceiptFile) $receiptPath
                $receiptTouched = $true
                [IO.File]::Delete($pending)
            } catch {
                $failure = $_.Exception.Message
                try {
                    if ($jarTouched) {
                        if ([IO.File]::Exists($newJar)) {
                            if ((Get-PrismHash $newJar) -cne $NewReceipt.grindless.sha256) {
                                throw 'The new mod changed during rollback.'
                            }
                            [IO.File]::Move($newJar, (Join-Path $backup 'failed-new.jar'))
                        }
                        [IO.File]::Copy($backupJar, $oldJar, $false)
                    }
                    if ($receiptTouched) {
                        $restore = Join-Path $Stage 'restore-receipt.json'
                        [IO.File]::Copy($backupReceipt, $restore, $false)
                        Set-PrismReplacement $restore $receiptPath
                    }
                    if ((Get-PrismHash $oldJar) -cne $old.grindless.sha256 -or
                        (Get-PrismHash $receiptPath) -cne $oldReceiptHash) { throw 'Rollback hash verification failed.' }
                    [IO.File]::Delete($pending)
                } catch {
                    throw 'Deployment failed and rollback is incomplete. The in-progress journal and backups are preserved; no completed deployment is accepted.'
                }
                throw "Deployment failed; previous managed runtime and receipt were restored. $failure"
            }
        }

        function Invoke-PrismInstall($Plan) {
            $operation = [Guid]::NewGuid().ToString('N')
            # InstancesRoot is actively scanned by Prism, including dot-prefixed
            # directories. Stage next to it on the same volume, never inside it.
            $stageParent = [IO.Path]::GetDirectoryName($Plan.InstancesRoot)
            $stage = Join-Path $stageParent ('.' + $Plan.InstanceId + '.grindless-stage-' + $operation)
            $lockPath = Join-Path $Plan.InstancesRoot ('.' + $Plan.InstanceId + '.grindless-deploy.lock')
            $null = Get-PrismPath $lockPath 'File' -AllowMissing
            $lock = $null
            $stageCreated = $false
            try {
                try { $lock = [IO.File]::Open($lockPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None) }
                catch { throw 'Another deployment or a stale deployment lock exists; inspect it manually.' }
                $null = Get-PrismPath $stage 'Directory' -AllowMissing
                New-Item -ItemType Directory -Path $stage -ErrorAction Stop | Out-Null
                $stageCreated = $true
                Write-PrismNewJson (Join-Path $stage '.grindless-stage.json') @{
                    tool = $script:PrismDevTool; state = 'staging'; operationId = $operation; instanceId = $Plan.InstanceId
                }
                $mods = Join-Path $stage 'minecraft/mods'
                [IO.Directory]::CreateDirectory($mods) | Out-Null
                Write-PrismNewBytes (Join-Path $mods $Plan.Package.JarName) $Plan.Package.JarBytes
                $dependencyReceipts = @()
                foreach ($pin in $Plan.Contract.Dependencies) {
                    $destination = Join-Path $mods $pin.fileName
                    if ($Plan.Sources.ContainsKey($pin.fileName)) {
                        $null = Get-PrismPath $Plan.Sources[$pin.fileName] 'File'
                        [IO.File]::Copy($Plan.Sources[$pin.fileName], $destination, $false)
                    } else { Save-PrismOfficialPin $pin $destination }
                    Assert-PrismDependency $destination $pin
                    $record = (ConvertTo-Json $pin -Depth 30) | ConvertFrom-Json
                    $actualSha256 = Get-PrismHash $destination
                    if ($null -ne $record.PSObject.Properties['sha256']) {
                        if ($record.sha256 -cne $actualSha256) { throw 'Staged dependency SHA256 mismatch.' }
                    } else {
                        $record | Add-Member -NotePropertyName sha256 -NotePropertyValue $actualSha256
                    }
                    $dependencyReceipts += $record
                }
                if ((Get-PrismHash (Join-Path $mods $Plan.Package.JarName)) -cne $Plan.Package.JarSha256) {
                    throw 'Staged Grindless SHA256 mismatch.'
                }
                if ($null -ne $Plan.State) { $owner = $Plan.State.Owner } else {
                    $owner = [ordered]@{ schemaVersion = 1; tool = $script:PrismDevTool
                        purpose = 'engineering-development'; instanceId = $Plan.InstanceId
                        ownershipId = [Guid]::NewGuid().ToString('N') }
                }
                $receipt = [ordered]@{
                    schemaVersion = 1; tool = $script:PrismDevTool; state = 'deployed'
                    instanceId = $Plan.InstanceId; ownershipId = $owner.ownershipId; operationId = $operation
                    deployedUtc = [DateTime]::UtcNow.ToString('o')
                    sourceRevision = $Plan.Package.Provenance.sourceRevision
                    runtimeSourceRevision = $Plan.Package.Provenance.runtimeSourceRevision
                    packSha256 = $Plan.Package.Sha256; packStatus = $script:PrismDevStatus
                    minecraft = $Plan.Contract.Properties.minecraft_version; forge = $Plan.Contract.Properties.forge_version
                    java = '17'; javaExecutable = $Plan.Java.Path; javaSha256 = $Plan.Java.Sha256; maxMemoryMiB = 4096
                    componentMetadata = $Plan.Components
                    grindless = @{ fileName = $Plan.Package.JarName; bytes = $Plan.Package.JarBytes.Length; sha256 = $Plan.Package.JarSha256 }
                    dependencies = $dependencyReceipts
                }
                Write-PrismNewJson (Join-Path $stage $script:PrismDevReceiptFile) $receipt
                # Recheck source, paths and live processes AFTER staging/downloads, before install.
                $null = Assert-PrismSource $Plan.Root $Plan.Package.Provenance.sourceRevision
                $null = Get-PrismPath $Plan.Target 'Directory' -AllowMissing
                Assert-PrismStopped $Plan.Target
                if ($null -ne $Plan.State) {
                    $current = Get-PrismOwnedState $Plan.Target $Plan.InstanceId
                    Assert-PrismEqual $current.Receipt $Plan.State.Receipt 'Previous deployment changed during staging'
                    Invoke-PrismUpdate $Plan $stage $receipt
                } else {
                    if (Test-Path -LiteralPath $Plan.Target) { throw 'Target appeared during staging; refusing to adopt it.' }
                    Write-PrismNewJson (Join-Path $stage $script:PrismDevOwnerFile) $owner
                    Write-PrismNewJson (Join-Path $stage 'mmc-pack.json') $Plan.Components
                    Write-PrismNewBytes (Join-Path $stage 'instance.cfg') `
                        ([Text.Encoding]::UTF8.GetBytes((Get-PrismCfgText $Plan.InstanceId $Plan.Java.Path)))
                    Assert-PrismTree $stage
                    [IO.File]::Delete((Join-Path $stage '.grindless-stage.json'))
                    [IO.Directory]::Move($stage, $Plan.Target)
                }
            } finally {
                # Cleanup is confined to this freshly allocated same-filesystem staging root.
                # Do not delete a linked/uninspectable stage or somebody else's lock.
                try {
                    if ($stageCreated -and [IO.Directory]::Exists($stage)) {
                        Assert-PrismTree $stage
                        Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction Stop
                    }
                } finally {
                    if ($null -ne $lock) { $lock.Dispose(); [IO.File]::Delete($lockPath) }
                }
            }
        }

function Get-PrismBuildResources([string]$Root, [string]$GradleHome) {
    try {
        $os = @(Get-CimInstance -ClassName Win32_OperatingSystem -ErrorAction Stop)
        $memory = @(Get-CimInstance -ClassName Win32_PerfFormattedData_PerfOS_Memory -ErrorAction Stop)
        if ($os.Count -ne 1 -or $memory.Count -ne 1 -or $null -eq $os[0].FreePhysicalMemory -or
            $null -eq $memory[0].CommitLimit -or $null -eq $memory[0].CommittedBytes -or
            [double]$memory[0].CommitLimit -le 0 -or
            [double]$memory[0].CommittedBytes -gt [double]$memory[0].CommitLimit) { throw 'Unknown memory metrics.' }
        $disks = @()
        foreach ($volume in @(@([IO.Path]::GetPathRoot($Root), [IO.Path]::GetPathRoot($GradleHome)) | Select-Object -Unique)) {
            $drive = New-Object IO.DriveInfo($volume)
            if (-not $drive.IsReady -or $drive.DriveType -ne [IO.DriveType]::Fixed) { throw 'Unknown work-disk metrics.' }
            $disks += [pscustomobject]@{ AvailableBytes = $drive.AvailableFreeSpace }
        }
        return [pscustomobject]@{ AvailablePhysicalMiB = ([double]$os[0].FreePhysicalMemory / 1024)
            AvailableCommitMiB = (([double]$memory[0].CommitLimit - [double]$memory[0].CommittedBytes) / 1MB)
            WorkDisks = $disks }
    } catch { throw 'Cannot measure fresh physical/commit/work-disk build headroom; no Gradle launch is permitted.' }
}

function Assert-PrismBuildResources($Metrics, $Properties) {
    $heap = [Regex]::Matches($Properties['org.gradle.jvmargs'], '(?:^|\s)-Xmx([1-9][0-9]*)([KMGkmg])(?=\s|$)')
    if ($heap.Count -ne 1) { throw 'Cannot budget the committed Gradle maximum heap.' }
    $scale = @{ k = (1.0 / 1024); m = 1.0; g = 1024.0 }
    $heapMiB = [Math]::Ceiling([double]$heap[0].Groups[1].Value * $scale[$heap[0].Groups[2].Value.ToLowerInvariant()])
    # Current 3 GiB Gradle heap + 3 GiB physical helper/native/OS margin and
    # +5 GiB commit margin. Do not reuse the retired constrained-VM caps.
    $physicalRequired = $heapMiB + 3072
    $commitRequired = $heapMiB + 5120
    if ($null -eq $Metrics) { throw 'Unknown build resource metrics; no Gradle launch is permitted.' }
    foreach ($key in @('AvailablePhysicalMiB', 'AvailableCommitMiB', 'WorkDisks')) {
        if ($null -eq $Metrics.PSObject.Properties[$key] -or $null -eq $Metrics.$key) {
            throw 'Unknown build resource metrics; no Gradle launch is permitted.'
        }
    }
    foreach ($value in @($Metrics.AvailablePhysicalMiB, $Metrics.AvailableCommitMiB) +
            @($Metrics.WorkDisks | ForEach-Object { $_.AvailableBytes })) {
        if ($null -eq $value -or $value -is [string] -or [double]::IsNaN([double]$value) -or
            [double]::IsInfinity([double]$value) -or [double]$value -lt 0) {
            throw 'Unknown build resource metrics; no Gradle launch is permitted.'
        }
    }
    if (@($Metrics.WorkDisks).Count -lt 1 -or @($Metrics.WorkDisks).Count -gt 2 -or
        $Metrics.AvailablePhysicalMiB -lt $physicalRequired -or
        $Metrics.AvailableCommitMiB -lt $commitRequired -or
        @($Metrics.WorkDisks | Where-Object { $_.AvailableBytes -lt 8GB }).Count -ne 0) {
        throw "Insufficient build headroom: need $physicalRequired MiB free physical, $commitRequired MiB free commit and 8 GiB free on each work/cache volume."
    }
}

function Assert-PrismBuildIdle([string]$Root, [string]$GradleHome) {
    foreach ($process in @(Get-PrismJavaProcesses)) {
        if ([string]::IsNullOrWhiteSpace($process.CommandLine) -or [string]::IsNullOrWhiteSpace($process.ExecutablePath)) {
            throw 'Uninspectable Java build ownership; no Gradle launch is permitted.'
        }
        $command = $process.CommandLine.Replace('\', '/').ToLowerInvariant()
        if ($command -match '(org\.gradle\.|gradledaemon|gradleworkermain|kotlincompiledaemon|dev\.architectury\.transformer|net\.fabricmc\.loom)' -or
            $command.Contains($Root.Replace('\', '/').ToLowerInvariant()) -or
            $command.Contains($GradleHome.Replace('\', '/').ToLowerInvariant())) {
            throw 'Competing agent/Gradle or shared-worktree Java execution; no Gradle launch is permitted.'
        }
        # A stopped Prism target is NOT a build requirement. Other inspectable
        # Java processes, including the player's game, are covered by headroom.
    }
}

function Invoke-PrismGradle([string]$Wrapper, [string[]]$Arguments) {
    & $Wrapper @Arguments | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'verifyModpack/buildModpack failed; no deployment was attempted.' }
}

function Invoke-PrismBuild([string]$Root, [string]$Expected, [string]$Runtime) {
    Assert-PrismRevision $Expected
    $null = Assert-PrismSource $Root $Expected
    if (-not $env:JAVA_HOME -or -not $env:GRADLE_USER_HOME) {
        throw 'Build requires inherited process-local JAVA_HOME and GRADLE_USER_HOME.'
    }
    $javaHome = Get-PrismPath $env:JAVA_HOME 'Directory'
    $gradleHome = Get-PrismPath $env:GRADLE_USER_HOME 'Directory' -AllowMissing
    if (Test-PrismWithin $gradleHome $Root) { throw 'Use the isolated external process-local Gradle home.' }
    $javaExe = Join-Path $javaHome 'bin/java.exe'
    $null = Get-PrismJava $javaExe (Get-PrismHash $javaExe)
    $wrapper = Get-PrismPath (Join-Path $Root 'gradlew.bat') 'File'
    $contract = Get-PrismContract $Root
    $arguments = @('verifyModpack', 'buildModpack', '-PpackProfile=playable', '--console=plain',
        '--no-daemon', '--no-parallel', '--max-workers=1')
    if ($Runtime) {
        Assert-PrismRuntime $Root $Expected $Runtime
        $arguments += "-PpackRuntimeRevision=$Runtime"
    }
    $buildDirectory = Get-PrismPath (Join-Path $Root 'build') 'Directory' -AllowMissing
    $locks = @()
    try {
        foreach ($directory in @($buildDirectory, $gradleHome)) {
            $null = Get-PrismPath $directory 'Directory' -AllowMissing
            [IO.Directory]::CreateDirectory($directory) | Out-Null
            $path = Get-PrismPath (Join-Path $directory '.grindless-prism-build.lock') 'File' -AllowMissing
            try { $handle = [IO.File]::Open($path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None) }
            catch { throw 'An active or stale worktree/cache build lock exists; no Gradle launch is permitted.' }
            $locks += [pscustomobject]@{ Path = $path; Handle = $handle }
            $bytes = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json @{
                tool = $script:PrismDevTool; state = 'building'; revision = $Expected; processId = $PID
            } -Compress))
            $handle.Write($bytes, 0, $bytes.Length); $handle.Flush($true)
        }
        # Fresh measurements after acquiring both non-waiting locks, immediately
        # before launching. Unknown/insufficient state never reaches Gradle.
        Assert-PrismBuildIdle $Root $gradleHome
        Assert-PrismBuildResources (Get-PrismBuildResources $Root $gradleHome) $contract.Properties
        $null = Assert-PrismSource $Root $Expected
        Push-Location $Root
        try { Invoke-PrismGradle $wrapper $arguments } finally { Pop-Location }
        $null = Assert-PrismSource $Root $Expected
        $zipPath = Join-Path $Root ("dist/Grindless-T0-$($contract.Properties.pack_version)-playable-$($Expected.Substring(0, 7)).zip")
        $package = Read-PrismPackage $Root $zipPath $contract $Expected
        if ($Runtime -and $package.Provenance.runtimeSourceRevision -cne $Runtime) {
            throw 'Built package runtime provenance differs from RuntimeRevision.'
        }
        return $package
    } finally {
        foreach ($entry in $locks) { $entry.Handle.Dispose(); [IO.File]::Delete($entry.Path) }
    }
}

        function Invoke-PrismDev {
            [CmdletBinding()]
            param(
                [ValidateSet('Prepare', 'Deploy', 'Inspect', 'Build', 'BuildAndDeploy', 'UpdateQuestDefinitions', 'RecoverQuestDefinitions')][string]$Action = 'Inspect',
                [string]$Root,
                [string]$InstancesRoot, [string]$InstanceId = 'Grindless-T0-Dev', [string]$TemplateInstance,
                [string]$JavaPath, [string]$JavaSha256, [string]$PackZip, [string]$ExpectedRevision,
                [string]$RuntimeRevision, [string]$QuestUpdatePlan, [string]$QuestUpdateSha256,
                [string[]]$DependencyDirectory = @()
            )
            if ($env:OS -ne 'Windows_NT') { throw 'The host-side Prism deployment tool targets Windows with CIM process inspection.' }
            if (-not $Root) { $Root = $script:PrismDevRepositoryRoot }
            $Root = Get-PrismPath ([IO.Path]::GetFullPath($Root)) 'Directory'
            if ($Action -eq 'Build') {
                if ($PackZip) { throw 'Build selects its exact source-bound output; PackZip is a Deploy input.' }
                Assert-PrismRevision $ExpectedRevision
                $built = Invoke-PrismBuild $Root $ExpectedRevision $RuntimeRevision
                return [pscustomobject]@{ Action = 'Build'; Outcome = 'BuiltVerified'; SourceRevision = $ExpectedRevision
                    RuntimeSourceRevision = $built.Provenance.runtimeSourceRevision; PackZip = $built.Path
                    GrindlessSha256 = $built.JarSha256; PackSha256 = $built.Sha256; PackStatus = $script:PrismDevStatus }
            }
            Assert-PrismLeaf $InstanceId
            if ($InstanceId -cnotmatch '^Grindless-[A-Za-z0-9._+-]+-Dev$') {
                throw 'Use a dedicated Grindless-...-Dev engineering instance ID.'
            }
            $instances = Get-PrismPath $InstancesRoot 'Directory'
            $target = Get-PrismPath (Join-Path $instances $InstanceId) 'Directory' -AllowMissing
            if ((Test-PrismWithin $target $Root) -or (Test-PrismWithin $Root $target)) {
                throw 'Repository and engineering instance paths must be separate.'
            }
            Assert-PrismStopped $target
            $state = $null
            if (Test-Path -LiteralPath $target) {
                $state = Get-PrismOwnedState $target $InstanceId -AllowQuestRecovery:($Action -eq 'RecoverQuestDefinitions')
            }
            if ($Action -eq 'Inspect') {
                if ($null -eq $state) { throw 'The dedicated engineering instance has not been deployed.' }
                return [pscustomobject]@{ Action = 'Inspect'; InstanceId = $InstanceId; Outcome = 'VerifiedStopped'
                    SourceRevision = $state.Receipt.sourceRevision; RuntimeSourceRevision = $state.Receipt.runtimeSourceRevision
                    GrindlessSha256 = $state.Receipt.grindless.sha256; PackSha256 = $state.Receipt.packSha256
                    PackStatus = $state.Receipt.packStatus }
            }
            if ($ExpectedRevision) { Assert-PrismRevision $ExpectedRevision }
            $source = Assert-PrismSource $Root $ExpectedRevision
            if ($Action -in @('UpdateQuestDefinitions', 'RecoverQuestDefinitions')) {
                if ($null -eq $state) { throw 'Native definition updates require an already owned engineering instance.' }
                Assert-PrismRevision $ExpectedRevision
                $plan = Read-PrismQuestPlan $QuestUpdatePlan $QuestUpdateSha256 $target $state
                if ($Action -eq 'UpdateQuestDefinitions') {
                    $contract = Get-PrismContract $Root
                    $package = Read-PrismPackage $Root $PackZip $contract $source
                    if ($plan.sourceRevision -cne $source -or
                        $plan.runtimeSourceRevision -cne $package.Provenance.runtimeSourceRevision -or
                        $plan.grindlessSha256 -cne $package.JarSha256) {
                        throw 'Native definition publisher plan does not match the verified source/runtime package.'
                    }
                    if ($state.Receipt.sourceRevision -cne $plan.sourceRevision -or
                        $state.Receipt.runtimeSourceRevision -cne $plan.runtimeSourceRevision -or
                        $state.Receipt.grindless.sha256 -cne $plan.grindlessSha256 -or
                        $state.Receipt.packSha256 -cne $package.Sha256) {
                        throw 'Deploy the verified current package while stopped, then produce a fresh definition plan before starting the game.'
                    }
                }
                $result = Invoke-PrismQuestDefinitions $plan $QuestUpdateSha256 $instances $QuestUpdatePlan -Recover:($Action -eq 'RecoverQuestDefinitions')
                return [pscustomobject]@{ Action = $Action; InstanceId = $InstanceId; Outcome = $result.Outcome
                    SourceRevision = $plan.sourceRevision; RuntimeSourceRevision = $plan.runtimeSourceRevision
                    GrindlessSha256 = $plan.grindlessSha256; PlanSha256 = $QuestUpdateSha256
                    Backup = $result.Backup; PackStatus = $script:PrismDevStatus }
            }
            if ($QuestUpdatePlan -or $QuestUpdateSha256) { throw 'Native definition plans require an explicit definition update/recovery action.' }
            $contract = Get-PrismContract $Root
            $forge = $contract.Properties.forge_version.Substring($contract.Properties.minecraft_version.Length + 1)
            $components = $null
            if ($TemplateInstance) {
                $TemplateInstance = Get-PrismPath $TemplateInstance 'Directory'
                if ((Test-PrismWithin $TemplateInstance $target) -or (Test-PrismWithin $target $TemplateInstance)) {
                    throw 'Template and target instances must be separate.'
                }
                Assert-PrismNoPatches $TemplateInstance
                $components = Get-PrismComponents (Read-PrismJson (Join-Path $TemplateInstance 'mmc-pack.json')) `
                    $contract.Properties.minecraft_version $forge
            }
            if ($null -ne $state) {
                if (-not $JavaPath) { $JavaPath = $state.Java.Path; $JavaSha256 = $state.Java.Sha256 }
                if ($null -ne $components) {
                    Assert-PrismEqual (Get-PrismComponentIdentity $components) `
                        (Get-PrismComponentIdentity $state.Components) 'Template/installed components'
                }
                $components = $state.Components
                $previousPins = @($state.Receipt.dependencies | ForEach-Object {
                    $copy = (ConvertTo-Json $_ -Depth 30) | ConvertFrom-Json
                    $declaredPin = @($contract.Dependencies | Where-Object { $_.projectID -eq $copy.projectID })
                    # Strip only receipt-only measurements, never a committed digest.
                    if ($declaredPin.Count -ne 1 -or $null -eq $declaredPin[0].PSObject.Properties['sha256']) {
                        $copy.PSObject.Properties.Remove('sha256')
                    }
                    $copy
                })
                Assert-PrismEqual $previousPins @($contract.Dependencies) 'Installed dependency pins (pin migrations require a new instance)'
            } elseif ($null -eq $components) { throw 'A working compatible TemplateInstance is required for new instances.' }
            $java = Get-PrismJava $JavaPath $JavaSha256
            if ($null -ne $state -and ($java.Path -ine $state.Java.Path -or $java.Sha256 -cne $state.Java.Sha256)) {
                throw 'This bounded updater does not migrate an existing instance Java installation.'
            }
            if ($Action -eq 'BuildAndDeploy') {
                if ($PackZip) { throw 'BuildAndDeploy selects its exact source-bound output; use Deploy for an explicit PackZip.' }
                Assert-PrismRevision $ExpectedRevision
                $null = Invoke-PrismBuild $Root $ExpectedRevision $RuntimeRevision
                $PackZip = Join-Path $Root ("dist/Grindless-T0-$($contract.Properties.pack_version)-playable-$($source.Substring(0, 7)).zip")
            } elseif ($RuntimeRevision) { throw 'RuntimeRevision is only a Build/BuildAndDeploy input; Deploy verifies archived runtime provenance.' }
            if (-not $PackZip) { throw 'Prepare/Deploy requires an explicit verified playable PackZip.' }
            $package = Read-PrismPackage $Root $PackZip $contract $source
            if ($RuntimeRevision -and $package.Provenance.runtimeSourceRevision -cne $RuntimeRevision) {
                throw 'Built package runtime provenance differs from the requested RuntimeRevision.'
            }
            $ownedTarget = ''
            if ($null -ne $state) { $ownedTarget = $target }
            $sources = Get-PrismDependencySources $contract $TemplateInstance $DependencyDirectory $ownedTarget
            $missing = @($contract.Dependencies | Where-Object { -not $sources.ContainsKey($_.fileName) } |
                ForEach-Object { "$($_.projectID)/$($_.fileID)" })
            $outcome = 'PreflightVerified'
            if ($Action -ne 'Prepare') {
                if ($null -ne $state -and $package.Sha256 -ceq $state.Receipt.packSha256) { $outcome = 'Unchanged' }
                else {
                    $plan = [pscustomobject]@{ Root = $Root; InstancesRoot = $instances; Target = $target; InstanceId = $InstanceId
                        State = $state; Components = $components; Java = $java; Contract = $contract; Package = $package; Sources = $sources }
                    Invoke-PrismInstall $plan
                    $outcome = 'Deployed'
                }
            }
            return [pscustomobject]@{ Action = $Action; InstanceId = $InstanceId; Outcome = $outcome
                SourceRevision = $source; RuntimeSourceRevision = $package.Provenance.runtimeSourceRevision
                GrindlessSha256 = $package.JarSha256; PackSha256 = $package.Sha256; PackStatus = $script:PrismDevStatus
                MissingLocalDependencies = @(if ($Action -eq 'Prepare') { $missing })
                DownloadedDependencies = @(if ($outcome -eq 'Deployed') { $missing }) }
        }

if ($MyInvocation.InvocationName -ne '.') { Invoke-PrismDev @PSBoundParameters }
