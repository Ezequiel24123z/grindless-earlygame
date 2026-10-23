# T0 external design references

This is a source/licence/design record, not imported implementation or acceptance.
Grindless keeps original layouts, names, quest prose, prices, models and processing.
Public source or modpack inclusion alone does not grant redistribution rights.

## Native inventory admission and recovery exchange

| Field | Record |
| --- | --- |
| Project/revision | GregTech CEu Modern, `b3b0ce37989c2dd0b28c6780ec71e20231c57662` |
| Actual sources | [RecipeHelper](https://github.com/GregTechCEu/GregTech-Modern/blob/b3b0ce37989c2dd0b28c6780ec71e20231c57662/src/main/java/com/gregtechceu/gtceu/api/recipe/RecipeHelper.java#L179-L204), [RecipeRunner](https://github.com/GregTechCEu/GregTech-Modern/blob/b3b0ce37989c2dd0b28c6780ec71e20231c57662/src/main/java/com/gregtechceu/gtceu/api/recipe/RecipeRunner.java#L121-L165). Parent re-reads the pinned Git tree to verify both paths rather than guessing a machine-trait path |
| Licence/classification | LGPL-3.0 source; CONCEPTUAL INSPIRATION, not a copied barter implementation |
| Problem | A recovery exchange must not consume payment if its complete result cannot fit |
| Strength | Check every input and output before committing execution |
| Limitation | A process runner can consume at start and deliver later; that is not the desired one-call fixed-inventory barter contract |
| Adopted principle | Admit the complete physical-item input/output batch, then commit through the existing native inventory mechanism |
| Not copied | Source code, recipe quantities, buses, voiding, delayed outputs, quests, textures, UI or a supposed literal GT barter system |
| Grindless fit | A bounded36-slot inventory can be copied/staged and committed synchronously; no generic external-handler rollback guarantee is claimed |
| Implications/decision | Retain one price/offer authority and explicit rejection; original direct barter, ADR-0163 |

## Optional machine inspection ecosystem

| Field | Record |
| --- | --- |
| Project/revision | Snownee/Jade, `forge-11.13.3`, commit `d7a9e1d3b782f2ea4c387c66e973cc46b9768270` |
| Publisher identity | Existing historical project324717/file8479276, `Jade-1.20.1-Forge-11.13.3.jar`; not a floating replacement |
| Actual sources | [plugin API](https://github.com/Snownee/Jade/blob/d7a9e1d3b782f2ea4c387c66e973cc46b9768270/src/main/java/snownee/jade/api/IWailaPlugin.java), [server provider](https://github.com/Snownee/Jade/blob/d7a9e1d3b782f2ea4c387c66e973cc46b9768270/src/main/java/snownee/jade/api/IServerDataProvider.java), [licence](https://github.com/Snownee/Jade/blob/d7a9e1d3b782f2ea4c387c66e973cc46b9768270/LICENSE.md) |
| Compatibility | Exact Forge1.20.1 API verified by the read-only research lane; common/server/client registration is separate |
| Licence | CC-BY-NC-SA-4.0, not LGPL; existing notices/ADR-0149 do not approve silently adding it to the restricted playable profile |
| Classification | COMPATIBILITY TARGET; optional API adapter only if isolated/absence-safe. Pack inclusion, direct redistribution or adapted external material remains DEFERRED |
| Strength | Server-data/client-presentation split; Forge ASM annotation discovery avoids requiring a plugin class when the overlay is absent |
| Limitation | The existing seven-pin playable profile intentionally excludes Jade; source API verification is not a licence/pack-use approval or an observed binary compatibility pass |
| Adopted principle | Bounded server-authoritative snapshots, optional presentation, public API only and no world scans |
| Not copied | Jade implementation, assets, provider bodies, UI, jar or API classes inside the Grindless artifact |
| Grindless fit | A small original native aimed-machine overlay can present the same neutral DTO without a new mandatory dependency or an unapproved pack change |
| Implications/decision | Native equivalent is the deliverable that works when Jade is absent. No MIT relicensing or project-wide licence assurance is inferred |

## Bounded structure/interface research

| Field | Record |
| --- | --- |
| Primary project/revision | GregTech CEu Modern, `v7.5.3-1.20.1`, `91a79b8a7a2b62ec6277423e6c0ded4af89a831e`, LGPL-3.0 |
| Proxy source | [CokeOvenHatch](https://github.com/GregTechCEu/GregTech-Modern/blob/91a79b8a7a2b62ec6277423e6c0ded4af89a831e/src/main/java/com/gregtechceu/gtceu/common/machine/multiblock/part/CokeOvenHatch.java) |
| Count source | [TraceabilityPredicate](https://github.com/GregTechCEu/GregTech-Modern/blob/91a79b8a7a2b62ec6277423e6c0ded4af89a831e/src/main/java/com/gregtechceu/gtceu/api/pattern/TraceabilityPredicate.java) |
| Contrasting project/revision | Mekanism `1.20.x`, `96c736241e308a9cf1c73f7dcfa9aa221ea559d3`, MIT |
| Contrasting sources | [TileComponentConfig](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/tile/component/TileComponentConfig.java), [MultiblockManager](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/lib/multiblock/MultiblockManager.java) |
| Classification | CONCEPTUAL INSPIRATION; no code, assets, dependency, DSL or reflection/persistence framework imported |
| Adopted principles | Fixed candidate roles with min/max counts; zero-storage controller proxy; relative sides; invalidate/rebind on every formation, removal, rotation and unload |
| Concrete review finding | Controller replacement at the same position must retire the old block-entity/store identity. Chunk-unload checks alone do not cover reform |
| Grindless adaptation | Two fixed T0 shapes, six original family-compatible variants/four roles, existing controller/native-store transaction authority and per-access loaded identity/shape checks |
| Not copied | GT pattern/layout/DSL, Coke Oven geometry, names, rendering, proxy bodies, recipe numbers, Mekanism cache/chemical/security systems |
| Why no generic kernel | Seven known Firing cells and two known Trough parts need bounded role/count checks, not variable aisles, huge patterns, many transmission types or an external storage cache |
| Additional native invariant | Partial output-quota visibility must bound native batch reservation across aliases and be revalidated before commit; same store identity alone is insufficient |

The research's player-security comparison is **not** adopted as structural-owner
evidence. A player's UUID/trust permissions and a port's owning controller are
different concepts. Existing public controller-only automation remains compatible;
this programme does not invent a player-ownership/security migration.

## Rules for subsequent entries

Record the immutable project/revision, exact system source, authoritative licence,
classification, problem, adopted principle, rejected aspects, original adaptation,
compatibility/performance/save implications and related ADR. A dependency or
licence-permitted material import needs an explicit applicable decision and notices
before inclusion. A source comparison alone never authorizes close imitation.
