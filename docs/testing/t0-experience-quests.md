# T0 quest teaching and the physical T1 boundary

Source baseline: `b8bd5e885aef598bf888d7e390b297a34cbb7d52`.
Implementation scope: the containing Git checkpoint, not deployed runtime747.
Checkpoint status: **INTERIM SOURCE; not integration-ready qualification**.
Final source checks and in-flight paid legacy Guide assembly compatibility are
still being closed; no JVM/backend pass is inferred.
This is the quest worker's bounded implementation/verification handoff. The
coordinator owns root documentation, the ADR index, generated guidance,
integrated JVM qualification, publication and deployment.

## Implemented contract

The canonical catalogue now has **46 stable IDs: 32 T0 tasks and 14 preserved
T1 objectives**. All original26 IDs, original prerequisite lists and original
material rewards remain. Presentation order and the workshop-handling line may
change; saved claims are keyed by ID, not menu index. Both existing status/evidence
ordinal meanings and the Overworld `grindless_quest_progress` schema stay intact.

The twenty additions explicitly use `Type.LESSON`. They grant no material,
never call reward resolution/capacity/delivery, and record only a player's
acknowledgement or current inventory checklist after prerequisites and the held
Quest Book check. `Type.REWARD` retains complete, nonempty, legal-batch validation,
capacity staging and the existing idempotent claim/delivery path. No lesson
claims successful operation, certification, renewable uptime or human acceptance.

T0 ends at the existing `relay` ID. A **new** claim requires five physical
`grindless:relay_matrix` items with exactly valid F1 specifications, using the
existing `QuestInventoryEvidence`/`ControlMatrixNbt` validation. F2, fractional
or malformed explicit ratings and unrelated items do not contribute. The
evidence is not consumed. Only that player's authoritative `relay` claim opens
T1 navigation; calibration, a menu, another player's progress and retained T1
claims do not.

Historical claims are not reset or paid again. Old saves did not record the
rating/inventory of a historical `relay` claim: the existing claim is honored,
not retrospectively described as a newly observed valid physical run. Old T1
claims without `relay` remain in the ledger but are masked from native
completion/navigation until the individual gate exists. Once it exists, those
same old claims reappear without another reward.

## Actual chapters, not a label-only partition

The native Quest Book has separate T0/T1 tabs. T1 is disabled until a new
server-synchronized gate flag is true. The original status and held-count banks
retain their order; the flag is appended. Client initial state does not assume
the new flag is true. Reading ahead within T0 remains possible.

Heracles has real `Grindless T0` and `Grindless T1` groups. Every T1 definition
adds the same `grindless_relay` prerequisite and uses the framework's
`IN_PROGRESS` hidden policy. Native rewards/autoclaim remain empty/disabled;
completion still mirrors the individual common ledger.

**Important upstream boundary:** Heracles' group-local visibility map treats an
absent cross-group parent as complete. Merely assigning T1 to another group and
setting hidden-until is insufficient. The existing `grindless_relay` quest
therefore also has a display position in the T1 group. It is one quest ID,
one claim and one reward, not a second unlock quest. Before the gate, a Heracles
group heading and that final T0 entry condition may be visible, but none of
the fourteen T1 machine/prototype objectives is visible. After the gate, normal
native prerequisites reveal the preserved objectives. Operator edit mode is a
developer view, not normal player progression.

The real publisher-binary contract test invokes its visibility function and
includes the counterexample with the anchor removed. It also checks codec/group/
hidden-policy preservation, old T1 masking and reconstruction without native
rewards. This test is authored here; execution is separately disclosed below.

