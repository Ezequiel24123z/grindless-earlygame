# T1 quest teaching

## Scope and authority

This is the owner-directed T1 development unit while the owner tests the
unchanged T0 development build. It is not T0 acceptance, a frozen campaign,
an RC, a T2 activation or permission to replace the installed package.
The source baseline is `a64d44f9ee6da1fe00ed9215eb05b4aad4132f64`.
The [T1 first-factory contract](t1.md) owns the production-line classification.

`QuestCatalogue` owns task identities, claim dependencies and rewards.
`T1Progression.byQuest` selects each T1 line; its requirement selects the
core, optional or physical-exit teaching path. Body keys include that line's
identity rather than sending all T1 objectives to the old generic preview.
`atlasForQuest` selects the actual `AtlasLogic.Entry` objects. Quantities,
outputs, catalysts, conditions, work and energy remain in the recipe authorities.
The Thermal Generator's native fuel authority is explicitly identified as
non-Atlas data; no synthetic recipe row or copied fuel duration is invented.

Both the native Quest Book and Heracles retain their shared
`QuestTeachingClient.content` entry point. Native navigation, every T1
Heracles subtitle and the shared lesson explicitly say **in development**. The Heracles
group names, stable generated IDs, individual progress, empty native rewards
and authoritative common claim service are unchanged. Resource handling and
next-use details still come from the shared live guidance, not a new tooltip,
JEI or quest-only material catalogue.
Framework descriptions remain empty: the
[pinned Heracles source API](https://github.com/terrarium-earth/Heracles/blob/9c62ea01b6dd689645b4deac045d7ddbc80f40d8/common/src/main/java/earth/terrarium/heracles/api/quests/QuestDisplay.java)
takes string descriptions, while localized rich instruction belongs to the
existing shared widget. The selected publisher-binary runner remains a separate gate.

## Implemented lesson selection

The book now has 50 tasks: all 32 T0 tasks, all 14 old T1 objectives and four
new rewardless T1 lessons. The existing 46 IDs and 26 rewarded tasks remain.

| Tasks | Teaching and next use | Classification |
| --- | --- | --- |
| `thermal`, `pylon`, `first_grid` | Native furnace fuel, renewable charcoal, actual pylon coverage/link/transfer limits, delivered power, under-volting and simultaneous load. | Core physical power; `first_grid` is a free reading checklist. |
| `scanner`, `terrestrial` | Survey a compatible virtual chunk deposit, then extract its actual finite material under power; preserve T0 mineral renewal. | Optional; neither is required for F2. |
| `pulverizer`, `furnace` | Dry milling, carbon reduction, ingots, Slag and CO; actual power, conditions, outputs and side-access troubleshooting. | Core milling/reduction. |
| `press`, `forming_dies`, `copper_coil` | Reusable die catalysts, iron plates/rods/gears, Machine Casings and the direct Press coil rather than the later Wire Mill. | Core physical forming; `forming_dies` is reading, `copper_coil` is one inventory checklist. |
| `assembler`, `matrix_line` | Reserve the last seed controller, construct the Assembler and reproduce the original F1 batch with a physically calibrated replacement core. | Core fabrication; retained stock is not evidence of a new run. |
| `glass` | Actual powered glass plus the existing geological, wet-separation, water, culture, charcoal and metal upstreams. | T0-backed mod-owned renewal, not a new vanilla-farm dependency. |
| `logistics`, `kiln`, `factory_world` | Actual belt/junction crafts; optional SO2 roast/R2 reduction distinct from ordinary CO reduction; the existing construction-space portal. | Optional branches, not frontier requirements. |
| `flux_cell`, `industrial_matrix` | The real copper/iron Flux Cell craft and live rated F1-to-F2 Assembler conversion. No cell charge, Harness, Drill Cell or T2 machine is required. | Physical exit; the new cell stock checklist is not an added claim prerequisite. |

The new reading/checklist tasks never mint items, reset an old claim or add
a required quest tax before `industrial_matrix`. Their physical components
remain required only where the real production recipes require them.

## Compatibility and the intentional edge change

Every T0 task declaration, body, reward, dependency and final F1 claim remains
unchanged. The original evidence/status ordinals, claim service, per-player
ledger, SavedData keys and reward-delivery behavior are not migrated.
The final T0 `relay` claim still requires five valid physical F1 Matrices for
new claims; historical claims remain authoritative.

The only old claim dependency removed is
`industrial_matrix -> factory_world`. The owner explicitly selected the
physical F2 frontier without requiring optional building space.
`industrial_matrix` still requires `matrix_line`, `glass` and the common
individual final-T0 chapter anchor. Its evidence is still a valid physical
F2 Matrix, not an untagged item, menu, reading lesson or world flag.
Its ID and eight-iron reward, and the portal's ID/reward/progress, remain intact.
Already-recorded terminal claims stay recorded; no reward can be paid again.

The shared source-binding guard changes only its exact expected
`QuestCatalogue.chapter` case list for the four new T1 IDs. Its closed AST,
book/sender/evidence/ledger/reward guards and T0 chapter anchor are not relaxed.

## Qualification and integration handoff

This completed source unit is not an executable pass. The existing
`VerifyQuest`, `VerifyQuestClaims` and publisher-binary `VerifyHeracles` checks
are updated for the new nodes and the optional portal edge.
`VerifyT1Teaching` is discovered by the existing common runner and checks the
actual line/path/body mapping, exact selected live row objects, missing-row
rejection, changed output/work/condition data, die catalysts, F1/F2 ratings,
real Flux Cell craft, upstream supply and the 33-key T0 body snapshot.

The Heracles upgrade regression authors a representative prior-source 46-ID
bundle with its old portal edge and subtitle shape. It exercises the actual
publisher codecs/ZIP loader, reconstructs all old native task records, restores
the common individual claim set through the unchanged NBT format, and requires
only the four new lessons to gain pending native records. It checks retained
native object identities, empty native rewards and projection/publication
idempotency. This fixture is not an existing-world or graphical migration pass.
Its claim-set comparison does not impose a new ordering on the old HashSet save.

Read-only source validation compares the entire T0 task declaration and all
pre-existing language key values against the stated baseline. The T0 declaration
matches with line endings normalized, old translation values are identical,
and every new lesson body fits the existing 1400-character contract.
This is not Java compilation, a successful recipe run or a client/save pass.
The initial core API's nested `AtlasSelection` scope defect was found by source
review and repaired by its owner before executable qualification.
The first quest checkpoint's attempted component-valued Heracles description
was also caught against the pinned upstream API before Java execution.
The final source restores the original empty description call rather than
inventing another string-format/localization authority; the old checkpoint
remains in history and is not a qualified candidate.
Source review also moved an inadvertently nested claim-test helper to class
scope before this final freeze. Neither correction is presented as an executed
compiler red/green.

The parent's bounded qualification must include common/Forge compilation,
`VerifyQuest`, `VerifyQuestClaims`, `VerifyT1Teaching`, the exact
`VerifySustainabilityRegistry --quest-binding-only` profile, the affected
recipe/Atlas/core-export checks and `:forge:verifyQuestBackend`. Existing
physical Matrix/executor proofs may be reused only within their unchanged
mechanism assumptions. New UI/save observations are not supplied by these
source contracts. No registration, process engine, storage format or
production-server harness changes are part of this quest unit.

The parent owns root changelog/README/ADRs/route-map/handoff, canonical export,
the composed source freeze, compute leases and publication. The quest-owned
map seams are `QuestCatalogue`, `QuestTeaching`, `QuestTeachingClient`,
the native T1 label/optional marker, the T1-only Heracles display fields,
focused quest checks and the exact shared chapter-profile expectation.

| Category | Current handoff |
| --- | --- |
| COMPLETED | T1-specific source teaching, canonical line/Atlas bindings, four rewardless lessons, development labels, preserved T0, intentional optional-portal edge, focused selector/claim/46-to-50 regression authoring and source/API self-review. |
| BLOCKED | Executable qualification has no PC/JVM lease. The parent must freeze the composed source before granting its bounded build/check lane. |
| NOT EXECUTED | Common/Forge compilation; focused quest/claim/teaching checks; actual Heracles codec/visibility/upgrade runner; affected canonical export; new native/client/save checks. Authored checks are not claimed passed. |
| NEEDS HUMAN VALIDATION | Real native/Heracles presentation, readability, interaction, no-command progression and backed-up saved-world compatibility. No player or campaign pass is inferred. |
| NEEDS MY DECISION | None. The optional-portal boundary is the parent's explicit decision. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None for source delivery. No new credential, dependency, generator or provider is requested. |
| COST / BILLING RISK | None. No paid generation, extra worker pool or new budget. |
| DEPLOYED STATE | Unchanged T0 source/runtime `298988d500dccf0823fb2eab52fbe57bff8fe831`, JAR `95bf47c4`, ZIP `3e5ad1fb`. This lane never touches Prism, launcher settings, worlds or the PC. |
