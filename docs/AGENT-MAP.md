# Agent route map

A task index. Open the entry for the change you are making. The files are where the
behaviour lives; the line under each entry is the invariant that is easy to break.
Code remains the source of the functions. Do not copy bodies into this file.

Design stays in the [README](../README.md), [`MACHINES.md`](MACHINES.md),
[`PROCESSES.md`](PROCESSES.md), [`BUILD-OUT.md`](BUILD-OUT.md) and
[`DECISIONS.md`](DECISIONS.md). If a section here starts restating one of those,
replace it with a link.

The same commit that changes a seam updates this file
([ADR-0087](DECISIONS.md#adr-0087--a-task-indexed-route-map-updated-with-the-seam)).

Two rules apply to every entry. Versions are written only in `gradle.properties`.
`common/` must not reference Forge types: loader differences go through an
`@ExpectPlatform` stub (`energy/FuelPlatform`, `energy/FluxPlatform`,
`container/ItemPlatform`) with the implementation in `forge/`, or through a
capability event
([ADR-0045](DECISIONS.md#adr-0045--capabilities-are-attached-by-event-not-overridden-on-the-block-entity)).

Coordination follows [AGENTS](../AGENTS.md) and
[the durable plan](../.agent/work-plan.json). The current coordinator owns design/final integration; workers
own bounded implementation/check execution with explicit files and input revisions.
ADR-0147 selects Sol 6.1 for the next isolated session; previous Astra reviews remain
historical evidence, not a requirement to restore that leader.
The current installed native-FTB runtime isdd0/JAR29bc/ZIP79ce in
Grindless-T0-FTB-Dev with the pristine existing-game/options copy. Read the
[native delivery record](testing/t0-ftb-native-qualification.md); actual owner
login/GUI/JEI/rewards and human longitudinal acceptance remain open.
298988d/95bf47c4 is the preserved original F instance, not the current FTB build.
The owner explicitly starts intended longitudinal T0 play under ADR-0170 while
T1 research/development proceeds. The [readiness investigation](testing/t0-longitudinal-readiness.md)
is preserved evidence, not a continuing disposable-only restriction. Read actual F evidence
before inferring playability from a gate label. Preserve indexed inventories,
per-player quest IDs, Matrix rating meaning and stopped-game copy-based updates.
`VeinData.DerivationVersion` is currently not consumed by scanner/extractor
generation; do not claim forward surveyed-vein stability if algorithm/material
inputs change. Old T1 owner-play and temporary FTB leases are returned.
The integrated-family426 and Blueprint-nativeM execution leases are returned;
last custody21:56:31Z reports cleanM/ownedJava0/work-cache locks0. No worker or
parent has an active PC grant. Reuse the closed source-bound proofs; future
execution needs a new explicit guarded assignment.
Gameplay blockers take priority; never stop/restart the player's game or deploy
T1 as part of independent development.
The new [experience plan](../plan/feature-t0-experience-1.md) under ADR-0161
explicitly opens bounded T0 quest/Guide/recovery economy/tank/interface/inspection/
physical sound/visual work. It does not authorize a generic engine rewrite or
unrestricted T1. Old no-port/shop proposal holds are historical for this new scope.
Every handback uses the eight AGENTS reporting categories in chat and durable state.

**Owner-reported questbook fix (ADR-0171):** native FTB Quests replaces the rejected
custom/Heracles presentation. Backend/build/pin/native-definition/migration
implementation is owned by ac50334e; copy190a024 is separate and preserves data.
Preserve `QuestProgress` paid history, stable logical IDs and individual reward
receipts; native completion must not masquerade as an already-paid reward.
Legacy late packets cannot keep minting once native ownership is established.
Do not satisfy old widget assertions by recreating that widget inside FTB.
Replace superseded presentation/pin contracts with precise native equivalents;
do not disable the closed authority/loader or deployment checks.
The [native integration reference](design/ftb-questbook-integration.md) records
exact official binary hashes, receipt APIs, Matrix adapter semantics and the
required XMod Compat JEI provider. Source research is not a loader/client pass.

The bounded fix is now installed atdd0/29bc/79ce in Grindless-T0-FTB-Dev with
the pristine36-file original-game/options copy, not the tested clone. Actual
clone load/save and a Minecraft NBT/region audit resolve retired Heracles IDs as
metadata-only; original/backup remain unchanged. Native player login, GUI/JEI/
physical reward delivery are not inferred. All temporary FTB leases are returned;
the separate2a93 build-only grant cannot update or access this T0 instance.

ADR-0172's independent T1 source2f4eee1 owns only ordinary Assembler component
data, T1 selection/provenance and affected recipe/Atlas/progression contracts.
Manual casing seeds the first Assembler; automatic output follows it, not a
forming-cycle dependency. No new engine/NBT/slots/T2 part is authorized.
Its baseline523b proof remains distinct from the later executed426 component/
model qualification and normal A85069 output merge into M.

ADR-0173 now opens isolated development through T15, not survival acceptance or
deployment. Native-baseline b38 retains the factory/component source. The
corrected3a79a1f adds one physical F1 fourth operand to `assemble/wire_mill`
and optional `wire_forming` after assembly, not an extra first-F2 prerequisite.
Four grouped slots, stock/time/FU and the ordinary F1 output remain; no Motor
charge, acid/bootstrap cycle or MK2 capability is introduced. `ProcessRecipe.milestone`
is informational, never a hidden permission. `IngredientSpec.matrixRating` /
`ProcessLookup.sameItem` are currently exact, whereas `ControlMatrixSpec.canControl`
supports stronger control; do not silently conflate quest/upgrade evidence with
future manufacturing substitution or claim the latter passed.
The later-frontier owner8210 derives source-backed T3-T15 work without promoting
prototype inventories or proposed documents to accepted routes. Root architecture,
ADR/map/plan integration remain coordinator-owned.

**Later owner clarification (ADR-0174):** baseline basic families start at T1,
not only the first five machines; later tiers grow lines/content and upgrade real
capabilities through the established chassis/MK/modules/condition design.
Preserve e265 source evidence but do not declare its former F2 operand an
upgraded Wire Mill. The source trace selects the baseline-F1 correction;
actual higher chassis/control/storage capabilities remain separate work.
The broad8210 dossier could not use `send_session_message` or write an artifact.
Parent subsequently recovers its13-tier dossier and bounded slice proposals from
the actual `session.task_complete` summary, not empty assistant messages or idle
notifications. This is read-only evidence, not an executable tier acceptance.
Published M0a676fe normally preserves Blueprint4550+d073, pressure541, corrected
F1 Wire Mill3a79, final-root ancestry and the four normal outputs via A85069.
Actual426 contracts/models, new e260 Blueprint/Planner regressions and one
real M Blueprint interaction1,182 close this bounded wave. Read
[the source-bound ledger](testing/latest-validation.md#integrated-development-source),
including exact-M CI success and the duplicate failed DRILL-OK observation.
No complete tier, new execution engine, client pass or PC grant follows.
Compatible external reuse is allowed after exact provenance/licence/notices;
neither visible source nor a pattern report authorizes a code import.

Native `FtbQuestFiles.ensure/preflight` rejects existing chapters missing new
quest/task IDs. Preserve that fail-closed rule: integrating50 source quests is
not proof that an installed46-definition configuration can update. A later
managed-file hash/backup migration must retain old IDs/native paid state and
refuse unknown edits; no raw world rewrite or relaxed preflight.
ADR-0177 now assigns that explicit production update to source ownercb273782
fromad5b50d. Reuse `FtbQuestFiles`, the native definition publisher and existing
Prism guarded-file/update patterns; no PC access or pass follows from authorship.
Source93ce975 normally integrates `FtbQuestDefinitionPublisher`,
`FtbQuestUpdatePlanner`, `forge/ftb-definition-compile.init.gradle` and the
existing host actions UpdateQuestDefinitions/RecoverQuestDefinitions.
Publishing/planning require source/runtime/JAR binding through a fresh normal
offline/serial compile-remap receipt and actual loaded images; reference schema2
requires compilationSha256. Do not reuse old build/classes as proof.
Use explicit InstanceId/plan/hash/current stopped installed pack; pending
journal blocks startup/Inspect/Deploy. Native/legacy paid world data stay untouched.
The shell twins forward the same API. Source/compiler/planner/real-plan JVM
qualification is pending; the unchanged-input Windows786/103 file proof is not it.

### Exact Matrix identity versus control capacity

ADR-0176 assigns source owner347a1c5b the typed input-role extension through
`recipe/IngredientSpec`, `ProcessLookup`, `AtlasLogic`, Forge JEI and the
sustainability/export adapters. Keep old exact constructor/factory semantics,
output identity, F2 predecessor and native exact-F1 quest filters. A named
minimum-control role reuses strict `ControlMatrixNbt` and `ControlMatrixSpec.canControl`
across legitimate architectures. Select only `assemble/wire_mill` initially;
do not special-case recipe IDs inside generic matching or alter canonical IDs.
No existing byte-equal projection or native proof covers the changed semantics.
Widening ingredient eligibility is not a chassis upgrade or permission to
invent a new family envelope, recipe bill or paid-world migration.
Only Relay `ControlMatrixItem` is registered at this checkpoint; native
minimum/exact coverage is limited to actual RelayF1-F3, with other architectures
covered by pure spec contracts. Do not register fictitious items for a test.
Source1ee2e73 is normally integrated with the die increment, not yet Java/native
qualified. `IngredientSpec.MatrixRole` names EXACT/MINIMUM_CONTROL;
`minimumControl` opts in and `acceptsMatrix` follows validated real item/NBT.
`ProcessLookup.matrixInputs` supplies only registered viewer alternatives.
Atlas/Operand/inspection carry the role with F1+ labels; canonical qualified IDs
stay unchanged. `AuthorityAdapters` names `minimum_control_input_3` and the
canonical-option supply limit. T1 exporter source/class hashes include the new
role/spec/adapter dependencies; never retarget old model provenance.
Forge `recipe/ControlMatrixInputChecks` and existing matrix-lifecycle additions
author real matching/handler/count/blocked/full-cycle assertions without new
registrations or budgets. Authored markers are not an observed native pass.

The independent automatic-tooling lane owns only four existing die outputs in
`ProcessGraph` and optional `T1Progression` selection. Derive exact material/
output parity from `T1Recipes` plus shipped JSON. Manual dies seed forming;
`tool_reproduction` depends on assembly, not the other way around. No new items,
wear, recipe engine or first-F2 requirement. Normal writers must emit any changed
canonical projections after integration, never a manual report edit.
Source63142dd/5b359a5 now normally integrates the four rows through
the shaped-catalogue grouping helper, an explicit whitelist from the live
catalogue. `VerifyRecipes` cross-checks shipped JSON; `VerifyAtlas` checks
displayed units separately from physical ProcessRecipe input groups, and
`VerifyT1Progression` keeps the new line optional/acyclic. Java and canonical
writer results remain pending, not supplied by these authored checks.
Source4f3aae3 extends that same helper as `automaticAssemblerCrafts` with
exact-bill Pulverizer/Arc/Press/Assembler reproduction and an optional
post-assembly line. Only those four new controller operands opt into minimumF1;
old F2 predecessor/outputs/quest predicates stay exact. Thermal's five groups
remain out. Current fixture shapes are209/84/266, not observed pass counts.
Exact423a CI exposes missing List overloads in recipe assertions; the author
repairs them in9b9c619 with ordered/full `.equals` and the existing boolean
helper. Actual compiler confirmation remains pending; do not weaken the guards.
Exact85e CI subsequently compiles and passes Recipe/Atlas/ControlMatrix but
exposes registry access in Graph.index for minimum-primary input. Restore pure
descriptor indexing and runtime candidate composition without fake bootstrap;
min-primary candidates must not vanish merely because another keyed list exists.
Keep optional wire ordering relative to assembly, not obsolete adjacency once
factory reproduction is inserted.

ADR-0180's thermal lane is separate from the original eight-craft whitelist:
an actual ordinary core plus final three-group assembly must cancel to the
exact manual Thermal multiset. No direct five-group projection, fifth slot,
free ingredient or reduced bill. Reuse the same item/asset/catalogue/role
mechanisms and preserve the manual first generator and mandatory F2 path.

The owner's2026-10-21 instruction separately opens T1 development (ADR-0168/0169).
Use [the first-factory plan](../plan/feature-t1-first-factory-1.md): live route
selection/data contracts, shared T1 teaching and reusable-system documentation.
Start with [the reusable-system handbook](architecture/reusable-systems.md) and
[T1 line/development map](tiers/t1-development-map.md) for authorities, extension
invariants and proof boundaries. `progression/T1Progression` selects real route
IDs/dependencies/roles; runtime teaching and export consume that shared selection,
not copied bills. Flux Cell's Atlas catalogue projection must match its existing
runtime JSON. Keep fuel routes explicitly distinct from Atlas entries and fail
missing mandatory selections.
The integrated T1 source checkpointc727894 contains50 tasks (32 unchanged T0,
14 retained T1 and four reward-free lessons), with shared body/path/recipe
selection in `QuestTeaching`/`QuestTeachingClient`. The native chapter label and
Heracles subtitle mark development; ledger, rewards, old IDs and ordinals stay
unchanged. `VerifyT1Teaching` covers live selector mutations/missing rows and
retained46-to50 claim codecs; these fixtures are not real-world migration.
Its data delta is27 T1 catalogue crafts/256 Atlas entries, not a changed Flux Cell
JSON or new recipe executor. `sustainability/T1Sustainability` reuses existing
adapters/audit/reserves for a bounded report; normalized snapshot identity belongs
in committed models and raw source/class identity in ignored provenance.
Do not alter the installed T0 candidate or treat an existing F1-rated prototype
as accepted T1 merely because it is registered. Existing catalogues, native
transactions, power and per-player claims remain the authorities.

Current experience seams (ADR-0162–0165):

- `quest/` owns typed teaching/reward claims and the existing per-player F1 exit;
  Guide registry compatibility is retained, not a new instruction authority.
- New `shop/` owns the single client/server entry and physical barter; no wallet.
  `telemetry/dev/` records opt-in causal aggregate events, not personal analytics.
- `fluid/`, `structure/` and capability adapters retain tank/controller store
  ownership; hatch aliases must not bypass produced-output quotas or old identity
  retirement after controller replacement.
- New inspection/optional native overlay packages consume bounded read-only facts;
  old machine-state ordinals and unknown facts are not reinterpreted as certainty.
- [External references](design/t0-external-references.md) record actual pinned
  principles/licences, not permission to import implementations or a Jade jar.

## Playable releases, profiles and work storage

- `docs/assets/image-generation-discovery.md` — verified external capability/
  billing/licensing boundaries and concrete deterministic pipeline. GitHub Models
  retirement is established by official sources, not a non-JSON catalogue body;
  Copilot vision/credits are not image-output entitlement. The actual local ComfyUI
  owner controls installation qualification, not this research fallback.
- `tools/visual-assets/`, its approved Multitool PNG/provenance and
  `docs/assets/comfyui-asset-workflow.md` — supervised local source generation,
  retained workflow/spec/model/seed binding and deterministic Minecraft adaptation.
  Canonical byte digests survive Git JSON transport; collection validates the
  reserved graph before HTTP and actual own history/PNG metadata. Never reset a
  spent/uncertain slot to repair observation. The regular JDK generator validates/
  copies the16px PNG and needs no Comfy/Python/network. Guide-only mode stays
  independent; preserve the other580 resource PNGs and all twelve reviewed sprites.
  Technical selection is not human visual acceptance.
- `docs/tiers/t0-superflat-entry-options.md` — source-checked initial material cuts
  and historical alternatives. ADR-0159 now selects the first free generous kit;
  do not keep that choice blocked or invent a shop/village/worldgen requirement.
  Use canonical quest/claim/projection authority, stable IDs, initial book access
  and safe one-time rewards; source math is not a survival pass. Bedrock is already
  a valid Collector bed.
- `plan/process-t0-playable-release-1.md` and `.agent/work-plan.json` — ADR-0144/PA-0007
  requirement trace, bounded ownership, separate readiness states and actual dependencies.
- `pack/profiles.json`, `pack/curseforge/manifest.json`, `pack/dependency-metadata.json`,
  `pack/public-overrides.json`, `pack/public-overrides/`, `gradle.properties`, root build
  and paired pack tools — curated version lock, explicit public configuration,
  distributable whitelist and provenance.
  ADR-0149 adds only the restricted playable selector and explicit public-override
  contract; preserve both historical profiles and the single dependency-pin authority.
  Empty overrides intentionally retain upstream defaults, not the pending official preset.
- Existing source/recipe/world/sustainability authorities below — trace every mandatory
  initial and recurrent resource before selecting/versioning the official flat preset.
  ADR-0150 keeps deposits virtual in Superflat: `vein/VeinGenerator`, `VeinData`,
  `PlanetCatalogue.veins` and extractor reads are the existing foundation, not terrain
  ore placement. Preserve seeded/surveyed identity; no staged spawn-food/landscaping.
- Existing tool environment/cache/output controls — prefer supported redirection after
  ownership and absolute-path audit; do not edit app session databases or move worktrees
  blindly. Private storage paths belong in operational evidence, not public pack data.
- `docs/reviews/go-gate.md` and `docs/testing/` — qualify technical/product RC readiness
  before delivery, record human acceptance afterward and preserve real-save upgrade proof.
- `docs/testing/t0-client-review-preparation.md` — source-bound objective literal-key/
  texture checks and exact isolated client-review probes. Its offline passes and
  old-source equality do not prove graphical launch, input, audio playback or human
  quality. Use explicit target/revision with the existing read-only Inspect, never
  silently Sync or update a save to bypass a mismatch.
- `tools/smoke/first-fluids-pack/`, `first-harness-pack/` and
  `VerifyFirstObservationFixtures` — one controlled setup, real chunk barrier and
  bounded scheduled observation of the original destination water/CO burn or five
  exact item IDs. Do not inject destination fluid or repair setup after observation
  begins. Source contracts are not native proof, and item existence is not
  post-tick survival. `docs/testing/t0-first-observation-fixtures.json` preserves
  failed CI, raw hashes and incomplete aggregate-tooling qualification. Both
  fresh-pack cases are excluded from the shared-world batch.
- ADR-0148/0156/0158, `client/GrindlessClient`, `machine/MachineEffects`, `registry/ModSounds`
  and sound resources — closed Quern/grinding and Clay Mixer/wet-work families.
  Preserve exact Quern A, import only the noticed Mixer candidate and retain
  unrelated hum callers/particles/gameplay. Passive TANK storage has no generator
  hum/flames; do not alter its fluid state/NBT. Native mix remains a human proof.
- `client/audio/T0MachineAudio` and the unchanged `QuernLoopTracker` — native tick/disconnect/reload adapter
  and testable per-position ownership. Visit block-entity maps in at most nine loaded nearby
  chunks, never blocks/worlds or force-loaded chunks; keys and weak identity scope cannot
  retain an old world. Pause freezes admission time; MASTER/BLOCKS mute clears ownership
  without consuming admission, allowing a still-running source to restart when unmuted.
  Stop a retired native object before dropping it. A never-ticked/cancelled/replaced attempt
  stays suppressed until eligibility/scope/resources change; admitted loss waits40 ticks.
  Engine `isActive` and tick admission are not claims of audible playback.
- `VerifyQuernAudio` and `VerifyAssets --sounds-only` — focused lifecycle and exact
  hash/mono/event/notice/generation-boundary checks. `tools/assetgen/.../GenerateAssets`
  preserves the import provenance; paired `convert-audio` tools convert only the five
  synthetic legacy names, never a stray WAV over `t0_grinding.ogg`.
  `docs/assets/quern-runtime-import.json` binds the source checkpoint, actual compile/
  contract/resource results, preserved blocked attempts and native gaps. A tracker
  contract or resource check does not establish a native binding or listening pass.

Grindless is Superflat-first, Overworld-compatible and longitudinal across tier releases.
The old conditional entry witness does not prove access in an unselected flat preset.
Release readiness, visual review and human acceptance are not synonyms. No T1 until
playable T0 publication and actual owner play; no new port implementation by implication.

## Machine

- `recipe/MachineFamily.java` — the family a recipe names, not a block.
- `machine/ProcessMachineKind.java` — envelope, held conditions, block-entity type.
- `menu/MachineMenuKind.java` — slot counts on the shared menu.
- `menu/ProcessMachineMenu.java` and `machine/ProcessMachineBlockEntity.java` — visible slot
  placement and persisted inventory layout; widening a menu must migrate any shifted saved slots.
- `container/SlottedContainer.java` — indexed vanilla inventory NBT. Layout 3 keeps sparse slots;
  compact legacy lists retain their recorded order, not guessed historical positions (ADR-0125).
- `machine/CrudeExtractorBlockEntity.java`, `VerifyExtractor` and
  `tools/smoke/extractor-backpressure-pack/` — A01 owned-output recovery and
  paid-work/Flux/accounting regression. Pending export must not depend on a
  runnable extraction or live vein; a re-added work callback may not debit twice
  in one world tick. Completed paid work settles before another power request;
  export retries do not reset runnable status debounce. `Progress`/`Output`
  retain their existing meaning. Native recovery, binary-NBT reconstruction and
  source contracts are distinct proofs; `VerifyExtractor --a01-vein-data` checks
  the diagnostic world's actual stopped Luna SavedData, not item-count proxies.
- `process/MachineEnvelopes.java` — when the machine holds a band from
  [`MACHINES.md`](MACHINES.md#machines-are-condition-envelopes).
- `registry/BlockCatalogue.java` — geometry and the asset list.

Paths are under `common/src/main/java/io/github/ezequiel24123z/grindless/`.
Registration is its own entry.

A recipe names a family ([ADR-0019](DECISIONS.md#adr-0019--design-machines-before-recipes)).
Out-of-band running costs time, never yield
([ADR-0040](DECISIONS.md#adr-0040--running-out-of-band-costs-time-never-yield)).
T2 and above are manufactured, never a crafting table
([ADR-0017](DECISIONS.md#adr-0017--machines-above-t1-are-manufactured-never-hand-crafted)).

## Recipe

- `recipe/ProcessGraph.java` — the generated graph (ore line, press, mill, contact,
  assembler).
  `wireMill()` now authors the baseline F1 fourth construction operand under
  ADR-0174. `progression/T1Progression.java` selects optional `wire_forming`
  after assembly: manufacture, iron wire, copper wire and copper coil. Keep the
  initial F2 dependencies and five-controller seed allocation unchanged.
- `recipe/ProcessLogic.java` and `recipe/FabricationLogic.java` — durations, FU/t,
  fluid ids.
- `recipe/ProcessLookup.java` — indexed lookup, rebuilt from that graph.
  Its opaque typed `Generation` identity is published inside the same graph/index object;
  `ProcessMachineBlockEntity.recipe` checks that identity before reusing `cached`.
  It is transient cache state, never another persisted work owner or a separate epoch.
  The base server ticker's bounded `beforeSubscriptions` hook refreshes process
  subscriptions only after a publication change, even if they were empty. Explicit
  logistics hold defers that hook until release; no machine callback runs on tag reload.
- `tools/checks/VerifyMachine.java` — the A03 generation-boundary regression binds actual
  item tags through `RegistryTagView` and `MaterialRegistry.rebuild`, including a retired
  row whose old input/output still resolve. See the
  [source-bound disposition](testing/t0-process-cache-generation.md) for actual versus
  pending validation; a prepared oracle is not a passing proof.
- `recipe/T1Recipes.java` and `common/src/main/resources/data/grindless/recipes/` —
  ordinary shaped crafting JSON, including the five-unit first Relay Matrix batch and its one-Matrix
  T1 processing-machine costs.
- `recipe/BootstrapRecipes.java` — T0 shaped JSON.
- `recipe/BootstrapProcesses.java` — the F0 ceramic routes, appended to `ProcessGraph`;
  quantities and recovery yields live here, never in a second JSON recipe graph.
- `research/ResearchLogic.java` and `machine/ResearchTerminalBlockEntity.java` — one physical
  Data Core calibration cycle. It must never write or read a global permission.

`tools/checks/VerifyRecipes.java` now expects209 generated rows for its material fixture
and27 T1 shaped crafts; `VerifyBootstrap` retains its own source/catalogue guards.
`VerifyAtlas` expects209 generated rows,84 Assembler rows and266 total handheld routes,
including both crafting catalogues, the shapeless core and physical calibration.
These are fixture shapes, not native pass counts; runtime counts depend on installed material tags.
Change every one of those numbers in the same commit as the graph. Recipe ids stay
unique. Every T2-and-above machine is an Assembler row in `ProcessGraph` and must not
gain a file under `data/grindless/recipes/`. The eight spatial prototype outputs have
neither a graph row nor a crafting JSON until their T10-T15 campaign frontiers.
Ratios are in [`PROCESSES.md`](PROCESSES.md#the-recipe-graph).

`recipe/AtlasLogic.java` decorates explicit physical ratings for display and rated lookup only.
Keep `IngredientSpec.qualified()` and `OutputSpec.qualified()` as unmodified graph identities;
unrated item lookup must still find the F2 route (ADR-0124).

## Optional process pressure

- `recipe/ProcessRecipe.java` — optional `pressureMPa` and
  `pressureHalfWidthMPa`; all old constructors leave both absent. Zero is a
  meaningful absolute optimum, not an absence/ambient sentinel.
- `recipe/ProcessLookup.conditionsOf` — shared builder conversion; use the
  existing `process/ConditionBand.absolute` and `ProcessConditions` evaluator,
  not a second eligibility, efficiency or fault implementation.
- `recipe/AtlasLogic.java`, `sustainability/AuthorityAdapters.java`,
  `inspection/MachineInspection.java` / `MachineInspectionSnapshot.java`,
  `client/inspection/MachineInspectionTooltip.java` and
  `forge/.../jei/GrindlessProcessCategory.java` — project the same declared
  optimum and half-width; do not invent a production pressure requirement.
- `inspection/MachineInspectionCodec.java` / `MachineInspectionProtocol.java` —
  source541 introduces schema2 pressure; source1ee now writes schema3 for the
  explicit Matrix input role. Read strict schema1 with no required pressure
  and schema2 with pressure, both EXACT by absence of role. Old-version role
  fields, partial/unknown roles, minimum outputs and invalid pressure bands fail
  through `MALFORMED`; existing observed/tank pressure is not removed.

Source541cf6c is integrated and technically qualified in M through exact426
contracts and normal model generation. [Accepted engineering ADR-0175](DECISIONS.md#adr-0175--carry-optional-absolute-process-pressure-through-existing-contracts)
records the rationale, not a new production recipe or accepted gameplay.
Machine work/item/fluid save ownership is unchanged; client presentation remains
unobserved and no new native run is required for this documentation closure.

## Blueprint construction preflight

- `item/BlueprintLogic.java` — immutable `Plan` / read-only `preflight`, full
  material multiset/shortfall and duplicate/bounds diagnosis.
- `item/BlueprintItem.java` — strict legacy `Blocks/X/Y/Z/Id/Item/Facing` decode,
  loaded-world/permission adapter, target recheck and bounded main/offhand payment.
  One `eligibleMaterial` predicate excludes `BlockEntityTag` stock from both
  counts and reservation: stamping default state must not burn a relocated
  machine's stores/upgrades. Check `Level.mayInteract` as well as player/item
  permissions at every destination, not only the original clicked anchor.
- `item/BlueprintToolItem.java` — capture uses the same diagnosis; retain the
  32-block edge/512-piece bounds and never clone block-entity contents.
- `tools/checks/VerifyBlueprint.java` / `VerifyPlanner.java` — new d073 regressions
  pass on exact e260 and are reused in M, not inferred from old426 contracts or
  the `first-blueprint` item-existence observation.
- Forge-only `item/BlueprintInteractionChecks.java` and the isolated
  `blueprint-interaction` manifest/commands/expect — actual ServerLevel item-use
  probe with a private non-OP FakePlayer, real inventory/world/refund behavior.
  Both shared-batch twins exclude its fresh fixture. One actual exact-M canonical
  run passes1,182 assertions/PASS1/FAIL0 and natural harness/server0; preserve its
  ten-file evidence rather than repeating it.

Source4550b97 preserves ordinary missing/blocked preflight refusal before
spending. An explicit false placement restores that attempted payment;
unattempted resources are refunded and already placed cells remain. A throwing
callback is ambiguous: keep that attempt paid, refund unattempted resources and
propagate the error. This is not global world rollback or universal zero loss.
The representative real capture/stamp/refusal/refund proof closes this changed
world/inventory boundary. It does not prove a later Blueprint save/reopen,
client/UX acceptance or whole T3. No drone/network-builder framework or new
payload is added. Further runtime work needs a newly changed assumption, not
the old unready-probe status or a documentation-only descendant.

## C3 sources, primitive assembly and generation

- `source/SourceLogic.java` / `source/SourceSite.java` — source costs, environmental eligibility
  and site faults. Declare environmental inputs; do not relabel finite veins renewable.
- `recipe/RenewableProcesses.java` — aggregate/biomass and downstream F0 routes.
- `recipe/BootstrapAssembly.java` — Bench/Assembler projections of existing craft quantities.
  The Bench covers Bootstrap/four vanilla crafts, never a Relay Matrix. The family-wide
  `VerifyRecipes` guard preserves the T0 hand-crafted exit (ADR-0137).
- `item/CrankHandleItem.java` — bounded bootstrap/emergency F0 work, not perpetual manual power.
- `machine/GeneratorKind.java` and the thermal generator implementation — shared F0/F1
  behavior; biomass registration follows supplier availability. Probe-before-drain alone does
  not guarantee exact extraction or safe handling of partial drains.
- Process kinds/block entities/menus/screens and registries — new source faults/enum cases
  must reach Forge, JEI, client effects, names and tooltips. Output slots append from two to
  five without moving old indexes; ten menu data fields include site faults.

Review global per-tick `heldConditions` before certifying the intended Firing regulation and
1450 C ceiling. Preserve `WorkRecipe`, `ItemLayout=3`, output reservations and single work
execution per tick. Recalculate the pre-C3 graph/Atlas/craft counts above and `ironBudget`
two-versus-three from source, not by weakening assertions.
Cancelled asset-generator changes and generated/state-fixture outputs exist without final QC;
review them before regeneration or acceptance.

## Control Matrix and Flux rating

- `energy/FluxTier.java` — the aligned F0-F15 rates and display names.
- `matrix/MatrixArchitecture.java` — Relay through Causal frontier ranges and retrospective
  manufacturing support.
- `matrix/ControlMatrixSpec.java` — physical rating, substitution and validation.
- `item/ControlMatrixItem.java` — stack persistence, names and tooltips.
- `matrix/ControlMatrixNbt.java` — the unchanged `Rating` key, legacy default and lossless numeric
  validation. `VerifyPersistence` exercises binary NBT and packet round-trips; `matrix-lifecycle`
  exercises the actual registered item and Assembler in Forge.
- `registry/ModItems.java`, `registry/BlockCatalogue.java` and `tools/assetgen/` — registration
  and generated functional art.

There is no T0 Control Matrix. Matrix rating Tn uses the same ordinal as Flux Fn; a higher
physical rating may substitute for a lower one, but an architecture cannot carry a rating above
its frontier range. A missing rating tag means that architecture's first frontier for vanilla
recipe compatibility; malformed explicit data is invalid rather than silently upgraded.
`VerifyEnergy` pins all sixteen rates and `VerifyControlMatrix` pins architecture and substitution
policy ([ADR-0107](DECISIONS.md#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)).

## Fluid

- `fluid/FluidState.java` and `fluid/FluidLogic.java` — volume, temperature, pressure.
  Ids are strings, not a Forge fluid type.
- `fluid/FluidBuffer.java` — each buffer has one native owner.
- `fluid/NativeFluidStore.java` — private snapshots/receipts and synchronous batch withdrawal,
  with explicit consumer `Settlement.apply/restore`. Restoration never uses public insertion.
- `container/FluidInsert.java` — source identity, backing-store deduplication and opposite-face
  permissions shared by selection and commit. Unsupported handlers do not participate.
- `fluid/FluidEndpoint.java` — native store bridge for the seven logistics endpoints and
  `ProcessMachineBlockEntity`; external capability restrictions are unchanged.
- `forge/.../fluid/forge/MachineFluidCapability.java` — the Forge `FLUID_HANDLER` map.

`tools/checks/VerifyFluid.java` requires the ids in `FluidLogic` and `ProcessLogic`
to be the same strings. Ambient gases do not travel the Clay Conduit; a tank holds
them. The catalogue is [`PROCESSES.md`](PROCESSES.md#the-fluid-catalogue).
The same suite exercises real native/process completion: split and extract-only sources,
aliases, stale identity/state/quota/permissions, boolean and exceptional aborts, complete
consumer/source restoration, deferred callbacks and exact retries. Source quota mutation is
silent until settlement. Preserve original exceptions and all remaining compensations if
one restoration reports an error; no VM/crash or external-handler guarantee is implied.

## Byproduct sink

- Vented fluids are `OutputSpec.ventedFluid` calls in `ProcessGraph`.
- Capture is `ProcessMachineBlockEntity`.
- The Thermal Generator burns only `FluidLogic.CARBON_MONOXIDE`
  (`machine/ThermalGeneratorBlockEntity.java`).

ADR-0131 supersedes ADR-0036's universal consumer requirement with honest classification and
handling. Current consumers must exist; future plans differ from uncertain potential.
Inert waste may be discarded, hazards need real handling and losses/emissions need not be
items. Matter deconstruction is not proof of recursive renewable supply.
Sulfur dioxide belongs in the tank/contact route, not generator fuel.

## Registration

- `Grindless.init` registers blocks, items, block entities, menus, recipe serializers,
  sounds and the creative tab, in that order.
- `registry/ModBlocks.java` registers the block and its block item together.
- `registry/ModBlockEntities.java` — one type per block that ticks.
- `registry/ModItems.java` and `material/SupplyCatalogue.java` — supplied forms.
- `assets/grindless/lang/en_us.json`.
- Assets are generated from `BlockCatalogue` by `tools/generate-assets.sh`
  ([ADR-0048](DECISIONS.md#adr-0048--assets-are-generated-from-the-jdk-with-a-named-list-of-what-cannot-be)).

`VerifyAssets` fails when `BlockCatalogue` and `ModBlocks` disagree. A change to
registration, a block entity or loader code also runs `tools/smoke-boot.sh`
([ADR-0049](DECISIONS.md#adr-0049--a-green-build-must-include-a-booted-server)).

## Worn equipment

- `item/HarnessLogic.java` and `item/ExosuitLogic.java` — the suit grids: how many
  slots a chassis has and which modules fit it.
- `item/HarnessItem.java`, `item/ExosuitItem.java`, `item/SuitModuleItem.java`,
  `item/FluxCellItem.java` — the pieces, the modules and the buffer.

`VerifyHarness` and `VerifyExosuit` hold the grid sizes. A chassis never generates FU
([ADR-0102](DECISIONS.md#adr-0102--the-voltaic-harness-has-one-slot-and-no-generator),
[ADR-0103](DECISIONS.md#adr-0103--the-exosuit-taps-a-pylon-and-does-not-generate)); the
Network Tap draws from pylon coverage and the drill's cell is not the suit's cell.

## Worlds and travel

- `structure/GroundArray*.java` — the fixed 3×3 ring. Not the multiblock kernel.
- `planet/`, `star/`, `station/`, `centre/` — Luna, the Drift, the station ride and the
  sealed chamber. A `*Catalogue` names the destination, a `*Travel` moves the player and
  a `*ReturnData` remembers where they left from.
- `flight/` — the launch pad and the survey rocket.

`VerifyPlanet`, `VerifyStarward`, `VerifyStation`, `VerifyRocket` and `VerifyCentre` hold
the FU each departure draws and that no return draws again. A link block that no longer
moves a player stays registered rather than being deleted
([ADR-0098](DECISIONS.md#adr-0098--the-supraluminal-station-is-the-ride-off-the-star)).
The first four checks also pin that the prototype transport has no generated survival recipe.

## Quests and the guide

- `quest/QuestCatalogue.java` and `quest/GuideCatalogue.java` — the lines and the pages.
- `QuestCatalogue.STARTER_SUPPLIES` — the first, explicitly `FREE` task, not an item
  objective with a zero count. `Task.rewards()` owns the complete finite allocation;
  the singular accessor is only compatibility for existing single-stack tasks.
- `client/FieldGuideScreen.java` — wraps each topic into physical pages; `GuideCatalogue.paginate`
  must retain every wrapped line rather than silently clipping a long topic or translation.
- `quest/QuestLogic.java`, `quest/QuestEvidence.java`, `quest/QuestProgress.java` — what
  a claim watches, and per-player progress. `menu/QuestBookMenu.java` synchronizes each task's
  live item count alongside its claim status for the client screen.
- `quest/QuestInventoryEvidence.java` — counts each inventory slot once; vanilla's inventory
  already includes the offhand. Matrix evidence checks both the item and exact valid rating.
- `quest/QuestRewardDelivery.java` — stages the complete batch with vanilla
  `SimpleContainer` stacking over copied main-inventory slots. Insufficient capacity
  records no claim, changes no inventory and drops nothing; record the existing
  one-time ledger only after a delivery can be committed on the server thread.
- `quest/QuestBookAccess.java` and Forge's `quest/QuestBookAccessEvents` — recover
  one existing utility book on login/respawn while starter supplies are unclaimed,
  checking the real inventory including offhand first. A full inventory defers
  delivery and retries when space becomes available. Recovery never grants supplies
  or changes claims, and does not depend on the optional renderer.
- `recipe/IngredientSpec.java` and `recipe/OutputSpec.java` — process inputs and outputs; their
  optional Matrix rating makes the T1-to-T2 hand-off physical rather than a global unlock.

`VerifyQuest` holds that a claim never consumes its evidence and never gates a machine
([ADR-0100](DECISIONS.md#adr-0100--the-quest-book-tracks-the-route-and-does-not-gate-it)).
The Field Guide states the ceramic-workshop Bootstrap contract and points from its five-Matrix
exit through the complete Voltaic factory; it remains readable independently of quest claims.
The Dynamo/Extractor still spend two iron; optional vanilla equipment is outside that budget.
The active route has twenty-six tasks in eleven lines, starting with a free finite
starter allocation, with ceramics before calibration and the physical Relay Matrix between
calibration and the first powered machines; it ends at the F2 Relay Matrix, contains no dimension
evidence and does not advertise spatial campaign prototypes.
Early quest rewards must never supply a core, insulator or Matrix that bypasses the workshop.
See [the starter-kit contract](testing/t0-starter-kit.md) for exact allocation,
inventory deferral, compatibility and the still-pending native/client observations.

The integrated experience catalogue/teaching supersedes the historical26-task
description above:46 IDs,32 T0/14 T1, retained old claims and a harmless Guide
redirect. `VerifyWorkshopPorts` exercises the actual shared port component
projector and both renderer call sites. Standalone checks use vanilla inputs,
not newly allocated intrusive registry fixtures. Positive registered Matrix
server claims and exact paid Guide settlement belong to the existing Forge
`fluid/forge/WorkshopInterfaceChecks` command and its isolated26-marker
`workshop-ports` smoke; common green is not that native proof.

## Multiblock

There is no parametric kernel. The Ground Array is a fixed shape, not one. Do not add a kernel
ahead of its slice. Shape, hatches and the Arc
Reactor are specified in
[`MACHINES.md`](MACHINES.md#multiblocks-shape-is-a-parameter),
[ADR-0022](DECISIONS.md#adr-0022--parametric-multiblocks-instead-of-fixed-schematics)
and [ADR-0067](DECISIONS.md#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier).
The schedule is slice AD in [`BUILD-OUT.md`](BUILD-OUT.md).

Slice F starts when a machine needs hatches or size. That machine is the Arc Reactor,
not the Arc Furnace.

ADR-0129 adds fixed, process-driven T0 workshop structures independently of that future kernel.
`structure/WorkshopStructure.java` owns local cells and clockwise rotations; the process block
entity reads only loaded world cells before working. The Firing Chamber is its controller plus
seven bricks in a 2x2x2 volume. Never persist a trusted "formed" flag or force-load a missing cell.
The Settling Trough requires a same-facing channel then screen behind its controller, with air
above those two parts. `WorkshopPartBlock` has facing but no artificial status or block entity.
`WorkshopGeometry` is the shared box source for collision, JSON models and the schematic preview.
The controller top remains usable by hoppers. Missing, wrong-facing, covered and unloaded cells
have separate synchronized faults; every fault enum addition appends to existing values.

`BlockCatalogue.Entry.hasStatus()` is false for a single visual variant. The generator and
state-smoke keys must omit the nonexistent property rather than create a one-value Minecraft
enum. `VerifyT0Preparation` checks the static parts, exact texture-end continuity and the
idle/active preview in addition to the ordinary controller models.

`ProcessMachineBlockEntity` retains paid work under `WorkRecipe`, separately from the live
`Recipe` selection. An incomplete selection may clear when inputs become invalid, but a
completed batch retains its selected recipe while primary/secondary fluids are unavailable.
Legacy saves derive the owner from `Recipe`. A different recipe resets work; quantity changes,
starvation and structure repair do not. A missing live selection, including a retired generated
row, suspends its paid `WorkRecipe` rather than clearing or transferring that owner.
Completed input waits poll without drawing more Flux;
missing fluid is `STARVED`, not output `BLOCKED`. No new persistence keys are introduced.
Outputs are reserved before any ingredient is consumed. Fluid arrival wakes work; pending output
has its own bounded export subscription, including after a neighboring inventory is emptied.
F0 kinds pass their rating into the base constructor before buffer creation, rather than reading
an uninitialized subclass field. Existing menu ordinals and `ItemLayout=3` stay unchanged.
`FluidOutputQuota` records only completed non-vented output. A front-side subscription exports
that quota without draining a ready recipe's primary batch. Consumption/extraction debits it;
missing legacy ownership stays unmarked, and invalid explicit ownership is logged, never used
to drain input. `FluidEndpoint.acceptedAmount` is shared by execution and Forge simulation.
Process work runs at most once per world tick despite subscription changes (ADR-0132).

## Sustainability export and dashboard ownership

The four static runtime files under `tools/dashboard/` implement the renderer.
See [its schema-1 contract](dashboard/README.md): adjacent `data.json`, no synthetic fallback,
canonical labels and explicit source-bound evidence. `AuditReports.document` supplies the
one canonical Result representation; common `DashboardProjection` derives schema-1 views,
and game-edge `tools/dashboard/DashboardExport.java` binds actual source/binary provenance.
`export.ps1`/`.sh` and `serve.ps1`/`.sh` generate and serve the eight-file ignored artifact;
`DashboardServer.java` permits only its explicit file whitelist on loopback.
Both exporters compile every tool source from the selected Root, not the script's worktree.
`tests/VerifyDashboardProjection.java`, `real-data-check.js` and paired
`verify-export-root` tests cover exact projections, real-data browser behavior and cross-root
source selection. The GitHub workflow exports/uploads only same-repository PR/push artifacts,
without a public deployment. Fixtures and artifact publication are not progression evidence.

Astra owns canonical schema/annotations. A bounded worker extracts real `ProcessGraph`,
`SupplyCatalogue`, local tags, `BootstrapRecipes`, `T1Recipes`, other authored JSON and quests
from an agreed pinned revision. Existing catalogues retain quantity authority; serialize shared
metadata/catalogue edits. Canonical inventory and the active conditional T0 model are reviewed
and verified on `debd3ae`; global future progression remains report-only. Worker real-data
and server evidence is source-bound; the composed source must generate its own artifact
and CI result. Player communication and dashboard views must project the same model.
The [ADR-0135](DECISIONS.md#adr-0135--sustainability-is-a-fixed-point-audit-over-live-authorities-and-typed-annotations)
S1 core is implemented and verified on `639881f`: loader-free `sustainability` model/auditor,
`AuthorityAdapters`, `TagResolver`, `ConstructionReserve`, `DebtManifest` and `AuditReports`.
`VerifySustainability` and `VerifySustainabilityAdapters` cover its proof/report seams;
`tools/sustainability/debt.json` has no invented live exceptions. Generated
`build/sustainability/` reports keep the global report-only verdict separate from the landed
`active_t0_conditional_gate`; neither is a human or Tier pass.
Circulating finite inventory is not unlimited net supply. Construction and hazardous handling
share one exact reserve witness; unresolved tag precedence stays unknown, never a sorted-hash
guess. Source identities must cover normalized authorities and the executed implementation.
S2 proceeds under
[ADR-0141](DECISIONS.md#adr-0141--canonical-t0-closure-uses-real-authorities-and-conditional-establishment).
Its integrated pack binding owns `CanonicalPack`, `TagResolver`, adapter/report
changes, the two Gradle classpath-export tasks and `VerifySustainabilityRegistry` in tools.
Actual registry/bootstrap access and JDK declaration analysis must stay outside the pure
model/auditor and runtime UI; partial syntax coverage cannot establish absence, and accepted
helper calls must bind to the validated signature. Do not keep a second regex inventory.
The shared `WorkshopStructure.forFamily` seam derives formation requirements rather than
duplicating physical counts. Construction, unit-vector normalization, conditional work and
finite manual-exit changes extend existing model checks; unchanged native proofs are reused.
The compatible `Route` constructor is not canonical automation evidence. Manual exit
production is required, but its Matrix is not an automated recurring root or a Bench output.
Follow the current validation ledger; a core test pass is not renewable-supply certification.
`T0Sustainability` authors the active conditional profile and `T0ConditionalGate` attributes
every finding's scope. `VerifySustainabilityT0` is a hard discovered check, not a global/Tier
acceptance switch. `ConstructionReserve` must prove shared custody, simultaneous non-consumed
requirements and real canonical exit production; the same Quest Book is held before claims.
Refresh game-edge exports on composed source, serializing or isolating Minecraft log output.

The optional quest UI follows `QuestBookItem` -> `QuestBackendPlatform` -> the Forge
`quest/forge/QuestBackendPlatformImpl`. All claims, including legacy menu buttons and the
sender-owned Heracles packet, delegate to common `QuestClaimService`; loader types stay out
of common. `QuestProjection` derives stable IDs from `QuestCatalogue`. Forge's
`HeraclesDefinitions`, `GrindlessClaimTask`, `HeraclesClaimProjection`, network and client
widget own only the framework projection. Native completion means an existing legacy claim,
not sticky possession; native rewards/autoclaim stay empty and progress stays individual.
Definitions and their ownership proof share one atomically replaced archive; collisions,
modified content and unpublished loose files are preserved/refused, never silently adopted.
`VerifyQuestClaims` covers the service; `tools/quest-checks/VerifyHeracles.java`, through
`:forge:verifyQuestBackend`, checks the actual selected codecs/loader, recovery and packets.
Those pure contracts are not a Forge boot or human save/client acceptance.
`VerifySustainabilityRegistry --quest-binding-only` parses the16 pinned common/Forge
authorities without explicit game bootstrap. Its50 closed AST profiles cover the claim/access methods and
reject alternate imports/overloads/callbacks or removed book/sender/evidence/reward guards.
The same `mechanics_binding:QuestBookItem_QuestBookMenu` key keeps the expanded source
fingerprint. Source-only inputs and the combined live export must retain separate identities.

Stable asset readiness lives in `docs/assets/t0-asset-manifest.json`; `source_bound_readiness`
is its complete current inventory, while legacy arrays preserve candidate descriptors.
The worker inventoried existing source/state/model/icon/effect coverage, not approved art.
The active section of `docs/testing/t0-survival-acceptance.md` controls client preparation;
the predecessor checklist is not the post-S2 route or an instruction to advance into T2.
`tools/assetgen/.../BlockModels.java` owns generated face references: internal boxes use bare
keys, the JSON writer adds one `#`, and shared `WorkshopGeometry` references are normalized
only at that boundary. `WorkshopPreview` retains its convention. The shared model root
inherits `minecraft:block/block` for native item display metadata, retaining local
elements/UV/textures and existing BlockItem parents; assembled pylon paths remain
separate. `FormTextures.bootstrapDataCore`, `calibratedDataCore` and `relayMatrix`
own the T0 physical forms, while legacy `dataCore` still serves the unchanged advanced
item. Refine local materials without changing global lighting/palettes or placed
machine textures (ADR-0146). `VerifyAssets` checks
local face resolution/inherited aliases and cycles. The corrected22 model JSONs now come
from two byte-identical staged runs; geometry, UVs and PNG/WAV bytes are unchanged.
Keep staging/raw-checkout EOL identities distinct. Generate in staging while other workers
own assets/lang, and never call alias resolution a client visual pass.

`FormTextures.atlas`, `questBook` and `fieldGuide` share local paper/binding materials,
not a global lighting change. `GenerateAssets --t0-guides-only` selects the same three
writers as normal generation and must never touch any other output. The JDK-only
`VerifyT0GuideSprites` checks their pixel contracts and the exact nine reviewed PNG hashes;
the normal behavior runner discovers it automatically. Two identical selective stages
and exact installed byte parity establish only the bounded generation/pixel contracts;
child/coordinator source-pixel reviews are not native-client or human art acceptance.
Preserve cyan/gold/teal function cues, registry/model identities and all placed-machine art.

Resource communication follows
[ADR-0143](DECISIONS.md#adr-0143--player-resource-guidance-is-a-generated-view-of-canonical-declarations).
The `guidance` package owns the typed projection, JSON/fingerprint, context selection and
runtime cache. `client/ResourceGuidanceClient` is the shared native formatter; Forge's
`ResourceGuidanceTooltips` includes annotated vanilla items and real fluid containers.
Field Guide, Atlas, machine and quest displays reuse those adapters.
`tools/generate-resource-guidance.ps1`/`.sh` use the shared canonical exporter/bootstrap;
`VerifyResourceGuidance` checks projection/runtime/drift boundaries. The real resource at
`common/src/main/resources/assets/grindless/guidance/resources.json` must be regenerated
from the current canonical Result after quest reward/access edges change; run the existing
writer, then its `-CheckOnly` mode, never repair the fingerprint by hand or relax equality.
The integrated03970 CI exposes the stale asset separately from the obsolete no-kit prose
assertion; [the starter repair record](testing/t0-starter-kit.md#integrated03970-executable-red)
retains the exact failures and execution ownership. Its sidecar reuses complete implementation identities and
projector-class hashes separately from the stable declaration fingerprint; do not omit
executed helpers or copy acceptance flags into the runtime resource.
Canonical Result metadata is the only declaration authority.
Installed-client hover must not call the source-root
`AuthorityAdapters` or invent an entry profile; generated fields are checked for drift.
Actual recipe objects/amounts stay in the existing process/Atlas/JEI authorities. Guide
prose must not make finite extraction or vanilla farms the permanent T0 source, or turn a
declaration into world/Tier acceptance.
Keep a physical Matrix's validated identity separate from its declaration lookup. Exact
rated metadata wins; only the same item's native default spec may use ordinary metadata
as context. Ordinary ITEM quests stay unrated and non-default/invalid ratings never inherit
the T0 declaration merely to fill a missing tooltip.

The functional-port follow-up starts after current integration with an audit of Firing/Trough/
Ground Array and all authored structures. It does not authorize starting the old kernel slice
early. Astra decides roles, positions, ownership, aggregated capabilities and migration before
workers implement actual buses/hatches and transaction/chunk/reform tests. AE2 is optional and
version/API-verified, never a fictional or mandatory core integration.

## Checks

- `tools/sound-design/sound-design.ps1` preserves UTC JSON strings across modern Core
  and WinPS5. Its grant/receipt checks must not accept coerced DateTime objects.
  Focused `-JsonOnly` tests do not replace default CI or rewrite the spent calibration.
- `tools/sound-design/test-generation-flow.ps1` / `.sh` exercise the real
  `Invoke-Generate` reservation/storage/recovery sequence without a provider or codec.
  Only test authorization/checkpoint, transport and QC boundaries are substituted;
  deliberately unauthorized markers must fail the real grant guard. The default
  `test-sound-design` suite invokes these ten isolated cases. Never substitute a real
  credential, existing ledger or production output directory into this harness.
- `tools/smoke/first-blueprint.commands` starts `first-blueprint-pack`, reusing the
  loaded-chunk/single-tagged-item observation pattern. The paired standalone
  `verify-blueprint-fixture` checks compare source contracts and mutate in memory;
  they are not a new Minecraft execution or a Blueprint gameplay proof.
- `tools/smoke/recipe-cache-reload/` and `recipe-cache-reload-optional/` author one
  ADR-0153 idle-miss/reload boundary with real tag predicates, immutable setup,
  exact batch/FU accounting and a tick-rate bound. Read its `README.txt` first:
  actual444 native red /559 green are qualified and the scenario is registered.
  The paired runners stage one
  `SMOKE_OPTIONAL_DATAPACK` explicitly in `initial-disabled-packs`, validate
  optional-mode paths/basenames and restore its environment per scenario.
  Reject linked ancestors of both input and world paths before replacing the
  diagnostic world; a leaf-only link check or logical/physical prefix comparison
  is insufficient. PS must choose one absolute FileSystem path from the PS
  provider/location, not .NET CurrentDirectory, and reuse it for checks and reads.
  Resolve `~` using that provider's Home anchor, not as a relative folder; do not
  invent `~user` expansion. Inspect logical ancestors root-first before dot
  normalization, including ROOT and Home tails.
  Publication wait has a separate bounded failure and never
  spends the confirmed wake budget; its reset must not reset paid-work samples.
  PS batch excludes reload/optional cases from its shared world.
  `verify-optional-datapack.ps1` / `.sh`, invoked by `VerifySmokeHarness`, exercise
  the real helper/dispatch plus phase-budget source contracts without a JVM/server;
  PS passes86 including c7 divergent-cwd and ae054 Home regressions, with locations
  and provider Home restored. The earlier75 did not cover Home.
  The limited local Git/MSYS bundle lacks `ln`, but exact444 Linux CI executes the
  full optional helper and Energy no-callback assertions successfully. Combined559
  PC Java tooling also executes OPTIONAL/RETENTION/BATCH once each. Register new
  fixtures only after actual qualification; never let donor energy/inventory/
  neighbor mutation substitute for reload wakeup.
- `tools/smoke-boot.ps1` / `.sh` accept only absent/0/1 for `SMOKE_KEEP_WORLD`.
  Validate before preparation/mutation;1 skips only final world deletion, not
  startup reset, preflight, process cleanup, shutdown or exit-status checks.
  Retained/absent diagnostics never certify a PASS. Preserve only the existing
  fixed smoke world, with no copying/path hook. Inspect it only after harness0
  and natural server0, then explicitly clean that exact world before another run.
  `verify-smoke-world-retention.ps1` / `.sh`, via `VerifySmokeHarness`, pass37 PS
  and43 Windows Git/MSYS helper/entrypoint/EXIT contracts without a JVM, plus the
  exact444 Linux Java-wrapper proof. Those helper proofs alone do not establish
  persistence. A01's separate559 native green and four actual stopped-world Luna
  checks now qualify the saved-accounting boundary, not restart/crash/player saves.

- Process UI uses `MachineMenuLayout` for real item-slot geometry and `MachineFluidView`
  for the read-only primary-store projection. `FluidClientPlatform` delegates native water
  rendering to `client/forge/FluidClientPlatformImpl` in the Forge module; loader types never
  enter common. `ProcessMachineBlockEntity.syncData` appends water confirmation at menu
  field10 (payload11) only on a known server. Fields0..9, item slots and save layout stay
  unchanged. An unknown ID is not an absent tank; output quota is included in stored volume.
  `VerifyMachineUi` covers layout, roles and restored/unconfirmed identities; production
  transformation and client rendering retain their own bounded proof requirements.
- `MachineEffects` is silent for ordinary STARVED and generic OUT_OF_BAND audio while
  retaining visual feedback, working/generating effects and the pylon's explicit warning.
  Do not mute unrelated alert systems or infer an asset licence from the code licence.
- `docs/assets/t0-sound-design.json` and `tools/sound-design/` define the bounded
  development-only calibration (ADR-0145). Preserve the historical manifest/contract,
  exhausted three-request grant and attribution; later approval/import authority is
  separate. ADR-0152's selected Clay Mixer wet-work purpose is not supported by that
  Quern/3/Astra generation contract. No credentials, approved local grants or discarded
  candidates enter Git; never auto-resend an ambiguous provider request.
- ADR-0146 preserves the placed machine identity and targets item materiality. Inspect
  real item-model parents/transforms and reuse block geometry before adding sprites or
  a renderer; an intake sheet of front textures does not establish flat item rendering.
- Client packs use root `build.gradle` tasks `buildModpack` and `verifyModpack`, with thin
  `tools/build-modpack.ps1`/`.sh` twins. `pack/curseforge/manifest.json` owns pins;
  `profiles.json` selects project IDs and `dependency-metadata.json` verifies them.
  Preserve expanded defaults/archive identity and the minimal preparation profile.
  The additive playable profile selects seven existing pins and has a source-bound,
  explicitly non-RC identity. Only `public-overrides.json` entries from the committed
  public source root may supply `config/` or `defaultconfigs/` files; reject path escapes,
  untracked sources/targets and portable destination collisions. Provenance distinguishes
  defaults from exact override destinations/hashes. The shared writer checks the full
  archive whitelist, including notices; mutation/synthetic archive fixtures belong to
  `verifyModpack`, not a second builder or a remapped-runtime/client acceptance claim.
  Keep pack-only changes below the root application's provenance boundary.
- [Risk-based proof reuse](testing/test-strategy.md) (ADR-0140) classifies remaining work
  and lists reusable proofs/assumptions. Validate ordinary content structurally through the
  proven mechanism; select native/manual checks only for an uncovered or invalidated behavior.
  New data may still alter selection, bounds or save meaning; review the effect, not the extension.
- `tools/smoke/renewable-cycle/`, its `.expect`/`.timeout` and manifest entry — verified
  ordinary native source-window/material/core proof. Downstream command staging/Mk1, derived
  water costs and the manual Matrix craft are explicit limits, not full factory acceptance.
  The revised oracle accounts for actual unburnt startup coal; its explicit
  `oracle-regression.commands`/`.expect` invokes ten synthetic native cases separately from
  production. Those markers are not a substitute for the ordinary24-marker scenario.
  Native execution of this correction remains pending; runner cleanup must not erase the
  only diagnostic evidence before it is retained.
- `tools/run-smokes.ps1` / `.sh` read optional per-scenario `.timeout` files; an explicit
  `SMOKE_EXPECT_TIMEOUT` wins over that assertion budget and the unchanged 120-second
  default. Validate selected metadata before boot, retain startup `SMOKE_TIMEOUT`, and do
  not leak one scenario's budget to the next or the caller (ADR-0139). CI uses these runners.
- `tools/run-smokes-batch.ps1` / `.sh` — the ADR-0111 development shortcut aggregates
  commands/expected markers in manifest order and invokes the existing native smoke boot
  once. Keep `foreign-providers`, `states`, `recipe-cache-reload`,
  `extractor-backpressure` and any selected `<name>-optional` path out of that world;
  refuse isolated-only requests and announce mixed exclusions (ADR-0154/0155).
  A01's post-stop VeinData oracle requires exactly two worked chunks/total two,
  independent of optional-pack discovery. Clear both inherited pack selections for the boot.
  Do not substitute isolated loops, add a second server runner, or apply per-scenario
  `.timeout` metadata to this shared batch. Existing boot owns handshake, shutdown,
  cleanup and the batch-wide environment budgets. Resolve relative Bash roots with
  `CDPATH` neutralized only for `cd`; retain the caller's environment and visible errors.
  `tools/checks/Test-SmokesBatch.ps1` / `.sh` exercise selection, aggregation,
  dispatch and failures with fake boots. `VerifySmokeHarness` invokes the platform
  twin once, alongside its existing optional-pack check, not a new CI entry.
  This is not Forge, VeinData or datapack reload acceptance.
- `tools/validate.ps1` / `.sh` — the fast environment/build/behavior/documentation entry point;
  opt into full smokes explicitly. Per-stage results live under `build/validation/<run-id>/`.
  See [the testing strategy](testing/test-strategy.md).
- CI's dashboard step follows successful foundation validation, checks the Bash root-selection
  twin and invokes `tools/dashboard/export.sh --mode pr`. The same-repository guard also
  applies to upload; missing files fail rather than yield an empty successful artifact.
  Keep the six native smoke shards and their manifest/limits unchanged.
- `tools/checks/Verify*.java`, via `tools/run-checks.sh .` after `:common:build`.
- `tools/checks/verify-runner-jdk.ps1`/`.sh` test the actual JDK-probe helper with real
  JAVA_TOOL_OPTIONS and rejected version/exit/stream cases. Native stderr is diagnostic
  data, not a failed version by itself; a valid17 line never overrides a nonzero exit.
  The Windows probe drains both pipes asynchronously. CI runs the Bash twin before the
  official suite; do not substitute debugger hooks or silently remove JVM limits.
- `tools/smoke/first-exosuit.commands` starts `first-exosuit-pack`: one summon per tagged
  item after actual chunk loading, bounded polling of all four IDs, then tagged cleanup.
  `VerifyExosuitFixture` checks the source contract, not native entity scheduling. Failure
  diagnostics use explicit retained `say` emitters, not silent function data-get feedback.
- `tools/checks/VerifySmokeHarness.java` — executes the native runner with fake boots in a
  temporary fixture. Report labels, actual dispatch and failure propagation must agree; Bash
  report variables must remain local (ADR-0127).
  Assertion-budget cases run both native twins: defaults, override, LF/CRLF, bounds,
  malformed selected metadata, consecutive scenarios and caller restoration.
  `-Only` / `--only` accepts comma-separated names through the actual CLI; unknown or empty
  selections fail before any boot, never silently shrinking or expanding the requested scope.
- `tools/checks/VerifyT0Preparation.java` — verifies the active
  `docs/tiers/t0-production-graph.json` references and topology, plus the original textures/models
  named by `docs/assets/t0-asset-manifest.json`. These are audit projections, never new gameplay
  sources of truth or proof of a balanced economy.
- `tools/checks/VerifyWorkshop.java` — exact F0 counts, non-amplifying ceramic solid potential,
  reachable sinks and every rotated/missing chamber cell. `workshop` is the registered Forge
  counterpart for full cycles, finite-water wakeup, NBT reconstruction and blocked-output recovery.
  `settling` proves actual separation in four orientations, wrong-facing/covered suspension,
  reconstructed fluid/work and exact conduit recovery. Scheduled checks wait boundedly for
  observed work rather than equating world age with block-entity processing ticks.
- `tools/check-links.ps1` — relative links and the ADR index.
- `tools/smoke/scenarios.txt` — the scenario list, when a block, a menu or a
  boot-visible item changes. Run it with `tools/run-smokes.ps1` or `run-smokes.sh`.
- `forge/build.gradle` — `prepareSmokeServer` installs the pinned official server and stages the
  actual remapped mod jar plus Architectury. Boot scripts run that artifact, not development
  transformer classes; generated libraries/worlds stay under ignored `forge/run/smoke-server`.
  The installer checks its path/size inventory before accepting an up-to-date runtime.

Every tool here must have a `.ps1` and a `.sh` twin, because the desk that validates a
branch runs Windows ([ADR-0106](DECISIONS.md#adr-0106--code-is-written-remotely-it-is-validated-on-the-windows-desk)).
A new scenario is a line in `scenarios.txt` and its `<name>.commands` / `<name>.expect`
files, never a step in `ci.yml`
([ADR-0105](DECISIONS.md#adr-0105--smoke-scenarios-are-a-list-sharded-across-parallel-ci-jobs)).

The Planner item-existence scenario uses a chest stack, not a transient entity selector.
Keep its exact registered ID/count assertion; it does not exercise interactive planner use.

A new graph row updates the size assertion in `VerifyRecipes`. A new block updates
`BlockCatalogue`, which `VerifyAssets` reads. Arithmetic policy stays in a check
that does not boot Minecraft, the way `ProcessGraph`, `FluidLogic` and `AtlasLogic`
already do.

Both behavior runners ask `:common:writeCheckClasspath` for Gradle's resolved runtime classpath
and current classes. They honor `GRADLE_USER_HOME` without scanning unrelated caches or
silently swallowing long-path errors.

Smoke expectations may be scheduled functions; both boot runners wait at most
`SMOKE_EXPECT_TIMEOUT` (120 seconds by default). They fail on fatal scenario errors or an
unclean/nonzero shutdown, rather than reporting a forced cleanup as a pass. Smoke servers
bind only to loopback. The canonical production path exits cleanly; the optional
`:forge:runServer` development-JVM hang remains in the [validation ledger](testing/latest-validation.md)
and must not be reported as an upstream fix (ADR-0126).

## Factory World

- `factory/FactoryCatalogue.java` — the fixed-noon flat-floor contract.
- `factory/FactoryPortalBlock.java` and `FactoryTravel.java` — a reusable physical portal and
  player-specific round trip. It does not consume power or replace a campaign travel system.
- `factory/FactoryReturnData.java` — saved return endpoints, stored on the overworld.
- `data/grindless/dimension/factory.json`, its type and biome — bedrock, three dirt, one grass;
  no structures, terrain features, rain or hostile spawns.

The Factory World keeps the installed Grindless material pool through `PlanetCatalogue.veins`.
Its resource fields are surveyed/depleted ChunkVeins, not exposed ore terrain: clean construction
space must never become an infinite free resource. `VerifyFactory` holds the floor, noon, safety,
portal component and finite-field contract (ADR-0121).

## Optional pack bridges

- `forge/.../jei/GrindlessJeiPlugin.java` — JEI entry point; generated recipes are read from
  `ProcessLookup`, never copied into a second recipe list.
- `forge/.../jei/GrindlessProcessCategory.java` — item/tag alternatives plus the non-native fluid
  conditions of a process. Rated item inputs and outputs use `ProcessLookup.resolve`, so the
  viewer preserves their NBT rather than substituting an untagged stack. It must keep the live
  graph visible even when no Forge fluid exists.
- `forge/.../jei/GrindlessCalibrationCategory.java` — the Research Terminal's physical cycle,
  which is not a JSON recipe and must therefore be registered explicitly.
- `pack/curseforge/manifest.json` — exact launcher-managed dependency file ids.
- `tools/build-modpack.ps1` and `.sh` — build the local jar and stage it as a pack override; they
  must keep the manifest version equal to `pack_version` in `gradle.properties`.

JEI is optional at runtime: the standalone Grindless jar must load without it. The T0 client pack
does not include AE2; its T4 recipe/energy/material integration needs a complete frontier slice,
not an unmodified early-game dependency (ADR-0120).

## SSH-first PC and Prism development

- `tools/pc-workflow.ps1` and `.sh` — local SSH transport and exact-revision synchronization
  into the isolated PC worktree; private host paths come from the caller's non-Git inventory.
- `tools/prism-dev.ps1` and `.sh` — host-side build/deployment into the separately owned
  engineering instance, using the existing pack, pin and provenance authorities.
- `tools/checks/Test-PrismDev.ps1` and `.sh` — isolated tooling fixtures, not game/client proof.
- `.github/workflows/ci.yml` — the independent Windows tooling job exercises both entrypoints
  without a JVM, SSH connection, download or real player data.
- [Operating runbook](operations/ssh-prism-workflow.md) — current identity, execution and
  player handoff contract.

Never change the original clone's branch, authentication or existing player instance.
Only a committed exact source and verified package may enter a stopped owned target.
Do not copy accounts, saves or private configuration; refuse foreign/tampered files.
Routine jar updates preserve launcher components and retain owned backups outside `mods/`.
Deployment is engineering preparation, not RC, Superflat or human acceptance (ADR-0151).
