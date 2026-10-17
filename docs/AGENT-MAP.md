# Agent route map

A task index. Open the entry for the change you are making. The files are where the
behaviour lives; the line under each entry is the invariant that is easy to break.
Code remains the source of the functions. Do not copy bodies into this file.

Design stays in the [README](../README.md), [`MACHINES.md`](MACHINES.md),
[`PROCESSES.md`](PROCESSES.md), [`BUILD-OUT.md`](BUILD-OUT.md) and
[`DECISIONS.md`](DECISIONS.md). If a section here starts restating one of those,
replace it with a link.

The same commit that changes a seam updates this file
([ADR-0087](DECISIONS.md#adr-0087--a-task-indexed-route-map-updated-with-the-seam)).

Two rules apply to every entry. Versions are written only in `gradle.properties`.
`common/` must not reference Forge types: loader differences go through an
`@ExpectPlatform` stub (`energy/FuelPlatform`, `energy/FluxPlatform`,
`container/ItemPlatform`) with the implementation in `forge/`, or through a
capability event
([ADR-0045](DECISIONS.md#adr-0045--capabilities-are-attached-by-event-not-overridden-on-the-block-entity)).

Coordination follows [AGENTS](../AGENTS.md) and
[the durable plan](../.agent/work-plan.json). Astra owns design/final integration; workers
own bounded implementation/check execution with explicit files and input revisions.
Current C3 source/assets are preserved WIP, not accepted functionality or permission to start ports.

## Machine

- `recipe/MachineFamily.java` — the family a recipe names, not a block.
- `machine/ProcessMachineKind.java` — envelope, held conditions, block-entity type.
- `menu/MachineMenuKind.java` — slot counts on the shared menu.
- `menu/ProcessMachineMenu.java` and `machine/ProcessMachineBlockEntity.java` — visible slot
  placement and persisted inventory layout; widening a menu must migrate any shifted saved slots.
- `container/SlottedContainer.java` — indexed vanilla inventory NBT. Layout 3 keeps sparse slots;
  compact legacy lists retain their recorded order, not guessed historical positions (ADR-0125).
- `process/MachineEnvelopes.java` — when the machine holds a band from
  [`MACHINES.md`](MACHINES.md#machines-are-condition-envelopes).
- `registry/BlockCatalogue.java` — geometry and the asset list.

Paths are under `common/src/main/java/io/github/ezequiel24123z/grindless/`.
Registration is its own entry.

A recipe names a family ([ADR-0019](DECISIONS.md#adr-0019--design-machines-before-recipes)).
Out-of-band running costs time, never yield
([ADR-0040](DECISIONS.md#adr-0040--running-out-of-band-costs-time-never-yield)).
T2 and above are manufactured, never a crafting table
([ADR-0017](DECISIONS.md#adr-0017--machines-above-t1-are-manufactured-never-hand-crafted)).

## Recipe

- `recipe/ProcessGraph.java` — the generated graph (ore line, press, mill, contact,
  assembler).
- `recipe/ProcessLogic.java` and `recipe/FabricationLogic.java` — durations, FU/t,
  fluid ids.
- `recipe/ProcessLookup.java` — indexed lookup, rebuilt from that graph.
- `recipe/T1Recipes.java` and `common/src/main/resources/data/grindless/recipes/` —
  ordinary shaped crafting JSON, including the five-unit first Relay Matrix batch and its one-Matrix
  T1 processing-machine costs.
- `recipe/BootstrapRecipes.java` — T0 shaped JSON.
- `recipe/BootstrapProcesses.java` — the F0 ceramic routes, appended to `ProcessGraph`;
  quantities and recovery yields live here, never in a second JSON recipe graph.
- `research/ResearchLogic.java` and `machine/ResearchTerminalBlockEntity.java` — one physical
  Data Core calibration cycle. It must never write or read a global permission.

`tools/checks/VerifyRecipes.java` pins 121 generated rows for its four-material fixture and
26 T1 crafts; `VerifyBootstrap` covers 18 T0 shaped crafts and the ceramic Data Core.
`VerifyAtlas` pins 121 generated rows, 41 Assembler rows and 167 total handheld routes,
including both crafting catalogues, the shapeless core and physical calibration.
Actual runtime counts depend on installed material tags; nine workshop rows are unconditional.
Change every one of those numbers in the same commit as the graph. Recipe ids stay
unique. Every T2-and-above machine is an Assembler row in `ProcessGraph` and must not
gain a file under `data/grindless/recipes/`. The eight spatial prototype outputs have
neither a graph row nor a crafting JSON until their T10-T15 campaign frontiers.
Ratios are in [`PROCESSES.md`](PROCESSES.md#the-recipe-graph).

`recipe/AtlasLogic.java` decorates explicit physical ratings for display and rated lookup only.
Keep `IngredientSpec.qualified()` and `OutputSpec.qualified()` as unmodified graph identities;
unrated item lookup must still find the F2 route (ADR-0124).

## C3 sources, primitive assembly and generation

- `source/SourceLogic.java` / `source/SourceSite.java` — source costs, environmental eligibility
  and site faults. Declare environmental inputs; do not relabel finite veins renewable.
- `recipe/RenewableProcesses.java` — aggregate/biomass and downstream F0 routes.
- `recipe/BootstrapAssembly.java` — Bench/Assembler projections of existing craft quantities.
  The current Bench covers Bootstrap/four vanilla crafts, not necessarily the Relay Matrix
  in `T1Recipes`; physical exit automation requires proof.
- `item/CrankHandleItem.java` — bounded bootstrap/emergency F0 work, not perpetual manual power.
- `machine/GeneratorKind.java` and the thermal generator implementation — shared F0/F1
  behavior; biomass registration follows supplier availability. Probe-before-drain alone does
  not guarantee exact extraction or safe handling of partial drains.
- Process kinds/block entities/menus/screens and registries — new source faults/enum cases
  must reach Forge, JEI, client effects, names and tooltips. Output slots append from two to
  five without moving old indexes; ten menu data fields include site faults.

Review global per-tick `heldConditions` before certifying the intended Firing regulation and
1450 C ceiling. Preserve `WorkRecipe`, `ItemLayout=3`, output reservations and single work
execution per tick. Recalculate the pre-C3 graph/Atlas/craft counts above and `ironBudget`
two-versus-three from source, not by weakening assertions.
Cancelled asset-generator changes and generated/state-fixture outputs exist without final QC;
review them before regeneration or acceptance.

## Control Matrix and Flux rating

- `energy/FluxTier.java` — the aligned F0-F15 rates and display names.
- `matrix/MatrixArchitecture.java` — Relay through Causal frontier ranges and retrospective
  manufacturing support.
- `matrix/ControlMatrixSpec.java` — physical rating, substitution and validation.
- `item/ControlMatrixItem.java` — stack persistence, names and tooltips.
- `matrix/ControlMatrixNbt.java` — the unchanged `Rating` key, legacy default and lossless numeric
  validation. `VerifyPersistence` exercises binary NBT and packet round-trips; `matrix-lifecycle`
  exercises the actual registered item and Assembler in Forge.
- `registry/ModItems.java`, `registry/BlockCatalogue.java` and `tools/assetgen/` — registration
  and generated functional art.

There is no T0 Control Matrix. Matrix rating Tn uses the same ordinal as Flux Fn; a higher
physical rating may substitute for a lower one, but an architecture cannot carry a rating above
its frontier range. A missing rating tag means that architecture's first frontier for vanilla
recipe compatibility; malformed explicit data is invalid rather than silently upgraded.
`VerifyEnergy` pins all sixteen rates and `VerifyControlMatrix` pins architecture and substitution
policy ([ADR-0107](DECISIONS.md#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)).

## Fluid

- `fluid/FluidState.java` and `fluid/FluidLogic.java` — volume, temperature, pressure.
  Ids are strings, not a Forge fluid type.
- `fluid/FluidBuffer.java` and `container/FluidInsert.java`.
- `forge/.../fluid/forge/MachineFluidCapability.java` — the Forge `FLUID_HANDLER` map.

`tools/checks/VerifyFluid.java` requires the ids in `FluidLogic` and `ProcessLogic`
to be the same strings. Ambient gases do not travel the Clay Conduit; a tank holds
them. The catalogue is [`PROCESSES.md`](PROCESSES.md#the-fluid-catalogue).

## Byproduct sink

- Vented fluids are `OutputSpec.ventedFluid` calls in `ProcessGraph`.
- Capture is `ProcessMachineBlockEntity`.
- The Thermal Generator burns only `FluidLogic.CARBON_MONOXIDE`
  (`machine/ThermalGeneratorBlockEntity.java`).

ADR-0131 supersedes ADR-0036's universal consumer requirement with honest classification and
handling. Current consumers must exist; future plans differ from uncertain potential.
Inert waste may be discarded, hazards need real handling and losses/emissions need not be
items. Matter deconstruction is not proof of recursive renewable supply.
Sulfur dioxide belongs in the tank/contact route, not generator fuel.

## Registration

- `Grindless.init` registers blocks, items, block entities, menus, recipe serializers,
  sounds and the creative tab, in that order.
- `registry/ModBlocks.java` registers the block and its block item together.
- `registry/ModBlockEntities.java` — one type per block that ticks.
- `registry/ModItems.java` and `material/SupplyCatalogue.java` — supplied forms.
- `assets/grindless/lang/en_us.json`.
- Assets are generated from `BlockCatalogue` by `tools/generate-assets.sh`
  ([ADR-0048](DECISIONS.md#adr-0048--assets-are-generated-from-the-jdk-with-a-named-list-of-what-cannot-be)).

`VerifyAssets` fails when `BlockCatalogue` and `ModBlocks` disagree. A change to
registration, a block entity or loader code also runs `tools/smoke-boot.sh`
([ADR-0049](DECISIONS.md#adr-0049--a-green-build-must-include-a-booted-server)).

## Worn equipment

- `item/HarnessLogic.java` and `item/ExosuitLogic.java` — the suit grids: how many
  slots a chassis has and which modules fit it.
- `item/HarnessItem.java`, `item/ExosuitItem.java`, `item/SuitModuleItem.java`,
  `item/FluxCellItem.java` — the pieces, the modules and the buffer.

`VerifyHarness` and `VerifyExosuit` hold the grid sizes. A chassis never generates FU
([ADR-0102](DECISIONS.md#adr-0102--the-voltaic-harness-has-one-slot-and-no-generator),
[ADR-0103](DECISIONS.md#adr-0103--the-exosuit-taps-a-pylon-and-does-not-generate)); the
Network Tap draws from pylon coverage and the drill's cell is not the suit's cell.

## Worlds and travel

- `structure/GroundArray*.java` — the fixed 3×3 ring. Not the multiblock kernel.
- `planet/`, `star/`, `station/`, `centre/` — Luna, the Drift, the station ride and the
  sealed chamber. A `*Catalogue` names the destination, a `*Travel` moves the player and
  a `*ReturnData` remembers where they left from.
- `flight/` — the launch pad and the survey rocket.

`VerifyPlanet`, `VerifyStarward`, `VerifyStation`, `VerifyRocket` and `VerifyCentre` hold
the FU each departure draws and that no return draws again. A link block that no longer
moves a player stays registered rather than being deleted
([ADR-0098](DECISIONS.md#adr-0098--the-supraluminal-station-is-the-ride-off-the-star)).
The first four checks also pin that the prototype transport has no generated survival recipe.

## Quests and the guide

- `quest/QuestCatalogue.java` and `quest/GuideCatalogue.java` — the lines and the pages.
- `client/FieldGuideScreen.java` — wraps each topic into physical pages; `GuideCatalogue.paginate`
  must retain every wrapped line rather than silently clipping a long topic or translation.
- `quest/QuestLogic.java`, `quest/QuestEvidence.java`, `quest/QuestProgress.java` — what
  a claim watches, and per-player progress. `menu/QuestBookMenu.java` synchronizes each task's
  live item count alongside its claim status for the client screen.
- `quest/QuestInventoryEvidence.java` — counts each inventory slot once; vanilla's inventory
  already includes the offhand. Matrix evidence checks both the item and exact valid rating.
- `recipe/IngredientSpec.java` and `recipe/OutputSpec.java` — process inputs and outputs; their
  optional Matrix rating makes the T1-to-T2 hand-off physical rather than a global unlock.

`VerifyQuest` holds that a claim never consumes its evidence and never gates a machine
([ADR-0100](DECISIONS.md#adr-0100--the-quest-book-tracks-the-route-and-does-not-gate-it)).
The Field Guide states the ceramic-workshop Bootstrap contract and points from its five-Matrix
exit through the complete Voltaic factory; it remains readable independently of quest claims.
The Dynamo/Extractor still spend two iron; optional vanilla equipment is outside that budget.
The active route has twenty-five tasks in eleven lines, with ceramics before calibration and the physical Relay Matrix between
calibration and the first powered machines; it ends at the F2 Relay Matrix, contains no dimension
evidence and does not advertise spatial campaign prototypes.
Early quest rewards must never supply a core, insulator or Matrix that bypasses the workshop.

## Multiblock

There is no parametric kernel. The Ground Array is a fixed shape, not one. Do not add a kernel
ahead of its slice. Shape, hatches and the Arc
Reactor are specified in
[`MACHINES.md`](MACHINES.md#multiblocks-shape-is-a-parameter),
[ADR-0022](DECISIONS.md#adr-0022--parametric-multiblocks-instead-of-fixed-schematics)
and [ADR-0067](DECISIONS.md#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier).
The schedule is slice AD in [`BUILD-OUT.md`](BUILD-OUT.md).

Slice F starts when a machine needs hatches or size. That machine is the Arc Reactor,
not the Arc Furnace.

ADR-0129 adds fixed, process-driven T0 workshop structures independently of that future kernel.
`structure/WorkshopStructure.java` owns local cells and clockwise rotations; the process block
entity reads only loaded world cells before working. The Firing Chamber is its controller plus
seven bricks in a 2x2x2 volume. Never persist a trusted "formed" flag or force-load a missing cell.
The Settling Trough requires a same-facing channel then screen behind its controller, with air
above those two parts. `WorkshopPartBlock` has facing but no artificial status or block entity.
`WorkshopGeometry` is the shared box source for collision, JSON models and the schematic preview.
The controller top remains usable by hoppers. Missing, wrong-facing, covered and unloaded cells
have separate synchronized faults; every fault enum addition appends to existing values.

`BlockCatalogue.Entry.hasStatus()` is false for a single visual variant. The generator and
state-smoke keys must omit the nonexistent property rather than create a one-value Minecraft
enum. `VerifyT0Preparation` checks the static parts, exact texture-end continuity and the
idle/active preview in addition to the ordinary controller models.

`ProcessMachineBlockEntity` retains paid work under `WorkRecipe`, separately from the live
`Recipe` selection (which is empty when inputs are invalid). Legacy saves derive the owner from
`Recipe`. A different recipe resets work; quantity changes, starvation and structure repair do not.
Outputs are reserved before any ingredient is consumed. Fluid arrival wakes work; pending output
has its own bounded export subscription, including after a neighboring inventory is emptied.
F0 kinds pass their rating into the base constructor before buffer creation, rather than reading
an uninitialized subclass field. Existing menu ordinals and `ItemLayout=3` stay unchanged.
`FluidOutputQuota` records only completed non-vented output. A front-side subscription exports
that quota without draining a ready recipe's primary batch. Consumption/extraction debits it;
missing legacy ownership stays unmarked, and invalid explicit ownership is logged, never used
to drain input. `FluidEndpoint.acceptedAmount` is shared by execution and Forge simulation.
Process work runs at most once per world tick despite subscription changes (ADR-0132).

## Sustainability export and dashboard ownership

The four static runtime files under `tools/dashboard/` implement the renderer only.
See [its schema-1 contract](dashboard/README.md): adjacent `data.json`, no synthetic fallback,
canonical labels and explicit source-bound evidence. Fixtures are not progression evidence.

Astra owns canonical schema/annotations. A bounded worker extracts real `ProcessGraph`,
`SupplyCatalogue`, local tags, `BootstrapRecipes`, `T1Recipes`, other authored JSON and quests
from an agreed pinned revision. Existing catalogues retain quantity authority; serialize shared
metadata/catalogue edits. Full inventory/exporter, recursive auditor, real data, CI reports/
artifacts and production serving remain incomplete; this section names ownership, not existing
implementations. Player communication and dashboard views must project the same model.
The [ADR-0135](DECISIONS.md#adr-0135--sustainability-is-a-fixed-point-audit-over-live-authorities-and-typed-annotations)
S1 core is implemented and verified on `639881f`: loader-free `sustainability` model/auditor,
`AuthorityAdapters`, `TagResolver`, `ConstructionReserve`, `DebtManifest` and `AuditReports`.
`VerifySustainability` and `VerifySustainabilityAdapters` cover its proof/report seams;
`tools/sustainability/debt.json` has no invented live exceptions. Generated
`build/sustainability/` reports remain report-only until the live annotations and gate land.
Circulating finite inventory is not unlimited net supply. Construction and hazardous handling
share one exact reserve witness; unresolved tag precedence stays unknown, never a sorted-hash
guess. Source identities must cover normalized authorities and the executed implementation.
S2 annotations, canonical pack resolution and live enforcement remain pending. Follow the
current validation ledger; a core test pass is not a renewable-supply certification.

The functional-port follow-up starts after current integration with an audit of Firing/Trough/
Ground Array and all authored structures. It does not authorize starting the old kernel slice
early. Astra decides roles, positions, ownership, aggregated capabilities and migration before
workers implement actual buses/hatches and transaction/chunk/reform tests. AE2 is optional and
version/API-verified, never a fictional or mandatory core integration.

## Checks

- `tools/validate.ps1` / `.sh` — the fast environment/build/behavior/documentation entry point;
  opt into full smokes explicitly. Per-stage results live under `build/validation/<run-id>/`.
  See [the testing strategy](testing/test-strategy.md).
- `tools/checks/Verify*.java`, via `tools/run-checks.sh .` after `:common:build`.
- `tools/checks/VerifySmokeHarness.java` — executes the native runner with fake boots in a
  temporary fixture. Report labels, actual dispatch and failure propagation must agree; Bash
  report variables must remain local (ADR-0127).
  `-Only` / `--only` accepts comma-separated names through the actual CLI; unknown or empty
  selections fail before any boot, never silently shrinking or expanding the requested scope.
- `tools/checks/VerifyT0Preparation.java` — verifies the active
  `docs/tiers/t0-production-graph.json` references and topology, plus the original textures/models
  named by `docs/assets/t0-asset-manifest.json`. These are audit projections, never new gameplay
  sources of truth or proof of a balanced economy.
- `tools/checks/VerifyWorkshop.java` — exact F0 counts, non-amplifying ceramic solid potential,
  reachable sinks and every rotated/missing chamber cell. `workshop` is the registered Forge
  counterpart for full cycles, finite-water wakeup, NBT reconstruction and blocked-output recovery.
  `settling` proves actual separation in four orientations, wrong-facing/covered suspension,
  reconstructed fluid/work and exact conduit recovery. Scheduled checks wait boundedly for
  observed work rather than equating world age with block-entity processing ticks.
- `tools/check-links.ps1` — relative links and the ADR index.
- `tools/smoke/scenarios.txt` — the scenario list, when a block, a menu or a
  boot-visible item changes. Run it with `tools/run-smokes.ps1` or `run-smokes.sh`.
- `forge/build.gradle` — `prepareSmokeServer` installs the pinned official server and stages the
  actual remapped mod jar plus Architectury. Boot scripts run that artifact, not development
  transformer classes; generated libraries/worlds stay under ignored `forge/run/smoke-server`.
  The installer checks its path/size inventory before accepting an up-to-date runtime.

Every tool here must have a `.ps1` and a `.sh` twin, because the desk that validates a
branch runs Windows ([ADR-0106](DECISIONS.md#adr-0106--code-is-written-remotely-it-is-validated-on-the-windows-desk)).
A new scenario is a line in `scenarios.txt` and its `<name>.commands` / `<name>.expect`
files, never a step in `ci.yml`
([ADR-0105](DECISIONS.md#adr-0105--smoke-scenarios-are-a-list-sharded-across-parallel-ci-jobs)).

The Planner item-existence scenario uses a chest stack, not a transient entity selector.
Keep its exact registered ID/count assertion; it does not exercise interactive planner use.

A new graph row updates the size assertion in `VerifyRecipes`. A new block updates
`BlockCatalogue`, which `VerifyAssets` reads. Arithmetic policy stays in a check
that does not boot Minecraft, the way `ProcessGraph`, `FluidLogic` and `AtlasLogic`
already do.

Both behavior runners ask `:common:writeCheckClasspath` for Gradle's resolved runtime classpath
and current classes. They honor `GRADLE_USER_HOME` without scanning unrelated caches or
silently swallowing long-path errors.

Smoke expectations may be scheduled functions; both boot runners wait at most
`SMOKE_EXPECT_TIMEOUT` (120 seconds by default). They fail on fatal scenario errors or an
unclean/nonzero shutdown, rather than reporting a forced cleanup as a pass. Smoke servers
bind only to loopback. The canonical production path exits cleanly; the optional
`:forge:runServer` development-JVM hang remains in the [validation ledger](testing/latest-validation.md)
and must not be reported as an upstream fix (ADR-0126).

## Factory World

- `factory/FactoryCatalogue.java` — the fixed-noon flat-floor contract.
- `factory/FactoryPortalBlock.java` and `FactoryTravel.java` — a reusable physical portal and
  player-specific round trip. It does not consume power or replace a campaign travel system.
- `factory/FactoryReturnData.java` — saved return endpoints, stored on the overworld.
- `data/grindless/dimension/factory.json`, its type and biome — bedrock, three dirt, one grass;
  no structures, terrain features, rain or hostile spawns.

The Factory World keeps the installed Grindless material pool through `PlanetCatalogue.veins`.
Its resource fields are surveyed/depleted ChunkVeins, not exposed ore terrain: clean construction
space must never become an infinite free resource. `VerifyFactory` holds the floor, noon, safety,
portal component and finite-field contract (ADR-0121).

## Optional pack bridges

- `forge/.../jei/GrindlessJeiPlugin.java` — JEI entry point; generated recipes are read from
  `ProcessLookup`, never copied into a second recipe list.
- `forge/.../jei/GrindlessProcessCategory.java` — item/tag alternatives plus the non-native fluid
  conditions of a process. Rated item inputs and outputs use `ProcessLookup.resolve`, so the
  viewer preserves their NBT rather than substituting an untagged stack. It must keep the live
  graph visible even when no Forge fluid exists.
- `forge/.../jei/GrindlessCalibrationCategory.java` — the Research Terminal's physical cycle,
  which is not a JSON recipe and must therefore be registered explicitly.
- `pack/curseforge/manifest.json` — exact launcher-managed dependency file ids.
- `tools/build-modpack.ps1` and `.sh` — build the local jar and stage it as a pack override; they
  must keep the manifest version equal to `pack_version` in `gradle.properties`.

JEI is optional at runtime: the standalone Grindless jar must load without it. The T0 client pack
does not include AE2; its T4 recipe/energy/material integration needs a complete frontier slice,
not an unmodified early-game dependency (ADR-0120).
