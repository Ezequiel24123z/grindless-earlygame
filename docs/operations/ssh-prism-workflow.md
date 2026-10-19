# SSH-first PC and Prism workflow

The owner-selected execution model is **local Copilot coordination -> ordinary SSH ->
personal PC -> build/package -> dedicated Prism development instance -> manual playtest**.
GitHub preserves source and synchronizes machines. This conversation must not be converted
to a Copilot Remote Host session. See [ADR-0151](../DECISIONS.md#adr-0151--keep-coordination-local-and-execute-through-the-existing-personal-pc-ssh).

## Receive the right source

The verified starting checkpoint is `70901eca2da386cd54fc769ba8a2a59be10eb76b` on
`ezequiel24123z-t0-playable-release`, not old `main` (`0d79860`). The SSH-first work continues
on its own published branch; resolve its current checkpoint from Git rather than treating
this initial SHA as a permanently current candidate.

The completed [T0 audit](https://github.com/Ezequiel24123z/grindless-earlygame/blob/e3f0124/docs/T0-AUDIT.md)
and [T0-T1 action plan](https://github.com/Ezequiel24123z/grindless-earlygame/blob/e3f0124/docs/T0-T1-ACTION-PLAN.md)
were read, not rerun. They audit `0d79860`. The subsequent connected workshop,
S1/S2/C3, guidance, Heracles, art, audio and pack work is preserved in the received branch.
Use [the current T0 contract](../tiers/t0.md),
[playable-release plan](../../plan/process-t0-playable-release-1.md),
[task graph](../../.agent/work-plan.json) and [validation ledger](../testing/latest-validation.md)
for current scope. In particular, the old two-iron-only route and manual-before-delivery
ordering do not override ADR-0144/0150.

## Verified environment and identity separation

The personal Windows PC accepts noninteractive, strict-known-host SSH with its existing
login. It has a clean continuation clone, an older independent main clone, Java 17,
Git for Windows and Prism Launcher 11.0.2. The work volume has substantially more free
space than the system volume. The older main clone and app-created worktrees are not
execution targets.

| Identity / component | Observed state | Rule |
| --- | --- | --- |
| Coordination machine -> PC SSH | Existing key/login succeeds | Reuse it; never copy private keys or disable host-key checking. |
| PC -> GitHub Git SSH | Existing alias authenticates as `Ezequiel24123z` and reads the exact continuation head | Keep its origin and key configuration unchanged. |
| PC `gh` API login | Stored personal token is invalid | Not a Git transport failure; no account switch or reauthentication is needed for this loop. |
| Local GitHub API | Authenticated as `Ezequiel24123z`; repository push/admin permissions verified | Use for repository API/publication where needed. |
| PC clone commit identity | Existing repository-local author identity | Inspect, do not replace globally. |
| Coordination worktree commit identity | No configured author at intake | Use command-scoped identity for checkpoint commits, not a persistent/global migration. |

Never print credential helpers' stored values, environment dumps, launcher accounts or
Minecraft process arguments. Store addresses and paths in a private host configuration,
not this repository. No GitHub credential is required on the SSH command line.

The coordination machine's connection inventory is
`%USERPROFILE%\.config\grindless\pc.json`, outside Git. Its schema is
`grindless.pc-workflow`, version 1; fields are `sshTarget`, `clonePath`, `worktreePath`,
`gradleUserHome`, `prismRoot`, `templateInstanceId`, `instanceId`, `javaHome` and
`javawSha256` (the inspected Java 17 `javaw.exe`, not a credential).
The other required fields are `personalGitSshAlias`, `receivedSourceRevision` and
`sourceBranch`, recording the existing personal Git alias and received source/branch;
none of these settings contains a key or token. A successor reads this file rather
than rediscovering or publishing personal addresses and paths. Do not replace it
or the SSH configuration merely because a new session starts.

## Preserve the existing player environment

At intake the existing Grindless preparation instance has Minecraft 1.20.1 / Forge 47.4.23,
Architectury and JEI, but its Grindless jar is the old `b982b10` runtime
(SHA-256 `79b5a02c0b1ed262e5439f1d66f6c20aebd5a8c9406bbcb8afc9421bd00ca376`).
It also contains a real exploratory save. It lacks the later Heracles/ResourcefulLib
and selected performance dependencies. Updating that instance in place would mix old
preparation and current engineering evidence and unnecessarily risk the save.

Use a new `Grindless-T0-Dev` instance, clearly named as engineering, with a deployment
ownership marker. Reuse only its predecessor's launcher component metadata and
hash-verified official dependency jars, never its saves, personal options or configuration.
The installed system Java 17 is usable; do not trust the launcher's stale global Java
path, and do not change that global setting to fix the new instance.

The opt-in `playable` profile is seven pinned dependencies plus Grindless. Its provenance
must still say `restricted-playable-profile-not-rc`. Upstream defaults and an explicitly
empty public-override selection are intentional, not an official Superflat preset.

### Why not automate Prism's Import dialog over SSH?

The official [CLI reference](https://prismlauncher.org/wiki/getting-started/command-line-interface/)
separates `--import` from `--launch`, but import still opens the GUI's modal creation
dialog and can require publisher-specific manual downloads. It is not a headless
installation command and must not unexpectedly activate the player's desktop.

Prism's inspected source at `804e2a02e0ef299cac33fc7ec3ff8a43422afb4c` supports a simpler
file-layout lane: [instance discovery](https://github.com/PrismLauncher/PrismLauncher/blob/804e2a02e0ef299cac33fc7ec3ff8a43422afb4c/launcher/InstanceList.cpp)
finds folders containing `instance.cfg`; the standard type is `OneSix`.
[Component serialization](https://github.com/PrismLauncher/PrismLauncher/blob/804e2a02e0ef299cac33fc7ec3ff8a43422afb4c/launcher/minecraft/PackProfile.cpp)
uses `mmc-pack.json` format 1 and `net.minecraft` / `net.minecraftforge` components.
[The game directory](https://github.com/PrismLauncher/PrismLauncher/blob/804e2a02e0ef299cac33fc7ec3ff8a43422afb4c/launcher/minecraft/MinecraftInstance.cpp)
is `minecraft/` (or an already existing `.minecraft/`), with jars discovered under `mods/`.
Routine Grindless updates do not require component/patch edits.

This is static upstream evidence, not an exact-tag diff or observed new client launch.
The actual installed template already supplies a working compatible component set.
Create a fresh unmanaged, ownership-marked instance from sanitized metadata only;
never invoke import, clone a private instance or replace its launcher-managed files.

## Repeatable execution contract

Keep one detached execution worktree and a process-local Gradle home on the work volume.
Resolve the personal target, clone, execution worktree, Gradle home, Prism root, template
instance and JDK from private per-host settings. Check the received SHA and cleanliness
before every build; refuse an unexpected revision or dirty worktree instead of resetting it.

1. Commit and push the coherent local source checkpoint to this session's branch.
2. Fetch through the PC clone's existing personal Git SSH origin. Materialize or advance
   only the isolated execution worktree to that exact commit; do not switch either original clone.
3. Build the actual remapped jar and `playable` ZIP with the existing Gradle pack task.
   Set `JAVA_HOME` and `GRADLE_USER_HOME` only for that process and use bounded, serialized workers.
4. Verify archive/runtime provenance and jar hashes. Prepare or update only the owned,
   stopped development instance, downloading missing official pins without redistributing them.
5. Leave Prism and Minecraft launch to the player. Record the instance, deployed commit
   and hash with every finding. Stop Minecraft before the next deployment.

Build and deployment are separate boundaries. While the owner plays the already
deployed jar, source synchronization and a capacity-guarded **Build-only** action may
use the separate execution worktree; neither writes the running instance. Serialize
agent builds and refuse unknown/insufficient physical memory, commit or work-disk
headroom. Do not switch source underneath a running Gradle task. **Deploy** waits
for the player to stop that instance, verifies ownership, and only then replaces its
managed jar. The combined **BuildAndDeploy** action is the stopped-instance convenience
path, not a reason to kill the player's game or prohibit unrelated source work.

### Commands from the local coordination worktree

The PowerShell and Bash entrypoints share the same implementation. The local inventory
is already configured on the coordination machine; there is no SSH or Git login step:

```powershell
$revision = git rev-parse HEAD
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\pc-workflow.ps1 -Action Sync -Revision $revision
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\pc-workflow.ps1 -Action Build -Revision $revision
```

Build prints the exact remote `PackZip`, source/runtime revision and hashes. While it
runs, the player's previous jar/world is untouched. Once the player stops Minecraft:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\pc-workflow.ps1 -Action Deploy -Revision $revision -PackZip '<absolute-PC-ZIP-from-Build>'
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\pc-workflow.ps1 -Action Inspect -Revision $revision
```

For initial setup or any other stopped-instance iteration, replace the separate Build/
Deploy pair with `-Action BuildAndDeploy`. On a Bash coordination shell, use
`bash tools/pc-workflow.sh -Action Sync -Revision <full-SHA>` and the same parameters
for the other actions. PowerShell must be available on the coordinator; the execution
host and native deployment contract are Windows. A fixed digest-bound bootstrap and
JSON stdin carry settings, not interpolated shell paths or long command-line payloads.

The current3GiB Gradle heap requires at least6GiB free physical memory,8GiB free commit
and8GiB on each distinct work/cache volume. These are fresh build preconditions, not
a global machine change or permission to stop unrelated programs. Build/worktree/cache
locks prevent overlapping agent execution and source switches. Unknown/stale locks
are preserved and reported, not automatically removed.

The deployment tool accepts the current explicitly empty public overrides and refuses
nonempty overrides before mutation. Pin or Java migrations require a new isolated
instance under this bounded tool; they are not silent modifications of an existing save.
Do not bypass a refusal by deleting markers, mods, locks or player configuration.

SSH commands must use `BatchMode=yes`, `StrictHostKeyChecking=yes` and a finite connection
timeout. Windows' remote command shell has an approximately 8191-character command-line
limit: do not place long base64 scripts on it. Prefer the committed entrypoints; for
bounded inspection, use short encoded PowerShell scripts or a fixed script over stdin.
Never work around SSH failures by binding this conversation to Remote Host.

## Playtest scope and remaining release gates

This development deployment enables useful client feedback while unrelated development
continues. First inspect startup, native item models, complete JEI/Atlas details, the
Heracles book and the approved Quern loop. Use a separate diagnostic world when commands
are necessary; do not label it no-command survival or adopt an old exploratory save as
the longitudinal reference. Preserve existing saves and report the exact changed surface.

The official Superflat acquisition/preset contract is still incomplete. Virtual deposits
are not proof that a bare flat spawn can obtain every finite bootstrap input; no staged
pond/tree/quarry/food terrain was authorized. Do not quietly invent a world preset,
quest-shop economy or new generator to disguise this gap. Normal Overworld diagnostics
are not a replacement for the Superflat release requirement.

T0 RC publication, actual client/visual/listening observations and authentic forward-save
qualification remain separate. T1 stays held under ADR-0144. See
[the player checklist](../testing/t0-survival-acceptance.md).

## Reuse existing native proof before another server run

Exact-source [CI39185959767](https://github.com/Ezequiel24123z/grindless-earlygame/actions/runs/39185959767)
at70901 completed successfully, including verify and all six native smoke shards.
The inspected shard1 checks out that SHA and proves eight production-server boots/
scenario passes. This resolves the previously unrecorded sound-registration/server
binding without repeating a server on the PC merely because execution moved.
Hosted startup alone does not establish PC build/package/deployment, native client
audio, visual quality, human play or a release-ready flat route.

The isolated PC build/package has subsequently passed on frozen70901: full build,
88 pack contracts,43 behavior suites and23 actual archive/resource checks, with zero
remaining JVMs. Its Forge jar SHA-256 is
`5431d6ad410abc326893e68bf3e2b763b4e868410858ec1e370fc680427899f7`.
See [the source-bound receipt](../testing/latest-validation.md#personal-pc-qualification-of-frozen70901).
Deployment/client observations remain separate; do not relabel this initial70901 archive
as a later tool-source package or final RC.

## Tool contract evidence

The initial frozen tooling passes317 assertions across31 isolated Windows PowerShell5.1 fixtures:
create/idempotence/update/rollback, owner/running/reparse/tamper/pin/provenance refusals,
the actual installed template's `cachedVolatile` shape, `.index` preservation, out-of-scan
staging, resource-gated Build while an attributable player game remains untouched,
and fixed stdin transport/exact detached Sync. All45 native helpers and the sole entrypoint
are top-level in the parsed AST. Fixtures use synthetic files and substituted OS/Git/
download/transport boundaries; they launch no JVM, SSH, network or real launcher.

The independent `prism-tooling-windows` CI job runs the direct WinPS5 entry and its
Git Bash twin. Actual real-PC transport/CDN/installed-instance results must be recorded
separately in [the validation ledger](../testing/latest-validation.md); the fixture
count alone is not that integration proof or native client acceptance.

The actual first Sync exposed WinPS `CLIXML` progress; the follow-up selects text output
and suppresses progress without hiding errors. The current322-assertion contract also
distinguishes `Prepare`'s missing-source pins from `DownloadedDependencies` after success;
`MissingLocalDependencies` is empty for a completed deployment. The initial0b29 hosted
Windows job passed both direct and Bash steps; later-source CI has its own identity.

A later repeated real RPC exposed an EOF wait after successful build/deploy/inspect.
The current330-assertion transport sends one compact newline-terminated JSON packet
and reads one line, with a256KiB UTF-8 bound, rather than waiting for SSH EOF.
Fifteen-second/three-miss SSH liveness checks bound dead connections, not legitimate
long builds. Preserve the installed instance and diagnose only the owned RPC helper;
do not retry a possibly partial operation blindly or kill unrelated PowerShell/Java.
