# Fresh-Superflat T0: entry options

**Decision now made: ADR-0159 selects a generous one-time kit in the first free
quest.** The owner explicitly rejected more rounds of entry alternatives.
Implementation is active; no village, shop or new primitive/worldgen route is
needed. The alternatives below are preserved analysis, not current blockers.
The campaign and T0 human acceptance/freeze are still unexecuted.

The implementation allocation is128 cobblestone,64 oak logs,64 bread,64 bricks,
64 clay balls,64 sand,32 charcoal,16 iron and2 water buckets, with initial book
access and one server-authoritative claim. The earlier smaller planning allocation,
food/difficulty questions and optional33-brick proposal below do not restrict that
owner decision or require further approval before coding.

The read-only planning worker inspected source269965b. It could not edit files,
commit or send a session handoff; the coordinator recovered its completed
analysis, materialized this proposal and checked the critical recipe/site/energy
facts against the unchanged production sources. The quantities below are source
planning, not native acquisition or measured player-time evidence.

## The actual entry cuts

Current machinery can produce aggregate/clay, stone/gravel/sand, biomass,
boards/sticks/paper/carbon, ceramics, glass and the first metal/redstone inputs.
The bootstrap still needs **initial construction stone, wood/sticks and an actual
world water source** before those producers exist.

Virtual deposits are not initial physical stock or power. Conversely, no new
physical ore generation or stone layer is necessary just to satisfy the Collector:
the existing `grindless:geological_bed` tag already accepts bedrock. The Collector
must stand immediately above that tagged block; a surface layout may instead
retain one cobblestone beneath it.

Classic Flat's bedrock/dirt/grass terrain does not itself provide the complete
wood/water/accepted-crafting-stone start. Structures enabled is not a verified
spawn guarantee. A deliberately selected village seed/site is a separate option
that must be checked, not an assumption about every flat world.

## One feasible industrial prefix

This is a working allocation, not a claim of a globally minimal or approved kit:

| Supplied requirement | Quantity and use |
| --- | --- |
| Accepted cobblestone | 16:15 initial craft inputs and1 retained practical surface rock bed |
| Actual vanilla planks | 24:4 Crafting Table,6 converted to12 sticks,14 Collector/Pump/Culture/Mixer construction |
| Actual source water | One source in the world; the mod's unpowered pump does not consume that source |
| Initial clay, bricks, carbon, copper or redstone | None for this first prefix |
| Loose iron or lava | None for the prefix; supplied water buckets nevertheless embody iron |

The15 initial stone craft the Handle(1), Collector(5), Quern(8) and Pump(1).
Run collection twice: the two clay construct the culture machine. Mill one
aggregate into4 cobblestone for Multitool(1), culture(2) and Mixer(1). Culture the
other aggregate and mix the first fibreboard. The plank/stick ledger is
nonnegative; the initial12 sticks leave3 after this prefix.

The exact process cost of that prefix is:

| Work | FU | Recipe water |
| --- | --- | --- |
| Two geological cycles | 1600 | 500 mB |
| One dry-aggregate cycle | 480 | None |
| One culture cycle | 640 | 250 mB |
| One fibreboard cycle | 1600 | 250 mB |
| Total | **4320** | **1000 mB** |

At160 FU per Handle action, this is27 charges, not a measured27-click walkthrough.
Placement, transfer, waiting and recovery still matter. The book cannot deliver
the initial bridge by assumption: its paper route only exists after production.

A proposed recovery allocation is **24 cobblestone,32 planks and2 filled water
buckets**. It adds8 stone/8 planks and protects a second water seed, covering a
defined early construction/water loss rather than destruction of the entire base.
It is not authorized. Two water buckets embody6 iron; do not call them free water
or silently substitute them for the campaign's metal production.

## First automation and the physical exit

The Firing Chamber requires33 brick items:5 controller inputs plus7 brick blocks
at4 bricks each. Existing biomass has200-tick vanilla furnace burn time, so the
first bricks do not require an established Firing Chamber or permanent tree farm.

The planning worker's conservative schedules to the first Biomass Dynamo,
without spending quest rewards, estimate:

| Construction entry | Process FU | Rounded manual-charge planning ceiling | Recipe water |
| --- | --- | --- | --- |
| Produce the initial construction bricks | 60240 | 377 | 15000 mB |
| Also bridge33 finite fired brick items | 34320 | 215 | 6500 mB |

These are source/material planning estimates, not accepted kits, proven minima
or playtimes. Reconcile the complete ordered ledger with the selected delivery/
profile before implementation. In particular, manual-work burden is a pacing
decision, not something to hide behind an engineering pass.

Collection plus culture costs1440 FU and yields4 biomass with6400 FU of nominal
F0 generation potential. Preserve startup fuel and routing margin; this surplus
does not prove nighttime, restart or whole-factory autonomy.

The exit remains a **hand craft of five physical native-default F1 Relay
Matrices**:2 glass,2 copper,2 redstone and1 Calibrated Data Core. The Core needs
1 insulator/1 redstone and4800 FU of calibration; the Terminal needs7 boards,
1 glass/1 cobblestone. The Handle does not supply the Terminal; the native F0
Dynamo does. The Bench does not fabricate the first Matrix batch.

