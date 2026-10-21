# Grindless T0 - client packs and reference preparation

This directory defines a reproducible **client pack** for Minecraft 1.20.1 / Forge 47.4.23.
`tools/build-modpack.ps1` (or `.sh`) builds the local Grindless jar, adds it as an override, and
creates an importable CurseForge-format zip under `dist/`. Nothing under `dist/` is source control.
Prism Launcher can import this format using **Add Instance > Import**. The archive includes
this guide, the production-remapped Grindless jar, the MIT licence,
`THIRD_PARTY_NOTICES.md` and `provenance.json` with source/runtime revisions, jar SHA-256,
dependency file IDs and published hashes. The new playable selector is a restricted
composition preview, **not an accepted or published Release Candidate**.
Minecraft, Forge, Java and third-party jars are not redistributed in the ZIP: the launcher
downloads the official pinned components. Internet access and a legitimate Minecraft account
are required. Never include account files or personal launcher configuration in an export.

For the owner's SSH-first personal-PC development loop, use
[the operating runbook](../docs/operations/ssh-prism-workflow.md) and the dedicated
ownership-marked engineering instance. Do not update the old preparation instance,
copy its world, or invoke Prism's interactive Import dialog from SSH. Normal public
ZIP import below remains available; development deployment is not an RC or save migration.

## Three profiles, one set of dependency pins

The default **expanded** pack keeps its convenience/performance role and archive
filename. Selecting nothing still means expanded. ADR-0171 replaces the quest
backend in every profile; old archives retain their original pins and provenance.
The **preparation** profile contains Grindless, its required Architectury API, JEI for
recipe-viewer observations, and native FTB Quests with FTB Library, FTB Teams and
FTB XMod Compat. XMod Compat supplies the actual FTB-to-JEI recipe helper; the
quest core alone has a no-op helper.
It deliberately excludes the expanded convenience extras: these are not Grindless
requirements or prerequisites for the standalone S2 evidence. The quest integration is
optional for Grindless itself; all three pack profiles include the selected backend.

