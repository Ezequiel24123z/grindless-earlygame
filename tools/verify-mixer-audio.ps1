# Offline source/resource contract, not Java execution, codec QC or a listening pass.
# Negative controls mutate only in-memory snapshots; no provider, workspace writes or JVM.
param(
    [string]$Root = (Join-Path $PSScriptRoot '..'),
    [switch]$SelfTest,
    [switch]$SourceOnly
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$baseline = 'cbd318abf1a7533df575764000384543a2dec15c'
$source = 'common/src/main/java/io/github/ezequiel24123z/grindless'
$assets = 'common/src/main/resources/assets/grindless'
$generator = 'tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/GenerateAssets.java'
$quernHash = '811b29471e3fa6a51ce76db2241259d02adfc64e244eadc096ef376d1d1970d5'
$mixerHash = '2c052cd678775f237fbf1a939037006ed26d05588e99a4ff29bbffcd66599f45'
$hashes = [ordered]@{
    machine_hum = '1a4444847bcd7b65fa245688b40538e713c857bc0595f7d86dab4e84d1bbe194'
    machine_hum_heavy = '3614968863fb10953acf429ac990ab92c3a1d196d23ec1e289f85714da7d98f3'
    pylon_link = '07e5624bfb2eaced8df7219419de6ad214967842db9af16f3f4a050f9ddccd24'
    relay_click = 'c5446171b5d6194057b2cfc860ffd1d5bf25a6b99004b7cda8c04e85d83a786e'
    brownout_alarm = 'c6222b817b90ca76c8b95e53df000c8d6cb7b4ddfa21117cd59b09767c2fffc5'
    t0_grinding = $quernHash
    t0_wet_work = $mixerHash
}

function Read-Source([string]$Path) {
    return [IO.File]::ReadAllText((Join-Path $Root $Path), [Text.Encoding]::UTF8)
}

function Invoke-LocalGit([string]$Arguments) {
    $process = New-Object Diagnostics.Process
    $process.StartInfo.FileName = 'git'
    $process.StartInfo.WorkingDirectory = $Root
    $process.StartInfo.Arguments = "--no-pager $Arguments"
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.CreateNoWindow = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $process.StartInfo.StandardOutputEncoding = [Text.Encoding]::UTF8
    try {
        if (-not $process.Start()) { throw 'Could not start local Git.' }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $text = $stdout.GetAwaiter().GetResult()
        $diagnostic = $stderr.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw "Local Git failed: $diagnostic" }
        return $text
    } finally { $process.Dispose() }
}

function Normalize-Lines([string]$Text) { return $Text.Replace("`r`n", "`n") }
function Code([string]$Text) {
    return ([regex]::Replace($Text, '(?s)/\*.*?\*/|//[^\r\n]*|"(?:\\.|[^"\\])*"|''(?:\\.|[^''\\])*''', ' ') -replace '\s+', ' ').Trim()
}

# The native files have no text blocks; balance executable braces after removing trivia.
function Code-Block([string]$Text, [string]$Marker) {
    $code = Code $Text
    $start = $code.IndexOf($Marker, [StringComparison]::Ordinal)
    if ($start -lt 0) { return '' }
    $open = $code.IndexOf('{', $start)
    if ($open -lt 0) { return '' }
    $depth = 0
    for ($i = $open; $i -lt $code.Length; $i++) {
        if ($code[$i] -eq '{') { $depth++ }
        if ($code[$i] -eq '}') {
            $depth--
            if ($depth -eq 0) { return $code.Substring($start, $i - $start + 1) }
        }
    }
    return ''
}

function Bytes-Hash([byte[]]$Bytes) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}

