# Native FTB questbook integration reference

Source/API and binary research for the player-blocking replacement accepted in
ADR-0171. This is not an executed Grindless integration, client or migration pass.
The native framework owns the experience; no upstream source, assets or other
pack's writing is copied into Grindless.

## Exact binary evidence

The public file index locates candidates; the actual JAR bytes were independently
retrieved from official FTB Maven and CurseForge CDN and matched. Manifest
dependencies below come from each JAR's `META-INF/mods.toml`, not an assumed
relationship between source repositories.

| Forge artifact | Version | CurseForge project / file | Bytes | SHA-256 |
| --- | --- | --- | ---: | --- |
| FTB Quests | 2001.4.22 | 289412 / 8078538 | 1255469 | `9ea9d159a78a2724cfe0457b024680fdb52a2126eff9cfa5280a30578f647a68` |
| FTB Library | 2001.2.13 | 404465 / 8226927 | 780558 | `41810281b65336265cae2c88412022d02f2f0a3f28c66a06173d86f98acf14a7` |
| FTB Teams | 2001.3.2 | 404468 / 7499810 | 252579 | `b1cb4d82ea9f137a87e12a7aaf3329619294b56f7d70337dfd557a76e10b6e07` |
| FTB XMod Compat | 2.1.3 | 889915 / 6402486 | 129725 | `16a8eb9de88d5c33d29cb076cc39dfb337b2c54348e1ef1efe9dd60cf85e5f72` |

