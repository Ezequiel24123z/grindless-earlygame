Pending native idle-cache reload regression (ADR-0153)

Status: AUTHORED OFFLINE / NOT_RUN. Not in scenarios.txt. No JVM, Gradle,
server/client, PC execution, application fix, deployment or player save is part
of this checkpoint. Source parent: ab8490545d2796fcfb79981cb2bb2bd9172f7113.
The separate real-tag Java red fixture is frozen at
3643c6a049e3beb39b28fe23f4b6d8a1f316db30; its results are not this native proof.

The uncovered property
----------------------
A real Level-ticked Pulverizer, already holding valid input and a full energy
buffer but no matching generated row/work subscription, must wake after a real
tag publication without an inventory edit, neighbor update, energy delivery or
unload/reconstruction being the cause. One ordinary, unupgraded B1 batch must
consume one gravel, produce two sand and spend exactly 3,840 FU. The observer
also bounds paid work by elapsed game ticks and retains the result for 40 more
ticks without a second batch, extra output or unexplained Flux loss.

Reflection/pure Java checks can prove selection and subscription methods in
isolation, not the registered ticker -> tags/reload -> loaded Level boundary.
States-only placement never exercises this transition. Existing workshop,
Matrix, fluid, persistence and renewable-cycle proofs are reused unchanged;
this scenario does not repeat those matrices or test every generated material.

One controlled case, not new gameplay
------------------------------------
The enabled base pack replaces exactly these cache_probe item tags:
  forge:raw_materials/cache_probe = [minecraft:gravel]
  grindless:crushed_materials/cache_probe = [minecraft:sand]
  forge:ingots/cache_probe = []
The initially disabled sibling recipe-cache-reload-optional contains ONLY the
ingot override [minecraft:iron_ingot]. Enabling it LAST changes only that form;
it neither removes nor replaces the still-valid raw/crushed providers.
ProcessGraph generates dry b1/cache_probe only when the ingot form exists.

Three isolated marker armor stands witness actual item-tag membership through
ordinary predicates. For 40 consecutive observations, require both I/O tags,
no ingot member, Recipe="", WorkRecipe="", Progress=0, one gravel, no product
and unchanged full buffers. A competing existing recipe cannot satisfy this
baseline; after reload, actual paid work must name b1/cache_probe, not just emit
an otherwise plausible sand output.

The fixture then issues `datapack enable "file/recipe-cache-reload-optional" last`.
This command performs Minecraft's real asynchronous resource/tag reload. A
storage-gated minecraft:load callback acknowledges the changed tag set; it does
not reconstruct, touch or notify either machine. The scheduled base-pack
observer continues across reload and never assumes one console sleep means
reload completion. No production Java method is called by a test sidecar.
Phase2 counts publication observations only. Its400-tick deadline emits
CACHE-RELOAD-FAIL-PUBLICATION-TIMEOUT, never the native pre-fix wake marker.
Only phase3 with reload_seen=1 resets and spends the separate400-tick wake/
cycle allowance. A late publication cannot steal the normal120-tick batch's
allowance. Paid-work/time samples continue across the barrier without reset.

The command-built equipment is a Pulverizer at 8 280 8 with 1,280 FU and one
gravel, plus an adjacent Thermal Generator at 7 280 8 precharged with 6,400 FU,
with zero Burn/Duration and no fuel. There is no pylon, hopper, other inventory
or fluid supply. The buffer sizes come from MachineBlockEntity.bufferCapacity,
ThermalGeneratorBlockEntity.bufferSeconds and FluxTier.F1, not invented NBT.
The ordinary B1 needs 120 ticks at 32 FU/t, more than the consumer's 40-tick
buffer: pretending that 1,280 FU alone finishes the batch would be a false test.

While the consumer is full and unsubscribed, receive() accepts zero and invokes
no onChanged callback (SimpleFluxStorage). The donor cannot independently wake
the miss. Only after reload-triggered work spends its first 32 FU may natural
delivery refill it. Ordinary status publication uses UPDATE_CLIENTS, not a
neighbor notification. No command changes any block, machine NBT, input, output,
fuel, charge, Progress, conditions or config after setup. Tag witnesses never
touch the machines. The donor rejects sand as fuel, keeping the output visible.

Read-only sampling requires one inventory stack: either slot0 gravel x1 before
completion or slot1 sand x2 afterward. It checks successful NBT reads and:
  consumer_FU + donor_FU + Progress * 32 + completed_batches * 3840 = 7680
