# A03 process-cache generation boundary

## Source and current state

Received source: `d22a8ed5efb7bbc55866ba15f5c866dcfca9239d`, not old main or
the original audit's `0d79860`. This bounded unit concerns the current shared executor,
not a repeat of the completed [T0 audit](../T0-AUDIT.md).

**Active-cache guard implemented; wakeup and JVM execution pending.** Recipe selection
now invalidates the cache against the identity of the immutable list already published
inside `ProcessLookup.GRAPH`. No independent epoch or publication field is introduced.
An empty live selection no longer clears its separate paid `WorkRecipe`/`Progress`;
the existing selection rule still resets work when a different recipe ID takes ownership.
This retains the documented paid-work separation without a new save key or identity.
The coordinator is selecting the generation-wakeup seam under reserved ADR-0153.
No native reproduction, build, deployment or human acceptance is claimed.

## Concrete source counterexample

`MaterialRegistry.rebuild` replaces the material snapshot and calls
`ProcessLookup.rebuild`, which publishes a complete immutable graph/index. A loaded
`ProcessMachineBlockEntity.recipe` at the received source nevertheless returns its
non-null `cached` object before consulting that publication. The guard now checks the
published list identity first; unchanged-identity work still uses the existing
`WorkRecipe` rule. Dormant machines do not yet receive the new-generation wakeup.

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

The fixture is diagnostic, not an authored resource route or a gameplay recipe.
It restores the prior registry tags afterward. No new registry IDs or NBT keys are added.
The existing `VerifyFluid.completedWorkRecovery` scenario now rebuilds the graph at its
secondary-starvation boundary, reusing all its exact work, NBT, fluid and no-extra-Flux
oracles rather than adding another equivalent completed-batch test.

## Reused proof and remaining scope

A01 is not reopened: ADR-0132's owned fluid-output recovery and ADR-0138's exact native
completion/retry contracts already have source-bound evidence, including the composed
`dcb8010` fluid suite and production states/settling proof.

A02 is not reopened: `SlottedContainer`, `ItemLayout=3`, `VerifyPersistence` and the
Matrix lifecycle fixture already cover sparse slots, legacy order and exact inventory
accounting. The received70901 qualification passed all43 suites. These proofs do not
establish recipe-generation invalidation, but their unchanged mechanisms remain reusable.
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
| `VerifyMachine` regression | Prepared, NOT_RUN |
| Active-cache generation guard | Implemented, NOT_RUN |
| Idle-miss wakeup / complete regression unit | Pending coordinator decision |
| Focused JVM checks / relevant regressions / build | NOT_RUN; exclusive PC window required |
| Native registration / deployment / client / retained world | NOT_RUN; no acceptance inferred |

The child must not use the coordinator's private default PC inventory as a mutable target,
push, open a PR or deploy. The coordinator owns publication and integrated qualification.
