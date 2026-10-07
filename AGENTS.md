# Working agreement

This file is the contract for anyone — human or AI assistant — working on Grindless.
Read it before doing anything else.

## Why this file exists

This project is built across many short-lived assistant sessions. Sessions are routinely lost:
the in-app terminal breaks, a session degrades, or work simply moves to a new branch. Two full
sessions were already lost this way, and the work in them survived only because it was recovered
by hand from abandoned worktrees.

The conclusion is the operating rule of this repository:

> **Git is the only memory. Anything not committed does not exist.**

A new session starts with no knowledge of previous sessions. It sees the repository and nothing
else. Everything a successor needs — what was decided, why, what changed, what is next — has to be
written down *in the repository*, not left in a chat log.

---

## The four rules

### 1. Commit at every meaningful checkpoint

Do not batch a whole session's work into one commit at the end. The end may never arrive.

Commit as soon as a unit of work is coherent, even if incomplete. A commit that says
"registry layer, does not compile yet" is infinitely more valuable than a perfect uncommitted
working tree in a session that dies.

Never end a turn with uncommitted work in the tree.

### 2. Document every decision, in the repository

| What changed | Where it is recorded |
| --- | --- |
| A design, architecture or dependency decision | [`docs/DECISIONS.md`](docs/DECISIONS.md) — add an ADR |
| Any change to code, build or docs | [`CHANGELOG.md`](CHANGELOG.md) — add a bullet under `[Unreleased]` |
| Progress through the build-out | [`README.md`](README.md) — update the *Implementation plan* table |
| Game design itself | [`README.md`](README.md) — it is the design document |

A decision is worth an ADR when a successor could reasonably wonder *"why was it done this way?"*
and get it wrong by guessing. Rejected alternatives matter as much as the choice: they stop the
next session from re-litigating a settled question.

Write the ADR *when the decision is made*, not later. Reconstructing rationale after the fact is
exactly what fails when a session is lost.

### 3. Prefer git and file edits over shell commands

Every shell command costs the user a manual approval, and the terminal is the component that keeps
breaking. Therefore:

- Edit files with file tools. Never shell out to write or move a file.
- Batch git into **one** terminal call per checkpoint — stage, commit and push together.
- Inspect the repository with search and file reads, not with `git grep`, `dir` or `cat`.
- Reserve the shell for what genuinely requires it: git, Gradle builds, `SETUP.ps1`.

Do not run exploratory shell commands to satisfy curiosity. If a fact is not needed to make the
next change correct, do not spend an approval on it.

### 4. Language

The user communicates in **Spanish** — reply in Spanish.

Everything committed to the repository is in **English**: code, identifiers, comments,
documentation, ADRs, changelog entries and commit messages. No exceptions, so the project stays
legible to the wider Minecraft modding community.

---

## Session checklist

**Starting.** Read this file, then `CHANGELOG.md` (`[Unreleased]` — what the last session did),
then `docs/BUILD-OUT.md` (the next slice), then `docs/DECISIONS.md` (what is already settled),
then the README's *Implementation plan*. That is the full handoff; nothing else is needed. Do
not ask what to do next: the build-out already says.

When the slice needs code, open [`docs/AGENT-MAP.md`](docs/AGENT-MAP.md). It names the
files and the invariant for each kind of change. Do not paste that map into this file.
The same commit that changes a seam updates the map, the same way it updates the changelog.

