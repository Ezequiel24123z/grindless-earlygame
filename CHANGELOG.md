# Changelog

Every change to code, build configuration or documentation is recorded here, newest first.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project will
follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) from its first release.

This file is a handoff document as much as a history. Sessions working on Grindless are
short-lived and frequently lost, so `[Unreleased]` is the first place a new session looks to find
out what the previous one actually did. Keep it current — see [`AGENTS.md`](AGENTS.md).

Design rationale is *not* recorded here. It goes in [`docs/DECISIONS.md`](docs/DECISIONS.md), and
entries below reference those records by id.

## [Unreleased]

### Added

- **Working agreement** (`AGENTS.md`). The contract for every session on this project: commit at
  every checkpoint, record decisions in the repository, prefer file edits and batched git over
  shell commands, and reply in Spanish while writing English in the repository. Also documents the
  repository layout, commit conventions, and how to push when git has no credentials.
- **Architecture decision records** (`docs/DECISIONS.md`). Fourteen records covering the choices
  made so far, from Architectury multiloader (ADR-0001) through to the branch consolidation
  (ADR-0014), each with the alternatives that were rejected.
- **This changelog.**
- **Project foundation**, recovered from two assistant sessions that lost their terminals before
  committing anything (ADR-0014):
  - `README.md` — the full design and reference document: the eight systems, Flux energy, the
    processing chain, research progression, the orbital and planetary layers, the compatibility
    strategy, the verified toolchain and the roadmap.
  - `docs/DESIGN.md` — the earlier standalone design note, kept for history and superseded by the
    README (ADR-0013).
  - Architectury multiloader build — root `build.gradle`, `settings.gradle`, a `gradle.properties`
    holding every version coordinate, and the `common`, `fabric` and `forge` subproject scripts.
    No `neoforge` subproject on 1.20.1; the Forge jar serves both loaders (ADR-0002).
  - `common/.../Grindless.java` — the loader-agnostic entrypoint.
  - `SETUP.ps1` — Windows bootstrap: `MAX_PATH` check, source tree creation, JDK and Gradle
    discovery, Gradle wrapper generation, and `-Commit` / `-MakePrivate` switches.
  - `.gitignore` covering Gradle, Loom/Architectury and IDE output.

### Changed

- `README.md`: the *Current blocker* section became *Environment setup*. It described a broken
  development terminal as the project's blocker and linked a workaround script that was
  deliberately not committed (ADR-0014). The architecture tree and the implementation plan were
  updated to match what is now in the repository.
- `gradle.properties`: added `supported_minecraft_versions` and `neoforge_loader_range`, and
  standardised the Architectury API version on the single key `architectury_api_version`, which
  the subproject scripts referenced under a second name (ADR-0014).
- `build.gradle`: the root build now bundles `LICENSE` into every jar.
- `SETUP.ps1`: removed header comments asserting that the work in the tree was uncommitted, which
  ceased to be true once it was.
- `docs/DESIGN.md`: marked superseded by the README (ADR-0013), and repointed its two dangling
  links — `ENERGY.md` and `ROADMAP.md` were planned splits that were never written — at the README
  sections that hold that content today.

### Not included

- `COMMIT.bat` and `README.new.md` from the recovered sessions. Both existed only to work around
  the broken terminal, and the latter was marked a stale scratch file by its own author
  (ADR-0014).

### Known state

The tree **does not compile**. `Grindless.java` calls into `GrindlessConfig`, `ModBlocks`,
`ModItems`, `ModBlockEntities`, `ModMenus`, `ModCreativeTabs` and `MaterialRegistry`, none of which
have been written. There is no Gradle wrapper yet either — `SETUP.ps1` generates it.

The next milestone is step 7 of the README's implementation plan: a minimal, *compiling*
multiloader skeleton that builds green on both Fabric and Forge. That requires the loader metadata
files (`fabric.mod.json`, `META-INF/mods.toml`, `pack.mcmeta`, lang) and the core registry layer
built on Architectury's `DeferredRegister`.
