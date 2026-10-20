# T0 recovery barter and development measurements

Source baseline: `b8bd5e885aef598bf888d7e390b297a34cbb7d52`.
This is a bounded implementation lane, not T0 acceptance or measured balance.
The coordinator owns the integration ADR, root documentation and final qualification.

## Exchange contract

The shop supports progression without replacing production. There is no currency
item, mint, wallet, balance, NPC, village, rare structure or new claim ledger.
`ShopCatalogue` is the sole authority for offers, exact payments, batches, roles,
quest prerequisites, surplus provenance and progression risks.

The normal surplus lane uses untagged mineral aggregate, biomass, fibreboard or ceramic grog.
These are real existing production outputs, not credits created by opening a
screen. Normal water refill additionally consumes one existing empty bucket. The old
generous starter allocation, quest IDs, rewards and claim records remain unchanged.
Buying a recovery item is never required for a machine recipe or T0 completion.

Three explicit **manual finite compatibility** restart offers additionally accept
ordinary dirt dug by hand in Superflat. They require only the already-claimed
starter quest, not industrial stock, a deposit, a water supply or a lost machine.
They exchange 32 dirt for 8 cobblestone, 32 dirt for 4 oak logs, or 64 dirt for one
filled replacement bucket. The last offer deliberately sells a real container:
it is not the refill offer and is fully paid, not a new free claim. Repeating it
costs the complete dirt batch each time. This prevents the pre-producer water/
wood/stone cut from depending circularly on an already-working factory.

Manual terrain digging is the finite, laborious **initial acquisition** fallback,
not the canonical unlimited mod-owned supply. Dirt itself also has the implemented
mod-owned renewable `bootstrap/wet_aggregate` route; these same repeatable paid
offers accept that dirt after the factory is established. Initial provenance
does not make all dirt or offer repeatability intrinsically finite forever.
They remain optional recovery, not a canonical replacement for production.
Native aggregate/fibreboard production is far more efficient. The potential check includes the returned
empty bucket, its iron crafting cost, wood/plank/stick and other relevant returns;
no T0 bucket-to-iron reverse recipe is invented.

No machine, insulator, Data Core, Calibrated Data Core, Relay Matrix or advanced
component is sold. Grog remains a useful ceramic feedstock; optional paid recovery
does not reclassify slag or invent a required waste consumer. Saplings and seeds
are optional vanilla compatibility, never the canonical renewable factory.

Only the 36 native main-inventory slots participate. Stage every payment and the
entire legal output using vanilla stacking before mutation. Reject missing
payment, full output, malformed goods or changed custody without dropping items,
partial debit or partial delivery. Do not consume named/tagged products, equipment,
offhand items or the cursor. Settlement runs on the server thread. Native inventory
settlement is not an external-capability transaction or a crash-atomic disk journal.

Menu identity, a transient per-opening token and a monotonic action sequence reject
stale/replayed requests, including a reused container ID after reopening. The
server resolves prices and goods; a request never supplies an item stack or price.
One admitted action pays one full batch. A short per-menu action interval prevents
click spam; it is neither a stock limit nor another persisted per-player ledger.

## Old/new balance ledger

All production rates, recipe quantities and starter/reward quantities remain
unchanged. There is no measured basis for blanket time inflation or an automatic
starter nerf. The new prices are deliberately conservative candidates for human
review, not a claim that T0 pacing has been validated.

