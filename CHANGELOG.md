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

- **Bootstrap entry and exit are explicit (ADR-0116).** T0 now names its normal-survival
  vanilla inputs, two-iron mod machinery budget, physical four-Relay-Matrix exit and roughly
  twenty-minute target. The Bootstrap check pins the complete visible Matrix batch.

- **The Atlas exposes physical Bootstrap routes (ADR-0115).** The Process Atlas now lists the
  Research Terminal's Data Core calibration and every authored Grindless shaped recipe alongside
  generated machine processes. The first Relay Matrix batch therefore shows its real ingredients,
  four-item output and physical workstation without adding a solver or a hidden gate.

- **Physical calibration replaces global research permissions (ADR-0114).** The Research
  Terminal now turns a Data Core into a hopper-recoverable Calibrated Data Core in thirty seconds
  at F0. The four-unit Relay Matrix batch consumes that physical output. The world research save,
  client sync, gated-recipe serializer and all runtime permission checks are removed; quests
  follow physical cores and never gate machines.

- **The design-reference hierarchy is explicit (ADR-0113).** GregTech guides industrial chains,
  material forms, machine roles and progression; Factorio and Satisfactory are secondary
  references for readable automation and scale. This records inspiration only, not an upstream
  material import.

- **A recoverable four-unit T1 Relay Matrix hand batch (ADR-0112).** The crafting table combines
  glass, tagged copper, redstone and a Calibrated Data Core into the first four physical
  matrices. It closes the first T0 route without consuming the bootstrap's two iron or requiring
  an impossible T1 machine.

- **The physical Control Matrix foundation.** Five architecture ranges now model frontier and
  retrospective ratings, a higher matrix rating substitutes for a lower one, and T0 has a
  registered T1 Relay Matrix item with persistent rating data, generated placeholder art and
  legible rating/architecture tooltips. Behaviour checks cover architecture bounds, invalid T0
  matrices and substitution. The manual survival batch is the next T0 route change.

- **A mechanical completion gate for every tier (ADR-0108).** Only one tier is active at a
  time. Resources, all four braided routes, machines, power, automation, byproduct sinks,
  progression guidance, failure recovery and an end-to-end survival playthrough must close
  before the next tier opens. Final hero art, animation, polished audio/effects and complete
  localization wait for the project-wide art pass; functional placeholders and mechanical
  legibility do not. Validation uses fast checks plus smokes targeted at changed Minecraft seams,
  with the full historical smoke matrix reserved for integration. T0 Bootstrap is the active
  audit, and BP is now its closure rather than permission to start T1.

- **The T0/F0–T15/F15 progression lattice (ADR-0107), in design.** Technology, physical
  Control Matrix rating and nominal Flux tier now advance together across sixteen named
  tiers. Every frontier braids materials, chemistry, computation/control and energy before
  its matrix, and each later tier grows through more routes, conditions, logistics and
  spatial scale rather than inflated stack counts or timers. Five matrix architectures
  provide better retrospective routes, the Research Terminal becomes a T4 Research Station
  for reusable physical patterns, and arrival at Sagittarius remains victory while
  black-hole exploitation becomes postgame T15.

- **Flow Meter and diagonal transport, in the design.** Two design-only additions to the
  logistics chapter; no code yet, and neither has a build-out row. The **Flow Meter** is one
  module that clamps onto a belt, a pipe or a flux cable and reports a rolling average rate —
  items/min, B/min, FU/t — as a readout and as a logic signal. It answers "how much is getting
  through", where the existing Belt Reader answers "what is on the belt". It adds no latency and
  no buffer, so removing one never changes what a line does. **Belts and pipes also connect
  diagonally** in the horizontal plane: a diagonal step covers √2 blocks, so it takes √2 times as
  long and carries about 71 % of the line's rated rate. Ground speed stays constant, no
  throughput table changes, and junctions stay axis-aligned so the lane model (ADR-0008) needs no
  second geometry.

- **The smoke scenarios run on Windows (ADR-0106).** `tools/smoke-boot.ps1` is the Windows twin
  of `smoke-boot.sh`, and `tools/run-smokes.ps1` / `tools/run-smokes.sh` run the whole list,
  `--only` a few, or `--shard i --of n`. The shell script feeds the server console through a
  FIFO, and a Cygwin FIFO cannot be read by a native Windows `java.exe`: under Git Bash the
  server booted and then every scenario failed at the handshake, which reads as a mod bug and
  is not. The PowerShell twin uses a real Win32 pipe, and copies the datapack through
  extended-length paths so a deep checkout does not trip MAX_PATH (ADR-0024).

### Changed

- **Active documentation names visible components, not obsolete unlocks.** Current machine
  descriptions now say that Assembler routes use their stated physical components. Historical ADRs
  retain the old terminology as an accurate record of the superseded design.

- **Bootstrap smoke now exercises the whole T0 machine seam.** One local server run proves
  Dynamo-to-Extractor power transfer, automated vein output to a chest, hopper-fed physical Data
  Core calibration, hopper extraction of its result and safe recovery of a legacy Advanced Data
  Core. This is evidence for the still-active T0 audit, not a claim that the tier is accepted.

- **Fast shared-world smoke batches are available locally (ADR-0111).**
  `tools/run-smokes-batch.ps1` starts Forge once and exercises the ordinary scenarios in sequence
  for the daily development loop. The provider and state scenarios retain fresh worlds, and the
  existing isolated matrix remains the integration evidence.

- **The runtime Flux ladder now reaches F15.** `FluxTier` preserves every existing nominal
  F0–F9 rate, extends the fourfold sequence through Event Horizon at 8,589,934,592 FU/t, aligns
  all sixteen display names with ADR-0107 and saturates lookup at F15. The energy checks now prove
  every adjacent ratio, the complete tier count and the Forge Energy `int` boundary at F14.

- **BO isolates the spatial prototypes from survival.** The generated graph no longer includes
  the eight Industrial recipes for the Ground Array, links, rocket or station, and the Atlas now
  exposes 107 reachable recipes with 39 Assembler rows. The quest book and field guide stop at
  electronic silicon: eleven tasks across four lines, with no dimension objective. Registrations,
  existing blocks and items, Luna, the Drift, Sagittarius, travel logic, commands and all smoke
  scenarios remain. **BP — Control Matrix foundation is next.**

- **Smoke scenarios are a list, and CI shards them (ADR-0105).** Every slice had been adding a
  `Boot and exercise ...` step to `ci.yml`, each booting a full dedicated server; the merged
  branch reached **39 steps in one sequential job** against `timeout-minutes: 45`. GitHub
  reports a timed-out job as *cancelled* rather than failed, so this never looked like a broken
  build — but every run from `cursor/interstellar-travel-e61d` onward was killed at 45.3 minutes
  and **six pull requests were merged on evidence that never finished**. The list now lives in
  `tools/smoke/scenarios.txt`, and CI is `verify` plus a six-way `smoke` matrix. Adding a
  scenario is a line in that file, never a step in `ci.yml`.

- **Where the work happens is written down (ADR-0106).** Slices are authored remotely as
  stacked pull requests; this Windows desk builds, smokes, fixes and merges them. `AGENTS.md`
  records that a branch is not finished when CI is green, that a tool which fails only on
  Windows is a bug in the tool, and that every `tools/*.sh` needs its `.ps1` twin.

- **The two weekend stacks are one line of history (ADR-0104).** Twenty-seven stacked
  pull requests were built from the same tip of `main` by sessions that could not see
  each other: PRs #25–#39 shipped build-out slices L through Y, PRs #40–#51 shipped the
  modpack expansion BD through BN and then BC, and PR #29 added the agent route map.
  Both large stacks allocated ADR-0088, ADR-0089 and ADR-0090. The expansion stack keeps
  them; the foundation stack's three records are now **ADR-0101, ADR-0102 and ADR-0103**,
  renamed everywhere they are referenced. `docs/DECISIONS.md` is reordered so its sections
  run in index order and every record is separated by `---`; ADR-0087 (the route map)
  fills the one gap, so the index is contiguous from 0001 to 0104.

- **Rows L–Y are shipped, not held.** The expansion stack held L through BB under
  ADR-0088 while the other stack was implementing L through Y. `BUILD-OUT.md`, the README
  implementation plan and *Where the project is now* record what is actually in the tree:
  G–Y and BD–BN plus BC are done, and only **Z–BB** are held.

- **The generated graph is the union of both stacks.** 39 recipes on `main`, plus 64 from
  the foundation stack and 12 from the expansion stack, is **115**. `VerifyRecipes` and
  `VerifyAtlas` assert that number, 47 assembler rows, and the two routes to a steel ingot
  (the electric arc and the caster). Regeneration emits 1366 assets byte-identical to what
  both stacks committed, which is independent evidence the merged registries agree.
