# A03 process-cache generation boundary

## Source and current state

Received source: `d22a8ed5efb7bbc55866ba15f5c866dcfca9239d`, not old main or
the original audit's `0d79860`. This bounded unit concerns the current shared executor,
not a repeat of the completed [T0 audit](../T0-AUDIT.md).

**Implementation, focused JVM/build and subsequent genuine native reload qualification passed.**
The original child-source proof and the later coordinator-native proof below remain distinct.
Frozen implementation: `fd9077ea397b28563bc301862a0753943b33527e`.
ADR-0153 was accepted
on the coordinator's `2b9218b0bfa19b5b4989f95a2f6c801f8a75f0eb` checkpoint. A fresh
opaque typed `Generation` identity is published inside the complete immutable
`ProcessLookup.GRAPH`, including equivalent/empty content. Reading it allocates nothing;
no independent epoch or publication field is introduced.
An empty live selection no longer clears its separate paid `WorkRecipe`/`Progress`;
the existing selection rule still resets work when a different recipe ID takes ownership.
This retains the documented paid-work separation without a new save key or identity.
The bounded `beforeSubscriptions` base-server hook compares that identity only for
process machines and refreshes subscriptions once after change, including empty lists.
Explicit logistics hold defers the hook until release; `lastWorkTick` is untouched.
The frozen pre-fix counterexample actually failed seven exact assertions. The fixed
source passes common/Forge compilation, five selected suites and full build. Native
server/reload proof, deployment and human acceptance are not supplied by these checks.

## Subsequent coordinator-native red/green

The coordinator's separate exact444 production fixture first proves40 idle
observations and actual ingot-tag publication, then only the expected missed-wake
failure: harness1, natural server0. Read-only recovery corrects the observer's
versioned-name assumption; the actual staged `grindless.jar` hashes to
`bf43947364bbb3f180c2f7cb728f460f98eea45bc652d4493e0ae682fda7b984`.
No JVM is replayed.

Integrated559 then full-builds/packages and passes the selected Java tooling gate.
Its real production reload green has all five `[Server]` markers: wake only after
publication, one gravel consumed/two sand produced, exactly3840FU paid and40
subsequent stable ticks without extra work. Harness0, natural server0 and0Java;
runtime jar SHA-256:
`98aa47dd18a555bf8a7e69989fd075e161fceaf6b2c799727c949ef5792cb061`.
Closed-file recovery receipt
`fdfa2485135c30626d0e7de077893c763331a852513a8f27d791c378e6538c1d`
preserves the original observer self-read failure separately; again no replay.
See [the complete integrated ledger](latest-validation.md) for all raw hashes.

Only after this pass is `recipe-cache-reload` added to the existing scenario
manifest. There is no new CI step. This closes loaded-idle/tag-reload wakeup,
not client acceptance, an actual restart or a longitudinal player-save claim.

## Concrete source counterexample

`MaterialRegistry.rebuild` replaces the material snapshot and calls
`ProcessLookup.rebuild`, which publishes a complete immutable graph/index. A loaded
`ProcessMachineBlockEntity.recipe` at the received source nevertheless returns its
non-null `cached` object before consulting that publication. The guard now checks the
published generation identity first; unchanged-identity work still uses the existing
`WorkRecipe` rule. Dormant machines observe the same publication before subscribed work.

The existing `VerifyMachine` suite exercises this exact counterexample using bound
vanilla item holders under diagnostic material tags:

- `cache_probe` has RAW, CRUSHED and INGOT forms, generating `b1/cache_probe`.
- Removing only INGOT retires that row. Its RAW ingredient still matches and its
  CRUSHED output still resolves, so ordinary completion revalidation alone cannot
  reject the obsolete cached definition.
- A machine loaded with one remaining paid tick must neither draw Flux nor consume or
  emit items for that retired row. Unchanged recipe identity must retain `WorkRecipe`
  and `Progress`; restoring the row must use the current material provider and charge
  only the remaining work.
- The real common `serverTick` path is also exercised with an empty subscription list:
  an unchanged generation must keep its cache, while a new generation must invalidate it.
  This no-level contract fixture does not claim a native loaded-world wakeup.
- A further generation publication in the same work tick must not charge or advance the
  next queued batch. This exercises the existing `lastWorkTick` protection across the
  new refresh boundary; scheduler-handle deduplication alone does not prove that case.

The green fixture keeps two queued raw items so a same-tick retry cannot appear safe
merely because the first batch left the machine empty. The original retirement
oracles still reject any one-item consumption, Flux draw or output from a withdrawn row.
Stable generation reads and distinct equivalent empty-index identities are also checked.