Official download examples:
[Quests Maven](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-forge/2001.4.22/ftb-quests-forge-2001.4.22.jar),
[Quests CDN](https://edge.forgecdn.net/files/8078/538/ftb-quests-forge-2001.4.22.jar),
[Library CDN](https://edge.forgecdn.net/files/8226/927/ftb-library-forge-2001.2.13.jar),
[Teams CDN](https://edge.forgecdn.net/files/7499/810/ftb-teams-forge-2001.3.2.jar),
[XMod Compat CDN](https://edge.forgecdn.net/files/6402/486/ftb-xmod-compat-forge-2.1.3.jar).
Versions in this evidence table are observed artifact identities; executable
build coordinates remain centralized in `gradle.properties`.

Quests requires Architectury >=9.0.8, Library >=2001.2.1 and Teams >=2001.1.4.
The actual selected Teams binary raises the Architectury floor to **9.1.12**.
Library2001.2.13 requires Forge >=47.3; the selected47.4.23 satisfies it.
Item Filters is optional, not a hidden mandatory install. Library2001.2.13's
published artifact is a release; a source-head upload flag is not its binary
release classification.

The FTB family is visible-source/All Rights Reserved, not MIT or automatically
open-source reusable implementation. The adapter uses APIs and the pack references
official downloads; this does not grant arbitrary redistribution or source/asset
copying rights. Architectury is LGPL-3.0. Keep the actual dependency notices and
distribution consequences in [the notices](../../THIRD_PARTY_NOTICES.md).

## Native APIs and the migration boundary

Reviewed Quests source is immutable commit
`a74ee04bf80e7fba2c8475112a07ace4a4abd0a6` on its Forge1.20.1 line, whose
properties name2001.4.22. Source version correspondence is not a substitute for
compiling/testing the exact selected binary API.

| Native source | Verified behavior and integration implication |
| --- | --- |
| [BaseQuestFile](https://github.com/FTBTeam/FTB-Quests/blob/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6/common/src/main/java/dev/ftb/mods/ftbquests/quest/BaseQuestFile.java) | Native `data.snbt`, chapter groups, chapter/task/reward definitions and reward tables; use the actual loader rather than a parallel schema |
| [FTBQuestsClient](https://github.com/FTBTeam/FTB-Quests/blob/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6/common/src/main/java/dev/ftb/mods/ftbquests/client/FTBQuestsClient.java) | `openGui()` opens the genuine client screen; the legacy book bridge must execute on the correct side |
| [TeamData](https://github.com/FTBTeam/FTB-Quests/blob/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6/common/src/main/java/dev/ftb/mods/ftbquests/quest/TeamData.java) | `markRewardAsClaimed(UUID, Reward, long)` imports a paid receipt without executing its reward; `isRewardClaimed`, `setProgress(Task,long)` and `saveIfChanged` are separate operations |
| [Reward](https://github.com/FTBTeam/FTB-Quests/blob/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6/common/src/main/java/dev/ftb/mods/ftbquests/quest/reward/Reward.java) | Individual reward receipts and disabled auto-claim are native defaults; set the intended behavior explicitly rather than depending on mutable defaults |
| [ItemReward](https://github.com/FTBTeam/FTB-Quests/blob/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6/common/src/main/java/dev/ftb/mods/ftbquests/quest/reward/ItemReward.java) | Native delivery can drop overflow. TeamData marks a reward before its `void claim` call; there is no stock atomic refuse-until-room return channel |
| [ItemFilterAdapter](https://github.com/FTBTeam/FTB-Quests/blob/a74ee04bf80e7fba2c8475112a07ace4a4abd0a6/common/src/main/java/dev/ftb/mods/ftbquests/api/ItemFilterAdapter.java) | A small owned filter adapter can keep stock ItemTask while recognizing exact Matrix rating, including the real untagged legacy F1 interpretation |

A tagged stock NBT filter does not match an untagged stack merely because weak
matching is enabled. Bind the owned Matrix filter explicitly to canonical
`ControlMatrixItem`/NBT semantics; do not accept wrong items, fractional/string
ratings or substitute F2 for the exact five-F1 T0 evidence.

Native TeamData progress is keyed by team. Reward receipts are keyed by individual
player unless configured as a team reward. Personal teams and chosen parties
therefore need explicit tests; individual paid history must not become a shared
claim or a newly payable reward. Do not invent a second minting ledger to simulate
the old screen.

Use public receipt/progress APIs for legacy import, before admitting new native
claims. Do not directly rewrite native progress files or call the native reward
delivery operation to simulate an already-paid receipt. A custom reward plus
reset-on-failure workaround is not adopted: it would not turn the native mark-
then-grant path into a proved crash-atomic transaction.

## JEI requires the native compatibility provider

FTB Quests core provides a `RecipeModHelper.NoOp`; the native item click and
recipe tooltip are gated on `isRecipeModAvailable()`. Merely including Quests,
Library and JEI does not demonstrate that interaction.

XMod Compat `v2.1.3` is pinned to
`3f9d9fe165f8ba110d0dd88aa1316cee2687cf49`.
[FTBQuestsSetup.init](https://github.com/FTBTeam/FTB-XMod-Compat/blob/3f9d9fe165f8ba110d0dd88aa1316cee2687cf49/common/src/main/java/dev/ftb/mods/ftbxmodcompat/ftbquests/FTBQuestsSetup.java)
installs its JEI helper when JEI is present.
[JEIRecipeHelper](https://github.com/FTBTeam/FTB-XMod-Compat/blob/3f9d9fe165f8ba110d0dd88aa1316cee2687cf49/common/src/main/java/dev/ftb/mods/ftbxmodcompat/ftbquests/jei/helper/JEIRecipeHelper.java)
and the native JEI plugin call the actual recipe GUI. Include this verified
provider for the requested JEI-first experience; do not ship a NoOp and claim
that the user's recipe-click requirement is satisfied.

## Actual native modpack examples

The bounded example study inspects native files, not only documentation. It
does not import quest text, rewards, graphics, scripts or progression.

| Pack / immutable revision | Actual sampled files and pattern | Evidence limit |
| --- | --- | --- |
| [Monifactory6db1be9](https://github.com/Omicron-Industries/Monifactory/tree/6db1be95d680ce75c1aa251fdcc9dc38d75ae8f6), Forge1.20.1, LGPL-3.0 | [dependency_chain.snbt](https://github.com/Omicron-Industries/Monifactory/blob/6db1be95d680ce75c1aa251fdcc9dc38d75ae8f6/config/ftbquests/quests/chapters/dependency_chain.snbt) and [simulating_resources.snbt](https://github.com/Omicron-Industries/Monifactory/blob/6db1be95d680ce75c1aa251fdcc9dc38d75ae8f6/config/ftbquests/quests/chapters/simulating_resources.snbt) use native dependencies, item tasks, rewards and cross-chapter links; some plumbing is deliberately invisible | Small selected samples, not a whole-pack voice/length audit. No custom currency or command-stage plumbing is adopted |
| [ATM-9255ca23](https://github.com/AllTheMods/ATM-9/tree/255ca23b91cebbb57130074e0bf7b86c266ac2a6), Forge1.20.1, All Rights Reserved | [welcome.snbt](https://github.com/AllTheMods/ATM-9/blob/255ca23b91cebbb57130074e0bf7b86c266ac2a6/config/ftbquests/quests/chapters/welcome.snbt) and [pylons.snbt](https://github.com/AllTheMods/ATM-9/blob/255ca23b91cebbb57130074e0bf7b86c266ac2a6/config/ftbquests/quests/chapters/pylons.snbt) use native checkmark/item tasks, dependencies, rewards and externalized language keys | Its referenced prose was not read, so no claim about its voice or brevity. The pack explicitly restricts redistribution of quest content |

These examples support using the framework's own graph/task/reward mechanisms,
not another custom screen. The short human voice is the Grindless owner's brief,
not a supposed universal rule that every mature pack uses tiny descriptions.
Neither sampled chapter implements a special recipe-viewer task; Grindless's
actual native JEI provider is established separately above. Retain existing
logical IDs/dependencies for the current fix, rather than importing another
pack's hidden stages, currency or a new required reading checklist.

## Proof still required

Actual compilation, native SNBT loading/ownership, logical-ID mapping, old paid
receipt import, two-player/team behavior, exact Matrix evidence, restart/retry,
overflow delivery, loader/side integration, package hashes and protected world
copy remain implementation/qualification work. A public-source review or correct
JAR hash alone supplies none of those runtime results. Real client presentation
remains distinct from a server probe.