### Changed

- **B2 now pairs materials exactly without stack NBT or random output.** The Chemical Washer
  produces one ingot of the next eligible material in stable name order for every eight crushed
  input, preserving the documented 0.25 u trace per raw. Washed crushed is the explicit
  `washed_crushed_materials` form; stale generated assets are removed (ADR-0110).

- **Third-party reuse is allowed when its licence permits it.** Grindless may now copy or adapt
  compatible code, textures, sounds and other material from upstream projects. Every import needs
  its exact source, version, licence, scope and required notices in `THIRD_PARTY_NOTICES.md`;
  upstream terms remain controlling. MIT continues to cover Grindless-authored material unless a
  documented import requires a compatible distribution change (ADR-0109).

### Fixed

- **Foreign-provider smoke expects the live supply line.** The boot still finds 32 materials, and the foreign tin ingot plus raw platinum still step two Grindless items aside. The catalogue is 181 supply items, so the log is `32 materials found; Grindless supplies 179 of its 181 items`. The old expect (`143 of its 145`) was the count from before this stack's forms.

- **A hopper can fill a multi-count process input one item at a time.** `ProcessLookup.accepts`
  required the incoming stack to already hold the whole recipe count, so the first crushed of a
  wash, the first plate of an assembler craft and the first insert of electric-arc steel or zone
  refining were all refused. Insertion now matches the item; the recipe still waits until the
  count is there. Both weekend stacks found this independently.

- **The ADR-0065 index anchor matches the link checker.** The heading keeps the SO₂
  subscript. The checker drops that character when it builds the slug, so the index
  link now uses the slug without it.

- **`VerifyMaterial` no longer fails on Windows for a path separator.** The vanilla-namespace
  assertion compared a relativized path against `tags/blocks/mineable/`, but `relativize`
  yields backslashes on Windows, so the check had been red on every run of `run-checks.ps1`
  since it was written. Nothing was ever shipped into that namespace.

- **The asset generator writes UTF-8 on Windows.** `generate-assets.ps1` called `javac`
  without `-encoding UTF-8`, so Windows read the em dash in the provenance text as cp1252
  and wrote mojibake into `GENERATED.md`. Both generator scripts and both check runners now
  pass the flag.

- **Bootstrap smoke no longer summons the Advanced Data Core onto the hopper.**
  Slice I placed the item at `8 72 8`, one block above the hopper feeding the
  terminal, so the entity vanished before `ADV-CORE-OK`. The core now appears
  at `8 100 8`, same as the Atlas and Wire Mill item smokes.

- **Boot smokes handshake before scenario commands.** GitHub lost the first FIFO lines
  after `Done`, so `PICKAXE-OK hand_crank_dynamo` and `ATLAS-OK` flaked while the same
  commit passed on the other event. `smoke-boot.sh` now waits for a `[Server] SMOKE-READY`
  broadcast. The loot scenario also `forceload`s chunk 0,0 like every other smoke.

- **State smoke no longer races a ticking machine (ADR-0061).** CI placed every status with a
  1s gap between `setblock` and `execute if`, so an empty belt published `idle` over `running`
  and a Crude / Terrestrial Extractor published `starved` / `out_of_band` over the first `idle`.
  Those placements now run as one datapack function in one tick. Belts also actually attach the
  Forge `ITEM_HANDLER` the hopper commit imported.

### Changed

- **The quest book is last, and the next slice is electric-arc steel (ADR-0090).**
  The owner deferred the original quest book until the end of the pack. It now sits
  with the in-game guide, after arrival at the galactic-centre black hole. The urgent
  calendar is intermediate and endgame tiers, more processing lines and materials,
  then megastructures and the Kardashev scales, then original planets and interstellar
  travel, then the black hole. BD — electric-arc steel on the Arc Furnace was
  named next. This change implements it. Rows L–BB stay held. No third-party code
  is copied. Own work stays MIT (ADR-0089).

- **External code may enter with its own license (ADR-0089).** Grindless's own work
  stays MIT. The copyright holder accepts adding an upstream license when a later
  change copies code that requires it (LGPL-3.0 for GregTech CE Unofficial and
  GregTech-Modern). That does not unlock All Rights Reserved assets, and it does
  not resolve BetterQuesting's MIT-versus-All-Rights-Reserved contradiction. No
  third-party code is copied in this change.

- **The project is a modpack-scale progression (ADR-0088).** The early-game work already
  built stays the foundation. The "remove the early-game grind" framing and the T0–T6 cap
  are retired. Many more tiers, more processing lines and more materials follow, with
  Kardashev Type I, II and III as the large milestones and megastructures along the way.
  Planets have unique extractable resources. Space and interstellar play are original.
  The goal is the black hole at the centre of the Milky Way: arriving is the victory, and
  the interior is a finite finale written when the route exists. Endgame generation targets
  the maximum a per-tick `long` can name, `Long.MAX_VALUE` FU/t. Sums, a multi-tick buffer
  at that rate, and the FE `int` bridge cannot express operating there, so the
  representation grows before that content ships. The quest book is an original
  BetterQuesting-style implementation. Slices L–BB are held. That record named BC —
  Original quest book as the next slice. ADR-0090 moves the book to the end.

- **Remaining work is playable slices, not system layers (ADR-0058).** First iron (Thermal
  Generator, Pulverizer, Arc Furnace, `ProcessRecipe` item-first, shared menu, Voltaic gate)
  before fluids, belts, conduits or the multiblock framework. Gaseous T1 byproducts vent until
  tanks exist. CI is marked done. The numbered plan from step 15 is rewritten to match.

### Added

- **Agent route map (ADR-0087).** `docs/AGENT-MAP.md` is a task index: the files to
  open and the invariant each kind of change breaks. The same commit that changes a
  seam updates the map. It does not catalogue function bodies.

- **Flux Exosuit, Network Tap and Exoskeleton Legs (ADR-0103).** Assembler-manufactured (casing, two motors, four plates; Industrial). Four pieces with the harness's protection and two slots each. The harness stays at one slot. The tap and the legs install only into an exosuit. A worn tap pulls up to 32 FU/t from pylon coverage into Flux Cells on the suit. Legs add 0.04 speed and spend 1 FU/t from a cell. No charge, no speed. The suit does not generate. The behaviour graph is 103 recipes; a Grindless-only pack logs 306. CI summons the helmet, the chest, the tap and the legs.

- **Voltaic Harness and Flux Cell (ADR-0102).** Four crafting-table pieces with iron's protection and one module slot each. Vanilla armour still equips. The Flux Cell stores 6,400 FU and is not the drill's fuel. Using it on a harness in the other hand installs it; sneak-use gives it back. Right-clicking a Capacitor Bank or a Flux Transformer inside pylon coverage fills it from that network. Walking through a pylon does not. The suit does not generate. The behaviour graph stays 97 recipes; a Grindless-only pack still logs 300. CI summons all five items.

- **Pattern Scanner, Deconstructor and Matter (ADR-0101).** Assembler-manufactured (casing, two motors, four plates; Industrial). The scanner stores an item id on the world and reports a replication cost walked from item outputs. Matter, a blank hand, and the `grindless:replication_blacklist` tag are refused. A stored pattern is not consumed again. No power leaves the item in hand. The deconstructor spends F1 and one item and gives one Matter. The blacklist does not block a smash, and Matter smashed again is still one Matter. Tag outputs are not expanded. The Replicator block is not in this slice. The behaviour graph is 97 recipes; a Grindless-only pack logs 300. CI places both blocks and summons Matter.

- **Deconstruction Planner (ADR-0086).** Assembler-manufactured (casing, two motors, four plates; Industrial). Right-click two corners. The box uses the same 32-block edge as a blueprint. The mark stays on the item and the tool reports the volume. It does not break blocks and it does not relocate them. The Multitool relocate is unchanged. Drones still wait. The behaviour graph is 95 recipes; a Grindless-only pack logs 298. CI summons the planner.

- **Blueprint Tool (ADR-0085).** Assembler-manufactured (casing, two motors, four plates; Industrial). Right-click two corners. The box may be 32 blocks on an edge and 512 blocks with an item. The Blueprint item keeps the block id and its facing, not the status and not the block entity, so a stamp cannot copy an inventory. Using it places that layout on the clicked face and spends the player's inventory, all or nothing. Creative mode does not spend. An anvil names it. Drones and the network wait. The behaviour graph is 94 recipes; a Grindless-only pack logs 297. CI summons the tool.

