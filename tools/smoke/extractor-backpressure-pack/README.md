# A01 Crude Extractor recovery regression

Source baseline: `48c9513f57a1a7391108c86b21bfcd386044651e`, inherited from the active
coordinator, not `main`. This is a diagnostic fixture, not a survival route, Tier
unlock, release proof or permission to deploy to a player's instance.

## Frozen pre-fix disposition: `4f1b30b7eb25912b2cfa9150a1a159bbf88bf60a`

Before any application change, no-JVM inspection confirms all six intentionally
missing source contracts: independent subscription, exporter release, retry
callback, same-world-tick work guard, copied offer and changed-remainder check.
Structural validation passes for nine functions, eleven resolved local function
references and ten conditional/terminal expectation markers. Documentation
validation passes 640 links and 154 ADR section/index pairs; whitespace passes.
At that checkpoint the runtime counterexample was not executed and application
source remained byte-for-byte at the baseline. The later execution below keeps
that source/fixture identity rather than relabelling source inspection as a run.

## Current counterexample and selected pattern

At that source, `CrudeExtractorBlockEntity.updateSubscriptions` cancels `working`
when completed work cannot fit the internal output. `pushOutput` is reached only
from a successful `eject`. There is no independent output subscription. Emptying
the adjacent chest therefore cannot move the already-owned full stack and wake
the completed cycle. The existing `VerifyExtractor` checks policy arithmetic,
not this loaded-world path. Process-machine output evidence is not extractor
recovery evidence.

Reuse the existing process-machine pending-export policy: one independently
subscribed, eight-tick export attempt while owned output remains, using
`ItemInsert`, a copied offered stack and changed-remainder notification.
Reuse its world-time work guard because `TickSubscriptions.tick` executes newly
added work in the same pass. Do not change the global scheduler, item sides,
vein derivation, F0 rates, products or the `Progress`/`Output` save keys.

## Repair disposition

The source repair follows that pattern without modifying `ProcessMachineBlockEntity`,
`MachineBlockEntity`, `TickSubscriptions`, `ItemInsert`, the loader or registries.
One transient `lastWorkTick` blocks repeated work from inventory/energy callbacks.
An already-completed paid cycle settles before requesting power. The final
subscription refresh restores work cancelled by the insertion listener; runnable
status remains work-owned, so an eight-tick export refresh does not continually
reset recovery debounce.

The private typed constructor follows the existing process-machine test seam:
ordinary registered construction delegates to it, while checks can instantiate
the actual block entity without a loader. There is no gameplay bypass flag.
Four binary-NBT reconstruction cases retain partial/completed/overshoot progress,
owned item tags, Flux, the single output slot and extract-only policy. Frozen
`c87dbc8` now passes those24 assertions within its59-check focused PC gate;
this is binary-NBT reconstruction, not a live Level or actual restart.

`VerifyExtractor --a01-vein-data <path>` reads the real cleanly stopped Luna
SavedData and requires exactly one extraction in each diagnostic chunk, exactly
two worked chunks and a total of two. It does not manufacture a production
counter or infer accounting from an item-only oracle.

| Evidence | Actual state | Scope |
| --- | --- | --- |
| Frozen pre-fix source | CONFIRMED | Six missing safeguards; unchanged application source from the inherited baseline |
| No-JVM repaired-source checks | PASS | Ten source safeguards; thirteen unchanged behavior bodies and the unchanged Container implementation |
| Native fixture structure | PASS | Nine functions, eleven resolved references, ten markers; executable fixture bytes unchanged from `4f1b30b` |
| Documentation and whitespace | PASS | 640 relative links, 154 ADR section/index pairs, no whitespace errors |
| Java baseline suite | EXPECTED RED | `4f1b30b`: classpath 0, javac 0, Java 1; 25 existing checks pass and exactly six source guards fail |
| Java repaired suite | PASS | `c87dbc8`: classpath 0, javac 0, Java 0; 59 assertions pass, including 24 actual binary-NBT assertions |
| Native baseline fixture | EXPECTED RED | One production boot on `4f1b30b`: valid setup/load/finish, four absent export/recovery markers, normal shutdown |
| Native repaired fixture | NOT_RUN | Repaired loaded recovery/world-time behavior still needs its separate window |
| Stopped-world SavedData oracle | NOT_RUN | Four authored exact accounting assertions |
| Compilation | SCOPED PASS | Common compilation on both sources; Forge/remapped baseline jar through native preparation only |
| Full build, repaired packaging, client and deployment | NOT_RUN | Not authorized by this bounded execution lease |

