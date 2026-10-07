# Items, fluids and the recipe graph

What actually flows through the machine layer. [`MACHINES.md`](MACHINES.md) defines machines as
*condition envelopes* and deliberately contains no recipes; this document is the other half — the
item and fluid catalogue, the shape of the recipe graph, the concrete processing routes with their
ratios, and which products are reachable more than one way.

The order was deliberate (ADR-0019): capabilities first, content second. The payoff is visible here
— roughly thirty machines host the whole graph below, because no machine encodes a recipe.

> **Precedence.** Nothing here may contradict [`MACHINES.md`](MACHINES.md). Where this document
> appeared to, the conflict was resolved in favour of `MACHINES.md` and recorded under
> [clarifications](#clarifications-to-earlier-documents). No corrective ADR against `MACHINES.md`
> was needed: every tension turned out to be an omission in this layer, not an error in that one.

> **Status.** This is a *design specification*, not an implementation. It is written to be
> executable by a later session: every ratio is concrete, every condition is named, and every
> number is a starting value to be tuned rather than a placeholder to be invented.

---

## Contents

- [How to read a process](#how-to-read-a-process)
- [Units, ratios and time](#units-ratios-and-time)
- [The item catalogue](#the-item-catalogue)
- [The fluid catalogue](#the-fluid-catalogue)
- [The recipe graph](#the-recipe-graph)
- [Tier 0 and Tier 1: the bootstrap chain](#tier-0-and-tier-1-the-bootstrap-chain)
- [Ore processing: beneficiation and reduction](#ore-processing-beneficiation-and-reduction)
- [The chemical core](#the-chemical-core)
- [Silicon, wafers and circuits](#silicon-wafers-and-circuits)
- [Materials: alloys, polymers, ceramics](#materials-alloys-polymers-ceramics)
- [Fuels, coolants and isotopes](#fuels-coolants-and-isotopes)
- [Components and machine fabrication](#components-and-machine-fabrication)
- [Products reachable by more than one route](#products-reachable-by-more-than-one-route)
- [Byproducts and their sinks](#byproducts-and-their-sinks)
- [Clarifications to earlier documents](#clarifications-to-earlier-documents)
- [Open questions](#open-questions)

---

## How to read a process

Every process in this document is written in one notation, which is the notation of
[ADR-0020](DECISIONS.md#adr-0020--recipes-are-parameterised-by-process-conditions):

```
inputs  +  [conditions]  +  time  ──>  outputs
```

Conditions use a fixed shorthand, matching the six dimensions in
[process conditions](MACHINES.md#process-conditions-the-central-mechanic):

| Shorthand | Dimension | Example |
| --- | --- | --- |
| `T` | Temperature | `T 1500` — degrees Celsius |
| `P` | Pressure | `P 20MPa`, `P vac` |
| `atm` | Atmosphere | `atm reducing`, `atm inert`, `atm O2` |
| `cat` | Catalyst | `cat vanadia` |
| `fld` | Field | `fld electric`, `fld magnetic` |
| `agi` | Agitation | `agi stirred`, `agi fluidised` |

A worked example, the carbothermic reduction of a copper concentrate:

```
2 u copper concentrate + 1 u carbon
  [T 1500 · atm reducing]  12 s
  ──> 2 u copper + 1 u slag + 1 B CO
```

### The omission rule

**An unwritten condition is not a condition.** If a process does not name a pressure, it has no
pressure requirement, and a machine does not have to be able to supply one for the process to run
in it.

This matters more than it looks. It is what keeps the matching rule in
[machines are condition envelopes](MACHINES.md#machines-are-condition-envelopes) cheap — a recipe
runs in any machine whose envelope contains the conditions the recipe *names* — and it is what
stops a six-dimensional condition space from forcing every recipe to specify six values. Most
processes name one or two. The sulfide roast names a temperature and an atmosphere and nothing
else, so it runs in a Kiln, an Arc Furnace or a Vacuum Furnace alike.

It is also the honest answer to the first
[open question](MACHINES.md#open-questions) in `MACHINES.md` — whether six condition dimensions
are too many for the UI. They are not, because no single recipe ever shows six.

### Bands, not points

A written condition is the centre of a band, not an exact value, per
[efficiency is continuous](MACHINES.md#efficiency-is-continuous-not-binary). `T 1500` means the
optimum is 1500 °C; the band around it is ±15 % unless stated otherwise. Inside the band, full
yield. At the edge, reduced yield or longer time. Outside, visible failure.

Where a process is unusually sensitive the band is written explicitly — `T 1420 ±5` for zone
refining, which is the whole point of zone refining.

---

## Units, ratios and time

Routes cannot be compared unless they are quoted on the same basis, and the Atlas's ratio solver
([ADR-0023](DECISIONS.md#adr-0023--a-native-route-viewer-with-a-ratio-solver)) cannot solve a line
whose steps use different ones. So the basis is fixed here, once.

| Symbol | Quantity | Definition |
| --- | --- | --- |
| **u** | unit | The canonical amount of a solid material. 1 u = 1 ingot = 1 dust = 1 plate = 9 nuggets = 1⁄9 block. |
| **B** | bucket | 1000 mB of fluid. |
| **u ↔ B** | — | 1 u of a material as melt = **144 mB**, the modded convention, so a Caster never needs a conversion table. |
| **s** | second | 20 ticks. |

**Gases are quoted at a reference state.** Because a fluid is a state and not a thing
([ADR-0015](DECISIONS.md#adr-0015--fluids-are-modelled-as-state-not-as-items)), "1 B of oxygen" is
meaningless on its own. Unless a process says otherwise, a gas volume is given at **20 °C and
0.1 MPa**. A compressor that takes 10 B of air to 0.6 MPa delivers the same matter in less volume,
and the recipe graph tracks the matter, not the volume.

### What time means

Every time in this document is for **MK I, no upgrades, at the process's base Flux tier**.

That baseline is chosen so the numbers stay true as the player progresses, because of how the two
scaling dials are defined elsewhere:

- A **chassis mark** widens the condition envelope and does *not* change speed
  ([ADR-0027](DECISIONS.md#adr-0027--chassis-marks-widen-the-condition-envelope)). An MK V Arc
  Furnace runs the 1500 °C reduction in exactly the 12 s an MK I does. What it buys is the 3500 °C
  processes the MK I cannot reach at all.
- **Speed** is an upgrade and a deliberately bad trade
  ([ADR-0028](DECISIONS.md#adr-0028--every-machine-upgrade-is-a-trade)), so quoting times at a
  "typical overclock" would bake in the choice the design wants the player to reject.

So throughput comes from **parallel, multiblocks and better routes**, exactly as
[scaling](MACHINES.md#scaling-parallel-overclock-multiblock) says — never from the tier number
alone. A line that needs twice the output needs twice the machines, a bigger multiblock, or a
different route. All three are decisions; a voltage number is not.

### What a ratio means

Yields are quoted **per unit of primary input**, with the primary input named. `3.1 u/u raw` means
three point one units of product per unit of raw ore entering the *front* of the route, not per
unit entering the last machine. Quoting per-stage is how mods end up with chains that look
generous and multiply out to a loss.

---

## The item catalogue

The catalogue is **a matrix, not a list**
([ADR-0032](DECISIONS.md#adr-0032--the-item-catalogue-is-a-matrix-not-a-list)). Grindless defines
*forms*; the materials come from the runtime tag scan
([ADR-0004](DECISIONS.md#adr-0004--the-material-registry-is-built-from-tags-at-runtime)), so the
catalogue's size is a property of the installed pack and the recipes that populate it are generated
rather than authored
([ADR-0005](DECISIONS.md#adr-0005--recipes-are-generated-at-runtime-not-shipped-as-json)).

### Ore-line forms

Produced and consumed by [ore processing](#ore-processing-beneficiation-and-reduction).

| Form | Produced by | Grade | Role |
| --- | --- | --- | --- |
| **raw** | Extractor | 1.00 | Straight from the chunk vein. |
| **crushed** | Pulverizer, dry | 2.00 | The standard doubling step. |
| **washed crushed** | Chemical Washer | 2.00 | Same metal, releases a byproduct. |
| **concentrate** | Froth Flotation, Magnetic or Electrostatic Separator | 2.40 | The best solid feed. |
| **purified dust** | Centrifuge | 2.55 | Concentrate with the gangue spun out. |
| **gangue** | any separation | — | The rejected fraction. Not waste — see [sinks](#byproducts-and-their-sinks). |
| **slag** | any reduction | — | The melt's rejected fraction. |
| **tailings** | Sifter, flotation | — | Low-grade residue; still contains trace metals. |

"Grade" is the multiplier this form contributes to final metal yield. It is a property of the
*form*, so it is the same for every material, which is what lets one generated recipe set cover the
whole pack.

### Metal and formed stock

| Form | Produced by | Notes |
| --- | --- | --- |
| **nugget** | Press, hand | 1⁄9 u. The smallest divisible amount. |
| **ingot** | Arc Furnace, Induction Furnace, Electrolysis Cell | 1 u. |
| **hot ingot** | Arc Furnace tapped hot | Carries heat; cools to ingot if left. |
| **block** | Press | 9 u. Storage and some bulk recipes. |
| **plate** | Press, `cat` plate die | The universal structural form. |
| **foil** | Press, `cat` foil die | Capacitors, shielding, thin films. |
| **rod** | Press, `cat` rod die | Axles, fuel rod cladding, frames. |
| **bolt** | Press from rod | Assembly fastener. |
| **gear** | Press, `cat` gear die | Anything mechanical. |
| **ring** | Press from rod | Seals, bearings. |
| **wire** | Wire Mill | Everything electrical. |
| **fine wire** | Wire Mill, MK III+ | Circuits, coils, superconductors. |
| **coil** | Wire Mill; T1 Press + coil die | Motors, induction, containment. T1 ships the Press route so the Assembler is craftable (ADR-0063). |

Dies live in the **catalyst slot** — they are not consumed, they degrade slowly, and they are the
reason one Press covers nine forms instead of nine blocks covering one each. This is the
[anatomy of a machine](MACHINES.md#anatomy-of-a-machine) doing exactly what it was designed to do.

### Compounds

A second matrix, and the one that gives chemistry its breadth: **material × anion**.

| Compound form | Written | Typical origin |
| --- | --- | --- |
| **oxide** | `<M> oxide` | Roasting in `atm O2` |
| **sulfide** | `<M> sulfide` | Native ore mineralogy |
| **chloride** | `<M> chloride` | Chlorination, chloride leach |
| **sulfate** | `<M> sulfate` | Sulfuric leach |
| **carbonate** | `<M> carbonate` | Precipitation from solution |
| **hydroxide** | `<M> hydroxide` | Alkaline precipitation |

Six anions across the pack's metals is a large recipe space from a very small amount of authored
content, and it is the mechanism behind most of the alternative routes in the
[multi-route index](#products-reachable-by-more-than-one-route): an oxide reduces differently from
a sulfide, and a sulfate is electrowon rather than reduced at all.

Not every material gets every compound. The generator emits a compound only where the route that
produces it exists, which keeps a pack's creative tab from filling with salts nobody can make.

### Catalysts

Not consumed; degrade with use; some processes require one and some are merely faster with one.

| Catalyst | Made from | Enables |
| --- | --- | --- |
| **Vanadia pellet** | vanadium oxide + ceramic support | SO₂ → SO₃, the sulfuric acid line |
| **Iron catalyst** | iron oxide + promoter | Ammonia synthesis |
| **Platinum gauze** | platinum fine wire | Ammonia → nitric acid |
| **Nickel mesh** | nickel fine wire | Hydrogenation, methanation |
| **Zeolite** | alumina + silica, hydrothermal | Catalytic cracking |
| **Ziegler–Natta** | titanium chloride + alkyl | Polymerisation |
| **Palladium membrane** | palladium foil | Hydrogen purification |

Catalysts are a deliberate second kind of progression: a process you already understand becomes
cheaper when you can finally make its catalyst, with no new machine and no new research.

### Discrete items

The things that are not a form of a material: components, dies, cards and tools. These are
enumerated in the README's
[block and item catalogue](../README.md#block-and-item-catalogue); the subset that participates in
fabrication is specified under
[components and machine fabrication](#components-and-machine-fabrication).

---

## The fluid catalogue

Fluids extend the README's
[System 5 table](../README.md#system-5--fluids-pressure-and-phase) rather than replacing it. Each
entry names the **reference state** it is quoted at, because the substance alone does not determine
behaviour ([ADR-0015](DECISIONS.md#adr-0015--fluids-are-modelled-as-state-not-as-items)).

### Utility fluids

| Fluid | Reference state | Produced by | Consumed by |
| --- | --- | --- | --- |
| **Water** | 20 °C, 0.1 MPa | Hand Pump, Fluid Well, Condenser | everything |
| **Deionised water** | 20 °C | Chemical Washer, ion-exchange resin | the ultrapure chain, electrolyte make-up |
| **Ultrapure water** | 20 °C | Distillation Tower (T3), Cryogenic Plant (T4), Vacuum Furnace (T5) | integrated circuits, crystal growth |
| **Steam** | 150 °C, 0.5 MPa | Boiler, Heat Exchanger | Steam Turbine, stripping, heating |
| **Superheated steam** | 450 °C, 6 MPa | reactor secondary loop | the same turbine, far higher output |
| **Supercritical water** | 400 °C, 25 MPa | reactor primary loop | heat transport, hydrothermal chemistry |
| **Compressed air** | 20 °C, 0.6 MPa | Compressor | Atmospheric Intake feed, agitation |

### Industrial gases

All five come from the Atmospheric Intake at T2, which is what makes the chemical core reachable
early.

| Gas | Reference state | Produced by | Consumed by |
| --- | --- | --- | --- |
| **Oxygen** | 20 °C, 0.1 MPa | Atmospheric Intake, water electrolysis | roasting, steelmaking, combustion |
| **Nitrogen** | 20 °C, 0.1 MPa | Atmospheric Intake | inert `atm`, ammonia synthesis |
| **Argon** | 20 °C, 0.1 MPa | Atmospheric Intake | inert `atm` where nitrogen reacts, arc shielding |
| **Hydrogen** | 20 °C, 0.1 MPa | chlor-alkali, electrolysis, reforming, cracker offgas | reduction, ammonia, fusion feed |
| **Chlorine** | 20 °C, 0.1 MPa | chlor-alkali | chlorination, the Siemens silicon route |

### Reagents

The acid and base economy. These are **Grindless's own registered fluids**, not tag lookups
([ADR-0033](DECISIONS.md#adr-0033--materials-come-from-tags-reagents-are-grindlesss-own)).

| Reagent | Produced by | Role |
| --- | --- | --- |
| **Sulfuric acid** | contact process from roaster SO₂ | The workhorse leach and the single most connected node in the graph. |
| **Hydrochloric acid** | hydrogen + chlorine, or Siemens offgas | Chloride leaching, pickling, silicon. |
| **Nitric acid** | Ostwald process from ammonia | Dissolving noble metals, etching, oxidiser. |
| **Sodium hydroxide** | chlor-alkali | Alkaline leach, bauxite digestion, neutralisation. |
| **Ammonia** | Haber process | Nitric acid, ammoniacal leach, refrigerant. |
| **Hydrogen peroxide** | anthraquinone loop | Oxidative leaching where acid alone stalls. |
| **Etching acid** | **formulated** in the Chemical Washer from nitric + hydrochloric + deionised water | Circuit boards. The first hard fluid gate (README). |
| **Surfactant** | from the organics line | Froth flotation. |

**Leachate is a family, not a fluid.** This is the chief source of route multiplicity in
hydrometallurgy, and it is why the Chemical Washer is one machine rather than four:

| Leachate | Lixiviant | Dissolves well | Leaves behind |
| --- | --- | --- | --- |
| **Acid leachate** | sulfuric | copper, zinc, nickel, cobalt | lead, silver, gold |
| **Chloride leachate** | hydrochloric + oxidant | lead, silver, tin, antimony | silica, barite |
| **Alkaline leachate** | sodium hydroxide | aluminium, tungsten, molybdenum | iron, most sulfides |
| **Ammoniacal leachate** | ammonia + oxygen | copper, nickel, cobalt, selectively | iron entirely |

Choosing a leachate *is* choosing which metal you are after and which you are deferring to the
residue — so a mixed ore is worked twice, with two lixiviants, rather than once with a loss. That
is route selection operating on a single machine.

### Process intermediates

| Fluid | Reference state | Produced by | Consumed by |
| --- | --- | --- | --- |
| **Ore slurry** | 20 °C, dense | Pulverizer in wet mode | flotation, washing |
| **Pregnant leach solution** | 60–90 °C | Chemical Washer | electrowinning, precipitation |
| **Raffinate** | 60 °C | electrowinning | acid regeneration — *not* a waste stream |
| **Molten metal** | 1000 °C+ | Arc Furnace tap | Caster, alloying |
| **Molten salt** | 500–800 °C | Electrolysis Bank | aluminium, reactive metals, reactor coolant |
| **Black liquor** | 90 °C | organics line | surfactant, recovered alkali |

### Organics

| Fluid | Produced by | Role |
| --- | --- | --- |
| **Crude hydrocarbon** | Fluid Well | The organics feedstock. |
| **Naphtha** | Distillation Tower, light cut | Cracker feed. |
| **Light / heavy fractions** | Distillation Tower, by plate | Fuels, lubricants, bitumen. |
| **Ethylene / propylene** | Catalytic Cracker | Monomers. |
| **Polymer resin** | Polymerizer | Plastic, insulation, composites, seals. |
| **Synthesis gas** | reforming, or late CO₂ + H₂ | The carbon-neutral organics route. |

### Cryogens and exotics

| Fluid | Reference state | Produced by | Consumed by |
| --- | --- | --- | --- |
| **Glycol coolant** | −40 °C | Chemical Reactor, chilled | machine cooling, T2–T3 process cooling |
| **Liquid nitrogen** | −196 °C | Atmospheric Intake | cryo lines, superconductor fabrication, containment |
| **Liquid oxygen** | −183 °C | Atmospheric Intake | rocket propellant, high-intensity combustion |
| **Cryogenic coolant** | −250 °C | Cryogenic Plant (T4) | fusion, quantum components |
| **Liquid helium** | −269 °C | Cryogenic Plant (T4) | singularity containment |
| **Deuterium** | pressurised gas | Centrifuge / electrolysis of heavy water | fusion fuel |
| **Tritium** | pressurised gas | lithium breeding blanket | D–T fusion |
| **Helium-3** | pressurised gas | lunar regolith | D–³He fusion |
| **Plasma** | magnetically contained | Plasma Chamber, Fusion Reactor | dissociation, exotic synthesis |

Note the coolant ladder carefully: **liquid nitrogen is available at T2**, from the Atmospheric
Intake, because air separation is itself a cryogenic process. That is what lets a T3 Superconductor
be cooled without a T4 Cryogenic Plant, and it is why the tier ordering in the README's component
table holds. See [clarifications](#clarifications-to-earlier-documents).

---

## The recipe graph

The graph has eight strata. Material flows upward; reagents circulate *within* stratum S3 and feed
back down into S1 and S2, which is the structural reason a Grindless factory becomes a system
rather than a set of parallel lines.

```
 S7  EXOTICS        isotopes · exotic matter · stellar materials · antimatter
      ▲
 S6  MACHINES       Assembler · Quantum Assembler · Orbital Assembly Bay
      ▲
 S5  COMPONENTS     casing · motor · pump · board · IC · superconductor · quantum core
      ▲
 S4  MATERIALS      alloys · polymers · ceramics · composites · wafers
      ▲
 S2  REDUCTION      metal from concentrate ───────┐
      ▲                                           │
 S1  BENEFICIATION  raw ──> crushed ──> concentrate│
      ▲                                           │
 S0  GEOLOGY        chunk vein · air · water · crude hydrocarbon
                                                  │
      ┌───────────────────────────────────────────┘
      │  byproducts (SO2, Cl2, offgas, raffinate)
      ▼
 S3  CHEMICALS      acids · bases · industrial gases · organics
      │
      └──> reagents returned to S1 (leaching) and S2 (electrowinning)
```

Three properties of this shape are design decisions, not accidents:

**S3 is a cycle, everything else is a DAG.** Acid is made from a byproduct of S1, spent in S1, and
regenerated from the spent stream. A player who builds the loop pays once; a player who does not
pays continuously. Neither is blocked, which is the
[pay in layout or pay in power](MACHINES.md#routes-many-ways-to-the-same-output) principle applied
to chemistry.

**S3 hangs off the side rather than sitting in the stack.** Chemistry is not a tier you pass
through; it is a utility the whole factory draws on. This is why the Chemical Reactor is T2 and not
T4 — the first circuit board needs acid, so every player builds a small chemical line early, at a
scale where it is a pleasant puzzle rather than a project.

**Every upward edge has at least two implementations.** That is the
[route rule](MACHINES.md#routes-many-ways-to-the-same-output) stated as a property of the graph,
and it is audited in the [multi-route index](#products-reachable-by-more-than-one-route).

---

## Tier 0 and Tier 1: the bootstrap chain

The only part of the graph that is hand-crafted. Slice D of the
[implementation plan](../README.md#implementation-plan) ships Press, Machine Casing and
Assembler ([ADR-0063](DECISIONS.md#adr-0063--the-factory-builds-the-factory-at-t1)). Everything past the
Assembler is manufactured ([ADR-0017](DECISIONS.md#adr-0017--machines-above-t1-are-manufactured-never-hand-crafted)).

The bootstrap has exactly one job: **get the player to their first Assembler and then get out of
the way.** It is short on purpose. A long hand-crafted opening is the grind this mod exists to
delete.

```
Hand Crank Dynamo ──> 8 FU/t  (F0)
                        │
Crude Extractor  [F0]  20 s  ──> 1 u raw <chunk material>
                        │
                        ▼
Kiln             [T 400]        8 s   ──> dried / calcined feed
Pulverizer       [F1]           6 s   ──> 2 u crushed
Arc Furnace      [T 1500 · atm reducing · +1 u carbon]  12 s  ──> 1 u ingot per grade
Press            [cat plate die]   4 s  ──> 1 u plate
                        │
                        ▼
              4 u plate + 2 u rod  ──>  1 Machine Casing   (crafting table, T1)
              1 Machine Casing + 1 u coil + 2 u gear  ──>  Assembler  (crafting table, T1)
                        │
                        ▼
                 the crafting table is finished
```

That last line is the point. The Assembler is the **last machine with a crafting-table recipe**;
from there the factory builds the factory. The bootstrap is six machines and perhaps twenty minutes,
against GregTech's several hours of hand-crafting, and nothing in it is repeated once passed.

**T0 power is a hand crank and that is deliberate.** 8 FU/t is enough for the Crude Extractor and
nothing else, so the first real decision a player makes is what to power — which is the whole game
in miniature.

---

## Ore processing: beneficiation and reduction

The largest single part of the graph, and the one the README's
[processing chain](../README.md#processing-chain) sketches. That sketch is the dry spine; this is
the full specification.

### Routes compose

A route is not a monolith. It is a **beneficiation stage followed by a reduction stage**
([ADR-0035](DECISIONS.md#adr-0035--routes-compose-from-a-beneficiation-stage-and-a-reduction-stage)),
and the two are chosen independently. Final yield is the product of the two multipliers:

```
yield (u metal per u raw)  =  feed grade  ×  reduction factor
```

Five beneficiation options and four reduction options give **twenty routes** from nine authored
process families. That is where the depth comes from, and it costs almost nothing in content.

### Beneficiation (S1)

Per 1 u raw ore in:

| # | Stage | Machine | Conditions | Time | Out | Grade |
| --- | --- | --- | --- | --- | --- | --- |
| B0 | **none** | — | — | — | 1 u raw | **1.00** |
| B1 | **dry mill** | Pulverizer | — | 6 s | 2 u crushed | **2.00** |
| B2 | **wash** | Pulverizer → Chemical Washer | `agi stirred` + 0.5 B water | 6 + 5 s | 2 u washed crushed + **0.25 u byproduct** | **2.00** |
| B3 | **flotation** | Pulverizer wet → Froth Flotation Cell | `agi stirred` + 0.05 B surfactant | 6 + 8 s | 2.4 u concentrate + 0.3 u tailings | **2.40** |
| B4 | **centrifuge** | …→ Centrifuge | `fld` n/a, high g | +10 s | 2.55 u purified dust + 0.4 u gangue | **2.55** |

B1 is the familiar doubling and is available at T1. B2 adds a *byproduct* rather than more metal —
the secondary material paired with the vein's primary material from the runtime tag scan — which is
exactly what the README's chain shows and is usually worth more than the extra metal would have
been. The executable B2 recipe batches four quoted units
(`8 crushed + 2 B water → 8 washed crushed + 1 secondary ingot` in 20 s) so the 0.25 u ratio stays
exact with integer item counts (ADR-0076). B3 and B4 are T2 and T3 and cost reagents and power.

The **Magnetic Separator** and **Electrostatic Separator** are not a row here because they do not
raise grade; they *split a mixed stream* into two single-material streams, each of which then
re-enters at its own grade. On a single-material vein they do nothing, which is correct.

### Reduction (S2)

Per unit of graded feed:

| # | Route | Machine | Conditions | Time | Factor | Byproducts |
| --- | --- | --- | --- | --- | --- | --- |
| R1 | **Carbothermic** | Arc Furnace | `T 1500 · atm reducing` + 1 u carbon | 12 s | **1.00** | slag, 1 B CO |
| R2 | **Roast then reduce** | Kiln → Arc Furnace | `T 700 · atm O2`, then `T 1200 · atm reducing` | 8 + 10 s | **1.15** | **1 B SO₂**, slag |
| R3 | **Leach then electrowin** | Chemical Washer → Electrolysis Cell | `T 90 · agi stirred` + acid, then `T 60 · fld electric` | 14 + 20 s | **1.30** | raffinate, trace metals |
| R4 | **Plasma dissociation** | Plasma Chamber | `T 8000 · fld magnetic` | 2 s | **1.60** | elemental sulfur, elemental oxygen |

These are precisely the four routes in the
[worked example](MACHINES.md#process-conditions-the-central-mechanic) in `MACHINES.md`, given
numbers. Their costs differ in *kind*, which is the requirement:

| Route | Tier | Power | Infrastructure | Why you would pick it |
| --- | --- | --- | --- | --- |
| R1 | T1 | low | one machine | It is the only thing you can build. It never stops being cheap. |
| R2 | T1 | low | two machines + gas feed | You want the SO₂. The acid line starts here. |
| R3 | T3 | **high** | acid line + cell | You are material-limited and power-rich. |
| R4 | T5 | **absurd** | plasma containment | Power has stopped being scarce; you want the sulfur elemental. |

### The composed table

Metal per unit of raw ore, for the combinations worth naming:

| Beneficiation × Reduction | Yield | Era | Note |
| --- | --- | --- | --- |
| B0 × R1 | **1.00** | T1 | The README's "1× ingot". The floor, and it never breaks. |
| B1 × R1 | **2.00** | T1 | The README's "2× ingot". The standard doubling. |
| B2 × R1 | **2.00** + byproduct | T2 | The README's "2× + byproduct". |
| B3 × R1 | **2.40** | T2 | Flotation pays for itself immediately. |
| B2 × R2 | **2.30** + SO₂ + byproduct | T2 | The acid bootstrap. Most players' second line. |
| B3 × R2 | **2.76** + SO₂ | T3 | |
| B3 × R3 | **3.12** | T3 | The hydrometallurgical plateau. |
| B4 × R3 | **3.32** | T4 | |
| B4 × R4 | **4.08** | T5 | The practical ceiling. |

The ladder runs **1× to roughly 4×**, which sits inside the Thermal-to-Mekanism band the README
commits to — Mekanism's own ladder tops out at 5×. Grindless reaches a little less, and charges for
the top of the range in fusion-era power rather than in a sequence of five machines each of which
must be built per material.

Above that, the **Particle Accelerator** short-circuits the diagram entirely by transmuting one
element into another, at which point yield per ore stops being a meaningful question. That is the
endgame's job and it is priced accordingly.

### The wet line is an opt-in, not a replacement

Every stage from the Pulverizer onward has a fluid-assisted variant, and the dry chain keeps working
forever. This is load-bearing: a player who does not want plumbing is never blocked, only slower
per ore. Since ore comes from an
[infinite-floor chunk vein](../README.md#system-2--resource-genesis), "slower per ore" is answered
by surveying another chunk — which is the horizontal pressure the mod wants anyway.

---

## The chemical core

Stratum S3. Four closed loops, each of which turns a waste stream into a reagent the rest of the
factory needs.

### The sulfur loop

The most important chain in the mod, because it converts the single most common ore byproduct into
the single most used reagent.

```
1 u sulfide concentrate + 1.5 B oxygen
  [T 700 · atm O2]  8 s
  ──> 1 u <M> oxide + 1 B SO2                        (Kiln)

1 B SO2 + 0.5 B oxygen
  [T 450 · cat vanadia]  6 s
  ──> 1 B SO3                                        (Chemical Reactor)

1 B SO3 + 0.2 B water
  [T 120]  4 s
  ──> 1 B sulfuric acid                              (Chemical Reactor)

2 B sulfuric acid + 2 u concentrate
  [T 90 · agi stirred]  14 s
  ──> 2 B pregnant leach solution                    (Chemical Washer)

2 B pregnant leach solution
  [T 60 · fld electric]  20 s
  ──> 2.6 u metal + 1.8 B raffinate                  (Electrolysis Cell)

1.8 B raffinate + 0.2 B SO3
  [T 120]  4 s
  ──> 2 B sulfuric acid                              (Chemical Reactor)
```

The loop closes at **90 %**: of the 2 B of acid that goes in, 1.8 B comes back in the raffinate, and
the 0.2 B of make-up comes from the roaster that was going to vent its SO₂ anyway. A player with a
roaster and a reactor has effectively free acid forever. A player without one buys acid with sulfur
they have to go and find.

Note the yield check: 2 u of concentrate gives 2.6 u of metal, which is the 1.30 factor route R3
below is quoted at.

### Chlor-alkali

```
1 B brine
  [T 60 · fld electric]  10 s
  ──> 0.5 B chlorine + 0.5 B hydrogen + 1 B sodium hydroxide   (Electrolysis Cell)
```

One process, three products, all of them wanted — which makes brine electrolysis the most
*efficient* thing in the early chemical line and the reason the Electrolysis Cell is worth its T2
cost. Scaling it is a layout problem on the **Electrolysis Bank** multiblock, where electrode area
sets current and cell count sets parallelism.

Chlorine returns from the [silicon line](#silicon-wafers-and-circuits) as HCl, so the chlorine
economy is also a loop.

### Air separation

```
10 B compressed air
  [T -190 · P 0.6MPa]  10 s
  ──> 7.8 B nitrogen + 2.1 B oxygen + 0.1 B argon    (Atmospheric Intake)
```

Real atmospheric ratios, which means argon is genuinely scarce and genuinely worth the trouble when
a process needs an inert atmosphere that nitrogen would spoil. The Intake is T2, and it is the
quiet prerequisite for most of the rest of this section: oxygen for roasting, nitrogen for ammonia
and for `atm inert`, and the cryogenic cold that liquid nitrogen carries.

### Nitrogen fixation

```
1 B nitrogen + 3 B hydrogen
  [T 450 · P 20MPa · cat iron]  16 s
  ──> 2 B ammonia                                    (Autoclave)

1 B ammonia + 2 B oxygen
  [T 900 · cat platinum]  8 s
  ──> 1 B nitric acid + 1 B water                    (Chemical Reactor)
```

The Haber synthesis is the first process in the game that needs **real pressure** — 20 MPa, which
is inside the Autoclave's envelope and outside the Chemical Reactor's. That is the clearest
demonstration of the whole condition-envelope idea: the recipe is not "an Autoclave recipe", it is
a recipe whose pressure only the Autoclave can hold.

### Etching acid: synthesis versus formulation

```
0.6 B nitric acid + 0.3 B hydrochloric acid + 0.1 B deionised water
  [T 40 · agi stirred]  6 s
  ──> 1 B etching acid                               (Chemical Washer)
```

Acids are **synthesised** in the Chemical Reactor and **formulated** — mixed, diluted, buffered —
in the Chemical Washer. Etching acid is a formulation, which is why the README correctly lists the
Chemical Washer as its source while the acids inside it are made elsewhere.

### Organics

```
1 B crude hydrocarbon
  [T 350]  12 s  ──> fractions by plate              (Distillation Tower, T3)

1 B naphtha
  [T 800 · cat zeolite]  10 s
  ──> 0.5 B ethylene + 0.3 B propylene + 0.2 B offgas (Catalytic Cracker)

1 B ethylene
  [T 200 · P 2MPa · cat ziegler-natta]  14 s
  ──> 1 u polymer resin                              (Polymerizer)
```

The **Distillation Tower's height sets how many fractions you get** — three cuts at six tall,
twelve at twenty tall — so the narrow, valuable fractions only exist in a tall tower. That is a
multiblock whose *shape* changes what is possible, not how fast it is, exactly as
[the tower](MACHINES.md#distillation-tower) specifies.

---

## Silicon, wafers and circuits

The deliberate bottleneck. `MACHINES.md` names
[four routes](MACHINES.md#example-silicon-wafers-four-ways); here they are with numbers, and with
the grade distinction that makes them non-interchangeable.

**Grade is the point.** Metallurgical silicon is 99 % pure and is perfectly good for solar cells
and silicones. It is *useless* for circuits, which need six nines. So the four routes are not four
ways to the same item — two of them make a different, cheaper item that has its own uses.

```
1 u silica + 2 u carbon
  [T 1900 · atm reducing]  14 s
  ──> 1 u metallurgical silicon + 2 B CO             (Arc Furnace, T1)
```

| Route to **electronic** silicon | Process | Time | Out per 1 u met-Si | Needs |
| --- | --- | --- | --- | --- |
| **Siemens** | `1 u met-Si + 3 B HCl [T 300] ──> 1 B trichlorosilane + H₂`, then `[T 1100 · atm reducing] ──> e-Si + 3 B HCl returned` | 10 + 20 s | **0.95 u** | chlorine economy, two machines |
| **Zone refining** | `[T 1420 ±5 · atm inert]` in an Induction Furnace MK III | 60 s | **0.70 u** | one machine, a precise envelope, patience |
| **Vacuum float** | `[T 1450 · P vac]` in a Vacuum Furnace | 30 s | **0.98 u** | T5 |
| **Orbital growth** | zero-g, hard vacuum | 30 s | **1.00 u**, no dislocations | the orbital stage |

Zone refining is the interesting one: it is **worse on every axis except infrastructure**. It
needs no chemistry at all, just one machine held inside a ±5 °C band — which is a chassis-mark
requirement, since only MK III and above hold a band that tight
([ADR-0027](DECISIONS.md#adr-0027--chassis-marks-widen-the-condition-envelope)). A player with a
good furnace and no chemical line gets to circuits anyway, slowly. That is the route rule working
as intended.

```
1 u electronic silicon
  [T 1450 · atm inert · agi static]  40 s
  ──> 1 u silicon boule                              (Induction or Vacuum Furnace)

1 u silicon boule
  [cat wafer saw]  8 s
  ──> 8 u wafer      (12 u if grown in orbit)        (Press)

1 u wafer + 0.2 B etching acid + 0.05 u gold fine wire
  [T 60]  12 s
  ──> 1 u circuit die + 0.2 B spent etchant          (Lithography Unit, T3)

4 u circuit die + 1 u plate + 0.1 B polymer resin
  ──> 1 Circuit Board                                (Assembler)
```

The spent etchant is not waste: it carries dissolved gold and copper, and recovering them is the
cheapest gold source in the mid game. See [sinks](#byproducts-and-their-sinks).

---

## Materials: alloys, polymers, ceramics

### Alloys

Alloying happens in the melt, which is why **molten metal** exists as a fluid: tapping the Arc
Furnace straight into a Caster skips the ingot stage entirely, at the cost of keeping the metal
hot.

```
0.9 u molten iron + 0.1 u molten chromium
  [T 1600 · atm inert]  10 s
  ──> 1 u molten stainless                           (Arc Furnace)

1 u molten <alloy>
  [cat ingot mould]  4 s  ──> 1 u ingot              (Caster)
1 u molten <alloy>
  [cat plate mould]  4 s  ──> 1 u plate              (Caster)
```

The Caster's second line is the real payoff: molten → plate directly, skipping *ingot and Press
both*. Two steps saved per plate, forever, for the price of a hot pipe run. That is the
[pay in layout](MACHINES.md#routes-many-ways-to-the-same-output) bargain in its purest form.

Alloy compositions are resolved from tags where the pack already defines them, so Grindless does
not register a rival bronze.

### Steel, three ways

Worth spelling out because it is most packs' first alloy and most mods' most boring recipe:

| Route | Process | Yield | Trade |
| --- | --- | --- | --- |
| **Oxygen blow** | `1 u molten iron + 0.3 B oxygen [T 1650]` 10 s | 1.0 u | Needs the Atmospheric Intake. Fast and clean. |
| **Electric arc** | `1 u scrap iron + 0.1 u carbon [T 1600 · fld electric]` 14 s | 1.0 u | Eats the pack's junk iron. Power-hungry. |
| **Direct reduction** | `1 u iron oxide + 1.5 B hydrogen [T 900 · atm reducing]` 18 s | 0.95 u | No carbon at all. Slow, and the only route that works before you have coke. |

Three genuinely different shapes of cost, and the third exists specifically so that a player who
has hydrogen from chlor-alkali but no coal line is not stuck.

### Ceramics and composites

| Product | Process | Used for |
| --- | --- | --- |
| **Refractory brick** | `alumina + silica [T 1400]` 20 s | Furnace linings, reactor shielding. |
| **Technical ceramic** | `[T 1600 · P 10MPa]` in an Autoclave | Crucibles, insulators, armour. |
| **Carbon fibre** | polymer resin `[T 1200 · atm inert]` | Lightweight structure, orbital hulls. |
| **Composite plate** | carbon fibre + resin `[P 5MPa]` in a Press | The structural material of the orbital tier. |

---

## Fuels, coolants and isotopes

### The coolant ladder

| Grade | Temperature | Source | Tier | Cools |
| --- | --- | --- | --- | --- |
| **Water** | 20 °C | pump | T1 | machines, the first reactors |
| **Glycol coolant** | −40 °C | Chemical Reactor, chilled | T2 | process cooling, condensers |
| **Liquid nitrogen** | −196 °C | **Atmospheric Intake** | **T2** | superconductor fabrication, cryo lines, containment rings |
| **Cryogenic coolant** | −250 °C | Cryogenic Plant | T4 | fusion magnets, quantum cores |
| **Liquid helium** | −269 °C | Cryogenic Plant | T4 | singularity containment |

The ladder is written out because the tier ordering matters: a T3 Superconductor needs cryogenic
cooling and the Cryogenic Plant is T4, which would be a gate inversion if liquid nitrogen were not
already available at T2 from air separation. It is, so it is not. Real high-temperature
superconductors are cooled with liquid nitrogen for exactly this reason.

### Isotope separation

```
1 B uranium hexafluoride
  [high g · fld n/a]  30 s
  ──> 0.007 B enriched + 0.993 B depleted            (Centrifuge, T3)

n × Centrifuge in cascade  ──> reactor-grade at 4 %
```

Enrichment is a **cascade**, not a recipe: one pass barely moves the needle, and the player builds a
bank of centrifuges whose count sets the separative work. That is throughput-by-parallel
(the [default scaling axis](MACHINES.md#scaling-parallel-overclock-multiblock)) made visible, and
it is the correct shape — real enrichment plants are exactly this.

### Fusion fuels

| Fuel | Source | Cycle |
| --- | --- | --- |
| **Deuterium** | Centrifuge on heavy water, or electrolysis of enriched water | D–D, D–T |
| **Tritium** | lithium blanket bred in a running reactor | D–T |
| **Helium-3** | lunar regolith, via the orbital layer | D–³He, aneutronic |
| **Boron-11** | boron ore, enriched | p–¹¹B, the late trophy |

Tritium being *bred in the reactor that consumes it* is the design in miniature: the reactor's
blanket is part of its design problem, not a separate line, and a reactor that does not breed
enough tritium to sustain itself is a net loss — which makes blanket layout a real engineering
question, matching the [Fusion Reactor](MACHINES.md#fusion-reactor) brief.

---

## Components and machine fabrication

The README's
[component table](../README.md#fabrication-the-factory-builds-the-factory) gates each component on
a fluid. Those gates, with the processes behind them:

| Component | Tier | Process | Fluid gate |
| --- | --- | --- | --- |
| **Machine Casing** | T1 | `4 u plate + 2 u rod` 8 s | none — the bootstrap |
| **Motor** | T2 | `1 casing stock + 2 u coil + 1 u rod` 10 s | none |
| **Pump** | T2 | `1 casing + 1 motor + 2 u ring + 0.1 B resin` 12 s | **resin** — so the organics line gates all plumbing |
| **Circuit Board** | T2 | `4 u circuit die + 1 u plate + 0.1 B resin`, etched | **etching acid** |
| **Integrated Circuit** | T3 | `1 board + 0.2 u gold fine wire + 0.5 B ultrapure water` 16 s | **ultrapure water** |
| **Superconductor** | T3 | `1 u fine wire + 0.3 B liquid nitrogen [T -196 · atm inert]` 20 s | **liquid nitrogen** |
| **Quantum Core** | T4 | `1 IC + 1 u exotic + 0.5 B cryogenic coolant` 30 s | **cryogenic coolant** |
| **Containment Ring** | T4 | `4 u superconductor + 2 u composite + 1 B liquid nitrogen` 40 s | **liquid nitrogen** |

Each fluid gate is placed where it forces a *small* piece of infrastructure at a point where small
is affordable — and never where it would force a rebuild. The gates arrive in the order
resin → acid → ultrapure water → cryogens, which is also the order the chemical core naturally
grows, so no gate is ever a surprise.

Machines themselves are assembled from these:

```
1 Machine Casing + 2 Motor + 1 Circuit Board + 4 u plate
  [+ researched blueprint]  20 s
  ──> 1 <T2 machine>                                 (Assembler)
```

---

## Products reachable by more than one route

The audit the [route rule](MACHINES.md#routes-many-ways-to-the-same-output) demands: *any
sufficiently important product should be reachable by at least two routes that differ in what they
cost.* Every row below differs in kind, not merely in amount.

| Product | Routes | They trade along |
| --- | --- | --- |
| **Any base metal** | 20 (5 beneficiation × 4 reduction) | power ↔ yield ↔ infrastructure ↔ byproducts |
| **Electronic silicon** | 4 — Siemens, zone refining, vacuum float, orbital | infrastructure ↔ time ↔ yield |
| **Steel** | 3 — oxygen blow, electric arc, direct reduction | feedstock: oxygen, scrap, or hydrogen |
| **Sulfuric acid** | 3 — roaster SO₂, elemental sulfur burn, raffinate regeneration | whether you already roast, mine or leach |
| **Hydrogen** | 4 — chlor-alkali, water electrolysis, steam reforming, cracker offgas | power ↔ carbon feedstock ↔ coproducts |
| **Oxygen** | 3 — air separation, water electrolysis, peroxide decomposition | scale ↔ purity ↔ power |
| **Ultrapure water** | 3 — distil + deionise (T3), cryogenic (T4), vacuum (T5) | tier ↔ throughput |
| **Chlorine** | 2 — chlor-alkali, HCl recovered from Siemens | whether the silicon line exists yet |
| **Ammonia** | 2 — Haber, coke-oven liquor recovery | pressure infrastructure ↔ having a coke line |
| **Polymer resin** | 3 — naphtha cracking, syngas route, replication | feedstock ↔ power |
| **Gold** | 3 — ore, spent-etchant recovery, accelerator transmutation | whether you already make circuits |
| **Trace and rare metals** | 2+ — hydromet byproduct, accelerator transmutation | geology ↔ energy |
| **Any scanned item** | +1 universal — the Replicator | always available, always expensive |

### The Replicator is not an excuse

The **Replicator** can make anything that has been scanned, which in principle satisfies the
two-route rule for every item in the game at once. It deliberately does not count as the second
route, for one reason: its cost is derived from the recipe graph
([ADR-0010](DECISIONS.md#adr-0010--replication-cost-is-derived-from-the-recipe-graph)), so it is
never cheaper than building the line. It is a convenience and a junk sink
([System 3](../README.md#system-3--matter-replication)), not an alternative process.

A real second route has to be *sometimes better*. That is the standard applied above, and anything
that failed it was not listed.

---

## Byproducts and their sinks

**Every byproduct has at least one named sink**
([ADR-0036](DECISIONS.md#adr-0036--every-byproduct-must-have-a-named-sink)). A stream the player
can only void is not a byproduct, it is a disposal chore — and chores are
[not content](MACHINES.md#the-design-target-complexity-is-not-grind).

| Byproduct | From | Sinks |
| --- | --- | --- |
| **Slag** | any reduction | Refractory brick; aggregate for construction blocks; road/ballast fill. |
| **Gangue / tailings** | separation | Re-leached at low grade; aggregate; Deconstructor. |
| **SO₂** | roasting | Sulfuric acid — the loop that defines the mid game. |
| **CO** | carbothermic reduction | Burned for heat; reductant for direct reduction; syngas feed. |
| **CO₂** | combustion, calcining | Syngas with hydrogen (late); carbonate precipitation; inert `atm`. |
| **Raffinate** | electrowinning | Acid regeneration; 90 % of the acid returns. |
| **Spent etchant** | lithography | Gold and copper recovery — the cheapest mid-game gold. |
| **Cracker offgas** | catalytic cracking | Fuel; hydrogen recovery over a palladium membrane. |
| **Depleted uranium** | enrichment | Dense plating; reactor reflector; breeder feed. |
| **Elemental sulfur** | plasma dissociation | Straight back into the acid line, skipping the roaster. |
| **Hydrogen** | chlor-alkali | Never waste — ammonia, direct reduction, or fusion feed. |
| **Black liquor** | organics | Surfactant for flotation; alkali recovery. |
| **Anything else** | anywhere | **Deconstructor → Matter**, the universal backstop. |

The Deconstructor is listed last on purpose. It guarantees nothing is ever *stuck*, but converting
a stream to generic Matter when it had a real use is leaving value on the floor — and the Atlas
will say so, because it knows what the stream was worth.

---

## Clarifications to earlier documents

Tensions found while writing this document, and how each was resolved. None required changing
`MACHINES.md`: every one was an omission in this layer rather than an error in that one.

| Tension | Resolution |
| --- | --- |
| **Ultrapure water gates the T3 Integrated Circuit, but the README lists only T4 and T5 sources.** | A T3 route is specified here: deionisation in the Chemical Washer followed by double distillation in the Distillation Tower. The T4/T5 sources remain the better ones. The README's *Source* column is the best source, not the only one. |
| **The T3 Superconductor needs cryogenic coolant, and the Cryogenic Plant is T4.** | No change needed. Liquid nitrogen is a cryogenic coolant and comes from the **T2** Atmospheric Intake, since air separation is itself cryogenic. The [coolant ladder](#fuels-coolants-and-isotopes) makes the grades explicit. |
| **The README sources etching acid from the Chemical Washer, but acid synthesis belongs in the Chemical Reactor.** | Both are right once synthesis and *formulation* are distinguished. Etching acid is a mixture; the Washer formulates it from acids the Reactor made. |
| **`MACHINES.md` gives the Distillation Tower no tier.** | Fixed at **T3** here, alongside the Autoclave, Cracker and Polymerizer it works with. An addition, not a contradiction. |
| **"Leachate" is one fluid in the README but must select between metals.** | Specified as a **family** of four lixiviants. The README entry stays true as the family's generic name. |
| **`MACHINES.md` asks whether six condition dimensions overload the UI.** | Partly answered by the [omission rule](#how-to-read-a-process): no recipe names more than three, because unwritten conditions are not requirements. The question stays open for the *machine* UI. |

---

## Open questions

Recorded so a later session knows they were considered and left open, not missed.

- **Are twenty ore routes too many to be legible?** The composition rule makes them cheap to
  *build*, but the Atlas has to present them as five plus four rather than as twenty, or the route
  picker becomes the wall of text this design exists to avoid.
- **Should grade be a continuous number on the stack rather than a form?** Continuous grade is more
  realistic and would collapse four forms into one, but it makes every stack unique, which is bad
  for belts, storage and the player's ability to predict a recipe.
- **How much of the chemical core should be mandatory?** Resin, etching acid, ultrapure water and
  liquid nitrogen are currently hard gates. Four may be one too many for a player who wants to
  reach T4 without ever enjoying chemistry.
- **Reagent loop losses are a flat 10 %.** It should probably depend on pipe tier, temperature and
  whether a Condenser is fitted — a loop you maintain well should be a loop that pays better.
- **Catalyst degradation rate is unspecified.** It has to be slow enough never to feel like a
  maintenance hatch ([ADR-0021](DECISIONS.md#adr-0021--complexity-beyond-gregtech-with-no-grind)
  forbids chores) and fast enough that catalyst production is a real line.
- **Nothing here is balanced against Flux cost yet.** Every ratio above is in items and fluids; the
  FU cost per process is deferred until the Flux API exists (step 10 of the
  [implementation plan](../README.md#implementation-plan)), because guessing power numbers before
  the energy layer is written is how a mod ends up with a spreadsheet nobody trusts.
