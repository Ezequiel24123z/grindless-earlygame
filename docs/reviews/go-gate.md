# Foundation GO gate

**Result: CONDITIONAL GO**

Executable checkpoint: `e2dfc2c847dd220178572b83e7516a9949bb8b58`, 2026-10-17.
This is the current execution gate, separate from the archived previous-session decision.
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

Until the conditions are observed, the proposed T0 graph/specification, asset manifest and
research stay preparatory. Do not add a T2 consumer, expand the campaign, add registry IDs,
or build a speculative multiblock framework. The draft pull request remains unmerged.