Here completed_batches can only be zero or one, derived from the real inventory.
It never defaults a failed read to zero. With unchanged optimal conditions and
no upgrades, the increase in paid work cannot exceed elapsed_game_ticks * 32.
Final stock is sand x2, zero input/progress and exactly 3,840 FU remaining.
The unused second-batch energy is intentional: an extra charge cannot hide
behind exhaustion. All stock/energy are explicit diagnostic setup, not a
Survival bootstrap, renewable-source claim or changed recipe balance.

Minimal authorized harness integration
-------------------------------------
At the source parent, the runner could copy only ONE initially enabled pack.
Fixture checkpoint 5bc4543bdf55754c12ade197bc67fbc876097b29 preserved that real
limitation. The coordinator then authorized the narrow extension under ADR-0154
(authored in the parent worktree; integrate that decision with this continuation).

Both smoke-boot twins now optionally accept ONE SMOKE_OPTIONAL_DATAPACK.
They preflight its directory/metadata, portable basename, reparse/symlink-free
tree and separation from the disposable world before Gradle/world replacement.
Both source and world paths reject linked ancestors before any physical-source
canonicalization can hide them. This includes a logical ROOT alias and a fresh
world whose suffix is not created yet; only expected missing path components
are tolerated, while unreadable metadata still fails. Rejecting these aliases,
rather than comparing a logical world against a physical input, prevents
cleanup from deleting an aliased source.
When present, also validate the primary; reject case-insensitive basename
collisions. Existing no-optional behavior is preserved. Copy the sibling next
to the primary and explicitly write initial-disabled-packs=file/<optional>.
Omission from initial-enabled-packs alone is NOT sufficient on 1.20.1.

Both isolated run-smokes twins discover tools/smoke/<name>-optional and scope
the new environment setting per scenario; the caller is preserved on success
and failure. The PS batch excludes recipe-cache-reload and every scenario with
an optional sibling, announces mixed exclusions and rejects isolated-only
selection. Its single boot clears/restores inherited optional state. There is
no run-smokes-batch.sh at the received source; another coordinator-owned child
is authoring that separate GREEN twin, not this fixture. Do not mistake Windows
Git Bash for a Linux run. CI and scenarios.txt remain untouched.
No watcher, RCON, arbitrary shell hook, extra mod or new framework is introduced.

Verified 1.20.1 source semantics (reference only, no imported code):
  https://github.com/themodderg/The-Digimod/tree/744e72e970ae33a77acd241221e917f6f3d5258a
  build.gradle selects net.neoforged:forge:1.20.1-47.1.79.
  Source root: build/tmp/.cache/expanded/zip_51348f0b239ab79c430d7ade20fd8068/
  net/minecraft/server/dedicated/DedicatedServerProperties.java:
    constructor/getDatapackConfig read BOTH initial-enabled/disabled-packs.
  net/minecraft/server/MinecraftServer.java: configurePackRepository excludes
    explicitly disabled IDs from its new-pack automatic-admission branch.
  net/minecraft/server/packs/repository/ServerPacksSource.java uses WORLD for
    the world folder, and PackSource.java sets WORLD.shouldAddAutomatically=true.
  net/minecraft/server/commands/DataPackCommand.java: enable last appends to the
    selected list and calls ReloadCommand.reloadPacks; permission level is 2.
This pinned public 1.20.1-derived source mirror is static behavior evidence,
not binary qualification of the selected Forge47.4.23 or another mod import.
Current Forge1.20.x source has newer APIs and is not substituted for1.20.1.

Smallest future execution, only after coordinator authorization
---------------------------------------------------------------
Integrate/publish the final application fix and this fixture/staging change,
then grant one exclusive diagnostic-server qualification window in a
separate exact-source PC worktree with fresh resource guards. Never use the
default private pc.json or a Prism/player save as a child execution surface.
Both base and optional packs must be present before boot; ONLY base is enabled.
Do not stage optional into a running world using an unreviewed file watcher.

The direct PS invocation from that isolated, exact-source root is:
  $env:SMOKE_DATAPACK=(Resolve-Path '.\tools\smoke\recipe-cache-reload').Path
  $env:SMOKE_OPTIONAL_DATAPACK=(Resolve-Path '.\tools\smoke\recipe-cache-reload-optional').Path
  $env:SMOKE_COMMANDS=(Resolve-Path '.\tools\smoke\recipe-cache-reload.commands').Path
  $env:SMOKE_EXPECT_FILE=(Resolve-Path '.\tools\smoke\recipe-cache-reload.expect').Path
  $env:SMOKE_LOG=Join-Path (Get-Location).Path ('forge\run\recipe-cache-reload-'+(Get-Date -Format 'yyyyMMddTHHmmssfff')+'.log')
  $env:SMOKE_TIMEOUT='900'
  $env:SMOKE_EXPECT_TIMEOUT='120'
  $env:SMOKE_PORT='25628'
  powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\smoke-boot.ps1 -Root .
