---
goal: Begin T1 with canonical first-factory lines, live teaching and a reusable-system handbook
version: 1
date_created: 2026-10-21
last_updated: 2026-10-21
owner: Sol coordinator
status: 'In progress'
tags: [feature, t1, progression, documentation, reuse, compatibility]
---

# Introduction

![Status: In progress](https://img.shields.io/badge/status-In%20progress-yellow)

The owner explicitly requests T1 development while testing the T0 dev build.
Start from `a64d44f9ee6da1fe00ed9215eb05b4aad4132f64`, the preserved T0 experience
branch, not older main. Keep installed runtime F298988d/JAR95bf47c4 unchanged.
This first development increment integrates the already implemented first-factory
mechanisms into an explicit T1 contract and usable guidance; it does not declare
the entire tier finished or introduce a replacement processing engine.

## 1. Requirements & Constraints

- **REQ-001**: Implement canonical T1 lines that reference actual recipe/craft/fuel authorities and are consumed by runtime quest guidance and an executable exporter.
- **REQ-002**: Cover F1 power/distribution, milling/reduction, renewable glass, forming/dies/coils/casings, Assembler reproduction and the physical F2 Relay Matrix exit.
- **REQ-003**: Mark finite extraction, optional logistics/storage, Kiln and Factory World branches explicitly; the F2 exit must not require optional Factory World access.
- **REQ-004**: Preserve all46 existing quest IDs/rewards/claims and every T0 task, recipe, starter allocation, physical F1 gate and save format.
- **REQ-005**: Differentiate T1 teaching from the old generic preview using live Atlas recipes and the shared line contract, without duplicating bills or claiming successful operation from possession.
- **REQ-006**: Document reusable authorities, invariants, safe extension seams, proof boundaries and concrete T1 uses with verified source references.
- **REQ-007**: Trace recurring required inputs to the existing mod-owned T0 base or declare an explicit unresolved dependency; repeatable finite extraction is not renewable.
- **SEC-001**: No credential/provider/account changes, private paths in Git, new generation charges or third-party implementation imports.
- **CON-001**: Do not modify, redeploy or restart the owner's installed Prism T0 instance or touch player worlds/configuration.
- **CON-002**: T1 development authorization is not T0 acceptance, a graphical pass, a T1 release or permission to start T2 production.
- **CON-003**: All versions stay in `gradle.properties`; common remains loader-neutral; existing processing, transaction, storage, grid and quest engines remain the authorities.
- **CON-004**: Use isolated dependent branches and one integrated build/cache owner. Player gameplay has resource priority; no user process is stopped.
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
| TASK-003 | Core ownera94c3085 implements `progression/T1Progression`: `Requirement`, immutable `Line(id, requirement, routeIds, requires)`, `lines()` and `byId()`. Resolve mandatory/optional/exit routes against existing authorities, including real Flux Cell crafting; add source-derived export and `VerifyT1*` contracts. No recipe quantities are redeclared. Depends on TASK-001. | No | |
| TASK-004 | Quest ownerac4962d5 maps actual T1 task IDs to the shared line contract, replaces generic previews for implemented first-factory tasks, preserves T0/claims/rewards and makes Factory World optional for `industrial_matrix`. Extend native/Heracles teaching and focused regression checks. Depends on TASK-001 and the stable API from TASK-003. | No | |
| TASK-005 | The coordinator writes `docs/architecture/reusable-systems.md` and `docs/tiers/t1-development-map.md`: verified authority/symbol, invariant, extension seam, actual proof/limits and T1 use. The attempted documentation worker0ad28462 has read-only rg/glob, no editor/Git, and produced no persisted output; withdraw that assignment without relaunching or inventing a SHA. Application code remains delegated. Depends on TASK-001. | Yes | 2026-10-21 |
| TASK-006 | Integrate preserved quest/documentation checkpoints in the isolated first-factory branch; serialize localization, source guards and generated output changes. Record actual source versus unexecuted claims. Depends on TASK-003, TASK-004 and TASK-005. | No | |

### Implementation Phase 3

- **GOAL-003**: Qualify and preserve the first T1 increment without interrupting T0 play.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-007 | Use the existing explicit SSH build-only workflow in an isolated execution root with fresh memory/cache ownership guards. Compile common/Forge/backend and run the new T1 graph/export plus affected recipe/quest/guidance/compatibility checks; generate through normal writers and retain red/green receipts. No Prism action. Depends on TASK-006. | No | |
| TASK-008 | Review source-bound route coverage, recurring input provenance, T0 byte/behavior preservation and physical F2/optional-branch evidence. Reuse unchanged native proof; add a representative native check only if a changed runtime boundary warrants it and gameplay resources allow. Depends on TASK-007. | No | |
| TASK-009 | Commit/publish the coherent code/docs/proof checkpoint and a separate in-development draft PR against its actual T0 parent when appropriate. Report first-factory progress, reusable systems, unresolved gaps and next T1 increment; leave T0 installed untouched and do not merge either release branch. Depends on TASK-008. | No | |

## 3. Alternatives

- **ALT-001**: Waiting for every historical T0 hold is rejected for development only because the owner directly opens concurrent T1 work. Acceptance/release gates are not fabricated.
- **ALT-002**: Rebuilding machine/transfer/power/quest engines is rejected; the first factory already has working shared mechanisms and needs explicit integration and truthful guidance.
- **ALT-003**: Hand-maintained copied recipe bills are rejected; stable identities select data and live authorities supply values.
- **ALT-004**: Making optional Factory World/finite-vein branches a hidden F2 prerequisite is rejected; the intended exit is physical manufacturing.
- **ALT-005**: Deploying a changing T1 build over the owner's test instance is rejected; source/build iteration remains separate until explicitly requested.

## 4. Dependencies

- **DEP-001**: Exact a64d44f T0 experience source and its preserved F runtime remain the compatibility baseline; PR56 is still a separate draft at task intake.
- **DEP-002**: Existing `ProcessGraph`, `T1Recipes`, Bootstrap/Renewable catalogues, rated Matrix items, `FluxTier`, `AuthorityAdapters` and sustainability/reserve mechanisms supply source data.
- **DEP-003**: Existing Quest Book/Heracles shared teaching and player/Overworld claim authority remain the runtime presentation/state mechanism.
- **DEP-004**: Existing SSH/personal Git/JDK/Gradle setup is reused; heavy execution requires fresh headroom and must not interrupt the player's game.

## 5. Files

- **FILE-001**: New `common/src/main/java/io/github/ezequiel24123z/grindless/progression/T1Progression.java` and tightly related source-derived views/checks.
- **FILE-002**: `quest/QuestTeaching.java`, actual T1 prerequisite seam in `QuestCatalogue.java`, shared client teaching/localization and focused quest/backend tests.
- **FILE-003**: New paired T1 exporter entrypoints under `tools/t1/`, the source-bound T1 line graph and `docs/tiers/t1.md`.
- **FILE-004**: `docs/architecture/reusable-systems.md`, `docs/tiers/t1-development-map.md` and `docs/tiers/t1-quests.md`.
- **FILE-005**: AGENTS/README/changelog/build-out/ADRs/route-map and active handoffs, serialized by the coordinator.

## 6. Testing

- **TEST-001**: Exact line IDs, dependencies/acyclicity, live recipe/craft/fuel resolution, unavailable/missing/duplicate route negatives and real F2 exit identity/rating.
- **TEST-002**: Required recurring-input provenance, no mandatory finite mine/vanilla farm/T2 machine dependency, catalyst/output role fidelity and honest unresolved metadata.
- **TEST-003**: Existing T0 task/recipe/claim/starter invariance; retained T1 IDs/rewards, explicit optional Factory World branch and physical F1/F2 gating.
- **TEST-004**: Shared native/Heracles teaching, complete live recipe selection and quantities, source/localization validity and no second authority.
- **TEST-005**: Normal independent writer/CheckOnly determinism, source/hash provenance, proof reuse and no private/generated scratch artifacts committed.
- **TEST-006**: Supported common/Forge compilation, affected focused suites, documentation links and existing platform/backend boundary; no new native/client pass inferred.

## 7. Risks & Assumptions

- **RISK-001**: Legacy registered recipes include later-tier prototypes; F1 power or an existing registry entry alone does not classify a route as accepted T1.
- **RISK-002**: The current sustainability ledger is T0-scoped; do not silently declare all T1 closure from a repeatable recipe or sufficient construction witness.
- **RISK-003**: The owner may be actively playing; memory/CPU contention or instance mutation must not disturb the test. Defer heavy execution or use hosted checks when fresh headroom is insufficient.
- **RISK-004**: The T0 parent remains under review; preserve isolated ancestry and source identity rather than silently basing on old main or overwriting the parent.
- **ASSUMPTION-001**: The latest instruction authorizes T1 development now, not a completed T0 campaign, accepted graphical gate or public release.
- **ASSUMPTION-002**: Existing recipe quantities and the installed T0 candidate remain stable unless a concrete tightly coupled defect requires a documented decision.

## 8. Related Specifications / Further Reading

- [Working agreement](../AGENTS.md)
- [Architecture decisions](../docs/DECISIONS.md)
- [T0-to-T1 bridge](../docs/T0-T1-ACTION-PLAN.md)
- [Recipe/process specification](../docs/PROCESSES.md)
- [Route map](../docs/AGENT-MAP.md)
- [Validation strategy](../docs/testing/test-strategy.md)
- [SSH/Prism workflow](../docs/operations/ssh-prism-workflow.md)
- [Installed T0 identity](../docs/testing/t0-experience-prism-deployment.md)
