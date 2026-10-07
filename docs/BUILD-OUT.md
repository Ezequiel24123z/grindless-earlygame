# Autonomous build-out

This file keeps the history of the slices already defined and names the next executable slice.

The README is still the design source of truth ([ADR-0013](DECISIONS.md#adr-0013--the-readme-is-the-design-source-of-truth)).
[ADR-0107](DECISIONS.md#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)
replaces the old six-tier calendar with an aligned T0/F0–T15/F15 campaign. The tables from G
through BN remain below as implementation history; their old tier placement is not an active
queue.

**Active tier: T0 Bootstrap. Next slice: BP — complete the first Relay Matrix route.** BO is
shipped: the Ground Array, placeholder links, rocket and supraluminal station remain registered
and smoke-tested, but have no generated or crafting-table survival recipes. F0-F15 and the physical
matrix rating/architecture model are now in code, including a registered T1 Relay Matrix with
functional generated art and its recoverable four-unit manual batch. The legacy-state seam,
Atlas/quest route and remaining T0 machine audit are still active. T1 stays locked until T0 passes
the gate below.

---

## Mechanical tier completion workflow

[ADR-0108](DECISIONS.md#adr-0108--finish-one-mechanical-tier-before-opening-the-next) makes this
the release gate for every tier. Work down the checklist for the active tier; split implementation
into checkpoint commits and small slices, but do not activate the next row early.

### Completion gate

1. **Entry and exit are explicit.** The tier names its survival starting state, its frontier
   Control Matrix or terminal objective, its expected scale and a target playtime range.
2. **The four routes are complete and braided.** Materials, chemistry, computation/control and
   energy have concrete inputs, processes, outputs and at least the cross-dependencies promised by
   the progression lattice. Every required byproduct has a reachable sink.
3. **Everything required is obtainable.** Resources, intermediates, catalysts, machines, tools,
   upgrades, power, storage and transport all have survival routes. No creative item, command,
   hidden world permission or future-tier prototype closes a gap.
4. **Machines work as factory parts.** Input and output automation, power and conditions,
   actionable status/fault feedback, persistence, blocked-output recovery and safe relocation or
   drops are covered. The route works after save/reload and does not depend on one exact placement
   unless that layout is the documented puzzle.
5. **Progression is discoverable.** The Atlas, quest book, guide and tooltips agree with the live
   route. A player does not need source code or an external wiki to discover the next mechanical
   action.
6. **The tier is played end to end.** Starting from the stated survival entry, the player reaches
   the frontier objective without commands. Repetition stays inside the tier's intentional scale;
   missing automation is a failure, not "difficulty".
7. **Evidence is green.** Relevant pure behaviour checks, `:common:compileJava`, all repository
   checks, link validation and the full build pass. Use targeted boot/interaction smokes for
   registration, loader, menu, persistence or other Minecraft-only seams. Run the full historical
   smoke matrix for integration/merge, not after every data-only edit.
8. **Handoff is complete.** ADRs capture choices, the changelog records the result, this table and
   the README name the accepted frontier, and only then is the next tier unlocked.

### Deferred art boundary

Final textures/models, animation, polished sound, effects and complete localization wait for the
project-wide art pass. Every tier still needs functional placeholder assets, readable names,
tooltips, menus and state feedback: presentation polish is deferred; mechanical legibility is not.

### Tier status

| Tier | Mechanical status | Acceptance objective |
| --- | --- | --- |
| **T0 Bootstrap** | **active audit** | A fresh survival player reaches a recoverable first T1 Relay Matrix batch with assisted extraction and F0 power. |
| **T1 Voltaic** | locked by T0 | The first powered factory manufactures a T2 Relay Matrix and no longer depends on hand fabrication. |
| **T2 Industrial** | locked by T1 | Closed material, chemical, control and energy loops manufacture the T3 frontier matrix. |
| **T3 Arc** | locked by T2 | Construction Drones and a fed Arc Reactor support the first designed multiblock frontier. |
| **T4 Precision** | locked by T3 | Precision chemistry, lithography and reusable patterns produce the first Integrated frontier. |
| **T5 Nuclear** | locked by T4 | A complete fuel cycle and designed fission core close the tier. |
| **T6 Cryogenic** | locked by T5 | Deep cryogens, superconductors, magnets and pulse storage prepare sustained fusion. |
| **T7 Fusion** | locked by T6 | A closed D-T fuel and blanket loop sustains fusion. |
| **T8 Particle** | locked by T7 | Accelerator scale, targets, separation and beam control produce the frontier exotics. |
| **T9 Quantum** | locked by T8 | Quantum control and programmable Matter operate as a terrestrial campus. |
| **T10 Orbital** | locked by T9 | Ground and orbit form one supplied production network. |
| **T11 Planetary** | locked by T10 | Several worlds operate as one recoverable industrial system. |
| **T12 Stellar** | locked by T11 | Stellar processing and a Dyson network reach Type II scale. |
| **T13 Interstellar** | locked by T12 | Several star systems exchange production through the real supraluminal route. |
| **T14 Galactic** | locked by T13 | Abstract self-expansion, antimatter and singularity power reach Type III scale. |
| **T15 Event Horizon** | locked by T14 | The complete route reaches Sagittarius; arrival is victory. |

---

## Campaign restoration

Only the first incomplete row is active. Later rows are named so a session does not have to
reconstruct the intended order, but it does not start them early.

| ID | Slice | Player can | Ship | Preserve | Done when |
| --- | --- | --- | --- | --- | --- |
| **BO** | **Withdraw prototype survival shortcut** | Follow every currently implemented survival quest without being sent into future-tier test infrastructure | Remove the Industrial Assembler recipes for Array Casing, Ground Array, Lunar Link, Starward Link, Launch Pad, Survey Rocket, Station Berth and Supraluminal Station. End the quest/guide route at the last genuinely reachable T2 objective. Update graph counts and checks. | All block/item registrations, Luna, the Drift, Sagittarius, travel logic, commands and smoke scenarios. Existing worlds may still contain and exercise every prototype. | ✅ done — 107 graph rows, 39 Assembler rows, eleven quests through electronic silicon, no prototype recipe or dimension quest. |
| **BP** | **T0 closure: Control Matrix foundation** | Complete Bootstrap by building and inspecting the first T1 Relay Matrix batch | Matrix item/data model, substitution rules and the recoverable four-unit T1 batch are in. Add the legacy-state migration seam and Atlas visibility; audit the whole T0 route against the tier gate. | Existing worlds and the current two-iron extraction bootstrap. | T0 passes the mechanical gate; matrix checks cover rating, architecture, substitution and migration; F0–F15 values match ADR-0107. |
| **BQ** | **Physical T2 gate** | Manufacture T2 machines from a T2 Relay Matrix rather than a world permission | T2 matrix route braided through the existing material, chemical, control and power lines. Remove Voltaic/Industrial checks from ordinary fabrication. Recast Data Cores as pattern media. | Existing fabricated machines and player progress. | Every T2 machine consumes the physical rating; no ordinary recipe consults a global blueprint flag. |
| **BR** | **T3 Arc frontier** | Build a larger factory with drones and fed Arc power | Refractory/Arc Cell routes, Construction Drones, the first parametric multiblock kernel, Arc Reactor and T3 Relay Matrix. | The existing T0–T2 lines as useful low-infrastructure routes. | The four lattice domains interact before the T3 matrix, and the Arc Reactor runs in a booted server. |

T4–T15 follow the lattice in
[`PROCESSES.md`](PROCESSES.md#the-progression-lattice). Split each frontier into playable slices
when its predecessor is real; do not create empty machine shells or a whole tier in one review.

---

## Standing rules

Do not stop to ask. If a number, name or recipe already exists in the README, `MACHINES.md` or
`PROCESSES.md`, use it. If it does not, write an ADR with the cheapest default that preserves
the records below, then implement.

### Product

- Playable slices, not system layers ([ADR-0058](DECISIONS.md#adr-0058--build-playable-slices-not-system-layers)).
  A slice ships when a player can do a new thing in-world.
- Finish one mechanical tier before opening the next
  ([ADR-0108](DECISIONS.md#adr-0108--finish-one-mechanical-tier-before-opening-the-next)).
  Several small slices may close one tier; none may silently start the next.
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
5. Targeted smoke in `tools/smoke/` when the slice changes a block, item, menu, registration,
   persistence or another Minecraft-only seam. Handshake after `Done` (`SMOKE-READY`) before
   scenario commands. The complete scenario matrix is the integration/merge pass.
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
| Etching, circuit board | AI, and AI is held | ADR-0064, ADR-0075, ADR-0076, ADR-0088 |
| JEI / REI / EMI | AY or AZ, both held | ADR-0066, ADR-0088 |
| Horizon Gate, mining dims | AT then AW, both held; original planets are the modpack expansion, not a mining dim | ADR-0068, ADR-0012, ADR-0088 |
| Resume any held row Z–BB | A later record that un-holds that row | ADR-0088 |
| Copy code whose license is unconfirmed or All Rights Reserved | Never | ADR-0088, ADR-0089 |
| Copy LGPL or MIT code without its notice and license text | The slice that adds those terms | ADR-0089 |
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
and the Redstone Interface (S), the Flux Drill and the Drill Cell (T), the Blueprint
Tool (U), the Deconstruction Planner (V), the Pattern Scanner and the Deconstructor (W),
the Voltaic Harness (X), the Flux Exosuit (Y), the whole modpack expansion from
electric-arc steel (BD) to the galactic centre (BN) and the quest book (BC), and the design
records for armour (ADR-0067)
and Horizon Gates (ADR-0068). Roadmap 0.1 definition of done — empty world to automated
iron without hand-mining — is met. 0.2 logistics leftovers from ADR-0060 (merger, tunnel,
overflow, sorter) and the powered belts from R are in. Belt Reader is not a leftover row.

---

## Historical six-tier slices

**G through Y are done**, and so is the whole modpack expansion (BD–BN, then BC). Every
row below that is not marked done was **held** by ADR-0088: Z–BB and BK. ADR-0107 supersedes
their tier placement and the active campaign calendar now starts at BO. These rows stay so the
history is not thrown away; do not execute them as written.

### 0.2 leftovers — the factory starts feeling like a factory

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
| **G** | **Belt junctions** | Join two belts; run a belt through a wall | Merger, tunnel belt, overflow. Same lane model (ADR-0008, ADR-0060, ADR-0071). Voltaic crafts. `VerifyBelt` covers join/tunnel. | Sorter (H). Powered belts. | ✅ done |
| **H** | **Sorter** | Filter a stream by item | Sorter block. Sneak-filter like the splitter unless a GUI is required for multiple filters. | Logic Controller. | ✅ done |

Sifter stays off this wave. Tailings exist as a form; there is no B-row. Do not register a
machine that runs no graph.

### 0.3 — T2 industry (the acid payoff)

I through S are done.

Kiln already vents SO₂ into a tank. T2 is when that gas becomes acid and the factory builds
the factory for real.

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
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

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
| **T** | **Flux Drill** | Mine with charge, not durability | Area modes 1×1 / 3×3 / vein / tunnel. Cell fuel. | Multitool mining. | ✅ done |
| **U** | **Blueprint Tool** | Save a layout and stamp it | The T2 checkpoint. Blueprints nameable and shareable. Drones still wait. | Construction Drones. | ✅ done |
| **V** | **Deconstruction Planner** | Mark a region to tear down | Returns items to storage when drones exist; until then the planner may only mark, or pick with the Multitool relocate — do not invent a second wrench. | A new pickup tool. | ✅ done |
| **W** | **Patterns** | Scan an item; smash to Matter | Pattern Scanner, Deconstructor. Replication *cost* from the graph (ADR-0010) can be computed before the Replicator block. | Replicator (T3, slice AH). | ✅ done |
| **X** | **Voltaic Harness** | Wear T1 modular armour | Four pieces, small grid, protection + Flux Cell. No onboard generation. Vanilla armour stays valid until this ships. | Arc miniature. Network Tap. | ✅ done |
| **Y** | **Flux Exosuit** | T2 chassis: network tap and mobility | Grid grows. Walk through pylons. **No worn reactor.** | Portable reactor module (ADR-0067). | ✅ done |

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

## Historical modpack expansion

ADR-0088, rescheduled by ADR-0090. This calendar ran alongside the T2 foundation rows
L–Y and is complete as prototype infrastructure. ADR-0107 supersedes its survival tier
placement; the active calendar is [Campaign restoration](#campaign-restoration).

BC is done (ADR-0100). BK (teleportation orbs) remains only a historical proposal.
The Lunar Link and the Starward Link stay registered. The station replaced the hop
(ADR-0098). The link block no longer moves a player.

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
| **BD** | **Electric-arc steel** | Turn iron and coal into steel | The electric-arc route on the Arc Furnace: 10 iron ingots + 1 carbon → 10 steel ingots in 140 s at 1600 °C (ADR-0090). Plates, rods, gears and wire then follow the existing graph. | A new furnace. Oxygen blow. Direct reduction. The washer. The Autoclave. Planets. The quest book. | ✅ done |
| **BE** | **Refractory brick** | Spend slag on a lining | Slag's named sink (ADR-0036, ADR-0091): 1 slag → 1 refractory brick in 20 s at 1400 °C on the Arc Furnace. The furnace's 1500 °C hold stays inside that band. | The Kiln. The alumina + silica route. A new furnace. Oxygen blow. Aggregate and road fill. Planets. The quest book. | ✅ done |
| **BF** | **Metallurgical silicon** | Turn silica and carbon into metal | The Arc Furnace line in `PROCESSES.md`: `1 u silica + 2 u carbon [T 1900 · atm reducing]` 14 s → `1 u metallurgical silicon + 2 B CO`. Silica is `#grindless:silica` (sand and nether quartz). The furnace's 1500 °C hold stays; 1900 °C is tolerated (ADR-0092). | Electronic silicon. A new furnace. Retuning the 1500 °C hold. The washer. Planets. Megastructures. The quest book. | ✅ done |
| **BG** | **Further processing lines** | Extend the graph past metallurgical silicon | Zone refining on the Arc Furnace (ADR-0093): 10 metallurgical silicon → 7 electronic silicon in 600 s at 1420 °C. That is the graph's 0.70 yield and 60 s per unit. The ±5 °C inert band is not applied: it would refuse the 1500 °C reducing hold. | Siemens. The Induction Furnace. A boule. A wafer. Vacuum float. Orbital growth. Megastructures. Planets. The quest book. | ✅ done |
| **BH** | **Megastructures** | Build the first structure past the factory | The Ground Array (ADR-0094): one controller and eight casings in a fixed 3×3. A complete ring under a pylon adds ten seconds of MK3 throughput, 6,553,600 FU, as storage. Assembler, Industrial. No generator, no supply cube, no hatches. | Kardashev Type I, II and III. The Dyson Collector. Planets. Interstellar travel. The black hole. The quest book. | ✅ done |
| **BI** | **Original planets** | Reach a world with its own resources | Luna (ADR-0095): regolith, no ore, helium-3 from the existing extractor, and a Lunar Link that spends one capacitor of FU to go there. The return does not draw again. The link stays as a placeholder for the rocket (ADR-0097). | The other fallback worlds. Interstellar travel. The black hole. The quest book. Kardashev Type I, II and III. Horizon Gates, rockets, colonies. | ✅ done |
| **BJ** | **Interstellar travel** | Leave the star | The Drift (ADR-0096): one deck between the home star and the galactic centre, with no ore. A Starward Link spends the Ground Array's buffer, 6,553,600 FU, to arrive. The return does not draw again. Assembler, Industrial: one lunar link and four array casings. The link stayed as a placeholder until the station (ADR-0098). | The black-hole interior. The quest book. Kardashev Type I, II and III. Further planets. | ✅ done |
| **BK** | **Teleportation orbs** | Skip a repeat rocket trip | Not sliced. Just before spaceflight, an alternate route of a magical material — unnamed until this slice — ends in teleportation orbs. Shift-right-click sets coordinates and dimension. Right-click teleports. The orb can sit on a pedestal. After the first landing on a planet, the orb is how you return. Draconic Evolution may inspire the design. | Naming the material. Copying Draconic Evolution code or assets. The current tree is All Rights Reserved under the CoFH Don't Be a Jerk license, and the assets are CC BY-NC-SA 4.0, so neither enters this MIT repository (ADR-0089). The 1.7.10 MIT text is an older generation and is not this orb. Implementing this row. Replacing the rocket. The station. The quest book. **Not implemented. Not next.** |
| **BL** | **Rocket ascent** | Fly to the ceiling and choose a landing site | The survey rocket (ADR-0097). It climbs to the build ceiling, then a landing map lists the home world and Luna. Assembler, Industrial: the pad is one machine casing and four steel plates; the rocket is one casing, one motor and two steel plates. Leaving home spends 102,400 FU. Leaving Luna does not. | The station. The black-hole interior. Deleting either placeholder link. The teleportation orbs. The quest book. Kardashev Type I, II and III. Further planets. | ✅ done |
| **BM** | **Supraluminal station** | Ride a station to another star | The supraluminal station (ADR-0098). It climbs to the ceiling, and that ceiling is the arrival on the Drift. Assembler, Industrial: the berth is one starward link and four steel plates; the station is one casing, one motor and two array casings. Leaving any world but the Drift spends 6,553,600 FU. Leaving the Drift does not. ADR-0099 sends that free ride to the galactic centre. The Starward Link stays registered and no longer moves a player. | The quest book. Kardashev Type I, II and III. Further planets. A third teleport link. The teleportation orbs. | ✅ done |
| **BN** | **Arrival at the galactic centre** | Reach the black hole by riding the station | The galactic centre (ADR-0099). Riding the station from the Drift arrives in a sealed chamber, `grindless:sagittarius`. The mass is unbreakable horizon shell, sixteen blocks tall, with no vein. The carve is a 7×4×7 room, one arrival mark, one berth, and a 3×3 shaft so the ride home has air. Leaving the Drift and leaving the chamber draw nothing. Leaving the chamber returns to the berth saved on the way to the Drift. The Starward Link stays registered and still does not move a player. No new link and no new recipe. | The quest book. Kardashev Type I, II and III. Further planets. An empty arrival. An endless interior. A link. | ✅ done |

The original quest book is in, with the in-game guide, after that arrival. Nothing follows it.

| ID | Slice | Player can | Ship | Do not | Done when |
| --- | --- | --- | --- | --- | --- |
| **BC** | **Original quest book and in-game guide** | Follow the pack, then read it in game | Last, after the black hole. An original quest book — lines, tasks, dependencies and rewards — together with the in-game guide (ADR-0100). The lines run from the Multitool to the sealed chamber. | Copying BetterQuesting, GregTech or Ad Astra code or assets. Planets are not this row. A machine gate. A slice after this one. | ✅ done |

## How a session starts

1. Read `AGENTS.md`, `CHANGELOG.md` `[Unreleased]`, this file's opening status, then the
   row it names. BP is next.
2. Branch from the current tip. Implement only the active row. The historical held rows
   are not a queue. Do not start BK or resume BN.
3. When the slice is green locally, push, open or update the stacked draft, mark the row done,
   and name the following slice before starting it.

Uncommitted work does not exist. A session does not work through historical Z–BB out of order.