EULA opt-in must come from existing coordinator/owner authorization; this
document is not consent. The corresponding .sh entry uses the identical four
SMOKE paths and the same expectation budget. Use only one native runner, not
both native boots merely to repeat this mechanism proof.

The existing runner first executes :forge:prepareSmokeServer; budget that
separately with the repository's current bounded Gradle settings and no
concurrent JVM owner. It then launches exactly ONE production Forge server at
the unchanged -Xmx2G (never shrink the canonical heap). Assertion budget is the
existing 120 seconds, startup 900, handshake 60, clean shutdown 60. The fixture
needs at most 201 chunk-wait ticks, 40 idle observations, 400 publication
observations, 400 confirmed work observations and 40 extra settled ticks:
under55 game seconds at20TPS. Each phase keeps its400-tick cap; the outer120s
assertion deadline and all server heap/startup/shutdown limits are unchanged.
Its own missing-pack, tag, read/balance and timeout failures are explicit.

Expected pre-fix result, NOT an executed result
----------------------------------------------
With both packs correctly staged and only the base enabled, ab8490545d2796f /
the unchanged application at frozen3643c6a should emit the idle and published-
ingot markers, then CACHE-RELOAD-FAIL-IDLE-WAKE-OR-CYCLE-TIMEOUT. The full
consumer cannot receive a donor callback; no neighbor or inventory mutation
rescues its empty work subscription. The wake, batch and final markers remain
absent and the existing expect runner must exit nonzero.
Missing staging instead produces CACHE-RELOAD-FAIL-OPTIONAL-PACK-NOT-STAGED-OR-NOT-DISABLED;
that is a harness/setup failure, not the intended red counterexample.
An unconfirmed publication instead produces CACHE-RELOAD-FAIL-PUBLICATION-TIMEOUT;
it is not evidence that the idle process missed a published graph.

Retain exact source/application-fix/fixture revisions, jar hash, command,
heap/budgets, raw native log, exit status and all five gated markers. Standard
cleanup removes the diagnostic world, so preserve useful failure evidence
before another run overwrites logs. Only after actual qualification add ONE
scenario-list line through the existing mechanism, never a duplicate CI step.
Minecraft function/predicate parsing, the real tag barrier, native donor
delivery and complete red/green remain NOT_RUN at this offline checkpoint.
The initial fixture's offline structure inspection passes ten JSON documents, 23 resolved function
calls, five unique marker emitters and setup-only world mutations. Expectations
require actual [Server] broadcasts, not marker text quoted in a parser error.
The initial8a433 PS optional-pack helper/dispatch check passed37 cases, including real
tree copies, collisions/invalid paths/reparse points, successive fake boots,
same-process environment restoration and batch exclusion. Its first attempt
failed in test-only nested -Command quoting; -EncodedCommand corrects that
failure, and the final check passes. No Minecraft process was involved.
An independent review then blocked8a433 on two P2 defects: conflated publication/
wake accounting and unchecked path ancestors. Two new offline checks separately
fail on that frozen source: "optional source through a junction ancestor" and
"publication wait cannot consume or masquerade as confirmed wake budget".
The repair passes59 focused PS checks, adding safe alias-to-world and ROOT-alias
cases with retained-source/world sentinels, separate budget guards and seven
in-memory source mutations. Junctions target only this check's own temporary
trees and are unlinked before cleanup; no external tree or server is deleted.
These are path/dispatch executions and source contracts, not a Minecraft
reload-scheduler execution. Native qualification is still required.
The paired SH check is authored but NOT_RUN: Bash is absent from PATH and four
bounded local Git candidates. VerifySmokeHarness calls these checks through its
existing suite, not a new CI step. The four focused VerifyEnergy assertions use
the actual SimpleFluxStorage type to cover full-buffer/no-callback and normal
post-withdrawal refill; JVM execution/compilation is NOT_RUN under the reservation.
The fixture checkpoint's standard documentation check passes639 links/153 ADRs.
No client interaction, genuine restart/crash recovery, saved-world migration,
human/T0 acceptance, freeze, release or T1 activation is implied.
