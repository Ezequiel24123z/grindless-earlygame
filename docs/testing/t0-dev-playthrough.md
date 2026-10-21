# T0 development playthrough protocol

**Technical/native candidate installed; owner-authorized longitudinal play
(ADR-0170), actual client/save observations pending.**
The owner explicitly chooses to begin the lasting T0 game while T1 develops.
Intent/permission is not an observed launch, completed smoke or finished
playthrough. Exact F is documented in the
[Prism delivery](t0-experience-prism-deployment.md),
[source qualification](t0-experience-source-qualification.md) and
[native package qualification](t0-experience-package-native.md).
The original documentation baseline was mainb8bd5e8/installed747; its checkpoint
disclosure below remains historical rather than a current missing-feature list.

## Authorities and boundaries

Use the [current gate](../reviews/go-gate.md), [active handoff](../../.agent/handoff.md),
[pending checks](../../.agent/pending-checks.json), [proof-reuse strategy](test-strategy.md)
and [playable-release plan](../../plan/process-t0-playable-release-1.md) together.
The parent coordinator owns product decisions, final qualification and safe
integration. ADR-0170 authorizes the owner's start and parallel T1 work; this
document records evidence without relabeling any unobserved property as passed.

The [README](../../README.md), [process definitions](../PROCESSES.md),
[machine definitions](../MACHINES.md) and live catalogues remain authoritative.
In particular, [QuestCatalogue](../../common/src/main/java/io/github/ezequiel24123z/grindless/quest/QuestCatalogue.java)
now has46 tasks:32 T0 and14 separately gated T1, including20 reward-free lessons
and all26 preserved original IDs. The31 milestones below are the human observation
route, not another runtime catalogue or proof that a machine operated.

