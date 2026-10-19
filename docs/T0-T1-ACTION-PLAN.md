# GRINDLESS — T0 → T1 ACTION PLAN

**2026-10-19 | Planning handoff, not tier acceptance.**
Baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Read [audit evidence](T0-AUDIT.md) for source locations, confidence and validation limits.
The [executable queue](../plan/process-t0-t1-1.md) gives task IDs, files and completion gates.

## Current state

**T0 is implemented but not ready to freeze.** Its actual endpoint is five F1 Relay Matrices
after assisted extraction and physical F0 calibration, from the documented vanilla-material
entry. It is not a first Assembler, a wet factory or a multiblock. The build and all 30 existing
behavior suites pass on Windows; the exact baseline also has successful historical CI.
Both local targeted smokes, `states` and `bootstrap`, passed. No client or no-command
survival playthrough was performed in this audit.

Much of the first powered factory already exists, including renewable glass, Press, Assembler
and the physical F2 Matrix output. Do not schedule those as new systems. T2 machine consumers
are not yet consistently physically rated. The current fixture has 112 processes, 41 Assembler
routes, 145 Atlas routes and twenty quests across nine lines. Old documentation counts are not
acceptance evidence.

| Readiness | Assessment |
| --- | --- |
| COMPLETE | T0 authored recipe/data floor, calibration policy, Matrix model, registration and generated resource contracts. Preserve them. |
| NEEDS VALIDATION | Survival feel and recovery, client information, actual pack integration, persistence/relocation, visual and sound feedback. |
| NEEDS POLISH | Truncated information, guidance consistency, input accessibility, placeholder presentation and audio repetition. |
| BLOCKING T0 | Output recovery defect, incorrect quest inventory counts, unresolved mandatory discoverability failures, and missing recorded survival acceptance. |
| NON-BLOCKING | Final hero assets/audio/locales, Atlas solver, broad configuration, multiblock kernel and later prototypes. |

## T0 blockers

1. **A01 / P1: the Crude Extractor can remain blocked after adjacent storage is emptied.**
   Add the failing runtime recovery scenario, then repair drain/wake behavior without changing
   yield, cycle timing or save IDs. Verify exact item/extraction accounting and reload.
2. **A02 / P2: offhand inventory is counted twice by the Quest Book.**
   Count each slot once and prove that three/four Matrices cannot satisfy the five-item objective.
   Claims remain non-consuming, once per player and non-gating.
3. **Acceptance evidence is absent.** Complete the campaign below on a pinned candidate; do not
   substitute injected-NBT Bootstrap smoke, quest completion or historical CI for survival.
4. **A05 / conditional P1: required information must actually be readable.** Run M01/M04/M07.
   If the Matrix ingredients, Terminal controls or recovery action cannot be discovered in the
   supported client, fix that before T0 sign-off. Final aesthetic polish is not this gate.

## T0 polish

Make the Crank/Extractor/Terminal interactions and faults explicit in existing tooltips/guide
surfaces; expose complete Atlas inputs/outputs/conditions without a new solver. Reconcile current
counts, five-Matrix text and the two-casing Assembler diagram. Improve keyboard navigation,
selected/claimed indicators and readable layouts where the manual campaign identifies friction.
Move or clearly label the early Terrestrial Extractor objective so it does not consume one of
the five seed controllers before the Assembler can replace it (A09).

Keep generated art and the five-event sound vocabulary. Evaluate silhouettes, state contrast and
many-machine audio before commissioning replacements. Final models, animation, bespoke SFX and
full translation stay deferred under ADR-0108. An unreadable fault or inaudible essential cue is
functional work, not deferred art.

## Manual testing campaign

Use a **fresh normal-survival world in the pinned client-pack candidate**, no creative inventory,
`/give`, time changes, recipe unlock commands or NBT edits. Keep the existing world for recovery
tests and a backup before migration experiments. Record mod/pack version, commit, Java/Forge,
seed, coordinates/dimension, difficulty, GUI scale and resource packs. Quest rewards are allowed
in the ordinary experience, but record them; also verify the recipe route without depending on
rewarded Data Cores, fuel or iron. Free guide/book delivery is not manufactured material.

