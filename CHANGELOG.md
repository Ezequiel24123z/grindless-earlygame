# Changelog

Every change to code, build configuration or documentation is recorded here, newest first.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project will
follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) from its first release.

This file is a handoff document as much as a history. Sessions working on Grindless are
short-lived and frequently lost, so `[Unreleased]` is the first place a new session looks to find
out what the previous one actually did. Keep it current — see [`AGENTS.md`](AGENTS.md).

Design rationale is *not* recorded here. It goes in [`docs/DECISIONS.md`](docs/DECISIONS.md), and
entries below reference those records by id.

## [Unreleased]

### Added

- **The process layer** (`docs/PROCESSES.md`) — step 9 of the implementation plan, and the last
  design step before systems code. It specifies what flows through the machine layer: the item and
  fluid catalogue, the eight-stratum recipe graph, the concrete routes with their conditions, times
  and ratios, and an audit of which products are reachable more than one way.
  Four structural ideas carry it. The **item catalogue is a matrix, not a list** — form × material
  and anion × material, so thirteen formed types and six compound families cover an unknown pack
  (ADR-0032). **Routes compose** from a beneficiation stage and a reduction stage, so nine authored
  process families produce twenty ore routes and yield is `grade × factor`, running 1× to ~4×
  (ADR-0035). An **unwritten condition is not a condition**, which keeps envelope matching cheap and
  partly answers `MACHINES.md`'s open question about UI load — no recipe names more than three of
  the six dimensions. And **every byproduct has a named sink**, enforced by a ledger, because a
  stream you can only void is a chore rather than content (ADR-0036).
  The chemical core is specified as four closed loops — sulfur, chlor-alkali, air separation,
  nitrogen fixation — which is what makes a factory a system instead of parallel lines: sulfuric
  acid is made from the SO₂ a roaster would have vented, spent in leaching, and regenerated from
  the raffinate at 90 %.
  Six tensions with earlier documents were found and all resolved as omissions in this layer rather
  than errors in `MACHINES.md`, so no corrective ADR was needed. Liquid nitrogen coming from the
  **T2** Atmospheric Intake is what keeps the T3 Superconductor from needing a T4 Cryogenic Plant;
  etching acid is *formulated* in the Chemical Washer from acids the Reactor *synthesised*, which
  makes the README's source attribution correct as written.
- Five decision records, ADR-0032 to ADR-0036, covering the catalogue matrix, the split between
  tag-resolved materials and Grindless's own reagents, the canonical process unit that makes routes
  comparable, route composition, and the byproduct-sink rule.
- **Construction Drones reframed as the enabler for massive multiblocks** (ADR-0031). They were in
  the design as a convenience, which is not a good enough reason to ship anything in a mod about
  removing busywork. The useful question is what they enable, and the answer changes the multiblock
  design: the real cap on multiblock scale in every mod is **placement tedium, not design** — nobody
  ships a two-thousand-block structure because nobody will place two thousand blocks by hand, so
  multiblocks stay at 5×5×5 and their design space stays shallow. Placing them by hand is grind by
  this project's own definition, so the placing is automated and the designing kept.
  Multiblock scale now grows with the ability to build: ~5³ by hand at T2, ~15³ with drones at T3,
  ~32³ with swarms at T4, chunk-scale at T5 via the new **Assembly Field**. Blueprints are
  **validated and simulated before construction**, reporting legality, computed output, heat,
  coolant demand, stability margin and bill of materials, so iterating on a design is affordable.
  The Fission Reactor is expanded to match — moderators and reflectors make the core's *geometry*
  matter, not just its volume.
