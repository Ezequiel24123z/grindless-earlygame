# Latest local validation

Date: 2026-10-16. Source under test:
`0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Verification branch: `t-ecastano-microsoft-previous-session-verification`.

**In progress; no green build or runtime acceptance is claimed at this document checkpoint.**
The [verification report](../reviews/previous-session-verification.md) records a NO-GO for
architectural continuation because the expected previous-session checkpoint is unidentified.

## Environment and evidence locations

All checks use the local Windows machine and this working directory:

```text
%USERPROFILE%\.copilot\repos\copilot-worktrees\grindless-earlygame\t-ecastano-microsoft-ubiquitous-potato
```

PowerShell is 5.1.26100.9711. Initially neither `java` nor `javac` was on PATH and
`JAVA_HOME`/`GRADLE_USER_HOME` were unset. The repository's documented local JDK locations
contained no JDK. After that observed failure, Temurin `17.0.20.1+1` was downloaded into
session-local storage; no machine-wide installation or dependency-manifest change was made.

Raw logs are outside Git at:

```text
%USERPROFILE%\.copilot\session-state\7dfd8c37-62a4-4ddf-86bd-5e899446bd37\files
```

This committed ledger preserves the results even if session-local raw logs become unavailable.
Build products and temporary toolchain files are not repository artifacts.
For JDK-dependent commands, each fresh shell explicitly sets:

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.copilot\session-state\7dfd8c37-62a4-4ddf-86bd-5e899446bd37\files\jdk17\jdk-17.0.20.1+1'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

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
| `.\gradlew.bat --no-daemon --console=plain :common:compileJava` | Wrapper works; cold dependency cache | Running | `07-compile.log` |
| PowerShell AST parse of `git ls-files '*.ps1'` using `Parser.ParseFile` | Read-only tracked scripts, independent of build output | 0 parse failures across 8 scripts | `08-static-inventory.log` |
| JSON parse of `git ls-files '*.json'` using `ConvertFrom-Json -ErrorAction Stop` | Read-only tracked resources | 0 parse failures across 910 JSON files; syntax only, not game schema validity | `08-static-inventory.log` |
| `powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -List` | Manifest-only; no game boot | 0; 40 scenario names listed | `08-static-inventory.log` |

The static inventory also compared top-level `tools/*.ps1` and `tools/*.sh` basenames.
Three missing twins were reported: `check-links.sh`, `convert-audio.ps1` and
`run-smokes-batch.sh`. This diagnostic did not turn the syntax-check exit into a failure;
it is explicitly a pre-existing portability finding, not a successful parity check.

JDK source:
`https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip`

Verified SHA-256:
`e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0`.

## Baseline comparison and coverage

There is no old validation report or confirmed Astra baseline against which to compare these
results. The comparison parent is provisional, not an attributed session boundary.
Missing Java is an environment failure before source compilation, not a demonstrated source
regression. Validation runners and build configuration are unchanged in `b78e5bc..0d79860`.

The repository has no `src/test` suite in this checkout; its 30 `tools/checks/Verify*.java`
programs are the supported behavior checks. They include assets, recipes, machines, quests,
factory dimensions and fixed structures. Gradle `test NO-SOURCE`, if observed, must not be
reported as passing unit tests.

No validation commands that share Gradle output, mapped jars or game state run concurrently.
Only read-only Git/resource/script inspection runs alongside compilation.

## Pending at this document checkpoint

- Observe shared compilation, then run all behavior checks, full build and appropriate
  isolated smokes if prerequisites permit.
- Record exact failures without altering production code to make them disappear.
- Revalidate final documentation after staging the new Markdown files, because the link
  checker intentionally enumerates `git ls-files`.
- Commit and push the final evidence; do not continue architecture work.