Start the T0 timing clock at ADR-0116's **vanilla-material entry**, not at world creation.
Separately record time spent acquiring vanilla redstone/copper/tools. With no rewards the required
mod crafts total 18 stone-crafting-material items, 3 sticks, 7 planks, 3 glass, 3 redstone,
2 copper and 2 iron, excluding table/tools/furnace/fuel. The five-Matrix craft itself uses only
two glass, two copper, two redstone and one Calibrated Data Core.

Severity: **P1** prevents completion, loses/duplicates resources or breaks a required factory
interaction; **P2** misleads or adds avoidable repeated work; **P3** cosmetic. "Conditional"
means block closure only when essential information or the core route is affected.

| ID / sequence | Do | Expect | Failure / severity | Blocks T0? | Capture |
| --- | --- | --- | --- | --- | --- |
| M01 - arrive and orient | Open the delivered book/guide and find Crank, Extractor, Terminal, Core and Matrix in JEI before consulting external docs. | A discoverable route with real ingredients and a five-Matrix exit. | Missing route, cut-off required ingredient or unexplained interaction, P1. | Yes | Screenshots of the missing page/recipe and GUI scale. |
| M02 - enter Bootstrap | Gather the stated vanilla inputs; craft Multitool, Crank and Extractor. Keep a material ledger. | Exactly two iron spent on these mod machines, not on hidden mandatory components. | Extra unexplained metal gate or unavailable ingredient, P1. | Yes | Inventory before/after and recipe screenshots. |
| M03 - first assisted output | Put the Crank next to the Extractor and a chest beside it. Crank and wait for actual output. | Visible work, a chunk-derived product, understandable charge depletion. | No output, unexplained placement restriction or misleading status, P1. | Yes | 30-60 second video, layout and output identity. |
| M04 - learn states | Let charge run out, recharge, rotate and relocate with the Multitool. | Fault/recovery is understandable without reading code; rotation and pickup are distinct. | State flicker conceals work or controls cannot be discovered, P1/P2. | Conditional | Video including sounds and held tool. |
| M05 - storage pressure | Fill the destination, allow the internal slot to fill, then remove chest items without breaking the machine. | Automatic recovery without loss, duplication or hidden hopper requirement. | Stuck output (A01), lost/extra item, P1. | Yes | Before/after inventories, short video and server log. |
| M06 - independent seed | Move to another chunk and obtain one assisted output there. Do not require it to be iron. | The Matrix route does not depend on one lucky vein or seed. | Mandatory material unavailable with no visible recovery path, P1. | Yes | Seed, chunk positions, product IDs. |
| M07 - physical calibration | Craft Core/Terminal, power it, insert one Core, interrupt power, resume, then retrieve empty-handed. | Approximately 30 powered seconds at F0; one physical calibrated output; no world permission. | Lost input/progress, double output or undiscoverable retrieval, P1. | Yes | Video of insertion, interruption and retrieval. |
| M08 - persistence | Save/reload during extraction and calibration; unload/reload the chunk. | Inventory, work, charge and vein accounting remain coherent; no free completed batch. | Reset/loss/duplication or permanently sleeping machine, P1. | Yes | Backup plus before/after inventories and logs. |
| M09 - relocation and ordinary break | Wrench-pick up loaded machines, replace them, then test ordinary breaking on a backed-up branch of the world. | Relocation preserves state once; breaking returns stored items once under the documented contract. | Missing inventory, duplicate drops or reset vein exploitation, P1. | Yes | Inventory ledger and video. |
| M10 - the exit | Craft the first Matrix batch once. Move it among inventory/offhand; try claiming with three, four and five. | Five physical F1 Matrices; accurate counts; insufficient counts never claim. | A02, wrong batch/rating or reward duplication, P1/P2. | Yes | Craft result, tooltips and count screenshots. |
| M11 - survival pacing | Record total Bootstrap time, number of cranks, waits, corrections and external lookups. | Roughly twenty minutes from the documented entry; no repeated compulsory hand batch. | Sustained confusion/repetition or dead end, P1/P2; a small timing overrun alone is not failure. | Product judgment | Timestamped diary with bottleneck, not just total time. |
| M12 - readable and listenable | Repeat active/starved/blocked states at two supported GUI scales, with several machines and with audio muted. | Legible names/states; essential information is not sound-only or color-only; no painful repetitive mix. | Essential cues disappear, P1; rough art/mix, P3. | Conditional | Screenshots, resource-pack settings and short audio/video. |
| M13 - death and second player | Die/recover normally; rejoin. If multiplayer is supported, repeat claims with another player and test claimed-area interactions. | Recoverable progression; claims persist per player; no duplicated rewards or cross-player permission. | Lost progression/items, claim bypass or protection mismatch, P1. | Yes for advertised mode | Player-specific steps and server/client logs. |
| M14 - optional automation proof | In a separate post-exit validation segment, use hoppers to feed/extract the Terminal. Do not charge their extra iron to T0's budget. | Input cannot be stolen before calibration; output leaves once; refill works. | Automation violates its contract, P1. | Yes for the machine contract | Layout and before/after counts. |
| M15 - first powered room | Only after the exit, allocate the five seed Matrices to Thermal Generator, Pulverizer, Arc Furnace, Press and Assembler. Delay Portal/Kiln/Terrestrial purchases. | No second manual Matrix batch is needed for these five machines. | Guide diverts the seed controllers into an avoidable restart, P1. | T1 gate; record T0 handoff confusion | Controller/material ledger. |
| M16 - form and assemble | Make dies, plates, rods, gears, coils, two casings, then the Assembler. Automate another five-Matrix batch. | Exact inputs, reusable dies, physical output; no quest permission. | Missing part route, manual-only repetition or wrong outputs, P1. | T1 gate | Actual route and inventory counts. |
| M17 - recurrent supply | Run cobblestone-to-sand-to-glass and a charcoal-fed power line; distinguish block generation from automatic harvesting/replanting. | Every claimed automatic feed actually reaches a machine unattended. | A cobble generator/tree source is called automated while a player still mines/replants indefinitely, P1/P2. | T1 gate | Five-minute observation and layout. |
| M18 - real factory pressure | Add a second consumer, brownouts, full outputs, water arriving last and recipe swaps. | Slower work, conservative resources, automatic restart; no carried work finishing an unrelated recipe. | A03 or loss/duplication, P1. | T1 gate | Stepwise inventories, fluid amounts, recipe and energy state. |
| M19 - byproduct route | For used roast/reduction recipes, inspect gas capture/venting, slag destination and sink availability. | Named reachable sinks; optional venting is explicit; no forced void chore. | Required stream has no reachable sink or blocks silently, P1. | T1 gate | JEI/Atlas recipe, tank contents and sink. |
| M20 - optional building world | After automated Matrix replacement, craft Portal and test return from two entrances/players, reload and full inventory. | Per-player correct return; flat/noon world; ordinary non-free surveyed extraction. | Stranding, overwriting or free-resource shortcut, P1. | Not core T0; blocks shipping this feature as supported | Coordinates, dimension and return evidence. |
| M21 - physical T1 exit | Assemble F2 from F1 Matrix + casing + four glass + Flux Cell. Try lower/higher/malformed ratings in a separate developer test. | F2 output persists and viewers agree; T2 consumers require the intended physical rating once implemented. | A04, invisible gate or misleading rating, P1. | T1/T2 gate | Input/output tooltips and matching recipes. |