- **Flux Drill and Drill Cell (ADR-0084).** Assembler-manufactured. The drill is a casing, two motors and four plates in 20 s. The cell is one copper coil and four plates in 4 s. The drill has no durability: each broken block costs 32 FU, a cell is 3,200 FU, and the drill holds two cells. Sneak-use cycles single, 3×3, vein (32, face-connected) and a horizontal tunnel (3×3, eight deep). It mines at diamond level, drops the block's normal loot, and does not break Grindless blocks. The Multitool still does not mine. Silk and fortune are not in this slice. The behaviour graph is 93 recipes; a Grindless-only pack logs 296. CI summons both items.

- **Signal Cable, Logic Controller and Redstone Interface (ADR-0083).** Assembler-manufactured (casing, two motors, four plates; Industrial). The cable carries one integer one block per tick and is not capped at 15. The controller lets the machine in front run while the inventory behind holds fewer than 500 of one item, and holds it at 500 or above. An empty filter does not hold. The hold is not saved. The interface writes redstone on its front into the cable behind, clipped to 0–15, and emits `min(15, value)` the other way only when no redstone is coming in. The behaviour graph is 91 recipes; a Grindless-only pack logs 294. CI places all three.

- **Pressure Pipe, Electric Pump, Industrial Tank, Fluid Manipulator, Flux Belt, Stack Manipulator and Filter Manipulator (ADR-0082).** Assembler-manufactured (casing, two motors, four plates; Industrial). The pipe and the industrial tank accept fluid up to 1200 °C and 1.0 MPa, so steam and molten metal fit and superheated steam does not. The pipe pushes 200 mB/t including gas and uphill, unpowered. The pump pulls 100 mB/t from behind and spends F1 only while it has fluid to move; it does not summon water. The tank is one block of 64 B and does not merge. The fluid arm moves 1 B/s. The flux belt moves 16 items/s and spends F1 only while it carries items. The stack arm moves 12 items a second. The filter arm is a whitelist of one item. The Clay Conduit, the Basic Tank and the T1 tunnel are unchanged. The behaviour graph is 88 recipes; a Grindless-only pack logs 291. CI places all seven.

- **Solar Array, Boiler and Condenser (ADR-0081).** Assembler-manufactured (casing, two
  motors, four plates; Industrial). The panel makes 32 FU/t while it is day and the
  block above can see the sky, and nothing at night. `1 B water → 1 B steam` in 10 s
  at 150 °C and 0.5 MPa. `1 B steam → 1 B water` in 4 s. The Basic Tank refuses the
  steam. The turbine is not in this slice. The behaviour graph is 81 recipes; a
  Grindless-only pack logs 284. CI places all three.

- **Froth Flotation and Magnetic Separator (ADR-0080).** Assembler-manufactured (casing,
  two motors, four plates; Industrial; no circuit board). `20 crushed + 500 mB surfactant
  → 24 concentrate + 3 tailings` in 80 s. Concentrate reduces like crushed (`b3_r1`).
  Ten tailings reduce to one ingot, so the batch is not a second 2.40. Surfactant is
  `1 carbon + 1 B water → 1 B` on the Chemical Reactor. The magnet sends iron, nickel
  and steel left and holds when that side is full; it does not change the item.
  Concentrate does not roast. The behaviour graph is 76 recipes; a Grindless-only pack
  logs 279. CI places both machines.

- **Induction Furnace and Caster (ADR-0079).** Assembler-manufactured (casing, two motors,
  four plates; Industrial; no circuit board). An ingot mould and a plate mould are four
  iron plates in 4 s. `1 ingot → 144 mB molten` in 8 s at 1000 °C, inert, with no slag.
  `144 mB molten + mould → 1 ingot or 1 plate` in 4 s; the mould is not consumed. Molten
  fluid is emitted at 1000 °C, so a Basic Tank and a clay conduit refuse it. The Arc
  Furnace still makes ingots. The behaviour graph is 67 recipes; a Grindless-only pack
  logs 240. CI places both machines.

- **Fluid Well (ADR-0078).** Assembler-manufactured (casing, two motors, four plates;
  Industrial; no circuit board). Draws F1 and pumps 100 mB/t of ambient water from the
  chunk, with no vanilla source, into a 2 B buffer and out the face it points at.
  Brine, crude hydrocarbon and geothermal are not emitted. No new pipe. The behaviour
  graph is 52 recipes; a Grindless-only pack logs 191. CI charges the well and checks
  that a neighbouring tank receives water.

- **Electrolysis and air (ADR-0077).** Electrolysis Cell and Atmospheric Intake are
  Assembler-manufactured (casing, two motors, four plates; Industrial; no circuit board).
  `2 B water → 2 B hydrogen + 1 B oxygen` in 10 s; oxygen vents into a neighbouring tank.
  `1 B hydrogen` burns 400 ticks, the same as CO. `2 B hydrogen + 1 B oxygen → 2 B water`
  in 8 s on the Chemical Reactor. The intake drinks free air and stores `2 B oxygen` in
  10 s. Nitrogen and argon are not emitted. Chlor-alkali, the Fluid Well, Haber and roast
  oxygen still wait. The behaviour graph is 51 recipes; a Grindless-only pack logs 190.
  CI places the cell, a water tank and the intake.

- **Washer and B2 (ADR-0076).** Chemical Washer is Assembler-manufactured (casing, two
  motors, four plates; Industrial; no circuit board). Washed crushed is a supplied form.
  `8 crushed + 2 B water → 8 washed crushed + 1 crushed of the next washable metal` in
  20 s. That metal's `b1_r1` is the named sink. Washed feed also reduces (`b2_r1`) and
  roasts. Flotation and electrolysis still wait. The behaviour graph is 46 recipes;
  a Grindless-only pack logs 185. CI places the washer, hoppers crushed iron, and
  stands a water tank.

- **Original quest book and in-game guide (ADR-0100).** Two T0 handhelds. The quest
  book is lines, tasks, dependencies and rewards for the route already built, from the
  Multitool through Voltaic, the contact process, the Arc Furnace metals, the Ground
  Array, Luna, the Drift and the sealed chamber. A claim watches an item in the
  inventory, a blueprint the world has unlocked, or the dimension the player is
  standing in. It does not consume that evidence and it does not gate a machine.
  Claims are per player. The field guide is the same route, read as pages, with the
  chamber last. Rewards are existing items. Nothing follows this slice. BK stays
  named and not started. Rows L–BB stay held. No third-party code or assets are
  copied. Own work stays MIT (ADR-0089).

- **Arrival at the galactic centre (ADR-0099).** Riding the station from the Drift
  lands in a sealed chamber, `grindless:sagittarius`. The mass is unbreakable horizon
  shell, sixteen blocks tall, with no vein. The carve is a 7 by 4 by 7 room, one
  arrival mark, one berth, and a 3 by 3 shaft so the ride home has air. Leaving the
  Drift and leaving the chamber draw nothing. Leaving the chamber returns to the
  berth saved on the way to the Drift. The Starward Link stays registered and still
  does not move a player. No new link and no new recipe. The next slice is the
  original quest book (BC), and it is not started. No third-party code or assets
  are copied. Own work stays MIT (ADR-0089).

- **Supraluminal station (ADR-0098).** Interstellar trips are a ride. A station,
  Assembler-built under Industrial from one machine casing, one motor and two array
  casings, stands on a berth (one starward link and four steel plates). It climbs to
  the build ceiling. That ceiling is the arrival: the Drift on the way out, the berth
  they left on the way home. Leaving any world but the Drift spends 6,553,600 FU.
  Leaving the Drift does not. The Starward Link stays registered and no longer moves
  a player. The next slice is arrival at the galactic centre (BN), and it is not
  started. No third link, no black-hole interior, no further planet. No third-party
  code or assets are copied. Own work stays MIT (ADR-0089).

