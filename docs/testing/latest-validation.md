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

## Initial intake status (superseded by the execution records below)

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

## F2 lifecycle corrective checkpoint

Source: `b07ed16` plus the ADR-0125 patch. `VerifyPersistence` first reproduced **five** failures
using the same vanilla compact-container methods as the original process machine; the ordinary
item packet retained its tag. After the slot-preserving implementation, the supported runner
passes **31 suites**, including binary NBT, sparse slots, repeated reconstruction, legacy compact
data, all integral numeric rating types, malformed/overflowing ratings and offhand counting.

| Check | Current result | Evidence |
| --- | --- | --- |
| Targeted `javac`/`java` for `VerifyPersistence` against Gradle's exported classpath | Exit 1, five expected pre-fix slot failures | `build/validation/persistence-red.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .` | Exit 0, 31 suites | `build/validation/persistence-checks.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -Only matrix-lifecycle` | 23:32:20-23:35:34, exit 1; all 16 gameplay assertions pass, launcher does not exit within 60 seconds | `build/validation/matrix-smoke.log`, `build/validation/matrix-initial-shutdown.log` |
| Same scenario after explicitly closing stdin | 23:37:09-23:40:06, exit 1; EOF alone does not repair shutdown | `build/validation/matrix-smoke-eof.log` |
| Plain boot plus thread dumps of only the probe's owned Java processes | Same hang without the Matrix scenario; world saving completes, non-daemon executor pools remain | `build/validation/shutdown-probe-server.log`, `shutdown-probe.out`, `shutdown-8592.log`, `shutdown-13344.log`, `shutdown-18696.log` under the same directory |
| Port 25599 after failure cleanup | No listener; no unrelated process stopped | Tool execution `matrix-smoke-postcondition` |

The new smoke covers exact F1 input, F2/fraction/string/overflow/architecture rejection,
legacy untagged F1, old output-slot migration, retained compact stacks, a zero-progress
charcoal-powered full cycle, mid-cycle reconstruction, output NBT reconstruction, hopper
extraction and blocked-output recovery without double consumption. The second blocked branch
alone starts near completion; the first cycle is not shortened. These observations are not
a passing smoke because its process lifecycle fails.

The stricter runner exposed a pre-existing dev-launch behavior that the earlier implementation
silently killed after waiting. It is not an F2 assertion failure, but it must not be suppressed.
EOF was a bounded unsuccessful attempt, retained here. The repository creates no executor pool
in its Java sources; exact ownership of the eight non-daemon pools is not yet proven.
No full integration smoke pass or GO is claimed. Interactive client screens, an actual player
quest claim and the no-command survival route also remain unexecuted.

## Production-artifact smoke runtime

Source Java: `b48e3f6`; runtime preparation changed under ADR-0126, without a Forge or
Architectury version change. The official installer and its libraries stay inside the
worktree's ignored `forge/run/smoke-server`. No host-wide installation is required.

