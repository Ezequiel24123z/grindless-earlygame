# Active foundation handoff

Date: 2026-10-16. Phase A: sanitized verification integration.
Branch: `ezequiel24123z-agent-ready-t0-foundation`.
HEAD at intake and authorized source baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
The commit containing this file records the evidence-integration checkpoint; do not confuse
the source revision under test with a later documentation commit.

- Original verification commits: `bcdb162a`, `46eb83fe`, preserved on their remote branch.
- Completed unit: classify all five files, archive historical handoff/validation, remove
  machine-specific prerequisite paths and record [the integration decision](../docs/integration/verification-branch-integration.md).
- Current environment: initial `java -version` failed (not on PATH). Reusing the existing
  isolated JDK produced `javac 17.0.20.1` and Gradle 8.8; new Gradle caches stay under
  workspace `.gradle/user-home`. No system installation or identity reconfiguration.
- Current source compile, behavior checks and smokes: not yet run. Old passes are historical.
- Gate: no GO claimed. F1/F2 Atlas, runtime NBT, complete smokes and interactive survival
  acceptance remain open. The missing old architecture roadmap has not been recovered.
- Background checks: none at this snapshot. Worktree contains only this coherent
  documentation checkpoint before its commit; verify status after publication.
- Evidence: [current validation](../docs/testing/latest-validation.md), archived reports and
  [pending checks](pending-checks.json). Raw new executable logs will use `build/validation/`.

**Next safe task:** characterize Atlas F1/F2 presentation and current Matrix semantics.
Fix only the rated-item seam, then prove NBT and machine reconstruction. Keep BP active and
T2-consumer/broad T0 work held until the new GO gate is honestly closed.