# Container/header parsing only: complete pages, one serial, BOS/EOS and Vorbis identification.
# Exact hashes bind the approved payload; this deliberately does not decode or claim audibility.
function Ogg-Info([byte[]]$Bytes) {
    $offset = 0; $pages = 0; $serial = 0; $eos = $false; $channels = 0; $rate = 0; $samples = 0L
    while ($offset -lt $Bytes.Length) {
        if ($eos -or $offset + 27 -gt $Bytes.Length -or
            [Text.Encoding]::ASCII.GetString($Bytes, $offset, 4) -cne 'OggS' -or $Bytes[$offset + 4] -ne 0) { return $null }
        $flags = $Bytes[$offset + 5]
        $segments = $Bytes[$offset + 26]
        $data = $offset + 27 + $segments
        if ($data -gt $Bytes.Length) { return $null }
        $payload = 0
        for ($i = 0; $i -lt $segments; $i++) { $payload += $Bytes[$offset + 27 + $i] }
        if ($data + $payload -gt $Bytes.Length) { return $null }
        $pageSerial = [BitConverter]::ToUInt32($Bytes, $offset + 14)
        if ([BitConverter]::ToUInt32($Bytes, $offset + 18) -ne $pages) { return $null }
        if ($pages -eq 0) {
            if ($flags -ne 2 -or $segments -ne 1 -or $payload -ne 30 -or $Bytes[$data] -ne 1 -or
                [Text.Encoding]::ASCII.GetString($Bytes, $data + 1, 6) -cne 'vorbis' -or
                [BitConverter]::ToUInt32($Bytes, $data + 7) -ne 0 -or ($Bytes[$data + 29] -band 1) -ne 1) { return $null }
            $serial = $pageSerial
            $channels = $Bytes[$data + 11]
            $rate = [BitConverter]::ToUInt32($Bytes, $data + 12)
        } elseif ($pageSerial -ne $serial -or ($flags -band 2) -ne 0) { return $null }
        $eos = ($flags -band 4) -ne 0
        $samples = [BitConverter]::ToInt64($Bytes, $offset + 6)
        $offset = $data + $payload
        $pages++
    }
    if (-not $eos -or $pages -lt 2 -or $samples -le 0 -or $rate -eq 0) { return $null }
    return [pscustomobject]@{ Channels = $channels; Rate = $rate; Pages = $pages; Samples = $samples }
}

$paths = [ordered]@{
    Audio = "$source/client/audio/T0MachineAudio.java"
    Client = "$source/client/GrindlessClient.java"
    Effects = "$source/machine/MachineEffects.java"
    Tracker = "$source/client/audio/QuernLoopTracker.java"
    Machine = "$source/machine/ProcessMachineBlockEntity.java"
    Registry = "$source/registry/ModSounds.java"
    Sounds = "$assets/sounds.json"
    Lang = "$assets/lang/en_us.json"
    Generator = $generator
    Shell = 'tools/convert-audio.sh'
    PowerShell = 'tools/convert-audio.ps1'
    Import = 'docs/assets/mixer-runtime-import.json'
}
$inputFiles = @{}
foreach ($entry in $paths.GetEnumerator()) { $inputFiles[$entry.Key] = Read-Source $entry.Value }
$oldAudio = Invoke-LocalGit "show ${baseline}:$source/client/audio/QuernAudio.java"
$oldEffects = Invoke-LocalGit "show ${baseline}:$source/machine/MachineEffects.java"
$oldTracker = Invoke-LocalGit "show ${baseline}:$source/client/audio/QuernLoopTracker.java"
$oldClient = Invoke-LocalGit "show ${baseline}:$source/client/GrindlessClient.java"
$oldSounds = (Invoke-LocalGit "show ${baseline}:$assets/sounds.json") | ConvertFrom-Json
$oldLang = (Invoke-LocalGit "show ${baseline}:$assets/lang/en_us.json") | ConvertFrom-Json
$oldGenerator = Invoke-LocalGit "show ${baseline}:$generator"
$inputFiles.Media = @{}
if (-not $SourceOnly) {
    foreach ($name in $hashes.Keys) {
        $path = Join-Path $Root "$assets/sounds/$name.ogg"
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "NOT_RUN media: missing parent-supplied resource $name.ogg" }
        $inputFiles.Media[$name] = [IO.File]::ReadAllBytes($path)
    }
    $inputFiles.Notice = Read-Source "$assets/AUDIO-NOTICE.txt"
    $inputFiles.Provenance = Read-Source 'THIRD_PARTY_NOTICES.md'
}

