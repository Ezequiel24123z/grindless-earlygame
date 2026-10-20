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

Independent source review found a synchronous invalidation race in source164:
`current()` could capture owner A, invoke a callback that replaced it with B,
then return A while a second face query validated B. The follow-up fences
reentrant access closed, resolves identity again after callbacks, rejects that
operation if its binding/revision changed, and uses one validated binding/face
for each item/fluid permission or mutation. All invalidators are attempted, with
the original unexpected exception retained. A callback cannot write the retired
controller or make a conduit debit fluid that the live controller never received.
The real native probe injects same-position replacement during both input and
output invalidation, checks a real Clay Conduit keeps its 50 mB pulse on rejection,
then verifies fresh access moves only the new owner's stock. This is an authored
regression until the coordinator executes the combined jar.

Fluid input follows the existing controller acceptance/batch limit. Fluid output
exposes only `FluidOutputQuota` that is not reserved for the primary recipe.
The same pure recipe selection as the native process is used, not the family's
preload allowance. A completed batch with no items must expose its 750 mB.
Putting the next dirt input in a Trough with only that recovered output must not
hide it forever while waiting for another 250 mB. A genuinely selected ready or
already-paid owner still reserves its primary volume. This read-only selection
never mutates the recipe cache, saved work or subscriptions.
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
original role overlays and rotate through four facing-only
states. No port has status, emitted light, particles or a working sound.
Side markings retain full UVs. The original two-unit-high rear bed edge is only
a secondary marker, not the primary role explanation. Each channel/screen also
has one UP label on the existing bed, from `(3.01,4.01,0.01)` to
`(12.99,4.01,6.99)`, before the screen bars and inside the rails. Its approved
`[2,3,14,10]` UV crop removes only the four original PNGs' transparent margin,
retaining every glyph/arrow pixel. The five-pixel glyph projects to at least four
native model units instead of below one on the rear strip. This adds no solid
geometry, collision, raised bulk, new texture or upward automation face.
Actual direction/role recognition, especially from behind, remains human visual
review; the technical label-geometry proof is not artistic approval.
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

The same existing smoke carrier also invokes the console/permission-4-only
`grindless_workshop_checks 2 280 13` after setup. Its one Forge
`WorkshopInterfaceChecks` class uses a dedicated non-OP `FakePlayer`, actual
registered tank/controller/port entities and actual item/fluid capabilities.
Bucket actions cross `BlockState.use` and the packaged `@ExpectPlatform` binder.
Only passed predicates log the nine `WORKSHOP-INTERFACES-*` markers: whole
fill/empty, 500 mB refusal, simulation/indices/hot/tag/native-gas handling,
creative and full-inventory delivery, actual filled drop and `BlockItem.useOn`
replacement, native quota/alias bounds, same-position controller replacement,
retired handler/access/batch rejection, synchronous callback/conduit refund and
fresh rebinding.

The probe refuses nonempty/unloaded/out-of-border reserved cells, foreign item
entities in its drop area, OP/online-player profile reuse, disabled tile drops or
denied normal `mayInteract`. It never grants permissions, force-loads chunks or
edits a real player inventory. Its profile is exclusively this probe; failure
logs and rethrows, leaves owned blocks as evidence, and clears only its owned
fake inventory. Repetition requires resetting the disposable region, not
reusing a successful marker. `setRemoved` is tested with live produced quota,
but a real chunk unload/reload is **not** claimed. No second runner or GameTest
framework is introduced, and the native command has not been executed here.
The existing paired batch runners explicitly exclude `workshop-ports`: foreign
item drops and overlapping scheduled old fixtures must not contaminate its
preflight/custody predicates. Use the ordinary isolated runner for this one boot;
the no-server batch contracts cover that refusal/skip behavior in both twins.
The first full tooling invocation actually failed: 18 cases passed, the first
new port case hit its 15-second child limit, and the unchanged 120-second suite
deadline left 33/51 cases failed with 565 assertions attempted. That result is
not called flaky, environmental or green. The existing runner now accepts a
case filter without changing either deadline or the default full selection.
The targeted `-CaseFilter 'workshop-ports*'` invocation passes all three new
cases and 98 assertions, three real Bash CLI runs and three PowerShell reference
comparisons with safe fake boot targets. It is not a fresh full-suite pass.
The old 48-case source-bound proof plus unchanged old cases/runner branches is
reused under the coordinator's risk-based policy. No JVM, server or PC was used
by these tooling probes; both owned temporary fixture roots were cleaned.

Actual cheap checks on the port source: `git diff --check`, language/manifest/six
recipe JSON parsing, and 730 relative links / 160 ADR sections and index rows.
The first tank commit attempt failed because Git had no author configured; the
retry used the repository-specified personal author through transient `git -c`,
without changing global identity or credentials.

The coordinator separately reports the actual early source759 proof on its
isolated integration265: common/Forge/classpath exit0, `VerifyBasicTank`120/0 and
`VerifyFluid`285/0. That worker later removed its newly owned validation root;
do not assume it remains available or resume it as a retained execution surface.
At that receipt's close the original clone was clean and Java count was zero;
this is not an observation of later PC activity. Those results
precede the creative-delivery correction and all port/native-limit changes:
they are not passing evidence for this later source or actual Forge container use.

| Category | Current bounded checkpoint |
| --- | --- |
| COMPLETED | Tank7594608 and ports164ee9a; follow-up source fixes for callback rebinding and produced-only selection; real-API native probe and UP-label/model contracts authored; 730 links/160 ADR rows, JSON, whitespace, 23 marker bindings and three new batch contracts/98 assertions pass. |
| BLOCKED | This worker has no JVM/PC execution lease. Causal telemetry and retired paid Guide hooks require the coordinator's real shop/DevTelemetry and quest/LegacyGuideAssembly integrations, not shims. |
| NOT EXECUTED | Common/Forge compilation, Java/model checks, integrated ordinary asset generation, actual workshop-ports smoke and retained-capability/bucket binder predicates on this source; a real chunk unload/reload remains separate. |
| NEEDS HUMAN VALIDATION | Actual client container use, readability and retained-world interaction. |
| NEEDS MY DECISION | None; fixed port positions/caps are approved by the coordinator. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None. |
| COST / BILLING RISK | None; no generation, new dependency or paid request. |
| DEPLOYED STATE | Unchanged; this worker does not publish or deploy. |
