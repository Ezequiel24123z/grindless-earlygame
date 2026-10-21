# T0 longitudinal-start readiness

Investigation requested by the owner on2026-10-21: the intended game is a
longitudinal reference world, not a disposable development playthrough.
This report distinguishes a demonstrated defect, missing objective observation,
missing delivery work and stale wording. It does not redo the completed T0 audit.

## Finding

**Later owner decision (ADR-0170):** after this investigation the owner explicitly
chooses to begin the installed T0 game as the intended longitudinal run while T1
development continues. This supersedes the temporary execution pause and old
disposable-only start restriction, not the observed facts or unresolved technical
properties in this report. Player blockers take priority; final T0 feedback drives
polish. No completed launch, save/reopen or campaign is inferred from intent.

**The installed candidate is not yet qualified as the longitudinal baseline.**
That is not evidence that its new worlds corrupt, and it does not mean the owner
must complete T0 before receiving a playable candidate. Human campaign acceptance
comes from the longitudinal run, not before it (ADR-0144).

There is a substantial technically qualified F candidate already installed.
The remaining objective client/start/save-reopen observation and an explicit
baseline/update contract must be closed. A real forward-update defect in the
vein-version contract is separate from those missing observations.

## Fresh installed facts

The coordinator performs only read-only production Inspect and bounded inventory
after the T1 executor explicitly returns its lease. No launcher, JVM, world,
configuration, artifact or authentication is changed.

| Property | Observation |
| --- | --- |
| Instance | `Grindless-T0-Dev` |
| Source and runtime | `298988d500dccf0823fb2eab52fbe57bff8fe831` (F) |
| Installed JAR SHA-256 | `95bf47c4e5d08a0788095555ed38078431e8f74d8d491389b56b7539d7c91621` |
| Installed ZIP SHA-256 | `3e5ad1fbbc23d19232a589bce4430ff2bdc344d0c6776bbf71400579aab607a1` |
| Production Inspect | `VerifiedStopped`, exact F, `restricted-playable-profile-not-rc` |
| Supplemental observation UTC | `2026-10-21T16:30:22.5646703Z` |
| Components / memory | Minecraft1.20.1, Forge47.4.23, LWJGL3.3.1;512–4096MiB instance memory |
| Live state | Prism not running;0Java;0 worlds containing `level.dat`; no `options.txt` or `logs/latest.log` in this instance |
| Launcher metadata | `instance.cfg` SHA-256 `a3e1e30ea53607a5558b672dac204ee7e082a8e0159550be0d9a28c0bd608842`; `mmc-pack.json` `e6185fd21a712b0c3ea7d07daa043b670db7a69baeb1983b0926d59be55062d2` |
| Existing proof | [Phase A](t0-experience-source-qualification.md), [Phase B](t0-experience-package-native.md), [exact deployment](t0-experience-prism-deployment.md) and both successful exact-F hosted runs remain applicable within their scopes |
| Public release check | No GitHub releases returned at this investigation; a release label is not itself a functional test |

Absence of a client log/options/save is not proof of a crash. It means there is
no observed initial client game or save-reopen result to certify. The later T1
branch and its incomplete canonical-output checks are **not** installed and do
not invalidate or replace F's successful checks.

Closed private evidence is retained outside Git. Its sanitized read-only outputs
have SHA-256 `82cf248b89857a062840c07e6e8822ca6800f04ae0c8d431cac8958ae7f7b300`
(Inspect) and `62f4b20bc98535766bc0c0d1cf5c87bbeaf616b78b66fb727d7bd761782e9fa1`
(installed-state observation). Private addresses, host paths, accounts and world
identifiers are not included here.

## Independent pack/entry review

Read-only QA reviews immutable F, not the later T1 source. It finds **no
demonstrated functional blocker in the initial Survival-to-kit route**:

