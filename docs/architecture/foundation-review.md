# Foundation architecture reconciliation

This is a newly written repository-specific review, **not a recovered architecture roadmap**.
The requested earlier current/target architecture and refactor-roadmap files were absent at the
authorized baseline. The target below is constrained by accepted ADRs and actual code, not a
generic clean-architecture template.

## Ownership and changes

| Boundary | Existing authority | Foundation result |
| --- | --- | --- |
| Material/recipe definitions | `SupplyCatalogue`, `MaterialRegistry`, `ProcessGraph`, `BootstrapRecipes`, `T1Recipes` | Preserved. No second runtime recipe list or new material registry. |
| Physical Matrix policy | `ControlMatrixSpec`, `MatrixArchitecture`, `FluxTier` | Preserved pure policy; `ControlMatrixNbt` is the Minecraft serialization boundary. No new rating key or architecture. |
| Inventory persistence | Process-machine slots/menu layout | Corrected through `SlottedContainer` and explicit versioned migration. Empty slots are meaningful process data. |
| Presentation | Atlas view rows and optional Forge JEI bridge | Rating labels are presentation metadata, not changes to qualified recipe identity. |
| Quest evidence | `QuestEvidence` policy, inventory bridge, menu synchronization | `QuestInventoryEvidence` makes counting testable and avoids double-counting vanilla offhand storage. Quest claims remain non-gating. |
| Loader | Architectury stubs/capability bridges in Forge | No Forge import was added to common. No registry or loader platform was replaced. |
| Validation | Gradle-resolved classpath; one scenario manifest | Checks no longer guess jars from caches. Production smokes test the built artifact and natural exit. Dispatch itself has an executable regression fixture. |
| Assets | `BlockCatalogue`, `SupplyCatalogue`, committed JDK generator | Preserved. The T0 manifest is an audit/planning projection, not a second generator or art import. |

Package interactions such as machine/menu/registry references already exist. No whole-repository
acyclic-package proof is claimed. The new policy-to-NBT and menu-to-inventory boundaries do not
introduce a Forge-to-common reversal or duplicate Tier/recipe/state definitions.

## Deliberate non-changes

No package migration, module split, generalized capability framework, public registry rename,
new serialized world system, global research permission, T2 consumer or T0 multiblock was added.
The fixed Ground Array prototype is not reclassified as a general multiblock kernel.
The exact-rating manufacturing input remains distinct from stronger-controller substitution.

## Remaining bounded work

| Item | Evidence / priority | Safe next boundary |
| --- | --- | --- |
| Interactive BP/client acceptance | Still not observed; release gate | Run the exact [checklist](../testing/t0-survival-acceptance.md), not another proxy. |
| Full quantitative production-graph analysis | Existing checks pin recipes and the new audit graph names dependencies; no economy solver exists | Add a live-catalogue validator only for a chosen chain after its requirements are stable. |
| Native Bootstrap recipe-view coverage | Atlas currently includes the T1 shaped catalogue and calibration, not every Bootstrap/vanilla recipe | Assess discoverability in BP, then a bounded viewer slice if needed. |
| Script twins | `convert-audio.ps1` and `run-smokes-batch.sh` remain missing at this checkpoint | Port only when those optional workflows are exercised; do not claim full tooling parity. |
| Hardcoded Shadow plugin coordinate | Pre-existing `build.gradle` literal conflicts with version-location convention | A separate no-version-change build cleanup; not a reason to rework the mod's dependencies. |
| Optional development launch hang | Reproduced; production acceptance no longer uses it | Upstream ownership/fix investigation, without reflective shutdown or forced green. |
| Final art and localization | Intentionally deferred by ADR-0108 | Original candidates and visual QC after mechanics, following the asset manifest. |

Architecture has **materially advanced at the validated seams**, not become a fully completed
reorganization. Broad changes still depend on the [GO gate](../reviews/go-gate.md).
