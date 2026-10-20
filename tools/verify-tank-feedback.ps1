# Offline source contract for the passive TANK checkpoint; no JVM, client or codecs.
# -SelfTest rejects the exact Git baseline and in-memory TANK-only mutations.
param(
    [string]$Root = (Join-Path $PSScriptRoot '..'),
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$baseline = '269965b1b0b8c1becda74459a853f245105649ee'
$source = 'common/src/main/java/io/github/ezequiel24123z/grindless'
$effectsPath = "$source/machine/MachineEffects.java"
$oldArm = 'case TANK -> generator(status, level, pos, random);'
$emptyArm = 'case TANK -> { }'
$tankDiagnostic = 'passive TANK has one independent empty effects arm'

function Read-Source([string]$Path) {
    return [System.IO.File]::ReadAllText((Join-Path $Root $Path), [System.Text.Encoding]::UTF8)
}

function Invoke-LocalGit([string]$Arguments) {
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = 'git'
    $process.StartInfo.WorkingDirectory = $Root
    $process.StartInfo.Arguments = "--no-pager $Arguments"
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.CreateNoWindow = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $process.StartInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    try {
        if (-not $process.Start()) { throw 'Could not start local Git.' }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $text = $stdout.GetAwaiter().GetResult()
        $diagnostic = $stderr.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw "Local Git failed: $diagnostic" }
        return $text
    } finally {
        $process.Dispose()
    }
}

function Remove-JavaTrivia([string]$Text) {
    return [regex]::Replace($Text, '(?s)/\*.*?\*/|//[^\r\n]*|"(?:\\.|[^"\\])*"|''(?:\\.|[^''\\])*''', ' ')
}

$baselineEffects = Invoke-LocalGit "show ${baseline}:$effectsPath"
$effects = Read-Source $effectsPath

function Get-EffectContracts([string]$Text) {
    $code = Remove-JavaTrivia $Text
    $start = $code.IndexOf('switch (geometry) {', [System.StringComparison]::Ordinal)
    $end = $code.IndexOf('if (status == MachineStatus.OUT_OF_BAND)', [Math]::Max(0, $start),
        [System.StringComparison]::Ordinal)
    $dispatch = ''
    if ($start -ge 0 -and $end -gt $start) { $dispatch = $code.Substring($start, $end - $start) }
    $result = [ordered]@{}
    $result[$tankDiagnostic] = ([regex]::Matches($dispatch, '\bTANK\b').Count -eq 1 -and
        [regex]::IsMatch($dispatch, '(?m)^\s*case\s+TANK\s*->\s*\{\s*}\s*$'))
    foreach ($geometry in @('GENERATOR', 'KILN', 'BOILER', 'FIREBOX', 'INDUSTRIAL')) {
        $result["$geometry retains generator feedback"] =
            $dispatch.Contains("case $geometry -> generator(status, level, pos, random);")
    }
    foreach ($geometry in @('MILL', 'PUMP', 'EPUMP')) {
        $result["$geometry retains the shared heavy-hum route"] =
            $dispatch.Contains("case $geometry -> mill(status, level, pos, random);")
    }
    foreach ($geometry in @('EXTRACTOR', 'DRILL')) {
        $result["$geometry retains extractor feedback"] =
            $dispatch.Contains("case $geometry -> extractor(status, level, pos, random);")
    }
    foreach ($geometry in @('DYNAMO', 'SOLAR', 'INTERFACE', 'TRANSFORMER')) {
        $result["$geometry retains dynamo feedback"] =
            $dispatch.Contains("case $geometry -> dynamo(status, front, level, pos, random);")
    }
    $result['Quern alone disables the mill hum, not its particles'] =
        $dispatch.Contains('case QUERN -> mill(status, level, pos, random, false);') -and
        $code.Contains('mill(status, level, pos, random, true);') -and
        $code.Contains('if (hum) {') -and $code.Contains('ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.7') -and
        $code.Contains('ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9')
    $generator = [regex]::Match($code, '(?s)private static void generator\(.*?(?=private static void mill\()').Value
    $result['active generator keeps its exact hum, flame and campfire smoke'] =
        $generator.Contains('if (status == MachineStatus.RUNNING)') -and
        $generator.Contains('ModSounds.MACHINE_HUM.get(), random, 0.08F, 0.3F, 0.7F') -and
        $generator.Contains('ParticleTypes.FLAME') -and $generator.Contains('ParticleTypes.CAMPFIRE_COSY_SMOKE')
    $result['active mill and extractor keep their heavy hums'] =
        $code.Contains('ModSounds.MACHINE_HUM_HEAVY.get(), random, 0.1F, 0.35F, 1.1F') -and
        $code.Contains('ModSounds.MACHINE_HUM_HEAVY.get(), random, 0.08F, 0.35F, 0.8F')
    $result['dynamo clicks and pylon brownout warnings remain'] =
        $dispatch.Contains('case PYLON -> pylon(tier, status, level, pos, random);') -and
        $dispatch.Contains('case BANK -> pylon(1, status, level, pos, random);') -and
        $dispatch.Contains('case ARRAY -> pylon(1, status, level, pos, random);') -and
        $code.Contains('ModSounds.RELAY_CLICK.get()') -and $code.Contains('ModSounds.BROWNOUT_ALARM.get()')
    $result['IDLE returns before geometry dispatch'] =
        [regex]::IsMatch($code, 'if \(status == MachineStatus.IDLE\) \{\s*return;\s*}\s*switch \(geometry\)')
    $result['out-of-band particles keep the existing shared path'] =
        $code.Contains('outOfBand(level, pos, random);') -and $code.Contains('ParticleTypes.WITCH')
    # Compare Git-normalized line endings; the printed SHA256 values are actual disk bytes.
    $result['no MachineEffects changes outside the exact TANK arm'] =
        $Text.Replace($emptyArm, $oldArm).Replace("`r`n", "`n") -ceq $baselineEffects.Replace("`r`n", "`n")
    return $result
}

