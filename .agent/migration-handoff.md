# Clean-context coordination transfer

Updated: 2026-10-17. Product state: **IMPLEMENTATION IN PROGRESS**.
The containing commit preserves source/assets WIP; it is not a C3 or T0 acceptance claim.
The fresh kickoff supplies its exact SHA and session identity after they exist.
Latest published pre-preservation commit: `d292dab005c8f8a321061d9885dd12b8351dd69c`.

Read with [AGENTS](../AGENTS.md), [the active handoff](handoff.md),
[the reloadable plan](work-plan.json) and [pending checks](pending-checks.json).

## Migration and execution contract

The owner stopped the loaded session and requested a genuinely clean session carrying the
information and planning, with Astra leading/orchestrating and work performed by subagents.
This supersedes the original charter's primary-coder role, not its product vision or safeguards.

The retiring coordinator preserves/reviews the handoff and publishes all existing WIP.
The successor starts in a **new isolated worktree** based on that exact feature checkpoint.
Do not use `fork_session`, replay the whole chat, or switch a branch in the main checkout.
Original request documents are transferred as staged attachments, available on demand.
The local environment bootstrap is private; do not put personal paths or credentials here.

**Receipt correction:** two originals arrived as staged attachments; the T0 charter arrived
through its authorized legacy local reference. All three hashes were verified and all three
originals were added through the native Library tool. The [active handoff](handoff.md) records
the receiving coordinator and checkpoint. This corrects the intended attachment modality above;
it does not claim that three staged attachments were copied.

Confirm the received commit, new branch, attachment fingerprints and understood open work.
Reload tasks from `work-plan.json`; the old session's SQLite task table is not transferred.
Receipt lifts the migration pause only, not product/manual acceptance gates. Continue through
bounded workers without waiting for a newly invented human documentation-approval gate.

- Astra owns architecture, product/Tiers, sustainability/production graphs, machine families,
  standard and assembled multiblocks, migrations, integration and final review.
- Delegate application implementation and test execution. Sol 6.1 Max handles normal coding;
  Luna handles simple/repetitive work. Complex design stays with Astra.
- Use few workers, each with a pinned input, outcome, owned files and stop condition.
  Serialize shared files and publication. Worker reports include changed files, actual evidence,
  failed/interrupted attempts and remaining risks. Escalate architectural decisions.
- Astra may investigate, design, review, evaluate evidence, maintain handoffs and integrate Git.
  Missing worker capabilities are reported, not an excuse to revert to solo application coding.
- Preserve prior sessions/worktrees, the renderer child and ignored JDK/cache/build/smoke evidence.
  A clean tree does not authorize deleting unique work or archiving an open-PR session.

## Existing PR and single-publisher ownership

