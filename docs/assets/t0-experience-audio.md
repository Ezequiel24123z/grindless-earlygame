# Physical T0 soundscape

Source audit: `b8bd5e885aef598bf888d7e390b297a34cbb7d52`, 2026-10-20.
This is the complete replacement wave requested after the earlier Quern/Mixer
imports, not a reinterpretation of that two-source audit or a reset of spent
grants. Human listening is **NOT APPROVED** for this new wave.

## Coverage and physical decisions

The authority is the implemented `BootstrapRecipes` block catalogue,
`BootstrapProcesses`, `RenewableProcesses`, `ProcessMachineKind`, actual block
entities and all common/Forge sound callers. Tier numbers and test fixtures do
not add gameplay. All eighteen T0 block identities, produced fibreboard,
explicit interactions and all seven baseline events were traced.

Ranges below are maximum admission radii in blocks, with mono linear native
attenuation; technical initial gains are not an artistic mix approval.
All loop admissions require the current loaded block/entity identity and
`RUNNING`, never mere stored energy/fluid or a structural part.

| Machine / interaction | Baseline event / file and classification | Intended physical source / mode | Condition and maximum range | Priority / human status |
| --- | --- | --- | --- | --- |
| Quern | `t0_grinding` / `t0_grinding.ogg`, imported A | Preserve exact stone/granular loop, gain 1 | RUNNING, 16 | Preserve; prior preview approval is not native mix approval |
| Clay Mixer | `t0_wet_work` / `t0_wet_work.ogg`, normalized imported A | Preserve exact viscous paddle loop, gain .35 | RUNNING, 16 | Preserve; listening pending |
| Firing Chamber | `machine_hum` / `machine_hum.ogg`, generic synth bursts | Quiet native furnace crackle one-shots; generated candidates failed loop QC | RUNNING controller only, 16 | Replace; listening pending |
| Biomass Dynamo | No MachineEffects sound branch, silent | Share the native physical fuel crackle, not an electric drone | RUNNING biomass block only, 16 | Add justified fuel voice; listening pending |
| Crude Extractor | `machine_hum_heavy` / matching Ogg, generic synth bursts | Selected compact mineral-working/grit loop, not Quern discs | RUNNING actual extractor, 12; gain .3 | Replace; listening pending |
| Hand Pump | `machine_hum_heavy` / matching Ogg, generic synth bursts | Selected quiet contained water-flow loop | RUNNING water movement, 10; gain .25 | Replace; no invented hand-crank/motor sound |
| Settling Trough | `machine_hum` / matching Ogg, generic synth bursts | Share narrow water/sediment flow, not thick Mixer friction | RUNNING controller only, 10; gain .25 | Replace; listening pending |
| Geological Collector | Silent | Share water/sediment flow: actual recipe consumes water to collect aggregate/clay | RUNNING eligible source process, 10; gain .25 | Add justified washing voice; listening pending |
| Biocultivator | Silent | Intentional silence: light/moisture growth is not an aerated bubbling tank | All states | Preserve; no paid family |
| Assembly Bench | Silent | Intentional silence: no proven continuously moving mechanism; inventory/GUI feedback remains | All states | Preserve; no invented powered hammer |
| Research Terminal / calibration | `machine_hum` / matching Ogg, generic tonal bursts | Intentional silence: physical core calibration does not prove a noisy motor or electronic drone | All states | Remove hum; no progress/completion beep |
| Hand Crank Dynamo | `relay_click` / `relay_click.ogg`, randomized synthesized work shots | Short native physical latch on transition to observed RUNNING; silence during sustained stored-charge output | One bounded state-edge event, 16 | Replace; not a claim of individual crank/energy success |
| Crank Handle | No explicit event | Intentional silence beyond existing action/state feedback; no loop per manual charge | Actual player action | Preserve; no extra paid sound |
| Basic Tank | Already corrected silent passive TANK | Intentional silence, including full tank reporting RUNNING | All states | Preserve storage/state/NBT; no fake combustion |
| Clay Conduit | Silent, particles only | Intentional silence; no per-segment ambient water voice | All states | Preserve |
| Conveyor Belt | Silent, particles only | Intentional silence; no invented motor or per-item clicking wall | All states | Preserve |
| Crude Manipulator | Silent, particles only | Intentional silence; no repeating automation notification | All states | Preserve |
| Settling Channel | Silent stateless part | Intentional silence; controller owns any real process voice | All states | Preserve |
| Settling Screen | Silent stateless part | Intentional silence; controller owns any real process voice | All states | Preserve |
| Fibreboard | Native wood material sounds, no machine event | Inherited material placement/break feedback, not a working bed | Explicit interaction only | Preserve; no copied vanilla files |
| Multitool rotation / relocation | Native affected block place/break events | Existing material interaction one-shots | Successful existing server action | Preserve; no extra notification |
| Flux Conduit link / unlink | `pylon_link` / `relay_click`, synthesized files | Preserve IDs; reference native tripwire attach / lever latch events | Existing successful server action, native attenuation | Replace resource only; not T1 implementation |
| Pylon-like shortage | `brownout_alarm` / synthesized alarm | Preserve ID; native physical safety-trip clack, not a tonal alarm | Existing explicit shortage route only | Replace resource only; ordinary faults silent |
| Pylon-like RUNNING | Random `pylon_link` chirp | Intentional silence; a successful-link event is not continual work ambience | All sustained work | Remove periodic chirp; no future machine implementation |
| Other existing later-machine hum callers | `machine_hum` / `machine_hum_heavy` synth | Remove inappropriate generic fallback; preserve particles/gameplay and compatibility IDs | No new later-tier audio work | Silence, explicitly not a completed T1 sound design |
| Multiblock formation / invalidity, routine errors and UI | No dedicated T0 event | Preserve restraint; do not invent repeated validation/completion/error notifications | Existing visual/text feedback | No paid event family or new gameplay hook |

