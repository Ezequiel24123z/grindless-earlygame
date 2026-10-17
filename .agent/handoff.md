# Active foundation handoff

Date: 2026-10-17. Phase B gate closure and non-gameplay T0 preparation.
Branch: `ezequiel24123z-agent-ready-t0-foundation`.
Authorized source baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Last published HEAD before this checkpoint: `3eb7351d45a3e1b2295bc5b796b651bfd62d099f`.
The commit containing this file records the preparation checkpoint; do not confuse
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
- Interrupted check: the full Windows run completed 23/41 scenarios, then stopped while
  preparing `first-drill`. No aggregate exit code exists; the process and port are now gone.
  Resume the remaining 18, preserving `build/validation/full-smokes.log`.
- Corrected Linux CI `38974943530` at `3eb7351`: verify and all six shards pass. Earlier cancelled
  or mis-dispatched jobs do not count.
- Preparation: research, licence-screening register, T0 contract/audit graph, asset manifest,
  prompts and exact human acceptance checklist are written, with no new gameplay or imports.
  All 33 suites pass; the asset check verifies 25 existing PNGs. Image keys are absent.
- Independent excerpt review found incomplete installer-output tracking. The fix now records
  installed libraries; a missing official jar was removed and correctly regenerated with the
  same SHA-256. The full review limits and failed first repair attempt still need recording.
- Pending coherent tooling patch: single validation entry, installer completeness and moving the
  existing Shadow version into gradle.properties without changing its value.
- Background checks: none at this snapshot.
- Evidence: [current validation](../docs/testing/latest-validation.md), archived reports and
  [pending checks](pending-checks.json). Raw new executable logs will use `build/validation/`.

**Next safe task:** checkpoint the validation-tool patch, execute the 18 remaining Windows
scenarios, finalize the honest gate and create a draft PR. BP, player acceptance and broad
T0 changes remain held. Do not claim this proposal is a finalized expanded Tier.
