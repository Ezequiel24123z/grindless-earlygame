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
| Inventory | A stacked container needs room for its result; otherwise nothing transfers. No result is silently discarded or dropped. Creative input retains its source item, but extraction always delivers the filled result through normal hand/stack staging. |
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
Forge's creative fill-and-stow branch deliberately drains the source while
leaving the original empty item unchanged. The first tank checkpoint reproduced
that behavior; the reviewed correction intentionally does not. Creative extraction
must acquire an actual filled container or transfer nothing, including stacked
buckets/full inventories. `TankContainerDelivery` stages the existing vanilla
inventory in both modes and is checked with real native tank settlement.

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
link, force-loaded owner or generic casing kernel is introduced. The
[checked manifest](t0-workshop-interfaces.json) projects the single
`WorkshopPortContract` authority; it is not a second gameplay catalogue.

| Variant | Replaces | Role | Exposed horizontal faces for a north-facing controller |
| --- | --- | --- | --- |
| `workshop_item_input` | One approved front-right brick | Item input | North and east |
| `workshop_item_output` | One approved front-right brick | Item output | North and east |
| `settling_item_input` | Channel | Item input | East and west |
| `settling_fluid_input` | Channel | Fluid input | East and west |
| `settling_item_output` | Screen | Item output | East, west and south |
| `settling_fluid_output` | Screen | Produced-fluid output | East, west and south |

All faces rotate with the controller. Unsided and vertical port automation is
closed; the unchanged controller top/bottom and direct interfaces remain valid.
Use the existing T0 manipulator or compatible sided pipes for item ports.
An output-fluid screen pushes at most 50 mB per four ticks in total through its
exposed faces into existing native conduits/tanks. It never pushes an input batch
or invents a foreign-handler rollback promise.

### Ownership, lifecycle and native custody

`WorkshopPortBlockEntity` is a view of one current loaded
`ProcessMachineBlockEntity`, not an inventory/tank owner. Every access derives the
finite candidate controller positions from the approved slot contract, verifies
the actual block-entity identity, orientation and complete current structure,
and rejects multiple candidates. Every other recognised port must also uniquely
belong to that controller. The controller's own work/formation check rejects
ambiguous or missing port entities before consuming power or inputs. These are
structural owners, not player-security or UUID permissions.

The weak cached owner is transient and is not trusted by the next operation.
Removal, load, rotation, neighbour change and bounded server polling retire
capability revisions. Forge providers use both event invalidation and the common
callback. Each retained handler captures a revision and checks it again after
fresh face validation, so replacing a controller at the same coordinates cannot
rebind an old handler. New requests can acquire a new view after repair.
Old native plans also capture `nativeAccessRevision()` and actual store identity.

Fluid input follows the existing controller acceptance/batch limit. Fluid output
exposes only `FluidOutputQuota` that is not reserved for the primary recipe.
Its `nativeStore()` is the actual controller store. The existing
`NativeFluidStore.Access` gains a rechecked role-visible limit while preserving its
two-argument constructor. A single backing store is counted once across faces or
proxies; aliases expose the greatest legitimate access, never the sum of quotas.
Planning 751 mB from a 750 mB output view must fail even when 1,750 mB exists in
the shared backing buffer.