## Finite production plan

The new [physical purpose](../../tools/sound-design/t0-physical-purpose.json)
selects **three families, two deliberate candidates each, six POSTs total**.
The separate new contract records the coordinator's maximum of four families /
two candidates each / eight POSTs. Unused slots are not permission for rerolls.
The original Quern3/3, Mixer2/2 and broader historical allowances stay immutable.

Use the existing reviewed reservation-before-HTTP tool, one private grant and
one ignored output/receipt. No new framework, credential relocation, entitlement
check, billing activation, paid images, retry or fallback provider. Stop on
missing access, HTTP errors, storage ambiguity or unexpected charging.
The conditional estimate is 120 credits per three-second request, **720 for six**;
no invoice or guaranteed USD maximum is claimed.

**Actual generation:** six POSTs, six HTTP200 saved sources, no provider failure,
ambiguity or retry, at the exact reviewed checkpoint `1789ce9`. The one receipt
is closed/exhausted6/6, SHA-256
`546b2dbfcfc6c500523b1989de436e5ac8efa5312bca9fd06c6a9ad8318528f2`.
Do not replay Generate or treat the unused two ceiling slots as a new grant.
All actual dates/source hashes and original QC identities are in
[the machine-readable record](t0-experience-audio.json).

Water B has a lower original encoded seam slope than A. Mineral B has a low
level recoverable by normalization; A has a0.386-second quiet interval.
Their normalized final decoded files have no flags or clipping:
water `5fbfa5c1`,33,490B,gain2.5929911824553469; extraction
`a634f9a1`,29,241B,gain5.3096296296296295. Both are mono44100/3seconds and
remain technically selected, not artistically approved.

Neither generated fire candidate is forced into the game. A has original
boundary warnings; one bounded2.63-second window/40ms periodic crossfade still
fails after normalization with0.1915s leading silence. A100ms energy scan also
shows a sparse transient dominates the purported bed. B has a1.0399s quiet
interval and boundary warning. Preserve both originals and the failed derivation
(`3b07d076` receipt), without reroll or weakened thresholds. Actual fuel roles
use existing native furnace crackle as sparse one-shots instead, not another
generic drone or a cosmetic "QC pass" on an unsuitable source.

Select on actual decoded codec/clipping/silence/boundary evidence. Normalize
with constant gain bounded by RMS .03 / peak .35, preserve source/derived hashes,
and use only justified deterministic seam repair when actual QC requires it.
Commit exact rights/notices before selected binary import. All unselected sources
and raw receipts stay private/ignored; only selected mono 44.1 kHz Vorbis enters
the runtime package. Technical selection is not listening.

## Runtime and compatibility contract

Keep the existing QuernLoopTracker unchanged and extend the single nearby loaded
chunk discovery, at most nine chunks, to actual eligible T0 source types.
No forced chunks, block-volume/world scan, strong old-world retention, duplicate
MachineEffects fallback or loop on tanks/parts. Preserve pause, mute/unmute,
admission suppression, retry backoff, unload/removal, dimension/player change,
disconnect and resource reload ownership retirement.
The nearest-eight stable-position mix caps working voices across all four
loop families. The small source-independent policy also caps observed crank
edges at four in one tick and observes suppressed edges without queuing a later
burst. Its mechanism cases are authored in the existing Java audio check, not a
second tracker; Java execution remains separate below.
This edge means observed RUNNING output, not proof of an individual crank input.
Voice construction captures only the immutable source type/kind; admission
separately rechecks mutable current block/status, avoiding a status-change/null
profile between discovery and native admission.

Keep all five old SoundEvent IDs registered to avoid registry/save churn.
Empty hum bindings intentionally represent silence; physical interaction aliases
reference installed Minecraft events rather than redistributing vanilla audio.
Remove the five old synthesized Ogg files. Normal generation may retain historical
development-only WAV writers, but both legacy converters must reject runtime
resurrection. No quest, fluid, structure, process execution or NBT change is needed.

