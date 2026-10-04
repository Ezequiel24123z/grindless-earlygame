# Autonomous build-out

This file keeps the history of the slices already defined, and names the next calendar.

The README is still the design source of truth ([ADR-0013](DECISIONS.md#adr-0013--the-readme-is-the-design-source-of-truth)).
[ADR-0088](DECISIONS.md#adr-0088--grindless-is-a-modpack-scale-progression) retires the six-tier
cap and the early-game convenience framing. Anything not yet started below is **held**. The
next calendar is the **modpack expansion**. The quest book is last, not first
([ADR-0090](DECISIONS.md#adr-0090--electric-arc-steel-is-the-first-line-past-contact)).

**Next slice: BK — Arrival at the galactic centre.** Not implemented. BJ (the Drift) is done.
K remains the last foundation slice. L–BB are held. The quest book is not next.

---

## Standing rules

Do not stop to ask. If a number, name or recipe already exists in the README, `MACHINES.md` or
`PROCESSES.md`, use it. If it does not, write an ADR with the cheapest default that preserves
the records below, then implement.

### Product

- Playable slices, not system layers ([ADR-0058](DECISIONS.md#adr-0058--build-playable-slices-not-system-layers)).
  A slice ships when a player can do a new thing in-world.
- One slice per stacked pull request. Do not batch two slices because they "feel small".
- Slice F (multiblock kernel) starts when a machine needs hatches or size. That machine is the
  **Arc Reactor** at T3 ([ADR-0067](DECISIONS.md#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier)).
  Do not start F in T1.
- T2+ machines are manufactured, never hand-crafted ([ADR-0017](DECISIONS.md#adr-0017--machines-above-t1-are-manufactured-never-hand-crafted)).
- Every byproduct has a named sink ([ADR-0036](DECISIONS.md#adr-0036--every-byproduct-must-have-a-named-sink)).
- Yield never drops because a condition is off-band ([ADR-0040](DECISIONS.md#adr-0040--running-out-of-band-costs-time-never-yield)).
- Generated art from `tools/` ([ADR-0048](DECISIONS.md#adr-0048--assets-are-generated-from-the-jdk-with-a-named-list-of-what-cannot-be)).
  Do not stop for hero sprites.
- English in the repository. Spanish in chat.

### Git and GitHub

- Branch `cursor/<descriptive-name>-e61d`, lowercase, from the current tip of the stack.
- Stacked drafts. Do not squash. Do not dump onto empty `main`. Do not merge onto
  `ezequiel24123z-improved-winner`.
- Do not merge pull requests unless the owner explicitly asks. The agent cannot `gh pr merge`.
- Commit trailer: `Co-authored-by: Ezequiel Castaño <ezeycema@gmail.com>` (not Copilot).
- Commit at every coherent checkpoint. Git is the only memory.

### Done-when for every slice

1. ADR if a decision was open, written *before* the code that depends on it.
2. Behaviour check in `tools/checks` with no Minecraft where the policy is arithmetic.
3. `./gradlew :common:compileJava` then `tools/run-checks.sh .`
4. `./gradlew build`
5. Smoke in `tools/smoke/` and a CI job when the slice adds a block, a menu, or a boot-visible
   item. Handshake after `Done` (`SMOKE-READY`) before scenario commands.
6. `CHANGELOG.md`, README implementation plan / *Where the project is*, this file's status column.
7. Draft stacked PR. Subscribe CI. Continue the next slice; do not wait for GitHub unless the
   **tip** is a real red (not the FIFO flake). Parent drafts may stay red.

### Do not start these until their row

| Temptation | Waits for | Record |
| --- | --- | --- |
| Multiblock kernel, hatches, formed structures | AD then AE (Arc Reactor) | ADR-0058, ADR-0067 |
| Sifter as an empty shell | An ADR that places it in the B-matrix or as a non-grade split | `PROCESSES.md` has no Sifter B-row |
| Ballistic Turret, Flux Wall, Aberrations | Z then AA (Resonance) | README 0.5 |
| Voltaic Harness / any armour chassis | X (armour line) | ADR-0067: not a T1 factory hole |
| Worn portable reactor with no cell recipe | AE | ADR-0067 |
| Washer, etching, T2 cores | L onward, and L is held | ADR-0064, ADR-0065, ADR-0074, ADR-0075, ADR-0088 |
| JEI / REI / EMI | AY or AZ, both held | ADR-0066, ADR-0088 |
| Horizon Gate, planets, mining dims | AT then AW, both held; original planets are the modpack expansion, not a mining dim | ADR-0068, ADR-0088 |
| Resume any held row L–BB | A later record that un-holds that row | ADR-0088 |
| Copy code whose license is unconfirmed or All Rights Reserved | Never | ADR-0088, ADR-0089 |
| Copy LGPL or MIT code without its notice and license text | The slice that adds those terms | ADR-0089 |
| Multitool mining | Never, without a new ADR | ADR-0055, ADR-0069 |
| Arc Furnace as a generator | Never | ADR-0067 |

---

## Already shipped

T0 loop, Slice A–E, Kiln/R2, Atlas stub, Multitool wrench, belt junctions (G), the sorter
(H), the T2 gate (I), Wire Mill / motor (J), the contact process (K), electric-arc steel
(BD), refractory brick (BE), and the design records for armour (ADR-0067)
and Horizon Gates (ADR-0068). Roadmap 0.1 definition of done — empty world to automated
iron without hand-mining — is met. 0.2 logistics leftovers from ADR-0060 (merger, tunnel,
overflow, sorter) are in. Powered belts wait for R. Belt Reader is not a leftover row.

---

## Remaining slices

**G, H, I, J, K, BD and BE are done.** Every row below that is not marked done is **held**
(ADR-0088). A held row stays in this file so the history is not thrown away. It is not the
next session. Do not mark a held row done by starting it.

### 0.2 leftovers — the factory starts feeling like a factory

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
| **G** | **Belt junctions** | Join two belts; run a belt through a wall | Merger, tunnel belt, overflow. Same lane model (ADR-0008, ADR-0060, ADR-0071). Voltaic crafts. `VerifyBelt` covers join/tunnel. | Sorter (H). Powered belts. | ✅ done |
| **H** | **Sorter** | Filter a stream by item | Sorter block. Sneak-filter like the splitter unless a GUI is required for multiple filters. | Logic Controller. | ✅ done |

Sifter stays off this wave. Tailings exist as a form; there is no B-row. Do not register a
machine that runs no graph.

### 0.3 — T2 industry (the acid payoff)

I, J and K are done. L through S are held.

Kiln already vents SO₂ into a tank. T2 is when that gas becomes acid and the factory builds
the factory for real.

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **I** | **T2 gate** | Research Industrial; MK2 is a real craft | T2 research + Advanced Data Core. Pylon MK2 recipe (block already exists). Assembler-only, not a crafting table (ADR-0017). | Wire Mill. Slice F. | ✅ done |
| **J** | **Wire and motors** | Fabricate coil, wire, motor | Wire Mill. Motor as a fabricated component. Feeds later electrical crafts. | Acid. | ✅ done |
| **K** | **Contact process** | Turn SO₂ into sulfuric acid | Chemical Reactor. SO₂ → SO₃ → sulfuric acid with the ratios in `PROCESSES.md`. Vanadia as catalyst. Named sinks for every fluid. R2 yield **1.15** may go live; it was waiting on this line (ADR-0065). | Washer. Etching acid (needs nitric + hydrochloric). | ✅ done |
| **L** | **Washer and B2** (held) | Wet line, byproduct from the vein | Chemical Washer. B2: crushed + water → washed crushed + byproduct. | Flotation (P). Electrolysis. |
| **M** | **Electrolysis and air** (held) | Split water and take N₂/O₂ | Electrolysis Cell, Atmospheric Intake. Hydrogen/oxygen sinks. | Fluid Well if the slice is already large — then N is next. |
| **N** | **Fluid Well** (held) | Chunk fluids | Water, brine, geothermal as specified. | New pipe tier. |
| **O** | **Induction and caster** (held) | Clean melt; skip the ingot | Induction Furnace, Caster. | Vacuum furnace (T5). |
| **P** | **Better separation** (held) | Concentrate sulfides; split mixed streams | Magnetic Separator, Froth Flotation Cell (B3). Surfactant reagent. Tailings sink. | Centrifuge (T3). Sifter still needs its own ADR. |
| **Q** | **Heat and steam** (held) | Power without burning coal only | Solar generation, Boiler, Condenser. Steam Turbine stays T3 with fission. | Arc Reactor. |
| **R** | **T2 fluids and belts** (held) | Move fluids and stacks | Pressure Pipe, Electric Pump, Industrial Tank, Fluid Manipulator, Flux Belt, Stack/Filter Manipulator. | Phase Manifold (T3). |
| **S** | **Logic** (held) | Enable a machine from a condition | Signal Cable, Logic Controller, Redstone Interface. README: *run only while copper ingots < 500*. | Operator Drones (T3). |

### 0.4 — Tools, matter, first two armour chassis (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **T** | **Flux Drill** (held) | Mine with charge, not durability | Area modes 1×1 / 3×3 / vein / tunnel. Cell fuel. | Multitool mining. |
| **U** | **Blueprint Tool** (held) | Save a layout and stamp it | The T2 checkpoint. Blueprints nameable and shareable. Drones still wait. | Construction Drones. |
| **V** | **Deconstruction Planner** (held) | Mark a region to tear down | Returns items to storage when drones exist; until then the planner may only mark, or pick with the Multitool relocate — do not invent a second wrench. | A new pickup tool. |
| **W** | **Patterns** (held) | Scan an item; smash to Matter | Pattern Scanner, Deconstructor. Replication *cost* from the graph (ADR-0010) can be computed before the Replicator block. | Replicator (T3, slice AE). |
| **X** | **Voltaic Harness** (held) | Wear T1 modular armour | Four pieces, small grid, protection + Flux Cell. No onboard generation. Vanilla armour stays valid until this ships. | Arc miniature. Network Tap. |
| **Y** | **Flux Exosuit** (held) | T2 chassis: network tap and mobility | Grid grows. Walk through pylons. **No worn reactor.** | Portable reactor module (ADR-0067). |

### 0.5 — Resonance and defence (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **Z** | **Resonance** (held) | See that industry has a cost | Emission, spread, decay, overlay. Configurable off. | Aberrations if the slice is already large — then AA. |
| **AA** | **Ballistic defence** (held) | Defend an unpowered outpost | Ballistic Turret, manufactured ammo, Flux Wall. | Laser (AB). |
| **AB** | **Powered defence and T2 weapons** (held) | Spend FU to fight | Laser Turret, Gauss Rifle, Arc Thrower, Flux Grenade. | Tesla / plasma (T3). |

### 0.6 — T3, drones, Slice F, Arc pair, fission (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AC** | **T3 gate and drones** (held) | Research Quantum; drones place blocks | T3 research, Construction Drones, Drone Bay. Drones consume a blueprint and network items. This is the T3 checkpoint. | Hand-placed 15³ trophy. |
| **AD** | **Slice F kernel** (held) | A formed structure with hatches | Formed multiblock + sided hatches as containers. Implemented *for* the Arc Reactor, not as an empty framework. | Distillation / fusion on this kernel before Arc runs. |
| **AE** | **Arc pair** (held) | F3 power you have to feed; a suit that burns the same cell | Arc Reactor (direct FU, not steam), Arc Cell line, Arc Exosuit miniature. Same unlock. If the cell line stops, both starve. | Arc Furnace as generator; fission; fusion. |
| **AF** | **Fission** (held) | Design a core | Parametric fission, neighbour bonus, coolant, SCRAM not boom, Steam Turbine. | Fusion. |
| **AG** | **Deep Core and centrifuge** (held) | Planetary pool; enrichment | Deep Core Drill, Centrifuge (B4). | Vacuum furnace. |
| **AH** | **Replicator** (held) | Matter + FU → scanned pattern | Priced from the live graph (ADR-0010). | Creative free items. |
| **AI** | **Circuits for real** (held) | Lithography bottleneck | Lithography Unit, circuit die, Integrated Circuit. Etching acid formulation in the Washer. | Quantum Assembler (T4). |
| **AJ** | **T3 logistics and weapons** (held) | Mag-Lev, crates, Tesla, shields, plasma, railgun, terraformer, cryo line/tank | As named in the T3 unlock row. | Phase Belt (T4). |

### 0.7 — T4 exotic (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AK** | **Fusion** (held) | Ignite D–T | Fusion Reactor, fuel cycles as in `MACHINES.md`. | p–¹¹B as the first cycle. |
| **AL** | **Accelerator** (held) | Make exotics and Exotic Data Cores | Particle Accelerator. Ring size changes *what* is possible. | A void ore dim. |
| **AM** | **T4 factory** (held) | Long chains, cold, dense Matter | Quantum Assembler, Cryogenic Plant, Matter Condenser. | Orbital vacuum machines. |
| **AN** | **Exotic Exosuit and T4 weapons** (held) | Last chassis; Fusion Lance, Singularity Charge | Exotic suit. Singularity Reactor if it is a real F-tier plant, not a trophy. | Horizon Gate. |

### 0.8 — Orbit (T5 checkpoint: Survey Satellite) (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AO** | **Leave the ground** (held) | Launch a rocket | Launch Pad, Rocket, payload mass. | Mass Driver in the same PR if the pad is not yet booting. |
| **AP** | **Bulk to orbit** (held) | Fire cargo without a rocket | Mass Driver, Orbital Catcher. | Using this as a gate. |
| **AQ** | **Station** (held) | Live in orbit | Orbital Platform and modules. | Planets. |
| **AR** | **Satellites** (held) | Map veins from orbit; beam power | Survey (headline), Solar Power, Relay, Sentinel, Logistics. Rectenna. | Deep Survey (T6). |
| **AS** | **Vacuum industry** (held) | Make what gravity forbids | Vacuum Furnace, Zero-G Crystallizer, Orbital Cryo Plant, Orbital Assembly Bay. | T6 colonies. |

### 0.9 — Interplanetary (T6 checkpoint: Horizon Gate) (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AT** | **Planet registry** (held) | See other worlds | Space-mod detection (ADR-0012). Fallback Thalassa + Helios (ADR-0068). Nether/End datapack-only. | A Grindless mining dimension. |
| **AU** | **Colonies** (held) | Profit without standing there | Colony Core, abstract simulation (ADR-0011), integrity, resupply, Drop Pods. | Force-loaded colony chunks. |
| **AV** | **Telepresence** (held) | Walk a Proxy Frame | Proxy Frame, Telepresence Terminal. | Creative possess-any-entity. |
| **AW** | **Horizon Gate** (held) | Walk after delivering the far ring | T6 dialed pair. People cheap, bulk still Mass Driver. Addresses from the registry. | Mining dim; skipping the cargo delivery. |
| **AX** | **Permanent link** (held) | Elevator and interplanetary routing | Space Elevator, Interplanetary Router. | Calling this 1.0. |

### 1.0 — Polish (held)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AY** | **Native solver** (held) | Ask the atlas *how many machines* | Route viewer with a ratio solver (ADR-0023). Atlas stops being a list. | Shipping JEI as a substitute for this. |
| **AZ** | **Recipe plugins** (held) | See Grindless in JEI/REI/EMI | Plugin after the graph and solver exist, or in parallel with AY if the stub is no longer enough. | Replacing the atlas. |
| **BA** | **Guide, advancements, config, locales** (held) | Learn in-game | Advancements, patchouli-or-native guide, config UI, `es_es` at least. | Stopping English as the source locale. |
| **BB** | **Balance and perf** (held) | A 300-mod pack runs | Belt/network perf, pack balance. Definition of 1.0. | New systems. |

---

## Modpack expansion

ADR-0088, rescheduled by ADR-0090. This is the urgent calendar. Do not resume L. Do not
start the quest book.

**Next slice: BK — Arrival at the galactic centre.** Not implemented. Do not start it in the interstellar change.

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **BD** | **Electric-arc steel** | Turn iron and coal into steel | The electric-arc route on the Arc Furnace: 10 iron ingots + 1 carbon → 10 steel ingots in 140 s at 1600 °C (ADR-0090). Plates, rods, gears and wire then follow the existing graph. | A new furnace. Oxygen blow. Direct reduction. The washer. The Autoclave. Planets. The quest book. | ✅ done |
| **BE** | **Refractory brick** | Spend slag on a lining | Slag's named sink (ADR-0036, ADR-0091): 1 slag → 1 refractory brick in 20 s at 1400 °C on the Arc Furnace. The furnace's 1500 °C hold stays inside that band. | The Kiln. The alumina + silica route. A new furnace. Oxygen blow. Aggregate and road fill. Planets. The quest book. | ✅ done |
| **BF** | **Metallurgical silicon** | Turn silica and carbon into metal | The Arc Furnace line in `PROCESSES.md`: `1 u silica + 2 u carbon [T 1900 · atm reducing]` 14 s → `1 u metallurgical silicon + 2 B CO`. Silica is `#grindless:silica` (sand and nether quartz). The furnace's 1500 °C hold stays; 1900 °C is tolerated (ADR-0092). | Electronic silicon. A new furnace. Retuning the 1500 °C hold. The washer. Planets. Megastructures. The quest book. | ✅ done |
| **BG** | **Further processing lines** | Extend the graph past metallurgical silicon | Zone refining on the Arc Furnace (ADR-0093): 10 metallurgical silicon → 7 electronic silicon in 600 s at 1420 °C. That is the graph's 0.70 yield and 60 s per unit. The ±5 °C inert band is not applied: it would refuse the 1500 °C reducing hold. | Siemens. The Induction Furnace. A boule. A wafer. Vacuum float. Orbital growth. Megastructures. Planets. The quest book. | ✅ done |
| **BH** | **Megastructures** | Build the first structure past the factory | The Ground Array (ADR-0094): one controller and eight casings in a fixed 3×3. A complete ring under a pylon adds ten seconds of MK3 throughput, 6,553,600 FU, as storage. Assembler, Industrial. No generator, no supply cube, no hatches. | Kardashev Type I, II and III. The Dyson Collector. Planets. Interstellar travel. The black hole. The quest book. | ✅ done |
| **BI** | **Original planets** | Reach a world with its own resources | Luna (ADR-0095): regolith, no ore, helium-3 from the existing extractor, and a Lunar Link that spends one capacitor of FU to go there. The return does not draw again. | The other fallback worlds. Interstellar travel. The black hole. The quest book. Kardashev Type I, II and III. Horizon Gates, rockets, colonies. | ✅ done |
| **BJ** | **Interstellar travel** | Leave the star | The Drift (ADR-0096): one deck between the home star and the galactic centre, with no ore. A Starward Link spends the Ground Array's buffer, 6,553,600 FU, to arrive. The return does not draw again. Assembler, Industrial: one lunar link and four array casings. | The black-hole interior. The quest book. Kardashev Type I, II and III. Further planets. | ✅ done |
| **BK** | **Arrival at the galactic centre** | Reach the black hole | Not sliced. Arriving at the Milky Way's central black hole is the victory. The interior is a finite finale written when this slice starts. | The quest book. Kardashev Type I, II and III. Further planets. An empty arrival. An endless interior. **Not implemented.** |

The original quest book is last, with the in-game guide, after that arrival.

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **BC** | **Original quest book and in-game guide** | Follow the pack, then read it in game | Last, after the black hole. An original quest book in the BetterQuesting style — lines, tasks, dependencies and rewards — together with the in-game guide. | Copying BetterQuesting, GregTech or Ad Astra code or assets. Starting this before the black hole. Planets are not this row. **Not next.** |

## How a session starts

1. Read `AGENTS.md`, `CHANGELOG.md` `[Unreleased]`, this file's **Next slice** line, then that
   slice's row.
2. Branch from the current tip. Implement only that slice. The next slice is BK. Held rows
   are not a queue. The quest book is last. Do not resume BJ.
3. When the slice is green locally, push, open or update the stacked draft, mark the row done,
   and name the following slice before starting it.

Uncommitted work does not exist. A session does not work through L–BB while they are held.