- **Teleportation orbs are named and not started (BK).** Just before rocket ascent,
  a later slice is an alternate route of a magical material, left unnamed, that ends
  in orbs. Shift-right-click sets coordinates and dimension. Right-click teleports.
  An orb can sit on a pedestal, so a return to a planet does not need another rocket.
  Draconic Evolution may inspire that slice. Its current code is All Rights Reserved
  (Don't Be a Jerk) and its assets are CC BY-NC-SA 4.0, so neither is copied. This
  change does not build the route. The quest book stays last. The station stays next.

- **Rocket ascent (ADR-0097).** Local trips are flights. A survey rocket, Assembler-built
  under Industrial from one machine casing, one motor and two steel plates, stands on a
  launch pad (one casing and four steel plates). It climbs to the build ceiling. A landing
  map then offers the home world and Luna. Leaving home spends 102,400 FU. Leaving Luna
  does not. The Lunar Link and the Starward Link stay, marked as placeholders. The next
  slice is the supraluminal station (BM), and it is not started. Arrival at the
  galactic centre is that ride, not a link. No third-party code or assets are copied.
  Own work stays MIT (ADR-0089).

- **The Drift (ADR-0096).** The first interstellar hop is one deck between the home star
  and the galactic centre. It has no ore: an extractor there finds nothing. A Starward
  Link, Assembler-built under Industrial from one lunar link and four array casings,
  draws 6,553,600 FU from a covering pylon and sends the player. The return pad does not
  draw again. Arrival at the black hole is named next and is not started. No vacuum
  damage, no Horizon Gate, no rocket, no further planet, no black-hole interior.

- **Luna (ADR-0095).** The first original planet is a regolith world with no ore. Every
  chunk's vein is helium-3, a reagent the Crude and Terrestrial Extractors emit only
  there. A Lunar Link, Assembler-built under Industrial from two array casings and one
  machine casing, draws 102,400 FU from a covering pylon and sends the player. The
  return pad does not draw again. Interstellar travel is named next and is not started.
  No vacuum damage, no Horizon Gate, no rocket, no black hole.

- **Ground Array (ADR-0094).** The first megastructure is a fixed 3×3: one controller
  and eight casings. A complete ring under a pylon adds 6,553,600 FU to that network,
  ten seconds of MK3 throughput, as storage. It does not generate and it does not
  project a supply cube. Both blocks are Assembler recipes under Industrial: four
  refractory bricks and one steel ingot make a casing; one machine casing, four steel
  plates and four refractory bricks make the controller. Kardashev Type I, II and III
  stay milestones. BI was named next and is now in (ADR-0095).

- **Zone refining (ADR-0093).** The Arc Furnace turns 10 metallurgical silicon into
  7 electronic silicon in 600 s at 1420 °C. That is the graph's 0.70 yield and 60 s
  per unit, as an integer batch. The ±5 °C inert band stays with the Induction Furnace,
  because that band does not admit the 1500 °C reducing hold. No new block. Siemens,
  the boule and wafers wait. BH was named next and is now in (ADR-0094).

- **Metallurgical silicon (ADR-0092).** The Arc Furnace turns 1 silica and 2 carbon into
  1 metallurgical silicon and 2 B of carbon monoxide in 14 s at 1900 °C in a reducing
  atmosphere. Silica is the tag over sand and nether quartz; no silica item is
  registered. The furnace's 1500 °C hold stays, inside tolerance and outside the
  optimal zone, so the line runs slower and the yield stays one. No new block.
  Zone refining was named next and is now in (ADR-0093).

- **Refractory brick (ADR-0091).** The Arc Furnace turns 1 slag into 1 refractory brick
  in 20 s at 1400 °C. That is the ceramics row's time and temperature, as one unit in
  and one unit out. The furnace's 1500 °C hold stays inside the band. No new block.
  Alumina, silica, the Kiln, aggregate and road fill wait. Metallurgical silicon was
  named next and is now in (ADR-0092).

- **Electric-arc steel (ADR-0090).** The Arc Furnace turns 10 iron ingots and 1 carbon
  into 10 steel ingots in 140 s at 1600 °C. That is the graph's 0.1 carbon per ingot
  and 14 s per ingot, as an integer batch. The furnace's 1500 °C hold stays inside the
  band. No new block. Oxygen blow, direct reduction, the washer and the Autoclave wait.
  Refractory brick was named next and is now in (ADR-0091).

- **Contact process (ADR-0075).** Chemical Reactor is Assembler-manufactured (casing, two
  motors, four plates; Industrial; no circuit board). `1 B SO₂ → 1 B SO₃` in 6 s on vanadia
  with held air; `1 B SO₃ + 0.2 B water → 1 B sulfuric acid` in 4 s, water from a neighbouring
  tank. Vanadia pellet is Voltaic (iron oxide + four bricks). Named spend is pickle:
  `1 iron ingot + 0.1 B acid → 1 iron plate` in 4 s. R2 yield 1.15 and the washer still wait.
  CI places the reactor, hoppers vanadia, and stands SO₂ and water tanks.

- **Wire Mill, wire and motor (ADR-0074).** T2 mill is Assembler-manufactured (casing, two
  coils, four plates; Industrial; no circuit board). Wire is a supplied form
  (`grindless:wires/<m>`): 1 ingot → 2 wire in 8 s F1. Mill coil is 2 copper wire → 1
  copper coil, no die. Motor is a reagent: casing + two coils + one rod in 10 s.
  Acid still waits. CI places the mill and hoppers an ingot in.

- **T2 gate (ADR-0073).** Industrial is the second blueprint on the same terminal. Advanced
  Data Core is Voltaic-gated (one core + four plates). `assemble/pylon_mk2` names that
  blueprint. Wire Mill is slice J.

- **Sorter (ADR-0072).** Inline filter. Sneak-click left or right; matching items leave that
  face and hold if it is full. Unmatched continue. Voltaic craft. `VerifyBelt` dumps the
  route. Logic Controller still waits.

- **Belt junctions (ADR-0071).** Merger (three inlets, round-robin), Tunnel Belt (pair, skip
  1–5 empty blocks) and Overflow Gate (front, then clockwise). Voltaic crafts. `VerifyBelt`
  dumps pick/range/route. Sorter is slice H.

- **Autonomous build-out (ADR-0070).** `docs/BUILD-OUT.md` is the remaining schedule. Slice F
  still waits for the Arc Reactor. Sifter, turret and armour stay parked until their rows.

- **Multitool rotates and relocates (ADR-0069).** Right-click turns a Grindless block.
  Sneak-click picks it up with `BlockStateTag` and `BlockEntityTag` so contents do not spill.
  Still not a pickaxe. `VerifyMultitool` dumps the facing cycle.

- **Horizon Gates and exotic fallback worlds (ADR-0068).** Design only. T6 dialed ring pair
  kills the interplanetary commute; it is not a mining dimension. People cheap, bulk still
  on the Mass Driver. Fallback adds Thalassa and Helios when no space mod is installed.
  Not started.

- **Modular armour line and the Arc Reactor pair (ADR-0067).** Design only. T1–T4 chassis;
  T3 / F3 factory Arc Reactor and miniature suit core share a manufactured Arc Cell line.
  Direct FU, not steam. Not the Arc Furnace, not fission, not fusion. Not started.

- **Process Atlas stub (ADR-0066).** Handheld lists the live `ProcessLookup` graph:
  family, I/O, catalysts, named conditions, duration and FU/t. `AtlasLogic` has no
  Minecraft imports; `VerifyAtlas` dumps it. No ratio solver. JEI still waits.
  Voltaic-gated craft. CI summons the item.

- **Kiln / R2 — roast then reduce (ADR-0065).** T1 Kiln (8 s F1, 700 °C oxidising)
  turns raw or crushed into oxide and vents 1 B SO₂. The named sink is the Basic
  Tank; the Thermal Generator does not burn it. Oxide is `grindless:oxides/<m>`
  (12 supply items). Arc Furnace R2 is oxide + carbon → ingot + slag in 10 s, no
  CO. Yield 1.15, sulfuric acid and Slice F stay later. `VerifyRecipes` dumps
  roast. CI places the Kiln and hoppers raw iron in.

- **Slice E — energy spanning (ADR-0064).** Flux Conduit is a T1 hand item that right-clicks
  two pylons into a manual link. No length limit; upkeep is `ceil(distance / 8)` FU/t.
  Capacitor Bank adds 102 400 FU to the covering network and projects no supply cube.
  Flux Transformer is a covered F1 tap, not a pylon. T2 chassis cores stay ADR-0025.
  `VerifyNetwork` covers merge, split, upkeep, banks and the F0/F1 cap. CI places the
  new blocks.

- **Slice D — factory builds factory (ADR-0017, ADR-0063).** Press (4 s F1, die catalyst)
  and Assembler (20 s F1) as real consumers on the shared process menu. Machine Casing
  is 4 plates + 2 rods. T1 coil is copper + coil die; Wire Mill stays T2. Pylon MK2 has
  no crafting-table recipe — the Assembler is the only source. Voltaic-gated crafts for
  Press, four dies, casing and Assembler. `VerifyRecipes` dumps press and assembler
  recipes. CI places both machines; a hopper cannot steal a die.

- **Slice C — first fluids (ADR-0058, ADR-0062).** `FluidState` is volume + temperature +
  pressure in millibuckets. Clay Conduit (unpowered, ambient liquid, gravity or level),
  Hand Pump (vanilla water source, does not drain it) and Basic Tank (16 B, refuses hot).
  Wet B1 is the same crushed yield plus 0.5 B water. Arc Furnace CO pushes into a tank or
  vents; captured CO burns 400 ticks in the Thermal Generator. Forge `FLUID_HANDLER` maps
  without T/P. `VerifyFluid` drives the arithmetic. CI pumps water into a tank and burns
  a bucket of CO.

- **Slice B — first factory (ADR-0058, ADR-0060).** Conveyor Belt (unpowered, 8 items/s, two
  lanes of positions, not entities), Splitter (filter-plus-priority, sneak-click a face),
  Crude Manipulator (one item a second, unpowered), Terrestrial Extractor (F1, five seconds
  per unit, surveyed chunks only) and the Prospector's Scanner (3×3 survey + overlay).
  Voltaic-gated crafts. `VerifyBelt` drives the lanes. CI places the line and hoppers a
  cobble onto a belt.

- **Slice A — first iron (ADR-0058, ADR-0059).** Generated B0×R1 / B1×R1 `ProcessRecipe` graph
  (item I/O, vented CO, `#grindless:carbon`, slag). Thermal Generator (F1, furnace fuel),
  Pulverizer and Arc Furnace as real consumers with one shared menu. Voltaic-gated crafts for
  those three machines and Pylon MK1 (`grindless:gated_shaped` + client unlock cache).
  `VerifyRecipes` dumps the graph. CI places the new machines and hoppers a coal into the
  generator.

- **T0 bootstrap recipes (ADR-0056).** Crafting-table JSON for the Multitool (cobble and sticks),
  Hand Crank Dynamo and Crude Extractor (one iron each), Research Terminal (planks, glass, cobble)
  and Data Core (cobble and redstone). Iron is `#forge:ingots/iron`; stone is
  `#minecraft:stone_crafting_materials`. `BootstrapRecipes` and `VerifyBootstrap` check the files
  against that budget.

- **Research Terminal (ADR-0057).** Real consumer: 8 FU/t, thirty seconds, one Data Core unlocks
  Voltaic. World-scoped `ResearchData`. Right-click to insert or take the core; hoppers may insert
  but not extract. Status idle / running / starved / blocked.   Voltaic now gates the Slice A crafts (ADR-0059).

- **Crude Extractor (ADR-0053).** T0 consumer: 8 FU/t, twenty seconds per unit of the chunk vein,
  scaled by richness, depletion and brownout. Power from a covering pylon or from an adjacent
  dynamo. Output is the pack's preferred raw (or ore), into a one-slot buffer that hoppers pull
  from and that auto-pushes into neighbouring inventories. Status is driven by the real loop
  (idle / running / blocked / starved). `VerifyExtractor` covers the numbers; CI places the block
  entity and the pylon tower.

- **Three-block pylons with factory-scale coverage (ADR-0054).** A pylon occupies three blocks of
  height (base plus two shafts). JSON models cannot exceed 32 units, so each third is its own
  model. Supply cubes are 48 / 80 / 128 with link ranges 64 / 112 / 192. Shafts have no item and
  drop nothing; breaking one breaks the pylon.

- **Out-of-band visuals.** Extractor and terminal gain `out_of_band` textures (magenta) and a
  shared witch-mote effect. Nothing sets that status yet; the art is in place for process
  machines.

- **The Multitool does not mine (ADR-0055).** Machines stay pickaxe-only.

- **Machine states, per-machine art and effects (ADR-0052).** `status` (idle, running, blocked,
  starved, out_of_band) and `facing` block-state properties; the hand-crank dynamo and flux pylons
  publish their real status through a debounce, the extractor and terminal are shells whose status
  nothing sets yet. Each machine has its own element model and generated textures per status
  (extractor, terminal with tilted screen, dynamo with crank, three pylon tiers). `MachineEffects`
  spawns particles and sounds per geometry and status; five mono Ogg sounds ship in `sounds.json`
  with subtitles (source WAVs in `tools/audio`, `tools/convert-audio.sh`). `VerifyAssets` checks
  every state, rotation and sound; `tools/smoke/states.commands` places all 53 states in CI.
  `pack.mcmeta` moved to the `forge` module.

- **Block assets** (ADR-0051). Blockstates, block and item models, self-drop loot tables and the
  `minecraft:mineable/pickaxe` entries for all six blocks, plus placeholder sprites for the
  Multitool and Data Core and a model for the Machine Casing item, generated from
  `BlockCatalogue`. `VerifyAssets` fails if any registered block or item lacks them or if any
  model names a missing texture. `tools/smoke-boot.sh` gains `SMOKE_COMMANDS` and
  `SMOKE_EXPECT_FILE`; CI places every block and checks its tag and drop.

- **Material compatibility (ADR-0050).** Grindless supplies 120 material items (tin, lead,
  silver, nickel, zinc, aluminium, titanium, tungsten, platinum, steel, and forms vanilla lacks
  for iron, copper and gold) and registers them regardless of other mods, but they are active
  only where no other mod or vanilla fills the slot. After each tag load `MaterialRegistry` scans
  the pack, hides redundant supply items from the creative tab, and exposes a deterministic
  output choice (`Unifier`: vanilla, named namespaces, other mods, Grindless). Recipes name
  materials through `MaterialTags`, never item IDs. Every shipped tag is additive.
- Platinum, as the first purely exotic material, with raw, crushed, dust, nugget, ingot, plate,
  rod, bolt, gear and ring forms.
- Item models, tags and lang keys for the supply items, generated from `SupplyCatalogue`;
  `tools/generate-assets.sh`.
- `VerifyMaterial` checks, including a lint that fails any shipped recipe naming a material item
  instead of a tag. `SMOKE_DATAPACK` and `SMOKE_EXPECT` in `tools/smoke-boot.sh`, with a datapack
  that stands in for another mod; CI runs it.

- **A boot smoke test and CI** (ADR-0049). `tools/smoke-boot.sh` starts the headless Forge
  dedicated server and fails unless it reaches `Done`; `.github/workflows/ci.yml` runs the build,
  the behaviour checks and the smoke test on Linux. `tools/run-checks.sh` is the Linux port of
  `tools/run-checks.ps1`. Until now "verified" meant a green build and 309 passing checks, and the
  mod could not start: neither gate instantiates Forge. The smoke test fails on the previous
  `ModCreativeTabs` with the original `Registry Object not present` crash and passes on the fix.
- **The asset generator** (`tools/assetgen`, `tools/generate-assets.ps1`) and **ADR-0048**. Art was
  the project's largest unexamined risk, so it was investigated rather than assumed, and three
  findings changed the plan. There is no Python and no Node on the toolchain. The JDK alone is
  enough — `javax.imageio` writes PNG, `javax.sound.sampled` writes WAV — and is better anyway,
  since it uses the toolchain the project already requires. And **audio turned out to be
  generatable**, which contradicted the initial assessment: it had been written off as impossible
  before being tried.
  Sprites are described as **height fields and lit by one shared pass** rather than drawn as
  coloured pixels. The first attempt picked a tone per pixel and produced flat shapes; shading
  those convincingly would have meant hand-placing highlights on every form and redoing it on
  every change. Describing geometry and lighting it afterwards inverts that — relief, bevels,
  specular highlights and the dark outline all come from one pass, so improving the lighting lifts
  all 108 textures at once. Nine forms across twelve materials.
  Looping sounds are built only from harmonics of their own loop frequency, so the waveform is
  periodic over exactly the loop length and the join is seamless *by construction* rather than by
  fading. A click once per second is maddening on a machine a player stands beside for hours.
  **ADR-0048's real output is the list of what this will not produce**: hero item sprites, complex
  models, entity animation, music and anything recorded. A future session can see those are known
  gaps with no owner rather than assuming art is handled because a generator exists.
- `AGENTS.md` now documents the failure that cost **four sessions**: every command failing with
  "Permission denied and could not request permission from user" means the session is in the app's
  **Automatic** work mode, which cannot prompt for approval. The fix is to switch to Interactive.
  Each lost session concluded the terminal had broken; it had not.

- **Resource Genesis** (`common/.../vein/`, `material/`) — step 14, where raw material comes from.
  `VeinGenerator` derives a chunk's vein from the world seed and coordinates, so an unexplored
  chunk costs zero bytes and the same seed always produces the same map (ADR-0009); `ChunkVein` is
  what a chunk would produce; `VeinData` persists only what has actually been taken out.
  The derivation **version is pinned per world** and saved with the extraction data. Changing the
  algorithm later would otherwise silently rewrite every surveyed-but-unbuilt chunk in every
  existing save — a player's planned copper outpost quietly becoming tin. Old worlds keep deriving
  under the version they were made with.
  `Material` and `MaterialForm` are the first half of the runtime tag scan (ADR-0004) and the form
  axis of the catalogue matrix (ADR-0032). A material carries only what a scan can honestly know,
  and degrades gracefully: one with an ore but no dust simply skips the pulverizing step.
- **Veins are deliberately long-lived, and extractors can be deepened** (ADR-0047). This closes a
  failure mode the design had but did not address: if veins ran down quickly, "expand outward"
  would stop meaning exploration and start meaning *abandon, relocate, rebuild the same layout,
  repeat* — which is re-solving a solved problem, and therefore grind by this project's own
  definition (ADR-0021). The system built to delete mining would have reinvented it.
  Three changes. A chunk now holds roughly **forty hours** of continuous T1 extraction. The decay
  curve is **gentle early and steep late** rather than linear — a quarter of the reserve gone is
  still 96 % of the original rate and half is 83 % — because linear decay is noticeable from the
  first hour and makes a player feel permanently on a clock. And the floor is **30 %**, not a
  token amount, so a worked outpost stays a real contributor rather than becoming a monument.
  (The checks caught the documentation here claiming 87 % where the curve actually gives 82.5 %.
  The maths was right and the prose was aspirational — which is exactly the kind of drift that
  turns a design document into fiction, so the numbers in `VerifyVein` are now the source of
  truth and the prose quotes them.)
  **Richness scales reserve as well as rate**, which makes a vein's lifetime independent of its
  richness. A rich chunk is a find rather than a countdown, and the player is never asked to weigh
  "rich but short" against "poor but long" — a false choice that adds arithmetic without adding a
  decision.
- **The Deep Bore upgrade** (`MACHINES.md`, eleventh in the upgrade table) reaches further into the
  same chunk, raising the floor to 45 % with one and 60 % with two, capped at 75 %. A depleting
  outpost therefore has two answers instead of one: deepen it, or found another, depending on
  whether you are short of power or short of territory. The cap is what keeps expansion eventually
  necessary — a chunk must never become infinite. It buys **nothing at a fresh vein**, which is
  what makes it a decision rather than something installed reflexively on day one.
- **`tools/checks/VerifyVein.java`** — the numbers above are claims, so they are checked: that the
  derivation is deterministic and version-stamped, that richness stays in band across a hundred
  chunks, that rare materials really are rare and common ones common, that **the map does not
  stripe along an axis** (the giveaway of a weak hash, and the thing a player notices first), that
  the rate never increases as a vein is worked, and that every generated vein really lasts tens of
  hours rather than only the base constant doing so.
- One decision record, ADR-0047.

- **The Flux Pylon, and machines drawing from their network** — step 13b, which turns the network
  layer into something a player can build. Three pylon blocks (MK1–MK3) share **one block entity
  type**, because the tier lives on the block rather than on the block entity: a single type
  serves all three, and the tier cannot drift from what the player sees since the block *is* the
  tier.
  Registration is the pylon's entire job, and both halves matter. It joins the network when
  placed and leaves when broken — deregistering **before** `super.onRemove` destroys the block
  entity, since a network left holding a pylon that no longer exists keeps its capacity and its
  topology, so a player breaks a pylon and watches nothing change. Chunk unload deliberately does
  *not* deregister: an unloaded pylon is still part of its network, and doing otherwise would make
  topology depend on where a player happens to be standing (ADR-0007).
- **`MachineBlockEntity.requestPower` / `drawPower`** — the two-phase brownout from the machine's
  side. A machine declares its intended draw, the network resolves one satisfaction fraction for
  everyone, and only then does anyone draw. A machine on an overloaded network gets a reduced
  share and runs slowly rather than stalling, which is the legible failure the design asks for.
- **`MachineBlockEntity.onFirstTick`** — an explicit hook for setup that needs a loaded level and
  loaded neighbours. It replaces overloading `updateSubscriptions` for the purpose, and exists
  because vanilla offers no load-completed hook `common/` may use — `onLoad` is a Forge addition,
  as the compiler established at step 12c.
- The Hand Crank Dynamo now **pushes into its Flux Network first**, falling back to adjacent
  blocks. A dynamo inside a pylon's supply area feeds the whole grid rather than only the block it
  touches, which is the point of area distribution.
- The network tick is registered through Architectury's `TickEvent.SERVER_LEVEL_POST`, so it stays
  in `common/`. Running after block entities means the satisfaction a machine sees was computed
  from the previous tick's demand — a deliberate one-tick lag, and the thing that makes the
  brownout proportional rather than first-come-first-served.

- **The Flux Network** (`common/.../network/`) — step 13, and the system that makes power
  wireless. `PylonTier` is the README's MK1–MK3 table; `FluxNetwork` is a set of linked pylons
  sharing one pooled buffer; `FluxNetworkData` is the per-dimension `SavedData` holding them all
  (ADR-0007), with the merge and split logic; `PylonIndex` is the spatial index that makes
  "which network powers this block?" cheap.
  **The index is a chunk bucket rather than a tree** (ADR-0046). Each pylon is filed under every
  chunk its supply cube touches, so a lookup hashes one chunk and exact-tests the few pylons
  filed there — cost tracks local density, not world size. The bucket is deliberately coarser
  than the cube, so the exact per-axis test still runs on every candidate; skipping it is an
  over-coverage bug that would mostly work.
  **Brownouts resolve in two phases**: every machine registers its intended draw, the network
  computes one satisfaction fraction, then everyone draws at that fraction. Serving machines as
  they tick would mean whoever ticks first runs at full speed while the rest stop dead — the
  individual starvation the README rejects, where a random subset of the base stops with no
  indication why. Two phases is what makes "everything is visibly sluggish" true rather than
  aspirational.
  **Energy follows capacity through merges and splits.** A fragment with a third of the pylons
  keeps a third of the charge; merging carries energy across. Any other rule either destroys power
  or lets a player manufacture it by breaking and replacing a pylon in a loop.
- **`tools/checks/VerifyNetwork.java`** — covers the parts that are easy to get subtly wrong:
  cube coverage at edges and corners, link range governed by the *shorter* of two tiers, a pylon
  bridging two networks merging them, removing a middle pylon splitting a chain, energy conserved
  across both, demand accumulating before resolution, and a shrinking network shedding what it can
  no longer hold.
- One decision record, ADR-0046.

### Fixed

- **A cranked dynamo left extractors looking starved while they actually worked.** It pushed 40 FU
  every five ticks, so the network went empty in between and `StatusDebounce` never saw twenty
  consecutive running ticks. It now pushes 8 FU every tick, matching F0, so one extractor lights
  cyan and two share a brownout instead of a lie.

- **Blocks rendered as missing-texture cubes and dropped nothing.** None of the six blocks had a
  model or loot table, and machines require the correct tool, so breaking one yielded no item.

- **The generator produced items vanilla already has.** Textures for iron ingot, gold ingot,
  gold nugget, iron nugget and copper ingot (and a crushed steel, which has no ore) are removed;
  nothing registered them.

- **Capability listeners leaked.** `NeighbourCache` registered a new invalidation listener on every
  re-lookup and never removed it, so a neighbour that kept invalidating accumulated listeners
  without bound. The listener is now registered once per cached capability and removed on
  `invalidate()`; `FluxPlatform.onInvalidated` returns the unsubscribe handle. The Hand-Crank
  Dynamo also releases its caches in `setRemoved()`.
- **The Hand-Crank Dynamo polled every pushed tick when nobody was listening.** Fruitless pushes
  now back off exponentially up to 20 ticks (`PushBackoff`) and reset on crank or neighbour change.
- **Machine upgrades and process conditions were lost on save.** `UpgradeSet` and `ConditionState`
  are now written to and read from NBT. Machines saved before this change load with no upgrades and
  ambient conditions.
- **Flux Network extraction ignored pylon throughput.** `FluxNetwork.extract` could drain a full
  buffer in one call; it is now capped per tick at the pylons' combined throughput, restored by
  `resolveTick()`.
- **The mod crashed during construction.** `ModCreativeTabs.register()` called `get()` on registry
  objects before the registry was populated, so Forge refused to create the mod instance with
  `Registry Object not present: grindless:multitool`. The tab now receives lazy suppliers. The
  three Flux Pylons were also missing from the creative tab and are added.
- **A broken pylon stayed in its network after a world reload.** `PylonBlockEntity` mirrored its
  membership in a transient `registered` flag that only a first-tick hook restored, and pylons have
  no ticker, so after a reload `deregister()` returned early and the network kept a ghost pylon,
  with its capacity and topology. Membership now lives only in `FluxNetworkData`. Confirmed in a
  real server: place, save, restart, break — the pylon stayed in the saved network before the fix
  and is gone after it. `PylonBlock.onPlace` also ignores a state-only change, since re-adding a
  member removes it first and re-runs the split flood fill.
- `gradlew` was committed without its executable bit, so it failed with `Permission denied` on
  Linux, macOS and CI. Invisible on Windows.
- `tools/run-checks.ps1` exited non-zero even when every suite passed, because a PowerShell script
  with no explicit `exit` inherits whatever `$LASTEXITCODE` the last native command left behind.
  It is used as a pre-commit gate, so a runner that reports failure on success is worse than no
  runner at all: it trains you to ignore it.

- **Machine energy buffers are exposed to other mods** (`MachineEnergyCapability`, ADR-0045) —
  step 12d, which was an open architectural question rather than just remaining work. Forge
  provides an energy capability through `getCapability`, which has to be overridden on the block
  entity, and machine block entities live in `common/`, which may not import Forge. The answer is
  `AttachCapabilitiesEvent<BlockEntity>`: Forge fires it for every block entity as it is created,
  so a listener in `forge/` can attach a capability to a `common/` class without that class
  knowing Forge exists. `common/` stays loader-clean and the Forge-facing surface is one file.
  **The invalidation listener is the load-bearing half, not the attach call.** The chain was read
  in Forge's source rather than assumed: `setRemoved()` → `invalidateCaps()` →
  `CapabilityDispatcher.invalidate()` → the `Runnable`s registered by
  `AttachCapabilitiesEvent.addListener` → our `LazyOptional.invalidate()` → the listener
  `NeighbourCache` registered on it (ADR-0044). Attaching without it compiles, works in testing,
  and leaks stale references in play.
- **The Hand Crank Dynamo's charge is now its Flux buffer**, not a separate field. Making the
  capability real exposed the bug immediately: exposing the buffer would have published an *empty
  load* — insertable, never read, and holding none of the power the dynamo had actually made.
  Generators now override the buffer's direction (no external insertion, extraction at the rated
  tier) so the object a machine fills is the object the world pulls from, and the dynamo needs no
  persistence of its own because the base class already saves that buffer. Only what a neighbour
  actually accepted is removed, rather than taking the allowance up front and refunding the
  remainder, which is the version that loses power on a rounding edge.
  Unifying the two surfaced a second bug, and a quiet one: the push read its allowance through
  `extract()`, which is **rate-limited to one tick's output**. A push throttled to every fifth tick
  was therefore capped at a fifth of what it should move, running the dynamo at 1.6 FU/t instead of
  8 with nothing visibly wrong. A buffer's rate limit governs what an *external* puller may take in
  one operation; a machine's own output path implements its rating itself.
