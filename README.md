# Grindless

**Grindless** is a full modpack-scale progression for Minecraft. The early game already
built — a hand crank, the first factory, the contact process — is the foundation of an
aligned **T0/F0-T15/F15** ladder carried by physical Control Matrices.

Each tier is a larger braided factory: materials, chemistry, computation/control and
energy interact throughout the tier and converge in the next frontier matrix. Later
tiers add more routes, conditions, logistics and spatial scale, while better automation
removes the repetition that scale would otherwise create. The large milestones are the
Kardashev scales. The goal is the black hole at the centre of the Milky Way. See
[Modpack-scale progression](#modpack-scale-progression),
[ADR-0088](docs/DECISIONS.md#adr-0088--grindless-is-a-modpack-scale-progression) and
[ADR-0107](docs/DECISIONS.md#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices).

The early hours of a typical pack are still the wrong way to start that ladder. Punch
wood, make a pick, dig a staircase, strip-mine for iron, strip-mine again for the next
tier: the foundation replaces that opening with automation, so the long game can begin.
Ideas from Factorio, GregTech, Ad Astra and BetterQuesting may inform the design. Their
code and assets do not enter this repository (ADR-0088).

The arc runs from a hand crank and two iron, through belts, reactors and a particle
accelerator, out past orbit, and on to that black hole. Arriving is the victory.

> **Status: pre-alpha.** The foundation through Industrial is playable and the consolidated
> branch is locally validated on Forge 1.20.1. The Ground Array, Luna, Drift, rocket,
> station and galactic-centre chamber are also implemented and smoke-tested, but ADR-0107
> reclassifies them as prototype/test infrastructure rather than an Industrial survival
> shortcut. BO removed their recipes and premature quest path while preserving the tested
> systems; the survival guide now ends at electronic silicon. BP's first recoverable Relay Matrix
> batch is in and uses a physical calibrated core; guidance and the T0 audit remain. The real campaign reaches orbit at T10, the Drift at T13 and
> Sagittarius at T15.
>
> **Forge 1.20.1 is the only build target** (ADR-0039). The same jar also loads on NeoForge 1.20.1
> unchanged (ADR-0002). Fabric was dropped so the work stays focused on one loader.
>
> The machine layer is designed in [`docs/MACHINES.md`](docs/MACHINES.md) and the content layer —
> items, fluids, the recipe graph and the routes — in [`docs/PROCESSES.md`](docs/PROCESSES.md).
>
> This README is deliberately the *single source of truth* for the whole project — design,
> architecture, verified dependency versions, roadmap and open work. It is split into
> [`docs/`](docs/) progressively as implementation advances.

---

## Contents

- [Environment setup](#environment-setup)
- [The problem](#the-problem)
- [Design principles](#design-principles)
- [The core promise](#the-core-promise)
- [Modpack-scale progression](#modpack-scale-progression)
**The eight systems**

- [System 1 — The Flux Network](#system-1--the-flux-network) — power without cables
- [System 2 — Resource Genesis](#system-2--resource-genesis) — chunks that own their materials
- [System 3 — Matter Replication](#system-3--matter-replication) — everything that isn't an ore
- [System 4 — Logistics and belts](#system-4--logistics-and-belts) — belts, drones, conduits, logic
- [System 5 — Fluids, pressure and phase](#system-5--fluids-pressure-and-phase) — pipes, tanks, loops
- [System 6 — Tools and equipment](#system-6--tools-and-equipment) — blueprints, drills, exosuit
- [System 7 — Resonance, defence and weapons](#system-7--resonance-defence-and-weapons)
- [System 8 — The futuristic tiers](#system-8--the-futuristic-tiers) — fission, fusion, accelerator
- [System 9 — Orbit and the planets](#system-9--orbit-and-the-planets) — satellites, remote colonies

**Mechanics**

- [Energy: Flux Units](#energy-flux-units)
- [Processing chain](#processing-chain)
- [Progression: Control Matrices and research](#progression-control-matrices-and-research)
- [Fabrication: the factory builds the factory](#fabrication-the-factory-builds-the-factory)
- [Containers: buffers, filters and voiding](#containers-buffers-filters-and-voiding)
- [Block and item catalogue](#block-and-item-catalogue)
- [Compatibility strategy](#compatibility-strategy)
- [Why this is not a cheat mod](#why-this-is-not-a-cheat-mod)

**Development**

- [Architecture](#architecture)
- [Supported platforms](#supported-platforms)
- [Verified toolchain](#verified-toolchain)
- [Building](#building)
- [Roadmap](#roadmap)
- [Implementation plan](#implementation-plan)
- [Autonomous build-out](docs/BUILD-OUT.md)
- [Assets](#assets)
- [Contributing](#contributing)

---

## Environment setup

Requires **JDK 17**. Everything else is fetched by Gradle on the first build.

On Windows, run the bootstrap once from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\SETUP.ps1
```

It creates the full source tree, locates a JDK 17 and Gradle 8.8 without requiring a system-wide
install, generates the committed Gradle wrapper, removes leftover scratch files, and warns you if
this checkout is deep enough to hit the Windows 260-character `MAX_PATH` limit. A multiloader mod
nests roughly forty source directories before a single `.java` file can exist, so that warning is
worth reading carefully — see [Building](#building).

### Switches

| Switch | Effect |
| --- | --- |
| `-Commit` | Stages, commits and pushes this worktree. |
| `-MakePrivate` | Switches the GitHub repository to private via the `gh` CLI. |
| `-SkipWrapper` | Skips Gradle wrapper generation. |

```powershell
powershell -ExecutionPolicy Bypass -File .\SETUP.ps1 -Commit -MakePrivate
```

---

## The problem

Every large modpack asks for the same opening: punch wood, stone tools, iron tools, dig down,
strip-mine, find diamonds, strip-mine more. It takes one to three hours, it is identical in every
pack, and it is *not what the pack is about*. The interesting content — the machines, the
logistics, the automation — sits behind it.

Worse, that grind never really ends. Every new tech tier wants another few stacks of a new ore, so
you go back to the same tunnels with a better pick. The mid-game becomes a loop of interrupting
whatever you were building to go mine more of something.

Existing solutions each break something:

| Approach | What goes wrong |
| --- | --- |
| Ore doubling / tripling | Still requires the mining. Multiplies output, not the fun. |
| Mining dimensions | The same grind, in a different room. |
| Quarries | Solves it, but arrives 10+ hours in — after the grind it would have fixed. |
| Void miners / cheat generators | Solves it *too* hard. Infinite free resources, zero decisions, pack over. |
| Creative-mode style item duplication | Trivialises every recipe in the pack at once. |

The foundation still aims at the target those approaches miss: **make resource acquisition an
automation problem instead of a time-tax, starting ten minutes in, without making resources
free.** That is how the ladder starts. The mod after that start is the
[modpack-scale progression](#modpack-scale-progression).

---

## Design principles

These are the rules every feature is measured against.

1. **Remove tedium, not decisions.** If a mechanic makes the player think, it stays. If it only
   makes them wait or repeat, it goes.
2. **Automation is the reward, not the grind.** The player should be laying out a base by minute
   fifteen, not mining until hour three.
3. **Resources cost something, always.** Energy, space, infrastructure, research. Nothing is free,
   ever — that is the line between this and a cheat mod.
4. **Carry the ladder.** Grindless is the pack-scale progression: its own later tiers,
   materials and processing lines. Early yields stay in a familiar range so the foundation
   is legible beside other mods.
5. **Work with mods it has never heard of.** Everything is driven by tags discovered at runtime.
   Zero hardcoded material lists, zero per-mod compat patches.
6. **Fail legibly.** When something is wrong the player must be able to see *what* and *why* —
   hence proportional brownouts instead of one randomly starved machine.
7. **Never hard-block the player.** Under-powered machines slow down; they do not stop.
8. **Original assets only.** Every texture and model is made for this project.
9. **Everything is a chassis plus modules.** Machines take upgrades, conduits take cores, drones
   take pods, pylons take bay modules. One pattern learned once, applied everywhere, and capability
   always arrives by slotting something in rather than by crafting a replacement block.
10. **Pay in layout, or pay in power.** Every convenience has a cheaper manual counterpart that
    stays correct forever: belts against drones, pipes against the phase network, both against
    conduits. Convenience is sold, never imposed.

---

## The core promise

Grindless works with **every material in your modpack**, including ones it has never heard of.

It does not ship a hardcoded list of ores. At runtime it reads the tags present in the loaded
pack — `forge:ores/*`, `forge:ingots/*`, `forge:raw_materials/*`, `forge:dusts/*`, `forge:gems/*`
and the Fabric `c:` conventions — and builds its material registry and its entire processing
chain from what it finds. Install Grindless alongside Mekanism, Thermal, Create, Ad Astra and
three obscure ore mods and it will extract, pulverize and smelt all of them on the first launch,
with no config, no compat addon and no patch release.

Materials degrade gracefully. If a material has an ore but no dust form, the pulverizing step is
simply skipped for it. If another mod already provides an item for a material, Grindless uses
that item instead of registering a duplicate.

---

## Modpack-scale progression

Recorded in [ADR-0088](docs/DECISIONS.md#adr-0088--grindless-is-a-modpack-scale-progression).
The systems below stay the foundation.

**Tiers.** Technology, controller and power advance together from T0/F0 through T15/F15
(ADR-0107). Every T1+ machine contains a physical Control Matrix of its rating. There is no
separate world permission that says a visible recipe is allowed.

**Milestones.** Kardashev scales are the large marks. Type I is a planet's energy, Type II a
star's, Type III a galaxy's. Megastructures sit along the way between those marks. The names
are milestones. A Flux Unit is not a physical watt. Type I belongs to the Planetary tier,
Type II to Stellar and Type III to Galactic.

**Planets.** Planetary gameplay has unique extractable resources. Luna, its regolith and
helium-3 already exist as validated prototypes (ADR-0095), but survival reaches them through
the T10 local-flight programme, not an Industrial teleport recipe. T11 turns worlds into an
interacting production network through life support, telepresence, abstract colonies and
Horizon Gates. Space and interstellar gameplay stay original to Grindless.

**Victory.** The goal is to reach the black hole at the centre of the Milky Way in T15.
Arriving is the victory, and the way there is the station, not a link (ADR-0097, ADR-0099).
Exploiting the accretion environment and rotational energy is useful postgame T15 content,
not an extra tier and not a prerequisite for the victory.

**Energy.** Nominal tier rates remain predictable: `F(n) = 8 × 4ⁿ FU/t`. F15 is
8,589,934,592 FU/t. Postgame black-hole infrastructure scales within T15 from that nominal
rate to `Long.MAX_VALUE` FU/t rather than adding F16-F30. Sums, multi-tick buffers and precise
work accounting require a wider representation before that content ships; the FE bridge
continues to saturate at its `int` boundary (ADR-0037, ADR-0107).

**Quest book.** A native book in the BetterQuesting style: lines, tasks, dependencies and
rewards. It never gates a machine (ADR-0100). Its current prototype route is shortened to
implemented survival content, then grows with the T0-T15 campaign. BetterQuesting's code
stays out.

**Quality of life.** Features known from other mods are in scope, built as original work
unless a later slice copies code under the terms in ADR-0089.

**Provenance.** Ideas from GregTech, Ad Astra, BetterQuesting and similar mods may inform
the design. Copying their code waits on the upstream license. Where that license was not
confirmed, the default is no reuse (ADR-0088). The copyright holder accepts adding an
upstream license so that compatible code can enter later (ADR-0089).

---

## System 1 — The Flux Network

*Factorio's electric poles, in three dimensions.*

Cable-based power is the single biggest source of early-game busywork in tech packs: running
wire, hiding wire, re-running wire after you move a machine, and debugging the one cable you
forgot to connect. Grindless deletes the category.

### Supply areas

A **Flux Pylon** is three blocks tall and projects a cubic **supply area** centred on its base.
Every Grindless machine inside that cube is powered. There are no wires between pylons and
machines, no per-face connections, and no cable loss to account for. One MK1 covers a small
factory floor; an MK3 covers a chunk-scale base.

| Pylon | Supply area | Throughput | Link range | Tier |
| --- | --- | --- | --- | --- |
| **MK1** | 48 × 48 × 48 | 512 FU/t | 64 blocks | LV–MV |
| **MK2** | 80 × 80 × 80 | 4,096 FU/t | 112 blocks | MV–HV |
| **MK3** | 128 × 128 × 128 | 32,768 FU/t | 192 blocks | HV–IV |

Range and throughput scale together, so upgrading a pylon is always unambiguously good — the
decision the player makes is *where* to put pylons and *how many*, which is the interesting
layout problem, not whether a bigger one is better.

### Linking

Two pylons within each other's link range **link automatically** and merge into one **Flux
Network** sharing a single pooled energy buffer. Generation anywhere on the network powers
consumption anywhere else on it.

For outposts beyond link range, the **Flux Conduit** hand item creates a manual link: right-click
pylon A, right-click pylon B, done. This is the mod's one concession to manual wiring, and it is
deliberate — it is the interesting kind (planning a trunk line between bases) rather than the
tedious kind (connecting two adjacent blocks).

Manual links have no length limit but cost an upkeep proportional to distance, so sprawling
networks are viable but not free.

### Implementation notes

Network state lives in server-side `SavedData`, not in block entities, with a spatial index over
pylon positions. Machines resolve their supplying network by querying that index, so:

- there is **no per-tick graph walk** — the classic performance killer in cable mods;
- coverage lookup is `O(log n)` in the number of pylons, not `O(n)` in the number of blocks;
- unloaded chunks do not break the network topology.

Network membership is recomputed only on pylon placement, breakage or manual link change.
Manual edges and capacitor extra live in the same `SavedData` as membership, so an unloaded
chunk does not drop a trunk or forget a bank (ADR-0064).

---

## System 2 — Resource Genesis

*Factorio's ore patches, mapped onto Minecraft chunks.*

### Deterministic chunk veins

Every chunk in the world **deterministically owns a Resource Vein**, derived by hashing the world
seed together with the chunk coordinates. A vein has three properties:

| Property | Meaning |
| --- | --- |
| **Material** | Which material this chunk produces. Drawn from the runtime tag scan, so it covers the whole pack. |
| **Richness** | A multiplier on extraction rate — how fast this chunk gives it up. |
| **Reserve** | How much is left before the vein decays toward its floor. |

Because it is derived rather than stored, no world data is generated up front, the same seed
always produces the same map, and the system costs nothing in worlds where the player never
builds an extractor.

Each primary material also has a deterministic **trace secondary**: the next eligible mineable
material in the runtime registry's stable name order. It is a property of the material's generated
process route rather than stack NBT, so raw ore from different chunks remains freely stackable.
The Chemical Washer exposes that trace material in B2 (ADR-0076).

Rarer materials are weighted to appear in fewer chunks and, usually, at lower richness — so a
diamond chunk is a find, and a copper chunk is not. Material weighting is derived from the
material's position in the pack's own progression where that can be inferred, and is
config-overridable.

### Finding and extracting

The **Prospector's Scanner** is a handheld that surveys the chunk you are standing in and nearby
chunks, reporting material and richness. Exploration therefore has a concrete purpose again: you
are looking for *specific* chunks, and finding a rich vein of the thing you need is a genuine
event.

The **Terrestrial Extractor** is placed in a surveyed chunk, draws Flux Units from the network,
and produces that chunk's material over time. Output rate is a function of richness, remaining
reserve, the extractor's tier and the voltage it is actually receiving.

### Depletion, and why it matters

Veins deplete — but toward a **nonzero floor**, exactly like Factorio's infinite ore patches. A
heavily worked vein keeps producing forever, just slowly.

This single decision defines the mod's whole rhythm:

- A vein is never *dead*, so no player ever loses an investment outright.
- But a worked vein is slower, so the efficient move is to **expand outward** — survey a new
  chunk, run a conduit, build another outpost.
- The pressure is horizontal, not vertical. You are exploring and laying out infrastructure,
  which is fun, instead of digging another 3×3 tunnel, which is not.

**Veins are deliberately long-lived** (ADR-0047). A chunk holds roughly **forty hours** of
continuous T1 extraction, the decay curve is gentle rather than linear — a quarter of the reserve
gone is still 96 % of the original rate, and half is 83 % — and the floor is **30 %**, not a token
amount.

That is a direct response to the failure mode this design could easily have had. If an outpost
needed relocating every session, expansion would stop being exploration and become a chore:
abandon, relocate, rebuild the same layout somewhere else, repeat. Rebuilding a solved layout is
grind by this project's own definition, so a vein outlives the interest a player has in watching
it.

**Richness scales reserve as well as rate**, so a vein's *lifetime* is the same whether it is rich
or poor. A rich chunk is a find rather than a countdown, and the player never has to weigh "rich
but short" against "poor but long" — a false choice nobody enjoys making.

### Deepening instead of moving

An extractor is a machine, so it takes chassis marks and upgrades like any other. The one that
matters here is **Deep Bore**: it reaches further into the same chunk, raising the rate a worked
vein settles at — 30 % to 45 % with one, 60 % with two, capped at 75 % however many are fitted.

So a depleting outpost has two answers rather than one: deepen it, or found another. Which is
right depends on whether you are short of power or short of territory, and that changes over a
playthrough. The cap is what keeps the choice alive — a chunk can never become infinite, so
expansion is always eventually the answer.

Deep Bore buys nothing at a fresh vein, which is what makes it a decision instead of a
must-have: fitting it on day one wastes a slot and pays upkeep for nothing.

### Late game

The **Deep Core Drill** stops caring about local geology entirely and pulls from a weighted
planetary pool — any material in the pack, at a rate set by tier and power. It is the T5 answer
to "I need a bit of everything and I do not want to think about it", and its cost reflects that.

---

## System 3 — Matter Replication

*For everything that isn't an ore.*

Ores are the easy half of the grind. The other half is items: rubber, silicon, dyes, mob drops,
compressed blocks, whatever the pack decided you need forty of. Those cannot come out of the
ground, so they need a separate answer.

| Block | Role |
| --- | --- |
| **Pattern Scanner** | Consumes an item once and permanently stores its **pattern**. |
| **Deconstructor** | Dissolves any item into generic **Matter**, the universal intermediate. |
| **Replicator** | Spends Flux Units + Matter to rebuild a stored pattern. |

Matter is deliberately fungible: anything can become Matter, and Matter can become anything you
have scanned. That makes it a genuine sink for the junk a pack generates — the eleventh stack of
cobblestone, the mob drops nobody uses — which is a second, quieter grind removed.

### Closing the pricing exploit

Copy-any-item mods usually die on flat pricing. If replication costs the same per item regardless
of what the item is, the correct play is immediately to mass-produce the single most valuable
item in the pack, and the pack is over.

Grindless derives replication cost by **walking the item's own recipe graph** — recursively
resolving its ingredients, and theirs, to estimate real crafting complexity — and prices
accordingly. Replicating a nether star costs like a nether star. The cost is computed from the
recipes actually present in the loaded pack, so it stays correct when the pack rebalances
something.

On top of that, a **blacklist** driven by datapack and config covers the genuinely
un-replicable: creative-mode items, quest rewards, anything a pack author wants kept unique.
Datapack-driven means pack authors can adjust it without touching the mod.

---

## System 4 — Logistics and belts

*The heart of Factorio, and the reason the game is about layout instead of inventory management.*

Generating materials is only half the problem. If the player still has to haul everything by
hand, the grind has merely changed shape. Grindless ships a full belt-based logistics layer so
that moving things is a design puzzle, not a chore.

### Flux Belts

Belts carry items along their surface, visibly, and feed machines directly.

| Belt | Throughput | Power | Tunnel range | Tier |
| --- | --- | --- | --- | --- |
| **Conveyor Belt** | 8 items/s | none — mechanical | 5 blocks | T1 |
| **Flux Belt** | 16 items/s | LV | 9 blocks | T2 |
| **Mag-Lev Belt** | 32 items/s | MV | 15 blocks | T3 |
| **Phase Belt** | 64 items/s, items are inert to entities | F9 | 24 blocks | T9 |

The first belt tier is **deliberately unpowered**. Belts must be available at the exact moment
the player builds their first extractor, before they have a real power network, or the early game
still ends up being manual hauling. Mechanical belts are craftable at T1 out of basic materials.

**Belts are not entities.** Items on a belt are stored as lane data inside the belt network and
rendered client-side, never as dropped `ItemEntity` instances. This is the difference between a
belt system you can build a thousand blocks of and one that destroys the server's tick time. A
belt segment ticks as one unit rather than as N items.

### Belt components

| Component | Behaviour |
| --- | --- |
| **Splitter** | Splits a lane evenly across up to three outputs, with per-output **filters** and **priority** (prefer this output until it backs up). |
| **Merger** | Combines lanes with round-robin or priority input. |
| **Tunnel Belt** | Passes under terrain and other belts, entrance/exit pair, range by tier. |
| **Manipulator** | The inserter. Moves items between belts and inventories. Tiers: **Crude**, **Fast**, **Stack** (moves up to 12 at once), **Filter** (whitelist/blacklist by item or tag). |
| **Sorter** | An inline multi-output filter, for splitting a mixed ore line into per-material lanes. |
| **Overflow Gate** | Passes items only when the downstream lane is backed up — the standard "send the excess to storage" pattern, as one block. |
| **Belt Reader** | Emits a redstone/logic signal describing lane contents — *what* is on the belt right now. |
| **Flow Meter** | Clamps onto a belt, a pipe or a flux cable and reports the **average rate**: items/min, B/min, FU/t. See below. |

The splitter's filter-plus-priority behaviour is copied faithfully from Factorio on purpose. It is
the single most expressive logistics primitive in that game, and almost every interesting belt
layout is built out of it.

### The Flow Meter

The Belt Reader answers *what is on this belt*. The Flow Meter answers *how much is actually
getting through*, which is the question a player asks when a line is underperforming and nothing
looks broken.

It is **one module, not three blocks**. The same item attaches to a belt, a fluid pipe or a flux
cable, and reads whichever carrier it is on:

| Carrier | Reads | Unit |
| --- | --- | --- |
| Belt | items past this point | items/min |
| Pipe | fluid past this point | **B/min** (buckets, the unit the rest of the fluid layer uses) |
| Flux cable / conduit | energy past this point | FU/t |

Three rules make it trustworthy:

- **It is a measurement, not a machine.** It adds no latency, no buffer and no backpressure.
  Removing a Flow Meter never changes what the line does. A diagnostic that perturbs the thing it
  measures is worse than no diagnostic.
- **It reports an average over a window, not an instant.** An instantaneous count on a belt that
  moves 8 items/s is noise, and a machine that runs a 600 s cycle reads zero almost always. The
  readout is a rolling average, displayed per minute, so a number can be compared against the
  ratios in [`PROCESSES.md`](docs/PROCESSES.md#the-recipe-graph) without arithmetic.
- **It is also a logic signal.** The value feeds the Logic Controller, so *run only while the
  output line is below 300 items/min* is buildable, not just readable.

This is the handheld [Process Atlas](docs/MACHINES.md#the-route-viewer) question — *where is the
line actually limited?* — answered in-world at a single point.

### Diagonal belts and pipes

Belts and pipes connect **diagonally in the horizontal plane**, not only along the axes. A
diagonal segment is still one block in one block space; what changes is which neighbours it will
join to.

A diagonal step covers √2 blocks of ground, so an item or a fluid parcel spends √2 times as long
crossing it. Ground speed is therefore constant and a diagonal run carries about **71 %** of the
line's rated items per second. That is the whole balance: diagonals are a routing and layout
freedom, never a speed upgrade, and no player-facing throughput number in the tables above
changes.

Junctions stay orthogonal. Splitters, mergers, sorters, overflow gates, tunnel endpoints and
manipulators attach on an axis; a diagonal segment is plain transport. Keeping the junction set
axis-aligned is what stops the lane model ([ADR-0008](docs/DECISIONS.md#adr-0008--belt-contents-are-lane-data-not-entities))
from needing a second geometry.

### Drone logistics — and the pylon's second job

At T3, logistics goes airborne, and here Grindless unifies two Factorio concepts that Minecraft
mods usually keep separate.

**A Flux Pylon's supply area is also its drone operation area.** The pylon is simultaneously
Factorio's electric pole *and* its roboport. This means the network the player already built for
power is automatically the network their drones fly in — one piece of infrastructure, two
payoffs, and no second grid to plan.

| Block | Role |
| --- | --- |
| **Drone Bay** | A pylon upgrade module. Houses and charges logistics drones. |
| **Provider Crate** | Offers its contents to the network. |
| **Requester Crate** | Requests items; drones deliver them. |
| **Buffer Crate** | Both, with a target stock level — the network keeps it topped up. |
| **Storage Crate** | Catch-all destination for deconstruction and overflow. |
| **Logistics Drone** | Carries items between crates within pylon coverage. |
| **Construction Drone** | Builds a blueprint from network materials, and performs deconstruction orders. This is what makes large multiblocks possible at all — see below. |

Drones consume FU from the network while flying, so logistics is a real, visible load on the
power grid rather than free teleportation.

#### Construction Drones lift the ceiling on multiblock size

Construction Drones look like a convenience and are not. The real limit on multiblock scale in
every mod is **placement tedium**: nobody ships a two-thousand-block structure because nobody will
place two thousand blocks by hand, so multiblocks stay small and their design space stays shallow.

Placing two thousand blocks by hand is precisely *repeating an action whose outcome you already
know* — this mod's own definition of grind. So the fix is not to keep multiblocks small. It is to
**automate the placing and keep the designing**.

With drones doing the construction, a reactor core can be a genuine engineering problem at a scale
worth engineering: hundreds of internal positions, fuel clustering against coolant routing, edge
effects from reflectors, all of it designed by the player and placed by the swarm. A blueprint is
**simulated before it is built**, reporting output, heat, coolant demand and bill of materials, so
designing is iterative instead of ruinous. The details are in
[`docs/MACHINES.md`](docs/MACHINES.md#multiblocks-shape-is-a-parameter).

At T10 the **Assembly Field** replaces the swarm and materialises a whole blueprint at once, for
structures measured in chunks.

### Operator Drones — work that is a sequence, not a flow

Logistics drones are **declarative**: you state that a crate should hold 64 iron and the network
works out the deliveries. That is exactly right for steady supply, and completely unable to express
*"wait until the autoclave finishes, take the batch to the press, run it, bring the byproduct
back, and swap the catalyst if it is spent."*

That kind of work is a **sequence** — irregular, conditional, multi-step — and belts, conduits and
logistics drones are all continuous-flow systems that cannot describe it. **Operator Drones** are
the imperative half of automation.

| Block / item | Role |
| --- | --- |
| **Operator Bay** | Houses, charges and programs Operator Drones. A pylon module, like the Drone Bay. |
| **Operator Drone** | Executes a routine, step by step, visibly. |
| **Instruction Card** | One step. Placed in an ordered strip to build a routine. |
| **Routine Card** | A whole finished routine, stamped onto one copyable item. |
| **Locator** | A handheld that binds a position, a face and a slot to a card. |

#### Routines are built, not typed

The README already refuses to make the player learn a scripting language for the circuit network,
and the same rule applies here. A routine is an **ordered strip of physical cards**, closer to a
player-piano roll than to code: you drag cards into a row and the drone performs them in order.

| Card | Does |
| --- | --- |
| **Go To** | Fly to a bound location. |
| **Take** / **Give** | Move items, filtered by item or tag, from or into a target's chosen face. |
| **Draw** / **Pour** | Move fluid, using the drone's own internal tank. |
| **Absorb** / **Emit** | Move heat, using a thermal pod — the same trick as the conduit heat core. |
| **Operate** | Start a machine, or swap a spent catalyst. |
| **Read** | Read a level, temperature or progress value for a later comparison. |
| **Wait Until** | Hold until a condition is true — machine idle, tank above a level, signal high. |
| **If / Else** | Branch on a condition. |
| **Repeat** | Loop, a fixed number of times or while a condition holds. |
| **Signal** | Emit a logic signal, so routines and the logic network can drive each other. |
| **Return** | Go home and start again. |

That is the whole vocabulary. Eleven cards, two of which are branches — expressive enough for real
batch work, small enough to learn in a minute.

#### Pods decide what a drone can do

An Operator Drone is a chassis; its **pods** decide its capabilities, exactly as cores decide a
conduit's and upgrades decide a machine's.

| Pod | Grants |
| --- | --- |
| **Cargo Pod** | Item capacity. |
| **Fluid Pod** | An internal tank, so the drone can carry fluid with its state intact. |
| **Thermal Pod** | Carries heat itself, for charging a process that needs it. |
| **Tool Arm** | Operating machines and swapping catalysts. |
| **Sensor** | The `Read` card, and richer conditions. |
| **Range Extender** | Working beyond pylon coverage, at a power cost. |

#### Debuggable by construction

A programmable system without debugging is misery, so this is designed in rather than added later:

- **The drone shows what it is doing.** The current instruction is visible above it in flight.
- **Step mode.** Advance one instruction at a time and watch.
- **Failures name themselves.** "Step 4: target inventory full" appears on the bay and as a logic
  signal, rather than the drone silently idling.
- **Holographic preview.** Editing a routine draws its path and targets in the world.

#### Why this does not make everything else pointless

Operator Drones are slow and they handle one task at a time. They are the wrong answer to anything
high-volume — a belt moves more ore in a second than a drone moves in a minute. They win precisely
where continuous flow loses: **irregular, conditional, multi-step work**, and locations that do not
justify permanent infrastructure.

At T11 the same routines run on remote colonies, where belts cannot reach at all, which is what
turns an off-world base from a resource trickle into a real factory.

### Control and logic

Pure belt layouts stop being enough once a base has to react to itself.

| Block | Role |
| --- | --- |
| **Signal Cable** | Carries multi-channel logic signals between components (not redstone-limited to 0–15). |
| **Logic Controller** | Condition-based enable/disable: *run this machine only while copper ingots < 500*. |
| **Arithmetic Unit** | Combines and transforms signals. |
| **Network Monitor** | Reports network power, deficit, and drone activity as signals. |
| **Redstone Interface** | Bridges Grindless signals to and from vanilla redstone, both directions. |

This is a deliberately simplified take on Factorio's circuit network: expressive enough for the
patterns people actually build (produce-on-demand, alarms, load balancing) without asking the
player to learn a second programming language.

From T2 onwards, Signal Cable is not a separate block: it is a **signal core** inside a Flux
Conduit, so logic rides the same infrastructure as everything else.

### Flux Conduits — one network for everything

By mid-game a serious base has belts for items, pipes for fluids, signal cable for logic and a heat
loop it would rather not run across half the factory. That is four parallel infrastructures solving
the same problem, and routing them around each other is busywork, not design.

**Flux Conduits** collapse all of it into one block. A conduit is a chassis; what it carries is
decided by the **cores** you insert into it:

| Core | Carries | Replaces |
| --- | --- | --- |
| **Item Core** | items, filtered and routed | belts for low-volume routing |
| **Fluid Core** | fluid, with its temperature and pressure intact | pipes |
| **Heat Core** | thermal energy directly, with no working fluid | a steam loop used only to move heat |
| **Signal Core** | multi-channel logic | Signal Cable |
| **Flux Core** | FU, for the rare spot a pylon cannot cover | nothing — this is the exception, not the rule |

Several cores coexist in one conduit block, so a single run can carry ore, coolant, heat and the
logic that controls them. One block, one route, four jobs.

The Heat Core deserves a note: moving heat *as heat* rather than as hot fluid is both more
realistic and genuinely useful, because it lets reactor waste heat reach a distant process without
plumbing a loop there and back.

#### Named networks, not coloured channels

EnderIO's channels are the standard solution and they do not survive scale: sixteen colours, and by
the time a base is large you cannot remember what purple meant.

Grindless conduits use **named networks**. You name a network — `iron-bus`, `reactor-coolant` — and
each endpoint subscribes by name. Names are unlimited, self-documenting, and a conduit tells you
what it belongs to when you look at it.

#### Visible, diagnosable, upgradeable

Three failings of existing conduit mods, addressed on purpose:

- **You can see the flow.** A conduit shows direction and load — colour by content, density by
  utilisation. A saturated line looks saturated.
- **Every segment reports itself.** Throughput, utilisation and backpressure are readable as logic
  signals, and the [Process Atlas](docs/MACHINES.md#the-route-viewer) can highlight the limiting
  segment in a line. "My factory stopped and I do not know why" should never be the answer.
- **Cores upgrade independently.** Bore (throughput), filtering, routing (priority, round-robin,
  overflow), insulation for the heat core, and at the top **Phase** — within one named network,
  distance stops mattering.

| Conduit tier | Throughput per core | Power | Era |
| --- | --- | --- | --- |
| **Basic Conduit** | low | none for items, pumps still needed for fluid | T2 |
| **Flux Conduit** | moderate | small, constant | T3 |
| **Phase Conduit** | high | proportional to load and to distance from ambient | T9 |
| **Singular Conduit** | effectively unlimited within a network | high | T13 |

#### Why belts and pipes survive this

A universal conduit that is strictly better than belts would delete the best part of the mod, so it
is deliberately not strictly better:

- **Belts are free and conduits are not.** Item cores draw power per item moved; a belt moves bulk
  for nothing. For a mine feeding a smelter — the highest-volume line in any base — belts stay
  correct forever.
- **A belt is also a buffer.** A full belt is a few thousand items of storage in transit. A conduit
  holds almost nothing, so a line fed by conduit stalls the moment production hiccups.
- **Pipes exploit physics for free.** Gravity, head pressure and a well-built condensate loop cost
  no power at all. The fluid core always costs power.

So the division that emerges is the one real factories have: **belts and pipes move bulk, conduits
move logistics.** The ore line is a belt. The forty-seven different components feeding an assembler
array are conduits, because running forty-seven belts there would be absurd.

At T13 the Singular Conduit genuinely can replace everything, and by then power is abundant enough
that some players will — which is a legitimate way to play and an expensive one. The trade is the
same one the mod makes everywhere: **pay in layout, or pay in power.**

---

## System 5 — Fluids, pressure and phase

*The other half of logistics, and the system that makes the futuristic tier feel earned.*

Most tech mods treat a fluid as an item with a different texture: it has an amount, it sits in a
tank, and a pipe moves it at a fixed rate. That is easy to implement and completely uninteresting
to play.

Grindless treats a fluid as a **state**, not a thing. What a fluid does depends on its temperature
and its pressure, moving it costs real work, and letting it cool or depressurise changes it into
something else. That single decision is what makes plumbing a layout puzzle instead of a chore —
and it is what lets the late game go somewhere genuinely futuristic without stopping being
physical.

### Fluids are states, not items

Every fluid stack carries **volume, temperature and pressure**. The same substance at different
points on that curve is a different resource with a different job:

| Fluid | State | Source | Used for |
| --- | --- | --- | --- |
| **Water** | ambient | Pump, rain catcher | washing, coolant feed, slurry |
| **Steam** | 100–250 °C, low pressure | Thermal Generator, Heat Exchanger | Steam Turbine — the T2 power backbone |
| **Superheated steam** | 250–600 °C, high pressure | reactor heat | the same turbine at far higher output |
| **Supercritical water** | >374 °C, >22 MPa | reactor primary loop | carries several times the heat per unit volume |
| **Ore slurry** | ambient, dense | Pulverizer fed with water | the wet processing line |
| **Leachate** | corrosive | Chemical Washer | strips byproducts; destroys the wrong pipe |
| **Etching acid** | corrosive | Chemical Washer | circuit boards — see [Fabrication](#fabrication-the-factory-builds-the-factory) |
| **Ultrapure water** | ambient, filtered | Cryogenic Plant, Vacuum Furnace | integrated circuits, crystal growth |
| **Coolant** | cryogenic | Cryogenic Plant | reactor loops, superconductors |
| **Liquid oxygen / nitrogen** | cryogenic | air separation | rocket fuel, cryo lines, life support |
| **Deuterium / Helium-3** | pressurised gas | Centrifuge; lunar regolith | fusion fuel |
| **Molten metal** | 1000 °C+ | Arc Furnace tap | direct casting and alloying, skipping the ingot step |
| **Plasma** | magnetically contained | Fusion Reactor, Particle Accelerator | exotic synthesis, the highest weapon tier |

The rule that turns this into gameplay rather than bookkeeping: **heat is neither free nor
discarded.** Steam that cools condenses back into water inside the pipe. A turbine fed condensate
instead of steam stalls. So the correct build is a *loop* — feed, boil, work, condense, return —
and closing that loop efficiently is the same kind of satisfying layout problem as a belt bus.

Molten metal is the clearest example of the design paying off in both directions at once. It is
completely realistic — this is how real foundries work — and it is a genuine shortcut: tapping the
Arc Furnace straight into a casting line skips the ingot stage entirely, at the cost of having to
keep the metal hot.

### Pressure is the throughput limit

Pipes do not move fluid by magic, and this is where the realism earns its place:

- **Liquids** fall downhill for free. Head pressure is real, so a tank on a tower is a valid,
  zero-power distribution strategy.
- **Gases** move only down a pressure differential. No pump, no flow, no exceptions.
- **Throughput** is a function of pipe tier, pressure differential and viscosity. Long runs lose
  pressure, which is what booster pumps are for.
- **Over-pressurise** a pipe past its rating and it ruptures: it vents loudly and visibly and can
  be repaired in place. It is never a base-deleting explosion — that punishes experimentation.
  Pressure relief valves exist precisely so a careful player never sees a rupture.
- **Corrosive fluids** eat the wrong pipe material. Running leachate through a T1 line is a
  mistake the game lets you make, tells you about, and lets you fix.

| Pipe | Tier | Rated for | Notes |
| --- | --- | --- | --- |
| **Clay Conduit** | T1 | ambient, gravity only | Unpowered. Available before any power network, like the first belt. |
| **Pressure Pipe** | T2 | MV pressures, hot | The workhorse. Corrosion-resistant lining. |
| **Cryo Line** | T6 | cryogenic, insulated | No boil-off while intact. |
| **Plasma Conduit** | T7 | magnetic containment | Consumes FU merely to stay intact; containment loss vents the line. |

| Component | Role |
| --- | --- |
| **Hand Pump** | T1, unpowered, slow. The bootstrap water source. |
| **Electric Pump** | Creates pressure. The thing that actually makes a gas network move. |
| **Booster Pump** | Restores pressure mid-run on long lines. |
| **Pressure Relief Valve** | Vents above a set point. Cheap insurance against rupture. |
| **Check Valve** | One-way flow. |
| **Fluid Manipulator** | The inserter for fluids: moves fluid between pipes, tanks and machines. |
| **Condenser** | Steam back to water, recovering heat into a loop. |
| **Boiler** | Water to steam, at a temperature set by its heat source. |
| **Separator** | Splits a mixed or multi-phase fluid into its components. |
| **Fluid Reader** | Emits fill level, temperature and pressure as logic signals. |

### The Phase Network — when fluids stop being plumbing

At T9 the mod's central idea arrives for fluids. A **Phase Manifold** is to fluids exactly what a
Flux Pylon is to power: inside its coverage area, any registered tank or machine can push and pull
any fluid the network holds, with no pipes at all.

This is deliberately *not* a strict upgrade. Dematerialising a fluid costs FU per unit,
proportional to how far that fluid sits from ambient — moving cryogenic coolant or plasma through
the phase network is expensive, and moving supercritical steam is very expensive. A well-built
pipe loop stays cheaper forever.

So the choice is the same one the belt-versus-drone decision offers: pay in layout, or pay in
power. Players who enjoy plumbing keep plumbing and are rewarded for it; players who are done with
plumbing can buy their way out. Neither is wrong, and the mod does not force the transition.

### Tanks

| Tank | Tier | Notes |
| --- | --- | --- |
| **Basic Tank** | T1 | Unpressurised, ambient only. Hot fluid will not enter it. |
| **Industrial Tank** | T2 | Rated pressure and temperature; blocks combine into one larger multiblock tank. |
| **Cryo Tank** | T6 | Insulated. Boils off slowly if it loses power — a real reason to care about brownouts. |
| **Containment Sphere** | T7 | Plasma and exotics. Powered containment. |

Every tank obeys the shared container contract — filters, buffer targets, configurable auto-void
and signal output. See [Containers](#containers-buffers-filters-and-voiding).

### Why fluids are not an optional side system

Two hooks make fluids load-bearing rather than decorative, and both are deliberate:

1. **Wet processing beats dry processing.** Slurry and leachate steps in the ore chain give
   materially better yields and recover byproducts that the dry line simply loses.
2. **Each frontier consumes fluid-made components.** T2 pumps need polymer seals, T4 controllers
   need etchant and ultrapure water, and later matrices need cryogens, plasma media and life-support
   loops. That is the [fabrication](#fabrication-the-factory-builds-the-factory) rule.

Neither hook is a wall. The dry chain keeps working forever, so a player who hates plumbing is
slowed, never stopped.

---

## System 6 — Tools and equipment

New tools should remove chores and create decisions. Each of these exists to delete a specific
category of busywork.

### Handheld tools

| Tool | Tier | What it does |
| --- | --- | --- |
| **Prospector's Scanner** | T1 | Surveys chunk veins — material, richness, reserve — with an overlay showing nearby surveyed chunks. |
| **Flux Conduit** | T1 | Right-click two pylons to link them manually. |
| **Multitool** | T0 | Right-click rotates a Grindless block. Sneak-click picks it up with contents and facing intact (ADR-0069). Does not mine (ADR-0055). |
| **Flux Drill** | T2 | Powered mining tool. Area modes (1×1, 3×3, vein-mine, tunnel), silk/fortune modules, runs on a portable cell — no durability, only charge. |
| **Blueprint Tool** | T2 | Captures a region as a **blueprint** and stamps it elsewhere. Construction drones build it from real items in the network. Blueprints are saveable, nameable and shareable between worlds. |
| **Deconstruction Planner** | T2 | Marks a region for drones to tear down and return to storage. |
| **Terraformer** | T3 | Flattens, fills and shapes terrain in a marked region, consuming FU and actual blocks. |
| **Matter Pattern Slate** | T3 | Portable pattern storage for the replication system. |

**Blueprints are the most important tool in this list.** Once a player has designed a good
extractor outpost, rebuilding it forty times by hand is precisely the tedium this mod exists to
remove. Blueprints turn "I have solved this problem" into "I have solved this problem
permanently", which is exactly what makes Factorio's mid-game feel good.

### Modular armour

Modular powered armour, in the tradition of Factorio's power armour. It is a **chassis line**,
not one T2 unlock ([ADR-0067](docs/DECISIONS.md#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier)).
Four pieces per tier, each with a grid of **equipment modules**. Larger grids and better modules
come with the next chassis. Modules pull out intact — experimenting is free, same as machine
upgrades.

| Chassis | Tier | What the last one could not do |
| --- | --- | --- |
| **Voltaic Harness** | T1 | Protection and a Flux Cell. No onboard generation. Vanilla armour stays valid until this exists. |
| **Flux Exosuit** | T2 | Network Tap, mobility, shields. You recharge by walking through pylons. |
| **Arc Exosuit** | T3 | **Miniature Arc Reactor** — generates FU while worn, on the same Arc Cells the factory reactor burns. |
| **Exotic Exosuit** | T10 | High-density cell, radiation shielding and orbital life support. |

| Module | First chassis | Effect |
| --- | --- | --- |
| **Flux Cell** | T1 | Onboard energy buffer. |
| **Network Tap** | T2 | Recharges the suit from any pylon supply area you walk through. |
| **Exoskeleton Legs** | T2 | Movement and jump. |
| **Thruster Pack** | T2 | Flight, with a real energy cost. |
| **Shield Projector** | T2 | Regenerating personal shield that absorbs damage before health. |
| **Auto-Repair Unit** | T2 | Repairs held and worn equipment from Matter. |
| **Night Vision** | T2 | Exactly what it says. |
| **Magnet Module** | T2 | Pulls dropped items toward you. |
| **Miniature Arc Reactor** | T3 | Generates FU while worn. Consumes Arc Cells. |
| **Personal Drone Bay** | T3 | A handful of construction drones that follow you and build blueprints anywhere. |

The grid layout is a genuine decision — energy generation competes with shields competes with
mobility — and it scales from "one cell" to a full late-game suit. There is no free worn
generator before T3: a Portable Reactor that needs no factory is not a module.

### The Arc Reactor

T3 / F3. The factory plant and the suit core are **one unlock** (ADR-0067).

A formed multiblock that produces **FU directly** at Arc voltage. It is not the Arc Furnace
(that smelts), not fission (heat into steam, neighbour bonus), not fusion (T7 D–T ignition).

- **Feed** is a manufactured **Arc Cell** from a named processing line. If the line stops, the
  reactor starves. The miniature suit reactor burns the same cell.
- **Coolant or stability** is real logistics, not flavour. Hatches take cells and fluids.
- **Playable**, not a trophy: output, starve, restart. Failure is recoverable (quench / idle),
  not a crater.
- Construction drones exist at this tier, so the structure can be a real design problem.

Exact cell chemistry, hatch layout and size stay open until that slice. Slice F starts when
this machine (or another that needs hatches) is scheduled — not as an empty kernel.

---

## System 7 — Resonance, defence and weapons

### Resonance — industry has a cost

Factorio's pollution is what stops the game being a peaceful idle builder: your factory is *loud*,
and something notices.

Grindless machines emit **Resonance** into the chunk they occupy, proportional to how hard they
are running. Resonance spreads to neighbouring chunks and decays over time, and it can be seen
directly through the Scanner's overlay.

High Resonance has consequences:

- **Aberrations** — hostile entities drawn to and empowered by resonant chunks. They scale with
  local Resonance, not with world difficulty, so the threat is something the player *caused* and
  can therefore reason about and manage.
- Hostile spawning pressure rises around heavy industry, and is essentially absent around a
  modest base.

This is what keeps a fully automated base from being a solved, static object. It also gives the
defence layer a reason to exist, and ties combat back into the automation loop: turrets need
ammunition, ammunition is manufactured, manufacturing raises Resonance, Resonance brings more
Aberrations. That feedback loop *is* Factorio.

Resonance is fully configurable and can be switched off by pack authors who want a peaceful pack.

### Defence

| Block | Tier | Behaviour |
| --- | --- | --- |
| **Ballistic Turret** | T1 | Fires manufactured ammunition. No power required — it can defend an outpost before that outpost has a grid. |
| **Laser Turret** | T2 | Draws FU directly from pylon coverage. No ammunition logistics, but it goes dark in a brownout. |
| **Tesla Turret** | T3 | Chains between targets. Heavy power draw. |
| **Flux Wall** | T1–T3 | Blast-resistant barrier, tiered. |
| **Shield Projector** | T3 | Projects a damage-absorbing dome over a pylon's supply area, fed by the network. |
| **Combat Drone Bay** | T3 | Launches escorting combat drones. |

The Ballistic/Laser split is a real trade-off rather than a straight upgrade: ballistic turrets
keep firing when the grid browns out, laser turrets never need a supply line. Good bases use both.

### Weapons

All Grindless weapons are manufactured, powered and fed by the production chain — never simply
found.

| Weapon | Tier | Behaviour |
| --- | --- | --- |
| **Gauss Rifle** | T2 | Electromagnetic coilgun. Consumes FU plus slugs, which the Assembler makes from any dense metal in the pack. Chargeable shot: hold for a heavier hit. |
| **Arc Thrower** | T2 | Chains lightning between nearby targets, drains the suit's cell fast. |
| **Plasma Caster** | T8 | Superheated projectile with area damage and a burn field. |
| **Railgun** | T8 | Extreme single-target damage, long charge, pierces everything in a line. |
| **Fusion Lance** | T14 | A sustained beam. The late-game answer to anything still standing. |
| **Flux Grenade** | T2 | Standard explosive, produced on a belt line. |
| **EMP Charge** | T8 | Disables Aberration abilities and drains hostile energy in an area. |
| **Singularity Charge** | T14 | Pulls entities and loose items to a point, then collapses. |

Ammunition tiers scale with the materials the pack provides, discovered through the same tag
system as everything else — so a pack with exotic late-game metals automatically gets exotic
late-game ammunition, with no compat work.

---

## System 8 — The futuristic tiers

The late campaign is where the mod stops being about *getting* materials and starts being about
*transforming* them. Every machine here is a multiblock, and each one is a project. The first of
those plants is the [Arc Reactor](#the-arc-reactor) at T3 / F3 — still a factory you feed, not
a trophy. Fission at T5, fusion at T7, particle engineering at T8 and singularity power at T14
each receive their own factory era rather than sharing one compressed "endgame tier".

### Fission Reactor

A T5 multiblock reactor running on fuel rods assembled from any fissile material in the pack —
discovered by tag, so uranium, thorium, plutonium or whatever a modded pack adds all work.

- Produces **heat**, which drives **Heat Exchangers** → **steam** → **Steam Turbines**.
- Requires active **coolant** circulation. Coolant flow is a real logistics problem.
- Adjacent reactor blocks boost each other's output, exactly like Factorio's neighbour bonus, so
  reactor *layout* is an optimisation puzzle worth solving.
- Produces **Depleted Rods**, reprocessable in the Chemical Washer for a partial fuel return.

**On failure:** an under-cooled reactor **SCRAMs** — it shuts itself down and needs to be
restarted — rather than cratering the base. A meltdown that deletes hours of work is the single
most tedium-generating mechanic in the genre, and this mod's whole thesis is against that.
Explosive failure is available as an opt-in config for packs that want it.

### Fusion Reactor

The T7 power source. T6 cryogenic infrastructure separates and stores deuterium; a running
reactor breeds tritium in a lithium blanket. High-field superconducting magnets confine the
plasma, so chemistry, materials, control and power remain coupled after ignition.

- **Ignition** costs a large burst of energy, so a fusion plant must be bootstrapped by an
  existing grid — a satisfying moment where the old base powers on the new one.
- Once burning, it produces power at a scale that trivialises every earlier generator, which is
  the correct feeling for a fusion reactor.
- Containment requires continuous power. Lose containment and the reaction simply quenches — a
  safe, restartable failure.
- Consumes essentially free fuel, so its real cost is the infrastructure to build and sustain it.

### Particle Accelerator

A T8 large-ring multiblock, and one of the most important machines in the mod.

| Mode | What it does |
| --- | --- |
| **Transmutation** | Converts one material into another. Cost is derived from the two materials' relative rarity in the loaded pack, so it is automatically balanced for *any* mod set. The universal answer to "I have twelve thousand copper and no tin." |
| **Exotic synthesis** | Produces materials that exist nowhere else: antimatter, exotic isotopes, strange matter. |
| **Matter creation** | Converts raw energy into generic Matter at a deliberately dreadful rate — E = mc² is not a good deal, and it should not be. |
| **Research** | Irradiates samples and records beam data used by the T4+ Research Station to make reusable physical patterns. |

Transmutation is the endgame's real payoff. It closes the loop opened on the first day: once you
have an accelerator, *any* material in the pack can become *any other*, and the only remaining
currency is energy. The player has fully converted a resource problem into a power problem, which
is the most satisfying possible end state for a mod about deleting the resource grind.

### Singularity Reactor

The T14 Galactic power project. An artificial micro-singularity fed with Matter produces power at
a scale where the constraint is no longer generation but the network's ability to carry and
coordinate it. It supports Type III expansion and the final T15 route; it is not itself victory.

### Supporting endgame machines

| Machine | Role |
| --- | --- |
| **Centrifuge (T5)** | Isotope separation, fuel enrichment, deuterium extraction. |
| **Heat Exchanger / Steam Turbine (T5)** | Converts reactor heat into FU. |
| **Cryogenic Plant (T6)** | Deep liquefaction and superconductor production. |
| **Quantum Assembler (T9)** | Resolves long component chains and programmable matter in one plant. |
| **Matter Condenser (T9)** | Compresses Matter for storage and high-throughput transport. |

---

## System 9 — Orbit and the planets

*Factorio launches a rocket and the game ends. Here it is where the game opens up.*

Space is the natural destination for a mod about turning resource problems into energy problems,
because orbit is where energy stops being scarce and where the interesting materials actually are.
The design goal is that **going to space must be a payoff, not a trophy** — every orbital object
has to do real, continuous work for the base back home.

### Getting there

| Step | Block | Notes |
| --- | --- | --- |
| 1 | **Launch Pad** (multiblock, T10) | Assembles and launches rockets. Requires Particle-era materials, Quantum guidance and an Orbital Matrix. |
| 2 | **Rocket** | Assembled from parts on a production line. Carries payload mass to orbit. Consumed on launch. |
| 3 | **Mass Driver** (multiblock, T10) | The bulk answer. An electromagnetic launcher that fires cargo canisters to orbit for pure FU and no rocket. Cheap per kilogram, but useless for anything fragile or alive. |
| 4 | **Orbital Platform** (T10) | Your space station. Built from launched modules; expands into a real base. |
| 5 | **Horizon Gate** (multiblock, T11) | Dialed ring. Instant presence after a far gate is delivered. Not a mining dimension ([ADR-0068](docs/DECISIONS.md#adr-0068--horizon-gates-are-commute-infrastructure-not-mining-dimensions)). |

Rockets are for the first trip and for anything crewed. **The Mass Driver is what makes an orbital
economy viable** — once it is running, sending material up is an energy cost rather than a
manufacturing project, which converts space from an event into infrastructure. **The Horizon Gate
is what kills the commute** — once a pair is standing, you walk; bulk ore still prefers the
driver.

### Satellites that actually do something

Satellites are permanent once launched — **no orbital decay, no refuelling, no maintenance
minigame**. They draw FU from the network through the relay grid. The cost is power and launch
mass, never chores.

| Satellite | What it does for you |
| --- | --- |
| **Survey Satellite** | Continuously reveals chunk vein data — material, richness, reserve — in a wide band beneath its orbit. This is the single most useful object in the mod: it replaces walking around with the Prospector's Scanner, and turns "find a rich tungsten chunk" into reading a map. |
| **Solar Power Satellite** | Collects unobstructed sunlight and beams it to a ground **Rectenna**. Constant output — no night, no weather, no seasons. The first genuinely passive large-scale power source. |
| **Relay Satellite** | Extends the Flux Network *across dimensions* and provides **bandwidth**, the resource that telepresence and remote colonies consume. Nothing remote works without relays. |
| **Sentinel Satellite** | Global Resonance map plus early warning of Aberration swarms, and target designation for orbital strikes. |
| **Logistics Satellite** | Enables drop-pod targeting and orbital item routing between your ground base, station and colonies. |
| **Deep Survey Satellite** | Surveys *other planets* from orbit. The prerequisite for exploiting a world you have never set foot on. |

A satellite constellation is a build worth optimising: coverage bands, bandwidth budget and power
draw all interact, and a well-planned network quietly improves every other system in the mod.

### Why build in orbit

Orbit is not just a place to put satellites. Vacuum and microgravity enable processes that are
impossible on the ground, which is the honest, physics-flavoured reason to move production up.

| Machine | Why it needs space |
| --- | --- |
| **Vacuum Furnace** | No atmosphere means no oxidation. Higher purity, better yields than any ground smelter. |
| **Zero-G Crystallizer** | Crystals grown without convection or sedimentation are flawless — the only source of the substrates advanced circuits need. |
| **Orbital Cryo Plant** | Space is already cold. Liquefaction and superconductor production at a fraction of the ground energy cost. |
| **Vacuum Deposition Chamber** | Thin-film and exotic alloy work with no contamination. |
| **Orbital Assembly Bay** | Builds things too large to construct under gravity: rocket stages, colony packages, station modules. |

Orbital industry also emits **no Resonance to the surface**, so a station is the one place heavy
production does not attract Aberrations. Moving your dirtiest processing off-world is a real
strategic option with a real cost.

### The planets

**Luna, the Drift and the galactic-centre chamber are implemented prototypes**
(ADR-0095–ADR-0099). Their worlds, travel logic and smoke coverage stay because they validate
the destination systems. Their Industrial recipes and premature quest route do not belong to
survival. The real campaign reaches Luna through T10 rocket flight, the Drift aboard the T13
supraluminal station and Sagittarius through the T15 final route. The Lunar and Starward Links
remain registered compatibility/test placeholders, not shortcuts.

**If a space mod is already installed, Grindless uses its planets.** Ad Astra, Galacticraft,
Beyond Earth and friends are detected at runtime, and Grindless layers its orbital mechanics,
chunk veins and remote-exploitation systems onto *their* dimensions rather than adding a
competing set. This is the same principle as the tag-driven material registry: integrate with the
pack, never duplicate it.

Only when no space mod is present does Grindless add its own fallback set:

| World | Character | Hazard | Signature resource |
| --- | --- | --- | --- |
| **Luna** | Airless, low gravity, close | Vacuum | Helium-3 — premium fusion fuel |
| **Tharsis** | Cold desert, thin atmosphere | Cold, dust storms | Iron-rich veins at high richness |
| **Vulcan** | Volcanic, hot, dense | Heat, ash | Heavy metals, geothermal power |
| **Kryos** | Ice moon, subsurface ocean | Extreme cold | Deuterium, cryogenic volatiles |
| **Erebus** | Rogue planet, no star, dark | Darkness, radiation | Exotic isotopes, strange matter |
| **Thalassa** | Ocean world, thick air | Pressure, corrosion | High-pressure chemistry, dissolved volatiles |
| **Helios** | Tidally locked | Dayside heat, nightside cold | Extreme process envelopes, not a second ore list |

Every world has its own **vein weighting pool**, so the chunk vein system from
[System 2](#system-2--resource-genesis) works identically off-world with different odds. Planets
are not reskins — they are different probability distributions over the same, fully pack-aware
material registry. A pack with exotic modded metals automatically gets them distributed across
planets by rarity, with no compat work.

Solar output scales with distance from the star, hazards demand specific life support, and local
gravity affects Mass Driver cost — so *where* you build is a genuine engineering decision.

### Four ways to exploit a world

This is the part of the design I am most confident is worth building, because it is the question
every space mod answers badly: **what do you do with a planet you cannot be standing on?**

#### 1. In person

Fly there, land, build normally. Full control, full flexibility, full risk — and you have to
actually be there, which means you are not at home doing anything else.

Requires life support appropriate to the hazard: the Exosuit chassis with the right modules, or a
pressurised base.

#### 2. Telepresence — the Proxy Frame

Stay home. Build a **Proxy Frame** — a robot body — and ship it to the target world. Then sit at
a **Telepresence Terminal** and *be* it: walk, mine, build, fight, with your own inventory linked
through the relay network.

- Consumes **bandwidth** from your satellite constellation and FU continuously.
- Signal latency scales with distance, which softly caps how far telepresence is practical
  without a dedicated relay chain.
- If the frame is destroyed you lose the frame, not your inventory and not your life. Failure
  costs materials, never progress.
- You can maintain several frames and switch between them.

Telepresence is the answer to the genre's worst pattern: the mod that makes you fly forty minutes
each way to check on an outpost. It keeps the *interesting* part of being on another planet — the
building, the exploring, the hostile environment — and deletes the commute.

#### 3. Automated colonies — the Colony Core

For worlds you want to *harvest* rather than inhabit, launch a **Colony Core** package. It lands,
unfolds, and runs without you.

| Property | Behaviour |
| --- | --- |
| **Output** | Extracts from local chunk veins at a rate set by its modules, the local vein richness and its power supply. |
| **Return path** | Ships material home by **Mass Driver** to an **Orbital Catcher**, or by drop pod to a ground beacon. |
| **Power** | Its own generation, or beamed down from a Solar Power Satellite. |
| **Integrity** | Slowly degrades under local hazards. Resupply — automatable via the return route in reverse — restores it. |
| **Expansion** | Additional modules launched later increase throughput, survivability and autonomy. |

**Colonies are simulated abstractly, not tick-by-tick.** A colony is a small state machine with a
computed production rate; it does not force-load chunks and does not run block entities while you
are away. Twenty colonies across the fallback worlds cost approximately nothing in server performance.

This is deliberate and load-bearing. The usual implementation — force-loading a remote base so its
machines keep ticking — is exactly how a server dies, and it is why most mods quietly discourage
the thing they advertise. Abstract simulation means the mod can genuinely encourage interplanetary
industry instead.

Colony integrity gives the system a slow, manageable rhythm: a colony wants attention every few
hours, not every few minutes, and the attention it wants can itself be automated. That is the
correct difficulty curve for something you are supposed to have dozens of.

#### 4. Horizon Gate — walk there

T11. In the tradition of a Stargate: a ring you dial, not a mining dimension you live in
(ADR-0068).

Deep Survey reveals **addresses**. You manufacture a second ring and deliver it once — rocket,
Mass Driver or colony package. After the pair exists, you walk through: players, Proxy Frames,
colony packages. Cargo *can* go through, at a worse FU-per-kilogram than the Mass Driver, so a
belt of ore still belongs on the driver.

The gate does not open a world whose only job is more ore. It dials planets already in the
registry. If Ad Astra (or a friend) is installed, those are the destinations. Vanilla Nether and
End stay out unless a pack author adds them by datapack.

### Orbital logistics

| Block | Role |
| --- | --- |
| **Orbital Catcher** | Receives Mass Driver canisters and feeds them into a belt or logistics network. |
| **Drop Pod Bay** | Targeted surface delivery, anywhere you have a beacon. |
| **Landing Beacon** | Marks a drop-pod destination. |
| **Rectenna** | Receives beamed power from Solar Power Satellites. |
| **Interplanetary Router** | Routes items between ground, station and colonies using logistics satellites. |
| **Space Elevator** (multiblock, T11) | Permanent, high-throughput, energy-only ground-to-orbit link. Makes the Mass Driver obsolete and orbit feel genuinely attached to the world. |

### Why space is not just "more numbers"

Every earlier system gains a new dimension rather than being replaced:

| System | What space adds |
| --- | --- |
| **Flux Network** | Relay satellites extend it across dimensions; bandwidth becomes a second, scarcer network resource. |
| **Resource Genesis** | Survey satellites map veins remotely; every planet is a different distribution to exploit. |
| **Matter Replication** | Orbital and exotic materials feed patterns that are unobtainable on the ground. |
| **Logistics** | Mass drivers, drop pods, the space elevator and Horizon Gates turn logistics interplanetary. Presence is the gate; bulk is the driver. |
| **Tools** | The Exosuit becomes life support; Proxy Frames become a second body. |
| **Resonance** | Orbital industry emits none to the surface — relocation is a real strategic answer. |
| **Futuristic tiers** | Helium-3 and deuterium from Luna and Kryos improve mature fusion routes. |

---

## Energy: Flux Units

Grindless has its own energy type, **Flux Units (FU)**, and it is fully interoperable:

| Conversion | Rate | Why |
| --- | --- | --- |
| FU ↔ FE / RF | **1 : 1, lossless** | Deliberate. Any other ratio forces rounding on every transfer, which silently creates or destroys energy over millions of ticks and turns the mod into a bug magnet in large packs. |
| FU ↔ EU | **4 : 1** (configurable) | The long-standing IC2 convention. |

The 1:1 FE rate is a considered decision, not laziness. Mods that pick a "flavourful" ratio like
1 FU = 2.5 RF have to round on every single transfer, in both directions, on thousands of
machines, sixty times a second. Those fractions of a unit accumulate into energy quietly being
created or destroyed, and the resulting bug reports are unreproducible. A 1:1 rate makes the
bridge provably lossless and the mod a good citizen in a 300-mod pack.

What makes FU genuinely *different* is its semantics, not its exchange rate.

### Flux tiers

Every Tn machine is nominally rated for Fn. LV through IV remain familiar display aliases
on F1 through F5; there is still only one power ladder (ADR-0038, ADR-0107).

| Tier | Name | Nominal |
| --- | --- | ---: |
| **F0** | Bootstrap / Manual | 8 FU/t |
| **F1 / LV** | Voltaic | 32 FU/t |
| **F2 / MV** | Industrial | 128 FU/t |
| **F3 / HV** | Arc | 512 FU/t |
| **F4 / EV** | Precision | 2,048 FU/t |
| **F5 / IV** | Nuclear | 8,192 FU/t |
| **F6** | Cryogenic | 32,768 FU/t |
| **F7** | Fusion | 131,072 FU/t |
| **F8** | Particle | 524,288 FU/t |
| **F9** | Quantum | 2,097,152 FU/t |
| **F10** | Orbital | 8,388,608 FU/t |
| **F11** | Planetary | 33,554,432 FU/t |
| **F12** | Stellar | 134,217,728 FU/t |
| **F13** | Interstellar | 536,870,912 FU/t |
| **F14** | Galactic | 2,147,483,648 FU/t |
| **F15** | Event Horizon | 8,589,934,592 FU/t |

The rule is always `F(n) = 8 × 4ⁿ FU/t`. F0-F9 therefore keep their existing values.

**Under-volting degrades speed smoothly rather than stalling the machine** — roughly halving
throughput per tier below requirement. Run an MV machine on LV and it works at about half speed;
run it two tiers down and it crawls. It never simply refuses to work.

This is the opposite of the usual tech-mod convention and it is intentional. Hard voltage gates
turn a power shortfall into a wall, and walls are exactly the kind of progress-stopping tedium
this mod exists to remove. A soft curve preserves all the *incentive* to upgrade while never
leaving a player stuck and confused.

(Over-volting is safe. Nothing explodes. Machines that explode when you connect the wrong cable
are a tedium generator, not a difficulty mechanic.)

### Endgame rate

F15 is the last named tier, not an arbitrary numerical finish. Black-hole infrastructure
scales inside postgame T15 from 8,589,934,592 FU/t to `Long.MAX_VALUE` FU/t. That avoids
fifteen empty F16-F30 labels while preserving the fourfold rule. F14 is the first nominal
rate above the FE `int` boundary. Sums and practical buffers near the absolute ceiling need
a wider internal amount type before that content exists (ADR-0088, ADR-0107).

### Performance: idle machines cost nothing

A base with thousands of machines is the design's explicit target, which makes machine ticking the
one place where a bad decision shows up as unplayable rather than as merely slow.

Grindless machines **do not tick by default** (ADR-0042). A machine subscribes work when something
makes that work necessary and unsubscribes the moment it is not, driven by change notifications
rather than polling — so an idle machine's tick is a check that it has nothing to do. Periodic work
carries a per-machine offset derived from block position, because throttling with raw game time
synchronises every machine in the world onto the same tick and leaves the worst tick exactly as bad
as it was.

Recipe lookup is indexed and cached rather than scanned (ADR-0043): a machine in steady state
re-checks the recipe it is already running and never searches at all.

Both patterns are taken from GregTech CEu Modern and Mekanism rather than invented here. Their
convergence on the same answers is the best available evidence that the obvious implementations do
not survive at this scale.

### Area distribution

Energy moves through the **pylon network**, not block-to-block adjacency. A machine is powered
because it stands inside a supply area, full stop. See [System 1](#system-1--the-flux-network).

### Brownouts

When a network's draw exceeds its supply for a sustained period, it **browns out**: every machine
on the network slows *proportionally* to the deficit, together.

Most mods instead let machines starve individually, which produces the worst possible failure
mode — some random subset of your base stops, with no indication of why, and the player goes
block-hunting. A proportional brownout is immediately legible: everything is visibly sluggish, the
network readout shows a deficit, and the fix is obviously "add generation or remove load".

Legible failure is a feature. It is the same principle as the soft voltage curve: never leave the
player stuck without knowing why.

---

## Progression: Control Matrices and research

Progress is physical. Every machine from T1 onward contains a **Control Matrix** rated for
its technology and Flux tier. If the factory can manufacture that matrix, it can manufacture
the machine; there is no second world-scoped permission that makes a visible recipe silently
fail. Under-volting still works, slowly, so building the first machine of a new tier never
requires an impossible power bootstrap.

The first route to Tn uses a Tn−1 matrix plus products from materials, chemistry,
computation/control and energy. Those domains interact before they converge: electronics
needs ultrapure chemicals and stable power; chemistry needs controlled equipment and
corrosion-resistant materials; energy plants need both controllers and process fluids.

Five architectures prevent the matrix line from becoming one fifteen-step tax:

| Architecture | Frontier tiers | Later payoff |
| --- | --- | --- |
| **Relay** | T1–T3 | the cheap, low-infrastructure foundation |
| **Integrated** | T4–T6 | semiconductor routes batch-produce old Relay ratings |
| **Superconducting** | T7–T9 | cryogenic routes compress Integrated production |
| **Photonic** | T10–T12 | zero-g optics replace long terrestrial interconnect chains |
| **Causal** | T13–T15 | relativistic control compresses every earlier architecture |

Old routes remain valid. New architectures trade greater infrastructure for lower unit cost,
larger batches or higher throughput. A higher-rated matrix can substitute for a lower one,
but is rarely economical.

| Tier | Theme | Braided progression and signature scale |
| --- | --- | --- |
| **T0 — Bootstrap** | Escape velocity | Assisted extraction, primitive material/chemical/control parts and manual power culminate in the first Relay Matrix batch. |
| **T1 — Voltaic** | First factory | Milling, reduction, ceramic insulation, coils and thermal power culminate in the Assembler and T2 Relay Matrix. |
| **T2 — Industrial** | Closed loops | Steel and wet beneficiation feed sulfur, electrolysis and resin loops; motors, discrete control, steam and solar turn them into a multi-line factory. |
| **T3 — Arc** | Powered construction | Refractories, hot metal, Arc Cells and industrial control feed the Arc Reactor and Construction Drones. |
| **T4 — Precision** | Purity and control | Pressure chemistry, ultrapure water, electronic silicon, lithography and the Research Station produce Integrated Matrices and reusable advanced patterns. |
| **T5 — Nuclear** | Designed cores | Fuel chemistry, isotope cascades, reactor alloys and radiation-hard control converge in parametric fission. |
| **T6 — Cryogenic** | Deep cold | Liquid helium, superconductors, high-field magnets and pulse storage prepare the fusion era. |
| **T7 — Fusion** | Sustained plasma | Breeding blankets, neutron-resistant materials, plasma control and fuel loops sustain fusion. |
| **T8 — Particle** | Designed nuclei | Accelerator size, target chemistry, isotope recovery and beam control create exotic precursors. |
| **T9 — Quantum** | Programmable matter | Quantum substrates, nanoprocesses, advanced replication and coherent power make matter a designed input. |
| **T10 — Orbital** | Ground and orbit | Propellant, composites, guidance, satellites, beamed power and zero-g industry form one distributed factory. |
| **T11 — Planetary** | Worlds as infrastructure | ISRU, life support, telepresence, colonies, routing and Horizon Gates reach Kardashev Type I. |
| **T12 — Stellar** | A star as infrastructure | Stellar materials, the Stellar Forge, photonic control and a Dyson network reach Type II. |
| **T13 — Interstellar** | Several systems | Causal logistics, supraluminal navigation and the Drift move the factory beyond its home star. |
| **T14 — Galactic** | Self-expanding industry | Antimatter, singularity power, distributed computation and abstract replication reach Type III. |
| **T15 — Event Horizon** | The galactic centre | Relativistic shielding, final navigation and galactic power reach Sagittarius. Arrival is victory; black-hole exploitation is postgame. |

Later tiers are approximately longer and contain more meaningful content than earlier ones.
The growth is in interacting routes, conditions, logistics and projects — never merely larger
stacks or longer timers. Automation grows with the burden: Assembler, Process Cards,
construction drones, Assembly Fields, abstract colonies and self-replicating infrastructure.

The old **Research Terminal** is not discarded. At T4 it becomes the **Research Station**:
samples plus Data Cores produce reusable physical patterns for advanced components and process
modes. A pattern is a catalyst in a recipe, not a permanent global unlock and never the gate
for an ordinary machine.

The early ramp remains intentional. The Crude Extractor removes manual mining first; the
Assembler removes hand fabrication; drones remove block placement; satellites remove manual
survey; gates remove repeated travel; abstract colonies and swarms remove remote ticking and
manual expansion.

### Mechanical tier gate

Development closes one tier completely before opening the next (ADR-0108). A tier is complete only
when a survival player can enter from the previous frontier, run every required material, chemical,
control and energy line, automate the machines and logistics, handle byproducts and normal failure
states, and manufacture the next Control Matrix or terminal objective without commands or hidden
permissions. Fast behaviour checks hold policy; targeted boot/interaction smokes cover changed
Minecraft-only seams; the full historical smoke matrix is an integration pass rather than the
inner loop.

Final hero art, bespoke animation, polished sound, effects and full localization are deferred to
the project-wide art pass. Functional placeholder assets, readable names, tooltips, menus and
machine-state feedback remain part of mechanical playability.

---

## Processing chain

Every recipe below is generated at runtime from tags, so it exists for every material in the pack.

```
chunk vein ──> Terrestrial Extractor ──> raw material
                                          │
      ┌───────────────────────────────────┤
      │                                   │
      ▼                                   ▼
  Arc Furnace                        Pulverizer
      │                                   │
      ▼                                   ▼
  1x ingot                              dust
                                          │
                          ┌───────────────┤
                          ▼               ▼
                    Arc Furnace     Chemical Washer
                          │               │
                          ▼               ▼
                     2x ingot       purified dust
                                          │
                                          ▼
                                     Arc Furnace
                                          │
                                          ▼
                              2x ingot + byproduct
```

Yields deliberately sit in the same range as Thermal and Mekanism. Grindless is designed to
**feed** the ore doubling your pack already has, not to invalidate it.

Beyond the core chain, the **Centrifuge** handles isotope separation and fuel enrichment, and the
**Particle Accelerator** short-circuits the whole diagram by transmuting any material directly
into any other at an energy price.

### The wet line

Every step from the Pulverizer onwards has a fluid-assisted variant that yields more and recovers
byproducts the dry line throws away. Feeding the Pulverizer water produces **ore slurry**; washing
slurry with **leachate** separates trace materials that dry pulverizing simply loses; and tapping
the Arc Furnace as **molten metal** lets a casting line skip the ingot stage entirely, provided the
metal is kept hot.

The dry chain above keeps working forever and is never removed. The wet line is strictly an
optimisation the player opts into — which is the right shape for a system that also happens to be
on the critical path to circuits (see [System 5](#system-5--fluids-pressure-and-phase)).

The diagram above is the dry spine. The full specification — every beneficiation and reduction
stage with its conditions, times and ratios, and the twenty routes they compose into — is in
[`docs/PROCESSES.md`](docs/PROCESSES.md).

---

## Fabrication: the factory builds the factory

In Factorio you never hand-craft a factory. You hand-craft the first burner drill, and from then
on the factory produces everything else, including itself. That loop is the entire reason the game
is compelling, and almost every Minecraft tech mod throws it away by letting the player assemble a
fusion reactor in a 3×3 grid from a full inventory.

The single most important structural rule in Grindless:

> **Past the bootstrap, machines are not crafted. They are manufactured.**

| Tier | How you obtain the machine |
| --- | --- |
| **T0** | Crafting table. The bootstrap, and only the ungated bootstrap. |
| **T1** | Crafting table or Press, but every machine consumes a T1 Relay Matrix. The first matrix is made in a small batch; the Assembler then automates it. |
| **T2–T8** | **Assembler** and specialised fabricators — physical Control Matrix, components, FU and time. |
| **T9** | **Quantum Assembler** — long component chains and programmable matter resolved in one plant. |
| **T10–T12** | **Orbital Assembly Bay** and **Assembly Field** — vacuum, zero gravity and blueprint-scale construction. |
| **T13–T15** | Distributed Assembly Fields — systems manufacture and deliver megastructure sections rather than individual blocks. |

A machine above T1 has **no crafting-table recipe at all**. Not a hidden one, not a deliberately
expensive one — none exists. The only way to obtain it is to run the process, which means owning
and feeding a production line.

### Components — the intermediate economy

Machines are not built from ingots. They are built from parts, and those parts are where the real
production chain lives:

| Component | Tier | Built from | What it gates |
| --- | --- | --- | --- |
| **Machine Casing** | T1 | plates | The first thing any Assembler makes. |
| **Control Matrix** | T1–T15 | a prior rating plus the tier's braided material, chemical, control and power products | Every machine at its rating. Five architectures add better routes to older ratings. |
| **Motor** | T2 | casing stock + copper coil | Anything that moves. |
| **Pump** | T2 | casing + motor + seals | The entire fluid tier. |
| **Circuit Board** | T3–T4 | silicon wafer + **etching acid** | Industrial control, then Integrated Matrices. |
| **Integrated Circuit** | T4 | board + gold + **ultrapure water** | Precision machines and logic. |
| **Superconductor** | T6 | fine wire + cryogenic processing | Fusion magnets and Superconducting Matrices. |
| **Quantum Core** | T9 | coherent substrate + exotic material + cryogenic coolant | Quantum fabrication and programmable matter. |
| **Containment Ring** | T7+ | superconductor + composite + coolant | Fusion first; particle, singularity and horizon systems later. |

Every ingredient above is resolved from tags, so "plates", "gold" and "silicon" mean whatever the
installed pack provides — the same runtime material registry everything else uses (ADR-0004). A
pack that already has a circuit will have Grindless use *its* circuit rather than registering a
rival one.

Note how the fluid dependencies are placed. T2 pumps need resin; T4 circuits need etchant and
ultrapure water; T6 and later matrices need progressively deeper coolants and process media.
Fluids are not a side system the player can skip, but each new dependency begins at a scale the
current factory can automate.

### Why the rule exists

1. **It makes the factory the point.** The reward for building a production line is that it builds
   the next production line. Without this, machines are just expensive items and the factory is
   decoration.
2. **It closes the hand-craft bypass.** A player who arrives with a full inventory of a pack's
   mid-game materials would otherwise skip straight past everything Grindless is about.
3. **It makes progression physical.** A matrix that has to be fed into an assembly line is a
   production target. A world flag in a menu is not.

### The counterweight

This rule must never become the grind it exists to delete. The guard rails are deliberate:

- **Matrices and patterns are reusable infrastructure.** Matrix production is automated, and a
  Research Station pattern is a catalyst rather than a consumed permission token.
- **Assemblers are cheap and parallelise.** Building ten of them is a throughput decision, not a
  punishment. The answer to "this is slow" is always "build another one", which is the correct
  answer in a factory game.
- **The bootstrap is always recoverable.** T0 stays ungated, and the first T1 Relay Matrix has a
  small manual route, so a player who loses everything can rebuild the ladder from local
  materials. There is no softlock, ever.
- **Pack authors can relax it.** The gate is datapack-driven, so a pack that wants hand-craftable
  T2 machines can have them without a mod patch.

---

## Containers: buffers, filters and voiding

Every container in Grindless — item crates, fluid tanks, and the input and output buffers built
into machines — obeys one shared contract. Learn the interface once and it is the same everywhere,
on a T1 tank and on a T11 colony module alike.

| Control | Behaviour |
| --- | --- |
| **Filter** | Lock a slot or tank to an item, a fluid or a tag. A locked slot keeps its identity while empty, so a sorted line never re-sorts itself the moment it runs dry. |
| **Buffer target** | The amount to keep on hand. Drones and logic read anything below the target as demand and anything above it as surplus. |
| **Capacity limit** | Cap a slot or tank below its physical maximum — useful to stop one material from eating a shared buffer. |
| **Auto-void** | Discard anything above a configurable threshold. |
| **Void mode** | Overflow only, filtered materials only, or everything. |
| **Side I/O** | Per face: insert, extract, both or nothing. |
| **Priority** | Independent insertion and extraction priority, so overflow and top-up routes resolve predictably. |
| **Signal output** | Fill level as a logic signal — and for fluids, temperature and pressure too. |

### Auto-void, handled carefully

Auto-void is the difference between an ore line that jams overnight and one that runs for a month
unattended. It is also the easiest possible way for a player to silently destroy something they
wanted. Both things are true, so the feature is built defensively:

- **Off by default.** On every container, always, with no exceptions.
- **Enabling it is explicit.** A deliberate confirmation, never a stray click in a crowded UI.
- **A voiding container is visibly marked.** A particle effect and a glow, so you can walk into a
  base you built three months ago and see at a glance which containers are discarding.
- **It trims, it never empties.** Voiding applies only above the threshold. A voiding container
  still holds its buffer.
- **It announces itself.** A distinct logic signal while actively voiding, so an alarm can be
  built for it.
- **It refuses to void the irreplaceable.** Anything on the replication blacklist — creative
  items, quest rewards, pack-unique items — is never discarded, regardless of settings.

This matters more here than in most mods. A system that generates its recipes from tags at runtime
produces byproducts for materials the player has never heard of, and unwanted byproducts backing up
a line are the characteristic failure mode of that design. Auto-void is the release valve, and
`Overflow Gate` plus a voiding `Storage Crate` is the canonical answer to "what do I do with eleven
thousand gravel".

---

## Block and item catalogue

The full planned content set, for reference. Tier is the technology and Control Matrix rating
needed to manufacture it.

### Power generation

| Block | Tier | Notes |
| --- | --- | --- |
| Hand Crank Dynamo | T0 | Manual. The bootstrap generator. |
| Thermal Generator | T1 | Burns any furnace fuel. |
| Solar Array | T2 | Daylight only; pairs with buffers. |
| Steam Turbine | T2 | Consumes steam from any heat source. |
| Arc Reactor (multiblock) | T3 | Direct F3 FU. Arc Cells. Same fuel as the suit core (ADR-0067). |
| Heat Exchanger | T5 | Reactor heat → steam. |
| Fission Reactor (multiblock) | T5 | Neighbour bonus; SCRAM on overheat. |
| Fusion Reactor (multiblock) | T7 | Ignition cost; breeding blanket; quench-safe failure. |
| Singularity Reactor (multiblock) | T14 | Type III generation and final-route power. |

### Distribution

| Block | Tier | Notes |
| --- | --- | --- |
| Flux Pylon chassis MK I–MK VIII | T1–T15 | Supply area + drone area; upgraded in place. |
| Flux Capacitor Bank | T2 | Network energy storage buffer. |
| Flux Transformer | T2 | Steps voltage between tiers. |
| Network Monitor | T2 | Power, deficit and drone telemetry. |

### Resource acquisition

| Block | Tier | Notes |
| --- | --- | --- |
| Crude Extractor | T0 | Slow, cheap, immediate. |
| Terrestrial Extractor | T1 | The workhorse. |
| Deep Core Drill (multiblock) | T5 | Weighted planetary pool. |

### Processing

| Block | Tier | Notes |
| --- | --- | --- |
| Arc Furnace | T1 | Smelting. |
| Pulverizer | T1 | Ore → dust. |
| Chemical Washer | T2 | Purification and byproducts. |
| Assembler | T1 | Multi-ingredient crafting. |
| Lithography Unit | T4 | Wafers, masks and Integrated Matrix batches. |
| Centrifuge | T5 | Isotopes, enrichment, deuterium. |
| Cryogenic Plant | T6 | Deep liquefaction and superconductors. |
| Fusion Reactor support plant | T7 | Fuel cleanup and blanket processing. |
| Particle Accelerator (multiblock) | T8 | Transmutation, exotics and sample irradiation. |
| Quantum Assembler | T9 | Long ingredient chains and programmable matter. |

### Fluids

| Block | Tier | Notes |
| --- | --- | --- |
| Clay Conduit | T1 | Unpowered, gravity feed, ambient fluids only. |
| Hand Pump | T1 | Slow and manual. The bootstrap water source. |
| Basic Tank | T1 | Unpressurised; refuses hot fluids. |
| Pressure Pipe | T2 | The workhorse. Corrosion-resistant lining. |
| Electric Pump / Booster Pump | T2 | Creates pressure; restores it on long runs. |
| Boiler / Condenser | T2 | Water ↔ steam. Closes the loop. |
| Fluid Manipulator | T2 | The inserter for fluids. |
| Industrial Tank (multiblock) | T2 | Rated pressure; adjacent blocks merge into one tank. |
| Pressure Relief Valve / Check Valve | T2 | Rupture insurance; one-way flow. |
| Separator | T2 | Splits mixed and multi-phase fluids. |
| Fluid Reader | T2 | Level, temperature and pressure as logic signals. |
| Cryo Line / Cryo Tank | T6 | Insulated. Boil-off when unpowered. |
| Plasma Conduit / Containment Sphere | T7 | Powered magnetic containment. |
| Phase Manifold | T9 | Coverage-area fluid network. Costs FU per unit moved. |

### Matter

| Block | Tier | Notes |
| --- | --- | --- |
| Pattern Scanner | T2 | Stores item patterns. |
| Deconstructor | T2 | Items → Matter. |
| Replicator | T4 | Matter + FU → patterned item. |
| Matter Condenser | T9 | Compressed Matter storage and throughput. |

### Logistics

| Block | Tier |
| --- | --- |
| Conveyor / Flux / Mag-Lev / Phase Belt | T1 / T2 / T3 / T9 |
| Splitter, Merger, Tunnel Belt, Sorter, Overflow Gate, Belt Reader, Flow Meter | T1–T2 |
| Manipulator: Crude / Fast / Stack / Filter | T1–T2 |
| Signal Cable, Logic Controller, Arithmetic Unit, Redstone Interface | T2 |
| Basic / Flux / Phase / Singular Conduit | T2 / T3 / T9 / T13 |
| Conduit cores: Item, Fluid, Heat, Signal, Flux | T2–T7 |
| Conduit upgrades: Bore, Filter, Routing, Insulation, Phase | T2–T9 |
| Drone Bay, Provider / Requester / Buffer / Storage Crate | T3 |
| Operator Bay, Operator Drone | T3 |
| Construction Drone, Assembly Field (multiblock) | T3 / T10 |
| Design Terminal — blueprint validation and simulation | T3 |
| Instruction Cards (11 kinds), Routine Card, Locator | T3 |
| Drone pods: Cargo, Fluid, Thermal, Tool Arm, Sensor, Range Extender | T3–T11 |

### Research and defence

| Block | Tier |
| --- | --- |
| Research Station | T4 |
| Ballistic Turret, Flux Wall | T1 |
| Laser Turret | T2 |
| Tesla Turret, Shield Projector, Combat Drone Bay | T3 |

### Orbital and planetary

| Block | Tier | Notes |
| --- | --- | --- |
| Launch Pad (multiblock) | T10 | Assembles and launches rockets. |
| Mass Driver (multiblock) | T10 | Bulk cargo to orbit for pure energy. |
| Orbital Catcher | T10 | Receives Mass Driver canisters. |
| Orbital Platform modules | T10 | The space station itself. |
| Rectenna | T10 | Receives beamed satellite power. |
| Vacuum Furnace | T10 | Higher purity than any ground smelter. |
| Zero-G Crystallizer | T10 | Flawless crystal substrates. |
| Orbital Cryo Plant | T10 | Efficient liquefaction and superconductors. |
| Vacuum Deposition Chamber | T10 | Thin films, exotic alloys. |
| Orbital Assembly Bay | T10 | Builds what gravity will not allow. |
| Telepresence Terminal | T11 | Drive a Proxy Frame on another world. |
| Colony Core + modules | T11 | Abstractly simulated automated colony. |
| Drop Pod Bay, Landing Beacon | T11 | Targeted surface delivery. |
| Interplanetary Router | T11 | Item routing across dimensions. |
| Space Elevator (multiblock) | T11 | Permanent ground-to-orbit link. |
| Horizon Gate (multiblock) | T11 | Dialed pair. Commute, not a mining dimension (ADR-0068). |

### Satellites

| Satellite | Tier | Utility |
| --- | --- | --- |
| Survey Satellite | T10 | Maps chunk veins from orbit. |
| Solar Power Satellite | T10 | Constant beamed power. |
| Relay Satellite | T10 | Cross-dimension network + bandwidth. |
| Sentinel Satellite | T10 | Global Resonance map, swarm warning. |
| Logistics Satellite | T10 | Drop-pod targeting, orbital routing. |
| Deep Survey Satellite | T11 | Surveys other planets remotely. |

### Items

| Item | Tier |
| --- | --- |
| Multitool | T0 |
| Prospector's Scanner, Flux Conduit | T1 |
| Machine Casing, Motor, Pump, Circuit Board | T1–T2 |
| Chassis Upgrade Kit MK II–MK VIII | T2–T14 |
| Machine upgrades: Speed, Parallel, Efficiency, Yield, Precision, Insulation, Containment, Catalyst Feed, Damping, Recovery | T2–T9 |
| Integrated Circuit | T4 |
| Superconductor | T6 |
| Containment Ring | T7+ |
| Quantum Core | T9 |
| Flux Drill, Blueprint Tool, Deconstruction Planner | T2 |
| Voltaic Harness / Flux Exosuit / Arc Exosuit / Exotic Exosuit (4 pieces) + equipment modules | T1 / T2 / T3 / T10 |
| Arc Cell | T3 |
| Terraformer, Matter Pattern Slate | T3 |
| Data Core / Advanced Data Core / Exotic Data Core | reusable pattern media from T4; existing items migrate rather than disappear |
| Matter, Raw materials, dusts, purified dusts, plates | various |
| Ammunition: slugs, plasma cells, rail slugs | T2–T8 |
| Gauss Rifle, Arc Thrower, Flux Grenade | T2 |
| Plasma Caster, Railgun, EMP Charge | T8 |
| Fusion Lance, Singularity Charge | T14 |
| Rocket parts, cargo canisters, satellite chassis | T10 |
| Life support modules (vacuum, thermal, radiation) | T10 |
| Deuterium / helium-3 / exotic isotopes / strange matter | T6 / T10 / T8 / T9 |
| Proxy Frame, colony modules, orbital survey charts | T11 |

---

## Compatibility strategy

Grindless is designed to be installed into a 300-mod pack that its author has never seen, and to
work correctly there on the first launch.

### Tags, never item IDs

Every material interaction goes through tags. Grindless reads both tag conventions:

| Convention | Examples |
| --- | --- |
| **Forge** | `forge:ores/*`, `forge:ingots/*`, `forge:raw_materials/*`, `forge:dusts/*`, `forge:gems/*`, `forge:storage_blocks/*`, `forge:nuggets/*`, `forge:plates/*` |
| **Fabric `c:`** | On 1.20.1-era Fabric the convention is still the older plural-item style (`c:iron_ores`, `c:iron_ingots`), *not* the newer `c:ores/iron` hierarchy. Both forms are handled. |

The material registry is rebuilt on **every datapack reload**, so adding a mod, changing a
datapack or running `/reload` is picked up without a restart.

### Graceful degradation

A material is not required to have every form. The processing chain is constructed per material
from whatever forms exist:

- ore but no dust → pulverizing is skipped, ore smelts directly;
- gem-type materials → routed through gem-appropriate steps instead of ingot ones;
- no ore anywhere in the pack → the material simply never appears in a chunk vein.

### No duplicate items

Grindless only registers its own item for a material when **no other mod already provides one**.
In a pack with Thermal installed, Grindless produces Thermal's dusts. In a pack without it, it
provides its own. Either way there is exactly one copper dust, and no JEI page full of
near-identical items.

### Energy interoperability

| Platform | Bridge |
| --- | --- |
| Forge / NeoForge | `IEnergyStorage` via `ForgeCapabilities.ENERGY` |

Architectury does **not** provide a unified energy API, so this bridge is written by hand in this
project — `FluxStorage` in `common/` with an `@ExpectPlatform` implementation in `forge/`.

FU and FE are **1:1 and lossless** (ADR-0006). The one asymmetry is width: FU is `long` because the
Flux ladder reaches 2 097 152 FU/t, while FE is `int`, so a transfer across the boundary saturates
at `Integer.MAX_VALUE` instead of overflowing and is treated as a request rather than a promise
(ADR-0037).

### Space mod integration

The same rule applies to dimensions as to materials: **detect and integrate, never duplicate.**
Luna is the exception that now ships (ADR-0088, ADR-0095): one original world, whether or
not another space mod is installed. The rows below are the held interplanetary design.
They are not what the game does today, and they are not a reason to remove Luna.

| Situation | Behaviour |
| --- | --- |
| Ad Astra, Galacticraft, Beyond Earth or similar installed | Grindless registers **no planets of its own**. Its satellites, chunk veins, colonies and telepresence all operate on *their* dimensions, and each detected world gets a vein weighting pool derived from its existing characteristics. |
| No space mod installed | Grindless adds its own minimal set — Luna, Tharsis, Vulcan, Kryos, Erebus, Thalassa, Helios. Horizon Gates dial those worlds. |
| A space mod added to an existing world later | Its worlds are picked up on the next load and become valid targets. |

Planet detection runs through a datapack-definable registry, so a pack author can point Grindless
at any dimension, set its hazards and bias its vein pool, without writing code. The result is that
Grindless adds an orbital *economy* to whatever space mod the pack already chose, rather than
competing with it — which is the only version of this feature that is actually useful in a real
modpack.

### Pack author controls

Everything above is overridable: material weights, vein richness curves, replication blacklists,
Resonance intensity (including off), energy conversion ratios, and per-tier research costs — all
through config and datapack, so a pack can tune Grindless without forking it.

---

## Why this is not a cheat mod

This deserves a direct answer, because "a mod that gives you resources" describes a lot of bad
mods.

| Cheat mods do this | Grindless does this |
| --- | --- |
| Give resources for free | Charges energy, space, infrastructure and research for every single unit |
| Are available immediately, fully formed | Ramps across seven research tiers; orbit is 25+ hours in |
| Make one optimal strategy | Makes layout, logistics and power budgeting into real decisions |
| Trivialise every other mod in the pack | Tuned to *feed* other mods' processing, at comparable yields |
| Remove all pressure | Adds Resonance, which makes industry noisy and consequential |
| Let you duplicate anything cheaply | Prices replication from the item's own recipe graph |
| End the game | Converts a resource problem into a power problem, which is a *better* game |

The honest summary: Grindless deletes the part of Minecraft that is *waiting*, and charges you for
the part that is *building*. You will spend more total time engaged with a pack that has it
installed, not less — you will just spend almost none of it in a tunnel.

---

## Architecture

**Forge 1.20.1 only**, structured with **Architectury**. The guiding rule is that the platform
module stays as thin as possible: shared logic lives in `common/`, and `forge/` contains only its
entrypoint and the handful of platform-specific bridges.

Keeping the `common/` + `forge/` split for a single loader is deliberate. It costs nothing now that
it exists, it keeps loader-specific code visible behind `@ExpectPlatform` instead of spread through
the codebase, and it means a future port is a build change rather than a rewrite (ADR-0039).

```
grindless/
├── AGENTS.md                 working agreement: read this before changing anything
├── CHANGELOG.md              what changed, in order
├── build.gradle              root: shared config for all subprojects
├── settings.gradle           includes common, forge
├── gradle.properties         all version coordinates, single source of truth
├── SETUP.ps1                 Windows bootstrap (see "Environment setup")
│
├── common/                   ~95% of the codebase; loader-agnostic
│   └── src/main/java/io/github/ezequiel24123z/grindless/
│       ├── Grindless.java            mod entrypoint and init
│       ├── api/                      public API for other mods
│       │   └── energy/               FluxStorage, FluxTier contracts
│       ├── block/                    block definitions
│       │   └── entity/               block entities
│       ├── client/                   rendering
│       │   └── screen/               GUI screens
│       ├── container/                the shared container contract + change notification
│       ├── energy/                   FU implementation, RF/EU conversion
│       ├── item/                     items, tools, weapons
│       ├── machine/                  chassis marks, upgrades, tick subscriptions
│       ├── material/                 runtime tag-driven material registry
│       ├── menu/                     containers
│       ├── network/                  Flux Network: pylons, links, SavedData
│       ├── process/                  conditions, bands, envelopes — the recipe/machine seam
│       ├── orbital/                  launch, satellites, station, mass driver
│       │   ├── colony/               abstractly simulated remote colonies
│       │   ├── planet/               planet registry + space mod detection
│       │   └── telepresence/         Proxy Frames and the terminal
│       ├── recipe/                   recipe types and runtime generation
│       ├── registry/                 Architectury DeferredRegisters
│       ├── research/                 Research Terminal progression
│       ├── vein/                     deterministic chunk veins
│       └── util/
│
├── forge/                    Forge entrypoint + capability energy bridge
│                             (this jar also loads on NeoForge 1.20.1)
├── docs/                     MACHINES.md, PROCESSES.md, DECISIONS.md (ADRs), DESIGN.md
├── tools/                    asset generation scripts
└── .github/workflows/        CI
```

### Key technical decisions

Summarised here; each one is recorded in full — with the alternatives that were rejected — in
[`docs/DECISIONS.md`](docs/DECISIONS.md), which is authoritative where the two disagree.

| Decision | Rationale |
| --- | --- |
| Network state in `SavedData`, not block entities | Avoids a per-tick graph walk; survives chunk unloads. |
| Spatial index over pylons | Coverage lookup is `O(log n)` in pylons, not `O(n)` in blocks. |
| Belt items as lane data, not entities | `ItemEntity` per item is the standard belt-mod performance disaster. |
| Colonies simulated abstractly, never force-loaded | Force-loading remote bases is how servers die. Abstract state machines let the mod *encourage* interplanetary industry instead of quietly discouraging it. |
| Planets detected from installed space mods | Adding a competing dimension set to a pack that already has one is worse than adding nothing. |
| Satellites have no upkeep | Orbital maintenance is a chore, and chores are what this mod deletes. Cost is launch mass and power. |
| Veins derived from seed + coords | No stored world data; identical across reloads; free until used. |
| Material registry rebuilt on datapack reload | Picks up pack changes without a restart. |
| FU ↔ FE at exactly 1:1 | Provably lossless; no rounding drift across millions of ticks. |
| Recipes generated at runtime from tags | One implementation covers every mod, present and future. |
| Replication priced from the recipe graph | Closes the exploit that breaks flat-priced duplication mods. |
| Hand-written energy bridge | Architectury has no unified energy API. |
| Mojmap + Parchment mappings | Readable names, permissively licensed, standard on 1.20.1. |
| Fluids carry temperature and pressure | The same substance in different states is a different resource; closing a heat loop becomes a layout puzzle instead of bookkeeping. |
| Pipes early, Phase Network late | Same trade as belts versus drones: pay in layout or pay in power, never forced to switch. |
| Machines above T1 cannot be hand-crafted | The factory builds the factory. Closes the full-inventory bypass that makes tech mods trivial in big packs. |
| One container contract, auto-void off by default | Learn the interface once; the release valve a tag-driven system needs must never silently delete things. |
| Conduits cost power, belts do not | Keeps belts correct for bulk forever, so the universal network never deletes the best part of the mod. |
| Named conduit networks, not coloured channels | Sixteen colours stop being readable long before a base stops growing. |
| Chassis marks widen the condition envelope | Upgrading unlocks recipes instead of adding a speed number, so tiering is a goal rather than a tax. |
| Construction drones, so multiblocks can be massive | Placement tedium, not design, is what caps multiblock size everywhere else. Automate the placing, keep the designing. |
| Blueprints simulated before they are built | Trial and error at a cost of ten thousand components is punishment, not engineering. |
| Every machine upgrade trades one resource for another | An upgrade that is strictly better is not a decision, it is a tax you pay once. |

---

## Supported platforms

| Minecraft | Forge | NeoForge | Fabric |
| --- | --- | --- | --- |
| **1.20.1** | ✅ | ✅ *(loads the Forge jar unchanged)* | ❌ *(dropped — ADR-0039)* |

**Fabric was dropped deliberately**, not abandoned for lack of time. Supporting it meant writing
every platform bridge twice against unrelated models — Forge capabilities versus Team Reborn Energy
with its transaction system — which doubled the implementation, the review and the failure surface
in exchange for an audience the mod cannot serve yet, since nothing is playable. One loader keeps
the work focused. The full reasoning, including what restoring Fabric would cost, is in ADR-0039.

On 1.20.1 there is no `net.neoforged:neoforge` artifact at all — that coordinate starts at
`20.2.x`. NeoForge's 1.20.1 release is `net.neoforged:forge:1.20.1-47.1.x`, a soft-fork of
Forge 47 that keeps the `net.minecraftforge` package names and the `META-INF/mods.toml` metadata
format. One Forge-compiled jar therefore loads on both. A dedicated NeoForge subproject arrives
with the 1.20.2+ port, where the real artifact and Architectury's `neoforge()` target exist.

The codebase keeps the Architectury layout even with one loader: essentially all logic lives in
`common/`, and `forge/` is thin.

### Why there is no `neoforge/` directory yet

This was verified directly against `https://maven.neoforged.net/releases/net/neoforged/`:

| Artifact | Versions | Earliest |
| --- | --- | --- |
| `net.neoforged:neoforge` | 1760 | **`20.2.x`** — Minecraft 1.20.2. Nothing for 1.20.1 exists. |
| `net.neoforged:forge` | 63 | `1.20.1-47.1.7` … `1.20.1-47.1.106` |

So on 1.20.1 "NeoForge" *is* `net.neoforged:forge:1.20.1-47.1.x`, the original soft-fork of
Forge 47. It keeps the `net.minecraftforge` package names and `META-INF/mods.toml`, so a single
Forge-compiled jar is byte-for-byte loadable on both. Adding a third subproject today would
produce an identical jar under a different name — which is not multiloader support, just
duplication.

A real `neoforge/` subproject is added with the 1.20.2+ port, where the genuine artifact and
Architectury's `neoforge()` platform target both exist.

---

## Verified toolchain

Every coordinate below was checked against live Maven metadata rather than guessed, and Gradle
has successfully resolved Minecraft 1.20.1 with this exact set.

| Component | Version | Notes |
| --- | --- | --- |
| Minecraft | `1.20.1` | |
| Java | **17** | Required by 1.20.1. |
| Gradle | **8.8** | Matches architectury-loom 1.7.x. |
| `architectury-plugin` | `3.4.164` | |
| `dev.architectury.loom` | `1.7.423` | 1.8.x does not exist; 1.9.436 targets newer MC. |
| Architectury API | `9.2.14` | The 9.x line is the 1.20.1 line. |
| Forge | `1.20.1-47.4.23` | Latest 1.20.1. |
| NeoForge (1.20.1) | `net.neoforged:forge:1.20.1-47.1.106` | Soft-fork of Forge 47. |
| Fabric Loader | `0.16.14` | Compile-time only — Loom builds `common` in a Fabric-shaped environment even with Fabric disabled. Nothing Fabric ships in the jar. |
| Parchment | `2023.09.03` | `org.parchmentmc.data:parchment-1.20.1`. |
| Shadow | `8.1.1` | Bundles `common` into the Forge jar. |

All of these live in [`gradle.properties`](gradle.properties) as the single source of truth.

---

## Building

Requires **JDK 17**. Everything else is fetched by Gradle.

### Windows — first time

Run the bootstrap once, from a normal PowerShell window:

```powershell
powershell -ExecutionPolicy Bypass -File .\SETUP.ps1
```

It creates the source tree, finds a JDK 17 and Gradle 8.8 without a system-wide install,
generates the committed Gradle wrapper, and checks whether this checkout will hit the Windows
260-character `MAX_PATH` limit.

**That path check matters.** Deep package trees plus a long checkout path produce build failures
that look like corrupt files rather than path-length errors. If it warns you, either enable long
paths:

```powershell
# Admin PowerShell, then reboot
New-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem' `
  -Name LongPathsEnabled -Value 1 -PropertyType DWORD -Force
```

or clone somewhere short, such as `C:\mc\grindless`.

### Building

```bash
./gradlew :forge:build
```

The jar lands in `forge/build/libs/grindless-0.1.0-forge.jar`, and also loads on NeoForge 1.20.1.
**The first build downloads and decompiles Minecraft and takes several minutes** — that is normal
and only happens once.

### Development runs

```bash
./gradlew :forge:runClient
```

### Useful tasks

| Task | Purpose |
| --- | --- |
| `./gradlew build` | Everything. |
| `./gradlew :common:build` | Compile shared code only — the fastest feedback loop. |
| `./gradlew clean` | Wipe outputs (does not re-download Minecraft). |
| `./gradlew --refresh-dependencies build` | Force dependency re-resolution. |

---

## Roadmap

### Validated prototype foundation

The Forge 1.20.1 build already contains the playable T0–T2 factory foundation: extraction,
tag-driven materials, dry and wet ore routes, belts, fluids, logic, fabricated machines, tools,
matter scanning and the first two armour chassis. It also contains locally smoke-tested prototype
worlds and travel systems through Sagittarius. Those prototypes prove the technical seams; they
are not the survival campaign.

**Prototype-foundation milestone:** a player can go from an empty world to automated iron in under
fifteen minutes without mining it by hand. This is met, but it predates the complete T0 mechanical
gate and does not by itself accept Bootstrap.

### Campaign restoration *(current)*

ADR-0107 replaces the compressed six-tier schedule with T0/F0–T15/F15. Work resumes in small,
playable slices under the mechanical gate in ADR-0108:

1. **BO — survival shortcut withdrawn.** The eight prototype recipes are gone and the quest
   book and guide stop at electronic silicon. Registrations, worlds, commands and smokes remain.
2. **BP — close T0 Bootstrap *(active)*.** Audit the complete two-iron bootstrap, add matrix
   rating/architecture data and the recoverable first T1 Relay Matrix batch, extend F0–F15 without
   changing existing values, and accept T0 only after an end-to-end survival playthrough.
3. **BQ — physical T2 gate.** Move T2 fabrication from the global Industrial permission to a
   consumed T2 Relay Matrix and convert Data Cores into future pattern media.
4. **BR onward — grow the campaign tier by tier.** Each frontier ships its four interacting routes,
   its Control Matrix, its signature project and the automation that makes its larger scale
   manageable.

### Terrestrial factory eras

| Range | Campaign work |
| --- | --- |
| **T3 Arc** | Refractories, Arc Cells, construction drones, the multiblock kernel and fed Arc power. |
| **T4 Precision** | Pressure chemistry, ultrapure water, electronic silicon, lithography, Integrated Matrices and Research Station patterns. |
| **T5 Nuclear** | Fuel conversion, isotope cascades, reactor materials, heat exchange and parametric fission. |
| **T6 Cryogenic** | Deep cooling, liquid helium, superconductors, high-field magnets and pulse storage. |
| **T7 Fusion** | Fuel breeding, plasma-facing materials, containment control and sustained fusion. |
| **T8 Particle** | Accelerator scale, target chemistry, isotope recovery and exotic precursors. |
| **T9 Quantum** | Coherent substrates, programmable matter, Quantum Assembly and high-throughput replication. |

### Distributed factory eras

| Range | Campaign work |
| --- | --- |
| **T10 Orbital** | Rocket flight, ground–orbit logistics, satellites, beamed power and zero-g industry. |
| **T11 Planetary** | Life support, ISRU, telepresence, abstract colonies, Space Elevators, Horizon Gates and Type I. |
| **T12 Stellar** | Stellar materials, the Stellar Forge, photonic control, a Dyson network and Type II. |
| **T13 Interstellar** | Causal logistics, supraluminal navigation, multiple systems and the Drift. |
| **T14 Galactic** | Self-expanding industry, antimatter, singularity power, distributed control and Type III. |
| **T15 Event Horizon** | Relativistic shielding, final navigation and arrival at Sagittarius; exploitation follows as postgame T15. |

### Polish

The native Atlas solver, JEI/REI/EMI integration, configuration, localisation, accessibility,
balance and performance are developed alongside the campaign seams they explain rather than
postponed behind an obsolete 1.0 bucket. Version ports still wait until the Forge 1.20.1 campaign
is coherent.

### Version ports

| Target | Notes |
| --- | --- |
| **1.20.2 – 1.20.4** | Adds the real `neoforge/` subproject. |
| **1.21.x** | Current-generation packs. |
| **1.19.2** | Still a very large pack ecosystem. |

Because nearly everything lives in `common/`, ports are mostly a matter of mapping and registry
churn rather than rewriting systems. That was the entire reason for choosing Architectury on day
one.

---

## Implementation plan

Tracked order of work. Each step must build green before the next begins.

| # | Step | Status |
| --- | --- | --- |
| 1 | Design and documentation | ✅ done — this file and [`docs/DESIGN.md`](docs/DESIGN.md) |
| 2 | Root Gradle build files | ✅ done |
| 3 | Source directory tree | ✅ done |
| 4 | Subproject build scripts (`common`, `forge`) | ✅ done |
| 5 | Loader metadata (`mods.toml`, `pack.mcmeta`, lang) | ✅ done |
| 6 | Core registry layer (Architectury `DeferredRegister`) | ✅ done — T0 bootstrap set registered |
| 7 | **First green build** | ✅ **done** — `grindless-0.1.0-forge.jar` |
| 8 | Machine layer design — machines, multiblocks, processes | ✅ done — [`docs/MACHINES.md`](docs/MACHINES.md) |
| 9 | Process design — items, fluids, recipe graph, routes, ratios | ✅ done — [`docs/PROCESSES.md`](docs/PROCESSES.md) |
| 10 | Flux energy API + Forge capability bridge | ✅ done — `FluxStorage`, `FluxTier`, `FluxConversion` |
| 11 | The condition system: envelopes, bands, efficiency, legible faults | ✅ done — `process/` |
| 12 | Machine layer: chassis marks, upgrades, the tick-subscription model | ✅ done — `machine/` |
| 12b | The container contract + change notification | ✅ done — `container/` |
| 12c | `MachineBlockEntity` + the Hand Crank Dynamo, wired to its block | ✅ done |
| 12d | Exposing machine buffers as Forge capabilities | ✅ done — ADR-0045 |
| 13 | Pylon network, supply areas, `SavedData` | ✅ done — `network/`, ADR-0046 |
| 13b | The Flux Pylon block, and machines drawing from their network | ✅ done |
| 14 | Chunk veins + vein derivation and persistence | ✅ done — `vein/`, ADR-0047 |
| 14b | The **runtime tag scan** — `MaterialRegistry`, and the Crude Extractor | ✅ done — ADR-0050, ADR-0053 |
| 15 | **Slice A — First iron:** `ProcessRecipe` (item-first), Thermal Generator, Pulverizer, Arc Furnace, Pylon MK1 recipe, shared menu, Voltaic gate | ✅ done — ADR-0058, ADR-0059 |
| 16 | Recipe visibility: `VerifyRecipes` + atlas stub; JEI/REI/EMI when the graph exists | ✅ done — Atlas stub ADR-0066; JEI waits |
| 17 | **Slice B — First factory:** belts, splitter, manipulator, Terrestrial Extractor, Prospector's Scanner | ✅ done — ADR-0060 |
| 17b | **Slice G — Belt junctions:** merger, tunnel, overflow | ✅ done — ADR-0071 |
| 17c | **Slice H — Sorter:** peel a mixed line by item | ✅ done — ADR-0072 |
| 18 | **Slice C — Fluids:** state, Clay Conduit, Hand Pump, Basic Tank, gas capture, wet pulverizer | ✅ done — ADR-0062 |
| 19 | **Slice D — Factory builds factory:** Press, Machine Casing, Assembler, T2+ fabrication gate | ✅ done — ADR-0017, ADR-0063 |
| 20 | **Slice E — Energy spanning:** Flux Conduits, transformers, capacitor banks | ✅ done — ADR-0064; pylons stay coverage |
| 20b | **Kiln / R2 — Roast then reduce:** T1 Kiln, oxide form, SO₂ capture | ✅ done — ADR-0065; 1.15 and acid stay later |
| 21 | **Slice F — Multiblock kernel:** formed structure + hatches, when a machine needs size or hatches | rescheduled into the T3 Arc campaign after BQ (ADR-0067, ADR-0107) |
| 21b | **Slice I — T2 gate:** Industrial research, Advanced Data Core, MK2 gated | ✅ done — ADR-0073 |
| 21c | **Slice J — Wire and motors:** Wire Mill, wire form, mill coil, motor | ✅ done — ADR-0074 |
| 21d | **Slice K — Contact process:** Chemical Reactor, SO₂ → acid, pickle | ✅ done — ADR-0075 |
| 21e | **Slice L — Washer and B2:** Chemical Washer, washed crushed, vein byproduct | ✅ done — ADR-0076 |
| 21f | **Slice M — Electrolysis and air:** Electrolysis Cell, Atmospheric Intake, hydrogen and oxygen sinks | ✅ done — ADR-0077 |
| 21g | **Slice N — Fluid Well:** powered chunk water | ✅ done — ADR-0078 |
| 21h | **Slice O — Induction and caster:** melt an ingot; cast a plate | ✅ done — ADR-0079 |
| 21i | **Slice P — Better separation:** flotation and a magnetic split | ✅ done — ADR-0080 |
| 21j | **Slice Q — Heat and steam:** daylight power and a closed steam loop | ✅ done — ADR-0081 |
| 21k | **Slice R — T2 fluids and belts:** move steam, melt and stacks | ✅ done — ADR-0082 |
| 21l | **Slice S — Logic:** enable a machine from a count | ✅ done — ADR-0083 |
| 21m | **Slice T — Flux Drill:** mine with charge | ✅ done — ADR-0084 |
| 21n | **Slice U — Blueprint Tool:** save a layout and stamp it | ✅ done — ADR-0085 |
| 21o | **Slice V — Deconstruction Planner:** mark a region | ✅ done — ADR-0086 |
| 21p | **Slice W — Patterns:** scan an item; smash it to Matter | ✅ done — ADR-0101 |
| 21q | **Slice X — Voltaic Harness:** wear T1 modular armour | ✅ done — ADR-0102 |
| 21r | **Slice Y — Flux Exosuit:** T2 chassis, network tap, exoskeleton legs | ✅ done — ADR-0103 |
| 22 | T2+ industry: washer, flotation, electrolysis, solar/steam | ✅ done — build-out L–S |
| 23 | Tools and matter: scanner, deconstructor, replicator, construction drones | partly done — scanner/deconstructor and tools are in; drones move to T3, replication to T4/T9 |
| 24 | Orbital layer: launch, satellites, station | prototypes exist; survival implementation is rescheduled to T10 (ADR-0107) |
| 25 | Planetary layer: colonies, telepresence, planet registry, Horizon Gates | planned for T11 (ADR-0068, ADR-0107) |
| 26 | CI workflow | ✅ done — `ci.yml` + `tools/smoke-boot.sh`, ADR-0049 |
| 27 | **Autonomous build-out** | active — [`docs/BUILD-OUT.md`](docs/BUILD-OUT.md) preserves the shipped calendars and names BP as next |
| 28 | **Prototype expansion** | ✅ technically complete — worlds, travel and quests are validated prototypes; ADR-0107 withdraws their premature survival route |
| 28b | **BD — Electric-arc steel** | ✅ done — ADR-0090. 10 iron + 1 carbon → 10 steel in 140 s on the Arc Furnace. |
| 28c | **BE — Refractory brick** | ✅ done — ADR-0091. 1 slag → 1 refractory brick in 20 s at 1400 °C on the Arc Furnace. |
| 28d | **BF — Metallurgical silicon** | ✅ done — ADR-0092. 1 silica + 2 carbon → 1 metallurgical silicon + 2 B CO in 14 s at 1900 °C on the Arc Furnace. |
| 28e | **BG — Zone refining** | ✅ done — ADR-0093. 10 metallurgical silicon → 7 electronic silicon in 600 s at 1420 °C on the Arc Furnace. |
| 28f | **BH — Megastructures** | ✅ done — ADR-0094. Ground Array: eight casings and one controller store 6,553,600 FU while a pylon covers a complete ring. |
| 28g | **BI — Original planets** | ✅ done — ADR-0095. Luna: regolith, no ore, helium-3 from the extractor. A Lunar Link spends 102,400 FU to arrive; the return does not draw. |
| 28h | **BJ — Interstellar travel** | ✅ done — ADR-0096. The Drift: one deck, no ore. A Starward Link spends 6,553,600 FU to leave the star; the return does not draw. The link stays as a placeholder (ADR-0097). |
| 28i | **BK — Teleportation orbs** | named, not started. An alternate route of an unnamed magical material ends in orbs. Shift-right-click sets coordinates and dimension. Right-click teleports. The orb can sit on a pedestal. Not next. |
| 28j | **BL — Rocket ascent** | ✅ done — ADR-0097. A survey rocket climbs to the ceiling. The landing map offers the home world and Luna. Leaving home spends 102,400 FU; leaving Luna does not. |
| 28k | **BM — Supraluminal station** | ✅ done — ADR-0098. A station climbs to the ceiling. That ceiling is the Drift on the way out. Leaving home spends 6,553,600 FU; leaving the Drift does not. ADR-0099 sends that free ride to the centre. The Starward Link stays and no longer moves a player. |
| 28l | **BN — Arrival at the galactic centre** | ✅ done — ADR-0099. Riding the station from the Drift arrives in a sealed chamber. Leaving the Drift and leaving the chamber draw nothing. Leaving the chamber returns to the berth saved on the way to the Drift. No new link. |
| 28m | **BC — Original quest book and in-game guide** | ✅ done — ADR-0100. BO shortened its campaign to eleven reachable tasks ending at electronic silicon. |
| 29 | **T0–T15 progression design** | ✅ done — ADR-0107; four braided routes, five matrix architectures and the F0–F15 ladder |
| 30 | **BO — Withdraw prototype survival shortcut** | ✅ done — 107 graph rows, 39 Assembler rows and no prototype recipe or dimension quest |
| 31 | **BP — Close T0 Bootstrap / Control Matrix foundation** | **active** — F0-F15, rating/architecture model and registered T1 item are in; the four-unit survival batch is in, while migration, guidance and machine fixes remain |
| 32 | **BQ — Physical T2 gate** | pending after BP — introduce the consumed T2 matrix route |
| 33 | **T3–T15 campaign** | pending — ship each frontier as playable slices following the lattice in `PROCESSES.md` |
| 34 | **Mechanical tier acceptance** | active — ADR-0108; T1 remains locked until T0 passes the shared completion gate |

Step 7 was the first real milestone and it is cleared: a skeleton that actually compiles and
packages, which means every later step is validated the moment it is written rather than
accumulating as a pile of uncompiled code.

Steps 4, 5 and 7 originally covered Fabric as well. Fabric was dropped at step 10 (ADR-0039), and
the rows above describe what the project builds now rather than what it once built.

Step 11 originally also carried the `ProcessRecipe` type. That moved to step 15 so generation and
the type land together. Fluids (now slice C) are **not** a prerequisite for the type: ADR-0058
ships item I/O first and leaves fluid slots empty. Condition matching is already done and
standalone.

Rows 15–26 were a *system-layer* backlog (all T1 machines, then fluids, then belts, then
conduits, then multiblocks). ADR-0058 replaced that with playable slices. Asset generation (the
old row 19) was already brought forward and is done (ADR-0048). The old row numbers should not be
revived: a session that starts conduits or a multiblock framework before first iron is ignoring
the record.

### Where the project actually is

The consolidated branch is locally validated: build, all behaviour checks, documentation and
39 smoke scenarios pass. The genuinely playable survival foundation reaches T2 and includes
extraction, logistics, fluids, fabricated machines, tools and matter scanning. Electric-arc steel,
refractory brick, metallurgical silicon and zone refining extend that foundation.

Luna, the Drift, the survey rocket, the supraluminal station, Sagittarius and the original quest
book are also implemented and tested. ADR-0107 reclassifies that vertical route as prototype
infrastructure because its Industrial recipes bypass T3–T15. The destination code, registrations
and smoke coverage stay; their survival recipes and premature quest claims left in **BO**. The
book and guide now stop at electronic silicon. **BP** has introduced F0-F15, physical Control
Matrix rating/architecture data, the registered T1 Relay Matrix item and its recoverable four-unit
survival batch. Its physical calibration route, guidance and remaining machine fixes are the active T0 work;
**BQ** adds physical T2 fabrication. The superseded Research Terminal permission system has been
removed: its F0 cycle now produces a physical Calibrated Data Core, and old research save data is
ignored. See [`docs/BUILD-OUT.md`](docs/BUILD-OUT.md).

T0 Bootstrap is the only mechanically active tier. Its audit covers the complete survival route,
not just registration: the Multitool, Hand Crank, Crude Extractor, primitive material, chemical
and control parts, discoverability, persistence, automation and the recoverable first T1 Relay
Matrix batch. T1 stays locked until that route passes the shared gate in ADR-0108.

The Flux Exosuit is in
(ADR-0103): Assembler-manufactured once Industrial is researched. Four pieces with the
harness's protection and two module slots each. A worn Network Tap pulls up to 32 FU/t
from pylon coverage into Flux Cells on the suit; Exoskeleton Legs add 0.04 speed for
1 FU/t. The suit does not generate. The Voltaic Harness is in
(ADR-0102): four crafting-table pieces, iron protection, one Flux Cell
slot each. The cell stores 6,400 FU and does not generate. Walking
through a pylon does not charge it. Vanilla armour still equips. The Pattern Scanner and the
Deconstructor are in (ADR-0101): Assembler-manufactured once Industrial is
researched. A scan stores the item id and reports a graph cost. A smash
yields one Matter. The Replicator block is not in this slice. The Deconstruction Planner is in
(ADR-0086): Assembler-manufactured once Industrial is researched. Two corners
mark a box of at most 32 on an edge. The mark stays on the item. The tool
does not break blocks. The Multitool relocate is still the pickup. The Blueprint Tool is in
(ADR-0085): Assembler-manufactured once Industrial is researched. Two corners
capture a box of at most 32 on an edge and 512 blocks. The blueprint keeps
facing and drops status. Stamping spends the inventory, all or nothing.
Drones still wait. The Flux Drill and the Drill Cell
are in (ADR-0084): Assembler-manufactured once Industrial is researched. The
drill spends 32 FU a block and holds two cells of 3,200 FU. Sneak-use cycles
single, 3×3, vein and a horizontal tunnel. It does not break Grindless blocks.
The Multitool still does not mine. The Signal Cable, the Logic
Controller and the Redstone Interface are in (ADR-0083): Assembler-manufactured
once Industrial is researched (`1 casing + 2 motor + 4 plates`). The cable
carries one integer one block per tick and is not capped at 15. The controller
lets the machine in front run while the inventory behind holds fewer than 500
of one item, and holds it at 500 or above. An empty filter does not hold.
The interface turns redstone into that integer, and the other way, and does
not emit while redstone is coming in. The Pressure Pipe, the Electric Pump,
the Industrial Tank, the Fluid Manipulator, the Flux Belt and the Stack and
Filter Manipulators are in (ADR-0082): Assembler-manufactured once Industrial
is researched (`1 casing + 2 motor + 4 plates`). The pipe and the industrial
tank accept fluid up to 1200 °C and 1.0 MPa. Steam and molten metal move.
Superheated steam does not. The Clay Conduit and the Basic Tank still refuse
both. The pump does not invent water. The tank is one block. The flux belt
moves 16 items/s and spends F1 only while it carries items. The T1 tunnel
stays at 5 blocks. The Solar Array, the Boiler and the
Condenser are in (ADR-0081): Assembler-manufactured once Industrial is researched.
The panel makes 32 FU/t in daylight and nothing at night. `1 B water → 1 B steam`
in 10 s at 150 °C and 0.5 MPa, and the condenser returns that steam to water. The
turbine stays with fission. The Froth Flotation Cell and the Magnetic
Separator are in (ADR-0080): Assembler-manufactured once Industrial is researched
(`1 casing + 2 motor + 4 plates`). `20 crushed + 500 mB surfactant → 24 concentrate +
3 tailings` in 80 s. Concentrate reduces on the Arc Furnace. Ten tailings make one
ingot. Surfactant is carbon and water on the Chemical Reactor. The magnet pulls iron,
nickel and steel to the left and does not change the item. The Induction Furnace and the Caster are in
(ADR-0079): Assembler-manufactured once Industrial is researched
(`1 casing + 2 motor + 4 plates`). `1 ingot → 144 mB molten` in 8 s at 1000 °C, inert,
with no slag. `144 mB molten + mould → 1 ingot or 1 plate` in 4 s. The moulds are four
iron plates. Molten fluid is 1000 °C, so a Basic Tank refuses it. The Arc Furnace still
makes ingots. The Fluid Well is in (ADR-0078):
Assembler-manufactured once Industrial is researched (`1 casing + 2 motor + 4 plates`).
It draws F1 and pumps 100 mB/t of chunk water with no vanilla source. Brine, oil and
geothermal are not emitted. The Electrolysis Cell and the Atmospheric
Intake are in (ADR-0077): Assembler-manufactured once Industrial is researched
(`1 casing + 2 motor + 4 plates`). `2 B water → 2 B hydrogen + 1 B oxygen` in 10 s.
Hydrogen burns like CO. Oxygen recombines to water on the Chemical Reactor. The intake
stores `2 B oxygen` from free air in 10 s. Nitrogen and argon are not emitted. The
Chemical Washer is in (ADR-0076):
Assembler-manufactured once Industrial is researched (`1 casing + 2 motor + 4 plates`).
`8 crushed + 2 B water → 8 washed crushed + 1 ingot of the next eligible metal` in 20 s.
The ingot feeds that material's forming and wire lines as the named byproduct sink. Washed crushed reduces like
crushed and roasts in the Kiln.
**BO — prototype isolation is in.** The generated graph has 107 recipes and 39 Assembler rows.
Array Casing, Ground Array, Lunar Link, Starward Link, Launch Pad, Survey Rocket, Station Berth
and Supraluminal Station have neither a generated route nor a crafting-table recipe. The quest
book has eleven tasks in four lines and the field guide stops at electronic silicon, with no
dimension objective. **Arrival at
the galactic centre (BN) is in** (ADR-0099): riding the station from the Drift lands in
a sealed chamber. Leaving the chamber returns to the berth saved on the way to the Drift
and does not draw. **The supraluminal
station (BM) remains a registered prototype** (ADR-0098). A covered berth draws 6,553,600 FU,
the station climbs to the ceiling, and that ceiling is the Drift. The ride on from the
Drift does not draw. The Starward Link stays registered and no longer moves a player. **Teleportation
orbs (BK) are named and not started.** **Rocket ascent (BL) is in**
(ADR-0097) as a registered prototype. A
covered pad draws 102,400 FU, the rocket climbs to the ceiling, and the landing map
offers the home world and Luna. The return from Luna does not draw. **The Drift is in**
(ADR-0096): one deck, no vein. The hop is the station, not the link. **Luna is in**
(ADR-0095) as a prototype world. A covered link draws 102,400 FU,
then a return pad on the regolith sends the player home without a second draw. The
link stays as a placeholder. Every
Luna vein is helium-3. **The Ground Array is in**
(ADR-0094) as a registered prototype. A
complete ring adds 6,553,600 FU to the covering network and nothing otherwise.
**Zone refining is in**
(ADR-0093): the Arc Furnace runs `silicon/zone_refining`, 10 metallurgical silicon → 7
electronic silicon in 600 s at 1420 °C. **Metallurgical silicon is in**
(ADR-0092): the Arc Furnace runs `silicon/metallurgical`, 1 silica + 2 carbon → 1
metallurgical silicon + 2 B CO in 14 s at 1900 °C. **Refractory brick is in**
(ADR-0091): the Arc Furnace runs `ceramic/refractory_brick`, 1 slag → 1 refractory brick
in 20 s at 1400 °C. **Electric-arc steel is in**
(ADR-0090): the Arc Furnace runs `alloy/steel`, 10 iron ingots + 1 carbon → 10 steel
ingots in 140 s. The old Slice F and Z–BB rows remain historical; ADR-0107 reschedules their
systems into the T0–T15 campaign. The reachable quest route is in (ADR-0100), and BP is next.
See [`docs/BUILD-OUT.md`](docs/BUILD-OUT.md). The Chemical Reactor is in (ADR-0075):
Assembler-manufactured once Industrial is researched (`1 casing + 2 motor + 4 plates`).
`1 B SO₂ → 1 B SO₃` in 6 s on a vanadia pellet with held air; `1 B SO₃ + 0.2 B water → 1 B
sulfuric acid` in 4 s, water from a neighbouring tank. Pickle is the named sulfuric spend
(`1 iron ingot + 0.1 B acid → 1 iron plate`). R2 yield 1.15 still waits. The Wire Mill is in
(ADR-0074): Assembler-manufactured
once Industrial is researched (`1 casing + 2 coil + 4 plates`). `1 ingot → 2 wire` in 8 s;
`2 copper wire → 1 coil` is the T2 mill route; the Press die remains the T1 bootstrap.
Motor is `1 casing + 2 coil + 1 rod` in 10 s. Pylon MK2 still has no crafting-table recipe; its
Assembler route is a physical fabrication requirement, not a world unlock.
The Sorter is in (ADR-0072): matching sides peel
a mixed line and hold when that lane is full; unmatched items continue. Merger, Tunnel Belt
and Overflow Gate are in
(ADR-0071): three inlets join, a pair skips one to five empty blocks, overflow dumps clockwise
when the front is blocked. The Process Atlas stub is in: a handheld lists the live process graph
(family, I/O, conditions, time, FU/t). It does not
solve a line. JEI still waits. The Multitool now rotates and relocates Grindless blocks
(ADR-0069); it still does not mine. Kiln / R2 are in: a T1 Kiln roasts feed to oxide and
vents 1 B SO₂ into a tank (or atmosphere). The Arc Furnace reduces oxide + carbon to an
ingot and slag in 10 s. Yield stays 1.00 until the acid line (build-out K). Slice E spanning
is already in. Modular armour and the Arc Reactor pair are **recorded, not started**
(ADR-0067): T3 / F3, same unlock for the factory plant and the suit core, fed by a cell
line. Horizon Gates and the extra fallback worlds are **recorded, not started**; ADR-0107
places that commute infrastructure at T11, never in a mining dimension. The quest book (BC),
arrival chamber (BN), supraluminal station (BM), rocket (BL), Drift and Luna are implemented
prototypes. BO removed their premature survival path while retaining those tested systems.

The **Terrestrial Extractor** moves to slice B with belts. T0 extraction is enough to stop mining
by hand; T1 extraction is a throughput upgrade, not the missing process.

Two art questions are open and recorded in ADR-0048. Sound synthesis currently produces
*serviceable* industrial noise rather than good audio, and compatible upstream libraries are the
better answer when their notices and licence terms are recorded (ADR-0109);
and hero sprites, complex models and entity animation are known gaps with no owner.

Step 12d was an open architectural question and is now settled (ADR-0045). Forge exposes a block
entity's energy buffer through `getCapability`, which has to be overridden on the block entity —
and block entities live in `common/`, which may not import Forge classes. The answer is to attach
the capability from `forge/` with `AttachCapabilitiesEvent<BlockEntity>`, which keeps `common/`
loader-clean and puts the whole Forge-facing surface in one class. Its invalidation chain was read
in Forge's own source rather than assumed, because attaching a capability without registering an
invalidation listener compiles, works in testing, and leaks stale references in play.

Steps 8 and 9 were deliberately ordered that way — machines are specified as *capabilities* before
any recipe exists, because designing recipes first is what produces a mod with four hundred blocks
that each host one recipe (ADR-0019). With both done, the design is complete enough that every
remaining step implements something already specified rather than inventing it, which is the state
the project wanted before writing the systems code.

---

## Assets

Textures and models are normally **original work created for Grindless**, generated reproducibly
by committed scripts in `tools/`. Code, textures, sounds and other material from another project
may be copied or adapted when that project's licence permits the intended distribution. Every such
import is recorded in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) with its exact source,
version, licence, scope and required notices; no Minecraft assets are redistributed.

Generating art programmatically is a deliberate choice: it keeps the visual language consistent
across a hundred-plus blocks, makes a palette change a one-line edit rather than a week of
redrawing, and keeps provenance unambiguous.

Visual direction: clean industrial futurism. Dark metals with cyan energy accents, emissive
surfaces on anything powered, and machine faces that tell you what the machine does at a glance.

---

## Contributing

The project is pre-alpha and the architecture is still moving, but issues and discussion are
welcome — particularly:

- **Tag convention coverage.** If your favourite mod uses a tag layout Grindless would miss, that
  is a high-value bug report.
- **Balance.** Yields are tuned against Thermal and Mekanism; other packs may need adjustment.
- **Performance.** Belts and networks are the two systems where a bad design decision would show
  up as tick lag at scale.

Before opening a pull request, read [`AGENTS.md`](AGENTS.md). It is the working agreement for this
repository and it is short: commit at every meaningful checkpoint, record design decisions as ADRs
in [`docs/DECISIONS.md`](docs/DECISIONS.md), and log every change in [`CHANGELOG.md`](CHANGELOG.md).
The project is built across many short-lived sessions, so anything that is not written down in the
repository is lost.

Code, comments, documentation and commit messages are written in English.

---

## License

[MIT](LICENSE) applies to Grindless-authored material. Code, textures, sounds and other imported
third-party material keeps its own required notices and licence terms; see
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). Use it, fork it, ship it in your pack subject
to those terms.
