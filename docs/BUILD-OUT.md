# Autonomous build-out

This is the remaining work, in the order a session implements it, so Grindless can be finished
without asking the owner what is next.

The README is still the design source of truth ([ADR-0013](DECISIONS.md#adr-0013--the-readme-is-the-design-source-of-truth)).
This file is the *schedule*. Open questions are answered here with the documented default; a
session writes an ADR only when it must pick among real alternatives, then ships the slice.

**Next slice: U — Blueprint Tool.** T (Flux Drill) is shipped.

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
| Etching, circuit board | AI | ADR-0064, ADR-0075, ADR-0076 |
| JEI / REI / EMI | AY or AZ | ADR-0066 |
| Horizon Gate, planets, mining dims | AT then AW | ADR-0068, ADR-0012 |
| Multitool mining | Never, without a new ADR | ADR-0055, ADR-0069 |
| Arc Furnace as a generator | Never | ADR-0067 |

---

## Already shipped

T0 loop, Slice A–E, Kiln/R2, Atlas stub, Multitool wrench, belt junctions (G), the sorter
(H), the T2 gate (I), Wire Mill / motor (J), the contact process (K), the Chemical
Washer (L), Electrolysis Cell and Atmospheric Intake (M), the Fluid Well (N), the
Induction Furnace and the Caster (O), the Froth Flotation Cell and the Magnetic
Separator (P), the Solar Array, the Boiler and the Condenser (Q), the Pressure
Pipe, Electric Pump, Industrial Tank, Fluid Manipulator, Flux Belt and the
Stack and Filter Manipulators (R), the Signal Cable, the Logic Controller
and the Redstone Interface (S), the Flux Drill and the Drill Cell (T), and the design
records for armour (ADR-0067)
and Horizon Gates (ADR-0068). Roadmap 0.1 definition of done — empty world to automated
iron without hand-mining — is met. 0.2 logistics leftovers from ADR-0060 (merger, tunnel,
overflow, sorter) are in. Powered belts wait for R. Belt Reader is not a leftover row.

---

## Remaining slices

Status is `pending` until the slice's PR exists on the stack. A session that finishes G marks
it done here in the same PR that ships G. **G through T are done.** A session that finishes U
marks U done and sets Next to V.

### 0.2 leftovers — the factory starts feeling like a factory

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
| **G** | **Belt junctions** | Join two belts; run a belt through a wall | Merger, tunnel belt, overflow. Same lane model (ADR-0008, ADR-0060, ADR-0071). Voltaic crafts. `VerifyBelt` covers join/tunnel. | Sorter (H). Powered belts. | ✅ done |
| **H** | **Sorter** | Filter a stream by item | Sorter block. Sneak-filter like the splitter unless a GUI is required for multiple filters. | Logic Controller. | ✅ done |

Sifter stays off this wave. Tailings exist as a form; there is no B-row. Do not register a
machine that runs no graph.

### 0.3 — T2 industry (the acid payoff)

Kiln already vents SO₂ into a tank. T2 is when that gas becomes acid and the factory builds
the factory for real.

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **I** | **T2 gate** | Research Industrial; MK2 is a real craft | T2 research + Advanced Data Core. Pylon MK2 recipe (block already exists). Assembler-only, not a crafting table (ADR-0017). | Wire Mill. Slice F. | ✅ done |
| **J** | **Wire and motors** | Fabricate coil, wire, motor | Wire Mill. Motor as a fabricated component. Feeds later electrical crafts. | Acid. | ✅ done |
| **K** | **Contact process** | Turn SO₂ into sulfuric acid | Chemical Reactor. SO₂ → SO₃ → sulfuric acid with the ratios in `PROCESSES.md`. Vanadia as catalyst. Named sinks for every fluid. R2 yield **1.15** may go live; it was waiting on this line (ADR-0065). | Washer. Etching acid (needs nitric + hydrochloric). | ✅ done |
| **L** | **Washer and B2** | Wet line, byproduct from the vein | Chemical Washer. B2: crushed + water → washed crushed + byproduct. | Flotation (P). Electrolysis. | ✅ done |
| **M** | **Electrolysis and air** | Split water and take N₂/O₂ | Electrolysis Cell, Atmospheric Intake. Hydrogen/oxygen sinks. | Fluid Well if the slice is already large — then N is next. | ✅ done |
| **N** | **Fluid Well** | Chunk fluids | Water, brine, geothermal as specified. | New pipe tier. | ✅ done |
| **O** | **Induction and caster** | Clean melt; skip the ingot | Induction Furnace, Caster. | Vacuum furnace (T5). | ✅ done |
| **P** | **Better separation** | Concentrate sulfides; split mixed streams | Magnetic Separator, Froth Flotation Cell (B3). Surfactant reagent. Tailings sink. | Centrifuge (T3). Sifter still needs its own ADR. | ✅ done |
| **Q** | **Heat and steam** | Power without burning coal only | Solar generation, Boiler, Condenser. Steam Turbine stays T3 with fission. | Arc Reactor. | ✅ done |
| **R** | **T2 fluids and belts** | Move fluids and stacks | Pressure Pipe, Electric Pump, Industrial Tank, Fluid Manipulator, Flux Belt, Stack/Filter Manipulator. | Phase Manifold (T3). | ✅ done |
| **S** | **Logic** | Enable a machine from a condition | Signal Cable, Logic Controller, Redstone Interface. README: *run only while copper ingots < 500*. | Operator Drones (T3). | ✅ done |

### 0.4 — Tools, matter, first two armour chassis

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **T** | **Flux Drill** | Mine with charge, not durability | Area modes 1×1 / 3×3 / vein / tunnel. Cell fuel. | Multitool mining. | ✅ done |
| **U** | **Blueprint Tool** | Save a layout and stamp it | The T2 checkpoint. Blueprints nameable and shareable. Drones still wait. | Construction Drones. |
| **V** | **Deconstruction Planner** | Mark a region to tear down | Returns items to storage when drones exist; until then the planner may only mark, or pick with the Multitool relocate — do not invent a second wrench. | A new pickup tool. |
| **W** | **Patterns** | Scan an item; smash to Matter | Pattern Scanner, Deconstructor. Replication *cost* from the graph (ADR-0010) can be computed before the Replicator block. | Replicator (T3, slice AE). |
| **X** | **Voltaic Harness** | Wear T1 modular armour | Four pieces, small grid, protection + Flux Cell. No onboard generation. Vanilla armour stays valid until this ships. | Arc miniature. Network Tap. |
| **Y** | **Flux Exosuit** | T2 chassis: network tap and mobility | Grid grows. Walk through pylons. **No worn reactor.** | Portable reactor module (ADR-0067). |

### 0.5 — Resonance and defence

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **Z** | **Resonance** | See that industry has a cost | Emission, spread, decay, overlay. Configurable off. | Aberrations if the slice is already large — then AA. |
| **AA** | **Ballistic defence** | Defend an unpowered outpost | Ballistic Turret, manufactured ammo, Flux Wall. | Laser (AB). |
| **AB** | **Powered defence and T2 weapons** | Spend FU to fight | Laser Turret, Gauss Rifle, Arc Thrower, Flux Grenade. | Tesla / plasma (T3). |

### 0.6 — T3, drones, Slice F, Arc pair, fission

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AC** | **T3 gate and drones** | Research Quantum; drones place blocks | T3 research, Construction Drones, Drone Bay. Drones consume a blueprint and network items. This is the T3 checkpoint. | Hand-placed 15³ trophy. |
| **AD** | **Slice F kernel** | A formed structure with hatches | Formed multiblock + sided hatches as containers. Implemented *for* the Arc Reactor, not as an empty framework. | Distillation / fusion on this kernel before Arc runs. |
| **AE** | **Arc pair** | F3 power you have to feed; a suit that burns the same cell | Arc Reactor (direct FU, not steam), Arc Cell line, Arc Exosuit miniature. Same unlock. If the cell line stops, both starve. | Arc Furnace as generator; fission; fusion. |
| **AF** | **Fission** | Design a core | Parametric fission, neighbour bonus, coolant, SCRAM not boom, Steam Turbine. | Fusion. |
| **AG** | **Deep Core and centrifuge** | Planetary pool; enrichment | Deep Core Drill, Centrifuge (B4). | Vacuum furnace. |
| **AH** | **Replicator** | Matter + FU → scanned pattern | Priced from the live graph (ADR-0010). | Creative free items. |
| **AI** | **Circuits for real** | Lithography bottleneck | Lithography Unit, circuit die, Integrated Circuit. Etching acid formulation in the Washer. | Quantum Assembler (T4). |
| **AJ** | **T3 logistics and weapons** | Mag-Lev, crates, Tesla, shields, plasma, railgun, terraformer, cryo line/tank | As named in the T3 unlock row. | Phase Belt (T4). |

### 0.7 — T4 exotic

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AK** | **Fusion** | Ignite D–T | Fusion Reactor, fuel cycles as in `MACHINES.md`. | p–¹¹B as the first cycle. |
| **AL** | **Accelerator** | Make exotics and Exotic Data Cores | Particle Accelerator. Ring size changes *what* is possible. | A void ore dim. |
| **AM** | **T4 factory** | Long chains, cold, dense Matter | Quantum Assembler, Cryogenic Plant, Matter Condenser. | Orbital vacuum machines. |
| **AN** | **Exotic Exosuit and T4 weapons** | Last chassis; Fusion Lance, Singularity Charge | Exotic suit. Singularity Reactor if it is a real F-tier plant, not a trophy. | Horizon Gate. |

### 0.8 — Orbit (T5 checkpoint: Survey Satellite)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AO** | **Leave the ground** | Launch a rocket | Launch Pad, Rocket, payload mass. | Mass Driver in the same PR if the pad is not yet booting. |
| **AP** | **Bulk to orbit** | Fire cargo without a rocket | Mass Driver, Orbital Catcher. | Using this as a gate. |
| **AQ** | **Station** | Live in orbit | Orbital Platform and modules. | Planets. |
| **AR** | **Satellites** | Map veins from orbit; beam power | Survey (headline), Solar Power, Relay, Sentinel, Logistics. Rectenna. | Deep Survey (T6). |
| **AS** | **Vacuum industry** | Make what gravity forbids | Vacuum Furnace, Zero-G Crystallizer, Orbital Cryo Plant, Orbital Assembly Bay. | T6 colonies. |

### 0.9 — Interplanetary (T6 checkpoint: Horizon Gate)

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AT** | **Planet registry** | See other worlds | Space-mod detection (ADR-0012). Fallback Thalassa + Helios (ADR-0068). Nether/End datapack-only. | A Grindless mining dimension. |
| **AU** | **Colonies** | Profit without standing there | Colony Core, abstract simulation (ADR-0011), integrity, resupply, Drop Pods. | Force-loaded colony chunks. |
| **AV** | **Telepresence** | Walk a Proxy Frame | Proxy Frame, Telepresence Terminal. | Creative possess-any-entity. |
| **AW** | **Horizon Gate** | Walk after delivering the far ring | T6 dialed pair. People cheap, bulk still Mass Driver. Addresses from the registry. | Mining dim; skipping the cargo delivery. |
| **AX** | **Permanent link** | Elevator and interplanetary routing | Space Elevator, Interplanetary Router. | Calling this 1.0. |

### 1.0 — Polish

| ID | Slice | Player can | Ship | Do not |
| --- | --- | --- | --- | --- |
| **AY** | **Native solver** | Ask the atlas *how many machines* | Route viewer with a ratio solver (ADR-0023). Atlas stops being a list. | Shipping JEI as a substitute for this. |
| **AZ** | **Recipe plugins** | See Grindless in JEI/REI/EMI | Plugin after the graph and solver exist, or in parallel with AY if the stub is no longer enough. | Replacing the atlas. |
| **BA** | **Guide, advancements, config, locales** | Learn in-game | Advancements, patchouli-or-native guide, config UI, `es_es` at least. | Stopping English as the source locale. |
| **BB** | **Balance and perf** | A 300-mod pack runs | Belt/network perf, pack balance. Definition of 1.0. | New systems. |

---

## How a session starts

1. Read `AGENTS.md`, `CHANGELOG.md` `[Unreleased]`, this file's **Next slice** line, then that
   slice's row.
2. Branch from the current tip. Implement only that slice.
3. When the slice is green locally, push, open or update the stacked draft, mark the row done,
   set **Next slice** to the following id, and start it.

If the owner is absent, keep going until BB is done or the session dies. Uncommitted work does
not exist.
