# Foundation GO gate

**Result: NO-GO**

Checkpoint: ADR-0125 corrective work above `b07ed16`, 2026-10-16.
This is the current execution gate, separate from the archived previous-session decision.

| Requirement | Evidence and disposition |
| --- | --- |
| Verified baseline and safe evidence integration | `0d79860` is the user-authorized baseline; the two evidence commits remain on their remote branch. Sanitized integration is `e55c518`. |
| Atlas F1/F2 distinction | Reproduced four failures, then corrected Atlas/JEI labels without changing identities (`b07ed16`). |
| Deterministic NBT and inventory checks | 31 suites pass. Five slot-loss failures were reproduced before the ADR-0125 repair. |
| Registered F2 lifecycle | All 16 scenario assertions pass, including a complete real-powered cycle and reconstruction. **The smoke exits 1**, because shutdown does not finish. |
| Complete safe smoke inventory | Pending: the strict shutdown failure also reproduces on an empty boot; repeating the same known failure across the inventory is not a pass. |
| Independent gate review | Pending after a coherent executable gate. |
| Client and survival acceptance | Not executed. Real player UI/quest interaction and the no-command BP route remain mandatory. |

The [current validation ledger](../testing/latest-validation.md) preserves commands, failures,
owned-process thread dumps and limitations. No tests were disabled; a forced cleanup is
deliberately not reported as successful shutdown.

**Next allowed work:** repair the validation runtime, then run the full inventory and independent
review. Read-only research, exact acceptance instructions and non-gameplay preparatory documents
may continue. Broad T0 implementation, T2 consumers, new registry IDs and a speculative
multiblock framework remain blocked. No merge is authorized.
