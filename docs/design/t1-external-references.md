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

### Concrete upgrade transfer comparison

The follow-up inspects **Mekanism10.4.16, Minecraft1.20.1, Forge47.1.76** at
`96c736241e308a9cf1c73f7dcfa9aa221ea559d3`, not its newer NeoForge default branch.
That revision's [MIT licence](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/LICENSE)
permits attributed reuse; no implementation or asset is copied by this study.

| Pinned implementation | Actual mechanism | Fit / limit |
| --- | --- | --- |
| [ItemTierInstaller58-110](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/item/ItemTierInstaller.java#L58-L110) | Snapshot old tile, replace the registered block, fetch a new tile and apply data; an open GUI refuses, but active work is not categorically refused | Same position is not same block/BE identity. The post-swap wrong-tile error has no rollback in this path |
| [ElectricMachine122-123](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/tile/prefab/TileEntityElectricMachine.java#L122-L123) and [Factory461-483](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/tile/factory/TileEntityFactory.java#L461-L483) | Energy amount, progress array, inventory values and serialized components copy into the destination's own stores/components | DTO references are read sources, not adoption of the old energy/inventory objects. This concrete Forge code closes the earlier reference-transplant inference |
| [CapabilityTileEntity72-81](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/tile/base/CapabilityTileEntity.java#L72-L81) and [Transmitter314-380](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/tile/transmitter/TileEntityTransmitter.java#L314-L380) | Old capabilities/network membership are invalidated after replacement; transmitter updates can partially succeed | Rebinding compensates for changed object identity, not proof of Grindless's same-object/store/connection preservation |

Directly importing that pipeline would introduce block/BE replacement, contrary
to Grindless's accepted in-place chassis requirement. Retain the existing
ChassisMark/UpgradeSet/store ownership and explicitly handle active paid work;
do not add a transplant framework merely because a compatible licence exists.
The useful comparison is the concrete preservation inventory and failure order,
not a borrowed idle-only rule or an atomicity guarantee. No native execution
or safe retained-world upgrade is proved by these source reads.

A targeted GT follow-up found no fitting in-place upgrade mechanism in the
inspected paths, consistent with its pinned distinct-tier registration above.
This is a bounded negative result, not exhaustive proof no narrow mechanism
exists anywhere in GT. Newer NeoForge transactions are not promoted to this
Forge1.20.1 evidence.

### Paid-work and cycle-setting reference

Separate read-only research verifies GregTechCEu/GregTech-Modern commit
`248e1f83829d585632015ceb2686acf6f9f23db3`, version8.0.0, against its
[Minecraft1.20.1/Forge47.4.0 version catalogue][gt-paid-versions] and
[LGPL-3.0 licence][gt-paid-license]. Earlier lead aae6d4e is an ancestor of
that pinned1.20.1 revision;91a79b8a is a divergent historical lead, not proof
of the current Forge implementation. Neither head movement nor a version label
alone certifies behavior.

The [overclock helper][gt-paid-oc] compares voltage with recipe EU/t and tick
duration, applying its own standard4x-EU/t/half-duration trade. These are GT
numbers, not Grindless balance or a new accepted tier rule. The useful pattern
is the [recipe trait's persisted execution identity and cycle state][gt-paid-state]:
canonical origin, executable recipe copy, progress and duration stay distinct.
[Setup and completion][gt-paid-cycle] sample the modified recipe at cycle
boundaries rather than retroactively treating paid ticks as a new recipe.
With regressWhenWaiting=false, a stall retains progress; definitions opting in
may reset progress to1. Machine-specific tier/voltage accessors and invalidation
hooks were not traced, so this is not a blanket safe-mid-cycle-upgrade claim.

GT consumes item/fluid inputs at setup; Grindless settles them through its
existing completion transaction and rollback snapshot. Do not transplant the GT
engine, consume inputs twice or change that ownership model. Conceptual reuse
of fixed-cycle settings/identity is relevant; direct source reuse would first
need compatible licence/distribution/notices review. No code/assets were copied.

The local source investigation remains a limit, not a new implementation:
MachineBlockEntity preserves its energy object and saves marks/UpgradeSet, while
ProcessMachineBlockEntity saves base-work progress/WorkRecipe and retains
completion rollback. Actual player installers are absent; energyMultiplier and
parallelCount are not wired as process consumers, and current kinds' narrow/full
envelopes coincide. A saved mark or stronger construction ingredient is therefore
not a qualified capability upgrade. Future work must reuse those authorities,
wire real trades, preserve paid work/stores and add an actual reachable
capability without narrowing legacy MKI. This research selects no kit, recipe,
envelope number, overclock formula or native pass.

[gt-paid-versions]: https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/gradle/libs.versions.toml
[gt-paid-license]: https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/LICENSE
[gt-paid-oc]: https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/recipe/OverclockingLogic.java#L26-L140
[gt-paid-state]: https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/machine/trait/recipe/RecipeLogic.java#L116-L165
[gt-paid-cycle]: https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/machine/trait/recipe/RecipeLogic.java#L452-L634

## Explicit manufacturing choice

The bounded source study uses the same Forge-era GT pin248e1f8
(LGPL-3.0; version8.0.0) and Mekanism96c7362 (MIT; Minecraft1.20.1,
Forge47.1.76/version10.4.16) already recorded above. No code, assets, UI library
or new dependency is imported. The study is source evidence, not runtime or
race-condition proof; untraced invalidation hooks remain an explicit limit.

- [GT circuit ingredient](https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/recipe/ingredient/IntCircuitIngredient.java#L20-L53)
  matches a programmed circuit value. That is an ingredient disambiguator, not
  a recipe-ID chooser. The
  [builder circuitMeta path](https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/data/recipe/builder/GTRecipeBuilder.java#L657-L731)
  explicitly uses notConsumable/chance0; the initial research description of
  ordinary consumption was wrong. The extraction handler was not re-traced,
  so matching, builder policy and observed execution remain distinct.
- [GT programmable circuit trait](https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/common/machine/trait/ProgrammableCircuitSlotTrait.java#L33-L107)
  persists configuration and connects it to matching through its existing
  framework. Importing its ghost-slot/ModularUI path would not fix Grindless's
  identical existing bills without new recipe/framework coupling.
- [GT RecipeLogic](https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/machine/trait/recipe/RecipeLogic.java#L383-L488)
  separates cached recipe validation from a later search/setup. This is useful
  prior art for intent versus execution, not proof that every external selector
  mutation preserves paid work or that our fractional carry is covered.
  Its progress/duration are integers with zero resets, not fractional carry.
  The core active branch is distinct from search; the traced generic
  [change notification](https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/machine/trait/recipe/RecipeHandlerList.java#L203-L234)
  does not establish every machine-specific interrupt/invalidation hook.
  [searchRecipe166-190](https://github.com/GregTechCEu/GregTech-Modern/blob/248e1f83829d585632015ceb2686acf6f9f23db3/src/main/java/com/gregtechceu/gtceu/api/recipe/GTRecipeType.java#L166-L190)
  was verified at the pin, not inferred from the indexed default branch.
- [Mekanism RecipeFormula](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/content/assemblicator/RecipeFormula.java#L18-L64)
  snapshots a crafting grid, resolves its recipe and revalidates actual inputs.
  Grindless already has stable ProcessRecipe IDs; it need not add a formula
  item, duplicate grid or another recipe resolver.
- [Mekanism formula loading](https://github.com/mekanism/Mekanism/blob/96c736241e308a9cf1c73f7dcfa9aa221ea559d3/src/main/java/mekanism/common/tile/machine/TileEntityFormulaicAssemblicator.java#L268-L330)
  differentiates a changed formula and resets operating ticks on an actual
  different recipe. Do not copy that progress-loss policy or treat unresolved
  formula fallback as permission to substitute another selected output.

ADR-0185 adopts only the compatible persisted-choice/revalidation patterns
through the existing Assembler/menu/transaction and recipe-owned work. The new
native boundary must establish real target reachability and retained work;
catalogue presence and ready51 do not establish them.

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

## Manual normal-machine construction and Forge ingredients

ADR-0188 is the owner's product rule, not a claim about GregTech's actual
machine recipes. Existing Grindless shaped JSON, Matrix decoder/minimum matcher
and Forge's public ingredient API solve this boundary; importing a GT recipe/UI
framework or adding a new manual workstation is unnecessary.

The configured Forge coordinate is1.20.1-47.4.23. Its published changelog/merge
correlates the examined source with
[3c3f496015632ab84560be77a57cc57ac42c49e1](https://github.com/MinecraftForge/MinecraftForge/tree/3c3f496015632ab84560be77a57cc57ac42c49e1);
this is NOT an artifact-bytecode/build attestation. The existing Forge
dependency supplies LGPL-2.1-only APIs; no implementation/assets are copied.

| Verified API / reference | Reuse and constraint |
| --- | --- |
| [`AbstractIngredient`](https://github.com/MinecraftForge/MinecraftForge/blob/3c3f496015632ab84560be77a57cc57ac42c49e1/src/main/java/net/minecraftforge/common/crafting/AbstractIngredient.java) and [`IIngredientSerializer`](https://github.com/MinecraftForge/MinecraftForge/blob/3c3f496015632ab84560be77a57cc57ac42c49e1/src/main/java/net/minecraftforge/common/crafting/IIngredientSerializer.java) | One small Forge-only adapter, own code; JSON/network delegate to existing common physical validation/minimum semantics. NBT-sensitive matching requires isSimplefalse and honest real display candidates. |
| [`CraftingHelper`](https://github.com/MinecraftForge/MinecraftForge/blob/3c3f496015632ab84560be77a57cc57ac42c49e1/src/main/java/net/minecraftforge/common/crafting/CraftingHelper.java), [`RegisterEvent`](https://github.com/MinecraftForge/MinecraftForge/blob/3c3f496015632ab84560be77a57cc57ac42c49e1/src/main/java/net/minecraftforge/registries/RegisterEvent.java) and [official ingredient documentation](https://docs.minecraftforge.net/en/1.20.x/resources/server/recipes/ingredients/) | Register before recipe load/network use on the MOD bus or appropriately enqueued setup. The existing Forge-bus native command subscriber is not a registration hook; CraftingHelper registration is not thread-safe. |
| Forge strict/partial NBT ingredients and official composition contracts | Static item/NBT templates do not faithfully delegate valid untagged defaults, higher capacity and malformed-state refusal to Grindless's live decoder. A bare-item OR partial-NBT workaround remains invalid. |
| [Vanilla crafting mechanics](https://minecraft.wiki/w/Crafting) | Shape equivalence includes trimming/placement and horizontal mirroring, not arbitrary rotations. Exhaustive source tests compare actual ingredient identities/overlap, not pattern letters; one actual RecipeManager/native proof validates the new binding. |

Only RelayF1-F3 is currently registered. A model architecture enum is not a
registered item; native/display cases must never invent Integrated or other
items. Use real item/spec/save factories and FluxTier constants, not hand-guessed
numeric ordinals from a report. Item-instance decoding plus minimum comparison
does not itself prove registry membership; validate the actual registered
identity before acceptance. Preserve all currently valid whole-number numeric
NBT types rather than replacing the shared decoder with an IntTag-only rule.

Research scope is deliberately qualified: several API/built-in files were read
directly, but Intersection/Difference behavior came from official docs; only
two recipe JSONs were spot-checked, not the entire catalogue. The initial
headline's broader read claim, incomplete event-bus suggestion and raw numeric
fixture example are corrected, not promoted to implementation authority.
JEI visibility is an API-contract expectation, not an actual rendered-client
pass. Implementers still validate every affected actual recipe and exact-source
compiler/loader behavior; no new per-recipe native matrix follows.

## Licence and evidence boundaries

GT Modern's LGPL-3.0 source and Immersive Engineering's custom visible-source
licence have different reuse/redistribution consequences. This study imports
nothing and does not replace the notices/provenance required before any import.
Source facts are separated by exact revision; indexed newer-branch snippets are
not promoted to an older pinned Forge API.

The references support the manual-bootstrap/automatic-component direction.
Actual batches, time/FU, machine ownership, transaction/persistence semantics,
source classification and tests come from Grindless's live authorities.
