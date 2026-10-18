# Development and merge gate

**Current state: T0 ENGINEERING CANDIDATE; PLAYABLE RC IN PROGRESS.**
[ADR-0144](../DECISIONS.md#adr-0144--ship-superflat-first-playable-tier-releases-with-longitudinal-save-continuity)
and PA-0007 are the current owner-directed delivery policy. `1389ebb` is not yet the
longitudinal-play release. Reviewed technical units may be committed and integrated,
but the current draft is not release-ready or merge-ready merely because its CI passes.

**Playable-release conditions remain open:** explicit Superflat progression/preset,
Overworld compatibility, curated locked/configured pack, coherent questbook, real art and
sound passes with redistribution provenance, applicable installation/startup/client QA,
forward-save strategy and no known ordinary-play blocker. Preserve required live PR
checks/reviews and branch protection; do not publish or merge a known incomplete product.

**Human acceptance is separate and begins with the RC.** The full no-command player route,
physical five-Matrix craft and longitudinal observations are not fictional pre-release
passes and are no longer prerequisites for the owner to receive that first playable RC.
After the RC is ready/published and the owner has begun its reference save, T1 may proceed
alongside feedback unless a concrete technical dependency blocks it. Neither event has
happened. Later updates must prove applicable compatibility/migrations before publication.
Do not infer `human_accepted` from `technically_verified`, `packaged` or `release_ready`.

The foundation decision below remains historical; its old ordering does not override
ADR-0144. Technical proof scopes, prior failures and genuine unobserved properties survive.

## Preserved foundation decision

**Result: CONDITIONAL GO**

Executable checkpoint: `e2dfc2c847dd220178572b83e7516a9949bb8b58`, 2026-10-17.
This was the foundation assignment's execution gate, preserved as historical evidence.
It permits review of the bounded foundation fixes, **not broad T0 implementation, Tier acceptance
or a merge**. A full GO requires the outstanding observed player acceptance below.

| Requirement | Evidence and disposition |
| --- | --- |
| Verified baseline and safe evidence integration | `0d79860` is the user-authorized baseline; the two evidence commits remain on their remote branch. Sanitized integration is `e55c518`. |
| Atlas F1/F2 distinction | Reproduced four failures, then corrected Atlas/JEI labels without changing identities (`b07ed16`). |
| Deterministic checks and full build | 33 suites, full build and documentation pass through the unified entry point. Five slot-loss failures were reproduced before the ADR-0125 repair. |
| Registered F2 lifecycle | All 16 assertions and a clean exit pass on the actual built jar under production Forge. The old dev-launch hang is isolated and remains documented. |
| Complete safe smoke inventory | 41/41 distinct Windows production scenarios completed across recorded batches. Two interrupted aggregates remain explicitly interrupted. The built and loaded jar hashes match; no local server remains. |
| Asset/data preparation | 25 existing T0 PNGs and the audit graph pass; isolated regeneration matches 1,377 files. No new art or gameplay was introduced. |
| Independent review | Limited supplied-excerpt review completed; one installer-completeness finding reproduced and repaired. No repository-wide independently authenticated clearance is claimed. |
| Client and survival acceptance | **Not executed.** Real UI, actual F2 player claim/sync, retained-world restart/compatibility and no-command BP route remain mandatory. |

The [current validation ledger](../testing/latest-validation.md) preserves commands, failures,
owned-process thread dumps and limitations. No tests were disabled; a forced cleanup is
deliberately not reported as successful shutdown.

See [all scenario evidence](../testing/integration-smoke-results.md) and
[review limits](foundation-independent-review.md). Corrected Linux CI has completed green runs;
the ledger identifies their exact revisions and preserves a later intermittent Planner failure.
The post-Planner-fix PR CI at `e2dfc2c` is also fully green (verify and six smoke shards).
CI state never replaces Windows or human acceptance.

**Exact next task:** perform and record
[the no-command survival and client checklist](../testing/t0-survival-acceptance.md)
using the validated jar. Back up any existing world used for migration checks. The historical
compact format never recorded empty slots; the new format prevents future loss but cannot
infer every original sparse position.

Under that earlier assignment, the proposed T0 graph/specification, asset manifest and
research stay preparatory. Do not add a T2 consumer, expand the campaign, add registry IDs,
or build a speculative multiblock framework was its restriction. ADR-0129 supersedes that
restriction for the connected T0 workshop, not the remaining manual acceptance requirements.
The foundation draft pull request remains unmerged.
