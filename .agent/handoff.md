# Active foundation handoff

Date: 2026-10-16. Phase B: validation-runner repair and rated Atlas remediation.
Branch: `ezequiel24123z-agent-ready-t0-foundation`.
Authorized source baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Last published HEAD before this Atlas checkpoint: `735daa4dc75f80b06c463b5d8d2c11515603b08f`.
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
- The runner repair is commit `735daa4`; this checkpoint commits the verified Atlas/JEI
  rating repair and ADR-0124. Root `logs/` output from the resolved Minecraft logger is ignored.
- Gate: no GO claimed. F1/F2 Atlas, runtime NBT, complete smokes and interactive survival
  acceptance remain open. The missing old architecture roadmap has not been recovered.
- Background work: bounded read-only external T0 research; no build or smoke still running.
  This checkpoint leaves the game and test changes together and no unrelated files staged.
- Evidence: [current validation](../docs/testing/latest-validation.md), archived reports and
  [pending checks](pending-checks.json). Raw new executable logs will use `build/validation/`.

**Next safe task:** prove NBT and machine
reconstruction. Bytecode inspection confirms that vanilla `SimpleContainer.createTag/fromTag`
does not retain slot indices; characterize the Assembler output round-trip before repairing it.
Keep BP active and
T2-consumer/broad T0 work held until the new GO gate is honestly closed.