- **Operator Drones** (README *System 4*). The imperative half of automation. Every system so far —
  belts, pipes, conduits, logistics drones — is continuous flow and declarative, and none of them
  can express *"wait until the autoclave finishes, take the batch to the press, run it, bring the
  byproduct back, swap the catalyst if spent"*. That is a sequence, and without it the player does
  such work by hand, which is grind by definition.
  Routines are **built, not typed**: an ordered strip of physical Instruction Cards, closer to a
  player-piano roll than to code, with a vocabulary of eleven cards of which only two are branches.
  Capability comes from pods — cargo, fluid tank, thermal, tool arm, sensor, range extender.
  Debuggability is designed in rather than added later: the drone shows its current instruction in
  flight, step mode advances one instruction at a time, failures name themselves on the bay and as
  a logic signal, and editing draws a holographic path preview. Routine Cards make a finished
  routine one copyable item. Deliberately slow and single-tasking, so belts stay correct for
  volume; at T6 the same routines run on remote colonies where belts cannot reach (ADR-0029).
- Two design principles added to the README, both describing patterns the design had already
  converged on: **everything is a chassis plus modules** (ADR-0030), and **pay in layout or pay in
  power** — every convenience has a cheaper manual counterpart that stays correct forever.
- Two decision records, ADR-0029 and ADR-0030.
- **Flux Conduits** (README *System 4*). One conduit chassis whose carried types are decided by
  **cores** inserted into it — item, fluid, heat, signal and, rarely, Flux — so a single run can
  carry ore, coolant, heat and the logic that controls them. Networks are **named** rather than
  picked from sixteen colours, because colours stop being readable long before a base stops growing
  (ADR-0026). Flow is visible, every segment reports its own utilisation and backpressure as a
  signal, and cores upgrade independently: bore, filtering, routing, insulation, and at the top
  Phase, where distance within a network stops mattering. Four conduit tiers from T2 to T5.
  Belts and pipes deliberately survive this: item cores draw power per item while belts move bulk
  for free, a full belt is also thousands of items of buffer, and pipes exploit gravity and head
  pressure for nothing. The division that results is the one real factories have — belts and pipes
  move bulk, conduits move logistics (ADR-0025). From T2, Signal Cable is a conduit core rather
  than a separate block.
- **Chassis marks and machine upgrades** (`docs/MACHINES.md`). Machines exist at MK I–MK V, applied
  **in place** with a Chassis Upgrade Kit so nothing is ever rebuilt or re-piped. A mark's defining
  property is that it **widens the condition envelope**, so because recipes are selected by
  conditions a higher mark unlocks *recipes* rather than adding a speed number — an MK I Arc
  Furnace reaches 1800 °C, an MK V reaches 3500 °C (ADR-0027).
  Ten upgrades, each of which **spends one resource to buy another** rather than being strictly
  better, with three mutually exclusive pairs (Speed↔Efficiency, Speed↔Precision,
  Insulation↔Parallel). Which one is right depends on whether you are power-limited,
  material-limited or throughput-limited, and that changes across the game. Speed is almost never
  right, consistent with the stance on overclocking (ADR-0028).
- Four decision records, ADR-0025 to ADR-0028.
- **The first green build on both loaders** — step 7 of the implementation plan, the project's
  first real milestone. `gradlew :fabric:build :forge:build` produces `grindless-0.1.0-fabric.jar`
  and `grindless-0.1.0-forge.jar`, each bundling the common classes, `pack.mcmeta` and the lang
  file, with loader metadata correctly expanded from `gradle.properties`.
- **Registry layer** on Architectury `DeferredRegister`: `ModBlocks`, `ModItems`,
  `ModBlockEntities`, `ModMenus`, `ModCreativeTabs`, plus `GrindlessConfig` and `MaterialRegistry`
  as the call sites `Grindless.init` already expected. Blocks register their own `BlockItem` so the
  two registers cannot drift. Registered content is the T0 bootstrap set only — Hand Crank Dynamo,
  Crude Extractor, Research Terminal, Multitool, Data Core, Machine Casing — as plain blocks and
  items, with block entities and menus deliberately empty until the machines behind them exist.