The implementation uses the existing compatible Heracles dependency. Source
inspection was pinned to upstream
[`9c62ea01b6dd689645b4deac045d7ddbc80f40d8`](https://github.com/terrarium-earth/Heracles/tree/9c62ea01b6dd689645b4deac045d7ddbc80f40d8),
specifically `QuestsWidget`, `ModUtils`, `ClientQuests`, `QuestDisplayStatus` and
`GroupDisplay`. This is an API/behavior reference and original adaptation,
not imported upstream implementation/text/assets or proof that a moving branch
is the selected binary. The selected binary must pass its existing
`:forge:verifyQuestBackend` contract.

## Quest progression and all31 teaching topics

T0 line order: bootstrap -> workshop -> renewable_supply -> settling ->
bioculture -> workshop_logistics -> relay. The catalogue is the exact dependency
and reward authority; the following is the pedagogical map, not another recipe
graph or a requirement to claim every optional checklist.

| # | Teaching topic | Quest IDs | Role / next use |
| --- | --- | --- | --- |
| 1 | Arrival and starter supplies | `starter_supplies` | Existing free, explicit, finite, once-per-player kit |
| 2 | Quest Book use and recovery | `quest_book_help` | Utility recovery, checklist policy, Atlas versus retired Guide |
| 3 | Superflat constraints | `superflat` | Virtual deposits; bedrock Collector; no permanent farm/structure dependency |
| 4 | Basic construction materials | `construction` | Reserve stone, boards, sticks, furnace and chamber bricks; renewable replacements |
| 5 | Multitool and relocation | `multitool` | Rotate, sneak-relocate saved machinery, recover dropped stacks; not mining |
| 6 | Crank Handle/manual work | `manual_work` | Actual SourceLogic charge/cooldown; eligible F0 startup; not Terminal power |
| 7 | Water collection and storage | `water`, `fluid_logistics` | Source below/front, facing export, capacity/direction troubleshooting |
| 8 | Quern/dry processing | `quern` | Sand and construction feed; complete batch, power and output space |
| 9 | Clay Mixer/wet processing | `clay_mixer` | Clay plus named aggregate and water; unfired bodies and fibreboard |
| 10 | Grog and ceramic recycling | `grog` | Fired/unfired recovery, fresh make-up, retain construction insulators |
| 11 | Firing Chamber formation/operation | `firing` | Plain2x2x2 and actual formation-cell offsets; fuel, conditions, repair |
| 12 | Insulators | `ceramics` | Live yields/grog; cores, storage and primitive logistics |
| 13 | Optional crude extraction | `crank`, `extractor` | Explicit optional iron/finite-deposit branch, never the continuing supply route |
| 14 | Geological Collector | `geological_collector` | Actual dimensions/rock-bed/water/F0 conditions and both outputs |
| 15 | Renewable aggregate | `aggregate` | Allocate dry construction, wet minerals and culture feed |
| 16 | Settling Trough formation | `settling` | Controller/channel/screen, same facing, air above; real cell list |
| 17 | Wet separation/water recovery | `water_recovery` | Actual distinct recipes; only eligible produced water, replace losses |
| 18 | Iron/copper/redstone/slag | `mineral_feed` | Raw feed versus ingots; reducing/carbon firing; honest optional slag handling |
| 19 | Biocultivator/daylight | `biocultivator` | Terrestrial, open sky, daylight; night/restart autonomy not assumed |
| 20 | Biomass | `biomass` | Reserve culture/construction feed before burning stock |
| 21 | Fibreboard/paper/sticks/charcoal | `fibreboard` | Actual boards tag and separate live transformations; no vanilla tag mutation |
| 22 | Biomass Dynamo | `biomass_dynamo` | Actual F0 output/burn duration; fuel/connection/demand troubleshooting |
| 23 | Tank/conduit/fluid logistics | `fluid_logistics` | Optional buffering and compatible ambient flow, not a water source |
| 24 | Conveyors/manipulators | `conveyors`, `workshop_flow` | Optional item routing, direction, destination space; not fluid/power transport |
| 25 | Assembly Bench | `assembly_bench` | Live primitive projections, complete batch and F0; never the Matrix exit |
| 26 | Research Terminal | `research_terminal` | Real F0 supply, physical output and blocked-output recovery |
| 27 | Data Core | `data_core` | Existing insulator/redstone authority, replace through production |
| 28 | Calibration | `calibrate` | Live Terminal cycle; owning the output is not proof of operation |
| 29 | Calibrated Core | `calibrated_core` | Physical custody and replacement by production, never reward reset |
| 30 | Relay Matrix F1 assembly | `relay` | Live manual crafting batch, exact valid F1 inventory evidence |
| 31 | T0 completion/T1 transition | `relay` | Existing individual final claim opens actual separate chapter; no new T1 chain |

The fourteen retained T1 IDs are `thermal`, `pylon`, `scanner`, `terrestrial`,
`pulverizer`, `furnace`, `press`, `assembler`, `matrix_line`, `glass`, `logistics`,
`kiln`, `factory_world` and `industrial_matrix`. Their common lesson explicitly
identifies them as preserved T1 objectives, not completed T0, accepted T1
progression or newly implemented chains.

## Guide/content migration and recovery

`field_guide`, its item model/sprite and its registered menu remain loadable.
Using an old stack never consumes, converts, renames or rewrites its NBT. It
ensures the actual utility Quest Book is available and opens current quests.
Browsing with the legacy item is harmless; claims and the shop still require
the real Quest Book. The retained old menu is only a migration notice plus an
Open current quests action.

The Guide no longer has a shaped catalogue craft, shipped JSON craft, Bench
projection, Assembler projection, quest reward or normal creative-tab offer.
Registry/model/legacy localization identities remain for compatibility. Old
translation values are retained unchanged and dormant; active item tooltips,
book-recovery notifications and teaching use new keys. The Process Atlas is
preserved as the live recipe viewer and is not removed.

Essential old T0 bootstrap/workshop/settling/relay/renewable/logistics explanations
now live in the corresponding quest lessons above. Later-tier Guide prose is
not promoted into T0. `QuestTeachingClient` is the shared native/Heracles lesson
projection; wrapped lines are paged losslessly in both interfaces. Recipe
quantities/times/FU/conditions come from `AtlasLogic` over current process and
craft catalogues, not hand-maintained localized numbers. Structure offsets come
from `WorkshopStructure`; Manual Handle and Biomass Dynamo values come from their
actual shared constants. Runtime resource guidance remains the canonical
generated projection; no source audit is invoked while browsing.

Book recovery now applies on join/respawn after the starter claim as well as
before it. Real inventory/offhand detection prevents a second carried book.
A full inventory preserves everything and retries at the existing bounded
cadence when space is made. Recovery grants exactly one utility book, no supplies
or core/Matrix, and never changes existing claims. The ordinary paper/stick craft
also remains. Losing a core/Matrix is explained as production-based replacement,
not permission to reset valuable rewards.

## Authority and integration seams

| Surface | Owning source / invariant |
| --- | --- |
| Stable IDs, typed lessons, chapter assignment and gate | `quest/QuestCatalogue`, `QuestLogic`; no machine permission |
| Exact current physical evidence | Existing `QuestInventoryEvidence`, `ControlMatrixNbt`; no permissive ITEM proxy for final Relay |
| Saved claims | Existing `QuestProgress`; schema/individual Overworld identity unchanged |
| Material delivery | `QuestClaimService`, existing `QuestRewardDelivery`; LESSON separate, REWARD batch guard retained |
| Recovery | `QuestBookAccess`, Forge `QuestBookAccessEvents`; utility only before/after kit |
| Native display | `QuestBookMenu`, `QuestBookScreen`, shared `QuestTeaching`/`QuestTeachingClient` |
| Heracles display/completion | `HeraclesDefinitions`, `HeraclesClaimProjection`, `GrindlessClaimTask`, `HeraclesQuestClient` |
| Deprecated compatibility | `FieldGuideItem`, `FieldGuideMenu`, `FieldGuideScreen`, existing registry/assets |
| Craft removal | `BootstrapRecipes`, removed `recipes/field_guide.json`; generated assembly follows the catalogue |
| Resource authority | `AuthorityAdapters`; rewardless lessons are objectives with no invented material reward route |
| Source binding | Closed profiles and negative mutations in `VerifySustainabilityRegistry`; actual book/sender/rating/ledger guards remain checked |

The default untagged Relay craft already creates the native first-frontier F1
item. Canonical resource accounting therefore retains that authored default
production identity while the reward route explicitly declares
`evidence=MATRIX` and `matrix_rating=F1`. Non-default F2 evidence remains a
separate rated resource. This does not change the strict stack validator or
invent a second recipe/output. Counterexamples and exact catalogue projections
are updated rather than dropping count/equality assertions.

**Coordinated dependencies, not private shims:** the shop worker supplies
`shop.ShopNetwork.requestOpen()` for both client buttons and
`shop.ShopAccess.open(ServerPlayer)` for real sender/held-book checks.
The telemetry worker supplies `telemetry.dev.DevTelemetry.milestone(Level,String)`.
The live server claim method invokes it only after a real `CLAIMED` result,
including LESSON, never on opening, polling, rejection or duplicate requests.
No second shop protocol, wallet, reflection fallback or telemetry engine is added.
These imports require integration of that worker before combined compilation.

The coordinator must update AGENT-MAP's Quests/Guide, resource-authority and
Heracles entries, record the owner-directed chapter/migration decision, and
update CHANGELOG/README/handoff. Suggested changelog scope:32 T0 teaching tasks,
preserved26 claims/rewards, exact F1 exit, real separate locked T1 groups,
deprecated safe Guide, utility-only recovery and shared live-recipe help.

## Verification and remaining execution

Actually executed on this source lane: one non-JVM PowerShell check confirms
46 unique IDs,32 T0/14 T1,20 explicit new lessons, all localized quest/body keys,
body length bounds, no duplicate JSON keys and no obsolete Guide recipe.
`git diff --check` passes. These are source/data checks, not Java execution.
The first broader PowerShell observer stopped at an apparent old-localization
mismatch under Windows' default text decoding. The explicit UTF-8 baseline
comparison subsequently passes every original localization value without
rewriting those values. Preserve that failed observer separately from a valid
source assertion; it is not a Java or gameplay failure.

The authored focused proof set is `VerifyQuest`, `VerifyQuestClaims`,
`VerifyPersistence`, `VerifyBootstrap`, `VerifyRecipes`, `VerifyAtlas`,
`VerifySustainability`, `VerifySustainabilityAdapters`,
`VerifySustainabilityRegistry --quest-binding-only`, `VerifyResourceGuidance`,
`VerifySustainabilityT0` and `:forge:verifyQuestBackend`. It covers old reward/
prerequisite pins, reading without delivery, exact F1/malformed/mixed evidence,
individual save/reload, retained T1 claims, actual native visibility, safe
publication and complete current recipe projections.

Required coordinated preparation uses the existing commands, after integrating
the shop/telemetry API:

```powershell
.\gradlew.bat --no-daemon --quiet --console=plain :common:compileJava :forge:compileJava :forge:verifyQuestBackend :common:writeCheckClasspath
powershell -ExecutionPolicy Bypass -File .\tools\generate-resource-guidance.ps1 -Root .
powershell -ExecutionPolicy Bypass -File .\tools\generate-resource-guidance.ps1 -Root . -CheckOnly
```

Run the selected existing check classes against that exported classpath, then
the affected guidance/T0/full-asset checks after normal regeneration/resource
refresh. The committed guidance JSON is intentionally **not** edited by hand:
these changed catalogue/claim/recipe authorities require normal regeneration on
the composed source. Do not weaken its strict equality or reuse runtime747's
old declaration fingerprint as a current pass.

No PC JVM/GPU/Prism/shared-cache lease is held by this worker. No build, Java
contract, native smoke, client interaction or retained-world check is credited
until the assigned integration runner actually executes it. Unchanged process,
fluid, sound and asset mechanisms do not need duplicate per-recipe smokes.

Controlled client follow-up, on a backed-up test copy, must verify new/existing
worlds; pre/post-starter book loss; full inventory/deferred recovery/offhand;
Guide stacks/NBT; lesson paging/long translations; shop routing; per-player
claims/rejoin/party behavior; four/invalid/mixed versus five valid F1 Matrices;
no premature T1 content; actual unlock; and no repeated original rewards.
These probes are not a no-command campaign, artistic approval or longitudinal
save acceptance.

## Worker handback categories

| Category | State |
| --- | --- |
| COMPLETED | Source implementation, targeted regression source and this31-topic/content/seam map; non-JVM data/whitespace checks as described |
| BLOCKED | Standalone combined compilation needs the coordinated shop/telemetry imports and the parent's assigned execution lane; no local JVM authorization |
| NOT EXECUTED | Common/Forge/backend Java execution, canonical regeneration, native/client/world/save probes, publication and deployment |
| NEEDS HUMAN VALIDATION | Actual UI/readability, lessons, ergonomics, recovery and multiplayer observations, no-command progression and retained worlds |
| NEEDS MY DECISION | None |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None |
| COST / BILLING RISK | None; no generation, billing activation or paid request |
| DEPLOYED STATE | Unchanged by this worker; runtime747 remains the prior installed candidate, not this source |