- **`tools/checks/VerifyEnergy.java`** — behaviour checks for the energy layer, written because the
  rate-limit bug above is exactly the kind that compiles, runs, and is wrong by a factor of five
  without anything looking broken. Covers both buffer shapes (generator and load), the 1:1 FE
  conversion, saturation at the `int` boundary so a full buffer can never read as a debt
  (ADR-0037), EU rounding down so a conversion chain cannot create energy, and the smooth
  under-volting curve (ADR-0038).
- **`MachineBlockEntity`** — step 12c, and the point where the separate layers become a machine. It
  carries a Flux buffer, a chassis mark and its upgrades, the condition state the machine is
  holding, the container contract and the tick subscriptions, and its `serverTick` does nothing but
  run the subscription list — so an idle machine costs an emptiness check (ADR-0042).
  Two invariants are documented in place because both are landmines. The energy buffer is **final
  and never replaced**: Forge hands it out as a capability, so anything holding it would keep
  pointing at a discarded object if it were ever swapped. And `ratedTier()` **must return a
  constant**, because it is called from the constructor to size that buffer and therefore runs
  before the subclass constructor body.
  Writing this exposed a design error worth recording: `setChassisMark` was rebuilding the energy
  buffer, but the buffer is sized from the machine's own rating, which a mark does not change — a
  mark widens the *condition envelope* (ADR-0027). The rebuild was pure churn that would also have
  produced stale capabilities, so it is gone.
  Subscriptions are initialised on the **first tick** rather than on load. `onLoad()` is the
  obvious hook and is a **Forge addition that vanilla does not declare**, so `common` cannot use
  it — the compiler caught the leak that `@ExpectPlatform` exists to prevent. Deferring is also
  what GregTech CEu recommends independently, since neighbours are not reliably available while a
  chunk is still loading.