The fixture is diagnostic, not an authored resource route or a gameplay recipe.
It restores the prior registry tags afterward. No new registry IDs or NBT keys are added.
The existing `VerifyFluid.completedWorkRecovery` scenario now rebuilds the graph at its
secondary-starvation boundary, reusing all its exact work, NBT, fluid and no-extra-Flux
oracles rather than adding another equivalent completed-batch test.

## Actual frozen red

Source: `3643c6a049e3beb39b28fe23f4b6d8a1f316db30`, clean and detached before/after.
The exclusive PC lease ran only `:common:writeCheckClasspath`, `javac VerifyMachine`
and that real suite. Their exits are respectively0,0,1. The suite reports exactly
**7 MACHINE CHECK(S) FAILED**, including unchanged-generation rebind, empty-ticker
invalidation and retired-row I/O. The retired recipe spends32 of64 stored Flux,
consumes the input, emits output and loses119 paid work ticks. Reconstruction then
fails the current-provider/yield oracle. These are assertion failures, not a
compilation, classpath or launcher failure.

Fresh guards pass before each JVM stage. Initial headroom is10552.9MiB physical,
34794.7MiB commit and178566852608 work/cache bytes, above unchanged6/8/8GiB floors.
Gradle is explicitly3GiB/CPU2/one worker/no parallel/no daemon; helpers are1GiB/CPU2.
Postcheck finds zero Java processes. No server, client, historical suite, pack or
deployment runs in this lease.

Ignored evidence: `build/validation/a03-cache-red-3643c6a-20261019T185949857Z/`.

| Evidence | SHA-256 |
| --- | --- |
| Frozen `VerifyMachine.java` | `a2c68372399069994fe91f6b639c23093d91cf441048e4546cc156c78c2f17b0` |
| Common/classpath stdout | `f14e57f6e340706b0b5be2184ffd9da31a796d803dc0cb9bdd4ca02e068d32fb` |
| Suite stdout | `ebc209328f8799b5ef9a0ff67707d8ea92e33a6c3429dc27c12561d47e240e55` |
| Original receipt | `15b1bb48f35b1e098ed886a827be78dab9037c0cf510df5571c9b18696b9791d` |

The original auxiliary receipt's `failedAssertions:0` is **invalid**: its anchored
collector ignored Minecraft's `[STDOUT]:` prefix. Its expected-red classification
also checked the actual exit, explicit failure labels and summary, which do prove
the seven failures. Preserve the original receipt/logs; do not silently rewrite it
or rerun unchanged code to repair an observation counter. The local collector is
corrected to recognize prefixed and unprefixed assertions. The outer CLI's duplicated
CLIXML information stream is not another test run.

## Actual frozen green and build

Source: `fd9077ea397b28563bc301862a0753943b33527e`, clean/detached before and after
the separately authorized PC lease. Commands compile common/Forge and export the actual
classpath, compile only the five selected suites, run them, then execute full `build`.
All those native exits are0. Gradle `test NO-SOURCE` is not counted as a passing suite.

| Selected suite | Passing assertions | Exit |
| --- | --- | --- |
| `VerifyMachine` | 273 | 0 |
| `VerifyFluid` | 285 | 0 |
| `VerifyMaterial` | 72 | 0 |
| `VerifyPersistence` | 42 | 0 |
| `VerifyWorkshop` | 266 | 0 |
| Total | 938, zero failures | Five real suites |

Fresh guards before every JVM stage remain above6GiB physical/8GiB commit/8GiB work/cache.
Initial measured headroom is10590.2MiB physical,34727.9MiB commit and178094727168 disk
bytes. Use the same3GiB/CPU2/one-worker/no-parallel/no-daemon Gradle and1GiB/CPU2 helpers;
no guard is lowered and no unrelated process is stopped. Both final postchecks find
zero Java processes. Compiler/dependency diagnostics remain in the preserved logs.

Actual `forge/build/libs/grindless-0.1.0-forge.jar`:1925530 bytes,
SHA-256 `a4225cc3e77000df996ab16e7ea71a526922dce23db107704822252ae0833204`.
The archive contains the new `ProcessLookup$Generation.class`. No pack or deployment
task is run. `VerifyRecipes` and UI suites are not rerun: process definitions/costs and
client/layout code remain unchanged; selected machine/fluid checks cover changed assumptions.

Ignored evidence: `build/validation/a03-cache-green-fd9077e-20261019T192825474Z/`.