The opt-in **playable** profile implements
[ADR-0149](../docs/DECISIONS.md#adr-0149--add-a-restricted-playable-profile-with-explicit-public-overrides):
the preparation dependencies plus ModernFix, FerriteCore and Embeddium.
It omits Jade, Backpacks/Core and FTB Essentials/Chunks. Library and Teams now
support the native quest backend in every profile, rather than only the expanded
map integration. Those omissions are intentional scope/licence/progression choices,
not failed interoperability findings or permission to remove mods from an existing save.
No existing instance is silently updated to this different dependency set.

All profiles select pins from `curseforge/manifest.json`; `profiles.json` selects project
IDs, not alternative versions. `dependency-metadata.json` records independently checked
file metadata and required-project edges, and builders reject a stale/missing record or a
Minecraft/Forge/Architectury/JEI/quest-backend mismatch against `gradle.properties`.

## Included integrations

| Mod | Purpose in this build | Progression policy |
| --- | --- | --- |
| Architectury API | Required Grindless platform runtime. | Required in all profiles. |
| JEI | Shows vanilla data recipes plus Grindless's generated machine graph and the Research Terminal calibration. | Available immediately; it reveals recipes, not permissions. |
| FTB Quests + Library + Teams + XMod Compat | Native quest graph, screens, tasks, item rewards, team progress and the actual JEI recipe helper. | Rewards have individual UUID receipts and no autoclaim. No held Grindless book, custom quest widget or custom recipe panel is required. |
| Jade | In-world machine and block information. | Available immediately. |
| Sophisticated Backpacks | Portable, upgradable inventory. | Available immediately. |
| FTB Essentials | `/home`, `/sethome`, `/back`, and practical single-player/server utilities. | Available immediately. |
| FTB Chunks + Teams + Library | Claims, map and team ownership. | Available immediately. |
| ModernFix, FerriteCore, Embeddium | Memory and client-performance baseline. | Available immediately; Embeddium is client-side. |

Jade, Backpacks/Core, FTB Essentials and FTB Chunks belong only to expanded.
The three performance dependencies belong to expanded and playable, not preparation.
The first three rows belong to every profile. No file ID, version or hash is duplicated
in the new selector.

## Explicit public overrides, not a launcher export

`public-overrides.json` is the playable profile's only public-configuration selector.
Its `files` list is **explicitly empty** for the current upstream-default composition.
No guessed config keys, personal options or content are added just to fill a directory.
This does not supply the pending official Superflat preset, full product composition,
questbook product review or actual installation/client qualification.

Each future reviewed entry names a `source` relative to the committed
`public-overrides/` source root and a public `destination` below the instance's
`config/` or `defaultconfigs/`. See [the source contract](public-overrides/README.md).
The builder copies only those individual files; unlisted files, including that guide,
never enter the archive. It rejects malformed profile/override data, non-portable,
absolute or traversal paths, escaping symlinks, duplicate/conflicting destinations and
missing/untracked inputs. Symlink targets must also be tracked under the source root.
Additional public content roots require a reviewed whitelist change. Never copy a
launcher, development config, home, cache, logs or world directory recursively.

Provenance records the exact input-profile object, the single pin authority, upstream
default policy and every included override's source, destination and SHA-256. All other
settings remain upstream-generated defaults, not files silently copied from this desk.
The complete archive file whitelist is `manifest.json`, `provenance.json`, this
`README.md`, `LICENSE`, `THIRD_PARTY_NOTICES.md`, the one remapped Grindless jar under
`overrides/mods/`, and those explicitly selected public files under `overrides/`.
The builder verifies that whitelist and the exact file hashes before copying a ZIP to
`dist/`. No third-party jars or saved worlds are included.

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
powershell -ExecutionPolicy Bypass -File .\tools\build-modpack.ps1 -Profile playable
```

On Linux/macOS, use `tools/build-modpack.sh .` or
`tools/build-modpack.sh . preparation <authorized-runtime-commit>`, or
`tools/build-modpack.sh . playable`. Both scripts are thin
wrappers around the same Gradle `buildModpack` task: Groovy parses the root manifest version,
selects dependencies and writes provenance; Ant creates the ZIP. No extra JSON parser,
Python installation or external `zip` command is required.
Run `gradlew verifyModpack` to check all selectors, historical preservation,
platform/dependency rejections, public override guards and the actual archive-writing
contract with synthetic fixtures. This task does not build a runtime, start a game or
generate a world. A host unable to create a filesystem symlink reports that fixture as
`NOT_RUN`; its canonical-target confinement check is separate, not a filesystem pass.

`RuntimeRevision` defaults to the package source revision. An explicit revision must be an
ancestor with unchanged `common/`, `forge/` and root build inputs. This separates a packaging
checkpoint from its authorized gameplay ancestor without calling an old binary a new candidate.
The preparation filename still includes `-preparation-<package-source-short-sha>`.
The new playable filename includes `-playable-<package-source-short-sha>` and its
provenance status is `restricted-playable-profile-not-rc`. A successful package build
does not qualify or publish that source as the playable RC. Build the actual candidate
only after the integrated audio/world source and package checkpoint are committed.

## Import and preparation on the real client

1. In Prism, choose **Add Instance > Import**, select the ZIP and create a **new isolated
   instance**. Do not overwrite a personal profile or saved world. Let the launcher fetch
   the exact dependencies; if a publisher requires a manual download, use the original
   publisher page without substituting a different file. CurseForge import is also supported.
2. Select a working 64-bit **Java 17** for this instance. Preserve the pinned Minecraft and
   Forge versions and the supplied mod set. No custom gameplay configuration is needed:
   this package uses mod defaults, not personal settings or diagnostic overrides.
3. Launch to the title screen. Confirm Minecraft/Forge versions and that Mods lists
   Grindless, Architectury, JEI, FTB Quests, FTB Library, FTB Teams and FTB XMod Compat, plus the selected profile's
   performance/convenience dependencies. Report any startup error, or recipe-viewer text that is
   missing/truncated, with the GUI scale. This is client preparation, not progression evidence.
4. Do not replace an existing world to apply this fix. The coordinator owns backup,
   stopped-instance migration and deployment of the retained campaign. A new campaign uses
   the authorized Superflat-first settings in the root design/release documents, not an
   invented diagnostic route. No save is bundled, and no smoke world is a campaign save.

The previously delivered26c8/740 archives remain pre-S2 preparation; their provenance is
not relabeled. Newly built preparation archives use their recorded source and remain
**integration previews, not T0 acceptance**. S2 inventory/model engineering is established
on debd, but that historical proof does not qualify the changed native quest backend.
The campaign uses only actual world supplies and its authorized finite starter rewards,
no commands/creative/injection,
and observes one physical craft of the first **five F1 Relay Matrices** plus the intended
T1 handoff, stopping before T1 machinery.
Authentic retained-world compatibility is a separate, provenance-known copy plus a full
save/exit/process-restart/reopen; a fresh world or serializer test cannot substitute for it.

## Quest definitions and existing data

Both the existing Grindless Quest Book and the retired Guide open the actual FTB
Quests screen. FTB owns navigation, task detection, progress and reward collection.
Ordinary notes are short translated descriptions; native item tasks retain the
framework's recipe-viewer affordances. Matrix objectives use the supported item-filter
adapter with the existing physical rating parser, not a custom task screen.

The catalogue supplies 46 stable logical identities, their dependencies and unchanged
reward quantities. Its two native chapters are installed append-only in
`config/ftbquests/quests/chapters/`. Existing files are never replaced. Colliding IDs,
incomplete owned chapters or altered loaded definitions stop admission visibly; keep
the files and diagnostics for the operator instead of deleting or regenerating them.
An FTB failure does not open a custom fallback in this pack.

Before native claims are admitted, historical paid receipts from
`grindless_quest_progress` are marked claimed through FTB's supported API, separately
for each player UUID. Import delivers no items. Native completion is never treated as
a paid receipt, and a login or retry never lowers newer native progress. Previously
paid T1 receipts survive even without an old Relay claim, but do not themselves open
the gated T1 chapter. The legacy grant engine is permanently fenced for migrated
players; there is one active reward authority.

FTB progress belongs to the player's current team; personal teams are the default,
and party members share progress. Rewards remain individual (`team_reward=false`),
so joining another player never inherits that player's paid receipts. The starter's
eleven stacks are separate native item rewards, not one atomic inventory transaction.
Make room before collecting them: FTB records a claim before giving its stack, and
items that do not fit can drop on the ground. This is not the retired book's
full-inventory refusal/retry guarantee. Book recovery never grants starter supplies.

Native loader/adapter checks and migration fixtures are engineering evidence, not a
client/render pass, a two-player campaign or retained-world compatibility acceptance.

## Dependency provenance

The manifest makes the launcher download each upstream file; it does not vendor their jars in this
repository. Exact CurseForge project/file identifiers are pinned in `curseforge/manifest.json`.
Their licences remain theirs: Architectury API (LGPL-3.0-only), JEI (MIT),
FTB Quests/XMod Compat (ARR), Jade (CC-BY-NC-SA-4.0), Sophisticated Backpacks and Core (ARR),
FTB Essentials/Chunks/Teams/Library (ARR), ModernFix (LGPL-3.0), FerriteCore (MIT), and Embeddium
(LGPL-3.0). See the upstream project pages before redistributing a modified dependency.

Jade's corrected label comes from the exact11.13.3 source tag, not an inspected binary
licence or a newly verified jar hash. Its existing expanded-profile pin is unchanged.
Launcher references do not authorize importing/relicensing its code or assets. Jade is
omitted from the restricted playable profile; commercial/direct distribution of the
historical expanded selection still needs separate evaluation. The included archive-root
`THIRD_PARTY_NOTICES.md` retains the source evidence and limits; see also
[the repository notices](../THIRD_PARTY_NOTICES.md#jade-licence-correction-and-evidence-boundary).
