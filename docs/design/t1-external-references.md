# T1 external implementation references

Read-only source research for ADR-0169/0170/0172/0173. Patterns are conceptual evidence;
no code, assets, recipes, prose or dependency is imported. The implemented
Grindless catalogues remain authoritative.

## First-factory tooling and fabrication

| Reference | Verified fact | Grindless fit / limit |
| --- | --- | --- |
| [GT Modern v7.5.3 source](https://github.com/GregTechCEu/GregTech-Modern/tree/91a79b8a7a2b62ec6277423e6c0ded4af89a831e), LGPL-3.0 | `FormingPressLogic` uses a non-consumed recipe ingredient for retained tooling | Grindless already has retained dies in its generic executor. Reuse that mechanism; do not implement it again or add die wear without a decision |
| [GT Modern sourceaae6d4e](https://github.com/GregTechCEu/GregTech-Modern/tree/aae6d4e5195f69fd55dd1f298887f0852eeb7710), LGPL-3.0 | `CraftingRecipeLoader`, `MachineRecipeLoader` and `PartsRecipeHandler` provide manual first-tool routes and machine-based reproduction using real material | Manual bootstrap plus later automated reproduction is useful precedent. This revision is separate from the v7.5.3 pin, not a byte-verified claim about that older release |
| Same source, `OreRecipeHandler` / primitive fabrication | Quick/direct routes can coexist with fuller processing; non-electric bootstrap avoids a self-hosting machine deadlock | Do not import finite-mining dependence, steam/steel gates, voltage explosions or mandatory extra machines into Grindless |

An existing Matrix is not automatically a retained catalyst merely because GT
can copy a die from a template. Grindless's actual five-F1 Assembler recipe and
construction seed remain unchanged. Do not copy costs, intermediate counts or
optional-interface penalties from the reference.

An Immersive Engineering Metal Press comparison used its newer indexed branch,
not a verified1.20.1 pin, and is only a structural contrast: bespoke mold state
versus the already-shared Grindless catalyst mechanism. It is not used as target-
version implementation evidence. The separately pinned1.20.1 study below is
about deposits, not that press comparison.

## Industrial and later-tier extensions

The new citations below use only GT Modernaae6d4e, not an assumed byte-equivalent
v7.5.3 implementation. They support bounded design comparisons; none establishes
Grindless tier completion or authorizes another executor or copied recipe bill.

| Pinned source | Actual property | Grindless fit / limit |
| --- | --- | --- |
| [IntCircuitIngredient, lines29-53](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/api/recipe/ingredient/IntCircuitIngredient.java#L29-L53) | The programmed-circuit ingredient checks the actual item and its exact stored configuration | Compound identity matters; exact recipe selection is not the same requirement as sufficient machine/control capacity |
| [TieredWorkableElectricMultiblockMachine, lines48-67](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/api/machine/multiblock/TieredWorkableElectricMultiblockMachine.java#L48-L67) | Effective overclock tier is bounded by physical hull and configured limits | A separate capacity boundary, not evidence that a higher configuration should match an exact ingredient. Grindless's deliberate soft under-volting remains its own policy |
| [MetaTileEntityLoader, lines38-43](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/data/recipe/misc/MetaTileEntityLoader.java#L38-L43) and [steam recipes, lines435-535](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/data/recipe/misc/MetaTileEntityLoader.java#L435-L535) | Casings and several bronze/steel steam machines have shaped acquisition paths | Useful non-self-hosting construction evidence, not a complete Assembler/bootstrap or sustainability proof; do not add a mandatory Steam Age to Grindless |
| [GTMultiMachines, lines145-201](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/common/data/machines/GTMultiMachines.java#L145-L201) | Blast-furnace and chemical-reactor registrations compose different recipe-modifier sets | New conditions can use shared mechanisms rather than one execution engine per tier; begin with Grindless's existing conditions and completion contracts |
| [MultiblockPartMachine, lines109-241](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/api/machine/multiblock/part/MultiblockPartMachine.java#L109-L241) and [registerLargeBoiler, lines622-632](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/common/data/machines/GTMachineUtils.java#L622-L632) | Reusable working hooks and parameterized family registration | Compare against existing Grindless machine/store ownership first; these are not generic rollback or save-compatibility guarantees |

The reference search did **not** close the first Assembler block's own acquisition
path. Do not turn casing/steam examples into a claim that the entire external
industrial cycle is proven acyclic. Nor is GT's tier convention an exact analogue
of Grindless's orthogonal Relay/Integrated/Superconducting/Photonic/Causal
architecture and rating model.

The actual Grindless source trace is stronger than an absence-of-symbol search:
`IngredientSpec` explicitly defines exact `matrixRating` and
`ProcessLookup.sameItem` compares that rating; `ControlMatrixSpec.canControl`
separately accepts sufficient ratings. The initial T2 Wire Mill source increment
uses the existing exact operand, while ADR-0173 retains the intended stronger-
substitution/ingredient-role boundary as unfinished shared work. No global matcher
relaxation, F1 alias or quest-receipt change follows from the external example.

For each future industrial input, trace construction seed, producer, recurrent
consumables and recovery margin through actual routes. For later machinery, add
only genuinely new conditions to the established graph/executor and invalidate
only affected mechanism proofs. Do not infer closure from registered prototypes
or add a condition/parallelism framework before an implemented need exists.

## Family and mark follow-up for ADR-0174

The same newer pin reports GT version8.0.0; do not interchange it with the
v7.5.3 pin or assume its exact loader/mappings match Grindless1.20.1.

- [GTMachineUtils96-101,184-198,820-875](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/common/data/machines/GTMachineUtils.java#L96-L101)
  defines an electric-family default tier range and shared tiered registration/
  simple-machine builders. The fetched [family sample](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/common/data/GTMachines.java#L295-L336)
  includes wire mill, mixer, washer, lathe and other ordinary families. It is not
  an exhaustive LV roster or proof no individual registration overrides its tiers.
- [SimpleTieredMachine20-35](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/api/machine/SimpleTieredMachine.java#L20-L35)
  composes common persistent behavior on a shared machine class. Grindless should
  first reuse its existing chassis, UpgradeSet, stores and conditions rather
  than import another trait/registration framework.
- [MachineDefinition134-164](https://github.com/GregTechCEu/GregTech-Modern/blob/aae6d4e5195f69fd55dd1f298887f0852eeb7710/src/main/java/com/gregtechceu/gtceu/api/machine/MachineDefinition.java#L134-L164)
  and tiered registration bind distinct block/item definitions per tier. That
  differs from Grindless's accepted in-place identity/store-preserving MK target.
  A fixed-tier hatch I/O swap is not evidence of a lossless tier-upgrade tool.

The GT builders depend on GT/Registrate types and version-specific APIs; direct
import is not selected. Reusing the family/parameterization principles within
existing Grindless mechanisms avoids that coupling. No code or assets are copied;
any later material adaptation still needs exact licence/provenance/notices before
import. The source sample does not choose Grindless's complete T1 family roster.

## Longitudinal virtual-deposit continuity

| Immutable reference | Observed storage/resolution pattern | Limit |
| --- | --- | --- |
| [GT Modernaae6d4e](https://github.com/GregTechCEu/GregTech-Modern/tree/aae6d4e5195f69fd55dd1f298887f0852eeb7710), LGPL-3.0; `BedrockOreVeinSavedData` / `OreVeinWorldEntry` | Persist chosen definition identity and progress/yield values; resolve parts of definition content live | Stable ID/counter does not freeze material weights, composition or all depletion parameters across updates |
| Same source, surface generation / `OreGenCache` | Literal generated ore blocks persist in vanilla chunks; short-lived cache coordinates generation | This is not the virtual-reserve model and is not permission to add physical Superflat ore |
| [Immersive Engineering1.20.1e63e482](https://github.com/BluSunrize/ImmersiveEngineering/tree/e63e4824800945eccf3684200a2c2270e2e1cdf2), custom licence; `MineralVein` / `MineralMix` / `ExcavatorHandler` | Persist identity/position/depletion and resolve recipe/configuration content live | A live yield cap or material list can change the meaning of old progress; not a complete frozen-save guarantee |

The GT study also identifies the lazy origin/footprint ordering hazard discussed
in [GregTechCEu/GregTech-Modern#4160](https://github.com/GregTechCEu/GregTech-Modern/issues/4160).
That is evidence of a failure class, not a reason to copy its footprint machinery
into Grindless's independent per-chunk model or assume the issue's status forever.

For Grindless, the actual gap is the unused saved derivation version recorded in
[the readiness investigation](../testing/t0-longitudinal-readiness.md). A stored
but unused version is not a stronger guarantee than these working references.
Before a conflicting update, make version/material resolution real and specify
which already-observed facts remain stable. Preserve extraction totals and old
surveys, allow future content deliberately, and test changed catalogue + reload +
survey/extract ordering. Do not infer a nearest replacement for a removed ID or
silently turn an unknown identity into an empty, apparently valid deposit.

The proposed version-gated resolution is a synthesis using existing SavedData,
not a complete shipped mechanism found in these references. A generic migration
framework or a global freeze that prevents later-tier resources is not justified.
Fields intentionally live versus frozen, and deliberately added future sources,
remain coordinator product/compatibility decisions.

## Licence and evidence boundaries

GT Modern's LGPL-3.0 source and Immersive Engineering's custom visible-source
licence have different reuse/redistribution consequences. This study imports
nothing and does not replace the notices/provenance required before any import.
Source facts are separated by exact revision; indexed newer-branch snippets are
not promoted to an older pinned Forge API.

The references support the manual-bootstrap/automatic-component direction.
Actual batches, time/FU, machine ownership, transaction/persistence semantics,
source classification and tests come from Grindless's live authorities.
