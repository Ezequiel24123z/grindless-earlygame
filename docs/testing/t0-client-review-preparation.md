# T0 client review preparation

**Preparation ready; client execution NOT EXECUTED.** This C2 record prepares a
bounded engineering review, not a visual/listening pass, a playable RC, a survival
entry or T0 acceptance. Follow [ADR-0156](../DECISIONS.md#adr-0156--disclose-blockers-and-separate-creative-capability-from-product-acceptance)
and the [proof-reuse strategy](test-strategy.md). No application, audio, launcher,
world, dependency or deployment files were changed by this workstream.

## Identity and actual evidence

Source inspected and checked: `269965b1b0b8c1becda74459a853f245105649ee`.
The installed identity below is the coordinator's preserved
[deployment receipt](../../RETURN-HANDOFF.md), not a new C2 inspection of the PC:

| Boundary | Identity / result |
| --- | --- |
| Owned instance | `Grindless-T0-Dev`; display `Grindless T0 Engineering Dev [Grindless-T0-Dev]` |
| Installed source/runtime | `559bed5e8ea70ac52ff4b3fed3850ab05ab48256` |
| Installed JAR | SHA-256 `98aa47dd18a555bf8a7e69989fd075e161fceaf6b2c799727c949ef5792cb061`; 1926158 bytes |
| Engineering ZIP | SHA-256 `bff0fb27434da5ecd49aaf69d0f6b6635b22679029a7b95c488a175b266e8a86`; 1510587 bytes |
| Selected profile | `playable`, `restricted-playable-profile-not-rc`; seven dependencies, empty public overrides |
| Recorded runtime | Minecraft 1.20.1 / Forge 47.4.23 / verified instance-only Java 17 / 4 GiB maximum |
| New offline execution | Windows PowerShell, exit 0: 28 client/menu/JEI/widget source files, 67 literal references, 48 distinct English keys, zero absent or blank values |
| Predicate controls | Four in-memory valid/absent/blank/case-mismatch cases passed against the documented missing-key predicate; no application input was edited |
| Client texture execution | Three literal client texture references exist with PNG signatures/IHDR: shared machine GUI 256x256; survey rocket and station item textures 16x16. The nominal24x17 arrow at176,0 fits that default256 atlas |
| GUI input | `textures/gui/process_machine.png` SHA-256 `7c6e808fab5c6e126fe49bd3ab1169b194334b75f9725f699e69a2bb635f5b2f`; header/bounds evidence, not decoded pixels or native rendering |
| New input-equivalence execution | Git diff exit 0: `common`, `forge`, root build/settings/properties, wrapper and `pack` equal deployed559 |
| English input | `en_us.json` SHA-256 `cd9eeb295bdc2bdb51173c89f762b4b93279602c58e8966e526b40675e430a59` |
| Input trees | common `2353dc45464148f583c14ebeec418a844e45c0d1`; forge `a1a2ae9ac4b9465f997ad0be52fa428f3a6566da`; pack `4aca168b2b2d714fb0bd64ec29fdf0c8d2bddf7a` |

This is **not a globally green CI statement**. The coordinator reports
[run 39336114196](https://github.com/Ezequiel24123z/grindless-earlygame/actions/runs/39336114196)
at `543598a` failed `first-fluids` / `WATER-IN` and `first-harness` / `HELMET-OK`;
verify, Windows tooling and four other native jobs succeeded. That independent
root-cause workstream owns those failures. C2 neither diagnoses the harness nor
repeats those jobs. Earlier source-bound native passes remain their actual proofs,
not a replacement for the failed run.

### Reuse instead of another suite

| Existing proof / authority | Reused scope | Still needs a real client |
| --- | --- | --- |
| `VerifyMachineUi`, shared `MachineMenuLayout` | Physical slots, fluid identity, status bounds, catalyst roles and JEI detail wrapping | Native font/layout, hovered details, input and screen-scale behavior |
| `VerifyQuest`, `VerifyQuestClaims`, `VerifyResourceGuidance`, `VerifyHeracles` | Catalogue labels/pages, claim policy, projection, packets, canonical guidance and lossless pagination | Actual Heracles widget/input/synchronization and reading experience |
| `VerifyAssets`, approved Quern resource/tracker proofs | Model/texture references, native display inheritance, mono/event/subtitle/provenance and loop ownership contracts | Actual baked models, audio device/playback, lifecycle and one/eight-machine mix |
| Qualified559 build/pack88/archive and A01/A03 records | Their exact engineering properties; application/pack inputs are unchanged here | None of them proves client startup, clean launcher import, player-world migration or manual play |
| `pc-workflow` / `prism-dev` | Existing ownership, Java/dependency hashes, stopped-instance inspection and deployment separation | An authenticated graphical launch and native interaction |

The literal scan below complements the existing targeted checks, including
Heracles action labels. It is a lexical reference check, **not a Java parser**:
assembled keys, `PREFIX + ...`, catalogue-derived keys, translation substitution,
other languages and font-pixel widths are outside its claim. Existing catalogue
checks retain their own scopes. Character counts are not used as a clipping oracle.
Impeccable was loaded for technical review guidance; neither web nor mobile audit
scores are fabricated for Minecraft.

## Reproduce the new offline check

Run from the repository root in Windows PowerShell 5.1 or later. It reads public
source/resources only; it starts no JVM, Gradle, SSH, Prism, network or game process.
Missing source directories, malformed JSON, missing/blank literal keys and absent
literal textures fail. PNG checks read signatures/dimensions, not decoded pixels.

```powershell
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
$root = (Get-Location).Path
$common = 'common\src\main\java\io\github\ezequiel24123z\grindless'
$forge = 'forge\src\main\java\io\github\ezequiel24123z\grindless\forge'
$files = @()
foreach ($directory in @("$common\client", "$common\menu", "$forge\jei")) {
    $found = @(Get-ChildItem -LiteralPath (Join-Path $root $directory) -Filter '*.java' -File -Recurse)
    if ($found.Count -eq 0) { throw "No Java sources in $directory" }
    $files += $found
}
$files += Get-Item -LiteralPath (Join-Path $root "$forge\quest\HeraclesQuestClient.java")
$lang = [IO.File]::ReadAllText((Join-Path $root 'common\src\main\resources\assets\grindless\lang\en_us.json'), [Text.Encoding]::UTF8) | ConvertFrom-Json
$translations = New-Object 'System.Collections.Generic.Dictionary[string,string]' ([StringComparer]::Ordinal)
foreach ($property in $lang.PSObject.Properties) {
    if ($property.Value -isnot [string]) { throw "Non-string translation: $($property.Name)" }
    $translations.Add($property.Name, $property.Value)
}
$pattern = '"(?<key>[A-Za-z_][A-Za-z0-9_]*\.grindless\.[a-z0-9_]+(?:\.[a-z0-9_]+)*)"'
$references = @(foreach ($file in $files) {
    foreach ($match in [regex]::Matches([IO.File]::ReadAllText($file.FullName, [Text.Encoding]::UTF8), $pattern)) {
        [pscustomobject]@{ Key = $match.Groups['key'].Value; File = $file.FullName.Substring($root.Length + 1) }
    }
})
if ($references.Count -eq 0) { throw 'No literal translation references were collected.' }
$missing = @($references | Where-Object { -not $translations.ContainsKey($_.Key) -or [string]::IsNullOrWhiteSpace($translations[$_.Key]) })
if ($missing.Count -gt 0) { $missing | Format-Table -AutoSize; throw "$($missing.Count) unresolved or blank literal references." }
$keys = @($references.Key | Sort-Object -Unique)
Write-Output "PASS: $($files.Count) source files; $($references.Count) literal references; $($keys.Count) unique English keys; zero missing/blank."
Write-Output ('LANG_SHA256=' + (Get-FileHash -LiteralPath (Join-Path $root 'common\src\main\resources\assets\grindless\lang\en_us.json') -Algorithm SHA256).Hash.ToLowerInvariant())
$assets = Join-Path $root 'common\src\main\resources\assets\grindless'
$textures = @(foreach ($file in $files) {
    foreach ($match in [regex]::Matches([IO.File]::ReadAllText($file.FullName), 'Grindless\.id\("(?<texture>textures/[a-z0-9_/]+\.png)"\)')) {
        $match.Groups['texture'].Value
    }
})
if ($textures.Count -eq 0) { throw 'No literal client texture references found.' }
foreach ($texture in $textures) {
    $path = Join-Path $assets $texture.Replace('/', '\')
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing client texture: $texture" }
    $bytes = [IO.File]::ReadAllBytes($path)
    if ($bytes.Length -lt 33 -or [BitConverter]::ToString($bytes, 0, 8) -cne '89-50-4E-47-0D-0A-1A-0A' -or [Text.Encoding]::ASCII.GetString($bytes, 12, 4) -cne 'IHDR') { throw "Invalid PNG signature/IHDR: $texture" }
    $width = [Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, 16))
    $height = [Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, 20))
    if ($width -le 0 -or $height -le 0) { throw "Invalid dimensions: $texture" }
    if ($texture -ceq 'textures/gui/process_machine.png' -and ($width -ne 256 -or $height -ne 256)) { throw 'Machine GUI texture differs from the default 256x256 blit atlas.' }
    Write-Output "PASS: $texture exists; PNG IHDR ${width}x${height}."
}
Write-Output ('GUI_SHA256=' + (Get-FileHash -LiteralPath (Join-Path $assets 'textures\gui\process_machine.png') -Algorithm SHA256).Hash.ToLowerInvariant())
```

The separately executed equality check was:

```powershell
$changes = @(git --no-pager status --porcelain --untracked-files=normal -- common forge build.gradle settings.gradle gradle.properties gradle gradlew gradlew.bat pack)
if ($LASTEXITCODE -ne 0 -or $changes.Count -gt 0) { throw 'Application/pack inputs must be clean and committed.' }
git --no-pager rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Source identity failed.' }
git --no-pager diff --exit-code 559bed5e8ea70ac52ff4b3fed3850ab05ab48256 HEAD -- common forge build.gradle settings.gradle gradle.properties gradle gradlew gradlew.bat pack
if ($LASTEXITCODE -ne 0) { throw 'Runtime/pack input equivalence failed.' }
git --no-pager rev-parse HEAD:common HEAD:forge HEAD:pack
if ($LASTEXITCODE -ne 0) { throw 'Source tree identity failed.' }
```

A later audio/UI/pack delta requires affected checks and a newly recorded artifact
identity before reviewing that delta. It does not retroactively enter this559
review or invalidate unrelated executor proofs.

## Prepare the actual review surface

**Not executed by C2:** no PC/client lease, explicit child diagnostic target or
client-launch authorization was supplied. Existing tools already cover deployment;
no redundant `review-client` launcher, new smoke suite or headless-graphics workaround
is introduced.

1. The coordinator obtains an exclusive client/JVM window and an explicit approved
   private target. Before launch, use the existing read-only inspection, not Sync,
   Build or Deploy. A child must not fall back to the default private inventory.

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\pc-workflow.ps1 -Action Inspect -ConfigFile '<coordinator-approved-private-inventory>'
   ```

   Require `VerifiedStopped`, the expected instance, source/runtime, JAR/ZIP hashes
   and pack status above. Inspect the instance-only Java 17 / memory settings.
   This is readiness, not an authentication, graphics or startup test. If anything
   differs, record it and stop; do not repair ownership, delete locks, rebuild or
   substitute another instance silently.
2. The owner opens the existing named engineering instance in Prism. Any account,
   OS authentication or unsafe-content barrier is handled by the owner; do not copy
   launcher authentication or publish its screen. Record actual launch/startup
   errors rather than assuming a server boot proves OpenGL, JEI or Heracles.
3. Create a **new disposable Creative, commands-enabled diagnostic world**, for
   example `Grindless-C2-Review-559` with a unique suffix if that name exists. A
   normal Overworld is sufficient for rendering/input diagnostics; it is not the
   pending official Superflat entry. Never open, copy, update or adopt the old
   preparation save or a longitudinal reference. No world has been created here.
4. Record observer (human/agent), UTC interval, artifact identity, language,
   framebuffer/window size, actual GUI scale, resource packs and MASTER/BLOCKS
   volume/subtitle settings. Begin in English with upstream resources. Use the
   owner's ordinary scale and one larger selectable scale in the same session;
   retain actual dimensions and any overflow rather than assuming fixed character
   limits or desktop resolution proves fit.

Only in that disposable diagnostic world, these existing items provide the small
review surface; the commands below have **not** been executed:

```text
/give @s grindless:process_atlas
/give @s grindless:quest_book
/give @s grindless:field_guide
/give @s grindless:multitool
/give @s grindless:quern 8
/give @s grindless:hand_crank_dynamo 8
/give @s minecraft:gravel 64
```

Use Creative inventory for the other named T0 controllers/components below.
Command-supplied stock is diagnostic only, never renewable/bootstrap/survival
evidence. Do not inject quest save flags, fake a running blockstate or alter recipes.

## One bounded native review, then human judgment

Every row below is **NOT EXECUTED**, not a passing test because a procedure exists.
Reuse the same diagnostic session and capture a defect only where observable.

| Surface | Exact probe / observation to record |
| --- | --- |
| Startup and resources | Reach title, open the diagnostic world, inspect the three book icons and native held/inventory/placed T0 models. Record missing-model/texture, resource-reload, mixin or dependency errors; do not infer visual quality from JSON or source pixels. |
| Machine UI | Open Quern and Settling Trough: empty and populated slot backgrounds, all five trough output positions, player slots, energy/status and fluid hover regions. Open an unformed Firing Chamber and a Geological Collector without a rock bed; hover the full status. With normal actual water supply, inspect the Mixer/trough tank and included output quota. Do not fabricate unknown-fluid state merely to repeat an existing sync contract. |
| Atlas, guide and JEI | Right-click each book/Atlas. Select the live Quern gravel route, wet mixing, settling and terminal calibration in the relevant viewer. Scroll Atlas rows, then scroll its detail region to cycle resource operands; hover with/without Shift. Page the guide across long topics. In JEI, use item recipe/use views for the T0 controllers/core: compare quantities, full conditions, item versus fluid cells and any actual nonconsumed catalyst label. No second recipe catalogue or T1 gameplay is required. |
| Heracles | Hold the Grindless Quest Book in either hand and right-click it. Select the Multitool task, supply that real item, claim once and compare inventory/reward/status. Attempt a repeat; then inspect a later unmet/locked task, long wrapped content and the claim button. Record native click alignment and server-confirmed state. Do not edit progress to force eligibility or treat a diagnostic claim as survival. Legacy fallback is a separate optional-dependency case, not covered by this seven-pin run. |
| Quern audio | Place one Quern touching its own Dynamo, insert gravel, empty hand and crank the Dynamo twice. Verify real running progress; replenish if needed. Observe start, starvation/idle stop, removal and distance beyond16 blocks/return. Repeat with eight separated, independently supplied pairs inside range, not eight machines sharing one F0 source. Pause/unpause, MASTER then BLOCKS mute/unmute, resource reload (`F3+T`) while work continues and world disconnect/rejoin expose the remaining native lifecycle boundary. Record audible behavior, device errors and settings; `isActive`/tracker admission is not proof of sound. |
| Human assessment | Owner judges legibility, discoverability, appearance, repetition/fatigue, one/eight-machine balance and sound quality. Keep observation distinct from automated resource/reference checks. New wet-work/other SFX families are a separate integration identity; none is assumed installed in559. |

The first useful runtime property is **this exact owned client reaches the title
screen and opens the native Atlas, guide, machine and Heracles surfaces without
resource/binding errors**. That resolves graphics/client-only integration uncertainty
which server/contracts cannot. If startup fails, collect that failure and stop;
do not rerun A01/A03, the44-suite gate or a server matrix as a proxy.

## Evidence collection and next execution request

Exit Minecraft normally before another inspection/deployment. Use Prism's Minecraft
folder action for **only the approved instance**. Retain the relevant
`minecraft\logs\latest.log` (or that instance's existing `.minecraft` equivalent),
one specifically identified client crash report if present, and bounded screenshots
of the observed surfaces. Do not scan launcher account files, unrelated saves or
process arguments. Keep raw logs/screenshots in ignored acceptance storage, not Git;
redact player/server identifiers, chat, tokens and private machine paths before sharing.
Record expected/actual behavior, reproduction, severity and the exact source/JAR,
not just "looks broken" or a generic test count.

Request to the coordinator: assign one exclusive graphical/client window with the
explicit diagnostic target and owner-authenticated Prism session, confirm current
artifact identity/headroom, and select owner-driven review or a separately authorized
bounded agent launch. No credential search, new account, build, deploy, paid service
or protected-save access is required by this preparation. Clean import and authentic
forward-save compatibility remain separate unexecuted release properties.

## C2 handoff disclosure

| Category | Actual C2 outcome |
| --- | --- |
| COMPLETED | Source inspection, executed literal-key/texture-reference, four predicate-control and deployed-input-equivalence checks; reproducible owner review procedure and proof boundaries. Documentation links/ADR anchors pass659/156. Existing tool/resource/claim/geometry proofs reused without JVM replay. |
| BLOCKED | Native execution remains held: no exclusive client lease or explicit child diagnostic target/launch authorization. Latest reported CI also has the two named failures, owned by a separate root-cause workstream. |
| NOT EXECUTED | PC/Prism inspection or writes, client launch, world creation, rendered layout/input, native audio/lifecycle/listening, clean pack import, actual retained-world compatibility and no-command campaign. No A01/A03/full-suite/matrix rerun. |
| NEEDS HUMAN VALIDATION | Appearance, UX/discoverability, listening/mix quality and the actual manual T0 campaign. No T0 acceptance/freeze follows from this preparation. |
| NEEDS MY DECISION | None within C2. Official Superflat/product choices and release acceptance remain with the coordinator/owner; no preset, starter kit, quest economy or T1 activation was selected. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None for offline checks. Native execution needs the existing owner-authenticated graphical Prism session and explicitly assigned runtime capacity/target, not copied credentials. Existing audio access is resolved outside C2. |
| COST / BILLING RISK | None incurred: zero provider/generation/download requests, no billing activation and no JVM/client runtime. |
| DEPLOYED STATE | No change by C2. Coordinator-recorded559 / JAR98aa / ZIPbff0 remains the engineering deployment; original preparation instance/save remain protected and were not accessed. |

Current release ordering and the later no-command/longitudinal criteria remain in
[the survival checklist's current policy](t0-survival-acceptance.md#current-release-and-world-policy)
and [the development/release gate](../reviews/go-gate.md). This document does not
promote its diagnostic review to either campaign completion or release readiness.
