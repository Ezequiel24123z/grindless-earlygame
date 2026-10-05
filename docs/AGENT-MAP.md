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
  Voltaic-gated crafting JSON (`grindless:gated_shaped`).
- `recipe/BootstrapRecipes.java` — T0 shaped JSON.
- `research/Blueprint.java` and `research/ResearchLogic.java` — a new blueprint id.
  Assembler recipes name it as a string (`industrial` today).

`tools/checks/VerifyRecipes.java` pins the generated graph size (115) and the gated
craft count (30); `VerifyAtlas` pins the same size and the assembler row count (47).
Change every one of those numbers in the same commit as the graph. Recipe ids stay
unique. Every T2-and-above machine is an Assembler row in `ProcessGraph` and must not
gain a file under `data/grindless/recipes/`.
Ratios are in [`PROCESSES.md`](PROCESSES.md#the-recipe-graph).

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

## Quests and the guide

- `quest/QuestCatalogue.java` and `quest/GuideCatalogue.java` — the lines and the pages.
- `quest/QuestLogic.java`, `quest/QuestEvidence.java`, `quest/QuestProgress.java` — what
  a claim watches, and per-player progress.

`VerifyQuest` holds that a claim never consumes its evidence and never gates a machine
([ADR-0100](DECISIONS.md#adr-0100--the-quest-book-tracks-the-route-and-does-not-gate-it)).

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
- `tools/smoke/` and `tools/smoke-boot.sh` when a block, a menu or a boot-visible
  item changes.

A new graph row updates the size assertion in `VerifyRecipes`. A new block updates
`BlockCatalogue`, which `VerifyAssets` reads. Arithmetic policy stays in a check
that does not boot Minecraft, the way `ProcessGraph`, `FluidLogic` and `AtlasLogic`
already do.
