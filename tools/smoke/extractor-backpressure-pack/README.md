# A01 Crude Extractor recovery regression

Source baseline: `48c9513f57a1a7391108c86b21bfcd386044651e`, inherited from the active
coordinator, not `main`. This is a diagnostic fixture, not a survival route, Tier
unlock, release proof or permission to deploy to a player's instance.

## Frozen pre-fix disposition

Before any application change, no-JVM inspection confirms all six intentionally
missing source contracts: independent subscription, exporter release, retry
callback, same-world-tick work guard, copied offer and changed-remainder check.
Structural validation passes for nine functions, eleven resolved local function
references and ten conditional/terminal expectation markers. Documentation
validation passes 640 links and 154 ADR section/index pairs; whitespace passes.
The runtime counterexample is frozen for later execution, not reported as a
Java/native RED pass. Application source remains byte-for-byte at the baseline.

## Current counterexample and selected pattern

At that source, `CrudeExtractorBlockEntity.updateSubscriptions` cancels `working`
when completed work cannot fit the internal output. `pushOutput` is reached only
from a successful `eject`. There is no independent output subscription. Emptying
the adjacent chest therefore cannot move the already-owned full stack and wake
the completed cycle. The existing `VerifyExtractor` checks policy arithmetic,
not this loaded-world path. Process-machine output evidence is not extractor
recovery evidence.

Reuse the existing process-machine pending-export policy: one independently
subscribed, eight-tick export attempt while owned output remains, using
`ItemInsert`, a copied offered stack and changed-remainder notification.
Reuse its world-time work guard because `TickSubscriptions.tick` executes newly
added work in the same pass. Do not change the global scheduler, item sides,
vein derivation, F0 rates, products or the `Progress`/`Output` save keys.

## Native oracle

- In Luna, a known direct helium-3 product avoids dependence on the pack's ore
  tags. A full internal stack and exactly one completed paid cycle wait beside
  a full chest with no power.
- Retain that exact NBT, clear only the original inventory before replacing
  the diagnostic block entity, and restore the existing saved keys into a fresh
  instance. Verify the blocked state survives its first ticks.
- Empty only the same chest's inventory. Expect exactly 65 helium-3 in the
  destination, no internal remainder, zero paid progress and zero Flux, stable
  across a further 16 ticks. No hopper, block replacement at the destination or
  new energy wakes this path.
- In the Drift, where no vein exists, offer a previously owned stack to a chest
  with only one unit of space. Expect a 63-unit remainder; freeing one filler
  slot must drain it. Destination totals 127 (63 staged + 64 owned), while
  progress 123.5 and stored Flux 16 remain unchanged.
- In a separate Luna chunk, a nearly completed cycle with 63 owned items and
  16 Flux crosses the last internal-space boundary. The observation scheduled
  exactly one world tick later must find 64 items and 8 Flux, not a second
  work/Flux debit from a newly appended subscription.

The first Luna chunk `(0, 0)` must record exactly one extraction and the guard
chunk `(1, 0)` exactly one. Read the resulting dimension's `grindless_veins.dat`
after clean server shutdown to check actual accounting; item counts alone do
not prove the SavedData counter. The fixture leaves its diagnostic block
entities and storage checkpoints available for inspection.

Reconstruction exercises native block-entity `load` and first-tick subscriptions.
It is not an actual server restart, chunk unload/reload, crash-atomic persistence,
foreign capability guarantee or retained-player-world migration.

## Execution boundary

This fixture owns only its uniquely named commands/expect/pack. It intentionally
does not edit `scenarios.txt`, the shared harness or CI; their owner/coordinator
selects integration. Before an explicit immutable-PC lease, all Java checks,
compilation, build, native execution, packaging and deployment remain **NOT_RUN**.
No `pc-workflow` command or default private inventory is used by this child.

After the coordinator stages the exact frozen source in a separately owned PC
worktree and grants the execution window, the existing direct harness is enough:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
$env:SMOKE_DATAPACK = (Resolve-Path .\tools\smoke\extractor-backpressure-pack).Path
$env:SMOKE_COMMANDS = (Resolve-Path .\tools\smoke\extractor-backpressure.commands).Path
$env:SMOKE_EXPECT_FILE = (Resolve-Path .\tools\smoke\extractor-backpressure.expect).Path
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\smoke-boot.ps1
```

Use the harness's ordinary assertion budget. The scenario lasts 65 world ticks
after its bounded loaded-chunk barrier and needs no extended timeout, optional pack, new dependency
or production diagnostic flag. Preserve the actual failing baseline log and
exact source before running the repaired source; source review is not a RED run.