**Failure report format:** `M-ID | commit/pack | seed/dimension/position | expected | actual |
minimal steps | severity | inventory delta | screenshot/video/log | reproducible after reload?`.
For an item-loss or duplication failure, preserve the world and stop that branch of the campaign.
Report the first failing prerequisite before downstream symptoms. A fix creates a new candidate:
rerun the affected test and its downstream route, rather than silently mixing evidence from builds.

## T0 freeze criteria

T0 closes only when A01/A02 are fixed; M01-M14's applicable mandatory checks pass; essential
information is readable; and a fresh no-command entry-to-five-Matrix run is recorded with its
candidate SHA, elapsed phases and recovery evidence. Record accepted deviations explicitly.
Run build, all checks, links and targeted changed-seam smokes; use the full isolated historical
matrix for integration. Update BUILD-OUT/README/changelog in the acceptance commit. This document
does not perform that acceptance.

| Freeze after acceptance | Preserve | Not frozen yet |
| --- | --- | --- |
| Save/resource identity | Registry IDs, physical Matrix NBT meaning/defaults, saved inventory layout and vein/accounting identity; migrations for changes. | Any untested legacy migration and new slot layout. |
| Loader boundary | Common stays loader-neutral; Forge adapters own capabilities; invalidate cached neighbors and exposed capabilities. | Claims integration and every external fluid/item provider. |
| Process semantics | Named inputs/outputs/catalysts, no invisible permission, condition penalties affect time not yield, explicit byproduct policy. | Process scheduler/cache/wake/transaction implementation until A03 is resolved. |
| Bootstrap route | Two-iron machine spend, physical calibration and recoverable five-Matrix seed contract once the player run accepts it. | Final rates, durations, prices, reward balance and larger renewable chains. |
| Reusable foundations | Flux `long` values/FE clamp, deterministic vein derivation, indexed material/recipe snapshots, lane-data transport. | Higher-tier overflow/capacity/UI scaling and per-family rated power. |
| Asset/audio pipeline | Catalogue -> generated resources, stable event IDs, required names/models, provenance and mono Ogg delivery. | Final art style details, textures, loudness, event variants and external generator provider. |
| Guidance | One truthful interpretation of the live route; quests observe, never authorize machines. | Exact screen layout, text lengths, Atlas solver and Matrix viewer representation. |