function Get-Contracts([hashtable]$Files, [bool]$Media) {
    $audio = Code $Files.Audio
    $effects = Code $Files.Effects
    $c = [ordered]@{}
    $c['generic tracker is unchanged from the actual reviewed source'] =
        (Normalize-Lines $Files.Tracker) -ceq (Normalize-Lines $oldTracker)
    $c['one tracker and one shared block-entity scan own both families'] =
        [regex]::Matches($audio, 'new QuernLoopTracker<>').Count -eq 1 -and
        [regex]::Matches($audio, 'chunk\.getBlockEntities\(\)\.values\(\)').Count -eq 1 -and
        $audio.Contains('eligible.put(machine.getBlockPos().asLong(), machine)')
    $tick = (Code-Block $Files.Audio 'private void tick(Minecraft client)').Replace(
        'eligible(level, chunk, machine, listener, familyFor(machine.kind()))', 'eligible(level, chunk, machine, listener)')
    $c['bounded loaded-only discovery, pause, overlay, mute and scope dispatch are unchanged'] =
        $tick -ceq (Code-Block $oldAudio 'private void tick(Minecraft client)')
    $c['renamed client registration replaces the old binding exactly once'] =
        -not (Test-Path -LiteralPath (Join-Path $Root "$source/client/audio/QuernAudio.java")) -and
        (Code ($Files.Client.Replace('T0MachineAudio', 'QuernAudio'))) -ceq (Code $oldClient) -and
        [regex]::Matches((Code $Files.Client), 'T0MachineAudio\.register\(\)').Count -eq 1
    $c['disconnect, respawn, shutdown and reload use the original cleanup'] =
        (Code-Block $Files.Audio 'public static void register()').Replace('T0MachineAudio', 'QuernAudio') -ceq
        (Code-Block $oldAudio 'public static void register()')
    $c['only Quern and Clay Mixer select a family'] = (Code-Block $Files.Audio 'private static Family familyFor(') -ceq
        'private static Family familyFor(ProcessMachineKind kind) { return switch (kind) { case QUERN -> Family.GRINDING; case CLAY_MIXER -> Family.WET_WORK; default -> null; }; }'
    $c['family events preserve Quern gain one and set only Mixer gain 0.35'] =
        $audio.Contains('private enum Family { GRINDING(ModSounds.T0_GRINDING, 1.0F), WET_WORK(ModSounds.T0_WET_WORK, 0.35F);') -and
        $audio.Contains('super(family.event.get(), SoundSource.BLOCKS,') -and $audio.Contains('volume = family.volume;') -and
        $audio.Contains('pitch = 1.0F;') -and $audio.Contains('looping = true;') -and $audio.Contains('delay = 0;') -and
        $audio.Contains('relative = false;') -and $audio.Contains('attenuation = SoundInstance.Attenuation.LINEAR;')
    $eligibility = 'private static boolean eligible(ClientLevel level, LevelChunk chunk, ProcessMachineBlockEntity machine, Vec3 listener, Family family) { BlockPos pos = machine.getBlockPos(); if (family == null || familyFor(machine.kind()) != family || machine.isRemoved() || machine.getLevel() != level || chunk.getBlockEntities().get(pos) != machine || !QuernLoopTracker.inRange(pos.getX() + 0.5 - listener.x, pos.getY() + 0.5 - listener.y, pos.getZ() + 0.5 - listener.z)) { return false; } var state = chunk.getBlockState(pos); return state.getBlock() instanceof ProcessMachineBlock block && block.kind() == machine.kind() && familyFor(block.kind()) == family && state.getValue(ProcessMachineBlock.STATUS) == MachineStatus.RUNNING; }'
    $c['eligibility requires matching current source identity, kind, family, world, RUNNING and range'] =
        (Code-Block $Files.Audio 'private static boolean eligible(') -ceq $eligibility
    $canPlay = (Code-Block $Files.Audio 'public boolean canPlaySound()').Replace(
        'client.gameRenderer.getMainCamera().getPosition(), family)', 'client.gameRenderer.getMainCamera().getPosition())')
    $c['native admission rechecks the captured family without retaining a world'] =
        $audio.Contains('private final WeakReference<ProcessMachineBlockEntity> source;') -and
        $audio.Contains('private final Family family;') -and $audio.Contains('this.family = family;') -and
        $audio.Contains('new MachineSound(position, source, familyFor(source.kind()))') -and
        $audio.Contains('client.gameRenderer.getMainCamera().getPosition(), family)') -and
        (Code $Files.Machine).Contains('private final ProcessMachineKind kind;') -and
        $canPlay -ceq (Code-Block $oldAudio 'public boolean canPlaySound()')
    $c['retirement stops the native object before SoundManager release'] =
        $audio.Contains('sound.retire(); Minecraft.getInstance().getSoundManager().stop(sound);') -and
        (Code-Block $Files.Audio 'public void tick()') -ceq (Code-Block $oldAudio 'public void tick()') -and
        (Code-Block $Files.Audio 'private void retire()') -ceq (Code-Block $oldAudio 'private void retire()')
    $c['only Mixer opts out of washer hum; Washer and Settler keep the true default'] =
        $effects.Contains('case MIXER -> washer(status, level, pos, random, false);') -and
        $effects.Contains('case SETTLER -> washer(status, level, pos, random);') -and
        $effects.Contains('case WASHER -> washer(status, level, pos, random);') -and
        $effects.Contains('washer(status, level, pos, random, true);') -and
        [regex]::Matches($effects, 'washer\(status, level, pos, random, false\)').Count -eq 1
    $projection = $effects.Replace('case MIXER -> washer(status, level, pos, random, false);',
        'case MIXER -> washer(status, level, pos, random);').Replace(
        'private static void washer(MachineStatus status, Level level, BlockPos pos, RandomSource random) { washer(status, level, pos, random, true); } private static void washer(MachineStatus status, Level level, BlockPos pos, RandomSource random, boolean hum)',
        'private static void washer(MachineStatus status, Level level, BlockPos pos, RandomSource random)').Replace(
        'if (hum) { sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.15F, 1.2F); }',
        'sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.15F, 1.2F);')
    $c['all particles, fault silence, Quern, TANK and unrelated effects match baseline'] = $projection -ceq (Code $oldEffects)
    $c['passive TANK retains one independent empty arm'] =
        [regex]::Matches($effects, '\bcase TANK -> \{ }').Count -eq 1
    $events = $Files.Sounds | ConvertFrom-Json
    $lang = $Files.Lang | ConvertFrom-Json
    $registered = @([regex]::Matches($Files.Registry, 'sound\("([a-z0-9_]+)"\)') | ForEach-Object { $_.Groups[1].Value })
    $c['registered and resource events are exactly the original six plus wet work'] =
        ($registered | Sort-Object) -join ',' -ceq (($hashes.Keys | Sort-Object) -join ',') -and
        (($events.PSObject.Properties.Name | Sort-Object) -join ',') -ceq (($hashes.Keys | Sort-Object) -join ',')
    $wet = $events.t0_wet_work
    $c['Mixer binds one exact non-streamed positional event at range sixteen'] =
        @($wet.sounds).Count -eq 1 -and $wet.sounds[0].name -ceq 'grindless:t0_wet_work' -and
        $wet.sounds[0].stream -is [bool] -and $wet.sounds[0].stream -eq $false -and
        $wet.sounds[0].attenuation_distance -eq 16 -and $wet.subtitle -ceq 'subtitles.grindless.t0_wet_work' -and
        (($wet.PSObject.Properties.Name | Sort-Object) -join ',') -ceq 'sounds,subtitle' -and
        (($wet.sounds[0].PSObject.Properties.Name | Sort-Object) -join ',') -ceq 'attenuation_distance,name,stream'
    $preserved = $true
    foreach ($property in $oldSounds.PSObject.Properties) {
        $preserved = $preserved -and (ConvertTo-Json $events.($property.Name) -Depth 5 -Compress) -ceq
            (ConvertTo-Json $property.Value -Depth 5 -Compress)
    }
    $c['Quern and all five legacy event definitions remain unchanged'] = $preserved
    $preserved = @($lang.PSObject.Properties).Count -eq (@($oldLang.PSObject.Properties).Count + 1)
    foreach ($property in $oldLang.PSObject.Properties) { $preserved = $preserved -and $lang.($property.Name) -ceq $property.Value }
    $c['only the new Mixer subtitle changes localization'] = $preserved -and
        $lang.'subtitles.grindless.t0_wet_work' -ceq 'Clay mixer stirs'
    $audioBlock = '(?s)// ---- sounds ----.*?(?=\s*writeProvenance\(root, written\);)'
    $c['asset generation preserves the original five synthetic writers exactly'] =
        [regex]::Match((Normalize-Lines $Files.Generator), $audioBlock).Value -ceq
        [regex]::Match((Normalize-Lines $oldGenerator), $audioBlock).Value
    $c['generator preserves both imported beds, events and public notices'] =
        $Files.Generator.Contains('`sounds/t0_wet_work.ogg`') -and
        $Files.Generator.Contains('Quern A import remains byte-identical') -and
        $Files.Generator.Contains('Generation does not write imported Ogg files, `sounds.json`, `AUDIO-NOTICE.txt`') -and
        -not $Files.Generator.Contains('deleteTree(sounds)') -and -not $Files.Generator.Contains('resourceDir(root, "sounds")')
    $c['paired converters remain closed to the five synthetic names'] =
        $Files.Shell.Contains('for name in machine_hum machine_hum_heavy pylon_link relay_click brownout_alarm; do') -and
        -not $Files.Shell.Contains('"$SRC"/*.wav') -and
        $Files.PowerShell.Contains("`$names = @('machine_hum', 'machine_hum_heavy', 'pylon_link', 'relay_click', 'brownout_alarm')") -and
        -not $Files.PowerShell.Contains('Get-ChildItem')
    if ($Media) {
        foreach ($name in $hashes.Keys) {
            $bytes = $Files.Media[$name]
            $info = Ogg-Info $bytes
            $c["$name has its exact approved bytes"] = (Bytes-Hash $bytes) -ceq $hashes[$name] -and
                ($name -ne 't0_grinding' -or $bytes.Length -eq 34364) -and ($name -ne 't0_wet_work' -or $bytes.Length -eq 33374)
            $c["$name is a complete mono 44100 Vorbis container"] = $null -ne $info -and $info.Channels -eq 1 -and $info.Rate -eq 44100
        }
        $info = Ogg-Info $Files.Media.t0_wet_work
        $c['Mixer container has the selected three-second sample extent'] = $null -ne $info -and $info.Samples -eq 132300
        $mixerNoticeStart = $Files.Notice.IndexOf('Grindless Clay Mixer wet-work audio', [StringComparison]::Ordinal)
        $mixerNotice = ''
        if ($mixerNoticeStart -ge 0) { $mixerNotice = $Files.Notice.Substring($mixerNoticeStart) -replace '\s+', ' ' }
        $c['both public notices bind the final Mixer hash and game-incorporation provenance'] =
            $mixerNotice.Contains('t0_wet_work.ogg') -and $Files.Provenance.Contains('t0_wet_work.ogg') -and
            $mixerNotice.Contains($mixerHash) -and $Files.Provenance.Contains($mixerHash) -and
            $mixerNotice.Contains('eleven_text_to_sound_v2') -and $Files.Provenance.Contains('eleven_text_to_sound_v2') -and
            $mixerNotice.Contains('https://elevenlabs.io/creative-terms') -and $Files.Provenance.Contains('https://elevenlabs.io/creative-terms') -and
            $mixerNotice.Contains('not a standalone sample or sound library')
        $record = $Files.Import | ConvertFrom-Json
        $r = $record.resource; $mix = $record.runtime_contract
        $c['public import metadata agrees with the selected resource, rights and native settings'] =
            $r.sha256 -ceq $mixerHash -and $r.bytes -eq 33374 -and
            $r.event -ceq 'grindless:t0_wet_work' -and $r.path -ceq "$assets/sounds/t0_wet_work.ogg" -and
            $r.imported -eq $true -and $r.import_authorized -eq $true -and $r.notice_recorded_before_import -eq $true -and
            $r.cc0_or_mit_relicensing -eq $false -and $r.standalone_audio_distribution -ceq 'not_permitted' -and
            $record.selection.selected_candidate -ceq 'wet-work-a' -and
            $record.normalization.source_preview -ceq 'wet-work-a.preview.wav' -and
            $mix.volume -eq 0.35 -and $mix.pitch -eq 1 -and $mix.radius_blocks -eq 16 -and
            $mix.category -ceq 'BLOCKS' -and $mix.attenuation -ceq 'LINEAR' -and $mix.ordinary_error_sounds -eq $false
    }
    return $c
}