| Source at F | Actual contract |
| --- | --- |
| `forge/.../quest/QuestBookAccessEvents.java:18–44` | Login/respawn invokes book recovery without Creative, permissions or preset conditions |
| `quest/QuestCatalogue.java:86–101` and `QuestEvidence.java:14–19` | The first starter task is free, has no prerequisites and supplies the agreed eleven stacks |
| `item/QuestBookItem.java:32–39` and `forge/.../quest/HeraclesQuestClient.java:70–79,117–121` | The book opens the chosen renderer/fallback and exposes the server-authoritative free-kit request |
| `quest/QuestClaimService.java:68–115` | The book must be held; the complete reward batch must fit before the once-only claim is recorded |
| `pack/profiles.json:10–13`, manifest and dependency metadata | All seven selected projects have unique matching pins and their declared required dependencies are included |

Thus trees, a village, paper crafting, physical ores, commands or Creative are
not prerequisites for receiving the initial kit. The reviewed code works with
manually selected vanilla classic Superflat; that statement is static evidence,
not a clicked/client-world pass.

Two delivery gaps are real but are not missing gameplay routes:

- `pack/public-overrides.json:1–4` is empty, no `world_preset`/
  `flat_level_generator_preset` resource is delivered, and `pack/README.md:62–68`
  still describes the official preset as pending. Pin and explain the accepted
  vanilla classic-Superflat selection first; do not invent another generator or
  change the settled first-quest kit merely to obtain a custom preset ID.
- The distributed `pack/README.md:129–157` still explains historical Overworld
  preparation rather than current Superflat entry and longitudinal updates.
  `build.gradle:321–330` includes that document in the ZIP. Replace the shipped
  instructions with the actual F entry, free-kit/book recovery, baseline identity
  and safe update steps. This can retain the exact F JAR, but a repackaged ZIP has
  a new hash/provenance and must not be called the old ZIP.

The ZIP does not promise offline installation: the launcher supplies Minecraft,
Forge, Java and dependencies (`pack/README.md:11–13`). No missing library or
broken pin is demonstrated by this review.

Some apparent blockers are stale prose, not code failures:
`t0-client-review-preparation.md:13–20` still targets747, and
`t0-starter-kit.md:42–52` says book recovery stops after the starter claim.
Actual F `QuestBookAccess.java:41–52` recovers a missing book independently of
that claim. Update those explanations; do not reintroduce a restriction into code.

## Save continuity: present safeguards and real gap

Application reads in this investigation are byte-identical to installed F;
the coordinator's branch contains documentation changes only in these surfaces.

| Source at F | What it actually establishes | Boundary |
| --- | --- | --- |
| `quest/QuestProgress.java:25–34,61–103` | Stable `grindless_quest_progress`, per-player `Players/Player/Tasks` and string task IDs | Deleted IDs are filtered on load. Future updates must preserve old IDs; a schema test is not a real player-world upgrade |
| `container/SlottedContainer.java:24–60` and `machine/ProcessMachineBlockEntity.java:931–989` | Indexed slots, persisted work/recipe/fluid/output quota and explicit older Assembler output migration | Legacy compact saves cannot reconstruct absent historical slot positions. This is not a reason to reject a new F-format world |
| `matrix/ControlMatrixNbt.java:19–43` | Existing numeric `Rating`, legacy untagged frontier interpretation and rejection of malformed explicit data | Preserve tier ordinal/architecture meaning; no downgrade guarantee is inferred |
| `tools/checks/VerifyPersistence.java:195–230` | Existing binary claim round trips and once-only starter behavior under the tested codec | Not a final graphical save/reconnect or reference-world migration observation |
| `vein/VeinData.java:30–54,104–120` | A saved `DerivationVersion` and per-chunk extracted totals | The saved version is not consumed by current generation callers |
| `vein/VeinGenerator.java:34,75–96`; `machine/CrudeExtractorBlockEntity.java:191–198`; `machine/TerrestrialExtractorBlockEntity.java:151–158`; `item/ProspectorsScannerItem.java:39–49` | Scanner/extractors derive from the current `VERSION` and live material list | Changing generation version, material order or weights in an update can remap previously surveyed resources despite the stated per-world pin |

The vein issue is a **real forward-compatibility contract defect**, not a
demonstrated corruption of a new F world today. Version1 with unchanged materials
remains deterministic. Existing `VerifyVein` checks same-input determinism, not
an old-world derivation under a changed future catalogue/version.

