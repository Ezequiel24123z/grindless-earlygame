# Grindless

**Grindless** is a Minecraft mod that deletes the early-game grind without deleting the game.

Modpacks have a structural problem: the first two hours are almost always the same two hours.
Punch wood, make a pick, dig a staircase, strip-mine for iron, strip-mine again for the next tier.
The pack's actual content — the thing you installed it for — starts *after* that. Grindless
replaces those two hours with a short, interesting automation puzzle, and then gets out of the way.

It is inspired by Factorio: electric coverage areas instead of cable spaghetti, belts and
splitters, resource patches that deplete and push you outward, industry that makes noise something
notices, and a research tree that gates what you can build.

The arc runs from a hand crank and two iron in the first ten minutes, through belts, reactors and
a particle accelerator, to satellites that map your world's ore from orbit and colonies that
harvest planets you have never set foot on. Factorio ends when you launch a rocket; here that is
where it opens up.

> **Status: pre-alpha.** The design below is complete and settled. The skeleton **builds green on
> Forge 1.20.1**, registers the T0 bootstrap set and carries a working Flux energy layer — but the
> machines have no behaviour yet, so nothing here is playable.
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
**The eight systems**

- [System 1 — The Flux Network](#system-1--the-flux-network) — power without cables
- [System 2 — Resource Genesis](#system-2--resource-genesis) — chunks that own their materials
- [System 3 — Matter Replication](#system-3--matter-replication) — everything that isn't an ore
- [System 4 — Logistics and belts](#system-4--logistics-and-belts) — belts, drones, conduits, logic
- [System 5 — Fluids, pressure and phase](#system-5--fluids-pressure-and-phase) — pipes, tanks, loops
- [System 6 — Tools and equipment](#system-6--tools-and-equipment) — blueprints, drills, exosuit
- [System 7 — Resonance, defence and weapons](#system-7--resonance-defence-and-weapons)
- [System 8 — The futuristic tier](#system-8--the-futuristic-tier) — fission, fusion, accelerator
- [System 9 — Orbit and the planets](#system-9--orbit-and-the-planets) — satellites, remote colonies

**Mechanics**

- [Energy: Flux Units](#energy-flux-units)
- [Processing chain](#processing-chain)
- [Progression: the Research Terminal](#progression-the-research-terminal)
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

Grindless aims at the narrow target all of those miss: **make resource acquisition an automation
problem instead of a time-tax, starting ten minutes in, without making resources free.**

---

## Design principles

These are the rules every feature is measured against.

1. **Remove tedium, not decisions.** If a mechanic makes the player think, it stays. If it only
   makes them wait or repeat, it goes.
2. **Automation is the reward, not the grind.** The player should be laying out a base by minute
   fifteen, not mining until hour three.
3. **Resources cost something, always.** Energy, space, infrastructure, research. Nothing is free,
   ever — that is the line between this and a cheat mod.
4. **Feed the pack, don't replace it.** Yields are tuned to *supply* Thermal/Mekanism/Create
   processing, not to obsolete it. Grindless should make other mods more playable.
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

## System 1 — The Flux Network

*Factorio's electric poles, in three dimensions.*

Cable-based power is the single biggest source of early-game busywork in tech packs: running
wire, hiding wire, re-running wire after you move a machine, and debugging the one cable you
forgot to connect. Grindless deletes the category.

### Supply areas

A **Flux Pylon** projects a cubic **supply area** centred on itself. Every Grindless machine
inside that cube is powered. There are no wires between pylons and machines, no per-face
connections, and no cable loss to account for.

| Pylon | Supply area | Throughput | Link range | Tier |
| --- | --- | --- | --- | --- |
| **MK1** | 16 × 16 × 16 | 512 FU/t | 24 blocks | LV–MV |
| **MK2** | 32 × 32 × 32 | 4,096 FU/t | 48 blocks | MV–HV |
| **MK3** | 64 × 64 × 64 | 32,768 FU/t | 96 blocks | HV–IV |

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
- But a worked vein is slow, so the efficient move is always to **expand outward** — survey a new
  chunk, run a conduit, build another outpost.
- The pressure is horizontal, not vertical. You are exploring and laying out infrastructure,
  which is fun, instead of digging another 3×3 tunnel, which is not.

### Late game

The **Deep Core Drill** stops caring about local geology entirely and pulls from a weighted
planetary pool — any material in the pack, at a rate set by tier and power. It is the T3 answer
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
| **Phase Belt** | 64 items/s, items are inert to entities | HV | 24 blocks | T3 |

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
| **Belt Reader** | Emits a redstone/logic signal describing lane contents. |

The splitter's filter-plus-priority behaviour is copied faithfully from Factorio on purpose. It is
the single most expressive logistics primitive in that game, and almost every interesting belt
layout is built out of it.

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

At T5 the **Assembly Field** replaces the swarm and materialises a whole blueprint at once, for
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

At T6 the same routines run on remote colonies, where belts cannot reach at all, which is what
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
| **Phase Conduit** | high | proportional to load and to distance from ambient | T4 |
| **Singular Conduit** | effectively unlimited within a network | high | T5 |

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

At T5 the Singular Conduit genuinely can replace everything, and by then power is abundant enough
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
| **Cryo Line** | T3 | cryogenic, insulated | No boil-off while intact. |
| **Plasma Conduit** | T4 | magnetic containment | Consumes FU merely to stay intact; containment loss vents the line. |

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

At T3 the mod's central idea arrives for fluids. A **Phase Manifold** is to fluids exactly what a
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
| **Cryo Tank** | T3 | Insulated. Boils off slowly if it loses power — a real reason to care about brownouts. |
| **Containment Sphere** | T4 | Plasma and exotics. Powered containment. |

Every tank obeys the shared container contract — filters, buffer targets, configurable auto-void
and signal output. See [Containers](#containers-buffers-filters-and-voiding).

### Why fluids are not an optional side system

Two hooks make fluids load-bearing rather than decorative, and both are deliberate:

1. **Wet processing beats dry processing.** Slurry and leachate steps in the ore chain give
   materially better yields and recover byproducts that the dry line simply loses.
2. **You cannot build a circuit without acid, and you cannot build a machine without a circuit.**
   That is the [fabrication](#fabrication-the-factory-builds-the-factory) rule, and it means every
   player passes through fluids on the way to their second tier of machines.

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
| **Multitool** | T1 | Rotates and configures machines; picks them up *with their contents and settings intact*. Removes the "empty the machine before moving it" tax entirely. |
| **Flux Drill** | T2 | Powered mining tool. Area modes (1×1, 3×3, vein-mine, tunnel), silk/fortune modules, runs on a portable cell — no durability, only charge. |
| **Blueprint Tool** | T2 | Captures a region as a **blueprint** and stamps it elsewhere. Construction drones build it from real items in the network. Blueprints are saveable, nameable and shareable between worlds. |
| **Deconstruction Planner** | T2 | Marks a region for drones to tear down and return to storage. |
| **Terraformer** | T3 | Flattens, fills and shapes terrain in a marked region, consuming FU and actual blocks. |
| **Matter Pattern Slate** | T3 | Portable pattern storage for the replication system. |

**Blueprints are the most important tool in this list.** Once a player has designed a good
extractor outpost, rebuilding it forty times by hand is precisely the tedium this mod exists to
remove. Blueprints turn "I have solved this problem" into "I have solved this problem
permanently", which is exactly what makes Factorio's mid-game feel good.

### The Flux Exosuit

Modular powered armour, in the tradition of Factorio's power armour. Four pieces, each with a
grid of **equipment modules**; larger grids come with higher tiers.

| Module | Effect |
| --- | --- |
| **Flux Cell** | Onboard energy buffer. |
| **Portable Reactor** | Generates FU while worn. |
| **Network Tap** | Recharges the suit from any pylon supply area you walk through. |
| **Exoskeleton Legs** | Movement and jump. |
| **Thruster Pack** | Flight, with a real energy cost. |
| **Shield Projector** | Regenerating personal shield that absorbs damage before health. |
| **Auto-Repair Unit** | Repairs held and worn equipment from Matter. |
| **Personal Drone Bay** | A handful of construction drones that follow you and build blueprints anywhere. |
| **Night Vision** | Exactly what it says. |
| **Magnet Module** | Pulls dropped items toward you. |

The grid layout is a genuine decision — energy generation competes with shields competes with
mobility — and it scales all the way from "one cell and night vision" to a full late-game suit.

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
| **Plasma Caster** | T3 | Superheated projectile with area damage and a burn field. |
| **Railgun** | T3 | Extreme single-target damage, long charge, pierces everything in a line. |
| **Fusion Lance** | T4 | A sustained beam. The endgame answer to anything still standing. |
| **Flux Grenade** | T2 | Standard explosive, produced on a belt line. |
| **EMP Charge** | T3 | Disables Aberration abilities and drains hostile energy in an area. |
| **Singularity Charge** | T4 | Pulls entities and loose items to a point, then collapses. |

Ammunition tiers scale with the materials the pack provides, discovered through the same tag
system as everything else — so a pack with exotic late-game metals automatically gets exotic
late-game ammunition, with no compat work.

---

## System 8 — The futuristic tier

The endgame is where the mod stops being about *getting* materials and starts being about
*transforming* them. Every machine here is a multiblock, and each one is a project.

### Fission Reactor

A multiblock reactor running on fuel rods assembled from any fissile material in the pack —
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

The T4 power source. Deuterium and tritium are separated out of water by the Chemical Washer and
Centrifuge chain, then confined magnetically.

- **Ignition** costs a large burst of energy, so a fusion plant must be bootstrapped by an
  existing grid — a satisfying moment where the old base powers on the new one.
- Once burning, it produces power at a scale that trivialises every earlier generator, which is
  the correct feeling for a fusion reactor.
- Containment requires continuous power. Lose containment and the reaction simply quenches — a
  safe, restartable failure.
- Consumes essentially free fuel, so its real cost is the infrastructure to build and sustain it.

### Particle Accelerator

A large ring multiblock, and the most interesting machine in the mod.

| Mode | What it does |
| --- | --- |
| **Transmutation** | Converts one material into another. Cost is derived from the two materials' relative rarity in the loaded pack, so it is automatically balanced for *any* mod set. The universal answer to "I have twelve thousand copper and no tin." |
| **Exotic synthesis** | Produces materials that exist nowhere else: antimatter, exotic isotopes, strange matter. |
| **Matter creation** | Converts raw energy into generic Matter at a deliberately dreadful rate — E = mc² is not a good deal, and it should not be. |
| **Research** | Generates **Exotic Data Cores**, the only key to T4 research. |

Transmutation is the endgame's real payoff. It closes the loop opened on the first day: once you
have an accelerator, *any* material in the pack can become *any other*, and the only remaining
currency is energy. The player has fully converted a resource problem into a power problem, which
is the most satisfying possible end state for a mod about deleting the resource grind.

### Singularity Reactor

The final tier. An artificial micro-singularity fed with Matter, producing power at a scale where
the constraint is no longer generation but the network's ability to carry it. Unlocks the last
Pylon tier and the Fusion Lance.

### Supporting endgame machines

| Machine | Role |
| --- | --- |
| **Centrifuge** | Isotope separation, fuel enrichment, deuterium extraction. |
| **Heat Exchanger / Steam Turbine** | Converts reactor heat into FU. |
| **Cryogenic Plant** | Liquefaction and superconductor production. |
| **Quantum Assembler** | Multi-step crafting in a single block, for recipes with long ingredient chains. |
| **Matter Condenser** | Compresses Matter for storage and transport. |

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
| 1 | **Launch Pad** (multiblock) | Assembles and launches rockets. Requires exotic materials from the Particle Accelerator, so T4 industry is a hard prerequisite. |
| 2 | **Rocket** | Assembled from parts on a production line. Carries payload mass to orbit. Consumed on launch. |
| 3 | **Mass Driver** (multiblock) | The bulk answer. An electromagnetic launcher that fires cargo canisters to orbit for pure FU and no rocket. Cheap per kilogram, but useless for anything fragile or alive. |
| 4 | **Orbital Platform** | Your space station. Built from launched modules; expands into a real base. |

Rockets are for the first trip and for anything crewed. **The Mass Driver is what makes an orbital
economy viable** — once it is running, sending material up is an energy cost rather than a
manufacturing project, which converts space from an event into infrastructure.

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

**If a space mod is already installed, Grindless uses its planets.** Ad Astra, Galacticraft,
Beyond Earth and friends are detected at runtime, and Grindless layers its orbital mechanics,
chunk veins and remote-exploitation systems onto *their* dimensions rather than adding a
competing set. This is the same principle as the tag-driven material registry: integrate with the
pack, never duplicate it.

Only when no space mod is present does Grindless add its own minimal set:

| World | Character | Hazard | Signature resource |
| --- | --- | --- | --- |
| **Luna** | Airless, low gravity, close | Vacuum | Helium-3 — premium fusion fuel |
| **Tharsis** | Cold desert, thin atmosphere | Cold, dust storms | Iron-rich veins at high richness |
| **Vulcan** | Volcanic, hot, dense | Heat, ash | Heavy metals, geothermal power |
| **Kryos** | Ice moon, subsurface ocean | Extreme cold | Deuterium, cryogenic volatiles |
| **Erebus** | Rogue planet, no star, dark | Darkness, radiation | Exotic isotopes, strange matter |

Every world has its own **vein weighting pool**, so the chunk vein system from
[System 2](#system-2--resource-genesis) works identically off-world with different odds. Planets
are not reskins — they are different probability distributions over the same, fully pack-aware
material registry. A pack with exotic modded metals automatically gets them distributed across
planets by rarity, with no compat work.

Solar output scales with distance from the star, hazards demand specific life support, and local
gravity affects Mass Driver cost — so *where* you build is a genuine engineering decision.

### Three ways to exploit a world

This is the part of the design I am most confident is worth building, because it is the question
every space mod answers badly: **what do you do with a planet you cannot be standing on?**

#### 1. In person

Fly there, land, build normally. Full control, full flexibility, full risk — and you have to
actually be there, which means you are not at home doing anything else.

Requires life support appropriate to the hazard: the Flux Exosuit with the right modules, or a
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
are away. Twenty colonies across five planets cost approximately nothing in server performance.

This is deliberate and load-bearing. The usual implementation — force-loading a remote base so its
machines keep ticking — is exactly how a server dies, and it is why most mods quietly discourage
the thing they advertise. Abstract simulation means the mod can genuinely encourage interplanetary
industry instead.

Colony integrity gives the system a slow, manageable rhythm: a colony wants attention every few
hours, not every few minutes, and the attention it wants can itself be automated. That is the
correct difficulty curve for something you are supposed to have dozens of.

### Orbital logistics

| Block | Role |
| --- | --- |
| **Orbital Catcher** | Receives Mass Driver canisters and feeds them into a belt or logistics network. |
| **Drop Pod Bay** | Targeted surface delivery, anywhere you have a beacon. |
| **Landing Beacon** | Marks a drop-pod destination. |
| **Rectenna** | Receives beamed power from Solar Power Satellites. |
| **Interplanetary Router** | Routes items between ground, station and colonies using logistics satellites. |
| **Space Elevator** (multiblock, T6) | Permanent, high-throughput, energy-only ground-to-orbit link. Makes the Mass Driver obsolete and orbit feel genuinely attached to the world. |

### Why space is not just "more numbers"

Every earlier system gains a new dimension rather than being replaced:

| System | What space adds |
| --- | --- |
| **Flux Network** | Relay satellites extend it across dimensions; bandwidth becomes a second, scarcer network resource. |
| **Resource Genesis** | Survey satellites map veins remotely; every planet is a different distribution to exploit. |
| **Matter Replication** | Orbital and exotic materials feed patterns that are unobtainable on the ground. |
| **Logistics** | Mass drivers, drop pods and the space elevator turn logistics interplanetary. |
| **Tools** | The Exosuit becomes life support; Proxy Frames become a second body. |
| **Resonance** | Orbital industry emits none to the surface — relocation is a real strategic answer. |
| **Futuristic tier** | Helium-3 and deuterium from Luna and Kryos make fusion genuinely cheap. |

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

### Voltage tiers

Every machine has a voltage requirement, and every energy source a voltage.

| Tier | Name | Nominal | Typical era |
| --- | --- | --- | --- |
| **LV** | Low | 32 FU/t | T0–T1 |
| **MV** | Medium | 128 FU/t | T1–T2 |
| **HV** | High | 512 FU/t | T2 |
| **EV** | Extreme | 2,048 FU/t | T2–T3 |
| **IV** | Insane | 8,192 FU/t | T3 |

**Under-volting degrades speed smoothly rather than stalling the machine** — roughly halving
throughput per tier below requirement. Run an MV machine on LV and it works at about half speed;
run it two tiers down and it crawls. It never simply refuses to work.

This is the opposite of the usual tech-mod convention and it is intentional. Hard voltage gates
turn a power shortfall into a wall, and walls are exactly the kind of progress-stopping tedium
this mod exists to remove. A soft curve preserves all the *incentive* to upgrade while never
leaving a player stuck and confused.

(Over-volting is safe. Nothing explodes. Machines that explode when you connect the wrong cable
are a tedium generator, not a difficulty mechanic.)

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

## Progression: the Research Terminal

This is what keeps Grindless from being a creative-mode cheat. Machines are not craftable until
they are researched. You insert **Data Cores** and Flux Units into a **Research Terminal** to
unlock blueprints.

Data Cores are themselves produced by the factory — basic cores from early materials, advanced
cores from processed ones, exotic cores only from the Particle Accelerator. Research is therefore
a *production target*, not a timer, and the research tree is something you automate like anything
else. That is straight out of Factorio, and it is what makes progression feel earned.

Research is necessary but **not sufficient**. Past T1 a blueprint does not become a crafting
recipe: it becomes something an Assembler can manufacture, given the components and the power. The
two gates are deliberately different — research says *you may build this*, fabrication says *your
factory is capable of building this* — and a player has to clear both. See
[Fabrication](#fabrication-the-factory-builds-the-factory).

| Tier | Time | Theme | Unlocks |
| --- | --- | --- | --- |
| **T0 — Bootstrap** | 0–10 min | Escape velocity | Hand Crank Dynamo, Crude Extractor, Multitool. Buildable from cobblestone, wood and two iron. **This is the moment the grind dies.** |
| **T1 — Voltaic** | 10–40 min | First factory | Thermal Generator, Flux Pylon MK1, Terrestrial Extractor, Pulverizer, Arc Furnace, Conveyor Belt, Crude Manipulator, Splitter, Prospector's Scanner, Ballistic Turret, Clay Conduit, Hand Pump, Basic Tank. |
| **T2 — Industrial** | 1–3 h | Real automation | Flux Pylon MK2, Chemical Washer, Assembler, Solar and Steam generation, Pattern Scanner, Deconstructor, Flux Belt, Stack/Filter Manipulator, Sorter, Logic Controller, Flux Drill, **Blueprint Tool**, Flux Exosuit, Gauss Rifle, Laser Turret, Pressure Pipe, Electric Pump, Boiler, Condenser, Industrial Tank, Fluid Manipulator. |
| **T3 — Quantum** | 3–10 h | Post-scarcity | Flux Pylon MK3, Deep Core Drill, Replicator, Mag-Lev Belt, Drone Bay and logistics crates, Construction Drones, **Fission Reactor**, Steam Turbine, Centrifuge, Terraformer, Tesla Turret, Shield Projector, Plasma Caster, Railgun, Phase Manifold, Cryo Line, Cryo Tank. |
| **T4 — Exotic** | 10–25 h | Energy is the only currency | Phase Belt, **Fusion Reactor**, **Particle Accelerator**, Singularity Reactor, Quantum Assembler, Cryogenic Plant, Matter Condenser, Fusion Lance, Singularity Charge, Plasma Conduit, Containment Sphere. |
| **T5 — Orbital** | 25–40 h | Leaving the ground | Launch Pad, Rocket, **Orbital Platform**, Mass Driver, Orbital Catcher, Rectenna, the satellite line (Survey, Solar Power, Relay, Sentinel, Logistics), Vacuum Furnace, Zero-G Crystallizer, Orbital Cryo Plant, Orbital Assembly Bay. |
| **T6 — Interplanetary** | endgame+ | Worlds as infrastructure | Deep Survey Satellite, interplanetary transfer, **Colony Core** and its modules, **Proxy Frame** and Telepresence Terminal, Drop Pod Bay, Interplanetary Router, Space Elevator. |

The ramp is intentional. Ten minutes in, you are never hand-mining iron again. Everything after
that is optimisation, which is the part worth playing.

Three checkpoints are worth calling out, because each converts effort into permanent leverage at
exactly the moment the player has earned it:

- **T0, the Crude Extractor** — the grind dies here, ten minutes in.
- **T2, the Blueprint Tool** — arrives right when the player has just worked out a layout worth
  repeating, and makes that insight permanent.
- **T3, the Construction Drone** — the moment building stops being placement and becomes design,
  which is what lets multiblocks grow into real engineering problems.
- **T5, the Survey Satellite** — the moment finding resources stops being an activity and becomes
  a map you read.

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
| **T0–T1** | Crafting table. The bootstrap, and only the bootstrap. |
| **T2–T3** | **Assembler** — consumes a researched blueprint, fabricated components, FU and time. |
| **T4** | **Quantum Assembler** — long component chains resolved in one machine. |
| **T5–T6** | **Orbital Assembly Bay** — in orbit, because vacuum and zero gravity are prerequisites, not flavour. |

A machine above T1 has **no crafting-table recipe at all**. Not a hidden one, not a deliberately
expensive one — none exists. The only way to obtain it is to run the process, which means owning
and feeding a production line.

### Components — the intermediate economy

Machines are not built from ingots. They are built from parts, and those parts are where the real
production chain lives:

| Component | Tier | Built from | What it gates |
| --- | --- | --- | --- |
| **Machine Casing** | T1 | plates | The first thing any Assembler makes. |
| **Motor** | T2 | casing stock + copper coil | Anything that moves. |
| **Pump** | T2 | casing + motor + seals | The entire fluid tier. |
| **Circuit Board** | T2 | silicon wafer + **etching acid** | The first hard fluid gate. |
| **Integrated Circuit** | T3 | board + gold + **ultrapure water** | T3 machines and logic. |
| **Superconductor** | T3 | wire + **cryogenic coolant** | Reactors, rails, containment. |
| **Quantum Core** | T4 | IC + exotic material + **supercooled coolant** | The exotic tier. |
| **Containment Ring** | T4 | superconductor + **liquid nitrogen** | Fusion, plasma, singularity. |

Every ingredient above is resolved from tags, so "plates", "gold" and "silicon" mean whatever the
installed pack provides — the same runtime material registry everything else uses (ADR-0004). A
pack that already has a circuit will have Grindless use *its* circuit rather than registering a
rival one.

Note how the fluid dependencies are placed. Circuits need acid, machines need circuits, so every
player builds a small chemical line on the way to their second tier of machines. Fluids are not a
side system the player can skip; they are on the critical path, once, early, at a point where the
scale required is small.

### Why the rule exists

1. **It makes the factory the point.** The reward for building a production line is that it builds
   the next production line. Without this, machines are just expensive items and the factory is
   decoration.
2. **It closes the hand-craft bypass.** A player who arrives with a full inventory of a pack's
   mid-game materials would otherwise skip straight past everything Grindless is about.
3. **It gives research teeth.** A blueprint you can immediately hand-craft is a note. A blueprint
   that has to be fed into an assembly line is a production target — which is exactly what makes
   research feel earned rather than clicked through.

### The counterweight

This rule must never become the grind it exists to delete. The guard rails are deliberate:

- **Blueprints are permanent.** Research a machine once and you can build it forever.
- **Assemblers are cheap and parallelise.** Building ten of them is a throughput decision, not a
  punishment. The answer to "this is slow" is always "build another one", which is the correct
  answer in a factory game.
- **The bootstrap is never gated.** T0 and T1 stay hand-craftable permanently, so a player who
  loses everything can always rebuild the ladder from cobblestone and two iron. There is no
  softlock, ever.
- **Pack authors can relax it.** The gate is datapack-driven, so a pack that wants hand-craftable
  T2 machines can have them without a mod patch.

---

## Containers: buffers, filters and voiding

Every container in Grindless — item crates, fluid tanks, and the input and output buffers built
into machines — obeys one shared contract. Learn the interface once and it is the same everywhere,
on a T1 tank and on a T6 colony module alike.

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

The full planned content set, for reference. Tier is the research tier that unlocks it.

### Power generation

| Block | Tier | Notes |
| --- | --- | --- |
| Hand Crank Dynamo | T0 | Manual. The bootstrap generator. |
| Thermal Generator | T1 | Burns any furnace fuel. |
| Solar Array | T2 | Daylight only; pairs with buffers. |
| Steam Turbine | T2 | Consumes steam from any heat source. |
| Heat Exchanger | T3 | Reactor heat → steam. |
| Fission Reactor (multiblock) | T3 | Neighbour bonus; SCRAM on overheat. |
| Fusion Reactor (multiblock) | T4 | Ignition cost; quench-safe failure. |
| Singularity Reactor (multiblock) | T4 | Final tier. |

### Distribution

| Block | Tier | Notes |
| --- | --- | --- |
| Flux Pylon MK1 / MK2 / MK3 | T1 / T2 / T3 | Supply area + drone area. |
| Flux Capacitor Bank | T2 | Network energy storage buffer. |
| Flux Transformer | T2 | Steps voltage between tiers. |
| Network Monitor | T2 | Power, deficit and drone telemetry. |

### Resource acquisition

| Block | Tier | Notes |
| --- | --- | --- |
| Crude Extractor | T0 | Slow, cheap, immediate. |
| Terrestrial Extractor | T1 | The workhorse. |
| Deep Core Drill (multiblock) | T3 | Weighted planetary pool. |

### Processing

| Block | Tier | Notes |
| --- | --- | --- |
| Arc Furnace | T1 | Smelting. |
| Pulverizer | T1 | Ore → dust. |
| Chemical Washer | T2 | Purification and byproducts. |
| Assembler | T2 | Multi-ingredient crafting. |
| Centrifuge | T3 | Isotopes, enrichment, deuterium. |
| Cryogenic Plant | T4 | Liquefaction, superconductors. |
| Quantum Assembler | T4 | Long ingredient chains in one block. |
| Particle Accelerator (multiblock) | T4 | Transmutation, exotics, research. |

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
| Cryo Line / Cryo Tank | T3 | Insulated. Boil-off when unpowered. |
| Phase Manifold | T3 | Coverage-area fluid network. Costs FU per unit moved. |
| Plasma Conduit / Containment Sphere | T4 | Powered magnetic containment. |

### Matter

| Block | Tier | Notes |
| --- | --- | --- |
| Pattern Scanner | T2 | Stores item patterns. |
| Deconstructor | T2 | Items → Matter. |
| Replicator | T3 | Matter + FU → patterned item. |
| Matter Condenser | T4 | Compressed Matter storage. |

### Logistics

| Block | Tier |
| --- | --- |
| Conveyor / Flux / Mag-Lev / Phase Belt | T1 / T2 / T3 / T4 |
| Splitter, Merger, Tunnel Belt, Sorter, Overflow Gate, Belt Reader | T1–T2 |
| Manipulator: Crude / Fast / Stack / Filter | T1–T2 |
| Signal Cable, Logic Controller, Arithmetic Unit, Redstone Interface | T2 |
| Basic / Flux / Phase / Singular Conduit | T2 / T3 / T4 / T5 |
| Conduit cores: Item, Fluid, Heat, Signal, Flux | T2–T4 |
| Conduit upgrades: Bore, Filter, Routing, Insulation, Phase | T2–T4 |
| Drone Bay, Provider / Requester / Buffer / Storage Crate | T3 |
| Operator Bay, Operator Drone | T3 |
| Construction Drone, Assembly Field (multiblock) | T3 / T5 |
| Design Terminal — blueprint validation and simulation | T3 |
| Instruction Cards (11 kinds), Routine Card, Locator | T3 |
| Drone pods: Cargo, Fluid, Thermal, Tool Arm, Sensor, Range Extender | T3–T4 |

### Research and defence

| Block | Tier |
| --- | --- |
| Research Terminal | T0 |
| Ballistic Turret, Flux Wall | T1 |
| Laser Turret | T2 |
| Tesla Turret, Shield Projector, Combat Drone Bay | T3 |

### Orbital and planetary

| Block | Tier | Notes |
| --- | --- | --- |
| Launch Pad (multiblock) | T5 | Assembles and launches rockets. |
| Mass Driver (multiblock) | T5 | Bulk cargo to orbit for pure energy. |
| Orbital Catcher | T5 | Receives Mass Driver canisters. |
| Orbital Platform modules | T5 | The space station itself. |
| Rectenna | T5 | Receives beamed satellite power. |
| Vacuum Furnace | T5 | Higher purity than any ground smelter. |
| Zero-G Crystallizer | T5 | Flawless crystal substrates. |
| Orbital Cryo Plant | T5 | Cheap liquefaction and superconductors. |
| Vacuum Deposition Chamber | T5 | Thin films, exotic alloys. |
| Orbital Assembly Bay | T5 | Builds what gravity will not allow. |
| Telepresence Terminal | T6 | Drive a Proxy Frame on another world. |
| Colony Core + modules | T6 | Abstractly simulated automated colony. |
| Drop Pod Bay, Landing Beacon | T6 | Targeted surface delivery. |
| Interplanetary Router | T6 | Item routing across dimensions. |
| Space Elevator (multiblock) | T6 | Permanent ground-to-orbit link. |

### Satellites

| Satellite | Tier | Utility |
| --- | --- | --- |
| Survey Satellite | T5 | Maps chunk veins from orbit. |
| Solar Power Satellite | T5 | Constant beamed power. |
| Relay Satellite | T5 | Cross-dimension network + bandwidth. |
| Sentinel Satellite | T5 | Global Resonance map, swarm warning. |
| Logistics Satellite | T5 | Drop-pod targeting, orbital routing. |
| Deep Survey Satellite | T6 | Surveys other planets remotely. |

### Items

| Item | Tier |
| --- | --- |
| Multitool | T0 |
| Prospector's Scanner, Flux Conduit | T1 |
| Machine Casing, Motor, Pump, Circuit Board | T1–T2 |
| Chassis Upgrade Kit MK II–MK V | T2–T5 |
| Machine upgrades: Speed, Parallel, Efficiency, Yield, Precision, Insulation, Containment, Catalyst Feed, Damping, Recovery | T2–T4 |
| Integrated Circuit, Superconductor | T3 |
| Quantum Core, Containment Ring | T4 |
| Flux Drill, Blueprint Tool, Deconstruction Planner | T2 |
| Flux Exosuit (4 pieces) + equipment modules | T2–T4 |
| Terraformer, Matter Pattern Slate | T3 |
| Data Core / Advanced Data Core / Exotic Data Core | T0 / T2 / T4 |
| Matter, Raw materials, dusts, purified dusts, plates | various |
| Ammunition: slugs, plasma cells, rail slugs | T2–T4 |
| Gauss Rifle, Arc Thrower, Flux Grenade | T2 |
| Plasma Caster, Railgun, EMP Charge | T3 |
| Fusion Lance, Singularity Charge | T4 |
| Rocket parts, cargo canisters, satellite chassis | T5 |
| Life support modules (vacuum, thermal, radiation) | T5 |
| Helium-3, deuterium, exotic isotopes, strange matter | T5–T6 |
| Proxy Frame, colony modules, orbital survey charts | T6 |

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

| Situation | Behaviour |
| --- | --- |
| Ad Astra, Galacticraft, Beyond Earth or similar installed | Grindless registers **no planets of its own**. Its satellites, chunk veins, colonies and telepresence all operate on *their* dimensions, and each detected world gets a vein weighting pool derived from its existing characteristics. |
| No space mod installed | Grindless adds its own minimal set — Luna, Tharsis, Vulcan, Kryos, Erebus. |
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
│       ├── energy/                   FU implementation, RF/EU conversion
│       ├── item/                     items, tools, weapons
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

### 0.1 — Foundation *(in progress)*

Build green on Forge. Registry layer, Flux energy API and its capability bridge, the pylon network
with supply areas and manual linking, deterministic chunk veins, the runtime tag-driven material
registry, and a minimal playable T0→T1 loop: Hand Crank Dynamo, Crude Extractor, Terrestrial
Extractor, Arc Furnace, Pulverizer, Research Terminal.

**Definition of done:** a player can go from an empty world to automated iron in under fifteen
minutes without mining it by hand.

### 0.2 — Logistics

Belts and lane data model, splitters, mergers, tunnel belts, manipulators, sorters. Signal cable
and the logic controller. This is the release where the mod starts feeling like Factorio.

### 0.3 — Industry

Chemical Washer, Assembler, the full processing chain with byproducts. Solar and steam
generation. Flux Transformer and capacitor banks. Voltage tiers fully implemented, including the
soft under-volt curve and proportional brownouts.

### 0.4 — Tools and matter

Flux Drill, Multitool, **Blueprint Tool** and construction drones, Deconstruction Planner. Pattern
Scanner, Deconstructor and Replicator with recipe-graph pricing. Flux Exosuit with its module
grid.

### 0.5 — Resonance and defence

Resonance emission, spread and decay. Aberrations. Turret line, Flux Walls, Shield Projector.
The T2–T3 weapon set and manufactured ammunition.

### 0.6 — The futuristic tier

Drone logistics and the logistics crates. Deep Core Drill. Fission Reactor multiblock with
neighbour bonuses and SCRAM. Centrifuge and the fuel cycle.

### 0.7 — Endgame

Fusion Reactor. Particle Accelerator with transmutation, exotic synthesis and Exotic Data Cores.
Singularity Reactor. Quantum Assembler, Cryogenic Plant, Matter Condenser. T4 weapons.

### 0.8 — Orbit

Launch Pad and rockets. Orbital Platform and station modules. Mass Driver and Orbital Catcher. The
satellite line, with the **Survey Satellite** as the headline feature. Rectenna and beamed power.
Vacuum and zero-g processing machines.

**Definition of done:** a satellite constellation measurably improves the ground base — veins are
mapped instead of hunted, and orbital solar carries real load.

### 0.9 — Interplanetary

Planet registry with space-mod detection, and the fallback worlds. Per-planet vein pools and
hazards. Life support. **Proxy Frames and telepresence.** **Colony Cores** with abstract
simulation, integrity and resupply. Drop pods, interplanetary routing, Space Elevator.

**Definition of done:** a player can profitably exploit a planet they have never physically
visited, and twenty remote colonies cost no measurable server performance.

### 1.0 — Polish

Full JEI/REI/EMI integration, advancements, an in-game guide, config UI, localisation, performance
passes on belts and networks, and a balance pass against the major packs.

### Beyond 1.0 — version ports

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
| 12 | Machine block entity framework: container contract, chassis marks, upgrades | **next** |
| 13 | Pylon network, supply areas, `SavedData` | pending |
| 14 | Chunk veins + runtime material registry | pending |
| 15 | The `ProcessRecipe` type + tag-driven runtime recipe generation | pending |
| 16 | T0/T1 machines, menus and screens | pending |
| 17 | Fluid layer: state, pressure, pipes, tanks | pending |
| 18 | Process Atlas + JEI/REI/EMI integration | pending |
| 19 | Original textures and models via `tools/` scripts | pending |
| 20 | Research Terminal, fabrication gate and progression | pending |
| 21 | Belts and the logistics layer | pending |
| 22 | Flux Conduits: cores, named networks, diagnostics | pending |
| 23 | Multiblock framework + the parametric multiblocks | pending |
| 24 | Orbital layer: launch, satellites, station | pending |
| 25 | Planetary layer: colonies, telepresence, planet registry | pending |
| 26 | CI workflow | pending |

Step 7 was the first real milestone and it is cleared: a skeleton that actually compiles and
packages, which means every later step is validated the moment it is written rather than
accumulating as a pile of uncompiled code.

Steps 4, 5 and 7 originally covered Fabric as well. Fabric was dropped at step 10 (ADR-0039), and
the rows above describe what the project builds now rather than what it once built.

Step 11 originally also carried the `ProcessRecipe` type. That moved to step 15, where recipe
*generation* already lives: a recipe type cannot be bound to `RecipeType` and `RecipeSerializer`
before the container contract it matches against (step 12) and the fluid stacks half its
ingredients are (step 17) exist. Writing it earlier would have meant guessing both. The condition
system itself — the part the whole machine layer rests on — is complete and standalone.

Steps 8 and 9 were deliberately ordered that way — machines are specified as *capabilities* before
any recipe exists, because designing recipes first is what produces a mod with four hundred blocks
that each host one recipe (ADR-0019). With both done, the design is complete enough that every
remaining step implements something already specified rather than inventing it, which is the state
the project wanted before writing the systems code.

---

## Assets

All textures and models in this repository are **original work created for Grindless**, generated
reproducibly by committed scripts in `tools/`. No Minecraft assets are redistributed, and no
third-party art is vendored.

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

[MIT](LICENSE). Use it, fork it, ship it in your pack.