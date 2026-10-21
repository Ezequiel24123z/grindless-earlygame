# Reusable systems handbook

This is the implementation/extension map for parallel T1 development under
ADR-0168/0169, not a new engine specification or a tier-acceptance certificate.
The original reviewed baseline is a64/F298988d. The current installed player
build is dd0/JAR29bc/ZIP79ce with the pristine existing-game copy; original F and
backup remain intact. Separate development426 integrates native b38, pressure,
Blueprint and corrected F1 Wire Mill source. Only2a93 owns its new fixed-source
qualification window; results are pending, not passes, and documentation never
Syncs underneath active stages/locks.

Java paths below are relative to
`common/src/main/java/io/github/ezequiel24123z/grindless/` unless stated otherwise.
Keep actual values in their authorities; documentation, quests and exports select
and explain those values rather than becoming replacement catalogues.

## Extension rules

Use the existing mechanism before designing another. For significant infrastructure
changes, inspect the local seam, then actual mature implementations in parallel;
the [pinned external-reference record](../design/t0-external-references.md) already
covers inventory admission, controller proxies and bounded inspection.
External code/assets need compatible provenance and notices before import;
conceptual comparison is not permission to copy implementation or art.

An extension must name its authority, invariant and uncovered behavior. A new
recipe/line/lesson within an existing domain normally needs exhaustive data checks,
not another native boot per row. New owners, side policies, callback order,
registrations or persisted meaning are mechanism changes, not harmless metadata.
See [proof reuse](../testing/test-strategy.md).

## Materials, supplies and process selection

| System | Source authority | Invariant / safe extension | T1 use and proof boundary |
| --- | --- | --- | --- |
| Material and supply catalogues | `material/MaterialRegistry.java`, `MaterialSnapshot.java`, `SupplyCatalogue.java` | Stable names, tags, forms and source provenance are shared data. Extend the catalogue/tag snapshot, not separate tooltip or quest quantities. A tag name does not prove all runtime members interchangeable. | Select existing copper/iron/forms and inherited renewable inputs. `VerifyMaterial`, `VerifyRecipes` and sustainability adapter/registry checks validate data; no new supply is renewable merely because it is registered. |
| Process graph and lookup generations | `recipe/ProcessGraph.java`, `ProcessRecipe.java`, `ProcessLookup.java` (`rebuild`, `generation`, `find`, `resolve`) | Families, ingredients/fluids, conditions and real output stacks govern selection. A rebuild publishes a new generation; cached recipe/work identities must be reconciled and sleepers awakened. | Reuse milling, reduction, glass, pressing and assembling. Add eligible rows through the graph; do not build a tier-specific executor or persist a list index. `VerifyLogic`, recipe checks and the existing reload regression cover their actual domains. |
| Crafting/Atlas projection | `recipe/T1Recipes.java`, `AtlasLogic.java`, live `data/grindless/recipes/*.json` and `sustainability/AuthorityAdapters.java` | Runtime JSON, catalogue projection and displayed ingredient counts must agree. A catalogue row is not an alternative recipe with its own balance. Missing route IDs are errors, not silently discarded selections. | Flux Cell has an existing JSON craft; its new Atlas projection must match that JSON exactly. `VerifyRecipes`/`VerifyAtlas` are the affected data checks; source-only review is not their execution. |
| Sustainability and construction reserves | `sustainability/T0Sustainability.java`, `AuthorityAdapters.java`, `SustainabilityAudit.java`, `ConstructionReserve.java` | Trace consumed dependencies to terminal sources and protect construction/finite bootstrap stock. Repeatable is not necessarily renewable; bounds or unresolved identities remain unknown, not a green default. | Inherit the T0 base without relabeling its proof as complete T1 establishment. New line selection uses live authorities and explicit assumptions. T0 profiles remain T0-scoped. |

Use [the T1 development map](../tiers/t1-development-map.md) to choose the line,
then change the actual authority. A registered F1-rated recipe may belong to a
later prototype; registration or FU demand alone does not authorize progression.

## Conditions, chassis and physical control

`process/ConditionEnvelope.java` describes supported temperature, pressure,
atmospheres, fields, agitation and catalyst capacity. `ConditionState` and process
conditions describe the actual state/request; `machine/ProcessMachineKind.java`
and `MachineBlockEntity.envelope()` connect family, chassis and machine behavior.
An envelope does not override family, site, structure or physical control checks.