- **The Hand Crank Dynamo** — the first machine that does something, and the first power in the
  game at F0, 8 FU/t. That is enough to run the Crude Extractor and nothing else, which makes the
  player's first real decision what to power.
  It is also the smallest honest demonstration of the tick model: cranking stores charge and
  subscribes the push loop, and the loop **unsubscribes itself the moment the charge runs out**, so
  a dynamo nobody has cranked costs nothing. Pushing is throttled on the machine's position-derived
  offset rather than every tick. `HandCrankDynamoBlock` wires it up, including the
  neighbour-changed path that ADR-0044 requires for a neighbour *appearing*, which capability
  invalidation cannot signal.

- **The container contract** (`common/.../container/`) — step 12b. `ContainerConfig` implements the
  controls the README promises on *every* buffer in the mod: filters, buffer target, capacity
  limit, auto-void with its mode and threshold, per-face I/O, and independent insert and extract
  priority. It holds configuration only and never contents, which is what lets item and fluid
  containers share it.
  **Auto-void is built defensively** (ADR-0018), and the behaviour is now enforced by code rather
  than promised in prose: off by default, it **trims and never empties** — voiding applies only
  above the threshold — and it **refuses to discard protected materials** regardless of settings,
  including in `EVERYTHING` mode. A voiding container reports that it is voiding, so an alarm can
  be built on it.
