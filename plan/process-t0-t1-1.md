---
goal: Close T0 with evidence while preparing T1 without changing the active frontier
version: 1
date_created: 2026-10-19
last_updated: 2026-10-19
owner: Grindless maintainers
status: Planned
tags: [process, architecture, testing, handoff]
---

# Introduction

![Status: Planned](https://img.shields.io/badge/status-Planned-blue)

This is the executable companion to the [T0 to T1 action plan](../docs/T0-T1-ACTION-PLAN.md)
and its [audit evidence](../docs/T0-AUDIT.md). Baseline:
`0d798607c66ff7e4b581d2786cfa32dc4a02ded7`. The audit is complete as documentation; the work
below is **not implemented** and T0 is not accepted. Source path prefix `C` denotes
`common\src\main\java\io\github\ezequiel24123z\grindless`.

## 1. Requirements & Constraints

- **REQ-001**: Close the ADR-0116/0122 route with one assisted extraction, physical calibration and five F1 Matrices from a two-iron machinery budget.
- **REQ-002**: Preserve exactly-once inventory/extraction accounting, persistent work and recoverable blocked/starved states.
- **REQ-003**: Required instructions and quantities must be readable in the supported client; quests observe rather than gate.
- **SEC-001**: Keep third-party/AI asset provenance and distribution terms; never commit credentials or add runtime service credentials.
- **CON-001**: No active T1 content or T0 balance change before the acceptance commit; preparation may add tests/tooling/docs without changing the candidate.
- **CON-002**: Keep registered IDs and legacy saves; common code must remain loader-neutral.
- **GUD-001**: Use fast semantic checks for proven data variants, targeted smokes for changed Minecraft seams, and isolated full smokes for integration.
- **PAT-001**: Reuse ProcessGraph, ProcessLookup, MachineBlockEntity, existing screens, BlockCatalogue and the paired script convention.

## 2. Implementation Steps

### Implementation Phase 1

- **GOAL-001**: Pin a reproducible candidate and produce failing regressions before changing behavior.

| Task | Description | Completed | Date |
| --- | --- | --- | --- |
| TASK-001 | Record candidate SHA, pack manifest/version, seed and inventory budget in the eventual acceptance evidence; begin M01-M04 from the action plan. Verify `:common:compileJava build`, `run-checks` and links. Do not edit runtime code. No prerequisite. | No | Not executed |
| TASK-002 | Add a failing blocked-output recovery scenario for `C\machine\CrudeExtractorBlockEntity.java` (`updateSubscriptions`, `work`, `pushOutput`) to `tools\smoke\scenarios.txt` with commands/expect files. Fill internal/destination storage, drain only the destination, assert autonomous recovery and conserved output; include reload. Depends on TASK-001. | No | Not executed |
| TASK-003 | Add real inventory regression coverage for `C\menu\QuestBookMenu.java` (`held`): main/offhand/split stacks, three/four/five Matrices and once-only claims. Demonstrate the offhand double count before fixing. Pure fixtures alone must not stand in for Inventory traversal. Depends on TASK-001. | No | Not executed |

Exit: the candidate is pinned and regressions distinguish the reported defects from missing power,
wrong recipes or invalid setup. TASK-002 and TASK-003 are independent after TASK-001.

### Implementation Phase 2

- **GOAL-002**: Repair T0 transitions and prepare shared contracts without invalidating human balance evidence.

| Task | Description | Completed | Date |
| --- | --- | --- | --- |
| TASK-004 | Fix only the reproduced A01/A02 behaviors in the two classes above. Drain output independently of a new successful cycle, arrange bounded wake/retry, and count every inventory slot once. Keep cycle/yield, registry/NBT identity and claim rewards unchanged. Run both regressions plus Bootstrap; publish candidate B and rerun M03-M10. Depends on TASK-002 and TASK-003. | No | Not executed |
| TASK-005 | Extend `tools\checks\VerifyRecipes.java`, `VerifyAtlas.java` and `VerifyControlMatrix.java` with semantic graph contracts: unique IDs, available tag providers/output forms, per-menu input/catalyst capacity, reachable byproduct sinks, forbidden spatial outputs and rating presentation. Add separate runtime fixtures for A03 before touching executor logic. Preserve 112/41/145 only for the existing fixture. Depends on TASK-001; independent of TASK-004. | No | Not executed |
| TASK-006 | Add `tools\convert-audio.ps1` and `tools\run-smokes-batch.sh` as semantic twins; make both `run-checks` runners consume a reproducible explicit dependency classpath rather than every cached jar. Preserve exit codes and isolated/full-matrix semantics. Prove missing dependencies fail explicitly, and success still runs all 30 suites. Depends on TASK-001; do not combine with gameplay fixes. | No | Not executed |
| TASK-007 | Extend existing `tools\assetgen` output/review tooling with a catalogue-driven contact sheet and audio event checklist; retain current functional assets. Define source/license/parameters/mono-Ogg checks for an optional future ElevenLabs offline step without calling the service or importing assets. Depends on TASK-001; independent of all runtime fixes. | No | Not executed |
| TASK-008 | Reconcile A06 in README, BUILD-OUT, AGENT-MAP, PROCESSES and pack README; retain historical ADR text. State current F2 output versus missing consumers, five seed controllers and exact Assembler parts. Ensure full required detail in Atlas/guide/JEI is testable; UI edits require the corresponding client check, not a new viewer. Depends on TASK-001; coordinate any shared language-file edits. | No | Not executed |

Exit: mandatory T0 regressions pass and a new candidate is identified. GREEN preparation can finish
independently; unfinished non-blocking tooling must not hold T0 hostage. TASK-005 is evidence
preparation, not permission to change scheduler or recipe balance.

### Implementation Phase 3

- **GOAL-003**: Accept T0 explicitly, then open only the proven T1 continuation.

| Task | Description | Completed | Date |
| --- | --- | --- | --- |
| TASK-009 | Record M01-M14 outcomes on candidate B, timing and product decisions; resolve mandatory readability failures. Run compile/build, all checks, links, targeted changed-seam smokes and isolated integration smokes. Only then mark BP accepted/T1 active in README and BUILD-OUT and record the evidence in CHANGELOG. Depends on TASK-004, the active-guidance portion of TASK-008 and human survival sign-off. | No | Not executed |
| TASK-010 | After A03 regressions exist, repair recipe identity/progress, fluid wake-up, output recovery and reload cache generation in `C\machine\ProcessMachineBlockEntity.java` and `C\recipe\ProcessLookup.java` as separate checkpoints. Preserve tested semantics. Then specify minimum/exact Matrix matching and attach one real F2 consumer in ProcessGraph, with runtime/viewer checks; do not recreate the existing F2 output. Depends on TASK-005 and TASK-009. | No | Not executed |

Exit: the active tier changes in an evidence-bearing acceptance commit, not because a task table
was checked. M15-M21 are the next T1 campaign, not retroactive T0 prerequisites.

## 3. Alternatives

- **ALT-001**: Wait for all human testing before any engineering. Rejected: independent regressions/tooling/docs can progress safely.
- **ALT-002**: Open T1 content immediately because CI is green. Rejected: known recovery defects and absent survival evidence violate the active gate.
- **ALT-003**: Rewrite a generic machine/multiblock framework first. Rejected: transition bugs need focused tests, and no T1 machine requires the T3 kernel.
- **ALT-004**: Require final art and every historical smoke after each recipe variant. Rejected: neither is proportional evidence for an unchanged mechanism.

## 4. Dependencies

- **DEP-001**: Java 17, repository Gradle wrapper and Forge toolchain; use the documented Windows execution-policy launcher.
- **DEP-002**: User-run survival/client campaign and explicit acceptance; cannot be replaced by server NBT injection.
- **DEP-003**: Existing generation/capability/menu/asset seams and exact pinned pack manifest.
- **DEP-004**: Task prerequisites in the tables are authoritative; all other tasks in a phase are independent unless they edit the same file.

## 5. Files

- **FILE-001**: `C\machine\CrudeExtractorBlockEntity.java` and `C\menu\QuestBookMenu.java`: T0 fixes.
- **FILE-002**: `C\machine\ProcessMachineBlockEntity.java`, `C\recipe\ProcessLookup.java`, `ProcessGraph.java`, `IngredientSpec.java`, `OutputSpec.java`: guarded T1 executor/rating work.
- **FILE-003**: `tools\checks\Verify*.java`, `tools\smoke\scenarios.txt` and scenario commands/expect files: semantic and runtime regressions.
- **FILE-004**: `tools\run-checks.ps1`, `run-checks.sh`, audio/batch twins and `tools\assetgen`: reproducible preparation.
- **FILE-005**: README, CHANGELOG, BUILD-OUT, AGENT-MAP, PROCESSES, pack README and client language/viewer files: truthful guidance and acceptance.

## 6. Testing

- **TEST-001**: A01 regression fails before the fix and passes with automatic recovery, no loss/duplication and correct saved vein accounting.
- **TEST-002**: A02 counts each slot once; three/four fail and five pass regardless of inventory arrangement; claims persist once.
- **TEST-003**: Runtime A03 fixtures cover late fluid arrival, full outputs, recipe swaps and tag reload; unrelated recipes never inherit completed work.
- **TEST-004**: Matrix matching and viewer evidence agree for default F1, explicit F2, invalid tags and the chosen higher-rating substitution rule.
- **TEST-005**: M01-M14 produce candidate-specific human evidence; M15-M21 remain separately labeled T1.
- **TEST-006**: All 30 behavior suites, compile/build and links pass; changed Minecraft seams get targeted smokes; isolated historical scenarios pass at integration.

## 7. Risks & Assumptions

- **RISK-001**: A green source-string/fixture check can miss the actual Minecraft inventory or scheduler path.
- **RISK-002**: Changing the human's candidate mid-run invalidates timing/recovery conclusions; publish new candidate IDs and rerun affected prerequisites.
- **RISK-003**: Mandatory product information may be truncated even though the graph contains it.
- **RISK-004**: Existing T2 recipes remain reachable without rated matrices; do not claim a closed frontier before consumer migration.
- **ASSUMPTION-001**: ADR-0116/0122's explicit five-Matrix endpoint governs T0; earlier broad lattice prose does not silently add another T0 content pass.
- **ASSUMPTION-002**: The pinned client pack is the first acceptance profile pending the owner's decision; no client/survival result is assumed.

## 8. Related Specifications / Further Reading

- [Action plan and manual campaign](../docs/T0-T1-ACTION-PLAN.md)
- [Audit findings and evidence](../docs/T0-AUDIT.md)
- [Mechanical completion gate](../docs/BUILD-OUT.md#completion-gate)
- [Architecture decisions](../docs/DECISIONS.md)
- [Task-indexed code map](../docs/AGENT-MAP.md)