**Finishing, or when the terminal starts failing.** Update the changelog, add any ADRs, update the
implementation plan, validate the documentation, then commit and push:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\check-links.ps1 -Root .
powershell -ExecutionPolicy Bypass -File .\tools\run-checks.ps1 -Root .
```

On Linux and macOS use `tools/run-checks.sh .` instead of the second one. Neither runs Forge, so
also boot the server after any change to registration, block entities or loader code
(ADR-0049): a build and 309 passing checks coexisted with a mod that could not start.

```powershell
$env:GRINDLESS_ACCEPT_EULA='true'
powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -Only states
powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root .   # all 39, tens of minutes
```

`tools/run-smokes.sh .` is the same thing elsewhere. The scenario list is
[`tools/smoke/scenarios.txt`](tools/smoke/scenarios.txt); add a line there, never a step in
`ci.yml` (ADR-0105). `-Only states` is the cheapest single check that every block still
registers and places.

All of them are worth the approval they cost. The link checker has caught broken links and a
drifted ADR index more than once; the behaviour checks have caught real bugs that compiled
cleanly; the smokes are the only thing that catches a mod which builds and cannot start. If the
terminal is already broken, say so plainly and tell the user to run `SETUP.ps1 -Commit`, which
commits and pushes without the app.

### Where the work happens

Slices are written away from this machine, in Cursor, as stacked pull requests. This Windows
desk is where they are built, smoked, fixed and merged (ADR-0106). So:

- **A branch is not finished when CI is green.** It is finished when it has been through here.
  CI reports a timed-out job as *cancelled*, which is not a pass.
- **A tool that fails only on Windows is a bug in the tool.** Fix it there. Three such bugs had
  made the sequence above unusable on the one machine that validates.
- **Every `tools/*.sh` needs its `.ps1` twin**, and vice versa. A step that only runs on Linux
  cannot be validated before the merge.

---

## Repository map

```
AGENTS.md            this file — the working agreement
README.md            game design + architecture + toolchain; the design source of truth
CHANGELOG.md         what changed, in order
docs/DECISIONS.md    why it changed — architecture decision records
docs/MACHINES.md     the machine layer: conditions, envelopes, chassis marks, multiblocks
docs/PROCESSES.md    the content layer: items, fluids, recipe graph, routes and ratios
docs/BUILD-OUT.md    remaining slices in ship order; a session implements the Next line
docs/AGENT-MAP.md    task index of files and invariants; updated with the seam
docs/DESIGN.md       early standalone design note; superseded by README, kept for history
SETUP.ps1            Windows bootstrap; also commits and pushes via -Commit
tools/               repository scripts, each with a .ps1 and a .sh twin (ADR-0106):
                     check-links  validates the docs before a commit
                     run-checks   runs the behaviour checks in tools/checks/
                     run-smokes   boots the scenarios in tools/smoke/scenarios.txt
                     smoke-boot   one scenario; the .ps1 is a real implementation, not a wrapper

build.gradle         root Gradle config shared by all subprojects
settings.gradle      includes common, forge
gradle.properties    every version coordinate — the only place versions are written
common/              loader-agnostic code; ~95% of the codebase belongs here
forge/               Forge entrypoint and bridges only (this jar also loads on NeoForge 1.20.1)
```

---

## Conventions

**Versions.** Every version lives in `gradle.properties` and is referenced as
`rootProject.<name>`. Never hardcode a version in a build script.

**Platform code.** Forge 1.20.1 is the only build target (ADR-0039), but `common/` must still not
reference loader-specific classes: anything touching Forge goes through an Architectury
`@ExpectPlatform` stub, with the implementation in `forge/`. Keeping that discipline for a single
loader is what makes a future port a build change rather than a rewrite.

**Commit messages.** A short imperative subject, then a body explaining *why*. Reference ADRs by
id (`ADR-0007`) when a commit implements one. Include the trailer:

```
Co-authored-by: Ezequiel Castaño <ezeycema@gmail.com>
```

**Never commit** build output (`build/`, `.gradle/`, `run/`), IDE files, or scratch and workaround
files. `.gitignore` covers the first two; the third is a judgement call — if a file exists only to
work around a broken tool, it does not belong in history.

**Third-party material.** Code, textures, sounds and other assets from other projects may be
reused or adapted when their licence permits the intended distribution. Before copying anything,
record its exact source, version or commit, licence, scope and required notices in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md), and keep the required attribution and licence
text with the distribution. Do not assume that the repository's MIT licence grants rights to
somebody else's work; if a source requires a different compatible distribution licence, record
that consequence before importing it.

---

## Troubleshooting: the terminal refuses every command

If every shell command fails immediately with **"Permission denied and could not request
permission from user"**, the session is running in the app's **Automatic** work mode, which cannot
prompt for command approval. Nothing is wrong with the repository, the shell or the environment.

**The fix is to switch the session to Interactive mode.** This cost four sessions before the cause
was identified, each of which was abandoned believing the terminal had broken. It had not.

While it is happening, the file tools still work — reading, writing and editing are all fine. Only
commands are blocked, which means no builds and no commits. If that happens mid-session, keep
working with files, then ask the user to run the build and `SETUP.ps1 -Commit -Message "..."`.

## Troubleshooting: `git push` fails

If push fails with `fatal: Cannot prompt because user interactivity has been disabled`, git has no
credentials. When a `GH_TOKEN` environment variable is present, push with a transient auth header
so that no credential is ever written to disk or to the repository:

```powershell
$b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("x-access-token:$env:GH_TOKEN"))
git -c http.extraheader="AUTHORIZATION: basic $b64" push -u origin HEAD
```

Never write a token into `.git/config`, a remote URL, or any tracked file.