- **`ChangeListeners`** — the other half of the tick-subscription model. Machines can only avoid
  ticking because something tells them when the world changed underneath them (ADR-0042); without
  notification a machine would have to poll to discover whether polling was needed. Configuration
  changes notify too, not just contents: closing a face can make tick work unnecessary, so a
  machine listening only to contents would silently stop.
- **`NeighbourCache`** (ADR-0044) — caches the adjacent block's energy buffer so a machine pushing
  power is not doing a chunk-and-map lookup every tick. The naive version of this is a correctness
  bug rather than an optimisation: a cached reference survives the neighbour being broken, which is
  the classic *"my machine stopped working until I broke and replaced it"*. The cache is therefore
  cleared from Forge's own `LazyOptional.addListener` invalidation, through a new `@ExpectPlatform`
  hook. **Two triggers are needed, not one** — invalidation covers a capability being revoked, but
  cannot cover a block *appearing* where there was none, so the neighbour-changed event clears it
  as well. Absence is cached too, so a machine facing a wall does not re-ask the wall forever.
- One decision record, ADR-0044.
- **`tools/checks/` and `tools/run-checks.ps1`** — the behaviour checks written over the last three
  steps, committed rather than thrown away. They have already caught two real bugs: an `EnumMap`
  constructor that throws on an empty source map, which is the path taken by installing the very
  first upgrade, and a band-efficiency expectation that was simply wrong. The runner checks the
  classes Gradle produced rather than recompiling them, so what is verified is what would ship.
  Two Windows details are commented in place because both fail in ways that say nothing useful:
  the classpath needs the whole Gradle module cache, since `Direction.<clinit>` drags in most of
  Minecraft's bootstrap; and it is far past the command-line length limit, so it goes in an
  argfile — where a backslash is an escape character, so paths must be written with forward
  slashes or javac reports every package as missing.
  This is not a substitute for a real test suite, which is step 26's job. It is the difference
  between arithmetic that has been run and arithmetic that has only been read.

- **The machine layer** (`common/.../machine/`) — step 12. `ChassisMark` is MK I–V, whose defining
  property is that it **widens the condition envelope** (ADR-0027) rather than adding a speed
  number, so a mark unlocks recipes. `widen()` interpolates between a machine's narrow and full
  envelopes, which matters at scale: thirty machines at five marks would otherwise be a hundred and
  fifty envelopes to hand-write and keep consistent. The interpolation fractions are not evenly
  spaced — they were chosen to reproduce the Arc Furnace ladder in `MACHINES.md` (1800 → 3500 °C),
  whose steps shorten near the machine's physical ceiling.
  `MachineUpgrade` is the ten upgrades, each carrying what it **buys** and what it **spends**
  (ADR-0028), with the three mutually exclusive pairs declared once and mirrored so a pair can
  never be half-declared. `UpgradeSet` enforces slot counts and exclusions **in the data model
  rather than the UI**, since a screen is only one of several ways an upgrade gets installed.
  Rejections explain themselves — "cannot be combined with Speed" rather than a bare `false`.
