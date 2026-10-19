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
| PC -> GitHub Git SSH | Existing personal alias reads the exact continuation head | Keep its origin and key configuration unchanged. |
| PC `gh` API login | Stored personal token is invalid | Not a Git transport failure; no account switch or reauthentication is needed for this loop. |
| Local GitHub API | Authenticated as `Ezequiel24123z` | Use for repository API/publication where needed. |
| PC clone commit identity | Existing repository-local author identity | Inspect, do not replace globally. |
| Coordination worktree commit identity | No configured author at intake | Use command-scoped identity for checkpoint commits, not a persistent/global migration. |

Never print credential helpers' stored values, environment dumps, launcher accounts or
Minecraft process arguments. Store addresses and paths in a private host configuration,
not this repository. No GitHub credential is required on the SSH command line.

The coordination machine's connection inventory is
`%USERPROFILE%\.config\grindless\pc.json`, outside Git. Its schema is
`grindless.pc-workflow`, version 1; fields are `sshTarget`, `clonePath`, `worktreePath`,
`gradleUserHome`, `prismRoot`, `templateInstanceId`, `instanceId` and `javaHome`.
The inventory also records the existing personal Git alias and received source/branch;
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
The PC still needs its actual isolated build/package/deployment proof. Hosted startup
does not prove native client audio, visual quality, human play or a release-ready flat route.
