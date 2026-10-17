# Foundation validation strategy

The fast deterministic entry point is:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate.ps1 -Root .
```

For the integration gate, opt into the Minecraft EULA and add the isolated smoke inventory:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate.ps1 -Root . -Smokes
```

Linux/macOS:

```bash
bash tools/validate.sh .
GRINDLESS_ACCEPT_EULA=true bash tools/validate.sh . --smokes
```

Prerequisites: Java 17 (`JAVA_HOME`), Git, network access on a cold Gradle/Forge cache, and
PowerShell Core (`pwsh`) for the shared documentation validator on Linux/macOS. Missing tools
fail explicitly. Versions remain in `gradle.properties`; neither script installs globally or
changes credentials. `GRADLE_USER_HOME` may point inside the worktree.

## Scope and evidence

| Layer | Real entry / authority | What it proves |
| --- | --- | --- |
| Environment/build | Committed wrapper, common compilation and full build | Actual Java/Gradle versions, compile/type compatibility and packaged artifacts; Gradle `test NO-SOURCE` is not a passing test suite |
| Behavior/resources | `tools/run-checks.ps1` / `.sh`, Gradle's resolved classpath | Every `Verify*.java` suite, actual NBT/persistence, recipe/quest policy, resource references, graph/asset preparation and platform-runner failure propagation |
| Documentation | `tools/check-links.ps1`, shared by `.sh` through `pwsh` | Tracked/new non-ignored Markdown files, relative anchors and ADR index |
| Integration | `tools/run-smokes.ps1` / `.sh`, `tools/smoke/scenarios.txt` | Fresh production Forge worlds loading the actual remapped jar; each scenario's declared assertions and natural exit 0 |
| Player acceptance | [Exact checklist](t0-survival-acceptance.md) | Real client UI, player claims, saved-world restart and no-command Bootstrap completion |

Each unified run writes `build/validation/<run-id>/source.txt`, stage logs and `status.tsv`
with UTC starts/finishes and actual exit codes. That ignored output is execution evidence;
preserve the meaningful results and tested revision in [latest-validation.md](latest-validation.md)
and [the active handoff](../../.agent/handoff.md). CI retains stage logs as artifacts.

The fast gate explicitly omits smokes unless requested. It is not full integration or Tier
acceptance. The full matrix can be split/resumed with the existing `-Only` / `--only` selectors
but each batch must retain its own revision, selected names, outcome and logs. An interrupted
aggregate remains interrupted even if a subsequent complementary batch closes the inventory.

## Isolation and recovery

Run only one Gradle/generated-output or game-world mutator in a worktree at a time. Use
read-only analysis or documentation while a smoke inventory is running; do not mutate its
runtime or scenario scripts. The server listens on loopback, uses a fresh `smoke-world` and
cleans only that named disposable directory. Failed shutdown is a failure, even if assertions
ran. A forced cleanup is not a clean exit.

Installer-owned libraries are distinct from mutable worlds and staged mod jars. The explicit
runtime inventory causes a missing/truncated library to be repaired, while `remapJar` dependencies
ensure the staged Grindless artifact is current. Hash the staged and built jars when recording
release-gate evidence; do not claim byte-for-byte reproducible builds from a single match.

The optional `:forge:runServer` development path has a separately recorded shutdown hang.
Integration uses the production path under ADR-0126, not reflective worker cleanup or a forced
`System.exit`. macOS runtime execution, installed-pack client behavior and old ambiguous compact
saves are not automatically established by Windows/Linux server passes.