$contracts = Get-Contracts $inputFiles (-not $SourceOnly)
$allowed = @("$source/client/GrindlessClient.java", "$source/client/audio/QuernAudio.java",
    "$source/client/audio/T0MachineAudio.java", "$source/machine/MachineEffects.java", "$source/registry/ModSounds.java")
$changed = (Invoke-LocalGit "diff --name-only $baseline -- common/src/main/java forge/src/main/java") -split '\r?\n'
$untracked = (Invoke-LocalGit 'ls-files --others --exclude-standard -- common/src/main/java forge/src/main/java') -split '\r?\n'
$outside = @(@($changed) + @($untracked) | Where-Object { $_ -and $_ -cnotin $allowed })
$contracts['no other application Java, gameplay, transfer or save-state sources change'] = $outside.Count -eq 0
$failed = @($contracts.GetEnumerator() | Where-Object { -not $_.Value })
foreach ($failure in $failed) { Write-Output "FAIL $($failure.Key)" }
Write-Output "$($contracts.Count) MIXER AUDIO SOURCE/RESOURCE CHECKS, $($failed.Count) FAILED"

$script:controls = 0
$script:controlFailures = 0
function Assert-Rejected([string]$Name, [hashtable]$Mutated, [string]$Diagnostic) {
    $script:controls++
    if ($contracts[$Diagnostic] -ne $true) {
        $script:controlFailures++
        Write-Output "FAIL negative control: $Name lacks a green baseline for $Diagnostic"
        return
    }
    $observed = Get-Contracts $Mutated (-not $SourceOnly)
    if ($observed[$Diagnostic] -eq $false) { Write-Output "PASS negative control: $Name -> $Diagnostic" }
    else { $script:controlFailures++; Write-Output "FAIL negative control: $Name -> $Diagnostic" }
}

