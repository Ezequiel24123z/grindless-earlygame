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
then `docs/DECISIONS.md` (what is already settled), then the README's *Implementation plan* (what
is next). That is the full handoff; nothing else is needed.

**Finishing, or when the terminal starts failing.** Update the changelog, add any ADRs, update the
implementation plan, then commit and push. If the terminal is already broken, say so plainly and
tell the user to run `SETUP.ps1 -Commit`, which commits and pushes without the app.

---

## Repository map

```
AGENTS.md            this file — the working agreement
README.md            game design + architecture + toolchain; the design source of truth
CHANGELOG.md         what changed, in order
docs/DECISIONS.md    why it changed — architecture decision records
docs/DESIGN.md       early standalone design note; superseded by README, kept for history
SETUP.ps1            Windows bootstrap; also commits and pushes via -Commit

build.gradle         root Gradle config shared by all subprojects
settings.gradle      includes common, fabric, forge
gradle.properties    every version coordinate — the only place versions are written
common/              loader-agnostic code; ~95% of the codebase belongs here
fabric/              Fabric entrypoint and bridges only
forge/               Forge entrypoint and bridges only (this jar also loads on NeoForge 1.20.1)
```

---

## Conventions

**Versions.** Every version lives in `gradle.properties` and is referenced as
`rootProject.<name>`. Never hardcode a version in a build script.

**Platform code.** `common/` must not reference loader-specific classes. Anything that differs
between Fabric and Forge goes through an Architectury `@ExpectPlatform` stub.

**Commit messages.** A short imperative subject, then a body explaining *why*. Reference ADRs by
id (`ADR-0007`) when a commit implements one. Include the trailer:

```
Co-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>
```

**Never commit** build output (`build/`, `.gradle/`, `run/`), IDE files, or scratch and workaround
files. `.gitignore` covers the first two; the third is a judgement call — if a file exists only to
work around a broken tool, it does not belong in history.

---

## Troubleshooting: `git push` fails

If push fails with `fatal: Cannot prompt because user interactivity has been disabled`, git has no
credentials. When a `GH_TOKEN` environment variable is present, push with a transient auth header
so that no credential is ever written to disk or to the repository:

```powershell
$b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("x-access-token:$env:GH_TOKEN"))
git -c http.extraheader="AUTHORIZATION: basic $b64" push -u origin HEAD
```

Never write a token into `.git/config`, a remote URL, or any tracked file.
