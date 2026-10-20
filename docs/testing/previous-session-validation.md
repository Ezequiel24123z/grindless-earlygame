# Previous-session local validation

**Historical evidence only.** Imported from verification commits `bcdb162a` and `46eb83fe`.
Machine-specific worktree, log and JDK paths were removed, not re-executed. See the
[integration decision](../integration/verification-branch-integration.md) and
[current validation](latest-validation.md) before using these results.

Date: 2026-10-16. Source under test:
`0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Verification branch: `t-ecastano-microsoft-previous-session-verification`.

**Completed: shared compilation, all 30 behavior suites, full build and four isolated
smokes pass. This is not acceptance of the missing Astra checkpoint or the complete campaign.**
The [verification report](../reviews/previous-session-verification.md) records a NO-GO for
architectural continuation because the expected previous-session checkpoint is unidentified.

## Environment and evidence locations

All checks ran from the original verification worktree root on Windows. Its machine-specific
location is deliberately not an executable prerequisite for another checkout.

PowerShell is 5.1.26100.9711. Initially neither `java` nor `javac` was on PATH and
`JAVA_HOME`/`GRADLE_USER_HOME` were unset. The repository's documented local JDK locations
contained no JDK. After that observed failure, Temurin `17.0.20.1+1` was downloaded into
session-local storage; no machine-wide installation or dependency-manifest change was made.

Raw logs were outside Git in that session's artifact storage. Their basenames below and recorded
hashes are retained as historical evidence; availability of those old files is not guaranteed.

This committed ledger preserves the results even if session-local raw logs become unavailable.
Build products and temporary toolchain files are not repository artifacts.
For JDK-dependent commands, each fresh shell explicitly set `JAVA_HOME` to that session's
isolated JDK 17 and prepended its `bin` directory to `PATH`. The exact private location is
intentionally omitted rather than presented as a portable command.

The isolated JDK is a local validation prerequisite, not a supported application dependency
or an assumed permanent path. A subsequent environment should point `JAVA_HOME` at its own JDK 17.

## Execution ledger

Every row uses the working directory above. Unless specified otherwise, no separate
HTML/XML report is generated: the listed raw log and this ledger are the report.
Times are local UTC+02:00.

| Check / exact command | Start condition | Exit / observed result | Log |
| --- | --- | --- | --- |
| `git diff --check b78e5bc 0d79860` | Clean candidate tree | 0; no whitespace errors | `06-git-recovery.log` |
| `git fsck --full --no-reflogs --unreachable` | Existing non-shallow clone; no recovery writes | 0; no unreachable objects reported | `06-git-recovery.log` |
| `git ls-remote --heads origin` | Network available; read-only | 0; live main equals candidate | `06-git-recovery.log` |
| `java -version` | Original PATH, no `JAVA_HOME` | Shell exit 1: command not found; original combined prerequisite probe stopped here | `02-prerequisites.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\check-links.ps1 -Root .` | Original tracked documentation, no JDK needed; 22:02:27 | 0; 373 links, 12 Markdown files, 123 ADR sections/index rows | `03-initial-checks.log` |
| `.\gradlew.bat --version` | Original environment | 9009; missing Java, no Gradle execution | `03-initial-checks.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .` | Original environment; no compiled classes | 1; JDK 17 required, no suites executed | `03-initial-checks.log` |
| Temurin restoration from the official Adoptium API, SHA-256 comparison, `javac -version` | Missing JDK reproduced; 22:03:13-22:03:37 | 0; archive verified, `javac 17.0.20.1` | `05-jdk-restore.log` |
| `.\gradlew.bat --version` | Restored isolated JDK, 22:04:11 | 0; Gradle 8.8, JVM 17.0.20.1, Windows amd64 | `07-compile.log` |
| `.\gradlew.bat --no-daemon --console=plain :common:compileJava` | Wrapper works; cold dependency cache; finished 22:11:13 | 0; BUILD SUCCESSFUL, task executed, Gradle duration 6m 46s | `07-compile.log` |
| PowerShell AST parse of `git ls-files '*.ps1'` using `Parser.ParseFile` | Read-only tracked scripts, independent of build output | 0 parse failures across 8 scripts | `08-static-inventory.log` |
| JSON parse of `git ls-files '*.json'` using `ConvertFrom-Json -ErrorAction Stop` | Read-only tracked resources | 0 parse failures across 910 JSON files; syntax only, not game schema validity | `08-static-inventory.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -List` | Manifest-only; no game boot | 0; 40 scenario names listed | `08-static-inventory.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\check-links.ps1 -Root .` | Five documentation changes staged, 22:09 | 0; 380 links, 15 Markdown files, 123 ADR sections/index rows | `09-document-checkpoint.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .` | Fresh successful shared compilation and JDK 17; 22:11:26-22:11:58 | 0; all 30 suites executed and passed, including assets, Atlas, recipes, quests, machines and fixed structures | `11-behavior-checks.log` |
| `.\gradlew.bat --no-daemon --console=plain build` | Behavior checks finished; same source tree; 22:12:07-22:13:20 | 0; BUILD SUCCESSFUL, 18 tasks (17 executed, one up-to-date); root/common/Forge test tasks all NO-SOURCE | `12-full-build.log` |
| `powershell -ExecutionPolicy Bypass -Command "& .\tools\run-smokes.ps1 -Root . -Only @('states','first-assembler','first-atlas','first-book')"` | Full build passed; port 25599 verified unused; EULA opt-in and JDK set; 22:13:52-22:23:12 | 0; all four isolated scenarios passed; HTML report: 4 passed, 0 failed, 9m 19s | `13-targeted-smokes.log` |
| `Get-FileHash -Algorithm SHA256 -LiteralPath .\forge\build\libs\grindless-0.1.0-forge.jar` | Full build finished, 22:13:49 | Artifact exists; hash below | `14-artifact-identity.log` |
| `git diff 0d79860 -- common forge build.gradle settings.gradle gradle.properties tools` | Documentation-only verification work | 0; empty diff, production inputs unchanged | `14-artifact-identity.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\check-links.ps1 -Root .` and `git diff --check` | Updated evidence, 22:20:51-22:20:53 | Both 0; 380 links across 15 Markdown files, no whitespace errors | `15-evidence-review.log` |
| `Get-NetTCPConnection -State Listen -LocalPort 25599 -ErrorAction SilentlyContinue` | Smoke runner finished; 22:23:31 | No listeners; server did not remain on the smoke port | `16-smoke-postconditions.log` |
| `git diff 0d79860 -- common forge build.gradle settings.gradle gradle.properties tools` and `git ls-files --others --exclude-standard` | All executable validation finished | 0; no production changes and no untracked non-ignored files | `16-smoke-postconditions.log` |

The static inventory also compared top-level `tools/*.ps1` and `tools/*.sh` basenames.
Three missing twins were reported: `check-links.sh`, `convert-audio.ps1` and
`run-smokes-batch.sh`. This diagnostic did not turn the syntax-check exit into a failure;
it is explicitly a pre-existing portability finding, not a successful parity check.

JDK source:
`https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip`

Verified SHA-256:
`e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0`.

Built artifact: `forge/build/libs/grindless-0.1.0-forge.jar`.
SHA-256: `bebe7d898eca79f2e780af8b56dd442b3817a86f0ef2ec80ab7d1c2fc0cc478f`.
This identifies the observed jar; no byte-for-byte reproducible-build claim is made.

Smoke-only environment:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
$env:SMOKE_PORT = '25599'
```

The runner writes `forge/run/smoke-report.html` and one
`forge/run/smoke-<scenario>.log` per scenario. These ignored local reports supplement the
session log, not the committed evidence ledger. They are not historical acceptance of the
remaining 36 scenarios or the no-command survival route.

| Scenario | Result / duration | Observed scope |
| --- | --- | --- |
| `states` | PASS / 2m 21s | Forge boot, handshake and expected block-state placement markers |
| `first-assembler` | PASS / 2m 28s | Press/Assembler placement, die retention, hopper insertion and F1 `MATRIX-RECIPE-OK` selection |
| `first-atlas` | PASS / 2m 12s | Boot and `ATLAS-OK` item marker, not a client screen inspection |
| `first-book` | PASS / 2m 18s | Boot, `BOOK-OK` and `GUIDE-OK` item markers, not an interactive quest claim |

Each child smoke exits 0 as checked by the runner before marking its row passed.
The runner also requires `Done` and `SMOKE-READY`; a build alone never substitutes for these.
The scenario manifest's phrase "exist and open" is broader than `first-book.commands`,
which summons the items but does not open a player screen.

Raw smoke log SHA-256 values, recorded after all servers stopped:

| Log basename | SHA-256 |
| --- | --- |
| `smoke-states.log` | `19ba6670a4f70f7d4951c0580cb02481fc5709c0de08c4efafaf7261a1dfbb17` |
| `smoke-first-assembler.log` | `8783aec0e152afae789836e1e55f67c72d3e96a914b5d5d11a1b2ef0a0b14656` |
| `smoke-first-atlas.log` | `7074ab4459f51f3dc63ae34ccc61213c8372e99865f5b0b0e3e3b1033e9de6e1` |
| `smoke-first-book.log` | `8765a1b841a2ed7a5ddf0f6dddd3e780fdaa5beb460d0d92cf260c02582b390b` |

### Warnings, not hidden passes

The first cold compile logged duplicate JEI input classes and failure to remap one
**dependency source** file, `ForgeBiomeModifiers.java`; compilation still executed and
returned 0. The behavior runner emitted its documented SLF4J `AbstractMethodError` during
three suites' logger initialization; each suite and the runner returned 0. The full build
reported deprecated APIs, common-code unchecked operations from compilation, and Gradle 9
incompatibility warnings. These warnings are retained in logs, not suppressed by changing
code, test assertions or exit handling.

No baseline warning replay was performed. The runner/build files responsible for dependency
selection and broad check classpaths are unchanged in the provisional candidate diff;
this supports treating them as pre-existing configuration issues, not proving their
historical runtime result.

No build, behavior-suite or targeted-smoke failure was observed after restoring Java.
No production code, runner, check or assertion was modified to obtain those results.

### Persistence

The provisional documentation commit is `bcdb162ae4baa7e3d0ca308514e818d003f1859e`.
The first commit attempt exited 128 because the fresh environment had no Git author identity.
The retry used per-command `git -c user.name=... -c user.email=...` for the Copilot App
identity; it changed no global/repository identity configuration.

`git push -u origin HEAD` was attempted with the configured transient authentication and
exited 128: GitHub returned **HTTP 403, permission denied**. The verification branch has
no upstream and the new evidence commit is **local only**. No alternate account, token,
remote, force push or permission bypass was attempted. Publishing requires an authorized
repository contributor; this is distinct from the already-published source candidate.

## Baseline comparison and coverage

There is no old validation report or confirmed Astra baseline against which to compare these
results. The comparison parent is provisional, not an attributed session boundary.
Missing Java is an environment failure before source compilation, not a demonstrated source
regression. Validation runners and build configuration are unchanged in `b78e5bc..0d79860`.
For **every execution row**, comparison to the previous Astra run is therefore unavailable;
no introduced-vs-baseline runtime failure is claimed. Current passes broadly agree with the
README's historical claims, but do not reproduce its unspecified revision or entire smoke matrix.

The repository has no `src/test` suite in this checkout; its 30 `tools/checks/Verify*.java`
programs are the supported behavior checks. They include assets, recipes, machines, quests,
factory dimensions and fixed structures. Gradle `test NO-SOURCE`, if observed, must not be
reported as passing unit tests.

No validation commands that share Gradle output, mapped jars or game state run concurrently.
Only read-only Git/resource/script inspection runs alongside compilation.

## Acceptance boundary and next action

Executable verification of the available candidate is complete at the stated scope.
The 36 unselected scenarios, client UI interactions, F2 production/reload/quest acceptance
and no-command T0 survival playthrough were **not run**. This is a verification checkpoint,
not branch-integration or tier-acceptance evidence; no full historical matrix is claimed.

Recover the missing Astra baseline/checkpoint/handoff before architectural continuation.
Evidence was preserved in local Git commits and the original session logs. Publishing remained blocked
by the observed 403 and requires an authorized contributor.