Before an update changes those inputs, the saved-world derivation identity and
compatible material mapping must actually govern all three production callers.
Preserve the existing version1 result, make unknown versions explicit and add a
regression where a saved/surveyed old world retains its material/richness/reserve/
depletion across a newer derivation/catalogue. A version number stored but unused
is not an adequate compatibility guarantee. Do not silently rewrite surveyed
chunks or treat version-only hashing as that proof.

No new serializer, transfer engine, global permission or generic migration
framework is justified by this investigation. Reuse the existing SavedData,
version field, registry/codec contracts and copy-based validation.

## What must be closed, and when

| Boundary | Required outcome | Not required before beginning the campaign |
| --- | --- | --- |
| Before recommending the first longitudinal start | Exact candidate client launch; intended new Superflat entry/book/free starter/menus; representative machine/tank/quest state survives save, exit and reopen; no known ordinary-start blocker | Completing T0, approving every sound/art choice or playing the whole progression twice |
| Baseline handover | Freeze the actual source/JAR/ZIP/dependency/preset identity; retain the installable artifact and provide an explicit stopped-game backup/update/rollback procedure | Rebuilding unchanged F solely to change a documentation hash or mark it RC |
| Before the first update | Compare changed IDs/NBT/recipes/config/world derivation; repair the vein-version gap if its inputs change; validate the new artifact on a verified copy of the actual reference save while preserving the original | A fictional pre-existing longitudinal save before the first world is created |
| During real play | Owner feedback establishes pacing, clarity, appearance, listening/fatigue, ergonomics and human acceptance | A prior fabricated pass for those subjective properties |

The full [controlled protocol](t0-dev-playthrough.md) retains its actual G03/G18
and other unobserved client properties; none is silently marked passed here.
Diagnostic setup remains separate from the reference world. Never adopt a
command-staged smoke save as the owner's no-command campaign.

The current tools see the coordinator's local applications, not the personal
PC's Prism desktop. No existing remote-client window is available; ordinary SSH
does not observe rendered menus, input or audible sound. A controlled graphical
run must therefore use an approved graphical route or the owner's observations.
No RDP/service/firewall/login workaround is enabled.

## Corrective sequence

1. Keep installed F and pause competing T1 PC execution. Verify exact installation
   read-only, as recorded above; do not deploy a changing T1 branch over the game.
2. Perform one bounded client smoke on a separate new world: correct world type,
   initial book/free claim and reopen, representative UI/inventory/machine/tank
   state, clean save/exit/reopen. Use Survival, cheats/bonus chest off and explicit
   classic Superflat; also check ordinary Overworld entry briefly. Record actual
   failures and fix only demonstrated blockers. Reuse the existing server, recipe,
   transaction and asset proofs; no full T0 route is needed for this smoke.
3. Correct/pin the distributed world-selection and entry/update guide, retaining
   F's JAR if no application/resource change is necessary. Hand over the exact
   versioned baseline and safe backup/update procedure. The
   owner then creates the genuine fresh reference world; do not require a complete
   T0 route beforehand or certify it before it is played.
4. Before updating that reference world, enforce old IDs/save meaning and the
   world-derivation contract, back up while stopped, test a copy and only then
   offer the update. Preserve the previous artifact and never imply downgrade
   compatibility from a successful forward upgrade.

## Disclosure

| Category | Investigation outcome |
| --- | --- |
| COMPLETED | Exact installed F reverified read-only; independent pack/entry review finds the real free-kit route and consistent seven pins; save safeguards and real vein forward-update gap distinguished from missing observations and stale instructions |
| BLOCKED | Objective graphical start/save-reopen remains unobserved; no personal-PC GUI target is exposed to the current tools |
| NOT EXECUTED | Client/world creation or reload, real save-copy upgrade, compatibility-code repair, RC publication, T1 deployment or new native/JVM execution |
| NEEDS HUMAN VALIDATION | Controlled client observation, then genuine campaign feedback/acceptance |
| NEEDS MY DECISION | None for this investigation; no gate waiver or automatic adoption of an old world is assumed |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | An approved graphical execution route or the owner's controlled observations; no credential value is requested |
| COST / BILLING RISK | No generation, paid service, model installation or new access was activated |
| DEPLOYED STATE | Exact F/95bf47c4/3e5ad1fb remains installed and stopped;0 worlds observed; no instance/world/configuration mutation |
