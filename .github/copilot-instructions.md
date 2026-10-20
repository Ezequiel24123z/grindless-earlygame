# Grindless agent entry point

Read [AGENTS.md](../AGENTS.md) first. It is the working agreement, not a supplement to a
generic plugin workflow. Continue through the changelog, build-out, ADRs and README plan.

Use [the active handoff](../.agent/handoff.md),
[pending checks](../.agent/pending-checks.json) and
[revision-bound validation](../docs/testing/latest-validation.md) for execution state.
Archived previous-session reports are historical evidence, not current passes.

Before code changes, use [the task-indexed route map](../docs/AGENT-MAP.md). Preserve the
catalogues as sources of truth and keep loader-specific types out of common.
Do not turn a test fixture, a proposed Tier document or an agent's unsupported summary into
accepted gameplay.

The [current gate](../docs/reviews/go-gate.md) separates authorized T0 development from merge
acceptance under ADR-0129. No-command survival, client interaction and saved-world compatibility
must be described honestly. Commit coherent
checkpoints with the required documentation; never hide a cancelled, skipped or failed check.