Freeze behavior and compatibility, not every Java signature forever. T1 can add families/data
through existing seams; changing frozen behavior requires a focused ADR, migration and regression.

## T1 GREEN tasks

These are preparation and validation, not premature tier activation:

- **TASK-001/002:** lock the baseline/evidence template and add failure-first backpressure and
  inventory-count regressions. The player can test while these are authored.
- **TASK-005:** add semantic data checks: unique IDs, nonempty tags/output providers, input/catalyst
  slot capacity, physical rating/viewer parity, byproduct sink reachability and prohibited prototype
  outputs. Keep exact fixture totals as sentinels, not as the only correctness test.
- **TASK-006:** make the audio conversion and shared-world smoke runners genuinely cross-platform;
  replace whole-cache test classpaths with an explicit reproducible dependency classpath. Preserve
  current pass/fail semantics and retain isolated integration smokes.
- **TASK-007:** document/generate an asset contact sheet and per-state/audio review matrix using the
  existing catalogue. Define deterministic conversion/provenance checks. Do not commission final art.
- **TASK-008:** reconcile current docs and map the already-existing T1 route and missing T2 consumers.
  Tests and tooling can merge with unchanged gameplay; proposed recipe balance cannot.

## T1 YELLOW tasks

- Repair A03 behind focused regressions, one transition per checkpoint; do not change balance
  while the human is measuring it. Keep changes off the pinned manual candidate until retest.
- Specify and test minimum versus exact Matrix rating semantics across runtime matching, NBT,
  Atlas, JEI and quest evidence before wiring consumers.
- Improve complete-detail rendering and input accessibility using existing screens. Avoid a new
  viewer architecture or data copy.
- Add concrete settings only with unchanged defaults and tests. Treat live balance controls and
  migrations as separate reviewed changes, not a green generic config framework.
- Extract a shared helper only after two real affected implementations and their regressions
  establish the same invariant. Do not impose a common base class on every block.

## T1 RED tasks

Do not finalize T1 duration/material ratios, new mandatory ceramic/control intermediates, renewable
metal/redstone/wood automation dependencies, reward economy or the final F2 consumer recipe graph
until their prerequisites and relevant gameplay evidence are accepted.

Do not build the T3 multiblock kernel/hatches/drones, AE2 integration, space progression,
Atlas rate solver or a new processing framework during T0 testing. Do not delete registered
prototypes or old save IDs as cleanup. Do not import final third-party/AI assets without
redistribution terms and provenance. A service name is not a license.

