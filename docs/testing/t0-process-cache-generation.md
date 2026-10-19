# A03 process-cache generation boundary

## Source and current state

Received source: `d22a8ed5efb7bbc55866ba15f5c866dcfca9239d`, not old main or
the original audit's `0d79860`. This bounded unit concerns the current shared executor,
not a repeat of the completed [T0 audit](../T0-AUDIT.md).

**Implementation and selected JVM/build proof complete; native qualification pending.** ADR-0153 was accepted
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
The frozen pre-fix counterexample has actually run and failed its exact assertions;
frozenfd9077e selected green/build are now recorded below. Native server, combined
deployment and human acceptance remain unclaimed.

## Concrete source counterexample

`MaterialRegistry.rebuild` replaces the material snapshot and calls
`ProcessLookup.rebuild`, which publishes a complete immutable graph/index. A loaded
`ProcessMachineBlockEntity.recipe` at the received source nevertheless returns its
non-null `cached` object before consulting that publication. The guard now checks the
published generation identity first; unchanged-identity work still uses the existing
`WorkRecipe` rule. Dormant machines observe the same publication before subscribed work.

The existing `VerifyMachine` suite now prepares this exact counterexample using bound
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

## Actual frozen green

Source `fd9077ea397b28563bc301862a0753943b33527e` remains clean/detached. Common/
Forge/classpath and checker compilation exit0, followed by Machine273, Fluid285,
Material72, Persistence42 and Workshop266 checks:938 PASS,0 FAIL. Full build exits0.
The1925530-byte Forge jar hashes to
`a4225cc3e77000df996ab16e7ea71a526922dce23db107704822252ae0833204`
and includes `ProcessLookup$Generation.class`.

The outer aggregate exits1 only after build in an auxiliary coordinate collector
whose regex did not accept CRLF. Keep original receipt
`94c6ad8010766e9944379afaf2acaa282a619e6d9b79f35752553edb04cbe2bd`.
A separate read-only source/JAR inspection produces addendum
`1ca17d5d02b13698617a8afbc9837f6e9249ffe0b135efcc72876ae3d18929c4`.
Do not repeat or relabel the successful compile/tests/build to repair that collector.
The lease releases with zero Java; no server/client/pack/deployment runs.
Evidence: `build/validation/a03-cache-green-fd9077e-20261019T192825474Z/`.

## Reused proof and remaining scope

A01 is a separate current Crude Extractor defect, not a proven process-fluid property.
Its class still lacks an independent pending-output exporter and calls `pushOutput`
only after `eject`; a dedicated unit now owns its drain/wakeup/accounting regressions.
ADR-0132/0138's process-fluid receipts and dcb8010 states/settling proof do not certify
that distinct class. No analogy is used to mark A01 fixed.

A02 is already corrected by `QuestInventoryEvidence.count`, which traverses each
Container slot exactly once and does not add offhand again. `VerifyPersistence` checks
the actual41-slot player Inventory, three offhand items and a split total of five.
Other sparse-slot/ItemLayout/lifecycle proofs retain their own scope; they are not a
substitute for that offhand test. The received70901 qualification passed all43 suites. These proofs do not
establish recipe-generation invalidation, but their unchanged mechanisms remain reusable.
An exact Git comparison with70901 confirms unchanged `NativeFluidStore`, `FluidInsert`,
`SlottedContainer`, `ProcessGraph`, `ProcessLogic` and `VerifyPersistence`; no balance,
transaction protocol or inventory migration is being redesigned. The affected fluid/
machine regressions have the selected green execution above; the real Level/tag/ticker
boundary is assigned to the separate native fixture.
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
| New-implementation selected regressions | Actual frozenfd9077e938 PASS/0FAIL |
| Active-cache generation guard and bounded idle refresh | Implemented under accepted ADR-0153; selected JVM proof passes |
| Native loaded-world miss/wakeup observation | NOT_RUN; no-level fixture is not that proof |
| Focused JVM checks / relevant regressions / build | Actual exits0; auxiliary post-build collector failure/addendum retained separately |
| Native registration / deployment / client / retained world | NOT_RUN; no acceptance inferred |

The child must not use the coordinator's private default PC inventory as a mutable target,
push, open a PR or deploy. The coordinator owns publication and integrated qualification.
