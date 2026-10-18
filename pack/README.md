# Grindless T0 - client packs and reference preparation

This directory defines a reproducible **client pack** for Minecraft 1.20.1 / Forge 47.4.23.
`tools/build-modpack.ps1` (or `.sh`) builds the local Grindless jar, adds it as an override, and
creates an importable CurseForge-format zip under `dist/`. Nothing under `dist/` is source control.
Prism Launcher can import this format using **Add Instance > Import**. The archive includes
this guide, the production-remapped Grindless jar, the MIT licence and `provenance.json`
with source/runtime revisions, jar SHA-256, dependency file IDs and published hashes.
Minecraft, Forge, Java and third-party jars are not redistributed in the ZIP: the launcher
downloads the official pinned components. Internet access and a legitimate Minecraft account
are required. Never include account files or personal launcher configuration in an export.

## Two profiles, one set of dependency pins

The default **expanded** pack below keeps its existing convenience/performance role.
The **preparation** profile contains only Grindless, its required Architectury API, and JEI
for recipe-viewer observations. It deliberately excludes the expanded extras: these are not
Grindless requirements or prerequisites for the standalone S2 evidence.

Both profiles select pins from `curseforge/manifest.json`; `profiles.json` selects project
IDs, not alternative versions. `dependency-metadata.json` records independently checked
file metadata and required-project edges, and builders reject a stale/missing record or a
Minecraft/Forge/Architectury/JEI mismatch against `gradle.properties`.

## Included integrations

| Mod | Purpose in this build | Progression policy |
| --- | --- | --- |
| Architectury API | Required Grindless platform runtime. | Required in both profiles. |
| JEI | Shows vanilla data recipes plus Grindless's generated machine graph and the Research Terminal calibration. | Available immediately; it reveals recipes, not permissions. |
| Jade | In-world machine and block information. | Available immediately. |
| Sophisticated Backpacks | Portable, upgradable inventory. | Available immediately. |
| FTB Essentials | `/home`, `/sethome`, `/back`, and practical single-player/server utilities. | Available immediately. |
| FTB Chunks + Teams + Library | Claims, map and team ownership. | Available immediately. |
| ModernFix, FerriteCore, Embeddium | Memory and client-performance baseline. | Available immediately; Embeddium is client-side. |

## Deliberately not in T0: Applied Energistics 2

AE2 is an intended **T4 Precision** integration, where storage cells, patterns and autocrafting
fit Grindless's first precision/electronics factory. Adding unmodified AE2 to this Bootstrap pack
would expose its native ores, machines and recipes before that frontier and would therefore make a
T0 playthrough non-authoritative. The future AE2 slice must ship its recipe replacement, material
bridges, energy behaviour and a complete survival proof in the same release. Until then this pack
does not claim AE2 compatibility or include the mod as a false gate.

## Build locally

Use a clean, committed source tree and Java 17:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\build-modpack.ps1
powershell -ExecutionPolicy Bypass -File .\tools\build-modpack.ps1 -Profile preparation -RuntimeRevision <authorized-runtime-commit>
```

On Linux/macOS, use `tools/build-modpack.sh .` or
`tools/build-modpack.sh . preparation <authorized-runtime-commit>`. Both scripts are thin
wrappers around the same Gradle `buildModpack` task: Groovy parses the root manifest version,
selects dependencies and writes provenance; Ant creates the ZIP. No extra JSON parser,
Python installation or external `zip` command is required.
Run `gradlew verifyModpack` to check both selectors and platform/dependency rejection cases
without starting a game or generating a world.

`RuntimeRevision` defaults to the package source revision. An explicit revision must be an
ancestor with unchanged `common/`, `forge/` and root build inputs. This separates a packaging
checkpoint from its authorized gameplay ancestor without calling an old binary a new candidate.
The preparation filename includes `-preparation-<package-source-short-sha>`.

## Import and preparation on the real client

1. In Prism, choose **Add Instance > Import**, select the ZIP and create a **new isolated
   instance**. Do not overwrite a personal profile or saved world. Let the launcher fetch
   the exact dependencies; if a publisher requires a manual download, use the original
   publisher page without substituting a different file. CurseForge import is also supported.
2. Select a working 64-bit **Java 17** for this instance. Preserve the pinned Minecraft and
   Forge versions and the supplied mod set. No custom gameplay configuration is needed:
   this package uses mod defaults, not personal settings or diagnostic overrides.
3. Launch to the title screen. Confirm Minecraft/Forge versions and that Mods lists
   Grindless, Architectury and JEI. Report any startup error, or recipe-viewer text that is
   missing/truncated, with the GUI scale. This is client preparation, not progression evidence.
4. Only once the coordinator authorizes the candidate/route, create a **new ordinary
   Overworld Survival** world, cheats **OFF**, bonus chest **OFF**, default generation and
   structures enabled. Record its agreed creation settings. No save is bundled: no world
   has been generated or played as part of this package, and no smoke/diagnostic save is reused.

The current reference preparation is **not post-S2 and not T0 accepted**. S2, canonical
source/budget/quest reconciliation, the final guide and the physical exit remain pending.
Do not treat the optional two-iron Dynamo/Extractor as a guaranteed entry allocation.
The eventual unique run uses only actual world supplies, no commands/creative/injection,
and observes one physical craft of the first **five F1 Relay Matrices** plus the intended
T1 handoff, stopping before T1 machinery. No final route is invented here.
Authentic retained-world compatibility is a separate, provenance-known copy plus a full
save/exit/process-restart/reopen; a fresh world or serializer test cannot substitute for it.

## Dependency provenance

The manifest makes the launcher download each upstream file; it does not vendor their jars in this
repository. Exact CurseForge project/file identifiers are pinned in `curseforge/manifest.json`.
Their licences remain theirs: Architectury API (LGPL-3.0-only), JEI (MIT), Jade (LGPL-3.0), Sophisticated Backpacks and Core (ARR),
FTB Essentials/Chunks/Teams/Library (ARR), ModernFix (LGPL-3.0), FerriteCore (MIT), and Embeddium
(LGPL-3.0). See the upstream project pages before redistributing a modified dependency.
