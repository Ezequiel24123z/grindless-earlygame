# T0 closure audit evidence

**Audit date:** 2026-10-19. **Source baseline:** `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`
(`Add the physical Voltaic Matrix exit`). Remote `main` matched this commit when checked.
The initial worktree was clean. This audit changes documentation, not gameplay.

Read the [action plan](T0-T1-ACTION-PLAN.md) for execution and the
[implementation queue](../plan/process-t0-t1-1.md) for independently assignable tasks.

## Evidence standard and limits

**Observed** means a command completed in this Windows worktree. **Inspected** means the
implementation demonstrates the behavior or defect, but the particular interaction was not
reproduced in a client. **Needs validation** is not a pass. Recommendations are not existing
features or accepted changes to balance.

This is a progression-led audit of the T0 trace and shared seams, with representative inspection
of later machinery, viewers, assets and tooling. It is not a claim to have executed every branch
of all 280 common Java files. No interactive client, listening session, pack import, survival
playthrough or full local historical smoke matrix was performed. The time-bounded review does not
certify multiplayer claims integration, every legacy save, every third-party material provider
or every later-tier prototype. No vulnerability/security certification is implied.

In citations below, `C` means `common\src\main\java\io\github\ezequiel24123z\grindless`,
and `F` means `forge\src\main\java\io\github\ezequiel24123z\grindless`. Line ranges refer to the
baseline, not to a future fix.

## Validation ledger

