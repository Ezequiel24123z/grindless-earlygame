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
| [0001](#adr-0001--architectury-for-multiloader-support) | Architectury for multiloader support | Superseded by ADR-0039 |
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
| [0025](#adr-0025--universal-conduits-that-do-not-obsolete-belts-or-pipes) | Universal conduits that do not obsolete belts or pipes | Accepted |
| [0026](#adr-0026--named-conduit-networks-instead-of-coloured-channels) | Named conduit networks instead of coloured channels | Accepted |
| [0027](#adr-0027--chassis-marks-widen-the-condition-envelope) | Chassis marks widen the condition envelope | Accepted |
| [0028](#adr-0028--every-machine-upgrade-is-a-trade) | Every machine upgrade is a trade | Accepted |
| [0029](#adr-0029--operator-drones-imperative-automation-beside-declarative-logistics) | Operator Drones: imperative automation | Accepted |
| [0030](#adr-0030--everything-is-a-chassis-plus-modules) | Everything is a chassis plus modules | Accepted |
| [0031](#adr-0031--construction-drones-exist-so-multiblocks-can-be-massive) | Construction drones exist so multiblocks can be massive | Accepted |
| [0032](#adr-0032--the-item-catalogue-is-a-matrix-not-a-list) | The item catalogue is a matrix, not a list | Accepted |
| [0033](#adr-0033--materials-come-from-tags-reagents-are-grindlesss-own) | Materials come from tags, reagents are Grindless's own | Accepted |
| [0034](#adr-0034--ratios-are-quoted-against-a-canonical-process-unit) | Ratios are quoted against a canonical process unit | Accepted |
| [0035](#adr-0035--routes-compose-from-a-beneficiation-stage-and-a-reduction-stage) | Routes compose from a beneficiation and a reduction stage | Accepted |
| [0036](#adr-0036--every-byproduct-must-have-a-named-sink) | Every byproduct must have a named sink | Accepted |
| [0037](#adr-0037--flux-amounts-are-long-and-clamp-at-the-fe-boundary) | Flux amounts are `long` and clamp at the FE boundary | Accepted |
| [0038](#adr-0038--one-flux-ladder-with-voltage-names-as-aliases) | One Flux ladder, with voltage names as aliases | Accepted |
| [0039](#adr-0039--forge-1201-is-the-only-build-target) | Forge 1.20.1 is the only build target | Accepted |
| [0040](#adr-0040--running-out-of-band-costs-time-never-yield) | Running out of band costs time, never yield | Accepted |
| [0041](#adr-0041--a-condition-check-returns-a-named-fault-not-a-boolean) | A condition check returns a named fault, not a boolean | Accepted |
| [0042](#adr-0042--machines-subscribe-to-ticking-they-do-not-tick-by-default) | Machines subscribe to ticking; they do not tick by default | Accepted |
| [0043](#adr-0043--recipe-lookup-is-indexed-and-cached-never-a-linear-scan) | Recipe lookup is indexed and cached, never a linear scan | Accepted |
| [0044](#adr-0044--neighbour-lookups-are-cached-against-capability-invalidation) | Neighbour lookups are cached against capability invalidation | Accepted |
| [0045](#adr-0045--capabilities-are-attached-by-event-not-overridden-on-the-block-entity) | Capabilities are attached by event, not overridden on the block entity | Accepted |
| [0046](#adr-0046--the-pylon-index-is-chunk-bucketed-and-brownouts-resolve-in-two-phases) | The pylon index is chunk-bucketed, and brownouts resolve in two phases | Accepted |
| [0047](#adr-0047--veins-are-long-lived-and-deepening-competes-with-relocating) | Veins are long-lived, and deepening competes with relocating | Accepted |
| [0048](#adr-0048--assets-are-generated-from-the-jdk-with-a-named-list-of-what-cannot-be) | Assets are generated from the JDK, with a named list of what cannot be | Accepted |
| [0049](#adr-0049--a-green-build-must-include-a-booted-server) | A green build must include a booted server | Accepted |
| [0050](#adr-0050--grindless-supplies-a-material-only-where-the-pack-has-none) | Grindless supplies a material only where the pack has none | Accepted |
| [0051](#adr-0051--every-block-ships-a-floor-of-assets-generated-from-one-list) | Every block ships a floor of assets generated from one list | Accepted |
| [0052](#adr-0052--each-machine-has-its-own-shape-state-textures-and-effects) | Each machine has its own shape, state textures and effects | Accepted |
| [0053](#adr-0053--the-crude-extractor-is-the-first-consumer) | The Crude Extractor is the first consumer | Accepted |
| [0054](#adr-0054--pylons-are-three-blocks-tall-and-cover-a-factory) | Pylons are three blocks tall and cover a factory | Accepted |
| [0055](#adr-0055--the-multitool-does-not-mine) | The Multitool does not mine | Accepted |
| [0056](#adr-0056--t0-bootstrap-recipes-are-authored-json-using-tags) | T0 bootstrap recipes are authored JSON using tags | Accepted |
| [0057](#adr-0057--the-research-terminal-unlocks-world-scoped-blueprints) | The Research Terminal unlocks world-scoped blueprints | Accepted |
| [0058](#adr-0058--build-playable-slices-not-system-layers) | Build playable slices, not system layers | Accepted |
| [0059](#adr-0059--first-iron-is-a-generated-graph-and-a-voltaic-gate) | First iron is a generated graph and a Voltaic gate | Accepted |
| [0060](#adr-0060--first-factory-is-lane-data-a-survey-and-an-unpowered-belt) | First factory is lane data, a survey and an unpowered belt | Accepted |
| [0061](#adr-0061--the-machine-state-smoke-runs-as-one-function) | The machine-state smoke runs as one function | Accepted |
| [0062](#adr-0062--first-fluids-are-millibuckets-gravity-clay-and-a-named-co-sink) | First fluids are millibuckets, gravity clay and a named CO sink | Accepted |
| [0063](#adr-0063--the-factory-builds-the-factory-at-t1) | The factory builds the factory at T1 | Accepted |
| [0064](#adr-0064--energy-spanning-is-distance-and-storage-not-coverage) | Energy spanning is distance and storage, not coverage | Accepted |
| [0065](#adr-0065--t1-kiln-is-roast-and-so₂-not-the-acid-line) | T1 Kiln is roast and SO₂, not the acid line | Accepted |
| [0066](#adr-0066--the-t1-atlas-is-a-live-lookup-not-the-solver) | The T1 Atlas is a live lookup, not the solver | Accepted |
| [0067](#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier) | Modular armour each tier; Arc Reactor is F3 factory and suit | Accepted |
| [0068](#adr-0068--horizon-gates-are-commute-infrastructure-not-mining-dimensions) | Horizon Gates are commute infrastructure, not mining dimensions | Accepted |
| [0069](#adr-0069--the-multitool-rotates-and-relocates-it-still-does-not-mine) | The Multitool rotates and relocates; it still does not mine | Accepted |
| [0070](#adr-0070--remaining-work-is-the-autonomous-build-out) | Remaining work is the autonomous build-out | Accepted |
| [0071](#adr-0071--t1-belt-junctions-are-merger-tunnel-and-overflow) | T1 belt junctions are merger, tunnel and overflow | Accepted |
| [0072](#adr-0072--the-t1-sorter-peels-it-does-not-split) | The T1 sorter peels; it does not split | Accepted |
| [0073](#adr-0073--industrial-is-the-second-blueprint-on-the-same-terminal) | Industrial is the second blueprint on the same terminal | Accepted |
| [0074](#adr-0074--the-wire-mill-is-t2-and-does-not-wait-for-acid) | The Wire Mill is T2 and does not wait for acid | Accepted |

---

## ADR-0001 — Architectury for multiloader support

*2026-09-29 · Superseded by [ADR-0039](#adr-0039--forge-1201-is-the-only-build-target)*

> **Superseded in part.** The multiloader *goal* is withdrawn — Forge 1.20.1 is now the only build
> target. The *structure* this record chose survives: Architectury, a `common/` module holding
> essentially all logic, and `@ExpectPlatform` for loader-specific code. See ADR-0039 for why
> keeping the structure while dropping the second loader is the cheap option rather than a
> contradiction.

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

> **Narrowed by [ADR-0039](#adr-0039--forge-1201-is-the-only-build-target).** The 1:1 FU↔FE rate
> and the hand-written bridge stand exactly as decided. Only the Fabric half is withdrawn: with
> Forge as the sole target there is one bridge, over Forge capabilities, and the TeamReborn
> dependency and its repository caveat no longer apply.

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

---

## ADR-0025 — Universal conduits that do not obsolete belts or pipes

*2026-10-01 · Accepted*

**Context.** By mid-game a base runs four parallel infrastructures that all solve the same problem:
belts for items, pipes for fluids, signal cable for logic, and a steam loop used only to move heat
somewhere. Routing them around each other is busywork. EnderIO's conduits are the genre's answer
and they are the right idea.

The danger is specific and severe: belts are the Factorio soul of this mod, and a universal conduit
that is strictly better deletes them. That is exactly the trap ADR-0016 avoided for fluids.

**Decision.** Ship **Flux Conduits** from T2: one chassis block whose carried types are decided by
**cores** inserted into it — item, fluid, heat, signal and (rarely) Flux. Several cores coexist in
one block, so one run carries ore, coolant, heat and the logic controlling them.

Belts and pipes survive because conduits are deliberately not strictly better:

- **Item cores draw power per item moved; belts move bulk for free.** For the highest-volume line
  in any base — a mine feeding a smelter — belts remain correct forever.
- **A belt is also a buffer.** A full belt is thousands of items of storage in transit; a conduit
  holds almost nothing, so a conduit-fed line stalls the instant production hiccups.
- **Pipes exploit physics for free.** Gravity, head pressure and a condensate loop cost no power.
  A fluid core always does.

The division that emerges is the one real factories have: **belts and pipes move bulk, conduits
move logistics**. At T5 the Singular Conduit genuinely can replace everything, and by then power is
abundant enough that some players will — a legitimate and expensive way to play.

**Consequences.** This is the third instance of one principle — pay in layout, or pay in power —
after belts versus drones and pipes versus the Phase Network. The repetition is intentional: it is
the mod's economic language, and a player who learns it once can predict every later trade.

The heat core is the genuinely new capability. Moving heat *as heat* rather than as hot fluid is
both more realistic and practically useful, since reactor waste heat can reach a distant process
without plumbing a loop there and back.

The balancing risk is the item core's power cost. Too cheap and belts die; too expensive and the
conduit is ornamental.

---

## ADR-0026 — Named conduit networks instead of coloured channels

*2026-10-01 · Accepted*

**Context.** Conduit mods separate logical networks sharing physical space using a fixed palette of
coloured channels, typically sixteen. This works at small scale and fails at large scale: the
colours are a fixed budget, and nobody remembers what purple meant three months later.

**Decision.** Networks are **named**. The player names a network — `iron-bus`,
`reactor-coolant` — and endpoints subscribe by name. Names are unlimited and self-documenting, and
a conduit reports which networks it belongs to when inspected.

**Alternatives rejected.** Coloured channels, for the reasons above. Automatic network inference
from connectivity, which is convenient until two networks accidentally touch and silently merge.

**Consequences.** Readability at scale, which is the whole problem, plus no arbitrary cap on
network count. The costs are a text-entry UI where a colour picker would have done, the need to
handle renames and typos gracefully, and the fact that names are strings and must therefore be
persisted, synchronised and shown in the conduit's tooltip.

---

## ADR-0027 — Chassis marks widen the condition envelope

*2026-10-01 · Accepted*

**Context.** Machine tiering is normally numeric: the same machine with a bigger speed number and a
higher voltage cap. GregTech ships nine of them. The player rebuilds identical infrastructure
repeatedly and nothing new becomes possible, which is the precise definition of grind this project
rejects (ADR-0021).

**Decision.** Machines exist at marks **MK I–MK V**, applied **in place** with a Chassis Upgrade
Kit — the machine keeps its position, contents, configuration and connections, and is never
rebuilt or re-piped.

A mark sets upgrade slot count and maximum Flux tier, but its defining property is that it
**widens the condition envelope**. Since recipes are selected by conditions (ADR-0020), a higher
mark unlocks *recipes*: an MK I Arc Furnace reaches 1800 °C and smelts common metals, an MK V
reaches 3500 °C and runs everything the machine type is physically capable of.

**Consequences.** Upgrading becomes a goal rather than a tax, because it changes what is possible
rather than how fast the same thing happens. The catalogue does not explode, since marks are an
attribute of a machine rather than five separate blocks. And low marks never become invalid — an
MK I still runs every recipe inside its envelope, forever.

This also gives the research tree something concrete to gate that is not a new block.

---

## ADR-0028 — Every machine upgrade is a trade

*2026-10-01 · Accepted*

**Context.** Most upgrade systems offer strict improvements, so the optimal play is to fill every
slot with the best one. The upgrade is then not a decision, it is a tax the player pays once and
forgets. Mekanism's speed upgrades are the canonical case: there is no reason not to max them.

**Decision.** **Every upgrade spends one resource to buy another.** Speed buys cycle time with
superlinear power and waste heat; Efficiency buys power with time; Yield buys output with time and
a catalyst; Precision buys condition accuracy with constant upkeep; Insulation buys cheap heat
retention with slow thermal response; Damping buys Resonance silence with throughput. Three pairs
are mutually exclusive — Speed↔Efficiency, Speed↔Precision, Insulation↔Parallel — because wanting
both is wanting the trade not to exist.

Which upgrade is correct therefore depends on which resource is scarce *now*: Efficiency when
power-limited early, Yield and Recovery when material-limited mid-game, Parallel when throughput-
limited with power to spare, Precision for tight chemistry, Damping when hiding from what the noise
attracts.

**Consequences.** The same machine is configured differently in different parts of the same base
and at different points in the game, which is the definition of a live decision. Notably **Speed is
almost never right**, consistent with the stance on overclocking: the correct way to produce more
is to build wider.

Keeping this from becoming its own chore is part of the decision: upgrades are reusable so
experimenting is free, Process Cards carry the loadout so configuring the tenth machine is a copy,
and the Atlas accounts for upgrades when solving a line so the choice is informed rather than
guessed.

The risk is balance surface area — ten upgrades times five marks times twenty machines is a large
space, and the exclusivity pairs are what keep it from collapsing into one dominant loadout.

---

## ADR-0029 — Operator Drones: imperative automation beside declarative logistics

*2026-10-01 · Accepted*

**Context.** Every automation system in the mod so far is **continuous flow**: belts, pipes,
conduits, and logistics drones that fulfil standing requests. All of them are declarative — you
describe a desired steady state and the system maintains it.

A large class of real factory work cannot be described that way. *"Wait until the autoclave
finishes, take the batch to the press, run it, bring the byproduct back, and swap the catalyst if
it is spent"* is a **sequence**: irregular, conditional, multi-step. No flow system expresses it,
and the player ends up doing it by hand — which is grind by definition.

**Decision.** Add **Operator Drones**: a drone that executes an ordered routine, step by step,
housed and programmed in an **Operator Bay** that is a pylon module like the Drone Bay.

Routines are **built, not typed**. A routine is an ordered strip of physical Instruction Cards —
closer to a player-piano roll than to code — because the README already refuses to make the player
learn a scripting language for the circuit network and the same rule applies here. The vocabulary
is eleven cards: `Go To`, `Take`, `Give`, `Draw`, `Pour`, `Absorb`, `Emit`, `Operate`, `Read`,
`Wait Until`, `If/Else`, `Repeat`, `Signal`, `Return` — of which only two are branches.

Capability comes from **pods**: cargo, fluid (the drone's own tank), thermal, tool arm, sensor and
range extender (ADR-0030).

**Alternatives rejected.** A text or graph scripting language, which is powerful and is exactly the
thing the mod has already decided not to ask of the player. Extending logistics drones with
conditions, which would make a simple declarative system complicated without making it imperative.

**Consequences.** Operator Drones are slow and single-tasking, so they are the wrong answer to
anything high-volume — a belt moves more ore in a second than a drone moves in a minute. They win
only where flow loses, which keeps every other logistics system intact. At T6 the same routines run
on remote colonies, where belts cannot reach at all, which is what turns an off-world base from a
trickle into a factory.

Debuggability is part of the decision rather than a later addition, because a programmable system
without debugging is misery: the drone displays its current instruction in flight, step mode
advances one instruction at a time, failures name themselves (`Step 4: target inventory full`) on
the bay and as a logic signal, and editing draws a holographic preview of the path.

Routine Cards make a finished routine one copyable item, so building the tenth identical setup is a
copy rather than a repeat — the same anti-repetition device as Process Cards.

The risk to watch is scope creep in the card vocabulary. Eleven cards is learnable; thirty is a
programming language with extra steps, and the line should be held.

---

## ADR-0030 — Everything is a chassis plus modules

*2026-10-01 · Accepted*

**Context.** Four systems arrived at the same shape independently: machines take upgrades in slots
(ADR-0028), conduits take cores that decide what they carry (ADR-0025), drones take pods that
decide what they can do (ADR-0029), and pylons take bay modules. This was convergence, not plan,
which usually means the pattern is load-bearing and should be stated before someone breaks it.

**Decision.** Adopt it as a design principle: **capability arrives by slotting a module into a
chassis, never by crafting a different block.** A chassis sets the ceiling — slot count, tier,
envelope — and modules decide what it actually does within that ceiling.

**Consequences.** The player learns one interaction pattern and it applies everywhere, which is a
large reduction in what a complex mod asks them to hold in their head. The block catalogue stays
small, because variation lives in modules rather than in block variants — the alternative is how
tech mods end up shipping nine near-identical machines per type.

It also means upgrading is non-destructive by construction: you slot something in, and the machine
keeps its position, contents, configuration and connections. Nothing is ever rebuilt or re-piped,
which is one of the mod's main anti-grind commitments.

The cost is that modules are items that must be produced, which pushes more load onto the
fabrication economy (ADR-0017), and that every chassis needs a module UI good enough to make the
slots legible. The failure mode to avoid is modules that are strictly mandatory — a module everyone
always installs should simply be part of the chassis.

---

## ADR-0031 — Construction drones exist so multiblocks can be massive

*2026-10-01 · Accepted*

**Context.** Construction Drones were in the design as a convenience — "builds from blueprints" —
and on review that is not a good enough reason to ship them. The Blueprint Tool already captures
layouts, and a mod whose whole premise is removing busywork should be suspicious of a feature whose
only justification is saving clicks.

The useful question is what they *enable*. The answer reframes them entirely.

**The real limit on multiblock scale in every mod is placement tedium, not design.** Nobody ships a
two-thousand-block structure because nobody will place two thousand blocks by hand. So multiblocks
stay at 5×5×5, their design space stays shallow, and "build this shape" becomes a schematic copied
once — which is the exact failure ADR-0022 set out to avoid by making multiblocks parametric.

ADR-0022 is therefore only half-true as written: a parametric multiblock is a real design space
only if the design space is big enough to have interesting answers, and by hand it cannot be.

**Decision.** Construction Drones are an **enabler, not a convenience**, and multiblocks are scaled
up to match. From T3 drones build a blueprint using materials drawn from the logistics network,
which lifts the size ceiling; multiblock scale then grows with the player's ability to build —
roughly 5³ by hand at T2, ~15³ with drones at T3, ~32³ with swarms at T4, and structures measured
in chunks at T5 via the **Assembly Field**, which materialises a blueprint at once.

Two things make this sound rather than merely large:

- **Placing blocks by hand at that scale is grind by the project's own definition** — repeating an
  action whose outcome is already known. Automating the placing while keeping the designing is
  precisely the rule in ADR-0021.
- **Blueprints are validated and simulated before construction.** A design reports legality and
  which part fails, computed output, heat, coolant demand and stability margin, its full bill of
  materials, and a warning when it will run at the edge of its envelope. Trial and error at a cost
  of ten thousand components is punishment, not engineering.

**Consequences.** The Fission Reactor becomes the flagship design problem it was supposed to be:
hundreds of internal positions, fuel clustering traded against coolant routing, moderators and
reflectors making the core's geometry matter rather than just its volume. The same applies to the
accelerator ring, the distillation column and the fusion confinement.

The friction that remains is the right friction — materials still have to be produced, and that
cost is what makes a large multiblock a commitment. What is removed is only the clicking.

Risks: the simulator must agree exactly with the runtime behaviour or it becomes a lie, which makes
it a real implementation constraint rather than a UI nicety. Chunk-scale structures raise genuine
performance questions that the multiblock framework has to answer before T5 content is built. And
early multiblocks must stay hand-placeable, since drones do not exist until T3.

---

## ADR-0032 — The item catalogue is a matrix, not a list

*2026-10-01 · Accepted*

**Context.** `PROCESSES.md` had to specify the item catalogue, and the obvious way to do that is to
enumerate items. That is how most tech mods do it, and it is why their catalogues reach four hundred
entries while still missing the one form a given pack needs.

Enumeration also cannot work here. The material set is not known at build time — it comes from the
runtime tag scan (ADR-0004) and therefore depends on the installed pack. A list cannot be written
for a set that does not exist yet.

**Decision.** The catalogue is defined as **axes, and the items are their product**. Grindless
authors the axes; the materials come from tags; the generator emits the cells.

- **Form × material.** Thirteen formed types (plate, foil, rod, bolt, gear, ring, wire, fine wire,
  coil, nugget, ingot, block, hot ingot) and eight ore-line types, applied to every material.
- **Anion × material.** Six compound families — oxide, sulfide, chloride, sulfate, carbonate,
  hydroxide — which is what gives the chemical layer its breadth.

A cell is emitted only when a process that produces it exists, so a pack never sees salts it has no
way to make.

**Alternatives rejected.** Enumerating items per material (does not survive an unknown material
set, and is unmaintainable at pack scale); a single generic "dust" with NBT material data (breaks
belts, filters, storage mods and every recipe viewer, and NBT-bearing stacks are the usual cause of
tech-mod performance complaints); deferring to another mod's unification (makes Grindless unusable
standalone).

**Consequences.** Adding a form is one authored entry that instantly covers every material in the
pack, which is the same leverage ADR-0005 gets for recipes. Grade becomes a property of the *form*
rather than of the item, which is what lets one generated recipe set cover the whole pack and is
the basis of the composition rule in ADR-0035.

The cost is that the catalogue's size is a pack property rather than a project one, so the project
cannot state how many items it has — only how many axes. Creative-tab organisation and texture
generation both have to be driven by the same matrix or they will drift from it.

---

## ADR-0033 — Materials come from tags, reagents are Grindless's own

*2026-10-01 · Accepted*

**Context.** ADR-0004 resolves materials from tags so Grindless uses the pack's copper rather than
registering a rival one. Writing the chemical core exposed the limit of that rule: it works for
materials and fails for reagents.

`forge:ingots/copper` is a real convention that essentially every mod follows. There is no
equivalent agreement for sulfuric acid. Mods that ship one disagree on its name, its concentration,
its colour and whether it is even a fluid, so a tag lookup would resolve to a different substance in
every pack — or, far worse, resolve to something whose amount means something else, silently
breaking every ratio in `PROCESSES.md`.

**Decision.** Split the rule by kind.

- **Materials** — anything with a `forge:`/`c:` convention — are resolved from tags at runtime and
  never registered by Grindless.
- **Reagents** — acids, bases, industrial gases, leachates, process intermediates — are
  **registered by Grindless**, with its own units and reference states, and are tagged so other
  mods can opt in.

The boundary is testable: if a widely-followed tag convention exists, it is a material; if it does
not, it is a reagent.

**Alternatives rejected.** Tag-resolving reagents too (the ratios in `PROCESSES.md` become
meaningless when the resolved fluid has a different concentration); a config mapping per pack
(pushes an unsolvable problem onto pack authors, and gets it wrong silently); avoiding named
reagents by making chemistry abstract (throws away the entire reason the condition system exists).

**Consequences.** Grindless's acid may coexist with another mod's acid. That is accepted: the
alternative is a wrong conversion rather than a visible duplicate, and a duplicate is something a
pack author can unify deliberately while a wrong ratio is a bug nobody can see.

Every registered reagent carries a reference state (ADR-0015), because a gas volume without one is
not a quantity. Reagents therefore need their own tags published early, so packs can integrate
rather than merely coexist.

---

## ADR-0034 — Ratios are quoted against a canonical process unit

*2026-10-01 · Accepted*

**Context.** `PROCESSES.md` specifies routes that must be *compared* — that is the whole point of
having more than one — and the Atlas's solver has to balance lines across them (ADR-0023). Neither
works if two routes quote their yields on different bases. "Three per operation" and "three per
input" differ by the whole chain length, and chains that look generous per stage routinely multiply
out to a loss.

**Decision.** One basis, fixed for the whole project.

- **u (unit)** is the canonical solid amount: 1 u = 1 ingot = 1 dust = 1 plate = 9 nuggets.
- **1 u as melt = 144 mB**, the established modded convention, so casting needs no conversion table.
- **B** is 1000 mB; **gases are quoted at 20 °C and 0.1 MPa** unless stated, because a fluid is a
  state and not a thing (ADR-0015).
- **Yields are per unit of primary input to the front of the route**, not per stage.
- **Times are for MK I, no upgrades, at the process's base Flux tier.**

**Alternatives rejected.** Per-stage ratios (hide end-to-end losses, which is exactly the number
the player needs); quoting at a "typical" overclock (bakes in the Speed upgrade the design wants
players to reject, ADR-0028); mB-only with no unit concept (every recipe grows a conversion).

**Consequences.** The time baseline is only stable because marks do not change speed (ADR-0027) and
overclocking is a trade rather than an expectation (ADR-0028). Those two decisions are what make a
quoted second meaningful three tiers later; if either is ever revisited, every time in
`PROCESSES.md` has to be requoted.

Throughput therefore has to come from parallel, multiblocks and route choice, which is the intended
shape. The cost is that `PROCESSES.md` cannot quote power: FU cost is deferred until the Flux API
exists, because guessing it before the energy layer is written produces numbers nobody can trust.

---

## ADR-0035 — Routes compose from a beneficiation stage and a reduction stage

*2026-10-01 · Accepted*

**Context.** The design requires several meaningfully different routes to every important product
(ADR-0021, and the route rule in `MACHINES.md`). Written as whole routes, that is quadratic
authoring: five ways to prepare an ore times four ways to reduce it is twenty chains to specify,
balance and maintain, and every new preparation step multiplies the existing work.

Twenty authored chains is also how a recipe viewer becomes unreadable.

**Decision.** An ore route is **two independent stages**, chosen separately:

```
yield (u metal per u raw) = feed grade × reduction factor
```

Beneficiation (B0–B4) sets a **grade** on the output form. Reduction (R1–R4) applies a **factor**.
Nine authored process families produce twenty routes, and adding a sixth preparation adds four
routes for one unit of work.

**Alternatives rejected.** Authoring whole routes (quadratic, and the combinations that nobody
thought to author become the ones players ask for); a single linear upgrade ladder as in most ore
mods (only one axis, so there is only ever one correct answer and no decision); additive bonuses
instead of multiplicative (makes late beneficiation worthless, since a flat bonus on a large number
is noise).

**Consequences.** Grade must be a property of the *form*, not of the stack — which is exactly what
ADR-0032 provides — so composition needs no per-item state and belts stay cheap. The generated
recipe set is per-stage, so the Atlas composes routes at query time rather than enumerating them,
and a "route" is a path through the graph rather than an object.

The risk is legibility: twenty routes are cheap to build and expensive to *present*. The Atlas has
to show five options and four options, never a list of twenty, or this decision trades an authoring
problem for a UI problem. That is recorded as an open question in `PROCESSES.md`.

---

## ADR-0036 — Every byproduct must have a named sink

*2026-10-01 · Accepted*

**Context.** Byproducts are what make routes interact, and that interaction is a stated design goal:
the messy route is often correct precisely because you want its waste. But byproducts are also the
most reliable way a complex mod generates busywork. A stream the player can only delete produces
exactly one behaviour — switch on auto-void and never think about it again — and at that point the
byproduct was not content, it was a config toggle with extra steps.

Worse, it teaches the player to void *everything*, including the streams that mattered.

**Decision.** A process may only emit a byproduct if that byproduct has **at least one named sink
elsewhere in the graph**, documented in the byproduct ledger in `PROCESSES.md`. "Deconstruct it into
Matter" does not count as the named sink: it is the universal backstop and it is always available,
so accepting it would make the rule vacuous.

If no real sink exists, the process does not emit the byproduct at all.

**Alternatives rejected.** Byproducts with no use, as a realism flourish (realism that generates a
chore is a bad trade in a mod defined by ADR-0021); pollution or waste-disposal mechanics (a
maintenance tax, which AGENTS' own stance on chores forbids); making all byproducts universally
valuable (removes the trade that makes route choice interesting).

**Consequences.** The ledger becomes a real constraint on adding processes: a new process that
produces a new residue cannot ship until something consumes it. That is a feature — it keeps the
graph connected, and connectedness is what makes a factory a system rather than a set of lines.

Auto-void stays off by default (ADR-0018) and is now genuinely defensible, because voiding is
always the player discarding something that had a use. The Atlas can therefore flag a voided stream
and say what it was worth, which turns a silent loss into a visible decision.

---

## ADR-0037 — Flux amounts are `long` and clamp at the FE boundary

*2026-10-01 · Accepted*

**Context.** Forge Energy stores and transfers `int` amounts, and the obvious thing is to match it,
since ADR-0006 makes FU and FE the same size. That works until the Flux ladder is taken seriously.

F9 is 2 097 152 FU/t. A buffer sized at a few seconds of throughput for a top-tier machine is in the
hundreds of millions, and a Singularity Reactor or a planetary grid buffer is plainly past
2 147 483 647. An `int` does not overflow politely: it wraps to a negative number, so a full buffer
reads as a debt and the energy is silently created or destroyed. That is the identical failure mode
ADR-0006 rejected a non-unit conversion ratio to avoid, arriving from the other direction.

**Decision.** `FluxStorage` is `long` throughout. Conversion to FE **saturates** at
`Integer.MAX_VALUE` rather than wrapping, and every caller treats a conversion result as a
*request*, believing the amount the other side reports as moved rather than the amount asked for.

**Alternatives rejected.** `int` amounts with a scaling factor on large buffers (reintroduces
rounding, which is exactly what ADR-0006 forbids); `BigInteger` (allocation per transfer, twenty
times a second, on thousands of machines); capping the Flux ladder so `int` suffices (lets a
storage-type choice dictate game design, and the long ladder is a stated goal).

**Consequences.** A single transfer to or from a foreign FE machine cannot exceed about 2.1 billion
FU. That is not a real limitation — no vanilla-shaped machine moves that much in one tick — and it
is correct behaviour rather than a cap: the transfer is simply smaller than requested, and the next
tick moves the rest.

Internal Grindless transfers never clamp, because both sides are `long`. The clamp exists only at
the boundary with another mod's energy system, which is the only place the width difference is
real. Saturation is the specific choice here: it makes an over-large request smaller, never
negative, so a clamp can never manufacture or destroy energy.

---

## ADR-0038 — One Flux ladder, with voltage names as aliases

*2026-10-01 · Accepted*

**Context.** The design accumulated two power scales. The README describes **voltage tiers** LV, MV,
HV, EV and IV at 32, 128, 512, 2 048 and 8 192 FU/t, while `MACHINES.md` describes the **Flux tier
ladder** F0–F9, whose F1–F5 are 32, 128, 512, 2 048 and 8 192 FU/t.

They are the same numbers. Implementing both would create two enums that must agree forever, and
enums that must agree forever eventually do not — one gains a tier, a tooltip reads from the wrong
one, and the player sees a machine that needs "HV" next to a cable rated "F3" with no stated
relationship.

**Decision.** There is **one** ladder, `FluxTier` F0–F9. LV through IV are display aliases on F1
through F5, carried as a nullable field on the enum. Tiers above F5 have no alias, which is correct:
the voltage vocabulary was never defined past IV.

**Alternatives rejected.** Two enums with a conversion function (the drift problem, merely delayed);
deleting the voltage names (they are established in the README and in the genre's vocabulary, and a
player reading "MV machine" understands it instantly); renaming the Flux tiers to voltages (stops at
IV and cannot express F6–F9 without inventing names).

**Consequences.** One source of truth for throughput, and the aliases are presentation only. A third
axis still exists and is deliberately separate: research tiers T0–T6 gate what may be *built*, while
Flux tiers gate how much power a built thing may *accept*. Keeping those distinct is what lets a T1
Arc Furnace be fed F5 power without becoming a T5 machine.

Under-volting is implemented here as a smooth curve — half throughput per tier of deficit — rather
than as a hard gate, matching the README. A machine fed too little power always runs, just slowly,
so a power shortfall is never a wall.

---

## ADR-0039 — Forge 1.20.1 is the only build target

*2026-10-01 · Accepted*

**Supersedes [ADR-0001](#adr-0001--architectury-for-multiloader-support) in part, and narrows
[ADR-0006](#adr-0006--flux-units-convert-to-fe-at-11-over-a-hand-written-bridge).**

**Context.** ADR-0001 chose multiloader reach: Fabric and Forge from one `common/` module. The cost
of that reach was not visible while the project was a registry skeleton. It became visible the
moment real platform code arrived.

Writing the Flux energy bridge meant writing it twice, against two unrelated models — Forge
capabilities with `int` amounts, and Team Reborn Energy with `long` amounts and a transaction
system. The Fabric half needed a `SnapshotParticipant` to be correct under transaction rollback,
which is a genuinely subtle piece of code with no Forge counterpart and no way to test it from the
Forge side. And `:fabric:build` was already failing with `Failed to remap 57 mods`, an opaque
toolchain failure that had nothing to do with the mod's own code.

So the second loader was costing double implementation, double review, double failure surface and a
broken build, in return for an audience the project does not yet have — nothing is playable, and
every block registered so far is an inert placeholder.

**Decision.** Target **Forge 1.20.1 only**. Remove the `fabric` subproject, its sources, its loader
metadata and the TeamReborn Energy dependency. `enabled_platforms=forge`.

**Keep the `common/` + `forge/` split and Architectury.** This is the part that looks like a
contradiction and is not: the split costs nothing now that it exists, `common/` already holds ~95%
of the code, and `@ExpectPlatform` is still the mechanism that keeps loader-specific code out of it.
Collapsing the two modules would mean rewriting every registry class against Forge's own
`DeferredRegister` — real work, today, to buy nothing, and it would make re-adding a loader a
rewrite instead of a build-file change.

**Alternatives rejected.** Keeping Fabric and fixing the remap failure (pays the double-implementation
cost indefinitely, for an audience that cannot play the mod yet); dropping Architectury entirely and
merging `common` into `forge` (a rewrite with no benefit, and it forecloses a future port); targeting
Fabric instead (Forge 1.20.1 has the larger modpack ecosystem for this genre, and NeoForge 1.20.1
loads the Forge jar unchanged, so one jar already serves two loaders).

**Consequences.** One bridge, one build, one failure surface. `:forge:build` goes green in about
50 seconds where the two-platform build failed outright. NeoForge 1.20.1 users are unaffected, since
ADR-0002 already established that they load this jar directly — so "Forge only" still means two
loaders in practice.

The cost is real and worth stating plainly: **Fabric support is gone, and restoring it means
rewriting the Fabric energy bridge**, including the transaction-rollback handling that was deleted
with it. That is a deliberate trade of future optionality for present velocity, taken while the
project is small enough that the rewrite would be cheap.

One oddity is left behind. `common/build.gradle` still depends on `fabric-loader` at compile time,
because Architectury Loom builds the common project in a Fabric-shaped environment regardless of
which platforms are enabled. Nothing Fabric ships in the jar. It is commented in place, because it
looks exactly like something a later session would "clean up" and then spend an afternoon
rediscovering.

---

## ADR-0040 — Running out of band costs time, never yield

*2026-10-01 · Accepted*

**Context.** `MACHINES.md` says a recipe run at the edge of its condition band "still works, with
reduced yield, longer time or extra byproducts". That is three options, and the implementation has
to pick one. The obvious pick is reduced yield, because it is what most mods do and it feels like a
natural penalty.

It is the wrong one here, and the reason is ADR-0034. Every ratio in `PROCESSES.md` is quoted per
unit of primary input — B3 × R3 yields 3.12 u of metal per unit of raw ore. If running slightly off
optimum silently produced less metal per ore, then **every one of those numbers becomes conditional
on tuning**, the Atlas's line solver cannot be trusted, and a player comparing two routes is
comparing two numbers that do not mean what they say.

It is also the worse failure to debug. A factory running 10 % slow is visible in a throughput
readout. A factory quietly returning 2.9 u instead of 3.12 u is invisible until someone does the
arithmetic by hand — which is the exact activity this project exists to remove.

**Decision.** A condition mismatch inside the tolerance zone reduces **speed only**. Outputs are
unchanged in both kind and quantity. Efficiency is a multiplier in `[0.25, 1]` applied to process
rate, and `PROCESSES.md`'s ratios hold at every point inside the band.

The remaining two options stay available as *per-recipe* effects rather than as the global rule: a
specific recipe may name an extra byproduct at the edge of its band when that is chemically
meaningful, which is a deliberate authored choice rather than a silent tax on every process.

**Alternatives rejected.** Reduced yield as the global rule (breaks every quoted ratio, and fails
invisibly); outright failure outside the optimal band (turns tuning into a wall, which is the
tedium ADR-0021 forbids, and wastes the tolerance zone entirely); scaling power draw instead of
speed (power is not yet implemented in balance terms, and a slow machine is more legible than an
expensive one).

**Consequences.** Tuning a machine is worth doing and never punishing. The material balance of the
whole graph is independent of how well anything is tuned, so the Atlas can solve a line from the
recipe graph alone and be right.

Efficiency is the **minimum** across the dimensions a recipe names, not their product. Two
dimensions slightly off should not compound into a crawl, and a minimum has a single identifiable
cause the machine can name — which is what ADR-0041 then reports.

The floor is 0.25, not zero: the edge of tolerance is four times slower, which is a real cost
without ever being a stall. A stalled machine that reports "running" is the worst outcome of all.

---

## ADR-0041 — A condition check returns a named fault, not a boolean

*2026-10-01 · Accepted*

**Context.** The natural signature for "can this recipe run here?" is a boolean, and the natural
signature for "how fast?" is a double. Both are easy, and both throw away the only information the
player actually needs when a factory stops.

`MACHINES.md` already identifies this as the single most common failure in complex packs — "my
factory stopped and I do not know why" — and commits to distinguishing blocked, starved and
out-of-band everywhere. A boolean cannot carry that, and neither can an efficiency of `0.0`.

**Decision.** Every condition check returns a `ConditionReport`: a named `ConditionFault`, the
limiting `ConditionDimension`, and the speed multiplier.

Faults carry a **direction**, not just a dimension. `TOO_COLD` and `TOO_HOT` are different faults
because they have different fixes and different in-world tells — too cold and the reaction does not
start, too hot and the product decomposes into something visibly wrong.

`OUTSIDE_ENVELOPE` is deliberately separate from the ordinary faults. The ordinary faults mean the
machine *could* reach the condition and currently is not, so the fix is a dial. `OUTSIDE_ENVELOPE`
means it never can, so the fix is a chassis upgrade or a different machine (ADR-0027). Collapsing
the two would send a player hunting for a setting that does not exist.

When a process runs below full speed, the report names the **one** dimension responsible, which is
what makes "running at 62 % — limited by Temperature" possible instead of an unexplained number.

**Alternatives rejected.** A boolean plus a separate query for the reason (two calls that can
disagree, and the second one gets forgotten at exactly the call sites that needed it); throwing on
mismatch (a mismatch is an ordinary game state, not an error, and this runs every tick); an
efficiency of zero as the failure signal (indistinguishable from a stalled-but-valid process, and
carries no cause).

**Consequences.** Machine status output, the logic signal, the Process Atlas's reachability display
and the multiblock simulator can all be built on one type, so they cannot drift into describing the
same failure three different ways.

The reports are allocated per check and checks run per machine per tick. The record is small and
short-lived, and the optimal case is a shared constant, so this is expected to be fine — but if
machine counts ever make it measurable, the fix is caching the report until an input or a setting
changes, not returning to booleans.

Fault messages live on the enum for now. They move behind translation keys when the client layer
exists; the enum is the single place that has to change.

---

## ADR-0042 — Machines subscribe to ticking; they do not tick by default

*2026-10-01 · Accepted*

**Context.** Grindless is aiming at bases with thousands of machines. The default Minecraft shape —
a `BlockEntityTicker` that runs every tick on every machine — means each one polls its inventory,
looks for a recipe and checks its neighbours sixty times a second whether or not anything has
changed.

The arithmetic is unforgiving. Ten thousand machines at thirty microseconds each is three hundred
milliseconds, or six times the entire 50 ms tick budget, spent on machines that are *doing nothing*.
This is the characteristic way a tech mod ruins a server, and it cannot be fixed later by
optimising the work: the fix has to be not doing it.

**Decision.** A machine holds a list of **tick subscriptions** and runs only those. Work is
subscribed when something makes it necessary and unsubscribed the moment it is not, so an idle
machine's tick is a check that the list is empty.

Subscriptions are re-evaluated from **change notifications** — a buffer gaining its first item, a
side being reconfigured, a neighbour appearing — never by polling, since polling to discover
whether polling is needed defeats the purpose.

Periodic work additionally uses a **per-machine offset** derived from block position, so throttled
work is spread across ticks instead of landing on all of them at once.

**Prior art.** This is GregTech CEu Modern's design, adopted deliberately rather than reinvented.
Their developer documentation states it directly — *"for the sake of performance, our machines are
no longer always in a tickable state. We introduced `ITickSubscription` for managed tick logic"* —
and their machines pair it with inventory change listeners that call an `updateSubscription` method.
Their throttles read `getOffsetTimer() % 5 == 0` rather than using raw game time, which is the
offset idea above.

**Alternatives rejected.** Ticking everything and optimising the work (moves the constant, not the
shape — ten thousand fast no-ops is still ten thousand); throttling with raw game time
(`gameTime % 20 == 0` synchronises *every machine in the world* onto the same tick, so average load
drops twentyfold and the worst tick does not drop at all, which is exactly what players feel);
ticking machines only in loaded chunks (already true, and irrelevant — the loaded base is the
problem).

**Consequences.** An idle base is nearly free, and cost tracks activity rather than machine count,
which is the property that lets the design keep promising large factories.

The cost is a real correctness burden. **A machine that forgets to resubscribe is silently broken**,
and broken in the worst way to diagnose: it does nothing, which is also what it does when correct.
Every input to a subscription decision must notify, so the rule is that a setter which can change
whether work is needed must re-evaluate the subscription. This is more invasive than it sounds and
is the main thing a reviewer should check in machine code.

Subscription work frequently cancels itself — that is how a machine goes idle — so the tick loop
tolerates cancellation and subscription during iteration, deferring list cleanup until the pass
ends.

---

## ADR-0043 — Recipe lookup is indexed and cached, never a linear scan

*2026-10-01 · Accepted*

**Context.** Recipes are generated at runtime from tags (ADR-0005), across every material in the
pack, multiplied by the form and anion matrices (ADR-0032) and the twenty composed ore routes
(ADR-0035). The recipe set will be large — plausibly tens of thousands of entries in a big pack —
and it is not known until the world loads.

"Which recipe matches these inputs?" asked by scanning that list is O(recipes) per machine per
attempt. It is also the question a machine asks most often, because an idle machine with inputs it
cannot use asks it forever.

ADR-0042 removes the cost for machines with *nothing* to do. It does nothing for a machine holding
items that match no recipe, which is a normal and common state.

**Decision.** Three layers, decided now so that step 15 implements them rather than discovering
them:

1. **An index, not a list.** Lookup is keyed by input so a search inspects candidates, not the
   catalogue.
2. **A cached last recipe.** Before searching, retry the recipe this machine ran last and check
   whether it still matches. A machine in steady state — which is nearly all of them, nearly all
   the time — then never searches at all.
3. **A negative result that sticks.** When a search fails, the machine does not retry every tick.
   It waits for an input change, or for a throttled retry on its offset timer.

**Prior art.** Both reference implementations do this, by different routes. GregTech CEu stores
recipes in a trie — `Map<AbstractMapIngredient, Either<GTRecipe, Branch>>`, descended ingredient by
ingredient — and its `RecipeLogic` checks `lastRecipe` before searching, keeps `lastFailedMatches`,
and falls back to `getOffsetTimer() % 5 == 0` for the retry when nothing matched. Mekanism reaches
the same place with typed `InputRecipeCache` classes keyed by item, fluid or chemical, in single,
double and triple input shapes.

That two mature implementations converged independently is the strongest evidence available that
the linear scan is not survivable at this scale.

**Alternatives rejected.** Scanning with an early exit (still O(recipes) in the failing case, which
is the case that repeats); caching only the last recipe without an index (the first search after
any input change is still a full scan, and that is exactly when a player is watching); precomputing
every input combination (combinatorial, and the material set is not known until load).

**Consequences.** The index must be rebuilt when recipes reload — world load and `/reload` — and a
stale index is a wrong-recipe bug rather than a crash, so rebuilding is tied to the recipe manager
rather than done opportunistically.

Condition matching stays **outside** the index. Conditions select which *machine* can run a recipe
(ADR-0020), not which recipe matches the inputs, and they are cheap to evaluate once a candidate
exists. Indexing six dimensions of continuous conditions would be an enormous structure answering a
question nobody asks.

The cached recipe must be invalidated when the machine's conditions or upgrades change, not only
when its inputs do — otherwise retuning a machine leaves it running the recipe it found before, which
would be a genuinely confusing bug.

---

## ADR-0044 — Neighbour lookups are cached against capability invalidation

*2026-10-01 · Accepted*

**Context.** A machine that pushes power or items to an adjacent block has to find that block.
`Level.getBlockEntity(BlockPos)` is a chunk lookup plus a map lookup — negligible once, and not
negligible done six times a tick by thousands of machines to rediscover something that only changes
when a player breaks a block.

The obvious fix is to cache the neighbour in a field. The obvious fix is also a correctness bug:
the cached reference survives the neighbour being broken, and the machine goes on pushing into a
block that no longer exists. This is the well-known cause of *"my machine stopped working until I
broke and replaced it"*.

**Decision.** Cache the neighbour, and invalidate the cache from the platform's own capability
invalidation rather than by re-checking.

Forge hands a capability out as a `LazyOptional` and calls `invalidate()` on it when it stops being
valid; holders register interest with `LazyOptional.addListener`. Grindless resolves a neighbour
once, registers a listener, and drops the cache when it fires.

Two paths are needed, not one. Invalidation covers a capability being **revoked**. It cannot cover
a block **appearing** where there was none, because there was nothing there to invalidate — so the
machine's neighbour-changed event also clears the cache. A cache that only listens to invalidation
never notices a new neighbour and looks exactly as broken as a stale one.

Absence is cached too: a machine facing a wall must not re-ask the wall every tick.

**Alternatives rejected.** Looking the neighbour up every tick (the cost this exists to remove);
caching without invalidation (stale references, and the failure is intermittent and
unreproducible, which is the worst kind of bug report); caching with a time-to-live (picks an
arbitrary number, and is simultaneously too slow to be correct and too fast to be cheap);
validating the cache by checking the block entity is still alive each tick (that check *is* the
lookup, so it saves nothing).

**Consequences.** Neighbour access becomes a field read in the steady state, which is what makes
per-tick pushing affordable at all.

The cost is that cache lifetime is now a real invariant with two independent triggers, and getting
either wrong produces a bug that is invisible in code review and intermittent in play. The
invalidation listener must not assume the block entity still exists when it fires, since the usual
reason it fires is that the block entity is being removed.

NeoForge 1.20.1 loads the Forge jar unchanged (ADR-0002), so this works there as written. Newer
NeoForge replaces this mechanism with `BlockCapabilityCache`, which does the same job with the
invalidation handled for you — a port target rather than a problem.

---

## ADR-0045 — Capabilities are attached by event, not overridden on the block entity

*2026-10-02 · Accepted*

**Context.** Other mods reach a Grindless machine's energy buffer through Forge's
`ForgeCapabilities.ENERGY`. The textbook way to provide one is to override `getCapability` on the
block entity.

That is not available here. Machine block entities live in `common/`, which must not import
loader-specific classes (ADR-0001), and `getCapability` is pure Forge. The constraint is not
theoretical: writing `MachineBlockEntity` produced exactly this mistake one layer down, where
`onLoad()` looked like a vanilla hook and turned out to be a Forge addition. The compiler caught
that one. It would not have caught a design that moved every machine into `forge/`.

**Decision.** Attach the capability from `forge/` with `AttachCapabilitiesEvent<BlockEntity>`.
Forge fires it as each block entity is constructed, so a listener there can attach an
`IEnergyStorage` view of any `MachineBlockEntity` without that class knowing Forge exists.

The attached provider **must** also register through `AttachCapabilitiesEvent.addListener`, which
is the load-bearing half. The chain was read in Forge's source rather than assumed:
`BlockEntity.setRemoved()` calls `invalidateCaps()`; `CapabilityProvider.invalidateCaps()` marks
itself invalid and calls `CapabilityDispatcher.invalidate()`; that runs every `Runnable` registered
by `addListener`; ours invalidates the `LazyOptional`, which fires the listener `NeighbourCache`
put on it (ADR-0044) and clears the stale reference.

Attaching without that listener compiles, works in testing, and leaks stale references in play.

**Alternatives rejected.** Overriding `getCapability` on the block entity (requires a Forge import
in `common/`, which is the rule ADR-0001 exists to enforce); an `@ExpectPlatform` capability hook
(the capability system is shaped around `LazyOptional` and invalidation callbacks, so the stub
would either leak those types into `common/` or be too lossy to invalidate correctly); moving
machine block entities into `forge/` on the grounds that ADR-0039 made the project Forge-only
(ADR-0039 explicitly kept the `common/` + `forge/` split so a future port stays a build change
rather than a rewrite, and block entities are most of what would have to move).

**Consequences.** `common/` stays loader-clean with no `@ExpectPlatform` stub for this, and the
whole Forge-facing surface is one class.

This also forced a latent bug into the open, which is the usual value of making something real.
The Hand Crank Dynamo stored its charge in a field separate from its Flux buffer, so exposing that
buffer would have published an empty load: insertable, never read, and holding none of the power
the dynamo had actually generated. Generation now writes to the buffer itself, and generators
override the buffer's direction — no external insertion, extraction at the rated tier — so the
object the machine fills is the object the world pulls from.

Two limits worth stating. The capability is offered on every face: the container contract can
express per-face energy gating, but honouring it needs the capability re-invalidated whenever a
face is reconfigured, and there is no screen to reconfigure one from yet. And the event fires for
*every* block entity in the game, so the listener must stay a cheap `instanceof` — Forge's own
documentation warns about this, and it is the one place where adding work would be felt
world-wide.

---

## ADR-0046 — The pylon index is chunk-bucketed, and brownouts resolve in two phases

*2026-10-02 · Accepted*

**Context.** ADR-0007 settled *where* network state lives — level-wide `SavedData` with a spatial
index, so there is no per-tick graph walk and an unloaded chunk cannot fragment a network.
Implementing it surfaced four questions that record did not answer.

**Decisions.**

**1. The index is a chunk bucket, not a tree.** Each pylon is filed under every chunk its supply
cube touches; a lookup hashes the query position's chunk once and exact-tests the few pylons filed
there. Supply cubes are small — an MK3's 64-block cube spans at most five chunks a side — so
buckets stay short and cost tracks *local density* rather than world size. A balanced tree would
also satisfy ADR-0007's `O(log n)`, at the price of rebalancing on every placement and far more
code for a worse constant.

The bucket is coarser than the cube, so a pylon can be filed under a chunk it only clips. The
exact per-axis test still runs on every candidate; skipping it is a subtle over-coverage bug that
would mostly work.

**2. Demand is registered before any of it is served.** Machines declare their intended draw, then
the network resolves one satisfaction fraction, then everyone draws at that fraction. Serving
machines as they tick instead would mean the ones that happen to tick early run at full speed
while the rest stop dead — which is exactly the individual starvation the README rejects, where a
random subset of the base stops with no indication why. Two phases is what makes "everything is
visibly sluggish" true rather than aspirational.

**3. Energy follows capacity when a network splits.** A fragment holding a third of the pylons
keeps a third of the charge. Giving it all to one fragment destroys energy; giving each fragment
the full amount manufactures it, and a player who noticed could break and replace a pylon in a
loop. Merging moves energy across for the same reason — if merging lost power, the optimal play
would be to drain a network before linking it, which is absurd.

**4. Where supply areas overlap, the first covering pylon wins.** Splitting a machine's draw
across overlapping networks would make its behaviour depend on index iteration order, which is
unpredictable to a player and untestable in practice. Overlap is a layout the player chose; it
should be stable, not clever.

**Consequences.** Placement and removal are the only expensive operations, and removal is the
expensive one — it recomputes connectivity by flood fill. That is the right place to pay: it
happens when a player breaks a block, not every tick.

Capacity is *derived* from the member pylons rather than stored, so it cannot drift out of step
with them, and a shrinking network clamps its contents rather than holding energy it no longer
has room for.

The saved form stores membership and rebuilds the index on load, so the two cannot disagree. Tier
ordinals are bounds-checked on read, because an unchecked enum ordinal from a future version
throws during world load — which a player experiences as a corrupt save rather than as a mod bug.

---

## ADR-0047 — Veins are long-lived, and deepening competes with relocating

*2026-10-02 · Accepted*

**Context.** Resource Genesis replaces mining with extraction from a chunk that depletes toward a
nonzero floor, and the README frames the resulting pressure as a virtue: a worked vein is slow, so
the efficient move is to expand outward, and the game becomes exploration and layout rather than
tunnelling.

That argument has a failure mode it does not address. If veins run down quickly, "expand outward"
stops meaning exploration and starts meaning **abandon, relocate, rebuild the same layout
somewhere else, repeat**. Rebuilding a layout you have already solved is repeating an action whose
outcome you already know, which is this project's own definition of grind (ADR-0021). The system
designed to delete mining would have reinvented it with extra steps.

The depletion rule was also written without numbers, so "slowly" could have meant anything.

**Decision.** Three changes, all aimed at making an outpost something you build and then stop
thinking about.

1. **Veins are long.** A chunk holds roughly forty hours of continuous T1 extraction before
   reaching its floor. The reserve is sized in hours against a reference rate rather than picked
   as a round number, so the figure stays meaningful when rates are tuned.
2. **The decay curve is gentle early and steep late**, not linear. A quarter of the reserve gone
   still yields 96 % of the original rate and half yields 83 %; the slowdown only becomes obvious
   past three quarters. Linear decay is noticeable from the first hour and makes a player feel
   permanently on a clock, which is the opposite of the intended rhythm.
3. **The floor is 30 %**, and a **Deep Bore** upgrade raises it — 45 % with one, 60 % with two,
   capped at 75 %. A depleting outpost therefore has two answers instead of one: deepen it, or
   found another.

**Richness scales reserve as well as rate**, which makes a vein's lifetime independent of its
richness. A rich chunk is a find rather than a countdown, and the player is never asked to weigh
"rich but short" against "poor but long" — a false choice that adds arithmetic without adding a
decision.

**Alternatives rejected.** A token floor around 10 % (an old outpost becomes a monument rather
than a contributor, and the treadmill returns); no floor at all (every extractor eventually
becomes litter a player has to go and tidy up, which is a chore); infinite veins with no decay
(removes the reason to expand, and the horizontal pressure is the part of this design worth
keeping); making relocation cheap through better build tools (treats the symptom — the problem is
being asked to re-solve a solved layout, not how long it takes).

**Consequences.** The Deep Bore cap is what keeps the decision alive. Without it a stack of
upgrades would make one chunk effectively infinite and expansion would stop being necessary at
all; with it, deepening buys time rather than permanence, and territory still matters.

Deep Bore buys nothing at a fresh vein, which is deliberate — it has to be a mid-game answer to a
mid-game situation rather than something installed reflexively on day one, so it costs an upgrade
slot and constant upkeep for no benefit if fitted early.

The risk to watch is pacing. Forty hours is long enough that a player may never see depletion in a
short playthrough, which makes the Deep Bore upgrade dead content for them. That is the right way
round — far better than the alternative — but if testing shows the depletion curve is never
experienced at all, the reserve comes down rather than the floor.

---

## ADR-0048 — Assets are generated from the JDK, with a named list of what cannot be

*2026-10-02 · Accepted*

**Context.** The README has committed since day one to original, script-generated assets, and gave
good reasons: consistency across a hundred-plus blocks, a palette change as a one-line edit, and
unambiguous provenance. What it never did was establish whether that is actually *achievable*, or
what happens to the parts it cannot cover. Art is the largest unexamined risk in the project — a
mod is judged on its first screenshot long before anyone reads its recipe graph.

This was investigated rather than assumed. The findings changed the plan twice.

**Finding 1: there is no Python and no Node on the toolchain.** A generator written in either
would add a dependency the project does not otherwise have, on a machine that cannot currently run
it.

**Finding 2: the JDK is enough, and that is better anyway.** `javax.imageio` writes PNG and
`javax.sound.sampled` writes WAV with no third-party library at all. A generator in Java uses the
toolchain the project already requires, can run as a Gradle task, and is checked by the same
compiler as the mod.

**Finding 3: audio is generatable, which contradicted the initial assessment.** Sound was written
off as impossible before being tried. A probe produced a seamless one-second machine hum (summed
harmonics at exact multiples of the loop frequency, so the ends join without a click) and a pylon
link chirp (a rising sweep under a decaying envelope). Synthesised industrial sound — hums, clicks,
relays, charge-ups, alarms — is squarely within reach. It is *tonal* sound that is hard, not sound.

**Decision.** Generate assets from a committed Java tool, run by Gradle, covering:

- **the form × material matrix** — ingots, plates, gears, dusts and the rest of ADR-0032's axis,
  across every material the pack provides. This is the bulk of the catalogue by count and the part
  where consistency *is* the quality;
- **machine faces and casings** — composed from a base plate, a recessed panel, vents, rivets and
  an emissive overlay, with active and idle variants;
- **block models and blockstates**, which are declarative JSON;
- **sound effects** of the industrial kind: hums, charge-ups, relay clicks, alarms.

And name, explicitly, what this approach **will not** produce to a shippable standard:

| Not generated | Why |
| --- | --- |
| Hero item sprites — the Multitool, the Research Terminal, the Prospector's Scanner | A script yields something legible, not something with character. These are what a player sees in the first screenshot. |
| Complex models — drone arms, turbines, the Space Elevator | Blockbench work. A generator produces boxes. |
| Entity animation | Keyframes are authored, not derived. |
| Music, and any voiced or recorded sound | Synthesis covers industrial noise, not melody or recording. |
| Particle effects that look *good* | The code is writable; judging the result needs eyes on it running. |

**Alternatives rejected.** Generating everything and accepting the result (the catalogue would be
consistent and characterless, and first impressions are the whole marketing budget of a mod);
hand-authoring everything (hundreds of form × material cells is exactly the repetitive work this
project refuses to inflict on *players*, and it would not survive a palette change); vendoring
another mod's assets or anything under an unclear licence (not negotiable — the README's provenance
claim is a promise, and third-party art would break it).

**Consequences.** The generator is written **early rather than at step 19**. If the approach is
going to fail it should fail at twenty blocks, not at three hundred, and the texture style
constrains block model design — discovering that late would mean redoing both.

The list above is the real output of this record. A future session can now see at a glance that
hero art and complex models are *known gaps with no owner*, rather than assuming assets are
handled because a generator exists. Those gaps are where a human artist would be worth finding;
everything else is deliberately not worth an artist's time.

Any third-party asset that ever enters the repository must be CC0 or equivalent, with its source
recorded. Sound effects sourced rather than synthesised fall under the same rule.


---

## ADR-0049 — A green build must include a booted server

*2026-10-02 · Accepted*

**Context.** Until now "verified" meant two things: `./gradlew build` succeeds, and the behaviour
checks in `tools/checks` pass. Both were true — 309 checks, a 25 second build — while the mod could
not start. `ModCreativeTabs.register()` called `get()` on registry objects during mod construction,
before the registry was populated, and Forge refused to create the mod instance with
`Registry Object not present: grindless:multitool`.

Neither existing gate can see that class of bug, by design. The checks run `common` classes in a
plain JVM, which is why they need no Minecraft; the build only compiles. Nothing instantiates
Forge, so nothing exercises registration order, capability attachment, `@ExpectPlatform`
resolution or anything else that only exists once the loader is running.

A second bug of the same family was found by running the game rather than reading it: a pylon's
network membership was mirrored in a transient `registered` flag, restored by a first-tick hook
that pylons never reach because they have no ticker. After a world reload the flag read false, so
breaking the pylon did nothing and the network kept a ghost. It passed every check, because the
checks never reload a world.

**Decision.** A change is not verified until the dedicated server has booted with the mod loaded.
`tools/smoke-boot.sh` starts `:forge:runServer` headless, waits for `Done`, sends `stop`, and fails
on `Mod Loading has failed` or an early exit. CI runs it after the build and the checks, on Linux.
It writes `eula.txt` only when `GRINDLESS_ACCEPT_EULA=true` or `CI=true`, because that records
acceptance of the Minecraft EULA on the caller's behalf.

State that must survive a reload lives in saved data and nowhere else. A block entity may cache
it, but must be correct with an empty cache, because a block entity is rebuilt on every chunk load
and its first-tick path is not guaranteed to run.

**Alternatives rejected.** Forge GameTests (the better long-term home for reload scenarios, but
they need infrastructure the project does not have yet, and the smoke test catches the failure
that matters today for thirty lines of shell); relying on the client to find it (a person has to
launch a game to learn the mod does not load, which is the situation this record exists to end);
adding `get()` guards at each call site (treats one symptom — registration order — and leaves the
class of bug unobserved).

**Consequences.** CI needs a Linux job with JDK 17 and a few minutes for the first remap. The
script also makes it cheap to run the same check locally on Linux and macOS; on Windows it runs
under WSL or Git Bash. `tools/run-checks.sh` is the Linux counterpart of `tools/run-checks.ps1`, so
the checks are no longer Windows-only. Reload scenarios still need a person or a future GameTest;
the smoke test proves the mod boots, not that every system survives a restart.

---

## ADR-0050 — Grindless supplies a material only where the pack has none

*2026-10-02 · Accepted*

**Context.** Grindless must work beside vanilla and any other mod, not merely tolerate them. Tin
should be the tin a pack already has; copper and iron should be vanilla's; platinum should come
from whichever mod adds it, and from Grindless only when none does. Recipes must be satisfiable by
another mod's equivalent item, and installing Grindless must not overwrite or duplicate anything.

ADR-0004 and ADR-0033 decided that materials are resolved from tags. They left three things open
that this record closes: items must be registered before any tag is read, so "register only if
missing" cannot be literal; the conventions are not equally followed (`forge:ingots/tin` is
universal, `forge:bolts/tin` does not exist); and nothing said how a recipe's output is chosen when
several mods provide the item.

An audit found the shipped assets already broke the rule: the generator produced textures for
vanilla's own iron ingot, gold ingot, gold nugget and copper ingot.

**Decision.** Split by *slot* — one material in one form — and decide each slot at two times.

At registration, a fixed **supply set** (`SupplyCatalogue`) is registered whatever else is
installed. Registry IDs are written into worlds, so they must not depend on the mod list, or
removing a mod would turn every Grindless item in a save into a missing mapping. The set never
includes what vanilla already has: no iron ingot, gold ingot, copper ingot, iron or gold nugget,
or raw iron, copper and gold. Vanilla gets Grindless items only for what it lacks (iron plates,
copper nuggets). Alloys get no raw or crushed form. This is the one hardcoded list in the mod and
it lists *what Grindless can make*, not *what exists*.

At runtime, after every tag load, `MaterialRegistry` scans the tags and records, for each slot,
which items fill it. A supply item is **active** only if no other namespace fills its slot.
Inactive supply items stay registered but are hidden from the creative tab, are never chosen as
output, and (when veins arrive) are never used to make a material mineable.

Four rules follow.

1. **Inputs are tags.** A recipe ingredient that is a material in a form is `forge:ingots/tin`,
   never `othermod:tin_ingot` (`MaterialTags`). `VerifyMaterial` lints every shipped recipe and
   fails on a hard-wired item ID, and has a self-test showing the linter can fail.
2. **Outputs are unified.** One item per slot: vanilla, then namespaces the pack author names,
   then other mods alphabetically, then Grindless last (`Unifier`). Deterministic, so server and
   client agree and the choice is stable across reloads.
3. **Add, never replace.** Every shipped tag is `"replace": false` and lists only Grindless items
   or sibling tags; nothing is shipped into `minecraft:`. Grindless can add to another mod's tag
   but cannot remove from it. Checked in `VerifyMaterial`.
4. **Conventional forms are shared, the rest are ours.** A form is published under `forge:` only if
   other mods agree on it (ore, raw, dust, nugget, ingot, gem, storage block, plate, rod, gear).
   Crushed, purified, foil, bolt, ring, wire and coil have no agreement, so they live under
   `grindless:` — published so others can opt in. This is the boundary ADR-0033 drew for reagents,
   applied to forms. Promoting one is a single flag in `MaterialForm` plus a regeneration.

The generator reads the same `SupplyCatalogue`, so a texture, model or tag can neither exist for an
item that is not registered nor be missing for one that is. Item names come from two lang keys per
item (`form.grindless.ingot` and `material.grindless.tin`), so a new material costs one line of
translation.

**Alternatives rejected.** Registering conditionally on `ModList` (breaks saves when the mod list
changes); registering nothing for materials and only tagging (an exotic material would not exist
with Grindless alone, contradicting the requirement); replacing other mods' recipes or tags with
unified ones (the kind of override this record exists to forbid); a per-pack config mapping
(ADR-0033 rejected it for reagents for the same reasons); one `forge:` tag for every form
(claims tags nobody else fills, so the form is never interchangeable and the claim is a lie).

**Consequences.** With only Grindless installed there are 120 supply items and all are active.
With another mod providing tin ingots and raw platinum, 118 are active and the other two are
hidden; this is demonstrated by booting a server with a datapack standing in for that mod
(`tools/smoke/foreign-providers`, `SMOKE_DATAPACK` in `tools/smoke-boot.sh`).

Known limits. Hiding in recipe viewers needs a JEI/REI integration that does not exist yet.
`c:` tags (Fabric) are not scanned, consistent with ADR-0039. The unifier's namespace preference
has a setter but no config file until `GrindlessConfig` is real. A vanilla-backed material is
listed by hand in `SupplyCatalogue`, so a Minecraft update that adds a vanilla form, such as a
copper nugget, needs a one-line edit and a regeneration; the check `no registered name equals a
vanilla item's name` guards the known cases. A player holding a hidden supply item (from `/give`
or an old chest) still has a working item, since it is tagged, which is intended.

---

## ADR-0051 — Every block ships a floor of assets, generated from one list

*2026-10-02 · Accepted*

**Context.** Booting the server proved the mod loads, not that its blocks work. Auditing the
resources found none of the six registered blocks had a blockstate or a model (the missing-texture
cube), none had a loot table, and none was in a mining tag. Because machines set
`requiresCorrectToolForDrops`, a player could place one and then destroy it for nothing. Three
hand-registered items had no model either, and the Multitool and Data Core had no sprite.

**Decision.** `BlockCatalogue` lists each block, its model shape and its decorated face. The
generator writes, from that list alone, the blockstate, block model, item model, a self-drop loot
table, and the entry in `minecraft:mineable/pickaxe`. Two shapes exist: an orientable casing with
one decorated face, and a column with the decorated face on all four sides. Blocks have no facing
property yet, so the decorated face is always north; that is a placeholder, not a design. The
Multitool and Data Core get generated placeholder sprites, and the Machine Casing item renders as
the casing cube.

`VerifyAssets` reads `ModBlocks` and `ModItems` and fails if a registered block lacks any of those
files, if any model names a texture or parent that does not exist, or if a hand-registered item has
no display name or model. A server-boot scenario (`tools/smoke/blocks.commands`) places every block
and checks that it is in the pickaxe tag and that its loot table drops one item with its real
name.

The pickaxe tag is the only thing Grindless ships into `minecraft:`. It is `"replace": false` and
lists only Grindless blocks, so it can add to vanilla's tag and cannot remove from it. This narrows
rule 3 of ADR-0050 ("nothing is shipped into `minecraft:`") to "nothing except mining tags", and
`VerifyMaterial` now enforces the narrower form. Vanilla has no other mechanism: a block is
mineable with a tool only by being in that tag.

**Alternatives rejected.** Hand-writing the JSON (six blocks now, dozens later, and the
catalogue-versus-registry drift this removes); generating the files from `ModBlocks` itself (it
needs Minecraft and a loaded registry, and the generator deliberately needs only a JDK, ADR-0048);
leaving it to the first person to launch the client (the situation ADR-0049 exists to end).

**Consequences.** Every block renders, drops itself and mines with a pickaxe. Not covered, and
still named gaps under ADR-0048: hero models, facing and active-state variants, block-entity
renderers. Rendering itself was not run: the checks prove the files exist and agree, and the server
proves tags and loot, but only a client shows the pixels. No tool requirement tier is set, so any
pickaxe works; whether the Multitool should mine is a design question this record does not answer.


## ADR-0052 — Each machine has its own shape, state textures and effects

*2026-10-02 · Accepted*

**Context.** ADR-0051 gave every block a floor of assets, but all machines were the same casing cube
with a different face, and none showed what it was doing. `docs/MACHINES.md` already defines five
machine states (running, idle, blocked, starved, out-of-band); nothing exposed them to the player.

**Decision.**

1. *State is a block-state property.* `MachineStatus` (idle, running, blocked, starved,
   out_of_band) is the `status` property; `facing` is added where the shape has a front. The light
   level derives from the status. Changing a block state is a client-visible update, so every
   publisher goes through `StatusDebounce`: entering running or a fault is immediate, leaving
   running waits 40 ticks and recovering from a fault waits 20, so a machine flickering between two
   conditions does not spam updates.
2. *Colours mean the same on every machine; shape and accent colour say which machine it is.*
   Running is cyan, blocked is amber, starved is red. Each geometry (dynamo, extractor, terminal,
   pylon with three tiers) has its own element model and its own painter in `tools/assetgen`.
3. *Effects are client-only, driven by `animateTick`.* `MachineEffects` spawns particles and plays
   one-shot sounds per geometry and status. It references no client class, so `common` stays
   server-safe. Sounds are mono Ogg Vorbis so Minecraft can position them in 3D; the source WAVs
   live in `tools/audio` and `tools/convert-audio.sh` produces the shipped files, so they never
   need hand editing.
4. *The generator stays JDK-only and is driven by `BlockCatalogue`*, which now lists geometry, tier,
   facing and statuses per block. `VerifyAssets` checks every state, rotation and sound.
5. `pack.mcmeta` moves to the `forge` module resources: in a dev run only that module's resources
   form the mod pack, and the old location produced a "failed to load a valid ResourcePackInfo"
   warning.

**Consequences.** The hand-crank dynamo reports running, idle and blocked from its own behaviour,
and flux pylons report the state of their network (the network tick writes a pylon only when its
debounced status is stale). The crude extractor and research terminal are `MachineShellBlock`s:
they have every model, texture and effect, but nothing sets their status yet, because they have no
behaviour. Starved pylon and out-of-band have no producer either; the former needs consumers, the
latter has no visuals. Effects are sparse one-shots, not looping sounds, and a pylon in an unloaded
chunk keeps its last status until the chunk loads. Face lighting is not emissive: only the block
light level changes. A client run (software GL under Xvfb) showed the models and textures
rendering with no model errors in the log; it did not demonstrate the particles over time.


## ADR-0053 — The Crude Extractor is the first consumer

*2026-10-02 · Accepted*

**Context.** The Hand Crank Dynamo generated power and the Flux Network could carry it, but nothing
consumed it. `docs/PROCESSES.md` already specified the T0 extractor: F0, twenty seconds, one unit
of the chunk's material. Veins, depletion and Deep Bore already had checks. The extractor was a
shell with art and no behaviour.

**Decision.** The Crude Extractor is a `MachineBlockEntity` that:

1. Resolves its chunk's vein from the world seed and `MaterialRegistry.snapshot().mineable()`.
2. Draws 8 FU/t from a covering Flux Network, or from its own buffer when an adjacent dynamo
   pushes into it. T0 has no pylons, so adjacency is the bootstrap path.
3. Progresses a 400-tick cycle scaled by `vein.rateAfter` (richness and depletion) and by how much
   of the requested power actually arrived (brownouts slow it, they do not stall it).
4. Outputs the pack's preferred raw form, or the ore if the pack has no raw, via `Unifier`.
5. Holds one output slot that hoppers can pull from and that the machine pushes into neighbouring
   inventories (vanilla `Container` first, Forge item capability otherwise).
6. Publishes status through the same debounce as every other machine: idle with no vein, starved
   with no power, blocked when the product cannot leave, running otherwise. A vein never idles
   from exhaustion — that would contradict ADR-0047.

Work is subscribed while there is a vein and the output is not stuck, and dropped the moment it
is: an extractor next to a full chest costs nothing until a hopper or a neighbour change frees a
slot. `ExtractorLogic` holds the numbers so `VerifyExtractor` can check them without a world.

**Consequences.** The first playable loop exists: crank a dynamo next to an extractor, or cover
both with a pylon, and put a chest on any face. The Terrestrial Extractor (T1, faster, network-only)
is still ahead. The extractor has no menu yet; hoppers and auto-push are the interface.


## ADR-0054 — Pylons are three blocks tall and cover a factory

*2026-10-02 · Accepted*

**Context.** Pylons were a one-block post with a 16/32/64-block supply cube. That is a lamp post,
not a power pylon, and one MK1 could not cover a factory floor. Minecraft JSON models cannot
extend past 32 units on an axis, so a three-block-tall model cannot hang off a single block.

**Decision.**

1. A pylon occupies three blocks of height. The player places the base; two `flux_pylon_shaft`
   blocks occupy y+1 and y+2. The shafts have no item, drop nothing, and breaking one breaks the
   base, which drops the pylon. Only the base is a network member.
2. Each third has its own model, sliced from a 48-unit tower, because JSON cannot represent the
   whole thing on one block. The item uses the tower scaled into 16 units.
3. Shafts copy the base's tier and status so the lights stay in step. Coverage, throughput and
   link range still key off the base position.
4. Supply cubes grow to a factory scale: MK1 48³ / link 64, MK2 80³ / link 112, MK3 128³ / link 192.
   Throughput is unchanged. The shorter of two link ranges still governs, so an MK3 cannot capture
   an MK1 that cannot reach back. The cube-per-axis rule is unchanged; a player can still see the
   coverage by eye.

**Consequences.** An MK1 covers a small factory. An MK3 covers a chunk-scale base. The spatial
index files an MK3 under at most 81 chunk buckets, which is still local density rather than world
size. Existing worlds with 1-block pylons will miss their shafts until the pylon is broken and
replaced; membership is unchanged.


## ADR-0055 — The Multitool does not mine

*2026-10-02 · Accepted*

**Context.** ADR-0051 put every machine in `minecraft:mineable/pickaxe` and left open whether the
Multitool should mine them. The Multitool is the T0 handheld that replaces the stone-tool *phase*,
not the pickaxe.

**Decision.** The Multitool is not a mining tool. Machines drop only for a pickaxe (or another
item in `minecraft:mineable/pickaxe`). The Multitool stays a scanner, configurator and progression
item. Giving it mining would collapse "requires the correct tool" into "the one item does
everything", which is how the early game becomes a single hotbar slot instead of a first factory.

**Consequences.** Players keep a pickaxe. If a later session wants a mining module on the
Multitool, it is an upgrade with a cost, not the default.


## ADR-0056 — T0 bootstrap recipes are authored JSON using tags

*2026-10-02 · Accepted*

**Context.** ADR-0005 says processing-chain recipes are generated at runtime from the material
registry, not shipped as JSON. The T0 loop — dynamo, extractor, multitool, terminal, Data Core —
does not depend on that registry. It is cobblestone, wood and two iron, and it is the only part of
the graph a player hand-crafts (ADR-0017). Leaving it uncraftable meant the "ten minutes, two iron"
claim was creative-mode only.

**Decision.**

1. T0 crafting-table recipes are authored JSON under `data/grindless/recipes/`. They are the
   exception to ADR-0005, not the start of shipping the processing chain as files.
2. Anything that is a material is a tag (`forge:ingots/iron`), never `minecraft:iron_ingot`.
   Stone is `minecraft:stone_crafting_materials`, wood is `minecraft:planks`. Vanilla items that
   are not material forms (stick, glass, redstone) may be named by ID.
3. The iron budget is two: one in the dynamo, one in the extractor. The Multitool is cobble and
   sticks — it replaces the stone-tool *phase*, so it is craftable before the first iron.
4. `BootstrapRecipes` lists the same recipes as data so `VerifyBootstrap` can check the JSON
   against the decision.

**Alternatives rejected.** Generating even these five at runtime (the injector needs mixin or
recipe-manager mutation, and the recipes do not change with the pack); hard-wiring
`minecraft:iron_ingot` (a pack's iron would not craft the bootstrap, contradicting ADR-0050).

**Consequences.** A survival player with a crafting table, cobble, wood and two iron can build the
loop. T1 recipes stay unwritten until those machines exist; they will be gated on the Voltaic
blueprint (ADR-0057). Processing-chain recipes remain generated (ADR-0005).


## ADR-0057 — The Research Terminal unlocks world-scoped blueprints

*2026-10-02 · Accepted*

**Context.** The Research Terminal was a `MachineShellBlock`: art, no block entity, no behaviour.
The README has the player insert Data Cores and Flux to unlock blueprints, and has research as a
*production target* the factory feeds, not a GUI timer. T1 machines do not exist yet, so an unlock
cannot grant a recipe today — but a terminal that does nothing until step 20 would leave T0
without its progression gate.

**Decision.**

1. The terminal is a `MachineBlockEntity` at F0. One Data Core and 600 ticks at full power unlock
   Voltaic, the T1 pack. Brownouts slow it; they do not stall it and they do not consume the core
   early.
2. Unlocks are world-scoped `SavedData` on the overworld, not per-player. A hopper can feed cores,
   which is what "research is a production target" requires. Unlocking twice is a no-op.
3. There is no menu. Right-click with a core inserts it; empty-handed takes it back. Hoppers may
   insert and may not extract — the core is spent, not buffered.
4. Status: idle with no core, starved with no power, running while the cycle advances, blocked
   once Voltaic is already unlocked. The same debounce as every other machine.
5. Voltaic currently gates nothing. That is deliberate: the switch flips now, T1 recipes attach
   later. A flag nobody reads is still a real unlock; a terminal that waits for the recipes is a
   shell.

**Alternatives rejected.** Per-player research (a hopper has no player); a full tech-tree GUI
(step 20; the T0 loop does not need it); consuming the core on insert (a brownout would then
waste the core with no unlock).

**Consequences.** The first playable progression exists: craft a Data Core, power the terminal,
wait thirty seconds, Voltaic is on. T1 crafting will read `ResearchData.isUnlocked(VOLTAIC)`.
Existing worlds have an empty set, which is locked, which is correct.


## ADR-0058 — Build playable slices, not system layers

*2026-10-02 · Accepted*

**Context.** The implementation plan after T0 listed remaining work as *systems in dependency
order*: `ProcessRecipe` (15), all T0/T1 machines and menus (16), fluids (17), JEI (18),
fabrication (20), belts (21), conduits (22), multiblocks (23). Step 11 had already deferred
`ProcessRecipe` until fluids existed, because "half the ingredients are fluid stacks".

That order builds a complete energy API, a complete recipe type, a complete fluid model and a
complete multiblock framework — each of which is empty of a factory that uses it. The 0.1
definition of done is narrower and already written: *a player can go from an empty world to
automated iron in under fifteen minutes without mining it by hand.* T0 currently stops at raw ore
in a chest. The missing piece is a **dry processing line**, not a catalogue.

The T1 floor in `PROCESSES.md` is B0×R1 and B1×R1: raw or crushed into an ingot in an Arc Furnace,
optional Pulverizer doubling, no water, no acid, no hatches. F0 crank power cannot run F1
machines. Pylons already cover a factory; conduits are the named-network *alternative*, not the
way power exists. Every T1 machine is a single block; the first parametric multiblock in
`MACHINES.md` is T3.

**Decision.** Remaining work is scheduled as playable slices. A slice ships when a player can do
a new thing in-world, not when a layer is "complete".

1. **Slice A — First iron (0.1).** `ProcessRecipe` with item I/O now and fluid I/O as an empty
   slot (the type is one union, not a rewrite later). Generate only B0×R1 and B1×R1. Thermal
   Generator (furnace fuel → F1) and a Voltaic-gated Pylon MK1 recipe, so the player can walk
   away from the crank. Pulverizer and Arc Furnace as real consumers. One shared machine menu
   (energy, slots, named fault). Voltaic gates those recipes. Hoppers and chests are the
   logistics. Gaseous byproducts (CO from R1) **vent to atmosphere** until slice C — that is the
   named T1 sink (ADR-0036), not a missing tank. `VerifyRecipes` dumps the generated graph so
   generation is testable without JEI.
2. **Slice B — First factory (0.2).** Belts, splitter, manipulator. Terrestrial Extractor.
   Prospector's Scanner. Kiln can wait until gas is capturable.
3. **Slice C — Fluids.** Volume, temperature, pressure, Clay Conduit, Hand Pump, Basic Tank.
   ProcessRecipe fluid slots go live. Wet pulverizer, gas capture. This is the second line, not
   the first.
4. **Slice D — The factory builds the factory.** Press, Machine Casing, Assembler. Fabrication
   gate for T2+ (ADR-0017). 0.1 does not need an Assembler.
5. **Slice E — Energy spanning.** Flux Conduits, transformers, capacitor banks. Pylons already
   did coverage; this layer is named networks and distance, and must not obsolete pylons
   (ADR-0025).
6. **Slice F — Multiblock kernel.** A `MachineBlockEntity` plus a formed structure and hatches
   that are sided containers. Implemented when the first machine *needs* hatches or size (T2/T3),
   not as a framework waiting for content. Single-block T1 machines stay single blocks. Parametric
   reactors, distillation height and construction drones stay with that content.

The numbered plan rows 15–26 are rewritten to match these slices. Historical rows 1–14b do not
move. CI (old row 26) is already green and is marked done.

**Alternatives rejected.** Finishing every T1 machine before any recipe (shells with nothing to
run — the Research Terminal already taught that lesson); fluids before first iron (the reason
`ProcessRecipe` sat on the shelf, and B1×R1 does not need water); the conduit layer as the energy
route (the route is Thermal Generator + pylons; conduits are a later alternative); the parametric
multiblock framework before a machine that has a size parameter (an unused kernel that T1 machines
would then be forced to pretend to be); reaching the Assembler as part of 0.1 (the definition of
done is automated iron, and the Assembler is the last crafting-table recipe, not the first ingot).

**Consequences.** The next session implements slice A, not "T1 machines" as a set and not
"ProcessRecipe + fluids" as a pair. A successor that starts a multiblock framework or a conduit
network before first iron is reopening this record. `ProcessRecipe` may ship without a fluid stack
implementation; adding fluids later fills the slot, it does not fork the type. Vented gas is
deliberate T1 behaviour and becomes a capture decision in slice C, which is how R2 (roast for
SO₂) starts.


## ADR-0059 — First iron is a generated graph and a Voltaic gate

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice A: `ProcessRecipe` item-first, B0×R1 and B1×R1, Thermal
Generator, Pulverizer, Arc Furnace, one shared menu, Voltaic-gated T1 crafts including Pylon MK1.
Three implementation questions were still open.

Forge `ICondition` is evaluated when recipes *load*, not against a world. Voltaic is world-scoped
(`ResearchData`). Binding T1 crafts to a datapack condition would unlock them for every world
as soon as anyone researched them in any world in the same JVM, or never, depending on when the
condition ran.

Vanilla `RecipeType` is the usual home for machine recipes. Half of a later process is fluids
(ADR-0015), and slice C has not been written. Putting item-only recipes on `RecipeType` now
would force a rewrite when tanks arrive, which is the pairing ADR-0058 rejected.

**Decision.**

1. Processing recipes are a generated graph (`ProcessGraph` / `ProcessLookup`), rebuilt with
   `MaterialRegistry`, indexed by family and primary input tag (ADR-0005, ADR-0043). They are not
   a vanilla `RecipeType`. Fluid slots exist on the record and stay empty except for vented CO.
2. T1 crafting-table recipes are authored JSON of type `grindless:gated_shaped`. `matches`
   consults `ResearchData` on the server and `ClientResearch` on the client. Unlock is synced on
   login and when the Research Terminal flips the flag. Forge conditions are not used.
3. Carbon is `#grindless:carbon`, currently `#minecraft:coals`. Slag is a Grindless reagent item,
   not a material form (ADR-0033). CO from R1 is a vented fluid output — the named T1 sink
   (ADR-0036), not a missing tank.
4. The Thermal Generator, Pulverizer and Arc Furnace share one menu. Hoppers and neighbouring
   inventories are the logistics. The Arc Furnace holds T 1500 reducing so R1 evaluates optimally
   without a player setting a dial.
5. `VerifyRecipes` dumps the generated graph and checks the gated JSON against `T1Recipes`.

**Alternatives rejected.** Shipping B0×R1 as furnace JSON (it cannot name carbon, slag or CO);
gating with Forge conditions (wrong lifetime); a vanilla `RecipeType` for two families that
would be forked when fluids arrive; a per-machine screen (the point of the shared menu is that
the next machine does not invent a new UI); capturing CO in slice A (there is nowhere to put it).

**Consequences.** Slice B can add belts without touching the graph. Slice C fills the fluid
slots and turns the CO vent into a capture decision. A successor that adds JEI walks
`ProcessLookup.recipes()`, not the vanilla manager. A successor that adds a second blueprint
extends `Blueprint` and writes another gated JSON.


## ADR-0060 — First factory is lane data, a survey and an unpowered belt

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice B: belts, splitter, manipulator, Terrestrial Extractor,
Prospector's Scanner. Hoppers already move items. The Crude Extractor already pulls a vein
without a survey. Three questions were still open.

Belt mods usually spawn an `ItemEntity` per item. ADR-0008 already forbade that; the
implementation still had to pick a packing and a tick. Factorio's yellow belt is 15 items/s
with four items per tile per lane. Grindless's Conveyor is specified at 8 items/s.

The Terrestrial Extractor is "placed in a surveyed chunk". The Crude Extractor cannot require
that — T0 has no scanner. A survey that is only a chat message would let the T1 extractor
run anywhere, which deletes the scanner.

The splitter's interesting behaviour is filter plus priority. A full GUI is a second menu
family. Sneak-clicking a face with an item is enough to name a filter; a matching filter
already *is* a priority over an open face.

**Decision.**

1. Conveyor contents are two `Lane`s of `{id, count, position}` on the tile. Four slots per
   lane at 8 items/s is one tile per second. The tile ticks once and advances the lanes;
   items become stacks only at an endpoint (ADR-0008). Hoppers insert from above and pull
   from below. A `BeltEndpoint` is how a manipulator or a neighbouring belt talks to one.
2. The Crude Manipulator is unpowered and moves one item a second. The first inserter has
   to work the moment the first belt does.
3. The splitter has one input (the back) and three outputs (front, left, right). Sneak-click
   a face with an item to filter it; empty hand clears. A matching filter beats an open
   face; equal priority round-robins. Merger, tunnel, sorter and overflow wait.
4. The Prospector's Scanner marks a 3×3 of chunks in `SurveyData`. The Terrestrial Extractor
   draws F1, takes five seconds per unit at richness 1 (the rate `VeinGenerator` sizes
   reserve against), and is `OUT_OF_BAND` until its chunk is surveyed. The Crude Extractor
   does not read the set.
5. All five crafts are Voltaic-gated JSON. `VerifyBelt` drives the lanes and the router
   without a world.

**Alternatives rejected.** Entity-per-item belts (ADR-0008); a powered first belt (the
opening would still be a hopper line); requiring a survey on the Crude Extractor (T0 has
no scanner); a survey that does not persist (the T1 extractor would ignore it); a second
menu family for three filters (sneak-click is the whole interesting verb).

**Consequences.** Slice C can add fluids without touching the lanes. A faster belt is
another `BeltLogic` constant and a new block, not a new representation. A successor that
spawns `ItemEntity`s on a belt is reopening ADR-0008.

---

## ADR-0061 — The machine-state smoke runs as one function

*2026-10-03 · Accepted*

**Context.** ADR-0049 requires a booted server. The states scenario then `setblock`s every
facing and status and `say`s `STATE-OK` if the block is still that state. `tools/smoke-boot.sh`
sends each console line a second later so hoppers have time to tick.

A ticking machine does not keep a status the world did not earn. An empty belt publishes
`idle`. A Crude Extractor over a chunk vein with no power publishes `starved`. A Terrestrial
Extractor in an unsurveyed chunk publishes `out_of_band`. After a one-second gap the
`execute if` is testing the live loop, not whether the blockstate exists. GitHub run
37154213057 missed ten `STATE-OK` lines that way: the first idle of each extractor, every
belt `running`, every manipulator `running`. Later idles of the same block type passed only
because `MachineDisplay` would not republish a status it had already shown, so the
`setblock` stuck. That is a debounce accident, not coverage.

**Decision.** `GenerateAssets` writes `tools/smoke/states-pack` with one function that
places every state and `say`s in the same tick, before block entities run. CI enables that
datapack and sends `forceload` then `function grindless:states`. Hopper, belt-feed and
loop smokes stay as one console line per second; they are waiting for the world to work.

**Alternatives rejected.** Dropping the 1s pause for every smoke (hopper inserts would
lose the race the other way); expecting only the block id and ignoring status (a missing
variant would still boot); stopping machines from publishing during smoke (the live loop
is correct; the test was asking the wrong question).

**Consequences.** Adding a machine state is still one `BlockCatalogue` row. A successor
that inlines the 315 console commands to "match the other smokes" is reopening this
record.


## ADR-0062 — First fluids are millibuckets, gravity clay and a named CO sink

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice C: volume, temperature and pressure, Clay Conduit, Hand
Pump, Basic Tank, live `ProcessRecipe` fluid slots, a wet pulverizer, gas capture. ADR-0015
already forbade modelling fluids as items. Three questions were still open.

`PROCESSES.md` quotes volumes in buckets `B`. Forge and vanilla use millibuckets. Recipe
counts have to pick one unit and stick to it, or wet B1 (0.5 B water) and R1 (1 B CO) will
disagree with a tank that thinks 1 is a bucket.

The Chemical Washer is B2 and T2. A T1 wet mill that produced slurry would have nowhere to
put it (ADR-0036). Dry B1 already doubles raw into crushed. Water at T1 is a fluid-slot
proof, not a new yield.

R1 names 1 B of carbon monoxide. Slice A vented it as the named T1 sink. Slice C has a tank,
so the sink can become a capture *or* a burn. Blocking the Arc Furnace until a tank takes
the gas would stall first iron the moment the player built the second line.

**Decision.**

1. Volume is millibuckets internally. 1000 mB is one `B`. Recipe fluid counts are mB:
   wet B1 takes 500, R1 names 1000 of CO. Temperature and pressure live on the stack
   (`FluidState`). Mix weights both by volume. Empty is zero millibuckets, not a null.
2. The Clay Conduit is unpowered, ambient liquid, level or downhill. Gases need pressure
   a T1 pipe does not have. Horizontal placement is the T1 facing; `toY <= fromY` is the
   rule so a later vertical piece does not invent a new check.
3. The Hand Pump is unpowered. It reads a vanilla water *source* on the face it points at
   or the block below, and does not drain it. 20 mB/t is a bucket every 2.5 s.
4. The Basic Tank holds 16 B at ambient. Anything hotter than 40 °C is refused. Gases at
   0.1 MPa are accepted — that is how captured CO waits to be burnt.
5. Capture is a push. The Arc Furnace offers its 1 B of CO to neighbouring
   `FluidEndpoint`s; whatever they refuse vents. The furnace never blocks on gas. The
   named sink for captured CO is the Thermal Generator: 1 B burns 400 ticks of F1
   (a quarter of a coal). Venting to atmosphere remains a legal sink.
6. Wet B1 is the same crushed yield as dry B1 plus 0.5 B water. No slurry, no byproduct.
   When the mill already holds enough water, the wet recipe wins; otherwise dry runs.
7. Forge `FLUID_HANDLER` is attached by event (ADR-0045). `FluidStack` has no T/P:
   export drops both, import is ambient. Carbon monoxide has no registered vanilla
   `Fluid`; it stays on `FluidEndpoint` and is invisible to other mods' pipes.

**Alternatives rejected.** Recipe counts in buckets (0.5 is not an int); a T1 slurry
(no washer); blocking the furnace on uncaptured CO (first iron would stall); registering
CO as a vanilla fluid in this slice (buckets, stills, textures, and a gas that clay
cannot move); putting T/P on the Forge stack (Forge does not have those fields).

**Consequences.** Slice D can add the Press without touching millibuckets. A successor
that stores fluids as items is reopening ADR-0015. A successor that makes wet B1 a
different yield from dry is inventing the washer a tier early. Kiln / R2 wait until
the player has somewhere to put SO₂ — they now do, so that content is unblocked, not
in this slice.


## ADR-0063 — The factory builds the factory at T1

*2026-10-03 · Accepted*

**Context.** ADR-0017 forbids crafting-table recipes past the bootstrap. ADR-0058 scheduled
slice D as Press, Machine Casing, Assembler, and the T2 fabrication gate. Two documents
disagree about *when* those machines exist.

`MACHINES.md` lists Press and Assembler as T2. `PROCESSES.md` bootstrap chain makes them
the last crafting-table recipes, and says the Assembler is how T2+ is manufactured. Slice D
cannot ship both readings. `PROCESSES.md` also lists coil as a Wire Mill product; the Wire
Mill is T2, so the first Assembler would have no coil if that were the only route.

The first T2 machine has to be something the player already wants. Pylon MK2 is already
in the world as a block; MK1 is the Voltaic-gated craft. Making MK2 the Assembler's first
recipe is the gate, not a new machine shell.

**Decision.**

1. Press and Assembler are T1 crafting-table recipes, Voltaic-gated. `MACHINES.md`'s T2
   row is the industrial workhorse, not the first craft. The Assembler is the last
   crafting-table machine. T2+ has no JSON craft.
2. The Press is 4 s at F1. One ingot and a die. The die is a catalyst: it occupies a
   menu input slot, is not consumed, and hoppers cannot pull it. Plate, rod and gear
   dies cover the forms the casing needs. A coil die on copper makes the `copper_coil`
   reagent. The Wire Mill remains the dedicated T2 route.
3. Machine Casing is a crafting-table recipe: 4 iron plates and 2 iron rods, tags not
   item ids. The 8 s Assembler process in `PROCESSES.md` is the later scale recipe, not
   the bootstrap.
4. The Assembler craft is 1 casing + 1 copper coil + 2 iron gears. Its first generated
   recipe manufactures Pylon MK2 from 1 casing + 4 iron plates + 2 iron gears, 20 s at
   F1. There is no `flux_pylon_mk2.json`.
5. Dies, coil, Press, casing and Assembler are authored `gated_shaped` JSON. Press and
   Assembler recipes are generated into `ProcessRecipe` with a `catalysts` list, so the
   type does not fork.

**Alternatives rejected.** Shipping Press/Assembler as T2-only (the bootstrap could not
reach the Assembler, so ADR-0017 would never fire); a crafting-table recipe for MK2 "just
this once" (the gate would be a comment); consuming the die (hoppers would steal the
catalyst every cycle); waiting for the Wire Mill before the first coil (the Assembler
would be uncraftable); putting the die in a separate inventory that hoppers cannot see
(the shared menu already has input slots, and `canTakeItemThroughFace` already forbids
extracting them).

**Consequences.** Slice E can add Flux Conduits without inventing a fabrication story.
A successor that adds `flux_pylon_mk2.json` is reopening ADR-0017. A successor that
starts the Wire Mill or the Kiln in this slice is ignoring the playable-slice order.
Kiln / R2 remain unblocked by fluids and unstarted.


## ADR-0064 — Energy spanning is distance and storage, not coverage

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice E as Flux Conduits, transformers and capacitor banks.
Pylons already project supply areas and auto-link within range. Three documents disagree about
what a Flux Conduit *is*.

`README` System 1 and the tools table: a **hand item** that right-clicks two pylons into a
manual link, with no length limit and an upkeep proportional to distance. `docs/DESIGN.md`
says the same: "a hand item, not a block".

`README` *Flux Conduits — one network* and ADR-0025: a **T2 chassis block** whose carried
types are decided by cores (item, fluid, heat, signal, Flux). ADR-0026 names those networks.
Shipping that in slice E would give the player a universal logistics pipe the moment they can
span two pylons, which is exactly the trap ADR-0025 exists to prevent — and it would let a
Flux Core cover spots pylons cannot, which is how pylons die.

Slice E's own sentence is the tie-break: "this layer is named networks and distance, and must
not obsolete pylons". The playable gap after the Assembler is an outpost beyond MK1 link
range, a buffer that is not "place more pylons", and a tap that is not a second supply cube.

**Decision.**

1. The T1 Flux Conduit is the handheld linker. Right-click pylon A, right-click pylon B.
   Clicking a shaft counts as the base. Sneak-click the pair to drop the link; sneak-click
   air to clear a pending mark. The item is not consumed. Manual links have no length limit.
2. Manual edges live in `FluxNetworkData` beside membership (ADR-0007). Flood-fill and
   neighbour-merge follow them as well as automatic range. Breaking a pylon drops its
   manual edges. Unloaded chunks do not. `PylonIndex.linkedTo` stays auto-range only.
3. Upkeep is `ceil(distance / 8)` FU/t, minimum 1. It is demand on the shared pool, paid
   from stored energy each tick. An unpaid link browns the network out; it does not
   silently drop. Auto-range links cost nothing.
4. A Capacitor Bank adds capacity to the **covering** network. It projects no supply cube.
   Uncovered, it is inert. Capacity is 200 ticks of MK1 throughput (102 400 FU). Membership
   of banks is SavedData, same as pylons: place registers, break unregisters, unload does
   not.
5. A Flux Transformer is a covered tap, not a pylon. It exchanges FU with the covering
   network at F1 (32 FU/t) through a local buffer other mods can see as FE. Grindless
   machines still resolve power by `networkCovering` — standing next to a transformer does
   not power them. Per-face F0/F1 gating waits until the energy capability is re-invalidated
   per face; the T1 step is the rate cap, not a second voltage network.
6. Named conduit networks (ADR-0026) and the core-chassis block (ADR-0025) stay T2. Slice E's
   "named network" is the Flux Network identity the linker joins. Item, fluid, heat and
   signal cores are not in this slice.

**Alternatives rejected.** Shipping the T2 chassis in E (obsoletes belts, pipes and pylons
the moment spanning exists); giving the transformer or the bank a supply cube (a cheaper
pylon); letting a Flux Core "for the rare spot a pylon cannot cover" land here (that *is*
obsoleting pylons); colour channels (ADR-0026); dropping the link when upkeep cannot be
paid (a silent topology change); putting manual edges only on the block entity (unloaded
chunks would split the trunk).

**Consequences.** An outpost beyond 64 blocks is a deliberate trunk with a running cost, not
a line of pylons and not a cable. A successor that registers `flux_conduit` as a block with
cores is starting ADR-0025, not extending this slice. A successor that lets
`networkCovering` return a transformer is reopening this record. Kiln / R2 and Slice F are
still unstarted.


## ADR-0065 — T1 Kiln is roast and SO₂, not the acid line

*2026-10-04 · Accepted*

**Context.** ADR-0058 scheduled playable slices, not system layers. ADR-0062 unblocked Kiln / R2
the moment a tank could hold a gas. Slice E (ADR-0064) spanned energy; it did not start roasting.
Three documents still disagree about what the first Kiln ships.

`PROCESSES.md` R2 is roast then reduce: Kiln at `T 700 · atm O2` for 8 s, then Arc Furnace at
`T 1200 · atm reducing` for 10 s, factor **1.15**, byproducts **1 B SO₂** and slag. The sulfur
loop then burns that SO₂ to SO₃ with bottled oxygen and vanadia, and that is sulfuric acid.

The T1 Arc Furnace is locked at 1500 °C reducing so R1 runs at full speed without a dial
(ADR-0059). A 1200 °C point band would put 1500 °C in the tolerance zone and halve R2. T1 has
no atmosphere bottles; `atm O2` is the oxidising envelope, not a fluid slot. Oxide is not a
conventional Forge form. Yield 1.15 is the reason to rebuild the line later, not the reason to
roast now — the reason to roast now is SO₂.

Slice F is the multiblock kernel, and only when a machine needs hatches or size (ADR-0058).
The Kiln does not.

**Decision.**

1. The T1 Kiln is a single-block `ProcessMachine` on the shared menu (1 in, 1 out). Envelope
   `MachineEnvelopes.KILN`. Held conditions are 700 °C oxidising, so roast evaluates optimally
   with no dial. Voltaic-gated crafting-table JSON, like the Pulverizer.
2. Roast is generated per material that has a vein feed and an oxide form. 1 u raw (or ore)
   or 1 u crushed → 1 u oxide + 1 B SO₂, 8 s at F1, `T 700 · OXIDISING`. No oxygen fluid.
   Crushed roast is a second recipe, not a different yield. 1.15 stays deferred.
3. Oxide is a Grindless form: `grindless:oxides/<material>`, not `forge:`. Supply covers
   mineable catalogue materials; alloys have no oxide. Item path is `<material>_oxide`.
4. R2 reduce is an Arc Furnace recipe: 1 u oxide + 1 u carbon → 1 ingot + slag, 10 s at F1.
   It names 1500 °C reducing so the locked T1 furnace runs it at full speed. The PROCESSES
   1200 °C is the envelope floor, not a second held temperature. It does not vent CO —
   PROCESSES names SO₂ and slag only.
5. SO₂ is a gas at ambient, like CO. The Kiln pushes 1 B into neighbouring `FluidEndpoint`s
   and vents the rest. The named T1 sink is the Basic Tank. The Thermal Generator must not
   burn it. Clay Conduit still refuses gases.
6. No sulfuric acid, no SO₃, no vanadia, no bottled oxygen, no Chemical Washer, no Wire Mill,
   no T2 cores, no Slice F kernel. Those wait for a machine that needs them.

**Alternatives rejected.** Shipping the 1.15 yield (fractional stacks and a lie until a later
route exists to justify the extra); a T1 oxygen fluid slot (nothing produces bottled O₂ yet);
naming R2 reduce at 1200 °C (the locked furnace would run it at half speed); burning SO₂ in
the Thermal Generator (that sink belongs to CO, and it would skip the acid line); registering
oxide under `forge:` (no convention); starting the contact-process reactors in this slice
(ADR-0036 would then demand a sulfuric-acid sink that T1 does not have).

**Consequences.** The player roasts because they want a tank of SO₂, not because they want
more metal. A successor that adds `sulfuric_acid` without a named spend is reopening
ADR-0036. A successor that starts Slice F because "the Kiln should be a multiblock" is
ignoring ADR-0058 — this Kiln does not need hatches.


## ADR-0066 — The T1 Atlas is a live lookup, not the solver

*2026-10-04 · Accepted*

**Context.** ADR-0023 ships a native Process Atlas with a ratio solver, because a pack without a
recipe viewer would make parameterised recipes unplayable. README step 16 is `VerifyRecipes` plus
an atlas stub; JEI, REI and EMI wait until 1.0 polish. After Kiln / R2 the live graph is ore line,
roast, press and assembler. The solver — given a rate, emit machine counts — is the feature
ADR-0023 itself flags as most at risk of going too far. Slice F is not next (ADR-0058).

**Decision.**

1. The T1 Atlas is a handheld item. Right-click opens a scrollable list of the live
   `ProcessLookup` graph: family, inputs, catalysts, outputs, named conditions, duration and
   FU/t. Query lives in `AtlasLogic` with no Minecraft imports so `VerifyAtlas` can dump it.
2. It is a lookup, not a solver. No target rate, no machine counts, no FU-per-unit overlay, no
   reachability against research. Those remain ADR-0023.
3. JEI, REI and EMI still wait. Vanilla already shows T0 JSON crafts. The Atlas lists process
   recipes only; a pack that ships no JEI can still see the graph that machines actually run.
4. Voltaic-gated crafting-table JSON, like the Prospector's Scanner. Process recipes only run
   after Voltaic machines exist; T0 table crafts do not need a second viewer.
5. The screen reads `ProcessLookup` on the client after tag sync. No extra S2C. No graph widget
   yet — a list of `AtlasLogic` lines is the stub.

**Alternatives rejected.** Shipping the ratio solver in this step (it would prescribe the factory
before the player has asked); JEI-only (ADR-0023 already rejected that); an always-available T0
craft (the gap is process recipes, which appear with Voltaic); a block terminal (the scanner and
linker are already handheld; a desk would duplicate the Research Terminal).

**Consequences.** A successor that adds `solve(rate)` is finishing ADR-0023, not extending this
stub. A successor that starts JEI integration is 1.0 polish, not a T1 blocker. Slice F is still
only when a machine needs hatches or size.


## ADR-0067 — Modular armour and the Arc Reactor are one tier

*2026-10-04 · Accepted*

**Context.** The README already has a T2 Flux Exosuit with a generic **Portable Reactor** module,
and T4 Fusion as the first *named* compact-power fantasy. Flux F3 is already called **Arc** and
has no generator. The Arc Furnace is a T1 smelter. Construction drones — the thing that makes a
real multiblock playable — arrive at T3 (ADR-0031). The user asked for modular armour *at every
research tier*, with upgrades, and for one of those tiers to carry a **miniature Arc Reactor**
on the suit **and** a fully playable Arc Reactor generation multiblock, with the processing
lines that feed it.

A successor who ships the current T2 "portable reactor" as a free worn generator, or who turns
the Arc Furnace into a power plant, or who drops an unfed trophy core in the world, has missed
the request.

**Decision.**

1. **Armour is a chassis line, not one T2 unlock.** T1–T4 each ship a four-piece modular suit.
   The grid grows. Modules are upgrades: inserting and removing is free, same as machine
   upgrades. T1 is protection plus a cell — no onboard generation. T2 adds the Network Tap and
   mobility (you walk through pylons). T3 is the Arc chassis. T4 is the exotic / orbital suit.
   Vanilla armour stays valid until the T1 chassis exists.
2. **The Arc pair is T3 / F3, together.** The factory **Arc Reactor** and the suit's
   **miniature Arc Reactor** unlock on the same research tier. One without the other is a
   different feature. The worn core burns the same manufactured fuel the multiblock burns, so
   the line you built for the plant also charges the suit.
3. **The factory reactor is a generator you have to run.** Formed structure, hatches, coolant
   or stability as real logistics, and a **named processing line** whose product is the fuel
   (working name: Arc Cell). It produces **FU directly** at F3, not heat into steam — fission
   already owns that route. If the cell line stops, the reactor starves. It is not a trophy
   block (MACHINES.md: every endgame system has a real use; this is the same rule at T3).
4. **It is not the Arc Furnace, not fission, not fusion.** The Furnace stays a smelter. Fission
   stays T3 heat + neighbour-bonus steam. Fusion stays T4 D–T ignition. The Arc Reactor is the
   missing F3 plant: compact, fed, direct FU.
5. **Do not start this in T1.** Slice F exists when *this* machine (or another that actually
   needs hatches) is scheduled — not as an empty kernel (ADR-0058). Equipment waits for the
   0.4 tools slice. Exact Arc Cell chemistry, hatch layout and grid sizes stay open.

**Alternatives rejected.** Shipping a T2 worn reactor that needs no factory (the current
Portable Reactor row); using the Arc Furnace as a generator (name collision and it already
smelts); a trophy core with no feed line; putting the pair at T2 before drones (a processing-line
multiblock you place by hand is a chore); starting Slice F "for later"; merging this into T4
Fusion (then F3 still has no generator and the suit waits until the endgame).

**Consequences.** A successor that adds `portable_reactor` as a T2 module with no cell recipe is
reopening this record. A successor that starts hatches because "armour will need a reactor" is
ignoring ADR-0058 — start F when the Arc Reactor (or another sized machine) is the slice, not
before. The README Exosuit table is the player-facing version of this decision.


## ADR-0068 — Horizon Gates are commute infrastructure, not mining dimensions

*2026-10-04 · Accepted*

**Context.** The README already rejects mining dimensions as "the same grind, in a different
room." System 9 already has rockets, Mass Driver, telepresence and Colony Cores. ADR-0012
already says planets come from installed space mods. The user asked for futuristic end goals
in the Stargate tradition: gates, dimensions, exotic planets.

A successor who adds a void-miner dimension, a creative teleport, or a second planet pack
beside Ad Astra has missed both the request and the existing records.

**Decision.**

1. **The Horizon Gate is T6 commute infrastructure.** A ring multiblock you dial. Addresses
   come from the planet registry after a Deep Survey, not from a creative list. It is the
   moment the interplanetary commute dies, the same way the Crude Extractor killed mining and
   the Blueprint Tool killed repeating a layout.
2. **Two rings.** The far gate is cargo: you deliver it by rocket, Mass Driver or colony
   package once. Until the pair exists, you still fly. After it exists, you walk. That is the
   factory payoff, not a free portal.
3. **People cheap, bulk expensive.** Players, Proxy Frames and colony packages go through at a
   modest FU cost. Item cargo is allowed but costs more FU per kilogram than the Mass Driver, so
   belts of ore still belong on the driver (principle 10: pay in layout or pay in power).
4. **Not a mining dimension.** A Horizon Gate never opens a world whose only job is "more
   ore." Destinations are planets in the registry — Ad Astra's if present (ADR-0012), otherwise
   the fallback set. Vanilla Nether and End are not auto-registered; a pack author may add them
   by datapack. Pocket "mining dims" and RFTools-style void worlds are out.
5. **Exotic fallback worlds are process envelopes.** When no space mod is installed, the
   existing five stay, and two more exotic worlds join: **Thalassa** (ocean, pressure chemistry)
   and **Helios** (tidally locked heat/cold). Signature resources and hazards differ; the
   material registry does not. Erebus remains the strange-matter world. Do not invent unique
   untaggable ores that only exist there.
6. **Do not start this in T1.** Orbital (0.8) and interplanetary (0.9) stay later. Slice F is
   still only when a machine needs hatches. Exact dial cost, ring size and address format stay
   open.

**Alternatives rejected.** A mining or void dimension (already in the anti-pattern table);
shipping Grindless planets beside Ad Astra (ADR-0012); a one-sided creative teleport (no
factory); routing all cargo through the gate (obsoletes the Mass Driver); calling the block
Stargate (someone else's name); treating the Nether as a planet by default.

**Consequences.** A successor that registers `mining_dim` is reopening this record and the
anti-pattern table. A successor that skips the far-gate delivery is shipping a creative
teleporter. The README System 9 travel table is the player-facing version.


## ADR-0069 — The Multitool rotates and relocates; it still does not mine

*2026-10-04 · Accepted*

**Context.** ADR-0055 forbids the Multitool from mining. The README still promises it rotates
machines and picks them up with contents and settings intact. The registered item is a blank
`Item`: it does neither. That is the empty-the-machine tax the T0 handheld was meant to delete.
Slice F, the Sifter, Resonance, the Ballistic Turret, armour and Horizon Gates are not the next
playable hole.

**Decision.**

1. Right-click a Grindless block with the Multitool: rotate 90° clockwise if it has a horizontal
   facing. The machine GUI, the dynamo crank and the Research Terminal do not open while the
   Multitool is the used item.
2. Sneak-right-click: pick the block up as its BlockItem, with `BlockStateTag` (facing, status)
   and `BlockEntityTag` (buffers, lanes, fluids, filter). `Relocation` is active so
   `onRemove` does not spill the inventory onto the floor. Pylon clicks resolve to the base
   (ADR-0054); shafts are not a separate item.
3. A pickaxe still breaks machines and they still drop empty (ADR-0051, ADR-0055). The Multitool
   is not in `mineable/pickaxe` and has no destroy speed.
4. Splitter face-filtering stays sneak-click *without* the Multitool. With it, sneak is relocate.
5. This is not the Blueprint Tool and not a mining module.

**Alternatives rejected.** Mining with the Multitool (ADR-0055); opening the GUI anyway (then
rotate is unreachable on machines with menus); spilling contents on relocate (that *is* the tax);
a T2 Blueprint-only move (too late for the machine you just placed at T0).

**Consequences.** A successor that gives the Multitool pickaxe behaviour is reopening ADR-0055.
A successor that starts the Sifter or a turret because "T1 is done" is skipping a tool the player
already crafted.


## ADR-0070 — Remaining work is the autonomous build-out

*2026-10-04 · Accepted*

**Context.** Slices A–E, Kiln/R2, the Atlas stub and the Multitool wrench are in. Armour and
Horizon Gates are recorded, not started. The README implementation plan still has four coarse
pending rows (F, T2+ industry, tools, orbit, planets). A session that infers "next" from those
rows restarts Slice F, the Sifter without a graph, or a turret without Resonance — all of which
the records already forbid.

The owner asked for a closed list so a successor can finish the mod without asking.

**Decision.**

1. [`docs/BUILD-OUT.md`](BUILD-OUT.md) is the remaining schedule. The README stays the design
   source of truth (ADR-0013). The build-out is the order.
2. The next playable slice is **G — Belt junctions** (merger, tunnel, overflow), which
   ADR-0060 already parked. Not F. Not the Sifter. Not a turret. Not armour.
3. Slice F still starts when a machine needs hatches. That machine is the Arc Reactor at T3
   (ADR-0067), as build-out **AD/AE**.
4. A session does not stop to ask. Defaults come from the README, `MACHINES.md` and
   `PROCESSES.md`. An ADR is written only when those conflict or are silent, then the slice
   ships.
5. One slice per stacked draft. Do not merge unless the owner asks. Do not squash.

**Alternatives rejected.** Leaving "T2+ industry" as one row (a session dumps the chemical core
into an unused kernel); starting F "so later slices are easier" (ADR-0058); asking the owner
at every T1-named hole (the holes are already classified: Sifter needs a graph ADR, turret
needs Resonance, harness is 0.4).

**Consequences.** A successor that starts hatches, a Sifter shell, or JEI because the build-out
looks long is ignoring this record. Updating the **Next slice** line is part of shipping G
and every slice after it.


## ADR-0071 — T1 belt junctions are merger, tunnel and overflow

*2026-10-04 · Accepted*

**Context.** ADR-0060 shipped the first belt, splitter and manipulator, and parked merger,
tunnel, overflow and sorter. BUILD-OUT slice G is those three junctions. README already
gives the T1 tunnel a range of five and describes overflow as the belt that does not stall
when a chest fills. Three questions were still open.

**Decision.**

1. **Merger.** Three inlets (back, left, right), one outlet (front). Round-robin from the last
   accepted face. No filters; that is the sorter (slice H).
2. **Tunnel Belt.** An entrance/exit pair facing the same way. The gap is one to five empty
   blocks (README range 5). Adjacent tiles are a conveyor, not a tunnel. Items travel only in
   the facing direction. A craft yields two tiles.
3. **Overflow Gate.** Front is preferred. The clockwise side takes the item only when the
   front is backed up. Not a second splitter.
4. Same lane model and BeltEndpoint as the conveyor (ADR-0008). Voltaic-gated crafts.
   `VerifyBelt` dumps pick/range/route. Sorter still waits.

**Alternatives rejected.** Powered T1 tunnels (the first belt is mechanical); opposite-facing
two-way pairs in this slice (a second direction is another block, not this one); overflow as
a splitter with a hidden filter (then two blocks do one job).

**Consequences.** A successor that starts the sorter in this slice is skipping H. A successor
that lets two tunnel tiles pair when they touch is deleting the conveyor.


## ADR-0072 — The T1 sorter peels; it does not split

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice H is the sorter ADR-0060 parked. README calls it an inline
multi-output filter for a mixed ore line. The splitter already has three filtered outlets and
round-robins unfiltered faces. If the sorter copied that, two blocks would do one job. The
remaining question is what "inline" means when a matching lane is full.

**Decision.**

1. One inlet (the back). Front is always the passthrough: unmatched items continue. Left and
   right are optional filters, sneak-clicked like the splitter. An empty filter does not steal.
2. A matching side, if open, takes the item. If that side is backed up, the sorter holds.
   Matching items never dump onto the front. That is the difference from the splitter, whose
   backed-up filter yields to an open face.
3. Two sides that name the same item round-robin. No GUI. Logic Controller still waits.
4. Voltaic-gated craft with a hopper in the middle so the recipe is not the splitter. Same lane
   model and BeltEndpoint (ADR-0008). `VerifyBelt` dumps the route.

**Alternatives rejected.** A second splitter under a new name; a GUI for a list of filters
(sneak-click is enough for two faces); overflowing a full copper lane onto the iron belt (that
deletes the sorter).

**Consequences.** A successor that lets a backed-up filter spill to the front is shipping
overflow on the wrong block. A successor that round-robins unfiltered sides is shipping a
splitter.


## ADR-0073 — Industrial is the second blueprint on the same terminal

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice I is the T2 gate: research Industrial, then Pylon MK2 is a real
Assembler craft. The MK2 process already exists (`assemble/pylon_mk2`) and has no crafting-table
JSON (ADR-0063). The terminal only unlocks Voltaic, and the Assembler does not ask whether
Industrial is researched, so a T1 factory already manufactures the T2 pylon.

**Decision.**

1. **Industrial** is the second `Blueprint`. The same Research Terminal unlocks it after Voltaic.
   It still draws F0. The cycle is sixty seconds. The spent item is an **Advanced Data Core**,
   not a basic core. You cannot skip Voltaic.
2. The Advanced Data Core is a Voltaic-gated crafting-table item: one Data Core and four iron
   plates. Processed plates are the "advanced cores from processed ones" line in the README.
3. `assemble/pylon_mk2` names blueprint `industrial`. The Assembler refuses the recipe until
   that blueprint is unlocked. Atlas still lists it. Wire Mill, washer and Slice F still wait.

**Alternatives rejected.** A second research block (the terminal is the production target);
re-rating the terminal to F1 for this slice (the first T2 wait should not demand a new power
tier); a crafting-table MK2 (ADR-0017); unlocking Industrial with a basic core (then the
advanced item is flavour).

**Consequences.** A successor that lets the Assembler build MK2 before Industrial is deleting
the T2 gate. A successor that starts the Wire Mill in this slice is skipping J.


## ADR-0074 — The Wire Mill is T2 and does not wait for acid

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice J is wire, coil and motor. `MACHINES.md` puts the Wire Mill at T2
for wire and coil. `PROCESSES.md` names Motor as `1 casing + 2 u coil + 1 u rod` in 10 s with
no fluid, and the generic T2 machine as `1 casing + 2 Motor + 1 Circuit Board + 4 u plate`.
Circuit Board needs etching acid (slice K). T1 already presses a copper coil with a die so
the Assembler can exist (ADR-0063). Fine wire is MK III+. Pump waits on resin.

**Decision.**

1. **Wire Mill** is a T2 single-block process machine. The Assembler manufactures it once
   Industrial is researched: `1 casing + 2 copper coil + 4 iron plates`, 20 s, F1. No circuit
   board. No crafting-table JSON (ADR-0017). Ambient envelope. Menu is one in, one out.
2. **Wire** is a supplied form for every catalogue material (`grindless:wires/<m>`, not
   conventional). `1 ingot → 2 wire`, 8 s, F1, no named conditions. Fine wire waits.
3. **T2 coil** is mill-only: `2 copper wire → 1 copper_coil`, 8 s, F1, no die. The Press plus
   coil die remains the T1 bootstrap (ADR-0063). `MaterialForm.COIL` is still not supplied.
4. **Motor** is a reagent item `grindless:motor`. Assembler: `1 casing + 2 copper coil +
   1 iron rod`, 10 s, F1, Industrial. No JSON. Pump and the generic T2 machine recipe wait.

**Alternatives rejected.** Using the generic T2 recipe this slice (that is acid); dropping
the Press coil (the first Assembler would have nothing to wind); supplying fine wire
(MK III); a crafting-table mill (ADR-0017); gating every mill recipe on Industrial (owning
the mill is the gate).

**Consequences.** A successor that starts sulfuric acid in this slice is skipping K. A
successor that crafts the mill at a table is deleting the T2 manufacturing gate.