| Surface | Old problem at baseline | New behavior | Intended effect and remaining risk |
| --- | --- | --- | --- |
| Recovery exchange | No shop/currency implementation | Direct in-kind recovery with small fully paid batches | Recover from local shortages using genuine surplus; usefulness/prices need play |
| Stone and sand | Recovering a shortage required returning to the native chain | Costlier convenience after the Quern prerequisite | Native dry processing remains more productive and keeps its coproducts |
| Ceramic recovery | Grog already has a valid body-reuse route | Optional small brick exchange after ceramics | No insulator/core bypass; grog-to-brick-to-grog consumes net payment |
| Food and fuel | Finite generous starter, native biocarbon route | Small paid food and one-charcoal emergency batches | No free periodic handout; native biocarbon remains eight times cheaper in biomass |
| Pre-producer total loss | All ordinary surplus payments themselves needed a working water/production base | Bare-hand dirt restart after the original starter claim | Paid wood/stone/one filled container before machines; not a free kit reset or permanent digging route |
| Water containers | Filled starter buckets and native pumping | Normal aggregate refill retains a container; separate costly dirt restart can replace a lost one | Both are fully paid; native pumping remains canonical |
| Trees, crops, terrain | Optional vanilla acquisition | Paid sapling/seed/landscaping access | No village/mob dependency or compulsory farming |
| Glass | First glass belongs to production/calibration | Decorative convenience only after calibration | Cannot bypass the first physical core/calibration |
| Critical production | Unmeasured rates already implemented | Unchanged | Observe causal counters and human friction before changing batches/rates |
| Starter and quest claims | Existing generous one-time native batch and ledger | Unchanged | No silent loss of previously promised supplies or old progress |

## Composed exploit analysis

`EconomyGraph` projects complete T0 process and crafting batches directly from
`BootstrapProcesses`, `RenewableProcesses`, `BootstrapRecipes`, the physical Relay
recipe in `T1Recipes`, `ResearchLogic`, `ShopCatalogue` and all quest rewards.
It also includes every actual Assembly Bench row from the canonical generated
graph, not only its hand-crafting counterpart. This covers the Bench's additional
basic crafts and admits their real completion IDs to local telemetry without
inventing another copy of the Bench recipe parameters.
It includes the relevant wood/brick/glass/fuel/container compatibility conversions.

`EconomyCycleProof` checks an exact positive material-potential certificate over
every repeatable conversion. A non-increase per complete batch composes across
arbitrarily long cycles, mixed recipes and arbitrary integer batch counts; this is
not a buy/sell-pair heuristic. Unpriced or increasing rows fail closed as
**unproven**, not automatically as an exploitable cycle. Counterexamples cover a
three-leg cycle, mixed batches, an added conversion and overflow.

Environmental geological/biological sources are explicitly separate and require
their actual environment, water and paid work. Quest rewards each spend one finite
per-player/world claim; they cannot be replayed as an infinite conversion. The
proof's abstract units are not gameplay currency, physical mass, energy profit or
a proof about arbitrary installed mods/datapack changes. The canonical
sustainability auditor remains the authority for recursive renewable closure.

The shop does not create a recovery fund out of nothing. A player who has already
claimed and lost the entire bootstrap and all industry can gather ordinary
Superflat dirt by hand and restart. A regression spends exactly 160 dirt on
4 logs, 16 cobblestone and one filled bucket with only the old starter claim,
no industrial stock or machine evidence. No free reset or automatic regrant is
introduced. This source contract does not claim recovery in an arbitrary world
without accessible dirt, or replace the human Survival/recovery campaign.

## Access and anonymous local telemetry

`ShopNetwork.requestOpen()` is the single quest-client entry. The server requires
the real held Quest Book before opening its extended native menu. The explicit
`/grindless shop` utility opens the same menu without granting goods. There is
one new menu registration and screen factory; no loader-specific type enters
common. The quest owner integrates the legacy/Heracles button separately.

Development telemetry is **off by default** and never restarts automatically.
An operator uses `/grindless telemetry start`, `snapshot`, and `stop`. Stopping
flushes one final local log snapshot; a later explicit start creates fresh
counters. Every 1200 game ticks an enabled session emits bounded aggregate JSON.
No UUID, player name, per-player identity, coordinates, seed, world name, host,
credential, absolute timestamp, external request or external analytics is recorded.
The ordinary Minecraft logger may wrap a line in its standard local log prefix;
the telemetry payload itself contains only relative game ticks and catalogue IDs.

The counters distinguish successful process completion, manual actions and
accepted FU, paid shop transactions, rejected exchanges, recovery purchases and
published blocked-state transitions. A Forge-only event adapter also counts
uncancelled catalogued machine/part placement events at lowest event priority,
without reading the actor or position. These are placement observations, not a
unique-machine/world census: replacement/rebuilding counts again and a later
external cancellation/rollback is not claimed as a committed placement.
First quest-claim milestone times are
server-aggregate observations, not a per-player profile or evidence that item
possession proves operation. Unknown labels are never logged or retained; only a
numeric rejected-record count is incremented. Counter overflow is explicit.
There is no world/chunk scan or unbounded position map. Status observation occurs
only after an actual published state change, not every blocked tick.