## Materially different alternatives

| Option | What the player experiences | Scope and decision |
| --- | --- | --- |
| **A. Explicit finite construction bridge** | Seed-independent flat start, then the existing mod-owned material/energy production | Approve allocation, optional brick allowance and one-time delivery/recovery custody. Never give insulators, cores, Matrices or finished machines. Reuse existing claim/persistence mechanisms where suitable, but do not assume pre-book access or silently grant old worlds new stock. |
| **D. Verified fixed-seed village profile** | Gather a finite starting supply from a deliberately qualified vanilla village, then use mod production | Least application-code change, but accepts a fixed seed, structures, travel/dismantling and nonempty scenery. Verify actual processed stone/wood/water and clear building space; do not depend on permanent vanilla farms. |
| **B. Soil-buildable primitive source** | No gift or required village; start with new mod-owned primitive interactions/machinery | Larger progression design. Must solve stone, biological construction input, actual water and initial work before the ordinary Handle; wrapping today's Collector does not solve these cuts. |
| **C. Finite requisition/shop** | Choose supplies using a bounded construction budget | Adds currency/catalogue/UI/softlock and custody decisions. Must solve first access before the paper/book route and preserve construction reserve; finite credit is not renewable supply. |

**Coordinator recommendation: A for the smallest seed-independent, clean-building-
space experience using the current machinery.** Decide its finite allocation
explicitly, including whether the33-brick allowance is appropriate for pacing.
If a fixed-seed village start is acceptable, D needs less implementation and is
the smallest code change. B/C should not be introduced merely to avoid asking
about a simpler product choice.

D has a real harvesting bill: a wooden pickaxe adds3 planks/2 sticks. Its working
prefix therefore needs at least27 gross planks, with those sticks covered by the
earlier surplus. With the8-plank reserve, require35, rounded to36 from9 logs.
Count usable crafting stone after village processors: mossy cobblestone is not
automatically accepted by the vanilla stone-crafting tag. No actual seed/site
has been generated or qualified here.

## Survival/profile questions not solved by an industrial kit

- **Food, difficulty and first-night safety remain explicit.** Food is not an
  industrial recipe input, but this analysis establishes no mod-owned edible
  renewable route or measured fresh-world survival experience. A village or finite
  food gift is not automatically the accepted permanent food source. Do not
  silently select Peaceful or claim the industrial prefix closes this release gate.
- **Recovery and save continuity need the selected design.** Version one-time
  delivery/profile rules, test rejoin/respawn/full inventory and distinguish new
  official worlds from existing saves. Preserve old worlds and surveyed identities.
- **Human pacing and the campaign remain unexecuted.** Static quantities and
  previously qualified renewable mechanisms do not establish enjoyment or the
  no-command five-Matrix route on the actual official profile.

No choice above is implemented. Independent art, SFX, client readiness and
engineering continue while the owner chooses the entry experience.

## Source locators

Java root: `common\src\main\java\io\github\ezequiel24123z\grindless\`.

- `recipe\BootstrapRecipes.java:19-27,48-147`: ingredient authorities and patterns.
- `recipe\RenewableProcesses.java:14-51`: real source, stone and biological routes.
- `source\SourceLogic.java:8-15`: durations, quantities, furnace fuel and160-FU work.
- `machine\ProcessMachineBlockEntity.java:606-625` and
  `source\SourceSite.java:16-29`: site predicates and manual-work boundary.
- `common\src\main\resources\data\grindless\tags\blocks\geological_bed.json:3-6`:
  bedrock and cobblestone are already eligible.
- `fluid\HandPumpBlockEntity.java:41-74`: unpowered source-water production/transfer.
- `structure\WorkshopStructure.java:18-25`: seven-brick-block Firing envelope.
- `recipe\T1Recipes.java:39-54` and `research\ResearchLogic.java:14-20`:
  five-Matrix recipe and4800-FU calibration.
- `sustainability\T0Sustainability.java:25-82`: conditional entry is not flat access.

Vanilla1.20.1 data was inspected by the planning worker at the versioned
`misode/mcmeta`1.20.1-data tree for Classic Flat, villages, crafting tags and
ordinary recipes. Reference only; no third-party code/assets are imported.

## Handoff

| Category | Current outcome |
| --- | --- |
| COMPLETED | Focused source analysis, quantified working prefix, four alternatives and a conditional recommendation; coordinator materializes the read-only worker result. |
| BLOCKED | Implementing an entry route awaits the owner's material product choice, not another technical audit. |
| NOT EXECUTED | Starter/profile/shop/source implementation, actual village seed/site acquisition, native ordered ledger and no-command campaign. |
| NEEDS HUMAN VALIDATION | Pacing, survival/first-night experience, client UX and actual manual campaign/save continuity. |
| NEEDS MY DECISION | A/D/B/C entry experience; afterward allocation/brick allowance and food/difficulty/recovery policy as separate concrete choices. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None for the proposal. The read-only agent lacked editing/Git/messaging, resolved by coordinator materialization rather than another worker. |
| COST / BILLING RISK | No model/provider/generation request or new service in this proposal. |
| DEPLOYED STATE | Unchanged; no proposed gameplay is promoted into the installed candidate. |
