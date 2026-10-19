# T0 audit: preserved source and current authority

The completed audit is preserved at
[e3f0124 / T0-AUDIT.md](https://github.com/Ezequiel24123z/grindless-earlygame/blob/e3f0124/docs/T0-AUDIT.md).
It targets `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`, not the later70901 continuation.
It was read during SSH intake and is not being rerun or rewritten.

Its findings remain useful identifiers: A01 output recovery, A02 inventory accounting,
A03 execution/cache transitions, A04 later Matrix consumers, A05 discoverability,
A06 documentation, A07 art/audio, A08 configuration/prototypes and A09 factory guidance.
Do not treat every original finding as an unfixed current defect: subsequent committed
work has source-bound recovery, inventory, execution, guidance and resource proofs.

Use [the current validation ledger](testing/latest-validation.md),
[T0 contract](tiers/t0.md), [action-plan bridge](T0-T1-ACTION-PLAN.md),
[task graph](../.agent/work-plan.json) and [return handoff](../RETURN-HANDOFF.md).
Any new fix must cite a concrete current counterexample and affected proof assumptions.
Human gameplay remains unexecuted where recorded; green engineering is not acceptance.

## Narrow current disposition

| Finding | Current evidence / action |
| --- | --- |
| A01 direct chest backpressure | Still open in the actual Crude Extractor: only the work subscription exists, and `pushOutput` runs only after successful `eject`. A separate bounded unit owns recovery/accounting tests and repair. Process-fluid output proofs do not certify this class. |
| A02 duplicate offhand evidence | Already corrected: `QuestInventoryEvidence` traverses the Container once; `VerifyPersistence` checks actual player Inventory size41, offhand3 and split5. Reuse that proof, not another arithmetic proxy. |
| A03 generated-graph membership/idle wake | Pre-fix3643c6a fails seven real assertions; completefd9077e passes938 selected checks/build. Atomic generation/server-hook code is integrated; native loaded-world proof is still pending. |

These are source-bound reconciliations of existing finding IDs, not another whole-code
audit or a declaration that all remaining human/tier criteria pass.
