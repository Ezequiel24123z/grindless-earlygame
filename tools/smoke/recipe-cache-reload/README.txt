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

Concrete harness limit and minimal proposed integration
-------------------------------------------------------
At the source parent, smoke-boot.ps1 and .sh accept ONE SMOKE_DATAPACK, copy it
and enable it in initial-enabled-packs. There is no hook for staging a second
disabled pack. Nested directories inside a datapack are not another world pack.
An enable command cannot discover a pack that has never been copied.
Therefore this fixture is deliberately NOT runnable through stock run-smokes
yet, and it must not be registered or called a passing native regression.

Reported before any shared-runner change and subsequently authorized by the
coordinator under forthcoming ADR-0154; implementation is a separate checkpoint:
  1. Optionally accept ONE SMOKE_OPTIONAL_DATAPACK in both smoke-boot twins.
  2. Copy it alongside SMOKE_DATAPACK and explicitly list it in initial-disabled-
     packs, never initial-enabled-packs. Keep normal single-pack behavior, handshake, cleanup, heap and
     timeout semantics unchanged. Refuse ambiguous/colliding pack basenames.
  3. For eventual manifest use, run-smokes twins may discover
     tools/smoke/<name>-optional and scope/restore the new setting.
This approved proposal is not yet an implemented environment variable.
Shared runner code, CI and scenarios.txt are untouched at this fixture checkpoint.
No watcher, RCON, arbitrary shell hook, extra mod or new test framework is needed.

Smallest future execution, only after coordinator authorization
---------------------------------------------------------------
Integrate/publish the final application fix and this fixture, approve/qualify
the staging change, then grant one exclusive diagnostic-server window in a
separate exact-source PC worktree with fresh resource guards. Never use the
default private pc.json or a Prism/player save as a child execution surface.
Both base and optional packs must be present before boot; ONLY base is enabled.
Do not stage optional into a running world using an unreviewed file watcher.

After the PROPOSED staging contract actually exists, the direct PS invocation
from that isolated source root is:
  $env:SMOKE_DATAPACK=(Resolve-Path '.\tools\smoke\recipe-cache-reload').Path
  $env:SMOKE_OPTIONAL_DATAPACK=(Resolve-Path '.\tools\smoke\recipe-cache-reload-optional').Path
  $env:SMOKE_COMMANDS=(Resolve-Path '.\tools\smoke\recipe-cache-reload.commands').Path
  $env:SMOKE_EXPECT_FILE=(Resolve-Path '.\tools\smoke\recipe-cache-reload.expect').Path
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
needs at most 201 chunk-wait ticks, 40 idle observations, 400 work/reload
observations and 40 extra settled ticks, normally under 35 game seconds.
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

Retain exact source/application-fix/fixture revisions, jar hash, command,
heap/budgets, raw native log, exit status and all five gated markers. Standard
cleanup removes the diagnostic world, so preserve useful failure evidence
before another run overwrites logs. Only after actual qualification add ONE
scenario-list line through the existing mechanism, never a duplicate CI step.
Minecraft function/predicate parsing, the real tag barrier, native donor
delivery and complete red/green remain NOT_RUN at this offline checkpoint.
Offline structure inspection passes ten JSON documents, 23 resolved function
calls, five unique marker emitters and setup-only world mutations. The standard
documentation check passes 639 links/153 ADRs; whitespace also passes.
No client interaction, genuine restart/crash recovery, saved-world migration,
human/T0 acceptance, freeze, release or T1 activation is implied.
