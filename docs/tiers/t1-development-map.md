# T1 development map

Status: **active development, not a release or completed T1 acceptance**.
The owner explicitly opens this work while testing the unchanged T0 dev build
(ADR-0168/0169). [The implementation plan](../../plan/feature-t1-first-factory-1.md)
is the execution graph; [the reusable-systems handbook](../architecture/reusable-systems.md)
is the extension guide.

## Product boundary

T1 Voltaic is the first electrically organized factory: establish useful F1
generation/distribution, transform inherited renewable materials, reproduce the
F1 control supply and manufacture the physical F2 exit. Existing machine families,
recipe conditions, inventories, native transactions, grid, teaching renderers and
claim ledger remain in use. Do not rebuild them for a tier label.

The baseline is the actual a64 experience source, not old main or a test fixture.
The player's installed executable remains F298988d/JAR95bf47c4/ZIP3e5ad1fb.
Development authorization is not T0 acceptance, a graphical pass, a restart,
deployment, T1 release or permission to skip physical progression.

## First-factory line contract

`progression/T1Progression` is loader-neutral selection metadata consumed by
runtime quest teaching and the source-derived report. It identifies real routes,
dependencies and mandatory/optional/exit roles; it is not a recipe catalogue,
execution engine, global unlock or list of invented bills.

| Line ID | Role | Existing authority / useful outcome |
| --- | --- | --- |
| `power` | Mandatory | Thermal Generator, Flux Pylon MK1/link and existing Flux/network curves; show generation, demand, distribution and brownout. Fuel semantics come from the generator authority. |
| `milling` | Mandatory | Pulverizer/ProcessGraph transforms, including aggregate-to-sand support; not a requirement for a vanilla ore farm. |
| `reduction` | Mandatory | Existing Arc Furnace reduction/material routes; their real conditions/by-products, not a new hypothetical blast-furnace chain. |
| `glass` | Mandatory | Existing sand/glass routes supporting assembly; source quantities from live recipes. |
| `forming` | Mandatory | Press, non-consumed dies, plates/rods/gears/copper coils and machine casings; Wire Mill is not a hidden first-F2 prerequisite. |
| `assembly` | Mandatory | Assembler and existing F1 Matrix reproduction; physical construction stock and power must be accounted for. |
| `relay_f2` | Exit | Only `assemble/relay_matrix_f2`; actual physical F1/casing/glass/Flux Cell operands remain canonical. |
| `extraction` | Optional | Scanner/finite terrestrial extraction as a compatible supplement, not the unlimited canonical source of mandatory inputs. |
| `logistics` | Optional | Existing hoppers/belts/arms/interfaces within their implemented sides/owner semantics; no new transaction framework. |
| `storage` | Optional | Existing storage/tank/capacitor mechanisms, not duplicate save formats or a mandatory external storage mod. |
| `kiln` | Optional | Existing Kiln branch; do not silently turn its optional product into a required F2 consumable. |
| `factory_space` | Optional | Existing Factory World branch; not a mandatory detour for the industrial Matrix exit. |

The contract's code is authoritative for route identities and edges; the table
explains purpose, not a second complete dependency or ingredient catalogue.
Atlas recipe IDs are selected explicitly. `fuel/thermal_generator/biomass` is a
fuel route, not an Atlas entry; explain it rather than silently deleting a missing
recipe. The existing Flux Cell craft must appear through a projection that matches
its unchanged runtime JSON, not an invented battery-charge recipe.

## Progression and construction boundaries

The first five F1 matrices serve power, milling, heat, forming and assembly.
Pylons distribute power without consuming an imaginary extra Matrix.
Reproduction is an Assembler route; the physical F2 output is the exit, not a
quest certificate or the mere possession of a casing.

Retain all old T0/T1 quest IDs/rewards/claims and the per-player T0 `relay` entry.
Specific T1 lessons can replace generic preview prose and add reward-free guidance.
The narrow intended graph change removes `factory_world` from mandatory
`industrial_matrix` prerequisites; it does not remove that optional task, grant
F2 by fiat or reset existing progress.