Actual imported output is exactly four Oggs: preserved `t0_grinding` /
`t0_wet_work`, selected `t0_water_flow` / `t0_mineral_extraction`. All five old
synthetic files are removed. All seven baseline event IDs remain, with three
new event IDs; native combustion is an event reference, not a fifth runtime file.
`MachineEffects` retains every original particle call and the explicit shortage
exception, but no light/heavy hum, repeated dynamo click or pylon-running chirp.
Sparse native combustion is limited to actual RUNNING FIREBOX/BIOGEN roles.

Preparation validation: actual WinPS5 default guard passes **695 assertions**
(128 legacy guards + 200 legacy flow + 171 purpose guards + 196 purpose flow).
The six-request Plan exits0 with authorization/network/secret-read all false.
The new negative fixture initially expected `SD_VALIDATION` instead of the
existing `SD_PURPOSE_UNAPPROVED` code and failed; only that test expectation was
corrected. No production guard, approval requirement or old receipt was weakened.
Previously approved FFmpeg9.0.2/ffprobe and Core7.6 executables match their
recorded SHA-256s; no installation or PC JVM/GPU/Prism lease was used.

After import, the updated existing source/resource checker passes **46 contracts
and21 negative controls** on actual WinPS5 and verified Core7.6, exit0. This includes exact mono44100/
132300-frame containers, all five absent synth Oggs, ten event bindings, actual
nineteen-block T0 coverage derived from the craft/catalogue intersection plus
fibreboard, no generic fallback and retained particles/Quern/Mixer/tracker/notices.
Verified Core also passes the default695 offline guard/flow assertions.
Actual link validation passes737 relative links across51 Markdown files and
160 ADR sections/index entries. The existing Java audio check now authors the
mix/onset mechanism cases; Java/common/Forge/native execution is **NOT EXECUTED**
in this lane because no JVM/PC lease was granted. Parent integration must run
the affected compilation/Java audio/assets checks and one new-binding boot.
Do not reuse old747 registration proof for three newly registered events.

The actual retired Windows converter refuses the real legacy-source root before
any writes. Native SH is **NOT EXECUTED**: the current app-bundled Git has no
Bash, the initial locator fails before launching a script, and the named standard
installation check also finds none. No dependency is installed. First QC-result
inspection also hits the host's script policy; the existing process-local Bypass
invocation reads retained results without repeating generation or changing OS policy.
These host limits do not turn the successful Windows/Core checks into Linux/SH
qualification or conceal the preserved generated-fire failure.

## In-client human review checklist

These are **NOT EXECUTED** by source or codec checks:

- One machine from each physical family: verify onset, only RUNNING eligibility,
  pause, starvation/block/idle stop, breaking/replacement and output backpressure.
- Eight identical machines, then a mixed workshop: assess balance against Quern
  and Mixer, distinguish mechanisms, check clipping, beating and speech/game masking.
- Listen continuously for at least ten minutes; judge fatigue, harshness,
  periodicity, repeated seams and whether a quieter gain or silence is preferable.
- Walk toward/away/across the10/12/16-block physical-family boundaries and chunk corners; verify smooth
  falloff and no remote/start-stop storm, including spectating/listener movement.
- Disconnect/reconnect, respawn/dimension change, unload/reload a chunk and reload
  resources; confirm old voices stop, one new owner returns, no duplicate or ghost.
- Mute MASTER and BLOCKS independently, pause with and without an overlay, unmute;
  verify no consumed admission, lingering voice or every-tick restart.
- Check crank latch once per observed running edge, not every animateTick, manual
  click or stored-charge tick; unrelated placement/relocation feedback stays natural.
- Compare full passive tanks, conduit/belt/manipulator rows, structural parts,
  culture, bench and terminal: silence must remain intentional and complete.

## Current handoff

| Category | Current source-complete handoff |
| --- | --- |
| COMPLETED | Full coverage; six HTTP200 sources; two noticed/normalized/imported physical files; all five synth Oggs retired; safe compatibility IDs, native combustion, one tracker/scan and bounded mix/onsets; WinPS/Core695 offline assertions and46+21 source/resource contracts plus737 links pass. |
| BLOCKED | Native SH qualification: current bundled Git and named standard install have no Bash; no install attempted. No provider, credential or source/resource blocker. |
| NOT EXECUTED | Native SH; Java/common/Forge compilation and authored mechanism/resource suites, one changed-event registration boot, native client playback/listening, deployment. No JVM/PC lease; parent owns combined qualification. |
| NEEDS HUMAN VALIDATION | All new artistic selection, encoded listening, gain/falloff, seams and one/eight/mixed workshop fatigue. |
| NEEDS MY DECISION | None for the bounded delegated sound unit. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None. Existing authorized credential was read only programmatically by the reviewed transport, never output, hashed, copied or committed. |
| COST / BILLING RISK | Actual new POSTs6 of maximum8, all HTTP200, closed selected6/6 grant, conditional estimate720 credits. No retries, billing change, paid images or historical-ledger reset. |
| DEPLOYED STATE | No build, Prism, save, JVM/GPU or installed-instance mutation by this lane. |
