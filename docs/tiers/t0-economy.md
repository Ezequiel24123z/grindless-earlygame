# T0 recovery barter and development measurements

Source baseline: `b8bd5e885aef598bf888d7e390b297a34cbb7d52`.
This is a bounded implementation lane, not T0 acceptance or measured balance.
The coordinator owns the integration ADR, root documentation and final qualification.

## Exchange contract

The shop supports progression without replacing production. There is no currency
item, mint, wallet, balance, NPC, village, rare structure or new claim ledger.
`ShopCatalogue` is the sole authority for offers, exact payments, batches, roles,
quest prerequisites, surplus provenance and progression risks.

Pay with ordinary untagged mineral aggregate, biomass, fibreboard or ceramic grog.
These are real existing production outputs, not credits created by opening a
screen. Water refill additionally consumes one existing empty bucket. The old
generous starter allocation, quest IDs, rewards and claim records remain unchanged.
Buying a recovery item is never required for a machine recipe or T0 completion.

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
| Water containers | Filled starter buckets and native pumping | Refill a real empty bucket for aggregate | Never mint buckets; native pumping remains canonical |
| Trees, crops, terrain | Optional vanilla acquisition | Paid sapling/seed/landscaping access | No village/mob dependency or compulsory farming |
| Glass | First glass belongs to production/calibration | Decorative convenience only after calibration | Cannot bypass the first physical core/calibration |
| Critical production | Unmeasured rates already implemented | Unchanged | Observe causal counters and human friction before changing batches/rates |
| Starter and quest claims | Existing generous one-time native batch and ledger | Unchanged | No silent loss of previously promised supplies or old progress |

## Composed exploit analysis

`EconomyGraph` projects complete T0 process and crafting batches directly from
`BootstrapProcesses`, `RenewableProcesses`, `BootstrapRecipes`, the physical Relay
recipe in `T1Recipes`, `ResearchLogic`, `ShopCatalogue` and all quest rewards.
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
claimed and lost the entire bootstrap **and** all production/earned stock cannot
buy from an empty inventory. Existing source/claim recovery and any operator repair
are separate; no free reset, automatic regrant or hidden soft-lock guarantee is
invented by this lane.

## Implementation and evidence

The first checkpoint contains the offer authority, native all-or-nothing
settlement, transient menu sequencing and complete-batch cycle certificate with
focused Java regressions. Menu/network/client, live cost export and opt-in local
telemetry are the following implementation unit in this same lane.

No PC/JVM lease is assigned to this worker. Java regressions and compilation are
prepared, **NOT EXECUTED**; the parent qualifies the frozen combined source with
the existing runtime wrapper. Native packet delivery, client ergonomics, actual
prices, recovery usefulness and pacing need their distinct proofs.

## Lane handoff categories

- `COMPLETED`: source implementation of the bounded core above; not executable qualification.
- `BLOCKED`: None for source implementation; PC/JVM qualification awaits the coordinator's serialized lease.
- `NOT EXECUTED`: Java compilation/regressions, server boot, native packets, client and campaign.
- `NEEDS HUMAN VALIDATION`: prices, usefulness, pacing, clarity and interaction comfort.
- `NEEDS MY DECISION`: None; direct barter and preserving the generous starter are assigned decisions.
- `NEEDS CREDENTIAL / EXTERNAL RESOURCE`: None.
- `COST / BILLING RISK`: None; no paid generation or external analytics.
- `DEPLOYED STATE`: unchanged; this worker does not publish, deploy or open worlds.