$contracts = Get-EffectContracts $effects
$fluids = (Remove-JavaTrivia (Read-Source "$source/fluid/FluidLogic.java")) -replace '\s+', ' '
$contracts['holding fluid still reports RUNNING without requiring movement'] =
    $fluids.Contains('public static MachineStatus tankStatus(boolean holding) { return holding ? MachineStatus.RUNNING : MachineStatus.IDLE; }')
$contracts['Basic Tank publishes holding state without changing tick or transfer logic'] =
    (Remove-JavaTrivia (Read-Source "$source/fluid/BasicTankBlockEntity.java")).Contains(
        'MachineProperties.publish(level, pos, FluidLogic.tankStatus(!tank.buffer.isEmpty()));')
$contracts['Basic Tank animateTick still selects TANK'] =
    (Remove-JavaTrivia (Read-Source "$source/fluid/BasicTankBlock.java")).Contains(
        'MachineEffects.animate(BlockCatalogue.Geometry.TANK, 1,')
$contracts['Basic Tank remains T0 craftable with insulator, glass and copper'] =
    [regex]::IsMatch((Read-Source "$source/recipe/BootstrapRecipes.java"),
        '(?s)new Shaped\("basic_tank",\s*List\.of\("IGI", "G G", "ICI"\),\s*Map\.of\("I", INSULATOR, "G", GLASS, "C", COPPER\),\s*"grindless:basic_tank", 0\)')

$changed = Invoke-LocalGit "diff --name-only $baseline -- common forge"
$untracked = Invoke-LocalGit 'ls-files --others --exclude-standard -- common forge'
$outsideEffects = @(($changed -split '\r?\n') | Where-Object { $_ -and $_ -cne $effectsPath })
$contracts['all other common/forge sources, NBT, transfers, registration, recipes and resources match baseline'] =
    $outsideEffects.Count -eq 0 -and [string]::IsNullOrWhiteSpace($untracked)

