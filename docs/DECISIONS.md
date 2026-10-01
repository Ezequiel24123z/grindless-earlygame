# Architecture decision records

Why Grindless is built the way it is. The README describes *what* the mod does; this file records
*why* each structural choice was made, and which alternatives were rejected.

Add a record whenever a decision would be non-obvious to someone reading the code cold. See
[`AGENTS.md`](../AGENTS.md) for when and how. Records are append-only: to reverse a decision, add a
new record that supersedes the old one and mark the old one `Superseded by ADR-xxxx`. Never edit
history — the reasoning that was wrong is itself useful information.

**Status values:** `Accepted` · `Superseded by ADR-xxxx` · `Proposed`

| # | Decision | Status |
| --- | --- | --- |
| [0001](#adr-0001--architectury-for-multiloader-support) | Architectury for multiloader support | Accepted |
| [0002](#adr-0002--no-neoforge-subproject-on-1201) | No `neoforge` subproject on 1.20.1 | Accepted |
| [0003](#adr-0003--mojang-mappings-layered-with-parchment) | Mojang mappings layered with Parchment | Accepted |
| [0004](#adr-0004--the-material-registry-is-built-from-tags-at-runtime) | Material registry built from tags at runtime | Accepted |
| [0005](#adr-0005--recipes-are-generated-at-runtime-not-shipped-as-json) | Recipes generated at runtime, not shipped as JSON | Accepted |
| [0006](#adr-0006--flux-units-convert-to-fe-at-11-over-a-hand-written-bridge) | Flux Units convert to FE at 1:1 | Accepted |
| [0007](#adr-0007--flux-network-state-lives-in-saveddata-with-a-spatial-index) | Network state in `SavedData` + spatial index | Accepted |
| [0008](#adr-0008--belt-contents-are-lane-data-not-entities) | Belt contents are lane data, not entities | Accepted |
| [0009](#adr-0009--ore-veins-are-derived-from-seed-and-coordinates) | Ore veins derived from seed and coordinates | Accepted |
| [0010](#adr-0010--replication-cost-is-derived-from-the-recipe-graph) | Replication cost derived from the recipe graph | Accepted |
| [0011](#adr-0011--remote-colonies-are-simulated-abstractly-never-force-loaded) | Remote colonies simulated abstractly | Accepted |
| [0012](#adr-0012--planets-come-from-installed-space-mods-and-satellites-have-no-upkeep) | Planets come from installed space mods | Accepted |
| [0013](#adr-0013--the-readme-is-the-design-source-of-truth) | The README is the design source of truth | Accepted |
| [0014](#adr-0014--consolidate-the-orphaned-session-branches-into-one-history) | Consolidate the orphaned session branches | Accepted |
| [0015](#adr-0015--fluids-are-modelled-as-state-not-as-items) | Fluids are modelled as state, not as items | Accepted |
| [0016](#adr-0016--pipes-early-the-phase-network-late) | Pipes early, the Phase Network late | Accepted |
| [0017](#adr-0017--machines-above-t1-are-manufactured-never-hand-crafted) | Machines above T1 are manufactured, never hand-crafted | Accepted |
| [0018](#adr-0018--one-container-contract-with-auto-void-off-by-default) | One container contract, auto-void off by default | Accepted |
| [0019](#adr-0019--design-machines-before-recipes) | Design machines before recipes | Accepted |
| [0020](#adr-0020--recipes-are-parameterised-by-process-conditions) | Recipes are parameterised by process conditions | Accepted |
| [0021](#adr-0021--complexity-beyond-gregtech-with-no-grind) | Complexity beyond GregTech, with no grind | Accepted |
| [0022](#adr-0022--parametric-multiblocks-instead-of-fixed-schematics) | Parametric multiblocks instead of fixed schematics | Accepted |
| [0023](#adr-0023--a-native-route-viewer-with-a-ratio-solver) | A native route viewer with a ratio solver | Accepted |
| [0024](#adr-0024--loomplatform-markers-and-the-windows-path-length-limit) | `loom.platform` markers and the Windows path limit | Accepted |

---

## ADR-0001 — Architectury for multiloader support

*2026-09-29 · Accepted*

**Context.** Grindless has to reach the largest possible modpack audience, which means Fabric,
Forge and NeoForge, across several Minecraft versions. Maintaining parallel codebases per loader
is the usual way such mods die.

**Decision.** Use Architectury with a `common/` module holding essentially all logic, and thin
`fabric/` and `forge/` modules containing only entrypoints and platform bridges. Platform
differences go through `@ExpectPlatform` stubs. Shadow bundles `common` into each platform jar.

**Alternatives rejected.** Separate per-loader repositories (duplicated logic); Forge-only (halves
the audience); a source-templating tool such as Preprocessor (worse IDE experience than a real
multi-module build).

**Consequences.** Ports to new Minecraft versions become mapping and registry churn rather than
rewrites. The cost is that `common/` must stay disciplined: one loader-specific import outside an
`@ExpectPlatform` stub breaks the other platform, usually at runtime rather than compile time.

---

## ADR-0002 — No `neoforge` subproject on 1.20.1

*2026-09-29 · Accepted*

**Context.** NeoForge is normally a distinct build target, so the obvious structure is
`common` + `fabric` + `forge` + `neoforge`. On 1.20.1 that is wrong.

**Decision.** Ship only `fabric` and `forge` on 1.20.1. The Forge jar is also the NeoForge
artifact.

Verified directly against `https://maven.neoforged.net/releases/net/neoforged/`: the modern
`net.neoforged:neoforge` coordinate begins at `20.2.x` — Minecraft 1.20.2 — and has nothing for
1.20.1. NeoForge's 1.20.1 release is `net.neoforged:forge:1.20.1-47.1.x`, a soft-fork of Forge 47
that keeps the `net.minecraftforge` package names and the `META-INF/mods.toml` metadata format.
One Forge-compiled binary therefore loads on both loaders unmodified.

**Consequences.** One less subproject to build, test and publish on 1.20.1. A real `neoforge`
module arrives with the 1.20.2+ port, where both the artifact and Architectury's `neoforge()`
target exist. The `mods.toml` loader range is kept at `[47,)` so the same file satisfies both.

---

## ADR-0003 — Mojang mappings layered with Parchment

*2026-09-29 · Accepted*

**Context.** 1.20.1 mods use either MCP/SRG-era mappings, Yarn, or official Mojang mappings.
A multiloader project needs one mapping set that works on both loaders.

**Decision.** Official Mojang mappings (`officialMojangMappings()`) layered with Parchment
`2023.09.03`, which adds parameter names and javadoc that Mojmap omits.

**Alternatives rejected.** Yarn is Fabric-oriented and awkward on Forge; MCP is legacy.

**Consequences.** Readable, permissively licensed names that are standard on 1.20.1, so code
matches what most 1.20.1 tutorials and other mods' sources show. `silentMojangMappingsLicense()`
is set in the root build to avoid the interactive licence prompt.

---

## ADR-0004 — The material registry is built from tags at runtime

*2026-09-29 · Accepted*

**Context.** The core promise is that Grindless processes *every* material in whatever pack it is
installed into, including mods it has never heard of. A hardcoded material list cannot do that,
and per-pack compat addons are the failure mode of every mod that has tried.

**Decision.** At runtime, read the tags actually present in the loaded pack — `forge:ores/*`,
`forge:ingots/*`, `forge:raw_materials/*`, `forge:dusts/*`, `forge:gems/*` and the Fabric `c:`
conventions — and build the material registry and the whole processing chain from what is found.
Rebuild on datapack reload so pack changes apply without a restart.

**Consequences.** New mods are supported with no patch release and no configuration. Materials
degrade gracefully: a material with an ore but no dust form simply skips the pulverizing step, and
where another mod already provides an item for a material, Grindless uses that item instead of
registering a duplicate. The cost is that nothing about the material set can be assumed at compile
time — every system downstream must be written against a registry discovered at runtime.

---

## ADR-0005 — Recipes are generated at runtime, not shipped as JSON

*2026-09-29 · Accepted*

**Context.** A consequence of ADR-0004: if materials are unknown until the pack loads, their
recipes cannot be authored in advance.

**Decision.** Generate the processing-chain recipes at runtime from the discovered material
registry.

**Consequences.** One implementation covers every mod, present and future, with no shipped recipe
JSON to maintain. Recipe viewers must be fed the generated recipes explicitly, and any bug in
generation affects every material at once rather than one file.

---

## ADR-0006 — Flux Units convert to FE at 1:1 over a hand-written bridge

*2026-09-29 · Accepted*

**Context.** Grindless has its own energy unit, Flux Units, but must interoperate with the pack's
existing energy economy — Forge Energy on Forge, TeamReborn Energy on Fabric. Architectury
provides no unified energy API, so the bridge has to be written by hand either way.

**Decision.** FU converts to FE at exactly 1:1, with a hand-written bridge per platform
(capabilities on Forge, TeamReborn Energy `5.0.0` on Fabric).

**Alternatives rejected.** A non-unit conversion ratio, which accumulates rounding drift across
millions of ticks and turns energy storage into a slow leak; adopting FE directly, which would
give up Grindless-specific tier semantics.

**Consequences.** Conversion is provably lossless and needs no rounding policy. Note that
TeamReborn Energy is hosted on `maven.fabricmc.net`, *not* on `maven.terraformersmc.com` — the
wrong repository here is a confusing build failure.

---

## ADR-0007 — Flux Network state lives in `SavedData` with a spatial index

*2026-09-29 · Accepted*

**Context.** The Flux Network is coverage-area power distribution: pylons project a supply area
and machines inside it draw power without cables. The naive implementation stores network
membership on block entities and walks the graph each tick.

**Decision.** Keep network state in a level-wide `SavedData`, indexed spatially over pylons.

**Consequences.** No per-tick graph walk, and the network survives chunk unloads intact —
block-entity-owned state would fragment the moment a chunk containing part of a network unloads.
Coverage lookup is `O(log n)` in the number of pylons rather than `O(n)` in blocks. The cost is
that `SavedData` and world blocks can drift out of sync, so pylon placement and removal must
maintain the index carefully.

---

## ADR-0008 — Belt contents are lane data, not entities

*2026-09-29 · Accepted*

**Context.** Belts are the headline logistics feature and the most likely source of tick lag at
scale.

**Decision.** Represent items on belts as positions in lane data structures, never as one
`ItemEntity` per item.

**Consequences.** Avoids the standard belt-mod performance disaster, where a few thousand items in
transit produce a few thousand ticking entities. Rendering must draw items from lane data
directly, and items must be materialised into real stacks only at belt endpoints.

---

## ADR-0009 — Ore veins are derived from seed and coordinates

*2026-09-29 · Accepted*

**Context.** Resource Genesis gives each chunk its own finite materials, which deplete and push
the player outward. Storing per-chunk vein contents for an effectively unbounded world is a
storage problem.

**Decision.** Derive vein composition deterministically from the world seed plus chunk
coordinates. Persist only what has actually been extracted.

**Consequences.** No stored world data until a vein is used, identical results across reloads, and
the cost of an unexplored chunk is zero. Changing the derivation function after release would
silently alter every unexploited chunk in every existing world, so it must be versioned.

---

## ADR-0010 — Replication cost is derived from the recipe graph

*2026-09-29 · Accepted*

**Context.** Matter Replication produces items that are not ores. Duplication mods that price
items with a flat cost are trivially exploitable: find the item whose crafting inputs are worth
less than its flat replication price and loop it.

**Decision.** Price replication by walking the item's recipe graph down to primitives, so an item
always costs at least what its inputs cost.

**Consequences.** Closes the arbitrage loop generically, for modded items the mod has never seen.
Requires recipe-graph traversal with cycle detection and a sane fallback for items that have no
recipe at all.

---

## ADR-0011 — Remote colonies are simulated abstractly, never force-loaded

*2026-09-29 · Accepted*

**Context.** The planetary layer lets players run industry on worlds they are not standing on.
The obvious implementation force-loads the remote chunks.

**Decision.** Model remote colonies as abstract state machines producing output over time. Never
force-load their chunks.

**Consequences.** Force-loading remote bases is how servers die; abstract simulation lets the mod
*encourage* interplanetary industry instead of quietly discouraging it through lag. Colony
behaviour must therefore be expressible as a rate-based state machine, and visiting a colony in
person has to reconcile abstract state with real blocks.

---

## ADR-0012 — Planets come from installed space mods, and satellites have no upkeep

*2026-09-29 · Accepted*

**Context.** Packs that include an orbital layer usually already have a space mod with its own
dimensions. Shipping a competing set of planets fragments the pack.

**Decision.** Detect planets from the space mods actually installed and integrate with them rather
than registering a rival dimension set. Give satellites no maintenance cost — they are paid for in
launch mass and power, once.

**Consequences.** Adding a competing dimension set to a pack that already has one is worse than
adding nothing, so integration keeps Grindless welcome in existing packs. Orbital upkeep would be
a recurring chore, and chores are precisely what this mod exists to delete. The cost is a detection
and mapping layer per supported space mod, plus a sensible fallback when none is installed.

---

## ADR-0013 — The README is the design source of truth

*2026-09-30 · Accepted*

**Context.** The session that wrote the design could not create a `docs/` directory, so the entire
design went into a single very large README. That constraint is gone, but the file now exists and
is coherent.

**Decision.** Keep `README.md` as the authoritative design document and split it into `docs/`
progressively, as each area is implemented. `docs/DESIGN.md` is an earlier standalone note,
superseded by the README and kept only for history.

**Consequences.** One place to look for design, at the cost of a large file. Where the README and
`docs/DESIGN.md` disagree, the README wins. Splitting must move content, never copy it, or the two
copies will diverge.

---

## ADR-0014 — Consolidate the orphaned session branches into one history

*2026-09-30 · Accepted*

**Context.** Two assistant sessions each produced substantial work and each lost its terminal
before committing anything. Their worktrees held complementary and partly conflicting trees:
`ezequiel24123z-fantastic-parakeet` had the multiloader skeleton, the common entrypoint and
`docs/DESIGN.md`; `ezequiel24123z-grindless-mod` had the full README design document, `.gitignore`,
a refined root Gradle setup, and two workaround files written to commit without a working shell.

**Decision.** Consolidate both into a single commit on one branch, taking the newer branch's root
Gradle configuration and the older branch's subproject scripts and sources.

Specifics worth recording, because they are invisible in the result:

- The subproject scripts referenced `rootProject.architectury_version` and
  `rootProject.neoforge_loader_range`; the newer `gradle.properties` named the first
  `architectury_api_version` and omitted the second. Unified on `architectury_api_version` and
  added the missing key, rather than keeping two aliases for one version.
- `COMMIT.bat` and `README.new.md` were excluded. Both existed only to work around the broken
  terminal, and `README.new.md` was explicitly marked a stale scratch file by its own author.
  `SETUP.ps1` was kept, because its bootstrap work is genuinely useful.
- The README's *Current blocker* section was rewritten as *Environment setup*: it described a
  broken terminal as the project's blocker and pointed at the now-removed `COMMIT.bat`.

**Consequences.** One branch carries the whole project and the orphaned branches can be discarded.
`Grindless.java` references registry classes that do not exist yet, so the committed tree does not
compile — this was accepted deliberately, because preserving the real work matters more than a
green build, and the state is documented in the README and the commit message.

This episode is the direct cause of the commit-early rule in [`AGENTS.md`](../AGENTS.md).

---

## ADR-0015 — Fluids are modelled as state, not as items

*2026-09-30 · Accepted*

**Context.** Minecraft tech mods almost universally model a fluid as an item with a different
texture: an amount in a tank, moved by a pipe at a fixed rate. It is simple to implement, it
integrates trivially with existing fluid APIs, and it produces no interesting decisions — a fluid
network is solved once and then copy-pasted forever.

**Decision.** A Grindless fluid stack carries **volume, temperature and pressure**. The same
substance at different points on that curve is a different resource: steam, superheated steam and
supercritical water drive the same turbine at very different outputs. Heat is conserved rather
than ignored, so steam that cools condenses in the pipe and a turbine fed condensate stalls.
Throughput is a function of pipe tier, pressure differential and viscosity; gases need a pump,
liquids can run downhill for free.

**Alternatives rejected.** Flat fluid-as-item, for the reasons above. A full thermodynamic
simulation, which is unplayable and unaffordable at tick rate — the model is deliberately three
numbers and a phase table, not a solver.

**Consequences.** Plumbing becomes a layout puzzle with the same shape as a belt bus: feed, boil,
work, condense, return. It also gives the futuristic tier somewhere physical to go — cryogenics,
supercritical loops, plasma containment — instead of inventing arbitrary new units.

The costs are real. Every fluid-carrying block has to track and display three values rather than
one, interop with the pack's plain fluid API has to pick a sane temperature and pressure for
foreign fluids entering the system, and the UI must make state legible or the whole thing reads as
unexplained failure. Rupture is therefore a loud, visible, repairable vent rather than an
explosion: the model must be safe to learn by experiment.

---

## ADR-0016 — Pipes early, the Phase Network late

*2026-09-30 · Accepted*

**Context.** The mod's founding idea is coverage areas instead of cable spaghetti (ADR-0001's
sibling in design terms): a Flux Pylon powers everything inside its area, so nobody runs wire.
Applying that to fluids immediately would delete the pipe gameplay that ADR-0015 just created.
Never applying it would leave fluids as the one system that stays tedious forever.

**Decision.** Both, in sequence. Real pipes with real pressure carry the early and middle game. At
T3 the **Phase Manifold** arrives: inside its coverage area, registered tanks and machines exchange
any fluid the network holds with no pipes at all.

The phase network is explicitly **not** a strict upgrade. Dematerialising a fluid costs FU per
unit, proportional to distance from ambient, so cryogenics and plasma are expensive to move and a
well-built pipe loop stays cheaper forever.

**Consequences.** This is the same trade the mod already offers between belts and drones: pay in
layout, or pay in power. Players who enjoy plumbing keep plumbing and are rewarded with lower
running costs; players who are finished with plumbing can buy their way out. The transition is
never forced, and no content is invalidated at T3.

The risk to watch in balancing is the phase network's FU price. Too cheap and pipes become
vestigial, taking the ADR-0015 gameplay with them; too expensive and it is a trap option nobody
builds.

---

## ADR-0017 — Machines above T1 are manufactured, never hand-crafted

*2026-09-30 · Accepted*

**Context.** In Factorio the factory builds the factory: you hand-craft the first burner drill and
essentially nothing else. Minecraft tech mods usually discard that loop by letting a player with a
full inventory assemble any machine in a crafting grid, which makes the factory decorative — a
thing that produces items rather than the thing that produces itself.

Grindless has a specific exposure here. It is designed to be installed into large existing packs,
where a player may arrive already holding the pack's mid-game materials.

**Decision.** Past the bootstrap, machines have **no crafting-table recipe at all** — not a hidden
one, not a deliberately expensive one. T2–T3 machines are produced by the **Assembler**, T4 by the
**Quantum Assembler**, T5–T6 by the **Orbital Assembly Bay**. Each consumes a researched blueprint,
fabricated components, power and time.

Components form the intermediate economy — casings, motors, pumps, circuit boards, integrated
circuits, superconductors, quantum cores — and several of them require fluids, so circuits need
etching acid and machines need circuits.

**Consequences.** The reward for building a production line becomes the ability to build the next
one, which is the loop the mod exists to deliver. Research gains teeth: a blueprint is a production
target rather than a note. Fluids become load-bearing exactly once, early, at small scale, rather
than being a system players can ignore entirely.

The danger is obvious — this rule could easily become the grind the mod exists to delete — so the
guard rails are part of the decision, not an afterthought: blueprints are permanent, Assemblers are
cheap and parallelise so the answer to "this is slow" is always "build another one", T0 and T1 stay
hand-craftable forever so there is no softlock path, and the gate is datapack-driven so a pack
author can relax it without a mod patch.

---

## ADR-0018 — One container contract, with auto-void off by default

*2026-09-30 · Accepted*

**Context.** Grindless will ship many storage blocks — logistics crates, four tiers of tank, and
the input and output buffers inside every machine. Tech mods typically give each its own ad-hoc
interface, so the player learns filtering four times and still cannot predict which blocks support
it.

Separately, a system that generates recipes from tags at runtime (ADR-0004, ADR-0005) produces
byproducts for materials the player has never heard of. Unwanted byproducts backing up a line are
the characteristic failure mode of that design, so a discard mechanism is not optional.

**Decision.** Every container — item or fluid, block or machine buffer — implements one shared
contract: per-slot filters that survive the slot emptying, a buffer target, a capacity limit below
physical maximum, configurable auto-void with a threshold and mode, per-face I/O, insertion and
extraction priority, and a fill-level signal output.

Auto-void is built defensively, because it is simultaneously the feature that keeps a base running
unattended and the easiest way for a player to silently destroy something they wanted:

- off by default on every container, always;
- enabling it requires an explicit confirmation;
- a voiding container is visibly marked with particles and a glow, so it is discoverable months
  later in a base you no longer remember building;
- it trims above the threshold and never empties a container;
- it emits a distinct signal while voiding, so alarms can be built;
- it refuses to discard anything on the replication blacklist — creative items, quest rewards,
  pack-unique items.

**Consequences.** One interface to learn and one implementation to maintain, at the cost of a
richer base container class than most blocks strictly need. The defensive defaults mean a player
cannot lose materials to a feature they did not know was on, which is the failure this design is
most exposed to.

The canonical overflow pattern becomes `Overflow Gate` into a voiding `Storage Crate` — the
standard answer to eleven thousand gravel.

---

## ADR-0019 — Design machines before recipes

*2026-10-01 · Accepted*

**Context.** The natural instinct is to design content item-first: decide what the player makes,
then add a machine for each process. Large tech mods that work this way accumulate hundreds of
blocks, most of which host a single recipe type, and the catalogue grows without the gameplay
getting deeper.

**Decision.** Specify the machine layer completely first — what machines exist, what they can do,
how they scale, how multiblocks behave — and only then specify the recipe graph, the item and fluid
catalogue and the concrete routes. The machine design lives in [`MACHINES.md`](MACHINES.md), the
recipe design in `PROCESSES.md`.

Machines are specified as **capabilities**, not as recipe holders, which is what makes the ordering
pay off: see ADR-0020.

**Consequences.** A small machine set can host an enormous recipe space, and adding content later
means adding recipes rather than blocks. The risk is designing a capability nothing ends up needing,
which is mitigated by every machine in `MACHINES.md` naming the processes that motivate it.

---

## ADR-0020 — Recipes are parameterised by process conditions

*2026-10-01 · Accepted*

**Context.** In essentially every Minecraft tech mod a recipe is `inputs → outputs` bound to a
machine type. The machine *is* the process. This makes each recipe a lookup with one answer, and it
forces a new machine for every new kind of transformation.

**Decision.** A Grindless recipe is `inputs + conditions + time → outputs`, where conditions are
physical parameters: temperature, pressure, atmosphere, catalyst, field, agitation. The same inputs
under different conditions produce different outputs.

A machine is then defined by the **condition envelope** it can maintain, and any recipe whose
conditions fall inside an envelope runs in that machine. Efficiency is continuous: inside the
optimal band is full yield, at the edges yield drops or byproducts appear, outside the band the
process fails visibly and informatively.

**Alternatives rejected.** Fixed recipes per machine — simple, and the thing that makes the genre
shallow. A full thermodynamic simulation — unaffordable at tick rate and unplayable.

**Consequences.** This single decision delivers several goals at once: multiple genuine routes to
the same product, a standing optimisation problem that changes as the factory grows, a reason for
the fluid system to carry temperature and pressure (ADR-0015), and a recipe space that is the
*product* of machines and conditions rather than a list.

The costs are real and concentrated in presentation. Recipe lookup is no longer "which machine" but
"which conditions", which is why the route viewer is a first-class feature rather than an
integration (ADR-0023). Out-of-band failure must teach rather than merely fail. And the number of
condition dimensions the UI can carry before configuration becomes work is still an open question —
recorded as such in `MACHINES.md`.

---

## ADR-0021 — Complexity beyond GregTech, with no grind

*2026-10-01 · Accepted*

**Context.** The brief is a mod deeper than GregTech that nonetheless contains no grind. Those
sound contradictory, because the genre's depth is habitually sold with repetition attached.

**Decision.** Treat complexity and grind as the orthogonal things they are. **Complexity** is the
number of meaningful decisions and how they interact. **Grind** is repeating an action whose outcome
is already known. Grindless maximises the first and refuses the second, under one rule:

> If the player has already solved a problem, never ask them to solve it again by hand. Asking them
> to solve it at a different scale is fair — that is a new problem.

Concretely, depth comes from the recipe graph, condition tuning, route selection and ratio
balancing; and the usual sources of repetition are removed outright. Hand-crafting intermediates is
replaced by fabrication (ADR-0017); per-material ore lines are replaced by the tag-driven registry
(ADR-0004); re-tiering is an in-place upgrade; machine configuration is copied with Process Cards;
and there is no maintenance mechanic at all, because chores are not content.

Scaling is given three axes with different shapes — parallel, overclock, multiblock — where
overclocking is deliberately the *worst* option and never required. In GregTech overclocking is
mandatory and therefore not a decision; making it usually-wrong turns it back into one.

**Consequences.** The mod can be very deep without being long. The risk to watch is that removing
repetition also removes pacing: if everything is instant, tiers blur. The counterweight is that
throughput still has to be built — post-scarcity in materials is never post-scarcity in throughput.

---

## ADR-0022 — Parametric multiblocks instead of fixed schematics

*2026-10-01 · Accepted*

**Context.** The standard multiblock is a fixed schematic: build this exact arrangement and receive
this exact machine. The player looks it up once and copies it forever, which makes a headline
feature into a chore with extra steps.

**Decision.** Grindless multiblocks are parametric. The player chooses dimensions and internal
arrangement; behaviour follows from what was built. Reactor output and heat depend on fuel rod
adjacency and coolant channel layout; distillation tower height sets how many fractions separate;
electrolysis electrode area sets current; accelerator ring circumference sets reachable particle
energy and therefore which transmutations exist at all; fusion coil count and containment strength
set which fuel cycles can ignite.

The reward for a multiblock is **better ratios and new capabilities**, not merely more throughput —
a bigger reactor loses proportionally less heat, a taller column makes fractions a short one cannot.

**Consequences.** Understanding beats copying, and there is no single correct schematic to look up.
Failure modes are deliberately recoverable: an over-hot reactor SCRAMs and needs a restart cycle
rather than detonating, because losing hours of progress to one mistake is not depth.

The cost is validation and feedback complexity — the game must explain *why* a given core design
underperforms, or the design space reads as noise.

---

## ADR-0023 — A native route viewer with a ratio solver

*2026-10-01 · Accepted*

**Context.** Parameterised recipes (ADR-0020) and multiple routes per product (`MACHINES.md`) make
the recipe graph unreadable through a conventional recipe viewer, which answers "what makes this"
with a flat list and cannot express conditions, routes or ratios.

**Decision.** Ship the **Process Atlas** in the mod itself, working with no other mod installed. It
shows every route to a product as a graph, with ratios per unit time at a chosen tier, the
conditions each route needs, a cost overlay in FU and machine count, and reachability marking which
routes are buildable now and naming what is missing from the others. Given a target rate it solves
the line: how many of each machine, at which tier, in what ratio.

JEI, REI and EMI all get full integration on top, with the Atlas reachable from a recipe screen.

**Alternatives rejected.** Relying on JEI alone — it cannot express condition ranges or compare
routes, and a pack that ships no recipe viewer would make the mod unplayable.

**Consequences.** The solver deliberately removes the arithmetic, on the position that arithmetic
is not gameplay while building, placing and feeding a line is. This is the decision most at risk of
going too far: if "press solve, build exactly that" becomes the entire loop, the solver should
propose rather than prescribe. Recorded as an open question in `MACHINES.md`.

---

## ADR-0024 — `loom.platform` markers and the Windows path length limit

*2026-10-01 · Accepted*

**Context.** Two environment problems surfaced on the first real build attempt, and both are the
kind that look like something else entirely.

**Architectury Loom decides a subproject's platform from a `loom.platform` property in that
subproject's own `gradle.properties`.** Without it the module defaults to Fabric, so `forge`
configured as a Fabric project and `dependencies { forge "net.minecraftforge:forge:..." }` failed
with `Could not find method forge()` — which reads as a broken dependency rather than a missing
marker. Both platform modules now carry the file.

**The checkout path is 181 characters**, and the deepest source file reaches 268 — past the Windows
`MAX_PATH` limit of 260. `LongPathsEnabled` is `0` on this machine and setting it needs
administrator rights plus a reboot. Git failed with `Filename too long` when staging.

**Decision.** Commit a one-line `gradle.properties` in each platform module. For path length, set
`core.longpaths = true` in the repository's git config, which makes Git for Windows use the Unicode
path APIs and needs no elevation.

**Alternatives rejected.** Enabling `LongPathsEnabled` system-wide, which needs admin on a machine
where the user is not one; shortening the package name, which would be contorting the project
around a tooling limit; relocating the checkout, which fights the tool that created the worktree.

**Consequences.** Both problems are fixed without elevation. `core.longpaths` is local to the
repository, so a fresh clone must set it again — `SETUP.ps1` should do this, and its `MAX_PATH`
warning is now known to be a real failure on this machine rather than a theoretical one.

Note that the JDK and Gradle are *not* protected by this setting. Nothing has broken so far, but a
build failure that looks like a corrupt file should be treated as a path-length problem first.