Source541cf6c adds optional absolute `ProcessRecipe.pressureMPa` and explicit
`pressureHalfWidthMPa`. Old constructors leave pressure absent; zero is a
meaningful optimum, not ambient/absence. `ProcessLookup.conditionsOf` converts
through the existing builder/absolute band, shared by Atlas/audit/inspection/JEI.
No existing production pressure recipe or machine capability changes.
[Proposed ADR-0175](../DECISIONS.md#adr-0175--carry-optional-absolute-process-pressure-through-existing-contracts)
records the unqualified engineering rationale; its affected results remain pending.

`matrix/ControlMatrixSpec.java`, `MatrixArchitecture.java` and
`ControlMatrixNbt.load()` are the physical control authority. Architecture and rating
are not a world permission flag. Legacy untagged matrices use the architecture's
first frontier; malformed explicit `Rating` data is rejected, not normalized into
a valid upgrade. Preserve that distinction when exporting resource identities.

Reuse these definitions to describe first-factory eligibility; do not create
per-quest power permissions. `VerifyControlMatrix`, `VerifyBand`, `VerifyMachine`
and recipe tests cover their declared contracts. A model that distinguishes
untagged F1 from an explicit F1 operand must disclose the mismatch rather than
claiming full factory reachability from an incompatible resource key.

## Scheduling and work completion

`machine/MachineBlockEntity.java` owns lifecycle, subscriptions, power requests,
conditions and base save behavior. `machine/TickSubscriptions.java` owns active
work handles; `ProcessMachineBlockEntity` subscribes processing/export work when
needed. Add bounded work through that lifecycle; do not add an independent busy
polling loop or preserve a subscription to a removed block entity.

`ProcessMachineBlockEntity.complete()`, `plannedOutputs()` and its `Completion`
settlement are the existing completion seam. Inputs must still match, the complete
output batch must fit and fluid reservations must remain valid before consumption.
Completed paid work stays recoverable while output is blocked. A failed completion
must not silently reset work, duplicate a catalyst or consume payment twice.

T1 Press/Assembler reuse this path; dies are catalysts, not expendable copies in a
quest bill. Reuse existing completion/output-block/reload proofs when the path and
bounds are unchanged. New output classes, settlement side effects or re-entrant
notifications require affected regression/transaction tests, not just a recipe count.

## Native fluids and external boundaries

`fluid/NativeFluidStore.java` (`Access`, `Request`, `plan`, `Batch.commit`) is a
server-thread synchronous ownership contract. Planning is non-mutating, aliases
are deduplicated by backing-store identity and access limits bound visible quota.
Commit rechecks identities/state/limits, compensates tentative owner mutations on
rejection or synchronous runtime failure, and notifies only after settlement.
It is single-use, not a world-crash transaction or a generic foreign-handler rollback.

`container/FluidInsert.java` connects loaded faces and endpoint revisions to this
contract. Preserve role-visible output quota even when several hatches expose the
same store. `fluidOutput` is produced quota, not permission to extract every fluid
inside a processor. `VerifyFluid`, `VerifyWorkshopPorts` and the source-bound native
completion/binding proofs cover their stated participants; a foreign handler or
new side/owner policy needs its own boundary proof.

## Items, tanks and persistence

`container/SlottedContainer.java` serializes indexed slots, including gaps. Legacy
compact lists retain their recorded order and log their missing-position limitation;
do not guess historical slots from an item's apparent role.
`ProcessMachineBlockEntity` persists `WorkRecipe`, `ItemLayout` and output quota,
with explicit old Assembler output migration. Preserve IDs and existing meanings;
an arbitrary slot reorder is a migration, not cleanup.

`container/ItemPlatform.java` is the common `@ExpectPlatform` insertion seam.
`forge/.../container/forge/ItemPlatformImpl.java` and `MachineItemCapability.java`
own the loader bridge and side admission. Keep Forge types out of common and
preserve actual remainder/simulation behavior; a simulated foreign insert is not
an atomic multi-inventory guarantee.

The Basic Tank retains its existing capacity, NBT and temperature/container rules.
T1 must reuse it rather than introducing a differently serialized copy. Indexed
inventory/quota/unit contracts use `VerifyContainer`, `VerifyPersistence`,
`VerifyBasicTank` and affected fluid checks. Real retained-world upgrade behavior
remains a copy-based compatibility observation, not a claim supplied by a codec test.

## Bounded Blueprint preflight and settlement

Source4550b975 reuses the existing Blueprint item/tool, not a drone or network
construction framework. `item/BlueprintLogic.Plan` / `preflight` produce an
immutable read-only diagnosis and material multiset/shortfall.
`BlueprintItem` strictly decodes the legacy payload, checks loaded destinations
and permissions, rechecks before placement and settles bounded main/offhand
payment; `BlueprintToolItem` shares the capture diagnosis. Preserve the32-edge/
512-piece bounds, facing and absence of copied block-entity contents.

Ordinary missing/blocked preflight refuses without spending. An explicit false
placement restores that attempted payment; already placed cells remain and
unattempted materials are refunded. A throwing callback is ambiguous and its
attempt stays paid while unattempted resources return and the error propagates.
This is not global rollback, crash atomicity or universal zero loss.
`VerifyBlueprint` / affected `VerifyPlanner` results and representative actual
capture/stamp/load evidence remain pending. The old native item-existence smoke
does not prove interaction; the author-owned representative Forge probe is unready.

## Structures and controller-backed interfaces

`structure/WorkshopStructure.java`, `WorkshopPortBlockEntity.java` and
`forge/.../fluid/forge/WorkshopPortCapability.java` implement the bounded existing
T0 structures/interfaces. The port is a zero-storage proxy: the live controller
owns inventory, fluid, processing and persistence. Family/role/face/count/shape and
loaded controller identity must be revalidated; replacing a controller at the same
position cannot keep an old capability/store alive.

Extend the known-role/shape declarations for a justified structure; do not import
a generic multiblock DSL or invent hatches with independent backing tanks.
The existing six variants/four roles are not proof that every future T1 family is
supported. `VerifyWorkshop`/`VerifyWorkshopPorts` and the exact-F registered native
ports probe cover their actual geometry, quota and identity cases only.

## Flux generation and distribution

`energy/FluxTier.java`, `network/FluxNetwork.java` and `FluxNetworkData.java` own
nominal levels, pooled storage, topology, throughput, upkeep and demand satisfaction.
Pylons are passive distribution infrastructure, not additional physical Matrix
consumers or energy generators. The network is a shared pool, not a new per-machine
cable-search algorithm.

Insufficient generation/throughput produces proportional brownout; under-volting
and upper voltage behavior use existing machine curves. Explain these through
real machine/network state rather than inventing a second quest-side simulator.
`VerifyEnergy`/`VerifyNetwork` cover their mechanism contracts. New topology,
storage participation or persistent network semantics invalidates the relevant
proof assumptions; a new teaching paragraph does not.

## Quests, claims and renderer adapters

`quest/QuestCatalogue.java`, `QuestLogic.java` and teaching retain stable logical
IDs, requirements and evidence. `QuestProgress` / `QuestClaimService` preserve
legacy paid history; the current Forge FTB adapter imports individual native
receipts and fences late legacy minting before native claims. Native completion
is not proof a reward was paid. LESSON additions are reward-free. A quest is
guidance/evidence, not machine permission or a second resource-generation engine.

`QuestTeaching.java` and `client/QuestTeachingClient.java` retain shared teaching
selectors; the player-facing framework is genuine native FTB under ADR-0171,
not the old custom widget in another window. `QuestBackendPlatform` and its Forge
implementation must not admit two reward authorities. Add T1 guidance without changing
old reward identities, ledger format, enum ordinals or the physical T0/F1 entry.
Factory World may remain an optional branch; it must not be a hidden F2 prerequisite.

`VerifyQuest`, `VerifyQuestClaims`, generated backend checks and locale/data checks
are affected by teaching/graph changes. The baseline's 46 IDs/26 reward identities
must remain; additive reward-free lessons are not a new allowance of starter items.
Server/headless projections cannot certify client readability or reward UX.

## Shared player-facing projections

`recipe/AtlasLogic.java`, `guidance/ResourceGuidance.java`,
`ResourceGuidanceProjection.java` and client adapters share canonical recipes and
resource classification/handling. Teach real recipe ingredients/FU/time instead
of hand-maintained shopping lists. Route IDs and display-entry IDs are different
namespaces: a generator fuel route is not automatically an Atlas recipe.

T1 line selectors must return explicit recipe selections and explicit non-Atlas
explanations; unresolved mandatory IDs cannot disappear behind `filter(nonNull)`.
Atlas/JEI/quests/resource tips are consumers, not new recipe authorities.
`VerifyAtlas`/`VerifyResourceGuidance` and renderer/backend data checks prove
projection contracts; a new widget/API still needs representative client review.

`inspection/MachineInspectionProtocol.java` and its snapshot/client adapters expose
bounded server-authoritative state for the optional F8 overlay. Keep packet/target/
rate/loaded-state validation and avoid world scans or client-invented machine truth.
Source541 writes inspection schema2 and reads strict schema1 without required
process pressure; reject unknown versions/partial bands through `MALFORMED`.
Existing observed/tank pressure is unchanged. Its new DTO/codec contracts need
their affected proof, not reuse of the old schema's result. Optional inspection
must remain usable without installing a new overlay dependency.

## Recovery exchange and telemetry

`shop/ShopCatalogue.java`, `ShopInventory.java`, `ProgressionCostModel.java`,
`EconomyProjection.java` and `ShopSession.java` keep physical barter, batch admission,
live route costs and session results together. There is no currency wallet. Do not
introduce T1 item grants or a second price table as a shortcut around production.
`VerifyShop`/`VerifyEconomy` cover their declared offers, admission and cost graph;
new mandatory dependencies need real source/resource analysis.

`telemetry/dev/DevTelemetry.java` is development evidence, not player acceptance or
another claim ledger. Preserve bounded, privacy-conscious observation and provenance.
Do not export private host paths, account/launcher command lines or credentials.
`VerifyDevTelemetry` validates the projection domain, not whether a player enjoys
or successfully completes the campaign.

## Visual/audio source pipelines

The [visual asset tools](../../tools/visual-assets/) and
[sound-design tools](../../tools/sound-design/) separate source candidates,
deterministic transforms, distribution assets and provenance. Reuse named presets
and lifecycle conventions; do not treat the existence of an Ogg/PNG as artistic
approval or silently restore rejected synthetic sounds.

T1 development has no new generation/provider spend allowance. Each future run
needs explicit finite purpose/budget and durable receipt/provenance; provider/model
access, copyright permission and artistic acceptance are separate facts.
Asset shape/licence/determinism checks are reusable technical evidence. Appearance,
listening/fatigue, in-game render and UX remain human/client observations.

## Build, packaging and evidence

`gradle.properties` is the version authority; common stays loader-neutral and Forge
owns platform bindings. Existing [twin checks](../../tools/run-checks.ps1),
[pack builder](../../tools/build-modpack.ps1) and
[SSH/Prism runbook](../operations/ssh-prism-workflow.md) are the workflow, not a new
T1-only build/deployment framework. Each shell entry point keeps its `.ps1`/`.sh`
twin and exact source/artifact identity.

Build-only work uses an isolated root, one Gradle/cache owner and fresh headroom.
An inspectable legitimate player client is not a global-zero-Java violation.
Do not touch/restart/deploy the dev instance while the owner tests it.
An unchanged T0 boot/transaction proof may cover unchanged mechanisms; new source
still needs affected compilation/data/projection checks. A documentation commit
must not be relabeled as the executable revision of an older JAR.

## Evidence and next use

The [Phase A source qualification](../testing/t0-experience-source-qualification.md),
[Phase B native/package record](../testing/t0-experience-package-native.md) and
[Prism preservation receipt](../testing/t0-experience-prism-deployment.md) retain
their exact source, successes, failed attempts and unobserved client/save boundaries.
They are reusable only under reviewed unchanged assumptions, not blanket T1 passes.

Before implementing the next line: select its existing mechanism, trace recurrent
inputs and construction stock, state new behavior, choose affected checks, then
update this map/ADRs only where the seam changes. Record unresolved modeling,
client, migration and product boundaries explicitly rather than adding machinery
or repeating successful native proofs merely to make a report look complete.
