# T0 workshop fluid storage and interfaces

Source baseline: `b8bd5e885aef598bf888d7e390b297a34cbb7d52`.
Scope: the owner's T0 experience-polish request, Basic Tank and the two existing
fixed workshop structures only. This does not open T1 or the parametric multiblock
kernel. The coordinator owns the root documentation, ADR and final integration.

## Basic Tank audit and repair

The existing `grindless:basic_tank` already has the required storage engine:
`FluidBuffer.ambient(FluidLogic.TANK_CAPACITY)`, 16,000 mB, one substance,
`FluidEndpoint`, a single `NativeFluidStore` and saved `Fluid` NBT. Its native
temperature/pressure, exact withdrawals and receipt compensation are retained.
There is no replacement block, second tank implementation, capacity change or
world-data migration.

The audit found missing player/container interaction, no useful amount readout
and no fluid-bearing ordinary drop. The Forge adapter also treated hot lava as
ambient, accepted invalid tank indexes and allowed retained handlers to reach
removed block entities. A Forge drain of an unregistered native gas could consume
fluid while returning an empty Forge stack. These are correctness repairs, not
new fluid machinery.

| Surface | Contract |
| --- | --- |
| Native sides | All six sides accept compatible ambient fluid and allow extraction; unsided native access retains its existing meaning. |
| Ratings | 40 C maximum, 0.1 MPa; reject mixed substances, invalid thermodynamic values and insertion above capacity. |
| Forge import | Use the registered fluid type's intrinsic temperature, at least 20 C. Reject tagged fluid because `FluidState` cannot preserve its tag. Never turn lava into cold water-compatible stock. |
| Forge export | Only registered representable fluids; an unknown native gas remains in its owner instead of being drained into an empty result. |
| Simulation | The same native acceptance as execution, no mutations, zero capacity/contents for invalid tank indexes or stale handlers. |
| Containers | Stage a real `IFluidHandlerItem` on a one-item copy. Probe the exact accepted drain again so an indivisible bucket cannot lose its last fraction. Settle native storage and vanilla player inventory through the existing native receipt. |
| Inventory | A stacked container needs room for its result; otherwise nothing transfers. No result is silently discarded or dropped. Creative mode retains Forge's no-item-consumption semantics. |
| Exceptions | Preserve the original synchronous exception and restore native metadata and player inventory. Foreign handler world side effects and VM/crash atomicity are not claimed. |
| Feedback | Server-authoritative contents and capacity on use, and the same contents on the preserved item's tooltip. An unrepresentable/native fluid is named honestly by its ID. |
| Break | A nonempty tank emits one NBT-bearing tank from removal and suppresses the ordinary empty loot item. This also covers creative breaking and explosion removal. Empty tanks retain ordinary loot rules. Explicit administrative drop suppression remains a vanilla rule. |
| Rotate | Same block identity, same block entity and same fluid. No removal drop is emitted. |
| Relocate | Existing Multitool `BlockEntityTag` custody; `Relocation.active()` prevents a second drop. Placing the item loads the unchanged `Fluid` schema. |
| Unload/removal | Retained endpoint/capability access cannot operate a removed, unloaded or replaced block entity; no chunk is force-loaded. |
| Legacy | Missing thermal fields remain ambient. Existing overcapacity NBT is preserved and drainable, never silently clipped; further insertion is refused until it fits. |

The Forge helper's established item-copy pattern is used through its public API;
its unchecked two-handler transfer is not copied as a rollback guarantee.
Reference: [Forge 1.20.x FluidUtil](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/src/main/java/net/minecraftforge/fluids/FluidUtil.java)
(LGPL-2.1-only, conceptual/API reference; no imported source).

## Approved fixed-structure port boundary

The coordinator approves two replaceable Firing Chamber cells `(1,0,0)` and
`(1,1,0)`, at most one item input and one item output, leaving at least five
ordinary bricks. The old controller plus seven bricks remains valid.

The Settling Channel cell `(0,0,1)` can instead be an item-input or fluid-input
channel. The Settling Screen cell `(0,0,2)` can instead be an item-output or
fluid-output screen. Each keeps its original channel/screen geometry, facing
and clear air above. At most two ports are present, and the ordinary channel
plus screen remains valid. Direct controller interfaces remain available.

Four roles and six geometry-preserving variants are sufficient. No independent
port inventory/tank, energy hatch, maintenance/control block, cross-dimension
link, force-loaded owner or generic casing kernel is introduced. Runtime
implementation and exact machine-readable contract follow in the port checkpoint.

## Evidence and handoff

`VerifyBasicTank` exercises actual `BasicTankBlockEntity` storage and existing
`NativeFluidStore` settlement, including all sides, limits, full-state NBT,
legacy excess, exact/partial exchanges, stale access and exception restoration.
It does not use a boolean fake store as evidence of transfer.

| Category | Current bounded checkpoint |
| --- | --- |
| COMPLETED | Source audit; compatible native/container/drop/capability repair and focused executable checks authored. |
| BLOCKED | No assigned JVM/PC execution lease yet. |
| NOT EXECUTED | Common/Forge compilation, executable Java checks and the changed registered/native bindings. |
| NEEDS HUMAN VALIDATION | Actual client container use, readability and retained-world interaction. |
| NEEDS MY DECISION | None; fixed port positions/caps are approved by the coordinator. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None. |
| COST / BILLING RISK | None; no generation, new dependency or paid request. |
| DEPLOYED STATE | Unchanged; this worker does not publish or deploy. |