| Command | Time (UTC+02:00) | Exit / observed result | Evidence |
| --- | --- | --- | --- |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -Only boot` | 23:52:40-23:54:32 | 0; official server installed, built jar boots and exits cleanly | `build/validation/production-boot.log`, `forge/run/smoke-boot.log` |
| Same command with `-Only matrix-lifecycle` | 23:56:10-23:57:58 | 0; all 16 F2 markers and clean exit, no forced termination | `build/validation/production-matrix.log`, `forge/run/smoke-matrix-lifecycle.log` |
| SHA-256 comparison of built jar and staged `mods/grindless.jar` | After the successful scenario | Equal; value below | Tool execution `production-matrix` |
| Port 25599 after the successful scenario | After Java exited | No listener | Same tool execution |

Built and loaded SHA-256:
`e0267ee23fdcfde776749df069e29effc72379dfca3b4fb58dc0b0e95a307866`.

This isolates the prior hang to the dev-launch path; it does not identify or patch the exact
upstream executor owner. The canonical integration path now tests what is shipped, including
loading without optional JEI. The first installation needs network access; the second preparation
was entirely up-to-date. Complete inventory and independent review are next, not inferred passes.

## Linux dispatch correction

The `verify` job of [CI run 38968479378](https://github.com/Ezequiel24123z/grindless-earlygame/actions/runs/38968479378)
passed, but its smoke shard 3 repeatedly executed Atlas assertions for other named scenarios.
The raw failed-job log is `build/validation/prior-linux-ci-failure.log`. The source defect and
its evidence consequence are recorded in ADR-0127.

| Check | Result | Evidence |
| --- | --- | --- |
| `VerifySmokeHarness --bash <Bash executable>` before the fix | Exit 1 from the regression check: runner dispatched `gamma` three times and falsely exited 0 | `build/validation/bash-dispatch-red.log` |
| Same fixture after the fix | Exit 0: exact `alpha,beta,gamma`; the intentional beta failure propagates; report agrees | `build/validation/bash-dispatch-green.log` |
| `VerifySmokeHarness` on Windows | Exit 0 with the native PowerShell twin and the same failure contract | `build/validation/powershell-dispatch-green.log` |
| `bash -n` on the three changed shell runners | All exit 0 | Tool execution `smoke-dispatch-green` |

No Bash was on PATH or in the app's minimal Git distribution. After that missing-tool result,
Portable Git `v2.56.0.windows.2` was installed only under ignored `build/tooling/portable-git`.
The [official release](https://github.com/git-for-windows/git/releases/tag/v2.56.0.windows.2)
archive matched its GitHub SHA-256
`075e158ef8e1f0ab80b347e245405d3eca735c2dc88fd8e032e137d0ca61f61b` and had a valid
Authenticode signature before execution. No key, global PATH or Git identity was changed.
This is a local test tool, not imported mod material.

The Windows full inventory started at **2026-10-17 00:03:19 UTC+02:00**, source `0ca31e3`.
On resumption at 13:07, the shell no longer existed. Its durable log records **23 completed
passes**, then stops during preparation of scenario 24 (`first-drill`) at approximately 00:36.
There is no final exit code or full-inventory success marker: this run is **interrupted**,
not 41 passed. The server port is closed. Logs remain in `build/validation/full-smokes.log`;
the remaining 18 scenarios must be executed separately.

[Corrected Linux CI run 38974943530](https://github.com/Ezequiel24123z/grindless-earlygame/actions/runs/38974943530)
at `3eb7351` completed with `verify` and all six smoke shards successful. It uses the corrected
dispatch and production runtime. Runs `38973202746` and `38972106991` were cancelled and are
not passes. Linux evidence does not replace the remaining Windows desk work.

## Gated preparation and resumed fast validation

`VerifyT0Preparation` ran directly against the exported classpath: exit 0, graph references/
cycles/missing-prerequisite fixtures and **25 distinct PNGs** checked. It checks PNG signature,
16x16 dimensions, 8-bit RGBA encoding, declared model/state coverage, shared body texture
references and idle/running pixel difference. It does not certify hero art, future animation
mask continuity, scientific recipe balance or no-command survival.
Evidence: `build/validation/t0-preparation.log`.

At 13:08-13:12 on 2026-10-17, the resumed build and all **33 behavior suites** passed with
the preparation artifacts and pending validation-tool patches:
`build/validation/review-fix-build.log` and `build/validation/all-foundation-checks.log`.
The new single-entry fast validator also passed environment, build, behavior and documentation;
its complete timestamp/exit ledger is `build/validation/20261017T111720160Z/status.tsv`.
Full smokes were explicitly not requested in that fast run.

Image-provider configuration was checked without displaying credentials: both supported keys
were absent. No image-generation call was made. The manifest includes prompts, atlas layout,
metadata and QC rules; existing generated placeholders remain unchanged.

## Review remediation and unified entry point

The [independent excerpt review](../reviews/foundation-independent-review.md) found one concrete
installer-completeness defect. It did not independently authenticate the checkout or log files.
The first repair attempt (directory output declaration alone) still left the installer up to
date after a library was removed; the known local backup was restored. The final explicit
path/size inventory forced the official installer to repair that missing jar, with matching
SHA-256. Both attempts are retained in `build/validation/installer-repair.log`,
`installer-inventory.log` and `installer-repair-verified.log`.

The final unified fast command is
`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate.ps1 -Root .`.
Environment, full build, **33 suites** and all links pass with stage exits 0 in
`build/validation/20261017T112251069Z/status.tsv`. A separate nonexistent-JDK fixture exits
nonzero at the first stage with an explicit error and does not launch a build/smoke:
`build/validation/unified-failure-verified.log`. Native compiler stderr is retained; actual
exit codes decide success. Bash/PowerShell script syntax passes.

Local `bash tools/check-links.sh .` returned 1 because `pwsh` is unavailable in this Windows
environment. The native Windows shared checker passes; Linux execution of the new wrapper
must be confirmed by the current CI rather than claimed from a syntax pass. No optional
prerequisite failure is silently counted as success.