- **Loader entrypoints and metadata**: a Fabric `ModInitializer` with `fabric.mod.json`, and a
  Forge `@Mod` class that registers the Architectury event bus before common init, with a
  `mods.toml` whose `[47,)` loader range also admits NeoForge 1.20.1 (ADR-0002).
- **Committed Gradle wrapper** (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) pinned to Gradle 8.8,
  so the repository builds without a system-wide Gradle.
- `.gitattributes` pinning `gradlew` to LF and `gradlew.bat` to CRLF. Git was about to normalise
  `gradlew` to CRLF on checkout, which breaks it on Linux, macOS and CI with a `bad interpreter`
  error that says nothing about line endings.
- **Machine layer design** — [`docs/MACHINES.md`](docs/MACHINES.md). The complete machine,
  multiblock and process specification, written before any recipe exists (ADR-0019). Its core is
  that a recipe is `inputs + conditions + time → outputs`, with conditions being temperature,
  pressure, atmosphere, catalyst, field and agitation, so a machine is a **condition envelope**
  rather than a recipe holder (ADR-0020). Also covers the complexity-without-grind position against
  GregTech (ADR-0021), the ten-step Flux tier ladder, three scaling axes with overclocking
  deliberately the worst of them, the single-block machine catalogue, parametric multiblocks whose
  shape is a design parameter (ADR-0022), the multiple-routes rule, the endgame tier, and the
  Process Atlas route viewer with its ratio solver (ADR-0023).
- Six decision records, ADR-0019 to ADR-0024.
- Implementation plan gained a step for the conduit layer and the upgrade framework.
- **Fluids, as a first-class system** (README *System 5*). Fluids carry volume, temperature and
  pressure rather than being items with a different texture, so steam, superheated steam and
  supercritical water are distinct resources; heat is conserved, so a cooling line condenses and a
  turbine fed condensate stalls. Pressure is the throughput limit — gases need pumps, liquids run
  downhill free, over-pressure vents repairably rather than exploding. Four pipe tiers, the pump
  and valve set, four tank tiers, and at T3 the **Phase Manifold**, a coverage-area fluid network
  that costs FU per unit moved and is deliberately not a strict upgrade over pipes
  (ADR-0015, ADR-0016).
- **The fabrication rule** (README *Fabrication: the factory builds the factory*). Past T1,
  machines have no crafting-table recipe at all: T2–T3 come from the Assembler, T4 from the
  Quantum Assembler, T5–T6 from the Orbital Assembly Bay, each consuming a researched blueprint,
  fabricated components, power and time. Adds the component economy — casings, motors, pumps,
  circuit boards, integrated circuits, superconductors, quantum cores — several of which require
  fluids, which is what puts fluids on the critical path exactly once, early (ADR-0017).
- **The container contract** (README *Containers: buffers, filters and voiding*). One interface
  for every crate, tank and machine buffer: persistent per-slot filters, buffer targets, capacity
  limits, per-face I/O, priorities, signal output, and configurable auto-void that is off by
  default, explicitly confirmed, visibly marked, trims rather than empties, and refuses to discard
  blacklisted items (ADR-0018).
- Four decision records, ADR-0015 to ADR-0018, covering the above.
- **Working agreement** (`AGENTS.md`). The contract for every session on this project: commit at
  every checkpoint, record decisions in the repository, prefer file edits and batched git over
  shell commands, and reply in Spanish while writing English in the repository. Also documents the
  repository layout, commit conventions, and how to push when git has no credentials.
- **Architecture decision records** (`docs/DECISIONS.md`). Fourteen records covering the choices
  made so far, from Architectury multiloader (ADR-0001) through to the branch consolidation
  (ADR-0014), each with the alternatives that were rejected.