Source checks do not execute Java, validate Minecraft command grammar or observe
the game's subscription/transfer ordering. The baseline boot below establishes
the real failure and fixture binding. Actual repaired `SimpleContainer`/
`ItemInsert` mutation, first-tick recovery and re-added work within the live
world's tick remain distinct uncertainties. The existing process-machine proof
does not cover this class.

## Executed focused and baseline-native evidence

The coordinator granted isolated immutable worktrees and exclusive serialized
PC ownership for focused RED/GREEN, then conditionally authorized one native RED
after the actual GREEN pass. Every phase refreshed the unchanged 6-GiB physical,
8-GiB commit, 8-GiB work/cache and zero-Java launch guards. Gradle used 3 GiB,
two processors, one worker, no parallel build, no persistent daemon and SerialGC;
focused helpers used 1 GiB/two processors. The native server retained the
canonical 2-GiB heap and 900/120/60/60-second startup/assertion/handshake/shutdown
budgets. No full `build`, repaired native, deployment or player world was used.

The selected `:common:writeCheckClasspath` stages exit 0 in 51.625 seconds
(`4f1b30b`) and 52.027 seconds (`c87dbc8`). Each actual `VerifyExtractor` compiles
with javac exit 0. The baseline Java run exits 1 with exactly these six failures:
independent subscription, empty-output release, eight-tick retry, world-time work
guard, copied offered stack and changed-remainder notification. Its other 25
checks pass. Repaired Java exits 0 with 59/0, not a Level or server observation.

The single native baseline attempt exits 1 in 195.114 seconds. Its implicit
`:forge:prepareSmokeServer` succeeds in 41 seconds; the actual server reaches
`Done` and the console handshake. The fixture emits all three blocked/load
markers, the one-tick/eight-Flux and single-completion markers, and its final
marker. The four export/recovery markers are absent, including
`A01-RECOVERED-EXACTLY-65`; the harness reports the first missing
`A01-PARTIAL-EXPORT-WITHOUT-VEIN`. There are no function-grammar failures.
This is the expected loaded-world counterexample, not a preparation/parser
failure, fixed-output proof or successful native suite.

The installed `grindless.jar` and built `grindless-0.1.0-forge.jar` hashes match.
The installed jar contains the real extractor class without the new exporter
or world-time guard, consistent with frozen `4f1b30b`. Shutdown reaches
`All dimensions are saved`, with no launcher/shutdown failure. The final
21:16:48Z inspection finds both source worktrees exact and clean, the original
clone HEAD/config preserved, zero Java and the reserved port free; the coordinator
received an explicit lease release. The old harness removes its diagnostic
world, so no disk-accounting pass is claimed.

Private raw evidence is retained under the ignored
`build/validation/a01-focused-lease/` directory of the baseline execution
surface. Source, stage exits and raw-stream hashes are separate from auxiliary
collection. Portable identities:

| Evidence | SHA-256 |
| --- | --- |
| RED Java stdout | `99b1f29f76cf929dfc2ff188a3a071a7f9ea537b25be277f6f336256d5bbab59` |
| RED Java receipt | `214c5aeee9daf1ab42987c08e97308b7791798260d7db7673a6be7d2e95e4d20` |
| GREEN Java stdout | `3b1f159094b8ad649b59bb9dc10445a7f8f5feb14e5f2493d8f96e82a8df47c8` |
| GREEN Java receipt | `c9981281a69c82e27b68d43491ef52fe9d8b80b99c9939bc56d7ee73d731c27a` |
| Focused collection | `7102a53c4d9563b4b18798d2fd69c78fb3ceff7a053d44f958c57515bd3e6d6b` |
| Baseline runtime/built jar | `c4dfc0f92e2d2778bb02b977179949e6fef22926cd7271c7cf9c4171b9c630f4` |
| Native baseline boot log | `e7ba12ed772e72d93a0b2e3e338a1bcac5191f11f381f09f40f1fdf098a359fe` |
| Native baseline stdout | `a229c832f87ffa09b25f3fd3198f4eadbe0b0b67df64a29a1d7fb8bb0d7cceec` |
| Native baseline stderr | `c0b980b59d2902966b83da022337473dd5adf9ec7fd731412408d659a234e3a1` |
| Native baseline receipt | `bfa5117ba9048192bf8b9b9cf67b8f2508decede5cf86bbd4a7e6b3a8341806c` |
| Separate native-log collection | `51cb689bffd7de3363eb476e5a7cefa7c2b210fa5b7ff66a2cef73ece3b5e03a` |

