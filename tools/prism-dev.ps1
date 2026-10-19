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
#>
[CmdletBinding()]
param(
    [ValidateSet('Prepare', 'Deploy', 'Inspect', 'Build', 'BuildAndDeploy')][string]$Action = 'Inspect',
    [string]$Root,
    [string]$InstancesRoot,
    [string]$InstanceId = 'Grindless-T0-Dev',
    [string]$TemplateInstance,
    [string]$JavaPath,
    [string]$JavaSha256,
    [string]$PackZip,
    [string]$ExpectedRevision,
    [string]$RuntimeRevision,
    [string[]]$DependencyDirectory = @()
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
$script:PrismDevTool = 'grindless-prism-dev'
$script:PrismDevStatus = 'restricted-playable-profile-not-rc'
$script:PrismDevProjects = @(419699, 238222, 845831, 570073, 790626, 429235, 908741)
$script:PrismDevOwnerFile = '.grindless-dev-owner.json'
$script:PrismDevReceiptFile = '.grindless-dev-deployment.json'
$script:PrismDevPendingFile = '.grindless-dev-pending.json'
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
        if (Test-Path -LiteralPath $current) {
            $item = Get-Item -LiteralPath $current -Force -ErrorAction Stop
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw 'Reparse points and symlinks are not allowed.'
            }
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
            'archives_base_name', 'mod_version', 'architectury_api_version', 'jei_version',
            'heracles_version', 'heracles_curse_file', 'resourcefullib_version', 'resourcefullib_curse_file')) {
        if (-not $values.ContainsKey($key) -or -not $values[$key]) { throw 'Missing required Gradle version property.' }
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
    Assert-PrismKeys $profiles.playable @('name', 'status', 'projectIDs') 'playable profile'
    if ($profiles.playable.status -cne $script:PrismDevStatus -or -not $profiles.playable.name) {
        throw 'The selected profile must retain its restricted, not-RC status.'
    }
    Assert-PrismEqual @($profiles.playable.projectIDs | Sort-Object) `
        @($script:PrismDevProjects | Sort-Object) 'Seven-project playable selection'
    if ($manifest.manifestType -cne 'minecraftModpack' -or $manifest.manifestVersion -ne 1 -or
        $manifest.overrides -cne 'overrides' -or $manifest.version -cne $props.pack_version -or
        $manifest.minecraft.version -cne $props.minecraft_version -or
        @($manifest.minecraft.modLoaders).Count -ne 1 -or
        $manifest.minecraft.modLoaders[0].primary -ne $true -or
        $manifest.minecraft.modLoaders[0].id -isnot [string] -or
        -not $manifest.minecraft.modLoaders[0].id.StartsWith('forge-') -or
        ($props.minecraft_version + '-' + $manifest.minecraft.modLoaders[0].id.Substring(6)) -cne $props.forge_version -or
        $metadata.minecraft -cne $props.minecraft_version -or $metadata.loader -cne 'Forge') {
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
    $selected = @($manifest.files | Where-Object { $_.projectID -in $profiles.playable.projectIDs })
    if ($selected.Count -ne 7) { throw 'Missing playable dependency pin.' }
    $dependencies = @()
    foreach ($pin in $selected) {
        $records = @($metadata.files | Where-Object { $_.projectID -eq $pin.projectID -and $_.fileID -eq $pin.fileID })
        if ($records.Count -ne 1) { throw 'Missing or duplicate dependency verification metadata.' }
        Assert-PrismPin $records[0]
        foreach ($required in @($records[0].requiredProjectIDs)) {
            if ($required -notin $profiles.playable.projectIDs) { throw 'Missing required dependency project.' }
        }
        $dependencies += $records[0]
    }
    $names = @{}
    foreach ($dep in $dependencies) {
        if ($names.ContainsKey($dep.fileName)) { throw 'Conflicting dependency jar names.' }
        $names[$dep.fileName] = $true
    }
    $expectedNames = @{
        419699 = "architectury-$($props.architectury_api_version)-forge.jar"
        238222 = "jei-$($props.minecraft_version)-forge-$($props.jei_version).jar"
        845831 = "Heracles-forge-$($props.minecraft_version)-$($props.heracles_version).jar"
        570073 = "resourcefullib-forge-$($props.minecraft_version)-$($props.resourcefullib_version).jar"
    }
    foreach ($id in $expectedNames.Keys) {
        if (@($dependencies | Where-Object { $_.projectID -eq $id })[0].fileName -cne $expectedNames[$id]) {
            throw 'Dependency version mismatch against gradle.properties.'
        }
    }
    if (@($dependencies | Where-Object { $_.projectID -eq 845831 })[0].fileID.ToString() -cne $props.heracles_curse_file -or
        @($dependencies | Where-Object { $_.projectID -eq 570073 })[0].fileID.ToString() -cne $props.resourcefullib_curse_file) {
        throw 'Quest file pin mismatch against gradle.properties.'
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

        function Get-PrismOwnedState([string]$Target, [string]$InstanceId) {
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
                @($script:PrismDevProjects | Sort-Object) 'Installed seven-project selection'
            $mods = Join-Path $Target 'minecraft/mods'
            $null = Get-PrismPath $mods 'Directory'
            Assert-PrismModIndex $mods
            $records = @($receipt.dependencies) + @($receipt.grindless)
            $actual = @(Get-ChildItem -LiteralPath $mods -Force | Where-Object { $_.Name -ine '.index' })
            if ($records.Count -ne 8 -or $actual.Count -ne 8) { throw 'Unknown, missing or tampered managed mods.' }
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
                    $record | Add-Member -NotePropertyName sha256 -NotePropertyValue (Get-PrismHash $destination)
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
                [ValidateSet('Prepare', 'Deploy', 'Inspect', 'Build', 'BuildAndDeploy')][string]$Action = 'Inspect',
                [string]$Root,
                [string]$InstancesRoot, [string]$InstanceId = 'Grindless-T0-Dev', [string]$TemplateInstance,
                [string]$JavaPath, [string]$JavaSha256, [string]$PackZip, [string]$ExpectedRevision,
                [string]$RuntimeRevision, [string[]]$DependencyDirectory = @()
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
            if (Test-Path -LiteralPath $target) { $state = Get-PrismOwnedState $target $InstanceId }
            if ($Action -eq 'Inspect') {
                if ($null -eq $state) { throw 'The dedicated engineering instance has not been deployed.' }
                return [pscustomobject]@{ Action = 'Inspect'; InstanceId = $InstanceId; Outcome = 'VerifiedStopped'
                    SourceRevision = $state.Receipt.sourceRevision; RuntimeSourceRevision = $state.Receipt.runtimeSourceRevision
                    GrindlessSha256 = $state.Receipt.grindless.sha256; PackSha256 = $state.Receipt.packSha256
                    PackStatus = $state.Receipt.packStatus }
            }
            if ($ExpectedRevision) { Assert-PrismRevision $ExpectedRevision }
            $source = Assert-PrismSource $Root $ExpectedRevision
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
                    $copy.PSObject.Properties.Remove('sha256')
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
                MissingLocalDependencies = $missing }
        }

if ($MyInvocation.InvocationName -ne '.') { Invoke-PrismDev @PSBoundParameters }