- **The tick-subscription model** (`TickSubscriptions`, `TickSubscription`, `TickOffset`) — the
  substrate that keeps a large base playable, and the answer to "will this drop us to 5 FPS".
  Machines **do not tick by default** (ADR-0042): work is subscribed when something makes it
  necessary and unsubscribed the moment it is not, so an idle machine's tick is an emptiness check.
  Ten thousand machines ticking at thirty microseconds each is six times the entire tick budget
  spent on machines doing nothing, and that cannot be fixed by making the work faster.
  `TickOffset` spreads periodic work across ticks using a position-derived hash. The naive
  `gameTime % 20 == 0` synchronises every machine in the world onto the same tick: average load
  drops twentyfold and the worst tick does not drop at all, which is what players actually feel.
  The hash is mixed rather than raw because block coordinates are highly regular and players build
  in **rows** — the exact case a weak hash would resynchronise.
- **The condition system** (`common/.../process/`) — step 11, and the seam the whole machine layer
  rests on. `ProcessConditions` is the `conditions` half of
  `inputs + conditions + time -> outputs` (ADR-0020); `ConditionEnvelope` is what a machine *can*
  hold and `ConditionState` what it *is* holding, kept apart because the Process Atlas asks the
  first ("which machines could run this?") and a running machine asks the second ("how well am I
  doing it?").
  `ConditionBand` gives a written condition three zones — optimal at full speed, a tolerance zone
  tapering to a quarter speed, and outright failure beyond it. Bands are relative (±15 %) by
  default and absolute where a process is unusually sensitive, as in zone refining's `T 1420 ±5`.
  The relative factory **refuses an optimum of zero** rather than silently producing a zero-width
  band no machine could ever satisfy.
  **The omission rule is implemented as actual absence**: a dimension a recipe does not name is not
  stored at all, so it carries no requirement and a machine is never rejected for being unable to
  pull a vacuum nobody asked for. Storing it as a full range would match identically but could not
  answer *how many* conditions a recipe names — which is what keeps a six-dimensional space from
  showing six dials per recipe. No recipe names more than three.
- **`MachineEnvelopes`** encodes the envelope table from `docs/MACHINES.md` directly, so the
  condition model is checked against the design rather than assumed to fit it. The Haber synthesis
  at 20 MPa lands inside the Autoclave and outside the Chemical Reactor, which is the
  condition-envelope idea working exactly as specified.
- Two decision records, ADR-0040 and ADR-0041, and two more, ADR-0042 and ADR-0043, recording the
  performance model. ADR-0043 decides the recipe lookup strategy **before** step 15 implements it —
  indexed rather than scanned, with a cached last recipe and a negative result that sticks — so the
  session that writes recipe generation does not have to rediscover it. Both records cite what
  GregTech CEu Modern and Mekanism actually do: GTCEu stores recipes in an ingredient trie and
  checks `lastRecipe` before searching, while Mekanism uses typed input caches. Two mature
  implementations converging independently is the strongest available evidence that a linear scan
  does not survive at this scale.

### Changed

- **`SETUP.ps1 -Commit` is usable again.** It is the documented fallback for when the in-app
  terminal stops working, and it had two faults that made it worse than useless in exactly that
  situation. Its commit message was **hard-coded to the day-one foundation commit**, so every later
  commit made with it would have been labelled with a description of work it did not contain — and
  a generic message on a specific change actively misleads whoever reads the history later. It now
  takes `-Message` and refuses to commit without one. Its push also had no credentials and so
  always failed; it now uses the transient `GH_TOKEN` auth header documented in ADR-0024, applied
  with `-c` so no token is ever written to `.git/config` or to any tracked file.
- **Out-of-band processing costs speed, never yield** (ADR-0040). `MACHINES.md` offered three
  penalties — reduced yield, longer time, extra byproducts — and yield is the one that cannot be
  chosen: every ratio in `PROCESSES.md` is quoted per unit of primary input (ADR-0034), so a
  silent yield penalty would make all of them conditional on tuning and leave the Atlas's line
  solver untrustworthy. It is also the worse failure to notice, since a slow factory shows up in a
  throughput readout while a quietly lossy one does not. Efficiency is the **minimum** across the
  named dimensions rather than their product, so two dimensions slightly off do not compound into a
  crawl and there is always one identifiable cause.
- **Condition checks return a named fault, not a boolean** (ADR-0041). Faults carry a direction —
  `TOO_COLD` and `TOO_HOT` are distinct because they have different fixes and different in-world
  tells — and `OUTSIDE_ENVELOPE` is separate from all of them, because "this machine never can" is
  a chassis upgrade while "it currently is not" is a dial. A degraded process names the single
  dimension limiting it, so a machine can say "running at 62 % — limited by Temperature" instead of
  reporting an unexplained number.
- Step 11's `ProcessRecipe` type moved to step 15, where recipe generation already lives. A recipe
  type cannot be bound to `RecipeType` and `RecipeSerializer` before the container it matches
  against (step 12) and the fluid stacks half its ingredients are (step 17) exist; writing it now
  would have meant guessing both. The condition system itself is complete and standalone.

- **Forge 1.20.1 is now the only build target** (ADR-0039). The `fabric` subproject, its sources,
  its loader metadata and the TeamReborn Energy dependency are removed.
  The reason is concrete rather than a change of heart: writing the Flux energy bridge meant writing
  it twice against unrelated models — Forge capabilities with `int` amounts, and Team Reborn Energy
  with `long` amounts and a transaction system needing a `SnapshotParticipant` to be correct under
  rollback — and `:fabric:build` was already failing with an opaque `Failed to remap 57 mods`. That
  is double the implementation, review and failure surface in exchange for an audience the mod
  cannot serve yet, since nothing is playable. `:forge:build` now goes green in about 50 seconds
  where the two-platform build failed outright.
  **NeoForge 1.20.1 users are unaffected**: ADR-0002 already established that they load this jar
  unchanged, so "Forge only" still means two loaders in practice.
  The `common/` + `forge/` split and Architectury are **kept**. The split costs nothing now that it
  exists, `@ExpectPlatform` still keeps loader-specific code out of `common/`, and collapsing the
  modules would mean rewriting every registry class today to buy nothing — while making a future
  port a rewrite instead of a build change.
- `SETUP.ps1` created the platform bridge directories as `forge\energy`, which is backwards:
  Architectury resolves `<pkg>.Foo` to `<pkg>.forge.FooImpl`, so an implementation lives in a
  `forge` package *beneath* the one it implements. The wrong layout compiles and then fails at
  runtime, so it is now created correctly and the reason is commented in place.
- ADR-0001 is marked superseded by ADR-0039 — its multiloader *goal* is withdrawn while the
  structure it chose survives. ADR-0006 is narrowed rather than superseded: the 1:1 FU↔FE rate and
  the hand-written bridge stand exactly as decided, and only the Fabric half is withdrawn.

### Added

- **The Flux energy layer** (`common/.../energy/`) — step 10 of the implementation plan, and the
  project's first real systems code rather than registration.
  `FluxStorage` is the energy contract, `SimpleFluxStorage` the ordinary implementation with
  independent insert and extract limits — so one class covers a generator, a machine buffer and a
  battery with no subclassing — and `FluxConversion` the FE and EU conversions. `FluxPlatform` is an
  `@ExpectPlatform` stub whose Forge implementation resolves `ForgeCapabilities.ENERGY`, letting
  Grindless push and pull power from any other mod's machines losslessly.
  Amounts are **`long`, not `int`** (ADR-0037). F9 is 2 097 152 FU/t and a top-tier buffer holds
  more than `Integer.MAX_VALUE`, which an `int` does not overflow politely — it wraps, so a full
  buffer reads as a debt. Conversion to FE saturates instead of wrapping, and callers treat a
  converted amount as a request rather than a promise, so a clamp can never create or destroy
  energy.
  `FluxTier` collapses what had become **two** power scales into one (ADR-0038): the README's
  voltage names LV–IV and `MACHINES.md`'s F0–F9 ladder were the same numbers written twice, so the
  voltage names are now aliases on F1–F5 rather than a second enum that would eventually disagree.
  Under-volting is a smooth curve — half throughput per tier of deficit — so a power shortfall is
  never a wall.
  Verified in the built jar rather than assumed: the `@ExpectPlatform` stub really is rewritten to
  call `FluxPlatformImpl`, which is the failure that would otherwise appear only at runtime.
- Three decision records, ADR-0037 to ADR-0039.
- **`tools/check-links.ps1`** — validates every relative link and anchor in the documentation and
  checks the ADR index against its actual sections. It is committed because it kept being written
  from scratch each session: it has already caught three broken links in one session and two here.
  Two details in it are load-bearing and commented as such — files are read as **UTF-8 explicitly**,
  or the em dashes in headings mangle every slug and produce a flood of phantom failures, and the
  file list comes from `git ls-files` rather than a tree walk, because recursing the working
  directory descends into `build/` and `.gradle/loom-cache` and dies on MAX_PATH (ADR-0024).
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