Preserved auxiliary failures: the first local helper invocation was blocked by
PowerShell execution policy before any remote stage; process-only `Bypass`
resolved it without a global policy change. Initial preparation and a later
collection each lost the stdin-framed SSH transport with exit 255. Read-only
inspection resolved unknown state before continuation; no completed JVM stage
was replayed. One local bootstrap-identification regex also failed before PC
execution. Its corrected inspection positively identified one childless owned
observer, which was stopped by its specific PID, never by process name or by
killing an unrelated process. A private hash-verified SCP helper plus short
`ssh -n -T` invocation completed the remaining stages. Helper paths, transport
code, host settings and raw logs stay outside Git. javac deprecation notes and
the baseline's two Forge warnings remain warnings, not failed compilation.

## Native oracle

- In Luna, a known direct helium-3 product avoids dependence on the pack's ore
  tags. A full internal stack and exactly one completed paid cycle wait beside
  a full chest with no power.
- Retain that exact NBT, clear only the original inventory before replacing
  the diagnostic block entity, and restore the existing saved keys into a fresh
  instance. Verify the blocked state survives its first ticks.
- Empty only the same chest's inventory. Expect exactly 65 helium-3 in the
  destination, no internal remainder, zero paid progress and zero Flux, stable
  across a further 16 ticks. No hopper, block replacement at the destination or
  new energy wakes this path.
- In the Drift, where no vein exists, offer a previously owned stack to a chest
  with only one unit of space. Expect a 63-unit remainder; freeing one filler
  slot must drain it. Destination totals 127 (63 staged + 64 owned), while
  progress 123.5 and stored Flux 16 remain unchanged.
- In a separate Luna chunk, a nearly completed cycle with 63 owned items and
  16 Flux crosses the last internal-space boundary. The observation scheduled
  exactly one world tick later must find 64 items and 8 Flux, not a second
  work/Flux debit from a newly appended subscription.

The first Luna chunk `(0, 0)` must record exactly one extraction and the guard
chunk `(1, 0)` exactly one. Read the resulting dimension's `grindless_veins.dat`
after clean server shutdown to check actual accounting; item counts alone do
not prove the SavedData counter. The fixture leaves its diagnostic block
entities and storage checkpoints available while the world exists; the original
harness removes that world in its final cleanup.

Reconstruction exercises native block-entity `load` and first-tick subscriptions.
It is not an actual server restart, chunk unload/reload, crash-atomic persistence,
foreign capability guarantee or retained-player-world migration.

## Execution boundary

The original standalone `c87dbc8` harness removes the world before a post-stop
check. The coordinator now integrates the separately reviewed `444409f` retention
under ADR-0155. The repaired sequence below requires that integrated harness and
a new explicit execution lease; it is not a native pass. The old harness remains
valid for the pre-fix counterexample, which must not be followed by a claimed
saved-accounting pass.

This fixture owns only its uniquely named commands/expect/pack. It intentionally
does not edit `scenarios.txt`, the shared harness or CI; their owner/coordinator
selects integration. The scoped execution above does not authorize a further
run. Repaired-native, its stopped-world accounting, a full build, packaging and
deployment remain **NOT_RUN** and require a separate coordinator-granted window.
No `pc-workflow` command or default private inventory was used by this child.

