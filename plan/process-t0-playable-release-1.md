---
goal: Turn the T0 engineering candidate into a Superflat-first playable modpack release candidate
version: 1
date_created: 2026-10-18
last_updated: 2026-10-19
owner: Sol 6.1 successor coordinator
status: In progress
tags: [product, release, superflat, compatibility, art, audio]
---

# Introduction

![Status: In progress](https://img.shields.io/badge/status-In%20progress-yellow)

[ADR-0144](../docs/DECISIONS.md#adr-0144--ship-superflat-first-playable-tier-releases-with-longitudinal-save-continuity)
accepts the owner's new product policy. `1389ebbffbadbed33848cbfe0e8ced9a36b68a11`
is the engineering baseline, not the playable release or the longitudinal save baseline.
The [JSON task graph](../.agent/work-plan.json), revision8/PA-0007, is the execution
authority; this document explains its work packages and evidence. Revision9/PA-0008
adds the owner's [sound calibration](../docs/DECISIONS.md#adr-0145--calibrate-a-small-shared-sound-palette-with-development-only-generation)
and [item materiality](../docs/DECISIONS.md#adr-0146--give-items-material-volume-and-reuse-block-models-for-machine-items)
constraints without changing the27-task graph. The source directive's
SHA-256 is `7a5a04b1f2709536f1187ecc70958829fb7383bf95135820735f352b91a1e317`.

Revision10/PA-0009 and [ADR-0147](../docs/DECISIONS.md#adr-0147--transfer-clean-session-coordination-to-sol-61-without-replaying-evidence)
transfer the next clean-session leadership to Sol6.1. The retiring session preserves
the focused CI repairs and stops; no successor has been launched. Existing task,
ownership and decision references to Astra below describe that role historically;
the received Sol coordinator assumes the same responsibilities. The compact
[migration handoff](../.agent/sol-migration-handoff.md) is the new entry.

Revision11/PA-0010 records actual receipt of17ae8b0 in the new isolated Sol session
and activation of its sole-publisher role. The paragraph above describes the outgoing
checkpoint, not current inactivity. Task identities/dependencies and historical proof
scope are unchanged. Checkpoint verification jobs pass; full hosted CI is still running.

## 1. Requirements & Constraints

| ID | Requirement | Owner directive sections |
| --- | --- | --- |
| REQ-001 | Ship a complete, pinned, configured playable modpack per tier; do not wait for T15 | 1, 2, 9, 10 |
| REQ-002 | Preserve the longitudinal reference save across releases; explicitly test necessary migration on a copy | 2, 3, 12 |
| REQ-003 | Treat1389 as engineering-only and finish T0 product readiness before T1 | 4, 15, 16 |
| REQ-004 | Perform a real industrial/futuristic sprite, texture and visual pass, preserving good assets | 5 |
| REQ-005 | Perform a real natural industrial sound pass with controlled repetition/volume and complete rights provenance | 6 |
| REQ-006 | Review BetterQB against current Heracles/API evidence; retain one claim authority and unique rewards | 7 |
| REQ-007 | Version an official Superflat preset and prove deliberate mandatory access; preserve normal Overworld play | 8 |
| REQ-008 | Curate QoL/performance/information/accessibility without unintended progression or automation bypasses | 9 |
| REQ-009 | Build/install the actual distributable with pinned public configuration and required content | 2, 10, 11 |
| REQ-010 | Deliver technically ready/polished RC before human acceptance; allow next-tier work only after publication and real longitudinal play begins | 11, 12, 16 |
| REQ-011 | Inspect the new disk, ownership and absolute paths before a reversible, verified migration | 13 |
| REQ-012 | Orchestrate ordinary work autonomously; retain negative evidence and stop at genuinely unsafe or unresolved decisions | 14, 15, immediate result |
| SEC-001 | Never bundle private worlds/configuration, accounts, credentials, caches or irrelevant development output | 10 |
| CON-001 | No T1, ports, engine rewrite, dependency switch or foreign-asset import just to increase scope | 4, 5, 7, 15 |
| CON-002 | Versions remain in gradle.properties; catalogues, claim ledger and loader separation remain authoritative | Existing agreement |
| CON-003 | Preserve failures and revision boundaries; technical, visual, packaged, human-accepted and release-ready are distinct | 3, 11, 14, 16 |
| PAT-001 | Reuse ADR-0140 contracts; invalidate only assumptions changed by profile, data, mechanism or integration | 11, 14 |

### Baseline gap analysis

| Gap | Already available and reusable | Missing deliverable / honest current state |
| --- | --- | --- |
| GAP-001 Product lifecycle | Reviewed engineering candidate and preserved preparation ZIPs | New release/readiness and longitudinal gates are accepted policy; no playable release exists |
| GAP-002 Superflat | Conditional T0 model, native producers and physical recipe graph | Official preset and initial-resource accessibility are unproved; ordinary-world finite stock cannot be assumed available |
| GAP-003 Pack | Existing launcher manifest/profiles, remapped jar builder, provenance and15 pack checks | Reasoned complete composition, public configs, bypass review, preset delivery and new locked import QA |
| GAP-004 Questbook | Heracles adapter, shared claims, atomic definitions,307 contracts and native binding evidence | Product/UX review and evidence-backed BetterQB decision; actual chosen interface on final pack |
| GAP-005 Art | Inventory, alias repair, deterministic generator and resource checks | Real sprite/texture craft, family/state readability and observed final visual result |
| GAP-006 Sound | Quiet ordinary faults and current sound dispatch | Suitable recorded sounds, licences/notices/derivation chain, mixing/looping and actual listening |
| GAP-007 Compatibility | Native NBT/layout/transaction and claim contracts | Release-to-release contract and checks; no longitudinal owner save has begun |
| GAP-008 Release QA |1389 CheckOnly4881,42 suites, three complete CI runs, existing package hashes | Updated pack/preset/import/startup/client/audio/visual proof and no known ordinary-flat-play blocker |
| GAP-009 Storage | Existing code/evidence preserved; new NTFS work volume observed with190GiB capacity | Ownership/path inventory, selected reversible layout and verified tool/worker use; nothing moved yet |
| GAP-010 Publication | Existing draft PR54/PR53, single coordinator publication and guarded fast-forward | Versioned installable GitHub RC/prerelease with verified contents and release notes; release list currently empty |

Counts above describe preserved proofs, not a quality target or passes for new inputs.

## 2. Implementation Steps

### Implementation Phase 1

- GOAL-001: Formalize the policy and establish bounded, source-based gap evidence.

| Task | Description | Completed | Date |
| --- | --- | --- | --- |
| TASK-001 | Record ADR-0144, PA-0007 and the stage distinction in AGENTS.md, README.md, BUILD-OUT, go-gate, test strategy and active handoff | Yes;560 links,144 ADRs and27-task/47-edge graph validated | 2026-10-18 |
| TASK-002 | Inspect owned storage sizes/path coupling and recommend a reversible layout; no movement during audit | Yes; approximate sizes with enumeration limits; new-write redirection selected, not verified | 2026-10-18 |
| TASK-003 | Trace mandatory T0 entry/environment sources through T0Sustainability, AuthorityAdapters, recipes and SourceSite against Superflat and Overworld | Yes; initial wood/stone/water and acquisition/smelting proof gaps identified; preset not selected | 2026-10-18 |
| TASK-004 | Review current pack profiles/manifest and exact compatible QoL/quest candidates, including BetterQB versus Heracles | Yes; shortlist and backend recommendation returned with explicit unverified locks/licence risks | 2026-10-18 |
| TASK-005 | Prepare a contact sheet from current actual sprites/textures and identify generator-preserving art scope | Yes;36 current textures shown, owner clarified item-first direction; native item-model audit pending | 2026-10-18 |
| TASK-006 | Find a bounded industrial recording shortlist with authoritative redistribution/licence evidence and family/use mapping | Yes; alternatives remain unimported; ElevenLabs now selected for initial calibration under ADR-0145 | 2026-10-18 |

TASK-002 through TASK-006 are independent discovery lanes on engineering1389 plus the
docs-only33b4af2 baseline. They may read while TASK-001 edits global documentation.
They do not receive permission to move data, alter gameplay or import assets.

### Implementation Phase 2

- GOAL-002: Make bounded decisions from evidence and implement only the identified gaps.

| Task | Description | Completed | Date |
| --- | --- | --- | --- |
| TASK-007 | Select safe storage using TASK-002, preserve originals, migrate only chosen roots and verify path/content/worker behavior before retiring any copy | Yes; new process-local Gradle writes verified with compile/classpath task, old roots deliberately untouched | 2026-10-18 |
| TASK-008 | Select the versioned flat preset/bootstrap contract from TASK-003; implement only missing accessibility/profile bindings and paired validation, preserving Overworld behavior | No | |
| TASK-009 | Pin the curated pack and public configurations from TASK-004 in existing metadata/build surfaces; document exclusions and bypass controls | No | |
| TASK-010 | Retain/polish or replace the quest interface only after the actual version/API/save comparison; preserve QuestClaimService and current-evidence/unique-reward semantics | No | |
| TASK-011 | Give items readable physical volume/materiality, prioritize the Core/Relay family and preserve placed machines; reuse verified native block-model hierarchy for machine items (ADR-0146) | Partial; nine sprites/261 parent-only JSONs installed,814 checks and source-pixel review pass; native client/product art closure pending | |
| TASK-012 | Complete minimal sound manifest/pipeline, generate at most3 Quern calibration candidates and wait for owner selection; then record rights/provenance and integrate a small shared palette (ADR-0145) | Partial;3 real previews delivered and owner-approved; style guide, runtime palette/mix and release-use qualification remain | |
| TASK-013 | Define release/save identity and compatibility manifest/checks; no automatic adoption of a personal or preparation world | No | |

TASK-007 depends on TASK-002. TASK-008 depends on TASK-003. TASK-009 and TASK-010 depend
on TASK-004 and share a serialized version/build/manifest owner. TASK-011 depends on
TASK-005; final asset coverage also consumes TASK-008's frozen content. TASK-012 depends
on TASK-006, reviewed manifest/rights, a bounded generation grant and the owner's initial
artistic choice before expansion. This is not a new per-step approval waterfall.
TASK-013 consumes the selected pack and persistence
surface inventory. No broad rebuild is required merely to accept these decisions.

### Implementation Phase 3

- GOAL-003: Integrate and publish a technically qualified, polished playable RC.

| Task | Description | Completed | Date |
| --- | --- | --- | --- |
| TASK-014 | Integrate reviewed storage/tooling first, then preset/bootstrap and pack pins, then quest/guidance, art and audio; refresh only affected canonical projections | No | |
| TASK-015 | Freeze a candidate; execute applicable contracts/data checks and required CI, then clean pack import/startup/preset and representative client/visual/audio/startup-progression QA | No | |
| TASK-016 | Build locked mod/modpack artifacts, inspect actual archive whitelist and hashes, verify reproduction within documented assumptions and write upgrade/release notes | No | |
| TASK-017 | Verify live PR/protection/release state and publish the versioned installable RC safely; do not overwrite old artifacts or tag an unverified source | No | |
| TASK-018 | After the owner actually begins the published RC, record the longitudinal reference identity and open dependency-safe T1 alongside classified T0 feedback | No; T1 remains held | |

TASK-014 waits for the relevant Phase2 sources to freeze; it is not permission for
workers to edit global files concurrently. TASK-015 requires the composed source and
pack. TASK-016 requires successful scoped qualification. TASK-017 requires artifact
inspection, live publication safety and no known ordinary-play blocker. TASK-018 requires
both publication and an actual owner-start signal, not completion of the whole playthrough.

## 3. Alternatives

- ALT-001: Relabel1389/preparation ZIP as the playable release. Rejected: product gaps remain.
- ALT-002: Replace the quest backend immediately because BetterQB was named. Rejected pending real compatibility/UX evidence.
- ALT-003: Use normal terrain or a gifted kit to hide flat-world gaps. Rejected: violates the official-preset requirement.
- ALT-004: Move app-managed worktrees wholesale or delete caches first. Rejected without ownership/path/rollback evidence.
- ALT-005: Regenerate unchanged assets or rerun every recipe to claim polish. Rejected: structural determinism is not visual/audio quality.
- ALT-006: Require the owner to finish T0 before delivering an RC or preparing T1 after play starts. Superseded by ADR-0144.

## 4. Dependencies

- DEP-001: Existing source-bound S1/C3/S2, native storage, UI/claim and projection contracts; retain their original revisions.
- DEP-002: Exact supported Forge/Minecraft, backend and publisher metadata; no floating coordinates.
- DEP-003: Astra preset/composition/art-direction choices based on the dispatched research, not worker-invented policy.
- DEP-004: Verified storage migration before heavy execution when appropriate; separate JVM/resource ownership from safe parallel reads/edits.
- DEP-005: Real client/audio observation and later authentic longitudinal-save access where those properties require them.
- DEP-006: Explicit JSON task dependencies and completion requirements, not the order of tables in this document.

## 5. Files

- FILE-001: AGENTS.md, README.md, CHANGELOG.md, docs/DECISIONS.md and docs/BUILD-OUT.md carry durable policy.
- FILE-002: .agent/work-plan.json, .agent/pending-checks.json and .agent/handoff.md carry operational stage, source pin, ownership and outstanding evidence.
- FILE-003: docs/reviews/go-gate.md and docs/testing/test-strategy.md separate RC qualification from human acceptance; t0-survival-acceptance.md records actual later observations.
- FILE-004: pack/profiles.json, pack/curseforge/manifest.json, pack/dependency-metadata.json, pack/README.md, gradle.properties and existing paired pack tools own composition/distribution.
- FILE-005: common source/recipe/world/sustainability authorities and the selected pack preset resources own world accessibility; exact new files require the source-audit decision.
- FILE-006: QuestClaimService, the existing Forge quest adapter and native guide/client seams own quest experience without a new authority.
- FILE-007: tools/assetgen, existing T0 texture/model resources and docs/assets/t0-asset-manifest.json own maintainable art.
- FILE-008: docs/assets/t0-sound-design.json, tools/sound-design, tools/audio, sound resources/dispatch and THIRD_PARTY_NOTICES.md own bounded development calibration and eventual approved derived audio/provenance. Secrets and actual invocation grants stay outside Git.
- FILE-009: Existing tool environment/cache settings own storage redirection where sufficient; app session metadata and unrelated worktrees are not worker-editable.

## 6. Testing

- TEST-001: Validate links/ADR index, JSON, unique task IDs, declared dependency graph and all16 directive sections; no game run for policy-only changes.
- TEST-002: Reuse unchanged1389 contracts and historical native proofs. Rebind Superflat/preset assumptions and validate all relevant data instead of rerunning every recipe.
- TEST-003: Prove each new world-profile/bootstrap class with targeted static/model and representative runtime cases; retain an Overworld compatibility case.
- TEST-004: Check pins, config whitelist, dependency compatibility, progression-bypass controls, clean import/startup and expected preset/content of the actual pack.
- TEST-005: Preserve claim/idempotency/current-evidence and persistence contracts for any quest change; actual interface checks do not replace server authority proof.
- TEST-006: Validate asset references/generation and observe the resulting art at native/scaled size and representative in-game contexts; preserve good unchanged assets.
- TEST-007: Verify audio rights/source/derived hashes and encoding/loop/gain behavior, then listen to representative single-machine and aggregate-factory situations.
- TEST-008: Keep exact-source CI, archive inspection, reproducibility assumptions, natural exits and negative results. Logs, suite counts and artifacts are different units.
- TEST-009: For later releases, compare changed persistence/config/IDs and exercise necessary migrations on a preserved reference-save copy; never alter the original.
- TEST-010: Verify selected storage content and actual build/tool/worker use; a directory copy alone is not successful migration.

## 7. Risks & Assumptions

- RISK-001: Classic flat may lack mandatory finite entry inputs; existing model success does not settle this.
- RISK-002: A QoL/worldgen mod can bypass mandatory logistics or alter tags, resources, recipes or environmental assumptions.
- RISK-003: Backend replacement can break claims or save/config ownership despite a visually attractive interface.
- RISK-004: Licence ambiguity, missing attribution or unauthorised derivatives block asset import/publication.
- RISK-005: Generator-only validation can conceal weak art; signal checks can conceal poor listening experience.
- RISK-006: Absolute paths and app-managed worktrees make broad filesystem migration unsafe; memory limits survive disk expansion.
- RISK-007: Global historical UNKNOWN findings remain visible; a conditional T0 pass is not acceptance of future tiers.
- ASSUMPTION-001: No longitudinal owner world has started; no preparation save is adopted implicitly.
- ASSUMPTION-002: The existing engine/catalogues are reused unless a new, demonstrated requirement invalidates them.
- ASSUMPTION-003: Ordinary reversible steps are autonomous; genuinely destructive/irreversible or unresolved subjective/licence decisions stop for direction.

## 8. Related Specifications / Further Reading

- [Working agreement](../AGENTS.md)
- [ADR-0144](../docs/DECISIONS.md#adr-0144--ship-superflat-first-playable-tier-releases-with-longitudinal-save-continuity)
- [Reloadable execution graph](../.agent/work-plan.json)
- [Active handoff](../.agent/handoff.md)
- [Risk-based proof reuse](../docs/testing/test-strategy.md)
- [Revision-bound validation](../docs/testing/latest-validation.md)
- [Player and longitudinal observations](../docs/testing/t0-survival-acceptance.md)
- [Third-party notices](../THIRD_PARTY_NOTICES.md)