| Evidence | Result | What it does and does not establish |
| --- | --- | --- |
| `.\gradlew.bat --no-daemon --console=plain :common:compileJava build` | **Observed pass**, 5m 40s | Compilation and packaging; not client usability or survival reachability. |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .` | **Observed pass**, all 30 suites | Policies, fixtures, resource contracts and some source-text assertions. Not 30 runtime integration suites. |
| `powershell -ExecutionPolicy Bypass -File .\tools\check-links.ps1 -Root .` | **Observed baseline pass**, 373 links, 123 ADRs | Links/index consistency, not semantic agreement between documents. |
| Local `states` and `bootstrap` smokes | **Observed pass**, both scenarios, Windows | All state markers and seven Bootstrap markers matched. The Bootstrap test injects energy and near-complete progress; it is not a natural 30-second calibration or survival run. |
| [CI run 37966565170](https://github.com/Ezequiel24123z/grindless-earlygame/actions/runs/37966565170) | **Observed remote status:** completed, success, exact baseline SHA, 2026-10-09 | Historical Linux integration evidence. Not a new Windows/client acceptance run. |
| Client-pack import, JEI interaction, audio, survival timing | **Not run** | Assigned to the manual campaign, not inferred from the build. |

Build warnings included deprecated APIs, unchecked operations in `ModCreativeTabs`, removal
warnings for `ResourceLocation(String)` and `FMLJavaModLoadingContext.get()`, and Gradle 9
incompatibility warnings. The behavior runner emitted the documented SLF4J
`AbstractMethodError` in Extractor, Material and Network checks but returned success.
Treat that classpath noise as tooling debt; do not generalize it into permission to ignore
exceptions in actual server/client logs. Direct invocation of the smoke script was initially
blocked by Windows execution policy; the documented `-ExecutionPolicy Bypass` launcher was used.

## Actual scope and counts

The latest explicit T0 contract is ADR-0116 as amended by ADR-0122: ordinary vanilla-material
entry, two iron spent on Grindless machinery, one assisted extraction, physical F0 calibration,
then **five F1 Relay Matrices**. It is not an Assembler, a closed chemical plant or a multiblock.
The broader lattice's clay/carbon language is not an additional implemented T0 machine chain.

Without quest rewards, the authored mandatory crafts aggregate to **18 stone-crafting-material
items, 3 sticks, 7 planks, 3 glass, 3 redstone, 2 tagged copper ingots and 2 tagged iron ingots**.
This excludes the pre-existing crafting table, vanilla tools/furnace and their fuel. One glass
and one redstone belong to the Terminal/Data Core, not to the Matrix recipe. The two-iron
budget is not a claim that a fresh vanilla world can obtain redstone without earlier tools.

Evidence: `C\recipe\BootstrapRecipes.java:40-72`, `C\recipe\T1Recipes.java:49-55`,
`tools\checks\VerifyBootstrap.java:95-112`.

The passing fixtures currently pin **112 generated processes, 41 Assembler routes and 145
Atlas routes**, plus **32 T1 catalogue crafts** and **20 quests across nine lines**.
Graph counts are fixture/material-pool dependent, not universal pack constants.
The scenario list has **40 entries**, not the historical 39.
Evidence: `tools\checks\VerifyAtlas.java:21-29`, `VerifyRecipes.java:63,907`,
`VerifyQuest.java:55-80`, `C\quest\QuestCatalogue.java:48-90`, `tools\smoke\scenarios.txt`.

## Findings

### A01 - Output backpressure can strand the Crude Extractor

**P1, blocking T0; inspected, high confidence.**
`C\machine\CrudeExtractorBlockEntity.java:94-102,115-120,199-227` checks only the internal
slot when deciding to stop. It unsubscribes when the completed cycle cannot fit. `pushOutput()`
runs only after a successful `eject()`. Emptying an adjacent chest therefore does not drain the
full internal slot or restart that cycle. Even a neighbor notification only repeats the same
capacity check. A hopper pulling from the extractor can work around it; direct chest automation
must not require that workaround.

Reproduction to add: fill adjacent storage and the internal output, reach a completed blocked
cycle, free storage without breaking the extractor, and assert eventual transfer, exactly one
extraction-accounting increment per produced item, and no item loss. Repeat after save/reload.
Fix the output-drain/wake path, not the recipe rate or vein yield.

### A02 - Quest inventory evidence counts the offhand twice

**P2, fix before freezing the T0 guidance contract; inspected.**
`C\menu\QuestBookMenu.java:126-142` iterates the complete vanilla inventory, then adds
`getOffhandItem()` again. Vanilla's inventory container already includes the offhand.
Three Matrices in the offhand can therefore appear as six against a five-item objective.
This does not create a machine permission, but it makes live progress and rewards dishonest.

Count each inventory slot once. Test main hand, offhand, split stacks, armor slots, zero/four/five
Matrices, and one-time claim persistence. `VerifyQuest`'s catalogue/source-string assertions do
not exercise this actual inventory traversal.

### A03 - The shared process machine is not yet a safe frozen execution engine

**P1 before expanding T1 processing; inspected risks, targeted runtime reproductions required.**

| Seam | Evidence | Specific consequence and next experiment |
| --- | --- | --- |
| Recipe identity and progress | `C\machine\ProcessMachineBlockEntity.java:83-97,165-213,337-346,531-546` | Inventory changes clear the recipe cache but not progress. If a different valid recipe replaces it, no identity comparison resets the old work. Swap a nearly finished long recipe for a short one and assert that old work cannot finish the new recipe for free. Check save/reload too. |
| Fluid wake-up | Same file, `142-156,356-385,440-463` | The machine subscribes only when a complete recipe matches. Receiving fluid clears the cache without resubscribing; pulling is itself inside the work subscription. Test fluid arriving last after the energy buffer has filled, and primary/secondary tanks becoming available. |
| Output recovery | Same file, `146-153,216-270` | Output pushing is tied to completion, like the T0 extractor. Exercise a full output and then a drained chest. Do not duplicate this defect into another machine family. |
| Reload generation | `C\recipe\ProcessLookup.java:43-61`; process machine `337-346` | A tag reload replaces the graph but does not invalidate every machine's cached recipe. Test `/reload` with a changed provider/recipe and an active machine before defining a stable cache contract. |
| Power scalability | Process machine `114-128,509-517` | Every process machine currently reports F1 and menu values narrow energy to `int`. This is not a completed general F0-F15 execution/UI contract. Fix when a real higher-rated consumer requires it, not with a speculative rewrite. |

These are not reasons to build a second engine. Add focused runtime regressions, then make
small changes inside the existing engine. Broad framework extraction before those tests would
move defects rather than remove them.

### A04 - F2 exists, but an enforced T2 frontier does not

**T1/T2 acceptance blocker, not a missing T0 craft; inspected.**
`C\recipe\ProcessGraph.java:413-426` manufactures an F2-rated Matrix. Existing Industrial
machine recipes still use casings/coils/motors/plates without that Matrix
(`433-472,977-992,1017-1032`). Their `"industrial"` labels are metadata, not permissions.
An F2 quest is therefore not proof that all Industrial machines require F2.

There is also a policy mismatch: `C\matrix\ControlMatrixSpec.java:24-28` accepts higher
ratings, while `C\recipe\ProcessLookup.java:222-233` compares an ingredient's rating for
equality. Ordinary shaped Matrix ingredients only match item identity. Specify minimum-rating
consumer matching versus any deliberately exact frontier transformation before extending it,
and test the actual runtime matcher rather than only the pure spec.

### A05 - Discoverability loses information at the presentation boundary

**P1 when required instructions are hidden; static truncation confirmed, client impact unmeasured.**

- `C\client\ProcessAtlasScreen.java:78-80,125-138` cuts recipe lines and inputs to a fixed
  width. It has no full-detail tooltip, search or expandable condition section. `AtlasLogic`
  contains more data than the player necessarily sees. The long physical Matrix ingredient
  list is a priority client test.
- `C\recipe\AtlasLogic.java:68-75` adds `T1Recipes` and calibration, not
  `BootstrapRecipes`' four shaped crafts or the shapeless Data Core. The native Atlas's claim to
  explain every authored route is too broad; JEI's ordinary crafting coverage is a separate path.
- `C\client\FieldGuideScreen.java:65-76` stops drawing at a fixed bottom without scrolling
  within a page. Check each current page and GUI scale; do not claim a particular page is
  clipped without rendering it.
- `F\forge\jei\GrindlessProcessCategory.java:105-149` creates plain input stacks without
  consulting `matrixRating`, and keeps only the first three condition/detail lines. For processes
  with multiple fluids and conditions this drops later information, potentially time/energy.
  T0 calibration has its own category; do not mislabel every JEI page broken.
- Quest rows and Atlas rows are custom mouse-hit lists rather than focusable widgets.
  Keyboard/narration support and large-GUI usability are unproven. Web WCAG/mobile scores would
  be inappropriate evidence for this Minecraft UI.
- T0 machines expose world states/effects, but the Crude Extractor has no interaction menu and
  the Terminal uses insert/retrieve clicks. Test whether a new player discovers adjacency,
  cranking, empty-hand retrieval and blocked output using only the shipped pack.

### A06 - Documentation has material semantic drift

**P2; inspected.** Link validation does not catch this:

| Location at baseline | Drift |
| --- | --- |
| `docs\AGENT-MAP.md`, Recipe / Quests | 111/40/144 and nineteen/eight instead of 112/41/145 and twenty/nine; says the route ends at the Portal. |
| `README.md`, implementation row 32 | Calls the Matrix route pending although ADR-0123 and code have implemented the F2 output. Consumers remain unfinished. |
| `README.md`, historical prose under Where the project actually is | Still says 107/39, twelve tasks, and JEI waits. Those are snapshots, not the current frontier. |
| `docs\PROCESSES.md:544-585` | Old diagram gives four automated Matrices and one casing for the Assembler; current crafts use five Matrices and two casings. |
| `pack\README.md`, acceptance paragraph | Still calls the first batch four. |
| `docs\PROCESSES.md:1058` | B2 sink still describes crushed secondary rather than ADR-0110's ingot. |
| `docs\BUILD-OUT.md`, Do not start table | Still lists JEI as held despite ADR-0120. |

This audit supplies an authoritative baseline and corrects active entry points. The first queue
also requires reconciling the remaining historical prose before copying it into player guidance.
Do not rewrite historical ADR decisions as though the old design never existed.

### A07 - Art and audio have a working floor, not a final quality sign-off

**Needs validation/polish, not a blanket T0 blocker.** `VerifyAssets` verifies registry/catalogue
agreement, every facing/status model, loot/mining tags, display names, texture references and
mono Ogg sound events. `C\machine\MachineEffects.java:30-96` routes geometry/status effects;
`C\registry\ModSounds.java:24-36` registers five reusable events. The Relay Matrix sprite was
inspected; a tiny placeholder is not evidence of missing registration or of finished hero art.

The existing floor is reusable. It does not prove item silhouettes at inventory scale, front/back
recognition, color-independent fault recognition, texture contrast, loudness, repetitive clicks
or many-machine audio overlap. No listening assessment was made.
`tools\convert-audio.sh` has no PowerShell twin, and `tools\run-smokes-batch.ps1` has no shell twin.
Add those narrow portability fixes before relying on the pipelines across both desks.

ElevenLabs is an optional offline asset-production candidate, not an installed integration or
a runtime dependency. A future pipeline must record source/provider, generation parameters,
license/redistribution terms and notices, retain reviewed masters, normalize and convert to mono
Ogg, and check file/event parity. Generated audio does not replace human listening.
Do not upload repository material or introduce paid service calls in this audit.

### A08 - Configuration and future prototypes must not be oversold

**Non-blocking for the fixed-default T0 candidate; inspected.**
`C\GrindlessConfig.java:3-18` is a placeholder which logs defaults; there is no implemented
config-file surface for the advertised vein/pricing/fabrication/void controls. Do not tell pack
authors those knobs work. Introduce a real setting only with an explicit default, compatibility
policy and tests.

The Ground Array is a fixed ring, not a parametric multiblock kernel. Arc Reactor/hatches/drones
belong to T3, not to T0 closure or an immediate T1 framework project. Registered space objects are
compatibility/test infrastructure with removed survival recipes; they are not automatically dead
code. `UpgradeSet` explicitly contains future-policy placeholders. Marker searches found no
actionable TODO/FIXME backlog in the inspected runtime sources; absence of markers is not
absence of unfinished work.

### A09 - The displayed quest order can spend the seed factory's spare controller

**P2, T1 guidance risk; inspected, survival impact to validate.**
`C\quest\QuestCatalogue.java:64-81` lists surveyed extraction before the first factory and
allows the Terrestrial Extractor after Scanner/Thermal, without waiting for automated matrices.
`C\quest\GuideCatalogue.java:16-18` uses the same page order. The Terrestrial Extractor consumes
one Matrix, whereas all five seed Matrices are already allocated to Thermal/Pulverizer/Arc/Press/
Assembler. Following the visible list literally can require another manual batch before the
Assembler. This is not a hard recipe deadlock: defer the Terrestrial purchase until automated
replacement. Make that order explicit instead of increasing the batch again without play evidence.

## Readiness by required category

| Category | Scope and evidence-based disposition |
| --- | --- |
| **COMPLETE** | Authored T0 recipe contract and JSON checks; physical calibration policy; five-Matrix output; F0-F15 and matrix architecture policy; registry/asset floor; removal of global research permission. These narrow pieces need no redesign. |
| **NEEDS VALIDATION** | Natural survival route/time, repeated cranking, Terminal interaction, material-provider behavior in the actual pack, save/reload and relocation in live play, death/recovery, JEI/guide readability, visual/audio identity and multiplayer claims. |
| **NEEDS POLISH** | Full recipe/detail readability, keyboard/narration paths, stale descriptions, compact inventory art and audio variation. Anything that hides a required mechanic escalates to a blocker; bespoke final art does not. |
| **BLOCKING T0** | A01 direct-output recovery; honest guidance/evidence in A02; a recorded no-command playthrough and resolution of any mandatory-information failures from A05. No automated pass accepts these by proxy. |
| **NON-BLOCKING** | Final hero art/SFX/locales, a general config system, Atlas solver, late-tier kernel/prototypes, warning cleanup and exhaustive future-content refactors. A03/A04 block relevant T1 expansion, not the five-Matrix T0 endpoint. |

## Foundations worth preserving

Keep loader-independent common code and event-attached Forge capabilities, indexed graph
generation, physical ingredient/output specs, named condition faults, immutable material snapshots,
position-derived veins with saved extraction accounting, subscription-based ticking and
relocation's inventory-drop suppression. The inspected item capability bridge uses sided wrappers
and invalidates its `LazyOptional`s (`F\container\forge\MachineItemCapability.java:43-113`).
The Terminal preserves legacy stored output rather than turning old research flags into free items.

The important debt is at transitions: backpressure to runnable, missing fluid to supplied, recipe A
to recipe B, old graph to new graph, and server state to client explanation. Test and repair those
transitions before multiplying content. Preserve working abstractions; do not replace them with
a new generic framework, fixed multiblock DSL or recipe editor solely for elegance.