Use the coordinator's approved JDK, process-local Gradle/cache settings and fresh
capacity guards. The smallest selected Java preparation/check, with the existing
bounded 1-GiB helper policy and argument-file classpath pattern, is:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 --no-parallel --console=plain :common:writeCheckClasspath
if ($LASTEXITCODE -ne 0) { throw 'A01 check classpath preparation failed.' }
$classpath = [IO.File]::ReadAllText((Resolve-Path .\common\build\checks\runtime-classpath.txt)).Trim()
$out = Join-Path (Get-Location).Path 'build\checks-a01'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$utf8 = New-Object Text.UTF8Encoding($false)
$compileArgs = Join-Path $out 'compile.txt'
$runArgs = Join-Path $out 'run.txt'
[IO.File]::WriteAllText($compileArgs, ('-cp "' + $classpath.Replace('\', '/') + '"'), $utf8)
[IO.File]::WriteAllText($runArgs, ('-cp "' + ("$out;$classpath").Replace('\', '/') + '"'), $utf8)
& (Join-Path $env:JAVA_HOME 'bin\javac.exe') -J-Xmx1G -J-XX:ActiveProcessorCount=2 `
  -nowarn -encoding UTF-8 "@$compileArgs" -d $out .\tools\checks\VerifyExtractor.java
if ($LASTEXITCODE -ne 0) { throw 'A01 check compilation failed.' }
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -Xmx1G -XX:ActiveProcessorCount=2 `
  "@$runArgs" io.github.ezequiel24123z.grindless.machine.VerifyExtractor
if ($LASTEXITCODE -ne 0) { throw 'A01 focused checks failed.' }
```

Do not change a shared worktree/cache while another window owns it. A full
`build` remains a separate required integration check. Reuse unchanged
seed/richness/yield derivation and `VeinData` storage arithmetic, not proof of
the extractor's new completion call sites. Recovery, live tick ordering and
one-time extraction accounting still require the native oracles below.

After the coordinator stages the exact integrated source in a separately owned PC
worktree and grants the execution window, use the existing direct harness with
explicit diagnostic retention. The coordinator supplies `$Root` as the absolute,
source-matched owned worktree with normal ancestors, never an unrelated current
directory or the default private inventory. Do not race cleanup to copy a live file:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
$env:SMOKE_DATAPACK = Join-Path $Root 'tools\smoke\extractor-backpressure-pack'
$env:SMOKE_COMMANDS = Join-Path $Root 'tools\smoke\extractor-backpressure.commands'
$env:SMOKE_EXPECT_FILE = Join-Path $Root 'tools\smoke\extractor-backpressure.expect'
$previousKeepWorld = $env:SMOKE_KEEP_WORLD
try {
    $env:SMOKE_KEEP_WORLD = '1'
    powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root 'tools\smoke-boot.ps1') -Root $Root
    if ($LASTEXITCODE -ne 0) { throw 'A01 native scenario/shutdown failed.' }
} finally {
    $env:SMOKE_KEEP_WORLD = $previousKeepWorld
}
```

Use the harness's ordinary assertion budget. The scenario lasts 65 world ticks
after its bounded loaded-chunk barrier and needs no extended timeout, optional
pack, new dependency or production diagnostic flag. Preserve the actual failing
baseline log and exact source; do not repeat the already executed baseline.
Source review is not a RED run. Require harness exit0 and natural server exit0 before the
postcheck: a retained-world banner after failure/forced stop is not evidence.
VeinData uses Luna's own ServerLevel storage, not the Overworld's root data.
Then use the same compiled check/classpath:

```powershell
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -Xmx1G -XX:ActiveProcessorCount=2 `
  "@$runArgs" io.github.ezequiel24123z.grindless.machine.VerifyExtractor --a01-vein-data `
  (Join-Path $Root 'forge\run\smoke-server\smoke-world\dimensions\grindless\luna\data\grindless_veins.dat')
if ($LASTEXITCODE -ne 0) { throw 'A01 native extraction accounting failed.' }
```

The argument file is the exact one prepared above, not a new required script.
The compressed file has root compound `data`; `data.Extracted` entries hold
long `Chunk`/`Amount`, and `DerivationVersion` is the version key. There is no
persisted `ExtractionCount` key. Preserve the actual file/log/receipt, then
explicitly remove only this verified owned diagnostic world. Another boot resets
it, so complete accounting first. This is not permission to restart or mutate a
player's world. Restore the process-scoped settings; never remove a worktree,
instance or parent directory as cleanup.
