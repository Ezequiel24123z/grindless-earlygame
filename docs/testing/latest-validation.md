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

## Phase B execution, before its corrective commits

The following commands ran on baseline `e55c518` plus the named working-tree patches, not on
an already published result. The first source compile still used unchanged production Java.
The commit history identifies the following runner repair and Atlas patch separately.

| Check | Time (UTC+02:00) / start state | Exit / result | Ignored evidence |
| --- | --- | --- | --- |
| `.\gradlew.bat --no-daemon --console=plain :common:compileJava` | 22:58:02-23:04:23; unchanged baseline, cold workspace cache | 0; common compiled; a dependency source-remapping warning is not a compilation failure | `build/validation/baseline-compile.log` |
| Direct `javac` characterization without the game classpath | Before the supported runner; isolated output directory | 1; missing Minecraft types, not a behavioral result; superseded by the supported runner below | Tool execution `atlas-characterization` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .` | 23:05:55; compiled classes present, original cache-scanning runner using the explicit cache | 1; "Could not locate a mapped Minecraft jar"; environment/tool defect | `build/validation/atlas-characterization.log` |
| Same runner command after Gradle classpath repair, before Atlas production changes | 23:08:19-23:09:16; baseline Java and new characterization assertions | 1; exactly four Atlas rating assertions fail, the other 29 suites pass; expected red characterization | `build/validation/atlas-red-checks.log` |
| Same runner command with the Atlas repair | 23:10:38 onward; explicit rating labels and preserved bare-id lookup | 0; all 30 suites pass, no mixed-SLF4J-cache initialization error | `build/validation/atlas-green-checks.log` |
| `.\gradlew.bat --no-daemon --console=plain build` | Completed 23:12:01; same patch, including the optional JEI bridge | 0; full build; Gradle test tasks are NO-SOURCE | `build/validation/atlas-build.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -Only states` | 23:12:55-23:15:22; unused port 25599, explicit EULA opt-in | 0; Forge boots, handshake and all state markers pass | `build/validation/atlas-states.log`, `forge/run/smoke-states.log` |

The supported runners now ask Gradle to write
`common/build/checks/runtime-classpath.txt`; they neither guess a mapped jar nor mix every
SLF4J version from a shared cache. The `.sh` twin no longer discards suite stderr.
This repairs the validation environment, not the game. Linux/macOS execution is not claimed.
F2 runtime persistence and interactive/client acceptance remain unproved at this checkpoint.