The coordinator serializes the process-machine and quest success hooks with their
owners. This lane owns the Research Terminal's completion hook, the Dynamo's actual
accepted manual FU and the status publication hook. `inspectionProgress()` on the
Research Terminal exposes its existing raw `progress` read-only; it changes no
NBT, work, recipe, energy or completion behavior.

## Live progression-cost export

`ProgressionCostModel` reads the real Relay craft, Data Core craft and calibration
definitions. At the unchanged final manufacturing boundary, one five-Matrix batch
requires two glass, two copper ingots, three redstone and one ceramic insulator,
including its one physical core/calibration. Calibration is 600 rated work ticks
and 4800 FU from `ResearchLogic`, not an elapsed-time target. These are a material
lower bound at that boundary, **not** a complete raw-material or machine bill.

The complete machine/material/cycle view reuses the existing
`SustainabilityAudit.deriveEntryProfile(...).replay()` result and
`T0ConditionalGate`, rather than constructing another planner. It exports every
executed establishment batch, gross consumed/returned materials and water,
serial work ticks, FU demand, finite reserves/margins, retained circulation
inventory, actual machines/structural catalysts and entry assumptions.
Serial work is not wall-clock playtime; the conditional sufficient witness is
not a globally optimal bill or an observed route from the generous starter.
The current starter allocation is exported separately, without silently treating
all finite gifts as permanent renewable stock.

