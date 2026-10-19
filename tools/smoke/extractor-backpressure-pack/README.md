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
The runtime counterexample is frozen for later execution, not reported as a
Java/native RED pass. At that checkpoint, application source remains byte-for-byte
at the baseline.

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
owned item tags, Flux, the single output slot and extract-only policy. These are
authored checks, not executed Java evidence.

`VerifyExtractor --a01-vein-data <path>` reads the real cleanly stopped Luna
SavedData and requires exactly one extraction in each diagnostic chunk, exactly
two worked chunks and a total of two. It does not manufacture a production
counter or infer accounting from an item-only oracle.

| Evidence | Actual state | Scope |
| --- | --- | --- |
| Frozen pre-fix source | CONFIRMED | Six missing safeguards; not an executed drain/recovery failure |
| No-JVM repaired-source checks | PASS | Ten source safeguards; thirteen unchanged behavior bodies and the unchanged Container implementation |
| Native fixture structure | PASS | Nine functions, eleven resolved references, ten markers; executable fixture bytes unchanged from `4f1b30b` |
| Documentation and whitespace | PASS | 640 relative links, 154 ADR section/index pairs, no whitespace errors |
| Java baseline and repaired suite | NOT_RUN | Explicit coordinator reservation; includes 24 authored binary-NBT assertions, not 24 passes |
| Native recovery/world-time fixture | NOT_RUN | Distinct loaded extractor path, no process-machine proof substitution |
| Stopped-world SavedData oracle | NOT_RUN | Four authored exact accounting assertions |
| Common/Forge compilation, full build and deployment | NOT_RUN | No JVM, Gradle, server, client or PC mutation by this child |

Source checks do not execute Java, validate Minecraft command grammar or observe
the game's subscription/transfer ordering. The smallest new native uncertainty
is actual `SimpleContainer`/`ItemInsert` mutation plus first-tick reconstruction
and re-added work within the live world's tick. The existing process-machine
proof does not cover this class.

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
entities and storage checkpoints available for inspection.

Reconstruction exercises native block-entity `load` and first-tick subscriptions.
It is not an actual server restart, chunk unload/reload, crash-atomic persistence,
foreign capability guarantee or retained-player-world migration.

## Execution boundary

This fixture owns only its uniquely named commands/expect/pack. It intentionally
does not edit `scenarios.txt`, the shared harness or CI; their owner/coordinator
selects integration. Before an explicit immutable-PC lease, all Java checks,
compilation, build, native execution, packaging and deployment remain **NOT_RUN**.
No `pc-workflow` command or default private inventory is used by this child.

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

After the coordinator stages the exact frozen source in a separately owned PC
worktree and grants the execution window, the existing direct harness is enough:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
$env:SMOKE_DATAPACK = (Resolve-Path .\tools\smoke\extractor-backpressure-pack).Path
$env:SMOKE_COMMANDS = (Resolve-Path .\tools\smoke\extractor-backpressure.commands).Path
$env:SMOKE_EXPECT_FILE = (Resolve-Path .\tools\smoke\extractor-backpressure.expect).Path
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\smoke-boot.ps1 -Root .
```

Use the harness's ordinary assertion budget. The scenario lasts 65 world ticks
after its bounded loaded-chunk barrier and needs no extended timeout, optional
pack, new dependency or production diagnostic flag. Preserve the actual failing
baseline log and exact source before running the repaired source; source review
is not a RED run. After natural shutdown, use the same compiled check/classpath:

```powershell
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -Xmx1G -XX:ActiveProcessorCount=2 `
  "@$runArgs" io.github.ezequiel24123z.grindless.machine.VerifyExtractor --a01-vein-data `
  .\forge\run\smoke-server\smoke-world\dimensions\grindless\luna\data\grindless_veins.dat
if ($LASTEXITCODE -ne 0) { throw 'A01 native extraction accounting failed.' }
```

The argument file is the exact one prepared above, not a new required script.
A second ordinary smoke boot resets its diagnostic world, so retain the first
stopped-world accounting before another run. This is not permission to restart
or mutate a player's world.
