# Offline physical-sound source/resource contracts. No JVM, provider, credential or listening claim.
param(
    [string]$Root = (Join-Path $PSScriptRoot '..'),
    [switch]$SelfTest,
    [switch]$SourceOnly
)
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$baseline = 'b8bd5e885aef598bf888d7e390b297a34cbb7d52'
$source = 'common/src/main/java/io/github/ezequiel24123z/grindless'
$assets = 'common/src/main/resources/assets/grindless'
$hashes = [ordered]@{
    t0_grinding = '811b29471e3fa6a51ce76db2241259d02adfc64e244eadc096ef376d1d1970d5'
    t0_wet_work = '2c052cd678775f237fbf1a939037006ed26d05588e99a4ff29bbffcd66599f45'
    t0_water_flow = '5fbfa5c1906fcc98ac310f3feaa9959fd7838ef5f4f6143dc293d37eadc6aeb1'
    t0_mineral_extraction = 'a634f9a17c5f5c71e7fd20da8f79ec5453715d125b1084c557de54bb380ba9fb'
}
$radii = @{ t0_grinding = 16; t0_wet_work = 16; t0_water_flow = 10; t0_mineral_extraction = 12 }
$sizes = @{ t0_grinding = 34364; t0_wet_work = 33374; t0_water_flow = 33490; t0_mineral_extraction = 29241 }
$native = [ordered]@{
    pylon_link = 'minecraft:block.tripwire.attach'
    relay_click = 'minecraft:block.lever.click'
    brownout_alarm = 'minecraft:block.iron_trapdoor.close'
    t0_fire_draft = 'minecraft:block.furnace.fire_crackle'
}
$retired = @('machine_hum','machine_hum_heavy','pylon_link','relay_click','brownout_alarm')
function Read-Source([string]$Path) { [IO.File]::ReadAllText((Join-Path $Root $Path), [Text.Encoding]::UTF8) }
function Normalize-Lines([string]$Text) { $Text.Replace("`r`n", "`n") }
function Code([string]$Text) {
    ([regex]::Replace($Text, '(?s)/\*.*?\*/|//[^\r\n]*|"(?:\\.|[^"\\])*"|''(?:\\.|[^''\\])*''', ' ') -replace '\s+', ' ').Trim()
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
    try {
        if (-not $process.Start()) { throw 'Could not start local Git.' }
        $stdout = $process.StandardOutput.ReadToEndAsync(); $stderr = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw ('Local Git failed: ' + $stderr.GetAwaiter().GetResult()) }
        $stdout.GetAwaiter().GetResult()
    } finally { $process.Dispose() }
}
function Bytes-Hash([byte[]]$Bytes) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { ([BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
function Particles([string]$Text) {
    ([regex]::Matches((Code $Text), 'particle\(level, ParticleTypes\.[^;]+;') |
        ForEach-Object { $_.Value }) -join "`n"
}
# Same complete-page Vorbis container check as the prior Mixer contract; no decoder/audibility claim.
function Ogg-Info([byte[]]$Bytes) {
    $offset = 0; $pages = 0; $serial = 0; $eos = $false; $channels = 0; $rate = 0; $samples = 0L
    while ($offset -lt $Bytes.Length) {
        if ($eos -or $offset + 27 -gt $Bytes.Length -or
            [Text.Encoding]::ASCII.GetString($Bytes, $offset, 4) -cne 'OggS' -or $Bytes[$offset + 4] -ne 0) { return $null }
        $flags = $Bytes[$offset + 5]; $segments = $Bytes[$offset + 26]; $data = $offset + 27 + $segments
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
            $serial = $pageSerial; $channels = $Bytes[$data + 11]; $rate = [BitConverter]::ToUInt32($Bytes, $data + 12)
        } elseif ($pageSerial -ne $serial -or ($flags -band 2) -ne 0) { return $null }
        $eos = ($flags -band 4) -ne 0; $samples = [BitConverter]::ToInt64($Bytes, $offset + 6)
        $offset = $data + $payload; $pages++
    }
    if (-not $eos -or $pages -lt 2 -or $samples -le 0 -or $rate -eq 0) { return $null }
    [pscustomobject]@{ Channels = $channels; Rate = $rate; Samples = $samples }
}
$paths = [ordered]@{
    Audio = "$source/client/audio/T0MachineAudio.java"
    Policy = "$source/client/audio/T0SoundscapePolicy.java"
    Tracker = "$source/client/audio/QuernLoopTracker.java"
    Client = "$source/client/GrindlessClient.java"
    Effects = "$source/machine/MachineEffects.java"
    Registry = "$source/registry/ModSounds.java"
    Catalogue = "$source/registry/BlockCatalogue.java"
    Bootstrap = "$source/recipe/BootstrapRecipes.java"
    Sounds = "$assets/sounds.json"
    Lang = "$assets/lang/en_us.json"
    Notice = "$assets/AUDIO-NOTICE.txt"
    Provenance = 'THIRD_PARTY_NOTICES.md'
    Record = 'docs/assets/t0-experience-audio.json'
    Mixer = 'docs/assets/mixer-runtime-import.json'
    Generator = 'tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/GenerateAssets.java'
    Shell = 'tools/convert-audio.sh'
    PowerShell = 'tools/convert-audio.ps1'
}
$snapshot = @{}
foreach ($entry in $paths.GetEnumerator()) { $snapshot[$entry.Key] = Read-Source $entry.Value }
$oldTracker = Invoke-LocalGit "show ${baseline}:$source/client/audio/QuernLoopTracker.java"
$oldEffects = Invoke-LocalGit "show ${baseline}:$source/machine/MachineEffects.java"
$oldSounds = (Invoke-LocalGit "show ${baseline}:$assets/sounds.json") | ConvertFrom-Json
$oldLang = (Invoke-LocalGit "show ${baseline}:$assets/lang/en_us.json") | ConvertFrom-Json
$oldNotice = Invoke-LocalGit "show ${baseline}:$assets/AUDIO-NOTICE.txt"
$snapshot.Retired = @($retired | Where-Object { Test-Path -LiteralPath (Join-Path $Root "$assets/sounds/$_.ogg") })
$snapshot.Media = @{}
if (-not $SourceOnly) {
    foreach ($name in $hashes.Keys) { $snapshot.Media[$name] = [IO.File]::ReadAllBytes((Join-Path $Root "$assets/sounds/$name.ogg")) }
    $snapshot.MediaNames = @(Get-ChildItem -LiteralPath (Join-Path $Root "$assets/sounds") -File | ForEach-Object { $_.Name })
}
function Get-Contracts([hashtable]$Files, [bool]$Media) {
    $audio = Code $Files.Audio; $effects = Code $Files.Effects; $policy = Code $Files.Policy
    $events = $Files.Sounds | ConvertFrom-Json; $lang = $Files.Lang | ConvertFrom-Json
    $record = $Files.Record | ConvertFrom-Json
    $c = [ordered]@{}
    $c['generic tracker unchanged'] = (Normalize-Lines $Files.Tracker) -ceq (Normalize-Lines $oldTracker)
    $c['one loaded-only discovery and tracker'] =
        [regex]::Matches($audio, 'new QuernLoopTracker<>').Count -eq 1 -and
        [regex]::Matches($audio, 'chunk\.getBlockEntities\(\)\.values\(\)').Count -eq 1 -and
        $audio.Contains('getChunk(x, z, ChunkStatus.FULL, false)') -and
        $audio.Contains('QuernLoopTracker.minChunk(listener.x)') -and $audio.Contains('QuernLoopTracker.maxChunk(listener.z)')
    $c['current entity world block and running identity required'] =
        $audio.Contains('!machine.isRemoved() && machine.getLevel() == level') -and
        $audio.Contains('chunk.getBlockEntities().get(pos) == machine') -and
        $audio.Contains('block.kind() == machine.kind()') -and
        $audio.Contains('state.getValue(ProcessMachineBlock.STATUS) == MachineStatus.RUNNING') -and
        $audio.Contains('familyFor(machine) == family') -and
        $audio.Contains('runningBlock(machine, chunk.getBlockState(machine.getBlockPos()))') -and
        $audio.Contains('new MachineSound(position, source, familyFor(source))')
    $c['closed physical families exclude passive biology bench and calibration loops'] =
        $audio.Contains('case QUERN -> Family.GRINDING; case CLAY_MIXER -> Family.WET_WORK; case SETTLING_TROUGH, GEOLOGICAL_COLLECTOR -> Family.WATER_FLOW; default -> null;') -and
        $audio.Contains('source instanceof CrudeExtractorBlockEntity && state.is(ModBlocks.CRUDE_EXTRACTOR.get())') -and
        $audio.Contains('source instanceof HandPumpBlockEntity && state.is(ModBlocks.HAND_PUMP.get())') -and
        -not $audio.Contains('case BIOCULTIVATOR') -and -not $audio.Contains('case ASSEMBLY_BENCH')
    $c['Quern Mixer gain and native loop semantics preserved'] =
        $audio.Contains('GRINDING(ModSounds.T0_GRINDING, 1.0F), WET_WORK(ModSounds.T0_WET_WORK, 0.35F),') -and
        $audio.Contains('super(family.event.get(), SoundSource.BLOCKS,') -and
        $audio.Contains('looping = true;') -and $audio.Contains('delay = 0;') -and
        $audio.Contains('pitch = 1.0F;') -and $audio.Contains('attenuation = SoundInstance.Attenuation.LINEAR;')
    $c['physical ranges match discovery and native admission'] =
        $audio.Contains('WATER_FLOW(ModSounds.T0_WATER_FLOW, 0.25F, 10)') -and
        $audio.Contains('EXTRACTION(ModSounds.T0_MINERAL_EXTRACTION, 0.3F, 12)') -and
        $audio.Contains('< family.radius * family.radius')
    $c['nearest eight stable voices and four nonqueued onsets'] =
        $audio.Contains('T0SoundscapePolicy.nearest(eligible,') -and $audio.Contains('feedback.onsets(cranks).forEach') -and
        $policy.Contains('MAX_LOOPS = 8;') -and $policy.Contains('MAX_ONSETS = 4;') -and
        $policy.Contains('.thenComparingLong(Map.Entry::getKey)).limit(MAX_LOOPS)') -and
        $policy.Contains('previous == null || previous.get() != entry.getValue()') -and
        $policy.Contains('running.put(entry.getKey(), new WeakReference<>(entry.getValue()))')
    $c['observed crank edge is loaded current RUNNING not animateTick spam'] =
        $audio.Contains('blockEntity instanceof HandCrankDynamoBlockEntity') -and
        $audio.Contains('state.is(ModBlocks.HAND_CRANK_DYNAMO.get())') -and
        $audio.Contains('state.getValue(HandCrankDynamoBlock.STATUS) == MachineStatus.RUNNING') -and
        -not $effects.Contains('ModSounds.RELAY_CLICK')
    $c['scope pause mute resource cleanup and late retirement retained'] =
        $audio.Contains('WeakReference<BlockEntity> source') -and $audio.Contains('onsetWorld.get() != level') -and
        $audio.Contains('onsetPlayer.get() != client.player') -and $audio.Contains('feedback.clear();') -and
        $audio.Contains('loops.tick(level, client.player, Map.of(), paused, enabled)') -and
        $audio.Contains('client.isPaused() || client.getOverlay() != null') -and
        $audio.Contains('getSoundSourceVolume(SoundSource.MASTER) > 0.0F') -and
        $audio.Contains('getSoundSourceVolume(SoundSource.BLOCKS) > 0.0F') -and
        $audio.Contains('CLIENT_PLAYER_QUIT.register') -and $audio.Contains('CLIENT_PLAYER_RESPAWN.register') -and
        $audio.Contains('CLIENT_STOPPING.register') -and $audio.Contains('ResourceManagerReloadListener') -and
        $audio.Contains('sound.retire(); Minecraft.getInstance().getSoundManager().stop(sound);')
    $c['one client registration and no loader imports'] =
        [regex]::Matches((Code $Files.Client), 'T0MachineAudio\.register\(\)').Count -eq 1 -and
        -not $audio.Contains('net.minecraftforge') -and -not $audio.Contains('net.fabricmc')
    $c['all generic and pylon-running synth fallbacks removed'] =
        -not $effects.Contains('ModSounds.MACHINE_HUM') -and
        -not $effects.Contains('ModSounds.MACHINE_HUM_HEAVY') -and -not $effects.Contains('ModSounds.PYLON_LINK')
    $c['passive tank and structural parts remain silent'] =
        $effects.Contains('case TANK -> { }') -and $effects.Contains('case CHANNEL, SCREEN -> { }')
    $c['all original particle calls preserved'] = (Particles $Files.Effects) -ceq (Particles $oldEffects)
    $c['physical combustion only on actual T0 fuel roles'] =
        $effects.Contains('case BIOGEN -> combustion(status, level, pos, random);') -and
        $effects.Contains('case FIREBOX -> { generator(status, level, pos, random); combustion(status, level, pos, random); }') -and
        $effects.Contains('if (status == MachineStatus.RUNNING) { sound(level, pos, ModSounds.T0_FIRE_DRAFT.get(), random, 0.04F, 0.2F, 1.0F); }')
    $registered = @([regex]::Matches($Files.Registry, 'sound\("([a-z0-9_]+)"\)') | ForEach-Object { $_.Groups[1].Value })
    $expectedEvents = @($hashes.Keys) + @($native.Keys) + @('machine_hum','machine_hum_heavy')
    $c['ten registered events with seven baseline identities retained'] =
        ($registered | Sort-Object) -join ',' -ceq (($expectedEvents | Sort-Object) -join ',') -and
        ($events.PSObject.Properties.Name | Sort-Object) -join ',' -ceq (($expectedEvents | Sort-Object) -join ',')
    $c['both compatibility hum events intentionally silent'] = @($events.machine_hum.sounds).Count -eq 0 -and @($events.machine_hum_heavy.sounds).Count -eq 0
    foreach ($name in $native.Keys) {
        $binding = $events.$name.sounds
        $c["$name uses its exact native physical event"] =
            @($binding).Count -eq 1 -and $binding[0].type -ceq 'event' -and $binding[0].name -ceq $native[$name]
    }
    foreach ($name in $hashes.Keys) {
        $binding = $events.$name.sounds
        $c["$name binding is positional and nonstreamed"] = @($binding).Count -eq 1 -and
            $binding[0].name -ceq "grindless:$name" -and $binding[0].stream -is [bool] -and
            -not $binding[0].stream -and $binding[0].attenuation_distance -eq $radii[$name]
    }
    $c['Quern and Mixer event definitions unchanged'] =
        (ConvertTo-Json $events.t0_grinding -Depth 5 -Compress) -ceq (ConvertTo-Json $oldSounds.t0_grinding -Depth 5 -Compress) -and
        (ConvertTo-Json $events.t0_wet_work -Depth 5 -Compress) -ceq (ConvertTo-Json $oldSounds.t0_wet_work -Depth 5 -Compress)
    $preserved = @($lang.PSObject.Properties).Count -eq @($oldLang.PSObject.Properties).Count + 3
    foreach ($property in $oldLang.PSObject.Properties) { $preserved = $preserved -and $lang.($property.Name) -ceq $property.Value }
    $c['existing localization preserved with three new subtitles'] = $preserved -and
        $lang.'subtitles.grindless.t0_wet_work' -ceq 'Clay mixer stirs'
    $c['old public audio notice retained additively'] = (Normalize-Lines $Files.Notice).StartsWith((Normalize-Lines $oldNotice).TrimEnd())
    $c['all five retired synthetic resources absent'] = @($Files.Retired).Count -eq 0
    $c['converters reject synthetic runtime resurrection'] =
        $Files.PowerShell.Contains("throw 'Synthetic runtime conversion is retired.") -and
        $Files.Shell.Contains('exit 1') -and -not $Files.PowerShell.Contains('& ffmpeg') -and -not $Files.Shell.Contains('ffmpeg -')
    $c['generator preserves imports and limits WAVs to historical development evidence'] =
        $Files.Generator.Contains('Generation does not write imported Ogg files, `sounds.json`, `AUDIO-NOTICE.txt`') -and
        $Files.Generator.Contains('converters reject runtime synthesis') -and
        [regex]::Matches($Files.Generator, 'SoundSynth\.write\(new File\(sounds, "[a-z_]+\.wav"').Count -eq 5
    $catalogue = @([regex]::Matches($Files.Catalogue, 'new Entry\("([a-z_]+)"') | ForEach-Object { $_.Groups[1].Value })
    $implemented = @([regex]::Matches($Files.Bootstrap, 'new Shaped\("([a-z_]+)"') | ForEach-Object { $_.Groups[1].Value } |
        Where-Object { $catalogue -ccontains $_ }) + @('fibreboard')
    $matrixBindings = $true
    foreach ($entry in $record.machine_coverage) {
        if ($entry.family -ceq 'intentional_silence') {
            $matrixBindings = $matrixBindings -and $null -eq $entry.event -and $null -eq $entry.file -and $entry.playback -ceq 'none'
        } elseif ($entry.event -is [string] -and $entry.event.StartsWith('grindless:', [StringComparison]::Ordinal)) {
            $name = $entry.event.Substring(10)
            if ($entry.file) {
                $matrixBindings = $matrixBindings -and $hashes.Contains($name) -and $entry.file -ceq "$name.ogg" -and
                    $entry.radius_blocks -eq $radii[$name]
            } else {
                $matrixBindings = $matrixBindings -and $native.Contains($name) -and $entry.native_event -ceq $native[$name]
            }
        } else {
            $matrixBindings = $matrixBindings -and $entry.id -ceq 'fibreboard' -and $entry.playback -ceq 'inherited_interaction'
        }
    }
    $c['coverage exactly matches implemented T0 block crafts plus fibreboard'] =
        ($implemented | Sort-Object) -join ',' -ceq (($record.machine_coverage.id | Sort-Object) -join ',') -and
        @($record.machine_coverage | Where-Object { -not $_.condition -or -not $_.human_approval }).Count -eq 0 -and $matrixBindings
    $c['actual finite six request ledger not reset or mistaken for approval'] =
        $record.generation.performed_posts -eq 6 -and $record.generation.successful_http_200 -eq 6 -and
        $record.generation.selected_max_posts -eq 6 -and $record.generation.new_wave_max_posts -eq 8 -and
        $record.generation.selected_grant_exhausted -eq $true -and -not $record.generation.historical_ledgers_reset -and
        -not $record.human_listening_approved -and -not $record.release_ready
    $c['two noticed authorized imports with clean actual decoded QC'] = @($record.selected_resources).Count -eq 2 -and
        @($record.selected_resources | Where-Object { -not $_.imported -or -not $_.import_authorized -or
            $_.human_approval -cne 'pending' -or @($_.decoded_qc.flags).Count -ne 0 -or $_.decoded_qc.clipped_samples -ne 0 }).Count -eq 0 -and
        $record.rights.notice_recorded_before_import -eq $true -and -not $record.rights.cc0_or_mit_relicensing
    foreach ($name in $hashes.Keys) {
        $c["$name noticed without relicensing"] = $Files.Notice.Contains($hashes[$name]) -and
            $Files.Provenance.Contains($hashes[$name]) -and $Files.Notice.Contains('not a standalone sample or sound library')
        if ($Media) {
            $bytes = $Files.Media[$name]; $info = Ogg-Info $bytes
            $c["$name exact selected bytes"] = $bytes.Length -eq $sizes[$name] -and (Bytes-Hash $bytes) -ceq $hashes[$name]
            $c["$name complete mono44100 three-second container"] = $null -ne $info -and
                $info.Channels -eq 1 -and $info.Rate -eq 44100 -and $info.Samples -eq 132300
        }
    }
    if ($Media) {
        $c['only four selected runtime Oggs no raw or candidate bundle'] =
            ($Files.MediaNames | Sort-Object) -join ',' -ceq ((@($hashes.Keys | ForEach-Object { "$_.ogg" }) | Sort-Object) -join ',')
    }
    $c
}
$contracts = Get-Contracts $snapshot (-not $SourceOnly)
$failed = @($contracts.GetEnumerator() | Where-Object { -not $_.Value })
foreach ($failure in $failed) { Write-Output "FAIL $($failure.Key)" }
Write-Output "$($contracts.Count) PHYSICAL T0 AUDIO SOURCE/RESOURCE CHECKS, $($failed.Count) FAILED"
$controls = 0; $controlFailures = 0
function Reject-Mutation([string]$Label, [hashtable]$Mutated, [string]$Diagnostic) {
    $script:controls++
    if ($contracts[$Diagnostic] -ne $true -or (Get-Contracts $Mutated (-not $SourceOnly))[$Diagnostic] -ne $false) {
        $script:controlFailures++; Write-Output "FAIL negative control $Label -> $Diagnostic"
    }
}
if ($SelfTest) {
    $mutations = @(
        @('force load','Audio','getChunk(x, z, ChunkStatus.FULL, false)','getChunk(x, z, ChunkStatus.FULL, true)','one loaded-only discovery and tracker'),
        @('stale source','Audio','chunk.getBlockEntities().get(pos) == machine','true','current entity world block and running identity required'),
        @('kind mismatch','Audio','block.kind() == machine.kind()','true','current entity world block and running identity required'),
        @('Quern gain','Audio','GRINDING(ModSounds.T0_GRINDING, 1.0F)','GRINDING(ModSounds.T0_GRINDING, 0.35F)','Quern Mixer gain and native loop semantics preserved'),
        @('water range','Audio','0.25F, 10','0.25F, 160','physical ranges match discovery and native admission'),
        @('voice wall','Policy','MAX_LOOPS = 8;','MAX_LOOPS = 800;','nearest eight stable voices and four nonqueued onsets'),
        @('repeat latch','Policy','previous == null || previous.get() != entry.getValue()','true','nearest eight stable voices and four nonqueued onsets'),
        @('passive tank','Effects','case TANK -> { }','case TANK -> generator(status, level, pos, random);','passive tank and structural parts remain silent'),
        @('particles','Effects','ParticleTypes.SPLASH, x, pos.getY() + 0.85','ParticleTypes.SMOKE, x, pos.getY() + 0.85','all original particle calls preserved'),
        @('generic hum','Effects','case MIXER -> washer(status, level, pos, random);','case MIXER -> washer(status, level, pos, random); sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 1, 1, 1);','all generic and pylon-running synth fallbacks removed'),
        @('tracker retry','Tracker','RETRY_TICKS = 40;','RETRY_TICKS = 1;','generic tracker unchanged')
    )
    foreach ($mutation in $mutations) {
        if (-not $snapshot[$mutation[1]].Contains($mutation[2])) { throw "Negative control no longer matches: $($mutation[0])" }
        $copy = $snapshot.Clone(); $copy[$mutation[1]] = $copy[$mutation[1]].Replace($mutation[2], $mutation[3])
        Reject-Mutation $mutation[0] $copy $mutation[4]
    }
    $copy = $snapshot.Clone(); $copy.Retired = @('machine_hum.ogg')
    Reject-Mutation 'retired Ogg restored' $copy 'all five retired synthetic resources absent'
    $copy = $snapshot.Clone(); $record = $copy.Record | ConvertFrom-Json; $record.machine_coverage = @($record.machine_coverage | Where-Object { $_.id -cne 'biocultivator' })
    $copy.Record = $record | ConvertTo-Json -Depth 20
    Reject-Mutation 'missing biological coverage' $copy 'coverage exactly matches implemented T0 block crafts plus fibreboard'
    $copy = $snapshot.Clone(); $record = $copy.Record | ConvertFrom-Json
    ($record.machine_coverage | Where-Object { $_.id -ceq 'clay_mixer' }).file = 't0_water_flow.ogg'
    $copy.Record = $record | ConvertTo-Json -Depth 20
    Reject-Mutation 'wrong machine file' $copy 'coverage exactly matches implemented T0 block crafts plus fibreboard'
    $copy = $snapshot.Clone(); $copy.Notice = $copy.Notice.Replace($hashes.t0_water_flow, ('0' * 64))
    Reject-Mutation 'new notice removed' $copy 't0_water_flow noticed without relicensing'
    $copy = $snapshot.Clone(); $record = $copy.Record | ConvertFrom-Json; $record.human_listening_approved = $true; $copy.Record = $record | ConvertTo-Json -Depth 20
    Reject-Mutation 'invented listening' $copy 'actual finite six request ledger not reset or mistaken for approval'
    if (-not $SourceOnly) {
        foreach ($name in @('t0_wet_work','t0_water_flow')) {
            $copy = $snapshot.Clone(); $copy.Media = $copy.Media.Clone(); $copy.Media[$name] = $copy.Media[$name].Clone()
            $copy.Media[$name][-1] = $copy.Media[$name][-1] -bxor 1
            Reject-Mutation "$name payload" $copy "$name exact selected bytes"
            $copy = $snapshot.Clone(); $copy.Media = $copy.Media.Clone(); $copy.Media[$name] = $copy.Media[$name].Clone()
            $channel = 27 + $copy.Media[$name][26] + 11; $copy.Media[$name][$channel] = 2
            Reject-Mutation "$name stereo" $copy "$name complete mono44100 three-second container"
        }
        $copy = $snapshot.Clone(); $copy.MediaNames = @($copy.MediaNames) + @('raw.mp3')
        Reject-Mutation 'raw candidate packaged' $copy 'only four selected runtime Oggs no raw or candidate bundle'
    }
    Write-Output "$controls NEGATIVE CONTROLS, $controlFailures FAILED"
}
if ($SourceOnly) { Write-Output 'NOT_EXECUTED: binary container/payload checks (-SourceOnly).' }
Write-Output "BASELINE $baseline; static source/container proof only, not Java/native/listening/deployment."
if ($failed.Count -ne 0 -or $controlFailures -ne 0) { exit 1 }
