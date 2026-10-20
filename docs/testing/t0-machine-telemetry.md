# T0 in-world machine inspection

## Source and ownership

This lane starts from clean `b8bd5e885aef598bf888d7e390b297a34cbb7d52`.
The owner requests an optional Jade/WAILA-equivalent overlay, accurate native
observations and no new execution engine or saved `MachineStatus` values.
The parent owns the optional-mod selection, dependency pins, root ADRs, route
map, release evidence and deployment. This lane owns the inspection/query,
transport and presentation plus focused checks. It does not own milestone
analytics, quest progression, fluid execution or multiblock formation.

The existing qualified747 installation and its recorded proofs are unchanged.
No PC execution inventory, shared Gradle cache, Prism instance or saved world
has been used by this lane.

## Baseline evidence inventory

| Surface at the baseline | Actual authority | Inspection consequence |
| --- | --- | --- |
| Optional in-world provider | No Jade/WAILA/TOP provider or dependency in common/Forge source | Integration is new, not an existing pass |
| Process machines | `ProcessMachineBlockEntity`, `ProcessMachineKind`, `ProcessRecipe` and `ProcessLookup` | Reuse the current cached recipe and native fractional work; never invoke recipe selection or a subscription from the query |
| Existing block-state feedback | Five `MachineStatus` values; process `STARVED` combines missing inputs and power | Preserve the ordinals; do not invent a precise cause from this combined state |
| Structure/site/conditions | `WorkshopStructure`, `SourceSite`, `ConditionFault` | Use their faults and localized recovery instructions, not a second validator |
| Energy | `MachineBlockEntity.energy()` | Actual local buffer and capacity; not a claim about network availability or actual draw this tick |
| Primary process fluid | `contents()`, `capacity()`, produced-fluid quota | Actual identity/amount/conditions; distinguish the produced share from freely extractable output |
| Basic Tank | Public `contents()` and `capacity()` | Report storage, not activity inferred from its legacy `RUNNING` block state |
| Clay Conduit | Public `contents()` and `capacity()`; its status may mean merely nonempty | Describe its buffer; do not claim a measured transfer rate |
| Hand Pump | Public fluid state; `pumpStatus(false, ...)` uniquely means no source | Show the native source-placement requirement; it is unpowered, not a crank consumer |
| Hand Crank Dynamo | Public `charge()`/energy and `CHARGE_PER_CRANK` | Show the actual energy buffer and fixed action charge |
| Research Terminal | `ResearchLogic` and a private native double | A minimal read-only progress getter is assigned to the shop/causal-telemetry author |
| Crude Extractor | `ExtractorLogic` and a private native double | This lane adds only its read-only progress getter; it does not regenerate veins or read world `SavedData` |
| Thermal/Biomass generators | Exact burn work/duration in the native menu data | Use only valid native bounds; the menu's synthetic one-tick zero-duration guard is not a fuel cycle |
| Ports | Infrastructure lane supplies the actual role/owner/facing/formation contract | Consume that API after its checkpoint; no inferred confirmed ownership |
| Client appearance | Existing Minecraft-font GUI/JEI and shared resource names | Reuse these names, text and non-colour status symbols; no new font or generated art |

## Public read-only contract

`common/.../inspection/MachineInspectionSnapshot` is an immutable observation
with primitives, enums and copied bounded lists. It contains no world, block
entity, `ItemStack`, mutable NBT, capability or execution callback.
`MachineInspection.query(ServerLevel, BlockPos)` admits only a loaded aimed
target in the Grindless namespace. It has no persistent world references,
global scan, ticking registration or chunk-loading fallback.

The bounds are 16 actual item slots, four fluid stores, eight resources per
process side and eight recognised ports. The producer marks truncation; the
codec rejects oversized/untyped/missing/unknown fields rather than converting
them into a successful empty/idle observation. Optional values remain absent
when their authority is absent. Targets carry dimension, position and block
identity so a client cannot display a response for a different target.

The richer state vocabulary is display-only. `NO_FLUID` and `OUTPUT_FULL`
must not be selected merely because a generic reported state looks similar.
Combined process starvation remains explicitly ambiguous. Actual native
fractional work is converted to percent; nominal cycle duration and nominal
work remaining are **not a wall-clock ETA**. Neither local stored energy nor a
recipe's nominal FU/t is measured network satisfaction or actual consumption.

The localized presentation separates stored contents, recipe requirements,
outputs, unconsumed catalysts and discharged fluid. A held machine stays held;
the overlay never repairs, supplies, forms or wakes it. Shift expands useful
recipe details. Developer-only identities and source ticks require a separate
opt-in setting, not merely Shift.

## Native equivalent and enable method

This is **Grindless native inspection**, not a Jade plugin. It is off by
default. In a client, use **Controls -> Grindless -> Toggle native machine
inspection** (default **F8**) or set `machineInspection.enabled = true` in
`config\grindless-inspection-client.toml`. The key persists that client setting.
Look at a loaded Grindless block within the ordinary aim range, capped at six
blocks. Hold Shift for requirements/conditions. When details need another page,
hold Shift and press the same configured toggle key; without Shift it toggles
the panel. Native text is drawn with Minecraft's current font, explicit
`[>]`/`[!]`/`[?]` symbols and labels, not colour-only feedback.

