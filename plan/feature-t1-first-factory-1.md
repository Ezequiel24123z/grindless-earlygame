---
goal: Begin T1 with canonical first-factory lines, live teaching and a reusable-system handbook
version: 3
date_created: 2026-10-21
last_updated: 2026-10-21
owner: Sol coordinator
status: 'In progress'
tags: [feature, t1, progression, documentation, reuse, compatibility]
---

# Introduction

![Status: In progress](https://img.shields.io/badge/status-In%20progress-yellow)

The owner explicitly requests T1 development while playing the intended
longitudinal game (ADR-0170), later opening isolated development through T15 and
baseline basic families from T1 (ADR-0173/0174). The original intakea64d44f remains
history; current source42654eb normally integrates native-baseline b38 and the
three bounded increments below. Root104de7bb is normally merged atab4d7bc.
The installed player build is now dd0/JAR29bc/ZIP79ce with the pristine existing-
world/options copy; preserve it and the original F/backup.
This first development increment integrates the already implemented first-factory
mechanisms into an explicit T1 contract and usable guidance; it does not declare
the entire tier finished or introduce a replacement processing engine.

The parent publishes426 and grants only2a93 the NEW
`integrated-family-qualification-20261021` build/cache window on that fixed source.
Results are pending, not passes. Parent/other workers have no PC/T0 access.
Documentation remains local; never Sync it under active stages/locks.

## 1. Requirements & Constraints

- **REQ-001**: Implement canonical T1 lines that reference actual recipe/craft/fuel authorities and are consumed by runtime quest guidance and an executable exporter.
- **REQ-002**: Cover F1 power/distribution, milling/reduction, renewable glass, forming/dies/coils/casings, Assembler reproduction and the physical F2 Relay Matrix exit.
- **REQ-003**: Mark finite extraction, optional logistics/storage, Kiln and Factory World branches explicitly; the F2 exit must not require optional Factory World access.
- **REQ-004**: Preserve all46 existing quest IDs/rewards/claims and every T0 task, recipe, starter allocation, physical F1 gate and save format.
- **REQ-005**: Differentiate T1 teaching from the old generic preview using live Atlas recipes and the shared line contract, without duplicating bills or claiming successful operation from possession.
- **REQ-006**: Document reusable authorities, invariants, safe extension seams, proof boundaries and concrete T1 uses with verified source references.
- **REQ-007**: Trace recurring required inputs to the existing mod-owned T0 base or declare an explicit unresolved dependency; repeatable finite extraction is not renewable.
- **REQ-008**: Follow ADR-0170: research/brainstorm/develop T1 during owner play, prioritize reported T0 blockers and integrate final T0 feedback/polish without discarding the reference world.
- **REQ-009**: Preserve source3a79a1f's baseline F1 Wire Mill and optional four-route `wire_forming` after assembly; no initial-F2 prerequisite, MK2, global ingredient matcher or storage change.
- **REQ-010**: Preserve source541cf6c's pressure-absent old constructors, meaningful zero absolute optimum, explicit positive half-width and shared conversion/projections; record ADR-0175 as proposed/unqualified, not a production pressure recipe.
- **REQ-011**: Preserve source4550b97's strict legacy Blueprint payload and read-only preflight. Ordinary missing/blocked refusal spends nothing; later throwing callbacks are ambiguous and do not have a global rollback/zero-loss guarantee.
- **REQ-012**: Preserve native46 old IDs/paid history within source50; an installed46-to50 managed-definition update remains a separate fail-closed backup/hash migration, not a raw world rewrite.
- **SEC-001**: These increments add no credential/provider/account changes, private paths in Git, generation charges or third-party implementation imports. Future permitted reuse follows ADR-0174 provenance/licence/notices before copying.
- **CON-001**: T1 work must not modify, redeploy or restart the owner's installed Prism T0 instance/worlds/configuration. A separately coordinated reported T0 fix uses the existing stopped-instance/backup/update contract, never an agent-initiated game interruption.
- **CON-002**: Development through T15 is separately authorized by ADR-0173; it is not T0 acceptance, a graphical pass, a T1 release or permission to deploy later-tier production.
- **CON-003**: All versions stay in `gradle.properties`; common remains loader-neutral; existing processing, transaction, storage, grid and quest engines remain the authorities.
- **CON-004**: Use isolated dependent branches and one integrated build/cache owner. Player gameplay has resource priority; no user process is stopped.
- **CON-005**: Only2a93 owns the current426 execution window. Publish/Sync coherent corrective source only after active stages/locks close and parent serialization permits it; documentation work has no execution or publication grant.
- **GUD-001**: Run focused changed-contract/data/quest proofs and reuse unchanged native mechanisms; add runtime work only for a concrete changed assumption.
- **PAT-001**: A line lists stable source IDs and dependencies; quantities, conditions, ticks, energy, catalysts and side streams are projected from the live catalogues.

## 2. Implementation Steps

### Implementation Phase 1

- **GOAL-001**: Establish the explicit development boundary and verified existing first-factory mechanisms.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-001 | Read AGENTS/current handoff, verify a64d44f and PR56's live base/head, inspect `T1Recipes`, `ProcessGraph`, `FabricationLogic`, `FluxTier` and actual quest teaching. Record ADR-0168/0169 without accepting T0. | Yes | 2026-10-21 |
| TASK-002 | Preserve the unchanged T0 testing/deployment boundary and create three isolated work lanes from the current experience branch. Keep root decisions and publication under the coordinator. | Yes | 2026-10-21 |

### Implementation Phase 2

- **GOAL-002**: Deliver one coherent first-factory contract, player guidance and reusable-system documentation.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-003 | Core ownera94c3085 implements `progression/T1Progression`: `Requirement`, immutable `Line(id, requirement, routeIds, requires)`, `lines()` and `byId()`. Resolve mandatory/optional/exit routes against existing authorities, including real Flux Cell crafting; add source-derived export and `VerifyT1*` contracts. No recipe quantities are redeclared. Depends on TASK-001. | Yes | 2026-10-21 |
| TASK-004 | Quest ownerac4962d5 maps actual T1 task IDs to the shared line contract, replaces generic previews for implemented first-factory tasks, preserves T0/claims/rewards and makes Factory World optional for `industrial_matrix`. Extend native/Heracles teaching and focused regression checks. Depends on TASK-001 and the stable API from TASK-003. | Yes | 2026-10-21 |
| TASK-005 | The coordinator writes `docs/architecture/reusable-systems.md` and `docs/tiers/t1-development-map.md`: verified authority/symbol, invariant, extension seam, actual proof/limits and T1 use. The attempted documentation worker0ad28462 has read-only rg/glob, no editor/Git, and produced no persisted output; withdraw that assignment without relaunching or inventing a SHA. Application code remains delegated. Depends on TASK-001. | Yes | 2026-10-21 |
| TASK-006 | Integrate preserved quest/documentation checkpoints in the isolated first-factory branch; serialize localization, source guards and generated output changes. Record actual source versus unexecuted claims. Depends on TASK-003, TASK-004 and TASK-005. | Yes | 2026-10-21 |

### Implementation Phase 3

- **GOAL-003**: Qualify and preserve the first T1 increment without interrupting T0 play.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-007 | Use the existing explicit SSH build-only workflow in an isolated execution root with fresh memory/cache ownership guards. Compile common/Forge/backend and run the new T1 graph/export plus affected recipe/quest/guidance/compatibility checks; generate through normal writers and retain red/green receipts. No Prism action. Depends on TASK-006. | Yes; bounded base523b, not later component source | 2026-10-21 |
| TASK-008 | Review source-bound route coverage, recurring input provenance, T0 byte/behavior preservation and physical F2/optional-branch evidence. Reuse unchanged native proof; add a representative native check only if a changed runtime boundary warrants it and gameplay resources allow. Depends on TASK-007. | Yes; honest unresolved establishment preserved | 2026-10-21 |
| TASK-009 | Commit/publish the coherent code/docs/proof checkpoint and a separate in-development draft PR against its actual T0 parent when appropriate. Report first-factory progress, reusable systems, unresolved gaps and next T1 increment; leave T0 installed untouched and do not merge either release branch. Depends on TASK-008. | No | |

Source integration is published as `c7278940d4b42999f1f128e560095fe3fb1329dc`.
The later sourceef9d670 and evidence checkpoint523b3ab preserve bounded
common/Forge/backend/affected checks and ignored-output writer/twin results.
Normal canonical T1/Guidance/Economy outputs, protected-fact comparison488 and
canonical rechecks502/417/2834 completed for that base. Parent independently
verifies13receipts/29native outcomes/58raw hashes/five outputs without replay.
The T1 PC lease returned when the actual ADR-0171 book blocker preempted execution.
No new Prism/package/server/native permission follows. Independent static QA found
one verifier defect: physical-component comparisons include the ENERGY input.
The correction74ccb36 is closed by narrow static recheck, with separate authoritative
FU assertions and all route/report energy retained. The early private observer,
javac/oracle/canonical/twin reds and interrupted run remain historical; their
successful repairs do not erase them. This base proof is not qualification of
the later two component routes, native FTB replacement, human play or full T1.

### Implementation Phase 4

- **GOAL-004**: Extend the first-factory increment into a source-checked T1 development map while preserving the feedback loop.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-010 | Read-only inventory of immutable sourceef9d670: `T1Recipes`, `ProcessGraph`, `FabricationLogic`, `ProcessMachineKind`, live crafting JSON and `T1Progression`/`T1Sustainability`. Update `docs/tiers/t1-development-map.md` with implemented/optional/proposed machine/route/component boundaries, automation gaps and authority references, not copied bills. Depends on TASK-006. | Yes | 2026-10-21 |
| TASK-011 | Verify mature external first-factory/component and persistent-vein patterns from immutable source/licence references; record conceptual fit and rejected approaches in `docs/design/t1-external-references.md`. No code/assets/dependency imports. This task is independent of the mutable writer/check execution. | Yes | 2026-10-21 |
| TASK-012 | Integrate TASK-010/TASK-011 findings into the same tier map: rank at most three next coherent implementation slices, name exact authority files, reusable engine/proof and real unresolved decisions. Preserve runtime/recipe/model/renewable-establishment distinctions. Depends on TASK-010 and TASK-011. | Yes | 2026-10-21 |

ADR-0172 selects the first follow-up. Source-only checkpoint2f4eee1 adds the two
ordinary Assembler component routes on qualified base523b3ab, with exact material
equivalence and shared timing/FU contracts. It is published to its isolated branch
but has no new Java/writer/runtime pass recorded here. Its integrated426
qualification is now assigned only to2a93 under the new explicit lease.
Parent verifies the retained first-factory base's
13closed receipts/29native outcomes/58raw hashes/five normal output identities
without replaying those JVM stages. Preserve the real reds/interruption.

### Implementation Phase 5

- **GOAL-005**: Compose the three authored seams, preserve the delivered T0 game and obtain actual fixed-source qualification without changing execution inputs underneath the owner.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-013 | Normally integrate pressure541cf6c9, Blueprint4550b975 and corrected baseline Wire Mill3a79a1f3 on native b38; preserve all ancestors, source ownership and both deltas in shared `VerifyAtlas`/`VerifyRecipes`. Record source42654eb and exact861-key locale union, with only the authorized38-word T1 Assembler lesson changed. Depends on TASK-006 and TASK-012. | Yes; source/static only, parent-published426 | 2026-10-21 |
| TASK-014 | Normally merge final T0 delivery root104de7bb after426 atab4d7bc and synchronize map/changelog/README/handoff/plan under the parent's explicit bounded delegation. Preserve dd0/JAR29bc/ZIP79ce, false acceptance flags and active sole2a93 ownership; keep ADR-0175 proposed and application bytes unchanged. Depends on TASK-013. | Yes; documentation only, local review pending | 2026-10-21 |
| TASK-015 | Only2a93 executes fixed426 under `integrated-family-qualification-20261021`: common/Forge and the deduplicated affected Recipe/Atlas/Persistence/T1/Blueprint/Planner/Band/Machine/Inspection/UI/Sustainability contracts; NORMAL Guidance/Economy/T1 writers and CheckOnly. Retain source-bound raw receipts and reds. Admit the author-owned representative Blueprint Forge probe only when ready and parent-serialized after active stages/locks close; use real capture/stamp/load/failure evidence, not old item existence. Reuse successful unchanged common compilation for a probe-only Forge delta. Depends on TASK-013 and the explicitly granted exclusive window. | No; active ownership, results pending | |

No completed source/documentation task marks TASK-015 passed. The integration
worker does not run Java, touch the PC or publish competing changes. The parent
reviews local documentation and serializes publication; actual execution failures
return to their owning author as coherent source corrections, never manual
canonical JSON or speculative integrator code.

## 3. Alternatives

- **ALT-001**: Waiting for every historical T0 hold is rejected for development only because the owner directly opens concurrent T1 work. Acceptance/release gates are not fabricated.
- **ALT-002**: Rebuilding machine/transfer/power/quest engines is rejected; the first factory already has working shared mechanisms and needs explicit integration and truthful guidance.
- **ALT-003**: Hand-maintained copied recipe bills are rejected; stable identities select data and live authorities supply values.
- **ALT-004**: Making optional Factory World/finite-vein branches a hidden F2 prerequisite is rejected; the intended exit is physical manufacturing.
- **ALT-005**: Deploying a changing T1 build over the owner's test instance is rejected; source/build iteration remains separate until explicitly requested.

## 4. Dependencies

- **DEP-001**: Preserve original a64/F intake evidence and the current dd0/JAR29bc/ZIP79ce existing-game delivery. Source426 is a separate development composition, not its installed update.
- **DEP-002**: Existing `ProcessGraph`, `T1Recipes`, Bootstrap/Renewable catalogues, rated Matrix items, `FluxTier`, `AuthorityAdapters` and sustainability/reserve mechanisms supply source data.
- **DEP-003**: Native FTB is the current player framework under ADR-0171. Stable catalogue/teaching and legacy `QuestProgress` paid history remain inputs to native progress/individual receipts and the legacy minting fence, not a second reward authority.
- **DEP-004**: Existing SSH/personal Git/JDK/Gradle setup is reused; heavy execution requires fresh headroom and must not interrupt the player's game.

## 5. Files

- **FILE-001**: New `common/src/main/java/io/github/ezequiel24123z/grindless/progression/T1Progression.java` and tightly related source-derived views/checks.
- **FILE-002**: `quest/QuestTeaching.java`, actual T1 prerequisite seam in `QuestCatalogue.java`, shared client teaching/localization and focused quest/backend tests.
- **FILE-003**: New paired T1 exporter entrypoints under `tools/t1/`, the source-bound T1 line graph and `docs/tiers/t1.md`.
- **FILE-004**: `docs/architecture/reusable-systems.md`, `docs/tiers/t1-development-map.md` and `docs/tiers/t1-quests.md`.
- **FILE-005**: AGENTS/README/changelog/build-out/ADRs/route-map and active handoffs, serialized by the coordinator.
- **FILE-006**: `docs/design/t1-external-references.md` stores source/licence-pinned conceptual precedent, not accepted recipe quantities or imported implementation.
- **FILE-007**: Pressure541 spans `recipe/ProcessRecipe`, `ProcessLookup`, `AtlasLogic`, `sustainability/AuthorityAdapters`, inspection snapshot/codec/projector, client tooltip, Forge JEI and affected contracts; reuse the existing evaluator.
- **FILE-008**: Blueprint4550 spans `item/BlueprintLogic`, `BlueprintItem`, `BlueprintToolItem`, `VerifyBlueprint` and14 feedback keys; the pending representative Forge probe remains author-owned/unready.
- **FILE-009**: Wire Mill3a spans `ProcessGraph`, `T1Progression`, one existing locale value and `VerifyRecipes`/`VerifyAtlas`/`VerifyPersistence`/`VerifyT1Progression`/`VerifyT1Teaching`.

## 6. Testing

- **TEST-001**: Exact line IDs, dependencies/acyclicity, live recipe/craft/fuel resolution, unavailable/missing/duplicate route negatives and real F2 exit identity/rating.
- **TEST-002**: Required recurring-input provenance, no mandatory finite mine/vanilla farm/T2 machine dependency, catalyst/output role fidelity and honest unresolved metadata.
- **TEST-003**: Existing T0 task/recipe/claim/starter invariance; retained T1 IDs/rewards, explicit optional Factory World branch and physical F1/F2 gating.
- **TEST-004**: Shared native/Heracles teaching, complete live recipe selection and quantities, source/localization validity and no second authority.
- **TEST-005**: Normal independent writer/CheckOnly determinism, source/hash provenance, proof reuse and no private/generated scratch artifacts committed.
- **TEST-006**: Supported common/Forge compilation, affected focused suites, documentation links and existing platform/backend boundary; no new native/client pass inferred.
- **TEST-007**: Pressure absence versus zero, finite optimum/positive half-width, shared conditions/projections, schema2 and strict schema1 compatibility, malformed/partial/unknown-version rejection.
- **TEST-008**: Strict Blueprint bounds/identities/facings/duplicates, loaded/permission refusal, full material shortfall, exact main/offhand settlement and explicit false/throwing placement boundaries; no global rollback assertion.
- **TEST-009**: Baseline F1 fourth operand, ordinary F1 output and unchanged bill/timing/FU; optional `wire_forming` selects four live routes after assembly without entering first-F2 dependencies.
- **TEST-010**: Locale861 unique keys:844 unchanged original values, one authorized38-word T1 lesson and unchanged two pressure/14 Blueprint additions; preserve old quest IDs/rewards/paid state.

## 7. Risks & Assumptions

- **RISK-001**: Legacy registered recipes include later-tier prototypes; F1 power or an existing registry entry alone does not classify a route as accepted T1.
- **RISK-002**: The current sustainability ledger is T0-scoped; do not silently declare all T1 closure from a repeatable recipe or sufficient construction witness.
- **RISK-003**: The owner may be actively playing; memory/CPU contention or instance mutation must not disturb the test. Defer heavy execution or use hosted checks when fresh headroom is insufficient.
- **RISK-004**: The T0 parent remains under review; preserve isolated ancestry and source identity rather than silently basing on old main or overwriting the parent.
- **RISK-005**: Blueprint placement callbacks can mutate before throwing; source4550 does not establish global world rollback, crash atomicity or universal zero loss. The old native item-existence smoke cannot close this boundary.
- **ASSUMPTION-001**: The latest instruction authorizes T1 development now, not a completed T0 campaign, accepted graphical gate or public release.
- **ASSUMPTION-002**: Existing recipe quantities and the installed T0 candidate remain stable unless a concrete tightly coupled defect requires a documented decision.
- **ASSUMPTION-003**: The owner-authorized longitudinal start is an intent/permission record until actual observations arrive; no client, save-copy or completed-campaign result is invented.

## 8. Related Specifications / Further Reading

- [Working agreement](../AGENTS.md)
- [Architecture decisions](../docs/DECISIONS.md)
- [T0-to-T1 bridge](../docs/T0-T1-ACTION-PLAN.md)
- [Recipe/process specification](../docs/PROCESSES.md)
- [Route map](../docs/AGENT-MAP.md)
- [Validation strategy](../docs/testing/test-strategy.md)
- [SSH/Prism workflow](../docs/operations/ssh-prism-workflow.md)
- [Installed T0 identity](../docs/testing/t0-experience-prism-deployment.md)
- [Current native FTB existing-game delivery](../docs/testing/t0-ftb-native-qualification.md)
- [Integrated development-source qualification](../docs/testing/latest-validation.md#integrated-development-source)
- [Proposed pressure engineering record](../docs/DECISIONS.md#adr-0175--carry-optional-absolute-process-pressure-through-existing-contracts)