Native screen-to-conduit transfer reuses the same
[ADR-0138 receipts](../DECISIONS.md#adr-0138--a-batch-completes-only-after-its-fluid-inputs-are-re-verified-and-drawn-exactly):
retain destination/source native snapshots and quotas, withdraw exactly, silently
offer the admitted amount, compensate synchronous exceptions, and notify only
after settlement. There is no duplicate `NativeFluidStore`, temporary tank engine,
cross-tick escrow, replacement save format or arbitrary external-store guarantee.

Ports save no contents, owner link or formed flag. Breaking or relocating one
cannot drop or copy the controller's inventory/fluid. The old controller inventory
layout, `Fluid`, `FluidOutputQuota`, `WorkRecipe`, process and native-store identity
contracts are unchanged. No accepted old default structure needs migration.

### Read-only telemetry and resources

`ProcessMachineBlockEntity.inspectionStructure()` returns a fresh
`WorkshopStructure.Inspection(fault, offset, ports)` with the first exact rotated
world-relative fault cell. Each port is a `WorkshopStructure.Port(offset, kind)`;
the kind exposes the canonical role/family. The six scalar/cached inspection
getters never resolve recipes, alter subscriptions or reuse a stale generation.
`WorkshopPortBlockEntity.inspection()` returns confirmed owner position, role,
facing, fault and formation; an unconfirmed candidate is never presented as owner.

Six recipe entries reuse the existing shaped-craft, Bench and Assembler projections.
They are optional infrastructure, not mandatory progression bypasses or T1 work.
Models retain brick/channel/screen material and collision families, share four
original role overlays, use full overlay UVs and rotate through four facing-only
states. No port has status, emitted light, particles or a working sound.
The ordinary paired asset generators include the pure role contract dependencies.
The visual lane supplies the four noticed source PNGs separately; the integrated
ordinary generation and final resource equality still require execution.

The coordinator additionally requests the narrow paid-Guide migration seam while
this worker owns the process file. The real
`LegacyGuideAssembly` helper from quest checkpoint
`07ff3972eeec197a77014bd9580daaf4a3e5381d` is consulted only for an exact saved
Bench/Assembler owner with positive paid work and matching physical inputs.
After one successful settlement, progress and both selection IDs are cleared
inside the existing compensated completion. No retired row is reintroduced to
the graph, recipe viewer or fresh jobs. `VerifyLegacyGuideMachine` checks real
process partial work, blocked outputs, exactly one output and cleared oversized
progress using an isolated headless item-ID fixture; native deprecated-book use
remains the quest lane's responsibility. This worker does not copy that helper
or alter the quest-owned retirement catalogue.

## Evidence and handoff

`VerifyBasicTank` exercises actual `BasicTankBlockEntity` storage and existing
`NativeFluidStore` settlement, including all sides, limits, full-state NBT,
legacy excess, exact/partial exchanges, stale access and exception restoration.
It does not use a boolean fake store as evidence of transfer.

`VerifyWorkshopPorts` exhausts rotations, compatible/incompatible cells, role
counts and precise faults; checks the manifest against the runtime contract; uses
real process and tank block entities/native stores for quota-plus-one, alias,
primary reservation, changed bound/revision, same-position replacement, partial
transfer and exception restoration; and checks an actual orphan port entity's
absence of saved storage/formation. Its quota-only fixture is a policy facade over
the actual process owner, not a loaded-world ownership or Forge-capability proof.
Real controller discovery/chunk boundaries and retained Forge handler behavior
still need the combined native qualification. Do not relabel these Java cases
as a graphical/client pass.

The focused `workshop-ports` smoke is a controlled paid-work fixture: one Trough
batch receives its actual 1,000 mB through an input channel and exports exactly
750 mB through an output screen/conduit, while an invalid facing first holds the
source. The Firing fixture receives unfired ceramic/charcoal through an item port
and transfers real insulators/grog through an output port using ordinary T0 arms.
The same boot checks a port break retains controller stock and a filled Basic
Tank rotation/removal produces one metadata-bearing item. It does not claim
power generation, paid-work earning, player bucket clicks, Multitool clicks,
cross-chunk retained handlers or the human campaign. The two completed-work
setups are explicit fixture inputs, never accepted gameplay.

Actual cheap checks on the port source: `git diff --check`, language/manifest/six
recipe JSON parsing, and 730 relative links / 160 ADR sections and index rows.
The first tank commit attempt failed because Git had no author configured; the
retry used the repository-specified personal author through transient `git -c`,
without changing global identity or credentials.

The coordinator separately reports the actual early source759 proof on its
isolated integration265: common/Forge/classpath exit0, `VerifyBasicTank`120/0 and
`VerifyFluid`285/0. That worker later removed its newly owned validation root;
do not assume it remains available or resume it as a retained execution surface.
The original clone remains clean and no Java process is running. Those results
precede the creative-delivery correction and all port/native-limit changes:
they are not passing evidence for this later source or actual Forge container use.

| Category | Current bounded checkpoint |
| --- | --- |
| COMPLETED | Compatible tank source checkpoint7594608; fixed T0 role/geometry contract, controller-backed proxies, narrow native quota bound, Forge role/revision adapters, recipes, generator hooks, telemetry getters/hooks and focused executable checks authored. |
| BLOCKED | This worker has no JVM/PC execution lease. Causal telemetry and retired paid Guide hooks require the coordinator's real shop/DevTelemetry and quest/LegacyGuideAssembly integrations, not shims. |
| NOT EXECUTED | Common/Forge compilation, executable Java checks, integrated ordinary asset generation, loaded-world ownership/retained-capability regressions and the changed native bindings on this source. |
| NEEDS HUMAN VALIDATION | Actual client container use, readability and retained-world interaction. |
| NEEDS MY DECISION | None; fixed port positions/caps are approved by the coordinator. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None. |
| COST / BILLING RISK | None; no generation, new dependency or paid request. |
| DEPLOYED STATE | Unchanged; this worker does not publish or deploy. |