`machineInspection.developer` separately opts in to internal recipe IDs and
observation ticks. `allowWithOtherOverlays` defaults false: Jade, WAILA/WTHIT
and The One Probe suppress this panel to avoid a duplicate HUD. No classes or
APIs from those mods are referenced. This does not install/configure an overlay
mod or make deferred Jade redistribution part of the gate.

The Forge channel uses the existing `SimpleChannel` pattern. Missing peers are
accepted; the client checks channel presence before sending. It sends at most
one aimed-target request per ten client ticks, keeps at most one request in
flight and expires observations/requests after 40 ticks. The server admits at
most one query per sender per 100 ms before any world query. It checks loaded
target, exact six-block distance to the target's nearest point and a server-side
outline ray whose hit must equal the requested block. Four bounded load
predicates cover that ray before clipping: no chunk retrieval, enumeration or
loading is used. Replies go only to the sender.

Requests are at most 13 bytes; replies at most 32 KiB, with independent
schema/list/text/type bounds. Malformed structured observations become an
explicit unavailable/error presentation, never idle or old successful data.
Raw malformed framing is rejected by the normal network decoder. Nonces,
dimension/position/block identity and a weak client-world scope reject stale
or wrong-target responses. Disconnect/respawn/shutdown clear state. Server
rate ownership has weak sender keys and is cleared on server stop; no player
identifiers, analytics, world references or history are retained in a DTO.

## Execution record

At the initial source checkpoint `a7ef36d`:

- DTO, strict versioned native-NBT codec and synthetic contract cases are
  authored. Cases cover immutability, boundaries, native fractional work,
  binary round-trip, target identity, absent authorities and malformed data.
- Source inspection confirms the baseline and authorities listed above.
- Existing `tools/check-links.ps1 -Root .` passes 728 relative links and all
  160 ADR index/section pairs. `git diff --check` passes; native PowerShell
  parses the locale JSON and finds all 82 new inspection keys. These checks
  start no Java process and do not count as compilation or behavior proof.
- Java/Forge compilation and the executable contract suite are **NOT EXECUTED**.
  The process/structure and Research Terminal getters are assigned dependencies;
  no unavailable method or unrun test is reported as a successful compile.
- The parent verifies Jade Forge 11.13.3 at
  `d7a9e1d3b782f2ea4c387c66e973cc46b9768270`, CurseForge file `8479276`.
  Its source is CC-BY-NC-SA-4.0. The subsequent parent decision selects an
  **original native opt-in equivalent** as the functional delivery. Jade
  compatibility is deferred; there is no new compile/runtime dependency, copied
  source/API/assets, shaded jar, reflective fallback or mandatory integration.
  A historical pack pin is not permission to distribute or install Jade.
  The native query/reply will follow the existing Forge `SimpleChannel` pattern
  and use the incumbent Architectury client HUD/tick events and Minecraft font.

The native presentation/network checkpoint additionally authors fixed packet
budgets, all-maxima transport, exact range/rate boundaries, no-prediction cache
and expiry/reset/wrong-target tests. Source binding guards cover the real
loaded-before-read checks, main-thread dispatch, explicit constructor hook,
client-only registration, optional absence, font/pagination and default-off
developer/mutual-exclusion behavior. These Java cases are **NOT EXECUTED** until
an assigned compilation surface is supplied.
Its existing documentation check again passes all 728 links/160 ADR pairs;
diff integrity passes, and native PowerShell parses all 101 inspection locale
keys and resolves all 43 exact UI key references. No Java is started for these
checks. The enum-derived locale closure remains in the unexecuted Java suite.

The smallest subsequent executable proof is common/Forge compilation plus
`VerifyMachineInspection` on the combined source. The new native loader/channel
binding needs one representative production boot **without Jade installed**.
There is no Jade-present build/install gate. Existing machine
execution/persistence proofs remain applicable to unchanged engines; they do
not prove this new transport/registration.

## Client proof still required

Use the eventual exact packaged revision. Observe one process at fractional
work, blocked output, ambiguous starvation, invalid site and invalid structure;
compare the actual inventory, energy and fluid quantities. Inspect an empty and
nonempty tank, a generator fuel cycle, a Research Terminal and Crude Extractor.
Inspect a formed port and an orphan/candidate, including chunk boundaries.
Change targets/dimensions and replace the target while data is in flight.
Check absent/malformed data, Shift details, developer opt-in, long translations,
GUI scale, readable font/icons and no dependence on colour.

These are unexecuted observations, not a claimed rendered/client, no-command,
multiplayer or retained-world acceptance.

## Handoff categories

| Category | Native presentation checkpoint |
| --- | --- |
| COMPLETED | Baseline/native-authority inventory; immutable DTO/query/codec; original opt-in native channel and font/paged HUD; localized text and authored synthetic/range/rate/lifecycle/binding cases; cheap documentation/JSON/source-key checks |
| BLOCKED | The independently owned process getters/port checkpoint and a permitted compilation surface are not yet received; Research Terminal getter is preserved in shop source `6286dc3` |
| NOT EXECUTED | Java/Forge build, executable Java cases, new native-binding production boot without Jade, real rendering |
| NEEDS HUMAN VALIDATION | In-world readability, useful amount of detail, actual client interaction and the existing manual campaign |
| NEEDS MY DECISION | None in this worker lane; the parent owns dependency/licence selection |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | No credential is required; an explicit execution root/lease or parent-run build is required before PC/JVM validation |
| COST / BILLING RISK | None; no paid service, generation or external analytics |
| DEPLOYED STATE | Unchanged qualified747; this lane has not pushed, opened a PR or deployed |