if ($SelfTest) {
    $mutations = @(
        @('Settler family leak', 'Audio', 'case CLAY_MIXER -> Family.WET_WORK;', 'case SETTLER -> Family.WET_WORK; // case CLAY_MIXER -> Family.WET_WORK;', 'only Quern and Clay Mixer select a family'),
        @('stale block kind admitted', 'Audio', 'block.kind() == machine.kind()', 'true', 'eligibility requires matching current source identity, kind, family, world, RUNNING and range'),
        @('stale block entity admitted', 'Audio', 'chunk.getBlockEntities().get(pos) != machine', 'false', 'eligibility requires matching current source identity, kind, family, world, RUNNING and range'),
        @('captured family bypassed', 'Audio', 'familyFor(machine.kind()) != family', 'false', 'eligibility requires matching current source identity, kind, family, world, RUNNING and range'),
        @('discovery force-loads a chunk', 'Audio', 'getChunk(x, z, ChunkStatus.FULL, false)', 'getChunk(x, z, ChunkStatus.FULL, true)', 'bounded loaded-only discovery, pause, overlay, mute and scope dispatch are unchanged'),
        @('Quern gain regressed', 'Audio', 'GRINDING(ModSounds.T0_GRINDING, 1.0F)', 'GRINDING(ModSounds.T0_GRINDING, 0.35F)', 'family events preserve Quern gain one and set only Mixer gain 0.35'),
        @('Mixer hum restored with comment decoy', 'Effects', 'case MIXER -> washer(status, level, pos, random, false);', 'case MIXER -> washer(status, level, pos, random); // case MIXER -> washer(status, level, pos, random, false);', 'only Mixer opts out of washer hum; Washer and Settler keep the true default'),
        @('passive TANK generator regression', 'Effects', 'case TANK -> { }', 'case TANK -> generator(status, level, pos, random);', 'passive TANK retains one independent empty arm'),
        @('Mixer particles changed', 'Effects', 'ParticleTypes.SPLASH, x, pos.getY() + 0.85', 'ParticleTypes.SMOKE, x, pos.getY() + 0.85', 'all particles, fault silence, Quern, TANK and unrelated effects match baseline'),
        @('generic recovery changed', 'Tracker', 'RETRY_TICKS = 40;', 'RETRY_TICKS = 1;', 'generic tracker is unchanged from the actual reviewed source'),
        @('generator writes imported wet bed', 'Generator', '"brownout_alarm.wav"', '"t0_wet_work.wav"', 'asset generation preserves the original five synthetic writers exactly')
    )
    foreach ($mutation in $mutations) {
        if (-not $inputFiles[$mutation[1]].Contains($mutation[2])) { throw "Negative control no longer matches: $($mutation[0])" }
        $copy = $inputFiles.Clone()
        $copy[$mutation[1]] = $copy[$mutation[1]].Replace($mutation[2], $mutation[3])
        Assert-Rejected $mutation[0] $copy $mutation[4]
    }
    $copy = $inputFiles.Clone()
    $event = $copy.Sounds | ConvertFrom-Json
    $event.t0_wet_work.sounds[0].stream = $true
    $copy.Sounds = ConvertTo-Json $event -Depth 10
    Assert-Rejected 'streaming Mixer event' $copy 'Mixer binds one exact non-streamed positional event at range sixteen'
    $copy = $inputFiles.Clone()
    $copy.Lang = $copy.Lang.Replace('"Clay mixer stirs"', '"Machine error"')
    Assert-Rejected 'error-palette subtitle' $copy 'only the new Mixer subtitle changes localization'
    if (-not $SourceOnly) {
        $copy = $inputFiles.Clone(); $copy.Media = $copy.Media.Clone()
        $copy.Media.t0_wet_work = $copy.Media.t0_wet_work.Clone()
        $copy.Media.t0_wet_work[$copy.Media.t0_wet_work.Length - 1] = $copy.Media.t0_wet_work[-1] -bxor 1
        Assert-Rejected 'changed final Mixer payload' $copy 't0_wet_work has its exact approved bytes'
        $copy = $inputFiles.Clone(); $copy.Media = $copy.Media.Clone()
        $copy.Media.t0_wet_work = $copy.Media.t0_wet_work.Clone()
        $channelOffset = 27 + $copy.Media.t0_wet_work[26] + 11
        $copy.Media.t0_wet_work[$channelOffset] = 2
        Assert-Rejected 'stereo Mixer header' $copy 't0_wet_work is a complete mono 44100 Vorbis container'
        $copy = $inputFiles.Clone(); $copy.Media = $copy.Media.Clone()
        $copy.Media.t0_wet_work = [byte[]]$copy.Media.t0_wet_work[0..($copy.Media.t0_wet_work.Length - 2)]
        Assert-Rejected 'truncated final Ogg page' $copy 't0_wet_work is a complete mono 44100 Vorbis container'
        $copy = $inputFiles.Clone()
        $copy.Notice = $copy.Notice.Replace($mixerHash, ('0' * 64))
        Assert-Rejected 'notice no longer binds imported Mixer' $copy 'both public notices bind the final Mixer hash and game-incorporation provenance'
        $copy = $inputFiles.Clone()
        $copy.Import = $copy.Import.Replace('"import_authorized": true', '"import_authorized": false')
        Assert-Rejected 'metadata import authorization missing' $copy 'public import metadata agrees with the selected resource, rights and native settings'
    }
    Write-Output "$script:controls NEGATIVE CONTROLS, $script:controlFailures FAILED"
}
Write-Output "BASELINE $baseline"
foreach ($path in @($paths.Values) + @('tools/checks/VerifyQuernAudio.java', 'tools/checks/VerifyAssets.java',
        'tools/verify-mixer-audio.ps1', 'tools/verify-mixer-audio.sh')) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $Root $path) -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Output "SHA256 $path $hash"
}
if ($SourceOnly) { Write-Output 'NOT_RUN: imported media/container/public-provenance checks (--SourceOnly).' }
else {
    foreach ($name in $hashes.Keys) { Write-Output "OGG $name bytes=$($inputFiles.Media[$name].Length) SHA256=$(Bytes-Hash $inputFiles.Media[$name])" }
}
Write-Output 'STATIC SOURCE/CONTAINER PROOF ONLY; Java compilation/execution, native playback and human listening NOT_RUN.'
if ($failed.Count -ne 0 -or $script:controlFailures -ne 0) { exit 1 }
