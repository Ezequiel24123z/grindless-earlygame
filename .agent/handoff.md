# Active foundation handoff

Date: 2026-10-16. Phase B: F2 persistence repaired; strict smoke shutdown blocked.
Branch: `ezequiel24123z-agent-ready-t0-foundation`.
Authorized source baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Last published HEAD before this checkpoint: `b07ed1678b3e48bf1ae1765d5378b97aed0a0ad2`.
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
- Gate: **NO-GO while shutdown is broken**. All 31 checks pass and all 16 F2 scenario
  assertions pass, but its launcher hangs after saving every dimension; EOF did not repair it.
  Three bounded probes and owned-process thread dumps are recorded in the current ledger.
- Background work: none. External research finished without edits or imported material.
  Its canonical pinned sources await the research document. No unrelated files are staged.
- Evidence: [current validation](../docs/testing/latest-validation.md), archived reports and
  [pending checks](pending-checks.json). Raw new executable logs will use `build/validation/`.

**Next safe task:** eliminate the dev-launch dependency from the smoke path by validating the
actual built jar on an isolated, official production Forge server, keeping strict exit checks.
Do not use reflection, forced JVM exits or weaker assertions to turn the hang green. Then run
the complete isolated inventory. BP, player acceptance and broad T0 changes remain held.