The paired exporter uses the existing Gradle-resolved check classpath and canonical
registry/source adapter:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\economy\export.ps1 -Root .
powershell -ExecutionPolicy Bypass -File .\tools\economy\export.ps1 -Root . -CheckOnly
```

`tools/economy/export.sh --root .` and `--check-only` are the POSIX twin.
The output is `docs/tiers/t0-economy.json`; generation must execute on the parent's
serialized JDK lease before the integrated `VerifyEconomy` gate. Do not hand-author
the JSON to satisfy equality. The current worker has not run this writer or
invented a generated output. Actual invocation/source/model hashes go to
`build/economy/provenance.json`, never a fabricated clean-publication receipt.

The document includes every offer's unlock, exact price, role, input classification,
canonical conditional renewable evidence, progression risk, quantity/capacity policy,
all projected exploit-graph edges, the full potential certificate, final-craft
minimum, complete existing conditional cost witness, old/new balance decisions,
and a Git-blob comparison against the exact baseline authorities. Unchanged
recipe/rate blobs are factual evidence; changed sources remain explicitly changed
rather than relabelled as an old/new measured balance result.

Each offer exports initial acquisition provenance separately from repeatability
and the actual canonical backing-input metadata. The manual restart classification
does not override dirt's later renewable declaration or declare this optional
exchange a newly accepted T0 production route.

## Focused qualification and human protocol

The existing behavior runner discovers `VerifyShop`, `VerifyDevTelemetry` and
`VerifyEconomy` automatically. The first covers every offer, partial/missing
payments, output capacity after payment, native stack limits, preserved legacy
stacks, tagged-item protection, changed snapshots, duplicate commits, reentrancy,
menu/token/sequence replay, sequence exhaustion, fixed packet framing, manual
pre-producer recovery and composed conversion counterexamples. It also checks
the source-side C2S/thread/book/ledger boundaries and localization. Those source
checks are not an observed live network or client proof.
The missing-second-payment regression explicitly binds the sorted payment order:
the first cost is fully staged before the second one fails. Invalid-output tests
likewise fund and stage a valid payment before rejecting the output registry or
native stack limit; they do not accidentally stop at an earlier invalid-input guard.

Telemetry tests cover default opt-out/NOP hooks, exact anonymous aggregates,
relative first milestones, immutable snapshots, bounded unknown labels, overflow
and absence of personal-data fields. Cost tests compare every cost row with the
actual canonical replay and require generated-data equality.

After combined compilation and a representative registration/command boot,
use an isolated controlled client to check the Quest Book action, all payment/
output details, keyboard navigation, a successful paid exchange, partial/full
inventory rejection, bucket replacement/refill, rapid repeated clicks, reopening/
reconnection and old claims. This does not prove the no-command campaign.

For the owner's longitudinal measurement, explicitly start telemetry immediately
before the first quest claim, note the tested source/artifact independently, and
record observed confusion, digging/recovery burden, manual repetition and workshop
downtime. Use snapshots at meaningful milestones, then stop. Interpret counters
as server aggregates and first observed claim times, not individualized sessions,
real-time durations, machine-operation certificates or human approval. Review
prices/batches only against that evidence; no starter nerf or time inflation is
part of this implementation.

## Implementation and evidence

The first checkpoint contains the offer authority, native all-or-nothing
settlement, transient menu sequencing and complete-batch cycle certificate.
The second adds the native menu, bounded C2S intent, client screen, commands,
manual pre-producer restart and anonymous opt-in telemetry with focused regressions.
The final source checkpoint adds live cost/economy projection, the paired canonical
exporter and its focused regression. New empty-material tutorial claims remain
finite graph rows and cannot break the projection or acquire a reward mint.

No PC/JVM lease is assigned to this worker. Java regressions and compilation are
prepared, **NOT EXECUTED**; the parent qualifies the frozen combined source with
the existing runtime wrapper. Native packet delivery, client ergonomics, actual
prices, recovery usefulness and pacing need their distinct proofs.

The coordinator has assigned a separate native executor (`0a0c3364`) to exact
`00b377bb2ec1eabaec7490089f7cf3b02e3027b5`: common/Forge compilation, the normal
economy writer/CheckOnly and the three focused Java suites, with no unrelated
suite replay. The verified incremental source bundle has 64617 bytes,
SHA-256 `0a4d9c41cba2c09ee3578ef59c338e174040c7513ec0adbbab976b566bfb372e`,
and only baseline `b8bd5e885aef598bf888d7e390b297a34cbb7d52` as a prerequisite.
A first raw-OID bundle command was refused as empty before producing a usable
bundle; checking the exact frozen HEAD and advertising that ref corrected the
transport without changing source. Assignment and successful bundle verification
are not a Java or generated-data pass. The worker starts no competing JVM.

The actual exact00 common compilation then failed at `ShopScreen.java:68`:
an extra closing parenthesis on the exchange button produced `';' expected`.
Its closed `daemon-29228.out.log` is 40700 bytes with SHA-256
`1b770e5a3f1213140a9d22987f61c4a9b7dbea8fda8671f281b77d8717bb31eb`.
A separate malformed native Gradle-property invocation remains preserved in
`daemon-29644.out.log`, 310782 bytes, SHA-256
`e6c899f8a9f381b62b7f303e99e77a01e56ab70e94e414177345bac73ac3d579`;
it never established a compiler/test pass. The syntax is corrected in this
source. The coordinator explicitly transferred the retained isolated worktree
and exclusive JVM/cache lease to this lane after observing zero Java processes.
Requalification includes the precise d916 tests; no old root, cache, failed log,
world or development instance is removed.

## Lane handoff categories

- `COMPLETED`: source implementation of the core, native shop/menu/network, paid pre-producer recovery, canonical cost/export lane and local telemetry above; not executable qualification.
- `BLOCKED`: no fundamental source/product blocker; actual qualification is pending the coordinator's assigned native executor. This worker has no competing host/JVM lease.
- `NOT EXECUTED`: Java compilation/regressions, actual economy JSON generation/check, server boot, native packets, client and campaign. Parent integrates the separately owned Quest Book and process/quest telemetry hooks before combined qualification.
- `NEEDS HUMAN VALIDATION`: prices, usefulness, pacing, clarity and interaction comfort.
- `NEEDS MY DECISION`: None; direct barter and preserving the generous starter are assigned decisions.
- `NEEDS CREDENTIAL / EXTERNAL RESOURCE`: None.
- `COST / BILLING RISK`: None; no paid generation or external analytics.
- `DEPLOYED STATE`: unchanged; this worker does not publish, deploy or open worlds.