- **This changelog.**
- **Project foundation**, recovered from two assistant sessions that lost their terminals before
  committing anything (ADR-0014):
  - `README.md` — the full design and reference document: the eight systems, Flux energy, the
    processing chain, research progression, the orbital and planetary layers, the compatibility
    strategy, the verified toolchain and the roadmap.
  - `docs/DESIGN.md` — the earlier standalone design note, kept for history and superseded by the
    README (ADR-0013).
  - Architectury multiloader build — root `build.gradle`, `settings.gradle`, a `gradle.properties`
    holding every version coordinate, and the `common`, `fabric` and `forge` subproject scripts.
    No `neoforge` subproject on 1.20.1; the Forge jar serves both loaders (ADR-0002).
  - `common/.../Grindless.java` — the loader-agnostic entrypoint.
  - `SETUP.ps1` — Windows bootstrap: `MAX_PATH` check, source tree creation, JDK and Gradle
    discovery, Gradle wrapper generation, and `-Commit` / `-MakePrivate` switches.
  - `.gitignore` covering Gradle, Loom/Architectury and IDE output.

### Changed

- `README.md`: systems renumbered to fit fluids in at its logical place, after logistics. *Tools
  and equipment* 5→6, *Resonance, defence and weapons* 6→7, *The futuristic tier* 7→8, *Orbit and
  the planets* 8→9. Contents, tier table, processing chain and block catalogue updated to match.
- `README.md`: the *Progression* section now states that research is necessary but not sufficient
  past T1 — a blueprint grants the right to manufacture, not a crafting recipe (ADR-0017).
- `README.md`: the processing chain gained *the wet line* — fluid-assisted variants that yield more
  and recover byproducts the dry chain loses, with the dry chain never removed.
- `README.md`: the *Current blocker* section became *Environment setup*. It described a broken
  development terminal as the project's blocker and linked a workaround script that was
  deliberately not committed (ADR-0014). The architecture tree and the implementation plan were
  updated to match what is now in the repository.
- `gradle.properties`: added `supported_minecraft_versions` and `neoforge_loader_range`, and
  standardised the Architectury API version on the single key `architectury_api_version`, which
  the subproject scripts referenced under a second name (ADR-0014).
- `build.gradle`: the root build now bundles `LICENSE` into every jar.
- `SETUP.ps1`: removed header comments asserting that the work in the tree was uncommitted, which
  ceased to be true once it was.
- `docs/DESIGN.md`: marked superseded by the README (ADR-0013), and repointed its two dangling
  links — `ENERGY.md` and `ROADMAP.md` were planned splits that were never written — at the README
  sections that hold that content today.

### Not included

- `COMMIT.bat` and `README.new.md` from the recovered sessions. Both existed only to work around
  the broken terminal, and the latter was marked a stale scratch file by its own author
  (ADR-0014).

### Fixed

- The build could not configure at all: Architectury Loom reads a subproject's platform from a
  `loom.platform` property in that subproject's own `gradle.properties`, which neither module had,
  so `forge` was configured as a Fabric project and `dependencies { forge ... }` failed with
  `Could not find method forge()`. Both markers added (ADR-0024).
- `SoundType` was imported from `net.minecraft.sounds`; under Mojang mappings on 1.20.1 it lives in
  `net.minecraft.world.level.block`.
- `git add` failed with `Filename too long`: the checkout is 181 characters deep and the deepest
  source file reaches 268, past the Windows 260-character limit, with `LongPathsEnabled` off and
  no administrator rights available. `core.longpaths` is now set on the repository, and
  `SETUP.ps1` sets it on a fresh clone (ADR-0024).

### Known state

The tree **compiles and packages on both loaders**, but nothing is playable: the registered T0
blocks are inert placeholders with no block entities, menus or behaviour, the material registry is
a stub, and no recipe system exists yet.

The next step is design, not code — `docs/PROCESSES.md`, specifying the items, fluids, recipe graph,
concrete routes and ratios on top of the machine layer that
[`docs/MACHINES.md`](docs/MACHINES.md) now defines.
