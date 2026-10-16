# Active foundation handoff

Date: 2026-10-16. Phase B: production-artifact runtime repaired; complete inventory next.
Branch: `ezequiel24123z-agent-ready-t0-foundation`.
Authorized source baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Last published HEAD before this checkpoint: `b48e3f64a1726ff0b56cdf9024b0e0ffb55fe7b6`.
The commit containing this file records the evidence-integration checkpoint; do not confuse
the source revision under test with a later documentation commit.

- Original verification commits: `bcdb162a`, `46eb83fe`, preserved on their remote branch.
- Completed unit: classify all five files, archive historical handoff/validation, remove
  machine-specific prerequisite paths and record [the integration decision](../docs/integration/verification-branch-integration.md).
- Current environment: initial `java -version` failed (not on PATH). Reusing the existing
  isolated JDK produced `javac 17.0.20.1` and Gradle 8.8; new Gradle caches stay under
  workspace `.gradle/user-home`. No system installation or identity reconfiguration.
- Current evidence: baseline compile passes; the repaired Gradle-classpath runner reproduces
  four Atlas failures before its fix, then all 30 checks, full build and `states` pass with
  the prepared Atlas/JEI patch. Exact commands, times and logs are in the current ledger.
- Completed commits: `e55c518` evidence, `735daa4` check classpath, `b07ed16` Atlas/JEI.
  This checkpoint adds ADR-0125, indexed process inventory NBT, strict rating decoding,
  single-count quest evidence and the registered-item lifecycle scenario.
- Gate: **NO-GO pending the complete inventory and independent review**. The ADR-0126
  production-artifact path passes plain boot and all 16 F2 assertions with clean exit. Its mod
  jar matches the build hash. The separate dev-launch hang remains documented, not hidden.
- Background work: none. External research finished without edits or imported material.
  Its canonical pinned sources await the research document. No unrelated files are staged.
- Evidence: [current validation](../docs/testing/latest-validation.md), archived reports and
  [pending checks](pending-checks.json). Raw new executable logs will use `build/validation/`.

**Next safe task:** run all 41 isolated production smokes and an independent gate review.
While they run, write the pinned-source research and non-gameplay preparation documents.
BP, player acceptance and broad T0 changes remain held.