## Architecture work before T1

Highest leverage: prove **wake-up, recipe identity and output conservation** once in the common
executor, then reuse those guarantees. Next, add a graph generation/reload invalidation contract
and a shared lossless recipe presentation contract. Keep native graph construction rather than
introducing a second JSON/JEI recipe catalogue.

The current long `ProcessGraph` and duplicate catalogue/JSON assertions will become maintenance
costs, but splitting files alone does not improve correctness. Add semantic contracts first; split
by established process family only when a real slice touches it. Similarly, make recipe/machine
power configurable by an actual family when F2 consumers arrive, not by creating unused F15 shells.

## Recommended execution order

1. **Pin candidate A** for the human; preserve a world backup and the baseline ledger. Begin M01-M04.
   In parallel, the developer authors A01/A02 failing regressions and GREEN tooling/data checks.
2. **Fix A01/A02 in a small branch/checkpoint.** Produce candidate B. Rerun M03-M10 and the natural
   T0 route on B; do not claim candidate A's measurements validate changed behavior.
3. **While the player finishes M11-M14**, run semantic checks, docs and portability work without
   changing the candidate's balance. Investigate A03 independently with automated fixtures.
4. **Accept T0 explicitly** only after the freeze gate. Make BP complete and T1 active in one
   evidence-bearing commit; until then BQ content remains blocked.
5. **Activate the existing T1 route**, repair A03, agree rating semantics, and then add the first
   real F2 consumer. Run M15-M21. Expand one playable route at a time.

One developer integration owner avoids collisions in `ProcessGraph`, `BlockCatalogue`, language
JSON and ADR numbering. Independent work may use separate branches, but no multi-agent workflow
or new session is required. Human testing and green preparation are the primary parallel lanes.

**Risk-based testing:** data-only variants of a proven family need graph/provider/slot/yield/viewer
checks, not a fresh server boot for every material. New registration, menu/capability wiring,
persistence, conditions, scheduler transitions or migration require a targeted boot interaction.
Shared-world batches accelerate diagnosis; they do not replace isolation at merge. Retain
`states` for registration changes and the full isolated matrix at integration. Do not spend the
human's time repeating arithmetic a check can prove.

## First work queue for the PC session

Open AGENTS, CHANGELOG, BUILD-OUT, this plan and the evidence register. Start **TASK-001**, then
**TASK-002** and **TASK-003**; begin M01 in parallel. Next take **TASK-005/006/008** while gameplay
feedback arrives. **TASK-004** fixes only the reproduced T0 blockers. **TASK-009** is the acceptance
commit, not a coding placeholder. Only after it may **TASK-010** change the active T1 route.

Never start by reimplementing Press/Assembler/renewable glass/F2 output: they are already present.
Never interpret an empty human report, green CI or a claimed quest as acceptance.

## Decisions needed from me

- **Pacing acceptance:** is the roughly twenty-minute target acceptable in the recorded run, and
  which repeated actions actually feel like grind? Use phase timings before changing recipes.
- **Supported release profile:** accept the pinned client pack first (recommended), or also require
  a separately demonstrated standalone no-JEI experience? The standalone jar must still load;
  recipe discoverability expectations differ.
- **Optional Factory World:** should an explicitly optional destination remain a prerequisite for
  claiming the F2 quest? It currently is, although it does not gate the physical craft.

No decision is needed to fix item accounting, add regressions or preserve the existing T0 gate.

## Local targeted-smoke outcome

**Both passed on 2026-10-19:** the runner reported `ALL 2 SMOKE SCENARIOS PASSED`.
Bootstrap matched `TERMINAL-OK`, `CORE-IN`, `CALIBRATED-OUT`, `LEGACY-OUT`, `DYNAMO-OK`,
`EXTRACTOR-OK` and `EXTRACTED-OUT`. Logs are local build artifacts at
`forge\run\smoke-states.log` and `forge\run\smoke-bootstrap.log`, not committed source.
These tests do not reproduce A01/A02 or substitute for the human campaign.
