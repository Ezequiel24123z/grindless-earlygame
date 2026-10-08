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

## Machine

- `recipe/MachineFamily.java` — the family a recipe names, not a block.
- `machine/ProcessMachineKind.java` — envelope, held conditions, block-entity type.
- `menu/MachineMenuKind.java` — slot counts on the shared menu.
- `menu/ProcessMachineMenu.java` and `machine/ProcessMachineBlockEntity.java` — visible slot
  placement and persisted inventory layout; widening a menu must migrate any shifted saved slots.
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
  ordinary shaped crafting JSON, including the four-unit first Relay Matrix batch and its one-Matrix
  T1 processing-machine costs.
- `recipe/BootstrapRecipes.java` — T0 shaped JSON.
- `research/ResearchLogic.java` and `machine/ResearchTerminalBlockEntity.java` — one physical
  Data Core calibration cycle. It must never write or read a global permission.

`tools/checks/VerifyRecipes.java` pins the generated graph size (111) and the ordinary
craft count (31); `VerifyAtlas` pins 111 generated rows, 40 Assembler rows and 143 total
handheld routes (including 31 shaped crafts and the physical calibration).
Change every one of those numbers in the same commit as the graph. Recipe ids stay
unique. Every T2-and-above machine is an Assembler row in `ProcessGraph` and must not
gain a file under `data/grindless/recipes/`. The eight spatial prototype outputs have
neither a graph row nor a crafting JSON until their T10-T15 campaign frontiers.
Ratios are in [`PROCESSES.md`](PROCESSES.md#the-recipe-graph).

## Control Matrix and Flux rating

- `energy/FluxTier.java` — the aligned F0-F15 rates and display names.
- `matrix/MatrixArchitecture.java` — Relay through Causal frontier ranges and retrospective
  manufacturing support.
- `matrix/ControlMatrixSpec.java` — physical rating, substitution and validation.
- `item/ControlMatrixItem.java` — stack persistence, names and tooltips.
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

A process may emit a byproduct only when [`PROCESSES.md`](PROCESSES.md#byproducts-and-their-sinks)
already names a sink for it
([ADR-0036](DECISIONS.md#adr-0036--every-byproduct-must-have-a-named-sink)).
Deconstructing into Matter does not count. Sulfur dioxide's sink is the tank and
then the contact process; the Thermal Generator must not burn it.

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
- `quest/QuestLogic.java`, `quest/QuestEvidence.java`, `quest/QuestProgress.java` — what
  a claim watches, and per-player progress.

`VerifyQuest` holds that a claim never consumes its evidence and never gates a machine
([ADR-0100](DECISIONS.md#adr-0100--the-quest-book-tracks-the-route-and-does-not-gate-it)).
The Field Guide states the complete two-iron Bootstrap contract and points from its four-Matrix
exit to the renewable-glass line; it remains readable independently of quest claims.
The active route has twelve tasks in four lines, with the physical Relay Matrix between calibration
and the first Arc Furnace; it stops at electronic silicon and contains
no dimension evidence; spatial pages return only with their campaign tiers.

## Multiblock

There is no kernel. The Ground Array is a fixed shape, not one. Do not add a kernel
ahead of its slice. Shape, hatches and the Arc
Reactor are specified in
[`MACHINES.md`](MACHINES.md#multiblocks-shape-is-a-parameter),
[ADR-0022](DECISIONS.md#adr-0022--parametric-multiblocks-instead-of-fixed-schematics)
and [ADR-0067](DECISIONS.md#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier).
The schedule is slice AD in [`BUILD-OUT.md`](BUILD-OUT.md).

Slice F starts when a machine needs hatches or size. That machine is the Arc Reactor,
not the Arc Furnace.

## Checks

- `tools/checks/Verify*.java`, via `tools/run-checks.sh .` after `:common:build`.
- `tools/check-links.ps1` — relative links and the ADR index.
- `tools/smoke/scenarios.txt` — the scenario list, when a block, a menu or a
  boot-visible item changes. Run it with `tools/run-smokes.ps1` or `run-smokes.sh`.

Every tool here has a `.ps1` and a `.sh` twin, because the desk that validates a
branch runs Windows ([ADR-0106](DECISIONS.md#adr-0106--code-is-written-remotely-it-is-validated-on-the-windows-desk)).
A new scenario is a line in `scenarios.txt` and its `<name>.commands` / `<name>.expect`
files, never a step in `ci.yml`
([ADR-0105](DECISIONS.md#adr-0105--smoke-scenarios-are-a-list-sharded-across-parallel-ci-jobs)).

A new graph row updates the size assertion in `VerifyRecipes`. A new block updates
`BlockCatalogue`, which `VerifyAssets` reads. Arithmetic policy stays in a check
that does not boot Minecraft, the way `ProcessGraph`, `FluidLogic` and `AtlasLogic`
already do.

## Optional pack bridges

- `forge/.../jei/GrindlessJeiPlugin.java` — JEI entry point; generated recipes are read from
  `ProcessLookup`, never copied into a second recipe list.
- `forge/.../jei/GrindlessProcessCategory.java` — item/tag alternatives plus the non-native fluid
  conditions of a process. It must keep the live graph visible even when no Forge fluid exists.
- `forge/.../jei/GrindlessCalibrationCategory.java` — the Research Terminal's physical cycle,
  which is not a JSON recipe and must therefore be registered explicitly.
- `pack/curseforge/manifest.json` — exact launcher-managed dependency file ids.
- `tools/build-modpack.ps1` and `.sh` — build the local jar and stage it as a pack override; they
  must keep the manifest version equal to `pack_version` in `gradle.properties`.

JEI is optional at runtime: the standalone Grindless jar must load without it. The T0 client pack
does not include AE2; its T4 recipe/energy/material integration needs a complete frontier slice,
not an unmodified early-game dependency (ADR-0120).