$ogg = Join-Path $Root 'common/src/main/resources/assets/grindless/sounds/t0_grinding.ogg'
$oggHash = (Get-FileHash -LiteralPath $ogg -Algorithm SHA256).Hash.ToLowerInvariant()
$oggBytes = (Get-Item -LiteralPath $ogg).Length
$contracts['Quern A keeps the exact approved 34364 bytes and SHA256'] =
    $oggBytes -eq 34364 -and $oggHash -ceq '811b29471e3fa6a51ce76db2241259d02adfc64e244eadc096ef376d1d1970d5'
$event = (Read-Source 'common/src/main/resources/assets/grindless/sounds.json' | ConvertFrom-Json).t0_grinding.sounds
$contracts['Quern keeps its positional non-streamed event binding'] =
    @($event).Count -eq 1 -and $event[0].name -ceq 'grindless:t0_grinding' -and
    $event[0].stream -ceq $false -and $event[0].attenuation_distance -eq 16

$failed = @($contracts.GetEnumerator() | Where-Object { -not $_.Value })
foreach ($failure in $failed) { Write-Output "FAIL $($failure.Key)" }
if ($outsideEffects.Count -gt 0) { Write-Output ("CHANGED outside MachineEffects: " + ($outsideEffects -join ', ')) }
if (-not [string]::IsNullOrWhiteSpace($untracked)) { Write-Output "UNTRACKED common/forge: $untracked" }
Write-Output "$($contracts.Count) TANK FEEDBACK SOURCE CHECKS, $($failed.Count) FAILED"

$controlFailures = 0
if ($SelfTest) {
    if ([regex]::Matches($effects, [regex]::Escape($emptyArm)).Count -ne 1) {
        throw 'SelfTest requires the real repaired source with exactly one literal empty TANK arm.'
    }
    $controls = [ordered]@{
        'exact Git baseline (already includes other empty arms)' = $baselineEffects
        'line-comment empty-arm decoy' = $effects.Replace($emptyArm, "$oldArm // $emptyArm")
        'block-comment empty-arm decoy' = $effects.Replace($emptyArm, "$oldArm /* $emptyArm */")
        'missing TANK despite other empty arms' = $effects.Replace($emptyArm, '')
        'nonempty TANK block' = $effects.Replace($emptyArm, 'case TANK -> { generator(status, level, pos, random); }')
        'string-literal empty-arm decoy' = $effects.Replace($emptyArm, 'case TANK -> { String decoy = "case TANK -> { }"; generator(status, level, pos, random); }')
        'duplicate empty TANK arms' = $effects.Replace($emptyArm, "$emptyArm`n            $emptyArm")
        'grouped TANK is not an independent arm' = $effects.Replace($emptyArm, 'case TANK, CHANNEL -> { }')
    }
    foreach ($control in $controls.GetEnumerator()) {
        $observed = Get-EffectContracts $control.Value
        $diagnostics = @($observed.GetEnumerator() | Where-Object { -not $_.Value } | ForEach-Object { $_.Key })
        $intended = $diagnostics -contains $tankDiagnostic
        if ($control.Key -like 'exact Git baseline*') {
            $intended = $intended -and $diagnostics.Count -eq 1
        }
        if ($intended) {
            Write-Output "PASS negative control: $($control.Key) -> $tankDiagnostic"
        } else {
            $controlFailures++
            Write-Output "FAIL negative control: $($control.Key); diagnostics: $($diagnostics -join ', ')"
        }
    }
    Write-Output "$($controls.Count) NEGATIVE CONTROLS, $controlFailures FAILED"
}

Write-Output "BASELINE $baseline"
foreach ($path in @($effectsPath, "$source/fluid/FluidLogic.java", "$source/fluid/BasicTankBlock.java",
        "$source/fluid/BasicTankBlockEntity.java", "$source/fluid/FluidBuffer.java",
        "$source/fluid/NativeFluidStore.java", "$source/fluid/FluidNbt.java", "$source/container/FluidInsert.java")) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $Root $path) -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Output "SHA256 $path $hash"
}
Write-Output "QUERN_A bytes=$oggBytes SHA256=$oggHash"
Write-Output 'STATIC SOURCE PROOF ONLY; compilation, rendering and native listening NOT_RUN.'
if ($failed.Count -ne 0 -or $controlFailures -ne 0) { exit 1 }
