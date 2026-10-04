# Agent route map

A task index. Open the entry for the change you are making. The files are where the
behaviour lives; the line under each entry is the invariant that is easy to break.
Code remains the source of the functions. Do not copy bodies into this file.

Design stays in the [README](../README.md), [`MACHINES.md`](MACHINES.md),
[`PROCESSES.md`](PROCESSES.md), [`BUILD-OUT.md`](BUILD-OUT.md) and
[`DECISIONS.md`](DECISIONS.md). If a section here starts restating one of those,
replace it with a link.

The same commit that changes a seam updates this file
([ADR-0076](DECISIONS.md#adr-0076--a-task-indexed-route-map-updated-with-the-seam)).

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

`tools/checks/VerifyRecipes.java` pins the generated graph size (39) and the gated
craft count (30). Change either number in the same commit as the graph. Recipe ids
stay unique. Pylon MK2, the Wire Mill, the motor and the Chemical Reactor are
Assembler rows in `ProcessGraph` and must not gain a file under `data/grindless/recipes/`.
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

## Multiblock

There is no kernel. Do not add one ahead of its slice. Shape, hatches and the Arc
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
