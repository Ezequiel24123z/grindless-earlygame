# T0 survival and client acceptance

**Status: NOT EXECUTED.** Automation and headless evidence do not fill in these results.
Record the exact mod jar hash, pack manifest revision, Java/Forge version, world seed,
operator, dates and evidence paths when this checklist is actually performed.

## Separate the two worlds

**Survival world:** create a fresh ordinary Overworld, Survival, cheats off. Use the same jar
as the validated candidate, with the documented optional client pack if testing its UI.
Gather entry materials normally. The provisional 35-60-minute target starts at the documented vanilla-
material entry, not a bare spawn; record acquisition time separately. No `/give`, `/setblock`,
`/data`, creative mode or carried inventory from a diagnostic world is permitted.

**Diagnostic world:** a separate disposable creative/commands-enabled world is allowed only
for the client/NBT checks below. Its commands and screenshots do not count as survival evidence.
Keep both worlds and their logs separate. Back up any existing world before migration testing.

## No-command route

| Step | Action | Expected observation / failure indicator | Evidence to record |
| --- | --- | --- | --- |
| Entry | Gather wood, crafting stone, glass/sand, copper, redstone, clay, gravel and carbon fuel; reserve two tagged iron for Dynamo/Extractor. Craft a chest from ordinary wood. | Inputs are obtainable without a future-tier device. Count the five controller bricks and seven brick blocks for the chamber; no command items fill a gap. | Starting inventory, seed, acquisition time. |
| Tools | Craft the Multitool, Dynamo, Extractor, Terminal and early Process Atlas from their visible recipes. | Dynamo and Extractor spend one iron each; the other workshop crafts need no iron or Matrix. No quest permission is required. | Recipe discovery route and before/after inventory. |
| Extraction | Place Dynamo next to Extractor and a chest next to the Extractor. Right-click the Dynamo, replenishing within its two-crank capacity. | Work proceeds and a real vein product reaches the chest. Material need not be iron. Record actual cycle time and vein variability; do not assume every deposit is richness 1. | Machine positions, status transitions, chest output. |
| Starvation | Let power run out, then crank again. | State explains lack of power; work/input/output are not lost and work resumes. | Before/after work and contents. |
| Ceramics | Mill gravel in a Quern. Put a Hand Pump over water facing the Clay Mixer; supply two clay, one sand and F0 power. Build the 2x2x2 Firing Chamber and feed three unfired pieces plus carbon. | Exactly three unfired pieces, then two insulators and one grog. Water arriving after the items wakes the Mixer. Insulator plus redstone makes the core; no early reward bypasses it. | Visible recipe routes, water amount, ingredients and outputs. |
| Structure recovery | During firing, remove the far top brick, wait, then repair it. Rotate/rebuild the layout in another direction. | Work and input/fuel remain; the menu names the missing structure. Repair resumes; the rotated layout works. | Before/after work, orientation, status and fuel counts. |
| Recovery and handling | Reuse grog with fresh clay/water. Build ceramic-based transport and storage; block an output and empty its target. | Grog has a useful sink without net solid gain. Pending output resumes automatically; no duplicate batch or permanent stall. | Inventory counts and observed automation. |
| Calibration | Allocate the Dynamo to the Terminal, avoiding simultaneous full-rate demand from the Extractor. Right-click Terminal with one Data Core. Keep supplying F0. | One core becomes one Calibrated Data Core after 600 full-power ticks; no hidden unlock. Empty-hand right-click retrieves it. An output-waiting blocked state is normal. | Core counts, timing, status and retrieval. |
| Relocation/reload | With recoverable contents or partial work, Shift-right-click a Grindless block with the Multitool, place it again, then save/quit and reopen the world. | Charge, contents and relevant work remain; no duplicate drops. Ordinary right-click with the Multitool rotates instead. | Inventory before/after, world save/reopen notes, client log. |
| Exit | Craft the physical batch from the calibrated core, two glass, two copper and two redstone. | Exactly five F1 Relay Matrices, recoverable from the craft. One batch funds the five named first factory controllers. | Exit inventory and elapsed time from material entry. |
| Discovery | Use the available guide/book/viewer during the route, not source code or an external wiki. | Every necessary action and Bootstrap craft is discoverable. Check the complete long structure-fault tooltip and water readout at each GUI scale. | Screenshots and exact confusing/missing text. |

Output-blocking/automation stress beyond the initial material budget belongs in the diagnostic
world or later survival continuation. Do not quietly add command-spawned hoppers to the
no-command route. Native hopper iron costs are recorded separately from the mod-machine budget.

## Diagnostic client and F2 checks

Use these only in the separate diagnostic world:

```text
/give @s grindless:process_atlas
/give @s grindless:quest_book
/give @s grindless:field_guide
/give @s grindless:relay_matrix 1
/give @s grindless:relay_matrix{Rating:2} 1
/give @s grindless:relay_matrix{Rating:1.5d} 1
/give @s grindless:relay_matrix{Rating:"2"} 1
```

Open the Atlas, select `assemble/relay_matrix_f2` and inspect the input/output detail pane:
F1 must be visible on the required Matrix, F2 on the result. Repeat with JEI installed and
check its item tooltips. Record GUI scale/resolution and whether either label is truncated.
Test a pack without JEI separately; the native viewer must still open.

For the Quest Book, follow/claim its existing prerequisites; do not edit quest save flags.
An F1 stack alone must not make `industrial_matrix` claimable. A valid F2 stack must count,
malformed ratings must not count, and moving the stack between main inventory and offhand must
not double its displayed count. Claim once, confirm evidence is not consumed, and confirm a
second claim does not duplicate the reward. The current quest requires the Factory Portal;
its consistency with the guide's "optional" wording remains a product question, not silently
changed by this foundation repair.

Save, fully exit the client/server, restart and recheck both ratings and claimed status.
For a second client on a dedicated test server, compare the Assembler output slot and book
count after transfer and reconnect. A generic item-packet check does not replace this observation.

The automated counterpart is:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -Only matrix-lifecycle
```

Its evidence is `forge/run/smoke-matrix-lifecycle.log`. It proves the registered-item lifecycle
and reconstruction, not a player screen or an OS-process restart of a retained survival world.

## Record and decision

Store actual screenshots/recordings and client/server logs with the tested revision, for example
under ignored `build/acceptance/<run-id>/`; preserve the outcome and reviewed evidence references
in `docs/testing/latest-validation.md`. Do not commit private machine paths, player identifiers
or entire world saves. Mark each row PASS, FAIL or NOT RUN with observations, not intentions.

Any missing input route, undiscoverable action, duplicate reward, lost/duplicated item, misleading
state, failed reload or mandatory repeated unautomated step prevents acceptance. Only a recorded
successful no-command route can close BP and allow the next frontier; automated green alone cannot.