[ADR-0159](../DECISIONS.md#adr-0159--start-t0-with-one-free-generous-starter-quest)
settles the free finite starter. Older no-kit entry prose in the
[survival checklist](t0-survival-acceptance.md) is historical, not the current entry
requirement. Its retained-world precautions remain useful. Likewise, the requested
Guide migration, expanded chapters, barter and hatches have actual source/contract/
native evidence on F; the optional overlay has its stated source/codec/loader scope.
Actual client rendering and interaction still require the controlled smoke.

The T0 exit remains the physical five-F1-Relay-Matrix batch, followed by the final
T0 quest and a separate T1 chapter/tab. Viewing that transition does not authorize
T1 machines, production chains, a release/tag or T0 acceptance. Keep
[ADR-0144](../DECISIONS.md#adr-0144--ship-superflat-first-playable-tier-releases-with-longitudinal-save-continuity)
and [ADR-0160](../DECISIONS.md#adr-0160--merge-the-testing-ready-t0-candidate-and-close-superseded-prs)
delivery, publication, play and acceptance boundaries distinct.

## Separate the evidence modes

| Mode | Permitted setup and purpose | Must not be claimed |
| --- | --- | --- |
| Controlled developer smoke | Coordinator-authorized disposable worlds or preserved test copies; commands/staged resources disclosed; targeted technical observations | No-command progression, measured natural pacing, longitudinal stability or human approval |
| Human longitudinal run | Owner-authorized reference save after all required gates pass or the owner explicitly reclassifies them; Survival without commands or diagnostic carryover | Completion before it is played, automatic artistic approval or unrestricted T1 development |

Never adopt a smoke world or an old exploratory save as the reference world.
Preserve originals, take a verified backup before migration and test on a copy.
Keep smoke and human logs separate. A command, quest repair or injected resource
contaminating a human segment must be recorded; retain earlier observations but do
not describe the assisted segment as no-command proof.

## Candidate identity and required gate

Fill the remaining human-run fields before execution. `MISSING` means no applicable
observation, not necessarily missing implementation; `PARTIAL` names the verified
subset and the still-required boundary. Neither is promoted to PASS.

| Identity field | Current record |
| --- | --- |
| Integrated candidate source and separate runtime revision, if different | Exact F `298988d500dccf0823fb2eab52fbe57bff8fe831`; documentation successors do not retarget the artifact |
| Mod/pack versions, selected profile and pinned manifest/configuration revision | Mod0.1.0/pack0.1.0-t0.1, seven-pin playable/restricted-not-RC, exact F; [version authority](../../gradle.properties) |
| Built and installed JAR SHA-256; pack ZIP SHA-256 | JAR `95bf47c4e5d08a0788095555ed38078431e8f74d8d491389b56b7539d7c91621`; ZIP `3e5ad1fbbc23d19232a589bce4430ff2bdc344d0c6776bbf71400579aab607a1` |
| Actual Minecraft, loader, Java and relevant optional-mod versions | Forge1.20.1/47.4.23, Java17; seven pins unchanged. Client-only dependencies/rendering not launched by server proof |
| Non-identifying run ID, UTC observation time and observer kind (human/agent/automated) | Automated F/native/deployment receipts exist; human controlled-run ID/time MISSING |
| World kind/preset, new/existing/copied-upgrade origin and prior artifact identity | MISSING |
| Verified backup/preservation receipt and safe rollback scope | Six old-instance files/two Dev metadata hashes unchanged; no protected Dev game paths existed before/after. Test an old-world upgrade only on a verified copy; no downgrade compatibility is inferred |
| Coordinator's source-bound gate decision and start authorization | Owner explicitly authorizes intended longitudinal T0 start under ADR-0170; actual graphical/save/campaign observations still pending, not PASS |

Forge is the baseline build target; compatible NeoForge loading is not a separate
compiled platform or an observed pass. Record the actual supported matrix and any
compatibility claim. Never silently substitute another loader, pack or installed JAR.

| Gate | Required property | Status | Evidence available / still required |
| --- | --- | --- | --- |
| G01 | Clean supported-loader builds, generated resources and package | PASS | Actual common/Forge/full52/backend776/normal generation and exact-F package88; hosted F checks have their own live result |
| G02 | Intended revision installed in the owned development instance | PASS | Exact F/95bf/3e5a Deployed/VerifiedStopped/Unchanged and fresh preservation |
| G03 | Client launch, new Superflat and Overworld creation/load, existing-world copy load | MISSING | BLOCKED / NOT EXECUTED: approved graphical access to the execution host is unavailable; source-bound graphical observations still required |
| G04 | Quest Book immediately available and recoverable; starter safely claimable once | PARTIAL | Actual contract/registered-item claim proof; client login/full-inventory/retry/rejoin/respawn MISSING |
| G05 | Standalone Guide no longer required; old Guide items harmless and progress retained | PARTIAL | Old ID/redirect retained, recipe removed, real registered paid jobs settle once; actual legacy-item client use MISSING |
| G06 | Expanded T0 route, optional branches and essential explanations in quests | PARTIAL |46/32/14/20 catalogue, live six-port help in both projectors and backend776 pass; client reading/input MISSING |
| G07 | T1 chapter locked early and unlocked only by valid definitive T0 completion | PARTIAL | Actual native wrong/malformed/four/five F1, ledger/player/once-only gate pass; actual UI transition MISSING |
| G08 | Shop usable, progression-gated and without known trivial infinite profit | PARTIAL |17 offers, physical staging/menu protocol/cycle tests and canonical ledger pass; real client menu transactions MISSING, prices remain unmeasured |
| G09 | Recovery avoids permanent soft locks without resetting one-time claims | PARTIAL | Utility-book/claim contracts and pre-producer dirt recovery pass; actual user recovery/pacing MISSING |
| G10 | Mandatory route avoids structures/mob farms and establishes mod-owned renewables | PARTIAL | Final canonical graph/conditional construction witness and unchanged source proofs pass; not a no-command played route or all-condition autonomy |
| G11 | Tanks store, transfer, expose side rules and persist safely | PARTIAL | Actual native containers/side/invalid/metadata drop-replace and166 Tank checks pass; actual player-world unload/reopen/upgrade MISSING |
| G12 | Intended multiblocks accept approved functional ports/hatches | PASS | Six real variants/four roles, rotations/collision/default contracts and actual item/fluid750 transport verified |
| G13 | Invalid/excess/orphan interfaces rejected without lost or duplicate work | PASS | Source/JDK negatives plus real retired handler/quota/alias/controller replacement/callback-conduit compensation pass |
| G14 | Accurate basic overlay state; optional integration safe when absent | PARTIAL |409 DTO/codec/query/absence checks and real Forge binding pass, no Jade dependency; client HUD/network comparison MISSING |
| G15 | Every distributed sound resolves; no inappropriate generic synth remains | PASS | Four exact physical Oggs/ten events, five synth files absent, intentional silence and native aliases verified |
| G16 | Audio loops obey state, removal, unload, dimension, disconnect and reload lifecycle | PARTIAL |101 mechanism and47+30 source/resource controls plus registration pass; audible final-client lifecycle MISSING |
| G17 | Essential assets/models/localization and redistribution provenance valid | PARTIAL | Normal generation,25/71/281 projection, references/rights/preserved sprites pass; representative actual rendering MISSING |
| G18 | Save/reconnect preserves inventories, work, quests, tanks and structures | MISSING | Controlled NBT/native save evidence exists, but final player save/reconnect and authentic copy-upgrade smoke NOT EXECUTED |
| G19 | No credentials, account data or private host paths in source/package/evidence | PASS | Sanitized tracked diff, provenance whitelist and exact package checked; private logs/inventories remain outside Git |
| G20 | Repository/installed identities, applicable checks, failures and deferrals recorded | PASS | Phase A/B/Prism handoffs bind exact F and raw proofs; all reds/cancellations/graphical gaps remain explicit |

For each gate evidence entry record source, artifacts, observation time, mode,
property, actual result, sanitized reference and limits. Reused proof keeps its
original source and requires an explicit unchanged-assumptions/delta justification
under ADR-0140. Failed, cancelled, skipped and NOT EXECUTED remain visible.
Do not rerun unchanged mechanisms once per recipe or texture.

**Technical evidence gate: required observations remain MISSING; no new technical
or human pass is inferred.** The owner separately authorizes the intended
longitudinal start under ADR-0170 and supplies actual play feedback. Current
local UI access does not reach that PC; agent-side graphical observation would
still require an approved route. No new access grant is created by this protocol.
Only the coordinator may record `TECHNICALLY PREPARED FOR LONGITUDINAL HUMAN VALIDATION`
after every required property has applicable evidence or an explicit owner-approved
reclassification. Subjective final art/audio, exact duration/prices, factory mix,
fatigue, clarity, ergonomics, fun and final acceptance may remain for the human run;
missing technical functionality cannot be silently moved into that list.

## Controlled smoke, before any longitudinal start

Use the existing [client preparation](t0-client-review-preparation.md) and
[SSH/Prism runbook](../operations/ssh-prism-workflow.md), not a new launcher or an
unowned default inventory. Only the explicitly authorized runtime owner executes.
Record the uncertainty each expensive observation resolves.

1. Confirm the exact stopped target without silently Syncing or deploying over a
   mismatch. Preserve saves, screenshots, options, keybindings and protected
   configuration. Stop on wrong identity, failed backup or unresolved ownership.
2. Use separate new Superflat/Overworld smoke worlds and an authentic preserved
   older-world copy. Check initial/recovered book, explicit free starter, complete
   reward delivery, full inventory/retry and no duplicate claim after rejoin/respawn.
   Exercise supported per-player behavior; label unobserved multiplayer NOT EXECUTED.
3. Inspect the actual T0/T1 chapters and safe old-Guide behavior. Compare quests,
   JEI, tooltips and overlay against canonical processes, including inputs,
   by-products, energy/manual/fluid/environment requirements, structures, blocked
   reasons and next use. Check the optional renderer/integration absent boundary
   in an isolated profile; never remove dependencies from the owner's live save.
4. Exercise representative manual/powered processing, shop/recovery, tank/container/
   conduit flow, default and hatch-equipped structures, invalid interfaces and
   repair. Compare quantities/state before and after save, full clean process exit,
   reconnect and relevant chunk lifecycle. Rotation/breaking must not lose or
   duplicate contents or retain an invalid owner.
5. Observe sound start/stop with one machine, several identical machines and a
   mixed workshop; include attenuation, pause/mute, state changes, removal,
   unload/dimension/disconnect and resource reload. Record missing sounds,
   duplicate loops, clipping/seams and visible feedback. Technical playback is not
   a human listening/fatigue approval.
6. Prove T1 cannot unlock early or through an unrelated/wrongly rated item.
   In the controlled world, use the actual physical F1 batch and normal final
   quest action, not an edited claim flag, to observe the valid transition.
   Disclose staged inputs; this does not prove natural production or pacing.
   Stop before playing T1 machinery or chains.
7. Save and stop cleanly. Record startup/shutdown failures and preserved-file
   comparison, retain the evidence and hand the gate disposition to the coordinator.
   A successful smoke does not start or authorize the longitudinal campaign.

## First 31 human-route milestones

**NOT EXECUTED throughout.** These are observation prompts, not a second recipe,
reward or dependency catalogue. Follow the integrated quest graph and measure actual
quantities; do not enforce this list as an invented unlock chain. Optional crude
extraction remains optional. Record real operation/output separately from crafting,
possession, quest completion and reward claim.

| Milestone | Route observation | Evidence to capture during the authorized human run |
| --- | --- | --- |
| M01 | Arrival and starter supplies | First controllable moment, discoverability, explicit free claim, delivered inventory and finite-supply understanding |
| M02 | Quest Book recovery and use | How opened/recovered; claim state unchanged; no standalone Guide required |
| M03 | Superflat constraints | Actual preset and understood initial/renewable routes; no structure, mob farm or command dependency |
| M04 | Basic construction materials | Acquisition, reserves, shortages and irreversible-spend risk |
| M05 | Multitool and relocation | Discovery, rotation versus collection, retained contents/work and readable icon |
| M06 | Crank Handle/manual work | Supported interactions, counted actions/work, fatigue and transition away from repetition |
| M07 | Water collection and storage | Source/container/pump acquisition, real delivered fluid and early shortage/recovery |
| M08 | Quern/dry processing | First real batch, outputs, power/manual work, sound and blocked explanation |
| M09 | Clay Mixer/wet processing | Inputs, consumed water, output batch and fluid-arrival recovery |
| M10 | Grog and ceramic recycling | Actual recovery/use, fresh-input requirements and clear by-product classification |
| M11 | Firing Chamber construction and operation | Shape/orientation instructions, formation/fault clarity, fuel and first firing |
| M12 | Insulators | Produced amount and next consumers; no reward bypass mistaken for manufacture |
| M13 | Optional crude extraction | Whether chosen and why; cost, time and recovery recorded separately from mandatory route |
| M14 | Geological Collector | Placement/environment, initial work and first real source output |
| M15 | Renewable aggregate | Repeated production and terminal inputs; no finite upstream stock described as renewable |
| M16 | Settling Trough construction | Shape, clear space, orientation and readable missing-part feedback |
| M17 | Wet separation and water recovery | Actual input/output and recovered water; losses, blocked output and reuse |
| M18 | Iron, copper, redstone and slag | Actual routes/yields, limiting material and honest optional/by-product handling |
| M19 | Biocultivator placement/daylight | Environment explanation, daylight/night behavior and visible blocked reason |
| M20 | Biomass | First output, repeated supply and sustainable terminal dependencies |
| M21 | Fibreboard, paper, sticks and charcoal | Cross-machine uses, batches, shortages and avoidable repeated crafts |
| M22 | Biomass Dynamo | Real fuel/energy accounting, workshop demand and restart/manual interventions |
| M23 | Tank, conduit and fluid logistics | Capacity/fill readability, container/sides, actual transfer and safe persistence |
| M24 | Conveyors and manipulators | Direction/filter/transfer clarity, backpressure and reduction of manual carrying |
| M25 | Assembly Bench | Actual supported crafts, work/power, bottlenecks and discoverability; not a Matrix-exit bypass |
| M26 | Research Terminal | Placement, power, visible process and blocked/output-waiting feedback |
| M27 | Data Core | Actual construction route, reserves and recovery without reward substitution |
| M28 | Calibration | Physical core input, actual energy/work/duration and safe interrupted/restarted work |
| M29 | Calibrated Core | Retrieval, count/rating where applicable, next use and loss-recovery path |
| M30 | Relay Matrix F1 assembly | Actual manual recipe, five physical F1 outputs, input accounting and elapsed time |
| M31 | T0 completion and T1 transition | Normal final quest action, retained one-time claim and newly unlocked separate T1 chapter; stop before T1 chains |

## Local telemetry and observation record

Optional local aggregate telemetry is implemented and automatically checked.
It is **off by default**: an operator can use `/grindless telemetry start`,
`snapshot` and `stop` in a diagnostic run; this is not authorization to use
commands in the later no-command campaign. Manual notes remain valid. Keep raw records in
ignored local evidence storage; publish only reviewed, sanitized summaries/references.

Use a non-identifying run ID. Bind each record to the candidate/artifacts and
milestone plus actual machine/process/quest/offer ID. Record elapsed seconds with a
consistent clock, observed quantities and the evidence mode. Never fabricate counts
for operations that are not observable; label estimates and MISSING measurements.

| Record | Minimum fields and interpretation |
| --- | --- |
| Milestone timing | Wall elapsed, active elapsed, segment duration and explicit pauses from first controllable spawn through M31. Include discovery, shortages and forced waiting in active time; exclude only declared outside-play pauses. Do not treat old pacing targets as measured results. |
| Machine/process counts | Machines built/placed/relocated, completed cycles by actual process, batch sizes, inputs/outputs/by-products and failed/blocked attempts. Separate concurrent production from serial elapsed time. |
| Manual repetition/downtime | Crank/use actions, crafting/carry trips, work supplied where visible, forced waiting, repeated low-value actions and manual recovery. Do not sum overlapping time categories twice. |
| Confusion/readability | Milestone, exact in-game text/state, what was misunderstood, first attempted action, resolution and whether external docs/source were needed. Note icons, native inventory/hotbar/JEI scale, lighting, rotation and color-independent cues. |
| Blocked conditions/shortages | First occurrence, machine/process state, limiting resource/environment/output, duration, diagnostic surface, intervention and stock after recovery. Record night/restart behavior rather than assuming autonomy. |
| Shop transactions | Actual offer/unlock, physical payment items, price, before/after inventory, intended emergency/convenience/decorative role and suspected loop. No wallet/currency is implemented. Never use an exploit to supply the accepted critical route. |
| Recovery events | Lost book/tool/container/core/Matrix, depleted supplies or full inventory; safe recovery attempted, time/cost, stock and progress/claim preservation. No silent claim reset or diagnostic rescue. |
| Audio/visual comfort | One machine, identical group and mixed workshop; start/stop/falloff, seams/clipping, extended-listening fatigue, GUI contrast, state/port readability and actual display settings without device identifiers. Human judgments stay attributed as human. |
| Save/reconnect | Before/after inventory, fluids, partial work, structure/port ownership and quest/claim state; clean stop/reopen result and upgrade-copy provenance. A same-version reopen is not upgrade proof. |
| Final summary | Wall/active/paused totals, milestone times, optional-branch time, bottlenecks, repetition, shop use, recovery, unresolved bugs and observed T1 transition. Exact duration, balance, fun and acceptance remain human conclusions. |

No account/player IDs, real-world coordinates, private host paths, tokens,
authentication screens or whole saves belong in reports. Optional in-world
coordinates/dimension may help reproduce a fault, but are not default telemetry.
Redact chat/player lists and log prefixes before sharing evidence.

## Defects, stop conditions and handoff

Use the [T0 bug form](../../.github/ISSUE_TEMPLATE/bug_report.yml) for a local
draft/report when a defect is found. Creating this form does not file an issue.
Capture tested revision/artifact, loader/version, world origin, affected ID,
expected/actual behavior, reproducible steps, evidence, severity and blocking
status. Use MISSING for unknown evidence, not an invented value. A reporter's
severity is not an accepted diagnosis.

Stop the affected run on corruption, duplication, unrecoverable progression,
unsafe migration, credential exposure or wrong installed identity. Preserve the
original evidence and world; do not delete locks, alter claims, overwrite a save or
spend on generation to make a gate pass. Report the failed prerequisite and the
next owner action. Ordinary fixes return to the owning feature stream; product,
world-safety and publication decisions remain with the parent coordinator.

Before a later authorized start, the owner opens only the qualified named instance,
checks its displayed candidate identity, creates the approved fresh Superflat
reference save without diagnostic carryover, records M01 and uses the recoverable
Quest Book/free starter. **These are future start instructions, not permission to
perform them now.** The initial engineering handback below changes no deployment.

### Documentation checkpoint handoff

The table below is the original baseline-only documentation checkpoint, not the
current implementation/qualification state above. Its then-unavailable YAML parser
is subsequently replaced by an actual existing-PyYAML parse/16-field check with
no package installation; GitHub form rendering and all graphical checks remain
separate. Preserve those original tool failures rather than rewriting their source.

| Required disclosure | This bounded checkpoint |
| --- | --- |
| COMPLETED | Bug issue form and this source-aware protocol only; no gameplay feature is certified by them |
| BLOCKED | Longitudinal start: final integrated experience-candidate evidence is MISSING; approved graphical access to the execution host is unavailable and client checks are NOT EXECUTED. Parent coordinator owns qualification/access routing and GO; no exemption is authorized |
| NOT EXECUTED | Application changes, feature integration, JVM/build/native/client checks, PC/Prism writes, deployment, issue filing, push/PR/merge, release/tags and human campaign; full YAML parsing and GitHub form rendering |
| NEEDS HUMAN VALIDATION | Appearance, listening/fatigue, quest clarity, shop usefulness, pacing/complexity, layout/ergonomics, enjoyment, longitudinal observations and final acceptance |
| NEEDS MY DECISION | None for this documentation checkpoint; required-gate deferrals cannot be self-approved |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None for this documentation checkpoint |
| COST / BILLING RISK | None; no provider requests, generation or billing changes |
| DEPLOYED STATE | Unchanged by this checkpoint; historical747 receipt remains in the validation ledger and was not re-inspected here |

Shared changelog, README, route-map and `.agent` synchronization belongs to the
parent coordinator and is deliberately excluded from this isolated checkpoint.

Documentation verification: the existing link checker exits0 (746 relative links,
160 ADR sections/index rows). The attempted YAML parser cannot run: the local
Python alias exits9009 and the Node fallback is unavailable (exit1). No dependency
is installed and no parse/render pass is claimed. The first native text-contract
command reaches its Git identity check, then exits1 because no author is configured;
the explicit personal identity from AGENTS is supplied per Git command without
changing global Git configuration. Subsequent native PowerShell text checks exit0:
16 unique form fields, required-field presence, no YAML tabs, 31 ordered milestones,
20 MISSING/PARTIAL gates, all eight disclosures, graphical BLOCKED state and ASCII
content. They are not a YAML parse. Retain the tooling failures separately from
document-content checks and later candidate qualification.
