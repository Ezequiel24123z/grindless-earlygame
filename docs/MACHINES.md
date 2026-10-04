# Machines, multiblocks and processes

The machine layer of Grindless, designed before any recipe exists. This document defines *what
machines are and how they behave*; the recipe graph, the item and fluid catalogue and the concrete
processing routes are specified separately in [`PROCESSES.md`](PROCESSES.md).

That order is deliberate. Designing recipes before machines produces a mod where every recipe needs
its own machine, which is how content catalogues reach four hundred blocks and still feel shallow.
Designing machines first — as *capabilities* rather than as recipe holders — means a small number
of machines can host an enormous recipe space.

> [`PROCESSES.md`](PROCESSES.md) now exists and is the content layer built on this one. Nothing in
> it may contradict this document; where it would, this document is wrong and must be changed
> explicitly. Writing it surfaced six tensions, all of which resolved as omissions in that layer
> rather than errors in this one, so nothing here needed correcting — see its
> [clarifications](PROCESSES.md#clarifications-to-earlier-documents).

> **Scope.** The target is a mod **deeper than GregTech**, with an endgame well past GregTech's or
> Mekanism's, that nevertheless contains **no grind**. Those are not in tension once complexity and
> repetition are separated, which is what the first section is about.

---

## Contents

- [The design target: complexity is not grind](#the-design-target-complexity-is-not-grind)
- [Process conditions: the central mechanic](#process-conditions-the-central-mechanic)
- [Machines are condition envelopes](#machines-are-condition-envelopes)
- [Anatomy of a machine](#anatomy-of-a-machine)
- [Chassis marks and upgrades](#chassis-marks-and-upgrades)
- [The Flux tier ladder](#the-flux-tier-ladder)
- [Scaling: parallel, overclock, multiblock](#scaling-parallel-overclock-multiblock)
- [Single-block machines](#single-block-machines)
- [Multiblocks: shape is a parameter](#multiblocks-shape-is-a-parameter)
- [Routes: many ways to the same output](#routes-many-ways-to-the-same-output)
- [The endgame tier](#the-endgame-tier)
- [The route viewer](#the-route-viewer)
- [Open questions](#open-questions)

---

## The design target: complexity is not grind

These two words are used interchangeably in modpack discussion and they are opposites.

**Complexity** is the number of meaningful decisions available and the degree to which they
interact. **Grind** is being asked to repeat an action whose outcome you already know.

GregTech is the benchmark for depth, and it is genuinely deep. But it charges for that depth in
repetition: hand-crafting hundreds of intermediates, rebuilding an ore line per material,
re-tiering the same machine nine times, maintaining hatches. Much of the time a GregTech player
spends is not spent deciding anything.

Grindless takes the depth and refuses the repetition.

| GregTech charges you in | Grindless charges you in |
| --- | --- |
| Hand-crafting hundreds of intermediates | Designing a line that produces them |
| Rebuilding ore processing per material | One tag-driven line that covers every material at once |
| Re-tiering the same machine nine times | Upgrading in place, or switching to a different route |
| Maintenance hatches and tool durability | Nothing. Chores are not content. |
| Waiting for a slow machine | Scaling a fast one |

The operating rule:

> **If the player has already solved a problem, never ask them to solve it again by hand.**
> Asking them to solve it *at a different scale* is fair — that is a new problem.

Everything below follows from that. Depth comes from the recipe graph, from condition tuning, from
route selection and from ratio balancing. None of those are repetitive, and all of them get *more*
interesting the larger your factory gets.

---

## Process conditions: the central mechanic

A Grindless recipe is not `inputs → outputs`. It is:

```
inputs + conditions + time ──> outputs
```

**Conditions** are physical parameters, not recipe ids:

| Condition | Range | Supplied by |
| --- | --- | --- |
| **Temperature** | −270 °C to 10⁸ °C | heaters, burners, arc, induction, cryo, plasma |
| **Pressure** | vacuum to 300 MPa | pumps, compressors, vacuum pumps, orbit |
| **Atmosphere** | air, inert, reducing, oxidising, vacuum | gas feed into the chamber |
| **Catalyst** | an item or fluid, not consumed, degrades slowly | catalyst slot |
| **Field** | magnetic, electric, none | coils, electrodes |
| **Agitation** | static, stirred, fluidised | mechanical input |

The same inputs under different conditions produce **different outputs**. This is simply how
chemistry and metallurgy actually work, and it buys four things at once:

1. **Multiple routes to the same product**, which is the explicit design goal.
2. **A real optimisation space** — the cheapest condition set depends on what you have spare.
3. **A reason for the fluid system to exist** beyond being a second kind of item.
4. **Enormous recipe space from few machines**, because the machine does not encode the recipe.

### Worked example — one ore, four routes

A sulfide ore, the most common real-world case:

| Route | Conditions | Output | Trade |
| --- | --- | --- | --- |
| **Pyrometallurgy** | 1500 °C, reducing, carbon | crude metal + slag | Fast, cheap, lossy. Available at T1. |
| **Roast then reduce** | 700 °C oxidising, then 1200 °C reducing | metal + **SO₂** → sulfuric acid | Better yield, and the "waste" feeds your acid line. |
| **Hydrometallurgy** | 90 °C, acid leach, stirred | metal sulfate solution → electrowin | Highest yield, slowest, needs fluid and power infrastructure. |
| **Plasma dissociation** | 8000 °C, magnetic containment | elemental metal + elemental sulfur | Instant, perfectly clean, absurd power cost. |

Every route is correct under some circumstance. Early, you pyro-smelt because it is the only thing
you can build. Later you roast because sulfuric acid becomes something you *want*. Later still you
leach because you are power-limited and material-hungry. At the end you dissociate because power
has stopped being scarce. **Nothing is ever obsoleted** — the cheap route stays cheap.

### Why this is more interesting than a recipe list

In a fixed-recipe mod, "which machine makes X" has one answer and the player looks it up once. Here
the question is "which route to X is cheapest *for me, right now*", and the answer changes as the
factory grows. That is a decision that stays alive for the whole game.

---

## Machines are condition envelopes

A machine is **not** defined by the recipes it holds. It is defined by the conditions it can
maintain. A recipe runs in any machine whose envelope contains the recipe's conditions.

| Machine | Temperature | Pressure | Notes |
| --- | --- | --- | --- |
| **Kiln** | 100–900 °C | ambient | Drying, calcining, roasting. |
| **Arc Furnace** | 1200–3500 °C | ambient | Any atmosphere; the metallurgy workhorse. |
| **Induction Furnace** | 200–2000 °C | ambient | Clean, precise, electrically efficient. |
| **Chemical Reactor** | −20–250 °C | 0.1–2 MPa | Liquid phase, stirred, catalyst slot. |
| **Autoclave** | 100–400 °C | up to 25 MPa | Where hydrothermal chemistry lives. |
| **Cryo Chamber** | −270–0 °C | any | Liquefaction, superconductors, separation by boiling point. |
| **Vacuum Chamber** | ambient–1500 °C | vacuum | No oxidation; thin films; higher purity than any air process. |
| **Plasma Chamber** | 5000–10⁵ °C | magnetic | Dissociation into elements. |
| **Accelerator** | n/a | n/a | Transmutation by particle energy, not heat. |

Three consequences, all of them good:

- **Few machines, vast recipe space.** Roughly twenty processing machines cover everything, because
  the recipe space is the *product* of machines and conditions rather than a list.
- **Machines never become obsolete.** A T1 Arc Furnace still runs every arc-furnace recipe in the
  game. It is slower and less efficient than its late-game counterpart, not invalid.
- **Discovery is real.** Pushing a known recipe to a condition you have not tried is a sensible
  thing to do, and sometimes it produces something new. The route viewer makes this legible rather
  than guesswork.

### Efficiency is continuous, not binary

Running a recipe *inside* its optimal band is full yield. Running it at the edge of the band still
works, with reduced yield, longer time or extra byproducts. Running outside the band fails
visibly — and in a way that teaches: too cold and the reaction does not start, too hot and the
product decomposes into something you can see is wrong.

This is what turns "set the temperature" into a tuning problem worth optimising, instead of a
checkbox.

---

## Anatomy of a machine

Every machine, from the T0 Crude Extractor to a T6 orbital assembler, is built from the same parts.
Learn the interface once.

| Part | Behaviour |
| --- | --- |
| **Input buffer** | Items and/or fluids. Obeys the [container contract](../README.md#containers-buffers-filters-and-voiding) in full — filters, targets, limits, per-face I/O. |
| **Output buffer** | Same contract. Auto-void available, off by default, for unwanted byproducts. |
| **Catalyst slot** | Not consumed; degrades with use; some recipes require it, some are merely faster with it. |
| **Condition controls** | Target temperature, pressure, atmosphere. Settable by hand, by logic signal, or by a Process Card. |
| **Energy buffer** | FU, sized by tier. |
| **Upgrade slots** | Count set by chassis mark. See [chassis and upgrades](#chassis-marks-and-upgrades). |
| **Process Card** | A saved condition set plus a recipe selection. Copyable, so configuring the second machine is never a repeat of configuring the first. |
| **Status output** | Running, idle, blocked, starved, out-of-band — as a logic signal and as a visible indicator. |

Two of these exist specifically to kill repetition:

**Process Cards** mean that once you have tuned a process you never tune it again — you copy the
card. Building your tenth reactor is a placement problem, not a configuration problem.

**Blocked versus starved versus out-of-band** are distinguished everywhere, because "my factory
stopped and I do not know why" is the single most common failure in complex packs, and the fix is
always better telemetry rather than less complexity.

---

## Chassis marks and upgrades

Two independent dials. The **chassis mark** is the machine's capability ceiling; **upgrades** tune
behaviour within that ceiling. Separating them is what stops the catalogue from exploding into nine
copies of every machine.

### The chassis mark

Every machine exists at marks **MK I** through **MK V**. A mark is not a different block — you
apply a **Chassis Upgrade Kit** to the machine in place, keeping its position, its contents, its
configuration and its connections. Nothing is ever rebuilt or re-piped.

| Mark | Upgrade slots | Max Flux tier | Envelope width | Research |
| --- | --- | --- | --- | --- |
| **MK I** | 1 | F1 | narrow | T1 |
| **MK II** | 2 | F3 | — | T2 |
| **MK III** | 3 | F5 | — | T3 |
| **MK IV** | 4 | F7 | — | T4 |
| **MK V** | 6 | F9 | full | T5 |

The important column is the last-but-one. **A higher mark widens the condition envelope**, and
because recipes are selected by conditions (ADR-0020), that means a mark unlocks *recipes* rather
than merely adding speed:

| Arc Furnace | Reaches | Therefore can |
| --- | --- | --- |
| MK I | 1800 °C | smelt common metals |
| MK II | 2300 °C | reduce refractory oxides |
| MK III | 2800 °C | melt tungsten-class materials |
| MK IV | 3200 °C | run carbothermic reduction of the hardest carbides |
| MK V | 3500 °C | everything the machine type is physically capable of |

This is the answer to GregTech's voltage ladder. There, a tier is the same machine with a bigger
number; here a mark changes *what is possible*, which makes upgrading a goal rather than a tax.

### Upgrades trade, they never simply improve

The failure mode to avoid is the one most upgrade systems fall into: if an upgrade is strictly
good, the optimal play is to fill every slot with it, and there is no decision — only a tax you pay
late. Mekanism's speed upgrades are the canonical example.

> **Rule: every upgrade spends one resource to buy another.** Which one you want depends on which
> resource is scarce for you right now, and that changes across the game.

| Upgrade | Buys | Spends |
| --- | --- | --- |
| **Speed** | shorter cycle time | FU per operation, superlinearly; more waste heat |
| **Parallel** | N recipes per cycle | FU linearly; buffer space; larger input bursts |
| **Efficiency** | less FU per operation | cycle time |
| **Yield** | more output, fewer losses | cycle time; needs a catalyst |
| **Precision** | tighter condition hold, so the optimal band is actually hit | constant FU upkeep |
| **Insulation** | far cheaper to *hold* a high temperature | slow thermal response, so recipe switching hurts |
| **Containment** | pressure and field beyond the chassis norm | constant FU upkeep |
| **Catalyst Feed** | catalysts replaced automatically from a buffer | an upgrade slot |
| **Damping** | much less Resonance emitted | cycle time |
| **Recovery** | captures byproducts that otherwise vent | an upgrade slot; an output buffer |
| **Deep Bore** | a much higher floor on a worked vein (extractors only) | constant FU upkeep; nothing at all on a fresh vein |

Three pairs are **mutually exclusive**, because wanting both is wanting the trade not to exist:

- **Speed ↔ Efficiency** — the whole point of each is the other's cost.
- **Speed ↔ Precision** — you cannot hold a tight condition band while rushing the cycle.
- **Insulation ↔ Parallel** — thermal mass fights throughput of mixed batches.

### Why this produces decisions

The same machine is configured differently depending on what the player is short of, and the answer
genuinely changes:

- **Power-limited, early.** Efficiency, every time. You have ore and no generation.
- **Material-limited, mid-game.** Yield plus Recovery — byproducts you were venting become inputs
  to a line you have since built.
- **Throughput-limited, with power to spare.** Parallel, and more machines.
- **Running a tight chemical process.** Precision, because being 20 °C off is the difference between
  the product and a mess.
- **Hiding from what the noise attracts.** Damping, at a real throughput cost. See System 7.

Note that **Speed is almost never the right answer**, which is deliberate and is the same stance
the mod takes on overclocking. The correct way to produce more is to build wider, not to run one
machine harder.

### Keeping it from becoming a chore

- **Upgrades are reusable.** Pulling one out returns it intact. Experimenting is free.
- **Process Cards carry the upgrade loadout** as well as the conditions, so configuring the tenth
  machine is a copy, not a repeat.
- **Low marks never stop working.** An MK I Arc Furnace runs every recipe inside its envelope
  forever. It is slower and narrower, not invalid.
- **The Atlas accounts for upgrades.** When it solves a line it tells you which loadout meets the
  target, so the decision is informed rather than guessed.

---

## The Flux tier ladder

Research tiers (T0–T6) gate *what you may build*. Flux tiers gate *how much power a machine can
accept*. They are deliberately separate axes, and the ladder is long because the user asked for
many tiers and because a long ladder gives the endgame somewhere to go.

| Flux tier | Name | Throughput | Typical era |
| --- | --- | --- | --- |
| **F0** | Manual | 8 FU/t | Hand crank. |
| **F1** | Voltaic | 32 FU/t | First real network. |
| **F2** | Industrial | 128 FU/t | Steam and solar. |
| **F3** | Arc | 512 FU/t | Arc Reactor. Serious metallurgy. |
| **F4** | Plasma | 2 048 FU/t | Fission. |
| **F5** | Quantum | 8 192 FU/t | Fusion. |
| **F6** | Singular | 32 768 FU/t | Singularity reactor. |
| **F7** | Stellar | 131 072 FU/t | Orbital solar, beamed power. |
| **F8** | Exotic | 524 288 FU/t | Interplanetary grid. |
| **F9** | Transcendent | 2 097 152 FU/t | Endgame; see [the endgame tier](#the-endgame-tier). |

A machine runs on any tier at or above its rating; feeding it more than its rating does nothing
unless it is overclocked. Transformers step between tiers, and feeding a machine a tier it is not
rated for is the one place where damage is possible — and even then it burns out a cheap, visible
component rather than deleting the block.

---

## Scaling: parallel, overclock, multiblock

GregTech scales almost entirely by overclocking, which makes voltage the only real decision.
Grindless offers three axes with genuinely different shapes.

### 1. Parallel — the default

The Parallel upgrade runs N recipes simultaneously. Power scales linearly, throughput scales
linearly, efficiency is unchanged. This is the boring, correct answer and it is always available.

### 2. Overclock — deliberately the worst option

Overclocking is not a separate mechanism; it is what the **Speed** upgrade does. Double speed costs
roughly triple power, and waste heat rises with it. It is for emergencies and for players with more
power than sense, and it is **never required** to progress.

This inverts GregTech, where overclocking is mandatory and therefore not a decision at all. Here it
is a decision precisely because it is usually the wrong one.

### 3. Multiblock — better ratios, not just more throughput

Multiblocks are where scaling becomes a design problem rather than a slider, because in a
multiblock the *ratios improve*: a larger reactor loses proportionally less heat, a taller column
separates more cuts, a bigger accelerator reaches energies a small one cannot.

That is the real reward. A multiblock is not "the same machine but faster"; it does things the
single block cannot do at all.

---

## Single-block machines

The working set. Every one of these is available across the whole tier ladder — a machine is never
replaced, only re-tiered, and re-tiering is an upgrade applied in place rather than a new block to
craft and re-pipe.

### Extraction

| Machine | First tier | Role |
| --- | --- | --- |
| **Crude Extractor** | T0 | The grind ends here. Slow, cheap, no infrastructure. |
| **Terrestrial Extractor** | T1 | The workhorse. Pulls from the chunk vein. |
| **Fluid Well** | T2 | Chunk-level fluid extraction: water, brine, oil-equivalents, geothermal. |
| **Atmospheric Intake** | T2 | Separates air into its gases. The nitrogen and oxygen source. |
| **Deep Core Drill** | T3 | Multiblock. Weighted planetary pool rather than a single chunk. |

### Comminution and separation

| Machine | First tier | Role |
| --- | --- | --- |
| **Pulverizer** | T1 | Ore to dust. Wet mode produces slurry. |
| **Sifter** | T1 | Size separation. Cheap concentration with no power beyond mechanical. |
| **Magnetic Separator** | T2 | Pulls ferromagnetics out of a mixed stream, nearly free. |
| **Froth Flotation Cell** | T2 | Concentrates sulfides with surfactant. The classic cheap upgrade. |
| **Centrifuge** | T3 | Density and isotope separation; the enrichment machine. |
| **Electrostatic Separator** | T3 | Separates by conductivity, where density fails. |

### Thermal

| Machine | First tier | Role |
| --- | --- | --- |
| **Kiln** | T1 | Drying, calcining, roasting. The cheapest heat. |
| **Arc Furnace** | T1 | 1200–3500 °C. Metallurgy. |
| **Induction Furnace** | T2 | Precise, efficient, clean. |
| **Vacuum Furnace** | T5 | No atmosphere, so no oxidation and the highest purity available. |
| **Cryogenic Plant** | T4 | The cold end: liquefaction and separation by boiling point. |

### Chemical

| Machine | First tier | Role |
| --- | --- | --- |
| **Chemical Reactor** | T2 | Stirred liquid-phase reactions with a catalyst slot. |
| **Chemical Washer** | T2 | Leaching and purification. |
| **Electrolysis Cell** | T2 | Splits compounds with current. Brine, water, alumina. |
| **Autoclave** | T3 | High-pressure hydrothermal chemistry, and crystal growth. |
| **Catalytic Cracker** | T3 | Breaks long molecules into useful short ones. |
| **Polymerizer** | T3 | The reverse. Plastics, resins, composites. |

### Forming and fabrication

| Machine | First tier | Role |
| --- | --- | --- |
| **Assembler** | T2 | Multi-ingredient fabrication. The machine that makes machines. |
| **Press** | T2 | Plates, rods, gears, casings, with a die in the catalyst slot. |
| **Caster** | T2 | Molten metal to solid shapes, skipping the ingot stage. |
| **Wire Mill** | T2 | Wire and coil, which everything electrical needs. |
| **Lithography Unit** | T3 | Wafers to circuit dies. The circuit bottleneck, deliberately. |
| **Quantum Assembler** | T4 | Resolves long component chains in one block. |

### Matter

| Machine | First tier | Role |
| --- | --- | --- |
| **Pattern Scanner** | T2 | Stores an item's pattern permanently. |
| **Deconstructor** | T2 | Any item to generic Matter. |
| **Replicator** | T3 | Matter plus FU to a stored pattern, priced from the recipe graph. |
| **Matter Condenser** | T4 | Compressed Matter storage. |

---

## Multiblocks: shape is a parameter

Most mods' multiblocks are a fixed schematic: build this exact arrangement, receive this exact
machine. You look it up once and copy it, which makes it a chore with extra steps.

Grindless multiblocks are **parametric**. You choose the dimensions and the internal arrangement,
and the behaviour follows from what you built. There is no single correct schematic — there is a
design space with real trade-offs, and understanding beats copying.

### What actually limits multiblock size

The ceiling on multiblock scale in every mod is not design, it is **placement tedium**. Nobody
ships a two-thousand-block structure because nobody will place two thousand blocks by hand. So
multiblocks stay small, the design space stays shallow, and "build this 5×5×5" ends up being a
schematic you copy once.

Placing two thousand blocks by hand is, precisely, repeating an action whose outcome you already
know — the mod's own definition of grind. So the honest fix is not to keep multiblocks small; it is
to **automate the placing and keep the designing**.

**Construction Drones exist for this.** From T3 they build a multiblock from a blueprint, drawing
materials from the logistics network, and that lifts the size ceiling entirely. What the player
spends effort on is the part worth spending it on: the *design*.

Multiblock scale therefore grows with the player's ability to build:

| Era | Typical scale | Built by |
| --- | --- | --- |
| **T2** | 3×3×3 – 5×5×5 | hand placement; small enough to be reasonable |
| **T3** | up to ~15³ | Construction Drones from a blueprint |
| **T4** | up to ~32³, hundreds of internal components | drone swarms |
| **T5–T6** | structures measured in chunks | the Assembly Field, which materialises a whole blueprint at once |

### Simulate before you build

A massive parametric multiblock is only playable if the player can evaluate a design *before*
paying for it. Trial and error at a cost of ten thousand components is not engineering, it is
punishment.

So a blueprint of a multiblock is **validated and simulated at design time**. Before a single block
is placed, the blueprint reports:

- whether the structure is legal, and exactly which part is not;
- its computed properties — output, heat generation, coolant demand, throughput, stability margin;
- its full bill of materials, and which of those you cannot currently produce;
- a warning for designs that are legal but will run at the edge of their envelope.

This is the same stance as the [route viewer](#the-route-viewer): the game does the arithmetic, the
player does the design and the building. Testing a reactor core layout becomes an afternoon of
genuine engineering rather than a savegame-backup ritual.

### Arc Reactor

T3 / F3. Direct FU, not heat into steam. The first generation multiblock that is a factory you
feed, not a trophy ([ADR-0067](DECISIONS.md#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier)).

Coil count and cell throughput set output. Hatches take **Arc Cells** and coolant (or whatever
stability fluid the slice names). If the cell line stops, the reactor starves. The worn
miniature on the Arc Exosuit burns the same cell, so the plant and the suit are one logistics
problem.

It is not the Arc Furnace. It is not fission's neighbour-bonus steam core. It is not T4 fusion.
Exact chemistry and size stay open until this slice; Slice F starts when this machine (or
another that needs hatches) is scheduled.

### Fission Reactor

The flagship design problem. You lay out the core yourself — fuel rods, control rods, coolant
channels, moderator, reflector — at any size the era supports, which by T4 means a core with
hundreds of internal positions.

- Fuel rods adjacent to other fuel rods produce a **neighbour bonus**: more output, more heat.
  Clustering is how you get power, and also how you melt.
- **Coolant channels** remove heat at a rate set by coolant choice and flow. Channel routing is the
  real puzzle: heat is generated where the fuel is, and must be carried out.
- **Moderator** blocks raise the reaction rate of neighbouring rods; **reflector** blocks on the
  boundary return neutrons that would otherwise be lost, so edges behave differently from the
  centre and the core's *shape* matters, not just its volume.
- **Control rods** throttle, globally or by bank.
- Exceed the heat limit and the core **SCRAMs**: it shuts down and needs a restart cycle. It does
  not detonate. Losing an afternoon of progress to a mistake is not depth.

The design problem — maximise output per unit of coolant while staying inside the thermal envelope,
with edge effects that reward thinking about geometry — is genuinely hard, has many good answers,
and scales up rather than out. That is the correct shape for a multiblock, and it only works at
this size because drones do the placing.

### Distillation Tower

Height determines how many fractions you separate. A six-tall tower gives three cuts; a twenty-tall
tower gives twelve, and the valuable narrow fractions only exist in a tall one. Each output plate
is tapped at its own temperature.

### Electrolysis Bank

Electrode area sets the current, cell count sets the parallelism, and electrolyte choice sets what
you can split at all. Scaling up is a layout decision rather than a tier upgrade.

### Particle Accelerator

Ring circumference determines achievable particle energy, which determines which transmutations are
reachable. A small ring makes isotopes; a large one makes exotic matter. This is the clearest case
of a multiblock whose size changes *what is possible* and not merely how fast.

### Fusion Reactor

Coil count and containment strength determine which fuel cycles will ignite:

| Cycle | Difficulty | Notes |
| --- | --- | --- |
| **D–T** | Easiest | Needs tritium breeding, produces neutrons. |
| **D–D** | Harder | No tritium supply needed. |
| **D–³He** | Harder still | Aneutronic; ³He comes from lunar regolith, tying fusion to the orbital layer. |
| **p–¹¹B** | Extreme | Fully aneutronic. The late trophy. |

Ignition has a real energy cost, so a reactor that cannot sustain itself is a net loss — which
makes "is my confinement good enough" a genuine engineering question.

### Orbital Assembly Bay

Zero gravity and hard vacuum as *process conditions*, not flavour. Some products simply cannot be
made under gravity: perfect crystals, large thin films, certain alloys that segregate when they
settle. This is what makes the orbital stage a manufacturing necessity rather than a victory lap.

---

## Routes: many ways to the same output

A design rule, stated plainly:

> **Any sufficiently important product should be reachable by at least two routes**, where the
> routes differ in what they cost rather than in how much they cost.

Not arbitrary alternatives — *meaningfully different* ones. A route that is strictly worse is not a
choice, it is a trap.

The axes a route can trade along:

| Axis | Cheap route | Expensive route |
| --- | --- | --- |
| **Power** | slow, low power | fast, power-hungry |
| **Time** | passive, hours | instant |
| **Infrastructure** | one machine | a chemical line |
| **Yield** | lossy | near-perfect |
| **Byproducts** | clean but wasteful | messy but everything is useful |
| **Attention** | fully automatic | needs balancing |

### Example: silicon wafers, four ways

| Route | Needs | Trade |
| --- | --- | --- |
| **Carbothermic** | Arc Furnace + carbon | Metallurgical grade. Fine for solar, useless for circuits. |
| **Siemens process** | chlorination, distillation, deposition | Electronic grade. Long chemical chain, excellent purity. |
| **Zone refining** | Induction Furnace with a precise gradient | Upgrades metallurgical to electronic slowly, with little infrastructure. |
| **Vacuum/zero-G growth** | orbital | Flawless, enormous boules. Needs the orbital stage. |

Four routes, four shapes of cost. A player with a big arc furnace and no chemistry takes zone
refining. A player with a chemical line takes Siemens. A player in orbit stops thinking about it.

### Byproducts make routes interact

The messier routes produce things the cleaner ones do not. Roasting sulfides gives SO₂ that becomes
sulfuric acid; chlorination gives chlorine that returns to the electrolysis line; cracking gives a
spread of fractions. So the "inefficient" route is often correct because you *want* its waste —
and that is the point at which a factory stops being a set of lines and becomes a system.

---

## The endgame tier

The brief is explicit: a bigger endgame than GregTech or Mekanism, more useful, more futuristic,
and still believable. The orbital and planetary stages in the README are that endgame's first half.
This is the second.

The organising idea: **at the end, the factory stops being made of machines and starts being made
of physics.**

| System | What it does | Why it is not just a bigger machine |
| --- | --- | --- |
| **Singularity Reactor** | Power from a contained micro-singularity, fed by matter | Mass becomes a fuel. Any matter is energy, so the junk problem inverts: waste becomes the power supply. |
| **Matter Condenser array** | Energy back into arbitrary matter | Closes the loop with the Deconstructor. The economy becomes energy-only, which is a genuine phase change in how the game plays. |
| **Transmutation Chain** | Element to element via the accelerator | Scarcity stops being geological and becomes energetic. |
| **Dyson Collector** | Orbital-ring solar at F8–F9 | The only power source that outruns a singularity, and it takes a space programme to build. |
| **Space Elevator** | Permanent ground-to-orbit link | Turns orbit from a destination into part of the base. |
| **Stellar Forge** | Processing at stellar temperatures and pressures | The only way to make the final materials; necessarily orbital. |
| **Causal Buffer** | Stores *process state*, not items | Pause a running process and resume it elsewhere. Lets huge batch processes migrate between planets. |
| **Planetary Engine** | Moves a colony's orbit over very long timescales | Changes insolation and temperature, and therefore which processes run there. Infrastructure as geoengineering. |

Two rules keep this from collapsing into "you win":

1. **Nothing removes the need to build.** The Matter Condenser can make anything, but it needs
   energy proportional to the recipe graph, so building a bigger factory is still the way to get
   more things. Post-scarcity in *materials* is not post-scarcity in *throughput*.
2. **Every endgame system has a real use.** Nothing here is a trophy block. If the player cannot
   name what a thing does for their factory, it does not ship.

---

## The route viewer

A mod with parameterised recipes and multiple routes is unplayable without a way to see the graph.
This is a first-class feature, not an integration afterthought.

### Native, always present

The **Process Atlas** ships in the mod and works with no other mod installed:

- Pick any item or fluid and see **every route that produces it**, as a graph rather than a list.
- Each route shows **ratios** — inputs per output, per second, at a chosen tier — so lines can be
  balanced without a spreadsheet.
- Each route shows **conditions**, and therefore which machines can run it.
- **Cost overlay**: FU per unit, machine count, byproducts, and what the route needs that you do
  not currently produce.
- **Reachability**: routes you can build now are distinguished from routes needing research or
  infrastructure you lack, with the missing piece named.
- Pick a target rate — "120 plates/min" — and the Atlas **solves the line**: how many of each
  machine, at which tier, with which ratios.

That last one is the feature that most directly removes grind. The tedious part of a complex pack
is not deciding what to build, it is arithmetic. Let the mod do the arithmetic; the player still
has to build it, place it and feed it.

### JEI, REI and EMI

All three get full integration: recipes appear with their conditions, catalysts are listed
correctly, and transfer works. Where a viewer supports it, the Atlas is reachable from a recipe
screen so the two are not separate worlds.

The native Atlas exists anyway because the ratio solver and route comparison need a surface those
viewers do not provide, and because a pack author who ships no recipe viewer must not end up with
an unplayable mod.

T1 ships the lookup stub only (ADR-0066): a handheld list of the live graph. The solver, cost
overlay and JEI integration stay later.

---

## Open questions

Deliberately unresolved, recorded so a later session does not assume they were overlooked.

- **How many condition dimensions can the UI carry** before setting up a machine becomes work?
  Temperature and pressure are clearly fine. Atmosphere and catalyst are probably fine. Field and
  agitation may be one dimension too many, and may be better as machine properties than as player
  controls. Partly answered by the
  [omission rule](PROCESSES.md#how-to-read-a-process): an unwritten condition is not a requirement,
  and in practice no recipe names more than three. The question remains open for the machine UI,
  which must still expose the dimensions a machine *can* control.
- **Does the Atlas's line solver trivialise the game?** The position taken here is that arithmetic
  is not gameplay, but it should be watched: if "press solve, then build exactly that" becomes the
  whole loop, the solver should propose rather than prescribe.
- **What is an Arc Cell made of?** ADR-0067 names the fuel and forbids furnace fuel, fissile rods
  and D–T. The recipe is not chosen yet; it has to be a line the T3 factory already wants to
  build, not a unique dead-end reagent.
- **How is condition tuning taught?** Out-of-band failure has to be legible from the first
  machine, not discovered by reading this document.
- **Exact tier ratios.** The 4× step per Flux tier is inherited from convention and has not been
  validated against the processing chain's real power demands.
- **Where the orbital line crosses GregTech's endgame.** Both exist in the same pack in some
  setups, and Grindless should complement rather than duplicate.