Draft [PR54](https://github.com/Ezequiel24123z/grindless-earlygame/pull/54) has head
`ezequiel24123z-tier-0-rework` and base `ezequiel24123z-agent-ready-t0-foundation`,
draft [PR53](https://github.com/Ezequiel24123z/grindless-earlygame/pull/53).
Neither is accepted or authorized for an immediate merge.

The successor's app-managed branch is different and does not inherit tracked PR identity.
Keep PR54; edit it explicitly by repository/number. Do not call `create_pull_request` for this
continuation, replace/retarget it, squash, force-push or rewrite history.
The new Astra coordinator is the sole continuation integrator/publisher. Workers do not
independently push to PR54; the retiring session is quiescent after preservation publication.

Before publishing reviewed commits, read the live PR/base/head and verify head ancestry.
Only fast-forward PR54's existing remote head; publish the new feature branch as appropriate.
Stop and reconcile unexpected remote movement. Never switch/write the old or main checkout.
Distinct future slices may use deliberately scoped PRs later; migration does not create one.

Old Agent merge was disabled successfully (`enabled=false`, `applied_enabled=false`).
Its saved `merge_pr` action is not running automation. No attached session automation or event
subscription was returned. The asset worker is cancelled; smoke port 25599 was closed.
Do not reactivate old automation. Merge authority remains conditional on required automated
checks, review and observed manual acceptance; drafts stay drafts while those are missing.

## Preserved milestones and invariants

| Checkpoint | Meaning |
| --- | --- |
| `c62de724632cce158988ddce62d4669619398f13` | Foundation preserved by fast-forward; main untouched |
| `9aea30e` | Active T0 workshop contract, ADR-0129 |
| `0838e88` | Exact comma-separated smoke selection and invalid-selection rejection |
| `0aad62a2ccc3b659f34970e4efb142c89667ab70` | W1 ceramic workshop |
| `ee9ae09fb0b4c57a7fdd01b9747e70f092ef65ba` | W2 partial checkpoint; actual water recovery initially failed |
| `40d5260`, `a533888` | Dashboard renderer integrated from `e2c2a0a`, `0cea296` |
| `d292dab005c8f8a321061d9885dd12b8351dd69c` | ADR-0132 actual fluid-output recovery |

W1 implements Quern, Clay Mixer and a fixed 2x2x2 Firing Chamber: controller at the front-left
bottom and seven bricks. Eight F0 ceramic/grog processes connect Data Cores, primitive
plumbing/handling, original assets, Atlas/JEI, quests and the paginated guide.

`WorkRecipe` owns paid progress separately from selected `Recipe`; changing recipes cannot
transfer paid work. `ItemLayout=3` preserves indexes. Old compact inventory cannot reveal lost
positions: preserve ordering with warnings, not invented sparse layouts. Reserve all outputs
before consuming inputs. Fluid arrival wakes work and blocked item exports retry.

W2 is an oriented three-block Settling Trough: controller, channel and screen. Parts are
static, without block entities or artificial status. Air is required above the parts;
a hopper can sit above the controller. Collision/model/preview geometry is shared.
Original process: 2 dirt + 1000 mB water -> 2 clay + 1 gravel + 750 mB recovered water.
Initial four-orientation solids and retained work passed; real water transfer failed.

ADR-0132 fixes actual transfer, not upstream sustainability:
- Clay Conduit forwards pushed fluid; it is not an automatic upstream extractor.
- `FluidOutputQuota` marks only produced non-vented volume and exports through the front.
- Input and legacy unmarked stock remain intact. Malformed quota warns; it never grants drain
  permission. Consumption/extraction debits owned volume; output ownership survives NBT.
- Reserve the applicable primary input batch against export and limit intake to that batch.
  Establish acceptance before extraction; Forge simulation shares the execution contract.
- Secondary fluids count only if the neighboring face permits extraction.
- At most one process work callback runs per world tick.
- Install F0 buffer/rating configuration before buffer creation; never read uninitialized
  subclass fields in a base constructor.

## Historical evidence, not a current-WIP certificate

| Scope | Evidence |
| --- | --- |
| W1 full build, 34 suites, docs | `build/validation/20261017T142252076Z/status.tsv` |
| W1 workshop: 19 markers, natural exit | `build/validation/workshop-final-smoke.log` |
| Initial W2 water-export failure | `build/validation/settling-native-smokes.log` |
| Fluid fix full build, 34 suites, docs | `build/validation/20261017T163448732Z/status.tsv` |
| Fluid fix workshop/settling/matrix-lifecycle, natural exit | `build/validation/fluid-output-recovery.log` |

Historical W1 built/loaded SHA-256:
`3005B11CFC748FECBE7B542DABB8C16C12590344059A6BC0190659CAD25826DC`.
It is **not** the C3 jar hash. Foundation 33-suite/41-smoke evidence is also historical.
The complete final matrix was then 43 scenarios; read `tools/smoke/scenarios.txt` for the live
inventory rather than copying this count. Actual GUI/JEI, no-command survival and retained-world
restart are still pending. Preserve failed/cancelled attempts rather than replacing them with
later green retries. See [the ledger](../docs/testing/latest-validation.md).

## C3 source and asset WIP

At migration intake, these were uncommitted. The preservation commit retains them, unaccepted:
- `source/SourceLogic.java`, `source/SourceSite.java`, `item/CrankHandleItem.java`.
- `machine/GeneratorKind.java`, `recipe/RenewableProcesses.java`, `recipe/BootstrapAssembly.java`.
- Process/generator implementations, registrations, menus/screens and crafting catalogues.
- Geological Collector, Biocultivator, Biomass Dynamo, Assembly Bench, Crank Handle,
  `mineral_aggregate`, `biomass`, `fibreboard`, five crafts and geological-bed/planks tags.

Primitive recipes remove unnecessary metal/redstone. Trough output slots append from two to
five without shifting old indexes. Menus now have ten data fields, including source-site faults.
Biomass fuel registration waits for its item supplier; `GeneratorKind` shares F0/F1 behavior.

Intended values, to verify against the source rather than duplicate as a second catalogue:
geo 200 ticks/250 mB/4 FU per tick -> aggregate + clay; bio 160 ticks/aggregate/250 mB/
4 FU per tick -> four biomass; biomass fuel 200 ticks; F0 Dynamo 8 FU per tick;
Crank 160 FU/cooldown 10; Bench 600 ticks; Assembler projection 200 ticks.

Twelve `RenewableProcesses` rows cover geology, biology, dry stone/gravel, wet raw iron/copper/
redstone/dirt/gravel with recovered water, sand/flint, fibreboard/paper/sticks/charcoal,
glass and iron/copper reduction with slag. Environmental sunlight/geology justify sources;
the finite chunk vein is not magically renewed. The initial ordinary furnace is bootstrap,
not the canonical perpetual industrial solution.

Only `common:compileJava` was observed successful on the runtime working tree (about 1m5s).
This is a prior terminal observation; no exact source-bound report is supplied for it here.
No full Forge build, current suite run or native renewable cycle is claimed.

The asset worker was cancelled without a handoff, but left `BlockModels`, `FormTextures`,
`GenerateAssets`, `MachineArt`, new `BootstrapArt`, `GENERATED.md`, generated blockstates/
models/textures/loot, mining tags and expanded `states-pack`/`states.expect`.
They cover five new blocks and three new items. Preserve and audit them; final QC/build/smoke
was not observed. No image-provider generation is claimed; assets are original JDK generation.

### First bounded integration review

1. `heldConditions` now runs for every machine each work tick. Review whether this overwrites
   other condition controls; restrict regulation appropriately and enforce the 1450 C Firing
   ceiling/envelope. Do not silently redesign all conditions.
2. Gas probing validates fuel before extraction but throws on partial extraction. The endpoint
   has no formal exact-drain guarantee: prove reservation/partial-drain/backpressure safety.
3. Integrate every enum/Forge/JEI/client-effect/menu/name/tooltip/localization surface.
   `MachineEffects.java` is under `machine`, not `client`.
4. Derive changed recipe/Atlas/test counts from real catalogues. `ironBudget` now sums three
   while a test expects two; investigate the contract rather than weakening assertions.
5. Bench projects Bootstrap crafts and four vanilla basics; the initial Relay Matrix still
   lives in `T1Recipes`. Prove automated physical exit fabrication instead of assuming coverage.
6. Audit cancelled asset-generator outputs and state fixtures before regeneration/acceptance.

## Sustainability and player communication still required

Every mandatory resource/consumable must recursively reach an unlimited, automatable,
mod-owned source. Finite bootstrap is allowed only to establish renewable producers with
construction reserves and recoverable mistakes. No canonical vanilla farms, exploits,
repeated looting, finite mining or dimension mob-drop dependency. No unexplained matter creation.
T0 establishes the first self-sustaining base; later tiers improve throughput, yield, purity,
efficiency, automation, parallelism, by-products, recycling, complexity and alternative routes.

Resource classes: `bootstrap`, `renewable`, `closed_loop`, `catalytic`, `lossy_loop`,
`finite_optional`, `forbidden_dependency` (validation failure).
Side streams: `byproduct_current`, `byproduct_future`, `byproduct_optional`, `waste_recoverable`,
`waste_hazardous`, `waste_inert`, `process_loss`, `emission`, `catalyst_spent`,
`potential_future_use`.

Current consumers must exist; future use needs a named plan, while uncertain potential stays
uncertain. Inert waste can be optional/discardable; hazards need actual handling. Do not invent
consumers or require limitless hoarding. Emissions/losses need not become item stacks.

The canonical graph must include IDs, mandatory flags, first-use/renewable tiers, bootstrap
sources, routes/machines/multiblocks, consumed/catalytic dependencies, rates/by-products,
energy/heat/pressure/fluids/catalysts/maintenance, recycling, automation, vanilla dependencies,
deadlock risk and validation. Share metadata across tooltips, fluids/gases, JEI/Atlas, machines,
quests, reports and dashboard; quantities remain in the live recipe authorities.

Root validation at mandatory tiers/machines/multiblocks/objectives. Trace recursively to
terminal sources; reject indirect finite/vanilla dependencies, unbootstrappable cycles,
consuming the only producer-construction supply and resource-positive recycling exploits.
Distinguish environmental sunlight/geology-fed energy loops from recycling exploits.
Produce machine-readable and human reports; fail CI on new unresolved mandatory dependencies.
Any exact versioned pre-existing waiver has concrete remediation and remains visible debt.

For every T0-T15 tier, state permitted bootstrap, newly renewable routes, easier old resources,
recycling improvements, useful by-products, removed vanilla dependencies, optional finite
resources and deadlock prevention. Unaudited/unauthored details stay explicit unknowns, not
invented implementations. This remains gameplay work, not a documentation-only program.

## Partial inventory and renderer handoffs

The Luna inventory worker returned research, **not** `production-inventory.json`.
At `ee9ae09`, the 13-material projection had 315 processes; the four-material test fixture had
121. It reported 50 crafts (49 shaped/one shapeless), 25 quests and 11 lines. These are not
current C3 counts or a complete audit. Export the real `ProcessGraph`, `SupplyCatalogue`,
local tags, `BootstrapRecipes`, `T1Recipes`, other authored JSON and quests.

Useful baseline fluid rows: steam/condensation each 1000 -> 1000; electrolysis water 2000 ->
hydrogen 2000 + oxygen 1000 (oxygen vented); recombination -> water 2000. Atmospheric oxygen
requires an explicit environmental source despite no inventory input. Emitted fluid state is
not the same thing as recipe operating conditions.

The renderer's original commits are `e2c2a0a4ad0d3facb8974b5c4e5aaedbe1bcc1cd` and
`0cea29678ce2dcab78767226bf45f6a966b782a8`, integrated as `40d5260` and `a533888`.
Its four runtime files under `tools/dashboard` fetch only adjacent `data.json`, schema 1,
without synthetic fallback. See [the full contract](../docs/dashboard/README.md).

Preserve context/classifications/tiers/resources/routes/machines/quests/assets/validation/
findings; T0-T15 unknown support; neutral unknown badges; discard only when explicitly true;
consumed/catalyst/output roles; depth 1-3, at most 48 graph nodes, shown/total counts and table
equivalents. Count only explicit completed deliverables whose `source_commit` equals
`context.commit`. Source links are commit-bound. Exclude personal paths, secrets and logs.

The child reported 26/26 real-browser synthetic checks at actual 1440/390 viewports, preserving
the first 22/23 failure and repair. Its behavior-suite attempt was cancelled; it did not certify
the mod build/smokes or the Bash twin. Its temporary server was stopped. No independent final
review, real `data.json`, exporter/auditor, CI artifact, production local serving or deployment
is claimed. Integrate real data incrementally alongside gameplay, not by rewriting the viewer.

## Ordered continuation and later ports

First complete bounded C3 integration while Astra designs canonical audit ownership and an
independent read-only worker can extract pinned inputs. Then integrate the source-bound audit,
handling/player views and actual dashboard data, reports and CI. Finish the current automated
integration milestone and preserve outstanding manual gates honestly.

Only then start the owner's 521-line functional-port follow-up:
1. Audit Firing Chamber, Settling Trough, Ground Array and every authored multiblock. Report
   current/target architecture, findings, migration, risks and affected files before code.
2. Astra decides reusable roles/types/tiers, constrained replaceable positions, minima/maxima,
   direction/filter rules, exclusive controller ownership, formation/invalidation/cache/NBT.
3. Recipes use aggregated capability views, not controller-only storage or concrete-class chains.
   Implement real basic buses/hatches, not empty shells for hypothetical future systems.
4. Prove simulation/reservation/commit, partial transfers and backpressure without loss or
   duplication; cover unload, break/reform, relocation and competing controllers.
5. Keep AE2 optional and core-independent. Verify the actual supported version/API, safe
   absence and real ME behavior; do not claim a fictional integration.
6. Delegate focused tests and independent review where warranted; Astra owns final review.

Plan amendment PA-0001 indexes the original's sections into the three held port tasks of
[the work plan](work-plan.json) as `requirements_trace`; read the original, not only that index.

No unrelated T1/T2 consumer or campaign frontier is authorized. Observed GUI/JEI, no-command
survival and retained-world restart remain required before merge/Tier acceptance; they are
not a new blanket freeze on further authorized T0 development.

## Validation and environment recovery

Workers use the existing paired `validate`, `check-links`, `run-checks` and `run-smokes`
scripts; the coordinator reviews results rather than running product checks itself.
Run the smallest relevant checks during development and the full current matrix for integration.
Capture exact source, exits and built/loaded artifact hashes. Cancellation, forced shutdown
and Gradle `NO-SOURCE` are not passes.

Use Java 17 and the Gradle-resolved `common:writeCheckClasspath`, not a cache-wide jar scan.
Production smokes load the remapped Forge jar, not the hanging development launcher.
Old JDK/dependency caches and raw logs remain in the retired worktree; explicit local paths
arrive privately. Restore missing dependencies only when necessary, never copy old worlds,
source or build outputs into a fresh test as proof. Environment variables reset per shell.
Preserve Windows/Bash script twins; Windows failures are tool bugs, not acceptable omissions.

See [active queue](../docs/BUILD-OUT.md), [route map](../docs/AGENT-MAP.md),
[validation](../docs/testing/latest-validation.md) and
[manual acceptance](../docs/testing/t0-survival-acceptance.md).