| Evidence | SHA-256 |
| --- | --- |
| Common/Forge compile stdout | `cfa3535a695f6b6e8079f2681b8e0906c1b52fbf69c6923eba80836f5d058fa4` |
| Machine suite stdout | `36aed2f1c0525ac75c7682296ded97a191b3811035d41c28d01fd4c727ec411b` |
| Fluid suite stdout | `95dab4c8768b1aaea94f4ccbe22266ef2613acede62c81645b58aef701cd5b4e` |
| Full build stdout | `08db5c42915886e7e1f087e121572f9a0cadd59110ef3aa018c842570a43ceda` |
| Original green receipt | `94c6ad8010766e9944379afaf2acaa282a619e6d9b79f35752553edb04cbe2bd` |
| Separate JAR inspection receipt | `1ca17d5d02b13698617a8afbc9837f6e9249ffe0b135efcc72876ae3d18929c4` |

The aggregate RPC exits1 **after** all successful native commands: its auxiliary JAR
coordinate collector uses a multiline regex that rejects Windows CRLF line endings.
The original receipt therefore retains `FAILED_OR_BLOCKED`, its explicit error, and
all actual stage exits/counts. Do not relabel that RPC as exit0 or discard its receipt.
A subsequent inspection uses the source properties' individual lines, confirms actual
CRLF, verifies JAR bytes/hash/new class and unchanged source, and writes the separate
inspection receipt. It launches no JVM and repeats no compile, suite or build. The
two receipts together distinguish successful code qualification from collector failure.

The native loaded-idle/tag-reload uncertainty is explicitly assigned to coordinator QA;
no parallel Level fixture, scenario, server or client is authored/run by this child.

## Reused proof and remaining scope

**A01 is not closed by this unit.** ADR-0132's owned fluid-output recovery and ADR-0138's
native completion/retry evidence apply to their process-machine/native-store paths.
The composed `dcb8010` fluid and states/settling proofs do not establish blocked-output
recovery for the separate `CrudeExtractorBlockEntity`. The coordinator's current-source
check identified its missing independent idle exporter and assigned that defect to
another bounded child. That separate repair is now source-integrated with59 focused
checks and an actual native baseline red; repaired native/SavedData is still its
own obligation. This unit does not close Crude recovery by analogy.

**A02 has current, distinct inventory-counting evidence.** `QuestInventoryEvidence`
counts each actual inventory slot once. The real41-slot `Inventory` in
`VerifyPersistence` reports an offhand stack of3 once and main2 plus offhand3 as5,
without consumption or duplication. That suite now passes42 assertions onfd9077e.
Separately, `SlottedContainer`, `ItemLayout=3` and the existing Matrix lifecycle fixture
cover sparse slots and legacy order. The received70901 qualification passed all43 suites;
none of those proofs establishes recipe-generation invalidation or the Crude's export.
An exact Git comparison with70901 confirms unchanged `NativeFluidStore`, `FluidInsert`,
`SlottedContainer`, `ProcessGraph`, `ProcessLogic` and `VerifyPersistence`; no balance,
transaction protocol or inventory migration is being redesigned. The affected fluid/
machine regressions have now passed on the frozen fixed source, including the changed
cache assumption; that does not replace authentic retained-world compatibility.
See [the validation ledger](latest-validation.md) and [proof-reuse strategy](test-strategy.md).

The base server ticker remains callable with an empty subscription list:
`ProcessMachineBlock.getTicker` binds `MachineBlockEntity::tick` without consulting
subscriptions, and the common dispatcher rejects the client side before `serverTick`.
The selected fix must revalidate/wake only on that server-thread path, with a generation
identity published atomically with the full graph. No rebuild-thread machine callbacks,
world scans or permanent idle lookup subscription are justified.

## Execution status

| Scope | Actual state |
| --- | --- |
| Source trace and exact retired-row construction | Inspected on received d22a8ed |
| Pre-fix3643c6a common/classpath and `javac VerifyMachine` | Actual exits0/0 |
| Pre-fix3643c6a `VerifyMachine` | Expected red: exit1, seven explicit assertion failures |
| Fixedfd9077e common/Forge, selected `javac`, five suites and full build | Actual exits0;938 passing assertions |
| Actual fixed distributable / new generation class | Inspected separately after preserved collector failure; exact hash above |
| Active-cache generation guard and bounded idle refresh | Implemented under ADR-0153; common contracts pass |
| Native loaded-world miss/wakeup observation | Actual exact444 red / integrated559 green,5 real markers/conserved batch/40 stable ticks; no-level fixture is not substituted |
| Native registration / deployment / client / retained world | Not run by this child; no acceptance inferred |

The child must not use the coordinator's private default PC inventory as a mutable target,
push, open a PR or deploy. The coordinator owns publication and integrated qualification.