Explain generation/distribution, soft voltage/brownout, optional extraction, dry
reduction, Press/catalysts, assembly/reproduction, inherited renewables, optional
Kiln and physical F2/Flux Cell. Recipes/quantities/FU/time come from canonical
runtime selections, not duplicated localized shopping lists. UX/pacing approval
remains a later player observation.

## Resource and by-product analysis

Mandatory recurrent supply must ultimately be unlimited, automatable and mod-owned.
Start with the existing T0 aggregate/clay/biomass/water/wood/paper/charcoal/glass/
iron/copper/redstone routes and trace every consumed upstream input. Finite
exploration may supplement that base, never quietly replace its canonical supply.
Protect construction, catalysts and seed stock before claiming recurrent surplus.

Reuse `AuthorityAdapters`, `SustainabilityAudit` and `ConstructionReserve`.
T0 annotations/proofs do not automatically certify all T1 rows. In particular,
an untagged F1 Matrix and an explicit F1-rated ingredient must reflect runtime
`ControlMatrixNbt` semantics consistently before a model can prove their connection.
A bounded/unresolved projection or sufficient witness is not a global minimal bill
or established renewable factory.

Use existing by-product/emission semantics. Current consumers need real recipes;
future feedstocks need named committed plans; uncertain waste stays honestly
classified. Do not add artificial sinks or infinite stockpiles to turn an unknown
into an apparently useful product.

## Ownership and integration

The core lane owns T1 selectors/export/contracts and the tier production report.
The quest lane owns shared teaching/content/locales and affected quest/backend
checks. The coordinator owns architecture/product decisions, root documentation,
this map/handbook and final integration. Serialize shared-file edits and executable
validation against a fixed source revision.

The attempted documentation worker had only read-only rg/glob, no editor/Git:
it produced no files or commit. Its writing assignment was withdrawn, not reported
as complete or relaunched. The coordinator writes documentation while application
implementation/testing stays delegated.

Keep T1 in a separate branch/PR based on the actual T0 experience source. Do not add
T1 code to the T0 draft or edit its installed ZIP. Preserve original failed checks,
source hashes, previous branches/worktrees and exact evidence when integrating.

## Sufficient proof for this increment

| Changed property | Targeted evidence | Not established by it |
| --- | --- | --- |
| Line IDs/roles/dependencies and live route resolution | T1 contract/export checks against real catalogues/JSON, deterministic normal/CheckOnly generation and negative missing-ID cases | Global renewable establishment, minimal costs or runtime factory operation |
| Existing Flux Cell Atlas projection | `VerifyRecipes` catalogue-versus-live-JSON and `VerifyAtlas` selection/shape assertions | A new recipe, charge requirement, storage/energy implementation or different balance |
| T1 teaching/optional graph edge | `VerifyQuest`, `VerifyQuestClaims`, locale/backend generation and affected strict registry parsing | Client readability, human pacing or a completed campaign |
| Source/API integration | Common/Forge compilation and affected neighboring selectors on one pinned input | A deployed/booted new artifact or T0 acceptance |
| Unchanged processing/native-fluid/save/registration paths | Static delta/assumption review plus exact existing source-bound proofs | New adapters, new serialization, retained-world upgrade or foreign-handler rollback |
| Reusable-system docs | Link/schema/plan integrity and verified source references | Compilation, playability or artistic approval |

No new native/client smoke is justified merely by adding a line, lesson or
catalogue projection. If review finds a changed binding, owner, settlement, save
meaning or other uncovered behavior, name that uncertainty and run the smallest
actual proof before claiming it. Do not replay every recipe or reset all prior
evidence because the development branch has a new commit.

Build-only execution may coexist with the owner's legitimate client under existing
headroom/ownership guards; it must yield rather than stop the game or lower limits.
No Prism/deploy/restart lease, new paid generation or external dependency activation
is part of this increment.

## Remaining progression work

After the first source increment, resolve any explicit projection/stock/renewable
unknowns with the existing model, verify actual line composition, and use feedback
to improve T1 clarity, construction ergonomics and automation. Quantities and new
mechanics need their actual authority/decision, not retrospective invention.

Later release qualification must bind an exact package, clean import/startup,
applicable copy-based save compatibility and representative real client behavior
to the candidate. The owner's T0 testing proceeds independently; preserve its
results without inferring a pass from this development document.
