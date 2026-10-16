# Current foundation validation

Date: 2026-10-16. Authorized baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Branch: `ezequiel24123z-agent-ready-t0-foundation`. All commands run at this worktree's root
unless stated otherwise. Production sources are unchanged at the evidence-integration checkpoint.

This is a new execution ledger, not a promotion of [historical passes](previous-session-validation.md).
The [integration record](../integration/verification-branch-integration.md) explains provenance.

| Check | Start / end state | Exit and result | Evidence |
| --- | --- | --- | --- |
| Git status, HEAD, remote URLs, worktrees, fetch and evidence diff | Clean `0d79860`; fetched main equals local HEAD; two evidence commits above it | 0; no production difference or conflict | Tool executions `foundation-intake`, `evidence-diff` |
| `gh api user`, repository permissions, protection and rules | Existing authentication; no configuration change | Repository login `Ezequiel24123z`, push allowed; protection 404, rules empty | `foundation-intake`; 404 means unprotected, not an authentication failure |
| `java -version` | Original PATH | 1; command not found, before source compilation | `baseline-environment`; environmental, not introduced by code |
| Existing isolated JDK's `javac -version`; `.\gradlew.bat --version` | Missing PATH entry; explicitly configured `JAVA_HOME`, workspace-local `GRADLE_USER_HOME` | 0; Java 17.0.20.1 and Gradle 8.8, Windows amd64 | `restore-workspace-toolchain` |

The JDK was reused, not installed globally. Set `JAVA_HOME` to a valid JDK 17 in each shell;
its private location is not a repository dependency. New Gradle files use
`$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle\user-home'`.
Raw build/check logs will be ignored files under `build/validation/`; smoke outputs use
`forge/run/smoke-<scenario>.log` and `forge/run/smoke-report.html`.

## Not yet established

Current source compilation, all behavior checks, full build, F2 runtime/NBT coverage,
complete isolated smokes and interactive acceptance have not yet run. Gradle `test NO-SOURCE`
must never be described as a passing unit suite. There is no current GO decision.
The supported executable unit layer is `tools/checks/Verify*.java`.
